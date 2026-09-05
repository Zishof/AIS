package ais.database.model.jatelindo;

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
import org.hibernate.envers.Audited;



import ais.database.model.DetailBiaya;
import ais.database.model.GeneralValueObject;

/**
 * Entity Hibernate untuk satu baris komponen biaya/fee tambahan pada sebuah {@link JatelindoRequest},
 * dipetakan ke tabel {@code jatelindo_request_detail_biaya}. Dibangkitkan oleh hbm2java, lalu dilengkapi
 * logika fallback nilai ({@link #getNilai()}) secara manual.
 *
 * <p>Kelas ini adalah kelas <i>rincian biaya</i> pada pola 4-kelas per payment gateway yang berulang
 * identik di seluruh integrasi H2H AIS (lihat {@link JatelindoRequest} untuk penjelasan pola lengkap).
 * Berbeda dari {@code JatelindoRequestDetail} (yang merinci item/cicilan pokok yang dibayar), entity ini
 * merinci komponen biaya tambahan (mis. biaya admin/fee gateway) yang dikaitkan ke {@link DetailBiaya}.</p>
 *
 * <p><b>Catatan keamanan:</b> tidak ada field kartu/PIN/password/token di kelas ini. Entity ini tidak
 * memiliki field kepemilikan/tenant eksplisit -- kepemilikan mengikuti {@link #getJatelindoRequest()}.</p>
 *
 * @see JatelindoRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jatelindo_request_detail_biaya")



public class JatelindoRequestDetailBiaya extends GeneralValueObject {
	/** 
	 * 
	 */
	private static final long serialVersionUID = 2463821327548439808L;
	private Long id;
	/** Nama pengguna (username) yang membuat/terakhir menyentuh baris audit ini. */
	private String oleh;
	/** Id pengguna yang membuat/terakhir menyentuh baris audit ini; pasangan dari {@link #oleh}. */
	private String olehId;

	/**
	 * @return id pengguna (audit) yang tercatat pada baris ini, sebagaimana adanya (tanpa fallback).
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengeset id pengguna audit. Nilai {@code null} atau string kosong/blank diabaikan (fail-safe)
	 * agar id pengguna audit yang sudah tersimpan tidak tertimpa nilai kosong.
	 * @param olehId id pengguna audit baru; diabaikan jika null/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengeset nama pengguna audit ({@link #oleh}). Nilai {@code null} atau kosong/blank diabaikan
	 * (fail-safe) supaya nama pengguna audit yang sudah tersimpan tidak tertimpa nilai kosong.
	 * @param oleh nama pengguna audit baru; diabaikan jika null/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna (audit) yang tercatat pada baris ini, sebagaimana adanya (tanpa fallback).
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini
	 * di-{@code UPDATE}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah timestamp "terakhir diubah" baru untuk baris ini.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return timestamp terakhir baris ini diubah (kolom audit, diperbarui otomatis oleh
	 * {@link #onUpdate()} pada setiap {@code UPDATE}).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas entity ini untuk keperluan log/debug: gabungan id, request induk,
	 * detail biaya, dan nilai.
	 */
	public String toString() {
		return id + "-" + jatelindoRequest + "-" + detailBiaya + "-" + nilai;
	}

	/** Request Jatelindo induk yang memiliki komponen biaya ini. */
	private JatelindoRequest jatelindoRequest;
	/** Detail biaya (skema/tarif) acuan komponen biaya ini. */
	private DetailBiaya detailBiaya;
	/** Nominal komponen biaya/fee tambahan pada baris ini. */
	private Double nilai;

	/**
	 * Konstruktor default (dipakai Hibernate).
	 */
	public JatelindoRequestDetailBiaya() {
	}

	/**
	 * @return id unik (primary key, auto-increment) baris {@code jatelindo_request_detail_biaya} ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id baris ini; normalnya tidak diset manual karena kolom bersifat
	 * {@code insertable = false} (auto-increment oleh database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return request Jatelindo induk ({@link JatelindoRequest}) yang memiliki komponen biaya ini;
	 * tidak pernah {@code null} untuk baris yang tersimpan (kolom {@code jatelindo_request} bersifat
	 * {@code nullable = false}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jatelindo_request", nullable = false)
	public JatelindoRequest getJatelindoRequest() {
		return jatelindoRequest;
	}

	/**
	 * @param jatelindoRequest request Jatelindo induk yang baru untuk komponen biaya ini.
	 */
	public void setJatelindoRequest(JatelindoRequest jatelindoRequest) {
		this.jatelindoRequest = jatelindoRequest;
	}

	/**
	 * @return detail biaya (skema/tarif) acuan komponen biaya ini, atau {@code null} jika tidak
	 * mengacu ke {@code DetailBiaya} tertentu.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detail_biaya", nullable = true)
	public DetailBiaya getDetailBiaya() {
		return detailBiaya;
	}

	/**
	 * @param detailBiaya detail biaya yang baru untuk komponen biaya ini.
	 */
	public void setDetailBiaya(DetailBiaya detailBiaya) {
		this.detailBiaya = detailBiaya;
	}

	/**
	 * @return nominal komponen biaya/fee ini. Jika belum diset atau bernilai (hampir) nol
	 * (&lt; 0.01) sementara {@link #detailBiaya} tersedia dengan tarif &gt; 0.1, nilai diisi otomatis
	 * dari tarif {@code DetailBiaya} tersebut (memakai {@code getNilaiBiayaBaru()} bila ada, kalau
	 * tidak {@code getNilaiBiaya()}) -- efek samping: field {@link #nilai} ikut termutasi oleh
	 * pemanggilan getter ini.
	 */
	public Double getNilai() {
		if (nilai == null) {
			nilai = 0.0;
		}

		if (nilai < 0.01 && (detailBiaya != null && (detailBiaya.getNilaiBiayaBaru() == null
				? detailBiaya.getNilaiBiaya() : detailBiaya.getNilaiBiayaBaru()) > 0.1)) {
			nilai = (detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
					: detailBiaya.getNilaiBiayaBaru());
		}

		return nilai;
	}

	/**
	 * @param nilai nominal komponen biaya/fee yang baru.
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

}
