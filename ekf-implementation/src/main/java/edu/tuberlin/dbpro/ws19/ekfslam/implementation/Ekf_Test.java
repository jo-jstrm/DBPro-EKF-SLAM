package edu.tuberlin.dbpro.ws19.ekfslam.implementation;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;

public class Ekf_Test {
    public static double vehicleL = 2.83;
    public static double vehicleH = 0.76;
    public static double vehicleB = 0.5;
    public static double vehicleA = 3.78;
    public static void main(String[] args){

        double timedif = 21.940;

        //Create Vector from previous state
        Double x_prev = 0.0;
        Double y_prev = 0.0;
        Double phi_prev = 0.0;
        double[] previous = {x_prev, y_prev, phi_prev};
        DoubleMatrix1D previousState = new DenseDoubleMatrix1D(3);
        previousState.assign(previous);
        System.out.println("previousState " + previousState);

        //Create Vector from current readings
        //:TODO: Change to appropriate movement model
        Double steering = -0.003472;
        Double speed = 2.0;

        /*motion model from the victoria park dataset
        Double x_new_inc =*/

        /*motion model from the victoria park data set*/
        Double x_new_inc = timedif*(speed*Math.cos(phi_prev)-(speed/vehicleL)*Math.tan(phi_prev)*(vehicleA*Math.sin(phi_prev)+vehicleB*Math.cos(phi_prev)));
        Double y_new_inc = timedif*(speed*Math.sin(phi_prev)+(speed/vehicleL)*Math.tan(phi_prev)*(vehicleA*Math.cos(phi_prev)-vehicleB*Math.sin(phi_prev)));
        Double phi_new_inc = timedif*(speed/vehicleL)*Math.tan(steering);
        /*motion model from the ekf-slam uni freiburg slides
        Double x_new_inc = ((-speed/steering)*Math.sin(phi_prev)+(speed/steering)*Math.sin(phi_prev+steering*timedif));
        Double y_new_inc = ((speed/steering)*Math.cos(phi_prev)-(speed/steering)*Math.cos(phi_prev+steering*timedif));
        Double phi_new_inc = (steering*timedif);*/
        double[] increment = {x_new_inc, y_new_inc, phi_new_inc};
        DoubleMatrix1D predictionIncrement = new DenseDoubleMatrix1D(3);
        predictionIncrement.assign(increment);
        System.out.println("predictionIncrement " + predictionIncrement);

        //do the prediction step by adding the motion increment to the previous state
        DoubleMatrix1D estimatedPoseVector = new DenseDoubleMatrix1D(3).assign(previousState.assign(predictionIncrement, (v, v1) -> v + v1));
        System.out.println("estimatedPoseVector " + estimatedPoseVector);



        //calculate estimatedSigma
        DoubleMatrix2D sigma_prev = new DenseDoubleMatrix2D(3,3).assign(0.0);
        System.out.println("sigma_prev " + sigma_prev);
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
        System.out.println("jacobianMatrixGt " + jacobianMatrixGt);
        /*generate Rt //TODO: figure out exact Rt*/
        double valueRt = 0.01;
        double[][] valuesRt = {{valueRt*10,0.0,0.0},{0.0, valueRt, 0.0}, {0.0,0.0,valueRt}};
        DoubleMatrix2D movementErrorMatrixRt = new DenseDoubleMatrix2D(3,3).assign(valuesRt);
        System.out.println("movementErrorMatrixRt " + movementErrorMatrixRt);
        //Matrix multiplication of Gt with previous sigma matrix (sigman_prev)
        DoubleMatrix2D sigmaStep1 = jacobianMatrixGt.zMult(sigma_prev, null, 1.0, 1.0, false, false);
        System.out.println("sigmaStep1 " + sigmaStep1);
        //Matrix multiplication of the result of Step on with the transposed jacobian matrix (Gt)T
        DoubleMatrix2D sigmaStep2 = sigmaStep1.zMult(jacobianMatrixGt, null, 1.0, 1.0, false, true);
        System.out.println("sigmaStep2 " + sigmaStep2);
        //Adding the movement Error to the estimated Sigma
        DoubleMatrix2D estimatedSigma = sigmaStep2.assign(movementErrorMatrixRt, (v, v1) -> v + v1);
        System.out.println("estimatedSigma " + estimatedSigma);

        /*Implement the update step for the motion model and jacobians of the victoria park
        dataset based on the gps data
         */
        System.out.println("-----<<<Update Step:>>>-----");
        //gpsData only gets x and y coordinates since the first entry is the key and the second the timestamp, which are bot handled vie KeyedDataPoints
        double[] gpsData = {0.0,0.0};
        DoubleMatrix1D gpsPosition = new DenseDoubleMatrix1D(2).assign(gpsData);
        System.out.println("estimatedPoseVector "+ estimatedPoseVector);
        //calculate deltaX, deltaY and deltaDelta for the observation jacobian as stated in the victoria park paper
        Double deltaX = (gpsPosition.get(0) - estimatedPoseVector.get(0));
        Double deltaY = (gpsPosition.get(1) - estimatedPoseVector.get(0));
        Double deltaDelta = (Math.sqrt((Math.pow(deltaX, 2)+Math.pow(deltaY, 2))));
        if (deltaDelta == 0.0){
            deltaDelta = 1.0;
        }
        System.out.println("deltaX: " + deltaX + "; deltaY: " + deltaY + "; deltaDelta: " + deltaDelta);
        //Calculate Jacobian matrix (H) based on observations
        Double obsJacobianRow1Column1 = (1/deltaDelta)*(-deltaX);
        Double obsJacobianRow1Column2 = (1/deltaDelta)*(-deltaY);
        Double obsJacobianRow1Column3 = 0.0;
        Double obsJacobianRow2Column1 = (deltaY/Math.pow(deltaDelta, 2));
        Double obsJacobianRow2Column2 = (-deltaX/Math.pow(deltaDelta, 2));
        Double obsJacobianRow2Column3 = -1.0;
        double[][] obsJacobian = {{obsJacobianRow1Column1,obsJacobianRow1Column2,obsJacobianRow1Column3}, {obsJacobianRow2Column1,obsJacobianRow2Column2,obsJacobianRow2Column3}};
        DoubleMatrix2D observationJacobianMatrix = new DenseDoubleMatrix2D(2,3).assign(obsJacobian);
        System.out.println("ObservationJacobianMatrix " + observationJacobianMatrix);
        //Calculate the KalmanGain with the ErrorMatrix for the measurement
        //TODO: Get accurate error Matrix for the GPS readings
        double[][] gpsErrorArray = {{0.01,0.0}, {0.0,0.01}};
        DoubleMatrix2D gpsErrorMatrix = new DenseDoubleMatrix2D(2,2).assign(gpsErrorArray);
        //Step 1: Calculate the Inverse as part of the KalmanGain
        //Step 1.1: Matrix multiplication between the Jacobian and the estimated Sigma
        DoubleMatrix2D kalmanInverseStep1_1 = observationJacobianMatrix.zMult(estimatedSigma, null, 1.0, 1.0, false, false);
        System.out.println("kalmanInverseStep1_1 " + kalmanInverseStep1_1);
        //Step 1.2: Matrix multiplication between the result of Step 1.1 and the transposed Jacobian
        DoubleMatrix2D kalmanInverseStep1_2 = kalmanInverseStep1_1.zMult(observationJacobianMatrix, null, 1.0, 1.0, false, true);
        System.out.println("kalmanInverseStep1_2 " + kalmanInverseStep1_2);
        //Step 1.3: Add the observation Error to Step 1.2
        DoubleMatrix2D kalmanInvervseStep1_3 = kalmanInverseStep1_2.assign(gpsErrorMatrix, ((v, v1) -> v + v1));
        System.out.println("kalmanInvervseStep1_3 " + kalmanInvervseStep1_3);
        //Step 1.4: Calculate the inverse of the result of kalmanInverseStep1_3
        DoubleMatrix2D kalmanInverse = new Algebra().inverse(kalmanInvervseStep1_3);
        System.out.println("kalmanInverse " + kalmanInverse);
        //Step 2: Calculate the Kalman Gain
        //Step 2.1 Multiply the estimated Sigma with the transposed observation Jacobian Matrix
        DoubleMatrix2D kalmanGainStep2_1 = estimatedSigma.zMult(observationJacobianMatrix, null, 1.0, 1.0 , false, true);
        System.out.println("kalmanGainStep2_1 " + kalmanGainStep2_1);
        //Step 2.2 Multiply the results from 2.1 and 1.4 to get the Kalman gain based on the gps data and vehicle position
        DoubleMatrix2D kalmanGain = kalmanGainStep2_1.zMult(kalmanInverse, null, 1.0, 1.0, false, false);
        System.out.println("kalmanGain " + kalmanGain);

        //Calculate the correction step pose vector
        //Get deltaBetween observed and estimated position
        //trying our a different Observed Pose to Observed Position Model
        /*Double positionMinusPoseX = -gpsPosition.get(0);
        Double positionMinusPoseY = -gpsPosition.get(0);
        double [][] positionPose = {{positionMinusPoseX}, {positionMinusPoseY}};
        DoubleMatrix2D positionPoseMatrix = new DenseDoubleMatrix2D(2,1).assign(positionPose);
        System.out.println("positionPoseMatrix " + positionPoseMatrix);*/
        //TODO: Test if to subtract gsp and pose from (0,0) or from each other for mathematical correctness
        Double gpsZr = Math.sqrt(Math.pow(gpsPosition.get(0), 2)+(Math.pow(gpsPosition.get(1), 2)));
        Double gpsZB = Math.atan2(gpsPosition.get(1),gpsPosition.get(0));
        double[] gpsZArr = {gpsZr,gpsZB};
        DoubleMatrix1D gpsZVector = new DenseDoubleMatrix1D(2).assign(gpsZArr);
        System.out.println("gpsZVector " + gpsZVector);
        Double poseZr = Math.sqrt(Math.pow(estimatedPoseVector.get(0), 2)+(Math.pow(estimatedPoseVector.get(1), 2)));
        Double poseZB = Math.atan2(estimatedPoseVector.get(1),estimatedPoseVector.get(0));
        double[] poseZArr = {poseZr,poseZB};
        DoubleMatrix1D poseZVector = new DenseDoubleMatrix1D(2).assign(poseZArr);
        System.out.println("poseZVector " + poseZVector);
        DoubleMatrix1D positionPoseVector = gpsZVector.assign(poseZVector, (v, v1) -> v - v1);
        System.out.println("positionPoseVector " + positionPoseVector);
        double [][] positionPose = {{positionPoseVector.get(0)},{positionPoseVector.get(1)}};
        DoubleMatrix2D positionPoseMatrix = new DenseDoubleMatrix2D(2,1).assign(positionPose);
        //Multiply delta between observed and estimated position with the Kalman Gain
        DoubleMatrix2D kalmanGainPosition = kalmanGain.zMult(positionPoseMatrix, null, 1.0, 1.0, false, false);
        DoubleMatrix1D kalmanGainPositionVector = new DenseDoubleMatrix1D(3).assign(kalmanGainPosition.viewColumn(0));
        System.out.println("kalmanGainPositionVector " + kalmanGainPositionVector);

        //Update PoseVector/PoseMatrix
        DoubleMatrix1D updatedPose = estimatedPoseVector.assign(kalmanGainPositionVector, (v, v1) -> v + v1);
        System.out.println("updatedPose " + updatedPose);

        //Update Sigma
        DoubleMatrix2D upSigma1 = kalmanGain.zMult(kalmanInvervseStep1_3, null, 1.0, 1.0, false, false);
        DoubleMatrix2D upSigma2 = upSigma1.zMult(kalmanGain, null, 1.0, 1.0, false, true);
        DoubleMatrix2D updatedSigma = estimatedSigma.assign(upSigma2, (v, v1) -> v - v1);
        System.out.println("updatedSigma " + updatedSigma);








    }
}
