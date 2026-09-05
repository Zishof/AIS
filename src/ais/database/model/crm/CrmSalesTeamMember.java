package ais.database.model.crm;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * Keanggotaan satu {@link Tbmuser} pada satu {@link CrmSalesTeam}, dengan {@link #peranTim}
 * (Ketua/Anggota). Mengikuti pola {@code ais.database.model.spi.TimAuditSPI} — relasi terstruktur
 * (bukan teks bebas nama anggota) supaya bisa dipakai untuk penugasan lead & rekap beban kerja.
 *
 * <p>Tabel {@code public.crm_sales_team_member} dibuat otomatis oleh {@code hbm2ddl=update} saat
 * restart.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "crm_sales_team_member")
public class CrmSalesTeamMember extends GeneralValueObject {

	/** Peran anggota: ketua tim (satu tim idealnya punya satu ketua, tidak dipaksakan di entity ini). */
	public static final String KETUA_TIM = "KETUA_TIM";
	/** Peran anggota: anggota biasa; juga nilai default {@link #getPeranTim()}. */
	public static final String ANGGOTA_TIM = "ANGGOTA_TIM";

	/** Peta peran (kode &rarr; label bahasa manusia) untuk pilihan combo pada UI dan {@link #getPeranTimLabel()}. */
	public static final Map<String, String> PERAN_TIM_DATA = new LinkedHashMap<String, String>();
	static {
		PERAN_TIM_DATA.put(KETUA_TIM, "Ketua Tim");
		PERAN_TIM_DATA.put(ANGGOTA_TIM, "Anggota Tim");
	}

	private static final long serialVersionUID = 3120260815005L;

	/** Primary key baris {@code crm_sales_team_member}. */
	private Long id;
	/** Tim penjualan yang diikuti anggota ini. */
	private CrmSalesTeam salesTeam;
	/** Pengguna yang menjadi anggota tim. */
	private Tbmuser anggota;
	/** Peran anggota dalam tim; lihat konstanta {@code KETUA_TIM}/{@code ANGGOTA_TIM} dan {@link #getPeranTim()}. */
	private String peranTim;
	/** Flag aktif/nonaktif (soft delete); lihat {@link #getAktif()} untuk default-nya. */
	private Boolean aktif;
	/** Nama pengguna pengubah terakhir; diisi jalur audit, lihat {@link #setOleh(String)}. */
	private String oleh;
	/** Id pengguna pengubah terakhir; diisi jalur audit, lihat {@link #setOlehId(String)}. */
	private String olehId;
	/** Stempel waktu perubahan terakhir; diperbarui otomatis lewat {@link #onUpdate()} saat update. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
	public CrmSalesTeamMember() {
	}

	/**
	 * Constructor pintas yang langsung mengaitkan keanggotaan ini ke sebuah {@link CrmSalesTeam}.
	 *
	 * @param salesTeam tim penjualan yang diikuti
	 */
	public CrmSalesTeamMember(CrmSalesTeam salesTeam) {
		this.salesTeam = salesTeam;
	}

	/**
	 * Mengembalikan primary key baris {@code crm_sales_team_member}.
	 *
	 * @return primary key, atau {@code null} bila entity belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
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
	 * Mengembalikan tim penjualan yang diikuti anggota ini, setelah diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}. Kolom database {@code nullable = false} — setiap
	 * baris keanggotaan wajib terkait satu tim.
	 *
	 * @return tim penjualan pemilik keanggotaan ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_team", nullable = false)
	public CrmSalesTeam getSalesTeam() {
		salesTeam = check(salesTeam);
		return salesTeam;
	}

	/**
	 * Menyetel tim penjualan pemilik keanggotaan ini.
	 *
	 * @param salesTeam tim penjualan baru
	 */
	public void setSalesTeam(CrmSalesTeam salesTeam) {
		this.salesTeam = salesTeam;
	}

	/**
	 * Mengembalikan pengguna yang menjadi anggota tim, setelah diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}. Kolom database {@code nullable = false} — memakai
	 * FK terstruktur ke {@link Tbmuser} (bukan teks bebas) supaya bisa dipakai untuk penugasan lead
	 * ({@link CrmLead#getDitugaskanUser()}) & rekap beban kerja per pengguna.
	 *
	 * @return pengguna anggota tim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota", nullable = false)
	public Tbmuser getAnggota() {
		anggota = check(anggota);
		return anggota;
	}

	/**
	 * Menyetel pengguna anggota tim.
	 *
	 * @param anggota pengguna baru
	 */
	public void setAnggota(Tbmuser anggota) {
		this.anggota = anggota;
	}

	/**
	 * Mengembalikan peran anggota dalam tim ({@link #KETUA_TIM}/{@link #ANGGOTA_TIM}). Nilai
	 * {@code null} dianggap {@link #ANGGOTA_TIM} — anggota baru tanpa peran eksplisit selalu
	 * dianggap anggota biasa, bukan ketua.
	 *
	 * @return peran anggota, tidak pernah {@code null}
	 */
	@Column(name = "peran_tim", nullable = false, length = 20)
	public String getPeranTim() {
		return peranTim == null ? ANGGOTA_TIM : peranTim;
	}

	/**
	 * Menyetel peran anggota. Tanpa validasi terhadap konstanta {@code KETUA_TIM}/
	 * {@code ANGGOTA_TIM}; tidak ada pemeriksaan bahwa satu tim hanya punya satu ketua — beberapa
	 * baris keanggotaan pada tim yang sama boleh saja sama-sama {@link #KETUA_TIM}.
	 *
	 * @param peranTim peran anggota baru
	 */
	public void setPeranTim(String peranTim) {
		this.peranTim = peranTim;
	}

	/**
	 * Label bahasa manusia dari {@link #getPeranTim()}, dipakai langsung oleh tampilan. Method ini
	 * bukan kolom database ({@code @Transient}) — nilainya diturunkan dari {@link #PERAN_TIM_DATA}
	 * setiap kali dipanggil. Bila kode peran tidak terdaftar pada peta (mis. data lama/rusak), kode
	 * mentahnya sendiri dikembalikan apa adanya sebagai fallback, bukan {@code null}.
	 *
	 * @return label peran, tidak pernah {@code null}
	 */
	@Transient
	public String getPeranTimLabel() {
		String label = PERAN_TIM_DATA.get(getPeranTim());
		return label == null ? getPeranTim() : label;
	}

	/**
	 * Mengembalikan status aktif/nonaktif (soft delete). Nilai {@code null} dianggap {@code true}.
	 *
	 * @return {@code true} bila aktif (default), {@code false} bila dinonaktifkan
	 */
	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif/nonaktif.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah entity ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi non-trivial: nilai {@code null}
	 * atau string kosong/spasi diabaikan diam-diam (lihat {@link GeneralValueObject#setOleh(String)}).
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah entity ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOleh(String)}: nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; nilainya akan ditimpa otomatis
	 * oleh {@link #onUpdate()} pada jalur update Hibernate.
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Hook siklus hidup Hibernate {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat
	 * sebelum statement {@code UPDATE} dieksekusi untuk baris ini, mendelegasikan pembaruan stempel
	 * waktu ke {@code AuditTimestampInterceptor.ubah(this)}.
	 */
	@PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Representasi teks ringkas untuk logging/debugging: {@code "<anggota>-(peranLabel)"}, atau
	 * {@code "-"} sebagai pengganti nama anggota bila {@link #anggota} bernilai {@code null}.
	 *
	 * @return representasi teks entity ini
	 */
	public String toString() {
		return (anggota == null ? "-" : anggota.toString()) + " (" + getPeranTimLabel() + ")";
	}
}
