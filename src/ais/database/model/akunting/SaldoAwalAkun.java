package ais.database.model.akunting;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Saldo awal (neraca awal) satu akun pada tanggal pembukaan pembukuan.
 *
 * <p><b>Kenapa perlu.</b> Tanpa saldo awal, Neraca dan Buku Besar hanya berisi transaksi yang
 * tercatat sejak sistem dipakai; kas, persediaan, piutang, utang, dan modal yang sudah ada
 * sebelum go-live tidak pernah muncul, sehingga laporan tidak akan pernah sama dengan keadaan
 * sebenarnya walau seluruh dokumen sudah diposting. Entitas ini menyimpan angka pembukaannya,
 * lalu diposting menjadi SATU jurnal pembukaan.</p>
 *
 * <p><b>Bentuk data sengaja datar</b> (satu baris = satu akun, bukan header + detail) supaya
 * bisa diisi/diperbaiki per akun, diunggah dari Excel, dan diposting bertahap tanpa mengunci
 * seluruh daftar. Baris yang sudah diposting ditandai {@code postingHistory} sehingga tidak
 * mungkin terposting dua kali; koreksi setelah posting dilakukan lewat jurnal penyesuaian,
 * bukan mengubah baris ini.</p>
 *
 * <p>Tabelnya dibuat otomatis oleh Hibernate (aturan proyek: ALTER/CREATE diserahkan ke Hibernate).</p>
 *
 * <h2>Bentuk baris (terverifikasi dari kolom kelas ini)</h2>
 * <p>Satu baris hanya membawa sepuluh kolom, dan tiga di antaranya murni jejak audit:</p>
 * <ul>
 *   <li>{@code id} &mdash; kunci utama IDENTITY;</li>
 *   <li>{@code akun} &mdash; {@code ManyToOne} LAZY ke {@link ais.database.model.akunting.Akun}
 *       (bagan akun / Chart of Accounts), kolom {@code akun}, <b>nullable</b>;</li>
 *   <li>{@code tanggal} &mdash; tanggal pembukaan yang dicalonkan menjadi tanggal jurnal;</li>
 *   <li>{@code debet} dan {@code kredit} &mdash; DUA kolom nominal terpisah, bukan satu kolom
 *       bertanda. Idiom yang sama dipakai {@link ais.database.model.akunting.Transaksi};</li>
 *   <li>{@code keterangan} &mdash; catatan bebas per baris;</li>
 *   <li>{@code satuanKerja} &mdash; {@code ManyToOne} LAZY ke
 *       {@code ais.database.model.rab.SatuanKerja}, kolom {@code satuan_kerja}, <b>nullable</b>;</li>
 *   <li>{@code postingHistory} &mdash; {@code ManyToOne} LAZY ke
 *       {@link ais.database.model.akunting.PostingHistory}, kolom {@code posting_history}. Kolom
 *       INILAH satu-satunya penanda status baris: kosong = belum masuk jurnal, terisi = sudah;</li>
 *   <li>{@code oleh} / {@code olehId} / {@code tanggal_dirubah} &mdash; jejak audit ringan.</li>
 * </ul>
 *
 * <p><b>Yang TIDAK ada dan sering dikira ada.</b> Kelas ini <b>tidak punya kolom tahun buku,
 * periode, sekolah, yayasan, maupun toko/outlet</b>. Satu-satunya sumbu waktu adalah
 * {@code tanggal}, dan satu-satunya sumbu organisasi adalah {@code satuanKerja} yang boleh null.
 * Konsekuensinya dibahas pada bagian "Cakupan" dan "Integritas" di bawah.</p>
 *
 * <h2>Siapa yang memakai entitas ini (terverifikasi)</h2>
 * <p>Seluruh perilakunya ada di <b>satu</b> kelas: {@code ais.action.servlet.api.SaldoAwalAkunHelper}.
 * <b>Tidak ada</b> {@code SaldoAwalAkunAction} (kelas ZK) di repo &mdash; layar ZK
 * {@code SiklusAkuntansiKantinAction} (halaman {@code siklus_saldo_awal.zul}) sengaja hanya
 * memanggil helper yang sama dengan yang dipakai jalur REST, supaya angka ZK dan angka POS
 * mustahil berbeda. Jalur REST-nya adalah {@code PosApi} dengan prefiks aksi {@code saldo_awal_}
 * ({@code _daftar}, {@code _simpan}, {@code _hapus}, {@code _impor}, {@code _draft},
 * {@code _posting}).</p>
 *
 * <p>Alurnya: isi/unggah angka per akun &rarr; lihat draf jurnal pembukaan &rarr; posting. Saat
 * posting, helper mengambil <b>seluruh</b> baris yang {@code postingHistory}-nya masih null,
 * membuang baris tanpa akun dan baris bernilai nol (ambang {@code 0,005}), menyusun larik
 * akun+nilai debet/kredit, lalu menyerahkannya ke
 * {@code CommonAkunting.saveTransaksi(Akun[], Akun[], ...)}. Dari situ terbentuk satu
 * {@link ais.database.model.akunting.GrupTransaksi} (header jurnal) dengan sekumpulan
 * {@link ais.database.model.akunting.Transaksi} sebagai kakinya. Setelah jurnal terbentuk, helper
 * menstempel baris-baris tadi dengan id {@code PostingHistory} yang baru dibuat.</p>
 *
 * <h2>VERIFIKASI ULANG: "punya penjaga keseimbangan Dr = Cr sendiri"</h2>
 * <p>Dokumentasi {@link ais.database.model.akunting.GrupTransaksi} menyebut
 * {@code SaldoAwalAkunHelper} sebagai salah satu dari sedikit mesin yang punya penjaga
 * keseimbangan sendiri &mdash; berbeda dari {@code CommonAkunting.saveTransaksi} generik yang
 * memang tidak memvalidasi Dr = Cr. Pembacaan ulang kode menunjukkan klaim itu <b>benar
 * arahnya tetapi perlu dikoreksi bentuknya</b>:</p>
 * <ol>
 *   <li><b>Dari sisi entitas ini: NOL.</b> Kelas ini tidak punya satu pun invarian, validasi,
 *       {@code @PrePersist}, maupun method bisnis yang membandingkan {@code debet} dengan
 *       {@code kredit}. Sebuah baris boleh berisi debet saja, kredit saja, keduanya sekaligus,
 *       atau nilai negatif; entitas menerimanya apa adanya. Tidak ada juga penjaga tingkat
 *       <i>tabel</i> yang memastikan total debet seluruh baris sama dengan total kreditnya.</li>
 *   <li><b>Dari sisi helper: ada, tetapi wujudnya PENYEIMBANG OTOMATIS, bukan PENOLAK.</b>
 *       {@code SaldoAwalAkunHelper.draftAtauPosting} menghitung {@code selisih = totalDebet -
 *       totalKredit}; bila {@code |selisih| >= 0,005} selisih itu <b>ditambahkan sebagai kaki
 *       jurnal ekstra pada akun Modal/Ekuitas Awal</b> (debet lebih besar &rarr; Modal Awal
 *       dikredit; sebaliknya didebet). Jadi jurnal pembukaan <i>selalu</i> keluar seimbang: bukan
 *       karena angkanya diverifikasi, melainkan karena selisih berapa pun diserap ke satu akun
 *       penampung. Ini keputusan desain yang wajar untuk neraca awal (modal awal memang biasanya
 *       belum dihitung), tetapi artinya <b>salah ketik nominal tidak akan pernah tertahan</b> di
 *       sini &mdash; ia hanya berpindah wujud menjadi angka Modal Awal yang keliru.</li>
 *   <li><b>Penolakan hanya terjadi pada satu kondisi:</b> {@code |selisih| >= 0,005} <i>dan</i>
 *       akun Modal Awal belum diatur (baik pada master Toko maupun pada konfigurasi
 *       {@code akun_modal_awal}). Bila akun penampung itu ada, tidak ada nilai selisih seberapa
 *       pun yang bisa menggagalkan posting.</li>
 *   <li><b>Penjaga lain yang benar-benar menolak</b> datang dari luar: {@code saveTransaksi}
 *       membandingkan {@code tanggal} jurnal dengan {@code MAX(closing.tanggal)} dan
 *       mengembalikan {@code false} bila tanggalnya sudah dilewati tutup buku; helper me-rollback
 *       transaksi dan melapor "periode mungkin sudah ditutup". Ini penjaga PERIODE, bukan penjaga
 *       keseimbangan.</li>
 * </ol>
 * <p>Kesimpulan yang akurat: mesin saldo awal <b>menjamin jurnal keluarannya seimbang</b>, tetapi
 * <b>tidak memvalidasi masukan penggunanya</b>. Perbedaan itu penting saat menelusuri angka
 * Modal/Ekuitas Awal yang tidak wajar di Neraca.</p>
 *
 * <h2>Cakupan: tidak ada pembatas tenant sama sekali</h2>
 * <p>Kelas ini tidak punya kolom tenant, dan jalur pemakainya tidak menambahkannya:</p>
 * <ul>
 *   <li>{@code daftar()} menjalankan SQL mentah
 *       {@code FROM akunting.saldo_awal_akun s LEFT JOIN akunting.akun a} tanpa klausa tenant
 *       apa pun &mdash; filternya hanya pencarian teks pada kode/nama akun;</li>
 *   <li>{@code draftAtauPosting()} menyapu {@code Restrictions.isNull("postingHistory")} atas
 *       SELURUH tabel, sehingga satu kali klik "Posting" akan menarik baris saldo awal milik
 *       instalasi mana pun ke dalam satu jurnal pembukaan;</li>
 *   <li>{@code barisAkun()} mencari baris "milik akun ini" hanya dengan
 *       {@code Restrictions.eq("akun", akun)} &mdash; aturan "satu akun = satu baris" karena itu
 *       berlaku global, bukan per tenant;</li>
 *   <li>{@code satuanKerja} tidak diisi dari konteks pengguna melainkan selalu dari
 *       {@code AkunKantinUtil.satkerKantin()}, yang membaca konfigurasi
 *       {@code satuan_kerja_kantin} dan <b>mengembalikan {@code null} secara diam-diam</b> bila
 *       konfigurasi itu kosong atau tidak valid (seluruh badan method dibungkus
 *       {@code catch (Exception) &rarr; return null}). Pola "erosi cakupan satuan kerja" yang
 *       sama sudah tercatat pada entitas lain di paket ini.</li>
 * </ul>
 * <p>Efek gabungannya bersifat fail-open: tanpa kolom tenant, tanpa filter kueri, dan dengan
 * satuan kerja yang boleh menguap jadi null, seluruh angka pembukaan diperlakukan sebagai satu
 * kumpulan global.</p>
 *
 * <h2>Gerbang hak (terverifikasi dari jalur pemanggil)</h2>
 * <p>Kunci menunya {@code saldo_awal_akun}, terdaftar di {@code EbisnisMenuKatalog.DAFTAR},
 * {@code KUNCI_AKUNTANSI}, {@code KUNCI_CRUD}, dan &mdash; niat yang benar &mdash;
 * {@code KUNCI_DEFAULT_NONAKTIF} (fail-closed: peran POS lama tidak boleh mendadak melihat menu
 * akuntansi). Pada kunci ini "create" sengaja diartikan <b>boleh memposting</b>. Namun
 * penerapannya bocor di tiga titik yang saling menguatkan:</p>
 * <ol>
 *   <li><b>Gerbang luar {@code PosApi.bolehAksesActionKantin} tidak mengenali prefiks
 *       {@code saldo_awal_}.</b> Tidak ada cabang untuknya, sehingga aksi ini jatuh ke
 *       {@code return true} di ujung method &mdash; niat fail-closed dari
 *       {@code KUNCI_DEFAULT_NONAKTIF} tidak pernah tereksekusi pada jalur aksi (hanya pada
 *       daftar flag menu yang dikirim ke klien untuk menyembunyikan tombol).</li>
 *   <li><b>Dua aksi baca tidak dipagari sama sekali di helper.</b> {@code proses()} memanggil
 *       {@code daftar()} dan {@code draftAtauPosting(..., false)} tanpa memeriksa hak lebih dulu;
 *       pemeriksaan hanya dipasang pada simpan/hapus/impor/posting. Artinya setiap pengguna POS
 *       yang tokennya sah &mdash; kasir sekalipun &mdash; dapat membaca daftar lengkap kode akun,
 *       nama akun, dan nominal pembukaannya, plus draf jurnal pembukaan beserta totalnya.</li>
 *   <li><b>Gerbang dalam {@code bolehAksiMenu} fail-open pada peran null:</b>
 *       {@code peran == null &rarr; return true}. Pengguna yang terautentikasi tetapi tidak
 *       terhubung ke {@code Tbmrole} memperoleh hak penuh create/update/delete/impor
 *       <b>dan hak memposting jurnal pembukaan</b>. Ini persis pola cacat yang sudah dilacak pada
 *       tujuh helper API domain akunting lain. (Anonim tetap tertutup: {@code PosApi} menolak
 *       permintaan tanpa token yang sah sebelum dispatch.)</li>
 * </ol>
 * <p><b>Pewarisan hak lewat menu induk.</b> Di sisi ZK, halaman {@code siklus_saldo_awal.zul}
 * bukan menu tersendiri melainkan salah satu tab yang <i>disisipkan</i> oleh
 * {@code PostingJurnalAction} (larik {@code TABS}, bersama Closing, Pajak, Gaji, Penyusutan, dan
 * belasan lainnya). Baik {@code PostingJurnalAction} maupun {@code SiklusAkuntansiKantinAction}
 * hanya memanggil {@code Common.doCheckSecurity()} (pemeriksaan login) dan <b>tidak memanggil
 * {@code checkPrevilages()} satu kali pun</b>. Jadi hak atas menu "Posting Jurnal" otomatis
 * membawa serta layar Saldo Awal; satu-satunya gerbang yang tersisa adalah pemeriksaan di dalam
 * helper, dengan celah fail-open di atas.</p>
 *
 * <h2>Integritas: bisakah baseline diubah setelah tahun buku berjalan?</h2>
 * <ul>
 *   <li><b>Setelah diposting: tidak.</b> {@code simpan()} menolak bila
 *       {@code getPostingHistory() != null} ("koreksi lewat jurnal penyesuaian"), {@code hapus()}
 *       menolak dengan alasan yang sama, {@code impor()} melewati baris tersebut, dan
 *       {@code draftAtauPosting()} hanya melihat baris ber-{@code postingHistory} null. Penjaga
 *       anti-posting-ganda diperkuat lagi di tingkat SQL:
 *       {@code UPDATE ... WHERE posting_history IS NULL AND id IN (...)}. Ini termasuk pengaman
 *       idempotensi yang paling rapi di paket ini.</li>
 *   <li><b>Sebelum diposting: ya, sepenuhnya, kapan saja.</b> Tidak ada batas bahwa saldo awal
 *       hanya boleh diisi sekali atau hanya sebelum tanggal tertentu. Baris baru dapat ditambahkan
 *       bertahun-tahun setelah pembukuan berjalan dan diposting sebagai jurnal pembukaan
 *       <i>kedua</i>, <i>ketiga</i>, dan seterusnya, masing-masing menghasilkan
 *       {@code PostingHistory} sendiri. Satu-satunya rem adalah tanggal tutup buku: jurnal yang
 *       tanggalnya sebelum {@code MAX(closing.tanggal)} ditolak. Bila periode belum pernah
 *       ditutup, baseline neraca dapat digeser secara retroaktif dengan menambah baris bertanggal
 *       lampau.</li>
 *   <li><b>Tanggal jurnal tidak deterministik.</b> {@code draftAtauPosting()} memakai
 *       {@code tanggal} dari baris <i>pertama</i> yang lolos saringan
 *       ({@code if (tanggal == null) tanggal = b.getTanggal();}), padahal kriteria pengambilannya
 *       <b>tanpa {@code ORDER BY}</b>. Bila baris-baris saldo awal punya tanggal berbeda, tanggal
 *       jurnal pembukaan ditentukan oleh urutan yang dikembalikan basis data &mdash; dan seluruh
 *       baris lain ikut terbawa ke tanggal itu. Bila baris pertama bertanggal null, jurnal jatuh
 *       ke tanggal hari ini.</li>
 * </ul>
 *
 * <h2>Kuirk teknis yang perlu diketahui sebelum menyentuh kelas ini</h2>
 * <ul>
 *   <li><b>{@code getDebet()}/{@code getKredit()} adalah getter substitusi, bukan getter
 *       write-back.</b> Keduanya mengembalikan {@code 0} bila field internalnya null, tetapi
 *       <i>tidak</i> menugaskan nilai itu kembali ke field &mdash; tidak ada UPDATE liar seperti
 *       pada beberapa entitas lain di paket ini. Efek sampingnya tetap ada namun terbatas: karena
 *       Hibernate memetakan kelas ini dengan <b>property access</b> (anotasi ada di getter), nilai
 *       yang dibaca Hibernate untuk INSERT/UPDATE dan untuk cuplikan Envers adalah keluaran getter
 *       &mdash; sehingga kolom {@code debet}/{@code kredit} praktis <b>tidak pernah tersimpan
 *       sebagai NULL</b>, selalu {@code 0}. Karena itu pula pemeriksaan
 *       {@code b.getDebet() == null} di {@code draftAtauPosting()} adalah <b>cabang mati</b>
 *       (tidak berbahaya, sekadar tidak pernah menyala).</li>
 *   <li><b>Stempel posting menembus Hibernate, sehingga LUPUT dari Envers.</b> Kelas ini
 *       ber-{@code @Audited}, tetapi transisi paling penting dalam hidup sebuah baris &mdash;
 *       "belum diposting" menjadi "sudah diposting" &mdash; ditulis helper dengan
 *       {@code session.createSQLQuery("UPDATE akunting.saldo_awal_akun SET posting_history = ...")}.
 *       SQL mentah tidak melewati event listener Envers, jadi <b>tidak ada revisi
 *       {@code saldo_awal_akun_aud} yang mencatat kapan dan oleh siapa baris itu diposting</b>;
 *       {@code @PreUpdate onUpdate()} pun tidak terpanggil, sehingga {@code tanggal_dirubah} juga
 *       tidak ikut bergerak. Jejaknya hanya tersisa di {@code PostingHistory} yang ditunjuk.</li>
 *   <li><b>Aturan "satu akun satu baris" tidak ditegakkan basis data.</b> Tidak ada
 *       {@code unique = true} pada kolom {@code akun} maupun indeks unik; yang ada hanya
 *       {@code barisAkun()} dengan {@code setMaxResults(1)} di sisi aplikasi. Bila dua baris untuk
 *       akun yang sama sempat terbentuk (mis. dua permintaan bersamaan), layar ubah/hapus hanya
 *       akan pernah melihat salah satunya, sementara <b>keduanya</b> tetap ikut terposting.</li>
 *   <li><b>Kolom {@code akun} nullable.</b> Baris tanpa akun dapat tersimpan (mis. lewat
 *       manipulasi langsung); {@code draftAtauPosting()} melewatinya diam-diam, sehingga baris
 *       seperti itu bertahan selamanya sebagai baris "belum diposting" yang tidak akan pernah
 *       diposting.</li>
 *   <li><b>Baris bernilai nol tidak pernah distempel.</b> Baris dengan debet dan kredit sama-sama
 *       di bawah {@code 0,005} disaring sebelum masuk {@code idBaris}, jadi
 *       {@code postingHistory}-nya tetap null dan ia akan ikut tersapu ulang pada setiap posting
 *       berikutnya. Tidak berbahaya (nilainya nol), tetapi menjelaskan kenapa daftar "belum
 *       diposting" tidak pernah benar-benar habis.</li>
 *   <li><b>Satu baris bisa melahirkan DUA kaki jurnal.</b> Bila {@code debet} dan {@code kredit}
 *       sama-sama diisi positif, keduanya dimasukkan ke larik masing-masing untuk akun yang sama
 *       &mdash; akun itu akan muncul sebagai kaki debet sekaligus kaki kredit pada jurnal
 *       pembukaan.</li>
 *   <li><b>{@code oleh} dan {@code olehId} diisi dengan nilai yang SAMA.</b>
 *       {@code SaldoAwalAkunHelper.isiOleh()} menugaskan {@code tbmuser.getUserId()} ke keduanya,
 *       jadi {@code olehId} di sini bukan id numerik pengguna melainkan duplikat user id
 *       teks &mdash; jangan diandalkan sebagai kunci join.</li>
 * </ul>
 *
 * <h2>Pewarisan dari {@code GeneralValueObject}</h2>
 * <p>Kelas ini {@code extends} {@link ais.database.model.GeneralValueObject}. Induk tersebut
 * <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa,
 * sehingga Hibernate <b>tidak</b> memetakan properti apa pun miliknya. Karena itu kolom seperti
 * {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>wajib</b> dideklarasikan ulang di
 * sini; pengulangan itu keharusan teknis, bukan duplikasi yang perlu "dirapikan".</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 *   <li><b>Kait daur hidup</b> &mdash; {@link #onUpdate()} ({@code @PreUpdate}).</li>
 *   <li><b>Identitas</b> &mdash; {@link #getId()} / {@link #setId(Long)}.</li>
 *   <li><b>Relasi</b> &mdash; {@link #getAkun()}, {@link #getSatuanKerja()},
 *       {@link #getPostingHistory()} beserta setter-nya.</li>
 *   <li><b>Nominal dan waktu</b> &mdash; {@link #getDebet()}, {@link #getKredit()},
 *       {@link #getTanggal()} beserta setter-nya.</li>
 *   <li><b>Deskriptif</b> &mdash; {@link #getKeterangan()} / {@link #setKeterangan(String)}.</li>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()} beserta setter-nya.</li>
 * </ol>
 * <p>Tidak ada method bisnis sama sekali di kelas ini: seluruh perhitungan, penyeimbangan, dan
 * penjagaan berada di {@code SaldoAwalAkunHelper}. Entitas ini murni pembawa data.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.akunting.Akun
 * @see ais.database.model.akunting.PostingHistory
 * @see ais.database.model.akunting.GrupTransaksi
 * @see ais.database.model.akunting.Transaksi
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "akunting", name = "saldo_awal_akun")
public class SaldoAwalAkun extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Bernilai {@code 1L} seperti mayoritas entitas paket ini; nilai ini
	 * dipakai bersama oleh banyak kelas sehingga tidak dapat dijadikan penanda identitas kelas.
	 */
	private static final long serialVersionUID = 1L;

	/** Kunci utama, dihasilkan basis data (IDENTITY). */
	private Long id;

	/** Akun buku besar yang saldo awalnya dicatat baris ini; boleh null di tingkat kolom. */
	private Akun akun;

	/** Tanggal pembukaan yang dicalonkan menjadi tanggal jurnal pembukaan. */
	private Date tanggal;

	/** Nominal sisi debet. Disimpan terpisah dari {@link #kredit}, bukan satu kolom bertanda. */
	private Double debet;

	/** Nominal sisi kredit. Disimpan terpisah dari {@link #debet}, bukan satu kolom bertanda. */
	private Double kredit;

	/** Catatan bebas per baris; ikut disalin ke draf jurnal hanya sebagai informasi layar. */
	private String keterangan;

	/**
	 * Satuan kerja pemilik baris. Diisi pemanggil dari {@code AkunKantinUtil.satkerKantin()}, yang
	 * mengembalikan null bila konfigurasi {@code satuan_kerja_kantin} kosong/tidak valid.
	 */
	private ais.database.model.rab.SatuanKerja satuanKerja;

	/**
	 * Cap posting. Null = baris belum masuk jurnal pembukaan dan masih boleh diubah/dihapus;
	 * terisi = baris sudah dijurnal dan terkunci.
	 */
	private PostingHistory postingHistory;

	/** User id pembuat/pengubah terakhir baris ini (teks). */
	private String oleh;

	/**
	 * Diisi dengan nilai yang SAMA dengan {@link #oleh} oleh
	 * {@code SaldoAwalAkunHelper.isiOleh()} &mdash; bukan id numerik pengguna.
	 */
	private String olehId;

	/**
	 * Kait {@code @PreUpdate} Hibernate/JPA: menyegarkan cap waktu perubahan sesaat sebelum baris
	 * ini di-UPDATE.
	 *
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)},
	 * mekanisme audit yang sama dipakai seluruh entitas paket ini, sehingga aturan cap waktu tidak
	 * bercabang per kelas.</p>
	 *
	 * <p><b>Kasus tepi penting:</b> kait ini hanya menyala pada UPDATE yang melewati Hibernate.
	 * Stempel posting ditulis {@code SaldoAwalAkunHelper} lewat SQL mentah
	 * ({@code UPDATE akunting.saldo_awal_akun SET posting_history = ...}), sehingga pada peristiwa
	 * terpenting dalam hidup baris ini method berikut <b>tidak pernah terpanggil</b> dan
	 * {@code tanggal_dirubah} tertinggal pada nilai lamanya.</p>
	 *
	 * <p>Dipanggil oleh runtime persistensi, bukan oleh kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Cap waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat
	 * ({@code WaktuUtil.getDate()}), lalu disegarkan {@link #onUpdate()} pada tiap UPDATE lewat
	 * Hibernate.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Objek yang dibuat lewat konstruktor ini sudah membawa {@code tanggal_dirubah} berisi waktu
	 * sekarang (lihat inisialisasi field), sedangkan seluruh field lain masih null. Pemanggil
	 * ({@code SaldoAwalAkunHelper.simpan()} dan {@code impor()}) selalu mengisi
	 * {@code akun}/{@code debet}/{@code kredit}/{@code tanggal}/{@code keterangan}/
	 * {@code satuanKerja} sebelum menyimpan.</p>
	 */
	public SaldoAwalAkun() {
	}

	/** @return kunci utama baris ini; null selama entitas belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama; normalnya hanya diisi Hibernate setelah INSERT. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Akun buku besar yang saldo awalnya dicatat baris ini.
	 *
	 * <p>Relasi {@code ManyToOne} LAZY ke {@link ais.database.model.akunting.Akun} lewat kolom
	 * {@code akun}. Getter murni &mdash; tidak ada penulisan balik apa pun (kontras dengan
	 * {@code Transaksi.getAkun()} yang menimpa akun dengan {@code akunOver}).</p>
	 *
	 * <p><b>Kasus tepi:</b> kolomnya {@code nullable = true}. Baris tanpa akun tidak akan pernah
	 * masuk jurnal &mdash; {@code SaldoAwalAkunHelper.draftAtauPosting()} melewatinya diam-diam
	 * dan tidak menstempelnya, sehingga baris tersebut tersangkut permanen sebagai "belum
	 * diposting".</p>
	 *
	 * @return akun terkait, atau null bila belum/tidak diisi
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menetapkan akun buku besar baris ini.
	 *
	 * <p>Setter polos tanpa validasi: tidak memeriksa akun duplikat (aturan "satu akun = satu
	 * baris" hanya ditegakkan {@code SaldoAwalAkunHelper.barisAkun()} di sisi aplikasi, tanpa
	 * indeks unik pendukung) dan tidak menolak perubahan akun pada baris yang sudah diposting
	 * &mdash; penjaga itu ada di helper, bukan di sini.</p>
	 *
	 * @param akun akun yang saldo awalnya dicatat; boleh null
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Tanggal pembukaan; jurnal pembukaan dibuat pada tanggal ini.
	 *
	 * <p><b>Ketelitian penting:</b> "tanggal ini" berlaku untuk jurnal secara keseluruhan, bukan
	 * per baris. {@code SaldoAwalAkunHelper.draftAtauPosting()} memakai tanggal dari baris
	 * <i>pertama</i> yang lolos saringan sebagai tanggal SATU jurnal pembukaan, dan kriteria
	 * pengambilan barisnya tidak memakai {@code ORDER BY}. Bila baris-baris saldo awal bertanggal
	 * berbeda, tanggal yang menang ditentukan urutan yang dikembalikan basis data, dan seluruh
	 * baris lain ikut terbawa ke tanggal itu. Bila baris pertama bertanggal null, jurnal jatuh ke
	 * tanggal hari ini ({@code WaktuUtil.getDate()}).</p>
	 *
	 * <p>Tanggal ini juga yang diadu dengan {@code MAX(closing.tanggal)} di
	 * {@code CommonAkunting.saveTransaksi}: jurnal bertanggal sebelum tutup buku terakhir ditolak
	 * dan posting dibatalkan.</p>
	 *
	 * @return tanggal pembukaan baris ini, atau null bila belum diisi
	 */
	@Column(name = "tanggal", nullable = true)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menetapkan tanggal pembukaan baris ini.
	 *
	 * <p>Pemanggil ({@code simpan()}/{@code impor()}) selalu melewatkan hasil
	 * {@code SaldoAwalAkunHelper.tanggalDari()}, yang memakai pola {@code yyyy-MM-dd} dan
	 * <b>diam-diam jatuh ke tanggal hari ini</b> bila teksnya kosong atau gagal di-parse &mdash;
	 * jadi tanggal yang salah ketik tidak menimbulkan galat, melainkan berubah menjadi hari
	 * pengisian.</p>
	 *
	 * @param tanggal tanggal pembukaan; boleh null
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Nominal sisi debet baris ini.
	 *
	 * <p><b>Getter substitusi:</b> mengembalikan {@code 0} bila field internalnya null, tetapi
	 * <b>tidak</b> menulis balik nilai itu ke field &mdash; membaca baris ini tidak menerbitkan
	 * UPDATE apa pun. Meski begitu, karena Hibernate memetakan kelas ini dengan property access,
	 * nilai yang dibacanya untuk INSERT/UPDATE dan untuk cuplikan Envers adalah keluaran getter
	 * ini; akibatnya kolom {@code debet} praktis tidak pernah tersimpan sebagai NULL. Efek
	 * lanjutannya: pemeriksaan {@code b.getDebet() == null} di
	 * {@code SaldoAwalAkunHelper.draftAtauPosting()} adalah cabang mati.</p>
	 *
	 * <p>Nilai ini dijumlahkan menjadi {@code totalDebet} pada draf/posting; baris dengan debet dan
	 * kredit sama-sama di bawah {@code 0,005} disaring keluar dan tidak ikut dijurnal.</p>
	 *
	 * @return nominal debet, atau {@code 0} bila belum diisi &mdash; tidak pernah null
	 */
	@Column(name = "debet", nullable = true)
	public Double getDebet() {
		return debet == null ? Double.valueOf(0) : debet;
	}

	/**
	 * Menetapkan nominal sisi debet.
	 *
	 * <p>Tidak ada validasi: nilai negatif dan pengisian debet bersamaan dengan kredit sama-sama
	 * diterima. Bila keduanya positif, akun yang sama akan muncul sebagai kaki debet <i>dan</i>
	 * kaki kredit pada jurnal pembukaan.</p>
	 *
	 * <p>Pada jalur impor Excel, nilainya berasal dari {@code SaldoAwalAkunHelper.angka()} yang
	 * membuang titik sebagai pemisah ribuan dan menerima koma sebagai desimal, serta
	 * <b>mengembalikan {@code 0} secara diam-diam</b> untuk teks yang tidak dapat diurai &mdash;
	 * sel rusak menjadi nol, bukan galat.</p>
	 *
	 * @param debet nominal debet; boleh null (akan terbaca sebagai {@code 0})
	 */
	public void setDebet(Double debet) {
		this.debet = debet;
	}

	/**
	 * Nominal sisi kredit baris ini.
	 *
	 * <p>Perilakunya cermin {@link #getDebet()}: getter substitusi yang mengembalikan {@code 0}
	 * untuk field null tanpa menulis balik, dan karena property access membuat kolom
	 * {@code kredit} praktis tidak pernah tersimpan NULL.</p>
	 *
	 * <p>Nilai ini dijumlahkan menjadi {@code totalKredit}; selisih {@code totalDebet -
	 * totalKredit} yang tersisa dilempar ke akun Modal/Ekuitas Awal sebagai kaki penyeimbang,
	 * sehingga selisih apa pun tidak akan menggagalkan posting selama akun tersebut sudah
	 * diatur.</p>
	 *
	 * @return nominal kredit, atau {@code 0} bila belum diisi &mdash; tidak pernah null
	 */
	@Column(name = "kredit", nullable = true)
	public Double getKredit() {
		return kredit == null ? Double.valueOf(0) : kredit;
	}

	/**
	 * Menetapkan nominal sisi kredit. Sama sekali tanpa validasi, sejalan dengan
	 * {@link #setDebet(Double)}.
	 *
	 * @param kredit nominal kredit; boleh null (akan terbaca sebagai {@code 0})
	 */
	public void setKredit(Double kredit) {
		this.kredit = kredit;
	}

	/**
	 * Catatan bebas untuk baris ini (mis. asal angka atau nomor referensi neraca lama).
	 *
	 * <p>Ditampilkan pada daftar saldo awal, tetapi <b>tidak</b> ikut masuk ke keterangan jurnal:
	 * keterangan jurnal pembukaan disusun helper sebagai teks seragam
	 * {@code "Saldo awal (neraca awal) <n> akun"}.</p>
	 *
	 * @return keterangan baris, atau null bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan catatan bebas baris ini.
	 *
	 * <p>Tidak ada penyaringan panjang maupun penyaringan HTML/XSS di sini; pengamanan tampilan
	 * diserahkan ke lapisan penyaji.</p>
	 *
	 * @param keterangan teks catatan; boleh null atau string kosong
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Satuan kerja pemilik baris ini.
	 *
	 * <p>Relasi {@code ManyToOne} LAZY ke {@code ais.database.model.rab.SatuanKerja} lewat kolom
	 * {@code satuan_kerja}. Getter murni, tanpa penulisan balik dan tanpa substitusi.</p>
	 *
	 * <p><b>Batas cakupan yang perlu disadari:</b> nilai ini tidak pernah berasal dari konteks
	 * pengguna. {@code SaldoAwalAkunHelper} selalu mengisinya dari
	 * {@code AkunKantinUtil.satkerKantin()}, yaitu satu satuan kerja tunggal hasil pembacaan
	 * konfigurasi {@code satuan_kerja_kantin}, dan method itu mengembalikan null diam-diam bila
	 * konfigurasinya kosong/rusak. Karena kelas ini juga tidak punya kolom sekolah/yayasan/toko,
	 * kolom ini adalah satu-satunya sumbu organisasi yang tersedia &mdash; dan ia tidak dipakai
	 * sebagai filter oleh satu pun kueri saldo awal.</p>
	 *
	 * @return satuan kerja terkait, atau null
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public ais.database.model.rab.SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja pemilik baris ini.
	 *
	 * <p>Setter polos: menerima null tanpa keberatan, dan memang rutin dipanggil dengan null bila
	 * konfigurasi satuan kerja kantin belum diisi.</p>
	 *
	 * @param satuanKerja satuan kerja pemilik; boleh null
	 */
	public void setSatuanKerja(ais.database.model.rab.SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Terisi bila baris ini sudah masuk jurnal pembukaan; kunci anti-posting-ganda.
	 *
	 * <p>Relasi {@code ManyToOne} LAZY ke {@link ais.database.model.akunting.PostingHistory} lewat
	 * kolom {@code posting_history}. Satu {@code PostingHistory} (berjenis
	 * {@code SaldoAwalAkunHelper.JENIS} = "Saldo Awal (Neraca Awal)") dipakai bersama oleh SEMUA
	 * baris yang terposting dalam satu klik, sekaligus menjadi cap pada
	 * {@link ais.database.model.akunting.GrupTransaksi} yang terbentuk &mdash; itulah tali
	 * penghubung dari baris saldo awal ke jurnalnya.</p>
	 *
	 * <p><b>Peran sebagai penjaga.</b> Nilai getter ini dipakai di empat tempat:
	 * {@code simpan()} dan {@code hapus()} menolak aksi bila tidak null;
	 * {@code impor()} melewati baris tersebut dengan pesan "sudah diposting, dilewati"; dan
	 * {@code draftAtauPosting()} hanya mengambil baris yang nilainya null
	 * ({@code Restrictions.isNull("postingHistory")}), diperkuat lagi oleh klausa SQL
	 * {@code WHERE posting_history IS NULL} saat menstempel. Kombinasi itu membuat posting ganda
	 * atas baris yang sama praktis tertutup.</p>
	 *
	 * @return cap posting, atau null bila baris belum dijurnal
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		postingHistory = check(postingHistory);
		return postingHistory;
	}

	/**
	 * Menetapkan cap posting baris ini.
	 *
	 * <p><b>Catatan penting:</b> pada alur normal setter ini <b>tidak pernah dipanggil</b>.
	 * {@code SaldoAwalAkunHelper} menstempel baris lewat SQL mentah
	 * ({@code UPDATE akunting.saldo_awal_akun SET posting_history = ... WHERE posting_history IS
	 * NULL AND id IN (...)}), bukan lewat objek. Konsekuensinya: transisi status paling penting
	 * pada baris ini melewatkan {@link #onUpdate()} dan &mdash; walau kelas ini {@code @Audited}
	 * &mdash; <b>tidak menghasilkan revisi Envers</b>. Bila kelak menambah jalur baru, pakailah
	 * setter ini agar audit trailnya utuh.</p>
	 *
	 * @param postingHistory cap posting; null berarti mengembalikan baris ke status belum
	 *                       diposting sehingga bisa diubah/dihapus/diposting ulang
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/** @return user id pembuat/pengubah terakhir baris ini, atau null. */
	@Column(name = "oleh", nullable = true)
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan user id pembuat/pengubah baris ini.
	 *
	 * <p>Diisi {@code SaldoAwalAkunHelper.isiOleh()} dari {@code tbmuser.getUserId()}; bila
	 * {@code tbmuser} null (mis. pemanggil internal), field ini dibiarkan apa adanya dan kegagalan
	 * apa pun ditelan ke {@code ErrorAuditUtil}.</p>
	 *
	 * @param oleh user id; boleh null
	 */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	/**
	 * Kolom {@code olehid}.
	 *
	 * <p><b>Jangan tertipu namanya:</b> {@code SaldoAwalAkunHelper.isiOleh()} mengisinya dengan
	 * {@code tbmuser.getUserId()} &mdash; nilai yang PERSIS SAMA dengan {@link #getOleh()}, bukan
	 * id numerik pengguna. Tidak dapat dipakai sebagai kunci join ke tabel pengguna.</p>
	 *
	 * @return duplikat user id, atau null
	 */
	@Column(name = "olehid", nullable = true)
	public String getOlehId() {
		return olehId;
	}

	/** @param olehId nilai kolom {@code olehid}; pada praktiknya diisi user id yang sama dengan {@link #setOleh(String)}. */
	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	/**
	 * Cap waktu perubahan terakhir baris ini.
	 *
	 * <p>Diinisialisasi ke waktu server saat objek dibuat dan disegarkan {@link #onUpdate()} pada
	 * setiap UPDATE yang melewati Hibernate. <b>Tidak</b> bergerak saat baris distempel posting,
	 * karena stempel itu ditulis dengan SQL mentah.</p>
	 *
	 * @return cap waktu perubahan terakhir
	 */
	@Column(name = "tanggal_dirubah", nullable = true)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menetapkan cap waktu perubahan terakhir.
	 *
	 * <p>Normalnya hanya dipanggil {@code AuditTimestampInterceptor} lewat {@link #onUpdate()};
	 * pengisian manual akan tertimpa pada UPDATE berikutnya.</p>
	 *
	 * @param tanggal_dirubah cap waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
