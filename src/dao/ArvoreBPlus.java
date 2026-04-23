package dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacao inicial de Arvore B+ com suporte a insercao e busca exata.
 * Esta versao foi desenhada para indexar chave inteira para offset em arquivo.
 */
public class ArvoreBPlus {

    private final int ordem;
    private final int maxChaves;
    private No raiz;

    public ArvoreBPlus(int ordem) {
        if (ordem < 3) {
            throw new IllegalArgumentException("A ordem minima da Arvore B+ e 3.");
        }
        this.ordem = ordem;
        this.maxChaves = ordem - 1;
        this.raiz = new No(true);
    }

    /** Reinicia a estrutura mantendo a mesma ordem. */
    public void limpar() {
        this.raiz = new No(true);
    }

    /**
     * Insere (ou atualiza) uma chave no indice.
     */
    public void inserir(int chave, long valor) {
        Split split = inserirRecursivo(raiz, chave, valor);
        if (split != null) {
            No novaRaiz = new No(false);
            novaRaiz.chaves.add(split.chavePromovida);
            novaRaiz.filhos.add(raiz);
            novaRaiz.filhos.add(split.noDireita);
            raiz = novaRaiz;
        }
    }

    /**
     * Busca exata da chave e retorna o offset associado.
     */
    public Long buscar(int chave) {
        No folha = localizarFolha(raiz, chave);
        int idx = Collections.binarySearch(folha.chaves, chave);
        if (idx >= 0) {
            return folha.valores.get(idx);
        }
        return null;
    }

    public Map<String, Object> obterEstatisticas() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("ordem", ordem);
        stats.put("altura", altura(raiz));
        stats.put("totalNos", contarNos(raiz));
        stats.put("totalFolhas", contarFolhas(raiz));
        stats.put("totalChaves", contarChaves(raiz));
        return stats;
    }

    private Split inserirRecursivo(No no, int chave, long valor) {
        if (no.folha) {
            int idx = Collections.binarySearch(no.chaves, chave);
            if (idx >= 0) {
                no.valores.set(idx, valor);
                return null;
            }

            int pontoInsercao = -idx - 1;
            no.chaves.add(pontoInsercao, chave);
            no.valores.add(pontoInsercao, valor);

            if (no.chaves.size() <= maxChaves) {
                return null;
            }
            return dividirFolha(no);
        }

        int idxFilho = localizarIndiceFilho(no, chave);
        Split splitFilho = inserirRecursivo(no.filhos.get(idxFilho), chave, valor);
        if (splitFilho == null) {
            return null;
        }

        no.chaves.add(idxFilho, splitFilho.chavePromovida);
        no.filhos.add(idxFilho + 1, splitFilho.noDireita);

        if (no.chaves.size() <= maxChaves) {
            return null;
        }
        return dividirInterno(no);
    }

    private Split dividirFolha(No folha) {
        int meio = (folha.chaves.size() + 1) / 2;

        No direita = new No(true);
        direita.chaves.addAll(folha.chaves.subList(meio, folha.chaves.size()));
        direita.valores.addAll(folha.valores.subList(meio, folha.valores.size()));

        folha.chaves = new ArrayList<>(folha.chaves.subList(0, meio));
        folha.valores = new ArrayList<>(folha.valores.subList(0, meio));

        direita.proximo = folha.proximo;
        folha.proximo = direita;

        int chavePromovida = direita.chaves.get(0);
        return new Split(chavePromovida, direita);
    }

    private Split dividirInterno(No interno) {
        int meio = interno.chaves.size() / 2;
        int chavePromovida = interno.chaves.get(meio);

        No direita = new No(false);
        direita.chaves.addAll(interno.chaves.subList(meio + 1, interno.chaves.size()));
        direita.filhos.addAll(interno.filhos.subList(meio + 1, interno.filhos.size()));

        interno.chaves = new ArrayList<>(interno.chaves.subList(0, meio));
        interno.filhos = new ArrayList<>(interno.filhos.subList(0, meio + 1));

        return new Split(chavePromovida, direita);
    }

    private No localizarFolha(No noAtual, int chave) {
        No no = noAtual;
        while (!no.folha) {
            int idxFilho = localizarIndiceFilho(no, chave);
            no = no.filhos.get(idxFilho);
        }
        return no;
    }

    private int localizarIndiceFilho(No no, int chave) {
        int idx = 0;
        while (idx < no.chaves.size() && chave >= no.chaves.get(idx)) {
            idx++;
        }
        return idx;
    }

    private int altura(No no) {
        int h = 1;
        No cursor = no;
        while (!cursor.folha) {
            h++;
            cursor = cursor.filhos.get(0);
        }
        return h;
    }

    private int contarNos(No no) {
        int total = 1;
        if (!no.folha) {
            for (No filho : no.filhos) {
                total += contarNos(filho);
            }
        }
        return total;
    }

    private int contarFolhas(No no) {
        if (no.folha) {
            return 1;
        }
        int total = 0;
        for (No filho : no.filhos) {
            total += contarFolhas(filho);
        }
        return total;
    }

    private int contarChaves(No no) {
        int total = no.chaves.size();
        if (!no.folha) {
            for (No filho : no.filhos) {
                total += contarChaves(filho);
            }
        }
        return total;
    }

    private static class Split {
        private final int chavePromovida;
        private final No noDireita;

        private Split(int chavePromovida, No noDireita) {
            this.chavePromovida = chavePromovida;
            this.noDireita = noDireita;
        }
    }

    private static class No {
        private final boolean folha;
        private List<Integer> chaves = new ArrayList<>();
        private List<No> filhos = new ArrayList<>();
        private List<Long> valores = new ArrayList<>();
        private No proximo;

        private No(boolean folha) {
            this.folha = folha;
        }
    }
}
