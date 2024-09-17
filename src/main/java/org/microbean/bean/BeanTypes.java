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

import java.lang.System.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import java.util.concurrent.ConcurrentHashMap;

import java.util.function.Predicate;

import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import org.microbean.lang.TypeAndElementSource;

import static java.lang.System.Logger.Level.WARNING;

import static java.util.HashSet.newHashSet;

import static java.util.stream.Stream.concat;

public final class BeanTypes {

  private static final Logger LOGGER = System.getLogger(BeanTypes.class.getName());

  private final Map<TypeMirror, List<TypeMirror>> beanTypes;

  private final Comparator<TypeMirror> c;

  private final TypeAndElementSource tes;

  public BeanTypes(final TypeAndElementSource tes) {
    super();
    this.tes = Objects.requireNonNull(tes, "tes");
    this.c = new SpecializationComparator();
    this.beanTypes = new ConcurrentHashMap<>();
  }

  public final void clearCaches() {
    this.beanTypes.clear();
  }

  public final List<TypeMirror> beanTypes(final TypeMirror t) {
    return switch (t.getKind()) {
      // https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#producer_field_types
      // https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#producer_method_types
    case ARRAY -> this.beanTypes.computeIfAbsent(t, t0 -> this.legalBeanType(t) ? List.of(t0, tes.declaredType("java.lang.Object")) : List.of());
    case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> this.beanTypes.computeIfAbsent(t, t0 -> List.of(t0, tes.declaredType("java.lang.Object")));
    case DECLARED, TYPEVAR -> this.beanTypes.computeIfAbsent(t, t0 -> supertypes(t0, this::legalBeanType));
    default -> {
      assert !this.legalBeanType(t);
      yield List.of();
    }
    };
  }

  public final boolean legalBeanType(final TypeMirror t) {
    // https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0.html#legal_bean_types
    // https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0.html#assignable_parameters
    return switch (t.getKind()) {

    // "A bean type may be an array type."
    //
    // "However, some Java types are not legal bean types: [...] An array type whose component type is not a legal bean
    // type"
    case ARRAY -> {
      if (!this.legalBeanType(((ArrayType)t).getComponentType())) { // note recursion
        if (LOGGER.isLoggable(WARNING)) {
          LOGGER.log(WARNING, t + " has a component type that is an illegal bean type (" + ((ArrayType)t).getComponentType() + ")");
        }
        yield false;
      }
      yield true;
    }

    // "A bean type may be a primitive type. Primitive types are considered to be identical to their corresponding
    // wrapper types in java.lang."
    case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> true;

    // "A bean type may be a parameterized type with actual [see below] type parameters [arguments] and type variables."
    //
    // "However, some Java types are not legal bean types: [...] A parameterized type that contains [see below] a
    // wildcard type parameter [argument] is not a legal bean type."
    //
    // Some ink has been spilled on what it means for a "parameterized" (generic) type to "contain" a "wildcard type
    // parameter [argument]" (https://issues.redhat.com/browse/CDI-502). Because it turns out that "actual type"
    // apparently means, among other things, a non-wildcard type, it follows that *no* wildcard type argument appearing
    // *anywhere* in a bean type is permitted. Note that the definition of "actual type" does not appear in the CDI
    // specification, but only in a closed JIRA issue
    // (https://issues.redhat.com/browse/CDI-502?focusedId=13036118&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-13036118).
    //
    // This still seems way overstrict to me but there you have it.
    case DECLARED -> {
      for (final TypeMirror typeArgument : ((DeclaredType)t).getTypeArguments()) {
        if (typeArgument.getKind() != TypeKind.TYPEVAR && !this.legalBeanType(typeArgument)) { // note recursion
          if (LOGGER.isLoggable(WARNING)) {
            LOGGER.log(WARNING, t + " has a type argument that is an illegal bean type (" + typeArgument + ")");
          }
          yield false;
        }
      }
      yield true;
    }

    // "A type variable is not a legal bean type." (Nothing else is either.)
    default -> {
      if (LOGGER.isLoggable(WARNING)) {
        LOGGER.log(WARNING, t + " is an illegal bean type");
      }
      yield false;
    }
    };
  }

