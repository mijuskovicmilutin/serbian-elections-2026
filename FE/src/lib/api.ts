const API_URL = process.env.NEXT_PUBLIC_API_URL;

export type Election = {
  id: number;
  name: string;
  electionDate: string;
};

export type ElectoralListStatus = "SUBMITTED" | "PROCLAIMED" | "REJECTED" | "WITHDRAWN";

export type ElectoralList = {
  id: number;
  name: string;
  ballotNumber: number | null;
  status: ElectoralListStatus;
  sourceUrl: string;
  publishedAt: string;
  lastSeenAt: string;
};

export type ElectionEventType = "CALLED" | "DEADLINE" | "ELECTION_DAY" | "OTHER";

export type ElectionEvent = {
  id: number;
  type: ElectionEventType;
  title: string;
  description: string | null;
  eventDate: string;
  sourceUrl: string | null;
};

export type NewsSource = "N1" | "NOVA" | "BLIC" | "INFORMER";

export type NewsArticle = {
  id: number;
  source: NewsSource;
  title: string;
  description: string | null;
  url: string;
  imageUrl: string | null;
  publishedAt: string;
  fetchedAt: string;
};

export type PollResult = {
  rawOptionName: string;
  percentage: number;
  displayOrder: number;
};

export type PollResultBasis = "ALL_RESPONDENTS" | "LIKELY_VOTERS" | "DECIDED_VOTERS" | "OTHER";

export type Poll = {
  id: number;
  pollster: { slug: string; name: string; kind: string };
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
  resultBasis: PollResultBasis | null;
  decidedSharePct: number | null;
  undecidedPct: number | null;
  wontVotePct: number | null;
  willVotePct: number | null;
  sourceKind: "PRIMARY" | "SECONDARY";
  sourceUrl: string;
  originalDocumentUrl: string | null;
  sourceNote: string | null;
  mediaSources: { name: string; url: string }[];
  results: PollResult[];
};

export type Pollster = {
  slug: string;
  name: string;
  kind: string;
  website: string | null;
  approvedPollCount: number;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
};

export type PricePoint = {
  t: number;
  p: number;
};

export type PredictionMarketOutcome = {
  name: string;
  price: number;
  imageUrl: string | null;
  volume: number | null;
  oneDayPriceChange: number | null;
  yesPrice: number | null;
  noPrice: number | null;
  priceHistory: PricePoint[] | null;
};

export type PredictionMarket = {
  id: number;
  provider: string;
  marketName: string;
  sourceUrl: string;
  updatedAt: string;
  volume: number | null;
  endDate: string | null;
  outcomes: PredictionMarketOutcome[];
};

async function apiFetch<T>(path: string): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    next: { revalidate: 60 },
  });
  if (!res.ok) {
    throw new Error(`API ${path} returned ${res.status}`);
  }
  return res.json() as Promise<T>;
}

export function getCurrentElection() {
  return apiFetch<Election>("/api/v1/elections/current");
}

export function getCurrentElectoralLists() {
  return apiFetch<ElectoralList[]>("/api/v1/elections/current/lists");
}

export async function getNewsBySource(source: NewsSource, size = 5) {
  const page = await apiFetch<PageResponse<NewsArticle>>(`/api/v1/news?source=${source}&size=${size}`);
  return page.content;
}

// Optional/supplementary section: returns null instead of throwing so a missing or
// not-yet-imported market doesn't take down the rest of the homepage.
export async function getCurrentPredictionMarket(): Promise<PredictionMarket | null> {
  const res = await fetch(`${API_URL}/api/v1/prediction-markets/current`, {
    next: { revalidate: 60 },
  });
  if (!res.ok) {
    return null;
  }
  return res.json() as Promise<PredictionMarket>;
}

export function getPollsters() {
  return apiFetch<Pollster[]>("/api/v1/pollsters");
}

export function getPolls(options: { pollster?: string; page?: number; size?: number } = {}) {
  const params = new URLSearchParams();
  if (options.pollster) params.set("pollster", options.pollster);
  params.set("page", String(options.page ?? 0));
  params.set("size", String(options.size ?? 20));
  return apiFetch<PageResponse<Poll>>(`/api/v1/polls?${params.toString()}`);
}

