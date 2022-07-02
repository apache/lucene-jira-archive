import re
import shutil
import signal
import sys
import os
import time
import datetime
import subprocess
import threading

VERBOSE = False

if sys.platform.find('sunos') != -1:
  NUM_THREADS = 6
else:
  NUM_THREADS = 4

START_TIME = datetime.datetime.now()

PR_LOCK = threading.Lock()

FAIL_FAST = False

DO_SYMLINKS = True

reTestSuite = re.compile('Testsuite: (.*?)$')
reTestCase = re.compile(r'^(\d+) msec for (.*?)$')
reTimeElapsed = re.compile('Time elapsed: (.*?) sec$')

if '-29' in sys.argv:
  is29 = True
  isFlex = is30 = False
else:
  isFlex = os.getcwd().lower().find('flex') != -1
  is29 = open('.svn/entries').read().find('/lucene_2_9') != -1
  is30 = open('.svn/entries').read().find('/lucene_3_0') != -1

isTrunk = not isFlex and not is29 and not is30

if isFlex:
  print '[FLEX]'
elif is29:
  print '[2.9]'
elif is30:
  print '[3.0]'
else:
  print '[TRUNK]'

def newBackwardsTests():
  return isFlex or isTrunk
  
def elapsed():
  return datetime.datetime.now()-START_TIME

class OneTest(threading.Thread):

  lock = threading.Lock()
  failed = False
  doStop = False
  
  SUBPROCESS = True

  def __init__(self, dirName, id, jobs, allTimes):
    threading.Thread.__init__(self)
    self.id = id
    self.allTimes = allTimes
    self.logFileName = '/tmp/%s/thread%d.log' % (dirName, id)
    if os.path.exists(self.logFileName):
      os.remove(self.logFileName)
    if os.path.exists('./thread%d.log' % id) or os.path.islink('./thread%d.log' % id):
      os.remove('./thread%d.log' % id)
    os.system('ln -s /tmp/%s/thread%d.log .' % (dirName, id))
    self.jobs = jobs
    self.testCount = 0

  def stop(self):
    self.doStop = True

  def doPrint(self, pending):
    # flush
    # print 'PENDING %s' % pending
    if VERBOSE and len(pending) > 0:
      self.lock.acquire()
      try:
        for v in pending:
          print '%s %s' % (self.prefix, v)
        print
      finally:
        self.lock.release()

    ts = None
    t = None
    d = {}
    for l in pending:
      m = reTestSuite.search(l.strip())
      if m is not None:
        ts = m.group(1).split('.')[-1]
      m = reTimeElapsed.search(l.strip())
      if m is not None:
        t = float(m.group(1))
      m = reTestCase.match(l.strip())
      #print 'check %s' % l.strip()
      if m is not None:
        #print ' match'
        tx = float(m.group(1))
        name = m.group(2)
        d[name] = d.get(name, 0.0) + tx

    if len(d) > 0:
      for x0, t0 in d.items():
        t0 = t0/1000.0
        # counts against last test:
        self.allTimes.append((t0, '%s:%s' % (self.lastTest, x0), self.job))
        #print 'add %s %s' % (t0, '%s:%s' % (ts, x0))

    if ts is not None and t is not None:
      self.lastTest = ts
      self.allTimes.append((t, ts, self.job))
    elif ts is not None or t is not None:
      print 'WARNING: failed to extract something ts=%s t=%s' % (ts, t)
      
    del pending[:]

  def runOneJob(self):
    job = self.jobs.nextJob()

    if job is None:
      return None, True

    self.job = job

    self.msg('run "%s"...' % job)
    
    f = open(self.logFileName, 'a')
    f.write('\n\n\n\n\n\n\n\n\n\nSTART: %s [%s]\n' % (job, elapsed()))
    f.close();

    if os.system('%s >> %s 2>&1 &' % (job, self.logFileName)):
      self.failed = True
      raise RuntimeError('launch %s failed' % job)

    p = subprocess.Popen(('/usr/bin/tail -f %s' % self.logFileName).split(),
                         shell=False,
                         stdout=subprocess.PIPE)

    testTop = job.split()[1]
    
    try:
      success = True

      pending = []
      while not self.doStop:
        l = p.stdout.readline()

        if l == '':
          break
        else:
          l = l.strip()
          if l.lower().find('caused an error') != -1:
            success = False
            self.msg('FAIL %s: %s' % (testTop, l))
          elif l.lower().find('testcase:') != -1:
            success = False
            self.msg('FAIL %s: %s' % (testTop, l))
          elif l.find('BUILD FAILED') != -1:
            if success:
              self.msg('FAIL %s: %s' % (testTop, l))
            success = False
          elif l.find('FAILED') != -1:
            if success:
              self.msg('FAIL %s: %s' % (testTop, l))
            success = False
          if l.startswith('[junit]'):
            l = l[7:]
            if l[0:1] == ' ':
              l = l[1:]
          if l == '':
            self.doPrint(pending)
          else:
            pending.append(l)

          if l.find('Total time:') != -1:
            break

          if l.find('Testsuite: ') != -1:
            self.testCount += 1

          if l.find('Invalid memory access') != -1:
            break

    finally:
      os.kill(p.pid, signal.SIGKILL)

    self.doPrint(pending)

    return job, success

  def msg(self, s):
    PR_LOCK.acquire()
    try:
      print '%s [%s]: %s' % (self.id, elapsed(), s)
    finally:
      PR_LOCK.release()

  def run(self):

    while True:
      job, success = self.runOneJob()
      if job is None:
        self.msg('DONE')
        break
      if False and not success:
        # fail fast
        self.failed = True
        if not FAIL_FAST:
          self.msg('DONE [FAILED]')
        break

