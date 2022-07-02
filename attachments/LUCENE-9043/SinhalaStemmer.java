package sl;

import java.util.*;


public class SinhalaStemmer {

    private char[] b;
    private char[] b0;
    private char[] b1;
    private char[] b2;
    private int k,i,k0;    /* offset into b */
    private static final int INITIAL_SIZE = 50;
    private static final String[] suffix_list_for_wordend= {"ක","කර","කරු","කලු","කාරී","ගත","ජ","ජනක","ත්","ට","ති","ත්ම","දායක","දායී","න","නවා","නීය","නු","නුව","න්ට","න්න","න්නට","න්නම්","බද","බර","මය","මාන","මි","මින්","මු","මුවා","ව","වරු","සම්පන්න","සහගත","හ","හි","හු","ගෙන්","ගේ"};
    private static final String[] suffix_list_for_wordmiddle = {"කාමි","කාර","ගරුක","ගාමි","චර","චාරී","ඥ","තම","තර","තුම්","දායක","ධර","ධාරී","නි","නු","මත්","මන්","ය","වන්ත","වර","වාදි","වාසී","වෙදි","ශීලි"};
    private static final String[] back = {"්", "ා", "ි", "ු", "ූ", "ී", "ැ", "ෑ", "ෙ", "ේ", "ො", "ෝ", "ං"};
    private static final String[] prefix_list = {"ස්ව", "ස්වයං"};
    private String stemmedString;

     SinhalaStemmer(ArrayList<String> l) {
        b = new char[INITIAL_SIZE];
        i = 0;
        stemmedString=stemming(l).toString();
    }
    public String getStemmedString(){
         return stemmedString;
    }
    private ArrayList<String> stemming(ArrayList<String> l) {
        //CharArraySet c = new CharArraySet(Version.LUCENE_CURRENT, stopWords, false);
        HashMap<String, Integer> stemmedWords = new HashMap<String, Integer>();
        ArrayList<String> stemwordlist = new ArrayList<String>();
        for (int i = 0; i < l.size(); i++) {
            b = l.get(i).toCharArray();
            b0=b;
            b1=b;
            b2=b;
            k = b.length - 1;
            if(k<0) continue;
			/*for (int j = 0; j < b.length; j++) {
				System.out.println(i+" "+j+" "+b[j]);
			}*/
            /*String s = new String(b);
            System.out.println(s);*/
            step1();
            if(k>=4)
                step2();
            boolean Stemmed_from_step_2 = isStemmed();

			if(k>=4 || !Stemmed_from_step_2)//avoid 4 character
			    step3();
            boolean Stemmed_from_step_3 = isStemmed();

            if(k>=4 && !Stemmed_from_step_3)
                step4();
            boolean Stemmed_from_step_4 = isStemmed();

            if (k>=4 && !(Stemmed_from_step_3 && Stemmed_from_step_4))
                step5();
            boolean Stemmed_from_step_5 = isStemmed();

            String ss = new String(b);
            if (stemmedWords.containsKey(ss)) {

            } else {
                stemmedWords.put(ss, 1);
                stemwordlist.add(ss);//hash  map
            }
        }
        //System.out.println(stemwordlist);
        return stemwordlist;
    }

    //check word is stemmed or not
    private boolean isStemmed() {
        if(b.equals(b2)){
            return false;
        }
        else {
            return true;
        }
    }

