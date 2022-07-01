import types
import re
import time
import os
import shutil
import sys
import cPickle
import datetime

# TODO
#   - verify step
#   - run searches
#   - get all docs query in here

if sys.platform.lower().find('darwin') != -1:
  osName = 'osx'
elif sys.platform.lower().find('win') != -1:
  osName = 'windows'
elif sys.platform.lower().find('linux') != -1:
  osName = 'linux'
else:
  osName = 'unix'

DEBUG = False

# let shell find it:
#JAVA_COMMAND = 'java -Xms2g -Xmx2g -server'
#JAVA_COMMAND = 'java -Xms1024M -Xmx1024M -Xbatch -server -XX:+AggressiveOpts -XX:CompileThreshold=100 -XX:+UseFastAccessorMethods'

LOG_SUB_DIR = 'logs'

BASE_SEARCH_ALG = '''
analyzer=org.apache.lucene.analysis.standard.StandardAnalyzer
directory=FSDirectory
work.dir = $INDEX$
search.num.hits = $NUM_HITS$
query.maker=org.apache.lucene.benchmark.byTask.feeds.FileBasedQueryMaker
file.query.maker.file = queries.txt
log.queries=true
log.step=100000
print.hits.field=$PRINT_HITS_FIELD$

OpenReader  
{"XSearchWarm" $SEARCH$}
SetProp(print.hits.field,)
$ROUNDS$
CloseReader 
RepSumByPrefRound XSearch
'''

BASE_INDEX_ALG = '''
analyzer=org.apache.lucene.analysis.standard.StandardAnalyzer

$OTHER$

doc.index.props = true

doc.stored = true
doc.body.stored = false

sort.rng = 1000000
rand.seed=17

doc.tokenized = false
doc.body.tokenized = true

doc.term.vector = false
log.step.AddDoc=10000
#writer.info.stream = SystemOut

directory=FSDirectory
compound=false
ram.flush.mb = 256

work.dir=$WORKDIR$

{ "BuildIndex"
  - CreateIndex
  $INDEX_LINE$
  - Optimize
  - CloseIndex
}

RepSumByPrefRound BuildIndex
'''

class SearchResult:

  def __init__(self, job, numHits, warmTime, bestQPS, hits):

    self.job = job
    self.warmTime = warmTime
    self.bestQPS = bestQPS
    self.hits = hits
    self.numHits = numHits

class Job:

  def __init__(self, cat, label, numIndexDocs, alg, queries=None, numRounds=None):

    # index or search
    self.cat = cat

    # eg clean, flex, csf, etc
    self.label = label

    self.queries = queries

    self.numRounds = numRounds

    self.numIndexDocs = numIndexDocs
    self.alg = alg

class SearchJob(Job):
  def __init__(self, label, numIndexDocs, alg, queries, numRounds):
    Job.__init__(self, 'search', label, numIndexDocs, alg, queries, numRounds)

class IndexJob(Job):
  def __init__(self, label, numIndexDocs, alg):
    Job.__init__(self, 'index', label, numIndexDocs, alg)

