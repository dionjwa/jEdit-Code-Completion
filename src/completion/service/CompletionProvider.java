package completion.service;

import java.util.List;
import java.util.Set;

import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.View;

/**
 * The service interface for plugins providing code completion.
 */
public interface CompletionProvider
{
    /**
     * @param view
     * @return The list of possible completions based on the current caret location.
     */
    public List<CompletionCandidate> getCompletionCandidates(View view);

    /**
     * @return A list of supported modes (usually only one if any), or null if not mode specific.
     */
    public Set<Mode> restrictToModes();
}