def init(dirName):
  if 0:
    if os.system('ant clean > init.log'):
      print 'clean failed'
      return False

  if True:

    for subDir in ('test', 'build'):
      if os.path.exists(subDir):
        if not os.path.islink(subDir):
          shutil.rmtree(subDir)
        else:
          os.unlink(subDir)
        
      subDirName = '/tmp/%s/%s' % (dirName, subDir)
      if subDir == 'build':
        scrubBuild(dirName)
      elif os.path.exists(subDirName):
        shutil.rmtree(subDirName)

      if not os.path.exists(subDirName):
        if os.system('mkdir -p %s >> init.log' % subDirName):
          print 'mkdir failed'
          return False

      if DO_SYMLINKS:
        if not os.path.islink(subDir) and os.system('ln -s %s .' % subDirName):
          print 'ln failed'
          return False

  return True

def scrubBuild(dirName):
  subDirName = '/tmp/%s/build' % dirName
  if os.path.exists(subDirName):
    for f in os.listdir(subDirName):
      full = '%s/%s' % (subDirName, f)
      if os.path.isdir(full):
        shutil.rmtree(full)
      else:
        os.remove(full)
  
PATCHED_FILES = ['build.xml', 'common-build.xml', 'contrib/contrib-build.xml', 'contrib/analyzers/common/src/test/org/apache/lucene/analysis/compound/TestCompoundWordTokenFilter.java', 'contrib/snowball/build.xml']

def doPatch():
  for fileName in PATCHED_FILES:
    if os.path.exists(fileName):
      shutil.copy(fileName, fileName+'.sav')
  if isFlex:
    p = BUILD_PATCH_FLEX
  elif is29:
    p = BUILD_PATCH_29
  elif is30:
    p = BUILD_PATCH_30
  else:
    p = BUILD_PATCH
  open('__build.patch', 'w').write(p)
  if os.system('patch -p0 < __build.patch > patch.log 2>&1'):
    for fileName in PATCHED_FILES:
      if os.path.exists(fileName + '.sav'):
        shutil.copy(fileName+'.sav', fileName)
    return False
  else:
    return True

def undoPatch():
  for fileName in PATCHED_FILES:
    if os.path.exists(fileName + '.sav'):
      os.remove(fileName)
      os.rename(fileName+'.sav', fileName)

def cleanup(dirName):
  for subDir in ('test',):
    subDirName = '/tmp/%s/%s' % (dirName, subDir)    
    if os.path.exists(subDirName):
      shutil.rmtree(subDirName)

  scrubBuild(dirName)

# clean
if newBackwardsTests():
  ALL_JOBS = [
    'ant compile-backwards compile-core compile-demo jar-core compile-test build-contrib',
    ]
else:
  ALL_JOBS = [
    'ant compile-tag compile-core compile-demo jar-core compile-test build-contrib',
    ]

if True:
  for package in ('contrib', 'index', 'search', 'store', 'analysis', 'lucene', 'util', 'document', 'queryParser'):
    if package == 'lucene':
      d = 'root'
    else:
      d = ''
    if package == 'contrib':
      ALL_JOBS.append('ant test-contrib')
    else:
      if newBackwardsTests():
        backTag = 'backwards'
      else:
        backTag = 'tag'
      for t in ('core', backTag):
        if package in ('search', 'index'):
          ALL_JOBS.append('ant test-%s -Dtestpackagea%s=%s' % (t, d, package))
          ALL_JOBS.append('ant test-%s -Dtestpackageb%s=%s' % (t, d, package))
        else:
          ALL_JOBS.append('ant test-%s -Dtestpackage%s=%s' % (t, d, package))
else:
  ALL_JOBS.append('ant test-core -Dtestcase=TestIndexWriter')

reTag = re.compile('property name="compatibility.tag" value="(.*?)"')

def getBackCompatTag():
  m = reTag.search(open('common-build.xml', 'rb').read())
  return m.group(1)

class Jobs:

  def __init__(self, dirName):
    self.jobs = ALL_JOBS
    if not os.path.exists('backwards') and newBackwardsTests():
      # one-time cutover
      ALL_JOBS[0] = ALL_JOBS[0].replace('ant ', 'ant download-backwards')
    self.next = 0
    self.lock = threading.Lock()

  def nextJob(self):
    self.lock.acquire()
    try:
      if self.next >= len(self.jobs):
        return None
      else:
        job = self.jobs[self.next]
        self.next += 1
        return job
    finally:
      self.lock.release()

