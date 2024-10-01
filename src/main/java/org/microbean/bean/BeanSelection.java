/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023–2024 microBean™.
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
import java.util.Objects;

/**
 * A pairing of a {@link BeanSelectionCriteria} used to select a {@link List} of {@link Bean}s from somewhere, and the
 * immutable {@link List} of {@link Bean}s so selected.
 *
 * @param beanSelectionCriteria a non-{@code null} {@link BeanSelectionCriteria}
 *
 * @param beans a non-{@code null} {@link List} of {@link Bean}s {@linkplain BeanSelectionCriteria#selects(Bean)
 * selected} by the {@link BeanSelectionCriteria}
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public record BeanSelection(BeanSelectionCriteria beanSelectionCriteria, List<Bean<?>> beans) {

  /**
   * Creates a new {@link BeanSelection}.
   *
   * @param beanSelectionCriteria a non-{@code null} {@link BeanSelectionCriteria}
   *
   * @param beans a non-{@code null} {@link List} of {@link Bean}s {@linkplain BeanSelectionCriteria#selects(Bean)
   * selected} by the {@link BeanSelectionCriteria}
   *
   * @exception NullPointerException if either {@code beanSelectionCriteria} or {@code beans} is {@code null}
   *
   * @exception IllegalArgumentException if the supplied {@link BeanSelectionCriteria} does not {@linkplain
   * BeanSelectionCriteria#selects(Bean) select} all of the {@link Bean}s in the supplied {@link List} of {@link Bean}s
   *
   * @see BeanSelectionCriteria#selects(Bean)
   */
  public BeanSelection {
    if (beans == null || beans.isEmpty()) {
      Objects.requireNonNull(beanSelectionCriteria, "beanSelectionCriteria");
      beans = List.of();
    } else {
      beans = List.copyOf(beans);
      for (final Bean<?> bean : beans) {
        if (!beanSelectionCriteria.selects(bean)) {
          throw new IllegalArgumentException("beanSelectionCriteria: " + beanSelectionCriteria + "; beans: " + beans);
        }
      }
    }
  }

  /**
   * A convenience method that returns the value of invoking the {@link List#contains(Object)} method on the return
   * value of an invocation of this {@link BeanSelection}'s {@link #beans()} method.
   *
   * @param bean the {@link Bean} to check; may be {@code null} in which case {@code false} will be returned
   *
   * @return whether or not this {@link BeanSelection} contains the supplied {@link Bean}
   */
  public final boolean contains(final Bean<?> bean) {
    return bean != null && this.beans().contains(bean);
  }

}
