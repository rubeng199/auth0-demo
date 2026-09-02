# TeamBoard

A multi-tenant B2B demo on **Auth0 Organizations**: a React/TypeScript SPA calling a
Java/Spring resource server.

The application surface is deliberately thin — a per-organization list of projects — so the
focus stays on the authentication and authorization design:

- **Universal Login + Organizations** — each customer is an Auth0 Organization
- **Org-scoped RBAC** — the same user is `admin` in one organization and `member` in another
- **Full token flow** — the SPA obtains an access token; the API validates it and scopes all
  data by it

## Architecture

Responsibilities are asymmetric:

| | React SPA | Spring API |
|---|---|---|
| Redirects to Auth0 | owns the login flow | never |
| Holds an access token | yes | receives one per request |
| Validates tokens | no | signature, issuer, audience, `org_id` |
| Client secret | public client, has none | not needed |
| Enforces roles | hides controls only | enforced via `@PreAuthorize` |

The two halves are registered separately in Auth0: the SPA is an **Application** (Client ID),
the backend is an **API** (audience identifier).

### Design decisions

- **The client never names its own tenant.** No endpoint accepts an org id as a parameter,
  header or body field; `org_id` is read from the signed token only. If the client could
  choose its tenant, every authorization decision would be forgeable.
- **`OrgIdValidator` runs at the token layer, not in controllers.** A token from an
  organization this API does not serve fails during validation with a 401, before any handler
  runs. Auth0 requires `org_id` to be checked against a known set rather than trusted on
  arrival.
- **Switching organizations is a re-authentication.** Access tokens are issued for a single
  organization, so the org switcher calls
  `loginWithRedirect({ authorizationParams: { organization } })` rather than mutating client
  state.
- **Roles arrive via a post-login Action.** `org_id` is included in tokens by default; roles
  are not. A post-login Action copies them into a namespaced claim, which the
  `JwtAuthenticationConverter` in `SecurityConfig` maps to `ROLE_*` authorities.
- **Storage is in-memory and resets when the API restarts.** There is no database. The demo
  exists to show the auth model, not CRUD or persistence, so `ProjectRepository` just holds a
  map seeded at startup. Projects created during a session survive switching organizations,
  but not a restart. Swapping the repository for Spring Data JPA would not affect the layers
  above it.

## Prerequisites

- JDK 21+
- Node 20.19+ or 22.12+

## Setup

### Auth0 tenant

The demo expects a tenant containing the following. Identifiers are yours to choose except
where noted as matched literally by the code.

| Object | Requirement |
|---|---|
| **API** | Any identifier — this is the audience. Enable RBAC. Under *Application Access Policy*, set **User-Delegated Access** to *All apps allowed* (or authorize the SPA explicitly), otherwise login fails at `/authorize`. |
| **Application** | Single Page Application, with callback, logout and web origin URLs set to `http://localhost:5173`. Under *Login Experience*, select **Business Users** and **Prompt for Organization**. |
| **Organizations** | Two, each with a database connection enabled. Their display labels live in `frontend/src/config.ts`. |
| **Roles** | Named `admin` and `member` — matched literally by `@PreAuthorize` and the UI. |
| **User** | A member of **both** organizations, holding `admin` in one and `member` in the other. |
| **Action** | Post-login, deployed *and* added to the login flow. |

Auth0 does not include roles in tokens by default. Without this Action the API sees no
roles and denies every write:

```javascript
exports.onExecutePostLogin = async (event, api) => {
  const ns = 'https://teamboard.demo'; // must match teamboard.claim-namespace
  if (event.authorization) {
    api.idToken.setCustomClaim(`${ns}/roles`, event.authorization.roles);
    api.accessToken.setCustomClaim(`${ns}/roles`, event.authorization.roles);
  }
};
```

### Configuration

Both config files are gitignored; copy the committed examples and fill them in.

```bash
cp backend/src/main/resources/application.example.yml \
   backend/src/main/resources/application.yml
cp frontend/.env.example frontend/.env.local
```

| File | Values |
|---|---|
| `application.yml` | `issuer-uri` (`https://<tenant>.us.auth0.com/` — trailing slash required), `audiences` (the API identifier), `teamboard.allowed-org-ids` (both organization IDs) |
| `.env.local` | tenant domain, SPA client ID, audience, and the two organization IDs |

None of these are secrets — the SPA is a public client and ships its client ID and
organization IDs to the browser. They are kept out of version control because they are
specific to one tenant, not because they are sensitive.

## Run

```bash
# API on :8080
cd backend && ./mvnw spring-boot:run

# SPA on :5173
cd frontend && npm install && npm run dev
```

Open http://localhost:5173.

## Tests

```bash
cd backend && ./mvnw test
```

The suite runs offline: `JwtDecoder` is mocked and authentication is injected directly, so it
exercises the authorization rules rather than token parsing.

- anonymous requests are rejected with 401
- a member can read, but is denied create with 403
- an admin can create within their own organization
- an admin of one organization deleting another's project gets 404, and the project survives
- `OrgIdValidator` rejects unknown organizations and tokens without `org_id`

## Possible extensions

- Organization members and invitations via the Auth0 Management API (requires an M2M
  application)
- Fine-grained `permissions`/scopes instead of coarse roles
- Persistence (H2 or Postgres) behind the existing repository
