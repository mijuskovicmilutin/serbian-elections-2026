import type { Metadata } from "next";
import styles from "@/app/page.module.css";
import SiteHeader from "@/components/SiteHeader";
import SiteFooter from "@/components/SiteFooter";

export const metadata: Metadata = {
  title: "Извори — Izbori 2026",
  description: "Одакле долазе подаци приказани на порталу и колико често се ажурирају.",
};

function ExtLink({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children}
    </a>
  );
}

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
                Изборне листе и њихов редни број преузимају се са{" "}
                <ExtLink href="https://www.rik.parlament.gov.rs/">rik.parlament.gov.rs</ExtLink> аутоматски, на
                сваких 15 минута. Портал не мења нити тумачи податке РИК-а — приказује их у изворном облику, уз
                линк ка документу листе.
              </p>
            </div>

            <div className={styles.sourceCard}>
              <div className={styles.sourceCardHead}>
                <h2>Вести из медија</h2>
                <span className={styles.sourceStatus}>активно</span>
              </div>
              <p>
                Наслови, слике и линкови преко јавних RSS feed-ова, на сваких 15 минута; за сваки медиј
                приказујемо 4 најновије вести. Не преузимамо пун текст, а клик увек води на сајт медија.
              </p>
              <ul>
                <li>
                  <ExtLink href="https://n1info.rs">N1</ExtLink> — општа рубрика „Вести“ (N1 нема посебну
                  политичку рубрику)
                </li>
                <li>
                  <ExtLink href="https://nova.rs">Nova.rs</ExtLink> — рубрика политика
                </li>
                <li>
                  <ExtLink href="https://www.blic.rs">Blic</ExtLink> — рубрика Вести / Политика
                </li>
                <li>
                  <ExtLink href="https://informer.rs">Informer</ExtLink> — рубрика политика
                </li>
              </ul>
              <p style={{ marginTop: 10 }}>
                Вести нису филтриране по теми избора, па се повремено појави и вест која нема везе са њима.
              </p>
            </div>

            <div className={styles.sourceCard}>
              <div className={styles.sourceCardHead}>
                <h2>Истраживања јавног мњења</h2>
                <span className={styles.sourceStatus}>активно</span>
              </div>
              <p>
                Приказујемо само истраживања агенција са познатом методологијом. Свако истраживање се ручно
                проверава пре објаве, а уз резултате је наведено на шта се проценти односе. Не рачунамо просек
                анкета нити „polling score“. Оно што сам извор напомиње о бројкама приказујемо као „Напомена извора“.
              </p>
              <ul>
                <li>
                  <ExtLink href="https://crta.rs">CRTA</ExtLink> — примарни извор, бројеве уносимо из објављених
                  извештаја
                </li>
                <li>
                  Faktor plus — секундарни извор: резултате преносе медији (нпр. Danas, Blic, 021), па је уз
                  истраживање наведено ко их је пренео
                </li>
                <li>CeSID — још нема објављеног истраживања о гласачким намерама</li>
              </ul>
            </div>

            <div className={styles.sourceCard}>
              <div className={styles.sourceCardHead}>
                <h2>Анкета посетилаца портала</h2>
                <span className={styles.sourceStatus}>активно</span>
              </div>
              <p>
                Ово је наша анкета, а не туђи податак: анонимна је, добровољна и није репрезентативна. Одговори се
                прерачунавају према структури становништва старијег од 18 година (старост, пол, регион, тип насеља) по
                подацима{" "}
                <ExtLink href="https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf">
                  Пописа 2022, Књига 2 „Старост и пол“, РЗС
                </ExtLink>
                . Резултати се освежавају на 15 минута, а анкета се затвара са почетком изборне тишине. Погледајте{" "}
                <a href="/anketa/rezultati#metodologija">методологију</a> и <a href="/anketa/privatnost">приватност</a>.
              </p>
            </div>

            <div className={styles.sourceCard}>
              <div className={styles.sourceCardHead}>
                <h2>Предикционо тржиште</h2>
                <span className={styles.sourceStatus}>активно</span>
              </div>
              <p>
                Цене и историја цена за питање „Next Prime Minister of Serbia?“ преузимају се преко јавног API-ја
                платформе <ExtLink href="https://polymarket.com">Polymarket</ExtLink>, на сваких 15 минута.
                Приказујемо два исхода са највишом ценом. Ово нису подаци из анкета нити прогноза портала, и
                портал не нуди куповину ни клађење.
              </p>
            </div>

            <div className={styles.sourceCard}>
              <div className={styles.sourceCardHead}>
                <h2>Кључни датуми</h2>
                <span className={styles.sourceStatus}>активно</span>
              </div>
              <p>
                Рокови и датуми изборног процеса унети су на основу објављених одлука и медијских извештаја (нпр.
                Danas, eUpravo za to), а уз сваки датум стоји линк на извор.
              </p>
            </div>

            <div className={styles.sourceCard}>
              <div className={styles.sourceCardHead}>
                <h2>Фотографија</h2>
              </div>
              <p>
                Фотографија Народне скупштине на почетној страни: Fred Romero,{" "}
                <ExtLink href="https://commons.wikimedia.org/wiki/File:Beograd_-_Narodna_skup%C5%A1tina_Republike_Srbije_(44881251532).jpg">
                  Wikimedia Commons
                </ExtLink>
                , лиценца CC BY 2.0.
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
