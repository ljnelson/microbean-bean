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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.function.Predicate;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.QualifiedNameable;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

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


  // "Condenses" t, according to the following rules.
  //
  // If t is null, returns List.of().
  //
  // If t is a type variable, returns the result of condensing its upper bound (thus eliminating the type
  // variable). Recall that the upper bound of a type variable may be an intersection type.
  //
  // If t is an intersection type, returns the result of condensing its bounds (thus eliminating the intersection type).
  //
  // If t is a wildcard type, returns the result of condensing either its super or extends bound (thus eliminating the
  // wildcard type). The result of condensing a wildcard type that declares neither bound (?) is the result of
  // condensing the type declared by java.lang.Object. If t declares both an extends and a super bound, an
  // IllegalArgumentException is thrown.
  //
  // In all other cases returns List.of(t).
  //
  // Note especially this is not type erasure, though it has some similarities.
  //
  // See #condense(List) below.
  public final List<? extends TypeMirror> condense(final TypeMirror t) {
    return t == null ? List.of() : switch (t.getKind()) {
    case INTERSECTION -> this.condense(((IntersectionType)t).getBounds());
    case TYPEVAR      -> this.condense(((TypeVariable)t).getUpperBound());
    case WILDCARD     -> {
      final WildcardType w = (WildcardType)t;
      final TypeMirror s = w.getSuperBound();
      final TypeMirror e = w.getExtendsBound();
      if (s == null) {
        yield e == null ? List.of(this.tes.declaredType("java.lang.Object")) : this.condense(e);
      } else if (e == null) {
        yield this.condense(s);
      }
      throw new IllegalArgumentException("t: " + t);
    }
    default           -> List.of(t); // t's bounds are defined solely by itself
    };
  }

  // If ts is empty, returns an empty unmodifiable List.
  //
  // If ts consists of a single element, returns the result of condensing t (see #condense(TypeMirror) above).
  //
  // Otherwise creates a new list, condenses each element of ts, and adds each element of the condensation to the new
  // list, and returns an unmodifiable view of the new list.
  //
  // Note that deliberately duplicates are not eliminated from the new list.
  public final  List<? extends TypeMirror> condense(final List<? extends TypeMirror> ts) {
    final int size = ts.size();
    if (size == 0) {
      return List.of();
    }
    // We take care not to allocate/copy new lists needlessly.
    ArrayList<TypeMirror> newBounds = null;
    for (int i = 0; i < size; i++) {
      final TypeMirror t = ts.get(i);
      switch (t.getKind()) {
      case INTERSECTION:
      case TYPEVAR:
      case WILDCARD:
        // These are the only types where condensing does anything. In these cases alone we need to make a new list.
        if (newBounds == null) {
          newBounds = new ArrayList<>(size * 2);
          if (i > 0) {
            // All ts up to this point have been non-condensable types, so catch up and add them as-is.
            newBounds.addAll(ts.subList(0, i));
          }
        }
        newBounds.addAll(condense(t));
        break;
      default:
        if (newBounds != null) {
          newBounds.add(t);
        }
        break;
      }
    }
    if (newBounds == null) {
      return ts;
    }
    newBounds.trimToSize();
    return Collections.unmodifiableList(newBounds);
  }

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
  
  // Is e a TypeElement representing java.lang.Object?
  public final boolean isJavaLangObject(final Element e) {
    // This method is non-static to permit changing its internal implementation, which might rely on the
    // TypeAndElementSource in the future.
    return
      e.getKind() == ElementKind.CLASS &&
      ((QualifiedNameable)e).getQualifiedName().contentEquals("java.lang.Object");
  }

  // Is t a DeclaredType whose asElement() method returns an Element representing java.lang.Object?
  public final boolean isJavaLangObject(final TypeMirror t) {
    return
      t.getKind() == TypeKind.DECLARED &&
      isJavaLangObject(((DeclaredType)t).asElement());
  }
  
  public final DeclaredType javaLangObjectType() {
    return this.tes.declaredType("java.lang.Object");
  }

  // Get the raw type yielded by t, assuming t is the sort of type that can yield a raw type.
  //
  // An array type with a parameterized element type can yield a raw type.
  //
  // A declared type that is parameterized can yield a raw type.
  //
  // No other type yields a raw type.
  //
  // t cannot be null.
  public final TypeMirror rawType(final TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY -> {
      final TypeMirror et = elementType(t);
      if (!parameterized(et)) {
        throw new IllegalArgumentException("t is an array type whose element type is not parameterized so cannot yield a raw type");
      }
      yield this.typeAndElementSource().arrayTypeOf(this.typeAndElementSource().erasure(et));
    }
    case DECLARED -> {
      if (!parameterized(t)) {
        throw new IllegalArgumentException("t is a declared type that is not parameterized so cannot yield a raw type");
      }
      yield this.typeAndElementSource().erasure(t);
    }
    default -> throw new IllegalArgumentException("t is a " + t.getKind() + " type and so cannot yield a raw type");
    };
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

  public final TypeAndElementSource typeAndElementSource() {
    return this.tes;
  }


  /*
   * Static methods.
   */


  // Returns the element type of t.
  //
  // The element type of an array type is the element type of its component type.
  //
  // The element type of every other kind of type is the type itself.
  public static final TypeMirror elementType(final TypeMirror t) {
    return t.getKind() == TypeKind.ARRAY ? elementType(((ArrayType)t).getComponentType()) : t;
  }

  // Is t the usage of a generic class, i.e. a usage (whether raw or parameterized) of a generic class declaration?
  //
  // t is deemed to be generic if it is a declared type whose defining element (its type declaration) is generic.
  //
  // Array types are never generic.
  public static final boolean generic(final TypeMirror t) {
    return
      t.getKind() == TypeKind.DECLARED &&
      generic(((DeclaredType)t).asElement());
  }

  // Does e represent a generic declaration?
  //
  // A declaration is generic if it declares one or more type parameters.
  //
  // Since an annotation interface cannot declare type parameters, it follows that TypeElements representing annotation
  // instances are never generic.
  private static final boolean generic(final Element e) {
    return switch (e.getKind()) {
    case CLASS, CONSTRUCTOR, ENUM, INTERFACE, METHOD, RECORD -> !((Parameterizable)e).getTypeParameters().isEmpty();
    default -> false;
    };
  }
  
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

  // Is t a parameterized type (and not a raw type) strictly according to the rules of the Java Language Specification?
  //
  // A type is parameterized if it is a declared type with a non-empty list of type arguments. No other type is
  // parameterized.
  public static final boolean parameterized(final TypeMirror t) {
    return
      t.getKind() == TypeKind.DECLARED &&
      !((DeclaredType)t).getTypeArguments().isEmpty();
  }
  
  private static final boolean isInterface(final TypeMirror t) {
    return t.getKind() == TypeKind.DECLARED && isInterface(((DeclaredType)t).asElement());
  }

  private static final boolean isInterface(final Element e) {
    return e.getKind().isInterface();
  }

  // Is t a raw type, following the rules of the Java Language Specification, not CDI?
  //
  // A raw type is either "the erasure of a parameterized type" (so List<String>'s raw type is List, clearly not
  // List<?>, and not List<E>) or "an array type whose element type is a raw type" (so List<String>[]'s raw type is
  // List[]). (String is not a raw type; its element defines no type parameters.)
  //
  // No other type is a raw type.
  //
  // CDI gets confused and uses "raw" in different senses. The sense used here is that of the Java Language
  // Specification.
  public static final boolean raw(final TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY -> raw(elementType((ArrayType)t));
    case DECLARED -> generic(t) && ((DeclaredType)t).getTypeArguments().isEmpty();
    default -> false;
    };
  }
  
  private static final <T> boolean returnTrue(final T ignored) {
    return true;
  }

  // Can t *yield* a raw type?
  //
  // We say that to yield a raw type, t must be either:
  //
  // * a declared type with at least one type argument
  // * an array type with a parameterized element type
  public static final boolean yieldsRawType(final TypeMirror t) {
    return parameterized(t) || t.getKind() == TypeKind.ARRAY && parameterized(elementType(t));
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
