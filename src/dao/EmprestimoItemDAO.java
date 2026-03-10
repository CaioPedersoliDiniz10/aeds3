package dao;

import model.EmprestimoItem;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para EmprestimoItem (itens/livros de um empréstimo).
 *
 * Estrutura do arquivo binário:
 *   Cabeçalho (8 bytes): [int ultimoId (4)] [int totalRegistros (4)]
 *   Registros: sequência de registros fixos (EmprestimoItem.TAMANHO bytes cada)
 */
public class EmprestimoItemDAO {

    private final String caminho;
    private static final int HEADER_SIZE = 8;

    public EmprestimoItemDAO(String caminho) {
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
        return HEADER_SIZE + (long) idx * EmprestimoItem.TAMANHO;
    }

    public EmprestimoItem criar(EmprestimoItem item) throws IOException {
        int[] cab = lerCabecalho();
        int novoId = cab[0] + 1;
        item.setId(novoId);

        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            raf.seek(offsetRegistro(cab[1]));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            item.serializar(dos);
            dos.flush();
            raf.write(baos.toByteArray());
        }
        atualizarCabecalho(novoId, cab[1] + 1);
        return item;
    }

    public EmprestimoItem buscarPorId(int id) throws IOException {
        int[] cab = lerCabecalho();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
            dis.skipBytes(HEADER_SIZE);
            for (int i = 0; i < cab[1]; i++) {
                byte[] buf = new byte[EmprestimoItem.TAMANHO];
                if (dis.read(buf) < EmprestimoItem.TAMANHO) break;
                EmprestimoItem item = EmprestimoItem.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!item.isLapide() && item.getId() == id) return item;
            }
        }
        return null;
    }

    /** Lista todos os itens ativos de um determinado empréstimo. */
    public List<EmprestimoItem> buscarPorEmprestimo(int idEmprestimo) throws IOException {
        List<EmprestimoItem> lista = new ArrayList<>();
        int[] cab = lerCabecalho();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
            dis.skipBytes(HEADER_SIZE);
            for (int i = 0; i < cab[1]; i++) {
                byte[] buf = new byte[EmprestimoItem.TAMANHO];
                if (dis.read(buf) < EmprestimoItem.TAMANHO) break;
                EmprestimoItem item = EmprestimoItem.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!item.isLapide() && item.getIdEmprestimo() == idEmprestimo) lista.add(item);
            }
        }
        return lista;
    }

    public List<EmprestimoItem> listarAtivos() throws IOException {
        List<EmprestimoItem> lista = new ArrayList<>();
        int[] cab = lerCabecalho();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
            dis.skipBytes(HEADER_SIZE);
            for (int i = 0; i < cab[1]; i++) {
                byte[] buf = new byte[EmprestimoItem.TAMANHO];
                if (dis.read(buf) < EmprestimoItem.TAMANHO) break;
                EmprestimoItem item = EmprestimoItem.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!item.isLapide()) lista.add(item);
            }
        }
        return lista;
    }

    /** Exclusão lógica por lápide. */
    public boolean excluir(int id) throws IOException {
        int[] cab = lerCabecalho();
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            for (int i = 0; i < cab[1]; i++) {
                long pos = offsetRegistro(i);
                raf.seek(pos);
                byte[] buf = new byte[EmprestimoItem.TAMANHO];
                raf.readFully(buf);
                EmprestimoItem item = EmprestimoItem.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!item.isLapide() && item.getId() == id) {
                    item.setLapide(true);
                    raf.seek(pos);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    item.serializar(dos);
                    dos.flush();
                    raf.write(baos.toByteArray());
                    return true;
                }
            }
        }
        return false;
    }
}