def main():
  global NUM_THREADS

  for i, path in enumerate(('contrib/analyzers/common/src/test/org/apache/lucene/analysis/snowball/data',
                            'contrib/snowball/src/test/org/apache/lucene/analysis/snowball/data')):
    if not os.path.exists(path):
      if os.path.exists(os.path.split(path)[0]):
        if os.system('ln -s /lucene/__start/contrib/analyzers/common/src/test/org/apache/lucene/analysis/snowball/data %s' % path):
          print 'SYMLINK failed for snowball data dir'
          sys.exit(1)
  
  start = datetime.datetime.now()

  if '-threads' in sys.argv:
    idx = sys.argv.index('-threads')
    NUM_THREADS = int(sys.argv[1+idx])

  if os.system('svn diff > diffs.x'):
    print 'svn diff failed'
    sys.exit(1)

  if is29 or is30:
    lx = os.listdir('tags')
    lx.sort(reverse=True)
    if os.system('svn diff tags/%s/src/test >> diffs.x' % lx[0]):
      print 'ERROR: failed to diff back compat tests'
      sys.exit(1)
  elif isFlex:
    if os.system('svn diff backwards/flex_1458_3_0_back_compat_tests/src/test >> diffs.x'):
      print 'ERROR: failed to diff back compat tests'
      sys.exit(1)
  else:
    # trunk
    if os.system('svn diff backwards/%s/src/test >> diffs.x' % os.listdir('backwards')[0]):
      print 'ERROR: failed to diff back compat tests'
      sys.exit(1)

  if not doPatch():
    print 'FAILURE: see patch.log'
    sys.exit(1)

  allTimes = []

  try:

    dirName = os.path.split(os.getcwd())[-1]

    if not init(dirName):
      print 'FAILURE: see init.log'
      sys.exit(1)

    jobs = Jobs(dirName)

    l = []
    for i in xrange(NUM_THREADS):
      oneTest = OneTest(dirName, i, jobs, allTimes)
      l.append(oneTest)

      if i == 0:
        # run first job in foreground
        job, success = oneTest.runOneJob()
        if not success:
          print 'FAILURE: see thread%s.log' % oneTest.id
          sys.exit(1)

      oneTest.setDaemon(True)
      oneTest.start()

    anyFailed = False
    while True:
      anyAlive = False
      for v in l:
        if v.failed:
          anyFailed = True
          if FAIL_FAST:
            print 'FAILURE: see %s '% v.logFileName
            for v in l:
              v.stop()
            break
        anyAlive = anyAlive or v.isAlive()

      if FAIL_FAST:
        if anyFailed:
          break
      if not anyAlive:
        break

      time.sleep(1.0)

    totCount = 0
    for v in l:
      v.join()
      totCount += v.testCount

    print
    if anyFailed:
      print 'FAILED: took %s [%d tests]' % (datetime.datetime.now() - start, totCount)
    else:
      print 'DONE: took %s [%d tests]' % (datetime.datetime.now() - start, totCount)

      reJira = re.compile(r'^%s\.(\d+)$')
      m = reJira.match(os.path.split(os.getcwd())[1])
      if m is not None:
        patchFileName = '/export/home/mike/patch/LUCENE-%s.patch' % m.group(1)
        shutil.copy('diffs.x', patchFileName)
        print 'COPIED patch -> %s' % patchFileName
  finally:
    undoPatch()
    cleanup(dirName)
    
  try:
    os.killpg(os.getpgid(os.getpid()), signal.SIGINT)
  except KeyboardInterrupt:
    pass

  if 0:
    allTimes.sort(reverse=True)
    print 'Test times:'
    for t, s, job in allTimes:
      if job.find('test-tag') != -1 or job.find('test-backwards') != -1:
        x = ' [backcompat tests]'
      else:
        x = ''
      print '  %5.3f sec %s%s' % (t, s, x)
    
  
