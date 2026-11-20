import { Link, useParams } from 'react-router';
import { useCompany, useDrives, useExperiences, useQuestions } from '../api/hooks';
import { Markdown } from '../components/Markdown';
import { Empty, QueryState, SourceNote } from '../components/ui';
import { inr, STAGE_LABEL } from '../lib/format';
import { DriveCard } from './DrivesPage';
import { ExperienceCard } from './ExperiencesPage';
import { QuestionCard } from './QuestionsPage';

export function CompanyPage() {
  const slug = useParams().slug!;
  const company = useCompany(slug);
  const drives = useDrives({ company: slug, page: 0 });
  const experiences = useExperiences({ company: slug, sort: 'top', page: 0 });
  const questions = useQuestions({ company: slug, sort: 'top', page: 0 });

  return (
    <QueryState query={company}>
      {(c) => (
        <div className="space-y-6">
          <div className="flex flex-wrap items-end justify-between gap-2">
            <div>
              <Link to="/companies" className="text-sm underline">← Companies</Link>
              <h1 className="page-title mt-1">{c.name}</h1>
              {c.website && (
                <a href={c.website} className="muted underline" target="_blank" rel="noopener noreferrer">{c.website}</a>
              )}
            </div>
            <Link to={`/experiences/new?company=${c.id}`} className="btn-primary">Share your {c.name} experience</Link>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <section className="card">
              <h2 className="mb-2 font-semibold">Full-time offers</h2>
              {c.placements.length === 0 ? (
                <p className="muted">No data yet.</p>
              ) : (
                <table className="w-full text-sm">
                  <thead className="text-left text-xs text-slate-500">
                    <tr><th className="py-1">Year</th><th>Role</th><th>CTC</th><th>Offers</th></tr>
                  </thead>
                  <tbody>
                    {c.placements.map((p) => (
                      <tr key={p.id} className="border-t border-slate-100 dark:border-slate-800">
                        <td className="py-1">{p.academicYear}</td>
                        <td>{p.roleTitle}</td>
                        <td className="font-medium">{p.ctcLabel}</td>
                        <td>{p.offers ?? '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              <SourceNote note={c.placements[0]?.sourceNote} url={c.placements[0]?.sourceUrl} />
            </section>

            <section className="card">
              <h2 className="mb-2 font-semibold">Internship stipend</h2>
              {c.internships.length === 0 ? (
                <p className="muted">Not known.</p>
              ) : (
                <ul className="text-sm">
                  {c.internships.map((i) => (
                    <li key={i.id}><span className="font-medium">{inr(i.stipendInrMonth)}</span>/month · {i.academicYear}</li>
                  ))}
                </ul>
              )}
              <SourceNote note={c.internships[0]?.sourceNote} url={c.internships[0]?.sourceUrl} />

              <h2 className="mb-2 mt-4 font-semibold">Selection process</h2>
              {c.selectionProcess.length === 0 ? (
                <p className="muted">Not added yet. Read the experiences below.</p>
              ) : (
                <ol className="list-decimal pl-5 text-sm">
                  {c.selectionProcess.map((s) => (
                    <li key={s.position}>{STAGE_LABEL[s.stageType]}{s.description ? `: ${s.description}` : ''}</li>
                  ))}
                </ol>
              )}
            </section>
          </div>

          <section className="card">
            <h2 className="mb-2 font-semibold">How to prepare</h2>
            {c.prepMarkdown ? <Markdown>{c.prepMarkdown}</Markdown> : <p className="muted">No guide yet. Admins can add one.</p>}
          </section>

          <section>
            <h2 className="mb-2 text-lg font-semibold">Drives</h2>
            <QueryState query={drives}>
              {(p) =>
                p.content.length === 0 ? <Empty title="No drives linked yet" /> : (
                  <div className="grid gap-3 sm:grid-cols-2">{p.content.slice(0, 6).map((d) => <DriveCard key={d.id} drive={d} />)}</div>
                )
              }
            </QueryState>
          </section>

          <section>
            <div className="mb-2 flex items-baseline justify-between">
              <h2 className="text-lg font-semibold">Interview experiences</h2>
              <Link className="text-sm underline" to={`/experiences?company=${c.slug}`}>See all</Link>
            </div>
            <QueryState query={experiences}>
              {(p) =>
                p.content.length === 0 ? <Empty title="No experiences yet">Be the first to share one.</Empty> : (
                  <div className="space-y-3">{p.content.slice(0, 5).map((e) => <ExperienceCard key={e.id} e={e} />)}</div>
                )
              }
            </QueryState>
          </section>

          <section>
            <h2 className="mb-2 text-lg font-semibold">Questions asked</h2>
            <QueryState query={questions}>
              {(p) =>
                p.content.length === 0 ? <Empty title="No questions yet" /> : (
                  <div className="space-y-3">{p.content.slice(0, 8).map((q) => <QuestionCard key={q.id} q={q} />)}</div>
                )
              }
            </QueryState>
          </section>
        </div>
      )}
    </QueryState>
  );
}
