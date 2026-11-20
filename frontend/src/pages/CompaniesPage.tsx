import { useState } from 'react';
import { Link } from 'react-router';
import { useCompanies } from '../api/hooks';
import { Empty, Pagination, QueryState } from '../components/ui';

export function CompaniesPage() {
  const [q, setQ] = useState('');
  const [sort, setSort] = useState<'ctc' | 'name'>('ctc');
  const [page, setPage] = useState(0);
  const companies = useCompanies(q, sort, page);

  return (
    <div className="space-y-4">
      <div>
        <h1 className="page-title">Companies</h1>
        <p className="muted">
          Past CTC and offers are approximate, from{' '}
          <a className="underline" href="https://cbit.ac.in/placement_post/year-wise-placements-2021/" target="_blank" rel="noopener noreferrer">
            CBIT public placement records
          </a>
          .
        </p>
      </div>
      <div className="flex gap-2">
        <input className="input" type="search" placeholder="Search companies…" aria-label="Search companies" value={q}
          onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        <select className="input w-40" aria-label="Sort" value={sort} onChange={(e) => setSort(e.target.value as 'ctc' | 'name')}>
          <option value="ctc">Highest CTC</option>
          <option value="name">A–Z</option>
        </select>
      </div>
      <QueryState query={companies}>
        {(p) =>
          p.content.length === 0 ? (
            <Empty title="No companies found" />
          ) : (
            <>
              <div className="overflow-x-auto rounded-2xl border border-slate-200 dark:border-slate-800">
                <table className="w-full text-left text-sm">
                  <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-900">
                    <tr>
                      <th className="px-3 py-2">Company</th>
                      <th className="px-3 py-2">Latest CTC</th>
                      <th className="hidden px-3 py-2 sm:table-cell">Offers</th>
                      <th className="hidden px-3 py-2 sm:table-cell">Year</th>
                      <th className="px-3 py-2">Experiences</th>
                    </tr>
                  </thead>
                  <tbody>
                    {p.content.map((c) => (
                      <tr key={c.id} className="border-t border-slate-200 dark:border-slate-800">
                        <td className="px-3 py-2 font-medium">
                          <Link className="hover:underline" to={`/companies/${c.slug}`}>{c.name}</Link>
                        </td>
                        <td className="px-3 py-2">{c.latestCtcLpa ? `${c.latestCtcLpa} LPA` : '—'}</td>
                        <td className="hidden px-3 py-2 sm:table-cell">{c.latestOffers ?? '—'}</td>
                        <td className="hidden px-3 py-2 sm:table-cell">{c.latestYear ?? '—'}</td>
                        <td className="px-3 py-2">{c.experienceCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Pagination page={p.page} totalPages={p.totalPages} onPage={setPage} />
            </>
          )
        }
      </QueryState>
    </div>
  );
}
