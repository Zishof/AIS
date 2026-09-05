package ais.database.model.sosial;

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

import ais.common.ConstantValues;
import ais.database.model.GeneralValueObject;
import ais.database.model.Negara;

/**
 * Entitas Hibernate untuk data donatur/pemberi dana pada modul sosial legacy AIS — dipetakan ke
 * tabel {@code public.donatur}. Berbeda dari {@link ProgramDonatur} dan {@link PenyaluranDonasi},
 * kelas ini <b>tidak</b> mewarisi {@link ais.database.model.sop.DataSop} (langsung mewarisi
 * {@link GeneralValueObject}) — donatur tidak punya alur persetujuan disposisi SOP sendiri; ia
 * murni data master pihak pemberi dana.
 *
 * <h2>Relasi tanpa foreign key ke program</h2>
 * <p>Meski {@link ProgramDonatur} membawa daftar donatur pesertanya lewat
 * {@link ProgramDonatur#getDonaturs()}, relasi itu SATU ARAH dan TIDAK diwujudkan sebagai foreign
 * key dari sisi kelas ini — {@code Donatur} tidak tahu program mana yang mengaitkannya; keterkaitan
 * hanya dapat ditelusuri dengan mencari program yang string {@code donaturs}-nya memuat id baris
 * ini. Satu-satunya relasi terpetakan yang benar-benar dimiliki kelas ini adalah ke
 * {@link GelombangDonatur} (lewat {@link #getGelombangDonatur()}) dan ke {@link Negara} (lewat
 * {@link #getNegara()}).</p>
 *
 * <h2>Field warisan generik ({@code oleh}/{@code olehId}/{@code tanggal_dirubah})</h2>
 * <p>Field {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta accessor-nya adalah
 * duplikasi tekstual persis dari field bernama sama pada {@link GeneralValueObject} — keharusan
 * teknis peninggalan hbm2java (tiap entity men-declare ulang field auditnya sendiri), bukan bug;
 * lihat penjelasan lengkap yang sama di javadoc kelas {@link ProgramDonatur}.</p>
 *
 * @see ProgramDonatur
 * @see GelombangDonatur
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "donatur")
public class Donatur extends GeneralValueObject {

	/**
	 * Versi serialisasi. Nilai ini disalin apa adanya dari template hbm2java bersama entitas
	 * legacy lain di paket ini (identik dengan {@link ProgramDonatur#serialVersionUID}); jangan
	 * diubah karena baris tersimpan mungkin sudah diserialkan ke cache/session ZK dengan nilai ini.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code donatur}. */
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
	 * @return gabungan id dan nama donatur
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode ringkas donatur. */
	private String kode;
	/** Gelombang/batch pendaftaran donatur ini. Wajib diisi. */
	private GelombangDonatur gelombangDonatur;
	/** Nama donatur. Wajib diisi, maksimum 255 karakter. */
	private String nama;
	/** Keterangan bebas mengenai donatur. */
	private String keterangan;
	/** Negara asal donatur. Wajib diisi; lihat fallback default pada {@link #getNegara()}. */
	private Negara negara;
	/** Alamat surel donatur. */
	private String email;
	/** Alamat lengkap donatur. */
	private String alamat;
	/** Nomor telepon donatur. */
	private String telp;
	/** Penanda status aktif donatur; {@code null} diperlakukan sebagai aktif. */
	private Boolean aktif;

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
	public Donatur() {
	}

	/**
	 * Mengembalikan primary key baris {@code donatur}.
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
	 * Mengembalikan kode donatur, dengan normalisasi: string kosong/{@code null} dinormalkan
	 * menjadi {@code null}, selain itu di-{@code trim()}.
	 *
	 * @return kode donatur yang sudah di-trim, atau {@code null} bila kosong/belum diisi
	 */
	public String getKode() {
		return kode == null || kode.isEmpty() ? null : kode.trim();
	}

	/**
	 * Menyetel kode donatur. Tanpa validasi/trim pada setter.
	 *
	 * @param kode kode donatur baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama donatur, di-{@code trim()} bila tidak {@code null}.
	 *
	 * @return nama donatur yang sudah di-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama donatur. Tanpa validasi/trim pada setter.
	 *
	 * @param nama nama donatur baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan donatur apa adanya (tanpa trim).
	 *
	 * @return keterangan donatur, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan donatur.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif donatur. Mengikuti pola flag aktif umum AIS: {@code null}
	 * diperlakukan sebagai aktif ("fail open" secara default) — identik dengan
	 * {@link ProgramDonatur#getAktif()}. Filter aktif ini yang dipakai
	 * {@code ProgramDonaturAction}/{@code PenyaluranDonasiAction} saat memuat daftar donatur untuk
	 * ditampilkan pada program ({@code Restrictions.or(Restrictions.isNull("aktif"),
	 * Restrictions.eq("aktif", true))} — query yang setara secara semantik dengan getter ini).
	 *
	 * @return {@code true} bila aktif atau belum pernah disetel; {@code false} hanya bila eksplisit
	 *         disetel {@code false}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif donatur. Tanpa validasi.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan negara asal donatur, meresolusi proxy lazy lewat {@code check()}, dengan
	 * fallback: bila hasilnya {@code null} DAN {@link ais.common.ConstantValues#INDONESIA} tidak
	 * {@code null}, dikembalikan Indonesia sebagai negara default — mencerminkan mayoritas donatur
	 * AIS yang berlokasi di Indonesia sehingga field ini jarang perlu diisi eksplisit oleh operator.
	 *
	 * @return negara donatur, atau Indonesia sebagai fallback bila belum diisi dan konstanta negara
	 *         tersedia
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "negara", nullable = false)
	public Negara getNegara() {
		negara = check(negara);
		if (negara == null && ConstantValues.INDONESIA != null) {
			negara = ConstantValues.INDONESIA;
		}
		return negara;
	}

	/**
	 * Menyetel negara asal donatur. Tanpa validasi.
	 *
	 * @param negara negara baru
	 */
	public void setNegara(Negara negara) {
		this.negara = negara;
	}

	/**
	 * Mengembalikan alamat surel donatur apa adanya. Tidak dipetakan lewat anotasi
	 * {@code @Column} eksplisit (memakai penamaan default Hibernate); tidak ada validasi format
	 * email pada level entitas ini.
	 *
	 * @return alamat surel, boleh {@code null}
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Menyetel alamat surel donatur. Tanpa validasi.
	 *
	 * @param email alamat surel baru
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Mengembalikan nomor telepon donatur apa adanya.
	 *
	 * @return nomor telepon, boleh {@code null}
	 */
	public String getTelp() {
		return telp;
	}

	/**
	 * Menyetel nomor telepon donatur. Tanpa validasi format.
	 *
	 * @param telp nomor telepon baru
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Mengembalikan alamat lengkap donatur apa adanya.
	 *
	 * @return alamat, boleh {@code null}
	 */
	@Column(name = "alamat", columnDefinition = "text", nullable = true)
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Menyetel alamat lengkap donatur.
	 *
	 * @param alamat alamat baru
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan gelombang/batch pendaftaran donatur ini, meresolusi proxy lazy lewat
	 * {@code check()}.
	 *
	 * @return gelombang donatur, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_donatur", nullable = false)
	public GelombangDonatur getGelombangDonatur() {
		gelombangDonatur = check(gelombangDonatur);
		return gelombangDonatur;
	}

	/**
	 * Menyetel gelombang/batch pendaftaran donatur. Tanpa validasi.
	 *
	 * @param gelombangDonatur gelombang donatur baru
	 */
	public void setGelombangDonatur(GelombangDonatur gelombangDonatur) {
		this.gelombangDonatur = gelombangDonatur;
	}

}
