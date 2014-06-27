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
package org.fluo.api.mapreduce;

import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
import org.apache.accumulo.core.client.mapreduce.RangeInputSplit;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.data.ArrayByteSequence;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Range;
import org.apache.hadoop.mapreduce.*;
import org.fluo.api.ColumnIterator;
import org.fluo.api.RowIterator;
import org.fluo.api.ScannerConfiguration;
import org.fluo.api.config.ConnectionProperties;
import org.fluo.impl.Configuration;
import org.fluo.impl.OracleClient;
import org.fluo.impl.TransactionImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * This input format reads a consistent snapshot from an Fluo table.
 */
public class FluoInputFormat extends InputFormat<ByteSequence,ColumnIterator> {
  
  private static String TIMESTAMP_CONF_KEY = FluoInputFormat.class.getName() + ".timestamp";
  private static String PROPS_CONF_KEY = FluoInputFormat.class.getName() + ".props";
  private static String FAMS_CONF_KEY = FluoInputFormat.class.getName() + ".families";


  @Override
  public RecordReader<ByteSequence,ColumnIterator> createRecordReader(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {

    return new RecordReader<ByteSequence,ColumnIterator>() {
      
      private Entry<ByteSequence,ColumnIterator> entry;
      private RowIterator rowIter;
      private Configuration fluoConf = null;
      
      @Override
      public void close() throws IOException {
        if (fluoConf != null)
          fluoConf.getSharedResources().close();
      }
      
      @Override
      public ByteSequence getCurrentKey() throws IOException, InterruptedException {
        return entry.getKey();
      }
      
      @Override
      public ColumnIterator getCurrentValue() throws IOException, InterruptedException {
        return entry.getValue();
      }
      
      @Override
      public float getProgress() throws IOException, InterruptedException {
        // TODO Auto-generated method stub
        return 0;
      }
      
      @Override
      public void initialize(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
        try {
          // TODO this uses non public Accumulo API!
          RangeInputSplit ris = (RangeInputSplit) split;
          
          Range range = ris.getRange();
          
          ByteArrayInputStream bais = new ByteArrayInputStream(context.getConfiguration().get(PROPS_CONF_KEY).getBytes("UTF-8"));
          Properties props = new Properties();
          props.load(bais);
          
          fluoConf = new Configuration(props);
          
          TransactionImpl ti = new TransactionImpl(fluoConf, context.getConfiguration().getLong(TIMESTAMP_CONF_KEY, -1));
          ScannerConfiguration sc = new ScannerConfiguration().setRange(range);
          
          for (String fam : context.getConfiguration().getStrings(FAMS_CONF_KEY, new String[0]))
            sc.fetchColumnFamily(new ArrayByteSequence(fam));

          rowIter = ti.get(sc);
        } catch (Exception e) {
          throw new IOException(e);
        }
      }
      
      @Override
      public boolean nextKeyValue() throws IOException, InterruptedException {
        if (rowIter.hasNext()) {
          entry = rowIter.next();
          return true;
        }
        return false;
      }
    };

  }

  @Override
  public List<InputSplit> getSplits(JobContext context) throws IOException, InterruptedException {
    return new AccumuloInputFormat().getSplits(context);
  }
  
  /**
   * Configure properties needed to connect to an Fluo instance
   * 
   * @param conf
   * @param props
   *          use {@link ConnectionProperties} to configure programmatically
   */
  public static void configure(Job conf, Properties props) {
    try {
      
      Configuration fluoConf = new Configuration(props);
      
      OracleClient client = OracleClient.getInstance(fluoConf);
      long ts = client.getTimestamp();
      
      conf.getConfiguration().setLong(TIMESTAMP_CONF_KEY, ts);
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      props.store(baos, "");
      
      conf.getConfiguration().set(PROPS_CONF_KEY, new String(baos.toByteArray(), "UTF8"));
      
      AccumuloInputFormat.setZooKeeperInstance(conf, props.getProperty(ConnectionProperties.ACCUMULO_INSTANCE_PROP), props.getProperty(ConnectionProperties.ZOOKEEPER_CONNECT_PROP));
      AccumuloInputFormat.setConnectorInfo(conf, props.getProperty(ConnectionProperties.ACCUMULO_USER_PROP), new PasswordToken(props.getProperty(ConnectionProperties.ACCUMULO_PASSWORD_PROP)));
      AccumuloInputFormat.setInputTableName(conf, fluoConf.getTable());
      AccumuloInputFormat.setScanAuthorizations(conf, fluoConf.getAuthorizations());
      
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // TODO support text
  public static void fetchFamilies(Job job, String... fams) {
    job.getConfiguration().setStrings(FAMS_CONF_KEY, fams);
  }

  public static void fetchFamilies(Job job, ByteSequence... fams) {
    // TODO support binary data
    String sfams[] = new String[fams.length];
    for (int i = 0; i < sfams.length; i++) {
      sfams[i] = fams[i].toString();
    }
    fetchFamilies(job, sfams);
  }

  // TODO let user set auths
  // TODO let user set ranges
  

}
