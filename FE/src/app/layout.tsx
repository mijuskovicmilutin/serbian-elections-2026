import type { Metadata } from "next";
import { Golos_Text, Wix_Madefor_Text, IBM_Plex_Mono } from "next/font/google";
import "./globals.css";

const golosText = Golos_Text({
  variable: "--font-display",
  subsets: ["latin", "cyrillic"],
  weight: ["500", "600", "700", "800"],
});

const wixMadeforText = Wix_Madefor_Text({
  variable: "--font-body",
  subsets: ["latin", "cyrillic"],
  weight: ["400", "500", "600"],
});

const ibmPlexMono = IBM_Plex_Mono({
  variable: "--font-mono",
  subsets: ["latin", "cyrillic"],
  weight: ["500", "600"],
});

export const metadata: Metadata = {
  title: "Izbori 2026",
  description:
    "Неутралан информациони портал за парламентарне изборе у Србији 2026 — подаци из јавно означених извора.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="sr"
      className={`${golosText.variable} ${wixMadeforText.variable} ${ibmPlexMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
