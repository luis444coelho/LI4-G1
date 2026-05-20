# Mini-Formiga - Tarefas de Implementacao por Ordem

Este ficheiro organiza o que falta implementar para que o projeto fique alinhado com o relatorio, os requisitos RF/RNF/RD, os casos de uso e os diagramas de classes/componentes fornecidos.

Estado de referencia analisado:

- Backend Spring Boot ja compila e passa `mvn verify`.
- Existem entidades de dominio, repositories, controllers e facades para os modulos principais.
- O frontend existe como mockup navegavel, mas ainda usa `mockData`.
- A sincronizacao esta apenas como placeholder.
- O relatorio promete endpoints e comportamentos que ainda nao estao todos implementados.

## Prioridade Global

1. Fechar divergencias entre diagramas, relatorio e codigo.
2. Completar funcionalidades Must Have incompletas.
3. Integrar frontend com backend real.
4. Implementar testes de integracao/sistema/aceitacao.
5. Atualizar o relatorio com evidencias reais.

## 0. Alinhamento Inicial Obrigatorio

Antes de implementar novas funcionalidades, decidir e aplicar estes alinhamentos para evitar retrabalho.

- [x] Confirmar que o diagrama de componentes deve ter 6 subsistemas na logica de negocio: `SubPDV`, `SubStock`, `SubUtilizadores`, `SubEncomendas`, `SubSincronizacao`, `SubAuditoria`.
- [x] Escolher uma de duas opcoes para auditoria:
  - [x] Criar `ISubAuditoria` e `AuditoriaFacade`, expondo-os em `IMiniFormigaLN`.
  - [ ] Ou atualizar o relatorio/diagrama para explicar que auditoria e um servico transversal interno, nao um subsistema exposto.
- [x] Uniformizar permissao/nomenclatura de responsavel de armazem: foi mantido apenas `RESPONSAVEL_ARMAZEM`.
- [x] Uniformizar a versao TLS no guia de tarefas: usar TLS 1.3 na arquitetura/documentacao final.
- [x] Rever o `GUIA_IMPLEMENTAÇÃO.md`: o ficheiro ja nao existe no repositorio atual; o plano operacional passou a estar neste ficheiro.

Resultado esperado: relatorio, diagramas e estrutura de codigo deixam de se contradizer.

## 1. SubUtilizadores e Autenticacao

Requisitos/casos de uso: RF-10, RNF-04, RNF-05, US-11, UC-01, UC-12.

Estado atual: autenticar, criar, listar, obter, atualizar dados/perfil/loja/password/estado e desativar ja existem. JWT, `@PreAuthorize`, bloqueio por falhas e endpoints auxiliares tambem existem.

Tarefas:

- [x] Completar edicao de utilizador para permitir alterar `nome`, `email`, `perfil` e `loja`, nao apenas password/ativo.
- [x] Garantir filtragem por loja para gerente, se o perfil `GERENTE` nao deve ver todos os utilizadores da cadeia.
- [x] Implementar bloqueio de conta apos 5 tentativas falhadas, como descrito no UC-01.
- [x] Registar tentativas falhadas de login no log de auditoria.
- [x] Garantir que logout nao falha quando `principal` for nulo.
- [x] Criar endpoints auxiliares para listar perfis e lojas, necessarios ao frontend de gestao de utilizadores.

Testes obrigatorios:

- [x] Login com credenciais validas devolve JWT.
- [x] Login invalido devolve 401 e regista tentativa.
- [x] Conta bloqueada apos 5 falhas.
- [x] Gerente sem permissao nao acede a endpoints de gestor.
- [x] Editar utilizador fica registado em auditoria.

## 2. SubAuditoria

Requisitos/casos de uso: RNF-05, UC-01, UC-02, UC-05, UC-10, UC-12, DA-08.

Estado atual: existe `AuditoriaService` com log JSON rotativo e, para alinhar com o diagrama de componentes, ja existe `ISubAuditoria` + `AuditoriaFacade` exposto por `IMiniFormigaLN`.

