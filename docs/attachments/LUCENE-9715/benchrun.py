import base64
import json
import os
import random
import re
import shutil
import time
import urllib.request
from datetime import datetime

import benchUtil, competition, constants

NIGHTLY_DIR = '%s/%s' % (constants.BASE_DIR, 'trunk')

reBytesIndexed = re.compile('^Indexer: net bytes indexed (.*)$', re.MULTILINE)
reIndexingTime = re.compile(r'^Indexer: finished \((.*) msec\)', re.MULTILINE)
reSVNRev = re.compile(r'revision (.*?)\.')
reIndexAtClose = re.compile('Indexer: at close: (.*?)$', re.M)


def buildIndex(r, runLogDir, desc, index, logFile):
    message('build %s' % desc)
    # t0 = now()
    indexPath = benchUtil.nameToIndexPath(index.getName())
    if os.path.exists(indexPath):
        shutil.rmtree(indexPath)
    indexPath, fullLogFile = r.makeIndex('trunk', index)
    # indexTime = (now()-t0)

    print(('Move log to %s/%s' % (runLogDir, logFile)))
    os.rename(fullLogFile, '%s/%s' % (runLogDir, logFile))

    s = open('%s/%s' % (runLogDir, logFile)).read()
    bytesIndexed = int(reBytesIndexed.search(s).group(1))
    m = reIndexAtClose.search(s)
    if m is not None:
        indexAtClose = m.group(1)
    else:
        # we have no index when we don't -waitForCommit
        indexAtClose = None
    indexTimeSec = int(reIndexingTime.search(s).group(1)) / 1000.0
    message('  took %.1f sec' % indexTimeSec)
    return indexPath, indexTimeSec, bytesIndexed, indexAtClose


NRT_DOCS_PER_SECOND = 1103  # = 1 MB / avg med wiki doc size
NRT_RUN_TIME = 30*60
NRT_SEARCH_THREADS = 4
NRT_INDEX_THREADS = 1
NRT_REOPENS_PER_SEC = 1
reNRTReopenTime = re.compile('^Reopen: +([0-9.]+) msec$', re.MULTILINE)


def runNRTTest(name, indexPath, runLogDir):
    print(os.getcwd())
    open('body10.tasks', 'w').write('Term: body:10\n')

    cmd = '%s -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -XX:StartFlightRecording=name=Default,filename=%s/%s_nrt.jfr,settings=profile -XX:+FlightRecorder -classpath "%s"  perf.NRTPerfTest %s "%s" multi "%s" 17 %s %s %s %s %s update 5 no 0.0 body10.tasks' % \
          (constants.JAVA_COMMAND,
           constants.JFR_BASE_FILEDR,
           name,
           benchUtil.classPathToString(benchUtil.getClassPath('trunk')),
           DIR_IMPL,
           indexPath + '/index',
           constants.WIKI_MEDIUM_DOCS_LINE_FILE,
           NRT_DOCS_PER_SECOND,
           NRT_RUN_TIME,
           NRT_SEARCH_THREADS,
           NRT_INDEX_THREADS,
           NRT_REOPENS_PER_SEC)

    logFile = '%s/nrt.log' % runLogDir
    cmd += '> %s 2>&1' % logFile
    runCommand(cmd)

    times = []
    for s in reNRTReopenTime.findall(open(logFile, 'r', encoding='utf-8').read()):
        times.append(float(s))

    # Discard first 10 (JVM warmup)
    times = times[10:]

    # Discard worst 2%
    times.sort()
    numDrop = len(times) // 50
    if numDrop > 0:
        message('drop: %s' % ' '.join(['%.1f' % x for x in times[-numDrop:]]))
        times = times[:-numDrop]
    message('times: %s' % ' '.join(['%.1f' % x for x in times]))


DIR_IMPL = 'MMapDirectory'
INDEXING_RAM_BUFFER_MB = 512


def run_and_upload():
    os.chdir('%s/%s' % (constants.BASE_DIR, 'trunk'))
    runCommand('git checkout master')
    runCommand('git pull')
    runCommand('git reset --hard origin/master')
    luceneRev = os.popen('git rev-parse HEAD').read().strip()
    os.chdir(constants.BENCH_BASE_DIR)

    base_name = "lucene_bench_%s_%s" % (datetime.today().strftime('%Y-%m-%d'), luceneRev[:7])
    run_benchmarks(base_name)




