import type { Poll, PollResultBasis } from "@/lib/api";
import { MONTHS_SR, formatDateSr } from "@/lib/format";

export const BASIS_LABEL: Record<PollResultBasis, string> = {
  ALL_RESPONDENTS: "Међу свим испитаницима",
  LIKELY_VOTERS: "Међу вероватним гласачима",
  DECIDED_VOTERS: "Међу опредељеним бирачима",
  OTHER: "Друга основа резултата",
};

const BASIS_SENTENCE: Record<PollResultBasis, string> = {
  ALL_RESPONDENTS: "Проценти се односе на све испитанике.",
  LIKELY_VOTERS: "Проценти се односе на вероватне гласаче.",
  DECIDED_VOTERS: "Проценти се односе на опредељене бираче.",
  OTHER: "Проценти се односе на основу коју је навео извор.",
};

export function formatPercent(value: number): string {
  return `${String(Number(value.toFixed(2))).replace(".", ",")}%`;
}

/** Result rows read as published: one decimal ("2,0%"), or two if the source gave two. */
export function formatResultPercent(value: number): string {
  const rounded = Number(value.toFixed(2));
  const digits = Number.isInteger(rounded * 10) ? 1 : 2;
  return `${rounded.toFixed(digits).replace(".", ",")}%`;
}

export function formatCount(value: number): string {
  return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

function parts(isoDate: string): { day: number; month: number; year: number } {
  const [year, month, day] = isoDate.split("-").map(Number);
  return { day, month, year };
}

/** "10–24. јун 2026.", "28. јун – 2. јул 2026.", a single date, or the source's own wording. */
export function formatFieldwork(poll: Poll): string | null {
  const { fieldworkFrom: from, fieldworkTo: to, fieldworkNote: note } = poll;
  if (from && to) {
    const a = parts(from);
    const b = parts(to);
    if (a.year === b.year && a.month === b.month) {
      return a.day === b.day
        ? `${a.day}. ${MONTHS_SR[a.month - 1]} ${a.year}.`
        : `${a.day}–${b.day}. ${MONTHS_SR[a.month - 1]} ${a.year}.`;
    }
    if (a.year === b.year) {
      return `${a.day}. ${MONTHS_SR[a.month - 1]} – ${b.day}. ${MONTHS_SR[b.month - 1]} ${a.year}.`;
    }
    return `${formatDateSr(from)} – ${formatDateSr(to)}`;
  }
  if (from || to) return formatDateSr((from ?? to) as string);
  return note;
}

export function basisSentence(poll: Poll): string | null {
  return poll.resultBasis ? BASIS_SENTENCE[poll.resultBasis] : null;
}

/** Short factual detail next to the basis label, e.g. "70% испитаника, 12% неопредељено, 9% неће гласати". */
export function basisDetail(poll: Poll): string | null {
  const detail = [
    poll.decidedSharePct !== null ? `${formatPercent(poll.decidedSharePct)} испитаника` : null,
    poll.undecidedPct !== null ? `${formatPercent(poll.undecidedPct)} неопредељено` : null,
    poll.wontVotePct !== null ? `${formatPercent(poll.wontVotePct)} неће гласати` : null,
  ].filter(Boolean);
  return detail.length > 0 ? detail.join(", ") : null;
}

export function pollHeading(poll: Poll): string {
  return poll.conductedBy || poll.pollster.name;
}

export function hostOf(url: string): string {
  try {
    return new URL(url).hostname.replace(/^www\./, "");
  } catch {
    return url;
  }
}

/** "2026-09" key and "Септембар 2026." label of a poll's publication month (UTC, like the stored instant). */
export function monthOf(poll: Poll): { key: string; label: string } {
  const d = new Date(poll.publishedAt);
  const month = MONTHS_SR[d.getUTCMonth()];
  return {
    key: `${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, "0")}`,
    label: `${month.charAt(0).toUpperCase()}${month.slice(1)} ${d.getUTCFullYear()}.`,
  };
}
