import styles from "@/app/page.module.css";
import type { PredictionMarket, PredictionMarketOutcome } from "@/lib/api";
import { formatDateSr, formatRelativeSr } from "@/lib/format";
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
        <img className={styles.pmPhoto} src={outcome.imageUrl} alt={outcome.name} width={56} height={56} loading="lazy" />
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

/** Compact, read-only view of the market: two leading outcomes and the price history, no buy/sell prompts. */
export default function PredictionMarketCard({ market }: { market: PredictionMarket }) {
  const leaders = market.outcomes.slice(0, SHOWN_OUTCOMES);
  const series = leaders.map((outcome, i) => ({
    name: outcome.name,
    color: SERIES_COLORS[i],
    points: outcome.priceHistory ?? [],
  }));

  return (
    <div className={styles.pmCard}>
      <div className={styles.pmGrid}>
        <div>
          <div className={styles.pmHeader}>
            <div className={styles.pmFlagTile} aria-hidden="true" />
            <h3 className={styles.pmTitle}>{market.marketName}</h3>
          </div>

          <div className={styles.pmMeta}>
            {market.volume !== null && (
              <span className={styles.pmMetaItem}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0z" />
                  <path d="M17 5h3v2a3 3 0 0 1-3 3M7 5H4v2a3 3 0 0 0 3 3" />
                </svg>
                {formatUsd(market.volume)} промет
              </span>
            )}
            {market.endDate && (
              <span className={styles.pmMetaItem}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 7v5l3 2" />
                </svg>
                Тржиште се затвара {formatDateSr(market.endDate.slice(0, 10))}
              </span>
            )}
          </div>

          {leaders.map((outcome, i) => (
            <OutcomeRow key={outcome.name} outcome={outcome} rank={i + 1} color={SERIES_COLORS[i]} />
          ))}
        </div>

        <PriceChart series={series} />
      </div>

      <div className={styles.pmFooter}>
        <p className={styles.pmDisclaimer}>
          Цене тржишта нису подаци из анкета. Приказујемо два исхода са највишом ценом; ажурирано{" "}
          {formatRelativeSr(market.updatedAt)}.
        </p>
        <a className={styles.pmSourceLink} href={market.sourceUrl} target="_blank" rel="noopener noreferrer">
          Извор: Polymarket ↗
        </a>
      </div>
    </div>
  );
}