Tarefas:

- [x] Decidir a representacao arquitetural final conforme a tarefa 0.
- [x] Criar `SubAuditoria` formal:
  - [x] Criar `ISubAuditoria`.
  - [x] Criar `AuditoriaFacade`.
  - [x] Expor `auditoria()` em `IMiniFormigaLN`.
  - [x] Manter escrita em ficheiros JSON para respeitar DA-08.
- [x] Normalizar os campos do log: `utilizadorId`, `tipoOperacao`, `dataHora`, `entidade`, `entidadeId`, `descricao`.
- [x] Garantir que operacoes sensiveis chamam auditoria de forma consistente.

Testes obrigatorios:

- [x] Venda finalizada gera log.
- [x] Fatura emitida gera log.
- [x] Fecho de caixa confirmado gera log.
- [x] Ajuste de stock gera log.
- [x] Alteracao de utilizador gera log.

## 3. SubPDV - Venda, Pagamento, Fatura e Devolucao

Requisitos/casos de uso: RF-12, RF-13, RF-14, RF-15, RF-16, RNF-01, RNF-02, RNF-08, RD-01, RD-02, RD-03, RD-04, RD-05, UC-02, UC-03, UC-04.

Estado atual: existem venda aberta, linhas persistidas por cascade, finalizar, anular linha, anular venda, fatura, devolucao persistida como documento de movimento e fecho de caixa.

Tarefas:

- [x] Persistir corretamente linhas de venda quando sao adicionadas, garantindo cascades JPA e reload consistente.
- [x] Validar quantidade positiva em `adicionarLinhaVenda`.
- [x] Garantir que remover/anular linha recalcula totais e nao afeta linhas restantes.
- [x] Confirmar que finalizar venda bloqueia stock negativo em concorrencia, nao apenas por validacao previa.
- [x] Garantir que os meios de pagamento obrigatorios existem: `NUMERARIO`, `CARTAO`, `MBWAY`.
- [x] Formalizar devolucao como entidade ou documento de movimento, se o relatorio exigir historico proprio. A devolucao ficou persistida em `Devolucao`.
- [x] Implementar nota de credito ou documentar que fica fora do ambito fiscal completo. Foi implementado documento de devolucao numerado com serie `NC/<ano>`.
- [x] Garantir que fatura simplificada/completa cumpre RD-02:
  - [x] simplificada ate 1000 EUR sem NIF;
  - [x] completa quando ha NIF ou valor superior ao limite.
- [x] Garantir numeracao sequencial unica e ininterrupta por serie em contexto concorrente.
- [x] Confirmar sinalizacao de venda fora de horario para auditoria.

Testes obrigatorios:

- [x] Registar venda com 3 produtos.
- [x] Pesquisar produto por codigo de barras.
- [x] Remover linha antes de finalizar.
- [x] Finalizar com numerario, cartao e MB Way.
- [x] Bloquear venda com stock insuficiente.
- [x] Emitir fatura simplificada.
- [x] Emitir fatura completa com NIF.
- [x] Bloquear fatura completa sem NIF quando obrigatoria.
- [x] Processar devolucao e repor stock.
- [x] Venda fora de horario gera log de auditoria.

## 4. SubStock - Stock, Alertas, Ajustes e Inventario

Requisitos/casos de uso: RF-05, RF-07, RF-11, RF-18, RF-19, RD-04, UC-07, UC-10, UC-11.

Estado atual: consultar stock, definir nivel minimo, alertas, ajustes e inventario fisico existem. Localizacao de produto tambem existe por controller proprio.

Tarefas:

