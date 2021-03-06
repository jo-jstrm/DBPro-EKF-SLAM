/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.tuberlin.dbpro.ws19.ekfslam.legacyJobs;

import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import edu.tuberlin.dbpro.ws19.ekfslam.functions.ExtendedKalmanFilter;
import edu.tuberlin.dbpro.ws19.ekfslam.functions.MoveFunction;
import edu.tuberlin.dbpro.ws19.ekfslam.sinks.InfluxDBSink;
import edu.tuberlin.dbpro.ws19.ekfslam.sinks.InfluxDBSinkGPS;
import edu.tuberlin.dbpro.ws19.ekfslam.util.KeyFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import edu.tuberlin.dbpro.ws19.ekfslam.data.*;

import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.security.Key;
import java.util.ArrayList;

/**
 * Skeleton for a Flink Streaming Job.
 *
 * <p>For a tutorial how to write a Flink streaming application, check the
 * tutorials and examples on the <a href="http://flink.apache.org/docs/stable/">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
public class StreamingJobExtendedKalmanFilter {

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


        DataStream<String> dataStream = env.readTextFile("src/main/resources/01_HOPEFULLY_FINAL-1.csv");
        //KeyedDataPoint<latitude>
        /*DataStream<KeyedDataPoint> dataStream = env.readTextFile("src/main/resources/01_HOPEFULLY_FINAL-1")
                .map(new ParseData())
                .keyBy("key")
                .flatMap(new MoveFunction());*/

        //other dataStreams
        DataStream<String> stream2 = env.addSource(new MySource("src/main/resources/01_HOPEFULLY_FINAL-1.csv"));
        DataStream<String> stream3 = env.addSource(new MySource("src/main/resources/01_HOPEFULLY_FINAL-1.csv"));
        DataStream<String> stream4 = env.addSource(new MySource("src/main/resources/01_HOPEFULLY_FINAL-1.csv"));
        DataStream<String> stream5 = env.addSource(new MySource("src/main/resources/01_HOPEFULLY_FINAL-1.csv"));
        DataStream<String> stream6 = env.addSource(new MySource("src/main/resources/01_HOPEFULLY_FINAL-1.csv"));

        DataStream<String> fullStream = dataStream
                .union(stream2, stream3, stream4, stream5, stream6);

        DataStream<KeyedDataPoint> filteredStream = fullStream
                .map(new ParseData())
                .keyBy("key")
                .flatMap(new ExtendedKalmanFilter());

        filteredStream.map(new getTuple4())
                .writeAsCsv("src/main/resources/parallelOutput.csv", FileSystem.WriteMode.OVERWRITE, "\n",";");
        //fullData.addSink(new InfluxDBSinkGPS("DBProTest", "gpstest"));
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
        public KeyedDataPoint<Tuple4> map(String record) {
            //String rawData = record.substring(1, record.length() - 1);
            String[] data = record.split(",");

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

            Tuple4 tuple = new Tuple4<Double, Double, Double, Double[]>(xInc, yInc, phiInc, laserArr);
            return new KeyedDataPoint<Tuple4>("ekf",timestamp, tuple);
        }
    }

    public final static class MySource implements SourceFunction<String>{
        private volatile boolean isRunning = true;
        private String path;

        public MySource(String path){
            this.path=path;
        }
        @Override
        public void run(SourceContext<String> ctx) throws Exception {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String row;
            while(isRunning&&(row = reader.readLine())!= null){
                ctx.collect(row);
            }
        }

        @Override
        public void cancel() {
            isRunning = false;
        }
    }

    private static class getTuple4 extends RichMapFunction<KeyedDataPoint, Tuple4<Long, Double, Double, ArrayList<Double>>>{

        @Override
        public Tuple4<Long, Double, Double, ArrayList<Double>> map(KeyedDataPoint value){
            Tuple3<Double, Double, ArrayList<Double>> val = (Tuple3<Double, Double, ArrayList<Double>>) value.getValue();
            long time = value.getTimeStampMs();
            return new Tuple4<>(time, val.f0, val.f1, val.f2);
        }
    }
}
