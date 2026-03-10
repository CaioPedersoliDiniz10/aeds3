package dao;

import model.Usuario;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para Usuario.
 *
 * Estrutura do arquivo binário:
 *   Cabeçalho (8 bytes): [int ultimoId (4)] [int totalRegistros (4)]
 *   Registros: sequência de registros de tamanho fixo (Usuario.TAMANHO bytes cada)
 */
public class UsuarioDAO {

    private final String caminho;
    private static final int HEADER_SIZE = 8; // 4 (ultimoId) + 4 (totalRegistros)

    public UsuarioDAO(String caminho) {
        this.caminho = caminho;
        inicializarArquivo();
    }

    private void inicializarArquivo() {
        File f = new File(caminho);
        if (!f.exists()) {
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(f))) {
                dos.writeInt(0); // ultimoId
                dos.writeInt(0); // totalRegistros
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int[] lerCabecalho() throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
            int ultimoId = dis.readInt();
            int total = dis.readInt();
            return new int[]{ultimoId, total};
        }
    }

    private void atualizarCabecalho(int ultimoId, int total) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            raf.seek(0);
            raf.writeInt(ultimoId);
            raf.writeInt(total);
        }
    }

    /** Retorna o offset (em bytes) do registro de índice idx (0-based). */
    private long offsetRegistro(int idx) {
        return HEADER_SIZE + (long) idx * Usuario.TAMANHO;
    }

    public Usuario criar(Usuario u) throws IOException {
        int[] cab = lerCabecalho();
        int novoId = cab[0] + 1;
        int total = cab[1] + 1;
        u.setId(novoId);

        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            raf.seek(offsetRegistro(cab[1])); // posição depois do último registro
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            u.serializar(dos);
            dos.flush();
            raf.write(baos.toByteArray());
        }
        atualizarCabecalho(novoId, total);
        return u;
    }

    public Usuario buscarPorId(int id) throws IOException {
        int[] cab = lerCabecalho();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
            dis.skipBytes(HEADER_SIZE);
            for (int i = 0; i < cab[1]; i++) {
                byte[] buf = new byte[Usuario.TAMANHO];
                int read = dis.read(buf);
                if (read < Usuario.TAMANHO) break;
                Usuario u = Usuario.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!u.isLapide() && u.getId() == id) return u;
            }
        }
        return null;
    }

    public List<Usuario> listarAtivos() throws IOException {
        List<Usuario> lista = new ArrayList<>();
        int[] cab = lerCabecalho();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
            dis.skipBytes(HEADER_SIZE);
            for (int i = 0; i < cab[1]; i++) {
                byte[] buf = new byte[Usuario.TAMANHO];
                int read = dis.read(buf);
                if (read < Usuario.TAMANHO) break;
                Usuario u = Usuario.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!u.isLapide()) lista.add(u);
            }
        }
        return lista;
    }

    public boolean atualizar(Usuario atualizado) throws IOException {
        int[] cab = lerCabecalho();
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            for (int i = 0; i < cab[1]; i++) {
                long pos = offsetRegistro(i);
                raf.seek(pos);
                byte[] buf = new byte[Usuario.TAMANHO];
                raf.readFully(buf);
                Usuario u = Usuario.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!u.isLapide() && u.getId() == atualizado.getId()) {
                    raf.seek(pos);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    atualizado.serializar(dos);
                    dos.flush();
                    raf.write(baos.toByteArray());
                    return true;
                }
            }
        }
        return false;
    }

    /** Exclusão lógica: marca a lápide sem remover o registro do arquivo. */
    public boolean excluir(int id) throws IOException {
        int[] cab = lerCabecalho();
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            for (int i = 0; i < cab[1]; i++) {
                long pos = offsetRegistro(i);
                raf.seek(pos);
                byte[] buf = new byte[Usuario.TAMANHO];
                raf.readFully(buf);
                Usuario u = Usuario.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!u.isLapide() && u.getId() == id) {
                    u.setLapide(true);
                    raf.seek(pos);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    u.serializar(dos);
                    dos.flush();
                    raf.write(baos.toByteArray());
                    return true;
                }
            }
        }
        return false;
    }
}
