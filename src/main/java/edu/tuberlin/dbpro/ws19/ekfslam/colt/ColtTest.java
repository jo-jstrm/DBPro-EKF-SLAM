package edu.tuberlin.dbpro.ws19.ekfslam.colt;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class ColtTest {
    public static void main(String[] args){
        System.out.println("Chill :)");
        DoubleMatrix2D matrix;
        matrix = new DenseDoubleMatrix2D(3,4);
//matrix = new SparseDoubleMatrix2D(3,4); // has same interface
//matrix = new RCDoubleMatrix2D(3,4); // has same interface
        System.out.println(matrix);
    }
}
