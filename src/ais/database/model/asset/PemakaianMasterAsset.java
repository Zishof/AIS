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

import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.rab.Mitra;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * HEADER dokumen PEMAKAIAN aset -- penugasan/penyerahan satu atau lebih unit aset kepada
 * seorang pegawai, unit kerja, atau mitra eksternal untuk DIPAKAI, bersifat terbuka/permanen
 * (tidak seperti {@link PeminjamanMasterAsset} yang meminjamkan aset untuk sementara).
 *
 * <h3>Pemakaian vs Peminjaman -- pembeda dari field, bukan dari nama</h3>
 *
 * <p>Kelas ini TIDAK memiliki field tanggal pinjam/kembali, nama peminjam, keperluan, lokasi
 * kegiatan, atau tautan ke dokumen pengembalian -- semuanya ada di {@link PeminjamanMasterAsset}.
 * Sebaliknya, kelas ini membawa {@link #getPemilikAsset()} (unit/perorangan yang ditetapkan
 * sebagai pemegang aset), {@link #getLokasi()} dan {@link #getRuang()} (lokasi fisik penempatan
 * permanen), serta {@link #getMitra()} (bila aset diserahkan ke mitra eksternal, bukan unit
 * internal). Kesimpulannya: "pemakaian" mencatat SIAPA memegang aset dan DI MANA aset
 * ditempatkan untuk jangka panjang, sedangkan "peminjaman" mencatat transaksi keluar-masuk
 * aset yang harus dikembalikan pada tanggal tertentu. Tidak ada kolom status yang menandai
 * apakah aset sedang "dipakai" secara eksklusif; beberapa baris pemakaian dapat dibuat untuk
 * aset yang sama tanpa penjagaan otomatis di tingkat entitas ini (lihat javadoc
 * {@link PemakaianMasterAssetDetail} untuk rincian baris barang).</p>
 *
 * <h3>Alur SOP dan penomoran</h3>
 *
 * <p>Mewarisi {@link ais.database.model.sop.DataSop} sehingga terhubung ke alur disposisi
 * berjenjang lewat {@link #getDisposisiSop()}; field {@link #getDibuatOleh()} dan
 * {@link #getDisetujuiOleh()} di kelas ini TIDAK menurunkan nilainya dari disposisi (berbeda
 * dengan pola yang sama di {@link PeminjamanMasterAsset}), melainkan diisi langsung saat
 * pembuatan/persetujuan dokumen. Nomor dokumen ({@link #getKode()}) unik dan alur penomoran
 * suratnya menggunakan {@link NomorSuratAlurPengadaan#PEMAKAIAN_BARANG_DATA} sebagai default.</p>
 *
 * <p>Baris @{@link org.hibernate.envers.Audited} mengaktifkan riwayat perubahan otomatis lewat
 * Hibernate Envers; tabel fisiknya {@code asset.pemakaian_master_asset}.</p>
 *
 * @see PemakaianMasterAssetDetail baris detail barang yang dipakai, menunjuk balik ke kelas ini
 * @see PeminjamanMasterAsset padanan untuk pemakaian SEMENTARA dengan tanggal kembali
 * @see PemilikAsset entitas independen (bukan snapshot) yang menjadi rujukan pemegang aset
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "pemakaian_master_asset")
public class PemakaianMasterAsset extends DataSop {

	/**
	 * Penanda versi serialisasi Java. Nilai ini identik di seluruh berkas entitas hbm2java
	 * sepaket karena dihasilkan dari templat yang sama; tidak masalah karena hanya dibandingkan
	 * antar-versi kelas yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, kolom {@code id}; di-generate database (strategi IDENTITY). */
	private Long id;

	/** Nomor urut/indeks tampilan baris; tidak dipetakan ke kolom database bertipe khusus selain kolomnya sendiri. */
	private Long index;

	/** Nama tampil pengguna terakhir yang menyunting baris ini (jejak audit ringan, bukan FK ke {@code Tbmuser}). */
	private String oleh;

	/** Id pengguna terakhir yang menyunting baris ini (pasangan {@link #oleh}). */
	private String olehId;

	/**
	 * @return id pengguna terakhir yang menyunting baris ini, atau {@code null} bila belum terisi.
	 */
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

	// public String toString() {
	// return kode;
	// }

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong/blank (lihat
	 * {@link #setOlehId(String)} untuk alasan yang sama).
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna terakhir yang menyunting baris ini, atau {@code null} bila belum terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang mencatat waktu perubahan lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini
	 * di-{@code UPDATE}. Field {@link #tanggal_dirubah} yang menyertainya adalah field AUDIT
	 * SHADOW -- inisialisasi {@code = WaktuUtil.getDate()} saat objek dibuat adalah KEHARUSAN
	 * TEKNIS (nilai default sebelum callback pertama berjalan), bukan bug.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir; biasanya diisi otomatis lewat {@link #onUpdate()}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nomor/kode dokumen pemakaian; unik per baris, ditrim saat dibaca lewat {@link #getKode()}. */
	private String kode;

	/** Catatan/keterangan bebas terkait dokumen pemakaian ini. */
	private String keterangan;

	/** Pemegang/penerima aset menurut daftar rujukan independen {@link PemilikAsset}. */
	private PemilikAsset pemilikAsset;

	/** Lokasi fisik penempatan aset hasil pemakaian ini. */
	private Lokasi lokasi;

	/** Ruang spesifik di dalam lokasi tempat aset ditempatkan. */
	private Ruang ruang;

	/** Mitra eksternal penerima aset, bila pemakaian ini diserahkan ke pihak luar (bukan unit internal). */
	private Mitra mitra;

	/** Tanggal dokumen pemakaian dibuat. */
	private Date tanggalPembuatan;

	/** Tanggal dokumen pemakaian disetujui. */
	private Date tanggalPersetujuan;

	/** Pengguna yang membuat dokumen pemakaian. */
	private Tbmuser dibuatOleh;

	/** Pengguna yang menyetujui dokumen pemakaian. */
	private Tbmuser disetujuiOleh;

	/** Unit kerja pemohon/pemakai aset. */
	private SatuanKerja satuanKerja;

	/** Simpul disposisi SOP yang mengatur alur persetujuan berjenjang dokumen ini. */
	private DisposisiSop disposisiSop;

	/** Bulan periode dokumen (1-12); default ke bulan berjalan bila belum diisi, lihat {@link #getBulan()}. */
	private Integer bulan;

	/** Alur penomoran surat yang dipakai untuk menghasilkan {@link #getKode()}; default {@link NomorSuratAlurPengadaan#PEMAKAIAN_BARANG_DATA}. */
	private NomorSuratAlurPengadaan nomorSuratAlurPengadaan;

	/** Tahun periode dokumen; default ke tahun berjalan bila belum diisi, lihat {@link #getTahun()}. */
	private Integer tahun;

	/**
	 * @return representasi ringkas berupa {@code id-kode}, dipakai untuk log dan tampilan debug.
	 */
	public String toString() {
		return id + "-" + kode;
	}

	/** Konstruktor default kosong, dibutuhkan Hibernate untuk instansiasi lewat refleksi. */
	public PemakaianMasterAsset() {
	}

	/**
	 * @return kunci utama baris ini, atau {@code null} bila belum persisten.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama; kolom {@code insertable=false} sehingga nilai ini hanya relevan setelah baris ada (di-generate database). */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nomor/kode dokumen pemakaian setelah di-trim, atau {@code null} bila kosong/belum diisi.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/** @param kode nomor/kode dokumen pemakaian; harus unik pada tabel {@code asset.pemakaian_master_asset}. */
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

	/** @param dibuatOleh pengguna yang membuat dokumen pemakaian; wajib diisi (kolom {@code NOT NULL}). */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * @return pengguna yang membuat dokumen pemakaian ini. Berbeda dengan
	 *         {@link PeminjamanMasterAsset#getDibuatOleh()}, getter ini TIDAK menurunkan nilainya
	 *         dari {@link #getDisposisiSop()} -- nilai kolom database dipakai apa adanya.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		return dibuatOleh;
	}

	/** @param disetujuiOleh pengguna yang menyetujui dokumen pemakaian. */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * @return pengguna yang menyetujui dokumen pemakaian, atau {@code null} bila belum disetujui.
	 *         Sama seperti {@link #getDibuatOleh()}, tidak diturunkan dari disposisi SOP.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		return disetujuiOleh;
	}

	/** @param tanggalPembuatan tanggal dokumen pemakaian dibuat. */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * @return tanggal dokumen dibuat; bila belum pernah diisi (baris baru), mengembalikan waktu
	 *         SAAT DIPANGGIL ({@link WaktuUtil#getDate()}) sebagai nilai bayangan -- bukan nilai
	 *         yang disimpan ke database sampai baris di-persist.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {

		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/** @param tanggalPersetujuan tanggal dokumen pemakaian disetujui. */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/** @return tanggal dokumen disetujui, atau {@code null} bila belum disetujui (tanpa nilai bayangan). */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		return tanggalPersetujuan;
	}

	/**
	 * @return {@link PemilikAsset} yang ditetapkan sebagai pemegang aset hasil pemakaian ini, atau
	 *         {@code null} bila belum ditetapkan. Entitas {@link PemilikAsset} adalah rujukan
	 *         master INDEPENDEN (bukan snapshot yang diturunkan otomatis dari transaksi
	 *         pemakaian/peminjaman) -- lihat javadoc kelas {@link PemilikAsset}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pemilik_asset", nullable = true)
	public PemilikAsset getPemilikAsset() {
		return pemilikAsset;
	}

	/** @param pemilikAsset pemegang/penerima aset yang ditetapkan untuk dokumen ini. */
	public void setPemilikAsset(PemilikAsset pemilikAsset) {
		this.pemilikAsset = pemilikAsset;
	}

	/** @return lokasi fisik penempatan aset, atau {@code null} bila belum ditetapkan. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		return lokasi;
	}

	/** @param lokasi lokasi fisik penempatan aset. */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * @return ruang spesifik penempatan aset, dilewatkan lewat {@code check()} untuk memastikan
	 *         proxy Hibernate lama (mis. dari sesi yang sudah tertutup) diganti proxy segar
	 *         sebelum dipakai -- pola umum di seluruh entitas paket ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);
		return this.ruang;
	}

	/** @param ruang ruang spesifik penempatan aset. */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/** @param index nomor urut/indeks tampilan baris. */
	public void setIndex(Long index) {
		this.index = index;
	}

	/** @return nomor urut/indeks tampilan baris. */
	public Long getIndex() {
		return index;
	}

	/** @return mitra eksternal penerima aset, atau {@code null} bila pemakaian ini untuk unit internal. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mitra", nullable = true)
	public Mitra getMitra() {
		return mitra;
	}

	/** @param mitra mitra eksternal penerima aset. */
	public void setMitra(Mitra mitra) {
		this.mitra = mitra;
	}

	/** @return unit kerja pemohon/pemakai aset, dilewatkan lewat {@code check()} (lihat {@link #getRuang()}). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/** @param satuanKerja unit kerja pemohon/pemakai aset. */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** @return simpul disposisi SOP yang mengatur alur persetujuan dokumen ini, atau {@code null} bila belum ada. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menetapkan disposisi SOP, MENOLAK nilai {@code null} atau tanpa id (baris belum persisten)
	 * lewat guard-clause awal. Baris ternary kedua ({@code this.disposisiSop != null &&
	 * (disposisiSop == null || disposisiSop.getId() == null) ? this.disposisiSop : disposisiSop})
	 * adalah SISA SALIN-TEMPEL dari pola yang sama di {@link PeminjamanMasterAsset#setDisposisiSop}
	 * -- kondisinya tidak pernah bernilai true di sini karena guard-clause di atas sudah
	 * memastikan {@code disposisiSop} non-null dan ber-id sebelum baris ini dieksekusi, sehingga
	 * secara efektif method ini SELALU menimpa field dengan nilai baru yang valid. Kode mati,
	 * bukan bug fungsional.
	 *
	 * @param disposisiSop simpul disposisi SOP baru; diabaikan bila {@code null} atau belum ber-id
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * @return tahun periode dokumen; bila belum diisi, DIISI dan DIKEMBALIKAN sebagai tahun
	 *         kalender saat ini (efek samping penulisan pada method get -- pola berulang di
	 *         paket ini untuk memastikan field wajib laporan periode selalu punya nilai).
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
	 * @return alur penomoran surat yang berlaku; bila belum ditetapkan, DIISI dan DIKEMBALIKAN
	 *         konstanta {@link NomorSuratAlurPengadaan#PEMAKAIAN_BARANG_DATA} (efek samping
	 *         penulisan pada method get, sama seperti {@link #getTahun()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_pengadaan", nullable = true)
	public NomorSuratAlurPengadaan getNomorSuratAlurPengadaan() {
		if (nomorSuratAlurPengadaan == null) {
			nomorSuratAlurPengadaan = NomorSuratAlurPengadaan.PEMAKAIAN_BARANG_DATA;
		} else {
			nomorSuratAlurPengadaan = check(nomorSuratAlurPengadaan);
		}
		return nomorSuratAlurPengadaan;
	}

	/** @param nomorSuratAlurPengadaan alur penomoran surat yang dipakai untuk dokumen ini. */
	public void setNomorSuratAlurPengadaan(NomorSuratAlurPengadaan nomorSuratAlurPengadaan) {
		this.nomorSuratAlurPengadaan = nomorSuratAlurPengadaan;
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

}
