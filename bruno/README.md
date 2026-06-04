# WS Fitness — Bruno Collection

API collection for the gym-flow-backend, with success and error scenarios for
every endpoint. Built for [Bruno](https://www.usebruno.com/).

## How to use

1. Start the backend (`./mvnw spring-boot:run`, default `http://localhost:8080`,
   context path `/api/v1`). A Postgres + Flyway migrations (incl. the bootstrap
   admin from `V8`) must be available.
2. Open this folder in Bruno (**Open Collection** → `gym-flow-backend/bruno`).
3. Select the **Local** environment (top-right).
4. Run the **Auth** requests in order first — they bootstrap the session:
   - `01 Consume Bootstrap Invite` sets the admin password (status → ACTIVE).
   - `02 Login` captures `accessToken` / `refreshToken` / `adminId` into the
     environment; every protected request reuses them.
5. From there, run the other folders. Requests that create resources
   (`Register Student`, `Register Exercise`, `Create Training`, …) capture the
   new id into an environment variable so later requests can reference it. Run
   each folder roughly top-to-bottom for the happy path.

## Authentication model

Protected endpoints require **both**:

- `Authorization: Bearer {{accessToken}}` — validated by the security filter;
- `X-User-Id` / `X-User-Role` — read directly by the controllers to identify the
  acting user (no filter derives them from the JWT in this build).

The collection sends both. Management requests act as the bootstrap
ADMINISTRATOR; student-context requests act as the seeded student.

## Naming convention

Each folder maps to a controller. Request names carry the expected outcome and
HTTP status, e.g. `Register Student - email taken (409)`. Every request asserts
its expected status via Bruno `assert`.

## Variables (Local environment)

| var | meaning |
|-----|---------|
| `baseUrl` | `http://localhost:8080/api/v1` |
| `bootstrapInvite` | static first-access token seeded by `V8` |
| `adminEmail` / `adminPassword` / `adminId` | bootstrap administrator |
| `accessToken` / `refreshToken` | captured at login (secret) |
| `studentId` / `instructorId` / `exerciseId` / `trainingId` / `executionId` / `bondId` | captured from create responses (secret) |
| `studentInviteToken` | invite emitted when registering a student (see note) |

> Note: invites for registered students are delivered by e-mail
> (`UserRegistered` → invite). Against a local SMTP (e.g. Mailpit) read the token
> from the message and set `studentInviteToken` to exercise that student's
> first-access + login.
