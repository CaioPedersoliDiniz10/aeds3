# Roteiro de Video - Entrega

Este roteiro cobre os 4 pontos solicitados:
1. implementacao do relacionamento 1:N
2. consulta utilizando indice
3. ordenacao externa por atributo
4. manipulacao da Arvore B+

## Preparacao

1. Executar o projeto:
   - Windows: run.bat
   - URL: http://localhost:8080
2. Abrir tambem o terminal para mostrar chamadas REST (opcional).

## 1) Relacionamento 1:N (Emprestimo -> Itens)

1. Acessar a tela de emprestimos.
2. Criar um emprestimo para um usuario existente.
3. Na secao "Adicionar Livros ao Emprestimo", inserir 2 livros para o mesmo emprestimo.
4. Mostrar na tabela que um emprestimo possui varios itens (1:N).
5. Editar um item (botao com lapis) e depois remover um item (botao X) para mostrar CRUD do relacionamento.

Evidencia tecnica:
- Rotas usadas: /api/emprestimos/itens
- CRUD de itens: GET, POST, PUT, DELETE

## 2) Consulta Utilizando Indice (Hash Extensivel)

1. Na tela de emprestimos, usar a secao "Consulta Usando Indice Hash Extensivel".
2. Informar o ID do emprestimo criado no passo anterior.
3. Mostrar o retorno com:
   - metodo = hash_extensivel
   - estatisticas do indice
   - itens encontrados

Evidencia tecnica:
- Rota: /api/emprestimos/indice?id={idEmprestimo}
- DAO indexado em runtime: EmprestimoItemDAOIndexado

## 3) Ordenacao Externa por Atributo

1. Acessar a tela de livros.
2. Clicar em "Ordenacao externa por titulo".
3. Mostrar a lista ordenada e o aviso de ordenacao externa.
4. Repetir com "Ordenacao externa por ano".

Evidencia tecnica:
- Rota: /api/livros/ordenacao-externa?atributo=titulo|ano
- Algoritmo: runs + merge k-way em arquivo temporario

## 4) Manipulacao da Arvore B+ (Insercao e Busca)

1. Ainda na tela de livros, cadastrar um livro novo.
2. Explicar que a insercao no cadastro alimenta o indice B+ (chave ID -> offset).
3. Em "Consultar por ID", escolher "Indice Arvore B+" e buscar o livro.
4. Clicar em "Ver estatisticas B+" e mostrar altura/nos/chaves.

Evidencia tecnica:
- Busca por indice: /api/livros/bplus?id={id}
- Estatisticas: /api/livros/bplus

## Extra (CRUD completo de todas as tabelas)

Para fechar o video, demonstrar rapidamente:
- Usuarios: CRUD em /api/usuarios
- Livros: CRUD em /api/livros
- Emprestimos: CRUD em /api/emprestimos
- EmprestimoItem: CRUD em /api/emprestimos/itens
- Cupons: CRUD em /api/cupons
