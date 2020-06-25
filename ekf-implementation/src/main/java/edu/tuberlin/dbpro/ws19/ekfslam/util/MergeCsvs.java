package edu.tuberlin.dbpro.ws19.ekfslam.util;

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;

public class MergeCsvs {

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/HOPEFULLY_FINAL-1.csv"));
        BufferedReader reader2 = new BufferedReader(new FileReader("src/main/resources/time_lat_lon_aa3_gpsx.csv"));
        String row;
        ArrayList<Tuple4> dataPoints = new ArrayList<>();
        while((row = reader.readLine())!= null) {
            String[] str = row.split(",");

            if (str[0].equals("odo")) {
                String key = "odo";
                long time = Math.round(Double.valueOf(str[1])*1000) + 20967;
                double speed = Double.valueOf(str[2]);
                double steering = Double.valueOf(str[3]);
                Tuple4 tuple = new Tuple4(key, time, speed, steering);
                dataPoints.add(tuple);
            }
        }

        while((row = reader2.readLine())!=null) {
            String[] str = row.split(",");
            long time = Long.valueOf(str[0]);
            double lat = Double.valueOf(str[1]);
            double lon = Double.valueOf(str[2]);
            Tuple4 tuple = new Tuple4("gps",time, lat, lon);
            dataPoints.add(tuple);
        }

        dataPoints.stream().sorted(new Comparator<Tuple4>() {
            @Override
            public int compare(Tuple4 o1, Tuple4 o2) {
                if ((long)o1.f0 < (long)o2.f0){return 1;}
                else { return 0;}
            }
        });

        BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/01_HOPEFULLY_FINAL.csv"));

        for(Tuple4 tuple : dataPoints){
            writer.write(tuple.f0+","+tuple.f1+","+tuple.f2+","+tuple.f3+"\n");
        }
    }
}
