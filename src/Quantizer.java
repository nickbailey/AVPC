import java.util.Random;

/** Filter to implement quantisation and dither demonstration **/


class Quantizer extends Filter {

	private final int qBits;
	private final double mul;
	private final int muli;
	private final boolean dither;
	private final boolean compand;
	private Random rand = null;

	public Quantizer(float[] source, int qb, boolean d, boolean l) {
		super(source);
		qBits = qb;
		muli = 1 << qBits;
		mul = muli;
		dither = d;
		compand = l;
		if (d) { rand = new Random(); }
	}

	public void doFilter(float[] out, float[] in, int start, int end) {
		/**
		   If the compand flag is set, compress data prior to processing
		   and expand it again afterwards. The expanding law is based on
		   that used in the US telephone system [Stremler, "Introduction
		   to Communication Systems, ISBN 0-201-18498-2, page 545
		**/

		for (int x = start; x < end; x++) {
			double dth = 0f;
			float smp = in[x];
			float result;
			float sign = 0f;

			// Perform compression
			if (compand) {
				sign = in[x] >= 0 ? 1f : -1f ;
				smp *= sign;
				smp = (float)Math.log(1f+255f*smp)/(float)Math.log(256);
			}

			// Add dither if required
			if (dither && qBits < 16)
				dth = (2.0*rand.nextDouble()-1.0)/mul;
			// s is sample value in range 0..2^(qBits)
 			int s = (int)Math.floor((0.5 + (smp+dth)/2.0)*mul);
			// Clip values to maximum representable value
			if (s >= muli) { s = muli-1; }
			if (s < 0) { s = 0; }
			result = (float)(s*2.0/(mul) - 1.0 + 1.0/mul);

			// Perform expansion
			if (compand)
				result = sign * ((float)Math.exp(result*Math.log(256)) - 1f)/255f;

			out[x] = result;
			//out[x]=in[x];  // Bodge to test infrastructure
		}
	}
}
