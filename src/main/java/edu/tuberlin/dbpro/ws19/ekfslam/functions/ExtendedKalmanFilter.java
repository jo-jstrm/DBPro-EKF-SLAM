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
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.io.IOException;

public class ExtendedKalmanFilter extends RichFlatMapFunction<KeyedDataPoint, KeyedDataPoint> {

    public static double vehicleL = 2.83;
    public static double vehicleH = 0.76;
    public static double vehicleB = 0.5;
    public static double vehicleA = 3.78;

    private transient ValueState<Tuple3<DoubleMatrix1D, DoubleMatrix2D, Long>> filterParams;

    @Override
    public void flatMap(KeyedDataPoint inputPoint, Collector<KeyedDataPoint> outFilteredPointCollector) throws Exception {

        //predict a priori state
        if(inputPoint.getKey().equals("odo")) {
            Tuple2 value = (Tuple2) inputPoint.getValue();
            Double phi_prev = (Double) value.f1;
            Tuple2 estimate = predict(filterParams, inputPoint);

            Tuple3<DoubleMatrix1D, DoubleMatrix2D, Long> updateValue = new Tuple3<DoubleMatrix1D, DoubleMatrix2D, Long>((DoubleMatrix1D) estimate.f0, (DoubleMatrix2D) estimate.f1, inputPoint.getTimeStampMs());
            filterParams.update(updateValue);

            // return filtered point
            outFilteredPointCollector.collect(new KeyedDataPoint<>("prediction", inputPoint.getTimeStampMs(), estimate.f0));

        }

    }

    public static Tuple2<DoubleMatrix1D, DoubleMatrix2D> predict(ValueState<Tuple3<DoubleMatrix1D, DoubleMatrix2D, Long>> valueState, KeyedDataPoint<Tuple2> inputPoint) throws IOException {

        //Get Vector of the previous state (input of prediction step)
        Double x_prev = (Double) valueState.value().f0.get(0);
        Double y_prev = (Double) valueState.value().f0.get(1);
        Double phi_prev = (Double) valueState.value().f0.get(2);
        double[] previous = {x_prev, y_prev, phi_prev};
        DoubleMatrix1D previousState = new DenseDoubleMatrix1D(3);
        previousState.assign(previous);

        /*
        Maybe the above can be replaced by
        DoubleMatrix1D previosState = filterparams.value().f0;
         */

        //get current steering, speed and timedif between current and last point
        Tuple2 input = (Tuple2) inputPoint.getValue();
        Double input_speed = (Double) input.f0;
        Double steering = (Double) input.f1;
        double timedif = (inputPoint.getTimeStampMs() - valueState.value().f2)/1000;

        //Map speed according to paper
        Double speed = input_speed/(1-Math.tan(steering)*vehicleH/vehicleL);


        /*motion model from the victoria park data set*/
        Double x_inc = timedif*(speed*Math.cos(phi_prev)-(speed/vehicleL)*Math.tan(phi_prev)*(vehicleA*Math.sin(phi_prev)+vehicleB*Math.cos(phi_prev)));
        Double y_inc = timedif*(speed*Math.sin(phi_prev)+(speed/vehicleL)*Math.tan(phi_prev)*(vehicleA*Math.cos(phi_prev)-vehicleB*Math.sin(phi_prev)));
        Double phi_inc = timedif*(speed/vehicleL)*Math.tan(steering);
        double[] increments = {x_inc, y_inc, phi_inc};

        DoubleMatrix1D stateIncrements = new DenseDoubleMatrix1D(3).assign(increments);

        //do the prediction step by adding the motion increment to the previous state
        DoubleMatrix1D estimatedPoseVector = new DenseDoubleMatrix1D(3).assign(previousState.assign(stateIncrements, (v, v1) -> v + v1));
        //System.out.println("estimatedPoseVector " + estimatedPoseVector);



        //---------------------------------------Matrix------------------------------------------------------------



        //calculate estimatedSigma
        DoubleMatrix2D sigma_prev = valueState.value().f1;
        //System.out.println("sigma_prev " + sigma_prev);
        /*generate Jacobian maxtrix (Gt in slides) for estimatedSigma, jacobian varies depending on motion model,
        with upperValue being the double value in index (0,2),
        and lowerValue being the double value in index (1,2)
         */
        /*Jacobian from slides of uni freiburg
        Double upperValue = (-(speed/steering*Math.cos(phi_prev)+(speed/steering)*Math.cos(phi_prev+steering*timedif)));
        Double lowerValue = (-(speed/steering*Math.sin(phi_prev)+(speed/steering)*Math.sin(phi_prev+steering*timedif)));*/
        /*Jacobian from Victoria Park dataset*/
        Double upperValue = -timedif*(speed*Math.sin(phi_prev)+(speed/vehicleL)*Math.tan(steering)*(vehicleA*Math.cos(phi_prev)-vehicleB*Math.sin(phi_prev)));
        Double lowerValue = -timedif*(speed*Math.cos(phi_prev)-(speed/vehicleL)*Math.tan(steering)*(vehicleA*Math.sin(phi_prev)+vehicleB*Math.cos(phi_prev)));
        double[][] jacobianArray = {{1.0, 0.0, upperValue}, {0.0, 1.0, lowerValue}, {0.0, 0.0, 1.0}};
        DoubleMatrix2D jacobianMatrixGt = new DenseDoubleMatrix2D(3,3).assign(jacobianArray);
        //System.out.println("jacobianMatrixGt " + jacobianMatrixGt);

        /*generate Rt //TODO: figure out exact Rt*/
        double valueRt = 0.01;
        double[][] valuesRt = {{valueRt*10,0.0,0.0},{0.0, valueRt, 0.0}, {0.0,0.0,valueRt}};
        DoubleMatrix2D movementErrorMatrixRt = new DenseDoubleMatrix2D(3,3).assign(valuesRt);
        //System.out.println("movementErrorMatrixRt " + movementErrorMatrixRt);

        //Matrix multiplication of Gt with previous sigma matrix (sigman_prev)
        DoubleMatrix2D sigmaStep1 = jacobianMatrixGt.zMult(sigma_prev, null, 1.0, 1.0, false, false);
        //System.out.println("sigmaStep1 " + sigmaStep1);

        //Matrix multiplication of the result of Step on with the transposed jacobian matrix (Gt)T
        DoubleMatrix2D sigmaStep2 = sigmaStep1.zMult(jacobianMatrixGt, null, 1.0, 1.0, false, true);
        //System.out.println("sigmaStep2 " + sigmaStep2);

        //Adding the movement Error to the estimated Sigma
        DoubleMatrix2D estimatedSigma = sigmaStep2.assign(movementErrorMatrixRt, (v, v1) -> v + v1);
        //System.out.println("estimatedSigma " + estimatedSigma);


        return Tuple2.of(estimatedPoseVector, estimatedSigma);
    }

    @Override
    public void open(Configuration config) {
        ValueStateDescriptor<Tuple3<DoubleMatrix1D, DoubleMatrix2D, Long>> descriptor = new ValueStateDescriptor<Tuple3<DoubleMatrix1D, DoubleMatrix2D, Long>>(

                "ekf", // the state name
                TypeInformation.of(new TypeHint<Tuple3<DoubleMatrix1D, DoubleMatrix2D, Long>>() {}), // type information
                Tuple3.of(new DenseDoubleMatrix1D(3).assign(0.0), new DenseDoubleMatrix2D(3,3).assign(0.0), 0L)); // default value of the state, if nothing was set  //TODO: check this initialisation!
        filterParams = getRuntimeContext().getState(descriptor);
    }
}
