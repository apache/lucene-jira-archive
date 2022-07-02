/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2007, Dawid Weiss, Stanisław Osiński.
 * Portions (C) Contributors listed in "carrot2.CONTRIBUTORS" file.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */

package org.apache.lucene.analysis.perf;

import java.io.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.fast.FastAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class AnalyzerBenchmark
{
    private int MIN_DOCUMENT_SIZE = 100;

    private Analyzer [] analyzers;
    private File textDir;

    private char [][] texts;

    public AnalyzerBenchmark(Analyzer [] analyzers, File textDir)
    {
        super();
        this.analyzers = analyzers;
        this.textDir = textDir;
    }

    public void go() throws IOException
    {
        System.out.println("Loading texts...");
        loadTexts();
        System.out.println("Warming up...");
        warmUp();
        System.out.println("Benchmarking...");
        benchmark();
    }

    private void benchmark() throws IOException
    {
        for (int i = 0; i < analyzers.length; i++)
        {
            long tokensMatched = 0;
            long start = System.currentTimeMillis();
            for (int j = 0; j < texts.length; j++)
            {
                if (texts[j].length > MIN_DOCUMENT_SIZE)
                {
                    tokensMatched += benchmarkAnalyzer(analyzers[i], texts[j]);
                }
            }
            long stop = System.currentTimeMillis();

            System.out.println(analyzers[i].getClass().getName() + ": " + (stop - start)
                + " ms, " + (int) (tokensMatched / ((stop - start) / 1000.0))
                + " tokens/s");
        }
    }

    private void warmUp() throws IOException
    {
        for (int i = 0; i < analyzers.length; i++)
        {
            for (int j = 0; j < texts.length; j++)
            {
                if (texts[j].length > MIN_DOCUMENT_SIZE)
                {
                    benchmarkAnalyzer(analyzers[i], texts[j]);
                }
            }
        }
    }

    private int benchmarkAnalyzer(Analyzer analyzer, char [] cs) throws IOException
    {
        int tokensMatched = 0;
        TokenStream tokenStream = analyzer.tokenStream(null, new CharArrayReader(cs));
        Token token;
        while ((token = tokenStream.next()) != null)
        {
            // Do something to the token to increase the chances it's not hoisted
            token.setEndOffset(0);
            tokensMatched++;
        }

        return tokensMatched;
    }

    private void loadTexts() throws IOException
    {
        if (!textDir.isDirectory())
        {
            throw new RuntimeException("The provided path must be a directory");
        }

        File [] files = textDir.listFiles();
        texts = new char [files.length] [];

        CharArrayWriter buffer = new CharArrayWriter();
        for (int i = 0; i < files.length; i++)
        {
            loadFile(files[i], buffer);
            texts[i] = buffer.toCharArray();
            buffer.reset();
        }
    }

    private char [] loadFile(File file, CharArrayWriter buffer) throws IOException
    {
        char [] cbuf = new char [512];
        int charsRead;

        FileReader fileReader = new FileReader(file);
        while ((charsRead = fileReader.read(cbuf)) > 0)
        {
            buffer.write(cbuf, 0, charsRead);
        }

        fileReader.close();

        return null;
    }

    public static void main(String [] args) throws IOException
    {
        final String textDirPath = "E:\\projects\\lucene\\trunk\\contrib\\benchmark\\work\\reuters-out";

        new AnalyzerBenchmark(new Analyzer []
        {
            new StandardAnalyzer(), new FastAnalyzer(), new WhitespaceAnalyzer()
        }, new File(textDirPath)).go();

    }
}
