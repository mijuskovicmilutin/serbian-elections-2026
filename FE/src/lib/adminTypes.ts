export type PollStatus = "DISCOVERED" | "DRAFT" | "APPROVED" | "REJECTED";
export type ResultBasis = "ALL_RESPONDENTS" | "LIKELY_VOTERS" | "DECIDED_VOTERS" | "OTHER";
export type OptionKind = "PARTY" | "COALITION" | "ELECTORAL_LIST" | "UNSPECIFIED";
export type SourceKind = "PRIMARY" | "SECONDARY";

export type PollsterRef = { slug: string; name: string; kind: string };

export type AdminPollSummary = {
  id: number;
  pollster: PollsterRef;
  title: string;
  publishedAt: string;
  status: PollStatus;
  sourceKind: SourceKind;
  modifiedAt: string;
};

export type AdminPollResult = {
  rawOptionName: string;
  percentage: number;
  displayOrder: number;
  optionKind: OptionKind;
  composition: string | null;
  electoralListId: number | null;
};

export type PollWarning = { code: string; value: string | null };

export type PollReadiness = { ready: boolean; missing: string[]; warnings: PollWarning[] };

export type PollAuditEntry = { action: string; details: string | null; at: string };

export type AdminPoll = {
  id: number;
  pollster: PollsterRef;
  status: PollStatus;
  title: string;
  publishedAt: string;
  fieldworkFrom: string | null;
  fieldworkTo: string | null;
  fieldworkNote: string | null;
  sampleSize: number | null;
  population: string | null;
  method: string | null;
  conductedBy: string | null;
  commissionedBy: string | null;
  marginOfError: number | null;
  resultBasis: ResultBasis | null;
  decidedSharePct: number | null;
  undecidedPct: number | null;
  wontVotePct: number | null;
  willVotePct: number | null;
  sourceKind: SourceKind;
  sourceUrl: string;
  originalDocumentUrl: string | null;
  mediaSources: { name: string; url: string }[];
  reviewedAt: string | null;
  reviewNote: string | null;
  scrapedAt: string | null;
  contentHash: string | null;
  sourceSnapshot: string | null;
  results: AdminPollResult[];
  readiness: PollReadiness;
  audit: PollAuditEntry[];
};

export type AdminPollster = { slug: string; name: string; kind: string; website: string | null; approvedPollCount: number };

export type AdminPage<T> = { content: T[]; totalElements: number };

export type PollSourceStatus = {
  source: string;
  lastRunAt: string | null;
  status: "RUNNING" | "SUCCESS" | "FAILED" | null;
  recordsFound: number | null;
  recordsCreated: number | null;
  errorMessage: string | null;
};
