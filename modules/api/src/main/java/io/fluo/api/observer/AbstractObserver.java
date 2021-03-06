/*
 * Copyright 2014 Fluo authors (see AUTHORS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fluo.api.observer;

import java.util.Map;

import io.fluo.api.client.TransactionBase;

/**
 * Implemented by users to a watch a {@link Column} and be notified of changes to the Column via the {@link #process(TransactionBase, Bytes, Column)} method.
 * AbstractObserver extends {@link Observer} but provides a default implementation for the {@link #init(Map)} and {@link #close()} method so that they can be
 * optionally implemented by user.
 */
public abstract class AbstractObserver implements Observer {

  /**
   * Optionally implemented by users to initialize {@link Observer}
   */
  @Override
  public void init(Map<String,String> config) throws Exception {}

  /**
   * Optionally implemented by users to close resources used by {@link Observer}
   */
  @Override
  public void close() {}
}
