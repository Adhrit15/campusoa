import { Link } from 'react-router';
import { useCompanies, useDrives, useMe } from '../api/hooks';
import { DriveCard } from './DrivesPage';
import { QueryState } from '../components/ui';

export function HomePage() {
  const { data: me } = useMe();
  const drives = useDrives({ page: 0 });
  const companies = useCompanies('', 'ctc', 0, 6);

  return (
    <div className="space-y-8">
      <section className="rounded-3xl bg-gradient-to-br from-brand-600 to-indigo-700 p-6 text-white shadow-lg sm:p-10">
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Placement prep, together.</h1>
        <p className="mt-2 max-w-2xl text-brand-100">
          Drive calendar from public CDC circulars, company pages with past CTCs, interview experiences from seniors,
          an eligibility checker and a prep tracker. Built by students, for students.
        </p>
        <div className="mt-5 flex flex-wrap gap-2">
          {me ? (
            <Link to="/prep" className="btn bg-white text-brand-700 hover:bg-brand-50">Open my prep tracker</Link>
          ) : (
            <Link to="/login" className="btn bg-white text-brand-700 hover:bg-brand-50">Sign in with college Google</Link>
          )}
          <Link to="/experiences/new" className="btn border border-white/40 text-white hover:bg-white/10">Share an experience</Link>
        </div>
      </section>

      <section>
        <div className="mb-3 flex items-baseline justify-between">
          <h2 className="text-lg font-semibold">Latest drives</h2>
          <Link to="/drives" className="text-sm text-brand-600 underline dark:text-brand-100">All drives →</Link>
        </div>
        <QueryState query={drives}>
          {(page) => (
            <div className="grid gap-3 sm:grid-cols-2">
              {page.content.slice(0, 6).map((d) => <DriveCard key={d.id} drive={d} />)}
            </div>
          )}
        </QueryState>
      </section>

      <section>
        <div className="mb-3 flex items-baseline justify-between">
          <h2 className="text-lg font-semibold">Top recruiters by CTC</h2>
          <Link to="/companies" className="text-sm text-brand-600 underline dark:text-brand-100">All companies →</Link>
        </div>
        <QueryState query={companies}>
          {(page) => (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
              {page.content.map((c) => (
                <Link key={c.id} to={`/companies/${c.slug}`} className="card hover:border-brand-500">
                  <p className="font-medium">{c.name}</p>
                  {c.latestCtcLpa && <p className="muted">{c.latestCtcLpa} LPA · {c.latestYear}</p>}
                </Link>
              ))}
            </div>
          )}
        </QueryState>
        <p className="mt-2 text-xs text-slate-500">Source: CBIT public placement records (approx.)</p>
      </section>
    </div>
  );
}
