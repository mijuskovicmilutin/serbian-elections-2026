"use client";

import Link from "next/link";
import Script from "next/script";
import { useActionState, useEffect, useState } from "react";
import { submitSurvey, type SurveyFormState } from "@/app/anketa/actions";
import type { Survey, SurveyQuestion, SurveyRole } from "@/lib/api";
import styles from "./survey.module.css";

const INITIAL: SurveyFormState = { status: "idle", message: "" };
const ABOUT_VOTING: SurveyRole[] = ["VOTE_INTENTION", "TURNOUT"];

type Props = { survey: Survey; siteKey: string | null };

export default function SurveyForm({ survey, siteKey }: Props) {
  const [state, formAction, pending] = useActionState(submitSurvey, INITIAL);
  const [answers, setAnswers] = useState<Record<number, number>>({});

  const questions = [...survey.questions].sort((a, b) => a.position - b.position);
  const about = questions.filter((q) => ABOUT_VOTING.includes(q.role));
  const you = questions.filter((q) => !ABOUT_VOTING.includes(q.role));
  const answered = questions.filter((q) => answers[q.id] !== undefined).length;
  const botCheckBroken = survey.botCheckRequired && !siteKey;

  // A Turnstile token can be used once; after a refused submission the widget must issue a new one.
  useEffect(() => {
    if (state.status === "error") {
      (window as unknown as { turnstile?: { reset: () => void } }).turnstile?.reset();
    }
  }, [state]);

  const choose = (questionId: number, optionId: number) => setAnswers((prev) => ({ ...prev, [questionId]: optionId }));

  return (
    <form action={formAction}>
      <input type="hidden" name="surveyId" value={survey.id} />

      <div className={styles.progressRow} aria-live="polite">
        <span>
          Одговорено {answered} од {questions.length}
        </span>
        <span className={styles.track} aria-hidden="true">
          <span className={styles.fill} style={{ width: `${(100 * answered) / Math.max(1, questions.length)}%` }} />
        </span>
      </div>

      <h2 className={styles.section}>О гласању</h2>
      {about.map((q) => (
        <Question key={q.id} question={q} number={q.position} answers={answers} onChoose={choose} />
      ))}

      <h2 className={styles.section}>О вама</h2>
      <p className={styles.sectionNote}>
        Ово нам служи само да резултат прерачунамо према структури становништва. Ништа од овога вас не идентификује.
      </p>
      {you.map((q) => (
        <Question key={q.id} question={q} number={q.position} answers={answers} onChoose={choose} />
      ))}

      {survey.botCheckRequired &&
        (siteKey ? (
          <>
            <Script src="https://challenges.cloudflare.com/turnstile/v0/api.js" strategy="afterInteractive" />
            <div className={`cf-turnstile ${styles.turnstile}`} data-sitekey={siteKey} data-language="sr" />
          </>
        ) : (
          <p className={styles.error}>Провера да нисте робот није подешена. Слање тренутно није могуће.</p>
        ))}

      <div className={styles.actions}>
        <button type="submit" className={styles.submit} disabled={pending || answered < questions.length || botCheckBroken}>
          {pending ? "Шаље се…" : "Пошаљи одговоре анонимно"}
        </button>
        {answered < questions.length && (
          <span className={styles.optQuiet}>Одговорите на сва питања да бисте послали.</span>
        )}
      </div>

      {state.status === "error" && (
        <p className={styles.error} role="alert">
          {state.message}
        </p>
      )}

      <p className={styles.fine}>
        Слањем се ваши одговори бележе анонимно, без имена, а IP адреса се не чува уз одговоре. Ради заштите од
        вишеструких одговора чува се само необратив запис мреже, одвојен од одговора, до затварања анкете. Одговарате
        једном по прегледачу. Заштита од ботова ради преко Cloudflare-а (трећа страна).{" "}
        <Link href="/anketa/privatnost">Приватност</Link> · <Link href="/anketa/rezultati#metodologija">Методологија</Link>
      </p>
    </form>
  );
}

function Question({
  question,
  number,
  answers,
  onChoose,
}: {
  question: SurveyQuestion;
  number: number;
  answers: Record<number, number>;
  onChoose: (questionId: number, optionId: number) => void;
}) {
  const selected = answers[question.id];
  const asList = question.role === "VOTE_INTENTION";
  const name = `q_${question.id}`;

  return (
    <fieldset className={styles.fieldset}>
      <legend className={styles.legend}>
        <span className={`${styles.qNum} ${selected !== undefined ? styles.qDone : ""}`} aria-hidden="true">
          {selected !== undefined ? "✓" : number}
        </span>
        {question.text}
      </legend>

      {asList ? (
        <div className={styles.optList}>
          {question.options.map((option) => {
            const isList = option.kind === "CHOICE" && option.position < 1000;
            return (
              <label
                key={option.id}
                className={`${styles.opt} ${selected === option.id ? styles.optSelected : ""} ${isList ? "" : styles.optQuiet}`}
              >
                <input
                  className={styles.radio}
                  type="radio"
                  name={name}
                  value={option.id}
                  checked={selected === option.id}
                  onChange={() => onChoose(question.id, option.id)}
                />
                {isList && <span className={styles.num}>{String(option.position).padStart(2, "0")}</span>}
                <span>{option.label}</span>
              </label>
            );
          })}
        </div>
      ) : (
        <div className={styles.pills}>
          {question.options.map((option) => (
            <label
              key={option.id}
              className={`${styles.pillChoice} ${selected === option.id ? styles.pillChoiceOn : ""}`}
            >
              <input
                type="radio"
                name={name}
                value={option.id}
                checked={selected === option.id}
                onChange={() => onChoose(question.id, option.id)}
              />
              {option.label}
            </label>
          ))}
        </div>
      )}

      {question.role === "SETTLEMENT" && (
        <p className={styles.help}>
          „Град или варош“ је насеље које има статус града по одлуци локалне самоуправе; сва остала насеља рачунају се
          као „Село“.
        </p>
      )}
    </fieldset>
  );
}
