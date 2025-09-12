package com.nnfs.layers;

import java.util.Random;

public class Dense {
    public final int nIn, nOut;
    public double[][] W;
    public double[] b;

    public Dense(int nIn, int nOut, double weightScale, long seed){
        this.nIn = nIn; this.nOut = nOut;
        W = new double[nIn][nOut];
        b = new double[nOut];
        Random rnd = new Random(seed);
        for (int i=0;i<nIn;i++)
            for (int j=0;j<nOut;j++)
                W[i][j] = rnd.nextGaussian() * weightScale;
    }
}