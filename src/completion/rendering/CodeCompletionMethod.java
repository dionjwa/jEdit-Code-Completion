package completion.rendering;


import java.util.LinkedList;
import java.util.List;

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
		s.append("): " + returnType);
		return s.toString();
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
