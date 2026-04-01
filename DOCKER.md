# Docker profiles for mVPN

This repo now has separate Compose files for local development and production:

- `compose.dev.yml`
- `compose.prod.yml`

## 1) Local development

1. Copy `.env.example` to `.env.dev`
2. Fill local values
3. Run stack

```bash
docker compose --env-file .env.dev -f compose.dev.yml up -d --build
```

Stop:

```bash
docker compose --env-file .env.dev -f compose.dev.yml down
```

## 2) Production

1. Copy `.env.example` to `.env.prod` on server
2. Fill production secrets
3. Run stack

```bash
docker compose --env-file .env.prod -f compose.prod.yml up -d --build
```

Stop:

```bash
docker compose --env-file .env.prod -f compose.prod.yml down
```