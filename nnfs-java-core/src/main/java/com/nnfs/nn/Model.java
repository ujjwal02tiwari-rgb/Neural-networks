package com.nnfs.nn;

import com.nnfs.layers.Dense;
import com.nnfs.math.MatrixOps;

public class Model {
    public final Dense l1;
    public final Dense l2;

    public Model(int input, int hidden, int output, long seed){
        this.l1 = new Dense(input, hidden, 0.01, seed);
        this.l2 = new Dense(hidden, output, 0.01, seed+1);
    }

    public double[][] forward(double[][] X, double[][] outHidden){
        double[][] z1 = MatrixOps.addRowVector(MatrixOps.dot(X, l1.W), l1.b);
        double[][] a1 = MatrixOps.relu(z1);
        if (outHidden != null){
            for (int i=0;i<a1.length;i++)
                System.arraycopy(a1[i], 0, outHidden[i], 0, a1[0].length);
        }
        double[][] z2 = MatrixOps.addRowVector(MatrixOps.dot(a1, l2.W), l2.b);
        return MatrixOps.softmax(z2);
    }

    public double[][] forward(double[][] X){
        return forward(X, null);
    }
}