package edu.tuberlin.dbpro.ws19.ekfslam.functions;

import edu.tuberlin.dbpro.ws19.ekfslam.data.KeyedDataPoint;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple6;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.util.ArrayList;

/*
Instead of calculating only the movement, the ClassifyTrees class calculates the movement but instead returns the positions of trees that where previously clusters
of x and y coordinates to determine the centre of a trees trunk. Thus it enables representation of single trees given by a single position coordinate instead of a cluster.
 */
public class ClassifyTrees extends RichFlatMapFunction<KeyedDataPoint, KeyedDataPoint> {
    //LastTimestamp, Lat, Long, Theta, Speed, P
    private transient ValueState<Tuple3<Double, Double, Double>> filterParams;

    @Override
    public void flatMap(KeyedDataPoint inputPoint, Collector<KeyedDataPoint> outFilteredPointCollector) throws Exception {
        /*
        x = v*cos(theta)
        y = v*sin(theta)
        theta = v/L*tan(alpha)
         */

        Tuple3< Double, Double, Double> previousPointParams = filterParams.value();

        double x_ = previousPointParams.f0;
        double y_ = previousPointParams.f1;
        double phi_ = previousPointParams.f2;

        //time update
        Tuple4 value = (Tuple4) inputPoint.getValue();

        double xinc = (Double) value.f0;
        double yinc = (Double) value.f1;
        double phiinc = (Double) value.f2;
        Double[] laserArr = (Double[]) value.f3;



        //prediction function
        double x2 = x_ + Math.cos(phi_ + phiinc/2)*xinc - Math.sin(phi_ + phiinc/2)*yinc;
        double y2 = y_ + Math.sin(phi_ + phiinc/2)*xinc + Math.cos(phi_ + phiinc/2)*yinc;
        double phi = phi_ + phiinc;

        //work with LaserArray
//        ArrayList<Tuple2> landmarks = new ArrayList<>();
//        for(int i = 0; i<laserArr.length;i++){
//            if(laserArr[i] == 0.0 || laserArr[i] > 80.0){continue;}
//            else{
//                double degrees = i*0.5;
//                double lx = getCoordinate(degrees, laserArr[i], phi, true);
//                double ly = getCoordinate(degrees, laserArr[i], phi, false);
//
//                Tuple2 tuple = new Tuple2(lx,ly);
//                landmarks.add(tuple);
//            }
//        }

        ArrayList<Tuple3> trees = getSingleCoordinateTrees(laserArr, phi);
        //returns
        Tuple3 res = new Tuple3<Double, Double, ArrayList<Tuple3>>(x2,y2,trees);
        Tuple3 update = new Tuple3<Double, Double, Double>(x2,y2,phi);
        filterParams.update(update);

        // return filtered point
        outFilteredPointCollector.collect(new KeyedDataPoint<>("move", inputPoint.getTimeStampMs(), res));
    }

    @Override
    public void open(Configuration config) {
        ValueStateDescriptor<Tuple3<Double, Double, Double>> descriptor = new ValueStateDescriptor<Tuple3<Double, Double, Double>>(

                "full", // the state name
                TypeInformation.of(new TypeHint<Tuple3<Double, Double, Double>>() {}), // type information
                Tuple3.of(0.0, 0.0, 0.0)); // default value of the state, if nothing was set  //TODO: check this initialisation!
        filterParams = getRuntimeContext().getState(descriptor);
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
    private ArrayList<ArrayList<Integer>> getIndices(Double[] input){
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
    private ArrayList<ArrayList<Integer>> removeZeroClusters(Double[] input){
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
    public ArrayList<Double> deltaBeta(Double[] input){
        ArrayList<ArrayList<Integer>> trees = removeZeroClusters(input);
        ArrayList<Double> deltas = new ArrayList<>();
        for (ArrayList<Integer> trunk : trees){
            Double delta = ( trunk.get(trunk.size()-1) - trunk.get(0) + 1 ) * Math.PI / 360;
            deltas.add(delta);
            System.out.println(trunk.get(0) + " First index; " + trunk.get(trunk.size()-1) + " Second index; " + delta);
        }
        return deltas;
    }
    public ArrayList<Double> averageDistance(Double[] input){
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
    public ArrayList<Double> getDiameters(Double[] input){
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
    public ArrayList<ArrayList<Integer>> getTrees(Double[] input){
        return removeZeroClusters(input);
    }
    /*
    Return trees as an X and Y coordinate with the diameter of the tree as a triple
     */
    public ArrayList<Tuple3> getSingleCoordinateTrees(Double[] input, Double phi){
        ArrayList<ArrayList<Integer>> trees = getTrees(input);
        ArrayList<Double> deltas = deltaBeta(input);
        ArrayList<Double> diameters = getDiameters(input);
        ArrayList<Tuple3> singleCoordinateTrees = new ArrayList<>();
        for (int i = 0; i < trees.size(); i++) {
            System.out.println("Degrees by index: " + trees.get(i).get(0)*0.5 + "; Half deltaBeta to Degree: " + Math.toDegrees(deltas.get(i)/2));
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
            System.out.println("minDistance: " + minDistance + "; Radius: " + radius);
            Double xCoordinate = getCoordinate(degrees, distanceToCentre, phi, true);
            Double yCoordinate = getCoordinate(degrees, distanceToCentre, phi, false);
            Tuple3 tuple = new Tuple3(xCoordinate,yCoordinate,diameter);
            singleCoordinateTrees.add(tuple);
            System.out.println(tuple.toString());
        }
        return singleCoordinateTrees;
    }
}
