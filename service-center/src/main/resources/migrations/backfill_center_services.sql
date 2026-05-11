-- Phase 3.6 — backfill center_services from existing service_category join table.
--
-- Default assumption: every existing center-category link gets a REPAIR service entry,
-- provided REPAIR is mapped for that category in category_services.
-- Owners refine their full service list via POST /centers/my/services after deploy.
--
-- Prerequisites:
--   1. The `service` catalog must be seeded (run app once so ServiceCatalogSeeder fires).
--   2. The `category_services` mapping must be seeded (same boot).
--   3. Run this SQL ONCE against the target database, then verify center_services rows.
--
-- Idempotent: ON CONFLICT DO NOTHING skips already-existing (center, category, service) rows.
-- Actual join table name: service_category  (not center_categories as some docs suggest).

INSERT INTO center_services (center_id, category_id, service_id, is_active, created_at)
SELECT
    sc.center_id,
    sc.category_id,
    s.id,
    true,
    NOW()
FROM service_category sc
CROSS JOIN (SELECT id FROM service WHERE code = 'REPAIR') s
WHERE EXISTS (
    SELECT 1
    FROM category_services cs
    WHERE cs.category_id = sc.category_id
      AND cs.service_id  = s.id
)
ON CONFLICT (center_id, category_id, service_id) DO NOTHING;
