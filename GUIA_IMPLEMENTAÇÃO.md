# PROMPT DE DESENVOLVIMENTO — SISTEMA MINI-FORMIGA

> **Instruções de utilização:** Este prompt destina-se a guiar um agente LLM na implementação completa do sistema Mini-Formiga. Deve ser fornecido integralmente no início da sessão de desenvolvimento. Trabalha secção a secção, confirmando cada entrega antes de avançar para a seguinte.

---

## 1. CONTEXTO E MISSÃO

Tens de implementar o sistema **Mini-Formiga**, uma plataforma de gestão integrada para uma cadeia de lojas de conveniência portuguesa. O cliente é o Sr. Manuel Joaquim Formiga, proprietário de uma cadeia em expansão. O sistema substitui registos manuais (caderno de caixa, ficheiros .xlsx dispersos, agendas de papel) por um sistema centralizado e fiável.

### 1.1 Objetivos do sistema

- Centralizar dados de todas as lojas num servidor central
- Permitir operação autónoma (offline) de cada loja durante o dia
- Sincronização automática fim de jornada entre lojas e servidor central
- Terminal PDV intuitivo para operadores de caixa
- Controlo de stock em tempo real com alertas automáticos
- Relatórios de desempenho e rentabilidade exportáveis
- Conformidade com legislação fiscal portuguesa (AT) e RGPD

### 1.2 Âmbito da implementação

Implementar todos os módulos identificados como **Must Have** e **Should Have** no documento SRS, seguindo as decisões arquiteturais documentadas. O sistema deve ser funcional, testável e pronto para integração futura de módulos Could Have.

---

## 2. STACK TECNOLÓGICA

Utiliza exclusivamente as tecnologias seguintes. Não introduzas dependências não listadas sem justificação explícita.

### Backend
- **Linguagem:** Java 21 (LTS)
- **Framework:** Spring Boot 3.x
- **API:** REST com serialização JSON (Jackson)
- **Segurança:** Spring Security + JWT (tokens stateless)
- **Persistência:** Spring Data JPA + Hibernate
- **Base de dados local (por loja):** SQLite (operação offline) ou H2 (desenvolvimento/teste)
- **Base de dados central:** PostgreSQL 15+
- **Sincronização:** mecanismo batch assíncrono customizado (não usar Kafka nem RabbitMQ)
- **Testes:** JUnit 5 + Mockito + JaCoCo (cobertura) + PIT (mutation testing)
- **Build:** Maven 3.9+
- **Identificadores:** UUID v4 em todas as entidades

### Frontend
- **Framework:** React 18 + TypeScript
- **UI Library:** shadcn/ui + TailwindCSS
- **Estado:** React Query (server state) + Zustand (client state)
- **Routing:** React Router v6
- **PDV:** interface otimizada para touch/teclado, suporte offline via Service Worker + IndexedDB
- **Gráficos (dashboard):** Recharts
- **Build:** Vite

### Infraestrutura de desenvolvimento
- **Containerização:** Docker + Docker Compose
- **Documentação API:** OpenAPI 3.0 (Springdoc)

---

## 3. ARQUITETURA DO SISTEMA

### 3.1 Visão geral

Arquitetura **cliente-servidor de 3 camadas** com operação híbrida online/offline:

```
┌─────────────────────────────────────────────────────┐
│                   CAMADA DE APRESENTAÇÃO             │
│  PDVUI | GerenteUI | GestorUI | ArmazemUI            │
│              (React + TypeScript)                    │
└──────────────────────┬──────────────────────────────┘
                       │ REST/JSON via IMiniFormigaLN
┌──────────────────────▼──────────────────────────────┐
│              CAMADA DE LÓGICA DE NEGÓCIO             │
│               MiniFormigaFacade                      │
│   SubPDV | SubStock | SubUtilizadores                │
│   SubEncomendas | SubSincronizacao                   │
│              (Spring Boot — servidor local)          │
└──────────────────────┬──────────────────────────────┘
                       │ JPA/Hibernate
┌──────────────────────▼──────────────────────────────┐
│                  CAMADA DE DADOS                     │
│  BD Local (SQLite/H2) ◄──► Servidor Central (PgSQL) │
│         Sincronização TLS 1.3 — fim de jornada      │
└─────────────────────────────────────────────────────┘
```

### 3.2 Decisões arquiteturais obrigatórias (não negociáveis)

| ID | Decisão | Justificação |
|----|---------|-------------|
| DA-01 | Padrão **Facade por subsistema** (não microserviços) | Simplicidade operacional para PME; subsistemas substituíveis via interfaces |
| DA-02 | **Sincronização em lote** no fim de jornada (não tempo real) | Resiliência offline; alinhamento com modelo de negócio |
| DA-03 | **REST API + JSON** (não gRPC) | Facilidade de debug, manutenção e integração |
| DA-04 | **UUID v4** como chave primária em todas as entidades | Evita colisões em operação distribuída offline |
| DA-05 | **Last-write-wins** para resolução de conflitos de sincronização | Continuidade operacional prioritária; conflitos registados para auditoria |
| DA-06 | **Optimistic locking** com campo `@Version` em entidades | Evita bloqueios em cascata; adequado à frequência de conflitos esperada |
| DA-07 | **Lógica de negócio descentralizada** nas classes de domínio | Encapsulamento OO; testabilidade unitária; alinhamento com DDD |

### 3.3 Estrutura de pacotes Java

