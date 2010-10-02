package completion.util;


import static completion.util.CompletionUtil.createAbbrev;

import java.util.LinkedList;
import java.util.List;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.TextArea;

import superabbrevs.SuperAbbrevs;

/**
 * Container describing a method from a
 * @author dion
 *
 */
public class CodeCompletionMethod extends CodeCompletionVariable
{
	public List<String> arguments;
	public List<String> argumentTypes;
	protected String returnType;

	public CodeCompletionMethod (String name, String returnType)
	{
	    super(CodeCompletionType.METHOD, name, returnType);
		arguments = new LinkedList<String>();
		argumentTypes = new LinkedList<String>();
		this.returnType = returnType ;
	}

	@Override
	public String getDescription ()
	{
		StringBuilder s = new StringBuilder(name + "(");
		for (int i = 0; i < arguments.size(); i++)
		{
			s.append((i==0?"":",") +arguments.get(i));
			if(i < argumentTypes.size() && argumentTypes.get(i).length() > 0) {
				s.append(":"+argumentTypes.get(i));
			}
		}
		if (returnType != null) {
		    s.append("): " + returnType);
		} else {
		    s.append(")");
		}
		return s.toString();
	}

	@Override
    public void complete (View view)
    {
        TextArea textArea = view.getTextArea();
        String prefix = CompletionUtil.getCompletionPrefix(view);
        int caret = textArea.getCaretPosition();
        JEditBuffer buffer = textArea.getBuffer();
        try
        {
            buffer.beginCompoundEdit();
            if (prefix.length() > 0) {
                buffer.remove(caret - prefix.length(), prefix.length());
            }
        }
        finally
        {
            buffer.endCompoundEdit();
        }

        //Check if a parametrized abbreviation is needed
        String sig = getStringForInsertion();//tag.getExtension("signature");
        if (sig == null || sig.length() == 0)
            return;
        String abbrev = createAbbrev(sig);
        SuperAbbrevs.expandAbbrev(view, abbrev, null);


    }

	@Override
	public String getStringForInsertion()
	{
		StringBuilder s = new StringBuilder(name+"(");
		for (int i = 0; i < arguments.size(); i++)
		{
			s.append((i==0?"":",") + arguments.get(i));
		}
		s.append(")");
		return s.toString();
	}

	@Override
    public String toString()
    {
        return getDescription();
    }
}
