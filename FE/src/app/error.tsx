"use client";

import { useEffect } from "react";
import styles from "./page.module.css";
import SiteHeader from "@/components/SiteHeader";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className={styles.page}>
      <SiteHeader />
      <div className={styles.wrap} style={{ padding: "48px 24px", maxWidth: 480 }}>
        <h1 style={{ fontFamily: "var(--font-display)", fontSize: 20, margin: "0 0 8px" }}>
          Тренутно није могуће учитати податке
        </h1>
        <p style={{ fontSize: 14, color: "#5b6158", margin: "0 0 20px", lineHeight: 1.5 }}>
          Дошло је до грешке при повезивању са сервером. Проверите везу и покушајте поново.
        </p>
        <button
          type="button"
          onClick={() => reset()}
          className={styles.pageBtn}
        >
          Покушај поново
        </button>
      </div>
    </div>
  );
}
