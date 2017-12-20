/**
 * Class Observations.
 * @author: Miguel Angel Pescador <miguelangelps@prometeoinnova.com>
 * @package com.geminisrti.sentiloconnect
 * @copyright 20/04/17 Geminis RTI.
 */
package com.geminisrti.sentiloconnect;

/**
 * Observatios Object
 * 
 * This object is intended to be part of the sentilo Rest Structure.
 * @author mapescador
 */
public class Observations {
   private String value;
   private String timestamp;
   private String location;
   /**
    * Base Constructor
    */
   public Observations(){}
   /**
    * Contructor with timestamp and location data.
    * 
    * Used to null value data.
    * 
    * @param timestamp Timestamp of sensor data
    * @param location  Location coordinates of sensor
    */
   public Observations(String timestamp, String location){
        this.timestamp = timestamp;
        this.location = location;
        this.value = "";
   }
   /**
    * Contructor with full load data .
    * @param timestamp Timestamp of sensor data
    * @param location  Location coordinates of sensor
    * @param value     Value sended by the sensor
    */
   public Observations(String timestamp, String location, String value){
       this.timestamp = timestamp;
       this.location = location;
       this.value = value;
   }
   /**
    * Get Value getter
    * @return String
    */
    public String getValue() {
        return value;
    }
    /**
     * Set Value setter
     * @param value String
     */
    public void setValue(String value) {
        this.value = value;
    }
    /**
     * Get Timestamp getter
     * @return  String
     */
    public String getTimestamp() {
        return timestamp;
    }
    /**
     * Set Timestamp setter
     * @param timestamp String with timestamp
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    /**
     * Get Location getter
     * @return String
     */
    public String getLocation() {
        return location;
    }
    /**
     * Set Location setter
     * 
     * @param location String with location
     */
    public void setLocation(String location) {
        this.location = location;
    }
}
