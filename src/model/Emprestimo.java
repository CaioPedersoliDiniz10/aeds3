package model;

import java.io.*;

public class Emprestimo implements Serializable {
    private int id;
    private int idUsuario;
    private int idLivro;
    private String dataEmprestimo;  // formato: "YYYY-MM-DD"
    private String dataDevolucao;   // formato: "YYYY-MM-DD" ou vazio
    private boolean lapide;

    public Emprestimo() {}

    public Emprestimo(int id, int idUsuario, int idLivro, String dataEmprestimo, String dataDevolucao) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.idLivro = idLivro;
        this.dataEmprestimo = dataEmprestimo;
        this.dataDevolucao = dataDevolucao == null ? "" : dataDevolucao;
        this.lapide = false;
    }

    // lapide(1) + id(4) + idUsuario(4) + idLivro(4) + dataEmprestimo(12) + dataDevolucao(12) = 37 bytes
    public static final int TAMANHO = 1 + 4 + 4 + 4 + 12 + 12;

    public void serializar(DataOutputStream dos) throws IOException {
        dos.writeBoolean(lapide);
        dos.writeInt(id);
        dos.writeInt(idUsuario);
        dos.writeInt(idLivro);
        writeFixedString(dos, dataEmprestimo, 12);
        writeFixedString(dos, dataDevolucao, 12);
    }

    public static Emprestimo desserializar(DataInputStream dis) throws IOException {
        Emprestimo e = new Emprestimo();
        e.lapide = dis.readBoolean();
        e.id = dis.readInt();
        e.idUsuario = dis.readInt();
        e.idLivro = dis.readInt();
        e.dataEmprestimo = readFixedString(dis, 12);
        e.dataDevolucao = readFixedString(dis, 12);
        return e;
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
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public int getIdLivro() { return idLivro; }
    public void setIdLivro(int idLivro) { this.idLivro = idLivro; }
    public String getDataEmprestimo() { return dataEmprestimo; }
    public void setDataEmprestimo(String dataEmprestimo) { this.dataEmprestimo = dataEmprestimo; }
    public String getDataDevolucao() { return dataDevolucao; }
    public void setDataDevolucao(String dataDevolucao) { this.dataDevolucao = dataDevolucao; }
    public boolean isLapide() { return lapide; }
    public void setLapide(boolean lapide) { this.lapide = lapide; }

    @Override
    public String toString() {
        return "{\"id\":" + id + ",\"idUsuario\":" + idUsuario + ",\"idLivro\":" + idLivro
                + ",\"dataEmprestimo\":\"" + dataEmprestimo.trim() + "\",\"dataDevolucao\":\"" + dataDevolucao.trim()
                + "\",\"lapide\":" + lapide + "}";
    }
}
