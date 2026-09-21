package rs.serbianelection2026.backend.ingestion.poll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rs.serbianelection2026.backend.ingestion.entity.DataImport;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.entity.ImportStatus;
import rs.serbianelection2026.backend.ingestion.poll.dto.DiscoveredItem;
import rs.serbianelection2026.backend.ingestion.repository.DataImportRepository;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollStatus;
import rs.serbianelection2026.backend.poll.entity.Pollster;
import rs.serbianelection2026.backend.poll.entity.PollsterKind;
import rs.serbianelection2026.backend.poll.entity.SourceKind;
import rs.serbianelection2026.backend.poll.repository.PollRepository;
import rs.serbianelection2026.backend.poll.repository.PollsterRepository;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PollDiscoveryServiceTest {

    @Mock
    private PollRepository pollRepository;

    @Mock
    private PollsterRepository pollsterRepository;

    @Mock
    private DataImportRepository dataImportRepository;

    private PollDiscoveryService service;
    private final Pollster crta = Pollster.builder().id(1L).slug("crta").name("CRTA").kind(PollsterKind.PRIMARY).active(true).build();
    private final Pollster faktor =
            Pollster.builder().id(2L).slug("faktor-plus").name("Faktor Plus").kind(PollsterKind.SECONDARY_VIA_MEDIA).active(true).build();
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        service = new PollDiscoveryService(
                pollRepository, pollsterRepository, dataImportRepository, new PollTextMatcher(), new PollDiscoveryProperties(), JsonMapper.builder().build());
        when(dataImportRepository.save(any(DataImport.class))).thenAnswer(i -> i.getArgument(0));
        when(pollsterRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(crta, faktor));
        when(pollRepository.existsByPollster_IdAndSourceUrl(anyLong(), any())).thenReturn(false);
        when(pollRepository.findByPollster_IdAndSourceKindAndStatusAndPublishedAtBetween(anyLong(), any(), any(), any(), any()))
                .thenReturn(List.of());
    }

    private PollDiscoveryFeed feed(ImportSource source, DiscoveredItem... items) {
        return new PollDiscoveryFeed() {
            @Override
            public ImportSource source() {
                return source;
            }

            @Override
            public List<DiscoveredItem> fetch() {
                return List.of(items);
            }
        };
    }

    private DiscoveredItem media(String title, String link, Instant at) {
        return new DiscoveredItem(null, "Blic", SourceKind.SECONDARY, title, link, "<p>opis</p>", at);
    }

    @Test
    void aMediaReportOfAPollBecomesADiscoveredCandidateWithItsSnapshotAndHash() {
        DataImport run = service.discover(feed(ImportSource.POLL_MEDIA,
                media("Faktor plus: SNS bi osvojila 47,2 odsto glasova", "https://blic.rs/a", now.minus(1, ChronoUnit.DAYS))));

        ArgumentCaptor<Poll> saved = ArgumentCaptor.forClass(Poll.class);
        verify(pollRepository).save(saved.capture());
        Poll candidate = saved.getValue();
        assertThat(candidate.getStatus()).isEqualTo(PollStatus.DISCOVERED);
        assertThat(candidate.getPollster()).isSameAs(faktor);
        assertThat(candidate.getSourceKind()).isEqualTo(SourceKind.SECONDARY);
        assertThat(candidate.getSourceUrl()).isEqualTo("https://blic.rs/a");
        assertThat(candidate.getMediaSources()).contains("Blic").contains("https://blic.rs/a");
        assertThat(candidate.getSourceSnapshot()).contains("47,2 odsto").contains("opis");
        assertThat(candidate.getContentHash()).hasSize(64);
        assertThat(candidate.getResultBasis()).isNull();
        assertThat(run.getStatus()).isEqualTo(ImportStatus.SUCCESS);
        assertThat(run.getRecordsCreated()).isEqualTo(1);
    }

    @Test
    void neverInventsPollNumbers() {
        service.discover(feed(ImportSource.POLL_MEDIA, media("Faktor plus: SNS bi osvojila 47,2 odsto", "https://blic.rs/a", now)));

        ArgumentCaptor<Poll> saved = ArgumentCaptor.forClass(Poll.class);
        verify(pollRepository).save(saved.capture());
        assertThat(saved.getValue().getSampleSize()).isNull();
        assertThat(saved.getValue().getUndecidedPct()).isNull();
        assertThat(saved.getValue().getFieldworkNote()).isNull();
    }

    @Test
    void skipsItemsThatAreNotPollsAreTooOldOrMentionNoKnownPollster() {
        DataImport run = service.discover(feed(ImportSource.POLL_MEDIA,
                media("Vučić otvorio fabriku", "https://blic.rs/1", now),
                media("Faktor plus: nova anketa uskoro", "https://blic.rs/2", now),
                media("Faktor plus: SNS bi osvojila 47,2 odsto", "https://blic.rs/3", now.minus(40, ChronoUnit.DAYS)),
                media("Nepoznata agencija: SNS 47,2 odsto u anketi", "https://blic.rs/4", now)));

        verify(pollRepository, never()).save(any(Poll.class));
        assertThat(run.getRecordsFound()).isZero();
    }

    @Test
    void anAlreadyKnownLinkIsNotCreatedAgain() {
        when(pollRepository.existsByPollster_IdAndSourceUrl(2L, "https://blic.rs/a")).thenReturn(true);

        DataImport run = service.discover(feed(ImportSource.POLL_MEDIA, media("Faktor plus: SNS bi osvojila 47,2 odsto", "https://blic.rs/a", now)));

        verify(pollRepository, never()).save(any(Poll.class));
        assertThat(run.getRecordsUnchanged()).isEqualTo(1);
        assertThat(run.getRecordsCreated()).isZero();
    }

    @Test
    void aSecondMediaReportOfTheSamePollIsMergedIntoTheUntouchedCandidate() {
        Poll existing = Poll.builder().id(9L).pollster(faktor).status(PollStatus.DISCOVERED)
                .mediaSources("[{\"name\":\"Blic\",\"url\":\"https://blic.rs/a\"}]").build();
        when(pollRepository.findByPollster_IdAndSourceKindAndStatusAndPublishedAtBetween(anyLong(), any(), any(), any(), any()))
                .thenReturn(List.of(existing));

        DataImport run = service.discover(feed(ImportSource.POLL_MEDIA,
                new DiscoveredItem(null, "Informer", SourceKind.SECONDARY, "Faktor plus: SNS bi osvojila 47,2 odsto", "https://informer.rs/b", "", now)));

        verify(pollRepository).save(existing);
        assertThat(existing.getMediaSources()).contains("Blic").contains("Informer").contains("https://informer.rs/b");
        assertThat(run.getRecordsUpdated()).isEqualTo(1);
        assertThat(run.getRecordsCreated()).isZero();
    }

    @Test
    void aPollstersOwnFeedNeedsNoPercentageAndIsPrimary() {
        service.discover(feed(ImportSource.POLL_CRTA,
                new DiscoveredItem("crta", null, SourceKind.PRIMARY, "Ново истраживање јавног мњења", "https://crta.rs/x", "", now)));

        ArgumentCaptor<Poll> saved = ArgumentCaptor.forClass(Poll.class);
        verify(pollRepository).save(saved.capture());
        assertThat(saved.getValue().getPollster()).isSameAs(crta);
        assertThat(saved.getValue().getSourceKind()).isEqualTo(SourceKind.PRIMARY);
        assertThat(saved.getValue().getMediaSources()).isNull();
    }

    @Test
    void aFailingFeedIsRecordedAsFailedInsteadOfThrowing() {
        PollDiscoveryFeed broken = new PollDiscoveryFeed() {
            @Override
            public ImportSource source() {
                return ImportSource.POLL_CESID;
            }

            @Override
            public List<DiscoveredItem> fetch() {
                throw new IllegalStateException("feed down");
            }
        };

        DataImport run = service.discover(broken);

        assertThat(run.getStatus()).isEqualTo(ImportStatus.FAILED);
        assertThat(run.getErrorMessage()).isEqualTo("feed down");
        verify(pollRepository, never()).save(any(Poll.class));
    }

    @Test
    void ignoresAnUnknownPollsterSlugFromAFeed() {
        DataImport run = service.discover(feed(ImportSource.POLL_CRTA,
                new DiscoveredItem("nobody", null, SourceKind.PRIMARY, "Истраживање јавног мњења", "https://x/y", "", now)));

        verify(pollRepository, never()).save(any(Poll.class));
        assertThat(run.getRecordsFound()).isZero();
        assertThat(Optional.ofNullable(run.getErrorMessage())).isEmpty();
    }
}