```
pt.miniFormiga
├── facade/                    # MiniFormigaFacade + interfaces
├── modules/
│   ├── pdv/                   # SubPDV: Venda, LinhaVenda, Fatura, FechoCaixa
│   ├── stock/                 # SubStock: Stock, NivelMinimo, AlertaStock, Inventario
│   ├── utilizadores/          # SubUtilizadores: Utilizador, Perfil, Autenticação
│   ├── encomendas/            # SubEncomendas: Encomenda, Fornecedor, EntradaMercadoria
│   └── sincronizacao/         # SubSincronizacao: Sincronizacao, EstadoSincronizacao
├── domain/                    # Entidades JPA partilhadas (Loja, Produto, Categoria...)
├── audit/                     # LogAuditoria, TipoOperacao, AuditoriaFacade
├── config/                    # Spring configs: Security, JPA, OpenAPI
├── api/                       # Controllers REST
│   ├── auth/
│   ├── vendas/
│   ├── stock/
│   ├── encomendas/
│   ├── utilizadores/
│   ├── sincronizacao/
│   └── relatorios/
└── sync/                      # Motor de sincronização offline→central
```

---

## 4. MODELO DE DOMÍNIO — ENTIDADES

Implementa as 30 entidades seguintes como classes JPA com UUID v4, `@Version` para optimistic locking, e timestamps `createdAt`/`updatedAt` automáticos onde aplicável. **Implementa a lógica de negócio diretamente nas entidades** (Rich Domain Model).

### 4.1 Entidades do núcleo operacional

#### `Loja`
```java
- UUID id
- String nome
- String morada
- String nif
- boolean ativa
// Relações: tem muitos Utilizador, Stock, Venda, FechoCaixa, Encomenda, Sincronizacao
```

#### `Utilizador`
```java
- UUID id
- String username (único)
- String passwordHash (BCrypt)
- String nome
- boolean ativo
- Perfil perfil (ManyToOne)
- Loja loja (ManyToOne)
// Lógica: boolean temPermissao(String modulo)
```

#### `Perfil`
Enum ou entidade com valores: `GESTOR`, `GERENTE`, `FUNCIONARIO`, `RESPONSAVEL_ARMAZEM`
```java
- UUID id
- String nome
- List<String> permissoes  // ex: ["PDV_READ","PDV_WRITE","STOCK_READ",...]
```

#### `Produto`
```java
- UUID id
- String codigo (EAN-13 ou interno, único)
- String nome
- BigDecimal precoVenda
- BigDecimal precoCusto
- TaxaIVA taxaIVA (ManyToOne)
- Categoria categoria (ManyToOne)
- boolean ativo
// Lógica: BigDecimal calcularMargem() { return precoVenda.subtract(precoCusto); }
//         BigDecimal calcularMargemPercentagem()
//         BigDecimal calcularPrecoComIVA()
```

#### `Categoria`
```java
- UUID id
- String nome
- String descricao
```

#### `TaxaIVA`
```java
- UUID id
- String descricao  // "Taxa Reduzida", "Taxa Intermédia", "Taxa Normal"
- BigDecimal percentagem  // 6, 13, 23 (conforme RD-01)
```

#### `Fornecedor`
```java
- UUID id
- String nome
- String nif
- String email
- String telefone
- boolean ativo
- TimeRange horarioArmazem  // seg-sex 08:00-18:00 (RD-06)
// Lógica: boolean estaDisponivel(LocalDateTime momento)
//         LocalDateTime calcularDataProcessamento(LocalDateTime submissao)
```

#### `CondicaoComercial`
```java
- UUID id
- Fornecedor fornecedor
- Produto produto
- BigDecimal precoUnitario
- int prazoEntregaDias
- int quantidadeMinima
```

### 4.2 Entidades de stock e inventário

#### `Stock`
```java
- UUID id
- Produto produto
- Loja loja
- int quantidadeDisponivel
- @Version long version  // optimistic locking
// Lógica: boolean estaBaixoMinimo() — delega em NivelMinimo associado
//         void subtrair(int qtd) throws StockInsuficienteException  (RD-04)
//         void adicionar(int qtd)
```

#### `NivelMinimo`
```java
- UUID id
- Stock stock (OneToOne)
- int quantidade
```

#### `AlertaStock`
```java
- UUID id
- NivelMinimo nivelMinimo
- LocalDateTime dataEmissao
- boolean lido
- List<Utilizador> destinatarios
```

#### `LocalizacaoProduto`
```java
- UUID id
- Produto produto (OneToOne)
- String corredor
- String prateleira
```

#### `AjusteInventario`
```java
- UUID id
- Stock stock
- int quantidade  // positivo = entrada, negativo = saída
- MotivoAjuste motivo
- Utilizador responsavel
- String observacoes
- LocalDateTime dataHora
```

#### `MotivoAjuste`
Enum: `QUEBRA`, `DESPERDICIO`, `CORRECAO_ERRO`, `OUTRO`

#### `InventarioFisico`
```java
- UUID id
- Loja loja
- Utilizador responsavel
- LocalDateTime dataInicio
- LocalDateTime dataFecho
- StatusInventario status  // EM_CURSO, FECHADO
- List<LinhaInventario> linhas (OneToMany, composição)
```

#### `LinhaInventario`
```java
- UUID id
- InventarioFisico inventario
- Produto produto
- int quantidadeSistema
- int quantidadeContada
// Lógica: int calcularDiscrepancia() { return quantidadeContada - quantidadeSistema; }
//         boolean temDiscrepancia()
```

### 4.3 Entidades de PDV e transações

#### `Venda`
```java
- UUID id
- Loja loja
- Utilizador operador
- LocalDateTime dataHora
- MeioPagamento meioPagamento
- StatusVenda status  // ABERTA, FINALIZADA, ANULADA, DEVOLVIDA
- List<LinhaVenda> linhas (OneToMany, composição)
- @Version long version
// Lógica: BigDecimal calcularSubtotal()
//         BigDecimal calcularTotalIVA()
//         BigDecimal calcularTotal()
//         Map<TaxaIVA, BigDecimal> calcularIVAPorTaxa()
//         boolean podeSerAnulada()
```

