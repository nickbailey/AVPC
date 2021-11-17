import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.JScrollBar;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import java.awt.Dimension;
import java.awt.Adjustable;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.lang.Thread;

 public class AudioView extends JComponent {

	 // Scrollbar permits replotting of viewed data in a zoomed context
	 private JScrollBar sb = new JScrollBar(Adjustable.HORIZONTAL, 0, 1, 0, 1);
	 
	 // The progress dialogue can display loading, processing and rendering
	 // work progress simultaneously.
	 private ProgressWatcher[] watch = {
		 new ProgressWatcher("Loading: "),
		 new ProgressWatcher("Processing: "),
		 new ProgressWatcher("Rendering: ")
	 };

	 private AudioGraph client;        // The client graph we're displaying
	 private Thread reader;            // Thread reading or generating audio
	 private Thread processor;         // Thread calculating results;
	 private Thread renderer;          // Client's thread working on display
	 private AudioSourceLoader asl;

	 // Command widgets
	 private final JButton genB = new JButton("Generate");
	 private final JButton zoomInB = new JButton("Zoom In");
	 private final JButton zoomOutB = new JButton("Zoom Out");
	 private final JButton fltB = new JButton("Filter");
	 private final JButton playB = new JButton("Play");
	 private final JComboBox<String> sourceChoice;
	 private final JCheckBox resultCB = new JCheckBox("Result", true);
	 private final JCheckBox allCB = new JCheckBox("All", true);

	 // Data storage and file/generator stats
	 private int targetLength;         // The amount of data we're getting
	 private boolean doResult = true;
	 private float[] data;             // Source audio storage
	 private float[] result;           // Filtered audio storage

	 // This needs to be called from the data reader routine, otherwise
	 // we've not been added to a window yet and placing the dialogue
	 // becomes arbitrary
	 private final ProgressDialogue pd = new ProgressDialogue(this, watch);
	 
	 // The audio processing is controlled by the Process object, so
	 // store a reference to it.
	 private Process process;

	 public AudioView(AudioGraph ag, Process p) {
		 client = ag;
		 process = p;
		 setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		 JPanel sourceOpts =
			 new JPanel(new FlowLayout(FlowLayout.LEADING));

		 asl = new AudioSourceLoader();

		 playB.setEnabled(false);
		 genB.setActionCommand("gen");
		 genB.addActionListener(asl);
		 fltB.setActionCommand("flt");
		 fltB.setEnabled(false);
		 fltB.addActionListener(asl); 

		 ZoomHandler zh = new ZoomHandler(client);
		 PlayHandler ap = new PlayHandler(this);
		 String[] choices = {"Sine", "Tone", "Speech", "Music"};
		 sourceChoice = new JComboBox<String>(choices);

		 zoomInB.setActionCommand("+");
		 zoomInB.addActionListener(zh);
		 zoomOutB.setActionCommand("-");
		 zoomOutB.addActionListener(zh);
		 playB.addActionListener(ap);
		 Dimension playBDim = playB.getPreferredSize();
		 playBDim.setSize(100.0, playBDim.getHeight());
		 playB.setPreferredSize(playBDim);
		 resultCB.setEnabled(false);
		 resultCB.addActionListener(new ActionListener() {
				 public void actionPerformed(ActionEvent ae) {
					 fltB.setEnabled(resultCB.isSelected());
				 }
			 }
									 );
				 
		 sourceOpts.add(genB);
		 sourceOpts.add(sourceChoice);
		 sourceOpts.add(zoomInB);
		 sourceOpts.add(zoomOutB);
		 sourceOpts.add(playB);
		 sourceOpts.add(allCB);
		 sourceOpts.add(resultCB);
		 sourceOpts.add(fltB);

		 // Only allow this panel to stretch horizontally
		 sourceOpts.setMaximumSize(new Dimension(Short.MAX_VALUE,
												 sourceOpts.getPreferredSize().height));
		 add(sourceOpts);
		 add(client);
		 add(sb);
		 sb.addAdjustmentListener(new MyAdjustmentListener(client));
	 }
	 

	 // Public access functions
	 public int getViewStart() { return sb.getValue(); }
	 public int getViewLength() { return sb.getVisibleAmount(); }
	 public float[] getViewData() { return resultCB.isSelected() ?
									    result : data; }
	 public boolean getPlayAll() { return allCB.isSelected(); }
	 public void setPlayButtonText(String t) { playB.setText(t); } 

	 // Issue a redraw request based on the new scroll position
	 private class MyAdjustmentListener implements AdjustmentListener {
		 private AudioGraph ag;

		 public MyAdjustmentListener(AudioGraph ag) {
			 super();
			 this.ag = ag;
		 }

		 public void adjustmentValueChanged(AdjustmentEvent e) {
			 ag.setXOrigin(e.getValue());
		 }
	 }

	 private void setScrollParameters(ScrollHints sh) {
		 System.out.println("Value: "+sh.value+"  Extent: "+sh.extent+"  Min: "+sh.min+"  Max: "+sh.max);
		 sb.setValues(sh.value, sh.extent, sh.min, sh.max);
		 //sb.setValues(sh.value, 1000, sh.min, sh.max);
	 }

	 private class ZoomHandler implements ActionListener {
		 private AudioGraph ag;

		 public ZoomHandler(AudioGraph ag) { this.ag = ag; }

		 public void actionPerformed(ActionEvent ae) {
			 String command = ae.getActionCommand();
			 if (command.equals("+")) {
				 ScrollHints sh = ag.zoomToSelection();
				 if (sh != null)
					 setScrollParameters(sh);
			 } else if (command.equals("-")) {
				 ScrollHints sh = ag.restoreContext();
				 if (sh != null)
					 setScrollParameters(sh);
			 }
		 }
	 }
	 
 	 private class AudioSourceLoader implements ActionListener {
		 // Load the approprate data source into the data array
		 // It is expected that worker will block from time to time
		 // in a file or URL load, causing the other threads
		 // to get a bite of the cherry.

		 private Filter flt = null;
		 private Generator src = null;

		 public AudioSourceLoader() { this(200000); }
		
		 public AudioSourceLoader(int tl) { resize(tl); }
		 
		 public void resize(int tl) {
			 System.out.println("Will load " + tl + " samples.");
			 targetLength = tl;
			 data = new float[targetLength];
		 }

		 public void actionPerformed(ActionEvent ae) {
			 boolean doLoader = false;
			 boolean doFilter = false;
			 String command = ae.getActionCommand();

			 // Disallow play requests while we're working
			 playB.setEnabled(false);

			 if (command.equals("gen")) {
				 doLoader = true;
				 src = sourceGenerator(sourceChoice.getSelectedIndex());
				 resultCB.setEnabled(true);
				 fltB.setEnabled(true);
			 } else { // command = flt
				 doLoader = false;
			 }

			 // Always construct a new filter,
			 // because the data might have changed.
			 flt = process.getProcessFilter(data);
			 doFilter = resultCB.isSelected();

			 if (doFilter) { // Perform processing operation?
				 result = new float[targetLength];
				 renderer = new RendererThread(flt);
				 processor = new ProcessorThread(src, flt, renderer);
				 client.setResultData(result);
				 if (doLoader)
					 reader = new LoaderThread(src, processor);
			 } else { // Send new data straight to renderer
				 result = null;
				 renderer = new RendererThread(src);
				 reader = new LoaderThread(src, renderer);
			 }

			 if (doLoader) { // Only reset scroll hints for new data.
				 ScrollHints sh = client.setSourceData(data);
				 reader.start();
				 setScrollParameters(sh);
			 }

			 // Force the progress dialogue to the top
			 // (annoying on a small screen)
			 //pd.show();
			 
			 if (resultCB.isSelected()) processor.start();

			 renderer.start();
		 }
	 }

	 private class LoaderThread extends Thread {
		 private Thread pipe;
		 private Generator src;

		 public LoaderThread(Generator g, Thread p) {
			 super();
			 pipe = p;        // Interrupt this thread on a per-block basis
			 src = g;
		 }

		 public int complete() { return src.getComplete(); }

		 public void run() {
			 src.generate(data, watch[0], pipe);
			 pipe.interrupt();
			 // If it isn't necessary to perform a processing op,
			 // re-enable the play button.
			 playB.setEnabled(true);
		 }
	 }

	 private class ProcessorThread extends Thread {

		 private Thread pipe;
		 private Generator src;
		 private Filter flt;

		 public ProcessorThread(Generator g, Filter f, Thread p) {
			 super();
			 pipe = p;
			 src = g;
			 flt = f;
		 }

		 public void run() {
			 if (doResult) {
				 int count = 0;
				 int target = 0;
				 do {
					 try {
						 sleep(1000);
					 } catch (InterruptedException e) {
						 // Don't worry about it :)
					 }
					 target = src.getComplete();
					 flt.generate(result,
								  watch[1], pipe,
								  count, target);
					 count = target;
				 } while (count < src.getTargetLength());
				 // Re-enable play button when data's ready
				 playB.setEnabled(true);
			 }
		 }
	 }


	 private class RendererThread extends Thread {

		 // Choose the data source
		 private Generator renderSrc;

		 public RendererThread(Generator src) { renderSrc = src; }

		 // Since rendering is necessarily done by the client, most of the
		 // RedererThread's business goes on there.
		 public void run() {
			 client.setResultData(result, renderSrc);
			 client.render(watch[2], renderSrc);
			 client.repaint();
			 //new PlayHandler(null).playSlice(result, 0, 49999);
		 }
		 
	 }

	 private Generator sourceGenerator(int kind) {
		 Generator g;
		 playB.setEnabled(true);
		 switch (kind) {
		 case 0: // A Sine-wave
			 g = new SquareWaveGenerator(1, 440.0, 0.8, 2000, 100);
			 asl.resize(200000);
			 return g;
		 case 1: // A Square-wave Approximation
			 g = new SquareWaveGenerator(6, 440.0, 0.8, 2000, 100);
			 asl.resize(200000);
			 return g;
		 case 2: // A Speech Sample
			 g = new AudioFileReader("/To_a_Mouse.wav");
			 asl.resize(g.getTargetLength());
			 return g;
		 case 3: // A Music Sample
			 g = new AudioFileReader("/Erlkönig.wav");
			 asl.resize(g.getTargetLength());
			 return g;
		 default:
			 System.err.println("Illegal generator choice: index "
								  + kind + " unsupported");
			 return null;
		 }
	 }

}
