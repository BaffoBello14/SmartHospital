package it.unipi.iot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Response;
import org.json.simple.JSONObject;

public class RemoteControlApplication implements Runnable {

    private static final float DNG_TEMP_TH = 38.9f;
    private static final float CTR_TEMP_TH = 36.9f;

    private static final float DNG_OX_TH = 49.9f;
    private static final float CTR_OX_TH = 59.9f;

    private static final int DNG_HB_TH = 70;
    private static final int CTR_HB_TH = 80;

    private static HashMap<String, Boolean[]> alreadyActivated = new HashMap<>();

    private static final String JDBC_URL = "jdbc:mysql://%s:%d/%s";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "iot22-23";
    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String DATABASE_NAME = "iot";

    private static final RemoteControlApplication instance = new RemoteControlApplication();

    String[] attuatori = new String[3];
    static HashMap<String, String[]> pazienti = new HashMap<>();

    public static RemoteControlApplication getInstance() {
        return instance;
    }

    public String retrieveActuatorType(int index)
    {
        String type = "";
        switch (index) {
            case 0:
                type = "med";
                break;
            case 1:
                type = "mask";
                break;
            case 2:
                type = "altro";
                break;
            default:
                System.out.println("Invalid control value!");
                return "";
        }
        return type;
    }

    public boolean alreadyInAction(String patient_id, int index)
    {
        for(String ids : pazienti.keySet())
        {
            if(ids.equals(patient_id))
            {
                // Allora è lo stesso paziente
                // Controlliamo se è attivo l'attuatore di cui abbiamo bisogno
                if(!pazienti.get(patient_id)[index].equals(""))
                {
                    // Allora quell attuatore di quel paziente è gia in azione
                    // Restituisco si
                    return true;
                }
            }
        }
        return false;
    }

    public boolean registerActuator(String patient_id, int index) throws SQLException
    {
        for(String ids : pazienti.keySet())
        {
            if(ids.equals(patient_id))
            {
                // Allora è lo stesso paziente
                // Controlliamo se è attivo l'attuatore di cui abbiamo bisogno
                if(pazienti.get(patient_id)[index].equals(""))
                {
                    String[] tmp = pazienti.get(patient_id);

                    Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
                    // Deve andare nella tabella degli attuatori
                    // PreparedStatement ps = connection.prepareStatement("SELECT ip, status FROM actuators WHERE type = ?");
                    PreparedStatement ps = connection.prepareStatement("SELECT ip FROM actuators WHERE type = ?");
                    String type = retrieveActuatorType(index);
                    ps.setString(1, type);
                    ResultSet rs = ps.executeQuery();
                    rs.close();
                    tmp[index] = rs.getString("ip");
                    pazienti.put(patient_id, tmp); 
                    return true;
                }
            }
        }
        return false;
    }

    public static String retrieveActuatorIP(String patient_id, int index) throws SQLException 
    {
        for(String ids : pazienti.keySet())
        {
            if(ids.equals(patient_id))
            {
                // Allora è lo stesso paziente
                // Controlliamo se è attivo l'attuatore di cui abbiamo bisogno
                if(!pazienti.get(patient_id)[index].equals(""))
                {
                    // Allora quell attuatore di quel paziente è gia in azione
                    // Restituisco si
                    return pazienti.get(patient_id)[index];
                }
            }
        }
        return "";
        /*
        String result = String.valueOf(id_paziente.charAt(0));
        String type = "";
        switch (result) {
            case "M":
                type = "med";
                break;
            case "K":
                type = "mask";
                break;
            case "A":
                type = "altro";
                break;
            default:
                System.out.println("Invalid control value!");
                return "";
        }
        // Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
        // Deve andare nella tabella degli attuatori
        // PreparedStatement ps = connection.prepareStatement("SELECT ip, status FROM actuators WHERE type = ?");
        // PreparedStatement ps = connection.prepareStatement("SELECT ip FROM actuators WHERE type = ?");
        // ps.setString(1, type);
        // ResultSet rs = ps.executeQuery();
        HashMap<String, Boolean> res = new HashMap<>();
        ip.put("ip", rs.getString("ip"));
        ip.put("status", rs.getString("status"));
        rs.close();
        return ip;
        */
    }

