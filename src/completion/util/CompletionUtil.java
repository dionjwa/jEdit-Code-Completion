package completion.util;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.TextArea;

import completion.CompletionActions;

public class CompletionUtil
{
    /**
    *
    * @param view
    * @param startChars A list of characters that define the beginning marker of a completion
    * prefix.  E.g.
    * <br>
    * someVar.abs*
    * <br>
    * The caret is at the position of the * character.  With startChars=".", the prefix will be
    * "abs".
    * @return The String prefix or "" if none.
    */
   public static String getCompletionPrefix (View view)
   {
       TextArea textArea = view.getTextArea();
       String prefix = "";
       int caret = textArea.getCaretPosition() - 1;
//       trace("General prefix area=" + textArea.getText(caret, 4));
       String token = textArea.getText(caret, 1);
//       trace("is " + token + " identifier?" + Character.isJavaIdentifierPart(token.charAt(0)));
       while (caret > -1 && Character.isJavaIdentifierPart(token.charAt(0))) {
           prefix = token + prefix;
           caret--;
           token = textArea.getText(caret, 1);
//           trace("is " + textArea.getText(caret, 1).charAt(0) + " identifier?" + Character.isJavaIdentifierPart(textArea.getText(caret, 1).charAt(0)));
       }
       return prefix;
   }

    /**
     * Creates a SuperAbbrev compatible abbreviation from a String
     * @param signature
     * @return
     */
    public static String createAbbrev(String signature)
    {
        StringBuffer sb = new StringBuffer();
        boolean startParam = false;
        for (int i = 0; i < signature.length(); i++)
        {
            char c = signature.charAt(i);
            switch (c) {
            case ',':
                sb.append("}");
                // fall through
            case '(':
                startParam = true;
                sb.append(c);
                break;
            case ')':
                if (! startParam) // Case of 'f()' - no parameters
                    sb.append("}");
                sb.append(c);
                break;
            case ' ':
            case '\t':
            case '\n':
                sb.append(c);
                break;
            default:
                if (startParam) {
                    startParam = false;
                    sb.append("${");
                    sb.append(i + 1);
                    sb.append(":");
                }
                sb.append(c);
                break;
            }
        }
        return sb.toString();
    }

    public static String prefixByIndex (String s, int index)
    {
        return (CompletionActions.isNumberSelectionEnabled && index <= 9) ? index + ": " + s : s;
    }
}
