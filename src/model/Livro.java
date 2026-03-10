package model;

import java.io.*;

public class Livro implements Serializable {
    private int id;
    private String titulo;
    private String autor;
    private int ano;
    private boolean lapide;

    public Livro() {}

    public Livro(int id, String titulo, String autor, int ano) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.ano = ano;
        this.lapide = false;
    }

    // lapide(1) + id(4) + titulo(150) + autor(100) + ano(4) = 259 bytes
    public static final int TAMANHO = 1 + 4 + 150 + 100 + 4;

    public void serializar(DataOutputStream dos) throws IOException {
        dos.writeBoolean(lapide);
        dos.writeInt(id);
        writeFixedString(dos, titulo, 150);
        writeFixedString(dos, autor, 100);
        dos.writeInt(ano);
    }

    public static Livro desserializar(DataInputStream dis) throws IOException {
        Livro l = new Livro();
        l.lapide = dis.readBoolean();
        l.id = dis.readInt();
        l.titulo = readFixedString(dis, 150);
        l.autor = readFixedString(dis, 100);
        l.ano = dis.readInt();
        return l;
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
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }
    public boolean isLapide() { return lapide; }
    public void setLapide(boolean lapide) { this.lapide = lapide; }

    @Override
    public String toString() {
        return "{\"id\":" + id + ",\"titulo\":\"" + titulo.trim() + "\",\"autor\":\"" + autor.trim() + "\",\"ano\":" + ano + ",\"lapide\":" + lapide + "}";
    }
}
