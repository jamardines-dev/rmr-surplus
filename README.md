# RMR SURPLUS Inventory System

## Run The Desktop App

Use the helper script from the project root:

```sh
./run.sh
```

Or run the JavaFX Maven plugin directly from the desktop module:

```sh
export DB_PASSWORD=jamjan08
./mvnw -pl rmr javafx:run
```

The local Maven wrapper also redirects this root command to the desktop module:

```sh
./mvnw javafx:run
```

The root project is a Maven parent project. The JavaFX plugin is configured inside `rmr`, so the wrapper selects that module for JavaFX runs.

## Export The Desktop App For macOS

Create a `.env` file in the project root before packaging:

```sh
DB_URL=jdbc:postgresql://localhost:5432/inventory_db
DB_USERNAME=postgres
DB_PASSWORD=your_database_password
```

Build a double-clickable macOS app:

```sh
./scripts/build-macos-app.sh
```

The script creates:

- `dist/macos-YYYYMMDD-HHMMSS/RMR Inventory.app`
- `dist/RMR-Inventory-macos-YYYYMMDD-HHMMSS.zip`

Send the `.zip` file to the client. After unzipping, they can open `RMR Inventory.app` directly.

## Export The Desktop App For Windows

Build this on a Windows computer with JDK 26 installed. The Windows build creates a folder that contains `RMR Inventory.exe`.

Use the same `.env` file in the project root:

```sh
DB_URL=jdbc:postgresql://localhost:5432/inventory_db
DB_USERNAME=postgres
DB_PASSWORD=your_database_password
```

Run:

```bat
scripts\build-windows-app.bat
```

The script creates:

- `dist\windows-YYYYMMDD-HHMMSS\RMR Inventory\RMR Inventory.exe`
- `dist\RMR-Inventory-windows-YYYYMMDD-HHMMSS.zip`

Send the `.zip` file to the client. After unzipping, they can open `RMR Inventory.exe` directly.

## Build Windows App With GitHub Actions

Before running the GitHub Actions workflow, add these repository secrets in GitHub:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Use `Settings` > `Secrets and variables` > `Actions` > `New repository secret`.

Then run the `Build Windows EXE` workflow from the `Actions` tab. When it succeeds, download the `RMR-Inventory-windows` artifact. It contains `RMR-Inventory-windows.zip`, which your client can unzip and run.

## Admin PC Server Mode

The Admin/Main PC should be the only computer with the database password. Start PostgreSQL on that computer, then run the backend server:

```sh
export DB_PASSWORD=jamjan08
./mvnw -pl inventory-server spring-boot:run
```

The server runs on port `8080` by default and now exposes secured APIs under `/api`.

Login first:

```sh
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

The response includes a `token`. Send it on protected requests:

```sh
curl http://localhost:8080/api/products \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

Server-side rules now protect the important actions:

- `ADMIN` can create, edit, deactivate, and view products.
- `EMPLOYEE` can view products and record cart sales.
- Sales are validated on the server before stock is deducted.
- Login, product changes, and sales create audit log records.

Next migration step: point the desktop app services to `http://ADMIN_PC_IP:8080/api` so teller PCs no longer connect directly to PostgreSQL.

## Test

```sh
./mvnw test
```
