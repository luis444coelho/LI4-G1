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

A última execução completa de testes do backend, após a implementação da tarefa 6, passou com sucesso: 130 testes executados, 0 falhas.

## Decisões com Impacto Pendente no Relatório

### DM-15 - Fatura simplificada/completa concretizada no backend

Decisão tomada: fatura simplificada é permitida até 1000 EUR sem NIF; fatura completa é exigida quando há NIF ou quando o valor ultrapassa 1000 EUR.

Impacto pendente: registar RD-02 como satisfeito na tabela de verificação do SRS (Capítulo 6), quando essa tabela for preenchida.


## Divergências Ainda Pendentes

### DP-01 - Secção de interface do Capítulo 5 por preencher

Estado atual: o frontend React consome a API real nos fluxos principais de negócio: autenticação, dashboard, relatórios/stock, PDV, devolução, fecho de caixa, ajustes, utilizadores, fornecedores, encomendas, sincronização, entrada de mercadoria e inventário físico. `frontend/src/data/mockData.ts` existe apenas como configuração estática de perfis, rotas e ícones — não é fonte de dados operacionais.

Ação necessária: preencher a secção "Interface" do Capítulo 5 (atualmente em branco) indicando que os fluxos principais consomem a API real e que a camada de apresentação conserva configuração estática para perfis, rotas e ícones.

### DP-02 - Dashboard e relatórios sem endpoints dedicados

Estado atual: existem dados de vendas, stock e fechos, mas não há controladores dedicados para `/api/v1/dashboard` e `/api/v1/relatorios/*`.

Diferença face ao relatório: RF-01 e RF-02 especificam dashboard, relatórios e exportação.

Ação necessária: implementar o módulo de relatórios/dashboard antes dos testes finais, ou assinalar RF-01 e RF-02 como parcialmente satisfeitos na tabela de verificação do SRS (Capítulo 6).

### DP-03 - Consolidação central baseada em metadados, não em snapshots completos

Estado atual: o perfil `central` recebe o payload, regista a sincronização consolidada e resolve conflitos por `updatedAt`/`version`. O payload atual contém metadados e logs, mas não serializa o estado completo de cada entidade para recriar registos ausentes no servidor central.

Diferença face ao relatório: RF-17, RNF-03 e UC-13 descrevem consolidação de dados entre loja e servidor central.

Ação necessária: expandir o payload com snapshots completos das entidades, ou documentar na tabela de verificação do SRS que a validação atual cobre contrato, estados, retry e conflitos por metadados, ficando a materialização completa de entidades ausentes como trabalho futuro (Capítulo 7).

### DP-05 - PIT e jqwik não configurados

Estado atual: o backend tem JaCoCo configurado, mas não há dependências de PIT nem jqwik no `pom.xml`. A secção de estratégia de testes do Capítulo 6 foi atualizada para indicar que estas ferramentas não foram implementadas no âmbito temporal do projeto.

Ação necessária: confirmar que o texto da secção de testes está alinhado com o que foi efetivamente executado e que não subsistem referências a PIT ou jqwik como trabalho realizado.

### DP-07 - Conformidade fiscal simplificada

Estado atual: há regras de fatura simplificada/completa e numeração sequencial, mas não há certificação fiscal, assinatura AT, SAF-T, comunicação AT ou nota de crédito fiscal completa.

Ação necessária: confirmar que o UC-04 corrigido e o parágrafo acrescentado no Capítulo 7 delimitam claramente o âmbito académico da conformidade fiscal implementada, evitando que o texto restante do relatório sugira conformidade plena com a AT.