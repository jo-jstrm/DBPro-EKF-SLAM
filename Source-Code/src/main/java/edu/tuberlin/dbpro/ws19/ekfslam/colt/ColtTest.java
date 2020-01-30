package edu.tuberlin.dbpro.ws19.ekfslam.colt;

//documentation source: https://dst.lbl.gov/ACSSoftware/colt/api/index.html
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;

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
        /*
        zMult on MatrixA is transposed and indicated by the "b: true" argument, lets you transpose matrixB also, and multiply A with v and B with v1
        in Addition returns result matrixC, which must be given as an argument "null" if to be created automatically
         */
        System.out.println(matrixA.zMult(matrixB, null, 1.0,1.0, true, false));
        //1172629399.28125
        System.out.println(Long.parseLong("117262939928125"));
        Date d = new Date();
        String date = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (Long.parseLong("1172629399281")));
        System.out.println(date);

        System.out.print("cos: "+Math.cos(6.283));

        double[][] source = {{2.5, 8.9, 1.0},{4.5,2.4, 1.0},{1.0,1.0,1.0}};
        DoubleMatrix2D sourced = new DenseDoubleMatrix2D(3,3).assign(source);
        System.out.println("Sourced " + sourced);
        DoubleMatrix2D inverse = new Algebra().inverse(sourced);
        System.out.println("inverse " + inverse);
    }
}