#### `LinhaVenda`
```java
- UUID id
- Venda venda
- Produto produto
- int quantidade
- BigDecimal precoUnitario  // snapshot do preço no momento da venda
- BigDecimal taxaIVAPercentagem  // snapshot da taxa
// Lógica: BigDecimal calcularSubtotal()
//         BigDecimal calcularIVA()
//         BigDecimal calcularTotal()
```

#### `MeioPagamento`
Enum: `NUMERARIO`, `CARTAO`, `MB_WAY`

#### `Fatura`
```java
- UUID id
- Venda venda (OneToOne)
- String numeroSequencial  // série + número, ex: "A/2025/0001" (RD-03)
- TipoFatura tipo  // SIMPLIFICADA, COMPLETA (RD-02)
- String nifCliente  // nullable para simplificada
- String nomeCliente  // nullable
- LocalDateTime dataEmissao
- BigDecimal totalSemIVA
- BigDecimal totalIVA
- BigDecimal totalComIVA
```

#### `FechoCaixa`
```java
- UUID id
- Loja loja
- Utilizador responsavel
- LocalDate data
- BigDecimal totalNumerario
- BigDecimal totalCartao
- BigDecimal totalMbWay
- BigDecimal totalGeral
- int numeroTransacoes
- String observacoes  // para registar discrepâncias
- LocalDateTime dataHoraRegisto
// Lógica: BigDecimal calcularTotais(List<Venda> vendas)
```

### 4.4 Entidades de fornecedores e encomendas

#### `Encomenda`
```java
- UUID id
- Loja loja
- Fornecedor fornecedor
- LocalDateTime dataSubmissao
- LocalDateTime dataEsperadaProcessamento
- List<EstadoEncomenda> historicoEstados (OneToMany)
- List<LinhaEncomenda> linhas (OneToMany, composição)
// Lógica: EstadoEncomenda getEstadoAtual()
//         boolean podeSerCancelada()
```

#### `EstadoEncomenda`
```java
- UUID id
- Encomenda encomenda
- StatusEncomenda status  // PENDENTE, ENVIADA, RECEBIDA, CANCELADA
- LocalDateTime dataTransicao
- Utilizador responsavel
- String observacoes
```

#### `LinhaEncomenda`
```java
- UUID id
- Encomenda encomenda
- Produto produto
- int quantidadeEncomendada
- int quantidadeRecebida  // preenchido na receção
```

#### `GuiaRemessa`
```java
- UUID id
- Encomenda encomenda
- String numeroGuia
- LocalDate dataEntrega
- Fornecedor fornecedor
```

#### `EntradaMercadoria`
```java
- UUID id
- GuiaRemessa guiaRemessa
- Loja loja
- Utilizador responsavel
- LocalDateTime dataRegisto
- List<LinhaEntradaMercadoria> linhas
```

#### `LinhaEntradaMercadoria`
```java
- UUID id
- EntradaMercadoria entrada
- Produto produto
- int quantidadeRecebida
- boolean temDiscrepancia
```

### 4.5 Entidades transversais

#### `LogAuditoria`
```java
- UUID id
- Utilizador utilizador
- TipoOperacao tipoOperacao
- LocalDateTime dataHora
- String descricao
- String entidadeAfetada  // ex: "Venda"
- UUID entidadeId
- String dadosAntes  // JSON snapshot (nullable)
- String dadosDepois  // JSON snapshot (nullable)
// IMUTÁVEL: sem setters, apenas constructor
```

#### `TipoOperacao`
Enum: `LOGIN`, `LOGOUT`, `VENDA_REGISTADA`, `VENDA_ANULADA`, `FATURA_EMITIDA`, `DEVOLUCAO_REGISTADA`, `FECHO_CAIXA`, `AJUSTE_INVENTARIO`, `ENCOMENDA_CRIADA`, `UTILIZADOR_CRIADO`, `UTILIZADOR_EDITADO`, `UTILIZADOR_DESATIVADO`, `SINCRONIZACAO_INICIADA`, `SINCRONIZACAO_CONCLUIDA`

#### `Sincronizacao`
```java
- UUID id
- Loja loja
- LocalDateTime dataHoraInicio
- LocalDateTime dataHoraFim
- List<EstadoSincronizacao> historico
- int registosEnviados
- int conflitosDetectados
```

#### `EstadoSincronizacao`
```java
- UUID id
- Sincronizacao sincronizacao
- StatusSincronizacao status  // PENDENTE, EM_CURSO, CONCLUIDA, COM_CONFLITOS, FALHOU
- LocalDateTime dataTransicao
- String detalhes
```

---

## 5. REQUISITOS FUNCIONAIS — ESPECIFICAÇÃO DE IMPLEMENTAÇÃO

Implementa cada RF com controller, service (facade), repositório e testes unitários.

### RF-01 | Dashboard consolidado
- Endpoint: `GET /dashboard?lojaId=&periodo=&categoria=`
- Agrega: vendas totais, margem média, nº transações, alertas ativos, top produtos
- Suporta filtros por loja (ou todas), período (dia/semana/mês/custom), categoria
- Dados provenientes do servidor central (pós-sincronização)

### RF-02 | Relatórios exportáveis
- Endpoints: `GET /relatorios/vendas`, `GET /relatorios/rentabilidade`, `GET /relatorios/stock`
- `POST /relatorios/exportar` — gera ficheiro PDF (Apache PDFBox ou iText) ou CSV
- CSV: campos obrigatórios — data, descrição, valor, IVA, loja (RNF-11)

### RF-03 | Gestão de fornecedores e encomendas consolidadas
- CRUD completo de Fornecedor com CondicaoComercial por produto
- Encomenda pode consolidar produtos de múltiplas lojas
- Sugestões automáticas de reposição baseadas em AlertaStock ativos

### RF-04 | Interface responsiva (Could Have — implementar se tempo permitir)

