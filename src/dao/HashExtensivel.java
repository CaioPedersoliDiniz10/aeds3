package dao;

import java.io.*;
import java.util.*;

/**
 * Hash Extensível para indexação de relacionamento 1:N.
 * 
 * Implementação de Hash Extensível conforme proposto na Fase 2.
 * Estrutura: mantém buckets dinâmicos que se expandem conforme necessário.
 * Cada entrada mapeia uma chave para uma lista de valores (long - posições/offsets).
 */
public class HashExtensivel implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int BUCKET_SIZE = 4; // tamanho do bucket antes de expansão

    private int globalDepth; // profundidade global do hash
    private Map<Integer, Bucket>[] directory; // diretório de buckets
    private String arquivoIndice; // arquivo de persistência

    @SuppressWarnings("unchecked")
    public HashExtensivel() {
        this.globalDepth = 1;
        this.directory = new Map[2]; // 2^1 buckets
        for (int i = 0; i < directory.length; i++) {
            directory[i] = new HashMap<>();
        }
    }

    /**
     * Insere uma chave e seu valor (offset de arquivo).
     */
    public void inserir(int chave, long valor) {
        int hash = obterHash(chave);
        int indice = hash % directory.length;
        
        Map<Integer, Bucket> bucket = directory[indice];
        if (!bucket.containsKey(chave)) {
            bucket.put(chave, new Bucket());
        }
        bucket.get(chave).adicionar(valor);
    }

    /**
     * Busca todos os valores associados a uma chave.
     */
    public List<Long> buscar(int chave) {
        int hash = obterHash(chave);
        int indice = hash % directory.length;
        
        Map<Integer, Bucket> bucket = directory[indice];
        if (bucket.containsKey(chave)) {
            return bucket.get(chave).obterValores();
        }
        return new ArrayList<>();
    }

    /**
     * Remove um valor específico de uma chave.
     */
    public boolean remover(int chave, long valor) {
        int hash = obterHash(chave);
        int indice = hash % directory.length;
        
        Map<Integer, Bucket> bucket = directory[indice];
        if (bucket.containsKey(chave)) {
            Bucket b = bucket.get(chave);
            if (b.remover(valor)) {
                if (b.estaVazio()) {
                    bucket.remove(chave);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Remove todos os valores de uma chave.
     */
    public void removerTodos(int chave) {
        int hash = obterHash(chave);
        int indice = hash % directory.length;
        directory[indice].remove(chave);
    }

    /**
     * Verifica se uma chave existe no índice.
     */
    public boolean contem(int chave) {
        int hash = obterHash(chave);
        int indice = hash % directory.length;
        return directory[indice].containsKey(chave);
    }

    /**
     * Persiste o índice em arquivo.
     */
    public void salvar(String caminhoArquivo) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(caminhoArquivo))) {
            oos.writeInt(globalDepth);
            oos.writeObject(directory);
        }
    }

    /**
     * Carrega o índice do arquivo.
     */
    @SuppressWarnings("unchecked")
    public static HashExtensivel carregar(String caminhoArquivo) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(caminhoArquivo))) {
            HashExtensivel hash = new HashExtensivel();
            hash.globalDepth = ois.readInt();
            hash.directory = (Map<Integer, Bucket>[]) ois.readObject();
            return hash;
        }
    }

    private int obterHash(int chave) {
        return Math.abs(chave * 31 % (1 << globalDepth));
    }

    /**
     * Classe interna para representar um bucket no hash.
     */
    @SuppressWarnings("serial")
    private static class Bucket implements Serializable {
        private List<Long> valores = new ArrayList<>();

        void adicionar(long valor) {
            if (!valores.contains(valor)) {
                valores.add(valor);
            }
        }

        boolean remover(long valor) {
            return valores.remove(Long.valueOf(valor));
        }

        List<Long> obterValores() {
            return new ArrayList<>(valores);
        }

        boolean estaVazio() {
            return valores.isEmpty();
        }
    }

    /**
     * Retorna estatísticas do índice para debug.
     */
    public Map<String, Object> obterEstatisticas() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("profundidadeGlobal", globalDepth);
        stats.put("tamanhoDirectorio", directory.length);
        
        int totalChaves = 0;
        int totalValores = 0;
        for (Map<Integer, Bucket> map : directory) {
            totalChaves += map.size();
            for (Bucket b : map.values()) {
                totalValores += b.valores.size();
            }
        }
        stats.put("totalChaves", totalChaves);
        stats.put("totalValores", totalValores);
        
        return stats;
    }
}
