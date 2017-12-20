/**
 * Class SentiloConnect.
 * @author: Miguel Angel Pescador <miguelangelps@prometeoinnova.com>
 * @package com.geminisrti.sentiloconnect
 * @copyright 20/04/17 Geminis RTI.
 */
package com.geminisrti.sentiloconnect;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
/**
 * Sentilo Connect: Main class and launcher.
 * @author mapescador
 */
public class SentiloConnect {
    private static final Logger log = Logger.getLogger(SentiloConnect.class);
    private static int debug=1;
    /**
     * Starting point for the SAE - SENTILO Connect
     * @param args Command line params
     */
    @SuppressWarnings("unused")
	public static void main(String args[])  {
        boolean createSensors=false;
        boolean deleteSensors=false;
        if (args.length == 0)
        {
            log.info("No console options: Set to normal behaviour.");
        }else{
         // Set some initial variables
         for(String argument: args)
         {
             if(argument.equals("-deleteSensors"))
             {
                 createSensors=false;
                 deleteSensors=true;
             }
             if(argument.equals("-createSensors"))
             {
                 createSensors=true;
                 deleteSensors=false;
             }
             if(argument.equals("-version"))
             {
                    log.info("Sentilo Connect version 1.0.2\n "
                            + "Use:\n "
                            + "java -jar SentiloConnect.jar -version -createSensors -deleteSensors ");
                    System.exit(0);
             }
         }
        }
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
            File loggerpath = new File("/var/log");
            RollingFileAppender fileAppender=null;
            if(loggerpath.canWrite()) {
                //Define file appender with layout and output log file name
                fileAppender = new RollingFileAppender(layout, "/var/log/sentilolog.log");
                fileAppender.setMaxBackupIndex(4);
                fileAppender.setMaxFileSize("1MB");
            }else{
                System.out.println("/var/log/sentiloconnect.log not writable. Using tmp appender /tmp/sentilolog.log");
                fileAppender = new RollingFileAppender(layout, "/tmp/sentilolog.log");
                fileAppender.setMaxBackupIndex(4);
                fileAppender.setMaxFileSize("10MB");
            }
	    	//Add the appender to root logger
	    	rootLogger.addAppender(fileAppender);
    	}
    	catch (IOException e)
    	{
    		log.error("Failed to add appender in /var/log !!");
    		
    		// Debug
                if (debug == 1){
                        String exceptionMessage = e.getMessage();
                        log.debug( exceptionMessage + "\n" );
                        log.debug( "Got an Exception: " + e.getClass().getName() + ": " + e.getMessage() + "\n" );
                }
            
              	// Exit terminate application
	    	// -------------------------
	    	Runtime.getRuntime().exit(0);
	    	
           
    	}
    	// -------------------
        //Try if config exists
        String configuration="";
        File f = new File("/etc/sentilo/preferences.conf");
        if(f.exists() && !f.isDirectory()) { 
            try{
                configuration = readFile("/etc/sentilo/preferences.properties");              
            }catch(IOException e){
                log.error( e.toString() + "\n" );
                e.printStackTrace();
            }
        }else{
              //System.out.println("Configuration file (/etc/sentilo/preferences.properties) does not exists.");
                log.error( "Configuration file (/etc/sentilo/preferences.properties) does not exists." + "\n" );
        }
        //Check csv is not empty.
        if(configuration.equals("")){
            //System.out.println("Configuration file is empty");
             log.error( "Configuration file is empty" + "\n" );
            System.exit(0);
        }
        
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
        String soapServer=defaultProps.getProperty("SOAPServer");
        String restServer=defaultProps.getProperty("RESTServerData");
        String sentiloSensor=defaultProps.getProperty("RESTServerSensor");
        String identityKey=defaultProps.getProperty("IDENTITY_KEY");
        String listFilePath=defaultProps.getProperty("ListBusFilePath");
        

        long cachettl=Integer.parseInt(defaultProps.getProperty("CacheTTL"));
        int cacherefresh=Integer.parseInt(defaultProps.getProperty("CacheRefresh"));
        int cachemaxitems=Integer.parseInt(defaultProps.getProperty("CacheMaxItems"));
        int saeRefresh=Integer.parseInt(defaultProps.getProperty("SAERefreshRate"));
        log.debug("SOAPServer:"+soapServer);
        log.debug("RESTServerData:"+restServer);
        log.debug("RESTServerSensor:"+sentiloSensor);
        log.debug("IDENTITY_KEY:"+identityKey);
        log.debug("ListBusFilePath:"+listFilePath);
        log.debug("CacheTTL:"+cachettl);
        log.debug("CacheRefresh:"+cacherefresh);
        log.debug("CacheMaxItems:"+cachemaxitems);
        log.debug("SAERefreshRate:"+saeRefresh);
        
        if(createSensors){
            Connect sentilo=new Connect(sentiloSensor, identityKey,listFilePath, 1);
            System.exit(0);
        }
        if(deleteSensors){
           Connect sentilo=new Connect(sentiloSensor, identityKey,listFilePath, 0);
           System.exit(0);
        }
       
        //Launch Sentilo Connect
        Connect sentilo=new Connect(soapServer,
                restServer,
                sentiloSensor,
                identityKey,
                cachettl,
                cacherefresh,
                cachemaxitems, 
                listFilePath, 
                saeRefresh);
    }
    
     /**
	 * Compute the absolute file path to the jar file.
	 * The framework is based on http://stackoverflow.com/a/12733172/1614775
	 * But that gets it right for only one of the four cases.
	 * 
	 * @param aclass A class residing in the required jar.
	 * 
	 * @return A File object for the directory in which the jar file resides.
	 * During testing with NetBeans, the result is ./build/classes/,
	 * which is the directory containing what will be in the jar.
	 */
	public static File getJarDir(@SuppressWarnings("rawtypes") Class aclass) {
	    URL url;
	    String extURL;      //  url.toExternalForm();

	    // get an url
	    try {
	        url = aclass.getProtectionDomain().getCodeSource().getLocation();
	          // url is in one of two forms
	          //        ./build/classes/   NetBeans test
	          //        jardir/JarName.jar  froma jar
	    } catch (SecurityException ex) {
	        url = aclass.getResource(aclass.getSimpleName() + ".class");
	        // url is in one of two forms, both ending "/com/physpics/tools/ui/PropNode.class"
	        //          file:/U:/Fred/java/Tools/UI/build/classes
	        //          jar:file:/U:/Fred/java/Tools/UI/dist/UI.jar!
	    }

	    // convert to external form
	    extURL = url.toExternalForm();

	    // prune for various cases
	    if (extURL.endsWith(".jar"))   // from getCodeSource
	        extURL = extURL.substring(0, extURL.lastIndexOf("/"));
	    else {  // from getResource
	        String suffix = "/"+(aclass.getName()).replace(".", "/")+".class";
	        extURL = extURL.replace(suffix, "");
	        if (extURL.startsWith("jar:") && extURL.endsWith(".jar!"))
	            extURL = extURL.substring(4, extURL.lastIndexOf("/"));
	    }

	    // convert back to url
	    try {
	        url = new URL(extURL);
	    } catch (MalformedURLException mux) {
	        // leave url unchanged; probably does not happen
	    }

	    // convert url to File
	    try {
	        return new File(url.toURI());
	    } catch(URISyntaxException ex) {
	        return new File(url.getPath());
	    }
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
}
