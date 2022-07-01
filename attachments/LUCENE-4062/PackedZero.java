package org.apache.lucene.util.packed;

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

/**
 * Dummy Mutable meant for calibrating PackedIntsBenchmark.
 */
class PackedZero extends PackedInts.MutableImpl {
  // We need to do something persistent with the values received from set
  // so that the method is not JITted away completely.
  private long jitTricker;

  protected PackedZero(int valueCount, int bitsPerValue) {
    super(valueCount, bitsPerValue);
  }

  @Override
  public void set(int index, long value) {
    jitTricker += value;
  }

  @Override
  public void clear() {
    jitTricker = 0;
  }

  @Override
  public long get(int index) {
    return jitTricker;
  }

  @Override
  public long ramBytesUsed() {
    return 0;
  }
}
