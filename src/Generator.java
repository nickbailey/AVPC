abstract class Generator {

	/**
	   Return an array of count blocks of floating-point samples,
	   each blocksize long.  Call the progress listener after each
	   block indicating percentage completion.
	**/

	protected int complete = 0;           // Samples valid
	protected boolean finished = false;   // Whether to expect more
	protected int targetLength;           // Number of samples to be generated 

	public void generate(float[] target,
	                     ProgressWatcher pw) {
		generate(target, pw, null);
	}

	abstract public void generate(float[] target,
	                              ProgressWatcher pw,
	                              Thread pipe);

	public int getComplete() { return complete; }
	public boolean getFinished() { return finished; }
	public int getTargetLength() { return targetLength; }
}
