package ais.database.model.library;

// Vendor yang dinilai dalam Survey Pemilihan Penilaian Vendor (Data Vendor - Lampiran 1.1 bag. A).

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import ais.database.model.GeneralValueObject;

/**
 * Entitas <b>vendor yang dinilai</b> (tabel {@code library.survey_vendor_vendor},
 * "Data Vendor - Lampiran 1.1 bag. A") dalam satu {@link SurveyVendor} — satu baris mewakili satu
 * vendor pembanding (I/II/III/...), opsional ditautkan ke master data {@link Penyedia} lewat
 * {@link #getPenyedia()}.
 *
 * <h2>Data manual boleh menimpang dari {@link Penyedia}</h2>
 * <p>Sama seperti {@link SeleksiVendorDetail}, {@link #getNamaVendor()}, {@link #getAlamatKontak()},
 * dan {@link #getPicVendor()} masing-masing memakai nilai manual tersimpan bila terisi, dan hanya
 * jatuh ke data {@link #getPenyedia()} sebagai bawaan bila kolom manualnya kosong — vendor pada
 * survei tidak wajib sudah terdaftar sebagai {@link Penyedia}.</p>
 *
 * <h2>Gerbang kualifikasi opsional (P1)</h2>
 * <p>{@link #getLulusQualification()} menandai apakah vendor ini lulus tahap kualifikasi awal
 * (bawaan {@code true}/lulus) sebelum dinilai skornya lewat
 * {@link SurveyVendorPenilaianDetail#getVendor()}. Gerbang ini hanya relevan bila header survei
 * mengaktifkannya lewat {@link SurveyVendor#getPakaiQualification()}; penegakannya (menolak
 * penilaian atas vendor yang gagal kualifikasi) berada di {@code SurveyVendorAction}, bukan pada
 * validasi entitas ini.</p>
 *
 * @see SurveyVendor
 * @see Penyedia
 * @see SurveyVendorPenilaianDetail
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "library", name = "survey_vendor_vendor")
public class SurveyVendorVendor extends GeneralValueObject {

	/** Penanda versi serialisasi Java. */
	private static final long serialVersionUID = 7720145511001000002L;

	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris vendor survei ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila belum pernah diubah lewat jalur yang
	 *         memasang interceptor audit
	 */
	public String getOlehId() { return olehId; }
	/**
	 * Menyetel id pengguna yang terakhir mengubah baris vendor survei ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) { return; } this.olehId = olehId; }
	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris vendor survei ini.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) { return; } this.oleh = oleh; }
	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris vendor survei ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() { return oleh; }

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui. Method sengaja
	 * {@code protected} dan tidak boleh dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya dipanggil {@code AuditTimestampInterceptor}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris vendor survei ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek baru di memori
	 */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }

	/**
	 * Representasi teks singkat: nama vendor, memakai logika fallback {@link #getNamaVendor()}.
	 *
	 * @return nama vendor (manual atau fallback {@link Penyedia}); dapat {@code null}
	 */
	public String toString() { return getNamaVendor(); }

	/** Header survei tempat vendor ini dinilai. */
	private SurveyVendor surveyVendor;
	/** Vendor master data yang ditunjuk (opsional) — lihat javadoc kelas soal data manual. */
	private Penyedia penyedia;
	/** Nomor urut vendor pada survei (Vendor I/II/III &rarr; 1,2,3). */
	private Integer urutan;
	/** Nama vendor, isian manual; jatuh ke {@link Penyedia#getNama()} bila kosong — lihat {@link #getNamaVendor()}. */
	private String namaVendor;
	/** Alamat+kontak vendor, isian manual; jatuh ke gabungan alamat/telp {@link Penyedia} bila kosong. */
	private String alamatKontak;
	/** Jenis barang/jasa yang ditawarkan vendor ini. */
	private String jenisBarangJasa;
	/** PIC vendor, isian manual; jatuh ke {@link Penyedia#getKontak()} bila kosong — lihat {@link #getPicVendor()}. */
	private String picVendor;
	/** Penanda vendor lulus tahap kualifikasi P1 (bawaan {@code true}/lulus) — lihat javadoc kelas. */
	private Boolean lulusQualification;   // P1: gerbang kualifikasi (default lulus)

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public SurveyVendorVendor() {}

	/**
	 * Mengembalikan kunci utama baris vendor survei ini.
	 *
	 * @return id, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/**
	 * Menyetel kunci utama baris vendor survei ini. Hanya untuk kebutuhan Hibernate/penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) { this.id = id; }

	/**
	 * Mengembalikan header survei tempat vendor ini dinilai.
	 *
	 * <p>Tidak memanggil {@code check(...)} karena relasi ini tidak dinyatakan {@code LAZY} pada
	 * anotasi (bawaan JPA untuk {@code @ManyToOne} adalah <i>eager</i>).</p>
	 *
	 * @return header survei; boleh {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "survey_vendor", nullable = true)
	public SurveyVendor getSurveyVendor() { return surveyVendor; }
	/**
	 * Menyetel header survei tempat vendor ini dinilai.
	 *
	 * @param v header survei; boleh {@code null}
	 */
	public void setSurveyVendor(SurveyVendor v) { this.surveyVendor = v; }

	/**
	 * Mengembalikan vendor master data yang ditunjuk, setelah proksi malasnya diselesaikan
	 * {@code check(...)}.
	 *
	 * @return vendor master data, atau {@code null} bila vendor ini murni data manual
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penyedia", nullable = true)
	public Penyedia getPenyedia() { penyedia = check(penyedia); return penyedia; }
	/**
	 * Menyetel vendor master data yang ditunjuk.
	 *
	 * @param penyedia vendor master data; boleh {@code null}
	 */
	public void setPenyedia(Penyedia penyedia) { this.penyedia = penyedia; }

	/**
	 * Mengembalikan nomor urut vendor pada survei, dengan bawaan 0 bila belum diisi.
	 *
	 * @return nomor urut; tidak pernah {@code null}
	 */
	@Column(name = "urutan") public Integer getUrutan() { return urutan == null ? 0 : urutan; }
	/**
	 * Menyetel nomor urut vendor pada survei.
	 *
	 * @param urutan nomor urut; boleh {@code null} untuk kembali ke bawaan 0
	 */
	public void setUrutan(Integer urutan) { this.urutan = urutan; }

	/**
	 * Mengembalikan nama vendor, dengan fallback ke {@link Penyedia#getNama()} bila kolom manual
	 * kosong dan {@link #getPenyedia()} tidak {@code null}. Method bebas efek samping — fallback
	 * tidak ditulis kembali ke bidang {@link #namaVendor}.
	 *
	 * @return nama vendor manual bila terisi; bila tidak, nama {@link Penyedia} bila tertaut;
	 *         atau {@code null} bila keduanya kosong
	 */
	@Column(name = "nama_vendor", length = 255)
	public String getNamaVendor() {
		if ((namaVendor == null || namaVendor.trim().isEmpty()) && getPenyedia() != null) { return getPenyedia().getNama(); }
		return namaVendor;
	}
	/**
	 * Menyetel nama vendor secara manual, menimpang fallback ke {@link Penyedia}.
	 *
	 * @param v nama vendor manual; boleh {@code null}/kosong untuk mengaktifkan fallback
	 */
	public void setNamaVendor(String v) { this.namaVendor = v; }

	/**
	 * Mengembalikan alamat dan kontak vendor, dengan fallback ke gabungan alamat dan telepon
	 * {@link Penyedia} (dipisah {@code " / "}) bila kolom manual kosong. Mengembalikan
	 * {@code null} (bukan string kosong) bila hasil gabungan kosong.
	 *
	 * @return alamat+kontak manual bila terisi; bila tidak, gabungan alamat/telepon
	 *         {@link Penyedia} bila tertaut dan tidak keduanya kosong; atau {@code null}
	 */
	@Column(name = "alamat_kontak")
	public String getAlamatKontak() {
		if ((alamatKontak == null || alamatKontak.trim().isEmpty()) && getPenyedia() != null) {
			String a = getPenyedia().getAlamat();
			String t = getPenyedia().getTelp();
			String gabung = (a == null ? "" : a) + (t == null || t.trim().isEmpty() ? "" : " / " + t);
			return gabung.trim().isEmpty() ? null : gabung.trim();
		}
		return alamatKontak;
	}
	/**
	 * Menyetel alamat dan kontak vendor secara manual, menimpang fallback ke {@link Penyedia}.
	 *
	 * @param v alamat+kontak manual; boleh {@code null}/kosong untuk mengaktifkan fallback
	 */
	public void setAlamatKontak(String v) { this.alamatKontak = v; }

	/**
	 * Mengembalikan jenis barang/jasa yang ditawarkan vendor ini. Tidak ada fallback ke
	 * {@link Penyedia} — kolom ini murni isian manual per survei.
	 *
	 * @return jenis barang/jasa; boleh {@code null}
	 */
	@Column(name = "jenis_barang_jasa") public String getJenisBarangJasa() { return jenisBarangJasa; }
	/**
	 * Menyetel jenis barang/jasa yang ditawarkan vendor ini.
	 *
	 * @param v jenis barang/jasa; boleh {@code null}
	 */
	public void setJenisBarangJasa(String v) { this.jenisBarangJasa = v; }

	/**
	 * Mengembalikan nama PIC vendor, dengan fallback ke {@link Penyedia#getKontak()} bila kolom
	 * manual kosong.
	 *
	 * @return PIC manual bila terisi; bila tidak, kontak {@link Penyedia} bila tertaut; atau
	 *         {@code null} bila keduanya kosong
	 */
	@Column(name = "pic_vendor")
	public String getPicVendor() {
		if ((picVendor == null || picVendor.trim().isEmpty()) && getPenyedia() != null) { return getPenyedia().getKontak(); }
		return picVendor;
	}
	/**
	 * Menyetel nama PIC vendor secara manual, menimpang fallback ke {@link Penyedia}.
	 *
	 * @param v PIC manual; boleh {@code null}/kosong untuk mengaktifkan fallback
	 */
	public void setPicVendor(String v) { this.picVendor = v; }

	/**
	 * Mengembalikan penanda kelulusan tahap kualifikasi (P1), dengan {@code null} dinormalkan
	 * menjadi {@code true} (bawaan lulus).
	 *
	 * <p>Hanya relevan bila header survei ({@link SurveyVendor#getPakaiQualification()})
	 * mengaktifkan gerbang kualifikasi; penegakannya berada di {@code SurveyVendorAction}.</p>
	 *
	 * @return {@code true} bila vendor lulus/dianggap lulus kualifikasi; {@code false} bila
	 *         eksplisit ditandai gagal
	 */
	@Column(name = "lulus_qualification") public Boolean getLulusQualification() { return lulusQualification == null || lulusQualification; }
	/**
	 * Menyetel penanda kelulusan tahap kualifikasi vendor ini.
	 *
	 * @param v {@code false} untuk menandai vendor gagal kualifikasi; boleh {@code null} untuk
	 *          kembali ke bawaan lulus
	 */
	public void setLulusQualification(Boolean v) { this.lulusQualification = v; }
}
