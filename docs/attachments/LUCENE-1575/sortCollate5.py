import sys
import cPickle

rowsTrunk = cPickle.load(open('/lucene/src/lucene.clean/contrib/benchmark/results.pk'))
d = {}
for query, sortBy, nhits, warmTime, best in rowsTrunk:
  tup = (query, sortBy)
  d[tup] = (nhits, warmTime, best)

patchDir = 'lucene.deliterator'
rowsPatch = cPickle.load(open('/lucene/src/lucene.collection/contrib/benchmark/results.pk'))

human = '-jira' not in sys.argv

def header():
  if not human:
    print '||query||sort||hits||qps||qpsnew||pctg||'
  else:
    print 'query\tsort\thits\tqps\tqpsnew\tpctg'

def line(s):
  if human:
    print s
  else:
    print '|%s|' % (s.replace('\t', '|'))

header()

for query, sortBy, nhits, warmTime, best in rowsPatch:

  tup = (query, sortBy)

  if tup in d:
    nhitsBase, warmTimeBase, bestBase = d[tup]
    if nhitsBase != nhits:
      raise RuntimeError('numhits differs %s vs %s' % (nhitsBase, nhits))
    pctChange = 100.0*(best-bestBase)/bestBase

    line('%s\t%s\t%7d\t%6.1f\t%6.1f\t%5.1f%%' % \
         (query, sortBy, nhits, bestBase, best, pctChange))
  else:
    line('%s\t%s\t%7d\t \t%6.1f' % \
         (query, sortBy, nhits, best))
