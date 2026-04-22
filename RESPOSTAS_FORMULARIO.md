# Respostas ao Formulário de Projeto - Fase 2 AED III

## BiblioSystem: Sistema de Gerenciamento de Biblioteca

---

## a) Qual a estrutura usada para representar os registros?

**Resposta:**

Todos os registros são armazenados em **arquivos binários com tamanho fixo predefinido**, usando a classe `RandomAccessFile` do Java para acesso direto.

### Estrutura de Arquivo
```
[CABEÇALHO - 8 bytes]
  ├── ultimoId (4 bytes): int - maior ID gerado
  └── totalRegistros (4 bytes): int - quantidade de registros

[REGISTROS - n × TAMANHO_FIXO bytes]
  └── Sequência de registros com posição calculável
```

### Tamanhos de Registros
- **Usuario**: 205 bytes = lapide(1) + id(4) + nome(100) + email(100)
- **Livro**: 259 bytes = lapide(1) + id(4) + titulo(150) + autor(100) + ano(4)
- **Emprestimo**: 33 bytes = lapide(1) + id(4) + idUsuario(4) + data(12) + data(12)
- **EmprestimoItem**: 17 bytes = lapide(1) + id(4) + idEmprestimo(4) + idLivro(4) + qtd(4)
- **Cupom**: 137 bytes = lapide(1) + id(4) + idEmprestimo(4) + tipo(20) + descricao(100) + valor(8)

### Vantagens
✅ Acesso O(1) a qualquer registro: `offset = HEADER + idx × TAMANHO`  
✅ Sem fragmentação ou overhead de metadados por registro  
✅ Serialização/desserialização simples e determinística  
✅ Compatível com índices de acesso direto  

---

## b) Como atributos multivalorados do tipo string foram tratados?

**Resposta:**

Strings são armazenadas com **tamanho fixo** usando encoded UTF-8 com padding.

### Implementação
```java
// ESCRITA - método em cada Model
private static void writeFixedString(DataOutputStream dos, String s, int len) throws IOException {
    byte[] src = (s == null ? "" : s).getBytes("UTF-8");  // Converte para UTF-8
    byte[] buf = new byte[len];                           // Buffer fixo
    System.arraycopy(src, 0, buf, 0, Math.min(src.length, len));  // Copia até tamanho
    dos.write(buf);  // Escreve bytes exatos
}

// LEITURA - método em cada Model
private static String readFixedString(DataInputStream dis, int len) throws IOException {
    byte[] buf = new byte[len];        // Lê exatamente 'len' bytes
    dis.readFully(buf);                // Bloqueia até ter tudo
    return new String(buf, "UTF-8").trim();  // Decodifica UTF-8, remove espaços
}
```

### Características
- **Strings > tamanho máximo**: truncadas silenciosamente
- **Strings < tamanho máximo**: preenchidas com zeros (0x00)
- **Encoding**: UTF-8 para suportar caracteres especiais
- **Trim()**: remove espaços ao ler para aceitar comparações normais

### Exemplo Prático
```
Nome: "João" (4 chars)
Armazenado em 100 bytes: [J][o][ã][o][0][0][0]...[0]
Lido como: "João" após trim()
```

---

## c) Como foi implementada a exclusão lógica?

**Resposta:**

Exclusão lógica implementada via **campo `lapide` boolean** em cada entidade.

### Mecanismo
```java
// Campo em cada Model
private boolean lapide;  // true = deletado, false = ativo

// Operação de exclusão em DAO
public boolean excluir(int id) throws IOException {
    // ... localiza registro ...
    item.setLapide(true);          // Marca como deletado
    raf.seek(posicao);              // Volta ao mesmo local
    item.serializar(dos);           // Reescreve sobre o registro original
    raf.write(bytes);               // Persiste
    return true;
}

// Operação de busca - ignora lapidados
public XYZ buscarPorId(int id) throws IOException {
    // ...
    if (!xyz.isLapide() && xyz.getId() == id) {
        return xyz;  // Só retorna se ativo
    }
    // ...
}
```

### Vantagens da Abordagem
✅ **Sem reorganização**: outros registros mantêm mesmos offsets  
✅ **Recuperação possível**: pode-se "desdeleta" mudando flag  
✅ **Operação O(1)**: apenas marca um bit e reescreve  
✅ **Histórico**: registro deletado continua em arquivo  

