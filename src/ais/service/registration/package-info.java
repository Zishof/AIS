/**
 * Alur pendaftaran tenant mandiri (self-service signup) AIS -- rute publik
 * {@code /pendaftaran} tempat pemilik usaha baru mendaftarkan tenant sendiri tanpa perantara admin,
 * beserta compatibility bridge ke rute lama {@code aksi=daftar}, backoffice persetujuan admin, dan
 * perkakas migrasi/verifikasi pendukungnya.
 *
 * <h2>Dokumen master</h2>
 * <p>
 * Seluruh kelas di paket ini merujuk berulang kali ke satu "dokumen master" proyek pendaftaran
 * tenant mandiri (naskah spesifikasi ber-bagian §1..§21+ di {@code docs/pendaftaran-tenant/},
 * termasuk berkas alur {@code 05-workflow.md} dan migrasi {@code 09-migration.md}) sebagai sumber
 * kebenaran tunggal untuk aturan bisnis: format/validasi field (§14), reservasi username/schema
 * (§3.4), transaksi submit (§9.1), katalog jenis usaha (§6.3/§7, §8.3), kebijakan privasi respons
 * (§13.5), backoffice admin (§15), serta rencana verifikasi mandiri maupun konkurensi (§21.x).
 * Javadoc method-level di kelas-kelas ini SENGAJA menautkan kembali ke nomor bagian tersebut alih-alih
 * mengulang-duplikasi teks spesifikasi -- pembaca yang butuh latar belakang keputusan desain sebaiknya
 * membuka dokumen master di bagian yang dirujuk.
 * </p>
 *
 * <h2>Alur inti (rute {@code /pendaftaran})</h2>
 * <ol>
 * <li>Field yang dikirim pendaftar dinormalisasi &amp; divalidasi murni tanpa akses DB oleh
 * {@link ais.service.registration.PendaftaranValidationService} (format email/username, kekuatan
 * password, versi dokumen kebijakan yang disetujui) -- lolos di tahap ini belum menjamin lolos
 * keseluruhan karena aturan yang butuh DB (ketersediaan username, jenis usaha aktif) diperiksa
 * belakangan.</li>
 * <li>Ketersediaan username/schema tenant dicek secara informatif oleh
 * {@link ais.service.registration.UsernameReservationService#tersedia}, lalu benar-benar
 * direservasi secara atomik (INSERT unique constraint sebagai penyerialisasi race dua submit
 * bersamaan) di dalam transaksi submit.</li>
 * <li>{@link ais.service.registration.PendaftaranTenantService} adalah SATU sumber aturan yang
 * mengorkestrasi seluruh langkah submit dalam SATU transaksi Hibernate (§9.1): cari/buat akun
 * {@code Pendaftar}, buat profil tenant, buat permohonan, catat pilihan jenis usaha (dengan katalog
 * dari {@link ais.service.registration.JenisUsahaTenantSeedService}), catat consent, reservasi
 * username, buat tantangan verifikasi email, dan catat audit event -- gagal di mana pun berarti
 * rollback total.</li>
 * <li>{@link ais.service.registration.EmailVerificationService} mengelola token verifikasi email
 * (hash SHA-256 tersimpan, token 32-byte SecureRandom dikirim via tautan), termasuk resend dan
 * supersede token lama, dengan pengiriman lewat jalur produksi
 * {@code ais.delivery.email.sender.MailSender} -- kegagalan kirim TIDAK membatalkan permohonan.</li>
 * <li>Setelah permohonan diverifikasi, admin memeriksa dan memutuskan lewat
 * {@link ais.service.registration.PendaftaranTenantAdminService} (backoffice §15): approve/reject
 * dengan alasan wajib, monitor step provisioning, retry idempoten, atau lepas reservasi
 * username -- seluruhnya mengasumsikan pemanggil sudah lolos gerbang privilege admin di lapisan
 * servlet.</li>
 * </ol>
 *
 * <h2>Perkakas pendukung (bukan bagian alur runtime rute {@code /pendaftaran})</h2>
 * <ul>
 * <li>{@link ais.service.registration.PendaftarBackfillTool} -- tool operator dijalankan manual
 * (bukan otomatis saat startup) untuk migrasi akun {@code Pendaftar} lama ke skema profil tenant
 * baru (§16.4), dengan aturan klasifikasi jenis usaha yang ketat (tidak mengarang data).</li>
 * <li>{@link ais.service.registration.VerifikasiPendaftaranTenantMandiri} -- harness verifikasi
 * mandiri (§21.1) untuk logika murni tanpa DB/kontainer (normalisasi, regex, hash), dijalankan
 * lewat {@code main(String[])} dengan exit code sebagai indikator lulus/gagal.</li>
 * <li>{@link ais.service.registration.VerifikasiKonkurensiPendaftaran} -- harness uji konkurensi
 * (§21.3) yang mengirim permintaan HTTP paralel ke server dev/UAT ter-deploy untuk memverifikasi
 * invariant "maksimal satu pendaftaran sukses per username" dan idempotensi replay berbasis
 * idempotency key.</li>
 * </ul>
 *
 * @see ais.database.model.tenant.PendaftaranTenant
 * @see ais.database.model.tenant.SchemaNameReservation
 * @see ais.delivery.email.sender.MailSender
 */
package ais.service.registration;
