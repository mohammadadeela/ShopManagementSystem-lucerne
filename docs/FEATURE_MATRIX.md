# Feature Matrix

| Area | Specialized workflow | Generic management/reporting | Transaction protected |
|---|---:|---:|---:|
| Authentication/session | Yes | Yes | Yes where state changes span tables |
| Dashboard/charts | Yes | — | Read-only |
| Products | Yes | Yes | Single-row CRUD |
| POS/sale creation | Yes | — | Yes |
| Sale cancellation/restock | Yes | — | Yes |
| Inventory adjustment/damage/transfer | Yes | Yes | Yes |
| User administration | Yes | Yes | Transactional guards where required |
| Roles and permissions | Yes | Yes | Yes |
| Discounts | Yes | Yes | Transactional audit update |
| Customers/employees | Add/manage | Yes | Single-row CRUD |
| Orders | Status/manage | Yes | Validate on target MySQL |
| Returns/exchanges | Request/manage | Yes | Full processing requires runtime acceptance test |
| Expenses | Add/manage | Yes | Approval requires runtime acceptance test |
| Stock requests | Review/manage | Yes | Fulfillment requires runtime acceptance test |
| Suppliers/purchase orders | Add/manage | Yes | Receiving requires runtime acceptance test |
| Daily closing | Create/manage | Yes | Unique constraint prevents duplicates |
| Reports/export | Yes | 22 reports | Read-only/export |
| Audit/notifications/settings | Yes | Yes | Protected by permissions |
| Backup/restore | Yes | — | External MySQL process result checked |
