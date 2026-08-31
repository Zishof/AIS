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

    /**
     * Pembawa data/helper lokal milik {@link HeadlessActionContext} untuk state. Tipe ini mengelompokkan nilai
     * antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * HeadlessActionContext}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan
     * diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String message}. Aturan bisnis
     * bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see HeadlessActionContext
     */
    private static final class State {
        private String message;
    }
}
