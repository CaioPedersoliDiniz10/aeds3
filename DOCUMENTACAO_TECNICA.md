# BiblioSystem - Documentação Técnica
## Fase 2 do Trabalho Prático de AED III

---

## 1. Visão Geral do Projeto

**BiblioSystem** é um sistema de gerenciamento de biblioteca implementado em Java com arquitetura MVC (Model-View-Controller) e persistência em arquivos binários. O sistema oferece:

- CRUD completo para Usuários, Livros, Empréstimos e Cupons
- Índices com **Hash Extensível** para otimização de buscas no relacionamento 1:N
- Interface web (HTML/CSS) via servidor HTTP embarcado
- Exclusão lógica por lápide (lapidação)
- Validação completa de entradas
- Persistência em disco com sincronização

---

## 2. Estrutura de Representação dos Registros

### 2.1 Formato Binário com Tamanho Fixo

Todos os registros são armazenados em **tamanho fixo** para permitir acesso direto via offset:

```
Usuario.TAMANHO    = 205 bytes
Livro.TAMANHO      = 259 bytes
Emprestimo.TAMANHO = 33 bytes
EmprestimoItem.TAMANHO = 17 bytes
Cupom.TAMANHO      = 137 bytes
```

**Estrutura de arquivo binário:**
```
[CABEÇALHO (8 bytes)]
  - ultimoId (4 bytes): int - maior ID gerado até agora
  - totalRegistros (4 bytes): int - quantidade de registros inseridos

[REGISTROS (n * TAMANHO_FIXO bytes)]
  - Sequência de registros de tamanho fixo
  - Cada registro contém: lapide (1 byte) + campos específicos
```

**Vantagens:**
- Acesso O(1) direto a um registro pela posição
- Facilita persistência sem fragmentação
- Simplifica serialização/desserialização

### 2.2 Serialização de Campos

**Tipos de dados persistidos:**
- `boolean lapide` (1 byte): marca exclusão lógica
- `int id` (4 bytes): identificador único
- Strings fixas: armazenadas em UTF-8 com padding
- `int, double`: tipos numéricos primitivos

**Exemplo - Usuario:**
```
[lapide(1)] [id(4)] [nome(100)] [email(100)]
```

---

## 3. Tratamento de Atributos Multivalorados (Strings)

### Solução Implementada

Strings são armazenadas com **tamanho fixo pré-definido** usando UTF-8:

```java
private static void writeFixedString(DataOutputStream dos, String s, int len) throws IOException {
    byte[] src = (s == null ? "" : s).getBytes("UTF-8");
    byte[] buf = new byte[len];
    System.arraycopy(src, 0, buf, 0, Math.min(src.length, len));
    dos.write(buf);
}

private static String readFixedString(DataInputStream dis, int len) throws IOException {
    byte[] buf = new byte[len];
    dis.readFully(buf);
    return new String(buf, "UTF-8").trim();
}
```

**Características:**
- Se string > tamanho máximo: truncada
- Se string < tamanho máximo: preenchida com zeros
- Ao ler: remove espaços em branco (`trim()`)
- Suporta caracteres especiais UTF-8

---

## 4. Exclusão Lógica (Lápide)

### Implementação

Todo registro possui um campo `boolean lapide`:
- `lapide = false`: registro ativo
- `lapide = true`: registro excluído logicamente

**Operação de exclusão:**
```java
public boolean excluir(int id) throws IOException {
    // ... encontra registro ...
    u.setLapide(true);      // Marca como deletado
    raf.seek(pos);
    u.serializar(dos);       // Reescreve no mesmo local
    return true;
}
```

**Benefícios:**
- Não requer reorganização de arquivo
- Preserva ofsets dos demais registros
- Permite recuperação de dados
- Operação O(1) de busca + atualização

**Operação de busca:**
```java
if (!u.isLapide() && u.getId() == id) {
    return u;  // Retorna apenas registros ativos
}
```

---

## 5. Chaves Secundárias Utilizadas

Além das chaves primárias (ID), foram implementadas buscas por:

### 5.1 Chave Secundária em Usuario
- **Email** (único): `emailExiste(String email)` - O(n)

### 5.2 Chave Secundária em Livro
- **Título + Autor** (único): `livroExiste(String titulo, String autor)` - O(n)

### 5.3 Chave Secundária em Emprestimo
- **idEmprestimo** em EmprestimoItem: indexado com **Hash Extensível** - O(1) médio

### 5.4 Chave Secundária em Cupom
- **idEmprestimo** (único): `buscarPorEmprestimo(int idEmprestimo)` - O(n)

