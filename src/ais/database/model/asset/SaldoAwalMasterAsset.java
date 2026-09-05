package ais.database.model.asset;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

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
import ais.database.model.inventory.Toko;

import ais.common.Common;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * <h2>SaldoAwalMasterAsset — dokumen tagihan vendor (invoice) pada alur pengadaan aset.</h2>
 *
 * <p>
 * Nama kelas ini historis dan bisa menyesatkan: entity ini BUKAN sekadar "saldo awal" migrasi.
 * Dalam alur pengadaan aset lengkap (Permintaan → Pemesanan → Penerimaan → <b>Tagihan/Saldo Awal</b>
 * → Pembayaran), baris di kelas inilah yang merepresentasikan <b>dokumen tagihan vendor</b> yang
 * menaungi informasi pajak (breakdown PPN/PPh), status persetujuan (via {@link DisposisiSop}),
 * status lunas, dan penomoran dokumen — bukan sekadar angka pembuka saldo. Header dokumen ini
 * dipasangkan dengan satu atau lebih baris {@link SaldoAwalMasterAssetDetail} yang memuat rincian
 * per aset (qty, harga, pajak per baris).
 * </p>
 *
 * <h3>Warisan nilai dari {@link PenerimaanPengadaanMasterAsset}</h3>
 * <p>
 * Banyak getter di kelas ini (mis. {@link #getKeterangan()}, {@link #getDibuatOleh()},
 * {@link #getTanggalPembuatan()}, {@link #getPemilikAsset()}, {@link #getLokasi()},
 * {@link #getRuang()}, {@link #getSatuanKerja()}, {@link #getNilai()}, {@link #getPenyedia()},
 * {@link #getKodeTermin()}, {@link #getKeteranganTermin()}, {@link #getJsonTermin()},
 * {@link #getKodeTagihan()}, {@link #getTanggalTagihan()}) mengikuti pola "warisi dari
 * {@link #getPenerimaanPengadaanMasterAsset()} bila tersedia, jatuh ke field lokal bila tidak" —
 * menjaga agar dokumen tagihan selalu konsisten dengan BAST asalnya tanpa duplikasi entri manual.
 * </p>
 *
 * <h3>Persetujuan via {@link DisposisiSop}</h3>
 * <p>
 * {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPembuatan()},
 * {@link #getTanggalPersetujuan()}, dan {@link #getAktif()} seluruhnya ikut menelusuri
 * {@link #getDisposisiSop()} (alur disposisi/persetujuan generik framework SOP) untuk menentukan
 * siapa pembuat/penyetuju aktual dan apakah dokumen ini masih berlaku — dokumen yang alur
 * disposisinya ditolak otomatis dianggap tidak aktif ({@link #getAktif()}).
 * </p>
 *
 * <h3>Mode Breakdown pajak (Bukti Potong vs Sesuai PO)</h3>
 * <p>
 * {@link #getBreakdownAktif() breakdownAktif} membedakan dua mode perhitungan pajak dokumen:
 * {@code true} = mode Breakdown (PPh dihitung dari {@link #getBreakdownBuktiPotong() Bukti
 * Potong} yang diinput manual), {@code false}/{@code null} = mode "Sesuai PO" (PPh dihitung per
 * baris {@link SaldoAwalMasterAssetDetail}, dijumlahkan). {@link #getBreakdownJenisPph()}
 * menentukan jenis PPh untuk baris jurnal saat mode Breakdown aktif — namun NILAI PPh tetap
 * berasal dari Bukti Potong, bukan dihitung ulang dari persentase jenis pajak tersebut.
 * </p>
 *
 * <h3>Pemetaan basis data &amp; audit</h3>
 * <p>
 * Dipetakan ke tabel <code>asset.saldo_awal_master_asset</code>, mewarisi {@link DataSop} (bukan
 * langsung {@link ais.database.model.GeneralValueObject GeneralValueObject}) untuk terintegrasi
 * dengan mekanisme alur SOP. Field jejak {@code oleh}/{@code olehId}/{@code tanggal_dirubah} diisi
 * otomatis lewat hook {@link javax.persistence.PreUpdate} {@link #onUpdate()}
 * ({@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}), dan setiap perubahan
 * direkam ke tabel revisi Envers karena kelas ditandai {@link org.hibernate.envers.Audited @Audited}.
 * </p>
 *
 * @author AIS
 * @see SaldoAwalMasterAssetDetail
 * @see PenerimaanPengadaanMasterAsset
 * @see DisposisiSop
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "asset", name = "saldo_awal_master_asset")
public class SaldoAwalMasterAsset extends DataSop {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak perlu
	 * diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.saldo_awal_master_asset}. */
	private Long id;
	/** Nomor urut tampilan (bukan primary key), dipakai mis. untuk penomoran baris di tabel UI. */
	private Long index;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String olehId;

	/**
	 * @return id pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila belum
	 *         pernah diubah sejak dimuat.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna audit. Nilai {@code null}/kosong diabaikan agar jejak lama tidak
	 * tertimpa hampa.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	// public String toString() {
	// return kode;
	// }

	/**
	 * Mengisi nama pengguna audit. Nilai {@code null}/kosong diabaikan, sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila
	 *         belum pernah diubah sejak dimuat.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus hidup JPA yang dipanggil Hibernate tepat sebelum setiap {@code UPDATE}.
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #tanggal_dirubah}, {@link #oleh}, dan {@link #olehId} dengan waktu serta
	 * identitas pengguna aktif. Dipicu otomatis oleh Hibernate, tidak dipanggil manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Waktu perubahan terakhir baris ini; diperbarui otomatis oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Field diinisialisasi ke waktu saat objek dibuat, lalu
	 * ditimpa ulang oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini; tidak pernah {@code null}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode dokumen unik (wajib), lihat {@link #getKode()}. */
	private String kode;
	/** Keterangan bebas; bisa diwarisi dari BAST asal, lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Toko/outlet terkait dokumen ini; opsional. */
	private Toko toko;
	/** Pemilik aset; bisa diwarisi dari BAST asal, lihat {@link #getPemilikAsset()}. */
	private PemilikAsset pemilikAsset;
	/** Vendor/penyedia; bisa diwarisi dari BAST asal, lihat {@link #getPenyedia()}. */
	private PenyediaAsset penyedia;
	/** Lokasi aset; bisa diwarisi dari BAST asal, lihat {@link #getLokasi()}. */
	private Lokasi lokasi;
	/** Ruang aset; bisa diwarisi dari BAST asal, lihat {@link #getRuang()}. */
	private Ruang ruang;
	/** Tanggal dokumen dibuat; punya fallback kompleks via disposisi/BAST, lihat {@link #getTanggalPembuatan()}. */
	private Date tanggalPembuatan;
	/** Tanggal dokumen disetujui; punya fallback kompleks via disposisi, lihat {@link #getTanggalPersetujuan()}. */
	private Date tanggalPersetujuan;
	/** Pengguna pembuat dokumen; bisa diwarisi dari disposisi/BAST, lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
	/** Pengguna penyetuju dokumen; ditentukan dari disposisi, lihat {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujuiOleh;
	/** Nilai/nominal dokumen; kalkulasi kompleks, lihat {@link #getNilai()}. */
	private Double nilai;
	/** Jumlah yang sudah dibayar; tergantung status transfer dana, lihat {@link #getDibayar()}. */
	private Double dibayar;
	/** Status lunas; dihitung dari perbandingan {@link #getNilai()} vs {@link #getDibayar()}. */
	private Boolean lunas;
	/** BAST/penerimaan pengadaan asal yang menjadi sumber warisan banyak field pada kelas ini. */
	private PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset;
	/** Satuan kerja pemilik dokumen; bisa diwarisi dari BAST asal, lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;

	/** Tahun pembuatan dokumen; default tahun berjalan bila belum diisi. */
	private Integer tahun;
	/** Bulan pembuatan dokumen (1-12); default bulan berjalan bila belum diisi. */
	private Integer bulan;
	/** Jenis alur pengadaan dokumen ini; default {@link NomorSuratAlurPengadaan#PENERIMAAN_TAGIHAN_DATA}. */
	private NomorSuratAlurPengadaan nomorSuratAlurPengadaan;
	/** Alur disposisi/persetujuan SOP dokumen ini; sumber banyak field turunan (lihat javadoc kelas). */
	private DisposisiSop disposisiSop;

	/** @return representasi ringkas {@code id-kode} untuk log/combobox. */
	public String toString() {
		return id + "-" + kode;
	}

	/** Id sementara in-memory (bukan primary key), dibuat acak sekali saat pertama diminta. */
	private Long idTemp;
	/** Kode unik gabungan {@link #getKode()}+{@link #getKodeTermin()}, lihat {@link #getKodeUnik()}. */
	private String kodeUnik;
	/** Status aktif dokumen; ditentukan dari status {@link #getDisposisiSop()}, lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Kode termin pembayaran; bisa diwarisi dari BAST asal, lihat {@link #getKodeTermin()}. */
	private String kodeTermin;
	/** Keterangan termin pembayaran; bisa diwarisi dari BAST asal, lihat {@link #getKeteranganTermin()}. */
	private String keteranganTermin;
	/** JSON detail termin (mis. field {@code penagihan}); bisa diwarisi dari BAST, lihat {@link #getJsonTermin()}. */
	private String jsonTermin;

	/** Riwayat posting jurnal akuntansi dokumen ini, bila sudah diposting; opsional. */
	private PostingHistory postingHistory;
	/** Pengajuan transfer dana untuk pembayaran dokumen ini; menentukan {@link #getDibayar()}. */
	private DaftarPengajuanTransfer daftarPengajuanTransfer;
	/** Tanggal persetujuan manual (di luar alur disposisi normal); opsional. */
	private Date tanggalPersetujuanManual;
	/** Kode tagihan; bisa diwarisi dari BAST asal, lihat {@link #getKodeTagihan()}. */
	private String kodeTagihan;
	/** Tanggal tagihan; bisa diwarisi dari BAST asal, lihat {@link #getTanggalTagihan()}. */
	private Date tanggalTagihan;

	/** Persentase PPN untuk mode Breakdown; opsional. */
	private Double breakdownPpnPersen;
	/** Nilai PPh dari Bukti Potong untuk mode Breakdown; sumber nilai PPh saat mode Breakdown aktif. */
	private Double breakdownBuktiPotong;
	/** Catatan khusus bebas untuk mode Breakdown; opsional. */
	private String breakdownSpecialNotes;
	/** true = mode Breakdown (PPh dari Bukti Potong); false/null = Sesuai PO (PPh per detail). */
	private Boolean breakdownAktif;
	/** Jenis PPh utk baris pajak mode Breakdown (dipakai POSTING jurnal; NILAI tetap dari Bukti Potong). */
	private JenisPajakBarang breakdownJenisPph;

	/**
	 * Mengembalikan kode unik dokumen: gabungan {@link #getKode()} dan {@link #getKodeTermin()}
	 * (dipisah {@code _}) bila kode termin terisi, atau {@link #getKode()} saja bila tidak. Nilai
	 * ditulis balik ke field {@link #kodeUnik} setiap kali getter ini dipanggil (bukan murni
	 * pembacaan), sehingga selalu sinkron dengan {@link #kode}/{@link #kodeTermin} terkini.
	 *
	 * @return kode unik dokumen; bisa {@code null} bila {@link #getKode()} juga {@code null}.
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + (getKodeTermin().trim().isEmpty() ? "" : "_" + kodeTermin);
		return kodeUnik;
	}

	/**
	 * Mengisi kode unik secara langsung. Nilai ini akan ditimpa ulang setiap kali
	 * {@link #getKodeUnik()} dipanggil.
	 *
	 * @param kodeUnik kode unik, boleh {@code null}.
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/** Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi via refleksi. */
	public SaldoAwalMasterAsset() {
	}

	/**
	 * Konstruktor kenyamanan untuk merujuk baris existing hanya dari id-nya (mis. untuk
	 * dipasangkan sebagai referensi FK tanpa memuat seluruh objek).
	 *
	 * @param id primary key baris yang dirujuk.
	 */
	public SaldoAwalMasterAsset(Long id) {
		this.id = id;
	}

	/** @return primary key baris ini, atau {@code null} untuk instance baru yang belum disimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key. Kolom database bersifat {@code insertable = false} (IDENTITY,
	 * auto-generate oleh database), sehingga pengisian manual tidak berpengaruh pada
	 * {@code INSERT}.
	 *
	 * @param id primary key.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return kode dokumen, sudah di-{@code trim}; {@code null} bila kosong/belum diisi. */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/**
	 * Mengisi kode dokumen. Tidak melakukan trim di sisi setter — trimming terjadi hanya saat
	 * dibaca lewat {@link #getKode()}.
	 *
	 * @param kode kode dokumen.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	// KE-1/KE-2 (16-07-2026): keterangan diisi via MyTextbox 4-baris (bebas panjang),
	// tapi kolom default varchar(255) tanpa columnDefinition -> "value too long" saat
	// insert (pola sama dgn Item.by_statement, lihat item-varchar255-overflow-by-statement).
	/**
	 * Mengembalikan keterangan dokumen. Bila {@link #penerimaanPengadaanMasterAsset} (BAST asal)
	 * terisi dan memiliki keterangan sendiri, nilai tersebut MENIMPA field lokal {@link #keterangan}
	 * tanpa syarat — dokumen tagihan selalu mencerminkan keterangan BAST terbaru selama tertaut.
	 * Kolom dipetakan {@code columnDefinition = "text"} (bukan {@code varchar(255)} default) agar
	 * menampung isian bebas-panjang dari widget input 4-baris tanpa error "value too long" saat
	 * {@code INSERT} (lihat catatan KE-1/KE-2 16-07-2026 di atas method ini).
	 *
	 * @return keterangan dokumen, boleh {@code null} bila kosong di kedua sumber.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {

		if (penerimaanPengadaanMasterAsset != null && penerimaanPengadaanMasterAsset.getKeterangan() != null) {
			keterangan = penerimaanPengadaanMasterAsset.getKeterangan();
		}

		return this.keterangan;
	}

	/**
	 * Mengisi keterangan secara langsung. Nilai ini bisa ditimpa tanpa syarat oleh fallback pada
	 * {@link #getKeterangan()} bila BAST asal memiliki keterangan sendiri.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan toko/outlet terkait, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)} (via superclass {@link DataSop}).
	 *
	 * @return {@link Toko} terkait, atau {@code null} bila belum ditetapkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = true)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/**
	 * Mengisi toko/outlet terkait.
	 *
	 * @param toko toko terkait, boleh {@code null}.
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Mengisi pengguna pembuat dokumen secara langsung. Nilai ini bisa ditimpa tanpa syarat oleh
	 * fallback pada {@link #getDibuatOleh()}.
	 *
	 * @param dibuatOleh pengguna pembuat, boleh {@code null}.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna pembuat dokumen, dengan dua lapis fallback (yang belakangan
	 * ditulis menimpa yang sebelumnya bila kondisinya terpenuhi): pertama field lokal diresolusi
	 * lewat {@link GeneralValueObject#check(Object)}, lalu diwarisi dari
	 * {@link #penerimaanPengadaanMasterAsset} bila BAST asal punya nilai; TERAKHIR, bila
	 * {@link #getDisposisiSop()} memiliki tahap mulai ({@code getDisposisiStart()}) dengan
	 * pengaju, pengguna pengaju itulah yang MENANG di atas kedua sumber sebelumnya — pencatatan
	 * siapa yang mengajukan alur SOP dianggap sumber kebenaran paling otoritatif.
	 *
	 * @return {@link Tbmuser} pembuat dokumen, atau {@code null} bila tidak ditemukan di ketiga
	 *         sumber.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		if (penerimaanPengadaanMasterAsset != null && penerimaanPengadaanMasterAsset.getDibuatOleh() != null) {
			dibuatOleh = penerimaanPengadaanMasterAsset.getDibuatOleh();
		}

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		}

		return dibuatOleh;
	}

	/**
	 * Mengisi pengguna penyetuju dokumen secara langsung. Nilai ini SELALU ditimpa ulang oleh
	 * {@link #getDisetujuiOleh()} (baik dari disposisi maupun di-null-kan) — lihat javadoc getter
	 * itu untuk detail.
	 *
	 * @param disetujuiOleh pengguna penyetuju, boleh {@code null}.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan pengguna penyetuju dokumen, DITENTUKAN SEPENUHNYA oleh status
	 * {@link #getDisposisiSop()} (bukan dari field lokal, walau field lokal tetap diresolusi lebih
	 * dulu lewat {@link GeneralValueObject#check(Object)} sebagai nilai awal sebelum ditimpa).
	 * Warisan dari {@link #penerimaanPengadaanMasterAsset} (baris komentar di tengah method)
	 * SENGAJA DINONAKTIFKAN — kode ini sisa perubahan yang tidak dihapus, menandakan keputusan
	 * desain bahwa persetujuan TAGIHAN tidak boleh otomatis mewarisi persetujuan BAST (dua
	 * keputusan bisnis yang berbeda meski berasal dari dokumen yang bertaut).
	 *
	 * <p>Bila {@link #getDisposisiSop()} memiliki tahap setuju ({@code getDisposisiSetuju()})
	 * dengan pengaju, pengguna itu yang dikembalikan. SEBALIKNYA — ini bagian penting — bila
	 * disposisi ADA tapi tahap setuju BELUM ADA atau belum punya pengaju, {@link #disetujuiOleh}
	 * DIPAKSA {@code null} secara eksplisit, MENGHAPUS nilai field lokal manapun yang mungkin
	 * sudah tersimpan. Efeknya: begitu {@link #getDisposisiSop()} terpasang pada dokumen ini,
	 * status "disetujui oleh" TIDAK BISA lagi diisi manual lewat {@link #setDisetujuiOleh(Tbmuser)}
	 * — field tersebut sepenuhnya dikendalikan status alur disposisi.
	 *
	 * @return {@link Tbmuser} penyetuju, atau {@code null} bila belum disetujui atau tidak ada
	 *         alur disposisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);
//		if (penerimaanPengadaanMasterAsset != null && penerimaanPengadaanMasterAsset.getDisetujuiOleh() != null) {
//			disetujuiOleh = penerimaanPengadaanMasterAsset.getDisetujuiOleh();
//		}

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

	/**
	 * Mengisi tanggal pembuatan secara langsung. Nilai ini bisa ditimpa oleh fallback pada
	 * {@link #getTanggalPembuatan()}.
	 *
	 * @param tanggalPembuatan tanggal pembuatan, boleh {@code null}.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan tanggal pembuatan dokumen, dengan dua lapis fallback berurutan: diwarisi dari
	 * {@link #penerimaanPengadaanMasterAsset}{@code .getTanggalPembuatan()} bila BAST asal
	 * tersedia; KEMUDIAN, bila {@link #getDisposisiSop()} memiliki tahap mulai dengan pengaju,
	 * waktu tahap mulai itu MENIMPA hasil warisan BAST (waktu pengajuan alur SOP dianggap lebih
	 * otoritatif). Bila kedua sumber kosong, jatuh ke {@link WaktuUtil#getDate()} (waktu saat ini)
	 * sebagai fallback terakhir.
	 *
	 * <p>Seluruh navigasi dibungkus {@code try/catch (Exception ...)} (dicatat ke
	 * {@link ais.common.ErrorAuditUtil}) karena {@link #penerimaanPengadaanMasterAsset}/
	 * {@link #disposisiSop} bisa berupa instance kanonis/shared (lewat
	 * {@code AuditTimestampInterceptor}) yang proxy-nya terikat ke sesi Hibernate lain yang sudah
	 * tertutup — kegagalan navigasi tidak boleh menjatuhkan getter ini, cukup lanjut dengan nilai
	 * fallback yang sudah ada.</p>
	 *
	 * @return tanggal pembuatan dokumen; tidak pernah {@code null} (fallback terakhir waktu saat
	 *         ini).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {

		try {
			// FIX LazyInitializationException: penerimaanPengadaanMasterAsset/disposisiSop bisa berupa
			// instance canonical/shared (AuditTimestampInterceptor) yang proxy-nya terikat ke Session
			// lain yang sudah closed -> jangan biarkan getter ini crash, cukup lewati bagian ini
			// (nilai fallback dipertahankan).
			if (penerimaanPengadaanMasterAsset != null && penerimaanPengadaanMasterAsset.getTanggalPembuatan() != null) {
				tanggalPembuatan = penerimaanPengadaanMasterAsset.getTanggalPembuatan();
			}

			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
					&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
				tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/asset/SaldoAwalMasterAsset.java:getTanggalPembuatan-lazy");
		}

		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Mengisi tanggal persetujuan secara langsung. Nilai ini bisa ditimpa/dikosongkan oleh
	 * fallback pada {@link #getTanggalPersetujuan()}.
	 *
	 * @param tanggalPersetujuan tanggal persetujuan, boleh {@code null}.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan tanggal persetujuan dokumen, mengikuti logika serupa
	 * {@link #getDisetujuiOleh()}: SEPENUHNYA ditentukan oleh status {@link #getDisposisiSop()}.
	 * Bila tahap setuju disposisi memiliki pengaju, waktu tahap setuju itu yang dipakai; bila
	 * disposisi ADA tapi tahap setuju belum ada/belum punya pengaju, {@link #tanggalPersetujuan}
	 * DIPAKSA {@code null} (menghapus nilai lama). SETELAH itu, bila
	 * {@link #getTanggalPersetujuanManual()} terisi DAN {@link #getDisetujuiOleh()} tidak
	 * {@code null} (dokumen benar-benar sudah disetujui oleh seseorang), tanggal persetujuan
	 * manual MENIMPA hasil di atas — memberi jalur override manual untuk kasus di mana tanggal
	 * disposisi otomatis tidak sesuai kebutuhan bisnis (mis. entri data mundur/backdate), namun
	 * hanya berlaku bila status persetujuan itu sendiri sudah valid.
	 *
	 * <p>Sama seperti {@link #getTanggalPembuatan()}, seluruh navigasi dibungkus
	 * {@code try/catch (Exception ...)} untuk mengantisipasi {@code LazyInitializationException}
	 * pada proxy {@link #disposisiSop} yang sesinya sudah tertutup.</p>
	 *
	 * @return tanggal persetujuan, atau {@code null} bila dokumen belum disetujui.
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

			disetujuiOleh = check(disetujuiOleh);
			if (getTanggalPersetujuanManual() != null && disetujuiOleh != null) {
				tanggalPersetujuan = getTanggalPersetujuanManual();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/asset/SaldoAwalMasterAsset.java:getTanggalPersetujuan-lazy");
		}

		return tanggalPersetujuan;
	}

	/**
	 * Mengembalikan pemilik aset, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)}; MENIMPA tanpa syarat dengan warisan dari
	 * {@link #penerimaanPengadaanMasterAsset} (BAST asal) bila BAST punya nilai.
	 *
	 * @return {@link PemilikAsset} terkait, atau {@code null} bila tidak ada di kedua sumber.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pemilik_asset", nullable = true)
	public PemilikAsset getPemilikAsset() {
		pemilikAsset = check(pemilikAsset);
		if (penerimaanPengadaanMasterAsset != null && penerimaanPengadaanMasterAsset.getPemilikAsset() != null) {
			pemilikAsset = penerimaanPengadaanMasterAsset.getPemilikAsset();
		}

		return pemilikAsset;
	}

	/**
	 * Mengisi pemilik aset secara langsung. Nilai ini bisa ditimpa tanpa syarat oleh fallback
	 * pada {@link #getPemilikAsset()}.
	 *
	 * @param pemilikAsset pemilik aset, boleh {@code null}.
	 */
	public void setPemilikAsset(PemilikAsset pemilikAsset) {
		this.pemilikAsset = pemilikAsset;
	}

	/**
	 * Mengembalikan lokasi aset, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)}; MENIMPA tanpa syarat dengan warisan dari
	 * {@link #penerimaanPengadaanMasterAsset} (BAST asal) bila BAST punya nilai.
	 *
	 * @return {@link Lokasi} terkait, atau {@code null} bila tidak ada di kedua sumber.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		if (penerimaanPengadaanMasterAsset != null && penerimaanPengadaanMasterAsset.getLokasi() != null) {
			lokasi = penerimaanPengadaanMasterAsset.getLokasi();
		}

		return lokasi;
	}

	/**
	 * Mengisi lokasi aset secara langsung. Nilai ini bisa ditimpa tanpa syarat oleh fallback
	 * pada {@link #getLokasi()}.
	 *
	 * @param lokasi lokasi terkait, boleh {@code null}.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengembalikan ruang aset, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)}; MENIMPA tanpa syarat dengan warisan dari
	 * {@link #penerimaanPengadaanMasterAsset} (BAST asal) bila BAST punya nilai.
	 *
	 * @return {@link Ruang} terkait, atau {@code null} bila tidak ada di kedua sumber.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);

		if (penerimaanPengadaanMasterAsset != null && penerimaanPengadaanMasterAsset.getRuang() != null) {
			ruang = penerimaanPengadaanMasterAsset.getRuang();
		}

		return this.ruang;
	}

	/**
	 * Mengisi ruang aset secara langsung. Nilai ini bisa ditimpa tanpa syarat oleh fallback pada
	 * {@link #getRuang()}.
	 *
	 * @param ruang ruang terkait, boleh {@code null}.
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Mengisi nomor urut tampilan.
	 *
	 * @param index nomor urut, boleh {@code null}.
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/** @return nomor urut tampilan (bukan primary key), boleh {@code null}. */
	public Long getIndex() {
		return index;
	}

	/** @return BAST/penerimaan pengadaan asal yang menjadi sumber warisan banyak field kelas ini, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerimaan_pengadaan_master_asset", nullable = true, unique = true)
	public PenerimaanPengadaanMasterAsset getPenerimaanPengadaanMasterAsset() {
		return penerimaanPengadaanMasterAsset;
	}

	/**
	 * Mengisi BAST/penerimaan pengadaan asal. Kolom dipetakan {@code unique = true} — satu baris
	 * BAST hanya boleh dipasangkan ke satu dokumen tagihan.
	 *
	 * @param penerimaanPengadaanMasterAsset BAST asal, boleh {@code null}.
	 */
	public void setPenerimaanPengadaanMasterAsset(PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset) {
		this.penerimaanPengadaanMasterAsset = penerimaanPengadaanMasterAsset;
	}

	/**
	 * Mengembalikan satuan kerja pemilik dokumen, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)}; MENIMPA tanpa syarat dengan warisan dari
	 * {@link #penerimaanPengadaanMasterAsset}{@code .getPemesananPengadaanMasterAsset()
	 * .getSatuanKerja()} bila rantai tautan tersebut lengkap.
	 *
	 * @return {@link SatuanKerja} terkait, atau {@code null} bila tidak ada di kedua sumber.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		if (penerimaanPengadaanMasterAsset != null
				&& penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset() != null) {
			satuanKerja = penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset().getSatuanKerja();
		}
		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja secara langsung. Nilai ini bisa ditimpa tanpa syarat oleh fallback
	 * pada {@link #getSatuanKerja()}.
	 *
	 * @param satuanKerja satuan kerja, boleh {@code null}.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Menghitung dan mengembalikan nilai/nominal dokumen ini, dibulatkan ke bilangan bulat
	 * terdekat.
	 *
	 * <p><b>Kasus khusus pembelian langsung:</b> bila BAST asal ({@link #getPenerimaanPengadaanMasterAsset()})
	 * bertaut ke pesanan pengadaan ({@code getPemesananPengadaanMasterAsset()}) yang ditandai
	 * {@code getPembelianLangsung() == true}, nilai dokumen ini SELALU diambil dari nilai pesanan
	 * tersebut ({@code getPemesananPengadaanMasterAsset().getNilai()}) — menimpa field
	 * {@link #nilai} lokal tanpa syarat. Alasannya: pada pembelian langsung, harga sudah
	 * disepakati di tahap pemesanan (bukan negosiasi ulang di tahap tagihan), sehingga tagihan
	 * harus mencerminkan angka yang sama persis dengan pesanan, bukan angka yang mungkin diinput
	 * ulang secara manual pada dokumen ini.</p>
	 *
	 * <p><b>Fallback di luar pembelian langsung:</b> bila field {@link #nilai} masih {@code null}
	 * DAN BAST asal punya nilai {@code > 0.1} (ambang kecil untuk menghindari mengganggap nilai
	 * mendekati nol sebagai "ada"), nilai tersebut yang dipakai; bila masih kosong juga,
	 * default {@code 0.0}.</p>
	 *
	 * <p>Hasil akhir SELALU dibulatkan ke bilangan bulat terdekat via {@code Math.round} —
	 * dokumen tagihan aset di modul ini tidak menyimpan pecahan mata uang.</p>
	 *
	 * @return nilai/nominal dokumen (bilangan bulat, dibungkus {@code double}); tidak pernah
	 *         {@code null}.
	 */
	public Double getNilai() {

		if (getPenerimaanPengadaanMasterAsset() != null
				&& getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset() != null
				&& getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset().getPembelianLangsung() != null
				&& getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset().getPembelianLangsung()) {
			nilai = getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset().getNilai();
		}

		nilai = (nilai == null && getPenerimaanPengadaanMasterAsset() != null
				&& getPenerimaanPengadaanMasterAsset().getNilai() > 0.1)
						? getPenerimaanPengadaanMasterAsset().getNilai()
						: (nilai == null ? 0.0 : nilai);

		return (double) Math.round(nilai);
	}

	/**
	 * Mengisi nilai dokumen secara langsung. Nilai ini bisa ditimpa tanpa syarat oleh fallback
	 * pembelian-langsung pada {@link #getNilai()}.
	 *
	 * @param nilai nilai dokumen, boleh {@code null}.
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan tahun pembuatan dokumen, default tahun kalender berjalan
	 * ({@link ais.ui.util.WaktuUtil#getCalendar()}) bila belum pernah diset. Nilai default
	 * dituliskan balik ke field {@link #tahun} sehingga terkunci begitu getter ini dipanggil
	 * sekali.
	 *
	 * @return tahun pembuatan; tidak pernah {@code null}.
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Mengisi tahun pembuatan secara langsung.
	 *
	 * @param tahun tahun pembuatan, boleh {@code null} (akan diisi ulang otomatis oleh
	 *              {@link #getTahun()} dengan tahun berjalan bila dipanggil lagi).
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan jenis alur pengadaan dokumen ini, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)} bila sudah diset; default
	 * {@link NomorSuratAlurPengadaan#PENERIMAAN_TAGIHAN_DATA} bila belum pernah diisi — dokumen
	 * kelas ini secara default tergolong alur "Penerimaan Tagihan".
	 *
	 * @return {@link NomorSuratAlurPengadaan} terkait; tidak pernah {@code null} setelah
	 *         {@link NomorSuratAlurPengadaan#reloadDefault()} pernah dijalankan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_pengadaan", nullable = true)
	public NomorSuratAlurPengadaan getNomorSuratAlurPengadaan() {
		if (nomorSuratAlurPengadaan == null) {
			nomorSuratAlurPengadaan = NomorSuratAlurPengadaan.PENERIMAAN_TAGIHAN_DATA;
		} else {
			nomorSuratAlurPengadaan = check(nomorSuratAlurPengadaan);
		}
		return nomorSuratAlurPengadaan;
	}

	/**
	 * Mengisi jenis alur pengadaan.
	 *
	 * @param nomorSuratAlurPengadaan jenis alur, boleh {@code null} (akan default ke
	 *                                {@link NomorSuratAlurPengadaan#PENERIMAAN_TAGIHAN_DATA} oleh
	 *                                {@link #getNomorSuratAlurPengadaan()}).
	 */
	public void setNomorSuratAlurPengadaan(NomorSuratAlurPengadaan nomorSuratAlurPengadaan) {
		this.nomorSuratAlurPengadaan = nomorSuratAlurPengadaan;
	}

	/**
	 * Mengembalikan bulan pembuatan dokumen (1-12), default bulan kalender berjalan bila belum
	 * pernah diset. Sama seperti {@link #getTahun()}, nilai default dituliskan balik ke field.
	 *
	 * @return bulan pembuatan (1-12); tidak pernah {@code null}.
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Mengisi bulan pembuatan secara langsung.
	 *
	 * @param bulan bulan pembuatan (1-12), boleh {@code null}.
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Menentukan jumlah yang sudah dibayar untuk dokumen ini. Bernilai penuh
	 * {@link #getNilai()} HANYA bila {@link #getDaftarPengajuanTransfer()} memiliki proses
	 * transfer yang SUDAH direalisasikan ({@code getProsesTransfer().getRealisasikanOleh()}
	 * terisi) — model ini "semua atau tidak sama sekali" (bukan pembayaran bertahap/sebagian):
	 * begitu satu pengajuan transfer terkait direalisasikan, seluruh nilai dokumen dianggap
	 * lunas dibayar dalam satu kali. Di luar kondisi itu, dibayar selalu {@code 0.0}.
	 *
	 * @return jumlah yang sudah dibayar; tidak pernah {@code null}.
	 */
	public Double getDibayar() {

		if (getDaftarPengajuanTransfer() != null && getDaftarPengajuanTransfer().getProsesTransfer() != null
				&& getDaftarPengajuanTransfer().getProsesTransfer().getRealisasikanOleh() != null) {
			dibayar = getNilai();
		} else {
			dibayar = 0.0;
		}

		return dibayar == null ? 0.0 : dibayar;
	}

	/**
	 * Mengisi jumlah dibayar secara langsung. Nilai ini akan ditimpa ulang setiap kali
	 * {@link #getDibayar()} dipanggil.
	 *
	 * @param dibayar jumlah dibayar, boleh {@code null}.
	 */
	public void setDibayar(Double dibayar) {
		this.dibayar = dibayar;
	}

	/**
	 * Menentukan status lunas dokumen dengan membandingkan {@link #getNilai()} dan
	 * {@link #getDibayar()} (dibandingkan sebagai {@code int}, membuang bagian desimal — konsisten
	 * dengan kedua nilai yang sudah dibulatkan). Lunas bila jumlah dibayar SUDAH MENCAPAI ATAU
	 * MELEBIHI nilai dokumen ({@code <=}, bukan {@code ==} murni) — mengantisipasi kasus
	 * pembulatan yang membuat dibayar sedikit lebih besar dari nilai akibat pembulatan terpisah
	 * pada kedua getter.
	 *
	 * @return {@code true} bila dokumen sudah lunas; tidak pernah {@code null}.
	 */
	public Boolean getLunas() {
		lunas = getNilai().intValue() <= getDibayar().intValue();
		return lunas;
	}

	/**
	 * Mengisi status lunas secara langsung. Nilai ini akan ditimpa ulang setiap kali
	 * {@link #getLunas()} dipanggil (method itu SELALU menghitung ulang, tidak pernah membaca
	 * field ini sebagai fallback) — setter ini pada praktiknya tidak berpengaruh pada hasil
	 * {@link #getLunas()}.
	 *
	 * @param lunas status lunas, boleh {@code null}.
	 */
	public void setLunas(Boolean lunas) {
		this.lunas = lunas;
	}

	/**
	 * Mengembalikan vendor/penyedia, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)}. Berbeda dari kebanyakan getter lain di kelas ini,
	 * warisan dari {@link #penerimaanPengadaanMasterAsset} HANYA dipakai bila field lokal
	 * {@link #penyedia} masih {@code null} (bukan menimpa tanpa syarat) — penyedia yang sudah
	 * diisi manual pada dokumen ini tetap dipertahankan.
	 *
	 * @return {@link PenyediaAsset} terkait, atau {@code null} bila tidak ada di kedua sumber.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penyedia", nullable = true)
	public PenyediaAsset getPenyedia() {
		penyedia = check(penyedia);
		if (penyedia == null && penerimaanPengadaanMasterAsset != null
				&& penerimaanPengadaanMasterAsset.getPenyedia() != null) {
			penyedia = penerimaanPengadaanMasterAsset.getPenyedia();
		}
		return penyedia;
	}

	/**
	 * Mengisi vendor/penyedia secara langsung. Nilai ini dipertahankan oleh {@link #getPenyedia()}
	 * bila sudah terisi (fallback hanya dipakai saat field ini kosong).
	 *
	 * @param penyedia penyedia terkait, boleh {@code null}.
	 */
	public void setPenyedia(PenyediaAsset penyedia) {
		this.penyedia = penyedia;
	}

	/**
	 * Mengembalikan alur disposisi/persetujuan SOP dokumen ini, meresolusi proxy lazy Hibernate
	 * lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return {@link DisposisiSop} terkait, atau {@code null} bila belum ada alur disposisi
	 *         (dokumen belum diajukan lewat SOP).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Mengisi alur disposisi SOP, dengan guard "hanya-isi-sekali, tolak nilai tak-valid":
	 * pemanggilan diabaikan sepenuhnya bila {@code disposisiSop} yang diberikan {@code null} atau
	 * belum tersimpan ({@code getId() == null} — mis. objek transient yang belum di-{@code save}).
	 * Bila field {@link #disposisiSop} SUDAH terisi sebelumnya, nilai baru yang valid sekalipun
	 * TETAP DIABAIKAN — ekspresi ternary pada baris terakhir method ini pada praktiknya SELALU
	 * mengevaluasi ke {@code this.disposisiSop} lama setiap kali kondisi
	 * {@code this.disposisiSop != null} terpenuhi, karena syarat kedua pada ternary tersebut
	 * ({@code disposisiSop == null || disposisiSop.getId() == null}) sudah pasti {@code false} di
	 * titik itu (guard di awal method sudah menyaring kasus itu lebih dulu dengan {@code return}).
	 * Efek praktisnya: setter ini berperilaku sebagai "isi sekali saja" — begitu alur disposisi
	 * pertama terpasang pada dokumen, ia tidak bisa diganti lewat setter ini lagi.
	 *
	 * @param disposisiSop alur disposisi baru; diabaikan bila {@code null}, belum tersimpan, atau
	 *                      bila dokumen ini sudah memiliki alur disposisi sebelumnya.
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Menentukan status aktif dokumen berdasarkan status {@link #getDisposisiSop()}. Bernilai
	 * {@code true} secara default (belum pernah diset) sehingga dokumen tanpa alur disposisi
	 * dianggap aktif. Dinonaktifkan ({@code false}) pada dua kondisi: (1) alur disposisi ada dan
	 * statusnya sendiri sudah tidak aktif ({@code !disposisiSop.getAktif()}); atau (2) alur
	 * disposisi memiliki tahap akhir ({@code getDisposisiEnd()}) yang alur SOP-nya menandai
	 * "penolakan ada di sini" ({@code getPenolakanAdaDiSini()}) — dokumen yang alur SOP-nya
	 * berakhir di titik PENOLAKAN otomatis dianggap tidak aktif lagi, tanpa perlu field terpisah
	 * yang menandai "ditolak".
	 *
	 * @return {@code true} bila dokumen masih berlaku/aktif; tidak pernah {@code null}.
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

	/**
	 * Mengisi status aktif secara langsung. Nilai ini hanya bisa DITURUNKAN (ke {@code false}) oleh
	 * {@link #getAktif()}, tidak pernah dinaikkan kembali ke {@code true} secara otomatis — sekali
	 * dokumen dinonaktifkan oleh status disposisi, field ini tetap {@code false} sampai diisi
	 * ulang manual lewat setter ini.
	 *
	 * @param aktif status aktif, boleh {@code null} (diperlakukan sebagai {@code true} oleh
	 *              {@link #getAktif()} bila alur disposisi juga tidak menonaktifkannya).
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan kode termin pembayaran, diwarisi dari {@link #getPenerimaanPengadaanMasterAsset()}
	 * (BAST asal) bila BAST punya kode termin non-kosong. SEBALIKNYA, bila BAST bertaut ke
	 * pesanan pengadaan yang ditandai BUKAN {@code getByTermin()} (bukan pembayaran per termin),
	 * kode termin DIPAKSA {@code null} — menghapus nilai manapun yang mungkin sudah tersimpan,
	 * karena dokumen non-termin seharusnya tidak memiliki kode termin sama sekali.
	 *
	 * @return kode termin, sudah di-{@code trim}; tidak pernah {@code null} (default string
	 *         kosong).
	 */
	public String getKodeTermin() {
		if (getPenerimaanPengadaanMasterAsset() != null && getPenerimaanPengadaanMasterAsset().getKodeTermin() != null
				&& !getPenerimaanPengadaanMasterAsset().getKodeTermin().trim().isEmpty()) {
			kodeTermin = getPenerimaanPengadaanMasterAsset().getKodeTermin();
		}

		if (getPenerimaanPengadaanMasterAsset() != null
				&& getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset() != null
				&& !getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset().getByTermin()) {
			kodeTermin = null;
		}

		return kodeTermin == null ? "" : kodeTermin.trim();
	}

	/**
	 * Mengisi kode termin secara langsung. Nilai ini bisa ditimpa/dikosongkan oleh fallback pada
	 * {@link #getKodeTermin()}.
	 *
	 * @param kodeTermin kode termin, boleh {@code null}.
	 */
	public void setKodeTermin(String kodeTermin) {
		this.kodeTermin = kodeTermin;
	}

	/**
	 * Mengembalikan keterangan termin pembayaran, dengan logika fallback identik
	 * {@link #getKodeTermin()} (diwarisi dari BAST bila terisi; dipaksa {@code null} bila BAST
	 * bertaut ke pesanan non-termin). Berbeda dari {@link #getKodeTermin()}, navigasi di sini
	 * dibungkus {@code try/catch (Exception ...)} (dicatat ke {@link ais.common.ErrorAuditUtil})
	 * untuk mengantisipasi {@code LazyInitializationException} pada
	 * {@link #penerimaanPengadaanMasterAsset} yang bisa berupa proxy dari sesi Hibernate yang
	 * sudah tertutup.
	 *
	 * @return keterangan termin, boleh {@code null} (TIDAK di-default ke string kosong, berbeda
	 *         dari {@link #getKodeTermin()}).
	 */
	@Column(columnDefinition = "text")
	public String getKeteranganTermin() {
		try {
			// FIX LazyInitializationException: penerimaanPengadaanMasterAsset bisa berupa instance
			// canonical/shared (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain
			// yang sudah closed -> jangan biarkan getter ini crash, cukup lewati bagian ini
			// (nilai fallback dipertahankan).
			if (getPenerimaanPengadaanMasterAsset() != null
					&& getPenerimaanPengadaanMasterAsset().getKeteranganTermin() != null
					&& !getPenerimaanPengadaanMasterAsset().getKeteranganTermin().trim().isEmpty()) {
				keteranganTermin = getPenerimaanPengadaanMasterAsset().getKeteranganTermin();
			}

			if (getPenerimaanPengadaanMasterAsset() != null
					&& getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset() != null
					&& !getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset().getByTermin()) {
				keteranganTermin = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/asset/SaldoAwalMasterAsset.java:getKeteranganTermin-lazy");
		}

		return keteranganTermin;
	}

	/**
	 * Mengisi keterangan termin secara langsung. Nilai ini bisa ditimpa/dikosongkan oleh fallback
	 * pada {@link #getKeteranganTermin()}.
	 *
	 * @param keteranganTermin keterangan termin, boleh {@code null}.
	 */
	public void setKeteranganTermin(String keteranganTermin) {
		this.keteranganTermin = keteranganTermin;
	}

	/**
	 * Mengembalikan JSON detail termin (mis. field {@code penagihan} yang dikonsumsi
	 * {@link SaldoAwalMasterAssetDetail#getHarga()} untuk mode Termin), dengan logika fallback dan
	 * penanganan {@code LazyInitializationException} yang identik dengan
	 * {@link #getKeteranganTermin()}. Kolom dipetakan ke {@code json_object} (bukan nama field
	 * {@code jsonTermin} apa adanya).
	 *
	 * <p>Kehadiran/ketiadaan nilai non-{@code null} dari getter ini adalah PENANDA UTAMA yang
	 * dipakai berulang kali di kelas ini maupun {@link SaldoAwalMasterAssetDetail} untuk
	 * menentukan apakah sebuah dokumen tergolong mode "Termin" (lihat javadoc kelas &amp;
	 * {@link SaldoAwalMasterAssetDetail#getJumlah()}/{@code getHarga()}).</p>
	 *
	 * @return JSON detail termin sebagai teks mentah, atau {@code null} bila kosong/blank.
	 */
	@Column(columnDefinition = "text", name = "json_object")
	public String getJsonTermin() {
		try {
			// FIX LazyInitializationException: penerimaanPengadaanMasterAsset bisa berupa instance
			// canonical/shared (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain
			// yang sudah closed -> jangan biarkan getter ini crash, cukup lewati bagian ini
			// (nilai fallback dipertahankan).
			if (getPenerimaanPengadaanMasterAsset() != null && getPenerimaanPengadaanMasterAsset().getJsonTermin() != null
					&& !getPenerimaanPengadaanMasterAsset().getJsonTermin().trim().isEmpty()) {
				jsonTermin = getPenerimaanPengadaanMasterAsset().getJsonTermin();
			}

			if (getPenerimaanPengadaanMasterAsset() != null
					&& getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset() != null
					&& !getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset().getByTermin()) {
				jsonTermin = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/asset/SaldoAwalMasterAsset.java:getJsonTermin-lazy");
		}

		return jsonTermin == null || jsonTermin.trim().isEmpty() ? null : jsonTermin;
	}

	/**
	 * Mengisi JSON detail termin secara langsung. Nilai ini bisa ditimpa/dikosongkan oleh
	 * fallback pada {@link #getJsonTermin()}.
	 *
	 * @param jsonTermin JSON detail termin sebagai teks mentah, boleh {@code null}.
	 */
	public void setJsonTermin(String jsonTermin) {
		this.jsonTermin = jsonTermin;
	}

	/** @return riwayat posting jurnal akuntansi dokumen ini, boleh {@code null} bila belum diposting. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Mengisi riwayat posting jurnal.
	 *
	 * @param postingHistory riwayat posting, boleh {@code null}.
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * @return pengajuan transfer dana untuk pembayaran dokumen ini (menentukan
	 *         {@link #getDibayar()}), boleh {@code null} bila belum diajukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
	public DaftarPengajuanTransfer getDaftarPengajuanTransfer() {
		return daftarPengajuanTransfer;
	}

	/**
	 * Mengisi pengajuan transfer dana.
	 *
	 * @param daftarPengajuanTransfer pengajuan transfer terkait, boleh {@code null}.
	 */
	public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
		this.daftarPengajuanTransfer = daftarPengajuanTransfer;
	}

	/**
	 * @return tanggal persetujuan manual (di luar alur disposisi normal), dipakai sebagai
	 *         override oleh {@link #getTanggalPersetujuan()} bila {@link #getDisetujuiOleh()}
	 *         tidak {@code null}; boleh {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPersetujuanManual() {
		return tanggalPersetujuanManual;
	}

	/**
	 * Mengisi tanggal persetujuan manual.
	 *
	 * @param tanggalPersetujuanManual tanggal persetujuan manual, boleh {@code null}.
	 */
	public void setTanggalPersetujuanManual(Date tanggalPersetujuanManual) {
		this.tanggalPersetujuanManual = tanggalPersetujuanManual;
	}

	/**
	 * Mengembalikan kode tagihan, dengan dua lapis penentuan (yang kedua menang bila kondisinya
	 * terpenuhi): pertama, bila {@link #getId()} sudah ada (baris tersimpan) dan field
	 * {@link #kodeTagihan} masih kosong, dibuat default {@code "INV-" + id}; KEMUDIAN, bila
	 * {@link #getPenerimaanPengadaanMasterAsset()} (BAST asal) tersedia, kode tagihannya MENIMPA
	 * hasil di atas tanpa syarat — dokumen tagihan yang bertaut BAST selalu memakai kode tagihan
	 * BAST, bukan kode default {@code INV-<id>}.
	 *
	 * @return kode tagihan; bisa {@code null} hanya untuk baris baru yang belum tersimpan
	 *         ({@link #getId()} masih {@code null}) dan tidak bertaut BAST.
	 */
	public String getKodeTagihan() {
		if (getId() != null && (kodeTagihan == null || kodeTagihan.isEmpty())) {
			kodeTagihan = "INV-" + getId();
		}

		if (getPenerimaanPengadaanMasterAsset() != null) {
			kodeTagihan = getPenerimaanPengadaanMasterAsset().getKodeTagihan();
		}

		return kodeTagihan;
	}

	/**
	 * Mengisi kode tagihan secara langsung. Nilai ini bisa ditimpa tanpa syarat oleh fallback BAST
	 * pada {@link #getKodeTagihan()}.
	 *
	 * @param kodeTagihan kode tagihan, boleh {@code null}.
	 */
	public void setKodeTagihan(String kodeTagihan) {
		this.kodeTagihan = kodeTagihan;
	}

	/**
	 * Mengembalikan tanggal tagihan, MENIMPA tanpa syarat dengan warisan dari
	 * {@link #getPenerimaanPengadaanMasterAsset()} (BAST asal) bila BAST tersedia — perilaku
	 * serupa {@link #getKodeTagihan()} tapi tanpa fallback lokal (field {@link #tanggalTagihan}
	 * hanya dipakai bila BAST tidak tersedia sama sekali).
	 *
	 * @return tanggal tagihan, boleh {@code null} bila BAST tidak tersedia dan field lokal juga
	 *         kosong.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihan() {
		if (getPenerimaanPengadaanMasterAsset() != null) {
			tanggalTagihan = getPenerimaanPengadaanMasterAsset().getTanggalTagihan();
		}
		return tanggalTagihan;
	}

	/**
	 * Mengisi tanggal tagihan secara langsung. Nilai ini bisa ditimpa tanpa syarat oleh fallback
	 * BAST pada {@link #getTanggalTagihan()}.
	 *
	 * @param tanggalTagihan tanggal tagihan, boleh {@code null}.
	 */
	public void setTanggalTagihan(Date tanggalTagihan) {
		this.tanggalTagihan = tanggalTagihan;
	}

	/** @return persentase PPN untuk mode Breakdown, boleh {@code null} bila belum diisi. */
	@Column(name = "breakdown_ppn_persen", nullable = true)
	public Double getBreakdownPpnPersen() { return breakdownPpnPersen; }
	/**
	 * Mengisi persentase PPN mode Breakdown.
	 *
	 * @param v persentase PPN, boleh {@code null}.
	 */
	public void setBreakdownPpnPersen(Double v) { this.breakdownPpnPersen = v; }

	/**
	 * @return nilai PPh dari Bukti Potong untuk mode Breakdown (sumber nilai PPh saat
	 *         {@link #getBreakdownAktif()} bernilai {@code true}), boleh {@code null}.
	 */
	@Column(name = "breakdown_bukti_potong", nullable = true)
	public Double getBreakdownBuktiPotong() { return breakdownBuktiPotong; }
	/**
	 * Mengisi nilai PPh dari Bukti Potong.
	 *
	 * @param v nilai Bukti Potong, boleh {@code null}.
	 */
	public void setBreakdownBuktiPotong(Double v) { this.breakdownBuktiPotong = v; }

	/** @return catatan khusus bebas untuk mode Breakdown, boleh {@code null}. */
	@Column(name = "breakdown_special_notes", nullable = true, columnDefinition = "text")
	public String getBreakdownSpecialNotes() { return breakdownSpecialNotes; }
	/**
	 * Mengisi catatan khusus mode Breakdown.
	 *
	 * @param v teks catatan, boleh {@code null}.
	 */
	public void setBreakdownSpecialNotes(String v) { this.breakdownSpecialNotes = v; }

	/**
	 * Menentukan mode perhitungan pajak dokumen ini. Berbeda dari kebanyakan flag Boolean lain di
	 * kelas ini yang default {@code true} saat {@code null}, flag ini SENGAJA default
	 * {@code false} (dinormalisasi via {@code != null && breakdownAktif}, bukan
	 * {@code == null ? true : ...}) — mode default adalah "Sesuai PO" (PPh dihitung per baris
	 * detail), BUKAN mode Breakdown, sehingga dokumen lama yang belum pernah menyentuh fitur ini
	 * tetap berperilaku seperti sebelum fitur Breakdown ada.
	 *
	 * @return {@code true} bila mode Breakdown (PPh dari Bukti Potong) aktif; tidak pernah
	 *         {@code null}.
	 */
	@Column(name = "breakdown_aktif", nullable = true)
	public Boolean getBreakdownAktif() { return breakdownAktif != null && breakdownAktif; }
	/**
	 * Mengisi status mode Breakdown.
	 *
	 * @param v {@code true}=mode Breakdown, {@code false}/{@code null}=mode Sesuai PO.
	 */
	public void setBreakdownAktif(Boolean v) { this.breakdownAktif = v; }

	/**
	 * Mengembalikan jenis PPh untuk baris jurnal mode Breakdown, meresolusi proxy lazy Hibernate
	 * lewat {@link GeneralValueObject#check(Object)}. Catatan penting: field ini HANYA menentukan
	 * KLASIFIKASI jenis pajak pada jurnal posting — NILAI nominal PPh tetap selalu berasal dari
	 * {@link #getBreakdownBuktiPotong()}, TIDAK dihitung ulang dari persentase jenis pajak ini.
	 *
	 * @return {@link JenisPajakBarang} untuk baris jurnal mode Breakdown, boleh {@code null} bila
	 *         belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "breakdown_jenis_pph", nullable = true)
	public JenisPajakBarang getBreakdownJenisPph() { breakdownJenisPph = check(breakdownJenisPph); return breakdownJenisPph; }
	/**
	 * Mengisi jenis PPh mode Breakdown.
	 *
	 * @param v jenis pajak barang, boleh {@code null}.
	 */
	public void setBreakdownJenisPph(JenisPajakBarang v) { this.breakdownJenisPph = v; }

	/**
	 * Mengembalikan id sementara in-memory (bukan primary key), dibuat sekali secara acak via
	 * {@link Common#randLong()} bila belum pernah diminta, lalu dipertahankan (idempoten) untuk
	 * pemanggilan berikutnya. Dipakai untuk kebutuhan identifikasi sisi klien (mis. key baris pada
	 * komponen UI sebelum baris tersimpan ke database dan punya {@link #getId()} sesungguhnya).
	 *
	 * @return id sementara; tidak pernah {@code null}.
	 */
	public Long getIdTemp() {
		if (idTemp == null) {
			idTemp = Common.randLong();
		}
		return idTemp;
	}

	/**
	 * Mengisi id sementara secara langsung.
	 *
	 * @param idTemp id sementara, boleh {@code null} (akan diisi ulang otomatis secara acak oleh
	 *               {@link #getIdTemp()} bila dipanggil lagi).
	 */
	public void setIdTemp(Long idTemp) {
		this.idTemp = idTemp;
	}
}
