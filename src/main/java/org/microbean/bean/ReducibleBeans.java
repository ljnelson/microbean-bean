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
import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;

public final class ReducibleBeans implements Selectable<BeanSelectionCriteria, Bean<?>>, Reducible<BeanSelectionCriteria, Bean<?>> {

  private final Map<BeanSelectionCriteria, Bean<?>> reductions;

  private final Selectable<BeanSelectionCriteria, Bean<?>> f;

  private final Reducer<BeanSelectionCriteria, Bean<?>> r;

  public ReducibleBeans(final Selectable<BeanSelectionCriteria, Bean<?>> f,
                        final Reducer<BeanSelectionCriteria, Bean<?>> r) {
    super();
    this.reductions = new ConcurrentHashMap<>();
    this.f = Objects.requireNonNull(f, "f");
    this.r = Objects.requireNonNull(r, "r");
  }

  // Implemented mostly for convenience.
  @Override // Beans (Selectable<BeanSelectionCriteria, Bean<?>>)
  public final List<Bean<?>> select(final BeanSelectionCriteria c) {
    return this.f.select(c);
  }

  @Override // ReducibleBeans (Reducible<BeanSelectionCriteria, Bean<?>>)
  public final Bean<?> reduce(final BeanSelectionCriteria c) {
    return reductions.computeIfAbsent(c, this::computeReduction);
  }

  private final Bean<?> computeReduction(final BeanSelectionCriteria c) {
    return this.r.reduce(this.f, c);
  }

}
