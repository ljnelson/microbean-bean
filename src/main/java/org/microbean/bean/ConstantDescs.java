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

public final class ConstantDescs {

  public static final ClassDesc CD_Bean = ClassDesc.of("org.microbean.bean.Bean");

  public static final ClassDesc CD_Beans = ClassDesc.of("org.microbean.bean.Beans");

  public static final ClassDesc CD_BeanSelectionCriteria = ClassDesc.of("org.microbean.bean.BeanSelectionCriteria");

  public static final ClassDesc CD_BeanTypeList = ClassDesc.of("org.microbean.bean.BeanTypeList");

  public static final ClassDesc CD_Dependency = ClassDesc.of("org.microbean.bean.Dependency");

  public static final ClassDesc CD_Factory = ClassDesc.of("org.microbean.bean.Factory");

  public static final ClassDesc CD_Id = ClassDesc.of("org.microbean.bean.Id");

  public static final ClassDesc CD_ReferenceTypeList = ClassDesc.of("org.microbean.bean.ReferenceTypeList");

  public static final ClassDesc CD_TypeMatcher = ClassDesc.of("org.microbean.bean.TypeMatcher");

  private ConstantDescs() {
    super();
  }

}
