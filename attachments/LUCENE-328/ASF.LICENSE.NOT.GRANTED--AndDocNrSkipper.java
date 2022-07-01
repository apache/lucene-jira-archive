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
 * Skips along multiple docNrSkipper sets which have common entries
 * @author maharwood
 */
public class AndDocNrSkipper implements DocNrSkipper
{
    private int lastDocNrLookup=Integer.MIN_VALUE;
    private DocNrSkipper[] skippers;
    private int docPos[];
    
    public AndDocNrSkipper(DocNrSkipper skippers[])
    {
        this.skippers=skippers;
        docPos=new int[skippers.length];
        Arrays.fill(docPos,Integer.MIN_VALUE);
    }
    
    public int nextDocNr(int docNr)
    {
        if(lastDocNrLookup>docNr)
        {
            //user has rewinded position - need to reset position on all lists
            for (int i = 0; i < skippers.length; i++)
            {
                docPos[i]=skippers[i].nextDocNr(docNr);
            }
        }
        lastDocNrLookup=docNr;
        
        int jumpTo=docNr;
        for (int i = 0; i < skippers.length; i++)
        {
            if (docPos[i] < jumpTo)
            {
                docPos[i] = skippers[i].nextDocNr(jumpTo);
            }
            if (docPos[i] == -1)
            {
                return -1; //at end
            }
            if (i == 0)
            {
                //set the bar at first list's position
                jumpTo = docPos[0];
            }
            else
            {
	            if (docPos[i] > jumpTo)
	            {
	                //current list is more advanced - reset search
	                //to this new advance position and reset i
	                jumpTo = docPos[i];
	                i = -1; //reset loop to start at 0 again
	            }
            }
        }
        return jumpTo;
    }
}
