package com.calculator.aa.calc;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Calc {

    private static Object lock = new Object();

    private static double[] yields(double[] values) {
        int length = values.length;
        double[] yields = new double[length - 1];

        for (int i = 1; i < length; i++) {
            double prev = values[i - 1];
            double curr = values[i];

            if (prev <= 0.0f || curr <= 0.0f) {
                return yields;
            }

            double divided = curr / prev;
            if (divided == Double.POSITIVE_INFINITY) {
                return yields;
            }

            yields[i - 1] = Math.log(divided);
        }

        return yields;
    }

    private static double correlation(double[] y1, double[] y2) {
        double avy1 = Arrays.stream(y1).average().orElse(0.0);
        double avy2 = Arrays.stream(y2).average().orElse(0.0);
        int length = y1.length;

        double sum1 = 0.0;
        double sum21 = 0.0;
        double sum22 = 0.0;

        for (int i = 0; i < length; i++) {
            double yv1d = y1[i] - avy1;
            double yv2d = y2[i] - avy2;

            sum1 += yv1d * yv2d;
            sum21 += yv1d * yv1d;
            sum22 += yv2d * yv2d;
        }

        return sum1 / (Math.sqrt(sum21 * sum22));
    }

    private static double covariance(double[] y1, double[] y2) {
        double avy1 = Arrays.stream(y1).average().orElse(0.0);
        double avy2 = Arrays.stream(y2).average().orElse(0.0);
        int length = y1.length;

        double sum = 0.0;

        for (int i = 0; i < length; i++) {
            double yv1d = y1[i] - avy1;
            double yv2d = y2[i] - avy2;

            sum += yv1d * yv2d;
        }

        return sum / length;
    }

    private static double[] multSquareMatrixAndVector(double[][] matrix, double[] vector) {
        int length = vector.length;
        double[] result = new double[length];

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                result[i] += matrix[i][j] * vector[j];
            }
        }

        return result;
    }

    private static double multVectors(double[] v1, double[] v2) {
        double sum = 0.0;
        int length = v1.length;
        for (int i = 0; i < length; i++) {
            sum += v1[i] * v2[i];
        }

        return sum;
    }

    public static double[] column(double[][] values, int col) {
        int length = values.length;
        double[] result = new double[length];
        for (int i = 0; i < length; i++) {
            result[i] = values[i][col];
        }
        return result;
    }

    public static double averageYields(double[] values) {
        return Arrays.stream(yields(values)).average().orElse(0.0);
    }

    public static double stdevYields(double[] values) {

        double sum2 = 0.0;
        double[] yields = yields(values);
        double average = averageYields(values);
        int length = yields.length;

        for (double yield : yields) {
            double difference = (yield - average);
            sum2 += difference * difference;
        }

        return Math.sqrt(1.0 / (length - 1) * sum2);
    }

    public static double[][] correlationTable(double[][] values) {

        int cols = values[0].length;
        double[][] corrTable = new double[cols][cols];

        for (int col1 = 0; col1 < cols; col1++) {
            for (int col2 = col1; col2 < cols; col2++) {
                double[] valuesC1 = column(values, col1);
                double[] valuesC2 = column(values, col2);

                double[] y1 = yields(valuesC1);
                double[] y2 = yields(valuesC2);

                corrTable[col1][col2] = correlation(y1, y2);
                corrTable[col2][col1] = corrTable[col1][col2];
            }
        }

        return corrTable;
    }

    public static double[][] covarianceTable(double[][] values) {

        int cols = values[0].length;
        double[][] covTable = new double[cols][cols];

        for (int col1 = 0; col1 < cols; col1++) {
            for (int col2 = col1; col2 < cols; col2++) {
                double[] valuesC1 = column(values, col1);
                double[] valuesC2 = column(values, col2);

                double[] y1 = yields(valuesC1);
                double[] y2 = yields(valuesC2);

                covTable[col1][col2] = covariance(y1, y2);
                covTable[col2][col1] = covTable[col1][col2];
            }
        }

        return covTable;
    }

    private static double portfolioYield(double[] averageYields, double[] weights) {
        return multVectors(averageYields, weights);
    }

    private static double portfolioRisk(double[][] covariations, double[] averageYields, double[] weights) {
        return Math.sqrt(multVectors(multSquareMatrixAndVector(covariations, averageYields), weights));
    }

    private static Portfolio portfolio(double[][] covariations, double[] averageYields, double[] weights) {
        return new Portfolio(
                new DoublePoint(
                        portfolioRisk(covariations, averageYields, weights),
                        portfolioYield(averageYields, weights)),
                weights);
    }

    public static ArrayList<Portfolio> iteratePortfolios(double[][] covariations, double[] averageYields,
                                                         int[] minimals, int[] maximals, int divStep) {
        ArrayList<Portfolio> result = new ArrayList<>();

        int length = averageYields.length;

        for (int i = 0; i < length; i++) {
            int min = minimals[i];
            int max = maximals[i];

            if (min < 0 || min > max || max > 100) {
                return result;
            }
        }

        int[] weights = new int[length];
        System.arraycopy(minimals, 0, weights, 0, length);

        iteratePortfolioHelper(covariations, averageYields, minimals, maximals, 100 / divStep, weights, 0, result);

        return result;
    }

    private static int sumIntArray(int[] array) {
        int sum = 0;
        for (int i : array) {
            sum += i;
        }
        return sum;
    }

    private static void iteratePortfolioHelper(double[][] covariations, double[] averageYields,
                                                    int[] minimals, int[] maximals, int step,
                                                    int[] weights, int index, ArrayList<Portfolio> acc) {
        if (sumIntArray(weights) > maximals[index] + step) {
            return;
        }

        for (int w = weights[index]; w < maximals[index] + step; w += step) {
            weights[index] = w;

            // clear tail
            System.arraycopy(minimals, index + 1, weights, index + 1, weights.length - (index + 1));

            int sum = sumIntArray(weights);
            // todo: check intervals!
            if (sum == 100) {
                acc.add(
                        portfolio(covariations, averageYields, Arrays.stream(weights).mapToDouble(d -> d / 100.0).toArray())
                );
            }

            if (index < weights.length - 1) {
                iteratePortfolioHelper(covariations, averageYields, minimals, maximals, step, weights, index + 1, acc);
            }
        }
    }

    public static String formatPercent(double f) {
        return String.format("%.2f%%", f * 100);
    }

}
