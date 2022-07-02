/* $Id$ */

/**
 * Copyright 2005 The Apache Software Foundation
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

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;


/**
 * @author  <a href="mailto:ronnie.kolehmainen@ub.uu.se">Ronnie Kolehmainen</a>
 * @version $Revision$, $Date$
 */
public class TestASCIIAnalyzer
    extends TestCase
{
    Analyzer a;
    String[] inputs;
    String[] expected;


    public void setUp()
    {
        inputs = new String[] {
            "Ren\u00e8\u00e9 Z\u00eallweger",
            "Wei\u00dfer Oleander",
            "M\u00f6tley Cr\u00fce",
            "Ana\u00efs \u00c9mile Zo\u00e9"
        };
        expected = new String[] {
            "renee zellweger",
            "weiser oleander",
            "motley crue",
            "anais emile zoe"
        };
        a = new ASCIIAnalyzer();
    }


    public void testAnalyzer()
    {
        for (int i = 0; i < inputs.length; i++) {
            assertEquals("Testing " + a.getClass()
                         + " with input string: " + inputs[i],
                         expected[i],
                         parse(inputs[i], a));
        }
    }


    private String parse(String s, Analyzer a)
    {
        try {
            org.apache.lucene.search.Query q =
                org.apache.lucene.queryParser.QueryParser.parse(s, "field", a);
            return q.toString("field");
        } catch (Exception e) {
            System.out.println(e.toString());
            return null;
        }
    }
}
