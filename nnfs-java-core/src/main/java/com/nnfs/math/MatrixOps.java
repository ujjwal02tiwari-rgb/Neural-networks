package com.nnfs.math;

public final class MatrixOps {
    private MatrixOps(){}

    public static double[][] dot(double[][] A, double[][] B) {
        int n = A.length, m = A[0].length, p = B[0].length;
        if (B.length != m) throw new IllegalArgumentException("A.cols must equal B.rows");
        double[][] C = new double[n][p];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < m; k++) {
                double a = A[i][k];
                for (int j = 0; j < p; j++) {
                    C[i][j] += a * B[k][j];
                }
            }
        }
        return C;
    }
    public static double[][] addRowVector(double[][] A, double[] b) {
        int n = A.length, m = A[0].length;
        if (b.length != m) throw new IllegalArgumentException("b.size must equal A.cols");
        double[][] C = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) C[i][j] = A[i][j] + b[j];
        }
        return C;
    }
    public static double[][] relu(double[][] X) {
        int n = X.length, m = X[0].length;
        double[][] Y = new double[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                Y[i][j] = Math.max(0.0, X[i][j]);
        return Y;
    }
    public static double[][] softmax(double[][] X) {
        int n = X.length, m = X[0].length;
        double[][] Y = new double[n][m];
        for (int i = 0; i < n; i++) {
            double max = Double.NEGATIVE_INFINITY;
            for (int j = 0; j < m; j++) max = Math.max(max, X[i][j]);
            double sum = 0.0;
            for (int j = 0; j < m; j++) { Y[i][j] = Math.exp(X[i][j] - max); sum += Y[i][j]; }
            for (int j = 0; j < m; j++) Y[i][j] /= (sum + 1e-12);
        }
        return Y;
    }
    public static int[] argmax(double[][] X){
        int n = X.length, m = X[0].length;
        int[] idx = new int[n];
        for (int i=0;i<n;i++){
            double max = -1; int arg=-1;
            for (int j=0;j<m;j++){
                if (X[i][j] > max){ max = X[i][j]; arg = j; }
            }
            idx[i]=arg;
        }
        return idx;
    }
}