import sys
import cPickle

rowsTrunk = cPickle.load(open('/tango/mike/src/lucene.clean/contrib/benchmark/results.pk'))
d = {}
for pDel, query, nhits, warmTime, best in rowsTrunk:
  tup = (pDel, query)
  d[tup] = (nhits, warmTime, best)
  #print 'add %s' % str(tup)
  
rowsPatch = cPickle.load(open('/tango/mike/src/lucene.jasondeliter/contrib/benchmark/results.pk'))

human = '-jira' not in sys.argv

def header():
  if not human:
    print '||%tg deletes||query||hits||qps||qpsnew||pctg||'
  else:
    print '%tg del\tquery\thits\tqps\tqpsnew\tpctg'

def line(s):
  if human:
    print s
  else:
    print '|%s|' % (s.replace('\t', '|'))

header()

for pDel, query, nhits, warmTime, best in rowsPatch:

  tup = (pDel, query)

  if query == '*:*':
    query = '<all>'
    continue

  #print 'check tup %s' % str(tup)
  if tup in d:
    nhitsBase, warmTimeBase, bestBase = d[tup]
    if nhitsBase != nhits:
      raise RuntimeError('numhits differs %s vs %s' % (nhitsbase, nhits))
    pctChange = 100.0*(best-bestBase)/bestBase

    line('%d%%\t%s\t%7d\t%6.1f\t%6.1f\t%5.1f%%' % \
         (pDel, query, nhits, bestBase, best, pctChange))
  else:
    line('%d%%\t%s\t%7d\t \t%6.1f' % \
         (pDel, query, nhits, best))
