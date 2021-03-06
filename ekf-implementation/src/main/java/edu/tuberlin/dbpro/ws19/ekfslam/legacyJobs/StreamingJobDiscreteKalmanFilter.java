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
import edu.tuberlin.dbpro.ws19.ekfslam.functions.DiskreteKalmanFilter;
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
public class StreamingJobDiscreteKalmanFilter {

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


		DataStream<KeyedDataPoint> sensorData = env.readTextFile("src/main/resources/time_steering_speed_aa3_dr.csv")
				.map(new StreamingJobDiscreteKalmanFilter.ParseData())
				.keyBy("key")
				.flatMap(new DiskreteKalmanFilter());

		sensorData.map(new StreamingJobDiscreteKalmanFilter.getTuple3())
				.writeAsCsv("src/main/resources/testcsv.csv", FileSystem.WriteMode.OVERWRITE);

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
		env.execute("EKF-SLAM");
	}

	private static class ParseData extends RichMapFunction<String, KeyedDataPoint> {
		private static final long serialVersionUID = 1L;


		@Override
		public KeyedDataPoint<Tuple2> map(String record) {
			//String rawData = record.substring(1, record.length() - 1);
			String[] data = record.split(",");

			// the data look like this...
			// timestamp, latitude, longitude

			//get timestamp, lat and lon from data
			//store lat, lan in Tuple2<Double, Double>
			long timestamp = Long.valueOf(data[0]);
			Tuple2 tuple = new Tuple2<Double, Double>(Double.valueOf(data[1]), Double.valueOf(data[2]));
			//Object d = tuple.getField(0);			//create and return Datapoint with latitude
			return new KeyedDataPoint<Tuple2>("sensordata",timestamp, tuple);
		}
	}

	private static class getTuple3 extends RichMapFunction<KeyedDataPoint, Tuple3<Long, Double, Double>>{

		@Override
		public Tuple3<Long, Double, Double> map(KeyedDataPoint value) throws Exception {
			Tuple3<Double,Double, Double> val = (Tuple3<Double,Double, Double>) value.getValue();
			long time = value.getTimeStampMs();
			return new Tuple3<Long, Double, Double>(time, val.f0, val.f1);
		}
	}
}
