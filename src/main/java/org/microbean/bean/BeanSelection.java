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
import java.util.Objects;

@Deprecated // not currently needed, but may be useful later for reporting
final record BeanSelection(AttributedType attributedType, List<Bean<?>> beans) {

  public BeanSelection(final AttributedType attributedType, final Bean<?> bean) {
    this(attributedType, List.of(bean));
  }

  public BeanSelection {
    Objects.requireNonNull(attributedType, "attributedType");
    beans = beans.isEmpty() ? List.of() : List.copyOf(beans);
  }

}
