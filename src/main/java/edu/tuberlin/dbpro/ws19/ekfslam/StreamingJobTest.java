package edu.tuberlin.dbpro.ws19.ekfslam;

import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import edu.tuberlin.dbpro.ws19.ekfslam.functions.MoveFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;

public class StreamingJobTest {
    public static void main(String[] args) throws Exception {
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        @SuppressWarnings({"rawtypes", "serial"})

        InfluxDB influxDB = InfluxDBFactory.connect("http://localhost:8086", "admin", "admin");
        String dbName = "DBProTest";
        influxDB.query(new Query("CREATE DATABASE " + dbName, dbName));
        influxDB.setDatabase(dbName);

        //KeyedDataPoint<latitude>
        DataStream<KeyedDataPoint> fullData = env.readTextFile("src/main/resources/time_xIncr_yIncr_phi_odo.csv")
                .map(new StreamingJobTest.ParseData())
                .keyBy("key")
                .flatMap(new MoveFunction());

        fullData.map(new StreamingJobTest.getTuple3())
                .writeAsCsv("src/main/resources/testcsv.csv", FileSystem.WriteMode.OVERWRITE);




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

    private static class ParseData extends RichMapFunction<String, KeyedDataPoint> {
        private static final long serialVersionUID = 1L;


        @Override
        public KeyedDataPoint<Tuple3> map(String record) {
            //String rawData = record.substring(1, record.length() - 1);
            String[] data = record.split(",");

            // the data look like this...
            // timestamp, latitude, longitude

            //get timestamp, lat and lon from data
            //store lat, lan in Tuple2<Double, Double>
            Long timestamp = (long) Math.round(Double.valueOf(data[0]));
            Double lat = Double.valueOf(data[1]);
            Double lon = Double.valueOf(data[2]);
            Double phi = Double.valueOf(data[3]);
            System.out.println(timestamp);

            //String[] laserdata = data[3].split(",");
            //ArrayList<Double> values = new ArrayList<Double>();

            /*for(String laservalue : laserdata){
                if(laservalue.contains(" ")){
                    laservalue.replace(" ","");
                }

                if(laservalue.contains("[")){
                    laservalue.replace("[", "");
                }

                if(laservalue.contains("]")){
                    laservalue.replace("]", "");
                }

                values.add(Double.valueOf(laservalue));
            }

            Tuple4 tuple = new Tuple4(lat, lon, phi, values);
            return new KeyedDataPoint<Tuple4>("fulldata",timestamp, tuple);*/
            Tuple3 tuple = new Tuple3(lat,lon,phi);
            return new KeyedDataPoint<Tuple3>("odo",timestamp,tuple);
        }
    }

    private static class getTuple3 extends RichMapFunction<KeyedDataPoint, Tuple3<Long, Double, Double>>{

        @Override
        public Tuple3<Long, Double, Double> map(KeyedDataPoint value) throws Exception {
            Tuple2<Double, Double> val = (Tuple2<Double, Double>) value.getValue();
            long time = value.getTimeStampMs();
            return new Tuple3<Long, Double, Double>(time, val.f0, val.f1);
        }
    }

}
