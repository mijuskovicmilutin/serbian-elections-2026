import { formatDateSr } from "@/lib/format";

export function formatPct(value: number | null): string {
  return value === null ? "—" : `${value.toFixed(1).replace(".", ",")}%`;
}

export function formatNumber(value: number): string {
  return String(Math.round(value)).replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

/** Weighting dimensions as they are named in the API, in the wording used on the page. */
export const DIMENSION_LABEL: Record<string, string> = {
  AGE: "старост",
  SEX: "пол",
  REGION: "регион",
  SETTLEMENT: "тип насеља",
};

export const DIMENSION_TITLE: Record<string, string> = {
  AGE: "Старост",
  SEX: "Пол",
  REGION: "Регион",
  SETTLEMENT: "Тип насеља",
};

/** "23. октобар 2026. у 00:00", in Belgrade time (the survey closes when the election silence starts). */
export function formatBelgradeDateTime(iso: string): string {
  const date = new Date(iso);
  const day = new Intl.DateTimeFormat("en-CA", { timeZone: "Europe/Belgrade" }).format(date);
  const time = new Intl.DateTimeFormat("sr-Latn", {
    timeZone: "Europe/Belgrade",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date);
  return `${formatDateSr(day)} у ${time}`;
}
