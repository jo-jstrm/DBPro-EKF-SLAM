package edu.tuberlin.dbpro.ws19.ekfslam.functions;

import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple6;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

/*
Performs diskrete kalman filter algorithm for the sensor data of the car.

v - Speed in m/s
L - distance between axes of the car in m (given as 2.83 from data README)
theta - I don 't exactly know. Kind of the direction the car is moving
x - latitude of the car (relative to starting point)
y - longitude of the car (relative to starting point)
alpha - steering from sensor data
 */
public class MoveFunction extends RichFlatMapFunction<KeyedDataPoint, KeyedDataPoint> {
    //LastTimestamp, Lat, Long, Theta, Speed, P
    private transient ValueState<Tuple3<Double, Double, Double>> filterParams;

    @Override
    public void flatMap(KeyedDataPoint inputPoint, Collector<KeyedDataPoint> outFilteredPointCollector) throws Exception {
        /*
        x = v*cos(theta)
        y = v*sin(theta)
        theta = v/L*tan(alpha)
         */

        Tuple3< Double, Double, Double> previousPointParams = filterParams.value();

        double x_ = previousPointParams.f0;
        double y_ = previousPointParams.f1;
        double theta_ = previousPointParams.f2;

        //time update
        Tuple3 value = (Tuple3) inputPoint.getValue();

        double x = x_ + (Double) value.f0;
        double y = y_ + (Double) value.f1;
        double theta = theta_ + (Double) value.f2;

        Tuple2 res = new Tuple2<Double, Double>(x,y);
        Tuple3 update = new Tuple3<Double, Double, Double>(x,y,theta);
        filterParams.update(update);

        // return filtered point
        outFilteredPointCollector.collect(new KeyedDataPoint<>("move", inputPoint.getTimeStampMs(), res));
    }

    @Override
    public void open(Configuration config) {
        ValueStateDescriptor<Tuple3<Double, Double, Double>> descriptor = new ValueStateDescriptor<Tuple3<Double, Double, Double>>(

                "odo", // the state name
                TypeInformation.of(new TypeHint<Tuple3<Double, Double, Double>>() {}), // type information
                Tuple3.of(0.0, 0.0, 0.0)); // default value of the state, if nothing was set  //TODO: check this initialisation!
        filterParams = getRuntimeContext().getState(descriptor);
    }
}
