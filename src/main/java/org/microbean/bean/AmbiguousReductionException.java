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
import java.util.List;

public class AmbiguousReductionException extends ReductionException {

  private static final long serialVersionUID = 1L;

  private final transient Collection<?> alternates;

  public AmbiguousReductionException() {
    this(null, null, null, null);
  }

  public AmbiguousReductionException(final Collection<?> alternates,
                                     final String message) {
    this(null, alternates, message, null);
  }

  public AmbiguousReductionException(final Object criteria,
                                     final Collection<?> alternates,
                                     final String message) {
    this(criteria, alternates, message, null);
  }

  public AmbiguousReductionException(final Collection<?> alternates,
                                     final Throwable cause) {
    this(null, alternates, null, cause);
  }

  public AmbiguousReductionException(final Object criteria,
                                     final Collection<?> alternates,
                                     final Throwable cause) {
    this(criteria, alternates, null, cause);
  }

  public AmbiguousReductionException(final Collection<?> alternates,
                                     final String message,
                                     final Throwable cause) {
    this(null, alternates, message, cause);
  }

  public AmbiguousReductionException(final Object criteria,
                                     final Collection<?> alternates,
                                     final String message,
                                     final Throwable cause) {
    super(criteria, message, cause);
    if (alternates == null || alternates.isEmpty()) {
      this.alternates = List.of();
    } else {
      this.alternates = List.copyOf(alternates);
    }
  }

  public final Collection<?> alternates() {
    return this.alternates;
  }

}
