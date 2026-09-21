import type { ReactNode } from "react";
import type { Poll } from "@/lib/api";
import { formatDateSr } from "@/lib/format";
import {
  BASIS_LABEL,
  basisSentence,
  formatCount,
  formatFieldwork,
  formatPercent,
  hostOf,
  pollHeading,
} from "@/lib/pollFormat";
import PollBars from "./PollBars";
import SourceKindChip from "./SourceKindChip";
import styles from "./polls.module.css";

const NOT_STATED = "Није наведено у извору";

function ExternalLink({ href, children }: { href: string; children: ReactNode }) {
  return (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children}
    </a>
  );
}

/** Every poll shows the same fields in the same order; what the source does not state stays as a gray placeholder. */
function facts(poll: Poll): { label: string; value: ReactNode | null }[] {
  return [
    { label: "Терен (период)", value: formatFieldwork(poll) },
    { label: "Узорак", value: poll.sampleSize !== null ? `${formatCount(poll.sampleSize)} испитаника` : null },
    { label: "Метод", value: poll.method },
    { label: "Популација", value: poll.population },
    { label: "Маргина грешке", value: poll.marginOfError !== null ? `±${formatPercent(poll.marginOfError)}` : null },
    { label: "Наручилац", value: poll.commissionedBy },
    {
      label: "Удео опредељених у узорку",
      value: poll.decidedSharePct !== null ? `${formatPercent(poll.decidedSharePct)} испитаника` : null,
    },
    { label: "Неопредељени", value: poll.undecidedPct !== null ? formatPercent(poll.undecidedPct) : null },
    { label: "Неће гласати", value: poll.wontVotePct !== null ? formatPercent(poll.wontVotePct) : null },
    {
      label: "Изјаснило се да ће гласати",
      value: poll.willVotePct !== null ? formatPercent(poll.willVotePct) : null,
    },
    {
      label: "Оригинални извештај",
      value: poll.originalDocumentUrl ? (
        <ExternalLink href={poll.originalDocumentUrl}>{hostOf(poll.originalDocumentUrl)} ↗</ExternalLink>
      ) : null,
    },
    {
      label: "Медијски пренос",
      value:
        poll.mediaSources.length > 0
          ? poll.mediaSources.map((m, i) => (
              <span key={m.url}>
                {i > 0 && ", "}
                <ExternalLink href={m.url}>{m.name}</ExternalLink>
              </span>
            ))
          : null,
    },
  ];
}

export default function PollCard({ poll }: { poll: Poll }) {
  const sentence = basisSentence(poll);

  return (
    <article className={`${styles.card} ${styles.themeScope}`} id={`poll-${poll.id}`}>
      <div className={styles.blockHead}>
        <div className={styles.nameRow}>
          <h3 className={styles.cardTitle}>{pollHeading(poll)}</h3>
          <SourceKindChip kind={poll.sourceKind} />
        </div>
        <span className={styles.published}>Објављено {formatDateSr(poll.publishedAt)}</span>
      </div>

      <dl className={styles.facts}>
        {facts(poll).map((fact) => (
          <div className={`${styles.fact} ${fact.value === null ? styles.factMissing : ""}`} key={fact.label}>
            <dt className={styles.factLabel}>{fact.label}</dt>
            <dd className={styles.factValue}>
              {fact.value === null || fact.value === "" ? <span className={styles.missing}>{NOT_STATED}</span> : fact.value}
            </dd>
          </div>
        ))}
      </dl>

      {poll.resultBasis && (
        <div className={styles.basisBanner}>
          <span className={`${styles.chip} ${styles.chipBasis}`}>{BASIS_LABEL[poll.resultBasis]}</span>
          <span>{sentence}</span>
        </div>
      )}

      <PollBars results={poll.results} />
      {poll.sourceNote && <p className={styles.sourceNote}>Напомена извора: {poll.sourceNote}</p>}

      <div className={styles.cardFoot}>
        <a className={styles.link} href={poll.sourceUrl} target="_blank" rel="noopener noreferrer" style={{ fontSize: 14 }}>
          Извор: {hostOf(poll.sourceUrl)} ↗
        </a>
      </div>
    </article>
  );
}