---

## 6. Implementação de Hash Extensível para Chaves de Pesquisa

### 6.1 Algoritmo de Hash Extensível

**Classe:** `HashExtensivel.java`

**Características:**
- Crescimento **dinâmico** de buckets conforme necessário
- Profundidade global: controla número de buckets (2^profundidadeGlobal)
- Cada bucket pode conter múltiplos pares chave-valor
- Implementação serializable para persistência em disco

**Estrutura de dados:**
```java
private int globalDepth;                    // Profundidade global do hash
private Map<Integer, Bucket>[] directory;  // Diretório de buckets
```

### 6.2 Operações Principais

**Inserção:**
```java
public void inserir(int chave, long valor) {
    int hash = obterHash(chave);
    int indice = hash % directory.length;
    Map<Integer, Bucket> bucket = directory[indice];
    // Adiciona par chave-valor ao bucket
}
```

**Busca:**
```java
public List<Long> buscar(int chave) {
    // Retorna todos os valores (offsets) associados a uma chave
    // Tempo: O(1) médio
}
```

**Remoção:**
```java
public boolean remover(int chave, long valor) {
    // Remove um valor específico de uma chave
}
```

### 6.3 Persistência do Índice

O índice é salvo em arquivo `.idx` (ex: `emprestimo_itens.dat.idx`):

```java
public void salvar(String caminhoArquivo) throws IOException {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(caminhoArquivo))) {
        oos.writeInt(globalDepth);
        oos.writeObject(directory);  // Serializa estrutura inteira
    }
}
```

**Reconstrução automática:**
- Se arquivo `.idx` não existe, é reconstruído lendo todos os registros
- Garante consistência entre dados e índice

---

## 7. Relacionamento 1:N com Hash Extensível

### 7.1 Estrutura: Emprestimo → EmprestimoItem → Livro

```
Um Emprestimo tem MÚLTIPLOS EmprestimoItems
Um EmprestimoItem referencia UMA Livro
```

### 7.2 Implementação Otimizada

**Classe:** `EmprestimoItemDAOIndexado.java`

Utiliza Hash Extensível para indexar por `idEmprestimo`:

```java
public List<EmprestimoItem> buscarPorEmprestimo(int idEmprestimo) throws IOException {
    List<Long> posicoes = indiceEmprestimo.buscar(idEmprestimo);  // O(1)
    // Acessa diretamente os offsets dos itens
    for (long pos : posicoes) {
        // Lê item no offset específico
    }
}
```

### 7.3 Comparação de Desempenho

**Sem índice (busca linear):**
- Tempo: **O(n)** onde n = total de registros
- 100 empréstimos com 5 itens cada = 500 registros a verificar

**Com Hash Extensível:**
- Tempo: **O(1)** médio
- Acesso direto aos offsets

### 7.4 Integridade Referencial

**Validações implementadas:**

```java
// Ao adicionar item
if (emprestimoDAO.buscarPorId(idEmprestimo) == null)
    throw new IllegalArgumentException("Empréstimo inexistente");

if (livroDAO.buscarPorId(idLivro) == null)
    throw new IllegalArgumentException("Livro inexistente");

// Evita duplicação
for (EmprestimoItem existente : itemDAO.buscarPorEmprestimo(idEmprestimo)) {
    if (existente.getIdLivro() == idLivro)
        throw new IllegalArgumentException("Livro já adicionado");
}
```

**Navegação:**
```
Emprestimo (ID) → EmprestimoItem (idEmprestimo) 
              ↓
            Livro (ID = idLivro)
```

---

## 8. Armazenamento de Índices em Disco

### 8.1 Estrutura de Arquivos

```
data/
├── usuarios.dat           # Arquivo de dados
├── livros.dat
├── emprestimos.dat
├── emprestimo_itens.dat   # Dados principal
├── emprestimo_itens.dat.idx  # Índice Hash Extensível
├── cupons.dat
```

### 8.2 Formato de Persistência do Índice

**Arquivo `.idx` (serialização Java):**

```
[ObjectStream Header]
[globalDepth (4 bytes)]
[Directory structure - serializado com ObjectOutputStream]
  └── Map<Integer, Bucket>[] com todos os pares chave-valor
```

### 8.3 Sincronização

**Atualização automática ao modificar dados:**

