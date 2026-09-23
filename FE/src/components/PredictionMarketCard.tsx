import styles from "@/app/page.module.css";
import type { PredictionMarket, PredictionMarketOutcome } from "@/lib/api";
import { formatRelativeSr } from "@/lib/format";
import PriceChart from "@/components/PriceChart";

const SERIES_COLORS = ["#5fbfb0", "#f0a24b"];
const SHOWN_OUTCOMES = 2;

function formatUsd(value: number): string {
  return `$${Math.round(value).toLocaleString("en-US")}`;
}

function DailyChange({ change }: { change: number | null }) {
  if (change === null) return null;
  const points = Math.round(Math.abs(change) * 100);
  if (points === 0) return null;
  const up = change > 0;
  return (
    <span className={up ? styles.changeUp : styles.changeDown}>
      <svg width="9" height="9" viewBox="0 0 9 9" aria-hidden="true">
        <path d={up ? "M4.5 1 8.5 8h-8z" : "M4.5 8 .5 1h8z"} fill="currentColor" />
      </svg>
      {points}%
    </span>
  );
}

function OutcomeRow({
  outcome,
  rank,
  color,
}: {
  outcome: PredictionMarketOutcome;
  rank: number;
  color: string;
}) {
  return (
    <div className={`${styles.pmRow} ${rank === 1 ? styles.pmRowLeader : ""}`}>
      {outcome.imageUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img className={styles.pmPhoto} src={outcome.imageUrl} alt={outcome.name} width={36} height={36} loading="lazy" />
      ) : (
        <div className={styles.pmPhoto} aria-hidden="true" />
      )}
      <div className={styles.pmName}>
        <div className={styles.pmNameLine}>
          <span className={styles.pmDot} style={{ background: color }} />
          <span className={styles.pmNameText}>{outcome.name}</span>
        </div>
        {outcome.volume !== null && <div className={styles.pmVolume}>{formatUsd(outcome.volume)} промет</div>}
      </div>
      <div className={styles.pmPct}>
        <span className={styles.pmPctValue}>{Math.round(outcome.price * 100)}%</span>
        <DailyChange change={outcome.oneDayPriceChange} />
      </div>
    </div>
  );
}

/**
 * Compact, read-only view of the market: two leading outcomes, the price history and a note that it is not a poll.
 * No buy/sell prompts. The width is decided by the row it sits in.
 */
export default function PredictionMarketCard({ market }: { market: PredictionMarket }) {
  const leaders = market.outcomes.slice(0, SHOWN_OUTCOMES);
  const series = leaders.map((outcome, i) => ({
    name: outcome.name,
    color: SERIES_COLORS[i],
    points: outcome.priceHistory ?? [],
  }));

  return (
    <section className={styles.pmCard} aria-labelledby="market-title">
      <div className={styles.pmTop}>
        <span className={styles.pmTag}>Предикционо тржиште</span>
        {market.volume !== null && (
          <span className={styles.pmMetaText}>
            <b>{formatUsd(market.volume)}</b> промет
          </span>
        )}
      </div>
      <h2 className={styles.pmTitle} id="market-title">
        {market.marketName}
      </h2>

      {leaders.map((outcome, i) => (
        <OutcomeRow key={outcome.name} outcome={outcome} rank={i + 1} color={SERIES_COLORS[i]} />
      ))}

      <PriceChart series={series} />

      <div className={styles.pmFooter}>
        <p className={styles.pmDisclaimer}>
          Цене тржишта нису подаци из анкета. Приказујемо два исхода са највишом ценом; ажурирано{" "}
          {formatRelativeSr(market.updatedAt)}.
        </p>
        <a className={styles.pmSourceLink} href={market.sourceUrl} target="_blank" rel="noopener noreferrer">
          Извор: Polymarket ↗
        </a>
      </div>
    </section>
  );
}
