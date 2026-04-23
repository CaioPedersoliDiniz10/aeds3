package dao;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.Livro;

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
    private static final int ORDEM_ARVORE_BPLUS = 4;
    private final ArvoreBPlus indiceBPlusPorId;

    public LivroDAO(String caminho) {
        this.caminho = caminho;
        inicializarArquivo();
        this.indiceBPlusPorId = new ArvoreBPlus(ORDEM_ARVORE_BPLUS);
        try {
            reconstruirIndiceBPlusInterno();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao construir indice B+ inicial de livros.", e);
        }
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
        long pos = offsetRegistro(cab[1]);

        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            raf.seek(pos);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            l.serializar(dos);
            dos.flush();
            raf.write(baos.toByteArray());
        }

        indiceBPlusPorId.inserir(l.getId(), pos);
        atualizarCabecalho(novoId, cab[1] + 1);
        return l;
    }

    public Livro buscarPorId(int id) throws IOException {
        return buscarPorIdComIndice(id);
    }

    /**
     * Busca direta por ID usando indice Arvore B+ (com fallback linear).
     */
    public Livro buscarPorIdComIndice(int id) throws IOException {
        Long pos = indiceBPlusPorId.buscar(id);
        if (pos != null) {
            Livro l = lerRegistroNoOffset(pos);
            if (l != null && !l.isLapide() && l.getId() == id) {
                return l;
            }
        }
        return buscarPorIdLinear(id);
    }

    private Livro buscarPorIdLinear(int id) throws IOException {
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

    private Livro lerRegistroNoOffset(long pos) throws IOException {
        if (pos < HEADER_SIZE) {
            return null;
        }

        try (RandomAccessFile raf = new RandomAccessFile(caminho, "r")) {
            if (pos + Livro.TAMANHO > raf.length()) {
                return null;
            }

            raf.seek(pos);
            byte[] buf = new byte[Livro.TAMANHO];
            raf.readFully(buf);
            return Livro.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
        }
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

                    // Mantem indice consistente removendo chaves lapidadas via reconstrucao.
                    reconstruirIndiceBPlusInterno();
                    return true;
                }
            }
        }
        return false;
    }

    public List<Livro> listarAtivosOrdenadosExternamente(String atributo) throws IOException {
        return OrdenacaoExternaLivro.ordenar(caminho, atributo, 64);
    }

    public Map<String, Object> obterEstatisticasIndiceBPlus() {
        return indiceBPlusPorId.obterEstatisticas();
    }

    public void reconstruirIndiceBPlus() throws IOException {
        reconstruirIndiceBPlusInterno();
    }

    private void reconstruirIndiceBPlusInterno() throws IOException {
        indiceBPlusPorId.limpar();
        int[] cab = lerCabecalho();

        try (RandomAccessFile raf = new RandomAccessFile(caminho, "r")) {
            for (int i = 0; i < cab[1]; i++) {
                long pos = offsetRegistro(i);
                raf.seek(pos);

                byte[] buf = new byte[Livro.TAMANHO];
                raf.readFully(buf);
                Livro l = Livro.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));

                if (!l.isLapide()) {
                    indiceBPlusPorId.inserir(l.getId(), pos);
                }
            }
        }
    }
}
