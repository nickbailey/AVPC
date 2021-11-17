import java.awt.Dimension;
import java.awt.geom.Line2D;
import java.awt.geom.Line2D.Float;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.awt.Color;
import java.awt.Font;
import java.lang.Math;
import java.awt.geom.Rectangle2D;
import java.awt.Point;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputAdapter;
import java.text.DecimalFormat;

public class AudioTDGraph extends AudioGraph {

	// Constants describing appearance

	static private final Color graphBGColor   = new Color(  0,  40,  70);
	static private final Color srcDataColor   = new Color(255, 150,   0);
	static private final Color resDataColor   = new Color(150, 255,   0);
	static private final Color ovlDataColor   = new Color(255, 255,   0);
	static private final Color errDataColor   = new Color(150,   0,   0);
	static private final Color scaleColor     = new Color(  0,  70, 255);
	static private final Color scaleTextColor = new Color( 60, 170, 255);
	static private final Color messageColor   = new Color(180,   0, 200);
	static private final Color selectedColor  = new Color(100,   0, 140);
	static private final Color extentColor    = new Color(200, 200, 200);

	static private final Font messageFont =
		new Font("SansSerif", Font.BOLD, 60);
	static private final Font scaleFont = 
		new Font("Dialog", Font.PLAIN, 12);
	static private final double dbValues[] = {0, -1.0, -2.0, -3.0,
											  -5.0, -10.0, -20.0};
	// Preferred multipliers for time axis
	static private final double preferredTimeValues[] = {1.0, 2.0, 5.0};
	static private final PreferredValue ptv =
		new PreferredValue(preferredTimeValues);
	static private final int divsPerScreen = 6; // just a hint. Uses pref vals
	static private final DecimalFormat timeFmt =
		new DecimalFormat("##0.##E0"); // How time values are formatted

	// Distance between final graticule line and drawing area
	static private final int topBorder = 8;
	static private final int rightBorder = 6;
	static private final int leftBorder = 28;
	static private final int bottomBorder = 30;
	static private final int selMargin = 2; // Pixels selection overlaps graph

	private boolean doResult = false;       // Only plot the results if valid

	private Line2D.Float[] srcLines;        // Source signal plot lines
	private Line2D.Float[] resLines;        // Result lines
	private Line2D.Float[] ovlLines;        // Parts of the above that overlap
	private Line2D.Float[] errLines;        // Lines displaying error energy
	private int xOffset = 0;                // Start plot at this cached pixel
	private int dataOffset = 0;             // left-most scroll in data array
	private int dataLength = 0;             // length of zoomed portion of data
	private int dataExtent = 0;             // No. of samples actually plotted
	private int selectionStart;             // Start & end of selection
	private int selectionEnd;
	private boolean selected = false;       // Whether any area's selected
	private LinkedList<Context> contextStack =
		new LinkedList<Context>();          // Context stack for zooming

	private ProgressWatcher renderW = null; // Render monitor

	private Object plotDataMutex = new Object(); // Lock over plot data arrays
	private PlotDataWorker pdw = null;      // Task which updates plot cache
	private boolean canMakePlotData = true; // plotData method termination

	private int w, h;                       // Dims of graph area
	private int cachedLines = 0;            // Dims of cached plot data

	public AudioTDGraph() {
		super();
		setBackground(graphBGColor);
		MyMouseHandler mh = new MyMouseHandler();
		addMouseListener(mh);
		addMouseMotionListener(mh);
	}

	private void makePlotData() {
		// Default to using the current dimensions
		makePlotData(dispDims);
	}

