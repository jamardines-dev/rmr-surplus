# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

**RMR SURPLUS Inventory System** is a client-server inventory management and sales tracking application for automotive parts. The system consists of:

- **Desktop Client** (JavaFX): A rich desktop application for tellers and inventory staff
- **REST Server** (Spring Boot): A centralized backend for data persistence and business logic
- **PostgreSQL Database**: Shared data store with Flyway migrations

The project uses Java 21, Maven (multi-module), Spring Boot 3.3.5, and JavaFX 21 for the UI framework.

## Architecture

### Multi-Module Maven Structure

```
vehicle-inventory-system (parent POM)
├── inventory-server     (Spring Boot REST API + business logic)
├── rmr                  (JavaFX desktop client)
└── inventory-desktop    (legacy/alternate desktop module)
```

### Key Architectural Patterns

**Domain-Driven Design**: Each feature domain (auth, product, sales, inventory, audit) is organized with distinct layers:
- **domain/**: Core business entities and logic (Product, Sale, SaleItem, User, Role)
- **application/**: Use case services and DTOs (RecordSaleService, SalesQueryService, etc.)
- **infrastructure/**: Repositories and data access (ProductRepository, SaleRepository, etc.)
- **presentation/**: Controllers and UI (JavaFX controllers in rmr, @RestController in server)

**Client-Server Data Flow**:
- Desktop client can connect directly to PostgreSQL (direct DB mode) OR
- Desktop client connects to REST server which enforces business rules (server-mediated mode)
- Server validates all mutations (sales, product changes) with role-based authorization
- Audit logging on the server tracks all mutations by user and timestamp

**Role-Based Authorization**:
- `ADMIN`: Create, edit, deactivate products; view all data
- `EMPLOYEE`: View products; record and view sales

### Module Purposes

**inventory-server** (`com.inventory.vehicle.server`):
- Spring Boot REST API server with authentication & authorization
- Exposes `/api/auth/login`, `/api/products`, `/api/sales`, `/api/audit` endpoints
- Manages PostgreSQL connection and Flyway migrations
- Bearer token authentication with SessionTokenService
- BearerTokenAuthenticationFilter for request authorization

**rmr** (`com.inventory.vehicle`):
- JavaFX desktop application with Spring Boot integration
- Organizes features by domain: auth, product, sales, inventory, report, dashboard, audit
- Uses `ViewLoader` and `SceneManager` for navigation between FXML screens
- `InventoryApplication` entry point launches `JavaFxApplication`
- Connects to PostgreSQL or REST server depending on configuration

### Data Model

Key entities shared across modules:
- **User**: Authentication identity with Role (ADMIN, EMPLOYEE)
- **Product**: Inventory item with quantity, price, restock date, photos
- **Sale**: Transaction record with timestamp, employee, total amount
- **SaleItem**: Line item in a sale (product quantity × unit price)
- **AuditLog**: Record of all mutations (login, product changes, sales)

Database migrations use Flyway versioning (V1–V9+) located in `src/main/resources/db/migration/`.

## Development Commands

### Setup

1. **Set environment variables** in `.env` file in project root:
   ```
   DB_URL=jdbc:postgresql://localhost:5432/inventory_db
   DB_USERNAME=postgres
   DB_PASSWORD=<your_password>
   ```

2. **Ensure PostgreSQL is running** locally on port 5432 with a database named `inventory_db`.

### Build & Run

**Run desktop app** (JavaFX):
```sh
./run.sh
# or
export DB_PASSWORD=jamjan08
./mvnw javafx:run
# or from root (Maven wrapper redirects to rmr module)
./mvnw javafx:run
```

**Run REST server** (Spring Boot on port 8080):
```sh
export DB_PASSWORD=jamjan08
./mvnw -pl inventory-server spring-boot:run
```

**Run all tests**:
```sh
./mvnw test
```

**Run tests for a single module**:
```sh
./mvnw -pl rmr test
./mvnw -pl inventory-server test
```

**Build JAR for server**:
```sh
./mvnw -pl inventory-server package
```

**Clean builds**:
```sh
./mvnw clean install
```

### Distribution

**Build macOS app** (requires macOS):
```sh
./scripts/build-macos-app.sh
# Creates: dist/macos-YYYYMMDD-HHMMSS/RMR Inventory.app
#          dist/RMR-Inventory-macos-YYYYMMDD-HHMMSS.zip
```

**Build Windows app** (requires Windows + JDK 21):
```bat
scripts\build-windows-app.bat
# Creates: dist\windows-YYYYMMDD-HHMMSS\RMR Inventory\RMR Inventory.exe
#          dist\RMR-Inventory-windows-YYYYMMDD-HHMMSS.zip
```

Windows builds can also be automated via GitHub Actions (see `.github/workflows/build-windows.yml`).

## Important Implementation Details

### Authentication & Authorization

**Server-side** (`inventory-server/src/main/java/com/inventory/vehicle/server/`):
- Login endpoint returns JWT-like bearer token via `SessionTokenService`
- `BearerTokenAuthenticationFilter` intercepts requests and validates token header
- `SecurityConfig` protects endpoints by role (ADMIN-only, EMPLOYEE-accessible, etc.)
- Credentials default: username `rmr`, password varies by migration

**Client-side** (`rmr/src/main/java/com/inventory/vehicle/auth/`):
- Login flow in `LoginController` stores token after authentication
- Token passed in `Authorization: Bearer <token>` header on API requests

### Sales Recording & Validation

**Desktop client** (rmr):
- Collects cart items and sale metadata in UI
- `RecordSaleService` prepares `RecordCartSaleCommand` with items
- Sends to server (or local DB if in direct mode)

**Server** (inventory-server):
- `SaleController.recordCartSale()` validates cart totals and stock availability
- Deducts inventory only after validation succeeds
- Creates AuditLog entry with employee ID and timestamp

### Product Management

Products support:
- Images (stored as file paths or references)
- Restock date tracking
- Receipt type classification
- Active/deactivated status

Changes are logged to AuditLog with user/timestamp.

### Database Migrations

- Migrations live in `src/main/resources/db/migration/`
- Versioned with V1, V2, etc. (Flyway naming)
- Both `inventory-server` and `rmr` have migration copies (they run independently)
- Keep migrations in sync between modules when schema changes
- Run automatically on application startup (Flyway enabled in `application.properties`)

### Testing

- Minimal test suite currently in place
- Test classes: `InventoryApplicationTests` (rmr), `InventoryServerApplicationTests` (server)
- Use `./mvnw test` to run; add `-pl <module>` to test a single module

## Future Migration Path

The system is transitioning from direct-DB desktop clients to a server-mediated architecture:

1. **Current**: Desktop app connects directly to PostgreSQL
2. **Next**: Desktop app connects to Spring Boot server at `http://ADMIN_PC_IP:8080/api`
3. **Server enforces all business rules** (role-based actions, stock validation, audit logging)

When implementing features, assume server-side validation is the source of truth.

## Common Developer Tasks

- **Add a new feature domain**: Create new folders under both `rmr/` and `inventory-server/` with domain/application/infrastructure/presentation layers
- **Modify database schema**: Add a new Flyway migration (V10, V11, etc.) in both modules' `db/migration/` folders
- **Add API endpoint**: Create controller in `inventory-server/src/main/java/com/inventory/vehicle/server/` and corresponding client service in `rmr/src/main/java/com/inventory/vehicle/`
- **Update UI screen**: Edit FXML in `rmr/src/main/resources/fxml/` and corresponding controller class
- **Test a change**: Run `./mvnw test` or `./mvnw -pl <module> test` after modifying code
