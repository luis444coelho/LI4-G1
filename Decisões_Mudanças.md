# Decisões e Mudanças Face ao Relatório

Este documento regista as decisões tomadas durante a implementação física que diferem, refinam ou tornam mais concreto o que estava definido no relatório e nos diagramas UML. O objetivo é manter rastreabilidade entre a especificação académica e o código efetivamente construído.

## Estado Analisado

Foram analisadas as tarefas já assinaladas em `TAREFAS_IMPLEMENTACAO.md`, o estado atual do `backend`, o frontend React existente e os pontos implementados até à tarefa 3:

- alinhamento arquitetural inicial;
- `SubUtilizadores` e autenticação;
- `SubAuditoria`;
- `SubPDV`, incluindo venda, pagamento, faturação e devolução.

A última execução completa de testes do backend, após a implementação da tarefa 3, passou com sucesso: 97 testes executados, 0 falhas.

## Decisões Implementadas

### DM-01 - `SubAuditoria` formalizado como subsistema

Diferença face ao relatório: o relatório descrevia auditoria como responsabilidade transversal e a decisão DA-08 indicava escrita em ficheiros JSON. No código, a auditoria passou a existir também como subsistema formal.

Decisão tomada: foram criados `ISubAuditoria` e `AuditoriaFacade`, expostos por `IMiniFormigaLN`, mantendo `AuditoriaService` como componente técnico de escrita dos logs.

Motivo: alinhar melhor o backend com o diagrama de componentes, onde a lógica de negócio inclui `SubAuditoria` a par de `SubPDV`, `SubStock`, `SubUtilizadores`, `SubEncomendas` e `SubSincronizacao`.

Impacto no relatório: deve ser assumido que `SubAuditoria` é simultaneamente um subsistema da lógica de negócio e um serviço técnico de persistência em ficheiros JSON.

Ficheiros principais:

- `backend/src/main/java/pt/miniFormiga/subsistemas/auditoria/ISubAuditoria.java`
- `backend/src/main/java/pt/miniFormiga/subsistemas/auditoria/AuditoriaFacade.java`
- `backend/src/main/java/pt/miniFormiga/auditoria/AuditoriaService.java`
- `backend/src/main/java/pt/miniFormiga/facade/IMiniFormigaLN.java`

### DM-02 - Campos do log de auditoria normalizados

Diferença face ao relatório: o relatório define os campos obrigatórios do log de auditoria de forma conceptual, mas não fixa uma estrutura JSON final.

Decisão tomada: o log passou a usar os campos normalizados `utilizadorId`, `tipoOperacao`, `dataHora`, `entidade`, `entidadeId` e `descricao`.

Motivo: cumprir RNF-05 de forma verificável e reduzir ambiguidade entre termos como recurso, operação e entidade afetada.

Impacto no relatório: a secção de implementação pode indicar esta estrutura como formato físico adotado para o log JSON.

### DM-03 - Perfil `RESPONSAVEL_ARMAZEM` como designação única

Diferença face ao relatório: existiam variações de nomenclatura para o responsável de armazém.

Decisão tomada: o código passou a usar `RESPONSAVEL_ARMAZEM` como designação canónica do perfil, removendo alias como `RESP_ARMAZEM`.

Motivo: evitar divergência entre permissões, seeds, controladores e interface.

Impacto no relatório: qualquer referência abreviada deve ser tratada apenas como texto descritivo; no sistema físico, o perfil é `RESPONSAVEL_ARMAZEM`.

### DM-04 - Bloqueio de conta após cinco falhas persistido em `Utilizador`

Diferença face ao diagrama de classes: o diagrama não explicita o atributo `tentativasFalhadas` na entidade `Utilizador`.

Decisão tomada: `Utilizador` passou a armazenar o número de tentativas falhadas e a bloquear a conta após cinco falhas consecutivas.

Motivo: implementar o fluxo de exceção do UC-01 e tornar o comportamento testável e persistente.

