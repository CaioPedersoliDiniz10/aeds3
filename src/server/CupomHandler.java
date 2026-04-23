package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import controller.CupomController;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Cupom;

/**
 * Rotas REST para /api/cupons
 *   GET    /api/cupons                     -> listar todos
 *   GET    /api/cupons?id=X                -> buscar por ID do cupom
 *   GET    /api/cupons?idEmprestimo=Y      -> buscar por emprestimo
 *   POST   /api/cupons                     -> criar (idEmprestimo, tipo, descricao, valor)
 *   PUT    /api/cupons?id=X                -> atualizar (tipo, descricao, valor)
 *   DELETE /api/cupons?id=X                -> excluir (lapide)
 */
public class CupomHandler implements HttpHandler {

    private final CupomController ctrl;

    public CupomHandler(CupomController ctrl) {
        this.ctrl = ctrl;
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

            if ("GET".equals(method)) {
                Map<String, String> params = queryParams(ex.getRequestURI());

                if (params.containsKey("id")) {
                    int id = Integer.parseInt(params.get("id"));
                    sendJson(ex, 200, ctrl.buscar(id).toString());
                } else if (params.containsKey("idEmprestimo")) {
                    int idEmprestimo = Integer.parseInt(params.get("idEmprestimo"));
                    sendJson(ex, 200, ctrl.buscarPorEmprestimo(idEmprestimo).toString());
                } else {
                    List<Cupom> lista = ctrl.listar();
                    sendJson(ex, 200, toJsonArray(lista));
                }

            } else if ("POST".equals(method)) {
                Map<String, String> body = parseBody(ex);
                int idEmprestimo = Integer.parseInt(body.getOrDefault("idEmprestimo", "0"));
                double valor = Double.parseDouble(body.getOrDefault("valor", "0"));
                Cupom c = ctrl.criar(idEmprestimo, body.get("tipo"), body.get("descricao"), valor);
                sendJson(ex, 201, c.toString());

            } else if ("PUT".equals(method)) {
                Map<String, String> params = queryParams(ex.getRequestURI());
                int id = Integer.parseInt(params.getOrDefault("id", "0"));
                Map<String, String> body = parseBody(ex);
                Double valor = null;
                if (body.containsKey("valor") && !body.get("valor").trim().isEmpty()) {
                    valor = Double.parseDouble(body.get("valor"));
                }
                Cupom c = ctrl.atualizar(id, body.get("tipo"), body.get("descricao"), valor);
                sendJson(ex, 200, c.toString());

            } else if ("DELETE".equals(method)) {
                Map<String, String> params = queryParams(ex.getRequestURI());
                int id = Integer.parseInt(params.getOrDefault("id", "0"));
                ctrl.excluir(id);
                sendJson(ex, 200, "{\"mensagem\":\"Cupom excluido com sucesso.\"}");

            } else {
                sendJson(ex, 405, "{\"erro\":\"Metodo nao permitido.\"}");
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
        if (query == null) {
            return map;
        }

        for (String p : query.split("&")) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], decode(kv[1]));
            }
        }
        return map;
    }

    private Map<String, String> parseBody(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), "UTF-8");
        Map<String, String> map = new HashMap<>();

        if (body == null || body.isEmpty()) {
            return map;
        }

        for (String p : body.split("&")) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], decode(kv[1]));
            }
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
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private <T> String toJsonArray(List<T> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(list.get(i).toString());
        }
        sb.append("]");
        return sb.toString();
    }
}
