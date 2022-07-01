import cPickle

rowsTrunk = cPickle.load(open('/tango/mike/src/lucene.clean/contrib/benchmark/results.pk'))
d = {}
for numSeg, indexLabel, sortField, query, numHits, method, nhits, warmTime, best in rowsTrunk:
  tup = (numSeg, indexLabel, sortField, query, method)
  d[tup] = (nhits, warmTime, best)
  # print 'add %s' % str(tup)
  
rowsPatch = cPickle.load(open('/tango/mike/src/lucene.multipre/contrib/benchmark/results.pk'))

print 'numSeg\tindex\tsortBy\tquery\ttopN\tmeth\thits\twarm\tqps\twarmnew\tqpsnew\t  pctg'

for numSeg, indexLabel, sortField, query, numHits, method, nhits, warmTime, best in rowsPatch:

  if method == 'val':
    continue
  
  if method in ('ord', 'val', 'ordval'):
    meth2 = 'string'
  else:
    meth2 = method

  tup = (numSeg, indexLabel, sortField, query, meth2)

  # print 'check tup %s' % str(tup)
  if tup in d:
    nhitsBase, warmTimeBase, bestBase = d[tup]
    if nhitsBase != nhits:
      raise RuntimeError('numhits differs %s vs %s' % (nhitsbase, nhits))
    pctChange = 100.0*(best-bestBase)/bestBase
    if False and method in ('ord', 'ordval'):
      print ('%s\t%s\t%s\t%s\t%s\t%s\t%7d\t%6.1f\t \t \t%6.1f\t%5.1f%%' % \
             (numSeg, indexLabel, sortField, query, numHits, method, nhits, warmTime, best, pctChange))
    else:
      print ('%s\t%s\t%s\t%s\t%s\t%s\t%7d\t%6.1f\t%6.1f\t%6.1f\t%6.1f\t%5.1f%%' % \
             (numSeg, indexLabel, sortField, query, numHits, method, nhits, warmTimeBase, bestBase, warmTime, best, pctChange))
  else:
    print ('%s\t%s\t%s\t%s\t%s\t%s\t%7d\t \t \t%6.1f\t%6.1f' % \
           (numSeg, indexLabel, sortField, query, numHits, method, nhits, warmTime, best))
