package edu.tuberlin.dbpro.ws19.ekfslam.mark;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.apache.flink.api.java.tuple.Tuple3;

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
        Double speed = 0.0;

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
        DoubleMatrix2D sigma_prev = new DenseDoubleMatrix2D(3,3).assign(1.0);
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
        dataset based on the observation of trees
         */












    }
}
