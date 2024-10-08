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

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;

import static org.microbean.bean.ConstantDescs.CD_Bean;
import static org.microbean.bean.ConstantDescs.CD_Factory;
import static org.microbean.bean.ConstantDescs.CD_Id;

/**
 * A ({@link Constable}) pairing of an {@link Id} with a {@link Factory}.
 *
 * @param <I> the type of the contextual instances the associated {@link Factory} creates
 *
 * @param id the {@link Id}; must not be {@code null}
 *
 * @param factory the {@link Factory}; must not be {@code null}
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see Factory
 *
 * @see Id
 */
public final record Bean<I>(Id id, Factory<I> factory) implements Aggregate, Constable, Ranked {

  /**
   * Creates a new {@link Bean}.
   *
   * @param id the {@link Id}; must not be {@code null}
   *
   * @param factory the {@link Factory}; must not be {@code null}
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  public Bean {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(factory, "factory");
  }

  @Override // Alternate
  public final boolean alternate() {
    return this.id().alternate();
  }

  /**
   * Returns this {@link Bean}, forcibly cast appropriately.
   *
   * @param <X> the type of contextual instances this {@link Bean} creates
   *
   * @return this {@link Bean}
   */
  @SuppressWarnings("unchecked")
  public final <X> Bean<X> cast() {
    return (Bean<X>)this;
  }

  @Override // Aggregate
  public final SequencedSet<AttributedElement> dependencies() {
    return this.factory().dependencies();
  }

  @Override // Constable
  public final Optional<DynamicConstantDesc<Bean<I>>> describeConstable() {
    return (this.factory() instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty())
      .flatMap(factoryDesc -> this.id().describeConstable()
               .map(idDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                     MethodHandleDesc.ofConstructor(CD_Bean,
                                                                                    CD_Id,
                                                                                    CD_Factory),
                                                     idDesc,
                                                     factoryDesc)));
  }

  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && other.getClass() == this.getClass()) {
      return Objects.equals(this.id(), ((Bean<?>)other).id());
    } else {
      return false;
    }
  }

  @Override // Object
  public int hashCode() {
    return this.id().hashCode();
  }

  @Override // Alternate (Ranked)
  public final int rank() {
    return this.id().rank();
  }

}
