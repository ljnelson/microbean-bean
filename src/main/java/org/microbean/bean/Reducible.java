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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A {@linkplain FunctionalInterface functional interface} whose implementations can reduce an unspecified notional
 * collection of elements to a single element according to some <em>criteria</em>.
 *
 * <p>This interface is related to, but should not be confused with, the {@link Reducer} interface, which can be used to
 * build {@link Reducible} instances.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #reduce(Object)
 *
 * @see Reducer
 */
@FunctionalInterface
public interface Reducible<C, T> {


  /**
   * Given a criteria object, which may be {@code null}, returns an object that represents the <em>reduction</em> of a
   * notional collection of objects.
   *
   * @param criteria the criteria; may be {@code null} to indicate no criteria
   *
   * @return a single object, or {@code null}
   *
   * @exception ReductionException if an error occurs
   */
  public T reduce(final C criteria);


  /*
   * Static methods.
   */


  public static <C, E> Reducible<C, E> of(final Selectable<C, E> selectable,
                                          final Reducer<C, E> r) {
    return of(selectable, r, Reducer::fail);
  }

  public static <C, E> Reducible<C, E> of(final Selectable<C, E> selectable,
                                          final Reducer<C, E> r,
                                          final BiFunction<? super List<? extends E>, ? super C, ? extends E> failureHandler) {
    Objects.requireNonNull(selectable, "selectable");
    Objects.requireNonNull(r, "r");
    final BiFunction<? super List<? extends E>, ? super C, ? extends E> fh =
      failureHandler == null ? Reducer::fail : failureHandler;
    return c -> r.reduce(selectable.select(c), c, fh);
  }

  public static <C, E> Reducible<C, E> ofCaching(final Selectable<C, E> selectable,
                                                 final Reducer<C, E> r) {
    return ofCaching(selectable, r, Reducer::fail);
  }

  public static <C, E> Reducible<C, E> ofCaching(final Selectable<C, E> selectable,
                                                 final Reducer<C, E> r,
                                                 final BiFunction<? super List<? extends E>, ? super C, ? extends E> failureHandler) {
    return ofCaching(selectable, r, failureHandler, new ConcurrentHashMap<C, E>()::computeIfAbsent);
  }

  public static <C, E> Reducible<C, E> ofCaching(final Selectable<C, E> selectable,
                                                 final Reducer<C, E> r,
                                                 final BiFunction<? super List<? extends E>, ? super C, ? extends E> failureHandler,
                                                 final BiFunction<? super C, Function<C, E>, ? extends E> computeIfAbsent) {
    final Reducible<C, E> rd = of(selectable, r, failureHandler);
    return c -> computeIfAbsent.apply(c, rd::reduce);
  }

}
