#!/usr/bin/perl -i
#
#  https://issues.apache.org/jira/browse/LUCENE-3949
#
# find -name \*.java | xargs -n 1 fix-license-jdoc.pl 
#
#  NOTE: the "-n 1" is VERY IMPORTANT ! ! ! ! 
#
#####
#
# This is very hackish, and should not be extended fo any other additional 
# purposes.  Among other things, i think it will break if there are multiple 
# succesive lines that start a javadoc comment -- but since that would be 
# invalid java, i'm not going ot worry about it.

my $jdocstart;
while (my $line = <>) {
    if (defined $jdocstart) {
	if ($line =~ m{^\ \*\ Licensed\ to\ the\ Apache\ Software}) {
	    $jdocstart =~ s{/\*\*}{/\*};
	}
	print $jdocstart;
	undef $jdocstart;
    } 
    if ($line =~ m{^/\*\*\s*$}) {
	$jdocstart = $line;
    } else {
	print $line;
    }
}
