"use client";

import { useState } from "react";
import type { SurveyOptionShare, SurveyResults, SurveyShareSet } from "@/lib/api";
import { DIMENSION_LABEL, formatNumber, formatPct } from "./surveyFormat";
import styles from "./survey.module.css";

type Mode = "raw" | "weighted";
type Basis = "all" | "likely";

/** The ballot numbers are the positions of the list options; the other options sit far above them. */
function isList(option: SurveyOptionShare): boolean {
  return option.kind === "CHOICE" && option.position < 1000;
}

function Bars({
  options,
  mode,
  twoColumns,
  quiet,
  numbered,
}: {
  options: SurveyOptionShare[];
  mode: Mode;
  twoColumns: boolean;
  quiet?: boolean;
  numbered?: boolean;
}) {
  const rows = twoColumns ? Math.ceil(options.length / 2) : options.length;
  return (
    <div
      className={twoColumns ? styles.bars : styles.barsSingle}
      style={twoColumns ? ({ "--rows": rows } as React.CSSProperties) : undefined}
    >
      {options.map((option) => {
        const value = mode === "weighted" ? option.weightedPct : option.rawPct;
        return (
          <div className={styles.barRow} key={option.optionId}>
            {numbered && isList(option) && <span className={styles.num}>{String(option.position).padStart(2, "0")}</span>}
            <div className={styles.barBody}>
              <div className={styles.barTop}>
                <span>{option.label}</span>
                <b>{formatPct(value)}</b>
              </div>
              <span className={styles.track} aria-hidden="true">
                <span
                  className={`${styles.fill} ${quiet ? styles.fillQuiet : ""}`}
                  style={{ width: `${Math.min(100, Math.max(0, value ?? 0))}%` }}
                />
              </span>
            </div>
          </div>
        );
      })}
    </div>
  );
}

/**
 * Question 1 (and on the full page also the turnout question) with the two views: every answer as it came in, and
 * the same answers recalculated to the structure of the population. The recalculated view is the default as soon
 * as the backend provides it. Options keep the ballot order, never sorted by result.
 */
export default function SurveyResultsView({ results, variant }: { results: SurveyResults; variant: "home" | "page" }) {
  const [mode, setMode] = useState<Mode>(results.weightedAvailable ? "weighted" : "raw");
  const [basis, setBasis] = useState<Basis>("all");

  const page = variant === "page";
  const set: SurveyShareSet = basis === "all" ? results.voteIntention.lists : results.voteIntention.likelyVoters;
  const weighting = results.weighting;
  const remaining = Math.max(0, results.minWeightedResponses - results.responseCount);
  const baseLabel =
    basis === "all" ? "међу онима који су изабрали листу" : "међу онима који ће сигурно или вероватно изаћи";

  return (
    <div>
      <div className={styles.tabs} role="tablist" aria-label="Приказ резултата">
        <button
          type="button"
          role="tab"
          aria-selected={mode === "raw"}
          className={`${styles.tab} ${mode === "raw" ? styles.tabActive : ""}`}
          onClick={() => setMode("raw")}
        >
          Сви одговори <small>(без формуле)</small>
        </button>
        {results.weightedAvailable ? (
          <button
            type="button"
            role="tab"
            aria-selected={mode === "weighted"}
            className={`${styles.tab} ${mode === "weighted" ? styles.tabActive : ""}`}
            onClick={() => setMode("weighted")}
          >
            По формули <small>(према структури становништва)</small>
          </button>
        ) : (
          <span className={`${styles.tab} ${styles.tabLocked}`} role="tab" aria-disabled="true">
            По формули <small>(још {remaining})</small>
          </span>
        )}
      </div>

      {!results.weightedAvailable && page && (
        <div className={styles.lockBox}>
          Преглед по формули отвара се када стигне {results.minWeightedResponses} одговора. До тада приказујемо само
          сирове одговоре, без прерачуна.
          <div className={styles.progressRow} style={{ margin: "8px 0 0" }}>
            <span>
              {results.responseCount} од {results.minWeightedResponses}
            </span>
            <span className={styles.track} aria-hidden="true">
              <span
                className={styles.fill}
                style={{ width: `${Math.min(100, (100 * results.responseCount) / results.minWeightedResponses)}%` }}
              />
            </span>
          </div>
        </div>
      )}

      {mode === "weighted" && weighting && page && (
        <>
          {weighting.smallSample && (
            <div className={styles.notice}>
              <span className={styles.noticeMark}>!</span>
              <div>
                <b>Мали узорак.</b> Ефективни узорак је {formatNumber(weighting.effectiveSampleSize)}. Резултати по
                формули су још веома несигурни и мењаће се како стижу нови одговори. Ово није истраживање јавног мњења.
              </div>
            </div>
          )}
          {weighting.droppedDimensions.length > 0 && (
            <div className={styles.notice}>
              <span className={styles.noticeMark}>!</span>
              <div>
                Није коришћена структура по: {weighting.droppedDimensions.map((d) => DIMENSION_LABEL[d] ?? d).join(", ")}
                . У некој категорији има премало одговора (мање од 5).
              </div>
            </div>
          )}
          {!weighting.converged && weighting.usedDimensions.length > 0 && (
            <div className={styles.notice}>
              <span className={styles.noticeMark}>!</span>
              <div>
                Прерачун није потпуно ускладио структуру узорка са становништвом (највеће одступање{" "}
                {weighting.maxDeviationPct.toFixed(1).replace(".", ",")} п.п.), јер су тежине ограничене.
              </div>
            </div>
          )}
        </>
      )}

      {page && (
        <div className={styles.basis} role="group" aria-label="Основа резултата">
          <button
            type="button"
            className={`${styles.pill} ${basis === "all" ? styles.pillOn : ""}`}
            onClick={() => setBasis("all")}
          >
            Сви који су изабрали листу
          </button>
          <button
            type="button"
            className={`${styles.pill} ${basis === "likely" ? styles.pillOn : ""}`}
            onClick={() => setBasis("likely")}
          >
            Само они који ће сигурно или вероватно изаћи
          </button>
        </div>
      )}

      {set.base === 0 ? (
        <div className={styles.empty}>Још нема одговора за овај приказ.</div>
      ) : (
        <Bars options={set.options} mode={mode} twoColumns={!page} numbered />
      )}

      {page && results.voteIntention.others.base > 0 && (
        <div className={styles.others}>
          <p className={styles.othersTitle}>Остали одговори (проценат свих одговора на ово питање)</p>
          <Bars options={results.voteIntention.others.options} mode={mode} twoColumns={false} quiet />
        </div>
      )}

      <div className={styles.foot}>
        <span>
          <b>{results.responseCount}</b> одговора
        </span>
        {mode === "weighted" && weighting && (
          <span>
            ефективни узорак <b>{formatNumber(weighting.effectiveSampleSize)}</b>
          </span>
        )}
        {mode === "weighted" && weighting?.smallSample && <span className={styles.smallSample}>Мали узорак</span>}
        <span>
          Проценти {baseLabel} ({set.base})
        </span>
      </div>
    </div>
  );
}
