package ais.database.model.koperasi;

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

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.ConstantValues;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Toko;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.WaktuUtil;

/**
 * <h2>KodePembayaranOnline — Bukti Konfirmasi Pembayaran Online Lintas-Modul</h2>
 *
 * <p>
 * Satu baris = satu kode pembayaran online (VA/QRIS/e-wallet, dsb.) yang <b>dibuat oleh callback
 * gateway pembayaran</b> setelah pembeli/wali/anggota berhasil membayar di kanal eksternal. Ini
 * BUKAN mesin virtual account multi-bank seperti {@link ais.database.model.VirtualAccountBank}
 * (yang dipanggil 17 servlet callback bank berbeda) -- entity ini adalah tabel bukti/hasil yang
 * lebih generik: kode unik dibangkitkan front-end kasir ({@code PosKantinAction.generateKodeUnik()},
 * minimal 50 karakter) saat transaksi dimulai, lalu dipoll berkala oleh kasir
 * ({@code KantinHelper.checkBayar}) sampai baris dengan kode tersebut muncul di tabel ini --
 * kemunculannya sendiri yang menandakan gateway telah mengonfirmasi pelunasan. Kelas ini hanya
 * DIBACA oleh alur poll tersebut; pembuatan barisnya terjadi di callback terpisah, di luar berkas
 * ini.
 * </p>
 *
 * <h3>Dipakai lintas modul</h3>
 * <p>
 * Karena payment gateway yang sama dipakai bersama, satu baris kode pembayaran dapat menunjuk ke
 * pembayar dari modul berbeda: {@link #getSiswa()} (SPP sekolah), {@link #getCalonSiswa()}/
 * {@link #getBiodataCalonMahasiswa()} (pendaftaran), {@link #getMahasiswa()} (kampus), atau
 * {@link #getAnggotaKoperasi()}/{@link #getTbmuser()} (koperasi/kantin). Getter {@link #getNama()}
 * dan {@link #getTbmuser()} punya efek samping menyimpulkan nilai dari relasi lain saat dipanggil
 * (lihat javadoc masing-masing) -- pola getter dgn logika turunan yang umum di model hbm2java AIS.
 * </p>
 *
 * <h3>Catatan desain</h3>
 * <p>
 * {@link #buatAtauAmbilAnggotaKoperasi(Session)} adalah method bisnis (bukan getter/setter murni)
 * yang tertanam di entity ini: ia membuka dan meng-commit transaksi Hibernate-nya SENDIRI di dalam
 * method entity -- pola yang tidak lazim (biasanya pengelolaan transaksi ada di layer
 * action/helper, bukan di model), sehingga pemanggil yang sudah berada di dalam transaksinya sendiri
 * berisiko konflik transaksi bersarang. Ditandai di javadoc method tsb agar pemanggil baru berhati-hati.
 * Kompatibel Java 1.7, {@code hbm2java generated}, nama tabel fisik {@code kode_pembayaran_pnline}
 * mengandung salah ketik ("pnline") yang sudah terlanjur dipakai produksi -- tidak diubah di sini
 * karena migrasi nama tabel di luar cakupan dokumentasi.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "kode_pembayaran_pnline")
public class KodePembayaranOnline extends GeneralValueObject {

	/** Versi serialisasi tetap untuk kompatibilitas antar-build. */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/** @return id pengguna (audit shadow) yang terakhir menyimpan/mengubah baris ini. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * @param olehId id pengguna audit; nilai kosong/{@code null} diabaikan (nilai lama
	 *               dipertahankan) -- pola field audit shadow yang sama dipakai entity lain di
	 *               paket koperasi.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * @param oleh nama pengguna audit; nilai kosong/{@code null} diabaikan (nilai lama
	 *             dipertahankan), sama seperti {@link #setOlehId(String)}.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna (audit shadow) yang terakhir menyimpan/mengubah baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: mendelegasikan pencatatan {@link #tanggal_dirubah} (dan field
	 * audit sejenis) ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}. Dipanggil
	 * otomatis oleh provider JPA setiap {@code UPDATE}, tidak untuk dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir (biasanya tidak diset manual). */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu baris terakhir diubah; diperbarui otomatis lewat {@link #onUpdate()}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas untuk log/debug: {@code id-nama}. */
	public String toString() {
		return id + "-" + nama;
	}

	private String kode;
	private Date waktu;
	private Double nominal;
	private String nama;
	private String logPembayaran;
	private String keterangan;
	private Boolean aktif;
	private Siswa siswa;
	private CalonSiswa calonSiswa;
	private Mahasiswa mahasiswa;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private PembelianAnggotaKoperasi pembelianAnggotaKoperasi;
	private AnggotaKoperasi anggotaKoperasi;
	private Tbmuser tbmuser;
	private Toko toko;

	/**
	 * Mencari {@link AnggotaKoperasi} yang cocok dengan pembayar baris ini (berdasarkan
	 * {@link #getSiswa()}, {@link #getMahasiswa()}, atau {@link #getTbmuser()} -- urutan
	 * pemeriksaan inilah urutan prioritasnya), atau <b>membuat baris anggota koperasi baru</b>
	 * bila belum ada yang cocok, lalu mengembalikannya.
	 *
	 * <p><b>PERINGATAN transaksi bersarang:</b> saat perlu membuat anggota baru, method ini
	 * memanggil {@code session.getTransaction().begin()} dan {@code commit()} SENDIRI di dalam
	 * method entity ini -- bukan pola umum di AIS (biasanya pengelolaan transaksi ada di
	 * action/helper pemanggil). Pemanggil yang sudah berada di dalam transaksi Hibernate-nya
	 * sendiri pada {@code session} yang sama berisiko error "transaction already active" atau,
	 * pada beberapa provider, commit dini yang tidak diharapkan. Sebaiknya dipanggil pada
	 * {@code session} yang belum punya transaksi terbuka.</p>
	 *
	 * @param session sesi Hibernate aktif; dipakai baik untuk pencarian maupun (bila perlu)
	 *                pembuatan baris {@link AnggotaKoperasi} baru dengan transaksinya sendiri.
	 * @return anggota koperasi yang cocok atau baru dibuat; {@code null} bila baris ini sama
	 *         sekali tidak menunjuk siswa, mahasiswa, maupun {@code tbmuser} (mis. pembayaran
	 *         calon siswa/mahasiswa yang belum diverifikasi jadi anggota).
	 */
	public AnggotaKoperasi buatAtauAmbilAnggotaKoperasi(Session session) {

		if (getSiswa() == null && getMahasiswa() == null && getTbmuser() == null) {
			return null;
		}

		Criterion criterion = Restrictions.sqlRestriction("false");

		if (getSiswa() != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("siswa", getSiswa()));
		} else if (getMahasiswa() != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("mahasiswa", getMahasiswa()));
		} else if (getTbmuser() != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tbmuser", getTbmuser()));
		}

		AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) ConstantValues.simpleObject(
				session.createCriteria(AnggotaKoperasi.class).add(criterion).setMaxResults(1), AnggotaKoperasi.class);
		if (anggotaKoperasi == null) {
			anggotaKoperasi = new AnggotaKoperasi();
			anggotaKoperasi.setMahasiswa(getMahasiswa());
			anggotaKoperasi.setDosen(getTbmuser() == null ? null : getTbmuser().getDosen());
			anggotaKoperasi.setGuru(getTbmuser() == null ? null : getTbmuser().getGuru());
			anggotaKoperasi.setSiswa(getSiswa());
			anggotaKoperasi.setPegawai(getTbmuser() == null ? null : getTbmuser().getPegawai());
			anggotaKoperasi.setTbmuser(getTbmuser());
			session.getTransaction().begin();
			session.save(anggotaKoperasi);
			session.getTransaction().commit();
		}
		return anggotaKoperasi;
	}

	/** Konstruktor bawaan (dipakai JPA/Hibernate; dan callback gateway saat mencatat pembayaran baru). */
	public KodePembayaranOnline() {
	}

	/** @return id baris (identity, dibuat DB). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; biasanya tidak diset manual, dibuat DB saat {@code save}. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return kode pembayaran unik yang dibangkitkan kasir dan dipoll dari front-end
	 *         ({@code PosKantinAction.generateKodeUnik()}, minimal 50 karakter agar tidak bentrok
	 *         antar-toko/kasir bersamaan). Kolom {@code unique = true} menegakkannya di level DB.
	 *         Nilai {@code null}/kosong dinormalisasi menjadi {@code null}; selain itu ditrim.
	 */
	@Column(name = "kode", nullable = false, columnDefinition = "text", unique = true)
	public String getKode() {
		return kode == null || kode.isEmpty() ? null : kode.trim();
	}

	/** @param kode kode pembayaran unik dari sisi kasir/gateway. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return nama pembayar untuk ditampilkan. Bila field {@link #nama} belum diisi eksplisit,
	 *         method ini MENYIMPULKANNYA saat dipanggil dari relasi yang terisi -- diperiksa
	 *         berurutan: {@link #getSiswa()}, {@link #getMahasiswa()}, {@link #getBiodataCalonMahasiswa()},
	 *         {@link #getCalonSiswa()}, pembeli lewat {@link #getPembelianAnggotaKoperasi()}, lalu
	 *         {@link #getTbmuser()} -- dan MENIMPA field {@link #nama} sebagai efek samping (bukan
	 *         getter murni). Setiap cabang menggabungkan identitas (NIS/NIM/kode member) dengan
	 *         nama orangnya. Bila tidak ada satupun relasi yang cocok, mengembalikan nilai
	 *         {@link #nama} apa adanya (ditrim, atau {@code null}).
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		if (getSiswa() != null) {
			nama = getSiswa().getNomorIndukNasional() + " " + getSiswa().getNama();
		} else if (getMahasiswa() != null) {
			nama = getMahasiswa().getNim() + " " + getMahasiswa().getNama();
		} else if (getBiodataCalonMahasiswa() != null) {
			nama = getBiodataCalonMahasiswa().getNoRegistrasi() + " " + getBiodataCalonMahasiswa().getNama();
		} else if (getCalonSiswa() != null) {
			nama = getCalonSiswa().getNoRegistrasi() + " " + getCalonSiswa().getNama();
		} else if (getPembelianAnggotaKoperasi() != null
				&& getPembelianAnggotaKoperasi().getAnggotaKoperasi() != null) {
			nama = getPembelianAnggotaKoperasi().getAnggotaKoperasi().getKodeIdentitas() + " "
					+ getPembelianAnggotaKoperasi().getAnggotaKoperasi().getNama();
		} else if (getTbmuser() != null) {
			nama = getTbmuser().getUserNama();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama pembayar; bila diset eksplisit, nilai ini akan tertimpa oleh {@link #getNama()}
	 *              pada panggilan berikutnya bila salah satu relasi pembayar terisi. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return catatan bebas terkait pembayaran ini (opsional; bisa diisi callback gateway). */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan bebas terkait pembayaran ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return siswa pembayar (SPP sekolah), bila kode pembayaran ini untuk siswa. {@code check()} menormalkan proxy Hibernate. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);

		return siswa;
	}

	/** @param siswa siswa pembayar. */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/** @return calon siswa pembayar (pendaftaran sekolah), bila kode pembayaran ini untuk calon siswa. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/** @param calonSiswa calon siswa pembayar. */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	/** @return mahasiswa pembayar (biaya kampus), bila kode pembayaran ini untuk mahasiswa. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);

		return mahasiswa;
	}

	/** @param mahasiswa mahasiswa pembayar. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** @return biodata calon mahasiswa pembayar (pendaftaran kampus), bila kode pembayaran ini untuk calon mahasiswa. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	/** @param biodataCalonMahasiswa biodata calon mahasiswa pembayar. */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * @return pengguna ({@code Tbmuser}) pembayar (mis. pegawai/guru/dosen di konteks
	 *         koperasi/kantin), bila kode pembayaran ini untuk pengguna. <b>Efek samping:</b> bila
	 *         salah satu dari {@link #getSiswa()}, {@link #getCalonSiswa()},
	 *         {@link #getBiodataCalonMahasiswa()}, atau {@link #getMahasiswa()} terisi, method ini
	 *         MEMAKSA hasilnya menjadi {@code null} walau field {@link #tbmuser} sendiri terisi --
	 *         relasi sekolah/kampus selalu diprioritaskan di atas relasi {@code tbmuser} generik.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		if (getSiswa() != null || getCalonSiswa() != null || getBiodataCalonMahasiswa() != null
				|| getMahasiswa() != null) {
			tbmuser = null;
		}
		return tbmuser;
	}

	/** @param tbmuser pengguna pembayar. */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/** @return pembelian anggota koperasi yang dilunasi lewat kode pembayaran ini, bila konteksnya koperasi/kantin. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembelian_anggota_koperasi", nullable = true)
	public PembelianAnggotaKoperasi getPembelianAnggotaKoperasi() {
		pembelianAnggotaKoperasi = check(pembelianAnggotaKoperasi);
		return pembelianAnggotaKoperasi;
	}

	/** @param pembelianAnggotaKoperasi pembelian anggota koperasi yang dilunasi lewat kode pembayaran ini. */
	public void setPembelianAnggotaKoperasi(PembelianAnggotaKoperasi pembelianAnggotaKoperasi) {
		this.pembelianAnggotaKoperasi = pembelianAnggotaKoperasi;
	}

	/** @return status aktif baris ini. Fallback ke {@code true} bila kolom {@code null}. */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif baris ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return waktu pembayaran dikonfirmasi. Fallback ke waktu sekarang ({@link WaktuUtil#getDate()}) bila kolom {@code null}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/** @param waktu waktu pembayaran dikonfirmasi. */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/** @return nominal pembayaran yang dikonfirmasi gateway. */
	public Double getNominal() {
		return nominal;
	}

	/** @param nominal nominal pembayaran yang dikonfirmasi gateway. */
	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	/** @return log mentah/response dari gateway pembayaran (untuk keperluan audit/debug). */
	@Column(name = "log_pembayaran", columnDefinition = "text")
	public String getLogPembayaran() {
		return logPembayaran;
	}

	/** @param logPembayaran log mentah/response dari gateway pembayaran. */
	public void setLogPembayaran(String logPembayaran) {
		this.logPembayaran = logPembayaran;
	}

	/** @return toko tempat transaksi/kode pembayaran ini dibuat, bila relevan (konteks kasir kantin/koperasi). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = true)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/** @param toko toko tempat transaksi/kode pembayaran ini dibuat. */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/** @return anggota koperasi pembayar, bila kode pembayaran ini untuk anggota koperasi secara langsung. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		anggotaKoperasi = check(anggotaKoperasi);
		return anggotaKoperasi;
	}

	/** @param anggotaKoperasi anggota koperasi pembayar. */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

}
