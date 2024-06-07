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

public interface ReferenceSelector {


  /*
   * Abstract methods.
   */


  // Yes, this is needed. Consider: the beanSelectionCriteria actually selects two beans. If you want a reference, you
  // have to say which one of the matched beans you want to use. You can't just use Id because you're going to need the
  // Factory<R> eventually.
  public <R> R reference(final BeanSelectionCriteria beanSelectionCriteria, final Bean<R> bean, final Creation<R> creation);

  /*
   * Default methods.
   */

  public default <R> R reference(final BeanSelection<R> beanSelection, final Creation<R> creation) {
    return this.reference(beanSelection.beanSelectionCriteria(), beanSelection.bean(), creation);
  }

  public default <R> R reference(final BeanSelectionCriteria beanSelectionCriteria, final Creation<R> creation) {
    return this.reference(beanSelectionCriteria, null, creation);
  }

}
