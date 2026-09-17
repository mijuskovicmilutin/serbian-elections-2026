# Serbia Elections 2026 — Project Plan

Status: draft plan, reviewed 2026-09-16. Implementacija ide milestone po milestone (ne generisati sve odjednom).

## 1. Cilj

Javni, neutralni informacioni portal za parlamentarne izbore u Srbiji 2026 (izbori raspisani za 25. oktobar 2026). Sajt agregira i strukturirano prikazuje podatke iz jasno označenih izvora — ne proizvodi političke procene niti rangira opcije.

Izvori: RIK (izborni podaci, izborne liste), političke rubrike medija (vesti), agencije za istraživanje javnog mnjenja, prediction-market podaci (Polymarket i sl., jasno odvojeni od anketa).

Za svaki podatak čuvamo i prikazujemo izvor, originalni URL i vreme poslednjeg ažuriranja.

## 2. Arhitektura

FE (Next.js) → BE (Spring Boot) → DB (PostgreSQL) / external sources.

Next.js nikad direktno ne komunicira sa bazom, RIK-om ili medijima. Spring Boot je vlasnik podataka i integracija.

## 3. Repository

Jedan monorepo sa `BE/` (Spring Boot) i `FE/` (Next.js), dve nezavisne aplikacije, `docker-compose.yml` za lokalni Postgres.

## 4. Backend stack

Java 21 LTS, Spring Boot 4.1.x, Spring Web, Spring Data JPA, Hibernate, PostgreSQL, Flyway, Jakarta Validation, Jsoup, Jackson, Spring Scheduler, Actuator, Maven, JUnit 5, Mockito, Testcontainers.

Namerno bez: Kafka, Redis, Elasticsearch, Kubernetes, microservices.

## 5. Frontend stack

Next.js 16 (App Router), React, TypeScript, Tailwind CSS, shadcn/ui, Recharts, Lucide Icons. Bez plain JS.

## 6. Dev environment

IntelliJ za backend, VS Code za frontend. Lokalno: Next.js :3000, Spring Boot :8080, PostgreSQL :5432 (Docker Compose za bazu).

## V1 — Election Core

Odgovara na: kada su izbori? koje liste učestvuju?

- Homepage: countdown (računat na FE, datum dolazi iz backend election objekta) + lista proglašenih izbornih lista sa statusom, izvorom i vremenom ažuriranja.
- **Pre RIK integracije: proveriti robots.txt za `rik.parlament.gov.rs`** (isti razlog kao provera ToS/robots.txt za medije pre V2 — scraping mora biti u skladu sa pravilima izvora).
- RIK integracija: Spring Scheduler na 15 min → RikImportJob → RikClient (fetch) → RikParser (Jsoup) → normalizacija → PostgreSQL. Nema scraping-a on-demand po poseti korisnika.
- Backend struktura: `election/{controller,service,repository,entity,dto,mapper}` + `ingestion/rik/{RikClient,RikParser,RikImportService,RikImportJob}` — fetch/parse/normalize/save namerno odvojeni radi testabilnosti.
- Entiteti: `election`, `electoral_list` (status: SUBMITTED/PROCLAIMED/REJECTED/WITHDRAWN, samo kad je pouzdano iz izvora), `data_import` (audit log scraper run-ova: started_at, finished_at, status, records_found/created/updated, error_message).
- REST: `GET /api/v1/elections/current`, `GET /api/v1/elections/current/lists`.
- Mora imati: responsive (desktop/tablet/mobile), loading/empty/error states, basic SEO, favicon, source links, "last updated", `/about`, `/sources` (objašnjenje izvora + da portal nije zvanični RIK sajt).

## V2 — News Aggregator

Izvori (redosled preference API > RSS > scraping): N1, Nova, Blic, Informer.

**Provereno 2026-09-17 (robots.txt + RSS dostupnost):**
- **N1** (`n1info.rs`): robots.txt dozvoljava (samo `/admin/` i par API putanja blokirano). RSS na `/feed/` — naslov, link, opis, `media:content` slika, kategorije. Nema posebnog RSS-a samo za politiku, filtrirati po kategoriji iz feed-a.
- **Nova** (`nova.rs`): ista platforma kao N1 (United Media), isti robots.txt oblik, RSS na `/feed/`.
- **Blic** (`blic.rs`): robots.txt dozvoljava sve osim `?strana=komentari`. Ima RSS po rubrici: `/rss/Vesti/Politika` — direktno filtrirano na politiku.
- **Informer** (`informer.rs`): robots.txt potpuno otvoren (`Disallow:` prazno). Ima RSS po rubrici: `/rss/politika` — direktno filtrirano na politiku.

