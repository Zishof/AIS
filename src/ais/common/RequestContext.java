package ais.common;

import javax.servlet.http.HttpServletRequest;

/**
 * Wadah statis berbasis {@link ThreadLocal} yang menyimpan referensi ke
 * {@link HttpServletRequest} yang sedang diproses oleh thread saat ini, sehingga kode di lapisan
 * mana pun (servis, util, kelas domain) dapat mengakses request HTTP yang aktif tanpa harus
 * meneruskannya sebagai parameter di sepanjang rantai pemanggilan method.
 *
 * <p>
 * Pola ini umum dipakai pada aplikasi web berbasis servlet/thread-per-request seperti AIS: setiap
 * permintaan HTTP masuk dilayani oleh satu thread dari pool container (Tomcat/Jetty dsb.), dan
 * {@link ThreadLocal} secara konseptual berperan seperti {@code Map<Thread, HttpServletRequest>}
 * — setiap thread memiliki slot penyimpanan sendiri yang terisolasi dari thread lain, sehingga
 * request milik satu pengguna tidak akan pernah "bocor" terbaca oleh thread yang sedang melayani
 * pengguna lain selama pemetaan thread-ke-request dikelola dengan benar.
 * </p>
 *
 * <h2>Siklus hidup dan risiko kebocoran</h2>
 * <p>
 * Kelas ini TIDAK mengatur sendiri kapan {@link #set(HttpServletRequest)} dan
 * {@link #remove()} dipanggil — itu tanggung jawab pemanggil, biasanya sebuah filter servlet yang
 * memanggil {@link #set(HttpServletRequest)} di awal pemrosesan request dan
 * {@link #remove()} di blok {@code finally} setelah request selesai diproses. Karena thread
 * container web umumnya dipakai ulang (pool thread, bukan dibuat baru per request), KELALAIAN
 * memanggil {@link #remove()} akan membuat referensi {@link HttpServletRequest} lama tetap
 * tersimpan di slot thread tersebut dan "bocor" terbaca oleh request BERIKUTNYA yang kebetulan
 * dilayani thread yang sama — berpotensi menimbulkan kebingungan data lintas-pengguna maupun
 * membebani memori karena request lama (beserta seluruh graf objek yang direferensikannya) tidak
 * pernah menjadi eligible untuk garbage collection selama thread masih hidup.
 * </p>
 *
 * <p>
 * Karena sifatnya yang statis dan berbasis {@link ThreadLocal}, kelas ini aman dipakai bersamaan
 * oleh banyak thread (thread-safe) tanpa memerlukan sinkronisasi eksplisit, sebab setiap thread
 * hanya pernah membaca/menulis slotnya sendiri.
 * </p>
 */
public class RequestContext {
    /**
     * Slot penyimpanan per-thread untuk {@link HttpServletRequest} yang sedang aktif. Bertindak
     * seperti {@code Map<Thread, HttpServletRequest>} — setiap thread hanya dapat membaca nilai
     * yang ditulis oleh dirinya sendiri lewat {@link #set(HttpServletRequest)}.
     */
    private static final ThreadLocal<HttpServletRequest> REQUEST_HOLDER = new ThreadLocal<HttpServletRequest>();

    /**
     * Menyimpan {@code request} ke slot thread saat ini. Biasanya dipanggil oleh filter servlet
     * di awal pemrosesan satu permintaan HTTP.
     *
     * @param request objek {@link HttpServletRequest} yang sedang diproses oleh thread ini
     */
    public static void set(HttpServletRequest request) {
        REQUEST_HOLDER.set(request);
    }

    /**
     * Mengambil {@link HttpServletRequest} yang tersimpan untuk thread saat ini.
     *
     * @return request yang sebelumnya disimpan lewat {@link #set(HttpServletRequest)}, atau
     *         {@code null} bila belum pernah diisi (atau sudah dibersihkan lewat
     *         {@link #remove()}) pada thread ini
     */
    public static HttpServletRequest get() {
        return REQUEST_HOLDER.get();
    }

    /**
     * Membersihkan slot request milik thread saat ini. WAJIB dipanggil (idealnya di blok
     * {@code finally} pada filter servlet) setelah pemrosesan satu permintaan selesai, agar
     * thread yang dipakai ulang oleh container tidak membawa referensi request lama ke request
     * berikutnya (lihat catatan kebocoran pada Javadoc kelas).
     */
    public static void remove() {
        REQUEST_HOLDER.remove();
    }
}