const API_URL = process.env.NEXT_PUBLIC_API_URL;

export type AdminResult<T> = { ok: true; data: T } | { ok: false; status: number; message: string };

/** Server-side only: attaches the admin key, which must never reach the browser. */
export async function adminRequest<T>(
  path: string,
  init: { method?: "GET" | "POST" | "PUT"; body?: unknown } = {},
): Promise<AdminResult<T>> {
  const key = process.env.ADMIN_API_KEY;
  if (!key) return { ok: false, status: 404, message: "Admin није омогућен." };

  try {
    const res = await fetch(`${API_URL}${path}`, {
      method: init.method ?? "GET",
      headers: { "X-Admin-Key": key, "Content-Type": "application/json" },
      body: init.body === undefined ? undefined : JSON.stringify(init.body),
      cache: "no-store",
    });
    const text = await res.text();
    if (!res.ok) {
      let message = `Грешка ${res.status}`;
      try {
        message = (JSON.parse(text) as { message?: string }).message ?? message;
      } catch {
        // keep the generic message
      }
      return { ok: false, status: res.status, message };
    }
    return { ok: true, data: (text ? JSON.parse(text) : null) as T };
  } catch {
    return { ok: false, status: 0, message: "Бекенд није доступан." };
  }
}
