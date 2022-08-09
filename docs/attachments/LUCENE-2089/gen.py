import os
import sys
try:
  from possibleStates import genTransitions
except ImportError:
  from finenight.possibleStates import genTransitions  

class LineOutput:

  def __init__(self, indent=''):
    self.l = []
    self._indent = self.startIndent = indent

  def __call__(self, s):
    if s.find('}') != -1:
      assert self._indent != self.startIndent
      self._indent = self._indent[:-2]
    self.l.append(self._indent + s.lstrip())
    if s.find('{') != -1:
      self._indent += '  '

  def __str__(self):
    assert self._indent == self.startIndent, 'indent %d vs start indent %d' % \
           (len(self._indent), len(self.startIndent))
    return '\n'.join(self.l)

  def indent(self):
    self._indent += '  '

  def outdent(self):
    assert self._indent != self.startIndent
    self._indent = self._indent[:-2]
    
def charVarNumber(charVar):
  """
  Maps binary number (eg [1, 0, 1]) to its decimal value (5).
  """

  p = 1
  sum = 0
  downTo = len(charVar)-1
  while downTo >= 0:
    sum += p * int(charVar[downTo])
    p *= 2
    downTo -= 1
  return sum

def main():

  n = int(sys.argv[1])

  tables = genTransitions(n)

  stateMap = {}

  # init null state
  stateMap['[]'] = -1

  # init start state
  stateMap['[(0, 0)]'] = 0

  w = LineOutput()

  w('package org.apache.lucene.util.automaton;')
  w('')
  w('// The following code was generated with the moman/finenight pkg')
  w('// This package is available under the MIT License, see NOTICE')
  w('// for more details.')
  w('')
  w('import org.apache.lucene.util.automaton.LevenshteinAutomata.ParametricDescription;')
  w('')

  className = 'Lev%dParametricDescription' % n

  w('class %s extends ParametricDescription {' % className)

  w('')
  w('@Override')
  w('int transition(int absState, int position, int vector) {')

  w('')
  w('  // decode absState -> state, offset')
  w('  int state = absToState[absState];')
  w('  int offset = absState - stateToAbs[state];')
  w('  assert offset >= 0;')
  w('')  
  w('  // null state should never be passed in')
  w('  assert state != -1;')

  for i, map in enumerate(tables):
    if i == 0:
      w('if (position == w) {')
    elif i == len(tables)-1:
      w('} else {')
    else:
      w('} else if (position == w-%d) {' % i)

    if i != 0:
      w('switch(vector) {')

    l = map.items()
    l.sort()

    for charVar, states in l:

      # somehow it's a string:
      charVar = eval(charVar)

      if i != 0:
        w('case %s: // <%s>' % (charVarNumber(charVar), ','.join([str(x) for x in charVar])))
        w.indent()
        
      l = states.items()

      # first pass to assign states
      byAction = {}
      for s, (toS, offset) in l:
        state = str(s)
        if state == '[]':
          # don't waste code on the null state
          continue
        
        toState = str(toS)
        if state not in stateMap:
          stateMap[state] = len(stateMap)-1
        if toState not in stateMap:
          stateMap[toState] = len(stateMap)-1

        fromStateDesc = ', '.join([str(x) for x in eval(s)])
        toStateDesc = ', '.join([str(x) for x in toS])   

        tup = (stateMap[toState], toStateDesc, offset)
        if tup not in byAction:
          byAction[tup] = []
        byAction[tup].append((fromStateDesc, stateMap[state]))

      # render switches
      w('switch(state) {')

      for (toState, toStateDesc, offset), lx in byAction.items():
        for fromStateDesc, fromState in lx:
          w('case %s: // %s' % (fromState, fromStateDesc))
        w.indent()
        w('  state = %s; // %s' % (toState, toStateDesc))
        if offset > 0:
          w('  offset += %s;' % offset)
        w('break;')
        w.outdent()

      if 0:
        for s, (toS, offset) in l:
          state = str(s)
          if state == '[]':
            # don't waste code on the null state
            continue

          toState = str(toS)
          if state not in stateMap:
            stateMap[state] = len(stateMap)-1
          if toState not in stateMap:
            stateMap[toState] = len(stateMap)-1


          w('case %s: // %s' % (stateMap[state], ', '.join([str(x) for x in eval(s)])))
          w.indent()
          w('  state = %s; // %s' % (stateMap[toState], ', '.join([str(x) for x in toS])))
          if offset > 0:
            w('  offset += %s;' % offset)
          w('break;')
          w.outdent()
          
      w('}')
      if i != 0:
        w('break;')
        w.outdent()
      
    if i != 0:
      w('}')
  w('}')

  stateMap2 = dict([[v,k] for k,v in stateMap.items()])
  w('')

  w('  if (state == -1) {')
  w('    // null state')
  w('    return -1;')
  w('  } else {')
  w('    // translate back to abs')
  w('    return stateToAbs[state] + offset;')
  w('  }')
  w('}')

  w('')
  w('// state map')
  sum = 0
  stateSizes = []
  minErrors = []
  for i in xrange(len(stateMap2)-1):
    w('//   %s -> %s' % (i, stateMap2[i]))
    v = eval(stateMap2[i])
    minError = min([-i+e for i, e in v])
    c = len(v)
    sum += c
    stateSizes.append(c)
    minErrors.append(minError)
  w('')

  w.indent()
  w('private final static int[] stateSizes = new int[] {%s};' % ','.join([str(x) for x in stateSizes]))
  w('private final static int[] minErrors = new int[] {%s};' % ','.join([str(x) for x in minErrors]))

  w.outdent()
  w('private final int[] stateToAbs;')
  w('private final int[] absToState;')

  w('')
  w('  public %s(int w) {' % className)
  w('    super(w);')
  w('    stateToAbs = new int[%d];' % len(stateSizes))
  w('    absToState = new int[(w+1)*%d];' % sum)
  w('    int upto = 0;')
  w('    for(int i=0;i<stateSizes.length;i++) {')
  w('      stateToAbs[i] = upto;')
  w('      for(int j=0;j<((w+1)*stateSizes[i]);j++) {')
  w('        absToState[upto++] = i;')
  w('      }')
  w('    }')
  w('  }')

  w('')
  w('@Override')
  w('public int size() {')
  w('  return absToState.length;')
  w('}')
  
  w('')
  w('@Override')
  w('public int getPosition(int absState) {')
  w('  int state = absToState[absState];')
  w('  int offset = absState - stateToAbs[state];')
  w('  return offset;')
  w('}')

  w('')
  w('@Override')
  w('public boolean isAccept(int absState) {')
  w('  // decode absState -> state, offset')
  w('  int state = absToState[absState];')
  w('  int offset = absState - stateToAbs[state];')
  w('  assert offset >= 0;')
  w('  return w - offset + minErrors[state] <= %d;' % n)
  w('}')
  
  # class
  w('}')
  w('')

  fileOut = 'src/java/org/apache/lucene/util/automaton/%s.java' % className

  open(fileOut, 'wb').write(str(w))

  print 'Wrote %s [%d lines; %.1f KB]' % \
        (fileOut, len(w.l), os.path.getsize(fileOut)/1024.)

if __name__ == '__main__':
  if not __debug__:
    print
    print 'ERROR: please run without -O'
    print
    sys.exit(1)
  main()
  
