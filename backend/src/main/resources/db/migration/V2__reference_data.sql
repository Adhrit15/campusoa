-- Reference data every environment needs: branches, question/problem topics, prep roadmap.

INSERT INTO branch (code, name, sort_order) VALUES
    ('CSE',      'Computer Science & Engineering',          1),
    ('CSE_AIML', 'CSE (Artificial Intelligence & ML)',      2),
    ('CSE_IOT',  'CSE (Internet of Things)',                3),
    ('IT',       'Information Technology',                  4),
    ('AIDS',     'AI & Data Science',                       5),
    ('AIML',     'AI & Machine Learning',                   6),
    ('ECE',      'Electronics & Communication Engineering', 7),
    ('EEE',      'Electrical & Electronics Engineering',    8),
    ('MECH',     'Mechanical Engineering',                  9),
    ('CIVIL',    'Civil Engineering',                      10),
    ('CHEM',     'Chemical Engineering',                   11),
    ('BIOTECH',  'Biotechnology',                          12);

-- Top-level topics
INSERT INTO topic (code, name, sort_order) VALUES
    ('dsa',           'Data Structures & Algorithms', 1),
    ('oop',           'Object-Oriented Programming',  2),
    ('dbms',          'DBMS / SQL',                   3),
    ('os',            'Operating Systems',            4),
    ('cn',            'Computer Networks',            5),
    ('aptitude',      'Aptitude & Reasoning',         6),
    ('hr',            'HR / Behavioural',             7),
    ('system-design', 'System Design',                8);

-- DSA sub-topics
INSERT INTO topic (code, name, parent_id, sort_order)
SELECT v.code, v.name, (SELECT id FROM topic WHERE code = 'dsa'), v.ord
FROM (VALUES
    ('dsa-arrays',       'Arrays',                 1),
    ('dsa-strings',      'Strings',                2),
    ('dsa-linked-list',  'Linked Lists',           3),
    ('dsa-stack-queue',  'Stacks & Queues',        4),
    ('dsa-hashing',      'Hashing',                5),
    ('dsa-binary-search','Binary Search',          6),
    ('dsa-sorting',      'Sorting',                7),
    ('dsa-recursion',    'Recursion & Backtracking', 8),
    ('dsa-trees',        'Trees & BST',            9),
    ('dsa-heap',         'Heaps / Priority Queue', 10),
    ('dsa-graphs',       'Graphs',                 11),
    ('dsa-greedy',       'Greedy',                 12),
    ('dsa-dp',           'Dynamic Programming',    13),
    ('dsa-bits',         'Bit Manipulation',       14),
    ('dsa-math',         'Math & Number Theory',   15)
) AS v(code, name, ord);

-- Prep roadmap (the order students are advised to follow)
INSERT INTO roadmap_section (code, name, sort_order) VALUES
    ('DSA',           'Data Structures & Algorithms', 1),
    ('OOP',           'OOP',                          2),
    ('DBMS',          'DBMS & SQL',                   3),
    ('OS',            'Operating Systems',            4),
    ('CN',            'Computer Networks',            5),
    ('APTITUDE',      'Aptitude',                     6),
    ('PROJECTS',      'Projects & Résumé',            7),
    ('SYSTEM_DESIGN', 'System Design Basics',         8);

INSERT INTO roadmap_item (section_code, position, title, description) VALUES
    ('DSA', 1, 'Time & space complexity (Big-O)', 'Analyse loops, recursion and common library operations.'),
    ('DSA', 2, 'Arrays & strings', 'Two pointers, sliding window, prefix sums, string manipulation.'),
    ('DSA', 3, 'Hashing', 'Hash maps/sets for counting, lookups and de-duplication.'),
    ('DSA', 4, 'Linked lists, stacks & queues', 'Reversal, cycle detection, monotonic stack, BFS queue.'),
    ('DSA', 5, 'Binary search & sorting', 'Search on answer, lower/upper bound, merge/quick sort.'),
    ('DSA', 6, 'Recursion & backtracking', 'Subsets, permutations, N-Queens.'),
    ('DSA', 7, 'Trees & BST', 'Traversals, height/diameter, LCA, BST operations.'),
    ('DSA', 8, 'Heaps', 'Top-K, merge K sorted lists, running median.'),
    ('DSA', 9, 'Graphs', 'BFS/DFS, topological sort, Dijkstra, union-find.'),
    ('DSA', 10, 'Dynamic programming', '1-D/2-D DP, knapsack, LIS, LCS, DP on grids.'),
    ('OOP', 1, 'Four pillars', 'Encapsulation, abstraction, inheritance, polymorphism with Java examples.'),
    ('OOP', 2, 'Interfaces vs abstract classes', 'When to use which; default methods.'),
    ('OOP', 3, 'SOLID principles & common design patterns', 'Singleton, factory, strategy, observer.'),
    ('DBMS', 1, 'ER model & normalisation', '1NF to BCNF, functional dependencies.'),
    ('DBMS', 2, 'SQL queries', 'Joins, GROUP BY/HAVING, subqueries, window functions.'),
    ('DBMS', 3, 'Transactions & indexing', 'ACID, isolation levels, B-tree indexes.'),
    ('OS', 1, 'Processes & threads', 'Scheduling algorithms, context switching.'),
    ('OS', 2, 'Synchronisation & deadlocks', 'Mutex, semaphores, Banker''s algorithm.'),
    ('OS', 3, 'Memory management', 'Paging, segmentation, virtual memory, page replacement.'),
    ('CN', 1, 'OSI & TCP/IP models', 'What each layer does; common protocols.'),
    ('CN', 2, 'TCP vs UDP, HTTP/HTTPS, DNS', 'Handshakes, TLS, what happens when you type a URL.'),
    ('APTITUDE', 1, 'Quantitative aptitude', 'Percentages, ratios, time & work, probability.'),
    ('APTITUDE', 2, 'Logical reasoning & verbal', 'Puzzles, series, seating, reading comprehension.'),
    ('PROJECTS', 1, 'Two solid projects', 'Deployed, on GitHub, with a README you can defend in an interview.'),
    ('PROJECTS', 2, 'One-page résumé', 'Impact-focused bullets, no spelling mistakes, links work.'),
    ('SYSTEM_DESIGN', 1, 'Scalability basics', 'Load balancers, caching, replication, sharding.'),
    ('SYSTEM_DESIGN', 2, 'Design a URL shortener', 'Classic warm-up: API, schema, scaling reads.');
