package edu.tuberlin.dbpro.ws19.ekfslam.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CSVMerger {
    public static void main(String args[]) throws IOException {
        BufferedReader csvReader = new BufferedReader(new FileReader("src/main/resources/time_steering_speed_aa3_dr.csv"));
        ArrayList<String[]> largeCSV = new ArrayList<String[]>();

        String row;
        while ((row = csvReader.readLine()) != null ){
            System.out.print(row);
            largeCSV.add(row.split(","));
        }

        BufferedReader csvReader2 = new BufferedReader(new FileReader("src/main/resources/time_lat_lon_aa3_gpsx.csv"));

        while (csvReader2.readLine() != null ){
            largeCSV.add(csvReader2.readLine().split(","));
        }

        Collections.sort(largeCSV, new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                return o1[0].compareTo(o2[0]);
            }
        });

        FileWriter csvWriter = new FileWriter("src/main/resources/gps_and_odo");

        for (String[] str : largeCSV) {
            for(String s : str){
                csvWriter.append(String.join(",",s));
            }
            csvWriter.append("\n");
        }

        csvWriter.flush();
        csvWriter.close();

    }
}