Impacto no relatório: deve ser documentado que `tentativasFalhadas` é um atributo físico adicional necessário à autenticação segura.

Ficheiros principais:

- `backend/src/main/java/pt/miniFormiga/domain/Utilizador.java`
- `backend/src/main/java/pt/miniFormiga/subsistemas/utilizadores/UtilizadoresFacade.java`

### DM-05 - Gestão de utilizadores limitada por loja para gerente

Diferença face ao relatório: o relatório indica que o gerente gere utilizadores da sua loja, mas a API inicial podia ser interpretada como listagem global.

Decisão tomada: a listagem de utilizadores filtra por loja quando o utilizador autenticado não tem permissões globais.

Motivo: cumprir RF-10, RNF-04 e US-11 sem permitir que um gerente consulte colaboradores de toda a cadeia.

Impacto no relatório: a especificação de API deve clarificar que `GET /utilizadores` é sensível ao perfil autenticado.

### DM-06 - Endpoints auxiliares para perfis e lojas

Diferença face ao relatório: os endpoints `/api/v1/utilizadores/perfis` e `/api/v1/utilizadores/lojas` não aparecem na tabela inicial da API.

Decisão tomada: foram adicionados endpoints auxiliares para suportar a interface de gestão de utilizadores.

Motivo: a UI precisa de listas controladas de perfis e lojas para criar/editar utilizadores sem hardcoding no frontend.

Impacto no relatório: a tabela de endpoints de utilizadores deve ser atualizada com estes dois endpoints.

### DM-07 - `Devolucao` criada como entidade persistente

Diferença face ao diagrama de classes: o relatório inclui US-17, RF-16 e UC-04 para devoluções, mas o modelo de domínio/classe original não continha uma entidade `Devolucao`.

Decisão tomada: foi criada a entidade `Devolucao` e o respetivo `DevolucaoRepository`.

Motivo: uma devolução não deve existir apenas como efeito colateral em stock e auditoria; precisa de histórico próprio para rastreabilidade operacional e financeira.

Impacto no relatório: o diagrama de classes e a tabela de entidades devem ser atualizados para incluir `Devolucao`, associada a `Venda` e `Produto`.

Ficheiros principais:

- `backend/src/main/java/pt/miniFormiga/domain/Devolucao.java`
- `backend/src/main/java/pt/miniFormiga/repository/DevolucaoRepository.java`
- `backend/src/main/java/pt/miniFormiga/subsistemas/pdv/PDVFacade.java`

### DM-08 - Nota de crédito tratada como documento simplificado interno

Diferença face ao relatório: o relatório refere devolução e conformidade fiscal, mas não detalha nota de crédito.

Decisão tomada: a devolução gera um documento numerado no formato `NC/<ano>/<sequencia>`, persistido em `Devolucao`. Isto cobre rastreabilidade interna, mas não equivale ainda a uma nota de crédito fiscal completa certificada pela AT.

Motivo: implementar RF-16 de forma concreta sem introduzir uma camada fiscal completa fora do âmbito atual.

Impacto no relatório: deve ser assumido explicitamente que a nota de crédito fiscal completa fica fora do âmbito da implementação atual, ou então deve ser modelada como trabalho futuro obrigatório.

### DM-09 - Numeração sequencial com bloqueio pessimista como exceção à DA-06

Diferença face ao relatório: a decisão DA-06 privilegia `optimistic locking` como regra geral de concorrência.

Decisão tomada: a numeração de faturas e documentos de devolução usa a sequência `FaturaSequencia` com bloqueio de escrita no repositório.

Motivo: RD-03 exige numeração sequencial, única e ininterrupta por série. Neste caso específico, o bloqueio pessimista é mais adequado do que aceitar falhas por conflito e repetir a operação.

Impacto no relatório: DA-06 deve incluir esta exceção: a concorrência geral usa `optimistic locking`, mas séries fiscais usam bloqueio pessimista para garantir sequência.

### DM-10 - Meios de pagamento obrigatórios garantidos no arranque do `SubPDV`

