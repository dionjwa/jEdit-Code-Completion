package completion.util;


import javax.swing.ListCellRenderer;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.TextArea;

import superabbrevs.SuperAbbrevs;

import completion.CompletionPlugin;
import completion.service.CompletionCandidate;

/**
 * Tokens suggested for code completion that are local variables or class members.
 */
public class CodeCompletionVariable implements CompletionCandidate
{
    protected String name;
	protected CodeCompletionType type;
	protected String className;
	protected ListCellRenderer renderer;

	public CodeCompletionVariable(CodeCompletionType type, String name, String className)
	{
		this.name = name;
		this.className = className;
		this.type = type;
        renderer = new CodeCellRenderer(type);
	}

	public int compareTo(CompletionCandidate o)
	{
		return getDescription().compareTo(o.getDescription());
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
        String sig = name;
        if (sig == null || sig.length() == 0)
            return;
        String abbrev = CompletionUtil.createAbbrev(sig);
        CompletionPlugin.trace("complete, abbrev=" + abbrev);
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
        return name + (className != null ? ":" + className : "");
    }

    @Override
    public boolean isValid (View view)
    {
        String prefix = CompletionUtil.getCompletionPrefix(view);
        if (prefix == null || prefix.length() == 0) {
            return true;
        }
        return name.toLowerCase().startsWith(prefix.toLowerCase());
    }

	public String getStringForInsertion()
	{
		return name;
	}

	@Override
	public String toString()
	{
		return getDescription();
	}
}