// Optional/supplementary section: any failure means "no polls to show", never a broken homepage.
// Shows the newest approved poll of every pollster, newest first, so no single agency is singled out.
export async function getLatestPollPerPollster(): Promise<Poll[]> {
  try {
    const pollsters = (await getPollsters()).filter((p) => p.approvedPollCount > 0);
    const latest = await Promise.all(
      pollsters.map(async (p) => (await getPolls({ pollster: p.slug, size: 1 })).content[0]),
    );
    return latest
      .filter((poll): poll is Poll => Boolean(poll))
      .sort((a, b) => b.publishedAt.localeCompare(a.publishedAt));
  } catch {
    return [];
  }
}

// Optional/supplementary section: a failed request just means no timeline, not a broken homepage.
export async function getCurrentEvents(): Promise<ElectionEvent[]> {
  try {
    return await apiFetch<ElectionEvent[]>("/api/v1/elections/current/events");
  } catch {
    return [];
  }
}

// ---- Visitor survey (V6) ----

export type SurveyRole = "VOTE_INTENTION" | "TURNOUT" | "AGE" | "SEX" | "REGION" | "SETTLEMENT";
export type SurveyOptionKind = "CHOICE" | "UNDECIDED" | "WONT_VOTE" | "NO_ANSWER";

export type SurveyOption = {
  id: number;
  position: number;
  label: string;
  kind: SurveyOptionKind;
  addedOn: string | null;
};

export type SurveyQuestion = {
  id: number;
  position: number;
  role: SurveyRole;
  text: string;
  options: SurveyOption[];
};

export type Survey = {
  id: number;
  slug: string;
  title: string;
  opensAt: string;
  closesAt: string;
  acceptingAnswers: boolean;
  resultsVisible: boolean;
  responseCount: number;
  minWeightedResponses: number;
  botCheckRequired: boolean;
  questions: SurveyQuestion[];
};

export type SurveyOptionShare = {
  optionId: number;
  label: string;
  position: number;
  kind: SurveyOptionKind;
  count: number;
  rawPct: number | null;
  weightedPct: number | null;
};

export type SurveyShareSet = { base: number; options: SurveyOptionShare[] };

export type SurveyWeighting = {
  effectiveSampleSize: number;
  designEffect: number;
  converged: boolean;
  maxDeviationPct: number;
  usedDimensions: string[];
  droppedDimensions: string[];
  smallSample: boolean;
};

export type SurveyStructureCategory = {
  code: string;
  label: string;
  sampleCount: number;
  samplePct: number | null;
  populationPct: number | null;
};

export type SurveyStructureDimension = { dimension: string; categories: SurveyStructureCategory[] };

export type SurveyResults = {
  surveyId: number;
  computedAt: string;
  opensAt: string;
  closesAt: string;
  responseCount: number;
  minWeightedResponses: number;
  weightedAvailable: boolean;
  weighting: SurveyWeighting | null;
  voteIntention: { lists: SurveyShareSet; likelyVoters: SurveyShareSet; others: SurveyShareSet };
  turnout: SurveyShareSet;
  structure: SurveyStructureDimension[];
};

// Optional section: no survey (or a backend that is down) means the survey is simply not shown.
export async function getCurrentSurvey(): Promise<Survey | null> {
  try {
    const res = await fetch(`${API_URL}/api/v1/surveys/current`, { next: { revalidate: 60 } });
    return res.ok ? ((await res.json()) as Survey) : null;
  } catch {
    return null;
  }
}

/** Null when the results are not available (not open yet, or closed for the election silence). */
export async function getSurveyResults(surveyId: number): Promise<SurveyResults | null> {
  try {
    const res = await fetch(`${API_URL}/api/v1/surveys/${surveyId}/results`, { next: { revalidate: 60 } });
    return res.ok ? ((await res.json()) as SurveyResults) : null;
  } catch {
    return null;
  }
}
