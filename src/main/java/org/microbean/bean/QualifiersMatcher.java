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

import java.util.function.Predicate;

import org.microbean.qualifier.NamedAttributeMap;

import static org.microbean.bean.Qualifiers.anyQualifier;
import static org.microbean.bean.Qualifiers.defaultQualifier;
import static org.microbean.bean.Qualifiers.defaultQualifiers;
import static org.microbean.bean.Qualifiers.qualifiers;

public final class QualifiersMatcher implements Matcher<Collection<? extends NamedAttributeMap<?>>, Collection<? extends NamedAttributeMap<?>>> {

  public QualifiersMatcher() {
    super();
  }

  @Override // Matcher<Collection<? extends NamedAttributeMap<?>>, Collection<? extends NamedAttributeMap<?>>>
  public final boolean test(final Collection<? extends NamedAttributeMap<?>> receiverAttributes,
                            final Collection<? extends NamedAttributeMap<?>> payloadAttributes) {
    final Collection<? extends NamedAttributeMap<?>> receiverQualifiers = qualifiers(receiverAttributes);
    final Collection<? extends NamedAttributeMap<?>> payloadQualifiers = qualifiers(payloadAttributes);
    if (receiverQualifiers.isEmpty()) {
      // Pretend receiver had [@Default] and payload had at least [@Default] (e.g. [@Default, @Any])
      return payloadQualifiers.isEmpty() || containsAllMatching(payloadQualifiers::contains, defaultQualifiers());
    } else if (payloadQualifiers.isEmpty()) {
      for (final NamedAttributeMap<?> receiverQualifier : receiverQualifiers) {
        if (anyQualifier(receiverQualifier) || defaultQualifier(receiverQualifier)) {
          // receiver had [@Default] or [@Any] or [@Default, @Any]; pretend payload had [@Default, @Any].
          return true;
        }
      }
      return false;
    }
    return containsAllMatching(payloadQualifiers::contains, receiverQualifiers);
  }

  private static final boolean containsAllMatching(final Predicate<? super Object> p, final Iterable<?> i) {
    for (final Object o : i) {
      if (!p.test(o)) {
        return false;
      }
    }
    return true;
  }

}
