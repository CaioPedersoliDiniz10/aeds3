# BiblioSystem – Sistema de Gerenciamento de Biblioteca
## Fase 2 - AED III

### ⭐ Resumo
Sistema **CRUD completo** de biblioteca com **Hash Extensível** para otimização de relacionamento 1:N. Implementa arquitetura **MVC**, persistência em **arquivos binários com tamanho fixo** e interface **web responsiva**.

---

## 🚀 Quick Start

### Pré-requisitos
- **Java 11+** (instale do [java.oracle.com](https://java.oracle.com) ou use OpenJDK)
- **Git** (opcional, para clonar repositório)

### Execução Rápida

#### Windows
```bash
run.bat
```

#### Linux/MacOS
```bash
mkdir -p bin
javac -encoding UTF-8 -d bin src/model/*.java src/dao/*.java src/controller/*.java src/server/*.java
java -cp bin server.AppServer
```

**Após iniciar:**
```
Abra no navegador: http://localhost:8080
```

---

## 📋 Funcionalidades

### CRUD Completo
- ✅ **Usuários**: Criar, buscar, listar, atualizar, deletar (com validação de email)
- ✅ **Livros**: Criar, buscar, listar, atualizar, deletar (com validação título+autor)
- ✅ **Empréstimos**: Criar, buscar, listar, atualizar, deletar
- ✅ **Empréstimo Itens**: Adicionar/remover livros de empréstimos (com índice otimizado)
- ✅ **Cupons**: Associar descontos/extensões a empréstimos

### Recursos Avançados
- 🔍 **Hash Extensível** para buscas O(1) em relacionamento 1:N
- 💾 **Persistência em disco** com sincronização automática
- 🔐 **Exclusão lógica (lápide)** sem reorganização de arquivos
- ✔️ **Validação robusta** de entradas (chaves únicas, campos obrigatórios)
- 📊 **Índices persistidos** em arquivos `.idx` para recuperação rápida

---

## 🏗️ Arquitetura

### Camadas do Projeto

```
┌─────────────────────────────────────┐
│   Frontend (HTML/CSS/JavaScript)    │
│   view/ - Interface web responsiva  │
└────────────┬────────────────────────┘
             │ HTTP REST
             ↓
┌─────────────────────────────────────┐
│   Handlers (Servidor HTTP)          │
│   server/ - Endpoints REST          │
└────────────┬────────────────────────┘
             │
             ↓
┌─────────────────────────────────────┐
│   Controllers (Lógica de Negócio)   │
│   controller/ - Validações          │
└────────────┬────────────────────────┘
             │
             ↓
┌─────────────────────────────────────┐
│   DAOs (Acesso a Dados)             │
│   dao/ - Persistência + Índices     │
└────────────┬────────────────────────┘
             │
             ↓
┌─────────────────────────────────────┐
│   Arquivos Binários (data/)         │
│   .dat (dados) + .idx (índices)     │
└─────────────────────────────────────┘
```

### Estrutura de Pastas

```
aeds3/
├── src/
│   ├── model/                     # Entidades
│   │   ├── Usuario.java
│   │   ├── Livro.java
│   │   ├── Emprestimo.java
│   │   ├── EmprestimoItem.java
│   │   └── Cupom.java
│   ├── dao/                       # Persistência
│   │   ├── UsuarioDAO.java
│   │   ├── LivroDAO.java
│   │   ├── EmprestimoDAO.java
│   │   ├── EmprestimoItemDAOIndexado.java  ⭐ com Hash Extensível
│   │   ├── HashExtensivel.java   ⭐ Índice 1:N
│   │   └── CupomDAO.java
│   ├── controller/                # Lógica
│   │   ├── UsuarioController.java
│   │   ├── LivroController.java
│   │   └── EmprestimoController.java
│   └── server/                    # API HTTP
│       ├── AppServer.java
│       ├── UsuarioHandler.java
│       ├── LivroHandler.java
│       ├── EmprestimoHandler.java
│       └── StaticHandler.java
├── view/                          # Frontend
│   ├── index.html
│   ├── usuarios.html
│   ├── livros.html
│   ├── emprestimos.html
│   ├── cupons.html
│   ├── style.css
│   └── livereload.js
├── data/                          # Dados persistidos (gerado em runtime)
│   ├── usuarios.dat
│   ├── livros.dat
│   ├── emprestimos.dat
│   ├── emprestimo_itens.dat
│   ├── emprestimo_itens.dat.idx  ← Índice Hash Extensível
│   └── cupons.dat
├── bin/                           # Classes compiladas (gerado)
├── run.bat                        # Script de execução Windows
└── DOCUMENTACAO_TECNICA.md        # Documentação detalhada
```

---

## 📚 Endpoints API

### Usuários
```http
GET    /api/usuarios              # Listar todos
GET    /api/usuarios?id=1         # Buscar por ID
POST   /api/usuarios              # Criar (body: nome=...&email=...)
PUT    /api/usuarios?id=1         # Atualizar
DELETE /api/usuarios?id=1         # Excluir (lápide)
```

### Livros
```http
GET    /api/livros                # Listar todos
GET    /api/livros?id=1           # Buscar por ID
POST   /api/livros                # Criar (title, author, year)
PUT    /api/livros?id=1           # Atualizar
DELETE /api/livros?id=1           # Excluir (lápide)
```

### Empréstimos
```http
GET    /api/emprestimos                    # Listar
GET    /api/emprestimos?id=1               # Buscar
POST   /api/emprestimos                    # Criar
PUT    /api/emprestimos?id=1               # Atualizar
DELETE /api/emprestimos?id=1               # Excluir

GET    /api/emprestimos/itens?id=1         # Listar itens
POST   /api/emprestimos/itens?id=1         # Adicionar livro
DELETE /api/emprestimos/itens?itemId=1     # Remover item

GET    /api/emprestimos/cupom?id=1         # Buscar cupom
POST   /api/emprestimos/cupom?id=1         # Associar cupom
```

---

## 🔧 Compilação Manual

```bash
# Criar diretório de saída
mkdir -p bin

# Compilar (requer Java 11+)
javac -encoding UTF-8 -d bin \
  src/model/*.java \
  src/dao/*.java \
  src/controller/*.java \
  src/server/*.java

# Executar
java -cp bin server.AppServer
```

---

## 🗄️ Estrutura de Dados

### Tamanho de Registros (bytes)
```
Usuario.TAMANHO       = 205  (lapide + id + nome[100] + email[100])
Livro.TAMANHO         = 259  (lapide + id + titulo[150] + autor[100] + ano)
Emprestimo.TAMANHO    = 33   (lapide + id + idUsuario + data[2x12])
EmprestimoItem.TAMANHO = 17  (lapide + id + idEmprestimo + idLivro + qtd)
Cupom.TAMANHO         = 137  (lapide + id + idEmprestimo + tipo[20] + desc[100] + valor)
```

### Persistência
- **Formato**: Binário com tamanho fixo (acesso O(1))
- **Cabeçalho**: ultimoId (4 bytes) + totalRegistros (4 bytes)
- **Índices**: Serializados em arquivos `.idx`
- **Exclusão**: Lógica via `lapide` boolean

---

## ⚙️ Validações

| Erro | Solução |
|------|---------|
| Email duplicado | Use outro email |
| Livro duplicado | Título+Autor devem ser únicos |
| ID não encontrado | Verifique IDs válidos |
| Campo vazio | Preencha campos obrigatórios |
| Ano inválido | Use 1000-2100 |
| Livro já no empréstimo | Cada livro uma vez por empréstimo |
| Cupom duplicado | Um cupom por empréstimo |

---

## 📖 Documentação Técnica Completa

Veja [DOCUMENTACAO_TECNICA.md](DOCUMENTACAO_TECNICA.md) para:
- Implementação de Hash Extensível
- Estrutura binária em detalhes
- Estratégia de índices
- Decisões de projeto

---

## 🐛 Troubleshooting

**Erro: "Servidor já em uso"**
- Porta 8080 ocupada. Mata processo: `lsof -i :8080` ou altere porta em `AppServer.java`

**Erro: "Java não encontrado"**
- Instale Java 11+: [java.oracle.com](https://java.oracle.com)
- Adicione ao PATH do Windows ou use caminho completo

**Dados não persistem**
- Verifique permissões da pasta `data/`
- Pasta é criada automaticamente na primeira execução

---

## 📝 Notas de Desenvolvimento

- Frontend com vanilla JS (sem dependências)
- Servidor HTTP embarcado (com.sun.net.httpserver)
- CORS habilitado para facilitar testes
- Hot-reload de dados entre execuções

---

## 👨‍💻 Autor

Desenvolvido como Trabalho Prático - Fase 2 de AED III  
Pontifícia Universidade Católica de Minas Gerais

---

## 📄 Licença

Projeto educacional - PUC Minas 2024
        ├── LivroHandler.java
        └── EmprestimoHandler.java
```

---

## Arquitetura (MVC + DAO)

```
View (HTML/CSS)
      │  HTTP (fetch API)
      ▼
 HTTP Server (AppServer)
      │
      ├── UsuarioHandler
      ├── LivroHandler
      └── EmprestimoHandler
              │
              ▼
        Controllers
   (regras de negócio)
              │
              ▼
           DAOs
   (arquivo binário)
              │
              ▼
         data/*.dat
   (cabeçalho + lápide)
```

---

## Formato dos Arquivos Binários

Cada arquivo `.dat` possui:
- **Cabeçalho (8 bytes):** `[int ultimoId][int totalRegistros]`
- **Registros de tamanho fixo:** cada registro começa com `boolean lápide` (1 = excluído logicamente)

---

## Diagrama Entidade-Relacionamento (DER)

```
USUARIO (1) ──────< (N) EMPRESTIMO (N) >────── (1) LIVRO
                           │
                          (1)
                           │
                          (1)
                         CUPOM
```

- Um Usuário realiza muitos Empréstimos  
- Um Livro pode ser emprestado muitas vezes  
- Um Empréstimo pode ter no máximo um Cupom  

---

## Endpoints da API REST

### Usuários `/api/usuarios`
| Método | Parâmetros | Ação |
|--------|-----------|------|
| GET | — | Listar todos os usuários ativos |
| GET | `?id=X` | Buscar usuário por ID |
| POST | body: `nome`, `email` | Cadastrar novo usuário |
| PUT | `?id=X`, body: `nome`, `email` | Atualizar usuário |
| DELETE | `?id=X` | Excluir logicamente |

### Livros `/api/livros`
| Método | Parâmetros | Ação |
|--------|-----------|------|
| GET | — | Listar todos os livros ativos |
| GET | `?id=X` | Buscar livro por ID |
| POST | body: `titulo`, `autor`, `ano` | Cadastrar novo livro |
| PUT | `?id=X`, body: `titulo`, `autor`, `ano` | Atualizar livro |
| DELETE | `?id=X` | Excluir logicamente |

### Empréstimos `/api/emprestimos`
| Método | Parâmetros | Ação |
|--------|-----------|------|
| GET | — | Listar empréstimos ativos |
| GET | `?id=X` | Buscar por ID |
| POST | body: `idUsuario`, `idLivro`, `dataEmprestimo`, `dataDevolucao` | Criar empréstimo |
| PUT | `?id=X`, body: datas | Atualizar datas |
| DELETE | `?id=X` | Excluir logicamente |
| GET | `/cupom?id=X` | Buscar cupom do empréstimo X |
| POST | `/cupom?id=X`, body: `tipo`, `descricao`, `valor` | Associar cupom |
