package server;

import com.sun.net.httpserver.*;
import controller.*;
import dao.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;

public class AppServer {

    private static final int PORT = 8080;
    private static final String DATA_DIR = "data" + File.separator;
    private static final String VIEW_DIR = "view" + File.separator;

    public static void main(String[] args) throws Exception {
        // ---- DAO instances ----
        UsuarioDAO usuarioDAO    = new UsuarioDAO(DATA_DIR + "usuarios.dat");
        LivroDAO livroDAO        = new LivroDAO(DATA_DIR + "livros.dat");
        EmprestimoDAO empDAO     = new EmprestimoDAO(DATA_DIR + "emprestimos.dat");
        CupomDAO cupomDAO        = new CupomDAO(DATA_DIR + "cupons.dat");

        // ---- Controller instances ----
        UsuarioController usuCtrl    = new UsuarioController(usuarioDAO);
        LivroController livCtrl      = new LivroController(livroDAO);
        EmprestimoController empCtrl = new EmprestimoController(empDAO, usuarioDAO, livroDAO, cupomDAO);

        // ---- HTTP Server ----
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Static view files
        server.createContext("/", new StaticHandler(VIEW_DIR));

        // API endpoints
        server.createContext("/api/usuarios",    new UsuarioHandler(usuCtrl));
        server.createContext("/api/livros",      new LivroHandler(livCtrl));
        server.createContext("/api/emprestimos", new EmprestimoHandler(empCtrl));

        server.setExecutor(null);
        server.start();
        System.out.println("=== Servidor iniciado em http://localhost:" + PORT + " ===");
        System.out.println("Acesse o sistema no navegador.");
    }
}
