package com.myzone.utils.math;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Arbitrary-precision fraction, utilizing BigIntegers for numerator and
 * denominator. Fraction is always kept in lowest terms. Fraction is
 * immutable, and guaranteed not to have a null numerator or denominator.
 * Denominator will always be positive (so sign is carried by numerator,
 * and a zero-denominator is impossible).
 */
public final class BigFraction extends Number implements Comparable<Number> {

    private static final long serialVersionUID = 2L; //because Number is Serializable

    public final static BigFraction ZERO = new BigFraction(BigInteger.ZERO, BigInteger.ONE, true);
    public final static BigFraction ONE = new BigFraction(BigInteger.ONE, BigInteger.ONE, true);
    public final static BigFraction TEN = new BigFraction(BigInteger.TEN, BigInteger.ONE, true);

    private final static BigInteger BIGINT_TWO = BigInteger.valueOf(2);
    private final static BigInteger BIGINT_FIVE = BigInteger.valueOf(5);
    private final static BigInteger BIGINT_MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    private final static BigInteger BIGINT_MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);

    private final BigInteger numerator;
    private final BigInteger denominator;

    /**
     * Private constructor, used when you can be certain that the fraction is already in
     * lowest terms. No check is done to reduce numerator/denominator. A check is still
     * done to maintain a positive denominator.
     *
     * @param reduced Indicates whether or not the fraction is already known to be
     *                  reduced to lowest terms.
     */
    private BigFraction(@NotNull BigInteger numerator, @NotNull BigInteger denominator, boolean reduced) {
        if (denominator.equals(BigInteger.ZERO))
            throw new ArithmeticException("Divide by zero: fraction denominator is zero.");

        //only numerator should be negative.
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }

        if (!reduced) {
            //create a reduced fraction
            BigInteger gcd = numerator.gcd(denominator);
            numerator = numerator.divide(gcd);
            denominator = denominator.divide(gcd);
        }

        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Constructs a BigFraction from given number. If the number is not one of the
     * primitive types, BigInteger, BigDecimal, or BigFraction, then Number.doubleValue()
     * will be used for construction.
     *
     * <p>Warning: when using floating point numbers, round-off error can result
     * in answers that are unexpected. For example,
     * System.out.println(BigFraction.valueOf(1.1))
     * will print:
     * 2476979795053773/2251799813685248
     * 
     * This is because 1.1 cannot be expressed exactly in binary form. The
     * computed fraction is exactly equal to the internal representation of
     * the double-precision floating-point number. (Which, for 1.1, is:
     * (-1)^0 * 2^0 * (1 + 0x199999999999aL / 0x10000000000000L).)
     * 
     * <p>NOTE: In many cases, BigFraction.valueOf(Double.toString(d)) may give a result
     * closer to what the user expects.
     *
     * @param number number
     * @return BigFraction representation of number
     */
    @NotNull
    public static BigFraction valueOf(@NotNull Number number) {
        if (number instanceof BigFraction)
            return (BigFraction) number;

        if (isInt(number))
            return new BigFraction(toBigInteger(number), BigInteger.ONE, true);

        if (number instanceof BigDecimal)
            return valueOfHelper((BigDecimal) number);

        return valueOfHelper(number.doubleValue());
    }

    /**
     * Constructs a BigFraction with given numerator and denominator. Fraction
     * will be reduced to lowest terms. If fraction is negative, negative sign will
     * be carried on numerator, regardless of how the values were passed in.
     *
     * <p>Warning: when using floating point numbers, round-off error can result
     * in answers that are unexpected. For example,
     * System.out.println(BigFraction.valueOf(1.1))
     * will print:
     * 2476979795053773/2251799813685248
     * 
     * This is because 1.1 cannot be expressed exactly in binary form. The
     * computed fraction is exactly equal to the internal representation of
     * the double-precision floating-point number. (Which, for 1.1, is:
     * (-1)^0 * 2^0 * (1 + 0x199999999999aL / 0x10000000000000L).)
     * 
     * <p>NOTE: In many cases, BigFraction.valueOf(Double.toString(d)) may give a result
     * closer to what the user expects.
     *
     * @param numerator numerator of result fraction
     * @param denominator denominator of result fraction
     * @return BigFraction representation of numerator / denominator
     * @throws ArithmeticException if denominator == 0.
     */
    @NotNull
    public static BigFraction valueOf(@NotNull Number numerator, @NotNull Number denominator) {
        if (denominator.equals(0))
            throw new ArithmeticException("Divide by zero: fraction denominator is zero.");

        if (isInt(numerator) && isInt(denominator))
            return new BigFraction(toBigInteger(numerator), toBigInteger(denominator), false);

        if (isFloat(numerator) && isFloat(denominator))
            return valueOfHelper(numerator.doubleValue(), denominator.doubleValue());

        if (numerator instanceof BigDecimal && denominator instanceof BigDecimal)
            return valueOfHelper((BigDecimal) numerator, (BigDecimal) denominator);

        return valueOf(numerator).divide(valueOf(denominator));
    }

    /**
     * Constructs a BigFraction from a String. Expected format is numerator/denominator,
     * but /denominator part is optional. Either numerator or denominator may be a floating-
     * point decimal number, which is in the same format as a parameter to the
     * <code>BigDecimal(String)</code> constructor.
     *
     * @param string string representation of result fraction
     * @return BigFraction representation of string
     * @throws NumberFormatException if the string cannot be properly parsed.
     * @throws ArithmeticException if denominator == 0.
     */
    @NotNull
    public static BigFraction valueOf(@NotNull String string) {
        int slashPosition = string.indexOf('/');

        if (slashPosition < 0) {
            return valueOfHelper(new BigDecimal(string));
        } else {
            BigDecimal num = new BigDecimal(string.substring(0, slashPosition));
            BigDecimal den = new BigDecimal(string.substring(slashPosition + 1, string.length()));

            return valueOfHelper(num, den);
        }
    }

    /**
     * Constructs a BigFraction from a floating-point number.
     */
    @NotNull
    private static BigFraction valueOfHelper(double d) {
        if (Double.isInfinite(d))
            throw new IllegalArgumentException("double val is infinite");
        if (Double.isNaN(d))
            throw new IllegalArgumentException("double val is NaN");

        //special case - math below won't work right for 0.0 or -0.0
        if (d == 0)
            return BigFraction.ZERO;

        //Per IEEE spec...
        final long bits = Double.doubleToLongBits(d);
        final int sign = (int) (bits >> 63) & 0x1;
        final int exponent = ((int) (bits >> 52) & 0x7ff) - 0x3ff;
        final long mantissa = bits & 0xfffffffffffffL;

        //Number is: (-1)^sign * 2^(exponent) * 1.mantissa
        //Neglecting sign bit, this gives:
        //           2^(exponent) * 1.mantissa
        //         = 2^(exponent) * (1 + mantissa/2^52)
        //         = 2^(exponent) * (2^52 + mantissa)/2^52
        // For exponent > 52:
        //         = 2^(exponent - 52) * (2^52 + mantissa)
        // For exponent = 52:
        //         = 2^52 + mantissa
        // For exponent < 52:
        //         = (2^52 + mantissa) / 2^(52 - exponent)

        BigInteger tmpNumerator = BigInteger.valueOf(0x10000000000000L + mantissa);
        BigInteger tmpDenominator = BigInteger.ONE;

        if (exponent > 52) {
            //numerator * 2^(exponent - 52) === numerator << (exponent - 52)
            tmpNumerator = tmpNumerator.shiftLeft(exponent - 52);
        } else if (exponent < 52) {
            //The gcd of (2^52 + mantissa) / 2^(52 - exponent) must be of the form 2^y,
            //since the only prime factors of the denominator are 2. In base-2, it is
            //easy to determine how many factors of 2 a number has--it is the number of
            //trailing "0" bits at the end of the number. (This is the same as the number
            //of trailing 0's of a base-10 number indicating the number of factors of 10
            //the number has).
            int y = Math.min(tmpNumerator.getLowestSetBit(), 52 - exponent);

            //Now 2^y = gcd( 2^52 + mantissa, 2^(52 - exponent) ), giving:
            // (2^52 + mantissa) / 2^(52 - exponent)
            //      = ((2^52 + mantissa) / 2^y) / (2^(52 - exponent) / 2^y)
            //      = ((2^52 + mantissa) / 2^y) / (2^(52 - exponent - y))
            //      = ((2^52 + mantissa) >> y) / (1 << (52 - exponent - y))
            tmpNumerator = tmpNumerator.shiftRight(y);
            tmpDenominator = tmpDenominator.shiftLeft(52 - exponent - y);
        }
        //else: exponent == 52: do nothing

        //Set sign bit if needed
        if (sign != 0)
            tmpNumerator = tmpNumerator.negate();

        //Guaranteed there is no gcd, so fraction is in lowest terms
        return new BigFraction(tmpNumerator, tmpDenominator, true);
    }

    /**
     * Constructs a BigFraction from two floating-point numbers.
     * 
     * Warning: round-off error in IEEE floating point numbers can result
     * in answers that are unexpected. See BigFraction(double) for more
     * information.
     * 
     * NOTE: In many cases, BigFraction(Double.toString(numerator) + "/" + Double.toString(denominator))
     * may give a result closer to what the user expects.
     *
     * @throws ArithmeticException if denominator == 0.
     */
    @NotNull
    private static BigFraction valueOfHelper(double numerator, double denominator) {
        if (denominator == 0)
            throw new ArithmeticException("Divide by zero: fraction denominator is zero.");

        if (denominator < 0) {
            numerator = -numerator;
            denominator = -denominator;
        }

        BigFraction numFract = valueOfHelper(numerator);
        BigFraction denFract = valueOfHelper(denominator);

        //We can avoid the check for gcd here because we know that a fraction created from
        //a double will be of the form n/2^x, where x >= 0. So we have:
        //     (n1/2^x1)/(n2/2^x2)
        //   = (n1/n2) * (2^x2 / 2^x1).
        //
        //Now, we only have to check for gcd(n1,n2), and we know gcd(2^x2, 2^x1) = 2^(abs(x2 - x1)).
        //This gives us the following:
        // For x1 < x2 : (n1 * 2^(x2 - x1)) / n2 =  (n1 << (x2 - x1)) / n2
        // For x1 = x2 : n1 / n2
        // For x1 > x2 : n1 / (n2 * 2^(x1 - x2)) =  n1 / (n2 << (x1 - x2))
        //
        //Further, we know that if x1 > 0, n1 is not divisible by 2 (likewise for x2 > 0 and n2).
        //This guarantees that the GCD for any of the above three cases is equal to gcd(n1,n2).
        //Since it is easier to compute GCD of smaller numbers, this can speed us up a bit.

        BigInteger gcd = numFract.numerator.gcd(denFract.numerator);
        BigInteger tmpNumerator = numFract.numerator.divide(gcd);
        BigInteger tmpDenominator = denFract.numerator.divide(gcd);

        int x1 = numFract.denominator.getLowestSetBit();
        int x2 = denFract.denominator.getLowestSetBit();

        //Note: a * 2^b === a << b
        if (x1 < x2)
            tmpNumerator = tmpNumerator.shiftLeft(x2 - x1);
        else if (x1 > x2)
            tmpDenominator = tmpDenominator.shiftLeft(x1 - x2);
        //else: x1 == x2: do nothing

        return new BigFraction(tmpNumerator, tmpDenominator, false);
    }

    /**
     * Constructs a new BigFraction from the given BigDecimal object.
     */
    @NotNull
    private static BigFraction valueOfHelper(@NotNull BigDecimal d) {
        //BigDecimal format: unscaled / 10^scale.
        BigInteger tmpNumerator = d.unscaledValue();
        BigInteger tmpDenominator = BigInteger.ONE;

        //Special case for d == 0 (math below won't work right)
        //Note: Cannot use d.equals(BigDecimal.ZERO), because BigDecimal.equals()
        //       does not consider numbers equal if they have different scales. So,
        //       0.00 is not equal to BigDecimal.ZERO.
        if (tmpNumerator.equals(BigInteger.ZERO))
            return BigFraction.ZERO;

        if (d.scale() < 0) {
            tmpNumerator = tmpNumerator.multiply(BigInteger.TEN.pow(-d.scale()));
        } else if (d.scale() > 0) {
            //Now we have the form: unscaled / 10^scale = unscaled / (2^scale * 5^scale)
            //We know then that gcd(unscaled, 2^scale * 5^scale) = 2^commonTwos * 5^commonFives

            //Easy to determine commonTwos
            int commonTwos = Math.min(d.scale(), tmpNumerator.getLowestSetBit());
            tmpNumerator = tmpNumerator.shiftRight(commonTwos);
            tmpDenominator = tmpDenominator.shiftLeft(d.scale() - commonTwos);

            //Determining commonFives is a little trickier..
            int commonFives = 0;

            BigInteger[] divMod = null;
            //while(commonFives < d.scale() && tmpNumerator % 5 == 0) { tmpNumerator /= 5; commonFives++; }
            while (commonFives < d.scale() && BigInteger.ZERO.equals((divMod = tmpNumerator.divideAndRemainder(BIGINT_FIVE))[1])) {
                tmpNumerator = divMod[0];
                commonFives++;
            }

            if (commonFives < d.scale())
                tmpDenominator = tmpDenominator.multiply(BIGINT_FIVE.pow(d.scale() - commonFives));
        }
        //else: d.scale() == 0: do nothing

        //Guaranteed there is no gcd, so fraction is in lowest terms
        return new BigFraction(tmpNumerator, tmpDenominator, true);
    }

    /**
     * Constructs a new BigFraction from two BigDecimals.
     *
     * @throws ArithmeticException if denominator == 0.
     */
    @NotNull
    private static BigFraction valueOfHelper(@NotNull BigDecimal numerator, @NotNull BigDecimal denominator) {
        //Note: Cannot use .equals(BigDecimal.ZERO), because "0.00" != "0.0".
        if (denominator.unscaledValue().equals(BigInteger.ZERO))
            throw new ArithmeticException("Divide by zero: fraction denominator is zero.");

        //Format of BigDecimal: unscaled / 10^scale
        BigInteger tmpNumerator = numerator.unscaledValue();
        BigInteger tmpDenominator = denominator.unscaledValue();

        // (u1/10^s1) / (u2/10^s2) = u1 / (u2 * 10^(s1-s2)) = (u1 * 10^(s2-s1)) / u2
        if (numerator.scale() > denominator.scale()) {
            tmpDenominator = tmpDenominator.multiply(BigInteger.TEN.pow(numerator.scale() - denominator.scale()));
        } else if (numerator.scale() < denominator.scale()) {
            tmpNumerator = tmpNumerator.multiply(BigInteger.TEN.pow(denominator.scale() - numerator.scale()));
        }
        //else: scales are equal, do nothing.

        BigInteger gcd = tmpNumerator.gcd(tmpDenominator);
        tmpNumerator = tmpNumerator.divide(gcd);
        tmpDenominator = tmpDenominator.divide(gcd);

        if (tmpDenominator.signum() < 0) {
            tmpNumerator = tmpNumerator.negate();
            tmpDenominator = tmpDenominator.negate();
        }

        return new BigFraction(tmpNumerator, tmpDenominator, true);
    }

    /**
     * Converts a Number to a BigInteger. Assumes that a check on the type of n
     * has already been performed.
     *
     * @param number Number representation of result
     * @return BigInteger representation of number
     */
    @NotNull
    private static BigInteger toBigInteger(@NotNull Number number) {
        if (number instanceof BigInteger)
            return (BigInteger) number;

        return BigInteger.valueOf(number.longValue());
    }

    /**
     * @return true if the given type represents an integer (Long, Integer, Short, Byte, or BigInteger).
     * Used to determine if a Number is appropriate to be passed into toBigInteger() method.
     */
    private static boolean isInt(@NotNull Number n) {
        return n instanceof Long
                || n instanceof Integer
                || n instanceof Short
                || n instanceof Byte
                || n instanceof BigInteger
                || n instanceof AtomicInteger
                || n instanceof AtomicLong;
    }

    /**
     * @return true if n is a floating-point primitive type (Double or Float).
     */
    private static boolean isFloat(@NotNull Number n) {
        return n instanceof Double || n instanceof Float;
    }

    @NotNull
    /**
     * @return numerator of fraction
     */
    public final BigInteger getNumerator() {
        return numerator;
    }

    @NotNull
    /**
     * @return numerator of fraction
     */
    public final BigInteger getDenominator() {
        return denominator;
    }

    /**
     * @param n number
     * @return new BigFraction of this + n.
     */
    @NotNull
    public BigFraction add(@NotNull Number n) {
        if (isInt(n)) {
            //n1/d1 + n2 = (n1 + d1*n2)/d1
            return new BigFraction(
                    numerator.add(denominator.multiply(toBigInteger(n))),
                    denominator,
                    true
            );
        } else {
            BigFraction f = valueOf(n);

            //n1/d1 + n2/d2 = (n1*d2 + d1*n2)/(d1*d2)
            return new BigFraction(
                    numerator.multiply(f.denominator).add(denominator.multiply(f.numerator)),
                    denominator.multiply(f.denominator),
                    false
            );
        }
    }

    /**
     * @param number number
     * @return new BigFraction of this - n.
     */
    @NotNull
    public BigFraction subtract(@NotNull Number number) {
        if (isInt(number)) {
            //n1/d1 - n2 = (n1 - d1*n2)/d1
            return new BigFraction(
                    numerator.subtract(denominator.multiply(toBigInteger(number))),
                    denominator,
                    true
            );
        } else {
            BigFraction f = valueOf(number);

            //n1/d1 - n2/d2 = (n1*d2 - d1*n2)/(d1*d2)
            return new BigFraction(
                    numerator.multiply(f.denominator).subtract(denominator.multiply(f.numerator)),
                    denominator.multiply(f.denominator),
                    false
            );
        }
    }

    /**
     * @param number number
     * @return new BigFraction of this * n.
     */
    @NotNull
    public BigFraction multiply(@NotNull Number number) {
        BigFraction f = valueOf(number);

        //(n1/d1)*(n2/d2) = (n1*n2)/(d1*d2)
        return new BigFraction(numerator.multiply(f.numerator), denominator.multiply(f.denominator), false);
    }

    /**
     * @param number number
     * @return new BigFraction of this / n.
     * @throws ArithmeticException if n == 0.
     */
    @NotNull
    public BigFraction divide(@NotNull Number number) {
        BigFraction f = valueOf(number);

        if (f.numerator.equals(BigInteger.ZERO))
            throw new ArithmeticException("Divide by zero");

        //(n1/d1)/(n2/d2) = (n1*d2)/(d1*n2)
        return new BigFraction(numerator.multiply(f.denominator), denominator.multiply(f.numerator), false);
    }

    /**
     * <p>Note: 0^0 will return 1/1. This is consistent with Math.pow(),
     * BigInteger.pow(), and BigDecimal.pow().
     *
     * @return new BigFraction of this^exponent.
     * @throws ArithmeticException if <code>this == 0 &amp;&amp; exponent &lt; 0</code>.
     */
    @NotNull
    public BigFraction pow(int exponent) {
        if (exponent < 0 && numerator.equals(BigInteger.ZERO))
            throw new ArithmeticException("Divide by zero: raising zero to negative exponent.");

        if (exponent == 0)
            return BigFraction.ONE;

        if (exponent == 1)
            return this;

        if (exponent > 0)
            return new BigFraction(numerator.pow(exponent), denominator.pow(exponent), true);

        return new BigFraction(denominator.pow(-exponent), numerator.pow(-exponent), true);
    }

    /**
     * @return new BigFraction of 1 / this
     * @throws ArithmeticException if this == 0.
     */
    @NotNull
    public BigFraction reciprocal() {
        if (this.equals(ONE))
            return this;

        if (numerator.equals(BigInteger.ZERO))
            throw new ArithmeticException("Divide by zero: reciprocal of zero.");
        
        return new BigFraction(denominator, numerator, true);
    }

    /**
     * @return the complement of this fraction, which is equal to 1 - this.
     * Useful for probabilities/statistics.
     */
    @NotNull
    public BigFraction complement() {
        //1 - n/d == d/d - n/d == (d-n)/d
        return new BigFraction(denominator.subtract(numerator), denominator, true);
    }

    /**
     * @return new BigFraction of -this.
     */
    @NotNull
    public BigFraction negate() {
        if (this.equals(ZERO)) 
            return this;
        
        return new BigFraction(numerator.negate(), denominator, true);
    }

    /**
     * @return the absolute value of this.
     */
    @NotNull
    public BigFraction abs() {
        return signum() < 0 ? negate() : this;
    }

    /**
     * @return -1, 0, or 1, representing the sign of this fraction.
     */
    public int signum() {
        return numerator.signum();
    }

    /**
     * @return this rounded to the nearest whole number, using
     * RoundingMode.HALF_UP as the default rounding mode.
     */
    public BigInteger round() {
        return round(RoundingMode.HALF_UP);
    }

    /**
     * @param roundingMode {@link RoundingMode}
     * @return this fraction rounded to the nearest whole number, using
     * the given rounding mode.
     *
     * @throws ArithmeticException if RoundingMode.UNNECESSARY is used but
     *                             this fraction does not exactly represent an integer.
     */
    @NotNull
    public BigInteger round(@NotNull RoundingMode roundingMode) {
        //Since fraction is always in lowest terms, this is an exact integer
        //iff the denominator is 1.
        if (denominator.equals(BigInteger.ONE))
            return numerator;

        //If the denominator was not 1, rounding will be required.
        if (roundingMode == RoundingMode.UNNECESSARY)
            throw new ArithmeticException("Rounding necessary");

        final Set<RoundingMode> ROUND_HALF_MODES = EnumSet.of(RoundingMode.HALF_UP, RoundingMode.HALF_DOWN, RoundingMode.HALF_EVEN);

        BigInteger intVal = null;
        BigInteger remainder = null;

        //Note: The remainder is only needed if we are using HALF_X rounding mode, and the
        //       remainder is not one-half. Since computing the remainder can be a bit
        //       expensive, only compute it if necessary.
        if (ROUND_HALF_MODES.contains(roundingMode) && !denominator.equals(BIGINT_TWO)) {
            BigInteger[] divMod = numerator.divideAndRemainder(denominator);
            intVal = divMod[0];
            remainder = divMod[1];
        } else {
            intVal = numerator.divide(denominator);
        }

        //For HALF_X rounding modes, convert to either UP or DOWN.
        if (ROUND_HALF_MODES.contains(roundingMode)) {
            //Since fraction is always in lowest terms, the remainder is exactly
            //one-half iff the denominator is 2.
            if (denominator.equals(BIGINT_TWO)) {
                if (roundingMode == RoundingMode.HALF_UP || (roundingMode == RoundingMode.HALF_EVEN && intVal.testBit(0))) {
                    roundingMode = RoundingMode.UP;
                } else {
                    roundingMode = RoundingMode.DOWN;
                }
            } else if (remainder.abs().compareTo(denominator.shiftRight(1)) <= 0) {
                //note: x.shiftRight(1) === x.divide(2)
                roundingMode = RoundingMode.DOWN;
            } else {
                roundingMode = RoundingMode.UP;
            }
        }

        //For ceiling and floor, convert to up or down (based on sign).
        if (roundingMode == RoundingMode.CEILING || roundingMode == RoundingMode.FLOOR) {
            //Use numerator.signum() instead of intVal.signum() to get correct answers
            //for values between -1 and 0.
            if (numerator.signum() > 0) {
                if (roundingMode == RoundingMode.CEILING) {
                    roundingMode = RoundingMode.UP;
                } else {
                    roundingMode = RoundingMode.DOWN;
                }
            } else {
                if (roundingMode == RoundingMode.CEILING) {
                    roundingMode = RoundingMode.DOWN;
                } else {
                    roundingMode = RoundingMode.UP;
                }
            }
        }

        //Sanity check... at this point all possible values should be turned to up or down.
        if (roundingMode != RoundingMode.UP && roundingMode != RoundingMode.DOWN)
            throw new IllegalArgumentException("Unsupported rounding mode: " + roundingMode);

        if (roundingMode == RoundingMode.UP) {
            if (numerator.signum() > 0) {
                intVal = intVal.add(BigInteger.ONE);
            } else {
                intVal = intVal.subtract(BigInteger.ONE);
            }
        }

        return intVal;
    }

    /**
     * @return a string representation of this, in the form
     * numerator/denominator.
     */
    @NotNull
    @Override
    public String toString() {
        return numerator.toString() + "/" + denominator.toString();
    }

    /**
     * @return string representation of this object as a mixed fraction.
     * For example, 4/3 would be "1 1/3". For negative fractions, the
     * sign is carried only by the whole number and assumed to be distributed
     * across the whole value. For example, -4/3 would be "-1 1/3". For
     * fractions that are equal to whole numbers, only the whole number will
     * be displayed. For fractions which have absolute value less than 1,
     * this will be equivalent to toString().
     */
    @NotNull
    public String toMixedString() {
        if (denominator.equals(BigInteger.ONE))
            return numerator.toString();

        if (numerator.abs().compareTo(denominator) < 0)
            return toString();

        BigInteger[] divmod = numerator.divideAndRemainder(denominator);
        return divmod[0] + " " + divmod[1].abs() + "/" + denominator;
    }

    /**
     * @return if this object is equal to another object. In order to maintain symmetry,
     * this will *only* return true if the other object is a BigFraction. For looser
     * comparison to other Number objects, use the equalsNumber(Number) method.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof BigFraction))
            return false;

        BigFraction f = (BigFraction) o;
        return numerator.equals(f.numerator) && denominator.equals(f.denominator);
    }

    /**
     * @return if this object is equal to another Number object. Equivalent
     * to: <code>this.equals(BigFraction.valueOf(n))</code>
     */
    public boolean equalsNumber(Number n) {
        return equals(valueOf(n));
    }

    /**
     * @return a hash code for this object.
     */
    @Override
    public int hashCode() {
        //using the method generated by Eclipse, but streamlined a bit..
        return (31 + numerator.hashCode()) * 31 + denominator.hashCode();
    }

    /**
     * @param bigFraction another BigFraction
     * @return a negative, zero, or positive number, indicating if this object
     * is less than, equal to, or greater than f, respectively.
     */
    public int compareTo(@NotNull BigFraction bigFraction) {
        //easy case: this and f have different signs
        if (signum() != bigFraction.signum())
            return signum() - bigFraction.signum();

        //next easy case: this and f have the same denominator
        if (denominator.equals(bigFraction.denominator))
            return numerator.compareTo(bigFraction.numerator);

        //not an easy case, so first make the denominators equal then compare the numerators
        return numerator.multiply(bigFraction.denominator).compareTo(denominator.multiply(bigFraction.numerator));
    }

    /**
     * @param number another number
     * @return a negative, zero, or positive number, indicating if this object
     * is less than, equal to, or greater than n, respectively.
     */
    @Override
    public int compareTo(@NotNull Number number) {
        return compareTo(valueOf(number));
    }

    /**
     * @param bigFraction another BigFraction
     * @return the smaller of this and bigFraction.
     */
    @NotNull
    public BigFraction min(@NotNull BigFraction bigFraction) {
        return this.compareTo(bigFraction) <= 0 ? this : bigFraction;
    }

    /**
     * @param number another number
     * @return the smaller of this and number.
     */
    @NotNull
    public Number min(@NotNull Number number) {
        return this.compareTo(number) <= 0 ? this : number;
    }

    /**
     * @param bigFraction another number
     * @return the maximum of this and bigFraction.
     */
    @NotNull
    public BigFraction max(@NotNull BigFraction bigFraction) {
        return this.compareTo(bigFraction) >= 0 ? this : bigFraction;
    }

    /**
     * @param number another number
     * @return the maximum of this and number.
     */
    @NotNull
    public Number max(@NotNull Number number) {
        return this.compareTo(number) >= 0 ? this : number;
    }

    /**
     * @return a BigDecimal representation of this fraction. If possible, the
     * returned value will be exactly equal to the fraction. If not, the BigDecimal
     * will have a scale large enough to hold the same number of significant figures
     * as both numerator and denominator, or the equivalent of a double-precision
     * number, whichever is more.
     */
    @NotNull
    public BigDecimal toBigDecimal() {
        //Implementation note: A fraction can be represented exactly in base-10 iff its
        //denominator is of the form 2^a * 5^b, where a and b are nonnegative integers.
        //(In other words, if there are no prime factors of the denominator except for
        //2 and 5, or if the denominator is 1). So to determine if this denominator is
        //of this form, continually divide by 2 to get the number of 2's, and then
        //continually divide by 5 to get the number of 5's. Afterward, if the denominator
        //is 1 then there are no other prime factors.

        //Note: number of 2's is given by the number of trailing 0 bits in the number
        int twos = denominator.getLowestSetBit();
        BigInteger tmpDen = denominator.shiftRight(twos); // x / 2^n === x >> n

        int fives = 0;
        BigInteger[] divMod = null;

        //while(tmpDen % 5 == 0) { tmpDen /= 5; fives++; }
        while (BigInteger.ZERO.equals((divMod = tmpDen.divideAndRemainder(BIGINT_FIVE))[1])) {
            tmpDen = divMod[0];
            fives++;
        }

        if (BigInteger.ONE.equals(tmpDen)) {
            //This fraction will terminate in base 10, so it can be represented exactly as
            //a BigDecimal. We would now like to make the fraction of the form
            //unscaled / 10^scale. We know that 2^x * 5^x = 10^x, and our denominator is
            //in the form 2^twos * 5^fives. So use max(twos, fives) as the scale, and
            //multiply the numerator and deminator by the appropriate number of 2's or 5's
            //such that the denominator is of the form 2^scale * 5^scale. (Of course, we
            //only have to actually multiply the numerator, since all we need for the
            //BigDecimal constructor is the scale.)
            BigInteger unscaled = numerator;
            int scale = Math.max(twos, fives);

            if (twos < fives)
                unscaled = unscaled.shiftLeft(fives - twos); //x * 2^n === x << n
            else if (fives < twos)
                unscaled = unscaled.multiply(BIGINT_FIVE.pow(twos - fives));

            return new BigDecimal(unscaled, scale);
        }

        //else: this number will repeat infinitely in base-10. So try to figure out
        //a good number of significant digits. Start with the number of digits required
        //to represent the numerator and denominator in base-10, which is given by
        //bitLength / log[2](10). (bitLenth is the number of digits in base-2).
        final double LG10 = 3.321928094887362; //Precomputed ln(10)/ln(2), a.k.a. log[2](10)
        int precision = Math.max(numerator.bitLength(), denominator.bitLength());
        precision = (int) Math.ceil(precision / LG10);

        //If the precision is less than that of a double, use double-precision so
        //that the result will be at least as accurate as a cast to a double. For
        //example, with the fraction 1/3, precision will be 1, giving a result of
        //0.3. This is quite a bit different from what a user would expect.
        if (precision < MathContext.DECIMAL64.getPrecision() + 2) {
            precision = MathContext.DECIMAL64.getPrecision() + 2;
        }

        return toBigDecimal(precision);
    }

    /**
     * @param precision the number of significant figures to be used in the result.
     * @return a BigDecimal representation of this fraction, with a given precision.
     */
    @NotNull
    public BigDecimal toBigDecimal(int precision) {
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), new MathContext(precision, RoundingMode.HALF_EVEN));
    }

    /**
     * @return a long representation of this fraction. This value is
     * obtained by integer division of numerator by denominator. If
     * the value is greater than Long.MAX_VALUE, Long.MAX_VALUE will be
     * returned. Similarly, if the value is below Long.MIN_VALUE,
     * Long.MIN_VALUE will be returned.
     */
    @Override
    public long longValue() {
        BigInteger rounded = this.round(RoundingMode.DOWN);
        
        if (rounded.compareTo(BIGINT_MAX_LONG) > 0)
            return Long.MAX_VALUE;
        
        if (rounded.compareTo(BIGINT_MIN_LONG) < 0)
            return Long.MIN_VALUE;
        
        return rounded.longValue();
    }

    /**
     * @return an int representation of this fraction. This value is
     * obtained by integer division of numerator by denominator. If
     * the value is greater than Integer.MAX_VALUE, Integer.MAX_VALUE will be
     * returned. Similarly, if the value is below Integer.MIN_VALUE,
     * Integer.MIN_VALUE will be returned.
     */
    @Override
    public int intValue() {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, longValue()));
    }

    /**
     * @return a short representation of this fraction. This value is
     * obtained by integer division of numerator by denominator. If
     * the value is greater than Short.MAX_VALUE, Short.MAX_VALUE will be
     * returned. Similarly, if the value is below Short.MIN_VALUE,
     * Short.MIN_VALUE will be returned.
     */
    @Override
    public short shortValue() {
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, longValue()));
    }

    /**
     * @return a byte representation of this fraction. This value is
     * obtained by integer division of numerator by denominator. If
     * the value is greater than Byte.MAX_VALUE, Byte.MAX_VALUE will be
     * returned. Similarly, if the value is below Byte.MIN_VALUE,
     * Byte.MIN_VALUE will be returned.
     */
    @Override
    public byte byteValue() {
        return (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, longValue()));
    }

    /**
     * @return the value of this fraction. If this value is beyond the
     * range of a double, Double.INFINITY or Double.NEGATIVE_INFINITY will
     * be returned.
     */
    @Override
    public double doubleValue() {
        //note: must use precision+2 so that new BigFraction(d).doubleValue() == d,
        //      for all possible double values.
        return toBigDecimal(MathContext.DECIMAL64.getPrecision() + 2).doubleValue();
    }

    /**
     * @return the value of this fraction. If this value is beyond the
     * range of a float, Float.INFINITY or Float.NEGATIVE_INFINITY will
     * be returned.
     */
    @Override
    public float floatValue() {
        //note: must use precision+2 so that new BigFraction(f).floatValue() == f,
        //      for all possible float values.
        return toBigDecimal(MathContext.DECIMAL32.getPrecision() + 2).floatValue();
    }

}
