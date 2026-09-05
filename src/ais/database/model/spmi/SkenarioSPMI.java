package ais.database.model.spmi;

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

import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.skenario_spmi} pada
 * modul SPMI (Sistem Penjaminan Mutu Internal) perguruan tinggi.
 * Merepresentasikan satu <b>skenario/langkah pemeriksaan bukti</b> saat audit
 * mutu internal (AMI) — instruksi konkret bagi auditor tentang apa yang harus
 * diperiksa untuk memverifikasi sebuah indikator (mis. "memeriksa laporan
 * tracer study terkait dengan capaian kompetensi lulusan", "memeriksa
 * kelengkapan ijazah setiap dosen program studi" — lihat data contoh pada
 * {@link JenisSPMI#initDataAmi}).
 *
 * <p><b>Posisi dalam hierarki PPEPP:</b> {@link JenisSPMI} &rarr;
 * {@link StandarSPMI} &rarr; {@link ButirMutuSPMI} &rarr; {@link IndikatorSPMI}
 * &rarr; <b>{@code SkenarioSPMI}</b> — simpul paling bawah dari hierarki
 * standar/master data. Skenario inilah yang menjadi acuan saat auditor mencatat
 * hasil pemeriksaan sesungguhnya: setiap {@link HasilTemuanSPMI} (temuan hasil
 * evaluasi/audit, fase Evaluasi PPEPP) menunjuk balik ke satu skenario di sini
 * lewat {@link HasilTemuanSPMI#getSkenarioSPMI()}, sehingga temuan dapat
 * ditelusuri kembali ke seluruh rantai standar-butir-indikator-skenario yang
 * diperiksanya.</p>
 *
 * <p>Setiap skenario wajib menunjuk ke satu {@link IndikatorSPMI} induk
 * (kolom {@code indikator_spmi} tidak boleh null) dan memiliki urutan tampil
 * ({@link #getNomorUrut()}). Seperti entitas SPMI lain di paket ini, tidak
 * memiliki kolom tenant sendiri — data master dibagikan lewat rantai induknya
 * hingga ke {@link JenisSPMI}. Diaudit oleh Hibernate Envers ({@link Audited}).</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "skenario_spmi")
public class SkenarioSPMI extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi tetap untuk kontrak {@link java.io.Serializable}.
	 * Nilai literal ini disalin dari template hbm2java bersama entitas SPMI
	 * lain di paket ini (bukan dihitung ulang per kelas).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return nilai mentah kolom audit shadow {@code olehId} (identitas
	 *         pengguna yang terakhir menyimpan/mengubah baris ini), atau
	 *         {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan identitas pengguna (kolom audit shadow {@code olehId}). Setter
	 * ini sengaja mengabaikan nilai {@code null} atau kosong (guard di baris
	 * pertama) — kebutuhan teknis (bukan bug): nilai yang sudah tercatat oleh
	 * interceptor audit tidak boleh tertimpa oleh panggilan berikutnya yang
	 * membawa nilai kosong/null.
	 *
	 * @param olehId identitas pengguna; diabaikan bila null/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna (kolom audit shadow {@code oleh}), dengan guard
	 * yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila null/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang tercatat pada kolom audit shadow {@code oleh},
	 *         atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence
	 * sesaat sebelum baris ini di-{@code UPDATE}, mendelegasikan pencatatan
	 * timestamp perubahan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 * Bukan API publik — tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah timestamp perubahan terakhir; biasanya diisi
	 *                        otomatis oleh {@link #onUpdate()}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return timestamp terakhir kali baris ini diubah, diinisialisasi ke
	 *         waktu saat objek dibuat dan diperbarui otomatis oleh
	 *         {@link #onUpdate()} saat baris diperbarui di database.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas berupa {@code id + "-" + nama}, dipakai
	 *         untuk log/debug dan tampilan singkat, bukan identitas bisnis.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private Integer nomorUrut;
	private IndikatorSPMI indikatorSPMI;
	private String nama;
	private String keterangan;
	private Boolean aktif;

	/** Konstruktor kosong wajib bagi Hibernate untuk membentuk proxy/instance entitas. */
	public SkenarioSPMI() {
	}

	/**
	 * @return primary key baris ini. Kolom {@code id} bertipe {@code IDENTITY}
	 *         (auto-increment oleh database) dan ditandai {@code insertable = false}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key; jarang dipanggil manual karena {@code id} adalah IDENTITY. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama/uraian skenario pemeriksaan ini (mis. "memeriksa
	 *         ketersediaan dokumen Pedoman Penilaian Kompetensi Lulusan"),
	 *         di-{@code trim()} terlebih dahulu; {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama/uraian skenario; wajib diisi (kolom {@code NOT NULL}). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/deskripsi tambahan bagi skenario ini; boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi tambahan; opsional. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return urutan tampil skenario ini di antara skenario lain milik
	 *         {@link IndikatorSPMI} yang sama. Default {@code 1} bila belum
	 *         pernah diset.
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/** @param nomorUrut urutan tampil skenario ini; lihat {@link #getNomorUrut()}. */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * @return {@code true} bila skenario ini masih aktif/berlaku (dipakai
	 *         dalam audit berjalan), {@code false} bila dinonaktifkan (soft
	 *         delete). Default {@code true} bila kolom belum pernah diisi —
	 *         pola flag aktif "default aman" yang konsisten dengan entitas
	 *         SPMI lain di paket ini.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif skenario ini; lihat {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return {@link IndikatorSPMI} induk yang diverifikasi oleh skenario ini.
	 *         Getter memanggil {@link #check(Object)} warisan dari
	 *         {@link GeneralValueObject} untuk menangani kemungkinan proxy
	 *         Hibernate yang stale/terputus dari session. Kolom
	 *         {@code indikator_spmi} wajib diisi ({@code nullable = false}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "indikator_spmi", nullable = false)
	public IndikatorSPMI getIndikatorSPMI() {
		indikatorSPMI = check(indikatorSPMI);
		return indikatorSPMI;
	}

	/** @param indikatorSPMI {@link IndikatorSPMI} induk skenario ini; lihat {@link #getIndikatorSPMI()}. */
	public void setIndikatorSPMI(IndikatorSPMI indikatorSPMI) {
		this.indikatorSPMI = indikatorSPMI;
	}

}
