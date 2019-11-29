package edu.tuberlin.dbpro.ws19.ekfslam.colt;

//documentation source: https://dst.lbl.gov/ACSSoftware/colt/api/index.html
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class ColtTest {
    public static void main(String[] args){
        System.out.println("Chill :)");
        DoubleMatrix2D matrixA, matrixB;
        /*
        Creates two matrices with 3 rows and 2 columns, there is also a factory class for this (may be worth investigating)
         */
        matrixA = new DenseDoubleMatrix2D(3,2);
        matrixB = new DenseDoubleMatrix2D(3,2);
        //matrix = new SparseDoubleMatrix2D(3,4); // has same interface
        //matrix = new RCDoubleMatrix2D(3,4); // has same interface
        /*
        Assigns values to the created Matrices per row from a double [][]
         */
        double [][] valuesA = {{1.0,3.0}, {3.0,1.0}, {1.0,3.0}};
        matrixA = matrixA.assign(valuesA);
        double [][] valuesB = {{1.0,4.0}, {0.0,-1.0}, {2.0,3.0}};
        matrixB = matrixB.assign(valuesB);
        System.out.println(matrixA);
        System.out.println(matrixB);
        /*
        zMult on MatrixA is transposed and indicated by the "b: true" argument, lets you transpose matrixB also, and multiply A with v and B with v1
        in Addition returns result matrixC, which must be given as an argument "null" if to be created automatically
         */
        System.out.println(matrixA.zMult(matrixB, null, 1.0,1.0, true, false));
    }
}
