package edu.tuberlin.dbpro.ws19.ekfslam.mark;

public class Playground {
    public static void main(String[] args) {
        //System.out.println(Math.atan2(0.0,0.0));
        double phi = 9.0472;
        System.out.println("Old Phi " + phi + "; Modulo 2*PI " + phi%(Math.PI*2));
        if(phi > Math.PI*2 && phi > 0.0){
            phi = phi - Math.PI*2;
        }else if(phi < -Math.PI*2 && phi < 0.0){
            phi = phi + Math.PI*2;
        }
        System.out.println("New Phi " + phi);
        System.out.println(Math.tan(-4.712578349645257));
    }
}
