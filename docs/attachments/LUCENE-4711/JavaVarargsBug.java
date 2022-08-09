
public class JavaVarargsBug {

	public static class Foo {

	  private static final Foo EMPTY = new Foo();

	  private final String[] components;
	  private final int length;

	  private Foo() {
	    components = new String[0];
	    length = 0;
	  }

	  public Foo(final String... components) {
//	    this.components = components;
	    this.components = new String[components.length];
	    System.arraycopy(components, 0, this.components, 0, components.length);
	    length = components.length;
	  }

	  public Foo(final String pathString, final char delimiter) {
	    String[] comps = pathString.split(Character.toString(delimiter));
	    if (comps.length == 1 && comps[0].isEmpty()) {
	      components = EMPTY.components;
	      length = 0;
	    } else {
	      components = comps;
	      length = components.length;
	    }
	  }

	  @Override
	  public String toString() {
	  	assert length <= components.length : "length=" + length + " components.length=" + components.length;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < length; i++) {
			  sb.append(components[i]).append('/');
			}
			return sb.toString();
	  }
	}
	
	public static void main(String[] args) {
		while (true) {
			new Foo("arg1").toString();
		}
	}

}
