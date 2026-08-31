package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

/**
 * Komponen dashboard khusus untuk role has dashboard. Kelas ini memilih variasi data atau tampilan
 * dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code id}, {@code userRole}, {@code dashboard},
 * {@code name}, {@code originDashboard}; operasi lokal: {@code onUpdate()}, {@code getId()}, {@code setId()},
 * {@code hakAkses()}, {@code setUserRole()}, {@code setDashboard()}, {@code getDashboard()}, {@code setName}().
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "role_has_dashboard")
public class RoleHasDashboard extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7579034468497961829L;
	private Long id;
	private Tbmrole userRole;
	private String dashboard;
	private String name;
	private Dashboard originDashboard;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "userrole", nullable = true)
	public Tbmrole hakAkses() {
		return this.userRole;
	}

	public void setUserRole(Tbmrole userRole) {
		this.userRole = userRole;
	}

	public void setDashboard(String dashboard) {
		this.dashboard = dashboard;
	}

	public String getDashboard() {
		if (getOriginDashboard() != null) {
			dashboard = getOriginDashboard().getClazz();
		}
		return dashboard;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		if (getOriginDashboard() != null) {
			name = getOriginDashboard().getNama();
		}
		return name;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "origin_dashboard", nullable = true)
	public Dashboard getOriginDashboard() {
		return originDashboard;
	}

	public void setOriginDashboard(Dashboard originDashboard) {
		this.originDashboard = originDashboard;
	}

}
