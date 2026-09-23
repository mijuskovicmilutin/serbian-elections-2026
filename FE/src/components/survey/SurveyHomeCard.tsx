import Link from "next/link";
import type { Survey, SurveyResults } from "@/lib/api";
import SurveyResultsView from "./SurveyResultsView";
import { formatBelgradeDateTime } from "./surveyFormat";
import styles from "./survey.module.css";

/**
 * Homepage card of the visitor survey: how many answered, a big button to answer, and the results right here.
 * Always labelled as a visitors' survey, never as public-opinion research.
 */
export default function SurveyHomeCard({ survey, results }: { survey: Survey; results: SurveyResults | null }) {
  const question = survey.questions.find((q) => q.role === "VOTE_INTENTION");
  const hasResults = results !== null && results.responseCount > 0;

  return (
    <div className={styles.homeRow}>
      <section className={`${styles.homeCard} ${styles.lightScope}`} aria-labelledby="survey-home-title">
        <div className={styles.homeHead}>
          <h2 className={styles.homeTitle} id="survey-home-title">
            Анкета посетилаца портала
          </h2>
          <span className={styles.meta}>Освежава се на 15 минута</span>
        </div>
        <p className={styles.warning}>
          <b>Није репрезентативно истраживање јавног мњења.</b> Учествује ко жели, анонимно; резултат одражава само оне
          који су одговорили.
        </p>

        <div className={styles.homeGrid}>
          <div>
            <div className={styles.count}>
              <b>{survey.responseCount}</b>
              <span>одговора</span>
            </div>
            <p className={styles.homeMeta}>
              Шест кратких питања, око две минуте. Затвара се {formatBelgradeDateTime(survey.closesAt)}, са почетком
              изборне тишине.
            </p>
            {survey.acceptingAnswers && (
              <>
                <Link className={styles.cta} href="/anketa">
                  Попуни анкету <span aria-hidden="true">→</span>
                </Link>
                <span className={styles.ctaSub}>Анонимно · одговор се не везује за вас</span>
              </>
            )}
            {survey.resultsVisible && (
              <Link className={styles.textLink} href="/anketa/rezultati">
                Сви резултати и методологија →
              </Link>
            )}
          </div>

          <div>
            {question && <p className={styles.question}>{question.text}</p>}
            {hasResults ? (
              <SurveyResultsView results={results} variant="home" />
            ) : (
              <div className={styles.empty}>
                Још нема одговора. Будите први: анкета траје две минуте, а чим стигне први одговор овде се појављују
                резултати.
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