	private void makePlotData(Dimension d) {
		/** Generate line drawing data so that the
            display can be redrawn very quickly **/
		if (data != null) {
			double smax = -1.0, smin = 1.0;
			double rmax = -1.0, rmin = 1.0;
			int pxl = 0, curpxl;
			int count = dataOffset;
			int target = 0;
			cachedLines = sampleToPixel(dataOffset+dataLength)
				- sampleToPixel(dataOffset);
			doResult = (result != null && result.length >= dataLength);
			srcLines = new Line2D.Float[cachedLines];
			if (doResult) {
				resLines = new Line2D.Float[cachedLines];
				ovlLines = new Line2D.Float[cachedLines];
				errLines = new Line2D.Float[cachedLines];
			}
			do  {
				if (gen != null) {
					// Wait if data is yet to become valid
					try {
						Thread.currentThread().sleep(5000);
					} catch (InterruptedException e) {}
					// If data is being generated, do as much as we can.
					// Otherwise just do the whole thing.
					target = gen.getComplete();
				} else
					target = dataOffset+dataLength;

				if (!newData) target = dataOffset+dataLength;

				// Calculate the line to draw at each pixel position
				for (int x = count; x < target; x++) {
					// Scan source data for max and min values
					if (data[x] < smin) smin = data[x];
					if (data[x] > smax) smax = data[x];
					if (doResult) {
						if (result[x] < rmin) rmin = result[x];
						if (result[x] > rmax) rmax = result[x];
					}
					curpxl = (int)(((long)(x-dataOffset) * (long)(cachedLines-1)) /
								   (long)dataLength);
					if (pxl != curpxl) { // Moved into next pixel; add plot
						if (!canMakePlotData) // Time to bail out
							break;
						float stop = (float)(0.5*h*(1.0-smax)-1.0 + topBorder);
						float sbot = (float)(0.5*h*(1.0-smin) + topBorder);
						float xc = pxl + leftBorder + 1;
						if (doResult) {  // Calculate overlaps
							float rtop = (float)(0.5*h*(1.0-rmax)-1.0 + topBorder);
							float rbot = (float)(0.5*h*(1.0-rmin) + topBorder);
							// Choose appropriate drawing regime
							if (stop > rbot || rtop > sbot) {
								// No overlap
								srcLines[pxl] =
									new Line2D.Float(xc, stop, xc, sbot);
								resLines[pxl] =
									new Line2D.Float(xc, rtop, xc, rbot);
								errLines[pxl] = (rtop > sbot) ?
									new Line2D.Float(xc, sbot+1, xc, rtop-1) :
									new Line2D.Float(xc, rbot+1, xc, stop-1);
							} else if (stop <= rtop && sbot >= rbot) {
								// Total overlap, source line bigger or same
								srcLines[pxl] = 
									new Line2D.Float(xc, stop, xc, sbot);
								ovlLines[pxl] =
									new Line2D.Float(xc, rtop, xc, rbot);
							} else if (stop >= rtop && sbot <= rbot) {
								// Total overlap, source line smaller
								srcLines[pxl] = 
									new Line2D.Float(xc, rtop, xc, rbot);
								ovlLines[pxl] =
									new Line2D.Float(xc, stop, xc, sbot);
							} else if (stop < rtop) {
								// Partial overlap, source line above
								srcLines[pxl] =
									new Line2D.Float(xc, stop, xc, rtop);
								ovlLines[pxl] =
									new Line2D.Float(xc, rtop, xc, sbot);
								resLines[pxl] =
									new Line2D.Float(xc, sbot, xc, rbot);
							} else {
								// Partial overlap, result line above
								resLines[pxl] =
									new Line2D.Float(xc, rtop, xc, stop);
								ovlLines[pxl] =
									new Line2D.Float(xc, stop, xc, rbot);
								srcLines[pxl] =
									new Line2D.Float(xc, rbot, xc, sbot);
							}
						} else {               // Just show source data
							srcLines[pxl] = new Line2D.Float(xc, stop, xc, sbot);
						}
						pxl = curpxl;// Consider new pixel
						x--;        // Overlap one sample (=> continuous lines)
						smax = -1.0;// Reset max and min points
						smin = 1.0;
						rmax = -1.0; rmin = 1.0;
						if (renderW != null) // report progress
							renderW.progress((double)pxl/(cachedLines-1));
					}
				}
			count = target;
			} while (canMakePlotData && 
					 gen != null &&
					 count < dataOffset+dataLength);
		}
		// Only declare display valid if we finished
		dispValid = canMakePlotData;
		canMakePlotData = true;
		// Data is read and processed
		newData = false;
	}

	private class PlotDataWorker extends Thread {
		public void abort() { canMakePlotData = false; }
		public void run() {
			synchronized (plotDataMutex) {
				// Got the lock: go ahead and make the plot data
				canMakePlotData = true;
				makePlotData();
				// No further changes to the plot data so redraw now,
				// but only if there is not a more recent paint pending
				if (dispValid)
					repaint();
			}
		}
	}

