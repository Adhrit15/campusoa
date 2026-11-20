import { useEffect, useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '../api/client';
import { useBranches, useMe } from '../api/hooks';
import type { Me } from '../api/types';
import { Avatar, Field } from '../components/ui';

interface ProfileForm {
  displayName: string;
  branchCode: string;
  gradYear: string;
  cgpa: string;
  activeBacklogs: string;
  leaderboardOptIn: boolean;
}

const toForm = (me: Me): ProfileForm => ({
  displayName: me.displayName,
  branchCode: me.branchCode ?? '',
  gradYear: me.gradYear?.toString() ?? '',
  cgpa: me.cgpa?.toString() ?? '',
  activeBacklogs: me.activeBacklogs?.toString() ?? '',
  leaderboardOptIn: me.leaderboardOptIn,
});

const num = (s: string) => (s.trim() === '' ? null : Number(s));

export function ProfilePage() {
  const { data: me } = useMe();
  const branches = useBranches();
  const qc = useQueryClient();
  const [form, setForm] = useState<ProfileForm | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (me && !form) setForm(toForm(me));
  }, [me, form]);

  const save = useMutation({
    mutationFn: (f: ProfileForm) =>
      api.put<Me>('/api/me/profile', {
        displayName: f.displayName,
        branchCode: f.branchCode || null,
        gradYear: num(f.gradYear),
        cgpa: num(f.cgpa),
        activeBacklogs: num(f.activeBacklogs),
        leaderboardOptIn: f.leaderboardOptIn,
      }),
    onSuccess: (updated) => {
      qc.setQueryData(['me'], updated);
      // Eligibility depends on the profile: refetch everything that shows it.
      qc.invalidateQueries({ queryKey: ['drives'] });
      qc.invalidateQueries({ queryKey: ['drive'] });
      qc.invalidateQueries({ queryKey: ['calendar'] });
      qc.invalidateQueries({ queryKey: ['leaderboard'] });
      setErrors({});
      setSaved(true);
    },
    onError: (e) => e instanceof ApiError && setErrors(e.fieldErrors.length ? e.fieldMap() : { form: e.message }),
  });

  const logout = useMutation({
    mutationFn: () => api.post('/api/auth/logout'),
    onSettled: () => { qc.clear(); window.location.href = '/'; },
  });

  const deleteAccount = useMutation({
    mutationFn: () => api.del('/api/me'),
    onSuccess: () => { qc.clear(); window.location.href = '/?deleted=1'; },
  });

  if (!me || !form) return null;
  const set = (patch: Partial<ProfileForm>) => { setForm({ ...form, ...patch }); setSaved(false); };
  const submit = (e: FormEvent) => { e.preventDefault(); save.mutate(form); };

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <div className="card flex items-center gap-3">
        <Avatar name={me.displayName} url={me.avatarUrl} size={48} />
        <div className="min-w-0">
          <p className="truncate font-semibold">{me.displayName}</p>
          <p className="muted truncate">{me.email} · {me.role.toLowerCase()}</p>
        </div>
        <button className="btn-ghost ml-auto" onClick={() => logout.mutate()}>Sign out</button>
      </div>

      <form onSubmit={submit} className="card space-y-4">
        <h1 className="text-lg font-semibold">Profile</h1>
        <Field label="Display name" error={errors.displayName}>
          <input className="input" value={form.displayName} maxLength={100} onChange={(e) => set({ displayName: e.target.value })} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Branch" error={errors.branchCode}>
            <select className="input" value={form.branchCode} onChange={(e) => set({ branchCode: e.target.value })}>
              <option value="">Select…</option>
              {branches.data?.map((b) => <option key={b.code} value={b.code}>{b.name}</option>)}
            </select>
          </Field>
          <Field label="Graduation year" error={errors.gradYear}>
            <input className="input" type="number" min={2020} max={2040} placeholder="2028" value={form.gradYear} onChange={(e) => set({ gradYear: e.target.value })} />
          </Field>
        </div>

        <fieldset className="rounded-xl border border-slate-200 p-3 dark:border-slate-800">
          <legend className="px-1 text-sm font-medium">🔒 Private: only you can see these</legend>
          <p className="muted mb-3">Used only to show your eligibility on each drive. Never shown to other students, moderators or admins.</p>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="CGPA (optional)" error={errors.cgpa}>
              <input className="input" type="number" step="0.01" min={0} max={10} value={form.cgpa} onChange={(e) => set({ cgpa: e.target.value })} />
            </Field>
            <Field label="Active backlogs (optional)" error={errors.activeBacklogs}>
              <input className="input" type="number" min={0} max={60} value={form.activeBacklogs} onChange={(e) => set({ activeBacklogs: e.target.value })} />
            </Field>
          </div>
        </fieldset>

        <label className="flex items-start gap-2 text-sm">
          <input type="checkbox" className="mt-1" checked={form.leaderboardOptIn} onChange={(e) => set({ leaderboardOptIn: e.target.checked })} />
          <span>Show me on leaderboards (your name, branch, batch and number of problems solved).</span>
        </label>
        {errors.form && <p className="text-sm text-rose-600">{errors.form}</p>}
        <div className="flex items-center gap-3">
          <button className="btn-primary" disabled={save.isPending}>Save</button>
          {saved && <span className="text-sm text-emerald-600">Saved ✓</span>}
        </div>
      </form>

      <div className="card border-rose-200 dark:border-rose-900">
        <h2 className="font-semibold text-rose-700 dark:text-rose-300">Delete account</h2>
        <p className="muted mt-1">
          Permanently deletes your profile, private details, prep progress, problem log and votes. Posts you wrote stay
          up as &ldquo;Deleted user&rdquo; so juniors can still learn from them. Delete them first if you want them gone.
        </p>
        <button className="btn-danger mt-3" disabled={deleteAccount.isPending}
          onClick={() => confirm('Delete your CampusOA account permanently? This cannot be undone.') && deleteAccount.mutate()}>
          Delete my account
        </button>
      </div>
    </div>
  );
}
