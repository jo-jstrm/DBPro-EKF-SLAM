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
    public static int getTreeIndex(DoubleMatrix2D mu, DoubleMatrix2D tree){
        int index = -1;
        for (int i = 0; i < mu.size(); i += 2) {
            if(mu.get(3+i,0) == tree.get(0,0) && mu.get(3+i+1, 0) == tree.get(1,0)){
                index = i;
                break;
            }
        }
        return index/2;
    }
    public static DoubleMatrix2D makeUpdateHelperMatrix(DoubleMatrix2D mu){
        DoubleMatrix2D fx = makePredictionHelperMatrix(mu);
        return fx;
    }



    public static void main(String[] args) {
        System.out.println(makePredictionHelperMatrix(DoubleFactory2D.dense.make(9,1)));
        DoubleMatrix2D mu = new DenseDoubleMatrix2D(9,1 ).assign(new double[][]{{0},{1},{2},{2},{4},{5},{4},{3},{4}});
        System.out.println(mu);
        System.out.println(getCarCoord(mu));
        System.out.println("Index " + getTreeIndex(mu, new DenseDoubleMatrix2D(2,1).assign(new double[][]{{3},{4}})));
        System.out.println(makeUpdateHelperMatrix(mu));
    }
}