BUILD_PATCH = '''
Index: common-build.xml
===================================================================
--- common-build.xml	(revision 807293)
+++ common-build.xml	(working copy)
@@ -381,6 +381,8 @@
 	      <not><or>
 	        <isset property="testcase" />
 	      	<isset property="testpackage" />
+	      	<isset property="testpackagea" />
+	      	<isset property="testpackageb" />
 	      	<isset property="testpackageroot" />
 	      </or></not>
 	    </condition>
@@ -410,9 +412,15 @@
 	      <batchtest fork="yes" todir="@{junit.output.dir}" if="runall">
 	        <fileset dir="@{dataDir}" includes="${junit.includes}" excludes="${junit.excludes}"/>
 	      </batchtest>
-	      <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackage">
-	        <fileset dir="@{dataDir}" includes="**/${testpackage}/**/Test*.java,**/${testpackage}/**/*Test.java" excludes="${junit.excludes}"/>
-	      </batchtest>
+      <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackage">
+        <fileset dir="@{dataDir}" includes="**/${testpackage}/**/Test*.java,**/${testpackage}/**/*Test.java" excludes="${junit.excludes}"/>
+      </batchtest>
+       <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackagea">
+         <fileset dir="@{dataDir}" includes=" **/${testpackagea}/**/TestA*.java **/${testpackagea}/**/A*Test.java **/${testpackagea}/**/TestB*.java **/${testpackagea}/**/B*Test.java **/${testpackagea}/**/TestC*.java **/${testpackagea}/**/C*Test.java **/${testpackagea}/**/TestD*.java **/${testpackagea}/**/D*Test.java **/${testpackagea}/**/TestE*.java **/${testpackagea}/**/E*Test.java **/${testpackagea}/**/TestF*.java **/${testpackagea}/**/F*Test.java **/${testpackagea}/**/TestG*.java **/${testpackagea}/**/G*Test.java **/${testpackagea}/**/TestH*.java **/${testpackagea}/**/H*Test.java **/${testpackagea}/**/TestI*.java **/${testpackagea}/**/I*Test.java **/${testpackagea}/**/TestJ*.java **/${testpackagea}/**/J*Test.java **/${testpackagea}/**/TestK*.java **/${testpackagea}/**/K*Test.java **/${testpackagea}/**/TestL*.java **/${testpackagea}/**/L*Test.java" excludes="${junit.excludes}"/>
+       </batchtest>
+       <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackageb">
+         <fileset dir="@{dataDir}" includes=" **/${testpackageb}/**/TestM*.java **/${testpackageb}/**/M*Test.java **/${testpackageb}/**/TestN*.java **/${testpackageb}/**/N*Test.java **/${testpackageb}/**/TestO*.java **/${testpackageb}/**/O*Test.java **/${testpackageb}/**/TestP*.java **/${testpackageb}/**/P*Test.java **/${testpackageb}/**/TestQ*.java **/${testpackageb}/**/Q*Test.java **/${testpackageb}/**/TestR*.java **/${testpackageb}/**/R*Test.java **/${testpackageb}/**/TestS*.java **/${testpackageb}/**/S*Test.java **/${testpackageb}/**/TestT*.java **/${testpackageb}/**/T*Test.java **/${testpackageb}/**/TestU*.java **/${testpackageb}/**/U*Test.java **/${testpackageb}/**/TestV*.java **/${testpackageb}/**/V*Test.java **/${testpackageb}/**/TestW*.java **/${testpackageb}/**/W*Test.java **/${testpackageb}/**/TestX*.java **/${testpackageb}/**/X*Test.java **/${testpackageb}/**/TestY*.java **/${testpackageb}/**/Y*Test.java **/${testpackageb}/**/TestZ*.java **/${testpackageb}/**/Z*Test.java" excludes="${junit.excludes}"/>
+       </batchtest>
 	      <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackageroot">
 	        <fileset dir="@{dataDir}" includes="**/${testpackageroot}/Test*.java,**/${testpackageroot}/*Test.java" excludes="${junit.excludes}"/>
 	      </batchtest>
@@ -422,14 +430,12 @@
 	    </junit>
 	    <!-- create this file, then if we don't fail, delete it -->
 	    <!-- this meme makes it easy to tell if contribs have failed later -->
-	    <echo file="@{junit.output.dir}/junitfailed.flag">MAYBE</echo>
 	    <fail if="tests.failed">Tests failed!</fail>
 	    <!-- life would be easier if echo had an 'if' attribute like fail -->
-	    <delete file="@{junit.output.dir}/junitfailed.flag" />
   	</sequential>
   </macrodef>
 	
-  <target name="test" depends="compile-test" description="Runs unit tests">
+  <target name="test" description="Runs unit tests">
     <test-macro dataDir="src/test" tempDir="${build.dir}/test">
     	<contrib-settings>
 	      <!-- set as a system property so contrib tests can have a fixed root
Index: build.xml
===================================================================
--- build.xml	(revision 899584)
+++ build.xml	(working copy)
@@ -119,7 +119,7 @@
   <!-- remove this -->
   <target name="test-tag" depends="test-backwards" description="deprecated"/>
 
-  <target name="test-backwards" depends="download-backwards, compile-core, jar-core"
+  <target name="compile-backwards"
   	description="Runs tests of a previous Lucene version.">
 	<sequential>
     <available property="backwards.tests.available" file="${backwards.dir}/${backwards.branch}/src/test" />
@@ -143,14 +143,18 @@
 		  	
 	  <!-- compile branch tests against branch jar -->	
 	  <compile-test-macro srcdir="${backwards.dir}/${backwards.branch}/src/test" destdir="${build.dir}/${backwards.branch}/classes/test"
+
 			  			  test.classpath="backwards.test.classpath" javac.source="${javac.source.backwards}" javac.target="${javac.target.backwards}"/>
-		
+	  </sequential>
+  </target>	
+
+  <target name="test-backwards"
+  	description="Runs tests of a previous Lucene version.">
 	  <!-- run branch tests against trunk jar -->
       <test-macro dataDir="${backwards.dir}/${backwards.branch}/src/test" 
       			  tempDir="${build.dir}/${backwards.branch}"
       			  junit.classpath="backwards.junit.classpath"
               junit.output.dir="${junit.output.dir.backwards}" />
-  	</sequential>
   </target>	
 
 	
@@ -683,7 +687,7 @@
     <contrib-crawl target="build-artifacts-and-tests"/>
   </target>
 
-  <target name="test-contrib" depends="build-contrib">
+  <target name="test-contrib">
     <!-- Don't fail on error, instead check for flag file so we run
          all the tests possible and can "ant generate-test-reports"
          for all of them.
Index: contrib/contrib-build.xml
===================================================================
--- contrib/contrib-build.xml	(revision 807293)
+++ contrib/contrib-build.xml	(working copy)
@@ -61,7 +61,8 @@
   </target>
 
   
-  <target name="init" depends="common.init,build-lucene,build-lucene-tests"/>
+  <!--<target name="init" depends="common.init,build-lucene,build-lucene-tests"/>-->
+  <target name="init"/>
   <target name="compile-test" depends="init" if="contrib.has.tests">
     <antcall target="common.compile-test" inheritRefs="true" />
   </target>
'''


