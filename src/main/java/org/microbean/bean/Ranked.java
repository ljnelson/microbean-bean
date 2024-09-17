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

/**
 * An interface whose implementations can be ranked numerically in ascending order (highest rank wins).
 *
 * <p>In addition, an implementation may be designated as an {@linkplain #alternate() alternate}, which may affect the
 * interpretation of the implementation's {@linkplain #rank() rank}.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #alternate()
 *
 * @see #rank()
 */
public interface Ranked {


  /*
   * Static fields.
   */

  
  /**
   * The default rank ({@value}) when returned by an implementatino of the {@link #rank()} method.
   *
   * @see #rank()
   */
  public static final int DEFAULT_RANK = 0;


  /*
   * Instance methods.
   */


  /**
   * Returns the rank of this {@link Ranked} implementation.
   *
   * <p>Implementations of this method may return any integer, including negative ones.</p>
   *
   * <p>The default implementation of this method returns the value of the {@link #DEFAULT_RANK} field ({@value
   * #DEFAULT_RANK}).</p>
   *
   * <p>Given two ranks, <em>i</em> and <em>j</em>, <em>i</em> <em>outranks</em> <em>j</em> if and only if <em>i</em> is
   * greater than ({@code >}) <em>j</em>.</p>
   *
   * @return the rank of this {@link Ranked} implementation
   *
   * @see #outranks(Ranked)
   */
  // Highest rank wins
  public default int rank() {
    return DEFAULT_RANK;
  }

  /**
   * Returns {@code true} if this {@link Ranked} is to be considered an <em>alternate</em>, which may have an effect on
   * how the return value of the {@link #rank()} method is interpreted in some situations.
   *
   * <p>The default implementation of this method returns {@code false}.</p>
   *
   * <p>Overrides of this method must be idempotent and return a determinate value.</p>
   *
   * @return {@code true} if this {@link Ranked} is to be considered an <em>alternate</em>
   */
  public default boolean alternate() {
    return false;
  }

  /**
   * Returns {@code true} if this {@link Ranked} outranks the supplied {@link Ranked} according to the rules described
   * in the {@linkplain #rank() specification for the <code>rank()</code> method}.
   *
   * <p>Overriding this method, while possible and permitted, is discouraged.</p>
   *
   * @param other a {@link Ranked}; may be {@code null} (in which case this method will return {@code true})
   *
   * @return {@code true} if this {@link Ranked} outranks the supplied {@link Ranked}
   */
  public default boolean outranks(final Ranked other) {
    return other == null || this.rank() > other.rank();
  }

}
