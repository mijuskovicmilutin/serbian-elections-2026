import type { Metadata } from "next";
import { headers } from "next/headers";
import Link from "next/link";
import { notFound } from "next/navigation";
import { adminRequest } from "@/lib/adminApi";
import { isAdminAuthorized } from "@/lib/adminAuth";
import type { AdminPage, AdminPoll, AdminPollSummary, AdminPollster, PollSourceStatus } from "@/lib/adminTypes";
import { getCurrentElectoralLists } from "@/lib/api";
import { formatDateSr, formatRelativeSr } from "@/lib/format";
import styles from "./admin.module.css";
import { AUDIT_LABEL, SOURCE_LABEL, STATUS_LABEL } from "./labels";
import PollReviewForm from "./PollReviewForm";

export const metadata: Metadata = {
  title: "Admin – истраживања",
  robots: { index: false, follow: false },
};

type Tab = "pending" | "approved" | "rejected";

const TABS: { key: Tab; label: string; query: string }[] = [
  { key: "pending", label: "На чекању", query: "status=DRAFT&status=DISCOVERED" },
  { key: "approved", label: "Одобрена", query: "status=APPROVED" },
  { key: "rejected", label: "Одбијена", query: "status=REJECTED" },
];

function first(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

function chipClass(status: string): string {
  if (status === "APPROVED") return styles.chipApproved;
  if (status === "REJECTED") return styles.chipRejected;
  if (status === "DRAFT") return styles.chipDraft;
  return styles.chipPlain;
}

function dateTime(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${formatDateSr(iso)} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default async function AdminPollsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  // The proxy already guards /admin/**; this keeps the page safe even if that ever changes.
  if (!isAdminAuthorized((await headers()).get("authorization"))) notFound();

  const params = await searchParams;
  const tabParam = first(params.tab);
  const tab: Tab = TABS.some((t) => t.key === tabParam) ? (tabParam as Tab) : "pending";
  const idParam = first(params.id);
  const isNew = first(params.new) === "1";
  const notice = first(params.notice);
  const errorNotice = first(params.error);

  const listPage = (query: string) => adminRequest<AdminPage<AdminPollSummary>>(`/internal/polls?${query}&size=100`);
  const [pending, approved, rejected, pollsters, electoralLists, detail, sources] = await Promise.all([
    listPage(TABS[0].query),
    listPage(TABS[1].query),
    listPage(TABS[2].query),
    adminRequest<AdminPollster[]>("/internal/pollsters"),
    getCurrentElectoralLists().catch(() => []),
    idParam && /^\d+$/.test(idParam) ? adminRequest<AdminPoll>(`/internal/polls/${idParam}`) : Promise.resolve(null),
    adminRequest<PollSourceStatus[]>("/internal/poll-sources"),
  ]);

  const byTab = { pending, approved, rejected };
  const backendError = [pending, approved, rejected, pollsters].find((r) => !r.ok);
  const list = byTab[tab].ok ? byTab[tab].data.content : [];
  const counts = (key: Tab) => (byTab[key].ok ? byTab[key].data.totalElements : 0);
  const pollsterList = pollsters.ok ? pollsters.data : [];
  const poll = detail && detail.ok ? detail.data : null;
  const sourceList = sources.ok ? sources.data : [];
  const lists = electoralLists.map((l) => ({
    id: l.id,
    label: `${l.ballotNumber ?? "?"}. ${l.name.replace(/^\d+\.\s*ИЗБОРНА\s+ЛИСТА\s*/i, "").slice(0, 60)}`,
  }));

  return (
    <div className={styles.shell}>
      <header className={styles.topbar}>
        <div style={{ display: "flex", alignItems: "center", gap: 14, flexWrap: "wrap" }}>
          <span className={styles.brand}>
            izbori<span>2026</span> · admin
          </span>
          <span className={styles.topNote}>скривено од претраге · приступ преко тајног кључа</span>
        </div>
        <Link href="/" style={{ color: "#9fd8cd", fontSize: 13 }}>
          Јавни сајт →
        </Link>
      </header>

      <div className={styles.layout}>
        <aside className={styles.aside}>
          <div className={styles.card} style={{ paddingTop: 4 }}>
            <nav className={styles.tabs} aria-label="Статус анкета">
              {TABS.map((t) => (
                <Link
                  key={t.key}
                  href={`/admin/istrazivanja?tab=${t.key}`}
                  className={`${styles.tab} ${tab === t.key ? styles.tabActive : ""}`}
                >
                  {t.label} ({counts(t.key)})
                </Link>
              ))}
            </nav>
            <div className={styles.queue}>
              {list.length === 0 && <p className={styles.sub}>Нема анкета у овој групи.</p>}
              {list.map((p) => (
                <Link
                  key={p.id}
                  href={`/admin/istrazivanja?tab=${tab}&id=${p.id}`}
                  className={`${styles.queueItem} ${poll?.id === p.id ? styles.queueItemActive : ""}`}
                >
                  <div className={styles.queueTop}>
                    <span className={styles.queueName}>{p.pollster.name}</span>
                    <span className={`${styles.chip} ${chipClass(p.status)}`}>{STATUS_LABEL[p.status]}</span>
                  </div>
                  <div className={styles.queueMeta}>
                    {p.title}
                    <br />
                    Објављено {formatDateSr(p.publishedAt)}
                  </div>
                </Link>
              ))}
            </div>
            <Link href="/admin/istrazivanja?tab=pending&new=1" className={styles.newLink}>
              + Нова анкета
            </Link>
          </div>

          <div className={styles.card}>
            <h2 className={styles.sideTitle}>Праћени извори</h2>
            {sourceList.map((source) => {
              const failed = source.status === "FAILED";
              return (
                <div className={styles.pollsterRow} key={source.source} style={{ flexDirection: "column", gap: 2 }}>
                  <span style={{ fontWeight: 600 }}>{SOURCE_LABEL[source.source] ?? source.source}</span>
                  <span style={{ fontSize: 12.5, color: failed ? "#a01b1b" : "#5b6158", lineHeight: 1.45 }}>
                    {!source.lastRunAt
                      ? "Још није проверено."
                      : failed
                        ? `Провера није успела ${formatRelativeSr(source.lastRunAt)}: ${source.errorMessage ?? "непозната грешка"}`
                        : `Проверено ${formatRelativeSr(source.lastRunAt)}. ${
                            source.recordsCreated ? `Нових кандидата: ${source.recordsCreated}.` : "Нема ништа ново."
                          }`}
                  </span>
                </div>
              );
            })}
            <p className={styles.sub} style={{ marginTop: 10, lineHeight: 1.45 }}>
              Faktor Plus нема јавни feed: прати се преко медија. Бројеве увек уносите ручно, аутоматика само јавља да је
              нешто објављено.
            </p>
          </div>

          <div className={styles.card}>
            <h2 className={styles.sideTitle}>Агенције</h2>
            {pollsterList.map((p) => (
              <div className={styles.pollsterRow} key={p.slug}>
                <span>{p.name}</span>
                <span style={{ color: "#5b6158" }}>
                  {p.approvedPollCount} {p.approvedPollCount === 1 ? "одобрена" : "одобрених"}
                </span>
              </div>
            ))}
          </div>

          <div className={styles.card}>
            <h2 className={styles.sideTitle}>Ток истраживања</h2>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 6, alignItems: "center", fontSize: 12 }}>
              <span className={`${styles.chip} ${styles.chipPlain}`}>ПРОНАЂЕНО</span>→
              <span className={`${styles.chip} ${styles.chipDraft}`}>НАЦРТ</span>→
              <span className={`${styles.chip} ${styles.chipApproved}`}>ОДОБРЕНО</span>/
              <span className={`${styles.chip} ${styles.chipRejected}`}>ОДБИЈЕНО</span>
            </div>
            <p className={styles.sub} style={{ marginTop: 10 }}>
              На јавном сајту се види само ОДОБРЕНО.
            </p>
          </div>
        </aside>

        <section className={styles.main}>
          {notice && <div className={`${styles.banner} ${styles.bannerOk}`}>{notice}</div>}
          {errorNotice && <div className={`${styles.banner} ${styles.bannerError}`}>{errorNotice}</div>}
          {backendError && !backendError.ok && (
            <div className={`${styles.banner} ${styles.bannerError}`}>{backendError.message}</div>
          )}
          {detail && !detail.ok && <div className={`${styles.banner} ${styles.bannerError}`}>{detail.message}</div>}

          {poll?.status === "DISCOVERED" && (
            <div className={`${styles.banner} ${styles.bannerOk}`} style={{ background: "#fbefd9", color: "#5a3608" }}>
              Ово је аутоматски пронађен кандидат: познати су само наслов, линк и датум. Отворите извор, унесите податке и
              резултате, па сачувајте као нацрт.
            </div>
          )}

          {poll || isNew ? (
            <>
              <div className={styles.headRow}>
                <div>
                  <h1 className={styles.h1}>{poll ? `Преглед: ${poll.pollster.name}` : "Нова анкета"}</h1>
                  <p className={styles.sub}>
                    {poll ? poll.title : "Унесите податке из извора, сачувајте нацрт, па одобрите када проверите."}
                  </p>
                </div>
                {poll && <span className={`${styles.chip} ${chipClass(poll.status)}`}>{STATUS_LABEL[poll.status]}</span>}
              </div>

              <div className={styles.columns}>
                <PollReviewForm
                  key={`${poll?.id ?? "new"}-${poll?.audit[0]?.at ?? ""}`}
                  poll={poll}
                  pollsters={pollsterList}
                  lists={lists}
                  tab={tab}
                />

                <div className={styles.stack}>
                  {poll && (
                    <div className={styles.card}>
                      <h2 className={styles.h2}>Извор</h2>
                      <a className={styles.sourceLink} href={poll.sourceUrl} target="_blank" rel="noopener noreferrer">
                        Отвори оригинал ↗
                      </a>
                      {poll.mediaSources.map((m) => (
                        <div key={m.url} style={{ marginTop: 6 }}>
                          <a className={styles.sourceLink} href={m.url} target="_blank" rel="noopener noreferrer">
                            {m.name} ↗
                          </a>
                        </div>
                      ))}
                      <p className={styles.hint} style={{ margin: "10px 0" }}>
                        {poll.scrapedAt ? `Преузето: ${dateTime(poll.scrapedAt)}. ` : "Унето ручно. "}
                        {poll.contentHash && `Отисак садржаја: ${poll.contentHash.slice(0, 12)}…`}
                      </p>
                      {poll.sourceSnapshot && <div className={styles.snapshot}>{poll.sourceSnapshot}</div>}
                    </div>
                  )}

                  {poll && (
                    <div className={styles.card}>
                      <h2 className={styles.h2}>Историја измена</h2>
                      {poll.audit.length === 0 && <p className={styles.hint}>Још нема записа.</p>}
                      {poll.audit.map((a, i) => (
                        <div className={styles.auditItem} key={`${a.at}-${i}`}>
                          <b>{AUDIT_LABEL[a.action] ?? a.action}</b> · {dateTime(a.at)}
                          {a.details && <pre>{a.details}</pre>}
                        </div>
                      ))}
                      {poll.reviewNote && (
                        <div className={styles.auditItem}>
                          <b>Белешка прегледа:</b> {poll.reviewNote}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </>
          ) : (
            <div className={`${styles.card} ${styles.empty}`}>
              Изаберите анкету са леве стране или направите нову.
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