- [x] Evitar duplicacao de alertas ativos para o mesmo stock enquanto o alerta anterior nao for lido/resolvido. Implementado com estado `resolvido` e validacao por alerta nao resolvido.
- [x] Associar alertas aos perfis relevantes, conforme diagrama: gestor e gerente.
- [x] Acrescentar endpoint para resolver/fechar alerta, alem de marcar como lido, se o relatorio exigir ciclo de vida do alerta.
- [x] Garantir que `registarAjuste` nao permite stock negativo.
- [x] Garantir que inventario fisico lista todos os produtos da loja, nao apenas linhas adicionadas manualmente.
- [x] Expor discrepancias de inventario de forma diretamente compativel com UC-11.
- [x] Confirmar que localizacao de produto cobre corredor e prateleira, como RF-18/US-21.

Testes obrigatorios:

- [x] Consulta de stock mostra quantidade e nivel minimo.
- [x] Definir nivel minimo abaixo do stock nao cria alerta.
- [x] Definir nivel minimo acima/igual ao stock cria alerta.
- [x] Venda que baixa stock abaixo do minimo cria alerta. Coberto pela combinacao `PDVFacadeTest` (finalizacao chama `SubStock`) + `StockFacadeTest` (descida abaixo do minimo emite alerta).
- [x] Ajuste positivo aumenta stock.
- [x] Ajuste negativo bloqueia stock negativo.
- [x] Inventario calcula discrepancia corretamente.
- [x] Fechar inventario impede novas linhas.
- [x] Atualizar localizacao de produto persiste corredor/prateleira.

## 5. SubEncomendas - Fornecedores, Encomendas e Entrada de Mercadoria

Requisitos/casos de uso: RF-03, RF-06, RD-06, UC-08, UC-09, UC-14.

Estado atual: fornecedores, condicoes comerciais, encomendas, estados e entradas de mercadoria existem.

Tarefas:

- [ ] Implementar sugestoes automaticas de encomenda com base em alertas de stock ativos.
- [ ] Implementar calculo da data esperada de processamento no momento da criacao da encomenda.
- [ ] Aplicar RD-06: encomenda fora de segunda a sexta, 08:00-18:00, so processa no proximo dia util.
- [ ] Guardar a data esperada de processamento na encomenda ou devolve-la no DTO.
- [ ] Impedir criacao de encomenda sem linhas.
- [ ] Validar quantidades positivas e preco unitario nao negativo.
- [ ] Validar que produtos encomendados pertencem ao fornecedor ou tem condicao comercial definida.
- [ ] Em entrada de mercadoria, suportar varias linhas/produtos por guia, de acordo com o UC-08 e o diagrama.
- [ ] Registar discrepancia recebido vs encomendado de forma consultavel.
- [ ] Atualizar estado da encomenda para `RECEBIDA` apenas quando todas as linhas forem recebidas, ou documentar entregas parciais.

Testes obrigatorios:

- [ ] Criar fornecedor.
- [ ] Definir condicao comercial.
- [ ] Criar encomenda com linhas validas.
- [ ] Bloquear encomenda vazia.
- [ ] Calcular processamento dentro do horario.
- [ ] Calcular proximo dia util fora do horario.
- [ ] Registar entrada de mercadoria e atualizar stock.
- [ ] Registar discrepancia de entrega.
- [ ] Estado da encomenda muda corretamente.

## 6. SubSincronizacao - Operacao Offline e Servidor Central

Requisitos/casos de uso: RF-17, RNF-02, RNF-03, RNF-06, DA-02, DA-04, DA-05, UC-13.

Estado atual: `SincronizacaoFacade.agendarSincronizacao` nao implementa comportamento real.

Esta e a maior lacuna antes de dizer que a implementacao fisica esta concluida.

Tarefas:

