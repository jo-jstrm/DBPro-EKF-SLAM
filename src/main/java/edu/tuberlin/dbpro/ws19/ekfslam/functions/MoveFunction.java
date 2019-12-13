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

import java.util.ArrayList;

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
        double phi_ = previousPointParams.f2;

        //time update
        Tuple4 value = (Tuple4) inputPoint.getValue();

        double xinc = (Double) value.f0;
        double yinc = (Double) value.f1;
        double phiinc = (Double) value.f2;
        Double[] laserArr = (Double[]) value.f3;

        //prediction function
        double x2 = x_ + Math.cos(phi_ + phiinc/2)*xinc - Math.sin(phi_ + phiinc/2)*yinc;
        double y2 = y_ + Math.sin(phi_ + phiinc/2)*xinc + Math.cos(phi_ + phiinc/2)*yinc;
        double phi = phi_ + phiinc;

        //work with LaserArray
        ArrayList<Tuple2> landmarks = new ArrayList<>();
        for(int i = 0; i<laserArr.length;i++){
            if(laserArr[i] == 0.0 || laserArr[i] > 80.0){continue;}
            else{
                double degrees = i*0.5;
                double lx = getCoordinate(degrees, laserArr[i], phi, true);
                double ly = getCoordinate(degrees, laserArr[i], phi, false);

                Tuple2 tuple = new Tuple2(lx,ly);
                landmarks.add(tuple);
            }
        }


        //returns
        Tuple3 res = new Tuple3<Double, Double, ArrayList<Tuple2>>(x2,y2,landmarks);
        Tuple3 update = new Tuple3<Double, Double, Double>(x2,y2,phi);
        filterParams.update(update);

        // return filtered point
        outFilteredPointCollector.collect(new KeyedDataPoint<>("move", inputPoint.getTimeStampMs(), res));
    }

    @Override
    public void open(Configuration config) {
        ValueStateDescriptor<Tuple3<Double, Double, Double>> descriptor = new ValueStateDescriptor<Tuple3<Double, Double, Double>>(

                "full", // the state name
                TypeInformation.of(new TypeHint<Tuple3<Double, Double, Double>>() {}), // type information
                Tuple3.of(0.0, 0.0, 0.0)); // default value of the state, if nothing was set  //TODO: check this initialisation!
        filterParams = getRuntimeContext().getState(descriptor);
    }

    public Double getCoordinate(Double degrees, Double gamma, Double phi, Boolean isX) {
        if (isX) {
            double x = gamma * Math.cos(Math.toRadians(degrees) - Math.toRadians(90) + phi) + 3.78;
            return x;
        } else {
            double y = gamma * Math.sin(Math.toRadians(degrees) - Math.toRadians(90) + phi) + 0.5;
            return y;
        }
    }
}