```java
public EmprestimoItem criar(EmprestimoItem item) throws IOException {
    // ... insere no arquivo ...
    indiceEmprestimo.inserir(item.getIdEmprestimo(), pos);
    salvarIndice();  // Persiste imediatamente
}

public boolean excluir(int id) throws IOException {
    // ... marca como lapide ...
    indiceEmprestimo.remover(item.getIdEmprestimo(), pos);
    salvarIndice();  // Persiste imediatamente
}
```

**Vantagens:**
- Índice sempre em sincronismo com dados
- Recuperação de falhas: reconstrução automática
- Sem necessidade de reorganização de arquivo

---

## 9. Validação de Entradas

### 9.1 Erros Tratados

| Erro | Local | Mensagem |
|------|-------|----------|
| PK Duplicada (Email) | UsuarioController | "E-mail já cadastrado" |
| PK Duplicada (Título+Autor) | LivroController | "Livro com este título e autor já existe" |
| Registro Inexistente | Controllers | "[Entidade] com ID [X] não encontrado" |
| Campo Obrigatório Vazio | Controllers | "[Campo] é obrigatório" |
| E-mail Inválido | UsuarioController | "E-mail inválido" |
| Ano Inválido | LivroController | "Ano inválido (1000-2100)" |
| Quantidade <= 0 | EmprestimoController | "Quantidade deve ser > 0" |
| Livro Duplicado no Empréstimo | EmprestimoController | "Este livro já está neste empréstimo" |
| Cupom Duplicado | EmprestimoController | "Este empréstimo já possui um cupom" |
| Tipo de Cupom Inválido | EmprestimoController | "Tipo inválido (DESCONTO ou EXTENSAO)" |

### 9.2 Tratamento de Erros HTTP

**Códigos de status retornados:**

```
201 Created   - Criação bem-sucedida
200 OK        - Operação bem-sucedida
400 Bad Request - Erro de validação (IllegalArgumentException)
404 Not Found - Recurso inexistente (implícito pelo erro)
405 Method Not Allowed - Método HTTP não suportado
500 Internal Server Error - Erro não tratado
```

---

## 10. Arquitetura do Projeto no GitHub

### 10.1 Estrutura de Pastas

```
aeds3/
├── src/
│   ├── model/              # Entidades (Model)
│   │   ├── Usuario.java
│   │   ├── Livro.java
│   │   ├── Emprestimo.java
│   │   ├── EmprestimoItem.java
│   │   └── Cupom.java
│   ├── dao/                # Acesso a dados (Data Access Object)
│   │   ├── UsuarioDAO.java
│   │   ├── LivroDAO.java
│   │   ├── EmprestimoDAO.java
│   │   ├── EmprestimoItemDAOIndexado.java
│   │   ├── HashExtensivel.java  # Índice 1:N
│   │   └── CupomDAO.java
│   ├── controller/         # Lógica de negócios (Controller)
│   │   ├── UsuarioController.java
│   │   ├── LivroController.java
│   │   └── EmprestimoController.java
│   └── server/             # Servidor HTTP (View)
│       ├── AppServer.java
│       ├── UsuarioHandler.java
│       ├── LivroHandler.java
│       ├── EmprestimoHandler.java
│       └── StaticHandler.java
├── view/                   # Interface web (HTML/CSS)
│   ├── index.html
│   ├── usuarios.html
│   ├── livros.html
│   ├── emprestimos.html
│   ├── cupons.html
│   ├── style.css
│   └── livereload.js
├── data/                   # Arquivos binários (gerados em execução)
│   ├── usuarios.dat
│   ├── livros.dat
│   ├── emprestimos.dat
│   ├── emprestimo_itens.dat
│   ├── emprestimo_itens.dat.idx
│   └── cupons.dat
├── bin/                    # Classes compiladas
├── run.bat                 # Script de execução (Windows)
├── dev.ps1                 # Script de desenvolvimento (PowerShell)
├── README.md               # Instruções de execução
└── DOCUMENTACAO_TECNICA.md # Este arquivo
```

### 10.2 Padrão de Arquitetura: MVC + DAO

