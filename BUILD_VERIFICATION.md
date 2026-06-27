# Build Verification Record

## Delivery environment

- Date: 2026-06-20
- JDK: Java 21 available
- Maven: not installed
- OpenJFX runtime: not installed
- MySQL Server/client runtime: not installed for live integration

## Checks performed

1. Extracted and inventoried the original upload.
2. Preserved the original files under `backup/original_project/`.
3. Compiled all active `src/main/java` files with `javac --release 21` against local API-compatible JavaFX and jBCrypt compile stubs.
4. Repeated compilation after transaction, authentication, filtering, receipt, and maintenance corrections.
5. Checked package/resource paths, CSS existence, and product image files.
6. Scanned active source for duplicate connection classes, hardcoded credentials, TODO/FIXME/placeholder markers, `printStackTrace`, and suspicious raw SQL concatenation.
7. Compared SQL table/view identifiers referenced in active Java queries with the final SQL definitions.
8. Checked SQL object counts, trigger type, marker separation, and foreign-key-check reset.
9. Verified the demo BCrypt hash using the system `htpasswd` implementation; it matches `Lucerne@123`.
10. Created a final ZIP and verified its CRC using `unzip -t`.

## What this proves

- Java syntax, package relationships, and referenced JavaFX APIs are internally consistent enough to compile under Java 21 with the represented APIs.
- Required project resources and generated product images are packaged.
- The delivered archive is structurally readable and not corrupt.

## What still requires the target environment

The following cannot honestly be certified without Maven dependencies, OpenJFX, and MySQL 8:

- `mvn clean compile` with the real dependencies.
- SQL import execution by MySQL.
- Connector/J connection and query execution.
- JavaFX launch and visual interaction.
- Live commit/rollback behavior against InnoDB.
- Hardware printer and OS-specific file chooser behavior.
- `mysql`/`mysqldump` backup and restore on the target OS.

Run the unchecked items in `TESTING_CHECKLIST.md` before production use.
