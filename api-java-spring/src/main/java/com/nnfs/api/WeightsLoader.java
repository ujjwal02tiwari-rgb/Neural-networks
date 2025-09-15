package com.nnfs.api;

import com.nnfs.nn.Model;

public final class WeightsLoader {
    private WeightsLoader() {}

    public static Model load(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Weights JSON cannot be null or empty");
        }

        int idxW1 = json.indexOf("\"l1\":{\"W\":");
        int idxB1 = json.indexOf("\"b\":", idxW1);
        int idxW2 = json.indexOf("\"l2\":{\"W\":");
        int idxB2 = json.indexOf("\"b\":", idxW2);

        if (idxW1 == -1 || idxB1 == -1 || idxW2 == -1 || idxB2 == -1) {
            throw new IllegalArgumentException("Invalid weights.json format: missing keys");
        }

        double[][] W1 = parseMatrix(
                safeSubstring(json, json.indexOf('[', idxW1), json.indexOf(']', idxB1) + 1, "W1"));
        double[] b1 = parseVector(
                safeSubstring(json, json.indexOf('[', idxB1), json.indexOf(']', idxW2) + 1, "b1"));
        double[][] W2 = parseMatrix(
                safeSubstring(json, json.indexOf('[', idxW2), json.indexOf(']', idxB2) + 1, "W2"));
        double[] b2 = parseVector(
                safeSubstring(json, json.indexOf('[', idxB2), json.lastIndexOf(']') + 1, "b2"));

        Model m = new Model(W1.length, b1.length, b2.length, 1);
        m.l1.W = W1;
        m.l1.b = b1;
        m.l2.W = W2;
        m.l2.b = b2;
        return m;
    }

    private static String safeSubstring(String s, int start, int end, String fieldName) {
        if (start < 0 || end <= start || end > s.length()) {
            throw new IllegalArgumentException("Invalid JSON structure for field: " + fieldName);
        }
        return s.substring(start, end);
    }

    static double[][] parseMatrix(String s) {
        s = cleanPrefix(s);
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start == -1 || end == -1 || start >= end) {
            return new double[0][0];
        }
        s = s.substring(start + 1, end).trim();
        if (s.isEmpty()) return new double[0][0];

        String[] rows = s.split("\\],\\[");
        double[][] M = new double[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            String row = rows[i].replace("[", "").replace("]", "").trim();
            if (row.isEmpty()) {
                M[i] = new double[0];
                continue;
            }
            String[] parts = row.split(",");
            M[i] = new double[parts.length];
            for (int j = 0; j < parts.length; j++) {
                M[i][j] = Double.parseDouble(parts[j].trim());
            }
        }
        return M;
    }

    static double[] parseVector(String s) {
        s = cleanPrefix(s);
        int start = s.indexOf('[');
        int end = s.indexOf(']', start);
        if (start == -1 || end == -1 || start >= end) {
            return new double[0];
        }
        s = s.substring(start + 1, end).trim();
        if (s.isEmpty()) return new double[0];

        String[] parts = s.split(",");
        double[] v = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            v[i] = Double.parseDouble(parts[i].trim());
        }
        return v;
    }

    private static String cleanPrefix(String s) {
        s = s.trim();
        if (s.startsWith(",")) s = s.substring(1).trim();
        if (s.startsWith(":")) s = s.substring(1).trim();
        return s;
    }
}
