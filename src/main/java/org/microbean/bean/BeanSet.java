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
import java.util.SequencedSet;

import java.util.function.BiFunction;

/**
 * A notional, immutable, and threadsafe set of {@link Bean}s that permits certain kinds of querying and resolution.
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 */
public interface BeanSet extends BeanSelector, BeansSelector {

  /**
   * Returns an entirely immutable {@link SequencedSet} of {@link Bean}s containing all {@link Bean}s known to this
   * {@link BeanSet} implementation.
   *
   * @return an entirely immutable {@link SequencedSet} of {@link Bean}s containing all {@link Bean}s known to this
   * {@link BeanSet} implementation; never {@code null}
   */
  // Give me all the Beans
  public SequencedSet<Bean<?>> beans();

}
