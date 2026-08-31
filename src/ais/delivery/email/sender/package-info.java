/**
 * Jalur pengiriman notifikasi keluar (email, WhatsApp, push mobile) untuk seluruh modul AIS
 * (akademik, sekolah, POS/akunting, dsb.) beserta arsip riwayatnya di tabel {@code notifikasi}.
 *
 * <h2>Peta kelas</h2>
 * <ul>
 * <li>{@link ais.delivery.email.sender.MailSender} — mesin produksi. Satu-satunya titik yang
 * dipanggil oleh kode aplikasi lain untuk mengirim email/notifikasi/WA/push. Semua metode publik
 * bersifat statis; tidak ada instance state selain pool thread dan cache dedup yang dibagi
 * seluruh JVM.</li>
 * <li>{@link ais.delivery.email.sender.MailHelper} — composer ZK non-modal yang menempel pada
 * tombol "Lupa Password" di layar login ({@code ais.action.maintenance.LoginAction#onForgotPassword()}).
 * Mencari akun lewat username di lima entitas (Tbmuser, Dosen, Pegawai, Mahasiswa, Siswa),
 * lalu men-<i>dekripsi</i> password tersimpan dan mengirimkannya lewat {@link ais.delivery.email.sender.MailSender#sendMail}.</li>
 * <li>{@link ais.delivery.email.sender.SendEmail}, {@link ais.delivery.email.sender.SimpleMail},
 * {@link ais.delivery.email.sender.CrunchifyJavaMailExample} — <b>kode contoh/pengujian yang tidak
 * pernah dipanggil dari bagian lain aplikasi</b> (diverifikasi lewat pencarian referensi di seluruh
 * {@code src/}; tiap kelas hanya memuat dirinya sendiri). Ditinggalkan dari masa awal proyek sebagai
 * potongan JavaMail API "hello world" per penyedia (SMTP polos, SMTP+auth manual, Gmail). Lihat
 * catatan keamanan di bawah sebelum menyalin pola dari kelas-kelas ini.</li>
 * </ul>
 *
 * <h2>Pola mesin produksi (MailSender)</h2>
 * <p>
 * Setiap permintaan kirim — apa pun kanalnya — SELALU membuat baris {@link ais.database.model.Notifikasi}
 * lebih dahulu (lewat salah satu varian {@code simpanNotif*}) sebagai arsip terpusat dan sumber
 * kebenaran untuk "apa yang sudah dikirim ke siapa", baru kemudian kanal aktual (email SMTP langsung,
 * email lewat Brevo/Sendinblue API, WhatsApp lewat Ultramsg, push lewat server mobile
 * {@code dev.ecampus.id:3000}) dijalankan secara asinkron di atas {@link
 * java.util.concurrent.ThreadPoolExecutor} berukuran tetap (properti sistem {@code mail.pool.size},
 * default 1) dengan {@link java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy} sebagai
 * pengaman tekanan-balik. Rancangan ini menggantikan pola lama {@code new Thread(...).start()} tanpa
 * batas yang pernah menghabiskan pool koneksi database di beban tinggi — lihat komentar sejarah pada
 * konstanta {@code MAIL_POOL_SIZE} di {@link ais.delivery.email.sender.MailSender} untuk kronologi
 * insidennya.
 * </p>
 * <p>
 * Pengiriman email maupun WhatsApp/push selalu tunduk pada saklar konfigurasi bertingkat yang dibaca
 * lewat {@code ais.common.Common#bolehKonfigurasi(String)} / {@code #getKonfigurasi(String, String)}
 * (mis. {@code aktfikan_pengiriman_notif}, {@code aktfikan_pengiriman_email},
 * {@code aktfikan_pengiriman_email_akademik}, {@code aktfikan_pengiriman_email_tagihan},
 * {@code aktfikan_pengiriman_email_menggunakan_sendinblue.com}, {@code aktifkan_reply_chatbot},
 * {@code aktifkan_push_multiple_devices_notif}) sehingga perilaku pengiriman dapat diubah per
 * tenant/instalasi tanpa deploy ulang. Baca sumber {@link ais.delivery.email.sender.MailSender}
 * untuk daftar lengkap kunci konfigurasi yang dipakai beserta nilai default masing-masing.
 * </p>
 *
 * <h2>Pola overload berlapis (berlaku di MailSender dan MailHelper)</h2>
 * <p>
 * Sebagian besar metode publik di paket ini hadir dalam banyak varian overload
 * ({@code sendMail}, {@code sendMailLampiran}, {@code sendMailLampiranAll},
 * {@code sendMailLampiranTagihan}, {@code kirimNotif}, {@code simpanNotif}) yang HANYA berbeda pada
 * kombinasi parameter opsional (lampiran {@code File...}, {@code attachmentsData} berbentuk JSON,
 * flag {@code kirimkankeWa}, {@code PrintStream out} untuk debug). Semua overload pada akhirnya
 * memanggil satu metode privat "kanonik" yang berparameter lengkap (mis. {@code sendMailLampiran}
 * publik → {@code sendMailLampiran(..., PrintStream, ..., JSONArray, boolean, File...)} privat).
 * Javadoc method-level di kelas masing-masing hanya ditulis lengkap pada varian kanonik tersebut;
 * overload lain didokumentasikan ringkas dengan menyebut nilai default yang disisipkan dan
 * {@code @see}/{@code @link} balik ke varian kanonik — pembaca yang butuh detail penuh (kapan
 * notifikasi dibuat, kapan email benar-benar terkirim, urutan penanganan galat) cukup mengikuti
 * tautan tersebut alih-alih membaca penjelasan yang diulang identik di belasan tempat.
 * </p>
 *
 * <h2>Peringatan keamanan — kredensial tertanam</h2>
 * <p>
 * Tiga kelas contoh di paket ini ({@code SendEmail}, {@code SimpleMail},
 * {@code CrunchifyJavaMailExample}) berisi kredensial SMTP/API dalam bentuk teks polos yang
 * ditulis langsung di kode sumber (bukan dibaca dari konfigurasi), termasuk kata sandi akun SMTP
 * dan alamat email pribadi pengembang. Mesin produksi {@link ais.delivery.email.sender.MailSender}
 * TIDAK memiliki masalah ini — kredensialnya selalu dibaca saat runtime lewat
 * {@code Common.getKonfigurasi("default_email_username"/"default_email_password", ...)} — KECUALI
 * satu pengecualian: metode {@code main(String[])} di {@code MailSender} sendiri (potongan uji coba
 * peninggalan, tidak dipanggil aplikasi) juga menanam kunci API Mailgun secara langsung. Karena
 * kelas-kelas contoh maupun {@code main} peninggalan ini tidak pernah dieksekusi oleh aplikasi yang
 * berjalan, dokumentasi ini TIDAK mengubah atau menghapus kredensial tersebut (perubahan semacam itu
 * di luar cakupan pekerjaan dokumentasi dan berpotensi memerlukan rotasi kredensial di sisi
 * penyedia) — namun siapa pun yang menyalin pola dari kelas-kelas ini WAJIB memindahkan kredensial
 * ke konfigurasi runtime dan meninjau apakah kredensial yang sudah terlanjur ter-commit perlu
 * dirotasi.
 * </p>
 *
 * @see ais.database.model.Notifikasi
 * @see ais.common.Common
 */
package ais.delivery.email.sender;
