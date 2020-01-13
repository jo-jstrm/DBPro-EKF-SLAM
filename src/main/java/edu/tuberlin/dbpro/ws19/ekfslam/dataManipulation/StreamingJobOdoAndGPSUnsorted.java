package edu.tuberlin.dbpro.ws19.ekfslam.dataManipulation;

import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoMapFunction;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;

public class StreamingJobOdoAndGPSUnsorted {
    public static void main(String[] args) throws Exception {
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        @SuppressWarnings({"rawtypes", "serial"})

        InfluxDB influxDB = InfluxDBFactory.connect("http://localhost:8086", "admin", "admin");
        String dbName = "DBProTest";
        influxDB.query(new Query("CREATE DATABASE " + dbName, dbName));
        influxDB.setDatabase(dbName);

        //KeyedDataPoint<latitude>
        DataStream<KeyedDataPoint> fullData = env.readTextFile("src/main/resources/time_steering_speed_aa3_dr.csv")
                .connect(env.readTextFile("src/main/resources/time_lat_lon_aa3_gpsx.csv"))
                .map(new Parse2Data());

        fullData.map(new StreamingJobOdoAndGPSUnsorted.getTuple4())
                .writeAsCsv("src/main/resources/gps_and_odo.csv", FileSystem.WriteMode.OVERWRITE);


        //fullData.addSink(new InfluxDBSinkGPS<>("DBProTest","testmove"));

        /*
         * Here, you can start creating your execution plan for Flink.
         *
         * Start with getting some data from the environment, like
         * 	env.readTextFile(textPath);
         *
         * then, transform the resulting DataStream<String> using operations
         * like
         * 	.filter()
         * 	.flatMap()
         * 	.join()
         * 	.coGroup()
         *
         * and many more.
         * Have a look at the programming guide for the Java API:
         *
         * http://flink.apache.org/docs/latest/apis/streaming/index.html
         *
         */

        // execute program
        env.execute("EKF-SLAM");
    }

    private static class Parse2Data implements CoMapFunction<String, String, KeyedDataPoint> {
        private static final long serialVersionUID = 1L;

        @Override
            public KeyedDataPoint map1(String value) throws Exception {
            //parse odo stream
            String[] data = value.split(",");

            // the data look like this...
            // timestamp, steering, speed

            //get timestamp, steer and speed from data
            //store lat, lan in Tuple2<Double, Double>
            Long timestamp = (long) Math.round(Double.valueOf(data[0]));
            Double steer = Double.valueOf(data[1]);
            Double speed = Double.valueOf(data[2]);

            Tuple2 tuple = new Tuple2(steer,speed);
            return new KeyedDataPoint<Tuple2>("odo",timestamp,tuple);
        }

        @Override
        public KeyedDataPoint map2(String value) throws Exception {
            //parse gpsStream
            String[] data = value.split(",");

            // the data look like this...
            // timestamp, steering, speed

            //get timestamp, steer and speed from data
            //store lat, lan in Tuple2<Double, Double>
            Long timestamp = (long) Math.round(Double.valueOf(data[0]));
            Double lat = Double.valueOf(data[1]);
            Double lon = Double.valueOf(data[2]);

            Tuple2 tuple = new Tuple2(lat,lon);
            return new KeyedDataPoint<Tuple2>("gps",timestamp,tuple);
        }
    }

    private static class getTuple4 extends RichMapFunction<KeyedDataPoint, Tuple4<String,Long, Double, Double>>{

        @Override
        public Tuple4<String,Long, Double, Double> map(KeyedDataPoint value) throws Exception {
            Tuple2<Double, Double> val = (Tuple2<Double, Double>) value.getValue();
            long time = value.getTimeStampMs();
            return new Tuple4<String,Long, Double, Double>(value.getKey(),time, val.f0, val.f1);
        }
    }

}
