import styles from "./page.module.css";
import {
  getCurrentElection,
  getCurrentElectoralLists,
  getCurrentEvents,
  getCurrentPredictionMarket,
  getLatestPollPerPollster,
  getNewsBySource,
  type NewsSource,
} from "@/lib/api";
import { formatDateSr, formatRelativeSr } from "@/lib/format";
import Countdown from "@/components/Countdown";
import ElectoralListsPaginated from "@/components/ElectoralListsPaginated";
import PollHomeCard from "@/components/polls/PollHomeCard";
import PredictionMarketCard from "@/components/PredictionMarketCard";
import SiteHeader from "@/components/SiteHeader";
import SiteFooter from "@/components/SiteFooter";
import Timeline from "@/components/Timeline";

const RIK_URL = "https://www.rik.parlament.gov.rs";

const NEWS_SOURCES: { key: NewsSource; label: string; logo: string; logoClass: string }[] = [
  { key: "N1", label: "N1", logo: "/images/logos/n1.svg", logoClass: styles.logoN1 },
  { key: "NOVA", label: "Nova.rs", logo: "/images/logos/nova.svg", logoClass: styles.logoNova },
  { key: "BLIC", label: "Blic", logo: "/images/logos/blic.png", logoClass: styles.logoBlic },
  { key: "INFORMER", label: "Informer", logo: "/images/logos/informer.png", logoClass: styles.logoInformer },
];

/** Lists RIK published after this instant get a "нова" badge. */
function hoursAgoIso(hours: number): string {
  return new Date(Date.now() - hours * 3_600_000).toISOString();
}

export default async function Home() {
  const [election, lists, newsBySource, predictionMarket, polls, events] = await Promise.all([
    getCurrentElection(),
    getCurrentElectoralLists(),
    Promise.all(NEWS_SOURCES.map((s) => getNewsBySource(s.key))),
    getCurrentPredictionMarket(),
    getLatestPollPerPollster(),
    getCurrentEvents(),
  ]);

  const targetIso = `${election.electionDate}T07:00:00+02:00`;
  const mostRecentlySeen = lists.reduce<string | null>((latest, list) => {
    if (!latest || list.lastSeenAt > latest) return list.lastSeenAt;
    return latest;
  }, null);

  // Backend keeps the latest 5 per source; only 4 are shown to fill the fixed 4-column row.
  const newsRows = NEWS_SOURCES.map((s, i) => ({
    source: s.label,
    logo: s.logo,
    logoClass: s.logoClass,
    items: newsBySource[i].slice(0, 4),
  })).filter((row) => row.items.length > 0);

  const recentSince = hoursAgoIso(48);

  const market = predictionMarket && predictionMarket.outcomes.length > 0 ? predictionMarket : null;

  return (
    <div className={`${styles.page} ${styles.homePage}`}>
      <SiteHeader wide />

      <div className={styles.heroAndLists}>
        <div className={styles.megaPhoto}>
          <div className={styles.tintBlue}>
            <div className={styles.wideWrap}>
              <section className={styles.record}>
                <h1 className={styles.h1}>{election.name}</h1>
                <p className={styles.recordSubtitle}>Избори за народне посланике</p>
                <p className={styles.recordDate}>
                  Дан гласања: <b>{formatDateSr(election.electionDate)}</b> · бирачка места отворена 07–20ч
                </p>
                <Countdown targetIso={targetIso} />
              </section>
            </div>
          </div>
          <a
            className={styles.photoCredit}
            href="https://commons.wikimedia.org/wiki/File:Beograd_-_Narodna_skup%C5%A1tina_Republike_Srbije_(44881251532).jpg"
            target="_blank"
            rel="noopener noreferrer"
          >
            фото: Fred Romero, CC BY 2.0
          </a>
        </div>

        <div className={styles.listGrid}>
          <div className={styles.listCardWrap}>
            <div className={styles.listBandHead}>
              <p className={styles.dividerLabel}>
                Изборне листе са{" "}
                <a className={styles.titleLink} href={RIK_URL} target="_blank" rel="noopener noreferrer">
                  РИК
                </a>
              </p>
              <p className={styles.dividerSub}>Освежава се на 15 минута</p>
            </div>
            <div className={styles.listCard}>
              <div className={styles.sectionHead}>
                <h2>Изборне листе</h2>
                <span className={styles.sectionMeta}>
                  {lists.length} {lists.length === 1 ? "листа" : "листе"}
                  {mostRecentlySeen ? ` · ажурирано ${formatRelativeSr(mostRecentlySeen)}` : ""}
                </span>
              </div>
              <ElectoralListsPaginated lists={lists} recentSince={recentSince} />
            </div>
            <div className={styles.cardFooter}>
              <a className={styles.cardSourceLink} href={RIK_URL} target="_blank" rel="noopener noreferrer">
                Погледај на РИК-у ↗
              </a>
            </div>
          </div>

          {polls.length > 0 && <PollHomeCard polls={polls} />}
        </div>
      </div>

      <div className={styles.darkArea}>
        {(events.length > 0 || newsRows.length > 0 || market) && (
          <main className={styles.wideWrap}>
            {events.length > 0 && (
              <section className={styles.timelineSection}>
                <Timeline events={events} />
              </section>
            )}

            {newsRows.length > 0 && (
              <section className={styles.newsSection}>
                <div className={styles.darkHead}>
                  <h2>Вести о изборима</h2>
                  <p>Најновији текстови из медија који прате изборе — водимо вас на изворни сајт.</p>
                </div>
                <div className={styles.newsGrid}>
                  {newsRows.map((row) => (
                    <div className={styles.newsBlock} key={row.source}>
                      <div className={styles.newsBlockHead}>
                        <span className={`${styles.newsLogo} ${row.logoClass}`}>
                          {/* eslint-disable-next-line @next/next/no-img-element */}
                          <img src={row.logo} alt={row.source} />
                        </span>
                        <span className={styles.newsBlockHint}>Води на сајт медија ↗</span>
                      </div>
                      {row.items.map((item) => (
                        <a
                          className={styles.newsItem}
                          href={item.url}
                          target="_blank"
                          rel="noopener noreferrer"
                          key={item.url}
                        >
                          {item.imageUrl ? (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img className={styles.newsThumb} src={item.imageUrl} alt="" loading="lazy" />
                          ) : (
                            <span className={styles.newsThumb} aria-hidden="true" />
                          )}
                          <span>
                            <span className={styles.newsItemTitle}>{item.title}</span>
                            <span className={styles.newsItemMeta}>{formatRelativeSr(item.publishedAt)}</span>
                          </span>
                        </a>
                      ))}
                    </div>
                  ))}
                </div>
              </section>
            )}

            {market && (
              <section className={styles.predictionSection}>
                <div className={styles.darkHead}>
                  <h2>Предикционо тржиште</h2>
                  <p>Цене које корисници дају на Polymarket-у. Нису резултат анкете. Освежава се на 15 минута.</p>
                </div>
                <PredictionMarketCard market={market} />
              </section>
            )}
          </main>
        )}

        <SiteFooter wide />
      </div>
    </div>
  );
}
