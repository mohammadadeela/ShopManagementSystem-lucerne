# Lucerne Boutique Professional

A Java desktop boutique management system built with **Java 21, JavaFX, JDBC, MySQL 8, JavaFX CSS, PreparedStatement, and BCrypt**. The original project is preserved under `backup/original_project/`; the active application is a refactored project with one database layer, role-aware navigation, transactional business operations, dashboards, charts, filters, reports, audit logging, notifications, and realistic test data.

## Main capabilities

- Role-aware application shell for OWNER, ADMIN, MANAGER, CASHIER, WAREHOUSE, and CUSTOMER.
- Live dashboards with MySQL summary cards, trends, rankings, warnings, and recent records.
- Products with category/subcategory, barcode, cost, price, margin, images, active status, variants, and stock visibility.
- Point of sale with product/barcode search, size/color variants, customer selection, discounts, payment validation, stock validation, receipt preview, and one atomic transaction.
- Sales and transactions with combined server-side filters, pagination, details, sale items, receipt regeneration, CSV export, and authorized cancellation that restores stock.
- Inventory by branch or warehouse with low/out/over-stock status, adjustment, damage recording, transfer, movement history, and rollback protection.
- Users, database-backed role/permission editing, account activation/locking, password reset, branch/warehouse assignment, last-owner protection, and self-deactivation protection.
- Discount management with percentage/fixed rules, validity dates, purchase minimums, caps, activation, audit, and export.
- Customers, employees, orders, returns/exchanges, expenses, suppliers, purchasing, stock requests, daily closing, notifications, settings, audit, reports, and database maintenance modules.
- Twenty-two report definitions with relevant date/entity/status/amount filters and UTF-8 CSV export.
- Configurable login lockout, password policy, currency, receipt footer, stock threshold, date format, and business identity.
- Database backup/restore UI that reports the real command result and requires MySQL command-line tools.

## Project structure

```text
src/main/java/com/lucerne/
  app/       application entry, session, navigation
  config/    application and database configuration
  dao/       JDBC access and parameterized queries
  model/     session, product, cart and dashboard models
  service/   authentication, authorization, sales, receipts, settings
  ui/        login, shell, dashboards and business pages
  util/      formatting, validation, export, alerts and logging
src/main/resources/
  css/app.css
  images/products/           60 local product images
  config/database.properties classpath fallback
database/
  lucerne_demo_final.sql       full fresh demo installation
  lucerne_demo_schema.sql      schema + roles/permissions, no business seed data
  lucerne_demo_sample_data.sql optional realistic test data
config/database.properties     external runtime configuration
backup/original_project/       untouched original upload
```

## Requirements

- JDK 21
- Apache Maven 3.9 or newer
- MySQL Server 8.0 or newer
- MySQL client tools (`mysql`; `mysqldump` for backup)
- Internet access on the first Maven build to download JavaFX, Connector/J, and jBCrypt

Dependencies are declared in `pom.xml`:

- OpenJFX 21.0.5
- MySQL Connector/J 8.4.0
- jBCrypt 0.4

## Database installation

### Option A — complete demo database

**This script drops and recreates `lucerne_demo`. Do not run it against the only copy of real data.**

```bash
mysql -u root -p < database/lucerne_demo_final.sql
```

On Linux/macOS, `scripts/import_demo.sh` adds an explicit confirmation. On Windows, use `scripts\import_demo.bat`.

### Option B — schema first, optional sample data

```bash
mysql -u root -p < database/lucerne_demo_schema.sql
mysql -u root -p < database/lucerne_demo_sample_data.sql
```

The schema includes 49 normalized tables, indexes, foreign keys, check constraints, 13 analytical views, and two safe `BEFORE UPDATE` timestamp triggers. Complex inventory, sales, cancellation, adjustment, and transfer logic is implemented in Java transactions rather than circular triggers, avoiding MySQL Error 1442.

## Database configuration

Edit `config/database.properties`:

```properties
db.host=localhost
db.port=3306
db.name=lucerne_demo
db.user=root
db.password=YOUR_MYSQL_PASSWORD
db.useSSL=false
db.connectTimeout=8000
```

Environment variables override file values, for example `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD`. The application first reads the external `config/database.properties`; if it is absent, it uses the classpath fallback.

## Compile and run

```bash
mvn clean compile
mvn javafx:run
```

Or use:

