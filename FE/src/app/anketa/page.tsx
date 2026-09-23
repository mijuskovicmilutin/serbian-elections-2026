import type { Metadata } from "next";
import { cookies } from "next/headers";
import Link from "next/link";
import pageStyles from "@/app/page.module.css";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/SiteHeader";
import SurveyForm from "@/components/survey/SurveyForm";
import styles from "@/components/survey/survey.module.css";
import { getCurrentSurvey } from "@/lib/api";
import { surveyCookieName } from "@/lib/surveyCookie";

export const metadata: Metadata = {
  title: "Анкета посетилаца — Izbori 2026",
  description: "Анонимна, добровољна анкета посетилаца портала. Није репрезентативно истраживање јавног мњења.",
};

function StateBox({ title, text, action }: { title: string; text: string; action?: { href: string; label: string } }) {
  return (
    <div className={styles.stateBox}>
      <b>{title}</b>
      <p>{text}</p>
      {action && (
        <div className={styles.actions}>
          <Link className={styles.secondary} href={action.href}>
            {action.label}
          </Link>
        </div>
      )}
    </div>
  );
}

export default async function AnketaPage() {
  const survey = await getCurrentSurvey();
  const cookieStore = await cookies();
  const alreadyAnswered = survey ? cookieStore.has(surveyCookieName(survey.id)) : false;
  const siteKey = process.env.NEXT_PUBLIC_TURNSTILE_SITE_KEY || null;

  return (
    <div className={pageStyles.page}>
      <SiteHeader />
      <main className={pageStyles.wrap}>
        <div className={`${styles.themeScope} ${styles.pageMain}`}>
          <h1 className={styles.pageTitle}>Анкета посетилаца</h1>

          {!survey ? (
            <StateBox title="Анкета тренутно није доступна" text="Покушајте поново за који минут." />
          ) : (
            <>
              <p className={styles.lead}>Шест кратких питања, око две минуте. Анонимно је, а резултати су видљиви одмах.</p>
              <div className={styles.disclaimer}>
                <b>Анкета посетилаца портала</b>
                <p>
                  Ово је анонимна, добровољна анкета. Није истраживање јавног мњења, није репрезентативна и није
                  прогноза изборног резултата: попуњавају је само посетиоци који то желе, па резултат одражава само њих.
                  Не тражимо име, ЈМБГ ни е-адресу и не чувамо IP адресу уз ваше одговоре. Да бисмо спречили више
                  одговора са исте мреже, чувамо само необратив, шифрован запис мреже, одвојен од одговора, који се
                  брише када се анкета затвори. Питамо само старост, пол, регион, тип насеља и два питања о гласању.
                  Листе су наведене редом са гласачког листића; портал не препоручује нити рангира ниједну листу.
                  Портал није повезан са РИК-ом, политичким странкама нити агенцијама за истраживање јавног мњења.
                </p>
              </div>

              {!survey.acceptingAnswers ? (
                <StateBox
                  title="Анкета је затворена"
                  text="Анкета се затвара са почетком изборне тишине и више не прима одговоре."
                />
              ) : alreadyAnswered ? (
                <StateBox
                  title="Већ сте одговорили"
                  text="Овај прегледач је већ послао одговоре. Можете погледати резултате."
                  action={survey.resultsVisible ? { href: "/anketa/rezultati", label: "Погледај резултате" } : undefined}
                />
              ) : (
                <SurveyForm survey={survey} siteKey={siteKey} />
              )}
            </>
          )}
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}