	public void paint(Graphics g) {
		Dimension d = getSize();
		Graphics2D g2d = (Graphics2D)g;
		Rectangle area = g2d.getClipBounds();
		FontRenderContext frc = g2d.getFontRenderContext();

		// Clear the working area
		// g2d.clearRect(Rectangle) is undefined (!!)
		g2d.setBackground(getBackground());
		g2d.clearRect(area.x, area.y, area.width, area.height);
		// Only recalculate line data if overall geometry has changed.
		// Do this in the background so that the dialogue plots the progress
		if (!dispDims.equals(d)) dispValid = false;
		if (!dispValid) {
			// Validate graph width and height
			dispDims = d;
			h = d.height - (topBorder + bottomBorder);
			w = d.width - (leftBorder + rightBorder);
			// Invalidate any currently running thread's activities
			if (pdw != null)
				pdw.abort();
			// This thread calls repaint when it finishes successfully..
			pdw = new PlotDataWorker();
			pdw.start();
		} else {
			// Now observe the mutual exclusion on the data drawing arrays
			synchronized (plotDataMutex) {
				if (data != null) {
					if (selected) {
						// Handle range selection first
						g2d.setColor(selectedColor);
						g2d.fillRect(selectionStart + leftBorder + 1,
									 topBorder - selMargin,
									 selectionEnd - selectionStart,
									 h + 2*selMargin);
					}
					// Surrounding rectangle
					g2d.setColor(scaleColor);
					g2d.drawRect(leftBorder,
								 topBorder,
								 w-1, h);
					// Draw the graticule
					// Y-axis Labels
					for (int yal = 0; yal < dbValues.length; yal++) {
						TextLayout tl =
							new TextLayout(Integer.toString((int)dbValues[yal]),
										   scaleFont, frc);
						Rectangle2D b = tl.getBounds();
						float yalX = leftBorder - (float)(b.getWidth() + 3);
						float ctrY = topBorder + h/2;
						float offsY =
							(float)(h/2 * Math.pow(10.0, dbValues[yal]/20.0));
						float yalY;
						g2d.setColor(scaleTextColor);
						yalY = ctrY - offsY + (float)(b.getHeight()/2.0);
						tl.draw(g2d, yalX, yalY);
						yalY = ctrY + offsY + (float)(b.getHeight()/2.0);
						tl.draw(g2d, yalX, yalY);
						g2d.setColor(scaleColor);
						yalY = ctrY - offsY;
						g2d.drawLine(leftBorder, (int)yalY,
									 leftBorder + w - 2, (int)yalY);
						yalY = ctrY + offsY;
						g2d.drawLine(leftBorder, (int)yalY,
									 leftBorder + w - 2, (int)yalY);
					}
					// X-axis Labels
					double range =
						(double)dataExtent / (double)AVPC.sampleRate;
					double gap = ptv.nearest(range/divsPerScreen);
					double startTime = 
						Math.ceil((double)pixelToSample(xOffset)
								  / ((double)AVPC.sampleRate * gap))
						* gap;
					for (int i = 0; i <= 2*divsPerScreen; i ++) {
						g2d.setColor(scaleColor);
						int ypos =
							sampleToPixel((int)((double)AVPC.sampleRate
												* (startTime + i*gap)))
							- xOffset;
						//System.out.println("Time: "+(startTime + i*gap) + "  Pixel: " + sampleToPixel((int)((double)AVPC.sampleRate * (startTime + i*gap))) + " ypos: "+ypos);
						if (ypos > leftBorder && ypos < leftBorder+w) {
							g2d.drawLine(ypos, topBorder, ypos, topBorder+h);
							TextLayout tl =
								new TextLayout(timeFmt.format(startTime+i*gap),
											   scaleFont, frc);
							Rectangle2D b = tl.getBounds();
							g2d.setColor(scaleTextColor);
							tl.draw(g2d,
									(float)(ypos - b.getWidth()/2),
									(float)(b.getHeight() + topBorder + h + 5));
						}
					}
					// Indication of current zoom extent
					g2d.setColor(scaleColor);
					g2d.fillRect(leftBorder, d.height - 10, w, 4);
					g2d.setColor(extentColor);
					// Don't get confused now...
					// data.length = total loaded data length;
					// dataLength = length of data in zoomed area.
					g2d.fillRect((int)((double)w * 
									   (double)dataOffset/(double)data.length)
								 + leftBorder, d.height - 12,
								 (int)((double)w *
									   (double)dataLength/(double)data.length),
								 8);
					// Which lines to plot, given that all line arrays have the
					// same length.
					int startx = Math.max(0, area.x-leftBorder-1) + xOffset;
					int	endx = Math.min(area.x + area.width - leftBorder,
										w - 1) + xOffset;

					// Draw graph of source data
					// Draw in error shade first
					if (doResult) {
						g2d.setColor(errDataColor);
						for (int i = startx;
							 i < endx && i < resLines.length;
							 i++)
							if (errLines[i] != null) {
								Line2D.Float l = errLines[i];
								g2d.drawLine((int)l.x1 - xOffset, (int)l.y1,
											 (int)l.x2 - xOffset, (int)l.y2);
							}
					}
					// Always draw main graph
					g2d.setColor(srcDataColor);
					for (int i = startx;
						 i < endx && i < srcLines.length;
						 i++)
						if (srcLines[i] != null) {
							Line2D.Float l = srcLines[i];
							g2d.drawLine((int)l.x1-xOffset, (int)l.y1,
										 (int)l.x2-xOffset, (int)l.y2);
						}
					// Draw restult of filtering operation,
					// then draw overlapping areas
					if (doResult) {
						g2d.setColor(resDataColor);
						for (int i = startx;
							 i < endx && i < resLines.length;
							 i++)
							if (resLines[i] != null) {
								Line2D.Float l = resLines[i];
								g2d.drawLine((int)l.x1-xOffset, (int)l.y1,
											 (int)l.x2-xOffset, (int)l.y2);
							}
						g2d.setColor(ovlDataColor);
						for (int i = startx;
							 i < endx && i < ovlLines.length;
							 i++)
							if (ovlLines[i] != null) {
								Line2D.Float l = ovlLines[i];
								g2d.drawLine((int)l.x1-xOffset, (int)l.y1,
											 (int)l.x2-xOffset, (int)l.y2);
							}
					}
				} else { // No data has been loaded yet
					g2d.setColor(messageColor);
					String msg = "No data loaded";
					TextLayout tl = new TextLayout(msg, messageFont, frc);
					Rectangle2D b = tl.getBounds();
					Point torg = new Point((int)(d.width - b.getWidth())/2,
										   (int)(d.height + b.getHeight())/2);
					tl.draw(g2d, (float)torg.getX(), (float)torg.getY());
				}
			}
		}
	}

