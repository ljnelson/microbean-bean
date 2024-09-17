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

import java.lang.constant.Constable;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import java.util.function.Predicate;

import javax.lang.model.type.TypeMirror;

import org.microbean.constant.Constables;

import org.microbean.lang.SameTypeEquality;
import org.microbean.lang.TypeAndElementSource;

import org.microbean.lang.type.DelegatingTypeMirror;

import org.microbean.qualifier.NamedAttributeMap;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_Collection;
import static java.lang.constant.ConstantDescs.FALSE;
import static java.lang.constant.ConstantDescs.TRUE;
import static java.lang.constant.DirectMethodHandleDesc.Kind.STATIC;

import static org.microbean.bean.ConstantDescs.CD_TypeMatcher;
import static org.microbean.bean.ConstantDescs.CD_BeanSelectionCriteria;
import static org.microbean.bean.InterceptorBindings.anyInterceptorBinding;
import static org.microbean.bean.Qualifiers.anyQualifier;
import static org.microbean.bean.Qualifiers.defaultQualifier;
import static org.microbean.bean.Qualifiers.defaultQualifiers;

import static org.microbean.lang.ConstantDescs.CD_TypeMirror;

/**
 * An object that represents criteria used to <em>select</em> {@link Bean}s according to rules used to <em>match</em>
 * types and qualifiers.
 *
 * @param typeMatcher a non-{@code null} {@link TypeMatcher} used to determine whether two types {@linkplain
 * TypeMatcher#matches(TypeMirror, TypeMirror) match}
 *
 * @param type a non-{@code null} {@link TypeMirror} representing the kind of {@link Bean} that should be selected
 *
 * @param attributes a (possibly {@code null}) {@link List} of {@link NamedAttributeMap} instances representing
 * qualifiers, interceptor bindings, or both
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #selects(Collection, Collection)
 *
 * @see TypeMatcher
 */
