import Link from "next/link";
import styles from "./survey.module.css";

/** The secondary "this is not research" strip at the bottom of the homepage card. */
export default function SurveyInfoStrip({ lead, smallSample }: { lead: string; smallSample: boolean }) {
  return (
    <div className={styles.vcInfo}>
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 11v5M12 8h.01" />
      </svg>
      <p>
        <b>Није репрезентативно истраживање јавног мњења.</b> {lead}
        {smallSample && <span className={styles.vcChip}>Мали узорак</span>}{" "}
        <Link href="/anketa/rezultati#metodologija">Методологија →</Link>
      </p>
    </div>
  );
}
