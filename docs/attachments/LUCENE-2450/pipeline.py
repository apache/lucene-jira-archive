class Anything:
  pass

class Stage:
  
  def __init__(self, pipeline):
    self.readFrom, self.writeTo = pipeline.addStage(self)
    
  def getRead(self, var):
    return self.readFrom.lookup(var)

  def getWrite(self, var):
    assert var not in self.writeTo.bindings
    # TODO: we would make this a strongly typed attr...
    ret = self.writeTo.bindings[var] = Anything()
    return ret

class Pipeline:

  def __init__(self):
    self.curWrite = Bindings()
    self.curRead = Bindings()

  def addStage(self, stage):
    self.lastStage = stage
    ret = self.curWrite
    self.curWrite = Bindings(self.curWrite)
    return ret, self.curWrite

class Bindings:

  def __init__(self, before=None):
    self.before = before
    self.bindings = {}

  def lookup(self, name):
    b = self
    while b is not None:
      try:
        return b.bindings[name]
      except KeyError:
        b = self.before
    return None
  


class WhitespaceTokenizer(Stage):

  def __init__(self, pipeline):
    Stage.__init__(self, pipeline)
    self.termOut = self.getWrite('term')
    self.posIncrOut = self.getWrite('posIncr')

    # we never change this:
    self.posIncrOut.posIncr = 1

  def reset(self, s):
    self.tokens = s.split()
    self.upto = 0

  def next(self):
    if self.upto >= len(self.tokens):
      return False

    self.termOut.term = self.tokens[self.upto]
    self.upto += 1
    return True

class StopFilter(Stage):

  def __init__(self, pipeline):
    self.input = pipeline.lastStage
    Stage.__init__(self, pipeline)
    self.termIn = self.getRead('term')
    self.termOut = self.getWrite('term')    
    self.posIncrIn = self.getRead('posIncr')
    self.posIncrOut = self.getWrite('posIncr')

  def setStopWords(self, s):
    self.stopWords = s

  def next(self):

    accum = 0
    while True:
      
      if not self.input.next():
        return False

      accum += self.posIncrIn.posIncr

      if self.termIn.term not in self.stopWords:
        self.termOut.term = self.termIn.term
        self.posIncrOut.posIncr = accum
        return True

class SynonymsFilter(Stage):

  def __init__(self, pipeline):
    self.input = pipeline.lastStage
    Stage.__init__(self, pipeline)
    self.termIn = self.getRead('term')
    self.termOut = self.getWrite('term')    
    self.posIncrIn = self.getRead('posIncr')
    self.posIncrOut = self.getWrite('posIncr')
    self.pending = []

  def setSynonyms(self, syns):
    self.syns = syns

  def next(self):
    while len(self.pending) != 0:
      self.termOut.term = self.pending.pop()
      self.posIncrOut.posIncr = 0
      return True

    if not self.input.next():
      return False

    # lookup syns for this term
    if self.termIn.term in self.syns:
      self.pending = list(self.syns[self.termIn.term])
    else:
      self.pending = ()

    self.termOut.term = self.termIn.term
    self.posIncrOut.posIncr = self.posIncrIn.posIncr

    return True
  
class Indexer(Stage):

  def __init__(self, pipeline):
    self.input = pipeline.lastStage
    Stage.__init__(self, pipeline)
    self.termIn = self.getRead('term')
    self.posIncrIn = self.getRead('posIncr')

  def index(self):

    pos = -1
    while self.input.next():
      pos += self.posIncrIn.posIncr
      print 'TERM=%s pos=%s' % (self.termIn.term, pos)

    print
    print 'done!'
    
def main():
  p = Pipeline()

  tokenizer = WhitespaceTokenizer(p)
  tokenizer.reset('this is a test of the emergency broadcast system')
  
  p.addStage(tokenizer)
  filter = StopFilter(p)
  filter.setStopWords(set(('this', 'is', 'a', 'of', 'the')))
  p.addStage(filter)

  syns = SynonymsFilter(p)
  syns.setSynonyms({'emergency': ('911',),
                    'broadcast': ('tv', 'television')})

  indexer = Indexer(p)
  p.addStage(indexer)

  indexer.index()

if __name__ == '__main__':
  main()
