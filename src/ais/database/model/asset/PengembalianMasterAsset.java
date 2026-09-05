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
 * Entity dokumen <b>Pengembalian Aset</b> (tabel {@code asset.pengembalian_master_asset}) —
 * berita acara pengembalian barang/fasilitas yang sebelumnya dipinjam lewat
 * {@link PeminjamanMasterAsset}, menutup siklus hidup satu transaksi peminjaman. Diproses lewat
 * dua layar {@code ais.action.master.asset.PengembalianMasterAssetAction} (input pengembalian oleh
 * peminjam/petugas) dan subkelasnya {@code ais.action.master.asset.PersetujuanPengembalianMasterAssetAction}
 * (persetujuan oleh pejabat berwenang, memakai konstruktor {@code super(true)}).
 *
 * <h2>Penjaga status: hanya peminjaman yang disetujui &amp; belum pernah dikembalikan yang bisa dipilih</h2>
 * <p>Field {@link #getPeminjamanMasterAsset()} adalah relasi wajib ({@code nullable = false}) ke
 * {@link PeminjamanMasterAsset} yang menjadi pasangannya. Penjaga integritas <b>bukan</b> berada di
 * entity ini, melainkan di query combobox pemilihan peminjaman pada
 * {@code PengembalianMasterAssetAction} (form input), yang membatasi kandidat hanya pada
 * {@code PeminjamanMasterAsset} dengan {@code disetujuiOleh IS NOT NULL} (peminjaman sudah disetujui)
 * <b>DAN</b> {@code pengembalianMasterAsset IS NULL} (belum pernah dipasangkan dengan pengembalian
 * lain) — sehingga aset yang tidak pernah tercatat dipinjam/disetujui tidak bisa "dikembalikan", dan
 * satu peminjaman tidak bisa dikembalikan dua kali lewat form ini. Begitu dokumen pengembalian
 * tersimpan, sisi invers {@code PeminjamanMasterAsset.setPengembalianMasterAsset(this)} diisi agar
 * peminjaman tersebut otomatis tersingkir dari daftar kandidat pengembalian berikutnya.</p>
 *
 * <h2>Relasi ke penyusutan/write-off: tidak langsung</h2>
 * <p>Berbeda dengan {@link PenghapusanMasterAsset} (yang punya jalur posting akuntansi lewat
 * {@code PenghapusanMasterAssetAction#postingSemua}), pengembalian aset dari peminjaman <b>tidak</b>
 * memicu jurnal akunting — ini murni pencatatan administratif penutupan siklus peminjaman (barang
 * kembali ke inventaris pengelola), bukan pelepasan aset dari pembukuan.</p>
 *
 * <h2>Pola berulang di keluarga dokumen SOP asset</h2>
 * <p>Kelas ini mewarisi {@link DataSop} dan memakai pola yang identik dengan
 * {@link PenghapusanMasterAsset} pada file sebelah: getter {@link #getDibuatOleh()},
 * {@link #getDisetujuiOleh()}, {@link #getTanggalPembuatan()}, dan {@link #getTanggalPersetujuan()}
 * semuanya membaca ulang dari {@link DisposisiSop} terkait (bukan murni field tersimpan) sehingga
 * status persetujuan selalu mengikuti alur disposisi SOP yang sesungguhnya, dengan try/catch kosong
 * pada dua getter tanggal untuk menahan {@code LazyInitializationException} saat instance
 * {@code disposisiSop} adalah proxy canonical/shared milik {@code AuditTimestampInterceptor} yang
 * terikat ke session lain yang sudah closed. Field audit bayangan {@code oleh}/{@code olehId}/
 * {@code tanggal_dirubah} adalah keharusan teknis siklus hidup Hibernate ({@link javax.persistence.PreUpdate}
 * mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}), bukan kode
 * mati.</p>
 *
 * @see PeminjamanMasterAsset
 * @see PenghapusanMasterAsset
 * @see ais.action.master.asset.PengembalianMasterAssetAction
 * @see ais.action.master.asset.PersetujuanPengembalianMasterAssetAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "pengembalian_master_asset")
public class PengembalianMasterAsset extends DataSop {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.pengembalian_master_asset}. */
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
	 * @return {@link #kode} dokumen pengembalian, boleh {@code null} bila belum diisi
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

	/** Kode dokumen pengembalian, unik, biasanya di-generate otomatis saat simpan pertama. */
	private String kode;
	/** Keterangan/catatan bebas terkait pengembalian (mis. kondisi barang saat dikembalikan). */
	private String keterangan;
	/**
	 * Dokumen {@link PeminjamanMasterAsset} pasangan yang ditutup siklusnya oleh pengembalian
	 * ini. Wajib diisi (kolom {@code nullable = false}); lihat catatan kelas untuk penjaga
	 * status yang membatasi peminjaman mana saja yang boleh dipasangkan.
	 */
	private PeminjamanMasterAsset peminjamanMasterAsset;
	/** Tanggal dokumen pengembalian dibuat; lihat {@link #getTanggalPembuatan()} untuk fallback disposisi SOP. */
	private Date tanggalPembuatan;
	/** Tanggal dokumen pengembalian disetujui; lihat {@link #getTanggalPersetujuan()} untuk fallback disposisi SOP. */
	private Date tanggalPersetujuan;
	/** Pengguna pembuat dokumen; lihat {@link #getDibuatOleh()} untuk fallback ke pengaju disposisi SOP. */
	private Tbmuser dibuatOleh;
	/** Pengguna penyetuju dokumen; lihat {@link #getDisetujuiOleh()} untuk fallback ke penyetuju disposisi SOP. */
	private Tbmuser disetujuiOleh;
	/** Alur disposisi SOP yang menaungi persetujuan dokumen ini. */
	private DisposisiSop disposisiSop;
	/** Satuan kerja pemilik dokumen; lihat {@link #getSatuanKerja()} — diturunkan dari peminjaman terkait bila ada. */
	private SatuanKerja satuanKerja;
	/** Nilai nominal dokumen (mis. total harga beli aset yang dikembalikan); {@code null} dibaca sebagai 0.0. */
	private Double nilai;
	/** Tahun periode dokumen; lihat {@link #getTahun()} — default tahun berjalan bila belum diisi. */
	private Integer tahun;
	/** Bulan periode dokumen (1-12); lihat {@link #getBulan()} — default bulan berjalan bila belum diisi. */
	private Integer bulan;
	/** Konfigurasi penomoran surat alur pengadaan; lihat {@link #getNomorSuratAlurPengadaan()} untuk default. */
	private NomorSuratAlurPengadaan nomorSuratAlurPengadaan;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public PengembalianMasterAsset() {
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
	 * setelah trim (berbeda dari beberapa entity sejenis yang mengembalikan string kosong).
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
	 * Mengembalikan keterangan bebas dokumen ini.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
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
	 * pembuat yang tampil selalu mengikuti pengaju disposisi SOP yang sesungguhnya, bukan sekadar
	 * nilai yang disimpan langsung ke kolom {@code dibuat_oleh} saat entity dibuat.
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
	 * di-null-kan — <b>artinya status "disetujui" entity ini selalu mengikuti kondisi disposisi
	 * SOP saat ini</b>, bukan nilai yang pernah tersimpan sebelumnya. Ini adalah salah satu
	 * bentuk penjaga yang membuat {@code getDisetujuiOleh() == null} bisa dipakai dengan aman di
	 * layar aksi sebagai indikator "belum disetujui" untuk mengatur visibilitas tombol
	 * ubah/hapus/setuju.
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
	 * {@link #getTanggalPersetujuan()} berdasarkan status disposisi SOP terkait — lihat javadoc
	 * getter tersebut.
	 *
	 * @param tanggalPersetujuan tanggal persetujuan
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan tanggal persetujuan dokumen, disinkronkan dengan waktu
	 * {@code disposisiSetuju} pada {@link #getDisposisiSop()} bila tersedia (pola sama dengan
	 * {@link #getDisetujuiOleh()}). Seluruh badan method dibungkus try/catch kosong yang hanya
	 * mencatat exception ke {@link ais.common.ErrorAuditUtil} — ini FIX untuk
	 * {@code LazyInitializationException}: instance {@code disposisiSop} kadang berupa proxy
	 * canonical/shared milik {@code AuditTimestampInterceptor} yang terikat ke session Hibernate
	 * lain yang sudah closed, sehingga mengakses relasinya bisa melempar exception di luar
	 * konteks tersebut. Getter sengaja tidak dibiarkan crash; bila exception terjadi, nilai
	 * {@link #tanggalPersetujuan} fallback (nilai lama yang tersimpan) dipertahankan apa adanya.
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
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/asset/PengembalianMasterAsset.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	/**
	 * Mengisi field mentah pengguna penyetuju. Nilai ini bisa ditimpa atau di-null-kan oleh
	 * {@link #getDisetujuiOleh()} berdasarkan status disposisi SOP terkait — lihat javadoc
	 * getter tersebut.
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
	 * Try/catch kosong yang membungkus akses disposisi SOP adalah FIX
	 * {@code LazyInitializationException} yang sama seperti pada {@link #getTanggalPersetujuan()}.
	 * Berbeda dari getter tanggal lain di kelas ini, method ini juga memiliki fallback akhir:
	 * bila hasil akhirnya tetap {@code null}, dikembalikan {@link WaktuUtil#getDate()} (waktu
	 * saat ini) alih-alih {@code null} — sehingga tanggal pembuatan pada tampilan tidak pernah
	 * kosong meski entity belum pernah diisi tanggalnya secara eksplisit.
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
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/asset/PengembalianMasterAsset.java:getTanggalPembuatan-lazy");
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
	 * setiap kali dipanggil (bukan murni field tersimpan), memenuhi constraint kolom
	 * {@code unique = true} tanpa perlu kode aplikasi mengelola keunikannya secara manual —
	 * kombinasi kode+id/disposisi praktis selalu unik walau kode dasarnya sama.
	 *
	 * @return string gabungan {@code "<kode>_<idDisposisiSopAtauId>"}
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Mengisi field mentah kode unik. Nilai ini akan ditimpa kembali oleh
	 * {@link #getKodeUnik()} setiap kali getter dipanggil, sehingga pengisian manual di sini
	 * hanya berlaku sesaat sebelum pembacaan berikutnya menghitung ulang.
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
	 * Mengembalikan satuan kerja pemilik dokumen. Bila {@link #getPeminjamanMasterAsset()}
	 * terisi, satuan kerja <b>diturunkan dari peminjaman terkait</b> (menimpa field mentah
	 * entity ini) — memastikan pengembalian selalu tercatat pada satuan kerja yang sama dengan
	 * peminjaman aslinya, bukan satuan kerja pengguna yang kebetulan sedang membuat dokumen
	 * pengembalian. Hanya jatuh ke field mentah (diresolusi lewat {@link #check(Object)}) bila
	 * belum ada peminjaman terkait.
	 *
	 * @return satuan kerja efektif dokumen ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (getPeminjamanMasterAsset() != null) {
			satuanKerja = getPeminjamanMasterAsset().getSatuanKerja();
		} else {
			satuanKerja = check(satuanKerja);
		}
		return satuanKerja;
	}

	/**
	 * Mengisi field mentah satuan kerja. Nilai ini akan ditimpa oleh {@link #getSatuanKerja()}
	 * bila dokumen sudah punya peminjaman terkait — lihat javadoc getter tersebut.
	 *
	 * @param satuanKerja satuan kerja
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan nilai nominal dokumen, dengan {@code null} dinormalisasi menjadi {@code 0.0}
	 * agar aman dipakai langsung dalam kalkulasi/agregasi tanpa null-check tambahan.
	 *
	 * @return nilai nominal, tidak pernah {@code null}
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Mengisi nilai nominal dokumen.
	 *
	 * @param nilai nilai nominal, boleh {@code null} (akan dibaca sebagai 0.0)
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan tahun periode dokumen. Bila belum pernah diisi, di-lazy-default ke tahun
	 * kalender saat ini pada saat pertama kali dibaca (nilai lalu disimpan ke field, bukan
	 * dihitung ulang tiap panggilan berikutnya seperti pola {@link #getKodeUnik()}).
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
	 * {@link NomorSuratAlurPengadaan#PENGEMBALIAN_BARANG_DATA} (bukan hasil query database);
	 * jika sudah ada nilai tersimpan, diresolusi lewat {@link #check(Object)} seperti relasi
	 * lazy lainnya.
	 *
	 * @return konfigurasi nomor surat efektif, tidak pernah {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_pengadaan", nullable = true)
	public NomorSuratAlurPengadaan getNomorSuratAlurPengadaan() {
		if (nomorSuratAlurPengadaan == null) {
			nomorSuratAlurPengadaan = NomorSuratAlurPengadaan.PENGEMBALIAN_BARANG_DATA;
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
	 * Mengembalikan dokumen {@link PeminjamanMasterAsset} pasangan yang ditutup siklusnya oleh
	 * pengembalian ini. Relasi wajib (kolom {@code nullable = false}); lihat catatan kelas untuk
	 * penjaga yang membatasi peminjaman mana saja yang boleh dipasangkan pada layar input.
	 *
	 * @return peminjaman terkait; seharusnya tidak pernah {@code null} untuk dokumen yang valid
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "peminjaman_master_asset", nullable = false)
	public PeminjamanMasterAsset getPeminjamanMasterAsset() {
		return peminjamanMasterAsset;
	}

	/**
	 * Mengisi dokumen peminjaman pasangan. Dipanggil oleh layar input
	 * ({@code PengembalianMasterAssetAction}) setelah pengguna memilih peminjaman dari combobox
	 * yang sudah difilter (disetujui &amp; belum pernah dikembalikan) — lihat catatan kelas.
	 *
	 * @param peminjamanMasterAsset peminjaman yang menjadi pasangan pengembalian ini
	 */
	public void setPeminjamanMasterAsset(PeminjamanMasterAsset peminjamanMasterAsset) {
		this.peminjamanMasterAsset = peminjamanMasterAsset;
	}

	/**
	 * Mengembalikan status aktif dokumen, disinkronkan dengan status disposisi SOP terkait:
	 * bernilai {@code false} bila disposisi SOP sudah tidak aktif ({@code !getAktif()}), atau
	 * bila alur disposisi SOP berakhir pada titik penolakan
	 * ({@code disposisiEnd.alurSop.penolakanAdaDiSini}) — dengan kata lain, dokumen yang
	 * pengajuannya ditolak lewat alur SOP otomatis dibaca sebagai tidak aktif meski field
	 * mentah {@link #aktif} sendiri tidak pernah diubah secara eksplisit oleh kode ini. Selain
	 * dua kondisi tersebut, {@code null} dibaca sebagai aktif.
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
	 * {@link #getAktif()} berdasarkan status disposisi SOP terkait — lihat javadoc getter
	 * tersebut.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
