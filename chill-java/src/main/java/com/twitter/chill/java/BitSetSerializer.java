/*
Copyright 2013 Twitter, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.twitter.chill.java;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import com.twitter.chill.IKryoRegistrar;
import com.twitter.chill.SingleRegistrar;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.BitSet;

public class BitSetSerializer extends Serializer<BitSet> implements Serializable {

    static public IKryoRegistrar registrar() {
      return new SingleRegistrar(BitSet.class, new BitSetSerializer());
    }

    private final static Field wordsField;
    private final static Constructor<BitSet> bitSetConstructor;
    private final static Method recalculateWordsInUseMethod;

    static {
        try {
            wordsField = BitSet.class.getDeclaredField("words");
            wordsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new KryoException("Error while getting field 'words' of bitSet", e);
        }
        try {
            bitSetConstructor = BitSet.class.getDeclaredConstructor(long[].class);
            bitSetConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new KryoException("Unable to get BitSet(long[]) constructor", e);
        }
        try {
            recalculateWordsInUseMethod = BitSet.class.getDeclaredMethod("recalculateWordsInUse");
            recalculateWordsInUseMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new KryoException("Unable to get BitSet.recalculateWordsInUse() method", e);
        }
    }

    @Override
    public void write(Kryo kryo, Output output, BitSet bitSet) {
        long words[] = null;
        // its sufficent to get only the 'words' field because
        // we can recompute the wordsInUse after deserialization
        try {
            words = (long[]) wordsField.get(bitSet);
        } catch (IllegalAccessException e) {
            throw new KryoException("Error while accessing field 'words' of bitSet", e);
        }
        output.writeInt(words.length, true);

        for(int i = 0; i < words.length; i++) {
            output.writeLong(words[i]);
        }
    }

    @Override
    public BitSet read(Kryo kryo, Input input, Class<BitSet> bitSetClass) {
        int len = input.readInt(true);
        long[] target = new long[len];

        for(int i = 0; i < len; i++) {
            target[i] = input.readLong();
        }

        BitSet ret = null;
        // call a private constructor: (the BitSet.valueOf() cTor is only available from Java 1.7)
        try {
            ret = bitSetConstructor.newInstance(target);
        } catch (ReflectiveOperationException e) {
            throw new KryoException("Unable to call BitSet(long[]) constructor", e);
        }
        try {
            recalculateWordsInUseMethod.invoke(ret);
        } catch (ReflectiveOperationException e) {
            throw new KryoException("Unable to call BitSet.recalculateWordsInUse() method", e);
        }

        return ret;
    }
}
