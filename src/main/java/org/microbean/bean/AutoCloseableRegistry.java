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

public interface AutoCloseableRegistry extends AutoCloseable, Cloneable {

  /**
   * Returns a new {@link AutoCloseableRegistry} instance that is not {@linkplain #closed() closed} and has no
   * {@linkplain #register(AutoCloseable)} registrations.
   *
   * <p>The new instance will be {@linkplain #register(AutoCloseable) registered} with this {@link
   * AutoCloseableRegistry} if this {@link AutoCloseableRegistry} is not {@linkplain #closed() closed}.</p>
   *
   * @return a new {@link AutoCloseableRegistry}
   *
   * @nullability Implementations of this method must not return {@code null}.
   *
   * @idempotency All invocations of implementations of this method must return new, distinct {@link AutoCloseable}
   * instances.
   *
   * @threadsafety Implementations of this method must be safe for concurrent use by multiple threads.
   *
   * @see Cloneable
   *
   * @see #closed()
   *
   * @see #register(AutoCloseable)
   */
  public AutoCloseableRegistry clone();

  // MUST be idempotent and thread safe
  @Override
  public void close();

  // MUST be idempotent and thread safe
  public boolean closed();

  // When closed, this must do nothing and return false, not throw an exception
  public boolean register(final AutoCloseable c);

}
