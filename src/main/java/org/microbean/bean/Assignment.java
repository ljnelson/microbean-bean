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

import java.util.Objects;

/**
 * An assignment of a contextual reference to a {@link Dependency}, usually as found by a {@link ReferenceSelector}.
 *
 * @param dependency the {@link Dependency}; must not be {@code null}
 *
 * @param value the contextual reference; may be {@code null}
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public record Assignment(Dependency dependency, Object value) {

  /**
   * Creates a new {@link Assignment}.
   *
   * @param dependency the {@link Dependency}; must not be {@code null}
   *
   * @param value the contextual reference; may be {@code null}
   */
  public Assignment {
    Objects.requireNonNull(dependency, "dependency");
  }

  /**
   * Creates a new {@link Assignment}.
   *
   * @param d the {@link Dependency}; must not be {@code null}
   *
   * @param r a {@link Request} used to locate the contextual reference; must not be {@code null}
   *
   * @exception NullPointerException if {@code r} is {@code null}
   */
  public Assignment(final Dependency d, final Request<?> r) {
    this(d, r.reference(d.beanSelectionCriteria(), r));
  }

}
