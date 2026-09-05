package ais.database.model.recruitment;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.common.KarirConfigUtil;
import ais.database.model.GeneralValueObject;

/**
 * Master syarat kelengkapan berkas pendaftaran calon pegawai — SATU baris mewakili SATU jenis
 * dokumen/berkas yang WAJIB atau OPSIONAL diverifikasi (mis. "Data Diri", "Curriculum Vitae",
 * "Ijazah", "Transkrip", "Sertifikat/Dokumen Pendukung"). Master ini dipasangkan ke gelombang
 * pendaftaran tertentu lewat koleksi {@code GelombangPendaftaranPegawai.getVerifikasiKelengkapanCalonPegawais()}
 * (relasi didefinisikan di {@code GelombangPendaftaranPegawai}, bukan di kelas ini), sehingga syarat
 * yang berlaku bisa berbeda antar gelombang.
 *
 * <p><b>Dua entity detail yang MERUJUK master ini secara independen.</b> Baris master di sini dipakai
 * oleh DUA mekanisme terpisah yang sama-sama merujuk pasangan {@code (calonPegawai, baris-master-ini)}
 * namun TIDAK saling mereferensi dan bisa tidak sinkron satu sama lain:</p>
 * <ul>
 * <li>{@link CalonPegawaiPunyaVerifikasiBerkas} — checklist boolean sederhana, dibuat OTOMATIS
 * (malas/lazy) saat layar cetak biodata calon pegawai memerlukannya, dengan lampiran bukti lewat
 * {@link ais.database.model.file.LampiranLain}.</li>
 * <li>{@link CalonPegawaiPunyaDokumen} — status tiga-tingkat ({@code BELUM}/{@code REVISI}/{@code
 * VERIFIKASI}), dikelola manual lewat layar admin dokumen, dengan lampiran independen sendiri.</li>
 * </ul>
 *
 * @see #reloadDefault() untuk mekanisme auto-seed baris default saat tabel masih kosong.
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "verifikasi_kelengkapan_calon_pegawai")
public class VerifikasiKelengkapanCalonPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap. Nilai literal ini disalin apa adanya dari boilerplate hbm2java yang
	 * sama dan dipakai berulang di banyak entity recruitment lain (bukan unik per kelas) — tidak
	 * masalah karena Java hanya mencocokkan {@code serialVersionUID} pada saat deserialisasi objek
	 * dari kelas yang SAMA.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris (identity, di-generate database). */
	private Long id;
	/** Nama penampil pengguna yang terakhir mengubah baris ini (audit ringan, bukan FK). */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini (audit ringan, pasangan {@link #oleh}). */
	private String olehId;

	/** @return id pengguna (audit) yang terakhir mengubah baris ini. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Set id pengguna audit; nilai kosong/blank diabaikan (fail-safe) agar baris tidak kehilangan
	 * jejak audit sebelumnya akibat pemanggilan dengan argumen kosong.
	 *
	 * @param olehId id pengguna yang mengubah; {@code null}/kosong tidak melakukan apa-apa
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Set nama pengguna audit; nilai kosong/blank diabaikan (fail-safe), simetris dengan
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna yang mengubah; {@code null}/kosong tidak melakukan apa-apa
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna (audit) yang terakhir mengubah baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook lifecycle JPA yang dipanggil sebelum {@code UPDATE}; mendelegasikan pencatatan timestamp
	 * perubahan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} — field audit
	 * shadow ini adalah KEHARUSAN TEKNIS interceptor Hibernate, bukan sisa kode yang bisa dihapus.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Tanggal terakhir baris ini dirubah; diinisialisasi ke waktu sekarang saat objek dibuat, lalu dimutakhirkan oleh {@link #onUpdate()}. */
private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah timestamp perubahan terakhir (biasanya diisi otomatis oleh {@link #onUpdate()}). */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return timestamp perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas untuk log/debug: {@code "<id>-<nama>"}. */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama jenis dokumen/berkas yang diverifikasi (mis. "Ijazah", "Curriculum Vitae"). */
	private String nama;
	/** Flag aktif/tidak; menentukan apakah syarat ini masih ditawarkan untuk gelombang baru. */
	private Boolean aktif;
	/** Flag wajib/opsional; menentukan apakah dokumen ini WAJIB diunggah/diverifikasi. */
	private Boolean wajib;
	/** Catatan/deskripsi bebas untuk syarat ini. */
	private String keterangan;

	/**
	 * Auto-seed data DEFAULT bila tabel ini masih kosong. Dipanggil pada startup/inisialisasi modul
	 * rekrutmen (bukan dari konstruktor entity). Logika: bila query {@code SELECT ... LIMIT 1} tanpa
	 * filter mengembalikan {@code null} (tabel benar-benar kosong), method ini membuat EMPAT baris
	 * WAJIB ({@code wajib = true}: "Data Diri", "Curriculum Vitae", "Ijazah", "Transkrip", masing-masing
	 * dengan {@code kode} berformat {@code "00" + urut} dan {@code nomorUrut} berurut mulai 1) lalu
	 * SATU baris OPSIONAL ({@code wajib = false}: "Sertifikat/Dokumen Pendukung"), melanjutkan urutan
	 * {@code nomorUrut} dari baris wajib.
	 *
	 * <p><b>Bukan idempoten murni per-nama.</b> Pengecekan "sudah ada atau belum" untuk tiap nama
	 * dilakukan lewat {@code Restrictions.ilike("nama", ss, MatchMode.EXACT)} (case-insensitive, exact
	 * match) SEBELUM membuat baris baru, namun pengecekan keseluruhan hanya dijalankan bila query awal
	 * ({@code setMaxResults(1)} tanpa filter) menemukan tabel kosong — bila tabel SUDAH berisi
	 * setidaknya satu baris apa pun (mis. hasil seed manual sebagian, atau baris dari nama lain), method
	 * ini langsung berhenti tanpa memeriksa keempat/kelima nama satu per satu. Artinya seed HANYA
	 * benar-benar all-or-nothing pada tabel yang kosong; pada tabel yang sudah tidak kosong sama sekali,
	 * method ini menjadi no-op walau salah satu dari kelima nama default belum ada.</p>
	 *
	 * <p><b>Transaksi per-baris, bukan satu transaksi besar.</b> Setiap baris di-{@code save} dalam
	 * transaksi {@code begin()}/{@code commit()} sendiri-sendiri; kegagalan di tengah proses (mis. baris
	 * ke-3 gagal) meninggalkan baris ke-1 dan ke-2 yang sudah ter-commit — seed bisa berhenti dalam
	 * keadaan SEBAGIAN selesai. Exception ditangkap, dicatat ({@code ErrorAuditUtil.record}), dan
	 * transaksi aktif (bila ada) di-rollback; method ini tidak melempar ulang exception ke pemanggil.</p>
	 *
	 * <p>Sesi Hibernate dibuka lewat {@link ais.database.hibernate.HibernateUtil#currentNativeSession()}
	 * dan SELALU ditutup di {@code finally} lewat {@code KarirConfigUtil.closeNativeSession(session)},
	 * termasuk pada jalur sukses maupun gagal.</p>
	 */
	public static void reloadDefault() {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();

			int urut = 1;
			VerifikasiKelengkapanCalonPegawai DATA = (VerifikasiKelengkapanCalonPegawai) session
					.createCriteria(VerifikasiKelengkapanCalonPegawai.class)

					.setMaxResults(1).uniqueResult();

			if (DATA == null) {
				String[] data = new String[] { "Data Diri", "Curriculum Vitae", "Ijazah", "Transkrip" };
				for (String ss : data) {
					DATA = (VerifikasiKelengkapanCalonPegawai) session
							.createCriteria(VerifikasiKelengkapanCalonPegawai.class)
							.add(Restrictions.ilike("nama", ss, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (DATA == null) {
						DATA = new VerifikasiKelengkapanCalonPegawai();
						DATA.setWajib(true);
						DATA.setNama(ss);
						DATA.setKode("00" + urut);
						DATA.setNomorUrut(urut);
						DATA.setKeterangan(ss);
						session.getTransaction().begin();
						session.save(DATA);
						session.getTransaction().commit();
						urut++;
					}
				}

				data = new String[] { "Sertifikat/Dokumen Pendukung" };
				for (String ss : data) {
					DATA = (VerifikasiKelengkapanCalonPegawai) session
							.createCriteria(VerifikasiKelengkapanCalonPegawai.class)
							.add(Restrictions.ilike("nama", ss, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (DATA == null) {
						DATA = new VerifikasiKelengkapanCalonPegawai();
						DATA.setWajib(false);
						DATA.setNama(ss);
						DATA.setNomorUrut(urut);
						DATA.setKode("00" + urut);
						DATA.setKeterangan(ss);
						session.getTransaction().begin();
						session.save(DATA);
						session.getTransaction().commit();
						urut++;
					}
				}
			}

		} catch (Exception e) {
			try {
				if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/recruitment/VerifikasiKelengkapanCalonPegawai.java:138");
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/recruitment/VerifikasiKelengkapanCalonPegawai.java:140");
		} finally {
			KarirConfigUtil.closeNativeSession(session);
		}
	}

	/** Konstruktor default (dibutuhkan Hibernate); semua field bernilai default/null. */
	public VerifikasiKelengkapanCalonPegawai() {
	}

	/** @return primary key baris ini, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baris; biasanya hanya diisi Hibernate, bukan kode aplikasi. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama jenis dokumen, hasil di-{@code trim()}; {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama jenis dokumen/berkas; disimpan apa adanya (trimming dilakukan di getter). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return catatan/deskripsi syarat ini; bisa {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/deskripsi bebas untuk syarat ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Status aktif. Default fail-open: nilai {@code null} (belum pernah diset) dibaca sebagai
	 * {@code true} (aktif) — konsisten dengan pola default di {@link ParameterVerifikasiCalonPegawai}.
	 *
	 * @return {@code true} bila syarat ini aktif/masih ditawarkan; {@code false} bila dinonaktifkan.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif baru; {@code null} akan dibaca sebagai aktif oleh {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Status wajib. Default fail-open: nilai {@code null} (belum pernah diset) dibaca sebagai
	 * {@code true} (wajib) — baris lama yang belum pernah punya kolom ini di-set dianggap wajib,
	 * bukan opsional; ini SISI AMAN untuk syarat kelengkapan (lebih baik keliru mewajibkan daripada
	 * keliru melewatkan dokumen yang sebenarnya wajib).
	 *
	 * @return {@code true} bila dokumen ini wajib diunggah/diverifikasi; {@code false} bila opsional.
	 */
	public Boolean getWajib() {
		return wajib == null ? true : wajib;
	}

	/** @param wajib status wajib baru; {@code null} akan dibaca sebagai wajib oleh {@link #getWajib()}. */
	public void setWajib(Boolean wajib) {
		this.wajib = wajib;
	}

}
