# BiblioSystem – Sistema de Gerenciamento de Biblioteca

## Descrição
Sistema CRUD para gerenciamento de **Usuários**, **Livros**, **Empréstimos** e **Cupons**.  
Utiliza persistência em **arquivos binários** com cabeçalho e **exclusão lógica por lápide**.  
Interface via **HTML/CSS** servida por servidor HTTP embarcado em Java.

---

## Requisitos
- Java 11 ou superior (disponível no PATH)

---

## Como executar

```
run.bat
```

Acesse no navegador: **http://localhost:8080**

---

## Estrutura do Projeto

```
aeds3/
├── run.bat                   ← Compila e executa
├── data/                     ← Arquivos binários gerados em execução
│   ├── usuarios.dat
│   ├── livros.dat
│   ├── emprestimos.dat
│   └── cupons.dat
├── view/                     ← Interface HTML/CSS (View)
│   ├── index.html
│   ├── usuarios.html
│   ├── livros.html
│   ├── emprestimos.html
│   ├── cupons.html
│   └── style.css
└── src/
    ├── model/                ← Entidades (Model)
    │   ├── Usuario.java
    │   ├── Livro.java
    │   ├── Emprestimo.java
    │   └── Cupom.java
    ├── dao/                  ← Acesso a dados (DAO)
    │   ├── UsuarioDAO.java
    │   ├── LivroDAO.java
    │   ├── EmprestimoDAO.java
    │   └── CupomDAO.java
    ├── controller/           ← Regras de negócio (Controller)
    │   ├── UsuarioController.java
    │   ├── LivroController.java
    │   └── EmprestimoController.java
    └── server/               ← Servidor HTTP + Handlers de API
        ├── AppServer.java
        ├── StaticHandler.java
        ├── UsuarioHandler.java
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