	// This graph accepts selection (drag) events, so here's the handler
	private class MyMouseHandler extends MouseInputAdapter {

		public void mouseDragged(MouseEvent me) {
			// Get coordinates relative to graph area
			me.translatePoint(-(leftBorder+1), -(topBorder+1));
			int x = me.getX();

			if (data == null || x < 0 || x > w) // bail out outside graph area
				return;

			modifySelection(x);
		}

		public void mousePressed(MouseEvent me) {
			// Get coordinates relative to graph area
			me.translatePoint(-(leftBorder+1), -(topBorder+1));
			int x = me.getX();
			
			if (data == null || x < 0 || x > w) // bail out outside graph area
				return;
			if (me.isShiftDown())
				// Shift => alter current selection.
				modifySelection(x);
			else {
				// When a click happens, start a selection
				selectionStart = x; selectionEnd = x;
				selected = false;
				repaint();
			}
		}

		private void modifySelection(int x) {
			if (x < 0 || x > w) // bail out if outside graph area
				return;

			int oldStart = selectionStart;
			int oldEnd = selectionEnd;

			if (selectionStart == selectionEnd) {
				// For a new selection, direct modification to
				// the appropriate variable
				if (x < selectionStart)
					selectionStart = x;
				else
					selectionEnd = x;
				selected = false;
			} else {
				// For a continuing selection, the
				// variable nearest to the endpoint is changed
				if (Math.abs(selectionStart - x) < Math.abs(selectionEnd - x))
					selectionStart = x;
				else
					selectionEnd = x;
				selected = true;
			}

			// Paint out the old selection and paint in the new one
			// The Swing run-time support will glue these together into
			// a single sensible call to paint(), I'm promised
			repaint(oldStart + leftBorder, topBorder,
					oldEnd - oldStart + 1, h);
			repaint(selectionStart + leftBorder, topBorder,
					selectionEnd - selectionStart + 1, h);
		}

	}

	// Implement a method of pushing and popping the context to satisfy
	// requirements of the Zoomable interface
	
	private class Context {
		public Line2D.Float[] srcLines, resLines, ovlLines, errLines;
		public int xOffset;
		public int dataOffset, dataLength, dataExtent, cachedLines;
		public Dimension dispDims;
		Context(Line2D.Float[] srcl,
				Line2D.Float[] resl,
				Line2D.Float[] ovll,
				Line2D.Float[] errl,
				int pxloffs,
				int dataoffs,
				int datalen,
				int dataExt,
				int cachedLns,
				Dimension dispD) {
			srcLines    = srcl;
			resLines    = resl;
			ovlLines    = ovll;
			errLines    = errl;
			xOffset     = pxloffs;
			dataOffset  = dataoffs;
			dataLength  = datalen;
			dataExtent  = dataExt;
			cachedLines = cachedLns;
			dispDims    = dispD;
		}
	}
	
