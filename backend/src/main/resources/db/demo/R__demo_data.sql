-- =====================================================================================
-- DEMO DATA: loaded ONLY with the "dev" profile (application-dev.yml adds db/demo to
-- Flyway's locations). Every name starts with "Demo" and every post with [DEMO DATA].
-- Nothing here is real. A repeatable migration (R__) re-runs when this file changes, so the
-- whole script is guarded to run only once per database.
-- =====================================================================================
DO $$
DECLARE
    admin_id bigint; mod_id bigint; s1 bigint; s2 bigint; s3 bigint; s4 bigint; s5 bigint;
    exp1 bigint; exp2 bigint; d1 bigint; d2 bigint; d3 bigint;
    t_arrays bigint; t_graphs bigint; t_dp bigint; t_dbms bigint; t_os bigint; t_hr bigint;
BEGIN
    IF EXISTS (SELECT 1 FROM app_user WHERE email = 'demo-admin@demo.campusoa.local') THEN
        RETURN;
    END IF;

    INSERT INTO app_user (email, google_sub, display_name, role, branch_code, grad_year, leaderboard_opt_in)
    VALUES ('demo-admin@demo.campusoa.local', 'demo-sub-admin', 'Demo Admin', 'ADMIN', 'CSE', 2027, false)
    RETURNING id INTO admin_id;
    INSERT INTO app_user (email, google_sub, display_name, role, branch_code, grad_year, leaderboard_opt_in)
    VALUES ('demo-mod@demo.campusoa.local', 'demo-sub-mod', 'Demo Moderator', 'MODERATOR', 'IT', 2027, true)
    RETURNING id INTO mod_id;
    INSERT INTO app_user (email, google_sub, display_name, branch_code, grad_year, leaderboard_opt_in)
    VALUES ('demo-s1@demo.campusoa.local', 'demo-sub-s1', 'Demo Student Aarav', 'CSE', 2027, true) RETURNING id INTO s1;
    INSERT INTO app_user (email, google_sub, display_name, branch_code, grad_year, leaderboard_opt_in)
    VALUES ('demo-s2@demo.campusoa.local', 'demo-sub-s2', 'Demo Student Bhavya', 'CSE', 2027, true) RETURNING id INTO s2;
    INSERT INTO app_user (email, google_sub, display_name, branch_code, grad_year, leaderboard_opt_in)
    VALUES ('demo-s3@demo.campusoa.local', 'demo-sub-s3', 'Demo Student Chaitra', 'ECE', 2027, true) RETURNING id INTO s3;
    INSERT INTO app_user (email, google_sub, display_name, branch_code, grad_year, leaderboard_opt_in)
    VALUES ('demo-s4@demo.campusoa.local', 'demo-sub-s4', 'Demo Student Dev', 'AIDS', 2028, true) RETURNING id INTO s4;
    INSERT INTO app_user (email, google_sub, display_name, branch_code, grad_year, leaderboard_opt_in)
    VALUES ('demo-s5@demo.campusoa.local', 'demo-sub-s5', 'Demo Student Esha', 'CSE', 2028, false) RETURNING id INTO s5;

    INSERT INTO private_profile (user_id, cgpa, active_backlogs) VALUES
        (s1, 8.70, 0), (s2, 7.10, 0), (s3, 9.10, 1), (s4, 6.40, 0);

    SELECT id INTO t_arrays FROM topic WHERE code = 'dsa-arrays';
    SELECT id INTO t_graphs FROM topic WHERE code = 'dsa-graphs';
    SELECT id INTO t_dp FROM topic WHERE code = 'dsa-dp';
    SELECT id INTO t_dbms FROM topic WHERE code = 'dbms';
    SELECT id INTO t_os FROM topic WHERE code = 'os';
    SELECT id INTO t_hr FROM topic WHERE code = 'hr';

    -- Demo drives with criteria (manual, no PDF)
    INSERT INTO drive (company_id, title, notice_date, source, role_title, ctc_text)
    VALUES ((SELECT id FROM company WHERE slug = 'jpmorgan-chase'), '[DEMO] JPMorgan Chase Code for Good: Online Assessment',
            current_date + 3, 'MANUAL', 'Software Engineer', '19.8 LPA') RETURNING id INTO d1;
    INSERT INTO drive_stage (drive_id, stage_type, starts_at, mode) VALUES
        (d1, 'PPT', (current_date + 2) + time '10:00' AT TIME ZONE 'Asia/Kolkata', 'ONLINE'),
        (d1, 'OA', (current_date + 3) + time '14:00' AT TIME ZONE 'Asia/Kolkata', 'ONLINE');
    INSERT INTO drive_criteria (drive_id, min_cgpa, max_backlogs, grad_year) VALUES (d1, 7.50, 0, 2027);
    INSERT INTO drive_eligible_branch (drive_id, branch_code) VALUES (d1, 'CSE'), (d1, 'IT'), (d1, 'CSE_AIML');

    INSERT INTO drive (company_id, title, notice_date, source, role_title, ctc_text)
    VALUES ((SELECT id FROM company WHERE slug = 'accenture'), '[DEMO] Accenture Full Drive: all branches',
            current_date + 6, 'MANUAL', 'Associate Software Engineer', '4.5–6.5 LPA') RETURNING id INTO d2;
    INSERT INTO drive_stage (drive_id, stage_type, starts_at, mode) VALUES
        (d2, 'OA', (current_date + 6) + time '09:30' AT TIME ZONE 'Asia/Kolkata', 'OFFLINE'),
        (d2, 'INTERVIEW', (current_date + 8) + time '10:00' AT TIME ZONE 'Asia/Kolkata', 'OFFLINE');
    INSERT INTO drive_criteria (drive_id, min_cgpa, max_backlogs) VALUES (d2, 6.00, 1);

    INSERT INTO drive (company_id, company_guess, title, notice_date, source)
    VALUES (NULL, 'Demo Orbit Labs', '[DEMO] Demo Orbit Labs In-Person Interviews', current_date - 2, 'MANUAL')
    RETURNING id INTO d3;
    INSERT INTO drive_stage (drive_id, stage_type) VALUES (d3, 'INTERVIEW');

    -- Demo experiences
    INSERT INTO interview_experience (author_id, company_id, interview_year, role_title, difficulty, outcome, body_md, anonymous)
    VALUES (s1, (SELECT id FROM company WHERE slug = 'microsoft'), 2026, 'SDE Intern', 4, 'SELECTED',
            E'[DEMO DATA] This is a sample post to show the layout.\n\n## Preparation\nI practised about 250 problems, focusing on **graphs** and **dynamic programming**, and revised OOP and DBMS basics.\n\n## Tips\n- Explain your approach before coding.\n- Talk about edge cases and complexity.',
            false) RETURNING id INTO exp1;
    INSERT INTO experience_round (experience_id, position, name, description) VALUES
        (exp1, 1, 'Online assessment', '3 coding problems in 90 minutes (arrays, graphs, DP).'),
        (exp1, 2, 'Technical interview 1', 'One medium graph problem and questions on my project.'),
        (exp1, 3, 'Technical interview 2', 'DP problem, then OOP design discussion.');
    INSERT INTO question (experience_id, company_id, author_id, body_md, difficulty) VALUES
        (exp1, (SELECT id FROM company WHERE slug = 'microsoft'), s1,
         '[DEMO DATA] Given a grid with obstacles, count the number of shortest paths from the top-left to the bottom-right corner.', 'MEDIUM');
    INSERT INTO question_topic (question_id, topic_id) VALUES (currval(pg_get_serial_sequence('question', 'id')), t_graphs);
    INSERT INTO question (experience_id, company_id, author_id, body_md, difficulty) VALUES
        (exp1, (SELECT id FROM company WHERE slug = 'microsoft'), s1,
         '[DEMO DATA] Find the longest subsequence of an array where each element is a multiple of the previous one.', 'HARD');
    INSERT INTO question_topic (question_id, topic_id) VALUES (currval(pg_get_serial_sequence('question', 'id')), t_dp);

    INSERT INTO interview_experience (author_id, company_id, interview_year, role_title, difficulty, outcome, body_md, anonymous)
    VALUES (s3, (SELECT id FROM company WHERE slug = 'oracle'), 2025, 'Server Technology Member', 3, 'REJECTED',
            E'[DEMO DATA] Sample anonymous post. The aptitude section was time-pressured; the interview focused on SQL joins, normalisation and OS scheduling. Practise writing queries by hand.',
            true) RETURNING id INTO exp2;
    INSERT INTO experience_round (experience_id, position, name, description) VALUES
        (exp2, 1, 'Aptitude + coding', 'Quant, logical reasoning and two easy coding questions.'),
        (exp2, 2, 'Technical interview', 'DBMS and OS fundamentals.');
    INSERT INTO question (experience_id, company_id, author_id, body_md, difficulty) VALUES
        (exp2, (SELECT id FROM company WHERE slug = 'oracle'), s3,
         '[DEMO DATA] Explain the difference between 3NF and BCNF with an example of a table that is in 3NF but not BCNF.', 'MEDIUM');
    INSERT INTO question_topic (question_id, topic_id) VALUES (currval(pg_get_serial_sequence('question', 'id')), t_dbms);
    INSERT INTO question (experience_id, company_id, author_id, body_md, difficulty) VALUES
        (exp2, (SELECT id FROM company WHERE slug = 'oracle'), s3,
         '[DEMO DATA] What is a deadlock? List the four Coffman conditions and one way to prevent each.', 'EASY');
    INSERT INTO question_topic (question_id, topic_id) VALUES (currval(pg_get_serial_sequence('question', 'id')), t_os);

    INSERT INTO question (company_id, author_id, body_md, difficulty) VALUES
        ((SELECT id FROM company WHERE slug = 'accenture'), s2, '[DEMO DATA] Tell me about a time you disagreed with a teammate and how you resolved it.', 'EASY');
    INSERT INTO question_topic (question_id, topic_id) VALUES (currval(pg_get_serial_sequence('question', 'id')), t_hr);

    INSERT INTO experience_vote (experience_id, user_id) VALUES (exp1, s2), (exp1, s3), (exp1, mod_id), (exp2, s1);
    INSERT INTO comment (experience_id, author_id, body) VALUES
        (exp1, s2, '[DEMO DATA] Thanks! How long did you spend on graphs?'),
        (exp1, s1, '[DEMO DATA] Around three weeks, mostly BFS/DFS and shortest paths.');

    -- Demo problem log (dates relative to today, so streaks and the weekly leaderboard have content)
    INSERT INTO problem_log (user_id, title, url, topic_id, difficulty, solved_on)
    SELECT u.uid, 'Demo problem ' || g, 'https://example.com/demo/' || u.uid || '/' || g,
           (ARRAY[t_arrays, t_graphs, t_dp])[1 + g % 3], (ARRAY['EASY', 'MEDIUM', 'HARD'])[1 + g % 3], current_date - (g % u.spread)
    FROM (VALUES (s1, 14, 7), (s2, 9, 5), (s3, 9, 4), (s4, 5, 3), (mod_id, 3, 3), (s5, 6, 6)) AS u(uid, n, spread)
    CROSS JOIN LATERAL generate_series(1, u.n) AS g;

    INSERT INTO roadmap_progress (user_id, item_id)
    SELECT s1, id FROM roadmap_item WHERE section_code IN ('DSA', 'OOP');
END $$;
