import java.lang.Math;

/**
   Generate an approximation to a square wave to the
   given number of terms.
**/

class SquareWaveGenerator extends Generator {

	private int partials;
	private double frequency;
	private double amplitude;
	private int count, blockSize;

	public SquareWaveGenerator(int p, double f, double a,
	                           int c, int b) {
		partials = p;
		frequency = f;
		amplitude = a;
		count = c;
		blockSize = b;
		targetLength = count * blockSize;
	}

	public void generate(float[] target, ProgressWatcher pw, Thread pl) {
		int i = 0;
		for (int c = 0; c < count; c++) { // For each block
			for (int j = 0; j < blockSize; j++) { // For each sample in the blk
				target[i] = 0;
				for (int p = 0;
					 p < partials && 4*p*frequency < AVPC.sampleRate;
					 p++) {   // For each partial
					double f = (double)(1+2*p);
					target[i] += (float)Math.sin(f * (double)i *
												 frequency*2.0*Math.PI /
												 AVPC.sampleRate)/f;
				}
				target[i] *= amplitude;
				i++;
			}
			// Update superclass Generators data
			complete = (c+1) * blockSize;
			// Tell the progress listener that a block has been completed
			if (pw != null)
				pw.progress((double)complete/(100.0*count));
			// If there's a thread further down the pipeline, restart it.
			if (pl != null)
				pl.interrupt();
		}
		// Inform superclass data structure we're done
		finished = true;
		if (pl != null) pl.interrupt();
	}
}
