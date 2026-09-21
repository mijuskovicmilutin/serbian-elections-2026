package rs.serbianelection2026.backend.poll.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.serbianelection2026.backend.common.exception.BadRequestException;
import rs.serbianelection2026.backend.common.exception.BusinessRuleException;
import rs.serbianelection2026.backend.common.exception.NotFoundException;
import rs.serbianelection2026.backend.election.entity.ElectoralList;
import rs.serbianelection2026.backend.election.repository.ElectoralListRepository;
import rs.serbianelection2026.backend.ingestion.entity.DataImport;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.repository.DataImportRepository;
import rs.serbianelection2026.backend.poll.dto.MediaSourceInput;
import rs.serbianelection2026.backend.poll.dto.PollInput;
import rs.serbianelection2026.backend.poll.dto.PollReadiness;
import rs.serbianelection2026.backend.poll.dto.PollResultInput;
import rs.serbianelection2026.backend.poll.entity.OptionKind;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollAuditAction;
import rs.serbianelection2026.backend.poll.entity.PollAuditLog;
import rs.serbianelection2026.backend.poll.entity.PollResult;
import rs.serbianelection2026.backend.poll.entity.PollStatus;
import rs.serbianelection2026.backend.poll.entity.Pollster;
import rs.serbianelection2026.backend.poll.repository.PollAuditLogRepository;
import rs.serbianelection2026.backend.poll.repository.PollRepository;
import rs.serbianelection2026.backend.poll.repository.PollResultRepository;
import rs.serbianelection2026.backend.poll.repository.PollsterRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Review workflow behind the internal API: create and edit drafts, then approve or reject. Every
 * change is written to the poll's audit log; nothing becomes public except through {@link #approve}.
 */
@Slf4j
@Service
public class AdminPollService {

    private final PollRepository pollRepository;
    private final PollResultRepository pollResultRepository;
    private final PollsterRepository pollsterRepository;
    private final PollAuditLogRepository auditLogRepository;
    private final ElectoralListRepository electoralListRepository;
    private final DataImportRepository dataImportRepository;
    private final ObjectMapper objectMapper;

    private static final List<ImportSource> DISCOVERY_SOURCES =
            List.of(ImportSource.POLL_CRTA, ImportSource.POLL_CESID, ImportSource.POLL_MEDIA);

    public AdminPollService(
            PollRepository pollRepository,
            PollResultRepository pollResultRepository,
            PollsterRepository pollsterRepository,
            PollAuditLogRepository auditLogRepository,
            ElectoralListRepository electoralListRepository,
            DataImportRepository dataImportRepository,
            ObjectMapper objectMapper) {
        this.pollRepository = pollRepository;
        this.pollResultRepository = pollResultRepository;
        this.pollsterRepository = pollsterRepository;
        this.auditLogRepository = auditLogRepository;
        this.electoralListRepository = electoralListRepository;
        this.dataImportRepository = dataImportRepository;
        this.objectMapper = objectMapper;
    }

    public record PollDetail(Poll poll, List<PollResult> results, List<PollAuditLog> audit) {
    }

    /** {@code lastRun} is null until the source has been checked once. */
    public record SourceStatus(ImportSource source, DataImport lastRun) {
    }

