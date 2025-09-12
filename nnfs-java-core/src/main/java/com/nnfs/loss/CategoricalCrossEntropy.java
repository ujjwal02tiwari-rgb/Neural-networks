package com.nnfs.loss;

public final class CategoricalCrossEntropy {
    private CategoricalCrossEntropy(){}
    public static double loss(double[][] yPred, int[] yTrue){
        int n = yPred.length; double sum = 0.0;
        for (int i=0;i<n;i++){
            double p = Math.max(1e-12, yPred[i][yTrue[i]]);
            sum += -Math.log(p);
        }
        return sum / n;
    }
}