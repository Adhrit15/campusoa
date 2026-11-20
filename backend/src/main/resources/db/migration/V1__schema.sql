-- =====================================================================================
-- CampusOA core schema (3NF). Flyway runs this once, in order, and records it in
-- flyway_schema_history. NEVER edit a migration after it has run anywhere: add V5, V6 ...
--
-- Conventions
--   * bigint identity primary keys (no meaning, never reused)
--   * timestamptz for instants (stored in UTC), date for calendar days
--   * small fixed vocabularies use varchar + CHECK (readable in psql, easy to extend);
--     vocabularies admins may extend (branches, topics) are lookup tables
--   * every FK states what happens on delete
-- =====================================================================================

-- ---------- Users ----------------------------------------------------------------------

CREATE TABLE branch (
    code        varchar(16)  PRIMARY KEY,           -- 'CSE', 'AIDS', ...
    name        varchar(64)  NOT NULL UNIQUE,       -- 'AI & Data Science'
    sort_order  smallint     NOT NULL
);

CREATE TABLE app_user (
    id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email               varchar(254) NOT NULL UNIQUE CHECK (email = lower(email)),
    google_sub          varchar(64)  NOT NULL UNIQUE,   -- Google's stable account id
    display_name        varchar(100) NOT NULL,
    avatar_url          varchar(500),
    role                varchar(16)  NOT NULL DEFAULT 'STUDENT'
                        CHECK (role IN ('STUDENT', 'MODERATOR', 'ADMIN')),
    branch_code         varchar(16)  REFERENCES branch (code),
    grad_year           smallint     CHECK (grad_year BETWEEN 2020 AND 2040),
    leaderboard_opt_in  boolean      NOT NULL DEFAULT false,   -- privacy: off by default
    banned              boolean      NOT NULL DEFAULT false,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    last_login_at       timestamptz
);

-- Private academic data lives in its own table. A query must JOIN it deliberately, so
-- "SELECT * FROM app_user" for a public author list can never leak CGPA.
CREATE TABLE private_profile (
    user_id          bigint       PRIMARY KEY REFERENCES app_user (id) ON DELETE CASCADE,
    cgpa             numeric(4,2) CHECK (cgpa BETWEEN 0 AND 10),
    active_backlogs  smallint     CHECK (active_backlogs >= 0),
    updated_at       timestamptz  NOT NULL DEFAULT now()
);

-- ---------- Companies ------------------------------------------------------------------

CREATE TABLE company (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug           varchar(80)  NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    name           varchar(120) NOT NULL UNIQUE,
    website        varchar(300),
    prep_markdown  text         NOT NULL DEFAULT '',
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now()
);

-- Other names a company appears under in circular titles ('JPMC', 'LTM' ...).
-- Used by the scraper's best-effort company matching.
CREATE TABLE company_alias (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id  bigint       NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    alias       varchar(120) NOT NULL
);
CREATE UNIQUE INDEX ux_company_alias_lower ON company_alias (lower(alias));
CREATE INDEX ix_company_alias_company ON company_alias (company_id);

-- Full-time offers aggregated per company, academic year and role.
-- "up to 9.5 LPA" is stored as ctc_min_lpa NULL, ctc_max_lpa 9.5; "51 LPA" as 51/51.
CREATE TABLE placement_stat (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id     bigint       NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    academic_year  char(7)      NOT NULL CHECK (academic_year ~ '^[0-9]{4}-[0-9]{2}$'),
    role_title     varchar(120) NOT NULL DEFAULT 'Full-time',
    ctc_min_lpa    numeric(6,2) CHECK (ctc_min_lpa > 0),
    ctc_max_lpa    numeric(6,2) NOT NULL CHECK (ctc_max_lpa > 0),
    offers         integer      CHECK (offers >= 0),
    source_url     varchar(300),
    source_note    varchar(200),
    CONSTRAINT ck_placement_ctc_range CHECK (ctc_min_lpa IS NULL OR ctc_min_lpa <= ctc_max_lpa),
    CONSTRAINT uq_placement_stat UNIQUE (company_id, academic_year, role_title)
);

CREATE TABLE internship_stat (
    id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id         bigint       NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    academic_year      char(7)      NOT NULL CHECK (academic_year ~ '^[0-9]{4}-[0-9]{2}$'),
    stipend_inr_month  integer      NOT NULL CHECK (stipend_inr_month > 0),
    source_url         varchar(300),
    source_note        varchar(200),
    CONSTRAINT uq_internship_stat UNIQUE (company_id, academic_year)
);

