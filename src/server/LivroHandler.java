package server;

import com.sun.net.httpserver.*;
import controller.LivroController;
import java.io.*;
import java.net.URI;
import java.util.*;
import model.Livro;

/**
 * Rotas REST para /api/livros
 *   GET    /api/livros         -> listar todos
 *   GET    /api/livros?id=X    -> buscar por ID
 *   POST   /api/livros         -> criar  (body: titulo=...&autor=...&ano=...)
 *   PUT    /api/livros?id=X    -> atualizar
 *   DELETE /api/livros?id=X    -> excluir (lápide)
 *
 * Rotas complementares:
 *   GET    /api/livros/ordenacao-externa?atributo=titulo|autor|ano
 *   GET    /api/livros/bplus                 -> estatisticas do indice B+
 *   GET    /api/livros/bplus?id=X            -> consulta direta por indice B+
 *   GET    /api/livros/bplus?reconstruir=true -> reconstrucao + estatisticas
 */
public class LivroHandler implements HttpHandler {

    private final LivroController ctrl;

    public LivroHandler(LivroController ctrl) {
        this.ctrl = ctrl;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        boolean isOrdenacaoExterna = path.endsWith("/ordenacao-externa");
        boolean isBPlus = path.endsWith("/bplus");

        try {
            if ("OPTIONS".equals(method)) { ex.sendResponseHeaders(204, -1); return; }

            if (isOrdenacaoExterna) {
                if (!"GET".equals(method)) {
                    sendJson(ex, 405, "{\"erro\":\"Método não permitido.\"}");
                    return;
                }

                Map<String, String> params = queryParams(ex.getRequestURI());
                String atributo = params.getOrDefault("atributo", "titulo");
                sendJson(ex, 200, toJsonArray(ctrl.listarOrdenadosExternamente(atributo)));
                return;
            }

            if (isBPlus) {
                if (!"GET".equals(method)) {
                    sendJson(ex, 405, "{\"erro\":\"Método não permitido.\"}");
                    return;
                }

                Map<String, String> params = queryParams(ex.getRequestURI());

                if ("true".equalsIgnoreCase(params.getOrDefault("reconstruir", "false"))) {
                    ctrl.reconstruirIndiceBPlus();
                }

                if (params.containsKey("id")) {
                    int id = Integer.parseInt(params.get("id"));
                    Livro l = ctrl.buscarComIndiceBPlus(id);
                    sendJson(ex, 200, "{\"metodo\":\"arvore_b_plus\",\"livro\":" + l.toString() + "}");
                } else {
                    sendJson(ex, 200,
                            "{\"metodo\":\"arvore_b_plus\",\"estatisticas\":"
                                    + toJsonObject(ctrl.obterEstatisticasBPlus()) + "}");
                }
                return;
            }

            if ("GET".equals(method)) {
                Map<String, String> params = queryParams(ex.getRequestURI());
                if (params.containsKey("id")) {
                    sendJson(ex, 200, ctrl.buscarComIndiceBPlus(Integer.parseInt(params.get("id"))).toString());
                } else if ("externa".equalsIgnoreCase(params.getOrDefault("ordenacao", ""))) {
                    String atributo = params.getOrDefault("atributo", "titulo");
                    sendJson(ex, 200, toJsonArray(ctrl.listarOrdenadosExternamente(atributo)));
                } else {
                    sendJson(ex, 200, toJsonArray(ctrl.listar()));
                }

            } else if ("POST".equals(method)) {
                Map<String, String> b = parseBody(ex);
                int ano = b.containsKey("ano") ? Integer.parseInt(b.get("ano")) : 0;
                Livro l = ctrl.cadastrar(b.get("titulo"), b.get("autor"), ano);
                sendJson(ex, 201, l.toString());

            } else if ("PUT".equals(method)) {
                Map<String, String> params = queryParams(ex.getRequestURI());
                int id = Integer.parseInt(params.getOrDefault("id", "0"));
                Map<String, String> b = parseBody(ex);
                Integer ano = b.containsKey("ano") && !b.get("ano").isEmpty() ? Integer.parseInt(b.get("ano")) : null;
                Livro l = ctrl.atualizar(id, b.get("titulo"), b.get("autor"), ano);
                sendJson(ex, 200, l.toString());

            } else if ("DELETE".equals(method)) {
                Map<String, String> params = queryParams(ex.getRequestURI());
                ctrl.excluir(Integer.parseInt(params.getOrDefault("id", "0")));
                sendJson(ex, 200, "{\"mensagem\":\"Livro excluído com sucesso.\"}");

            } else {
                sendJson(ex, 405, "{\"erro\":\"Método não permitido.\"}");
            }

        } catch (IllegalArgumentException e) {
            sendJson(ex, 400, "{\"erro\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            sendJson(ex, 500, "{\"erro\":\"Erro interno: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
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

    private Map<String, String> parseBody(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), "UTF-8");
        Map<String, String> map = new HashMap<>();
        for (String p : body.split("&")) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2) map.put(kv[0], decode(kv[1]));
        }
        return map;
    }

    private String decode(String s) {
        try { return java.net.URLDecoder.decode(s, "UTF-8"); } catch (Exception e) { return s; }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private <T> String toJsonArray(List<T> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i).toString());
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJsonObject(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(escapeJson(e.getKey())).append("\":");
            Object valor = e.getValue();
            if (valor == null) {
                sb.append("null");
            } else if (valor instanceof Number || valor instanceof Boolean) {
                sb.append(valor.toString());
            } else {
                sb.append("\"").append(escapeJson(valor.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