public final record BeanSelectionCriteria(TypeMatcher typeMatcher, // not included in equality/hashcode
                                          TypeMirror type,
                                          List<NamedAttributeMap<?>> attributes)
  implements Constable {


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link BeanSelectionCriteria}.
   *
   * @param typeMatcher a non-{@code null} {@link TypeMatcher} used to determine whether two types {@linkplain
   * TypeMatcher#matches(TypeMirror, TypeMirror) match}
   *
   * @param type a non-{@code null} {@link TypeMirror} representing the kind of {@link Bean} that should be selected
   *
   * @exception NullPointerException if either {@code typeMatcher} or {@code type} is {@code null}
   */
  public BeanSelectionCriteria(final TypeMatcher typeMatcher, final TypeMirror type) {
    this(typeMatcher, type, null);
  }

  /**
   * Creates a new {@link BeanSelectionCriteria}.
   *
   * @param typeMatcher a non-{@code null} {@link TypeMatcher} used to determine whether two types {@linkplain
   * TypeMatcher#matches(TypeMirror, TypeMirror) match}
   *
   * @param type a non-{@code null} {@link TypeMirror} representing the kind of {@link Bean} that should be selected
   *
   * @param attributes a (possibly {@code null}) {@link List} of {@link NamedAttributeMap} instances representing
   * qualifiers, interceptor bindings, or both; if {@code null} then the return value from an invocation of the {@link
   * Qualifiers#defaultQualifiers()} method will be used instead
   *
   * @exception NullPointerException if either {@code typeMatcher} or {@code type} is {@code null}
   */
  public BeanSelectionCriteria {
    final TypeAndElementSource tes = typeMatcher.typeAndElementSource();
    type = validateType(DelegatingTypeMirror.of(type, tes, new SameTypeEquality(tes)));
    attributes = attributes == null ? defaultQualifiers() : List.copyOf(attributes);
  }


  /*
   * Instance methods.
   */


  /**
   * Returns an immutable sublist of this {@link BeanSelectionCriteria}'s {@linkplain #attributes() attributes} that are
   * interceptor bindings.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>This method is idempotent and returns a determinate value.</p>
   *
   * @return an immutable sublist of this {@link BeanSelectionCriteria}'s {@linkplain #attributes() attributes} that are
   * interceptor bindings; never {@code null}
   *
   * @see InterceptorBindings#interceptorBindings(Collection)
   */
  public final List<NamedAttributeMap<?>> interceptorBindings() {
    return InterceptorBindings.interceptorBindings(this.attributes());
  }

  /**
   * Returns an immutable sublist of this {@link BeanSelectionCriteria}'s {@linkplain #attributes() attributes} that are
   * qualifiers.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>This method is idempotent and returns a determinate value.</p>
   *
   * @return an immutable sublist of this {@link BeanSelectionCriteria}'s {@linkplain #attributes() attributes} that are
   * qualifiers; never {@code null}
   *
   * @see Qualifiers#qualifiers(Collection)
   */
  public final List<NamedAttributeMap<?>> qualifiers() {
    return Qualifiers.qualifiers(this.attributes());
  }

  /**
   * Returns {@code true} if an invocation of the {@link #selects(Collection, Collection)} method supplied with the
   * supplied {@link Id}'s {@linkplain BeanTypeList#types() types} and {@linkplain Id#attributes() attributes} returns
   * {@code true}.
   *
   * @param id an {@link Id}; must not be {@code null}
   *
   * @return {@code true} if an invocation of the {@link #selects(Collection, Collection)} method supplied with the
   * supplied {@link Id}'s {@linkplain BeanTypeList#types() types} and {@linkplain Id#attributes() attributes} returns
   * {@code true}; {@code false} in all other cases
   *
   * @exception NullPointerException if {@code id} is {@code null}
   *
   * @see #selects(Collection, Collection)
   *
   * @see Id#types()
   *
   * @see Id#attributes()
   *
   * @see BeanTypeList#types()
   */
  public final boolean selects(final Id id) {
    return this.selects(id.types().types(), id.attributes());
  }

  public final boolean selects(final Bean<?> bean) {
    return this.selects(bean.id());
  }

  public final boolean selects(final TypeMirror type) {
    return this.selects(List.of(type));
  }

  public final boolean selects(final Collection<? extends TypeMirror> types) {
    return this.selects(types, List.of());
  }

  public final boolean selects(final TypeMirror type, final Collection<? extends NamedAttributeMap<?>> attributes) {
    return this.selects(List.of(type), attributes);
  }

  /**
   * Returns {@code true} if an invocation of {@link #selectsQualifiers(Collection)} supplied with the supplied {@code
   * attributes} returns {@code true}, and if an invocation of {@link #selectsInterceptorBindings(Collection)} supplied
   * with the supplied {@code attributes} also returns {@code true}, and if an invocation of {@link
   * #selectsTypeFrom(Collection)} supplied with the supplied {@code types} returns {@code true}.
   *
   * @param types a {@link Collection} of {@link TypeMirror} instances; must not be {@code null}
   *
   * @param attributes a {@link Collection} of {@link NamedAttributeMap} instances representing qualifiers, interceptor
   * bindings or both
   *
   * @return {@code true} if an invocation of {@link #selectsQualifiers(Collection)} supplied with the supplied {@code
   * attributes} returns {@code true}, and if an invocation of {@link #selectsInterceptorBindings(Collection)} supplied
   * with the supplied {@code attributes} also returns {@code true}, and if an invocation of {@link
   * #selectsTypeFrom(Collection)} supplied with the supplied {@code types} returns {@code true}; {@code false} in all
   * other cases
   *
   * @exception NullPointerException if either {@code types} or {@code attributes} is {@code null}
   *
   * @see #selectsInterceptorBindings(Collection)
   *
   * @see #selectsQualifiers(Collection)
   *
   * @see #selectsTypeFrom(Collection)
   */
  public final boolean selects(final Collection<? extends TypeMirror> types,
                               final Collection<? extends NamedAttributeMap<?>> attributes) {
    return this.selectsQualifiers(attributes) && this.selectsInterceptorBindings(attributes) && this.selectsTypeFrom(types);
  }

  public final boolean selectsInterceptorBindings(final Collection<? extends NamedAttributeMap<?>> attributes) {
    final List<? extends NamedAttributeMap<?>> herBindings = InterceptorBindings.interceptorBindings(attributes);
    if (herBindings.isEmpty()) {
      return this.interceptorBindings().isEmpty();
    } else if (herBindings.size() == 1 && anyInterceptorBinding(herBindings.iterator().next())) {
      return true;
    }
    final List<NamedAttributeMap<?>> ibs = this.interceptorBindings();
    return ibs.size() == herBindings.size() && ibs.containsAll(herBindings) && herBindings.containsAll(ibs);
  }

  /**
   * Returns {@code true} if and only if this {@link BeanSelectionCriteria}'s {@linkplain #qualifiers() qualifiers} are
   * contained by the qualifiers present in the supplied {@link Collection} of attributes.
   *
   * <p>If the return value of an invocation of the {@link #qualifiers()} method is empty, then this method behaves as
   * if the return value of an invocation of {@link Qualifiers#defaultQualifiers()} were returned instead.</p>
   *
   * <p>If the supplied {@code attributes} has no qualifiers, then the return value of {@link
   * Qualifiers#anyAndDefaultQualifiers()} will be used instead.</p>
   *
   * @param attributes a {@link Collection} of {@link NamedAttributeMap} instances representing attributes; must not be {@code null}
   *
   * @return {@code true} if and only if this {@link BeanSelectionCriteria}'s {@linkplain #qualifiers() qualifiers} are
   * contained by the qualifiers present in the supplied {@link Collection} of attributes; {@code false} in all other
   * cases
   *
   * @exception NullPointerException if {@code attributes} is {@code null}
   */
  public final boolean selectsQualifiers(final Collection<? extends NamedAttributeMap<?>> attributes) {
    final Collection<? extends NamedAttributeMap<?>> myQualifiers = this.qualifiers();
    final List<? extends NamedAttributeMap<?>> herQualifiers = Qualifiers.qualifiers(attributes);
    if (myQualifiers.isEmpty()) {
      // Pretend I had [@Default] and she had at least [@Default] (e.g. [@Default, @Any])
      return herQualifiers.isEmpty() || containsAll(herQualifiers::contains, defaultQualifiers());
    } else if (herQualifiers.isEmpty()) {
      for (final NamedAttributeMap<?> myQualifier : myQualifiers) {
        if (anyQualifier(myQualifier) || defaultQualifier(myQualifier)) {
          // I had [@Default] or [@Any] or [@Default, @Any]; pretend she had [@Default, @Any].
          return true;
        }
      }
      return false;
    } else {
      return containsAll(herQualifiers::contains, myQualifiers);
    }
  }

  /**
   * Returns {@code true} if and only if this {@link BeanSelectionCriteria}'s {@linkplain #type() type} {@linkplain
   * TypeMatcher#matches(TypeMirror, TypeMirror) <em>matches</em>} at least one of the {@link TypeMirror} instances in
   * the supplied {@link Collection} of {@link TypeMirror}s.
   *
   * @param types a {@link Collection} of {@link TypeMirror}s; must not be {@code null}
   *
   * @return {@code true} if and only if this {@link BeanSelectionCriteria}'s {@linkplain #type() type} {@linkplain
   * TypeMatcher#matches(TypeMirror, TypeMirror) <em>matches</em>} at least one of the {@link TypeMirror} instances in
   * the supplied {@link Collection} of {@link TypeMirror}s; {@code false} in all other cases
   *
   * @exception NullPointerException if {@code types} is {@code null}
   */
  public final boolean selectsTypeFrom(final Collection<? extends TypeMirror> types) {
    final TypeMatcher typeMatcher = this.typeMatcher();
    final TypeMirror receiver = this.type();
    for (final TypeMirror payload : types) {
      if (typeMatcher.matches(receiver, payload)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns an {@link Optional} containing a {@link DynamicConstantDesc} describing this {@link BeanSelectionCriteria},
   * or an {@linkplain Optional#isEmpty() empty <code>Optional</code>} if it could not be described.
   *
   * @return an {@link Optional} containing a {@link DynamicConstantDesc} describing this {@link BeanSelectionCriteria},
   * or an {@linkplain Optional#isEmpty() empty <code>Optional</code>} if it could not be describe; never {@code null}
   */
  @Override // Constable
  public final Optional<DynamicConstantDesc<BeanSelectionCriteria>> describeConstable() {
    return this.typeMatcher().describeConstable()
      .flatMap(typeMatcherDesc -> ((DelegatingTypeMirror)this.type()).describeConstable()
               .flatMap(typeDesc -> Constables.describeConstable(this.attributes())
                        .map(attributesDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                                      MethodHandleDesc.ofMethod(STATIC,
                                                                                                CD_BeanSelectionCriteria,
                                                                                                "of",
                                                                                                MethodTypeDesc.of(CD_BeanSelectionCriteria,
                                                                                                                  CD_TypeMatcher,
                                                                                                                  CD_TypeMirror,
                                                                                                                  CD_Collection)),
                                                                      typeMatcherDesc,
                                                                      typeDesc,
                                                                      attributesDesc))));
  }

  @Override // Object
  public final int hashCode() {
    int hashCode = 17;
    hashCode = 31 * hashCode + this.type().hashCode();
    hashCode = 31 * hashCode + this.attributes().hashCode();
    return hashCode;
  }

  @Override // Object
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && other.getClass() == this.getClass()) {
      final BeanSelectionCriteria her = (BeanSelectionCriteria)other;
      return
        Objects.equals(this.type(), her.type()) &&
        Objects.equals(this.attributes(), her.attributes());
    } else {
      return false;
    }
  }

  @Override
  public final String toString() {
    return
      this.getClass().getSimpleName() + "[" +
      "type=" + this.type() + ", " +
      "attributes=" + this.attributes() +
      "]";
  }


  /*
   * Static methods.
   */


  private static final <T extends DelegatingTypeMirror> T validateType(final T type) {
    return switch (type.getKind()) {
    case ARRAY, BOOLEAN, BYTE, CHAR, DECLARED, DOUBLE, FLOAT, INT, LONG, SHORT -> type;
    case ERROR, EXECUTABLE, INTERSECTION, MODULE, NONE, NULL, OTHER, PACKAGE, TYPEVAR, UNION, VOID, WILDCARD ->
      throw new IllegalArgumentException("type: " + type);
    };
  }

  private static final boolean containsAll(final Predicate<? super Object> p, final Iterable<?> i) {
    for (final Object o : i) {
      if (!p.test(o)) {
        return false;
      }
    }
    return true;
  }

  // Called by describeConstable().
  public static final BeanSelectionCriteria of(final TypeMatcher tm,
                                               final TypeMirror type,
                                               final Collection<? extends NamedAttributeMap<?>> attributes) {
    return new BeanSelectionCriteria(tm, type, List.copyOf(attributes));
  }

}
