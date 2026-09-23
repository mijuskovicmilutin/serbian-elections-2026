"use client";

import { useState, type ReactNode } from "react";
import type { SurveyOptionShare, SurveyResults } from "@/lib/api";
import SurveyInfoStrip from "./SurveyInfoStrip";
import { formatPct } from "./surveyFormat";
import styles from "./survey.module.css";

type Mode = "raw" | "weighted";

/** Only this many lists are shown until the visitor asks for all of them (the first ones, by ballot number). */
export const COLLAPSED_LISTS = 5;

function pctOf(option: SurveyOptionShare, mode: Mode): number | null {
  return mode === "weighted" ? option.weightedPct : option.rawPct;
}

/**
 * Results part of the homepage card: two views (raw / recalculated to the population), the first five lists with a
 * "show all" toggle, the answers that are not a list, then the actions passed in as children and the info strip.
 * Lists stay in ballot order; nothing is sorted by result.
 */
export default function SurveyHomeResults({ results, children }: { results: SurveyResults; children: ReactNode }) {
  const [mode, setMode] = useState<Mode>(results.weightedAvailable ? "weighted" : "raw");
  const [expanded, setExpanded] = useState(false);

  const lists = results.voteIntention.lists.options;
  const shown = expanded ? lists : lists.slice(0, COLLAPSED_LISTS);
  const hidden = lists.length - COLLAPSED_LISTS;
  const remaining = Math.max(0, results.minWeightedResponses - results.responseCount);
  const weighting = results.weighting;

  return (
    <>
      <div className={styles.vcTabs} role="tablist" aria-label="Приказ резултата">
        <button
          type="button"
          role="tab"
          aria-selected={mode === "raw"}
          className={`${styles.vcTab} ${mode === "raw" ? styles.vcTabOn : ""}`}
          onClick={() => setMode("raw")}
        >
          Сви одговори
        </button>
        {results.weightedAvailable ? (
          <button
            type="button"
            role="tab"
            aria-selected={mode === "weighted"}
            className={`${styles.vcTab} ${mode === "weighted" ? styles.vcTabOn : ""}`}
            onClick={() => setMode("weighted")}
          >
            По формули <small>(према структури становништва)</small>
          </button>
        ) : (
          <span className={`${styles.vcTab} ${styles.vcTabLocked}`} role="tab" aria-disabled="true">
            По формули <small>(још {remaining})</small>
          </span>
        )}
      </div>

      <div>
        {shown.map((option) => {
          const value = pctOf(option, mode);
          return (
            <div className={styles.vcRow} key={option.optionId}>
              <span className={styles.vcNum}>{String(option.position).padStart(2, "0")}</span>
              <div>
                <div className={styles.vcName}>{option.label}</div>
                <span className={styles.vcTrack} aria-hidden="true">
                  <span className={styles.vcFill} style={{ width: `${Math.min(100, Math.max(0, value ?? 0))}%` }} />
                </span>
              </div>
              <span className={styles.vcVal}>{formatPct(value)}</span>
            </div>
          );
        })}
      </div>

      {hidden > 0 && (
        <div className={styles.vcToggle}>
          <button type="button" className={styles.vcMore} aria-expanded={expanded} onClick={() => setExpanded(!expanded)}>
            {expanded ? "Прикажи мање" : `Прикажи све листе (${lists.length})`}
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d={expanded ? "M6 15l6-6 6 6" : "M6 9l6 6 6-6"} />
            </svg>
          </button>
          <span className={styles.vcHint}>
            {expanded ? "Све листе, по броју на листићу" : `Приказано првих ${COLLAPSED_LISTS}, по броју на листићу`}
          </span>
        </div>
      )}

      {results.voteIntention.others.base > 0 && (
        <p className={styles.vcOthers}>
          {results.voteIntention.others.options.map((option) => (
            <span key={option.optionId}>
              {option.label}
              <b>{formatPct(pctOf(option, mode))}</b>
            </span>
          ))}
        </p>
      )}

      {children}

      <SurveyInfoStrip
        lead="Учествује ко жели, анонимно; резултат одражава само оне који су одговорили."
        smallSample={mode === "weighted" && Boolean(weighting?.smallSample)}
      />
    </>
  );
}
