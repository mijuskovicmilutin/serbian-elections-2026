import { createHash, timingSafeEqual } from "node:crypto";

const USERNAME = "admin";

function digest(value: string): Buffer {
  return createHash("sha256").update(value).digest();
}

export function isAdminEnabled(): boolean {
  return Boolean(process.env.ADMIN_API_KEY);
}

/** Basic auth where the password is ADMIN_API_KEY; digests are compared so timing reveals nothing. */
export function isAdminAuthorized(authorization: string | null): boolean {
  const key = process.env.ADMIN_API_KEY;
  if (!key || !authorization?.startsWith("Basic ")) return false;

  const decoded = Buffer.from(authorization.slice("Basic ".length), "base64").toString("utf8");
  const separator = decoded.indexOf(":");
  if (separator < 0) return false;

  const userOk = timingSafeEqual(digest(decoded.slice(0, separator)), digest(USERNAME));
  const passOk = timingSafeEqual(digest(decoded.slice(separator + 1)), digest(key));
  return userOk && passOk;
}
