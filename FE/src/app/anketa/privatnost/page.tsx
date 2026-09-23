import type { Metadata } from "next";
import pageStyles from "@/app/page.module.css";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/SiteHeader";
import styles from "@/components/survey/survey.module.css";

export const metadata: Metadata = {
  title: "Приватност анкете — Izbori 2026",
  description: "Које податке анкета посетилаца прикупља и шта се са њима ради.",
};

export default function PrivatnostPage() {
  return (
    <div className={pageStyles.page}>
      <SiteHeader />
      <main className={pageStyles.wrap}>
        <div className={`${styles.themeScope} ${styles.pageMain}`}>
          <h1 className={styles.pageTitle}>Приватност анкете посетилаца</h1>
          <p className={styles.lead}>
            Анкета је анонимна. Овде је описано шта тачно прикупљамо, шта користимо привремено и шта не чувамо.
          </p>

          <div className={styles.block}>
            <h3 className={styles.blockTitle}>Шта се чува уз ваше одговоре</h3>
            <p className={styles.blockNote}>
              Одговори на шест питања (два о гласању, и старосна група, пол, регион и тип насеља) и датум слања, без
              времена. Не тражимо и не чувамо име, ЈМБГ, е-адресу, број телефона ни IP адресу.
            </p>
          </div>

          <div className={styles.block}>
            <h3 className={styles.blockTitle}>Један одговор по мрежи</h3>
            <p className={styles.blockNote}>
              Да бисмо спречили да иста мрежа пошаље много одговора, у тренутку слања из ваше мреже (IPv4 адресе или
              првих 64 бита IPv6 адресе) прави се шифрован запис (кључни хеш) који се не може вратити у адресу. Чува се
              у посебној табели која садржи само тај запис и бројач, без везе са вашим одговорима и без времена. Са
              једне мреже може се послати највише три одговора. Ова табела се брише када се анкета затвори.
            </p>
          </div>

          <div className={styles.block}>
            <h3 className={styles.blockTitle}>Ограничење броја слања</h3>
            <p className={styles.blockNote}>
              Ради заштите од злоупотребе бројимо колико је одговора послато са исте адресе у последњих сат времена.
              Ово бројање постоји само у меморији сервера, не уписује се у базу ни у евиденцију и нестаје при
              поновном покретању.
            </p>
          </div>

          <div className={styles.block}>
            <h3 className={styles.blockTitle}>Колачић у вашем прегледачу</h3>
            <p className={styles.blockNote}>
              Након слања поставља се колачић који само памти да је овај прегледач већ одговорио, да вас не бисмо
              поново питали. Није повезан са вашим одговорима и истиче након 60 дана.
            </p>
          </div>

          <div className={styles.block}>
            <h3 className={styles.blockTitle}>Провера да нисте робот</h3>
            <p className={styles.blockNote}>
              Користимо Cloudflare Turnstile. Приликом слања ваш прегледач комуницира са Cloudflare-ом, који је трећа
              страна са сопственом политиком приватности. Наш сервер Cloudflare-у не шаље вашу адресу.
            </p>
          </div>

          <div className={styles.block}>
            <h3 className={styles.blockTitle}>Шта се објављује</h3>
            <p className={styles.blockNote}>
              Само збирни резултати, најчешће освежени на 15 минута. Појединачни одговори се не објављују. Анкета се
              затвара са почетком изборне тишине.
            </p>
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}
