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

import java.util.function.BiFunction;

@FunctionalInterface
public interface BeanSelector {

  /**
   * Returns the sole {@link Bean} {@linkplain BeanSelectionCriteria#selects(Bean) selected by the supplied
   * <code>beanSelectionCriteria</code>}, or {@code null} if there is no such {@link Bean}.
   *
   * <p>If there is more than one {@link Bean} {@linkplain BeanSelectionCriteria#selects(Bean) selected by the supplied
   * <code>beanSelectionCriteria</code>}, then the supplied {@link BiFunction}, serving as a disambiguator, is invoked
   * with the supplied {@code beanSelectionCriteria} and the selected {@link Bean}s and its result is returned.</p>
   *
   * @param beanSelectionCriteria a {@link BeanSelectionCriteria}; must not be {@code null}
   *
   * @param op a disambiguating {@link BiFunction} that can further resolve a {@link Collection} of {@link Bean}s to a
   * single {@link Bean}; must not be {@code null}
   *
   * @return the resolved {@link Bean}, or {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @exception RuntimeException if the supplied {@code op} throws a {@link RuntimeException}
   */
  // Give me the single Bean that matches, null if none match, or run op on the conflicting bits
  public Bean<?> bean(final BeanSelectionCriteria beanSelectionCriteria,
                      final BiFunction<? super BeanSelectionCriteria, ? super Collection<? extends Bean<?>>, ? extends Bean<?>> op);

  /**
   * Returns the sole {@link Bean} {@linkplain BeanSelectionCriteria#selects(Bean) selected by the supplied
   * <code>beanSelectionCriteria</code>}, or {@code null} if there is no such {@link Bean}.
   *
   * <p>If there is more than one {@link Bean} {@linkplain BeanSelectionCriteria#selects(Bean) selected by the supplied
   * <code>beanSelectionCriteria</code>}, then an {@link AmbiguousResolutionException} is thrown.</p>
   *
   * @param beanSelectionCriteria a {@link BeanSelectionCriteria}; must not be {@code null}
   *
   * @return the resolved {@link Bean}, or {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @exception AmbiguousResolutionException if the supplied {@code beanSelectionCriteria} selects more than one {@link
   * Bean}
   */
  // Give me the single Bean that matches, null if none match, or throw an exception
  public default Bean<?> bean(final BeanSelectionCriteria beanSelectionCriteria) {
    return this.bean(beanSelectionCriteria, Alternate.Resolver::fail);
  }

  public default BeanSelection<?> beanSelection(final BeanSelectionCriteria beanSelectionCriteria) {
    final Bean<?> bean = this.bean(beanSelectionCriteria);
    if (bean == null) {
      throw new UnsatisfiedResolutionException(beanSelectionCriteria);
    }
    return new BeanSelection<>(beanSelectionCriteria, bean);
  }

  public default BeanSelection<?> beanSelection(final BeanSelectionCriteria beanSelectionCriteria,
                                                final BiFunction<? super BeanSelectionCriteria, ? super Collection<? extends Bean<?>>, ? extends Bean<?>> op) {
    final Bean<?> bean = this.bean(beanSelectionCriteria, op);
    if (bean == null) {
      throw new UnsatisfiedResolutionException(beanSelectionCriteria);
    }
    return new BeanSelection<>(beanSelectionCriteria, bean);
  }

}
