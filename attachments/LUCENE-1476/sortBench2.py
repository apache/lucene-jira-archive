import time
import os
import shutil
import sys
import cPickle

# rm -rf logs; mkdir logs; python -uOO run.py

# TODO
#   - run on clean too
#   - do different queue sizes

print 'compile...'
if '-nocompile' not in sys.argv:
  if os.system('ant compile >& compile.log') != 0:
    raise RuntimeError('compile failed (see compile.log)')

ALG = '''
analyzer=org.apache.lucene.analysis.WhitespaceAnalyzer
directory=FSDirectory
work.dir = $WORK$
search.num.hits = 10
query.maker=org.apache.lucene.benchmark.byTask.feeds.FileBasedQueryMaker
file.query.maker.file = queries.txt
log.queries=true

OpenReader  
{"XSearchWarm" Search}
{ "Rounds"
  { "Run"
    { "TestSortSpeed"
      { "XSearchReal" Search > : 3.0s
    }
    NewRound
  } : $NROUND$
} 
CloseReader 
RepSumByPrefRound XSearch
'''

FAST = '-fast' in sys.argv

if '-queries' in sys.argv:
  queries = sys.argv[sys.argv.index('-queries')+1].split(',')
else:
  queries = ('147', 'text', '1 AND 2', '1', '1 OR 2')

if os.path.exists('searchlogs'):
  shutil.rmtree('searchlogs')

os.makedirs('searchlogs')

open('results.txt', 'wb').write('ptgDel\tquery\thits\twarmsec\tqps\n')

def run(pDel, query):

  t0 = time.time()

  s = ALG

  index = '/lucene/work.wiki2M.1seg.%dpdeletes' % pDel

  s = s.replace('$WORK$', index);
  
  open('queries.txt', 'wb').write(query + '\n')

  if FAST:
    nround = 2
  else:
    nround = 5

  s = s.replace('$NROUND$', str(nround))

  fileOut = 'searchlogs/del_%s.query_%s' % (pDel, query.replace(' ', '_'))

  open('tmp.alg', 'wb').write(s)

  command = 'java -Xms1024M -Xmx1024M -Xbatch -server -classpath ../../build/classes/java:../../build/classes/demo:../../build/contrib/highlighter/classes/java:../../contrib/benchmark/lib/commons-digester-1.7.jar:../../contrib/benchmark/lib/commons-collections-3.1.jar:../../contrib/benchmark/lib/commons-logging-1.0.4.jar:../../contrib/benchmark/lib/commons-beanutils-1.7.0.jar:../../contrib/benchmark/lib/xerces-2.9.0.jar:../../contrib/benchmark/lib/xml-apis-2.9.0.jar:../../build/contrib/benchmark/classes/java org.apache.lucene.benchmark.byTask.Benchmark tmp.alg > %s' % fileOut

  print '  %s' % fileOut

  res = os.system(command)

  if res != 0:
    raise RuntimeError('FAILED')

  best = None
  count = 0
  nhits = None
  warmTime = None
  
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

  open('results.txt', 'ab').write('%s\t%s\t%s\t%6.1f\t%6.1f\n' % \
                                  (pDel, query, nhits, warmTime, best))

  print '  %.1f qps; %.1f sec' %  (best, time.time()-t0)
  all.append((pDel, query, nhits, warmTime, best))
    
trunk = '-trunk' in sys.argv
all = []
for pDel in (0, 1, 2, 5, 10, 20, 50):
  for query in queries:
    print
    print 'RUN pctDel=%s query=%s' % (pDel, query)

    run(pDel, query)

    f = open('results.pk', 'wb')
    cPickle.dump(all, f)
    f.close()