- [ ] Criar persistencia real para `Sincronizacao` e `EstadoSincronizacao`.
- [ ] Criar estado inicial `PENDENTE` quando o fecho de caixa e confirmado.
- [ ] Implementar endpoint `POST /api/v1/sincronizacao/iniciar`.
- [ ] Implementar endpoint `GET /api/v1/sincronizacao/estado`.
- [ ] Implementar endpoint `GET /api/v1/sincronizacao/historico`.
- [ ] Implementar endpoint `GET /api/v1/sincronizacao/conflitos`.
- [ ] Definir payload de sincronizacao: vendas, faturas, stock, ajustes, fechos, entradas de mercadoria e logs relevantes.
- [ ] Implementar deteccao de registos pendentes por `updatedAt`/estado/local marker.
- [ ] Implementar transmissao para servidor central via REST/JSON.
- [ ] Implementar resolucao `last-write-wins` com base em timestamp/version.
- [ ] Registar conflitos resolvidos para consulta posterior.
- [ ] Garantir que falha de rede mantem estado `PENDENTE` e agenda retry.
- [ ] Garantir que a loja continua a operar localmente com SQLite.
- [ ] Documentar que TLS e garantido pela configuracao de deployment/reverse proxy, ou configurar HTTPS no Spring se for demonstrado localmente.

Testes obrigatorios:

- [ ] Confirmar fecho de caixa cria sincronizacao pendente.
- [ ] Iniciar sincronizacao sem rede mantem pendente.
- [ ] Iniciar sincronizacao com sucesso marca concluida.
- [ ] Conflito aplica last-write-wins.
- [ ] Historico lista sincronizacoes anteriores.
- [ ] Endpoint de conflitos lista conflitos detetados.

## 7. Relatorios e Dashboard

Requisitos/casos de uso: RF-01, RF-02, RF-09, RNF-11, US-01, US-03, US-06, UC-06.

Estado atual: ha dados de vendas e fechos, mas faltam endpoints dedicados ao dashboard/relatorios/exportacao prometidos no relatorio.

Tarefas:

- [ ] Criar modulo/servico de relatorios.
- [ ] Implementar `GET /api/v1/dashboard`.
- [ ] Implementar `GET /api/v1/relatorios/vendas`.
- [ ] Implementar `GET /api/v1/relatorios/stock`.
- [ ] Implementar `GET /api/v1/relatorios/rentabilidade`.
- [ ] Implementar `POST /api/v1/relatorios/exportar`.
- [ ] Suportar filtros por loja, periodo e categoria.
- [ ] Calcular vendas por loja.
- [ ] Calcular margem/rentabilidade por produto e categoria.
- [ ] Agregar alertas de stock ativos.
- [ ] Exportar CSV.
- [ ] Exportar PDF, ou ajustar relatorio se PDF ficar fora do ambito.
- [ ] Garantir estrutura contabilistica minima: data, descricao, valor, IVA, loja.

Testes obrigatorios:

- [ ] Dashboard devolve KPIs com vendas, margem, lojas e alertas.
- [ ] Filtro por periodo altera resultados.
- [ ] Relatorio de rentabilidade calcula margem corretamente.
- [ ] Exportacao CSV contem colunas esperadas.
- [ ] Exportacao sem dados devolve resposta controlada.

## 8. Frontend Real

Requisitos/casos de uso: todos os UC com interface; RNF-10; RF-04 se for mantido.

Estado atual: frontend React tem rotas e paginas por perfil, mas usa `mockData`.

Tarefas:

- [ ] Instalar dependencias com `npm ci` ou `npm install`.
- [ ] Confirmar `npm run build` e `npm run lint`.
- [ ] Criar cliente HTTP centralizado para `/api/v1`.
- [ ] Implementar login real com `/auth/login`.
- [ ] Guardar token JWT e inclui-lo no header `Authorization`.
- [ ] Redirecionar utilizador para pagina inicial conforme perfil.
- [ ] Substituir `mockData` por chamadas reais progressivamente.
- [ ] Implementar estados de loading, erro e vazio.
- [ ] Implementar formularios funcionais:
  - [ ] PDV: iniciar venda, adicionar produto, remover linha, finalizar, emitir fatura.
  - [ ] Devolucao: localizar venda e registar devolucao.
  - [ ] Gerente: stock, fecho de caixa, ajustes, funcionarios.
  - [ ] Armazem: entrada de mercadoria, inventario fisico, localizacao/stock.
  - [ ] Gestor: dashboard, relatorios, fornecedores, encomendas, sincronizacao, utilizadores.
