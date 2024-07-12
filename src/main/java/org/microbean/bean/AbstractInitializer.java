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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.SequencedSet;

public class AbstractInitializer<I> implements Initializer<I> {


  /*
   * Instance fields.
   */


  private final SequencedSet<Dependency> dependencies;


  /*
   * Constructors.
   */


  public AbstractInitializer() {
    super();
    this.dependencies = Aggregate.EMPTY_DEPENDENCIES;
  }

  public AbstractInitializer(final SequencedSet<? extends Dependency> dependencies) {
    super();
    this.dependencies =
      dependencies == null || dependencies.isEmpty() ? EMPTY_DEPENDENCIES : Collections.unmodifiableSequencedSet(new LinkedHashSet<>(dependencies));
  }


  /*
   * Instance methods.
   */


  @Override // Initializer<I>
  public I initialize(final I i, final Request<I> r) {
    return i;
  }

  @Override // Initializer<I> (Aggregate)
  public final SequencedSet<Dependency> dependencies() {
    return this.dependencies;
  }

}
