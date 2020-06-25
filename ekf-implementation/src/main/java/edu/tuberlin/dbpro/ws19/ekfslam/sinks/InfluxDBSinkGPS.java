/**
 * (c) dataartisans
 */

package edu.tuberlin.dbpro.ws19.ekfslam.sinks;

import edu.tuberlin.dbpro.ws19.ekfslam.data.DataPoint;
import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

import java.util.concurrent.TimeUnit;

public class InfluxDBSinkGPS<T extends DataPoint<? extends Number>> extends RichSinkFunction<T> {

    private transient InfluxDB influxDB = null;
    private static String dataBaseName;
    private static String field0Name = "latitude";
    private static String field1Name = "longitude";
    private String measurement;

    /**
     * Provide Database name and measurement (series) name
     * @param dbName
     * @param measurement
     */
    public InfluxDBSinkGPS(String dbName, String measurement){
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
    public void invoke(DataPoint dataPoint, Context context) throws Exception {
        Tuple2 value =  (Tuple2) dataPoint.getValue();
        Point.Builder builder = Point.measurement(measurement)
                .time(dataPoint.getTimeStampMs(), TimeUnit.MILLISECONDS)
                .addField(field0Name, (Double) value.f0)
                .addField(field1Name, (Double) value.f1);

        if(dataPoint instanceof KeyedDataPoint){
            builder.tag("key", ((KeyedDataPoint) dataPoint).getKey());

        }

        Point p = builder.build();

        influxDB.write(dataBaseName, "autogen", p);
    }
}