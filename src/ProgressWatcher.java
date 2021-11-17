import java.lang.String;
import javax.swing.JProgressBar;

/**
   The ProgressWatcher class sets up a progress bar and provides just
   one method, progress(), whereby a generator, file reader, processor
   or renderer can simply and easily report its progress so far.
**/

class ProgressWatcher extends JProgressBar{

	private String description;

	public ProgressWatcher(String s) {
		super();
		setStringPainted(true);
		description = s; }

	public String getDescription() { return new String(description); }

	public void progress(double complete) {
		setValue((int)(complete*100 + 0.5));
		//System.out.println(description+" "+(int)(complete*100 + 0.5)+"%");
	}
}
