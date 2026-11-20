import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '../api/client';
import { useCompanies, useTopics } from '../api/hooks';
import type { Difficulty, ExperienceDetail, ExperienceInput, Outcome, QuestionInput } from '../api/types';
import { Field } from '../components/ui';
import { OUTCOME_LABEL } from '../lib/format';

/** Client-side checks mirror the server's Bean Validation, for instant feedback. The server still re-checks. */
export function validateExperience(v: ExperienceInput): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!v.companyId) errors.companyId = 'Pick a company';
  if (!v.roleTitle.trim()) errors.roleTitle = 'Role is required';
  if (v.bodyMd.trim().length < 50) errors.bodyMd = 'Write at least 50 characters';
  if (v.bodyMd.length > 20000) errors.bodyMd = 'At most 20000 characters';
  if (v.year < 2015 || v.year > 2040) errors.year = 'Year looks wrong';
  v.questions.forEach((q, i) => {
    if (q.bodyMd.trim().length < 10) errors[`questions[${i}].bodyMd`] = 'Describe the question (10+ characters)';
    if (q.topicCodes.length === 0) errors[`questions[${i}].topicCodes`] = 'Pick a topic';
  });
  if (!v.acceptGuidelines) errors.acceptGuidelines = 'Please confirm you followed the guidelines';
  return errors;
}

const emptyQuestion = (): QuestionInput => ({ bodyMd: '', difficulty: 'MEDIUM', topicCodes: [] });

