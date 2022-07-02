import re
import sys

f = open(sys.argv[1], 'rb')
l = []

r = re.compile(r'new ([a-zA-Z\.]+)Analyzer\(')

def process(fname, l):
  if len(l) > 0:
    adds = []
    dels = []
    for lx in l:
      m = r.search(lx)
      if m is not None:
        if lx[0] == '+':
          v = adds
        elif lx[0] == '-':
          v = dels
        else:
          continue
        v.append(m.group(1))
    #adds.sort()
    #dels.sort()
    if adds != dels:
      if len(dels) == 0 and adds == ['Whitespace'] * len(adds):
        # OK -- replaced null w/ WhitespaceAnalyzer
        pass
      else:
        print '%s\n  %s' % (fname, l[0])
        if len(adds) == len(dels):
          for i in xrange(len(adds)):
            print '    %s -> %s' % (dels[i], adds[i])
        else:
          print '  adds: %s' % adds
          print '  dels: %s' % dels
        print

  del l[:]

fname = None
while True:
  line = f.readline()
  if line == '':
    break
  if line.startswith('Index:'):
    process(fname, l)
    fname = line[6:].strip()
  if line.startswith('@@ '):
    process(fname, l)
  l.append(line.strip())
