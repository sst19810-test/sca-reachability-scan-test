package demo.sast.cross;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class AccountRepository {
    // CWE-798: hardcoded database credentials
    private static final String URL = "jdbc:postgresql://db.internal:5432/accounts";
    private static final String USER = "accounts_rw";
    private static final String PASSWORD = "Acc0unts#2024";

    // CWE-89: owner arrives from AdminController via AdminService
    public List<String> findByOwner(String owner) throws SQLException {
        String sql = "SELECT iban FROM accounts WHERE owner = '" + owner + "' ORDER BY id";
        List<String> rows = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) rows.add(rs.getString(1));
        }
        return rows;
    }
}
