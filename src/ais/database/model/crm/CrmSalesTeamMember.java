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

	public static final String KETUA_TIM = "KETUA_TIM";
	public static final String ANGGOTA_TIM = "ANGGOTA_TIM";

	public static final Map<String, String> PERAN_TIM_DATA = new LinkedHashMap<String, String>();
	static {
		PERAN_TIM_DATA.put(KETUA_TIM, "Ketua Tim");
		PERAN_TIM_DATA.put(ANGGOTA_TIM, "Anggota Tim");
	}

	private static final long serialVersionUID = 3120260815005L;

	private Long id;
	private CrmSalesTeam salesTeam;
	private Tbmuser anggota;
	private String peranTim;
	private Boolean aktif;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public CrmSalesTeamMember() {
	}

	public CrmSalesTeamMember(CrmSalesTeam salesTeam) {
		this.salesTeam = salesTeam;
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_team", nullable = false)
	public CrmSalesTeam getSalesTeam() {
		salesTeam = check(salesTeam);
		return salesTeam;
	}

	public void setSalesTeam(CrmSalesTeam salesTeam) {
		this.salesTeam = salesTeam;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota", nullable = false)
	public Tbmuser getAnggota() {
		anggota = check(anggota);
		return anggota;
	}

	public void setAnggota(Tbmuser anggota) {
		this.anggota = anggota;
	}

	@Column(name = "peran_tim", nullable = false, length = 20)
	public String getPeranTim() {
		return peranTim == null ? ANGGOTA_TIM : peranTim;
	}

	public void setPeranTim(String peranTim) {
		this.peranTim = peranTim;
	}

	/** Label bahasa manusia dari {@link #getPeranTim()}, dipakai langsung oleh tampilan. */
	@Transient
	public String getPeranTimLabel() {
		String label = PERAN_TIM_DATA.get(getPeranTim());
		return label == null ? getPeranTim() : label;
	}

	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	public String toString() {
		return (anggota == null ? "-" : anggota.toString()) + " (" + getPeranTimLabel() + ")";
	}
}
