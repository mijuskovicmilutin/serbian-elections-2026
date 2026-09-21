package rs.serbianelection2026.backend.election.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.serbianelection2026.backend.common.exception.NotFoundException;
import rs.serbianelection2026.backend.election.entity.Election;
import rs.serbianelection2026.backend.election.entity.ElectionEvent;
import rs.serbianelection2026.backend.election.entity.ElectoralList;
import rs.serbianelection2026.backend.election.entity.EventType;
import rs.serbianelection2026.backend.election.repository.ElectionEventRepository;
import rs.serbianelection2026.backend.election.repository.ElectionRepository;
import rs.serbianelection2026.backend.election.repository.ElectoralListRepository;

@ExtendWith(MockitoExtension.class)
class ElectionServiceTest {

    @Mock
    private ElectionRepository electionRepository;

    @Mock
    private ElectoralListRepository electoralListRepository;

    @Mock
    private ElectionEventRepository electionEventRepository;

    private ElectionService electionService;

    @BeforeEach
    void setUp() {
        electionService = new ElectionService(electionRepository, electoralListRepository, electionEventRepository);
    }

    @Test
    void getCurrentElectionReturnsElectionWhenPresent() {
        Election election = Election.builder().id(1L).name("Парламентарни избори 2026")
                .electionDate(LocalDate.of(2026, 10, 25)).build();
        when(electionRepository.findFirstByOrderByElectionDateAsc()).thenReturn(Optional.of(election));

        Election result = electionService.getCurrentElection();

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Парламентарни избори 2026");
    }

    @Test
    void getCurrentElectionThrowsNotFoundWhenNoneExists() {
        when(electionRepository.findFirstByOrderByElectionDateAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> electionService.getCurrentElection())
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getCurrentElectoralListsReturnsListsForCurrentElection() {
        Election election = Election.builder().id(1L).name("Парламентарни избори 2026")
                .electionDate(LocalDate.of(2026, 10, 25)).build();
        ElectoralList list = ElectoralList.builder().id(10L).election(election).build();
        when(electionRepository.findFirstByOrderByElectionDateAsc()).thenReturn(Optional.of(election));
        when(electoralListRepository.findByElection_IdOrderByBallotNumberAsc(1L)).thenReturn(List.of(list));

        List<ElectoralList> result = electionService.getCurrentElectoralLists();

        assertThat(result).containsExactly(list);
    }

    @Test
    void getCurrentElectoralListsThrowsNotFoundWithoutQueryingListsWhenNoElection() {
        when(electionRepository.findFirstByOrderByElectionDateAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> electionService.getCurrentElectoralLists())
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(electoralListRepository);
    }

    @Test
    void getCurrentEventsReturnsTheCurrentElectionsEventsInDateOrder() {
        Election election = Election.builder().id(1L).name("e").electionDate(LocalDate.of(2026, 10, 25)).build();
        when(electionRepository.findFirstByOrderByElectionDateAsc()).thenReturn(Optional.of(election));
        ElectionEvent called = ElectionEvent.builder().id(1L).type(EventType.CALLED).title("called")
                .eventDate(LocalDate.of(2026, 9, 9)).build();
        when(electionEventRepository.findByElection_IdOrderByEventDateAscIdAsc(1L)).thenReturn(List.of(called));

        List<ElectionEvent> events = electionService.getCurrentEvents();

        assertThat(events).containsExactly(called);
    }

    @Test
    void getCurrentEventsFailsClearlyWhenNoElectionIsConfigured() {
        when(electionRepository.findFirstByOrderByElectionDateAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> electionService.getCurrentEvents()).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(electionEventRepository);
    }
}
