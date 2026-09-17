"use client";

import { useEffect, useState } from "react";
import styles from "@/app/page.module.css";

type Props = {
  targetIso: string;
};

type Remaining = {
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
};

function computeRemaining(targetMs: number): Remaining {
  const diff = Math.max(0, targetMs - Date.now());
  return {
    days: Math.floor(diff / 86_400_000),
    hours: Math.floor(diff / 3_600_000) % 24,
    minutes: Math.floor(diff / 60_000) % 60,
    seconds: Math.floor(diff / 1_000) % 60,
  };
}

export default function Countdown({ targetIso }: Props) {
  const targetMs = new Date(targetIso).getTime();
  // Starts null so server and first client render match exactly (both show
  // placeholders); the real, time-sensitive value only appears after mount.
  const [remaining, setRemaining] = useState<Remaining | null>(null);

  useEffect(() => {
    setRemaining(computeRemaining(targetMs));
    const id = setInterval(() => {
      setRemaining(computeRemaining(targetMs));
    }, 1000);
    return () => clearInterval(id);
  }, [targetMs]);

  const cells: Array<[string, keyof Remaining]> = [
    ["дана", "days"],
    ["сати", "hours"],
    ["минута", "minutes"],
    ["секунди", "seconds"],
  ];

  return (
    <div className={styles.countdown}>
      {cells.map(([label, key]) => (
        <div className={styles.countdownCell} key={key}>
          <span className={styles.countdownNum}>
            {remaining ? String(remaining[key]).padStart(2, "0") : "--"}
          </span>
          <span className={styles.countdownLabel}>{label}</span>
        </div>
      ))}
    </div>
  );
}
