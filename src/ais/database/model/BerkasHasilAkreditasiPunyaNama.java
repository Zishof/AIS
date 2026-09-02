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
 * <h2>BerkasHasilAkreditasiPunyaNama &mdash; <i>metadata bibliografi</i> satu dokumen bukti
 * akreditasi (tabel {@code public.berkas_hasil_akreditasi_punya_nama}).</h2>
 *
 * <p>Satu baris entity ini adalah <b>satu dokumen</b> bukti akreditasi beserta katalog
 * bibliografinya: kode/identifier ({@code kode}), judul ({@code nama}), penulis
 * ({@code penulis}), editor ({@code editor}), abstrak ({@code abstrak}), kata kunci
 * ({@code keyword}), penerbit ({@code diterbitkanoleh}), tanggal terbit ({@code tanggal}), dan
 * keterangan bebas ({@code keterangan}). Baris ini <b>wajib</b> menempel pada satu
 * {@link BerkasHasilAkreditasi} lewat FK {@code berkas_hasil_akreditasi} ({@code nullable = false})
 * &mdash; yaitu wadah/kategori berlabel (&laquo;map borang&raquo; / periode visitasi) yang
 * mengelompokkan dokumen-dokumen ini. Entity induk itu sendiri tidak memuat kolom hasil akreditasi
 * apa pun; pasangan induk&ndash;anak inilah yang membentuk struktur <i>kategori &rarr; dokumen</i>.</p>
 *
 * <p><b>Berkas fisiknya TIDAK disimpan di entity ini.</b> Kolom-kolom di sini murni metadata;
 * PDF/gambar/dokumen sesungguhnya dipegang {@link ais.database.model.file.LampiranLain} dengan
 * kunci majemuk {@code (ref = id baris ini, jenis =
 * "ais.database.model.BerkasHasilAkreditasiPunyaNama")}. Dua titik pemakaian yang membuktikannya:</p>
 * <ul>
 *   <li>{@code BerkasHasilAkreditasiPunyaNamaHelper.DetailBerkasHasilAkreditasiPunyaNamaRenderer#render}
 *   memanggil {@code LampiranLain.createDownloadUploadFileLain(hbox, }{@link #getId()}{@code ,
 *   BerkasHasilAkreditasiPunyaNama.class.getName(), "Lampiran", false, ...)} &mdash; kotak
 *   unggah/unduh berkas menempel pada tiap baris grid;</li>
 *   <li>{@code JurusanAction#getDspace(String, BerkasHasilAkreditasiPunyaNama, boolean)} memanggil
 *   {@code LampiranLain.ambil(}{@link #getId()}{@code , BerkasHasilAkreditasiPunyaNama.class.getName())}
 *   untuk mengambil berkas yang akan diunggah ke DSpace.</li>
 * </ul>
 * <p>Konsekuensi struktural: relasi ke lampiran bersifat <b>satu berkas per baris</b> (helper
 * {@code LampiranLain.ambil} mengambil satu hasil saja), dan menghapus baris ini
 * <b>tidak</b> menghapus berkas di {@code LampiranLain} &mdash; tidak ada FK maupun
 * {@code cascade} yang menghubungkan keduanya, sehingga berkas menjadi yatim di penyimpanan.</p>
 *
 * <h3>Integrasi repositori DSpace: entity ini adalah <i>item</i></h3>
 *
 * <p>Pemetaan ke DSpace mengikuti hierarki induk&ndash;anak secara langsung:</p>
 * <ul>
 *   <li>{@link BerkasHasilAkreditasi} (induk) &rarr; <b>collection</b> DSpace, lewat
 *   {@code JurusanAction#getDspaceBerkasHasilAkreditasi};</li>
 *   <li>entity ini (anak) &rarr; <b>item</b> DSpace di dalam collection tersebut, lewat
 *   {@code JurusanAction#getDspace(String, BerkasHasilAkreditasiPunyaNama, boolean)} yang
 *   memetakan field-field di sini menjadi metadata Dublin Core:
 *   {@code dc.contributor.author} &larr; {@link #getPenulis()},
 *   {@code dc.contributor.editor} &larr; {@link #getEditor()},
 *   {@code dc.description} &larr; {@link #getKeterangan()},
 *   {@code dc.description.abstract} &larr; {@link #getAbstrak()},
 *   {@code dc.identifier} &larr; {@link #getKode()},
 *   {@code dc.title} &larr; {@link #getNama()},
 *   {@code dc.subject} &larr; {@link #getKeyword()},
 *   {@code dc.publisher} &larr; {@link #getDiterbitkanoleh()},
 *   {@code dc.date.issued} &larr; {@link #getTanggal()} (hanya bila tidak {@code null}), dan
 *   {@code dc.identifier.uri} &larr; URI lampiran {@code LampiranLain}.</li>
 * </ul>
 * <p><b>Hanya kelas ini</b> yang terdaftar di {@code DspaceInformation.linksForClass} &mdash;
 * {@link BerkasHasilAkreditasi} <b>tidak</b>. Itu konsisten dengan perannya: {@code linksForClass}
 * menjadi penjaga {@code DspaceInformation#showLink(GeneralValueObject, Long)}, yang hanya
 * merender tautan publik untuk objek yang punya <i>handle</i> DSpace tersendiri. Sebuah
 * <i>collection</i> memang tidak pernah ditautkan dari grid AIS; yang ditautkan adalah
 * <i>item</i>-nya. Tombol &laquo;Ekspor Berkas ke DSpace&raquo;/&laquo;Batalkan Ekspor Berkas&raquo;
 * di {@code FakultasAction}, {@code JurusanAction}, dan {@code PerguruanTinggiAction} pun
 * meng-<i>query</i> kelas ini (bukan induknya) dan menghapus DSpace {@code items/<uuid>}.</p>
 *
 * <h3>Alur pemakaian</h3>
 *
 * <ol>
 *   <li>Layar {@code /pages/master/berkas_hasil_akreditasi.zul} ({@code BerkasHasilAkreditasiAction})
 *   menampilkan daftar {@link BerkasHasilAkreditasi} sebagai baris yang dapat dibentangkan. Saat
 *   dibentangkan, {@code BerkasHasilAkreditasiPunyaNamaHelper#display} membangun grid dokumen
 *   anak &mdash; itulah tempat entity ini muncul.</li>
 *   <li>Tombol &laquo;Lampiran Baru&raquo; membuka dialog form yang mengisi seluruh field bisnis,
 *   lalu menyimpan lewat {@code Common.refreshUpdate(...)}. Relasi induk disetel di
 *   {@code save} listener dari konteks layar, jadi baris hasil UI selalu punya induk.</li>
 *   <li>Kolom &laquo;Keterangan&raquo; pada grid adalah {@code MyTextbox} yang <b>langsung
 *   menyimpan</b> ke database pada event {@code onChange} &mdash; penyuntingan <i>inline</i> tanpa
 *   dialog dan tanpa konfirmasi.</li>
 *   <li>Tombol hapus hanya tampil bagi pengguna dengan hak {@code CommonPrivilages.DELETE};
 *   pencarian memakai {@code ilike} pada {@code nama} yang di-<i>scope</i> ke induk yang sedang
 *   dibuka.</li>
 *   <li>Tombol &laquo;Download&raquo; mengekspor kolom {@code id}, {@code berkasHasilAkreditasi},
 *   {@code nama}, {@code keterangan} ke Excel, ditambah kolom &laquo;Lampiran&raquo; berisi
 *   <i>hyperlink</i> berkas &mdash; lihat catatan bug di bawah.</li>
 * </ol>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 *
 * <ol>
 *   <li><b>Identitas &amp; kunci</b>: {@link #getId()}/{@link #setId(Long)} &mdash; {@code IDENTITY},
 *   {@code insertable = false}.</li>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()}
 *   dan callback {@link #onUpdate()}. Kelas ber-{@code @Audited} (Hibernate Envers); riwayatnya
 *   dibaca tombol {@code RevisiHelper.createNewRevisi(BerkasHasilAkreditasiPunyaNama.class, ...)}
 *   pada kolom pertama grid.</li>
 *   <li><b>Relasi induk</b>: {@link #getBerkasHasilAkreditasi()} &mdash; satu-satunya relasi kelas
 *   ini.</li>
 *   <li><b>Metadata bibliografi</b>: {@link #getKode()}, {@link #getNama()}, {@link #getPenulis()},
 *   {@link #getEditor()}, {@link #getAbstrak()}, {@link #getKeyword()},
 *   {@link #getDiterbitkanoleh()}, {@link #getTanggal()}, {@link #getKeterangan()}.</li>
 *   <li><b>Utilitas</b>: {@link #toString()}.</li>
 * </ol>
 *
 * <p><b>Tidak ada method bisnis, tidak ada method/query statis, dan tidak ada koleksi anak yang
 * dipetakan.</b> Kelas ini murni <i>value object</i>; semua logika (pencarian, simpan, ekspor
 * Excel, ekspor DSpace) berada di {@code BerkasHasilAkreditasiPunyaNamaHelper},
 * {@code JurusanAction}, {@code FakultasAction}, dan {@code PerguruanTinggiAction}. Arah relasi ke
 * induk bersifat <b>unidirectional</b>: hanya anak yang menunjuk induk, {@link BerkasHasilAkreditasi}
 * tidak memetakan {@code Set} dokumen.</p>
 *
 * <h3>Verifikasi pola berulang paket ini</h3>
 *
 * <p>Tiga kuirk yang lazim ditemukan di entity {@code ais.database.model} <b>diperiksa langsung pada
 * kode berkas ini</b>, dengan hasil:</p>
 * <ul>
 *   <li><b>Getter yang menulis balik ke field lalu ikut ter-flush ke DB: TIDAK ADA.</b> Seluruh
 *   getter di kelas ini mengembalikan field apa adanya tanpa normalisasi, {@code trim()}, maupun
 *   penugasan. Merender daftar dokumen tidak membuat baris menjadi <i>dirty</i>.</li>
 *   <li><b>Getter yang menutup sesi Hibernate: TIDAK ADA.</b> {@link #getBerkasHasilAkreditasi()}
 *   pun tidak memanggil {@code check(...)} milik {@link GeneralValueObject}.</li>
 *   <li><b>Getter destruktif (menghapus/menimpa data saat dibaca): TIDAK ADA.</b></li>
 * </ul>
 *
 * <h3>Kuirk &amp; hal non-obvious</h3>
 *
 * <ul>
 *   <li><b>Judul generator salah salin.</b> Javadoc bawaan berbunyi <i>"Bank generated by
 *   hbm2java"</i>. Berkas ini bukan {@link Bank}; judul itu sisa salin-tempel dari kelas sumber yang
 *   menular ke puluhan entity paket ini. Header generator di baris 3 dipertahankan sebagai jejak
 *   sejarah, blok ini menggantikan judul satu barisnya. {@code serialVersionUID}-nya pun identik
 *   dengan milik {@link Bank}, {@link BerkasHasilAkreditasi}, dan {@link DspaceInformation}.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 *   duplikasi yang boleh dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak biasa &mdash;
 *   bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate sama sekali
 *   tidak memetakan properti kelas induk. Setiap entity turunan <b>wajib</b> mendeklarasikan sendiri
 *   kolom-kolom itu. Hal yang sama berlaku untuk {@code nama}, {@code kode}, dan
 *   {@code keterangan}.</li>
 *   <li><b>{@link #getKeterangan()} meng-<i>override</i> versi induk dan membalik kontraknya.</b>
 *   {@code GeneralValueObject#getKeterangan()} menjamin tidak pernah {@code null} (mengembalikan
 *   {@code ""}); versi di kelas ini mengembalikan field apa adanya, jadi <b>bisa {@code null}</b>.
 *   Pola yang sama sudah dikenal di beberapa entity lain paket ini &mdash; variasi arsitektural
 *   yang mapan, bukan anomali. Akibat praktisnya: metadata {@code dc.description} pada ekspor
 *   DSpace <b>lenyap tanpa pesan</b> untuk dokumen yang keterangannya kosong, karena
 *   {@code org.json.JSONObject.put(key, null)} justru <i>menghapus</i> kunci alih-alih menuliskan
 *   nilai kosong. Tidak ada NPE &mdash; kegagalannya senyap.</li>
 *   <li><b>{@link #toString()} membaca field langsung, bukan getter.</b> Hasilnya
 *   {@code "<induk> - <keterangan>"} &mdash; memakai <b>keterangan</b>, bukan {@code nama} seperti
 *   lazimnya. Untuk dokumen tanpa keterangan hasilnya berbunyi {@code "... - null"}, dan itulah teks
 *   yang tampil di bilah progres ekspor DSpace ({@code "Sedang memproses data ... - null (12 %)"}).
 *   Selain itu ia merangkai {@code berkasHasilAkreditasi} yang <b>lazy proxy</b>: memanggil
 *   {@code toString()} di luar sesi Hibernate dapat melempar
 *   {@code LazyInitializationException}.</li>
 *   <li><b>Tanpa {@code equals}/{@code hashCode} sendiri.</b> Keduanya diwarisi dari
 *   {@link GeneralValueObject}, yang membandingkan berdasarkan {@link #getId()}; dua objek baru
 *   (id masih {@code null}) dianggap sama hanya bila referensinya sama.</li>
 *   <li><b>{@code kode} bukan kunci unik.</b> Tidak ada {@code unique = true} maupun validasi di
 *   layar &mdash; dua dokumen di kategori yang sama boleh berkode identik, dan kode itu tetap
 *   dikirim sebagai {@code dc.identifier} ke DSpace.</li>
 *   <li><b>Panjang kolom.</b> Hanya {@code keterangan} dan {@code abstrak} yang dipetakan sebagai
 *   {@code text}; {@code nama}, {@code penulis}, {@code editor}, {@code keyword}, dan
 *   {@code diterbitkanoleh} memakai {@code varchar(255)} bawaan Hibernate. Judul atau daftar penulis
 *   yang panjang akan ditolak database saat disimpan, bukan dipotong.</li>
 *   <li><b>Kolom &laquo;Lampiran&raquo; pada ekspor Excel selalu kosong (bug di helper, bukan di
 *   entity ini).</b> {@code BerkasHasilAkreditasiPunyaNamaHelper#display} mengekspor 4 kolom
 *   ({@code id}, {@code berkasHasilAkreditasi}, {@code nama}, {@code keterangan}) sehingga header
 *   &laquo;LAMPIRAN&raquo; jatuh di indeks 4 dan &laquo;NO URUT&raquo; di indeks 5; tetapi
 *   {@code dataAddingHelper.process(row, 5, ...)} menulis <i>hyperlink</i>-nya di indeks <b>5</b>,
 *   dan {@code CommonDownloadUpload} kemudian menimpa sel itu dengan nomor urut. Hasilnya kolom
 *   &laquo;LAMPIRAN&raquo; tidak pernah terisi. Dicatat apa adanya; <b>tidak diperbaiki</b> di sini
 *   karena berkas ini hanya didokumentasikan.</li>
 * </ul>
 *
 * @see BerkasHasilAkreditasi
 * @see ais.database.model.file.LampiranLain
 * @see DspaceInformation
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "berkas_hasil_akreditasi_punya_nama")

public class BerkasHasilAkreditasiPunyaNama extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya <b>disalin apa adanya</b> dari {@link Bank} bersama sisa
	 * kerangka kelas ini, sehingga sama persis dengan milik beberapa entity lain di paket ini
	 * (termasuk {@link BerkasHasilAkreditasi} dan {@link DspaceInformation}). Tidak berpengaruh pada
	 * pemetaan Hibernate; hanya relevan bila objek diserialisasi ke sesi ZK atau cache
	 * terdistribusi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris. Dibangkitkan {@code SERIAL} PostgreSQL. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini. Diisi otomatis oleh {@link #onUpdate()}. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini. Diisi otomatis oleh {@link #onUpdate()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini (jejak audit ringan yang
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
	 * kelas), callback ini hanya terpicu oleh perubahan yang benar-benar diniatkan pengguna. Perlu
	 * dicatat: penyuntingan <i>inline</i> kolom &laquo;Keterangan&raquo; di grid memicu jalur ini
	 * pada setiap {@code onChange}, jadi jejak audit dapat bertambah tanpa pengguna membuka dialog
	 * form sama sekali.</p>
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
	 * Representasi teks berformat {@code "<induk> - <keterangan>"}.
	 *
	 * <p>Meng-<i>override</i> {@code GeneralValueObject#toString()} yang normalnya merangkai
	 * {@code kode} + {@code nama}. Perhatikan dua hal yang mudah terlewat:</p>
	 * <ul>
	 *   <li>bagian kedua memakai <b>keterangan</b>, bukan judul dokumen ({@code nama}); untuk baris
	 *   tanpa keterangan hasilnya berbunyi {@code "... - null"};</li>
	 *   <li>kedua nilai dibaca dari <b>field</b> langsung, bukan lewat getter, dan
	 *   {@code berkasHasilAkreditasi} adalah relasi <i>lazy</i> &mdash; memanggil method ini di luar
	 *   sesi Hibernate bisa melempar {@code LazyInitializationException}, di dalam sesi ia memicu
	 *   SELECT tambahan ke tabel induk.</li>
	 * </ul>
	 *
	 * <p>Dipakai antara lain sebagai teks bilah progres ekspor/pembatalan ekspor DSpace di
	 * {@code FakultasAction}, {@code JurusanAction}, dan {@code PerguruanTinggiAction}.</p>
	 *
	 * @return teks gabungan induk dan keterangan; tidak pernah {@code null}
	 */
	public String toString() {
		return berkasHasilAkreditasi + " - " + keterangan;
	}

	/**
	 * Kategori/wadah pemilik dokumen ini. Wajib terisi ({@code nullable = false}). Lihat
	 * {@link #getBerkasHasilAkreditasi()}.
	 */
	private BerkasHasilAkreditasi berkasHasilAkreditasi;

	/** Kode/identifier dokumen, teks bebas dan tidak unik. Lihat {@link #getKode()}. */
	private String kode;

	/**
	 * Judul dokumen. Dideklarasikan ulang di sini karena {@link GeneralValueObject} tidak dipetakan
	 * Hibernate. Lihat {@link #getNama()}.
	 */
	private String nama;

	/** Nama penulis dokumen, teks bebas. Lihat {@link #getPenulis()}. */
	private String penulis;

	/** Nama editor dokumen, teks bebas. Lihat {@link #getEditor()}. */
	private String editor;

	/** Abstrak/ringkasan dokumen (kolom {@code text}). Lihat {@link #getAbstrak()}. */
	private String abstrak;

	/** Kata kunci dokumen, satu string bebas tanpa pemisah baku. Lihat {@link #getKeyword()}. */
	private String keyword;

	/** Nama penerbit dokumen, teks bebas. Lihat {@link #getDiterbitkanoleh()}. */
	private String diterbitkanoleh;

	/**
	 * Tanggal terbit dokumen, diinisialisasi ke waktu server saat objek Java dibuat sehingga dialog
	 * tambah selalu terisi tanggal hari ini. Lihat {@link #getTanggal()}.
	 */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Keterangan bebas (kolom {@code text}). Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} tidak dipetakan Hibernate. Lihat {@link #getKeterangan()}.
	 */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Selain dipakai penyedia persistensi,
	 * dipanggil langsung oleh {@code BerkasHasilAkreditasiPunyaNamaHelper#onAdd} dan listener
	 * {@code save} dialog untuk membuat dokumen baru. Objek hasilnya sudah memiliki
	 * {@code tanggal} dan {@code tanggal_dirubah} berisi waktu server saat ini; seluruh field lain
	 * {@code null}, termasuk relasi induk yang wajib diisi sebelum disimpan.
	 */
	public BerkasHasilAkreditasiPunyaNama() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Selain sebagai identitas persistensi, nilai ini menjadi <b>kunci pencarian lampiran</b>:
	 * berkas fisik dokumen disimpan di {@link ais.database.model.file.LampiranLain} dengan
	 * {@code ref} = nilai ini dan {@code jenis} = nama lengkap kelas ini. Nilai yang sama juga
	 * dipakai sebagai {@code refId} baris {@link DspaceInformation} yang menyimpan UUID item
	 * DSpace.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris. Kolomnya {@code insertable = false} dan dibangkitkan database,
	 * jadi kode aplikasi praktis tidak pernah memanggil setter ini; Hibernate yang mengisinya
	 * setelah INSERT.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kategori/wadah berkas akreditasi pemilik dokumen ini (FK
	 * {@code berkas_hasil_akreditasi}, <b>wajib terisi</b>).
	 *
	 * <p>Relasi ini <b>unidirectional</b>: hanya sisi ini yang dipetakan, {@link BerkasHasilAkreditasi}
	 * tidak memiliki koleksi dokumen. Pengambilan memakai {@code FetchMode.SELECT}, artinya induk
	 * dimuat lewat SELECT terpisah saat pertama kali diakses &mdash; merender satu halaman grid
	 * dapat menghasilkan SELECT tambahan per baris (masalah N+1 klasik), termasuk lewat
	 * {@link #toString()}.</p>
	 *
	 * <p>{@code cascade = {PERSIST, MERGE}} berarti menyimpan dokumen ikut menyimpan/menggabungkan
	 * induknya bila induk itu objek baru atau <i>detached</i>; tidak ada {@code REMOVE}, jadi
	 * menghapus dokumen tidak pernah menghapus kategorinya.</p>
	 *
	 * @return kategori berkas akreditasi pemilik; {@code null} hanya untuk objek yang belum
	 *         dilengkapi sebelum disimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "berkas_hasil_akreditasi", nullable = false)
	public BerkasHasilAkreditasi getBerkasHasilAkreditasi() {
		return berkasHasilAkreditasi;
	}

	/**
	 * Menyetel kategori/wadah pemilik dokumen ini. Tanpa validasi; nilai {@code null} diterima di
	 * memori tetapi ditolak database saat disimpan karena kolomnya {@code nullable = false}.
	 * Dipanggil listener {@code save} dialog dengan kategori yang sedang dibuka di layar.
	 *
	 * @param berkasHasilAkreditasi kategori pemilik yang baru
	 */
	public void setBerkasHasilAkreditasi(BerkasHasilAkreditasi berkasHasilAkreditasi) {
		this.berkasHasilAkreditasi = berkasHasilAkreditasi;
	}

	/**
	 * Mengembalikan kode/identifier dokumen (label &laquo;Kode Berkas&raquo; pada dialog).
	 *
	 * <p>Meng-<i>override</i> {@code GeneralValueObject#getKode()} agar membaca field milik kelas ini
	 * (properti induk tidak dipetakan Hibernate). Nilainya <b>tidak dijamin unik</b> dan tidak
	 * divalidasi; dikirim ke DSpace sebagai {@code dc.identifier}.</p>
	 *
	 * @return kode dokumen, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode/identifier dokumen. Tanpa validasi maupun pemangkasan spasi.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan judul dokumen (label &laquo;Nama Berkas&raquo; pada dialog).
	 *
	 * <p>Meng-<i>override</i> {@code GeneralValueObject#getNama()} agar membaca field milik kelas
	 * ini. Nilai ini menjadi kolom pertama grid, kunci pengurutan dan pencarian {@code ilike} pada
	 * {@code BerkasHasilAkreditasiPunyaNamaHelper#initCriteria}, judul entri riwayat
	 * {@code RevisiHelper}, serta metadata {@code dc.title} pada ekspor DSpace.</p>
	 *
	 * @return judul dokumen, atau {@code null} bila belum diisi
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Menyetel judul dokumen. Tanpa validasi maupun pemangkasan spasi; kolomnya
	 * {@code varchar(255)} bawaan, sehingga judul yang lebih panjang ditolak database saat disimpan.
	 *
	 * @param nama judul baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas dokumen (kolom {@code text}, tanpa batas panjang praktis).
	 *
	 * <p><b>Membalik kontrak kelas induk:</b> {@code GeneralValueObject#getKeterangan()} menjamin
	 * tidak pernah {@code null} (mengembalikan {@code ""}), sedangkan versi ini mengembalikan field
	 * apa adanya sehingga <b>bisa {@code null}</b>. Dua konsekuensi yang teramati:</p>
	 * <ul>
	 *   <li>{@link #toString()} mencetak {@code "null"} untuk baris tanpa keterangan;</li>
	 *   <li>pada ekspor DSpace, {@code dc.description} <b>tidak ikut terkirim</b> karena
	 *   {@code org.json.JSONObject.put(key, null)} menghapus kunci &mdash; hilang tanpa error.</li>
	 * </ul>
	 *
	 * @return keterangan dokumen, atau {@code null} bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas dokumen. Tanpa validasi.
	 *
	 * <p>Dipanggil dari dua tempat: listener {@code save} dialog, dan &mdash; yang perlu diwaspadai
	 * &mdash; listener {@code onChange} kotak teks keterangan pada grid, yang langsung menyusulnya
	 * dengan {@code Common.refreshUpdate(...)}. Jadi mengetik di kolom keterangan grid <b>menyimpan
	 * ke database seketika</b>, tanpa dialog dan tanpa konfirmasi.</p>
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nama penulis dokumen (teks bebas satu baris, bukan relasi ke entity dosen atau
	 * pegawai mana pun). Dikirim ke DSpace sebagai {@code dc.contributor.author} dan ditampilkan di
	 * grid bersama {@link #getEditor()} dengan pemisah {@code " / "}.
	 *
	 * @return nama penulis, atau {@code null} bila belum diisi
	 */
	public String getPenulis() {
		return penulis;
	}

	/**
	 * Menyetel nama penulis dokumen. Tanpa validasi; kolom {@code varchar(255)}, tidak ada format
	 * baku untuk penulis jamak.
	 *
	 * @param penulis nama penulis baru
	 */
	public void setPenulis(String penulis) {
		this.penulis = penulis;
	}

	/**
	 * Mengembalikan nama editor dokumen (teks bebas, bukan relasi). Dikirim ke DSpace sebagai
	 * {@code dc.contributor.editor}.
	 *
	 * @return nama editor, atau {@code null} bila belum diisi
	 */
	public String getEditor() {
		return editor;
	}

	/**
	 * Menyetel nama editor dokumen. Tanpa validasi.
	 *
	 * @param editor nama editor baru
	 */
	public void setEditor(String editor) {
		this.editor = editor;
	}

	/**
	 * Mengembalikan abstrak/ringkasan dokumen (kolom {@code text}). Tidak ditampilkan di grid daftar
	 * &mdash; hanya di dialog form dan pada ekspor DSpace sebagai
	 * {@code dc.description.abstract}.
	 *
	 * @return abstrak dokumen, atau {@code null} bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getAbstrak() {
		return abstrak;
	}

	/**
	 * Menyetel abstrak/ringkasan dokumen. Tanpa validasi.
	 *
	 * @param abstrak abstrak baru
	 */
	public void setAbstrak(String abstrak) {
		this.abstrak = abstrak;
	}

	/**
	 * Mengembalikan kata kunci dokumen sebagai <b>satu string bebas</b> &mdash; tidak ada pemisah
	 * baku dan tidak ada pemecahan menjadi beberapa nilai. Dikirim ke DSpace sebagai satu entri
	 * {@code dc.subject} utuh, jadi beberapa kata kunci yang diketik dalam satu kotak akan terbaca
	 * DSpace sebagai satu subjek tunggal.
	 *
	 * @return kata kunci dokumen, atau {@code null} bila belum diisi
	 */
	public String getKeyword() {
		return keyword;
	}

	/**
	 * Menyetel kata kunci dokumen. Tanpa validasi maupun normalisasi pemisah.
	 *
	 * @param keyword kata kunci baru
	 */
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	/**
	 * Mengembalikan nama penerbit dokumen (label &laquo;Diterbitkan oleh&raquo;, teks bebas).
	 * Ditampilkan di grid bersama {@link #getTanggal()} dan dikirim ke DSpace sebagai
	 * {@code dc.publisher}.
	 *
	 * @return nama penerbit, atau {@code null} bila belum diisi
	 */
	public String getDiterbitkanoleh() {
		return diterbitkanoleh;
	}

	/**
	 * Menyetel nama penerbit dokumen. Tanpa validasi.
	 *
	 * @param diterbitkanoleh nama penerbit baru
	 */
	public void setDiterbitkanoleh(String diterbitkanoleh) {
		this.diterbitkanoleh = diterbitkanoleh;
	}

	/**
	 * Mengembalikan tanggal terbit dokumen (label &laquo;Diterbitkan tanggal&raquo;).
	 *
	 * <p>Kolomnya tidak dianotasi {@code @Temporal}, jadi Hibernate memetakannya sebagai
	 * {@code TIMESTAMP} lengkap dengan jam &mdash; berbeda dari maksudnya sebagai tanggal saja.
	 * Nilai bawaan objek baru adalah waktu server saat objek dibuat, sehingga dokumen yang
	 * tanggalnya tidak sengaja diisi tetap tercatat bertanggal hari input.</p>
	 *
	 * <p>Pada ekspor DSpace nilai ini menjadi {@code dc.date.issued} dan merupakan satu-satunya
	 * metadata yang <b>dilewati</b> bila {@code null}.</p>
	 *
	 * @return tanggal terbit, atau {@code null} bila dikosongkan lewat dialog
	 */
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menyetel tanggal terbit dokumen. Tanpa validasi &mdash; {@code null} maupun tanggal di masa
	 * depan diterima apa adanya. Diisi dari {@code MyDatebox} dialog, yang dapat mengembalikan
	 * {@code null} bila pengguna mengosongkan kotaknya.
	 *
	 * @param tanggal tanggal terbit baru
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}
}
