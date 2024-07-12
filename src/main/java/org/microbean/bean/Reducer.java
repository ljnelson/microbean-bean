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

// Experimental.
public interface Reducer<C, T> {

  // List, not Stream, for equality semantics and caching purposes.
  // List, not Set, because it's much faster and reduction can take care of duplicates if needed
  // C, not Predicate, because it may not be necessary to actually filter the list
  // failureHandler will receive only those elements that could not be eliminated
  // c is a pass-through used only during failure
  public T reduce(final List<? extends T> elements,
                  final C c,
                  final BiFunction<? super List<? extends T>, ? super C, ? extends T> failureHandler);

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

  // A Reducer that simply calls its supplied failure handler.
  public static <C, T> Reducer<C, T> ofFailing() {
    return (l, c, fh) -> fh.apply(l, c);
  }

  // Probably overkill.
  public static <C, T> Reducer<C, T> ofCaching(final Reducer<C, T> r) {
    record Key<C, T>(List<T> l, C c) {};
    final Map<Key<C, T>, T> cache = new ConcurrentHashMap<>();
    return (l, c, fh) -> cache.computeIfAbsent(new Key<C, T>(List.copyOf(l), c), k -> r.reduce(k.l(), k.c(), fh));
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
