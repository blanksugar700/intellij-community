// "Replace with 'removals.forEach(source::remove)'" "true"

import java.util.*;

class Test {
  void foo(Set<Integer> source, ArrayList<Integer> removals) {
    source.removeAll(removals)<caret>;
  }
}