def run_benchmarks(base_name):
    r = benchUtil.RunAlgs(constants.JAVA_COMMAND, True, True)
    comp = competition.Competition(taskRepeatCount=1000,
                                   taskCountPerCat=5,
                                   verifyCounts=False)  # only verify top hits, not counts
    mediumSource = competition.WIKI_MEDIUM_ALL
    # mediumSource = competition.WIKI_MEDIUM_2M
    # mediumSource = competition.WIKI_MEDIUM_500K
    fastIndexMedium = comp.newIndex('trunk', mediumSource,
                                    name="%s_medium" % base_name,
                                    analyzer='StandardAnalyzerNoStopWords',
                                    postingsFormat='Lucene84',
                                    numThreads=constants.INDEX_NUM_THREADS,
                                    directory=DIR_IMPL,
                                    idFieldPostingsFormat='Lucene84',
                                    ramBufferMB=INDEXING_RAM_BUFFER_MB,
                                    waitForMerges=False,
                                    waitForCommit=False,
                                    disableIOThrottle=True,
                                    grouping=False,
                                    verbose=False,
                                    mergePolicy='TieredMergePolicy',
                                    maxConcurrentMerges=3,
                                    useCMS=True,
                                    vectorFile=constants.GLOVE_VECTOR_DOCS_FILE,
                                    vectorDimension=100, )

    nrtIndexMedium = comp.newIndex('trunk', mediumSource,
                                   name="%s_nrt" % base_name,
                                   analyzer='StandardAnalyzerNoStopWords',
                                   postingsFormat='Lucene84',
                                   numThreads=constants.INDEX_NUM_THREADS,
                                   directory=DIR_IMPL,
                                   idFieldPostingsFormat='Lucene84',
                                   ramBufferMB=INDEXING_RAM_BUFFER_MB,
                                   waitForMerges=True,
                                   waitForCommit=True,
                                   disableIOThrottle=True,
                                   grouping=False,
                                   verbose=False,
                                   mergePolicy='TieredMergePolicy',
                                   maxConcurrentMerges=3,
                                   useCMS=True,
                                   vectorFile=constants.GLOVE_VECTOR_DOCS_FILE,
                                   vectorDimension=100, )

    # bigSource = WIKI_BIG_1M
    # fastIndexBig = comp.newIndex('trunk', bigSource,
    #                              analyzer='StandardAnalyzerNoStopWords',
    #                              postingsFormat='Lucene84',
    #                              numThreads=constants.INDEX_NUM_THREADS,
    #                              directory=DIR_IMPL,
    #                              idFieldPostingsFormat='Lucene84',
    #                              ramBufferMB=INDEXING_RAM_BUFFER_MB,
    #                              waitForMerges=False,
    #                              waitForCommit=False,
    #                              disableIOThrottle=True,
    #                              grouping=False,
    #                              verbose=False,
    #                              mergePolicy='TieredMergePolicy',
    #                              maxConcurrentMerges=3,
    #                              useCMS=True,
    #                              vectorFile=constants.GLOVE_VECTOR_DOCS_FILE,
    #                              vectorDimension=100,)

    # Must use only 1 thread so we get same index structure, always:
    index = comp.newIndex('trunk', mediumSource,
                          name="%s_medium_1thread" % base_name,
                          analyzer='StandardAnalyzerNoStopWords',
                          postingsFormat='Lucene84',
                          numThreads=1,
                          directory=DIR_IMPL,
                          idFieldPostingsFormat='Lucene84',
                          mergePolicy='LogDocMergePolicy',
                          facets=(('taxonomy:Date', 'Date'),
                                  ('taxonomy:Month', 'Month'),
                                  ('taxonomy:DayOfYear', 'DayOfYear'),
                                  ('sortedset:Month', 'Month'),
                                  ('sortedset:DayOfYear', 'DayOfYear')),
                          maxConcurrentMerges=3,
                          addDVFields=True,
                          vectorFile=constants.GLOVE_VECTOR_DOCS_FILE,
                          vectorDimension=100, )

    id = 'jfrtest'
    c = comp.competitor(id, 'trunk',
                        index=index,
                         vectorDict=constants.GLOVE_WORD_VECTORS_FILE,
                        directory=DIR_IMPL,
                        commitPoint='multi')

    r.compile(c)
    runLogDir = '%s/%s' % (constants.LOGS_DIR, id)
    os.makedirs(runLogDir, exist_ok=True)
    message('log dir %s' % runLogDir)
    # 1: test indexing speed: small (~ 1KB) sized docs, flush-by-ram
    medIndexPath, medIndexTime, medBytesIndexed, atClose = buildIndex(r, runLogDir, 'medium index (fast)',
                                                                     fastIndexMedium, 'fastIndexMediumDocs.log')
    message('medIndexAtClose %s' % atClose)

    # 2: NRT test
    nrtIndexPath, nrtIndexTime, nrtBytesIndexed, atClose = buildIndex(r, runLogDir, 'nrt medium index', nrtIndexMedium,
                                                                     'nrtIndexMediumDocs.log')
    message('nrtMedIndexAtClose %s' % atClose)
    runNRTTest(nrtIndexMedium.getName(), nrtIndexPath, runLogDir)

    # # 3: test indexing speed: medium (~ 4KB) sized docs, flush-by-ram
    # ign, bigIndexTime, bigBytesIndexed, atClose = buildIndex(r, runLogDir, 'big index (fast)', fastIndexBig, 'fastIndexBigDocs.log')
    # message('bigIndexAtClose %s' % atClose)

    # 4: test searching speed; first build index, flushed by doc count (so we get same index structure night to night)
    indexPathNow, ign, ign, atClose = buildIndex(r, runLogDir, 'search index (fixed segments)', index, 'fixedIndex.log')
    message('fixedIndexAtClose %s' % atClose)
    fixedIndexAtClose = atClose

    # Search
    rand = random.Random(714)
    staticSeed = rand.randint(-10000000, 1000000)
    # staticSeed = -1492352

    message('search')
    t0 = now()

    coldRun = False
    comp = c
    comp.tasksFile = '%s/tasks/wikinightly.tasks' % constants.BENCH_BASE_DIR
    comp.printHeap = True
    seed = rand.randint(-10000000, 1000000)
    r.runSimpleSearchBench(0, id, comp, coldRun, seed, staticSeed, filter=None)
    message('done search (%s)' % (now() - t0))


def now():
    return datetime.now()


def message(s):
    print('[%s] %s' % (now(), s))


def runCommand(command):
    message('RUN: %s' % command)
    t0 = time.time()
    out = os.system(command)
    if out:
        message('  FAILED')
        raise RuntimeError('command failed: %s' % command)
    message('  took %.1f sec' % (time.time() - t0))


run_and_upload()
