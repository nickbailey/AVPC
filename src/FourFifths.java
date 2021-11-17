class FourFifths extends Filter {

	public FourFifths(float[] source) { super(source); }

	public void doFilter(float[] out, float[] in, int start, int end) {
		for (int x = start; x < end; x++)
			out[x] = (float)(0.1 * in[x]);
	}
}
