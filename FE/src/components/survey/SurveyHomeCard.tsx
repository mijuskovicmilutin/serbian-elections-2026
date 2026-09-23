import Link from "next/link";
import type { Survey, SurveyResults } from "@/lib/api";
import SurveyHomeResults from "./SurveyHomeResults";
import SurveyInfoStrip from "./SurveyInfoStrip";
import { formatBelgradeDateTime } from "./surveyFormat";
import styles from "./survey.module.css";

const GHOST_ROWS = 4;

/**
 * Homepage card of the visitor survey, in the dark part of the page: the question and its results come first,
 * taking part is secondary, and the "not representative" notice sits in a quiet strip at the bottom. The width
 * is decided by the row it sits in (half the page on wide screens, full width on narrower ones).
 */
export default function SurveyHomeCard({ survey, results }: { survey: Survey; results: SurveyResults | null }) {
  const question = survey.questions.find((q) => q.role === "VOTE_INTENTION");
  const hasResults = results !== null && results.responseCount > 0;
  const ghostLists = (question?.options ?? []).filter((o) => o.kind === "CHOICE").slice(0, GHOST_ROWS);

  const actions = (
    <div className={styles.vcAct}>
      {survey.acceptingAnswers && (
        <Link className={styles.vcBtn} href="/anketa">
          Попуни анкету
        </Link>
      )}
      {survey.resultsVisible && hasResults && (
        <Link className={styles.vcLink} href="/anketa/rezultati">
          Сви резултати →
        </Link>
      )}
      <span className={styles.vcClose}>Затвара се {formatBelgradeDateTime(survey.closesAt)}</span>
    </div>
  );

  return (
    <>
      <section className={`${styles.vcCard} ${styles.themeScope}`} aria-labelledby="survey-home-title">
        <header className={styles.vcHead}>
          <span className={styles.vcTag}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <circle cx="9" cy="8" r="3.2" />
              <path d="M3 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5" />
              <circle cx="17" cy="9" r="2.5" />
              <path d="M17 14.6c2.3 0 4 1.6 4 4.4" />
            </svg>
            Анкета посетилаца
          </span>
          <span className={styles.vcMeta}>
            <b>{survey.responseCount}</b> одговора · освежава се на 15 минута
          </span>
        </header>

        <h2 className={styles.vcQ} id="survey-home-title">
          {question?.text ?? survey.title}
        </h2>

        {hasResults ? (
          <SurveyHomeResults results={results}>{actions}</SurveyHomeResults>
        ) : (
          <>
            <p className={styles.vcEmptyNote}>Још нема одговора. Будите први: траје две минуте и анонимно је.</p>
            <div>
              {ghostLists.map((option) => (
                <div className={`${styles.vcRow} ${styles.vcGhost}`} key={option.id}>
                  <span className={styles.vcNum}>{String(option.position).padStart(2, "0")}</span>
                  <div>
                    <div className={styles.vcName}>{option.label}</div>
                    <span className={styles.vcTrack} aria-hidden="true" />
                  </div>
                  <span className={styles.vcVal}>—</span>
                </div>
              ))}
            </div>
            {actions}
            <SurveyInfoStrip
              lead="Учествује ко жели, анонимно. Резултати се појављују чим стигну први одговори."
              smallSample={false}
            />
          </>
        )}
      </section>
    </>
  );
}
