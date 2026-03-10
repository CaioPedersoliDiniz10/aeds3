package controller;

import dao.LivroDAO;
import model.Livro;
import java.io.IOException;
import java.util.List;

public class LivroController {

    private final LivroDAO dao;

    public LivroController(LivroDAO dao) {
        this.dao = dao;
    }

    public Livro cadastrar(String titulo, String autor, int ano) throws IOException {
        if (titulo == null || titulo.trim().isEmpty()) throw new IllegalArgumentException("Título é obrigatório.");
        if (autor == null || autor.trim().isEmpty()) throw new IllegalArgumentException("Autor é obrigatório.");
        if (ano < 1000 || ano > 2100) throw new IllegalArgumentException("Ano inválido.");
        Livro l = new Livro(0, titulo.trim(), autor.trim(), ano);
        return dao.criar(l);
    }

    public Livro buscar(int id) throws IOException {
        Livro l = dao.buscarPorId(id);
        if (l == null) throw new IllegalArgumentException("Livro com ID " + id + " não encontrado.");
        return l;
    }

    public List<Livro> listar() throws IOException {
        return dao.listarAtivos();
    }

    public Livro atualizar(int id, String novoTitulo, String novoAutor, Integer novoAno) throws IOException {
        Livro l = dao.buscarPorId(id);
        if (l == null) throw new IllegalArgumentException("Livro com ID " + id + " não encontrado.");
        if (novoTitulo != null && !novoTitulo.trim().isEmpty()) l.setTitulo(novoTitulo.trim());
        if (novoAutor != null && !novoAutor.trim().isEmpty()) l.setAutor(novoAutor.trim());
        if (novoAno != null) {
            if (novoAno < 1000 || novoAno > 2100) throw new IllegalArgumentException("Ano inválido.");
            l.setAno(novoAno);
        }
        dao.atualizar(l);
        return l;
    }

    public void excluir(int id) throws IOException {
        boolean ok = dao.excluir(id);
        if (!ok) throw new IllegalArgumentException("Livro com ID " + id + " não encontrado.");
    }
}
