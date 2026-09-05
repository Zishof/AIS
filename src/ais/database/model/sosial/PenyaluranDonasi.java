package ais.database.model.sosial;

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
import org.json.JSONArray;

import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entitas Hibernate untuk event/kegiatan penyaluran donasi pada modul sosial legacy AIS —
 * dipetakan ke tabel {@code public.penyaluran_donasi}. Merepresentasikan satu kejadian penyaluran
 * (mis. "Penyaluran Tahap 1 - Desember 2026") yang terikat (opsional) pada satu
 * {@link ProgramDonatur} lewat {@link #getProgramDonatur()}.
 *
 * <h2>Bukan transaksi keuangan — murni catatan kegiatan</h2>
 * <p><b>Penting:</b> entitas ini TIDAK membawa kolom nominal/jumlah dana apa pun (tidak ada
 * {@code jumlah}, {@code nominal}, atau referensi ke rekening/kas). Field substantifnya
 * ({@link #getNama()}, {@link #getKeterangan()}, rentang tanggal {@link #getMulai()}/
 * {@link #getSampai()}, galeri {@link #getGambars()}/{@link #getVideos()}/{@link #getLinkUrl()})
 * seluruhnya bersifat deskriptif/dokumentatif — cocok untuk mempublikasikan laporan kegiatan
 * penyaluran (foto/video penyerahan bantuan) di portal publik, BUKAN untuk mencatat pergerakan
 * uang. Pencatatan finansial yang sesungguhnya (nominal, saldo alokasi sumber, validasi
 * kompatibilitas jenis dana) berada pada entitas generasi lebih baru
 * {@code DetailPenyaluranDonasi} (paket sama, berpenamaan campur Inggris) yang justru
 * MEREFERENSIKAN kelas ini lewat {@code getDistribution()} sebagai event/batch induknya —
 * sehingga satu baris {@code PenyaluranDonasi} legacy ini bisa menaungi banyak baris detail
 * finansial modern sekaligus.</p>
 *
 * <h2>Alur pembuatan dan persetujuan (SOP disposisi)</h2>
 * <p>Sama seperti {@link ProgramDonatur}, kelas ini mewarisi {@link DataSop} dan boleh (opsional)
 * diikat ke satu {@link ais.database.model.sop.DisposisiSop}. Selama diikat, getter
 * {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPembuatan()}, dan
 * {@link #getTanggalPersetujuan()} menimpa nilai kolom tersimpannya secara live dari disposisi
 * terkait — mekanisme, urutan resolusi, dan penanganan kegagalannya identik byte-per-byte dengan
 * {@link ProgramDonatur}; lihat javadoc getter masing-masing di kelas ini untuk detail lengkap.
 * <b>Verifikasi eksplisit soal gerbang penyaluran dana:</b> karena kelas ini tidak membawa
 * nominal, disposisi SOP di sini menggerbangi <i>pencatatan kegiatan penyaluran</i> (administratif
 * — siapa membuat, siapa menyetujui laporan kegiatan), bukan pergerakan uang; tidak ada validasi
 * saldo/otorisasi keuangan apa pun pada kelas atau action ({@code PenyaluranDonasiAction}) ini.
 * Uang yang benar-benar berpindah digerbangi terpisah oleh {@code SocialDistributionService}
 * (privilese {@code FINANCE}, mesin status, validasi saldo alokasi) atas baris
 * {@code DetailPenyaluranDonasi} — jalur itu sudah bergerbang server-side dengan baik dan tidak
 * bergantung sama sekali pada entitas atau alur persetujuan di kelas ini.</p>
 *
 * <h2>Field warisan generik ({@code oleh}/{@code olehId}/{@code tanggal_dirubah})</h2>
 * <p>Field {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta accessor-nya adalah
 * duplikasi tekstual persis dari field bernama sama pada
 * {@link ais.database.model.GeneralValueObject} — keharusan teknis peninggalan hbm2java, bukan
 * bug. Lihat catatan lengkap yang sama di javadoc kelas {@link ProgramDonatur}.</p>
 *
 * @see ProgramDonatur
 * @see DataSop
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "penyaluran_donasi")
public class PenyaluranDonasi extends DataSop {

	/**
	 * Versi serialisasi. Nilai ini disalin apa adanya dari template hbm2java bersama entitas
	 * legacy lain di paket ini (identik dengan {@link ProgramDonatur#serialVersionUID}); jangan
	 * diubah karena baris tersimpan mungkin sudah diserialkan ke cache/session ZK dengan nilai ini.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code penyaluran_donasi}. */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini. Duplikasi field audit dari
	 * {@link ais.database.model.GeneralValueObject#getOleh()} — lihat catatan kelas di atas.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini. Duplikasi field audit dari
	 * {@link ais.database.model.GeneralValueObject#getOlehId()} — lihat catatan kelas di atas.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna pengubah terakhir.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong/spasi diabaikan diam-diam.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi abaikan-nilai-kosong yang sama
	 * seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna pengubah terakhir.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@link javax.persistence.PreUpdate}: menyetel {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini
	 * diperbarui oleh Hibernate.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu pembuatan object. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: {@code "<id>-<nama>"}. Sama seperti {@link ProgramDonatur#toString()};
	 * lihat catatan di sana soal perbedaannya dari konvensi {@code "kode - nama"} umum AIS.
	 *
	 * @return gabungan id dan nama event penyaluran
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Program donasi induk yang dilayani event penyaluran ini; opsional ({@code nullable = true}).
	 * Meski secara pemetaan boleh {@code null}, {@code PenyaluranDonasiAction.onSave()} menolak
	 * simpan bila kombobox program di UI kosong — jadi pada praktiknya field ini selalu terisi
	 * lewat jalur form standar; hanya baris yang dibuat lewat jalur lain (mis. migrasi/impor
	 * langsung ke database) yang mungkin membiarkannya {@code null}.
	 */
	private ProgramDonatur programDonatur;
	/** Tanggal mulai event penyaluran. */
	private Date mulai;
	/** Tanggal berakhir event penyaluran. */
	private Date sampai;

	/**
	 * Tanggal pembuatan baris, tersimpan sebagai kolom database. Bila baris diikat ke
	 * {@link #disposisiSop}, getter {@link #getTanggalPembuatan()} menimpa nilai ini secara live
	 * dari disposisi terkait — lihat javadoc getter.
	 */
	private Date tanggalPembuatan;
	/**
	 * Tanggal persetujuan, tersimpan sebagai kolom database. Bila baris diikat ke
	 * {@link #disposisiSop}, getter {@link #getTanggalPersetujuan()} menimpa nilai ini secara live
	 * dari disposisi terkait — lihat javadoc getter.
	 */
	private Date tanggalPersetujuan;
	/**
	 * Pengguna pembuat baris, tersimpan sebagai kolom database. Bila baris diikat ke
	 * {@link #disposisiSop}, getter {@link #getDibuatOleh()} menimpa nilai ini secara live dari
	 * disposisi terkait — lihat javadoc getter.
	 */
	private Tbmuser dibuatOleh;
	/**
	 * Pengguna penyetuju baris, tersimpan sebagai kolom database. Bila baris diikat ke
	 * {@link #disposisiSop}, getter {@link #getDisetujuiOleh()} menimpa nilai ini secara live dari
	 * disposisi terkait — lihat javadoc getter.
	 */
	private Tbmuser disetujuiOleh;
	/**
	 * Disposisi SOP yang diikat ke baris ini; opsional. Bila terisi, menjadi sumber kebenaran
	 * untuk empat field audit di atas — lihat javadoc kelas dan getter masing-masing.
	 */
	private DisposisiSop disposisiSop;
	/** Satuan kerja pelaksana event penyaluran ini; dipakai juga sebagai filter cakupan daftar. */
	private SatuanKerja satuanKerja;

	/** Judul/nama event penyaluran. Wajib diisi, maksimum 255 karakter. */
	private String nama;
	/** Deskripsi/keterangan bebas mengenai event penyaluran. */
	private String keterangan;
	/** Penanda status aktif; {@code null} diperlakukan sebagai aktif (lihat {@link #getAktif()}). */
	private Boolean aktif;
	/** Daftar url/lampiran galeri foto event, disimpan sebagai teks JSON array mentah. */
	private String gambars;
	/** Daftar url/lampiran video event, disimpan sebagai teks JSON array mentah. */
	private String videos;
	/** Daftar tautan url eksternal terkait event, disimpan sebagai teks JSON array mentah. */
	private String linkUrl;

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
	public PenyaluranDonasi() {
	}

	/**
	 * Mengembalikan primary key baris {@code penyaluran_donasi}.
	 *
	 * @return primary key, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan judul/nama event penyaluran, di-{@code trim()} bila tidak {@code null}.
	 *
	 * @return nama event yang sudah di-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel judul/nama event penyaluran. Tanpa validasi/trim pada setter.
	 *
	 * @param nama nama event baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/deskripsi event apa adanya (tanpa trim).
	 *
	 * @return keterangan event, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan/deskripsi event.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif event. Mengikuti pola flag aktif umum AIS: {@code null}
	 * diperlakukan sebagai aktif ("fail open" secara default) — identik dengan
	 * {@link ProgramDonatur#getAktif()}.
	 *
	 * @return {@code true} bila aktif atau belum pernah disetel; {@code false} hanya bila eksplisit
	 *         disetel {@code false}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif. Tanpa validasi; dipanggil langsung dari checkbox "Aktif" pada layar
	 * daftar ({@code PenyaluranDonasiAction}) dan langsung disimpan tanpa melalui alur persetujuan
	 * apa pun — sama seperti {@link ProgramDonatur#setAktif(Boolean)}.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan tanggal mulai event penyaluran, dengan fallback: {@code null} dinormalkan
	 * menjadi {@code WaktuUtil.kemarin()} (kemarin) — sehingga getter ini, tidak seperti
	 * {@link ProgramDonatur#getTanggalMulai()}, tidak pernah mengembalikan {@code null} dan selalu
	 * menyiratkan rentang yang "sudah mulai" bila belum diisi eksplisit.
	 *
	 * @return tanggal mulai, atau kemarin bila belum diisi
	 */
	public Date getMulai() {
		return mulai == null ? WaktuUtil.kemarin() : mulai;
	}

	/**
	 * Menyetel tanggal mulai event penyaluran.
	 *
	 * @param mulai tanggal mulai baru
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan tanggal berakhir event penyaluran, dengan fallback: {@code null} dinormalkan
	 * menjadi {@code WaktuUtil.besok()} (besok) — simetris dengan fallback {@link #getMulai()}.
	 *
	 * @return tanggal berakhir, atau besok bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai == null ? WaktuUtil.besok() : sampai;
	}

	/**
	 * Menyetel tanggal berakhir event penyaluran.
	 *
	 * @param sampai tanggal berakhir baru
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan program donasi induk event ini, meresolusi proxy lazy lewat {@code check()}.
	 *
	 * @return program induk, atau {@code null} bila tidak/belum diikat ke program apa pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "program_donatur", nullable = true)
	public ProgramDonatur getProgramDonatur() {
		programDonatur = check(programDonatur);
		return programDonatur;
	}

	/**
	 * Menyetel program donasi induk. Tanpa validasi pada level entitas; validasi wajib-isi
	 * dikerjakan di {@code PenyaluranDonasiAction.onSave()} sebelum method ini dipanggil.
	 *
	 * @param programDonatur program induk baru
	 */
	public void setProgramDonatur(ProgramDonatur programDonatur) {
		this.programDonatur = programDonatur;
	}

	/**
	 * Menyetel pengguna pembuat baris secara langsung. Tanpa validasi. Sama seperti
	 * {@link ProgramDonatur#setDibuatOleh(Tbmuser)}, nilai ini bisa ditimpa oleh
	 * {@link #getDibuatOleh()} bila baris diikat ke disposisi; dipanggil dari
	 * {@code PenyaluranDonasiAction.onSave()} hanya pada jalur pembuatan baris baru.
	 *
	 * @param dibuatOleh pengguna pembuat baris
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna pembuat baris ini — getter dengan efek samping tersembunyi yang dapat
	 * menimpa nilai field {@link #dibuatOleh} tersimpan; perilaku dan alasannya identik dengan
	 * {@link ProgramDonatur#getDibuatOleh()}.
	 *
	 * <p>Ringkas: field {@link #dibuatOleh} diresolusi lazy dulu sebagai fallback; bila
	 * {@link #getDisposisiSop()} tidak {@code null} dan tahap {@code getDisposisiStart()}-nya punya
	 * {@code getDiajukanOleh()} tidak {@code null}, field DITIMPA dengan pengguna pengaju disposisi
	 * awal tersebut. Selama baris belum diikat ke disposisi apa pun (kasus paling umum pada alur
	 * standar {@code PenyaluranDonasiAction}, yang secara eksplisit meng-{@code null}-kan field
	 * lokal {@code disposisiSop} sebelum tiap pemanggilan {@code form(...)}), getter ini hanya
	 * mengembalikan nilai kolom tersimpan apa adanya. Kegagalan lazy-init pada proxy disposisi
	 * canonical/shared yang session-nya sudah tertutup ditelan dan dicatat ke
	 * {@link ais.common.ErrorAuditUtil}, mempertahankan nilai fallback — lihat penjelasan lengkap
	 * mekanisme dan alasannya di {@link ProgramDonatur#getDibuatOleh()}.</p>
	 *
	 * @return pengguna pembuat baris — hasil timpaan dari disposisi bila tersedia, atau nilai
	 *         kolom tersimpan sebagai fallback
	 * @see ProgramDonatur#getDibuatOleh()
	 * @see #getDisetujuiOleh()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
					&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
				dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sosial/PenyaluranDonasi.java:getDibuatOleh-lazy");
		}
		return dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna yang menyetujui baris ini — <b>gerbang persetujuan event penyaluran
	 * dilihat dari sisi model data</b>; perilaku identik dengan
	 * {@link ProgramDonatur#getDisetujuiOleh()}.
	 *
	 * <p>Ringkas: field {@link #disetujuiOleh} diresolusi lazy dulu sebagai fallback. Bila
	 * {@link #getDisposisiSop()} tidak {@code null} dan tahap {@code getDisposisiSetuju()}-nya
	 * punya {@code getDiajukanOleh()} tidak {@code null}, field DITIMPA dengan penyetuju tahap
	 * tersebut. Bila disposisi ADA tetapi tahap setuju belum terisi, field DITIMPA MENJADI
	 * {@code null} secara eksplisit — status "siapa menyetujui" sepenuhnya mengikuti keadaan
	 * disposisi terkini begitu baris diikat ke disposisi apa pun, kolom lama tidak bisa "nyangkut".</p>
	 *
	 * <p><b>Verifikasi eksplisit — gerbang penyaluran dana:</b> getter ini murni mencerminkan
	 * status persetujuan administratif event penyaluran (siapa menyetujui laporan/catatan
	 * kegiatan) dan TIDAK mencegah operasi apa pun; tidak ada kode di {@code PenyaluranDonasiAction}
	 * atau kelas ini yang menolak {@code onSave()} bila {@code getDisetujuiOleh() == null}. Karena
	 * entitas ini sendiri tidak membawa nominal dana (lihat javadoc kelas), tidak ada "penyaluran
	 * dana" yang benar-benar digerbangi di level ini — berbeda dengan pergerakan uang
	 * sesungguhnya pada {@code DetailPenyaluranDonasi} yang digerbangi terpisah dan berlapis oleh
	 * {@code SocialDistributionService} (privilese {@code FINANCE} + mesin status + validasi
	 * saldo), independen dari alur di kelas ini.</p>
	 *
	 * <p>Kegagalan lazy-init pada proxy disposisi ditelan dan dicatat ke
	 * {@link ais.common.ErrorAuditUtil}; nilai fallback dipertahankan.</p>
	 *
	 * @return pengguna penyetuju — hasil timpaan dari disposisi (termasuk ditimpa {@code null} bila
	 *         disposisi ada tapi belum disetujui), atau nilai kolom tersimpan bila tidak ada
	 *         disposisi terikat
	 * @see ProgramDonatur#getDisetujuiOleh()
	 * @see #getDibuatOleh()
	 * @see #getTanggalPersetujuan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				disetujuiOleh = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sosial/PenyaluranDonasi.java:getDisetujuiOleh-lazy");
		}
		return disetujuiOleh;
	}

	/**
	 * Menyetel tanggal persetujuan pada field tersimpan. Tanpa validasi; bisa ditimpa oleh
	 * {@link #getTanggalPersetujuan()} bila baris diikat ke disposisi.
	 *
	 * @param tanggalPersetujuan tanggal persetujuan baru
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan tanggal persetujuan, mengikuti pola timpa-dari-disposisi yang identik dengan
	 * {@link #getDisetujuiOleh()} dan {@link ProgramDonatur#getTanggalPersetujuan()}: ditimpa
	 * dengan waktu tahap setuju bila diajukan, atau ditimpa {@code null} bila disposisi ada tapi
	 * tahap setuju belum terisi. Tanpa disposisi terikat, nilai kolom tersimpan dikembalikan apa
	 * adanya. Kegagalan lazy-init pada proxy disposisi ditelan dan dicatat ke
	 * {@link ais.common.ErrorAuditUtil}.
	 *
	 * @return tanggal persetujuan efektif, bisa {@code null}
	 * @see #getDisetujuiOleh()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sosial/PenyaluranDonasi.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	/**
	 * Menyetel pengguna penyetuju pada field tersimpan. Tanpa validasi; tidak dipanggil dari
	 * {@code PenyaluranDonasiAction} pada berkas ini.
	 *
	 * @param disetujuiOleh pengguna penyetuju baru
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Menyetel tanggal pembuatan pada field tersimpan. Tanpa validasi. Dipanggil dari
	 * {@code PenyaluranDonasiAction.onSave()} dengan {@code WaktuUtil.getDate()} hanya pada jalur
	 * pembuatan baris baru.
	 *
	 * @param tanggalPembuatan tanggal pembuatan baru
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan tanggal pembuatan, mengikuti pola timpa-dari-disposisi yang identik dengan
	 * {@link ProgramDonatur#getTanggalPembuatan()}: ditimpa dengan waktu tahap awal disposisi bila
	 * diajukan (tanpa cabang timpa-{@code null} bila tahap awal belum terisi — kolom tersimpan
	 * dipertahankan), dan hasil akhirnya di-fallback ke {@code WaktuUtil.getDate()} (waktu saat
	 * ini) bila masih {@code null} — sehingga getter ini tidak pernah mengembalikan {@code null}.
	 * Kegagalan lazy-init pada proxy disposisi ditelan dan dicatat ke
	 * {@link ais.common.ErrorAuditUtil}.
	 *
	 * @return tanggal pembuatan efektif; tidak pernah {@code null}
	 * @see #getDibuatOleh()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
					&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
				tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sosial/PenyaluranDonasi.java:getTanggalPembuatan-lazy");
		}

		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Mengembalikan disposisi SOP yang diikat ke baris ini, meresolusi proxy lazy lewat
	 * {@code check()}.
	 *
	 * @return disposisi terikat, atau {@code null} bila baris ini belum/tidak diikat ke disposisi
	 *         apa pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Mengikat baris ini ke sebuah disposisi SOP, dengan guard identik dengan
	 * {@link ProgramDonatur#setDisposisiSop(DisposisiSop)}: argumen {@code null} atau disposisi
	 * belum tersimpan diabaikan diam-diam, dan disposisi yang sudah terikat tidak pernah bisa
	 * dilepas lewat setter ini. Baris kedua method ini (ekspresi ternary) juga merupakan kode
	 * mati/berlebihan dengan alasan yang sama seperti di {@link ProgramDonatur} — lihat catatan
	 * lengkap di sana; kedua kelas jelas berasal dari templat/salinan yang sama.
	 *
	 * @param disposisiSop disposisi baru; diabaikan bila {@code null} atau belum tersimpan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Mengembalikan satuan kerja pelaksana event ini, meresolusi proxy lazy lewat {@code check()}.
	 *
	 * @return satuan kerja pelaksana, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pelaksana. Tanpa validasi.
	 *
	 * @param satuanKerja satuan kerja baru
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** Representasi teks array JSON kosong ({@code "[]"}), fallback bagi {@link #getGambars()},
	 * {@link #getVideos()}, dan {@link #getLinkUrl()} ketika kolomnya belum diisi. */
	private static String T = new JSONArray().toString();

	/**
	 * Mengembalikan daftar galeri foto event sebagai teks JSON array mentah, atau {@link #T}
	 * ({@code "[]"}) bila belum diisi.
	 *
	 * @return teks JSON array foto, tidak pernah {@code null}/kosong
	 */
	@Column(name = "gambars", nullable = true, columnDefinition = "text")
	public String getGambars() {
		return gambars == null || gambars.isEmpty() ? T : gambars;
	}

	/**
	 * Menyetel daftar galeri foto (teks JSON array mentah).
	 *
	 * @param gambars teks JSON array foto baru
	 */
	public void setGambars(String gambars) {
		this.gambars = gambars;
	}

	/**
	 * Mengembalikan daftar video event sebagai teks JSON array mentah, atau {@link #T}
	 * ({@code "[]"}) bila belum diisi.
	 *
	 * @return teks JSON array video, tidak pernah {@code null}/kosong
	 */
	@Column(name = "videos", nullable = true, columnDefinition = "text")
	public String getVideos() {
		return videos == null || videos.isEmpty() ? T : videos;
	}

	/**
	 * Menyetel daftar video (teks JSON array mentah).
	 *
	 * @param videos teks JSON array video baru
	 */
	public void setVideos(String videos) {
		this.videos = videos;
	}

	/**
	 * Mengembalikan daftar tautan url eksternal sebagai teks JSON array mentah, atau {@link #T}
	 * ({@code "[]"}) bila belum diisi.
	 *
	 * @return teks JSON array tautan, tidak pernah {@code null}/kosong
	 */
	@Column(name = "link_url", nullable = true, columnDefinition = "text")
	public String getLinkUrl() {
		return linkUrl == null || linkUrl.isEmpty() ? T : linkUrl;
	}

	/**
	 * Menyetel daftar tautan url eksternal (teks JSON array mentah).
	 *
	 * @param linkUrl teks JSON array tautan baru
	 */
	public void setLinkUrl(String linkUrl) {
		this.linkUrl = linkUrl;
	}
}
