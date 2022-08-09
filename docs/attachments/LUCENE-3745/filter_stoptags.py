#!/usr/bin/env python
# -*- coding: utf-8 -*-
#

import optparse
import gzip
import re

def main():
    parser = optparse.OptionParser(usage='filter_stoptags.py --stoptags TSVFILE',
                                   version='filter_stoptags 0.1')
    parser.add_option('-s',
                      '--stoptagsfilte',
                      action='store',
                      type='string',
                      dest='stoptags_filename',
                      help='read stoptags from STOPTAGS')

    (options, args) = parser.parse_args()

    if not options.stoptags_filename:
        parser.error('a stoptags filename is required.')

    if len(args) != 1:
        parser.error('incorrect number of arguments')

    stoptags = read_stoptags(options.stoptags_filename)

    if args[0].endswith('.gz'):
        input = gzip.GzipFile(args[0], 'r')
    else:
        input = open(args[0], 'r')

    input = open(args[0], 'r')

    for line in input:
        try:
            line = line[:-1]
            (surface, pos, freq) = line.split('\t', 3)
            if pos in stoptags:
                print "stop: " + surface,
            else:
                print "pass: " + surface,
            print "\tfreq: " + freq + "\tpos: " + pos

        except Exception:
            print "error: " + line


def read_stoptags(filename):
    stoptags = []
    input = open(filename, 'r')
    for line in input:
        line = line[:-1]
        if not re.match("^[\s]*#.*$", line):
            stoptags.append(line)
    return stoptags

if __name__ == "__main__":
    main()