```
┌─────────────┐
│   Frontend  │  (HTML/CSS + JavaScript)
│  (view/)    │
└──────┬──────┘
       │ HTTP REST
       ↓
┌──────────────────────────────────────┐
│     Server (HTTP Handlers)           │
│  - StaticHandler: arquivos estáticos │
│  - UsuarioHandler: /api/usuarios     │
│  - LivroHandler: /api/livros         │
│  - EmprestimoHandler: /api/...       │
└──────┬───────────────────────────────┘
       │ Delegação
       ↓
┌──────────────────────────────────────┐
│     Controllers (Lógica)             │
│  - UsuarioController                 │
│  - LivroController                   │
│  - EmprestimoController              │
└──────┬───────────────────────────────┘
       │ Operações
       ↓
┌──────────────────────────────────────┐
│     DAOs (Persistência)              │
│  - UsuarioDAO                        │
│  - LivroDAO                          │
│  - EmprestimoDAO                     │
│  - EmprestimoItemDAOIndexado         │
│  - CupomDAO                          │
└──────┬───────────────────────────────┘
       │ Acesso
       ↓
┌──────────────────────────────────────┐
│     Models (Entidades)               │
│  - Usuario, Livro, Emprestimo, etc   │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│   Arquivos Binários (data/)          │
│   + Índices (.idx)                   │
└──────────────────────────────────────┘
```

### 10.3 Benefícios da Arquitetura

- **Separação de responsabilidades:** cada camada tem um propósito
- **Reutilização:** Controllers podem ser usados por diferentes interfaces
- **Testabilidade:** DAOs podem ser mockados
- **Manutenibilidade:** mudanças em persistência não afetam lógica

---

## 11. Endpoints REST da API

### Usuários
```
GET    /api/usuarios              # Listar todos
GET    /api/usuarios?id=1         # Buscar por ID
POST   /api/usuarios              # Criar (nome, email)
PUT    /api/usuarios?id=1         # Atualizar (nome, email)
DELETE /api/usuarios?id=1         # Excluir
```

### Livros
```
GET    /api/livros                # Listar todos
GET    /api/livros?id=1           # Buscar por ID
POST   /api/livros                # Criar (titulo, autor, ano)
PUT    /api/livros?id=1           # Atualizar
DELETE /api/livros?id=1           # Excluir
```

### Empréstimos
```
GET    /api/emprestimos                    # Listar todos
GET    /api/emprestimos?id=1               # Buscar por ID
POST   /api/emprestimos                    # Criar (idUsuario, dataEmprestimo)
PUT    /api/emprestimos?id=1               # Atualizar datas
DELETE /api/emprestimos?id=1               # Excluir

GET    /api/emprestimos/itens?id=1         # Listar itens do empréstimo
POST   /api/emprestimos/itens?id=1         # Adicionar livro (idLivro, quantidade)
DELETE /api/emprestimos/itens?itemId=1     # Remover item

GET    /api/emprestimos/cupom?id=1         # Buscar cupom
POST   /api/emprestimos/cupom?id=1         # Associar cupom (tipo, descricao, valor)
```

---

## 12. Compilação e Execução

### Requisitos
- Java 11+
- Windows/Linux/MacOS

### Compilação
```bash
# Automático (Windows)
run.bat

# Manual (Multiplataforma)
javac -encoding UTF-8 -d bin src/model/*.java src/dao/*.java src/controller/*.java src/server/*.java
```

### Execução
```bash
java -cp bin server.AppServer
```

**Saída esperada:**
```
=== Servidor iniciado em http://localhost:8080 ===
Acesse o sistema no navegador.
```

### Acesso
```
URL: http://localhost:8080
Porta: 8080
```

---

## 13. Decisões de Projeto

### 13.1 Por que Tamanho Fixo de Registros?
- ✅ Permite acesso O(1) por offset
- ✅ Simplifica serialização
- ✅ Evita fragmentação de arquivo
- ❌ Desperdiça espaço com strings curtas

### 13.2 Por que Hash Extensível?
- ✅ O(1) médio para buscas secundárias
- ✅ Crescimento dinâmico sem reorganização
- ✅ Melhor do que tabela hash fixa para dados dinâmicos
- ❌ Mais complexo que hash simples

### 13.3 Por que Exclusão Lógica?
- ✅ Sem necessidade de reorganizar arquivo
- ✅ Recuperação de dados possível
- ✅ Mantém offsets consistentes
- ❌ Arquivo cresce sem limite

### 13.4 Por que Servidor HTTP Embarcado?
- ✅ Interface web sem configuração extra
- ✅ Simplicidade de deployment
- ✅ Fácil testes via navegador
- ❌ Menos recursos que servidor profissional

---

## 14. Conclusão

**BiblioSystem** demonstra implementação prática de:
- ✅ CRUD completo com persistência
- ✅ Índices secundários eficientes (Hash Extensível)
- ✅ Relacionamento 1:N otimizado
- ✅ Validação robusta de dados
- ✅ Interface web funcional
- ✅ Arquitetura MVC escalável

O sistema atende todos os requisitos da Fase 2 do TP de AED III.
