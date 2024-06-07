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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultAutoCloseableRegistry implements AutoCloseableRegistry {

  private final Lock lock;

  // @GuardedBy("lock")
  private Set<AutoCloseable> closeables;

  public DefaultAutoCloseableRegistry() {
    super();
    this.lock = new ReentrantLock();
  }

  @Override // AutoCloseableRegistry
  public DefaultAutoCloseableRegistry newChild() {
    final DefaultAutoCloseableRegistry child = new DefaultAutoCloseableRegistry();
    if (!this.register(child)) { // CRITICAL
      throw new AssertionError();
    }
    return child;
  }

  @Override // AutoCloseableRegistry
  public final void close() {
    final Set<? extends AutoCloseable> closeables;
    lock.lock();
    try {
      closeables = this.closeables;
      if (closeables == Set.<AutoCloseable>of()) {
        // Already closed
        return;
      }
      this.closeables = Set.of();
    } finally {
      lock.unlock();
    }
    if (closeables == null) {
      // nothing to close
      return;
    }
    RuntimeException re = null;
    for (final AutoCloseable c : closeables) {
      try {
        c.close();
      } catch (final RuntimeException e) {
        if (re == null) {
          re = e;
        } else {
          re.addSuppressed(e);
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        if (re == null) {
          re = new BeanException(e.getMessage(), e);
        } else {
          re.addSuppressed(e);
        }
      } catch (final Exception e) {
        if (re == null) {
          re = new BeanException(e.getMessage(), e);
        } else {
          re.addSuppressed(e);
        }
      }
    }
    if (re != null) {
      throw re;
    }
  }

  @Override // AutoCloseableRegistry
  public final boolean register(final AutoCloseable closeable) {
    if (closeable == null || closeable == this) {
      return false;
    }
    lock.lock();
    try {
      if (this.closeables == Set.<AutoCloseable>of()) {
        // Already closed
        return false;
      } else if (this.closeables == null) {
        this.closeables = new LinkedHashSet<>();
      }
      return this.closeables.add(closeable);
    } finally {
      lock.unlock();
    }
  }

}