	public void saveContext() {
		contextStack.add(new Context(srcLines,
									 resLines,
									 ovlLines,
									 errLines,
									 xOffset,
									 dataOffset,
									 dataLength,
									 dataExtent,
									 cachedLines,
									 dispDims));
	}

	public ScrollHints restoreContext() {
		ScrollHints rv = null;
		try {
			Context c   = (Context)contextStack.removeLast();
			srcLines    = c.srcLines;
			resLines    = c.resLines;
			ovlLines    = c.ovlLines;
			errLines    = c.errLines;
			xOffset     = c.xOffset;
			dataOffset  = c.dataOffset;
			dataLength  = c.dataLength;
			dataExtent  = c.dataExtent;
			cachedLines = c.cachedLines;

			// Must force redraw if the screen was resized
			if (!c.dispDims.equals(dispDims)) {
				dispDims = c.dispDims;
				dispValid = false;
			}

			rv = new ScrollHints(dataOffset, dataOffset+dataLength,
								 pixelToSample(xOffset), dataExtent);

			repaint();

		} catch (NoSuchElementException e) { }

		return rv;
	}

	public ScrollHints zoomToSelection() {
		ScrollHints rv = null;

		if (selected) {               // Provided there's a current selection
			saveContext();            // Remember where we were
			selected = false;         // Current selection no longer valid
			// Set the new portion of the data array to display
			int newDataOffset = pixelToSample(xOffset);
			int sliderMax = pixelToSample(xOffset + w);
			int sliderPos = pixelToSample(xOffset + selectionStart);
			int newDataLength = sliderMax - newDataOffset;
			dataExtent = pixelToSample(xOffset + selectionEnd)
				- pixelToSample(xOffset + selectionStart);
			// Set all of the local properties
			dataOffset = newDataOffset;
			dataLength = newDataLength;
			cachedLines = sampleToPixel(dataOffset+dataLength)
				- sampleToPixel(dataOffset);
			System.out.println("Lines: "+cachedLines+" @"+dataOffset+"+"+dataLength+" for "+dataExtent);
			dispValid = false;
			repaint();
			rv = new ScrollHints(dataOffset, dataOffset+dataLength,
								 sliderPos, dataExtent);
		}
		return rv;
	}

	public void setXOrigin(int x) {
		xOffset = sampleToPixel(x);
		//System.out.println("New origin: "+sampleToPixel(x)+"  Last line to draw: "+(sampleToPixel(x)+w)+"  Cached lines: "+cachedLines);
		if (cachedLines > 0) // Anything to draw yet?
			repaint();
	}

	private int pixelToSample(int pxl) {
		// Given the pixel index within the graph area, work out which
		// sample is being referred to in the data or result array.
		int sample =  dataOffset +
			(int)((double)dataExtent*(double)pxl/(double)w);
		// Check that's not out of bounds of the data array
		return Math.min(sample, data.length - 1);
	}

	private int sampleToPixel(int sample) {
		return (int)((double)w * ((double)sample - (double)dataOffset) /
					 (double)dataExtent);
	}

	// Try to precompute image at load-time
	public void render(ProgressWatcher pw, Generator g) {
		renderW = pw;
		gen = g;
		// Make the plot data for the current size of display
		// Don't upset any current redraw
		synchronized (plotDataMutex) {
			makePlotData(dispDims);
		}
		// Tell the superclass no need to go through all that again
		gen = null;
	}
	
	// Remember the data length for plot data generation when a load happens
	public ScrollHints setSourceData(float[] data) {
		super.setSourceData(data);       // Ignores returned ScrollHint

		// Generate internal data structures relating to source data,
		// but since this is a time-domain graph, that's not hard...
		dataLength = data.length;        // Display whole sample...
		dataOffset = 0;                  // ... from the beginning...
		xOffset = 0;                     // ... with no slider offset...
		dataExtent = data.length;        // ... so all samples are visible.
		contextStack =
		  new LinkedList<Context>();     // Forget the zoom context
		cachedLines = 0;                 // Mark cache as empty

		// Return the new settings we'd like for the scroll bar
		// (not very important, because we can't scroll until zoomed)
		return 	new ScrollHints(dataOffset, dataLength, xOffset, dataLength);
	}
}
