-- Visitor survey (PLAN.md, V6): anonymous, opt-in, weighted to the census. No personal data is stored:
-- a response holds only its answers and the day it was sent; the per-network duplicate guard lives in its own
-- table (survey_dedupe) that has no link to any response and no timestamps.

CREATE TABLE survey (
    id                      BIGSERIAL PRIMARY KEY,
    slug                    VARCHAR(80)  NOT NULL,
    title                   VARCHAR(255) NOT NULL,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    opens_at                TIMESTAMPTZ  NOT NULL,
    closes_at               TIMESTAMPTZ  NOT NULL,
    min_weighted_responses  INTEGER      NOT NULL DEFAULT 50,
    max_per_network         INTEGER      NOT NULL DEFAULT 3,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_survey_slug ON survey (slug);

CREATE TABLE survey_question (
    id         BIGSERIAL PRIMARY KEY,
    survey_id  BIGINT       NOT NULL REFERENCES survey (id) ON DELETE CASCADE,
    position   INTEGER      NOT NULL,
    role       VARCHAR(30)  NOT NULL,
    text       VARCHAR(500) NOT NULL
);

CREATE UNIQUE INDEX idx_survey_question_position ON survey_question (survey_id, position);
CREATE UNIQUE INDEX idx_survey_question_role ON survey_question (survey_id, role);

CREATE TABLE survey_option (
    id                 BIGSERIAL PRIMARY KEY,
    question_id        BIGINT       NOT NULL REFERENCES survey_question (id) ON DELETE CASCADE,
    position           INTEGER      NOT NULL,
    code               VARCHAR(50)  NOT NULL,
    label              VARCHAR(500) NOT NULL,
    kind               VARCHAR(20)  NOT NULL DEFAULT 'CHOICE',
    electoral_list_id  BIGINT REFERENCES electoral_list (id),
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    added_on           DATE
);

CREATE UNIQUE INDEX idx_survey_option_code ON survey_option (question_id, code);
CREATE INDEX idx_survey_option_list ON survey_option (electoral_list_id);

CREATE TABLE survey_response (
    id            BIGSERIAL PRIMARY KEY,
    survey_id     BIGINT  NOT NULL REFERENCES survey (id) ON DELETE CASCADE,
    submitted_on  DATE    NOT NULL,
    excluded      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_survey_response_survey ON survey_response (survey_id);

CREATE TABLE survey_answer (
    response_id  BIGINT NOT NULL REFERENCES survey_response (id) ON DELETE CASCADE,
    question_id  BIGINT NOT NULL REFERENCES survey_question (id) ON DELETE CASCADE,
    option_id    BIGINT NOT NULL REFERENCES survey_option (id) ON DELETE CASCADE,
    PRIMARY KEY (response_id, question_id)
);

-- One row per keyed hash of a network (see NetworkHasher). Deliberately no id of a response and no timestamp,
-- so a hash cannot be tied to an answer. Removed when the survey closes.
CREATE TABLE survey_dedupe (
    survey_id  BIGINT      NOT NULL REFERENCES survey (id) ON DELETE CASCADE,
    ip_hash    VARCHAR(64) NOT NULL,
    count      SMALLINT    NOT NULL DEFAULT 1,
    PRIMARY KEY (survey_id, ip_hash)
);

CREATE TABLE population_margin (
    id              BIGSERIAL PRIMARY KEY,
    dimension       VARCHAR(20)   NOT NULL,
    category_code   VARCHAR(50)   NOT NULL,
    population      BIGINT        NOT NULL,
    source          VARCHAR(255)  NOT NULL,
    source_url      VARCHAR(2048) NOT NULL,
    reference_year  INTEGER       NOT NULL
);

CREATE UNIQUE INDEX idx_population_margin ON population_margin (dimension, category_code);

CREATE TABLE survey_result_snapshot (
    id              BIGSERIAL PRIMARY KEY,
    survey_id       BIGINT      NOT NULL REFERENCES survey (id) ON DELETE CASCADE,
    computed_at     TIMESTAMPTZ NOT NULL,
    response_count  INTEGER     NOT NULL,
    payload         TEXT        NOT NULL
);

CREATE INDEX idx_survey_snapshot_latest ON survey_result_snapshot (survey_id, computed_at DESC);

-- Population 18+ by category, Popis 2022 (RZS), Knjiga 2 "Starost i pol"; extracted 2026-09-23 (see PLAN.md).
INSERT INTO population_margin (dimension, category_code, population, source, source_url, reference_year)
VALUES ('AGE', 'AGE_18_29', 848012, 'RZS, Popis 2022, Knjiga 2 Starost i pol', 'https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf', 2022),
       ('AGE', 'AGE_30_44', 1325378, 'RZS, Popis 2022, Knjiga 2 Starost i pol', 'https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf', 2022),
       ('AGE', 'AGE_45_59', 1377869, 'RZS, Popis 2022, Knjiga 2 Starost i pol', 'https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf', 2022),
       ('AGE', 'AGE_60_PLUS', 1940761, 'RZS, Popis 2022, Knjiga 2 Starost i pol', 'https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf', 2022),
       ('SEX', 'SEX_MALE', 2637451, 'RZS, Popis 2022, Knjiga 2 Starost i pol', 'https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf', 2022),
       ('SEX', 'SEX_FEMALE', 2854569, 'RZS, Popis 2022, Knjiga 2 Starost i pol', 'https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf', 2022),
       ('REGION', 'REGION_BELGRADE', 1380388, 'RZS, Popis 2022, Knjiga 2 Starost i pol', 'https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf', 2022),
       ('REGION', 'REGION_VOJVODINA', 1432972, 'RZS, Popis 2022, Knjiga 2 Starost i pol', 'https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf', 2022),
       ('REGION', 'REGION_SUMADIJA_WEST', 1502710, 'RZS, Popis 2022, Knjiga 2 Starost i pol', 'https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf', 2022),
       ('REGION', 'REGION_SOUTH_EAST', 1175950, 'RZS, Popis 2022, Knjiga 2 Starost i pol', 'https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf', 2022),
       ('SETTLEMENT', 'SETTLEMENT_URBAN', 3382634, 'RZS, Popis 2022, Knjiga 2 Starost i pol', 'https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf', 2022),
       ('SETTLEMENT', 'SETTLEMENT_OTHER', 2109386, 'RZS, Popis 2022, Knjiga 2 Starost i pol', 'https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf', 2022);

-- The survey itself: open immediately, closes when the election silence starts (2026-10-23 00:00 Belgrade time,
-- to be confirmed with legal review, see PLAN.md).
INSERT INTO survey (slug, title, status, opens_at, closes_at)
VALUES ('posetioci-2026', 'Анкета посетилаца портала', 'OPEN', '2026-09-23T00:00:00+02:00', '2026-10-23T00:00:00+02:00');

INSERT INTO survey_question (survey_id, position, role, text)
SELECT s.id, q.position, q.role, q.text
FROM survey s,
     (VALUES (1, 'VOTE_INTENTION', 'За коју листу планирате да гласате на изборима 25. октобра?'),
             (2, 'TURNOUT', 'Колико је вероватно да ћете изаћи на изборе?'),
             (3, 'AGE', 'Колико имате година?'),
             (4, 'SEX', 'Ваш пол'),
             (5, 'REGION', 'У ком делу Србије живите?'),
             (6, 'SETTLEMENT', 'Где живите?')) AS q (position, role, text)
WHERE s.slug = 'posetioci-2026';

-- Options of question 1 that are not lists (the lists come from electoral_list, see SurveyOptionSyncService).
INSERT INTO survey_option (question_id, position, code, label, kind)
SELECT qu.id, o.position, o.code, o.label, o.kind
FROM survey_question qu
JOIN survey s ON s.id = qu.survey_id AND s.slug = 'posetioci-2026'
JOIN (VALUES ('VOTE_INTENTION', 1001, 'UNDECIDED', 'Нисам одлучан/на', 'UNDECIDED'),
             ('VOTE_INTENTION', 1002, 'WONT_VOTE', 'Нећу гласати', 'WONT_VOTE'),
             ('VOTE_INTENTION', 1003, 'NO_ANSWER', 'Не желим да одговорим', 'NO_ANSWER'),
             ('TURNOUT', 1, 'SURE_YES', 'Сигурно ћу изаћи', 'CHOICE'),
             ('TURNOUT', 2, 'PROBABLY_YES', 'Вероватно ћу изаћи', 'CHOICE'),
             ('TURNOUT', 3, 'PROBABLY_NO', 'Вероватно нећу изаћи', 'CHOICE'),
             ('TURNOUT', 4, 'SURE_NO', 'Сигурно нећу изаћи', 'CHOICE'),
             ('AGE', 1, 'AGE_18_29', '18–29', 'CHOICE'),
             ('AGE', 2, 'AGE_30_44', '30–44', 'CHOICE'),
             ('AGE', 3, 'AGE_45_59', '45–59', 'CHOICE'),
             ('AGE', 4, 'AGE_60_PLUS', '60+', 'CHOICE'),
             ('SEX', 1, 'SEX_MALE', 'Мушко', 'CHOICE'),
             ('SEX', 2, 'SEX_FEMALE', 'Женско', 'CHOICE'),
             ('REGION', 1, 'REGION_BELGRADE', 'Београд', 'CHOICE'),
             ('REGION', 2, 'REGION_VOJVODINA', 'Војводина', 'CHOICE'),
             ('REGION', 3, 'REGION_SUMADIJA_WEST', 'Шумадија и Западна Србија', 'CHOICE'),
             ('REGION', 4, 'REGION_SOUTH_EAST', 'Јужна и Источна Србија', 'CHOICE'),
             ('SETTLEMENT', 1, 'SETTLEMENT_URBAN', 'Град или варош', 'CHOICE'),
             ('SETTLEMENT', 2, 'SETTLEMENT_OTHER', 'Село', 'CHOICE')) AS o (role, position, code, label, kind)
     ON o.role = qu.role;
