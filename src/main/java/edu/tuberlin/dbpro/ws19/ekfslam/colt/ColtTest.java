package edu.tuberlin.dbpro.ws19.ekfslam.colt;

//documentation source: https://dst.lbl.gov/ACSSoftware/colt/api/index.html
import cern.colt.function.DoubleDoubleFunction;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import java.util.Date;

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

        matrixA.assign(matrixB, (v, v1) -> v+v1);

        System.out.println(matrixA);

        double[] valuesC = {1,2,3};
        DoubleMatrix1D vector = new DenseDoubleMatrix1D(valuesC);
        System.out.println(vector.get(0));
        /*
        zMult on MatrixA is transposed and indicated by the "b: true" argument, lets you transpose matrixB also, and multiply A with v and B with v1
        in Addition returns result matrixC, which must be given as an argument "null" if to be created automatically
         */
        /*System.out.println(matrixA.zMult(matrixB, null, 1.0,1.0, true, false));
        //1172629399.28125
        System.out.println(Long.parseLong("117262939928125"));
        Date d = new Date();
        String date = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (Long.parseLong("1172629399281")));
        System.out.println(date);

        System.out.print("cos: "+Math.cos(6.283));*/
    }
}
