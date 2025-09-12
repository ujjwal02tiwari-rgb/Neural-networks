package com.nnfs.optim;

public class SGD {
    public double lr = 1e-2;
    public double l2 = 1e-4;

    public SGD(){}
    public SGD(double lr, double l2){ this.lr = lr; this.l2 = l2; }

    public void step(double[][] W, double[][] dW, double[] b, double[] db){
        int nIn = W.length, nOut = W[0].length;
        for (int i=0;i<nIn;i++){
            for (int j=0;j<nOut;j++){
                double reg = l2 * W[i][j];
                W[i][j] -= lr * (dW[i][j] + reg);
            }
        }
        for (int j=0;j<nOut;j++){
            b[j] -= lr * db[j];
        }
    }
}