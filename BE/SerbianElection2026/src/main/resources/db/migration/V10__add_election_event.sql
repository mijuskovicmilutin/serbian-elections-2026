CREATE TABLE election_event (
    id           BIGSERIAL PRIMARY KEY,
    election_id  BIGINT        NOT NULL REFERENCES election (id),
    type         VARCHAR(30)   NOT NULL,
    title        VARCHAR(255)  NOT NULL,
    description  TEXT,
    event_date   DATE          NOT NULL,
    source_url   VARCHAR(2048),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    modified_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_election_event_election_date ON election_event (election_id, event_date);

-- Key dates as reported by Danas (calling of the elections, 2026-09-09) and Mondo "EUpravo zato" (deadlines,
-- 2026-09-09). Election day is taken from the election row itself so the two can never disagree.
INSERT INTO election_event (election_id, type, title, description, event_date, source_url)
SELECT e.id, v.type, v.title, v.description, v.event_date::date, v.source_url
FROM (SELECT id FROM election ORDER BY election_date LIMIT 1) e
CROSS JOIN (VALUES
    ('CALLED', 'Расписани ванредни парламентарни избори',
     'Председник је 9. септембра 2026. распустио Народну скупштину и расписао изборе за 25. октобар. Изборни процес траје 45 дана.',
     '2026-09-09', 'https://www.danas.rs/vesti/politika/vucic-izbori-25-oktobar-raspisivanje/'),
    ('DEADLINE', 'Рок за пријаву гласања у иностранству',
     'Крајњи рок за подношење захтева за гласање у иностранству преко дипломатско-конзуларних представништава.',
     '2026-10-03', 'https://eupravozato.mondo.rs/politika-prosirenja/izbori/a20305/izbori-u-srbiji-2026-ovo-su-rokovi-za-izborne-liste-i-kako-se-prijaviti-za-glasanje-u-inostranstvu.html'),
    ('DEADLINE', 'Рок за подношење изборних листа',
     'Изборне листе се подносе са најмање 10.000 потписа бирача. Бирач може да подржи више листа.',
     '2026-10-04', 'https://eupravozato.mondo.rs/politika-prosirenja/izbori/a20305/izbori-u-srbiji-2026-ovo-su-rokovi-za-izborne-liste-i-kako-se-prijaviti-za-glasanje-u-inostranstvu.html'),
    ('DEADLINE', 'Рок за пријаву домаћих посматрача', NULL,
     '2026-10-17', 'https://eupravozato.mondo.rs/politika-prosirenja/izbori/a20305/izbori-u-srbiji-2026-ovo-su-rokovi-za-izborne-liste-i-kako-se-prijaviti-za-glasanje-u-inostranstvu.html')
) AS v(type, title, description, event_date, source_url);

INSERT INTO election_event (election_id, type, title, description, event_date, source_url)
SELECT id, 'ELECTION_DAY', 'Дан гласања', 'Бирачка места су отворена од 07 до 20 часова.', election_date,
       'https://www.danas.rs/vesti/politika/vucic-izbori-25-oktobar-raspisivanje/'
FROM election ORDER BY election_date LIMIT 1;