    //check format of the morpheme
    private void CheckMorpheme(){
        String s = new String(b);
        //check for morphemes end with අ
        if(b.length-1>=0 && (!iSispilipapili(b[b.length-1])) && isASinhalaLetter(s)){
            b0=null;
            b0=b;
        }
        //check for morphemes end with '්'
        else if (k>0 && b[k]=='්'){
            char c = b[k-1];
            switch (c){
                case 'ක' :
                case 'ප' :
                case 'ත' :
                case 'න' :
                case 'ම' :
                case 'ල' :
                case 'ස' :
                case 'බ' :
                case 'ව' :  b0=null; b0=b;break;
                default: b=null; b=b0; k=b.length-1;
            }
        }
        else if(k>=0){
            switch (b[k]){
                case 'ා' :
                case 'ැ' :
                case 'ෑ' :
                case 'ි' :
                case 'ු' :
                case 'ෙ' :
                case 'ෝ' :
                case 'ං' : b0=null; b0=b; break;
                default: b=null; b=b0; k=b.length-1;
            }
        }
    }
    private boolean isASinhalaLetter(String s) {
        if(s.length() != 1) return true;
        int sinhalaLowerBound = 3456;
        int sinhalaUpperBound = 3583;

        int cp = s.codePointAt(0);
        if(cp >= sinhalaLowerBound && cp <= sinhalaUpperBound) {
            return true;
        }
        return false;
    }

    private boolean iSispilipapili(char c) {
        int ispilla = 3530;
        int ispiliLowerBound = 3535; //ODCF ා
        int ispiliUpperBound = 3551; //oDDF

        //String s = new String(c);
        int cp = (int) c;

        if((cp >= ispiliLowerBound && cp <= ispiliUpperBound) || cp == ispilla) {
            return true;
        }
        return false;
    }

    //add new  a 1 character to b at the end
    private void add(char ch) {
        k += 1;
        String s = new String(b);
        StringBuilder sb = new StringBuilder(s);
        sb=sb.append(ch);
        b=sb.toString().toCharArray();
    }


    //remove characters from word
    private void remove(int removeFrom) {
        String s = new String(b);
        s = s.substring(0, removeFrom);
        b = s.toCharArray();
        k = (removeFrom - 1);
    }

    //remove string from end
    private void remove(String suf) {
        if(k>0 && (!(b.length<=suf.length())) ){
            int l = suf.length();
            String s = new String(b);
            s = s.substring(0, k + 1 - l);
            b = s.toCharArray();
            k = k - l;
        }
    }

    private void removeF(String pre) {
        int l = pre.length();
        String s = new String(b);
        s = s.substring(l);
        b = s.toCharArray();
        k = k - l;
    }

    private boolean ends(String s){
        if(!(b.length<=s.length()))
        {
            String s0 = new String(b);
            if (s0.endsWith(s)) return true;
        }
        return false;
    }

    private boolean starts(String s) {
        String s0 = new String(b);
        final boolean b = s0.startsWith(s);
        return b;
    }

    private void step1() {
        for (String prefix : prefix_list) {
            if (starts(prefix)) {
                removeF(prefix);
            }

        }
    }

    private void checkstem(List<char[]> temp_words){
        CheckMorpheme();
        temp_words.add(b);
        b=b1;
        k=b.length-1;
        String ss = new String(b);
    }

