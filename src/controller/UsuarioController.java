package controller;

import dao.UsuarioDAO;
import model.Usuario;
import java.io.IOException;
import java.util.List;

public class UsuarioController {

    private final UsuarioDAO dao;

    public UsuarioController(UsuarioDAO dao) {
        this.dao = dao;
    }

    public Usuario cadastrar(String nome, String email) throws IOException {
        if (nome == null || nome.trim().isEmpty()) throw new IllegalArgumentException("Nome é obrigatório.");
        if (email == null || email.trim().isEmpty()) throw new IllegalArgumentException("E-mail é obrigatório.");
        if (!email.contains("@")) throw new IllegalArgumentException("E-mail inválido.");
        Usuario u = new Usuario(0, nome.trim(), email.trim());
        return dao.criar(u);
    }

    public Usuario buscar(int id) throws IOException {
        Usuario u = dao.buscarPorId(id);
        if (u == null) throw new IllegalArgumentException("Usuário com ID " + id + " não encontrado.");
        return u;
    }

    public List<Usuario> listar() throws IOException {
        return dao.listarAtivos();
    }

    public Usuario atualizar(int id, String novoNome, String novoEmail) throws IOException {
        Usuario u = dao.buscarPorId(id);
        if (u == null) throw new IllegalArgumentException("Usuário com ID " + id + " não encontrado.");
        if (novoNome != null && !novoNome.trim().isEmpty()) u.setNome(novoNome.trim());
        if (novoEmail != null && !novoEmail.trim().isEmpty()) {
            if (!novoEmail.contains("@")) throw new IllegalArgumentException("E-mail inválido.");
            u.setEmail(novoEmail.trim());
        }
        dao.atualizar(u);
        return u;
    }

    public void excluir(int id) throws IOException {
        boolean ok = dao.excluir(id);
        if (!ok) throw new IllegalArgumentException("Usuário com ID " + id + " não encontrado.");
    }
}