export function NewExperiencePage() {
  const [params] = useSearchParams();
  const [companyQuery, setCompanyQuery] = useState('');
  const companies = useCompanies(companyQuery, 'name', 0, 50);
  const topics = useTopics();
  const navigate = useNavigate();
  const qc = useQueryClient();

  const [form, setForm] = useState<ExperienceInput>({
    companyId: Number(params.get('company') ?? 0),
    year: new Date().getFullYear(),
    roleTitle: '',
    difficulty: 3,
    outcome: 'WAITING',
    bodyMd: '',
    rounds: [{ name: 'Online assessment', description: '' }],
    questions: [],
    anonymous: false,
    acceptGuidelines: false,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = <K extends keyof ExperienceInput>(k: K, v: ExperienceInput[K]) => setForm((f) => ({ ...f, [k]: v }));

  const submit = useMutation({
    mutationFn: (body: ExperienceInput) => api.post<ExperienceDetail>('/api/experiences', body),
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: ['experiences'] });
      navigate(`/experiences/${created.id}`);
    },
    onError: (e) => e instanceof ApiError && setErrors(e.fieldMap()),
  });

  const onSubmit = (ev: FormEvent) => {
    ev.preventDefault();
    const clientErrors = validateExperience(form);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length === 0) {
      submit.mutate({ ...form, rounds: form.rounds.filter((r) => r.name.trim()) });
    }
  };

  const updateQuestion = (i: number, patch: Partial<QuestionInput>) =>
    set('questions', form.questions.map((q, j) => (j === i ? { ...q, ...patch } : q)));

  return (
    <form onSubmit={onSubmit} className="mx-auto max-w-3xl space-y-4" noValidate>
      <h1 className="page-title">Share an interview experience</h1>
      <div className="rounded-xl border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-200">
        Describe questions <strong>in your own words</strong>. Don&apos;t paste or photograph confidential test
        material, and don&apos;t name other candidates. <Link to="/guidelines" className="underline">Read the guidelines</Link>.
      </div>

      <div className="card grid gap-4 sm:grid-cols-2">
        <Field label="Company" error={errors.companyId}>
          <input className="input mb-1" placeholder="Type to filter…" aria-label="Filter companies" value={companyQuery} onChange={(e) => setCompanyQuery(e.target.value)} />
          <select className="input" aria-label="Company" value={form.companyId || ''} onChange={(e) => set('companyId', Number(e.target.value))}>
            <option value="">Select…</option>
            {companies.data?.content.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </Field>
        <Field label="Role" error={errors.roleTitle}>
          <input className="input" placeholder="SDE, Analyst, SDE Intern…" value={form.roleTitle} onChange={(e) => set('roleTitle', e.target.value)} />
        </Field>
        <Field label="Year of interview" error={errors.year}>
          <input className="input" type="number" min={2015} max={2040} value={form.year} onChange={(e) => set('year', Number(e.target.value))} />
        </Field>
        <Field label="Outcome">
          <select className="input" value={form.outcome} onChange={(e) => set('outcome', e.target.value as Outcome)}>
            {Object.entries(OUTCOME_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </Field>
        <Field label={`Difficulty: ${form.difficulty}/5`}>
          <input type="range" min={1} max={5} className="w-full" value={form.difficulty} onChange={(e) => set('difficulty', Number(e.target.value))} />
        </Field>
        <label className="flex items-center gap-2 self-end text-sm">
          <input type="checkbox" checked={form.anonymous} onChange={(e) => set('anonymous', e.target.checked)} />
          Post anonymously (moderators can still see who posted)
        </label>
      </div>

      <div className="card space-y-3">
        <h2 className="font-semibold">Rounds</h2>
        {form.rounds.map((r, i) => (
          <div key={i} className="grid gap-2 sm:grid-cols-3">
            <input className="input" aria-label={`Round ${i + 1} name`} placeholder={`Round ${i + 1}`} value={r.name}
              onChange={(e) => set('rounds', form.rounds.map((x, j) => (j === i ? { ...x, name: e.target.value } : x)))} />
            <input className="input sm:col-span-2" aria-label={`Round ${i + 1} description`} placeholder="What happened (duration, format)…" value={r.description}
              onChange={(e) => set('rounds', form.rounds.map((x, j) => (j === i ? { ...x, description: e.target.value } : x)))} />
          </div>
        ))}
        {form.rounds.length < 10 && (
          <button type="button" className="btn-ghost" onClick={() => set('rounds', [...form.rounds, { name: '', description: '' }])}>+ Add round</button>
        )}
      </div>

      <div className="card">
        <Field label="Your experience (markdown supported)" error={errors.bodyMd} hint={`${form.bodyMd.length} / 20000`}>
          <textarea className="input font-mono" rows={10} value={form.bodyMd} onChange={(e) => set('bodyMd', e.target.value)}
            placeholder="How did you prepare? What was each round like? Tips for juniors?" />
        </Field>
      </div>

      <div className="card space-y-3">
        <h2 className="font-semibold">Questions you were asked (optional)</h2>
        {form.questions.map((q, i) => (
          <div key={i} className="space-y-2 rounded-xl border border-slate-200 p-3 dark:border-slate-800">
            <Field label={`Question ${i + 1}`} error={errors[`questions[${i}].bodyMd`]}>
              <textarea className="input" rows={2} value={q.bodyMd} onChange={(e) => updateQuestion(i, { bodyMd: e.target.value })} placeholder="In your own words…" />
            </Field>
            <div className="grid gap-2 sm:grid-cols-2">
              <Field label="Topic" error={errors[`questions[${i}].topicCodes`]}>
                <select className="input" value={q.topicCodes[0] ?? ''} onChange={(e) => updateQuestion(i, { topicCodes: e.target.value ? [e.target.value] : [] })}>
                  <option value="">Select…</option>
                  {topics.data?.map((t) => <option key={t.code} value={t.code}>{t.parentCode ? `  DSA › ${t.name}` : t.name}</option>)}
                </select>
              </Field>
              <Field label="Difficulty">
                <select className="input" value={q.difficulty} onChange={(e) => updateQuestion(i, { difficulty: e.target.value as Difficulty })}>
                  <option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option>
                </select>
              </Field>
            </div>
            <button type="button" className="text-xs text-rose-600 underline" onClick={() => set('questions', form.questions.filter((_, j) => j !== i))}>Remove</button>
          </div>
        ))}
        {form.questions.length < 15 && (
          <button type="button" className="btn-ghost" onClick={() => set('questions', [...form.questions, emptyQuestion()])}>+ Add question</button>
        )}
      </div>

      <div className="card space-y-3">
        <label className="flex items-start gap-2 text-sm">
          <input type="checkbox" className="mt-1" checked={form.acceptGuidelines} onChange={(e) => set('acceptGuidelines', e.target.checked)} />
          I wrote this in my own words, it contains no confidential test material and no one else&apos;s personal details.
        </label>
        {errors.acceptGuidelines && <p className="text-xs text-rose-600">{errors.acceptGuidelines}</p>}
        {submit.error && !(submit.error instanceof ApiError && submit.error.fieldErrors.length) && (
          <p className="text-sm text-rose-600">{submit.error instanceof ApiError ? submit.error.message : 'Something went wrong'}</p>
        )}
        <button type="submit" className="btn-primary w-full sm:w-auto" disabled={submit.isPending}>
          {submit.isPending ? 'Posting…' : 'Post experience'}
        </button>
      </div>
    </form>
  );
}
