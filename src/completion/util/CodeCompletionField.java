package completion.util;

public class CodeCompletionField extends CodeCompletionVariable
{
	public CodeCompletionField(String name, String className)
	{
	    super(CodeCompletionType.FIELD, name, className);
	}

	@Override
	public String toString()
	{
		return name + " : " + className;
	}
}
