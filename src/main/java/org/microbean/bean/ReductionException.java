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

public class ReductionException extends BeanException {

  private static final long serialVersionUID = 1L;

  private final transient Object criteria;

  public ReductionException() {
    super();
    this.criteria = null;
  }

  public ReductionException(final Object criteria) {
    super();
    this.criteria = criteria;
  }

  public ReductionException(final String message) {
    super(message);
    this.criteria = null;
  }

  public ReductionException(final Object criteria,
                            final String message) {
    super(message);
    this.criteria = criteria;
  }

  public ReductionException(final Throwable cause) {
    super(cause);
    this.criteria = null;
  }

  public ReductionException(final Object criteria,
                            final Throwable cause) {
    super(cause);
    this.criteria = criteria;
  }

  public ReductionException(final String message,
                            final Throwable cause) {
    super(message, cause);
    this.criteria = null;
  }

  public ReductionException(final Object criteria,
                            final String message,
                            final Throwable cause) {
    super(message, cause);
    this.criteria = criteria;
  }

  public final Object criteria() {
    return this.criteria;
  }

  @Override
  public String toString() {
    final Object criteria = this.criteria();
    if (criteria == null) {
      return super.toString();
    } else if (this.getLocalizedMessage() == null) {
      return super.toString() + ": criteria: " + criteria;
    } else {
      return super.toString() + "; criteria: " + criteria;
    }
  }

}
