"use client";

import { useActionState, useState } from "react";
import type { AdminPoll, AdminPollster } from "@/lib/adminTypes";
import { submitPoll, type FormState } from "./actions";
import styles from "./admin.module.css";
import { BASIS_LABEL, KIND_LABEL, MISSING_LABEL, STATUS_LABEL, warningText } from "./labels";

type ListOption = { id: number; label: string };

type Row = {
  key: number;
  name: string;
  pct: string;
  kind: string;
  comp: string;
  list: string;
};

const INITIAL_STATE: FormState = { status: "idle", message: "" };

function num(value: number | null | undefined): string {
  return value === null || value === undefined ? "" : String(value);
}

function Field({ label, hint, wide, children }: { label: string; hint?: string; wide?: boolean; children: React.ReactNode }) {
  return (
    <label className={`${styles.field} ${wide ? styles.span2 : ""}`}>
      <span className={styles.label}>{label}</span>
      {children}
      {hint && <span className={styles.hint}>{hint}</span>}
    </label>
  );
}

export default function PollReviewForm({
  poll,
  pollsters,
  lists,
  tab,
}: {
  poll: AdminPoll | null;
  pollsters: AdminPollster[];
  lists: ListOption[];
  tab: string;
}) {
  const [state, formAction, pending] = useActionState(submitPoll, INITIAL_STATE);

  const initialRows: Row[] = poll?.results.length
    ? poll.results.map((r, i) => ({
        key: i,
        name: r.rawOptionName,
        pct: String(r.percentage),
        kind: r.optionKind,
        comp: r.composition ?? "",
        list: r.electoralListId ? String(r.electoralListId) : "",
      }))
    : [{ key: 0, name: "", pct: "", kind: "UNSPECIFIED", comp: "", list: "" }];
  const [rows, setRows] = useState<Row[]>(initialRows);
  const [nextKey, setNextKey] = useState(initialRows.length);

  const addRow = () => {
    setRows((current) => [...current, { key: nextKey, name: "", pct: "", kind: "UNSPECIFIED", comp: "", list: "" }]);
    setNextKey((k) => k + 1);
  };
  const removeRow = (key: number) => setRows((current) => current.filter((r) => r.key !== key));

  const media = poll?.mediaSources.map((m) => `${m.name} | ${m.url}`).join("\n") ?? "";
  const isApproved = poll?.status === "APPROVED";

  return (
    <form action={formAction} className={styles.stack}>
      <input type="hidden" name="id" value={poll?.id ?? ""} />
      <input type="hidden" name="tab" value={tab} />

      {state.status !== "idle" && (
        <div className={`${styles.banner} ${state.status === "ok" ? styles.bannerOk : styles.bannerError}`} role="status">
          {state.message}
        </div>
      )}

      <div className={styles.card}>
        <h2 className={styles.h2}>Подаци о истраживању</h2>
        <div className={styles.grid2}>
          <Field label="Агенција">
            <select className={styles.select} name="pollsterSlug" defaultValue={poll?.pollster.slug ?? pollsters[0]?.slug}>
              {pollsters.map((p) => (
                <option key={p.slug} value={p.slug}>
                  {p.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Тип извора">
            <select
              className={styles.select}
              name="sourceKind"
              defaultValue={poll?.sourceKind ?? "PRIMARY"}
            >
              <option value="PRIMARY">Примарни (објавила агенција)</option>
              <option value="SECONDARY">Секундарни (медијски пренос)</option>
            </select>
          </Field>
          <Field label="Наслов" wide>
            <input className={styles.input} name="title" defaultValue={poll?.title ?? ""} maxLength={500} required />
          </Field>
          <Field label="Објављено">
            <input className={styles.input} type="date" name="publishedAt" defaultValue={poll?.publishedAt.slice(0, 10) ?? ""} required />
          </Field>
          <Field label="Опис периода терена" hint="Нпр. „август–септембар 2026.“ ако тачни датуми нису наведени.">
            <input className={styles.input} name="fieldworkNote" defaultValue={poll?.fieldworkNote ?? ""} maxLength={255} />
          </Field>
          <Field label="Терен од">
            <input className={styles.input} type="date" name="fieldworkFrom" defaultValue={poll?.fieldworkFrom ?? ""} />
          </Field>
          <Field label="Терен до">
            <input className={styles.input} type="date" name="fieldworkTo" defaultValue={poll?.fieldworkTo ?? ""} />
          </Field>
          <Field label="Узорак">
            <input className={`${styles.input} ${styles.mono}`} name="sampleSize" inputMode="numeric" defaultValue={num(poll?.sampleSize)} />
          </Field>
          <Field label="Метод">
            <input className={styles.input} name="method" defaultValue={poll?.method ?? ""} maxLength={255} />
          </Field>
          <Field label="Популација">
            <input className={styles.input} name="population" defaultValue={poll?.population ?? ""} maxLength={255} />
          </Field>
          <Field label="Спровела">
            <input className={styles.input} name="conductedBy" defaultValue={poll?.conductedBy ?? ""} maxLength={255} />
          </Field>
          <Field label="Наручилац">
            <input className={styles.input} name="commissionedBy" defaultValue={poll?.commissionedBy ?? ""} maxLength={255} />
          </Field>
          <Field label="Маргина грешке (%)">
            <input className={`${styles.input} ${styles.mono}`} name="marginOfError" inputMode="decimal" defaultValue={num(poll?.marginOfError)} />
          </Field>
          <Field label="Основа резултата">
            <select className={styles.select} name="resultBasis" defaultValue={poll?.resultBasis ?? ""}>
              <option value="">Није изабрано</option>
              {Object.entries(BASIS_LABEL).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Удео опредељених у узорку (%)">
            <input className={`${styles.input} ${styles.mono}`} name="decidedSharePct" inputMode="decimal" defaultValue={num(poll?.decidedSharePct)} />
          </Field>
          <Field label="Неопредељени (%)">
            <input className={`${styles.input} ${styles.mono}`} name="undecidedPct" inputMode="decimal" defaultValue={num(poll?.undecidedPct)} />
          </Field>
          <Field label="Неће гласати (%)">
            <input className={`${styles.input} ${styles.mono}`} name="wontVotePct" inputMode="decimal" defaultValue={num(poll?.wontVotePct)} />
          </Field>
          <Field label="Изјаснило се да ће гласати (%)">
            <input className={`${styles.input} ${styles.mono}`} name="willVotePct" inputMode="decimal" defaultValue={num(poll?.willVotePct)} />
          </Field>
          <Field label="Извор (линк)" wide hint="Само http(s) линкови.">
            <input className={styles.input} name="sourceUrl" type="url" defaultValue={poll?.sourceUrl ?? ""} required />
          </Field>
          <Field label="Оригинални документ (линк)" wide>
            <input className={styles.input} name="originalDocumentUrl" type="url" defaultValue={poll?.originalDocumentUrl ?? ""} />
          </Field>
          <Field
            label="Напомена извора"
            wide
            hint="Шта извор сам каже о бројевима а не стоји у другим пољима, нпр. „остали не прелазе изборни праг“. Јавно се приказује."
          >
            <textarea className={styles.textarea} name="sourceNote" maxLength={1000} defaultValue={poll?.sourceNote ?? ""} />
          </Field>
          <Field label="Медијски пренос" wide hint="Један по реду, у облику: Назив | линк">
            <textarea className={styles.textarea} name="mediaSources" defaultValue={media} />
          </Field>
        </div>
      </div>

      <div className={styles.card}>
        <div className={styles.resultsHead}>
          <h2 className={styles.h2} style={{ margin: 0 }}>
            Резултати (као у извору)
          </h2>
          <button type="button" className={styles.button} onClick={addRow}>
            + Додај ред
          </button>
        </div>
        {rows.map((row, index) => (
          <div className={styles.resultRow} key={row.key}>
            <span className={styles.resultIndex}>{index + 1}</span>
            <input
              className={`${styles.input} ${styles.smallInput}`}
              name="r_name"
              aria-label={`Назив опције ${index + 1}`}
              defaultValue={row.name}
              maxLength={500}
              placeholder="Назив опције тачно као у извору"
            />
            <input
              className={`${styles.input} ${styles.smallInput} ${styles.mono}`}
              name="r_pct"
              aria-label={`Проценат ${index + 1}`}
              inputMode="decimal"
              defaultValue={row.pct}
              placeholder="%"
              style={{ textAlign: "right" }}
            />
            <button type="button" className={styles.iconButton} aria-label={`Уклони ред ${index + 1}`} onClick={() => removeRow(row.key)}>
              ×
            </button>
            <div className={styles.resultSecond}>
              <select
                className={`${styles.select} ${styles.smallInput}`}
                name="r_kind"
                aria-label={`Шта опција обухвата ${index + 1}`}
                defaultValue={row.kind}
              >
                {Object.entries(KIND_LABEL).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
              <input
                className={`${styles.input} ${styles.smallInput}`}
                name="r_comp"
                aria-label={`Партнери у коалицији ${index + 1}`}
                defaultValue={row.comp}
                placeholder="Партнери у коалицији / напомена из извора"
              />
              <select
                className={`${styles.select} ${styles.smallInput}`}
                name="r_list"
                aria-label={`Повезана РИК листа ${index + 1}`}
                defaultValue={row.list}
              >
                <option value="">Без везе са РИК листом</option>
                {lists.map((l) => (
                  <option key={l.id} value={l.id}>
                    {l.label}
                  </option>
                ))}
              </select>
            </div>
          </div>
        ))}
        {poll?.readiness.warnings.map((w) => (
          <div className={styles.warn} key={w.code}>
            <b>!</b>
            <span>{warningText(w)}</span>
          </div>
        ))}
        <p className={styles.hint} style={{ marginTop: 10 }}>
          Повезивање са РИК листом ради се само где је јасно; није нужно да су „СНС“ у анкети и листа са коалиционим партнерима исто.
          Ознака опције и партнери су интерна евиденција и јавно се не приказују.
        </p>
      </div>

      <div className={styles.card}>
        <h2 className={styles.h2}>Услови за објаву</h2>
        {poll ? (
          <ul className={styles.checklist}>
            <li className={styles.checkItem}>
              <span className={`${styles.mark} ${styles.markOk}`}>✓</span>
              <span>Агенција је позната</span>
            </li>
            {["PERIOD", "RESULT_BASIS", "SOURCE", "RESULTS"].map((code) => {
              const missing = poll.readiness.missing.includes(code);
              const soft = code === "PERIOD" && !missing && poll.readiness.warnings.some((w) => w.code === "EXACT_DATES_MISSING");
              return (
                <li className={styles.checkItem} key={code}>
                  <span className={`${styles.mark} ${missing ? styles.markBad : soft ? styles.markWarn : styles.markOk}`}>
                    {missing ? "×" : soft ? "!" : "✓"}
                  </span>
                  <span>
                    {MISSING_LABEL[code]}
                    {soft && " (познат је само опис периода)"}
                  </span>
                </li>
              );
            })}
          </ul>
        ) : (
          <p className={styles.hint}>Услови се проверавају након првог чувања.</p>
        )}

        <div className={styles.actions}>
          <div>
            {poll && poll.status !== "REJECTED" && (
              <details className={styles.rejectBox}>
                <summary>{isApproved ? "Повуци (одбиј)…" : "Одбиј…"}</summary>
                <textarea className={styles.textarea} name="note" placeholder="Разлог одбијања (обавезно)" />
                <div>
                  <button type="submit" name="intent" value="reject" className={styles.buttonDanger + " " + styles.button} formNoValidate disabled={pending}>
                    Потврди одбијање
                  </button>
                </div>
              </details>
            )}
          </div>
          <div className={styles.actionsRight}>
            <button type="submit" name="intent" value="save" className={styles.button} formNoValidate disabled={pending}>
              {isApproved ? "Сачувај измене" : "Сачувај нацрт"}
            </button>
            {!isApproved && (
              <button type="submit" name="intent" value="approve" className={`${styles.button} ${styles.buttonPrimary}`} disabled={pending}>
                Одобри и објави
              </button>
            )}
          </div>
        </div>
        <p className={styles.hint} style={{ marginTop: 10 }}>
          Након одобрења истраживање се појављује на јавном сајту. Измене после објаве остављају запис у историји.
          {poll && ` Статус: ${STATUS_LABEL[poll.status]}.`}
        </p>
      </div>
    </form>
  );
}
