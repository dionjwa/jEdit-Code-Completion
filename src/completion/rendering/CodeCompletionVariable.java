package completion.rendering;


import javax.swing.ListCellRenderer;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.TextArea;

import completion.CompletionActions;
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
	    String prefix = CompletionActions.getCompletionPrefix(view, ".");
        int caret = textArea.getCaretPosition();
        JEditBuffer buffer = textArea.getBuffer();
        try
        {
            buffer.beginCompoundEdit();
            if (prefix.length() > 0) {
                buffer.remove(caret - prefix.length(), prefix.length());
            }
            buffer.insert(caret - prefix.length(), getStringForInsertion());
        }
        finally
        {
            buffer.endCompoundEdit();
        }
    }

    @Override
    public ListCellRenderer getCellRenderer ()
    {
        return renderer;
    }

    @Override
    public String getDescription ()
    {
        return name + ":" + className;
    }

    @Override
    public boolean isValid (View view)
    {
        String prefix = CompletionActions.getCompletionPrefix(view, ".");
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
