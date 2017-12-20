/**
 * Class Sensors.
 * @author: Miguel Angel Pescador <miguelangelps@prometeoinnova.com>
 * @package com.geminisrti.sentiloconnect
 * @copyright 20/04/17 Geminis RTI.
 */
package com.geminisrti.sentiloconnect;

import java.util.ArrayList;
import java.util.List;

/**
 * Class Sensor
 * This object is intended to be generate the sentilo Rest Structure.
 * @author mapescador
 */
public class Sensors {
    private List<Sensor> sensors;
    /**
     * Base Constructor.
     * Instantiuate an ArrayList for Sensor Objects
     */
    public Sensors(){
        this.sensors=new ArrayList<Sensor>();
    }
    /**
     * Add a Sensor data
     * @param sensor Sensor Object
     * @see Sensor
     */    
    public void addSensor(Sensor sensor){
        sensors.add(sensor);
    }
    /**
     * Getter Sensors Object
     * Return a list object with all the sensors in the current object.
     * @return List
     * @see List
     */
    public List<Sensor> getSensors() {
        return sensors;
    }
    /**
     * Setter for Sensors
     * @param sensors Sensor collection in List object
     * @see List
     * @see Sensor
     */
    public void setSensors(List<Sensor> sensors) {
        this.sensors = sensors;
    }
}
