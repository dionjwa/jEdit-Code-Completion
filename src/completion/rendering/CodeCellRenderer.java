package completion.rendering;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import completion.service.CompletionCandidate;

public class CodeCellRenderer implements ListCellRenderer
{
	private static Icon greenCircleIcon = new CircleIcon(new Color(20,202,59));
	private static Icon magentaDiamondIcon = new DiamondIcon(new Color(126, 20, 20));
	private static Icon greyCircleIcon = new CircleIcon(Color.gray);
	protected Icon classImageIcon;
	protected CodeCompletionType type;

	protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

	public CodeCellRenderer (CodeCompletionType type)
	{
		ClassLoader cldr = this.getClass().getClassLoader();
		java.net.URL imageURL = cldr.getResource("icons/classicon.png");
		classImageIcon = new ImageIcon(imageURL);
		this.type = type;
	}

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		renderer.setMinimumSize(new Dimension(1, 16));

		if (value instanceof CompletionCandidate)
		{
		    CompletionCandidate cc = (CompletionCandidate)value;

			switch(type)
			{
				case METHOD:
				{
					renderer.setIcon(greenCircleIcon);
					renderer.setText(cc.getDescription());
					break;
				}

				case FIELD:
				{
					renderer.setIcon(magentaDiamondIcon);
					renderer.setText(cc.getDescription());
					break;
				}

				case VARIABLE:
				{
					renderer.setIcon(greyCircleIcon);
					renderer.setText(cc.getDescription());
					break;
				}

				case CLASS:
				{
					renderer.setIcon(classImageIcon);
					renderer.setText(cc.getDescription());
					break;
				}

				default :
				{
					renderer.setText(cc.getDescription());
					break;
				}
			}

		}
		else
		{
			renderer.setText(value.toString());
		}
		return renderer;
	}
}

