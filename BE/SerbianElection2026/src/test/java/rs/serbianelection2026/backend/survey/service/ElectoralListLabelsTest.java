package rs.serbianelection2026.backend.survey.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ElectoralListLabelsTest {

    @Test
    void stripsRiksNumberAndTitlePrefix() {
        assertThat(ElectoralListLabels.displayName("4. ИЗБОРНА ЛИСТА РАСИМ ЉАЈИЋ – ЉУДСКИ (СДП – ЦДП)"))
                .isEqualTo("РАСИМ ЉАЈИЋ – ЉУДСКИ (СДП – ЦДП)");
        assertThat(ElectoralListLabels.displayName("8.ИЗБОРНА ЛИСТА АУТЕНТИЧНА ДЕСНИЦА")).isEqualTo("АУТЕНТИЧНА ДЕСНИЦА");
        assertThat(ElectoralListLabels.displayName("9. ИЗБОРНА ЛИСТА - ИЗБОР НАРОДА")).isEqualTo("ИЗБОР НАРОДА");
    }

    @Test
    void leavesOtherNamesAlone() {
        assertThat(ElectoralListLabels.displayName("Some list")).isEqualTo("Some list");
    }
}
