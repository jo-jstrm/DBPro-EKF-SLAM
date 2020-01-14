package edu.tuberlin.dbpro.ws19.ekfslam.mark;

import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.security.Key;
import java.util.Arrays;

public class StreamingJobEKFMark {
    public static void main(String[] args) throws Exception {
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStream<KeyedDataPoint> fullInputData = env.readTextFile("src/main/resources/GPS_DRS_LASER")
                .map(new ParseData());

        env.execute("EKF-Mark");
    }

    private static class ParseData extends RichMapFunction<String, KeyedDataPoint> {
        private static final long serialVersionUID = 1L;


        @Override
        public KeyedDataPoint<Tuple4> map(String record) {
            //String rawData = record.substring(1, record.length() - 1);
            String[] data = record.split(",");

            String key = data[0];
            long timestamp = Math.round(Double.valueOf(data[1]));

            switch(data[0]){
                case "odo" : return parseOdo(data);
                case "gps" : return parseGPS(data);
                case "laser" : return parseLaser(data);
            }
            /*
            // the data look like this...
            // timestamp, latitude, longitude

            //get timestamp, lat and lon from data
            //store lat, lan in Tuple2<Double, Double>
            long timestamp = Math.round(Double.valueOf(data[0]));
            Double xInc = Double.valueOf(data[1]);
            Double yInc = Double.valueOf(data[2]);
            Double phiInc = Double.valueOf(data[3]);

            Double[] laserArr = new Double[data.length-4];

            for(int i=4; i<data.length;i++){
                String s = data[i];
                if(s.contains("[")){ s=s.substring(2);}
                if(s.contains("]")){ s=s.substring(0,s.length()-2);}

                laserArr[i-4] = Double.valueOf(s);
            }
            */

            return KeyedDataPoint();
        }

        private Tuple2 parseOdo(String[] data){
            Double speed = Double.valueOf(data[2]);
            Double steering = Double.valueOf(data[3]);

            return new Tuple2<Double,Double>(speed, steering);
        }

        private Tuple2 parseGPS(String[] data){
            Double x = Double.valueOf(data[2]);
            Double y = Double.valueOf(data[3]);

            return new Tuple2<Double,Double>(x,y);
        }

        private Tuple parseLaser(String[] data){
            double[] laserArr = Arrays.stream(data[2].split(","))
                    .mapToDouble(Double::parseDouble)
                    .toArray();

            return new Tuple2(laserArr,0);
        }
    }
}