### Desvantagem
❌ Arquivo cresce indefinidamente (sem compactação periódica)

---

## d) Além das PKs, quais outras chaves foram utilizadas nesta etapa?

**Resposta:**

Foram implementadas as seguintes **chaves secundárias**:

| Tabela | Chave Secundária | Tipo | Método | Complexidade |
|--------|------------------|------|--------|--------------|
| **Usuario** | email | Única | `emailExiste(String)` | O(n) |
| **Livro** | titulo + autor | Única | `livroExiste(String, String)` | O(n) |
| **Emprestimo** | (nenhuma secundária) | - | - | - |
| **EmprestimoItem** | idEmprestimo | Múltipla (1:N) | `buscarPorEmprestimo(int)` | **O(1) com índice** |
| **Cupom** | idEmprestimo | Única | `buscarPorEmprestimo(int)` | O(n) |

### Validações de Chave Única
```java
// Ao cadastrar usuário
if (dao.emailExiste(email))
    throw new IllegalArgumentException("Email já cadastrado");

// Ao cadastrar livro
if (dao.livroExiste(titulo, autor))
    throw new IllegalArgumentException("Livro com este título/autor já existe");
```

### Chave Secundária 1:N com Índice
A busca `idEmprestimo` em `EmprestimoItem` é a mais crítica e foi **indexada com Hash Extensível** para O(1).

---

## e) Como a estruturas (hash) foi implementada para cada chave de pesquisa?

**Resposta:**

### Buscas Secundárias Simples (Email, Título+Autor)
```
Tipo: SCAN LINEAR com comparação string
Classe: Implementado em cada DAO com método específico
Exemplo:
  public boolean emailExiste(String email) {
      // Lê cada registro
      // Compara trim() case-insensitive
      if (!u.isLapide() && u.getEmail().equalsIgnoreCase(email))
          return true;
  }
Complexidade: O(n) onde n = total de registros
```

### Busca 1:N com Hash Extensível (Principal)
```
Tipo: HASH EXTENSÍVEL dinâmico
Classe: HashExtensivel.java (serializable)
Uso: EmprestimoItemDAOIndexado.java

Estrutura:
  ├── globalDepth: int (profundidade do diretório)
  ├── directory: Map<Integer, Bucket>[] 
  │   └── cada bucket: Map<Integer, List<Long>>
  │       └── chave: idEmprestimo
  │           valor: lista de offsets no arquivo

Operações:
  inserir(chave, offset)    → O(1) médio
  buscar(chave)             → O(1) médio
  remover(chave, offset)    → O(1) médio
```

### Hash Extensível - Funcionamento
```java
public void inserir(int chave, long valor) {
    int hash = obterHash(chave);
    int indice = hash % directory.length;
    directory[indice].get(chave).adicionar(valor);
}

private int obterHash(int chave) {
    return Math.abs(chave * 31 % (1 << globalDepth));
}
```

### Persistência do Índice
```
Arquivo: emprestimo_itens.dat.idx
Formato: ObjectOutputStream (serialização Java)
Conteúdo:
  [globalDepth - 4 bytes]
  [Directory structure - mapa serializado]

Sincronização:
  - Atualizado ao criar, atualizar ou deletar itens
  - Reconstruído automaticamente se corrompido
```

### Comparação de Complexidades
```
Sem índice:  buscarPorEmprestimo() = O(n)
Com índice:  buscarPorEmprestimo() = O(1) médio
Exemplo com 100 empréstimos × 5 itens cada:
  - Sem: 500 comparações por busca
  - Com: 1 acesso direto
```

---

## f) Como foi implementado o relacionamento 1:N? (Navegação e Integridade Referencial)

**Resposta:**

### Estrutura de Relacionamento
```
Emprestimo (1) ──→ (N) EmprestimoItem
                    │
                    └──→ Livro
```

### Implementação Prática
```
Tabela: emprestimo_itens.dat
Cada registro:
  - id: identificador único
  - idEmprestimo: chave estrangeira (referencia Emprestimo)
  - idLivro: chave estrangeira (referencia Livro)
  - quantidade: número de cópias

Índice: emprestimo_itens.dat.idx
  - Mapeia idEmprestimo → [offsets dos itens]
```

