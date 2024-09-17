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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import java.util.function.Predicate;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.microbean.constant.Constables;

import org.microbean.lang.Equality;
import org.microbean.lang.NameTypeMirrorComparator;
import org.microbean.lang.SameTypeEquality;
import org.microbean.lang.SpecializationDepthTypeMirrorComparator;
import org.microbean.lang.TypeAndElementSource;

import org.microbean.lang.type.DelegatingTypeMirror;

import org.microbean.lang.visitor.Visitors;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_int;
import static java.lang.constant.ConstantDescs.CD_List;

import static org.microbean.lang.ConstantDescs.CD_Equality;
import static org.microbean.lang.ConstantDescs.CD_TypeAndElementSource;

@Deprecated(forRemoval = true)
class ReferenceTypeList implements Constable {

  final Equality equality;

  final TypeAndElementSource tes;

  final int classesIndex;

  final int arraysIndex;

  final int interfacesIndex;

  private final List<DelegatingTypeMirror> types;

  private final int hashCode;

  ReferenceTypeList(final Collection<? extends TypeMirror> types,
                    final TypeAndElementSource tes) {
    this(types, ReferenceTypeList::validateType, tes, null);
  }

  ReferenceTypeList(final Collection<? extends TypeMirror> types,
                    final TypeAndElementSource tes,
                    final Equality equality) {
    this(types, ReferenceTypeList::validateType, tes, equality);
  }

  ReferenceTypeList(final Collection<? extends TypeMirror> types,
                    final Predicate<? super DelegatingTypeMirror> typeValidator,
                    final TypeAndElementSource tes,
                    final Equality equality) {
    super();    
    this.tes = Objects.requireNonNull(tes, "tes");
    this.equality = equality == null ? new SameTypeEquality(tes) : equality;
    if (types.isEmpty()) {
      this.types = List.of();
      this.classesIndex = -1;
      this.arraysIndex = -1;
      this.interfacesIndex = -1;
    } else {
      final ArrayList<DelegatingTypeMirror> newTypes = new ArrayList<>(types.size());
      for (final TypeMirror t : types) {
        final DelegatingTypeMirror dt = DelegatingTypeMirror.of(t, tes, this.equality);
        if (!this.seen(newTypes, dt) && typeValidator.test(dt)) {
          newTypes.add(dt);
        }
      }
      if (newTypes.isEmpty()) {
        this.types = List.of();
        this.classesIndex = -1;
        this.arraysIndex = -1;
        this.interfacesIndex = -1;
      } else {
        Collections.sort(newTypes,
                         // Sort first by precedence:
                         //  type variables precede non-interfaces
                         //  non-interfaces precede arrays
                         //  arrays precede interfaces
                         TypeKindComparator.INSTANCE
                         // Sort next by specialization depth
                         .thenComparing(new SpecializationDepthTypeMirrorComparator(tes, this.equality))
                         // Sort last by name
                         .thenComparing(NameTypeMirrorComparator.INSTANCE));
        int classesIndex = -1;
        int arraysIndex = -1;
        int interfacesIndex = -1;
        LOOP:
        for (int i = 0; i < newTypes.size(); i++) {
          final DelegatingTypeMirror newType = newTypes.get(i);
          switch (newType.getKind()) {
          case ARRAY:
            if (arraysIndex < 0) {
              arraysIndex = i;
            }
            break;
          case DECLARED:
            if (newType.asElement().getKind().isInterface()) {
              interfacesIndex = i;
              break LOOP;
            } else if (classesIndex < 0) {
              classesIndex = i;
            }
            break;
          case TYPEVAR:
            break;
          default:
            throw new AssertionError("non-reference type: " + newType);
          }
        }
        newTypes.trimToSize();
        this.types = Collections.unmodifiableList(newTypes);
        this.classesIndex = classesIndex;
        this.arraysIndex = arraysIndex;
        this.interfacesIndex = interfacesIndex;
      }
    }
    this.hashCode = this.types.hashCode();
  }

  // Deliberately unvalidated constructor for use by describeConstable() only.
  @Deprecated
  ReferenceTypeList(final List<DelegatingTypeMirror> types,
                    final int classesIndex,
                    final int arraysIndex,
                    final int interfacesIndex,
                    final Equality equality,
                    final TypeAndElementSource tes) {
    super();
    this.types = types;
    this.classesIndex = classesIndex;
    this.arraysIndex = arraysIndex;
    this.interfacesIndex = interfacesIndex;
    this.equality = equality;
    this.tes = tes;
    this.hashCode = types.hashCode();
  }