BUILD_PATCH_29 = '''
Index: common-build.xml
===================================================================
--- common-build.xml	(revision 807293)
+++ common-build.xml	(working copy)
@@ -381,6 +381,8 @@
 	      <not><or>
 	        <isset property="testcase" />
 	      	<isset property="testpackage" />
+	      	<isset property="testpackagea" />
+	      	<isset property="testpackageb" />
 	      	<isset property="testpackageroot" />
 	      </or></not>
 	    </condition>
@@ -410,9 +412,15 @@
 	      <batchtest fork="yes" todir="@{junit.output.dir}" if="runall">
 	        <fileset dir="@{dataDir}" includes="${junit.includes}" excludes="${junit.excludes}"/>
 	      </batchtest>
-	      <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackage">
-	        <fileset dir="@{dataDir}" includes="**/${testpackage}/**/Test*.java,**/${testpackage}/**/*Test.java" excludes="${junit.excludes}"/>
-	      </batchtest>
+      <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackage">
+        <fileset dir="@{dataDir}" includes="**/${testpackage}/**/Test*.java,**/${testpackage}/**/*Test.java" excludes="${junit.excludes}"/>
+      </batchtest>
+       <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackagea">
+         <fileset dir="@{dataDir}" includes=" **/${testpackagea}/**/TestA*.java **/${testpackagea}/**/A*Test.java **/${testpackagea}/**/TestB*.java **/${testpackagea}/**/B*Test.java **/${testpackagea}/**/TestC*.java **/${testpackagea}/**/C*Test.java **/${testpackagea}/**/TestD*.java **/${testpackagea}/**/D*Test.java **/${testpackagea}/**/TestE*.java **/${testpackagea}/**/E*Test.java **/${testpackagea}/**/TestF*.java **/${testpackagea}/**/F*Test.java **/${testpackagea}/**/TestG*.java **/${testpackagea}/**/G*Test.java **/${testpackagea}/**/TestH*.java **/${testpackagea}/**/H*Test.java **/${testpackagea}/**/TestI*.java **/${testpackagea}/**/I*Test.java **/${testpackagea}/**/TestJ*.java **/${testpackagea}/**/J*Test.java **/${testpackagea}/**/TestK*.java **/${testpackagea}/**/K*Test.java **/${testpackagea}/**/TestL*.java **/${testpackagea}/**/L*Test.java" excludes="${junit.excludes}"/>
+       </batchtest>
+       <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackageb">
+         <fileset dir="@{dataDir}" includes=" **/${testpackageb}/**/TestM*.java **/${testpackageb}/**/M*Test.java **/${testpackageb}/**/TestN*.java **/${testpackageb}/**/N*Test.java **/${testpackageb}/**/TestO*.java **/${testpackageb}/**/O*Test.java **/${testpackageb}/**/TestP*.java **/${testpackageb}/**/P*Test.java **/${testpackageb}/**/TestQ*.java **/${testpackageb}/**/Q*Test.java **/${testpackageb}/**/TestR*.java **/${testpackageb}/**/R*Test.java **/${testpackageb}/**/TestS*.java **/${testpackageb}/**/S*Test.java **/${testpackageb}/**/TestT*.java **/${testpackageb}/**/T*Test.java **/${testpackageb}/**/TestU*.java **/${testpackageb}/**/U*Test.java **/${testpackageb}/**/TestV*.java **/${testpackageb}/**/V*Test.java **/${testpackageb}/**/TestW*.java **/${testpackageb}/**/W*Test.java **/${testpackageb}/**/TestX*.java **/${testpackageb}/**/X*Test.java **/${testpackageb}/**/TestY*.java **/${testpackageb}/**/Y*Test.java **/${testpackageb}/**/TestZ*.java **/${testpackageb}/**/Z*Test.java" excludes="${junit.excludes}"/>
+       </batchtest>
 	      <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackageroot">
 	        <fileset dir="@{dataDir}" includes="**/${testpackageroot}/Test*.java,**/${testpackageroot}/*Test.java" excludes="${junit.excludes}"/>
 	      </batchtest>
@@ -422,14 +430,12 @@
 	    </junit>
 	    <!-- create this file, then if we don't fail, delete it -->
 	    <!-- this meme makes it easy to tell if contribs have failed later -->
-	    <echo file="@{junit.output.dir}/junitfailed.flag">MAYBE</echo>
 	    <fail if="tests.failed">Tests failed!</fail>
 	    <!-- life would be easier if echo had an 'if' attribute like fail -->
-	    <delete file="@{junit.output.dir}/junitfailed.flag" />
   	</sequential>
   </macrodef>
 	
-  <target name="test" depends="compile-test" description="Runs unit tests">
+  <target name="test" description="Runs unit tests">
     <test-macro dataDir="src/test" tempDir="${build.dir}/test">
     	<contrib-settings>
 	      <!-- set as a system property so contrib tests can have a fixed root
Index: build.xml
===================================================================
--- build.xml	(revision 807293)
+++ build.xml	(working copy)
@@ -110,10 +110,9 @@
       </exec>
 	</sequential>
   </target>
-	
-  <target name="test-tag" depends="download-tag, compile-core, compile-demo, jar-core"
-  	description="Runs tests of a previous Lucene version. Specify tag version like this: -Dtag=branches/lucene_2_4_back_compat_tests">
-	<sequential>
+
+  <target name="compile-tag" depends="download-tag, compile-core, compile-demo, jar-core">
+    <sequential>
       <available property="tag.available" file="${tags.dir}/${tag}/src/test" />
 
 	  <fail unless="tag.available">
@@ -135,12 +134,15 @@
 	  <!-- compile tag tests against tag jar -->	
 	  <compile-test-macro srcdir="${tags.dir}/${tag}/src/test" destdir="${build.dir}/${tag}/classes/test"
 			  			  test.classpath="tag.test.classpath"/>
-		
-	  <!-- run tag tests against trunk jar -->
-      <test-macro dataDir="${tags.dir}/${tag}/src/test" 
-      			  tempDir="${build.dir}/${tag}"
-      			  junit.classpath="tag.junit.classpath"/>
-  	</sequential>
+    </sequential>
+  </target>
+	
+  <target name="test-tag"
+  	description="Runs tests of a previous Lucene version. Specify tag version like this: -Dtag=branches/lucene_2_9_back_compat_tests">
+    <!-- run tag tests against trunk jar -->
+    <test-macro dataDir="${tags.dir}/${tag}/src/test" 
+		tempDir="${build.dir}/${tag}"
+		junit.classpath="tag.junit.classpath"/>
   </target>	
 
 	
@@ -639,7 +641,7 @@
     <contrib-crawl target="build-artifacts-and-tests"/>
   </target>
 
-  <target name="test-contrib" depends="build-contrib">
+  <target name="test-contrib">
     <!-- Don't fail on error, instead check for flag file so we run
          all the tests possible and can "ant generate-test-reports"
          for all of them.
Index: contrib/contrib-build.xml
===================================================================
--- contrib/contrib-build.xml	(revision 807293)
+++ contrib/contrib-build.xml	(working copy)
@@ -61,7 +61,8 @@
   </target>
 
   
-  <target name="init" depends="common.init,build-lucene,build-lucene-tests"/>
+  <!--<target name="init" depends="common.init,build-lucene,build-lucene-tests"/>-->
+  <target name="init"/>
   <target name="compile-test" depends="init" if="contrib.has.tests">
     <antcall target="common.compile-test" inheritRefs="true" />
   </target>
Index: contrib/analyzers/common/src/test/org/apache/lucene/analysis/compound/TestCompoundWordTokenFilter.java
===================================================================
--- contrib/analyzers/common/src/test/org/apache/lucene/analysis/compound/TestCompoundWordTokenFilter.java	(revision 807293)
+++ contrib/analyzers/common/src/test/org/apache/lucene/analysis/compound/TestCompoundWordTokenFilter.java	(working copy)
@@ -42,12 +42,7 @@
 
 public class TestCompoundWordTokenFilter extends BaseTokenStreamTestCase {
   private static String[] locations = {
-      "http://dfn.dl.sourceforge.net/sourceforge/offo/offo-hyphenation.zip",
-      "http://surfnet.dl.sourceforge.net/sourceforge/offo/offo-hyphenation.zip",
-      "http://superb-west.dl.sourceforge.net/sourceforge/offo/offo-hyphenation.zip",
-      "http://voxel.dl.sourceforge.net/sourceforge/offo/offo-hyphenation.zip"};
-      // too slow:
-      //"http://superb-east.dl.sourceforge.net/sourceforge/offo/offo-hyphenation.zip"};
+    "file:///lucene/offo-hyphenation.zip"};
 
   private static byte[] patternsFileContent;
 
'''

