package controller;

import dao.EmprestimoDAO;
import dao.EmprestimoItemDAO;
import dao.UsuarioDAO;
import dao.LivroDAO;
import dao.CupomDAO;
import model.Emprestimo;
import model.EmprestimoItem;
import model.Cupom;
import java.io.IOException;
import java.util.List;

public class EmprestimoController {

    private final EmprestimoDAO emprestimoDAO;
    private final EmprestimoItemDAO itemDAO;
    private final UsuarioDAO usuarioDAO;
    private final LivroDAO livroDAO;
    private final CupomDAO cupomDAO;

    public EmprestimoController(EmprestimoDAO emprestimoDAO, EmprestimoItemDAO itemDAO,
                                 UsuarioDAO usuarioDAO, LivroDAO livroDAO, CupomDAO cupomDAO) {
        this.emprestimoDAO = emprestimoDAO;
        this.itemDAO = itemDAO;
        this.usuarioDAO = usuarioDAO;
        this.livroDAO = livroDAO;
        this.cupomDAO = cupomDAO;
    }

    // ---- Emprestimo ----

    public Emprestimo criar(int idUsuario, String dataEmprestimo, String dataDevolucao) throws IOException {
        if (usuarioDAO.buscarPorId(idUsuario) == null)
            throw new IllegalArgumentException("Usuário com ID " + idUsuario + " não encontrado.");
        if (dataEmprestimo == null || dataEmprestimo.trim().isEmpty())
            throw new IllegalArgumentException("Data de empréstimo é obrigatória.");
        Emprestimo e = new Emprestimo(0, idUsuario, dataEmprestimo.trim(),
                dataDevolucao != null ? dataDevolucao.trim() : "");
        return emprestimoDAO.criar(e);
    }

    public Emprestimo buscar(int id) throws IOException {
        Emprestimo e = emprestimoDAO.buscarPorId(id);
        if (e == null) throw new IllegalArgumentException("Empréstimo com ID " + id + " não encontrado.");
        return e;
    }

    public List<Emprestimo> listar() throws IOException {
        return emprestimoDAO.listarAtivos();
    }

    public Emprestimo atualizar(int id, String novaDataEmprestimo, String novaDataDevolucao) throws IOException {
        Emprestimo e = emprestimoDAO.buscarPorId(id);
        if (e == null) throw new IllegalArgumentException("Empréstimo com ID " + id + " não encontrado.");
        if (novaDataEmprestimo != null && !novaDataEmprestimo.trim().isEmpty())
            e.setDataEmprestimo(novaDataEmprestimo.trim());
        if (novaDataDevolucao != null)
            e.setDataDevolucao(novaDataDevolucao.trim());
        emprestimoDAO.atualizar(e);
        return e;
    }

    public void excluir(int id) throws IOException {
        boolean ok = emprestimoDAO.excluir(id);
        if (!ok) throw new IllegalArgumentException("Empréstimo com ID " + id + " não encontrado.");
    }

    // ---- Itens (livros do empréstimo) ----

    public EmprestimoItem adicionarItem(int idEmprestimo, int idLivro, int quantidade) throws IOException {
        if (emprestimoDAO.buscarPorId(idEmprestimo) == null)
            throw new IllegalArgumentException("Empréstimo com ID " + idEmprestimo + " não encontrado.");
        if (livroDAO.buscarPorId(idLivro) == null)
            throw new IllegalArgumentException("Livro com ID " + idLivro + " não encontrado.");
        if (quantidade <= 0)
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        for (EmprestimoItem existente : itemDAO.buscarPorEmprestimo(idEmprestimo)) {
            if (existente.getIdLivro() == idLivro)
                throw new IllegalArgumentException("Este livro já está neste empréstimo.");
        }
        return itemDAO.criar(new EmprestimoItem(0, idEmprestimo, idLivro, quantidade));
    }

    public List<EmprestimoItem> listarItens(int idEmprestimo) throws IOException {
        if (emprestimoDAO.buscarPorId(idEmprestimo) == null)
            throw new IllegalArgumentException("Empréstimo com ID " + idEmprestimo + " não encontrado.");
        return itemDAO.buscarPorEmprestimo(idEmprestimo);
    }

    public void removerItem(int idItem) throws IOException {
        boolean ok = itemDAO.excluir(idItem);
        if (!ok) throw new IllegalArgumentException("Item com ID " + idItem + " não encontrado.");
    }

    // ---- Cupom ----

    public Cupom associarCupom(int idEmprestimo, String tipo, String descricao, double valor) throws IOException {
        if (emprestimoDAO.buscarPorId(idEmprestimo) == null)
            throw new IllegalArgumentException("Empréstimo com ID " + idEmprestimo + " não encontrado.");
        if (cupomDAO.buscarPorEmprestimo(idEmprestimo) != null)
            throw new IllegalArgumentException("Este empréstimo já possui um cupom associado.");
        String tipoNorm = tipo != null ? tipo.trim().toUpperCase() : "";
        if (!tipoNorm.equals("DESCONTO") && !tipoNorm.equals("EXTENSAO"))
            throw new IllegalArgumentException("Tipo de cupom inválido. Use DESCONTO ou EXTENSAO.");
        if (valor <= 0) throw new IllegalArgumentException("Valor do cupom deve ser positivo.");
        return cupomDAO.criar(new Cupom(0, idEmprestimo, tipoNorm,
                descricao != null ? descricao.trim() : "", valor));
    }

    public Cupom buscarCupomDoEmprestimo(int idEmprestimo) throws IOException {
        Cupom c = cupomDAO.buscarPorEmprestimo(idEmprestimo);
        if (c == null) throw new IllegalArgumentException("Nenhum cupom associado ao empréstimo ID " + idEmprestimo + ".");
        return c;
    }
}
