package functions;

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
    //LastTimestamp, Lat, Long, Theta, Speed, P
    private transient ValueState<Tuple4<Double, Double, Double, Double[]>> filterParams;
    private Double[] R = {0.1,0.1,0.1}; //default value
    private long prevTimestamp;
    private Double speed;

    @Override
    public void flatMap(KeyedDataPoint inputPoint, Collector<KeyedDataPoint> outFilteredPointCollector) throws Exception {
        /*
        x = v*cos(theta)
        y = v*sin(theta)
        theta = v/L*tan(alpha)
         */

        Tuple4< Double, Double, Double, Double[]> previousPointParams = filterParams.value();

        double x_ = previousPointParams.f0;
        double y_ = previousPointParams.f1;
        double theta_ = previousPointParams.f2;
        Double[] P_ = previousPointParams.f3;

        //time update
        Tuple2 value = (Tuple2) inputPoint.getValue();
        long timedif = inputPoint.getTimeStampMs()-prevTimestamp;
        double alpha = (Double) value.f0;

        double x = x_ + speed*Math.cos(theta_)*timedif;
        double y = y_ + speed*Math.sin(theta_)*timedif;
        double theta = speed*timedif*Math.tan(alpha)/L;

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
        speed = (Double) value.f1;
        prevTimestamp = inputPoint.getTimeStampMs();
        Tuple4<Double, Double, Double, Double[]> updated = new Tuple4<>(x,y,theta, P);
        filterParams.update(updated);

        // return filtered point
        outFilteredPointCollector.collect(new KeyedDataPoint<Tuple3<Double, Double, Double>>("filteredSensor", inputPoint.getTimeStampMs(), new Tuple3<Double, Double, Double>(x,y,theta)));
    }

    @Override
    public void open(Configuration config) {
        ValueStateDescriptor<Tuple4<Double, Double, Double, Double[]>> descriptor = new ValueStateDescriptor<Tuple4<Double, Double, Double, Double[]>>(

                "average", // the state name
                TypeInformation.of(new TypeHint<Tuple4<Double, Double, Double, Double[]>>() {}), // type information
                Tuple4.of(0.0, 0.0, 0.5*Math.PI, new Double[] {1.0, 1.0, 1.0})); // default value of the state, if nothing was set  //TODO: check this initialisation!
        filterParams = getRuntimeContext().getState(descriptor);
        this.speed = 0.0;
        this.prevTimestamp = 0;
    }
}
