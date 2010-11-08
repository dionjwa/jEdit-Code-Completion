package completion.util;

import javax.swing.ListCellRenderer;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.TextArea;

import superabbrevs.SuperAbbrevs;

import completion.service.CompletionCandidate;

public class BaseCompletionCandidate
    implements CompletionCandidate
{

    protected String description;
    protected ListCellRenderer renderer;

    public BaseCompletionCandidate (String description)
    {
        super();
        this.description = description;
        renderer = new BaseCompletionRenderer();
    }

    @Override
    public int compareTo (CompletionCandidate o)
    {
        return getDescription().compareTo(o.getDescription());
    }

    @Override
    public boolean isValid (View view)
    {
        String prefix = CompletionUtil.getCompletionPrefix(view);
        if (prefix == null || prefix.length() == 0) {
            return true;
        }
        //Case is ignored for determining validity
        return getDescription().toLowerCase().startsWith(prefix.toLowerCase());
    }

    @Override
    public void complete (View view)
    {
        TextArea textArea = view.getTextArea();
        String prefix = CompletionUtil.getCompletionPrefix(view);
        int caret = textArea.getCaretPosition();
        JEditBuffer buffer = textArea.getBuffer();
        if (prefix.length() > 0) {
            buffer.remove(caret - prefix.length(), prefix.length());
        }

        // Check if a parametrized abbreviation is needed
        String sig = getDescription();
        if (sig == null || sig.length() == 0)
            return;
        String abbrev = CompletionUtil.createAbbrev(sig);
        SuperAbbrevs.expandAbbrev(view, abbrev, null);
    }

    @Override
    public ListCellRenderer getCellRenderer ()
    {
        return renderer;
    }

    @Override
    public String getDescription ()
    {
        return description;
    }

}
