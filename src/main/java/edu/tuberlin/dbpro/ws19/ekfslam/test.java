package edu.tuberlin.dbpro.ws19.ekfslam;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.api.common.functions.MapFunction;

public class test {
    public static void main (String[] args){
        System.out.print("Hello there2");

        final ParameterTool params = ParameterTool.fromArgs(args);

        final StreamExecutionEnvironment env = org.apache.flink.streaming.api.environment.StreamExecutionEnvironment.getExecutionEnvironment();

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        @SuppressWarnings({"rawtypes", "serial"})

        //         Tuple3<key, time stamp, measurement>
                DataStream<Tuple3<Integer, Double, Double>> gpsData = env.readTextFile("./src/main/resources/time_lan_lat.csv")
                .map(new ParseData());
        gpsData.print();


    }

    private static class ParseData extends RichMapFunction<String, Tuple3<Integer, Double, Double>> {
        private static final long serialVersionUID = 1L;


        @Override
        public Tuple3<Integer, Double, Double> map(String record) {
            //String rawData = record.substring(1, record.length() - 1);
            String rawData = record;
            String[] data = rawData.split(",");

            // the data look like this...
            // time,lat,long,timestamp
            //20967,-41.71421779374552,-67.64927093982358
            //21968,-41.668097161603534,-67.73098493758533
            //22168,-41.82121765817564,-67.61997724236394                                                         time stamp in ms
            return new Tuple3<Integer, Double, Double>(Integer.valueOf(data[0]), Double.valueOf(data[1]), Double.valueOf(data[2]));

        }
    }
}
