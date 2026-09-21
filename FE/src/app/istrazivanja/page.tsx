import type { Metadata } from "next";
import Link from "next/link";
import pageStyles from "@/app/page.module.css";
import PollCard from "@/components/polls/PollCard";
import styles from "@/components/polls/polls.module.css";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/SiteHeader";
import { getPolls, getPollsters, type Poll } from "@/lib/api";
import { monthOf } from "@/lib/pollFormat";

export const metadata: Metadata = {
  title: "Истраживања јавног мњења — Izbori 2026",
  description:
    "Проверена истраживања јавног мњења у истом облику, са назначеном основом резултата, узорком, методом и извором.",
};

const PAGE_SIZE = 20;

function href(pollster: string | undefined, page: number): string {
  const params = new URLSearchParams();
  if (pollster) params.set("pollster", pollster);
  if (page > 0) params.set("page", String(page));
  const query = params.toString();
  return query ? `/istrazivanja?${query}` : "/istrazivanja";
}

function groupByMonth(polls: Poll[]): { key: string; label: string; polls: Poll[] }[] {
  const groups: { key: string; label: string; polls: Poll[] }[] = [];
  for (const poll of polls) {
    const month = monthOf(poll);
    const last = groups[groups.length - 1];
    if (last && last.key === month.key) last.polls.push(poll);
    else groups.push({ ...month, polls: [poll] });
  }
  return groups;
}

export default async function IstrazivanjaPage({
  searchParams,
}: {
  searchParams: Promise<{ pollster?: string; page?: string }>;
}) {
  const params = await searchParams;
  const pollsters = await getPollsters();
  const active = pollsters.find((p) => p.slug === params.pollster);
  const pageNumber = Math.max(0, Number.parseInt(params.page ?? "0", 10) || 0);
  const result = await getPolls({ pollster: active?.slug, page: pageNumber, size: PAGE_SIZE });

  const total = pollsters.reduce((sum, p) => sum + p.approvedPollCount, 0);
  const empty = pollsters.filter((p) => p.approvedPollCount === 0);
  const groups = groupByMonth(result.content);

  return (
    <div className={pageStyles.page}>
      <SiteHeader />
      <main className={pageStyles.wrap}>
        <div className={styles.pageMain}>
          <h1 className={styles.pageTitle}>Истраживања јавног мњења</h1>
          <p className={styles.pageLead}>
            Овде су сва истраживања која смо проверили и објавили, у истом облику, без обзира ко их је спровео. Не
            рачунамо просек и не рангирамо опције.
          </p>
          <p className={styles.pageNote}>
            Резултати нису директно упоредиви: агенције питају различите узорке (сви испитаници или само опредељени) и
            нуде различите опције (странка, коалиција, листа). Зато уз сваки број пише на шта се односи.
          </p>

          <nav className={styles.filters} aria-label="Филтер по агенцији">
            <Link
              href="/istrazivanja"
              className={`${styles.filter} ${!active ? styles.filterActive : ""}`}
              aria-current={!active ? "page" : undefined}
            >
              Сва <span className={styles.filterCount}>({total})</span>
            </Link>
            {pollsters.map((p) =>
              p.approvedPollCount === 0 ? (
                <span key={p.slug} className={`${styles.filter} ${styles.filterDisabled}`}>
                  {p.name} <span className={styles.filterCount}>(0)</span>
                </span>
              ) : (
                <Link
                  key={p.slug}
                  href={href(p.slug, 0)}
                  className={`${styles.filter} ${active?.slug === p.slug ? styles.filterActive : ""}`}
                  aria-current={active?.slug === p.slug ? "page" : undefined}
                >
                  {p.name} <span className={styles.filterCount}>({p.approvedPollCount})</span>
                </Link>
              ),
            )}
          </nav>
          {empty.map((p) => (
            <p className={styles.filterNote} key={p.slug}>
              {p.name}: још нема објављеног истраживања о гласачким намерама за ове изборе.
            </p>
          ))}

          {result.content.length === 0 ? (
            <div className={styles.empty}>
              {active
                ? `Још нема објављених истраживања агенције ${active.name}.`
                : "Још нема објављених истраживања. Истраживања се објављују тек пошто се ручно провере."}
            </div>
          ) : (
            groups.map((group) => (
              <section key={group.key}>
                <h2 className={styles.monthLabel}>{group.label}</h2>
                <div className={styles.cards}>
                  {group.polls.map((poll) => (
                    <PollCard key={poll.id} poll={poll} />
                  ))}
                </div>
              </section>
            ))
          )}

          {result.totalPages > 1 && (
            <div className={styles.pager}>
              {pageNumber > 0 ? (
                <Link className={pageStyles.cardSourceLink} href={href(active?.slug, pageNumber - 1)}>
                  ‹ Претходна
                </Link>
              ) : (
                <span />
              )}
              {!result.last && (
                <Link className={pageStyles.cardSourceLink} href={href(active?.slug, pageNumber + 1)}>
                  Следећа ›
                </Link>
              )}
            </div>
          )}

          <p className={styles.disclaimer}>
            Приказујемо само истраживања за која можемо да утврдимо ко их је спровео, када, на ком узорку и шта
            проценти представљају. Ако неки податак није објављен, то пише уместо њега. Портал није повезан са
            агенцијама.
          </p>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}
