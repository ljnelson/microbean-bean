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

import java.util.Objects;

@Deprecated(forRemoval = true) // not really needed because DefaultRequest is better suited
public class DefaultCreation<I> implements AutoCloseableRegistry, Creation<I> {

  private final AutoCloseableRegistry registry;

  // private final BeanSelectionCriteria beanSelectionCriteria;

  public DefaultCreation(final AutoCloseableRegistry registry) {
    // this(registry, null);
    super();
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  @Deprecated(forRemoval = true)
  public DefaultCreation(final AutoCloseableRegistry registry,
                         final BeanSelectionCriteria beanSelectionCriteria) {
    super();
    this.registry = Objects.requireNonNull(registry, "registry");
    // this.beanSelectionCriteria = beanSelectionCriteria;
  }

  @Override // Creation<I>
  public void created(final I instance) {
    // TODO:
  }

  @Override // AutoCloseableRegistry
  public DefaultCreation<I> newChild() {
    // return this.newChild(this.beanSelectionCriteria);
    return new DefaultCreation<>(this.registry.newChild());
  }

  @Deprecated(forRemoval = true)
  @SuppressWarnings("unchecked")
  public DefaultCreation<I> newChild(final BeanSelectionCriteria beanSelectionCriteria) {
    // return new DefaultCreation<>(this.registry.newChild(), beanSelectionCriteria);
    return new DefaultCreation<>(this.registry.newChild());
  }

  @Override // AutoCloseableRegistry
  public final void close() {
    this.registry.close();
  }

  @Override // AutoCloseableRegistry
  public final boolean register(final AutoCloseable ac) {
    return this.registry.register(ac);
  }

}
