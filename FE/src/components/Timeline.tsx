import type { CSSProperties } from "react";
import styles from "@/app/page.module.css";
import type { ElectionEvent } from "@/lib/api";
import { formatDateSr } from "@/lib/format";

function belgradeToday(): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Europe/Belgrade" }).format(new Date());
}

function daysUntil(fromIso: string, toIso: string): number {
  const [fy, fm, fd] = fromIso.split("-").map(Number);
  const [ty, tm, td] = toIso.split("-").map(Number);
  return Math.round((Date.UTC(ty, tm - 1, td) - Date.UTC(fy, fm - 1, fd)) / 86_400_000);
}

function daysWord(n: number): string {
  const lastTwo = n % 100;
  const last = n % 10;
  return last === 1 && lastTwo !== 11 ? "дан" : "дана";
}

/** Key dates in order, each with its source; past dates are dimmed and the next one is highlighted. */
export default function Timeline({ events }: { events: ElectionEvent[] }) {
  const today = belgradeToday();
  const nextId = events.find((e) => e.eventDate > today)?.id;

  return (
    <div className={styles.timeline}>
      <div className={styles.timelineHead}>
        <h2>Кључни датуми</h2>
        <p>Рокови и датуми изборног процеса, са изворима.</p>
      </div>
      <ol className={styles.timelineList} style={{ "--cols": events.length } as CSSProperties}>
        {events.map((event) => {
          const diff = daysUntil(today, event.eventDate);
          const state = diff < 0 ? styles.timelinePast : diff === 0 ? styles.timelineToday : "";
          const isNext = event.id === nextId;
          const isDay = event.type === "ELECTION_DAY";
          const tag =
            diff < 0 ? "прошло" : diff === 0 ? "данас" : `за ${diff} ${daysWord(diff)}`;
          return (
            <li className={`${styles.timelineItem} ${state} ${isNext ? styles.timelineNext : ""} ${isDay ? styles.timelineDay : ""}`} key={event.id}>
              <span className={styles.timelineDot} aria-hidden="true" />
              <div>
                <div className={styles.timelineDateRow}>
                  <time dateTime={event.eventDate}>{formatDateSr(event.eventDate)}</time>
                  <span className={styles.timelineTag}>{tag}</span>
                </div>
                <h3 className={styles.timelineTitle}>{event.title}</h3>
                {event.description && <p className={styles.timelineDesc}>{event.description}</p>}
                {event.sourceUrl && (
                  <a className={styles.timelineSource} href={event.sourceUrl} target="_blank" rel="noopener noreferrer">
                    Извор ↗
                  </a>
                )}
              </div>
            </li>
          );
        })}
      </ol>
    </div>
  );
}
