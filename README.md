# Reserve — Mood-Tracked Expense Tracker

A Java Spring Boot REST API for personal expense tracking with mood tagging and four AI-style insight features: Story, Mood, Prediction, and Gamification.

## How to run

**Requirements:** Java 17, Maven (or use the IntelliJ Maven panel)

1. Open the project in IntelliJ IDEA
2. Let Maven download dependencies (click "Load Maven Project" when prompted)
3. Run `ExpenseTrackerApplication.java` (green ▶ button)
4. Open **http://localhost:8080/swagger-ui.html** in your browser
5. Click **Authorize** (top right), register via `POST /auth/register`, paste the returned token

The app uses an **H2 in-memory database** — all data resets on every restart. No installation required.

## Endpoint reference

### Authentication — no token required
| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register` | Register a new user, returns JWT token |
| POST | `/auth/login` | Log in, returns JWT token |

### Expenses — token required
| Method | Path | Description |
|--------|------|-------------|
| POST | `/expenses` | Create a new expense |
| GET | `/expenses` | List all expenses for the logged-in user |
| GET | `/expenses/{id}` | Get a single expense by ID |
| PUT | `/expenses/{id}` | Update an expense |
| DELETE | `/expenses/{id}` | Delete an expense |

### Budgets — token required
| Method | Path | Description |
|--------|------|-------------|
| POST | `/budgets` | Create or update a monthly category budget |
| GET | `/budgets` | List all budgets for the logged-in user |

### Insights — token required, `?month=YYYY-MM` query param
| Method | Path | Description |
|--------|------|-------------|
| GET | `/insights/story` | Monthly narrative recap in plain English |
| GET | `/insights/mood` | Which emotional state drives the most spending |
| GET | `/insights/prediction` | 6-month spending forecast + what-if savings |
| GET | `/insights/gamification` | Streak detection: consecutive months under budget |

## Architecture overview

```
com.expensetracker
├── entity/          User, Expense (+ Mood enum), Budget, Currency
├── repository/      Spring Data JPA interfaces — no SQL written
├── dto/             AuthDtos, ExpenseDtos (Java records)
├── service/
│   ├── AnalyticsService     ← single source of all number-crunching
│   ├── ExpenseService       ← CRUD business logic
│   └── insight/
│       ├── InsightGenerator (interface + InsightResult record)
│       ├── StoryGenerator
│       ├── MoodInsightGenerator
│       ├── PredictionGenerator
│       └── GamificationGenerator
├── controller/      AuthController, ExpenseController, BudgetController, InsightController
├── security/        JwtService, JwtAuthFilter, SecurityConfig
├── config/          SwaggerConfig
└── exception/       GlobalExceptionHandler, ResourceNotFoundException
```

**Core design principle:** All four insight generators read from `AnalyticsService` for their raw numbers. None of them compute their own totals or query the database directly. This means aggregation logic is written and tested once.

## Running the tests

```
mvn test
```

Tests cover `AnalyticsService` with mocked repositories — no database required, runs in under a second.

## Enums reference

**Mood** (on every Expense): `HAPPY`, `STRESSED`, `NEUTRAL`, `SAD`, `EXCITED`

**Currency** (on Expense and User preferredCurrency): `USD`, `INR`
