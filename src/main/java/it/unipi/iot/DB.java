package it.unipi.iot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class DB {
    private static final String JDBC_URL = "jdbc:mysql://%s:%d/%s";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "iot22-23";
    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String DATABASE_NAME = "iot";

    private static Connection db = null;

    private DB() {
        // Costruttore privato per evitare l'istanziazione diretta
    }

    public static Connection getDb() {
        try {
            if (db == null || db.isClosed()) {
                String jdbcUrl = String.format(JDBC_URL, HOST, PORT, DATABASE_NAME);
                Properties properties = new Properties();
                properties.put("user", USERNAME);
                properties.put("password", PASSWORD);
                properties.put("zeroDateTimeBehavior", "CONVERT_TO_NULL");
                properties.put("serverTimeZone", "CET");
    
                db = DriverManager.getConnection(jdbcUrl, properties);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    
        return db;
    }

    public static boolean changeStatus(String actuatorIp, String actuatorType, String change)
    {
        System.out.println("SONO NELLA CLASSE DB A FARE LA CHANGE STATUS\n");
        try (Connection connection = DB.getDb())
        {
            System.out.println("CONNESSIONE STABILITA\n");
            try (PreparedStatement ps = connection.prepareStatement("REPLACE INTO iot.actuator(ip, type, status) VALUES(?,?,?);"))
            {
                // 1 = Inet
                ps.setString(1, actuatorIp); //substring(1)
                // 2 = Tipo attuatore
                ps.setString(2, "med");
                ps.setString(3, "ON");
                System.out.println("PREPARED STATEMENT CON INDIRIZZO: "+ actuatorIp +" TIPO ATTUATORE: MED");
                System.out.println("CERCO DI ESEGUIRE LA UPDATE\n");
                // Tryna execute UPDATE
                ps.executeUpdate();
                // Ritorna il numero di righe coinvolte
                int success = ps.getUpdateCount();
                if(success>0)
                {
                    System.out.println("STATO CAMBIATO\n");
                    System.out.println("RIGHE COINVOLTE: "+success+"\n");
                    // response = new Response(CoAP.ResponseCode.CREATED);
                    return true;
                }
                else
                {
                    System.out.println("ERRORE NELLA UPDATE UPDATE\n SUCCESS="+success);
                    // response = new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                    return false;
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            // response = new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR); // handle SQLException
        }
        // QUA NON CI DEVE ARRIVARE
        System.out.println("CE STATO UN ERRORE NELLA UPDATE DELLO STATO\n");
        return false;
    }
    
}