```bash
./scripts/run.sh
```

Windows:

```bat
scripts\run.bat
```

The minimum practical window is 1180×720. Layouts are designed for 1366×768, 1440×900, and 1920×1080.

## Demo users

All optional demo accounts use the password **`Lucerne@123`**.

| Role | Username |
|---|---|
| OWNER | `owner` |
| ADMIN | `admin` |
| MANAGER | `manager.ramallah` |
| MANAGER | `manager.birzeit` |
| CASHIER | `cashier.ramallah` |
| CASHIER | `cashier.birzeit` |
| CASHIER | `cashier.nablus` |
| WAREHOUSE | `warehouse.central` |
| CUSTOMER | `customer.demo` |

Change demo passwords before using the database outside a local test environment.

## Included test data

The optional seed contains multiple branches and warehouses, nine role accounts, employees, 200 customers, 10 categories, 28 subcategories, 60 illustrated product images, 180 size/color variants, branch and warehouse inventory, 10,000 sales, 30,000 sale items, orders, returns, expenses, suppliers, purchase orders, stock requests, notifications, daily closings, movements, and audit history. Product image paths point to the bundled PNG resources.

## Security behavior

- Passwords are checked with BCrypt and never compared case-insensitively.
- Legacy plain-text accounts can be migrated on successful login; the legacy value is cleared after BCrypt hashing.
- Failed attempts, lock duration, active state, expiration, last login, sessions, and logout are recorded.
- Unknown usernames are logged without revealing whether an account exists.
- Page visibility and sensitive service operations both use permissions.
- PreparedStatement is used for user values; resources use try-with-resources.
- Passwords, hashes, database passwords, and tokens are excluded from logs and exports.

## Financial formulas

The application and views use these meanings:

- **Gross sales** = sum of item unit price × sold quantity before discounts.
- **Discounts** = sale-level and validated discount reductions.
- **Net revenue** = gross sales − discounts, excluding cancelled sales and adjusted for processed returns where recorded.
- **COGS** = `CostAtSale × valid sold quantity`, adjusted for returned quantity.
- **Gross profit** = net revenue − COGS.
- **Net profit** = gross profit − approved operating expenses − included refunds/operating costs.

Salary is stored and permission-protected. It is not silently subtracted from every net-profit query; add payroll-period records before treating salary as a period expense.

## Receipts and export

Receipts show boutique, reference, date, branch, cashier, customer, variants, quantity, unit price, subtotal, discount, total, payment, paid amount, change, and configured footer. The preview can save a printable UTF-8 text receipt.

CSV exports use UTF-8 with a BOM for Excel compatibility, export the filtered result set, format headings, and omit credentials/security secrets.

## Backup and restore

Open **Administration → Database Maintenance** with the required permission. Backup invokes `mysqldump`; restore invokes `mysql` with direct process arguments and redirected input. The page displays success only when the command exits successfully.

Requirements:

- `mysql` and `mysqldump` must be on `PATH`.
- The configured MySQL user must have appropriate privileges.
- Store backups outside the project before replacing or deleting the project folder.
- Restore is destructive and requires confirmation.

## Logging

Application logs are written under `logs/`. Startup, shutdown, connection problems, unexpected failures, transaction rollbacks, export failures, and backup/restore failures are logged without secrets.

## Troubleshooting

- **Cannot connect:** verify MySQL is running, import the schema, and correct `config/database.properties`.
- **Access denied:** verify `db.user`, `db.password`, and MySQL grants.
- **JavaFX runtime error:** run with `mvn javafx:run`, not by double-clicking a plain JAR.
- **Images missing:** do not remove `src/main/resources/images/products` and rebuild with Maven.
- **Backup command not found:** install MySQL client tools and add their `bin` directory to `PATH`.
- **Charts empty:** confirm demo data was imported and the selected date/branch filters include records.

## Verification scope

The source tree was compiled in the delivery environment using Java 21 against local API-compatible compile stubs because Maven/OpenJFX and MySQL Server were not installed there. Static compilation, resource/path checks, SQL/Java identifier checks, unsafe-pattern scans, image checks, and ZIP integrity checks were performed. A live MySQL import, real Connector/J connection, JavaFX launch, printer interaction, and OS-specific MySQL CLI execution must be run on the target machine; see `BUILD_VERIFICATION.md` and `TESTING_CHECKLIST.md`.
