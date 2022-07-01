package org.apache.lucene.index;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

/**
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

/**
 * Tool to rename a field in an index
 */
public class RenameField {

	private final static String srcSuffix = "fnm";
	private final static String dstSuffix = srcSuffix;
	private final static String bakSuffix = srcSuffix + ".bak";

	static void renameField(Directory dir, String srcFieldName,
			String targetFieldName) throws IOException {
		try {
			IndexWriter iw = new IndexWriter(dir, new StandardAnalyzer(Version.LUCENE_CURRENT), MaxFieldLength.UNLIMITED);

			SegmentInfos sis = new SegmentInfos();
			sis.read(dir);
			Iterator<SegmentInfo> iter = sis.iterator();
			while (iter.hasNext()) {
				SegmentInfo si = iter.next();
				String srcFile = si.name + "." + srcSuffix;
				String dstFile = si.name + "." + dstSuffix;
				String bakFile = si.name + "." + bakSuffix;

				System.out.println("- Segment file " + srcFile);
				FieldInfos fis = new FieldInfos(dir, srcFile);
				fis.write(dir, bakFile);
				for (int i = 0; i < fis.size(); i++) {
					FieldInfo fi = fis.fieldInfo(i);
					String srcField = fi.name;
					if (srcFieldName.equals(srcField)) {
						fi.name = targetFieldName;
					}
				}
				fis.write(dir, dstFile);
			}
			iw.close();
		} finally {
			if (dir != null) {
				dir.close();
			}
		}
	}
}