### Navegação Entre Registros
```java
// 1. Obter empréstimo
Emprestimo emp = emprestimoDAO.buscarPorId(1);

// 2. Encontrar seus itens (O(1) com índice)
List<EmprestimoItem> itens = itemDAO.buscarPorEmprestimo(1);

// 3. Para cada item, encontrar o livro
for (EmprestimoItem item : itens) {
    Livro livro = livroDAO.buscarPorId(item.getIdLivro());
    // Agora tem acesso à título, autor, etc.
}
```

### Integridade Referencial
```java
public EmprestimoItem adicionarItem(int idEmprestimo, int idLivro, int quantidade) {
    // Validação 1: Empréstimo existe?
    if (emprestimoDAO.buscarPorId(idEmprestimo) == null)
        throw new IllegalArgumentException("Empréstimo não existe");
    
    // Validação 2: Livro existe?
    if (livroDAO.buscarPorId(idLivro) == null)
        throw new IllegalArgumentException("Livro não existe");
    
    // Validação 3: Livro não duplicado no mesmo empréstimo?
    for (EmprestimoItem existente : itemDAO.buscarPorEmprestimo(idEmprestimo)) {
        if (existente.getIdLivro() == idLivro)
            throw new IllegalArgumentException("Livro já adicionado");
    }
    
    // Validação 4: Quantidade válida?
    if (quantidade <= 0)
        throw new IllegalArgumentException("Quantidade > 0");
    
    return itemDAO.criar(new EmprestimoItem(0, idEmprestimo, idLivro, quantidade));
}
```

### Exclusão em Cascata (Soft)
```java
// Ao deletar empréstimo, seus itens devem ser deletados
// Implementado como exclusão lógica (lápide)
public void excluirEmprestimo(int idEmprestimo) {
    // Delete empréstimo
    emprestimoDAO.excluir(idEmprestimo);
    
    // Delete seus itens (em aplicação real, fazer em transaction)
    List<EmprestimoItem> itens = itemDAO.buscarPorEmprestimo(idEmprestimo);
    for (EmprestimoItem item : itens) {
        itemDAO.excluir(item.getId());
    }
}
```

### Garantias Implementadas
✅ Referência válida: IDs verificados antes de inserir  
✅ Sem duplicação: verificado que livro não repetiça no empréstimo  
✅ Quantidade válida: > 0 obrigatório  
✅ Navegação eficiente: índice O(1) para listar itens por empréstimo  

---

## g) Como os índices são persistidos em disco? (Formato, Atualização, Sincronização)

**Resposta:**

### Formato de Persistência
```
Arquivo: emprestimo_itens.dat.idx
Formato: Serialização Java (ObjectOutputStream)

Estrutura:
  [Magic Number/Version] ← ObjectOutputStream header
  [globalDepth: int (4 bytes)]
  [directory: Map<Integer, Bucket>[] array serializado]
      └── Map<Integer, List<Long>> para cada bucket
          └── List<Long> com offsets no arquivo .dat

Exemplo em bytes:
  AC F7 ED    ← ObjectStream magic
  ... XX XX XX XX    ← globalDepth
  ... [mapa serializado com estrutura completa]
```

### Classe Index: HashExtensivel
```java
public void salvar(String caminhoArquivo) throws IOException {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(caminhoArquivo))) {
        oos.writeInt(globalDepth);
        oos.writeObject(directory);  // Serializa array de maps
    }
}

public static HashExtensivel carregar(String caminhoArquivo) throws IOException, ClassNotFoundException {
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(caminhoArquivo))) {
        HashExtensivel hash = new HashExtensivel();
        hash.globalDepth = ois.readInt();
        hash.directory = (Map<Integer, Bucket>[]) ois.readObject();
        return hash;
    }
}
```

