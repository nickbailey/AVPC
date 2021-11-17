import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import java.awt.Font;
import java.awt.font.LineMetrics;
import java.awt.font.FontRenderContext;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.geom.AffineTransform;
import java.awt.Toolkit;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public class VerticalTextIcon implements Icon, SwingConstants { 
	private Font font = UIManager.getFont("Label.font"); 
	private LineMetrics lm;
 
	private String text; 
	private double width, height; 
	private boolean clockwise; 
 
	public VerticalTextIcon(String text, boolean clockwise){ 
		FontRenderContext frc = new FontRenderContext(null, false, false);
		Rectangle2D bounds = font.getStringBounds(text, frc);
		width = bounds.getWidth();
		height = bounds.getHeight();
		this.text = text; 
		this.clockwise = clockwise; 
	} 
 
	public void paintIcon(Component c, Graphics g, int x, int y){ 
		Graphics2D g2 = (Graphics2D)g;
		FontRenderContext frc = g2.getFontRenderContext();
		lm = font.getLineMetrics(text, frc); 
		//height = lm.getHeight(); 
		Font oldFont = g.getFont(); 
		Color oldColor = g.getColor(); 
		AffineTransform oldTransform = g2.getTransform(); 
 
		g.setFont(font); 
		g.setColor(Color.black); 
		if (clockwise){ 
			g2.translate(x+getIconWidth(), y); 
			g2.rotate(Math.PI/2); 
		} else { 
			g2.translate(x, y+getIconHeight()); 
			g2.rotate(-Math.PI/2); 
		} 
		g.drawString(text, 0, (int)(lm.getLeading()+lm.getAscent())); 
 
		g.setFont(oldFont); 
		g.setColor(oldColor); 
		g2.setTransform(oldTransform); 
	} 
 
	public int getIconWidth(){ 
		return (int)height; 
	} 
 
	public int getIconHeight(){ 
		return (int)width; 
	} 
}
