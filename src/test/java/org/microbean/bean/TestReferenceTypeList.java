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

import java.io.Serializable;

import java.lang.constant.ClassDesc;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentMap;

import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import jakarta.enterprise.inject.Produces;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import jakarta.inject.Singleton;

import org.jboss.weld.util.reflection.HierarchyDiscovery;

import org.junit.jupiter.api.Test;

import org.microbean.lang.Lang;
import org.microbean.lang.SameTypeEquality;
import org.microbean.lang.TypeAndElementSource;

import org.microbean.lang.type.DelegatingTypeMirror;

import org.microbean.lang.visitor.Visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.microbean.lang.Lang.unwrap;

final class TestReferenceTypeList {

  private TestReferenceTypeList() {
    super();
  }

  @Test
  final void testInterfacesOnly() {
    final TypeMirror s = Lang.declaredType("java.io.Serializable");
    final ReferenceTypeList rtl = ReferenceTypeList.closure(s, Lang.typeAndElementSource());
    final List<? extends TypeMirror> types = rtl.types();
    System.out.println("*** types: " + types);

  }

  @Test
  final <T> void testSorting() {
    final TypeAndElementSource tes = Lang.typeAndElementSource();
    final List<TypeMirror> l = new ArrayList<>();
    final TypeMirror object = tes.declaredType(Object.class);
    assertTrue(object instanceof DelegatingTypeMirror);
    l.add(object);
    final TypeMirror mapStringString = tes.declaredType(null, tes.typeElement(Map.class), tes.declaredType(String.class), tes.declaredType(String.class));
    assertTrue(mapStringString instanceof DelegatingTypeMirror);
    l.add(mapStringString);
    final TypeMirror concurrentMapStringString = tes.declaredType(null, tes.typeElement(ConcurrentMap.class), tes.declaredType(String.class), tes.declaredType(String.class));
    assertTrue(concurrentMapStringString instanceof DelegatingTypeMirror);
    l.add(concurrentMapStringString);
    final TypeMirror objectArray = tes.arrayType(Object[].class);
    assertTrue(objectArray instanceof DelegatingTypeMirror);
    l.add(objectArray);
    final ReferenceTypeList rtl = new ReferenceTypeList(l, tes, new SameTypeEquality(tes));
    List<? extends TypeMirror> types = rtl.types();
    assertEquals(4, types.size());
    assertSame(object, types.get(0));
    assertSame(objectArray, types.get(1));
    assertSame(concurrentMapStringString, types.get(2));
    assertSame(mapStringString, types.get(3));
    types = rtl.classes();
    assertEquals(1, types.size());
    assertSame(object, types.get(0));
    types = rtl.arrays();
    assertEquals(1, types.size());
    assertSame(objectArray, types.get(0));
    types = rtl.typeVariables();
    assertTrue(types.isEmpty());
    types = rtl.interfaces();
    assertEquals(2, types.size());
    assertSame(concurrentMapStringString, types.get(0));
    assertSame(mapStringString, types.get(1));
  }

