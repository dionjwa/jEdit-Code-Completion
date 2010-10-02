package completion.popup;

import static completion.CompletionPlugin.trace;

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
import java.util.ArrayList;
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
import completion.util.CompletionUtil;

public class CompletionPopup extends JWindow
{
    /**
     * Each completion service launches in a new thread, so if there are no completions we need
     * to know if that's simply due to a completion service thread not yet arrived.
     */
    public int threadsRemaining;
    public boolean canHandleBackspace;
    protected final View view;
    protected final KeyHandler keyHandler;
    protected List<CompletionCandidate> candidates;
    protected List<CompletionCandidate> validCandidates;
    protected final JList list;
    protected final int initialCaretPos;
    protected boolean requestFocus;
    protected boolean hasFocus;

    /**
     * Create a completion popup.
     * It is not shown until reset() method is called with valid
     * candidates. All key events for the view are intercepted by
     * this popup untill end of completion.
     * @since jEdit 4.3pre13
     */
    public CompletionPopup(View view)
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

        canHandleBackspace = true;

        candidates = new ArrayList<CompletionCandidate>();
        validCandidates = new ArrayList<CompletionCandidate>();

        setLocation(getLocation(view.getTextArea(), view.getTextArea().getCaretPosition(), CompletionUtil.getCompletionPrefix(view)));
        initialCaretPos = view.getTextArea().getCaretPosition();

        requestFocus = false;
        hasFocus = false;
        setVisible(false);
    }

    public CompletionPopup(View view, Point location)
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
                @Override
                public void run()
                {
                    view.getTextArea().requestFocus();
                }
            });
        }
    }

    /**
     * Some completions may not be provided instantly.  Add them when they arrive.
     * @param candidates Can be null, as this method still has to be called by the
     * thread worker to reduce the thread count.
     */
    public void addCompletions (List<CompletionCandidate> newCandidates)
    {
        if (newCandidates != null) {
            candidates.addAll(newCandidates);
        }
        threadsRemaining--;
        trace("CompletionPopup added " + (newCandidates == null ? "0" : newCandidates.size()) + " candidates, threads remaining=" + threadsRemaining);
        reset();
    }

    /**
     * Start completion.
     * @param nowActive Set focus to the popup
     */
    public void reset (boolean nowActive)
    {
        checkForValidCandidates();
        if(threadsRemaining == 0 && validCandidates.isEmpty())
        {
            dispose();
            return;
        }

        if (threadsRemaining == 0 && validCandidates.size() == 1 && CompletionActions.isCompleteInstant && doSelectedCompletion()) {
            return;
        }

        if (validCandidates.isEmpty() && isVisible()) {
            setVisible(false);
        } else if (!validCandidates.isEmpty() && !isVisible()) {
            setVisible(true);
        }

        list.setModel(new CandidateListModel());
        list.setVisibleRowCount(Math.min(validCandidates.size(), 8));
        pack();
        setLocation(fitInScreen(getLocation(null),this,
            view.getTextArea().getPainter().getFontMetrics().getHeight()));

        requestFocus = nowActive;
        //If we're already in focus, ignore focus requests
        requestFocus = requestFocus && !hasFocus;

        if (requestFocus && !validCandidates.isEmpty())
        {
            requestFocus = false;
            hasFocus = true;
            setSelectedIndex(0);
            GUIUtilities.requestFocus(this,list);
        }


//        setVisible(true);
        view.setKeyEventInterceptor(keyHandler);
    }

    public void reset ()
    {
        reset(false);
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
        if (0 <= index && index < validCandidates.size())
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
        if (validCandidates.size() == 1) {
            selected = 0;
        }
        if (0 <= selected && selected < validCandidates.size())
        {
            validCandidates.get(selected).complete(view);
            dispose();
            return true;
        }
        return false;
    }

    protected void keyPressed(KeyEvent evt)
    {
        handleControlKeys(evt);
        if (evt.isConsumed()) {
            return;
        }

        char keyChar = evt.getKeyChar();
        int selectedIndex = getSelectedIndex();
        //If the popup isn't selected, pass the keystroke to the buffer
        if(selectedIndex == -1) {
            if (CompletionActions.acceptChars.indexOf(keyChar) > -1 && validCandidates.size() == 1) {
                validCandidates.get(0).complete(view);
                dispose();
            } else {
                if (Character.isJavaIdentifierPart(keyChar)) {
                    view.getTextArea().userInput(keyChar);
                }
                reset();
            }
        } else if (CompletionActions.acceptChars.indexOf(keyChar) > -1) {
            validCandidates.get(selectedIndex).complete(view);
            dispose();
        } else {
            if (Character.isJavaIdentifierPart(keyChar)) {
                view.getTextArea().userInput(keyChar);
            }
            reset();
        }

        if (view.getTextArea().getCaretPosition() < initialCaretPos) {
            dispose();
        }
        evt.consume();
    }

    protected void handleControlKeys(KeyEvent evt)
    {
        switch(evt.getKeyCode())
        {
            case KeyEvent.VK_TAB:
            case KeyEvent.VK_ENTER:
                if (doSelectedCompletion())
                {
                    evt.consume();
                }
                else
                {
                    dispose();
                }
                break;
            case KeyEvent.VK_ESCAPE:
                dispose();
                evt.consume();
                break;
            case KeyEvent.VK_UP:
                moveRelative(-1);
                evt.consume();
                break;
            case KeyEvent.VK_DOWN:
                moveRelative(1);
                evt.consume();
                break;
            case KeyEvent.VK_PAGE_UP:
                moveRelativePages(-1);
                evt.consume();
                break;
            case KeyEvent.VK_PAGE_DOWN:
                moveRelativePages(1);
                evt.consume();
                break;
            case KeyEvent.VK_SHIFT:
                evt.consume();
                break;
            default:
                if(evt.isActionKey()
                    || evt.isControlDown()
                    || evt.isAltDown()
                    || evt.isMetaDown())
                {
                    dispose();
                }
                break;
        }

        if (CompletionActions.isNumberSelectionEnabled && Character.isDigit(evt.getKeyChar())) {
            int index = Character.digit(evt.getKeyChar(), 10);
            if (index < validCandidates.size()) {
                validCandidates.get(index).complete(view);
                evt.consume();
                dispose();
            }
        }
    }

    protected void checkForValidCandidates ()
    {
        validCandidates.clear();
        for (CompletionCandidate c : candidates) {
            if (c.isValid(view)) {
                validCandidates.add(c);
            }
        }
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

    private static Point getLocation(JEditTextArea textArea, int caret, String prefix)
    {
        Point location = textArea.offsetToXY(caret - prefix.length());
        location.y += textArea.getPainter().getFontMetrics().getHeight();
        SwingUtilities.convertPointToScreen(location,
            textArea.getPainter());
        return location;
    }

    private class CandidateListModel extends AbstractListModel
    {
        @Override
        public int getSize()
        {
            return validCandidates.size();
        }

        @Override
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
            CompletionPopup.this.keyPressed(e);
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
        @Override
        public void windowGainedFocus(WindowEvent e)
        {
        }

        @Override
        public void windowLostFocus(WindowEvent e)
        {
            dispose();
        }
    }
}
