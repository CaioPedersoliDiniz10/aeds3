package dao;

import model.Livro;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para Livro.
 *
 * Estrutura do arquivo binário:
 *   Cabeçalho (8 bytes): [int ultimoId (4)] [int totalRegistros (4)]
 *   Registros: sequência de registros de tamanho fixo (Livro.TAMANHO bytes cada)
 */
public class LivroDAO {

    private final String caminho;
    private static final int HEADER_SIZE = 8;

    public LivroDAO(String caminho) {
        this.caminho = caminho;
        inicializarArquivo();
    }

    private void inicializarArquivo() {
        File f = new File(caminho);
        if (!f.exists()) {
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(f))) {
                dos.writeInt(0);
                dos.writeInt(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int[] lerCabecalho() throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
            return new int[]{dis.readInt(), dis.readInt()};
        }
    }

    private void atualizarCabecalho(int ultimoId, int total) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            raf.seek(0);
            raf.writeInt(ultimoId);
            raf.writeInt(total);
        }
    }

    private long offsetRegistro(int idx) {
        return HEADER_SIZE + (long) idx * Livro.TAMANHO;
    }

    public Livro criar(Livro l) throws IOException {
        int[] cab = lerCabecalho();
        int novoId = cab[0] + 1;
        l.setId(novoId);

        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            raf.seek(offsetRegistro(cab[1]));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            l.serializar(dos);
            dos.flush();
            raf.write(baos.toByteArray());
        }
        atualizarCabecalho(novoId, cab[1] + 1);
        return l;
    }

    public Livro buscarPorId(int id) throws IOException {
        int[] cab = lerCabecalho();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
            dis.skipBytes(HEADER_SIZE);
            for (int i = 0; i < cab[1]; i++) {
                byte[] buf = new byte[Livro.TAMANHO];
                if (dis.read(buf) < Livro.TAMANHO) break;
                Livro l = Livro.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!l.isLapide() && l.getId() == id) return l;
            }
        }
        return null;
    }

    public List<Livro> listarAtivos() throws IOException {
        List<Livro> lista = new ArrayList<>();
        int[] cab = lerCabecalho();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
            dis.skipBytes(HEADER_SIZE);
            for (int i = 0; i < cab[1]; i++) {
                byte[] buf = new byte[Livro.TAMANHO];
                if (dis.read(buf) < Livro.TAMANHO) break;
                Livro l = Livro.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!l.isLapide()) lista.add(l);
            }
        }
        return lista;
    }

    public boolean atualizar(Livro atualizado) throws IOException {
        int[] cab = lerCabecalho();
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            for (int i = 0; i < cab[1]; i++) {
                long pos = offsetRegistro(i);
                raf.seek(pos);
                byte[] buf = new byte[Livro.TAMANHO];
                raf.readFully(buf);
                Livro l = Livro.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!l.isLapide() && l.getId() == atualizado.getId()) {
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

    public boolean excluir(int id) throws IOException {
        int[] cab = lerCabecalho();
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            for (int i = 0; i < cab[1]; i++) {
                long pos = offsetRegistro(i);
                raf.seek(pos);
                byte[] buf = new byte[Livro.TAMANHO];
                raf.readFully(buf);
                Livro l = Livro.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!l.isLapide() && l.getId() == id) {
                    l.setLapide(true);
                    raf.seek(pos);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    l.serializar(dos);
                    dos.flush();
                    raf.write(baos.toByteArray());
                    return true;
                }
            }
        }
        return false;
    }
}
