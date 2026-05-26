# Relatório Técnico — Fase III (Relacionamento N:N)

> Texto pronto para colar no Word (pode manter os títulos/itens).

## Requisitos atendidos (resumo)

1) **Tabela intermediária com chave primária composta**: a tabela associativa é representada por `EmprestimoItem`, com chave composta **(idEmprestimo, idLivro)**.

2) **Acesso a partir de ambas as tabelas principais**:
- Do lado do empréstimo: listar livros de um empréstimo (`GET /api/emprestimos/itens?id=X`).
- Do lado do livro: listar empréstimos que contêm um livro (`GET /api/livros/emprestimos?idLivro=Y`).

3) **Estrutura de índice justificada**: Hash Extensível para relacionamento 1:N (múltiplos offsets por chave) e Árvore B+ para recuperação **ordenada**.

4) **Árvore B+ em consulta ordenada**: os itens de um empréstimo são retornados **ordenados por idLivro** via varredura de folhas (range scan) na B+.

5) **Mesmo padrão de cabeçalho e lápide**: arquivo `.dat` possui cabeçalho de 8 bytes e exclusão lógica por lápide.

6) **Sem interface via console**: interação via páginas HTML já existentes em `view/` + API HTTP.

---

## Formulário

### 1. Qual foi o relacionamento N:N escolhido e quais tabelas ele conecta?

**Relacionamento escolhido:** `Emprestimo` N:N `Livro`.

**Tabela intermediária (associativa):** `EmprestimoItem`.

- Um empréstimo pode conter vários livros.
- Um livro pode aparecer em vários empréstimos.

---

### 2. Qual estrutura de índice foi utilizada (B+ ou Hash Extensível)? Justifique a escolha.

Foram utilizadas **duas estruturas**, cada uma para um objetivo específico:

1) **Hash Extensível (principal para o relacionamento)**
- Usado para mapear:
  - `idEmprestimo -> [offsets dos vínculos]`
  - `idLivro -> [offsets dos vínculos]`
- **Justificativa:** relacionamento N:N gera naturalmente o caso **1:N do ponto de vista de um índice** (um empréstimo possui N livros; um livro aparece em N empréstimos). Hash Extensível é adequado para **buscas rápidas por igualdade** (acesso médio O(1)) e para retornar **múltiplos offsets** por chave sem varrer o arquivo inteiro.

2) **Árvore B+ (para consulta ordenada)**
- Usada para permitir listagem ordenada **sem ordenar em memória**.
- **Justificativa:** B+ é ideal quando se deseja **recuperar dados em ordem** (range scan / varredura ordenada nas folhas encadeadas).

---

### 3. Como foi implementada a chave composta da tabela intermediária?

A chave primária da tabela associativa é **composta**:

**PK = (idEmprestimo, idLivro)**

Isso garante que o mesmo livro não seja associado duas vezes ao mesmo empréstimo.

Observação de compatibilidade com a UI/API: para manter endpoints existentes que referenciam `itemId`, o sistema expõe um **id sintético** calculado:

`idSintetico = idEmprestimo * 1_000_000 + idLivro`

Esse id sintético não é a PK persistida; a PK persistida continua sendo a composta.

---

### 4. Como é feita a busca eficiente de registros por meio do índice?

- Para listar livros de um empréstimo:
  1) consulta no **Hash Extensível** por `idEmprestimo`
  2) obtém a lista de offsets
  3) lê diretamente do arquivo `.dat` apenas as posições necessárias

- Para listar empréstimos que contêm um livro:
  1) consulta no **Hash Extensível** por `idLivro`
  2) obtém offsets
  3) lê vínculos e agrega `idEmprestimo`
  4) busca os empréstimos correspondentes no arquivo principal de empréstimos

---

### 5. Como o sistema trata a integridade referencial (remoção/atualização) entre as tabelas?

- **Inserção:** ao adicionar um vínculo (idEmprestimo,idLivro), o sistema valida se:
  - o empréstimo existe
  - o livro existe
  - não existe vínculo duplicado (mesma PK composta)

- **Remoção (integridade em cascata soft):** ao excluir um empréstimo, o sistema executa exclusão lógica (lápide) para todos os vínculos associados na tabela intermediária.

- **Atualização:** por envolver PK composta, não é permitido alterar `idEmprestimo`/`idLivro` via update do vínculo; caso necessário, deve-se **remover e inserir novamente**. Atualização de `quantidade` é suportada.

---

### 6. Como foi organizada a persistência dos dados dessa nova tabela (mesmo padrão de cabeçalho e lápide)?

Arquivo de dados:
- `data/emprestimo_livros.dat`

Padrão:
- **Cabeçalho (8 bytes):** `[int ultimoId][int totalRegistros]`
  - `ultimoId` é mantido por compatibilidade de formato (PK é composta).
- **Registros de tamanho fixo:**
  - `lapide (boolean) + idEmprestimo (int) + idLivro (int) + quantidade (int)`
- **Exclusão lógica:** marcada por `lapide=true`.

Arquivos de índice:
- `data/emprestimo_livros.dat.idx` (hash por idEmprestimo)
- `data/emprestimo_livros.dat.livro.idx` (hash por idLivro)

---

### 7. Descreva como o código da tabela intermediária se integra com o CRUD das tabelas principais.

- Ao criar/editar empréstimos e livros, a tabela associativa não é automaticamente preenchida.
- A associação é feita via endpoints de itens do empréstimo:
  - `POST /api/emprestimos/itens?id=X` adiciona vínculo `(X,idLivro)`
  - `GET /api/emprestimos/itens?id=X` lista vínculos (ordenado por `idLivro`)
  - `DELETE /api/emprestimos/itens?itemId=...` remove vínculo (lápide)

- Para navegação pelo outro lado:
  - `GET /api/livros/emprestimos?idLivro=Y` lista os empréstimos que contêm o livro Y.

---

### 8. Descreva como está organizada a estrutura de diretórios e módulos no repositório após esta fase.

- `src/model/`: modelos (`Emprestimo`, `Livro`, `EmprestimoItem` etc.).
- `src/dao/`: persistência/índices (`EmprestimoItemDAOIndexado`, `HashExtensivel`, `ArvoreBPlus` etc.).
- `src/controller/`: regras de negócio e validações.
- `src/server/`: servidor HTTP e handlers REST (endpoints).
- `view/`: interface HTML (sem console).
- `data/`: arquivos `.dat` e índices `.idx`.

---

## Como comprovar rapidamente (para prints no relatório)

1) Criar empréstimo
- `POST /api/emprestimos`

2) Adicionar 2 livros fora de ordem (ex.: idLivro=3 e depois idLivro=2)
- `POST /api/emprestimos/itens?id=<idEmprestimo>`

3) Listar itens do empréstimo e verificar ordenação por idLivro
- `GET /api/emprestimos/itens?id=<idEmprestimo>`

4) Consultar empréstimos que contêm um livro
- `GET /api/livros/emprestimos?idLivro=2`

5) Excluir empréstimo e verificar cascata (itens somem)
- `DELETE /api/emprestimos?id=<idEmprestimo>`
