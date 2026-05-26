package controller;

import dao.CupomDAO;
import dao.EmprestimoDAO;
import dao.EmprestimoItemDAOIndexado;
import dao.LivroDAO;
import dao.UsuarioDAO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import model.Cupom;
import model.Emprestimo;
import model.EmprestimoItem;

public class EmprestimoController {

    private final EmprestimoDAO emprestimoDAO;
    private final EmprestimoItemDAOIndexado itemDAO;
    private final UsuarioDAO usuarioDAO;
    private final LivroDAO livroDAO;
    private final CupomDAO cupomDAO;

    public EmprestimoController(EmprestimoDAO emprestimoDAO, EmprestimoItemDAOIndexado itemDAO,
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
        if (emprestimoDAO.buscarPorId(id) == null) {
            throw new IllegalArgumentException("Empréstimo com ID " + id + " não encontrado.");
        }

        // Exclusão em cascata (soft): marca itens do empréstimo como lápide.
        itemDAO.excluirPorEmprestimo(id);

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
        return itemDAO.criar(new EmprestimoItem(idEmprestimo, idLivro, quantidade));
    }

    public List<EmprestimoItem> listarItens(int idEmprestimo) throws IOException {
        if (emprestimoDAO.buscarPorId(idEmprestimo) == null)
            throw new IllegalArgumentException("Empréstimo com ID " + idEmprestimo + " não encontrado.");
        // Listagem ordenada usando Árvore B+ (por idLivro)
        return itemDAO.buscarPorEmprestimoOrdenadoPorLivro(idEmprestimo);
    }

    public List<EmprestimoItem> listarTodosItens() throws IOException {
        return itemDAO.listarAtivos();
    }

    /**
     * Acesso pelo outro lado do N:N: listar todos os empréstimos que contêm um livro.
     */
    public List<Emprestimo> listarEmprestimosQueContemLivro(int idLivro) throws IOException {
        if (livroDAO.buscarPorId(idLivro) == null) {
            throw new IllegalArgumentException("Livro com ID " + idLivro + " não encontrado.");
        }

        List<EmprestimoItem> vinculos = itemDAO.buscarPorLivro(idLivro);
        Set<Integer> ids = new LinkedHashSet<>();
        for (EmprestimoItem v : vinculos) {
            ids.add(v.getIdEmprestimo());
        }

        List<Emprestimo> lista = new ArrayList<>();
        for (int idEmp : ids) {
            Emprestimo e = emprestimoDAO.buscarPorId(idEmp);
            if (e != null && !e.isLapide()) {
                lista.add(e);
            }
        }
        return lista;
    }

    public EmprestimoItem buscarItem(int idItem) throws IOException {
        EmprestimoItem item = itemDAO.buscarPorId(idItem);
        if (item == null) {
            throw new IllegalArgumentException("Item com ID " + idItem + " não encontrado.");
        }
        return item;
    }

    public EmprestimoItem atualizarItem(int idItem, Integer novoIdEmprestimo, Integer novoIdLivro, Integer novaQuantidade)
            throws IOException {
        EmprestimoItem item = itemDAO.buscarPorId(idItem);
        if (item == null) {
            throw new IllegalArgumentException("Item com ID " + idItem + " não encontrado.");
        }

        // PK composta (idEmprestimo,idLivro): nesta fase não permitimos trocar as chaves via update.
        if (novoIdEmprestimo != null || novoIdLivro != null) {
            throw new IllegalArgumentException("Não é permitido alterar idEmprestimo/idLivro do item (chave composta). Remova e adicione novamente.");
        }

        if (novaQuantidade == null) {
            return item;
        }
        if (novaQuantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }

        EmprestimoItem atualizado = new EmprestimoItem(item.getIdEmprestimo(), item.getIdLivro(), novaQuantidade);
        itemDAO.atualizar(atualizado);
        return itemDAO.buscarPorChaveComposta(item.getIdEmprestimo(), item.getIdLivro());
    }

    public void removerItem(int idItem) throws IOException {
        boolean ok = itemDAO.excluir(idItem);
        if (!ok) throw new IllegalArgumentException("Item com ID " + idItem + " não encontrado.");
    }

    public Map<String, Object> obterEstatisticasIndiceItens() {
        return itemDAO.obterEstatisticasIndice();
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
