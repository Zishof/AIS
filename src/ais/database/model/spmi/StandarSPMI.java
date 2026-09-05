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
 * Entitas Hibernate yang memetakan tabel {@code public.standar_spmi} pada modul
 * SPMI (Sistem Penjaminan Mutu Internal) perguruan tinggi. Merepresentasikan
 * satu <b>standar mutu</b> — mengacu pada 24 Standar SPMI baku Dikti (mis.
 * "STANDAR KOMPETENSI LULUSAN", "STANDAR DOSEN DAN TENAGA KEPENDIDIKAN",
 * "STANDAR PENGELOLAAN PEMBELAJARAN", "STANDAR SARANA DAN PRASARANA", dst. —
 * lihat data contoh yang dibangkitkan oleh {@link JenisSPMI#initDataAmi}) —
 * yang menjadi tolok ukur utama dalam siklus PPEPP (Penetapan-Pelaksanaan-
 * Evaluasi-Pengendalian-Peningkatan).
 *
 * <p><b>Posisi dalam hierarki PPEPP:</b> {@link JenisSPMI} (mis. "Lembar Kerja
 * AMI") &rarr; <b>{@code StandarSPMI}</b> &rarr; {@link ButirMutuSPMI} (butir/
 * kriteria rinci di bawah satu standar) &rarr; {@link IndikatorSPMI} &rarr;
 * {@link SkenarioSPMI} (skenario/bukti audit). Relasi anak ({@code ButirMutuSPMI})
 * tidak dipetakan langsung di sini (unidirectional dari sisi anak via
 * {@link ButirMutuSPMI#getStandarSPMI()}); untuk mendapatkan seluruh butir mutu
 * dari satu standar, kueri {@code ButirMutuSPMI} dengan {@code standarSPMI = ini}.</p>
 *
 * <p>Setiap standar wajib menunjuk ke satu {@link JenisSPMI} induk (kolom
 * {@code jenis_spmi} tidak boleh null) dan memiliki urutan tampil
 * ({@link #getNomorUrut()}) agar tersaji berurutan (mis. "1. STANDAR
 * KOMPETENSI LULUSAN", "2. STANDAR DOSEN...", dst) sesuai penomoran baku
 * standar mutu Dikti. Entitas ini diaudit oleh Hibernate Envers ({@link Audited}),
 * dan seperti seluruh entitas SPMI lain di paket ini, tidak memiliki kolom
 * penanda tenant (perguruan tinggi) sendiri — standar mutu bersifat data
 * master/rujukan yang dibagikan lewat induknya ({@link JenisSPMI}); pemisahan
 * antar-institusi (bila diperlukan) dilakukan di level {@link JenisSPMI} atau
 * di level hasil evaluasi ({@link HasilSPMI}, yang membawa referensi
 * {@code perguruanTinggi} eksplisit).</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "standar_spmi")
public class StandarSPMI extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi tetap untuk kontrak {@link java.io.Serializable}.
	 * Nilai literal ini disalin dari template hbm2java bersama entitas SPMI
	 * lain di paket ini (bukan dihitung ulang per kelas) — konsisten dengan
	 * pola yang sama pada {@link JenisSPMI}, {@link IndikatorSPMI}, dst.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return nilai mentah kolom audit shadow {@code olehId} (identitas pengguna
	 *         yang terakhir menyimpan/mengubah baris ini), atau {@code null}
	 *         bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan identitas pengguna (kolom audit shadow {@code olehId}). Setter
	 * ini sengaja mengabaikan nilai {@code null} atau kosong (guard di baris
	 * pertama) — ini adalah kebutuhan teknis (bukan bug): begitu terisi oleh
	 * interceptor audit, nilai lama tidak boleh tertimpa oleh panggilan
	 * berikutnya yang membawa nilai kosong/null (mis. dari form yang tidak
	 * mengisi ulang field ini saat update).
	 *
	 * @param olehId identitas pengguna; diabaikan bila null/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna (kolom audit shadow {@code oleh}), dengan guard
	 * yang sama seperti {@link #setOlehId(String)} — nilai null/kosong
	 * diabaikan agar tidak menimpa jejak audit yang sudah tercatat.
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
	 *                        otomatis oleh {@link #onUpdate()}, bukan dipanggil
	 *                        manual.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return timestamp terakhir kali baris ini diubah. Diinisialisasi ke
	 *         waktu saat ini ({@link ais.ui.util.WaktuUtil#getDate()}) pada
	 *         saat objek dibuat, dan diperbarui otomatis oleh {@link #onUpdate()}
	 *         setiap kali baris diperbarui di database.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas berupa {@code id + "-" + nama}, dipakai
	 *         untuk keperluan log/debug dan tampilan singkat (mis. label
	 *         combobox), bukan untuk identitas bisnis.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private Integer nomorUrut;
	private JenisSPMI jenisSPMI;
	private String nama;
	private String keterangan;
	private Boolean aktif;

	/** Konstruktor kosong wajib bagi Hibernate untuk membentuk proxy/instance entitas. */
	public StandarSPMI() {
	}

	/**
	 * @return primary key baris ini. Kolom {@code id} bertipe {@code IDENTITY}
	 *         (auto-increment oleh database) dan ditandai {@code insertable = false}
	 *         karena nilainya diserahkan sepenuhnya ke database saat insert,
	 *         bukan dikirim dari aplikasi.
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
	 * @return nama/judul standar mutu ini (mis. "STANDAR KOMPETENSI LULUSAN"),
	 *         di-{@code trim()} terlebih dahulu; {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama/judul standar mutu; wajib diisi (kolom {@code NOT NULL}). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/deskripsi tambahan bagi standar ini; boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi tambahan; opsional. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return urutan tampil standar ini di antara standar-standar lain milik
	 *         {@link JenisSPMI} yang sama (mis. urutan penomoran 24 Standar
	 *         SPMI Dikti). Default {@code 1} bila belum pernah diset.
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/** @param nomorUrut urutan tampil standar ini; lihat {@link #getNomorUrut()}. */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * @return {@code true} bila standar ini masih aktif/berlaku (dipakai dalam
	 *         evaluasi mutu berjalan), {@code false} bila dinonaktifkan (soft
	 *         delete). Default {@code true} bila kolom belum pernah diisi
	 *         (data lama sebelum kolom ini ada) — pola flag aktif "default
	 *         aman" yang konsisten dengan entitas SPMI lain di paket ini.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif standar ini; lihat {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return {@link JenisSPMI} induk (mis. "Lembar Kerja AMI") yang menaungi
	 *         standar ini. Getter memanggil {@link #check(Object)} warisan dari
	 *         {@link GeneralValueObject} untuk menangani kemungkinan proxy
	 *         Hibernate yang stale/terputus dari session sebelum dikembalikan.
	 *         Kolom {@code jenis_spmi} wajib diisi ({@code nullable = false}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_spmi", nullable = false)
	public JenisSPMI getJenisSPMI() {
		jenisSPMI = check(jenisSPMI);
		return jenisSPMI;
	}

	/** @param jenisSPMI {@link JenisSPMI} induk standar ini; lihat {@link #getJenisSPMI()}. */
	public void setJenisSPMI(JenisSPMI jenisSPMI) {
		this.jenisSPMI = jenisSPMI;
	}

}
