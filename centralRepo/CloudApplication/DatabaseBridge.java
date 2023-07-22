package med.unipi.it;

import java.sql.*;
// import java.time.Instant;
import java.util.HashMap;

public class DatabaseBridge {
	
	private static final String url = "jdbc:mysql://localhost:3306/smartsuit";
    private static final String user = "root";
    private static final String pw = "ubuntu";
    private static final String name_table = "actuators";
    
    public static int updateActuators(String address, String actuatorType, String status) throws SQLException 
    {
    	// Start connection with DB 
        Connection connection = DriverManager.getConnection(url, user, pw);
        System.out.println("CONNESSIONE STABILITA\n");
        
        // Istanziate an obj PreparedStatement needed to compute UPDATE 
        PreparedStatement ps = connection.prepareStatement("REPLACE INTO" + name_table +" (ip, actuator_type, status) VALUES(?,?,?);");
        ps.setString(1, address.substring(1)); //substring(1)
        ps.setString(2, actuatorType);
        ps.setString(3, status);
        
        System.out.println("PREPARED STATEMENT CON\n INDIRIZZO: "+address.substring(1)+" TIPO ATTUATORE: "+actuatorType.toString()+" STATUS: "+status.toString());
        System.out.println("CERCO DI ESEGUIRE LA UPDATE\n");
        
        // Tryna execute UPDATE
        ps.executeUpdate();
        System.out.println("UPDATE ESEGUITA\n");
        
        // Return number of raw coinvolte
        return ps.getUpdateCount();
    }
    
    public static HashMap<String, String> retrieveActuator(String actuatorType) throws SQLException 
    {
    	// Istantiate an Hash Map to collect data retrieved
        HashMap<String, String> result = new HashMap<String, String>();
        
        // Start connection
        Connection connection = DriverManager.getConnection(url, user, pw);
        
        // Istantiate a PreparedStatement obj
        PreparedStatement ps = connection.prepareStatement("SELECT ip, status FROM " + name_table + " WHERE actuator_type = ?");
        ps.setString(1, actuatorType);
        
        // Data retrieved will be stored into ResultSet obj
        ResultSet rs = ps.executeQuery();
        if(!rs.next())
        {
        	// No res -> return empty hash map
            return result; 
        }
        else 
        {
        	// Some results -> populate hash map
        	result.put("status", rs.getString("status"));
            result.put("ip", rs.getString("ip"));
            rs.close();
            return result;
        }
    }
    
    public static int insertData(Long value, String type) throws SQLException 
    {
    	// Start connection with the DB
        Connection connection = DriverManager.getConnection(url, user, pw);
        
        // Istanziate a PreparedStatement
        PreparedStatement ps = connection.prepareStatement("INSERT INTO data (value, sensor) VALUES(?,?);");

        ps.setString(1, String.valueOf(value)); //substring(1)
        ps.setString(2, type);
        //ps.setString(3, String.valueOf(Instant.now()));
        ps.execute();
        //System.out.println(ps.getUpdateCount());
        return ps.getUpdateCount();
    }
    
    public static HashMap<String, Integer> retrieveData() throws SQLException{
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        Connection connection = DriverManager.getConnection(url, user, pw);
        PreparedStatement ps = connection.prepareStatement("SELECT sensor, value  FROM data WHERE (sensor, timestamp) IN (SELECT sensor, MAX(timestamp) FROM data GROUP BY sensor)");
        ResultSet rs = ps.executeQuery();
        while(rs.next()){
               result.put(rs.getString("sensor"), rs.getInt("value"));
        }
        rs.close();
        //System.out.println(result);
        return result;
    }
    
    public static void resetActuators() throws SQLException{
        Connection connection = DriverManager.getConnection(url, user, pw);
        PreparedStatement ps = connection.prepareStatement("DELETE from " + name_table);
        ps.executeUpdate();
    }
    

}
