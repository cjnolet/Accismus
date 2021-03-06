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
package io.fluo.api.types;

import io.fluo.api.data.Bytes;

/**
 * Transforms Java primitives to and from bytes using a String encoding
 */
public class StringEncoder implements Encoder {

  @Override
  public int decodeInteger(Bytes b) {
    return Integer.parseInt(decodeString(b));
  }

  @Override
  public Bytes encode(int i) {
    return encode(Integer.toString(i));
  }

  @Override
  public long decodeLong(Bytes b) {
    return Long.parseLong(decodeString(b));
  }

  @Override
  public Bytes encode(long l) {
    return encode(Long.toString(l));
  }

  @Override
  public String decodeString(Bytes b) {
    return b.toString();
  }

  @Override
  public Bytes encode(String s) {
    return Bytes.wrap(s);
  }

  @Override
  public float decodeFloat(Bytes b) {
    return Float.parseFloat(decodeString(b));
  }

  @Override
  public Bytes encode(float f) {
    return encode(Float.toString(f));
  }

  @Override
  public double decodeDouble(Bytes b) {
    return Double.parseDouble(decodeString(b));
  }

  @Override
  public Bytes encode(double d) {
    return encode(Double.toString(d));
  }

  @Override
  public boolean decodeBoolean(Bytes b) {
    return Boolean.parseBoolean(decodeString(b));
  }

  @Override
  public Bytes encode(boolean b) {
    return encode(Boolean.toString(b));
  }
}
