/**
   Class to permit arithmetic involving "Preferred Values".

   A Preferred Value is one of a series of values of the form
   a * 10^n, n integer, a <- V, a vector of perferred values.
**/

public class PreferredValue {

	protected double[] values;

	public PreferredValue(double[] pvs) {
		values = pvs;
	}

	// Return the nearest preferred value to v
	public double nearest(double v) {
		double sign = v < 0 ? -1.0 : 1.0;
		double absv = v * sign;
		double logv = Math.log(absv);
		int exp = (int)Math.floor(logv/Math.log(10.0));
		double mant = absv / Math.pow(10.0, (double)exp);
		double upper = 0, lower = 0, lowErr = 0, uppErr = 0;
		for (int i = -1; i < values.length; i++) {
			if (i == -1) {
				lower = values[values.length - 1];
				upper = values[0] * 10.0;
			} else if (i == values.length - 1) {
				lower = values[values.length - 1];
				upper = values[0];
			} else {
				lower = values[i];
				upper = values[i+1];
			}
			// Is the supplied value within this range?
			if (mant <= upper && mant >= lower) {
				// Find out whether it's nearer the lower or upper value
				// and return the most appropriate.
				lowErr = mant/lower;
				uppErr = upper/mant;
				break;
			}
		}
		return (lowErr > uppErr ? upper : lower)
			* Math.pow(10, (double)exp) * sign;
	}

	public static void main(String[] args) {
		// Test routine: prints nearest E6 value
		// Usage: java PreferredValue <double_value>
		double[] e6 = {1.0, 2.2, 3.3, 4.7, 6.8, 8.2};
		PreferredValue pv = new PreferredValue(e6);
		for (int i = 0; i < args.length; i++) {
			double d = new Double(args[i]).doubleValue();
			System.out.println("Value: " + d + " NPV: " + pv.nearest(d));
		}
	}
}
