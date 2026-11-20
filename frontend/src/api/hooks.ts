import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, ApiError, qs } from './client';
import type {
  AdminUser,
  Branch,
  CalendarEvent,
  Comment,
  CompanyDetail,
  CompanySummary,
  Dashboard,
  DriveDetail,
  DriveSummary,
  ExperienceDetail,
  ExperienceSummary,
  Leaderboard,
  Me,
  ModAction,
  Page,
  Period,
  Problem,
  Question,
  QueueItem,
  RoadmapSection,
  Scope,
  ScrapeRun,
  Topic,
} from './types';

/**
 * TanStack Query hooks: each one names its data with a "query key" (e.g. ['drives', filters]).
 * The library caches results per key, de-duplicates simultaneous requests, refetches stale data
 * in the background, and lets mutations invalidate keys so the UI refreshes itself.
 */

// ---------- auth ----------

/** The signed-in user, or null when signed out (the backend answers 401). */
export function useMe() {
  return useQuery<Me | null>({
    queryKey: ['me'],
    queryFn: async () => {
      try {
        return await api.get<Me>('/api/me');
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) return null;
        throw e;
      }
    },
    staleTime: 5 * 60_000,
  });
}

// ---------- lookups (rarely change: cache for an hour) ----------

export const useBranches = () =>
  useQuery({ queryKey: ['branches'], queryFn: () => api.get<Branch[]>('/api/branches'), staleTime: 3_600_000 });

export const useTopics = () =>
  useQuery({ queryKey: ['topics'], queryFn: () => api.get<Topic[]>('/api/topics'), staleTime: 3_600_000 });

// ---------- drives ----------

export interface DriveFilters {
  company?: string;
  branch?: string;
  stage?: string;
  q?: string;
  from?: string;
  to?: string;
  page?: number;
}

export const useDrives = (f: DriveFilters) =>
  useQuery({
    queryKey: ['drives', f],
    queryFn: () => api.get<Page<DriveSummary>>('/api/drives' + qs({ ...f, size: 20 })),
    placeholderData: keepPreviousData, // keep showing the old page while the next one loads
  });

export const useDrive = (id: number) =>
  useQuery({ queryKey: ['drive', id], queryFn: () => api.get<DriveDetail>(`/api/drives/${id}`) });

export const useCalendar = (from: string, to: string) =>
  useQuery({
    queryKey: ['calendar', from, to],
    queryFn: () => api.get<CalendarEvent[]>('/api/drives/events' + qs({ from, to })),
  });

// ---------- companies ----------

export const useCompanies = (q: string, sort: 'ctc' | 'name', page: number, size = 20) =>
  useQuery({
    queryKey: ['companies', q, sort, page, size],
    queryFn: () => api.get<Page<CompanySummary>>('/api/companies' + qs({ q, sort, page, size })),
    placeholderData: keepPreviousData,
  });

export const useCompany = (slug: string) =>
  useQuery({ queryKey: ['company', slug], queryFn: () => api.get<CompanyDetail>(`/api/companies/${slug}`) });

// ---------- experiences & questions ----------

export interface ExperienceFilters {
  q?: string;
  company?: string;
  year?: number;
  outcome?: string;
  sort?: string;
  page?: number;
}

export const useExperiences = (f: ExperienceFilters) =>
  useQuery({
    queryKey: ['experiences', f],
    queryFn: () => api.get<Page<ExperienceSummary>>('/api/experiences' + qs({ ...f, size: 20 })),
    placeholderData: keepPreviousData,
  });

export const useExperience = (id: number) =>
  useQuery({ queryKey: ['experience', id], queryFn: () => api.get<ExperienceDetail>(`/api/experiences/${id}`) });

export const useComments = (id: number) =>
  useQuery({
    queryKey: ['comments', id],
    queryFn: () => api.get<Page<Comment>>(`/api/experiences/${id}/comments?size=50`),
  });

export interface QuestionFilters {
  q?: string;
  topic?: string;
  difficulty?: string;
  company?: string;
  sort?: string;
  page?: number;
}

export const useQuestions = (f: QuestionFilters) =>
  useQuery({
    queryKey: ['questions', f],
    queryFn: () => api.get<Page<Question>>('/api/questions' + qs({ ...f, size: 20 })),
    placeholderData: keepPreviousData,
  });

// ---------- prep ----------

export const useRoadmap = () =>
  useQuery({ queryKey: ['roadmap'], queryFn: () => api.get<RoadmapSection[]>('/api/prep/roadmap') });

export const useProblems = (page: number) =>
  useQuery({
    queryKey: ['problems', page],
    queryFn: () => api.get<Page<Problem>>(`/api/prep/problems?page=${page}&size=20`),
    placeholderData: keepPreviousData,
  });

export const useDashboard = () =>
  useQuery({ queryKey: ['dashboard'], queryFn: () => api.get<Dashboard>('/api/prep/dashboard') });

export const useLeaderboard = (period: Period, scope: Scope, page: number) =>
  useQuery({
    queryKey: ['leaderboard', period, scope, page],
    queryFn: () => api.get<Leaderboard>('/api/leaderboard' + qs({ period, scope, page, size: 20 })),
    retry: false,
  });

/** Optimistic tick: the checkbox flips instantly; if the server fails we roll back. */
export function useTickRoadmap() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, done }: { itemId: number; done: boolean }) =>
      api.put(`/api/prep/roadmap/${itemId}`, { done }),
    onMutate: async ({ itemId, done }) => {
      await qc.cancelQueries({ queryKey: ['roadmap'] });
      const previous = qc.getQueryData<RoadmapSection[]>(['roadmap']);
      qc.setQueryData<RoadmapSection[]>(['roadmap'], (old) =>
        old?.map((s) => {
          const items = s.items.map((i) => (i.id === itemId ? { ...i, done } : i));
          return { ...s, items, done: items.filter((i) => i.done).length };
        }),
      );
      return { previous };
    },
    onError: (_e, _v, ctx) => qc.setQueryData(['roadmap'], ctx?.previous),
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['roadmap'] });
      qc.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
}

// ---------- moderation / admin ----------

export const useModQueue = (status: string, page: number) =>
  useQuery({
    queryKey: ['mod-queue', status, page],
    queryFn: () => api.get<Page<QueueItem>>('/api/mod/reports' + qs({ status, page, size: 20 })),
  });

export const useModActions = (page: number) =>
  useQuery({
    queryKey: ['mod-actions', page],
    queryFn: () => api.get<Page<ModAction>>('/api/mod/actions' + qs({ page, size: 20 })),
  });

export const useAdminUsers = (q: string, page: number) =>
  useQuery({
    queryKey: ['admin-users', q, page],
    queryFn: () => api.get<Page<AdminUser>>('/api/admin/users' + qs({ q, page, size: 20 })),
    placeholderData: keepPreviousData,
  });

export const useScrapeRuns = () =>
  useQuery({ queryKey: ['scrape-runs'], queryFn: () => api.get<Page<ScrapeRun>>('/api/admin/scraper/runs?size=20') });
