package com.carrotsearch.hppc.caliper;

import java.lang.reflect.Array;

import com.google.caliper.*;

/**
 * Benchmark generic vs. Object[]-and-cast array access speed.
 */
public class BenchmarkArrayAccess extends SimpleBenchmark
{
    public static class A<T>
    {
        public T [] array;

        @SuppressWarnings("unchecked")
        public A(Class<T> clazz, int size)
        {
            array = (T []) Array.newInstance(clazz, size);
        }

        public T get(int pos)
        {
            return array[pos];
        }
    }

    public static class C extends A<Integer>
    {
        public C(int size)
        {
            super(Integer.class, size);
        }
    }

    public static class B<T>
    {
        public Object [] array;

        public B(int size)
        {
            array = new Object [size];
        }

        @SuppressWarnings("unchecked")
        public T get(int pos)
        {
            return (T) array[pos];
        }
    }

    @Param(
    {
        "1000000"
    })
    public int size;

    private A<Integer> instanceA;
    private B<Integer> instanceB;
    private C instanceC;

    /** */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.instanceA = new A<Integer>(Integer.class, size);
        this.instanceB = new B<Integer>(size);
        this.instanceC = new C(size);

        for (int i = 0; i < size; i++)
        {
            instanceA.array[i] = i;
            instanceB.array[i] = i;
            instanceC.array[i] = i;
        }
    }

    /** */
    public int timeGeneric(int reps)
    {
        final A<Integer> instance = instanceA;
        int count = 0;
        for (int i = 0; i < reps; i++)
        {
            for (int j = 0; j < size; j++)
                count += instance.get(j).intValue();
        }
        return count;
    }

    /** */
    public int timeGenericSubclass(int reps)
    {
        final C instance = instanceC;
        int count = 0;
        for (int i = 0; i < reps; i++)
        {
            for (int j = 0; j < size; j++)
                count += instance.get(j).intValue();
        }
        return count;
    }

    /** */
    public int timeObject(int reps)
    {
        final B<Integer> instance = instanceB;
        int count = 0;
        for (int i = 0; i < reps; i++)
        {
            for (int j = 0; j < size; j++)
                count += instance.get(j).intValue();
        }
        return count;
    }

    /** */
    public static void main(String [] args)
    {
        Runner.main(BenchmarkArrayAccess.class, args);
    }
}