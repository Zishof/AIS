package ais.database.model.employ;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Statusabsensi;
import ais.database.model.surat.NomorSurat;

/**
 * Entitas Hibernate katalog jenis cuti &amp; izin kepegawaian AIS — dipetakan ke tabel
 * {@code employ.jenis_cuti_dan_izin}. Baris entitas ini adalah master/katalog (mis. "Cuti
 * Tahunan", "Cuti Sakit", "Izin Keperluan Keluarga") yang dirujuk oleh field
 * {@code jenisCutiDanIzin} pada {@link ais.database.model.payroll.CutiDanIzin} (dokumen pengajuan
 * cuti/izin aktual milik seorang pegawai) — entitas ini sendiri TIDAK menyimpan data pengajuan,
 * hanya mendefinisikan jenis-jenis yang tersedia untuk dipilih beserta parameter tambahan apa
 * saja yang relevan untuknya.
 *
 * <h2>Cache statis parameter tambahan ({@link #mapParameters})</h2>
 * <p>
 * {@link #getKelompokParameterTambahanCutiDanIzins()} dan
 * {@link #setKelompokParameterTambahanCutiDanIzins(Set)} membaca/menulis lewat sebuah
 * {@code static} {@link Map} tingkat-kelas ({@link #mapParameters}), berkunci {@link #id}. Ini
 * BUKAN cache per-request/per-sesi Hibernate biasa — ia hidup selama JVM berjalan, dibagi oleh
 * SEMUA instance {@link JenisCutiDanIzin} (termasuk lintas sesi pengguna &amp; lintas thread) dan
 * TIDAK PERNAH dibersihkan/di-invalidasi otomatis. Konsekuensinya: begitu kelompok parameter
 * untuk suatu {@code id} pernah di-set sekali (mis. lewat form admin), nilai itu akan terus
 * dipakai oleh {@code getKelompokParameterTambahanCutiDanIzins()} pada instance mana pun dengan
 * {@code id} yang sama sampai proses aplikasi di-restart atau nilai baru ditulis ulang — perubahan
 * data di database oleh proses/instance lain tidak otomatis tercermin di sini. Waspadai potensi
 * data basi (stale) pada deployment multi-instance atau setelah perubahan langsung di database.
 * </p>
 *
 * <h2>Getter destruktif format CSV ({@link #getJenisPengguna()}, {@link #getUsernamePengguna()})</h2>
 * <p>
 * Kedua getter ini BUKAN getter murni: setiap kali dipanggil, keduanya menormalkan ulang isi field
 * yang bersangkutan (membungkusnya dengan koma di awal/akhir lalu merapikan koma ganda) dan
 * MENIMPA field instance dengan hasil normalisasi tersebut sebagai efek samping, baru kemudian
 * mengembalikan nilai yang sudah di-trim. Lihat catatan pada masing-masing getter.
 * </p>
 *
 * @see ais.database.model.payroll.CutiDanIzin
 * @see KelompokParameterTambahanCutiDanIzin
 * @see ParameterTambahanCutiDanIzin
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "jenis_cuti_dan_izin")
public class JenisCutiDanIzin extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris ini, di-generate database (IDENTITY). */
	private Long id;
	/** Nama pengguna audit terakhir yang mengubah baris ini. */
	private String oleh;
	/** Id pengguna audit terakhir yang mengubah baris ini (pasangan {@link #oleh}). */
	private String olehId;

	/** @return {@link #olehId} — id pengguna audit terakhir yang mengubah baris ini. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Meng-set {@link #olehId}; dilewati (no-op) bila {@code olehId} {@code null} atau kosong/hanya
	 * berisi spasi, sehingga nilai audit lama tidak pernah tertimpa nilai kosong.
	 *
	 * @param olehId id pengguna yang melakukan perubahan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Meng-set {@link #oleh}; dilewati (no-op) bila {@code oleh} {@code null} atau kosong/hanya
	 * berisi spasi, sehingga nilai audit lama tidak pernah tertimpa nilai kosong.
	 *
	 * @param oleh nama pengguna yang melakukan perubahan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return {@link #oleh} — nama pengguna audit terakhir yang mengubah baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}, dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-UPDATE; mendelegasikan pembaruan {@link #tanggal_dirubah} ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak dipanggil manual
	 * dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah nilai timestamp audit baru; dipanggil manual maupun otomatis oleh {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return {@link #tanggal_dirubah} — timestamp terakhir baris ini diubah; nilai awal saat konstruksi objek adalah waktu sekarang. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi teks baris ini: {@link #id} digabung dengan {@link #nama}. */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode singkat/mnemonic jenis cuti/izin ini (opsional, dipakai untuk pencocokan cepat di luar {@link #id}). */
	private String kode;

	/** Nama jenis cuti/izin (mis. "Cuti Tahunan", "Izin Sakit"). */
	private String nama;
	/** Keterangan/deskripsi bebas untuk jenis cuti/izin ini, boleh {@code null}. */
	private String keterangan;
	/** Menandai apakah jenis cuti/izin ini masih aktif/boleh dipilih pada pengajuan baru; {@code null} diperlakukan sebagai aktif (lihat {@link #getAktif()}). */
	private Boolean aktif;

	/** Nomor surat terkait jenis cuti/izin ini (mis. dasar hukum/referensi surat edaran), boleh {@code null}. */
	private NomorSurat nomorSurat;

	/**
	 * Cache statis tingkat-kelas berkunci {@link #id}, dibagi oleh SEMUA instance
	 * {@link JenisCutiDanIzin} sepanjang umur JVM — lihat "Cache statis parameter tambahan" pada
	 * Javadoc kelas untuk implikasi (data basi lintas sesi/instance, tidak pernah kedaluwarsa
	 * otomatis).
	 */
	public static Map<Long, Set<KelompokParameterTambahanCutiDanIzin>> mapParameters = new HashMap<Long, Set<KelompokParameterTambahanCutiDanIzin>>();

	/** Kelompok parameter tambahan yang berlaku untuk jenis cuti/izin ini; lihat {@link #getKelompokParameterTambahanCutiDanIzins()} untuk perilaku cache statisnya. */
	private Set<KelompokParameterTambahanCutiDanIzin> kelompokParameterTambahanCutiDanIzins = new TreeSet<KelompokParameterTambahanCutiDanIzin>();
	/** Status absensi yang terkait/dipicu oleh pemilihan jenis cuti/izin ini, boleh {@code null}. */
	private Statusabsensi statusabsensi;
	/** Daftar username pengguna (format CSV dibungkus koma) yang dibatasi boleh memakai jenis cuti/izin ini; lihat {@link #getUsernamePengguna()}. */
	private String usernamePengguna;
	/** Daftar jenis pengguna (format CSV dibungkus koma) yang dibatasi boleh memakai jenis cuti/izin ini; lihat {@link #getJenisPengguna()}. */
	private String jenisPengguna;

	/**
	 * @return {@link #kelompokParameterTambahanCutiDanIzins} — kelompok parameter tambahan yang
	 *         berlaku untuk jenis cuti/izin ini, terurut menurut {@code nomorUrut} lalu
	 *         {@code nama}. Bila {@link #id} sudah terisi, nilai kembalian SEBENARNYA dibaca dari
	 *         cache statis {@link #mapParameters} (bila ada entri untuknya) — BUKAN murni field
	 *         instance ini — lihat "Cache statis parameter tambahan" pada Javadoc kelas.
	 */
	@ManyToMany(targetEntity = KelompokParameterTambahanCutiDanIzin.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nomorUrut asc, nama asc")
	@JoinTable(name = "jenis_pengajuan_cuti_has_parameter", schema = "employ", joinColumns = @JoinColumn(name = "jenis_catatan_administrasi"), inverseJoinColumns = @JoinColumn(name = "parameter"))
	public Set<KelompokParameterTambahanCutiDanIzin> getKelompokParameterTambahanCutiDanIzins() {
		if (id != null) {
			Set<KelompokParameterTambahanCutiDanIzin> temp = mapParameters.get(id);
			if (temp != null) {
				kelompokParameterTambahanCutiDanIzins = temp;
			}
		}
		return kelompokParameterTambahanCutiDanIzins;
	}

	/**
	 * Meng-set {@link #kelompokParameterTambahanCutiDanIzins}; bila {@link #id} sudah terisi, nilai
	 * yang sama JUGA dituliskan ke cache statis {@link #mapParameters} (berkunci {@code id}) —
	 * sehingga pemanggilan ini berefek global lintas seluruh JVM, bukan hanya pada instance ini.
	 * Lihat "Cache statis parameter tambahan" pada Javadoc kelas.
	 *
	 * @param kelompokParameterTambahanCutiDanIzins kelompok parameter tambahan baru
	 */
	public void setKelompokParameterTambahanCutiDanIzins(
			Set<KelompokParameterTambahanCutiDanIzin> kelompokParameterTambahanCutiDanIzins) {
		this.kelompokParameterTambahanCutiDanIzins = kelompokParameterTambahanCutiDanIzins;
		if (id != null) {
			mapParameters.put(id, kelompokParameterTambahanCutiDanIzins);
		}
	}

	/** Konstruktor default (dibutuhkan Hibernate); tidak menginisialisasi field apa pun secara eksplisit selain default deklarasi field. */
	public JenisCutiDanIzin() {
	}

	/** @return {@link #id} — primary key baris ini. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baru; normalnya di-generate database, jarang di-set manual dari kode aplikasi. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return {@link #kode} yang sudah di-trim; string kosong (bukan {@code null}) bila {@link #kode} {@code null}. */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/** @param kode kode singkat/mnemonic baru untuk jenis cuti/izin ini. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return {@link #nama} yang sudah di-trim; {@code null} bila {@link #nama} {@code null}. */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama jenis cuti/izin baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return {@link #keterangan} — keterangan/deskripsi bebas jenis cuti/izin ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi bebas baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return {@link #aktif}; {@code true} bila belum pernah di-set ({@code null}) — default aktif. */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif baru untuk jenis cuti/izin ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return {@link #nomorSurat} — nomor surat terkait jenis cuti/izin ini, dilewatkan {@link #check(Object)} (pola lazy-init standar {@code GeneralValueObject}). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat", nullable = true)
	public NomorSurat getNomorSurat() {
		nomorSurat = check(nomorSurat);
		return nomorSurat;
	}

	/** @param nomorSurat nomor surat baru. */
	public void setNomorSurat(NomorSurat nomorSurat) {
		this.nomorSurat = nomorSurat;
	}


	/** @return {@link #statusabsensi} — status absensi yang terkait jenis cuti/izin ini, dilewatkan {@link #check(Object)} (pola lazy-init standar {@code GeneralValueObject}). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "statusabsensi", nullable = true)
	public Statusabsensi getStatusabsensi() {
		statusabsensi = check(statusabsensi);
		return statusabsensi;
	}

	/** @param statusabsensi status absensi baru. */
	public void setStatusabsensi(Statusabsensi statusabsensi) {
		this.statusabsensi = statusabsensi;
	}



	/**
	 * @return {@link #jenisPengguna} setelah dinormalkan ulang: nilai field DITIMPA (efek samping)
	 *         menjadi bentuk {@code ",item1,item2,"} (dibungkus koma di kedua ujung, koma ganda
	 *         dirapikan), lalu dikembalikan string kosong bila hasil normalisasi berujung kosong
	 *         (mis. {@code ","}, {@code ",-,"}) atau nilai yang sudah di-trim jika berisi. Getter
	 *         ini BUKAN getter murni — lihat "Getter destruktif format CSV" pada Javadoc kelas.
	 */
	@Column(name = "jenis_pengguna", nullable = true, columnDefinition = "text")
	public String getJenisPengguna() {

		jenisPengguna = (jenisPengguna == null || jenisPengguna.trim().equalsIgnoreCase(",") ? ""
				: "," + jenisPengguna.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (jenisPengguna.equals(",")) {
			jenisPengguna = "";
		} else if (jenisPengguna.equals(",,")) {
			jenisPengguna = "";
		} else if (jenisPengguna.equals(",,,")) {
			jenisPengguna = "";
		} else if (jenisPengguna.equals(",,,,")) {
			jenisPengguna = "";
		}

		if (jenisPengguna.equals(",-,")) {
			jenisPengguna = "";
		}

		return jenisPengguna == null ? "" : jenisPengguna.trim();
	}

	/** @param jenisPengguna daftar jenis pengguna baru (bentuk mentah, akan dinormalkan saat berikutnya dibaca lewat {@link #getJenisPengguna()}). */
	public void setJenisPengguna(String jenisPengguna) {
		this.jenisPengguna = jenisPengguna;
	}

	/**
	 * @return {@link #usernamePengguna} setelah dinormalkan ulang, dengan mekanisme identik
	 *         {@link #getJenisPengguna()} (bungkus koma, rapikan koma ganda, kosongkan bila hasil
	 *         akhirnya hanya berisi koma/{@code "-"}). Getter ini BUKAN getter murni — lihat
	 *         "Getter destruktif format CSV" pada Javadoc kelas.
	 */
	@Column(name = "username_pengguna", nullable = true, columnDefinition = "text")
	public String getUsernamePengguna() {

		usernamePengguna = (usernamePengguna == null || usernamePengguna.trim().equalsIgnoreCase(",") ? ""
				: "," + usernamePengguna.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (usernamePengguna.equals(",")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(",,")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(",,,")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(",,,,")) {
			usernamePengguna = "";
		}

		if (usernamePengguna.equals(",-,")) {
			usernamePengguna = "";
		}

		return usernamePengguna == null ? "" : usernamePengguna.trim();
	}

	/** @param usernamePengguna daftar username pengguna baru (bentuk mentah, akan dinormalkan saat berikutnya dibaca lewat {@link #getUsernamePengguna()}). */
	public void setUsernamePengguna(String usernamePengguna) {
		this.usernamePengguna = usernamePengguna;
	}
}
