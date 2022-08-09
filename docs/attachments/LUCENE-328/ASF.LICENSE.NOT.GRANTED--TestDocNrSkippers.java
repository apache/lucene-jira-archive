package org.apache.lucene.util;

import java.util.BitSet;

import junit.framework.TestCase;
/**
 * Copyright 2005 Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 * Tests all iterators (currently ANDs/Ors) across all set implementations (bitsets, int arrays, sortedVInt)
 * @author maharwood
 */
public class TestDocNrSkippers extends TestCase
{
    int nums1357[] = { 1, 3, 5, 7 };
    int nums0246[] = { 0, 2, 4, 6 };
    int nums478[] = { 4, 7, 8 };

    SkipperFactory skipperFactories[] = new SkipperFactory[] { new IntArrayFactory(),
            new BitsetFactory()
            , new SortedVIntSetFactory() };
    
    //TODO the SortedVIntSet does not support out-of-sequence calls - set the flag below to
    // "true" to see this error
    static final boolean testRewinds=false;

    public void testSingleOrLogic()
    {
        for (int i = 0; i < skipperFactories.length; i++)
        {
            String msg="single OR test:"+skipperFactories[i].getClass().getName();
            DocNrSkipper skipper = new OrDocNrSkipper(new DocNrSkipper[]{
                    skipperFactories[i].getSkipper(nums1357), 
                    skipperFactories[i].getSkipper(nums0246)});
            int expectedResults[] = { 0, 1, 2, 3, 4, 5, 6, 7, -1 };
            validateResults(msg,skipper, expectedResults);
        }
    }

    public void testMultipleOrLogic()
    {
        for (int i = 0; i < skipperFactories.length; i++)
        {
            String msg="Multiple OR test:"+skipperFactories[i].getClass().getName();
            DocNrSkipper skipper = new OrDocNrSkipper(new DocNrSkipper[]{
                    skipperFactories[i].getSkipper(nums1357), 
                    skipperFactories[i].getSkipper(nums0246), 
                    skipperFactories[i].getSkipper(nums478)});
            int expectedResults[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, -1 };
            validateResults(msg,skipper, expectedResults);
        }
    }

    public void testMixedAndOrLogic()
    {
        for (int i = 0; i < skipperFactories.length; i++)
        {
            //equivalent of 0246 OR (1357 AND 478)
            String msg="mixed AND/OR test:"+skipperFactories[i].getClass().getName();
            DocNrSkipper skipper = new AndDocNrSkipper(new DocNrSkipper[]{
                    skipperFactories[i].getSkipper(nums1357), 
                    skipperFactories[i].getSkipper(nums478)});
            skipper = new OrDocNrSkipper(new DocNrSkipper[]{skipper, 
                    skipperFactories[i].getSkipper(nums0246)});
            int expectedResults[] = { 0, 2, 4, 6, 7, -1 };
            validateResults(msg,skipper, expectedResults);
        }
    }

    public void testAndLogic()
    {
        for (int i = 0; i < skipperFactories.length; i++)
        {
            String msg="single AND test:"+skipperFactories[i].getClass().getName();
            DocNrSkipper skipper = new AndDocNrSkipper(new DocNrSkipper[]{
                    skipperFactories[i].getSkipper(nums1357), 
                    skipperFactories[i].getSkipper(nums0246)});
            int expectedResults[] = { -1 };
            validateResults(msg,skipper, expectedResults);

            skipper = new AndDocNrSkipper(new DocNrSkipper[]{
                    skipperFactories[i].getSkipper(nums1357),
                    skipperFactories[i].getSkipper(nums478)});
            expectedResults = new int[] { 7, -1 };
            validateResults(msg,skipper, expectedResults);

            skipper = new AndDocNrSkipper(new DocNrSkipper[]{
                    skipperFactories[i].getSkipper(nums0246),
                    skipperFactories[i].getSkipper(nums478)});
            expectedResults = new int[] { 4, -1 };
            validateResults(msg,skipper, expectedResults);
        }
    }

    private void validateResults(String assertMsg,DocNrSkipper is, int[] expectedResults)
    {
        int docNr = -1;
        for (int i = 0; i < expectedResults.length; i++)
        {
            int searchDoc=++docNr;
            docNr = is.nextDocNr(searchDoc);
            assertEquals(assertMsg,expectedResults[i], docNr);
        }
        //repeat test to ensure doc sets can rewind
        if(testRewinds)
        {
	        docNr = expectedResults[0]-1;
	        for (int i = 0; i < expectedResults.length; i++)
	        {
	            int searchDoc=++docNr;
	            docNr = is.nextDocNr(searchDoc);
	            assertEquals("rewind:"+assertMsg,expectedResults[i],docNr);
	        }
        }

    }

    //A simple abstraction that allows this test code to work with multiple 
    // forms of document list representations
    interface SkipperFactory
    {
        DocNrSkipper getSkipper(int[] docNrs);
    }

    class IntArrayFactory implements SkipperFactory
    {
        public DocNrSkipper getSkipper(int[] docNrs)
        {
            return new IntArraySortedIntList(docNrs);
        }
    }

    class BitsetFactory implements SkipperFactory
    {
        public DocNrSkipper getSkipper(int[] docNrs)
        {
            BitSet bits = new BitSet(docNrs[docNrs.length - 1]);
            for (int i = 0; i < docNrs.length; i++)
            {
                bits.set(docNrs[i]);
            }
            return new BitSetSortedIntList(bits);
        }
    }

    class SortedVIntSetFactory implements SkipperFactory
    {
        public DocNrSkipper getSkipper(int[] docNrs)
        {
            return new SortedVIntList(docNrs, docNrs.length).getDocNrSkipper();
        }
    }
}