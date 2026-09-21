import type { OptionKind, PollStatus, PollWarning, ResultBasis } from "@/lib/adminTypes";

export const STATUS_LABEL: Record<PollStatus, string> = {
  DISCOVERED: "ПРОНАЂЕНО",
  DRAFT: "НАЦРТ",
  APPROVED: "ОДОБРЕНО",
  REJECTED: "ОДБИЈЕНО",
};

export const BASIS_LABEL: Record<ResultBasis, string> = {
  ALL_RESPONDENTS: "Сви испитаници",
  LIKELY_VOTERS: "Вероватни гласачи",
  DECIDED_VOTERS: "Међу опредељеним бирачима",
  OTHER: "Друго",
};

export const KIND_LABEL: Record<OptionKind, string> = {
  PARTY: "Само странка",
  COALITION: "Коалиција",
  ELECTORAL_LIST: "Изборна листа",
  UNSPECIFIED: "Није наведено у извору",
};

export const AUDIT_LABEL: Record<string, string> = {
  CREATED: "Креирано",
  UPDATED: "Измењено",
  APPROVED: "Одобрено",
  REJECTED: "Одбијено",
};

function optionsWord(count: number): string {
  const lastTwo = count % 100;
  const last = count % 10;
  if (last === 1 && lastTwo !== 11) return "опција";
  if (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) return "опције";
  return "опција";
}

export function warningText(warning: PollWarning): string {
  switch (warning.code) {
    case "SUM_OFF":
      return `Збир је ${warning.value?.replace(".", ",")}%. За основу „опредељени“ очекује се око 100%; разлика су вероватно опције испод цензуса. Не блокира објаву.`;
    case "PARTNERS_UNKNOWN":
      return `${warning.value} ${optionsWord(Number(warning.value))} без података о партнерима у коалицији (интерна евиденција, јавно се не приказује).`;
    case "EXACT_DATES_MISSING":
      return "Тачни датуми терена нису наведени, познат је само опис периода.";
    default:
      return warning.code;
  }
}

export const MISSING_LABEL: Record<string, string> = {
  PERIOD: "Период истраживања (датуми или опис)",
  RESULT_BASIS: "Основа резултата",
  SOURCE: "Извор",
  RESULTS: "Бар један резултат",
};

export const SOURCE_LABEL: Record<string, string> = {
  POLL_CRTA: "CRTA (сопствени feed)",
  POLL_CESID: "CeSID (сопствени feed)",
  POLL_MEDIA: "Медији (N1, Nova.rs, Blic, Informer)",
};
