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

### V3.8 Dizajn početne — implementirano (2026-09-23)

Samo FE, po mockupu "Izbori 2026 – Dizajn V3.8" (https://claude.ai/artifact/KPDtfb9uafm4SJ3Xc21pvH).
- **Širina:** cela početna (osim hero teksta) deli kontejner 1592px (`.wideWrap`). Liste i istraživanja: jedna pored druge od 1200px (po pola ekrana, najviše 760px), od 720 do 1199px jedna ispod druge širine 60% ekrana (min 520px), ispod 720px cela širina.
- **Ključni datumi:** horizontalno od 900px, vertikalno ispod; Dan glasanja u `--signal` boji.
- **Vesti:** blok po izvoru (logo + 4 stavke sa slikom levo), dva bloka u redu od 900px. Ostaje 4 vesti po izvoru i **bez filtera po temi izbora** (N1 koristi opšti feed; namerno odloženo jer je filter po ključnim rečima težak za održavanje).
- **Predikciono tržište:** kompaktno, ishodi levo i grafik desno, bez Yes/No dugmadi i bez rang oznake.
- **Tekst:** rečenica (prvo veliko slovo) umesto versala svuda, wordmark "Избори 2026" ćirilicom, oznaka УЖИВО uklonjena (čuva se za izbornu noć, V4). Imena listi ostaju kako ih daje RIK.
- **Značka "нова"** na listi koju je RIK objavio u poslednja 48 sata.

## V4 — Izborni dan i izborna noć (Election Day Mode)

**Status: dogovoreno 2026-09-21, nije početo.** Numeracija: ovo je novi V4, a raniji "V4 — Deployment" je sada **V5**. Zavisi od V3.6 (fazni model i prekidač izborne tišine): V4 ga proširuje.

**Cilj:** od početka izborne tišine sajt prelazi u drugačiji režim sa drugačijim dizajnom, namenjen praćenju izbornog dana i noći. Isti principi kao ostatak portala: samo javni, označeni izvori, bez sopstvenih procena, rangiranja i prognoza; svaki podatak ima izvor, link i vreme ažuriranja.

**Pravni okvir (potvrđeno iz medijskih izvora 2026-09-21, ipak proveriti sa pravnikom):** Zakon o izboru narodnih poslanika zabranjuje 48 časova pre dana glasanja i na dan glasanja do zatvaranja biračkih mesta da se u medijima i na javnim skupovima objavljuju procene rezultata izbora, javno predstavljaju kandidati i njihovi izborni programi i pozivaju birači da glasaju ili ne glasaju za određene liste. Kazne za pravna lica su 100.000–600.000 dinara. Izvori: [Danas](https://www.danas.rs/vesti/politika/izborna-tisina-kazne-zabrana/), [N1](https://n1info.rs/vesti/izbori-2023/izborna-tisina-pravila-kazne/), [Pravni portal](https://www.pravniportal.com/izborna-tisina/). Za 25. oktobar 2026. to znači da tišina počinje oko **23. oktobra u 00:00** (tačan trenutak proveriti) i traje do zatvaranja birališta u **20:00** na dan glasanja. Šta tačno spada pod "procene rezultata" (objavljene ankete? predikciono tržište?) i "javno predstavljanje kandidata" (kartica izbornih lista? vesti iz medija?) nije potvrđeno i mora da se pita pravnik pre izbora.

**Faze (fazni model, jedan servis za ceo sistem):**
- `NORMAL`: sve kao sada.
- `SILENCE` (od početka tišine do 20:00 na dan glasanja): sakriveno je sve što bi moglo biti "procena rezultata" (ankete, predikciono tržište), a ostalo po odluci pravnika (vesti, kartica lista). Prikazuje se samo neutralno: praktične informacije, tajmer do zatvaranja birališta, rokovi, izvori. Na dan glasanja (07–20h) i **izlaznost po satima** koju objavljuje RIK.
- `RESULTS_LIVE` (od 20:00 na dan glasanja): preliminarni rezultati RIK-a uživo.
- `RESULTS_FINAL`: konačni rezultati i proglašenje kada ih RIK objavi.
- Ručno pregažavanje faze iz admina (fail-safe, sa zapisom). Filtriranje se sprovodi i na **backendu** (API ne vraća ankete/tržište u tišini), ne samo na frontendu.

**Sadržaj i dizajn:**
- Drugačiji dizajn od svakodnevne početne: "uživo" tabla, tamna i kontrastna, krupni brojevi, minimalna navigacija, automatsko osvežavanje, mobilni prikaz prvo (najveći deo saobraćaja izborne noći), pristupačnost.
- Ekrani za mockup (Claude Design, kao do sada, pre kodiranja): (1) početna u tišini, (2) izborni dan sa izlaznošću po satima, (3) izborna noć sa rezultatima, (4) mobilni prikaz sva tri.
- **Izlaznost:** poslednji podatak, grafik po satima, izvor i vreme (RIK objavljuje izlaznost više puta tokom dana; tačne satnice proveriti).
- **Rezultati:** po listi (redosled po broju na listiću kao RIK, ne po rezultatu), glasovi i procenat, procenat obrađenih biračkih mesta, izborni prag kao činjenična linija, mandati **samo ako ih RIK objavi** (bez sopstvene raspodele mandata). Jasno: "preliminarni rezultati, nisu konačni".
- **Procene trećih strana** (paralelno brojanje CRTA/CeSID, exit poll) samo posle 20:00, jasno odvojeno od RIK brojeva i označeno kao procena, i samo ako pravnik potvrdi (isto načelo kao ankete naspram predikcionog tržišta).
- Bez državnog grba i bez zvaničnog izgleda; napomena da portal nije RIK.

**Podaci (V4.0 izviđanje pre bilo kog koda):** kako RIK objavljuje izlaznost i rezultate (u 2023. izlaznost tokom dana, preliminarni rezultati posle 20:00 uživo na RIK sajtu): proučiti stranice iz prethodnih izbora (struktura, format, da li postoji JSON/API), robots.txt i uslove korišćenja, pa izabrati pristup. **Uvek predvideti ručni unos iz admina kao rezervu** jer je struktura RIK stranica na izborni dan nepoznata i ne sme da zavisi jedan parser.

**Model (predlog):** `turnout_snapshot` (election_id, as_of, percentage, voters_count, source_url, entry_kind AUTO/MANUAL), `result_snapshot` (election_id, as_of, counted_stations_pct, turnout_pct, source_url) + `result_line` (snapshot_id, electoral_list_id ili raw_name, votes, percentage, seats), `election_phase_override` (ili konfiguracija sa vremenima: početak tišine, otvaranje i zatvaranje birališta). Snapshot-ovi se čuvaju (istorija), API vraća najnoviji. Javno: `GET /api/v1/elections/current/phase`, `/turnout`, `/results/latest`. Interno: unos i ispravke sa zapisom, kao kod anketa.

**Neopterećenost i pouzdanost (izborna noć je najveći skok saobraćaja):** kratak TTL keš (ISR/CDN, `stale-if-error`), osvežavanje na 30–60 s samo u relevantnoj fazi, statička rezerva (poslednji poznati JSON) ako BE padne, upozorenje kada je podatak stariji od N minuta, praćenje i dežurstvo.

**Provera pre izbora:** "simulacioni režim" (podesivo vreme i lažni ili istorijski podaci, npr. rezultati iz 2023.) i generalna proba ~19–21. oktobra, testiranje opterećenja, zamrzavanje koda ~22. oktobra.

**Milestone-i:** V4.0 izviđanje (RIK izvori, pravna pitanja, mockup ekrana) → V4.1 fazni servis + API + ručno pregažavanje (uključuje V3.6) → V4.2 početna u tišini → V4.3 izlaznost (unos + prikaz) → V4.4 rezultati (uvoz + prikaz + ručni unos) → V4.5 simulacioni režim i generalna proba → V4.6 opterećenje i pouzdanost.

**Vremenski okvir (danas je 2026-09-21):** tišina počinje ~23.10. Pošto V5 (deploy) mora da bude gotov pre izbora, praktičan redosled je V3.6 do ~2.10, **V5 do ~9.10**, V4 gotov do ~18.10, proba 19–21.10, zamrzavanje 22.10.

**Otvorena pitanja (za korisnika i pravnika):** (1) šta se sme prikazivati u tišini (ankete, tržište, vesti, liste); (2) tačan početak tišine; (3) prikaz procena trećih strana posle 20:00; (4) izvor rezultata samo RIK; (5) prikaz mandata; (6) ko radi ručni unos i dežura izborne noći; (7) kapacitet hostinga za skok saobraćaja; (8) da li posle izbora ostaje arhiva.

## V5 — Deployment

**(Izmena 2026-09-17) Deploy je namerno pomeren na kraj**, posle V2 i V3 — cela aplikacija se gradi i testira lokalno (FE :3000, BE :8080, Postgres :5432 iz `docker-compose.yml`) i tek kad je funkcionalno kompletna ide se na hosting. Domen se kupuje tek u ovom koraku, ne ranije — nema smisla plaćati zakup dok sajt nije spreman za javnost, niti vredi vrteti sajt sa domenom iz lokala (kućni internet ima dinamičku IP, treba port forwarding, nema lak https) — to je više rizika nego koristi u odnosu na to da se samo sačeka do stvarnog deploy-a.

- **Domen:** `izbori.rs` je zauzet (proveren whois 2026-09-17, registrovan od 2016, ističe 2026-10-16). `srbijaizbori.rs` je slobodan (proveren isti dan) — kandidat za registraciju u ovom koraku. Registracija ide preko RNIDS akreditovanog registrara (npr. Adriahost), ~2.150-2.600 RSD/god + PDV.
- FE: Vercel (Hobby plan, $0/mesečno — dovoljno za ovaj obim saobraćaja).
- BE: Render (Docker, Web Service). Free compute postoji ali se gasi posle 15 min neaktivnosti, što bi pauziralo i RIK scheduler u međuvremenu — razmotriti plaćeni compute (~$7/mesečno) ako se pokaže da free tier pravi probleme sa redovnošću osvežavanja.
- DB: Render PostgreSQL (managed). **Free tier ima 30-dnevni limit pa se baza briše** (+14 dana grace period) — neprihvatljivo za projekat koji treba da traje bar do i posle 25.10.2026. Ide se na najjeftiniji plaćeni tier (~$6/mesečno, 256MB RAM/1GB storage — dovoljno za ovaj dataset).
- Produkcija (kad se domen odabere): `<domen>` (Vercel/Next.js) → `api.<domen>` (Render/Spring Boot) → PostgreSQL.
- Env vars: `NEXT_PUBLIC_API_URL` (FE); `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `RIK_BASE_URL` (BE). Ništa osetljivo u Git-u.

**Pre deploy-a:** razmotriti HTTP cache headers (ili prost in-memory cache) na javnim `/api/v1/*` endpoint-ima — nema auth/rate-limiting u V1, a podaci se osvežavaju na 15 min, pa keširanje smanjuje opterećenje baze bez uvođenja Redis-a.

## V6 — Anketa posetilaca ("naša anketa")

**Status: predlog 2026-09-23, revidiran istog dana; odluke korisnika iz istog dana upisane niže (anonimno, dva prikaza rezultata, odmah otvorena, zatvara se sa tišinom, besplatna zaštita od botova). Popisne margine su izvučene (vidi "Popisni podaci"); čeka još pravnu proveru; mockup je odobren za implementaciju; **V6.1 (backend) i V6.2 (FE) su implementirani 2026-09-24**, admin (V6.3) nije.**

**Cilj:** *opt-in* anketa posetilaca portala: svako popunjava dobrovoljno i anonimno. Odgovara se na 2 kratka pitanja (glasačka namera i izlaznost) i 4 demografska (starost, pol, region, tip naselja), a rezultati se **ponderišu prema strukturi stanovništva** formulom niže. Rezultat je *anketa posetilaca portala*, ne istraživanje javnog mnjenja i ne izborna prognoza: uzorak je samoodabran pa nije reprezentativan, i tako se uvek označava. Cilj nije "naša CRTA", nego velika, transparentna anketa posetilaca sa jasno dokumentovanom metodologijom i otvoreno iskazanim ograničenjima.

### Odnos prema načelima portala (odluka za korisnika)

Portal je do sada namerno bez sopstvenih procena ("ne proizvodi političke procene"). Ova sekcija je prvo mesto gde portal sam prikuplja podatke, pa je rizik da ponderisan rezultat deluje kao pravo istraživanje. Ograde koje su deo dizajna, ne dodatak:
- Uvek u naslovu i pored svakog broja: **"Анкета посетилаца портала — није репрезентативно истраживање јавног мњења"**. Posebna sekcija i posebna stranica, nikad na istom grafiku ni u istom spisku kao agencijske ankete, bez zajedničkog proseka.
- **Ne prikazujemo marginu greške.** Margina važi samo za slučajan uzorak. Prikazujemo n, efektivni uzorak (n_eff) i napomenu o samoodabiru.
- Ponderisano je glavni prikaz, a **neponderisano** je uvek dostupno uz njega (isti ekran, sekundarno). Uz rezultat stoje veličina uzorka, period prikupljanja i link na metodologiju.
- Redosled odgovora u rezultatima je fiksan (broj na listiću), nikad po rezultatu; bez isticanja "pobednika".
- Ponderisanje ispravlja samo poznate razlike u strukturi (starost, region, tip naselja). Ne ispravlja to što se javljaju oni koji su motivisani da glasaju za određenu opciju ili koje je neko pozvao linkom. To piše u metodologiji.

### Pitanja (samo ona koja formula traži)

Ukupno **6 pitanja**, sva jednostruki izbor, sva obavezna. Ništa što ne ulazi u formulu se ne pita. **Redosled odgovora je fiksan**: liste u pitanju 1 idu po zvaničnom broju na listiću (kao RIK), a skala izlaznosti ima prirodan redosled. Ne mešamo ih nasumično: nasumičan redosled listi ne odgovara onome što glasač vidi na listiću, a poredan raspon ne sme da se meša. (Prvobitni predlog nasumičnog redosleda je povučen.)

**Deo A — šta se meri (ulazi u rezultat):**
1. **Glasačka namera:** "За коју листу планирате да гласате на изборима 25. октобра?" (datum je u pitanju, pa pitanje ostaje tačno do kraja).
   Odgovori: liste iz RIK-a (redni broj i naziv, po broju na listiću) + "Нисам одлучан/на" + "Нећу гласати" + "Не желим да одговорим".
   Služi za: procenu po listi i udeo neopredeljenih.
2. **Izlaznost:** "Колико је вероватно да ћете изаћи на изборе?"
   Odgovori: Сигурно ћу изаћи / Вероватно ћу изаћи / Вероватно нећу изаћи / Сигурно нећу изаћи.
   Služi za: osnovu "sigurno + verovatno izlaze".

**Deo B — ko odgovara (ulazi u težine, ne prikazuje se pojedinačno):**
3. **Starost:** 18–29 / 30–44 / 45–59 / 60+.
4. **Pol:** Мушко / Женско (dve kategorije jer popis daje samo te dve; pitanje je obavezno, bez opcije koja bi izbacila odgovor iz ponderisanja).
5. **Region** (zvanični statistički regioni, Kosovo i Metohija nije pokriven): Београд / Војводина / Шумадија и Западна Србија / Јужна и Источна Србија.
6. **Tip naselja:** kategorije su doslovno iz popisa (administrativno-pravni kriterijum RZS): **Градско насеље** („Град или варош“) / **Остало насеље** („Село“). Ne izmišljamo prag "velikog grada". Preslikavanje na popisnu kategoriju je 1:1.

Deo B su četiri margine raking-a (starost, pol, region, tip naselja); ciljni udeli su u odeljku "Popisni podaci".

### Formula (ponderisanje raking-om)

Ciljni udeli (margine) dolaze iz **jednog autoritativnog popisnog skupa** (predlog: **Popis 2022, RZS**), za stanovnike 18+, po svakoj dimenziji posebno; upisuju se u tabelu sa izvorom, godinom i linkom. **Brojeve ne izmišljamo** — prvi korak je da se izvuku iz popisa, a kategorije u anketi moraju da se poklope sa popisnim. Ne koristimo punu kombinaciju (4×2×4×2 = 64 ćelije) jer bi većina ćelija bila prazna pri malom uzorku, već raking (iterativno prilagođavanje) na četiri margine:

1. Svaki ispitanik i dobija početnu težinu `w_i = 1`.
2. Za svaku dimenziju d (starost, pol, region, naselje) redom: `w_i ← w_i · P_d(k) / Σ_j∈k w_j`, gde je k kategorija ispitanika i u dimenziji d, a `P_d(k)` ciljni udeo te kategorije.
3. Ponavlja se dok najveće odstupanje neke margine od cilja ne padne ispod 0,1 procentni poen (obično 10–30 krugova).
4. **Ograničenje težina:** posle svakog kruga težine se seku na [0,2 ; 5] prosečne težine (sprečava da nekoliko retkih ispitanika odlučuje o rezultatu), pa se raking nastavlja. Granice 0,2 i 5 su **predlog, ne utvrđena vrednost**: potvrđuju se simulacijom na probnim podacima pre V6.1 (vidi specifikaciju).
5. Procena za opciju j: `p_j = Σ_i w_i · [i je izabrao j] / Σ_i w_i` (na izabranoj osnovi: svi koji su odgovorili, ili samo opredeljeni koji sigurno izlaze).
6. **Efektivni uzorak:** `n_eff = (Σ w_i)² / Σ w_i²`; **design effect** `deff = n / n_eff`. Prikazujemo n i n_eff.
7. Ako neka kategorija neke margine ima manje od 10 ispitanika, kategorije se spajaju (npr. 45–59 i 60+) ili se ponderisani rezultat ne objavljuje.

Osnove rezultata (ista logika kao `result_basis` kod agencijskih anketa): svi odgovori / opredeljeni / opredeljeni koji sigurno izlaze (Q2 = "Сигурно ћу изаћи" ili "Вероватно ћу изаћи").

**Prikaz rezultata (odluka 2026-09-23): dva taba.** Broj odgovora je vidljiv od prvog ("Прикупљамо одговоре: N").
- **Tab 1 — „Сви одговори“ (bez formule):** sirovi, neponderisani rezultati. Prikazuje se odmah, koliko god da je odgovora.
- **Tab 2 — „По формули“ (ponderisano):** rezultati sa težinama iz formule. Dok ima manje od **50** odgovora tab je zaključan uz poruku "Још N одговора до прегледа по формули". Čim se skupi 50, **tab 2 postaje podrazumevani** (prvi koji se vidi), a tab 1 ostaje pored njega kao prikaz bez formule.
- Rizik koji prihvatamo svesno: sa 50 odgovora i četiri margine ponderisanje je nestabilno (mnoge kategorije imaju samo nekoliko ljudi). Zato uz tab 2 uvek stoje n, n_eff i upozorenje "мали узорак" dok n_eff ne pređe ~300; kategorije sa manje od 5 ispitanika se spajaju sa susednom, težine su ograničene (korak 4 formule), a ako se margine ne mogu zadovoljiti rezultat se ne prikazuje.
- Rezultati se preračunavaju po rasporedu (npr. na 15 minuta, kao ostali podaci) i čuvaju kao snimak; javno se vidi poslednji snimak sa vremenom izračunavanja.

### Kako rade profesionalne ankete i šta od toga možemo mi

Profesionalna anketa (CRTA, Ipsos, Faktor plus…) ima šest koraka. **Formula za ponderisanje je tek četvrti; ono što anketu čini profesionalnom je prvi korak, izbor uzorka.**

1. **Uzorak.** Slučajan, stratifikovan, višeetapan: strate su region × tip naselja; primarne jedinice (biračka mesta ili popisni krugovi) biraju se sa verovatnoćom srazmernom veličini; u njima se nasumično biraju domaćinstva, a u domaćinstvu ispitanik (npr. prema poslednjem rođendanu). Ispitivanje ide licem u lice ili telefonom, n obično 1000–2000. **Ovo mi ne možemo:** kod nas se javlja ko hoće (samoodabir).
2. **Margina greške** (samo za slučajan uzorak, nivo poverenja 95%): `MOE = 1,96 · √( p(1−p) / n )`, a sa ponderisanjem `MOE = 1,96 · √( deff · p(1−p) / n )`. Primer: n = 1000, p = 0,5 → ±3,1 p.p.; ako je deff = 2 → ±4,4 p.p. **Kod nas se ne prikazuje** (uzorak nije slučajan).
3. **Nonresponse i ciljni udeli.** Uz popis (RZS 2022) određuju se ciljni udeli po starosti, polu, regionu i tipu naselja.
4. **Ponderisanje.** Post-stratifikacija (jedna dimenzija) ili raking (više dimenzija).
   - Post-stratifikacija: `w_k = P_k / u_k`, gde je `P_k` udeo grupe k u populaciji, a `u_k` udeo u uzorku.
   - Raking: iterativno ponavljanje istog koraka po svakoj dimenziji dok se margine ne poklope (vidi gore). **Ovo možemo.**
5. **Modelovanje izlaznosti** ("likely voters"). Ispitanik se pušta u osnovu ako je izjavio da će izaći, ili mu se težina množi verovatnoćom izlaska: `w'_i = w_i · P(izlazak_i)`. Verovatnoće (npr. "сигурно" = 1, "вероватно" = 0,7…) su odluka agencije, ne prirodni zakon; zato je kod nas jednostavnije **prikazati dve osnove**: svi odgovori i samo "сигурно + вероватно".
6. **Neopredeljeni.** Ili se izbace ("među opredeljenima": `p_j = glasovi_j / Σ glasovi opredeljenih`), ili se prikažu posebno, ili se raspodele srazmerno. Agencije obično navode i udeo neopredeljenih. Kod nas: izbaciti i uvek prikazati udeo neopredeljenih.
   (Neke agencije dodatno ponderišu prema **glasu na prošlim izborima** koji ispitanik prijavi. Snažno je, ali zavisi od tačnog sećanja i zahteva zvanične rezultate za 2023; ne uvodimo u prvoj verziji.)

**Radni primer (ponderisanje).** Populacija: 40% mladi, 60% stariji. Uzorak (n = 1000): 80% mladi, 20% stariji. Za opciju A: mladi 70%, stariji 30%.
- Neponderisano: `0,8·0,70 + 0,2·0,30 = 62%`.
- Težine: mladi `0,40/0,80 = 0,5`; stariji `0,60/0,20 = 3,0`.
- Ponderisano: `(0,8·0,5·0,70 + 0,2·3,0·0,30) / (0,8·0,5 + 0,2·3,0) = 0,46 / 1,0 = 46%`, što je isto kao populacioni prosek `0,4·0,70 + 0,6·0,30`.
- Cena ponderisanja: `Σw = 800·0,5 + 200·3 = 1000`, `Σw² = 800·0,25 + 200·9 = 2000`, `n_eff = 1000² / 2000 = 500`, `deff = 2`. Ponderisanjem smo ispravili strukturu, ali nam je "vredeo" samo pola uzorka.

**Šta ovo znači za nas, iskreno:** možemo isto ponderisanje kao profesionalci (koraci 3–6), ali ne i njihov uzorak (korak 1). Zato je naš rezultat *ponderisana anketa posetilaca*, ne istraživanje javnog mnjenja, i ne dobija marginu greške.

### Nove liste u ponudi (bez talasa)

**Odluka 2026-09-23: nema talasa, anketa je jedna** i otvara se odmah, čim je portal javan. Glavne liste su već podnete; ako RIK naknadno proglasi novu listu, ona se samo **dodaje u ponudu** pitanja 1 (`survey_option.added_on`).
- Odgovori dati ranije ostaju u istom skupu. Nova lista nije mogla biti izabrana pre dodavanja, pa joj je udeo verovatno potcenjen; uz nju se u rezultatima navodi datum dodavanja.
- **Lista koju RIK odbije ili povuče skida se iz ponude i iz rezultata (odluka 2026-09-23, povučena lista ne ostaje u rezultatima).** Opcija dobija `active = false`. Odgovori koji su izabrali tu listu ne brišu se iz baze, ali se **ne računaju u pitanju 1** (kao da na to pitanje nisu odgovorili: ispadaju iz osnove i procenti ostalih se računaju bez njih); njihovi demografski podaci i odgovor o izlaznosti i dalje ulaze u težine i pitanje 2. Ako RIK vrati listu u važeće, opcija se ponovo aktivira i njeni odgovori se vraćaju u rezultate.

### Sprečavanje zloupotrebe i privatnost

- **Bez naloga, bez imena i bez IP adrese uz odgovor (odluka korisnika).** Ne pitamo ni ne čuvamo ime, prezime, JMBG, e-adresu ni broj telefona. Pitamo samo starost (grupa), pol, region, tip naselja i dva pitanja o glasanju. **IP adresa se ne čuva uz odgovor.** Tehnički se koristi: (a) potpisani kolačić "već ste odgovorili" (bez veze sa odgovorima), (b) ograničenje učestalosti po IP adresi **samo u memoriji servera**, (c) **jedan glas po mreži** (vidi sledeću tačku), (d) zaštita od botova — vidi niže. Ovo smanjuje višestruke odgovore, ne sprečava odlučnog napadača; otud i ograde.
- **Jedan glas po mreži (IP), odluka 2026-09-23.** Cilj korisnika: sprečiti više glasova sa iste IP adrese. Rešenje koje čuva anonimnost odgovora: pri slanju server izračuna `ip_hash = HMAC-SHA256(SURVEY_HASH_KEY, survey_id ‖ mreža)`, gde je mreža IPv4 adresa ili IPv6 prefiks /64; tajni ključ je samo u env varijabli, ne u bazi. Heš ide u **posebnu tabelu `survey_dedupe`** (`survey_id`, `ip_hash`, `count`; **bez vremena, bez id-ja odgovora, bez sekvencijalnog ključa**), a odgovor u `survey_response` sa **datumom bez vremena**, da se heš ne može povezati sa odgovorom po vremenu ubacivanja. Dozvoljeno je najviše `survey.max_per_network` odgovora po hešu (predlog **3**, da porodica ili kancelarija iza iste IP adrese ne ostane bez glasa; korisnik može da postavi 1). Tabela se **briše kad se anketa zatvori**, a ključ se rotira. IP u čistom obliku se ne upisuje nigde (ni u bazu, ni u log; isključiti IP iz pristupnih logova za ovu putanju).
  - **Ograničenja:** mobilni operateri i javne mreže dele IP (blokiraju se i pošteni korisnici), VPN i promena mreže zaobilaze proveru. Zato je ovo samo jedan sloj uz kolačić i Turnstile, ne garancija.
  - **Pravno:** heš IP adrese je izveden iz ličnog podatka i može se smatrati pseudonimizovanim ličnim podatkom po ZZPL, čak i kad je odvojen od odgovora; mora u obaveštenje o privatnosti (svrha: zaštita od višestrukog glasanja, rok čuvanja: do zatvaranja ankete) i na pravnu proveru.
- **Zaštita od botova: Cloudflare Turnstile** (besplatan, bez ograničenja broja provera, bez kartice; potreban je besplatan Cloudflare nalog). To je treća strana: pregledač posetioca pri slanju komunicira sa Cloudflare-om, pa to mora da stoji u obaveštenju o privatnosti; tvrdnja na stranici je zato "mi ne čuvamo", a ne "niko ne vidi IP". Alternativa ako se predomislimo: hCaptcha (besplatan nivo); reCAPTCHA se ne preporučuje zbog privatnosti.
- Uz odgovor čuvamo samo odgovore na 6 pitanja i datum (bez vremena); ne čuvamo IP adresu, user agent ni bilo šta što identifikuje osobu. Jedini trag mreže je posebna tabela sa ključnim hešom (vidi "Jedan glas po mreži"), bez veze sa odgovorom. Odgovor na pitanje o glasanju otkriva političko opredeljenje: čak i pseudonimizovano, to spada u posebnu vrstu podataka po **Zakonu o zaštiti podataka o ličnosti**, pa se anonimnost mora dokazati (nema veze odgovor–mreža). Ide na istu pravnu proveru kao izborna tišina.
- Admin (isti `ADMIN_API_KEY` mehanizam kao za ankete): zatvaranje ankete, isključivanje sumnjivih odgovora (nalet odgovora iz iste sekunde ili sa istog heša), izmena margina, pregled n po kategorijama. Bez naloga i bez pregleda pojedinačnih osoba.

### Popisni podaci (margine): izvučeno 2026-09-23

Izvor: **Popis stanovništva, domaćinstava i stanova 2022, konačni rezultati (RZS), Knjiga 2. „Starost i pol“** ([PDF](https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf), 746 strana; tabele 1.1 i 2 na stranama PDF-a 38–61 i dalje). Popis nije obuhvatio Kosovo i Metohiju. Brojevi su preuzeti iz tabela knjige (**stanovništvo staro 18 i više godina**, „пунолетно становништво“); kontrola: svaka margina sabira se na 5.492.020, a regioni se poklapaju sa zbirovima Srbija–sever (2.813.360) i Srbija–jug (2.678.660).

**Ukupno 18+: 5.492.020** (od 6.647.003 stanovnika svih uzrasta).

| Dimenzija | Kategorija | 18+ stanovnika | Udeo |
| --- | --- | --- | --- |
| Starost | 18–29 | 848.012 | 15,44% |
| | 30–44 | 1.325.378 | 24,13% |
| | 45–59 | 1.377.869 | 25,09% |
| | 60+ | 1.940.761 | 35,34% |
| Pol | Muški | 2.637.451 | 48,02% |
| | Ženski | 2.854.569 | 51,98% |
| Region | Београд | 1.380.388 | 25,13% |
| | Војводина | 1.432.972 | 26,09% |
| | Шумадија и Западна Србија | 1.502.710 | 27,36% |
| | Јужна и Источна Србија | 1.175.950 | 21,41% |
| Tip naselja | Градска | 3.382.634 | 61,59% |
| | Остала | 2.109.386 | 38,41% |

**Kako je izračunata starost:** knjiga daje petogodišnje grupe i posebno ukupan broj 18+. Grupa 18–29 = (18+ ukupno − zbir grupa 20–24 … 85+) [to je 18–19: 137.820] + 20–24 + 25–29; ostale grupe su zbirovi punih petogodišnjih grupa (30–34, 35–39, 40–44 = 30–44; 45–49, 50–54, 55–59 = 45–59; 60–64 … 85+ = 60+). Nema procena, sve iz tabele.

**Tip naselja (administrativno-pravni kriterijum RZS):** „градска“ su naselja koja su aktom lokalne samouprave dobila status grada, „остала“ su sva ostala (uključujući sela). Nema posebne kategorije „veliki grad“; ne izmišljamo prag. U formi: „Град или варош“ = градско, „Село“ = остало. Ispitanik sam procenjuje kojoj kategoriji pripada (nesigurnost samoprocene je poznato ograničenje). Beograd i Niš su naselja koja obuhvataju više gradskih opština, pa su celo „градска“.

**Ispravka ranije procene:** u ranijoj verziji ovog plana pisalo je da je oko 75% stanovništva gradsko. To je bilo iz rezimea u pretrazi i **netačno**: tabela daje 4.120.782 gradskog i 2.526.221 ostalog (62,0% / 38,0%) za sve uzraste, a 61,6% / 38,4% za 18+.

Ove brojeve ne menjamo ručno: ako RZS objavi ispravku, tabela `population_margin` se ažurira uz novu godinu/verziju izvora.

### Tekst upozorenja (nacrt, mora da ga pregleda pravnik)

Prikazuje se na vrhu `/anketa`, na kartici na početnoj i uz svaki rezultat (skraćeno):
> **Анкета посетилаца портала.** Ово је анонимна, добровољна анкета. Није истраживање јавног мњења, није репрезентативна и није прогноза изборног резултата: попуњавају је само посетиоци који то желе, па резултат одражава само њих. Не тражимо име, ЈМБГ ни е-адресу и не чувамо IP адресу уз ваше одговоре. Да бисмо спречили више одговора са исте мреже, чувамо само необратив, шифрован запис мреже, одвојен од одговора, који се брише када се анкета затвори. Питамо само старост, пол, регион, тип насеља и два питања о гласању. Листе су наведене редом са гласачког листића; портал не препоручује нити рангира ниједну листу. Портал није повезан са РИК-ом, политичким странкама нити агенцијама за истраживање јавног мњења.

Резултат "по формули" носи и посебну ноту: *„Резултати су прерачунати према структури становништва (старост, пол, регион, тип насеља, Попис 2022). То не поправља чињеницу да узорак није случајан.“*

### Izborna tišina

Objava rezultata ove ankete tretira se kao **procena rezultata**: ide pod isti prekidač faze kao ankete i tržište (V3.6/V4) i sakriva se u tišini i na dan glasanja do 20:00. **Odluka (2026-09-23): anketa se otvara odmah (čim je portal javan) i zatvara se automatski u trenutku početka tišine** (`closes_at` = isti trenutak kao prekidač iz V3.6, oko 23. oktobra u 00:00 — tačan trenutak potvrditi). Poslednji snimak rezultata vidljiv je do tada, a posle toga se sakriva; najkasnije objavljivanje je 22. oktobra uveče. Da li je i samo prikupljanje odgovora dozvoljeno u tišini pitamo pravnika; do odgovora se ne prikuplja.

### Model i API (predlog)

`survey` (slug, naslov, status DRAFT/OPEN/CLOSED, closes_at, min_n_publish) → `survey_question` (pozicija, tekst, uloga: VOTE_INTENTION / TURNOUT / AGE / SEX / REGION / SETTLEMENT) → `survey_option` (tekst, pozicija, `added_on`, `active`); `survey_response` (survey_id, submitted_on (samo datum, bez vremena), age_group, sex, region, settlement) + `survey_answer` (response_id, question_id, option_id); `survey_dedupe` (survey_id, ip_hash, count; bez vremena i bez veze sa odgovorom, briše se sa zatvaranjem ankete); `population_margin` (dimension, category, share, source, source_url, year); `survey_result_snapshot` (survey_id, computed_at, n, n_eff, deff, basis, rezultati po opciji kao JSON: ponderisano i neponderisano). Flyway migracije.
- `GET /api/v1/surveys/current` — pitanja i opcije.
- `POST /api/v1/surveys/{id}/responses` — validacija (sve opcije moraju pripadati pitanju, jedan odgovor, ograničenje učestalosti).
- `GET /api/v1/surveys/{id}/results` — poslednji snimak; 404/prazno dok nema `min_n_publish` ili dok je faza tišine (filtrira se **na backendu**, kao ankete i tržište u V4).
- `RakingWeighter` je čista klasa bez baze, sa jediničnim testovima (poznat primer, konvergencija, ograničenje težina, n_eff); računanje snimka radi Spring Scheduler.

### FE

- Nova stranica `/anketa` (objašnjenje i ograde na vrhu, forma od 6 pitanja (2 o glasanju, pa starost, pol, region, naselje), zahvalnica sa linkom na rezultate) i mala kartica na početnoj strani koja vodi na nju.
- Rezultati na `/anketa`: dva taba (vidi "Prikaz rezultata"), uz svaki veličina uzorka (n i n_eff), period prikupljanja, osnova, vreme izračunavanja, link na metodologiju i istaknuto upozorenje da uzorak nije reprezentativan.
- **Prvo mockup** (Claude Design), pa kod: ekrani (1) kartica na početnoj, (2) forma, (3) zahvalnica, (4) rezultati, (5) prazno stanje "prikupljamo odgovore", (6) mobilni.

### Metodološka specifikacija (mora biti zatvorena pre mockupa i koda)

| Stavka | Stanje |
| --- | --- |
| Autoritativni popisni skup | **Odlučeno i izvučeno:** Popis 2022 (RZS), Knjiga 2 „Starost i pol“, stanovništvo 18+; brojevi u „Popisni podaci“. |
| Dimenzije ponderisanja | Starost (4), pol (2), region (4), tip naselja (2: градска / остала), kategorije preuzete iz popisa. |
| Algoritam | Raking (iterativno proporcionalno prilagođavanje), formula gore. Kriterijum konvergencije 0,1 p.p., najviše ~50 krugova. |
| Ograničenje težina | **Otvoreno.** Predlog [0,2 ; 5] prosečne težine; potvrditi simulacijom. |
| Minimalni uzorak | **Odlučeno:** sirovi prikaz odmah (bez minimuma); prikaz po formuli od **50** odgovora, tada postaje podrazumevani tab; upozorenje "мали узорак" do n_eff ~300; kategorije sa manje od 5 ispitanika se spajaju. |
| Neponderisan i ponderisan rezultat | Oba se računaju istom formulom `p_j = Σ w_i·[izabrao j] / Σ w_i`, s tim što su kod neponderisanog sve težine 1. Osnove: svi odgovori / opredeljeni / opredeljeni koji sigurno ili verovatno izlaze. |
| Verzionisanje | **Nema talasa** (odluka): jedna anketa, nove liste se dodaju u ponudu. |
| Zaštita od zloupotrebe | **Odlučeno:** bez imena i bez IP adrese uz odgovor; **jedan glas po mreži** preko ključnog heša u posebnoj tabeli (max 3 po mreži, briše se sa zatvaranjem ankete); kolačić; Cloudflare Turnstile (besplatan); ograničenje učestalosti po IP-u samo u memoriji. |

### V6.1 Backend — implementirano (2026-09-24)

Paket `survey/` (controller, dto, entity, mapper, repository, service, weighting) i migracija `V12__add_survey.sql`.
- **Model:** `survey`, `survey_question`, `survey_option`, `survey_response` (samo datum, bez vremena), `survey_answer`, `survey_dedupe` (samo `survey_id`, `ip_hash`, `count`; bez id-ja odgovora i bez vremena), `population_margin` (seed sa brojevima iz Popisa 2022, 18+), `survey_result_snapshot`. Seed: anketa `posetioci-2026`, OPEN, otvorena od 2026-09-23, zatvara se 2026-10-23 00:00 (Beograd; početak tišine, potvrditi), 6 pitanja i njihove opcije. Opcije pitanja 1 su liste iz `electoral_list`, sinhronizovane job-om (nova lista se dodaje, odbijena ili povučena dobija `active = false`).
- **API:** `GET /api/v1/surveys/current`, `GET /api/v1/surveys/{id}`, `POST /api/v1/surveys/{id}/responses` (201 `{accepted:true}`, bez ikakvog id-ja), `GET /api/v1/surveys/{id}/results` (poslednji snimak; 404 pre otvaranja i posle zatvaranja).
- **Računanje:** `RakingWeighter` (čist, 13 testova, uključuje primer iz plana 62% → 46%) i `SurveyResultsCalculator` (sirovi i ponderisani procenti po osnovama "svi koji su izabrali listu" / "sigurno ili verovatno izlaze", n_eff, deff, tabela uzorak naspram stanovništva). Dimenzija sa kategorijom koja ima manje od 5 ispitanika se izostavlja i prijavljuje (`droppedDimensions`); težine su ograničene na [0,2 ; 5] prosečne težine; `smallSample` kad je n_eff < 300; ponderisano tek sa `min_weighted_responses` (50) odgovora. Odgovori za povučenu listu ispadaju iz pitanja 1, a i dalje ulaze u težine i pitanje 2.
- **Zaštita:** jedan glas po mreži (ključni HMAC-SHA256 heš, IPv6 na /64, najviše `max_per_network` = 3, 429 posle toga; nevažeći odgovori ne troše kvotu); ograničenje učestalosti po IP u memoriji (20 na sat); Cloudflare Turnstile (proverava se na serveru, IP se ne šalje Cloudflare-u; bez tajne provera je isključena); IP se nigde ne upisuje ni loguje. Job na 15 minuta: sinhronizacija listi, novi snimak, a posle `closes_at` briše se `survey_dedupe`.
- **Konfiguracija (env):** `SURVEY_HASH_KEY` (**obavezan**; bez njega slanje odgovora vraća 503), `TURNSTILE_SECRET` (prazno = bez provere, samo za razvoj), `SURVEY_TRUST_FORWARDED_HEADER=true` iza proksija (inače se IP čita sa soketa i svi iza proksija bi delili jedan). **FE:** ako forma šalje odgovor iz Next.js servera, mora da prosledi IP posetioca u `X-Forwarded-For`, inače BE vidi samo adresu FE servera.
- **Testovi:** 66 novih (jedinični: raking, kalkulator, heš, IP, ograničenje učestalosti, imena listi; integracioni kroz pravi HTTP i lokalnu bazu: prihvatanje, odbijanje nevažećih, limit po mreži, da se IP i vreme ne čuvaju, zatvorena anketa, povučena lista, prag ponderisanja). Ukupno BE: 117 testova.
- **Nije urađeno:** FE (V6.2: forma, kartica na početnoj, rezultati, kolačić "već ste odgovorili"), admin (V6.3: zatvaranje, isključivanje sumnjivih odgovora, izmena margina), povezivanje sa prekidačem tišine (V3.6), pravna provera. Snapshot se osvežava na 15 minuta, pa se odgovor pojavljuje u rezultatima tek posle sledećeg prerađivanja.

### V6.2 Frontend — implementirano (2026-09-24)

Po odobrenom mockupu (anketa odmah ispod odbrojavanja na početnoj).
- **Početna (revidirano 2026-09-24, tamna kompaktna kartica):** `SurveyHomeCard` je na **početku tamnog dela** (pre Ključnih datuma), a ne ispod hero dela. Ispod 1200px puna širina, od 1200px pola ekrana od leve ivice (`calc(50% - 12px)`, kao prva kolona vesti). Pitanje je naslov kartice, uz sitnu oznaku "Анкета посетилаца" i broj odgovora kao meta; ispod su tabovi "Сви одговори" / "По формули" i trake u boji `--visitor` (narandžasta, različita od zelene kojom su istraživanja agencija). **Prikazuje se samo prvih 5 listi (po broju na listiću, ne po rezultatu)**, dok se ne klikne "Прикажи све листе (N)" ("Прикажи мање"). Red "Нисам одлучан/на · Нећу гласати · Без одговora" je uvek vidljiv. Dugme "Попуни анкету" je normalne veličine, a upozorenje o nereprezentativnosti (sa oznakom "Мали узорак") je u sivoj traci na dnu. Prazno stanje: ista kartica sa prvih 4 liste bez vrednosti. Client deo je `SurveyHomeResults`, server `SurveyHomeCard`. **Predikciono tržište (2026-09-24)** više nije poseban široki odeljak na dnu: kompaktna kartica (oznaka i promet, naslov, dva vodeća ishoda, mali grafik, napomena da nije anketa, link na Polymarket; bez Yes/No i bez datuma zatvaranja) je u istom redu kao anketa, **30% širine ekrana, priljubljena uz desnu ivicu**, a anketa 50% uz levu; ispod 1200px jedna ispod druge u punoj širini. Kartica se ne prikazuje ako nema ankete ili je zatvorena (tišina). Početna ne čita kolačiće, pa ostaje statički keširana (revalidate 60 s).
- **`/anketa`:** upozorenje, forma sa 6 pitanja (`SurveyForm`, client, `useActionState`), zaštita od botova (Turnstile widget kad backend to traži i kad je `NEXT_PUBLIC_TURNSTILE_SITE_KEY` podešen), stanja "zatvorena" i "već ste odgovorili". Slanje ide preko Server Action-a (`app/anketa/actions.ts`) koji prosleđuje IP posetioca u `X-Forwarded-For`; posle uspeha postavlja httpOnly kolačić `anketa_<id>` (60 dana, samo pamti da je pregledač odgovorio) i vodi na `/anketa/hvala`.
- **`/anketa/rezultati`:** činjenice (odgovori, ef. uzorak, period, poslednji prerađun), pitanje 1 sa tabovima i izborom osnove, izlaznost, tabela uzorak naspram stanovništva, metodologija (`#metodologija`). Obaveštenja: "Мали узорак", izostavljene dimenzije, neusaglašene margine.
- **`/anketa/privatnost`:** opis šta se čuva i šta ne (činjenice iz sistema; **pravnik mora da pregleda, i treba dodati kontakt/rukovaoca i rok čuvanja odgovora posle izbora, što još nije odlučeno**).
- Zaglavlje ima link "Анкета", stranice "О порталу" i "Извори" opisuju anketu.
- **Produkcija:** FE env `NEXT_PUBLIC_TURNSTILE_SITE_KEY`; BE env `SURVEY_HASH_KEY`, `TURNSTILE_SECRET`, `SURVEY_TRUST_FORWARDED_HEADER=true`. Proveriti da li hosting prosleđuje pravi IP u `X-Forwarded-For` do Server Action-a.
- Provereno u pregledaču: prikaz na početnoj, slanje odgovora kroz formu (odgovor prihvaćen, kolačić postavljen, preusmeravanje), rezultati sa 71 probnim odgovorom (tabovi, upozorenja), zatim su probni podaci obrisani.

### Milestone-i i kalendar

V6.0 metodologija i podaci (margine iz popisa, odluke o pitanjima, pravna pitanja, mockup) → V6.1 BE model, API, `RakingWeighter` + testovi → V6.2 FE forma i početna kartica → V6.3 rezultati, snimci, admin → V6.4 zaštita od zloupotrebe i pregled privatnosti → povezivanje sa prekidačem tišine (V3.6).
Kalendar je tesan: V3.6 ~2.10, V5 deploy ~9.10, V4 ~18.10, tišina ~23.10. Da bi anketa imala smisla mora biti javno otvorena bar ~10 dana, pa V6.0–V6.3 treba da budu gotovi oko deploy-a (~9.10), a sama anketa se otvara čim sajt bude javan. Ako to ne stigne, odložiti V4 ili sažeti V6 (bez admin isključivanja, ručno računanje snimka).

**Realno očekivanje:** sa nekoliko stotina samoodabranih odgovora ponderisani rezultat je pre svega odraz toga ko čita sajt, uz dosta šuma. Vrednost sekcije je više u angažovanju i transparentnoj metodologiji nego u tačnosti; zato je označavanje kritično.

**Otvorena pitanja:** (1) pravna provera: da li se rezultati ove ankete smatraju "procenom rezultata", tačan početak zabrane, da li je prikupljanje u tišini dozvoljeno, i tekst upozorenja; (2) zaštita podataka: da li anonimni odgovori (kolačić, Turnstile kao treća strana) ostaju van Zakona o zaštiti podataka o ličnosti i šta ide u obaveštenje o privatnosti; (3) granice težina (simulacija); (4) učestalost osvežavanja snimka; (5) naziv sekcije; (6) da li se prihvata odstupanje od "bez sopstvenih procena" uz ograde iznad (korisnik je odlučio da ide, ograde ostaju obavezne).

**Odlučeno 2026-09-23:** anonimno bez ličnih podataka; dva taba rezultata (bez formule / po formuli, prag 50); Cloudflare Turnstile; anketa se otvara odmah i zatvara sa početkom tišine.

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

V1.0 skeleton → V1.1 Postgres+Flyway → V1.2 RIK integration → V1.3 election REST API → V1.4 countdown UI → V1.5 electoral lists UI → V1.6 responsive+SEO → V2.0-2.4 news → V3.0 prediction markets → V3.1 raspored početne (relayout) → V3.2 polls backend → V3.3 polls admin pregled → V3.4 polls discovery → V3.5 polls FE (početna + `/istrazivanja`) → V3.6 prekidač izborne tišine (posle pravne provere) → V3.7 timeline → V4.0–V4.6 izborni dan i izborna noć (vidi sekciju V4) → **V5.0 production deploy (domen + Vercel + Render + plaćeni Postgres)**. Predlog: V6.0–V6.4 anketa posetilaca (vidi sekciju V6, čeka odobrenje).

**Status (2026-09-21):** V1 i V2 gotovi, V3 gotov osim V3.6: V3.0 (Polymarket), V3.1 (relayout početne), V3.2 (model i javni API anketa), V3.3 (admin pregled), V3.4 (otkrivanje objava), V3.5 (prikaz anketa) i V3.7 (timeline). Ostaje **V3.6 (prekidač izborne tišine, posle pravne provere)**, zatim V4 (izborna noć, vidi sekciju V4) i V5 (deploy, u praksi pre V4). **Pre objave prve prave ankete proveriti pravilo o izbornoj tišini.**

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
- **V3.6 izborna tišina:** fazni servis (`NORMAL` / `SILENCE`) sa podesivim početkom i krajem (početak oko 23.10. 00:00, kraj 25.10. u 20:00, vidi sekciju V4 za pravni okvir), ručno pregažavanje iz admina i filtriranje na backendu: u tišini API ne vraća ankete ni predikciono tržište, a FE na njihovom mestu prikazuje napomenu. Uključiti tek posle pravne provere šta se sme. V4 ovo proširuje.
- **Prve dve prave ankete (2026-09-21):** CRTA / DAL Stanford (jun 2026, n=2.324, među opredeljenima) i Faktor Plus (avgust–septembar 2026, n=1.200, "odgovarali samo opredeljeni", prvi objavio Blic) uneti su i odobreni kroz admin, sa napomenom izvora. Brojevi su proveravani u sirovom tekstu članaka (Danas, N1, Nova, Informer, 021, Tanjug), imena opcija prepisana u ćirilicu po objavljenom tekstu. **Nalaze se samo u lokalnoj bazi**: za produkciju ih treba ponovo uneti kroz admin.
- **V3.7 timeline (gotovo):** tabela `election_event` (`V10`), `GET /api/v1/elections/current/events`, sekcija "Кључни датуми" na početnoj (prva u tamnom delu): vertikalna linija, prošli datumi prigušeni, sledeći istaknut sa "за N дана", svaki datum sa izvorom. Seed: raspisivanje izbora 9. septembra (Danas), rok za prijavu glasanja u inostranstvu 3. oktobra, rok za liste sa 10.000 potpisa 4. oktobra i rok za domaće posmatrače 17. oktobra (Mondo "EUpravo zato"), dan glasanja iz `election.election_date`. Novi događaji se za sada dodaju migracijom (admin za događaje nije potreban dok ih ima malo).

## Review napomene (2026-09-16)

- Verzije potvrđene: Spring Boot 4.1.1 je zaista trenutna stabilna verzija (izašla avgust 2026, spring.io). Next.js je na 16.3 kao najnovijoj minor verziji (App Router je default). Oba izbora su validna i aktuelna.
- RIK nema javno vidljiv REST/open-data API — samo web stranice (npr. `rik.parlament.gov.rs/zapisnici/...`). Jsoup scraping pristup je opravdan, ali HTML struktura nije garantovano stabilna — otud opravdano insistiranje na HTML fixture testovima za parser i na `data_import` audit tabeli.
- ~~Polymarket za ovu konkretnu izbornu trku je neizvesno u V3 obliku koji plan predviđa.~~ **Rešeno (V3.0):** koristi se događaj "Next Prime Minister of Serbia?" sa ishodom po kandidatu (ne po listi), pa je model `prediction_market_outcome` generički (name, price, ...). Nema tržišta po izbornoj listi, što se i ne prikazuje.
- ~~Pre V2 treba proveriti ToS/robots.txt za N1, Nova, Blic, Informer i da li neki od njih ima RSS.~~ **Provereno 2026-09-17: svi dozvoljavaju crawl i svi imaju RSS** — vidi detalje u sekciji V2.
- ~~Proveriti dostupnost domena izbori.rs pre V1.1 deploy koraka.~~ **Provereno 2026-09-17: izbori.rs zauzet, srbijaizbori.rs slobodan.** Kupovina domena pomerena u V5 (vidi ispod).
- **(Dodato) Pre RIK integracije (V1.2) proveriti robots.txt za `rik.parlament.gov.rs`.**
- **(Dodato) Pred V5 (production deploy) razmotriti HTTP caching na public API endpoint-ima zbog odsustva auth/rate-limiting-a u V1.**
- **(Dodato 2026-09-17) Deploy (V1.7 → V4 → sada V5) pomeren na kraj, posle V2/V3 — vidi sekciju V5 i "Način rada sa Claude-om".**
- **(Provereno 2026-09-21) Izvori anketa:** CRTA — robots.txt otvoren, ali automatski zahtev vraća 403. Faktor Plus — sajt blokira automatski pristup; rezultati stižu preko medija (Danas, RTS, Tanjug). CeSID — nema redovnih anketa o glasačkim namerama, nema RSS-a. Zaključak: automatski scraping anketa nije pouzdan put; radi se otkrivanje objava + ručno odobravanje (vidi sekciju Ankete).
- **(Provereno 2026-09-21) Činjenice iz izvora za mockup:** CRTA/DAL Stanford, terenski rad 10–24. jun 2026, licem u lice, n=2.324, među opredeljenima (70% uzorka) Studentska lista 44,9%, SNS 35,7%, SPS 3,8%; 12% neopredeljenih, 9% neće glasati. Faktor Plus, avgust–septembar 2026, n=1.200, terensko istraživanje, SNS 47,2%, studentska lista 31,5%, SPS 4,9%, proevropska koalicija 3,6%, NPS opcija 3,4%, Mi snaga naroda 2,9%, NADA 2,1%, SRS 2,0%; 31% neopredeljenih, 64% se izjasnilo da će glasati. Ovo pokazuje da se procenti odnose na različite osnove (opredeljeni birači u oba slučaja, ali različiti udeli neopredeljenih i različito imenovane opcije) — zato je osnova rezultata obavezno polje.
- **(Provereno 2026-09-21) Izborna tišina:** zakon zabranjuje 48 časova pre dana glasanja i do zatvaranja birališta objavljivanje "procena rezultata izbora" (detalji i izvori u sekciji V4). **Otvoreno:** da li se to odnosi na objavljene ankete i predikciono tržište, i šta još spada u tišinu; pitati pravnika pre 23.10.
- **(Otvoreno) Odluka o prikazu koalicija:** trenutno se ne prikazuje javno (interna evidencija `option_kind`/`composition`), vidi sekciju Ankete. Preispitati ako se pokaže da korisnici pogrešno porede "SNS" iz različitih anketa.

## Način rada sa Claude-om

Ovaj dokument se koristi kao project context/spec, ali implementacija ide milestone po milestone — ne generisati sve odjednom. Prvi implementacioni prompt: monorepo skeleton, Spring Boot projekat, Next.js projekat, Docker Postgres, provera da sva tri rade lokalno. Tek posle toga: model baze i RIK ingestion.

**(Dodato 2026-09-17)** Cela aplikacija (V1-V3) se gradi i verifikuje lokalno pre bilo kakvog hostinga. Deploy, kupovina domena i produkcioni troškovi dolaze tek na kraju, kao V5 — vidi sekciju V5 — Deployment.
