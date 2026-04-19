package cpsc326;

import java.util.Map;
import java.util.HashMap;

public class Environment {
    Environment enclosing;
    Map<String, Object> values = new HashMap<String, Object>();

    Environment() { // base constructor for global env
        enclosing = null;
    }

    Environment(Environment enclosing) { // constructor for all nested envs, takes in father env
        this.enclosing = enclosing;
    }

    void define(String name, Object value) { // define a new variable in the current environment
        values.put(name, value);
    }

    Object get(Token name) { // get the value of a variable. 
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        // If it is not in the current environment, check the enclosing environment
        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value) { // assign a new value to an existing var
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) { // checks enclosing env if doesn't exist in current env
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
