package ais.common;

import org.zkoss.zk.ui.metainfo.LanguageDefinition;

/**
 * Bootstrap kecil untuk memastikan extension ZUL dikenali oleh ZK 5.x.
 *
 * Pada beberapa deploy lama, forward langsung ke file *.zul di dalam WEB-INF
 * dapat terjadi sebelum mapping extension "zul" terdaftar sempurna. Akibatnya
 * ZK melempar: DefinitionNotFoundException: Language not found for extension zul.
 *
 * Class ini hanya menambahkan mapping extension ke bahasa bawaan xul/html.
 * Tidak mengubah logic halaman, tidak mematikan routing, dan aman dipanggil
 * berulang karena dikunci dengan flag inisialisasi.
 */
public final class ZkLanguageBootstrap {

    private static boolean initialized = false;
    private static final Object LOCK = new Object();

    private ZkLanguageBootstrap() {
    }

    public static void ensureZulLanguageMapping() {
        if (initialized) {
            return;
        }

        synchronized (LOCK) {
            if (initialized) {
                return;
            }

            try {
                /*
                 * Nama bahasa ZUL bawaan ZK adalah xul/html. Extension utama
                 * normalnya zul dan xul. Pemanggilan addExtension aman sebagai
                 * fallback bila mapping default belum terdaftar saat runtime.
                 */
                LanguageDefinition.addExtension("zul", "xul/html");
                LanguageDefinition.addExtension("xul", "xul/html");

                /*
                 * Verifikasi ringan. Jika masih gagal, exception ditangkap di
                 * bawah agar startup aplikasi tidak berhenti total; error asli
                 * tetap akan terlihat di log ZK bila library ZUL benar-benar
                 * tidak ada di WEB-INF/lib.
                 */
                LanguageDefinition.getByExtension("zul");

                initialized = true;
            } catch (Throwable t) {
                initialized = false;
                try {
                    System.err.println("[AIS] Gagal bootstrap mapping bahasa ZUL: " + t.getMessage());
                    t.printStackTrace(System.err);
                } catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/ZkLanguageBootstrap.java:57");
                    // Abaikan agar tidak mengganggu startup container.
                }
            }
        }
    }
}
