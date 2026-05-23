# Funcionalidades do Frontend que Nao Estao a Funcionar Totalmente

Este documento foi criado a partir do estado real do codigo, comparando chamadas do frontend com os endpoints/DTOs do backend e fazendo alguns pedidos reais a API. Nao se baseia nos checklists existentes.

Data da analise: 2026-05-23.

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

## Funcionalidades Parcialmente ou Nao Funcionais

### 1. Relatorios e Exportacao no Frontend

Estado atual:

- A pagina de relatorios carrega apenas `/relatorios/stock`.
- Os filtros de loja, periodo e categoria sao estaticos.
- Os botoes `Gerar relatorio`, `Exportar CSV` e `Exportar PDF` nao chamam a API.

Impacto:

- Nao e possivel gerar relatorios filtrados pelo frontend.
- Nao e possivel exportar CSV/PDF pela interface, apesar de o backend ter endpoint para exportacao.

Ficheiros relevantes:

- `frontend/src/components/pageSections.tsx`
- `backend/src/main/java/pt/miniFormiga/api/RelatoriosController.java`

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
- Depois de registar entrada, a pagina nao mostra historico de entradas nem atualiza claramente a encomenda apresentada.

Impacto:

- UC-08 so fica testavel se primeiro existir uma encomenda criada por API ou por uma sugestao no frontend.

Ficheiros relevantes:

- `frontend/src/pages/ArmazemPages.tsx`
- `backend/src/main/java/pt/miniFormiga/api/EntradasMercadoriaController.java`

### 5. Localizacao de Produto Nao Esta Exposta no Frontend

Estado atual:

- O backend tem `GET/PUT /api/v1/produtos/{id}/localizacao`.
- O frontend nao chama estes endpoints.
- Nao existe formulario para editar corredor e prateleira.

Impacto:

- RF-18/US-21 existem no backend, mas nao sao utilizaveis pela interface.

Ficheiros relevantes:

- `backend/src/main/java/pt/miniFormiga/api/ProdutoLocalizacaoController.java`
- `frontend/src/pages/ArmazemPages.tsx`

### 6. Alertas de Stock Nao Têm Ciclo de Vida na Interface

Estado atual:

- O backend suporta listar, marcar como lido e resolver alertas.
- O frontend apenas mostra uma indicacao derivada do stock/relatorio.
- Nao ha botoes para marcar alerta como lido ou resolvido.
- Nao ha formulario para definir nivel minimo diretamente a partir da vista de stock.

Impacto:

- RF-05 funciona parcialmente na UI: ve-se estado de reposicao, mas nao se gere o ciclo de vida do alerta.

Ficheiros relevantes:

- `frontend/src/components/pageSections.tsx`
- `backend/src/main/java/pt/miniFormiga/api/StockController.java`

### 7. Gestao de Funcionarios pelo Gerente Esta Incompleta

Estado atual:

- A pagina do gerente lista funcionarios.
- Os botoes `Novo funcionario` e `Editar` nao executam nenhuma acao.

Impacto:

- US-11/RF-10 estao mais completos no backend e na area do gestor, mas a interface do gerente nao permite criar/editar funcionarios da loja.

Ficheiro relevante:

- `frontend/src/pages/GerentePages.tsx`

### 8. Faturacao Completa Nao Esta Disponivel no PDV

Estado atual:

- Ao finalizar venda, o frontend chama `/vendas/{id}/fatura` com corpo vazio.
- Nao existem campos para NIF ou nome do cliente.

Impacto:

- E possivel emitir fatura simplificada.
- Nao e possivel emitir fatura completa pela interface quando o cliente pede NIF ou quando o valor exige fatura completa.

Ficheiros relevantes:

- `frontend/src/pages/FuncionarioPages.tsx`
- `backend/src/main/java/pt/miniFormiga/api/VendasController.java`

### 9. Devolucao de Produto E Pouco Utilizavel

Estado atual:

- A devolucao exige que o utilizador introduza manualmente o UUID da venda.
- A selecao de produto mostra IDs de produto, nao nomes.
- O frontend nao tem pesquisa por numero de fatura/recibo.
- Algumas falhas de devolucao nao sao tratadas com `try/catch`, podendo gerar erro silencioso ou erro no console.

Impacto:

- O backend suporta devolucao, mas o fluxo de utilizador no frontend ainda nao e adequado para uso real de PDV.

Ficheiro relevante:

- `frontend/src/pages/FuncionarioPages.tsx`

### 10. Sincronizacao So Mostra Historico

Estado atual:

- O frontend mostra historico de sincronizacoes.
- Nao existe botao para chamar `POST /sincronizacao/iniciar`.
- Nao existe vista para `GET /sincronizacao/conflitos`.
- Nao existe estado visual de tentativa/retry.

Impacto:

- UC-13 esta implementado no backend, mas a UI do gestor nao permite iniciar ou inspecionar a sincronizacao de forma completa.

Ficheiros relevantes:

- `frontend/src/pages/GestorPages.tsx`
- `backend/src/main/java/pt/miniFormiga/api/SincronizacaoController.java`

### 11. Inventario Fisico Nao Carrega Linhas Ja Registadas

Estado atual:

- A pagina de inventario lista stock e permite registar contagens.
- Quando existe inventario aberto, o frontend nao carrega as linhas ja registadas nem as discrepancias a partir de `/inventarios/{id}/discrepancias`.
- Ao recarregar a pagina, as contagens registadas podem desaparecer da vista local, embora existam no backend.

Impacto:

- UC-11 fica parcialmente funcional, mas a experiencia e inconsistente apos refresh/navegacao.

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
- dashboard;
- relatorio de stock;
- listagem de fornecedores;
- listagem de lojas;
- listagem de produtos;
- meios de pagamento;
- consulta de stock;
- historico de sincronizacao.

Isto nao significa que todos os fluxos estejam bons na interface, apenas que a API base respondeu.

## Prioridade Recomendada Para Corrigir

1. Melhorar mensagens de erro no frontend para mostrar `ApiError.message` em todas as acoes.
2. Corrigir relatorios: filtros reais e botoes de exportacao.
3. Completar encomendas manuais e condicoes comerciais.
4. Completar faturacao com NIF/nome no PDV.
5. Completar ciclo de alertas e definicao de nivel minimo.
6. Completar sincronizacao: iniciar, historico e conflitos.
7. Completar localizacao de produto.
8. Tornar devolucao pesquisavel por fatura/venda com nomes de produtos.