-- The company's usual selection process, in order.
CREATE TABLE selection_stage (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id   bigint       NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    position     smallint     NOT NULL CHECK (position >= 1),
    stage_type   varchar(16)  NOT NULL CHECK (stage_type IN ('PPT', 'OA', 'GD', 'INTERVIEW', 'HR', 'OTHER')),
    description  varchar(300),
    CONSTRAINT uq_selection_stage UNIQUE (company_id, position)
);

-- ---------- Drives ---------------------------------------------------------------------

CREATE TABLE drive (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id      bigint       REFERENCES company (id) ON DELETE SET NULL,  -- NULL = not matched yet
    company_guess   varchar(120),             -- scraper's best guess when no company matched
    title           varchar(300) NOT NULL,
    notice_date     date         NOT NULL,
    pdf_url         varchar(500) UNIQUE,      -- scraper's dedupe key; NULL for manual drives
    source          varchar(8)   NOT NULL CHECK (source IN ('SCRAPED', 'MANUAL')),
    role_title      varchar(200),
    ctc_text        varchar(100),
    merged_into_id  bigint       REFERENCES drive (id) ON DELETE SET NULL,
    hidden          boolean      NOT NULL DEFAULT false,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_drive_not_self_merged CHECK (merged_into_id IS NULL OR merged_into_id <> id)
);
-- Drive list / calendar: newest visible drives first. Partial index = only rows the list shows.
CREATE INDEX ix_drive_visible_notice_date ON drive (notice_date DESC)
    WHERE merged_into_id IS NULL AND NOT hidden;
CREATE INDEX ix_drive_company ON drive (company_id);

CREATE TABLE drive_stage (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    drive_id    bigint       NOT NULL REFERENCES drive (id) ON DELETE CASCADE,
    stage_type  varchar(16)  NOT NULL CHECK (stage_type IN ('PPT', 'OA', 'GD', 'INTERVIEW', 'HR', 'OTHER')),
    starts_at   timestamptz,
    mode        varchar(8)   CHECK (mode IN ('ONLINE', 'OFFLINE', 'HYBRID')),
    notes       varchar(500)
);
CREATE INDEX ix_drive_stage_drive ON drive_stage (drive_id);
CREATE INDEX ix_drive_stage_starts_at ON drive_stage (starts_at);   -- calendar month range

-- One row per drive whose criteria an admin has entered. No row => "criteria unknown".
CREATE TABLE drive_criteria (
    drive_id      bigint       PRIMARY KEY REFERENCES drive (id) ON DELETE CASCADE,
    min_cgpa      numeric(4,2) CHECK (min_cgpa BETWEEN 0 AND 10),
    max_backlogs  smallint     CHECK (max_backlogs >= 0),
    grad_year     smallint     CHECK (grad_year BETWEEN 2020 AND 2040),
    notes         varchar(500),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);

-- Junction table (1NF: no comma-separated "CSE,IT,ECE" column).
-- No rows for a drive that HAS criteria => open to every branch.
CREATE TABLE drive_eligible_branch (
    drive_id     bigint      NOT NULL REFERENCES drive_criteria (drive_id) ON DELETE CASCADE,
    branch_code  varchar(16) NOT NULL REFERENCES branch (code),
    PRIMARY KEY (drive_id, branch_code)
);
CREATE INDEX ix_drive_eligible_branch_branch ON drive_eligible_branch (branch_code);

-- ---------- Community content ------------------------------------------------------------

CREATE TABLE topic (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        varchar(40)  NOT NULL UNIQUE,     -- 'dsa-graphs'
    name        varchar(80)  NOT NULL,
    parent_id   bigint       REFERENCES topic (id),
    sort_order  smallint     NOT NULL DEFAULT 0
);
CREATE INDEX ix_topic_parent ON topic (parent_id);

