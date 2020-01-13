package edu.tuberlin.dbpro.ws19.ekfslam.dataManipulation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StreamingJobMatchNumberOfOdoToGPS {
    public static void main(String[] args) throws Exception {
        try{
            BufferedReader reader = new BufferedReader(new FileReader("./src/main/resources/gps_and_odo_sorted.csv"), 32000);

            CSVParser csvParser = new CSVParser(reader, CSVFormat.newFormat(';'));

            //flags: (target path, append)
            BufferedWriter writer = new BufferedWriter(new FileWriter("./src/main/resources/gps_and_odo_matched.csv"),32000);
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withDelimiter(';').withRecordSeparator("\r\n"));

            CSVRecord prevRecord = null;
            Boolean isFirstEntry = true;

            /*
            int cnt = 0;
            for (CSVRecord hi : csvParser){
                if (hi.get(0).equals("gps")) cnt++;
            }
            System.out.println("GPS COUNT: "+cnt);
            */

            for (CSVRecord csvRecord: csvParser) {
                CSVRecord current = csvRecord;

                if (current.get(0).equals("gps")){
                    if(isFirstEntry || prevRecord.get(0) == "gps"){
                        //handle two consecutive gps-datapoints
                        //handle first iteration
                        csvPrinter.printRecord(current);
                        isFirstEntry = false;
                    }
                    else{
                        csvPrinter.printRecord(prevRecord);
                        csvPrinter.printRecord(current);
                    }
                }
                prevRecord = current;
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
