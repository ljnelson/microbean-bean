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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import java.util.function.Predicate;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.QualifiedNameable;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import org.microbean.lang.TypeAndElementSource;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;

import static org.microbean.lang.ConstantDescs.CD_TypeAndElementSource;

sealed abstract class AbstractTypeMatcher implements Constable permits BeanTypeMatcher, EventTypeMatcher {


  /*
   * Instance fields.
   */


  private final TypeAndElementSource tes;


  /*
   * Constructors.
   */


  AbstractTypeMatcher(final TypeAndElementSource tes) {
    super();
    this.tes = Objects.requireNonNull(tes, "tes");
  }


  /*
   * Instance methods.
   */


  // Return a possibly new ArrayType whose component type is the supplied componentType.
  final ArrayType arrayTypeOf(final TypeMirror componentType) {
    return this.tes.arrayTypeOf(componentType);
  }

  // Is payload's condensed super bound (lower bound) covariantly assignable to receiver?
  //
  // Since either of a wildcard type's bounds may be a type variable, and since a type variable's bound may be an
  // intersection type, it follows that after condensation either of a wildcard type's bounds may be a list of actual
  // types.
  //
  // Therefore this is really: Is there one bound among payload's actual super bounds that is assignable to receiver?
  final boolean assignableFromCondensedSuperBound(final ReferenceType receiver, final WildcardType payload) {
    assert payload.getKind() == TypeKind.WILDCARD;
    final ReferenceType superBound = (ReferenceType)payload.getSuperBound();
    return superBound == null || switch (superBound.getKind()) {
    case ARRAY, DECLARED -> covariantlyAssignable(receiver, superBound);
    case TYPEVAR -> covariantlyAssignable(receiver, (ReferenceType)this.condense(superBound));
    default -> throw new AssertionError();
    };
  }

  // Is candidate covariantly assignable to w's condensed extends (upper) bound?
  final boolean assignableToCondensedExtendsBound(final WildcardType w, final ReferenceType candidate) {
    assert w.getKind() == TypeKind.WILDCARD;
    final ReferenceType extendsBound = (ReferenceType)w.getExtendsBound();
    return extendsBound == null || switch (extendsBound.getKind()) {
    case ARRAY, DECLARED -> covariantlyAssignable(extendsBound, candidate);
    case TYPEVAR -> covariantlyAssignable((ReferenceType)this.condense(extendsBound), List.of(candidate));
    default -> throw new AssertionError();
    };
  }

  // Is the type argument represented by payload assignable to all of the receiver's condensed bounds?
  //
  // Pay close attention to how this is called, i.e. what is payload and what is receiver is often "backwards". For
  // example:
  //
  //   @Inject Foo<String> foo; <-- Bean<T extends CharSequence> // is the String "actual type" (payload) assignable to T's (receiver's) CharSequence bound?
  //   //          ^ payload!            ^ receiver!
  //
  // Recall that after condensing "T extends CharSequence" you get CharSequence.
  //
  // Recall that if, instead, you had T extends S and S extends CharSequence, condensing T still yields CharSequence.
  final boolean assignableToCondensedTypeVariableBounds(final TypeVariable receiver, final TypeMirror payload) {
    assert receiver.getKind() == TypeKind.TYPEVAR;
    return covariantlyAssignable(List.of(receiver), List.of(payload)); // deliberately List.of(payload) and not condense(payload)
  }

