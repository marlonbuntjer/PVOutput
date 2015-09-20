package com.github.marlonbuntjer.pvoutput;

/**
 * Created by Marlon Buntjer on 30-6-2015.
 */
class PVOutputConnectionException extends Exception {

    public PVOutputConnectionException() {
        super();
    }

    public PVOutputConnectionException(String message) {
        super(message);
    }

    public PVOutputConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PVOutputConnectionException(Throwable cause) {
        super(cause);
    }

}
