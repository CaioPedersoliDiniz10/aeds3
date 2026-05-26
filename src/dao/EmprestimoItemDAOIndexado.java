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
    private final String caminhoIndiceEmprestimo;
    private final String caminhoIndiceLivro;
    private HashExtensivel indiceEmprestimo; // índice: idEmprestimo -> posições (offsets)
    private HashExtensivel indiceLivro;      // índice: idLivro -> posições (offsets)
    private final ArvoreBPlus indiceOrdenadoPorLivro; // índice ordenado: (idEmprestimo,idLivro) -> posição do item

    // Multiplicador para compor chave (idEmprestimo,idLivro) em um int.
    // Assumimos idLivro < 1_000_000 (suficiente para o escopo do trabalho).
    private static final int CHAVE_MULT = 1_000_000;
    private static final int HEADER_SIZE = 8;

    public EmprestimoItemDAOIndexado(String caminho) {
        this.caminho = caminho;
        this.caminhoIndiceEmprestimo = caminho + ".idx";
        this.caminhoIndiceLivro = caminho + ".livro.idx";
        this.indiceOrdenadoPorLivro = new ArvoreBPlus(8);
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
        File fEmp = new File(caminhoIndiceEmprestimo);
        if (fEmp.exists()) {
            try {
                indiceEmprestimo = HashExtensivel.carregar(caminhoIndiceEmprestimo);
            } catch (IOException | ClassNotFoundException e) {
                indiceEmprestimo = new HashExtensivel();
            }
        } else {
            indiceEmprestimo = new HashExtensivel();
        }

        File fLiv = new File(caminhoIndiceLivro);
        if (fLiv.exists()) {
            try {
                indiceLivro = HashExtensivel.carregar(caminhoIndiceLivro);
            } catch (IOException | ClassNotFoundException e) {
                indiceLivro = new HashExtensivel();
            }
        } else {
            indiceLivro = new HashExtensivel();
        }

        // Sempre sincroniza índices a partir do .dat para evitar .idx desatualizado.
        reconstruirIndice();
    }

    private void reconstruirIndice() {
        try {
            indiceEmprestimo = new HashExtensivel();
            indiceLivro = new HashExtensivel();
            indiceOrdenadoPorLivro.limpar();
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
                        indiceLivro.inserir(item.getIdLivro(), pos);
                        indiceOrdenadoPorLivro.inserir(chaveEmprestimoLivro(item.getIdEmprestimo(), item.getIdLivro()), pos);
                    }
                }
            }
            salvarIndice();
        } catch (IOException e) {
            System.err.println("Erro ao reconstruir índice: " + e.getMessage());
        }
    }

    private int chaveEmprestimoLivro(int idEmprestimo, int idLivro) {
        return idEmprestimo * CHAVE_MULT + idLivro;
    }

    private int[] parseChaveCompostaFromIdSintetico(int idSintetico) {
        int idEmprestimo = idSintetico / CHAVE_MULT;
        int idLivro = idSintetico % CHAVE_MULT;
        return new int[]{idEmprestimo, idLivro};
    }

    private void salvarIndice() {
        try {
            indiceEmprestimo.salvar(caminhoIndiceEmprestimo);
            indiceLivro.salvar(caminhoIndiceLivro);
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
        long pos = offsetRegistro(cab[1]);
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            raf.seek(pos);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            item.serializar(dos);
            dos.flush();
            raf.write(baos.toByteArray());
        }
        // Mantém padrão de cabeçalho (ultimoId,total), mas ultimoId não é usado em PK composta.
        atualizarCabecalho(0, cab[1] + 1);
        
        // Atualizar índice
        indiceEmprestimo.inserir(item.getIdEmprestimo(), pos);
        indiceLivro.inserir(item.getIdLivro(), pos);
        indiceOrdenadoPorLivro.inserir(chaveEmprestimoLivro(item.getIdEmprestimo(), item.getIdLivro()), pos);
        salvarIndice();
        
        return item;
    }

    public EmprestimoItem buscarPorId(int id) throws IOException {
        int[] chave = parseChaveCompostaFromIdSintetico(id);
        return buscarPorChaveComposta(chave[0], chave[1]);
    }

    public EmprestimoItem buscarPorChaveComposta(int idEmprestimo, int idLivro) throws IOException {
        List<Long> posicoes = indiceEmprestimo.buscar(idEmprestimo);
        if (posicoes.isEmpty()) {
            return null;
        }

        try (RandomAccessFile raf = new RandomAccessFile(caminho, "r")) {
            for (long pos : posicoes) {
                if (pos < HEADER_SIZE) continue;
                raf.seek(pos);
                byte[] buf = new byte[EmprestimoItem.TAMANHO];
                if (raf.read(buf) != EmprestimoItem.TAMANHO) continue;
                EmprestimoItem item = EmprestimoItem.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!item.isLapide() && item.getIdEmprestimo() == idEmprestimo && item.getIdLivro() == idLivro) {
                    return item;
                }
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

        try (RandomAccessFile raf = new RandomAccessFile(caminho, "r")) {
            for (long pos : posicoes) {
                if (pos < HEADER_SIZE) continue;
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

    /**
     * Acesso pelo outro lado do N:N: listar todos os empréstimos que contêm um livro.
     */
    public List<EmprestimoItem> buscarPorLivro(int idLivro) throws IOException {
        List<EmprestimoItem> lista = new ArrayList<>();
        List<Long> posicoes = indiceLivro.buscar(idLivro);

        try (RandomAccessFile raf = new RandomAccessFile(caminho, "r")) {
            for (long pos : posicoes) {
                if (pos < HEADER_SIZE) continue;
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

    /**
     * Listagem ordenada por idLivro usando Árvore B+.
     * Retorna apenas itens ativos (sem lápide).
     */
    public List<EmprestimoItem> buscarPorEmprestimoOrdenadoPorLivro(int idEmprestimo) throws IOException {
        int min = chaveEmprestimoLivro(idEmprestimo, 0);
        int max = chaveEmprestimoLivro(idEmprestimo, CHAVE_MULT - 1);

        List<EmprestimoItem> lista = new ArrayList<>();
        List<Long> posicoesOrdenadas = indiceOrdenadoPorLivro.buscarIntervalo(min, max);

        try (RandomAccessFile raf = new RandomAccessFile(caminho, "r")) {
            for (long pos : posicoesOrdenadas) {
                if (pos < HEADER_SIZE) {
                    continue;
                }
                raf.seek(pos);
                byte[] buf = new byte[EmprestimoItem.TAMANHO];
                if (raf.read(buf) == EmprestimoItem.TAMANHO) {
                    EmprestimoItem item = EmprestimoItem.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                    if (!item.isLapide() && item.getIdEmprestimo() == idEmprestimo) {
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
        int[] chave = parseChaveCompostaFromIdSintetico(id);
        return excluirPorChaveComposta(chave[0], chave[1]);
    }

    public boolean excluirPorChaveComposta(int idEmprestimo, int idLivro) throws IOException {
        List<Long> posicoes = indiceEmprestimo.buscar(idEmprestimo);
        if (posicoes.isEmpty()) {
            return false;
        }

        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            for (long pos : new ArrayList<>(posicoes)) {
                if (pos < HEADER_SIZE) continue;
                raf.seek(pos);
                byte[] buf = new byte[EmprestimoItem.TAMANHO];
                if (raf.read(buf) != EmprestimoItem.TAMANHO) continue;
                EmprestimoItem item = EmprestimoItem.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!item.isLapide() && item.getIdEmprestimo() == idEmprestimo && item.getIdLivro() == idLivro) {
                    indiceEmprestimo.remover(idEmprestimo, pos);
                    indiceLivro.remover(idLivro, pos);
                    indiceOrdenadoPorLivro.inserir(chaveEmprestimoLivro(idEmprestimo, idLivro), -1L);

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

    /**
     * Exclusão lógica (lápide) em cascata: remove todos os itens de um empréstimo.
     * Mantém o índice hash consistente e invalida as chaves no índice B+.
     */
    public int excluirPorEmprestimo(int idEmprestimo) throws IOException {
        List<Long> posicoes = indiceEmprestimo.buscar(idEmprestimo);
        if (posicoes.isEmpty()) {
            return 0;
        }

        int removidos = 0;
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            for (long pos : new ArrayList<>(posicoes)) {
                raf.seek(pos);
                byte[] buf = new byte[EmprestimoItem.TAMANHO];
                if (raf.read(buf) != EmprestimoItem.TAMANHO) {
                    continue;
                }
                EmprestimoItem item = EmprestimoItem.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (item.isLapide() || item.getIdEmprestimo() != idEmprestimo) {
                    continue;
                }

                indiceEmprestimo.remover(idEmprestimo, pos);
                indiceLivro.remover(item.getIdLivro(), pos);
                indiceOrdenadoPorLivro.inserir(chaveEmprestimoLivro(item.getIdEmprestimo(), item.getIdLivro()), -1L);

                item.setLapide(true);
                raf.seek(pos);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                item.serializar(dos);
                dos.flush();
                raf.write(baos.toByteArray());
                removidos++;
            }
        }

        salvarIndice();
        return removidos;
    }

    public boolean atualizar(EmprestimoItem atualizado) throws IOException {
        // Em PK composta, não permitimos alterar (idEmprestimo,idLivro) via update.
        // Atualização suportada: quantidade.
        int idEmprestimo = atualizado.getIdEmprestimo();
        int idLivro = atualizado.getIdLivro();

        List<Long> posicoes = indiceEmprestimo.buscar(idEmprestimo);
        if (posicoes.isEmpty()) {
            return false;
        }

        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            for (long pos : posicoes) {
                if (pos < HEADER_SIZE) continue;
                raf.seek(pos);
                byte[] buf = new byte[EmprestimoItem.TAMANHO];
                if (raf.read(buf) != EmprestimoItem.TAMANHO) continue;
                EmprestimoItem item = EmprestimoItem.desserializar(new DataInputStream(new ByteArrayInputStream(buf)));
                if (!item.isLapide() && item.getIdEmprestimo() == idEmprestimo && item.getIdLivro() == idLivro) {
                    item.setQuantidade(atualizado.getQuantidade());
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

    public Map<String, Object> obterEstatisticasIndice() {
        return indiceEmprestimo.obterEstatisticas();
    }
}
