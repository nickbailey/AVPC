/**
   Filters read data from an input array and place a modified version
   of the data in the output array
**/

abstract class Filter extends Generator {

	protected float[] source;

	public Filter(float[] s) { source = s; }

	public void generate(float[] target,
						 ProgressWatcher pw,
						 Thread pipe) {
		generate(target, pw, pipe, 0, target.length);
	}

	void generate(float[] target,
				  ProgressWatcher pw,
				  Thread pipe,
				  int start, int end) {
		targetLength = target.length;

		// Whatever you don, _don't_ go past the end of the data
		if (end > targetLength) end = targetLength;
		// Do it blocks to exploit parallelism and indicate progress
		int blockLength = (int)(0.05 * targetLength);
		if (blockLength == 0) blockLength = 1; // don't hang on small blocks!
		while (complete < end) {
			int thisBlockEnd = complete+blockLength;
			if (thisBlockEnd > end) thisBlockEnd = end;
			doFilter(target, source, start, thisBlockEnd);
			complete = thisBlockEnd;
			if (pw != null) pw.progress((double)thisBlockEnd /
										(double)targetLength);
			start = thisBlockEnd;
		}
		finished = (end == targetLength);

		if (pipe != null) pipe.interrupt();
	}

	abstract void doFilter(float[] out, float[] in, int start, int end);
}
