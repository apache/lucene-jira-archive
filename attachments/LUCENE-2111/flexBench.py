import sys
import os
import benchUtil

# TODO
#   - measure warm time imrovements (field cache / terms index)

JAVA_COMMAND = 'java -Xms2g -Xmx2g -server'

INDEX_NUM_DOCS = 5000000
INDEX_NUM_THREADS = 1
NUM_HITS = 10
NUM_ROUNDS = 6

TRUNK_DIR = '/root/src/clean/lucene/contrib/benchmark'
FLEX_DIR = '/root/src/flex.clean/contrib/benchmark'

if benchUtil.osName == 'osx':
  WIKI_FILE = '/x/lucene/enwiki-20090724-pages-articles.xml.bz2'
  WIKI_LINE_FILE = '/x/lucene/enwiki-20090306-lines-1k-fixed.txt'
  INDEX_DIR_BASE = '/lucene'
else:
  WIKI_FILE = '/x/lucene/enwiki-20090724-pages-articles.xml.bz2'
  WIKI_LINE_FILE = '/x/lucene/enwiki-20090306-lines-1k-fixed.txt'
  INDEX_DIR_BASE = '/x/lucene'

def usage():
  print
  print 'Usage: python -u %s -run <name> | -report <name>' % sys.argv[0]
  print
  print '  -run <name> runs all tests, saving results to file <name>.pk'
  print '  -report <name> opens <name>.pk and prints Jira table'
  print '  -verify confirm old & new produce identical results'
  print
  sys.exit(1)

def cmpPct(n1, n2):
  pct = 100.0*(n2-n1)/n1
  if pct < 0:
    return '%.1f%% worse' % pct
  else:
    return '%.1f%% better' % pct

def report(name):

  print '||Source||Query||Tot hits||Sort||Top N||Warm old||Warm new||Pct change||QPS old||QPS new||Pct change||'

  results = cPickle.load(open('%s.pk' % name))
  for warmOld, qpsOld, warmNew, qpsNew, params in results:
    
    params = list(params)
    sort = params[4]
    sort = sort.replace(':string', '')
    sort = sort.replace('doctitle', 'title')
    sort = sort.replace('sort_field:int', 'rand int')
    sort = sort.replace('random_string', 'rand string')
    params[4] = sort

    query = params[2]
    if query == '*:*':
      query = '<all>'
    params[2] = query
    
    print '|%s|%.2f|%.2f|%s|' % \
          ('|'.join(str(x) for x in params),
           warmOld, qpsOld, getPct(warmOld, qpsOld),
           qpsOld, qpsNew, getPct(qpsOld, qpsNew))

def run(mode, name):
    
  r = benchUtil.RunAlgs(INDEX_DIR_BASE, JAVA_COMMAND)
  r.printEnv()

  indexes = {}
  #for source in ('wiki', 'random'):
  for source in ('wiki',):
    for label, dir in (('flex', FLEX_DIR),
                       ('trunk', TRUNK_DIR)):
      indexes[(label, source)] = r.makeIndex(dir, label, source, INDEX_NUM_DOCS, INDEX_NUM_THREADS, lineDocSource=WIKI_LINE_FILE)

  doVerify = mode == 'verify'
  #sources = ('random', 'wiki')
  sources = ('wiki',)

  for source in sources:
    if source == 'random':
      queries = ('legal',)
    else:
      #queries = ('1', '1 OR 2', '1 AND 2', '"united states"', 'uni*')
      queries = ('un*t',)
      #queries = ('1',)

    for query in queries:
      if source == 'random':
        sorts = (
          'country:string',
          'random_string:string',
          'sort_field:int',
          )
      else:
        #sorts = (None, 'doctitle:string')
        sorts = (None,)

      for sort in sorts:

        print '\nRUN: source=%s query=%s sort=%s' % \
              (source, query, sort)

        # baseline -- use trunk
        indexPath = indexes[('trunk', source)]

        if sort is None:
          search = 'Search'
          if source == 'random':
            printField = 'country'
          else:
            printField = 'doctitle'
        else:
          search = 'SearchWithSort(%s,noscore,nomaxscore)' % sort
          printField = sort[:sort.find(':')]

        alg = r.getSearchAlg(indexPath, search, NUM_HITS, NUM_ROUNDS, verify=doVerify)
        alg = alg.replace('$PRINT_HITS_FIELD$', printField)
        
        job = benchUtil.SearchJob('trunk', INDEX_NUM_DOCS, alg, query, NUM_ROUNDS)
        print '  run trunk...'
        trunk = r.runOne(TRUNK_DIR, job, verify=doVerify)
        print '    %.2f QPS' % trunk.bestQPS

        print '  run flex on trunk index...'
        job = benchUtil.SearchJob('flexOnTrunk', INDEX_NUM_DOCS, alg, query, NUM_ROUNDS)
        flexOnTrunkIndex = r.runOne(FLEX_DIR, job, verify=doVerify)
        print '    %.2f QPS [%s]' % (flexOnTrunkIndex.bestQPS, cmpPct(trunk.bestQPS, flexOnTrunkIndex.bestQPS))

        print '  run flex on flex index...'
        indexPath = indexes[('flex', source)]
        alg = r.getSearchAlg(indexPath, search, NUM_HITS, NUM_ROUNDS, verify=doVerify)
        alg = alg.replace('$PRINT_HITS_FIELD$', printField)
        job = benchUtil.SearchJob('flexOnFlex', INDEX_NUM_DOCS, alg, query, NUM_ROUNDS)
        flexOnFlexIndex = r.runOne(FLEX_DIR, job, verify=doVerify)
        print '    %.2f QPS [%s]' % (flexOnFlexIndex.bestQPS, cmpPct(trunk.bestQPS, flexOnFlexIndex.bestQPS))
        print '  %d hits' % trunk.numHits
        
        benchUtil.verify(trunk, flexOnTrunkIndex)
        benchUtil.verify(trunk, flexOnFlexIndex)

        if mode == 'run':
          r.compare(trunk, (flexOnFlexIndex, flexOnTrunkIndex), source, query, sort)
          r.save(name)

def main():

  if '-run' in sys.argv:
    i = sys.argv.index('-run')
    mode = 'run'
    if i < len(sys.argv)-1:
      name = sys.argv[1+i]
    else:
      usage()
  elif '-report' in sys.argv:
    i = sys.argv.index('-report')
    mode = 'report'
    if i < len(sys.argv)-1:
      name = sys.argv[1+i]
    else:
      usage()
  elif '-verify' in sys.argv:
    mode = 'verify'
    name = None
  else:
    usage()

  if mode in ('run', 'verify'):
    run(mode, name)
  else:
    report(name)


if __name__ == '__main__':
  main()
