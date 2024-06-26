/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zeppelin.flink.shims118;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.experimental.CollectSink;
import org.apache.flink.table.sinks.RetractStreamTableSink;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Table sink for collecting the results locally using sockets.
 */
public class CollectStreamTableSink implements RetractStreamTableSink<Row> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CollectStreamTableSink.class);

  private final InetAddress targetAddress;
  private final int targetPort;
  private final TypeSerializer<Tuple2<Boolean, Row>> serializer;

  private String[] fieldNames;
  private TypeInformation<?>[] fieldTypes;

  public CollectStreamTableSink(InetAddress targetAddress,
                                int targetPort,
                                TypeSerializer<Tuple2<Boolean, Row>> serializer) {
    LOGGER.info("Use address: " + targetAddress.getHostAddress() + ":" + targetPort);
    this.targetAddress = targetAddress;
    this.targetPort = targetPort;
    this.serializer = serializer;
  }

  @Override
  public String[] getFieldNames() {
    return fieldNames;
  }

  @Override
  public TypeInformation<?>[] getFieldTypes() {
    return fieldTypes;
  }

  @Override
  public CollectStreamTableSink configure(String[] fieldNames, TypeInformation<?>[] fieldTypes) {
    final CollectStreamTableSink copy =
            new CollectStreamTableSink(targetAddress, targetPort, serializer);
    copy.fieldNames = fieldNames;
    copy.fieldTypes = fieldTypes;
    return copy;
  }

  @Override
  public TypeInformation<Row> getRecordType() {
    return Types.ROW_NAMED(fieldNames, fieldTypes);
  }

  @Override
  public DataStreamSink<?> consumeDataStream(DataStream<Tuple2<Boolean, Row>> stream) {
    // add sink
    return stream
            .addSink(new CollectSink<>(targetAddress, targetPort, serializer))
            .name("Zeppelin Flink Sql Stream Collect Sink " + UUID.randomUUID())
            .setParallelism(1);
  }

  @Override
  public TupleTypeInfo<Tuple2<Boolean, Row>> getOutputType() {
    return new TupleTypeInfo<>(Types.BOOLEAN, getRecordType());
  }
}
