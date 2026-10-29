package demo.sast.single;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Hashtable;
import java.util.Random;
import java.util.logging.Logger;

/**
 * INTENTIONALLY VULNERABLE SAST fixture: every source and sink lives in this one file.
 * Compile-only; it is never deployed and never invoked by Main or the tests.
 */
@WebServlet("/single/*")
public class VulnerableServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(VulnerableServlet.class.getName());

    // CWE-798: hardcoded database credentials
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/app";
    private static final String DB_USER = "app_admin";
    private static final String DB_PASSWORD = "Sup3rS3cret!";

    // CWE-321: hardcoded cryptographic key
    private static final byte[] COOKIE_KEY = "k3yB1t5!".getBytes(StandardCharsets.UTF_8);

    // Unsalted MD5 of "admin123"
    private static final String ADMIN_PASSWORD_MD5 = "0192023a7bbd73250516f069df18b500";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            switch (String.valueOf(req.getPathInfo())) {
                case "/user" -> findUser(req, resp);
                case "/greet" -> greet(req, resp);
                case "/ping" -> ping(req, resp);
                case "/download" -> download(req, resp);
                case "/fetch" -> fetch(req, resp);
                case "/redirect" -> redirect(req, resp);
                case "/directory" -> directory(req, resp);
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            // CWE-209: stack trace returned to the client
            e.printStackTrace(resp.getWriter());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            switch (String.valueOf(req.getPathInfo())) {
                case "/login" -> login(req, resp);
                case "/import" -> importXml(req, resp);
                case "/restore" -> restoreSession(req, resp);
                case "/event" -> readEvent(req, resp);
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            // CWE-209: stack trace returned to the client
            e.printStackTrace(resp.getWriter());
        }
    }

    // CWE-89: SQL injection through string concatenation
    private void findUser(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String name = req.getParameter("name");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT email FROM users WHERE name = '" + name + "'")) {
            while (rs.next()) {
                resp.getWriter().println(rs.getString("email"));
            }
        }
    }

    // CWE-79: reflected cross-site scripting
    private void greet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        resp.getWriter().println("<h1>Hello, " + req.getParameter("name") + "</h1>");
    }

    // CWE-78: OS command injection through a shell
    private void ping(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String host = req.getParameter("host");
        Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ping -c 1 " + host});
        process.getInputStream().transferTo(resp.getOutputStream());
    }

    // CWE-22: path traversal ("../" escapes the upload directory)
    private void download(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        File file = new File("/var/app/uploads", req.getParameter("file"));
        resp.getOutputStream().write(Files.readAllBytes(file.toPath()));
    }

    // CWE-918: server-side request forgery
    private void fetch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (InputStream in = new URL(req.getParameter("url")).openStream()) {
            in.transferTo(resp.getOutputStream());
        }
    }

    // CWE-601: open redirect
    private void redirect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getParameter("next"));
    }

    // CWE-90: LDAP injection
    private void directory(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://localhost:389");
        DirContext ctx = new InitialDirContext(env);
        try {
            String filter = "(uid=" + req.getParameter("uid") + ")";
            resp.getWriter().println(ctx.search("ou=people,dc=example,dc=com", filter, new SearchControls()).hasMore());
        } finally {
            ctx.close();
        }
    }

    // CWE-117: log injection; CWE-328: unsalted MD5 password hash;
    // CWE-330: predictable session token; CWE-614 / CWE-1004: cookie lacks Secure and HttpOnly
    private void login(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String user = req.getParameter("user");
        LOG.info("Login attempt for " + user);
        byte[] digest = MessageDigest.getInstance("MD5")
                .digest(req.getParameter("password").getBytes(StandardCharsets.UTF_8));
        if (!"admin".equals(user) || !ADMIN_PASSWORD_MD5.equals(HexFormat.of().formatHex(digest))) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String token = Long.toHexString(new Random().nextLong());
        resp.addCookie(new Cookie("session", token));
        resp.addCookie(new Cookie("remember", encrypt(user)));
    }

    // CWE-327: broken cipher (DES in ECB mode), keyed with the hardcoded COOKIE_KEY
    private static String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(COOKIE_KEY, "DES"));
        return Base64.getUrlEncoder().encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    // CWE-611: XML external entities (parser left at insecure defaults)
    private void importXml(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(req.getInputStream());
        resp.getWriter().println(doc.getDocumentElement().getTextContent());
    }

    // CWE-502: Java deserialization of the raw request body
    private void restoreSession(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(req.getInputStream())) {
            req.getSession().setAttribute("state", in.readObject());
        }
    }

    // CWE-502: Jackson default typing lets the payload name any class to instantiate
    @SuppressWarnings("deprecation")
    private void readEvent(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping();
        Object event = mapper.readValue(req.getInputStream(), Object.class);
        resp.getWriter().println(event.getClass().getName());
    }
}
