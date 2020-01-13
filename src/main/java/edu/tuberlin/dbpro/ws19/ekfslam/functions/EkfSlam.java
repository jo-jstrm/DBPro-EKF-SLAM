package edu.tuberlin.dbpro.ws19.ekfslam.functions;

import cern.colt.function.DoubleDoubleFunction;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

public class EkfSlam extends RichFlatMapFunction<KeyedDataPoint, KeyedDataPoint> {

    private transient ValueState<Tuple4<DoubleMatrix1D, DoubleMatrix2D, Long, Double>> filterParams;
    static final double L = 2.83;

    @Override
    public void flatMap(KeyedDataPoint inputPoint, Collector<KeyedDataPoint> outFilteredPointCollector) throws Exception {
        /*
        x = v*cos(theta)
        y = v*sin(theta)
        theta = v/L*tan(alpha)
         */


        //get values from persisted state
        DoubleMatrix1D stateVector = filterParams.value().f0;
        DoubleMatrix2D sigma = filterParams.value().f1;
        Long prevtime = filterParams.value().f2;
        Double speed = filterParams.value().f3;

        Double x_prev = stateVector.get(0);
        Double y_prev = stateVector.get(1);
        Double phi_prev = stateVector.get(2);

        //1. calculate time difference between last and current tuple
        Long actualTime = (Long) inputPoint.getTimeStampMs();
        Long timedif = (actualTime - prevtime);

        //2. get steering and speed of current tuple
        Tuple2 input = (Tuple2) inputPoint.getValue();
        Double alpha = (Double) input.f0;


        /*Double x_ = x_prev + (speed*Math.cos(phi_prev)*timedif)/1000;
        Double y_ = y_prev + (speed*Math.sin(phi_prev)*(timedif))/1000;
        Double phi_ = (speed*timedif*Math.tan(alpha))/(L*1000);*/

        //write triangular velocity equations into a vector (incVector)
        Double x_inc = (-speed/alpha*Math.sin(phi_prev)+speed/alpha*Math.sin(phi_prev+alpha*timedif/1000));
        Double y_inc = (speed/alpha*Math.cos(phi_prev) - speed/alpha*Math.cos(phi_prev + alpha*timedif/1000));
        Double phi_inc = alpha*timedif/1000;


        /*speed = speed/(1.0-Math.tan(alpha)*0.76/L);
        Double phi_tryit = speed*Math.tan(alpha);
        Double timedifInSecs = Double.valueOf(timedif)/1000.0;

        Double x_inc = timedifInSecs*(speed*Math.cos(phi_tryit)-(speed/L)*Math.tan(phi_tryit)*(speed*Math.sin(phi_tryit)+0.5*Math.cos(phi_tryit))) ;
        Double y_inc = timedifInSecs*(speed*Math.sin(phi_tryit)+(speed/L)*Math.tan(phi_tryit)*(speed*Math.cos(phi_tryit)-0.5*Math.sin(phi_tryit)));
        Double phi_inc = timedifInSecs*(speed/L)*Math.tan(alpha);*/
        double[] values = {x_inc, y_inc, phi_inc};
        DoubleMatrix1D incVector = new DenseDoubleMatrix1D(3).assign(values);

        //estimate state of the vehicle
        /*Double x_ = x_prev + (-speed/alpha*Math.sin(phi_prev)+speed/alpha*Math.sin(phi_prev+alpha*timedif));
        Double y_ = y_prev + (speed/alpha*Math.cos(phi_prev) - speed/alpha*Math.cos(phi_prev + alpha*timedif));
        Double phi_ = phi_prev + alpha*timedif;*/

        stateVector.assign(incVector, (v, v1) -> v+v1);
        Double newSpeed = (Double) input.f1;
        System.out.println(stateVector);

        /*stateVector.set(0,x_);
        stateVector.set(1, y_);
        stateVector.set(2, phi_);*/

        filterParams.update(Tuple4.of(stateVector, sigma, actualTime, newSpeed));

        outFilteredPointCollector.collect(new KeyedDataPoint("ekf", actualTime, stateVector));
    }

    @Override
    public void open(Configuration config) {
        ValueStateDescriptor<Tuple4<DoubleMatrix1D, DoubleMatrix2D, Long, Double>> descriptor = new ValueStateDescriptor<Tuple4<DoubleMatrix1D, DoubleMatrix2D, Long, Double>>(

                "ekf", // the state name
                TypeInformation.of(new TypeHint<Tuple4<DoubleMatrix1D, DoubleMatrix2D, Long, Double>>() {}), // type information
                Tuple4.of(new DenseDoubleMatrix1D(3).assign(0.0), new DenseDoubleMatrix2D(3,3).assign(0.0), 0L, 0.0)); // default value of the state, if nothing was set  //TODO: check this initialisation!
        filterParams = getRuntimeContext().getState(descriptor);
    }
}
