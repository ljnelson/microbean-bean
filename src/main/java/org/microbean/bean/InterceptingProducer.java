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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;

import org.microbean.interceptor.InterceptionFunction;
import org.microbean.interceptor.InterceptorMethod;

import static org.microbean.interceptor.Interceptions.ofConstruction;

// Applies around-construct logic to contextual instance production.
public final class InterceptingProducer<I> implements Producer<I> {

  private final InterceptionFunction f;

  private final Producer<I> producer;

  @SuppressWarnings("unchecked")
  public InterceptingProducer(final Collection<? extends InterceptorMethod> interceptorMethods,
                              final Producer<I> producer) {
    super();
    this.producer = Objects.requireNonNull(producer, "producer");
    final SequencedSet<AttributedElement> dependencies = producer.dependencies();
    this.f = ofConstruction(interceptorMethods, (ignored, argumentsArray) -> {
        final SequencedSet<Assignment<?>> assignments = new LinkedHashSet<>();
        int i = 0;
        for (final AttributedElement dependency : dependencies) {
          assignments.add(new Assignment<>(dependency, argumentsArray[i++]));
        }
        return this.produce(Collections.unmodifiableSequencedSet(assignments));
      });
  }

  @Override // Producer<I> (Aggregate)
  public final SequencedSet<? extends Assignment<?>> assign(final Request<?> r) {
    return this.producer.assign(r);
  }

  @Override // Producer<I> (Aggregate)
  public final SequencedSet<AttributedElement> dependencies() {
    return this.producer.dependencies();
  }

  @Override // Producer<I>
  public final void dispose(final I i, final Request<I> r) {
    this.producer.dispose(i, r);
  }

  @Override // Producer<I>
  @SuppressWarnings("unchecked")
  public final I produce(final Request<?> r) {
    final Collection<? extends AttributedElement> dependencies = this.dependencies();
    final Object[] array = new Object[dependencies.size()];
    int i = 0;
    for (final AttributedElement d : dependencies) {
      array[i++] = r.reference(d.attributedType());
    }
    return (I)this.f.apply(array);
  }

  @Override // Producer<I>
  public final I produce(final SequencedSet<? extends Assignment<?>> assignments) {
    return this.producer.produce(assignments);
  }

}
