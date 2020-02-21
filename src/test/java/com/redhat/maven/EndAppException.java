package com.redhat.maven;

/** Exception for simulating end of application
 */
public class EndAppException extends RuntimeException {
    public EndAppException(int returnValue) {
        super();
        this.returnValue = returnValue;
    }

    private int returnValue;

    public int getReturnValue() {
        return returnValue;
    }
}
