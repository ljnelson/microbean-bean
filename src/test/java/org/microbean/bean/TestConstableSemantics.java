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

import java.lang.constant.Constable;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

import java.lang.invoke.MethodHandles;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.microbean.constant.Constables;

import org.microbean.lang.Lang;
import org.microbean.lang.SameTypeEquality;
import org.microbean.lang.TypeAndElementSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodHandles.privateLookupIn;

import static org.microbean.bean.Qualifiers.anyAndDefaultQualifiers;

import static org.microbean.scope.Scope.SINGLETON_ID;

final class TestConstableSemantics {

  private static TypeAndElementSource tes;
  
  private TestConstableSemantics() {
    super();
  }

  @BeforeAll
  static final void initializeTes() {
    tes = Lang.typeAndElementSource();
  }

  @Test
  final void testAnyAndDefaultQualifiersList() throws ReflectiveOperationException {
    final List<?> list = anyAndDefaultQualifiers();
    assertEquals(list, Constables.describeConstable(list).orElseThrow().resolveConstantDesc(MethodHandles.lookup()));
  }

  @Test
  final void testId() throws ReflectiveOperationException {
    final Id id =
      new Id(List.of(tes.declaredType(String.class), tes.declaredType(Object.class)),
             anyAndDefaultQualifiers(),
             SINGLETON_ID);
    assertEquals(id, Constables.describeConstable(id).orElseThrow().resolveConstantDesc(lookup()));
  }

  /*
  @Test
  final void testFactory() throws ReflectiveOperationException {
    final Factory<String> f = new Singleton<>("Hello");
    assertNull(f.singleton());
    @SuppressWarnings("unchecked")
    final Factory<String> f2 = (Factory<String>)Constables.describeConstable(f).orElseThrow().resolveConstantDesc(lookup());
    assertNotSame(f, f2);
    assertSame(f.singleton(), f2.singleton());
  }
  */

  @Test
  final void testConstant() throws ReflectiveOperationException {
    final Constant<String> c = new Constant<>("Hello");
    assertNotNull(c.singleton());
    @SuppressWarnings("unchecked")
    final Constant<String> c2 = (Constant<String>)Constables.describeConstable(c).orElseThrow().resolveConstantDesc(lookup());
    assertNotSame(c, c2);
    assertSame(c.singleton(), c2.singleton());
  }

}
