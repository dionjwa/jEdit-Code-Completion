package completion;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.KeyEventWorkaround;


/**
 * Manages our key bindings.
 */
class CompletionBindings extends KeyAdapter
{
    @Override
    public void keyTyped(KeyEvent evt)
    {
        evt = KeyEventWorkaround.processKeyEvent(evt);
        if(evt == null) {
            return;
        }

        char ch = evt.getKeyChar();
        if(ch == '\b')
            return;

        View view = GUIUtilities.getView((Component)evt.getSource());
        if (CompletionActions.acceptChars.indexOf(ch) > -1) {
            CompletionActions.completeFromInstantKey(view);
        } else {
            CompletionActions.completeFromAuto(view);
        }
    }
}
