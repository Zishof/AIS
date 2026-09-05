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
 * Entitas Hibernate yang memetakan tabel {@code public.butir_mutu_spmi} pada
 * modul SPMI (Sistem Penjaminan Mutu Internal) perguruan tinggi.
 * Merepresentasikan satu <b>butir/kriteria mutu</b> rinci di bawah sebuah
 * standar — pernyataan kewajiban/aturan konkret (mis. "Ketua Sekolah Tinggi
 * memastikan lulusan harus memiliki standar kompetensi yang mengacu pada
 * kemampuan lulusan..." — lihat data contoh pada {@link JenisSPMI#initDataAmi})
 * yang menjabarkan {@link StandarSPMI} induknya menjadi kriteria yang dapat
 * diperiksa satu per satu saat audit.
 *
 * <p><b>Posisi dalam hierarki PPEPP:</b> {@link JenisSPMI} &rarr;
 * {@link StandarSPMI} &rarr; <b>{@code ButirMutuSPMI}</b> &rarr;
 * {@link IndikatorSPMI} (indikator ketercapaian butir ini) &rarr;
 * {@link SkenarioSPMI}. Relasi anak ({@code IndikatorSPMI}) tidak dipetakan
 * langsung di sini; untuk mendapatkan seluruh indikator dari satu butir mutu,
 * kueri {@code IndikatorSPMI} dengan {@code butirMutuSPMI = ini}.</p>
 *
 * <p>Setiap butir mutu wajib menunjuk ke satu {@link StandarSPMI} induk
 * (kolom {@code standar_spmi} tidak boleh null) dan memiliki urutan tampil
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
@Table(schema = "public", name = "butir_mutu_spmi")
public class ButirMutuSPMI extends GeneralValueObject {

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
	private StandarSPMI standarSPMI;
	private String nama;
	private String keterangan;
	private Boolean aktif;

	/** Konstruktor kosong wajib bagi Hibernate untuk membentuk proxy/instance entitas. */
	public ButirMutuSPMI() {
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
	 * @return nama/uraian butir mutu ini (biasanya kalimat kewajiban lengkap,
	 *         mis. dimulai dengan "Ketua Sekolah Tinggi memastikan..."),
	 *         di-{@code trim()} terlebih dahulu; {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama/uraian butir mutu; wajib diisi (kolom {@code NOT NULL}). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/deskripsi tambahan bagi butir mutu ini; boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi tambahan; opsional. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return urutan tampil butir mutu ini di antara butir lain milik
	 *         {@link StandarSPMI} yang sama. Default {@code 1} bila belum
	 *         pernah diset.
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/** @param nomorUrut urutan tampil butir mutu ini; lihat {@link #getNomorUrut()}. */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * @return {@code true} bila butir mutu ini masih aktif/berlaku, {@code false}
	 *         bila dinonaktifkan (soft delete). Default {@code true} bila
	 *         kolom belum pernah diisi — pola flag aktif "default aman" yang
	 *         konsisten dengan entitas SPMI lain di paket ini.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif butir mutu ini; lihat {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return {@link StandarSPMI} induk yang dijabarkan oleh butir mutu ini.
	 *         Getter memanggil {@link #check(Object)} warisan dari
	 *         {@link GeneralValueObject} untuk menangani kemungkinan proxy
	 *         Hibernate yang stale/terputus dari session. Kolom
	 *         {@code standar_spmi} wajib diisi ({@code nullable = false}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "standar_spmi", nullable = false)
	public StandarSPMI getStandarSPMI() {
		standarSPMI = check(standarSPMI);
		return standarSPMI;
	}

	/** @param standarSPMI {@link StandarSPMI} induk butir mutu ini; lihat {@link #getStandarSPMI()}. */
	public void setStandarSPMI(StandarSPMI standarSPMI) {
		this.standarSPMI = standarSPMI;
	}

}
