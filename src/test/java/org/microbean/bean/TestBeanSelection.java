/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023–2024 microBean™.
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

import java.util.List;
import java.util.Optional;

import java.util.function.BiPredicate;

import javax.lang.model.type.TypeKind;

import org.junit.jupiter.api.Test;

import org.microbean.lang.TypeAndElementSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.lang.Lang.typeAndElementSource;

import static org.microbean.scope.Scope.NONE_ID;

final class TestBeanSelection {

  private static final TypeAndElementSource tes = typeAndElementSource();

  private static final Matcher<AttributedType, Id> matcher =
    new IdMatcher(new QualifiersMatcher(), new InterceptorBindingsMatcher(), new TypeMatcher(tes));

  private TestBeanSelection() {
    super();
  }

  @Test
  final void testCDI_502_CDI_823() {
    // This is interesting. The following must be permitted by
    // https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#assignable_parameters, bullet point 1 (see also
    // https://issues.redhat.com/browse/CDI-502?focusedId=13036118&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-13036118
    // and https://github.com/jakartaee/cdi/issues/823):
    //
    //   List<Optional<? extends Object>> x = List.<Optional<Object>>of();
    //
    // ...even though it does not compile. (Go ahead; uncomment the snippet above and see for yourself.)
    final AttributedType t =
      new AttributedType(tes.declaredType(tes.typeElement(List.class),
                                          tes.declaredType(tes.typeElement(Optional.class),
                                                           tes.wildcardType(tes.declaredType(Object.class),
                                                                            null))));
    assertTrue(matcher.test(t, new Id(List.of(tes.declaredType(tes.typeElement(List.class),
                                                               tes.declaredType(tes.typeElement(Optional.class),
                                                                                tes.declaredType(Object.class)))),
                                      List.of(),
                                      NONE_ID)));
  }

  @Test
  final void testStringSelectsString() {
    final AttributedType t = new AttributedType(tes.declaredType(String.class));
    assertTrue(matcher.test(t, new Id(List.of(tes.declaredType(String.class)),
                                      List.of(),
                                      NONE_ID)));
  }

  @Test
  final void testStringDoesNotSelectObject() {
    final AttributedType t = new AttributedType(tes.declaredType(String.class));
    assertFalse(matcher.test(t, new Id(List.of(tes.declaredType(Object.class)),
                                       List.of(),
                                       NONE_ID)));
  }

  @Test
  final void testIntSelectsInteger() {
    final AttributedType t = new AttributedType(tes.primitiveType(TypeKind.INT));
    assertTrue(matcher.test(t, new Id(List.of(tes.declaredType(Integer.class)),
                                      List.of(),
                                      NONE_ID)));
  }

  @Test
  final void testObjectDoesNotSelectString() {
    final AttributedType t = new AttributedType(tes.declaredType(Object.class));
    assertFalse(matcher.test(t, new Id(List.of(tes.declaredType(String.class)),
                                       List.of(),
                                       NONE_ID)));
  }

  @Test
  final void testListUnknownExtendsStringSelectsListString() {
    final AttributedType t = new AttributedType(tes.declaredType(tes.typeElement(List.class),
                                                                 tes.wildcardType(tes.declaredType(String.class),
                                                                                  null)));
    assertTrue(matcher.test(t, new Id(List.of(tes.declaredType(tes.typeElement(List.class),
                                                               tes.declaredType(String.class))),
                                      List.of(),
                                      NONE_ID)));
  }

}
