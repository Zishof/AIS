package ais.database.model.sosial;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.ui.util.WaktuUtil;

/**
 * Entitas Hibernate untuk gelombang/batch pendaftaran donatur pada modul sosial legacy AIS —
 * dipetakan ke tabel {@code public.gelombang_donatur}. Merepresentasikan satu periode
 * pendaftaran/perekrutan donatur (mis. "Gelombang 1 - 2026") yang membentang dari
 * {@link #getMulai()} hingga {@link #getSampai()}; setiap {@link Donatur} wajib terikat ke satu
 * gelombang lewat {@link Donatur#getGelombangDonatur()}. Kelas ini murni data master referensi
 * (seperti {@link KategoriProgramDonatur}) tanpa alur persetujuan disposisi SOP — mewarisi
 * {@link GeneralValueObject} langsung, bukan {@link ais.database.model.sop.DataSop}.
 *
 * <h2>Field warisan generik ({@code oleh}/{@code olehId}/{@code tanggal_dirubah})</h2>
 * <p>Field {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta accessor-nya adalah
 * duplikasi tekstual persis dari field bernama sama pada {@link GeneralValueObject} — keharusan
 * teknis peninggalan hbm2java, bukan bug; lihat penjelasan lengkap yang sama di javadoc kelas
 * {@link ProgramDonatur}.</p>
 *
 * @see Donatur
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "gelombang_donatur")
public class GelombangDonatur extends GeneralValueObject {

	/**
	 * Versi serialisasi. Nilai ini disalin apa adanya dari template hbm2java bersama entitas
	 * legacy lain di paket ini (identik dengan {@link ProgramDonatur#serialVersionUID}); jangan
	 * diubah karena baris tersimpan mungkin sudah diserialkan ke cache/session ZK dengan nilai ini.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code gelombang_donatur}. */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini. Duplikasi field audit dari
	 * {@link GeneralValueObject#getOleh()} — lihat catatan kelas di atas.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini. Duplikasi field audit dari
	 * {@link GeneralValueObject#getOlehId()} — lihat catatan kelas di atas.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna pengubah terakhir.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong/spasi diabaikan diam-diam.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi abaikan-nilai-kosong yang sama
	 * seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna pengubah terakhir.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@link javax.persistence.PreUpdate}: menyetel {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini
	 * diperbarui oleh Hibernate.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu pembuatan object. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: {@code "<id>-<nama>"}. Sama seperti {@link ProgramDonatur#toString()};
	 * berbeda dari konvensi {@code "kode - nama"} umum AIS yang dipakai kelas induk
	 * {@link GeneralValueObject#toString()}.
	 *
	 * @return gabungan id dan nama gelombang
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode ringkas gelombang. */
	private String kode;
	/** Tanggal mulai periode gelombang. */
	private Date mulai;
	/** Tanggal berakhir periode gelombang. */
	private Date sampai;
	/** Nama gelombang. Wajib diisi, maksimum 255 karakter. */
	private String nama;
	/** Keterangan bebas mengenai gelombang. */
	private String keterangan;
	/** Penanda status aktif gelombang; {@code null} diperlakukan sebagai aktif. */
	private Boolean aktif;

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
	public GelombangDonatur() {
	}

	/**
	 * Mengembalikan primary key baris {@code gelombang_donatur}.
	 *
	 * @return primary key, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode gelombang, dengan normalisasi: string kosong/{@code null} dinormalkan
	 * menjadi {@code null}, selain itu di-{@code trim()}.
	 *
	 * @return kode gelombang yang sudah di-trim, atau {@code null} bila kosong/belum diisi
	 */
	public String getKode() {
		return kode == null || kode.isEmpty() ? null : kode.trim();
	}

	/**
	 * Menyetel kode gelombang. Tanpa validasi/trim pada setter.
	 *
	 * @param kode kode gelombang baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama gelombang, di-{@code trim()} bila tidak {@code null}.
	 *
	 * @return nama gelombang yang sudah di-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama gelombang. Tanpa validasi/trim pada setter.
	 *
	 * @param nama nama gelombang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan gelombang apa adanya (tanpa trim).
	 *
	 * @return keterangan gelombang, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan gelombang.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif gelombang. Mengikuti pola flag aktif umum AIS: {@code null}
	 * diperlakukan sebagai aktif ("fail open" secara default) — identik dengan
	 * {@link ProgramDonatur#getAktif()}.
	 *
	 * @return {@code true} bila aktif atau belum pernah disetel; {@code false} hanya bila eksplisit
	 *         disetel {@code false}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif gelombang. Tanpa validasi.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan tanggal mulai periode gelombang, dengan fallback: {@code null} dinormalkan
	 * menjadi {@code WaktuUtil.kemarin()} (kemarin) — sehingga getter ini tidak pernah
	 * mengembalikan {@code null}, identik dengan fallback pada
	 * {@link PenyaluranDonasi#getMulai()}.
	 *
	 * @return tanggal mulai, atau kemarin bila belum diisi
	 */
	public Date getMulai() {
		return mulai == null ? WaktuUtil.kemarin() : mulai;
	}

	/**
	 * Menyetel tanggal mulai periode gelombang.
	 *
	 * @param mulai tanggal mulai baru
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan tanggal berakhir periode gelombang, dengan fallback: {@code null}
	 * dinormalkan menjadi {@code WaktuUtil.besok()} (besok) — simetris dengan fallback
	 * {@link #getMulai()}.
	 *
	 * @return tanggal berakhir, atau besok bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai == null ? WaktuUtil.besok() : sampai;
	}

	/**
	 * Menyetel tanggal berakhir periode gelombang.
	 *
	 * @param sampai tanggal berakhir baru
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

}
