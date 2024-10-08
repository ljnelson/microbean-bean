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

import java.lang.System.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import static java.util.HashSet.newHashSet;

/**
 * A {@link Selectable} and {@link Reducible} implementation that works with {@link Bean} and {@link
 * AttributedType} instances.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #Beans(Selectable, Reducible)
 *
 * @see Selectable
 *
 * @see Reducible
 *
 * @see Reducer
 *
 * @see RankedReducer
 *
 * @see Bean
 *
 * @see AttributedType
 */
public final class Beans implements Selectable<AttributedType, Bean<?>>, Reducible<AttributedType, Bean<?>> {


  /*
   * Static fields.
   */


  private static final Logger LOGGER = System.getLogger(Beans.class.getName());

  private static final Comparator<Bean<?>> byRankComparator = Comparator
    .<Bean<?>>comparingInt(Ranked::rank)
    .reversed();

  private static final Comparator<Bean<?>> byAlternateThenByRankComparator = Comparator
    .<Bean<?>, Boolean>comparing(Ranked::alternate)
    .reversed()
    .thenComparing(byRankComparator);


  /*
   * Instance fields.
   */


  private final Selectable<AttributedType, Bean<?>> s;

  private final Reducible<AttributedType, Bean<?>> r;


  /*
   * Constructors.
   */


  public Beans(final Selectable<AttributedType, Bean<?>> s,
               final Reducible<AttributedType, Bean<?>> r) {
    this.s = Objects.requireNonNull(s, "s");
    this.r = Objects.requireNonNull(r, "r");
  }


  /*
   * Instance methods.
   */


  @Override // Selectable<AttributedType, Bean<?>>
  public final List<Bean<?>> select(final AttributedType c) {
    return this.s.select(c);
  }

  @Override // Reducible<AttributedType, Bean<?>>
  public final Bean<?> reduce(final AttributedType c) {
    return this.r.reduce(c);
  }


  /*
   * Static methods.
   */


  public static final Selectable<AttributedType, Bean<?>> cachingSelectableOf(final Matcher<? super AttributedType, ? super Id> idMatcher,
                                                                              final Map<? extends AttributedType, ? extends List<Bean<?>>> selections,
                                                                              final Collection<? extends Bean<?>> beans) {
    Objects.requireNonNull(idMatcher, "idMatcher");
    final Map<AttributedType, List<Bean<?>>> selectionCache = new ConcurrentHashMap<>();
    final ArrayList<Bean<?>> newBeans = new ArrayList<>(31); // 31 == arbitrary
    final Set<Bean<?>> newBeansSet = newHashSet(31); // 31 == arbitrary
    for (final Entry<? extends AttributedType, ? extends List<Bean<?>>> e : selections.entrySet()) {
      final List<Bean<?>> selection = e.getValue();
      if (!selection.isEmpty()) {
        final Set<Bean<?>> newSelectionSet = newHashSet(7); // 7 == arbitrary
        final ArrayList<Bean<?>> newSelection = new ArrayList<>(selection.size());
        for (final Bean<?> b : selection) {
          if (newSelectionSet.add(b)) {
            newSelection.add(b);
          }
          if (newBeansSet.add(b)) {
            newBeans.add(b);
          }
        }
        newSelectionSet.clear();
        newSelection.trimToSize();
        Collections.sort(newSelection, byAlternateThenByRankComparator);
        selectionCache.put(e.getKey(), Collections.unmodifiableList(newSelection));
      }
    }
    for (final Bean<?> bean : beans) {
      if (newBeansSet.add(bean)) {
        newBeans.add(bean);
      }
    }
    newBeansSet.clear();
    if (newBeans.isEmpty()) {
      return Beans::empty;
    }
    Collections.sort(newBeans, byAlternateThenByRankComparator);
    newBeans.trimToSize();
    return attributedType -> selectionCache.computeIfAbsent(attributedType, at -> newBeans.stream().filter(b -> idMatcher.test(at, b.id())).toList());
  }

  private static final <C, T> List<T> empty(final C ignored) {
    return List.of();
  }

}
