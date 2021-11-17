import javax.swing.JTabbedPane;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.JPanel;
import java.awt.Dimension;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/** A class which makes publicly available all of the control
	parameters for each of the demonstrations **/

class Process extends JTabbedPane {
	// Controlled Parameters
	private JSpinner quantizationBits;
	private JCheckBox quantizationDither, quantizationLog;

	private JSlider resampRate;

	public Process() {
		setTabPlacement(JTabbedPane.RIGHT);

		// Quantization and Dither demo.
		Box quantization = new Box(BoxLayout.Y_AXIS);
		JPanel bp = new JPanel();
		bp.setLayout(new BoxLayout(bp, BoxLayout.X_AXIS));
		// Only valid bit-counts allowed
		SpinnerNumberModel snm
			= new SpinnerNumberModel(new Integer(16),  // Default
									 new Integer(1),   // Min
									 new Integer(16),  // Max
									 new Integer(1));  // Increment
		quantizationBits = new JSpinner(snm);
		//bp.add(new JLabel("Resolution:"));
		//bp.add(Box.createHorizontalStrut(5));
		bp.add(quantizationBits);
		bp.add(Box.createHorizontalStrut(5));
		bp.add(new JLabel("Bits"));
		// Contstrain to resize only vertically
		Dimension ps = bp.getPreferredSize();
		bp.setMaximumSize(new Dimension(Short.MAX_VALUE, ps.height));
		//quantizationBits.getEditor().setEditable(false);
		// Ask whether dither should be applied
		quantizationDither = new JCheckBox("Dithering");
		// Perhaps use something like A-law compression
		quantizationLog = new JCheckBox("Log Quantization");
		// Add components to this box
		quantization.add(bp);
		quantization.add(Box.createVerticalStrut(5));
		quantization.add(quantizationDither);
		quantization.add(Box.createVerticalStrut(5));
		quantization.add(quantizationLog);
		quantization.add(Box.createVerticalGlue());

		Box resamp = new Box(BoxLayout.Y_AXIS);
		resampRate = new JSlider(0, 400); // 1/50th of the real rate!

		final JLabel valLabel =
			new JLabel(String.valueOf(resampRate.getValue()*50));
		Box rslbls = new Box(BoxLayout.X_AXIS);
		rslbls.add(new JLabel("Sample Rate:"));
		rslbls.add(Box.createHorizontalGlue());
		rslbls.add(valLabel);

		resampRate.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent ce) {
					valLabel.setText(String.valueOf(((JSlider)ce.getSource()).getValue()*50));
				}
			}
		);

		resamp.add(rslbls);
		resamp.add(Box.createVerticalStrut(5));
		resamp.add(resampRate);
		resamp.add(Box.createVerticalGlue());

		addTab(null,
		       new VerticalTextIcon(" Quantization ", tabPlacement==JTabbedPane.RIGHT),
		       quantization);
		addTab(null,
		       new VerticalTextIcon(" Resampling ", tabPlacement==JTabbedPane.RIGHT),
		       resamp);
	}

	public Filter getProcessFilter(float[] data) {
		/**
		   Filter the source data according to the paramters
		   expressed in the widgets contained herein depending
		   on the selected demonstration.
		**/

		Filter flt = null;

		switch (getSelectedIndex()) {
		case 0: // Quantization
			flt = new Quantizer(data,
			                    ((Integer)quantizationBits.getValue()).intValue(),
			                    quantizationDither.isSelected(),
			                    quantizationLog.isSelected());
			break;
		case 1: // Resampling
			flt = new ReReSampler(data, resampRate.getValue()*50);
		}

		return flt;
	}
}
	
