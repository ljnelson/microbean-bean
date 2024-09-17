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

import java.util.List;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.microbean.lang.TypeAndElementSource;

import org.microbean.lang.type.DelegatingTypeMirror;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.bean.BeanTypes.name;

import static org.microbean.lang.Lang.typeAndElementSource;

final class TestBeanTypes {

  private TypeAndElementSource tes;

  private BeanTypes beanTypes;

  private TestBeanTypes() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.tes = typeAndElementSource();
    this.beanTypes = new BeanTypes(this.tes);
  }


  @Test
  final void testDirectSupertypesOfintAreEmpty() {
    final List<? extends TypeMirror> ts = tes.directSupertypes(tes.primitiveType(TypeKind.INT));
    assertTrue(ts.isEmpty()); // although note JLS 4.10.1; oh well
  }

  @Test
  final void testSupertypesOfint() {
    final List<TypeMirror> ts = beanTypes.supertypes(tes.primitiveType(TypeKind.INT));
    assertEquals(1, ts.size());
    assertEquals("int", name(ts.get(0)));
  }

  @Test
  final void testBeanTypesOfint() {
    final List<TypeMirror> ts = beanTypes.beanTypes(tes.primitiveType(TypeKind.INT));
    assertEquals(2, ts.size());
    assertEquals("int", name(ts.get(0)));
    assertEquals("java.lang.Object", name(ts.get(1))); // per CDI specification
  }


  @Test
  final void testDirectSupertypesOfObjectArrayAreASingleIntersectionType() {
    final List<? extends TypeMirror> ts = tes.directSupertypes(tes.arrayTypeOf(tes.declaredType("java.lang.Object")));
    assertEquals(1, ts.size());
    assertEquals("java.io.Serializable&java.lang.Cloneable", name(ts.get(0))); // wut
  }

  @Test
  final void testSupertypesOfObjectArray() {
    final List<TypeMirror> ts = beanTypes.supertypes(tes.arrayTypeOf(tes.declaredType("java.lang.Object")));
    assertEquals(5, ts.size());
    assertEquals("java.lang.Object[]", name(ts.get(0)));
    assertEquals("java.io.Serializable&java.lang.Cloneable", name(ts.get(1))); // wut
    assertEquals("java.lang.Object", name(ts.get(2)));
    assertEquals("java.io.Serializable", name(ts.get(3)));
    assertEquals("java.lang.Cloneable", name(ts.get(4)));
  }

  @Test
  final void testBeanTypesOfObjectArray() {
    final List<TypeMirror> ts = beanTypes.beanTypes(tes.arrayTypeOf(tes.declaredType("java.lang.Object")));
    assertEquals(2, ts.size());
    assertEquals("java.lang.Object[]", name(ts.get(0)));
    assertEquals("java.lang.Object", name(ts.get(1)));
  }


  @Test
  final void testSupertypesOfString() {
    final List<TypeMirror> ts = beanTypes.supertypes(tes.declaredType("java.lang.String"));
    assertEquals(7, ts.size(), ts.toString());
    assertEquals("java.lang.String", name(ts.get(0)));
    assertEquals("java.lang.Object", name(ts.get(1)));
    assertEquals("java.io.Serializable", name(ts.get(2)));
    assertEquals("java.lang.CharSequence", name(ts.get(3)));
    assertEquals("java.lang.Comparable", name(ts.get(4)));
    assertEquals("java.lang.constant.Constable", name(ts.get(5)));
    assertEquals("java.lang.constant.ConstantDesc", name(ts.get(6)));
  }

  @Test
  final void testBeanTypesOfString() {
    final List<TypeMirror> ts = beanTypes.beanTypes(tes.declaredType("java.lang.String"));
    assertEquals(7, ts.size(), ts.toString());
    assertEquals("java.lang.String", name(ts.get(0)));
    assertEquals("java.lang.Object", name(ts.get(1)));
    assertEquals("java.io.Serializable", name(ts.get(2)));
    assertEquals("java.lang.CharSequence", name(ts.get(3)));
    assertEquals("java.lang.Comparable", name(ts.get(4)));
    assertEquals("java.lang.constant.Constable", name(ts.get(5)));
    assertEquals("java.lang.constant.ConstantDesc", name(ts.get(6)));

  }


  @Test
  final void testDirectSupertypesOfInterfaceA() {
    final List<? extends TypeMirror> ts = tes.directSupertypes(tes.declaredType("org.microbean.bean.TestBeanTypes.A"));
    assertEquals(1, ts.size());
    assertEquals("java.lang.Object", ts.get(0).toString());
  }

  @Test
  final void testDirectSupertypesOfInterfaceBExtendsA() {
    final List<? extends TypeMirror> ts = tes.directSupertypes(tes.declaredType("org.microbean.bean.TestBeanTypes.B"));
    assertEquals(2, ts.size());
    assertEquals("java.lang.Object", name(ts.get(0)));
    assertEquals("org.microbean.bean.TestBeanTypes.A", name(ts.get(1)));
  }

  @Test
  final void testDirectSupertypesOfInterfaceCExtendsBAreInDeclarationOrder() {
    final List<? extends TypeMirror> ts = tes.directSupertypes(tes.declaredType("org.microbean.bean.TestBeanTypes.C"));
    assertEquals(3, ts.size());
    assertEquals("java.lang.Object", name(ts.get(0)));
    assertEquals("org.microbean.bean.TestBeanTypes.A", name(ts.get(1)));
    assertEquals("org.microbean.bean.TestBeanTypes.B", name(ts.get(2)));
  }


  @Test
  final void testSupertypesOfListExtendsString() {
    final List<? extends TypeMirror> ts =
      beanTypes.supertypes(tes.declaredType(tes.typeElement("java.util.List"),
                                            tes.wildcardType(tes.declaredType("java.lang.String"),
                                                             null)));
    assertEquals(5, ts.size());
    assertEquals("java.lang.Object", name(ts.get(0)));
    assertEquals("java.util.List", name(ts.get(1)));
    assertEquals("java.util.SequencedCollection", name(ts.get(2)));
    assertEquals("java.util.Collection", name(ts.get(3)));
    assertEquals("java.lang.Iterable", name(ts.get(4)));
  }

  @Test
  final void testBeanTypesOfListExtendsString() {
    final List<TypeMirror> ts = beanTypes.beanTypes(tes.declaredType(tes.typeElement("java.util.List"),
                                                                     tes.wildcardType(tes.declaredType("java.lang.String"),
                                                                                      null)));
    assertEquals(1, ts.size());
    assertEquals("java.lang.Object", name(ts.get(0)));
  }


  @Test
  final void testSupertypesOfWildcard() {
    final TypeMirror t = tes.wildcardType(tes.declaredType("java.lang.String"), null);
    final List<TypeMirror> ts = beanTypes.supertypes(t);
    assertEquals(1, ts.size()); // no direct supertypes
    assertSame(t, ts.get(0));
  }

  @Test
  final void testBeanTypesOfWildcard() {
    assertTrue(beanTypes.beanTypes(tes.wildcardType(tes.declaredType("java.lang.String"), null)).isEmpty());
  }


  @Test
  final <T> void testSupertypesOfTypeVariable() throws ReflectiveOperationException {
    final List<TypeMirror> ts =
      beanTypes.supertypes(tes.typeVariable(this.getClass().getDeclaredMethod("testSupertypesOfTypeVariable").getTypeParameters()[0]));
    assertEquals(2, ts.size());
    assertEquals("T", name(ts.get(0)));
    assertEquals("java.lang.Object", name(ts.get(1)));
  }

  @Test
  final <T> void testBeanTypesOfTypeVariable() throws ReflectiveOperationException {
    final List<TypeMirror> ts = beanTypes.beanTypes(tes.typeVariable(this.getClass().getDeclaredMethod("testBeanTypesOfTypeVariable").getTypeParameters()[0]));
    assertEquals(1, ts.size());
    assertEquals("java.lang.Object", name(ts.get(0)));
  }


  private static interface A {};

  private static interface B extends A {};

  private static interface C extends A, B {};

}
