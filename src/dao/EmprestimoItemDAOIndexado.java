package dao;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.EmprestimoItem;

/**
 * DAO para EmprestimoItem com índice Hash Extensível para otimizar buscas por idEmprestimo.
 * 
 * O índice é mantido em memória durante a execução e persistido em disco.
 * Relacionamento 1:N: Um Emprestimo pode ter múltiplos EmprestimoItem (Livros).
 */
public class EmprestimoItemDAOIndexado {

    private final String caminho;
    private final String caminhoIndice;
    private HashExtensivel indiceEmprestimo; // índice: idEmprestimo -> posições dos itens
    private static final int HEADER_SIZE = 8;

    public EmprestimoItemDAOIndexado(String caminho) {
        this.caminho = caminho;
        this.caminhoIndice = caminho + ".idx";
        inicializarArquivo();
        carregarIndice();
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

    private void carregarIndice() {
        File f = new File(caminhoIndice);
        if (f.exists()) {
            try {
                indiceEmprestimo = HashExtensivel.carregar(caminhoIndice);
            } catch (IOException | ClassNotFoundException e) {
                indiceEmprestimo = new HashExtensivel();
                reconstruirIndice();
            }
        } else {
            indiceEmprestimo = new HashExtensivel();
            reconstruirIndice();
        }
    }

    private void reconstruirIndice() {
        try {
            indiceEmprestimo = new HashExtensivel();
            int[] cab = lerCabecalho();
            try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
                dis.skipBytes(HEADER_SIZE);
                for (int i = 0; i < cab[1]; i++) {
                    byte[] buf = new byte[EmprestimoItem.TAMANHO];
                    if (dis.read(buf) < EmprestimoItem.TAMANHO) break;
                    EmprestimoItem item = EmprestimoItem.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                    if (!item.isLapide()) {
                        long pos = HEADER_SIZE + (long) i * EmprestimoItem.TAMANHO;
                        indiceEmprestimo.inserir(item.getIdEmprestimo(), pos);
                    }
                }
            }
            salvarIndice();
        } catch (IOException e) {
            System.err.println("Erro ao reconstruir índice: " + e.getMessage());
        }
    }

    private void salvarIndice() {
        try {
            indiceEmprestimo.salvar(caminhoIndice);
        } catch (IOException e) {
            System.err.println("Erro ao salvar índice: " + e.getMessage());
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

        long pos = offsetRegistro(cab[1]);
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            raf.seek(pos);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            item.serializar(dos);
            dos.flush();
            raf.write(baos.toByteArray());
        }
        atualizarCabecalho(novoId, cab[1] + 1);
        
        // Atualizar índice
        indiceEmprestimo.inserir(item.getIdEmprestimo(), pos);
        salvarIndice();
        
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

    /**
     * Busca otimizada usando Hash Extensível para encontrar todos os itens de um empréstimo.
     * Tempo de acesso é O(1) médio em vez de O(n).
     */
    public List<EmprestimoItem> buscarPorEmprestimo(int idEmprestimo) throws IOException {
        List<EmprestimoItem> lista = new ArrayList<>();
        List<Long> posicoes = indiceEmprestimo.buscar(idEmprestimo);
        
        for (long pos : posicoes) {
            try (RandomAccessFile raf = new RandomAccessFile(caminho, "r")) {
                raf.seek(pos);
                byte[] buf = new byte[EmprestimoItem.TAMANHO];
                if (raf.read(buf) == EmprestimoItem.TAMANHO) {
                    EmprestimoItem item = EmprestimoItem.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                    if (!item.isLapide()) {
                        lista.add(item);
                    }
                }
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
                    // Remover do índice antes de marcar como lapide
                    indiceEmprestimo.remover(item.getIdEmprestimo(), pos);
                    
                    item.setLapide(true);
                    raf.seek(pos);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    item.serializar(dos);
                    dos.flush();
                    raf.write(baos.toByteArray());
                    
                    salvarIndice();
                    return true;
                }
            }
        }
        return false;
    }

    public boolean atualizar(EmprestimoItem atualizado) throws IOException {
        int[] cab = lerCabecalho();
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            for (int i = 0; i < cab[1]; i++) {
                long pos = offsetRegistro(i);
                raf.seek(pos);
                byte[] buf = new byte[EmprestimoItem.TAMANHO];
                raf.readFully(buf);
                EmprestimoItem item = EmprestimoItem.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!item.isLapide() && item.getId() == atualizado.getId()) {
                    // Se mudou o idEmprestimo, atualizar índice
                    if (item.getIdEmprestimo() != atualizado.getIdEmprestimo()) {
                        indiceEmprestimo.remover(item.getIdEmprestimo(), pos);
                        indiceEmprestimo.inserir(atualizado.getIdEmprestimo(), pos);
                    }
                    
                    raf.seek(pos);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    atualizado.serializar(dos);
                    dos.flush();
                    raf.write(baos.toByteArray());
                    
                    salvarIndice();
                    return true;
                }
            }
        }
        return false;
    }

    public Map<String, Object> obterEstatisticasIndice() {
        return indiceEmprestimo.obterEstatisticas();
    }
}
