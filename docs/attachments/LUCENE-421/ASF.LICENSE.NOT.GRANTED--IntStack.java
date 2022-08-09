/**
 * 
 */
package org.apache.lucene.util;

import java.util.EmptyStackException;

/**
 * @author randy
 *
 */

public class IntStack
{
    private int[] buffer;

    private int increment;

    private int size;

    private int top;

    IntStack(int initialSize, int increment)
    {
        this.increment = increment;
        this.size = initialSize;
        this.buffer = new int[initialSize];
        this.top = -1;
    }

    private void resize()
    {
        int[] tmp = new int[size + increment];
        System.arraycopy(buffer, 0, tmp, 0, buffer.length);
        buffer = tmp;
        size += increment;
        increment *= 2;
    }

    void push(int val)
    {
        top++;
        if (top == size) resize();
        buffer[top] = val;

    }

    int pop() throws EmptyStackException
    {
        if (top == -1) throw new EmptyStackException();
        int ret = buffer[top];
        top--;
        return ret;
    }

    boolean empty()
    {
        return (top == -1);
    }

}
