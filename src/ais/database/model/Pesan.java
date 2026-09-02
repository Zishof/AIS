package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Entity <b>satu baris pesan chat</b> (tabel {@code public.pesan}) — dan inilah <b>mesin chat AIS
 * yang benar-benar berjalan</b>. Menyimpan isi percakapan ({@code isi}, kolom {@code text}), waktu
 * kirim ({@code waktu}), konteks kelas opsional ({@code perkuliahan}), flag siaran
 * ({@code semua}), flag hidup/mati ({@code aktif}), daftar penerima yang sudah membaca
 * ({@code diterimaOleh}), serta — inti rancangannya — <b>empat pasang <i>foreign key</i></b> untuk
 * mengidentifikasi pengirim dan penerima.
 *
 * <h3>PENTING: dua entity chat, hanya SATU yang aktif</h3>
 * <p>Di paket ini ada dua entity yang namanya menyesatkan mirip:</p>
 * <table border="1" cellpadding="4">
 * <tr><th></th><th>{@link ChatMessage} (+ {@code ChatConvertation})</th><th>{@code Pesan} (class ini)</th></tr>
 * <tr><td><b>Status</b></td>
 *     <td><b>YATIM TOTAL</b> — nol pembaca/penulis di seluruh <i>source tree</i>; hanya terdaftar
 *     di {@code hibernate.cfg.xml}. Sisa rancangan 2010 yang tak pernah dituntaskan.</td>
 *     <td><b>AKTIF</b> — dipakai paket {@code ais.action.master.chat} dan dua API perpustakaan.</td></tr>
 * <tr><td><b>Identitas lawan bicara</b></td>
 *     <td>Sepasang {@link String} bebas ({@code from}/{@code to}), tanpa FK.</td>
 *     <td><b>8 relasi ber-FK</b>: 4 sisi pengirim + 4 sisi penerima (lihat di bawah), sehingga
 *     penyaringan "pesan untuk saya dari dia" bisa dilakukan penuh di level SQL.</td></tr>
 * <tr><td><b>Konteks kelas</b></td>
 *     <td>Tidak ada.</td>
 *     <td>{@link #getPerkuliahan()} + {@link #getSemua()} — mendukung forum satu mata kuliah.</td></tr>
 * <tr><td><b>Waktu kirim</b></td>
 *     <td>Tidak ada kolom waktu kirim sama sekali.</td>
 *     <td>{@link #getWaktu()}, terisi otomatis saat objek dibuat.</td></tr>
 * <tr><td><b>Siklus hidup baris</b></td>
 *     <td>Dirancang sebagai arsip permanen berbasis percakapan.</td>
 *     <td><b>Efemeral</b>: pesan japri <b>DIHAPUS</b> setelah dirender ke layar penerima; pesan
 *     siaran hanya ditandai pada {@link #getDiterimaOleh()}.</td></tr>
 * </table>
 * <p>Kesimpulan bagi pemelihara: <b>segala perbaikan/penambahan fitur chat dikerjakan di sini</b>,
 * bukan di {@link ChatMessage}.</p>
 *
 * <h3>Rancangan pengirim/penerima: 4 pasang FK dengan akhiran {@code f}</h3>
 * <p>AIS tidak punya satu tabel "orang" tunggal: seorang pemakai bisa muncul sebagai
 * {@link Tbmuser} (akun umum), {@link Mahasiswa}, {@link Dosen}, atau {@link Pegawai}. Karena itu
 * satu baris pesan menyediakan <b>empat kolom untuk sisi pengirim</b> dan <b>empat kolom kembar
 * untuk sisi penerima</b>. Kolom sisi penerima memakai akhiran huruf {@code f} (dari
 * <i>friend</i>, istilah yang dipakai {@code ChatRoom.getFriend()}):</p>
 * <table border="1" cellpadding="4">
 * <tr><th>Peran</th><th>Pengirim (kolom)</th><th>Penerima (kolom)</th></tr>
 * <tr><td>Akun umum</td><td>{@link #getTbmuser()} ({@code tbmuser})</td><td>{@link #getTbmuserf()} ({@code tbmuserf})</td></tr>
 * <tr><td>Mahasiswa</td><td>{@link #getMahasiswa()} ({@code mahasiswa})</td><td>{@link #getMahasiswaf()} ({@code mahasiswaf})</td></tr>
 * <tr><td>Dosen</td><td>{@link #getDosen()} ({@code dosen})</td><td>{@link #getDosenf()} ({@code dosenf})</td></tr>
 * <tr><td>Pegawai</td><td>{@link #getPegawai()} ({@code pegawai})</td><td>{@link #getPegawaif()} ({@code pegawaif})</td></tr>
 * </table>
 * <p><b>Kolom-kolom itu TIDAK saling eksklusif.</b> Seorang dosen yang juga punya akun
 * {@code Tbmuser} akan mengisi {@code tbmuser} <i>dan</i> {@code dosen} sekaligus (lihat
 * {@code Chatter.addMessage(...)}). Satu-satunya pengecualian yang ditegakkan adalah <b>mahasiswa
 * mengosongkan slot {@code Tbmuser}</b>: {@code Chatter.addMessage(...)} memanggil
 * {@code setTbmuser(null)}/{@code setTbmuserf(null)} begitu {@code mahasiswa}/{@code mahasiswaf}
 * terisi, dan {@link #getTbmuser()} <b>menegakkan ulang</b> invarian itu setiap kali dibaca (lihat
 * peringatan di method tersebut). Akibatnya, kode pembaca harus memakai pola
 * "{@code mahasiswa != null ? mahasiswa : tbmuser}" — persis yang dilakukan {@link #toString()}
 * dan {@code Chatter.renderMessages(...)}.</p>
 * <p>Query penyaringan dibangun sebagai <b>OR dari kolom-kolom yang relevan</b> untuk pemakai
 * berjalan, dengan penjaga {@code Restrictions.sqlRestriction("1!=1")} sebagai nilai awal supaya
 * pemakai tanpa peran apa pun tidak mendapat pesan siapa pun. Pola ini disalin di tiga tempat:
 * {@code Chatter.run()}, {@code ChatUsers.checkPesan(...)}, dan {@code ChatUsers.getPesanDari()}.</p>
 *
 * <h3>Dua mode: japri (1&ndash;1) vs siaran ({@code semua})</h3>
 * <ul>
 * <li><b>Japri</b> ({@code semua = false}) — dipilih bila lawan bicara dipilih eksplisit dari
 * daftar pemakai. Penyaringan: pasangan (pengirim = lawan bicara) DAN (penerima = saya) DAN
 * {@code aktif = true}. Setelah dirender, {@code Chatter.renderMessages(...)} memanggil
 * {@code Common.refreshDelete(session, msg)} sehingga <b>barisnya hilang dari tabel</b>.</li>
 * <li><b>Siaran</b> ({@code semua = true}, tab "Forum Komunikasi") — penyaringan <b>hanya</b>
 * {@code semua = true} + kesamaan {@link #getPerkuliahan()}; <b>tidak ada</b> penyaringan
 * pengirim/penerima dan <b>tidak ada</b> penyaringan {@code aktif}. Baris siaran <b>tidak
 * dihapus</b>; sebagai gantinya id pemakai yang sudah menerimanya di-<i>append</i> ke
 * {@link #getDiterimaOleh()} dalam bentuk {@code "[userId]"}, dan penyaringan berikutnya
 * mengecualikan baris yang {@code diterimaOleh}-nya sudah memuat penanda itu
 * ({@code Restrictions.not(ilike(..., ANYWHERE))}).</li>
 * </ul>
 * <p>{@link #getPerkuliahan()} membagi ruang pesan: {@code null} berarti chat global aplikasi
 * (tombol "Chat" di {@code MainAction}/{@code MainAction2}, halaman
 * {@code /pages/master/chat.zul}), sedangkan nilai terisi berarti forum internal satu kelas.
 * Penyaringannya memakai {@code Restrictions.isNull("perkuliahan")} vs
 * {@code Restrictions.eq("perkuliahan", ...)}, jadi kedua ruang itu benar-benar terpisah.</p>
 *
 * <h3>Alur pemakaian (paket {@code ais.action.master.chat})</h3>
 * <ol>
 * <li>{@code ChatUsers} — layar utama: daftar pemakai online/offline, membuat satu tab per lawan
 * bicara, memanggil {@code checkPesan(...)}/{@code getPesanDari()} untuk memunculkan notifikasi
 * "Ada pesan masuk".</li>
 * <li>{@code ChatWindow} — komponen ZK per tab; {@code onSendMsg()} mengirim isi {@code Textbox}
 * dan langsung merender gelembung pesan milik sendiri.</li>
 * <li>{@code ChatRoom.say(...)} — <b>membangun objek {@code Pesan} baru</b> ({@code isi},
 * {@code tbmuser} pengirim, {@code perkuliahan}, {@code semua}).</li>
 * <li>{@code Chatter} — {@link Thread} <i>server push</i> per tab; {@code addMessage(...)}
 * melengkapi slot peran pengirim/penerima lalu {@code save}; {@code run()} melakukan
 * <i>polling</i> tiap 6 detik; {@code renderMessages(...)} merender lalu menghapus/menandai.</li>
 * <li>{@code ChatUtil.createPesanBox(...)} — merender satu gelembung pesan (avatar + isi + nama
 * dan jam), dengan deduplikasi berbasis {@link #getId()}.</li>
 * </ol>
 *
 * <h3>Pemakai kedua: tabel ini juga dipakai sebagai antrean tiket perpustakaan</h3>
 * <p>Di luar modul chat, {@code ais.action.master.library.modern.LibraryEngagementApi} dan
 * {@code LibraryOperationsApi} <b>menumpang</b> tabel {@code pesan} sebagai antrean layanan
 * anggota: barisnya dibuat dengan {@code semua = false}, tanpa penerima sama sekali, dan
 * {@link #getIsi()} diawali penanda kurung siku
 * ({@code "[ASK_LIBRARIAN][LIBRARY_ID=<id>] subjek — uraian"} atau
 * {@code "[INTERLIBRARY_LOAN]..."}). Konvensinya:</p>
 * <ul>
 * <li>{@link #getAktif()} dipakai sebagai <b>status tiket</b>: {@code true} = masih terbuka,
 * {@code false} = sudah ditindaklanjuti.</li>
 * <li>Penyelesaian tiket meng-<i>append</i> jejak {@code "[APPROVED|REJECTED|CLOSED date=... actor=...] catatan"}
 * ke {@link #getIsi()} (dibatasi 10.000 karakter) — jadi kolom {@code isi} di sini bukan sekadar
 * isi chat, melainkan juga log penyelesaian.</li>
 * <li>Pemilikan perpustakaan disimpan sebagai <b>teks</b> di dalam {@code isi}
 * ({@code [LIBRARY_ID=n]}), sehingga penyaringan per-perpustakaan dilakukan <b>di memori</b>
 * setelah 200 baris teratas ditarik, bukan di SQL.</li>
 * </ul>
 * <p>Barisnya aman dari mesin chat karena tidak punya penerima ({@code tbmuserf}/{@code mahasiswaf}/
 * {@code dosenf}/{@code pegawaif} semuanya {@code null}) sehingga tidak pernah cocok dengan
 * penyaring japri, dan {@code semua = false} membuatnya tidak muncul di forum. Meski begitu,
 * <b>pencampuran dua domain yang tak berhubungan dalam satu tabel</b> patut dicatat: menambah
 * penyaring baru di modul chat berpotensi ikut menyeret baris tiket perpustakaan, dan sebaliknya.</p>
 *
 * <h3 id="keamanan">CATATAN KEAMANAN — bukti konkret IDOR baca-apa-saja {@code /Api dataRinci}</h3>
 * <p><b>Entity ini adalah contoh paling gamblang dari dampak temuan {@code /Api dataRinci}</b>,
 * karena isinya adalah percakapan pribadi. Rantai buktinya sudah diverifikasi langsung:</p>
 * <ol>
 * <li>Class ini <b>terdaftar</b> di {@code src/hibernate.cfg.xml} sebagai
 * {@code <mapping class="ais.database.model.Pesan" />} (baris 867), jadi Hibernate mengenalnya
 * sebagai entity yang bisa di-query lewat nama kelas.</li>
 * <li>{@code ais.action.servlet.api.ElearningApiUtil#dataRinci(...)} (terdaftar di
 * {@code ApiRouteRegistry} sebagai aksi {@code "dataRinci"} pada servlet {@code /Api}) hanya
 * memeriksa bahwa token <b>ada dan valid</b> ({@code ApiUtil.currentUser(...)} mengembalikan
 * {@code Tbmuser} dengan {@code userId} tidak {@code null}) — <b>tidak ada satu pun pemeriksaan
 * kepemilikan atau peran</b>.</li>
 * <li>Nama kelas diambil <b>mentah dari klien</b>: {@code Class.forName(request.getString("class"))},
 * dan id juga dari klien: {@code Long.parseLong(...request.get("id"))}. Query yang dijalankan
 * hanyalah {@code createCriteria(clazz).add(Restrictions.idEq(id))} — <b>tanpa penyaring
 * tambahan apa pun</b>.</li>
 * <li>Hasilnya diserialisasi lewat {@code Common.insertProperty(clazz, ..., deep)} dengan
 * kedalaman default <b>6</b> dan <b>bisa dinaikkan klien</b> lewat parameter {@code deep}.</li>
 * </ol>
 * <p>Artinya: <b>pemegang token AIS mana pun</b> (mahasiswa, siswa, penduduk — peran apa saja)
 * dapat mengirim {@code {"class":"ais.database.model.Pesan","id":<n>}} dan menerima balik
 * <b>isi chat pribadi orang lain</b> beserta <b>graf identitas kedua belah pihak sedalam 6
 * tingkat</b> (mahasiswa &rarr; jurusan &rarr; fakultas, dosen, pegawai, dan seterusnya) — cukup
 * dengan menaikkan {@code id} satu per satu. Ini memperkuat task keamanan yang sudah ada untuk
 * {@code /Api dataRinci}; <b>bukan temuan terpisah</b>.</p>
 * <p><b>Faktor peringan (parsial):</b> baris japri dihapus segera setelah penerima merendernya,
 * sehingga jendela pembacaan hanya mencakup pesan yang <b>belum sempat terkirim</b> (penerima
 * offline atau tabnya belum dibuka) — namun <i>polling</i>-nya 6 detik, jadi selalu ada antrean
 * hidup pada sistem yang ramai. <b>Baris siaran tidak pernah dihapus sama sekali</b>, jadi
 * seluruh riwayat Forum Komunikasi terbaca penuh.</p>
 * <p><b>Faktor pemberat yang jauh lebih besar:</b> class ini beranotasi {@link Audited}
 * (Hibernate Envers) <b>dan</b> {@code hibernate.cfg.xml} menyetel
 * {@code org.hibernate.envers.store_data_at_delete = true} (baris 176). Konsekuensinya
 * <b>penghapusan baris japri oleh {@code Chatter.renderMessages(...)} sama sekali TIDAK
 * menghapus isi pesannya</b> — seluruh isi percakapan tersalin permanen ke tabel audit
 * {@code new_audit.pesan__audit} (sufiks {@code __audit}, skema {@code new_audit}), lengkap
 * dengan revisi {@code ADD} dan {@code DEL}. "Chat yang hilang dari layar" pada kenyataannya
 * adalah <b>arsip percakapan permanen yang tak pernah dipangkas</b>.</p>
 * <p><b>Catatan positif:</b> jalur ZK chat sendiri ({@code ChatUsers}/{@code Chatter}) menegakkan
 * <i>scoping</i> kepemilikan dengan benar di level SQL — pemakai hanya bisa menarik pesan yang
 * memang ditujukan kepadanya. Tidak ada layar "PesanAction" atau CRUD master lama untuk tabel
 * ini (sudah dicari; tidak ada), sehingga <b>satu-satunya jalur baca tanpa otorisasi adalah
 * endpoint reflektif generik</b>, bukan UI-nya.</p>
 *
 * <h3>Relasi dengan {@code GeneralValueObject}</h3>
 * <p>Class ini turunan langsung {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa — Hibernate
 * <b>tidak</b> memetakan properti induknya. Karena itu deklarasi ULANG {@link #id},
 * {@link #oleh}, {@link #olehId} dan {@link #tanggal_dirubah} di sini <b>bukan bug atau duplikasi
 * ceroboh</b>, melainkan keharusan teknis supaya keempat kolom itu ikut terpetakan.
 * Konsekuensinya field-field tersebut <b>membayangi (shadow)</b> field senama milik induk; yang
 * terbaca dari luar selalu versi milik {@code Pesan} ini. Sebaliknya, properti induk yang
 * <b>tidak</b> dideklarasikan ulang di sini — {@code nama}, {@code kode}, {@code keterangan},
 * {@code aktif} milik induk — <b>tidak terpetakan</b> ke kolom; khusus {@code aktif}, class ini
 * mendeklarasikan field {@link #aktif} sendiri sehingga tetap tersimpan.</p>
 * <p>Seluruh getter relasi memanggil {@code check(...)} warisan induk untuk meresolusi proxy
 * <i>lazy</i> sebelum objeknya dikembalikan — lihat
 * {@link ais.database.model.GeneralValueObject#check(Object)}.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ol>
 * <li><b>Identitas &amp; penyajian:</b> {@link #getId()} (PK {@code IDENTITY}),
 * {@link #toString()}, konstruktor {@link #Pesan()} dan {@link #Pesan(String, Tbmuser)}.</li>
 * <li><b>Isi pesan:</b> {@link #getIsi()} (kolom {@code text}), {@link #getWaktu()}.</li>
 * <li><b>Pengirim (4 slot peran):</b> {@link #getTbmuser()}, {@link #getMahasiswa()},
 * {@link #getDosen()}, {@link #getPegawai()}.</li>
 * <li><b>Penerima (4 slot peran, akhiran {@code f}):</b> {@link #getTbmuserf()},
 * {@link #getMahasiswaf()}, {@link #getDosenf()}, {@link #getPegawaif()}.</li>
 * <li><b>Konteks &amp; status:</b> {@link #getPerkuliahan()}, {@link #getSemua()},
 * {@link #getAktif()}, {@link #getDiterimaOleh()}.</li>
 * <li><b>Jejak audit (deklarasi ulang milik induk):</b> {@link #getOleh()},
 * {@link #getOlehId()}, {@link #getTanggal_dirubah()}, kait {@link #onUpdate()}.</li>
 * </ol>
 * <p>Tidak ada method bisnis, tidak ada method query statis, tidak ada validasi, dan tidak ada
 * konstanta status di class ini. Seluruh logika chat berada di paket
 * {@code ais.action.master.chat}; entity ini murni wadah data.</p>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui pemelihara</h3>
 * <p><b>Verifikasi pola berulang (dilakukan langsung atas isi file ini, bukan diasumsikan dari
 * file lain):</b></p>
 * <ul>
 * <li><b>Getter yang menulis balik ke field: ADA, dua buah.</b>
 *   <ul>
 *   <li>{@link #getTbmuser()} — <b>destruktif terhadap field</b>: bila {@link #mahasiswa} terisi,
 *   getter ini <b>menyetel {@code tbmuser = null}</b> sebelum mengembalikannya. Karena pemetaan
 *   Hibernate di class ini memakai <i>property access</i> (anotasi ditempel pada getter), nilai
 *   yang dibaca Hibernate saat <i>flush</i> adalah hasil getter — sehingga sekadar <b>membaca</b>
 *   objek terkelola bisa memicu {@code UPDATE pesan SET tbmuser = NULL}. Baca peringatan lengkap
 *   di method itu.</li>
 *   <li>Seluruh getter relasi ({@link #getMahasiswa()}, {@link #getDosen()},
 *   {@link #getPegawai()}, {@link #getTbmuserf()}, {@link #getMahasiswaf()},
 *   {@link #getDosenf()}, {@link #getPegawaif()}, {@link #getPerkuliahan()}) menulis balik hasil
 *   {@code check(...)} ke field-nya ({@code x = check(x); return x;}). Ini <i>write-back</i> yang
 *   tidak berbahaya — hanya mengganti proxy dengan objek nyata, nilai logisnya sama.</li>
 *   </ul></li>
 * <li><b>Getter yang menutup {@code Session} Hibernate: TIDAK ADA.</b> Tidak satu pun method di
 * file ini menyentuh {@code HibernateUtil}. Penutupan sesi seluruhnya dikerjakan pemanggil
 * ({@code Chatter}/{@code ChatUsers}/{@code LibraryEngagementApi}).</li>
 * <li><b>Getter destruktif terhadap baris DB (menghapus/mengubah baris lain): TIDAK ADA.</b>
 * Penghapusan baris pesan japri terjadi di {@code Chatter.renderMessages(...)}, bukan di entity.
 * Namun perhatikan {@link #getTbmuser()} di atas — destruktif terhadap <i>field</i> baris ini
 * sendiri, dan {@link #getDiterimaOleh()} yang menormalkan {@code null} menjadi {@code ""}
 * sehingga baris ber-{@code diterimaOleh} {@code NULL} akan diam-diam ter-{@code UPDATE} menjadi
 * string kosong pada <i>flush</i> berikutnya.</li>
 * </ul>
 * <p><b>Kuirk lain yang tercatat (tidak diperbaiki di sini — hanya didokumentasikan):</b></p>
 * <ul>
 * <li>{@link #setOleh(String)} dan {@link #setOlehId(String)} <b>mengabaikan diam-diam</b>
 * argumen {@code null}/kosong, jadi jejak audit tidak bisa dikosongkan sekali sudah terisi.</li>
 * <li>{@link #getSemua()} dan {@link #getAktif()} bertipe {@link Boolean} (bisa {@code null}).
 * {@code Chatter.renderMessages(...)} memakai {@code if (msg.getSemua())} — <i>auto-unboxing</i>
 * yang akan {@code NullPointerException} bila ada baris dengan {@code semua} {@code NULL} di DB
 * (baris yang dibuat lewat jalur non-Java atau ditulis endpoint generik). Nilai awal Java-nya
 * memang {@code false}/{@code true}, tapi itu tidak menjamin isi kolomnya.</li>
 * <li>{@link #getDiterimaOleh()} tumbuh <b>tanpa batas</b>: setiap penerima siaran menambah
 * {@code "[userId]"} ke kolom {@code text} yang sama. Untuk forum berisi ribuan pemakai, satu
 * baris pesan bisa menyimpan daftar puluhan ribu karakter — dan daftar itu sekaligus merupakan
 * <b>tanda-baca (read receipt) semua orang</b> yang ikut terekspos lewat endpoint reflektif.</li>
 * <li>Penyaringan {@code Restrictions.not(ilike("diterimaOleh", "[" + userId + "]", ANYWHERE))}
 * memasukkan {@code userId} <b>langsung sebagai pola {@code LIKE}</b>. Bila ada {@code userId}
 * yang mengandung {@code %} atau {@code _}, karakter itu bekerja sebagai <i>wildcard</i> dan
 * pemakai tersebut bisa dianggap "sudah menerima" pesan yang belum dibacanya.</li>
 * <li>{@link #getId()} dipetakan dengan {@code insertable = false} — konsisten dengan
 * {@code IDENTITY}: nilai PK selalu dibangkitkan basis data.</li>
 * <li>{@code toString()} membaca <b>field langsung</b>, bukan getter, sehingga bebas dari efek
 * samping {@link #getTbmuser()} — tetapi juga tidak meresolusi proxy <i>lazy</i>, sehingga
 * hasilnya bisa berupa representasi proxy pada objek yang belum terinisialisasi.</li>
 * <li>Komentar {@code "Bank generated by hbm2java"} pada Javadoc asli class ini adalah
 * <b>sisa salin-tempel</b> dari {@link Bank} (sumber generator yang dibajak puluhan entity lain
 * di paket ini) — sama sekali tidak ada hubungannya dengan perbankan.</li>
 * </ul>
 *
 * @see ChatMessage
 * @see ais.database.model.GeneralValueObject
 * @see Perkuliahan
 * @see Tbmuser
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pesan")

public class Pesan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, diwarisi lewat {@link java.io.Serializable} pada
	 * {@link ais.database.model.GeneralValueObject}. Nilainya tetap agar objek yang pernah
	 * di-serialisasi (mis. tersimpan di sesi ZK) tetap kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key baris pesan; dibangkitkan basis data ({@code IDENTITY}). Lihat {@link #getId()}. */
	private Long id;

	/**
	 * Nama pemakai terakhir yang menyimpan/mengubah baris ini (jejak audit). Deklarasi ULANG
	 * properti {@code GeneralValueObject} — wajib agar terpetakan Hibernate. Lihat {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * Id pemakai terakhir yang menyimpan/mengubah baris ini (jejak audit). Deklarasi ULANG
	 * properti {@code GeneralValueObject}. Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pemakai terakhir yang mengubah baris ini (jejak audit).
	 *
	 * @return id pemakai; {@code null} bila baris belum pernah melewati
	 *         {@code AuditTimestampInterceptor} maupun pengisian manual
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pemakai pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> setter ini <b>mengabaikan diam-diam</b> argumen {@code null} maupun
	 * string yang hanya berisi spasi — nilai lama dipertahankan dan tidak ada indikasi kegagalan.
	 * Jadi jejak audit hanya bisa ditimpa dengan nilai berisi, tidak bisa dikosongkan.</p>
	 *
	 * @param olehId id pemakai; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pemakai pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> sama seperti {@link #setOlehId(String)}, argumen {@code null}/kosong
	 * <b>diabaikan diam-diam</b>.</p>
	 *
	 * @param oleh nama pemakai; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pemakai terakhir yang mengubah baris ini (jejak audit).
	 *
	 * @return nama pemakai; {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} sekaligus deklarasi field {@link #tanggal_dirubah} (keduanya
	 * ditulis pada satu baris fisik oleh penyisip audit otomatis — jangan dipisah agar diff
	 * lintas entity tetap seragam).
	 *
	 * <p>{@code onUpdate()} dipanggil <b>otomatis oleh Hibernate/JPA</b> tepat sebelum baris ini
	 * di-{@code UPDATE}, lalu mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang menyegarkan
	 * {@link #tanggal_dirubah} serta {@link #oleh}/{@link #olehId} dari pengguna sesi berjalan.
	 * Tidak ada padanan {@code @PrePersist}, sehingga jejak audit baris <b>baru</b> hanya
	 * mengandalkan nilai awal field.</p>
	 *
	 * <p>Untuk entity ini kait tersebut praktis hanya aktif pada dua skenario: penandaan
	 * {@link #getDiterimaOleh()} untuk pesan siaran, dan penyelesaian tiket perpustakaan yang
	 * mengubah {@link #getAktif()}/{@link #getIsi()}. Pesan japri umumnya lahir lalu langsung
	 * dihapus tanpa pernah di-{@code UPDATE}.</p>
	 *
	 * <p>Field {@link #tanggal_dirubah} sendiri diinisialisasi seketika saat objek dibuat dengan
	 * {@code ais.ui.util.WaktuUtil.getDate()} (waktu server, menghormati penyetelan zona/offset
	 * aplikasi), bukan {@code new Date()} langsung.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 *
	 * <p>Normalnya diisi otomatis oleh {@link #onUpdate()}; pemanggilan manual hanya dipakai saat
	 * memigrasikan/menyalin data.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code TIMESTAMP}).
	 *
	 * <p><b>Jangan dikelirukan dengan waktu kirim pesan</b> — untuk itu pakai
	 * {@link #getWaktu()}. Pada pesan yang tak pernah di-{@code UPDATE}, kedua nilai kebetulan
	 * sama karena sama-sama diinisialisasi {@code WaktuUtil.getDate()} saat objek dibuat.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat lewat
	 *         konstruktor Java
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas untuk log dan <i>debug</i>, berbentuk
	 * {@code "<id>-<pengirim>-<penerima>-<isi>"}.
	 *
	 * <p>Pengirim dan penerima dipilih dengan pola "mahasiswa didahulukan": bila slot mahasiswa
	 * terisi, itulah yang ditampilkan; selain itu dipakai slot {@link Tbmuser}. Slot dosen dan
	 * pegawai <b>tidak ikut ditampilkan</b>.</p>
	 *
	 * <p><b>Non-obvious:</b> method ini membaca <b>field secara langsung</b>, bukan lewat getter.
	 * Akibatnya ia (a) bebas dari efek samping {@link #getTbmuser()}, tetapi (b) <b>tidak
	 * meresolusi proxy <i>lazy</i></b> lewat {@code check(...)}, sehingga pada objek yang belum
	 * terinisialisasi hasilnya bisa berupa representasi proxy Hibernate, atau memicu
	 * {@code LazyInitializationException} bila sesinya sudah tertutup.</p>
	 *
	 * <p><b>Perhatian privasi:</b> keluarannya memuat <b>isi pesan utuh</b>. Jangan dipakai pada
	 * log yang dibagikan atau pesan kesalahan yang bisa dilihat pengguna lain.</p>
	 *
	 * @return string ringkas berisi id, pengirim, penerima, dan isi pesan
	 */
	public String toString() {
		return id + "-" + (mahasiswa == null ? tbmuser : mahasiswa) + "-" + (mahasiswaf == null ? tbmuserf : mahasiswaf)
				+ "-" + isi;
	}

	/** Badan/isi pesan (kolom {@code text}, panjang bebas). Lihat {@link #getIsi()}. */
	private String isi;

	/**
	 * Waktu kirim pesan, terisi otomatis saat objek dibuat memakai
	 * {@code ais.ui.util.WaktuUtil.getDate()} (waktu server). Lihat {@link #getWaktu()}.
	 */
	private Date waktu = ais.ui.util.WaktuUtil.getDate();

	/** Slot pengirim untuk akun umum. Dikosongkan bila {@link #mahasiswa} terisi. Lihat {@link #getTbmuser()}. */
	private Tbmuser tbmuser;

	/** Slot pengirim bila pengirimnya seorang mahasiswa. Lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;

	/** Slot pengirim bila pengirimnya seorang dosen (dapat terisi bersamaan dengan {@link #tbmuser}). Lihat {@link #getDosen()}. */
	private Dosen dosen;

	/** Slot pengirim bila pengirimnya seorang pegawai (dapat terisi bersamaan dengan {@link #tbmuser}). Lihat {@link #getPegawai()}. */
	private Pegawai pegawai;

	/**
	 * Konteks mata kuliah/kelas tempat pesan ini hidup. {@code null} berarti chat global
	 * aplikasi; nilai terisi berarti forum internal satu kelas. Lihat {@link #getPerkuliahan()}.
	 */
	private Perkuliahan perkuliahan;

	/** Slot penerima untuk akun umum (akhiran {@code f} = <i>friend</i>). Lihat {@link #getTbmuserf()}. */
	private Tbmuser tbmuserf;

	/** Slot penerima bila penerimanya seorang mahasiswa. Lihat {@link #getMahasiswaf()}. */
	private Mahasiswa mahasiswaf;

	/** Slot penerima bila penerimanya seorang dosen. Lihat {@link #getDosenf()}. */
	private Dosen dosenf;

	/** Slot penerima bila penerimanya seorang pegawai. Lihat {@link #getPegawaif()}. */
	private Pegawai pegawaif;

	/**
	 * Penanda pesan japri masih hidup; nilai awal {@code true}. Dipakai ulang oleh API
	 * perpustakaan sebagai status tiket ({@code false} = sudah ditindaklanjuti).
	 * Lihat {@link #getAktif()}.
	 */
	private Boolean aktif = true;

	/**
	 * Penanda mode siaran (forum), nilai awal {@code false}. {@code true} berarti pesan dibaca
	 * semua peserta ruang dan barisnya tidak dihapus. Lihat {@link #getSemua()}.
	 */
	private Boolean semua = false;

	/**
	 * Daftar penerima pesan siaran yang sudah merendernya, berbentuk rangkaian {@code "[userId]"}
	 * yang di-<i>append</i> tanpa pemisah. Lihat {@link #getDiterimaOleh()}.
	 */
	private String diterimaOleh;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA, sekaligus konstruktor yang dipakai
	 * kode aplikasi ({@code ChatRoom.say(...)} dan {@code LibraryEngagementApi}).
	 *
	 * <p>Field {@link #waktu}, {@link #tanggal_dirubah}, {@link #aktif} ({@code true}) dan
	 * {@link #semua} ({@code false}) sudah terisi nilai awal lewat inisialisasi field.</p>
	 */
	public Pesan() {
	}

	/**
	 * Konstruktor ringkas: isi pesan + pengirim sebagai akun umum.
	 *
	 * <p><b>Status: tidak dipakai kode produksi mana pun</b> (satu-satunya rujukan tersisa adalah
	 * baris yang sudah dikomentari di {@code ChatRoom.unsubscribe(...)}, peninggalan contoh chat
	 * ZK asli). Alur nyata memakai {@link #Pesan()} lalu memanggil setter satu per satu.</p>
	 *
	 * <p><b>Perhatikan:</b> konstruktor ini <b>tidak</b> mengisi slot peran turunan
	 * ({@code dosen}/{@code pegawai}/{@code mahasiswa}) maupun sisi penerima, sehingga objek yang
	 * dihasilkan belum siap disimpan lewat alur chat — pelengkapan itu tugas
	 * {@code Chatter.addMessage(...)}.</p>
	 *
	 * @param isi     badan pesan
	 * @param tbmuser akun pengirim
	 */
	public Pesan(String isi, Tbmuser tbmuser) {
		this.isi = isi;
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan primary key baris pesan.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}) dan dipetakan {@code insertable = false},
	 * sehingga nilainya baru terisi setelah {@code save}/{@code flush}. Nilai ini juga dipakai
	 * {@code ChatUtil.createPesanBox(...)} sebagai kunci deduplikasi render.</p>
	 *
	 * @return id baris; {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key baris pesan.
	 *
	 * <p>Tidak dipakai alur normal (PK dibangkitkan basis data); hanya relevan untuk pengujian
	 * atau pemuatan data buatan.</p>
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan badan/isi pesan apa adanya (kolom {@code text}, tanpa batas panjang skema).
	 *
	 * <p><b>Tidak ada sanitasi/pemangkasan apa pun di sini.</b> Nilainya dirender
	 * {@code ChatUtil.createPesanBox(...)} lewat {@code org.zkoss.zul.Label} yang meng-<i>escape</i>
	 * HTML, sehingga risiko XSS pada jalur ZK rendah — tetapi konsumen lain (API JSON,
	 * endpoint reflektif) menerima teks mentah.</p>
	 *
	 * <p><b>Pemakaian ganda:</b> pada baris milik API perpustakaan, kolom ini bukan sekadar isi
	 * chat melainkan format bertanda: {@code "[ASK_LIBRARIAN][LIBRARY_ID=n] subjek — uraian"}
	 * yang kemudian di-<i>append</i> jejak penyelesaian {@code "[APPROVED date=... actor=...] catatan"}.
	 * Lihat catatan "Pemakai kedua" pada Javadoc kelas.</p>
	 *
	 * @return isi pesan; dapat {@code null} (kolom {@code nullable})
	 */
	@Column(name = "isi", nullable = true, columnDefinition = "text")
	public String getIsi() {
		return this.isi;
	}

	/**
	 * Menyetel badan/isi pesan.
	 *
	 * @param isi teks pesan (boleh {@code null})
	 */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/**
	 * Mengembalikan waktu kirim pesan (kolom {@code TIMESTAMP}).
	 *
	 * <p>Terisi otomatis saat objek dibuat ({@code WaktuUtil.getDate()}), jadi mencerminkan
	 * <b>waktu pembuatan objek di server</b>, bukan waktu klien.</p>
	 *
	 * <p><b>Kuirk yang perlu diketahui:</b> jam yang tampil di gelembung pesan <b>tidak</b>
	 * berasal dari kolom ini — {@code Chatter.renderMessages(...)} dan
	 * {@code ChatWindow.onSendMsg()} meneruskan {@code WaktuUtil.getDate()} (waktu <b>render</b>)
	 * ke {@code ChatUtil.createPesanBox(...)}. Untuk pesan yang tertunda karena penerima sedang
	 * offline, jam yang terlihat penerima bisa jauh lebih baru daripada nilai kolom ini.</p>
	 *
	 * @return waktu kirim
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu;
	}

	/**
	 * Menyetel waktu kirim pesan.
	 *
	 * @param waktu waktu kirim
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan akun {@link Tbmuser} <b>pengirim</b> pesan.
	 *
	 * <p><b>PERINGATAN — getter ini punya efek samping yang menghapus data.</b> Bila slot
	 * {@link #mahasiswa} terisi, method ini <b>menyetel field {@code tbmuser} menjadi
	 * {@code null}</b> sebelum mengembalikannya. Tujuannya menegakkan invarian rancangan "pesan
	 * dari mahasiswa diidentifikasi lewat slot mahasiswa, bukan slot akun umum" — invarian yang
	 * sama juga dipaksakan {@code Chatter.addMessage(...)} lewat {@code setTbmuser(null)}.</p>
	 * <p><b>Konsekuensi teknis:</b> pemetaan Hibernate class ini memakai <i>property access</i>
	 * (anotasi ditempel pada getter), sehingga nilai yang dibaca Hibernate saat <i>dirty check</i>
	 * / <i>flush</i> adalah hasil getter ini. Untuk objek terkelola yang barisnya di DB
	 * <b>terlanjur</b> mengisi {@code tbmuser} <i>dan</i> {@code mahasiswa} sekaligus, sekadar
	 * <b>membaca</b> objek dapat memicu {@code UPDATE pesan SET tbmuser = NULL} — perubahan data
	 * permanen yang dipicu operasi baca. Ini kasus nyata pola "getter yang menulis balik" yang
	 * berulang di paket entity AIS.</p>
	 * <p>Setelah pemeriksaan itu, nilai dilewatkan {@code check(...)} untuk meresolusi proxy
	 * <i>lazy</i>.</p>
	 *
	 * @return akun pengirim; {@code null} bila pengirimnya mahasiswa, atau bila memang tidak
	 *         diisi (mis. baris tiket perpustakaan lama)
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		if (mahasiswa != null) {
			tbmuser = null;
		}
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Menyetel akun {@link Tbmuser} pengirim pesan.
	 *
	 * <p>Dipanggil {@code ChatRoom.say(...)} saat pesan dibuat, dan dipanggil ulang dengan
	 * {@code null} oleh {@code Chatter.addMessage(...)} begitu diketahui pengirimnya mahasiswa.</p>
	 *
	 * @param tbmuser akun pengirim (boleh {@code null})
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan {@link Mahasiswa} <b>pengirim</b> pesan.
	 *
	 * <p>Terisi oleh {@code Chatter.addMessage(...)} bila akun pengirim tertaut ke data
	 * mahasiswa. Nilai ini yang dipakai {@code Chatter.renderMessages(...)} untuk memutuskan
	 * jalur render (avatar mahasiswa vs avatar akun), dan dipakai {@code ChatUsers.getPesanDari()}
	 * untuk menyusun daftar "ada pesan masuk dari" ketika pengirimnya mahasiswa.</p>
	 * <p><i>Write-back</i> {@code check(...)} yang tidak berbahaya: mengganti proxy dengan objek
	 * nyata tanpa mengubah nilai logis.</p>
	 *
	 * @return mahasiswa pengirim, atau {@code null} bila pengirim bukan mahasiswa
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menyetel {@link Mahasiswa} pengirim pesan.
	 *
	 * <p>Mengisi slot ini secara efektif <b>membatalkan</b> slot {@link #getTbmuser()}, karena
	 * getter tersebut mengosongkannya saat dibaca.</p>
	 *
	 * @param mahasiswa mahasiswa pengirim (boleh {@code null})
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan {@link Dosen} <b>pengirim</b> pesan.
	 *
	 * <p>Diisi {@code Chatter.addMessage(...)} bila akun pengirim tertaut ke data dosen; berbeda
	 * dari kasus mahasiswa, slot {@link #getTbmuser()} <b>tetap terisi</b> berdampingan dengan
	 * slot ini. Dipakai penyaring japri {@code Restrictions.eq("dosen", ...)} agar pesan dari
	 * seorang dosen tetap ketemu walau akunnya berganti.</p>
	 *
	 * @return dosen pengirim, atau {@code null} bila pengirim bukan dosen
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Menyetel {@link Dosen} pengirim pesan.
	 *
	 * @param dosen dosen pengirim (boleh {@code null})
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan {@link Pegawai} <b>pengirim</b> pesan.
	 *
	 * <p>Diisi {@code Chatter.addMessage(...)} bila akun pengirim tertaut ke data pegawai; sama
	 * seperti slot dosen, dapat terisi berdampingan dengan {@link #getTbmuser()}.</p>
	 *
	 * @return pegawai pengirim, atau {@code null} bila pengirim bukan pegawai
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menyetel {@link Pegawai} pengirim pesan.
	 *
	 * @param pegawai pegawai pengirim (boleh {@code null})
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan akun {@link Tbmuser} <b>penerima</b> pesan (akhiran {@code f} =
	 * <i>friend</i>).
	 *
	 * <p>Diisi {@code Chatter.addMessage(...)} dari {@code ChatRoom.getFriend()} — lawan bicara
	 * tab yang sedang aktif — lalu dikosongkan lagi bila lawan bicara itu ternyata mahasiswa.
	 * Kolom inilah kunci penyaringan "pesan untuk saya" pada {@code Chatter.run()},
	 * {@code ChatUsers.checkPesan(...)} dan {@code ChatUsers.getPesanDari()}.</p>
	 *
	 * <p><b>Beda dengan {@link #getTbmuser()}:</b> getter sisi penerima ini <b>tidak</b>
	 * mengosongkan dirinya berdasarkan {@link #mahasiswaf}; pembersihannya hanya terjadi sekali
	 * di {@code Chatter.addMessage(...)}. Jadi baris yang dibuat lewat jalur lain bisa mengisi
	 * {@code tbmuserf} dan {@code mahasiswaf} sekaligus dan tetap bertahan seperti itu —
	 * asimetri yang tidak disengaja antara sisi pengirim dan sisi penerima.</p>
	 *
	 * @return akun penerima, atau {@code null} (penerimanya mahasiswa, atau pesan siaran/tiket
	 *         yang memang tanpa penerima)
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuserf", nullable = true)
	public Tbmuser getTbmuserf() {
		tbmuserf = check(tbmuserf);
		return tbmuserf;
	}

	/**
	 * Menyetel akun {@link Tbmuser} penerima pesan.
	 *
	 * @param tbmuserf akun penerima (boleh {@code null})
	 */
	public void setTbmuserf(Tbmuser tbmuserf) {
		this.tbmuserf = tbmuserf;
	}

	/**
	 * Mengembalikan {@link Mahasiswa} <b>penerima</b> pesan.
	 *
	 * <p>Diisi {@code Chatter.addMessage(...)} bila lawan bicara adalah mahasiswa (sekaligus
	 * mengosongkan {@link #tbmuserf}). Dipakai penyaring {@code Restrictions.eq("mahasiswaf", ...)}
	 * pada ketiga query chat.</p>
	 *
	 * @return mahasiswa penerima, atau {@code null} bila penerima bukan mahasiswa
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswaf", nullable = true)
	public Mahasiswa getMahasiswaf() {
		mahasiswaf = check(mahasiswaf);
		return mahasiswaf;
	}

	/**
	 * Menyetel {@link Mahasiswa} penerima pesan.
	 *
	 * @param mahasiswaf mahasiswa penerima (boleh {@code null})
	 */
	public void setMahasiswaf(Mahasiswa mahasiswaf) {
		this.mahasiswaf = mahasiswaf;
	}

	/**
	 * Mengembalikan {@link Dosen} <b>penerima</b> pesan.
	 *
	 * <p>Diisi {@code Chatter.addMessage(...)} bila lawan bicara tertaut ke data dosen; dapat
	 * terisi berdampingan dengan {@link #getTbmuserf()}.</p>
	 *
	 * @return dosen penerima, atau {@code null} bila penerima bukan dosen
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosenf", nullable = true)
	public Dosen getDosenf() {
		dosenf = check(dosenf);
		return dosenf;
	}

	/**
	 * Menyetel {@link Dosen} penerima pesan.
	 *
	 * @param dosenf dosen penerima (boleh {@code null})
	 */
	public void setDosenf(Dosen dosenf) {
		this.dosenf = dosenf;
	}

	/**
	 * Mengembalikan {@link Pegawai} <b>penerima</b> pesan.
	 *
	 * <p>Diisi {@code Chatter.addMessage(...)} bila lawan bicara tertaut ke data pegawai; dapat
	 * terisi berdampingan dengan {@link #getTbmuserf()}.</p>
	 *
	 * @return pegawai penerima, atau {@code null} bila penerima bukan pegawai
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawaif", nullable = true)
	public Pegawai getPegawaif() {
		pegawaif = check(pegawaif);
		return pegawaif;
	}

	/**
	 * Menyetel {@link Pegawai} penerima pesan.
	 *
	 * @param pegawaif pegawai penerima (boleh {@code null})
	 */
	public void setPegawaif(Pegawai pegawaif) {
		this.pegawaif = pegawaif;
	}

	/**
	 * Mengembalikan penanda pesan japri masih hidup.
	 *
	 * <p><b>Dua makna berbeda tergantung asal barisnya:</b></p>
	 * <ul>
	 * <li><b>Modul chat:</b> penyaring japri selalu menambahkan {@code Restrictions.eq("aktif", true)},
	 * jadi baris ber-{@code aktif} {@code false} tidak akan pernah terkirim ke penerima. Karena
	 * baris japri dihapus setelah dirender, praktis nilainya selalu {@code true} selama hidupnya
	 * — mekanisme "matikan pesan tanpa menghapus" ini <b>tidak dipakai UI mana pun</b>.
	 * Penyaring siaran ({@code semua = true}) <b>sama sekali tidak memeriksa</b> kolom ini.</li>
	 * <li><b>API perpustakaan:</b> dipakai sebagai <b>status tiket</b> — {@code true} berarti
	 * permintaan masih terbuka, {@code false} berarti sudah ditindaklanjuti
	 * ({@code LibraryOperationsApi.serviceResolve(...)} menyetelnya {@code false} sekaligus
	 * memakainya sebagai penjaga agar tiket tidak diproses dua kali).</li>
	 * </ul>
	 * <p>Nilai awal Java {@code true}, tetapi tipenya {@link Boolean} sehingga kolom di DB masih
	 * bisa {@code NULL} untuk baris yang ditulis di luar jalur Java.</p>
	 *
	 * @return {@code true} bila aktif/terbuka, {@code false} bila dinonaktifkan/selesai,
	 *         {@code null} bila kolom tidak terisi
	 */
	public Boolean getAktif() {
		return aktif;
	}

	/**
	 * Menyetel penanda aktif/status tiket.
	 *
	 * @param aktif {@code true} aktif/terbuka, {@code false} nonaktif/selesai
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan {@link Perkuliahan} (kelas/mata kuliah) tempat pesan ini hidup.
	 *
	 * <p>Nilai ini <b>membagi ruang pesan menjadi dua yang saling terpisah rapat</b>:</p>
	 * <ul>
	 * <li>{@code null} &mdash; chat global aplikasi, dibuka lewat tombol "Chat" pada
	 * {@code MainAction}/{@code MainAction2} yang memuat {@code /pages/master/chat.zul}. Hanya
	 * ruang inilah yang memunculkan notifikasi judul halaman "PESAN MASUK - ..." dan mengubah
	 * warna tombol Chat.</li>
	 * <li>terisi &mdash; forum internal satu kelas; {@code ChatUsers} dikonstruksi dengan
	 * {@link Perkuliahan} dan daftar lawan bicaranya dibatasi peserta
	 * ({@code Detailperkuliahan}) serta dosen pengampu kelas tersebut.</li>
	 * </ul>
	 * <p>Ketiga query chat memakai {@code Restrictions.isNull("perkuliahan")} versus
	 * {@code Restrictions.eq("perkuliahan", ...)}, sehingga pesan di satu ruang tidak pernah
	 * bocor ke ruang lain.</p>
	 *
	 * @return perkuliahan konteks, atau {@code null} untuk chat global
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perkuliahan", nullable = true)
	public Perkuliahan getPerkuliahan() {
		perkuliahan = check(perkuliahan);
		return perkuliahan;
	}

	/**
	 * Menyetel {@link Perkuliahan} konteks pesan.
	 *
	 * <p>Diisi {@code ChatRoom.say(...)} dari nilai yang dibawa {@code ChatWindow}/{@code Chatter}.</p>
	 *
	 * @param perkuliahan perkuliahan konteks, atau {@code null} untuk chat global
	 */
	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	/**
	 * Mengembalikan penanda mode siaran (tab "Forum Komunikasi").
	 *
	 * <p>{@code true} berarti pesan ditujukan ke <b>semua</b> peserta ruang, bukan ke satu lawan
	 * bicara. Perbedaan perilakunya besar:</p>
	 * <ul>
	 * <li>Penyaringannya <b>hanya</b> {@code semua = true} + kesamaan {@link #getPerkuliahan()}
	 * + belum tercatat di {@link #getDiterimaOleh()} — tanpa penyaring pengirim/penerima dan
	 * tanpa penyaring {@link #getAktif()}.</li>
	 * <li>Barisnya <b>tidak pernah dihapus</b>; {@code Chatter.renderMessages(...)} hanya
	 * meng-<i>append</i> penanda penerima ke {@link #getDiterimaOleh()}. Riwayat forum karena itu
	 * bertahan permanen di tabel {@code pesan}.</li>
	 * </ul>
	 * <p><b>Kuirk:</b> tipe {@link Boolean} (bisa {@code null}), tetapi
	 * {@code Chatter.renderMessages(...)} memakainya sebagai {@code if (msg.getSemua())} —
	 * <i>auto-unboxing</i> yang akan melempar {@link NullPointerException} bila kolomnya
	 * {@code NULL}. Nilai awal Java {@code false} melindungi jalur normal, tapi tidak melindungi
	 * baris yang ditulis di luar jalur Java.</p>
	 *
	 * @return {@code true} bila pesan siaran, {@code false} bila japri, {@code null} bila kolom
	 *         tidak terisi
	 */
	public Boolean getSemua() {
		return semua;
	}

	/**
	 * Menyetel penanda mode siaran.
	 *
	 * <p>Diisi {@code ChatRoom.say(...)} dari flag yang dibawa {@code ChatWindow} — bernilai
	 * {@code true} untuk tab "Forum Komunikasi" ({@code tbmuser == null} pada
	 * {@code ChatUsers.prosess(...)}), {@code false} untuk tab lawan bicara perorangan.</p>
	 *
	 * @param semua {@code true} siaran, {@code false} japri
	 */
	public void setSemua(Boolean semua) {
		this.semua = semua;
	}

	/**
	 * Mengembalikan daftar penerima pesan siaran yang <b>sudah</b> merender pesan ini, berbentuk
	 * rangkaian penanda {@code "[userId]"} yang disambung tanpa pemisah (mis.
	 * {@code "[andi][budi][cici]"}).
	 *
	 * <p><b>Getter menormalkan nilai:</b> {@code null} dikembalikan sebagai string kosong dan
	 * hasilnya di-{@code trim()}. Normalisasi ini bukan kosmetik — ia diperlukan agar
	 * {@code Chatter.renderMessages(...)} bisa langsung melakukan
	 * {@code msg.setDiterimaOleh(msg.getDiterimaOleh() + "[" + userId + "]")} tanpa memunculkan
	 * teks {@code "null[andi]"}.</p>
	 * <p><b>Efek samping tak langsung:</b> karena pemetaan memakai <i>property access</i>, baris
	 * yang di DB bernilai {@code NULL} akan terbaca sebagai {@code ""} saat <i>dirty check</i>,
	 * sehingga <i>flush</i> berikutnya dapat menuliskan {@code UPDATE pesan SET diterima_oleh = ''}
	 * — perubahan yang tidak diminta siapa pun. Hal yang sama berlaku untuk nilai berspasi di
	 * ujung, yang akan ikut terpangkas permanen.</p>
	 *
	 * <p><b>Cara pemakaiannya sebagai penyaring:</b> ketiga query chat menambahkan
	 * {@code Restrictions.not(Restrictions.ilike("diterimaOleh", "[" + userId + "]", MatchMode.ANYWHERE))}
	 * sehingga sebuah pesan siaran dikirim tepat sekali ke tiap pemakai. Kurung siku dipakai agar
	 * id yang satu bukan awalan id yang lain (mis. {@code "[ali]"} tidak cocok dengan
	 * {@code "[ali2]"}).</p>
	 * <p><b>Dua kelemahan yang tercatat (tidak diperbaiki di sini):</b></p>
	 * <ol>
	 * <li><b>Tumbuh tanpa batas.</b> Kolomnya bertipe {@code text} dan tiap penerima menambah
	 * beberapa karakter selamanya; untuk forum berisi ribuan pemakai satu baris bisa menyimpan
	 * daftar puluhan ribu karakter, sekaligus menjadi <b>read receipt seluruh peserta</b> yang
	 * ikut terekspos lewat endpoint reflektif (lihat catatan keamanan pada Javadoc kelas).</li>
	 * <li><b>{@code userId} masuk mentah sebagai pola {@code LIKE}.</b> Bila sebuah {@code userId}
	 * memuat {@code %} atau {@code _}, karakter itu berlaku sebagai <i>wildcard</i> dan pemakai
	 * tersebut bisa dianggap "sudah menerima" pesan yang belum pernah dibacanya.</li>
	 * </ol>
	 *
	 * @return daftar penanda penerima; <b>tidak pernah {@code null}</b> — string kosong bila belum
	 *         ada yang menerima
	 */
	@Column(columnDefinition = "text")
	public String getDiterimaOleh() {
		return diterimaOleh == null ? "" : diterimaOleh.trim();
	}

	/**
	 * Menyetel daftar penanda penerima pesan siaran.
	 *
	 * <p>Satu-satunya pemanggil produksi adalah {@code Chatter.renderMessages(...)}, yang selalu
	 * memakai pola <i>append</i>: {@code setDiterimaOleh(getDiterimaOleh() + "[" + userId + "]")}
	 * lalu {@code Common.refreshSaveOrUpdate(session, msg)}. Menyetel nilai secara langsung
	 * (menimpa, bukan menambah) akan membuat pesan siaran terkirim ulang ke pemakai yang
	 * penandanya terhapus.</p>
	 *
	 * @param diterimaOleh rangkaian penanda {@code "[userId]"} (boleh {@code null})
	 */
	public void setDiterimaOleh(String diterimaOleh) {
		this.diterimaOleh = diterimaOleh;
	}

}
