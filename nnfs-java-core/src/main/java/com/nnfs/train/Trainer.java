package com.nnfs.train;

import com.nnfs.data.SpiralData;
import com.nnfs.loss.CategoricalCrossEntropy;
import com.nnfs.math.MatrixOps;
import com.nnfs.nn.Model;
import com.nnfs.optim.SGD;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class Trainer {
    public static class Grad {
        public double[][] dW1, dW2;
        public double[] db1, db2;
    }

    static Grad backward(Model model, double[][] X, double[][] a1, double[][] yPred, int[] yTrue){
        int n = X.length;
        int classes = yPred[0].length;
        double[][] dZ2 = new double[n][classes];
        for (int i=0;i<n;i++){
            for (int j=0;j<classes;j++){
                double t = (yTrue[i]==j)?1.0:0.0;
                dZ2[i][j] = yPred[i][j] - t;
            }
        }
        double[][] a1T = transpose(a1);
        double[][] dW2 = matScale(com.nnfs.math.MatrixOps.dot(a1T, dZ2), 1.0/n);
        double[] db2 = meanCols(dZ2);
        double[][] W2T = transpose(model.l2.W);
        double[][] dA1 = com.nnfs.math.MatrixOps.dot(dZ2, W2T);
        double[][] dZ1 = new double[a1.length][a1[0].length];
        for (int i=0;i<a1.length;i++)
            for (int j=0;j<a1[0].length;j++)
                dZ1[i][j] = a1[i][j] > 0 ? dA1[i][j] : 0.0;
        double[][] XT = transpose(X);
        double[][] dW1 = matScale(com.nnfs.math.MatrixOps.dot(XT, dZ1), 1.0/n);
        double[] db1 = meanCols(dZ1);

        Grad g = new Grad();
        g.dW1 = dW1; g.db1 = db1; g.dW2 = dW2; g.db2 = db2;
        return g;
    }

    static double[][] transpose(double[][] A){
        int n=A.length, m=A[0].length;
        double[][] T = new double[m][n];
        for (int i=0;i<n;i++) for (int j=0;j<m;j++) T[j][i]=A[i][j];
        return T;
    }
    static double[][] matScale(double[][] A, double s){
        int n=A.length, m=A[0].length; double[][] B=new double[n][m];
        for (int i=0;i<n;i++) for (int j=0;j<m;j++) B[i][j]=A[i][j]*s;
        return B;
    }
    static double[] meanCols(double[][] A){
        int n=A.length, m=A[0].length; double[] b=new double[m];
        for (int j=0;j<m;j++){ double sum=0; for (int i=0;i<n;i++) sum+=A[i][j]; b[j]=sum/n; }
        return b;
    }

    public static void main(String[] args) throws Exception {
        int points=100; int classes=3;
        double[][][] data = SpiralData.make(points, classes, 42);
        double[][] X = data[0];
        int[] y = SpiralData.labels(points, classes);

        Model model = new Model(2, 64, classes, 1337L);
        SGD optim = new SGD(0.1, 1e-4);

        int epochs = 4000;
        double[][] hidden = new double[X.length][64];
        for (int epoch=1; epoch<=epochs; epoch++){
            double[][] probs = model.forward(X, hidden);
            double loss = CategoricalCrossEntropy.loss(probs, y);

            Grad g = backward(model, X, hidden, probs, y);
            optim.step(model.l1.W, g.dW1, model.l1.b, g.db1);
            optim.step(model.l2.W, g.dW2, model.l2.b, g.db2);

            if (epoch % 400 == 0){
                int correct=0;
                int[] preds = com.nnfs.math.MatrixOps.argmax(probs);
                for (int i=0;i<preds.length;i++) if (preds[i]==y[i]) correct++;
                double acc = correct/(double)preds.length;
                System.out.printf("Epoch %d loss=%.4f acc=%.3f%n", epoch, loss, acc);
            }
        }
        Path mp = Path.of("model");
        Files.createDirectories(mp);
        try (FileWriter fw = new FileWriter(new File(mp.toFile(), "weights.json"))){
            fw.write(serialize(model));
        }
        System.out.println("Saved model to model/weights.json");
    }

    static String serialize(com.nnfs.nn.Model model){
        StringBuilder sb = new StringBuilder();
        sb.append("{\"l1\":{\"W\":").append(toJson(model.l1.W)).append(",\"b\":").append(toJson(model.l1.b))
          .append("},\"l2\":{\"W\":").append(toJson(model.l2.W)).append(",\"b\":").append(toJson(model.l2.b)).append("}}");
        return sb.toString();
    }
    static String toJson(double[][] A){
        StringBuilder sb = new StringBuilder("[");
        for (int i=0;i<A.length;i++){
            if (i>0) sb.append(',');
            sb.append('[');
            for (int j=0;j<A[i].length;j++){
                if (j>0) sb.append(',');
                sb.append(String.format("%.10f", A[i][j]));
            }
            sb.append(']');
        }
        sb.append(']'); return sb.toString();
    }
    static String toJson(double[] b){
        StringBuilder sb = new StringBuilder("[");
        for (int j=0;j<b.length;j++){
            if (j>0) sb.append(',');
            sb.append(String.format("%.10f", b[j]));
        }
        sb.append(']'); return sb.toString();
    }
}