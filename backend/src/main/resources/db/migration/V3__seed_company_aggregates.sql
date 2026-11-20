-- Public, company-level aggregates only (no student data). Figures are approximate and
-- labelled with their source everywhere they are shown.

INSERT INTO company (slug, name) VALUES
    ('microsoft', 'Microsoft'),
    ('amazon', 'Amazon'),
    ('salesforce', 'Salesforce'),
    ('paypal', 'PayPal'),
    ('servicenow', 'ServiceNow'),
    ('goldman-sachs', 'Goldman Sachs'),
    ('electronic-arts', 'Electronic Arts'),
    ('jpmorgan-chase', 'JPMorgan Chase'),
    ('oracle', 'Oracle'),
    ('qualcomm', 'Qualcomm'),
    ('barclays', 'Barclays'),
    ('ibm', 'IBM'),
    ('ltimindtree', 'LTIMindtree'),
    ('accenture', 'Accenture'),
    ('infosys', 'Infosys'),
    ('super-money', 'Super.Money (Flipkart)'),
    ('de-shaw', 'D. E. Shaw'),
    ('f5-networks', 'F5 Networks'),
    ('pegasystems', 'Pegasystems'),
    ('bosch', 'Bosch'),
    ('blackbaud', 'Blackbaud'),
    ('mssquare', 'MSsquare');

INSERT INTO company_alias (company_id, alias)
SELECT c.id, a.alias
FROM (VALUES
    ('jpmorgan-chase', 'JPMC'),
    ('jpmorgan-chase', 'JP Morgan'),
    ('jpmorgan-chase', 'J.P. Morgan'),
    ('jpmorgan-chase', 'JPMorgan'),
    ('ltimindtree', 'LTM'),
    ('ltimindtree', 'LTI Mindtree'),
    ('super-money', 'Super.Money'),
    ('super-money', 'Super Money'),
    ('de-shaw', 'DE Shaw'),
    ('de-shaw', 'D E Shaw'),
    ('de-shaw', 'D.E. Shaw'),
    ('electronic-arts', 'EA'),
    ('f5-networks', 'F5'),
    ('pegasystems', 'Pega'),
    ('goldman-sachs', 'Goldman')
) AS a(slug, alias)
JOIN company c ON c.slug = a.slug;

-- Full-time offers. (slug, year, ctc_min, ctc_max, offers)
INSERT INTO placement_stat (company_id, academic_year, ctc_min_lpa, ctc_max_lpa, offers, source_url, source_note)
SELECT c.id, s.yr, s.ctc_min, s.ctc_max, s.offers,
       'https://cbit.ac.in/placement_post/year-wise-placements-2021/',
       'Source: CBIT public placement records (approx.)'
FROM (VALUES
    ('microsoft',       '2024-25', 51.0,  51.0,  2),
    ('amazon',          '2024-25', 47.0,  47.0,  1),
    ('salesforce',      '2024-25', 40.0,  40.0,  2),
    ('paypal',          '2024-25', 34.0,  34.0,  1),
    ('servicenow',      '2024-25', 33.0,  33.0,  6),
    ('goldman-sachs',   '2024-25', 30.0,  30.0,  1),
    ('electronic-arts', '2024-25', 30.0,  30.0,  3),
    ('jpmorgan-chase',  '2024-25', 19.8,  19.8, 24),
    ('oracle',          '2024-25', 19.7,  19.7, 12),
    ('qualcomm',        '2024-25', 18.7,  18.7,  9),
    ('barclays',        '2024-25', 13.3,  13.3,  8),
    ('ibm',             '2024-25', 11.0,  11.0,  9),
    ('ltimindtree',     '2024-25',  4.1,   4.1, 53),
    ('accenture',       '2024-25',  4.6,   4.6, 50),
    ('infosys',         '2024-25', NULL,   9.5, 36),   -- "up to 9.5 LPA"
    ('super-money',     '2025-26', 54.0,  54.0,  2),
    ('electronic-arts', '2025-26', 30.0,  30.0,  3),
    ('de-shaw',         '2025-26', 26.0,  26.0,  1),
    ('f5-networks',     '2025-26', 20.0,  20.0,  5),
    ('jpmorgan-chase',  '2025-26', 19.8,  19.8, 22),
    ('pegasystems',     '2025-26', 16.0,  16.0,  6),
    ('barclays',        '2025-26', 13.5,  13.5, 14),
    ('bosch',           '2025-26', 12.0,  12.0, 10),
    ('blackbaud',       '2025-26', 12.0,  12.0, 10),
    ('mssquare',        '2025-26',  6.0,   6.0, 55),
    ('accenture',       '2025-26',  4.5,   6.5, 33),
    ('ltimindtree',     '2025-26',  4.1,   4.1, 33)
) AS s(slug, yr, ctc_min, ctc_max, offers)
JOIN company c ON c.slug = s.slug;

-- Internship stipends, ₹ per month.
INSERT INTO internship_stat (company_id, academic_year, stipend_inr_month, source_url, source_note)
SELECT c.id, '2024-25', s.stipend,
       'https://cbit.ac.in/placement_post/year-wise-placements-2021/',
       'Source: CBIT public placement records (approx.)'
FROM (VALUES
    ('microsoft',      125000),
    ('goldman-sachs',  120000),
    ('paypal',         100000),
    ('jpmorgan-chase',  75000),
    ('barclays',        75000),
    ('salesforce',      70000),
    ('oracle',          50000),
    ('servicenow',      45000),
    ('qualcomm',        45000)
) AS s(slug, stipend)
JOIN company c ON c.slug = s.slug;
