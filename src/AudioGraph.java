import java.awt.Dimension;
import javax.swing.JComponent;

/** 
   This is the superclass of all components whose job it is to display
   audio data. The reqirements of the application are that there are
   two sets of such data, and the "original" can be compared with the
   "processed" by auditioning either.  All such components represent
   time as the horizontal axis.
 **/

abstract class AudioGraph extends JComponent {

	protected float[] data = null;        // Data which is being plotted
	protected float[] result = null;      // Result of DSP operations
	protected boolean dispValid = false;  // Forces rerender
	protected Dimension dispDims = new Dimension(0, 0);
                                          // Remember the old display dims
	protected boolean newData;            // Flags source data having changed
	protected Generator gen;              // Generator producing data

	public AudioGraph() {
		setPreferredSize(new Dimension(600, 400));
	}	

	public ScrollHints setSourceData(float[] d) {
		data = d;
		newData = true;
		dispValid = false;
		return new ScrollHints();
	}
	public void setResultData(float[] d) {
		result = d;
		dispValid = false;
	}
	public void setResultData(float[] d, Generator g) {
		// as above, but attach a new generator to this data stream
		gen = g;
		setResultData(d);
	}

	// Using the partially filled data arrays, render as much of the
	// display as possible.
	abstract public void render(ProgressWatcher pw, Generator g);

	// All audio graphs must be zoomable.
	abstract public void saveContext();
	abstract public ScrollHints restoreContext();
	abstract public ScrollHints zoomToSelection();
	abstract public void setXOrigin(int sample);
}
