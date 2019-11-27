/**
 * (c) dataartisans
 */

package edu.tuberlin.dbpro.ws19.ekfslam.sinks;

import edu.tuberlin.dbpro.ws19.ekfslam.data.DataPoint;
import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import java.util.concurrent.TimeUnit;

public class InfluxDBSink<T extends DataPoint<? extends Number>> extends RichSinkFunction<T> {

    private transient InfluxDB influxDB = null;
    private static String dataBaseName;
    private static String fieldName = "value";
    private String measurement;

    /**
     * Provide Database name and measurement (series) name
     * @param dbName
     * @param measurement
     */
    public InfluxDBSink(String dbName, String measurement){
        this.dataBaseName = dbName;
        this.measurement = measurement;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        influxDB = InfluxDBFactory.connect("http://localhost:8086", "admin", "admin");
        influxDB.createDatabase(dataBaseName);
        influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws Exception {
        super.close();
    }

    @Override
    public void invoke(T dataPoint, Context context) throws Exception {
        Point.Builder builder = Point.measurement(measurement)
                .time(dataPoint.getTimeStampMs(), TimeUnit.MILLISECONDS)
                .addField(fieldName, dataPoint.getValue());

        if(dataPoint instanceof KeyedDataPoint){
            builder.tag("key", ((KeyedDataPoint) dataPoint).getKey());
        }

        Point p = builder.build();

        influxDB.write(dataBaseName, "autogen", p);
    }
}