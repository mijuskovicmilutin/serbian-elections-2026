# Serbia Elections 2026 — Project Plan

Status: plan, reviewed 2026-09-16, dopunjen 2026-09-21 (V3: Polymarket kartica, ankete, novi raspored početne). Implementacija ide milestone po milestone (ne generisati sve odjednom).

## 1. Cilj

Javni, neutralni informacioni portal za parlamentarne izbore u Srbiji 2026 (izbori raspisani za 25. oktobar 2026). Sajt agregira i strukturirano prikazuje podatke iz jasno označenih izvora — ne proizvodi političke procene niti rangira opcije.

Izvori: RIK (izborni podaci, izborne liste), političke rubrike medija (vesti), agencije za istraživanje javnog mnjenja (CRTA, Faktor Plus, CeSID; svaka anketa se ručno pregleda i odobri pre objave), prediction-market podaci (Polymarket i sl., jasno odvojeni od anketa).

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

Mockupi za sve što je u ovoj sekciji su u Claude Design canvasu "Izbori 2026 – Ankete" (https://claude.ai/artifact/3sgSQ6pKLbxNeeCQhUP8Q9): ekran 1 (sekcija Istraživanja na početnoj), 2 (`/istrazivanja`), 3 (admin pregled), 4 i 5 (novi raspored početne, široki i uži ekran). Polymarket kartica ima svoj canvas (https://claude.ai/artifact/64GCv3fcTZyqy2KsrHbg7r).

### V3.0 Prediction markets — implementirano (2026-09-20)

Potpuno odvojen modul od polls, sa eksplicitnom UI napomenom "Cene tržišta nisu podaci iz anketa". API > scraping kad god provider ima API.

- **Izvor:** Polymarket Gamma API (`/events?slug=...`, događaj "Next Prime Minister of Serbia?") + CLOB API (`/prices-history`) za istoriju cena. Uvoz na 15 min (`PolymarketImportJob`). Placeholder slotovi bez prometa ("Person C", cena 0.50) se izbacuju u `PolymarketNormalizer`.
- **Model (V6 + V7):** `prediction_market` (provider, external_id, market_name, source_url, updated_at, volume, end_date) + `prediction_market_outcome` (name, price, image_url, volume, one_day_price_change, best_ask, best_bid, price_history). `price_history` je kompaktan JSON (TEXT) i čuva se samo za dva vodeća ishoda (`polymarket.history-outcomes=2`, `history-fidelity-minutes=720`). Neuspeh dobavljanja istorije ne obara uvoz.
- **API:** `GET /api/v1/prediction-markets/current`; `yesPrice` = best ask, `noPrice` = 1 − best bid.
- **UI:** posebna kartica (naslov, trobojna ikona, grafik oba vodeća ishoda, ukupni promet i datum zatvaranja, redovi 1 i 2 sa slikom, procentom, promenom za 24h i cenama Yes/No koje vode na Polymarket, napomena da to nisu ankete). Nema "Buy" poziva na kupovinu.
- **Namerno bez državnog grba:** ikona događaja na Polymarketu sadrži grb, mi ga ne prikazujemo (portal ne sme da liči na zvaničan državni sajt). Umesto toga trobojka (crvena, plava, bela).

### Ankete (polls) — dizajn dogovoren 2026-09-21, nije implementirano (milestone-i V3.2–V3.6)

**Princip:** ne uniformišemo izvorne podatke, uniformišemo naš model i prikaz, a izvorne vrednosti čuvamo. Tačnost je važnija od automatizacije: ankete izlaze nekoliko puta mesečno, pa je greška u parsiranju (npr. "među opredeljenima" pročitano kao "svi ispitanici") skuplja od ručnog unosa.

**Izvori (whitelist, ne "svaka anketa na internetu"):**
- **CRTA** (primarni izvor, `crta.rs`): robots.txt otvoren; stranice i sitemap vraćaju 403 (Cloudflare), ali RSS feed radi. Prati se feed, a brojevi se unose ručno.
- **Faktor Plus** (sekundarni izvor): Joomla sajt bez feeda (uz podrazumevani User-Agent robots.txt odgovara "Not Acceptable"). Rezultate prenose Danas, RTS, Tanjug i drugi mediji; anketa se vodi kao *sekundarni izvor* sa navedenim medijima, a otkriva se skeniranjem medijskih feedova.
- **CeSID:** njihova stranica "istraživanja" ne sadrži redovne ankete o glasačkim namerama (poslednje stavke su o EU integracijama i mladima), ali sajt ima RSS feed koji se prati. Ostaje na listi izvora sa praznim stanjem ("još nema objavljenog istraživanja") dok ne objave anketu.
- Agregatori (npr. `rejtingpolitickihstranaka.rs`) se koriste najviše kao cross-check, ne kao izvor. BIRODI je model procene rezultata, ne anketa, i ne prikazuje se.
- Novi izvor se dodaje tek kad objavljuje dovoljno metapodataka: ko je sproveo, kada, na kome i šta procenat predstavlja.
- **Pravilo objave:** ne prikazuje se anketa ako ne možemo pouzdano utvrditi agenciju, period, na šta se procenti odnose i izvor (primarni/sekundarni). Ostala polja mogu biti nepoznata, ali se to jasno pokazuje.

**Tok (hibrid, objava je uvek ručna):**
`DISCOVERED` (nađena nova objava, samo naslov/datum/link) → `DRAFT` (izvučene vrednosti, najbolji pokušaj ili ručni unos) → `APPROVED` / `REJECTED`. Javni API vraća isključivo `APPROVED`. Automatika je samo otkrivanje i pokušaj izvlačenja, odobrava uvek čovek. Izvor: `Polls*Provider` po agenciji (praćenje objava), sa ručnim unosom kao normalnim putem.

**Model (Flyway migracije):**
- `pollster`: id, name, kind (PRIMARY/SECONDARY_VIA_MEDIA), website, discovery_url, active.
- `poll`: id, pollster_id, title, published_at, fieldwork_from, fieldwork_to (opciono), fieldwork_note (npr. "avgust–septembar 2026."), sample_size, population, method, conducted_by, commissioned_by, margin_of_error, result_basis (ALL_RESPONDENTS / LIKELY_VOTERS / DECIDED_VOTERS / OTHER), decided_share_pct, undecided_pct, wont_vote_pct, will_vote_pct, source_kind (PRIMARY/SECONDARY), source_url, original_document_url, media_sources (JSON), status, reviewed_at, review_note, scraped_at, content_hash, source_snapshot, created_at, modified_at.
- `poll.source_note` (V11): šta izvor sam kaže o brojevima a ne stoji u drugim poljima (npr. "ostali ne prelaze izborni prag"); javno se prikazuje ispod rezultata ("Напомена извора"), unosi se u adminu.
- `poll_result`: id, poll_id, `raw_option_name` (tačno kako je objavljeno, nikad ne menjamo), percentage, display_order (redosled iz izvora), option_kind (PARTY / COALITION / ELECTORAL_LIST / UNSPECIFIED), composition (partneri, tekst iz izvora), electoral_list_id (opciono, samo kad je povezivanje jasno; predlog mora da se potvrdi).
- Puna tabela `political_option` + članovi se ne uvodi u prvoj verziji.
- **Odluka o prikazu koalicija:** `option_kind` i `composition` se čuvaju kao interna evidencija, a javno se **ne prikazuju** (oznake "+ k", "(?)" i beleške ispod kartice su testirane u mockupu i ocenjene kao nepregledne). Poznat rizik: "SNS" u jednoj anketi može biti sama stranka, a u drugoj sa koalicionim partnerima; javno to nose samo nazivi opcija iz izvora. Ako se odluka promeni, podatak je već u bazi.

**Uniforman prikaz (svako istraživanje isto):**
- Ista kartica bez obzira na izvor. Fiksnih 12 polja u istom redosledu: Teren (period), Uzorak, Metod / Populacija, Margina greške, Naručilac / Udeo opredeljenih u uzorku, Neopredeljeni, Neće glasati / Izjasnilo se da će glasati, Originalni izveštaj, Medijski prenos. Polje koje izvor ne navodi ostaje na istom mestu, sivo, sa istim tekstom "Nije navedeno u izvoru".
- Traka "Među opredeljenim biračima" (ili odgovarajuća osnova rezultata) i oznaka primarni/sekundarni izvor u zaglavlju.
- Trake sve iste boje (bez boja stranaka), skala 0–100%, opcije redosledom iz izvora, bez sortiranja i bez izdvajanja pobednika.
- Ne računamo sopstveni prosek ni "polling score". Zajednički grafik tek kasnije: samo istorija jedne agencije sa istom osnovom rezultata, nikad linija kroz nesaporedive ankete.
- Ne kopiramo tuđe grafikone ni PDF-ove, samo brojeve uz link na izvor.

**API:** javno `GET /api/v1/polls` (`?pollster=`, `?page=`, `&size=`, samo APPROVED), `GET /api/v1/polls/{id}`, `GET /api/v1/pollsters` (sa brojem odobrenih). Interno (zaštićeno tajnim ključem): `GET /internal/polls?status=`, `PATCH /internal/polls/{id}`, `POST /internal/polls/{id}/approve`, `.../reject`.

**Admin pregled (izuzetak od "bez admin panela"):** skrivena stranica `/admin/istrazivanja` (noindex), zaštićena jednim tajnim ključem iz env varijable (`ADMIN_API_KEY`), bez naloga i korisnika. Levo: tabovi Na čekanju / Odobrena / Odbijena, praćeni izvori sa stanjem (npr. "sajt blokira automatski pristup, prati se preko medija"), tok statusa. U sredini: forma sa svim poljima ankete (nepoznato polje je označeno), redovi rezultata (naziv kao u izvoru, procenat, interna oznaka opcije i partneri, opciono povezivanje sa RIK listom), provera zbira (npr. 97,6% za osnovu "opredeljeni" je upozorenje koje ne blokira objavu), lista uslova za objavu i dugmad Odbij / Sačuvaj nacrt / Odobri i objavi. Desno: izvor sa linkom, vreme preuzimanja, otisak sadržaja i istaknute izvučene vrednosti. Izmene posle objave ostavljaju zapis.

**Izborna tišina (pravna provera pre objave!):** verovatno postoji zabrana objavljivanja rezultata anketa neposredno pred izbore i na izborni dan; tačno pravilo nije potvrđeno i mora ga proveriti neko pravno kompetentan (ili pravila RIK-a) pre nego što se ankete objave. Ugraditi prekidač u konfiguraciji koji automatski sakriva ankete (i razmotriti predikciono tržište) u podesivom periodu, sa napomenom na mestu kartice.

**Homepage i stranice:** kartica "Istraživanja javnog mnjenja" na početnoj (najnovije istraživanje svake agencije, po datumu objave, prva tri rezultata, link "Još N opcija u detaljima" i "Sva istraživanja"), stranica `/istrazivanja` (filter po agenciji sa brojevima, npr. CeSID (0), grupisanje po mesecima, pune kartice, napomena da rezultati nisu direktno uporedivi).

### Timeline (milestone V3.7)

`election_event` (election_id, type, title, description, event_date, source_url) — vizuelna timeline ključnih datuma (raspisivanje, predaja/proglašenje lista, izborni dan).

## Raspored početne strane (dogovoreno 2026-09-21)

Redosled odozgo: zaglavlje, hero sa odbrojavanjem, zatim, preko fotografije Skupštine, kartice **Izborne liste** i **Istraživanja javnog mnjenja**; ispod njih tamna sekcija **Vesti o izborima** i u njoj **Predikciono tržište**; na kraju podnožje.
- **Liste + istraživanja:** jedno pored drugog kad ima mesta (oko 1200px i više, svaka kartica bar ~560px), inače istraživanja ispod listi. Kartice su bele.
- **Predikciono tržište** se premešta iz hero dela ispod vesti (trenutno je ispod izbornih lista). Kartica je tamna (`#1B211F`, okvir `#2B322D`), kao kartice vesti, i zauzima širinu sadržaja (760px). Grafik se skalira na širinu kartice.
- **Tamna boja** sekcije vesti (`#14181A`) ide bez prekida do dna stranice, uključujući podnožje ("O portalu", "Izvori", napomena da portal nije zvaničan). Tamna paleta u ovom delu je fiksna (ne zavisi od teme), za razliku od ostatka koji sada prati `prefers-color-scheme`; potvrditi pri implementaciji.
- Mobilni prikaz: sve u jednoj koloni istim redosledom.

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

Microservices, Kafka, Redis u V1, Kubernetes, authentication, user accounts, comments, admin panel u V1 (jedini izuzetak je mala skrivena stranica za pregled anketa u V3, zaštićena tajnim ključem, bez naloga), GraphQL, WebSockets, AI-generated political summaries, AI ranking stranaka, AI prediction pobednika, prikaz državnog grba ili zvaničnog izgleda, automatsko objavljivanje anketa bez ručnog odobrenja, sopstveni prosek/rejting iz anketa.

## Milestones

V1.0 skeleton → V1.1 Postgres+Flyway → V1.2 RIK integration → V1.3 election REST API → V1.4 countdown UI → V1.5 electoral lists UI → V1.6 responsive+SEO → V2.0-2.4 news → V3.0 prediction markets → V3.1 raspored početne (relayout) → V3.2 polls backend → V3.3 polls admin pregled → V3.4 polls discovery → V3.5 polls FE (početna + `/istrazivanja`) → V3.6 prekidač izborne tišine (posle pravne provere) → V3.7 timeline → **V4.0 production deploy (domen + Vercel + Render + plaćeni Postgres)**.

**Status (2026-09-21):** V1 i V2 gotovi, V3 gotov osim V3.6: V3.0 (Polymarket), V3.1 (relayout početne), V3.2 (model i javni API anketa), V3.3 (admin pregled), V3.4 (otkrivanje objava), V3.5 (prikaz anketa) i V3.7 (timeline). Ostaje **V3.6 (prekidač izborne tišine, posle pravne provere)** i onda V4 (deploy). **Pre objave prve prave ankete proveriti pravilo o izbornoj tišini.**

- **V3.1 relayout (gotovo):** samo FE. Polymarket je premešten ispod vesti i postao tamna kartica (boje iz CSS promenljivih), tamna sekcija (`.darkArea`, fiksna paleta) ide do dna zajedno sa podnožjem. Red kartica u hero delu je `flex` sa prelamanjem (`flex: 1 1 560px`, max 760px): jedna kartica (liste) je široka 760px, a kad se doda karta istraživanja stajaće jedna pored druge od oko 1200px, inače jedna ispod druge (proveren u pregledaču simulacijom druge kartice). Namerno nije dodat prazan slot za istraživanja dok ne postoji backend.
- **V3.2 polls backend (gotovo):** migracija `V8__add_polls.sql` (`pollster` sa seed-om CRTA / Faktor Plus / CeSID, `poll`, `poll_result`), entiteti i enum-i u `poll/`, javni read-only API: `GET /api/v1/polls` (`?pollster=<slug>`, paginacija, samo APPROVED, najnovije prvo), `GET /api/v1/polls/{id}`, `GET /api/v1/pollsters` (sa brojem odobrenih). Neodobrena anketa je za API isto što i nepostojeća (404); javni DTO ne sadrži `option_kind`, `composition`, status, belešku pregleda ni snimak izvora. Odstupanja od modela iz plana: dodato `pollster.slug` (za `?pollster=crta`), `poll.published_at` je za sada NOT NULL (otkriveni kandidati imaju datum objave), `PageResponse` premešten u `common.dto`. Testovi: mapper, servis (Mockito) i integracioni test kroz pravi HTTP i lokalnu bazu (sam pravi i briše svoje redove). Nema seed-ovanih anketa: prve idu tek kroz admin pregled (V3.3).
- **V3.3 admin pregled (gotovo):**
  - **BE `/internal/**`:** `GET /internal/polls?status=` (paginirano), `GET /internal/polls/{id}` (sa uslovima objave, upozorenjima i istorijom), `POST /internal/polls` (nacrt), `PUT /internal/polls/{id}` (zamena sadržaja; PUT umesto PATCH jer forma uvek šalje celo stanje), `POST .../approve`, `POST .../reject` (razlog obavezan), `GET /internal/pollsters`. Novi izuzeci: 400 (`BadRequestException`, validacija), 422 (`BusinessRuleException`, npr. objava nepotpune ankete ili duplikat izvora).
  - **Zaštita:** `AdminKeyFilter` traži zaglavlje `X-Admin-Key` (poređenje SHA-256 digest-a, konstantno vreme). Bez podešenog `ADMIN_API_KEY` interni API ne postoji (404), pa zaboravljena promenljiva nikad ne otvara pristup. Nema naloga.
  - **Validacija:** dodata zavisnost `spring-boot-starter-validation` (Jakarta Validation je bila u planu, a nije bila u `pom.xml`). Samo `http(s)` linkovi, procenti 0–100, redosled datuma, jedinstven (agencija, izvor).
  - **Uslovi objave (blokiraju):** period (datumi ili opis), osnova rezultata, izvor, bar jedan rezultat. **Upozorenja (ne blokiraju):** zbir procenata van očekivanog, opcije bez podataka o partnerima, samo opis perioda bez tačnih datuma.
  - **Istorija:** nova tabela `poll_audit_log` (`V9`) beleži svaku izmenu (i pre i posle objave) kao razliku polja, plus odobrenje i odbijanje. Izmena objavljene ankete je dozvoljena i ostaje javna, uz zapis.
  - **FE `/admin/istrazivanja`:** Next.js `proxy.ts` štiti `/admin/**` HTTP Basic autentifikacijom (korisnik `admin`, lozinka je `ADMIN_API_KEY`), sa `X-Robots-Tag: noindex`. Ključ je samo na serveru (`process.env.ADMIN_API_KEY`), Server Actions pozivaju BE i sami dodatno proveravaju kredencijal (Server Action je javan POST). Stranica: tabovi Na čekanju / Odobrena / Odbijena, forma sa svim poljima, redovi rezultata (naziv kao u izvoru, procenat, interna oznaka opcije, partneri, RIK lista), uslovi objave, Sačuvaj / Odobri i objavi / Odbij sa razlogom, panel sa izvorom i istorijom.
  - **Lokalno pokretanje:** isti tajni ključ u `FE/.env.local` (`ADMIN_API_KEY`, git ga ignoriše, primer je u `FE/.env.example`) i kao env promenljiva backenda (`ADMIN_API_KEY`). U pregledač se ulazi kroz Basic-auth prozor (korisnik `admin`).
  - **Testovi:** filter (6), uslovi objave (7), integracioni test celog toka kroz pravi HTTP (8: bez ključa 401, nacrt nije javan, nepotpuna anketa se ne objavljuje, objava i povlačenje, izmena sa zapisom, nevalidan unos, duplikat, filter po statusu). Ukupno BE: 51 test. FE: proveren u pregledaču (kreiranje, objava, izmena, povlačenje) i produkcioni build.
  - **Nije urađeno (V3.4):** praćenje izvora (status "proveren pre 15 min") u levom panelu, `DISCOVERED` kandidati, snimak izvora i otisak (polja postoje, ali ih još niko ne popunjava).
- **V3.4 discovery (gotovo):**
  - **Provera izvora (2026-09-21):** RSS feed CRTA (`crta.rs/feed/`) i CeSID (`cesid.rs/feed/`) rade uz običan User-Agent (CRTA sitemap blokira Cloudflare, feed ne). Faktor Plus je Joomla sajt bez feeda, a stranica "Политички барометар" je zastarela (decembar 2024), pa se on prati samo preko medija. robots.txt sva tri sajta dozvoljava ove putanje.
  - **Izvori (`ingestion/poll/`):** `PollsterOwnFeed` za CRTA i CeSID (primarni izvor), `MediaPollFeed` skenira pune feedove N1, Nova, Blic i Informer (sekundarni izvor; `NewsProvider.fetchAllItems()`). Raspored na 15 min (`polls.discovery.*`), svaki izvor beleži svoj `data_import` red (`POLL_CRTA`, `POLL_CESID`, `POLL_MEDIA`).
  - **Prepoznavanje (`PollTextMatcher`):** tekst se svodi na malu latinicu bez dijakritika (ćirilica i latinica se poklapaju). Vest liči na anketu ako ima reč o anketi ili formulaciju tipa "bi osvojila / bi glasalo / cenzus". Iz medija se traži i procenat, a agencija se prepoznaje po imenu uz padeže ("Crte", "Faktor plusa"). Namerno labavo: lažno pozitivan košta jedan klik "odbij", propuštena anketa se nikad ne bi videla.
  - **Šta se pravi:** samo kandidat `DISCOVERED` (naslov, link, datum, tekst koji smo videli kao snimak, SHA-256 otisak, `scraped_at`). **Brojevi se nikad ne izvlače automatski**: šta procenti znače odlučuje čovek u pregledu. Vesti starije od 14 dana se ignorišu (da prvi rad ne poplavi pregled), a više medijskih izveštaja o istoj anketi (isti agencija, ±3 dana, još neotvoren kandidat) spajaju se u jednog.
  - **Admin:** prvo čuvanje otvorenog kandidata prebacuje ga u `DRAFT` (zapis u istoriji); `GET /internal/poll-sources` vraća poslednju proveru svakog izvora, a u levom panelu je "Праћени извори" (proveren pre X min, broj novih kandidata ili greška).
  - **Testovi:** matcher (7), servis otkrivanja (8, uključujući spajanje izveštaja i pad izvora), integracioni tok kandidata i status izvora. Na pravim feedovima: skenirano 215 medijskih stavki, 3 CRTA i 10 CeSID, bez novih kandidata (nijedna ne liči na anketu).
- **V3.5 polls FE (gotovo):**
  - **Početna:** `PollHomeCard` pored kartice izbornih lista (isti `flex` red iz V3.1: jedna pored druge od oko 1200px, inače ispod). Prikazuje najnoviju odobrenu anketu svake agencije, poređane po datumu objave (`getLatestPollPerPollster`), prva tri rezultata, osnovu rezultata, izvor (za sekundarni "preuzeto iz medija"), link "Још N опција у детаљима" i "Сва истраживања". Bez odobrenih anketa kartice nema, a greška API-ja nikad ne ruši početnu.
  - **`/istrazivanja`:** filter po agenciji (`?pollster=<slug>`, sa brojevima, npr. CeSID (0) i napomena), grupisanje po mesecima objave, paginacija, puna kartica `PollCard` sa fiksnih 12 polja istim redosledom (nedostajuće: sivo "Није наведено у извору"), traka o osnovi rezultata, sve opcije redosledom iz izvora, iste boje traka, skala 0–100%. Link "Истраживања" dodat u zaglavlje.
  - **Neutralnost u kodu:** ne sortiramo opcije, ne bojimo po strankama, ne računamo prosek. `option_kind` i `composition` se ne šalju javnim API-jem pa ih FE ni ne može prikazati.
  - **Tema:** kartica na početnoj je fiksno svetla (kao kartica lista), stranica `/istrazivanja` prati svetlu/tamnu temu (boje kroz `--p*` promenljive u `polls.module.css`).
  - **Provera:** izmenjeni fajlovi prolaze `tsc` i `eslint`, produkcioni build prolazi. U pregledaču sa privremenim anketama (obrisane): oba rasporeda početne, filter, svetla/tamna tema, mobilni prikaz bez horizontalnog skrola, prazna stanja, formatiranje perioda preko dva meseca, margine greške i procenata sa dve decimale. Nema FE unit testova (test runner još nije dodat).
- **V3.6 izborna tišina:** konfigurabilan prekidač koji sakriva ankete; uključiti tek posle pravne provere.
- **Prve dve prave ankete (2026-09-21):** CRTA / DAL Stanford (jun 2026, n=2.324, među opredeljenima) i Faktor Plus (avgust–septembar 2026, n=1.200, "odgovarali samo opredeljeni", prvi objavio Blic) uneti su i odobreni kroz admin, sa napomenom izvora. Brojevi su proveravani u sirovom tekstu članaka (Danas, N1, Nova, Informer, 021, Tanjug), imena opcija prepisana u ćirilicu po objavljenom tekstu. **Nalaze se samo u lokalnoj bazi**: za produkciju ih treba ponovo uneti kroz admin.
- **V3.7 timeline (gotovo):** tabela `election_event` (`V10`), `GET /api/v1/elections/current/events`, sekcija "Кључни датуми" na početnoj (prva u tamnom delu): vertikalna linija, prošli datumi prigušeni, sledeći istaknut sa "за N дана", svaki datum sa izvorom. Seed: raspisivanje izbora 9. septembra (Danas), rok za prijavu glasanja u inostranstvu 3. oktobra, rok za liste sa 10.000 potpisa 4. oktobra i rok za domaće posmatrače 17. oktobra (Mondo "EUpravo zato"), dan glasanja iz `election.election_date`. Novi događaji se za sada dodaju migracijom (admin za događaje nije potreban dok ih ima malo).

## Review napomene (2026-09-16)

- Verzije potvrđene: Spring Boot 4.1.1 je zaista trenutna stabilna verzija (izašla avgust 2026, spring.io). Next.js je na 16.3 kao najnovijoj minor verziji (App Router je default). Oba izbora su validna i aktuelna.
- RIK nema javno vidljiv REST/open-data API — samo web stranice (npr. `rik.parlament.gov.rs/zapisnici/...`). Jsoup scraping pristup je opravdan, ali HTML struktura nije garantovano stabilna — otud opravdano insistiranje na HTML fixture testovima za parser i na `data_import` audit tabeli.
- ~~Polymarket za ovu konkretnu izbornu trku je neizvesno u V3 obliku koji plan predviđa.~~ **Rešeno (V3.0):** koristi se događaj "Next Prime Minister of Serbia?" sa ishodom po kandidatu (ne po listi), pa je model `prediction_market_outcome` generički (name, price, ...). Nema tržišta po izbornoj listi, što se i ne prikazuje.
- ~~Pre V2 treba proveriti ToS/robots.txt za N1, Nova, Blic, Informer i da li neki od njih ima RSS.~~ **Provereno 2026-09-17: svi dozvoljavaju crawl i svi imaju RSS** — vidi detalje u sekciji V2.
- ~~Proveriti dostupnost domena izbori.rs pre V1.1 deploy koraka.~~ **Provereno 2026-09-17: izbori.rs zauzet, srbijaizbori.rs slobodan.** Kupovina domena pomerena u V4 (vidi ispod).
- **(Dodato) Pre RIK integracije (V1.2) proveriti robots.txt za `rik.parlament.gov.rs`.**
- **(Dodato) Pred V4 (production deploy) razmotriti HTTP caching na public API endpoint-ima zbog odsustva auth/rate-limiting-a u V1.**
- **(Dodato 2026-09-17) Deploy (V1.7 → preimenovano V4) pomeren na kraj, posle V2/V3 — vidi sekciju V4 i "Način rada sa Claude-om".**
- **(Provereno 2026-09-21) Izvori anketa:** CRTA — robots.txt otvoren, ali automatski zahtev vraća 403. Faktor Plus — sajt blokira automatski pristup; rezultati stižu preko medija (Danas, RTS, Tanjug). CeSID — nema redovnih anketa o glasačkim namerama, nema RSS-a. Zaključak: automatski scraping anketa nije pouzdan put; radi se otkrivanje objava + ručno odobravanje (vidi sekciju Ankete).
- **(Provereno 2026-09-21) Činjenice iz izvora za mockup:** CRTA/DAL Stanford, terenski rad 10–24. jun 2026, licem u lice, n=2.324, među opredeljenima (70% uzorka) Studentska lista 44,9%, SNS 35,7%, SPS 3,8%; 12% neopredeljenih, 9% neće glasati. Faktor Plus, avgust–septembar 2026, n=1.200, terensko istraživanje, SNS 47,2%, studentska lista 31,5%, SPS 4,9%, proevropska koalicija 3,6%, NPS opcija 3,4%, Mi snaga naroda 2,9%, NADA 2,1%, SRS 2,0%; 31% neopredeljenih, 64% se izjasnilo da će glasati. Ovo pokazuje da se procenti odnose na različite osnove (opredeljeni birači u oba slučaja, ali različiti udeli neopredeljenih i različito imenovane opcije) — zato je osnova rezultata obavezno polje.
- **(Otvoreno) Pravna provera izborne tišine** za objavu anketa (i razmotriti predikciono tržište) pre V3.5 objave — vidi sekciju Ankete.
- **(Otvoreno) Odluka o prikazu koalicija:** trenutno se ne prikazuje javno (interna evidencija `option_kind`/`composition`), vidi sekciju Ankete. Preispitati ako se pokaže da korisnici pogrešno porede "SNS" iz različitih anketa.

## Način rada sa Claude-om

Ovaj dokument se koristi kao project context/spec, ali implementacija ide milestone po milestone — ne generisati sve odjednom. Prvi implementacioni prompt: monorepo skeleton, Spring Boot projekat, Next.js projekat, Docker Postgres, provera da sva tri rade lokalno. Tek posle toga: model baze i RIK ingestion.

**(Dodato 2026-09-17)** Cela aplikacija (V1-V3) se gradi i verifikuje lokalno pre bilo kakvog hostinga. Deploy, kupovina domena i produkcioni troškovi dolaze tek na kraju, kao V4 — vidi sekciju V4 — Deployment.
