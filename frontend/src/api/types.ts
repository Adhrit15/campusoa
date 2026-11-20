// TypeScript mirrors of the backend DTOs (records in */...Dtos.java).
// Keeping them in one file makes it obvious when the API contract changes.

export type Role = 'STUDENT' | 'MODERATOR' | 'ADMIN';
export type StageType = 'PPT' | 'OA' | 'GD' | 'INTERVIEW' | 'HR' | 'OTHER';
export type EligibilityStatus = 'ELIGIBLE' | 'NOT_ELIGIBLE' | 'UNKNOWN';
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type Outcome = 'SELECTED' | 'REJECTED' | 'WAITING' | 'WITHDREW';
export type ContentStatus = 'PUBLISHED' | 'HIDDEN' | 'REMOVED';
export type ContentTarget = 'EXPERIENCE' | 'QUESTION' | 'COMMENT';
export type ReportReason = 'SPAM' | 'CONFIDENTIAL' | 'PERSONAL_INFO' | 'ABUSIVE' | 'OFF_TOPIC' | 'OTHER';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  status: number;
  error: string;
  code: string;
  message: string;
  fieldErrors?: FieldError[];
  path?: string;
}

export interface Me {
  id: number;
  email: string;
  displayName: string;
  avatarUrl?: string;
  role: Role;
  branchCode?: string;
  gradYear?: number;
  leaderboardOptIn: boolean;
  cgpa?: number;
  activeBacklogs?: number;
  profileComplete: boolean;
}

export interface Author {
  id: number;
  displayName: string;
  avatarUrl?: string;
  branchCode?: string;
  gradYear?: number;
}

export interface Branch {
  code: string;
  name: string;
}

export interface Topic {
  code: string;
  name: string;
  parentCode?: string;
}

export interface CompanyRef {
  id: number;
  slug: string;
  name: string;
}

// ----- companies -----
export interface CompanySummary {
  id: number;
  slug: string;
  name: string;
  latestYear?: string;
  latestCtcLpa?: number;
  latestOffers?: number;
  bestCtcLpa?: number;
  experienceCount: number;
}

export interface PlacementStat {
  id: number;
  academicYear: string;
  roleTitle: string;
  ctcMinLpa?: number;
  ctcMaxLpa: number;
  offers?: number;
  ctcLabel: string;
  sourceUrl?: string;
  sourceNote?: string;
}

export interface InternshipStat {
  id: number;
  academicYear: string;
  stipendInrMonth: number;
  sourceUrl?: string;
  sourceNote?: string;
}

export interface SelectionStage {
  position: number;
  stageType: StageType;
  description?: string;
}

export interface CompanyDetail {
  id: number;
  slug: string;
  name: string;
  website?: string;
  prepMarkdown: string;
  aliases: string[];
  placements: PlacementStat[];
  internships: InternshipStat[];
  selectionProcess: SelectionStage[];
  updatedAt: string;
}

// ----- drives -----
export interface Eligibility {
  status: EligibilityStatus;
  reasons: string[];
}

export interface DriveSummary {
  id: number;
  title: string;
  noticeDate: string;
  pdfUrl?: string;
  source: 'SCRAPED' | 'MANUAL';
  company?: CompanyRef;
  companyGuess?: string;
  roleTitle?: string;
  ctcText?: string;
  stageTypes: StageType[];
  hasCriteria: boolean;
  eligibility?: Eligibility;
}

export interface DriveStage {
  id: number;
  stageType: StageType;
  startsAt?: string;
  mode?: 'ONLINE' | 'OFFLINE' | 'HYBRID';
  notes?: string;
}

export interface Criteria {
  minCgpa?: number;
  maxBacklogs?: number;
  gradYear?: number;
  branches: string[];
  notes?: string;
}

export interface DriveDetail extends Omit<DriveSummary, 'stageTypes' | 'hasCriteria'> {
  stages: DriveStage[];
  criteria?: Criteria;
  mergedIntoId?: number;
  hidden: boolean;
}

export interface CalendarEvent {
  driveId: number;
  title: string;
  companyName?: string;
  companySlug?: string;
  kind: 'NOTICE' | StageType;
  date: string;
  startsAt?: string;
  eligibility?: EligibilityStatus;
}

