import type { PollResult } from "@/lib/api";
import { formatResultPercent } from "@/lib/pollFormat";
import styles from "./polls.module.css";

/** Options in the order the source published them, all bars the same color, scale 0-100%. */
export default function PollBars({ results, limit }: { results: PollResult[]; limit?: number }) {
  const shown = limit === undefined ? results : results.slice(0, limit);
  return (
    <div>
      {shown.map((result) => (
        <div className={styles.barRow} key={result.displayOrder}>
          <span className={styles.barLabel}>{result.rawOptionName}</span>
          <span className={styles.barTrack} aria-hidden="true">
            <span className={styles.barFill} style={{ width: `${Math.min(100, Math.max(0, result.percentage))}%` }} />
          </span>
          <span className={styles.barValue}>{formatResultPercent(result.percentage)}</span>
        </div>
      ))}
    </div>
  );
}
