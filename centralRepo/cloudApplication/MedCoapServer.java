/*
public static class MyCoapResource extends CoapResource 
    {
        public MyCoapResource(String name)
        {
            super(name);
        }

        @Override
        public void handleGET(CoapExchange exchange) 
        {
            exchange.respond("NO GET HANDLER\n");
        }
        @Override
        public void handlePOST(CoapExchange exchange) 
        {
            System.out.println("STARTING HANDLE POST\n");
            // Try to register new actuator

            String s = new String(exchange.getRequestPayload());
            
            JSONObject obj;
            JSONParser parser = new JSONParser();

            System.out.println("TRYNA PARSE STRING: "+s);
            
            try 
            {
                obj = (JSONObject) parser.parse(s);
            } 
            catch (ParseException e) 
            {
                throw new RuntimeException(e);
            }

            Response response = null;
            InetAddress address = exchange.getSourceAddress();

            System.out.println("TRYNA REGISTER THE ACTUATOR WITH IP: "+address);
            
            int success = -1;
            
            try (Connection connection = DB.getDb())
            {
                System.out.println("CONNESSIONE STABILITA\n");
                try (PreparedStatement ps = connection.prepareStatement("REPLACE INTO iot.actuator(ip, type) VALUES(?,?);")) 
                {
                    String ip = address.toString().substring(1); // Rimuove il slash iniziale
                    // 1 = Inet
                    ps.setString(1, ip); //substring(1)
                    // 2 = Tipo attuatore
                    ps.setString(2, (String) obj.get("type"));
                    
                    System.out.println("PREPARED STATEMENT CON INDIRIZZO: "+ ip +" TIPO ATTUATORE: "+(String) obj.get("type"));
                    System.out.println("CERCO DI ESEGUIRE LA UPDATE\n");
                    
                    // Tryna execute UPDATE
                    ps.executeUpdate();
                    // Ritorna il numero di righe coinvolte
                    success = ps.getUpdateCount();
                    if(success>0)
                    {
                        System.out.println("UPDATE ESEGUITA\n");
                        System.out.println("RIGHE COINVOLTE: "+success+"\n");
                        response = new Response(CoAP.ResponseCode.CREATED);
                    }
                    else
                    {
                        System.out.println("ERRORE NELLA UPDATE UPDATE\n SUCCESS="+success);
                        response = new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                    }
                }
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                response = new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR); // handle SQLException
            }
            
            // Return number of raw coinvolte
            //return ps.getUpdateCount();
            exchange.respond(response);
        }
    }

    public Coordinator() {
        // Inizializzazione del server CoAP
        this.coapServer = new CoapServer(5683);
        this.coapServer.add(new MyCoapResource("test"));
        this.coapServer.start();

        // Inizializzazione del client MQTT
        this.mqttClient = connectToBroker();
        subscribeToTopics();
    } 
*/

import org.eclipse.californium.core.CoapServer;

public class MedCoapServer extends CoapServer implements Runnable
{
    public static final MedCoapServer server = new MedCoapServer();

    private CoapServer() {}
    
    public static MedCoapServer getServer()
    {
        return server;
    }

    public void run()
    {
        // Createes a new instance of the servr
        MedCoapServer medServer = new MedCoapServer();
        // Adds a resource to implements serber registration
        medServer.add(new medCoapRegistrator("registrator"));
        System.out.println("TRYNA START THE SERVER\n...\n...\n");
        // Fa partire il thread
        medServer.start();
        System.out.println("SERVER STARTED\n");
    }

}