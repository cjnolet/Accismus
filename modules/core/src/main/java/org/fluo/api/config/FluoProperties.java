/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fluo.api.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

/**
 * The class helps create a properties object with the key/values required to connect to an Fluo instance.
 */
public class FluoProperties extends Properties {
  
  private static final long serialVersionUID = 1L;

  public static final String ACCUMULO_PASSWORD_PROP = "org.fluo.accumulo.password";
  public static final String ACCUMULO_USER_PROP = "org.fluo.accumulo.user";
  public static final String ACCUMULO_INSTANCE_PROP = "org.fluo.accumulo.instance";

  public static final String ZOOKEEPER_ROOT_PROP = "org.fluo.zookeeper.root";
  public static final String ZOOKEEPER_TIMEOUT_PROP = "org.fluo.zookeeper.timeout";
  public static final String ZOOKEEPER_CONNECT_PROP = "org.fluo.zookeeper.connect";
  
  public FluoProperties() {
    super(org.fluo.impl.Configuration.getDefaultProperties());
  }
  
  public FluoProperties(File file) throws FileNotFoundException, IOException {
    this();
    load(new FileInputStream(file));
  }

  public FluoProperties(Properties props) {
    super(props);
    Set<java.util.Map.Entry<Object,Object>> es = org.fluo.impl.Configuration.getDefaultProperties().entrySet();
    for (java.util.Map.Entry<Object,Object> entry : es) {
      setDefault((String) entry.getKey(), (String) entry.getValue());
    }
  }

  public FluoProperties setZookeepers(String zookeepers) {
    setProperty(ZOOKEEPER_CONNECT_PROP, zookeepers);
    return this;
  }
  
  public FluoProperties setTimeout(int timeout) {
    setProperty(ZOOKEEPER_TIMEOUT_PROP, timeout + "");
    return this;
  }
  
  public FluoProperties setZookeeperRoot(String zookeeperRoot) {
    setProperty(ZOOKEEPER_ROOT_PROP, zookeeperRoot);
    return this;
  }
  
  public FluoProperties setAccumuloInstance(String accumuloInstance) {
    setProperty(ACCUMULO_INSTANCE_PROP, accumuloInstance);
    return this;
  }
  
  public FluoProperties setAccumuloUser(String accumuloUser) {
    setProperty(ACCUMULO_USER_PROP, accumuloUser);
    return this;
  }
  
  public FluoProperties setAccumuloPassword(String accumuloPassword) {
    setProperty(ACCUMULO_PASSWORD_PROP, accumuloPassword);
    return this;
  }
    
  protected void setDefault(String key, String val) {
    if (getProperty(key) == null)
      setProperty(key, val);
  }

}
