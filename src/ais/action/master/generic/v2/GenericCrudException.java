package ais.action.master.generic.v2;

/** Exception safe untuk dipetakan menjadi respons HTTP tanpa membuka detail internal. */
public class GenericCrudException extends Exception {
    private static final long serialVersionUID = 1L;
    private final int status;
    private final String code;

    public GenericCrudException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public GenericCrudException(int status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
}
