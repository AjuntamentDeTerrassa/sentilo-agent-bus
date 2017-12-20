/**
 * Class Sensor.
 * @author: Miguel Angel Pescador <miguelangelps@prometeoinnova.com>
 * @package com.geminisrti.sentiloconnect
 * @copyright 20/04/17 Geminis RTI.
 */
package com.geminisrti.sentiloconnect;

import java.util.ArrayList;
import java.util.List;

/**
 * Class Sensor
 * This object is intended to be part of the sentilo Rest Structure.
 * @author mapescador
 */
public class Sensor {
    private String sensor;
    private List<Observations> observations;
    private String type;
    private String component;
    private String componentType;
    private String unit;

    /**
     * Base Constructor
     */
    public Sensor(){
        
    }
    /**
     * Constructor with Sensor Name
     * @param sensor String
     */
    public Sensor(String sensor) {
        this.sensor = sensor;
        this.observations=new ArrayList<Observations>();
    }
     /**
     * Constructor with Sensor Name and type
     * Used to generate adding sensor components
     * @param name String
     * @param sensor String
     * @param type String
     */
    public Sensor(String name,String sensor,String type, String unit) {
        this.sensor = sensor;
        this.type=type;
        this.component=name;       
        this.componentType="bus_vehicle";
        this.unit=unit;
    }
    /**
     * Constructor with name of sensor and Observations data.
     * 
     * @param sensor Observations
     * @param observations Observations
     * @see Observations
     */
    public Sensor(String sensor,Observations observations ) {
        this.sensor = sensor;
        this.observations=new ArrayList<Observations>();
        this.observations.add(observations);
    }
    /**
     * Getter sensor name
     * @return String
     */
    public String getSensor() {
        return sensor;
    }
    /**
     * Set Sensor Name
     * 
     * @param sensor String with sensor name
     */
    public void setSensor(String sensor) {
        this.sensor = sensor;
    }
    /**
     * Get Observations collection
     * @return List observation collections
     * @see List
     */
    public List<Observations> getObservations() {
        return observations;
    }
    /**
     * Add Observations object
     * @param observations Observations object
     * @see Observations
     */
    public void addObservations(Observations observations) {
        this.observations.add(observations);
    }
    /**
     * Setter observations.
     * 
     * @param observations Observations List
     * @see Observations
     * @see List
     */
    public void setObservations(List<Observations> observations) {
        this.observations = observations;
    }
    /**
     * Get Type of sensor.
     * 
     * @return String type
     */
   public String getType() {
        return type;
    }
   /**
    * Set Type of sensor.
    * @param type String type
    */
    public void setType(String type) {
        this.type = type;
    }
    /**
     * Get Component Identifier
     * 
     * @return String component
     */
    public String getComponent() {
        return component;
    }
    /**
     * Set Component Identifier
     * 
     * @param component String component
     */
    public void setComponent(String component) {
        this.component = component;
    }
    /**
     * Get Component Type
     * 
     * @return String componentType
     */
    public String getComponentType() {
        return componentType;
    }
    /**
     * Set Component Type
     * 
     * @param componentType String componentType
     */
    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }
    /**
     * Get Unit
     * @return String unit
     */
    public String getUnit() {
        return unit;
    }
    /**
     * Set Unit
     * @param unit Measure unit
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }
}
