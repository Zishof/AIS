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
	/** Nama pelaku (audit shadow, lihat {@link GeneralValueObject}) yang membuat/mengubah baris ini. */
	private String oleh;
	/** Id pelaku (audit shadow) yang membuat/mengubah baris ini. */
	private String olehId;

	/** @return id pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pelaku. Nilai kosong/blank diabaikan (fail-safe agar audit shadow tidak
	 * tertimpa string kosong secara tidak sengaja) &mdash; bukan validasi keamanan.
	 *
	 * @param olehId id pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pelaku. Nilai kosong/blank diabaikan, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengubah stempel waktu perubahan terakhir secara manual. Nilai default sudah di-set ke
	 * waktu saat ini pada deklarasi field dan di-refresh otomatis oleh {@link #onUpdate()} pada
	 * setiap update; setter ini jarang perlu dipanggil langsung.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Memeriksa apakah kombinasi {@code paket} + program studi {@code prodis} yang dipilih calon
	 * mahasiswa sudah memenuhi seluruh prasyarat jurusan yang terdaftar untuk {@code paket} ini.
	 * Untuk setiap {@link Paket} prasyarat (baris {@link PersyaratanPilihanPaket} dengan
	 * {@code paket} sama, dikelompokkan per {@code persyaratan}), diambil himpunan
	 * {@link Jurusan} yang tersedia untuk paket prasyarat itu ({@link PaketJurusanPmb}); bila
	 * TIDAK ADA satu pun irisan dengan {@code prodis} yang dipilih untuk SALAH SATU prasyarat,
	 * metode langsung mengembalikan {@code false}.
	 *
	 * @param paket paket pendaftaran yang hendak diperiksa prasyaratnya
	 * @param prodis daftar program studi (pilihan jurusan) yang dipilih calon mahasiswa
	 * @return {@code true} bila seluruh paket prasyarat memiliki irisan jurusan dengan
	 * {@code prodis}, atau bila {@code paket} tidak memiliki prasyarat sama sekali;
	 * {@code false} bila ada satu saja prasyarat yang tidak terpenuhi
	 */
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

	/** @return id baris (primary key, auto-generated identity di database). */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, nullable = false, unique = true)
	public Long getId() {
		return id;
	}

	/** @param id id baris; kolom tidak insertable sehingga nilai ini diabaikan saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @param keterangan keterangan tambahan untuk baris persyaratan ini. */
	@Column(name = "keterangan")
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return keterangan tambahan untuk baris persyaratan ini, boleh {@code null}. */
	public String getKeterangan() {
		return keterangan;
	}

	/** @return {@link Paket} yang disyaratkan (paket prasyarat) untuk {@link #getPaket()}, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "persyaratan", nullable = true)
	public Paket getPersyaratan() {
		return persyaratan;
	}

	/** @param persyaratan paket prasyarat yang harus lebih dulu dipilih/dipenuhi. */
	public void setPersyaratan(Paket persyaratan) {
		this.persyaratan = persyaratan;
	}

	/** @return {@link Paket} yang mensyaratkan pemilihan {@link #getPersyaratan()} lebih dulu, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "paket", nullable = true)
	public Paket getPaket() {
		return paket;
	}

	/** @param paket paket yang mensyaratkan pemilihan {@link #getPersyaratan()} lebih dulu. */
	public void setPaket(Paket paket) {
		this.paket = paket;
	}

}
