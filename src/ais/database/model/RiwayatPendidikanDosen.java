package ais.database.model;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.ConstantValues;

/**
 * Model data untuk riwayat pendidikan dosen. Tipe ini membawa state yang dipertukarkan oleh
 * lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi
 * yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Dosen dosen}, {@code Jenjang jenjangPendidikan}, {@code
 * String namaSekolah}, {@code Kota kota}; pemetaan persistence: tabel {@code public.riwayat_pendidikan_dosen};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()},
 * {@code getDosen()}, {@code getJenjangPendidikan()}); mutasi data ({@code setOlehId()}, {@code setId()}, {@code
 * setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code setDosen()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "riwayat_pendidikan_dosen")

public class RiwayatPendidikanDosen extends GeneralValueObject {

	/** Versi serialisasi Java untuk kompatibilitas {@code Serializable}. */
	private static final long serialVersionUID = 8445312019405120038L;

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

	/** Dosen pemilik riwayat pendidikan ini (wajib). */
	private Dosen dosen;
	/** Jenjang pendidikan (mis. S1/S2/S3) dari riwayat ini (wajib). */
	private Jenjang jenjangPendidikan;
	/** Nama sekolah/perguruan tinggi asal. */
	private String namaSekolah;
	/** Kota asal sekolah/perguruan tinggi (relasi ke master kota), opsional. */
	private Kota kota;
	/** Nama kota dalam teks bebas, dipakai bila {@link #kota} tidak ada di master. */
	private String kotaLain;
	/** Tahun masuk pendidikan. */
	private Integer tahunMasuk;
	/** Tahun keluar/lulus pendidikan. */
	private Integer tahunKeluar;
	/** Nilai akhir/IPK dari jenjang pendidikan ini. */
	private Double nilaiAkhir;
	/** Gelar akademik yang diperoleh (mis. "S.Kom", "M.T"). */
	private String gelarAkademik;
	/** Kode perguruan tinggi asal (kode resmi, mis. PDDIKTI). */
	private String kodePerguruanTinggi;
	/** Bidang ilmu/program studi yang ditempuh. */
	private String bidangIlmu;
	/** Tanggal terbit ijazah. */
	private Date tanggalIjazah;
	/** Negara asal sekolah/perguruan tinggi; lihat {@link #getNegara()} untuk perilaku default. */
	private Negara negara;

	/** @return dosen pemilik riwayat pendidikan ini; dimuat lazy lewat sesi Hibernate aktif. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen", nullable = false)
	public Dosen getDosen() {
		return dosen;
	}

	/** @param dosen dosen pemilik riwayat pendidikan yang baru. */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * @return jenjang pendidikan riwayat ini, di-resolve lewat {@link GeneralValueObject#check(Object)}
	 *         untuk menangani proxy lazy Hibernate.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang_pendidikan", nullable = false)
	public Jenjang getJenjangPendidikan() {
		jenjangPendidikan = check(jenjangPendidikan);
		return jenjangPendidikan;
	}

	/** @param jenjangPendidikan jenjang pendidikan baru. */
	public void setJenjangPendidikan(Jenjang jenjangPendidikan) {
		this.jenjangPendidikan = jenjangPendidikan;
	}

	/** @return nama sekolah/perguruan tinggi asal, boleh {@code null}. */
	@Column(name = "nama_sekolah")
	public String getNamaSekolah() {
		return namaSekolah;
	}

	/** @param namaSekolah nama sekolah/perguruan tinggi baru. */
	public void setNamaSekolah(String namaSekolah) {
		this.namaSekolah = namaSekolah;
	}

	/** @return kota asal sekolah/perguruan tinggi (relasi master), boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kota", nullable = true)
	public Kota getKota() {
		return kota;
	}

	/** @param kota kota asal (relasi master) yang baru. */
	public void setKota(Kota kota) {
		this.kota = kota;
	}

	/** @return tahun masuk pendidikan, boleh {@code null}. */
	@Column(name = "tahun_masuk")
	public Integer getTahunMasuk() {
		return tahunMasuk;
	}

	/** @param tahunMasuk tahun masuk pendidikan baru. */
	public void setTahunMasuk(Integer tahunMasuk) {
		this.tahunMasuk = tahunMasuk;
	}

	/** @return tahun keluar/lulus pendidikan, boleh {@code null}. */
	@Column(name = "tahun_keluar")
	public Integer getTahunKeluar() {
		return tahunKeluar;
	}

	/** @param tahunKeluar tahun keluar/lulus pendidikan baru. */
	public void setTahunKeluar(Integer tahunKeluar) {
		this.tahunKeluar = tahunKeluar;
	}

	/** @return nilai akhir/IPK, boleh {@code null}. */
	@Column(name = "nilai_akhir")
	public Double getNilaiAkhir() {
		return nilaiAkhir;
	}

	/** @param nilaiAkhir nilai akhir/IPK baru. */
	public void setNilaiAkhir(Double nilaiAkhir) {
		this.nilaiAkhir = nilaiAkhir;
	}

	/** @return gelar akademik yang diperoleh, boleh {@code null}. */
	@Column(name = "gelar_akademik")
	public String getGelarAkademik() {
		return gelarAkademik;
	}

	/** @param gelarAkademik gelar akademik baru. */
	public void setGelarAkademik(String gelarAkademik) {
		this.gelarAkademik = gelarAkademik;
	}

	/** @return kode resmi perguruan tinggi asal, boleh {@code null}. */
	@Column(name = "kode_perguruan_tinggi")
	public String getKodePerguruanTinggi() {
		return kodePerguruanTinggi;
	}

	/** @param kodePerguruanTinggi kode resmi perguruan tinggi baru. */
	public void setKodePerguruanTinggi(String kodePerguruanTinggi) {
		this.kodePerguruanTinggi = kodePerguruanTinggi;
	}

	/** @return bidang ilmu/program studi yang ditempuh, boleh {@code null}. */
	@Column(name = "bidang_ilmu")
	public String getBidangIlmu() {
		return bidangIlmu;
	}

	/** @param bidangIlmu bidang ilmu/program studi baru. */
	public void setBidangIlmu(String bidangIlmu) {
		this.bidangIlmu = bidangIlmu;
	}

	/** @return tanggal terbit ijazah, boleh {@code null}. */
	@Column(name = "tanggal_ijazah")
	public Date getTanggalIjazah() {
		return tanggalIjazah;
	}

	/** @param tanggalIjazah tanggal terbit ijazah baru. */
	public void setTanggalIjazah(Date tanggalIjazah) {
		this.tanggalIjazah = tanggalIjazah;
	}

	/** @param kotaLain nama kota (teks bebas) baru, dipakai bila kota tidak ada di master. */
	public void setKotaLain(String kotaLain) {
		this.kotaLain = kotaLain;
	}

	/** @return nama kota (teks bebas) bila {@link #getKota()} tidak ada di master, boleh {@code null}. */
	@Column(name = "kota_lain")
	public String getKotaLain() {
		return kotaLain;
	}

	/** @param negara negara asal sekolah/perguruan tinggi yang baru. */
	public void setNegara(Negara negara) {
		this.negara = negara;
	}

	/** @return negara asal sekolah/perguruan tinggi; default {@link ConstantValues#INDONESIA} bila belum diisi. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "negara", nullable = false)
	public Negara getNegara() {
		return negara == null ? ConstantValues.INDONESIA : negara;
	}

}
