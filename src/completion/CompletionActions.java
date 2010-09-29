package completion;

import static completion.CompletionPlugin.trace;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;
import javax.swing.Timer;

import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;

import completion.popup.PopupWindow;
import completion.service.CompletionCandidate;
import completion.service.CompletionProvider;

public class CompletionActions
{
    public enum CompletionMode { COMPLETE_COMMAND, COMPLETE_DELAY_KEY, COMPLETE_INSTANT_KEY }


    public static String acceptChars;
    public static String insertChars;

    private static boolean automaticallyShowCompletionPopup;
    private static int automaticallyShowCompletionPopupDelay;
    private static Timer autoDelayTimer;
    private static boolean autoCompletePopupGetFocus;


    private static boolean completeDelay;
    private static boolean completeInstant;
    private static int delay;
    private static WeakReference<JEditTextArea> delayedCompletionTarget;
    private static int caretWhenCompleteKeyPressed;
//    private static SideKickCompletionPopup popup;
    public static PopupWindow popup;

    /**
     * Returns if completion popups should be shown after any period of
     * inactivity. Otherwise, they are only shown if explicitly requested
     * by the user.
     *
     * Returns true by default.
     */
    public boolean canCompleteAnywhere()
    {
        return true;
    }

    public static void completeFromAuto (View view)
    {
        if(!automaticallyShowCompletionPopup) {
            return;
        }

        if(autoDelayTimer != null) {
            autoDelayTimer.stop();
        }

        JEditTextArea textArea = view.getTextArea();
        if (delayedCompletionTarget == null || delayedCompletionTarget.get() != textArea)
        {
            delayedCompletionTarget = new WeakReference<JEditTextArea>(textArea);
        }
        caretWhenCompleteKeyPressed = textArea.getCaretPosition();

        if(autoDelayTimer == null)
        {
            autoDelayTimer = new Timer(0,new ActionListener()
            {
                public void actionPerformed(ActionEvent evt)
                {
                    JEditTextArea textArea = delayedCompletionTarget.get();
                    if(textArea != null
                        && caretWhenCompleteKeyPressed == textArea.getCaretPosition())
                    {
                        complete(textArea.getView(), CompletionMode.COMPLETE_DELAY_KEY);
                    }
                }
            });

            autoDelayTimer.setInitialDelay(automaticallyShowCompletionPopupDelay);
            autoDelayTimer.setRepeats(false);
        }

        autoDelayTimer.start();
    }

    public static void completeFromInstantKey (View view)
    {
        if(autoDelayTimer != null) {
            autoDelayTimer.stop();
        }

        if (!completeInstant) {
            return;
        }

        JEditTextArea textArea = view.getTextArea();
        if (delayedCompletionTarget == null || delayedCompletionTarget.get() != textArea)
        {
            delayedCompletionTarget = new WeakReference<JEditTextArea>(textArea);
        }
        caretWhenCompleteKeyPressed = textArea.getCaretPosition();

        if(autoDelayTimer == null)
        {
            autoDelayTimer = new Timer(0, new ActionListener()
            {
                public void actionPerformed(ActionEvent evt)
                {
                    JEditTextArea textArea = delayedCompletionTarget.get();
                    if(textArea != null
                        && caretWhenCompleteKeyPressed == textArea.getCaretPosition())
                    {
                        complete(textArea.getView(), CompletionMode.COMPLETE_INSTANT_KEY);
                    }
                }
            });

            autoDelayTimer.setInitialDelay(delay);
            autoDelayTimer.setRepeats(false);
        }

        autoDelayTimer.start();
    }

    public static void completeFromCommand (View view)
    {
        if(autoDelayTimer != null) {
            autoDelayTimer.stop();
        }

        complete(view, CompletionMode.COMPLETE_COMMAND);
    }

    public static void complete (View view, CompletionMode completionMode)
    {
//        EditPane editPane = view.getEditPane();
//        Buffer buffer = editPane.getBuffer();
//        JEditTextArea textArea = editPane.getTextArea();


        if (popup != null) {
            popup.dispose();
        }

        popup = new PopupWindow(view);//, completionMode, autoCompletePopupGetFocus || completionMode == CompletionMode.COMPLETE_COMMAND, completeInstant);
        popup.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                popup = null;
            }
        });

        trace("complete, CompletionPlugin.serviceNames=" + Arrays.toString(ServiceManager.getServiceNames(CompletionProvider.class)));

        //Some completion computations may take some time.  Run in a work thread and add when finished
        //so they don't block the whole completion process.
        Mode mode = view.getTextArea().getBuffer().getMode();
        for (String serviceName : ServiceManager.getServiceNames(CompletionProvider.class)) {
            final CompletionProvider provider = ServiceManager.getService(CompletionProvider.class, serviceName);
            Collection<Mode> modes = provider.restrictToModes();
            if (modes == null || modes.contains(mode)) {
                CompletionSwingWorker worker = new CompletionSwingWorker(provider, popup, view);
                worker.execute();
                popup.threadsRemaining++;
            }
        }

//        SideKickCompletion complete = null;

