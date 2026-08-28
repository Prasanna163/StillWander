package com.cinecraft.director;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;

/** Bounded, session-local memory of recently accepted shots. */
public final class ContinuityState {
    public static final int HISTORY_LIMIT = 8;

    private final ArrayDeque<ContinuityFrame> recent = new ArrayDeque<>();

    public void remember(ContinuityFrame frame) {
        recent.addFirst(frame);
        while (recent.size() > HISTORY_LIMIT) recent.removeLast();
    }

    public Optional<ContinuityFrame> last() {
        return Optional.ofNullable(recent.peekFirst());
    }

    /** Newest shot first. */
    public List<ContinuityFrame> recent() {
        return List.copyOf(recent);
    }

    public int consecutiveSubject(String subjectKey) {
        int count = 0;
        for (ContinuityFrame frame : recent) {
            if (!frame.subjectKey().equals(subjectKey)) break;
            count++;
        }
        return count;
    }

    public int consecutiveSubjectType(SubjectType subjectType) {
        int count = 0;
        for (ContinuityFrame frame : recent) {
            if (frame.subjectType() != subjectType) break;
            count++;
        }
        return count;
    }

    public void reset() {
        recent.clear();
    }
}
