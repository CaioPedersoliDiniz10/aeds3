package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.HuffmanTextCompressor;
import util.PatternAlgorithms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FerramentasHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"ok\":false,\"message\":\"Use POST\"}");
            return;
        }

        String path = exchange.getRequestURI().getPath().toLowerCase(Locale.ROOT);
        Map<String, String> body = parseBody(exchange);

        try {
            if (path.endsWith("/pattern")) {
                responderPattern(exchange, body);
                return;
            }
            if (path.endsWith("/compress")) {
                responderCompressao(exchange, body);
                return;
            }
            sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Rota não encontrada\"}");
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, jsonErro(e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, jsonErro("Erro interno: " + e.getMessage()));
        }
    }

    private void responderPattern(HttpExchange exchange, Map<String, String> body) throws IOException {
        String texto = body.getOrDefault("texto", "");
        String padrao = body.getOrDefault("padrao", "");
        String algoritmo = body.getOrDefault("algoritmo", "kmp");

        if (texto.trim().isEmpty() || padrao.trim().isEmpty()) {
            throw new IllegalArgumentException("Informe o texto e o padrão.");
        }

        PatternAlgorithms.Result resultado = PatternAlgorithms.search(algoritmo, texto, padrao);
        StringBuilder matchesJson = new StringBuilder();
        matchesJson.append('[');
        for (int i = 0; i < resultado.matches.size(); i++) {
            if (i > 0) matchesJson.append(',');
            matchesJson.append(resultado.matches.get(i));
        }
        matchesJson.append(']');

        String json = "{"
                + "\"ok\":true,"
                + "\"algorithm\":\"" + escapeJson(resultado.algorithm) + "\","
                + "\"found\":" + resultado.found() + ","
                + "\"comparisons\":" + resultado.comparisons + ","
                + "\"textLength\":" + resultado.textLength + ","
                + "\"patternLength\":" + resultado.patternLength + ","
                + "\"matches\":" + matchesJson
                + "}";
        sendJson(exchange, 200, json);
    }

    private void responderCompressao(HttpExchange exchange, Map<String, String> body) throws IOException {
        String texto = body.getOrDefault("texto", "");
        if (texto.trim().isEmpty()) {
            throw new IllegalArgumentException("Informe um texto para compactar.");
        }

        HuffmanTextCompressor.Result resultado = HuffmanTextCompressor.compressAndRestore(texto);
        String json = "{"
                + "\"ok\":true,"
                + "\"originalBytes\":" + resultado.originalBytes + ","
                + "\"compressedBytes\":" + resultado.compressedBytes + ","
                + "\"ratio\":" + String.format(Locale.ROOT, "%.4f", resultado.ratio) + ","
                + "\"compressedBase64\":\"" + escapeJson(resultado.compressedBase64) + "\","
                + "\"restoredText\":\"" + escapeJson(resultado.restoredText) + "\""
                + "}";
        sendJson(exchange, 200, json);
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] resp = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }

    private static Map<String, String> parseBody(HttpExchange exchange) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream in = exchange.getRequestBody()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
        }
        String raw = baos.toString(StandardCharsets.UTF_8.name());
        Map<String, String> values = new HashMap<>();
        if (raw.isEmpty()) {
            return values;
        }
        for (String part : raw.split("&")) {
            int idx = part.indexOf('=');
            String key = idx >= 0 ? part.substring(0, idx) : part;
            String value = idx >= 0 ? part.substring(idx + 1) : "";
            values.put(decode(key), decode(value));
        }
        return values;
    }

    private static String decode(String value) throws IOException {
        return URLDecoder.decode(value == null ? "" : value, "UTF-8");
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static String jsonErro(String message) {
        return "{\"ok\":false,\"message\":\"" + escapeJson(message == null ? "Erro desconhecido" : message) + "\"}";
    }
}