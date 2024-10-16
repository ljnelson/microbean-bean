/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2024 microBean™.
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

import java.io.Serializable;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import org.junit.jupiter.api.Test;

import org.microbean.lang.TypeAndElementSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.lang.Lang.typeAndElementSource;

final class TestContains {

  private static final TypeAndElementSource tes = typeAndElementSource();

  private TestContains() {
    super();
  }

  @Test
  final <T extends String, S extends T> void testWildcardContains() throws NoSuchMethodException {
    final java.lang.reflect.TypeVariable<java.lang.reflect.Method>[] tvs = this.getClass().getDeclaredMethod("testWildcardContains").getTypeParameters();
    final TypeVariable t = tes.typeVariable(tvs[0]);
    final TypeVariable s = tes.typeVariable(tvs[1]);
    final DeclaredType string = tes.declaredType("java.lang.String");
    final DeclaredType charSequence = tes.declaredType("java.lang.CharSequence");
    final DeclaredType object = tes.declaredType("java.lang.Object");
    final WildcardType qExtendsCharSequence = tes.wildcardType(charSequence, null);
    final WildcardType qExtendsString = tes.wildcardType(string, null);
    final WildcardType qSuperCharSequence = tes.wildcardType(null, charSequence);
    final WildcardType qSuperString = tes.wildcardType(null, string);

    assertTrue(tes.contains(string, string)); // reflexive
    assertTrue(tes.contains(qExtendsString, qExtendsString)); // reflexive
    assertTrue(tes.contains(qExtendsString, string));
    assertTrue(tes.contains(qExtendsCharSequence, charSequence));
    assertTrue(tes.contains(qExtendsCharSequence, string));
    assertTrue(tes.contains(qExtendsCharSequence, qExtendsString));

    assertTrue(tes.contains(qExtendsCharSequence, t));
    assertTrue(tes.contains(qExtendsCharSequence, s));
    assertTrue(tes.contains(qExtendsString, t));
    assertTrue(tes.contains(qExtendsString, s));
    
    assertTrue(tes.contains(qSuperString, qSuperString)); // reflexive
    assertTrue(tes.contains(qSuperString, string));
    assertTrue(tes.contains(qSuperString, charSequence));
    assertTrue(tes.contains(qSuperString, qSuperCharSequence));
    assertTrue(tes.contains(qSuperCharSequence, charSequence));
    assertTrue(tes.contains(qSuperCharSequence, object));

    assertFalse(tes.contains(qSuperCharSequence, string));
    assertFalse(tes.contains(string, charSequence));
    assertFalse(tes.contains(charSequence, string));
    assertFalse(tes.contains(charSequence, qExtendsCharSequence));
    assertFalse(tes.contains(string, qExtendsString));
    assertFalse(tes.contains(string, qSuperString));
    assertFalse(tes.contains(qExtendsString, qSuperString));
    assertFalse(tes.contains(qSuperString, qExtendsString));
  }

  @Test
  final <T extends Integer, S extends Integer & Serializable, R extends S> void testTypeVariableContainsNothingExceptItself() throws NoSuchMethodException {
    final java.lang.reflect.TypeVariable<java.lang.reflect.Method>[] tvs = this.getClass().getDeclaredMethod("testTypeVariableContainsNothingExceptItself").getTypeParameters();
    final TypeVariable t = tes.typeVariable(tvs[0]);
    final TypeVariable s = tes.typeVariable(tvs[1]);
    final TypeVariable r = tes.typeVariable(tvs[2]);
    final DeclaredType integer = tes.declaredType("java.lang.Integer");

    assertTrue(tes.contains(t, t));
    
    assertFalse(tes.contains(t, integer));
    assertFalse(tes.contains(integer, t));
    assertFalse(tes.contains(s, s.getUpperBound()));
    assertFalse(tes.contains(s.getUpperBound(), s));
    assertFalse(tes.contains(t, s));
    assertFalse(tes.contains(s, t));
    assertFalse(tes.contains(s, r));
    assertFalse(tes.contains(r, s));
    assertFalse(tes.contains(t, r));
    assertFalse(tes.contains(r, t));
  }

}
