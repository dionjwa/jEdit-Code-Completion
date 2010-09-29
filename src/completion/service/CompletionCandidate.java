package completion.service;

import javax.swing.ListCellRenderer;

import org.gjt.sp.jedit.View;

public interface CompletionCandidate
{
    /**
     * Returns whether this completion is still valid after the
     * characters have been typed.
     */
    public boolean isValid (String prefix);

    /**
     * Do the completion.
     */
    public void complete (String prefix, View view);

    /**
     * Returns a component to render a cell for the index
     * in the popup.
     */
    public ListCellRenderer getCellRenderer ();

    /**
     * Returns a description text shown when the index is
     * selected in the popup, or null if no description is
     * available.
     */
    public String getDescription ();
}
