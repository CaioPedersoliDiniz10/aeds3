package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import controller.EmprestimoController;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Emprestimo;

/**
 * Rotas REST para /api/livros/emprestimos
 *   GET /api/livros/emprestimos?idLivro=X   -> lista empréstimos que contêm o livro X
 */
public class LivroEmprestimosHandler implements HttpHandler {

    private final EmprestimoController empCtrl;

    public LivroEmprestimosHandler(EmprestimoController empCtrl) {
        this.empCtrl = empCtrl;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);
        String method = ex.getRequestMethod();

        try {
            if ("OPTIONS".equals(method)) {
                ex.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equals(method)) {
                sendJson(ex, 405, "{\"erro\":\"Método não permitido.\"}");
                return;
            }

            Map<String, String> params = queryParams(ex.getRequestURI());
            int idLivro = Integer.parseInt(params.getOrDefault("idLivro", "0"));
            List<Emprestimo> lista = empCtrl.listarEmprestimosQueContemLivro(idLivro);
            sendJson(ex, 200, toJsonArray(lista));

        } catch (IllegalArgumentException e) {
            sendJson(ex, 400, "{\"erro\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            sendJson(ex, 500, "{\"erro\":\"Erro interno: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> queryParams(URI uri) {
        Map<String, String> map = new HashMap<>();
        String query = uri.getQuery();
        if (query == null) return map;
        for (String p : query.split("&")) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2) map.put(kv[0], decode(kv[1]));
        }
        return map;
    }

    private String decode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String toJsonArray(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toString());
            if (i < list.size() - 1) sb.append(',');
        }
        return sb.append(']').toString();
    }
}
