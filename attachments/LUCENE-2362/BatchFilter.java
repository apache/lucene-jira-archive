/*
 * Copyright 2001-2010 Fizteh-Center Lab., MIPT, Russia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created on 01.04.2010
 */
package ru.arptek.arpsite.search.lucene;

import java.io.IOException;
import java.util.BitSet;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Filter;

/**
 * 
 * 
 * @author vlsergey {at} gmail {dot} com
 */
public abstract class BatchFilter extends Filter {
    /**
     * Serial Version UID
     * 
     * @see Serializable
     */
    private static final long serialVersionUID = 7557961341471353390L;

    public abstract BitSet filter(IndexReader indexReader, BitSet docs)
            throws IOException;
}
