package edu.tuberlin.dbpro.ws19.ekfslam.mark;

import cern.colt.matrix.DoubleMatrix1D;
import edu.tuberlin.dbpro.ws19.ekfslam.StreamingJobMilestone;
import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import edu.tuberlin.dbpro.ws19.ekfslam.functions.ExtendedKalmanFilter;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.security.Key;
import java.util.ArrayList;

public class StreamingJobEKFMark {
    public static void main(String[] args) throws Exception {
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStream<KeyedDataPoint> fullData = env.readTextFile("src/main/resources/GPS_DRS_LASER.csv")
                .map(new ParseData())
                .keyBy("key")
                .flatMap(new ExtendedKalmanFilter());

        fullData.map(new getTuple3())
                .writeAsCsv("src/main/resources/hopefullyItDidntFuckUp.csv", FileSystem.WriteMode.OVERWRITE, "\n", ";");

        env.execute("EKF-Mark");
    }

    private static class ParseData extends RichMapFunction<String, KeyedDataPoint> {
        private static final long serialVersionUID = 1L;


        @Override
        public KeyedDataPoint<Tuple2> map(String record) {
            //String rawData = record.substring(1, record.length() - 1);
            String[] data = record.split(",");

            String key = data[0];
            long timestamp = Math.round(Double.valueOf(data[1])*1000);
            Tuple2 parsedData = new Tuple2(0.0, 0.0);

            switch (key) {
                case "odo":
                    parsedData = parseOdo(data);
                    break;
                case "gps":
                    parsedData = parseGPS(data);
                    break;
                case "laser":
                    parsedData = parseLaser(data);
                    break;
            }

            return new KeyedDataPoint<Tuple2>(key, timestamp, parsedData);
        }

        private Tuple2 parseOdo(String[] data) {
            Double speed = Double.valueOf(data[2]);
            Double steering = Double.valueOf(data[3]);

            return new Tuple2<Double, Double>(speed, steering);
        }

        private Tuple2 parseGPS(String[] data) {
            Double x_coord = Double.valueOf(data[2]);
            Double y_coord = Double.valueOf(data[3]);

            return new Tuple2(x_coord, y_coord);
        }

        private Tuple2 parseLaser(String[] data) {
            //first two values are not part of laser data
            Double[] laserArr = new Double[data.length - 2];

            //parse laser array as Double and erase '"[' and ']"'
            for (int i = 2; i < data.length; i++) {
                String s = data[i];
                if (s.contains("[")) {
                    s = s.substring(2);
                }
                if (s.contains("]")) {
                    s = s.substring(0, s.length() - 2);
                }

                laserArr[i - 2] = Double.valueOf(s);
            }

            //second return value is dummy-value
            return new Tuple2(laserArr, 0.0);
        }
    }

    private static class getTuple3 extends RichMapFunction<KeyedDataPoint, Tuple3<String, Double, Double>> {

        @Override
        public Tuple3<String, Double, Double> map(KeyedDataPoint value) throws Exception {

            KeyedDataPoint<DoubleMatrix1D> val = (KeyedDataPoint<DoubleMatrix1D>) value;
            String key = val.getKey();
            Double x = val.getValue().get(0);
            Double y = val.getValue().get(1);

            return Tuple3.of(key, x, y);
        }
    }
}
