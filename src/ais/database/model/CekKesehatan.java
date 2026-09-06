package ais.database.model;

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

/**
 * Model data untuk satu HASIL PEMERIKSAAN KESEHATAN calon mahasiswa (mis. tes kesehatan PMB):
 * status kesehatan umum ({@link #getSehat()}), hingga 5 slot penyakit yang terdeteksi, tekanan
 * darah, buta warna, hingga 3 slot hasil rontgen, tes narkoba, serta hingga 4 slot kondisi
 * "sehat terbatas" (kondisi kesehatan yang membatasi tapi tidak menggagalkan kelulusan tes).
 * Data kesehatan ini TERGOLONG SENSITIF (data kesehatan pribadi); tidak ditemukan penjagaan
 * akses eksplisit pada model ini -- otorisasi pembacaan/penulisan sepenuhnya menjadi tanggung
 * jawab lapisan Action/service yang memanggilnya. Tipe ini membawa state yang dipertukarkan
 * oleh lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta
 * relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code BiodataCalonMahasiswa biodataCalonMahasiswa}, {@code
 * String status_sehat}, lima slot {@code String penyakit1..5}; pemetaan persistence: tabel
 * {@code public.cek_kesehatan}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code getOleh()},
 * {@code getTanggal_dirubah()}, {@code getBiodataCalonMahasiswa()}, {@code getSehat()}); mutasi data ({@code
 * setOlehId()}, {@code setId()}, {@code setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code
 * setBiodataCalonMahasiswa()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Struktur data slot bernomor:</b> penyakit (1-5), rontgen (1-3), dan sehat-terbatas (1-4) masing-masing
 * dipetakan sebagai kolom TERPISAH bernomor, bukan koleksi/tabel anak -- pola denormalisasi yang sama seperti
 * {@code jenisPekerjaanPenyedia1..5} pada {@code PenyediaAsset} (didokumentasikan di
 * {@link ais.database.model.ParameterTambahanAstract}). Kolom {@code no__urut} bertipe {@code String}
 * (bukan {@code Integer}) meski namanya menyarankan nomor urut.</p>
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
@Table(schema = "public", name = "cek_kesehatan")
public class CekKesehatan extends GeneralValueObject {

	/** Nilai status kesehatan: sehat sepenuhnya. */
	public static String Sehat = "SEHAT";
	/** Nilai status kesehatan: sehat dengan keterbatasan (lihat slot {@code sehatTerbatas1..4}). */
	public static String SehatTerbatas = "SEHAT TERBATAS";
	/** Nilai status kesehatan: sakit/tidak lolos tes kesehatan. */
	public static String Sakit = "SAKIT";
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

	/** @return representasi ringkas untuk debug/log: {@link #getBiodataCalonMahasiswa()} yang dikonversi ke {@code String}. */
	public String toString() {
		return biodataCalonMahasiswa + " ";
	}

	/** Calon mahasiswa subjek pemeriksaan kesehatan ini (wajib). */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Status kesehatan umum; lihat konstanta {@link #Sehat}, {@link #SehatTerbatas}, {@link #Sakit}. */
	private String status_sehat;
	/** Slot penyakit terdeteksi 1. */
	private String penyakit1;
	/** Slot penyakit terdeteksi 2. */
	private String penyakit2;
	/** Slot penyakit terdeteksi 3. */
	private String penyakit3;
	/** Slot penyakit terdeteksi 4. */
	private String penyakit4;
	/** Slot penyakit terdeteksi 5. */
	private String penyakit5;
	/** Hasil pengukuran tekanan darah (teks bebas). */
	private String tekananDarah;
	/** Hasil tes buta warna (teks bebas). */
	private String butaWarna;
	/** Slot hasil rontgen 1. */
	private String rontgen1;
	/** Slot hasil rontgen 2. */
	private String rontgen2;
	/** Slot hasil rontgen 3. */
	private String rontgen3;
	/** Hasil tes narkoba (teks bebas). */
	private String narkoba;
	/** Slot kondisi sehat-terbatas 1. */
	private String sehatTerbatas1;
	/** Slot kondisi sehat-terbatas 2. */
	private String sehatTerbatas2;
	/** Slot kondisi sehat-terbatas 3. */
	private String sehatTerbatas3;
	/** Slot kondisi sehat-terbatas 4. */
	private String sehatTerbatas4;
	/** Nomor urut tampilan (bertipe teks, bukan angka meski namanya menyarankan nomor). */
	private String noUrut;

	/** Konstruktor kosong, dipakai Hibernate. */
	public CekKesehatan() {
	}

	/**
	 * Membangun instance baru untuk satu calon mahasiswa.
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa subjek pemeriksaan.
	 */
	public CekKesehatan(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
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

	/** @return calon mahasiswa subjek pemeriksaan (wajib); dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_mahasiswa", nullable = false)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	/** @param biodataCalonMahasiswa calon mahasiswa subjek pemeriksaan yang baru. */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/** @return status kesehatan umum (lihat {@link #Sehat}/{@link #SehatTerbatas}/{@link #Sakit}), boleh {@code null}. */
	@Column(name = "status_sehat")
	public String getSehat() {
		return status_sehat;
	}

	/** @param sehat status kesehatan umum yang baru. */
	public void setSehat(String sehat) {
		this.status_sehat = sehat;
	}

	/** @return slot penyakit terdeteksi 1, boleh {@code null}. */
	@Column(name = "penyakit_1")
	public String getPenyakit1() {
		return penyakit1;
	}

	/** @param penyakit1 slot penyakit 1 yang baru. */
	public void setPenyakit1(String penyakit1) {
		this.penyakit1 = penyakit1;
	}

	/** @return slot penyakit terdeteksi 2, boleh {@code null}. */
	@Column(name = "penyakit_2")
	public String getPenyakit2() {
		return penyakit2;
	}

	/** @param penyakit2 slot penyakit 2 yang baru. */
	public void setPenyakit2(String penyakit2) {
		this.penyakit2 = penyakit2;
	}

	/** @return slot penyakit terdeteksi 3, boleh {@code null}. */
	@Column(name = "penyakit_3")
	public String getPenyakit3() {
		return penyakit3;
	}

	/** @param penyakit3 slot penyakit 3 yang baru. */
	public void setPenyakit3(String penyakit3) {
		this.penyakit3 = penyakit3;
	}

	/** @return slot penyakit terdeteksi 4, boleh {@code null}. */
	@Column(name = "penyakit_4")
	public String getPenyakit4() {
		return penyakit4;
	}

	/** @param penyakit4 slot penyakit 4 yang baru. */
	public void setPenyakit4(String penyakit4) {
		this.penyakit4 = penyakit4;
	}

	/** @return slot penyakit terdeteksi 5, boleh {@code null}. */
	@Column(name = "penyakit_5")
	public String getPenyakit5() {
		return penyakit5;
	}

	/** @param penyakit5 slot penyakit 5 yang baru. */
	public void setPenyakit5(String penyakit5) {
		this.penyakit5 = penyakit5;
	}

	/** @return hasil pengukuran tekanan darah, boleh {@code null}. */
	@Column(name = "tekanan_darah")
	public String getTekananDarah() {
		return tekananDarah;
	}

	/** @param tekananDarah hasil pengukuran tekanan darah yang baru. */
	public void setTekananDarah(String tekananDarah) {
		this.tekananDarah = tekananDarah;
	}

	/** @return hasil tes buta warna, boleh {@code null}. */
	@Column(name = "buta_warna")
	public String getButaWarna() {
		return butaWarna;
	}

	/** @param butaWarna hasil tes buta warna yang baru. */
	public void setButaWarna(String butaWarna) {
		this.butaWarna = butaWarna;
	}

	/** @return slot hasil rontgen 1, boleh {@code null}. */
	@Column(name = "rontgen_1")
	public String getRontgen1() {
		return rontgen1;
	}

	/** @param rontgen1 slot hasil rontgen 1 yang baru. */
	public void setRontgen1(String rontgen1) {
		this.rontgen1 = rontgen1;
	}

	/** @return slot hasil rontgen 2, boleh {@code null}. */
	@Column(name = "rontgen_2")
	public String getRontgen2() {
		return rontgen2;
	}

	/** @param rontgen2 slot hasil rontgen 2 yang baru. */
	public void setRontgen2(String rontgen2) {
		this.rontgen2 = rontgen2;
	}

	/** @return slot hasil rontgen 3, boleh {@code null}. */
	@Column(name = "rontgen_3")
	public String getRontgen3() {
		return rontgen3;
	}

	/** @param rontgen3 slot hasil rontgen 3 yang baru. */
	public void setRontgen3(String rontgen3) {
		this.rontgen3 = rontgen3;
	}

	/** @return hasil tes narkoba, boleh {@code null}. */
	@Column(name = "narkoba")
	public String getNarkoba() {
		return narkoba;
	}

	/** @param narkoba hasil tes narkoba yang baru. */
	public void setNarkoba(String narkoba) {
		this.narkoba = narkoba;
	}

	/** @return slot kondisi sehat-terbatas 1, boleh {@code null}. */
	@Column(name = "sehat_terbatas_1")
	public String getSehatTerbatas1() {
		return sehatTerbatas1;
	}

	/** @param sehatTerbatas1 slot sehat-terbatas 1 yang baru. */
	public void setSehatTerbatas1(String sehatTerbatas1) {
		this.sehatTerbatas1 = sehatTerbatas1;
	}

	/** @return slot kondisi sehat-terbatas 2, boleh {@code null}. */
	@Column(name = "sehat_terbatas_2")
	public String getSehatTerbatas2() {
		return sehatTerbatas2;
	}

	/** @param sehatTerbatas2 slot sehat-terbatas 2 yang baru. */
	public void setSehatTerbatas2(String sehatTerbatas2) {
		this.sehatTerbatas2 = sehatTerbatas2;
	}

	/** @return slot kondisi sehat-terbatas 3, boleh {@code null}. */
	@Column(name = "sehat_terbatas_3")
	public String getSehatTerbatas3() {
		return sehatTerbatas3;
	}

	/** @param sehatTerbatas3 slot sehat-terbatas 3 yang baru. */
	public void setSehatTerbatas3(String sehatTerbatas3) {
		this.sehatTerbatas3 = sehatTerbatas3;
	}

	/** @return slot kondisi sehat-terbatas 4, boleh {@code null}. */
	@Column(name = "sehat_terbatas_4")
	public String getSehatTerbatas4() {
		return sehatTerbatas4;
	}

	/** @param sehatTerbatas4 slot sehat-terbatas 4 yang baru. */
	public void setSehatTerbatas4(String sehatTerbatas4) {
		this.sehatTerbatas4 = sehatTerbatas4;
	}

	/** @param noUrut nomor urut tampilan (teks) yang baru. */
	public void setNoUrut(String noUrut) {
		this.noUrut = noUrut;
	}

	/** @return nomor urut tampilan (teks, bukan angka), boleh {@code null}. */
	@Column(name = "no__urut")
	public String getNoUrut() {
		return noUrut;
	}

}
