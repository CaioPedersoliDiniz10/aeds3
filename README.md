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

