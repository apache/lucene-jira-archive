package org.apache.lucene.util;

import java.util.Arrays;

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
 * @author maharwood
 */
public class IntArraySortedIntList implements DocNrSkipper
{
    int ints[];
    int cursor=0;
    
    /**
     * @param ints a sorted list of integers
     */
    public IntArraySortedIntList(int ints[])
    {
        super();
        this.ints=ints;
    }


    /* (non-Javadoc)
     * @see org.apache.lucene.util.DocNrSkipper#nextDocNr(int)
     */
    public int nextDocNr(int docNr)
    {
        //check required number not off end of our set
        if(docNr>ints[ints.length-1])
        {
            return -1;
        }
        //Check current cursor position
        if((cursor<ints.length)&&(ints[cursor]<docNr))
        {
            //Currently positioned before required point so scan from cursor to end 
            //(see binarySearch code below for possible speed-up)
	        while(ints[cursor]<docNr)
	        {
	            cursor++;
	        }
        }
        else
        {
            if((cursor==ints.length)||(ints[cursor]!=docNr))
            {
	            //cursor is out of synch - check first item (this could be the first call) 
	            if(ints[0]>=docNr)
	            {
	                cursor=0;
	            }
	            else
	            {
		            //Cursor out of synch - binary search whole array to resynch position
	                int pos=Arrays.binarySearch(ints,docNr);
	                if(pos>=0)
	                {
	                    cursor=pos; //found exact match
	                }
	                else
	                {
	                    cursor=Math.abs(pos)-1; //negative pos indicates insert pos
	                }
	            }
            }
            //else - cursor is positioned correctly
        }
        //return the selected item and increment cursor position
        return ints[cursor++];
    }    
}
