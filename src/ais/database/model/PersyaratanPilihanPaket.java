package ais.database.model;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;

/**
 * Model data untuk persyaratan pilihan paket. Tipe ini membawa state yang dipertukarkan oleh
 * lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi
 * yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Paket paket}, {@code Paket persyaratan}, {@code String
 * keterangan}; pemetaan persistence: tabel {@code public.persyaratan_pilihan_paket}; pembacaan/pencarian ({@code
 * getOlehId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getId()}, {@code getKeterangan()},
 * {@code getPersyaratan()}); validasi/perhitungan ({@code checkKombinasiPaket()}); mutasi data ({@code
 * setOlehId()}, {@code setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code setId()}, {@code
 * setKeterangan()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "persyaratan_pilihan_paket")

public class PersyaratanPilihanPaket extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -227313087242633498L;

	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	@SuppressWarnings("unchecked")
	public static boolean checkKombinasiPaket(Paket paket, List<Jurusan> prodis) {
		Session session = HibernateUtil.currentSession();
		List<Paket> prasyaratPakets = session.createCriteria(PersyaratanPilihanPaket.class)
				.add(Restrictions.eq("paket", paket)).setProjection(Projections.groupProperty("persyaratan")).list();

		for (Paket prasyarat : prasyaratPakets) {
			List<Jurusan> jurusans = session.createCriteria(PaketJurusanPmb.class)
					.add(Restrictions.eq("paket", prasyarat)).setProjection(Projections.groupProperty("jurusan"))
					.list();
			boolean ada = false;

			for (Jurusan jurusan : jurusans) {
				for (Jurusan prodi : prodis) {
					if (prodi.getId().equals(jurusan.getId())) {
						ada = true;
						break;
					}
				}
			}

			if (!ada) {
				return false;
			}
		}

		return true;
	}

	private Paket paket;
	private Paket persyaratan;
	private String keterangan;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, nullable = false, unique = true)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "keterangan")
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public String getKeterangan() {
		return keterangan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "persyaratan", nullable = true)
	public Paket getPersyaratan() {
		return persyaratan;
	}

	public void setPersyaratan(Paket persyaratan) {
		this.persyaratan = persyaratan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "paket", nullable = true)
	public Paket getPaket() {
		return paket;
	}

	public void setPaket(Paket paket) {
		this.paket = paket;
	}

}
