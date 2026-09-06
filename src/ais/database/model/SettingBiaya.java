package ais.database.model;

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

/**
 * Entity Hibernate/JPA untuk tabel {@code public.setting_biaya} — <b>master pengaturan/skema
 * biaya</b>: aturan penyaringan (jenis kegiatan, angkatan, jenjang, status awal mahasiswa,
 * status mahasiswa, program, jenis seleksi, jurusan, gelombang pendaftaran, paket, rentang
 * semester, jenis kelamin, afiliasi calon mahasiswa, tahun akademik/semester) yang menentukan
 * mahasiswa mana yang tercakup satu skema biaya, serta parameter cara penagihan (jumlah
 * pembayaran/termin, prioritas pemilihan, notifikasi, batas waktu, daftar pengecualian NIM).
 *
 * <p>Rincian nominal per termin/item biaya <b>tidak</b> disimpan langsung di sini, melainkan
 * pada baris {@link DetailSettingBiaya} anak yang menunjuk balik ke satu {@code SettingBiaya}
 * ini (satu {@code SettingBiaya} bisa punya banyak {@code DetailSettingBiaya}, satu per
 * kombinasi item-biaya/termin pembayaran).</p>
 *
 * <h4>Pemilihan skema mana yang berlaku untuk seorang mahasiswa</h4>
 * <p>Mesin billing ({@code ais.action.master.helper.SetingBiayaHelper}, dipakai antara lain oleh
 * {@code DetailBiaya}/{@code Kegiatan}/{@code JenisKegiatan} yang sudah didokumentasikan lengkap
 * pada batch sebelumnya) mencari kandidat {@code SettingBiaya} yang kriterianya cocok dengan
 * data mahasiswa/kegiatan, lalu memilih berdasar {@link #getPrioritas()} (angka lebih kecil
 * didahulukan). Kriteria yang bernilai {@code null} pada sebuah {@code SettingBiaya} umumnya
 * berarti "tidak membatasi" (cocok untuk siapa saja pada dimensi itu), sehingga setting dengan
 * kriteria lebih sedikit cenderung menjadi fallback yang lebih luas cakupannya.</p>
 *
 * <h4>Mode "Khusus Buat Mahasiswa Tertentu" dan getter yang menulis balik</h4>
 * <p>Flag {@link #getKhususBuatMahasiswaTertentu()} mengubah SettingBiaya ini dari "skema umum
 * bergantung kriteria" menjadi "skema insidentil khusus untuk mahasiswa yang eksplisit terdaftar
 * di {@link DetailSettingBiaya}/melalui mekanisme di luar entity ini". Saat flag ini {@code
 * true}, sejumlah getter kriteria — {@link #getJenjang()}, {@link #getStatusAwalMahasiswa()},
 * {@link #getProgram()}, {@link #getJenisSeleksi()}, {@link #getJurusan()}, {@link
 * #getGelombangPendaftaran()}, {@link #getPaket()} — <b>menuliskan {@code null} ke field
 * masing-masing</b> setiap kali dipanggil, dan {@link #getGunakanBiayaDefault()} dipaksa
 * menjadi {@code true}. Efeknya permanen pada entity yang <i>attached</i> ke sesi Hibernate
 * aktif: sekadar <b>membaca</b> kriteria-kriteria itu (mis. saat entity ditampilkan di grid)
 * dapat membuat nilai yang sebelumnya tersimpan <b>terhapus permanen</b> dari database begitu
 * flush terjadi — pola arsitektur "getter yang mengubah object lain/dirinya sendiri" yang sudah
 * tercatat berulang pada domain lain di codebase ini (categori sistemik, lihat catatan
 * dokumentasi getter-mutasi-field). Perhatikan bahwa {@link #getAngkatan()} <b>sengaja
 * dikecualikan</b> dari pola ini — kode nullifikasinya untuk field {@code angkatan} ada tetapi
 * dikomentari (mati), sehingga angkatan tetap dipertahankan meski mode khusus menyala; asimetri
 * ini tampak disengaja, bukan alpa.</p>
 *
 * <p>Perubahan tercatat historisnya lewat {@link Audited} (Hibernate Envers).</p>
 *
 * @see DetailSettingBiaya
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "setting_biaya")
public class SettingBiaya extends GeneralValueObject {
	/** ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database). */
	private static final long serialVersionUID = -7050466125892447098L;
	/** Primary key baris {@code setting_biaya}, kolom {@code id} (identity, auto-generate). */
	private Long id;
	/** Nama/username aktor yang membuat/terakhir mengubah baris ini (field audit longgar, bukan FK). */
	private String oleh;
	/** ID aktor yang membuat/terakhir mengubah baris ini (pasangan {@link #oleh}, bukan FK). */
	private String olehId;

	/**
	 * @return ID aktor ({@link #olehId}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID aktor audit. Setter ini <b>fail-closed diam-diam</b>: nilai {@code null} atau
	 * string kosong/berspasi diabaikan sepenuhnya (nilai lama tetap dipertahankan), tanpa
	 * exception maupun log.
	 *
	 * @param olehId ID aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau
	 * kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama aktor ({@link #oleh}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum {@code
	 * UPDATE} dieksekusi, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * untuk memperbarui jejak audit "terakhir diubah" milik entity ini.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu "terakhir diubah"; diinisialisasi ke waktu sekarang saat instance dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu "terakhir diubah" secara manual. Field ini juga diinisialisasi ke
	 * waktu sekarang saat instance dibuat, dan ditulis ulang otomatis oleh {@link #onUpdate()}
	 * setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu terakhir baris ini diubah (kolom timestamp), diisi otomatis oleh
	 *         {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log/debug, menggabungkan seluruh dimensi kriteria utama:
	 * {@code "<jenisKegiatan>_<angkatan>_<jenjang>_<statusAwalMahasiswa>_<program>_<jurusan>_
	 * <gelombangPendaftaran>"}.
	 *
	 * <p><b>Efek samping signifikan:</b> memanggil {@link #getJenisKegiatan()}, {@link
	 * #getJenjang()}, {@link #getStatusAwalMahasiswa()}, {@link #getProgram()}, {@link
	 * #getJurusan()}, {@link #getGelombangPendaftaran()} — pada mode {@link
	 * #getKhususBuatMahasiswaTertentu()} aktif, sekadar memanggil {@code toString()} (mis. dari
	 * log atau debugger) ikut memicu penulisan {@code null} ke lima field kriteria tersebut,
	 * lihat javadoc kelas.</p>
	 *
	 * @return string ringkas identitas kombinasi kriteria baris ini
	 */
	public String toString() {
		jenisKegiatan = getJenisKegiatan();
		jenjang = getJenjang();
		statusAwalMahasiswa = getStatusAwalMahasiswa();
		program = getProgram();
		jurusan = getJurusan();
		gelombangPendaftaran = getGelombangPendaftaran();
		return jenisKegiatan + "_" + angkatan + "_" + jenjang + "_" + statusAwalMahasiswa + "_" + program + "_"
				+ jurusan + "_" + gelombangPendaftaran;
	}


	/** Jenis kegiatan (mis. Her-Registrasi/Wisuda) yang menjadi kriteria pemilihan skema ini. */
	private JenisKegiatan jenisKegiatan;
	/** Kriteria angkatan; berbeda dari kriteria khusus-mahasiswa lain, TIDAK dinolkan oleh mode {@link #getKhususBuatMahasiswaTertentu()} — lihat {@link #getAngkatan()}. */
	private Integer angkatan;
	/** Kriteria jenjang (mis. S1/S2); dinolkan saat {@link #getKhususBuatMahasiswaTertentu()} aktif. */
	private Jenjang jenjang;
	/** Kriteria status awal mahasiswa (mis. Baru-A/Pindahan); dinolkan saat mode khusus aktif. */
	private StatusAwalMahasiswa statusAwalMahasiswa;
	/** Kriteria status mahasiswa saat ini; TIDAK termasuk dalam daftar field yang dinolkan mode khusus. */
	private StatusMahasiswa statusMahasiswa;
	/** Kriteria program studi (string bebas, mis. Reguler/Karyawan); dinolkan saat mode khusus aktif. */
	private String program;
	/** Kriteria jenis seleksi PMB; dinolkan saat mode khusus aktif. */
	private JenisSeleksi jenisSeleksi;
	/** Kriteria jurusan/prodi; dinolkan saat mode khusus aktif. */
	private Jurusan jurusan;
	/** Flag "pakai biaya default" ({@link DetailSettingBiaya#getDefaultBiaya()} sebagai nominal); dipaksa {@code true} saat mode khusus aktif. */
	private Boolean gunakanBiayaDefault;
	/** Flag "tampilkan/nilai per program studi" — mengaktifkan jalur {@link DetailSettingBiaya#ambilDefaultBiaya(Jurusan)}; lihat catatan bug fallback nominal pada method itu. */
	private Boolean tampilkanPerProdi;
	/** Kriteria gelombang pendaftaran PMB; dinolkan saat mode khusus aktif. */
	private GelombangPendaftaran gelombangPendaftaran;
	/** Kriteria paket biaya; dinolkan saat mode khusus aktif. */
	private Paket paket;
	/** Batas bawah semester cakupan skema ini; berlaku bila {@link #getSmtIkutiSettinganDisini()} {@code true}. */
	private Integer minSmt;
	/** Batas atas semester cakupan skema ini; berlaku bila {@link #getSmtIkutiSettinganDisini()} {@code true}. */
	private Integer maxSmt;
	// Bila true: rentang semester (minSmt/maxSmt) DIATUR DI SINI (SettingBiaya),
	// dan rentang semester (minSmt/maxSmt) milik JenisKegiatan TIDAK BERLAKU utk setting ini.
	/** Lihat {@link #getSmtIkutiSettinganDisini()}. */
	private Boolean smtIkutiSettinganDisini;
	/** Jumlah termin/tahap pembayaran skema ini; default 1 bila kosong/kurang dari 1. */
	private Integer jumlahPembayaran;
	/** Flag mode "khusus buat mahasiswa tertentu" — lihat javadoc kelas untuk efek getter-menulis-balik yang ditimbulkannya. */
	private Boolean khususBuatMahasiswaTertentu;
	/** Flag pembatasan billing bulanan hanya untuk mahasiswa terdaftar di {@link DetailSettingBiaya}; lihat {@link #getBatasiMahasiswaTertentu()}. */
	private Boolean batasiMahasiswaTertentu;
	/** Template pesan notifikasi tagihan; dinolkan bila {@link #getAktifkanNotifikasi()} {@code false}. */
	private String templateNotifikasi;
	/** Waktu pengiriman notifikasi tagihan; dinolkan bila {@link #getAktifkanNotifikasi()} {@code false}. */
	private Date waktuNotifikasi;
	/** Flag aktifkan notifikasi tagihan untuk skema ini. */
	private Boolean aktifkanNotifikasi;
	/** Batas waktu pembayaran skema ini. */
	private Date batasWaktuPembayaran;
	/** Kriteria jenis kelamin ({@code "Laki-laki"}/{@code "Perempuan"}); nilai lain dianggap tidak valid, lihat {@link #getKelamin()}. */
	private String kelamin;
	/** Kriteria afiliasi calon mahasiswa. */
	private AfiliasiCalonMahasiswa afiliasiCalonMahasiswa;
	/** ID tahun-ajaran+semester gabungan, diturunkan oleh {@link #getTa()}; jangan diset langsung dari luar formula itu. */
	private Integer ta;
	/** Kriteria semester ({@link Perkuliahan#GENAP}/ganjil), dipakai juga oleh {@link #getTa()}. */
	private String semester;
	/** Kriteria tahun akademik (format {@code "YYYY/YYYY"}), dipakai juga oleh {@link #getTa()}. */
	private String tahunAkademik;
	/** Daftar NIM yang dikecualikan dari skema ini; lihat {@link #getPengecualianMahasiswa()}. */
	private String pengecualianMahasiswa;
	/** Urutan prioritas pemilihan skema; default 10. Lihat {@link #getPrioritas()}. */
	private Integer prioritas = 10;

	/**
	 * @return primary key baris {@code setting_biaya}; {@code null} sebelum baris di-{@code
	 *         INSERT}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key; biasanya tidak perlu diset manual karena kolomnya {@code
	 *           insertable = false} (identity, dibangkitkan database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kriteria angkatan mahasiswa untuk pemilihan skema ini.
	 *
	 * <p><b>Sengaja dikecualikan dari pola nullifikasi mode khusus:</b> badan method ini masih
	 * menyisakan kode yang DIKOMENTARI (mati) — {@code if (getKhususBuatMahasiswaTertentu()) {
	 * angkatan = null; }} — yang bila diaktifkan akan meniru pola nullifikasi tujuh getter lain
	 * (lihat javadoc kelas). Karena baris itu dikomentari, {@link #getAngkatan()} MENGEMBALIKAN
	 * NILAI APA ADANYA tanpa nullifikasi, berbeda dari saudara-saudaranya. Tidak jelas dari kode
	 * semata apakah ini debug sementara yang lupa dihapus atau keputusan sadar bahwa angkatan
	 * tetap relevan meski mode khusus menyala; dicatat apa adanya, tidak diubah di sesi
	 * dokumentasi ini.</p>
	 *
	 * @return angkatan yang tersimpan; boleh {@code null}.
	 */
	@Column(name = "angkatan")
	public Integer getAngkatan() {
//		if (getKhususBuatMahasiswaTertentu()) {
//			angkatan = null;
//		}
		return this.angkatan;
	}

	/**
	 * @param angkatan angkatan baru untuk kriteria skema ini.
	 */
	public void setAngkatan(Integer angkatan) {
		this.angkatan = angkatan;
	}

	/**
	 * @param jenisKegiatan jenis kegiatan baru untuk kriteria skema ini.
	 */
	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	/**
	 * @return jenis kegiatan kriteria skema ini (proxy lazy diresolusi via {@code check()});
	 *         boleh {@code null} (tidak membatasi jenis kegiatan).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kegiatan", nullable = true)
	public JenisKegiatan getJenisKegiatan() {
		jenisKegiatan = check(jenisKegiatan);
		return jenisKegiatan;
	}

	/**
	 * @param jenjang jenjang baru untuk kriteria skema ini.
	 */
	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}

	/**
	 * Kriteria jenjang untuk pemilihan skema ini.
	 *
	 * <p><b>Getter yang menulis balik (mode khusus):</b> bila {@link
	 * #getKhususBuatMahasiswaTertentu()} {@code true}, field {@link #jenjang} ditimpa menjadi
	 * {@code null} setiap kali getter ini dipanggil — lihat javadoc kelas untuk penjelasan
	 * lengkap dan konsekuensinya pada entity yang <i>attached</i>.</p>
	 *
	 * @return jenjang kriteria skema ini (proxy lazy diresolusi via {@code check()}); {@code
	 *         null} bila tidak membatasi jenjang ATAU bila mode khusus sedang aktif.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang", nullable = true)
	public Jenjang getJenjang() {
		jenjang = check(jenjang);
		if (getKhususBuatMahasiswaTertentu()) {
			jenjang = null;
		}
		return jenjang;
	}

	/**
	 * Flag "pakai biaya default" — bila {@code true}, {@code DetailBiaya.getNilaiBiaya()}
	 * memakai {@link DetailSettingBiaya#getDefaultBiaya()} sebagai nominal (cabang prioritas
	 * ketiga/terakhir pada method itu, di bawah per-prodi dan peta JSON per item biaya).
	 *
	 * <p><b>Getter yang menulis balik (mode khusus):</b> bila {@link
	 * #getKhususBuatMahasiswaTertentu()} {@code true}, field {@link #gunakanBiayaDefault}
	 * dipaksa menjadi {@code true} setiap kali getter ini dipanggil — konsisten dengan semantik
	 * mode khusus sebagai "skema insidentil bernilai default/tetap", tetapi tetap mengikuti pola
	 * getter-menulis-balik yang sama seperti tujuh getter kriteria lain (lihat javadoc kelas).</p>
	 *
	 * @return {@code true} bila skema ini memakai biaya default; default {@code true} bila
	 *         field mentah {@code null} DAN mode khusus TIDAK aktif akan tetap true karena
	 *         fallback null-ke-true di akhir method — perhatikan bahwa nilai {@code false} yang
	 *         eksplisit disimpan TETAP dihormati selama mode khusus tidak aktif.
	 */
	public Boolean getGunakanBiayaDefault() {
		if (getKhususBuatMahasiswaTertentu()) {
			gunakanBiayaDefault = true;
		}
		return gunakanBiayaDefault == null ? true : gunakanBiayaDefault;
	}

	/**
	 * @param gunakanBiayaDefault flag baru; bisa tetap ditimpa menjadi {@code true} saat dibaca
	 *                            via {@link #getGunakanBiayaDefault()} bila mode khusus aktif.
	 */
	public void setGunakanBiayaDefault(Boolean gunakanBiayaDefault) {
		this.gunakanBiayaDefault = gunakanBiayaDefault;
	}

	/**
	 * Kriteria status awal mahasiswa untuk pemilihan skema ini.
	 *
	 * <p><b>Getter yang menulis balik (mode khusus):</b> sama seperti {@link #getJenjang()} —
	 * dinolkan setiap kali dibaca bila {@link #getKhususBuatMahasiswaTertentu()} {@code true}.</p>
	 *
	 * @return status awal mahasiswa kriteria skema ini (proxy lazy diresolusi via {@code
	 *         check()}); {@code null} bila tidak membatasi ATAU mode khusus sedang aktif.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_mahasiswa", nullable = true)
	public StatusAwalMahasiswa getStatusAwalMahasiswa() {
		statusAwalMahasiswa = check(statusAwalMahasiswa);
		if (getKhususBuatMahasiswaTertentu()) {
			statusAwalMahasiswa = null;
		}
		return statusAwalMahasiswa;
	}

	/**
	 * @param statusAwalMahasiswa status awal baru untuk kriteria skema ini.
	 */
	public void setStatusAwalMahasiswa(StatusAwalMahasiswa statusAwalMahasiswa) {
		this.statusAwalMahasiswa = statusAwalMahasiswa;
	}

	/**
	 * Kriteria program studi (string bebas) untuk pemilihan skema ini.
	 *
	 * <p><b>Getter yang menulis balik (mode khusus):</b> dinolkan setiap kali dibaca bila mode
	 * khusus aktif, sama seperti {@link #getJenjang()}. Selain itu, nilai yang dikembalikan
	 * <b>tidak di-{@code trim()}</b> (berbeda dari kebanyakan getter string sejenis di kelas
	 * lain pada cluster ini) — hanya diperiksa kosong-setelah-trim untuk menentukan apakah
	 * mengembalikan {@code null} atau nilai field mentah.</p>
	 *
	 * @return program kriteria skema ini apa adanya (tidak di-{@code trim()}); {@code null}
	 *         bila kosong/hanya spasi ATAU mode khusus sedang aktif.
	 */
	public String getProgram() {
		if (getKhususBuatMahasiswaTertentu()) {
			program = null;
		}
		return program == null || program.trim().isEmpty() ? null : program;
	}

	/**
	 * @param program program baru untuk kriteria skema ini.
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Kriteria jenis seleksi PMB untuk pemilihan skema ini.
	 *
	 * <p><b>Getter yang menulis balik (mode khusus):</b> dinolkan setiap kali dibaca bila mode
	 * khusus aktif, sama seperti {@link #getJenjang()}.</p>
	 *
	 * @return jenis seleksi kriteria skema ini (proxy lazy diresolusi via {@code check()});
	 *         {@code null} bila tidak membatasi ATAU mode khusus sedang aktif.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_seleksi", nullable = true)
	public JenisSeleksi getJenisSeleksi() {
		jenisSeleksi = check(jenisSeleksi);
		if (getKhususBuatMahasiswaTertentu()) {
			jenisSeleksi = null;
		}
		return jenisSeleksi;
	}

	/**
	 * @param jenisSeleksi jenis seleksi baru untuk kriteria skema ini.
	 */
	public void setJenisSeleksi(JenisSeleksi jenisSeleksi) {
		this.jenisSeleksi = jenisSeleksi;
	}

	/**
	 * Kriteria jurusan/prodi untuk pemilihan skema ini — dimensi filter pada level {@code
	 * SettingBiaya} (menentukan APAKAH skema ini berlaku untuk jurusan tersebut), berbeda dari
	 * {@code DetailBiaya.getJurusan()} yang menentukan jurusan satu BARIS TAGIHAN untuk lookup
	 * override nominal via {@link DetailSettingBiaya#ambilDefaultBiaya(Jurusan)}.
	 *
	 * <p><b>Getter yang menulis balik (mode khusus):</b> dinolkan setiap kali dibaca bila mode
	 * khusus aktif, sama seperti {@link #getJenjang()}.</p>
	 *
	 * @return jurusan kriteria skema ini (proxy lazy diresolusi via {@code check()}); {@code
	 *         null} bila tidak membatasi ATAU mode khusus sedang aktif.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		if (getKhususBuatMahasiswaTertentu()) {
			jurusan = null;
		}
		return jurusan;
	}

	/**
	 * @param jurusan jurusan baru untuk kriteria skema ini.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return batas bawah semester cakupan skema ini (berlaku bila {@link
	 *         #getSmtIkutiSettinganDisini()} {@code true}); {@code 0} bila belum diisi.
	 */
	public Integer getMinSmt() {
		return minSmt == null ? 0 : minSmt;
	}

	/**
	 * @param minSmt batas bawah semester baru.
	 */
	public void setMinSmt(Integer minSmt) {
		this.minSmt = minSmt;
	}

	/**
	 * Batas atas semester cakupan skema ini (berlaku bila {@link #getSmtIkutiSettinganDisini()}
	 * {@code true}).
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code 30} pada pembacaan pertama — bukan sekadar
	 * dikembalikan sebagai nilai fallback sesaat seperti kebanyakan getter default lain di kelas
	 * ini.</p>
	 *
	 * @return batas atas semester; {@code 30} bila belum diisi (dan setelah pembacaan pertama,
	 *         tersimpan permanen sebagai {@code 30}).
	 */
	public Integer getMaxSmt() {
		if (maxSmt == null) {
			maxSmt = 30;
		}
		return maxSmt;
	}

	/**
	 * @param maxSmt batas atas semester baru.
	 */
	public void setMaxSmt(Integer maxSmt) {
		this.maxSmt = maxSmt;
	}

	/**
	 * Bila {@code true}, rentang Minimal/Maksimal Semester pada SettingBiaya ini yang dipakai,
	 * dan rentang semester (minSmt/maxSmt) pada {@link JenisKegiatan} TIDAK diberlakukan untuk
	 * setting ini. Bila {@code false} (default), perilaku lama dipertahankan: rentang JenisKegiatan
	 * tetap menjadi penentu.
	 *
	 * @return status flag ini; default {@code false} bila belum diisi.
	 * @see #getMinSmt()
	 * @see #getMaxSmt()
	 */
	public Boolean getSmtIkutiSettinganDisini() {
		return smtIkutiSettinganDisini == null ? false : smtIkutiSettinganDisini;
	}

	/**
	 * @param smtIkutiSettinganDisini nilai flag baru.
	 */
	public void setSmtIkutiSettinganDisini(Boolean smtIkutiSettinganDisini) {
		this.smtIkutiSettinganDisini = smtIkutiSettinganDisini;
	}

	/**
	 * Kriteria gelombang pendaftaran PMB untuk pemilihan skema ini.
	 *
	 * <p><b>Getter yang menulis balik (mode khusus):</b> dinolkan setiap kali dibaca bila mode
	 * khusus aktif, sama seperti {@link #getJenjang()}.</p>
	 *
	 * @return gelombang pendaftaran kriteria skema ini (proxy lazy diresolusi via {@code
	 *         check()}); {@code null} bila tidak membatasi ATAU mode khusus sedang aktif.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran", nullable = true)
	public GelombangPendaftaran getGelombangPendaftaran() {
		gelombangPendaftaran = check(gelombangPendaftaran);
		if (getKhususBuatMahasiswaTertentu()) {
			gelombangPendaftaran = null;
		}
		return gelombangPendaftaran;
	}

	/**
	 * @param gelombangPendaftaran gelombang pendaftaran baru untuk kriteria skema ini.
	 */
	public void setGelombangPendaftaran(GelombangPendaftaran gelombangPendaftaran) {
		this.gelombangPendaftaran = gelombangPendaftaran;
	}

	/**
	 * Flag mode "Khusus Buat Mahasiswa Tertentu" — lihat javadoc kelas untuk penjelasan
	 * arsitektur lengkap. Ringkasnya: saat {@code true}, tujuh getter kriteria pada kelas ini
	 * ({@link #getJenjang()}, {@link #getStatusAwalMahasiswa()}, {@link #getProgram()}, {@link
	 * #getJenisSeleksi()}, {@link #getJurusan()}, {@link #getGelombangPendaftaran()}, {@link
	 * #getPaket()}) menuliskan {@code null} ke field masing-masing setiap kali dipanggil, dan
	 * {@link #getGunakanBiayaDefault()} dipaksa {@code true} — mengubah skema ini dari "berlaku
	 * berdasar kriteria umum" menjadi "insidentil, hanya untuk mahasiswa yang eksplisit
	 * terdaftar" (biasanya lewat {@link DetailSettingBiaya} atau mekanisme di luar entity ini).
	 * {@link #getAngkatan()} sengaja/tidak sengaja dikecualikan dari pola ini (lihat javadoc
	 * method tersebut).
	 *
	 * @return status flag ini; default {@code false} bila belum diisi.
	 */
	public Boolean getKhususBuatMahasiswaTertentu() {
		return khususBuatMahasiswaTertentu == null ? false : khususBuatMahasiswaTertentu;
	}

	/**
	 * @param khususBuatMahasiswaTertentu nilai flag baru; mengaktifkan/menonaktifkan seluruh
	 *                                    pola getter-menulis-balik yang dijelaskan pada javadoc
	 *                                    kelas dan {@link #getKhususBuatMahasiswaTertentu()}.
	 */
	public void setKhususBuatMahasiswaTertentu(Boolean khususBuatMahasiswaTertentu) {
		this.khususBuatMahasiswaTertentu = khususBuatMahasiswaTertentu;
	}

	/**
	 * Membatasi setting biaya umum/bulanan hanya untuk mahasiswa yang tercatat pada
	 * SettingBiayaDetail. Berbeda dengan khususBuatMahasiswaTertentu, flag ini tidak
	 * mengubah tagihan menjadi nilai default/insidentil dan tetap memakai billing bulanan.
	 *
	 * @return status flag ini; default {@code false} bila belum diisi.
	 */
	@Column(name = "batasi_mahasiswa_tertentu")
	public Boolean getBatasiMahasiswaTertentu() {
		return batasiMahasiswaTertentu == null ? false : batasiMahasiswaTertentu;
	}

	/**
	 * @param batasiMahasiswaTertentu nilai flag baru.
	 */
	public void setBatasiMahasiswaTertentu(Boolean batasiMahasiswaTertentu) {
		this.batasiMahasiswaTertentu = batasiMahasiswaTertentu;
	}

	/**
	 * Daftar NIM yang tidak boleh memakai setting biaya ini. Format NIM biasa berlaku untuk
	 * seluruh semester; format {@code NIM:SMT_MULAI:SMT_SAMPAI} hanya berlaku pada rentang
	 * semester tersebut. Antarentri dapat dipisahkan koma, titik koma, spasi, tab, atau baris baru.
	 *
	 * @return daftar pengecualian, di-{@code trim()}; string kosong ({@code ""}) bila belum
	 *         diisi — tidak pernah {@code null}.
	 * @see #isMahasiswaDikecualikan(String, Integer)
	 * @see #validasiFormatPengecualianMahasiswa(String)
	 */
	@Column(name = "pengecualian_mahasiswa", columnDefinition = "text")
	public String getPengecualianMahasiswa() {
		return pengecualianMahasiswa == null ? "" : pengecualianMahasiswa.trim();
	}

	/**
	 * @param pengecualianMahasiswa daftar pengecualian baru; di-{@code trim()} sebelum disimpan
	 *                              ({@code null} tetap {@code null}, bukan di-trim).
	 */
	public void setPengecualianMahasiswa(String pengecualianMahasiswa) {
		this.pengecualianMahasiswa = pengecualianMahasiswa == null ? null : pengecualianMahasiswa.trim();
	}

	/**
	 * Urutan pemilihan Setting Biaya. Angka yang lebih kecil didahulukan,
	 * sedangkan nilai 10 menjaga perilaku seluruh data lama dan data baru.
	 *
	 * @return prioritas skema ini; default {@code 10} bila belum diisi.
	 */
	@Column(name = "prioritas")
	public Integer getPrioritas() {
		return prioritas == null ? Integer.valueOf(10) : prioritas;
	}

	/**
	 * @param prioritas prioritas baru; {@code null} dinormalkan menjadi {@code 10} langsung di
	 *                  setter ini (berbeda dari kebanyakan setter lain di kelas ini yang
	 *                  menyimpan {@code null} apa adanya dan menormalkannya hanya di getter).
	 */
	public void setPrioritas(Integer prioritas) {
		this.prioritas = prioritas == null ? Integer.valueOf(10) : prioritas;
	}

	/**
	 * Memvalidasi format sebelum disimpan dari UI. Nilai kosong sah karena berarti tidak ada
	 * pengecualian. Method ini sengaja tidak dipanggil dari setter agar data legacy yang pernah
	 * tersimpan tidak membuat Hibernate gagal memuat entity.
	 *
	 * @param daftar string daftar pengecualian NIM (format lihat {@link
	 *               #getPengecualianMahasiswa()}); boleh {@code null}/kosong
	 * @throws IllegalArgumentException bila ada entri berformat rentang semester yang tidak
	 *                                   valid (bukan 3 bagian, bagian kosong, atau semester
	 *                                   bukan angka/rentang terbalik)
	 */
	public static void validasiFormatPengecualianMahasiswa(String daftar) {
		if (daftar == null || daftar.trim().length() == 0) {
			return;
		}
		String nilai = daftar.trim().replaceAll("\\s*:\\s*", ":");
		String[] entri = nilai.split("[\\s,;]+");
		for (int i = 0; i < entri.length; i++) {
			String item = entri[i] == null ? "" : entri[i].trim();
			if (item.length() == 0) {
				continue;
			}
			String[] bagian = item.split(":", -1);
			if (bagian.length == 1) {
				continue;
			}
			if (bagian.length != 3 || bagian[0].trim().length() == 0
					|| bagian[1].trim().length() == 0 || bagian[2].trim().length() == 0) {
				throw new IllegalArgumentException("Format pengecualian tidak valid pada '" + item
						+ "'. Gunakan NIM atau NIM:SMT_MULAI:SMT_SAMPAI.");
			}
			try {
				int semesterMulai = Integer.parseInt(bagian[1].trim());
				int semesterSampai = Integer.parseInt(bagian[2].trim());
				if (semesterMulai < 1 || semesterSampai < semesterMulai) {
					throw new IllegalArgumentException("Rentang semester tidak valid pada '" + item
							+ "'. Semester mulai minimal 1 dan tidak boleh melebihi semester sampai.");
				}
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Semester pada '" + item + "' harus berupa angka.");
			}
		}
	}

	/**
	 * Pemeriksaan kompatibel format lama; entri berentang diabaikan bila semester tidak
	 * tersedia.
	 *
	 * @param nim NIM yang diperiksa; boleh {@code null}
	 * @return {@code true} bila {@code nim} tercantum sebagai pengecualian format-lama (tanpa
	 *         rentang semester) pada {@link #getPengecualianMahasiswa()}
	 * @see #isMahasiswaDikecualikan(String, Integer)
	 */
	public boolean isMahasiswaDikecualikan(String nim) {
		return isMahasiswaDikecualikan(nim, null);
	}

	/**
	 * Pemeriksaan tunggal agar seluruh jalur billing memakai aturan pengecualian semester yang
	 * sama.
	 *
	 * @param nim           NIM yang diperiksa; boleh {@code null}
	 * @param semesterAktif semester aktif acuan untuk entri berformat rentang ({@code
	 *                      NIM:SMT_MULAI:SMT_SAMPAI}); entri berentang diabaikan bila {@code null}
	 * @return {@code true} bila {@code nim} tercantum pada {@link #getPengecualianMahasiswa()}
	 *         (format biasa, atau format rentang yang mencakup {@code semesterAktif})
	 */
	public boolean isMahasiswaDikecualikan(String nim, Integer semesterAktif) {
		if (nim == null || nim.trim().length() == 0 || getPengecualianMahasiswa().length() == 0) {
			return false;
		}
		String nimDicari = nim.trim();
		String nilai = getPengecualianMahasiswa().replaceAll("\\s*:\\s*", ":");
		String[] daftarNim = nilai.split("[\\s,;]+");
		for (int i = 0; i < daftarNim.length; i++) {
			String[] bagian = daftarNim[i].trim().split(":", -1);
			if (bagian.length == 1 && nimDicari.equalsIgnoreCase(bagian[0].trim())) {
				return true;
			}
			if (bagian.length == 3 && nimDicari.equalsIgnoreCase(bagian[0].trim())
					&& semesterAktif != null) {
				try {
					int semesterMulai = Integer.parseInt(bagian[1].trim());
					int semesterSampai = Integer.parseInt(bagian[2].trim());
					if (semesterAktif.intValue() >= semesterMulai
							&& semesterAktif.intValue() <= semesterSampai) {
						return true;
					}
				} catch (NumberFormatException e) {
					// Data legacy salah format tidak boleh menghentikan proses penagihan.
				}
			}
		}
		return false;
	}

	/**
	 * Kriteria paket biaya untuk pemilihan skema ini.
	 *
	 * <p><b>Getter yang menulis balik (mode khusus):</b> dinolkan setiap kali dibaca bila mode
	 * khusus aktif, sama seperti {@link #getJenjang()}.</p>
	 *
	 * @return paket kriteria skema ini (proxy lazy diresolusi via {@code check()}); {@code
	 *         null} bila tidak membatasi ATAU mode khusus sedang aktif.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket", nullable = true)
	public Paket getPaket() {
		paket = check(paket);
		if (getKhususBuatMahasiswaTertentu()) {
			paket = null;
		}
		return paket;
	}

	/**
	 * @param paket paket baru untuk kriteria skema ini.
	 */
	public void setPaket(Paket paket) {
		this.paket = paket;
	}

	/**
	 * @return jumlah termin/tahap pembayaran skema ini; {@code 1} bila belum diisi atau bernilai
	 *         kurang dari 1.
	 */
	public Integer getJumlahPembayaran() {
		return jumlahPembayaran == null || jumlahPembayaran < 1 ? 1 : jumlahPembayaran;
	}

	/**
	 * @param jumlahPembayaran jumlah termin/tahap pembayaran baru.
	 */
	public void setJumlahPembayaran(Integer jumlahPembayaran) {
		this.jumlahPembayaran = jumlahPembayaran;
	}

	/**
	 * Flag "Tampilkan Per Prodi" — mengaktifkan cabang PERTAMA (prioritas tertinggi) pada
	 * {@code DetailBiaya.getNilaiBiaya()}: nominal diambil dari {@link
	 * DetailSettingBiaya#ambilDefaultBiaya(Jurusan)} untuk jurusan baris tagihan yang
	 * bersangkutan.
	 *
	 * <p><b>Lihat catatan bug pada {@link DetailSettingBiaya#ambilDefaultBiaya(Jurusan)}:</b>
	 * method itu TIDAK memiliki fallback ke {@link DetailSettingBiaya#getDefaultBiaya()} bila
	 * jurusan tertentu belum diberi override per-prodi eksplisit — hasilnya {@code 0.0}.
	 * Mengaktifkan flag ini pada sebuah {@link SettingBiaya} tanpa memastikan SETIAP jurusan
	 * relevan sudah diberi override di {@link DetailSettingBiaya#getBiayaPerProdi()} berisiko
	 * menagihkan Rp 0 untuk jurusan yang terlewat.</p>
	 *
	 * @return status flag ini; default {@code false} bila belum diisi.
	 */
	public Boolean getTampilkanPerProdi() {
		return tampilkanPerProdi == null ? false : tampilkanPerProdi;
	}

	/**
	 * @param tampilkanPerProdi nilai flag baru.
	 */
	public void setTampilkanPerProdi(Boolean tampilkanPerProdi) {
		this.tampilkanPerProdi = tampilkanPerProdi;
	}

	/**
	 * @return status mahasiswa saat ini sebagai kriteria skema ini (proxy lazy diresolusi via
	 *         {@code check()}); boleh {@code null}. Berbeda dari tujuh kriteria lain, field ini
	 *         TIDAK ikut dinolkan oleh mode {@link #getKhususBuatMahasiswaTertentu()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_mahasiswa", nullable = true)
	public StatusMahasiswa getStatusMahasiswa() {
		statusMahasiswa = check(statusMahasiswa);
		return statusMahasiswa;
	}

	/**
	 * @param statusMahasiswa status mahasiswa baru untuk kriteria skema ini.
	 */
	public void setStatusMahasiswa(StatusMahasiswa statusMahasiswa) {
		this.statusMahasiswa = statusMahasiswa;
	}

	/**
	 * @return {@code true} bila notifikasi tagihan diaktifkan untuk skema ini; default {@code
	 *         false} bila belum diisi.
	 */
	public Boolean getAktifkanNotifikasi() {
		return aktifkanNotifikasi == null ? false : aktifkanNotifikasi;
	}

	/**
	 * @param aktifkanNotifikasi nilai flag baru.
	 */
	public void setAktifkanNotifikasi(Boolean aktifkanNotifikasi) {
		this.aktifkanNotifikasi = aktifkanNotifikasi;
	}

	/**
	 * Waktu pengiriman notifikasi tagihan.
	 *
	 * <p><b>Getter yang menulis balik:</b> bila {@link #getAktifkanNotifikasi()} {@code false},
	 * field {@link #waktuNotifikasi} ditimpa menjadi {@code null} setiap kali getter ini
	 * dipanggil — menonaktifkan notifikasi lalu membaca entity ini (mis. untuk ditampilkan di
	 * form) dapat menghapus permanen waktu yang sebelumnya dikonfigurasi, sehingga
	 * mengaktifkan kembali notifikasi nanti tidak mengembalikan waktu lama.</p>
	 *
	 * @return waktu notifikasi; {@code null} bila belum diisi ATAU notifikasi tidak aktif.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuNotifikasi() {
		if (!getAktifkanNotifikasi()) {
			waktuNotifikasi = null;
		}
		return waktuNotifikasi;
	}

	/**
	 * @param waktuNotifikasi waktu notifikasi baru.
	 */
	public void setWaktuNotifikasi(Date waktuNotifikasi) {
		this.waktuNotifikasi = waktuNotifikasi;
	}

	/**
	 * Template pesan notifikasi tagihan.
	 *
	 * <p><b>Getter yang menulis balik:</b> pola yang sama seperti {@link #getWaktuNotifikasi()}
	 * — dinolkan setiap kali dibaca bila notifikasi tidak aktif, berisiko menghapus permanen
	 * template yang sebelumnya dikonfigurasi.</p>
	 *
	 * @return template notifikasi, di-{@code trim()}; string kosong ({@code ""}) bila belum
	 *         diisi ATAU notifikasi tidak aktif — tidak pernah {@code null}.
	 */
	@Column(name = "template_notifikasi", columnDefinition = "text")
	public String getTemplateNotifikasi() {
		if (!getAktifkanNotifikasi()) {
			templateNotifikasi = null;
		}
		return templateNotifikasi == null ? "" : templateNotifikasi.trim();
	}

	/**
	 * @param templateNotifikasi template notifikasi baru.
	 */
	public void setTemplateNotifikasi(String templateNotifikasi) {
		this.templateNotifikasi = templateNotifikasi;
	}

	/**
	 * @return batas waktu pembayaran skema ini; boleh {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getBatasWaktuPembayaran() {
		return batasWaktuPembayaran;
	}

	/**
	 * @param batasWaktuPembayaran batas waktu pembayaran baru.
	 */
	public void setBatasWaktuPembayaran(Date batasWaktuPembayaran) {
		this.batasWaktuPembayaran = batasWaktuPembayaran;
	}

	/**
	 * Kriteria jenis kelamin.
	 *
	 * <p><b>Getter yang menulis balik (validasi-saat-baca):</b> nilai yang bukan persis {@code
	 * "Laki-laki"} atau {@code "Perempuan"} — termasuk data legacy dengan ejaan/kapitalisasi
	 * berbeda — ditimpa menjadi {@code null} setiap kali getter ini dipanggil, menghapus
	 * permanen nilai yang dianggap tidak valid dari database begitu entity di-flush.</p>
	 *
	 * @return jenis kelamin kriteria skema ini; {@code null} bila belum diisi ATAU nilai
	 *         tersimpan tidak persis {@code "Laki-laki"}/{@code "Perempuan"}.
	 */
	public String getKelamin() {
		if (kelamin != null && !(kelamin.equals("Laki-laki") || kelamin.equals("Perempuan"))) {
			kelamin = null;
		}
		return kelamin;
	}

	/**
	 * @param kelamin jenis kelamin baru; idealnya persis {@code "Laki-laki"} atau {@code
	 *                "Perempuan"} agar tidak dinolkan oleh {@link #getKelamin()}.
	 */
	public void setKelamin(String kelamin) {
		this.kelamin = kelamin;
	}

	/**
	 * @return afiliasi calon mahasiswa kriteria skema ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "afiliasi_calon_mahasiswa", nullable = true)
	public AfiliasiCalonMahasiswa getAfiliasiCalonMahasiswa() {
		afiliasiCalonMahasiswa = check(afiliasiCalonMahasiswa);
		return afiliasiCalonMahasiswa;
	}

	/**
	 * @param afiliasiCalonMahasiswa afiliasi calon mahasiswa baru untuk kriteria skema ini.
	 */
	public void setAfiliasiCalonMahasiswa(AfiliasiCalonMahasiswa afiliasiCalonMahasiswa) {
		this.afiliasiCalonMahasiswa = afiliasiCalonMahasiswa;
	}

	/**
	 * @return tahun akademik kriteria skema ini (format {@code "YYYY/YYYY"}); boleh {@code null}.
	 * @see #getTa()
	 */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * @param tahunAkademik tahun akademik baru.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * @return kode semester kriteria skema ini ({@link Perkuliahan#GENAP} atau ganjil); boleh
	 *         {@code null}.
	 * @see #getTa()
	 */
	public String getSemester() {
		return semester;
	}

	/**
	 * @param semester kode semester baru.
	 */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/**
	 * Menurunkan ID tahun-ajaran+semester gabungan (dipakai sebagai kunci pembanding numerik
	 * ringkas) dari {@link #getTahunAkademik()} dan {@link #getSemester()}.
	 *
	 * <p>Formula: digit pertama {@code getTahunAkademik().split("/")[0]} (tahun awal rentang
	 * tahun akademik, atau {@code "0"} bila kosong) digabung dengan {@code "2"} bila semester
	 * genap ({@link Perkuliahan#GENAP}), {@code "1"} bila ganjil, atau {@code "0"} bila semester
	 * kosong — lalu hasil gabungan string itu di-parse menjadi {@link Integer}. Contoh: tahun
	 * akademik {@code "2024/2025"} semester genap menghasilkan {@code "20242"} → {@code 20242}.
	 *
	 * <p><b>Penanganan galat dan potensi nilai basi (stale):</b> parsing dibungkus {@code
	 * try/catch(Exception)} generik yang hanya mencatat ke {@link ais.common.ErrorAuditUtil}
	 * tanpa melempar exception. Karena {@link #ta} adalah FIELD instance (bukan variabel lokal),
	 * bila parsing gagal pada satu pemanggilan, {@code ta} TIDAK direset — ia mempertahankan
	 * nilai dari pemanggilan SEBELUMNYA (atau {@code null} bila belum pernah berhasil sama
	 * sekali, yang baru kemudian dinormalkan ke {@code 0} oleh pengecekan setelah blok {@code
	 * try}). Pada praktiknya input yang menyebabkan kegagalan parse hampir tidak mungkin terjadi
	 * (kedua komponen sudah dinormalkan ke digit tunggal terlebih dahulu), sehingga risiko nilai
	 * basi ini bersifat teoretis, bukan celah yang mudah dipicu.</p>
	 *
	 * @return ID tahun-ajaran+semester gabungan; {@code 0} bila tidak dapat diturunkan sama
	 *         sekali (belum pernah berhasil di-parse).
	 */
	public Integer getTa() {
		String id_smt = (getTahunAkademik() == null || getTahunAkademik().trim().isEmpty() ? "0"
				: getTahunAkademik().split("/")[0])
				+ (getSemester() == null || getSemester().trim().isEmpty() ? "0"
						: getSemester().equals(Perkuliahan.GENAP) ? "2" : "1");
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/SettingBiaya.java:403");

		}
		if (ta == null) {
			ta = 0;
		}
		return ta;
	}

	/**
	 * @param ta ID tahun-ajaran+semester gabungan; biasanya tidak perlu diset manual karena
	 *           diturunkan otomatis oleh {@link #getTa()} dari {@link #getTahunAkademik()} dan
	 *           {@link #getSemester()}.
	 */
	public void setTa(Integer ta) {
		this.ta = ta;
	}
}
