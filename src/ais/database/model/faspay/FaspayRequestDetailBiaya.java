package ais.database.model.faspay;

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
 * Entity JPA/Hibernate rincian komponen biaya (fee/administrasi) untuk satu {@link
 * FaspayRequest}. Berbeda dengan {@code ais.database.model.faspay.FaspayRequestDetail} yang
 * merepresentasikan pos tagihan pokok (mis. cicilan bulanan/item biaya), baris {@code
 * faspay_request_detail_biaya} ini merepresentasikan komponen biaya tambahan yang ditautkan ke
 * satu {@link DetailBiaya} spesifik.
 *
 * <p>Lihat javadoc kelas {@link FaspayRequest} untuk penjelasan pola arsitektur umum 4-entity
 * (Request/RequestDetail/RequestDetailBiaya/Response) yang dipakai di semua gateway H2H AIS.</p>
 *
 * @see FaspayRequest
 * @see ais.database.model.faspay.FaspayRequestDetail
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "faspay_request_detail_biaya")



public class FaspayRequestDetailBiaya extends GeneralValueObject {
	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build ({@link java.io.Serializable}).
	 */
	private static final long serialVersionUID = 2463821327548439808L;
	/** Primary key auto-increment (identity) baris komponen biaya ini. */
	private Long id;
	/** Nama/label pengguna (audit shadow) yang terakhir membuat/mengubah baris ini. */
	private String oleh;
	/** ID pengguna (audit shadow) yang terakhir membuat/mengubah baris ini, independen dari
	 * relasi entity user. */
	private String olehId;

	/**
	 * Mengambil ID pengguna (audit shadow) yang terakhir membuat/mengubah baris ini.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna (audit shadow). Nilai {@code null} atau kosong diabaikan sehingga
	 * nilai audit sebelumnya tetap dipertahankan.
	 *
	 * @param olehId ID pengguna yang akan dicatat; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna (audit shadow). Nilai {@code null} atau kosong diabaikan sehingga
	 * nilai audit sebelumnya tetap dipertahankan.
	 *
	 * @param oleh nama pengguna yang akan dicatat; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna (audit shadow) yang terakhir membuat/mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: memperbarui {@link #getTanggal_dirubah()} otomatis lewat
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini di-UPDATE.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi timestamp terakhir baris ini diubah. Biasanya diisi otomatis oleh {@link
	 * #onUpdate()}.
	 *
	 * @param tanggal_dirubah timestamp perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp terakhir baris ini diubah.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk debugging/log: {@code id-faspayRequest-detailBiaya-nilai}.
	 *
	 * @return string ringkas berisi field utama baris ini.
	 */
	public String toString() {
		return id + "-" + faspayRequest + "-" + detailBiaya + "-" + nilai;
	}

	/** Header transaksi Faspay tempat baris komponen biaya ini berada (relasi wajib, {@code
	 * nullable = false}). */
	private FaspayRequest faspayRequest;
	/** Detail biaya (komponen biaya spesifik, mis. biaya admin/fee) yang menjadi acuan nilai
	 * baris ini. */
	private DetailBiaya detailBiaya;
	/** Nominal komponen biaya ini. */
	private Double nilai;

	/**
	 * Konstruktor default (dibutuhkan Hibernate).
	 */
	public FaspayRequestDetailBiaya() {
	}

	/**
	 * Mengambil primary key baris ini.
	 *
	 * @return ID baris, atau {@code null} bila belum dipersistensi.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key baris ini. Umumnya tidak dipanggil manual karena kolom {@code id}
	 * bersifat {@code insertable = false}.
	 *
	 * @param id nilai ID yang akan diisi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil header transaksi Faspay pemilik baris komponen biaya ini.
	 *
	 * @return {@link FaspayRequest} induk baris ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "faspay_request", nullable = false)
	public FaspayRequest getFaspayRequest() {
		return faspayRequest;
	}

	/**
	 * Mengisi header transaksi Faspay pemilik baris komponen biaya ini.
	 *
	 * @param faspayRequest header transaksi yang akan ditautkan.
	 */
	public void setFaspayRequest(FaspayRequest faspayRequest) {
		this.faspayRequest = faspayRequest;
	}

	/**
	 * Mengambil detail biaya (komponen biaya spesifik) acuan nilai baris ini.
	 *
	 * @return {@link DetailBiaya} terkait, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detail_biaya", nullable = true)
	public DetailBiaya getDetailBiaya() {
		return detailBiaya;
	}

	/**
	 * Mengisi detail biaya (komponen biaya spesifik) acuan nilai baris ini.
	 *
	 * @param detailBiaya detail biaya yang akan ditautkan.
	 */
	public void setDetailBiaya(DetailBiaya detailBiaya) {
		this.detailBiaya = detailBiaya;
	}

	/**
	 * Mengambil nominal komponen biaya ini. Bila belum pernah diisi ({@code null}), nilainya
	 * di-default-kan ke {@code 0.0}. Selain itu, bila nilai tersimpan masih dianggap kosong
	 * (kurang dari {@code 0.01}) namun {@link #detailBiaya} memiliki nilai biaya baru/lama yang
	 * signifikan (lebih dari {@code 0.1}), getter ini <b>mengambil-alih</b> nilai dari {@link
	 * DetailBiaya#getNilaiBiayaBaru()} (atau {@link DetailBiaya#getNilaiBiaya()} bila nilai baru
	 * tidak ada) dan menyimpannya sebagai efek samping getter — pola fallback otomatis dari
	 * master data biaya, bukan getter murni.
	 *
	 * @return nominal komponen biaya, sudah termasuk fallback dari {@link #detailBiaya} bila
	 *         berlaku.
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
	 * Mengisi nominal komponen biaya ini secara eksplisit.
	 *
	 * @param nilai nominal yang akan diisi.
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

}
