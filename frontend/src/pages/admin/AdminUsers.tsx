import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '../../api/client';
import { useAdminUsers, useMe } from '../../api/hooks';
import type { Role } from '../../api/types';
import { Pagination, QueryState } from '../../components/ui';
import { timeAgo } from '../../lib/format';

export function AdminUsers() {
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const users = useAdminUsers(q, page);
  const { data: me } = useMe();
  const qc = useQueryClient();
  const refresh = () => qc.invalidateQueries({ queryKey: ['admin-users'] });
  const setRole = useMutation({ mutationFn: ({ id, role }: { id: number; role: Role }) => api.put(`/api/admin/users/${id}/role`, { role }), onSuccess: refresh });
  const setBan = useMutation({ mutationFn: ({ id, banned }: { id: number; banned: boolean }) => api.put(`/api/admin/users/${id}/ban`, { banned }), onSuccess: refresh });
  const error = setRole.error ?? setBan.error;

  return (
    <div className="space-y-3">
      <input className="input" placeholder="Search by email or name…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
      <p className="text-xs text-slate-500">Changing a role or banning signs that user out everywhere. Private CGPA/backlogs are never shown here.</p>
      {error && <p className="text-sm text-rose-600">{(error as ApiError).message}</p>}
      <QueryState query={users}>
        {(p) => (
          <>
            <div className="overflow-x-auto rounded-2xl border border-slate-200 dark:border-slate-800">
              <table className="w-full text-sm">
                <thead className="bg-slate-100 text-left text-xs uppercase text-slate-500 dark:bg-slate-900">
                  <tr><th className="px-3 py-2">User</th><th className="px-3 py-2">Role</th><th className="px-3 py-2">Last login</th><th className="px-3 py-2"></th></tr>
                </thead>
                <tbody>
                  {p.content.map((u) => (
                    <tr key={u.id} className="border-t border-slate-200 dark:border-slate-800">
                      <td className="px-3 py-2"><span className="font-medium">{u.displayName}</span><span className="muted block">{u.email}</span></td>
                      <td className="px-3 py-2">
                        <select className="input py-1" value={u.role} disabled={u.id === me?.id} onChange={(e) => setRole.mutate({ id: u.id, role: e.target.value as Role })}>
                          <option value="STUDENT">Student</option><option value="MODERATOR">Moderator</option><option value="ADMIN">Admin</option>
                        </select>
                      </td>
                      <td className="px-3 py-2 text-xs">{u.lastLoginAt ? timeAgo(u.lastLoginAt) : 'never'}</td>
                      <td className="px-3 py-2">
                        {u.id !== me?.id && (
                          <button className={u.banned ? 'btn-ghost py-1' : 'btn-danger py-1'}
                            onClick={() => (u.banned || confirm(`Ban ${u.email}?`)) && setBan.mutate({ id: u.id, banned: !u.banned })}>
                            {u.banned ? 'Unban' : 'Ban'}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={p.page} totalPages={p.totalPages} onPage={setPage} />
          </>
        )}
      </QueryState>
    </div>
  );
}
