package controller;

import dao.CupomDAO;
import dao.EmprestimoDAO;
import java.io.IOException;
import java.util.List;
import model.Cupom;

public class CupomController {

    private final CupomDAO cupomDAO;
    private final EmprestimoDAO emprestimoDAO;

    public CupomController(CupomDAO cupomDAO, EmprestimoDAO emprestimoDAO) {
        this.cupomDAO = cupomDAO;
        this.emprestimoDAO = emprestimoDAO;
    }

    public Cupom criar(int idEmprestimo, String tipo, String descricao, double valor) throws IOException {
        if (emprestimoDAO.buscarPorId(idEmprestimo) == null) {
            throw new IllegalArgumentException("Emprestimo com ID " + idEmprestimo + " nao encontrado.");
        }
        if (cupomDAO.buscarPorEmprestimo(idEmprestimo) != null) {
            throw new IllegalArgumentException("Ja existe cupom associado a este emprestimo.");
        }

        String tipoNormalizado = validarTipo(tipo, true);
        if (valor <= 0) {
            throw new IllegalArgumentException("Valor do cupom deve ser positivo.");
        }

        Cupom novo = new Cupom(0, idEmprestimo, tipoNormalizado, descricao == null ? "" : descricao.trim(), valor);
        return cupomDAO.criar(novo);
    }

    public Cupom buscar(int id) throws IOException {
        Cupom c = cupomDAO.buscarPorId(id);
        if (c == null) {
            throw new IllegalArgumentException("Cupom com ID " + id + " nao encontrado.");
        }
        return c;
    }

    public Cupom buscarPorEmprestimo(int idEmprestimo) throws IOException {
        Cupom c = cupomDAO.buscarPorEmprestimo(idEmprestimo);
        if (c == null) {
            throw new IllegalArgumentException("Nenhum cupom associado ao emprestimo ID " + idEmprestimo + ".");
        }
        return c;
    }

    public List<Cupom> listar() throws IOException {
        return cupomDAO.listarAtivos();
    }

    public Cupom atualizar(int id, String tipo, String descricao, Double valor) throws IOException {
        Cupom atual = cupomDAO.buscarPorId(id);
        if (atual == null) {
            throw new IllegalArgumentException("Cupom com ID " + id + " nao encontrado.");
        }

        if (tipo != null && !tipo.trim().isEmpty()) {
            atual.setTipo(validarTipo(tipo, true));
        }
        if (descricao != null) {
            atual.setDescricao(descricao.trim());
        }
        if (valor != null) {
            if (valor <= 0) {
                throw new IllegalArgumentException("Valor do cupom deve ser positivo.");
            }
            atual.setValor(valor);
        }

        cupomDAO.atualizar(atual);
        return atual;
    }

    public void excluir(int id) throws IOException {
        boolean ok = cupomDAO.excluir(id);
        if (!ok) {
            throw new IllegalArgumentException("Cupom com ID " + id + " nao encontrado.");
        }
    }

    private String validarTipo(String tipo, boolean obrigatorio) {
        String valor = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!obrigatorio && valor.isEmpty()) {
            return valor;
        }
        if (!"DESCONTO".equals(valor) && !"EXTENSAO".equals(valor)) {
            throw new IllegalArgumentException("Tipo de cupom invalido. Use DESCONTO ou EXTENSAO.");
        }
        return valor;
    }
}
