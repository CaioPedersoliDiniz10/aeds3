package dao;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import model.Livro;

/**
 * Ordenacao externa para registros de Livro com estrategia de runs + merge k-way.
 */
public final class OrdenacaoExternaLivro {

    private static final int HEADER_SIZE = 8;

    private OrdenacaoExternaLivro() {
    }

    public static List<Livro> ordenar(String caminhoArquivoDados, String atributo, int tamanhoBloco) throws IOException {
        if (tamanhoBloco <= 0) {
            tamanhoBloco = 64;
        }

        Comparator<Livro> comparator = comparatorPorAtributo(atributo);
        List<File> runs = gerarRuns(caminhoArquivoDados, tamanhoBloco, comparator);

        try {
            return intercalarRuns(runs, comparator);
        } finally {
            for (File run : runs) {
                if (run.exists() && !run.delete()) {
                    run.deleteOnExit();
                }
            }
        }
    }

    private static List<File> gerarRuns(String caminhoArquivoDados, int tamanhoBloco, Comparator<Livro> comparator)
            throws IOException {
        List<File> runs = new ArrayList<>();

        File arquivoDados = new File(caminhoArquivoDados);
        if (!arquivoDados.exists() || arquivoDados.length() < HEADER_SIZE) {
            return runs;
        }

        try (DataInputStream dis = new DataInputStream(new FileInputStream(arquivoDados))) {
            dis.readInt(); // ultimoId
            int totalRegistros = dis.readInt();

            List<Livro> bloco = new ArrayList<>(tamanhoBloco);

            for (int i = 0; i < totalRegistros; i++) {
                byte[] registro = new byte[Livro.TAMANHO];
                try {
                    dis.readFully(registro);
                } catch (EOFException eof) {
                    break;
                }

                Livro livro = Livro.desserializar(new DataInputStream(new ByteArrayInputStream(registro)));
                if (!livro.isLapide()) {
                    bloco.add(livro);
                }

                if (bloco.size() == tamanhoBloco) {
                    runs.add(escreverRun(bloco, comparator));
                    bloco.clear();
                }
            }

            if (!bloco.isEmpty()) {
                runs.add(escreverRun(bloco, comparator));
            }
        }

        return runs;
    }

    private static File escreverRun(List<Livro> bloco, Comparator<Livro> comparator) throws IOException {
        bloco.sort(comparator);

        File run = File.createTempFile("livros_run_", ".tmp");
        run.deleteOnExit();

        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(run))) {
            dos.writeInt(bloco.size());
            for (Livro livro : bloco) {
                livro.serializar(dos);
            }
        }

        return run;
    }

    private static List<Livro> intercalarRuns(List<File> runs, Comparator<Livro> comparator) throws IOException {
        List<Livro> resultado = new ArrayList<>();
        if (runs.isEmpty()) {
            return resultado;
        }

        List<RunCursor> cursores = new ArrayList<>();
        PriorityQueue<HeapItem> heap = new PriorityQueue<>((a, b) -> comparator.compare(a.livro, b.livro));

        try {
            for (File run : runs) {
                RunCursor cursor = new RunCursor(run);
                cursores.add(cursor);
                Livro primeiro = cursor.proximo();
                if (primeiro != null) {
                    heap.add(new HeapItem(primeiro, cursor));
                }
            }

            while (!heap.isEmpty()) {
                HeapItem atual = heap.poll();
                resultado.add(atual.livro);

                Livro proximo = atual.cursor.proximo();
                if (proximo != null) {
                    heap.add(new HeapItem(proximo, atual.cursor));
                }
            }
        } finally {
            for (RunCursor cursor : cursores) {
                cursor.close();
            }
        }

        return resultado;
    }

    private static Comparator<Livro> comparatorPorAtributo(String atributo) {
        String attr = (atributo == null ? "titulo" : atributo.trim().toLowerCase(Locale.ROOT));

        if ("ano".equals(attr)) {
            return Comparator
                    .comparingInt(Livro::getAno)
                    .thenComparing(l -> normalizar(l.getTitulo()))
                    .thenComparingInt(Livro::getId);
        }

        if ("autor".equals(attr)) {
            return Comparator
                    .comparing((Livro l) -> normalizar(l.getAutor()))
                    .thenComparing(l -> normalizar(l.getTitulo()))
                    .thenComparingInt(Livro::getId);
        }

        return Comparator
                .comparing((Livro l) -> normalizar(l.getTitulo()))
                .thenComparing((Livro l) -> normalizar(l.getAutor()))
                .thenComparingInt(Livro::getId);
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }

    private static class HeapItem {
        private final Livro livro;
        private final RunCursor cursor;

        private HeapItem(Livro livro, RunCursor cursor) {
            this.livro = livro;
            this.cursor = cursor;
        }
    }

    private static class RunCursor implements Closeable {
        private final DataInputStream dis;
        private int restantes;

        private RunCursor(File runFile) throws IOException {
            this.dis = new DataInputStream(new FileInputStream(runFile));
            this.restantes = dis.readInt();
        }

        private Livro proximo() throws IOException {
            if (restantes <= 0) {
                return null;
            }
            byte[] registro = new byte[Livro.TAMANHO];
            dis.readFully(registro);
            restantes--;
            return Livro.desserializar(new DataInputStream(new ByteArrayInputStream(registro)));
        }

        @Override
        public void close() throws IOException {
            dis.close();
        }
    }
}
