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
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class ExperimentalFunctions {

  private ExperimentalFunctions() {
    super();
  }

  /**
   * Returns a {@link BiFunction} that will resolve dependencies.
   *
   * @param <E> the type of thing the returned dependency resolver can resolve dependencies for; normally something
   * (such as a {@link javax.lang.model.element.VariableElement} or {@link java.lang.reflect.Parameter} or {@link
   * java.lang.reflect.Field} or similar) representing a field or an executable's parameter
   *
   * @param bscf a {@link Function} that accepts an argument and returns a suitable {@link BeanSelectionCriteria} (or
   * {@code null}); must not be {@code null}
   *
   * @param optionalCf a {@link BiFunction} that accepts a {@link Creation} and an {@link Element} and returns a
   * suitable {@link Creation} (almost always the supplied {@link Creation}, but possibly a different one to account for
   * transient references); may be {@code null}
   *
   * @param rs a {@link BiFunction} that accepts a {@link BeanSelectionCriteria} and a {@link Creation} and returns a
   * resolved dependency, which may be {@code null}; must not be {@code null}; normally {@link
   * Request#reference(BeanSelectionCriteria, Creation) Request::reference}
   *
   * @return a {@link BiFunction} that accepts a key and a {@link Creation} and returns a resolved dependency suitable
   * for that key; never {@code null}
   *
   * @exception NullPointerException if {@code bscf} or {@code rs} is {@code null}
   */
  public static final <E> BiFunction<? super E, ? super Creation<?>, ?> dependencyResolver(final Function<? super E, ? extends BeanSelectionCriteria> bscf,
                                                                                           final BiFunction<? super Creation<?>, ? super E, ? extends Creation<?>> optionalCf,
                                                                                           final BiFunction<? super BeanSelectionCriteria, ? super Creation<?>, ?> rs) {
    Objects.requireNonNull(bscf, "bscf");
    Objects.requireNonNull(rs, "rs");
    final BiFunction<? super Creation<?>, ? super E, ? extends Creation<?>> cf =
      optionalCf == null ? ExperimentalFunctions::returnFirstArgument : optionalCf;
    return (e, c) -> Optional.ofNullable(bscf.apply(e)).map(bsc -> rs.apply(bsc, cf.apply(c, e))).orElseThrow(UnsatisfiedReductionException::new);
  }

  // (For setting up InvocationContexts for interceptors.)
  public static final <E> Collection<?> arguments(final Collection<? extends E> es,
                                                  final Creation<?> c,
                                                  final BiFunction<? super E, ? super Creation<?>, ?> dr) {
    return es.stream()
      .map(e -> dr.apply(e, c))
      .toList();
  }

  public static final <E> Object[] argumentsArray(final Collection<? extends E> es,
                                                  final Creation<?> c,
                                                  final BiFunction<? super E, ? super Creation<?>, ?> dr) {
    return arguments(es, c, dr).toArray();
  }

  
  private static final <X, Y> X returnFirstArgument(final X x, final Y y) {
    return x;
  }
  
}
