/**
 * (c) datartisans
 */

package edu.tuberlin.dbpro.ws19.ekfslam.data;

import java.util.Date;
import java.text.SimpleDateFormat;

public class KeyedDataPoint<T> extends DataPoint<T> {

    private String key;

    public KeyedDataPoint(){
        super();
        this.key = null;
    }

    public KeyedDataPoint(String key, long timeStampMs, T value) {
        super(timeStampMs, value);
        this.key = key;
    }

    @Override
    public String toString() {
        Date date = new Date(getTimeStampMs());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return  sdf.format(date) + "," + getValue();
        // short in tm
        // return getTimeStampMs() + "," + getKey() + "," + getValue();
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public <R> KeyedDataPoint<R> withNewValue(R newValue){
        return new KeyedDataPoint<>(this.getKey(), this.getTimeStampMs(), newValue);
    }
}