import os
import urllib.request
import subprocess
import re

# TODO
#  - we could also assert both cfs and nocfs were tested?

reVersion1 = re.compile(r'\>(\d+)\.(\d+)\.(\d+)(-alpha|-beta)?/\<', re.IGNORECASE)
reVersion2 = re.compile(r'-(\d+)\.(\d+)\.(\d+)(-alpha|-beta)?\.', re.IGNORECASE)

def getAllLuceneReleases():
  s = urllib.request.urlopen('https://archive.apache.org/dist/lucene/java').read().decode('UTF-8')

  releases = set()
  for r in reVersion1, reVersion2:
    for tup in r.findall(s):
      if tup[-1].lower() == '-alpha':
        tup = tup[:3] + ('0',)
      elif tup[-1].lower() == '-beta':
        tup = tup[:3] + ('1',)
      elif tup[-1] == '':
        tup = tup[:3]
      else:
        raise RuntimeError('failed to parse version: %s' % tup[-1])
      releases.add(tuple(int(x) for x in tup))

  l = list(releases)
  l.sort()
  return l

print('Find all past Lucene releases')

allReleases = getAllLuceneReleases()
for tup in allReleases:
  print('  %s' % '.'.join(str(x) for x in tup))

testedIndices = set()

for suffix in '', '3x':
  print('Run TestBackwardsCompatibility%s' % suffix)

  command = 'ant test -Dtestcase=TestBackwardsCompatibility%s -Dtests.verbose=true' % suffix
  p = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
  stdout, stderr = p.communicate()
  if p.returncode is not 0:
    # Not good: the test failed!
    raise RuntimeError('%s failed:\n%s' % (command, stdout))
  stdout = stdout.decode('utf-8')

  if stderr is not None:
    # Should not happen since we redirected stderr to stdout:
    raise RuntimeError('stderr non-empty')

  reIndexName = re.compile('TEST: index[= ](.*?)$', re.MULTILINE)

  for s in reIndexName.findall(stdout):
    # Fragile: decode the inconsistent naming schemes we've used in TestBWC's indices:
    name = os.path.splitext(s)[0]
    if name == '410':
      tup = 4, 10, 0
    elif len(name) == 2:
      tup = int(name[0]), int(name[1]), 0
    elif len(name) == 3:
      tup = int(name[0]), int(name[1]), int(name[2])
    else:
      raise RuntimeError('do not know how to parse index name %s' % name)
    testedIndices.add(tup)

l = list(testedIndices)
l.sort()
for release in l:
  print('  %s' % '.'.join(str(x) for x in release))

allReleases = set(allReleases)

for x in testedIndices:
  if x not in allReleases:
    # Curious:
    if x != (1, 9, 0):
      raise RuntimeError('tested version=%s but it was not released?' % '.'.join(str(y) for y in x))

notTested = []
for x in allReleases:
  if x not in testedIndices:
    notTested.append(x)

if len(notTested) > 0:
  notTested.sort()
  print('Releases that don\'t seem to be tested:')
  failed = True
  for x in notTested:
    print('  %s' % '.'.join(str(y) for y in x))
  raise RuntimeError('some releases are not tested by TestBackwardsCompatibility/3x?')
    
