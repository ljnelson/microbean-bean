/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2024 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.microbean.bean;

import java.util.function.BiPredicate;

/**
 * A {@link BiPredicate} with particular semantics associated with its {@link #test(Object, Object)} method.
 *
 * @param <A> the criteria object
 *
 * @param <B> the object being tested
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #test(Object, Object)
 */
@FunctionalInterface
public interface Matcher<A, B> extends BiPredicate<A, B> {

  /**
   * Returns {@code true} if and only if the first argument <dfn>matches</dfn> the second argument.
   *
   * @param a an object serving as a kind of criteria; must not be {@code null}
   *
   * @param b an object to test; must not be {@code null}
   *
   * @return {@code true} if and only if the first argument <dfn>matches</dfn> the second argument; {@code false}
   * otherwise
   *
   * @exception NullPointerException if either {@code a} or {@code b} is {@code null}
   */
  @Override // BiPredicate<A, B>
  public boolean test(final A a, final B b);

}
