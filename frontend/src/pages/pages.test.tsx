import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { mockFetch, renderWithProviders } from '../test/utils';
import { RoadmapChecklist } from './PrepPage';
import { DrivesPage } from './DrivesPage';
import { validateExperience } from './NewExperiencePage';
import { RequireAuth } from '../components/RequireAuth';
import type { ExperienceInput, RoadmapSection } from '../api/types';

afterEach(() => vi.unstubAllGlobals());

const page = <T,>(content: T[]) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 });

describe('RoadmapChecklist', () => {
  const sections: RoadmapSection[] = [
    { code: 'DSA', name: 'DSA', done: 1, total: 2, items: [
      { id: 1, position: 1, title: 'Big-O', done: true },
      { id: 2, position: 2, title: 'Arrays', done: false },
    ] },
  ];

  it('shows progress and reports ticks', async () => {
    const onToggle = vi.fn();
    renderWithProviders(<RoadmapChecklist sections={sections} onToggle={onToggle} />);
    expect(screen.getByText('1/2')).toBeInTheDocument();
    expect(screen.getByLabelText(/Big-O/)).toBeChecked();
    await userEvent.click(screen.getByLabelText(/Arrays/));
    expect(onToggle).toHaveBeenCalledWith(2, true);
  });
});

describe('DrivesPage', () => {
  it('lists drives with eligibility and sends filters to the API', async () => {
    const fetchMock = mockFetch({
      '/api/branches': [{ code: 'CSE', name: 'Computer Science & Engineering' }],
      '/api/drives': page([
        { id: 1, title: 'Acme OA circular', noticeDate: '2026-11-20', source: 'SCRAPED', company: { id: 9, slug: 'acme', name: 'Acme' },
          stageTypes: ['OA'], hasCriteria: true, eligibility: { status: 'ELIGIBLE', reasons: [] } },
        { id: 2, title: 'Mystery drive', noticeDate: '2026-11-19', source: 'SCRAPED', companyGuess: 'Mystery Co', stageTypes: [], hasCriteria: false },
      ]),
    });
    renderWithProviders(<DrivesPage />, { route: '/drives' });

    expect(await screen.findByText('Acme')).toBeInTheDocument();
    expect(screen.getByText('Mystery Co')).toBeInTheDocument();
    expect(within(screen.getByText('Acme').closest('a')!).getByText('Eligible')).toBeInTheDocument();

    await userEvent.selectOptions(await screen.findByLabelText('Stage'), 'OA');
    await waitFor(() => expect(fetchMock.mock.calls.some(([u]) => String(u).includes('stage=OA'))).toBe(true));
  });
});

describe('validateExperience', () => {
  const valid: ExperienceInput = {
    companyId: 1, year: 2026, roleTitle: 'SDE', difficulty: 3, outcome: 'SELECTED',
    bodyMd: 'x'.repeat(60), rounds: [], questions: [], anonymous: false, acceptGuidelines: true,
  };

  it('accepts a complete post', () => {
    expect(validateExperience(valid)).toEqual({});
  });

  it('requires guidelines, a long-enough body and tagged questions', () => {
    const errors = validateExperience({
      ...valid, bodyMd: 'too short', acceptGuidelines: false,
      questions: [{ bodyMd: 'short', difficulty: 'EASY', topicCodes: [] }],
    });
    expect(Object.keys(errors).sort()).toEqual(['acceptGuidelines', 'bodyMd', 'questions[0].bodyMd', 'questions[0].topicCodes']);
  });
});

describe('RequireAuth', () => {
  it('asks signed-out users to sign in', async () => {
    mockFetch({ '/api/me': () => ({ status: 401, body: { code: 'UNAUTHENTICATED' } }) });
    renderWithProviders(<Routes><Route path="/" element={<RequireAuth><p>secret</p></RequireAuth>} /></Routes>);
    expect(await screen.findByText('Sign in to continue')).toBeInTheDocument();
    expect(screen.queryByText('secret')).not.toBeInTheDocument();
  });

  it('blocks students from moderator pages', async () => {
    mockFetch({ '/api/me': { id: 1, email: 's@x', displayName: 'S', role: 'STUDENT', leaderboardOptIn: false, profileComplete: true } });
    renderWithProviders(<RequireAuth role="MODERATOR"><p>queue</p></RequireAuth>);
    expect(await screen.findByText(/don.t have access/)).toBeInTheDocument();
  });
});
