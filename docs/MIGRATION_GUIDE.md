# Migration Guide

The original upload is preserved in `backup/original_project/`. The new schema is a normalized replacement intended for a fresh test installation first.

## Safe migration sequence

1. Stop the old application.
2. Create a verified backup of the existing MySQL database using MySQL Workbench or `mysqldump`.
3. Copy the backup to a location outside this project.
4. Import `lucerne_demo_final.sql` only into a disposable/test MySQL instance.
5. Compare old and new identifiers, especially users, branches, products, customers, sales, inventory, and foreign keys.
6. Migrate real records with explicit `INSERT ... SELECT` mappings or an ETL script; do not run the demo script over real data.
7. Preserve original primary keys where possible. When not possible, build mapping tables before importing dependent rows.
8. Reconcile inventory quantities and financial totals before switching applications.
9. Create production users with new passwords and force a password change where appropriate.
10. Run the full testing checklist and keep the old application/database read-only until acceptance.

## Legacy password support

The new `users` table includes both `PasswordHash` and temporary `LegacyPassword`. If a legacy account must be imported, place the old value in `LegacyPassword`, keep `PasswordHash` null, and set `PasswordChangeRequired=1`. On successful authentication the application writes a BCrypt hash and clears `LegacyPassword`. Remove all remaining legacy values after the migration window.

## Important schema differences

- Roles and permissions are normalized into `roles`, `permissions`, `role_permissions`, and optional `user_permissions`.
- Employee/customer account links are explicit and unique.
- Products use category/subcategory plus `product_variants` for size/color/barcode SKU detail.
- Branch and warehouse quantities are separated and accompanied by immutable `stock_movements`.
- Sales use header, item, payment, cash movement, return, and audit tables.
- Settings are stored in `system_settings` rather than hardcoded throughout UI code.
- Complex business updates are Java transactions instead of self-updating triggers.
