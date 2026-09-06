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

import org.hibernate.envers.Audited;

/**
 * Entitas Hibernate: baris tautan many-to-many-like antara {@link BiodataCalonMahasiswa} (calon
 * mahasiswa pendaftar) dan {@link VerifikasiKelengkapanCalonMahasiswa} (item verifikasi berkas
 * yang wajib dipenuhi, mis. "Ijazah", "KTP Orang Tua") — dipetakan ke tabel
 * {@code public.biodata_calon_mahasiswa_punya_verifikasi_berkas}. Satu baris = status verifikasi
 * satu jenis berkas untuk satu calon mahasiswa: sudah diunggah ({@link #uploaded}) dan/atau sudah
 * diverifikasi petugas ({@link #verified}), berikut nama berkas dan keterangan bebas.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "biodata_calon_mahasiswa_punya_verifikasi_berkas")
public class BiodataCalonMahasiswaPunyaVerifikasiBerkas extends GeneralValueObject {

	/** ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database). */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key baris, kolom {@code id} (identity, auto-generate). */
	private Long id;
	/** Nama/username aktor yang membuat/terakhir mengubah baris ini (field audit longgar, bukan FK). */
	private String oleh;
	/** ID aktor yang membuat/terakhir mengubah baris ini (pasangan {@link #oleh}, bukan FK). */
	private String olehId;
	/** Stempel waktu "terakhir diubah"; diinisialisasi ke waktu sekarang saat instance dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Calon mahasiswa pemilik berkas ini. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Item/jenis verifikasi kelengkapan yang harus dipenuhi oleh berkas ini. */
	private VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa;
	/** Apakah berkas sudah diverifikasi/disetujui petugas; dinormalisasi ke {@code false} (bukan {@code null}) sebelum simpan. */
	private Boolean verified;
	/** Apakah berkas sudah diunggah oleh calon mahasiswa; dinormalisasi ke {@code false} (bukan {@code null}) sebelum simpan. */
	private Boolean uploaded;
	/** Nama berkas yang diunggah. */
	private String namaFile;
	/** Keterangan bebas untuk baris verifikasi berkas ini. */
	private String keterangan;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public BiodataCalonMahasiswaPunyaVerifikasiBerkas() {
	}

	/**
	 * @return primary key baris ini; {@code null} sebelum baris di-{@code INSERT}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * @param id primary key; biasanya tidak perlu diset manual karena kolomnya {@code
	 *           insertable = false} (identity, dibangkitkan database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return ID aktor ({@link #olehId}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID aktor audit. Setter ini <b>fail-closed diam-diam</b>: nilai {@code null} atau
	 * string kosong/berspasi diabaikan sepenuhnya (nilai lama tetap dipertahankan, lihat {@link
	 * #isBlank(String)}), tanpa exception maupun log. Nilai yang diterima di-{@code trim()}
	 * sebelum disimpan.
	 *
	 * @param olehId ID aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOlehId(String olehId) {
		if (isBlank(olehId)) {
			return;
		}
		this.olehId = olehId.trim();
	}

	/**
	 * @return nama aktor ({@link #oleh}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null}
	 * atau kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan; nilai yang diterima
	 * di-{@code trim()} sebelum disimpan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {
		if (isBlank(oleh)) {
			return;
		}
		this.oleh = oleh.trim();
	}

	/**
	 * Callback JPA {@code @PrePersist}: dipanggil otomatis oleh Hibernate tepat sebelum baris
	 * ini di-{@code INSERT} untuk pertama kali, mendelegasikan ke {@link #normalize()}.
	 *
	 * <p>Berbeda dari beberapa entity lain di cluster ini yang menormalkan/menghapus field
	 * lewat efek samping GETTER (getter-menulis-balik), kelas ini memakai lifecycle callback
	 * JPA yang eksplisit ({@code @PrePersist}/{@code @PreUpdate}) — pola yang lebih aman karena
	 * hanya berjalan tepat sebelum operasi tulis sungguhan, bukan setiap kali field dibaca.</p>
	 */
	@javax.persistence.PrePersist
	protected void onPersist() {
		normalize();
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum {@code
	 * UPDATE} dieksekusi. Selain memperbarui jejak audit "terakhir diubah" lewat {@link
	 * ais.database.hibernate.AuditTimestampInterceptor#ubah}, juga mendelegasikan ke {@link
	 * #normalize()} — lihat catatan pola pada {@link #onPersist()}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
		normalize();
	}

	/** Dipanggil dari {@link #onPersist()}/{@link #onUpdate()}: mengisi {@link #verified}/{@link #uploaded} dengan {@code false} bila {@code null}, dan men-trim {@link #namaFile}/{@link #keterangan}. */
	private void normalize() {
		if (verified == null) {
			verified = Boolean.FALSE;
		}
		if (uploaded == null) {
			uploaded = Boolean.FALSE;
		}
		if (namaFile != null) {
			namaFile = namaFile.trim();
		}
		if (keterangan != null) {
			keterangan = keterangan.trim();
		}
	}

	/**
	 * @param tanggal_dirubah stempel waktu "terakhir diubah" baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu terakhir baris ini diubah (kolom timestamp), diperbarui otomatis
	 *         oleh {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return calon mahasiswa pemilik baris verifikasi berkas ini (proxy lazy diresolusi via
	 *         {@code check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	/**
	 * @param biodataCalonMahasiswa calon mahasiswa pemilik baru; {@code null} untuk melepas tautan.
	 */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * @return item/jenis verifikasi kelengkapan yang harus dipenuhi oleh berkas ini (proxy lazy
	 *         diresolusi via {@code check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "verifikasi_kelengkapan_calon_mahasiswa", nullable = true)
	public VerifikasiKelengkapanCalonMahasiswa getVerifikasiKelengkapanCalonMahasiswa() {
		verifikasiKelengkapanCalonMahasiswa = check(verifikasiKelengkapanCalonMahasiswa);
		return verifikasiKelengkapanCalonMahasiswa;
	}

	/**
	 * @param verifikasiKelengkapanCalonMahasiswa item verifikasi kelengkapan baru; {@code null}
	 *                                            untuk melepas tautan.
	 */
	public void setVerifikasiKelengkapanCalonMahasiswa(
			VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa) {
		this.verifikasiKelengkapanCalonMahasiswa = verifikasiKelengkapanCalonMahasiswa;
	}

	/**
	 * @return keterangan bebas baris ini, di-{@code trim()}; string kosong ({@code ""}) bila
	 *         belum diisi — tidak pernah {@code null}.
	 */
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * @param keterangan keterangan baru; di-trim otomatis oleh {@link #normalize()} sebelum
	 *                   disimpan (bukan langsung di setter ini).
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return {@code true} bila berkas sudah diverifikasi/disetujui petugas; default {@code
	 *         false} bila belum diisi (dinormalkan permanen ke {@code false} saat simpan lewat
	 *         {@link #normalize()}, bukan hanya fallback sesaat pada getter ini).
	 */
	public Boolean getVerified() {
		return verified == null ? Boolean.FALSE : verified;
	}

	/**
	 * @param verified status verifikasi baru.
	 */
	public void setVerified(Boolean verified) {
		this.verified = verified;
	}

	/**
	 * @return {@code true} bila berkas sudah diunggah oleh calon mahasiswa; default {@code
	 *         false} bila belum diisi (dinormalkan permanen ke {@code false} saat simpan lewat
	 *         {@link #normalize()}, bukan hanya fallback sesaat pada getter ini).
	 */
	public Boolean getUploaded() {
		return uploaded == null ? Boolean.FALSE : uploaded;
	}

	/**
	 * @param uploaded status unggah baru.
	 */
	public void setUploaded(Boolean uploaded) {
		this.uploaded = uploaded;
	}

	/**
	 * @return nama berkas yang diunggah, di-{@code trim()}; string kosong ({@code ""}) bila
	 *         belum diisi — tidak pernah {@code null}.
	 */
	public String getNamaFile() {
		return namaFile == null ? "" : namaFile.trim();
	}

	/**
	 * @param namaFile nama berkas baru; di-trim otomatis oleh {@link #normalize()} sebelum
	 *                 disimpan (bukan langsung di setter ini).
	 */
	public void setNamaFile(String namaFile) {
		this.namaFile = namaFile;
	}

	/**
	 * Representasi ringkas untuk log/debug: {@code "<nama calon mahasiswa> - <nama item
	 * verifikasi>"}.
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getVerifikasiKelengkapanCalonMahasiswa()} dan
	 * {@link #getBiodataCalonMahasiswa()}, yang menulis balik field terkait (resolusi proxy
	 * lazy via {@code check()}). Bila salah satu relasi gagal diresolusi (mis. entity sudah
	 * terputus dari sesi Hibernate) exception ditelan dan method jatuh kembali ke {@link
	 * #getNamaFile()} sebagai representasi fallback.</p>
	 *
	 * @return string ringkas identitas baris ini, atau nama berkas saja bila relasi gagal dibaca
	 */
	public String toString() {
		try {
			String namaBerkas = getVerifikasiKelengkapanCalonMahasiswa() == null ? "" : getVerifikasiKelengkapanCalonMahasiswa().getNama();
			String namaCalon = getBiodataCalonMahasiswa() == null ? "" : getBiodataCalonMahasiswa().getNama();
			return (namaCalon + " - " + namaBerkas).trim();
		} catch (Exception e) {
			return getNamaFile();
		}
	}

	/**
	 * @param value string yang diperiksa
	 * @return {@code true} bila {@code value} {@code null} atau hanya berisi spasi/kosong
	 *         setelah di-{@code trim()}.
	 */
	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
