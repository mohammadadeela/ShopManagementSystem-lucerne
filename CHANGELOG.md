# Changelog — 2.0.0

## Architecture

- Preserved the uploaded project unchanged under `backup/original_project/`.
- Replaced the 7,304-line monolithic `MainFX.java` architecture with application, config, DAO, model, service, UI, component, dialog, and utility packages.
- Consolidated duplicate database connection classes into one configurable `DatabaseConnection`.
- Removed runtime schema creation/alteration from page-loading code.
- Moved reusable styling into `src/main/resources/css/app.css`.

## UI and navigation

- Added a professional warm boutique theme, sidebar, top bar, breadcrumb/page context, notification count, user menu, database status, last refresh, responsive cards, tables, dialogs, loading/error/empty states, and collapsible navigation.
- Replaced category-specific navigation with one filterable Products page.
- Added role-specific navigation for OWNER, ADMIN, MANAGER, CASHIER, WAREHOUSE, and CUSTOMER.
- Added reusable cards, filter controls, paginated tables, status formatting, tooltips, confirmation dialogs, and receipt preview.

## Business modules

- Added live role-aware dashboards and MySQL-backed charts.
- Added product CRUD, image preview, category/subcategory data, variant visibility, stock and margin calculations.
- Added transactional POS with stock validation, payments, movement history, audit, rollback, and receipt generation.
- Added a dedicated Sales & Transactions page with combined filters, details, item inspection, export, receipt regeneration, and atomic cancellation/restocking.
- Added inventory adjustment, damaged-stock recording, transfer, history, and branch/warehouse isolation.
- Added user administration with activation, locking, password reset, role/location links, and owner safety rules.
- Added transactional Roles & Permissions administration with audited grant replacement and owner/admin escalation guards.
- Added discount creation, editing, activation/deactivation, validation, filtering, cards, audit, and export.
- Added customer, employee, order, expense, return/exchange, stock request, supplier, purchasing, daily closing, audit, notification, report, settings, profile, and database-maintenance pages.
- Added twenty-two CSV report definitions and role/branch/warehouse scoping.

## Database

- Added `lucerne_demo_final.sql`, schema-only and optional sample-data scripts.
- Added 49 normalized tables, foreign keys, indexes, unique/check constraints, timestamps, soft-active fields, reference numbers, permission tables, sessions, login attempts, audit logs, settings, payments, inventory movements, and daily closing.
- Added 13 analytical views for current inventory, low stock, sales, performance, customer/order/return statistics, gross profit, and net profit.
- Kept only two safe timestamp triggers; removed circular/self-updating trigger behavior that can cause MySQL Error 1442.
- Added realistic performance-test data including 10,000 sales, 30,000 sale items, 60 products, and 60 local product images.

## Security and reliability

- Added BCrypt authentication and gradual legacy-password migration.
- Added configurable failed-attempt lockout, account expiration, active checks, password-change-required flow, sessions, and audit events.
- Replaced user-value SQL concatenation with parameterized statements.
- Added transaction commit/rollback to sale, cancellation, inventory adjustment, damage, and transfer workflows.
- Added friendly database errors, centralized logging, and secret-safe export/log rules.
- Added direct `ProcessBuilder` backup/restore execution and real exit-code validation.

## Removed or merged

- Removed duplicate connection code from the active application.
- Merged duplicated category/product pages into Products.
- Merged sales history and transaction search into Sales & Transactions.
- Replaced disconnected demonstration screens with reusable data modules and specialized workflow pages.
- Removed hardcoded active-source database credentials.

## Known environment limitations

- The delivery container had Java 21 and Ant, but not Maven/OpenJFX/MySQL Server. Source compilation was therefore checked with API-compatible compile stubs; live JavaFX/MySQL runtime testing remains required on the target computer.
- Receipt output is previewable and savable as printable UTF-8 text; direct hardware-printer behavior depends on the target OS/printer setup.
- Backup/restore requires MySQL client executables on `PATH` and adequate database privileges.
