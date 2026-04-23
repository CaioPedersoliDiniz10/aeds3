package server;

import com.sun.net.httpserver.*;
import controller.EmprestimoController;
import java.io.*;
import java.net.URI;
import java.util.*;
import model.Cupom;
import model.Emprestimo;

/**
 * Rotas REST para /api/emprestimos
 *   GET    /api/emprestimos                   -> listar todos
 *   GET    /api/emprestimos?id=X              -> buscar por ID
 *   POST   /api/emprestimos                   -> criar empréstimo
 *   PUT    /api/emprestimos?id=X              -> atualizar datas
 *   DELETE /api/emprestimos?id=X              -> excluir (lápide)
 *
 * Itens do relacionamento 1:N (emprestimo -> itens):
 *   GET    /api/emprestimos/itens             -> listar todos os itens
 *   GET    /api/emprestimos/itens?id=X        -> listar itens de um empréstimo
 *   GET    /api/emprestimos/itens?itemId=Y    -> buscar item por ID
 *   POST   /api/emprestimos/itens?id=X        -> adicionar item ao empréstimo X
 *   PUT    /api/emprestimos/itens?itemId=Y    -> atualizar item
 *   DELETE /api/emprestimos/itens?itemId=Y    -> remover item
 *
 * Consulta usando índice hash extensível:
 *   GET    /api/emprestimos/indice?id=X
 *   GET    /api/emprestimos/indice
 *
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
            boolean isItens = path.endsWith("/itens");
            boolean isIndice = path.endsWith("/indice");

            if (isIndice) {
                if (!"GET".equals(method)) {
                    sendJson(ex, 405, "{\"erro\":\"Método não permitido.\"}");
                    return;
                }

                Map<String, String> params = queryParams(ex.getRequestURI());
                String indiceJson = toJsonObject(ctrl.obterEstatisticasIndiceItens());

                if (params.containsKey("id")) {
                    int idEmprestimo = Integer.parseInt(params.get("id"));
                    String itensJson = toJsonArray(ctrl.listarItens(idEmprestimo));
                    sendJson(ex, 200,
                            "{\"metodo\":\"hash_extensivel\",\"idEmprestimo\":" + idEmprestimo
                                    + ",\"indice\":" + indiceJson + ",\"itens\":" + itensJson + "}");
                } else {
                    sendJson(ex, 200,
                            "{\"metodo\":\"hash_extensivel\",\"indice\":" + indiceJson + "}");
                }
                return;
            }

            if (isItens) {
                Map<String, String> params = queryParams(ex.getRequestURI());

                if ("GET".equals(method)) {
                    if (params.containsKey("itemId")) {
                        int itemId = Integer.parseInt(params.get("itemId"));
                        sendJson(ex, 200, ctrl.buscarItem(itemId).toString());
                    } else if (params.containsKey("id")) {
                        int idEmprestimo = Integer.parseInt(params.get("id"));
                        sendJson(ex, 200, toJsonArray(ctrl.listarItens(idEmprestimo)));
                    } else {
                        sendJson(ex, 200, toJsonArray(ctrl.listarTodosItens()));
                    }

                } else if ("POST".equals(method)) {
                    Map<String, String> b = parseBody(ex);
                    int id = Integer.parseInt(params.getOrDefault("id", b.getOrDefault("idEmprestimo", "0")));
                    int idLivro = Integer.parseInt(b.getOrDefault("idLivro", "0"));
                    int quantidade = Integer.parseInt(b.getOrDefault("quantidade", "1"));
                    model.EmprestimoItem item = ctrl.adicionarItem(id, idLivro, quantidade);
                    sendJson(ex, 201, item.toString());

                } else if ("PUT".equals(method)) {
                    int itemId = Integer.parseInt(params.getOrDefault("itemId", "0"));
                    Map<String, String> b = parseBody(ex);
                    Integer idEmprestimo = parseOptionalInteger(b.get("idEmprestimo"));
                    Integer idLivro = parseOptionalInteger(b.get("idLivro"));
                    Integer quantidade = parseOptionalInteger(b.get("quantidade"));
                    model.EmprestimoItem item = ctrl.atualizarItem(itemId, idEmprestimo, idLivro, quantidade);
                    sendJson(ex, 200, item.toString());

                } else if ("DELETE".equals(method)) {
                    int itemId = Integer.parseInt(params.getOrDefault("itemId", "0"));
                    ctrl.removerItem(itemId);
                    sendJson(ex, 200, "{\"mensagem\":\"Item removido com sucesso.\"}");

                } else {
                    sendJson(ex, 405, "{\"erro\":\"Método não permitido.\"}");
                }
                return;
            }

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
                Emprestimo e = ctrl.criar(idUsuario, b.get("dataEmprestimo"), b.get("dataDevolucao"));
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

    private Integer parseOptionalInteger(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        if (limpo.isEmpty()) return null;
        return Integer.parseInt(limpo);
    }
}
