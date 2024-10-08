/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2024 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.microbean.bean;

import java.util.Collection;
import java.util.List;

import org.microbean.qualifier.NamedAttributeMap;

import static org.microbean.bean.InterceptorBindings.anyInterceptorBinding;
import static org.microbean.bean.InterceptorBindings.interceptorBindings;

public final class InterceptorBindingsMatcher implements Matcher<Collection<? extends NamedAttributeMap<?>>, Collection<? extends NamedAttributeMap<?>>> {

  public InterceptorBindingsMatcher() {
    super();
  }

  @Override // Matcher<Collection<? extends NamedAttributeMap<?>>, Collection<? extends NamedAttributeMap<?>>>
  public final boolean test(final Collection<? extends NamedAttributeMap<?>> receiverAttributes,
                            final Collection<? extends NamedAttributeMap<?>> payloadAttributes) {
    final List<? extends NamedAttributeMap<?>> payloadBindings = interceptorBindings(payloadAttributes);
    if (payloadBindings.isEmpty()) {
      return interceptorBindings(receiverAttributes).isEmpty();
    } else if (payloadBindings.size() == 1 && anyInterceptorBinding(payloadBindings.get(0))) {
      return true;
    }
    final List<? extends NamedAttributeMap<?>> receiverBindings = interceptorBindings(receiverAttributes);
    return
      receiverBindings.size() == payloadBindings.size() &&
      receiverBindings.containsAll(payloadBindings) &&
      payloadBindings.containsAll(receiverBindings);
  }

}
