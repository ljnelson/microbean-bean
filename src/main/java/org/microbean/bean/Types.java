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

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

import static java.lang.constant.ConstantDescs.BSM_INVOKE;

import static java.util.HashSet.newHashSet;

import static java.util.stream.Stream.concat;

import static org.microbean.lang.ConstantDescs.CD_TypeAndElementSource;

public class Types implements Constable {


  /*
   * Instance fields.
   */


  private final Comparator<TypeMirror> c;

  private final TypeAndElementSource tes;


  /*
   * Constructors.
   */


  public Types(final TypeAndElementSource tes) {
    super();
    this.tes = Objects.requireNonNull(tes, "tes");
    this.c = new SpecializationComparator();
  }


  /*
   * Instance methods.
   */


  /**
   * Returns an {@link Optional} housing a {@link ConstantDesc} that represents this {@link Types}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>The default implementation of this method relies on the presence of a {@code public} constructor that accepts a
   * single {@link TypeAndElementSource}-typed argument.</p>
   *
   * <p>The {@link Optional} returned by an invocation of this method may be, and often will be, {@linkplain
   * Optional#isEmpty() empty}.</p>
   *
   * @return an {@link Optional} housing a {@link ConstantDesc} that represents this {@link Types}; never {@code null}
   *
   * @see Constable#describeConstable()
   */
  @Override // Constable
  public Optional<? extends ConstantDesc> describeConstable() {
    return (this.tes instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty())
      .map(tesDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                             MethodHandleDesc.ofConstructor(ClassDesc.of(this.getClass().getName()),
                                                                            CD_TypeAndElementSource),
                                             tesDesc));
  }

  public final DeclaredType objectType() {
    return this.tes.declaredType("java.lang.Object");
  }

  public final List<TypeMirror> supertypes(final TypeMirror t) {
    return this.supertypes(t, Types::returnTrue);
  }

  public final List<TypeMirror> supertypes(final TypeMirror t, final Predicate<? super TypeMirror> p) {
    final ArrayList<TypeMirror> nonInterfaceTypes = new ArrayList<>(7); // arbitrary size
    final ArrayList<TypeMirror> interfaceTypes = new ArrayList<>(17); // arbitrary size
    supertypes(t, p, nonInterfaceTypes, interfaceTypes, newHashSet(13)); // arbitrary size
    nonInterfaceTypes.trimToSize();
    interfaceTypes.trimToSize();
    return
      concat(nonInterfaceTypes.stream(), // non-interface supertypes are already sorted from most-specific to least
             interfaceTypes.stream().sorted(this.c)) // have to sort interfaces because you can extend them in any order
      .toList();
  }

  private final void supertypes(final TypeMirror t,
                                final Predicate<? super TypeMirror> p,
                                final ArrayList<? super TypeMirror> nonInterfaceTypes,
                                final ArrayList<? super TypeMirror> interfaceTypes,
                                final Set<? super String> seen) {
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
    return n == null || n.isEmpty() ? name(qn.getSimpleName()) : name(n);
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
        // t is a subtype of s; s is a proper supertype of t
        return -1;
      } else if (tes.subtype(s, t)) {
        // s is a subtype of t; t is a proper supertype of s
        return 1;
      } else {
        return name(t).compareTo(name(s));
      }
    }

  }

}
