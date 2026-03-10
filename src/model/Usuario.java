package model;

import java.io.*;

public class Usuario implements Serializable {
    private int id;
    private String nome;
    private String email;
    private boolean lapide; // true = excluído logicamente

    public Usuario() {}

    public Usuario(int id, String nome, String email) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.lapide = false;
    }

    // tamanho fixo do registro em bytes:
    // lapide(1) + id(4) + nome(100) + email(100) = 205 bytes
    public static final int TAMANHO = 1 + 4 + 100 + 100;

    public void serializar(DataOutputStream dos) throws IOException {
        dos.writeBoolean(lapide);
        dos.writeInt(id);
        writeFixedString(dos, nome, 100);
        writeFixedString(dos, email, 100);
    }

    public static Usuario desserializar(DataInputStream dis) throws IOException {
        Usuario u = new Usuario();
        u.lapide = dis.readBoolean();
        u.id = dis.readInt();
        u.nome = readFixedString(dis, 100);
        u.email = readFixedString(dis, 100);
        return u;
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

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isLapide() { return lapide; }
    public void setLapide(boolean lapide) { this.lapide = lapide; }

    @Override
    public String toString() {
        return "{\"id\":" + id + ",\"nome\":\"" + nome.trim() + "\",\"email\":\"" + email.trim() + "\",\"lapide\":" + lapide + "}";
    }
}
