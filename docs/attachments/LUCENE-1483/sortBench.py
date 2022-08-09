import time
import os
import shutil
import sys

# rm -rf logs; mkdir logs; python -uOO run.py

# TODO
#   - run on clean too
#   - do different queue sizes

ALG = '''
analyzer=org.apache.lucene.analysis.WhitespaceAnalyzer
directory=FSDirectory
work.dir = $WORK$
search.num.hits = $NUMHITS$
query.maker=org.apache.lucene.benchmark.byTask.feeds.FileBasedQueryMaker
file.query.maker.file = queries.txt
log.queries=true

OpenReader  
{"XSearchWarm" $SEARCH$}
{ "Rounds"
  { "Run"
    { "TestSortSpeed"
      { "XSearchWithSort" $SEARCH$ > : $ITER$
    }
    NewRound
  } : $NROUND$
} 
CloseReader 
RepSumByPrefRound XSearch
'''

FAST = '-fast' in sys.argv

if os.path.exists('sortlogs'):
  shutil.rmtree('sortlogs')

os.makedirs('sortlogs')

f = open('results.txt', 'wb')
f.write('numSeg\tindex\tsortBy\tquery\ttopN\tmeth\thits\twarmsec\tqps\n')
f.close()

def run(numSeg, sortField, method, query, numHits):

  t0 = time.time()

  s = ALG

  if sortField in ('score', 'title', 'doc'):
    index = '/lucene/work.wiki2M.%dseg' % numSeg
    indexLabel = 'wiki'
  else:
    index = '/lucene/work.sortable2M.%dseg' % numSeg
    indexLabel = 'simple'
    
  s = s.replace('$WORK$', index);
  
  if sortField == 'score':
    sortStep = 'Search'
  elif sortField == 'doc':
    sortStep = 'SearchWithSort(doc)'
  else:
    if method == 'val':
      v = 'string_val'
    elif method == 'string':
      # trunk only
      v = 'string'
    elif method == 'ord':
      v = 'string_ord'
    elif method == 'ordval':
      v = 'string_ord_val'
    else:
      v = method

    if sortField == 'title':
      sf = 'doctitle'
    else:
      sf = sortField
      
    sortStep = 'SearchWithSort(%s:%s)' % (sf, v)
  
  s = s.replace('$SEARCH$', sortStep)
  open('queries.txt', 'wb').write(query + '\n')

  # guestimate reasonable number of iters so that search takes long
  # enough to get more accurate results:
  if method == 'int' or query =='*:*' or indexLabel == 'simple':
    # 2M hits
    iter = 60
  elif query == 'text':
    iter = 300
  elif query == '1':
    iter = 90
  elif query == '147':
    iter = 5000

  if method in ('string', 'val'):
    iter /= 3

  if FAST:
    nround = 2
  else:
    nround = 5
    iter *= 2

  s = s.replace('$NROUND$', str(nround))
  s = s.replace('$ITER$', str(iter))
  s = s.replace('$NUMHITS$', str(numHits))

  fileOut = 'sortlogs/sort_%s' % sortField
  if method is not None:
    fileOut += '.meth_%s' % method
  fileOut += '.query_%s' % query
  fileOut += '.hits_%s' % numHits

  open('tmp.alg', 'wb').write(s)

  command = 'java -Xms1024M -Xmx1024M -Xbatch -server -classpath ../../build/classes/java:../../build/classes/demo:../../build/contrib/highlighter/classes/java:../../contrib/benchmark/lib/commons-digester-1.7.jar:../../contrib/benchmark/lib/commons-collections-3.1.jar:../../contrib/benchmark/lib/commons-logging-1.0.4.jar:../../contrib/benchmark/lib/commons-beanutils-1.7.0.jar:../../contrib/benchmark/lib/xerces-2.9.0.jar:../../contrib/benchmark/lib/xml-apis-2.9.0.jar:../../build/contrib/benchmark/classes/java org.apache.lucene.benchmark.byTask.Benchmark tmp.alg > %s' % fileOut

  print '  %s' % fileOut

  res = os.system(command)

  if res != 0:
    raise RuntimeError('FAILED')

  best = None
  count = 0
  comp = None
  nhits = None
  warmTime = None
  
  for line in open(fileOut, 'rb').readlines():
    if line.startswith('COMP='):
      comp = line[5:].strip()
    if line.startswith('NUMHITS='):
      nhits = int(line[8:].strip())
    if line.startswith('XSearchWarm'):
      v = line.strip().split()
      warmTime = float(v[5])
    if line.startswith('XSearchWithSort_'):
      v = line.strip().split()
      #print len(v), v
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
    
  if method is not None:
    if comp is None:
      raise RuntimeError('did not see COMP=line')
    print '  COMP: %s' % comp

  if nhits is None:
    raise RuntimeError('did not see NUMHITS=line')

  print '  NHIT: %s' % nhits

  if sortField == 'sort_field':
    sortField = 'int'
  if method in (None, 'int'):
    method = ' '
    
  open('results.txt', 'ab').write('%s\t%s\t%s\t%s\t%s\t%s\t%7d\t%6.1f\t%6.1f\n' % \
                                  (numSeg, indexLabel, sortField, query, numHits, method, nhits, warmTime, best))

  print '  %.1f sec' %  (time.time()-t0)
  all.append((numSeg, indexLabel, sortField, query, numHits, method, nhits, warmTime, best))
    
trunk = '-trunk' in sys.argv
all = []
for nSeg in (1, 10, 100):
  for sortField in 'score', 'doc', 'sort_field', 'country', 'title':
    if sortField in ('country', 'title'):
      if not trunk:
        methods = 'val', 'ord', 'ordval'
      else:
        methods = ('string',)
    elif sortField in ('score', 'doc'):
      methods = (None,)
    else:
      methods = ('int',)
    for query in ('147', 'text', '1', '*:*'):
      #for numHits in (10, 50, 100):
      for numHits in (10,):

        if sortField in ('country', 'sort_field') and query not in ('text', '*:*'):
          continue

        if sortField == 'score' and query == '*:*':
          continue

        for method in methods:

          print
          print 'RUN numSeg=%s sortField=%s method=%s query=%s numHits=%s' % \
                (nSeg, sortField, method, query, numHits)

          run(nSeg, sortField, method, query, numHits)

          import cPickle
          f = open('results.pk', 'wb')
          cPickle.dump(all, f)
          f.close()

