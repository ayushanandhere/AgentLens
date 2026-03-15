# AgentLens Dashboard

Operator UI for AgentLens.

## Run

Start the backend first with the `local` profile so the dashboard has demo traffic and the local operator key:

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Then run the dashboard:

```bash
npm install
npm run dev
```

The dashboard expects:

- UI: `http://localhost:5173`
- API: `http://localhost:8080`
- Operator key: `operator-local-key`

## Environment

Copy `.env.example` if you want to override the defaults:

- `VITE_API_BASE_URL`
- `VITE_OPERATOR_API_KEY`
- `VITE_APPROVER_ID`

## Commands

```bash
npm test
npm run build
```
