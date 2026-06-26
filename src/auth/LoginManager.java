package auth;

import dao.UsuarioDAO;
import model.Usuario;

import java.io.*;

public class LoginManager {

    private static final int HEADER_SIZE = 8;
    private static final int EMAIL_SIZE = 120;
    private static final int TOKEN_SIZE = 512;
    private static final int TAMANHO = 1 + EMAIL_SIZE + TOKEN_SIZE;

    private final String caminho;
    private final UsuarioDAO usuarioDAO;

    public LoginManager(String caminho, UsuarioDAO usuarioDAO) {
        this.caminho = caminho;
        this.usuarioDAO = usuarioDAO;
        inicializarArquivo();
    }

    public synchronized void registrar(String email, String senha) throws IOException {
        String emailNormalizado = normalizar(email);
        String senhaNormalizada = normalizar(senha);

        if (emailNormalizado.isEmpty() || senhaNormalizada.isEmpty()) {
            throw new IllegalArgumentException("Informe e-mail e senha.");
        }

        Usuario usuario = usuarioDAO.buscarPorEmail(emailNormalizado);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado. Cadastre o usuário antes do login.");
        }

        if (buscarRegistro(emailNormalizado) != null) {
            throw new IllegalArgumentException("Este e-mail já possui credenciais cadastradas.");
        }

        Registro novo = new Registro();
        novo.email = emailNormalizado;
        novo.token = XorCipher.encrypt(senhaNormalizada);
        novo.lapide = false;

        int[] cabecalho = lerCabecalho();
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            long pos = offsetRegistro(cabecalho[1]);
            raf.seek(pos);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (DataOutputStream dos = new DataOutputStream(baos)) {
                novo.serializar(dos);
            }
            raf.write(baos.toByteArray());
        }
        atualizarCabecalho(cabecalho[1] + 1);
    }

    public synchronized boolean autenticar(String email, String senha) throws IOException {
        String emailNormalizado = normalizar(email);
        String senhaNormalizada = normalizar(senha);

        if (emailNormalizado.isEmpty() || senhaNormalizada.isEmpty()) {
            throw new IllegalArgumentException("Informe e-mail e senha.");
        }

        Registro registro = buscarRegistro(emailNormalizado);
        if (registro == null) {
            throw new IllegalArgumentException("Credenciais não cadastradas.");
        }

        String token = XorCipher.encrypt(senhaNormalizada);
        if (!registro.token.equals(token)) {
            throw new IllegalArgumentException("Senha incorreta.");
        }

        return true;
    }

    private void inicializarArquivo() {
        File f = new File(caminho);
        if (!f.exists()) {
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(f))) {
                dos.writeInt(0);
                dos.writeInt(0);
            } catch (IOException e) {
                throw new RuntimeException("Erro ao inicializar login.dat", e);
            }
        }
    }

    private int[] lerCabecalho() throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(caminho))) {
            int ultimoId = dis.readInt();
            int total = dis.readInt();
            return new int[]{ultimoId, total};
        }
    }

    private void atualizarCabecalho(int total) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "rw")) {
            raf.seek(0);
            raf.writeInt(0);
            raf.writeInt(total);
        }
    }

    private long offsetRegistro(int idx) {
        return HEADER_SIZE + (long) idx * TAMANHO;
    }

    private Registro buscarRegistro(String email) throws IOException {
        String alvo = normalizar(email);
        int[] cabecalho = lerCabecalho();
        try (RandomAccessFile raf = new RandomAccessFile(caminho, "r")) {
            for (int i = 0; i < cabecalho[1]; i++) {
                raf.seek(offsetRegistro(i));
                Registro registro = Registro.desserializar(raf);
                if (!registro.lapide && registro.email.equalsIgnoreCase(alvo)) {
                    return registro;
                }
            }
        }
        return null;
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static void writeFixedString(DataOutputStream dos, String valor, int tamanho) throws IOException {
        byte[] origem = (valor == null ? "" : valor).getBytes("UTF-8");
        byte[] buffer = new byte[tamanho];
        System.arraycopy(origem, 0, buffer, 0, Math.min(origem.length, tamanho));
        dos.write(buffer);
    }

    private static final class Registro {
        private boolean lapide;
        private String email;
        private String token;

        private void serializar(DataOutputStream dos) throws IOException {
            dos.writeBoolean(lapide);
            writeFixedString(dos, email, EMAIL_SIZE);
            writeFixedString(dos, token, TOKEN_SIZE);
        }

        private static Registro desserializar(RandomAccessFile raf) throws IOException {
            Registro registro = new Registro();
            registro.lapide = raf.readBoolean();

            byte[] emailBytes = new byte[EMAIL_SIZE];
            raf.readFully(emailBytes);
            registro.email = new String(emailBytes, "UTF-8").trim();

            byte[] tokenBytes = new byte[TOKEN_SIZE];
            raf.readFully(tokenBytes);
            registro.token = new String(tokenBytes, "UTF-8").trim();
            return registro;
        }
    }
}