package dev.msmr;
import java.util.*;

/** Ranks exact names first, then token/namespace matches, then small typing errors. */
public final class LookupSearch {
    private LookupSearch() {}
    private static String normalize(String text) { return text.toLowerCase(Locale.ROOT).replace('_',' ').replace('/',' '); }
    public static int score(String query,String name,String id,String related) {
        String q=normalize(query.trim()), n=normalize(name), key=normalize(id), extra=normalize(related);
        if(q.isEmpty()) return 10;
        if(key.equals(q) || key.equals(q.startsWith("#")?q.substring(1):q)) return 0;
        if(q.startsWith("#block:")) q=q.substring(7);
        else if(q.startsWith("#") || q.startsWith("@")) q=q.substring(1);
        if(n.equals(q)) return 1;
        int score=0;
        for(String word:q.split("\\s+")) {
            if(n.startsWith(word)) score+=2;
            else if(n.contains(word)) score+=4;
            else if(key.contains(word)) score+=6;
            else if(extra.contains(word)) score+=12;
            else if(word.length()>=4 && Arrays.stream((n+" "+key.replace(':',' ')).split("\\s+")).anyMatch(candidate -> near(word,candidate))) score+=30;
            else return -1;
        }
        return score;
    }
    private static boolean near(String a,String b) {
        if(Math.abs(a.length()-b.length())>1) return false;
        int i=0,j=0,edits=0;
        while(i<a.length() && j<b.length()) {
            if(a.charAt(i)==b.charAt(j)) { i++; j++; continue; }
            if(++edits>1) return false;
            if(a.length()>=b.length()) i++;
            if(b.length()>=a.length()) j++;
        }
        return edits+(a.length()-i)+(b.length()-j)<=1;
    }
}
