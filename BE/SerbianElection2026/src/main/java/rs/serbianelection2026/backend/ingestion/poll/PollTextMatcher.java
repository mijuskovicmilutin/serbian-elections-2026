package rs.serbianelection2026.backend.ingestion.poll;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.poll.entity.Pollster;

/**
 * Decides whether a headline looks like it reports a poll. Deliberately loose: a false positive costs
 * the reviewer one click on "reject", while a missed poll would never reach review at all. Text is
 * compared in plain lowercase Latin so Cyrillic and Latin headlines match the same way.
 */
@Component
public class PollTextMatcher {

    /** Headlines rarely say "istraživanje"; they say who "bi osvojila" or "bi glasalo" how much, hence the phrases. */
    private static final List<String> POLL_WORDS = List.of(
            "istrazivanj", "anket", "rejting", "javnog mnjenja", "javno mnjenje", "ispitanik", "bi osvojil", "bi glasal", "cenzus");

    private static final Pattern PERCENTAGE = Pattern.compile("\\d+([.,]\\d+)?\\s*(%|odsto|posto|procent)");

    private static final Map<Character, String> CYRILLIC = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"), Map.entry('д', "d"),
            Map.entry('ђ', "dj"), Map.entry('е', "e"), Map.entry('ж', "z"), Map.entry('з', "z"), Map.entry('и', "i"),
            Map.entry('ј', "j"), Map.entry('к', "k"), Map.entry('л', "l"), Map.entry('љ', "lj"), Map.entry('м', "m"),
            Map.entry('н', "n"), Map.entry('њ', "nj"), Map.entry('о', "o"), Map.entry('п', "p"), Map.entry('р', "r"),
            Map.entry('с', "s"), Map.entry('т', "t"), Map.entry('ћ', "c"), Map.entry('у', "u"), Map.entry('ф', "f"),
            Map.entry('х', "h"), Map.entry('ц', "c"), Map.entry('ч', "c"), Map.entry('џ', "dz"), Map.entry('ш', "s"));

    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder latin = new StringBuilder(text.length());
        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            latin.append(CYRILLIC.getOrDefault(c, String.valueOf(c)));
        }
        String stripped = Normalizer.normalize(latin, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return stripped.replace("đ", "dj");
    }

    public boolean mentionsPoll(String normalizedText) {
        return POLL_WORDS.stream().anyMatch(normalizedText::contains);
    }

    public boolean hasPercentage(String normalizedText) {
        return PERCENTAGE.matcher(normalizedText).find();
    }

    /**
     * The first active pollster whose name appears as a whole word in the text. Serbian declines names
     * ("Crte", "Faktor plusa"), so up to two extra letters are allowed after the name's stem.
     */
    public Optional<Pollster> findPollster(String normalizedText, Collection<Pollster> pollsters) {
        for (Pollster pollster : pollsters) {
            String alias = normalize(pollster.getName());
            String stem = alias.length() > 3 && alias.endsWith("a") ? alias.substring(0, alias.length() - 1) : alias;
            if (Pattern.compile("(^|[^a-z0-9])" + Pattern.quote(stem) + "[a-z]{0,2}([^a-z0-9]|$)")
                    .matcher(normalizedText)
                    .find()) {
                return Optional.of(pollster);
            }
        }
        return Optional.empty();
    }
}