  @Override // Constable
  public Optional<DynamicConstantDesc<?>> describeConstable() {
    return Constables.describeConstable(this.types(), this.tes::describeConstable)
      .flatMap(typesDesc -> this.equality.describeConstable()
               .flatMap(equalityDesc -> (this.tes instanceof Constable c ? c.describeConstable() : Optional.<ConstantDesc>empty())
                        .map(tesDesc -> DynamicConstantDesc.of(BSM_INVOKE,
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

  public final List<? extends DelegatingTypeMirror> types() {
    return this.types;
  }

  public final List<? extends DelegatingTypeMirror> classes() {
    if (this.classesIndex < 0) {
      return List.of();
    } else if (this.arraysIndex < 0) {
      if (this.classesIndex == 0) {
        return this.interfacesIndex < 0 ? this.types : this.types.subList(0, this.interfacesIndex);
      }
      return this.types.subList(this.classesIndex, this.interfacesIndex < 0 ? this.types.size() : this.interfacesIndex);
    }
    return this.types.subList(this.classesIndex, this.arraysIndex);
  }

  public final List<? extends DelegatingTypeMirror> arrays() {
    if (this.arraysIndex < 0) {
      return List.of();
    } else if (this.arraysIndex == 0) {
      return this.interfacesIndex < 0 ? this.types : this.types.subList(0, this.interfacesIndex);
    }
    return this.types.subList(this.arraysIndex, this.interfacesIndex < 0 ? this.types.size() : this.interfacesIndex);
  }

  public final List<? extends DelegatingTypeMirror> interfaces() {
    if (this.interfacesIndex < 0) {
      return List.of();
    } else if (this.interfacesIndex == 0) {
      return this.types;
    }
    return this.types.subList(this.interfacesIndex, this.types.size());
  }

  public final List<? extends DelegatingTypeMirror> typeVariables() {
    return this.classesIndex <= 0 ? List.of() : this.types.subList(0, this.classesIndex);
  }

  @Override // Object
  public int hashCode() {
    return this.hashCode; // each TypeMirror will be a DelegatingTypeMirror using value-based hashCode semantics
  }

  @Override // Object
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other != null && other.getClass() == this.getClass()) {
      return Objects.equals(this.types(), ((ReferenceTypeList)other).types()); // each TypeMirror will be a DelegatingTypeMirror using proper equality semantics
    } else {
      return false;
    }
  }

  @Override // Object
  public String toString() {
    return String.valueOf(this.types());
  }

  private final boolean seen(final Iterable<? extends DelegatingTypeMirror> seen, final DelegatingTypeMirror t) {
    for (final TypeMirror s : seen) {
      if (s == t || this.equality.equals(s, t)) {
        return true;
      }
    }
    return false;
  }


  /*
   * Static methods.
   */


  public static ReferenceTypeList closure(final TypeMirror t,
                                          final TypeAndElementSource tes) {
    return closure(t, new Visitors(tes));
  }

  public static ReferenceTypeList closure(final TypeMirror t,
                                          final Visitors visitors) {
    final TypeAndElementSource tes = visitors.typeAndElementSource();
    return new ReferenceTypeList(visitors.typeClosureVisitor().visit(DelegatingTypeMirror.of(t, tes)).toList(),
                                 tes);
  }

  // Every so often and only under parallel testing:
  //
  // java.lang.NullPointerException: Cannot invoke "javax.lang.model.type.TypeMirror.getKind()" because "t" is null
  //  at org.microbean.bean@0.0.1-SNAPSHOT/org.microbean.bean.ReferenceTypeList.validateType(ReferenceTypeList.java:305)
  //  at org.microbean.bean@0.0.1-SNAPSHOT/org.microbean.bean.ReferenceTypeList.<init>(ReferenceTypeList.java:116)
  //  at org.microbean.bean@0.0.1-SNAPSHOT/org.microbean.bean.ReferenceTypeList.<init>(ReferenceTypeList.java:83)
  //  at org.microbean.bean@0.0.1-SNAPSHOT/org.microbean.bean.TestReferenceTypeList.testSorting(TestReferenceTypeList.java:71)
  static final boolean validateType(final DelegatingTypeMirror t) {
    return switch (t.getKind()) {
    case ARRAY, DECLARED, TYPEVAR -> true;
    default -> throw new IllegalArgumentException("t is not a suitable reference type: " + t);
    };
  }


  /*
   * Inner and nested classes.
   */


  private static final class TypeKindComparator implements Comparator<TypeMirror> {

    private static final TypeKindComparator INSTANCE = new TypeKindComparator();

    private TypeKindComparator() {
      super();
    }

    @Override // Comparator<TypeMirror>
    public final int compare(final TypeMirror t, final TypeMirror s) {
      return t != null && t == s ? 0 : switch (t.getKind()) {
        case ARRAY -> switch (s.getKind()) {
          case ARRAY -> 0;
          // arrays precede interfaces; non-interfaces precede arrays:
          case DECLARED -> ((DeclaredType)s).asElement().getKind().isInterface() ? -1 : 1;
          // type variables precede arrays
          case TYPEVAR -> 1;
          default -> throw new IllegalArgumentException("s: " + s);
        };

        case DECLARED -> switch (s.getKind()) {
          // non-interfaces precede arrays; arrays precede interfaces:
          case ARRAY -> ((DeclaredType)t).asElement().getKind().isInterface() ? 1 : -1;
          // non-interfaces precede interfaces:
          case DECLARED -> ((DeclaredType)t).asElement().getKind().isInterface() ?
            ((DeclaredType)s).asElement().getKind().isInterface() ? 0 : 1 :
            ((DeclaredType)s).asElement().getKind().isInterface() ? -1 : 0;
          // type variables precede non-interfaces and interfaces:
          case TYPEVAR -> 1;
          default -> throw new IllegalArgumentException("s: " + s);
        };

        case TYPEVAR -> switch (s.getKind()) {
          // type variables precede non-interfaces, arrays and interfaces:
          case ARRAY, DECLARED -> -1;
          case TYPEVAR -> 0;
          default -> throw new IllegalArgumentException("s: " + s);
        };

        default -> throw new IllegalArgumentException("t: " + t);
      };
    }

  }



}
