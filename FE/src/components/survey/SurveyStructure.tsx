import type { SurveyResults } from "@/lib/api";
import { DIMENSION_TITLE, formatPct } from "./surveyFormat";
import styles from "./survey.module.css";

/** Sample against population: why the recalculation is needed. */
export function SurveyStructure({ results }: { results: SurveyResults }) {
  return (
    <div className={styles.block}>
      <h3 className={styles.blockTitle}>Структура узорка и становништва</h3>
      <p className={styles.blockNote}>
        Зато је потребан прерачун: међу онима који су одговорили нека група обично је заступљена више или мање него
        међу пунолетним становништвом Србије.
      </p>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>Ко је одговорио</th>
            <th>У узорку</th>
            <th>Међу пунолетнима у Србији</th>
          </tr>
        </thead>
        <tbody>
          {results.structure.map((dimension) => (
            <RowsOf key={dimension.dimension} dimension={dimension} />
          ))}
        </tbody>
      </table>
    </div>
  );
}

function RowsOf({ dimension }: { dimension: SurveyResults["structure"][number] }) {
  return (
    <>
      <tr className={styles.tableGroup}>
        <td colSpan={3}>{DIMENSION_TITLE[dimension.dimension] ?? dimension.dimension}</td>
      </tr>
      {dimension.categories.map((category) => (
        <tr key={category.code}>
          <td>{category.label}</td>
          <td>{formatPct(category.samplePct)}</td>
          <td>{formatPct(category.populationPct)}</td>
        </tr>
      ))}
    </>
  );
}

export function SurveyMethodology({ minWeighted }: { minWeighted: number }) {
  return (
    <details className={styles.how} id="metodologija" open>
      <summary>Како рачунамо резултат по формули</summary>
      <ol>
        <li>
          Свакој особи која је одговорила даје се тежина, тако да структура узорка по <b>старости, полу, региону и типу
          насеља</b> одговара становништву старијем од 18 година (Попис 2022, РЗС).
        </li>
        <li>
          Поступак се понавља (raking) док се све четири структуре не поклопе са пописом. Тежине су ограничене на
          0,2–5 просечне тежине, да неколико ретких одговора не одлучује о резултату. Ако у некој категорији има
          мање од 5 одговора, та структура се не користи и то пише уз резултат.
        </li>
        <li>
          Ефективни узорак је број одговора „умањен“ због тежина: <code>(Σw)² / Σw²</code>. Што је мањи од броја
          одговора, то је резултат несигурнији.
        </li>
        <li>
          Маргину грешке <b>не приказујемо</b>: она важи само за случајан узорак, а овде се јавља ко жели. Прерачун
          поправља структуру, али не поправља то ко се јавља.
        </li>
        <li>
          Преглед по формули отвара се од {minWeighted} одговора. Листе су увек по броју на гласачком листићу, не по
          резултату. Листа коју РИК одбије или повуче не улази у резултате питања о листи.
        </li>
      </ol>
      <p className={styles.fine}>
        Извор структуре становништва:{" "}
        <a
          href="https://publikacije.stat.gov.rs/G2023/pdf/G20234003.pdf"
          target="_blank"
          rel="noopener noreferrer"
        >
          Попис 2022, Књига 2 „Старост и пол“, РЗС ↗
        </a>
      </p>
    </details>
  );
}
