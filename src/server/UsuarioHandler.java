package server;

import com.sun.net.httpserver.*;
import controller.UsuarioController;
import model.Usuario;

import java.io.*;
import java.net.URI;
import java.util.*;

/**
 * Rotas REST para /api/usuarios
 *   GET    /api/usuarios         -> listar todos
 *   GET    /api/usuarios?id=X    -> buscar por ID
 *   POST   /api/usuarios         -> criar  (body: nome=...&email=...)
 *   PUT    /api/usuarios?id=X    -> atualizar (body: nome=...&email=...)
 *   DELETE /api/usuarios?id=X    -> excluir (lápide)
 */
public class UsuarioHandler implements HttpHandler {

    private final UsuarioController ctrl;

    public UsuarioHandler(UsuarioController ctrl) {
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
                    Usuario u = ctrl.buscar(id);
                    sendJson(ex, 200, u.toString());
                } else {
                    List<Usuario> lista = ctrl.listar();
                    sendJson(ex, 200, toJsonArray(lista));
                }

            } else if ("POST".equals(method)) {
                Map<String, String> body = parseBody(ex);
                Usuario u = ctrl.cadastrar(body.get("nome"), body.get("email"));
                sendJson(ex, 201, u.toString());

            } else if ("PUT".equals(method)) {
                Map<String, String> params = queryParams(ex.getRequestURI());
                int id = Integer.parseInt(params.getOrDefault("id", "0"));
                Map<String, String> body = parseBody(ex);
                Usuario u = ctrl.atualizar(id, body.get("nome"), body.get("email"));
                sendJson(ex, 200, u.toString());

            } else if ("DELETE".equals(method)) {
                Map<String, String> params = queryParams(ex.getRequestURI());
                int id = Integer.parseInt(params.getOrDefault("id", "0"));
                ctrl.excluir(id);
                sendJson(ex, 200, "{\"mensagem\":\"Usuário excluído com sucesso.\"}");

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
}
