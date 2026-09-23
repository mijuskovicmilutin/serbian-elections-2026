import styles from "@/app/page.module.css";

export default function SiteFooter({ wide = false }: { wide?: boolean }) {
  return (
    <footer className={styles.footer}>
      <div className={`${wide ? styles.wideWrap : styles.wrap} ${styles.footerInner}`}>
        <p className={styles.disclaimer}>
          Овај портал агрегира јавно доступне податке из означених извора. Није званични сајт Републичке
          изборне комисије.
        </p>
        <div className={styles.footerLinks}>
          <a href="/o-portalu">О порталу</a>
          <a href="/izvori">Извори</a>
        </div>
      </div>
    </footer>
  );
}
