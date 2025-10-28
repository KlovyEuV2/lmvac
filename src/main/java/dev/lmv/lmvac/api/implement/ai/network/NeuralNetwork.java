package dev.lmv.lmvac.api.implement.ai.network;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

public class NeuralNetwork implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int inputSize;
    private final int hiddenSize;
    private final int outputSize;

    private double[][] weightsIH;
    private double[][] weightsHO;
    private double[] biasH;
    private double[] biasO;

    private double[] hiddenLayer;
    private double[] outputLayer;

    private final Random random = new Random();

    public NeuralNetwork(int inputSize, int hiddenSize, int outputSize) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;

        weightsIH = new double[hiddenSize][inputSize];
        weightsHO = new double[outputSize][hiddenSize];
        biasH = new double[hiddenSize];
        biasO = new double[outputSize];

        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                weightsIH[i][j] = random.nextDouble() * 2 - 1;
            }
            biasH[i] = random.nextDouble() * 2 - 1;
        }

        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                weightsHO[i][j] = random.nextDouble() * 2 - 1;
            }
            biasO[i] = random.nextDouble() * 2 - 1;
        }

        hiddenLayer = new double[hiddenSize];
        outputLayer = new double[outputSize];
    }

    public double forward(double[] inputs) {
        for (int i = 0; i < hiddenSize; i++) {
            double sum = biasH[i];
            for (int j = 0; j < inputSize; j++) {
                sum += weightsIH[i][j] * inputs[j];
            }
            hiddenLayer[i] = sigmoid(sum);
        }

        for (int i = 0; i < outputSize; i++) {
            double sum = biasO[i];
            for (int j = 0; j < hiddenSize; j++) {
                sum += weightsHO[i][j] * hiddenLayer[j];
            }
            outputLayer[i] = sigmoid(sum);
        }

        return outputLayer[0];
    }

    public void backward(double target, double learningRate) {
        double outputError = target - outputLayer[0];
        double outputDelta = outputError * sigmoidDerivative(outputLayer[0]);

        double[] hiddenErrors = new double[hiddenSize];
        double[] hiddenDeltas = new double[hiddenSize];

        for (int i = 0; i < hiddenSize; i++) {
            hiddenErrors[i] = outputDelta * weightsHO[0][i];
            hiddenDeltas[i] = hiddenErrors[i] * sigmoidDerivative(hiddenLayer[i]);
        }

        for (int i = 0; i < outputSize; i++) {
            biasO[i] += outputDelta * learningRate;
            for (int j = 0; j < hiddenSize; j++) {
                weightsHO[i][j] += hiddenLayer[j] * outputDelta * learningRate;
            }
        }

        for (int i = 0; i < hiddenSize; i++) {
            biasH[i] += hiddenDeltas[i] * learningRate;
            for (int j = 0; j < inputSize; j++) {
                weightsIH[i][j] += hiddenDeltas[i] * learningRate * 0.1;
            }
        }
    }

    private double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    private double sigmoidDerivative(double x) {
        return x * (1 - x);
    }
}