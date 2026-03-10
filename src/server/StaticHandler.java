package server;

import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.file.*;

/**
 * Serve arquivos estáticos da pasta view/.
 */
public class StaticHandler implements HttpHandler {

    private final String viewDir;

    public StaticHandler(String viewDir) {
        this.viewDir = viewDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        // Previne path traversal
        String normalized = path.replaceAll("\\.\\.", "").replaceAll("//+", "/");
        File file = new File(viewDir + normalized.substring(1));

        if (!file.exists() || !file.isFile()) {
            byte[] msg = "404 Not Found".getBytes();
            exchange.sendResponseHeaders(404, msg.length);
            exchange.getResponseBody().write(msg);
            exchange.getResponseBody().close();
            return;
        }

        String contentType = "text/html; charset=UTF-8";
        if (normalized.endsWith(".css")) contentType = "text/css; charset=UTF-8";
        else if (normalized.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";

        byte[] bytes = Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }
}
