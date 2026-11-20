import type { EligibilityStatus, Outcome, StageType } from '../api/types';

const IST = 'Asia/Kolkata';

/** "20 Nov 2026" from "2026-11-20" (a calendar date: no timezone shifting). */
export function formatDate(isoDate: string): string {
  const [y, m, d] = isoDate.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d)).toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  });
}

/** "Fri, 20 Nov, 10:30 am" in IST, from an ISO instant. */
export function formatDateTime(instant: string): string {
  return new Date(instant).toLocaleString('en-IN', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    hour: 'numeric',
    minute: '2-digit',
    timeZone: IST,
  });
}

export function timeAgo(instant: string, now: Date = new Date()): string {
  const s = Math.round((now.getTime() - new Date(instant).getTime()) / 1000);
  if (s < 60) return 'just now';
  const m = Math.round(s / 60);
  if (m < 60) return `${m} min ago`;
  const h = Math.round(m / 60);
  if (h < 24) return `${h} h ago`;
  const d = Math.round(h / 24);
  if (d < 30) return `${d} d ago`;
  return formatDate(instant.slice(0, 10));
}

/**
 * Plain-text preview of markdown for cards: drops heading/emphasis/quote markers and turns
 * [text](url) into text. Leaves [[highlight]] markers from search intact.
 */
export function stripMarkdown(md: string): string {
  return md
    .replace(/!?\[([^[\]]+)\]\([^)]*\)/g, '$1')
    .replace(/^\s{0,3}(#{1,6}|>|[-*+]|\d+\.)\s+/gm, '')
    .replace(/(\*\*|__|\*|_|`)/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

/** Indian digit grouping: 125000 -> "₹1,25,000". */
export function inr(amount: number): string {
  return '₹' + amount.toLocaleString('en-IN');
}

/** Today's date in IST as YYYY-MM-DD. */
export function todayIst(): string {
  return new Date().toLocaleDateString('en-CA', { timeZone: IST });
}

export const STAGE_LABEL: Record<StageType | 'NOTICE', string> = {
  NOTICE: 'Circular',
  PPT: 'Pre-placement talk',
  OA: 'Online assessment',
  GD: 'Group discussion',
  INTERVIEW: 'Interview',
  HR: 'HR round',
  OTHER: 'Other',
};

export const OUTCOME_LABEL: Record<Outcome, string> = {
  SELECTED: 'Selected',
  REJECTED: 'Not selected',
  WAITING: 'Awaiting result',
  WITHDREW: 'Withdrew',
};

export const ELIGIBILITY_LABEL: Record<EligibilityStatus, string> = {
  ELIGIBLE: 'Eligible',
  NOT_ELIGIBLE: 'Not eligible',
  UNKNOWN: 'Criteria unknown',
};

export const BRANCH_SHORT: Record<string, string> = {
  CSE: 'CSE',
  CSE_AIML: 'CSE-AIML',
  CSE_IOT: 'CSE-IoT',
  IT: 'IT',
  AIDS: 'AI&DS',
  AIML: 'AIML',
  ECE: 'ECE',
  EEE: 'EEE',
  MECH: 'Mech',
  CIVIL: 'Civil',
  CHEM: 'Chem',
  BIOTECH: 'Biotech',
};