CREATE TABLE interview_experience (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    author_id        bigint       REFERENCES app_user (id) ON DELETE SET NULL,  -- deleted user keeps post
    company_id       bigint       NOT NULL REFERENCES company (id),
    interview_year   smallint     NOT NULL CHECK (interview_year BETWEEN 2015 AND 2040),
    role_title       varchar(120) NOT NULL,
    difficulty       smallint     NOT NULL CHECK (difficulty BETWEEN 1 AND 5),
    outcome          varchar(10)  NOT NULL CHECK (outcome IN ('SELECTED', 'REJECTED', 'WAITING', 'WITHDREW')),
    body_md          text         NOT NULL CHECK (length(body_md) <= 20000),
    anonymous        boolean      NOT NULL DEFAULT false,
    status           varchar(10)  NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('PUBLISHED', 'HIDDEN', 'REMOVED')),
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    -- Full-text search document, kept up to date by Postgres itself. Role title matches
    -- weigh more ('A') than body matches ('B') when ranking.
    search_vector    tsvector GENERATED ALWAYS AS (
                         setweight(to_tsvector('english', coalesce(role_title, '')), 'A') ||
                         setweight(to_tsvector('english', coalesce(body_md, '')), 'B')
                     ) STORED
);
CREATE INDEX ix_experience_search ON interview_experience USING gin (search_vector);
CREATE INDEX ix_experience_company_published ON interview_experience (company_id, created_at DESC)
    WHERE status = 'PUBLISHED';
CREATE INDEX ix_experience_published_created ON interview_experience (created_at DESC)
    WHERE status = 'PUBLISHED';
CREATE INDEX ix_experience_author ON interview_experience (author_id);

CREATE TABLE experience_round (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    experience_id  bigint       NOT NULL REFERENCES interview_experience (id) ON DELETE CASCADE,
    position       smallint     NOT NULL CHECK (position >= 1),
    name           varchar(100) NOT NULL,
    description    text         CHECK (length(description) <= 5000),
    CONSTRAINT uq_experience_round UNIQUE (experience_id, position)
);

CREATE TABLE question (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    experience_id  bigint       REFERENCES interview_experience (id) ON DELETE SET NULL,
    company_id     bigint       REFERENCES company (id) ON DELETE SET NULL,
    author_id      bigint       REFERENCES app_user (id) ON DELETE SET NULL,
    body_md        text         NOT NULL CHECK (length(body_md) BETWEEN 10 AND 5000),
    difficulty     varchar(6)   NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    status         varchar(10)  NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('PUBLISHED', 'HIDDEN', 'REMOVED')),
    created_at     timestamptz  NOT NULL DEFAULT now(),
    search_vector  tsvector GENERATED ALWAYS AS (to_tsvector('english', body_md)) STORED
);
CREATE INDEX ix_question_search ON question USING gin (search_vector);
CREATE INDEX ix_question_company ON question (company_id);
CREATE INDEX ix_question_experience ON question (experience_id);
CREATE INDEX ix_question_published_created ON question (created_at DESC) WHERE status = 'PUBLISHED';

CREATE TABLE question_topic (
    question_id  bigint NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    topic_id     bigint NOT NULL REFERENCES topic (id),
    PRIMARY KEY (question_id, topic_id)
);
CREATE INDEX ix_question_topic_topic ON question_topic (topic_id);  -- "questions tagged graphs"

-- One vote per user per post is enforced by the composite primary key.
CREATE TABLE experience_vote (
    experience_id  bigint      NOT NULL REFERENCES interview_experience (id) ON DELETE CASCADE,
    user_id        bigint      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    created_at     timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (experience_id, user_id)
);
CREATE INDEX ix_experience_vote_user ON experience_vote (user_id);

CREATE TABLE question_vote (
    question_id  bigint      NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    user_id      bigint      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    created_at   timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (question_id, user_id)
);
CREATE INDEX ix_question_vote_user ON question_vote (user_id);

CREATE TABLE comment (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    experience_id  bigint        NOT NULL REFERENCES interview_experience (id) ON DELETE CASCADE,
    author_id      bigint        REFERENCES app_user (id) ON DELETE SET NULL,
    body           varchar(2000) NOT NULL,
    status         varchar(10)   NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('PUBLISHED', 'HIDDEN', 'REMOVED')),
    created_at     timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX ix_comment_experience ON comment (experience_id, created_at);

-- ---------- Moderation -----------------------------------------------------------------

