/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023–2024 microBean™.
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

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import java.lang.System.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import java.util.function.Predicate;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.microbean.constant.Constables;

import org.microbean.lang.Equality;
import org.microbean.lang.SameTypeEquality;
import org.microbean.lang.TypeAndElementSource;

import org.microbean.lang.type.DelegatingTypeMirror;

import org.microbean.lang.visitor.Visitors;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_int;
import static java.lang.constant.ConstantDescs.CD_List;

import static java.lang.System.Logger.Level.WARNING;

import static org.microbean.lang.ConstantDescs.CD_Equality;
import static org.microbean.lang.ConstantDescs.CD_TypeAndElementSource;

/**
 * A list of {@link DelegatingTypeMirror}s that are both reference types and {@linkplain
 * #legalBeanType(DelegatingTypeMirror) legal bean types}, sorted in a specific way.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #types()
 *
 * @see #classes()
 *
 * @see #interfaces()
 *
 * @see #arrays()
 *
 * @see #typeVariables()
 *
 * @deprecated See {@link BeanTypes} which will eventually replace this class.
 */
@Deprecated(forRemoval = true)
public final class BeanTypeList extends ReferenceTypeList {

  private static final Logger LOGGER = System.getLogger(BeanTypeList.class.getName());

  public BeanTypeList(final Collection<? extends TypeMirror> types,
                      final TypeAndElementSource tes) {
    this(types, tes, new SameTypeEquality(tes));
  }

  public BeanTypeList(final Collection<? extends TypeMirror> types,
                      final TypeAndElementSource tes,
                      final Equality equality) {
    super(types, BeanTypeList::legalBeanType, tes, equality);
  }

  /**
   * Creates a new, valid {@link BeanTypeList} when called from the {@link #describeConstable()} method, <strong>and
   * creates an invalid {@link BeanTypeList} in all other cases</strong>.
   *
   * <p>No validation of any kind of any argument is performed.</p>
   *
   * @param types a {@link List} of {@link DelegatingTypeMirror}, sorted appropriately
   *
   * @param classesIndex the zero-based index identifying where classes exist in the {@code types} list
   *
   * @param arraysIndex the zero-based index identifying where arrays exist in the {@link types} list
   *
   * @param interfacesIndex the zero-based index identifying where interfaces exist in the {@link types} list
   *
   * @param equality an {@link Equality}
   *
   * @param tes a {@link TypeAndElementSource}
   *
   * @deprecated This constructor is for use only by the {@link #describeConstable()} method and for no other purpose.
   */
  // Deliberately unvalidated constructor for use by describeConstable() only.
  @Deprecated
  public BeanTypeList(final List<DelegatingTypeMirror> types,
                      final int classesIndex,
                      final int arraysIndex,
                      final int interfacesIndex,
                      final Equality equality,
                      final TypeAndElementSource tes) {
    super(types, classesIndex, arraysIndex, interfacesIndex, equality, tes);
  }

