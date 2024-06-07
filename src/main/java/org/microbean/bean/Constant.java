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

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import java.util.Objects;
import java.util.Optional;

import org.microbean.constant.Constables;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_Object;

public final record Constant<I>(I singleton) implements Constable, Factory<I> {

  public Constant {
    Objects.requireNonNull(singleton, "singleton");
  }

  @Override // Factory<I>
  public final I create(final Request<I> r) {
    return this.singleton();
  }

  @Override // Factory<I>
  public final boolean destroys() {
    return this.singleton() instanceof AutoCloseable;
  }

  // Experimental
  @Override // Factory<I>
  public final void destroy(final I i, final AutoCloseable autoCloseableRegistry, final Request<I> request) {
    Factory.super.destroy(i, autoCloseableRegistry, request);
  }

  @Override // Constable
  public final Optional<DynamicConstantDesc<I>> describeConstable() {
    return Constables.describeConstable(this.singleton())
      .map(iDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                           MethodHandleDesc.ofConstructor(ClassDesc.of(this.getClass().getName()),
                                                                          CD_Object),
                                           iDesc));
  }


}
