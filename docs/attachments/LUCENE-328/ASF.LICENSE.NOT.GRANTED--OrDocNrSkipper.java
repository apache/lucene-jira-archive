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
 * Skips along multiple docNrSkipper sets in numerical order with no dups
 * @author maharwood
 */
public class OrDocNrSkipper implements DocNrSkipper
{
    private int lastDocNrLookup=Integer.MIN_VALUE;
    private DocNrSkipper[] skippers;
    private int[] docPos;
    
    public OrDocNrSkipper(DocNrSkipper skippers[])
    {
        this.skippers=skippers;
        docPos=new int[skippers.length];
        Arrays.fill(docPos,Integer.MIN_VALUE);
    }
    
    public int nextDocNr(int docNr)
    {
        //Only call nextDocNr if necessary....
        for (int i = 0; i < skippers.length; i++)
        {
            if((docNr>lastDocNrLookup)&&(docPos[i]>=docNr))
            {
                //no need to advance list - already positioned
            }        
            else
            {
                docPos[i]=skippers[i].nextDocNr(docNr); //advance list
            }
        }
        lastDocNrLookup=docNr;//save the docNr
        int lowest=Integer.MAX_VALUE;
        for (int i = 0; i < docPos.length; i++)
        {
            if(docPos[i]>=0)
            {
                lowest=Math.min(docPos[i],lowest);
            }
        }
        if(lowest==Integer.MAX_VALUE)
        {
            return -1; //all at end
        }
        return lowest;
    }
}
