package ais.common;

/** Business rule Action existing menolak penyimpanan dari endpoint New UI. */
public class HeadlessBusinessRuleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public HeadlessBusinessRuleException(String message) {
        super(message);
    }

    public HeadlessBusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
