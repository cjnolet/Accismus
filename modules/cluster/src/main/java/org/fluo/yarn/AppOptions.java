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
package org.fluo.yarn;

import com.beust.jcommander.Parameter;

public class AppOptions {
  
  @Parameter(names = "-fluo-home", description = "Location of org.fluo.accismus home", required = true)
  private String accismusHome;

  @Parameter(names = "-hadoop-prefix", description = "Location of hadoop prefix", required = true)
  private String hadoopPrefix;
  
  @Parameter(names = {"-h", "-help", "--help"}, help = true, description = "Prints help")
  public boolean help;
  
  public String getFluoHome() {
    return accismusHome;
  }
  
  public String getHadoopPrefix() {
    return hadoopPrefix;
  }
}
