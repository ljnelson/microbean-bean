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

public class Beans implements Selectable<BeanSelectionCriteria, Bean<?>>, Constable {

  private static final Logger LOGGER = System.getLogger(Beans.class.getName());

  private static final Comparator<Bean<?>> byRankComparator = Comparator
    .<Bean<?>>comparingInt(Ranked::rank)
    .reversed();

  private static final Comparator<Bean<?>> byAlternateThenByRankComparator = Comparator
    .<Bean<?>, Boolean>comparing(Ranked::alternate)
    .reversed()
    .thenComparing(byRankComparator);

  private final List<Bean<?>> beans;

  private final ConcurrentMap<BeanSelectionCriteria, List<Bean<?>>> selections;

  public Beans(final Collection<? extends Bean<?>> beans) {
    this(Map.of(), beans);
  }

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

  @Override // Beans (Selectable<BeanSelectionCriteria, Bean<?>>)
  public final List<Bean<?>> select(final BeanSelectionCriteria beanSelectionCriteria) {
    return
      beanSelectionCriteria == null ?
      this.list() :
      this.selections.computeIfAbsent(beanSelectionCriteria, this::computeSelection);
  }

  private final List<Bean<?>> computeSelection(final BeanSelectionCriteria bsc) {
    // No need to sort via ranks because the stream will be in encounter order and this.beans has already been properly sorted
    return this.stream().filter(bsc::selects).toList();
  }

  public final Stream<Bean<?>> stream() {
    return this.list().stream();
  }

  @Override // Beans (Filerable<BeanSelectionCriteria, Bean<?>>)
  public final List<Bean<?>> list() {
    return this.beans;
  }

  // Snapshot; slow
  public final List<Bean<?>> listUsed() {
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

  // Snapshot; slow
  public final List<Bean<?>> listUnused() {
    final Set<Bean<?>> used = newHashSet(this.selections.size());
    this.selections.values().forEach(c -> c.forEach(b -> used.add(b)));
    final List<Bean<?>> unused = this.stream().filter(not(used::contains)).toList();
    used.clear();
    return unused;
  }

  // Snapshot
  public final Set<BeanSelectionCriteria> selectCriteria() {
    return Collections.unmodifiableSet(this.selections.keySet());
  }

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

  @Override
  public String toString() {
    final StringJoiner sj = new StringJoiner(lineSeparator());
    this.list().forEach(b -> sj.add(b.toString()));
    return sj.toString();
  }

}