  final List<TypeMirror> supertypes(final TypeMirror t) {
    return this.supertypes(t, BeanTypes::returnTrue);
  }

  final List<TypeMirror> supertypes(final TypeMirror t, final Predicate<? super TypeMirror> p) {
    final ArrayList<TypeMirror> nonInterfaceTypes = new ArrayList<>(17); // 17 == arbitrary
    final ArrayList<TypeMirror> interfaceTypes = new ArrayList<>(17); // 17 == arbitrary
    supertypes(t, p, nonInterfaceTypes, interfaceTypes, newHashSet(13));
    nonInterfaceTypes.trimToSize();
    interfaceTypes.trimToSize();
    return
      concat(nonInterfaceTypes.stream().sorted(this.c),
             interfaceTypes.stream().sorted(this.c))
      .toList();
  }

  private final void supertypes(final TypeMirror t,
                                final Predicate<? super TypeMirror> p,
                                final ArrayList<TypeMirror> nonInterfaceTypes,
                                final ArrayList<TypeMirror> interfaceTypes,
                                final Set<String> seen) {
    if (seen.add(name(t))) {
      if (p.test(t)) {
        if (isInterface(t)) {
          interfaceTypes.add(t); // reflexive
        } else {
          nonInterfaceTypes.add(t); // reflexive
        }
      }
      for (final TypeMirror directSupertype : tes.directSupertypes(t)) {
        this.supertypes(directSupertype, p, nonInterfaceTypes, interfaceTypes, seen); // note recursion
      }
    }
  }


  /*
   * Static methods.
   */


  static final String name(final TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY -> name(((ArrayType)t).getComponentType()) + "[]";
    case BOOLEAN -> "boolean";
    case BYTE -> "byte";
    case CHAR -> "char";
    case DECLARED -> name(((DeclaredType)t).asElement());
    case DOUBLE -> "double";
    case FLOAT -> "float";
    case INT -> "int";
    case INTERSECTION -> {
      final java.util.StringJoiner sj = new java.util.StringJoiner("&");
      for (final TypeMirror bound : ((IntersectionType)t).getBounds()) {
        sj.add(name(bound));
      }
      yield sj.toString();
    }
    case LONG -> "long";
    case SHORT -> "short";
    case TYPEVAR -> name(((TypeVariable)t).asElement());
    default -> t.toString();
    };
  }

  static final String name(final Element e) {
    return e instanceof QualifiedNameable qn ? name(qn) : name(e.getSimpleName());
  }

  private static final String name(final QualifiedNameable qn) {
    final CharSequence n = qn.getQualifiedName();
    return n.isEmpty() ? name(qn.getSimpleName()) : name(n);
  }

  private static final String name(final CharSequence cs) {
    return cs instanceof String s ? s : cs.toString();
  }

  private static final boolean isInterface(final TypeMirror t) {
    return t.getKind() == TypeKind.DECLARED && isInterface(((DeclaredType)t).asElement());
  }

  private static final boolean isInterface(final Element e) {
    return e.getKind().isInterface();
  }

  private static final <T> boolean returnTrue(final T ignored) {
    return true;
  }


  /*
   * Inner and nested classes.
   */


  private final class SpecializationComparator implements Comparator<TypeMirror> {

    private SpecializationComparator() {
      super();
    }

    @Override
    public final int compare(final TypeMirror t, final TypeMirror s) {
      if (t == s) {
        return 0;
      } else if (t == null) {
        return 1; // nulls right
      } else if (s == null) {
        return -1; // nulls right
      } else if (tes.sameType(t, s)) {
        return 0;
      } else if (tes.subtype(t, s)) {
        // t is a subtype of s
        return -1;
      } else if (tes.subtype(s, t)) {
        // s is a subtype of t
        return 1;
      }
      return name(t).compareTo(name(s));
    }

  }

}
