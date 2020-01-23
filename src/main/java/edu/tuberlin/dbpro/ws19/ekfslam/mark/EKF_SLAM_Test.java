package edu.tuberlin.dbpro.ws19.ekfslam.mark;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import edu.tuberlin.dbpro.ws19.ekfslam.util.SlamUtils;
import edu.tuberlin.dbpro.ws19.ekfslam.util.TreeProcessing;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;

import java.util.ArrayList;

public class EKF_SLAM_Test {
    public static double vehicleL = 2.83;
    public static double vehicleH = 0.76;
    public static double vehicleB = 0.5;
    public static double vehicleA = 3.78;
    public static void main(String[] args) {
        //Given inputs by the saved value State
        Double x_prev = 0.0;
        Double y_prev = 0.0;
        Double phi_prev = 0.0;
        phi_prev = phi_prev%(Math.PI*2);
        double[][] previous = {{x_prev}, {y_prev}, {phi_prev}};
        DoubleMatrix2D mu = DoubleFactory2D.dense.make(previous.length, 1).assign(previous);
        System.out.println("mu " + mu);
        //Motion model for the car based on EKF implementation
        Double timedif = 1.9;
        Double speed = 0.0;
        Double steering = 0.0;
        Double x_inc = timedif*(speed*Math.cos(phi_prev)-(speed/vehicleL)*Math.tan(steering)*(vehicleA*Math.sin(phi_prev)+vehicleB*Math.cos(phi_prev)));
        Double y_inc = timedif*(speed*Math.sin(phi_prev)+(speed/vehicleL)*Math.tan(steering)*(vehicleA*Math.cos(phi_prev)-vehicleB*Math.sin(phi_prev)));
        Double phi_inc = timedif*(speed/vehicleL)*Math.tan(steering);
        double[][] increments = {{x_inc}, {y_inc}, {phi_inc}};
        DoubleMatrix2D muIncrement = new DenseDoubleMatrix2D(3, 1).assign(increments);
        System.out.println("muIncrement " + muIncrement);
        //Fx matrix from the prediction step based on the uni freiburg slides to map increments to 3+2N space
        DoubleMatrix2D Fx = SlamUtils.makePredictionHelperMatrix(mu);
        System.out.println("Fx " + Fx);
        //Multiply Fx transposed with the increments from the motion model
        DoubleMatrix2D muIncrementHighDim = Fx.zMult(muIncrement, null, 1.0, 1.0, true, false);
        System.out.println("muIncrementHighDim " + muIncrementHighDim);
        //Prediction Step for mu in EKF SLAM from the uni freiburg slides page 22
        DoubleMatrix2D estimatedMu = mu.assign(muIncrementHighDim, (v, v1) -> v+v1);
        System.out.println("estimatedMu " + estimatedMu);

        //Preparing the prediction step for the covariance matrix
        //Previous covariance matrix
        DoubleMatrix2D cov_prev = DoubleFactory2D.dense.identity(3);
        System.out.println("cov_prev " + cov_prev);
        //motion model error matrix Rt
        double[][] rtArr = {{0.5,0,0},{0,0.5,0},{0,0,0.5}};
        DoubleMatrix2D Rtx = new DenseDoubleMatrix2D(3,3).assign(rtArr);
        System.out.println("Rtx " + Rtx);
        //JacobianMatrix for Gt as the covariance update step
        Double upperValue = -timedif*(speed*Math.sin(phi_prev)+(speed/vehicleL)*Math.tan(steering)*(vehicleA*Math.cos(phi_prev)-vehicleB*Math.sin(phi_prev)));
        Double lowerValue = timedif*(speed*Math.cos(phi_prev)-(speed/vehicleL)*Math.tan(steering)*(vehicleA*Math.sin(phi_prev)+vehicleB*Math.cos(phi_prev)));
        double[][] jacobianArray = {{0.0, 0.0, upperValue}, {0.0, 0.0, lowerValue}, {0.0, 0.0, 0.0}};
        DoubleMatrix2D jacobianMatrix = new DenseDoubleMatrix2D(3,3).assign(jacobianArray);
        System.out.println("jacobianMatrix " + jacobianMatrix);
        //Step1 to calculate Gt: Fx transposed multiplied by the jacobianMatrix
        DoubleMatrix2D gtStep1 = Fx.zMult(jacobianMatrix, null, 1.0, 1.0, true, false);
        System.out.println("gtStep1 " + gtStep1);
        //Step2 to calculate Gt: The result of Step1 multiplied by Fx
        DoubleMatrix2D gtStep2 = gtStep1.zMult(Fx, null, 1.0, 1.0, false, false);
        System.out.println("gtStep2 " + gtStep2);
        //Step3 to calclulate Gt: Get the identity matrix with the same size as gtStep2
        DoubleMatrix2D gtStep3 = DoubleFactory2D.dense.identity(gtStep2.rows());
        System.out.println("gtStep3 " + gtStep3);
        //Calculate Gt
        DoubleMatrix2D Gt = gtStep3.assign(gtStep2, (v, v1) -> v + v1);
        System.out.println("Gt " + Gt);

        //Calculate the estimated covariance matrix
        //First generate the error matrix Rt which needs to be mapped from 3x3 to high dimensional space with Fx
        //Step1 to calculate Rt: multiply Fx transposed with Rtx
        DoubleMatrix2D rtStep1 = Fx.zMult(Rtx, null, 1.0, 1.0, true, false);
        System.out.println("rtStep1 " + rtStep1);
        //Step2 to calculate Rt: multiply Step1 with Fx
        DoubleMatrix2D Rt = rtStep1.zMult(Fx, null, 1.0, 1.0, false, false);
        System.out.println("Rt " + Rt);
        //Second generate the increment for the estimated covariance matrix (Gt.cov_prev.GtT)
        //Step1 to calculate covIncr: Multiply Gt with the previous covariance matrix
        DoubleMatrix2D covIncrStep1 = Gt.zMult(cov_prev, null, 1.0, 1.0, false, false);
        System.out.println("covIncrStep1 " + covIncrStep1);
        //Step2 to calculate covIncr: Multiply covIncrStep1 with GtT
        DoubleMatrix2D covIncr = covIncrStep1.zMult(Gt, null, 1.0, 1.0, false, true);
        System.out.println("covIncr " + covIncr);
        //Calculate the estimated covariance matrix
        DoubleMatrix2D estimatedCov = covIncr.assign(Rt, (v, v1) -> v + v1);
        System.out.println("estimatedCov " + estimatedCov);



        //TODO: Figure out mu Expansion and Sigma Expansion
        DoubleMatrix2D workingMu = new DenseDoubleMatrix2D(estimatedMu.rows(), estimatedMu.columns()).assign(estimatedMu);
        System.out.println("workingMu " + workingMu);
        DoubleMatrix2D workingCov = new DenseDoubleMatrix2D(estimatedCov.rows(), estimatedCov.columns()).assign(estimatedCov);
        System.out.println("workingCov " + workingCov);
        //Example observation
        int[] observationRaw = {83, 84, 84, 85, 84, 84, 85, 85, 86, 86, 86, 89, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8187, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8187, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8187, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 2028, 2029, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 2947, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8187, 8191, 870, 853, 853, 855, 8191, 8191, 8183, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8183, 8187, 8191, 8187, 2856, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 1269, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 2372, 2380, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 2429, 2416, 2418, 2424, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 1257, 1246, 1247, 1251, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 311, 308, 309, 313, 8191, 8191, 8191, 8191, 8191, 8191, 8187, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191};
        Double[] observation = new Double[observationRaw.length];
        for (int i = 0; i < observationRaw.length; i++) {
            observation[i] = (double) observationRaw[i];
        }
        ArrayList<Tuple3> singleTrees = TreeProcessing.singleTrees(observation, 0.0);
        for (Tuple3 t:
             singleTrees) {
            System.out.println(t);
        }
        //SLAM Coorection Step as shown on page 43 of the uni freiburg slides
        //Observation error matrix
        double[][] qtArr = {{0.1,0.0},{0.0,0.1}};
        DoubleMatrix2D Qt = new DenseDoubleMatrix2D(2,2).assign(qtArr);
        System.out.println("Qt " + Qt);
        //for loop over all observed features
        for (Tuple3 tree: singleTrees) {
            //TODO: figure out what j = cti stands for exactly
            //TODO: figure out how to distingish new tree from old
            DoubleMatrix2D workingLandmark = null;
            if (true){
                //adding new tree to Mu
                workingMu = SlamUtils.addTree(workingMu, tree);
                System.out.println("workingMu " + workingMu);
                //set the tree being worked on
                workingLandmark = SlamUtils.getLastTree(workingMu);
                //System.out.println("workingLandmark " + workingLandmark);
            }
            //Car coordinates x and y from estimatedMu as a matrix
            DoubleMatrix2D carCoord = SlamUtils.getCarCoord(workingMu);
            System.out.println("carCoord " + carCoord);
            //Calculate delta between the working tree coordinates and the car coordinates
            DoubleMatrix2D delta = workingLandmark.assign(carCoord, (v, v1) -> v - v1);
            System.out.println("delta " + delta);
            //Calculate q by multiplying transposed delta with delta
            DoubleMatrix2D q = delta.zMult(delta, null, 1.0, 1.0, true, false);
            System.out.println("q " + q);
            //Calculate step 14 from slide 43 of uni freiburg, estimatedObservation
            DoubleMatrix2D estimatedObservation = new DenseDoubleMatrix2D(2, 1).assign(new double[][]{{Math.sqrt(q.get(0,0))},{Math.atan2(delta.get(1,0), delta.get(0,0))-workingMu.get(2,0)}});
            System.out.println("estimatedObservation " + estimatedObservation);

        }
    }
}
