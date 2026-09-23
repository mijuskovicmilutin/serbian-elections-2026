/** Name of the cookie that remembers, in this browser only, that the survey was already answered. */
export function surveyCookieName(surveyId: number): string {
  return `anketa_${surveyId}`;
}

/** Longer than the survey itself, so a returning visitor is not asked again before it closes. */
export const SURVEY_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 60;
