package ais.database.model.surat;

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
 * Entity JPA/Hibernate untuk tabel {@code surat.opsi_surat_keluar}: definisi/header opsi atau
 * checkbox dinamis yang bisa dilampirkan ke surat keluar — dikelola lewat layar master data
 * {@code OpsiSuratKeluarAction}.
 *
 * <p>
 * Ini adalah sisi "header" dari pola header-value bersama {@link OpsiSuratKeluarValue}, paralel
 * persis dengan pasangannya di modul surat masuk ({@link OpsiSuratMasuk}/
 * {@link ais.database.model.surat.OpsiSuratMasukValue}): satu baris {@code OpsiSuratKeluar}
 * mendefinisikan satu opsi yang tersedia secara global, sementara setiap kali opsi tersebut
 * dicentang/dipilih pada surat keluar tertentu direkam sebagai baris {@link OpsiSuratKeluarValue}
 * terpisah yang menunjuk balik ke sini.
 * </p>
 *
 * <p>
 * Field {@link #getJenisPengguna()} dan {@link #getUsernamePengguna()} membatasi visibilitas opsi
 * ini: bila diisi, hanya jenis pengguna (mis. "Pegawai", "Mahasiswa") atau username tertentu dalam
 * daftar itu yang akan melihat opsi ini ditawarkan saat mengisi surat keluar — keduanya disimpan
 * sebagai string CSV dibungkus koma (mis. {@code ",pegawai,dosen,"}) agar pencarian substring
 * {@code LIKE '%,nilai,%'} pada query bisa dipakai tanpa false-positive pada nilai yang beririsan
 * prefix/suffix. Kedua getter melakukan normalisasi CSV tersebut (menambah/menghapus koma
 * pembungkus, membereskan koma ganda) setiap kali dipanggil — <b>efek samping ini termasuk pola
 * getter yang menulis kembali ke field saat dibaca</b>, identik dengan implementasi di
 * {@link OpsiSuratMasuk}. {@code jenisPengguna} diprioritaskan: bila terisi, {@code
 * usernamePengguna} otomatis dianggap kosong ({@code null}) — keduanya tidak dimaksudkan aktif
 * bersamaan.
 * </p>
 *
 * <p>
 * Mewarisi field audit shadow {@code oleh}/{@code olehId}/{@code tanggal_dirubah} dari kerangka
 * entity AIS (lihat {@link GeneralValueObject}); field-field tersebut adalah kebutuhan teknis
 * (integrasi Envers/cache), bukan cacat desain.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "opsi_surat_keluar")
