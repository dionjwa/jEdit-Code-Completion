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
       String token = textArea.getText(caret, 1);
       while (caret > -1 && Character.isJavaIdentifierPart(token.charAt(0))) {
           prefix = token + prefix;
           caret--;
           if (caret < 0) {
               break;
           }
           token = textArea.getText(caret, 1);
       }
       return prefix;
   }

   public static String getWordAtCaret (View view)
   {
       TextArea textArea = view.getTextArea();
       String prefix = "";
       int caret = textArea.getCaretPosition() - 1;
       String token = textArea.getText(caret, 1);
       while (caret > -1 && Character.isJavaIdentifierPart(token.charAt(0))) {
           prefix = token + prefix;
           caret--;
           if (caret < 0) {
               break;
           }
           token = textArea.getText(caret, 1);
       }
       caret = textArea.getCaretPosition();
       int lineEnd = textArea.getLineEndOffset(textArea.getLineOfOffset(caret));
       token = textArea.getText(caret, 1);
       while (caret < lineEnd && Character.isJavaIdentifierPart(token.charAt(0))) {
           prefix += token;
           caret++;
           if (caret >= lineEnd) {
               break;
           }
           token = textArea.getText(caret, 1);
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
