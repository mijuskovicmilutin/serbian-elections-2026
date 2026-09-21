package rs.serbianelection2026.backend.ingestion.rik;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rs.serbianelection2026.backend.ingestion.rik.dto.NormalizedElectoralList;
import rs.serbianelection2026.backend.ingestion.rik.dto.RikDocumentRecord;
import rs.serbianelection2026.backend.ingestion.rik.dto.RikDocumentsResponse;
import tools.jackson.databind.json.JsonMapper;

class RikParserTest {

    private RikParser rikParser;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        RikProperties properties = new RikProperties();
        properties.setBaseUrl("https://www.rik.parlament.gov.rs");
        rikParser = new RikParser(properties);
    }

    @Test
    void parsesRealRikFixtureIntoNormalizedElectoralLists() throws IOException {
        RikDocumentsResponse response = jsonMapper.readValue(loadFixture(), RikDocumentsResponse.class);

        List<NormalizedElectoralList> result = rikParser.parse(response.records());

        assertThat(result).hasSize(5);

        NormalizedElectoralList first = result.get(0);
        assertThat(first.externalId()).isEqualTo("792");
        assertThat(first.ballotNumber()).isEqualTo(1);
        assertThat(first.name()).isEqualTo("1. ИЗБОРНА ЛИСТА АЛЕКСАНДАР ВУЧИЋ – УЈЕДИЊЕНА СРБИЈА");
        assertThat(first.sourceUrl()).isEqualTo(
                "https://www.rik.parlament.gov.rs/extfile/sr/files/additionalDocuments/680/792/1.%20UJEDINJENA%20SRBIJA%20ALEKSANDAR%20VUCIC%20%20-%20za%20sajt.docx");
        assertThat(first.publishedAt()).isEqualTo(Instant.ofEpochSecond(1789308660L));

        NormalizedElectoralList fifth = result.get(4);
        assertThat(fifth.externalId()).isEqualTo("005");
        assertThat(fifth.ballotNumber()).isEqualTo(5);
    }

    private RikDocumentRecord record(String rowNumber, String title) {
        return new RikDocumentRecord(
                rowNumber,
                title,
                "1789308660",
                List.of("<a href=\"/extfile/sr/files/additionalDocuments/680/792/x.docx\">x</a>"));
    }

    @Test
    void takesTheListNumberFromTheTitleNotFromTheRowPosition() {
        // RIK's "number" column is the row position: a newly published list can arrive first and shift all others.
        List<NormalizedElectoralList> result = rikParser.parse(List.of(
                record("1.", "10. ИЗБОРНА ЛИСТА - ЕВРОПСКА СРБИЈА – МАРИНИКА ТЕПИЋ"),
                record("2.", "1. ИЗБОРНА ЛИСТА АЛЕКСАНДАР ВУЧИЋ – УЈЕДИЊЕНА СРБИЈА"),
                record("9.", "8.ИЗБОРНА ЛИСТА АУТЕНТИЧНА ДЕСНИЦА – ДР МИЛОШ ЈОВАНОВИЋ")));

        assertThat(result).extracting(NormalizedElectoralList::ballotNumber).containsExactly(10, 1, 8);
    }

    @Test
    void aTitleWithoutANumberGetsNoBallotNumberInsteadOfAGuessedOne() {
        List<NormalizedElectoralList> result = rikParser.parse(List.of(record("3.", "ИЗБОРНА ЛИСТА БЕЗ БРОЈА")));

        assertThat(result.get(0).ballotNumber()).isNull();
    }

    private String loadFixture() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/rik/electoral-lists-response.json")) {
            if (in == null) {
                throw new IllegalStateException("Fixture not found on classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
