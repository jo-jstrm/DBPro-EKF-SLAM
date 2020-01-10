package edu.tuberlin.dbpro.ws19.ekfslam.colt;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import java.sql.Date;
import java.sql.Timestamp;

public class BeneColtTest {
    public static void main(String[] args) {

        DoubleMatrix2D matrix = new DenseDoubleMatrix2D(3, 4);
        int counter = 0;
        for (int i = 0; i < matrix.rows(); i++) {
            for (int j = 0; j < matrix.columns(); j++) {
                matrix.set(i, j, counter);
                counter++;
            }
        }
        Date date = new Date(Math.round(1172629399.28125*1000));
        System.out.println(date);
    }
}
