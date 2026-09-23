"use server";

import { cookies, headers } from "next/headers";
import { redirect } from "next/navigation";
import { SURVEY_COOKIE_MAX_AGE_SECONDS, surveyCookieName } from "@/lib/surveyCookie";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export type SurveyFormState = { status: "idle" | "error"; message: string };

const ERROR = (message: string): SurveyFormState => ({ status: "error", message });

/**
 * Sends the answers to the backend. The visitor's address is passed on in X-Forwarded-For (the backend only reads it
 * when it is told to trust that header) and is used there for the duplicate guard: it is never stored by this app.
 */
export async function submitSurvey(_previous: SurveyFormState, formData: FormData): Promise<SurveyFormState> {
  const surveyId = Number(formData.get("surveyId"));
  if (!Number.isInteger(surveyId) || surveyId < 1) return ERROR("Анкета није пронађена.");

  const answers: { questionId: number; optionId: number }[] = [];
  for (const [key, value] of formData.entries()) {
    const match = /^q_(\d+)$/.exec(key);
    if (match && typeof value === "string" && /^\d+$/.test(value)) {
      answers.push({ questionId: Number(match[1]), optionId: Number(value) });
    }
  }
  if (answers.length === 0) return ERROR("Одговорите на питања пре слања.");

  const token = formData.get("cf-turnstile-response");
  const requestHeaders = await headers();
  const forwarded = requestHeaders.get("x-forwarded-for")?.split(",")[0]?.trim() || requestHeaders.get("x-real-ip");

  let status: number;
  try {
    const res = await fetch(`${API_URL}/api/v1/surveys/${surveyId}/responses`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...(forwarded ? { "X-Forwarded-For": forwarded } : {}) },
      body: JSON.stringify({ answers, turnstileToken: typeof token === "string" ? token : null }),
      cache: "no-store",
    });
    status = res.status;
  } catch {
    return ERROR("Сервер тренутно није доступан. Покушајте поново за који минут.");
  }

  if (status === 201) {
    const cookieStore = await cookies();
    cookieStore.set(surveyCookieName(surveyId), "1", {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      path: "/",
      maxAge: SURVEY_COOKIE_MAX_AGE_SECONDS,
    });
    redirect("/anketa/hvala");
  }
  if (status === 429) return ERROR("Са ваше мреже је већ послато највише дозвољено одговора, или је послато превише одговора одједном. Покушајте касније.");
  if (status === 403) return ERROR("Провера да нисте робот није прошла. Освежите страницу и покушајте поново.");
  if (status === 422) return ERROR("Анкета више не прима одговоре.");
  if (status === 503) return ERROR("Слање одговора тренутно није омогућено.");
  return ERROR("Одговори нису прихваћени. Проверите да сте одговорили на сва питања и покушајте поново.");
}
