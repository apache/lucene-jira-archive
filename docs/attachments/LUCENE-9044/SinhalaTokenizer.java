package sl;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.arraycopy;
import static org.apache.commons.lang3.StringUtils.join;


class SinhalaTokenizer {


    private final String[] letters = {"ඒ", "බී", "සී", "ඩී", "ඊ", "එෆ්", "ජී", "එච්", "අයි", "ජේ", "කේ", "එල්", "එම්", "එන්", "ඕ", "පී", "ආර්", "එස්", "ටී", "යූ", "වී", "ඩබ්ලිව්", "වයි"};
    private final String[] eng_letters = {"ඒ.", "බී.", "සී.", "ඩී.", "ඊ.", "එෆ්.", "ජී.", "එච්.", "අයි.", "ජේ.", "කේ.", "එල්.", "එම්.", "එන්.", "ඕ.", "පී.", "ආර්.", "එස්.", "ටී.", "යූ.", "වී.", "ඩබ්ලිව්.", "වයි."};

    private final String[] ignoringCharList = {",", "{", "}", "[", "]", "!", ";", "?", "?", "+", "@", "#", "$", "%", "'", "\'", "*", "&", "\"", "”", "“", "‘", "’", "●", "Ê",
            "\u00a0", "\u2003", // spaces
            "\ufffd", "\uf020", "\uf073", "\uf06c", "\uf190", // unknown or invalid unicode chars
            "\u202a", "\u202c", "\u200f", "\u200c", "\u0160", "\u00ad", "\u0088", "\uf086", "\u200b", "\ufeff"};
    private String[] lm = {"ශ්\u200Dරි ලංකා"};
    private String[] tokens;
    SinhalaTokenizer(String string){
        tokens=spliit_words(string);
    }

    CharArraySet getTokensCharSet() {
        return new CharArraySet(Version.LUCENE_42, Arrays.asList(tokens), false);
    }
    List<String> getTokens(){
        return Arrays.asList(tokens);
    }
    private String [] spliit_words(String str){
        if(str.contains("."))
            str = Handledot(str);
        if((str.contains("(")) || (str.contains(")")))
            str = HandleBrackets(str);
        if(str.contains(":"))
            str = HandleColon(str);
        if(str.contains("-"))
            str = Handlehypen(str);
        if(str.contains("/"))
             str = Handleslash(str);

       //remove unwanted characters - characters in ignoringCharList
        for(String ignoringChar : ignoringCharList) {
            if(str.contains(ignoringChar)) {
               str = str.replaceAll(Pattern.quote(ignoringChar), Matcher.quoteReplacement(" "));
            }
        }

        //remove unwanted numbers
        String[] list = str.split(" ");
        for (int i = 0; i < list.length; i++) {
            if(StringUtils.isNumeric(list[i]) ) {
                list[i] = list[i].replaceAll(Pattern.quote(list[i]), Matcher.quoteReplacement(" "));
            }
        }

        //language model results applied
        for (String s: lm) {
            if (str.contains(s))
            {
              for (int x=0;x<list.length-2;x++){
                  if((list[x]+" "+list[x+1]).equals(s)){
                      list[x]=s;
                      list[x+1]=list[x+1].replace(list[x+1],"");
                  }
              }
            }
        }
        return list;
    }

    //check for sinhala letters
    private boolean isASinhalaLetter(String s) {
        if(s.length() != 1) return true;
        int sinhalaLowerBound = 3456;
        int sinhalaUpperBound = 3583;

        int cp = s.codePointAt(0);
        final boolean b = cp >= sinhalaLowerBound && cp <= sinhalaUpperBound;
        return b;
    }

    //check for sinhala words
    private boolean containsSinhala(String s) {
        for(int i = 0; i < s.length(); ++i) {
            if(isASinhalaLetter(s.charAt(i) + "")) {
                return true;
            }
        }
        return false;
    }

    /* Check initials are available */
    private boolean is_available_in_english_letters(String letter){
        boolean availability = false;
        for (String eng_letter : eng_letters) {
            if (eng_letter.equals(letter)) {
                availability = true;
                break;
            }
        }
        return availability;
    }

    //remove element from word
    private char[] removeElt( char [] arr, int remIndex )
    {
        String s0 = new String(arr);
        StringBuilder sb = new StringBuilder(s0);
        String s= new String(sb.deleteCharAt(remIndex));
        return s.toCharArray();
    }

    //remove
    private char[] remove(char[] ch){
        char[] ch0 = new char[ch.length-1];
        arraycopy(ch, 0, ch0, 0, ch0.length);
        ch=ch0;
        return ch;
    }

    // Function to remove the element from list
    private static String[] removeTheElement(String[] arr, int index)
    {
        /* If the array is empty
           or the index is not in array range
           return the original array */
        if (arr == null
                || index < 0
                || index >= arr.length)
            return arr;
        // Create another array of size one less
        String [] anotherArray = new String[arr.length - 1];
        /* Copy the elements except the index
           from original array to the other array */
        for (int i = 0, k = 0; i < arr.length; i++) {
            // if the index is
            // the removal element index
            if (i == index)
                continue;
            // if the index is not
            // the removal element index
            anotherArray[k++] = arr[i];
        }
        // return the resultant array
        return anotherArray;
    }

