package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Pencacah kode member {@link AnggotaKoperasi} yang DIPERSISTENSIKAN per <b>(koperasi, jenis)</b>
 * &mdash; tabel {@code koperasi.anggota_koperasi_kode_counter}. Entity ini adalah perbaikan dari
 * {@link AnggotaKoperasi#generateKodeMember(org.hibernate.Session, java.util.Date)} versi lama, yang
 * MENGHITUNG ULANG {@code COUNT(*) + 1} atas baris {@code koperasi.anggota_koperasi} setiap kali
 * dipanggil &mdash; tanpa penguncian apa pun, padahal kolom {@link AnggotaKoperasi#getKode()}
 * dipetakan UNIQUE. Dua pendaftaran anggota bersamaan dapat membaca COUNT yang sama dan menyusun
 * kode identik; COUNT itu sendiri juga MUNDUR bila ada baris anggota yang terhapus, sehingga kode
 * yang sama bisa diterbitkan dua kali seiring waktu bahkan tanpa konkurensi. Baris tabel ini
 * menggantikan penghitungan ulang itu dengan satu angka yang dinaikkan atomik satu per satu, dikunci
 * lewat {@code ais.database.hibernate.KunciEntityHelper#jalankanDenganKunci(Class,
 * java.io.Serializable, KunciEntityHelper.PekerjaanTransaksi)} (kunci baris {@code FOR NO KEY UPDATE
 * NOWAIT} + retry, aman lintas node aplikasi) &mdash; pola yang persis sama dengan perbaikan NIS
 * kembar terdahulu, lihat {@link ais.database.model.sekolah.NisCounter}.
 *
 * <h3>Kunci baris: (koperasi, jenis)</h3>
 * <ul>
 * <li>{@code koperasi_id} &mdash; id {@link Koperasi} pemilik anggota, atau sentinel
 * {@link #KOPERASI_TANPA_TENANT} bila anggota belum ditautkan ke koperasi mana pun. Kolom ini
 * SENGAJA bukan {@code @ManyToOne}/foreign key sungguhan ke {@code koperasi.koperasi} &mdash;
 * sentinel {@code 0} tidak menunjuk baris koperasi yang benar-benar ada, dan memaksa FK di sini akan
 * menolak baris pencacah bagi anggota tanpa koperasi (mayoritas pemanggil saat ini, mis. pendaftaran
 * member lewat API kantin/elearning, tidak menautkan koperasi sama sekali).</li>
 * <li>{@code jenis_id} &mdash; id {@link JenisAnggotaKoperasi} untuk pencacah NOMOR URUT PER JENIS,
 * atau sentinel {@link #JENIS_GLOBAL} untuk pencacah NOMOR URUT GLOBAL (lintas jenis, dalam lingkup
 * koperasi yang sama). Kedua nomor pada format kode member lama ({@code urutanPerJenis} dan
 * {@code urutanGlobal}) tetap dipertahankan sebagai DUA baris pencacah terpisah dengan kunci ini.</li>
 * </ul>
 *
 * <p><b>Catatan cakupan tenant:</b> versi lama menghitung {@code COUNT(*)} LINTAS SELURUH TENANT
 * tanpa menyaring koperasi maupun jenis untuk nomor urut global. Menautkan pencacah pada
 * {@code koperasi_id} membuat nomor urut BERMAKNA per koperasi bagi anggota yang koperasinya sudah
 * ditautkan, tanpa mengubah satu pun kode member yang sudah terlanjur terbit &mdash; string
 * {@link AnggotaKoperasi#getKode()} yang tersimpan tidak pernah ditulis ulang oleh perubahan ini.
 * Anggota yang belum ditautkan ke koperasi tetap berbagi SATU pencacah sentinel bersama, meniru
 * perilaku lama (global) untuk populasi tersebut sampai jalur pendaftarannya ikut menautkan koperasi.</p>
 *
 * <p><b>Bukan {@code @Audited}, dan itu disengaja</b> &mdash; alasan sama dengan
 * {@link ais.database.model.sekolah.NisCounter}: baris ini murni pencacah teknis, bukan data bisnis
 * yang perlu riwayat perubahan.</p>
 *
 * @see AnggotaKoperasi#generateKodeMember(org.hibernate.Session, java.util.Date)
 * @see ais.database.hibernate.KunciEntityHelper
 */
@Entity
@Table(schema = "koperasi", name = "anggota_koperasi_kode_counter",
		uniqueConstraints = @UniqueConstraint(name = "uq_anggota_koperasi_kode_counter_koperasi_jenis",
				columnNames = { "koperasi_id", "jenis_id" }))
public class AnggotaKoperasiKodeCounter {

	private Long id;
	private Long koperasiId;
	private Long jenisId;
	private Long nilai;

	/**
	 * Sentinel {@code koperasi_id} bagi anggota yang belum ditautkan ke {@link Koperasi} mana pun.
	 * BUKAN id koperasi yang sah (id sungguhan dimulai dari 1 via {@code IDENTITY}), sehingga aman
	 * dipakai sebagai penanda "tanpa tenant" tanpa risiko bentrok dengan koperasi nyata.
	 */
	public static final long KOPERASI_TANPA_TENANT = 0L;

	/**
	 * Sentinel {@code jenis_id} untuk baris pencacah NOMOR URUT GLOBAL (bukan per jenis anggota).
	 * BUKAN id jenis anggota yang sah, dengan alasan yang sama dengan {@link #KOPERASI_TANPA_TENANT}.
	 */
	public static final long JENIS_GLOBAL = 0L;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/** Id {@link Koperasi} pemilik pencacah ini, atau {@link #KOPERASI_TANPA_TENANT}. */
	@Column(name = "koperasi_id", nullable = false)
	public Long getKoperasiId() {
		return koperasiId;
	}

	public void setKoperasiId(Long koperasiId) {
		this.koperasiId = koperasiId;
	}

	/** Id {@link JenisAnggotaKoperasi} untuk pencacah per-jenis, atau {@link #JENIS_GLOBAL}. */
	@Column(name = "jenis_id", nullable = false)
	public Long getJenisId() {
		return jenisId;
	}

	public void setJenisId(Long jenisId) {
		this.jenisId = jenisId;
	}

	/**
	 * Nilai pencacah saat ini &mdash; nomor urut TERAKHIR yang sudah diterbitkan pada lingkup
	 * (koperasi, jenis) ini. Nomor urut BERIKUTNYA adalah nilai ini ditambah satu; kenaikan dilakukan
	 * atomik oleh pemanggil di dalam blok terkunci
	 * {@code KunciEntityHelper.jalankanDenganKunci(AnggotaKoperasiKodeCounter.class, id, ...)}.
	 */
	@Column(name = "nilai", nullable = false)
	public Long getNilai() {
		return nilai == null ? 0L : nilai;
	}

	public void setNilai(Long nilai) {
		this.nilai = nilai;
	}
}
