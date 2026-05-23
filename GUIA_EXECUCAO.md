# Guia de Execucao - Mini-Formiga

Este guia descreve como arrancar o sistema com a arquitetura definida no relatorio: o mesmo backend Spring Boot em modo `local` para a loja e em modo `central` para consolidacao.

## Pre-requisitos

- Java 21
- Maven 3.9+
- Node.js 20+
- Docker e Docker Compose, se for usado PostgreSQL via container

## Credenciais demo

Todos os utilizadores demo usam a password:

```text
MiniFormiga2026!
```

Utilizadores:

- `gestor.formiga`
- `gerente.braga`
- `operador.braga`
- `armazem.braga`

## Arranque rapido com Docker Compose

```bash
docker compose up --build
```

Servicos:

- Frontend: `http://localhost:3000`
- Backend local: `http://localhost:8080`
- Backend central: `http://localhost:8081`
- Swagger local: `http://localhost:8080/api/swagger-ui.html`
- Swagger central: `http://localhost:8081/api/swagger-ui.html`

Nota: se ja existir PostgreSQL local na porta `5432`, nao ha conflito. O container usa `5432` internamente, mas fica exposto no host em `5433`.

## Arranque manual

### 1. PostgreSQL central

```bash
docker compose up -d postgres
```

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

Noutra consola:

```bash
cd backend
SPRING_PROFILES_ACTIVE=local \
SERVER_PORT=8080 \
DB_URL=jdbc:sqlite:./mini-formiga-local.db \
CENTRAL_SYNC_URL=http://localhost:8081/api/v1/central/sincronizacao/receber \
mvn spring-boot:run
```

### 4. Frontend

Noutra consola:

```bash
cd frontend
npm ci
VITE_API_BASE_URL=http://localhost:8080/api/v1 npm run dev -- --host 0.0.0.0 --port 3000
```

Abrir `http://localhost:3000`.

## Validacao rapida por terminal

Login demo:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"gestor.formiga","password":"MiniFormiga2026!"}'
```

Swagger:

```bash
curl -I http://localhost:8080/api/swagger-ui.html
```

Testes backend:

```bash
cd backend
mvn test
```

Testes frontend:

```bash
cd frontend
npm run lint
npm run build
```

## Dados demo e dados de teste

Os dados demo sao carregados automaticamente por omissao (`DEMO_DATA_ENABLED=true`). Para arrancar sem seed demo, usar:

```bash
DEMO_DATA_ENABLED=false mvn spring-boot:run
```

Isto separa os dados de demonstracao dos cenarios de teste automatizado, que devem criar apenas os dados necessarios para cada caso.

## CORS e frontend

Quando o frontend corre numa porta diferente da API, configurar:

```bash
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000
```

No Compose isto ja fica definido para o frontend em `http://localhost:3000`.
Se abrires o Vite por `127.0.0.1` ou `0.0.0.0`, essa origem tambem tem de estar nesta lista.

## Artefactos locais

Os ficheiros SQLite (`*.db`) e logs (`logs/`, `backend/logs/`) sao artefactos locais e estao excluidos do controlo de versoes.
