package ais.database.model.payroll;

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

/**
 * Master data "Cabang" (unit organisasi/lokasi kerja) pada modul payroll AIS — dimensi tempat
 * seorang pegawai terdaftar, dipakai bersama {@code Departemen}, {@code LevelJabatan}, dsb. sebagai
 * atribut struktur organisasi pegawai. Entity ini <b>hidup dan dipakai luas</b>, bukan entity
 * dorman: relasi utamanya ada di {@code Pegawai.cabang} ({@code @ManyToOne}, lihat
 * {@code Pegawai#getCabang()}), diseleksi lewat picker {@code CabangPunyaPegawaiAction} dan
 * {@code AmbilDataPegawaiBanyak}, dipakai sebagai filter/kolom pada laporan payroll (mis.
 * {@code GajiPegawaiAction}, {@code GajiTabahanAction}, {@code FormatItemGajiAction}), dikelola
 * lewat CRUD-nya sendiri di {@code CabangAction}, serta terdaftar sebagai salah satu entity yang
 * di-seed otomatis oleh {@code InitData} (bersama {@code Bank}, {@code Departemen},
 * {@code FormatItemGaji}, dst.) — lihat masing-masing kelas tersebut untuk detail pemakaian.
 *
 * <p>Bentuknya adalah master data generik (kode/nama/keterangan) tanpa kolom lain: tidak ada relasi
 * balik eksplisit ke induk organisasi lain (mis. satuan kerja) di kelas ini sendiri — pemetaan
 * pegawai ke cabang sepenuhnya ditentukan oleh field {@code Pegawai.cabang} di sisi entity pegawai,
 * bukan sebaliknya. Entity ini tidak memuat filter satuan kerja/tenant sendiri; siapa saja yang
 * memiliki akses ke layar {@code CabangAction} dapat melihat/mengelola seluruh baris cabang lintas
 * satuan kerja/tenant, sama seperti master data payroll sejenis lainnya di modul ini.</p>
 *
 * <h2>Field audit bawaan {@code GeneralValueObject}</h2>
 * <p>Field {@link #oleh}, {@link #olehId}, dan {@code tanggal_dirubah} di kelas ini adalah
 * <b>redeklarasi lokal</b> (shadow) atas field privat bernama sama pada superclass
 * {@link GeneralValueObject} — pola berulang di banyak entity AIS (lihat catatan arsitektur
 * "field audit shadow"), diperlukan secara teknis karena field induk bersifat {@code private}
 * sehingga tidak bisa diwarisi langsung, namun getter/setter di sini tetap menyediakan kontrak
 * publik yang sama (termasuk validasi non-trivial pada {@link #setOleh(String)}/
 * {@link #setOlehId(String)} yang mengabaikan nilai kosong/{@code null} secara diam-diam, identik
 * dengan perilaku induknya).</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "cabang")
public class Cabang extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris cabang. Lihat {@link #getId()}. */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini. Redeklarasi lokal (shadow) atas field privat
	 * sejenis pada {@link GeneralValueObject} — lihat catatan kelas.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini. Redeklarasi lokal (shadow) atas field privat
	 * sejenis pada {@link GeneralValueObject} — lihat catatan kelas.
	 */
	private String olehId;
	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris cabang ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {return olehId;}
	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null} atau string kosong/spasi diabaikan
	 * secara diam-diam (method langsung {@code return} tanpa mengubah apa pun) — perilaku ini
	 * disengaja agar jejak audit yang sudah terisi tidak terhapus oleh jalur simpan yang kebetulan
	 * tidak membawa informasi pengguna (mis. proses batch tanpa sesi login). Sama persis dengan
	 * kontrak {@link GeneralValueObject#setOlehId(String)}.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}


	/**
	 * Representasi teks baris ini untuk komponen ZK (isi {@code Combobox}/{@code Listcell}/label
	 * bandbox saat memilih cabang), berupa {@link #nama} apa adanya. Berbeda dari
	 * {@link GeneralValueObject#toString()} (format {@code "kode - nama"}): entity ini tidak
	 * memetakan {@code kode}, sehingga override ini menghindari awalan {@code "null - "}.
	 *
	 * @return nilai {@link #nama}, boleh {@code null} bila belum pernah disetel
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam. Sama persis dengan
	 * kontrak {@link GeneralValueObject#setOleh(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris cabang ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook {@code @PreUpdate} wajib dari {@link GeneralValueObject}: memanggil
	 * {@code AuditTimestampInterceptor.ubah(this)} untuk memutakhirkan {@link #tanggal_dirubah}
	 * setiap kali baris ini diperbarui lewat Hibernate. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /**
	 * Stempel waktu perubahan terakhir baris ini. Redeklarasi lokal (shadow) atas field privat
	 * sejenis pada {@link GeneralValueObject}; diinisialisasi ke waktu pembuatan object memakai
	 * {@code WaktuUtil.getDate()} sehingga baris baru selalu punya nilai walau jalur simpan lupa
	 * mengisinya.
	 */
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
	 * Mengembalikan stempel waktu perubahan terakhir. Dipetakan sebagai {@code TIMESTAMP} sehingga
	 * bagian jam ikut tersimpan.
	 *
	 * @return waktu perubahan terakhir baris ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama cabang. Kolom wajib diisi; bagian ini yang tampil pada {@link #toString()}. */
	private String nama;
	/** Keterangan bebas tentang cabang ini. */
	private String keterangan;

	/**
	 * Constructor default tanpa argumen, wajib ada agar Hibernate dapat membuat instance saat
	 * hidrasi baris dari hasil query.
	 */
	public Cabang() {
	}

	/**
	 * Mengembalikan primary key baris cabang ini.
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
	 * Menyetel primary key baris ini. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama cabang.
	 *
	 * @return nama cabang, tidak {@code null} pada baris yang tersimpan (kolom wajib)
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama cabang. Tanpa validasi.
	 *
	 * @param nama nama cabang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas tentang cabang ini.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas tentang cabang ini. Tanpa validasi.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
