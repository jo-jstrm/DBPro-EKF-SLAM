package edu.tuberlin.dbpro.ws19.ekfslam.functions;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import edu.tuberlin.dbpro.ws19.ekfslam.util.SlamUtils;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.io.IOException;

public class EKF_SLAM extends RichFlatMapFunction<KeyedDataPoint, KeyedDataPoint> {
    public static double vehicleL = 2.83;
    public static double vehicleH = 0.76;
    public static double vehicleB = 0.5;
    public static double vehicleA = 3.78;

    public static boolean fullEKFSLAM = false;
    public static boolean printPrediction = true;
    public static boolean printUpdate = true;

    private transient ValueState<Tuple3<DoubleMatrix2D, DoubleMatrix2D, Long>> filterParams;

    @Override
    public void flatMap(KeyedDataPoint inputPoint1, Collector<KeyedDataPoint> outFilteredPointCollector) throws Exception {
        Tuple3 inputPoint = (Tuple3) inputPoint1.getValue();
        if(fullEKFSLAM){

        }else{
            if(inputPoint.f2.equals("odo")) {
                System.out.println("filterParamsVectorPREDICT: " + filterParams.value().f0);
                Tuple2 estimate = predict(filterParams, inputPoint1);

                Tuple3 updateValue = new Tuple3(estimate.f0, estimate.f1, inputPoint1.getTimeStampMs());
                filterParams.update(updateValue);

                // return filtered point
                //returned field is the state vector with [x,y,phi]
                if(printPrediction){
                    outFilteredPointCollector.collect(new KeyedDataPoint("prediction", inputPoint1.getTimeStampMs(), estimate.f0));
                }

            }
        }
    }
    public static Tuple2<DoubleMatrix2D, DoubleMatrix2D> predict(ValueState<Tuple3<DoubleMatrix2D, DoubleMatrix2D, Long>> valueState, KeyedDataPoint<Tuple3> inputPoint) throws IOException {
        //Given inputs by the saved value State
        Double x_prev = valueState.value().f0.get(0,0);
        Double y_prev = valueState.value().f0.get(1,0);
        Double phi_prev = valueState.value().f0.get(2,0);
        phi_prev = phi_prev%(Math.PI*2);
        double[][] previous = {{x_prev}, {y_prev}, {phi_prev}};
        DoubleMatrix2D mu = DoubleFactory2D.dense.make(previous.length, 1).assign(previous);
        //System.out.println("mu " + mu);
        //Motion model for the car based on EKF implementation
        double timedif = (double)(inputPoint.getTimeStampMs() - valueState.value().f2)/1000;
        Double inputSpeed = (Double) inputPoint.getValue().f0;
        Double steering = (Double) inputPoint.getValue().f1;
        Double speed = inputSpeed/(1-Math.tan(steering)*vehicleH/vehicleL);
        Double x_inc = timedif*(speed*Math.cos(phi_prev)-(speed/vehicleL)*Math.tan(steering)*(vehicleA*Math.sin(phi_prev)+vehicleB*Math.cos(phi_prev)));
        Double y_inc = timedif*(speed*Math.sin(phi_prev)+(speed/vehicleL)*Math.tan(steering)*(vehicleA*Math.cos(phi_prev)-vehicleB*Math.sin(phi_prev)));
        Double phi_inc = timedif*(speed/vehicleL)*Math.tan(steering);
        double[][] increments = {{x_inc}, {y_inc}, {phi_inc}};
        DoubleMatrix2D muIncrement = new DenseDoubleMatrix2D(3, 1).assign(increments);
        //System.out.println("muIncrement " + muIncrement);
        //Fx matrix from the prediction step based on the uni freiburg slides to map increments to 3+2N space
        DoubleMatrix2D Fx = SlamUtils.makePredictionHelperMatrix(mu);
        //System.out.println("Fx " + Fx);
        //Multiply Fx transposed with the increments from the motion model
        DoubleMatrix2D muIncrementHighDim = Fx.zMult(muIncrement, null, 1.0, 1.0, true, false);
        //System.out.println("muIncrementHighDim " + muIncrementHighDim);
        //Prediction Step for mu in EKF SLAM from the uni freiburg slides page 22
        DoubleMatrix2D estimatedMu = mu.assign(muIncrementHighDim, (v, v1) -> v+v1);
        //System.out.println("estimatedMu " + estimatedMu);

        //Preparing the prediction step for the covariance matrix
        //Previous covariance matrix
        DoubleMatrix2D cov_prev = DoubleFactory2D.dense.identity(3);
        //System.out.println("cov_prev " + cov_prev);
        //motion model error matrix Rt
        double[][] rtArr = {{0.5,0,0},{0,0.5,0},{0,0,0.5}};
        DoubleMatrix2D Rtx = new DenseDoubleMatrix2D(3,3).assign(rtArr);
        //System.out.println("Rtx " + Rtx);
        //JacobianMatrix for Gt as the covariance update step
        Double upperValue = -timedif*(speed*Math.sin(phi_prev)+(speed/vehicleL)*Math.tan(steering)*(vehicleA*Math.cos(phi_prev)-vehicleB*Math.sin(phi_prev)));
        Double lowerValue = timedif*(speed*Math.cos(phi_prev)-(speed/vehicleL)*Math.tan(steering)*(vehicleA*Math.sin(phi_prev)+vehicleB*Math.cos(phi_prev)));
        double[][] jacobianArray = {{0.0, 0.0, upperValue}, {0.0, 0.0, lowerValue}, {0.0, 0.0, 0.0}};
        DoubleMatrix2D jacobianMatrix = new DenseDoubleMatrix2D(3,3).assign(jacobianArray);
        //System.out.println("jacobianMatrix " + jacobianMatrix);
        //Step1 to calculate Gt: Fx transposed multiplied by the jacobianMatrix
        DoubleMatrix2D gtStep1 = Fx.zMult(jacobianMatrix, null, 1.0, 1.0, true, false);
        //System.out.println("gtStep1 " + gtStep1);
        //Step2 to calculate Gt: The result of Step1 multiplied by Fx
        DoubleMatrix2D gtStep2 = gtStep1.zMult(Fx, null, 1.0, 1.0, false, false);
        //System.out.println("gtStep2 " + gtStep2);
        //Step3 to calclulate Gt: Get the identity matrix with the same size as gtStep2
        DoubleMatrix2D gtStep3 = DoubleFactory2D.dense.identity(gtStep2.rows());
        //System.out.println("gtStep3 " + gtStep3);
        //Calculate Gt
        DoubleMatrix2D Gt = gtStep3.assign(gtStep2, (v, v1) -> v + v1);
        //System.out.println("Gt " + Gt);

        //Calculate the estimated covariance matrix
        //First generate the error matrix Rt which needs to be mapped from 3x3 to high dimensional space with Fx
        //Step1 to calculate Rt: multiply Fx transposed with Rtx
        DoubleMatrix2D rtStep1 = Fx.zMult(Rtx, null, 1.0, 1.0, true, false);
        //System.out.println("rtStep1 " + rtStep1);
        //Step2 to calculate Rt: multiply Step1 with Fx
        DoubleMatrix2D Rt = rtStep1.zMult(Fx, null, 1.0, 1.0, false, false);
        //System.out.println("Rt " + Rt);
        //Second generate the increment for the estimated covariance matrix (Gt.cov_prev.GtT)
        //Step1 to calculate covIncr: Multiply Gt with the previous covariance matrix
        DoubleMatrix2D covIncrStep1 = Gt.zMult(cov_prev, null, 1.0, 1.0, false, false);
        //System.out.println("covIncrStep1 " + covIncrStep1);
        //Step2 to calculate covIncr: Multiply covIncrStep1 with GtT
        DoubleMatrix2D covIncr = covIncrStep1.zMult(Gt, null, 1.0, 1.0, false, true);
        //System.out.println("covIncr " + covIncr);
        //Calculate the estimated covariance matrix
        DoubleMatrix2D estimatedCov = covIncr.assign(Rt, (v, v1) -> v + v1);
        //System.out.println("estimatedCov " + estimatedCov);
        return Tuple2.of(estimatedMu, estimatedCov);
    }

    @Override
    public void open(Configuration config) {
        ValueStateDescriptor<Tuple3<DoubleMatrix2D, DoubleMatrix2D, Long>> descriptor = new ValueStateDescriptor<Tuple3<DoubleMatrix2D, DoubleMatrix2D, Long>>(
                "SLAM", // the state name
                TypeInformation.of(new TypeHint<Tuple3<DoubleMatrix2D, DoubleMatrix2D, Long>>() {}), // type information
                Tuple3.of(new DenseDoubleMatrix2D(3,1).assign(0.0), new DenseDoubleMatrix2D(3,3).assign(0.0), 0L)); // default value of the state, if nothing was set  //TODO: check this initialisation!
        filterParams = getRuntimeContext().getState(descriptor);
    }


}
