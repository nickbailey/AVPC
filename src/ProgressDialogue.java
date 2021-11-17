import javax.swing.JComponent;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;


class ProgressDialogue extends JDialog {

	/**
	   Dialogue to monitor the prgress of loading a new data source.
	   Maintains a list of ProgressWatchers, and provides an abort
	   and hide button
	**/

	private JPanel dp = new JPanel();
	
	public ProgressDialogue(JComponent parent, ProgressWatcher[] pws) {
		super();
		JLabel l;
		//setResizable(false);
		//setUndecorated(true);
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		dp.setLayout(gbl);
		c.weighty = 1.0;
		c.ipadx = 5; c.ipady = 5;
		c.fill = GridBagConstraints.HORIZONTAL;
		for (int i = 0; i < pws.length; i++) {
			c.weightx = 0;
			c.gridx = 0; c.gridy = i; c.gridwidth = 1;
			l = new JLabel(pws[i].getDescription(), JLabel.RIGHT);
			gbl.setConstraints(l, c);
			dp.add(l);
			c.gridx = 1;
			c.weightx = 1.0; c.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(pws[i], c);
			dp.add(pws[i]);
		}
		JButton hideb = new JButton("Hide");
		JButton cancelb = new JButton("Cancel");
		JLabel title = new JLabel("Update Progress",
								  new ImageIcon("clock.png"),
								  JLabel.LEFT);
		c.gridx = 0; c.gridy = pws.length;
		c.gridwidth = 2; c.weightx = 1.0;
		gbl.setConstraints(title, c);
		dp.add(title);
		c.fill = GridBagConstraints.NONE;
		c.gridx = GridBagConstraints.RELATIVE; c.weightx = 0;
		c.gridwidth = 1;
		gbl.setConstraints(hideb, c); dp.add(hideb);
		gbl.setConstraints(cancelb, c); dp.add(cancelb);
		setTitle("Update Progress");
		getContentPane().add(dp);
		pack();
		// This could go in Audioview.java:220 or thereabouts,
		// but I prefer it hanging around quietly in the background
		setVisible(true);
	}
}
