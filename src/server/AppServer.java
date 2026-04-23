package server;

import com.sun.net.httpserver.*;
import controller.*;
import dao.*;
import java.io.*;
import java.net.InetSocketAddress;

public class AppServer {

    private static final int PORT = 8080;
    private static final String DATA_DIR = "data" + File.separator;
    private static final String VIEW_DIR = "view" + File.separator;
    private static final long SERVER_START = System.currentTimeMillis();

    public static void main(String[] args) throws Exception {
        // ---- DAO instances ----
        UsuarioDAO usuarioDAO           = new UsuarioDAO(DATA_DIR + "usuarios.dat");
        LivroDAO livroDAO               = new LivroDAO(DATA_DIR + "livros.dat");
        EmprestimoDAO empDAO            = new EmprestimoDAO(DATA_DIR + "emprestimos.dat");
        EmprestimoItemDAOIndexado empItemDAO = new EmprestimoItemDAOIndexado(DATA_DIR + "emprestimo_itens.dat");
        CupomDAO cupomDAO               = new CupomDAO(DATA_DIR + "cupons.dat");

        // ---- Controller instances ----
        UsuarioController usuCtrl    = new UsuarioController(usuarioDAO);
        LivroController livCtrl      = new LivroController(livroDAO);
        CupomController cupomCtrl    = new CupomController(cupomDAO, empDAO);
        EmprestimoController empCtrl = new EmprestimoController(empDAO, empItemDAO, usuarioDAO, livroDAO, cupomDAO);

        // ---- HTTP Server ----
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Static view files
        server.createContext("/", new StaticHandler(VIEW_DIR));

        // API endpoints
        server.createContext("/api/usuarios",    new UsuarioHandler(usuCtrl));
        server.createContext("/api/livros",      new LivroHandler(livCtrl));
        server.createContext("/api/emprestimos", new EmprestimoHandler(empCtrl));
        server.createContext("/api/cupons",      new CupomHandler(cupomCtrl));

        // Live reload endpoint — retorna o timestamp de início do servidor
        server.createContext("/api/livereload", exchange -> {
            byte[] resp = ("{\"v\":" + SERVER_START + "}").getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("=== Servidor iniciado em http://localhost:" + PORT + " ===");
        System.out.println("Acesse o sistema no navegador.");
    }
}
