package rs.serbianelection2026.backend.poll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.serbianelection2026.backend.common.exception.NotFoundException;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollResult;
import rs.serbianelection2026.backend.poll.entity.PollStatus;
import rs.serbianelection2026.backend.poll.repository.PollRepository;
import rs.serbianelection2026.backend.poll.repository.PollResultRepository;
import rs.serbianelection2026.backend.poll.repository.PollsterRepository;
import rs.serbianelection2026.backend.poll.service.PollService;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock
    private PollRepository pollRepository;

    @Mock
    private PollResultRepository pollResultRepository;

    @Mock
    private PollsterRepository pollsterRepository;

    @InjectMocks
    private PollService pollService;

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void listingAlwaysAsksOnlyForApprovedPolls() {
        when(pollRepository.findByStatusOrderByPublishedAtDesc(eq(PollStatus.APPROVED), any()))
                .thenReturn(Page.empty());

        pollService.getApprovedPolls(null, pageable);

        verify(pollRepository).findByStatusOrderByPublishedAtDesc(PollStatus.APPROVED, pageable);
    }

    @Test
    void listingByPollsterAlsoRestrictsToApproved() {
        when(pollsterRepository.existsBySlugAndActiveTrue("crta")).thenReturn(true);
        when(pollRepository.findByStatusAndPollster_SlugOrderByPublishedAtDesc(eq(PollStatus.APPROVED), eq("crta"), any()))
                .thenReturn(new PageImpl<>(List.of()));

        pollService.getApprovedPolls("crta", pageable);

        verify(pollRepository).findByStatusAndPollster_SlugOrderByPublishedAtDesc(PollStatus.APPROVED, "crta", pageable);
    }

    @Test
    void unknownPollsterIsNotFoundAndNoQueryIsRun() {
        when(pollsterRepository.existsBySlugAndActiveTrue("nope")).thenReturn(false);

        assertThatThrownBy(() -> pollService.getApprovedPolls("nope", pageable))
                .isInstanceOf(NotFoundException.class);

        verify(pollRepository, never()).findByStatusOrderByPublishedAtDesc(any(), any());
    }

    @Test
    void aPollThatIsNotApprovedLooksLikeAMissingOne() {
        when(pollRepository.findByIdAndStatus(5L, PollStatus.APPROVED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.getApprovedPoll(5L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void resultsAreGroupedByPollInOneQuery() {
        Poll a = Poll.builder().id(1L).build();
        Poll b = Poll.builder().id(2L).build();
        PollResult r1 = PollResult.builder().poll(a).rawOptionName("A1").percentage(BigDecimal.ONE).displayOrder(1).build();
        PollResult r2 = PollResult.builder().poll(b).rawOptionName("B1").percentage(BigDecimal.ONE).displayOrder(1).build();
        PollResult r3 = PollResult.builder().poll(a).rawOptionName("A2").percentage(BigDecimal.ONE).displayOrder(2).build();
        when(pollResultRepository.findByPoll_IdInOrderByDisplayOrderAsc(List.of(1L, 2L))).thenReturn(List.of(r1, r2, r3));

        Map<Long, List<PollResult>> grouped = pollService.getResultsByPollId(List.of(a, b));

        assertThat(grouped.get(1L)).extracting("rawOptionName").containsExactly("A1", "A2");
        assertThat(grouped.get(2L)).extracting("rawOptionName").containsExactly("B1");
    }

    @Test
    void noPollsMeansNoQuery() {
        assertThat(pollService.getResultsByPollId(List.of())).isEmpty();

        verify(pollResultRepository, never()).findByPoll_IdInOrderByDisplayOrderAsc(any());
    }
}
