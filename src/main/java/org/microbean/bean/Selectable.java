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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;

import java.util.function.BiFunction;

public interface Selectable<C, T> {

  // Filters this thing according to the supplied criteria, producing a List.
  // List not Stream to permit caching
  // List not Collection so equals() is well-defined
  // List is unmodifiable and is always valid for the supplied criteria (unenforceable)
  // C and not Predicate because equality semantics for Predicate are not well-defined (caching again)
  public List<T> select(final C criteria);

  public default List<T> list() {
    return this.select(null);
  }

  /*
   * Static methods.
   */


  @SuppressWarnings("unchecked")
  public static <C, E> Selectable<C, E> of(final Collection<? extends E> collection, final BiFunction<? super E, ? super C, ? extends Boolean> f) {
    Objects.requireNonNull(f, "f");
    return collection.isEmpty() ? c -> List.of() : c -> (List<E>)collection.stream()
      .filter(e -> f.apply(e, c))
      .toList();
  }

  @SuppressWarnings("unchecked")
  public static <C, E> Selectable<C, E> ofCaching(final Collection<? extends E> collection, final BiFunction<? super E, ? super C, ? extends Boolean> f) {
    Objects.requireNonNull(f, "f");
    if (collection.isEmpty()) {
      return c -> List.of();
    }
    final Map<C, List<? extends E>> m = new ConcurrentHashMap<>();
    return c ->
      (List<E>)m.computeIfAbsent(c, fc -> collection.stream()
                                 .filter(e -> f.apply(e, fc))
                                 .toList());
  }

}