    /* Tokenize names with initials accurately */
    private String name_ananlyzer(String str){

        String[] list = str.split(" ");

        StringBuilder temp_str= new StringBuilder();
        for(int i=0;i<list.length;i++){

            // එච්. එම්. ණසේකර
            if (is_available_in_english_letters(list[i])){
                temp_str.append(list[i]);
                list[i]=",";
            } else {
                if (!temp_str.toString().equals("")){
                    list[i]= temp_str+list[i] ;
                    temp_str = new StringBuilder();
                }
            }

            //එච්.එම්. ණසේකර
            if(list[i].endsWith(".") && Arrays.stream(eng_letters).parallel().anyMatch(list[i]::contains)){
                String t = list[i+1];
                list[i] = list[i]+t;
                list[i+1]=",";
            }
        }
        StringBuilder b = new StringBuilder();
        for (String s : list) {
            b.append(s);
            b.append(" ");
        }
        return join(list," ");
    }

    /* remove dot */
    private String Handledot(String str) {
        str = name_ananlyzer(str);

        String[] list = str.split(" ");

        outerloop:
        for (int i = 0; i < list.length; i++) {
            for(String letter : letters) {
                //check for initials
                if (list[i].contains(letter)) {
                    continue outerloop;
                }
            }

            char[] a = list[i].toCharArray();
            int count =0;
            for (char c : a) {
                //count number of dots
                if (c == '.') {
                    count++;
                }
            }

            if (count==1){
                for(int j=0; j<a.length; j++){
                    if(a[j]== '.'){
                        // a=removeElt(a,j);
                    }
                }
            }
            else if(count<2){
                continue ;
            }

            else{
                for(int j=0; j<a.length; j++){
                    if(a[j]== '.'){
                        a=removeElt(a,j);
                    }
                }
            }

            String str1 = new String(a);
            list[i]= str1;

            if(i+1<list.length) {
                while (list[i].endsWith(".") && list[i + 1].endsWith(".")) {
                    list[i] = list[i] + list[i + 1];
                    list = removeTheElement(list, i + 1);
                }
            }
            if(StringUtils.isNumeric(StringUtils.getDigits(list[i]))){
                if(list[i].endsWith("."))
                    list[i]=list[i].replace(".","");
                if(containsSinhala(list[i]))
                    list[i]=list[i].replace("."," ");
            }
            if(list[i].endsWith("."))
                list[i]=list[i].replace(".","");
        }

        return join(list," ");
    }

    //handle brackets ( & ) only
    private String HandleBrackets(String str){
        String[] list = str.split(" ");
        for (int i = 0; i < list.length; i++) {
            char[] a = list[i].toCharArray();
            if (list[i].contains("(") || list[i].contains(")")) {
                int a0 = list[i].indexOf('(');
                int a1 = list[i].indexOf(')');
                if (list[i].startsWith("(")) {
                    a = removeElt(a, 0);
                    String s = new String(a);
                    list[i]=s;
                }
                if (list[i].endsWith(")")){
                    a = removeElt(a, a.length - 1);
                    String s = new String(a);
                    list[i]=s;
                }
                else if ( 1 < (a1 - a0) && (a1 - a0)< 4) {
                    //Silent letter in sinhala
                    // do nothing
                 }
                else {
                    list[i]=list[i].replace('(',' ');
                    list[i]=list[i].replace(')',' ');
                }
            }
        }

        return join(list," ");
    }


    //handle colon :
    private String HandleColon(String str){
        String[] list = str.split(" ");
        for (int i = 0; i < list.length; i++) {
            char[] w = list[i].toCharArray();
            int count=0;
            if(list[i].contains(":")){
                for (char c : w) {
                    if (c == ':') {
                        count++;
                    }
                }
                if (count == 1 && w[w.length - 1] == ':') {
                    w=remove(w);
                    String s = new String(w);
                    list[i]= s;
                }
            }
        }
        return join(list," ");
    }

    //handle hyphen -
    private String Handlehypen(String str){
        String[] list = str.split(" ");
        outerloop:
        for (int i = 0; i < list.length; i++) {
            if (list[i].contains("-"))
            {
                if (StringUtils.isNumeric(StringUtils.getDigits(list[i])) && list[i].contains("-"))
                {
                    continue outerloop;
                }
                else
                {
                    list[i]=list[i].replace('-',' ');
                }
            }
        }

        return join(list," ");
    }

    //Handle slash /
    private String Handleslash(String str){
        String[] list = str.split(" ");
        outerloop:
        for (int i = 0; i < list.length; i++) {
            if (list[i].contains("/")) {
                if (StringUtils.isNumeric(StringUtils.getDigits(list[i])) && list[i].contains("/")) {
                    continue outerloop;
                }
                else {
                    String[] tokens = list[i].split("/");
                    StringBuilder b = new StringBuilder();
                    for (int j = 0; j < tokens.length; j++) {
                        String str1=b.append(tokens[j]+" ").toString();
                        list[i]=str1;
                    }
                    break outerloop;
                }
            }
        }
        return join(list, " ");
    }
}
