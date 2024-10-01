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
import java.util.Arrays;
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
 * BeanSelectionCriteria} instances.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see Beans(Map, Collection, Reducer)
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
 * @see BeanSelectionCriteria
 */
public class Beans implements Selectable<BeanSelectionCriteria, Bean<?>>, Reducible<BeanSelectionCriteria, Bean<?>> {


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


  private final Selectable<BeanSelectionCriteria, Bean<?>> s;

  private final Reducible<BeanSelectionCriteria, Bean<?>> r;


  /*
   * Constructors.
   */


  // Pathological; useful mainly for testing
  public Beans(final Bean<?>... beans) {
    this(beans == null ? List.of() : Arrays.asList(beans));
  }

  public Beans(final Collection<? extends Bean<?>> beans) {
    this(null, beans, null);
  }

  public Beans(final Collection<? extends Bean<?>> beans,
               final Reducer<BeanSelectionCriteria, Bean<?>> r) {
    this(null, beans, r);
  }

  public Beans(final Map<? extends BeanSelectionCriteria, ? extends List<Bean<?>>> selections,
               final Collection<? extends Bean<?>> beans) {
    this(selections, beans, null);
  }

  public Beans(final Map<? extends BeanSelectionCriteria, ? extends List<Bean<?>>> selections,
               final Collection<? extends Bean<?>> beans,
               final Reducer<BeanSelectionCriteria, Bean<?>> r) {
    this(cachingSelectableOf(selections == null || selections.isEmpty() ? Map.of() : selections,
                             beans == null || beans.isEmpty() ? List.of() : beans),
         r);
  }


  public Beans(final Selectable<BeanSelectionCriteria, Bean<?>> s) {
    this(s, (Reducer<BeanSelectionCriteria, Bean<?>>)null);
  }

  public Beans(final Selectable<BeanSelectionCriteria, Bean<?>> s,
               final Reducer<BeanSelectionCriteria, Bean<?>> r) {
    this(s, Reducible.ofCaching(s, r == null ? RankedReducer.of() : r));
  }

  public Beans(final Selectable<BeanSelectionCriteria, Bean<?>> s,
               final Reducible<BeanSelectionCriteria, Bean<?>> r) {
    this.s = Objects.requireNonNull(s, "s");
    this.r = Objects.requireNonNull(r, "r");
  }


  /*
   * Instance methods.
   */


  @Override // Selectable<BeanSelectionCriteria, Bean<?>>
  public final List<Bean<?>> select(final BeanSelectionCriteria c) {
    return this.s.select(c);
  }

  @Override // Reducible<BeanSelectionCriteria, Bean<?>>
  public final Bean<?> reduce(final BeanSelectionCriteria c) {
    return this.r.reduce(c);
  }


  /*
   * Static methods.
   */


  public static final Selectable<BeanSelectionCriteria, Bean<?>> cachingSelectableOf(final Collection<? extends Bean<?>> beans) {
    return cachingSelectableOf(Map.of(), beans);
  }

  public static final Selectable<BeanSelectionCriteria, Bean<?>> cachingSelectableOf(final Map<? extends BeanSelectionCriteria, ? extends List<Bean<?>>> selections,
                                                                                     final Collection<? extends Bean<?>> beans) {
    final Map<BeanSelectionCriteria, List<Bean<?>>> selectionCache = new ConcurrentHashMap<>();
    final ArrayList<Bean<?>> newBeans = new ArrayList<>(31); // 31 == arbitrary
    final Set<Bean<?>> newBeansSet = newHashSet(31); // 31 == arbitrary
    if (!selections.isEmpty()) {
      final Set<Bean<?>> newSelectionSet = newHashSet(7); // 7 == arbitrary
      for (final Entry<? extends BeanSelectionCriteria, ? extends List<Bean<?>>> e : selections.entrySet()) {
        final List<Bean<?>> selection = e.getValue();
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
        if (!newSelection.isEmpty()) {
          newSelection.trimToSize();
          Collections.sort(newSelection, byAlternateThenByRankComparator);
          selectionCache.put(e.getKey(), Collections.unmodifiableList(newSelection));
        }
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
    return c -> selectionCache.computeIfAbsent(c, bsc -> newBeans.stream().filter(bsc::selects).toList());
  }

  private static final <C, T> List<T> empty(final C ignored) {
    return List.of();
  }

}
