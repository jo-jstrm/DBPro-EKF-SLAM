package edu.tuberlin.dbpro.ws19.ekfslam.functions;

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

    private transient ValueState<Tuple2<DoubleMatrix1D, DoubleMatrix2D>> filterParams;

    @Override
    public void flatMap(KeyedDataPoint inputPoint, Collector<KeyedDataPoint> outFilteredPointCollector) throws Exception {
        /*
        x = v*cos(theta)
        y = v*sin(theta)
        theta = v/L*tan(alpha)
         */

        /*
        previous x, y, and phi readings from the Value state,
        Construction of a Vector
         */
        Double x_prev = (Double) filterParams.value().f0.get(0);
        Double y_prev = (Double) filterParams.value().f0.get(1);
        Double phi_prev = (Double) filterParams.value().f0.get(2);



        //predict a priori state
        Tuple3 input = (Tuple3) inputPoint.getValue();

        Double x_ = x_prev + (Double) input.f0;
        Double y_ = y_prev + (Double) input.f1;
        Double phi_ = (Double) input.f2;

    }

    @Override
    public void open(Configuration config) {
        ValueStateDescriptor<Tuple2<DoubleMatrix1D, DoubleMatrix2D>> descriptor = new ValueStateDescriptor<Tuple2<DoubleMatrix1D, DoubleMatrix2D>>(

                "ekf", // the state name
                TypeInformation.of(new TypeHint<Tuple2<DoubleMatrix1D, DoubleMatrix2D>>() {}), // type information
                Tuple2.of(new DenseDoubleMatrix1D(3).assign(0.0), new DenseDoubleMatrix2D(3,3).assign(0.0))); // default value of the state, if nothing was set  //TODO: check this initialisation!
        filterParams = getRuntimeContext().getState(descriptor);
    }
}
