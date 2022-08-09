/*
   Copyright 2008 Olivier Chafik

   Licensed under the Apache License, Version 2.0 (the License);
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an AS IS BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.ochafik.util;

import java.util.Arrays;
import java.util.Random;

public class BinarySearchUtils {
	/// Testing statistics
	static long totalSteps, totalCalls;

	/**
	 * Searches a sorted int array for a specified value,
	 * using an optimized binary search algorithm (which tries to guess
	 * smart pivots).<br/>
	 * The result is unspecified if the array is not sorted.<br/>
	 * The method returns an index where key was found in the array.
	 * If the array contains duplicates, this might not be the first occurrence.
	 * @see java.util.Arrays.sort(int[])
	 * @see java.util.Arrays.binarySearch(int[])
	 * @param array sorted array of integers
	 * @param key value to search for in the array
	 * @param offset index of the first valid value in the array
	 * @param length number of valid values in the array
	 * @return index of an occurrence of key in array, 
	 * 		or -(insertionIndex + 1) if key is not contained in array (<i>insertionIndex</i> is then the index at which key could be inserted).
	 */
	public static final int binarySearch(int[] array, int key, int offset, int length) {//min, int max) {
		if (length == 0) {
			return -1 - offset;
		}
		
		int min = offset, max = offset + length - 1;
		int minVal = array[min], maxVal = array[max];

		int nPreviousSteps = 0;

		// Uncomment these two lines to get statistics about the average number of steps in the test report :
		//totalCalls++;
		for (;;) {
			//totalSteps++;

			// be careful not to compute key - minVal, for there might be an integer overflow.
			if (key <= minVal) return key == minVal ? min : -1 - min;
			if (key >= maxVal) return key == maxVal ? max : -2 - max;

			assert min != max;

			int pivot;
			// A typical binarySearch algorithm uses pivot = (min + max) / 2.
			// The pivot we use here tries to be smarter and to choose a pivot close to the expectable location of the key.
			// This reduces dramatically the number of steps needed to get to the key.
			// However, it does not work well with a logaritmic distribution of values, for instance.
			// When the key is not found quickly the smart way, we switch to the standard pivot.
			if (nPreviousSteps > 2) {
				pivot = (min + max) >>> 1;
				// stop increasing nPreviousSteps from now on
			} else {
				// NOTE: We cannot do the following operations in int precision, because there might be overflows.
				//       long operations are slower than float operations with the hardware this was tested on (intel core duo 2, JVM 1.6.0).
				//       Overall, using float proved to be the safest and fastest approach.
				pivot = min + (int)((key - (float)minVal) / (maxVal - (float)minVal) * (max - min));
				nPreviousSteps++;
			}

			int pivotVal = array[pivot];

			// NOTE: do not store key - pivotVal because of overflows
			if (key > pivotVal) {
				min = pivot + 1;
				max--;
			} else if (key == pivotVal) {
				return pivot;
			} else {
				min++;
				max = pivot - 1;
			}
			maxVal = array[max];
			minVal = array[min];
		}
	}

	public static class Tests {
		//static Random random = new Random(1); // deterministic seed for reproductible tests
		static Random random = new Random(System.currentTimeMillis());
		static int[] createSortedRandomArray(int size) {
			int[] array = new int[size];
			for (int i = size; i-- != 0;) array[i] = random.nextInt();
			Arrays.sort(array);
			return array;
		}
		static int[] createSortedRandomArray(int size, int minVal, int maxVal) {
			int[] array = new int[size];
			for (int i = size; i-- != 0;) array[i] = minVal + (int)(random.nextDouble() * (maxVal - (double)minVal));
			Arrays.sort(array);
			return array;
		}
		static int[] createEmptiedSequentialArray(int size, float loadFactor) {
			IntVector list = new IntVector();
			for (int i = 0; i< size; i++) list.add(i);

			int nRemoves = (int)(size * (1 - loadFactor));
			for (int i = nRemoves; i-- != 0;) {
				list.remove((int)(random.nextDouble() * (list.size() - 1)));
			}
			return list.toArray();
		}
		static int[] createSequentialArray(int size) {
			int[] array = new int[size];
			for (int i = size; i-- != 0;) array[i] = i;
			return array;
		}
		static int[] createSequentialDuplicatesArray(int size, int duplicationDegree) {
			int[] array = new int[size];
			for (int i = size; i-- != 0;) array[i] = i / duplicationDegree;
			return array;
		}
		static int[] createLogArray(int size, int scale) {
			int[] array = new int[size];
			for (int i = size; i-- != 0;) array[i] = (int)(scale * Math.log(i + 1));
			return array;
		}


		static void runTest(String title, int[] array, int[] keys, int nTests) {
			//System.out.println("#\n# "+title+"\n#");
			System.out.println("# "+title);
			long initTime;

			initTime = System.nanoTime();
			searchAll_Olive(array, keys, nTests);
			long oliveTime = System.nanoTime() - initTime;
			//System.out.println("Olive : " + (elapsedTime / 1000) + " (" +((float)elapsedTime / nTests / keys.length) + " each)");

			initTime = System.nanoTime();
			searchAll_Java(array, keys, nTests);
			long javaTime = System.nanoTime() - initTime;
			//System.out.println(" Java : " + (elapsedTime2 / 1000) + " (" +((float)elapsedTime2 / nTests / keys.length) + " each)");

			initTime = System.nanoTime();
			searchAll_Whitness(array, keys, nTests);
			long whitnessTime = System.nanoTime() - initTime;

			javaTime -= whitnessTime;
			oliveTime -= whitnessTime;

			System.out.println("\t"+(javaTime > oliveTime ?
					"zOlive " + (((javaTime * 10) / oliveTime) / 10.0) + " x faster" : 
						"  Java " + (((oliveTime * 10) / javaTime) / 10.0) + " x faster") +
						(totalCalls == 0 ?
								"" :
									" (avg. of " + ((totalSteps * 100 / totalCalls) / 100.0) + " steps)"));

			//if (totalCalls != 0) System.out.println("Steps avg : " + ((totalSteps * 100 / totalCalls) / 100.0));
			totalSteps = 0;
			totalCalls = 0;
			//System.out.println();

		}

		static boolean validate_searchAll(int[] array) {
			int len = array.length;
			for (int i = len; i-- != 0;) {
				int key = array[i];
				int a = binarySearch(array, key, 0, len);
				if (a >= 0 && array[a] != key) {
					return false;
				}
				int b = Arrays.binarySearch(array, key);
				if (b >= 0 && array[b] != key) {
					return false;
				}

				// if key was not found, both implementations return values < 0
				// still, values might be different because of duplicates
				if ((a >= 0) != (b >= 0)) {
					return false;
				}

				// if key was not found, insertionIndex shall be the same in both implementations
				if (a < 0 && (a != b)) {
					return false;
				}
			}
			return true;
		}
		static int searchAll_Olive(int[] array, int[] keys, int times) {
			int r = 0;
			int len = keys.length, arrayLen = array.length;
			for (int t = times; t-- != 0;) {
				for (int i = len; i-- != 0;) {
					r ^= binarySearch(array, keys[i], 0, arrayLen);
				}
			}
			return r;	
		}
		
		/// Does nothing but the same loop as searchAll_Olive and searchAll_Java
		static int searchAll_Whitness(int[] array, int[] keys, int times) {
			int r = 0;
			int len = keys.length, arrayLen = array.length;
			for (int t = times; t-- != 0;) {
				for (int i = len; i-- != 0;) {
					r ^= keys[i];
				}
			}
			return r;	
		}

		static int searchAll_Java(int[] array, int[] keys, int times) {
			int r = 0;
			int len = keys.length;
			for (int t = times; t-- != 0;) {
				for (int i = len; i-- != 0;) {
					r ^= Arrays.binarySearch(array, keys[i]);
				}
			}
			return r;
		}

	}

	public static void main(String[] args) {

		// JVM WARMUP
		int nWarmup = 100000;
		int nTests = 30;
		int[] array, keys;

		System.out.print("Warming up... ");
		array = Tests.createSortedRandomArray(100);
		Tests.searchAll_Java(array, array, nWarmup);
		Tests.searchAll_Olive(array, array, nWarmup);
		Tests.searchAll_Whitness(array, array, nWarmup);

		System.out.println("done.");

		totalCalls = totalSteps = 0;

		// TESTS
		int testSize = 100000, nKeys = 1000000, nValidations = 5, validationSize = 100000;
		for (int i = 0; i < nValidations; i++) {
			System.out.print("Validating ("+(i + 1)+" / "+nValidations +")... ");
			array = Tests.createSortedRandomArray(validationSize);
			boolean validated = Tests.validate_searchAll(array);
			System.out.println(validated ? "OK." : "FAILURE !!!");
			if (!validated) return;
		}

		System.out.println("Size of data arrays = " + testSize);

		keys = Tests.createSortedRandomArray(nKeys);

		Tests.runTest("Random elements, search of existing elements", array, array, nTests);
		Tests.runTest("Random elements, search of random elements", array, keys, nTests);

		array = Tests.createSequentialArray(testSize);

		Tests.runTest("Sequential elements, search of existing elements", array, array, nTests);
		Tests.runTest("Sequential elements, search of random elements", array, keys, nTests);

		array = Tests.createSequentialDuplicatesArray(testSize, 100);

		Tests.runTest("Sequential duplicated elements, search of existing elements", array, array, nTests);
		Tests.runTest("Sequential duplicated elements, search of random elements", array, keys, nTests);

		System.out.println();

		int scale = testSize;
		array = Tests.createLogArray(testSize, scale);
		keys = Tests.createSortedRandomArray(nKeys, 0, (int)(scale * Math.log(testSize)));

		for (int i = 1; i-- != 0;) {
			Tests.runTest("Logaritmic elements, search of existing elements", array, array, nTests);
			Tests.runTest("Logaritmic elements, search of random elements", array, keys, nTests);
		}

		for (float loadFactor : new float[] {0.1f, 0.3f, 0.5f, 0.75f, 0.9f}) {
			array = Tests.createEmptiedSequentialArray(testSize, loadFactor);
			keys = Tests.createSequentialArray(nKeys);

			Tests.runTest("Sparse sequential elements (loadFactor = "+loadFactor+"), search of existing elements", array, array, nTests);
			Tests.runTest("Sparse sequential elements (loadFactor = "+loadFactor+"), sequential keys", array, keys, nTests);

		}
		System.out.println();
	}

}