Zaključak: **svi izvori imaju RSS i dozvoljavaju crawl** — nema potrebe za HTML scraping-om u V2, `NewsProvider` implementacije parsiraju RSS XML direktno preko Jsoup-a (xmlParser mod), bez dodatne biblioteke. Blic/Informer imaju gotov politika-only feed. Nova ima poseban `/vesti/politika/feed/` feed, takođe već filtriran. **N1 nema poseban "Politika" tag niti kategoriju** (provereno i na nivou koda: kad je implementiran filter po `<category>Politika</category>`, uživo je vratio 0 od 20 stavki, jer N1 taj tag jednostavno ne koristi) — koristi se njihova opšta "Vesti" rubrika (`/vesti/feed/`) bez filtriranja, kao najbliži dostupan ekvivalent.

Ne kopiramo pun tekst članka — samo metapodatke. `news_article`: id, source, external_id, title, description, url (obavezno), image_url, published_at, fetched_at, created_at.

Backend: `NewsProvider` interface (getSource, fetch) sa implementacijama po izvoru; scheduler na 15 min; normalize → dedupe (min. source + original URL) → DB. Bez AI za detekciju duplikata u V2. **Svaki provider vraća samo najnovijih 5 stavki po pokretanju** (feed-ovi vraćaju od 20 do 100+ stavki, ne čuvamo ceo backlog — samo najsvežije po izvoru na svakih 15 min).

API: `GET /api/v1/news` sa `?source=`, `?page=`, `&size=`. UI: sekcija na homepage + `/news` sa paginacijom/infinite scroll. Klik vodi na originalni medij.

## V3 — Polls + Prediction Markets + Timeline

Polls: `poll` (pollster, title, published_at, fieldwork_from/to, sample_size, methodology, population, source_url) + `poll_result` (poll_id, electoral_list_id, label, percentage). Ne računamo sopstveni "polling score"; kasnije eventualno istorijski chart iz objavljenih istraživanja (ne prognoza).

Prediction markets: potpuno odvojen modul od polls, sa eksplicitnom UI napomenom "Market prices are not polling data". `prediction_market` (provider, market_name, source_url, updated_at) + `prediction_market_outcome` (market_id, name, price, updated_at). API > scraping kad god provider ima API.

Timeline: `election_event` (election_id, type, title, description, event_date, source_url) — vizuelna timeline ključnih datuma (raspisivanje, predaja/proglašenje lista, izborni dan).

## V4 — Deployment

**(Izmena 2026-09-17) Deploy je namerno pomeren na kraj**, posle V2 i V3 — cela aplikacija se gradi i testira lokalno (FE :3000, BE :8080, Postgres :5432 iz `docker-compose.yml`) i tek kad je funkcionalno kompletna ide se na hosting. Domen se kupuje tek u ovom koraku, ne ranije — nema smisla plaćati zakup dok sajt nije spreman za javnost, niti vredi vrteti sajt sa domenom iz lokala (kućni internet ima dinamičku IP, treba port forwarding, nema lak https) — to je više rizika nego koristi u odnosu na to da se samo sačeka do stvarnog deploy-a.

