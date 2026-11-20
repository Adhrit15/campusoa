import { Link, Navigate, useParams } from 'react-router';
import { useBranches, useDrive, useMe } from '../api/hooks';
import { EligibilityBadge } from '../components/EligibilityBadge';
import { QueryState } from '../components/ui';
import { BRANCH_SHORT, formatDate, formatDateTime, STAGE_LABEL } from '../lib/format';

export function DriveDetailPage() {
  const id = Number(useParams().id);
  const drive = useDrive(id);
  const { data: me } = useMe();
  const branches = useBranches();

  return (
    <QueryState query={drive}>
      {(d) => {
        if (d.mergedIntoId) return <Navigate to={`/drives/${d.mergedIntoId}`} replace />;
        const company = d.company?.name ?? d.companyGuess ?? 'Unknown company';
        return (
          <div className="mx-auto max-w-3xl space-y-4">
            <Link to="/drives" className="text-sm underline">← All drives</Link>
            <div className="card space-y-3">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <h1 className="page-title">{company}</h1>
                  <p className="muted">{d.title}</p>
                </div>
                <time className="chip" dateTime={d.noticeDate}>{formatDate(d.noticeDate)}</time>
              </div>
              {(d.roleTitle || d.ctcText) && (
                <p className="text-sm">{[d.roleTitle, d.ctcText].filter(Boolean).join(' · ')}</p>
              )}
              <div className="flex flex-wrap gap-2">
                {d.pdfUrl && (
                  <a className="btn-primary" href={d.pdfUrl} target="_blank" rel="noopener noreferrer">Open official circular (PDF)</a>
                )}
                {d.company && <Link className="btn-ghost" to={`/companies/${d.company.slug}`}>Company page & experiences</Link>}
              </div>
            </div>

            <div className="card">
              <h2 className="mb-2 font-semibold">Your eligibility</h2>
              {me ? (
                <EligibilityBadge eligibility={d.eligibility} showReasons />
              ) : (
                <p className="muted">Sign in and fill in your profile to check eligibility. Your CGPA stays private.</p>
              )}
              {d.criteria ? (
                <dl className="mt-3 grid grid-cols-2 gap-2 text-sm sm:grid-cols-4">
                  <div><dt className="muted">Min CGPA</dt><dd>{d.criteria.minCgpa ?? 'Any'}</dd></div>
                  <div><dt className="muted">Max backlogs</dt><dd>{d.criteria.maxBacklogs ?? 'Any'}</dd></div>
                  <div><dt className="muted">Batch</dt><dd>{d.criteria.gradYear ?? 'Any'}</dd></div>
                  <div>
                    <dt className="muted">Branches</dt>
                    <dd>
                      {d.criteria.branches.length === 0
                        ? 'All'
                        : d.criteria.branches.map((b) => BRANCH_SHORT[b] ?? branches.data?.find((x) => x.code === b)?.name ?? b).join(', ')}
                    </dd>
                  </div>
                </dl>
              ) : (
                <p className="muted mt-2">Criteria haven&apos;t been entered yet. Check the circular.</p>
              )}
              {d.criteria?.notes && <p className="mt-2 text-sm">{d.criteria.notes}</p>}
            </div>

            <div className="card">
              <h2 className="mb-2 font-semibold">Stages</h2>
              {d.stages.length === 0 ? (
                <p className="muted">No stages listed yet.</p>
              ) : (
                <ol className="space-y-2">
                  {d.stages.map((s) => (
                    <li key={s.id} className="flex flex-wrap items-center gap-2 text-sm">
                      <span className="chip">{STAGE_LABEL[s.stageType]}</span>
                      {s.startsAt ? <span>{formatDateTime(s.startsAt)} IST</span> : <span className="muted">time TBA</span>}
                      {s.mode && <span className="muted">· {s.mode.toLowerCase()}</span>}
                      {s.notes && <span className="muted">· {s.notes}</span>}
                    </li>
                  ))}
                </ol>
              )}
            </div>
          </div>
        );
      }}
    </QueryState>
  );
}