### RF-05 | Monitorização de stock e alertas automáticos
- Trigger: após cada `Venda.finalizar()` ou `AjusteInventario`, verificar `Stock.estaBaixoMinimo()`
- Se sim: criar `AlertaStock` e notificar utilizadores com perfil GERENTE e GESTOR via SSE (Server-Sent Events) ou polling
- **O alerta deve ser emitido no momento exato da transação** (não em batch)

### RF-06 | Registo de entrada de mercadoria
- Associar à GuiaRemessa e Encomenda correspondente
- Atualizar Stock automaticamente após confirmação
- Detetar e registar discrepâncias entre encomendado e recebido
- Atualizar EstadoEncomenda para RECEBIDA

### RF-07 | Listagem de stock por loja
- `GET /stock?lojaId=` — devolve todos os produtos com quantidade atual
- Destacar (campo `alertaAtivo: true`) produtos abaixo do NivelMinimo

### RF-08 | Fecho de caixa diário automático
- Totaliza automaticamente todas as Vendas com status FINALIZADA do dia
- Agrupa por MeioPagamento
- Permite registar observação de discrepância (RNF-05: registar no log)
- Agenda sincronização após confirmação (UC-13)

### RF-09 | Relatórios de vendas por produto/turno/período
- Filtros: produtoId, turno (manhã/tarde/noite por intervalo de horas), dataInicio, dataFim
- Retornar dados agrupados com totais

### RF-10 | Gestão de utilizadores
- CRUD de Utilizador pelo Gerente (restrito à sua loja)
- Definir Perfil por utilizador
- Desativação lógica (nunca eliminar fisicamente)
- Registar todas as operações no LogAuditoria

### RF-11 | Ajustes manuais de inventário
- Permitir ajuste positivo e negativo
- **Impedir ajuste negativo que resulte em stock negativo** (mesma regra de RD-04)
- Obrigar preenchimento de MotivoAjuste
- Registar no LogAuditoria com snapshot do stock antes e depois

### RF-12 | Terminal PDV
- Registo de produto por código EAN ou pesquisa por nome
- Adicionar/remover LinhaVenda antes de finalizar
- Apresentar total com IVA discriminado por taxa
- Resposta < 2 segundos (RNF-01)
- **Modo offline**: persistir localmente via Service Worker + IndexedDB no frontend, sincronizar ao recuperar ligação

### RF-13 | Processamento de pagamentos
- Suportar NUMERARIO, CARTAO, MB_WAY
- Registar MeioPagamento na Venda
- Para numerário: calcular troco (valor pago - total)

### RF-14 | Emissão de recibos e faturas
- **Fatura simplificada**: transações ≤ 1.000€ sem NIF do cliente (RD-02)
- **Fatura completa**: valor > 1.000€ ou quando cliente fornece NIF
- Numeração sequencial por série, única e ininterrupta (RD-03)
- IVA discriminado por taxa (RD-01)
- Emissão em PDF (download/impressão)

### RF-15 | Remoção de linhas de venda
- Permitir remover LinhaVenda enquanto Venda.status == ABERTA
- Recalcular total após remoção
- Não registar no log (operação pré-transação)

### RF-16 | Devoluções
- Pesquisar Venda original por ID ou data
- Selecionar produto e quantidade a devolver
- Reverter impacto financeiro (criar nota de crédito)
- Repor Stock na loja
- Registar no LogAuditoria

### RF-17 | Modo offline e sincronização
- Frontend: Service Worker intercepta chamadas API; IndexedDB armazena transações locais
- Backend: `SincronizacaoFacade.iniciarSincronizacao(lojaId)` envia dados pendentes
- Conflitos resolvidos por **last-write-wins** baseado em `updatedAt`
- Conflitos registados em `EstadoSincronizacao.detalhes` e notificados ao GESTOR

### RF-18 | Localização de produtos no armazém (Could Have)

### RF-19 | Inventário físico
- Gerar listagem de todos os produtos da loja
- Registar contagem por produto
- Calcular discrepância automaticamente (`LinhaInventario.calcularDiscrepancia()`)
- Fechar inventário e disponibilizar discrepâncias ao Gerente para correção via RF-11

---

## 6. REQUISITOS NÃO FUNCIONAIS — IMPLEMENTAÇÃO

### RNF-01 | Performance PDV < 2 segundos
- Usar índices na BD: `produto.codigo`, `stock(produto_id, loja_id)`
- Cache de produtos em memória (Caffeine) no servidor de loja
- Endpoints PDV devem ter SLA de 2s verificado em testes de integração

### RNF-02 | Disponibilidade local ≥ 99,5%
- Toda a lógica PDV deve funcionar sem ligação ao servidor central
- Frontend usa Service Worker para operação offline
- Backend local (por loja) não depende do servidor central para operações do dia

### RNF-03 | Sincronização automática diária
- Trigger: após `FechoCaixa` confirmado (UC-05)
- Executar `SincronizacaoFacade.iniciarSincronizacao()` de forma assíncrona
- Registar `EstadoSincronizacao` com resultado

### RNF-04 | Autenticação e controlo de acessos
- JWT com expiração de 8 horas (jornada de trabalho)
- Refresh token com expiração de 24 horas
- `@PreAuthorize("hasRole('GERENTE') or hasRole('GESTOR')")` nos endpoints sensíveis
- Bloquear conta após 5 tentativas falhadas (desbloquear só manualmente pelo Gerente)

### RNF-05 | Log de auditoria imutável
- `LogAuditoria` sem métodos de update/delete
- AuditoriaFacade injetado transversalmente via Spring AOP (`@AfterReturning` em operações financeiras)
- Campos obrigatórios: utilizadorId, tipoOperacao, dataHora, entidadeAfetada, entidadeId

### RNF-06 | Comunicação cifrada TLS 1.3
- Configurar HTTPS com TLS 1.3 no servidor central (Spring Security + certificado)
- Forçar redirecionamento HTTP → HTTPS
- Configurar `application.properties` com `server.ssl.*`

