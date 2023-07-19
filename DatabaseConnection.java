import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    // Variabili di connessione al database
    private static final String DB_URL = "jdbc:mysql://localhost:3306/iot";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "iot23";

    // Implementazione del singleton
    private static DatabaseConnection instance = null;

    private DatabaseConnection() {
        // Costruttore privato per evitare l'istanziazione diretta
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public String getUsername() {
        return DB_USERNAME;
    }

    public String getPassword() {
        return DB_PASSWORD;
    }

    // Altri metodi per la gestione della connessione al database...
}
