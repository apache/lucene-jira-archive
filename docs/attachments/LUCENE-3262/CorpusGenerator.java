package org.apache.lucene.facet.example;

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

import java.util.Random;

/**
 * Generates hierarchical paths used for building a corpus for testing.
 * Currently the most advanced form of generation is binomial, which is not
 * that advanced at all. A Zipf-generator would be an nice extension.
 * </p><p>
 * The overall principle for the generator is that the number of paths, the
 * depth of each path and the content of each path are created by three
 * producers. The producers can be independent or intertwined. They are building
 * blocks for flexible corpus setups.
 */
public class CorpusGenerator {
  private IntProducer paths = new Binomial(new Random(), 1, 1, 1.0);
  private IntProducer depth = new Binomial(new Random(), 2, 2, 1.0);
  private StringProducer tags = new StaticProducer("A");

  /**
   * Generate an array of paths for a given docID.
   * @param docID depending on setup this is either ignored or used as a seed
   *              for the paths.
   * @return values[pathIndex][pathElements]
   */
  public String[][] getPaths(final int docID) {
    final String[][] result = new String[paths.getValue(docID)][];
    for (int p = 0; p < result.length; p++) {
      result[p] = new String[depth.getValue(docID, p)];
      for (int d = 0 ; d < result[p].length ; d++) {
        result[p][d] = tags.getValue(docID, p, d);
      }
    }
    return result;
  }

  public static interface StringProducer {
    public String getValue(int... seeds);
  }

  public static interface IntProducer {
    public int getValue(int... seeds);
  }

  public abstract static class RandomizerImpl implements IntProducer {
    protected final Random random;

    public RandomizerImpl(Random random) {
      this.random = random;
    }
  }

  /**
   * Trivial producer that returns a single specific tag for each level.
   */
  public static class StaticProducer implements StringProducer {
    final String defaultValue;
    final String values[];

    /**
     * @param defaultValue the fallback value if there are no seeds or the
     *                     first seed is seed >= values.length.
     * @param values       used as {@code values[seed[0]]}.
     */
    public StaticProducer(String defaultValue, String... values) {
      this.defaultValue = defaultValue;
      this.values = values;
    }

    public String getValue(int... seeds) { // docID, pathNum, depth(used)
       return seeds.length < 3 || seeds[2] >= values.length ?
             defaultValue : values[seeds[2]];
    }
  }

  /**
   * Simple producer that takes an IntProducer and returns the String
   * representation of the generated integers.
   */
  public static class SimplePathElementProducer implements StringProducer {
    private final String prefix;
    private final IntProducer producer;

    public SimplePathElementProducer(String prefix, IntProducer producer) {
      this.prefix = prefix;
      this.producer = producer;
    }

    public String getValue(int... seeds) {
      return prefix + producer.getValue(seeds);
    }
  }

  public static class LevelWrapper implements IntProducer {
    private final int level;
    private final IntProducer[] producers;

    /**
     *
     * @param level the seed number to use for selecting producer,
     *              counting from 0.
     * @param producers the producers at the values, selected with
     *                  {@code producers[seeds[level]]} from
     *                  {@link #getValue(int...)}.
     */
    public LevelWrapper(int level, IntProducer... producers) {
      this.level = level;
      this.producers = producers;
    }

    public int getValue(int... seeds) {
      if (seeds.length <= level) {
        throw new IllegalArgumentException(
            "There was only " + seeds.length + " seeds while the stated seed"
            + " level was " + level);
      }
      if (producers.length <= seeds[level]) {
        throw new IllegalArgumentException(
            "There was only " + producers.length + " producers while the "
            + "stated index from seeds[" + level + "] was " + seeds[level]);
      }
      return producers[seeds[level]].getValue(seeds);
    }
  }

  /**
   * Plain implementation that returns a pseudo-random value from min to max,
   * both inclusive.
   * Example case: Selecting a number from 1 to 10.
   */
  public static class SimpleRandom extends RandomizerImpl {
    private final int min;
    private final int max;

    public SimpleRandom(Random random, int min, int max) {
      super(random);
      this.min = min;
      this.max = max;
    }

    public int getValue(int... seeds) {
      return (min + random.nextInt((max - min + 1)));
    }
  }
  /**
   * Randomizer that produces binomially distributed values.
   * Example case: Tossing a coin 10 times and counting tails.
   */
  public static class Binomial extends RandomizerImpl {
    private final int min;
    private final int max;
    private final double probability;

    /**
     * The result value is set to min. A random value between 0 and 1 is
     * generated (max - min) times. Every time the random value is below
     * probability, the result value is increased by 1.
     *
     * @param random      the generator for random values from 0 to 1.
     * @param min         the minimum value to return.
     * @param max         the maximum value to return.
     * @param probability the probability for the value to be increased.
     */
    public Binomial(Random random, int min, int max, double probability) {
      super(random);
      this.min = min;
      this.max = max;
      this.probability = probability;
    }

    public int getValue(int... seeds) { // seeds are ignored here
      int value = min;
      for (int i = 0; i < (max - min); i++) {
        if (random.nextDouble() < probability) {
          value++;
        }
      }
      return value;
    }
  }

  /**
   * @param producer generates the number of paths for any given document.
   *        The docID is given as seed.
   * @return the encapsulating CorpusGenerator, intended for chaining.
   */
  public CorpusGenerator setPaths(IntProducer producer) {
    paths = producer;
    return this;
  }

  /**
   * @param producer generates the depth for any given path.
   *        The docID, pathNumber is given as seed.
   * @return the encapsulating CorpusGenerator, intended for chaining.
   */
  public CorpusGenerator setDepths(IntProducer producer) {
    depth = producer;
    return this;
  }

  /**
   * @param producer generates the tag for any given depth.
   *        The docID, pathNumber, depth is given as seed.
   * @return the encapsulating CorpusGenerator, intended for chaining.
   */
  public CorpusGenerator setTags(StringProducer producer) {
    this.tags = producer;
    return this;
  }
}