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
 * <b>Nilai yang diberikan seorang asesor atas satu butir beban kerja dosen (BKD).</b>
 * Satu baris menjawab pertanyaan: <i>"berapa SKS <b>kinerja</b> yang diakui asesor
 * {@link #getAsesor() X} untuk butir beban {@link #getAsesemenPenilaian() Y}, apa buktinya, dan
 * apa catatannya?"</i>. Memetakan tabel {@code public.penilaian_asesor}.
 *
 * <p>Entity ini adalah <b>tabel jembatan berpayload</b> antara dua sisi proses BKD:</p>
 *
 * <ul>
 *   <li>{@link AsesemenPenilaian} &mdash; butir beban yang <b>diklaim</b> dosen (kolom
 *   {@code sks_beban}), dihasilkan otomatis oleh keluarga {@code Bkd*Helper};</li>
 *   <li>{@link Asesor} &mdash; penilai yang ditugaskan atas dosen tersebut (lewat
 *   {@link AsesorPegawai}), dengan perannya dijelaskan
 *   {@link AsesorPenunjangKinerjaDosen}.</li>
 * </ul>
 *
 * <p>Payload-nya adalah hasil penilaian: {@link #getSks() SKS kinerja yang diakui}
 * (kolom {@code sks_kinerja}), {@link #getBukti() bukti}, {@link #getKeterangan() catatan},
 * dan {@link #getPilih() penanda sudah/belum digarap}. Karena satu butir beban dinilai oleh
 * beberapa asesor, jumlah baris di sini = jumlah butir beban &times; jumlah asesor aktif yang
 * ditugaskan atas dosen bersangkutan.</p>
 *
 * <h3>Konteks pemakaian: BKD, bukan akreditasi</h3>
 *
 * <p>Verifikasi dari kode: seluruh pembaca/penulis entity ini berada di paket
 * {@code ais.action.master.bkd} (Beban Kerja Dosen) &mdash; bukan modul akreditasi.
 * Alurnya dua tahap:</p>
 *
 * <ol>
 *   <li><b>Pendataan beban (otomatis).</b> Dua belas helper
 *   ({@code BkdPengajaranHelper}, {@code BkdArtikelHelper}, {@code BkdBimbinganSkripsiHelper},
 *   {@code BkdPengujiSkripsiHelper}, {@code BkdPengujiProposalSkripsiHelper},
 *   {@code BkdKknHelper}, {@code BkdPklHelper}, {@code BkdDosenPaHelper},
 *   {@code BkdKegiatanDosenHelper}, {@code BkdPenulisHelper},
 *   {@code BkdPenelitianDanPengabdianHelper}, {@code BkdPenunjangHelper}) memindai data
 *   operasional (jadwal mengajar, SK bimbingan, artikel, dsb) lalu membuat/menyegarkan baris
 *   {@link AsesemenPenilaian}. Segera setelah itu masing-masing memanggil
 *   {@code PenilaianAsesorAction.checkPenilaian(session, asesors, asesemenPenilaian)}, yang
 *   <b>membuat baris kelas ini bila belum ada</b> (satu per asesor, {@code sks} awal
 *   {@code 0.0}). Jadi baris di sini nyaris tidak pernah dibuat lewat form &mdash; melainkan
 *   dilahirkan massal oleh proses "Proses Ulang"
 *   ({@code PenilaianAsesorAction.prosesUlang(...)}, berjalan di thread terpisah).</li>
 *   <li><b>Penilaian (manual oleh asesor).</b> Asesor mengisi SKS kinerja, bukti dan catatan
 *   lewat grid {@code ais.action.master.bkd.helper.PenilaianAsesorHelper#formNilai(...)} yang
 *   ditempelkan pada layar rincian tiap jenis kegiatan, atau lewat layar
 *   {@code /pages/master/bkd/asesor_memberikan_penilaian_rinci.zul}
 *   ({@code PenilaianAsesorAction}) yang di-<i>include</i> dari daftar asesi
 *   ({@code ais.action.master.bkd.PegawaiAction}).</li>
 * </ol>
 *
 * <p><b>"Belum dinilai" = {@code sks_kinerja} &le; 0,1.</b> Tidak ada kolom status
 * tersendiri; baik {@code PegawaiAction} maupun {@code PenilaianAsesorAction} memakai
 * ambang {@code Restrictions.le("sks", 0.1)} / {@code gt(...)} untuk memisahkan asesi yang
 * sudah dan belum dinilai. Konsekuensinya nilai kinerja sah sebesar 0 SKS (mis. kegiatan
 * ditolak asesor) <b>tidak bisa dibedakan</b> dari "belum disentuh".</p>
 *
 * <h3>Dua belas konstanta spesifikasi</h3>
 *
 * <p>Konstanta {@code String} di kelas ini ({@link #ARTIKEL} &hellip;
 * {@link #PENUNJANG_DAN_LAIN_LAIN}) <b>bukan</b> properti kelas ini: nilainya disimpan pada
 * kolom {@code spesifikasi} milik {@link AsesemenPenilaian}. Kelas ini hanya menjadi
 * "kamus" bersama karena dialah titik temu seluruh helper BKD. Nilai-nilai itu dipakai
 * sebagai (a) label combo filter, (b) kunci pencarian {@code Restrictions.eq("spesifikasi",
 * ...)}, (c) saklar percabangan {@code kasihPenilaian(...)} yang menentukan form rincian mana
 * yang dibuka, dan (d) sumber nilai bawaan {@code masaTugas} di
 * {@link AsesemenPenilaian#getMasaTugas()}. Karena teksnya ikut <b>tersimpan di database</b>,
 * mengubah string konstanta akan memutus kecocokan dengan baris lama.</p>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>Salah ketik yang sudah terlanjur jadi data.</b> {@link #ARTIKEL} bernilai
 *   {@code "Pubikasi Ilmiah"} &mdash; kurang huruf "l" dari "Publikasi". Teks ini tampil di
 *   combo filter <i>dan</i> tersimpan di kolom {@code spesifikasi}, sehingga memperbaikinya
 *   memerlukan migrasi data, bukan sekadar edit konstanta.</li>
 *   <li><b>{@link #getSks()} adalah getter yang MENULIS balik.</b> Bila field {@code sks}
 *   masih {@code null}, getter menetapkannya ke {@code 0.0} sebelum mengembalikan nilai.
 *   Karena kelas ini memakai <i>property access</i> (anotasi menempel pada getter), pembacaan
 *   biasa atas entity terkelola dapat membuat Hibernate melihat perubahan {@code null &rarr;
 *   0.0} dan menerbitkan UPDATE. Ini satu-satunya getter yang menulis di kelas ini &mdash;
 *   {@link #getKeterangan()}, {@link #getBukti()} dan {@link #getPilih()} menormalkan nilai
 *   <b>tanpa</b> menulis balik ke field.</li>
 *   <li><b>Tidak ada getter destruktif dan tidak ada getter yang menutup sesi Hibernate.</b>
 *   Diperiksa satu per satu: kedua getter relasi ({@link #getAsesor()},
 *   {@link #getAsesemenPenilaian()}) mengembalikan field apa adanya &mdash; tidak memanggil
 *   {@link GeneralValueObject#check(Object)}, tidak menghapus apa pun, tidak menyentuh
 *   {@code HibernateUtil}. Perhatikan bahwa entity tetangganya <b>berbeda</b> dalam hal ini:
 *   {@link Asesor#getTbmuser()} dan {@link AsesemenPenilaian#getJenjang()} memang memanggil
 *   {@code check(...)}, dan {@link AsesemenPenilaian#getMatakuliah()},
 *   {@link AsesemenPenilaian#getJenjang()} serta
 *   {@link AsesemenPenilaian#getMasaTugas()} menulis balik ke fieldnya sendiri. Jangan
 *   menyimpulkan sifat kelas ini dari kelas-kelas itu.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *   BUKAN duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak
 *   biasa &mdash; bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga
 *   Hibernate sama sekali tidak memetakan properti kelas induk. Setiap entity turunan wajib
 *   mendeklarasikan sendiri kolom-kolom itu agar terpetakan.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi
 *   tidak ada {@code @PrePersist}, sehingga {@code oleh}/{@code olehId} hanya terisi saat
 *   baris di-<i>update</i>, bukan saat pertama dibuat &mdash; dan baris di sini justru lahir
 *   massal dari {@code checkPenilaian(...)}, jadi kolom itu kosong sampai asesor benar-benar
 *   menilai. Riwayat lengkap tetap tersedia lewat {@code @Audited} (Hibernate Envers).</li>
 *   <li><b>{@link #toString()} membaca field {@code keterangan} mentah, bukan getternya</b>,
 *   sehingga bisa mengembalikan {@code null} untuk baris yang belum dinilai (getter-nya
 *   menormalkan {@code null} jadi string kosong). Jangan diandalkan untuk label UI.</li>
 *   <li><b>Cascade PERSIST/MERGE mengarah ke induk.</b> Kedua {@code @ManyToOne} memakai
 *   {@code CascadeType.PERSIST, MERGE}, jadi menyimpan satu baris penilaian ikut
 *   mem-persist/merge {@link Asesor} dan {@link AsesemenPenilaian} yang menempel padanya.
 *   Tidak ada {@code REMOVE}: menghapus baris di sini tidak menghapus butir bebannya.</li>
 *   <li><b>Komentar generator "Bank generated by hbm2java" salah nama</b> (sisa salin-tempel
 *   generator Apr 2010); tidak ada hubungannya dengan entity {@code Bank}. Nilai
 *   {@code serialVersionUID} pun identik dengan {@link Asesor} dan {@link AsesemenPenilaian},
 *   ciri khas berkas hasil generator yang sama.</li>
 * </ol>
 *
 * <h3>Kuirk visibilitas di grid penilaian</h3>
 *
 * <p>Pada {@code PenilaianAsesorHelper#formNilai(...)} cakupan baris yang ditampilkan
 * <b>terbalik</b> dari yang diduga: pengguna yang <i>merupakan</i> asesor dosen tersebut
 * hanya melihat baris <b>miliknya sendiri</b> (query dibatasi
 * {@code Restrictions.in("asesor", merupakanAsesor)}) dan bisa mengeditnya, sedangkan
 * pengguna yang <i>bukan</i> asesor melihat baris <b>seluruh asesor</b> &mdash; lengkap
 * dengan SKS kinerja, bukti dan catatan masing-masing &mdash; dalam mode hanya-baca. Jadi
 * pihak yang tidak berkepentingan justru mendapat pandangan yang lebih luas.</p>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 *
 * <ul>
 *   <li><b>Kamus spesifikasi (konstanta)</b>: {@link #ARTIKEL}, {@link #PENGAJARAN},
 *   {@link #PEMBIMBING_TA}, {@link #PENGUJI_TA}, {@link #PENGUJI_PROPOSAL_TA},
 *   {@link #PEMBIMBING_KKN}, {@link #PEMBIMBING_PKL}, {@link #PENULIS_BUKU},
 *   {@link #KEGIATAN_DOSEN}, {@link #PEMBIMBING_AKADEMIK},
 *   {@link #PENELITIAN_ATAU_PENGABDIAN}, {@link #PENUNJANG_DAN_LAIN_LAIN}.</li>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}/{@link #setId(Long)}, {@link #toString()},
 *   konstruktor {@link #PenilaianAsesor()}.</li>
 *   <li><b>Pasangan yang dijembatani</b>: {@link #getAsesor()} dan
 *   {@link #getAsesemenPenilaian()}.</li>
 *   <li><b>Hasil penilaian</b>: {@link #getSks()}, {@link #getBukti()},
 *   {@link #getKeterangan()}, {@link #getPilih()}.</li>
 * </ul>
 *
 * <p>Tidak ada method bisnis maupun query statis di kelas ini; seluruh logika pembuatan baris
 * ada di {@code PenilaianAsesorAction.checkPenilaian(...)} dan seluruh logika penyimpanan di
 * helper UI-nya.</p>
 *
 * @see AsesemenPenilaian
 * @see Asesor
 * @see AsesorPegawai
 * @see AsesorPenunjangKinerjaDosen
 * @see PenunjangKinerjaDosen
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "penilaian_asesor")

public class PenilaianAsesor extends GeneralValueObject {

	/**
	 * Spesifikasi "publikasi ilmiah/artikel" &mdash; butir beban yang dibentuk
	 * {@code BkdArtikelHelper} dari entity
	 * {@code ais.database.model.penelitiandanpengabdian.Artikel}.
	 *
	 * <p><b>Perhatian:</b> teksnya salah ketik ({@code "Pubikasi Ilmiah"}, kurang huruf "l")
	 * dan sudah terlanjur tersimpan pada kolom {@code spesifikasi} di database; jangan
	 * dikoreksi tanpa migrasi data.</p>
	 */
	public static final String ARTIKEL = "Pubikasi Ilmiah";
	/**
	 * Spesifikasi "pengajaran" &mdash; beban mengajar per {@code Perkuliahan}/
	 * {@code Matakuliah}, dibentuk {@code BkdPengajaranHelper}. Membuka form rincian
	 * {@code DosenPerkuliahanHelper}/{@code DosenMengajarHelper} saat dinilai.
	 */
	public static final String PENGAJARAN = "Pengajaran";
	/** Spesifikasi "pembimbing tugas akhir/skripsi", dibentuk {@code BkdBimbinganSkripsiHelper}; rinciannya dibuka {@code BimbinganSkripsiAction.displayRow(...)}. */
	public static final String PEMBIMBING_TA = "Pembimbing Tugas Akhir";
	/** Spesifikasi "penguji tugas akhir/skripsi", dibentuk {@code BkdPengujiSkripsiHelper}; rinciannya dibuka {@code PengujiSkripsiAction.displayRow(...)}. */
	public static final String PENGUJI_TA = "Penguji Tugas Akhir";
	/** Spesifikasi "penguji proposal tugas akhir", dibentuk {@code BkdPengujiProposalSkripsiHelper}; rinciannya dibuka {@code BimbinganSkripsiAction.displayRow(...)}. */
	public static final String PENGUJI_PROPOSAL_TA = "Penguji Proposal Tugas Akhir";
	/** Spesifikasi "pembimbing KKN", dibentuk {@code BkdKknHelper}; rinciannya dibuka {@code KelompokKknAction.displayRow(...)}. */
	public static final String PEMBIMBING_KKN = "Pembimbing KKN";
	/** Spesifikasi "pembimbing PKL", dibentuk {@code BkdPklHelper}; rinciannya dibuka {@code KelompokPklAction.displayRow(...)}. */
	public static final String PEMBIMBING_PKL = "Pembimbing PKL";
	/** Spesifikasi "penulis buku/bahan ajar", dibentuk {@code BkdPenulisHelper} dari {@link BukuBahanAjar}. */
	public static final String PENULIS_BUKU = "Penulis Buku";
	/** Spesifikasi "kegiatan dosen", dibentuk {@code BkdKegiatanDosenHelper} dari {@link KegiatanKedosenanPunyaDosen}. */
	public static final String KEGIATAN_DOSEN = "Kegiatan Dosen";
	/** Spesifikasi "pembimbing akademik" (dosen PA), dibentuk {@code BkdDosenPaHelper}; rinciannya dibuka {@code DosenPembimbingAkademikAction.displayRow(...)}. */
	public static final String PEMBIMBING_AKADEMIK = "Pembimbing Akademik";
	/** Spesifikasi "penelitian atau pengabdian", dibentuk {@code BkdPenelitianDanPengabdianHelper} dari {@code PengajuanPenelitianDanPengabdian}. */
	public static final String PENELITIAN_ATAU_PENGABDIAN = "Penelitian atau Pengabdian";
	/** Spesifikasi "penunjang dan lain-lain", dibentuk {@code BkdPenunjangHelper} dari {@link PenunjangKinerjaDosen}. */
	public static final String PENUNJANG_DAN_LAIN_LAIN = "Penunjang dan lain-lain";

	/**
	 * Versi serialisasi Java. Nilainya identik dengan {@link Asesor} dan
	 * {@link AsesemenPenilaian} karena ketiganya keluar dari generator hbm2java yang sama;
	 * bukan penanda kompatibilitas yang pernah dikelola manual.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci primer baris (kolom {@code id}, IDENTITY). Dideklarasikan ulang di sini karena {@link GeneralValueObject} tidak dipetakan Hibernate. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris; pasangan dari {@link #oleh}. */
	private String olehId;

	/** @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah. <b>Menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/spasi: nilai lama dipertahankan alih-alih ditimpa, sehingga jejak audit terakhir
	 * tidak hilang saat interceptor dipanggil tanpa konteks pengguna (mis. proses "Proses
	 * Ulang" BKD yang berjalan di thread terpisah).
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong <b>diabaikan</b> dan nilai lama dipertahankan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * dari pengguna sesi berjalan lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum baris
	 * di-UPDATE. Tidak ada padanan {@code @PrePersist}, jadi baris yang dilahirkan massal oleh
	 * {@code PenilaianAsesorAction.checkPenilaian(...)} tidak mencatat siapa pembuatnya (lihat
	 * javadoc kelas). Pada baris deklarasi yang sama juga dideklarasikan field
	 * {@code tanggal_dirubah}, yang diinisialisasi ke waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) sehingga baris baru tetap punya stempel waktu
	 * meski belum pernah di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir baris ini */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}, presisi TIMESTAMP) */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini: catatan asesor apa adanya.
	 *
	 * <p><b>Membaca field {@code keterangan} secara langsung, bukan {@link #getKeterangan()}</b>,
	 * sehingga dapat mengembalikan {@code null} untuk baris yang baru dilahirkan
	 * {@code checkPenilaian(...)} dan belum pernah dinilai &mdash; berbeda dari getternya yang
	 * menormalkan {@code null} menjadi string kosong. Jangan dipakai sebagai label UI atau
	 * kunci pencarian.</p>
	 *
	 * @return isi catatan asesor, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return keterangan;
	}

	/** Asesor yang memberikan penilaian ini (kolom FK {@code asesor}). */
	private Asesor asesor;
	/** Butir beban kerja yang dinilai (kolom FK {@code asesemen_penilaian}). */
	private AsesemenPenilaian asesemenPenilaian;

	/** SKS kinerja yang <b>diakui</b> asesor (kolom {@code sks_kinerja}); berbeda dari SKS beban yang diklaim dosen di {@link AsesemenPenilaian#getSks()}. */
	private Double sks;
	/** Catatan bebas asesor atas butir beban ini (kolom {@code keterangan}, bertipe {@code text}). */
	private String keterangan;
	/** Uraian bukti pendukung yang dicatat asesor (kolom {@code bukti}, bertipe {@code text}). */
	private String bukti;
	/** Penanda bahwa asesor sudah membuka/menggarap baris ini; mengaktifkan kotak isian SKS/bukti/catatan di grid penilaian. */
	private Boolean pilih;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate. Baris baru praktis selalu dibuat
	 * lewat {@code PenilaianAsesorAction.checkPenilaian(...)}, yang langsung mengisi
	 * {@link #setAsesor(Asesor)}, {@link #setAsesemenPenilaian(AsesemenPenilaian)} dan
	 * {@link #setSks(Double)} dengan {@code 0.0}.
	 */
	public PenilaianAsesor() {
	}

	/** @return kunci primer baris; {@code null} bila entity belum tersimpan */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci primer; normalnya hanya disetel Hibernate karena kolomnya IDENTITY dan {@code insertable = false} */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan asesor dalam bentuk yang <b>sudah dinormalkan</b>: {@code null}
	 * menjadi string kosong dan spasi di ujung dipangkas. Normalisasi ini hanya berlaku pada
	 * nilai yang dikembalikan &mdash; field {@code keterangan} <b>tidak</b> ditulis ulang,
	 * jadi getter ini tidak memicu UPDATE (berbeda dari {@link #getSks()}).
	 *
	 * <p>Nilai ini yang ditampilkan sebagai kolom "Catatan" di grid penilaian dan sebagai
	 * baris "Catatan: &hellip;" di layar rincian.</p>
	 *
	 * @return catatan asesor tanpa spasi di ujung, atau string kosong bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * Menyetel catatan asesor. Nilai disimpan apa adanya (pemangkasan spasi dilakukan di sisi
	 * pemanggil, mis. {@code PenilaianAsesorHelper}) dan {@code null} diperbolehkan.
	 *
	 * @param keterangan catatan bebas asesor; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Asesor pemberi nilai pada baris ini.
	 *
	 * <p>Getter murni: mengembalikan field apa adanya tanpa memanggil
	 * {@link GeneralValueObject#check(Object)}, tanpa menulis balik, dan tanpa menyentuh sesi
	 * Hibernate. Karena relasi ini <i>eager</i> dengan {@link FetchMode#SELECT}, objeknya
	 * dimuat lewat query terpisah saat baris penilaian dibaca.</p>
	 *
	 * <p>Nama yang tampil di grid sebenarnya berasal dari
	 * {@code getAsesor().getAsesorPenunjangKinerjaDosen()} (peran/kategori asesor), sedangkan
	 * identitas orangnya ada di {@link Asesor#getTbmuser()} dan hanya ditampilkan bila
	 * konfigurasi {@code tampilkan_asesor} bernilai benar.</p>
	 *
	 * @return asesor penilai, atau {@code null} untuk baris yatim (kolom FK {@code asesor} nullable)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "asesor", nullable = true)
	public Asesor getAsesor() {
		return asesor;
	}

	/**
	 * Menyetel asesor penilai. Karena relasinya ber-cascade {@code PERSIST}/{@code MERGE},
	 * menyimpan baris ini ikut mem-persist/merge objek {@link Asesor} yang diberikan.
	 *
	 * @param asesor asesor penilai; boleh {@code null}
	 */
	public void setAsesor(Asesor asesor) {
		this.asesor = asesor;
	}

	/**
	 * Penanda bahwa baris ini sedang/sudah digarap asesor. Nilai {@code null} dianggap
	 * <b>{@code false}</b> (bawaan berpihak "belum digarap"), dan normalisasi itu
	 * <b>tidak</b> ditulis balik ke field.
	 *
	 * <p>Di grid {@code PenilaianAsesorHelper#formNilai(...)} nilai ini menjadi checkbox
	 * "Pilih" yang mengendalikan {@code setDisabled(...)} kotak Masa Tugas/SKS/Bukti/Catatan:
	 * selama belum dicentang, keempatnya terkunci. Mencentangnya juga mengisi kotak Bukti
	 * dengan teks bukti bawaan milik layar pemanggil. Tidak ada anotasi {@code @Column}, jadi
	 * dipetakan ke kolom bernama {@code pilih}.</p>
	 *
	 * @return {@code true} bila baris sudah digarap asesor; {@code false} bila belum atau bila field masih {@code null}
	 */
	public Boolean getPilih() {
		return pilih == null ? false : pilih;
	}

	/**
	 * Menyetel penanda "sudah digarap".
	 *
	 * @param pilih penanda garapan; {@code null} akan dibaca sebagai {@code false} oleh {@link #getPilih()}
	 */
	public void setPilih(Boolean pilih) {
		this.pilih = pilih;
	}

	/**
	 * SKS <b>kinerja</b> yang diakui asesor untuk butir beban ini (kolom {@code sks_kinerja}).
	 * Bandingkan dengan {@link AsesemenPenilaian#getSks()} yang memetakan kolom
	 * {@code sks_beban} &mdash; yaitu beban yang diklaim/dihitung sistem untuk dosen. Selisih
	 * keduanya inilah inti asesmen BKD.
	 *
	 * <p><b>Getter ini MENULIS.</b> Bila field masih {@code null}, getter menetapkannya ke
	 * {@code 0.0} lebih dulu. Karena pemetaan kelas ini memakai <i>property access</i>
	 * (anotasi menempel pada getter), pembacaan biasa atas entity terkelola dapat membuat
	 * Hibernate mendeteksi perubahan {@code null &rarr; 0.0} dan menerbitkan UPDATE tanpa ada
	 * niat menyimpan apa pun. Efek praktisnya kecil karena {@code checkPenilaian(...)} memang
	 * sudah mengisi {@code 0.0} saat baris dibuat, tetapi tetap perlu diketahui saat
	 * membedakan baris "belum pernah disentuh" dari "sudah dibaca layar".</p>
	 *
	 * <p>Nilai ini juga menjadi <b>penanda status</b>: seluruh query BKD memakai ambang
	 * {@code sks &le; 0,1} untuk "belum dinilai" dan {@code sks &gt; 0,1} untuk "telah
	 * dinilai", sehingga penilaian sah bernilai 0 SKS tidak dapat dibedakan dari yang belum
	 * digarap.</p>
	 *
	 * @return SKS kinerja yang diakui; tidak pernah {@code null} (dinormalkan ke {@code 0.0})
	 */
	@Column(name = "sks_kinerja", nullable = true)
	public Double getSks() {
		if (sks == null) {
			sks = 0.0;
		}
		return sks;
	}

	/**
	 * Menyetel SKS kinerja yang diakui asesor. Dipanggil dari kotak isian "SKS Kinerja" pada
	 * grid {@code PenilaianAsesorHelper#formNilai(...)} (langsung diikuti
	 * {@code Common.refreshSaveOrUpdate(...)}, jadi setiap perubahan tersimpan seketika) dan
	 * dari {@code PenilaianAsesorAction.checkPenilaian(...)} yang mengisi {@code 0.0} untuk
	 * baris baru.
	 *
	 * @param sks SKS kinerja; {@code null} akan dinormalkan menjadi {@code 0.0} oleh {@link #getSks()}
	 */
	public void setSks(Double sks) {
		this.sks = sks;
	}

	/**
	 * Uraian bukti pendukung yang dicatat asesor, sudah dinormalkan ({@code null} menjadi
	 * string kosong, spasi ujung dipangkas). Seperti {@link #getKeterangan()}, normalisasi
	 * hanya berlaku pada nilai kembalian &mdash; field tidak ditulis ulang.
	 *
	 * <p>Isi awalnya berasal dari teks bukti bawaan layar pemanggil, yang otomatis disalin ke
	 * kotak "Bukti" saat checkbox {@link #getPilih() Pilih} dicentang; asesor dapat
	 * menimpanya.</p>
	 *
	 * @return uraian bukti tanpa spasi di ujung, atau string kosong bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getBukti() {
		return bukti == null ? "" : bukti.trim();
	}

	/**
	 * Menyetel uraian bukti pendukung.
	 *
	 * @param bukti uraian bukti; boleh {@code null}
	 */
	public void setBukti(String bukti) {
		this.bukti = bukti;
	}

	/**
	 * Butir beban kerja yang dinilai baris ini &mdash; sisi "apa yang diklaim dosen" dari
	 * jembatan ini. Dari sinilah layar penilaian mengambil dosen yang dinilai
	 * ({@link AsesemenPenilaian#getPegawai()}), bidang/spesifikasi, tahun akademik/semester,
	 * SKS beban, masa tugas, serta objek rincian (perkuliahan, matakuliah, artikel, buku,
	 * kegiatan, penelitian, penunjang) yang menentukan form mana yang dibuka
	 * {@code PenilaianAsesorAction.kasihPenilaian(...)}.
	 *
	 * <p>Getter murni: mengembalikan field apa adanya, tanpa {@code check(...)}, tanpa menulis
	 * balik, dan tanpa menutup sesi Hibernate.</p>
	 *
	 * @return butir beban yang dinilai, atau {@code null} untuk baris yatim (kolom FK nullable)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "asesemen_penilaian", nullable = true)
	public AsesemenPenilaian getAsesemenPenilaian() {
		return asesemenPenilaian;
	}

	/**
	 * Menyetel butir beban yang dinilai. Ber-cascade {@code PERSIST}/{@code MERGE}, sehingga
	 * menyimpan baris penilaian ikut mem-persist/merge {@link AsesemenPenilaian} yang
	 * menempel. Tidak ada cascade {@code REMOVE}: menghapus baris penilaian tidak menghapus
	 * butir bebannya.
	 *
	 * @param asesemenPenilaian butir beban kerja yang dinilai; boleh {@code null}
	 */
	public void setAsesemenPenilaian(AsesemenPenilaian asesemenPenilaian) {
		this.asesemenPenilaian = asesemenPenilaian;
	}

}
