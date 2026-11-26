# RouteSync — Startup Guide

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.x
- Node.js 18+ and npm

---

## 1. Database Setup

```sql
-- Create the database (Spring will create tables automatically on first boot)
CREATE DATABASE IF NOT EXISTS routesync_db;
```

After the app starts for the first time and tables are created, load sample data:

```bash
mysql -u root -p routesync_db < database/schema.sql
mysql -u root -p routesync_db < database/sample-data.sql
```

> Note: `spring.jpa.hibernate.ddl-auto=update` will auto-create tables on startup. Run sample-data.sql only after first boot.

---

## 2. Backend (Spring Boot)

### Configuration

The backend uses sensible defaults. No `.env` file needed for local development.

| Property | Default | Override with Env Var |
|---|---|---|
| DB URL | `jdbc:mysql://localhost:3306/routesync_db` | `DB_URL` |
| DB Username | `root` | `DB_USERNAME` |
| DB Password | `root` | `DB_PASSWORD` |
| JWT Secret | (default long key) | `JWT_SECRET` |
| Admin email | `admin@ktroute.com` | `SEED_ADMIN_EMAIL` |
| Admin password | `Admin@1234` | `SEED_ADMIN_PASSWORD` |

### Start the backend

```bash
cd backend
mvn spring-boot:run
```

Or with custom DB credentials:

```bash
DB_USERNAME=myuser DB_PASSWORD=mypass mvn spring-boot:run
```

The server starts at **http://localhost:8080**

**On first startup:**
- All tables are auto-created by Hibernate
- Roles (`ROLE_ADMIN`, `ROLE_PASSENGER`) are seeded automatically
- Admin account is created: `admin@ktroute.com` / `Admin@1234`

---

## 3. Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

The app starts at **http://localhost:5173**

API calls are proxied to `http://localhost:8080` via the Vite proxy config.

---

## 4. Load Sample Data

After the backend starts and creates the schema, load sample data:

```bash
mysql -u root -p routesync_db < database/sample-data.sql
```

This loads:
- 10 stations (Tamil Nadu, Karnataka, Telangana)
- 8 buses (Volvo, TATA, Ashok Leyland, Scania)
- 6 drivers
- 8 routes
- ~20 trips (mix of SCHEDULED and COMPLETED)
- Operating cost records for profitability analysis

---

## 5. Access the Application

| URL | Description |
|---|---|
| http://localhost:5173 | Main application |
| http://localhost:5173/login | Login page |
| http://localhost:5173/register | Passenger registration |
| http://localhost:5173/admin | Admin dashboard (admin only) |
| http://localhost:8080/api/auth/login | Backend API |

### Default Credentials

**Admin:** `admin@ktroute.com` / `Admin@1234`

**Passenger:** Register a new account at `/register`

---

## 6. Key Features

### Admin Panel (`/admin`)
- Dashboard with charts and KPIs
- Route, Station, Bus, Driver management (full CRUD)
- Schedule/Trip management with conflict detection
- Route demand analytics (busiest / least-travelled)
- Station boarding activity
- Capacity & occupancy reports
- Monthly demand trends
- Revenue & profitability analysis
- Downloadable CSV/PDF reports

### Passenger Portal
- Search buses by route and date
- Book seats with passenger details
- Cancel bookings (up to 2 hours before departure)
- View booking history and details

---

## 7. Project Structure

```
routesync/
├── backend/                  # Spring Boot application
│   ├── src/main/java/com/ktroute/app/
│   │   ├── config/           # SecurityConfig, AppConfig
│   │   ├── controller/       # REST controllers
│   │   ├── dto/              # Request/Response DTOs
│   │   ├── entity/           # JPA entities
│   │   ├── enums/            # Application enums
│   │   ├── exception/        # Custom exceptions + GlobalExceptionHandler
│   │   ├── repository/       # Spring Data JPA repositories
│   │   ├── reporting/        # PDF/CSV report generator
│   │   ├── security/         # JWT filter, UserDetailsService
│   │   └── service/          # Business logic services
│   └── src/main/resources/
│       └── application.properties
├── frontend/                 # React + Vite application
│   └── src/
│       ├── api/              # Axios API layer
│       ├── components/       # Reusable UI components
│       ├── context/          # AuthContext
│       └── pages/            # Page components
│           ├── admin/        # Admin pages (12 pages)
│           ├── passenger/    # Passenger pages (5 pages)
│           └── public/       # Public pages (5 pages)
└── database/
    ├── schema.sql            # DDL for all tables
    └── sample-data.sql       # Sample data for testing
```