BUILD_PATCH_30 = '''
Index: common-build.xml
===================================================================
--- common-build.xml	(revision 807293)
+++ common-build.xml	(working copy)
@@ -381,6 +381,8 @@
 	      <not><or>
 	        <isset property="testcase" />
 	      	<isset property="testpackage" />
+	      	<isset property="testpackagea" />
+	      	<isset property="testpackageb" />
 	      	<isset property="testpackageroot" />
 	      </or></not>
 	    </condition>
@@ -410,9 +412,15 @@
 	      <batchtest fork="yes" todir="@{junit.output.dir}" if="runall">
 	        <fileset dir="@{dataDir}" includes="${junit.includes}" excludes="${junit.excludes}"/>
 	      </batchtest>
-	      <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackage">
-	        <fileset dir="@{dataDir}" includes="**/${testpackage}/**/Test*.java,**/${testpackage}/**/*Test.java" excludes="${junit.excludes}"/>
-	      </batchtest>
+      <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackage">
+        <fileset dir="@{dataDir}" includes="**/${testpackage}/**/Test*.java,**/${testpackage}/**/*Test.java" excludes="${junit.excludes}"/>
+      </batchtest>
+       <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackagea">
+         <fileset dir="@{dataDir}" includes=" **/${testpackagea}/**/TestA*.java **/${testpackagea}/**/A*Test.java **/${testpackagea}/**/TestB*.java **/${testpackagea}/**/B*Test.java **/${testpackagea}/**/TestC*.java **/${testpackagea}/**/C*Test.java **/${testpackagea}/**/TestD*.java **/${testpackagea}/**/D*Test.java **/${testpackagea}/**/TestE*.java **/${testpackagea}/**/E*Test.java **/${testpackagea}/**/TestF*.java **/${testpackagea}/**/F*Test.java **/${testpackagea}/**/TestG*.java **/${testpackagea}/**/G*Test.java **/${testpackagea}/**/TestH*.java **/${testpackagea}/**/H*Test.java **/${testpackagea}/**/TestI*.java **/${testpackagea}/**/I*Test.java **/${testpackagea}/**/TestJ*.java **/${testpackagea}/**/J*Test.java **/${testpackagea}/**/TestK*.java **/${testpackagea}/**/K*Test.java **/${testpackagea}/**/TestL*.java **/${testpackagea}/**/L*Test.java" excludes="${junit.excludes}"/>
+       </batchtest>
+       <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackageb">
+         <fileset dir="@{dataDir}" includes=" **/${testpackageb}/**/TestM*.java **/${testpackageb}/**/M*Test.java **/${testpackageb}/**/TestN*.java **/${testpackageb}/**/N*Test.java **/${testpackageb}/**/TestO*.java **/${testpackageb}/**/O*Test.java **/${testpackageb}/**/TestP*.java **/${testpackageb}/**/P*Test.java **/${testpackageb}/**/TestQ*.java **/${testpackageb}/**/Q*Test.java **/${testpackageb}/**/TestR*.java **/${testpackageb}/**/R*Test.java **/${testpackageb}/**/TestS*.java **/${testpackageb}/**/S*Test.java **/${testpackageb}/**/TestT*.java **/${testpackageb}/**/T*Test.java **/${testpackageb}/**/TestU*.java **/${testpackageb}/**/U*Test.java **/${testpackageb}/**/TestV*.java **/${testpackageb}/**/V*Test.java **/${testpackageb}/**/TestW*.java **/${testpackageb}/**/W*Test.java **/${testpackageb}/**/TestX*.java **/${testpackageb}/**/X*Test.java **/${testpackageb}/**/TestY*.java **/${testpackageb}/**/Y*Test.java **/${testpackageb}/**/TestZ*.java **/${testpackageb}/**/Z*Test.java" excludes="${junit.excludes}"/>
+       </batchtest>
 	      <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackageroot">
 	        <fileset dir="@{dataDir}" includes="**/${testpackageroot}/Test*.java,**/${testpackageroot}/*Test.java" excludes="${junit.excludes}"/>
 	      </batchtest>
@@ -422,14 +430,12 @@
 	    </junit>
 	    <!-- create this file, then if we don't fail, delete it -->
 	    <!-- this meme makes it easy to tell if contribs have failed later -->
-	    <echo file="@{junit.output.dir}/junitfailed.flag">MAYBE</echo>
 	    <fail if="tests.failed">Tests failed!</fail>
 	    <!-- life would be easier if echo had an 'if' attribute like fail -->
-	    <delete file="@{junit.output.dir}/junitfailed.flag" />
   	</sequential>
   </macrodef>
 	
-  <target name="test" depends="compile-test" description="Runs unit tests">
+  <target name="test" description="Runs unit tests">
     <test-macro dataDir="src/test" tempDir="${build.dir}/test">
     	<contrib-settings>
 	      <!-- set as a system property so contrib tests can have a fixed root
Index: build.xml
===================================================================
--- build.xml	(revision 899586)
+++ build.xml	(working copy)
@@ -105,7 +105,7 @@
 	</sequential>
   </target>
 	
-  <target name="test-tag" depends="download-tag, compile-core, jar-core"
+  <target name="compile-tag" depends="download-tag, compile-core, jar-core"
   	description="Runs tests of a previous Lucene version. Specify tag version like this: -Dtag=branches/lucene_2_9_back_compat_tests">
 	<sequential>
       <available property="tag.available" file="${tags.dir}/${tag}/src/test" />
@@ -130,12 +130,15 @@
 	  <!-- compile tag tests against tag jar -->	
 	  <compile-test-macro srcdir="${tags.dir}/${tag}/src/test" destdir="${build.dir}/${tag}/classes/test"
 			  			  test.classpath="tag.test.classpath" javac.source="${javac.source.tag}" javac.target="${javac.target.tag}"/>
-		
+	  </sequential>
+  </target>	
+
+  <target name="test-tag"
+  	description="Runs tests of a previous Lucene version. Specify tag version like this: -Dtag=branches/lucene_2_9_back_compat_tests">
 	  <!-- run tag tests against trunk jar -->
       <test-macro dataDir="${tags.dir}/${tag}/src/test" 
       			  tempDir="${build.dir}/${tag}"
       			  junit.classpath="tag.junit.classpath"/>
-  	</sequential>
   </target>	
 
 	
@@ -662,7 +665,7 @@
     <contrib-crawl target="build-artifacts-and-tests"/>
   </target>
 
-  <target name="test-contrib" depends="build-contrib">
+  <target name="test-contrib">
     <!-- Don't fail on error, instead check for flag file so we run
          all the tests possible and can "ant generate-test-reports"
          for all of them.
Index: contrib/contrib-build.xml
===================================================================
--- contrib/contrib-build.xml	(revision 807293)
+++ contrib/contrib-build.xml	(working copy)
@@ -61,7 +61,8 @@
   </target>
 
   
-  <target name="init" depends="common.init,build-lucene,build-lucene-tests"/>
+  <!--<target name="init" depends="common.init,build-lucene,build-lucene-tests"/>-->
+  <target name="init"/>
   <target name="compile-test" depends="init" if="contrib.has.tests">
     <antcall target="common.compile-test" inheritRefs="true" />
   </target>
Index: contrib/analyzers/common/src/test/org/apache/lucene/analysis/compound/TestCompoundWordTokenFilter.java
===================================================================
--- contrib/analyzers/common/src/test/org/apache/lucene/analysis/compound/TestCompoundWordTokenFilter.java	(revision 807293)
+++ contrib/analyzers/common/src/test/org/apache/lucene/analysis/compound/TestCompoundWordTokenFilter.java	(working copy)
@@ -42,12 +42,7 @@
 
 public class TestCompoundWordTokenFilter extends BaseTokenStreamTestCase {
   private static String[] locations = {
-      "http://dfn.dl.sourceforge.net/sourceforge/offo/offo-hyphenation.zip",
-      "http://surfnet.dl.sourceforge.net/sourceforge/offo/offo-hyphenation.zip",
-      "http://superb-west.dl.sourceforge.net/sourceforge/offo/offo-hyphenation.zip",
-      "http://voxel.dl.sourceforge.net/sourceforge/offo/offo-hyphenation.zip"};
-      // too slow:
-      //"http://superb-east.dl.sourceforge.net/sourceforge/offo/offo-hyphenation.zip"};
+    "file:///lucene/offo-hyphenation.zip"};
 
   private static byte[] patternsFileContent;
 
'''