    private void step2(){
        List<char[]> temp_words = new ArrayList<char[]>();
        //අ
        if (!(Arrays.asList(back).contains(Character.toString(b[k]))) && isASinhalaLetter(Character.toString(b[k])))
        {
            if (b[k] == 'ව' || b[k] == 'ය') {
                remove(k);
            }
            else if(b[k-1]=='්'){
                remove(k-1);
                add('ි');
            }
            else
                add('්');
            checkstem(temp_words);
        }
        //අක්
        if (b[k] == '්' && b[k - 1] == 'ක' && !(Arrays.asList(back).contains(Character.toString(b[k - 2]))) && isASinhalaLetter(Character.toString(b[k-2])))
        {
            if ('ව' == b[k - 2]) {
                remove(k-2);
            } else if (b[k - 2] == 'ය') {
                remove(k - 2);
            }
            else if(b[k-3]=='්'){
                remove(k-3);
                add('ි');
            }
            else {
                remove(k - 1);
                add('්');
            }
            checkstem(temp_words);
        }
        //අක
        if (b[k] == 'ක' && !(Arrays.asList(back).contains(Character.toString(b[k - 1]))) && isASinhalaLetter(Character.toString(b[k-1])))
        {
            if (b[k-1] == 'ව' || b[k-1] == 'ය')
            {
                remove(k-1);
            }
            checkstem(temp_words);
        }
        //අකු
        if (b[k]=='ු' && b[k-1] == 'ක' && !(Arrays.asList(back).contains(Character.toString(b[k - 2]))))
        {
            if(b[k-2]=='ෙ'){
                remove(k-2);
                add('්');
            }
            else if(b[k-2]=='ය' || b[k-2]=='ව')
                remove(k-2);
            else {
                remove(k - 1);
                add('්');
            }
            checkstem(temp_words);
        }

        //අට
        if(ends("ට") && !iSispilipapili(b[k-1])){
            if(b[k-1]=='ව' || b[k-1]=='ය')
                remove(k-1);
            else {
                remove(k);
                add('්');
            }
            checkstem(temp_words);
        }
        //අටු
        if(ends("ටු")){
            if(b[k-2]=='ව' || b[k-2]=='ය')
                remove(k-2);
            else {
                remove(k-1);
            }
            checkstem(temp_words);
        }
        //අත්
        if(ends("ත්") && !iSispilipapili(b[k-2])){
            if(b[k-2]=='ව' || b[k-2]=='ය')
                remove(k-2);
            else {
                remove(k-1);
            }
            checkstem(temp_words);
        }
        //අන්
        if(ends("න්") && !iSispilipapili(b[k-2])){
            if(b[k-2]=='ව' || b[k-2]=='ය')
                remove(k-2);
            else {
                remove(k-1);
            }
            checkstem(temp_words);
        }
        //අනුකූල
        if(ends("නුකූල")){
            remove("නුකූල");
            remove(k);
            checkstem(temp_words);
        }
        //අන්විත
        if(ends("න්විත")){
            remove("න්විත");
            remove(k);
            checkstem(temp_words);
        }

        //ආ
        if (b[k] == 'ා') {

            if((b[k-2]=='්')) {
                //do nothing  කාන්තා දක්වා
            }
            else if (b[k - 1] == 'ය' || b[k - 1] == 'ව') {
                remove(k - 1);
            }
            else if( iSispilipapili(b[k-2])) {
                ///මිනිසා
                remove(k);
                add('්');
            }
            else {
                remove(k);
            }
            checkstem(temp_words);
        }
        //ආක්
        if(ends("ාක්")){
            if(b[k-3]=='ව' || b[k-3]=='ය')
                remove(k-3);
            else if(k>=4){
                remove(k-4);
            }
            checkstem(temp_words);
        }
        //ආට
        if(ends("ාට")){
            if(b[k-2]=='ව' || b[k-2]=='ය')
                remove(k-2);
            else if(k>=4){
                remove(k-4);
            }
            checkstem(temp_words);

        }
        //ආත්මක
        if(k>=4 && ends("ත්මක") && b[k-4]=='ා'){
            remove("ත්මක");
            remove(k);
            checkstem(temp_words);
        }

        //ආම
        if(ends("ම") && b[k-1]=='ා'){
            if(b[k-2]=='ව' || b[k-2]=='ය')
                remove(k-2);
            else {
                remove(k-1);
            }
            checkstem(temp_words);
        }
        //ආය
        if(ends("ය") && b[k-1]=='ා'){
            if(b[k-2]=='ව' || b[k-2]=='ය')
                remove(k-2);
            else {
                remove(k-1);
            }
            checkstem(temp_words);
        }

        //ආයින්
        if(k>=4 && ends("යින්") && b[k-4]=='ා'){
            if(k>=5 && b[k-5]=='ව' || b[k-5]=='ය')
                remove(k-5);
            else {
                remove(k-4);
            }
            checkstem(temp_words);
        }
        //ආවු
        if(ends("වු") && b[k-2]=='ා'){
            if(b[k-3]=='ව' || b[k-3]=='ය')
                remove(k-3);
            else {
                remove(k-2);
            }
            checkstem(temp_words);
        }
        //ආහ
        if(ends("හ") && b[k-1]=='ා'){
            if(b[k-2]=='ව' || b[k-2]=='ය')
                remove(k-2);
            else {
                remove(k-1);
            }
            checkstem(temp_words);
        }
        //ඉ
        if (b[k] == 'ි') {
            if(b[k-1]=='ය'){
                remove(k-1);
                add('ා');
            }
            else {
                remove("ි");
                add('්');
            }
            checkstem(temp_words);
        }
        //ඉක
        if(b[k]=='ක' && b[k-1]=='ි'){
            remove(k-1);
            add('්');
            checkstem(temp_words);
        }
        //ඉච්චි
        if(k>=4 && ends("ච්චි") && b[k-4]=='ි'){
            remove(k-4);
            add('ෙ');
            checkstem(temp_words);
        }
        //ඉණ
        if(ends("ණ") && b[k-1]=='ි'){
            remove(k-1);
            add('ෙ');
            checkstem(temp_words);
        }
        ////ඉණි
        if(ends("ණි") && b[k-2]=='ි'){
            remove(k-2);
            add('ෙ');
            checkstem(temp_words);
        }
        ////ඉත
        if(ends("ත") && b[k-1]=='ි'){
            remove(k-1);
            checkstem(temp_words);
        }
        //ඉන්
        if (b[k] == '්' && b[k - 1] == 'න' && b[k-2]=='ි') {
            if((b[k-3]=='ය' || b[k-3]=='ව')) {
                remove(k - 3);
                add('ා');
            }
            else if (b[k-3]=='ඳ'){
                remove(k-2);
            }
            else{
                remove(k-2);
                add('්');
            }
            checkstem(temp_words);
        }
        //ඉය
        if(ends("ය") && b[k-1]=='ි'){
            remove(k-1);
            if(b[k-1]=='ැ'){
                b[k-1]=b[k];
                remove(k);
            }
            checkstem(temp_words);
        }
        //ඊ
        if(b[k]=='ී'){
            remove(k);
            if(b[k-1]=='ැ' || b[k-1]=='ඇ'){
                add('ෙ');
            }
            checkstem(temp_words);
        }
        //ඊම් ඊමු
        if((ends("මු")||ends("ම්")) && b[k-2]=='ී'){
            remove(k-2);
            checkstem(temp_words);
        }
        //උ
        if(b[k]=='ු'){
            if(b[k-1]=='ව') {
                remove(k - 1);
                add('ා');
            }
            else if(b[k-2]=='්') {
                remove(k - 1);
            }
            else
                remove(k);
            checkstem(temp_words);
        }
        //උණ
        if(b[k]=='ණ' && b[k-1]=='ු'){
            if(b[k-3]=='ැ' || b[k-3]=='ඇ'){
                remove(k-1);
                add('ෙ');
            }
            else
                remove(k-1);
            checkstem(temp_words);
        }
        //උණු
        if(ends("ණු") && b[k-2]=='ු'){
            if(k>=4 && (b[k-4]=='ැ' || b[k-4]=='ඇ')){
                remove(k-2);
                add('ෙ');
            }
            else
                remove(k-1);
            checkstem(temp_words);
        }
        // උන්
        if(ends("න්") && b[k-2]=='ු'){
            if(k>=4 && b[k-4]=='ි') {
                remove(k - 2);
                add('්');
            }
            else
                remove(k-2);
            checkstem(temp_words);
        }
        //ඌ
        if(b[k]=='ූ'){
            if(b[k-2]=='ැ' || b[k-2]=='ඇ') {
                remove(k);
                b[k-1]=b[k];
                remove(k);
            }
            else
                remove(k);
            checkstem(temp_words);
        }
        // ඌහ
        if(ends("හ") && b[k-1]=='ූ'){
            if(b[k-3]=='ැ' || b[k-3]=='ඇ') {
                remove(k-1);
                b[k-1]=b[k];
                remove(k);
            }
            else
                remove(k-1);
            checkstem(temp_words);
        }
        //එක්
        if (b[k - 2] == 'ෙ' && b[k - 1] == 'ක' && b[k] == '්')   //'එක්'
        {
            if (b[k - 3] == 'ය' || b[k - 3] == 'ව') { //සිංහයෙක් යහළුවෙක්
                remove(k - 3);
            }
            //ඇතෙක්
            else {
                remove(k - 2);
                add('්');
            }
            checkstem(temp_words);
        }
        //එකු
        if (b[k - 2] == 'ෙ' && b[k - 1] == 'ක' && b[k] == 'ු')
        {
            if (b[k - 3] == 'ය' || b[k - 3] == 'ව') { //සිංහයෙක් යහළුවෙක්
                remove(k - 3);
            }
            //ඇතෙක්
            else {
                remove(k - 2);
                add('්');
            }
            checkstem(temp_words);
        }
        //එන්
        if(b[k]=='්' && b[k-1]=='න' && b[k-2]=='ෙ'){
            if (b[k-3]=='ය' || b[k-3]=='ව')
                remove(k-3);
            else if(k>=4 && b[k-4] == '්') {
                remove(k - 4);
                add('ු');
            }
            else if(b[k-3]=='ණ'){
                remove(k-2);
                add('ු');
            }
            else {
                remove(k - 2);
                add('්');
            }
            checkstem(temp_words);
        }
        //එමි එමු
        else if((b[k]=='ි'|| b[k]=='ු') && b[k-1]=='ම' && b[k-2]=='ෙ'){
            if(k>=4 && b[k-4]=='්'){
                remove(k-4);
            }
            else if(b[k-3]=='ය' || b[k-3]=='ව'){
                remove(k-3);
            }
            checkstem(temp_words);
        }
        //එහි එහු
        if((b[k]=='ි'||b[k]=='ු') && b[k-1]=='හ' && b[k-2]=='ෙ'){
            if (b[k-3]=='ය' || b[k-3]=='ව')
                remove(k-3);
            else if(k>=4 && b[k-4]=='්'){
                remove(k-4);
            }
            else {
                remove(k-2);
                add('්');
            }
            checkstem(temp_words);
        }
        // ඒ
        if(b[k]=='ේ'){
            if (b[k-1]=='ය' || b[k-1]=='ව')
                remove(k-1);
            else if(b[k-2]=='්'){
                remove(k-2);
            }
            else {
                remove(k);
                add('්');
            }
            checkstem(temp_words);
        }
        // ඒය
        if(ends("ය") && b[k-1]=='ේ'){
            if (b[k-2]=='ය' || b[k-2]=='ව')
                remove(k-2);
            else if(b[k-3]=='්'){
                remove(k-3);
            }
            else {
                remove(k-2);
                add('්');
            }
            checkstem(temp_words);
        }
        // ඔත්
        if(ends("ත්") && b[k-2]=='ො'){
            if (b[k-3]=='ය' || b[k-3]=='ව') {
                remove(k - 3);
            }
            else if(b[k-3]=='ණ'){
                remove(k-3);
                add('ු');
            }
            else {
                remove(k);
                add('්');
            }
            checkstem(temp_words);
        }
        // ඕ
        if (b[k] == 'ෝ') {
            if (b[k-1]=='ව' || b[k-1]=='ය')
                remove(k-1);

            checkstem(temp_words);
        }
        // ඕය
        if (ends("ය") && b[k-1] == 'ෝ') {
            if (b[k-2]=='ව' ||  b[k-2]=='ය')
                remove(k-2);
            else if(b[k-3]=='්'){
                remove(k-3);
            }
            else {
                remove(k - 1);
                add('්');
            }
            checkstem(temp_words);
        }

        //get the correct shortest morpheme
        Get_Correct_Shortest_Morpheme(temp_words);
    }

