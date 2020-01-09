package edu.tuberlin.dbpro.ws19.ekfslam.mark;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;

import java.util.ArrayList;

public class Test {
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
    private static ArrayList<ArrayList<Integer>> removeZeroClusters(Double[] input){
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
    public Double getCoordinate(Double degrees, Double gamma, Double phi, Boolean isX) {
        if (isX) {
            double x = gamma * Math.cos(Math.toRadians(degrees) - Math.toRadians(90) + phi) + 3.78;
            return x;
        } else {
            double y = gamma * Math.sin(Math.toRadians(degrees) - Math.toRadians(90) + phi) + 0.5;
            return y;
        }
    }
    public static ArrayList<Double> deltaBeta(Double[] input){
        ArrayList<ArrayList<Integer>> trees = removeZeroClusters(input);
        ArrayList<Double> deltas = new ArrayList<>();
        for (ArrayList<Integer> trunk : trees){
            Double delta = ( trunk.get(trunk.size()-1) - trunk.get(0) + 1 ) * Math.PI / 360;
            deltas.add(delta);
            System.out.println(trunk.get(0) + " First index; " + trunk.get(trunk.size()-1) + " Second index; " + delta);
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
            System.out.println(trunk.get(trunk.size()-1) + " Last; " + trunk.get(0) + " First");
            Integer divider = ( trunk.get(trunk.size()-1) - trunk.get(0) + 1 );
            Double average = sum / divider;
            averages.add(average);
            System.out.println(sum + " Sum; " + average + "; Divider " + divider);
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
            System.out.println(diameter);
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
    public static ArrayList<Tuple3> getSingleCoordinateTrees(Double[] input){
        ArrayList<ArrayList<Integer>> trees = getTrees(input);
        ArrayList<Double> deltas = deltaBeta(input);
        ArrayList<Double> averages = averageDistance(input);



    }
    public static void main(String[] args) {

        //Double[] input = {0.0,0.0,22.0,22.5,2.5,2.0,0.0,4.0,4.3,8.0};
        //Double[] input = {1.2,1.3,1.3,0.0,0.0,222.0,221.0,0.0,1.0};
        Double[] input = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.280001, 20.290001, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 29.469999, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 8.7, 8.53, 8.53, 8.55, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 28.559999, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 12.69, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 23.719999, 23.799999, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 24.290001, 24.16, 24.18, 24.24, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 12.57, 12.46, 12.47, 12.51, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.11, 3.08, 3.09, 3.13, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        ArrayList<ArrayList<Integer>> result = removeZeroClusters(input);
        for (ArrayList<Integer> a: result) {
            for (Integer b: a) {
                //System.out.print(b + ", ");
                System.out.print(input[b] + ", ");
            }
            System.out.println("\nNext");
        }
        System.out.println(result.size() + " trees found");
        //deltaBeta(input);
        //averageDistance(input);
        getDiameters(input);
    }
}
