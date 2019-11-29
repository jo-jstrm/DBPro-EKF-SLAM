package edu.tuberlin.dbpro.ws19.ekfslam.functions;

import cern.colt.matrix.DoubleMatrix2D;
import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.util.Collector;

public class EKFSLAM extends RichFlatMapFunction<KeyedDataPoint, KeyedDataPoint> {

    private transient ValueState<DoubleMatrix2D> filterParams;

    @Override
    public void flatMap(KeyedDataPoint inputPoint, Collector<KeyedDataPoint> outFilteredPointCollector) throws Exception {
        /*
        x = v*cos(theta)
        y = v*sin(theta)
        theta = v/L*tan(alpha)
         */

        //get input for prediction step
        Double x_prev = (Double) filterParams.value().get(1,0);
        Double y_prev = (Double) filterParams.value().get(1,1);
        Double phi_prev = (Double) filterParams.value().get(1,2);

        //predict a priori state
        Tuple3 input = (Tuple3) inputPoint.getValue();

        Double x_ = x_prev + (Double) input.f0;
        Double y_ = y_prev + (Double) input.f1;
        Double phi_ = (Double) input.f2;





    }
}
