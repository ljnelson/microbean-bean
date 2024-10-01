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

import java.lang.constant.Constable;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import java.util.Optional;

import javax.lang.model.element.Element;

import org.microbean.lang.element.DelegatingElement;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;

import static org.microbean.bean.ConstantDescs.CD_BeanSelectionCriteria;
import static org.microbean.bean.ConstantDescs.CD_Dependency;

import static org.microbean.lang.ConstantDescs.CD_Element;

/**
 * A representation of a dependency that an {@link Element} has on a contextual reference described by a {@link
 * BeanSelectionCriteria}.
 *
 * <p>The dependency relationship represented by an instance of this class does not necessarily imply the existence of a
 * contextual reference that can satisfy it.</p>
 *
 * @param beanSelectionCriteria a {@link BeanSelectionCriteria}; must not be {@code null}
 *
 * @param element the {@link Element} advertising this dependency; must not be {@code null}
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 */
public record Dependency(Element element, BeanSelectionCriteria beanSelectionCriteria) implements Constable {


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link Dependency}.
   *
   * @param beanSelectionCriteria a {@link BeanSelectionCriteria}; must not be {@code null}
   *
   * @param element the {@link Element} advertising this dependency; must not be {@code null}
   */
  public Dependency {
    final TypeMatcher tm = beanSelectionCriteria.typeMatcher();
    element = DelegatingElement.of(element, tm.typeAndElementSource());
    if (element.getKind().isVariable()) {
      // Replace the bsc so its type is the element's
      beanSelectionCriteria = new BeanSelectionCriteria(tm, element.asType(), beanSelectionCriteria.attributes());
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Returns an {@link Optional} providing access to a {@link DynamicConstantDesc} describing this {@link Dependency}.
   *
   * @return an {@link Optional} providing access to a {@link DynamicConstantDesc} describing this {@link Dependency};
   * never {@code null}
   */
  @Override // Constable
  public final Optional<DynamicConstantDesc<Dependency>> describeConstable() {
    return ((DelegatingElement)this.element()).describeConstable()
      .flatMap(eDesc -> this.beanSelectionCriteria().describeConstable()
               .map(bscDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                      MethodHandleDesc.ofConstructor(CD_Dependency,
                                                                                     CD_Element,
                                                                                     CD_BeanSelectionCriteria),
                                                      bscDesc,
                                                      eDesc)));
  }

}
