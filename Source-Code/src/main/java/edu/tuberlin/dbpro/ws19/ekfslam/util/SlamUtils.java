package edu.tuberlin.dbpro.ws19.ekfslam.util;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.DoubleMatrix2DProcedure;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple5;
import scala.Int;

import java.util.ArrayList;

public class SlamUtils {
    public static DoubleMatrix2D addTree(DoubleMatrix2D mu, Tuple2 input){
        double [][] inputArr = {{(double) input.f0 + mu.get(0,0)},{(double) input.f1 + mu.get(1,0)}};
        DoubleMatrix2D copy = new SparseDoubleMatrix2D(2,1).assign(inputArr);
        return DoubleFactory2D.sparse.appendRows(mu, copy);
    }
    public static DoubleMatrix2D expandCovMatrix(DoubleMatrix2D cov){
        DoubleMatrix2D columnExpansion = new SparseDoubleMatrix2D(cov.rows(), 2);
        DoubleMatrix2D rowExpansion = new SparseDoubleMatrix2D(2, cov.columns()+2);
        DoubleMatrix2D newCov = DoubleFactory2D.sparse.appendRows(DoubleFactory2D.sparse.appendColumns(cov, columnExpansion), rowExpansion);
        newCov.set(newCov.rows()-2, newCov.rows()-2, 1000000);
        newCov.set(newCov.rows()-1, newCov.rows()-1, 1000000);
        return newCov;
    }
    public static DoubleMatrix2D addTree(DoubleMatrix2D mu, Tuple5 input){
        return addTree(mu, Tuple2.of(input.f0, input.f1));
    }

    /**
     * Generates a 2x1 DoubleMatrix with range and bearing for an observed tree
     * @param input
     * @return 2x1 DoubleMatrix with range and bearing in radians
     */
    public static DoubleMatrix2D getObservationModelTree(Tuple5 input){
        return DoubleFactory2D.sparse.make(2,1).assign(new double[][]{{(double) input.f3},{(double) input.f4}});
    }
    public static DoubleMatrix2D tupleToLandmark(Tuple5 input){
        return DoubleFactory2D.sparse.make(2,1).assign(new double[][]{{(double) input.f0},{(double) input.f1}});
    }
    public static DoubleMatrix2D getLastTree(DoubleMatrix2D mu){
        return mu.viewSelection(new int[]{mu.rows()-2, mu.rows()-1},null);
    }
    public static DoubleMatrix2D getCarCoord(DoubleMatrix2D mu){
        return mu.viewSelection(new int[]{0,1}, null);
    }
    public static DoubleMatrix2D makePredictionHelperMatrix(DoubleMatrix2D mu){
        DoubleMatrix2D zeros = DoubleFactory2D.sparse.make(3, mu.size());
        for (int i = 0; i < 3; i++) {
            zeros.set(i,i, 1);
        }
        return zeros;
    }
    public static int getTreeIndex(DoubleMatrix2D mu, DoubleMatrix2D tree){
        int index = -2;
        for (int i = 3; i < mu.size(); i += 2) {
            if(mu.get(i,0) == tree.get(0,0) && mu.get(i+1, 0) == tree.get(1,0)){
                index = i;
                break;
            }
        }
        return index/2;
    }
    public static DoubleMatrix2D makeUpdateHelperMatrix(DoubleMatrix2D mu, Integer index) throws Exception {

        //System.out.println("Index " + index);
        DoubleMatrix2D fx = makePredictionHelperMatrix(mu);
        //System.out.println("fx " + fx);
        DoubleMatrix2D fxLowerRows = new SparseDoubleMatrix2D(2, fx.columns());
        //System.out.println("fxLowerRows " + fxLowerRows);
        fxLowerRows.set(0, index*2+1, 1);
        //System.out.println("fxLowerRows " + fxLowerRows);
        fxLowerRows.set(1, index*2+2, 1);
        return DoubleFactory2D.sparse.appendRows(fx, fxLowerRows);
    }
    public static DoubleMatrix2D makeUpdateJacobian(DoubleMatrix2D q, DoubleMatrix2D delta){
        //preparing values for the jacobian matrix, based on Freiburg uni slides pages 39
        //naming: r1 equals Row 1 (or 0 in indices) and C1 equals Column 1 (or 0 in indices)
        Double r1C1 = -Math.sqrt(q.get(0,0))*delta.get(0,0)/q.get(0,0);
        Double r1C2 = -Math.sqrt(q.get(0,0))*delta.get(1,0)/q.get(0,0);
        Double r1C3 = 0.0;
        Double r1C4 = Math.sqrt(q.get(0,0))*delta.get(0,0)/q.get(0,0);
        Double r1C5 = Math.sqrt(q.get(0,0))*delta.get(1,0)/q.get(0,0);

        Double r2C1 = delta.get(1,0)/q.get(0,0);
        Double r2C2 = -delta.get(0,0)/q.get(0,0);
        Double r2C3 = -q.get(0,0)/q.get(0,0);
        Double r2C4 = -delta.get(1,0)/q.get(0,0);
        Double r2C5 = delta.get(0,0)/q.get(0,0);

        double[][] jacobianArr = {{r1C1, r1C2, r1C3, r1C4, r1C5}, {r2C1, r2C2, r2C3, r2C4, r2C5}};
        return new SparseDoubleMatrix2D(2, 5).assign(jacobianArr);
    }

