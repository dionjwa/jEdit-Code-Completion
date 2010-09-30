package completion.rendering;


import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

public class CircleIcon implements Icon
{
	private static RenderingHints r = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	private Color color;
	private int width;
	private int height;

	private static final int DEFAULT_WIDTH = 8;

	private static final int DEFAULT_HEIGHT = 8;

	public CircleIcon(Color color)
	{
		this(color, true, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public CircleIcon(Color color, boolean selected)
	{
		this(color, selected, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public CircleIcon(Color color, boolean selected, int width, int height)
	{
		this.color = color;
		this.width = width;
		this.height = height;
	}

	public int getIconHeight()
	{
		return height;
	}

	public int getIconWidth()
	{
		return width;
	}

	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		Graphics2D g2 = (Graphics2D)g;
		r.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHints(r);
		g.setColor(color);
		g.translate(x, y);
		g.fillOval(0, 0, width, height);
		g.translate(-x, -y);
	}
}