    @Transactional(readOnly = true)
    public List<SourceStatus> getDiscoverySources() {
        return DISCOVERY_SOURCES.stream()
                .map(source -> new SourceStatus(
                        source, dataImportRepository.findFirstBySourceOrderByStartedAtDesc(source).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<Poll> list(Collection<PollStatus> statuses, Pageable pageable) {
        Collection<PollStatus> effective = statuses == null || statuses.isEmpty() ? List.of(PollStatus.values()) : statuses;
        log.info("Listing polls for review: statuses={}, page={}, size={}", effective, pageable.getPageNumber(), pageable.getPageSize());

        Page<Poll> page = pollRepository.findByStatusInOrderByPublishedAtDesc(effective, pageable);

        log.info("Successfully listed {} poll(s) for review (page {} of {})", page.getNumberOfElements(), page.getNumber(), page.getTotalPages());
        return page;
    }

    @Transactional(readOnly = true)
    public PollDetail get(Long id) {
        log.info("Fetching poll for review: id={}", id);
        return detail(load(id));
    }

    @Transactional
    public PollDetail create(PollInput input) {
        log.info("Creating draft poll: pollster={}, title={}", input.pollsterSlug(), input.title());

        Pollster pollster = pollster(input.pollsterSlug());
        assertSourceNotTaken(pollster, input.sourceUrl(), null);

        Poll poll = Poll.builder().status(PollStatus.DRAFT).build();
        applyContent(poll, pollster, input);
        poll = pollRepository.save(poll);
        List<PollResult> results = replaceResults(poll, input.results());
        audit(poll, PollAuditAction.CREATED, "Draft created with " + results.size() + " result(s)");

        log.info("Successfully created draft poll: id={}", poll.getId());
        return detail(poll);
    }

    @Transactional
    public PollDetail update(Long id, PollInput input) {
        log.info("Updating poll: id={}", id);

        Poll poll = load(id);
        Pollster pollster = pollster(input.pollsterSlug());
        assertSourceNotTaken(pollster, input.sourceUrl(), id);

        Map<String, String> before = snapshot(poll, pollResultRepository.findByPoll_IdOrderByDisplayOrderAsc(id));
        boolean wasCandidate = poll.getStatus() == PollStatus.DISCOVERED;
        applyContent(poll, pollster, input);
        if (wasCandidate) {
            poll.setStatus(PollStatus.DRAFT);
        }
        poll = pollRepository.save(poll);
        List<PollResult> results = replaceResults(poll, input.results());
        Map<String, String> after = snapshot(poll, results);

        String changes = (wasCandidate ? "status: DISCOVERED -> DRAFT (opened by reviewer)\n" : "") + diff(before, after);
        changes = changes.strip();
        if (changes.isEmpty()) {
            log.info("Poll update changed nothing: id={}", id);
        } else {
            audit(poll, PollAuditAction.UPDATED, changes);
            log.info("Successfully updated poll: id={}, status={}", id, poll.getStatus());
        }
        return detail(poll);
    }

    @Transactional
    public PollDetail approve(Long id) {
        log.info("Approving poll: id={}", id);

        Poll poll = load(id);
        if (poll.getStatus() == PollStatus.APPROVED) {
            throw new BusinessRuleException("Poll is already approved");
        }
        List<PollResult> results = pollResultRepository.findByPoll_IdOrderByDisplayOrderAsc(id);
        PollReadiness readiness = PollReadinessEvaluator.evaluate(poll, results);
        if (!readiness.ready()) {
            log.warn("Refusing to approve poll id={}: missing {}", id, readiness.missing());
            throw new BusinessRuleException("Poll cannot be published yet, missing: " + String.join(", ", readiness.missing()));
        }

        poll.setStatus(PollStatus.APPROVED);
        poll.setReviewedAt(Instant.now());
        poll.setReviewNote(null);
        poll = pollRepository.save(poll);
        audit(poll, PollAuditAction.APPROVED, null);

        log.info("Successfully approved poll: id={}", id);
        return detail(poll);
    }

    @Transactional
    public PollDetail reject(Long id, String note) {
        log.info("Rejecting poll: id={}", id);

        Poll poll = load(id);
        if (poll.getStatus() == PollStatus.REJECTED) {
            throw new BusinessRuleException("Poll is already rejected");
        }
        boolean wasPublic = poll.getStatus() == PollStatus.APPROVED;

        poll.setStatus(PollStatus.REJECTED);
        poll.setReviewedAt(Instant.now());
        poll.setReviewNote(note.trim());
        poll = pollRepository.save(poll);
        audit(poll, PollAuditAction.REJECTED, (wasPublic ? "Withdrawn from public: " : "") + note.trim());

        log.info("Successfully rejected poll: id={}, wasPublic={}", id, wasPublic);
        return detail(poll);
    }

    private Poll load(Long id) {
        return pollRepository.findWithPollsterById(id).orElseThrow(() -> {
            log.warn("Poll not found for review: id={}", id);
            return new NotFoundException("Poll not found: " + id);
        });
    }

    private Pollster pollster(String slug) {
        return pollsterRepository.findBySlug(slug).orElseThrow(() -> {
            log.warn("Unknown pollster in request: slug={}", slug);
            return new BadRequestException("Unknown pollster: " + slug);
        });
    }

    private void assertSourceNotTaken(Pollster pollster, String sourceUrl, Long ownId) {
        boolean taken = ownId == null
                ? pollRepository.existsByPollster_IdAndSourceUrl(pollster.getId(), sourceUrl.trim())
                : pollRepository.existsByPollster_IdAndSourceUrlAndIdNot(pollster.getId(), sourceUrl.trim(), ownId);
        if (taken) {
            throw new BusinessRuleException("A poll from this pollster with the same source URL already exists");
        }
    }

    private void applyContent(Poll poll, Pollster pollster, PollInput in) {
        if (in.fieldworkFrom() != null && in.fieldworkTo() != null && in.fieldworkFrom().isAfter(in.fieldworkTo())) {
            throw new BadRequestException("fieldworkFrom must not be after fieldworkTo");
        }
        poll.setPollster(pollster);
        poll.setTitle(in.title().trim());
        poll.setPublishedAt(in.publishedAt());
        poll.setFieldworkFrom(in.fieldworkFrom());
        poll.setFieldworkTo(in.fieldworkTo());
        poll.setFieldworkNote(blankToNull(in.fieldworkNote()));
        poll.setSampleSize(in.sampleSize());
        poll.setPopulation(blankToNull(in.population()));
        poll.setMethod(blankToNull(in.method()));
        poll.setConductedBy(blankToNull(in.conductedBy()));
        poll.setCommissionedBy(blankToNull(in.commissionedBy()));
        poll.setMarginOfError(in.marginOfError());
        poll.setResultBasis(in.resultBasis());
        poll.setDecidedSharePct(in.decidedSharePct());
        poll.setUndecidedPct(in.undecidedPct());
        poll.setWontVotePct(in.wontVotePct());
        poll.setWillVotePct(in.willVotePct());
        poll.setSourceKind(in.sourceKind());
        poll.setSourceUrl(in.sourceUrl().trim());
        poll.setOriginalDocumentUrl(blankToNull(in.originalDocumentUrl()));
        poll.setSourceNote(blankToNull(in.sourceNote()));
        poll.setMediaSources(serializeMediaSources(in.mediaSources()));
    }

    private List<PollResult> replaceResults(Poll poll, List<PollResultInput> inputs) {
        pollResultRepository.deleteByPoll_Id(poll.getId());
        pollResultRepository.flush();
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<PollResult> rows = new java.util.ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            PollResultInput in = inputs.get(i);
            rows.add(PollResult.builder()
                    .poll(poll)
                    .rawOptionName(in.rawOptionName().trim())
                    .percentage(in.percentage())
                    .displayOrder(i + 1)
                    .optionKind(in.optionKind() == null ? OptionKind.UNSPECIFIED : in.optionKind())
                    .composition(blankToNull(in.composition()))
                    .electoralList(electoralList(in.electoralListId()))
                    .build());
        }
        return pollResultRepository.saveAll(rows);
    }

    private ElectoralList electoralList(Long id) {
        if (id == null) {
            return null;
        }
        return electoralListRepository.findById(id).orElseThrow(() -> new BadRequestException("Unknown electoral list: " + id));
    }

    private String serializeMediaSources(List<MediaSourceInput> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(sources);
    }

    private PollDetail detail(Poll poll) {
        List<PollResult> results = pollResultRepository.findByPoll_IdOrderByDisplayOrderAsc(poll.getId());
        List<PollAuditLog> audit = auditLogRepository.findTop50ByPoll_IdOrderByCreatedAtDescIdDesc(poll.getId());
        return new PollDetail(poll, results, audit);
    }

    private void audit(Poll poll, PollAuditAction action, String details) {
        auditLogRepository.save(PollAuditLog.builder().poll(poll).action(action).details(details).build());
    }

    private Map<String, String> snapshot(Poll p, List<PollResult> results) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("pollster", p.getPollster().getSlug());
        m.put("title", p.getTitle());
        m.put("publishedAt", String.valueOf(p.getPublishedAt()));
        m.put("fieldworkFrom", String.valueOf(p.getFieldworkFrom()));
        m.put("fieldworkTo", String.valueOf(p.getFieldworkTo()));
        m.put("fieldworkNote", String.valueOf(p.getFieldworkNote()));
        m.put("sampleSize", String.valueOf(p.getSampleSize()));
        m.put("population", String.valueOf(p.getPopulation()));
        m.put("method", String.valueOf(p.getMethod()));
        m.put("conductedBy", String.valueOf(p.getConductedBy()));
        m.put("commissionedBy", String.valueOf(p.getCommissionedBy()));
        m.put("marginOfError", num(p.getMarginOfError()));
        m.put("resultBasis", String.valueOf(p.getResultBasis()));
        m.put("decidedSharePct", num(p.getDecidedSharePct()));
        m.put("undecidedPct", num(p.getUndecidedPct()));
        m.put("wontVotePct", num(p.getWontVotePct()));
        m.put("willVotePct", num(p.getWillVotePct()));
        m.put("sourceKind", String.valueOf(p.getSourceKind()));
        m.put("sourceUrl", p.getSourceUrl());
        m.put("originalDocumentUrl", String.valueOf(p.getOriginalDocumentUrl()));
        m.put("sourceNote", String.valueOf(p.getSourceNote()));
        m.put("mediaSources", String.valueOf(p.getMediaSources()));
        m.put("results", results.stream()
                .map(r -> r.getRawOptionName() + " = " + num(r.getPercentage()) + " [" + r.getOptionKind() + "]"
                        + (r.getComposition() == null ? "" : " {" + r.getComposition() + "}")
                        + (r.getElectoralList() == null ? "" : " ->list#" + r.getElectoralList().getId()))
                .collect(Collectors.joining("; ")));
        return m;
    }

    private String diff(Map<String, String> before, Map<String, String> after) {
        return after.entrySet().stream()
                .filter(e -> !Objects.equals(before.get(e.getKey()), e.getValue()))
                .map(e -> e.getKey() + ": " + before.get(e.getKey()) + " -> " + e.getValue())
                .collect(Collectors.joining("\n"));
    }

    private static String num(BigDecimal v) {
        return v == null ? "null" : v.stripTrailingZeros().toPlainString();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
