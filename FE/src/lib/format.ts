export const MONTHS_SR = [
  "јануар",
  "фебруар",
  "март",
  "април",
  "мај",
  "јун",
  "јул",
  "август",
  "септембар",
  "октобар",
  "новембар",
  "децембар",
];

export function formatDateSr(iso: string): string {
  const d = new Date(iso);
  return `${d.getDate()}. ${MONTHS_SR[d.getMonth()]} ${d.getFullYear()}.`;
}

export function formatRelativeSr(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.max(0, Math.round(diffMs / 60_000));
  if (minutes < 1) return "управо сада";
  if (minutes < 60) return `пре ${minutes} мин`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `пре ${hours} ч`;
  const days = Math.round(hours / 24);
  return `пре ${days} д`;
}
