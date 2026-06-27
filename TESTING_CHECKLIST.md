# Lucerne Boutique Testing Checklist

Legend: `[x]` verified statically in the delivery environment; `[ ]` requires the target machine, MySQL, JavaFX runtime, or manual business validation.

## Build and installation

- [x] All active Java source files pass Java 21 compilation against API-compatible JavaFX/jBCrypt stubs.
- [x] Package names and source paths match.
- [x] CSS and bundled image paths exist.
- [x] 60 PNG product images are included and non-empty.
- [x] Final SQL contains tables, keys, indexes, views, safe triggers, permissions, and optional sample data.
- [x] Active project contains one database connection class.
- [x] No active hardcoded real database password.
- [x] ZIP structure and CRC integrity checked.
- [ ] Run `mvn clean compile` with downloaded dependencies.
- [ ] Import `lucerne_demo_final.sql` into MySQL 8.
- [ ] Start with `mvn javafx:run`.
- [ ] Test database connection from the login and maintenance screens.

## Authentication and authorization

- [ ] OWNER login.
- [ ] ADMIN login.
- [ ] MANAGER logins for each assigned branch.
- [ ] CASHIER logins for each branch.
- [ ] WAREHOUSE login.
- [ ] CUSTOMER login and personal-data isolation.
- [ ] Invalid username/password response.
- [ ] Failed-attempt counter and timed lock.
- [ ] Inactive and expired account behavior.
- [ ] Forced password-change flow.
- [ ] Logout closes the session and records logout time.
- [ ] Unauthorized direct page access is blocked.
- [ ] Last active OWNER cannot be deactivated.
- [ ] Current owner cannot deactivate their own account.
- [ ] Manager/cashier/warehouse/customer cannot escalate roles.

## Dashboard and charts

- [ ] Owner dashboard cards and all-period calculations.
- [ ] Manager branch isolation.
- [ ] Cashier own-sales isolation.
- [ ] Warehouse inventory isolation.
- [ ] Customer personal-order isolation.
- [ ] Date, branch, category, product, cashier, payment, and status filters.
- [ ] Charts update after filters.
- [ ] Charts display real values with data.
- [ ] Charts show a friendly empty state without data.
- [ ] Database disconnection produces a friendly error and does not freeze the UI.

## Products

- [ ] Create product with barcode, category, prices, image path, and details.
- [ ] Duplicate barcode validation.
- [ ] Edit product and verify audit history.
- [ ] Deactivate/reactivate product.
- [ ] Cost greater than selling price warning.
- [ ] Product image preview and missing-image fallback.
- [ ] Combined product filters and pagination.
- [ ] Cost/margin hidden from unauthorized roles.

## POS and sales

- [ ] Product and barcode search.
- [ ] Category/subcategory and variant selection.
- [ ] Add/increase/decrease/remove/clear cart.
- [ ] Walk-in and registered customer.
- [ ] Discount validation.
- [ ] Cash paid/change validation.
- [ ] Card payment.
- [ ] Successful sale creates sale, items, payment, movement, audit, and inventory reduction.
- [ ] Insufficient stock blocks checkout.
- [ ] Forced failure rolls back every sale step.
- [ ] Receipt preview and save.
- [ ] Sales combined filters and server-side pagination.
- [ ] Sale details and sale item details.
- [ ] Authorized cancellation restores inventory and updates payment/movement/audit.
- [ ] Ineligible or duplicate cancellation is rejected.
- [ ] Filtered sales CSV contains all matching rows and no secrets.

## Inventory and stock

- [ ] Branch inventory filter/isolation.
- [ ] Warehouse inventory filter/isolation.
- [ ] Quantity adjustment with reason.
- [ ] Negative stock prevention.
- [ ] Damaged quantity recording.
- [ ] Branch-to-warehouse, warehouse-to-branch, and valid location transfers.
- [ ] Transfer rollback on failure.
- [ ] Movement history.
- [ ] Reorder/low/out/over-stock calculations.
- [ ] Filtered inventory export.
- [ ] Stock request creation, approval, partial approval, rejection, fulfillment, and cancellation.

## Customers, employees, users

- [ ] Create/edit/deactivate/reactivate customer.
- [ ] Phone/email validation and duplicate behavior.
- [ ] Customer sales/order/return history.
- [ ] Create/edit/assign/deactivate employee.
- [ ] Salary visibility permission.
- [ ] Link employee/customer to only one account.
- [ ] Create/edit/activate/deactivate/lock/unlock user.
- [ ] Reset password and force password change.
- [ ] Branch/warehouse assignment and filters.

## Orders, returns, expenses, purchasing

- [ ] Order filters, details, and valid status progression.
- [ ] Return request creation.
- [ ] Return approval/rejection and processed refund.
- [ ] Exchange replacement and inventory restoration.
- [ ] Return/exchange transaction rollback.
- [ ] Expense add/edit/approve/reject and recurring flag.
- [ ] Supplier add/edit/activate/deactivate.
- [ ] Purchase order create/approve/cancel.
- [ ] Goods receiving updates received quantity, inventory, movements, and status atomically.
- [ ] Damaged receiving path.

## Daily closing, audit, notifications, reports

- [ ] Create daily closing.
- [ ] Duplicate cashier/date/shift closing prevention.
- [ ] Expected/actual/difference calculations.
- [ ] Manager approval.
- [ ] Audit filters and immutability for normal users.
- [ ] Notification unread count, read/unread, type/priority/date filters, and mark-all-read.
- [ ] All 22 report definitions load with data.
- [ ] Reports respect role and location scope.
- [ ] Every CSV export opens correctly in Excel with UTF-8 text.
- [ ] Empty report export behavior.

## Maintenance and failure handling

- [ ] Connection test displays MySQL version.
- [ ] Successful `mysqldump` backup creates a usable file.
- [ ] Missing `mysqldump` produces a failure, not false success.
- [ ] Restore requires strong confirmation.
- [ ] Successful restore from a known backup.
- [ ] Failed restore is reported clearly.
- [ ] Invalid config, timeout, duplicate key, FK failure, export failure, and null data have friendly messages.
- [ ] Logs contain operational failures but no passwords/hashes/tokens.
