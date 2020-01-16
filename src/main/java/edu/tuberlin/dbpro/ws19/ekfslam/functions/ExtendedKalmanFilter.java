package edu.tuberlin.dbpro.ws19.ekfslam.functions;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
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
            System.out.println("filterParamsVectorPREDICT: " + filterParams.value().f0);
            Tuple2 estimate = predict(filterParams, inputPoint);

            Tuple3<DoubleMatrix1D, DoubleMatrix2D, Long> updateValue = new Tuple3<>((DoubleMatrix1D) estimate.f0, (DoubleMatrix2D) estimate.f1, inputPoint.getTimeStampMs());
            filterParams.update(updateValue);

            // return filtered point
            //returned field is the state vector with [x,y,phi]
            outFilteredPointCollector.collect(new KeyedDataPoint<>("prediction", inputPoint.getTimeStampMs(), estimate.f0));

        }
        if(inputPoint.getKey().equals("gps")){
            System.out.println("filterParamsVectorUPDATE:  " + filterParams.value().f0);
            Tuple2 updatedEstimate = update(filterParams, inputPoint);

            DoubleMatrix1D dm = (DoubleMatrix1D) updatedEstimate.f0;
            dm.assign((v -> v+1));
            Tuple3<DoubleMatrix1D, DoubleMatrix2D, Long> updateValue = new Tuple3<>(dm, (DoubleMatrix2D) updatedEstimate.f1, inputPoint.getTimeStampMs());
            filterParams.update(updateValue);

            // return filtered point
            //returned field is the state vector with [x,y,phi]
            outFilteredPointCollector.collect(new KeyedDataPoint<>("update", inputPoint.getTimeStampMs(), updatedEstimate.f0));


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
        Long currentTime = inputPoint.getTimeStampMs();
        double timedif = ((double)(inputPoint.getTimeStampMs() - valueState.value().f2))/1000;

        //Map speed according to paper
        Double speed = input_speed/(1-Math.tan(steering)*vehicleH/vehicleL);
        //Double speed = input_speed;


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


    public static Tuple2<DoubleMatrix1D, DoubleMatrix2D> update(ValueState<Tuple3<DoubleMatrix1D, DoubleMatrix2D, Long>> valueState, KeyedDataPoint<Tuple2> inputPoint) throws IOException {
    /*Implement the update step for the motion model and jacobians of the victoria park
        dataset based on the gps data
         */
        //System.out.println("-----<<<Update Step:>>>-----");
        //gpsData only gets x and y coordinates since the first entry is the key and the second the timestamp, which are bot handled vie KeyedDataPoints
        //System.out.println("input: " + valueState.value().f0);
        double x_coord = (double) inputPoint.getValue().f0;
        double y_coord = (double) inputPoint.getValue().f1;
        double[] gpsData = {x_coord, y_coord};
        DoubleMatrix1D gpsPosition = new DenseDoubleMatrix1D(2).assign(gpsData);
        //System.out.println("estimatedPoseVector "+ valueState.value().f0);

        //calculate deltaX, deltaY and deltaDelta for the observation jacobian as stated in the victoria park paper
        DoubleMatrix1D estimatedPoseVector = valueState.value().f0;
        Double deltaX = (gpsPosition.get(0) - estimatedPoseVector.get(0));
        Double deltaY = (gpsPosition.get(1) - estimatedPoseVector.get(1));
        Double deltaDelta = (Math.sqrt((Math.pow(deltaX, 2)+Math.pow(deltaY, 2))));
        if (deltaDelta == 0.0){
            deltaDelta = 1.0;
        }
        //System.out.println("deltaX: " + deltaX + "; deltaY: " + deltaY + "; deltaDelta: " + deltaDelta);
        //Calculate Jacobian matrix (H) based on observations
        Double obsJacobianRow1Column1 = (1/deltaDelta)*(-deltaX);
        Double obsJacobianRow1Column2 = (1/deltaDelta)*(-deltaY);
        Double obsJacobianRow1Column3 = 0.0;
        Double obsJacobianRow2Column1 = (deltaY/Math.pow(deltaDelta, 2));
        Double obsJacobianRow2Column2 = (-deltaX/Math.pow(deltaDelta, 2));
        Double obsJacobianRow2Column3 = -1.0;
        double[][] obsJacobian = {{obsJacobianRow1Column1,obsJacobianRow1Column2,obsJacobianRow1Column3}, {obsJacobianRow2Column1,obsJacobianRow2Column2,obsJacobianRow2Column3}};
        DoubleMatrix2D observationJacobianMatrix = new DenseDoubleMatrix2D(2,3).assign(obsJacobian);
        //System.out.println("ObservationJacobianMatrix " + observationJacobianMatrix);

        //Calculate the KalmanGain with the ErrorMatrix for the measurement
        //TODO: Get accurate error Matrix for the GPS readings
        double[][] gpsErrorArray = {{0.01,0.0}, {0.0,0.01}};
        DoubleMatrix2D gpsErrorMatrix = new DenseDoubleMatrix2D(2,2).assign(gpsErrorArray);

        //Step 1: Calculate the Inverse as part of the KalmanGain
        //Step 1.1: Matrix multiplication between the Jacobian and the estimated Sigma
        DoubleMatrix2D estimatedSigma = valueState.value().f1;
        DoubleMatrix2D kalmanInverseStep1_1 = observationJacobianMatrix.zMult(estimatedSigma, null, 1.0, 1.0, false, false);
        //System.out.println("kalmanInverseStep1_1 " + kalmanInverseStep1_1);

        //Step 1.2: Matrix multiplication between the result of Step 1.1 and the transposed Jacobian
        DoubleMatrix2D kalmanInverseStep1_2 = kalmanInverseStep1_1.zMult(observationJacobianMatrix, null, 1.0, 1.0, false, true);
        //System.out.println("kalmanInverseStep1_2 " + kalmanInverseStep1_2);

        //Step 1.3: Add the observation Error to Step 1.2
        DoubleMatrix2D kalmanInvervseStep1_3 = kalmanInverseStep1_2.assign(gpsErrorMatrix, ((v, v1) -> v + v1));
        //System.out.println("kalmanInvervseStep1_3 " + kalmanInvervseStep1_3);

        //Step 1.4: Calculate the inverse of the result of kalmanInverseStep1_3
        DoubleMatrix2D kalmanInverse = new Algebra().inverse(kalmanInvervseStep1_3);
        //System.out.println("kalmanInverse " + kalmanInverse);

        //Step 2: Calculate the Kalman Gain
        //Step 2.1 Multiply the estimated Sigma with the transposed observation Jacobian Matrix
        DoubleMatrix2D kalmanGainStep2_1 = estimatedSigma.zMult(observationJacobianMatrix, null, 1.0, 1.0 , false, true);
        //System.out.println("kalmanGainStep2_1 " + kalmanGainStep2_1);

        //Step 2.2 Multiply the results from 2.1 and 1.4 to get the Kalman gain based on the gps data and vehicle position
        DoubleMatrix2D kalmanGain = kalmanGainStep2_1.zMult(kalmanInverse, null, 1.0, 1.0, false, false);
        //System.out.println("kalmanGain " + kalmanGain);

        //Calculate the correction step pose vector
        //Get deltaBetween observed and estimated position
        //trying out a different Observed Pose to Observed Position Model
        /*Double positionMinusPoseX = -gpsPosition.get(0);
        Double positionMinusPoseY = -gpsPosition.get(0);
        double [][] positionPose = {{positionMinusPoseX}, {positionMinusPoseY}};
        DoubleMatrix2D positionPoseMatrix = new DenseDoubleMatrix2D(2,1).assign(positionPose);
        System.out.println("positionPoseMatrix " + positionPoseMatrix);*/
        //TODO: Test if to subtract gps and pose from (0,0) or from each other for mathematical correctness
        //distance between origin and gps measurement
        Double gpsZr = Math.sqrt(Math.pow(gpsPosition.get(0), 2)+(Math.pow(gpsPosition.get(1), 2)));
        //angle from origin to gps measurement
        Double gpsZB = Math.atan2(gpsPosition.get(1),gpsPosition.get(0));
        double[] gpsZArr = {gpsZr,gpsZB};
        DoubleMatrix1D gpsZVector = new DenseDoubleMatrix1D(2).assign(gpsZArr);
        //System.out.println("gpsZVector " + gpsZVector);

        //distance form origin to est. position
        Double poseZr = Math.sqrt(Math.pow(estimatedPoseVector.get(0), 2)+(Math.pow(estimatedPoseVector.get(1), 2)));
        //angle from origin to vector
        Double poseZB = Math.atan2(estimatedPoseVector.get(1),estimatedPoseVector.get(0));
        double[] poseZArr = {poseZr,poseZB};
        DoubleMatrix1D poseZVector = new DenseDoubleMatrix1D(2).assign(poseZArr);
        //System.out.println("poseZVector " + poseZVector);
        DoubleMatrix1D positionPoseVector = gpsZVector.assign(poseZVector, (v, v1) -> v - v1);
        //System.out.println("positionPoseVector " + positionPoseVector);
        double [][] positionPose = {{positionPoseVector.get(0)},{positionPoseVector.get(1)}};
        DoubleMatrix2D positionPoseMatrix = new DenseDoubleMatrix2D(2,1).assign(positionPose);
        //Multiply delta between observed and estimated position with the Kalman Gain
        DoubleMatrix2D kalmanGainPosition = kalmanGain.zMult(positionPoseMatrix, null, 1.0, 1.0, false, false);
        DoubleMatrix1D kalmanGainPositionVector = new DenseDoubleMatrix1D(3).assign(kalmanGainPosition.viewColumn(0));
        //System.out.println("kalmanGainPositionVector " + kalmanGainPositionVector);

        //Update PoseVector/PoseMatrix

        DoubleMatrix1D updatedPose = estimatedPoseVector.assign(kalmanGainPositionVector, (v, v1) -> v + v1);
        //System.out.println("updatedPose " + updatedPose);

        //Update Sigma
        DoubleMatrix2D upSigma1 = kalmanGain.zMult(kalmanInvervseStep1_3, null, 1.0, 1.0, false, false);
        DoubleMatrix2D upSigma2 = upSigma1.zMult(kalmanGain, null, 1.0, 1.0, false, true);
        DoubleMatrix2D updatedSigma = estimatedSigma.assign(upSigma2, (v, v1) -> v - v1);
        //System.out.println("updatedSigma " + updatedSigma);


        return Tuple2.of(updatedPose, updatedSigma);
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
