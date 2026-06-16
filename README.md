# FixItNow — Backend (Spring Boot)

REST API for the FixItNow on-demand home-repair platform. **Java 21 · Spring Boot 4 · MySQL 8 · JWT + Google OAuth2.** The Maven project lives in [`FixItNow/`](FixItNow/).

---

## ⚠️ Read this first — fixing the "DB error" on a fresh clone

When you clone and run the app you will likely see:

```
Access denied for user 'root'@'localhost' (using password: YES)
```

This is **not** a missing-schema problem (the schema auto-creates). It happens because the
file that holds your MySQL password — `application-local.properties` — is **git-ignored**,
so it does not come with the clone. **You must create your own.**

### The fix (one-time, ~1 minute)

1. Make sure **MySQL 8** is installed and running on `localhost:3306`, and know your root password.
2. From `FixItNow/src/main/resources/`, copy the template and set your password:
   ```bash
   cd FixItNow/src/main/resources
   cp application-local.properties.example application-local.properties
   ```
   Open `application-local.properties` and set:
   ```properties
   spring.datasource.password=YOUR_MYSQL_PASSWORD
   ```
   (Optionally set `jwt.secret` and the Google OAuth keys — see the template comments.)
3. Run the app (next section). The database `fixitnow_db` and all tables are created
   automatically on first start (`spring.jpa.hibernate.ddl-auto=update` +
   `createDatabaseIfNotExist=true`).

`application-local.properties` is auto-imported by `application.properties`
(`spring.config.import`), so once it exists the app runs with **no environment variables**.

---

## Local setup & running

### Prerequisites
- **JDK 21** (`java -version` → 21.x)
- **MySQL 8** running on `localhost:3306`
- Maven is **not** required — the Maven Wrapper (`mvnw` / `mvnw.cmd`) is included.

### Run
```bash
cd FixItNow
# macOS/Linux/Git Bash:
./mvnw spring-boot:run
# Windows PowerShell:
.\mvnw.cmd spring-boot:run
```
Wait for `Started FixItNowApplication` → API is at **http://localhost:8080**.

> A convenience launcher is also provided at the monorepo root: `start-backend.cmd`.

---

## Database

The schema is created automatically by Hibernate on first run, so **you normally do not
touch SQL at all**. For explicit provisioning or to read the schema, two scripts are
provided in [`FixItNow/db/`](FixItNow/db/):

| File | Purpose | Run |
|---|---|---|
| `db/schema.sql` | Creates `fixitnow_db` + all 16 tables | `mysql -u root -p < FixItNow/db/schema.sql` |
| `db/seed.sql` | Optional demo categories & services | `mysql -u root -p fixitnow_db < FixItNow/db/seed.sql` |

To create an **admin** for local testing, register through the app, then promote the row:
```sql
UPDATE users SET user_type = 'ADMIN', is_verified = 1 WHERE email = 'you@example.com';
```

> `mysql` not on your PATH? On Windows it's at
> `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`.

---

## Configuration reference

`application.properties` reads every secret from an env var with a safe local default,
and also imports `application-local.properties` if present. You can configure via **either**
the local file (recommended) **or** environment variables:

| Property / Env var | Default | Notes |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/fixitnow_db?...` | JDBC URL (auto-creates the DB) |
| `DB_USERNAME` | `root` | MySQL user |
| `DB_PASSWORD` | `password` | **Set this to your password** |
| `JWT_SECRET` | dev fallback | HS256 signing key (≥ 32 chars in prod) |
| `SERVER_PORT` | `8080` | Backend port |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:8080` | Frontend origins |
| `GOOGLE_OAUTH_CLIENT_ID` / `..._SECRET` | empty | Enables Google sign-in when set |

---

## Testing

```bash
cd FixItNow
.\mvnw.cmd test      # Windows  (./mvnw test on macOS/Linux)
```
The suite uses **JUnit 5 + Mockito + AssertJ** for unit tests
(`AuthServiceTest`, `SmartMatchServiceTest`, …) and **MockMvc** for integration tests
(`FlowIntegrationTest`, `UserAcceptanceTest`). It runs automatically on every push via
**GitHub Actions** (`.github/workflows/backend-ci.yml`) against a MySQL service container.

---

## Project layout
```
backend/
├── README.md
├── start-backend.cmd            (root-level convenience launcher, if present)
└── FixItNow/                    ← Maven project
    ├── mvnw / mvnw.cmd / pom.xml
    ├── db/
    │   ├── schema.sql           ← full DB schema
    │   └── seed.sql             ← optional demo data
    └── src/
        ├── main/java/com/example/FixItNow/
        │   ├── controller/  service/  repository/  entity/
        │   ├── dto/  enums/  security/  config/  exception/
        └── main/resources/
            ├── application.properties
            └── application-local.properties.example   ← copy → application-local.properties
```

## Troubleshooting
- **`Access denied for user 'root'`** → create `application-local.properties` with your password (see top).
- **`Port 8080 was already in use`** → an old run is still bound; end the Java process (Task Manager) or set `SERVER_PORT`.
- **Google sign-in 404 / `redirect_uri_mismatch`** → register `http://localhost:8080/login/oauth2/code/google` in your Google OAuth client.