    /**
     * This function, given an observation compares it to the already saved landmarks,
     * and if one already exists returns the original, whereas when there is no similar landmark
     * the function returns null.
     * @param mu
     * @param tree
     * @return The original landmark or null if no similar landmark available
     */
    public static Tuple2<DoubleMatrix2D, Integer> existingReferredLandmark(DoubleMatrix2D mu, Tuple5 tree){
        DoubleMatrix2D landmark = tupleToLandmark(tree);
        DoubleMatrix2D referredLandmark = null;
        Integer index = -2;
        double lowerLimit = 0.4;
        double upperLimit = 2.0;
        for (int i = 3; i < mu.size(); i += 2) {
            double distance = Math.sqrt(Math.pow(mu.get(i,0)-landmark.get(0,0),2)+Math.pow(mu.get(i+1,0)-landmark.get(1,0),2));
            //System.out.println("Euclidean distance: " + Math.sqrt(Math.pow(mu.get(i,0)-landmark.get(0,0),2)+Math.pow(mu.get(i+1,0)-landmark.get(1,0),2)));
            if (distance > lowerLimit && distance < upperLimit){
                index = -10;
            }else if (distance < lowerLimit){
                referredLandmark = new SparseDoubleMatrix2D(2,1).assign(new double[][]{{mu.get(i,0)},{mu.get(i+1, 0)}});
                index = getTreeIndex(mu, referredLandmark);
                break;
            }
        }
        return Tuple2.of(referredLandmark, index);
    }
    public static ArrayList<Tuple2<Tuple5, Tuple2<DoubleMatrix2D, Integer>>> mapObservations(ArrayList<Tuple5> input, DoubleMatrix2D mu){
        ArrayList<Tuple2<Tuple5, Tuple2<DoubleMatrix2D, Integer>>> mapped = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            mapped.add(i, Tuple2.of(input.get(i), existingReferredLandmark(mu, input.get(i))));
        }
        return mapped;
    }



    public static void main(String[] args) throws Exception {
        System.out.println(makePredictionHelperMatrix(DoubleFactory2D.sparse.make(9,1)));
        DoubleMatrix2D mu = new SparseDoubleMatrix2D(9,1 ).assign(new double[][]{{0},{1},{2},{20},{-12},{24},{10},{10},{4}});
        System.out.println(mu);
        System.out.println(getCarCoord(mu));
        System.out.println("Index " + getTreeIndex(mu, new SparseDoubleMatrix2D(2,1).assign(new double[][]{{3},{4}})));


        DoubleMatrix2D cov = DoubleFactory2D.sparse.make(3,3,1);
        System.out.println("Cov " + cov);
        DoubleMatrix2D covExpanded = expandCovMatrix(cov);
        System.out.println("covExpanded " + covExpanded);

        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<------------------->>>>>>>>>>>>>>>>>>>>");
        Tuple5 tree = Tuple5.of(9.8, 4.1, 0.1, 0, 0);
        System.out.println(existingReferredLandmark(mu, tree));

        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<------------------->>>>>>>>>>>>>>>>>>>>");
        //  0,0,0,19.67809,-12.37402
        DoubleMatrix2D state = new SparseDoubleMatrix2D(5,1).assign(new double[][]{{0},{0},{0},{19.67809},{-12.37402}});
        DoubleMatrix2D oldTree = new SparseDoubleMatrix2D(2,1).assign(new double[][]{{19.67809},{-12.37402}});
        System.out.println(makeUpdateHelperMatrix(state ,getTreeIndex(state, oldTree)));
    }
}
