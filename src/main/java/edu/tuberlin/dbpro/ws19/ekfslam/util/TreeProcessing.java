package edu.tuberlin.dbpro.ws19.ekfslam.util;

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple5;

import java.util.ArrayList;

public class TreeProcessing {
    public static Double getCoordinate(Double degrees, Double gamma, Double phi, Boolean isX) {
        if (isX) {
            double x = gamma * Math.cos(Math.toRadians(degrees) - Math.toRadians(90) + phi) + 3.78;
            return x;
        } else {
            double y = gamma * Math.sin(Math.toRadians(degrees) - Math.toRadians(90) + phi) + 0.5;
            return y;
        }
    }
    private static ArrayList<ArrayList<Integer>> getIndices(Double[] input){
        Double rangeTolerance = 1.1;
        ArrayList<ArrayList<Integer>> indicesArr = new ArrayList<>();
        ArrayList<Integer> cluster = new ArrayList<>();
        cluster.add(0);
        for (int i = 1; i < input.length; i++){
            //if(input[i] - input[i-1] >= Math.abs(1.0) || input[i-1] - input[i] >= Math.abs(1.0)) {
            if(Math.abs(input[i] - input[i-1]) > rangeTolerance) {
                //System.out.println("Here:" + i);
                indicesArr.add(cluster);
                cluster = new ArrayList<>();
                cluster.add(i);
            }else{
                cluster.add(i);
            }
        }
        indicesArr.add(cluster);
//        for (ArrayList<Integer> a: indicesArr) {
//            for (Integer b: a) {
//                System.out.print(b);
//            }
//            System.out.println("Next");
//        }
//        System.out.println(indicesArr.get(0).get(0));
        return indicesArr;
    }
    public static ArrayList<ArrayList<Integer>> removeZeroClusters(Double[] input){
        ArrayList<ArrayList<Integer>> clustered = getIndices(input);
        ArrayList<ArrayList<Integer>> filtered = new ArrayList<>();
        for (ArrayList<Integer> ints : clustered){
            Double sum = 0.0;
            for (Integer i : ints) {
                sum += input[i];
            }
            if(sum > 0.0){
                //System.out.println("Add");
                filtered.add(ints);
            }
        }
        return filtered;
    }
    public static ArrayList<Double> deltaBeta(Double[] input){
        ArrayList<ArrayList<Integer>> trees = removeZeroClusters(input);
        ArrayList<Double> deltas = new ArrayList<>();
        for (ArrayList<Integer> trunk : trees){
            Double delta = ( trunk.get(trunk.size()-1) - trunk.get(0) + 1 ) * Math.PI / 360;
            deltas.add(delta);
            //System.out.println(trunk.get(0) + " First index; " + trunk.get(trunk.size()-1) + " Second index; " + delta);
        }
        return deltas;
    }
    public static ArrayList<Double> averageDistance(Double[] input){
        ArrayList<ArrayList<Integer>> trees = removeZeroClusters(input);
        ArrayList<Double> averages = new ArrayList<>();
        for (ArrayList<Integer> trunk : trees) {
            Double sum = 0.0;
            for (Integer i : trunk) {
                sum += input[i];
            }
            //System.out.println(trunk.get(trunk.size()-1) + " Last; " + trunk.get(0) + " First");
            Integer divider = ( trunk.get(trunk.size()-1) - trunk.get(0) + 1 );
            Double average = sum / divider;
            averages.add(average);
            //System.out.println(sum + " Sum; " + average + "; Divider " + divider);
        }
        return averages;
    }
    public static ArrayList<Double> getDiameters(Double[] input){
        ArrayList<Double> deltas = deltaBeta(input);
        ArrayList<Double> averages = averageDistance(input);
        if(deltas.size() != averages.size()){
            throw new IllegalStateException("Diameters cannot be calculated!");
        }
        ArrayList<Double> diameters = new ArrayList<>();
        for (int i = 0; i < deltas.size(); i++) {
            Double diameter = deltas.get(i) * averages.get(i);
            //System.out.println(diameter);
            diameters.add(diameter);
        }
        return diameters;
    }
    public static ArrayList<ArrayList<Integer>> getTrees(Double[] input){
        return removeZeroClusters(input);
    }
    /*
    Return trees as an X and Y coordinate with the diameter of the tree as a triple
     */
    private static ArrayList<Tuple5> getSingleCoordinateTrees(Double[] input, Double phi){
        ArrayList<ArrayList<Integer>> trees = getTrees(input);
        ArrayList<Double> deltas = deltaBeta(input);
        ArrayList<Double> diameters = getDiameters(input);
        ArrayList<Tuple5> singleCoordinateTrees = new ArrayList<>();
        for (int i = 0; i < trees.size(); i++) {
            //System.out.println("Degrees by index: " + trees.get(i).get(0)*0.5 + "; Half deltaBeta to Degree: " + Math.toDegrees(deltas.get(i)/2));
            Double degrees = trees.get(i).get(0)*0.5 + Math.toDegrees(deltas.get(i)/2);
            Double minDistance = 1000000.0;
            for (int j = 0; j < trees.get(i).size(); j++) {
                if(input[trees.get(i).get(j)] < minDistance){
                    minDistance = input[trees.get(i).get(j)];
                }
            }
            Double diameter = diameters.get(i);
            Double radius = diameter/2;
            Double distanceToCentre = minDistance + radius;
            //System.out.println("minDistance: " + minDistance + "; Radius: " + radius);
            Double xCoordinate = getCoordinate(degrees, distanceToCentre, phi, true);
            Double yCoordinate = getCoordinate(degrees, distanceToCentre, phi, false);
            Tuple5 tuple = new Tuple5(xCoordinate,yCoordinate,diameter, Math.sqrt(xCoordinate*xCoordinate+yCoordinate*yCoordinate), Math.atan2(yCoordinate, xCoordinate) - phi);
            singleCoordinateTrees.add(tuple);
            //System.out.println(tuple.toString());
        }
        return singleCoordinateTrees;
    }private static Double[] fitTreeData(Double[] input) {
        Double[] adjust15 = input;
        for (int i = 0; i < input.length; i++) {
            if (i<15) {
                adjust15[i] = 81.00;
            }else{
                adjust15[i] = adjust15[i]/100;
            }
        }
        return adjust15;
    }
    private static Double[] validTreesOnly(Double[] input){
        Double[] valid = input;
        for (int i = 0; i < valid.length; i++) {
            if (valid[i] <= 80.00){
                continue;
            }else{
                valid[i] = 0.0;
            }
        }
        return valid;
    }
    private static Double[] parseRawTrees(Double[] input){
        return validTreesOnly(fitTreeData(input));
    }

    /**
     * Computes trees based on KeyedDataPoint "laser" entries, with their respective x and y coordinates
     * and their diameters based on the rotation of the car, returning a tuple3 of these attributes.
     * @param input
     * @param phi
     * @return List of single point (x,y) trees with respective diameter as Tuple3
     */
    public static ArrayList<Tuple5> singleTrees(Double[] input, Double phi){
        return getSingleCoordinateTrees(parseRawTrees(input), phi);
    }
}
