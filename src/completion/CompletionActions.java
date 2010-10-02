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
import completion.popup.CompletionPopup;
import completion.service.CompletionCandidate;
import completion.service.CompletionProvider;

public class CompletionActions
{
    public enum CompletionMode { COMPLETE_COMMAND, COMPLETE_DELAY_KEY, COMPLETE_INSTANT_KEY }

    public static String acceptChars;
    public static String insertChars;

    private static boolean isPopupShowingAutomatically;
    private static int automaticallyShowCompletionPopupDelay;
    private static Timer autoDelayTimer;
    private static boolean isAutoCompletePopupGetFocus;

    public static boolean isNumberSelectionEnabled;
    private static boolean isTriggerKeysEnabled;
    public static boolean isCompleteInstant;
    private static int delay;
    private static WeakReference<JEditTextArea> delayedCompletionTarget;
    private static int caretWhenCompleteKeyPressed;
    public static CompletionPopup popup;

    public static void completeFromAuto (View view)
    {
        if(!isPopupShowingAutomatically) {
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
                @Override
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

        if (!isTriggerKeysEnabled) {
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
                @Override
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
        if (popup != null) {
            popup.dispose();
        }

        if(!view.getTextArea().getBuffer().isEditable()) {
            return;
        }

        popup = new CompletionPopup(view);//, completionMode, isAutoCompletePopupGetFocus || completionMode == CompletionMode.COMPLETE_COMMAND, isCompleteInstant);
        popup.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                popup = null;
            }
        });

        //Some completion computations may take some time.  Run in a work thread and add when finished
        //so they don't block the whole completion process.
        Mode mode = view.getTextArea().getBuffer().getMode();
        trace("Services=" + Arrays.toString(ServiceManager.getServiceNames(CompletionProvider.class)));
        for (String serviceName : ServiceManager.getServiceNames(CompletionProvider.class)) {
            final CompletionProvider provider = ServiceManager.getService(CompletionProvider.class, serviceName);
            Collection<Mode> modes = provider.restrictToModes();
            if (modes == null || modes.contains(mode)) {
                CompletionSwingWorker worker = new CompletionSwingWorker(provider, popup, view);
                popup.threadsRemaining++;
                worker.execute();
            }
        }

        if (completionMode == CompletionMode.COMPLETE_DELAY_KEY && isAutoCompletePopupGetFocus) {
            popup.reset(true);
        }
    }

    public static void propertiesChanged()
    {
        isPopupShowingAutomatically = jEdit.getBooleanProperty("completion.auto-complete.toggle");
        isAutoCompletePopupGetFocus = jEdit.getBooleanProperty("completion.auto-complete-popup-get-focus");
        automaticallyShowCompletionPopupDelay = jEdit.getIntegerProperty("completion.auto-complete-delay", 500);

        isCompleteInstant = jEdit.getBooleanProperty("completion.complete-instant.toggle");
        isTriggerKeysEnabled = jEdit.getBooleanProperty("completion.complete-delay.toggle");
        acceptChars = MiscUtilities.escapesToChars(jEdit.getProperty("completion.complete-popup.accept-characters"));
        insertChars = MiscUtilities.escapesToChars(jEdit.getProperty("completion.complete-popup.insert-characters"));
        delay = jEdit.getIntegerProperty("completion.complete-delay",500);
        if(autoDelayTimer != null)
            autoDelayTimer.setInitialDelay(delay);
        isNumberSelectionEnabled = jEdit.getBooleanProperty("options.completion.select-by-numbers.toggle");
    }

    private static class CompletionSwingWorker extends SwingWorker<List<CompletionCandidate>, Object>
    {
        private CompletionProvider provider;
        private CompletionPopup popup;
        private View view;

        public CompletionSwingWorker(CompletionProvider provider, CompletionPopup popup, View view)
        {
            this.provider = provider;
            this.popup = popup;
            this.view = view;
        }

        @Override
        protected List<CompletionCandidate> doInBackground ()
            throws Exception
        {
            try {
                List<CompletionCandidate> candidates = provider.getCompletionCandidates(view);
                return candidates;
            } catch (Exception e) {
                trace("Problem with getting candidates from provider=" + provider);
                trace(e);
                e.printStackTrace();
            }
            return new ArrayList<CompletionCandidate>();
        }

        @Override
        protected void done ()
        {
            try {
                if (CompletionActions.popup == popup) {
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
