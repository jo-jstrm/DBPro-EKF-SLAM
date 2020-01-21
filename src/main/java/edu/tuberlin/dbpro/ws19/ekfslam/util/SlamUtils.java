package edu.tuberlin.dbpro.ws19.ekfslam.util;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.DoubleMatrix2DProcedure;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;

public class SlamUtils {
    public static DoubleMatrix2D addTree(DoubleMatrix2D mu, Tuple2 input){
        double [][] inputArr = {{(double) input.f0},{(double) input.f1}};
        DoubleMatrix2D copy = new DenseDoubleMatrix2D(2,1).assign(inputArr);
        return DoubleFactory2D.dense.appendRows(mu, copy);
    }
    public static DoubleMatrix2D addTree(DoubleMatrix2D mu, Tuple3 input){
        return addTree(mu, Tuple2.of(input.f0, input.f1));
    }
    public static DoubleMatrix2D makePredictionHelperMatrix(DoubleMatrix2D mu){
        DoubleMatrix2D zeros = DoubleFactory2D.dense.make(3, mu.size());
        for (int i = 0; i < 3; i++) {
            zeros.set(i,i, 1);
        }
        return zeros;
    }

    public static void main(String[] args) {
        System.out.println(makePredictionHelperMatrix(DoubleFactory2D.dense.make(5,1)));
    }
}