Diferença face ao relatório: o relatório enumera os meios `numerário`, `cartão` e `MB Way`, mas não define como garantir a sua existência física.

Decisão tomada: `PDVFacade` garante no arranque que existem `NUMERARIO`, `CARTAO` e `MBWAY`.

Motivo: evitar dependência exclusiva de seed/demo data para uma regra de domínio essencial do PDV.

Impacto no relatório: pode ser descrito como inicialização técnica obrigatória do subsistema `SubPDV`.

### DM-11 - Tipos de meio de pagamento normalizados em maiúsculas

Diferença face ao relatório: o relatório usa nomes legíveis para meios de pagamento, mas não fixa representação interna.

Decisão tomada: `MeioPagamento` normaliza o tipo para maiúsculas (`NUMERARIO`, `CARTAO`, `MBWAY`).

Motivo: impedir divergências como `Cartao`, `cartão`, `CARTAO` ou `MB Way` no backend.

Impacto no relatório: a API pode continuar a apresentar nomes amigáveis, mas a representação interna é normalizada.

### DM-12 - Persistência explícita da venda após alterar linhas

Diferença face ao relatório: o relatório especifica o comportamento do PDV, mas não detalha cascades JPA nem recarregamento de agregados.

Decisão tomada: ao adicionar ou remover linhas de venda, a venda é guardada explicitamente e os totais são recalculados.

Motivo: garantir consistência entre `Venda` e `LinhaVenda` em contexto JPA e tornar os testes de reload previsíveis.

Impacto no relatório: sem impacto conceptual; é uma decisão técnica de persistência.

### DM-13 - Remoção de linha inexistente passa a falhar explicitamente

Diferença face ao relatório: UC-02/UC-16 descrevem a remoção de linhas antes da finalização, mas não definem o comportamento se a linha não existir.

Decisão tomada: `Venda.anularLinha` lança erro quando a linha indicada não pertence à venda.

Motivo: evitar respostas silenciosas e facilitar deteção de erros de interface ou API.

Impacto no relatório: pode ser acrescentado como fluxo de exceção do UC-02.

### DM-14 - Stock negativo bloqueado na escrita transacional

Diferença face ao relatório: RD-04 exige bloquear stock negativo, mas não especifica como tratar concorrência.

Decisão tomada: a finalização da venda aplica a atualização de stock dentro da mesma transação e depende da validação em `Stock.atualizarStock`, com suporte de versionamento herdado de `EntidadeBase`.

Motivo: impedir que a validação prévia fique desatualizada em cenários concorrentes.

Impacto no relatório: a secção de implementação pode indicar que RD-04 é garantido na operação de escrita, não apenas na consulta prévia.

### DM-15 - Fatura simplificada/completa concretizada no backend

Diferença face ao relatório: RD-02 define a regra fiscal, mas a implementação precisou de tornar explícitas as condições.

Decisão tomada: fatura simplificada é permitida até 1000 EUR sem NIF; fatura completa é exigida quando há NIF ou quando o valor ultrapassa 1000 EUR.

Motivo: alinhar o comportamento físico com RD-02 e RNF-08.

Impacto no relatório: deve ser possível referir esta regra como já implementada e coberta por testes.

## Divergências Ainda Pendentes

### DP-01 - Frontend ainda usa dados mock

Estado atual: o frontend React existe e tem páginas por perfil, mas ainda importa dados de `frontend/src/data/mockData.ts`.

Diferença face ao relatório: o relatório apresenta a interface como parte do sistema implementado, com API REST documentada. Neste momento, a interface é essencialmente navegável/mockada e ainda não consome os endpoints reais.

Decisão necessária: integrar progressivamente o frontend com o backend ou documentar a interface como protótipo funcional parcial.

### DP-02 - Dashboard e relatórios ainda não têm endpoints dedicados

Estado atual: existem dados de vendas, stock e fechos, mas não há controladores dedicados para `/api/v1/dashboard` e `/api/v1/relatorios/*`.

