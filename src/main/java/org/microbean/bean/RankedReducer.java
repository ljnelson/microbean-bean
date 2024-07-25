/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2024 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.microbean.bean;

import java.util.ArrayList;
import java.util.List;

import java.util.function.BiFunction;

import static java.util.Collections.unmodifiableList;

import static org.microbean.bean.Ranked.DEFAULT_RANK;

public class RankedReducer<C, T extends Ranked> implements Reducer<C, T> {

  public RankedReducer() {
    super();
  }

  @Override
  public T reduce(final List<? extends T> elements,
                  final C c,
                  final BiFunction<? super List<? extends T>, ? super C, ? extends T> failureHandler) {
    if (elements == null || elements.isEmpty()) {
      return null;
    } else if (elements.size() == 1) {
      return elements.get(0);
    }

    T candidate = null;
    List<T> unresolved = null;
    // Highest rank wins
    int maxRank = DEFAULT_RANK;

    for (final T element : elements) {
      if (element.alternate()) {
        final int elementRank = element.rank();
        if (elementRank == maxRank) {
          if (candidate == null || !candidate.alternate()) {
            // Prefer elements regardless of ranks.
            candidate = element;
          } else {
            assert candidate.rank() == maxRank : "Unexpected rank: " + candidate.rank() + "; was expecting: " + maxRank;
            // The existing candidate is an alternate and by definition has the highest rank we've seen so far; the
            // incoming element is also an alternate; both have equal ranks: we can't resolve this.
            if (unresolved == null) {
              unresolved = new ArrayList<>(6);
            }
            unresolved.add(candidate);
            unresolved.add(element);
            candidate = null;
          }
        } else if (elementRank > maxRank) {
          if (candidate == null || !candidate.alternate() || elementRank > candidate.rank()) {
            // The existing candidate is either null, not an alternate (and alternates are always preferred), or an
            // alternate with losing rank, so junk it in favor of the incoming element.
            candidate = element;
            // We have a new maxRank.
            maxRank = elementRank;
          } else if (elementRank == candidate.rank()) {
            // The existing candidate is also an alternate and has the same rank.
            if (unresolved == null) {
              unresolved = new ArrayList<>(6);
            }
            unresolved.add(candidate);
            unresolved.add(element);
            candidate = null;
          } else {
            assert elementRank < candidate.rank() : "elementRank >= candidate.rank(): " + elementRank + " >= " + candidate.rank();
            // The existing candidate is also an alternate but has a higher rank than the alternate, so keep it (do
            // nothing).
          }
        }
        // ...else drop element by doing nothing
      } else if (candidate == null) {
        // The incoming element is not an alternate, but that doesn't matter; the candidate is null, so accept the
        // element no matter what.
        candidate = element;
      } else if (!candidate.alternate()) {
        // The existing candidate is not an alternate. The incoming element is not an alternate. Ranks in this case are
        // irrelevant, perhaps surprisingly. We cannot resolve this.
        if (unresolved == null) {
          unresolved = new ArrayList<>(6);
        }
        unresolved.add(candidate);
        unresolved.add(element);
        candidate = null;
      }
      // ...else do nothing
    }

    if (unresolved != null && !unresolved.isEmpty()) {
      if (candidate != null) {
        unresolved.add(candidate);
      }
      candidate =
        failureHandler == null ? Reducer.fail(unmodifiableList(unresolved), c) : failureHandler.apply(unmodifiableList(unresolved), c);
    }

    return candidate;
  }

}
