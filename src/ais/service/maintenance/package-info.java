/**
 * Tugas pemeliharaan latar belakang (housekeeping) yang berjalan otomatis di dalam siklus hidup
 * webapp AIS, di luar alur permintaan pengguna biasa -- pembersihan data yang menumpuk seiring
 * waktu, tanpa memerlukan campur tangan admin atau job eksternal (cron OS, scheduler terpisah).
 *
 * <h2>Pola scheduler "daemon sekali-jalan"</h2>
 * <p>
 * Kelas-kelas di paket ini (saat ini: {@link ais.service.maintenance.LogMobileCleanupService})
 * mengikuti satu pola yang sama persis dengan {@link ais.service.tenant.TenantProvisioningWorker}
 * di paket {@code ais.service.tenant}:
 * </p>
 * <ol>
 * <li><b>Dipicu oleh {@code ServletContextListener} terpisah</b>, BUKAN dari
 * {@code AppStartupListener} pusat. Ini disengaja -- {@code AppStartupListener} adalah file yang
 * sering "panas" (sedang dikerjakan sesi/berkas lain secara paralel), sehingga menambah dependensi
 * di sana berisiko konflik commit SVN. Listener sendiri (mis.
 * {@code ais.common.LogMobileCleanupListener}) didaftarkan lewat {@code web.xml} dan cukup
 * memanggil {@code mulai()} pada {@code contextInitialized()} serta {@code hentikan()} pada
 * {@code contextDestroyed()}.</li>
 * <li><b>Dijadwalkan dengan delay awal</b> (bukan langsung dieksekusi saat startup) memakai
 * {@link java.util.concurrent.ScheduledExecutorService} single-thread, agar proses berat lain yang
 * juga terjadi saat startup webapp (inisialisasi ZK, {@code SessionFactory} Hibernate, listener
 * lain) sempat selesai lebih dulu dan tidak ikut melambat karena berebut resource dengan tugas
 * housekeeping.</li>
 * <li><b>Thread daemon</b> -- executor selalu dibuat dengan {@link java.util.concurrent.ThreadFactory}
 * kustom yang men-set {@code setDaemon(true)}, sehingga thread housekeeping tidak pernah menahan
 * JVM tetap hidup bila webapp/container hendak berhenti.</li>
 * <li><b>Sekali jalan per start webapp, BUKAN periodik berulang.</b> Setelah tugas selesai
 * (sukses maupun gagal), executor langsung di-{@code shutdown()} dan referensi statis ke jadwal
 * direset ke {@code null}. Tidak ada pengulangan otomatis dalam satu masa hidup proses; siklus
 * berikutnya hanya terjadi bila webapp direstart/di-reload sehingga listener terpicu ulang.</li>
 * <li><b>Idempoten terhadap pemanggilan ganda</b> -- bila jadwal sudah aktif, pemanggilan
 * {@code mulai()} berikutnya adalah no-op. Method {@code hentikan()} dipanggil dari
 * {@code contextDestroyed()} sebagai jaga-jaga bila tugas belum sempat berjalan saat webapp
 * direstart/di-reload cepat (mis. saat deploy ulang di tengah proses), mencegah task lama tertunda
 * pada executor yang sudah ditinggalkan.</li>
 * <li><b>Kegagalan tidak boleh mengganggu startup/shutdown webapp</b> -- seluruh {@link Throwable}
 * dari tugas ditangkap dan dicatat lewat {@code ais.common.ErrorAuditUtil}, tidak pernah dibiarkan
 * merambat ke listener pemanggil.</li>
 * </ol>
 *
 * <h2>Kelas di paket ini</h2>
 * <p>
 * {@link ais.service.maintenance.LogMobileCleanupService} menghapus baris tabel
 * {@code public.log_mobile} (log request/response API mobile dari {@code ApiMobileLogger}) yang
 * lebih tua dari ambang retensi yang dapat dikonfigurasi (default 30 hari, kunci konfigurasi
 * {@code log_mobile_retensi_hari}, dibaca ulang setiap kali berjalan sehingga dapat diubah tanpa
 * deploy ulang). Penghapusan dilakukan per-batch (native SQL dengan {@code ctid}, idiom umum
 * PostgreSQL untuk "DELETE ... LIMIT") dalam transaksi kecil terpisah per batch, bukan satu
 * statement DELETE tunggal untuk seluruh baris kedaluwarsa -- pendekatan batch ini adalah
 * perbaikan atas insiden nyata di mana DELETE tunggal pada tabel log yang sudah membengkak jutaan
 * baris melebihi {@code statement_timeout} server dan dibatalkan paksa tanpa satu pun baris
 * berhasil terhapus. Dengan batch, progres yang sudah terhapus tetap tersimpan walau batch
 * berikutnya gagal atau proses dihentikan di tengah jalan.
 * </p>
 *
 * <h2>Menambah tugas housekeeping baru</h2>
 * <p>
 * Kelas housekeeping baru di paket ini sebaiknya mengikuti pola yang sama: kelas utilitas statis
 * final dengan konstruktor privat, method {@code mulai()}/{@code hentikan()} yang
 * {@code synchronized} dan idempoten, delay awal yang wajar sebelum eksekusi, thread daemon
 * bernama deskriptif, penghapusan/pembersihan berbatch bila volume data berpotensi besar, serta
 * listener {@code ServletContextListener} terpisah yang didaftarkan lewat {@code web.xml} --
 * bukan menambah pemanggilan langsung di {@code AppStartupListener} pusat.
 * </p>
 *
 * @see ais.service.tenant.TenantProvisioningWorker
 * @see ais.common.ErrorAuditUtil
 */
package ais.service.maintenance;
