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

import java.util.List;
import java.util.SequencedSet;

import javax.lang.model.element.VariableElement;

// Subordinate to Factory<I>.
// Akin to CDI's Producer.
// Handles instance production and disposal, *including intercepted production*.
// Does NOT handle initialization; see for example https://github.com/search?q=repo%3Aweld%2Fcore+%22.produce%28%29%22+language%3AJava&type=code
// Does NOT handle post-initialization.
// Does NOT handle business method interception.
// Does NOT handle pre-disposal.
// See also: InterceptingProducer
@FunctionalInterface
public interface Producer<I> extends Aggregate {

  public default void dispose(final I i, final Request<I> r) {
    if (i instanceof AutoCloseable ac) {
      try {
        ac.close();
      } catch (final RuntimeException | Error e) {
        throw e;
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new DestructionException(e.getMessage(), e);
      } catch (final Exception e) {
        throw new DestructionException(e.getMessage(), e);
      }
    }
  }

  public default I produce(final Request<I> r) {
    return this.produce(this.assign(r));
  }

  /**
   * Produces a new contextual instance and returns it, possibly (often) making use of the supplied, dependent,
   * contextual references.
   *
   * <p>Implementations of this method must not call {@link #produce(Creation, ReferenceSelector)} or an infinite loop
   * may result.</p>
   *
   * @param dependentContextualReferences a {@link List} of other objects this {@link Producer} needs to create the contextual
   * instance; must not be {@code null}
   *
   * @return a new contextual instance, or {@code null}
   *
   * @exception NullPointerException if {@code dependentContextualReferences} is {@code null}
   *
   * @see 
   */
  public I produce(final List<?> dependentContextualReferences);

}
