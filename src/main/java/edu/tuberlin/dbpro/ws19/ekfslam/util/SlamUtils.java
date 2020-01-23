package edu.tuberlin.dbpro.ws19.ekfslam.util;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.DoubleMatrix2DProcedure;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;

import java.util.ArrayList;

public class SlamUtils {
    public static DoubleMatrix2D addTree(DoubleMatrix2D mu, Tuple2 input){
        double [][] inputArr = {{(double) input.f0 + mu.get(0,0)},{(double) input.f1 + mu.get(1,0)}};
        DoubleMatrix2D copy = new DenseDoubleMatrix2D(2,1).assign(inputArr);
        return DoubleFactory2D.dense.appendRows(mu, copy);
    }
    public static DoubleMatrix2D expandCovMatrix(DoubleMatrix2D cov){
        DoubleMatrix2D columnExpansion = new DenseDoubleMatrix2D(cov.rows(), 2);
        DoubleMatrix2D rowExpansion = new DenseDoubleMatrix2D(2, cov.columns()+2);
        return DoubleFactory2D.dense.appendRows(DoubleFactory2D.dense.appendColumns(cov, columnExpansion), rowExpansion);
    }
    public static DoubleMatrix2D addTree(DoubleMatrix2D mu, Tuple3 input){
        return addTree(mu, Tuple2.of(input.f0, input.f1));
    }
    public static DoubleMatrix2D getLastTree(DoubleMatrix2D mu){
        return mu.viewSelection(new int[]{mu.rows()-2, mu.rows()-1},null);
    }
    public static DoubleMatrix2D getCarCoord(DoubleMatrix2D mu){
        return mu.viewSelection(new int[]{0,1}, null);
    }
    public static DoubleMatrix2D makePredictionHelperMatrix(DoubleMatrix2D mu){
        DoubleMatrix2D zeros = DoubleFactory2D.dense.make(3, mu.size());
        for (int i = 0; i < 3; i++) {
            zeros.set(i,i, 1);
        }
        return zeros;
    }
    private static int getTreeIndex(DoubleMatrix2D mu, DoubleMatrix2D tree){
        int index = -1;
        for (int i = 0; i < mu.size(); i += 2) {
            if(mu.get(3+i,0) == tree.get(0,0) && mu.get(3+i+1, 0) == tree.get(1,0)){
                index = i;
                break;
            }
        }
        return index/2;
    }
    public static DoubleMatrix2D makeUpdateHelperMatrix(DoubleMatrix2D mu, DoubleMatrix2D tree){
        int index = getTreeIndex(mu, tree);
        DoubleMatrix2D fx = makePredictionHelperMatrix(mu);
        DoubleMatrix2D fxLowerRows = new DenseDoubleMatrix2D(2, fx.columns());
        fxLowerRows.set(0, index*2+3, 1);
        fxLowerRows.set(1, index*2+1+3, 1);
        return DoubleFactory2D.dense.appendRows(fx, fxLowerRows);
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
        return new DenseDoubleMatrix2D(2, 5).assign(jacobianArr);
    }



    public static void main(String[] args) {
        System.out.println(makePredictionHelperMatrix(DoubleFactory2D.dense.make(9,1)));
        DoubleMatrix2D mu = new DenseDoubleMatrix2D(9,1 ).assign(new double[][]{{0},{1},{2},{2},{4},{3},{4},{3.1},{4}});
        System.out.println(mu);
        System.out.println(getCarCoord(mu));
        System.out.println("Index " + getTreeIndex(mu, new DenseDoubleMatrix2D(2,1).assign(new double[][]{{3},{4}})));
        System.out.println(makeUpdateHelperMatrix(mu, new DenseDoubleMatrix2D(2,1).assign(new double[][]{{3},{4}})));

        DoubleMatrix2D cov = DoubleFactory2D.dense.make(3,3,1);
        System.out.println("Cov " + cov);
        DoubleMatrix2D covExpanded = expandCovMatrix(cov);
        System.out.println("covExpanded " + covExpanded);
    }
}
