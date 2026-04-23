# BiblioSystem – Sistema de Gerenciamento de Biblioteca
## Fase 2 - AED III

### ⭐ Resumo
Sistema de biblioteca com arquitetura **MVC**, persistencia em **arquivos binarios de tamanho fixo**, interface web e estruturas de dados para acesso eficiente.

### Funcionalidades Entregues
- CRUD de todas as tabelas: Usuario, Livro, Emprestimo, EmprestimoItem e Cupom.
- Relacionamento 1:N: Emprestimo -> EmprestimoItem.
- Consulta por indice com **Hash Extensivel** para itens por emprestimo.
- **Ordenacao externa** por atributo em livros (titulo, autor, ano).
- **Arvore B+ (inicial)** com insercao e busca exata por ID de livro.

### Rotas de Demonstracao
- Hash extensivel: `/api/emprestimos/indice?id={idEmprestimo}`
- Ordenacao externa: `/api/livros/ordenacao-externa?atributo=titulo|autor|ano`
- Arvore B+ busca: `/api/livros/bplus?id={idLivro}`
- Arvore B+ estatisticas: `/api/livros/bplus`

### Roteiro do Video
- Ver roteiro em: `docs/ROTEIRO_VIDEO_DEMONSTRACAO.md`

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