Diferença face ao relatório: RF-01 e RF-02 prometem dashboard, relatórios e exportação.

Decisão necessária: implementar o módulo de relatórios/dashboard antes dos testes finais ou assinalar estes requisitos como parcialmente satisfeitos.

### DP-03 - Sincronização ainda é esqueleto

Estado atual: `SincronizacaoFacade` expõe `agendarSincronizacao`, mas a lógica real de sincronização local-central, conflitos e `last-write-wins` ainda não está implementada.

Diferença face ao relatório: RF-17, RNF-03, RNF-06 e UC-13 descrevem sincronização automática com deteção/resolução de conflitos.

Decisão necessária: implementar a sincronização ou limitar a validação a um stub demonstrativo.

### DP-04 - TLS não está configurado no Spring Boot

Estado atual: não existe configuração HTTPS/TLS no backend. A API corre em HTTP local.

Diferença face ao relatório: RNF-06/UC-13 referem TLS 1.2+ ou TLS 1.3.

Decisão necessária: configurar HTTPS no Spring Boot, usar reverse proxy em ambiente de implantação, ou documentar TLS como requisito de infraestrutura não implementado localmente.

### DP-05 - PIT e jqwik ainda não estão configurados

Estado atual: o backend tem JaCoCo configurado, mas não há dependências/configuração de PIT nem jqwik no `pom.xml`.

Diferença face ao relatório: a estratégia de testes menciona análise de mutação com PIT e property-based testing com jqwik.

Decisão necessária: adicionar estas ferramentas e testes reais, ou remover/ajustar essa promessa no relatório.

### DP-06 - Docker Compose não orquestra o backend

Estado atual: `docker-compose.yml` contém PostgreSQL e frontend, mas não inclui serviço backend.

Diferença face ao relatório: a configuração do ambiente sugere orquestração dos componentes de aplicação.

Decisão necessária: adicionar serviço backend ao Compose ou clarificar que o backend é executado localmente por Maven durante o desenvolvimento.

### DP-07 - Fiscalidade real ainda é simplificada

Estado atual: há regras de fatura simplificada/completa e numeração sequencial, mas não há certificação fiscal, assinatura AT, SAF-T, comunicação AT ou nota de crédito fiscal completa.

Diferença face ao relatório: o texto fala em conformidade com normas da Autoridade Tributária de forma ampla.

Decisão necessária: reduzir o alcance do relatório para conformidade simplificada académica, ou planear um módulo fiscal completo.

## Atualizações Recomendadas ao Relatório e Diagramas

- Atualizar o diagrama de classes com `Devolucao`.
- Atualizar a tabela de entidades com `Devolucao` e a sua relação com `Venda` e `Produto`.
- Acrescentar `tentativasFalhadas` a `Utilizador` como detalhe físico de autenticação.
- Clarificar que `SubAuditoria` é um subsistema formal, mas persiste em ficheiros JSON por decisão DA-08.
- Atualizar a API de utilizadores com `/utilizadores/perfis` e `/utilizadores/lojas`.
- Explicitar a exceção à DA-06: numeração fiscal usa bloqueio pessimista.
- Corrigir a referência TLS para uma versão única e indicar se será garantida pela aplicação ou pela infraestrutura.
- Rever a secção de testes para decidir definitivamente entre implementar PIT/jqwik ou retirar essas referências.

## Resumo Executivo

As mudanças implementadas mantêm o espírito do relatório e, na maior parte dos casos, tornam a especificação mais concreta e testável. As principais diferenças reais são a criação da entidade `Devolucao`, a formalização de `SubAuditoria`, os endpoints auxiliares de utilizadores e a exceção de bloqueio pessimista para numeração fiscal.

Os maiores desvios ainda por resolver não estão no `SubPDV` nem na autenticação, mas sim em funcionalidades de integração e validação final: frontend ligado à API, dashboard/relatórios, sincronização real, TLS e ferramentas avançadas de testes.
