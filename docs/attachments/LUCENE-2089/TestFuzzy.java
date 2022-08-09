import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.BasicAutomata;
import org.apache.lucene.util.automaton.BasicOperations;
import org.apache.lucene.util.automaton.MinimizationOperations;

import junit.framework.TestCase;

public class TestFuzzy extends TestCase {
  public void testMarksAlgorithm() throws Exception {
    Automaton a1 = fuzzyAutomaton("foobar");
    // Automaton a2 = marksAlgorithm("foobar");
    // Automaton .equals() means they accept the same langauge.
    // assertEquals(a1, a2);
  }
  
  /**
   * Return an automaton that accepts all 1-character insertions, deletions, and
   * substitutions of s.
   */
  Automaton fuzzyAutomaton(String s) {
    Automaton a = insertionsOf(s);
    a = BasicOperations.union(a, deletionsOf(s));
    MinimizationOperations.minimize(a);
    a = BasicOperations.union(a, substitutionsOf(s));
    MinimizationOperations.minimize(a);
    
    return a;
  }
  
  /**
   * Return an automaton that accepts all 1-character insertions of s (inserting
   * one character)
   */
  Automaton insertionsOf(String s) {
    List<Automaton> list = new ArrayList<Automaton>();
    
    for (int i = 0; i <= s.length(); i++) {
      Automaton a = BasicAutomata.makeString(s.substring(0, i));
      a = BasicOperations.concatenate(a, BasicAutomata.makeAnyChar());
      a = BasicOperations.concatenate(a, BasicAutomata.makeString(s
          .substring(i)));
      list.add(a);
    }
    
    Automaton a = BasicOperations.union(list);
    MinimizationOperations.minimize(a);
    return a;
  }
  
  /**
   * Return an automaton that accepts all 1-character deletions of s (deleting
   * one character)
   */
  Automaton deletionsOf(String s) {
    List<Automaton> list = new ArrayList<Automaton>();
    
    for (int i = 0; i < s.length(); i++) {
      Automaton a = BasicAutomata.makeString(s.substring(0, i));
      a = BasicOperations.concatenate(a, BasicAutomata.makeString(s
          .substring(i + 1)));
      a.expandSingleton();
      list.add(a);
    }
    
    Automaton a = BasicOperations.union(list);
    MinimizationOperations.minimize(a);
    return a;
  }
  
  /**
   * Return an automaton that accepts all 1-character substitutions of s
   * (replacing one character)
   */
  Automaton substitutionsOf(String s) {
    List<Automaton> list = new ArrayList<Automaton>();
    
    for (int i = 0; i < s.length(); i++) {
      Automaton a = BasicAutomata.makeString(s.substring(0, i));
      a = BasicOperations.concatenate(a, BasicAutomata.makeAnyChar());
      a = BasicOperations.concatenate(a, BasicAutomata.makeString(s
          .substring(i + 1)));
      list.add(a);
    }
    
    Automaton a = BasicOperations.union(list);
    MinimizationOperations.minimize(a);
    return a;
  }
}