BUILD_PATCH_FLEX = '''
Index: common-build.xml
===================================================================
--- common-build.xml	(revision 807293)
+++ common-build.xml	(working copy)
@@ -381,6 +381,8 @@
 	      <not><or>
 	        <isset property="testcase" />
 	      	<isset property="testpackage" />
+	      	<isset property="testpackagea" />
+	      	<isset property="testpackageb" />
 	      	<isset property="testpackageroot" />
 	      </or></not>
 	    </condition>
@@ -410,9 +412,15 @@
 	      <batchtest fork="yes" todir="@{junit.output.dir}" if="runall">
 	        <fileset dir="@{dataDir}" includes="${junit.includes}" excludes="${junit.excludes}"/>
 	      </batchtest>
-	      <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackage">
-	        <fileset dir="@{dataDir}" includes="**/${testpackage}/**/Test*.java,**/${testpackage}/**/*Test.java" excludes="${junit.excludes}"/>
-	      </batchtest>
+      <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackage">
+        <fileset dir="@{dataDir}" includes="**/${testpackage}/**/Test*.java,**/${testpackage}/**/*Test.java" excludes="${junit.excludes}"/>
+      </batchtest>
+       <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackagea">
+         <fileset dir="@{dataDir}" includes=" **/${testpackagea}/**/TestA*.java **/${testpackagea}/**/A*Test.java **/${testpackagea}/**/TestB*.java **/${testpackagea}/**/B*Test.java **/${testpackagea}/**/TestC*.java **/${testpackagea}/**/C*Test.java **/${testpackagea}/**/TestD*.java **/${testpackagea}/**/D*Test.java **/${testpackagea}/**/TestE*.java **/${testpackagea}/**/E*Test.java **/${testpackagea}/**/TestF*.java **/${testpackagea}/**/F*Test.java **/${testpackagea}/**/TestG*.java **/${testpackagea}/**/G*Test.java **/${testpackagea}/**/TestH*.java **/${testpackagea}/**/H*Test.java **/${testpackagea}/**/TestI*.java **/${testpackagea}/**/I*Test.java **/${testpackagea}/**/TestJ*.java **/${testpackagea}/**/J*Test.java **/${testpackagea}/**/TestK*.java **/${testpackagea}/**/K*Test.java **/${testpackagea}/**/TestL*.java **/${testpackagea}/**/L*Test.java" excludes="${junit.excludes}"/>
+       </batchtest>
+       <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackageb">
+         <fileset dir="@{dataDir}" includes=" **/${testpackageb}/**/TestM*.java **/${testpackageb}/**/M*Test.java **/${testpackageb}/**/TestN*.java **/${testpackageb}/**/N*Test.java **/${testpackageb}/**/TestO*.java **/${testpackageb}/**/O*Test.java **/${testpackageb}/**/TestP*.java **/${testpackageb}/**/P*Test.java **/${testpackageb}/**/TestQ*.java **/${testpackageb}/**/Q*Test.java **/${testpackageb}/**/TestR*.java **/${testpackageb}/**/R*Test.java **/${testpackageb}/**/TestS*.java **/${testpackageb}/**/S*Test.java **/${testpackageb}/**/TestT*.java **/${testpackageb}/**/T*Test.java **/${testpackageb}/**/TestU*.java **/${testpackageb}/**/U*Test.java **/${testpackageb}/**/TestV*.java **/${testpackageb}/**/V*Test.java **/${testpackageb}/**/TestW*.java **/${testpackageb}/**/W*Test.java **/${testpackageb}/**/TestX*.java **/${testpackageb}/**/X*Test.java **/${testpackageb}/**/TestY*.java **/${testpackageb}/**/Y*Test.java **/${testpackageb}/**/TestZ*.java **/${testpackageb}/**/Z*Test.java" excludes="${junit.excludes}"/>
+       </batchtest>
 	      <batchtest fork="yes" todir="@{junit.output.dir}" if="testpackageroot">
 	        <fileset dir="@{dataDir}" includes="**/${testpackageroot}/Test*.java,**/${testpackageroot}/*Test.java" excludes="${junit.excludes}"/>
 	      </batchtest>
@@ -422,14 +430,12 @@
 	    </junit>
 	    <!-- create this file, then if we don't fail, delete it -->
 	    <!-- this meme makes it easy to tell if contribs have failed later -->
-	    <echo file="@{junit.output.dir}/junitfailed.flag">MAYBE</echo>
 	    <fail if="tests.failed">Tests failed!</fail>
 	    <!-- life would be easier if echo had an 'if' attribute like fail -->
-	    <delete file="@{junit.output.dir}/junitfailed.flag" />
   	</sequential>
   </macrodef>
 	
-  <target name="test" depends="compile-test" description="Runs unit tests">
+  <target name="test" description="Runs unit tests">
     <test-macro dataDir="src/test" tempDir="${build.dir}/test">
     	<contrib-settings>
 	      <!-- set as a system property so contrib tests can have a fixed root
Index: build.xml
===================================================================
--- build.xml	(revision 899584)
+++ build.xml	(working copy)
@@ -119,7 +119,7 @@
   <!-- remove this -->
   <target name="test-tag" depends="test-backwards" description="deprecated"/>
 
-  <target name="test-backwards" depends="download-backwards, compile-core, jar-core"
+  <target name="compile-backwards"
   	description="Runs tests of a previous Lucene version.">
 	<sequential>
     <available property="backwards.tests.available" file="${backwards.dir}/${backwards.branch}/src/test" />
@@ -143,14 +143,18 @@
 		  	
 	  <!-- compile branch tests against branch jar -->	
 	  <compile-test-macro srcdir="${backwards.dir}/${backwards.branch}/src/test" destdir="${build.dir}/${backwards.branch}/classes/test"
+
 			  			  test.classpath="backwards.test.classpath" javac.source="${javac.source.backwards}" javac.target="${javac.target.backwards}"/>
-		
+	  </sequential>
+  </target>	
+
+  <target name="test-backwards"
+  	description="Runs tests of a previous Lucene version.">
 	  <!-- run branch tests against trunk jar -->
       <test-macro dataDir="${backwards.dir}/${backwards.branch}/src/test" 
       			  tempDir="${build.dir}/${backwards.branch}"
       			  junit.classpath="backwards.junit.classpath"
               junit.output.dir="${junit.output.dir.backwards}" />
-  	</sequential>
   </target>	
 
 	
@@ -683,7 +687,7 @@
     <contrib-crawl target="build-artifacts-and-tests"/>
   </target>
 
-  <target name="test-contrib" depends="build-contrib">
+  <target name="test-contrib">
     <!-- Don't fail on error, instead check for flag file so we run
          all the tests possible and can "ant generate-test-reports"
          for all of them.
Index: contrib/contrib-build.xml
===================================================================
--- contrib/contrib-build.xml	(revision 807293)
+++ contrib/contrib-build.xml	(working copy)
@@ -61,7 +61,8 @@
   </target>
 
   
-  <target name="init" depends="common.init,build-lucene,build-lucene-tests"/>
+  <!--<target name="init" depends="common.init,build-lucene,build-lucene-tests"/>-->
+  <target name="init"/>
   <target name="compile-test" depends="init" if="contrib.has.tests">
     <antcall target="common.compile-test" inheritRefs="true" />
   </target>
Index: contrib/snowball/build.xml
===================================================================
--- contrib/snowball/build.xml	(revision 899398)
+++ contrib/snowball/build.xml	(working copy)
@@ -131,7 +131,7 @@
   </target>
 
   <target name="compile-core" depends="build-analyzers, common.compile-core" />
-  <target name="compile-test" depends="download-vocab-tests, common.compile-test" />
+  <target name="compile-test" depends="common.compile-test" />
   
   <target name="build-analyzers" unless="analyzers.jar.present">
     <echo>Snowball building dependency ${analyzers.jar}</echo>
'''

if __name__ == '__main__':
  main()
