package com.calculator.aa.calc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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

    private static double sumProduct(double[] v1, double[] v2) {

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
        return sumProduct(averageYields, weights);
    }

    private static double portfolioRisk(double[][] correlations, double[] stdevYields, double[] weights) {

        int length = weights.length;
        double sum = 0;

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                sum += weights[i] * weights[j] * stdevYields[i] * stdevYields[j] * correlations[i][j];
            }
        }

        return Math.sqrt(sum);
    }

    private static Portfolio portfolio(double[][] correlations, double[] averageYields, double[] stdevYields, double[] weights, String[] instruments) {
        return new Portfolio(
                new DoublePoint(
                        portfolioRisk(correlations, stdevYields, weights),
                        portfolioYield(averageYields, weights)),
                weights,
                instruments);
    }

    public static List<Portfolio> iteratePortfolios(double[][] correlations, double[] averageYields,
                                                         double[] stdevYields, int[] minimals, int[] maximals,
                                                         String[] instruments, int divStep) {
        List<Portfolio> result = new LinkedList<>();
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

        iteratePortfolioHelper(correlations, averageYields, stdevYields, minimals, maximals, 100 / divStep, weights, instruments, 0, result);

        result.sort(Portfolio::compareTo);

        return result;
    }

    public static List<Portfolio> getOptimalBorder(List<Portfolio> sourceSorted) {
        List<Portfolio> result = new LinkedList<>();
        List<Portfolio> rest = new ArrayList<>(sourceSorted);

        Portfolio current = sourceSorted.get(0);
        result.add(current);

        Portfolio maxYield = sourceSorted.stream().max(Portfolio::compareToYield).orElse(current);

        while (!rest.isEmpty()) {
            rest.remove(current);
            double yield = current.yield();
            rest = rest.stream().filter(pf -> pf.yield() >= yield).collect(Collectors.toList());
            current = findMaxSine(current, rest);
            result.add(current);
            if (current == maxYield) {
                break;
            }
        }

        return result;
    }

    private static Portfolio findMaxSine(Portfolio origin, List<Portfolio> portfolios) {
        double max = Double.MIN_VALUE;
        Portfolio maxPf = portfolios.get(0);

        for (Portfolio p : portfolios) {
            if (p.risk() < origin.risk() || p.yield() < origin.yield()) {
                continue;
            }

            double c = sine(origin.performance(), p.performance());
            if (c > max) {
                max = c;
                maxPf = p;
            }
        }

        return maxPf;
    }

    private static double sine(DoublePoint from, DoublePoint to) {
        double x = to.getX() - from.getX();
        double y = to.getY() - from.getY();
        double r = Math.sqrt(x * x + y * y);

        return y / r;
    }

    private static int sumIntArray(int[] array) {
        int sum = 0;
        for (int i : array) {
            sum += i;
        }
        return sum;
    }

    private static void iteratePortfolioHelper(double[][] correlations, double[] averageYields, double[] stdevYields,
                                               int[] minimals, int[] maximals, int step,
                                               int[] weights, String[] instruments, int index, List<Portfolio> acc) {

        if (sumIntArray(weights) > 100) {
            return;
        }

        while (weights[index] <= maximals[index]) {

            // clear tail
            System.arraycopy(minimals, index + 1, weights, index + 1, weights.length - (index + 1));

            int sum = sumIntArray(weights);

            // todo: check intervals!
            if (sum == 100) {
                acc.add(
                        portfolio(correlations, averageYields, stdevYields,
                                Arrays.stream(weights).mapToDouble(d -> d / 100.0).toArray(), instruments)
                );
            }

            if (index < weights.length - 1 && sum < 100) {
                iteratePortfolioHelper(correlations, averageYields, stdevYields, minimals, maximals, step, weights, instruments, index + 1, acc);
            }

            weights[index] += step;
        }
    }

    public static double distance(DoublePoint p1, DoublePoint p2) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static String formatPercent2(double f) {
        return String.format("%.2f%%", f * 100);
    }

    public static String formatPercent1(double f) {
        return String.format("%.1f%%", f * 100);
    }
}
