# Mini-Formiga

Guia rapido para correr o projeto (backend + frontend) e comandos principais.

## Pre-requisitos

- Java 17+
- Maven 3.9+
- Node.js 18+ (ou 20+)
- Docker (opcional, para Postgres)

## Estrutura

- `backend/`: Spring Boot (perfil local com SQLite, perfil central com Postgres)
- `frontend/`: React + Vite

## Backend (Spring Boot)

### Perfil local (SQLite, porta 8080)

```bash
cd backend
mvn spring-boot:run
```

### Perfil central (Postgres, porta 8081)

```bash
cd backend
SPRING_PROFILES_ACTIVE=central mvn spring-boot:run
```

### Variaveis uteis (opcionais)

- `SPRING_PROFILES_ACTIVE=local|central`
- `SERVER_PORT=8080|8081`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (para o perfil central)
- `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`
- `SYNC_TOKEN`, `CENTRAL_SYNC_URL`

As defaults estao em:
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-local.properties`
- `backend/src/main/resources/application-central.properties`

## Base de dados (Postgres para perfil central)

```bash
docker compose up -d postgres
```

A base de dados local (perfil `local`) usa SQLite em `./mini-formiga-local.db`.

## Frontend (Vite)

```bash
cd frontend
npm install
npm run dev
```

- Frontend: http://localhost:3000
- Backend local: http://localhost:8080
- Backend central: http://localhost:8081

Nota: o Vite faz proxy de `/api` para `http://localhost:8080` (ver `frontend/vite.config.ts`).

## Docker Compose (opcional)

O `docker-compose.yml` sobe apenas:
- Postgres (para o perfil `central`)
- Frontend (dev server)

```bash
docker compose up -d
```

Se usares o frontend via Docker, o proxy para `localhost:8080` aponta para o container e nao para o host.
Nesse caso, ou corres o frontend localmente com `npm run dev`, ou ajustas o proxy/base URL.

## Credenciais demo

O backend cria utilizadores iniciais no arranque:

- `gestor.formiga`
- `gerente.braga`
- `operador.braga`
- `armazem.braga`

Password (para todos):

```
MiniFormiga2026!
```

## Swagger / API Docs

- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/docs`

## Testes (backend)

```bash
cd backend
mvn test
```

## Limpeza rapida

```bash
# parar containers
docker compose down

# limpar cache e deps do frontend
cd frontend
rm -rf node_modules

# limpar build do backend
cd ../backend
mvn clean
```
