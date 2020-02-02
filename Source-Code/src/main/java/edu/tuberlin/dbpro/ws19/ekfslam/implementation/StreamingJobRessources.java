package edu.tuberlin.dbpro.ws19.ekfslam.implementation;
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
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

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
public class StreamingJobRessources {

    public static void main(String[] args) throws Exception {
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        @SuppressWarnings({"rawtypes", "serial"})


        DataStream<String> sensorData = env.readTextFile("C:/Users/Mark Paranskij/Documents/Uni/DBPRO/dataset_victoria_park/GPS_DRS_LASER.csv");


        sensorData.map(new ParseData())
                .writeAsCsv("src/main/resources/GPS_DRS_LASER.csv", FileSystem.WriteMode.OVERWRITE);

		/*DataStream<KeyedDataPoint> sensorDataFiltered = sensorData
				.keyBy("key")
				.flatMap(new DiskreteKalmanFilter());

		 */

        //sensorDataFiltered.map(new StreamingJobDiscreteKalmanFilter.getTuple3())
        //sensorDataFiltered.print();
        //	.writeAsCsv("src/main/resources/testcsv.csv", FileSystem.WriteMode.OVERWRITE);

        //sensorDataFiltered.addSink(new InfluxDBSinkSensor<>("DBProTest", "sensorTest"));
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
        env.execute("RessourceAddition");
    }

    private static class ParseData implements MapFunction<String, Tuple> {
        private static final long serialVersionUID = 1L;



        @Override
        public Tuple map(String value) throws Exception{
            String[] data = value.split(",");

            // the data look like this...
            // timestamp, latitude, longitude

            //get timestamp, lat and lon from data
            //store lat, lan in Tuple2<Double, Double>
            long timestamp = new Double(Double.valueOf(data[1])*1000).longValue();
            //Tuple2 tuple = new Tuple2<Double, Double>(Double.valueOf(data[1]), Double.valueOf(data[2]));
            if (data.length == 4){
                Tuple4 tuple = new Tuple4(data[0], timestamp, Double.valueOf(data[2]), Double.valueOf(data[3]));
                return tuple;
            }else{
                return new Tuple3(data[0], timestamp, data[2]);
            }
        }
    }
}

