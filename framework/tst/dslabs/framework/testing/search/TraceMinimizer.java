/*
 * Copyright (c) 2019 Ellis Michael (emichael@cs.washington.edu)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dslabs.framework.testing.search;

import dslabs.framework.testing.StatePredicate;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

abstract class TraceMinimizer {
    static SearchState minimizeInvariantViolatingTrace(SearchState state,
                                                       final StatePredicate predicate,
                                                       boolean expectedResult) {
        boolean shortenedEventsList;
        do {
            shortenedEventsList = false;
            LinkedList<Event> events = new LinkedList<>();
            for (SearchState s = state; s.previous() != null;
                 s = s.previous()) {
                SearchState test = applyEvents(s.previous(), events);
                if (test == null || predicate.test(test) != expectedResult) {
                    events.addFirst(s.previousEvent());
                } else {
                    shortenedEventsList = true;
                    state = test;
                }
            }
        } while (shortenedEventsList);
        return state;
    }

    /**
     * Returns a state that results in an exception of the same class as the
     * original one.
     *
     * @param state
     *         the state that throws an exception
     * @return another state throwing the same type of exception
     */
    static SearchState minimizeExceptionCausingTrace(SearchState state) {
        final Throwable exception = state.thrownException();
        assert exception != null;

        return minimizeInvariantViolatingTrace(state,
                StatePredicate.statePredicate(null, s -> {
                    if (!(s instanceof SearchState)) {
                        return false;
                    }

                    Throwable e = ((SearchState) s).thrownException();
                    if (e == null) {
                        return false;
                    }

                    return Objects.equals(e.getClass(), exception.getClass());
                }), true);
    }

    private static SearchState applyEvents(SearchState initialState,
                                           List<Event> events) {
        SearchState s = initialState;
        for (Event e : events) {
            // TODO: don't use null settings here, it re-initialized every time
            // TODO: do we need to use same settings as the search?
            SearchState next = s.stepEvent(e, null, false);
            if (next == null) {
                break;
            }
            s = next;
        }

        return s;
    }
}
