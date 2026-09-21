import Link from "next/link";
import pageStyles from "@/app/page.module.css";
import type { Poll } from "@/lib/api";
import { formatDateSr } from "@/lib/format";
import { BASIS_LABEL, basisDetail, formatCount, formatFieldwork, hostOf, pollHeading } from "@/lib/pollFormat";
import PollBars from "./PollBars";
import SourceKindChip from "./SourceKindChip";
import styles from "./polls.module.css";

const SHOWN_OPTIONS = 3;

function Summary({ poll }: { poll: Poll }) {
  const fieldwork = formatFieldwork(poll);
  const meta = [
    fieldwork ? `Терен: ${fieldwork}` : null,
    poll.sampleSize !== null ? `${formatCount(poll.sampleSize)} испитаника` : null,
    poll.method,
    `објављено ${formatDateSr(poll.publishedAt)}`,
  ].filter(Boolean);
  const detail = basisDetail(poll);
  const more = poll.results.length - SHOWN_OPTIONS;
  const details = `/istrazivanja#poll-${poll.id}`;

  return (
    <div className={styles.block}>
      <div className={styles.blockHead}>
        <div className={styles.nameRow}>
          <h3 className={styles.name}>{pollHeading(poll)}</h3>
          <SourceKindChip kind={poll.sourceKind} />
        </div>
        <Link className={styles.link} href={details}>
          Детаљи →
        </Link>
      </div>
      <p className={styles.meta}>{meta.join(" · ")}</p>
      {poll.resultBasis && (
        <div className={styles.basisRow}>
          <span className={`${styles.chip} ${styles.chipBasis}`}>{BASIS_LABEL[poll.resultBasis]}</span>
          {detail && <span className={styles.basisDetail}>{detail}</span>}
        </div>
      )}
      <PollBars results={poll.results} limit={SHOWN_OPTIONS} />
      {poll.sourceNote && <p className={styles.sourceNote}>Напомена извора: {poll.sourceNote}</p>}
      <div className={styles.sourceRow}>
        <span className={styles.sourceText}>
          Извор:{" "}
          <a href={poll.sourceUrl} target="_blank" rel="noopener noreferrer">
            {hostOf(poll.sourceUrl)} ↗
          </a>
          {poll.sourceKind === "SECONDARY" && " (преузето из медија)"}
        </span>
        {more > 0 && (
          <Link className={styles.link} href={details}>
            Још {more} {more === 1 ? "опција" : more < 5 ? "опције" : "опција"} у детаљима →
          </Link>
        )}
      </div>
    </div>
  );
}

/** Homepage card: the newest approved poll of each pollster, newest first. */
export default function PollHomeCard({ polls }: { polls: Poll[] }) {
  return (
    <div className={`${pageStyles.listCardWrap} ${styles.lightScope}`}>
      <div className={pageStyles.listBandHead}>
        <p className={pageStyles.dividerLabel}>Истраживања јавног мњења</p>
        <p className={pageStyles.dividerSub}>Само проверена истраживања</p>
      </div>
      <p className={styles.intro}>
        Истраживања нису међусобно упоредива: мере различите узорке и понуђене опције. Уз сваки резултат је наведена
        основа на коју се односи. Приказујемо најновије истраживање сваке агенције, по датуму објаве, а опције у
        редоследу из извора.
      </p>
      {polls.map((poll) => (
        <Summary key={poll.id} poll={poll} />
      ))}
      <div className={styles.homeFoot}>
        <p className={styles.homeFootNote}>
          Резултате не рачунамо и не пондеришемо. Приказани су онако како их је објавила агенција.
        </p>
        <Link className={pageStyles.cardSourceLink} href="/istrazivanja">
          Сва истраживања →
        </Link>
      </div>
    </div>
  );
}
