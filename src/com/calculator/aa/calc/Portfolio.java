package com.calculator.aa.calc;

import com.calculator.aa.Main;
import com.calculator.aa.ui.ShowTable;

import java.util.Arrays;

public class Portfolio implements Comparable<Portfolio> {
    private final DoublePoint parameters;
    private final DoublePoint rebalancedParameters;
    private final double [][] data;
    private final double[] weights;
    private final String[] instruments;
    private boolean rebalancedMode;
    private double coefficient;
    private final Calc.RebalanceMode rebalanceMode;
    private final int rebalancetTreshold;

    public Portfolio(Portfolio o) {
        parameters = new DoublePoint(o.parameters);
        rebalancedParameters = new DoublePoint(o.rebalancedParameters);

        int len = o.data.length;
        data = new double[len][];
        for (int i = 0; i < len; i++) {
            data[i] = Arrays.copyOf(o.data[i], o.data[i].length);
        }

        weights = Arrays.copyOf(o.weights, o.weights.length);
        instruments = Arrays.copyOf(o.instruments, o.instruments.length);
        rebalancedMode = o.rebalancedMode;
        coefficient = o.coefficient;
        rebalanceMode = o.rebalanceMode;
        rebalancetTreshold = o.rebalancetTreshold;
    }

    Portfolio(DoublePoint p, double[] w, String[] i, double[][] df, Calc.RebalanceMode mode, int threshold) {
        parameters = p;
        data = df;
        weights = w;
        instruments = i;
        rebalancedMode = false;
        rebalanceMode = mode;
        rebalancetTreshold = threshold;
        rebalancedParameters = calculateRebalances();
        coefficient = Double.NaN;
    }

    @Override
    public int compareTo(Portfolio o) {
        double myRisk = rebalancedMode ? rebalancedParameters.getX() : parameters.getX();
        double otherRisk = rebalancedMode ? o.rebalancedParameters.getX() : o.parameters.getX();

        if (myRisk < otherRisk) {
            return -1;
        } else if (myRisk > otherRisk) {
            return 1;
        } else {
            return 0;
        }
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof Portfolio)) {
            return false;
        }

        Portfolio oo = (Portfolio)o;

        if (!parameters.equals(oo.parameters)) {
            return false;
        }

        if (!Arrays.equals(instruments, oo.instruments)) {
            return false;
        }

        double[] ws = oo.weights;
        int length = weights.length;
        if (length != ws.length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (Math.abs(weights[i] - ws[i]) > Calc.epsilon) {
                return false;
            }
        }

        return true;
    }

    int compareToYield(Portfolio o) {
        double myYield = rebalancedMode ? rebalancedParameters.getY() : parameters.getY();
        double otherYield = rebalancedMode ? o.rebalancedParameters.getY() : o.parameters.getY();

        if (myYield < otherYield) {
            return -1;
        } else if (myYield > otherYield) {
            return 1;
        } else {
            return 0;
        }
    }

    public String[] labels() {
        int length = weights.length;
        String[] result = new String[length + 2];

        System.arraycopy(instruments, 0, result, 0, length);

        result[length] = Main.resourceBundle.getString("text.risk");
        result[length + 1] = Main.resourceBundle.getString("text.compound_yield");

        return result;
    }

    public String[] labels(double riskFreeRate) {
        int length = weights.length;
        String[] result = new String[length + 5];

        System.arraycopy(instruments, 0, result, 0, length);

        result[length] = Main.resourceBundle.getString("text.risk");
        result[length + 1] = Main.resourceBundle.getString("text.compound_yield");
        result[length + 2] = Main.resourceBundle.getString("text.risk_free_rate");
        result[length + 3] = Main.resourceBundle.getString("text.sharpe_ratio") + ShowTable.noPercentFormat;
        result[length + 4] = Main.resourceBundle.getString("text.sortino_ratio") + ShowTable.noPercentFormat;

        return result;
    }

    public String[] getInstruments() {
        return instruments;
    }

    public double[][] values() {
        int length = weights.length;
        double[][] result = new double[length + 2][1];

        for (int i = 0; i < length; i++) {
            result[i][0] = weights[i];
        }

        result[length][0] = risk();
        result[length + 1][0] = yield();

        return result;
    }

    public double[][] values(double riskFreeRate) {
        int length = weights.length;
        double[][] result = new double[length + 5][1];

        for (int i = 0; i < length; i++) {
            result[i][0] = weights[i];
        }

        result[length][0] = risk();
        result[length + 1][0] = yield();
        result[length + 2][0] = riskFreeRate;
        result[length + 3][0] = Calc.ratioSharpe(this, riskFreeRate, rebalanceMode, rebalancetTreshold);
        result[length + 4][0] = Calc.ratioSortino(this, riskFreeRate, rebalanceMode, rebalancetTreshold);

        return result;
    }

    public double[] weights() {
        return weights;
    }

    public double[][] data() {
        return data;
    }

    public double yield() {
        return rebalancedMode ? rebalancedParameters.getY() : parameters.getY();
    }

    public double risk() {
        return rebalancedMode ? rebalancedParameters.getX() : parameters.getX();
    }

    public DoublePoint performance() {
        return rebalancedMode ? rebalancedParameters : parameters;
    }

    private DoublePoint calculateRebalances() {
        double[] result = Calc.calculateRebalances(this, false, false, rebalanceMode, rebalancetTreshold);
        return new DoublePoint(
                Calc.stdevYields(result),
                Calc.averagePercentYields(result));
    }

    public void setRebalancedMode(boolean mode) {
        rebalancedMode = mode;
    }

    public boolean getRebalancedMode() {
        return rebalancedMode;
    }

    public void setCoefficient(double c) {
        coefficient = c;
    }

    public double getCoefficient() {
        return coefficient;
    }

    public boolean hasCoefficient() {
        return !Double.isNaN(coefficient);
    }
}