### RNF-07 | Conformidade RGPD
- Dados pessoais de clientes (NIF, nome) apenas presentes na Fatura
- Sem dados pessoais de clientes noutras entidades
- Endpoint de eliminação de dados de cliente: `DELETE /faturas/cliente/{nif}/dados`
- Documentar no README o processo de eliminação de dados

### RNF-08 | Documentos fiscais conformes
- Série de faturação configurável (ex: "A/2025/")
- Numeração sequencial gerida por contador atómico em BD (evitar gaps com pessimistic lock APENAS para numeração de faturas)
- Formato: `{série}/{ano}/{número com 4 dígitos}`

### RNF-09 | Escalabilidade
- Toda a configuração de loja por ID (nunca hardcoded)
- Nova loja = nova instância do backend com BD própria + registo no servidor central

### RNF-10 | Usabilidade PDV
- Interface PDV com máximo 3 cliques/teclas para registar produto
- Suporte a leitor de código de barras (input focus automático)
- Atalhos de teclado documentados: Enter=adicionar, Delete=remover linha, F10=finalizar
- Feedback visual imediato (loading states, confirmações)

### RNF-11 | Exportação contabilística
- CSV com campos: `data;descricao;valor_sem_iva;iva;valor_com_iva;loja`
- Encoding UTF-8 com BOM (compatibilidade Excel)
- XLSX alternativo via Apache POI

---

## 7. REQUISITOS DE DOMÍNIO — REGRAS DE NEGÓCIO

Estas regras são **obrigatórias** e devem ser implementadas como validações nas entidades/serviços:

### RD-01 | Taxas de IVA
```java
// Ao criar/editar Produto, validar:
assert Arrays.asList(new BigDecimal("6"), new BigDecimal("13"), new BigDecimal("23"))
    .contains(produto.getTaxaIVA().getPercentagem());
```

### RD-02 | Tipo de fatura
```java
// Em FaturaService.emitir():
if (venda.calcularTotal().compareTo(new BigDecimal("1000")) > 0 || nifCliente != null) {
    tipo = TipoFatura.COMPLETA;
} else {
    tipo = TipoFatura.SIMPLIFICADA;
}
```

### RD-03 | Numeração sequencial de faturas
- Implementar `FaturaSerieService` com `synchronized` ou `@Lock(LockModeType.PESSIMISTIC_WRITE)` para garantir sequência sem gaps

### RD-04 | Proibir stock negativo
```java
// Em Stock.subtrair():
if (this.quantidadeDisponivel - qtd < 0) {
    throw new StockInsuficienteException(
        "Stock insuficiente para " + produto.getNome() + ". Disponível: " + quantidadeDisponivel
    );
}
```

### RD-05 | Transações fora de horário
- Horário normal: Segunda-Sábado, 07:30-20:00
- Venda fora deste horário: `Venda.foraDeHorario = true`
- Registar no LogAuditoria com tipo `OPERACAO_FORA_HORARIO`
- Não bloquear (apenas sinalizar)

### RD-06 | Processamento de encomendas
```java
// Em EncomendaService.calcularDataProcessamento():
LocalDateTime horarioFim = dataSubmissao.toLocalDate()
    .atTime(18, 0);
boolean dentroHorario = dataSubmissao.getDayOfWeek().getValue() <= 5  // seg-sex
    && dataSubmissao.toLocalTime().isAfter(LocalTime.of(8, 0))
    && dataSubmissao.isBefore(horarioFim);

if (!dentroHorario) {
    // Calcular próximo dia útil às 08:00
    return calcularProximoDiaUtil(dataSubmissao).atTime(8, 0);
}
return dataSubmissao;  // processamento imediato
```

### RD-07 | Dados mínimos RGPD na fatura
- Fatura simplificada: sem dados do cliente
- Fatura completa: apenas NIF e nome (campos obrigatórios por lei)
- Sem email, morada ou outros dados dispensáveis

---

## 8. ESPECIFICAÇÃO DA API REST

Todos os endpoints requerem header `Authorization: Bearer {jwt}`, exceto `/auth/login`.

Formato de resposta de erro:
```json
{
  "timestamp": "2025-04-24T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Stock insuficiente para produto X. Disponível: 3",
  "path": "/api/vendas"
}
```

### 8.1 Autenticação (`/api/auth`)

| Método | Path | Perfil | Descrição |
|--------|------|--------|-----------|
| POST | `/auth/login` | Todos | Body: `{username, password}` → `{token, refreshToken, perfil, loja}` |
| POST | `/auth/logout` | Todos | Invalida sessão |
| POST | `/auth/refresh` | Todos | Body: `{refreshToken}` → `{token}` |

### 8.2 Utilizadores (`/api/utilizadores`)

| Método | Path | Perfil | Descrição |
|--------|------|--------|-----------|
| GET | `/utilizadores` | GERENTE, GESTOR | Lista utilizadores da loja |
| POST | `/utilizadores` | GERENTE | Cria conta. Body: `{username, nome, perfilId, lojaId}` |
| GET | `/utilizadores/{id}` | GERENTE | Detalhes do utilizador |
| PUT | `/utilizadores/{id}` | GERENTE | Atualiza dados ou perfil |
| DELETE | `/utilizadores/{id}` | GERENTE | Desativação lógica |

### 8.3 Produtos (`/api/produtos`)

| Método | Path | Perfil | Descrição |
|--------|------|--------|-----------|
| GET | `/produtos` | Todos | Lista produtos ativos. Query: `?q=` (pesquisa por nome/código) |
| GET | `/produtos/{id}` | Todos | Detalhes do produto |
| GET | `/produtos/codigo/{codigo}` | Todos | Busca por EAN (para PDV) |
| POST | `/produtos` | GESTOR | Cria produto |
| PUT | `/produtos/{id}` | GESTOR, GERENTE | Atualiza produto |

