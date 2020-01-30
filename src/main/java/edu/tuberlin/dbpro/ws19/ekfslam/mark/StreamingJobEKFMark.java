package edu.tuberlin.dbpro.ws19.ekfslam.mark;

import cern.colt.matrix.DoubleMatrix1D;
import edu.tuberlin.dbpro.ws19.ekfslam.StreamingJobExtendedKalmanFilter;
import edu.tuberlin.dbpro.ws19.ekfslam.StreamingJobMilestone;
import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import edu.tuberlin.dbpro.ws19.ekfslam.functions.ExtendedKalmanFilter;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.*;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.runtime.state.Keyed;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.io.FileReader;
import java.security.Key;
import java.util.ArrayList;

public class StreamingJobEKFMark {
    public static void main(String[] args) throws Exception {
        // set up the streaming execution environment
        long timer1 = System.currentTimeMillis();
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(4);

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStream<String> dataStream = env.readTextFile("src/main/resources/ODO_GPS_KEYED_1.csv")
                .setParallelism(1);
                //.map(new ParseData())
        //KeyedDataPoint<latitude>
        /*DataStream<KeyedDataPoint> dataStream = env.readTextFile("src/main/resources/01_HOPEFULLY_FINAL-1")
                .map(new ParseData())
                .keyBy("key")
                .flatMap(new MoveFunction());*/

        //other dataStreams
        DataStream<String> stream2 = env.readTextFile("src/main/resources/ODO_GPS_KEYED_2.csv")
                .setParallelism(1);
                //.map(new ParseData());

        DataStream<String> stream3 = env.readTextFile("src/main/resources/ODO_GPS_KEYED_3_REVERSE.csv")
                .setParallelism(1);

        DataStream<String> fullStream = dataStream
                .union(stream2, stream3);
                //.keyBy("key");

        DataStream<KeyedDataPoint> filteredStream = dataStream
                .map(new ParseData())
                .setParallelism(1)
                .keyBy("key")
                //.window(TumblingEventTimeWindows.of(Time.milliseconds(200)))
                //.apply()
                .flatMap(new ExtendedKalmanFilter())
                .setParallelism(4);
                ;


        filteredStream
                .map(new getTuple3withTime())
                .setParallelism(1)
                .writeAsCsv("src/main/resources/ekfSingleCar.csv", FileSystem.WriteMode.OVERWRITE, "\n", ",")
                .setParallelism(1);

        env.execute("EKF-Mark");
        long timer2 = System.currentTimeMillis();
        System.out.println(timer2-timer1);
    }

    private static class ParseData extends RichMapFunction<String, KeyedDataPoint> {
        private static final long serialVersionUID = 1L;


        @Override
        public KeyedDataPoint<Tuple3> map(String record) {
            //String rawData = record.substring(1, record.length() - 1)
            record = record.replaceAll(";", ",");
            String[] data = record.split(",");

            String key = data[0];
            long timestamp = Long.parseLong(data[1]);
            //long timestamp = Math.round(Double.valueOf(data[1])*1000);
            Tuple3 parsedData = null;

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

            return new KeyedDataPoint<Tuple3>("vehicle"+data[4], timestamp, parsedData);
        }

        private Tuple3 parseOdo(String[] data) {
            Double speed = Double.valueOf(data[3]);
            Double steering = Double.valueOf(data[2]);
            return new Tuple3(speed, steering, "odo");
        }

        private Tuple3 parseGPS(String[] data) {
            Double x_coord = Double.valueOf(data[3]);
            Double y_coord = Double.valueOf(data[2]);
            return new Tuple3(x_coord, y_coord, "gps");
        }

        private Tuple3 parseLaser(String[] data) {
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
            return new Tuple3(laserArr, 0.0, "laser");
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

    private static class getTuple3withTime extends RichMapFunction<KeyedDataPoint, Tuple4<String, Long, Double, Double>> {

        @Override
        public Tuple4<String, Long, Double, Double> map(KeyedDataPoint value) throws Exception {

            KeyedDataPoint<DoubleMatrix1D> val = (KeyedDataPoint<DoubleMatrix1D>) value;
            String key = val.getKey();
            Long time = val.getTimeStampMs();
            Double x = val.getValue().get(0);
            Double y = val.getValue().get(1);

            return Tuple4.of(key, time, x, y);
        }
    }

    /*
    Just for evaluating. Returns a Tuple6 for Evaluating Prediction and Update step
    Fields:
    VehicleKey | predictedX | predictedY | updatedX | updatedY | euclideanDistance
     */
    public static class getEvaluationTuple extends RichMapFunction<KeyedDataPoint, Tuple6<String, Double, Double, Double, Double, Double>>{

        @Override
        public Tuple6<String, Double, Double, Double, Double, Double> map(KeyedDataPoint value) throws Exception {
            KeyedDataPoint<Tuple2<DoubleMatrix1D, DoubleMatrix1D>> val = (KeyedDataPoint<Tuple2<DoubleMatrix1D, DoubleMatrix1D>>) value;
            String key = val.getKey();
            DoubleMatrix1D predicted = val.getValue().f0;
            DoubleMatrix1D updated = val.getValue().f1;

            Double x_pred = predicted.get(0);
            Double y_pred = predicted.get(1);
            Double x_up = updated.get(0);
            Double y_up = updated.get(1);

            Double distance =Math.sqrt(Math.pow(x_pred-x_up,2)+Math.pow(y_pred-y_up,2));

            return Tuple6.of(key, x_pred, y_pred, x_up, y_up, distance);
        }
    }
}
