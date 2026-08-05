package ais.database.model.spi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * <h2>PenugasanAuditSPI &mdash; Surat Tugas/Pelaksanaan Satu Kegiatan Audit</h2>
 *
 * <p>
 * Kelas ini merepresentasikan SATU pelaksanaan audit nyata di lapangan &mdash; dokumen yang di
 * dunia audit internal setara dengan "Surat Tugas" atau "Lembar Kerja Audit". Satu baris di sini
 * adalah satu kali SPI benar-benar terjun mengaudit satu unit kerja ({@link SatuanKerja}) dengan
 * satu jenis audit tertentu ({@link JenisAuditSPI}), lengkap dengan tim yang ditugaskan
 * ({@link TimAuditSPI}) dan temuan-temuan yang dihasilkan ({@link TemuanAuditSPI}). Kelas ini
 * adalah muara dari seluruh data yang dibangun pada Bagian A (checklist) dan Bagian B (rencana
 * berbasis risiko) &mdash; lihat {@link #getRencanaAuditTahunanSPI()} untuk tautan opsional ke
 * rencana yang mendasarinya.
 * </p>
 *
 * <h3>Mengapa {@code extends DataSop}: alur persetujuan otomatis lewat mesin SOP</h3>
 * <p>
 * Kelas ini SENGAJA dibuat turunan {@link DataSop} (bukan entity biasa) agar penugasan audit
 * otomatis mendapat alur persetujuan berjenjang lewat mesin SOP/Disposisi yang SUDAH ADA dan
 * terbukti di produksi &mdash; sama seperti {@code ais.database.model.spmi.HasilSPMI} pada modul
 * Audit Mutu Internal akademik. Dengan begitu, TIDAK PERLU menulis kode routing/persetujuan baru
 * sama sekali: cukup {@link ais.action.master.spi.PenugasanAuditSPIAction} mengimplementasikan
 * antarmuka {@code ais.ui.util.FormSop}, dan mesin SOP yang sudah ada akan memanggilnya secara
 * refleksi setiap kali dokumen ini perlu ditampilkan/disetujui di sepanjang alur berjenjang yang
 * dikonfigurasi admin SOP.
 * </p>
 *
 * <h3>Prinsip Three Lines Model: alur persetujuan HARUS independen dari unit yang diaudit</h3>
 * <p>
 * Berbeda dari kebanyakan dokumen lain di aplikasi ini yang alur persetujuannya mengikuti hierarki
 * struktural organisasi biasa (mis. atasan langsung), praktik terbaik audit internal (IIA's Three
 * Lines Model) mensyaratkan SPI sebagai "lini pertahanan ketiga" yang independen dari struktur yang
 * diaudit &mdash; sehingga alur persetujuan penugasan audit semestinya diarahkan ke Yayasan/Senat/
 * Dewan Pengawas, BUKAN ke Rektor/Kepala Sekolah unit yang justru sedang diperiksa. Kelas ini
 * sendiri tidak memaksakan rute tertentu secara terprogram &mdash; rute persetujuan sepenuhnya
 * berupa KONFIGURASI DATA pada layar admin SOP ({@code Sop}/{@code AlurSop}), bukan kode. Ini
 * sengaja dibuat fleksibel karena struktur pengawasan riil berbeda-beda antar lembaga (ada yang
 * punya Dewan Pengawas formal, ada yang cukup Ketua Yayasan), namun operator WAJIB diingatkan saat
 * konfigurasi awal untuk TIDAK mengarahkan alur ke posisi yang justru diaudit.
 * </p>
 *
 * <h3>Auditee terstruktur ({@link SatuanKerja}), tim auditor terstruktur ({@link TimAuditSPI})</h3>
 * <p>
 * Field {@link #getSatuanKerja()} (bukan kolom teks bebas) menjamin auditee SELALU merujuk unit
 * organisasi yang benar-benar ada di data resmi &mdash; mencegah salah ketik nama unit yang lazim
 * terjadi bila dipakai kolom teks bebas, sekaligus otomatis memungkinkan laporan/dasbor
 * direkapitulasi per unit dengan akurat. Demikian pula, siapa saja yang bertugas dalam satu
 * penugasan TIDAK disimpan sebagai satu nama teks tunggal, melainkan lewat relasi many-to-many ke
 * {@link Tbmuser} lewat tabel {@link TimAuditSPI} &mdash; sehingga satu penugasan bisa punya banyak
 * anggota tim dengan peran berbeda (Ketua Tim, Anggota), dan riwayat siapa saja yang pernah menjadi
 * auditor tetap tercatat secara terstruktur untuk keperluan rotasi/independensi auditor di kemudian
 * hari.
 * </p>
 *
 * @author e-Campus SPI Team
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "penugasan_audit_spi")
public class PenugasanAuditSPI extends DataSop {

	public static final String PENGAJUAN = "Pengajuan";
	public static final String DISETUJU = "Disetujui";
	public static final String DITOLAK = "Ditolak";

	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		return id + "-" + nama;
	}

	private DisposisiSop disposisiSop;
	private RencanaAuditTahunanSPI rencanaAuditTahunanSPI;
	private JenisAuditSPI jenisAuditSPI;
	private SatuanKerja satuanKerja;
	private String nama;
	private String keterangan;
	private Date tanggalMulai;
	private Date tanggalSelesai;
	private Boolean aktif;
	private String status;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;
	private Date tanggalPembuatan;
	private Date tanggalPersetujuan;
	private Date tanggalPersetujuanManual;

	public PenugasanAuditSPI() {
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

	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? "" : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "rencana_audit_tahunan_spi", nullable = true)
	public RencanaAuditTahunanSPI getRencanaAuditTahunanSPI() {
		rencanaAuditTahunanSPI = check(rencanaAuditTahunanSPI);
		return rencanaAuditTahunanSPI;
	}

	public void setRencanaAuditTahunanSPI(RencanaAuditTahunanSPI rencanaAuditTahunanSPI) {
		this.rencanaAuditTahunanSPI = rencanaAuditTahunanSPI;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_audit_spi", nullable = false)
	public JenisAuditSPI getJenisAuditSPI() {
		jenisAuditSPI = check(jenisAuditSPI);
		return jenisAuditSPI;
	}

	public void setJenisAuditSPI(JenisAuditSPI jenisAuditSPI) {
		this.jenisAuditSPI = jenisAuditSPI;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = false)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalMulai() {
		return tanggalMulai == null ? WaktuUtil.getDate() : tanggalMulai;
	}

	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_selesai")
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
					&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
				dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/spi/PenugasanAuditSPI.java:getDibuatOleh-lazy");
		}
		return dibuatOleh;
	}

	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				disetujuiOleh = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/spi/PenugasanAuditSPI.java:getDisetujuiOleh-lazy");
		}

		disetujuiOleh = check(disetujuiOleh);
		if (getTanggalPersetujuanManual() != null && disetujuiOleh != null) {
			tanggalPersetujuan = getTanggalPersetujuanManual();
		}

		return disetujuiOleh;
	}

	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/spi/PenugasanAuditSPI.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan == null ? new Date() : tanggalPembuatan;
	}

	public String getStatus() {
		if (getDisetujuiOleh() != null) {
			status = DISETUJU;
		} else if (status != null && status.equals(DISETUJU)) {
			status = PENGAJUAN;
		}

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			disposisiSop = getDisposisiSop();
			if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
					&& disposisiSop.getDisposisiEnd().getAlurSop() != null
					&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
				status = DITOLAK;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/spi/PenugasanAuditSPI.java:getStatus-lazy");
		}

		return status == null || status.trim().isEmpty() ? PENGAJUAN : status;
	}

	public void setStatus(String status) {
		if (status != null && status.equals(DITOLAK)) {
			setDisetujuiOleh(null);
			setTanggalPersetujuan(null);
		}
		this.status = status;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPersetujuanManual() {
		return tanggalPersetujuanManual;
	}

	public void setTanggalPersetujuanManual(Date tanggalPersetujuanManual) {
		this.tanggalPersetujuanManual = tanggalPersetujuanManual;
	}

}
