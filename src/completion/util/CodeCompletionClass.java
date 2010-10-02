package completion.util;

import org.gjt.sp.jedit.View;

public class CodeCompletionClass extends CodeCompletionVariable
{
    public CodeCompletionClass(String name)
    {
        super(CodeCompletionType.CLASS, name, null);
    }

    @Override
    public String getDescription ()
    {
        return name;
    }

    @Override
    public String getStringForInsertion()
    {
        return name.split(" ")[0];
    }

    @Override
    public boolean isValid (View view)
    {
        if (super.isValid(view)) {
            return true;
        }


        String prefix = CompletionUtil.getCompletionPrefix(view);
        String insertion = getStringForInsertion();
        String capitals = "";
        for (int ii = 0; ii < insertion.length(); ++ii) {
            if (Character.isUpperCase(insertion.charAt(ii))) {
                capitals += insertion.charAt(ii);
            }
        }

        return capitals.toLowerCase().startsWith(prefix.toLowerCase());
    }
}
