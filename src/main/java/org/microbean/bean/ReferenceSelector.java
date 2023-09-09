/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023 microBean™.
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

public interface ReferenceSelector extends AutoCloseable {

  public BeanSet beanSet();

  public <I> Creation<I> creation();

  public <R> R reference(final BeanSelector selector, final Bean<R> bean, final Creation<R> creation);

  @Override // AutoCloseable
  public default void close() {

  }

  public default <R> R reference(final BeanSelector selector, final Creation<R> creation) {
    final Bean<?> b = this.beanSet().bean(selector);
    if (b == null) {
      throw new UnsatisfiedResolutionException(selector);
    }
    return this.reference(selector, b.cast(), creation);
  }

}
