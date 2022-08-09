#!/usr/bin/env python
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import sys
import re

license = """/*
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
"""

pattern = re.escape(license)
pattern = re.sub('\r?\n', 's+', pattern) # match any trailing whitespace
pattern = re.sub('(\\\ ){2,}', '\s+', pattern) # match more than one repeating space
pattern = re.compile(pattern, re.MULTILINE)

def stop_if_matches_current_license(text):
    exact_pattern = re.escape(license)
    exact_pattern = re.sub('\r?\n', 's+', exact_pattern)
    match = re.search(exact_pattern, text, re.MULTILINE)
    if match is not None and match.start(0) == 0:
        exit()
    
def find_license(text, file_name):
    match = pattern.search(text)
    if match is None:
        print 'License header not found in [%s]!?' % file_name
        exit()
    return match

def should_process_file(start, text, file_name):
    if start is not 0 and not text.startswith('package org.apache.'):
        print '[%s] has leading lines, fix manually!!' % file_name
        return False
    
    return True
        
file_name = sys.argv[1]
# print 'Processing %s' % file_name
with open(file_name, 'r+') as f:
    text = f.read()
    
    stop_if_matches_current_license(text)
    
    match = find_license(text, file_name)
    start = match.start(0)
    end = match.end(0)
    if not should_process_file(start, text, file_name):
        exit()

    newtext = '%s%s%s' % (license, text[0:start], text[end:])
    f.seek(0)
    f.write(newtext)
    f.truncate()
    f.close()