### 8.4 Vendas e PDV (`/api/vendas`)

| Método | Path | Perfil | Descrição |
|--------|------|--------|-----------|
| POST | `/vendas` | FUNCIONARIO | Regista venda completa. Body: `{lojaId, meioPagamento, linhas: [{produtoId, quantidade}]}` |
| GET | `/vendas` | GERENTE, GESTOR | Lista vendas. Query: `?lojaId=&dataInicio=&dataFim=&turno=` |
| GET | `/vendas/{id}` | FUNCIONARIO, GERENTE | Detalhe da venda |
| POST | `/vendas/{id}/fatura` | FUNCIONARIO | Body: `{nifCliente?, nomeCliente?}` → Fatura em PDF |
| POST | `/vendas/{id}/devolucao` | FUNCIONARIO | Body: `{produtoId, quantidade}` |
| POST | `/fechos-caixa` | GERENTE | Body: `{lojaId, observacoes?}` |
| GET | `/fechos-caixa` | GERENTE, GESTOR | Lista fechos. Query: `?lojaId=&dataInicio=&dataFim=` |
| GET | `/fechos-caixa/{id}` | GERENTE, GESTOR | Detalhe do fecho |

### 8.5 Stock e inventário (`/api/stock`)

| Método | Path | Perfil | Descrição |
|--------|------|--------|-----------|
| GET | `/stock` | GERENTE, RESPONSAVEL_ARMAZEM | Lista stock da loja. Query: `?lojaId=&apenasAlertas=` |
| GET | `/stock/{produtoId}` | GERENTE, FUNCIONARIO | Stock de produto específico na loja |
| PUT | `/stock/{produtoId}/nivel-minimo` | GERENTE | Body: `{quantidade}` |
| GET | `/stock/alertas` | GERENTE | Alertas ativos |
| POST | `/stock/ajustes` | GERENTE | Body: `{produtoId, lojaId, quantidade, motivo, observacoes}` |
| GET | `/stock/ajustes` | GERENTE | Histórico de ajustes |
| POST | `/inventarios` | RESPONSAVEL_ARMAZEM | Inicia inventário físico. Body: `{lojaId}` |
| PUT | `/inventarios/{id}/linhas` | RESPONSAVEL_ARMAZEM | Body: `[{produtoId, quantidadeContada}]` |
| POST | `/inventarios/{id}/fechar` | RESPONSAVEL_ARMAZEM | Fecha e consolida inventário |
| GET | `/inventarios/{id}/discrepancias` | RESPONSAVEL_ARMAZEM, GERENTE | Lista discrepâncias |

### 8.6 Encomendas e fornecedores (`/api`)

| Método | Path | Perfil | Descrição |
|--------|------|--------|-----------|
| GET | `/fornecedores` | GESTOR, GERENTE | Lista fornecedores ativos |
| POST | `/fornecedores` | GESTOR | Cria fornecedor com condições comerciais |
| PUT | `/fornecedores/{id}` | GESTOR | Atualiza |
| DELETE | `/fornecedores/{id}` | GESTOR | Desativa logicamente |
| GET | `/encomendas` | GERENTE, GESTOR | Lista encomendas. Query: `?lojaId=&status=` |
| POST | `/encomendas` | GESTOR, GERENTE | Cria encomenda. Body: `{fornecedorId, lojaId, linhas: [{produtoId, quantidade}]}` |
| GET | `/encomendas/{id}` | GERENTE, GESTOR | Detalhe |
| PATCH | `/encomendas/{id}/estado` | RESPONSAVEL_ARMAZEM | Body: `{status, observacoes}` |
| POST | `/entradas-mercadoria` | RESPONSAVEL_ARMAZEM | Body: `{encomendaId, numeroGuia, linhas: [{produtoId, quantidadeRecebida}]}` |
| GET | `/encomendas/sugestoes` | GESTOR, GERENTE | Sugestões automáticas baseadas em alertas ativos |

### 8.7 Sincronização (`/api/sincronizacao`)

| Método | Path | Perfil | Descrição |
|--------|------|--------|-----------|
| POST | `/sincronizacao/iniciar` | SISTEMA, GERENTE | Body: `{lojaId}` |
| GET | `/sincronizacao/estado` | GERENTE, GESTOR | Estado da sincronização atual |
| GET | `/sincronizacao/historico` | GESTOR | Lista sincronizações anteriores |
| GET | `/sincronizacao/conflitos` | GESTOR | Lista conflitos resolvidos |

### 8.8 Relatórios e dashboard (`/api`)

| Método | Path | Perfil | Descrição |
|--------|------|--------|-----------|
| GET | `/dashboard` | GESTOR | Query: `?lojaId=&periodo=&categoriaId=` |
| GET | `/relatorios/vendas` | GESTOR | Query: `?lojaId=&dataInicio=&dataFim=` |
| GET | `/relatorios/stock` | GESTOR, GERENTE | Query: `?lojaId=` |
| GET | `/relatorios/rentabilidade` | GESTOR | Query: `?lojaId=&dataInicio=&dataFim=` |
| POST | `/relatorios/exportar` | GESTOR | Body: `{tipo: "vendas"|"stock"|"rentabilidade", formato: "PDF"|"CSV", filtros: {...}}` |

---

## 9. CASOS DE USO — FLUXOS DE IMPLEMENTAÇÃO

Para cada UC, implementa o controller, a lógica no facade, e os testes correspondentes.

### UC-01 | Autenticar no Sistema
1. `POST /auth/login` com `{username, password}`
2. Validar credenciais (BCrypt); registar tentativa no LogAuditoria
3. Se válido: gerar JWT com claims `{sub: userId, role: perfil, lojaId}`; retornar token
4. Se inválido: incrementar contador; bloquear após 5 falhas (`utilizador.bloqueado = true`)
5. Registar acesso no LogAuditoria (`TipoOperacao.LOGIN`)

