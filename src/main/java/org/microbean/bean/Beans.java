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

import java.lang.constant.Constable;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.stream.Collector;
import java.util.stream.Stream;

import org.microbean.constant.Constables;

import static java.lang.System.lineSeparator;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_Collection;
import static java.lang.constant.ConstantDescs.CD_Map;

import static java.util.HashSet.newHashSet;

import static java.util.function.Predicate.not;

import static java.util.stream.Collector.Characteristics.UNORDERED;

import static org.microbean.bean.ConstantDescs.CD_Beans;

/**
 * A {@link Selectable} implementation that {@linkplain #select(BeanSelectionCriteria) selects} {@link Bean}s using
 * {@link BeanSelectionCriteria} instances.
 *
 * <p>Elements overall in any {@link List} returned by methods in this class will be sorted by their {@linkplain
 * Ranked#alternate() alternate status} and then by their {@linkplain Ranked#rank() rank}.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see Selectable
 *
 * @see #select(BeanSelectionCriteria)
 *
 * @see ReducibleBeans
 *
 * @deprecated See {@link ReducibleBeans} instead.
 */
@Deprecated // See ReducibleBeans
public class Beans implements Selectable<BeanSelectionCriteria, Bean<?>>, Constable {


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


  private final List<Bean<?>> beans;