class RunAlgs:

  def __init__(self, baseDir, javaCommand):
    self.baseDir = baseDir
    self.logCounter = 0
    self.results = []
    self.compiled = set()
    self.javaCommand = javaCommand
    
  def printEnv(self):
    print
    print 'JAVA:\n%s' % os.popen('%s -version 2>&1' % self.javaCommand).read()

    print
    if osName != 'windows':
      print 'OS:\n%s' % os.popen('uname -a 2>&1').read()
    else:
      print 'OS:\n%s' % sys.platform
      
  def makeIndex(self, workingDir, prefix, source, numDocs, numThreads, lineDocSource=None, xmlDocSource=None):

    if source not in ('wiki', 'random'):
      raise RuntimeError('source must be wiki or random')

    indexName = '%s.work.%s.nd%gM' % (prefix, source, numDocs/1000000.0)
    fullIndexPath = '%s/%s' % (self.baseDir, indexName)
    
    if os.path.exists(fullIndexPath):
      print 'Index %s already exists...' % fullIndexPath
      return fullIndexPath

    print 'Now create index %s...' % fullIndexPath

    alg = self.getIndexAlg(fullIndexPath, source, numDocs, numThreads, lineDocSource=lineDocSource, xmlDocSource=xmlDocSource)

    job = IndexJob(prefix, numDocs, alg)

    try:
      self.runOne(workingDir, job, 'index.%s' % prefix)
    except:
      if os.path.exists(fullIndexPath):
        shutil.rmtree(fullIndexPath)
      raise

    return fullIndexPath
    
  def runOne(self, workingDir, job, verify=False):

    logFileName = '%s.%d' % (job.label, self.logCounter)
    self.logCounter += 1

    savDir = os.getcwd()
    os.chdir(workingDir)
    print '    cd %s' % workingDir

    try:

      if False and workingDir not in self.compiled:
        self.compiled.add(workingDir)
        print '  compile...'
        if os.system('ant compile > compile.log 2>&1') != 0:
          raise RuntimeError('ant compile failed in %s' % workingDir)

      if job.queries is not None:
        if type(job.queries) in types.StringTypes:
          job.queries = [job.queries]
        open('queries.txt', 'wb').write('\n'.join(job.queries))

      if DEBUG:
        algFile = 'tmp.alg'
      else:
        algFile = 'tmp.%s.alg' % os.getpid()
      open(algFile, 'wb').write(job.alg)

      if not os.path.exists(LOG_SUB_DIR):
        os.makedirs(LOG_SUB_DIR)

      fullLogFileName = '%s/%s' % (LOG_SUB_DIR, logFileName)
      print '    log: %s/%s' % (workingDir, fullLogFileName)

      command = '%s -classpath ../../build/classes/java:../../build/classes/demo:../../build/contrib/highlighter/classes/java:lib/commons-digester-1.7.jar:lib/commons-collections-3.1.jar:lib/commons-compress-1.0.jar:lib/commons-logging-1.0.4.jar:lib/commons-beanutils-1.7.0.jar:lib/xerces-2.9.0.jar:lib/xml-apis-2.9.0.jar:../../build/contrib/benchmark/classes/java org.apache.lucene.benchmark.byTask.Benchmark %s > "%s" 2>&1' % \
                (self.javaCommand, algFile, fullLogFileName)

      if DEBUG:
        print 'command=%s' % command

      success = False
      try:
        t0 = time.time()
        if os.system(command) != 0:
          raise RuntimeError('FAILED')
        t1 = time.time()
        success = True
      finally:
        if not DEBUG:
          if not success:
            os.rename(algFile, 'tmp.alg')
          else:
            os.remove(algFile)

      if job.cat == 'index':
        s = open(fullLogFileName, 'rb').read()
        if s.find('Exception in thread "') != -1 or s.find('at org.apache.lucene') != -1:
          raise RuntimeError('alg hit exceptions')
        return

      else:

        # Parse results:
        bestQPS = None
        count = 0
        nhits = None
        ndocs = None
        warmTime = None
        r = re.compile('^  ([0-9]+): (.*)$')
        topN = []

        for line in open(fullLogFileName, 'rb').readlines():
          m = r.match(line.rstrip())
          if m is not None:
            topN.append(m.group(2))
          if line.startswith('totalHits ='):
            nhits = int(line[11:].strip())
          if line.startswith('numDocs() ='):
            ndocs = int(line[11:].strip())
          if line.startswith('maxDoc()  ='):
            maxDoc = int(line[11:].strip())
          if line.startswith('XSearchWarm'):
            v = line.strip().split()
            warmTime = float(v[5])
          if line.startswith('XSearchReal'):
            v = line.strip().split()
            # print len(v), v
            upto = 0
            i = 0
            qps = None
            while i < len(v):
              if v[i] == '-':
                i += 1
                continue
              else:
                upto += 1
                i += 1
                if upto == 5:
                  qps = float(v[i-1].replace(',', ''))
                  break

            if qps is None:
              raise RuntimeError('did not find qps')

            count += 1
            if bestQPS is None or qps > bestQPS:
              bestQPS = qps

        if not verify:
          if count != job.numRounds:
            raise RuntimeError('did not find %s rounds (got %s)' % (job.numRounds, count))
          if warmTime is None:
            raise RuntimeError('did not find warm time')
        else:
          bestQPS = 1.0
          warmTime = None

        if nhits is None:
          raise RuntimeError('did not see totalHits line')

        if ndocs is None:
          raise RuntimeError('did not see numDocs line')

        if ndocs != job.numIndexDocs:
          raise RuntimeError('indexNumDocs mismatch: expected %d but got %d' % (job.numIndexDocs, ndocs))

        return SearchResult(job, nhits, warmTime, bestQPS, topN)
    finally:
      os.chdir(savDir)
                           
  def getIndexAlg(self, fullIndexPath, source, numDocs, numThreads, lineDocSource=None, xmlDocSource=None):

    s = BASE_INDEX_ALG

    if source == 'wiki':
      if lineDocSource is not None:
        s2 = '''
content.source=org.apache.lucene.benchmark.byTask.feeds.LineDocSource
docs.file=%s
''' % lineDocSource
      elif xmlDocSource is not None:
        s2 = '''
content.source=org.apache.lucene.benchmark.byTask.feeds.EnwikiContentSource
docs.file=%s
''' % xmlDocSource
      else:
        raise RuntimeError('if source is wiki, either lineDocSource or xmlDocSource must be set')
        
    elif source == 'random':
      s2 = '''
content.source=org.apache.lucene.benchmark.byTask.feeds.SortableSingleDocSource
'''
    else:
      raise RuntimeError('source must be wiki or random (got "%s")' % source)
                         
    if numThreads > 1:
      # other += 'doc.reuse.fields=false\n'
      s = s.replace('$INDEX_LINE$', '[ { "AddDocs" AddDoc > : %s } : %s' % \
                    (numDocs/numThreads, numThreads))
    else:
      s = s.replace('$INDEX_LINE$', '{ "AddDocs" AddDoc > : %s' % \
                    (numDocs))

    s = s.replace('$WORKDIR$', fullIndexPath)
    s = s.replace('$OTHER$', s2)

    return s

  def getSearchAlg(self, indexPath, searchTask, numHits, numRounds, verify=False):

    s = BASE_SEARCH_ALG
    
    if not verify:
      s = s.replace('$ROUNDS$',
  '''                
  { "Rounds"
    { "Run"
      { "TestSearchSpeed"
        { "XSearchReal" $SEARCH$ > : 5.0s
      }
      NewRound
    } : %d
  } 
  ''' % numRounds)
    else:
      s = s.replace('$ROUNDS$', '')

    s = s.replace('$INDEX$', indexPath)
    s = s.replace('$SEARCH$', searchTask)
    s = s.replace('$NUM_HITS$', str(numHits))
    
    return s

  def compare(self, baseline, newList, *params):

    for new in newList:
      if new.numHits != baseline.numHits:
        raise RuntimeError('baseline found %d hits but new found %d hits' % (baseline[0], new[0]))

      warmOld = baseline.warmTime
      warmNew = new.warmTime
      qpsOld = baseline.bestQPS
      qpsNew = new.bestQPS
      pct = 100.0*(qpsNew-qpsOld)/qpsOld
      #print '  diff: %.1f%%' % pct

      pct = 100.0*(warmNew-warmOld)/warmOld
      #print '  warmdiff: %.1f%%' % pct

    self.results.append([baseline] + [newList] + list(params))

  def save(self, name):
    f = open('%s.pk' % name, 'wb')
    cPickle.dump(self.results, f)
    f.close()

def getPct(old, new):
  pct = 100.0*(new-old)/old
  if pct < 0.0:
    c = 'red'
  else:
    c = 'green'
  return '{color:%s}%.1f%%{color}' % (c, pct)

def cleanScores(l):
  for i in range(len(l)):
    pos = l[i].find(' score=')
    l[i] = l[i][:pos].strip()
  
def verify(r1, r2):
  if r1.numHits != r2.numHits:
    raise RuntimeError('different total hits: %s vs %s' % (r1.numHits, r2.numHits))
                       
  h1 = r1.hits
  h2 = r2.hits
  if len(h1) != len(h2):
    raise RuntimeError('different number of results')
  else:
    for i in range(len(h1)):
      s1 = h1[i].replace('score=NaN', 'score=na').replace('score=0.0', 'score=na')
      s2 = h2[i].replace('score=NaN', 'score=na').replace('score=0.0', 'score=na')
      if s1 != s2:
        raise RuntimeError('hit %s differs: %s vs %s' % (i, repr(s1), repr(s2)))
