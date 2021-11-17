class ReReSampler extends Filter {

	private float rate;
	private FIR fir;
	
	protected int firLength = 256;
	protected float kernel[];
	protected FIR theFIR;
	
	public ReReSampler(float[] source, int r) {
		super(source);
		//System.out.println("New FIR at r = " + (r/(AVPC.sampleRate/2.0)));
		kernel = new float[firLength];
		// The supplied paramenter is a sample rate;
		// the filter cutoff must be the nyqyist rate, so...
		float nyq = r/2f;
		// Generate the kernel for this firLength
		float sum = 0f;
		for (int i = 0; i < firLength; i++) {
			kernel[i] = (
			  sinc((i - firLength/2)*(nyq/(AVPC.sampleRate/2f)))
			);
			// Apply a Hann window
			float x = (float)(
			  1.0 + Math.cos( Math.PI*(i-(firLength/2))/(firLength/2f+2) )
			);
			sum += kernel[i];
		}
		// Normalise the kernel so convolving with it doesn’t alter amplitudes
		for (int i = 0; i < firLength; i++)
			kernel[i] /= sum;
		
		theFIR = new FIR(kernel);
	}

	public void doFilter(float[] out, float[] in, int start, int end) {
		for (int x = start; x < end; x++)
			out[x] = theFIR.getOutputSample(in[x]);
	}
	
	// New-fangled sinc
	protected final float sinc(float x) {
		if (Math.abs(x) < 1e-6)
			return 1f;
		else
			return (float)Math.sin(Math.PI * x)/(float)(Math.PI * x);
	}
}
