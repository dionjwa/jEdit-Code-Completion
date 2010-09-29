package completion.service;

import java.util.Collection;
import java.util.List;

import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.View;


public interface CompletionProvider
{
//    /**
//     * Sometimes computing completions can take too long so these completions should not
//     * automatically occur without the completion keystroke.  E.g. the HaXe completions, which
//     * call an external compiler that, though quick, should not be called every delay.
//     * @return
//     */
//    public boolean isNeedingCompletionKeystroke();

    public List<CompletionCandidate> getCompletionCandidates(View view);

    /**
     * @return A list of supported modes (usually only one if any), or null if not mode specific.
     */
    public Collection<Mode> restrictToModes();
}
