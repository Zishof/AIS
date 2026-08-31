package ais.common;

import javax.servlet.http.HttpServletResponse;

/**
 * Penyimpan {@link HttpServletResponse} milik thread yang sedang menangani satu request HTTP,
 * memakai pola <i>thread-local holder</i> agar objek response servlet dapat diakses dari
 * lapisan-lapisan kode (service, util, helper) yang berada jauh di bawah controller/servlet
 * tanpa perlu meneruskan (passing) parameter {@code HttpServletResponse} secara eksplisit di
 * sepanjang rantai pemanggilan method.
 *
 * <p>
 * Latar belakang kebutuhan: pada arsitektur web tradisional (servlet/JSP maupun ZK), objek
 * {@link HttpServletResponse} biasanya hanya tersedia pada method yang langsung dipanggil oleh
 * kontainer servlet (mis. {@code doGet}/{@code doPost}, atau listener event pertama pada suatu
 * request). Namun ada kalanya kode util/helper di lapisan yang jauh lebih dalam — misalnya untuk
 * mengatur header respons secara manual, melakukan redirect, menulis cookie, atau menghasilkan
 * unduhan file langsung ke output stream response — membutuhkan akses ke response tersebut tanpa
 * harus mengubah signature seluruh method di antara titik masuk request dan titik pemakaian.
 * Kelas ini menjawab kebutuhan tersebut dengan menyimpan response pada
 * {@link ThreadLocal}, yang secara efektif berperan seperti sebuah {@code Map<Thread, HttpServletResponse>}
 * tersembunyi: setiap thread request memiliki slot penyimpanannya sendiri yang terisolasi dari
 * thread request lain yang berjalan bersamaan.
 * </p>
 *
 * <p>
 * <b>Siklus hidup wajib</b> — karena thread-thread pada server servlet umumnya berasal dari
 * <i>thread pool</i> yang dipakai ulang untuk request berikutnya, nilai yang disimpan lewat
 * {@link #set(HttpServletResponse)} pada awal pemrosesan request WAJIB dibersihkan lewat
 * {@link #remove()} setelah request selesai diproses (idealnya di blok {@code finally} pada
 * filter atau titik masuk request). Kegagalan memanggil {@link #remove()} akan menyebabkan
 * <b>kebocoran memori</b> (objek response beserta seluruh graf objek yang dirujuknya tertahan
 * selama thread masih hidup di pool) dan berpotensi membuat request BERIKUTNYA yang kebetulan
 * dilayani oleh thread yang sama secara keliru membaca response milik request SEBELUMNYA bila
 * {@link #set(HttpServletResponse)} tidak dipanggil ulang di awal request baru.
 * </p>
 *
 * <p>
 * Kelas ini murni statis (utility holder), tidak memiliki state instance, dan seluruh method
 * bersifat thread-safe secara inheren karena {@link ThreadLocal} menjamin isolasi antar-thread.
 * </p>
 */
public class ResponseContext {
    // ThreadLocal bertindak seperti Map<Thread, HttpServletResponse>
    private static final ThreadLocal<HttpServletResponse> RESPONSE_HOLDER = new ThreadLocal<HttpServletResponse>();

    /**
     * Menyimpan {@link HttpServletResponse} milik request yang sedang diproses pada thread saat
     * ini, agar dapat diambil kembali lewat {@link #get()} dari lapisan kode mana pun selama
     * masih berjalan pada thread yang sama.
     *
     * @param request objek {@link HttpServletResponse} milik request aktif; parameter dinamai
     *                {@code request} pada signature ini walau bertipe response (mengikuti kode
     *                sumber aslinya) — nilai ini akan dikembalikan apa adanya oleh {@link #get()}
     */
    public static void set(HttpServletResponse request) {
    	RESPONSE_HOLDER.set(request);
    }

    /**
     * Mengambil {@link HttpServletResponse} yang sebelumnya disimpan lewat
     * {@link #set(HttpServletResponse)} pada thread yang sama.
     *
     * @return response milik thread saat ini, atau {@code null} bila belum pernah diisi
     *         (atau sudah dibersihkan lewat {@link #remove()}) pada thread ini
     */
    public static HttpServletResponse get() {
        return RESPONSE_HOLDER.get();
    }

    /**
     * Membersihkan nilai response yang tersimpan pada thread saat ini. WAJIB dipanggil setelah
     * pemrosesan request selesai (biasanya di blok {@code finally}) untuk mencegah kebocoran
     * memori dan kebocoran data antar-request akibat pemakaian ulang thread oleh thread pool
     * server.
     */
    public static void remove() {
    	RESPONSE_HOLDER.remove();
    }
}