  private final ConcurrentMap<BeanSelectionCriteria, List<Bean<?>>> selections;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link Beans}.
   *
   * @param beans a {@link Collection} of {@link Bean}s from which sublists will be {@linkplain
   * #select(BeanSelectionCriteria) selected}; must not be {@code null}
   *
   * @exception NullPointerException if {@code beans} is {@code null}
   */
  public Beans(final Collection<? extends Bean<?>> beans) {
    this(Map.of(), beans);
  }

  /**
   * Creates a new {@link Beans}.
   *
   * <p>Duplicate elements in the union of the supplied beans and the selections will be ignored.</p>
   *
   * @param selections a precomputed cache of selections; must not be {@code null}
   *
   * @param beans a {@link Collection} of {@link Bean}s from which sublists will be {@linkplain
   * #select(BeanSelectionCriteria) selected}; must not be {@code null}
   *
   * @exception NullPointerException if either {@code selections} or {@code beans} is {@code null}
   */
  public Beans(final Map<? extends BeanSelectionCriteria, ? extends List<Bean<?>>> selections,
               final Collection<? extends Bean<?>> beans) {
    super();
    this.selections = new ConcurrentHashMap<>();
    final ArrayList<Bean<?>> newBeans = new ArrayList<>(31); // 31 == arbitrary
    final Set<Bean<?>> newBeansSet = newHashSet(31); // 31 == arbitrary
    if (!selections.isEmpty()) {
      final Set<Bean<?>> newSelectionSet = newHashSet(7); // 7 == arbitrary
      for (final Entry<? extends BeanSelectionCriteria, ? extends List<Bean<?>>> e : selections.entrySet()) {
        final List<Bean<?>> selection = e.getValue();
        final List<Bean<?>> newSelection = new ArrayList<>(selection.size());
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
          Collections.sort(newSelection, byAlternateThenByRankComparator);
          this.selections.put(e.getKey(), Collections.unmodifiableList(newSelection));
        }
      }
    }
    for (final Bean<?> bean : beans) {
      if (newBeansSet.add(bean)) {
        newBeans.add(bean);
      }
    }
    newBeansSet.clear();
    Collections.sort(newBeans, byAlternateThenByRankComparator);
    newBeans.trimToSize();
    this.beans = Collections.unmodifiableList(newBeans);
  }


  /*
   * Instance methods.
   */


  @Override // Constable
  public Optional<DynamicConstantDesc<Beans>> describeConstable() {
    return Constables.describeConstable(this.selections)
      .flatMap(selectionsDesc -> Constables.describeConstable(this.list())
               .map(beansDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                        MethodHandleDesc.ofConstructor(CD_Beans,
                                                                                       CD_Map,
                                                                                       CD_Collection),
                                                        selectionsDesc,
                                                        beansDesc)));
  }

  @Override // Selectable<BeanSelectionCriteria, Bean<?>>
  public final List<Bean<?>> select(final BeanSelectionCriteria beanSelectionCriteria) {
    return
      beanSelectionCriteria == null ?
      this.list() :
      this.selections.computeIfAbsent(beanSelectionCriteria, this::computeSelection);
  }

  private final List<Bean<?>> computeSelection(final BeanSelectionCriteria bsc) {
    // No need to sort via ranks because the stream will be in encounter order and this.beans has already been properly
    // sorted
    return this.stream().filter(bsc::selects).toList();
  }

  /**
   * Returns a {@link Stream} of this {@link Beans}' elements.
   *
   * <p>This method is idempotent and produces a determinate value.</p>
   *
   * @return a {@link Stream} of this {@link Beans}' elements; never {@code null}
   */
  public final Stream<Bean<?>> stream() {
    return this.list().stream();
  }

  /**
   * Returns an immutable {@link List} of this {@link Beans}' elements.
   *
   * <p>This method is idempotent and produces a determinate value.</p>
   *
   * @return an immutable {@link List} of this {@link Beans}' elements; never {@code null}
   */
  @Override // Selectable<BeanSelectionCriteria, Bean<?>>
  public final List<Bean<?>> list() {
    return this.beans;
  }

  /**
   * A reporting-oriented method that returns an immutable {@link List} of all {@link Bean}s that have been {@linkplain
   * #select(BeanSelectionCriteria) selected} so far.
   *
   * <p>This method is idempotent but does not return a determinate value.</p>
   *
   * @return an immutable {@link List} of all {@link Bean}s that have been {@linkplain #select(BeanSelectionCriteria)
   * selected} so far
   */
  // Snapshot; slow
  public final List<Bean<?>> selected() {
    final ArrayList<Bean<?>> used = new ArrayList<>(this.selections.size()); // estimated size
    final Set<Bean<?>> set = newHashSet(this.selections.size());
    this.selections.values().forEach(c -> c.forEach(b -> {
          if (set.add(b)) {
            used.add(b);
          }
        }));
    set.clear();
    used.trimToSize();
    Collections.sort(used, byAlternateThenByRankComparator);
    return Collections.unmodifiableList(used);
  }

  /**
   * A reporting-oriented method that returns an immutable {@link List} of all {@link Bean}s that have not been
   * {@linkplain #select(BeanSelectionCriteria) selected} by any {@link BeanSelectionCriteria} so far.
   *
   * <p>This method is idempotent but does not return a determinate value.</p>
   *
   * @return an immutable {@link List} of all {@link Bean}s that have not been {@linkplain
   * #select(BeanSelectionCriteria) selected} by any {@link BeanSelectionCriteria} so far; never {@code null}
   */
  // Snapshot; slow
  public final List<Bean<?>> unselected() {
    final Set<Bean<?>> used = newHashSet(this.selections.size());
    this.selections.values().forEach(c -> c.forEach(b -> used.add(b)));
    final List<Bean<?>> unused = this.stream().filter(not(used::contains)).toList();
    used.clear();
    return unused;
  }

  /**
   * A reporting-oriented method that returns an immutable {@link Set} of all known {@link BeanSelectionCriteria}
   * instances that have been used so far to {@linkplain #select(BeanSelectionCriteria) select} beans.
   *
   * <p>Two invocations of this method may return differently-ordered {@link Set}s.</p>
   *
   * <p>This method is idempotent but does not return a determinate value.</p>
   *
   * @return an immutable {@link Set} of all known {@link BeanSelectionCriteria} instances that have been used so far to
   * {@linkplain #select(BeanSelectionCriteria) select} beans; never {@code null}
   */
  // Snapshot
  public final Set<BeanSelectionCriteria> criteria() {
    return Collections.unmodifiableSet(this.selections.keySet());
  }

  /**
   * A reporting-oriented method that returns an immutable {@link Set} of all known {@link BeanSelection}s that have
   * been made so far.
   *
   * <p>Two invocations of this method may return differently-ordered {@link Set}s.</p>
   *
   * <p>This method is idempotent but does not return a determinate value.</p>
   *
   * @return an immutable {@link Set} of all known {@link BeanSelection}s that have been made so far; never {@code null}
   */
  // Snapshot
  public final Set<BeanSelection> selections() {
    return this.selections.entrySet().stream()
      .map(e -> new BeanSelection(e.getKey(), e.getValue()))
      .<Set<BeanSelection>, HashSet<BeanSelection>>collect(Collector.of(HashSet::new,
                                                                        HashSet::add,
                                                                        (set0, set1) -> {
                                                                          set0.addAll(set1);
                                                                          return set0;
                                                                        },
                                                                        Collections::unmodifiableSet,
                                                                        UNORDERED));
  }

  /**
   * Returns a {@link String} representation of this {@link Beans}.
   *
   * <p>This method is idempotent and returns a determinate value.</p>
   *
   * @return a {@link String} representation of this {@link Beans}; never {@code null}
   */
  @Override
  public String toString() {
    final StringJoiner sj = new StringJoiner(lineSeparator());
    this.list().forEach(b -> sj.add(b.toString()));
    return sj.toString();
  }

}
