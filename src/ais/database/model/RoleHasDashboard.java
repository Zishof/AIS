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
 * <p><b>Entitas ini dormant/yatim.</b> Selain deklarasi field di {@link Dashboard} dan pemetaan
 * Hibernate ({@code hibernate.cfg.xml}), satu-satunya pemakai lain di seluruh repo adalah blok
 * kode yang <b>di-comment-out</b> di {@code ais.action.maintenance.BlankAction} (query
 * {@code Criteria} atas kelas ini untuk membangun tab dashboard per role) &mdash; tidak ada
 * {@code Action} CRUD, servis, maupun jalur UI aktif yang membaca/menulis baris tabel
 * {@code role_has_dashboard}. Tidak ditemukan pemanggil hidup.
 *
 * <p><b>Kuirk penamaan getter yang berpotensi memutus pemetaan Hibernate.</b> Anotasi
 * {@code @ManyToOne}/{@code @JoinColumn(name = "userrole")} diletakkan pada method
 * {@link #hakAkses()}, yang <b>tidak mengikuti konvensi JavaBean</b> ({@code getUserRole()})
 * dan tidak berpasangan nama dengan {@link #setUserRole(Tbmrole)}. Ini berbeda dari pola yang
 * sama-sama bernama {@code hakAkses()} di {@link Tbmuser}: pada kelas itu, properti persisten
 * sesungguhnya adalah {@code getUserRole()}/{@code setUserRole()} (mengikuti konvensi
 * JavaBean), sedangkan {@code hakAkses()} di sana adalah method bisnis biasa (tidak
 * beranotasi) yang membungkus {@code getUserRole()} dengan cache. Di kelas ini, satu-satunya
 * getter untuk relasi {@code userRole} justru yang beranotasi dan tidak mengikuti konvensi,
 * sehingga besar kemungkinan Hibernate (yang menemukan properti lewat introspeksi JavaBean
 * standar untuk {@code AccessType.PROPERTY}) tidak mengenali {@code hakAkses()} sebagai
 * getter untuk properti apa pun, dan kolom {@code userrole} tidak pernah benar-benar
 * dipetakan/tersimpan lewat entity ini. Karena entitas ini dormant (lihat di atas), kuirk ini
 * belum pernah teruji lewat jalur pakai nyata.</p>
 *
 * @see GeneralValueObject
 * @see Dashboard
 * @see Tbmuser
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
	/** Role yang berhak melihat widget dashboard ini; lihat catatan kuirk pemetaan pada javadoc kelas. */
	private Tbmrole userRole;
	/** Nama kelas Java komponen dashboard, diturunkan dari {@link #getOriginDashboard()} bila ada. */
	private String dashboard;
	/** Label tampil widget dashboard, diturunkan dari {@link #getOriginDashboard()} bila ada. */
	private String name;
	/** Widget dashboard master ({@link Dashboard}) sumber nilai {@link #getDashboard()}/{@link #getName()}. */
	private Dashboard originDashboard;

	/** Callback JPA sebelum update: menyegarkan stempel waktu perubahan lewat interceptor audit. */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** @return id baris (primary key, auto-generated identity di database). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; kolom tidak insertable sehingga nilai ini diabaikan saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return role yang berhak melihat widget dashboard ini, boleh {@code null}. Lihat catatan
	 *     kelas soal penamaan getter ini yang menyimpang dari konvensi JavaBean
	 *     ({@code getUserRole()}) dan kemungkinan tidak dikenali Hibernate sebagai properti
	 *     persisten.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "userrole", nullable = true)
	public Tbmrole hakAkses() {
		return this.userRole;
	}

	/** @param userRole role yang berhak melihat widget dashboard ini. */
	public void setUserRole(Tbmrole userRole) {
		this.userRole = userRole;
	}

	/** @param dashboard nama kelas Java komponen dashboard; akan ditimpa oleh {@link #getDashboard()} bila {@link #getOriginDashboard()} terisi. */
	public void setDashboard(String dashboard) {
		this.dashboard = dashboard;
	}

	/**
	 * @return nama kelas Java komponen dashboard yang akan dirender. <b>Bukan getter murni:</b>
	 *     bila {@link #getOriginDashboard()} tidak {@code null}, nilai diturunkan dari
	 *     {@code getOriginDashboard().getClazz()} dan ditulis balik ke field {@code dashboard}
	 *     sebelum dikembalikan &mdash; pola getter-menulis yang sama juga muncul di banyak
	 *     entitas lain pada paket ini.
	 */
	public String getDashboard() {
		if (getOriginDashboard() != null) {
			dashboard = getOriginDashboard().getClazz();
		}
		return dashboard;
	}

	/** @param name label tampil widget dashboard; akan ditimpa oleh {@link #getName()} bila {@link #getOriginDashboard()} terisi. */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return label tampil widget dashboard. <b>Bukan getter murni:</b> bila
	 *     {@link #getOriginDashboard()} tidak {@code null}, nilai diturunkan dari
	 *     {@code getOriginDashboard().getNama()} dan ditulis balik ke field {@code name}
	 *     sebelum dikembalikan, sama seperti {@link #getDashboard()}.
	 */
	public String getName() {
		if (getOriginDashboard() != null) {
			name = getOriginDashboard().getNama();
		}
		return name;
	}

	/** @return widget dashboard master sumber nilai {@link #getDashboard()}/{@link #getName()}, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "origin_dashboard", nullable = true)
	public Dashboard getOriginDashboard() {
		return originDashboard;
	}

	/** @param originDashboard widget dashboard master sumber nilai {@link #getDashboard()}/{@link #getName()}. */
	public void setOriginDashboard(Dashboard originDashboard) {
		this.originDashboard = originDashboard;
	}

}
