#!/usr/bin/perl
use strict;
use warnings;

use File::Spec::Functions qw( catfile catdir );
use Cwd qw( getcwd );

# ensure call from correct location and with required arg
my $source_dir = $ARGV[0];
die "Usage: ./extract_reuters.plx /path/to/expanded/archive"
    unless -d $source_dir;
my $working_dir = getcwd;
die "Must be run from the benchmarks/ directory"
    unless ( $working_dir =~ /benchmarks\W*$/ );

# create the main output directory
my $main_out_dir = 'extracted_corpus';
if ( !-d $main_out_dir ) {
    mkdir $main_out_dir or die "Couldn't mkdir '$main_out_dir': $!";
}

# get a list of the sgm files
opendir SOURCE_DIR, $source_dir or die "Couldn't open directory: $!";
my @sgm_files = grep {/\.sgm$/} readdir SOURCE_DIR;
closedir SOURCE_DIR or die "Couldn't close directory: $!";
die "Couldn't find all the sgm files"
    unless @sgm_files == 22;

# track number of story docs
my $num_files = 0;

for my $sgm_file (@sgm_files) {
    # get the sgm file
    my $sgm_filepath = catfile( $source_dir, $sgm_file );
    print "Processing $sgm_filepath\n";
    open( my $sgm_fh, '<', $sgm_filepath )
        or die "Couldn't open file '$sgm_filepath': $!";

    # prepare output directory
    $sgm_file =~ /(\d+)\.sgm$/ or die "no match";
    my $out_dir = catdir( $main_out_dir, "articles$1" );
    if ( !-d $out_dir ) {
        mkdir $out_dir or die "Couldn't create directory '$out_dir': $!";
    }

    my $in_body  = 0;
    my $in_title = 0;
    my ( $title, $body );
    while (<$sgm_fh>) {
        # start a new story doc
        if (/<REUTERS/) {
            $title = '';
            $body  = '';
        }

        # extract title and body
        if (s/.*?<TITLE>//) {
            $in_title = 1;
            $title    = '';
        }
        $title .= $_ if $in_title;
        if (s/.*?<BODY>//) {
            $in_body = 1;
            $body    = '';
        }
        $body .= $_ if $in_body;
        if (m#</TITLE>.*#) {
            $in_title = 0;
            $title =~ s#</TITLE>.*##s;
        }
        if (m#</BODY>.*#) {
            $in_body = 0;
            $body =~ s#</BODY>.*##s;
        }

        # write out a finished article doc
        if (m#</REUTERS>#) {
            die "Malformed data" if ( $in_title or $in_body );
            if ( length $title and length $body ) {
                my $out_filename = sprintf( "article%05d.txt", $num_files );
                my $out_filepath = catfile( $out_dir, $out_filename );
                open( my $out_fh, '>', $out_filepath )
                    or die "Couldn't open '$out_filepath' for writing: $!";
                $title =~ s/^\s*//;
                $title =~ s/\s*$//;
                print $out_fh "$title\n\n" or die "print failed: $!";
                print $out_fh $body        or die "print failed: $!";
                close $out_fh or die "Couldn't close '$out_filepath': $!";
                $num_files++;
            }
        }
    }
}

print "Total articles extracted: $num_files\n";

__END__

=head1 NAME

extract_reuters.plx - parse Reuters 21578 corpus into individual files

=head1 SYNOPSIS

    ./extract_reuters.plx /path/to/expanded/reuters/archive

=head1 DESCRIPTION

This script will extract TITLE and BODY for each item in the Reuters 21578
corpus into individual files.  It expects to be passed the location of the
decompressed archive as a command line argument.

=head1 AUTHOR

Marvin Humphrey E<lt> marvin at rectangular dot com E<gt>.

=head1 COPYRIGHT AND LICENSE

Copyright 2006 Marvin Humphrey

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut

