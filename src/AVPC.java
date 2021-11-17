// File AVPC.java, the main entry point for all of the
// audio and video processing and coding examples.

import java.lang.String;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import java.awt.Container;
import java.awt.Dimension;

import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

class AVPC extends JFrame {

	static final int sampleRate = 44100;

	public AVPC() {
		JPanel mainPanel = new JPanel();
		Process p = new Process();
		AudioTDGraph tdg = new AudioTDGraph();
		AudioView av = new AudioView(tdg, p);
		Container pane = getContentPane();
		mainPanel.setLayout(new BoxLayout(mainPanel,
										  BoxLayout.X_AXIS));
		p.setPreferredSize(new Dimension(p.getPreferredSize().width,
										 av.getPreferredSize().height));
		p.setMaximumSize(new Dimension(p.getPreferredSize().width,
									   Short.MAX_VALUE));
		addWindowListener(new MyWindowListener());
		mainPanel.add(av);
		mainPanel.add(p);
		pane.add(mainPanel);
		pack();
		setVisible(true);
	}


	public static void main(String[] args) {
		AVPC avpc = new AVPC();
	}

	private class MyWindowListener extends WindowAdapter {
		public void windowClosed(WindowEvent we) {
			System.out.println("Finishing up");
			System.exit(0);
		}
	}
}
