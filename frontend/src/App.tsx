import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router';
import { Layout } from './components/Layout';
import { RequireAuth } from './components/RequireAuth';
import { Spinner } from './components/ui';
import { HomePage } from './pages/HomePage';
import { DrivesPage } from './pages/DrivesPage';
import { DriveDetailPage } from './pages/DriveDetailPage';
import { CompaniesPage } from './pages/CompaniesPage';
import { CompanyPage } from './pages/CompanyPage';
import { ExperiencesPage } from './pages/ExperiencesPage';
import { ExperienceDetailPage } from './pages/ExperienceDetailPage';
import { NewExperiencePage } from './pages/NewExperiencePage';
import { QuestionsPage } from './pages/QuestionsPage';
import { PrepPage } from './pages/PrepPage';
import { LeaderboardPage } from './pages/LeaderboardPage';
import { ProfilePage } from './pages/ProfilePage';
import { LoginPage } from './pages/LoginPage';
import { GuidelinesPage, PrivacyPage } from './pages/StaticPages';
import { NotFoundPage } from './pages/NotFoundPage';

// The admin area is code-split: students never download its JavaScript.
const AdminPage = lazy(() => import('./pages/admin/AdminPage'));

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="drives" element={<DrivesPage />} />
        <Route path="drives/:id" element={<DriveDetailPage />} />
        <Route path="companies" element={<CompaniesPage />} />
        <Route path="companies/:slug" element={<CompanyPage />} />
        <Route path="experiences" element={<ExperiencesPage />} />
        <Route path="experiences/new" element={<RequireAuth><NewExperiencePage /></RequireAuth>} />
        <Route path="experiences/:id" element={<ExperienceDetailPage />} />
        <Route path="questions" element={<QuestionsPage />} />
        <Route path="prep" element={<RequireAuth><PrepPage /></RequireAuth>} />
        <Route path="leaderboard" element={<RequireAuth><LeaderboardPage /></RequireAuth>} />
        <Route path="me" element={<RequireAuth><ProfilePage /></RequireAuth>} />
        <Route path="login" element={<LoginPage />} />
        <Route path="guidelines" element={<GuidelinesPage />} />
        <Route path="privacy" element={<PrivacyPage />} />
        <Route
          path="admin/*"
          element={
            <RequireAuth role="MODERATOR">
              <Suspense fallback={<Spinner />}>
                <AdminPage />
              </Suspense>
            </RequireAuth>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