  @Test
  final <T extends Integer> void testWeldClosure() throws NoSuchMethodException {
    // Weld's/CDI's notion of a type closure, the backbone of CDI, includes operating on array types. Javac's
    // Types#typeClosure(TypeMirror) does not. Consider a bean that is a producer method that returns Integer[] and an
    // injection point that wants a Serializable.
    //
    // ...oh, but actually this is complicated. See
    // https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#producer_method_types, where it is explicitly
    // stated that "If a return type is primitive or is a Java array type, the unrestricted set of bean types contains
    // exactly two types: the method return type and java.lang.Object.". I don't know why, for example, Serializable and
    // Cloneable are omitted from the set of types. Or, in the case of Integer[], why Number[] is not part of the set of
    // types.
    //
    // Interestingly, Weld's HierarchyDiscovery does not implement this described behavior. In fact, it implements a strange set of behaviors that neither conforms to CDI nor the Java Language Specification:
    Set<Type> closure = new HierarchyDiscovery(Integer[].class).getTypeClosure();
    assertTrue(closure.size() == 4);
    assertTrue(closure.contains(Integer[].class)); // reflexive
    // (Where is Number[]?)
    // (Where is Object[]?)
    assertTrue(closure.contains(Serializable.class));
    assertTrue(closure.contains(Cloneable.class));
    assertTrue(closure.contains(Object.class));

    // Also note above that Number[] and Object[] are not part of the hierarchy? Why not? How odd.

    // Whatever this is, it is not described by the Java Language Specification (see
    // https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.10.1). The direct supertypes of int.class
    // should be long.class. The direct supertypes of long.class are float.class. The direct supertypes of float.class
    // are double.class. Shouldn't those show up? What *is* a type closure, exactly, anyway? It turns out Weld does not
    // know.
    assertNull(int.class.getSuperclass());
    closure = new HierarchyDiscovery(int.class).getTypeClosure();
    assertEquals(1, closure.size()); // er what?
    assertSame(int.class, closure.iterator().next());

    // HierarchyDiscovery also claims it is reflexive, but it is not when it is supplied with a type variable:
    closure =
      new HierarchyDiscovery(this.getClass().getDeclaredMethod("testWeldClosure").getTypeParameters()[0]).getTypeClosure();
    assertTrue(closure.isEmpty());

    @Singleton
    final class Foo {

      @Produces
      final Integer[] produceIntegerArray() {
        return new Integer[] { 42 };
      }

    };

    // Among other things, this means that a producer method with a return type of Integer[] is not assignable to an
    // injection point of Number[]. Or Object[]. Or Cloneable. Or Serializable. Java permits all of these things. See
    // https://www.eclipse.org/lists/cdi-dev/msg00806.html.
    try (final SeContainer c = SeContainerInitializer.newInstance()
         .disableDiscovery()
         .addBeanClasses(Foo.class) // see below
         .initialize()) {
      final Set<jakarta.enterprise.inject.spi.Bean<?>> beans = c.getBeanManager().getBeans(Number[].class);
      assertTrue(beans.isEmpty());
    }

    // Let's just verify that indeed Weld's HierarchyDiscovery is broken.
    final TypeMirror t = Lang.arrayTypeOf(Lang.declaredType(Integer.class));
    assertTrue(t.getKind() == TypeKind.ARRAY);

    // Number[] is a direct supertype of Integer[]. So Weld misses this.
    List<? extends TypeMirror> sts = Lang.directSupertypes(t);
    assertEquals(1, sts.size());
    assertEquals(sts.get(0), Lang.arrayTypeOf(Lang.declaredType(Number.class)));

    // Object[] is a direct supertype of Number[]. So Weld misses this.
    sts = Lang.directSupertypes(Lang.arrayTypeOf(Lang.declaredType(Number.class)));
    assertEquals(1, sts.size());
    assertEquals(sts.get(0), Lang.arrayTypeOf(Lang.declaredType(Object.class)));

    // The direct supertype of Object[] is a single intersection type.
    sts = Lang.directSupertypes(Lang.arrayTypeOf(Lang.declaredType(Object.class)));
    assertEquals(1, sts.size()); // ...because it's an intersection type
    final IntersectionType i = (IntersectionType)sts.get(0);
    assertSame(TypeKind.INTERSECTION, i.getKind());

    // The bounds of that intersection type must include Cloneable and Serializable. So Weld misses this.
    final List<? extends TypeMirror> bounds = i.getBounds();
    assertEquals(2, bounds.size());
    assertTrue(bounds.contains(Lang.declaredType(Cloneable.class)));
    assertTrue(bounds.contains(Lang.declaredType(Serializable.class)));

    // The direct supertypes of any intersection type are simply its bounds.
    sts = Lang.directSupertypes(i);
    assertEquals(2, sts.size());
    assertTrue(sts.contains(Lang.declaredType(Cloneable.class)));
    assertTrue(sts.contains(Lang.declaredType(Serializable.class)));

    // The supertype of Cloneable is Object.
    sts = Lang.directSupertypes(Lang.declaredType(Cloneable.class));
    assertEquals(1, sts.size());
    assertEquals(Lang.declaredType(Object.class), sts.get(0));

    // The supertype of Serializable is Object.
    sts = Lang.directSupertypes(Lang.declaredType(Serializable.class));
    assertEquals(1, sts.size());
    assertEquals(Lang.declaredType(Object.class), sts.get(0));
  }
  
  @Test
  final void testClosure() {
    final TypeAndElementSource tes = Lang.typeAndElementSource();
    final TypeMirror string = tes.declaredType(String.class);
    final ReferenceTypeList rtl = ReferenceTypeList.closure(string, tes);
    final List<? extends TypeMirror> types = rtl.types();
    assertEquals(7, types.size());
    assertSame(string, types.get(0));
    assertSame(unwrap(tes.declaredType(Object.class)), unwrap(types.get(1)));
    assertSame(unwrap(tes.declaredType(java.io.Serializable.class)), unwrap(types.get(2)));
    assertSame(unwrap(tes.declaredType(CharSequence.class)), unwrap(types.get(3)));
    assertTrue(tes.sameType(unwrap(tes.declaredType(null, tes.typeElement(Comparable.class), tes.declaredType(String.class))), unwrap(types.get(4))));
    assertSame(unwrap(tes.declaredType(java.lang.constant.Constable.class)), unwrap(types.get(5)));
    assertSame(unwrap(tes.declaredType(java.lang.constant.ConstantDesc.class)), unwrap(types.get(6)));
  }

  @Test
  final <T extends ClassDesc, S extends String> void testTypeVariables() throws IllegalAccessException, NoSuchMethodException {
    final Visitors visitors = new Visitors(Lang.typeAndElementSource());
    final TypeVariable t = Lang.typeVariable(this.getClass().getDeclaredMethod("testTypeVariables"), "T");
    final TypeVariable s = Lang.typeVariable(this.getClass().getDeclaredMethod("testTypeVariables"), "S");

    System.out.println("*** supertype of " + t + ": " + visitors.supertypeVisitor().visit(t));
    System.out.println("*** supertype of " + s + ": " + visitors.supertypeVisitor().visit(s));
    ReferenceTypeList rtl = ReferenceTypeList.closure(t, visitors);
    System.out.println("*** rtl types: " + rtl.types());
    rtl = ReferenceTypeList.closure(s, visitors);
    System.out.println("*** rtl types: " + rtl.types());
  }

}
