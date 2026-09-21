import type { Poll } from "@/lib/api";
import styles from "./polls.module.css";

export default function SourceKindChip({ kind }: { kind: Poll["sourceKind"] }) {
  return kind === "PRIMARY" ? (
    <span className={`${styles.chip} ${styles.chipPrimary}`}>Примарни извор</span>
  ) : (
    <span className={`${styles.chip} ${styles.chipSecondary}`}>Секундарни извор</span>
  );
}
