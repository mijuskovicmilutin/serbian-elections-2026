import styles from "./page.module.css";
import { getCurrentElection, getCurrentElectoralLists } from "@/lib/api";
import { formatDateSr, formatRelativeSr } from "@/lib/format";
import { PLACEHOLDER_NEWS_ROWS } from "@/lib/placeholderNews";
import Countdown from "@/components/Countdown";
import ElectoralListsPaginated from "@/components/ElectoralListsPaginated";
import SiteHeader from "@/components/SiteHeader";
import SiteFooter from "@/components/SiteFooter";

export default async function Home() {
  const [election, lists] = await Promise.all([
    getCurrentElection(),
    getCurrentElectoralLists(),
  ]);

  const targetIso = `${election.electionDate}T07:00:00+02:00`;
  const mostRecentlySeen = lists.reduce<string | null>((latest, list) => {
    if (!latest || list.lastSeenAt > latest) return list.lastSeenAt;
    return latest;
  }, null);

  return (
    <div className={styles.page}>
      <SiteHeader />

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
      </div>

      <main className={styles.wrap}>
        <section className={styles.newsSection}>
          <div className={styles.newsHead}>
            <h2>Вести о изборима</h2>
            <p>Најновији текстови из медија који прате изборе — водимо вас на изворни сајт.</p>
          </div>
          <div className={styles.newsRows}>
            {PLACEHOLDER_NEWS_ROWS.map((row) => (
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
                    <img className={styles.newsCardImg} src={item.image} alt="" loading="lazy" />
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

      <SiteFooter />
    </div>
  );
}
