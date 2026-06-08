# RMR SURPLUS Inventory System

## Run The Desktop App

Use the helper script from the project root:

```sh
./run.sh
```

Or run the JavaFX Maven plugin directly from the desktop module:

```sh
export DB_PASSWORD=jamjan08
./mvnw -pl inventory-desktop javafx:run
```

The local Maven wrapper also redirects this root command to the desktop module:

```sh
./mvnw javafx:run
```

The root project is a Maven parent project. The JavaFX plugin is configured inside `inventory-desktop`, so the wrapper selects that module for JavaFX runs.

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
