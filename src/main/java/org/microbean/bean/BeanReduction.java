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
import java.util.Objects;

/**
 * A representation of the reduction that has occurred of a {@link BeanSelection}, yielding a single {@link Bean}.
 *
 * <p>A {@link BeanReduction} provides, in addition to a result, a mechanism for determining that a selection has
 * occurred according to some criteria.</p>
 *
 * @param <I> the type of contextual instances the associated {@link Bean} creates
 *
 * @param beanSelection a non-{@code null} {@link BeanSelection} that {@linkplain BeanSelection#contains(Bean) contains}
 * the <a href="#param-bean">{@code bean}</a> record component
 *
 * @param bean a non-{@code null} {@link Bean}
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see BeanSelection
 */
public record BeanReduction<I>(BeanSelection beanSelection, Bean<I> bean) {


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link BeanReduction} from an implicit {@link BeanSelection} that selects only the given {@link
   * Bean}.
   *
   * @param beanSelectionCriteria a {@link BeanSelectionCriteria}; may be {@code null}
   *
   * @param bean the {@link Bean} that is both the sole selection and the reduction; must not be {@code null}
   *
   * @exception NullPointerException if {@code bean} is {@code null}
   *
   * @exception IllegalArgumentException if the supplied {@link BeanSelectionCriteria} does not {@linkplain
   * BeanSelectionCriteria#selects(Bean) select} the supplied {@link Bean}
   *
   * @see BeanSelection
   */
  public BeanReduction(final BeanSelectionCriteria beanSelectionCriteria, final Bean<I> bean) {
    this(new BeanSelection(beanSelectionCriteria, List.of(bean)), bean);
  }

  /**
   * Creates a new {@link BeanReduction}.
   *
   * @param beanSelection a {@link BeanSelection}; must not be {@code null}
   *
   * @param bean the {@link Bean} representing the reduction; must not be {@code null}
   *
   * @exception NullPointerException if either {@code beanSelection} or {@code bean} is {@code null}
   *
   * @exception IllegalArgumentException if {@code bean} is not {@linkplain BeanSelection#contains(Bean) contained} by
   * {@code beanSelection}
   */
  public BeanReduction {
    if (!beanSelection.contains(Objects.requireNonNull(bean, "bean"))) {
      throw new IllegalArgumentException("beanSelection: " + beanSelection + "; bean: " + bean);
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Returns this {@link BeanReduction}'s {@link #beanSelection() BeanSelection}'s {@link
   * BeanSelection#beanSelectionCriteria() BeanSelectionCriteria}.
   *
   * @return this {@link BeanReduction}'s {@link #beanSelection() BeanSelection}'s {@link
   * BeanSelection#beanSelectionCriteria() BeanSelectionCriteria}.
   */
  public final BeanSelectionCriteria beanSelectionCriteria() {
    return this.beanSelection().beanSelectionCriteria();
  }

}