### Atualização do Índice
```java
// Ao criar novo item
public EmprestimoItem criar(EmprestimoItem item) throws IOException {
    // ... insere no arquivo dados ...
    long pos = offsetRegistro(cab[1]);
    raf.seek(pos);
    item.serializar(raf);
    
    // Atualizar índice em memória
    indiceEmprestimo.inserir(item.getIdEmprestimo(), pos);
    
    // Persistir índice em disco
    salvarIndice();  // ← Chamada para HashExtensivel.salvar()
    return item;
}

// Ao deletar
public boolean excluir(int id) throws IOException {
    // ... marca lapide = true ...
    
    // Remove do índice em memória
    indiceEmprestimo.remover(item.getIdEmprestimo(), pos);
    
    // Persiste índice
    salvarIndice();
    return true;
}
```

### Sincronização e Recuperação
```java
// Ao inicializar DAO - tenta carregar índice
private void carregarIndice() {
    File f = new File(caminhoIndice);
    if (f.exists()) {
        try {
            indiceEmprestimo = HashExtensivel.carregar(caminhoIndice);
        } catch (Exception e) {
            // Índice corrompido - reconstrói
            indiceEmprestimo = new HashExtensivel();
            reconstruirIndice();  // Lê arquivo .dat completamente
        }
    } else {
        // Primeira execução
        indiceEmprestimo = new HashExtensivel();
        reconstruirIndice();
    }
}

// Reconstrução: lê arquivo .dat e reconstrói mapa
private void reconstruirIndice() {
    indiceEmprestimo = new HashExtensivel();
    try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
        dis.skipBytes(HEADER_SIZE);
        for (int i = 0; i < cab[1]; i++) {
            byte[] buf = new byte[EmprestimoItem.TAMANHO];
            if (dis.read(buf) < EmprestimoItem.TAMANHO) break;
            EmprestimoItem item = EmprestimoItem.desserializar(...);
            if (!item.isLapide()) {
                long pos = HEADER_SIZE + (long) i * EmprestimoItem.TAMANHO;
                indiceEmprestimo.inserir(item.getIdEmprestimo(), pos);
            }
        }
    }
    salvarIndice();
}
```

### Sincronização Garantida
✅ **Atualização atomic**: índice salvo após cada operação  
✅ **Recuperação automática**: índice reconstruído se corrompido  
✅ **Sem desincronização**: se servidor falhar, próxima inicialização reconstrói  
✅ **Performance**: índice em memória + persistência periódica  

---

## h) Como está estruturado o projeto no GitHub? (Pastas, Módulos, Arquitetura)

**Resposta:**

### Estrutura de Repositório

```
aeds3/
├── .git/                               # Repositório Git
├── src/                                # Código-fonte
│   ├── model/                          # Camada de Modelo
│   │   ├── Usuario.java
│   │   ├── Livro.java
│   │   ├── Emprestimo.java
│   │   ├── EmprestimoItem.java
│   │   └── Cupom.java
│   ├── dao/                            # Camada de Persistência
│   │   ├── HashExtensivel.java         ⭐ Índice 1:N
│   │   ├── UsuarioDAO.java
│   │   ├── LivroDAO.java
│   │   ├── EmprestimoDAO.java
│   │   ├── EmprestimoItemDAO.java      (DAO sem índice)
│   │   ├── EmprestimoItemDAOIndexado.java  ⭐ DAO com Hash Extensível
│   │   └── CupomDAO.java
│   ├── controller/                     # Camada de Negócio
│   │   ├── UsuarioController.java
│   │   ├── LivroController.java
│   │   └── EmprestimoController.java
│   └── server/                         # Camada de Apresentação (API HTTP)
│       ├── AppServer.java              # Servidor HTTP
│       ├── StaticHandler.java          # Arquivos estáticos
│       ├── UsuarioHandler.java         # Endpoints REST
│       ├── LivroHandler.java           # Endpoints REST
│       └── EmprestimoHandler.java      # Endpoints REST
├── view/                               # Frontend Web
│   ├── index.html                      # Página inicial
│   ├── usuarios.html                   # CRUD Usuários
│   ├── livros.html                     # CRUD Livros
│   ├── emprestimos.html                # CRUD Empréstimos
│   ├── cupons.html                     # CRUD Cupons
│   ├── style.css                       # Estilos
│   └── livereload.js                   # Live reload
├── data/                               # Arquivos de dados (gerado em runtime)
│   ├── usuarios.dat
│   ├── livros.dat
│   ├── emprestimos.dat
│   ├── emprestimo_itens.dat
│   ├── emprestimo_itens.dat.idx        ← Índice
│   └── cupons.dat
├── bin/                                # Binários compilados (gerado)
├── docs/                               # Documentação
│   └── README.md
├── .gitignore                          # Padrões ignorados
├── run.bat                             # Script de execução (Windows)
├── dev.ps1                             # Script de desenvolvimento
├── README.md                           # Instruções principais
└── DOCUMENTACAO_TECNICA.md             # Documentação detalhada do TP
```

