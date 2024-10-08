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

@FunctionalInterface
interface ReferenceSelector {


  /*
   * Abstract methods.
   */


  // Yes, this is needed. Consider: the attributedType actually selects two beans. If you want a reference, you
  // have to say which one of the matched beans you want to use. You can't just use Id because you're going to need the
  // Factory<R> eventually.
  //
  // bean can be null, in which case the implementation has to use attributedType in some deliberately
  // unspecified way to find it.
  //
  // Must throw an exception if bean != null and it is not selected in some way by the attributedType
  public <R> R reference(final AttributedType attributedType, final Bean<R> bean, final Creation<R> creation);


  /*
   * Default methods.
   */

  public default <R> R reference(final AttributedType attributedType, final Creation<R> creation) {
    return this.reference(attributedType, null, creation);
  }

}
