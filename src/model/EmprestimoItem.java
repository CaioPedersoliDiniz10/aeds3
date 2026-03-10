package model;

import java.io.*;

/**
 * Representa um item (livro) dentro de um Empréstimo.
 * Relacionamento N:N entre Emprestimo e Livro.
 */
public class EmprestimoItem implements Serializable {
    private int id;
    private int idEmprestimo;
    private int idLivro;
    private int quantidade;
    private boolean lapide;

    public EmprestimoItem() {}

    public EmprestimoItem(int id, int idEmprestimo, int idLivro, int quantidade) {
        this.id = id;
        this.idEmprestimo = idEmprestimo;
        this.idLivro = idLivro;
        this.quantidade = quantidade;
        this.lapide = false;
    }

    // lapide(1) + id(4) + idEmprestimo(4) + idLivro(4) + quantidade(4) = 17 bytes
    public static final int TAMANHO = 1 + 4 + 4 + 4 + 4;

    public void serializar(DataOutputStream dos) throws IOException {
        dos.writeBoolean(lapide);
        dos.writeInt(id);
        dos.writeInt(idEmprestimo);
        dos.writeInt(idLivro);
        dos.writeInt(quantidade);
    }

    public static EmprestimoItem desserializar(DataInputStream dis) throws IOException {
        EmprestimoItem item = new EmprestimoItem();
        item.lapide = dis.readBoolean();
        item.id = dis.readInt();
        item.idEmprestimo = dis.readInt();
        item.idLivro = dis.readInt();
        item.quantidade = dis.readInt();
        return item;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdEmprestimo() { return idEmprestimo; }
    public void setIdEmprestimo(int idEmprestimo) { this.idEmprestimo = idEmprestimo; }
    public int getIdLivro() { return idLivro; }
    public void setIdLivro(int idLivro) { this.idLivro = idLivro; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public boolean isLapide() { return lapide; }
    public void setLapide(boolean lapide) { this.lapide = lapide; }

    @Override
    public String toString() {
        return "{\"id\":" + id + ",\"idEmprestimo\":" + idEmprestimo
                + ",\"idLivro\":" + idLivro + ",\"quantidade\":" + quantidade
                + ",\"lapide\":" + lapide + "}";
    }
}
