/**
 * 
 */
package org.apache.lucene.util;

import java.util.Random;

import org.apache.lucene.util.IntStack;

/**
 * @author randy
 * 
 */
public final class Sort
{

    /**
     * @param docnos -
     *            an array of integers to be sorted by value
     */
    public static final void quickSort(int[] docnos)
    {
        quickSort(docnos, 0, docnos.length - 1);
    }

    /**
     * @param docnos -
     *            an array of integer values containing subarray to be sorted
     * @param first -
     *            initial index within array to be sorted
     * @param last -
     *            final index within array to be sorted
     */
    public static final void quickSort(int[] docnos, int first, int last)
    {
        IntStack s = new IntStack(1000, 10000);
        int[] splitpoints = new int[2];

        s.push(first);
        s.push(last);
        while (!s.empty())
        {
            last = s.pop();
            first = s.pop();
            for (;;)
            {
                if (last - first > 10)
                {
                    split(docnos, first, last, splitpoints);
                    // push the smaller list
                    if (splitpoints[0] - first < last - splitpoints[1])
                    {
                        s.push(first);
                        s.push(splitpoints[0]);
                        first = splitpoints[1];
                    }
                    else
                    {
                        s.push(splitpoints[1]);
                        s.push(last);
                        last = splitpoints[0];
                    }
                }
                else
                { // sort the smaller
                    // sub-lists
                    // through insertion sort
                    insertion_sort(docnos, first, last);
                    break;
                }
            }
        } // iterate for larger list
    }

    public static final void quickSort(int[] docnos, int[] docvals)
    {
        quickSort(docnos, 0, docnos.length - 1, docvals);
    }

    public static final void quickSort(int[] docnos, int first, int last,
            int[] docvals)
    {
        IntStack s = new IntStack(1000, 10000);
        int[] splitpoints = new int[2];

        s.push(first);
        s.push(last);
        while (!s.empty())
        {
            last = s.pop();
            first = s.pop();
            for (;;)
            {
                if (last - first > 10)
                {
                    split(docnos, first, last, docvals, splitpoints);
                    // push the smaller list
                    if (splitpoints[0] - first < last - splitpoints[1])
                    {
                        s.push(first);
                        s.push(splitpoints[0]);
                        first = splitpoints[1];
                    }
                    else
                    {
                        s.push(splitpoints[1]);
                        s.push(last);
                        last = splitpoints[0];
                    }
                }
                else
                { // sort the smaller
                    // sub-lists
                    // through insertion sort
                    insertion_sort(docnos, first, last, docvals);
                    break;
                }
            }
        } // iterate for larger list
    }

    /**
     * @param docnos -
     *            array of indices into the docvals array, on output sorted
     *            according to the values in docvals
     * @param docvals -
     *            values used in sorting comparison; the 'value' of an entry, i,
     *            is docvals[docnos[i]]
     */
    public static final void quickSort(int[] docnos, float[] docvals)
    {
        quickSort(docnos, 0, docnos.length - 1, docvals);
    }

    /**
     * @param docnos -
     *            array of indices into the docvals array, on output sorted
     *            according to the values in docvals
     * @param first -
     *            index of initial entry in docnos
     * @param last -
     *            index of final entry in docnos
     * @param docvals -
     *            values used in sorting comparison; the 'value' of an entry, i,
     *            is docvals[docnos[i]]
     */
    public static final void quickSort(int[] docnos, int first, int last,
            float[] docvals)
    {
        IntStack s = new IntStack(1000, 10000);
        int[] splitpoints = new int[2];

        s.push(first);
        s.push(last);
        while (!s.empty())
        {
            last = s.pop();
            first = s.pop();
            for (;;)
            {
                if (last - first > 10)
                {
                    split(docnos, first, last, docvals, splitpoints);
                    // push the smaller list
                    if (splitpoints[0] - first < last - splitpoints[1])
                    {
                        s.push(first);
                        s.push(splitpoints[0]);
                        first = splitpoints[1];
                    }
                    else
                    {
                        s.push(splitpoints[1]);
                        s.push(last);
                        last = splitpoints[0];
                    }
                }
                else
                { // sort the smaller
                    // sub-lists
                    // through insertion sort
                    insertion_sort(docnos, first, last, docvals);
                    break;
                }
            }
        } // iterate for larger list
    }

    static final void insertion_sort(int[] docnos, int first, int last)
    {
        for (int i = first; i <= last; i++)
        {
            int vidx = docnos[i];
            int j = i - 1;
            while ((j >= first) && (docnos[j] > vidx))
            {
                docnos[j + 1] = docnos[j];
                j--;
            }
            docnos[j + 1] = vidx;
        }
    }

    static final void insertion_sort(int[] docnos, int first, int last,
            int[] docvals)
    {
        for (int i = first; i <= last; i++)
        {
            int vidx = docnos[i];
            int j = i - 1;
            while ((j >= first) && (docvals[docnos[j]] > docvals[vidx]))
            {
                docnos[j + 1] = docnos[j];
                j--;
            }
            docnos[j + 1] = vidx;
        }
    }

    static final void insertion_sort(int[] docnos, int first, int last,
            float[] docvals)
    {
        for (int i = first; i <= last; i++)
        {
            int vidx = docnos[i];
            int j = i - 1;
            while ((j >= first) && (docvals[docnos[j]] > docvals[vidx]))
            {
                docnos[j + 1] = docnos[j];
                j--;
            }
            docnos[j + 1] = vidx;
        }
    }

    private static final Random RND = new Random();

    private static final void swap(int[] arr, int i, int j)
    {
        int tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    private static final void split(int[] docnos, int first, int last,
            int[] partitions)
    {
        int index = (first + last)/2;
        int pivot = docnos[index];
        swap(docnos, index, last);
        int lowIdx = first;
        for (int i = first; i <= last; ++i)
        {
            if (docnos[i] < pivot)
            {
                swap(docnos, lowIdx, i);
                ++lowIdx;
            }
        }

        swap(docnos, lowIdx, last);
        partitions[0] = lowIdx - 1;
        partitions[1] = lowIdx + 1;
    }

    private static final void split(int[] docnos, int first, int last,
            int[] docvals, int[] partitions)
    {
        int index = first + RND.nextInt(last - first + 1);
        int pivot = docvals[docnos[index]];
        // swap(docnos, index, last);
        int lowIdx = first;
        int numPivots = 0;
        for (int i = first; i <= last; ++i)
        {
            if (docvals[docnos[i]] < pivot)
            {
                swap(docnos, lowIdx, i);
                ++lowIdx;
            }
            else if (docvals[docnos[i]] > pivot)
            {
                swap(docnos, i, last);
                last--;
                i--;
            }
            else
            {
                numPivots++;
            }
        }

        // swap(docnos, lowIdx, last);
        partitions[0] = lowIdx - 1;
        partitions[1] = last + 1;
    }

    private static final void split(int[] docnos, int first, int last,
            float[] docvals, int[] partitions)
    {
        int index = first + RND.nextInt(last - first + 1);
        float pivot = docvals[docnos[index]];
        // swap(docnos, index, last);
        int lowIdx = first;
        int numPivots = 0;
        for (int i = first; i <= last; ++i)
        {
            if (docvals[docnos[i]] < pivot)
            {
                swap(docnos, lowIdx, i);
                ++lowIdx;
            }
            else if (docvals[docnos[i]] > pivot)
            {
                swap(docnos, i, last);
                last--;
                i--;
            }
            else
            {
                numPivots++;
            }
        }

        // swap(docnos, lowIdx, last);
        partitions[0] = lowIdx - 1;
        partitions[1] = last + 1;
    }
}
