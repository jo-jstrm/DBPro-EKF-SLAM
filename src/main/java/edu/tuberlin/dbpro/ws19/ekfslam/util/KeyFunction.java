package edu.tuberlin.dbpro.ws19.ekfslam.util;

import edu.tuberlin.dbpro.ws19.ekfslam.data.DataPoint;
import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;

public class KeyFunction implements MapFunction<DataPoint<Tuple2<Double, Double>>, KeyedDataPoint<Tuple2<Double, Double>>> {

        private String key;

        public KeyFunction(String key) {
            this.key = key;
        }

        @Override
        public KeyedDataPoint<Tuple2<Double, Double>> map(DataPoint<Tuple2<Double, Double>> dataPoint) throws Exception {
            return dataPoint.withKey(key);
        }
}
