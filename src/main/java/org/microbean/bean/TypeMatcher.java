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

import java.lang.System.Logger;

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

/**
 * A {@link Matcher} encapsulating <a
 * href="https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#performing_typesafe_resolution">CDI-compatible
 * type matching rules</a>.
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 *
 * @see #test(TypeMirror, TypeMirror)
 */
public final class TypeMatcher implements Constable, Matcher<TypeMirror, TypeMirror> {


  /*
   * Static fields.
   */


  private static final Logger LOGGER = System.getLogger(TypeMatcher.class.getName());


  /*
   * Instance fields.
   */


  private final TypeAndElementSource tes;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link TypeMatcher}.
   *
   * @param tes a {@link TypeAndElementSource}; must not be {@code null}
   *
   * @exception NullPointerException if {@code tes} is {@code null}
   *
   * @see TypeAndElementSource
   */
  public TypeMatcher(final TypeAndElementSource tes) {
    super();
    this.tes = Objects.requireNonNull(tes, "tes");
  }


  /*
   * Instance methods.
   */


  /*
   * Public methods.
   */

  /**
   * Returns an {@link Optional} housing a {@link ConstantDesc} that represents this {@link TypeMatcher}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>The default implementation of this method relies on the presence of a {@code public} constructor that accepts a
   * single {@link TypeAndElementSource}-typed argument.</p>
   *
   * <p>The {@link Optional} returned by an invocation of this method may be, and often will be, {@linkplain
   * Optional#isEmpty() empty}.</p>
   *
   * @return an {@link Optional} housing a {@link ConstantDesc} that represents this {@link TypeMatcher}; never {@code
   * null}
   *
   * @see Constable#describeConstable()
   */
  @Override // Constable
  public final Optional<? extends ConstantDesc> describeConstable() {
    return (this.tes instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty())
      .map(tesDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                             MethodHandleDesc.ofConstructor(ClassDesc.of(this.getClass().getName()),
                                                                            CD_TypeAndElementSource),
                                             tesDesc));
  }

  /**
   * Returns {@code true} if and only if the supplied {@code payload} argument <a
   * href="https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#performing_typesafe_resolution"><em>matches</em></a>
   * the supplied {@code receiver} argument, according to the rules defined by <a
   * href="https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#performing_typesafe_resolution">section
   * 2.4.2.1 of the CDI specification</a>.
   *
   * @param receiver the left hand side of a type assignment; must not be {@code null}
   *
   * @param payload the right hand side of a type assignment; must not be {@code null}
   *
   * @return {@code true} if and only if the supplied {@code payload} argument <a
   * href="https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#performing_typesafe_resolution">matches</a>
   * the supplied {@code receiver} argument, according to the rules defined by <a
   * href="https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#performing_typesafe_resolution">section
   * 2.4.2.1 of the CDI specification</a>; {@code false} otherwise
   *
   * @exception NullPointerException if either argument is {@code null}
   *
   * @exception IllegalArgumentException if either type is any type other than an {@linkplain TypeKind#ARRAY array
   * type}, a {@link TypeKind#isPrimitive() primitive type}, or a {@linkplain TypeKind#DECLARED declared type}
   */
  // Is the payload assignable to the receiver? That is, does the payload "match the receiver", in CDI parlance?
  @Override
  public final boolean test(final TypeMirror receiver, final TypeMirror payload) {
    // "A bean is assignable to a given injection point if:
    //
    // "The bean has a bean type [payload] that matches the required type [receiver]. For this purpose..."
    return receiver == Objects.requireNonNull(payload, "payload") || switch (receiver.getKind()) {
      // "...primitive types are considered to match their corresponding wrapper types in java.lang..."
      case BOOLEAN  -> payload.getKind() == TypeKind.BOOLEAN || declaredTypeNamed(payload, "java.lang.Boolean");
      case BYTE     -> payload.getKind() == TypeKind.BYTE    || declaredTypeNamed(payload, "java.lang.Byte");
      case CHAR     -> payload.getKind() == TypeKind.CHAR    || declaredTypeNamed(payload, "java.lang.Character");
      case DOUBLE   -> payload.getKind() == TypeKind.DOUBLE  || declaredTypeNamed(payload, "java.lang.Double");
      case FLOAT    -> payload.getKind() == TypeKind.FLOAT   || declaredTypeNamed(payload, "java.lang.Float");
      case INT      -> payload.getKind() == TypeKind.INT     || declaredTypeNamed(payload, "java.lang.Integer");
      case LONG     -> payload.getKind() == TypeKind.LONG    || declaredTypeNamed(payload, "java.lang.Long");
      case SHORT    -> payload.getKind() == TypeKind.SHORT   || declaredTypeNamed(payload, "java.lang.Short");
      // "...and array types are considered to match only if their element types [note: not component types] are
      // identical ['identical' is actually undefined in the specification]..."
      case ARRAY    -> payload.getKind() == TypeKind.ARRAY   && this.identical(elementType(receiver), elementType(payload));
      case DECLARED -> switch (payload.getKind()) {
        // "...primitive types are considered to match their corresponding wrapper types in java.lang..."
        case BOOLEAN  -> named((DeclaredType)receiver, "java.lang.Boolean");
        case BYTE     -> named((DeclaredType)receiver, "java.lang.Byte");
        case CHAR     -> named((DeclaredType)receiver, "java.lang.Character");
        case DOUBLE   -> named((DeclaredType)receiver, "java.lang.Double");
        case FLOAT    -> named((DeclaredType)receiver, "java.lang.Float");
        case INT      -> named((DeclaredType)receiver, "java.lang.Integer");
        case LONG     -> named((DeclaredType)receiver, "java.lang.Long");
        case SHORT    -> named((DeclaredType)receiver, "java.lang.Short");
        // "Parameterized and raw types [and non-generic classes, and non-array types]..."
        case DECLARED ->
          // "...are considered to match if they are identical [undefined]..."
          this.identical(receiver, payload) ||
          // "...or if the bean type [payload] is assignable to [see #assignable(TypeMirror, TypeMirror)] the required type
          // [receiver]...."
          this.assignable((DeclaredType)receiver, (DeclaredType)payload);
        default       -> throw new IllegalArgumentException("Illegal payload kind: " + payload.getKind() + "; payload: " + payload);
      };
      default       -> throw new IllegalArgumentException("Illegal receiver kind: " + receiver.getKind() + "; receiver: " + receiver);
    };
  }

  /**
   * Returns the {@link TypeAndElementSource} used by this {@link TypeMatcher}.
   *
   * @return the {@link TypeAndElementSource} used by this {@link TypeMatcher}; never {@code null}
   *
   * @see #TypeMatcher(TypeAndElementSource)
   *
   * @see TypeAndElementSource
   */
  public final TypeAndElementSource typeAndElementSource() {
    return this.tes;
  }

  /*
   * Private methods.
   */

  private final boolean assignable(final DeclaredType receiver, final DeclaredType payload) {
    assert receiver.getKind() == TypeKind.DECLARED;
    assert payload.getKind() == TypeKind.DECLARED;
    return switch (payload) {
      // "A parameterized bean type [payload] is considered assignable..."
      case DeclaredType parameterizedPayload when parameterized(payload) -> switch (receiver) {
        // "...to a [non-generic class or] raw required type [receiver]..."
        case TypeMirror rawReceiver when !generic(receiver) || raw(receiver) -> {
          // "...if the [non-generic class or] raw types are identical [undefined] and all type parameters [type
          // arguments] of the bean type [payload] are either unbounded type variables [undefined] or java.lang.Object."
          yield
            this.identical(this.nonGenericClassOrRawType(rawReceiver), this.nonGenericClassOrRawType(parameterizedPayload)) &&
            allTypeArgumentsAre(parameterizedPayload.getTypeArguments(),
                                ((Predicate<TypeMirror>)this::unboundedTypeVariable).or(TypeMatcher::isJavaLangObject));
        }
        // "...to a parameterized required type [receiver]..."
        case DeclaredType parameterizedReceiver when parameterized(receiver) -> {
          // "...if they have identical raw type [really if their declarations/elements are 'identical']..."
          if (this.identical(this.rawType(parameterizedReceiver), this.rawType(parameterizedPayload))) {
            // "...and for each parameter [type argument pair]..."
            final List<? extends TypeMirror> rtas = parameterizedReceiver.getTypeArguments();
            final List<? extends TypeMirror> ptas = parameterizedPayload.getTypeArguments();
            assert rtas.size() == ptas.size();
            for (int i = 0; i < rtas.size(); i++) {
              final TypeMirror rta = rtas.get(i);
              final TypeMirror pta = ptas.get(i);
              // "...the required [receiver] type parameter [type argument] and the bean [payload] type parameter [type
              // argument] are actual types [non-type variable, non-wildcard reference types]..."
              if (actual(rta)) {
                if (actual(pta)) {
                  // "...with identical [non-generic classes or] raw type[s]..."
                  if (this.identical(this.nonGenericClassOrRawType(rta), this.nonGenericClassOrRawType(pta))) {
                    // "...and, if the type [?] is parameterized [?]..."
                    //
                    // Let rta and pta be array types with parameterized element types, such as List<Number>[] and
                    // List<String>[]. Then their raw types are List[] and List[]. Their parameterized element types are
                    // List<Number> and List<String>. According to the JLS, neither List<Number>[] nor List<String>[] is
                    // parameterized.
                    //
                    // In this example, if we get here, we have only proven that List[] is "identical to" List[].
                    //
                    // The "if the type is parameterized" clause is tough. Which type is under discussion? "the type"
                    // seems to refer to the "identical raw type". But a raw type by definition is not parameterized, so
                    // this clause would seem to be superfluous and thus never apply, and so List<Number>[] :=
                    // List<String>[] is OK. Oops. So not that interpretation.
                    //
                    // What if instead the "if the type is parameterized" clause means the receiver type itself and
                    // somehow loosely the payload type as well? Well, clearly it cannot correctly do this, since an
                    // array type is never a parameterized type. Same bad result. Oops. So not that interpretation.
                    //
                    // Or what if "identical raw type" really means "identical raw type (or identical component type if
                    // they are array types)"? That would be most efficient, since it would rule out List<Number>[] :=
                    // List<String>[] right off the bat: we wouldn't even get here. But that really doesn't seem to be
                    // what is meant. So probably not that interpretation.
                    //
                    // What if the "if the type is parameterized" clause really means "if the element type declaration
                    // used by both the receiver and payload types is generic"? That would work. That's also
                    // semantically equivalent to something like: "...if at least one of the two arguments is a
                    // parameterized type [e.g. List<Number>, not List<Number>[], not String, not List], or at least one
                    // of the two types is an array type with a parameterized element type [e.g. List<Number>[], not
                    // List[], not String[]]..."
                    //
                    // That is the interpretation we apply here. So:
                    //
                    //   "...and, if the type [?] is parameterized [?]..."
                    //
                    // becomes:
                    //
                    //   "...and, if at least one of the two type arguments is a parameterized type, or if at least one
                    //   of the two types is an array type with a parameterized element type..."
                    //
                    // That, in turn, designates any type capable of properly yielding a raw type, while ruling out
                    // those that can't! That means it is exactly equal to our yieldsRawType(TypeMirror) method, and so
                    // that's what cdiParameterized(TypeMirror) returns (go see for yourself).
                    if (cdiParameterized(rta)) { // really just yieldsRawType(rta)
                      assert cdiParameterized(pta); // ...because otherwise their raw types would not have been "identical"
                      // "...the bean type parameter [type argument] is assignable to the required type parameter [type
                      // argument] according to [all of] these rules [including 'matching']..."
                      if (test(rta, pta)) { // note recursion
                        continue;
                      }
                      yield false;
                    } else {
                      assert !cdiParameterized(pta);
                      continue; // yes, trust me; vetted
                    }
                  }
                  yield false;
                } else if (pta.getKind() == TypeKind.TYPEVAR) {
                  // "...the required [receiver] type parameter [type argument] is an actual type [a non-type variable,
                  // non-wildcard reference type], the bean [payload] type parameter [type argument] is a type variable and
                  // the actual type [non-type variable, non-wildcard reference type] [required type argument, receiver] is
                  // assignable to the upper bound, if any, of the type variable [bean type argument, payload] [type
                  // variables have multiple bounds]..."
                  //
                  // (This is weird. Yes, the *receiver* type argument is being tested to see if it is assignable to the
                  // *payload* type argument.)
                  if (assignableToCondensedTypeVariableBounds((TypeVariable)pta, (ReferenceType)rta)) {
                    continue;
                  }
                  yield false;
                }
                throw new AssertionError("Unexpected payload type argument kind: " + pta.getKind());
              } else if (rta.getKind() == TypeKind.WILDCARD) {
                // "...the required type parameter [type argument] is a wildcard, the bean type parameter [type
                // argument] is an actual type [a non-type variable, non-wildcard reference type]..."
                if (actual(pta)) {
                  // "...and the actual type [non-type variable, non-wildcard reference type] is assignable to the upper
                  // bound, if any, of the wildcard and assignable from the lower bound, if any of the wildcard"
                  if (assignableToCondensedExtendsBound((WildcardType)rta, (ReferenceType)pta) &&
                      assignableFromCondensedSuperBound((ReferenceType)pta, (WildcardType)rta)) {
                    continue;
                  }
                  yield false;
                } else if (pta.getKind() == TypeKind.TYPEVAR) {
                  // "...the required [receiver] type parameter [type argument] is a wildcard, the bean [payload] type
                  // parameter [type argument] is a type variable and the upper bound of the type variable [a type
                  // variable has many bounds?] is assignable to or assignable from the upper bound, if any, of the
                  // wildcard and assignable from the lower bound, if any, of the wildcard"
                  if ((condensedTypeVariableBoundsAssignableToCondensedExtendsBound((WildcardType)rta, (TypeVariable)pta) ||
                       condensedTypeVariableBoundsAssignableFromCondensedExtendsBound((TypeVariable)pta, (WildcardType)rta)) &&
                      condensedTypeVariableBoundsAssignableFromCondensedSuperBound((TypeVariable)pta, (WildcardType)rta)) {
                    continue;
                  }
                  yield false;
                }
                throw new AssertionError("Unexpected payload type argument kind: " + pta.getKind());
              } else if (rta.getKind() == TypeKind.TYPEVAR) {
                if (pta.getKind() == TypeKind.TYPEVAR) {
                  // "...the required [receiver] type parameter [type argument] and the bean [payload] type parameter
                  // [type argument] are both type variables and the upper bound of the required [receiver] type
                  // parameter [type argument] [a type variable has many bounds?] is assignable to the upper bound [a
                  // type variable has many bounds?], if any, of the bean [payload] type parameter [type argument]"
                  //
                  // (This is weird. Yes, the *receiver* type argument is being tested to see if it is assignable to the
                  // *payload* type argument.)
                  if (condensedTypeVariableBoundsAssignableToCondensedTypeVariableBounds((TypeVariable)pta, (TypeVariable)rta)) {
                    continue;
                  }
                  yield false;
                }
                throw new AssertionError("Unexpected payload type argument kind: " + pta.getKind());
              }
              throw new AssertionError("Unexpected receiver type argument kind: " + rta.getKind());
            }
            yield true; // we passed all the type argument assignability tests
          }
          yield false; // the type arguments' non-generic classes or raw types were not identical
        }
        default -> throw new AssertionError("Unexpected receiver kind: " + receiver.getKind());
      };
      // "A [non-generic or] raw bean type [payload] is considered assignable..."
      case TypeMirror nonGenericOrRawPayload -> switch (receiver) {
        // "...to a parameterized required type [receiver]..."
        case DeclaredType parameterizedReceiver when parameterized(receiver) -> {
          // "...if the[ir] [non-generic classes or] raw types are identical and all type parameters [type arguments] of
          // the required type [receiver] are either unbounded type variables or java.lang.Object."
          yield
            this.identical(this.nonGenericClassOrRawType(parameterizedReceiver), nonGenericOrRawPayload) &&
            allTypeArgumentsAre(parameterizedReceiver.getTypeArguments(),
                                ((Predicate<TypeMirror>)this::unboundedTypeVariable).or(TypeMatcher::isJavaLangObject));
        }
        // [Otherwise the payload is not assignable to the receiver; identity checking should have already happened in
        // matches(), not here.]
        case DeclaredType nonGenericOrRawReceiver when receiver.getKind() == TypeKind.DECLARED -> {
          yield false; // or yield this.identical(nonGenericOrRawReceiver, nonGenericOrRawPayload);
        }
        default -> throw new AssertionError("Unexpected payload kind: " + payload.getKind() + "; receiver: " + receiver + "; payload: " + payload);
      };
    };
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
  private final boolean identical(final TypeMirror receiver, final TypeMirror payload) {
    // CDI has an undefined notion of "identical to". This method attempts to divine and implement the intent. Recall
    // that javax.lang.model.* compares types with "sameType" semantics.
    return receiver == payload || this.tes.sameType(receiver, payload);
  }

  // Is the "actual" type argument represented by payload assignable to all of the receiver's condensed bounds?
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
  private final boolean assignableToCondensedTypeVariableBounds(final TypeVariable receiver, final ReferenceType payload) {
    assert receiver.getKind() == TypeKind.TYPEVAR;
    assert actual(payload); // We can assert this only because of where the method is called.
    return covariantlyAssignable(List.of(receiver), List.of(payload)); // deliberately List.of(payload) and not condense(payload)
  }

  // Is candidate covariantly assignable to w's condensed extends (upper) bound?
  private final boolean assignableToCondensedExtendsBound(final WildcardType w, final ReferenceType candidate) {
    assert w.getKind() == TypeKind.WILDCARD;
    assert actual(candidate); // We can assert this only because of where this method is called from.
    final ReferenceType extendsBound = (ReferenceType)w.getExtendsBound();
    return extendsBound == null || switch (extendsBound.getKind()) {
    case ARRAY, DECLARED -> covariantlyAssignable(extendsBound, candidate);
    case TYPEVAR -> covariantlyAssignable((ReferenceType)this.condense(extendsBound), List.of(candidate));
    default -> throw new AssertionError();
    };
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
  private final boolean covariantlyAssignable(final List<? extends TypeMirror> receiverBounds, List<? extends TypeMirror> payloadBounds) {
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
  // Throws ClassCastException or IllegalArgumentException if any encountered type is neither an ArrayType nor a
  // DeclaredType.
  private final boolean covariantlyAssignable(final ReferenceType classOrArrayTypeReceiver, final List<? extends TypeMirror> condensedPayloadBounds) {
    assert actual(classOrArrayTypeReceiver); // We can assert this only because of where this method is called.
    return switch (classOrArrayTypeReceiver.getKind()) {
    case ARRAY, DECLARED -> {
      for (final TypeMirror condensedPayloadBound : condensedPayloadBounds) {
        assert actual(condensedPayloadBound);
        if (covariantlyAssignable(classOrArrayTypeReceiver, (ReferenceType)condensedPayloadBound)) {
          yield true;
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
  private final boolean covariantlyAssignable(final ReferenceType classOrArrayTypeReceiver, final ReferenceType classOrArrayTypePayload) {
    assert actual(classOrArrayTypeReceiver); // Based on where and how this method is called.
    assert actual(classOrArrayTypePayload); // Based on where and how this method is called.
    return
      classOrArrayTypeReceiver == classOrArrayTypePayload || // Optimization
      // Note that TypeAndElementSource#assignable(TypeMirror, TypeMirror) follows the lead of
      // javax.lang.model.util.Types#isAssignable(TypeMirror, TypeMirror) where the "payload" is the *first* parameter
      // and the "receiver" is the *second* argument.
      this.tes.assignable(classOrArrayTypePayload, classOrArrayTypeReceiver); // yes, "backwards"
  }

  // Are payload's condensed bounds assignable to receiver's condensed extends bound (upper bound)?
  private final boolean condensedTypeVariableBoundsAssignableToCondensedExtendsBound(final WildcardType receiver, final TypeVariable payload) {
    assert receiver.getKind() == TypeKind.WILDCARD;
    assert payload.getKind() == TypeKind.TYPEVAR;
    // "...the upper bound of the type variable [a type variable has many bounds?] is assignable TO [...] the upper
    // bound, if any, of the wildcard..."
    final TypeMirror extendsBound = receiver.getExtendsBound();
    // No need to condense arguments to eliminate useless type variables and intersection types so that Java covariant
    // semantics will work properly in this case; #covariantlyAssignable(List, List) does this already.
    return extendsBound == null || covariantlyAssignable(List.of((ReferenceType)extendsBound), List.of(payload));
  }

  // Is payload's condensed extends bound (upper bound) covariantly assignable to receiver's condensed bounds?
  private final boolean condensedTypeVariableBoundsAssignableFromCondensedExtendsBound(final TypeVariable receiver, final WildcardType payload) {
    assert receiver.getKind() == TypeKind.TYPEVAR;
    assert payload.getKind() == TypeKind.WILDCARD;
    // "...the upper bound of the type variable [a type variable has many bounds?] is assignable [...] FROM the upper
    // bound, if any, of the wildcard..."
    final TypeMirror extendsBound = payload.getExtendsBound();
    // No need to condense arguments to eliminate useless type variables and intersection types so that Java covariant
    // semantics will work properly in this case; #covariantlyAssignable(List, List) does this already.
    return extendsBound == null || covariantlyAssignable(List.of(receiver), List.of((ReferenceType)extendsBound));
  }

  // Is payload's super bound (lower bound) covariantly assignable to receiver's condensed bounds?
  private final boolean condensedTypeVariableBoundsAssignableFromCondensedSuperBound(final TypeVariable receiver, final WildcardType payload) {
    assert receiver.getKind() == TypeKind.TYPEVAR;
    assert payload.getKind() == TypeKind.WILDCARD;
    final TypeMirror superBound = payload.getSuperBound();
    // No need to condense arguments to eliminate useless type variables and intersection types so that Java covariant
    // semantics will work properly in this case; #covariantlyAssignable(List, List) does this already.
    return superBound == null || covariantlyAssignable(List.of(receiver), List.of(superBound));
  }

  // Are payload's condensed bounds covariantly assignable to receiver's condensed bounds?
  private final boolean condensedTypeVariableBoundsAssignableToCondensedTypeVariableBounds(final TypeVariable receiver, final TypeVariable payload) {
    assert receiver.getKind() == TypeKind.TYPEVAR;
    assert payload.getKind() == TypeKind.TYPEVAR;
    // No need to condense arguments to eliminate useless type variables and intersection types so that Java covariant
    // semantics will work properly in this case; #covariantlyAssignable(List, List) does this already.
    return covariantlyAssignable(List.of(receiver), List.of(payload));
  }

  // Is payload's condensed super bound (lower bound) covariantly assignable to receiver?
  //
  // Since either of a wildcard type's bounds may be a type variable, and since a type variable's bound may be an
  // intersection type, it follows that after condensation either of a wildcard type's bounds may be a list of actual
  // types.
  //
  // Therefore this is really: Is there one bound among payload's actual super bounds that is assignable to receiver?
  private final boolean assignableFromCondensedSuperBound(final ReferenceType receiver, final WildcardType payload) {
    assert payload.getKind() == TypeKind.WILDCARD;
    assert actual(receiver); // We can assert this only because of where this method is called from.
    final ReferenceType superBound = (ReferenceType)payload.getSuperBound();
    return superBound == null || switch (superBound.getKind()) {
    case ARRAY, DECLARED -> covariantlyAssignable(receiver, superBound);
    case TYPEVAR -> covariantlyAssignable(receiver, (ReferenceType)this.condense(superBound));
    default -> throw new AssertionError();
    };
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
  private final TypeMirror rawType(final TypeMirror t) {
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

  // Return t if its element declares a non-generic class, or if it is the raw type usage of a generic class.
  private final TypeMirror nonGenericClassOrRawType(final TypeMirror t) {
    return yieldsRawType(t) ? this.rawType(t) : t;
  }

  // Erase t and return its erasure. Erasing is a complex business that can involve the creation of new types.
  private final TypeMirror erasure(final TypeMirror t) {
    return this.tes.erasure(t);
  }

  // Return a possibly new ArrayType whose component type is the supplied componentType.
  private final ArrayType arrayTypeOf(final TypeMirror componentType) {
    return this.tes.arrayTypeOf(componentType);
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
  // condensing the type declared by java.lang.Object.
  //
  // In all other cases returns List.of(t).
  //
  // Note especially this is not type erasure, though it has some similarities.
  //
  // See #condense(List) below.
  private final List<? extends TypeMirror> condense(final TypeMirror t) {
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
      } else {
        throw new AssertionError();
      }
    }
    default           -> List.of(t); // t's bounds are defined solely by itself, or it's a wildcard and we didn't say which bounds
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
  private final List<? extends TypeMirror> condense(final List<? extends TypeMirror> ts) {
    final int size = ts.isEmpty() ? 0 : ts.size();
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
  private final boolean unboundedTypeVariable(final TypeMirror t) {
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

  // Is t the usage of a generic class, i.e. a usage (whether raw or parameterized) of a generic class declaration?
  //
  // t is deemed to be generic if it is a declared type whose defining element (its type declaration) is generic.
  //
  // Array types are never generic.
  private static final boolean generic(final TypeMirror t) {
    return
      t.getKind() == TypeKind.DECLARED &&
      generic(((DeclaredType)t).asElement());
  }

  // Is t a parameterized type (and not a raw type) strictly according to the rules of the Java Language Specification?
  //
  // A type is parameterized if it is a declared type with a non-empty list of type arguments. No other type is
  // parameterized.
  private static final boolean parameterized(final TypeMirror t) {
    return
      t.getKind() == TypeKind.DECLARED &&
      !((DeclaredType)t).getTypeArguments().isEmpty();
  }

  // Is t a type that CDI considers to be "parameterized"?
  //
  // There are some cases, but not all, where CDI (incorrectly) considers an array type to be something that can be
  // parameterized, or else "bean type parameter" resolution would never work. See
  // https://stackoverflow.com/questions/76493672/when-cdi-speaks-of-a-parameterized-type-does-it-also-incorrectly-mean-array-typ.
  //
  // The semantics CDI wants to express are really: can t *yield* a raw type, for a certain definition of "yield"? See
  // #yieldsRawType(TypeMirror) below.
  private static final boolean cdiParameterized(final TypeMirror t) {
    return yieldsRawType(t);
  }

  // Can t *yield* a raw type?
  //
  // We say that to yield a raw type, t must be either:
  //
  // * an array type with a parameterized element type
  // * a declared type with at least one type argument
  private static final boolean yieldsRawType(final TypeMirror t) {
    return parameterized(t) || t.getKind() == TypeKind.ARRAY && parameterized(elementType(t));
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
  private static final boolean raw(final TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY -> raw(elementType((ArrayType)t));
    case DECLARED -> generic(t) && ((DeclaredType)t).getTypeArguments().isEmpty();
    case TYPEVAR -> false; // called out explicitly just so you realize it
    default -> false;
    };
  }

  // Is t a declared type that bears the supplied fully qualified name?
  private static final boolean declaredTypeNamed(final TypeMirror t, final CharSequence n) {
    return
      t.getKind() == TypeKind.DECLARED &&
      named(((DeclaredType)t), n);
  }

  // Regardless of its reported TypeKind, does t's declaring TypeElement bear the supplied fully qualified name?
  //
  // Throws ClassCastException if the return value of t.asElement() is not a TypeElement.
  private static final boolean named(final DeclaredType t, final CharSequence n) {
    // (No getKind() check on purpose.)
    return ((QualifiedNameable)t.asElement()).getQualifiedName().contentEquals(n);
  }

  // Do all supplied TypeMirrors, when used as type arguments, pass the test represented by the supplied Predicate?
  private static final boolean allTypeArgumentsAre(final Iterable<? extends TypeMirror> typeArguments, final Predicate<? super TypeMirror> p) {
    for (final TypeMirror t : typeArguments) {
      if (!p.test(t)) {
        return false;
      }
    }
    return true;
  }

  // Is e a TypeElement representing java.lang.Object?
  private static final boolean isJavaLangObject(final Element e) {
    return
      e.getKind() == ElementKind.CLASS &&
      ((QualifiedNameable)e).getQualifiedName().contentEquals("java.lang.Object");
  }

  // Is t a DeclaredType whose asElement() method returns an Element representing java.lang.Object?
  private static final boolean isJavaLangObject(final TypeMirror t) {
    return
      t.getKind() == TypeKind.DECLARED &&
      isJavaLangObject(((DeclaredType)t).asElement());
  }

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
  private static final boolean actual(final TypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY, BOOLEAN, BYTE, CHAR, DECLARED, DOUBLE, FLOAT, INT, LONG, SHORT -> true;
    default -> false;
    };
  }

  // Returns the element type of t.
  //
  // The element type of an array type is the element type of its component type.
  //
  // The element type of every other kind of type is the type itself.
  private static final TypeMirror elementType(final TypeMirror t) {
    return t.getKind() == TypeKind.ARRAY ? elementType(((ArrayType)t).getComponentType()) : t;
  }

}
