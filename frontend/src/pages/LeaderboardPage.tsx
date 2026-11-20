import { useState } from 'react';
import { Link } from 'react-router';
import { useLeaderboard } from '../api/hooks';
import type { Period, Scope } from '../api/types';
import { Avatar, ErrorBox, Pagination, Spinner } from '../components/ui';
import { BRANCH_SHORT, formatDate } from '../lib/format';

const MEDAL = ['🥇', '🥈', '🥉'];

export function LeaderboardPage() {
  const [period, setPeriod] = useState<Period>('WEEK');
  const [scope, setScope] = useState<Scope>('OVERALL');
  const [page, setPage] = useState(0);
  const lb = useLeaderboard(period, scope, page);

  return (
    <div className="space-y-4">
      <div>
        <h1 className="page-title">Leaderboard</h1>
        <p className="muted">Problems logged in the prep tracker. Opt-in only, and it works on the honour system.</p>
      </div>
      <div className="flex flex-wrap gap-2">
        <select className="input w-auto" aria-label="Period" value={period} onChange={(e) => { setPeriod(e.target.value as Period); setPage(0); }}>
          <option value="WEEK">This week</option><option value="MONTH">This month</option><option value="ALL">All time</option>
        </select>
        <select className="input w-auto" aria-label="Scope" value={scope} onChange={(e) => { setScope(e.target.value as Scope); setPage(0); }}>
          <option value="OVERALL">Everyone</option><option value="COHORT">My branch & batch</option>
        </select>
      </div>

      {lb.isPending && <Spinner />}
      {lb.error && <ErrorBox error={lb.error} />}
      {lb.data && (
        <>
          {!lb.data.optedIn && (
            <div className="rounded-xl bg-brand-50 p-3 text-sm dark:bg-slate-900">
              You&apos;re not on the leaderboard. <Link to="/me" className="underline">Opt in from your profile</Link> if you&apos;d like to appear.
            </div>
          )}
          {lb.data.me && <p className="text-sm">Your rank: <strong>#{lb.data.me.rank}</strong> with {lb.data.me.solved} problems.</p>}
          <p className="muted">
            {formatDate(lb.data.from)} – {formatDate(lb.data.to)}
            {lb.data.cohortBranch && ` · ${BRANCH_SHORT[lb.data.cohortBranch] ?? lb.data.cohortBranch} ${lb.data.cohortYear}`}
            {' · '}{lb.data.totalRanked} ranked
          </p>
          <ol className="card divide-y divide-slate-100 p-0 dark:divide-slate-800">
            {lb.data.entries.length === 0 && <li className="muted p-4">No one has logged problems in this period yet.</li>}
            {lb.data.entries.map((e) => (
              <li key={e.user.id} className={`flex items-center gap-3 px-4 py-2 ${e.me ? 'bg-brand-50 dark:bg-slate-800' : ''}`}>
                <span className="w-10 text-center font-bold">{e.rank <= 3 ? MEDAL[e.rank - 1] : `#${e.rank}`}</span>
                <Avatar name={e.user.displayName} url={e.user.avatarUrl} />
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-medium">{e.user.displayName}{e.me && ' (you)'}</span>
                  <span className="text-xs text-slate-500">{e.user.branchCode ? BRANCH_SHORT[e.user.branchCode] : ''} {e.user.gradYear ?? ''}</span>
                </span>
                <span className="font-semibold">{e.solved}</span>
              </li>
            ))}
          </ol>
          <Pagination page={page} totalPages={Math.ceil(lb.data.totalRanked / 20)} onPage={setPage} />
          <p className="text-xs text-slate-500">Ties share a rank (1, 1, 3…), as in SQL&apos;s RANK().</p>
        </>
      )}
    </div>
  );
}
