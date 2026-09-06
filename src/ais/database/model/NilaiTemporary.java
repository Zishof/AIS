/* Copyright (C) Syarif Hidayatullah State Islamic University Jakarta, Inc - All Rights Reserved
* Unauthorized copying of this file, via any medium is strictly prohibited
* Proprietary and confidential
* Written by PUSTIPANDA <pustipanda@uinjkt.ac.id>, December 2015
*/

package ais.database.model;

// Generated Dec 12, 2009 7:42:38 PM by Hibernate Tools 3.2.4.CR1

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
import org.hibernate.annotations.ForeignKey;
import org.hibernate.envers.Audited;

/**
 * Baris "staging"/antrean nilai (tabel {@code public.nilai}) — SETIAP baris menyatakan satu komponen
 * nilai ({@link #getJumlah()}, mengacu ke {@link #getFormatNilai()}) yang MASIH MENUNGGU untuk
 * dipindahkan ke kolom nilai permanen pada {@link Detailperkuliahan} terkait.
 *
 * <p><b>Status: AKTIF dipakai</b> (BUKAN dorman) — diverifikasi lewat pemakai
 * {@link ais.action.master.helper.util.JamPerkuliahanSyncrhonizerProcessor#prosesMigrasiNilai()},
 * yang men-scan baris dengan {@code jumlah > 0.1} dan {@link #getUdahMasuk()} bernilai
 * {@code false}/{@code null}, memanggil {@code Detailperkuliahan#populateDetailNilai} untuk menyalin
 * nilainya, lalu menandai {@link #getUdahMasuk()} menjadi {@code true} agar baris yang sama tidak
 * diproses ulang. Proses migrasi ini hanya berjalan bila konfigurasi
 * {@code aktifkan_proses_migrasi_nilai} bernilai aktif — sehingga meski tabelnya bernama umum
 * ("nilai"), perannya murni sebagai antrean satu-arah menuju {@link Detailperkuliahan}, bukan sumber
 * nilai akhir mahasiswa.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "nilai")

public class NilaiTemporary extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 1773103633393812359L;
	private Long id;
	private String oleh;

	private String olehId;

	/**
	 * @return id akun yang membuat/mengubah baris ini.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param olehId id akun pembuat/pengubah.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama akun yang membuat/mengubah baris ini.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #tanggal_dirubah} setiap kali baris
	 * ini di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas "{detailperkuliahan}_{formatNilai}_{jumlah}", dipakai untuk
	 *         keperluan log/debug.
	 */
	public String toString() {
		return detailperkuliahan + "_" + formatNilai + "_" + jumlah;
	}

	private Detailperkuliahan detailperkuliahan;
	private FormatNilai formatNilai;
	private Double jumlah;
	private Boolean udahMasuk;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public NilaiTemporary() {
	}

	/**
	 * @return id unik baris (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id unik baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return baris {@link Detailperkuliahan} tujuan (KRS mahasiswa pada matakuliah tertentu) tempat
	 *         nilai ini akan dipindahkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detailperkuliahan", nullable = false)
	@ForeignKey(name = "fk___detailperkuliahan")
	public Detailperkuliahan getDetailperkuliahan() {
		return this.detailperkuliahan;
	}

	/**
	 * @param detailperkuliahan baris {@link Detailperkuliahan} tujuan.
	 */
	public void setDetailperkuliahan(Detailperkuliahan detailperkuliahan) {
		this.detailperkuliahan = detailperkuliahan;
	}

	/**
	 * @return besaran nilai komponen ini, untuk komponen {@link #getFormatNilai()}.
	 */
	@Column(name = "jumlah", nullable = false, precision = 10)
	public Double getJumlah() {
		return this.jumlah;
	}

	/**
	 * @param jumlah besaran nilai komponen ini.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * @param formatNilai komponen nilai (mis. UTS/UAS/tugas) yang diacu baris ini.
	 */
	public void setFormatNilai(FormatNilai formatNilai) {
		this.formatNilai = formatNilai;
	}

	/**
	 * @return komponen nilai (mis. UTS/UAS/tugas) yang diacu baris ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "formatnilai", nullable = false)
	@ForeignKey(name = "fk___formatnilai")
	public FormatNilai getFormatNilai() {
		return formatNilai;
	}

	/**
	 * @return {@code true} bila baris ini SUDAH dipindahkan ke {@link Detailperkuliahan} oleh
	 *         {@code JamPerkuliahanSyncrhonizerProcessor#prosesMigrasiNilai()}; {@code false}/
	 *         {@code null} berarti masih menunggu diproses (kandidat migrasi).
	 */
	public Boolean getUdahMasuk() {
		return udahMasuk;
	}

	/**
	 * @param udahMasuk status sudah/belum dipindahkan ke {@link Detailperkuliahan}.
	 */
	public void setUdahMasuk(Boolean udahMasuk) {
		this.udahMasuk = udahMasuk;
	}

}
