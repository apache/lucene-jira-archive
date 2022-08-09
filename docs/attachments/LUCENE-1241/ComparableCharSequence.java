import java.nio.CharBuffer;

class ComparableCharSequence implements CharSequence,Comparable {
	private CharSequence seq;
	private int currentIndex;
	
	ComparableCharSequence(CharSequence str){
		if(str==null) throw new IllegalArgumentException("CharSequence is null");
		this.seq=str;
	}
	
	ComparableCharSequence(char[] buf,int offset,int length){
		this.seq=CharBuffer.wrap(buf,offset,length);
	}
	
	// CharSequence
	public char charAt(int index){
		return seq.charAt(index);
	}
	public int length(){
		return seq.length();
	}
	public CharSequence subSequence(int start,int end){
		return seq.subSequence(start,end);
	}
	public String toString(){
		return seq.toString();
	}
	
	// Comparable
	public int compareTo(Object ob){
		ComparableCharSequence o=null;
		if(ob instanceof ComparableCharSequence){
			o=(ComparableCharSequence)ob;
		}else{
			// depends on whether we want null last or first.
			return -1;
		}
		int maxlen = seq.length()<o.length() ? seq.length() : o.length();
		for(int i=0; i<maxlen; i++){
			if(charAt(i) < o.charAt(i)) return -1;
			if(charAt(i) > o.charAt(i)) return 1;
		}
		if(seq.length()<o.length()) return -1;
		if(seq.length()>o.length()) return 1;
		return 0;
	}
	
	// hashCode()
	public int hashCode(){
		// this is the same with this.toString().hashCode();
		// to avoid memory allocation, we calculate here.
		int code=0;
		for(int i=seq.length();i>=0;i--){
			code=code*31+charAt(i);
		}
		return code;
	}
}
