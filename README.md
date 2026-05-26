# Mini-Formiga

Guia rapido para correr o projeto. Para instrucoes completas, ver tambem `GUIA_EXECUCAO.md`.

## Pre-requisitos

- Java 21
- Maven 3.9+
- Node.js 20+
- Docker e Docker Compose

Se o sistema tiver versoes antigas, este workspace tambem pode usar a toolchain local em `.tools/`:

```bash
export JAVA_HOME="$PWD/.tools/jdk-21.0.11+10"
export PATH="$JAVA_HOME/bin:$PWD/.tools/node-v20.19.5-linux-x64/bin:$PATH"
```

## Estrutura

- `backend/`: Spring Boot (perfil local com SQLite, perfil central com Postgres)
- `frontend/`: React + Vite

## Arranque rapido com Docker Compose

O Compose arranca a arquitetura usada na implementacao fisica:

- PostgreSQL central;
- backend central no perfil `central`, porta `8081`;
- backend local no perfil `local`, porta `8080`, com SQLite;
- frontend React/Vite, porta `3000`.

```bash
docker compose up --build
```

URLs principais:

- Frontend: `http://localhost:3000`
- Backend local: `http://localhost:8080`
- Backend central: `http://localhost:8081`
- Swagger local: `http://localhost:8080/api/swagger-ui.html`
- Swagger central: `http://localhost:8081/api/swagger-ui.html`

## Arranque manual

### 1. PostgreSQL central

```bash
docker compose up -d postgres
```

O PostgreSQL fica exposto no host em `localhost:5433`, para evitar conflitos com instalacoes locais na porta `5432`.

### 2. Backend central

```bash
cd backend
SPRING_PROFILES_ACTIVE=central \
SERVER_PORT=8081 \
DB_URL=jdbc:postgresql://localhost:5433/miniFormiga_central \
DB_USERNAME=miniFormiga \
DB_PASSWORD=miniFormiga_dev \
mvn spring-boot:run
```

### 3. Backend local da loja

```bash
cd backend
SPRING_PROFILES_ACTIVE=local \
SERVER_PORT=8080 \
DB_URL=jdbc:sqlite:./mini-formiga-local.db \
CENTRAL_SYNC_URL=http://localhost:8081/api/v1/central/sincronizacao/receber \
mvn spring-boot:run
```

### 4. Frontend

```bash
cd frontend
npm ci
VITE_API_BASE_URL=http://localhost:8080/api/v1 \
VITE_CENTRAL_API_BASE_URL=http://localhost:8081/api/v1 \
npm run dev -- --host 0.0.0.0 --port 3000
```

O frontend escolhe a API pelo perfil:

- Gestor da Cadeia -> backend central `http://localhost:8081/api/v1`
- Gerente, Funcionário e Responsável de Armazém -> backend local `http://localhost:8080/api/v1`

## Variaveis uteis

- `SPRING_PROFILES_ACTIVE=local|central`
- `SERVER_PORT=8080|8081`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`
- `SYNC_TOKEN`, `CENTRAL_SYNC_URL`
- `DEMO_DATA_ENABLED=true|false`
- `CORS_ALLOWED_ORIGINS=http://localhost:3000`

Existe um exemplo em `.env.example`.

## Credenciais demo

O backend cria utilizadores iniciais no arranque:

- `gestor.formiga`
- `gerente.braga`
- `operador.braga`
- `armazem.braga`
- `gestor.porto`
- `gerente.porto`
- `operador.porto`
- `armazem.porto`
- `gestor.lisboa`
- `gerente.lisboa`
- `operador.lisboa`
- `armazem.lisboa`

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
mvn clean verify
```

Validado em 2026-05-26 com Temurin JDK 21.0.11: 215 testes, 0 falhas, JaCoCo OK. Cobertura global: 87.79% linhas e 64.13% ramos.

## Testes (frontend)

```bash
cd frontend
npm run lint
npm run build
```

Validado em 2026-05-26 com Node.js 20.19.5 e npm 10.8.2.

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
