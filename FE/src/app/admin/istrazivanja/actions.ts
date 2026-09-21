"use server";

import { revalidatePath } from "next/cache";
import { headers } from "next/headers";
import { redirect } from "next/navigation";
import { adminRequest } from "@/lib/adminApi";
import { isAdminAuthorized } from "@/lib/adminAuth";
import type { AdminPoll } from "@/lib/adminTypes";

export type FormState = { status: "idle" | "ok" | "error"; message: string };

const BASE = "/admin/istrazivanja";

// A Server Action is a public POST endpoint, so it re-checks the credential the proxy already checked.
async function requireAdmin() {
  const requestHeaders = await headers();
  if (!isAdminAuthorized(requestHeaders.get("authorization"))) {
    throw new Error("Unauthorized");
  }
}

function text(formData: FormData, name: string): string {
  const value = formData.get(name);
  return typeof value === "string" ? value.trim() : "";
}

function orNull(value: string): string | null {
  return value === "" ? null : value;
}

function readNumber(raw: string, label: string, errors: string[]): number | null {
  const normalized = raw.trim().replace(",", ".");
  if (normalized === "") return null;
  const value = Number(normalized);
  if (!Number.isFinite(value)) {
    errors.push(`${label}: неважећи број`);
    return null;
  }
  return value;
}

function parseMediaSources(raw: string, errors: string[]): { name: string; url: string }[] {
  const sources: { name: string; url: string }[] = [];
  raw.split("\n").forEach((line, index) => {
    const trimmed = line.trim();
    if (!trimmed) return;
    const separator = trimmed.indexOf("|");
    if (separator < 0) {
      errors.push(`Медијски пренос, ред ${index + 1}: очекује се „Назив | линк“`);
      return;
    }
    sources.push({ name: trimmed.slice(0, separator).trim(), url: trimmed.slice(separator + 1).trim() });
  });
  return sources;
}

function parsePoll(formData: FormData): { ok: true; input: Record<string, unknown> } | { ok: false; message: string } {
  const errors: string[] = [];
  const num = (name: string, label: string) => readNumber(text(formData, name), label, errors);

  const publishedDate = text(formData, "publishedAt");
  if (!publishedDate) errors.push("Датум објаве је обавезан");

  const names = formData.getAll("r_name").map((v) => String(v).trim());
  const pcts = formData.getAll("r_pct").map((v) => String(v));
  const kinds = formData.getAll("r_kind").map((v) => String(v));
  const comps = formData.getAll("r_comp").map((v) => String(v).trim());
  const lists = formData.getAll("r_list").map((v) => String(v));
  const results: Record<string, unknown>[] = [];
  names.forEach((name, i) => {
    const pctRaw = (pcts[i] ?? "").trim();
    if (!name && !pctRaw) return;
    if (!name) {
      errors.push(`Резултат ${i + 1}: назив је обавезан`);
      return;
    }
    const percentage = readNumber(pctRaw, `Резултат ${i + 1} (проценат)`, errors);
    if (percentage === null) {
      if (!pctRaw) errors.push(`Резултат ${i + 1}: проценат је обавезан`);
      return;
    }
    const listId = lists[i] ? Number(lists[i]) : null;
    results.push({
      rawOptionName: name,
      percentage,
      optionKind: kinds[i] || "UNSPECIFIED",
      composition: orNull(comps[i] ?? ""),
      electoralListId: Number.isInteger(listId) ? listId : null,
    });
  });

  const mediaSources = parseMediaSources(text(formData, "mediaSources"), errors);

  if (errors.length > 0) return { ok: false, message: errors.join("; ") };

  return {
    ok: true,
    input: {
      pollsterSlug: text(formData, "pollsterSlug"),
      title: text(formData, "title"),
      publishedAt: `${publishedDate}T12:00:00Z`,
      fieldworkFrom: orNull(text(formData, "fieldworkFrom")),
      fieldworkTo: orNull(text(formData, "fieldworkTo")),
      fieldworkNote: orNull(text(formData, "fieldworkNote")),
      sampleSize: num("sampleSize", "Узорак"),
      population: orNull(text(formData, "population")),
      method: orNull(text(formData, "method")),
      conductedBy: orNull(text(formData, "conductedBy")),
      commissionedBy: orNull(text(formData, "commissionedBy")),
      marginOfError: num("marginOfError", "Маргина грешке"),
      resultBasis: orNull(text(formData, "resultBasis")),
      decidedSharePct: num("decidedSharePct", "Удео опредељених"),
      undecidedPct: num("undecidedPct", "Неопредељени"),
      wontVotePct: num("wontVotePct", "Неће гласати"),
      willVotePct: num("willVotePct", "Изјаснило се да ће гласати"),
      sourceKind: text(formData, "sourceKind"),
      sourceUrl: text(formData, "sourceUrl"),
      originalDocumentUrl: orNull(text(formData, "originalDocumentUrl")),
      sourceNote: orNull(text(formData, "sourceNote")),
      mediaSources,
      results,
    },
  };
}

function to(tab: string, id: number, params: Record<string, string> = {}): string {
  const query = new URLSearchParams({ tab, id: String(id), ...params });
  return `${BASE}?${query.toString()}`;
}

export async function submitPoll(_previous: FormState, formData: FormData): Promise<FormState> {
  await requireAdmin();

  const intent = text(formData, "intent");
  const idRaw = text(formData, "id");
  const id = idRaw ? Number(idRaw) : null;
  if (idRaw && !Number.isInteger(id)) return { status: "error", message: "Неисправан ИД." };

  if (intent === "reject") {
    if (!id) return { status: "error", message: "Анкета још није сачувана." };
    if (!text(formData, "note")) return { status: "error", message: "Разлог одбијања је обавезан." };
    const result = await adminRequest<AdminPoll>(`/internal/polls/${id}/reject`, {
      method: "POST",
      body: { note: text(formData, "note") },
    });
    if (!result.ok) return { status: "error", message: result.message };
    revalidatePath(BASE);
    redirect(to("rejected", id, { notice: "Анкета је одбијена." }));
  }

  const parsed = parsePoll(formData);
  if (!parsed.ok) return { status: "error", message: parsed.message };

  let pollId = id;
  if (id) {
    const result = await adminRequest<AdminPoll>(`/internal/polls/${id}`, { method: "PUT", body: parsed.input });
    if (!result.ok) return { status: "error", message: result.message };
  } else {
    const result = await adminRequest<AdminPoll>("/internal/polls", { method: "POST", body: parsed.input });
    if (!result.ok) return { status: "error", message: result.message };
    pollId = result.data.id;
  }
  revalidatePath(BASE);

  if (intent === "approve") {
    const approved = await adminRequest<AdminPoll>(`/internal/polls/${pollId}/approve`, { method: "POST" });
    if (!approved.ok) {
      const tab = ["pending", "approved", "rejected"].includes(text(formData, "tab")) ? text(formData, "tab") : "pending";
      redirect(to(tab, pollId!, { error: `Сачувано, али није објављено: ${approved.message}` }));
    }
    redirect(to("approved", pollId!, { notice: "Анкета је објављена." }));
  }

  // Redirecting (instead of returning) keeps the confirmation visible after the form re-mounts with fresh data.
  const tab = ["pending", "approved", "rejected"].includes(text(formData, "tab")) ? text(formData, "tab") : "pending";
  redirect(to(tab, pollId!, { notice: id ? "Сачувано." : "Нацрт је сачуван." }));
}
