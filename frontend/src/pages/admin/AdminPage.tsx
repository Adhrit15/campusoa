import { NavLink, Route, Routes, Navigate } from 'react-router';
import { useMe } from '../../api/hooks';
import { ModerationQueue, AuditLog } from './ModerationQueue';
import { AdminDrives } from './AdminDrives';
import { AdminCompanies } from './AdminCompanies';
import { AdminUsers } from './AdminUsers';
import { AdminScraper } from './AdminScraper';

/** Moderators see the queue and audit log; admins also see drives, companies, users and the scraper. */
export default function AdminPage() {
  const { data: me } = useMe();
  const isAdmin = me?.role === 'ADMIN';
  const tabs = [
    { to: 'moderation', label: 'Moderation' },
    { to: 'audit', label: 'Audit log' },
    ...(isAdmin
      ? [
          { to: 'drives', label: 'Drives' },
          { to: 'companies', label: 'Companies' },
          { to: 'users', label: 'Users' },
          { to: 'scraper', label: 'Scraper' },
        ]
      : []),
  ];
  return (
    <div className="space-y-4">
      <h1 className="page-title">{isAdmin ? 'Admin' : 'Moderation'}</h1>
      <nav className="flex gap-1 overflow-x-auto">
        {tabs.map((t) => (
          <NavLink key={t.to} to={t.to}
            className={({ isActive }) => `rounded-xl px-3 py-1.5 text-sm ${isActive ? 'bg-brand-600 text-white' : 'bg-slate-100 dark:bg-slate-800'}`}>
            {t.label}
          </NavLink>
        ))}
      </nav>
      <Routes>
        <Route index element={<Navigate to="moderation" replace />} />
        <Route path="moderation" element={<ModerationQueue />} />
        <Route path="audit" element={<AuditLog />} />
        {isAdmin && (
          <>
            <Route path="drives" element={<AdminDrives />} />
            <Route path="companies" element={<AdminCompanies />} />
            <Route path="users" element={<AdminUsers />} />
            <Route path="scraper" element={<AdminScraper />} />
          </>
        )}
      </Routes>
    </div>
  );
}