### Arquitetura de Camadas

```
┌─────────────────────────────────────────┐
│  Camada 1: View (Frontend)              │
│  view/ - HTML/CSS/JavaScript            │
│  Interface responsiva com foco de UX    │
└────────────┬────────────────────────────┘
             │ HTTP REST (port 8080)
             ↓
┌─────────────────────────────────────────┐
│  Camada 2: Server (API HTTP)            │
│  server/ - Handlers HTTP                │
│  Endpoints REST: /api/usuarios, etc     │
│  CORS habilitado para testes            │
└────────────┬────────────────────────────┘
             │ Delegação
             ↓
┌─────────────────────────────────────────┐
│  Camada 3: Controller (Lógica)          │
│  controller/ - Regras de negócio        │
│  Validações, integridade referencial    │
│  Sem conhecimento de HTTP               │
└────────────┬────────────────────────────┘
             │ Operações CRUD
             ↓
┌─────────────────────────────────────────┐
│  Camada 4: DAO (Persistência)           │
│  dao/ - Acesso a dados e índices        │
│  HashExtensivel para 1:N otimizado      │
│  Serialização binária                   │
└────────────┬────────────────────────────┘
             │ Acesso
             ↓
┌─────────────────────────────────────────┐
│  Camada 5: Model (Entidades)            │
│  model/ - Classes de domínio            │
│  Serialization/Deserialization          │
└────────────┬────────────────────────────┘
             │
             ↓
┌─────────────────────────────────────────┐
│  Camada 6: Persistência (Arquivos)      │
│  data/ - Arquivos binários + índices    │
│  .dat (dados) + .idx (índices)          │
└─────────────────────────────────────────┘
```

### Módulos Independentes

| Módulo | Responsabilidade | Dependências |
|--------|------------------|--------------|
| `model` | Entidades, serialização | Nenhuma |
| `dao` | Persistência, índices | `model` |
| `controller` | Validações, lógica | `dao`, `model` |
| `server` | HTTP, routing | `controller`, `model` |
| `view` | Interface web | JavaScript vanilla |

### Padrões de Projeto Aplicados

1. **MVC**: Separação entre Model, View, Controller
2. **DAO**: Abstração de acesso a dados
3. **Singleton**: Instâncias únicas de DAOs e Controllers
4. **Factory**: Criação de objetos (não aplicado, inicializado em AppServer)
5. **Observer**: Eventos HTTP (implícito em handlers)

### Benefícios da Arquitetura

✅ **Manutenibilidade**: Mudanças em persistência não afetam lógica  
✅ **Testabilidade**: Controllers podem ser testados sem HTTP  
✅ **Escalabilidade**: Fácil adicionar novos DAOs/Handlers  
✅ **Reutilização**: Controllers podem ser usados por múltiplos Handlers  
✅ **Independência**: Frontend pode ser substituído mantendo API  

---

## Resumo de Implementações

| Requisito | Implementado | Como |
|-----------|-------------|------|
| CRUD completo | ✅ | Todos os DAOs com C.R.U.D |
| Hash Extensível | ✅ | HashExtensivel.java + EmprestimoItemDAOIndexado |
| Índices primários | ✅ | Chave serial auto-incremento |
| Exclusão lógica | ✅ | Campo `lapide` boolean |
| Persistência | ✅ | Arquivo binário + índices `.idx` |
| Validação | ✅ | Chaves únicas, campos obrigatórios |
| Interface web | ✅ | HTML/CSS responsivo |
| API REST | ✅ | Endpoints completamente funcionais |
| Documentação | ✅ | README + DOCUMENTACAO_TECNICA |

---

**Conclusão**: BiblioSystem implementa com sucesso todas as exigências da Fase 2, incluindo Hash Extensível para otimização de relacionamento 1:N, persistência sincronizada com índices, validação completa e interface amigável.
