package cpsc326;

import java.util.List;


interface OurPLCallable {
      int arity();
      Object call(Interpreter interpreter, List<Object> arguments);
      String toString();
    }
