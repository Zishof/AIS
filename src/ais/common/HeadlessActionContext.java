package ais.common;

/**
 * Konteks thread-local untuk menjalankan business rule Action existing dari
 * endpoint New UI tanpa menulis dialog/JavaScript ZK ke response JSON.
 */
public final class HeadlessActionContext {
    private static final ThreadLocal<State> STATE = new ThreadLocal<State>();

    private HeadlessActionContext() { }

    public static void enter() {
        STATE.set(new State());
    }

    public static boolean isActive() {
        return STATE.get() != null;
    }

    public static void record(String message) {
        State state = STATE.get();
        if (state != null && message != null && message.trim().length() > 0) {
            state.message = message.trim();
        }
    }

    public static String exit() {
        State state = STATE.get();
        STATE.remove();
        return state == null ? null : state.message;
    }

    private static final class State {
        private String message;
    }
}
