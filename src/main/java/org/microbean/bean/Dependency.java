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

import java.lang.constant.Constable;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;

import java.util.function.Function;

import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.microbean.lang.Lang;

import org.microbean.lang.element.DelegatingElement;

import static java.lang.constant.ConstantDescs.BSM_INVOKE;

import static org.microbean.bean.ConstantDescs.CD_BeanSelectionCriteria;
import static org.microbean.bean.ConstantDescs.CD_Dependency;

import static org.microbean.lang.ConstantDescs.CD_VariableElement;

/**
 * A dependency that a type has on some other {@linkplain BeanSelectionCriteria qualified type}, possibly at a
 * particular site represented by a {@link VariableElement} (representing, in turn, a field or parameter).
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 */
public record Dependency(BeanSelectionCriteria beanSelectionCriteria, VariableElement element) implements Constable {


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link Dependency}.
   *
   * @param beanSelectionCriteria a {@link BeanSelectionCriteria}; must not be {@code null}
   *
   * @exception NullPointerException if {@code beanSelectionCriteria} is {@code null}
   *
   * @see #Dependency(BeanSelectionCriteria, VariableElement)
   */
  public Dependency(final BeanSelectionCriteria beanSelectionCriteria) {
    this(beanSelectionCriteria, null);
  }

  public Dependency {
    if (element == null) {
      Objects.requireNonNull(beanSelectionCriteria, "beanSelectionCriteria");
    } else {
      switch (element.getKind()) {
      case FIELD:
      case PARAMETER:
        final Assignability a = beanSelectionCriteria.assignability();
        element = DelegatingElement.of(element, a.typeAndElementSource());
        beanSelectionCriteria = new BeanSelectionCriteria(a, element.asType(), beanSelectionCriteria.attributes(), beanSelectionCriteria.box());
        break;
      default:
        throw new IllegalArgumentException("element: " + element);
      }
    }
  }


  /*
   * Instance methods.
   */


  public final Assignment assign(final Request<?> r) {
    return new Assignment(this, r.reference(this.beanSelectionCriteria(), r));
  }

  @Override // Constable
  public final Optional<DynamicConstantDesc<Dependency>> describeConstable() {
    return Lang.describeConstable(this.element())
      .flatMap(eDesc -> this.beanSelectionCriteria().describeConstable()
               .map(bscDesc -> DynamicConstantDesc.of(BSM_INVOKE,
                                                      MethodHandleDesc.ofConstructor(CD_Dependency,
                                                                                     CD_BeanSelectionCriteria,
                                                                                     CD_VariableElement),
                                                      bscDesc,
                                                      eDesc)));
  }

  // Returns a negative value for a field; otherwise a zero-based index for an executable parameter
  @SuppressWarnings("fallthrough")
  public final int index() {
    final Element e = this.element();
    switch (e.getKind()) {
    case FIELD:
      return -1;
    case PARAMETER:
      final CharSequence name = e.getSimpleName();
      int i = 0;
      for (final Element p : ((ExecutableElement)e.getEnclosingElement()).getParameters()) {
        if (p.getSimpleName().contentEquals(name)) {
          return i;
        }
        i++;
      }
      // fall through
    default:
      throw new AssertionError();
    }
  }


  /*
   * Static methods.
   */


  public static final Dependency of(final BeanSelectionCriteria bsc) {
    return new Dependency(bsc);
  }

  public static final Dependency of(final BeanSelectionCriteria bsc, final VariableElement e) {
    return new Dependency(bsc, e);
  }

  public static final SequencedSet<Dependency> dependencies(final Element e,
                                                            final Function<? super VariableElement, ? extends BeanSelectionCriteria> f) {
    switch (e.getKind()) {
    case FIELD:
    case PARAMETER:
      return dependencies((VariableElement)e, f);
    case CONSTRUCTOR:
    case METHOD:
      return dependencies((ExecutableElement)e, f);
    default:
      return dependencies(e.getEnclosedElements().stream().filter(ee -> switch (ee.getKind()) {
          case FIELD -> true;
          default -> false;
          })
        .map(ve -> (VariableElement)ve),
        f);
    }
  }

  public static final SequencedSet<Dependency> dependencies(final VariableElement e,
                                                            final Function<? super VariableElement, ? extends BeanSelectionCriteria> f) {
    return dependencies(Stream.of(e), f);
  }

  // Parameters
  public static final SequencedSet<Dependency> dependencies(final ExecutableElement ee,
                                                            final Function<? super VariableElement, ? extends BeanSelectionCriteria> f) {
    return dependencies(ee.getParameters().stream(), f);
  }

  public static final SequencedSet<Dependency> dependencies(final Stream<? extends VariableElement> s,
                                                            final Function<? super VariableElement, ? extends BeanSelectionCriteria> f) {
    return s
      .filter(ve -> switch (ve.getKind()) {
        case FIELD, PARAMETER -> true;
        default -> false;
        })
      .map(ve -> of(f.apply(ve), ve))
      .collect(Collector.<Dependency, LinkedHashSet<Dependency>, SequencedSet<Dependency>>of(LinkedHashSet::new,
                                                                                             LinkedHashSet::add,
                                                                                             (l, r) -> { l.addAll(r); return l; },
                                                                                             Collections::unmodifiableSequencedSet));
  }

}