public class OpsiSuratKeluar extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi, dibagi bersama entity AIS lain hasil template hbm2java yang sama;
	 * jangan diubah tanpa memeriksa dampaknya terhadap objek yang sudah terserialisasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengambil ID (username/NIP) shadow pencatat perubahan terakhir.
	 *
	 * @return ID pencatat perubahan terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengeset ID shadow pencatat perubahan. Nilai kosong/blank sengaja diabaikan (silent no-op)
	 * agar nilai lama yang sudah tercatat tidak tertimpa oleh input kosong.
	 *
	 * @param olehId ID pencatat perubahan; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks singkat entity ini, dipakai antara lain oleh komponen ZK agar baris
	 * tampil sebagai nama opsi itu sendiri.
	 *
	 * @return nilai field {@code nama} apa adanya (tanpa trim).
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Mengeset nama shadow pencatat perubahan (audit). Nilai kosong/blank sengaja diabaikan.
	 *
	 * @param oleh nama pencatat perubahan; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama shadow pencatat perubahan terakhir.
	 *
	 * @return nama pencatat perubahan terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/** Callback JPA {@code @PreUpdate}: memperbarui stempel waktu audit sebelum baris di-UPDATE. */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengeset stempel waktu perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah waktu perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil stempel waktu perubahan terakhir. Diinisialisasi ke waktu saat ini saat object
	 * dibuat, dan diperbarui otomatis oleh {@link #onUpdate()} setiap UPDATE lewat Hibernate.
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String nama;
	private String keterangan;
	private Boolean aktif;
	private String usernamePengguna;
	private String jenisPengguna;

	/** Constructor default (dibutuhkan Hibernate). Tidak menginisialisasi field apa pun. */
	public OpsiSuratKeluar() {
	}

	/**
	 * Mengambil primary key baris ini.
	 *
	 * @return ID baris, atau {@code null} untuk entity yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengeset primary key. Normalnya tidak dipanggil manual karena kolom {@code id} bersifat
	 * {@code insertable = false} (auto-generated oleh database via strategi IDENTITY).
	 *
	 * @param id ID baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama/label opsi ini.
	 *
	 * @return nama setelah di-trim, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengeset nama/label opsi.
	 *
	 * @param nama nama baru (belum di-trim; trimming terjadi saat pembacaan via {@link #getNama()}).
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan/deskripsi tambahan untuk opsi ini.
	 *
	 * @return keterangan, bisa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengeset keterangan/deskripsi tambahan.
	 *
	 * @param keterangan keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil status aktif/tidaknya opsi ini sebagai pilihan yang bisa ditawarkan ke pengguna.
	 *
	 * @return {@code true} bila aktif; default {@code true} ketika field belum pernah diset.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengeset status aktif/tidaknya opsi ini.
	 *
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}


	/**
	 * Mengambil daftar jenis pengguna yang berhak melihat/menggunakan opsi ini, sebagai string
	 * CSV yang dibungkus koma di kedua ujungnya (mis. {@code ",pegawai,dosen,"}) — format ini
	 * dipilih agar pencarian substring {@code LIKE '%,<jenis>,%'} pada query aman dari
	 * false-positive antar nilai yang salah satunya adalah prefix/suffix dari yang lain.
	 *
	 * <p>
	 * <b>Perhatian:</b> method ini menormalisasi field {@code jenisPengguna} setiap kali
	 * dipanggil (menambahkan koma pembungkus, membereskan koma ganda berulang) dan menuliskan
	 * hasilnya kembali ke field — efek samping ini terjadi pada operasi baca (getter), bukan hanya
	 * saat disimpan.
	 * </p>
	 *
	 * @return string CSV berkoma-bungkus daftar jenis pengguna, atau {@code null} bila kosong
	 *         setelah normalisasi (opsi berlaku untuk semua jenis pengguna).
	 */
	@Column(name = "jenis_pengguna", nullable = true, columnDefinition = "text")
	public String getJenisPengguna() {

		jenisPengguna = (jenisPengguna == null || jenisPengguna.trim().equalsIgnoreCase(",") ? ""
				: "," + jenisPengguna.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (jenisPengguna.equals(",")) {
			jenisPengguna = "";
		} else if (jenisPengguna.equals(",,")) {
			jenisPengguna = "";
		} else if (jenisPengguna.equals(",,,")) {
			jenisPengguna = "";
		} else if (jenisPengguna.equals(",,,,")) {
			jenisPengguna = "";
		}

		return jenisPengguna == null || jenisPengguna.trim().isEmpty() ? null : jenisPengguna.trim();
	}

	/**
	 * Mengeset daftar jenis pengguna mentah (belum tentu ternormalisasi); normalisasi CSV
	 * dilakukan oleh {@link #getJenisPengguna()} saat pembacaan berikutnya.
	 *
	 * @param jenisPengguna daftar jenis pengguna baru.
	 */
	public void setJenisPengguna(String jenisPengguna) {
		this.jenisPengguna = jenisPengguna;
	}

	/**
	 * Mengambil daftar username spesifik yang berhak melihat/menggunakan opsi ini, dengan format
	 * CSV berkoma-bungkus yang sama seperti {@link #getJenisPengguna()}.
	 *
	 * <p>
	 * Bila {@link #getJenisPengguna()} menghasilkan nilai non-{@code null} (pembatasan berbasis
	 * jenis pengguna sedang aktif), method ini akan memaksa field {@code usernamePengguna}
	 * menjadi {@code null} — kedua mekanisme pembatasan (jenis vs username spesifik) tidak
	 * dimaksudkan aktif bersamaan, dan jenis pengguna diprioritaskan.
	 * </p>
	 *
	 * @return string CSV berkoma-bungkus daftar username, atau {@code null} bila kosong setelah
	 *         normalisasi atau bila pembatasan jenis pengguna sedang aktif.
	 */
	@Column(name = "username_pengguna", nullable = true, columnDefinition = "text")
	public String getUsernamePengguna() {

		usernamePengguna = (usernamePengguna == null || usernamePengguna.trim().equalsIgnoreCase(",") ? ""
				: "," + usernamePengguna.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (usernamePengguna.equals(",")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(",,")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(",,,")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(",,,,")) {
			usernamePengguna = "";
		}

		if (getJenisPengguna() != null) {
			usernamePengguna = null;
		}

		return usernamePengguna == null || usernamePengguna.trim().isEmpty() ? null : usernamePengguna.trim();
	}

	/**
	 * Mengeset daftar username mentah (belum tentu ternormalisasi); normalisasi CSV dan aturan
	 * prioritas terhadap {@code jenisPengguna} dilakukan oleh {@link #getUsernamePengguna()} saat
	 * pembacaan berikutnya.
	 *
	 * @param usernamePengguna daftar username baru.
	 */
	public void setUsernamePengguna(String usernamePengguna) {
		this.usernamePengguna = usernamePengguna;
	}
}