    public void run() {
        alreadyActivated = new HashMap<>();
        Boolean[] booleani = new Boolean[3];
        for (int i = 0; i < 3; i++) {
            attuatori[i] = "";
            booleani[i] = false;
        }
        pazienti.put("", attuatori);

        try {
            List<String> typeList = Arrays.asList("oxygen_sensor", "temperature_sensor", "heartbeat_sensor");
            HashMap<String, Float> retrieved = new HashMap<>();

            try (Connection connection = DB.getDb()) {
                for (String s : typeList) {
                    String query = "SELECT id, AVG(value) AS media_valore "
                            + "FROM ( "
                            + "  SELECT "
                            + "    t1.id, "
                            + "    t1.value "
                            + "  FROM "
                            + "    (SELECT "
                            + "      @row_num := IF(@prev_value = id, @row_num + 1, 1) AS rn, "
                            + "      value, "
                            + "      @prev_value := id AS id "
                            + "    FROM "
                            + "      " + s + " t, "
                            + "      (SELECT @row_num := 1) x, "
                            + "      (SELECT @prev_value := '') y "
                            + "    ORDER BY "
                            + "      id, "
                            + "      timestamp DESC "
                            + "    ) t1 "
                            + "  WHERE t1.rn <= 3 "
                            + ") t2 "
                            + "GROUP BY id";

                    try (PreparedStatement ps = connection.prepareStatement(query)) {
                        try (ResultSet result = ps.executeQuery()) {
                            int count = 0;
                            while (result.next()) {
                                float sensorValue = result.getFloat("media_valore");
                                String id = result.getString("id");
                                retrieved.put(id, sensorValue);
                                count++;
                            }
                            System.out.println("HO " + count + " ELEMENTI NELLA MAPPA");
                        }
                    }
                }
            } 
            catch (SQLException e) 
            {
                e.printStackTrace();
            }
            if (retrieved.isEmpty()) 
            {
                System.out.println("NESSUN DATO RECUPERATO\n");
            } 
            else 
            {
                for (String s : retrieved.keySet()) 
                {
                    String id_paziente = s.substring(1,s.length()-1);
                    if (s.startsWith("H")) 
                    {
                        if (retrieved.get(s) >= DNG_HB_TH) 
                        {
                            System.out.println("BATTITO CRITICO :  " + retrieved.get(s));
                            String actuatorIp = "";
                            // actuatorIp = retrieveActuatorIP(id_paziente,0);
                            if(alreadyInAction(id_paziente, 0))
                            {
                                // Teoricament non fai nulla
                                // Ce un pericolo ma l'attuatore è gia attivo
                                System.out.println("Attuatore gia in azione\n");
                            }
                            else
                            {
                                // Si deve registrare
                                // e fare la chiamata al coap client
                                // per attivare l'attuatore
                                // -> query per cambiare lo stato da off ad on
                                System.out.println("TRYNA REGISTER ACTUATOR\n");
                                if(registerActuator(id_paziente, 0))
                                {
                                    // Registrazzione avvenuta con successo
                                    // Messo dentro la mappa
                                    System.out.println("ACTUATOR REGISTERED\n");
                                    actuatorIp = retrieveActuatorIP(id_paziente,0);
                                    // Adesso deve fare la chiamata al coap client per attivarlo
                                    
                                    // Response response = null;
                                    
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
                                            }
                                            else
                                            {
                                                System.out.println("ERRORE NELLA UPDATE UPDATE\n SUCCESS="+success);
                                                // response = new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                                            }
                                        }
                                    }
                                    catch (SQLException e)
                                    {
                                        e.printStackTrace();
                                        // response = new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR); // handle SQLException
                                    }
                                    
                                    // Return number of raw coinvolte
                                    //return ps.getUpdateCount();
                                    // exchange.respond(response);

                                    // Fatta la update deve richiedere la put
                                    // s = nome risorsa attuatore
                                    CoapClient client = new CoapClient("coap://[" + actuatorIp + "]/" + s);

                                    JSONObject object = new JSONObject();
                                    object.put("action", "ON");

                                    CoapResponse response = client.put(object.toJSONString().replace("\"",""), MediaTypeRegistry.APPLICATION_JSON);

                                    if (response == null) 
                                    {
                                        System.err.println("An error occurred while contacting the actuator");
                                    } 
                                    else 
                                    {
                                        CoAP.ResponseCode code = response.getCode();
                                        //System.out.println(code);
                                        switch (code) 
                                        {
                                            case CHANGED:
                                                System.err.println("State correctly changed because of danger or user input");
                                                break;
                                            case BAD_OPTION:
                                                System.err.println("Parameters error");
                                                break;
                                        }
                                    }
                                }
                            }
                                
                        } 
                        else if(retrieved.get(s) >= CTR_HB_TH)
                        {
                            System.out.println("BATTITO PERICOLOSO :  " + retrieved.get(s));
                        }
                        else
                        {
                            System.out.println("BATTITO NON CRITICO :  " + retrieved.get(s));
                        }
                    } else if (s.startsWith("O")) {
                        if (retrieved.get(s) >= DNG_OX_TH) {
                            System.out.println("OSSIGENO CRITICO :  " + retrieved.get(s));
                        } else {
                            System.out.println("OSSIGENO NON CRITICO :  " + retrieved.get(s));
                        }
                    } else if (s.startsWith("T")) {
                        if (retrieved.get(s) >= DNG_TEMP_TH) {
                            System.out.println("TEMPERATURA CRITICA :  " + retrieved.get(s));
                        } else {
                            System.out.println("TEMPERATURA NON CRITICA :  " + retrieved.get(s));
                        }
                    } else {
                        System.out.println("SENSORE NON RICONOSICUTO\nSENORI DISPONIBILI: ossigeno, battito, temperatura\n");
                    }
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            Thread.currentThread().interrupt();
        }

    }

    public static void main(String[] args) {
        Thread thread = new Thread(new RemoteControlApplication());
        thread.start();
    }

}