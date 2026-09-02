package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

/**
 * <h2>BerkasHasilAkreditasi &mdash; <i>kategori/map</i> berkas bukti akreditasi
 * (tabel {@code public.berkas_hasil_akreditasi}).</h2>
 *
 * <p>Satu baris entity ini <b>bukan</b> sertifikat, SK, nilai, atau peringkat akreditasi. Baris ini
 * adalah <b>wadah (folder/kategori) berlabel</b> tempat dokumen-dokumen bukti akreditasi
 * dikumpulkan &mdash; kira-kira setara satu &laquo;map borang&raquo; atau satu &laquo;periode
 * visitasi&raquo;. Isi kolomnya hanya: nama kategori ({@code nama}, mis. &laquo;Akreditasi BAN-PT
 * 2024&raquo;), dua nama asesor sebagai <b>teks bebas</b> ({@code asesor1}/{@code asesor2}), satu
 * tanggal ({@code tanggal}), keterangan bebas, dan penanda pemilik berupa <b>salah satu</b> dari
 * jurusan/fakultas/perguruan tinggi.</p>
 *
 * <p><b>Dokumen sesungguhnya ada di entity anak</b>
 * {@link BerkasHasilAkreditasiPunyaNama} (FK {@code berkas_hasil_akreditasi}, {@code nullable =
 * false}): di sanalah metadata bibliografi (kode, nama, penulis, editor, abstrak, kata kunci,
 * penerbit, tanggal terbit) dan berkas fisiknya tersimpan. Berkas fisik itu sendiri tidak dipegang
 * oleh kedua entity, melainkan oleh {@link ais.database.model.file.LampiranLain} dengan
 * {@code jenis = "ais.database.model.BerkasHasilAkreditasiPunyaNama"} dan {@code ref} = id baris
 * anak &mdash; lihat catatan keamanan di bawah.</p>
 *
 * <p><b>Perhatikan penamaan yang menyesatkan.</b> Nama kelas berbunyi &laquo;hasil akreditasi&raquo;
 * padahal <b>tidak ada satu pun kolom hasil</b> (tidak ada peringkat A/B/C, tidak ada skor, tidak
 * ada nomor SK, tidak ada masa berlaku). Judul dialognya pun berbunyi &laquo;Tambah/Ubah Berkas
 * Akreditasi&raquo;, dan tombol tambah pada layar anaknya berbunyi &laquo;Lampiran Baru&raquo;
 * &mdash; keduanya lebih jujur menggambarkan isi tabel daripada nama kelasnya. Entity ini juga
 * <b>berbeda</b> dari {@code DokumenAkreditasi} yang berdiri sendiri di paket yang sama.</p>
 *
 * <h3>Alur pemakaian</h3>
 *
 * <ol>
 *   <li>Layar {@code /pages/master/berkas_hasil_akreditasi.zul} ditangani
 *   {@code ais.action.master.BerkasHasilAkreditasiAction}. Layar ini <b>tidak pernah muncul sebagai
 *   menu tersendiri</b>; ia selalu di-<i>include</i> sebagai tab di dalam layar master lain,
 *   dengan tepat satu parameter URL yang menentukan pemiliknya:
 *   <ul>
 *     <li>{@code JurusanAction} &rarr; {@code ...zul?jurusan=<id>}</li>
 *     <li>{@code FakultasAction} &rarr; {@code ...zul?fakultas=<id>}</li>
 *     <li>{@code PerguruanTinggiAction} &rarr; {@code ...zul?perguruanTinggi=<id>}</li>
 *   </ul>
 *   Ketiga parameter itulah yang kemudian disimpan ke {@link #setJurusan(Jurusan)},
 *   {@link #setFakultas(Fakultas)}, {@link #setPerguruanTinggi(PerguruanTinggi)} &mdash; jadi ketiga
 *   relasi bersifat <b>saling eksklusif dalam praktik</b>, meski secara skema ketiganya
 *   {@code nullable = true} dan boleh terisi bersamaan.</li>
 *   <li>Baris ini di-render sebagai satu baris grid dengan {@code MyDetail} yang dapat dibentangkan;
 *   saat dibentangkan, {@code BerkasHasilAkreditasiPunyaNamaHelper#display} memuat daftar dokumen
 *   anaknya.</li>
 *   <li><b>Sinkronisasi ke repositori DSpace.</b>
 *   {@code JurusanAction#getDspaceBerkasHasilAkreditasi(String, BerkasHasilAkreditasi)} memetakan
 *   satu baris entity ini menjadi satu <i>collection</i> DSpace, ditempatkan di bawah
 *   <i>community</i> jurusan/fakultas/perguruan tinggi sesuai relasi yang terisi (diperiksa dengan
 *   urutan {@code jurusan} &rarr; {@code fakultas} &rarr; {@code perguruanTinggi}); setiap dokumen
 *   anak menjadi satu <i>item</i> DSpace beserta unggahan berkasnya. Tautan hasilnya disimpan di
 *   {@link DspaceInformation}.</li>
 * </ol>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 *
 * <ol>
 *   <li><b>Identitas &amp; kunci</b>: {@link #getId()}/{@link #setId(Long)} &mdash; {@code IDENTITY},
 *   {@code insertable = false}.</li>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()}
 *   dan callback {@link #onUpdate()}. Kelas ber-{@code @Audited} (Hibernate Envers), sehingga tiap
 *   perubahan direkam ke tabel revisi &mdash; itu yang dibaca tombol riwayat
 *   {@code RevisiHelper.createNewRevisi(BerkasHasilAkreditasi.class, ...)} pada kolom pertama
 *   grid.</li>
 *   <li><b>Data bisnis</b>: {@link #getNama()}, {@link #getAsesor1()}, {@link #getAsesor2()},
 *   {@link #getTanggal()}, {@link #getKeterangan()}.</li>
 *   <li><b>Relasi pemilik (saling eksklusif)</b>: {@link #getJurusan()}, {@link #getFakultas()},
 *   {@link #getPerguruanTinggi()}.</li>
 *   <li><b>Utilitas</b>: {@link #toString()}.</li>
 * </ol>
 *
 * <p>Tidak ada method bisnis, tidak ada method/query statis, dan tidak ada koleksi anak yang
 * dipetakan (arah relasi ke {@link BerkasHasilAkreditasiPunyaNama} <b>unidirectional</b>: hanya anak
 * yang menunjuk induk). Seluruh logika layar berada di {@code BerkasHasilAkreditasiAction} dan
 * {@code BerkasHasilAkreditasiPunyaNamaHelper}.</p>
 *
 * <h3>Verifikasi pola berulang paket ini</h3>
 *
 * <p>Tiga kuirk yang lazim ditemukan di entity {@code ais.database.model} <b>diperiksa langsung pada
 * kode berkas ini</b>, dengan hasil:</p>
 * <ul>
 *   <li><b>Getter yang menulis balik ke field lalu ikut ter-flush ke DB: TIDAK ADA.</b>
 *   {@link #getNama()} memang memanggil {@code trim()}, tetapi mengembalikan salinan dan
 *   <b>tidak</b> menyentuh field &mdash; berbeda dari {@code Kota#getNama()} atau
 *   {@code ScholarArticle#getNama()} yang menulis balik. {@link #getPerguruanTinggi()} pun hanya
 *   menyaring nilai kembaliannya, bukan field. Jadi sekadar menampilkan daftar akreditasi tidak
 *   membuat baris menjadi <i>dirty</i>.</li>
 *   <li><b>Getter yang menutup sesi Hibernate: TIDAK ADA.</b> Ketiga getter relasi mengembalikan
 *   field apa adanya dan bahkan <b>tidak</b> memanggil {@code check(...)} milik
 *   {@link GeneralValueObject} &mdash; lihat kuirk di bawah.</li>
 *   <li><b>Getter destruktif (menghapus/menimpa data saat dibaca): TIDAK ADA.</b></li>
 * </ul>
 *
 * <h3>Kuirk &amp; hal non-obvious</h3>
 *
 * <ul>
 *   <li><b>Judul generator salah salin.</b> Javadoc bawaan berbunyi <i>"Bank generated by
 *   hbm2java"</i>. Berkas ini bukan {@code Bank}; judul itu adalah sisa salin-tempel dari
 *   {@link Bank} (sumber aslinya) yang menular ke puluhan entity paket ini. Header generator di
 *   baris 3 dipertahankan sebagai jejak sejarah, blok ini menggantikan judul satu barisnya.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 *   duplikasi yang boleh dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak biasa &mdash;
 *   bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate sama sekali
 *   tidak memetakan properti kelas induk. Setiap entity turunan <b>wajib</b> mendeklarasikan sendiri
 *   kolom-kolom itu. Hal yang sama berlaku untuk {@code nama} dan {@code keterangan}.</li>
 *   <li><b>{@link #getKeterangan()} meng-<i>override</i> versi induk dan membalik kontraknya.</b>
 *   {@code GeneralValueObject#getKeterangan()} menjamin tidak pernah {@code null} (mengembalikan
 *   {@code ""}); versi di kelas ini mengembalikan field apa adanya, jadi <b>bisa {@code null}</b>.
 *   Ini instance ketujuh dari pola yang sama di paket ini (setelah {@code Bank},
 *   {@code SintaArticle}, {@code PendaftaranSidang}, {@code ScholarAuthor}, {@code ScholarArticle},
 *   {@code DiskusiKomentar}) &mdash; sudah layak dianggap variasi arsitektural yang mapan, bukan
 *   anomali. Akibat praktisnya: {@code new Label(berkas.getKeterangan())} di renderer grid dan
 *   {@code new Textbox(berkas.getKeterangan())} di dialog menerima {@code null} untuk baris yang
 *   keterangannya kosong (ZK memperlakukannya sebagai teks kosong, jadi tidak terlihat oleh
 *   pengguna).</li>
 *   <li><b>{@link #getNama()} bisa mengembalikan {@code null} padahal kolomnya
 *   {@code nullable = false}.</b> Pemetaan memakai <i>property access</i> (anotasi menempel pada
 *   getter), sehingga nilai yang benar-benar ditulis ke kolom {@code nama} adalah hasil
 *   {@code trim()}, bukan isi field mentah. Objek yang belum diisi namanya akan ditolak database
 *   dengan pelanggaran {@code NOT NULL}; validasi wajib-isi hanya ada di lapisan UI
 *   ({@code BerkasHasilAkreditasiAction#onSave}).</li>
 *   <li><b>{@link #getPerguruanTinggi()} menyembunyikan instance transien.</b> Getter ini
 *   mengembalikan {@code null} bila objek {@code PerguruanTinggi} yang tersimpan di field belum
 *   punya id. Karena Hibernate membaca properti lewat getter, efeknya <b>bukan sekadar kosmetik</b>:
 *   kolom {@code perguruan_tinggi} akan ditulis {@code NULL} saat <i>flush</i>. Dua getter relasi
 *   lainnya ({@link #getJurusan()}, {@link #getFakultas()}) <b>tidak</b> punya penyaring serupa
 *   &mdash; asimetri yang disengaja atau tidak, tetapi nyata.</li>
 *   <li><b>Asesor disimpan sebagai teks bebas, bukan relasi.</b> {@code asesor1}/{@code asesor2}
 *   adalah {@code String} tanpa FK ke {@link Dosen}, {@link Pegawai}, maupun
 *   {@code AsesorPenunjangKinerjaDosen}. Salah ketik nama asesor tidak terdeteksi, dan tidak ada
 *   cara menelusuri &laquo;semua akreditasi yang diases oleh X&raquo; selain pencocokan teks.</li>
 *   <li><b>Hanya {@code nama} yang dapat dicari.</b> {@code initCriteria} memfilter dengan
 *   {@code Restrictions.ilike("nama", ..., MatchMode.ANYWHERE)} (ter-<i>parameterize</i>, aman dari
 *   injeksi SQL); ketiga relasi pemilik dicocokkan dengan {@code Restrictions.eq}, dan bila
 *   parameter URL-nya tidak ada dipakai {@code sqlRestriction("true")} &mdash; sebuah literal tetap,
 *   bukan nilai dari klien.</li>
 *   <li><b>Impor Excel menghasilkan baris yatim.</b> {@code Common.uploadData} pada layar ini hanya
 *   memetakan kolom {@code id, nama, asesor1, asesor2, tanggal, keterangan} &mdash; ketiga relasi
 *   pemilik <b>tidak ikut</b>. Baris hasil impor karenanya punya
 *   {@code jurusan = fakultas = perguruanTinggi = null}, sehingga (a) tidak pernah lolos filter tab
 *   mana pun dan praktis <b>hilang dari seluruh UI</b>, dan (b) membuat
 *   {@code getDspaceBerkasHasilAkreditasi} jatuh ke cabang {@code else} dan mengembalikan
 *   {@code null}. Sisi baiknya, tombol impor tersebut hanya tampil bila pengguna memegang ketiga hak
 *   CREATE, UPDATE, <i>dan</i> DELETE sekaligus &mdash; gerbang yang justru lebih ketat dari
 *   kebiasaan layar lain.</li>
 *   <li><b>Menyunting baris menulis ulang kepemilikannya dari parameter URL.</b>
 *   {@code onSave()} selalu memanggil ketiga <i>setter</i> relasi dengan nilai yang dibaca dari
 *   parameter {@code jurusan}/{@code fakultas}/{@code perguruanTinggi} pada saat layar dibuka &mdash;
 *   nilai lama di baris tidak dipertahankan. Dalam alur normal hal ini tidak terasa (setiap tab
 *   hanya menampilkan baris yang cocok dengan parameternya sendiri), tetapi berarti kepemilikan
 *   baris ditentukan sepenuhnya oleh URL, bukan oleh data yang tersimpan.</li>
 *   <li><b>Kelas ini tidak terdaftar di {@code DspaceInformation.linksForClass}.</b> Yang terdaftar
 *   hanya {@link BerkasHasilAkreditasiPunyaNama}. Padahal
 *   {@code getDspaceBerkasHasilAkreditasi} tetap membuat baris {@link DspaceInformation} untuk
 *   entity ini &mdash; barisnya ada di database, tetapi {@code DspaceInformation#showLink} tidak akan
 *   pernah merendernya, sehingga tautan ke <i>collection</i> DSpace tidak dapat diklik dari
 *   AIS.</li>
 *   <li><b>{@code cascade = {PERSIST, MERGE}} pada ketiga relasi pemilik.</b> Menyimpan satu berkas
 *   akreditasi dapat ikut men-{@code merge} entity {@link Jurusan}/{@link Fakultas}/
 *   {@link PerguruanTinggi} yang menempel padanya. Tidak ada {@code REMOVE}, jadi menghapus berkas
 *   tidak menghapus jurusan/fakultas.</li>
 *   <li><b>{@code @Fetch(FetchMode.SELECT)}</b> pada ketiga relasi memaksa satu SELECT terpisah per
 *   relasi per baris (bukan JOIN). Pada grid dengan {@code Common.ROWS_COUNT_ON_PAGE} baris, ini
 *   pola N+1 klasik &mdash; walau di layar ini dampaknya kecil karena renderer tidak membaca ketiga
 *   getter relasi sama sekali.</li>
 * </ul>
 *
 * <h3>Catatan keamanan</h3>
 *
 * <ul>
 *   <li><b>Layar ini termasuk contoh positif untuk gerbang tulis.</b>
 *   {@code BerkasHasilAkreditasiAction} memanggil {@code Common.doCheckSecurity()} pada
 *   {@code doBeforeCompose}, menyembunyikan tombol Tambah tanpa hak {@code CREATE}, dan menyimpan
 *   hak {@code UPDATE}/{@code DELETE} ke penanda yang mengatur tombol per baris &mdash; berbeda dari
 *   pola &laquo;inversi hak akses&raquo; yang berulang di layar master lain. Halaman ini juga
 *   <b>tidak</b> ada di {@code CommonPrivilages.MUST_CHECKED}, tetapi karena selalu di-<i>include</i>
 *   di dalam {@code jurusan.zul}/{@code fakultas.zul} yang <b>ada</b> di daftar itu, gerbang READ
 *   tetap berlaku lewat halaman induknya.</li>
 *   <li><b>Berkas fisiknya tetap tunduk pada IDOR {@code LampiranLain} yang sudah dikenal.</b>
 *   Dokumen bukti akreditasi diunggah lewat
 *   {@code LampiranLain.createDownloadUploadFileLain(..., BerkasHasilAkreditasiPunyaNama.class
 *   .getName(), ...)}, sehingga berbagi mekanisme pengambilan yang sama dengan lampiran lain di AIS.
 *   Entity ini sendiri tidak menambah permukaan serangan baru; ia hanya menambah <i>satu jenis
 *   konten lagi</i> (dokumen internal borang/visitasi) ke dalam himpunan yang terjangkau lewat jalur
 *   tersebut. Tidak ada kredensial, data pribadi mahasiswa, maupun nilai yang disimpan di tabel
 *   ini.</li>
 * </ul>
 *
 * @see BerkasHasilAkreditasiPunyaNama
 * @see GeneralValueObject
 * @see ais.database.model.file.LampiranLain
 * @see DspaceInformation
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "berkas_hasil_akreditasi")

public class BerkasHasilAkreditasi extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya <b>disalin apa adanya</b> dari {@link Bank} bersama sisa
	 * kerangka kelas ini, sehingga sama persis dengan milik beberapa entity lain di paket ini
	 * (termasuk {@link BerkasHasilAkreditasiPunyaNama}). Tidak berpengaruh pada pemetaan Hibernate;
	 * hanya relevan bila objek diserialisasi ke sesi ZK atau cache terdistribusi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris. Dibangkitkan {@code SERIAL} PostgreSQL. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini. Diisi otomatis oleh {@link #onUpdate()}. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini. Diisi otomatis oleh {@link #onUpdate()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini (bagian jejak audit ringan yang
	 * berdampingan dengan Hibernate Envers).
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah melewati {@link #onUpdate()}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Efek samping yang tidak biasa:</b> nilai {@code null} atau string kosong/spasi
	 * <b>diabaikan diam-diam</b> &mdash; nilai lama dipertahankan dan tidak ada exception yang
	 * dilempar. Pola ini seragam di seluruh entity paket ini dan berfungsi menjaga agar jejak audit
	 * tidak terhapus oleh alur yang kebetulan menyetel nilai kosong; konsekuensinya, jejak audit
	 * <b>tidak dapat dikosongkan kembali</b> lewat setter ini.</p>
	 *
	 * @param olehId id pengguna baru; {@code null}/kosong tidak berpengaruh apa pun
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau kosong/spasi <b>diabaikan diam-diam</b> sehingga nilai lama bertahan.
	 *
	 * @param oleh nama pengguna baru; {@code null}/kosong tidak berpengaruh apa pun
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila baris belum pernah melewati {@link #onUpdate()}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * dari konteks pengguna yang sedang aktif lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, tepat sebelum pernyataan
	 * UPDATE dikirim ke database. <b>Tidak dipanggil pada INSERT</b>, sehingga baris yang baru dibuat
	 * tidak punya jejak pembuat &mdash; identitas pembuat baru terekam setelah penyuntingan pertama.
	 * Jangan dipanggil manual dari kode aplikasi.
	 *
	 * <p>Pada baris deklarasi yang sama juga dideklarasikan field {@code tanggal_dirubah}, yang
	 * diinisialisasi ke waktu server saat objek Java dibuat ({@code ais.ui.util.WaktuUtil.getDate()})
	 * sehingga baris baru tetap punya stempel waktu meski belum pernah di-update. Tata letak satu
	 * baris ini adalah hasil penyisipan otomatis lintas entity; jangan dirapikan tanpa memeriksa
	 * kembali seluruh paket.</p>
	 *
	 * <p>Karena kelas ini tidak punya <i>getter</i> yang menulis balik ke field (lihat javadoc
	 * kelas), callback ini hanya terpicu oleh perubahan yang benar-benar diniatkan pengguna &mdash;
	 * sekadar merender daftar berkas akreditasi tidak menghasilkan UPDATE.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya <b>tidak</b> dipanggil kode aplikasi:
	 * nilainya diisi otomatis oleh {@link #onUpdate()} sesaat sebelum UPDATE. Tanpa validasi &mdash;
	 * {@code null} maupun waktu di masa depan diterima apa adanya.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}, presisi
	 * {@code TIMESTAMP}). Untuk baris yang belum pernah di-update, nilainya adalah waktu server saat
	 * <b>objek Java dibuat</b> &mdash; bukan waktu baris disimpan ke database.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat
	 *         lewat konstruktor
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks berformat {@code "<id>-<nama>"}.
	 *
	 * <p><b>Perhatian:</b> membaca <b>field</b> {@code nama} secara langsung, bukan lewat
	 * {@link #getNama()}. Akibatnya spasi tepi tidak dipangkas dan nilai {@code null} dicetak apa
	 * adanya, sehingga hasilnya bisa berbunyi {@code "12-null"} atau berbeda dari {@code getNama()}
	 * untuk objek yang sama.</p>
	 *
	 * <p>Nilai ini ikut terlihat pengguna secara tidak langsung: {@code toString()} milik entity
	 * anak {@link BerkasHasilAkreditasiPunyaNama} merangkai {@code berkasHasilAkreditasi + " - " +
	 * keterangan}, sehingga id numerik baris ini muncul pada bilah progres sinkronisasi DSpace di
	 * {@code JurusanAction}/{@code FakultasAction}/{@code PerguruanTinggiAction}.</p>
	 *
	 * @return gabungan id dan nama kategori, dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Nama/label kategori berkas akreditasi (mis. &laquo;Akreditasi BAN-PT 2024&raquo;). Satu-satunya
	 * kolom wajib ({@code nullable = false}) dan satu-satunya kolom yang dapat dicari dari layar.
	 * Nilai inilah yang dipakai sebagai nama <i>collection</i> DSpace. Lihat {@link #getNama()}.
	 */
	private String nama;

	/**
	 * Keterangan bebas. Perhatikan {@link #getKeterangan()} di kelas ini <b>bisa</b> mengembalikan
	 * {@code null}, berbeda dari kontrak {@link GeneralValueObject#getKeterangan()}.
	 */
	private String keterangan;

	/**
	 * Nama asesor pertama sebagai <b>teks bebas</b> &mdash; bukan relasi ke {@link Dosen} maupun
	 * {@link Pegawai}. Tidak ada validasi apa pun; kolom boleh kosong.
	 */
	private String asesor1;

	/** Nama asesor kedua, teks bebas dengan sifat sama seperti {@link #asesor1}. */
	private String asesor2;

	/**
	 * Tanggal kategori akreditasi ini (dalam praktik: tanggal visitasi/penerbitan). Dipetakan
	 * {@code TemporalType.DATE} sehingga komponen jamnya dibuang. Boleh kosong. Jangan dikelirukan
	 * dengan field {@code tanggal} milik entity anak, yang bermakna tanggal terbit dokumen.
	 */
	private Date tanggal;

	/**
	 * Jurusan/program studi pemilik kategori ini (FK {@code jurusan}). Terisi bila layar dibuka dari
	 * tab pada {@code JurusanAction}. Lihat {@link #getJurusan()}.
	 */
	private Jurusan jurusan;

	/**
	 * Fakultas pemilik kategori ini (FK {@code fakultas}). Terisi bila layar dibuka dari tab pada
	 * {@code FakultasAction}. Lihat {@link #getFakultas()}.
	 */
	private Fakultas fakultas;

	/**
	 * Perguruan tinggi pemilik kategori ini (FK {@code perguruan_tinggi}). Terisi bila layar dibuka
	 * dari tab pada {@code PerguruanTinggiAction}. Lihat {@link #getPerguruanTinggi()} untuk
	 * penyaring instance transien yang khusus ada pada relasi ini.
	 */
	private PerguruanTinggi perguruanTinggi;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate/JPA dan dipakai langsung oleh
	 * {@code BerkasHasilAkreditasiAction#onAdd} untuk menyiapkan dialog &laquo;Tambah Berkas
	 * Akreditasi&raquo;. Seluruh field dibiarkan pada nilai bawaannya, kecuali
	 * {@code tanggal_dirubah} yang langsung diisi waktu server (lihat {@link #onUpdate()}).
	 */
	public BerkasHasilAkreditasi() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dipetakan sebagai {@code IDENTITY} dengan {@code insertable = false}: nilainya sepenuhnya
	 * dibangkitkan urutan {@code SERIAL} PostgreSQL, sehingga memanggil {@link #setId(Long)} sebelum
	 * INSERT tidak berpengaruh. Bernilai {@code null} untuk objek yang belum pernah disimpan &mdash;
	 * kondisi {@code getId() == null} itulah yang dipakai {@code BerkasHasilAkreditasiAction#init}
	 * untuk membedakan judul dialog &laquo;Tambah&raquo; dari &laquo;Ubah&raquo;, dan dipakai
	 * {@code onSave()} untuk memutuskan perlu tidaknya {@code session.load(...)}.</p>
	 *
	 * @return kunci utama, atau {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Praktis hanya dipakai Hibernate saat memuat/menyimpan baris; menyetelnya
	 * manual pada objek baru tidak berpengaruh karena kolomnya {@code insertable = false}.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama/label kategori akreditasi, sudah dipangkas spasi tepinya.
	 *
	 * <p>Karena pemetaan memakai <i>property access</i>, nilai yang benar-benar ditulis ke kolom
	 * {@code nama} adalah hasil {@code trim()} ini &mdash; namun field internal <b>tidak</b> ikut
	 * diubah, sehingga membaca nama tidak membuat baris menjadi <i>dirty</i> (bandingkan
	 * {@code Kota#getNama()} yang menulis balik). Nilai kembalian dipakai sebagai judul tombol
	 * riwayat revisi di grid dan sebagai {@code name}/{@code shortDescription} <i>collection</i>
	 * DSpace.</p>
	 *
	 * @return nama kategori tanpa spasi tepi, atau {@code null} bila field belum diisi &mdash;
	 *         padahal kolomnya {@code nullable = false}, sehingga menyimpan objek seperti itu akan
	 *         ditolak database
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama/label kategori akreditasi. Tanpa validasi, tanpa {@code trim()}, tanpa
	 * pemeriksaan duplikasi &mdash; keharusan mengisi kolom ini hanya ditegakkan di
	 * {@code BerkasHasilAkreditasiAction#onSave}, dan panjang maksimum 255 karakter hanya ditegakkan
	 * database.
	 *
	 * @param nama nama kategori baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini, <b>apa adanya</b>.
	 *
	 * <p><b>Meng-<i>override</i> {@link GeneralValueObject#getKeterangan()} dan membalik
	 * kontraknya:</b> versi induk menjamin tidak pernah {@code null} (mengembalikan {@code ""}),
	 * versi ini <b>bisa</b> {@code null}. Cabang pengurutan berdasarkan {@code keterangan} di
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} sudah menjaga diri dengan pemeriksaan
	 * {@code != null}, jadi tidak ada risiko NPE dari sana &mdash; tetapi pemanggil lain yang
	 * memercayai janji kelas induk perlu berhati-hati.</p>
	 *
	 * @return keterangan bebas, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi; {@code null} diterima dan akan terbaca kembali
	 * sebagai {@code null} lewat {@link #getKeterangan()}.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nama asesor pertama apa adanya (tanpa {@code trim()}, tanpa normalisasi).
	 * Ditampilkan sebagai kolom &laquo;Asesor I&raquo; pada grid.
	 *
	 * @return nama asesor pertama, atau {@code null} bila belum diisi
	 */
	public String getAsesor1() {
		return asesor1;
	}

	/**
	 * Menyetel nama asesor pertama. Teks bebas tanpa validasi maupun pencocokan ke master
	 * dosen/pegawai.
	 *
	 * @param asesor1 nama asesor pertama
	 */
	public void setAsesor1(String asesor1) {
		this.asesor1 = asesor1;
	}

	/**
	 * Mengembalikan nama asesor kedua apa adanya. Ditampilkan sebagai kolom &laquo;Asesor II&raquo;
	 * pada grid.
	 *
	 * @return nama asesor kedua, atau {@code null} bila belum diisi
	 */
	public String getAsesor2() {
		return asesor2;
	}

	/**
	 * Menyetel nama asesor kedua. Teks bebas tanpa validasi maupun pencocokan ke master
	 * dosen/pegawai.
	 *
	 * @param asesor2 nama asesor kedua
	 */
	public void setAsesor2(String asesor2) {
		this.asesor2 = asesor2;
	}

	/**
	 * Mengembalikan tanggal kategori akreditasi ini. Dipetakan {@code TemporalType.DATE} sehingga
	 * bagian jam diabaikan saat disimpan/dimuat. Renderer grid menampilkan string kosong bila
	 * nilainya {@code null}.
	 *
	 * @return tanggal akreditasi, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menyetel tanggal kategori akreditasi. Tanpa validasi; {@code null} maupun tanggal di masa
	 * depan diterima. Diisi dari komponen {@code MyDatebox} pada dialog simpan.
	 *
	 * @param tanggal tanggal baru
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan jurusan/program studi pemilik kategori ini (FK {@code jurusan}, boleh
	 * {@code null}).
	 *
	 * <p>Relasi {@code @ManyToOne} dengan {@code cascade = {PERSIST, MERGE}} dan
	 * {@code @Fetch(FetchMode.SELECT)} &mdash; artinya satu SELECT terpisah dijalankan saat relasi
	 * ini pertama kali disentuh, bukan JOIN. Getter mengembalikan field apa adanya (bisa berupa
	 * proxy Hibernate yang belum terinisialisasi) dan <b>tidak</b> memanggil
	 * {@code GeneralValueObject#check(...)} seperti sebagian entity lain di paket ini.</p>
	 *
	 * <p>Nilai ini adalah cabang <b>pertama</b> yang diperiksa
	 * {@code JurusanAction#getDspaceBerkasHasilAkreditasi}: bila terisi, <i>collection</i> DSpace
	 * dibuat di bawah <i>community</i> jurusan dan kedua relasi lainnya diabaikan.</p>
	 *
	 * @return jurusan pemilik, atau {@code null} bila kategori ini dimiliki fakultas/perguruan
	 *         tinggi (atau tidak dimiliki siapa pun, mis. baris hasil impor Excel)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/**
	 * Menyetel jurusan pemilik kategori ini.
	 *
	 * <p>Selalu dipanggil {@code BerkasHasilAkreditasiAction#onSave} dengan nilai yang berasal dari
	 * parameter URL {@code jurusan} saat layar dibuka &mdash; termasuk {@code null} bila layar dibuka
	 * dari tab fakultas atau perguruan tinggi. Menyunting baris karenanya <b>menulis ulang</b>
	 * kepemilikannya, tidak mempertahankan nilai yang tersimpan. Tidak ada pemeriksaan bahwa hanya
	 * satu dari tiga relasi pemilik yang terisi.</p>
	 *
	 * @param jurusan jurusan pemilik baru; {@code null} berarti kategori ini tidak dimiliki jurusan
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan fakultas pemilik kategori ini (FK {@code fakultas}, boleh {@code null}).
	 * Karakteristik pemetaannya identik dengan {@link #getJurusan()} ({@code PERSIST}/{@code MERGE},
	 * {@code FetchMode.SELECT}, tanpa penyaring nilai kembalian).
	 *
	 * <p>Merupakan cabang <b>kedua</b> pada pemilihan induk <i>community</i> DSpace: hanya dipakai
	 * bila {@link #getJurusan()} bernilai {@code null}.</p>
	 *
	 * @return fakultas pemilik, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		return fakultas;
	}

	/**
	 * Menyetel fakultas pemilik kategori ini. Sama seperti {@link #setJurusan(Jurusan)}, nilainya
	 * selalu ditulis ulang dari parameter URL {@code fakultas} pada setiap penyimpanan.
	 *
	 * @param fakultas fakultas pemilik baru; {@code null} berarti kategori ini tidak dimiliki
	 *                 fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan perguruan tinggi pemilik kategori ini (FK {@code perguruan_tinggi}, boleh
	 * {@code null}).
	 *
	 * <p><b>Satu-satunya getter relasi yang menyaring nilai kembaliannya:</b> bila field berisi objek
	 * {@link PerguruanTinggi} yang belum punya id (instance transien/baru), getter mengembalikan
	 * {@code null} alih-alih objek tersebut. Karena Hibernate membaca properti lewat getter
	 * (<i>property access</i>), efeknya bukan sekadar kosmetik &mdash; kolom {@code perguruan_tinggi}
	 * ikut ditulis {@code NULL} saat <i>flush</i>. Penyaring serupa <b>tidak ada</b> pada
	 * {@link #getJurusan()} dan {@link #getFakultas()}.</p>
	 *
	 * <p>Field <b>tidak</b> diubah oleh getter ini &mdash; jadi ini bukan <i>getter</i> yang menulis
	 * balik maupun destruktif; penyaringannya murni pada nilai kembalian.</p>
	 *
	 * <p>Merupakan cabang <b>ketiga/terakhir</b> pada pemilihan induk <i>community</i> DSpace; bila
	 * ketiganya {@code null}, {@code getDspaceBerkasHasilAkreditasi} mengembalikan {@code null} dan
	 * dokumen anaknya tidak dapat disinkronkan.</p>
	 *
	 * @return perguruan tinggi pemilik, atau {@code null} bila belum diisi <b>atau</b> objek yang
	 *         tersimpan belum punya id
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "perguruan_tinggi", nullable = true)
	public PerguruanTinggi getPerguruanTinggi() {
		return perguruanTinggi == null || perguruanTinggi.getId() == null ? null : perguruanTinggi;
	}

	/**
	 * Menyetel perguruan tinggi pemilik kategori ini. Menerima objek transien apa adanya &mdash;
	 * penyaringannya baru terjadi saat dibaca lewat {@link #getPerguruanTinggi()}. Seperti dua setter
	 * relasi lainnya, nilainya ditulis ulang dari parameter URL {@code perguruanTinggi} pada setiap
	 * penyimpanan.
	 *
	 * @param perguruanTinggi perguruan tinggi pemilik baru; {@code null} berarti kategori ini tidak
	 *                        dimiliki perguruan tinggi
	 */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

}
