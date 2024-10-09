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
package org.microbean.bean.model;

import java.io.StringWriter;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import javax.lang.model.util.ElementFilter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.microbean.lang.element.DelegatingElement;

import org.microbean.lang.TypeAndElementSource;

import static org.microbean.lang.Lang.typeAndElementSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class TestElements {

  private static final TypeAndElementSource tes = typeAndElementSource();
  
  private TestElements() {
    super();
  }

  @Test
  final void testStreamDepthFirst() {
    final Element self = tes.typeElement(this.getClass().getName());
    assertTrue(self instanceof DelegatingElement);
    Trees.streamDepthFirst(self, Elements::parametersAndEnclosedElements)
      .forEachOrdered(e -> System.out.println(e.getSimpleName() + " (" + e.getKind() + ")"));
  }

  @Disabled // This works fine, but takes a good long while so is disabled by default.
  @Test
  final void testBigHorkingStream() {
    final Element javaBase = tes.moduleElement("java.base");
    assertNotNull(javaBase);
    Trees.streamDepthFirst(javaBase, Elements::parametersAndEnclosedElements)
      .forEachOrdered(e -> System.out.println(e.getSimpleName() + " (" + e.getKind() + ")"));
  }

  private static final void frob(int a, int b) {
    class goop {};
  }
  
}