### UC-02 | Registar Venda no PDV
1. Frontend abre transação local (gera UUID)
2. `POST /vendas` com linhas de produtos
3. Backend valida: produto existe, stock suficiente (RD-04)
4. Criar Venda + LinhaVenda; chamar `Stock.subtrair()` para cada produto
5. Verificar `Stock.estaBaixoMinimo()` → emitir AlertaStock se necessário (RF-05)
6. Se PDV offline: persistir em IndexedDB; sincronizar quando online
7. Registar no LogAuditoria (`TipoOperacao.VENDA_REGISTADA`)
8. Retornar Venda completa com totais calculados

### UC-03 | Emitir Fatura
1. `POST /vendas/{id}/fatura` com NIF opcional
2. Determinar tipo (RD-02): simplificada (≤1000€ sem NIF) ou completa
3. Obter próximo número sequencial via `FaturaSerieService.proximoNumero()` (com lock)
4. Gerar PDF com Apache PDFBox
5. Persistir Fatura; registar LogAuditoria (`TipoOperacao.FATURA_EMITIDA`)
6. Retornar PDF como `application/pdf`

### UC-05 | Fecho de Caixa Diário
1. `POST /fechos-caixa` com lojaId
2. Agregar todas as Vendas FINALIZADAS da loja no dia atual
3. Calcular totais por MeioPagamento
4. Persistir FechoCaixa; registar LogAuditoria (`TipoOperacao.FECHO_CAIXA`)
5. Disparar `SincronizacaoFacade.agendarSincronizacao(lojaId)` de forma assíncrona

### UC-09 | Criar Encomenda a Fornecedor
1. `POST /encomendas` com fornecedorId, lojaId, linhas
2. Verificar disponibilidade do armazém via `Fornecedor.estaDisponivel(now())`
3. Calcular `dataEsperadaProcessamento` via `Fornecedor.calcularDataProcessamento(now())`
4. Persistir Encomenda com EstadoEncomenda PENDENTE
5. Retornar encomenda com `dataEsperadaProcessamento` informada ao utilizador (RD-06)

### UC-13 | Sincronizar Dados com Servidor Central
1. Coletar todos os registos com `sincronizado = false` na BD local
2. Abrir conexão HTTPS/TLS 1.3 com servidor central
3. Enviar batch via `POST /central/sync` com payload assinado
4. Servidor central: aplicar last-write-wins por `updatedAt`; registar conflitos
5. Confirmar sincronização; marcar registos como `sincronizado = true`
6. Atualizar `EstadoSincronizacao`; notificar Gestor se existirem conflitos

---

## 10. ESTRUTURA DO PROJETO FRONTEND

```
src/
├── components/
│   ├── pdv/           # PDVTerminal, ProductSearch, SaleCart, PaymentModal
│   ├── stock/         # StockList, StockAlert, AdjustmentForm, InventoryForm
│   ├── dashboard/     # DashboardMetrics, SalesChart, AlertsPanel
│   ├── reports/       # ReportFilters, ReportTable, ExportButton
│   ├── orders/        # OrderList, OrderForm, SupplierSelector
│   └── shared/        # Layout, Navbar, Sidebar, LoadingSpinner, ErrorBoundary
├── pages/
│   ├── Login.tsx
│   ├── pdv/PDVPage.tsx
│   ├── manager/       # StockPage, CashClosingPage, ReportsPage
│   ├── admin/         # DashboardPage, SuppliersPage, UsersPage, SyncPage
│   └── warehouse/     # InventoryPage, GoodsReceiptPage
├── hooks/
│   ├── useAuth.ts
│   ├── useOfflineSync.ts
│   ├── usePDV.ts
│   └── useAlerts.ts
├── services/
│   ├── api.ts         # axios instance com JWT interceptor
│   ├── offline.ts     # IndexedDB adapter para modo offline
│   └── sw/            # Service Worker para intercepção offline
├── store/
│   ├── authStore.ts   # Zustand: user, token, loja
│   └── pdvStore.ts    # Zustand: carrinho atual, estado PDV
└── types/
    └── api.ts         # TypeScript interfaces das respostas da API
```

### Comportamento offline do PDV

```typescript
// Em offline.ts: armazenar venda localmente
async function registarVendaOffline(venda: VendaRequest): Promise<Venda> {
  const db = await openDB('miniFormiga', 1);
  const vendaLocal = { ...venda, id: crypto.randomUUID(), sincronizado: false };
  await db.add('vendas_pendentes', vendaLocal);
  return vendaLocal as Venda;
}

// Service Worker: tentar API → fallback para offline
self.addEventListener('fetch', (event) => {
  if (event.request.url.includes('/api/vendas') && event.request.method === 'POST') {
    event.respondWith(
      fetch(event.request).catch(() => registarVendaOffline(event.request))
    );
  }
});
```

---

## 11. TESTES — ESTRATÉGIA E COBERTURA

### 11.1 Testes unitários (JUnit 5 + Mockito)

Para cada entidade de domínio, testar:
- Cálculos de negócio (`calcularMargem()`, `calcularTotal()`, `calcularDiscrepancia()`)
- Validações (`Stock.subtrair()` lança exceção quando stock insuficiente)
- Regras de domínio (RD-01 a RD-07)

Para cada Facade/Service, testar:
- Fluxos normais com mocks dos repositórios
- Fluxos de exceção (produto não encontrado, credenciais inválidas, etc.)

### 11.2 Testes de integração (Spring Boot Test + H2)

- Testar cada endpoint REST com `@WebMvcTest` ou `@SpringBootTest`
- Verificar autenticação JWT (403 sem token, 200 com token válido)
- Verificar restrições de perfil (FUNCIONARIO não acede a /dashboard)
- Verificar RNF-01: tempo de resposta < 2s nos endpoints PDV

