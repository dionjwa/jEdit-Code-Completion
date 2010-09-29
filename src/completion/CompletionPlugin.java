package completion;

import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;


public class CompletionPlugin extends EditPlugin
{
    public static final String NAME = "completion";
    public static final String OPTION_PREFIX = "options.completion.";

    @Override
    public void start()
    {
        View view = jEdit.getFirstView();
        while(view != null)
        {
            EditPane[] panes = view.getEditPanes();
            for (EditPane pane : panes)
                initTextArea(pane.getTextArea());
            view = view.getNext();
        }
        CompletionActions.propertiesChanged();
        EditBus.addToBus(this);
    }

    @Override
    public void stop()
    {
        EditBus.removeFromBus(this);
        View view = jEdit.getFirstView();
        while(view != null)
        {
            EditPane[] panes = view.getEditPanes();
            for (EditPane pane : panes)
                uninitTextArea(pane.getTextArea());
            view = view.getNext();
        }
    }

    @EBHandler
    public void handleEditPaneUpdate(EditPaneUpdate epu)
    {
        EditPane editPane = epu.getEditPane();

        if(epu.getWhat() == EditPaneUpdate.CREATED)
            initTextArea(editPane.getTextArea());
        else if(epu.getWhat() == EditPaneUpdate.DESTROYED)
            uninitTextArea(editPane.getTextArea());
    }

    @EBHandler
    public void handlePropertiesChanged(PropertiesChanged msg)
    {
        CompletionActions.propertiesChanged();
    }

    private static void initTextArea(JEditTextArea textArea)
    {
        CompletionBindings b = new CompletionBindings();
        textArea.putClientProperty(CompletionBindings.class,b);
        textArea.addKeyListener(b);
    }

    private static void uninitTextArea(JEditTextArea textArea)
    {
        CompletionBindings b = (CompletionBindings)
            textArea.getClientProperty(
            CompletionBindings.class);
        textArea.putClientProperty(CompletionBindings.class,null);
        textArea.removeKeyListener(b);
    }

    //For  debugging
    public static void trace (Object... arguments)
    {
        StringBuffer sb = new StringBuffer();
        for (Object s : arguments) {
            sb.append(s.toString() + " ");
        }
        Log.log(Log.NOTICE, "HaXe", sb.toString());
    }
}
