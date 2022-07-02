import time
import os
import shutil
import sys
import cPickle
import datetime

if sys.platform.lower().find('darwin') != -1:
  windows = False
elif sys.platform.lower().find('win') != -1:
  windows = True
else:
  windows = False

# rm -rf logs; mkdir logs; python -uOO run.py

# TODO
#   - run on clean too
#   - do different queue sizes

print 'compile...'
if '-nocompile' not in sys.argv:
  if os.system('ant compile > compile.log 2>&1') != 0:
    raise RuntimeError('compile failed (see compile.log)')

if windows:
  RESULTS = 'results.win64'
else:
  RESULTS = 'results'

ALG = '''
analyzer=org.apache.lucene.analysis.standard.StandardAnalyzer
directory=FSDirectory
work.dir = $INDEX$
search.num.hits = 10
query.maker=org.apache.lucene.benchmark.byTask.feeds.FileBasedQueryMaker
file.query.maker.file = queries.txt
log.queries=true

OpenReader  
{"XSearchWarm" Search}
{ "Rounds"
  { "Run"
    { "TestSearchSpeed"
      { "XSearchReal" $SEARCH$ > : 3.0s
    }
    NewRound
  } : $NROUND$
} 
CloseReader 
RepSumByPrefRound XSearch
'''

if windows:
  ALG = ALG.replace('$INDEX$', 'c:/lucene/work.wiki2M.1seg.0pdeletes')
else:
  ALG = ALG.replace('$INDEX$', '/lucene/work.wikifull.14seg')

if '-queries' in sys.argv:
  queries = sys.argv[sys.argv.index('-queries')+1].split(',')
else:
  #queries = ('147', 'text', '1 AND 2', '1', '1 OR 2')
  #queries = ('147', 'text', '1')
  #queries = ('text', '1')
  #queries = ('1', '"united states"', '1 OR 2', '1 AND 2', '1 OR 2 OR 3 OR 4', '1 AND 2 AND 3 AND 4', '1 OR 2 OR 3 OR 4 OR 5 OR 6 OR 7 OR 8 OR 9 OR 10')
  #queries = ('1 OR 2',)

  #queries = ('1', '"united states"', '1 OR 2', '1 AND 2', '1 OR 2 OR 3 OR 4', '1 AND 2 AND 3 AND 4', '1 OR 2 OR 3 OR 4 OR 5 OR 6 OR 7 OR 8 OR 9 OR 10', '1 AND 2 AND 3 AND 4 AND 5 AND 6 AND 7 AND 8 AND 9 AND 10')
  #queries = ('1 OR 2', '1 OR 2 OR 3 OR 4', '1 OR 2 OR 3 OR 4 OR 5 OR 6 OR 7 OR 8 OR 9 OR 10',)
  #queries = ('1 OR 2', '1 AND 2')
  #queries = ('1 OR 2', '1 OR 2 OR 3 OR 4', '1 OR 2 OR 3 OR 4 OR 5 OR 6 OR 7 OR 8 OR 9 OR 10')
  queries = ('147', 'text', '1', '1 OR 2', '1 AND 2')
  
if os.path.exists('searchlogs'):
  shutil.rmtree('searchlogs')

os.makedirs('searchlogs')

open('%s.txt' % RESULTS, 'wb').write('query\tsort\thits\twarm\tqps\n')

def run(query, sortField):

  t0 = time.time()

  s = ALG

  open('queries.txt', 'wb').write(query + '\n')

  nround = 5

  s = s.replace('$NROUND$', str(nround))

  if sortField == 'score':
    sortStep = 'Search'
  elif sortField == 'doc':
    sortStep = 'SearchWithSort(doc)'
  elif sortField == 'title':
    sortStep = 'SearchWithSort(doctitle:string)'
  else:
    raise RuntimeError("no")

  s = s.replace('$SEARCH$', sortStep)  

  fileOut = 'searchlogs/query_%s_%s' % (query.replace(' ', '_').replace('"', ''), sortField)

  open('tmp.alg', 'wb').write(s)

  if windows:
    command = 'java -Xms1024M -Xmx1024M -Xbatch -server -classpath "../../build/classes/java;../../build/classes/demo;../../build/contrib/highlighter/classes/java;../../contrib/benchmark/lib/commons-digester-1.7.jar;../../contrib/benchmark/lib/commons-collections-3.1.jar;../../contrib/benchmark/lib/commons-logging-1.0.4.jar;../../contrib/benchmark/lib/commons-beanutils-1.7.0.jar;../../contrib/benchmark/lib/xerces-2.9.0.jar;../../contrib/benchmark/lib/xml-apis-2.9.0.jar;../../build/contrib/benchmark/classes/java" org.apache.lucene.benchmark.byTask.Benchmark tmp.alg > %s' % fileOut
  else:
    command = 'java -Xms1024M -Xmx1024M -Xbatch -server -classpath ../../build/classes/java:../../build/classes/demo:../../build/contrib/highlighter/classes/java:../../contrib/benchmark/lib/commons-digester-1.7.jar:../../contrib/benchmark/lib/commons-collections-3.1.jar:../../contrib/benchmark/lib/commons-logging-1.0.4.jar:../../contrib/benchmark/lib/commons-beanutils-1.7.0.jar:../../contrib/benchmark/lib/xerces-2.9.0.jar:../../contrib/benchmark/lib/xml-apis-2.9.0.jar:../../build/contrib/benchmark/classes/java org.apache.lucene.benchmark.byTask.Benchmark tmp.alg > %s' % fileOut

  print '  %s' % fileOut

  res = os.system(command)

  if res != 0:
    raise RuntimeError('FAILED')

  best = None
  count = 0
  nhits = None
  warmTime = None
  filt = None
  meths = []
  
  for line in open(fileOut, 'rb').readlines():
    if line.startswith('NUMHITS='):
      nhits = int(line[8:].strip())
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
            #print 'GOT: %s' % v[i-1]
            qps = float(v[i-1].replace(',', ''))
            break

      if qps is None:
        raise RuntimeError('did not find qps')
      
      count += 1
      if best is None or qps > best:
        best = qps

  if count != nround:
    raise RuntimeError('did not find %s rounds (got %s)' % (nround, count))

  if warmTime is None:
    raise RuntimeError('did not find warm time')
    
  if nhits is None:
    raise RuntimeError('did not see NUMHITS=line')
  
  print '  NHIT: %s' % nhits

  open('%s.txt' % RESULTS, 'ab').write('%s\t%s\t%s\t%6.1f\t%6.1f\n' % \
                                  (query, sortBy, nhits, warmTime, best))

  print '  %.1f qps; %.1f sec' %  (best, time.time()-t0)
  all.append((query, sortBy, nhits, warmTime, best))
    
trunk = '-trunk' in sys.argv
all = []

for query in queries:

  for sortBy in ('score', 'title', 'doc'):

    print
    print 'RUN query=%s sort=%s [%s]' % (query, sortBy, datetime.datetime.now())

    run(query, sortBy)

    f = open('%s.pk' % RESULTS, 'wb')
    cPickle.dump(all, f)
    f.close()

