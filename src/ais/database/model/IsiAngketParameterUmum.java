package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.ChecklistBaruPenilaianGuruOlehSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;

/**
 * Entitas Hibernate untuk satu pengisian angket/kuesioner berparameter umum — dipetakan ke tabel
 * {@code public.isi_angket_parameter_umum}. "Umum" di sini berarti satu skema angket generik ini
 * dipakai lintas jenis pengisi ({@link Mahasiswa}, {@link Siswa}, {@link Dosen}, {@link Guru},
 * atau {@link Tbmuser} untuk pengguna umum/admin) dan lintas jenis penilaian (checklist dosen oleh
 * mahasiswa, checklist guru oleh siswa, atau jadwal checklist penilaian umum lain) — HANYA SATU
 * dari relasi pengisi/relasi konteks penilaian yang relevan aktif per baris.
 *
 * <h2>"Pemilik khusus" vs {@link #tbmuser} (lihat {@link #setTbmuser(Tbmuser)})</h2>
 * <p>
 * Kolom {@code tbmuser} HANYA dipakai untuk pengguna umum/admin yang benar-benar berupa baris di
 * tabel {@code tbmuser} — bila salah satu dari {@link #mahasiswa}, {@link #siswa}, {@link #dosen},
 * atau {@link #guru} terisi ("pemilik khusus"), {@link #getTbmuser()} SELALU dipaksa {@code null}
 * lewat {@link #clearTbmuserJikaAdaPemilikKhusus()} (dipanggil dari keempat setter pemilik khusus
 * tsb). {@link #setTbmuser(Tbmuser)} juga menolak {@link Tbmuser} yang sebenarnya cuma
 * wrapper/pseudo (mis. {@code new Tbmuser(mahasiswa)} — dideteksi lewat
 * {@link #isTbmuserWrapperPeserta(Tbmuser)}) agar tidak melanggar FK {@code tbmuser} sungguhan.
 * </p>
 *
 * <h2>Nama default &amp; parameter tambahan berformat teks</h2>
 * <p>
 * {@link #getNama()} membangkitkan nama default lewat {@link #buildNamaDefault()} bila belum diisi
 * manual (kombinasi konteks penilaian + identitas pengisi, dipotong maksimal 250 karakter). Pola
 * penyimpanan {@link #parameterTambahan}/{@link #parameterTambahanInds} sebagai string {@code TEXT}
 * multi-baris berdelimiter {@code "<=>"} identik dengan
 * {@link ais.database.model.payroll.CutiDanIzin} — lihat Javadoc kelas itu untuk detail formatnya;
 * di sini jenis parameter ("jenis") dibedakan lewat prefiks {@code "DOSEN:"}/{@code "GURU:"} atau
 * id grup checklist penilaian umum, lihat {@link #buildJenis(Row, ParameterTambahan)}.
 * </p>
 *
 * <h2>RENTAN terhadap tabrakan namespace {@code jenis} lampiran (perluasan task_484d4bd0)</h2>
 * <p>
 * <b>Kelas ini adalah salah satu dari beberapa kandidat yang belum diverifikasi untuk pola
 * tabrakan namespace {@code LampiranLain} yang sudah ditambal di ~90 titik lain</b> (lihat
 * {@link ais.database.model.file.LampiranLain#resolveJenisParameterTambahan(Class, Long, String)}).
 * Setelah membaca kode, kelas ini TERBUKTI masih memakai jalur RENTAN:
 * {@link #populateParameterTambahan(List)} memanggil
 * {@code LampiranLain.ambil(getId(), jenis)} secara LANGSUNG dengan {@code jenis} mentah dari
 * {@link #buildJenis(Row, ParameterTambahan)} — TIDAK melalui
 * {@code LampiranLain.resolveJenisParameterTambahan(IsiAngketParameterUmum.class, ref, jenisMentah)}
 * yang menambahkan namespace nama kelas pemilik. Untuk cabang grup checklist penilaian umum,
 * {@code jenis} bahkan berbentuk paling telanjang: {@code "<idGrupChecklistPenilaianUmum>-><idParameterTambahan>"}
 * tanpa marker apa pun. Karena tabel {@code lampiran_lain} memakai pasangan ({@code ref}, {@code jenis})
 * bersama untuk SEMUA entitas pemilik, baris {@link IsiAngketParameterUmum} dengan id X dan
 * {@code jenis} tertentu dapat bertabrakan dengan baris entitas LAIN yang kebetulan memiliki id X
 * yang sama dan membentuk {@code jenis} dengan format serupa (mis. {@code CutiDanIzin} atau
 * entitas lain yang memakai pola parameter-tambahan-berformat-teks yang sama) — berpotensi
 * menampilkan atau menimpa URL lampiran milik baris entitas lain. Ini BUKAN kerentanan baru,
 * melainkan konfirmasi bahwa {@code IsiAngketParameterUmum} termasuk kandidat yang masih rentan
 * dari daftar task_484d4bd0; perbaikannya mengikuti pola yang sama seperti ~90 titik lain: ganti
 * pemanggilan {@code LampiranLain.ambil(getId(), jenis)} pada
 * {@link #populateParameterTambahan(List)} menjadi memakai
 * {@code resolveJenisParameterTambahan(IsiAngketParameterUmum.class, getId(), jenis)} terlebih
 * dahulu.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "isi_angket_parameter_umum")
public class IsiAngketParameterUmum extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk {@code IsiAngketParameterUmum}. Dikunci agar objek
	 * yang sudah pernah diserialisasi tetap dapat dibaca setelah kelas ini diubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Id baris (primary key {@code public.isi_angket_parameter_umum.id}), diisi otomatis oleh basis data. */
	private Long id;
	/**
	 * Nama pengguna/proses yang menyimpan pengisian angket ini. Boleh {@code null}; bila diisi
	 * lewat {@link #setOleh(String)}, teks kosong/hanya-spasi diabaikan diam-diam.
	 */
	private String oleh;
	/**
	 * Id pengguna/proses yang menyimpan pengisian angket ini, pasangan dari {@link #oleh}. Boleh
	 * {@code null}; bila diisi lewat {@link #setOlehId(String)}, teks kosong/hanya-spasi diabaikan
	 * diam-diam.
	 */
	private String olehId;
	/**
	 * Waktu baris ini terakhir dibuat/diubah. Diinisialisasi ke waktu saat ini pada deklarasi
	 * field, sehingga sudah terisi begitu objek dibuat dengan {@code new IsiAngketParameterUmum()},
	 * sebelum {@link #onPersist()}/{@link #onUpdate()} sempat berjalan.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Pengisi angket sebagai mahasiswa; bila terisi, {@link #tbmuser} otomatis dikosongkan (lihat "Pemilik khusus" pada Javadoc kelas). */
	private Mahasiswa mahasiswa;
	/** Pengisi angket sebagai dosen; lihat catatan "Pemilik khusus" pada Javadoc kelas. */
	private Dosen dosen;
	/** Pengisi angket sebagai siswa; lihat catatan "Pemilik khusus" pada Javadoc kelas. */
	private Siswa siswa;
	/** Pengisi angket sebagai guru; lihat catatan "Pemilik khusus" pada Javadoc kelas. */
	private Guru guru;
	/** Pengisi angket sebagai pengguna umum/admin (BUKAN mahasiswa/siswa/dosen/guru) — lihat {@link #setTbmuser(Tbmuser)}. */
	private Tbmuser tbmuser;
	/** Jadwal checklist penilaian umum yang menjadi konteks pengisian ini (wajib/non-null di kolom DB). */
	private JadwalChecklistPenilaianUmum jadwalChecklistPenilaianUmum;
	/** Konteks penilaian dosen oleh mahasiswa, bila angket ini bagian dari checklist tsb. */
	private ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa;
	/** Konteks penilaian guru oleh siswa, bila angket ini bagian dari checklist tsb. */
	private ChecklistBaruPenilaianGuruOlehSiswa checklistBaruPenilaianGuruOlehSiswa;
	/** Nama tampilan pengisian angket ini; dibangkitkan otomatis oleh {@link #buildNamaDefault()} bila kosong. */
	private String nama;
	/** Lihat "Nama default & parameter tambahan berformat teks" pada Javadoc kelas — bentuk dengan label lengkap. */
	private String parameterTambahan;
	/** Lihat "Nama default & parameter tambahan berformat teks" pada Javadoc kelas — bentuk ringkas dengan id/jenis saja. */
	private String parameterTambahanInds;

	/** Constructor kosong; seluruh field diisi lewat setter oleh pemanggil (aksi/listener ZK). */
	public IsiAngketParameterUmum() {
	}

	/**
	 * @return id baris ini; {@code null} sebelum baris pernah disimpan (nilai dibangkitkan
	 *         basis data lewat {@code IDENTITY}, kolom tidak {@code insertable})
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * @param id id baris; dalam praktiknya tidak berpengaruh saat insert karena kolom
	 *           {@code id} bertanda {@code insertable = false} (nilai selalu dari basis data)
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return id pengguna/proses yang menyimpan pengisian angket ini, lihat {@link #olehId} */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * @param olehId nilai baru untuk {@link #olehId}; teks {@code null}, kosong, atau
	 *               hanya-spasi diabaikan diam-diam sehingga nilai lama (bila ada) tetap
	 *               dipertahankan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** @return nama pengguna/proses yang menyimpan pengisian angket ini, lihat {@link #oleh} */
	public String getOleh() {
		return oleh;
	}

	/**
	 * @param oleh nilai baru untuk {@link #oleh}; teks {@code null}, kosong, atau hanya-spasi
	 *             diabaikan diam-diam sehingga nilai lama (bila ada) tetap dipertahankan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Callback JPA {@code @PrePersist}: memastikan field wajib terisi sebelum baris pertama
	 * kali disimpan. Lihat {@link #pastikanFieldWajibTerisi()}.
	 */
	@javax.persistence.PrePersist
	protected void onPersist() {
		pastikanFieldWajibTerisi();
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memastikan field wajib tetap terisi pada setiap
	 * pembaruan, lalu mencatat waktu perubahan lewat {@code AuditTimestampInterceptor}. Lihat
	 * {@link #pastikanFieldWajibTerisi()}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		pastikanFieldWajibTerisi();
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** @return {@link #tanggal_dirubah} */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @param tanggal_dirubah nilai baru untuk {@link #tanggal_dirubah} */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Dipanggil dari {@link #onPersist()} dan {@link #onUpdate()} untuk memastikan invarian
	 * baris ini terjaga sebelum disimpan: memaksa {@link #tbmuser} kosong bila ada "pemilik
	 * khusus" (lihat {@link #clearTbmuserJikaAdaPemilikKhusus()}), mengisi {@link #nama} dari
	 * {@link #buildNamaDefault()} bila masih kosong, dan mengganti {@link #parameterTambahan}/
	 * {@link #parameterTambahanInds} yang {@code null} menjadi string kosong (kolom {@code TEXT}
	 * NOT NULL secara konvensi, walau tidak ditandai {@code nullable = false} di anotasi).
	 */
	private void pastikanFieldWajibTerisi() {
		clearTbmuserJikaAdaPemilikKhusus();
		if (nama == null || nama.trim().isEmpty()) {
			nama = buildNamaDefault();
		}
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}
	}

	/**
	 * @return nama default: {@code "Angket Parameter Umum"} ditambah konteks penilaian
	 *         (Dosen/Guru/Jadwal beserta id-nya) dan identitas pengisi (MHS/SISWA/DOSEN/GURU/USER
	 *         beserta id/username), dipotong maksimal 250 karakter. Exception saat membaca
	 *         relasi apa pun diabaikan secara aman (bagian nama tsb cukup dilewati).
	 */
	private String buildNamaDefault() {
		StringBuilder sb = new StringBuilder("Angket Parameter Umum");
		try {
			if (checklistBaruPenilaianDosenOlehMahasiswa != null
					&& checklistBaruPenilaianDosenOlehMahasiswa.getId() != null) {
				sb.append(" Dosen #").append(checklistBaruPenilaianDosenOlehMahasiswa.getId());
			} else if (checklistBaruPenilaianGuruOlehSiswa != null
					&& checklistBaruPenilaianGuruOlehSiswa.getId() != null) {
				sb.append(" Guru #").append(checklistBaruPenilaianGuruOlehSiswa.getId());
			} else if (jadwalChecklistPenilaianUmum != null && jadwalChecklistPenilaianUmum.getId() != null) {
				sb.append(" Jadwal #").append(jadwalChecklistPenilaianUmum.getId());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/IsiAngketParameterUmum.java:139");
		}

		try {
			if (mahasiswa != null && mahasiswa.getId() != null) {
				sb.append(" - MHS #").append(mahasiswa.getId());
			} else if (siswa != null && siswa.getId() != null) {
				sb.append(" - SISWA #").append(siswa.getId());
			} else if (dosen != null && dosen.getId() != null) {
				sb.append(" - DOSEN #").append(dosen.getId());
			} else if (guru != null && guru.getId() != null) {
				sb.append(" - GURU #").append(guru.getId());
			} else if (tbmuser != null && tbmuser.getUserId() != null) {
				sb.append(" - USER " ).append(tbmuser.getUserId());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/IsiAngketParameterUmum.java:154");
		}

		String value = sb.toString();
		return value.length() > 250 ? value.substring(0, 250) : value;
	}

	/**
	 * @return {@link #nama}, dibangkitkan otomatis lewat {@link #buildNamaDefault()} bila belum
	 *         pernah diisi manual atau masih kosong/hanya-spasi (kolom {@code NOT NULL})
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		if (nama == null || nama.trim().isEmpty()) {
			nama = buildNamaDefault();
		}
		return nama;
	}

	/** @param nama nilai baru untuk {@link #nama}; boleh {@code null}/kosong, akan diganti default saat dibaca atau disimpan */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return {@link #mahasiswa}, setelah dipastikan bukan proxy Hibernate basi lewat
	 *         {@code check(...)} milik kelas induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * @param mahasiswa pengisi angket sebagai mahasiswa; bila terisi, {@link #tbmuser} otomatis
	 *                  dikosongkan lewat {@link #clearTbmuserJikaAdaPemilikKhusus()} (lihat
	 *                  "Pemilik khusus" pada Javadoc kelas)
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
		clearTbmuserJikaAdaPemilikKhusus();
	}

	/**
	 * @return {@link #dosen}, setelah dipastikan bukan proxy Hibernate basi lewat
	 *         {@code check(...)} milik kelas induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * @param dosen pengisi angket sebagai dosen; bila terisi, {@link #tbmuser} otomatis
	 *              dikosongkan lewat {@link #clearTbmuserJikaAdaPemilikKhusus()} (lihat
	 *              "Pemilik khusus" pada Javadoc kelas)
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
		clearTbmuserJikaAdaPemilikKhusus();
	}

	/**
	 * @return {@link #siswa}, setelah dipastikan bukan proxy Hibernate basi lewat
	 *         {@code check(...)} milik kelas induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * @param siswa pengisi angket sebagai siswa; bila terisi, {@link #tbmuser} otomatis
	 *              dikosongkan lewat {@link #clearTbmuserJikaAdaPemilikKhusus()} (lihat
	 *              "Pemilik khusus" pada Javadoc kelas)
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
		clearTbmuserJikaAdaPemilikKhusus();
	}

	/**
	 * @return {@link #guru}, setelah dipastikan bukan proxy Hibernate basi lewat
	 *         {@code check(...)} milik kelas induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = true)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * @param guru pengisi angket sebagai guru; bila terisi, {@link #tbmuser} otomatis
	 *             dikosongkan lewat {@link #clearTbmuserJikaAdaPemilikKhusus()} (lihat
	 *             "Pemilik khusus" pada Javadoc kelas)
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
		clearTbmuserJikaAdaPemilikKhusus();
	}

	/**
	 * @return {@link #tbmuser}, setelah dipastikan bukan proxy Hibernate basi lewat
	 *         {@code check(...)} milik kelas induk. Selalu {@code null} bila baris ini punya
	 *         "pemilik khusus" (lihat {@link #memilikiPemilikKhusus()} dan Javadoc kelas).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * @param tbmuser pengguna umum/admin pengisi angket; diabaikan (di-set {@code null}) bila baris
	 *                ini sudah punya "pemilik khusus" ({@link #memilikiPemilikKhusus()}) atau bila
	 *                {@code tbmuser} sebenarnya wrapper/pseudo peserta (lihat "Pemilik khusus" pada
	 *                Javadoc kelas)
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		/*
		 * Kolom tbmuser hanya dipakai untuk pengguna umum/admin yang benar-benar
		 * ada di tabel tbmuser. Untuk mahasiswa/siswa/dosen/guru, relasi khusus
		 * sudah disimpan pada kolom masing-masing. Jika object Tbmuser adalah
		 * wrapper/pseudo seperti new Tbmuser(mahasiswa) atau new Tbmuser(siswa),
		 * jangan disimpan ke kolom tbmuser karena bisa melanggar FK tbmuser.
		 */
		if (memilikiPemilikKhusus() || isTbmuserWrapperPeserta(tbmuser)) {
			this.tbmuser = null;
			return;
		}
		this.tbmuser = tbmuser;
	}

	/**
	 * Memaksa {@link #tbmuser} menjadi {@code null} bila baris ini sudah punya "pemilik
	 * khusus" ({@link #memilikiPemilikKhusus()}). Dipanggil dari keempat setter pemilik khusus
	 * ({@link #setMahasiswa(Mahasiswa)}, {@link #setDosen(Dosen)}, {@link #setSiswa(Siswa)},
	 * {@link #setGuru(Guru)}) dan dari {@link #pastikanFieldWajibTerisi()} sebelum baris
	 * disimpan, sehingga kombinasi pemilik-khusus + tbmuser tidak mungkin tersimpan ke basis
	 * data walau urutan pemanggilan setter berbeda-beda.
	 */
	private void clearTbmuserJikaAdaPemilikKhusus() {
		if (memilikiPemilikKhusus()) {
			this.tbmuser = null;
		}
	}

	/**
	 * @return {@code true} bila salah satu dari {@link #mahasiswa}, {@link #siswa},
	 *         {@link #dosen}, atau {@link #guru} terisi — menandakan baris ini punya "pemilik
	 *         khusus" sehingga {@link #tbmuser} tidak boleh dipakai (lihat Javadoc kelas)
	 */
	private boolean memilikiPemilikKhusus() {
		return mahasiswa != null || siswa != null || dosen != null || guru != null;
	}

	/** @return {@code true} bila {@code user} sebenarnya {@link Tbmuser} wrapper/pseudo yang membungkus mahasiswa/siswa/dosen/guru (lihat {@link Tbmuser#getMahasiswa()} dkk.), bukan baris {@code tbmuser} sungguhan — {@code false} juga bila terjadi exception saat pengecekan. */
	private boolean isTbmuserWrapperPeserta(Tbmuser user) {
		if (user == null) {
			return false;
		}
		try {
			return user.getMahasiswa() != null || user.getSiswa() != null || user.ambilDosen() != null
					|| user.ambilGuru() != null;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * @return {@link #jadwalChecklistPenilaianUmum}, setelah dipastikan bukan proxy Hibernate
	 *         basi lewat {@code check(...)} milik kelas induk (kolom {@code NOT NULL})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_checklist_penilaian_umum", nullable = false)
	public JadwalChecklistPenilaianUmum getJadwalChecklistPenilaianUmum() {
		jadwalChecklistPenilaianUmum = check(jadwalChecklistPenilaianUmum);
		return jadwalChecklistPenilaianUmum;
	}

	/** @param jadwalChecklistPenilaianUmum nilai baru untuk {@link #jadwalChecklistPenilaianUmum} */
	public void setJadwalChecklistPenilaianUmum(JadwalChecklistPenilaianUmum jadwalChecklistPenilaianUmum) {
		this.jadwalChecklistPenilaianUmum = jadwalChecklistPenilaianUmum;
	}

	/**
	 * @return {@link #checklistBaruPenilaianDosenOlehMahasiswa}, setelah dipastikan bukan
	 *         proxy Hibernate basi lewat {@code check(...)} milik kelas induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "checklist_baru_penilaian_dosen_oleh_mahasiswa", nullable = true)
	public ChecklistBaruPenilaianDosenOlehMahasiswa getChecklistBaruPenilaianDosenOlehMahasiswa() {
		checklistBaruPenilaianDosenOlehMahasiswa = check(checklistBaruPenilaianDosenOlehMahasiswa);
		return checklistBaruPenilaianDosenOlehMahasiswa;
	}

	/** @param checklistBaruPenilaianDosenOlehMahasiswa nilai baru untuk {@link #checklistBaruPenilaianDosenOlehMahasiswa} */
	public void setChecklistBaruPenilaianDosenOlehMahasiswa(
			ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa) {
		this.checklistBaruPenilaianDosenOlehMahasiswa = checklistBaruPenilaianDosenOlehMahasiswa;
	}

	/**
	 * @return {@link #checklistBaruPenilaianGuruOlehSiswa}, setelah dipastikan bukan proxy
	 *         Hibernate basi lewat {@code check(...)} milik kelas induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "checklist_baru_penilaian_guru_oleh_siswa", nullable = true)
	public ChecklistBaruPenilaianGuruOlehSiswa getChecklistBaruPenilaianGuruOlehSiswa() {
		checklistBaruPenilaianGuruOlehSiswa = check(checklistBaruPenilaianGuruOlehSiswa);
		return checklistBaruPenilaianGuruOlehSiswa;
	}

	/** @param checklistBaruPenilaianGuruOlehSiswa nilai baru untuk {@link #checklistBaruPenilaianGuruOlehSiswa} */
	public void setChecklistBaruPenilaianGuruOlehSiswa(
			ChecklistBaruPenilaianGuruOlehSiswa checklistBaruPenilaianGuruOlehSiswa) {
		this.checklistBaruPenilaianGuruOlehSiswa = checklistBaruPenilaianGuruOlehSiswa;
	}

	/**
	 * @return {@link #parameterTambahan}, atau string kosong (bukan {@code null}) bila belum
	 *         pernah diisi -- aman dipakai langsung tanpa pengecekan null tambahan
	 */
	@Column(name = "parameter_tambahan", columnDefinition = "text")
	public String getParameterTambahan() {
		return parameterTambahan == null ? "" : parameterTambahan;
	}

	/** @param parameterTambahan nilai baru untuk {@link #parameterTambahan} */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * @return {@link #parameterTambahanInds}, atau string kosong (bukan {@code null}) bila
	 *         belum pernah diisi -- aman dipakai langsung tanpa pengecekan null tambahan
	 */
	@Column(name = "parameter_tambahan_inds", columnDefinition = "text")
	public String getParameterTambahanInds() {
		return parameterTambahanInds == null ? "" : parameterTambahanInds;
	}

	/** @param parameterTambahanInds nilai baru untuk {@link #parameterTambahanInds} */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Mem-parse {@link #getParameterTambahan()} menjadi daftar {@link CommonVO}, satu per baris
	 * teks — lihat "Nama default & parameter tambahan berformat teks" pada Javadoc kelas untuk
	 * format persisnya. Baris kosong dilewati; hasil terurut menurut nomor urut parameter.
	 *
	 * @return daftar parameter tambahan terisi milik pengisian angket ini
	 */
	public List<CommonVO> ambilDataParameterTambahan() {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		String[] splNama = getParameterTambahan().split("\\n");
		for (int j = 0; j < splNama.length; j++) {
			String namaCol = splNama[j] == null ? "" : splNama[j];
			if (namaCol.trim().isEmpty()) {
				continue;
			}
			CommonVO commonVO = new CommonVO();
			String[] value = namaCol.split("<=>");
			String lbl = value.length > 0 ? value[0].trim() : "";
			String url = value.length > 2 ? value[2].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			Integer nomorUrut = Integer.valueOf(1);
			try {
				nomorUrut = value.length > 3 ? Integer.valueOf(Integer.parseInt(value[3].trim())) : Integer.valueOf(1);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/IsiAngketParameterUmum.java:334");
			}
			Long paramId = Long.valueOf(1L);
			try {
				paramId = value.length > 4 ? Long.valueOf(Long.parseLong(value[4].trim())) : Long.valueOf(1L);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/IsiAngketParameterUmum.java:339");
			}
			commonVO.setId(paramId.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setNomorUrut(nomorUrut);
			commonVOs.add(commonVO);
		}
		Collections.sort(commonVOs);
		return commonVOs;
	}

	/**
	 * Membangun ulang {@link #parameterTambahan} dan {@link #parameterTambahanInds} dari baris
	 * komponen ZK ({@code parameterRows}) hasil input pengguna. Setiap {@link Row} diharapkan
	 * membawa attribute {@code parameterTambahan} dan salah satu dari
	 * {@code grupChecklistPenilaianDosen}/{@code grupChecklistPenilaianGuru}/
	 * {@code grupChecklistPenilaianUmum} (dipakai {@link #buildJenis(Row, ParameterTambahan)} dan
	 * {@link #getLabelGrup(Row)} untuk menentukan jenis/label grup) — baris tanpa keduanya
	 * dilewati. Bila {@link ParameterTambahan#getHarusMenyertakanLampiran()} true, method ini juga
	 * mencoba melampirkan URL berkas via {@link LampiranLain#ambil(Long, String)}.
	 *
	 * <p><b>RENTAN terhadap tabrakan namespace {@code jenis} (perluasan task_484d4bd0):</b>
	 * pemanggilan {@code LampiranLain.ambil(getId(), jenis)} di sini memakai {@code jenis}
	 * mentah dari {@link #buildJenis(Row, ParameterTambahan)} TANPA melalui
	 * {@code LampiranLain.resolveJenisParameterTambahan(IsiAngketParameterUmum.class, getId(),
	 * jenis)}. Baris {@link LampiranLain} lain milik entitas berbeda dengan pasangan
	 * ({@code ref}, {@code jenis}) yang kebetulan sama dapat tertukar dengan lampiran milik
	 * pengisian angket ini. Lihat penjelasan lengkap pada Javadoc kelas.</p>
	 *
	 * @param parameterRows baris ZK berisi komponen input parameter tambahan; tidak melakukan apa
	 *                       pun bila {@code null}/kosong
	 */
	public void populateParameterTambahan(List<Row> parameterRows) {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return;
		}

		String parameterTambahanStr = "";
		String parameterTambahanInds = "";
		for (Row row : parameterRows) {
			try {
				ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
				if (parameterTambahan == null) {
					continue;
				}
				String jenis = buildJenis(row, parameterTambahan);
				if (jenis == null || jenis.trim().isEmpty()) {
					continue;
				}

				Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
						&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan") : null);
				String ket = keterangan == null ? "" : keterangan.getValue().trim();
				String val = ParameterTambahan.ambilVal(row, parameterTambahan);

				String url = "";
				if (parameterTambahan.getHarusMenyertakanLampiran()) {
					LampiranLain lam = LampiranLain.ambil(getId(), jenis);
					if (lam != null) {
						try {
							url = lam.createLinkUri();
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}

				String labelGrup = getLabelGrup(row);
				String s = labelGrup + "->" + parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url
						+ "<=>" + parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
						+ jenis + "<=>0<=>" + ket;
				parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

				String sIds = jenis + "<=>" + val + "<=>" + url + "<=>" + ket;
				parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	/**
	 * @return kode "jenis" parameter tambahan untuk {@code row}: {@code "DOSEN:<idGrup>->id"},
	 *         {@code "GURU:<idGrup>->id"}, atau {@code "<idGrupUmum>->id"} tergantung attribute
	 *         grup mana yang ada pada {@code row}; string kosong bila tidak ada satupun.
	 *         <b>Ketiga bentuk ini tidak mengandung marker nama kelas pemilik</b> (berbeda dari
	 *         bentuk ber-namespace {@code ownerClass.getName() + "#" + jenisMentah} yang
	 *         dihasilkan {@link LampiranLain#resolveJenisParameterTambahan(Class, Long, String)});
	 *         nilai kembaliannya dipakai langsung sebagai {@code jenis} pada pemanggilan
	 *         {@code LampiranLain.ambil(getId(), jenis)} di {@link #populateParameterTambahan(List)},
	 *         sehingga rentan tabrakan lintas entitas -- lihat catatan keamanan pada Javadoc kelas.
	 */
	private String buildJenis(Row row, ParameterTambahan parameterTambahan) {
		Object grup = row.getAttribute("grupChecklistPenilaianDosen");
		if (grup instanceof GrupChecklistPenilaianDosen) {
			return "DOSEN:" + ((GrupChecklistPenilaianDosen) grup).getId() + "->" + parameterTambahan.getId();
		}
		grup = row.getAttribute("grupChecklistPenilaianGuru");
		if (grup instanceof ais.database.model.sekolah.GrupChecklistPenilaianGuru) {
			return "GURU:" + ((ais.database.model.sekolah.GrupChecklistPenilaianGuru) grup).getId() + "->" + parameterTambahan.getId();
		}
		grup = row.getAttribute("grupChecklistPenilaianUmum");
		if (grup instanceof GrupChecklistPenilaianUmum) {
			return ((GrupChecklistPenilaianUmum) grup).getId() + "->" + parameterTambahan.getId();
		}
		return "";
	}

	/** @return label tampilan grup checklist untuk {@code row} (prefiks {@code "Dosen:"}/{@code "Guru:"} atau isi grup umum apa adanya), default {@code "Parameter"} bila tidak ada attribute grup yang cocok. */
	private String getLabelGrup(Row row) {
		Object grup = row.getAttribute("grupChecklistPenilaianDosen");
		if (grup instanceof GrupChecklistPenilaianDosen) {
			return "Dosen:" + ((GrupChecklistPenilaianDosen) grup).getIsi();
		}
		grup = row.getAttribute("grupChecklistPenilaianGuru");
		if (grup instanceof ais.database.model.sekolah.GrupChecklistPenilaianGuru) {
			return "Guru:" + ((ais.database.model.sekolah.GrupChecklistPenilaianGuru) grup).getIsi();
		}
		grup = row.getAttribute("grupChecklistPenilaianUmum");
		if (grup instanceof GrupChecklistPenilaianUmum) {
			return ((GrupChecklistPenilaianUmum) grup).getIsi();
		}
		return "Parameter";
	}

	/**
	 * Ekspor entitas ini (mengikuti mekanisme {@code write} umum di {@link GeneralValueObject}),
	 * dengan relasi {@link JadwalChecklistPenilaianUmum}, {@link ChecklistBaruPenilaianDosenOlehMahasiswa},
	 * {@link ChecklistBaruPenilaianGuruOlehSiswa}, dan {@link ParameterTambahan} disertakan sebagai
	 * nama kelas yang boleh di-resolve/di-load bersamaan.
	 *
	 * @return berkas hasil ekspor
	 */
	public File write() {
		return write(JadwalChecklistPenilaianUmum.class.getName(), ChecklistBaruPenilaianDosenOlehMahasiswa.class.getName(),
				ChecklistBaruPenilaianGuruOlehSiswa.class.getName(), ParameterTambahan.class.getName());
	}

	/**
	 * @return {@link #getNama()} (yang membangkitkan nama default bila belum diisi), atau
	 *         string kosong bila hasilnya {@code null}
	 */
	public String toString() {
		String n = getNama();
		return n == null ? "" : n;
	}
}
