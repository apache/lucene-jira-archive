/*
 * ConcurrencyTest.java
 *
 * Created on 29 September 2003, 13:03
 */

package net.motile.pittwater.index.test;

import java.io.*;
import java.util.*;

import net.motile.pittwater.index.*;

/**
 *
 * @author Esmond Pitt
 */
public class ConcurrencyTest implements Runnable
{
	Index	index;
	Random	random = new Random();
	
	/** Creates a new instance of ConcurrencyTest */
	public ConcurrencyTest(String directory)
		throws IOException
	{
		if (false)
		{
			try
			{
				this.index = Index.getIndex(new File(directory),false);
				index.optimize();
				index.close();
				System.out.print("optimized ... "); System.in.read();
				return;
			}
			catch (IOException exc)
			{
			}
		}
		this.index = Index.getIndex(new File(directory),true);
		System.out.print("created ... "); System.in.read();
	}
	
	public void run()
	{
		String	prefix = Thread.currentThread().getName();
		System.out.println(prefix+": running");
		try
		{
			System.out.println(prefix+": starting insertions");
			for (int i = 0 ; i < 100; i++)
			{
				Hashtable	h = new Hashtable();
				h.put("modified",new Date());
				h.put("Author","Esmond Pitt");
				h.put("URI","documents/tests/"+i);
				String	id = "286CRM/"+i;
				h.put("ID",id);
				index.addItem("<Company id="+i+">Melbourne Software Company<Director>Esmond Pitt</Director><Director>David Pitt</Director><Address>"+i+" Canterbury Rd</Address></Company>",h);
				Thread.currentThread().sleep(random.nextInt() & 0x3ff);
			}
			System.out.println(prefix+": starting deletions");
			for (int i = 0 ; i < 100; i++)
			{
				String	id = "286CRM/"+i;
				index.deleteItem("ID",id);
				Thread.currentThread().sleep(random.nextInt() & 0x3ff);
			}
			System.out.println(prefix+": starting optimize");
			index.optimize();
			System.out.println(prefix+": closing");
			index.close();
		}
		catch (IOException exc)
		{
			synchronized (this)
			{
				System.err.print(prefix+": ");
				exc.printStackTrace();
			}
		}
		catch (InterruptedException exc)
		{
		}
		catch (RuntimeException exc)
		{
			synchronized (this)
			{
				System.err.print(prefix+": ");
				exc.printStackTrace();
			}
		}
		finally
		{
			try
			{
				index.close();
			}
			catch (IOException exc)
			{
				exc.printStackTrace();
			}
		}
		System.out.println(prefix+": exiting");
	}
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
		throws IOException, InterruptedException
	{
		int	count = args.length > 0 ? Integer.parseInt(args[0]) : 10;
		Runnable	r = new ConcurrencyTest("concurrent-index");
		Thread[]	t = new Thread[count];
		for (int i = 0; i < count; i++)
			t[i] = new Thread(r,"Thread "+(i+1));
		for (int i = 0; i < count; i++)
			t[i].start();
		for (int i = 0; i < count; i++)
			t[i].join();
		System.out.println("ConcurrencyTest finished.");
	}
}
