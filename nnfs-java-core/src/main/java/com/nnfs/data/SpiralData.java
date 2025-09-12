package com.nnfs.data;

import java.util.Random;

public final class SpiralData {
    private SpiralData(){}

    public static double[][][] make(int pointsPerClass, int classes, long seed){
        java.util.Random rnd = new Random(seed);
        int n = pointsPerClass * classes;
        double[][] X = new double[n][2];
        int[] y = new int[n];
        for (int classNumber=0; classNumber<classes; classNumber++){
            double r = 0.0;
            double t = classNumber * 4.0;
            for (int i=0;i<pointsPerClass;i++){
                int ix = pointsPerClass*classNumber + i;
                r = (double)i / pointsPerClass;
                double noise = rnd.nextGaussian() * 0.2;
                double theta = t + r * 4.0 + noise;
                X[ix][0] = r * Math.sin(theta);
                X[ix][1] = r * Math.cos(theta);
                y[ix] = classNumber;
            }
        }
        return new double[][][]{ X, toOneHot(y, classes) };
    }

    public static int[] labels(int pointsPerClass, int classes){
        int n = pointsPerClass * classes;
        int[] y = new int[n];
        for (int c=0;c<classes;c++)
            for (int i=0;i<pointsPerClass;i++)
                y[c*pointsPerClass+i]=c;
        return y;
    }

    public static double[][] toOneHot(int[] y, int classes){
        double[][] Y = new double[y.length][classes];
        for (int i=0;i<y.length;i++) Y[i][y[i]] = 1.0;
        return Y;
    }
}