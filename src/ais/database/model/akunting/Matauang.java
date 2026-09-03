package ais.database.model.akunting;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Matauang — katalog master "Mata Uang" (valuta) modul akunting</h3>
 *
 * <p><b>Untuk apa (terverifikasi dari kode, bukan dari nama kelas):</b><br>
 * Entity ini memetakan tabel <code>akunting.matauang</code> dan berperan sebagai
 * <em>katalog datar</em> berisi daftar nama valuta yang boleh dipilih operator —
 * Rupiah, Dolar Amerika, dan seterusnya. Isinya benar-benar hanya dua kolom bisnis:
 * {@link #getNama() nama} (wajib) dan {@link #getKeterangan() keterangan} (opsional),
 * ditambah jejak audit standar ({@link #getOleh() oleh}, {@link #getOlehId() olehId},
 * {@link #getTanggal_dirubah() tanggal_dirubah}) yang diwarisi pola dari
 * {@link ais.database.model.GeneralValueObject}. Tidak ada logika bisnis, tidak ada
 * perhitungan, tidak ada validasi di dalam kelas ini sendiri.</p>
 *
 * <p><b>Yang justru TIDAK dimiliki entity ini — penting untuk tidak salah duga:</b></p>
 * <ul>
 *   <li><b>Tidak ada kolom kurs / nilai tukar / rate sama sekali.</b> Sudah diperiksa
 *       seluruh 107 baris versi asli: tidak ada <code>kurs</code>, <code>rate</code>,
 *       <code>nilaiTukar</code>, maupun tanggal berlaku kurs. Nilai tukar dalam
 *       rancangan lama justru dititipkan ke <em>baris jurnalnya</em>
 *       (<code>Transaksi.currencyCurs</code>), bukan ke katalog ini. Konsekuensinya
 *       struktural: sekalipun relasi ke jurnal dihidupkan kembali, katalog ini
 *       <em>secara rancangan tidak sanggup</em> menyuplai kurs untuk konversi apa pun.</li>
 *   <li><b>Tidak ada kolom kode maupun simbol.</b> Ini bertentangan dengan dokumentasi
 *       yang menyertainya: berkas bantuan <code>WEB-INF/bantuan/matauang.html</code>
 *       berulang kali menyuruh operator "isi nama, kode, simbol, dan keterangan", dan
 *       Javadoc lama {@code MataUangAction} juga menyinggung kode valuta (IDR/USD).
 *       Kolom itu tidak pernah ada. Formulir ZK pun hanya punya dua kotak isian
 *       (Nama Matauang dan Keterangan) — jadi teks bantuan menjanjikan field yang tidak
 *       tersedia. Perlu dicatat: {@code GeneralValueObject} memang punya field Java
 *       bernama <code>kode</code>, tetapi induk itu <b>bukan</b> {@code @MappedSuperclass}
 *       sehingga tidak dipetakan Hibernate, dan kelas ini tidak mendeklarasikan ulang
 *       <code>kode</code> — artinya nilai apa pun yang disetel ke sana hanya hidup di
 *       memori dan tidak pernah tersimpan.</li>
 *   <li><b>Tidak ada kolom tenant apa pun</b> — tidak ada <code>satuanKerja</code>,
 *       <code>sekolah</code>, maupun <code>yayasan</code>. Katalog ini <b>global untuk
 *       seluruh instalasi</b>: satu daftar dipakai bersama semua tenant, dan operator
 *       tenant mana pun yang punya hak ubah menyunting daftar yang sama. Ini bukan
 *       kasus "fail-open cakupan tenant" (penyaring yang gagal terbuka) melainkan
 *       ketiadaan sumbu tenant sejak awal — polanya sama dengan
 *       {@link ais.database.model.akunting.Devisi} dan
 *       {@link ais.database.model.akunting.Closing}.</li>
 *   <li>Tidak ada bendera aktif/nonaktif, tidak ada nomor urut, tidak ada penanda
 *       "mata uang default", dan tidak ada indeks unik pada kolom <code>nama</code>.</li>
 * </ul>
 *
 * <h3>Status HIDUP/MATI — hasil verifikasi menyeluruh seluruh repo</h3>
 *
 * <p>Pertanyaan yang mesti dijawab: karena {@link ais.database.model.akunting.Transaksi}
 * mendokumentasikan <code>matauang</code>/<code>currencyCurs</code>/<code>nilaiRupiah</code>
 * sebagai <em>kolom mati terverifikasi</em> (multi-mata-uang tidak pernah diaktifkan),
 * apakah katalog ini ikut mati total, atau masih hidup lewat modul lain? Penelusuran
 * dilakukan ke SELURUH repo (bukan hanya paket <code>akunting</code>): 58 kecocokan
 * teks <code>Matauang</code> pada berkas <code>*.java</code>, ditambah sapuan
 * case-insensitive ke <code>webapp</code> (JSP/ZUL/XML). Hasilnya <b>terbelah rapi</b>:</p>
 *
 * <p><b>(A) HULU — HIDUP.</b> Katalog ini benar-benar dikelola operator:</p>
 * <ul>
 *   <li>{@code ais.action.master.akunting.MataUangAction} + layar ZK
 *       <code>/pages/master/akunting/matauang.zul</code> — CRUD lengkap (tambah/ubah/
 *       hapus/cari) memakai {@code ais.database.dao.akunting.MataUangDao}.</li>
 *   <li>Menu tersendiri terdaftar di {@code ais.common.MenuSnapshotData}:
 *       <code>363522|400000000|600000004|Matauang|/pages/master/akunting/matauang.zul</code>
 *       — jadi jalur ZK punya menu sendiri dan hak aksesnya dievaluasi terhadap menu itu.</li>
 *   <li>{@code ais.action.master.helper.RevisiHelper} dipanggil per baris grid, sehingga
 *       riwayat revisi Envers katalog ini bisa ditelusuri dari UI.</li>
 *   <li>Jalur CRUD generik: <code>/WEB-INF/baru/modul/pagesmasterakuntingmatauangzul/index.jsp</code>
 *       memanggil {@code ais.common.DynamicJspCrudGenerator.generate(Matauang.class)}.
 *       Manifes <code>generic-crud/manifests/general_value_object_inventory.csv</code>
 *       menandainya <code>ELIGIBLE_METADATA_FIRST</code> dengan catatan "tetap default
 *       disabled sampai verifikasi Hibernate/menu/scope".</li>
 *   <li>Scaffold UI baru <code>WEB-INF/new/akunting/uiux/mata_uang.jsp</code> dan
 *       <code>.../services/mata_uang_service.jsp</code>. <b>Verifikasi negatif yang
 *       menenangkan:</b> keduanya murni metadata (hanya {@code request.setAttribute}
 *       lalu {@code jsp:include} ke dispatcher bersama) — <b>nol akses data</b>, jadi
 *       berbeda tajam dari <code>_monitor_akunting_service.jsp</code> yang benar-benar
 *       menyajikan agregat jurnal tanpa otentikasi.</li>
 * </ul>
 *
 * <p><b>(B) HILIR — MATI TOTAL.</b> Tidak satu pun konsumen membaca katalog ini untuk
 * keperluan akuntansi:</p>
 * <ul>
 *   <li>Hanya ada <b>dua</b> FK masuk di seluruh repo:
 *       {@link ais.database.model.akunting.Transaksi#getMatauang()} dan
 *       {@link ais.database.model.akunting.TemplateTransaksi#getMatauang()}.</li>
 *   <li>{@code setMatauang(...)} <b>tidak dipanggil dari mana pun</b> — grep seluruh
 *       repo hanya menemukan deklarasi setter itu sendiri di kedua entity tersebut,
 *       tanpa satu pun pemanggil di Action, Helper, ApiHelper, JSP, maupun ZUL.
 *       Kolom <code>transaksi.matauang</code> dan <code>template_transaksi.matauang</code>
 *       karena itu <b>permanen NULL</b>. Hal yang sama berlaku untuk
 *       <code>setCurrencyCurs(...)</code> dan <code>setNilaiRupiah(...)</code> —
 *       ini mengonfirmasi ulang klaim "kolom mati terverifikasi" pada
 *       {@link ais.database.model.akunting.Transaksi}.</li>
 *   <li>{@link ais.database.model.akunting.TemplateTransaksi} sendiri sudah lebih dulu
 *       terverifikasi sebagai <em>entity tidur/yatim</em> (nol pemanggil di seluruh
 *       repo), jadi kaki kedua ini mati berlapis.</li>
 *   <li>Nol laporan, nol helper, nol HQL/SQL, nol permukaan REST yang menyentuh
 *       katalog ini. Tidak ada modul lain (CRM, repository, koperasi, asset, SIRS,
 *       kantin, dsb.) yang punya sistem multi-mata-uang sendiri yang memakai kelas ini.</li>
 * </ul>
 *
 * <p><b>Kesimpulan status:</b> entity ini <b>BUKAN</b> "entity tidur/yatim" kelima —
 * ia punya layar, menu, DAO, dan riwayat revisi yang benar-benar terpakai. Polanya
 * adalah pola <b>master hidup dengan seluruh hilir mati</b>, sama seperti
 * {@link ais.database.model.akunting.AkunPajak}. Bedanya, di sini kematian hilirnya
 * lebih dalam: {@code AkunPajak} setidaknya menyimpan tarif yang <em>bermakna</em> bila
 * kelak disambung, sedangkan katalog mata uang tanpa kolom kurs tidak menyimpan
 * informasi apa pun yang bisa dipakai mesin jurnal untuk melakukan konversi. Operator
 * boleh terus menambah "USD", "SGD", "EUR" ke daftar ini sampai kapan pun; tidak ada
 * satu baris jurnal pun yang akan pernah merujuknya, dan tidak ada laporan yang berubah.
 * Dalam praktiknya isi tabel ini murni dokumentasi manusia.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Identitas &amp; siklus hidup</b> — {@link #Matauang()}, {@link #getId()},
 *       {@link #setId(Long)}.</li>
 *   <li><b>Data bisnis</b> — {@link #getNama()}, {@link #setNama(String)},
 *       {@link #getKeterangan()}, {@link #setKeterangan(String)}.</li>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}, dan
 *       kait siklus hidup {@code onUpdate()}.</li>
 *   <li><b>Penyajian</b> — {@link #toString()}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang perlu diketahui</h3>
 * <ul>
 *   <li><b>Javadoc asli menyebut kelas yang salah.</b> Baris komentar bawaan hbm2java
 *       berbunyi "Bank generated by hbm2java" — sisa salin-tempel dari entity Bank,
 *       bukan indikasi hubungan apa pun dengan perbankan.</li>
 *   <li><b>{@link #getNama()} memangkas spasi saat dibaca, {@link #setNama(String)}
 *       tidak memangkas saat ditulis.</b> Pemangkasan bersifat <em>non-destruktif</em>
 *       (nilai field tidak ditimpa), berbeda dari getter destruktif seperti
 *       {@code Transaksi.getAkun()} — lihat catatan pada method masing-masing.
 *       Efek sampingnya ada di penjaga duplikasi (butir berikut).</li>
 *   <li><b>Penjaga duplikasi nama bocor dua arah.</b> {@code MataUangAction.checkNamaMatauang()}
 *       membandingkan <code>Restrictions.eq("nama", input.trim())</code> terhadap kolom
 *       mentah. Karena setter menyimpan apa adanya, baris lama yang tersimpan sebagai
 *       <code>"USD&nbsp;"</code> tidak akan pernah cocok dengan input <code>"USD"</code>,
 *       sehingga duplikat lolos — dan karena getter memangkas saat dibaca, keduanya
 *       <em>tampil identik</em> di grid. Pembandingnya juga case-sensitive, jadi "usd"
 *       dan "USD" sama-sama bisa tersimpan. Tidak ada indeks unik di database sebagai
 *       jaring pengaman, dan ada jendela TOCTOU antara pengecekan dan penyimpanan.
 *       Dampak nyatanya rendah justru karena hilirnya mati.</li>
 *   <li><b>{@link #toString()} membaca field <code>nama</code> langsung</b>, bukan lewat
 *       getter, sehingga nilainya <em>tidak</em> dipangkas dan dapat bernilai
 *       <code>null</code> untuk instance transien yang belum diisi.</li>
 *   <li><b>Stempel "oleh" hanya terpasang saat UPDATE.</b> Kelas ini punya kait
 *       {@code @PreUpdate} tetapi <b>tidak</b> punya {@code @PrePersist}, jadi baris yang
 *       baru dibuat dan belum pernah disunting akan memiliki <code>oleh</code>/
 *       <code>olehId</code> bernilai <code>null</code> — pembuat data tidak pernah
 *       tercatat, hanya penyunting terakhir.</li>
 *   <li><b>Setter jejak audit menolak nilai kosong.</b> {@link #setOleh(String)} dan
 *       {@link #setOlehId(String)} langsung <code>return</code> bila argumen
 *       <code>null</code> atau kosong — stempel yang sudah terisi tidak bisa dikosongkan
 *       kembali lewat setter.</li>
 *   <li><b>{@code @Audited} (Envers) aktif</b>, sehingga setiap versi baris digandakan ke
 *       tabel revisi <code>akunting.matauang_aud</code>. Untuk katalog nama valuta ini
 *       tidak membawa risiko kerahasiaan; catatan ini relevan hanya sebagai konteks
 *       mekanisme yang sama yang membuat kredensial pada entity lain ikut terduplikasi
 *       permanen.</li>
 *   <li><b>{@code serialVersionUID} bernilai {@code 2463821577548439808L}</b> — konstanta
 *       boilerplate yang dipakai bersama puluhan entity lain di repo ini; nilai itu bukan
 *       sidik jari kelas ini dan tidak menandakan hubungan kekerabatan apa pun.</li>
 *   <li><b>{@code id} dipetakan dengan {@code insertable = false}</b> mengikuti strategi
 *       {@code IDENTITY}: nilai kunci dihasilkan database, bukan dikirim aplikasi.</li>
 * </ul>
 *
 * <h3>Catatan hak akses (hasil verifikasi, bukan dugaan)</h3>
 * <ul>
 *   <li><b>Jalur ZK aman secara struktur.</b> {@code MataUangAction} memanggil
 *       {@code Common.doCheckSecurity()} di {@code doBeforeCompose} dan memeriksa ulang
 *       sesi + hak READ di {@code doAfterCompose}; tombol Tambah/Ubah/Hapus dikendalikan
 *       hak CREATE/UPDATE/DELETE. Menu untuk layar ini terdaftar sendiri, jadi hak
 *       dievaluasi terhadap menu yang benar.</li>
 *   <li><b>Jalur CRUD generik mewarisi hak dari menu yang salah.</b>
 *       {@code DynamicJspCrudGenerator} memakai
 *       {@code CommonPrivilages.checkPrevilages(kode, tbmuser)}, yang me-resolve hak
 *       terhadap <code>Common.getCurrentMenu()</code> — atribut sesi yang <em>tidak
 *       pernah di-set ulang</em> untuk halaman CRUD generik. Akibatnya hak tambah/ubah/
 *       hapus atas katalog ini ditentukan oleh menu apa pun yang terakhir dibuka
 *       pengguna, bukan oleh menu Matauang. Ini instans lain dari pola pewarisan hak
 *       lewat menu induk/menu salah yang sudah berulang kali ditemukan di repo ini.
 *       Sisi meringankannya: bila <code>currentMenu</code> bernilai <code>null</code>,
 *       {@code checkPrevilages} mengembalikan <code>false</code> (fail-closed).</li>
 *   <li><b>Aksi baca pada CRUD generik tidak digerbangi hak sama sekali.</b> Pada
 *       {@code DynamicJspCrudGenerator.handleAjax}, cabang <code>save</code>,
 *       <code>delete</code>, <code>uploadExcel</code>, dan <code>stats</code> diperiksa
 *       haknya, tetapi <code>meta</code>, <code>list</code>, <code>get</code>,
 *       <code>options</code>, dan <code>download</code> (ekspor Excel seluruh tabel)
 *       tidak. Untuk entity ini isinya sekadar nama valuta sehingga dampaknya rendah;
 *       yang perlu diingat adalah mekanismenya bersifat generik dan berlaku untuk
 *       setiap entity yang dipaparkan lewat mesin CRUD generik yang sama.</li>
 *   <li><b>Verifikasi NEGATIF untuk pola fail-open {@code bolehAksi()}.</b> Katalog ini
 *       <b>tidak</b> termasuk tujuh master yang dijaga
 *       {@code ais.action.servlet.api.MasterKeuanganApiHelper} (Akun,
 *       CaraPembayaranTransfer, JenisKasBesar, JenisKasKecil, JenisPengeluaran,
 *       JenisReimbursement, JenisUangMuka) dan tidak muncul di ApiHelper mana pun —
 *       nol permukaan REST. Lebih jauh, gerbang {@code canCreate}/{@code canEdit}/
 *       {@code canDelete} pada mesin CRUD generik justru <b>fail-closed</b>
 *       (<code>catch</code> mengembalikan <code>false</code>, pengguna <code>null</code>
 *       ditolak) — kebalikan dari pola fail-open yang ditemukan di lapisan REST modul
 *       keuangan.</li>
 * </ul>
 *
 * <h3>Catatan teknis pewarisan</h3>
 * <p>Kelas ini {@code extends} {@link ais.database.model.GeneralValueObject}, namun induk
 * tersebut <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak
 * biasa. Hibernate karena itu <b>tidak</b> memetakan properti induknya. Deklarasi ulang
 * field {@code id}, {@code nama}, {@code keterangan}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di kelas ini <b>bukan bug atau duplikasi ceroboh</b>, melainkan
 * keharusan teknis agar kolom-kolom itu benar-benar terpetakan. Konsekuensinya juga
 * berlaku terbalik: properti induk yang <em>tidak</em> dideklarasikan ulang di sini —
 * misalnya <code>kode</code>, <code>nim</code>, dan <code>nomorUrut</code> — tidak
 * pernah tersimpan ke database sekalipun disetel dari Java.</p>
 *
 * <h3>Pemeliharaan</h3>
 * <p>Bila multi-mata-uang hendak benar-benar diaktifkan, menambah kolom di sini saja
 * tidak cukup. Yang dibutuhkan minimal: (1) kolom kurs beserta tanggal berlakunya —
 * atau tabel kurs harian terpisah, karena satu kurs statis per valuta tidak memadai
 * untuk akuntansi; (2) pemanggil untuk {@code Transaksi.setMatauang(...)} dan
 * {@code setCurrencyCurs(...)} di mesin posting; (3) keputusan eksplisit soal cakupan
 * tenant, karena katalog ini kini global; dan (4) penjaga duplikasi yang benar (indeks
 * unik pada <code>nama</code> setelah normalisasi). Sebelum keempatnya ada, menambah
 * kolom di kelas ini hanya akan memperbesar tabel tanpa mengubah satu pun angka jurnal.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.akunting.Transaksi
 * @see ais.database.model.akunting.TemplateTransaksi
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "matauang")
public class Matauang extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilai {@code 2463821577548439808L} adalah konstanta boilerplate yang dipakai
	 * bersama puluhan entity lain di repo ini (antara lain {@code Agama}, {@code Akun},
	 * {@code AkunArusKas}, {@code AkunPajak}). Nilai ini <b>bukan</b> sidik jari kelas
	 * Matauang dan tidak menandakan kekerabatan salin-tempel dengan kelas mana pun.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci utama baris katalog, dipetakan ke kolom <code>id</code> dan dibangkitkan
	 * database melalui strategi {@code IDENTITY}. Bernilai <code>null</code> selama
	 * objek masih transien (mode "tambah" pada formulir ZK).
	 */
	private Long id;

	/**
	 * Nama pengguna yang terakhir menyunting baris ini, diisi otomatis oleh kait
	 * {@code @PreUpdate}. Bernilai <code>null</code> untuk baris yang belum pernah
	 * disunting sejak dibuat, karena tidak ada kait {@code @PrePersist} di kelas ini.
	 */
	private String oleh;

	/**
	 * Identitas (id) pengguna yang terakhir menyunting baris ini, pasangan teknis dari
	 * {@link #oleh}. Sama seperti {@link #oleh}, hanya terisi pada operasi UPDATE.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir menyunting baris katalog ini.
	 *
	 * @return id pengguna penyunting terakhir; <code>null</code> bila baris belum pernah
	 *         disunting sejak dibuat
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna penyunting terakhir.
	 *
	 * <p>Dipanggil otomatis dari
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(Object)} melalui kait
	 * {@code @PreUpdate}; secara normal tidak dipanggil kode aplikasi.</p>
	 *
	 * <p><b>Efek samping / perilaku non-obvious:</b> argumen <code>null</code> atau string
	 * kosong (setelah dipangkas) <b>diabaikan diam-diam</b> — method langsung
	 * <code>return</code> tanpa mengubah apa pun. Artinya stempel yang sudah terisi
	 * tidak dapat dikosongkan kembali lewat setter ini.</p>
	 *
	 * @param olehId id pengguna penyunting; nilai <code>null</code>/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan representasi teks baris katalog, yaitu nama mata uangnya.
	 *
	 * <p>Dipakai oleh komponen ZK (label, combobox, listbox) saat objek entity dipakai
	 * langsung sebagai nilai model tampilan.</p>
	 *
	 * <p><b>Non-obvious:</b> method ini membaca field {@link #nama} <em>langsung</em>,
	 * bukan melalui {@link #getNama()}. Akibatnya nilai yang dikembalikan
	 * <b>tidak dipangkas</b> spasinya, dan dapat bernilai <code>null</code> untuk objek
	 * transien yang namanya belum diisi (misalnya instance kosong yang baru dibuat
	 * tombol "Tambah").</p>
	 *
	 * @return nama mata uang apa adanya; dapat <code>null</code> untuk objek transien
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menetapkan nama pengguna penyunting terakhir.
	 *
	 * <p>Dipanggil otomatis dari
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(Object)} melalui kait
	 * {@code @PreUpdate}; secara normal tidak dipanggil kode aplikasi.</p>
	 *
	 * <p><b>Efek samping / perilaku non-obvious:</b> sama seperti
	 * {@link #setOlehId(String)}, argumen <code>null</code> atau kosong diabaikan diam-diam
	 * sehingga stempel yang sudah ada tidak bisa dihapus lewat setter ini.</p>
	 *
	 * @param oleh nama pengguna penyunting; nilai <code>null</code>/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir menyunting baris katalog ini.
	 *
	 * @return nama pengguna penyunting terakhir; <code>null</code> bila baris belum pernah
	 *         disunting sejak dibuat
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait siklus hidup JPA yang dijalankan tepat sebelum baris ini di-UPDATE, sekaligus
	 * baris yang mendeklarasikan field {@code tanggal_dirubah}.
	 *
	 * <p><b>Tujuan:</b> memenuhi kontrak {@code abstract void onUpdate()} milik
	 * {@link ais.database.model.GeneralValueObject} dan memperbarui metadata audit baris
	 * (waktu perubahan serta identitas penyunting) tanpa membebani kode bisnis.</p>
	 *
	 * <p><b>Cara kerja:</b> mendelegasikan seluruhnya ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang akan
	 * memanggil {@link #setTanggal_dirubah(Date)}, {@link #setOleh(String)}, dan
	 * {@link #setOlehId(String)}. Interceptor tersebut lebih dulu berkonsultasi ke
	 * {@code AuditTrailHelper.peekUpdateDecision(...)} dan <b>melewati</b> pembaruan
	 * metadata bila tidak ada perubahan bisnis nyata pada baris — sehingga penyimpanan
	 * yang tidak mengubah apa pun tidak menghasilkan revisi Envers palsu.</p>
	 *
	 * <p><b>Kapan dipanggil:</b> oleh penyedia JPA/Hibernate saat flush atas entity yang
	 * kotor. Tidak pernah dipanggil langsung oleh kode aplikasi.</p>
	 *
	 * <p><b>Non-obvious:</b> (1) tidak ada pasangan {@code @PrePersist}, sehingga stempel
	 * audit tidak terisi pada operasi INSERT — baris baru punya
	 * <code>oleh</code>/<code>olehId</code> <code>null</code> sampai pertama kali
	 * disunting; (2) deklarasi field <code>tanggal_dirubah</code> ditulis pada
	 * <em>baris fisik yang sama</em> dengan method ini (gaya asli berkas, dipertahankan
	 * apa adanya) dan diinisialisasi ke waktu pembuatan objek melalui
	 * {@code ais.ui.util.WaktuUtil.getDate()} — jadi baris baru tetap punya
	 * <code>tanggal_dirubah</code> terisi walau <code>oleh</code>-nya kosong.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir baris katalog ini.
	 *
	 * <p>Umumnya dipanggil otomatis dari {@code AuditTimestampInterceptor.ubah(Object)}
	 * lewat kait {@code @PreUpdate}. Berbeda dari {@link #setOleh(String)} dan
	 * {@link #setOlehId(String)}, setter ini <b>tidak</b> menyaring nilai
	 * <code>null</code> — memberi <code>null</code> akan benar-benar mengosongkan stempel
	 * waktu.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh <code>null</code>
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris katalog ini, dipetakan sebagai
	 * kolom {@code TIMESTAMP}.
	 *
	 * <p>Untuk baris yang belum pernah disunting, nilainya adalah waktu saat objek Java
	 * dibuat (hasil inisialisasi field, bukan hasil kait {@code @PreUpdate}).</p>
	 *
	 * @return waktu perubahan terakhir; dapat <code>null</code> hanya bila sengaja
	 *         dikosongkan lewat {@link #setTanggal_dirubah(Date)}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Nama mata uang, dipetakan ke kolom <code>nama</code> (wajib, panjang 255).
	 * Satu-satunya identitas bisnis baris katalog ini — tidak ada kolom kode maupun
	 * simbol yang mendampinginya, dan tidak ada indeks unik yang menjaganya.
	 */
	private String nama;

	/**
	 * Keterangan bebas mengenai mata uang, dipetakan ke kolom <code>keterangan</code>
	 * (opsional, tanpa batas panjang eksplisit). Murni catatan operator; tidak dibaca
	 * sebagai semantik oleh kode mana pun.
	 */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen.
	 *
	 * <p>Dibutuhkan Hibernate untuk membuat instance saat memuat baris dari database,
	 * dan dipakai {@code MataUangAction.onAdd(Event)} untuk membuat objek kosong yang
	 * mewakili mode "tambah" pada formulir ZK (ditandai {@link #getId()} bernilai
	 * <code>null</code>).</p>
	 */
	public Matauang() {
	}

	/**
	 * Mengembalikan kunci utama baris katalog ini.
	 *
	 * <p>Nilai dibangkitkan database ({@code IDENTITY}); kolomnya dipetakan
	 * {@code insertable = false} sehingga aplikasi tidak pernah mengirimkan nilai id
	 * pada perintah INSERT. Nilai <code>null</code> dipakai kode UI sebagai penanda
	 * bahwa entity masih transien (mode tambah, bukan mode ubah).</p>
	 *
	 * @return id baris; <code>null</code> bila entity belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris katalog ini.
	 *
	 * <p>Secara normal hanya dipanggil Hibernate saat memuat/menyimpan entity. Kode
	 * aplikasi tidak perlu memanggilnya.</p>
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama mata uang, sudah dipangkas spasi depan-belakangnya.
	 *
	 * <p><b>Non-obvious — pemangkasan hanya terjadi saat baca.</b> Method ini
	 * mengembalikan {@code this.nama.trim()} tetapi <b>tidak menulis balik</b> hasil
	 * pemangkasan ke field. Jadi getter ini <em>bukan</em> getter destruktif seperti
	 * {@code Transaksi.getAkun()} — membaca nama tidak pernah mengubah data tersimpan.
	 * Konsekuensi lain: karena {@link #setNama(String)} menyimpan apa adanya, nilai di
	 * database bisa berbeda dari nilai yang ditampilkan, dan dua baris yang di layar
	 * tampak identik ("USD" dan "USD&nbsp;") sebenarnya berbeda di kolomnya. Inilah yang
	 * membuat penjaga duplikasi di {@code MataUangAction.checkNamaMatauang()} bisa
	 * terlewati.</p>
	 *
	 * @return nama mata uang tanpa spasi tepi; <code>null</code> bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama mata uang.
	 *
	 * <p>Dipanggil dari {@code MataUangAction.onSave(Event)} dengan nilai mentah kotak
	 * teks ZK, dan dari mesin CRUD generik {@code DynamicJspCrudGenerator}.</p>
	 *
	 * <p><b>Non-obvious:</b> nilai disimpan <b>apa adanya, tanpa dipangkas</b>, meskipun
	 * {@link #getNama()} memangkasnya saat dibaca. Kolomnya {@code nullable = false} di
	 * database, sehingga menyimpan <code>null</code> akan gagal di tingkat DB, bukan di
	 * setter ini; validasi "nama wajib diisi" dilakukan di lapisan Action.</p>
	 *
	 * @param nama nama mata uang; tidak divalidasi maupun dinormalisasi di sini
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas mata uang apa adanya (tidak dipangkas, tidak
	 * disaring).
	 *
	 * @return keterangan mata uang; <code>null</code> bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas mata uang.
	 *
	 * <p>Dipanggil dari {@code MataUangAction.onSave(Event)} (isi kotak teks multibaris
	 * pada formulir) dan dari mesin CRUD generik. Bersifat opsional: kolomnya
	 * {@code nullable = true} dan tidak ada validasi apa pun terhadap isinya —
	 * pengecekan "keterangan harus diisi" pada Action sudah dinonaktifkan (dikomentari).</p>
	 *
	 * @param keterangan keterangan bebas; boleh <code>null</code> atau kosong
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
