package dao;

import model.Emprestimo;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para Emprestimo.
 *
 * Estrutura do arquivo binário:
 *   Cabeçalho (8 bytes): [int ultimoId (4)] [int totalRegistros (4)]
 *   Registros: sequência de registros de tamanho fixo (Emprestimo.TAMANHO bytes cada)
 */
public class EmprestimoDAO {

    private final String caminho;
    private static final int HEADER_SIZE = 8;

    public EmprestimoDAO(String caminho) {
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
        return HEADER_SIZE + (long) idx * Emprestimo.TAMANHO;
    }

    public Emprestimo criar(Emprestimo e) throws IOException {
        int[] cab = lerCabecalho();
        int novoId = cab[0] + 1;
        e.setId(novoId);

        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            raf.seek(offsetRegistro(cab[1]));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            e.serializar(dos);
            dos.flush();
            raf.write(baos.toByteArray());
        }
        atualizarCabecalho(novoId, cab[1] + 1);
        return e;
    }

    public Emprestimo buscarPorId(int id) throws IOException {
        int[] cab = lerCabecalho();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
            dis.skipBytes(HEADER_SIZE);
            for (int i = 0; i < cab[1]; i++) {
                byte[] buf = new byte[Emprestimo.TAMANHO];
                if (dis.read(buf) < Emprestimo.TAMANHO) break;
                Emprestimo em = Emprestimo.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!em.isLapide() && em.getId() == id) return em;
            }
        }
        return null;
    }

    public List<Emprestimo> listarAtivos() throws IOException {
        List<Emprestimo> lista = new ArrayList<>();
        int[] cab = lerCabecalho();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
            dis.skipBytes(HEADER_SIZE);
            for (int i = 0; i < cab[1]; i++) {
                byte[] buf = new byte[Emprestimo.TAMANHO];
                if (dis.read(buf) < Emprestimo.TAMANHO) break;
                Emprestimo em = Emprestimo.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!em.isLapide()) lista.add(em);
            }
        }
        return lista;
    }

    public List<Emprestimo> buscarPorUsuario(int idUsuario) throws IOException {
        List<Emprestimo> lista = new ArrayList<>();
        for (Emprestimo e : listarAtivos()) {
            if (e.getIdUsuario() == idUsuario) lista.add(e);
        }
        return lista;
    }

    public boolean atualizar(Emprestimo atualizado) throws IOException {
        int[] cab = lerCabecalho();
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            for (int i = 0; i < cab[1]; i++) {
                long pos = offsetRegistro(i);
                raf.seek(pos);
                byte[] buf = new byte[Emprestimo.TAMANHO];
                raf.readFully(buf);
                Emprestimo e = Emprestimo.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!e.isLapide() && e.getId() == atualizado.getId()) {
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
                byte[] buf = new byte[Emprestimo.TAMANHO];
                raf.readFully(buf);
                Emprestimo e = Emprestimo.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!e.isLapide() && e.getId() == id) {
                    e.setLapide(true);
                    raf.seek(pos);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    e.serializar(dos);
                    dos.flush();
                    raf.write(baos.toByteArray());
                    return true;
                }
            }
        }
        return false;
    }
}
