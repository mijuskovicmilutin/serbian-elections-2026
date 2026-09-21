package rs.serbianelection2026.backend.ingestion.poll;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import rs.serbianelection2026.backend.poll.entity.Pollster;
import rs.serbianelection2026.backend.poll.entity.PollsterKind;

class PollTextMatcherTest {

    private final PollTextMatcher matcher = new PollTextMatcher();

    private final List<Pollster> pollsters = List.of(
            pollster("cesid", "CeSID"), pollster("crta", "CRTA"), pollster("faktor-plus", "Faktor Plus"));

    private Pollster pollster(String slug, String name) {
        return Pollster.builder().slug(slug).name(name).kind(PollsterKind.PRIMARY).active(true).build();
    }

    @Test
    void cyrillicAndLatinDiacriticsCollapseToPlainLowercaseLatin() {
        assertThat(matcher.normalize("Истраживање: СНС бише освојила 47,2 одсто")).isEqualTo("istrazivanje: sns bise osvojila 47,2 odsto");
        assertThat(matcher.normalize("Đorđe ćuti, čuje šta žele")).isEqualTo("djordje cuti, cuje sta zele");
        assertThat(matcher.normalize(null)).isEmpty();
    }

    @Test
    void recognisesPollWordsInBothScripts() {
        assertThat(matcher.mentionsPoll(matcher.normalize("Novo istraživanje javnog mnjenja"))).isTrue();
        assertThat(matcher.mentionsPoll(matcher.normalize("Ново истраживање: студентска листа води"))).isTrue();
        assertThat(matcher.mentionsPoll(matcher.normalize("Rejting stranaka nakon izbora"))).isTrue();
        assertThat(matcher.mentionsPoll(matcher.normalize("Vučić otvorio novu fabriku"))).isFalse();
    }

    @Test
    void percentagesAreRecognisedInTheWaysHeadlinesWriteThem() {
        assertThat(matcher.hasPercentage(matcher.normalize("SNS 47,2 odsto"))).isTrue();
        assertThat(matcher.hasPercentage(matcher.normalize("СНС 47,2%"))).isTrue();
        assertThat(matcher.hasPercentage(matcher.normalize("liste sa 35.7 posto glasova"))).isTrue();
        assertThat(matcher.hasPercentage(matcher.normalize("istraživanje o stavovima građana"))).isFalse();
    }

    @Test
    void findsThePollsterByNameInEitherScript() {
        assertThat(matcher.findPollster(matcher.normalize("Faktor plus: SNS bi osvojila 47,2 odsto"), pollsters))
                .get().extracting(Pollster::getSlug).isEqualTo("faktor-plus");
        assertThat(matcher.findPollster(matcher.normalize("Фактор плус: нови резултати"), pollsters))
                .get().extracting(Pollster::getSlug).isEqualTo("faktor-plus");
        assertThat(matcher.findPollster(matcher.normalize("Istraživanje CRTA: studentska lista vodi"), pollsters))
                .get().extracting(Pollster::getSlug).isEqualTo("crta");
    }

    @Test
    void declinedNamesStillMatch() {
        assertThat(matcher.findPollster(matcher.normalize("Istraživanje Crte: studentska lista vodi"), pollsters))
                .get().extracting(Pollster::getSlug).isEqualTo("crta");
        assertThat(matcher.findPollster(matcher.normalize("Novo istraživanje Faktor plusa"), pollsters))
                .get().extracting(Pollster::getSlug).isEqualTo("faktor-plus");
        assertThat(matcher.findPollster(matcher.normalize("Prema CeSID-u, izlaznost je 30 odsto"), pollsters))
                .get().extracting(Pollster::getSlug).isEqualTo("cesid");
    }

    @Test
    void headlinesWithoutTheWordPollStillCountWhenTheyStateWhoWouldWin() {
        assertThat(matcher.mentionsPoll(matcher.normalize("Faktor plus: Da se izbori održavaju u nedelju SNS bi osvojila 47,2 odsto glasova"))).isTrue();
        assertThat(matcher.mentionsPoll(matcher.normalize("Koliko lista bi prešlo cenzus"))).isTrue();
    }

    @Test
    void aPollsterNameInsideAnotherWordIsNotAMatch() {
        assertThat(matcher.findPollster(matcher.normalize("granicrta istrazivanje 50 odsto"), pollsters)).isEmpty();
        assertThat(matcher.findPollster(matcher.normalize("Nema nikakvog pomena"), pollsters)).isEmpty();
    }
}
