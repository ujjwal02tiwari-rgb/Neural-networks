package com.nnfs.api;

import com.nnfs.nn.Model;

public final class WeightsLoader {
    private WeightsLoader(){}

    public static Model load(String json){
        int idxW1 = json.indexOf("\"l1\":{\"W\":");
        int idxB1 = json.indexOf("\"b\":", idxW1);
        int idxW2 = json.indexOf("\"l2\":{\"W\":");
        int idxB2 = json.indexOf("\"b\":", idxW2);

        double[][] W1 = parseMatrix(json.substring(json.indexOf('[', idxW1), json.indexOf(']', idxB1)+1));
        double[] b1 = parseVector(json.substring(json.indexOf('[', idxB1), json.indexOf(']', idxW2)+1));
        double[][] W2 = parseMatrix(json.substring(json.indexOf('[', idxW2), json.indexOf(']', idxB2)+1));
        double[] b2 = parseVector(json.substring(json.indexOf('[', idxB2), json.lastIndexOf(']')+1));

        Model m = new Model(W1.length, b1.length, b2.length, 1);
        m.l1.W = W1; m.l1.b = b1;
        m.l2.W = W2; m.l2.b = b2;
        return m;
    }

    static double[][] parseMatrix(String s){
        s = s.trim();
        if (s.startsWith(",")) s = s.substring(1).trim();
        if (s.startsWith(":")) s = s.substring(1).trim();
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        s = s.substring(start+1, end);
        String[] rows = s.split("\\],\\[");
        double[][] M = new double[rows.length][];
        for (int i=0;i<rows.length;i++){
            String row = rows[i].replace("[","").replace("]","");
            String[] parts = row.split(",");
            M[i] = new double[parts.length];
            for (int j=0;j<parts.length;j++) M[i][j] = Double.parseDouble(parts[j]);
        }
        return M;
    }
    static double[] parseVector(String s){
        s = s.trim();
        if (s.startsWith(",")) s = s.substring(1).trim();
        if (s.startsWith(":")) s = s.substring(1).trim();
        int start = s.indexOf('[');
        int end = s.indexOf(']', start);
        s = s.substring(start+1, end);
        if (s.isEmpty()) return new double[0];
        String[] parts = s.split(",");
        double[] v = new double[parts.length];
        for (int i=0;i<parts.length;i++) v[i] = Double.parseDouble(parts[i]);
        return v;
    }
}