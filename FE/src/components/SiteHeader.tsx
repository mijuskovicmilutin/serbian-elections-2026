import Link from "next/link";
import styles from "@/app/page.module.css";

export default function SiteHeader() {
  return (
    <div className={styles.topbarBand}>
      <div className={styles.wrap}>
        <div className={styles.topbar}>
          <Link className={styles.wordmark} href="/">
            izbori<span className={styles.wordmarkAccent}>2026</span>
          </Link>
          <nav className={styles.nav}>
            <Link href="/">Почетна</Link>
            <Link href="/istrazivanja">Истраживања</Link>
            <Link href="/o-portalu">О порталу</Link>
            <Link href="/izvori">Извори</Link>
          </nav>
        </div>
      </div>
    </div>
  );
}
