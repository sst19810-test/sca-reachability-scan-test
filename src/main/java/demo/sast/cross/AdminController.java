package demo.sast.cross;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * INTENTIONALLY VULNERABLE SAST fixture. Request data enters here, passes through
 * AdminService, and reaches its sink in a third source file. Compile-only.
 */
@WebServlet("/cross/*")
public class AdminController extends HttpServlet {
    private final AdminService service = new AdminService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (String.valueOf(req.getPathInfo())) {
                case "/accounts" -> {
                    resp.setContentType("text/html");
                    // CWE-79: AdminService echoes the owner parameter back unescaped
                    resp.getWriter().println(service.accountsPage(req.getParameter("owner")));
                }
                case "/backup" -> resp.getWriter().println(service.backup(req.getParameter("target")));
                case "/preview" -> resp.getWriter().println(service.preview(req.getParameter("url")));
                case "/prefs" -> resp.getWriter().println(service.preferences(cookie(req, "prefs")));
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (String.valueOf(req.getPathInfo())) {
                case "/notes" -> service.saveNote(req.getParameter("name"), req.getParameter("body"));
                case "/import" -> resp.getWriter().println(service.importCatalog(req.getInputStream()));
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private static String cookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) {
            if (c.getName().equals(name)) return c.getValue();
        }
        return null;
    }
}
