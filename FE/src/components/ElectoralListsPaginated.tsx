"use client";

import { useState } from "react";
import { ArrowUpRight } from "lucide-react";
import styles from "@/app/page.module.css";
import type { ElectoralList } from "@/lib/api";
import { formatDateSr } from "@/lib/format";

const PAGE_SIZE = 5;

// The stored name is the raw official text from RIK, e.g. "4. ИЗБОРНА ЛИСТА
// ...". The ballot number already has its own badge, so strip that leading
// prefix for display only — the raw value in the database is untouched.
const RAW_PREFIX = /^\d+\.\s*ИЗБОРНА\s+ЛИСТА\s*(?:[-–]\s*)?/i;

function displayName(name: string): string {
  return name.replace(RAW_PREFIX, "").trim();
}

type Props = {
  lists: ElectoralList[];
  /** ISO instant; lists published after it get the "нова" badge. Passed from the server so SSR and hydration agree. */
  recentSince?: string;
};

export default function ElectoralListsPaginated({ lists, recentSince }: Props) {
  const recentMs = recentSince ? Date.parse(recentSince) : null;
  const [page, setPage] = useState(0);
  const pageCount = Math.max(1, Math.ceil(lists.length / PAGE_SIZE));
  const visible = lists.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  if (lists.length === 0) {
    return (
      <p className={styles.pageIndicator} style={{ padding: "14px 0" }}>
        РИК још није објавио ниједну изборну листу.
      </p>
    );
  }

  return (
    <>
      <div className={styles.ledger}>
        {visible.map((list) => (
          <div className={styles.ledgerRow} key={list.id}>
            <div className={styles.ledgerNum}>{list.ballotNumber ?? "?"}</div>
            <div className={styles.ledgerBody}>
              <p className={styles.ledgerName}>{displayName(list.name)}</p>
              <div className={styles.ledgerMeta}>
                <a
                  className={styles.ledgerSource}
                  href={list.sourceUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  извор: РИК, {formatDateSr(list.publishedAt)}
                  <ArrowUpRight aria-hidden="true" />
                </a>
                {recentMs !== null && Date.parse(list.publishedAt) >= recentMs && (
                  <span className={styles.newBadge}>нова</span>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
      <div className={styles.pagination}>
        <button
          type="button"
          className={styles.pageBtn}
          disabled={page === 0}
          onClick={() => setPage((p) => Math.max(0, p - 1))}
        >
          ‹ Претходна
        </button>
        <span className={styles.pageIndicator}>
          Страна {page + 1} од {pageCount}
        </span>
        <button
          type="button"
          className={styles.pageBtn}
          disabled={page >= pageCount - 1}
          onClick={() => setPage((p) => Math.min(pageCount - 1, p + 1))}
        >
          Следећа ›
        </button>
      </div>
    </>
  );
}