### 11.3 Cobertura (JaCoCo)

Configurar no `pom.xml`:
```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <configuration>
    <rules>
      <rule>
        <limits>
          <limit>
            <counter>LINE</counter>
            <minimum>0.70</minimum>  <!-- 70% mínimo nas classes de domínio -->
          </limit>
        </limits>
      </rule>
    </rules>
  </configuration>
</plugin>
```

### 11.4 Testes de aceitação

Para cada User Story Must Have, implementar um teste de aceitação com o critério de verificação definido no SRS (Secção 2.12 do documento de requisitos).

---

## 12. DOCKER COMPOSE — AMBIENTE DE DESENVOLVIMENTO

```yaml
# docker-compose.yml
version: '3.9'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: miniFormiga_central
      POSTGRES_USER: miniFormiga
      POSTGRES_PASSWORD: miniFormiga_dev
    ports: ["5432:5432"]

  backend-central:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: central
      DB_URL: jdbc:postgresql://postgres:5432/miniFormiga_central
      JWT_SECRET: ${JWT_SECRET}
    ports: ["8080:8080"]
    depends_on: [postgres]

  backend-loja:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: loja
      LOJA_ID: ${LOJA_ID:-loja-001}
      CENTRAL_URL: http://backend-central:8080
      DB_URL: jdbc:sqlite:/data/loja.db
    ports: ["8081:8080"]
    volumes: ["loja_data:/data"]

  frontend:
    build: ./frontend
    environment:
      VITE_API_URL: http://localhost:8081/api
    ports: ["3000:3000"]

volumes:
  loja_data:
```

---

## 13. ORDEM DE IMPLEMENTAÇÃO RECOMENDADA

Segue esta ordem para garantir que cada camada está funcional antes de construir sobre ela.

**Fase 1 — Fundações (semana 1-2)**
1. Setup Maven/Spring Boot; configurar JPA com H2 para desenvolvimento
2. Implementar todas as entidades de domínio com testes unitários
3. Setup React/Vite/TypeScript; configurar Tailwind + shadcn/ui

**Fase 2 — Autenticação e Utilizadores (semana 2)**
4. SubUtilizadores: entidades, repositórios, service, controller
5. JWT: login, refresh, interceptor no frontend
6. Página de Login com seleção de perfil

**Fase 3 — Módulo PDV (semana 3)**
7. SubPDV: Venda, LinhaVenda, Fatura, FechoCaixa
8. API de produtos (listagem, pesquisa por código)
9. Terminal PDV frontend com modo offline (Service Worker)
10. Faturação com numeração sequencial

**Fase 4 — Stock e Inventário (semana 4)**
11. SubStock: Stock, NivelMinimo, AlertaStock
12. API de stock; alertas via SSE
13. Interfaces do Gerente de Loja
14. Inventário físico e ajustes manuais

**Fase 5 — Encomendas e Fornecedores (semana 5)**
15. SubEncomendas: Fornecedor, Encomenda, EntradaMercadoria
16. Regras RD-06 (horário armazém)
17. Interface de encomendas e gestão de fornecedores

**Fase 6 — Sincronização e Dashboard (semana 6)**
18. SubSincronizacao: motor de sync last-write-wins
19. Dashboard com Recharts
20. Relatórios e exportação PDF/CSV

**Fase 7 — Qualidade e entrega (semana 7)**
21. Completar testes unitários e de integração; verificar cobertura JaCoCo ≥ 70%
22. Documentação OpenAPI automática via Springdoc
23. Docker Compose final; README com instruções de instalação
24. Verificação dos SRS (cruzar cada RF/RNF com testes existentes)

---

## 14. CHECKLIST FINAL DE ENTREGA

Antes de concluir, verificar:

- [ ] Todos os endpoints REST documentados no OpenAPI (`/api/docs`)
- [ ] Todos os RF Must Have implementados e com teste de aceitação
- [ ] Todos os RNF Must Have verificáveis (RNF-01: teste de performance; RNF-04: teste de autenticação; RNF-05: log presente após cada operação financeira)
- [ ] Todas as regras de domínio RD-01 a RD-07 implementadas com testes
- [ ] Modo offline do PDV funcional (testar desligando o backend)
- [ ] Sincronização testada com conflito simulado
- [ ] LogAuditoria presente para: login, venda, fatura, devolução, fecho de caixa, ajuste de inventário, criar/editar/desativar utilizador
- [ ] Numeração sequencial de faturas sem gaps sob concorrência
- [ ] README com: setup, variáveis de ambiente, como correr testes, como adicionar nova loja
- [ ] Nenhum dado pessoal de cliente fora da entidade Fatura (RGPD)
- [ ] Cobertura JaCoCo ≥ 70% nas classes de domínio e serviços

---

## 15. NOTAS FINAIS PARA O AGENTE

- **Prioriza conformidade funcional sobre elegância**: o Sr. Formiga precisa de um sistema que funcione, não de código perfeitamente refatorado.
- **Nunca remove lógica de negócio das entidades**: `calcularMargem()` fica em `Produto`, não num `ProductService`.
- **UUID em tudo**: nunca usar `Long` ou `Integer` como ID de entidade.
- **Nenhuma eliminação física**: todas as operações de "delete" são desativações lógicas (`ativo = false`).
- **Português em toda a nomenclatura de domínio**: entidades, campos, enums e mensagens de erro devem estar em português (exceto termos técnicos de framework como `repository`, `facade`, `controller`).
- **Inconsistência TLS a corrigir**: usar TLS 1.3 em toda a implementação (o documento menciona 1.2 em alguns sítios por gralha); configurar `server.ssl.enabled-protocols=TLSv1.3` no Spring.
- Quando tiveres dúvida sobre um requisito, consulta a secção correspondente deste prompt antes de fazer suposições.