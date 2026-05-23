# Funcionalidades do Frontend que Nao Estao a Funcionar Totalmente

Este documento foi criado a partir do estado real do codigo, comparando chamadas do frontend com os endpoints/DTOs do backend e fazendo alguns pedidos reais a API. Nao se baseia nos checklists existentes.

Data da analise: 2026-05-23.

Ultima atualizacao: 2026-05-23, apos nova iteracao de implementacao, atualizacao da toolchain local e validacao de testes.

## Nota Sobre Autenticacao

A autenticacao demo funciona no backend com:

- `gestor.formiga`
- `gerente.braga`
- `operador.braga`
- `armazem.braga`

Password:

```text
MiniFormiga2026!
```

Se o login falhar no browser, a causa mais provavel nao e falta de `.env`. O backend tem valores por defeito. As causas mais provaveis sao:

- frontend aberto por uma origem nao permitida em CORS, por exemplo IP de rede diferente de `localhost`;
- backend local ainda a arrancar;
- frontend antigo em cache no browser;
- backend a correr com uma BD SQLite antiga sem seed demo.

## Funcionalidades Ainda Parcialmente ou Nao Funcionais

Resumo da iteracao mais recente:

- relatorios passaram a suportar tipos `STOCK`, `VENDAS` e `RENTABILIDADE`, filtros reais e exportacao CSV/PDF;
- stock passou a permitir gerir niveis minimos, alertas e localizacao de produto;
- sincronizacao passou a mostrar estado atual, historico, conflitos e acao de iniciar sincronizacao;
- PDV passou a recolher NIF/nome para fatura completa;
- gerente passou a criar/editar funcionarios da loja;
- inventario passou a recarregar discrepancias ja registadas.
- backend passou a expor corretamente `/api/swagger-ui/**` sem autenticacao, alem de `/api/docs`.
- `mvn clean verify`, `npm run lint`, `npm run build` e arranque Docker Compose foram validados com a toolchain local.

### 1. Relatorios e Exportacao no Frontend

Estado atual:

- A pagina de relatorios permite escolher `Stock`, `Vendas` ou `Rentabilidade`.
- Os filtros de loja, periodo e categoria sao carregados da API.
- `Gerar relatorio` chama o endpoint adequado: `/relatorios/stock`, `/relatorios/vendas` ou `/relatorios/rentabilidade`.
- `Exportar CSV` e `Exportar PDF` chamam `POST /relatorios/exportar` com autenticacao e descarregam o ficheiro devolvido.

Impacto residual:

- Falta teste manual/E2E em browser com backend local/central a correr.

Ficheiros relevantes:

- `frontend/src/components/pageSections.tsx`
- `frontend/src/lib/api.ts`
- `backend/src/main/java/pt/miniFormiga/api/RelatoriosController.java`
- `backend/src/main/java/pt/miniFormiga/api/CategoriasController.java`

### 2. Encomendas do Gestor Dependem de Sugestoes

Estado atual:

- A pagina de encomendas do gestor so permite criar encomenda a partir de sugestoes automaticas.
- Se nao existirem alertas de stock ativos, `/encomendas/sugestoes` devolve lista vazia.
- Nao existe formulario manual para escolher fornecedor, produto, quantidade e preco.

Impacto:

- Com dados demo normais, a pagina pode mostrar `0 produtos abaixo do minimo` e nao permite criar encomenda.
- O fluxo RF-03/UC-09 fica limitado quando nao ha sugestoes.

Ficheiros relevantes:

- `frontend/src/pages/GestorPages.tsx`
- `backend/src/main/java/pt/miniFormiga/api/EncomendasController.java`

### 3. Condicoes Comerciais de Fornecedores Nao Sao Geridas no Frontend

Estado atual:

- O backend tem endpoints para listar e criar condicoes comerciais:
  - `GET /fornecedores/{id}/condicoes`
  - `POST /fornecedores/{id}/condicoes`
