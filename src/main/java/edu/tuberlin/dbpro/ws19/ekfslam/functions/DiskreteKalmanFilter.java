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
public class DiskreteKalmanFilter extends RichFlatMapFunction<KeyedDataPoint, KeyedDataPoint> {
    static final double L = 2.83;
    //x, y, phi, P, time, speed
    private transient ValueState<Tuple6<Double, Double, Double, Double [], Long, Double>> filterParams;
    private  Double[] R = {0.1,0.1,0.1}; //default value


    @Override
    public void flatMap(KeyedDataPoint inputPoint, Collector<KeyedDataPoint> outFilteredPointCollector) throws Exception {
        /*
        x = v*cos(theta)
        y = v*sin(theta)
        theta = v/L*tan(alpha)
         */

        Tuple6< Double, Double, Double, Double[], Long, Double> previousPointParams = filterParams.value();

        Double x_ = previousPointParams.f0;
        Double y_ = previousPointParams.f1;
        Double phi_ = previousPointParams.f2;
        Double[] P_ = previousPointParams.f3;
        Long prevtime = previousPointParams.f4;
        Double speed = previousPointParams.f5;

        //time update
        Tuple2 value = (Tuple2) inputPoint.getValue();

        //time in ms
        Long timedif = inputPoint.getTimeStampMs()-prevtime;
        Double alpha = (Double) value.f0;
        Double nspeed = (Double) value.f1;

        Double x = x_ + (speed*Math.cos(phi_)*timedif)/1000;
        Double y = y_ + (speed*Math.sin(phi_)*(timedif))/1000;
        Double phi = (speed*timedif*Math.tan(alpha))/(L*1000);


        //measurement update
        Double[] K = new Double[P_.length];

        for(int i = 0; i < K.length; i++){
            K[i] = P_[i]/ (P_[i] + R[i]);
        }

        Double[] P = new Double[P_.length];
        for(int i = 0; i < P.length; i++){
            P[i] = (1 - K[i]) * P_[i];
        }

        //update state
        Long nprevtime = inputPoint.getTimeStampMs();
        Tuple6<Double, Double, Double, Double[], Long, Double> updated = new Tuple6<>(x,y,phi, P, nprevtime, nspeed);
        filterParams.update(updated);

        // return filtered point
        outFilteredPointCollector.collect(new KeyedDataPoint<Tuple3<Double, Double, Double>>("filteredSensor", inputPoint.getTimeStampMs(), new Tuple3<Double, Double, Double>(x,y,phi)));
    }

    @Override
    public void open(Configuration config) {
        ValueStateDescriptor<Tuple6<Double, Double, Double, Double [], Long, Double>> descriptor = new ValueStateDescriptor<Tuple6<Double, Double, Double, Double [], Long, Double>>(
                "sensordata", // the state name
                TypeInformation.of(new TypeHint<Tuple6<Double, Double, Double, Double [], Long, Double>>() {}), // type information
                Tuple6.of(0.0, 0.0, 0.0, new Double[] {1.0, 1.0, 1.0}, 0L, 0.0)); // default value of the state, if nothing was set  //TODO: check this initialisation!
        filterParams = getRuntimeContext().getState(descriptor);
    }
}
