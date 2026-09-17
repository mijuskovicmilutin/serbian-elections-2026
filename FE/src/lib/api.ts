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

type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
};

export type PredictionMarketOutcome = {
  name: string;
  price: number;
};

export type PredictionMarket = {
  id: number;
  provider: string;
  marketName: string;
  sourceUrl: string;
  updatedAt: string;
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
