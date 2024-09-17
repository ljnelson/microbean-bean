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

/**
 * A notional list of elements from which sublists may be <em>selected</em> according to some <em>criteria</em>.
 *
 * @param <C> the type of criteria
 *
 * @param <T> the type of the elements
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public interface Selectable<C, T> {

  /**
   * <em>Selects</em> and returns an immutable {@link List} representing a sublist of this {@link Selectable}'s
   * elements, as mediated by the supplied criteria.
   *
   * <p>Implementations of this method must be idempotent and must return a determinate value.</p>
   *
   * <p>Implementations of this method must not return {@code null}.</p>
   *
   * <p>Implementations of this method should not call {@link #list()}, since that method is typically implemented in
   * terms of this one.</p>
   *
   * @param criteria the criteria to use; may be {@code null}
   *
   * @return an immuable sublist of this {@link Selectable}'s elements; never {@code null}
   */
  // Filters this thing according to the supplied criteria, producing a List.
  // List not Stream to permit caching
  // List not Collection so equals() is well-defined
  // List is unmodifiable and is always valid for the supplied criteria (unenforceable)
  // C and not Predicate because equality semantics for Predicate are not well-defined (caching again)
  public List<T> select(final C criteria);

  /**
   * Returns an immutable {@link List} of all of this {@link Selectable}'s elements.
   *
   * <p>Implementations of this method must be idempotent and must return a determinate value.</p>
   *
   * <p>Implementations of this method must not return {@code null}.</p>
   *
   * <p>The default implementation of this method calls the {@link #select(Object)} method with {@code null} as the sole
   * argument.</p>
   *
   * @return an immutable {@link List} of all of this {@link Selectable}'s elements; never {@code null}
   *
   * @see #select(Object)
   */
  public default List<T> list() {
    return this.select(null);
  }


  /*
   * Static methods.
   */


  /**
   * Returns a {@link Selectable} using the supplied {@link Collection} as its elements, and the supplied {@link
   * BiFunction} as its <em>selector function</em>.
   *
   * <p>There is no guarantee that this method will return new {@link Selectable} instances.</p>
   *
   * <p>The {@link Selectable} instances returned by this method may or may not cache their selections.</p>
   *
   * <p>The selector function must select a sublist from the supplied {@link Collection} as mediated by the supplied
   * criteria. The selector function must additionally be idempotent and must produce a determinate value when given the
   * same arguments.</p>
   *
   * <p>No validation of these semantics of the selector function is performed.</p>
   *
   * @param <C> the type of criteria
   *
   * @param <E> the type of the elements
   *
   * @param collection a {@link Collection} of elements from which sublists may be selected; must not be {@code null}
   *
   * @param f the selector function; must not be {@code null}
   *
   * @return a {@link Selectable}; never {@code null}
   *
   * @exception NullPointerException if either {@code collection} or {@code f} is {@code null}
   */
  @SuppressWarnings("unchecked")
  public static <C, E> Selectable<C, E> of(final Collection<? extends E> collection, final BiFunction<? super E, ? super C, ? extends Boolean> f) {
    Objects.requireNonNull(f, "f");
    return collection.isEmpty() ? c -> List.of() : c -> (List<E>)collection.stream()
      .filter(e -> f.apply(e, c))
      .toList();
  }

  /**
   * Returns a {@link Selectable} using the supplied {@link Collection} as its elements, and the supplied {@link
   * BiFunction} as its <em>selector function</em>.
   *
   * <p>There is no guarantee that this method will return new {@link Selectable} instances.</p>
   *
   * <p>The {@link Selectable} instances returned by this method will cache their selections.</p>
   *
   * <p>The selector function must select a sublist from the supplied {@link Collection} as mediated by the supplied
   * criteria. The selector function must additionally be idempotent and must produce a determinate value when given the
   * same arguments.</p>
   *
   * <p>No validation of these semantics of the selector function is performed.</p>
   *
   * @param <C> the type of criteria
   *
   * @param <E> the type of the elements
   *
   * @param collection a {@link Collection} of elements from which sublists may be selected; must not be {@code null}
   *
   * @param f the selector function; must not be {@code null}
   *
   * @return a {@link Selectable}; never {@code null}
   *
   * @exception NullPointerException if either {@code collection} or {@code f} is {@code null}
   */
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