-- A report targets exactly ONE of: an experience, a question or a comment. Three nullable
-- FKs + a CHECK keep real foreign keys (a single "target_id" column could not have one).
CREATE TABLE report (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reporter_id      bigint       REFERENCES app_user (id) ON DELETE SET NULL,
    experience_id    bigint       REFERENCES interview_experience (id) ON DELETE CASCADE,
    question_id      bigint       REFERENCES question (id) ON DELETE CASCADE,
    comment_id       bigint       REFERENCES comment (id) ON DELETE CASCADE,
    reason           varchar(16)  NOT NULL
                     CHECK (reason IN ('SPAM', 'CONFIDENTIAL', 'PERSONAL_INFO', 'ABUSIVE', 'OFF_TOPIC', 'OTHER')),
    details          varchar(500),
    status           varchar(10)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED')),
    resolved_by      bigint       REFERENCES app_user (id) ON DELETE SET NULL,
    resolved_at      timestamptz,
    resolution_note  varchar(500),
    created_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_report_one_target CHECK (num_nonnulls(experience_id, question_id, comment_id) = 1),
    -- NULLs are distinct in UNIQUE, so each of these only bites for its own target type.
    CONSTRAINT uq_report_experience UNIQUE (reporter_id, experience_id),
    CONSTRAINT uq_report_question UNIQUE (reporter_id, question_id),
    CONSTRAINT uq_report_comment UNIQUE (reporter_id, comment_id)
);
CREATE INDEX ix_report_open_queue ON report (created_at) WHERE status = 'OPEN';   -- moderation queue
CREATE INDEX ix_report_experience ON report (experience_id) WHERE experience_id IS NOT NULL;
CREATE INDEX ix_report_question ON report (question_id) WHERE question_id IS NOT NULL;
CREATE INDEX ix_report_comment ON report (comment_id) WHERE comment_id IS NOT NULL;

-- Append-only audit log of what moderators did (and the automatic hides).
CREATE TABLE moderation_action (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    moderator_id   bigint       REFERENCES app_user (id) ON DELETE SET NULL,  -- NULL = automatic
    action         varchar(16)  NOT NULL CHECK (action IN ('AUTO_HIDE', 'HIDE', 'REMOVE', 'RESTORE', 'DISMISS')),
    experience_id  bigint       REFERENCES interview_experience (id) ON DELETE SET NULL,
    question_id    bigint       REFERENCES question (id) ON DELETE SET NULL,
    comment_id     bigint       REFERENCES comment (id) ON DELETE SET NULL,
    note           varchar(500),
    created_at     timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX ix_moderation_action_created ON moderation_action (created_at DESC);

-- ---------- Prep tracker ---------------------------------------------------------------

CREATE TABLE roadmap_section (
    code        varchar(24) PRIMARY KEY,
    name        varchar(60) NOT NULL,
    sort_order  smallint    NOT NULL UNIQUE
);

CREATE TABLE roadmap_item (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    section_code  varchar(24)  NOT NULL REFERENCES roadmap_section (code),
    position      smallint     NOT NULL,
    title         varchar(150) NOT NULL,
    description   varchar(500),
    resource_url  varchar(300),
    CONSTRAINT uq_roadmap_item UNIQUE (section_code, position)
);

CREATE TABLE roadmap_progress (
    user_id       bigint      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    item_id       bigint      NOT NULL REFERENCES roadmap_item (id) ON DELETE CASCADE,
    completed_at  timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, item_id)
);

CREATE TABLE problem_log (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     bigint       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    title       varchar(200) NOT NULL,
    url         varchar(500) NOT NULL,
    topic_id    bigint       NOT NULL REFERENCES topic (id),
    difficulty  varchar(6)   NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    solved_on   date         NOT NULL CHECK (solved_on >= DATE '2015-01-01'),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_problem_log_user_url UNIQUE (user_id, url)   -- no padding the leaderboard
);
-- Leaderboard: "everyone's problems solved between :from and :to".
CREATE INDEX ix_problem_log_solved_user ON problem_log (solved_on, user_id);
-- Dashboard / streak: "my problems, newest first".
CREATE INDEX ix_problem_log_user_solved ON problem_log (user_id, solved_on DESC);

-- ---------- Scraper bookkeeping --------------------------------------------------------

CREATE TABLE scrape_run (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    started_at     timestamptz  NOT NULL DEFAULT now(),
    finished_at    timestamptz,
    status         varchar(16)  NOT NULL
                   CHECK (status IN ('RUNNING', 'SUCCESS', 'NOT_MODIFIED', 'SKIPPED_ROBOTS', 'FAILED')),
    http_status    smallint,
    etag           varchar(200),
    last_modified  varchar(100),
    items_seen     integer      NOT NULL DEFAULT 0,
    items_new      integer      NOT NULL DEFAULT 0,
    error          varchar(1000)
);
CREATE INDEX ix_scrape_run_started ON scrape_run (started_at DESC);