  // "Condenses" t, according to the following rules.
  //
  // If t is null, returns List.of().
  //
  // If t is a type variable, returns the result of condensing its upper bound (thus eliminating the type variable).
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
  final List<? extends TypeMirror> condense(final TypeMirror t) {
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
  final List<? extends TypeMirror> condense(final List<? extends TypeMirror> ts) {
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

  // It's not immediately clear what CDI means by a type variable's upper bound. In javax.lang.model.type parlance, the
  // upper bound of a TypeVariable could be an IntersectionType, which java.lang.reflect.TypeVariable represents as a
  // collection of bounds. CDI blundered into this earlier: https://issues.redhat.com/browse/CDI-440 and
  // https://github.com/jakartaee/cdi/issues/682
  //
  // "the upper bound of the required type parameter [receiver type argument] is assignable to the upper bound, if any,
  // of the bean type parameter [payload type argument]" (when both arguments are type variables) should *actually*
  // read:
  //
  // "for each bound, PTA, of the bean type parameter [payload type argument], there is at least one bound, RTA, of the
  // required type parameter [receiver type argument], which is assignable to PTA."
  //
  // The TCK enforces this, even though it's not in the specification (!).
  //
  // Weld's implementation confuses type parameters with arguments, just like the specification. They have a series of
  // methods implemented as part of PR 614 (https://github.com/weld/core/pull/614) named "parametersMatch" [arguments
  // match].
  //
  // Weld also has methods named things like "getUppermostTypeVariableBounds" and "getUppermostBounds". These appear to
  // "condense" useless type variable extensions to "get to" the "real" types involved. So T extends S extends String
  // becomes String.
  //
  // Digging deeper, you (I) might think getUppermostTypeVariableBounds(tv) is just erase(tv) applied recursively. But I
  // think you would be wrong.
  //
  // Start with:
  // https://github.com/openjdk/jdk/blob/181845ae46157a9bb3bf8e2a328fa59eddc0273a/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2450
  //
  // Compare vs.:
  // https://github.com/weld/core/blob/e894d1699ff1c91332605f5ecae5f53410effb81/impl/src/main/java/org/jboss/weld/resolution/AbstractAssignabilityRules.java#L57-L62
  //
  // To illustrate the difference between the two operations, recursive erasure (according to the Java Language
  // Specification) of the pseudo-declaration T extends S extends List<String> & Serializable would yield, simply, List
  // (T would erease to "the erasure of its leftmost bound", which would be the erasure of S, which would be "the
  // erasure of its leftmost bound", which would be the erasure of List<String>, which would be List. Serializable just
  // gets dropped.)
  //
  // By contrast, Weld's getUppermostTypeVariableBounds(T) operation would yield [List<String>, Serializable] (have not
  // tested this with code, just reading).
  //
  // I think this is what the javac compiler calls, somewhat confusingly, "classBound":
  // https://github.com/openjdk/jdk/blob/jdk-24%2B7/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2760-L2796
  //
  // So then:
  //
  // For every bound in (condensed) receiverBounds, is there a bound in (condensed) payloadBounds that is covariantly
  // assignable to it?
  //
  // (Is there one bound in (condensed) payloadBounds that matches all bounds in (condensed) receiver bounds?)
  //
  // Throws ClassCastException if, after condensing, any encountered bound is not either an ArrayType or a DeclaredType.
  final boolean covariantlyAssignable(final List<? extends TypeMirror> receiverBounds, List<? extends TypeMirror> payloadBounds) {
    payloadBounds = this.condense(payloadBounds);
    for (final TypeMirror condensedReceiverBound : this.condense(receiverBounds)) {
      if (!covariantlyAssignable((ReferenceType)condensedReceiverBound, payloadBounds)) {
        return false;
      }
    }
    return true;
  }

  // Is there a DeclaredType-or-ArrayType bound in condensedPayloadBounds that is assignable to classOrArrayTypeReceiver
  // using Java, not CDI, assignability semantics?
  //
  // It is assumed condensedPayloadBounds is the result of a condense() call.
  //
  // Throws ClassCastException or IllegalArgumentException if the classOrArrayTypeReceiver is anything other than
  // TypeKind.ARRAY or TypeKind.DECLARED, or if any of the condensedPayloadBounds is not of TypeKind.ARRAY or TypeKind.DECLARED
  final boolean covariantlyAssignable(final ReferenceType classOrArrayTypeReceiver, final List<? extends TypeMirror> condensedPayloadBounds) {
    return switch (classOrArrayTypeReceiver.getKind()) {
    case ARRAY, DECLARED -> {
      for (final TypeMirror condensedPayloadBound : condensedPayloadBounds) {
        switch (condensedPayloadBound.getKind()) {
        case ARRAY:
        case DECLARED:
          if (covariantlyAssignable(classOrArrayTypeReceiver, (ReferenceType)condensedPayloadBound)) {
            yield true;
          }
          break;
        default:
          throw new IllegalArgumentException("condensedPayloadBounds: " + condensedPayloadBounds);
        }
      }
      yield false;
    }
    default -> throw new IllegalArgumentException("classOrArrayTypeReceiver: " + classOrArrayTypeReceiver + "; kind: " + classOrArrayTypeReceiver.getKind());
    };
  }

  // Is classOrArrayTypePayload assignable to classOrArrayTypeReceiver following the rules of Java assignability
  // (i.e. covariance)?
  //
  // The types are ReferenceTypes because this is only ever invoked in the context of type arguments.
  final boolean covariantlyAssignable(final ReferenceType classOrArrayTypeReceiver, final ReferenceType classOrArrayTypePayload) {
    return
      classOrArrayTypeReceiver == classOrArrayTypePayload || // Optimization
      // Note that TypeAndElementSource#assignable(TypeMirror, TypeMirror) follows the lead of
      // javax.lang.model.util.Types#isAssignable(TypeMirror, TypeMirror) where the "payload" is the *first* parameter
      // and the "receiver" is the *second* argument.
      this.tes.assignable(classOrArrayTypePayload, classOrArrayTypeReceiver); // yes, "backwards"
  }

  /**
   * Returns an {@link Optional} housing a {@link ConstantDesc} that represents this {@link AbstractTypeMatcher}
   * implementation.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>The default implementation of this method relies on the presence of a {@code public} constructor that accepts a
   * single {@link TypeAndElementSource}-typed argument.</p>
   *
   * <p>The {@link Optional} returned by an invocation of this method may be, and often will be, {@linkplain
   * Optional#isEmpty() empty}.</p>
   *
   * @return an {@link Optional} housing a {@link ConstantDesc} that represents this {@link AbstractTypeMatcher}
   * implementation; never {@code null}
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

  // Erase t and return its erasure. Erasing is a complex business that can involve the creation of new types.
  final TypeMirror erasure(final TypeMirror t) {
    return this.tes.erasure(t);
  }

  // Is payload "identical to" receiver, following the intent of CDI? The relation "identical to" is not defined in the
  // specification. Does it mean ==? Does it mean equals()? Does it mean
  // javax.lang.model.util.Types#isSameType(TypeMirror, TypeMirror)? Something else?
  //
  // This implementation chooses TypeAndElementSource#sameType(TypeMirror, TypeMirror), but with one
  // change. TypeAndElementSource#sameType(TypeMirror, TypeMirror) is usually backed by the
  // javax.lang.model.util.Types#isSameType(TypeMirror, TypeMirror) method. That method will return false if either
  // argument is a wildcard type. This method first checks to see if the arguments are the same Java references (==),
  // regardless of type.
  final boolean identical(final TypeMirror receiver, final TypeMirror payload) {
    // CDI has an undefined notion of "identical to". This method attempts to divine and implement the intent. Recall
    // that javax.lang.model.* compares types with "sameType" semantics.
    return receiver == payload || this.tes.sameType(receiver, payload);
  }

  // Return t if its element declares a non-generic class, or if it is the raw type usage of a generic class.
  final TypeMirror nonGenericClassOrRawType(final TypeMirror t) {
    return yieldsRawType(t) ? this.rawType(t) : t;
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
  final TypeMirror rawType(final TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY -> {
      final TypeMirror et = elementType(t);
      if (!parameterized(et)) {
        throw new IllegalArgumentException("t is an array type whose element type is not parameterized so cannot yield a raw type");
      }
      yield this.arrayTypeOf(this.erasure(et));
    }
    case DECLARED -> {
      if (!parameterized(t)) {
        throw new IllegalArgumentException("t is a declared type that is not parameterized so cannot yield a raw type");
      }
      yield this.erasure(t);
    }
    default -> throw new IllegalArgumentException("t is a " + t.getKind() + " type and so cannot yield a raw type");
    };
  }


  /**
   * Returns the {@link TypeAndElementSource} used by this {@link AbstractTypeMatcher} implementation.
   *
   * @return the {@link TypeAndElementSource} used by this {@link AbstractTypeMatcher} implementation; never {@code
   * null}
   *
   * @see #AbstractTypeMatcher(TypeAndElementSource)
   *
   * @see TypeAndElementSource
   */
  final TypeAndElementSource typeAndElementSource() {
    return this.tes;
  }

  // Is t an unbounded type variable?
  //
  // CDI does not define what an "unbounded type variable" is. This method attempts to divine and implement the intent.
  //
  // Since according to the Java Language Specification, all type variables have an upper bound, which in the
  // pathological case is java.lang.Object, it would seem that "unbounded" means "has java.lang.Object as its upper
  // bound".
  //
  // It is not clear in the case of T extends S whether T is an unbounded TypeVariable or not. This interpretation
  // behaves as if it is.
  //
  // Weld seems to take the position that an unbounded type variable is one that has java.lang.Object as its sole upper
  // bound; see
  // https://github.com/weld/core/blob/5.1.2.Final/impl/src/main/java/org/jboss/weld/util/Types.java#L258. Under this
  // interpretation T extends S would not be considered an unbounded type variable. Type variable bounds are erased in
  // every other situation in CDI.
  final boolean unboundedTypeVariable(final TypeMirror t) {
    return switch (t.getKind()) {
    case TYPEVAR -> {
      final List<? extends TypeMirror> condensedBounds = this.condense(((TypeVariable)t).getUpperBound());
      yield condensedBounds.size() == 1 && isJavaLangObject(condensedBounds.get(0));
    }
    default -> false;
    };
  }



  /*
   * Static methods.
   */


  // Is t an "actual type"?
  //
  // CDI mentions actual types but does not define what they are. This method attempts to divine and implement the
  // intent.
  //
  // A comment in a closed bug report (CDI-502)
  // (https://issues.redhat.com/browse/CDI-502?focusedId=13036118&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-13036118)
  // by one of the reference implementation's authors (Jozef Hartinger) provides the only definition:
  //
  // "An actual type is a type that is not a wildcard nor [sic] an unresolved [sic] type variable."
  //
  // The Java Language Specification does not mention anything about "actual types" or type variable "resolution". CDI
  // does not mention anything about type variable "resolution".
  //
  // (Clearly type variable "resolution" must simply be the process of supplying a type argument for a type parameter.)
  //
  // Presumably the null type is not intended to be an actual type either.
  //
  // More strictly, therefore, the intent seems to be that an actual type is an array, declared or primitive type, and
  // none other.
  //
  // See also: https://github.com/weld/core/blob/5.1.2.Final/impl/src/main/java/org/jboss/weld/util/Types.java#L213
  static final boolean actual(final TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY, BOOLEAN, BYTE, CHAR, DECLARED, DOUBLE, FLOAT, INT, LONG, SHORT -> true;
    default -> false;
    };
  }

  // Do all supplied TypeMirrors, when used as type arguments, pass the test represented by the supplied Predicate?
  static final boolean allTypeArgumentsAre(final Iterable<? extends TypeMirror> typeArguments, final Predicate<? super TypeMirror> p) {
    for (final TypeMirror t : typeArguments) {
      if (!p.test(t)) {
        return false;
      }
    }
    return true;
  }

  // Is t a type that CDI considers to be "parameterized"?
  //
  // There are some cases, but not all, where CDI (incorrectly) considers an array type to be something that can be
  // parameterized, or else "bean" and "event type parameter" resolution would never work. See
  // https://stackoverflow.com/questions/76493672/when-cdi-speaks-of-a-parameterized-type-does-it-also-incorrectly-mean-array-typ.
  //
  // The semantics CDI wants to express are really: can t *yield* a raw type, for a certain definition of "yield"? See
  // #yieldsRawType(TypeMirror) below.
  static final boolean cdiParameterized(final TypeMirror t) {
    return yieldsRawType(t);
  }

  // Returns the element type of t.
  //
  // The element type of an array type is the element type of its component type.
  //
  // The element type of every other kind of type is the type itself.
  static final TypeMirror elementType(final TypeMirror t) {
    return t.getKind() == TypeKind.ARRAY ? elementType(((ArrayType)t).getComponentType()) : t;
  }

// Does e represent a generic declaration?
  //
  // A declaration is generic if it declares one or more type parameters.
  //
  // Since an annotation interface cannot declare type parameters, it follows that TypeElements representing annotation
  // instances are never generic.
  static final boolean generic(final Element e) {
    return switch (e.getKind()) {
    case CLASS, CONSTRUCTOR, ENUM, INTERFACE, METHOD, RECORD -> !((Parameterizable)e).getTypeParameters().isEmpty();
    default -> false;
    };
  }

  // Is t the usage of a generic class, i.e. a usage (whether raw or parameterized) of a generic class declaration?
  //
  // t is deemed to be generic if it is a declared type whose defining element (its type declaration) is generic.
  //
  // Array types are never generic.
  static final boolean generic(final TypeMirror t) {
    return
      t.getKind() == TypeKind.DECLARED &&
      generic(((DeclaredType)t).asElement());
  }

  static IllegalArgumentException illegalPayload(final TypeMirror payload) {
    return new IllegalArgumentException("Illegal payload kind: " + payload.getKind() + "; payload: " + payload);
  }

  static IllegalArgumentException illegalReceiver(final TypeMirror receiver) {
    return new IllegalArgumentException("Illegal receiver kind: " + receiver.getKind() + "; receiver: " + receiver);
  }

  // Is e a TypeElement representing java.lang.Object?
  static final boolean isJavaLangObject(final Element e) {
    return
      e.getKind() == ElementKind.CLASS &&
      ((QualifiedNameable)e).getQualifiedName().contentEquals("java.lang.Object");
  }

  // Is t a DeclaredType whose asElement() method returns an Element representing java.lang.Object?
  static final boolean isJavaLangObject(final TypeMirror t) {
    return
      t.getKind() == TypeKind.DECLARED &&
      isJavaLangObject(((DeclaredType)t).asElement());
  }

  // Is t a parameterized type (and not a raw type) strictly according to the rules of the Java Language Specification?
  //
  // A type is parameterized if it is a declared type with a non-empty list of type arguments. No other type is
  // parameterized.
  static final boolean parameterized(final TypeMirror t) {
    return
      t.getKind() == TypeKind.DECLARED &&
      !((DeclaredType)t).getTypeArguments().isEmpty();
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
  static final boolean raw(final TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY -> raw(elementType((ArrayType)t));
    case DECLARED -> generic(t) && ((DeclaredType)t).getTypeArguments().isEmpty();
    case TYPEVAR -> false; // called out explicitly just so you realize it
    default -> false;
    };
  }

  // Can t *yield* a raw type?
  //
  // We say that to yield a raw type, t must be either:
  //
  // * an array type with a parameterized element type
  // * a declared type with at least one type argument
  static final boolean yieldsRawType(final TypeMirror t) {
    return parameterized(t) || t.getKind() == TypeKind.ARRAY && parameterized(elementType(t));
  }


}