- O frontend permite criar fornecedor, mas nao permite associar produtos nem definir preco, prazo ou quantidade minima.

Impacto:

- Uma encomenda valida pode falhar se o produto nao tiver condicao comercial associada ao fornecedor.
- O fluxo UC-14 nao esta completo na interface.

Ficheiros relevantes:

- `frontend/src/pages/GestorPages.tsx`
- `backend/src/main/java/pt/miniFormiga/api/FornecedoresController.java`

### 4. Entrada de Mercadoria Depende de Encomenda Existente

Estado atual:

- A pagina de armazem para entrada de mercadoria lista encomendas existentes e regista rececao multi-linha.
- Se nao houver encomendas criadas com linhas, nao ha forma util de testar a rececao pela interface.
- Depois de registar entrada, a pagina recarrega as encomendas, limpa quantidades locais e apresenta mensagem de sucesso/erro.

Impacto:

- UC-08 continua a depender de existir uma encomenda criada por API ou por uma sugestao no frontend.
- Continua sem historico dedicado de entradas de mercadoria na interface.

Ficheiros relevantes:

- `frontend/src/pages/ArmazemPages.tsx`
- `backend/src/main/java/pt/miniFormiga/api/EntradasMercadoriaController.java`

### 5. Localizacao de Produto Nao Esta Exposta no Frontend

Estado atual:

- O backend tem `GET/PUT /api/v1/produtos/{id}/localizacao`.
- A vista de stock chama estes endpoints.
- Existe formulario para selecionar produto e editar corredor/prateleira.

Impacto residual:

- Falta teste manual/E2E em browser para confirmar o fluxo completo com dados reais.

Ficheiros relevantes:

- `backend/src/main/java/pt/miniFormiga/api/ProdutoLocalizacaoController.java`
- `frontend/src/components/pageSections.tsx`

### 6. Alertas de Stock Nao Têm Ciclo de Vida na Interface

Estado atual:

- O backend suporta listar, marcar como lido e resolver alertas.
- A vista de stock lista alertas ativos da loja.
- Existem botoes para marcar alerta como lido e resolver.
- A tabela de stock permite editar e guardar nivel minimo por produto.

Impacto residual:

- Falta teste manual/E2E em browser para confirmar permissoes por perfil e atualizacao visual apos cada acao.

Ficheiros relevantes:

- `frontend/src/components/pageSections.tsx`
- `backend/src/main/java/pt/miniFormiga/api/StockController.java`

### 7. Gestao de Funcionarios pelo Gerente Esta Incompleta

Estado atual:

- A pagina do gerente lista funcionarios.
- O gerente pode criar funcionario para a sua loja.
- O gerente pode editar nome, email, perfil operacional, estado e password.

Impacto residual:

- Falta teste manual/E2E em browser para validar todas as combinacoes de perfis e permissoes.

Ficheiro relevante:

- `frontend/src/pages/GerentePages.tsx`

### 8. Faturacao Completa Nao Esta Disponivel no PDV

Estado atual:

- Ao finalizar venda, o frontend chama `/vendas/{id}/fatura` com `nifCliente` e `nomeCliente` quando preenchidos.
- Existem campos para NIF e nome do cliente no painel de finalizacao.

Impacto residual:

- Falta teste manual/E2E em browser para confirmar fatura completa em vendas acima do limite e com NIF.

Ficheiros relevantes:

- `frontend/src/pages/FuncionarioPages.tsx`
- `backend/src/main/java/pt/miniFormiga/api/VendasController.java`

### 9. Devolucao de Produto E Pouco Utilizavel

Estado atual:

- A devolucao exige que o utilizador introduza manualmente o UUID da venda.
- A selecao de produto mostra nomes de produto.
- O frontend nao tem pesquisa por numero de fatura/recibo.
- Falhas de pesquisa e devolucao sao tratadas com mensagens de erro.

Impacto:

- O backend suporta devolucao, mas o fluxo ainda nao e ideal para uso real de PDV porque falta pesquisa por numero de fatura/recibo.