- [ ] Confirmar responsividade mobile para RF-04/US-05, se mantido no ambito.
- [ ] Adicionar protecao de rotas por perfil/permissao.
- [ ] Preparar fluxo offline do PDV, se o frontend tambem assumir operacao offline via browser.

Testes obrigatorios:

- [ ] Build frontend passa.
- [ ] Login real funciona.
- [ ] Rotas protegidas bloqueiam utilizador sem token.
- [ ] Fluxo PDV completo funciona contra backend.
- [ ] Dashboard mostra dados reais.
- [ ] Formulario de stock cria ajuste real.
- [ ] Interface nao depende de `mockData` nos fluxos principais.

## 9. Infraestrutura e Dados

Requisitos/decisoes: arquitetura local SQLite + central PostgreSQL, DA-01, DA-02, DA-03.

Estado atual: existe SQLite local e perfil PostgreSQL central. Docker Compose inclui PostgreSQL e frontend, mas nao backend.

Tarefas:

- [ ] Adicionar backend ao `docker-compose.yml`, ou documentar explicitamente que o backend corre fora do compose.
- [ ] Validar perfil `local` com SQLite.
- [ ] Validar perfil `central` com PostgreSQL.
- [ ] Garantir seed de dados suficiente para demonstracao e testes.
- [ ] Separar dados demo de dados de teste.
- [ ] Criar `.env.example` com `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
- [ ] Garantir que ficheiro SQLite e logs nao entram no commit se forem artefactos locais.
- [ ] Documentar comandos de arranque:
  - [ ] backend local;
  - [ ] backend central;
  - [ ] frontend;
  - [ ] docker compose.

Testes obrigatorios:

- [ ] App arranca com SQLite.
- [ ] App arranca com PostgreSQL.
- [ ] Swagger abre.
- [ ] Login demo funciona.
- [ ] Frontend consegue chamar backend.

## 10. Testes Automatizados

Requisitos/capitulo: Capitulo 6 do relatorio, verificacao SRS, cobertura, ISO/IEC 25010.

Estado atual: existem testes de dominio e facades; `mvn verify` passa. Ainda faltam testes de API, seguranca, integracao e sistema.

Tarefas:

- [ ] Manter todos os testes existentes verdes.
- [ ] Adicionar testes de controllers com MockMvc:
  - [ ] AuthController.
  - [ ] UtilizadoresController.
  - [ ] VendasController.
  - [ ] StockController.
  - [ ] EncomendasController.
  - [ ] InventariosController.
  - [ ] SincronizacaoController, quando existir.
  - [ ] RelatoriosController, quando existir.
- [ ] Adicionar testes de seguranca:
  - [ ] sem token da 401;
  - [ ] token valido permite;
  - [ ] permissao insuficiente da 403.
- [ ] Adicionar testes de integracao com base de dados de teste.
- [ ] Adicionar testes dos fluxos principais ponta-a-ponta a nivel API:
  - [ ] login -> venda -> fatura -> fecho -> sincronizacao;
  - [ ] alerta stock -> encomenda -> entrada mercadoria -> stock atualizado;
  - [ ] inventario -> discrepancia -> ajuste.
- [ ] Avaliar se PIT e jqwik vao mesmo ser usados. Se sim, adicionar dependencias/configuracao. Se nao, remover do relatorio.
- [ ] Subir threshold JaCoCo para cobrir mais do que `domain`, ou justificar porque apenas o dominio tem threshold.

Evidencias a recolher:

- [ ] Output de `mvn verify`.
- [ ] Screenshot/HTML do JaCoCo.
- [ ] Percentagens de cobertura global e por pacote.
- [ ] Lista de testes por RF/UC.

## 11. Verificacao Requisito a Requisito

Antes de fechar a implementacao fisica, criar uma matriz de rastreabilidade final.

Formato recomendado:

| Requisito | Estado | Implementacao | Testes | Observacoes |
|---|---|---|---|---|
| RF-01 | Parcial/Completo | Dashboard/Relatorios | DashboardControllerTest | Falta export PDF, se aplicavel |

Classificacao:

- `Completo`: funcionalidade implementada, integrada e testada.
- `Parcial`: existe parte da funcionalidade, mas falta integracao, detalhe ou teste.
- `Nao implementado`: nao existe comportamento executavel.
- `Fora do ambito`: remover/justificar no relatorio, se nao for entregue.

Requisitos que merecem atencao especial:

- [ ] RF-01/RF-02: dashboard e exportacao.
- [ ] RF-04: mobile/responsivo.
- [ ] RF-14/RNF-08/RD-01..RD-03: faturacao.
- [ ] RF-17/RNF-03: sincronizacao.
- [ ] RNF-01: desempenho PDV inferior a 2 segundos.
- [ ] RNF-02: disponibilidade local/offline.
- [ ] RNF-05: auditoria.
- [ ] RNF-06: comunicacao cifrada.
- [ ] RNF-10: usabilidade PDV apos treino de 30 minutos.
- [ ] RNF-11: export contabilistico.
- [ ] RD-06: horario de fornecedor.

## 12. Atualizacao do Relatorio

O relatorio deve refletir o estado real do codigo. Nao deixar promessas sem implementacao ou sem classificacao como trabalho futuro.

Tarefas:

- [ ] Completar "Revisao de codigo" com exemplos reais de problemas encontrados/corrigidos.
- [ ] Completar "Interface" com descricao do frontend implementado, nao apenas mockups.
- [ ] Completar "Testes Unitarios" com classes reais e exemplos.
- [ ] Completar "Testes de Integracao" com controllers/facades/API.
- [ ] Completar "Testes de Sistema" com fluxos ponta-a-ponta.
- [ ] Completar "Testes de Aceitacao" usando os criterios das User Stories.
- [ ] Inserir resultados JaCoCo reais.
- [ ] Decidir PIT/jqwik: implementar ou remover as referencias.
- [ ] Completar avaliacao ISO/IEC 25010 com evidencias reais.
- [ ] Completar verificacao da satisfacao dos SRS com matriz RF/RNF/RD.
- [ ] Atualizar diagramas se o codigo final divergir:
  - [ ] `SubAuditoria` como subsistema ou servico transversal.
  - [ ] endpoints reais.
  - [ ] sincronizacao real.
  - [ ] relatorios/exportacao.

## Ordem Recomendada de Execucao

Usar esta ordem para evitar dependencias partidas:

1. Alinhamento diagramas/relatorio/codigo.
2. Auditoria formal ou correcao do diagrama.
3. Utilizadores/autenticacao completa.
4. PDV e faturacao robustos.
5. Stock/alertas/inventario robustos.
6. Encomendas com regra RD-06 e entradas multi-linha.
7. Sincronizacao real.
8. Relatorios/dashboard/exportacao.
9. Frontend ligado a API.
10. Docker/configuracao final.
11. Testes automatizados por camadas.
12. Matriz SRS e atualizacao do relatorio.

## Definicao de Pronto

A implementacao fisica so deve ser considerada pronta para avancar para testes finais quando:

- [ ] `mvn verify` passa.
- [ ] `npm run build` passa.
- [ ] Backend arranca com perfil local SQLite.
- [ ] Backend arranca com perfil central PostgreSQL.
- [ ] Frontend autentica contra backend real.
- [ ] Fluxo PDV completo funciona sem mocks.
- [ ] Fecho de caixa agenda sincronizacao real.
- [ ] Dashboard/relatorios usam dados reais.
- [ ] Exportacao contabilistica existe ou esta explicitamente fora do ambito.
- [ ] Todos os Must Have estao `Completos` ou justificados no relatorio.
- [ ] A matriz RF/RNF/RD esta preenchida.
- [ ] O relatorio nao contem notas pendentes como "rever" ou "inserir depois".
