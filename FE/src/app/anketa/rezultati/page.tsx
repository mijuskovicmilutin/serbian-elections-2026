import type { Metadata } from "next";
import Link from "next/link";
import pageStyles from "@/app/page.module.css";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/SiteHeader";
import SurveyResultsView from "@/components/survey/SurveyResultsView";
import { SurveyMethodology, SurveyStructure } from "@/components/survey/SurveyStructure";
import styles from "@/components/survey/survey.module.css";
import { formatBelgradeDateTime, formatNumber } from "@/components/survey/surveyFormat";
import { getCurrentSurvey, getSurveyResults } from "@/lib/api";
import { formatDateSr, formatRelativeSr } from "@/lib/format";

export const metadata: Metadata = {
  title: "Резултати анкете посетилаца — Izbori 2026",
  description: "Резултати анонимне, добровољне анкете посетилаца, са методологијом. Није репрезентативно истраживање.",
};

function belgradeDay(iso: string): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Europe/Belgrade" }).format(new Date(iso));
}

export default async function RezultatiPage() {
  const survey = await getCurrentSurvey();
  const results = survey ? await getSurveyResults(survey.id) : null;

  return (
    <div className={pageStyles.page}>
      <SiteHeader />
      <main className={pageStyles.wrap}>
        <div className={`${styles.themeScope} ${styles.pageMain}`}>
          <h1 className={styles.pageTitle}>Резултати анкете посетилаца</h1>
          <div className={styles.disclaimer}>
            <b>Анкета посетилаца портала — није репрезентативно истраживање јавног мњења</b>
            <p>Учествује ко жели, анонимно. Резултат одражава само оне који су попунили анкету и није прогноза изборног резултата.</p>
          </div>

          {!survey || !results ? (
            <div className={styles.stateBox}>
              <b>Резултати нису доступни</b>
              <p>
                Резултати се приказују док анкета траје. Анкета се затвара са почетком изборне тишине, а у периоду
                изборне тишине резултати се не приказују.
              </p>
            </div>
          ) : results.responseCount === 0 ? (
            <div className={styles.stateBox}>
              <b>Још нема одговора</b>
              <p>Будите први: анкета траје две минуте и потпуно је анонимна.</p>
              {survey.acceptingAnswers && (
                <div className={styles.actions}>
                  <Link className={styles.submit} href="/anketa">
                    Попуни анкету
                  </Link>
                </div>
              )}
            </div>
          ) : (
            <>
              <div className={styles.facts}>
                <div className={styles.fact}>
                  <b>{results.responseCount}</b>
                  <span>одговора</span>
                </div>
                <div className={styles.fact}>
                  <b>{results.weighting ? formatNumber(results.weighting.effectiveSampleSize) : "—"}</b>
                  <span>ефективни узорак</span>
                </div>
                <div className={styles.fact}>
                  <b>
                    {formatDateSr(belgradeDay(results.opensAt))} – {formatDateSr(belgradeDay(results.computedAt))}
                  </b>
                  <span>период прикупљања</span>
                </div>
                <div className={styles.fact}>
                  <b>{formatRelativeSr(results.computedAt)}</b>
                  <span>последњи прерачун</span>
                </div>
              </div>

              <div className={styles.block}>
                <h3 className={styles.blockTitle}>
                  {survey.questions.find((q) => q.role === "VOTE_INTENTION")?.text}
                </h3>
                <SurveyResultsView results={results} variant="page" />
              </div>

              <div className={styles.block}>
                <h3 className={styles.blockTitle}>{survey.questions.find((q) => q.role === "TURNOUT")?.text}</h3>
                <TurnoutBars results={results} />
              </div>

              <SurveyStructure results={results} />
            </>
          )}

          {survey && <SurveyMethodology minWeighted={survey.minWeightedResponses} />}

          {survey && (
            <p className={styles.fine}>
              Анкета се затвара {formatBelgradeDateTime(survey.closesAt)}, са почетком изборне тишине.{" "}
              {survey.acceptingAnswers && <Link href="/anketa">Попуни анкету</Link>}
            </p>
          )}
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

function TurnoutBars({ results }: { results: NonNullable<Awaited<ReturnType<typeof getSurveyResults>>> }) {
  const mode = results.weightedAvailable ? "weighted" : "raw";
  return (
    <div style={{ marginTop: 12 }}>
      <p className={styles.blockNote}>
        {mode === "weighted" ? "По формули (према структури становништва)." : "Сви одговори, без формуле."}
      </p>
      {results.turnout.options.map((option) => {
        const value = mode === "weighted" ? option.weightedPct : option.rawPct;
        return (
          <div className={styles.barRow} key={option.optionId}>
            <div className={styles.barBody}>
              <div className={styles.barTop}>
                <span>{option.label}</span>
                <b>{value === null ? "—" : `${value.toFixed(1).replace(".", ",")}%`}</b>
              </div>
              <span className={styles.track} aria-hidden="true">
                <span className={styles.fill} style={{ width: `${Math.min(100, Math.max(0, value ?? 0))}%` }} />
              </span>
            </div>
          </div>
        );
      })}
    </div>
  );
}
