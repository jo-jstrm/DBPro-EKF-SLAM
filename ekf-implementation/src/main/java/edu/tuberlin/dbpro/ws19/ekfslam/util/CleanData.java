package edu.tuberlin.dbpro.ws19.ekfslam.util;

import org.apache.flink.api.java.tuple.Tuple3;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class CleanData {
    public static void main(String[] args) throws IOException {
        //getPredictionErrors("src/main/resources/predictionErrors.csv");

        getShifts("src/main/resources/ekfSingleCar.csv");
    }

    private static void getPredictionErrors(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/predictionErrorsEdited.csv"));
        String row;
        String newline = "";
        Double predX = 0.0;
        Double predY = 0.0;
        while((row = reader.readLine()) != null){
            row = row.replaceAll(";", ",");
            String data[] = row.split(",");
            if(data[1].equals("prediction")){
                predX = Double.valueOf(data[3]);
                predY = Double.valueOf(data[4]);
                newline = row.replace("prediction,","");
            }else{
                String[] fields = row.split(",");
                newline = newline + ","+fields[3]+"," + fields[4];

                //calculate prediction error
                Double updX = Double.valueOf(fields[3]);
                Double updY = Double.valueOf(fields[4]);

                Double distance = Math.sqrt(Math.pow((predX-updX),2) + Math.pow((predY - updY),2));
                newline = newline + "," + distance;

                //write
                writer.write(newline + "\n");
            }
        }
    }

    /*
    Gets distance of how much our position changed since last step. Regardless of prediction or update
     */
    private static void getShifts(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/Shifts.csv"));

        String row = "";
        boolean firstLine = true;
        Double xPrev = 0.0;
        Double yPrev = 0.0;
        while((row = reader.readLine())!=null){
            String[] data = row.split(",");
            if(firstLine){
                xPrev = Double.valueOf(data[3]);
                yPrev = Double.valueOf(data[4]);
                firstLine =false;
                continue;
            }
            Long time = Long.valueOf(data[2]);

            Double xNew = Double.valueOf(data[3]);
            Double yNew = Double.valueOf(data[4]);

            Double distance = Math.sqrt(Math.pow((xNew-xPrev),2) + Math.pow((yNew - yPrev),2));

            String out = data[1]+","+time + "," + distance + "\n";
            writer.write(out);

            xPrev = xNew;
            yPrev = yNew;
        }
    }
}
