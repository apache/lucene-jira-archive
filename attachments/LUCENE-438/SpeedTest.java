
interface Call {
  public int call(int arg);
}

public class SpeedTest {

  public static void main(String[] args) throws Exception {
    int iter=Integer.parseInt(args[0]);
    String cname = "time";
    if (args.length > 1) { cname=args[1]; }
    Call obj = (Call)(Class.forName(cname).newInstance());

    long start = System.currentTimeMillis();

    int val=2;
    for (int i=1000000; i<1000000+iter; i++) {
      val += obj.call(val);
    }

    System.out.println("time=" + (System.currentTimeMillis()-start));
    System.out.println(val);

  }

}
