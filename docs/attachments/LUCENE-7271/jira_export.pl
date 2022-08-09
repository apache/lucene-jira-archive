#!/usr/bin/perl -l

use strict;
use warnings;
use LWP::Simple;
use URI;
$| = 1;

my $total_rows_expected = 4828;

# defaults to 100, but seems to allow up to 200 ?
my $rows = 200;
my $uri = URI->new('https://issues.apache.org/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml');

my $params = {
    'tempMax' => $rows,
    'jqlQuery' => 'project in (LUCENE, SOLR) AND fixVersion = master ORDER BY key DESC',
};

for (my $start = 0; $start < $total_rows_expected; $start += $rows) {
    $params->{'pager/start'} = $start;
    $uri->query_form($params);
    print STDERR $uri;

    my $status = getstore($uri, "LUCENE-7271_S2_master_report_$start.xml");
    die "$uri => $status" unless 200 == $status;

    sleep 1;
} 
