package ais.database.model;

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

import ais.database.model.surat.NomorSurat;

/**
 * Model data untuk satu JENIS pengajuan pegawai (mis. "Cuti", "Lembur", "Dinas Luar") -- definisi
 * TEMPLATE yang mengatur perilaku pengajuan dari jenis tersebut: apakah masuk hitungan presensi
 * ({@link #getMasukPresensi()}), lembur ({@link #getMasukLembur()}), berhak konsumsi ({@link
 * #getDapatKonsumsi()}), penomoran surat lewat {@link NomorSurat}, daftar parameter tambahan
 * dinamis yang harus diisi ({@link #getKelompokParameterTambahanPengajuanPegawais()}), serta
 * pembatasan jenis/username pengguna yang boleh mengajukan. <b>Ini TABEL TERPISAH TOTAL</b> dari
 * {@code JenisPengajuan} (root, sudah didokumentasikan batch sebelumnya) -- keduanya kebetulan
 * mirip nama tapi tidak berelasi FK maupun berbagi tabel; {@code JenisPengajuan} untuk pengajuan
 * NON-pegawai (mis. akademik/kemahasiswaan), kelas ini KHUSUS untuk pengajuan pegawai.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Boolean aktif}, {@code NomorSurat nomorSurat}, {@code
 * Boolean dapatKonsumsi}, {@code Boolean masukLembur}, {@code Boolean masukPresensi}, {@code String
 * jenisPengguna}, {@code String usernamePengguna}; pemetaan persistence: tabel
 * {@code public.jenis_pengajuan_pegawai}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code
 * getOleh()}, {@code getTanggal_dirubah()}, {@code getAktif()}, {@code getKelompokParameterTambahanPengajuanPegawais()},
 * {@code getJenisPengguna()}); mutasi data ({@code setOlehId()}, {@code setId()}, {@code setOleh()}, {@code
 * onUpdate()}, {@code setTanggal_dirubah()}, {@code setAktif()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Cache statis {@link #mapParameters}:</b> {@link #getKelompokParameterTambahanPengajuanPegawais()}
 * dan setter-nya menyalin ke/dari {@code Map<Long, Set<...>>} STATIS bersama SELURUH JVM/tenant (bukan
 * per-request), diindeks id entity -- pola yang sama dipakai beberapa entity "Jenis*" lain di AIS. Cache ini
 * tidak pernah kedaluwarsa sendiri; perubahan definisi kelompok parameter di database tidak otomatis
 * tercermin sampai baris entity ini di-{@code set} ulang lewat kode yang memanggil setter-nya.</p>
 * <p><b>Getter yang menulis field ({@code getJenisPengguna()}, {@code getUsernamePengguna()}):</b> keduanya
 * MENIMPA field-nya sendiri setiap pemanggilan untuk menormalkan format daftar dipisah koma (membungkus
 * dengan koma di kedua ujung, memampatkan koma ganda berulang, dan mengosongkan bila hasilnya hanya berisi
 * separator) -- pola berulang di puluhan entity AIS ({@code ais-getter-mutasi-field-anti-pattern-sistemik}),
 * bukan cacat unik kelas ini.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori
 * (kecuali {@link #mapParameters} yang bersifat cache statis lintas request seperti dijelaskan di atas).
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see JenisPengajuan tabel terpisah total untuk pengajuan non-pegawai
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_pengajuan_pegawai")
public class JenisPengajuanPegawai extends GeneralValueObject {

	/** Versi serialisasi Java untuk kompatibilitas {@code Serializable}. */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key entity (kolom {@code id}, identity/auto-increment). */
	private Long id;
	/**
	 * Nama pengguna pengubah terakhir. Field ini MENIMPA (shadow) field bernama sama pada
	 * {@link GeneralValueObject}; getter/setter di bawah beroperasi pada field lokal ini.
	 */
	private String oleh;
	/** Id pengguna pengubah terakhir; shadow dari field sama pada {@link GeneralValueObject}. */
	private String olehId;

	/**
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 * @see GeneralValueObject#getOlehId()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link GeneralValueObject#setOlehId(String)}.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link #setOlehId(String)}.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA sebelum UPDATE: memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir yang baru. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas untuk debug/log: {@code "<id>-<nama>"}. */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode ringkas jenis pengajuan; lihat {@link #getKode()} untuk perilaku default. */
	private String kode;

	/** Nama jenis pengajuan (mis. "Cuti", "Lembur"). */
	private String nama;
	/** Keterangan bebas. */
	private String keterangan;
	/** Menandai jenis pengajuan ini masih aktif ditawarkan; lihat {@link #getAktif()} untuk default. */
	private Boolean aktif;

	/** Konfigurasi penomoran surat untuk pengajuan jenis ini (opsional). */
	private NomorSurat nomorSurat;

	/** Cache statis lintas request; lihat catatan cache pada javadoc class. */
	public static Map<Long, Set<KelompokParameterTambahanPengajuanPegawai>> mapParameters = new HashMap<Long, Set<KelompokParameterTambahanPengajuanPegawai>>();

	/** Kumpulan kelompok parameter tambahan yang harus diisi untuk jenis pengajuan ini. */
	private Set<KelompokParameterTambahanPengajuanPegawai> kelompokParameterTambahanPengajuanPegawais = new TreeSet<KelompokParameterTambahanPengajuanPegawai>();
	/** Menandai pengajuan jenis ini berhak mendapat konsumsi; lihat {@link #getDapatKonsumsi()} untuk default. */
	private Boolean dapatKonsumsi;
	/** Menandai pengajuan jenis ini dihitung sebagai lembur; lihat {@link #getMasukLembur()} untuk default. */
	private Boolean masukLembur;
	/** Menandai pengajuan jenis ini dihitung dalam rekap presensi; lihat {@link #getMasukPresensi()} untuk default. */
	private Boolean masukPresensi;
	/** Daftar jenis pengguna (peran) yang boleh mengajukan, dipisah koma; lihat {@link #getJenisPengguna()}. */
	private String jenisPengguna;
	/** Daftar username spesifik yang boleh mengajukan, dipisah koma; lihat {@link #getUsernamePengguna()}. */
	private String usernamePengguna;

	/**
	 * @return kumpulan kelompok parameter tambahan untuk jenis pengajuan ini. Bila
	 *         {@link #getId()} tidak {@code null} dan ADA entri di {@link #mapParameters} untuk id
	 *         tersebut, field ini DITIMPA dari cache statis itu sebelum dikembalikan -- lihat
	 *         catatan cache pada javadoc class soal kedaluwarsa.
	 */
	@ManyToMany(targetEntity = KelompokParameterTambahanPengajuanPegawai.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nomorUrut asc, nama asc")
	@JoinTable(name = "jenis_pengajuan_pegawai_has_parameter", schema = "public", joinColumns = @JoinColumn(name = "jenis_catatan_administrasi"), inverseJoinColumns = @JoinColumn(name = "parameter"))
	public Set<KelompokParameterTambahanPengajuanPegawai> getKelompokParameterTambahanPengajuanPegawais() {
		if (id != null) {
			Set<KelompokParameterTambahanPengajuanPegawai> temp = mapParameters.get(id);
			if (temp != null) {
				kelompokParameterTambahanPengajuanPegawais = temp;
			}
		}
		return kelompokParameterTambahanPengajuanPegawais;
	}

	/**
	 * @param kelompokParameterTambahanPengajuanPegawais kumpulan kelompok parameter tambahan baru;
	 *                                                    JUGA dituliskan ke {@link #mapParameters}
	 *                                                    (cache statis) bila {@link #getId()}
	 *                                                    tidak {@code null}.
	 */
	public void setKelompokParameterTambahanPengajuanPegawais(
			Set<KelompokParameterTambahanPengajuanPegawai> kelompokParameterTambahanPengajuanPegawais) {
		this.kelompokParameterTambahanPengajuanPegawais = kelompokParameterTambahanPengajuanPegawais;
		if (id != null) {
			mapParameters.put(id, kelompokParameterTambahanPengajuanPegawais);
		}
	}

	/** Konstruktor kosong, dipakai Hibernate. */
	public JenisPengajuanPegawai() {
	}

	/** @return primary key entity, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baru. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return kode ringkas, string kosong (bukan {@code null}) bila belum diisi. */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/** @param kode kode ringkas baru. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return nama jenis pengajuan, sudah di-{@code trim}; {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama jenis pengajuan baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan bebas yang baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return {@code true} (default) bila jenis pengajuan ini masih aktif ditawarkan. */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif penanda aktif yang baru. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return konfigurasi penomoran surat, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat", nullable = true)
	public NomorSurat getNomorSurat() {
		nomorSurat = check(nomorSurat);
		return nomorSurat;
	}

	/** @param nomorSurat konfigurasi penomoran surat yang baru. */
	public void setNomorSurat(NomorSurat nomorSurat) {
		this.nomorSurat = nomorSurat;
	}

	/** @return {@code true} (default) bila pengajuan jenis ini dihitung dalam rekap presensi. */
	public Boolean getMasukPresensi() {
		return masukPresensi == null ? true : masukPresensi;
	}

	/** @param masukPresensi penanda masuk-presensi yang baru. */
	public void setMasukPresensi(Boolean masukPresensi) {
		this.masukPresensi = masukPresensi;
	}

	/** @return {@code true} (default) bila pengajuan jenis ini dihitung sebagai lembur. */
	public Boolean getMasukLembur() {
		return masukLembur == null ? true : masukLembur;
	}

	/** @param masukLembur penanda masuk-lembur yang baru. */
	public void setMasukLembur(Boolean masukLembur) {
		this.masukLembur = masukLembur;
	}

	/** @return {@code false} (default) bila belum diisi; {@code true} berarti berhak konsumsi. */
	public Boolean getDapatKonsumsi() {
		return dapatKonsumsi == null ? false : dapatKonsumsi;
	}

	/** @param dapatKonsumsi penanda berhak-konsumsi yang baru. */
	public void setDapatKonsumsi(Boolean dapatKonsumsi) {
		this.dapatKonsumsi = dapatKonsumsi;
	}


	/**
	 * @return daftar jenis pengguna (peran) yang boleh mengajukan, dipisah koma dan DIBUNGKUS koma
	 *         di kedua ujung (mis. {@code ",dosen,pegawai,"}). Setiap pemanggilan MENIMPA field ini
	 *         dengan versi ternormalisasi: koma ganda dimampatkan berulang, dan variasi hasil yang
	 *         hanya berisi separator (mis. {@code ","}, {@code ",,"}, {@code ",-,"}) diseragamkan
	 *         menjadi string kosong.
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

	/** @param jenisPengguna daftar jenis pengguna baru; akan dinormalkan ulang pada pemanggilan {@link #getJenisPengguna()} berikutnya. */
	public void setJenisPengguna(String jenisPengguna) {
		this.jenisPengguna = jenisPengguna;
	}

	/**
	 * @return daftar username spesifik yang boleh mengajukan, dipisah koma; normalisasi sama
	 *         seperti {@link #getJenisPengguna()}.
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

	/** @param usernamePengguna daftar username spesifik baru; akan dinormalkan ulang pada pemanggilan {@link #getUsernamePengguna()} berikutnya. */
	public void setUsernamePengguna(String usernamePengguna) {
		this.usernamePengguna = usernamePengguna;
	}
}
