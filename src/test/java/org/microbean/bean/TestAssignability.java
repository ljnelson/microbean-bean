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

final class TestAssignability {

  private static final TypeAndElementSource tes = typeAndElementSource();

  private TestAssignability() {
    super();
  }

  @Test
  final <T extends Integer & Serializable, S extends Number & Serializable, R extends S> void testAssignabilityOfTypeVariable() throws NoSuchMethodException {
    final java.lang.reflect.TypeVariable<java.lang.reflect.Method>[] tvs = this.getClass().getDeclaredMethod("testAssignabilityOfTypeVariable").getTypeParameters();
    final TypeVariable t = tes.typeVariable(tvs[0]);
    final TypeVariable s = tes.typeVariable(tvs[1]);
    final TypeVariable r = tes.typeVariable(tvs[2]);
    final DeclaredType integer = tes.declaredType("java.lang.Integer");
    final DeclaredType serializable = tes.declaredType("java.io.Serializable");
    final DeclaredType number = tes.declaredType("java.lang.Number");

    //
    // IMPORTANT:
    //
    // Remember that TypeAndElementSource#assignable(), like javax.lang.model.util.Types#isAssignable() on which it is
    // based, is "backwards"!
    //

    // integer is the *payload*; number is the *receiver*; integer is assignable to number
    assertTrue(tes.assignable(integer, number));
    assertTrue(tes.assignable(integer, serializable));
    assertTrue(tes.assignable(number, serializable));

    // t is the *payload*; integer is the *receiver*; T is assignable to Integer
    assertTrue(tes.assignable(t, integer));
    assertTrue(tes.assignable(t, serializable));

    // t has multiple bounds so its upper bound is an intersection type
    final IntersectionType tUpper = (IntersectionType)t.getUpperBound();
    assertSame(TypeKind.INTERSECTION, tUpper.getKind());

    // t's upper bound is an intersection type, one of whose components is Integer; it is therefore assignable to integer
    assertTrue(tes.assignable(tUpper, integer));
    assertTrue(tes.assignable(tUpper, serializable));

    // t is the *payload*; number is the *receiver*; T is assignable to Number (by transitivity)
    assertTrue(tes.assignable(t, number));

    // t and s are not assignable in either direction
    assertFalse(tes.assignable(t, s));
    assertFalse(tes.assignable(s, t));

    // r is the *payload*; s is the *receiver*; R is assignable to S (it extends it)
    assertTrue(tes.assignable(r, s));
  }

  @Test
  final void testAssignabilityOfWildcard() {
    final DeclaredType string = tes.declaredType("java.lang.String");
    final WildcardType qExtendsString = tes.wildcardType(string, null);
    final DeclaredType extendsBound = (DeclaredType)qExtendsString.getExtendsBound();
    assertSame(TypeKind.DECLARED, extendsBound.getKind());
    assertNotSame(string, extendsBound); // or could be; can't rely on it
    assertEquals("java.lang.String", ((TypeElement)extendsBound.asElement()).getQualifiedName().toString());
    assertTrue(tes.sameType(string, extendsBound));
    assertEquals(string, extendsBound);
    
    // Perhaps these assertions are surprising but they are correct.
    assertFalse(tes.assignable(string, qExtendsString));
    assertFalse(tes.assignable(qExtendsString, string));
  }

}
