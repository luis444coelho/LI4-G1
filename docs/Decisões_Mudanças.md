# Decisões e Mudanças Face ao Relatório

Este documento regista as divergências ainda pendentes entre a implementação física e o relatório, bem como decisões de implementação com impacto no relatório ainda por resolver. As decisões já refletidas no relatório foram removidas deste documento.

## Estado Analisado

Foram analisadas as tarefas já assinaladas em `TAREFAS_IMPLEMENTACAO.md`, o estado atual do `backend`, o frontend React existente e os pontos implementados até à tarefa 6:

- alinhamento arquitetural inicial;
- `SubUtilizadores` e autenticação;
- `SubAuditoria`;
- `SubPDV`, incluindo venda, pagamento, faturação e devolução.
- `SubStock`, incluindo alertas, ajustes, inventário físico e localização.
- `SubEncomendas`, incluindo fornecedores, encomendas e entrada de mercadoria.
- `SubSincronizacao`, incluindo agendamento, payload, transporte REST/JSON, endpoint central por perfil, estados e conflitos.
- infraestrutura de execucao com o mesmo backend em perfil `local` e `central`, Docker Compose completo, seed demo configuravel e CORS para o frontend em porta separada.
- frontend mais ligado a API real para relatorios/exportacao, categorias, alertas de stock, localizacao, fatura completa, sincronizacao, inventario e funcionarios.
- ambiente local atualizado com Temurin JDK 21.0.11 e Node.js 20.19.5 em `.tools/`, excluido do Git.

A validacao oficial foi atualizada em 2026-05-26 com a toolchain local: `mvn clean verify` executou 215 testes, 0 falhas e 0 erros, com JaCoCo global em 87.79% de linhas e 64.13% de ramos. O `pom.xml` passou a aplicar thresholds globais de 75% em linhas e 50% em ramos, mantendo 70% de linhas para `pt.miniFormiga.domain`. No frontend, `npm run lint` e `npm run build` passaram com Node.js 20.19.5. A validacao anterior de Docker Compose manteve-se como evidencia de integracao tecnica.

## Decisões com Impacto Pendente no Relatório

### DM-15 - Fatura simplificada/completa concretizada no backend

Decisão tomada: fatura simplificada é permitida até 1000 EUR sem NIF; fatura completa é exigida quando há NIF ou quando o valor ultrapassa 1000 EUR.

Impacto pendente: registar RD-02 como satisfeito na tabela de verificação do SRS (Capítulo 6), quando essa tabela for preenchida.

### DM-16 - Execução local/central por perfis da mesma aplicação

Decisão tomada: não foi criado um segundo projeto para o servidor central. O mesmo backend Spring Boot arranca em dois perfis:

- `local`: instância de loja, SQLite, operação offline e envio de sincronização;
- `central`: instância central, PostgreSQL, receção de sincronizações, histórico e consolidação.

Impacto pendente: no relatório, quando for descrita a execução física, explicitar que "servidor central" significa uma segunda instância do mesmo monólito modular, não um microserviço nem um repositório separado.

### DM-17 - Porta PostgreSQL do host em desenvolvimento

Decisão tomada: no `docker-compose.yml`, o PostgreSQL continua a usar a porta `5432` dentro do container, mas fica exposto no host em `5433`.

Impacto pendente: esta é apenas uma decisão de ambiente de desenvolvimento para evitar conflitos com PostgreSQL local. Não altera a arquitetura do relatório.

### DM-18 - Seed demo configurável

Decisão tomada: os dados demo são carregados por omissão, mas podem ser desligados com `DEMO_DATA_ENABLED=false`.

Impacto pendente: no Capítulo 6, ao falar de testes, distinguir dados de demonstração de dados criados pelos testes automatizados.


## Divergências Ainda Pendentes

### DP-01 - Secção de interface do Capítulo 5 por preencher

Estado atual: o frontend React consome a API real nos fluxos principais de negócio: autenticação, dashboard, relatórios/stock, PDV, devolução, fecho de caixa, ajustes, utilizadores, fornecedores, encomendas, sincronização, entrada de mercadoria e inventário físico. `frontend/src/data/mockData.ts` existe apenas como configuração estática de perfis, rotas e ícones — não é fonte de dados operacionais.

Ação necessária: preencher a secção "Interface" do Capítulo 5 (atualmente em branco) indicando que os fluxos principais consomem a API real e que a camada de apresentação conserva configuração estática para perfis, rotas e ícones.

### DP-03 - Consolidação central baseada em metadados, não em snapshots completos

Estado atual: o perfil `central` recebe o payload, regista a sincronização consolidada e resolve conflitos por `updatedAt`/`version`. O payload atual contém metadados e logs, mas não serializa o estado completo de cada entidade para recriar registos ausentes no servidor central.

Diferença face ao relatório: RF-17, RNF-03 e UC-13 descrevem consolidação de dados entre loja e servidor central.

Ação necessária: expandir o payload com snapshots completos das entidades, ou documentar na tabela de verificação do SRS que a validação atual cobre contrato, estados, retry e conflitos por metadados, ficando a materialização completa de entidades ausentes como trabalho futuro (Capítulo 7).

### DP-05 - PIT e jqwik não configurados

Estado atual: o backend tem JaCoCo configurado e validado com thresholds globais, mas não há dependências de PIT nem jqwik no `pom.xml`. A secção de estratégia de testes do Capítulo 6 foi atualizada para indicar que estas ferramentas não foram implementadas no âmbito temporal do projeto.

Ação necessária: concluida para o Capítulo 6; manter esta decisão caso sejam feitas novas alterações ao relatório.

### DP-07 - Conformidade fiscal simplificada

Estado atual: há regras de fatura simplificada/completa e numeração sequencial, mas não há certificação fiscal, assinatura AT, SAF-T, comunicação AT ou nota de crédito fiscal completa.

Ação necessária: confirmar que o UC-04 corrigido e o parágrafo acrescentado no Capítulo 7 delimitam claramente o âmbito académico da conformidade fiscal implementada, evitando que o texto restante do relatório sugira conformidade plena com a AT.
