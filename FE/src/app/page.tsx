import styles from "./page.module.css";
import {
  getCurrentElection,
  getCurrentElectoralLists,
  getCurrentPredictionMarket,
  getNewsBySource,
  type NewsSource,
} from "@/lib/api";
import { formatDateSr, formatRelativeSr } from "@/lib/format";
import Countdown from "@/components/Countdown";
import ElectoralListsPaginated from "@/components/ElectoralListsPaginated";
import SiteHeader from "@/components/SiteHeader";
import SiteFooter from "@/components/SiteFooter";

const NEWS_SOURCES: { key: NewsSource; label: string }[] = [
  { key: "N1", label: "N1" },
  { key: "NOVA", label: "Nova.rs" },
  { key: "BLIC", label: "Blic" },
  { key: "INFORMER", label: "Informer" },
];

export default async function Home() {
  const [election, lists, newsBySource, predictionMarket] = await Promise.all([
    getCurrentElection(),
    getCurrentElectoralLists(),
    Promise.all(NEWS_SOURCES.map((s) => getNewsBySource(s.key))),
    getCurrentPredictionMarket(),
  ]);

  const targetIso = `${election.electionDate}T07:00:00+02:00`;
  const mostRecentlySeen = lists.reduce<string | null>((latest, list) => {
    if (!latest || list.lastSeenAt > latest) return list.lastSeenAt;
    return latest;
  }, null);

  // Backend keeps the latest 5 per source; only 4 are shown to fill the fixed 4-column row.
  const newsRows = NEWS_SOURCES.map((s, i) => ({
    source: s.label,
    items: newsBySource[i].slice(0, 4),
  })).filter((row) => row.items.length > 0);

  const topOutcomes = predictionMarket?.outcomes.slice(0, 2) ?? [];

  return (
    <div className={styles.page}>
      <SiteHeader />

      <div className={styles.heroAndLists}>
        <div className={styles.megaPhoto}>
          <div className={styles.tintBlue}>
            <div className={styles.wrap}>
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
                Изборне листе са РИК.<span className={styles.liveBadge}>УЖИВО</span>
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
              <ElectoralListsPaginated lists={lists} />
            </div>
          </div>

          {predictionMarket && topOutcomes.length > 0 && (
            <div className={`${styles.listCardWrap} ${styles.predictionCardWrap}`}>
              <div className={styles.listBandHead}>
                <p className={styles.dividerLabel}>Предикционо тржиште</p>
                <p className={styles.dividerSub}>Polymarket</p>
              </div>
              <div className={styles.listCard}>
                <h2 className={styles.predictionTitle}>{predictionMarket.marketName}</h2>
                <div className={styles.predictionOutcomes}>
                  {topOutcomes.map((outcome) => (
                    <div className={styles.predictionOutcome} key={outcome.name}>
                      <span className={styles.predictionOutcomeName}>{outcome.name}</span>
                      <span className={styles.predictionOutcomePrice}>
                        {Math.round(outcome.price * 100)}%
                      </span>
                    </div>
                  ))}
                </div>
                <p className={styles.predictionDisclaimer}>Цене тржишта нису подаци из анкета.</p>
                <a
                  className={styles.predictionLink}
                  href={predictionMarket.sourceUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Погледај на Polymarket ↗
                </a>
              </div>
            </div>
          )}
        </div>
      </div>

      {newsRows.length > 0 && (
        <main className={styles.wrap}>
          <section className={styles.newsSection}>
            <div className={styles.newsHead}>
              <h2>Вести о изборима</h2>
              <p>Најновији текстови из медија који прате изборе — водимо вас на изворни сајт.</p>
            </div>
            <div className={styles.newsRows}>
              {newsRows.map((row) => (
                <div className={styles.newsRow} key={row.source}>
                  <div className={styles.newsRowLabel}>{row.source}</div>
                  {row.items.map((item) => (
                    <a
                      className={styles.newsCard}
                      href={item.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      key={item.url}
                    >
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img className={styles.newsCardImg} src={item.imageUrl ?? ""} alt="" loading="lazy" />
                      <span className={styles.newsCardBody}>
                        <span className={styles.newsCardTitle}>{item.title}</span>
                        <span className={styles.newsCardCat}>Политика</span>
                      </span>
                    </a>
                  ))}
                </div>
              ))}
            </div>
          </section>
        </main>
      )}

      <SiteFooter />
    </div>
  );
}
