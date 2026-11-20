import type { Eligibility } from '../api/types';
import { ELIGIBILITY_LABEL } from '../lib/format';

const STYLE = {
  ELIGIBLE: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300',
  NOT_ELIGIBLE: 'bg-rose-100 text-rose-800 dark:bg-rose-950 dark:text-rose-300',
  UNKNOWN: 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300',
} as const;

const ICON = { ELIGIBLE: '✓', NOT_ELIGIBLE: '✕', UNKNOWN: '?' } as const;

/**
 * Shows the server-computed eligibility. Renders nothing when signed out (eligibility undefined).
 * With {@code showReasons}, lists why (e.g. "Minimum CGPA is 7.5").
 */
export function EligibilityBadge({ eligibility, showReasons = false }: { eligibility?: Eligibility; showReasons?: boolean }) {
  if (!eligibility) return null;
  const { status, reasons } = eligibility;
  return (
    <div>
      <span
        className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${STYLE[status]}`}
        title={reasons.join('\n')}
      >
        <span aria-hidden>{ICON[status]}</span>
        {ELIGIBILITY_LABEL[status]}
      </span>
      {showReasons && reasons.length > 0 && (
        <ul className="mt-2 list-disc pl-5 text-sm text-slate-600 dark:text-slate-300">
          {reasons.map((r) => (
            <li key={r}>{r}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
