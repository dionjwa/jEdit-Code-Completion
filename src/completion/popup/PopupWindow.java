package completion.popup;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.JEditTextArea;

import completion.CompletionActions;
import completion.service.CompletionCandidate;

public class PopupWindow extends JWindow
{
    public int threadsRemaining;
    public boolean canHandleBackspace;
    protected final View view;
    protected final KeyHandler keyHandler;
    protected List<CompletionCandidate> candidates;
    protected List<CompletionCandidate> validCandidates;
    protected final JList list;
    protected boolean isActive;


    /**
     * Create a completion popup.
     * It is not shown until reset() method is called with valid
     * candidates. All key events for the view are intercepted by
     * this popup untill end of completion.
     * @since jEdit 4.3pre13
     */
    public PopupWindow(View view)
    {
        super(view);
        this.view = view;
        this.keyHandler = new KeyHandler();
        this.list = new JList();

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new CellRenderer());
        list.addKeyListener(keyHandler);
        list.addMouseListener(new MouseHandler());

        JPanel content = new JPanel(new BorderLayout());
        content.setFocusTraversalKeysEnabled(false);
        // stupid scrollbar policy is an attempt to work around
        // bugs people have been seeing with IBM's JDK -- 7 Sep 2000
        JScrollPane scroller = new JScrollPane(list,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        content.add(scroller, BorderLayout.CENTER);
        setContentPane(content);
        addWindowFocusListener(new WindowFocusHandler());

        isActive = false;

        canHandleBackspace = true;
    }

    public PopupWindow(View view, Point location)
    {
        this(view);
        if (location != null)
        {
            setLocation(location);
        }
    }

