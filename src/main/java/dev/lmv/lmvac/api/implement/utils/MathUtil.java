package dev.lmv.lmvac.api.implement.utils;

import org.bukkit.Location;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class MathUtil {
    public static double distanceXZ(Location loc1, Location loc2) {
        double dx = loc1.getX() - loc2.getX();
        double dz = loc1.getZ() - loc2.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double correlation(List<Double> actual, List<Double> predicted) {
        int n = actual.size();
        if (n != 0 && predicted.size() == n) {
            double meanActual = actual.stream().mapToDouble((d) -> {
                return d;
            }).average().orElse(0.0D);
            double meanPredicted = predicted.stream().mapToDouble((d) -> {
                return d;
            }).average().orElse(0.0D);
            double numerator = 0.0D;
            double denominatorActual = 0.0D;
            double denominatorPredicted = 0.0D;

            for(int i = 0; i < n; ++i) {
                double aDiff = (Double)actual.get(i) - meanActual;
                double pDiff = (Double)predicted.get(i) - meanPredicted;
                numerator += aDiff * pDiff;
                denominatorActual += aDiff * aDiff;
                denominatorPredicted += pDiff * pDiff;
            }

            double denominator = Math.sqrt(denominatorActual * denominatorPredicted);
            return denominator == 0.0D ? 0.0D : numerator / denominator;
        } else {
            return 0.0D;
        }
    }

    public static double jerk(List<Double> values) {
        if (values.size() < 4) {
            return 0.0D;
        } else {
            double jerk = 0.0D;

            for(int i = 3; i < values.size(); ++i) {
                double j = (Double)values.get(i) - 3.0D * (Double)values.get(i - 1) + 3.0D * (Double)values.get(i - 2) - (Double)values.get(i - 3);
                jerk += Math.abs(j);
            }

            return jerk;
        }
    }

    public static double getMean(Collection<? extends Number> data) {
        if (data != null && !data.isEmpty()) {
            double sum = 0.0D;
            int count = 0;

            for(Iterator var4 = data.iterator(); var4.hasNext(); ++count) {
                Number number = (Number)var4.next();
                sum += number.doubleValue();
            }

            return count == 0 ? 0.0D : sum / (double)count;
        } else {
            return 0.0D;
        }
    }

    public static double autocorrelation(List<Double> data, int lag) {
        int n = data.size();
        if (n <= lag + 1) {
            return 1.0D;
        } else {
            double mean = 0.0D;

            double v;
            for(Iterator<Double> var5 = data.iterator(); var5.hasNext(); mean += v) {
                v = (Double)var5.next();
            }

            mean /= (double)n;
            double numerator = 0.0D;
            double denominator = 0.0D;

            for(int i = 0; i < n; ++i) {
                double diff = (Double)data.get(i) - mean;
                denominator += diff * diff;
                int j = i + lag;
                if (j < n) {
                    double diffLag = (Double)data.get(j) - mean;
                    numerator += diff * diffLag;
                }
            }

            if (denominator == 0.0D) {
                return 1.0D;
            } else {
                return numerator / denominator;
            }
        }
    }

    public static double getKurtosis(List<Double> values) {
        int n = values.size();
        double mean = 0.0D;

        double v;
        for(Iterator<Double> var4 = values.iterator(); var4.hasNext(); mean += v) {
            v = (Double)var4.next();
        }

        mean /= (double)n;
        double sum2 = 0.0D;

        for(Iterator<Double> var6 = values.iterator(); var6.hasNext(); sum2 += Math.pow(v - mean, 2.0D)) {
            v = (Double)var6.next();
        }

        double variance = sum2 / (double)(n - 1);
        double stdDev = Math.sqrt(variance);
        if (stdDev == 0.0D) {
            return 0.0D;
        } else {
            double sum4 = 0.0D;

            for(Iterator<Double> var12 = values.iterator(); var12.hasNext(); sum4 += Math.pow(v - mean, 4.0D)) {
                v = (Double)var12.next();
            }

            double m4 = sum4 / (double)n;
            return m4 / Math.pow(stdDev, 4.0D) - 3.0D;
        }
    }

    public static double r2Linearity(List<Double> deltas) {
        int n = deltas.size();
        if (n < 2) {
            return 1.0D;
        } else {
            double sumX = 0.0D;
            double sumY = 0.0D;

            for(int i = 0; i < n; ++i) {
                sumX += (double)i;
                sumY += (Double)deltas.get(i);
            }

            double meanX = sumX / (double)n;
            double meanY = sumY / (double)n;
            double ssXX = 0.0D;
            double ssYY = 0.0D;
            double ssXY = 0.0D;

            for(int i = 0; i < n; ++i) {
                double dx = (double)i - meanX;
                double dy = (Double)deltas.get(i) - meanY;
                ssXX += dx * dx;
                ssYY += dy * dy;
                ssXY += dx * dy;
            }

            if (ssXX != 0.0D && ssYY != 0.0D) {
                double slope = ssXY / ssXX;
                double intercept = meanY - slope * meanX;
                double ssr = 0.0D;

                for(int i = 0; i < n; ++i) {
                    double pred = slope * (double)i + intercept;
                    double resid = (Double)deltas.get(i) - pred;
                    ssr += resid * resid;
                }

                double r2 = 1.0D - ssr / ssYY;
                if (Double.isNaN(r2)) {
                    r2 = 1.0D;
                }

                return Math.max(0.0D, Math.min(1.0D, r2));
            } else {
                return 1.0D;
            }
        }
    }

    public static double averageDeltaChange(List<Double> deltas) {
        if (deltas.size() < 2) {
            return 0.0D;
        } else {
            double sum = 0.0D;

            for(int i = 1; i < deltas.size(); ++i) {
                sum += Math.abs((Double)deltas.get(i) - (Double)deltas.get(i - 1));
            }

            return sum / (double)(deltas.size() - 1);
        }
    }

    public static double cosineSimilarity(List<Double> deltas) {
        if (deltas.size() < 2) {
            return 1.0D;
        } else {
            double sumCos = 0.0D;
            int count = 0;

            for(int i = 1; i < deltas.size(); ++i) {
                double prev = (Double)deltas.get(i - 1);
                double curr = (Double)deltas.get(i);
                if (prev != 0.0D || curr != 0.0D) {
                    double cos = prev * curr / (Math.sqrt(prev * prev) * Math.sqrt(curr * curr));
                    sumCos += cos;
                    ++count;
                }
            }

            return count == 0 ? 1.0D : sumCos / (double)count;
        }
    }

    public static boolean isNearlySame(double d1, double d2, double number) {
        return Math.abs(d1 - d2) < number;
    }

    public static double getGCD(double a, double b) {
        if (a != 0.0D && b != 0.0D) {
            a = Math.abs(a);

            double temp;
            for(b = Math.abs(b); b > 1.0E-9D; a = temp) {
                temp = b;
                b = a % b;
            }

            return a;
        } else {
            return 0.0D;
        }
    }

    public static double mean(double[] arr) {
        if (arr != null && arr.length != 0) {
            double sum = 0.0D;
            double[] var3 = arr;
            int var4 = arr.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                double v = var3[var5];
                sum += v;
            }

            return sum / (double)arr.length;
        } else {
            return 0.0D;
        }
    }

    public static double symmetry(double[] arr) {
        if (arr != null && arr.length >= 4) {
            double mean = mean(arr);
            double sym = 0.0D;
            int pairs = 0;

            for(int i = 0; i < arr.length / 2; ++i) {
                double sum = arr[i] + arr[arr.length - 1 - i];
                sym += Math.abs(sum - 2.0D * mean);
                ++pairs;
            }

            return pairs > 0 ? sym / (double)pairs : 1.0D;
        } else {
            return 1.0D;
        }
    }

    public static double[] diff(List<Double> values) {
        if (values != null && values.size() >= 2) {
            double[] d = new double[values.size() - 1];

            for(int i = 1; i < values.size(); ++i) {
                d[i - 1] = (Double)values.get(i) - (Double)values.get(i - 1);
            }

            return d;
        } else {
            return new double[0];
        }
    }

    public static boolean hasJitterBreaks(List<Double> data, double threshold) {
        if (data.size() < 3) {
            return true;
        } else {
            for(int i = 2; i < data.size(); ++i) {
                double delta1 = (Double)data.get(i) - (Double)data.get(i - 1);
                double delta2 = (Double)data.get(i - 1) - (Double)data.get(i - 2);
                double secondDerivative = Math.abs(delta1 - delta2);
                if (secondDerivative > threshold) {
                    return true;
                }
            }

            return false;
        }
    }
}