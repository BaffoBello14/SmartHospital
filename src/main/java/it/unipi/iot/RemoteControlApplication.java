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

public class RemoteControlApplication implements Runnable {

    // Setting threshold

    // DANGER: TEMP > 39
    private static float DNG_TEMP_TH = 38.9f;

    // DANGER OX < 50
    private static float DNG_OX_TH = 49.9f;


    // DANGER: HB < 70
    private static int DNG_HB_TH = 70;

    private static final RemoteControlApplication instance = new RemoteControlApplication();
    
    public static RemoteControlApplication getInstance(){
        return instance;
    }

    public void run() 
    {

        try
        {
            // HashMap<String, Integer> values = DatabaseAccess.retrieveData();
            //System.out.println(values);
            
            // Preparing HashMap to temporaneamente store data retireved
            // id STARTSWITH O = oxygen
            // id STARTSWITH H = hb
            // id STARTSWITH T = temperature
            // String = idSensore
            // Value = valoreSensore
            HashMap<String, Float> retrieved = new HashMap<>();

            // Try to connect w/ DB
            // try (Connection connection = DB.getDb())
            List<String> typeList = Arrays.asList("oxygen_sensor", "temperature_sensor", "heartbeat_sensor");
            // String = tipo sensore
            // Float = media ultimi 3 valori sensore
            try (Connection connection = DB.getDb())
            {
                for (String s : typeList) 
                {
                    System.out.println("MONITORING "+s+"TABLE\n");
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
                    // Creazione del PreparedStatement
                    try (PreparedStatement ps = connection.prepareStatement(query)) 
                    {

                        // Eseguire la query
                        try (ResultSet result = ps.executeQuery()) 
                        {

                            // Processa i risultati
                            int count = 0;
                            while (result.next()) 
                            {
                                float sensorValue = result.getFloat("media_valore");
                                String id = result.getString("id");
                                System.out.println("ID: " + id + ", Media Valore: " + sensorValue);
                                // Inserisco la misurazione recuperata nella mappa
                                retrieved.put(id, sensorValue);
                                count++;

                            }
                            System.out.println("HO "+count+" ELEMENTI NELLA MAPPA");
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            if(retrieved.isEmpty())
            {
                // Mappa vuota
                System.out.println("NESSUN DATO RECUPERATO\n");
            }
            else
            {
                // Implements logic control
                // Controllo soglie di threshold
                // Recupero il primo numero dell'id che rappresenta il tipo di sensore
                for(String s : retrieved.keySet())
                {
                    if(s.startsWith("H"))
                    {
                        // Allora stiamo analizzando il battito
                        // Recupero il valore corrispondente al battito
                        if(retrieved.get(s)>=DNG_HB_TH)
                        {
                            // Allora il battito cardiaco è critico -> do something
                            System.out.println("BATTITO CARDIACO CRITICO :  "+retrieved.get(s));
                            // Implementare logica di controllo 
                        }
                    }
                    else if(s.startsWith("O"))
                    {
                        if(retrieved.get(s)>=DNG_OX_TH)
                        {
                            // Allora l'ossigeno è critico -> do something
                            System.out.println("OSSIGENO CRITICO :  "+retrieved.get(s));
                            // Implementare logica di controllo 
                        }
                    }
                    else if(s.startsWith("T"))
                    {
                        if(retrieved.get(s)>=DNG_TEMP_TH)
                        {
                            // Allora temperatura critica -> do something
                            System.out.println("TEMPERATURA CRITICA :  "+retrieved.get(s));
                            // Implementare logica di controllo 

                        }
                    }
                    else
                    {
                        // Il sensore non esiste, scritto male
                        // non ci dovrebbe entrare mai
                        System.out.println("SENSORE NON RICONOSICUTO\nSENORI DISPONIBILI: ossigeno, battito, temperatura\n");
                    }
                }
            }
        }
        
        finally {
            Thread.currentThread().interrupt();
        }

    }

    public static void main(String[] args) {
        Thread thread = new Thread(new RemoteControlApplication());
        thread.start();
    }

}


