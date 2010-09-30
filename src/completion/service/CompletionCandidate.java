package completion.service;

import javax.swing.ListCellRenderer;

import org.gjt.sp.jedit.View;

public interface CompletionCandidate extends Comparable<CompletionCandidate>
{
    /**
     * Returns whether this completion is valid, usually based on the caret position.
     */
    public boolean isValid (View view);

    /**
     * Insert the completion.
     */
    public void complete (View view);

    /**
     * Returns a component to render a cell for the index
     * in the popup window.
     */
    public ListCellRenderer getCellRenderer ();

    /**
     * Returns a description text shown when the index is
     * selected in the popup, or null if no description is
     * available.
     */
    public String getDescription ();
}
