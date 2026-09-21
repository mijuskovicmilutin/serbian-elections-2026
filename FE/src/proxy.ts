import { NextResponse, type NextRequest } from "next/server";
import { isAdminAuthorized, isAdminEnabled } from "@/lib/adminAuth";

const NO_INDEX = "noindex, nofollow";

export function proxy(request: NextRequest) {
  if (!isAdminEnabled()) {
    return new NextResponse("Not found", { status: 404 });
  }

  if (!isAdminAuthorized(request.headers.get("authorization"))) {
    return new NextResponse("Authentication required", {
      status: 401,
      headers: {
        "WWW-Authenticate": 'Basic realm="izbori2026 admin", charset="UTF-8"',
        "X-Robots-Tag": NO_INDEX,
      },
    });
  }

  const response = NextResponse.next();
  response.headers.set("X-Robots-Tag", NO_INDEX);
  response.headers.set("Cache-Control", "no-store");
  return response;
}

export const config = {
  matcher: ["/admin", "/admin/:path*"],
};
