package model;

import java.io.*;

public class Cupom implements Serializable {
    private int id;
    private int idEmprestimo;
    private String tipo;        // "DESCONTO" ou "EXTENSAO"
    private String descricao;
    private double valor;       // percentual de desconto ou dias de extensão
    private boolean lapide;

    public Cupom() {}

    public Cupom(int id, int idEmprestimo, String tipo, String descricao, double valor) {
        this.id = id;
        this.idEmprestimo = idEmprestimo;
        this.tipo = tipo;
        this.descricao = descricao;
        this.valor = valor;
        this.lapide = false;
    }

    // lapide(1) + id(4) + idEmprestimo(4) + tipo(20) + descricao(100) + valor(8) = 137 bytes
    public static final int TAMANHO = 1 + 4 + 4 + 20 + 100 + 8;

    public void serializar(DataOutputStream dos) throws IOException {
        dos.writeBoolean(lapide);
        dos.writeInt(id);
        dos.writeInt(idEmprestimo);
        writeFixedString(dos, tipo, 20);
        writeFixedString(dos, descricao, 100);
        dos.writeDouble(valor);
    }

    public static Cupom desserializar(DataInputStream dis) throws IOException {
        Cupom c = new Cupom();
        c.lapide = dis.readBoolean();
        c.id = dis.readInt();
        c.idEmprestimo = dis.readInt();
        c.tipo = readFixedString(dis, 20);
        c.descricao = readFixedString(dis, 100);
        c.valor = dis.readDouble();
        return c;
    }

    private static void writeFixedString(DataOutputStream dos, String s, int len) throws IOException {
        byte[] src = (s == null ? "" : s).getBytes("UTF-8");
        byte[] buf = new byte[len];
        System.arraycopy(src, 0, buf, 0, Math.min(src.length, len));
        dos.write(buf);
    }

    private static String readFixedString(DataInputStream dis, int len) throws IOException {
        byte[] buf = new byte[len];
        dis.readFully(buf);
        return new String(buf, "UTF-8").trim();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdEmprestimo() { return idEmprestimo; }
    public void setIdEmprestimo(int idEmprestimo) { this.idEmprestimo = idEmprestimo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public boolean isLapide() { return lapide; }
    public void setLapide(boolean lapide) { this.lapide = lapide; }

    @Override
    public String toString() {
        return "{\"id\":" + id + ",\"idEmprestimo\":" + idEmprestimo + ",\"tipo\":\"" + tipo.trim()
                + "\",\"descricao\":\"" + descricao.trim() + "\",\"valor\":" + valor + ",\"lapide\":" + lapide + "}";
    }
}
