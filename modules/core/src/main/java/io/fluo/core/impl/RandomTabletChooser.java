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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.hadoop.io.Text;

/**
 * 
 */
public class RandomTabletChooser {
  
  private static final long CACHE_TIME = 5 * 60 * 1000;

  private final Environment env;
  private final Random rand = new Random();
  private List<TabletInfo> cachedTablets;
  private long listSplitsTime = 0;
  
  static class TabletInfo {
    final Text start;
    final Text end;
    final Lock lock = new ReentrantLock();
    long retryTime;
    long sleepTime = 100;
    
    TabletInfo(Text start, Text end) {
      this.start = start;
      this.end = end;
    }
    
    private int hashCode(Text t) {
      if (t == null)
        return 0;
      return t.hashCode();
    }

    @Override
    public int hashCode() {
      return hashCode(start) + hashCode(end);
    }
    
    private boolean equals(Text t1, Text t2) {
      if (t1 == t2)
        return true;
      
      if (t1 == null || t2 == null)
        return false;
      
      return t1.equals(t2);
    }
    
    @Override
    public boolean equals(Object o) {
      if (o instanceof TabletInfo) {
        TabletInfo oti = (TabletInfo) o;
        
        if (equals(start, oti.start)) {
          return equals(end, oti.end);
        }
        
        return false;
      }
      
      return false;
    }

  }
  
  public RandomTabletChooser(Environment env) {
    this.env = env;
  }

  private List<TabletInfo> listSplits() throws TableNotFoundException, AccumuloSecurityException, AccumuloException {
    List<Text> splits = new ArrayList<>(env.getConnector().tableOperations().listSplits(env.getTable()));
    Collections.sort(splits);
    
    List<TabletInfo> tablets = new ArrayList<>(splits.size() + 1);
    for (int i = 0; i < splits.size(); i++) {
      tablets.add(new TabletInfo(i == 0 ? null : splits.get(i - 1), splits.get(i)));
    }
    
    tablets.add(new TabletInfo(splits.size() == 0 ? null : splits.get(splits.size() - 1), null));
    listSplitsTime = System.currentTimeMillis();
    return tablets;
  }
  
  private synchronized List<TabletInfo> getTablets() throws Exception {
    if (cachedTablets == null) {
      cachedTablets = listSplits();
    } else if (System.currentTimeMillis() - listSplitsTime > CACHE_TIME) {
      List<TabletInfo> tablets = listSplits();
      Map<TabletInfo,TabletInfo> oldTablets = new HashMap<>();
      for (TabletInfo tabletInfo : cachedTablets) {
        oldTablets.put(tabletInfo, tabletInfo);
      }
      
      List<TabletInfo> newTablets = new ArrayList<>(tablets.size());
      
      for (TabletInfo tabletInfo : tablets) {
        TabletInfo oldTI = oldTablets.get(tabletInfo);
        if (oldTI != null)
          newTablets.add(oldTI);
        else
          newTablets.add(tabletInfo);
      }
      
      cachedTablets = newTablets;
    }
    
    return cachedTablets;
  }

  synchronized TabletInfo getRandomTablet() throws Exception {
    List<TabletInfo> tablets = getTablets();
    TabletInfo ti = tablets.get(rand.nextInt(tablets.size()));
    if (ti.lock.tryLock())
      return ti;
    else
      return null;
  }
}