    /**
     * Quit completion.
     */
    @Override
    public void dispose()
    {
        if (isDisplayable())
        {
            if (view.getKeyEventInterceptor() == keyHandler)
            {
                view.setKeyEventInterceptor(null);
            }
            super.dispose();

            // This is a workaround to ensure setting the
            // focus back to the textArea. Without this, the
            // focus gets lost after closing the popup in
            // some environments. It seems to be a bug in
            // J2SE 1.4 or 5.0. Probably it relates to the
            // following one.
            // "Frame does not receives focus after closing
            // of the owned window"
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4810575
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    view.getTextArea().requestFocus();
                }
            });
        }
    }

    /**
     * Some completions may not be provided instantly.  Add them when they arrive.
     * @param candidates
     */
    public void addCompletions (List<CompletionCandidate> candidates)
    {
        this.candidates.addAll(candidates);
        threadsRemaining--;
        reset(isActive);
    }

    /**
     * Start completion.
     * @param active Set focus to the popup
     */
    public void reset (boolean active)
    {
        checkForValidCandidates();
        if(threadsRemaining == 0 && (validCandidates == null || candidates.size() <= 0))
        {
            dispose();
            return;
        }

        list.setModel(new CandidateListModel());
        list.setVisibleRowCount(Math.min(validCandidates.size(), 8));
        pack();
        setLocation(fitInScreen(getLocation(null),this,
            view.getTextArea().getPainter().getFontMetrics().getHeight()));
        if (active)
        {
            isActive = true;
            setSelectedIndex(0);
            GUIUtilities.requestFocus(this,list);
        }
        setVisible(true);
        view.setKeyEventInterceptor(keyHandler);
    }

    public void reset ()
    {
        reset(isActive);
    }

    /**
     * Current candidates of completion.
     */
    public List<CompletionCandidate> getCandidates()
    {
        return candidates;
    }

    /**
     * Returns index of current selection.
     * Returns -1 if nothing is selected.
     */
    public int getSelectedIndex()
    {
        return list.getSelectedIndex();
    }

    /**
     * Set selection.
     */
    public void setSelectedIndex(int index)
    {
        if (validCandidates != null && 0 <= index && index < validCandidates.size())
        {
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
            String description = validCandidates.get(index).getDescription();
            if (description != null)
            {
                view.getStatus().setMessageAndClear(description);
            }
        }
    }

    /**
     * Do completion with current selection and quit.
     */
    public boolean doSelectedCompletion()
    {
        int selected = list.getSelectedIndex();
        if (validCandidates != null && 0 <= selected && selected < validCandidates.size())
        {
            validCandidates.get(selected).complete(getCompletionPrefix(view.getTextArea()), view);
            dispose();
            return true;
        }
        return false;
    }


    public void keyTyped(KeyEvent evt)
    {
        char ch = evt.getKeyChar();
        if(ch == '\b' && !canHandleBackspace)
        {
            evt.consume();
            return;
        }

        keyTyped(ch);

        evt.consume();
    }

    protected void keyTyped(char ch)
    {
        // If no completion is selected, do not pass the key to
        // handleKeystroke() method. This avoids interfering
        // between a bit intermittent user typing and automatic
        // completion (which is not selected initially).
        int selected = getSelectedIndex();
        if(selected == -1)
        {
            view.getTextArea().userInput(ch);
            updateCompletion(false);
        }
        else if(handleKeystroke(selected, ch))
        {
            updateCompletion(true);
        }
        else {
            dispose();
        }
    }


    protected void keyPressed(KeyEvent evt)
    {
        // These code should be reduced to make this popup behave
        // like a builtin popup. But these are here to keep
        // compatibility with the old implementation before
        // refactoring out of CompletionPopup.
        switch(evt.getKeyCode())
        {
            case KeyEvent.VK_ENTER:
                keyTyped('\n');
                evt.consume();
                break;
            case KeyEvent.VK_TAB:
                keyTyped('\t');
                evt.consume();
                break;
            case KeyEvent.VK_SPACE:
                evt.consume();
                break;
            case KeyEvent.VK_BACK_SPACE:
                 if(!canHandleBackspace)
                 {
                     dispose();
                 }
                 break;
            case KeyEvent.VK_DELETE:
                dispose();
                break;
            default:
                break;
        }
    }

    /**
     * @param selectedIndex The index of the selected completion.
     * @param keyChar the character typed by the user.
     * @return True if completion should continue, false otherwise.
     * @since SideKick 0.3.2
     */
    public boolean handleKeystroke(int selectedIndex, char keyChar)
    {
        // if(keyChar == '\t' || keyChar == '\n')
        if(CompletionActions.acceptChars.indexOf(keyChar) > -1)
        {
            validCandidates.get(selectedIndex).complete(getCompletionPrefix(), view);
//            insert(selectedIndex);
//            if(SideKickActions.insertChars.indexOf(keyChar) > -1)
//                textArea.userInput(keyChar);
            return false;
        }
        else
        {
            view.getTextArea().userInput(keyChar);
            return true;
        }
    }

    private void updateCompletion(boolean active)
    {
//        SideKickCompletion newComplete = complete;
//        EditPane editPane = view.getEditPane();
//        JEditTextArea textArea = editPane.getTextArea();
//        int caret = textArea.getCaretPosition();
//        if(!newComplete.updateInPlace(editPane, caret))
//        {
//            newComplete = parser.complete(editPane, caret);
//        }
//        if(newComplete == null || newComplete.size() == 0)
//        {
//            dispose();
//        }
//        else
//        {
//            complete = newComplete;
//            setLocation(getLocation(textArea, caret, complete));
//            reset(new Candidates(), active);
//        }
    }

    protected void checkForValidCandidates ()
    {
        validCandidates = candidates;
        reset();
    }


    private static Point fitInScreen(Point p, Window w, int lineHeight)
    {
        Rectangle screenSize = w.getGraphicsConfiguration().getBounds();
        if(p.y + w.getHeight() >= screenSize.height)
            p.y = p.y - w.getHeight() - lineHeight;
        return p;
    }

    private void moveRelative(int n)
    {
        int selected = list.getSelectedIndex();

        int newSelect = selected + n;
        if (newSelect < 0)
        {
            newSelect = 0;
        }
        else
        {
            int numItems = list.getModel().getSize();
            if(numItems < 1)
            {
                return;
            }
            if(newSelect >= numItems)
            {
                newSelect = numItems - 1;
            }
        }

        if(newSelect != selected)
        {
            setSelectedIndex(newSelect);
        }
    }

    private void moveRelativePages(int n)
    {
        int pageSize = list.getVisibleRowCount() - 1;
        moveRelative(pageSize * n);
    }

    //{{{ passKeyEventToView() method
    private void passKeyEventToView(KeyEvent e)
    {
        // Remove intercepter to avoid infinite recursion.
        assert (view.getKeyEventInterceptor() == keyHandler);
        view.setKeyEventInterceptor(null);

        // Here depends on an implementation detail.
        // Use ACTION_BAR to force processing KEY_TYPED event in
        // the implementation of gui.InputHandler.processKeyEvent().
        view.getInputHandler().processKeyEvent(e, View.ACTION_BAR, false);

        // Restore keyHandler only if this popup is still alive.
        // The key event might trigger dispose() of this popup.
        if (this.isDisplayable())
        {
            view.setKeyEventInterceptor(keyHandler);
        }
    }

    protected String getCompletionPrefix ()
    {
        return getCompletionPrefix(view.getTextArea());
    }
    private static String getCompletionPrefix (JEditTextArea text, int caret)
    {
        return "";
    }

    private static String getCompletionPrefix (JEditTextArea text)
    {
        return getCompletionPrefix(text, text.getCaretPosition());
    }

    private class CandidateListModel extends AbstractListModel
    {
        public int getSize()
        {
            return validCandidates.size();
        }

        public Object getElementAt(int index)
        {
            // This value is not used.
            // The list is only rendered by components
            // returned by getCellRenderer().
            return validCandidates;
        }
    }

    private class CellRenderer implements ListCellRenderer
    {
        public Component getListCellRendererComponent(JList list,
            Object value, int index,
            boolean isSelected, boolean cellHasFocus)
        {
            return validCandidates.get(index).getCellRenderer().getListCellRendererComponent(list, validCandidates.get(index), index,
                isSelected, cellHasFocus);
        }
    }

    private class KeyHandler extends KeyAdapter
    {
        @Override
        public void keyPressed(KeyEvent e)
        {
            PopupWindow.this.keyPressed(e);

            if (validCandidates == null || validCandidates.size() == 0)
            {
                dispose();
            }
            else if (!e.isConsumed())
            {
                switch(e.getKeyCode())
                {
                case KeyEvent.VK_TAB:
                case KeyEvent.VK_ENTER:
                    if (doSelectedCompletion())
                    {
                        e.consume();
                    }
                    else
                    {
                        dispose();
                    }
                    break;
                case KeyEvent.VK_ESCAPE:
                    dispose();
                    e.consume();
                    break;
                case KeyEvent.VK_UP:
                    moveRelative(-1);
                    e.consume();
                    break;
                case KeyEvent.VK_DOWN:
                    moveRelative(1);
                    e.consume();
                    break;
                case KeyEvent.VK_PAGE_UP:
                    moveRelativePages(-1);
                    e.consume();
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    moveRelativePages(1);
                    e.consume();
                    break;
                default:
                    if(e.isActionKey()
                        || e.isControlDown()
                        || e.isAltDown()
                        || e.isMetaDown())
                    {
                        dispose();
                    }
                    break;
                }
            }

            if (!e.isConsumed())
            {
                passKeyEventToView(e);
            }
        }

        @Override
        public void keyTyped(KeyEvent e)
        {
            PopupWindow.this.keyTyped(e);

            if (validCandidates == null || validCandidates.size() == 0)
            {
                dispose();
            }

            if (!e.isConsumed())
            {
                passKeyEventToView(e);
            }
        }
    }

    private class MouseHandler extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent e)
        {
            if (doSelectedCompletion())
            {
                e.consume();
            }
            else
            {
                dispose();
            }
        }
    }

    private class WindowFocusHandler implements WindowFocusListener
    {
        public void windowGainedFocus(WindowEvent e)
        {
        }

        public void windowLostFocus(WindowEvent e)
        {
            dispose();
        }
    }


}
