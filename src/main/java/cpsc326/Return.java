package cpsc326;

class Return extends RuntimeException {
    final Object value; // the value being returned

    Return(Object value) {
        super(null, null, false, false); // disable stack trace for performance
        this.value = value;
    }
    
}
