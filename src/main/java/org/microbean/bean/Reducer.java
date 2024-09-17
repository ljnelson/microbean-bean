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

import java.util.concurrent.ConcurrentHashMap;

import java.util.function.BiFunction;

/**
 * A {@linkplain FunctionalInterface functional interface} whose implementations can <em>reduce</em> a supplied {@link
 * List} of elements to a single element according to some <em>criteria</em>.
 *
 * <p>The reduction may be a simple filtering operation, or may be a summing or aggregating operation, or anything
 * else.</p>
 *
 * <p>This interface is related to, but should not be confused with, the {@link Reducible} interface.</p>
 *
 * <p>{@link Reducer} implementations are often used to help build {@link Reducible} implementations. See, for example,
 * {@link Reducible#ofCaching(Selectable, Reducer, BiFunction)}.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #reduce(List, Object, BiFunction)
 *
 * @see Reducible
 */
@FunctionalInterface
public interface Reducer<C, T> {

  /**
   * Performs some kind of reductive or filtering operation on the supplied {@link List}, according to the supplied
   * criteria, and returns the single result, or, if reduction fails, invokes the supplied {@link BiFunction} with a
   * sublist representing a partial reduction (or an empty list representing a reduction that simply could not be
   * performed), along with the supplied criteria, and returns its result.
   *
   * <p>Implementations of this method must return determinate values.</p>
   *
   * @param elements the {@link List} to reduce; must not be {@code null}
   *
   * @param c the criteria effectively describing the reduction; may be {@code null} to indicate no criteria
   *
   * @param failureHandler a {@link BiFunction} receiving a partial reduction and the criteria that returns a substitute
   * reduction (or, more commonly, throws an exception); must not be {@code null}
   *
   * @return a single element drawn or computed from the supplied {@code elements} which may be {@code null}
   *
   * @see #fail(List, Object)
   */
  // List, not Stream, for equality semantics and caching purposes.
  // List, not Set, because it's much faster and reduction can take care of duplicates if needed
  // C, not Predicate, because it may not be necessary to actually filter the list
  // failureHandler will receive only those elements that could not be eliminated
  // c is a pass-through used only during failure
  public T reduce(final List<? extends T> elements,
                  final C c,
                  final BiFunction<? super List<? extends T>, ? super C, ? extends T> failureHandler);


  /*
   * Default methods.
   */


  public default T reduce(final Selectable<? super C, ? extends T> f,
                          final C c,
                          final BiFunction<? super List<? extends T>, ? super C, ? extends T> failureHandler) {
    return this.reduce(f.select(c), c, failureHandler);
  }

  public default T reduce(final Selectable<? super C, ? extends T> f, final C c) {
    return this.reduce(f.select(c), c, Reducer::fail);
  }

  public default T reduce(final List<? extends T> elements, final C c) {
    return this.reduce(elements, c, Reducer::fail);
  }

  // Experimental. Probably overkill. Unbounded.
  public default Reducer<C, T> cached() {
    record Key<C, T>(List<T> l, C c) {};
    final Map<Key<C, T>, T> cache = new ConcurrentHashMap<>();
    return (l, c, fh) -> cache.computeIfAbsent(new Key<C, T>(List.copyOf(l), c), k -> this.reduce(k.l(), k.c(), fh));
  }


  /*
   * Static methods.
   */


  // A Reducer that simply calls its supplied failure handler.
  public static <C, T> Reducer<C, T> ofFailing() {
    return (l, c, fh) -> fh.apply(l, c);
  }

  // Default failure handler; call by method reference
  public static <C, T> T fail(final List<? extends T> elements, final C c) {
    return elements.stream().reduce((e0, e1) -> {
        throw new AmbiguousReductionException(c, elements, "Cannot resolve: " + elements);
      })
      .orElseThrow(() -> new UnsatisfiedReductionException((Object)c));
  }

  // Convenience failure handler; call by method reference
  public static <A, B, C> C returnNull(final A a, final B b) {
    return null;
  }

}
