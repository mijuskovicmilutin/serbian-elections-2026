import type { Metadata } from "next";
import styles from "@/app/page.module.css";
import SiteHeader from "@/components/SiteHeader";
import SiteFooter from "@/components/SiteFooter";

export const metadata: Metadata = {
  title: "О порталу — Izbori 2026",
  description: "Шта је izbori2026.rs, чиме се бави и чиме се не бави.",
};

export default function OPortaluPage() {
  return (
    <div className={styles.page}>
      <SiteHeader />
      <main className={styles.wrap}>
        <section className={styles.article}>
          <h1 className={styles.articleH1}>О порталу</h1>
          <p className={styles.articleLead}>
            Izbori2026 је независан, непрофитан информациони портал који прикупља и приказује јавно доступне
            податке о парламентарним изборима у Србији заказаним за 25. октобар 2026. године. Портал није
            повезан са Републичком изборном комисијом (РИК), политичким странкама, медијским кућама нити
            агенцијама за истраживање јавног мњења.
          </p>
          <p className={styles.articleLead}>
            Циљ портала је да на једном месту буду прегледно организовани подаци који су иначе разбацани по
            различитим сајтовима и документима — изборне листе, вести, анкете и подаци са predikcionih tržišta.
            Портал не производи сопствене политичке процене, не рангира изборне листе и не даје прогнозе
            победника. Сваки приказани податак носи назначен извор, оригинални линк ка извору и време последњег
            ажурирања.
          </p>
          <p className={styles.articleNote}>
            Приметили сте грешку у подацима или застарелу информацију? Проверите оригинални извор на страници{" "}
            <a href="/izvori">Извори</a> — исправке се увек раде усклађивањем са изворним подацима, никада
            ручно.
          </p>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
