package ais.database.model.temp;

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
import org.hibernate.envers.Audited;

import ais.database.model.FormatNilai;
import ais.database.model.FormatNilaiTambahan;
import ais.database.model.GeneralValueObject;



/**
 * Entitas Hibernate (skema {@code public}, tabel {@code nilai_temp}) yang menjadi BUFFER/staging
 * komponen nilai (mis. nilai tugas/UTS/UAS sesuai {@link FormatNilai}) untuk satu baris
 * {@link DetailperkuliahanTemp} — analog {@code Nilai} (non-temp) pada modul akademik resmi, tapi
 * untuk jalur data sementara/percobaan yang sama dengan {@link DetailperkuliahanTemp}.
 *
 * <p>
 * <b>Status dorman — TERVERIFIKASI</b>: seperti {@link DetailperkuliahanTemp} dan
 * {@link AisFlagsData}, kelas ini terdaftar di pemetaan Hibernate ({@code hibernate.cfg.xml})
 * tetapi TIDAK direferensikan oleh Action/Helper/API manapun di seluruh codebase AIS (pencarian
 * menyeluruh terhadap nama kelas fully-qualified hanya menemukan file ini sendiri dan baris
 * registrasi mapping). Karena tidak ada proses bisnis yang menulis ke tabel ini, pertanyaan
 * "apakah proses commit ke tabel resmi aman/atomik" tidak relevan untuk kondisi kode saat ini —
 * tidak ada kode yang memindahkan data dari {@code nilai_temp}/{@code detailperkuliahan_temp} ke
 * tabel {@code Nilai}/{@code Detailperkuliahan} resmi; seandainya jalur staging→resmi ini
 * diaktifkan kembali di masa depan, transaksi/atomisitasnya perlu dirancang saat itu.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "nilai_temp")



public class NilaiTemp extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 1773103633393812359L;
	private Long id;
	/** Field audit shadow (bukan kolom Hibernate): nama pemroses terakhir, diisi lewat {@link #setOleh(String)}. */
	private String oleh;/** Field audit shadow (bukan kolom Hibernate): ID pemroses terakhir, diisi lewat {@link #setOlehId(String)}. */private String olehId;/** @return ID pemroses terakhir yang mengubah baris ini (field audit shadow). */public String getOlehId() {return olehId;}/**
	 * Menyetel ID pemroses terakhir — SETTER MENOLAK nilai kosong/null (guard fail-closed): bila
	 * {@code olehId} null atau hanya berisi spasi, method ini langsung {@code return} tanpa
	 * mengubah field, mempertahankan nilai audit sebelumnya.
	 *
	 * @param olehId ID pemroses yang akan diset; diabaikan bila null/kosong.
	 */public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menyetel nama pemroses terakhir — SETTER MENOLAK nilai kosong/null (guard fail-closed) dengan
	 * pola yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pemroses yang akan diset; diabaikan bila null/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pemroses terakhir yang mengubah baris ini (field audit shadow, lihat {@link #setOleh(String)}). */
	public String getOleh() {
		return oleh;
	}

	/** Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} lewat {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini di-update. */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir yang akan diset. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini (diperbarui otomatis lewat {@link #onUpdate()}). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas: {@link #detailperkuliahan}, {@link #formatNilai}, dan {@link #jumlah} digabung. */
	public String toString() {
		return detailperkuliahan + "_" + formatNilai + "_" + jumlah;
	}

	/** Baris {@link DetailperkuliahanTemp} induk yang komponen nilai ini menjadi bagiannya. */
	private DetailperkuliahanTemp detailperkuliahan;
	/** Format/jenis komponen nilai (mis. UTS/UAS/Tugas), lihat {@link FormatNilai}. */
	private FormatNilai formatNilai;
	/** Format nilai tambahan, bila komponen ini termasuk kategori nilai tambahan. */
	private FormatNilaiTambahan formatNilaiTambahan;
	/** Besaran/nilai komponen ini; default {@code 0.0} bila belum diisi. */
	private Double jumlah;

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public NilaiTemp() {
	}

	/** @return ID baris (primary key, auto-increment). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id ID baris (primary key) yang akan diset. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return baris {@link DetailperkuliahanTemp} induk yang komponen nilai ini menjadi bagiannya. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detailperkuliahan", nullable = false)
	public DetailperkuliahanTemp getDetailperkuliahan() {
		return this.detailperkuliahan;
	}

	/** @param detailperkuliahan baris induk yang akan diset. */
	public void setDetailperkuliahan(DetailperkuliahanTemp detailperkuliahan) {
		this.detailperkuliahan = detailperkuliahan;
	}

	/** @return besaran/nilai komponen ini; default {@code 0.0} bila belum diisi. */
	@Column(name = "jumlah", nullable = false, precision = 10)
	public Double getJumlah() {
		if (jumlah == null) {
			jumlah = 0.0;
		}
		return this.jumlah;
	}

	/** @param jumlah besaran/nilai komponen yang akan diset. */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/** @param formatNilai format/jenis komponen nilai yang akan diset. */
	public void setFormatNilai(FormatNilai formatNilai) {
		this.formatNilai = formatNilai;
	}

	/** @return format/jenis komponen nilai (mis. UTS/UAS/Tugas). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "formatnilai", nullable = true)
	public FormatNilai getFormatNilai() {
		return formatNilai;
	}

	/** @return format nilai tambahan, bila komponen ini termasuk kategori nilai tambahan. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "formatnilai_tambahan", nullable = true)
	public FormatNilaiTambahan getFormatNilaiTambahan() {
		return formatNilaiTambahan;
	}

	/** @param formatNilaiTambahan format nilai tambahan yang akan diset. */
	public void setFormatNilaiTambahan(FormatNilaiTambahan formatNilaiTambahan) {
		this.formatNilaiTambahan = formatNilaiTambahan;
	}

}
