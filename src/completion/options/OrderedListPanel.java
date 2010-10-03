package completion.options;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.util.StandardUtilities;


public class OrderedListPanel<T> extends JPanel
{

    public OrderedListPanel ()
    {
        super();

        caption = new JLabel("Completion plugin/service order");
        setLayout(new BorderLayout());
        add(BorderLayout.NORTH, caption);

        listModel = new DefaultListModel();

        list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new ListHandler());

        add(BorderLayout.CENTER,new JScrollPane(list));

        buttons = new JPanel();
        buttons.setBorder(new EmptyBorder(3,0,0,0));
        buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
        ActionHandler actionHandler = new ActionHandler();
        buttons.add(Box.createHorizontalStrut(6));
        buttons.add(Box.createHorizontalStrut(6));
        moveUp = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.moveUp.icon")));
        moveUp.setToolTipText(jEdit.getProperty("common.moveUp"));
        moveUp.addActionListener(actionHandler);
        buttons.add(moveUp);
        buttons.add(Box.createHorizontalStrut(6));
        moveDown = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.moveDown.icon")));
        moveDown.setToolTipText(jEdit.getProperty("common.moveDown"));
        moveDown.addActionListener(actionHandler);
        buttons.add(moveDown);
        buttons.add(Box.createGlue());

        updateButtons();
        add(BorderLayout.SOUTH, buttons);
    }

    public void setList (List<T> objects)
    {
        listModel.clear();
        for (T obj : objects) {
            listModel.addElement(obj);
        }
        updateButtons();
    }

    public List<T> getList ()
    {
        List<T> list = new ArrayList<T>();
        for (Object obj : listModel.toArray()) {
            list.add((T)obj);
        }
        return list;
    }

    private void updateButtons()
    {
        int index = list.getSelectedIndex();
        moveUp.setEnabled(index > 0);
        moveDown.setEnabled(index != -1 && index != listModel.getSize() - 1);
    }

    private DefaultListModel listModel;
    private JList list;
    private JButton moveUp, moveDown;
    private JLabel caption;
    private JPanel buttons;

    class ActionHandler implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            Object source = evt.getSource();

            if(source == moveUp)
            {
                int index = list.getSelectedIndex();
                Object selected = list.getSelectedValue();
                listModel.removeElementAt(index);
                listModel.insertElementAt(selected,index-1);
                list.setSelectedIndex(index-1);
                list.ensureIndexIsVisible(index - 1);
            }
            else if(source == moveDown)
            {
                int index = list.getSelectedIndex();
                Object selected = list.getSelectedValue();
                listModel.removeElementAt(index);
                listModel.insertElementAt(selected,index+1);
                list.setSelectedIndex(index+1);
                list.ensureIndexIsVisible(index+1);
            }
        }
    }

    class ListHandler implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent evt)
        {
            updateButtons();
        }
    }

    static class ItemCompare implements Comparator<Object>
    {
        public int compare(Object obj1, Object obj2)
        {
            return StandardUtilities.compareStrings(obj1.toString(), obj2.toString(), true);
        }
    }
}
