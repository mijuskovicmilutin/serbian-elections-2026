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
import PredictionMarketCard from "@/components/PredictionMarketCard";
import SiteHeader from "@/components/SiteHeader";
import SiteFooter from "@/components/SiteFooter";

const NEWS_SOURCES: { key: NewsSource; label: string; logo: string; logoClass: string }[] = [
  { key: "N1", label: "N1", logo: "/images/logos/n1.svg", logoClass: styles.logoN1 },
  { key: "NOVA", label: "Nova.rs", logo: "/images/logos/nova.svg", logoClass: styles.logoNova },
  { key: "BLIC", label: "Blic", logo: "/images/logos/blic.png", logoClass: styles.logoBlic },
  { key: "INFORMER", label: "Informer", logo: "/images/logos/informer.png", logoClass: styles.logoInformer },
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
    logo: s.logo,
    logoClass: s.logoClass,
    items: newsBySource[i].slice(0, 4),
  })).filter((row) => row.items.length > 0);

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

          {predictionMarket && predictionMarket.outcomes.length > 0 && (
            <PredictionMarketCard market={predictionMarket} />
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
                  <div className={styles.newsRowLabel}>
                    <span className={`${styles.newsLogo} ${row.logoClass}`}>
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img src={row.logo} alt={row.source} />
                    </span>
                  </div>
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
