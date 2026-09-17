import styles from "./page.module.css";
import SiteHeader from "@/components/SiteHeader";

export default function Loading() {
  return (
    <div className={styles.page}>
      <SiteHeader />
      <div className={styles.wrap} style={{ padding: "48px 24px" }}>
        <p style={{ color: "var(--inkMuted, #868c81)", fontSize: 14 }}>Учитавање...</p>
      </div>
    </div>
  );
}