//        if(buffer.isEditable())
//        {
//            complete = parser.complete(editPane,
//                textArea.getCaretPosition());
//        }

//        if(complete == null || complete.size() == 0)
//        {
//            if(mode == COMPLETE_INSTANT_KEY
//                || mode == COMPLETE_DELAY_KEY)
//            {
//                // don't bother user with beep if eg
//                // they press < in XML mode
//                return;
//            }
//            else
//            {
//                view.getToolkit().beep();
//                return;
//            }
//        }
//        else if(complete.size() == 1)
//        {
//            // if user invokes complete explicitly, insert the
//            // completion immediately.
//            //
//            // if the user eg enters </ in XML mode, there will
//            // only be one completion and / is an instant complete
//            // key, so we insert it
//            if(mode == COMPLETE_COMMAND
//                || mode == COMPLETE_INSTANT_KEY)
//            {
//                complete.insert(0);
//                return;
//            }
//        }

        // show the popup if
        // - complete has one element and user invoked with delay key
        // - or complete has multiple elements
        // and popup is not already shown because of explicit invocation
        // of the complete action during the trigger delay
//        if(popup != null)
//            return;
//
//        boolean active = (mode == COMPLETE_COMMAND)
//            || autoCompletePopupGetFocus;
//        popup = parser.getCompletionPopup(view,
//            textArea.getCaretPosition(), complete, active);
//        popup.addWindowListener(new WindowAdapter() {
//            public void windowClosed(WindowEvent e) {
//                popup = null;
//            }
//        });
    }

    public static void insert (View view)
    {

    }

//    private static void complete (final View view, boolean fromKeyStroke)
//    {
//
//        if (popup != null) {
//            popup.dispose();
//        }
//
//        popup = new PopupWindow(view, fromKeyStroke);
//
//        trace("complete, CompletionPlugin.serviceNames=" + Arrays.toString(ServiceManager.getServiceNames(CompletionProvider.class)));
//
//        //Some completion computations may take some time.  Run in a work thread and add when finished
//        //so they don't block the whole completion process.
//        Mode mode = view.getTextArea().getBuffer().getMode();
//        for (String serviceName : ServiceManager.getServiceNames(CompletionProvider.class)) {
//            final CompletionProvider provider = ServiceManager.getService(CompletionProvider.class, serviceName);
//            Collection<Mode> modes = provider.getModes();
//            if (modes == null || modes.contains(mode)) {
//                CompletionSwingWorker worker = new CompletionSwingWorker(provider, popup, view);
//                worker.execute();
//                popup.threadsRemaining++;
//            }
//        }
//    }

    public static void propertiesChanged()
    {
        automaticallyShowCompletionPopup = jEdit.getBooleanProperty("completion.auto-complete.toggle");
        automaticallyShowCompletionPopupDelay = jEdit.getIntegerProperty("completion.auto-complete-delay", 500);
        completeDelay = jEdit.getBooleanProperty("completion.complete-delay.toggle");
        completeInstant = jEdit.getBooleanProperty("completion.complete-instant.toggle");
        autoCompletePopupGetFocus = jEdit.getBooleanProperty("completion.auto-complete-popup-get-focus");
        acceptChars = MiscUtilities.escapesToChars(jEdit.getProperty("completion.complete-popup.accept-characters"));
        insertChars = MiscUtilities.escapesToChars(jEdit.getProperty("completion.complete-popup.insert-characters"));
        delay = jEdit.getIntegerProperty("completion.complete-delay",500);
        if(autoDelayTimer != null)
            autoDelayTimer.setInitialDelay(delay);
    }

    private static class CompletionSwingWorker extends SwingWorker<List<CompletionCandidate>, Object>
    {
        private CompletionProvider provider;
        private PopupWindow popup;
        private View view;

        public CompletionSwingWorker(CompletionProvider provider, PopupWindow popup, View view)
        {
            this.provider = provider;
            this.popup = popup;
            this.view = view;
        }

        @Override
        protected List<CompletionCandidate> doInBackground ()
            throws Exception
        {
            // TODO Auto-generated method stub
            trace("doInBackground provider.getCompletionCandidates");

            try {
                List<CompletionCandidate> candidates = provider.getCompletionCandidates(view);
                return candidates;
            } catch (Exception e) {
                trace("Problem with getting candidates from provider=" + provider);
                trace(e);
                e.printStackTrace();
            }
            return new ArrayList<CompletionCandidate>();//provider.getCompletionCandidates(view);
        }

        @Override
        protected void done ()
        {
//            popup.threadsRemaining--;
            try {
                trace("CompletionPlugin.currentPopup == popup=" + (CompletionActions.popup== popup));
                trace("isDone=" + isDone());
                trace("isCancelled()=" + isCancelled());
                trace("state=" + getState());
                if (CompletionActions.popup == popup) {
                    trace("popup=" + popup);
                    trace("get()=" + get());
                    popup.addCompletions(get());
                }
            } catch (ExecutionException ignore) {
                trace(ignore.getMessage());
                ignore.printStackTrace();
            } catch (InterruptedException ignore) {
                trace(ignore);
            }



        }

    }

}