  /**
   * Returns an {@link Optional} containing a {@link DynamicConstantDesc} describing this {@link BeanTypeList}.
   *
   * @return an {@link Optional} containing a {@link DynamicConstantDesc} describing this {@link BeanTypeList}; never
   * {@code null}
   */
  @Override // Constable
  public final Optional<DynamicConstantDesc<?>> describeConstable() {
    return Constables.describeConstable(this.types(), this.tes::describeConstable)
      .flatMap(typesDesc -> (this.tes instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty())
               .flatMap(tesDesc -> this.equality.describeConstable()
                        .map(equalityDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                                    MethodHandleDesc.ofConstructor(ClassDesc.of(this.getClass().getName()),
                                                                                                   CD_List,
                                                                                                   CD_int,
                                                                                                   CD_int,
                                                                                                   CD_int,
                                                                                                   CD_Equality,
                                                                                                   CD_TypeAndElementSource),
                                                                    typesDesc,
                                                                    this.classesIndex,
                                                                    this.arraysIndex,
                                                                    this.interfacesIndex,
                                                                    equalityDesc,
                                                                    tesDesc))));
  }


  /*
   * Static methods.
   */


  public static final BeanTypeList closure(final TypeMirror t,
                                           final TypeAndElementSource tes) {
    return closure(t, new Visitors(tes));
  }

  /**
   * Returns a non-{@code null} {@link BeanTypeList} containing the set of types {@code t} bears.
   *
   * @param t the {@link TypeMirror} whose type closure will be returned as a {@link BeanTypeList}; must not be {@code
   * null}; must be either a {@linkplain TypeKind#DECLARED declared type}, an {@linkplain TypeKind#INTERSECTION
   * intersection type} or a {@linkplain TypeKind#TYPEVAR type variable}
   *
   * @param visitors a {@link Visitors} providing access to a {@linkplain Visitors#typeClosureVisitor() type closure
   * visitor}; must not be {@code null}
   *
   * @return a non-{@code null} {@link BeanTypeList}
   *
   * @exception NullPointerException if {@code t} or {@code visitors} is {@code null}
   *
   * @exception IllegalArgumentException if {@code t} is neither a {@linkplain TypeKind#DECLARED declared type}, an
   * {@linkplain TypeKind#INTERSECTION intersection type} nor a {@linkplain TypeKind#TYPEVAR type variable}
   *
   * @see Visitors#typeClosureVisitor()
   */
  public static final BeanTypeList closure(final TypeMirror t,
                                           final Visitors visitors) {
    final TypeAndElementSource tes = visitors.typeAndElementSource();
    final Equality ehc = new SameTypeEquality(tes);
    final DelegatingTypeMirror dt = DelegatingTypeMirror.of(t, tes, ehc);
    // CDI has very strange rules for arrays and primitives; we need to account for them here.
    // TODO: even though we're accounting for primitive types et al. here the superclass is (incorrectly, I guess) called *Reference*TypeList
    final Collection<? extends TypeMirror> types = switch (dt.getKind()) {
    case ARRAY, BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> List.of(dt, DelegatingTypeMirror.of(tes.declaredType("java.lang.Object"), tes, ehc));
    default -> visitors.typeClosureVisitor().visit(dt).toList();
    };
    return new BeanTypeList(types, tes, ehc);
  }

  /**
   * Returns {@code true} if and only if {@code t} is non-{@code null} and a <a
   * href="https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0.html#legal_bean_types">legal bean type</a>.
   *
   * <p>Very loosely speaking, a legal bean type is either:</p>
   *
   * <ul>
   *
   * <li>an array type whose component type is a legal bean type</li>
   *
   * <li>a declared type that is neither a type variable nor a parameterized type that contains, at any level, a
   * wildcard type argument</li>
   *
   * <li>a primitive type</li>
   *
   * </ul>
   *
   * <p>All other types are illegal bean types.</p>
   *
   * @param t the {@link TypeMirror} in question; must not be {@code null}
   *
   * @return {@code true} if and only if {@code t} is non-{@code null} and a <a
   * href="https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0.html#legal_bean_types">legal bean type</a>;
   * {@code false} otherwise
   *
   * @exception NullPointerException if {@code t} is {@code null}
   */
  public static final boolean legalBeanType(DelegatingTypeMirror t) {
    // https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0.html#legal_bean_types
    // https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0.html#assignable_parameters
    return switch (t.getKind()) {

    // "A bean type may be an array type."
    //
    // "However, some Java types are not legal bean types: [...] An array type whose component type is not a legal bean
    // type"
    case ARRAY -> {
      if (legalBeanType(t.getComponentType())) { // note recursion
        yield true;
      }
      if (LOGGER.isLoggable(WARNING)) {
        LOGGER.log(WARNING, t + " has a component type that is an illegal bean type (" + t.getComponentType() + ")");
      }
      yield false;
    }

    // "A bean type may be a primitive type. Primitive types are considered to be identical to their corresponding
    // wrapper types in java.lang."
    case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> true;

    // "However, some Java types are not legal bean types: [...] A parameterized type that contains [see below] a
    // wildcard type parameter [argument] is not a legal bean type."
    //
    // Some ink has been spilled on what it means to "contain" a "wildcard type parameter [argument]"
    // (https://issues.redhat.com/browse/CDI-502). Because it turns out that "actual type" apparently means, among other
    // things, a non-wildcard type, it follows that *no* wildcard type argument appearing *anywhere* in a bean type is
    // permitted. Note that the definition of "actual type" does not appear in the CDI specification, but only in a
    // closed JIRA issue
    // (https://issues.redhat.com/browse/CDI-502?focusedId=13036118&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-13036118).
    //
    // This still seems way overstrict to me but there you have it.
    case DECLARED -> {
      for (final DelegatingTypeMirror typeArgument : t.getTypeArguments()) {
        if (legalBeanType(typeArgument)) { // note recursion
          continue;
        }
        if (LOGGER.isLoggable(WARNING)) {
          LOGGER.log(WARNING, t + " has a type argument that is an illegal bean type (" + typeArgument + ")");
        }
        yield false;
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
}
