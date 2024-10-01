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

/**
 * A request for a contextual reference of a particular type, along with functionality to help fulfil the request.
 *
 * @param <I> the type of the contextual reference
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #beanReduction()
 *
 * @see ReferenceSelector
 *
 * @see Creation
 */
public interface Request<I> extends Creation<I>, ReferenceSelector {

  /**
   * Returns the {@link BeanReduction} describing this {@link Request} in progress.
   *
   * @return the {@link BeanReduction} describing this {@link Request} in progress; never {@code null}
   *
   * @idempotency Implementations of this method must be idempotent and deterministic.
   *
   * @nullability Implementations of this method must not return {@code null}.
   *
   * @threadsafety Implementations of this method must be safe for concurrent use by multiple threads.
   *
   * @see BeanReduction
   */
  public BeanReduction<I> beanReduction();

  // Returns a new "child" Request, where "childness" means "in the service of the parent". So a request for a
  // CoffeeMaker is the logical parent of a request for a Heater, which is the new child.
  //
  // It is very common for a Request implementation to also be (or house) an AutoCloseableRegistry implementation. So if
  // the Heater object is in "none"/dependent scope, it should get added to the parent's set of AutoCloseables to be
  // closed when the parent goes out of scope, and if a child request for a Fuse object gets created, then the Fuse
  // should be added to the child's set of AutoCloseables, and so on.
  //
  // See AutoCloseableRegistry#newChild() for how that part works.
  //
  // Note that the semantics of *this* newChild(BeanReduction) method don't have the same "tree node"-like quality as
  // AutoCloseableRegistry#newChild(). That is, there's no requirement that a child Request retain a reference to its
  // parent Request.
  public <J> Request<J> newChild(final BeanReduction<J> beanReduction);

  @SuppressWarnings("unchecked")
  public default <R> R reference(final BeanSelectionCriteria beanSelectionCriteria) {
    return this.reference(beanSelectionCriteria, (Creation<R>)this);
  }

}
