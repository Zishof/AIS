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

import org.hibernate.envers.Audited;

import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entity dokumen <b>Penghapusan Aset</b> / write-off (tabel {@code asset.penghapusan_master_asset})
 * — berita acara penghapusan alat/fasilitas dari inventaris (rusak, hilang, dijual, dihibahkan,
 * dsb. — alasannya dikategorikan lewat {@link #getJenisPengapusanBarang()}). Diproses lewat layar
 * {@code ais.action.master.asset.PenghapusanMasterAssetAction}, dengan rincian barang yang dihapus
 * disimpan sebagai baris {@link PenghapusanMasterAssetDetail}.
 *
 * <h2>Soft-delete, bukan hard-delete: baris {@code AssetDetail}/{@code MasterAsset} tidak pernah dihapus</h2>
 * <p>Penghapusan aset di modul ini murni menambahkan <b>dokumen baru</b> yang MEREFERENSIKAN baris
 * {@code AssetDetail} lewat {@link PenghapusanMasterAssetDetail#getAssetDetail()} — tidak ada kode
 * di jalur simpan ({@code PenghapusanMasterAssetAction#onSave}) maupun di helper grid
 * ({@code PenghapusanMasterAssetHelper}) yang menghapus baris {@code AssetDetail} atau
 * {@code MasterAsset} itu sendiri dari database. "Penghapusan" di sini adalah istilah domain
 * (write-off akuntansi/administratif), bukan operasi {@code DELETE} pada tabel aset induk. Yang
 * benar-benar bisa di-{@code DELETE} lewat UI hanyalah baris {@link PenghapusanMasterAssetDetail}
 * (dan dokumen {@code PenghapusanMasterAsset} itu sendiri) — keduanya dibatasi hanya bisa dihapus
 * selama dokumen belum disetujui, lihat bagian berikut.</p>
 *
 * <h2>Penjaga hapus: terikat status persetujuan, bukan langsung ke {@code postingHistory}</h2>
 * <p>Tombol hapus dokumen maupun baris detail pada {@code PenghapusanMasterAssetAction} dan
 * {@code PenghapusanMasterAssetHelper} hanya tampil ({@code setVisible}) ketika
 * {@code getDisetujuiOleh() == null} (dokumen belum disetujui). Karena
 * {@code PenghapusanMasterAssetAction.postingSemua(...)} (mesin posting jurnal write-off, dok 61
 * butir D) hanya memproses dokumen yang <b>sudah disetujui</b> (lewat kriteria dasbor) dan mengisi
 * {@link #getPostingHistory()} setelah berhasil dijurnal, secara transitif dokumen yang sudah
 * ter-{@code postingHistory} juga sudah "disetujui" — sehingga penjaga berbasis
 * {@code disetujuiOleh == null} ini SECARA TIDAK LANGSUNG juga mencegah penghapusan dokumen yang
 * sudah diposting, mirip pola yang seharusnya ada namun berbeda mekanisme dari pola
 * "penjaga postingHistory langsung" yang dipakai gerbang posting gaji/jurnal penyesuaian (lihat
 * catatan proyek). <b>Catatan risiko yang ditemukan saat dokumentasi ini:</b> tombol individual
 * "Batalkan" (membatalkan persetujuan — {@code disetujuiOleh}/{@code tanggalPersetujuan} di-null-kan)
 * pada {@code PenghapusanMasterAssetAction} TIDAK memeriksa {@link #getPostingHistory()} sama sekali
 * sebelum membatalkan persetujuan — berbeda dari pasangan dasbornya
 * {@code batalkanPostingSemua(...)} yang membatalkan persetujuan SEKALIGUS menghapus baris jurnal
 * turunan ({@code akunting.transaksi}/{@code grup_transaksi}) dan mengosongkan
 * {@code postingHistory} secara atomik. Akibatnya, dokumen yang SUDAH diposting (punya
 * {@code postingHistory} terisi dan jurnal live di buku besar) bisa dibatalkan persetujuannya lewat
 * tombol individual ini tanpa membalik jurnalnya — membuat tombol "Ubah" dan "Hapus" kembali
 * tampil (karena keduanya juga hanya bergantung pada {@code disetujuiOleh == null}) padahal jurnal
 * lama masih tercatat di pembukuan berdasarkan nilai/jenis penghapusan yang lama. {@code onSave()}
 * pada layar yang sama juga tidak memeriksa {@code postingHistory} sebelum mengizinkan
 * {@code nilai}/{@code jenisPengapusanBarang}/baris detail diubah, sehingga dokumen dan jurnal bisa
 * diam-diam berbeda (drift) tanpa peringatan maupun re-posting otomatis. Constraint FK basis data
 * pada {@code grup_transaksi.penghapusan_master_asset} kemungkinan tetap mencegah hard-delete
 * dokumen selama baris jurnal masih ada (ditangkap sebagai exception ramah di UI), tapi EDIT
 * setelah posting tidak terhalang mekanisme apa pun.</p>
 *
 * <h2>Field {@link #getPostingHistory()}: gerbang idempoten mesin posting</h2>
 * <p>Diisi oleh {@code PenghapusanMasterAssetAction#postingSemua(...)} setelah jurnal Dr/Cr
 * (memakai pasangan akun {@link JenisPengapusanBarang#getDebet()}/{@link JenisPengapusanBarang#getKredit()})
 * berhasil disimpan; kriteria posting hanya mengambil dokumen dengan {@code postingHistory IS NULL},
 * sehingga field ini juga berfungsi sebagai penanda "sudah diposting, jangan diposting ulang" —
 * dikosongkan kembali hanya oleh {@code batalkanPostingSemua(...)} setelah jurnal turunannya
 * dihapus.</p>
 *
 * @see PenghapusanMasterAssetDetail
 * @see JenisPengapusanBarang
 * @see ais.action.master.asset.PenghapusanMasterAssetAction
 * @see ais.action.master.asset.helper.PenghapusanMasterAssetHelper
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "penghapusan_master_asset")
public class PenghapusanMasterAsset extends DataSop {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.penghapusan_master_asset}. */
	private Long id;
	/** Nomor urut tampilan (mis. urutan baris pada grid/laporan); tidak dipakai untuk logika bisnis. */
	private Long index;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat catatan kelas. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat catatan kelas. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna audit, atau {@code null} bila belum pernah diubah sejak dimuat
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna audit. Guard di awal method membuat setter ini diam-diam mengabaikan
	 * nilai {@code null}/blank — tidak menghapus nilai lama, berbeda dari setter lain di kelas
	 * ini yang selalu menimpa.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi ringkas untuk log/debug dan tampilan combobox generik.
	 *
	 * @return {@link #kode} dokumen penghapusan, boleh {@code null} bila belum diisi
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Mengisi nama pengguna audit. Guard yang sama seperti {@link #setOlehId(String)} membuat
	 * nilai {@code null}/blank diabaikan, bukan menghapus nilai lama.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna audit, atau {@code null} bila belum pernah diubah sejak dimuat
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus hidup JPA yang dipanggil Hibernate tepat sebelum setiap {@code UPDATE}.
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #tanggal_dirubah}, {@link #oleh}, dan {@link #olehId} dengan waktu
	 * serta identitas pengguna aktif. Dipicu otomatis oleh Hibernate lewat
	 * {@link javax.persistence.PreUpdate}, tidak dipanggil manual di tempat lain.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Field diinisialisasi ke waktu saat ini pada konstruksi
	 * objek, lalu ditimpa ulang oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * @return timestamp perubahan terakhir; tidak pernah {@code null} karena field diinisialisasi
	 *         saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode dokumen penghapusan, unik, biasanya di-generate otomatis saat simpan pertama. */
	private String kode;
	/** Keterangan/alasan penghapusan, bebas teks. */
	private String keterangan;
	/** Tanggal dokumen penghapusan dibuat; lihat {@link #getTanggalPembuatan()} untuk fallback disposisi SOP. */
	private Date tanggalPembuatan;
	/** Tanggal dokumen penghapusan disetujui; lihat {@link #getTanggalPersetujuan()} untuk fallback disposisi SOP. */
	private Date tanggalPersetujuan;
	/** Pengguna pembuat dokumen; lihat {@link #getDibuatOleh()} untuk fallback ke pengaju disposisi SOP. */
	private Tbmuser dibuatOleh;
	/** Pengguna penyetuju dokumen; lihat {@link #getDisetujuiOleh()} untuk fallback ke penyetuju disposisi SOP. */
	private Tbmuser disetujuiOleh;
	/** Alur disposisi SOP yang menaungi persetujuan dokumen ini. */
	private DisposisiSop disposisiSop;
	/** Satuan kerja pemilik dokumen. */
	private SatuanKerja satuanKerja;
	/** Nilai nominal total dokumen (jumlah harga beli/nilai buku seluruh baris detail); {@code null} dibaca sebagai 0.0. Inilah nominal yang dijurnal oleh {@code postingSemua(...)} — lihat catatan kelas. */
	private Double nilai;
	/** Tahun periode dokumen; lihat {@link #getTahun()} — default tahun berjalan bila belum diisi. */
	private Integer tahun;
	/** Bulan periode dokumen (1-12); lihat {@link #getBulan()} — default bulan berjalan bila belum diisi. */
	private Integer bulan;
	/** Konfigurasi penomoran surat alur pengadaan; lihat {@link #getNomorSuratAlurPengadaan()} untuk default. */
	private NomorSuratAlurPengadaan nomorSuratAlurPengadaan;

	/**
	 * Kategori/alasan penghapusan yang menentukan pasangan akun debet/kredit write-off yang
	 * dijurnal saat posting — lihat {@link JenisPengapusanBarang} dan catatan kelas.
	 */
	private JenisPengapusanBarang jenisPengapusanBarang;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public PenghapusanMasterAsset() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return id, atau {@code null} untuk instance baru yang belum disimpan
	 */
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
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode dokumen, ditrim dan dinormalisasi menjadi {@code null} bila kosong
	 * setelah trim.
	 *
	 * @return kode ter-trim, atau {@code null} bila belum diisi/hanya berisi whitespace
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/**
	 * Mengisi kode dokumen. Tidak ada trim di sisi setter — trimming terjadi hanya saat dibaca
	 * lewat {@link #getKode()}.
	 *
	 * @param kode kode dokumen, harus unik pada tabel
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan/alasan penghapusan bebas teks.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan/alasan penghapusan.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi field mentah pembuat dokumen. Nilai ini bisa ditimpa oleh {@link #getDibuatOleh()}
	 * bila disposisi SOP terkait punya pengaju yang berbeda — lihat javadoc getter tersebut.
	 *
	 * @param dibuatOleh pengguna pembuat dokumen
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna pembuat dokumen. Meresolusi proxy lazy lewat {@link #check(Object)},
	 * lalu bila {@link #getDisposisiSop()} punya {@code disposisiStart} dengan pengaju yang
	 * terisi, nilai tersebut MENIMPA field mentah {@link #dibuatOleh} — sehingga identitas
	 * pembuat yang tampil selalu mengikuti pengaju disposisi SOP yang sesungguhnya.
	 *
	 * @return pengguna pembuat efektif (dari disposisi SOP bila ada, atau field tersimpan)
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
	 * Mengembalikan pengguna penyetuju dokumen, disinkronkan dengan status disposisi SOP
	 * terkait: bila {@link #getDisposisiSop()} punya {@code disposisiSetuju} dengan pengaju
	 * terisi, nilai tersebut dipakai sebagai penyetuju efektif; sebaliknya bila disposisi SOP ada
	 * tapi belum punya {@code disposisiSetuju} (atau belum ada pengajunya), field ini
	 * di-null-kan. <b>Ini adalah nilai yang dibaca oleh seluruh gerbang UI
	 * {@code disetujuiOleh == null}</b> pada {@code PenghapusanMasterAssetAction} untuk
	 * memutuskan visibilitas tombol Ubah/Hapus/Setujui/Batalkan (lihat catatan kelas soal celah
	 * pada tombol Batalkan individual yang tidak memeriksa {@link #getPostingHistory()}).
	 *
	 * @return pengguna penyetuju efektif, atau {@code null} bila belum disetujui atau disposisi
	 *         SOP belum mencapai tahap persetujuan
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

	/**
	 * Mengisi field mentah tanggal persetujuan. Nilai ini bisa ditimpa atau di-null-kan oleh
	 * {@link #getTanggalPersetujuan()} berdasarkan status disposisi SOP terkait.
	 *
	 * @param tanggalPersetujuan tanggal persetujuan
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan tanggal persetujuan dokumen, disinkronkan dengan waktu
	 * {@code disposisiSetuju} pada {@link #getDisposisiSop()} bila tersedia. Method ini juga
	 * dipakai sebagai tanggal jurnal ({@code tglJurnal}) oleh {@code postingSemua(...)} (dengan
	 * fallback ke {@link #getTanggalPembuatan()} bila {@code null}) — lihat catatan kelas.
	 * Seluruh badan method dibungkus try/catch kosong yang hanya mencatat exception ke
	 * {@link ais.common.ErrorAuditUtil} — FIX untuk {@code LazyInitializationException}: instance
	 * {@code disposisiSop} kadang berupa proxy canonical/shared milik
	 * {@code AuditTimestampInterceptor} yang terikat ke session Hibernate lain yang sudah closed.
	 * Getter sengaja tidak dibiarkan crash; bila exception terjadi, nilai fallback (nilai lama
	 * yang tersimpan) dipertahankan apa adanya.
	 *
	 * @return tanggal persetujuan efektif, atau {@code null} bila belum disetujui; bisa juga
	 *         berupa nilai fallback lama bila terjadi {@code LazyInitializationException} saat
	 *         mencoba membaca disposisi SOP
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
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/asset/PenghapusanMasterAsset.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	/**
	 * Mengisi field mentah pengguna penyetuju. Nilai ini bisa ditimpa atau di-null-kan oleh
	 * {@link #getDisetujuiOleh()} berdasarkan status disposisi SOP terkait.
	 *
	 * @param disetujuiOleh pengguna penyetuju
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengisi field mentah tanggal pembuatan. Nilai ini bisa ditimpa oleh
	 * {@link #getTanggalPembuatan()} berdasarkan {@code disposisiStart} disposisi SOP terkait.
	 *
	 * @param tanggalPembuatan tanggal pembuatan dokumen
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan tanggal pembuatan dokumen, disinkronkan dengan waktu {@code disposisiStart}
	 * pada {@link #getDisposisiSop()} bila tersedia (pola sama dengan {@link #getDibuatOleh()}).
	 * Dipakai sebagai fallback {@code tglJurnal} oleh {@code postingSemua(...)} ketika
	 * {@link #getTanggalPersetujuan()} kosong. Try/catch kosong yang membungkus akses disposisi
	 * SOP adalah FIX {@code LazyInitializationException} yang sama seperti pada
	 * {@link #getTanggalPersetujuan()}. Berbeda dari getter tanggal lain di kelas ini, method ini
	 * juga memiliki fallback akhir: bila hasil akhirnya tetap {@code null}, dikembalikan
	 * {@link WaktuUtil#getDate()} (waktu saat ini) alih-alih {@code null}.
	 *
	 * @return tanggal pembuatan efektif; tidak pernah {@code null}, jatuh ke waktu saat ini bila
	 *         tidak ada nilai tersimpan maupun dari disposisi SOP
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
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/asset/PenghapusanMasterAsset.java:getTanggalPembuatan-lazy");
		}

		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Mengisi nomor urut tampilan.
	 *
	 * @param index nomor urut, tidak dipakai untuk logika bisnis
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Mengembalikan nomor urut tampilan.
	 *
	 * @return nomor urut, boleh {@code null}
	 */
	public Long getIndex() {
		return index;
	}

	/** Cache field kode unik gabungan; lihat {@link #getKodeUnik()} — dihitung ulang tiap dibaca. */
	private String kodeUnik;
	/** Flag aktif mentah; lihat {@link #getAktif()} — bisa ditimpa {@code false} oleh status disposisi SOP. */
	private Boolean aktif;

	/**
	 * Membangun dan mengembalikan kode unik gabungan: {@link #getKode()} disambung id disposisi
	 * SOP (bila ada) atau id entity sendiri (bila belum punya disposisi SOP). Dihitung ulang
	 * setiap kali dipanggil, memenuhi constraint kolom {@code unique = true} tanpa perlu kode
	 * aplikasi mengelola keunikannya secara manual.
	 *
	 * @return string gabungan {@code "<kode>_<idDisposisiSopAtauId>"}
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Mengisi field mentah kode unik. Nilai ini akan ditimpa kembali oleh {@link #getKodeUnik()}
	 * setiap kali getter dipanggil.
	 *
	 * @param kodeUnik kode unik gabungan
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan alur disposisi SOP yang menaungi dokumen ini, meresolusi proxy lazy lewat
	 * {@link #check(Object)}.
	 *
	 * @return disposisi SOP terkait, atau {@code null} bila dokumen belum/tidak memakai alur SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Mengisi alur disposisi SOP. Guard di awal method membuat setter ini diam-diam mengabaikan
	 * argumen {@code null} atau yang id-nya {@code null} (belum tersimpan) — nilai lama tetap
	 * dipertahankan bila argumen baru tidak valid, kecuali field lama sendiri memang masih
	 * {@code null} (baru pertama diisi).
	 *
	 * @param disposisiSop disposisi SOP baru; diabaikan bila {@code null} atau belum punya id
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Mengembalikan satuan kerja pemilik dokumen, meresolusi proxy lazy lewat
	 * {@link #check(Object)}. Berbeda dari {@code PengembalianMasterAsset} sebelah, getter ini
	 * TIDAK menurunkan satuan kerja dari dokumen lain — murni membaca field tersimpan pada
	 * entity ini sendiri.
	 *
	 * @return satuan kerja pemilik dokumen, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja pemilik dokumen.
	 *
	 * @param satuanKerja satuan kerja
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan nilai nominal total dokumen, dengan {@code null} dinormalisasi menjadi
	 * {@code 0.0}. Inilah nominal yang dijurnal Dr/Cr oleh {@code postingSemua(...)} — dokumen
	 * dengan nilai {@code 0.0} (atau {@code null}) dilewati begitu saja oleh mesin posting
	 * (lihat catatan kelas).
	 *
	 * @return nilai nominal, tidak pernah {@code null}
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Mengisi nilai nominal total dokumen.
	 *
	 * @param nilai nilai nominal, boleh {@code null} (akan dibaca sebagai 0.0)
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan tahun periode dokumen. Bila belum pernah diisi, di-lazy-default ke tahun
	 * kalender saat ini pada saat pertama kali dibaca.
	 *
	 * @return tahun periode; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Mengisi tahun periode dokumen secara eksplisit, mem-bypass lazy-default
	 * {@link #getTahun()}.
	 *
	 * @param tahun tahun periode
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan bulan periode dokumen (1-12). Bila belum pernah diisi, di-lazy-default ke
	 * bulan kalender saat ini (perhatikan {@code +1} karena {@link Calendar#MONTH} berbasis 0
	 * sedangkan field ini berbasis 1) pada saat pertama kali dibaca.
	 *
	 * @return bulan periode (1-12); tidak pernah {@code null} setelah pemanggilan pertama
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Mengisi bulan periode dokumen secara eksplisit, mem-bypass lazy-default
	 * {@link #getBulan()}.
	 *
	 * @param bulan bulan periode (1-12)
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan konfigurasi penomoran surat alur pengadaan yang dipakai dokumen ini. Bila
	 * belum pernah diisi, di-default ke konstanta statis
	 * {@link NomorSuratAlurPengadaan#PENGHAPUSAN_BARANG_DATA} (bukan hasil query database); jika
	 * sudah ada nilai tersimpan, diresolusi lewat {@link #check(Object)}.
	 *
	 * @return konfigurasi nomor surat efektif, tidak pernah {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_pengadaan", nullable = true)
	public NomorSuratAlurPengadaan getNomorSuratAlurPengadaan() {
		if (nomorSuratAlurPengadaan == null) {
			nomorSuratAlurPengadaan = NomorSuratAlurPengadaan.PENGHAPUSAN_BARANG_DATA;
		} else {
			nomorSuratAlurPengadaan = check(nomorSuratAlurPengadaan);
		}
		return nomorSuratAlurPengadaan;
	}

	/**
	 * Mengisi konfigurasi penomoran surat alur pengadaan secara eksplisit, mem-bypass default
	 * {@link #getNomorSuratAlurPengadaan()}.
	 *
	 * @param nomorSuratAlurPengadaan konfigurasi nomor surat
	 */
	public void setNomorSuratAlurPengadaan(NomorSuratAlurPengadaan nomorSuratAlurPengadaan) {
		this.nomorSuratAlurPengadaan = nomorSuratAlurPengadaan;
	}

	/**
	 * Mengembalikan kategori/alasan penghapusan ({@link JenisPengapusanBarang}), meresolusi
	 * proxy lazy lewat {@link #check(Object)}. Relasi wajib (kolom {@code nullable = false}) —
	 * dipakai langsung oleh {@code postingSemua(...)} untuk membaca pasangan akun
	 * {@link JenisPengapusanBarang#getDebet()}/{@link JenisPengapusanBarang#getKredit()} yang
	 * dijurnal senilai {@link #getNilai()} (lihat catatan kelas).
	 *
	 * @return jenis penghapusan barang; seharusnya tidak pernah {@code null} untuk dokumen yang
	 *         valid
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pengapusan_barang", nullable = false)
	public JenisPengapusanBarang getJenisPengapusanBarang() {
		jenisPengapusanBarang = check(jenisPengapusanBarang);
		return jenisPengapusanBarang;
	}

	/**
	 * Mengisi kategori/alasan penghapusan.
	 *
	 * @param jenisPengapusanBarang jenis penghapusan barang yang dipilih pengguna di form
	 */
	public void setJenisPengapusanBarang(JenisPengapusanBarang jenisPengapusanBarang) {
		this.jenisPengapusanBarang = jenisPengapusanBarang;
	}

	/**
	 * Mengembalikan status aktif dokumen, disinkronkan dengan status disposisi SOP terkait:
	 * bernilai {@code false} bila disposisi SOP sudah tidak aktif ({@code !getAktif()}), atau
	 * bila alur disposisi SOP berakhir pada titik penolakan
	 * ({@code disposisiEnd.alurSop.penolakanAdaDiSini}). Selain dua kondisi tersebut,
	 * {@code null} dibaca sebagai aktif.
	 *
	 * @return {@code true} bila aktif; {@code false} bila disposisi SOP tidak aktif atau berakhir
	 *         di titik penolakan
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
	 * Mengisi field mentah status aktif. Nilai ini bisa ditimpa {@code false} oleh
	 * {@link #getAktif()} berdasarkan status disposisi SOP terkait.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Field mentah riwayat posting; lihat {@link #getPostingHistory()} untuk semantik lengkap
	 * (penanda idempoten posting sekaligus indikator transitif dokumen sudah disetujui/dijurnal).
	 */
	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Riwayat posting jurnal penghapusan (dok 61 butir D): terisi begitu mesin posting
	 * "Penghapusan Aset" menjurnalkan dokumen ini memakai pasangan akun debet/kredit dari
	 * {@link JenisPengapusanBarang} -- pasangan yang sudah lama ada di masternya tetapi
	 * tidak pernah dipakai satu jalur pun.
	 *
	 * <p>Selain sebagai riwayat, field ini adalah <b>gerbang idempoten</b>:
	 * {@code postingSemua(...)} hanya mengambil dokumen dengan {@code postingHistory IS NULL},
	 * sehingga dokumen yang sudah pernah dijurnal tidak akan dijurnal ulang oleh pemanggilan
	 * berikutnya. Dikosongkan kembali hanya oleh {@code batalkanPostingSemua(...)}, yang juga
	 * menghapus baris jurnal turunan pada saat bersamaan — lihat catatan kelas untuk celah pada
	 * tombol "Batalkan" individual yang TIDAK melalui jalur ini.</p>
	 *
	 * @return riwayat posting bila dokumen sudah dijurnal, atau {@code null} bila masih berstatus
	 *         draf/belum diposting
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		postingHistory = check(postingHistory);
		return postingHistory;
	}

	/**
	 * Mengisi riwayat posting. Dipanggil oleh {@code postingSemua(...)} setelah jurnal berhasil
	 * disimpan, dan oleh {@code batalkanPostingSemua(...)} dengan argumen {@code null} setelah
	 * jurnal turunan dihapus — lihat catatan kelas. Tidak ada validasi di setter ini; pemanggil
	 * bertanggung jawab menjaga konsistensi antara field ini dan baris jurnal yang sesungguhnya.
	 *
	 * @param postingHistory riwayat posting baru, atau {@code null} untuk menandai belum/tidak
	 *                       lagi diposting
	 */
	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
