package server;

import com.sun.net.httpserver.*;
import controller.EmprestimoController;
import model.Emprestimo;
import model.Cupom;

import java.io.*;
import java.net.URI;
import java.util.*;

/**
 * Rotas REST para /api/emprestimos
 *   GET    /api/emprestimos                   -> listar todos
 *   GET    /api/emprestimos?id=X              -> buscar por ID
 *   POST   /api/emprestimos                   -> criar empréstimo
 *   PUT    /api/emprestimos?id=X              -> atualizar datas
 *   DELETE /api/emprestimos?id=X              -> excluir (lápide)
 *   POST   /api/emprestimos/cupom?id=X        -> associar cupom ao empréstimo X
 *   GET    /api/emprestimos/cupom?id=X        -> buscar cupom do empréstimo X
 */
public class EmprestimoHandler implements HttpHandler {

    private final EmprestimoController ctrl;

    public EmprestimoHandler(EmprestimoController ctrl) {
        this.ctrl = ctrl;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath(); // e.g. /api/emprestimos or /api/emprestimos/cupom

        try {
            if ("OPTIONS".equals(method)) { ex.sendResponseHeaders(204, -1); return; }

            boolean isCupom = path.endsWith("/cupom");

            if (isCupom) {
                Map<String, String> params = queryParams(ex.getRequestURI());
                int id = Integer.parseInt(params.getOrDefault("id", "0"));

                if ("GET".equals(method)) {
                    Cupom c = ctrl.buscarCupomDoEmprestimo(id);
                    sendJson(ex, 200, c.toString());

                } else if ("POST".equals(method)) {
                    Map<String, String> b = parseBody(ex);
                    double valor = b.containsKey("valor") ? Double.parseDouble(b.get("valor")) : 0;
                    Cupom c = ctrl.associarCupom(id, b.get("tipo"), b.get("descricao"), valor);
                    sendJson(ex, 201, c.toString());

                } else {
                    sendJson(ex, 405, "{\"erro\":\"Método não permitido.\"}");
                }
                return;
            }

            if ("GET".equals(method)) {
                Map<String, String> params = queryParams(ex.getRequestURI());
                if (params.containsKey("id")) {
                    sendJson(ex, 200, ctrl.buscar(Integer.parseInt(params.get("id"))).toString());
                } else {
                    sendJson(ex, 200, toJsonArray(ctrl.listar()));
                }

            } else if ("POST".equals(method)) {
                Map<String, String> b = parseBody(ex);
                int idUsuario = Integer.parseInt(b.getOrDefault("idUsuario", "0"));
                int idLivro   = Integer.parseInt(b.getOrDefault("idLivro", "0"));
                Emprestimo e = ctrl.criar(idUsuario, idLivro, b.get("dataEmprestimo"), b.get("dataDevolucao"));
                sendJson(ex, 201, e.toString());

            } else if ("PUT".equals(method)) {
                Map<String, String> params = queryParams(ex.getRequestURI());
                int id = Integer.parseInt(params.getOrDefault("id", "0"));
                Map<String, String> b = parseBody(ex);
                Emprestimo e = ctrl.atualizar(id, b.get("dataEmprestimo"), b.get("dataDevolucao"));
                sendJson(ex, 200, e.toString());

            } else if ("DELETE".equals(method)) {
                Map<String, String> params = queryParams(ex.getRequestURI());
                ctrl.excluir(Integer.parseInt(params.getOrDefault("id", "0")));
                sendJson(ex, 200, "{\"mensagem\":\"Empréstimo excluído com sucesso.\"}");

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