- **Domen:** `izbori.rs` je zauzet (proveren whois 2026-09-17, registrovan od 2016, ističe 2026-10-16). `srbijaizbori.rs` je slobodan (proveren isti dan) — kandidat za registraciju u ovom koraku. Registracija ide preko RNIDS akreditovanog registrara (npr. Adriahost), ~2.150-2.600 RSD/god + PDV.
- FE: Vercel (Hobby plan, $0/mesečno — dovoljno za ovaj obim saobraćaja).
- BE: Render (Docker, Web Service). Free compute postoji ali se gasi posle 15 min neaktivnosti, što bi pauziralo i RIK scheduler u međuvremenu — razmotriti plaćeni compute (~$7/mesečno) ako se pokaže da free tier pravi probleme sa redovnošću osvežavanja.
- DB: Render PostgreSQL (managed). **Free tier ima 30-dnevni limit pa se baza briše** (+14 dana grace period) — neprihvatljivo za projekat koji treba da traje bar do i posle 25.10.2026. Ide se na najjeftiniji plaćeni tier (~$6/mesečno, 256MB RAM/1GB storage — dovoljno za ovaj dataset).
- Produkcija (kad se domen odabere): `<domen>` (Vercel/Next.js) → `api.<domen>` (Render/Spring Boot) → PostgreSQL.
- Env vars: `NEXT_PUBLIC_API_URL` (FE); `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `RIK_BASE_URL` (BE). Ništa osetljivo u Git-u.

**Pre deploy-a:** razmotriti HTTP cache headers (ili prost in-memory cache) na javnim `/api/v1/*` endpoint-ima — nema auth/rate-limiting u V1, a podaci se osvežavaju na 15 min, pa keširanje smanjuje opterećenje baze bez uvođenja Redis-a.

## Cross-cutting

- Global error handling (`@RestControllerAdvice`, standardizovan `{code, message, timestamp}` response).
- Logging: scraper start/source/duration/fetched/created/updated/failure, bez teškog DEBUG u produkciji.
- Spring Actuator `/actuator/health` za Render healthcheck.
- Flyway isključivo, bez `ddl-auto=update` u produkciji.
- Testing: unit (parser, services, normalizacija), integration (repository, REST, Testcontainers Postgres), HTML fixtures u `src/test/resources/rik/` za parser testove (ne gađati pravi RIK). FE: Vitest + RTL, kasnije Playwright za par E2E flow-ova.
- Git: `main` + `feature/*` branch-evi, PR → main, merge pokreće deploy. CI: GitHub Actions (PR: `mvn test` + FE lint/test/build; merge main: Vercel + Render deploy).

## Namerno NE radimo

Microservices, Kafka, Redis u V1, Kubernetes, authentication, user accounts, comments, admin panel u V1, GraphQL, WebSockets, AI-generated political summaries, AI ranking stranaka, AI prediction pobednika.

## Milestones

V1.0 skeleton → V1.1 Postgres+Flyway → V1.2 RIK integration → V1.3 election REST API → V1.4 countdown UI → V1.5 electoral lists UI → V1.6 responsive+SEO → V2.0-2.4 news → V3.0-3.5 polls/markets/timeline/final dashboard → **V4.0 production deploy (domen + Vercel + Render + plaćeni Postgres)**.

V1.0-V1.6 su gotovi (FE+BE, lokalno testirano). Sledeći korak: V2 News Aggregator.

## Review napomene (2026-09-16)

- Verzije potvrđene: Spring Boot 4.1.1 je zaista trenutna stabilna verzija (izašla avgust 2026, spring.io). Next.js je na 16.3 kao najnovijoj minor verziji (App Router je default). Oba izbora su validna i aktuelna.
- RIK nema javno vidljiv REST/open-data API — samo web stranice (npr. `rik.parlament.gov.rs/zapisnici/...`). Jsoup scraping pristup je opravdan, ali HTML struktura nije garantovano stabilna — otud opravdano insistiranje na HTML fixture testovima za parser i na `data_import` audit tabeli.
- Polymarket za ovu konkretnu izbornu trku je neizvesno u V3 obliku koji plan predviđa: postoje Polymarket tržišta vezana za Srbiju (npr. "will parliamentary election be called before 2027", predsednik/premijer predictions), ali nije potvrđeno postojanje tržišta sa kvotama po izbornoj listi/stranci za parlamentarne izbore 25. oktobra 2026. Ovo treba proveriti neposredno pre V3 rada na prediction markets modulu — moguće da model `prediction_market_outcome` treba da bude generičniji (npr. binarni ishodi umesto per-lista kvota).
- ~~Pre V2 treba proveriti ToS/robots.txt za N1, Nova, Blic, Informer i da li neki od njih ima RSS.~~ **Provereno 2026-09-17: svi dozvoljavaju crawl i svi imaju RSS** — vidi detalje u sekciji V2.
- ~~Proveriti dostupnost domena izbori.rs pre V1.1 deploy koraka.~~ **Provereno 2026-09-17: izbori.rs zauzet, srbijaizbori.rs slobodan.** Kupovina domena pomerena u V4 (vidi ispod).
- **(Dodato) Pre RIK integracije (V1.2) proveriti robots.txt za `rik.parlament.gov.rs`.**
- **(Dodato) Pred V4 (production deploy) razmotriti HTTP caching na public API endpoint-ima zbog odsustva auth/rate-limiting-a u V1.**
- **(Dodato 2026-09-17) Deploy (V1.7 → preimenovano V4) pomeren na kraj, posle V2/V3 — vidi sekciju V4 i "Način rada sa Claude-om".**

## Način rada sa Claude-om

Ovaj dokument se koristi kao project context/spec, ali implementacija ide milestone po milestone — ne generisati sve odjednom. Prvi implementacioni prompt: monorepo skeleton, Spring Boot projekat, Next.js projekat, Docker Postgres, provera da sva tri rade lokalno. Tek posle toga: model baze i RIK ingestion.

**(Dodato 2026-09-17)** Cela aplikacija (V1-V3) se gradi i verifikuje lokalno pre bilo kakvog hostinga. Deploy, kupovina domena i produkcioni troškovi dolaze tek na kraju, kao V4 — vidi sekciju V4 — Deployment.
