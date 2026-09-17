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
