package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * HEADER dokumen PEMINJAMAN aset -- mencatat penyerahan SEMENTARA satu atau lebih unit aset
 * kepada seorang peminjam untuk suatu keperluan, dengan kewajiban dikembalikan pada
 * {@link #getTanggalKembali()}. Padanan permanen/tanpa batas waktu ada di
 * {@link PemakaianMasterAsset}; lihat javadoc kelas tersebut untuk perbandingan field lengkap.
 *
 * <h3>Ciri pembeda: identitas peminjam bebas teks, bukan FK</h3>
 *
 * <p>{@link #getNamaPeminjam()}, {@link #getKeperluan()}, dan {@link #getLokasiKegiatan()} semua
 * berupa teks bebas ({@code columnDefinition = "text"}), BUKAN relasi ke {@code Tbmuser} atau
 * entitas pegawai -- peminjam boleh siapa pun (termasuk pihak luar) tanpa perlu akun sistem.
 * Rentang waktu peminjaman ({@link #getTanggalPinjam()}/{@link #getTanggalKembali()}) dan tautan
 * opsional ke {@link #getPengembalianMasterAsset()} (dokumen pengembalian header-level) adalah
 * penanda utama bahwa transaksi ini SEMENTARA.</p>
 *
 * <h3>Dua mekanisme pelacakan pengembalian yang TIDAK saling sinkron otomatis</h3>
 *
 * <p>Header ini memiliki {@link #getPengembalianMasterAsset()} sebagai tautan opsional ke satu
 * dokumen pengembalian, sedangkan tiap baris {@link PeminjamanMasterAssetDetail} PUNYA SENDIRI
 * flag {@code dikembalikan}/{@code waktuPengembalian} per unit barang. Kedua mekanisme ini
 * berdiri sendiri di tingkat entitas -- tidak ada constraint atau callback di sini yang menjamin
 * status keduanya konsisten (mis. header bisa saja belum ditautkan ke pengembalian meski semua
 * baris detail sudah bertanda {@code dikembalikan = true}, atau sebaliknya).</p>
 *
 * <h3>Tidak ada penjagaan double-booking di tingkat entitas</h3>
 *
 * <p>Tidak ada kolom atau validasi di kelas ini (maupun di {@link PeminjamanMasterAssetDetail})
 * yang mencegah unit aset yang SEDANG dipinjam (baris detail existing dengan
 * {@code dikembalikan = false}) ditambahkan lagi ke dokumen peminjaman BARU untuk peminjam lain.
 * Penjagaan semacam itu, bila ada, harus ditegakkan di lapisan action/helper pemanggil saat
 * memilih aset untuk baris detail baru.</p>
 *
 * <h3>Field turunan dari disposisi SOP</h3>
 *
 * <p>Berbeda dengan {@link PemakaianMasterAsset}, getter {@link #getDibuatOleh()},
 * {@link #getDisetujuiOleh()}, dan {@link #getTanggalPersetujuan()} di kelas ini AKTIF menimpa
 * nilai kolom database dengan data dari {@link #getDisposisiSop()} bila tersedia -- lihat javadoc
 * masing-masing getter untuk detail dan penanganan {@code LazyInitializationException}.</p>
 *
 * @see PeminjamanMasterAssetDetail baris detail unit barang yang dipinjam, membawa status kembali per-unit
 * @see PemakaianMasterAsset padanan permanen/tanpa tanggal kembali
 * @see PengembalianMasterAsset dokumen pengembalian header-level yang dapat ditautkan lewat {@link #getPengembalianMasterAsset()}
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "peminjaman_master_asset")
public class PeminjamanMasterAsset extends DataSop {

	/**
	 * Penanda versi serialisasi Java, identik di seluruh berkas entitas hbm2java sepaket (lihat
	 * catatan yang sama di {@link PemakaianMasterAsset#serialVersionUID}).
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, kolom {@code id}; di-generate database (strategi IDENTITY). */
	private Long id;

	/** Nomor urut/indeks tampilan baris. */
	private Long index;

	/** Nama tampil pengguna terakhir yang menyunting baris ini (jejak audit ringan). */
	private String oleh;

	/** Id pengguna terakhir yang menyunting baris ini (pasangan {@link #oleh}). */
	private String olehId;

	/** @return id pengguna terakhir yang menyunting baris ini, atau {@code null} bila belum terisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, MENGABAIKAN nilai kosong/blank agar proses batch
	 * tanpa pengguna aktif tidak menimpa jejak audit yang sudah tercatat.
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** @return {@link #kode} apa adanya (TANPA trim, berbeda dari {@link #getKode()} pada {@link PemakaianMasterAsset}), dipakai untuk log dan tampilan debug. */
	public String toString() {
		return kode;
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong/blank (lihat
	 * {@link #setOlehId(String)}).
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna terakhir yang menyunting baris ini, atau {@code null} bila belum terisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang mencatat waktu perubahan lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Field
	 * {@link #tanggal_dirubah} adalah field AUDIT SHADOW -- inisialisasi
	 * {@code = WaktuUtil.getDate()} saat objek dibuat adalah KEHARUSAN TEKNIS, bukan bug.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir; biasanya diisi otomatis lewat {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nomor/kode dokumen peminjaman; unik per baris, ditrim saat dibaca lewat {@link #getKode()}. */
	private String kode;

	/** Catatan/keterangan bebas terkait dokumen peminjaman ini. */
	private String keterangan;

	/** Nama peminjam, teks bebas (bukan FK ke entitas pegawai/pengguna). */
	private String namaPeminjam;

	/** Keperluan/tujuan peminjaman, teks bebas. */
	private String keperluan;

	/** Lokasi kegiatan tempat aset akan dipakai selama masa pinjam, teks bebas. */
	private String lokasiKegiatan;

	/** Tanggal mulai peminjaman; default hari ini bila belum diisi, lihat {@link #getTanggalPinjam()}. */
	private Date tanggalPinjam;

	/** Tanggal rencana/realisasi pengembalian; default hari ini bila belum diisi, lihat {@link #getTanggalKembali()}. */
	private Date tanggalKembali;

	/** Tanggal dokumen peminjaman dibuat. */
	private Date tanggalPembuatan;

	/** Tanggal dokumen peminjaman disetujui (kolom database; getter dapat menimpanya dari disposisi SOP). */
	private Date tanggalPersetujuan;

	/** Pengguna yang membuat dokumen peminjaman (kolom database; getter dapat menimpanya dari disposisi SOP). */
	private Tbmuser dibuatOleh;

	/** Pengguna yang menyetujui dokumen peminjaman (kolom database; getter dapat menimpanya dari disposisi SOP). */
	private Tbmuser disetujuiOleh;

	/** Simpul disposisi SOP yang mengatur alur persetujuan berjenjang dokumen ini. */
	private DisposisiSop disposisiSop;

	/** Unit kerja pemohon/peminjam aset. */
	private SatuanKerja satuanKerja;

	/** Estimasi nilai total aset yang dipinjam; default {@code 0.0} bila belum diisi, lihat {@link #getNilai()}. */
	private Double nilai;

	/** Tahun periode dokumen; default ke tahun berjalan bila belum diisi, lihat {@link #getTahun()}. */
	private Integer tahun;

	/** Bulan periode dokumen (1-12); default ke bulan berjalan bila belum diisi, lihat {@link #getBulan()}. */
	private Integer bulan;

	/** Alur penomoran surat yang dipakai untuk menghasilkan {@link #getKode()}; default {@link NomorSuratAlurPengadaan#PEMINJAMAN_BARANG_DATA}. */
	private NomorSuratAlurPengadaan nomorSuratAlurPengadaan;

	/**
	 * Tautan opsional ke dokumen pengembalian header-level. Lihat catatan kelas mengenai dua
	 * mekanisme pelacakan pengembalian yang tidak saling sinkron otomatis dengan flag
	 * {@code dikembalikan} per baris di {@link PeminjamanMasterAssetDetail}.
	 */
	private PengembalianMasterAsset pengembalianMasterAsset;

	/** Konstruktor default kosong, dibutuhkan Hibernate untuk instansiasi lewat refleksi. */
	public PeminjamanMasterAsset() {
	}

	/** @return kunci utama baris ini, atau {@code null} bila belum persisten. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama; kolom {@code insertable=false} sehingga hanya relevan setelah baris ada. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nomor/kode dokumen peminjaman setelah di-trim, atau {@code null} bila kosong/belum diisi. */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/** @param kode nomor/kode dokumen peminjaman; harus unik pada tabel {@code asset.peminjaman_master_asset}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return catatan/keterangan bebas dokumen ini, atau {@code null} bila tidak ada. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas dokumen ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param dibuatOleh pengguna yang membuat dokumen peminjaman (dapat ditimpa getter dari disposisi SOP). */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * @return pengguna pembuat dokumen. Nilai kolom database di-REFRESH lewat {@code check()},
	 *         lalu DITIMPA dengan pengaju disposisi awal ({@link DisposisiSop#getDisposisiStart()}
	 *         {@code .getDiajukanOleh()}) bila tersedia -- berbeda dari
	 *         {@link PemakaianMasterAsset#getDibuatOleh()} yang tidak menurunkan nilai dari
	 *         disposisi sama sekali.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		}
		return dibuatOleh;
	}

	/**
	 * @return pengguna yang menyetujui dokumen. Bila simpul disposisi SETUJU sudah punya pengaju,
	 *         nilai DITIMPA dari sana; sebaliknya bila disposisi ADA tetapi simpul setuju BELUM
	 *         punya pengaju, nilai DIPAKSA {@code null} (mengabaikan kolom database) -- sehingga
	 *         status "siapa yang menyetujui" selalu mengikuti kondisi disposisi SOP terkini,
	 *         bukan potongan data lama di kolom {@code disetujui_oleh}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujuiOleh = null;
		}
		return disetujuiOleh;
	}

	/** @param tanggalPersetujuan tanggal dokumen peminjaman disetujui (dapat ditimpa getter dari disposisi SOP). */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * @return tanggal persetujuan, diturunkan dari waktu disposisi SETUJU ({@link
	 *         DisposisiSop#getDisposisiSetuju()}{@code .getWaktu()}) bila pengajunya sudah ada,
	 *         atau dipaksa {@code null} bila disposisi ada tetapi simpul setuju belum punya
	 *         pengaju -- pola sama seperti {@link #getDisetujuiOleh()}.
	 *
	 *         <p>Seluruh akses ke {@link #getDisposisiSop()} dibungkus {@code try/catch} untuk
	 *         menangani {@code LazyInitializationException}: {@code disposisiSop} bisa berupa
	 *         instance canonical/shared (dipakai ulang {@link
	 *         ais.database.hibernate.AuditTimestampInterceptor}) yang proxy Hibernate-nya terikat
	 *         ke sesi lain yang SUDAH TERTUTUP. Bila itu terjadi, exception di-CATAT lewat {@link
	 *         ais.common.ErrorAuditUtil#record} dan getter tetap mengembalikan nilai fallback yang
	 *         sudah ada (baik dari kolom database maupun hasil percobaan sebelumnya) alih-alih
	 *         melempar exception ke pemanggil.</p>
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
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/asset/PeminjamanMasterAsset.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	/** @param disetujuiOleh pengguna yang menyetujui dokumen peminjaman (dapat ditimpa getter dari disposisi SOP). */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/** @param tanggalPembuatan tanggal dokumen peminjaman dibuat. */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * @return tanggal dokumen dibuat; bila belum pernah diisi, mengembalikan waktu SAAT DIPANGGIL
	 *         ({@link WaktuUtil#getDate()}) sebagai nilai bayangan -- tidak disimpan ke database
	 *         sampai baris di-persist.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/** @param index nomor urut/indeks tampilan baris. */
	public void setIndex(Long index) {
		this.index = index;
	}

	/** @return nomor urut/indeks tampilan baris. */
	public Long getIndex() {
		return index;
	}

	/** Nilai gabungan {@link #kode} dan id disposisi/baris, dijaga unik lewat {@link #getKodeUnik()}. */
	private String kodeUnik;

	/** Penanda status aktif dokumen; lihat efek samping penulisan pada {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * @return kode unik gabungan berformat {@code kode_idDisposisi} (atau {@code kode_idBaris}
	 *         bila belum ada disposisi SOP), DIHITUNG ULANG dan DITULIS ke field {@link
	 *         #kodeUnik} setiap kali getter ini dipanggil -- bukan hanya dibaca dari database.
	 *         Tujuannya memastikan nilai kolom {@code unique} ini selalu sinkron dengan kombinasi
	 *         kode dokumen dan disposisi terkininya, meski disposisi berubah setelah baris dibuat.
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/** @param kodeUnik nilai kode unik; akan DITIMPA ULANG oleh {@link #getKodeUnik()} pada pembacaan berikutnya. */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/** @return simpul disposisi SOP yang mengatur alur persetujuan dokumen ini, atau {@code null} bila belum ada. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menetapkan disposisi SOP, MENOLAK nilai {@code null} atau tanpa id lewat guard-clause awal.
	 * Baris ternary kedua adalah SISA SALIN-TEMPEL dari pola yang identik di
	 * {@link PemakaianMasterAsset#setDisposisiSop} -- lihat javadoc method tersebut untuk
	 * penjelasan mengapa kondisinya tidak pernah bernilai true di titik ini (kode mati, bukan bug
	 * fungsional).
	 *
	 * @param disposisiSop simpul disposisi SOP baru; diabaikan bila {@code null} atau belum ber-id
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/** @return unit kerja pemohon/peminjam aset, dilewatkan lewat {@code check()} (pola umum paket ini). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/** @param satuanKerja unit kerja pemohon/peminjam aset. */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** @return estimasi nilai total aset yang dipinjam; {@code 0.0} bila belum diisi. */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/** @param nilai estimasi nilai total aset yang dipinjam. */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * @return tahun periode dokumen; bila belum diisi, DIISI dan DIKEMBALIKAN tahun kalender saat
	 *         ini (efek samping penulisan pada method get, pola berulang di paket ini).
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/** @param tahun tahun periode dokumen. */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * @return bulan periode dokumen (1-12); bila belum diisi, DIISI dan DIKEMBALIKAN bulan
	 *         kalender saat ini (efek samping penulisan pada method get, sama seperti {@link #getTahun()}).
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/** @param bulan bulan periode dokumen (1-12). */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * @return alur penomoran surat yang berlaku; bila belum ditetapkan, DIISI dan DIKEMBALIKAN
	 *         konstanta {@link NomorSuratAlurPengadaan#PEMINJAMAN_BARANG_DATA} (efek samping
	 *         penulisan pada method get, sama seperti {@link #getTahun()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_pengadaan", nullable = true)
	public NomorSuratAlurPengadaan getNomorSuratAlurPengadaan() {
		if (nomorSuratAlurPengadaan == null) {
			nomorSuratAlurPengadaan = NomorSuratAlurPengadaan.PEMINJAMAN_BARANG_DATA;
		} else {
			nomorSuratAlurPengadaan = check(nomorSuratAlurPengadaan);
		}
		return nomorSuratAlurPengadaan;
	}

	/** @param nomorSuratAlurPengadaan alur penomoran surat yang dipakai untuk dokumen ini. */
	public void setNomorSuratAlurPengadaan(NomorSuratAlurPengadaan nomorSuratAlurPengadaan) {
		this.nomorSuratAlurPengadaan = nomorSuratAlurPengadaan;
	}

	/** @return nama peminjam (teks bebas, bukan FK), atau {@code null} bila belum diisi. */
	@Column(name = "nama_peminjam", nullable = true, columnDefinition = "text")
	public String getNamaPeminjam() {
		return namaPeminjam;
	}

	/** @param namaPeminjam nama peminjam (teks bebas). */
	public void setNamaPeminjam(String namaPeminjam) {
		this.namaPeminjam = namaPeminjam;
	}

	/** @return keperluan/tujuan peminjaman (teks bebas), atau {@code null} bila belum diisi. */
	@Column(name = "keperluan", nullable = true, columnDefinition = "text")
	public String getKeperluan() {
		return keperluan;
	}

	/** @param keperluan keperluan/tujuan peminjaman (teks bebas). */
	public void setKeperluan(String keperluan) {
		this.keperluan = keperluan;
	}

	/** @return lokasi kegiatan tempat aset dipakai selama masa pinjam (teks bebas), atau {@code null} bila belum diisi. */
	@Column(name = "lokasi_kegiatan", nullable = true, columnDefinition = "text")
	public String getLokasiKegiatan() {
		return lokasiKegiatan;
	}

	/** @param lokasiKegiatan lokasi kegiatan tempat aset dipakai selama masa pinjam (teks bebas). */
	public void setLokasiKegiatan(String lokasiKegiatan) {
		this.lokasiKegiatan = lokasiKegiatan;
	}

	/**
	 * @return tanggal rencana/realisasi pengembalian; bila belum pernah diisi, mengembalikan
	 *         tanggal SAAT DIPANGGIL ({@link WaktuUtil#getDate()}) sebagai nilai bayangan --
	 *         BUKAN berarti aset otomatis dianggap sudah kembali, murni nilai tampilan default.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalKembali() {
		return tanggalKembali == null ? WaktuUtil.getDate() : tanggalKembali;
	}

	/** @param tanggalKembali tanggal rencana/realisasi pengembalian. */
	public void setTanggalKembali(Date tanggalKembali) {
		this.tanggalKembali = tanggalKembali;
	}

	/** @return tanggal mulai peminjaman; default tanggal saat dipanggil bila belum pernah diisi (nilai bayangan). */
	@Temporal(TemporalType.DATE)
	public Date getTanggalPinjam() {
		return tanggalPinjam == null ? WaktuUtil.getDate() : tanggalPinjam;
	}

	/** @param tanggalPinjam tanggal mulai peminjaman. */
	public void setTanggalPinjam(Date tanggalPinjam) {
		this.tanggalPinjam = tanggalPinjam;
	}

	/**
	 * @return dokumen pengembalian header-level yang ditautkan, atau {@code null} bila belum ada
	 *         (termasuk bila seluruh baris detail sudah {@code dikembalikan = true} tetapi header
	 *         ini belum ditautkan secara eksplisit -- lihat catatan kelas soal dua mekanisme
	 *         pelacakan yang tidak saling sinkron otomatis).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengembalian_master_asset", nullable = true)
	public PengembalianMasterAsset getPengembalianMasterAsset() {
		return pengembalianMasterAsset;
	}

	/** @param pengembalianMasterAsset dokumen pengembalian header-level yang ditautkan ke peminjaman ini. */
	public void setPengembalianMasterAsset(PengembalianMasterAsset pengembalianMasterAsset) {
		this.pengembalianMasterAsset = pengembalianMasterAsset;
	}

	/**
	 * @return status aktif dokumen. Nilai kolom {@link #aktif} DIPAKSA {@code false} (menimpa
	 *         nilai tersimpan) bila disposisi SOP-nya sendiri tidak aktif, ATAU bila simpul akhir
	 *         disposisi menunjuk ke alur SOP yang menandai penolakan terjadi DI TITIK ITU
	 *         ({@code getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()}). Dengan kata lain,
	 *         dokumen peminjaman yang disposisinya ditolak otomatis dianggap tidak aktif meski
	 *         kolom database {@code aktif} tidak pernah diubah secara eksplisit. Bila tidak ada
	 *         kondisi di atas yang terpenuhi dan field belum diisi, default {@code true}.
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif dokumen; dapat ditimpa oleh {@link #getAktif()} berdasarkan kondisi disposisi SOP. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
