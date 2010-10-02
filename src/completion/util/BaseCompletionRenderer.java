package completion.util;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import completion.service.CompletionCandidate;

public class BaseCompletionRenderer extends DefaultListCellRenderer
{
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        JLabel renderer = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        CompletionCandidate cc = (CompletionCandidate) value;
        renderer.setText(CompletionUtil.prefixByIndex(cc.getDescription(), index));
        return renderer;
    }
}
