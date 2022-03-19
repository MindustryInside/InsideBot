package inside.util;

import java.security.SecureRandom;

public final class Mathf {

    public static final SecureRandom random = new SecureRandom();

    private Mathf() {
    }

    public static int digits(int n) {
        return n < 100000 ?
                n < 100 ?
                n < 10 ? 1 : 2 :
                n < 1000 ? 3 :
                n < 10000 ? 4 : 5 :
                n < 10000000 ?
                n < 1000000 ? 6 : 7 :
                n < 100000000 ? 8 :
                n < 1000000000 ? 9 : 10;
    }

    public static int digits(long n) {
        return n == 0 ? 1 : (int) (Math.log10(n) + 1);
    }

    public static int sign(float f) {
        return f < 0 ? -1 : 1;
    }

    public static int sign(boolean b) {
        return b ? 1 : -1;
    }

    public static int num(boolean b) {
        return b ? 1 : 0;
    }

    public static int nextPowerOfTwo(int value) {
        if (value == 0) {
            return 1;
        }
        value--;
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;
        return value + 1;
    }

    public static int round(int value, int step) {
        return value / step * step;
    }

    public static float round(float value, float step) {
        return (int) (value / step) * step;
    }

    public static int round(float value, int step) {
        return (int) (value / step) * step;
    }

    public static boolean isPowerOfTwo(long value) {
        return value != 0 && (value & value - 1) == 0;
    }

    public static short max(short a, short b) {
        return a >= b ? a : b;
    }

    public static short min(short a, short b) {
        return a <= b ? a : b;
    }

    public static short clamp(short value, short min, short max) {
        return max(min, min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value) {
        return clamp(value, 0f, 1f);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
