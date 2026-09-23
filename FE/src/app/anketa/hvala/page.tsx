import type { Metadata } from "next";
import { cookies } from "next/headers";
import Link from "next/link";
import { redirect } from "next/navigation";
import pageStyles from "@/app/page.module.css";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/SiteHeader";
import styles from "@/components/survey/survey.module.css";
import { formatBelgradeDateTime } from "@/components/survey/surveyFormat";
import { getCurrentSurvey } from "@/lib/api";
import { surveyCookieName } from "@/lib/surveyCookie";

export const metadata: Metadata = { title: "Хвала — Izbori 2026", robots: { index: false } };

export default async function HvalaPage() {
  const survey = await getCurrentSurvey();
  const cookieStore = await cookies();
  // Only a browser that has just answered belongs here.
  if (!survey || !cookieStore.has(surveyCookieName(survey.id))) redirect("/anketa");

  return (
    <div className={pageStyles.page}>
      <SiteHeader />
      <main className={pageStyles.wrap}>
        <div className={`${styles.themeScope} ${styles.pageMain}`}>
          <div className={styles.center}>
            <div className={styles.tick}>
              <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" style={{ color: "var(--sAccent)" }} aria-hidden="true">
                <path d="M5 12.5l4.5 4.5L19 7.5" />
              </svg>
            </div>
            <h1 className={styles.pageTitle}>Хвала, ваш одговор је забележен</h1>
            <p className={styles.lead} style={{ maxWidth: 520, margin: "0 auto" }}>
              Одговор је анониман. Резултати се освежавају на 15 минута, па се ваш одговор појављује у следећем
              прерачуну.
            </p>
            <div className={styles.actions}>
              {survey.resultsVisible && (
                <Link className={styles.submit} href="/anketa/rezultati">
                  Погледај резултате
                </Link>
              )}
              <Link className={styles.secondary} href="/">
                Назад на почетну
              </Link>
            </div>
            <p className={styles.fine} style={{ maxWidth: 520, margin: "18px auto 0" }}>
              Анкета се затвара {formatBelgradeDateTime(survey.closesAt)}, са почетком изборне тишине. Овај прегледач је
              већ одговорио, па поновно слање није могуће.
            </p>
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}