// ----- community -----
export interface ExperienceSummary {
  id: number;
  company: CompanyRef;
  year: number;
  roleTitle: string;
  difficulty: number;
  outcome: Outcome;
  anonymous: boolean;
  author?: Author;
  excerpt: string;
  votes: number;
  comments: number;
  votedByMe: boolean;
  createdAt: string;
}

export interface Question {
  id: number;
  bodyMd: string;
  difficulty: Difficulty;
  topics: Topic[];
  company?: CompanyRef;
  experienceId?: number;
  author?: Author;
  anonymous: boolean;
  votes: number;
  votedByMe: boolean;
  status: ContentStatus;
  createdAt: string;
}

export interface ExperienceDetail {
  id: number;
  company: CompanyRef;
  year: number;
  roleTitle: string;
  difficulty: number;
  outcome: Outcome;
  anonymous: boolean;
  author?: Author;
  bodyMd: string;
  rounds: { position: number; name: string; description?: string }[];
  questions: Question[];
  votes: number;
  votedByMe: boolean;
  mine: boolean;
  status: ContentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface Comment {
  id: number;
  author?: Author;
  body: string;
  mine: boolean;
  createdAt: string;
}

export interface QuestionInput {
  bodyMd: string;
  difficulty: Difficulty;
  topicCodes: string[];
}

export interface ExperienceInput {
  companyId: number;
  year: number;
  roleTitle: string;
  difficulty: number;
  outcome: Outcome;
  bodyMd: string;
  rounds: { name: string; description?: string }[];
  questions: QuestionInput[];
  anonymous: boolean;
  acceptGuidelines: boolean;
}

// ----- prep -----
export interface RoadmapItem {
  id: number;
  position: number;
  title: string;
  description?: string;
  resourceUrl?: string;
  done: boolean;
}

export interface RoadmapSection {
  code: string;
  name: string;
  items: RoadmapItem[];
  done: number;
  total: number;
}

export interface Problem {
  id: number;
  title: string;
  url: string;
  topicCode: string;
  topicName: string;
  difficulty: Difficulty;
  solvedOn: string;
}

export interface Dashboard {
  totalSolved: number;
  solvedThisWeek: number;
  solvedThisMonth: number;
  currentStreak: number;
  longestStreak: number;
  perWeek: { weekStart: string; solved: number }[];
  topicCoverage: { code: string; name: string; solved: number }[];
  easy: number;
  medium: number;
  hard: number;
  roadmapDone: number;
  roadmapTotal: number;
}

export type Period = 'WEEK' | 'MONTH' | 'ALL';
export type Scope = 'OVERALL' | 'COHORT';

export interface Leaderboard {
  period: Period;
  scope: Scope;
  from: string;
  to: string;
  cohortBranch?: string;
  cohortYear?: number;
  entries: { rank: number; denseRank: number; user: Author; solved: number; me: boolean }[];
  totalRanked: number;
  me?: { rank: number; solved: number };
  optedIn: boolean;
}

// ----- moderation / admin -----
export interface QueueItem {
  reportId: number;
  targetType: ContentTarget;
  targetId: number;
  experienceId?: number;
  preview: string;
  targetStatus?: ContentStatus;
  reason: ReportReason;
  details?: string;
  reporterName?: string;
  openReportsForTarget: number;
  createdAt: string;
}

export interface ModAction {
  id: number;
  moderatorName: string;
  action: string;
  targetType?: ContentTarget;
  targetId?: number;
  note?: string;
  createdAt: string;
}

export interface AdminUser {
  id: number;
  email: string;
  displayName: string;
  role: Role;
  branchCode?: string;
  gradYear?: number;
  banned: boolean;
  createdAt: string;
  lastLoginAt?: string;
}

export interface ScrapeRun {
  id: number;
  startedAt: string;
  finishedAt?: string;
  status: 'RUNNING' | 'SUCCESS' | 'NOT_MODIFIED' | 'SKIPPED_ROBOTS' | 'FAILED';
  httpStatus?: number;
  itemsSeen: number;
  itemsNew: number;
  error?: string;
}
