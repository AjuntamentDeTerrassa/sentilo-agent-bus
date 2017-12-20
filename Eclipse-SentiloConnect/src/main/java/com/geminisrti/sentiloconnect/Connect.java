/**
 * Class Connect.
 * @author: Miguel Angel Pescador <miguelangelps@prometeoinnova.com>
 * @package com.geminisrti.sentiloconnect
 * @copyright 20/04/17 Geminis RTI.
 */
package com.geminisrti.sentiloconnect;

import au.com.bytecode.opencsv.CSVReader;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.collections.MapIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 * Class Connect
 * @author mapescador
 */
public class Connect {
    /**
     * @see Logger
     */
    private static final Logger log = Logger.getLogger(SentiloConnect.class);
    
    /**
     * @see int
     */
    private final static boolean debug=true;
    /**
     * @see String
     */
    private String url;
    /**
     * @see String
     */
    private String sentilo;
    private String sentiloSensor;
    /**
     * @see String
     */
    private String identityKey;
    private String listFilePath;
    /**
     * @var ScheduledExecutorService
     */
    private static  ScheduledExecutorService timerCheck = Executors.newSingleThreadScheduledExecutor();
    /**
     * @see ScheduledExecutorService
     */
    private static  ScheduledExecutorService timerRetry = Executors.newSingleThreadScheduledExecutor();
     /**
     * @see ScheduledExecutorService
     */
    private static  ScheduledExecutorService timerConfig = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * @see SimpleCache
     */
    private SimpleCache<String, String> cache;
    private List<String> busReferences; 
    Map<String, String> historic = new HashMap<String, String>();
    private int numBuses=0;
    private int saeRefresh=0;
    private CSVReader reader;
    private int numhilos=0;
    private int remainder;
    /**
     * Connect Constructor
     * 
     * This constructor receive the parameters to initiate the each 30 seconds 
     * loop to retrieve full information from SAE servers and manage the failed
     * elements stored in cache.
     * 
     * Two scheduled task will be launched:
     * -The normal flow for the application each 30 seconds
     * -The retry flow each 10 seconds.
     * 
     * @param soapserver Url of SAE SOAP Server
     * @param restserver Url of Sentilo REST Server
     * @param sentiloSensorPassed Url of Sentilo REST Server to Create/Delete Sensors
     * @param identitykey Token of Sentilo REST SERVER
     * @param cachettl Time to live in seconds for cached element
     * @param cacherefresh Time to refresh cached elements
     * @param cachemaxitems Max items on cache
     * @param listFilePathPassed Filename of the list csv
     * @param saeRefreshPassed Refresh time of the sae server.
     */
    public Connect(String soapserver, String restserver,String sentiloSensorPassed, String identitykey,long cachettl, int cacherefresh, int cachemaxitems, String listFilePathPassed, int saeRefreshPassed){
        
        this.url=soapserver;
        this.sentilo=restserver;
        this.sentiloSensor=sentiloSensorPassed;
        this.identityKey=identitykey;
        this.cache = new SimpleCache<String, String>(cachettl, cacherefresh, cachemaxitems);
        this.listFilePath=listFilePathPassed;
        this.saeRefresh=saeRefreshPassed;
        this.configureLogger();        
        //System.out.println("Refreshing bus list");
        log.debug( "Parameters:\n"+this.url
                +"\n"+this.sentilo
                +"\n"+this.sentiloSensor
                +"\n"+this.identityKey
                +"\n"+cachettl
                +"\n"+cacherefresh
                +"\n"+cachemaxitems
                +"\n"+this.listFilePath
                +"\n"+ this.saeRefresh+"\n");
            
        //(listFilePath);
        this.busReferences=refreshBusList();      
       
        // -------------------------
    	// Timer Check received data
    	// each 1 seconds
    	// -------------------------
        //System.out.println("Launch getData.");
        log.info ("busReferences:"+(String)this.busReferences.get(0));

    	timerCheck.scheduleAtFixedRate(new TimerTask() {
    		
    		public void run() {
                        if(busReferences.isEmpty()){
                            log.debug("Not found References. Reloading");
                            busReferences=refreshBusList();
                        }
                        /*System.out.println("Init getData");
                        System.out.println("Remainder"+remainder+"\n");
                        System.out.println("Numhilos:"+numhilos+"\n");
                        System.out.println("busReferences:"+busReferences.get(0));
                        */
                        //Start the data collection
                        if(remainder>0){
                            getData(numhilos+1, busReferences);
                            remainder--;
                        }
                        else
                            getData(numhilos, busReferences);
    		}

    	}, 1000,  1000, TimeUnit.MILLISECONDS);
         // -------------------------
    	// Timer Retry send data
    	// each 10 seconds
    	// -------------------------
        //System.out.println("Launch getDataRetry.");
    	timerRetry.scheduleAtFixedRate(new TimerTask() {
    		
    		@SuppressWarnings("unchecked")
			public void run() {
                        //System.out.println("Init getDataRetry");
                        SimpleCache<String, String>.SimpleCacheObject c = null;
                        int counter = 0;
                        while(cache.size()>0 && counter<numhilos){
                            //Start the data collection
                            //Iterate Simple Cache
                            MapIterator itr = cache.iterator();
                            
                            if (itr.hasNext()) {                   
                                itr.next();
                                c = (SimpleCache<String, String>.SimpleCacheObject) itr.getValue();
                                String json=(String)c.value;
                                sendSentiloRetry(json);
                                counter++;
                            }
                        }

    		}

    	}, 1 * 1000, 1 * 1000, TimeUnit.MILLISECONDS);
        //Update Configuration after 24 hours. 
        timerConfig.scheduleAtFixedRate(new TimerTask() {
    		
    		public void run() {
                    //Load config file
                    Properties defaultProps = new Properties();
                    try{

                        FileInputStream in = new FileInputStream("/etc/sentilo/preferences.properties");
                        defaultProps.load(in);
                        in.close();
                    }catch(FileNotFoundException fnfe){
                        //System.out.println("Configuration file (/etc/sentilo/preferences.properties) does not exists.");
                        log.error( "Configuration file (/etc/sentilo/preferences.conf) does not exists." + "\n" );
                    } catch (IOException ex) {
                        //System.out.println("Configuration file (/etc/sentilo/preferences.properties) does not exists.");
                        log.error( "Configuration file (/etc/sentilo/preferences.properties) does not exists." + "\n" );
                    }
                    url=defaultProps.getProperty("SOAPServer");
                    sentilo=defaultProps.getProperty("RESTServerData");
                    sentiloSensor=defaultProps.getProperty("RESTServerSensor");
                    identityKey=defaultProps.getProperty("IDENTITY_KEY");
                    long cachettl=Integer.parseInt(defaultProps.getProperty("CacheTTL"));
                    int cacherefresh=Integer.parseInt(defaultProps.getProperty("CacheRefresh"));
                    int cachemaxitems=Integer.parseInt(defaultProps.getProperty("CacheMaxItems"));
                    saeRefresh=Integer.parseInt(defaultProps.getProperty("SAERefreshRate"));
                    cache = new SimpleCache<String, String>(cachettl, cacherefresh, cachemaxitems);
                    listFilePath=defaultProps.getProperty("ListBusFilePath");

    		}

    	}, 24, 24, TimeUnit.HOURS);
        
    }
    /**
     * Connect Constructor
     * 
     * This constructor receive the parameters to create or destroy sensors
     * in sentilo server.
     * 
     * The option parameter define the create/delete operation:
     * -1 to create.
     * -0 to delete.
     * 
     * @param sentiloSensor Url of Sentilo REST Server
     * @param identitykey Token of Sentilo REST SERVER
     * @param listFilePathPassed Filename of the list csv
     * @param operation Value 1 to create or 0 to delete.

     */
    public Connect(String sentiloSensor, String identitykey, String listFilePathPassed, int operation){
        
        this.sentiloSensor=sentiloSensor;
        this.identityKey=identitykey;
        this.listFilePath=listFilePathPassed;
        this.saeRefresh=2;
        this.configureLogger();        
        
        //System.out.println("Refreshing bus list");
        log.debug( "Parameters:\n"+this.sentiloSensor
                +"\n"+this.identityKey
                +"\n"+this.listFilePath
                +"\n");
            
        //(listFilePath);
        this.busReferences=refreshBusList();      
       
        // -------------------------
    	// Timer Check received data
    	// each 1 seconds
    	// -------------------------
        //System.out.println("Launch getData.");
        log.info("busReferences:"+(String)this.busReferences.get(0));
        
        String [] nextLine;
            //Create all sensors
            for (int i=0;i<this.numBuses;i++){
                //if reader next == null refresh buslist.
                nextLine = new String[2];
                if(busReferences.isEmpty()){
                    log.debug("Not found References. ArrayList");
                    refreshBusList();
                    return;
                }
                nextLine[0]=(String)busReferences.get(0);
                busReferences.remove(0);
                nextLine[1]=(String)busReferences.get(0);          
                busReferences.remove(0);
               
                //Set Operation
                if(operation==1){
                    //Create sensors
                    log.debug("Creating Sensor for reference: "+nextLine[1]);
                    this.sendSentiloSensor(this.createSensors(nextLine[1]),this.sentiloSensor, nextLine[1]);
                }else{
                    //Delete Sensors 
                    log.debug("Deleting Sensor for reference: "+nextLine[1]);
                    this.sendSentiloDelete(this.sentiloSensor,nextLine[1]);
                }
                
            }
        
    }
    /**
     * Configure logger
     */
    public void configureLogger(){
         //Configure Logger
        // -------------------------
    	// Configure Log4J
    	// -------------------------
    	BasicConfigurator.configure();
    	log.setLevel(Level.ALL);

    	//This is the root logger provided by log4j
    	Logger rootLogger = Logger.getRootLogger();
    	rootLogger.setLevel(Level.ALL);

    	//Define log pattern layout
    	PatternLayout layout = new PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n");

    	//Add console appender to root logger
    	rootLogger.addAppender(new ConsoleAppender(layout));
        //Define file appender with layout and output log file name
        try
    	{
	    File loggerpath = new File("/var/log/sentilolog.log");
            RollingFileAppender fileAppender=null;
            if(loggerpath.canWrite()) {
                //Define file appender with layout and output log file name
                fileAppender = new RollingFileAppender(layout, "/var/log/sentilolog.log");
                fileAppender.setMaxBackupIndex(50);
                fileAppender.setMaxFileSize("20MB");
            }else{
                log.info("/var/log/sentiloconnect.log not writable. Using tmp appender /tmp/sentilolog.log");
                fileAppender = new RollingFileAppender(layout, "/tmp/sentilolog.log");
                fileAppender.setMaxBackupIndex(50);
                fileAppender.setMaxFileSize("20MB");
            }
	    	//Add the appender to root logger
	    	rootLogger.addAppender(fileAppender);
    	}
    	catch (IOException e)
    	{
    		log.error("Failed to add appender in /var/log !!");
    		
    		// Debug
			if (debug){
				String exceptionMessage = e.getMessage();
				log.debug( exceptionMessage + "\n" );
				log.debug( "Got an Exception: " + e.getClass().getName() + ": " + e.getMessage() + "\n" );
			}
            
                 // -------------------------
	    	// Exit terminate application
	    	// -------------------------
	    	Runtime.getRuntime().exit(0);
	    	// -------------------------
	    	// -------------------------
    	}
    }
    /**
     * Start method for data collection
     */
    public void getData(int hilos, List<String> busReferences){
                     
        for (int i=0;i<hilos;i++){
            //if reader next == null refresh buslist.
            final String [] nextLine = new String[2];
            if(busReferences.isEmpty()){
                log.debug("Not found References. ArrayList");
                refreshBusList();
                return;
            }
            nextLine[0]=(String)busReferences.get(0);
            busReferences.remove(0);
            nextLine[1]=(String)busReferences.get(0);          
            busReferences.remove(0);

            log.debug("Element data on: "+nextLine[0]+" reference "+nextLine[1]);
            new Thread(new Runnable() {
                public void run() {
                    try {  
                        String responseSoap="";
                        // Create SOAP Connection
                        // Send SOAP Message to SOAP Server
                        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
                        SOAPConnection soapConnection = soapConnectionFactory.createConnection();
                       // nextLine[] is an array of values from the line
                        SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(nextLine[0]), url);

                        // Process the SOAP Response
                        responseSoap=parseSOAPResponse(soapResponse);
                        String json=generateJson(responseSoap,nextLine[1]);
                        //If the soap request generate sentilo request
                        if(!json.equals(""))
                            sendSentilo(json, sentilo, nextLine[1]);
                        soapConnection.close();     
                    } catch (Exception e) {
                        log.error("Error occurred while sending SOAP Request to Server");
                        log.error(e.toString());
                        e.printStackTrace();
                    }
                }
            }).start();

        }     
       //System.out.println(reference);
    }
    /**
     * createSOAPRequest
     * @param reference Vehicle reference
     * @return SOAPMessage
     * @see SOAPMessage
     * @throws Exception Throwable Exception
     */
    private SOAPMessage createSOAPRequest(String reference) throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        soapMessage.getSOAPHeader().setPrefix("soapenv");
       // soapMessageContext.setMessage(soapMessage);
   

        SOAPPart soapPart = soapMessage.getSOAPPart();

        String serverURI = "http://tempuri.org/";
        String serverURISiri = "http://www.siri.org.uk/siri";
        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();   
        envelope.setPrefix("soapenv");
        envelope.addNamespaceDeclaration("tem", serverURI);
        envelope.addNamespaceDeclaration("siri", serverURISiri);
        envelope.removeNamespaceDeclaration("SOAP-ENV");
        //Set Header
        SOAPHeader header=soapMessage.getSOAPHeader();                      
        header.setPrefix("soapenv");    
       
        // SOAP Body
        SOAPBody soapBody = envelope.getBody();
        soapBody.setPrefix("soapenv");
        SOAPElement soapBodyElem = soapBody.addChildElement("GetVehicleMonitoring", "tem");
        SOAPElement soapBodyElem1 = soapBodyElem.addChildElement("request", "tem");
        //Login Info
        SOAPElement soapBodyElem2 = soapBodyElem1.addChildElement("ServiceRequestInfo");
        SOAPElement soapBodyElem3 = soapBodyElem2.addChildElement("AccountId", "xxxx");
        soapBodyElem3.addTextNode("siriroot");
        SOAPElement soapBodyElem4 = soapBodyElem2.addChildElement("AccountKey", "xxxx");
        soapBodyElem4.addTextNode("gmvsirigmv");
        //Request Info
        SOAPElement soapBodyElem5 = soapBodyElem1.addChildElement("Request");
        soapBodyElem5.setAttribute("version", "2.0");
        SOAPElement soapBodyElem6 = soapBodyElem5.addChildElement("VehicleMonitoringRef", "xxxx");
        soapBodyElem6.addTextNode(reference);

        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", serverURI  + "GetVehicleMonitoring");

        soapMessage.saveChanges();

        return soapMessage;
    }

    /**
     * Method used to print the SOAP Response
     * 
     * @param soapResponse SOAPMessage with the SOAP Response.
     * @return String Parsed String
     * @throws Exception Throwable Exception
     */
    private String parseSOAPResponse(SOAPMessage soapResponse) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        Source sourceContent = soapResponse.getSOAPPart().getContent();
        log.debug("\nResponse SOAP Message = ");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        soapResponse.writeTo(out);
        String strMsg = new String(out.toByteArray());
        log.debug(strMsg);
        StreamResult result = new StreamResult(System.out);
        transformer.transform(sourceContent, result);
        XMLInputFactory xif = XMLInputFactory.newFactory();
        XMLStreamReader xsr = xif.createXMLStreamReader(soapResponse.getSOAPPart().getContent());
        xsr.nextTag(); // Advance to Envelope tag
        xsr.nextTag(); // Advance to Body tag
        xsr.nextTag(); // Advance to GetVehicleMonitoringResponse
        xsr.nextTag(); // Advance to GetVehicleMonitoringResult
        xsr.nextTag(); // Advance to ServiceDeliveryInfo
        xsr.nextTag(); // Answer
        String responseTimestamp="";
        StringBuilder resultado = new StringBuilder();
        while (xsr.hasNext()) {
            int eventType = xsr.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    resultado.append("\n"+xsr.getLocalName()+"|");
                    break;
                case XMLStreamReader.CHARACTERS:
                case XMLStreamReader.CDATA:
                    resultado.append(xsr.getText());
                    break;
                case XMLStreamReader.END_ELEMENT:
                    responseTimestamp=resultado.toString();
            }
        }
        return responseTimestamp;
    }
    /**
     * Generate Json
     * @param responseTimestamp String with the parsed SOAP Message
     * @param reference Reference
     * @return String
     */
    private String generateJson(String responseTimestamp,String reference){
         //System.out.println(responseTimestamp); 
        StringTokenizer st=new StringTokenizer(responseTimestamp,"\n");
        Map<String, String> map = new HashMap<String, String>();
        String json ="";
        while(st.hasMoreTokens()){
            String token=st.nextToken();
            StringTokenizer innerst=new StringTokenizer(token,"|");
            String nombre=innerst.nextToken();
            if(innerst.hasMoreTokens()){
                String valor=innerst.nextToken();
                map.put(nombre, valor);
            }
        }
        //Check the type of Response.
        if(map.containsKey("Monitored")){
            String time=this.fixDate(map.get("RecordedAtTime"));
            //Check if Location is "0 0"
            if(map.get("Latitude").equals("0") && map.get("Longitude").equals("0")){
                log.debug("Ignorando Localización '0 0': "+reference+" con tiempo "+time);
                return "";
            }
            //Check if register is duplicated.
            if(this.historic.containsKey(reference)){
                log.debug("Referencia "+reference+" existe con tiempo: "+this.historic.get(reference));
                if(this.historic.get(reference).equals(time)){
                    log.debug("Ignorando Registro Duplicado: "+reference+" con tiempo "+time);
                    //System.out.println("Registro Duplicado: "+time);
                    return "";
                }else {
                    log.debug("Actualizamos Referencia => Metemos referencia: "+reference+" con tiempo: "+time);
                    this.historic.put(reference, time);
                }
            }else{
                log.debug("Referencia no existe => Metemos referencia: "+reference+" con tiempo: "+time);
                this.historic.put(reference, time);
            }
            
            Observations vehicle_orientation= new Observations(time, map.get("Latitude")+" "+map.get("Longitude"),map.get("Bearing"));
            Observations vehicle_speed      = new Observations(time, map.get("Latitude")+" "+map.get("Longitude"),map.get("Velocity"));
            Observations vehicle_delay      = new Observations(time, map.get("Latitude")+" "+map.get("Longitude"));
            Observations vehicle_line       = new Observations(time, map.get("Latitude")+" "+map.get("Longitude"));
            Observations vehicle_direction  = new Observations(time, map.get("Latitude")+" "+map.get("Longitude"));
            
            //Type 3 Response
            if(map.containsKey("LineRef")){
                vehicle_delay.setValue(map.get("Delay"));
                vehicle_line.setValue(map.get("LineRef"));
                vehicle_direction.setValue(map.get("DirectionRef"));
            }
            Sensor s_vehicle_orientation=new Sensor(reference+"-vehicle_orientation",vehicle_orientation);
            Sensor s_vehicle_speed=new Sensor(reference+"-vehicle_speed",vehicle_speed);
            Sensor s_vehicle_delay=new Sensor(reference+"-vehicle_delay",vehicle_delay);
            Sensor s_vehicle_line=new Sensor(reference+"-vehicle_line",vehicle_line);
            Sensor s_vehicle_direction=new Sensor(reference+"-vehicle_direction",vehicle_direction);
            //Add sensors to List
            Sensors sensors=new Sensors();
            sensors.addSensor(s_vehicle_orientation);
            sensors.addSensor(s_vehicle_speed);
            sensors.addSensor(s_vehicle_delay);
            sensors.addSensor(s_vehicle_line);
            sensors.addSensor(s_vehicle_direction); 
            //Jsonize Sensors
            Gson gson = new Gson();
            json = gson.toJson(sensors);
            //Double check to assure sentilo does not have already the data
            if(checkDataSentilo(this.sentilo, reference, time))
                return "";
            log.debug("Sensors String Json:"+json);
            //Send to Sentilo
        }
        
        return json;
    }
    /**
     * Send Update to Sentilo Sensors
     * @param json Json to send.
     * @param url Url to send the json.
     * @param reference Reference of the vehicle.
     * @throws RuntimeException 
     */
    public void sendSentilo(String json,String url, String reference) throws RuntimeException{
        try {
                //Initiate http connection
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPut putRequest = new HttpPut(url);
                StringEntity params =new StringEntity(json,"UTF-8");
                params.setContentType("application/json");
		putRequest.addHeader("accept", "application/json");
                //Add identity key
                putRequest.addHeader("IDENTITY_KEY",this.identityKey);
                putRequest.setEntity(params);
		HttpResponse response = httpClient.execute(putRequest);
		String output;
                BufferedReader br = new BufferedReader(
                         new InputStreamReader((response.getEntity().getContent())));
                log.debug("Output from Server .... \n");
		while ((output = br.readLine()) != null) {
			log.debug(output);
		}
                //If response not ok.
		if (response.getStatusLine().getStatusCode() != 200) {
                    // If sensor does not exits, create it. and retry.
                    if(response.getStatusLine().getStatusCode() == 404) {
                        httpClient.getConnectionManager().shutdown();
                        //create sensor and retry
                        if(sendSentiloSensor(createSensors(reference), this.sentiloSensor, reference)){
                            //Wait to propagate changes
                             Thread.sleep(1000);
                            //Retry
                            sendSentilo(json,url, reference);
                            return;
                        }
                    }
                    log.error("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
                    //If error store data in cache.
                    cache.put(response.getStatusLine().getStatusCode()+"-"+System.currentTimeMillis(),json);
                    httpClient.getConnectionManager().shutdown();
                    return;
		}

		httpClient.getConnectionManager().shutdown();

	  } catch (ClientProtocolException e) {
                log.error(e.toString());
		e.printStackTrace();

	  } catch (IOException e) {
                log.error(e.toString());
		e.printStackTrace();
	  }catch (InterruptedException e){
               log.error(e.toString());
		e.printStackTrace();
          }
    }
    /**
     * Send Update to Sentilo Sensors
     * @param url Url to send the request.
     * @param reference Reference of the vehicle.
     * @param timestamp timestamp for reference to check
     * @throws RuntimeException 
     */
    public boolean checkDataSentilo(String url, String reference, String timestamp) throws RuntimeException{
        try {
                //Initiate http connection
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet getRequest = new HttpGet(url+"/"+reference+"-vehicle_speed?limit=10&from="+timestamp+"&to="+timestamp);
                log.debug("Get Request to Server Url: "+url+"/"+reference+"-vehicle_speed?limit=10&from="+timestamp+"&to="+timestamp);
		getRequest.addHeader("accept", "application/json");
                //Add identity key
                getRequest.addHeader("IDENTITY_KEY",this.identityKey);
                //getRequest.setEntity(params);
		HttpResponse response = httpClient.execute(getRequest);
		String output;
                String salida="";
                BufferedReader br = new BufferedReader(
                         new InputStreamReader((response.getEntity().getContent())));
                log.debug("Output from Server .... \n");
		while ((output = br.readLine()) != null) {
			log.debug(output);
                        salida+=output;
		}
                //If response not ok.
		if (response.getStatusLine().getStatusCode() != 200) {
                    log.error("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
                    httpClient.getConnectionManager().shutdown();
                    //Server error continue as if does not exists
                    return false;
		}
                log.debug("Compare Output: "+salida+" With empty: "+"{\"observations\":[]}");
                if(salida.equals("{\"observations\":[]}")){
                    log.debug("Reference does not exists.");
                    httpClient.getConnectionManager().shutdown();
                    return false;
                }else{
                     log.debug("Reference exists.");
                    httpClient.getConnectionManager().shutdown();
                    return true;
                }

	  } catch (ClientProtocolException e) {
                //Server error continue as if does not exists
                log.error(e.toString());
		e.printStackTrace();
                
                return true;
	  } catch (IOException e) {
               //Server error continue as if does not exists
                log.error(e.toString());
		e.printStackTrace();
                return true;
          }
    }
    /**
     * Send Update to Sentilo Sensors
     * @param json Json to send.
     * @param url Url to send the json.
     * @param reference Reference of the vehicle.
     * @throws RuntimeException 
     */
    public boolean sendSentiloDelete(String url, String reference) throws RuntimeException{
        String json="{\"sensors\":[\""+reference+"-vehicle_orientation"+"\",\""+reference+"-vehicle_speed"+"\",\""+reference+"-vehicle_delay"+"\",\""+reference+"-vehicle_line"+"\",\""+reference+"-vehicle_direction"+"\"]}";
        try {
                //Initiate http connection
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPut putRequest = new HttpPut(url+"?method=delete");
                StringEntity params =new StringEntity(json,"UTF-8");
                params.setContentType("application/json");
		putRequest.addHeader("accept", "application/json");
                //Add identity key
                putRequest.addHeader("IDENTITY_KEY",this.identityKey);
                putRequest.setEntity(params);
		HttpResponse response = httpClient.execute(putRequest);
		String output;
                BufferedReader br = new BufferedReader(
                         new InputStreamReader((response.getEntity().getContent())));
                log.debug("Output from Server .... \n");
		while ((output = br.readLine()) != null) {
			log.debug(output);
		}
                //If response not ok.
		if (response.getStatusLine().getStatusCode() != 200) {
                    // If sensor does not exits, create it. and retry.
                    log.error("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
                    //If error store data in cache.
                    httpClient.getConnectionManager().shutdown();
                    return false;
		}

		httpClient.getConnectionManager().shutdown();
                return true;
	  } catch (ClientProtocolException e) {
                log.error(e.toString());
		e.printStackTrace();
                return false;

	  } catch (IOException e) {
                log.error(e.toString());
		e.printStackTrace();
                return false;
	  }
    }
    /**
     * Send Update to Sentilo Sensors
     * @param json Json to send.
     * @param url Url to send the json.
     * @param reference Reference of the vehicle.
     * @throws RuntimeException 
     */
    public boolean sendSentiloSensor(String json,String url, String reference) throws RuntimeException{
        try {
                //Initiate http connection
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost postRequest = new HttpPost(url);
                StringEntity params =new StringEntity(json,"UTF-8");
                params.setContentType("application/json");
		postRequest.addHeader("accept", "application/json");
                //Add identity key
                postRequest.addHeader("IDENTITY_KEY",this.identityKey);
                postRequest.setEntity(params);
		HttpResponse response = httpClient.execute(postRequest);
		String output;
                BufferedReader br = new BufferedReader(
                         new InputStreamReader((response.getEntity().getContent())));
                log.debug("Output from Server .... \n");
		while ((output = br.readLine()) != null) {
			log.debug(output);
		}
                //If response not ok.
		if (response.getStatusLine().getStatusCode() != 200) {
                    //Delete sensor if already exists.
                    //sendSentiloDelete(url, reference);
                    //sendSentiloSensor(json,url,reference);
                    // If sensor does not exits, create it. and retry.
                    log.error("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
                    httpClient.getConnectionManager().shutdown();
                    return false;
		}
		httpClient.getConnectionManager().shutdown();
                return true;
	  } catch (ClientProtocolException e) {
                log.error(e.toString());
		e.printStackTrace();
                return false;

	  } catch (IOException e) {
                log.error(e.toString());
		e.printStackTrace();
                return false;
	  }
    }
    /**
     * Send Update to Sentilo Sensors
     * Start point to Sentilo Retry
     * @param json Vehicles data
     * @throws RuntimeException Execution fail.
     */
    public void sendSentiloRetry(String json) throws RuntimeException{
       
        //Initiate cache Item Retry
        try {            
            for(int i=0;i<10;i++){
                log.debug("Attempt "+i+" to retry request:\n"+json);
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpPut putRequest = new HttpPut(this.sentilo);
                StringEntity params =new StringEntity(json,"UTF-8");
                params.setContentType("application/json");
                putRequest.addHeader("accept", "application/json");
                //Add identity key
                putRequest.addHeader("IDENTITY_KEY",this.identityKey);
                putRequest.setEntity(params);
                HttpResponse response = httpClient.execute(putRequest);
                String output;
                BufferedReader br = new BufferedReader(
                         new InputStreamReader((response.getEntity().getContent())));
                log.debug("Output from Server .... \n");
                while ((output = br.readLine()) != null) {
                        log.debug(output);
                }
                //if error status continue retries else remove key and break for loop.
                if (response.getStatusLine().getStatusCode() != 200) {
                        log.error("Failed : HTTP error code Retry "+i+": " + response.getStatusLine().getStatusCode());                                
                }else{
                    httpClient.getConnectionManager().shutdown();
                    break;
                }

                httpClient.getConnectionManager().shutdown();
                //If reach 10 retries remove key
                if(i==9){
                }else{
                //else pause 1 second
                    Thread.sleep(1000);
                }
                   
            }
           } catch (InterruptedException e) {
                log.error(e.toString());
		e.printStackTrace();

	  } catch (ClientProtocolException e) {
                log.error(e.toString());
		e.printStackTrace();

	  } catch (IOException e) {
                log.error(e.toString());
		e.printStackTrace();
	  }
    }
    /**
     * Change date format between SAE and Sentilo
     * @param timestamp SAE Timestamp
     * @return String Sentilo Timestamp
     */
    private String fixDate(String timestamp) {
           StringTokenizer st=new StringTokenizer(timestamp, "T");
           String date=st.nextToken();
           String time=st.nextToken();
           StringTokenizer stdate=new StringTokenizer(date, "-");     
           int year=Integer.parseInt(stdate.nextToken());
           int month=Integer.parseInt(stdate.nextToken());
           int day=Integer.parseInt(stdate.nextToken());
           StringTokenizer sttime=new StringTokenizer(time, ":");
           int hour=Integer.parseInt(sttime.nextToken());
           int minute=Integer.parseInt(sttime.nextToken());
           int second=Integer.parseInt(sttime.nextToken());
           Calendar cal = Calendar.getInstance();
           cal.setTimeInMillis(0);
           cal.set(year, month-1, day, hour, minute, second);
           Date dateObj = cal.getTime(); // get back a Date object
           DateFormat gmtFormat = new SimpleDateFormat("dd/MM/yyyy'T'HH:mm:ss");
           TimeZone gmtTime = TimeZone.getTimeZone("GMT");
           gmtFormat.setTimeZone(gmtTime);
           return gmtFormat.format(dateObj);
       
    }
 
    /**
     * Load File to String
     * @param path Filepath
     * @return String with the file contents
     * @throws IOException Exception if file does not exists
     */
    public static String readFile(String path) throws IOException 
    {
        StringBuffer fileData = new StringBuffer();
        BufferedReader reader = new BufferedReader(
                new FileReader(path));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }
    /**
     * Load references from file.
     * 
     * @param listFilePath String file path from load references
     */
    public void loadReferences(String listFilePath){
        //Try if list exists
        String reference="";
        File fr = new File(listFilePath);
        if(fr.exists() && !fr.isDirectory()) { 
            try{
                reference = readFile(listFilePath);   
            }catch(IOException e){
                log.error( e.toString() + "\n" );
                 e.printStackTrace();
            }
        }else{
            //System.out.println("List file ("+listFilePath+") don't exists");
            log.error( "List file ("+listFilePath+") don't exists" + "\n" );
            System.exit(1);
        }
                
        log.debug("References:"+reference);
        //Check csv is not empty.
        if(reference.equals("")){
            //System.out.println("Csv file is empty");
            log.error( "Csv file is empty" + "\n" );
            System.exit(0);
        }
    }
    /**
     * Refresh bus list from file.
     * 
     * @return List<String>
     */
    private List<String> refreshBusList(){
        List<String> busReferences = new ArrayList<String>(); 
        //Load references from file.
        try{
            this.reader = new CSVReader(new FileReader(this.listFilePath));
        }catch(FileNotFoundException fnfe){
            //System.out.println("List file ("+this.listFilePath+") does not exists.");
            log.error( "List file ("+this.listFilePath+") does not exists." + "\n" );
        }
        //Get the number of buses.
        try{ 
            String [] nextLine;
            //Ignore header
            this.reader.readNext();
            //Load csv data
            while ((nextLine = this.reader.readNext()) != null) {
                busReferences.add(nextLine[0]);
                busReferences.add(nextLine[1]);
            }
            this.reader.close();
            
            this.numBuses=busReferences.size()/2;
            this.reader.close();
        }catch(IOException e){
            log.error("Error Loading file: "+this.listFilePath);
                    e.printStackTrace();
        }
        log.debug("Sae Refresh: "+this.saeRefresh+(((int)this.saeRefresh)-1));
        this.numhilos = (int)(this.numBuses/(((int)this.saeRefresh)-1));
        this.remainder = this.numBuses % (this.saeRefresh-1);  
        log.debug("Number of Buses:"+this.numBuses+"\nNumber of theads:"+this.numhilos+ "\nRemainder: "+this.remainder+"\nbusreferences: "+busReferences.size());
        return busReferences;
    }
    /**
     * Create sensor
     * 
     * @param reference
     * @return String Json string with create sensor request.
     */
    public String createSensors(String reference){
            Sensor s_vehicle_orientation=new Sensor(reference,reference+"-vehicle_orientation","vehicle_orientation","°");
            Sensor s_vehicle_speed=new Sensor(reference,reference+"-vehicle_speed","vehicle_speed", "m/s");
            Sensor s_vehicle_delay=new Sensor(reference,reference+"-vehicle_delay","vehicle_delay","");
            Sensor s_vehicle_line=new Sensor(reference,reference+"-vehicle_line","vehicle_line","");
            Sensor s_vehicle_direction=new Sensor(reference,reference+"-vehicle_direction","vehicle_direction","");
            //Add sensors to List
            Sensors sensors=new Sensors();
            sensors.addSensor(s_vehicle_orientation);
            sensors.addSensor(s_vehicle_speed);
            sensors.addSensor(s_vehicle_delay);
            sensors.addSensor(s_vehicle_line);
            sensors.addSensor(s_vehicle_direction); 
            //Jsonize Sensors
            Gson gson = new Gson();
            String json = gson.toJson(sensors);
            return json;
            
    }
}
