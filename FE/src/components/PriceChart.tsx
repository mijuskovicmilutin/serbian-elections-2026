import styles from "@/app/page.module.css";
import type { PricePoint } from "@/lib/api";

type Series = { name: string; color: string; points: PricePoint[] };

const MONTHS_SHORT_SR = ["Јан", "Феб", "Мар", "Апр", "Мај", "Јун", "Јул", "Авг", "Сеп", "Окт", "Нов", "Дец"];
const TICK_STEP = 0.2;

export default function PriceChart({ series }: { series: Series[] }) {
  const drawable = series.filter((s) => s.points.length > 1);
  if (drawable.length === 0) return null;

  const all = drawable.flatMap((s) => s.points);
  const tMin = Math.min(...all.map((p) => p.t));
  const tMax = Math.max(...all.map((p) => p.t));
  const maxPrice = Math.max(...all.map((p) => p.p));
  const yMax = Math.max(TICK_STEP, Math.ceil(maxPrice / TICK_STEP) * TICK_STEP);

  const xPct = (t: number) => ((t - tMin) / (tMax - tMin)) * 100;
  const yPct = (p: number) => 100 - (p / yMax) * 100;

  const ticks: number[] = [];
  for (let v = 0; v <= yMax + 1e-9; v += TICK_STEP) ticks.push(Number(v.toFixed(2)));

  const monthMarks: { label: string; left: number }[] = [];
  const cursor = new Date(tMin * 1000);
  cursor.setDate(1);
  cursor.setHours(0, 0, 0, 0);
  cursor.setMonth(cursor.getMonth() + 1);
  while (cursor.getTime() / 1000 < tMax) {
    const left = xPct(cursor.getTime() / 1000);
    if (left > 4 && left < 96) monthMarks.push({ label: MONTHS_SHORT_SR[cursor.getMonth()], left });
    cursor.setMonth(cursor.getMonth() + 1);
  }

  const summary = drawable.map((s) => s.name).join(" и ");

  return (
    <div className={styles.chart} role="img" aria-label={`Кретање цена на тржишту: ${summary}.`}>
      <div className={styles.chartPlot}>
        {ticks.map((v) => (
          <div key={v} className={styles.chartGrid} style={{ top: `${yPct(v)}%` }}>
            <span className={styles.chartTick}>{Math.round(v * 100)}%</span>
          </div>
        ))}
        <svg className={styles.chartSvg} viewBox="0 0 1000 100" preserveAspectRatio="none" aria-hidden="true">
          {drawable.map((s) => (
            <polyline
              key={s.name}
              fill="none"
              stroke={s.color}
              strokeWidth={2}
              strokeLinejoin="round"
              strokeLinecap="round"
              vectorEffect="non-scaling-stroke"
              points={s.points.map((p) => `${xPct(p.t) * 10},${yPct(p.p)}`).join(" ")}
            />
          ))}
        </svg>
        {drawable.map((s) => {
          const last = s.points[s.points.length - 1];
          return (
            <span
              key={s.name}
              className={styles.chartDot}
              style={{ background: s.color, top: `${yPct(last.p)}%`, left: `${xPct(last.t)}%` }}
            />
          );
        })}
        <span className={styles.chartSource}>Извор: Polymarket.com</span>
      </div>
      <div className={styles.chartAxis}>
        {monthMarks.map((m) => (
          <span key={m.label + m.left} style={{ left: `${m.left}%` }}>
            {m.label}
          </span>
        ))}
      </div>
    </div>
  );
}