Ficheiro relevante:

- `frontend/src/pages/FuncionarioPages.tsx`

### 10. Sincronizacao So Mostra Historico

Estado atual:

- O frontend mostra historico de sincronizacoes.
- Existe botao para chamar `POST /sincronizacao/iniciar`.
- Existe vista para `GET /sincronizacao/conflitos`.
- Existe estado visual da sincronizacao atual e proxima tentativa quando aplicavel.

Impacto residual:

- Falta teste manual/E2E em browser com backend local/central a correr.

Ficheiros relevantes:

- `frontend/src/pages/GestorPages.tsx`
- `backend/src/main/java/pt/miniFormiga/api/SincronizacaoController.java`

### 11. Inventario Fisico Nao Carrega Linhas Ja Registadas

Estado atual:

- A pagina de inventario lista stock e permite registar contagens.
- Quando existe inventario aberto, o frontend carrega discrepancias a partir de `/inventarios/{id}/discrepancias`.
- Ao iniciar inventario, registar contagem ou fechar inventario, a pagina apresenta feedback de sucesso/erro.

Impacto:

- UC-11 ficou mais consistente para discrepancias.
- Linhas com discrepancia zero ainda nao sao reidratadas apos refresh porque o backend so expoe `/discrepancias`; para resolver totalmente, falta endpoint/listagem de todas as linhas do inventario ou resposta completa no DTO.

Ficheiro relevante:

- `frontend/src/pages/ArmazemPages.tsx`

### 12. Pesquisa por Codigo de Barras Nao Usa Endpoint Dedicado

Estado atual:

- O backend tem `GET /produtos/barcode/{codigo}`.
- O frontend carrega todos os produtos e faz pesquisa local em memoria.

Impacto:

- Funciona com poucos produtos demo.
- Nao escala bem e nao valida o comportamento real de leitura de codigo de barras contra a API.

Ficheiros relevantes:

- `frontend/src/pages/FuncionarioPages.tsx`
- `backend/src/main/java/pt/miniFormiga/api/ProdutosController.java`

### 13. Estados de Erro Ainda Sao Inconsistentes

Estado atual:

- Algumas paginas mostram mensagem amigavel.
- Outras ignoram erros com `catch(() => undefined)` ou limpam a lista sem explicar a causa.
- Algumas acoes nao usam `try/catch`.

Impacto:

- Para quem testa no frontend, varias falhas parecem "nada aconteceu".
- Isto dificulta distinguir erro de permissao, erro de validacao e falta de dados.

Ficheiros relevantes:

- `frontend/src/pages/GestorPages.tsx`
- `frontend/src/pages/GerentePages.tsx`
- `frontend/src/pages/ArmazemPages.tsx`
- `frontend/src/pages/FuncionarioPages.tsx`

## Funcionalidades que Parecem Funcionar no Backend

Os seguintes endpoints responderam corretamente em testes rapidos com os utilizadores demo:

- login com JWT;
- login local com `operador.braga` contra o backend local;
- login central com `gestor.formiga` contra o backend central;
- dashboard;
- relatorio de stock;
- listagem de fornecedores;
- listagem de lojas;
- listagem de produtos;
- meios de pagamento;
- consulta de stock;
- historico de sincronizacao.

O frontend tambem respondeu 200 dentro da rede Docker Compose. Isto nao significa que todos os fluxos estejam bons na interface, apenas que a API base e o arranque integrado responderam.

## Prioridade Recomendada Para Corrigir

1. Melhorar mensagens de erro no frontend para mostrar `ApiError.message` em todas as acoes.
2. Completar encomendas manuais e condicoes comerciais de fornecedores.
3. Criar historico dedicado para entradas de mercadoria na interface.
4. Tornar devolucao pesquisavel por numero de fatura/recibo.
5. Usar o endpoint dedicado de codigo de barras no PDV.
6. Fazer testes E2E/browser dos fluxos principais e responsividade mobile.