    private void Get_Correct_Shortest_Morpheme(List<char[]> temp_words) {
        if(temp_words.size()>0) {
            char[] temp_b = temp_words.get(0);
            int min = temp_b.length;
            for (char[] w : temp_words) {
                if (w.length <= min) { //ඉක්මන් ඉක්මනට equal in length
                    min = w.length;
                    temp_b = w;
                }
            }
            //get the final word & k from step2
            b = temp_b;
            k = min-1;
        }
    }

    private void step3() {
        ArrayList<String> s_array = new ArrayList<String>();
        char [] b_temp;
        b_temp = b;
        /*get all suffixes*/
        for (String sufffix : suffix_list_for_wordend) {
            if (ends(sufffix))
                s_array.add(sufffix);
        }
        /*get the longest suffix*/
        int max=0; String max_suffix="";
        for (String e: s_array) {
            if(max<e.length()){
                max=e.length();
                max_suffix=e;
            }
        }
        /*remove longet suffix*/
        remove(max_suffix);

        CheckMorpheme();

        if(b.length<=3)
            b=b_temp;
    }
    private void step4(){
        List<char[]> temp_words_2 = new ArrayList<char[]>();
        //අගාර
        if(ends("ගාර") && !iSispilipapili(b[k-4])){
            remove(k-2);
            checkstem(temp_words_2);
        }
        //අණි //අණු
        if((ends("ණි") || ends("ණු")) && !iSispilipapili(b[k-2])){
            if(b[k-2]=='ව' || b[k-2]=='ය')
                remove(k-2);
            else {
                remove(k-1);
                add('්');
            }
            checkstem(temp_words_2);
        }

        //අන්තර
        if (ends("න්තර")){
            if(b[k-4]=='ා') {
                remove(k - 4);
            }
            else if(k>=6){
                remove(k -6);
                add('ි');
            }
            checkstem(temp_words_2);
        }
        //අස්
        if(ends("ස්")){
            remove(k-1);
            add('්');
            checkstem(temp_words_2);
        }
        //ආවලි
        if(ends("වලි") && b[k-3]=='ා'){
            remove(k-3);
            checkstem(temp_words_2);
        }
        //ඉකා
        if(ends("කා") && b[k-2]=='ි'){
            if(b[k-3]=='ය'){
                remove(k-2);
                add('ක');
            }
            else
                remove(k-2);
            checkstem(temp_words_2);
        }
        // ඉච්චි
        if(k>=4 && ends("ච්චි") && ((b[k-4]=='ි')||(b[k-4]=='ී'))){
            if(k>=5 && b[k-5]=='ළ'){
                remove(k-4);
                add('ු');
            }
            else if((b[k-4]=='ී')){
                remove(k-3);
            }
            else
                remove(k-3);
            checkstem(temp_words_2);
        }
        //ඉනි
        if(ends("නි") && b[k-2]=='ි'){
            remove(k-2);
            add('්');
            checkstem(temp_words_2);
        }

        //ඉස්සි
        if(k>=4 &&  ends("ස්සි") && b[k-4]=='ි'){
            if(k>=5 && b[k-5]=='ළ'){
                remove(k-4);
                add('ු');
            }
            else
                remove(k-3);
            checkstem(temp_words_2);
        }
        // ඊන  ඊය
        if((ends("න") || ends("ය")) && b[k-1]=='ී'){
            remove(k-1);
            checkstem(temp_words_2);
        }
        //උම්
        if (ends("ම්") && b[k-2]=='ු'){
            if(b[k-4]=='ැ'){
                b[k-4]=b[k-3];
                remove(k-3);
            }
            else
                remove(k-2);
            checkstem(temp_words_2);
        }

        //get the correct shortest morpheme
            Get_Correct_Shortest_Morpheme(temp_words_2);
    }

    private void step5(){
        ArrayList<String> s_array = new ArrayList<String>();
        char [] b_temp;
        b_temp = b;
        /*get all suffixes*/
        for (String sufffix : suffix_list_for_wordend) {
            if (ends(sufffix))
                s_array.add(sufffix);
        }
        /*get the longest suffix*/
        int max=0; String max_suffix="";
        for (String e: s_array) {
            if(max<e.length()){
                max=e.length();
                max_suffix=e;
            }
        }
        /*remove longet suffix*/
        remove(max_suffix);

        CheckMorpheme();

        if(b.length<=3)
            b=b_temp;
    }
}
