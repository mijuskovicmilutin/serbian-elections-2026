import type { Metadata } from "next";
import styles from "@/app/page.module.css";
import SiteHeader from "@/components/SiteHeader";
import SiteFooter from "@/components/SiteFooter";

export const metadata: Metadata = {
  title: "Извори — Izbori 2026",
  description: "Одакле долазе подаци приказани на порталу и колико често се ажурирају.",
};

export default function IzvoriPage() {
  return (
    <div className={styles.page}>
      <SiteHeader />
      <main className={styles.wrap}>
        <section className={styles.article}>
          <h1 className={styles.articleH1}>Извори</h1>
          <p className={styles.articleLead}>
            Портал не прикупља податке на лицу места нити их сам процењује — сваки податак преузима се из
            јавно доступног извора и приказује се уз назначен извор, оригинални линк и време последњег
            ажурирања.
          </p>

          <div className={styles.sourceList}>
            <div className={styles.sourceCard}>
              <div className={styles.sourceCardHead}>
                <h2>Републичка изборна комисија (РИК)</h2>
                <span className={styles.sourceStatus}>активно</span>
              </div>
              <p>
                Изборне листе и статус пријаве/проглашења преузимају се са{" "}
                <a href="https://www.rik.parlament.gov.rs/" target="_blank" rel="noopener noreferrer">
                  rik.parlament.gov.rs
                </a>{" "}
                аутоматски, на сваких 15 минута. Портал не мења нити тумачи податке РИК-а — приказује их у
                изворном облику.
              </p>
            </div>

            <div className={styles.sourceCard}>
              <div className={styles.sourceCardHead}>
                <h2>Вести из медија</h2>
                <span className={`${styles.sourceStatus} ${styles.sourceStatusPending}`}>ускоро</span>
              </div>
              <p>
                Наслови и слике из политичких рубрика медија (нпр. N1, Nova, Blic, Informer) — само метаподаци,
                без преузимања пуног текста. Клик на вест увек води на изворни медиј.
              </p>
            </div>

            <div className={styles.sourceCard}>
              <div className={styles.sourceCardHead}>
                <h2>Анкете јавног мњења</h2>
                <span className={`${styles.sourceStatus} ${styles.sourceStatusPending}`}>ускоро</span>
              </div>
              <p>
                Објављена истраживања агенција за испитивање јавног мњења, са методологијом, узорком и датумом
                истраживања. Портал не рачуна сопствени &quot;polling score&quot; нити просек анкета.
              </p>
            </div>

            <div className={styles.sourceCard}>
              <div className={styles.sourceCardHead}>
                <h2>Predikciona tržišta</h2>
                <span className={`${styles.sourceStatus} ${styles.sourceStatusPending}`}>ускоро</span>
              </div>
              <p>
                Цене са платформи попут Polymarket-a, приказане у посебној целини, јасно одвојено од анкета.
                Ово нису подаци из анкета нити прогноза портала.
              </p>
            </div>
          </div>

          <p className={styles.articleNote}>
            Портал не производи сопствене политичке процене, рангирања изборних листа нити прогнозе победника —
            свака бројка на сајту потиче директно из означеног извора.
          </p>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
