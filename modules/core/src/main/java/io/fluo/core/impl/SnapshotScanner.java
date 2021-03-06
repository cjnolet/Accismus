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
package io.fluo.core.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import io.fluo.accumulo.iterators.SnapshotIterator;
import io.fluo.accumulo.util.ColumnConstants;
import io.fluo.accumulo.values.WriteValue;
import io.fluo.api.config.ScannerConfiguration;
import io.fluo.api.data.Column;
import io.fluo.api.data.RowColumn;
import io.fluo.api.data.Span;
import io.fluo.core.exceptions.StaleScanException;
import io.fluo.core.util.ByteUtil;
import io.fluo.core.util.SpanUtil;
import io.fluo.core.util.UtilWaitThread;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

/**
 * 
 */
public class SnapshotScanner implements Iterator<Entry<Key,Value>> {

  private final long startTs;
  private final Environment env;
  private final TxStats stats;

  private Iterator<Entry<Key,Value>> iterator;
  private Entry<Key,Value> next;
  private ScannerConfiguration config;

  static final long INITIAL_WAIT_TIME = 50;
  // TODO make configurable
  static final long MAX_WAIT_TIME = 60000;

  public SnapshotScanner(Environment env, ScannerConfiguration config, long startTs, TxStats stats) {
    this.env = env;
    this.config = config;
    this.startTs = startTs;
    this.stats = stats;
    setUpIterator();
  }
  
  private void setUpIterator() {
    Scanner scanner;
    try {
      scanner = env.getConnector().createScanner(env.getTable(), env.getAuthorizations());
    } catch (TableNotFoundException e) {
      throw new RuntimeException(e);
    }
    scanner.clearColumns();
    scanner.clearScanIterators();
    
    scanner.setRange(SpanUtil.toRange(config.getSpan()));

    setupScanner(scanner, config.getColumns(), startTs);
    
    this.iterator = scanner.iterator();
  }

  static void setupScanner(ScannerBase scanner, List<Column> columns, long startTs) {
    for (Column col : columns) {
      if (col.getQualifier() != null) {
        scanner.fetchColumn(ByteUtil.toText(col.getFamily()), ByteUtil.toText(col.getQualifier()));
      } else {
        scanner.fetchColumnFamily(ByteUtil.toText(col.getFamily()));
      }
    }
    
    IteratorSetting iterConf = new IteratorSetting(10, SnapshotIterator.class);
    SnapshotIterator.setSnaptime(iterConf, startTs);
    scanner.addScanIterator(iterConf);
  }
  
  @Override
  public boolean hasNext() {
    if (next == null) {
      next = getNext();
    }
    
    return next != null;
  }
  
  @Override
  public Entry<Key,Value> next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    
    Entry<Key,Value> tmp = next;
    next = null;
    return tmp;
  }
  
  private void resetScanner(Span span) {
    try {
      config = (ScannerConfiguration) config.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }

    config.setSpan(span);
    setUpIterator();
  }

  public void resolveLock(Entry<Key,Value> lockEntry) {

    // read ahead a little bit looking for other locks to resolve

    long startTime = System.currentTimeMillis();
    long waitTime = INITIAL_WAIT_TIME;

    List<Entry<Key,Value>> locks = new ArrayList<>();
    locks.add(lockEntry);
    int amountRead = 0;
    int numRead = 0;

    RowColumn origEnd = config.getSpan().getEnd();
    boolean isEndInclusive = config.getSpan().isEndInclusive();

    while (true) {
      while (iterator.hasNext()) {
        Entry<Key,Value> entry = iterator.next();

        long colType = entry.getKey().getTimestamp() & ColumnConstants.PREFIX_MASK;

        if (colType == ColumnConstants.LOCK_PREFIX) {
          locks.add(entry);
        }

        amountRead += entry.getKey().getSize() + entry.getValue().getSize();
        numRead++;

        if (numRead > 100 || amountRead > 1 << 12) {
          break;
        }
      }

      boolean resolvedLocks = LockResolver.resolveLocks(env, startTs, stats, locks, startTime);

      if (!resolvedLocks) {
        UtilWaitThread.sleep(waitTime);
        stats.incrementLockWaitTime(waitTime);
        waitTime = Math.min(MAX_WAIT_TIME, waitTime * 2);

        RowColumn start = SpanUtil.toRowColumn(locks.get(0).getKey());
        RowColumn end = SpanUtil.toRowColumn(locks.get(locks.size() - 1).getKey()).following();

        resetScanner(new Span(start, true, end, false));

        locks.clear();

      } else {
        break;
      }
    }

    RowColumn start = SpanUtil.toRowColumn(lockEntry.getKey());

    resetScanner(new Span(start, true, origEnd, isEndInclusive));
  }

  public Entry<Key,Value> getNext() {
    mloop: while (true) {
      // its possible a next could exist then be rolled back
      if (!iterator.hasNext())
        return null;

      Entry<Key,Value> entry = iterator.next();

      long colType = entry.getKey().getTimestamp() & ColumnConstants.PREFIX_MASK;

      if (colType == ColumnConstants.LOCK_PREFIX) {
        resolveLock(entry);
        continue mloop;
      } else if (colType == ColumnConstants.DATA_PREFIX) {
        stats.incrementEntriesReturned(1);
        return entry;
      } else if (colType == ColumnConstants.WRITE_PREFIX) {
        if (WriteValue.isTruncated(entry.getValue().get())) {
          throw new StaleScanException();
        } else {
          throw new IllegalArgumentException();
        }
      } else {
        throw new IllegalArgumentException();
      }
    }
  }

  @Override
  public void remove() {
    iterator.remove();
  }
}
