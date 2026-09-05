package ais.database.model.sop;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.FormSop;

/**
 * <b>Header satu pengajuan SOP</b>: satu baris di sini mewakili satu berkas/permohonan yang sedang
 * atau pernah berjalan melalui mesin alur persetujuan SOP, sedangkan tiap langkah yang dilaluinya
 * dicatat sebagai baris {@link DisposisiAlurSop} tersendiri.
 *
 * <h2>Hubungan dengan {@link DisposisiAlurSop}</h2>
 * <p>Keduanya adalah pasangan <b>header : detail</b>, bukan dua konsep yang berdiri sendiri:</p>
 * <ul>
 * <li>Ke arah bawah: banyak {@code DisposisiAlurSop} menunjuk ke satu {@code DisposisiSop} lewat
 * kolom {@code disposisi_sop}. Itulah riwayat lengkap disposisi pengajuan ini.</li>
 * <li>Ke arah atas: header menyimpan <b>tiga penunjuk pintas</b> ke baris detail tertentu, yaitu
 * {@link #getDisposisiStart()} (langkah awal/pengaju), {@link #getDisposisiEnd()} (langkah terakhir
 * yang tercatat), dan {@link #getDisposisiSetuju()} (langkah yang dianggap titik disetujui).
 * Ketiganya sekadar cache navigasi agar dasbor dan laporan tidak perlu menyusuri seluruh rantai
 * tiap kali ingin tahu status pengajuan.</li>
 * </ul>
 * <p>Jadi pertanyaan "apakah pengajuan ini sudah disetujui?" pada praktiknya dijawab dengan
 * memeriksa apakah {@link #getDisposisiSetuju()} mengembalikan sesuatu.</p>
 *
 * <h2>Apa yang disimpan header</h2>
 * <p>Identitas pengajuan: prosedur yang diikuti ({@link #getSop()}), waktu pengajuan, keterangan,
 * dan pengaju — yang bisa berupa pengguna internal ({@link #getDiajukanOleh()}), mahasiswa, atau
 * siswa, tiga slot yang saling meniadakan. Header juga menyimpan kantong properti JSON bebas dan
 * penanda aktif. Dokumen sesungguhnya (mis. permintaan pengadaan, surat keluar, pengajuan
 * transfer) berada di tabel modul masing-masing dan ditemukan kembali lewat
 * {@link #ambil(Session, FormSop)}.</p>
 *
 * <h2>Sifat pemakaian: mesin generik lintas modul</h2>
 * <p>Entity ini bukan milik satu modul. Daftar tabel pada {@link #hapus()} memberi gambaran
 * cakupannya: akuntansi (pengajuan transfer, kas kecil, pertanggungjawaban, uang muka, transitori),
 * aset (pengadaan, pembayaran DP/termin, peminjaman, penghapusan, perbaikan, kerja sama),
 * persuratan (surat masuk/keluar), sekolah (kelompok pendaftaran PSB), kepegawaian, kemahasiswaan,
 * pengaduan, sampai seleksi vendor perpustakaan. Karena itu perubahan sekecil apa pun di kelas ini
 * berdampak lintas modul.</p>
 *
 * <h2>PERINGATAN KEAMANAN — header pun tidak memvalidasi wewenang</h2>
 * <p>Seperti {@link DisposisiAlurSop}, kelas ini tidak memiliki penjaga otorisasi:</p>
 * <ul>
 * <li>Satu-satunya callback siklus hidup adalah {@link #onUpdate()} beranotasi {@code @PreUpdate},
 * yang isinya murni mencatat stempel waktu/pelaku audit. Tidak ada {@code @PrePersist} dan tidak
 * ada {@code @EntityListeners}.</li>
 * <li>{@link #setDisposisiSetuju(DisposisiAlurSop)} — penunjuk "pengajuan ini disetujui" — adalah
 * setter biasa yang menerima baris jenjang mana pun asalkan sudah punya id. Ia tidak memeriksa
 * bahwa baris itu memang milik pengajuan ini, bahwa jenjangnya memang jenjang persetujuan, atau
 * bahwa pelakunya berwenang.</li>
 * <li>Tidak ada penjaga <i>self-approval</i>: tidak ada apa pun yang membandingkan
 * {@link #getDiajukanOleh()} dengan pelaku pada baris jenjang yang ditunjuk
 * {@code disposisiSetuju}.</li>
 * <li>Tidak ada penegakan urutan jenjang di sini; urutan sepenuhnya terbentuk dari rantai
 * {@code sebelumnya}/{@code setelahnya} antar baris detail yang dirangkai lapis service.</li>
 * <li>Di basis data tidak ada {@code UNIQUE}/{@code CHECK} yang menjaga hal-hal tersebut — indeks
 * yang dibuat {@code InitIndex} untuk {@code disposisi_sop} bersifat kinerja.</li>
 * </ul>
 * <p>Yang <i>ada</i> justru sebaliknya: {@link #getDisposisiSetuju()} <b>menyimpulkan sendiri</b>
 * status persetujuan dari konfigurasi alur dan menuliskan hasilnya ke field yang persisten. Dengan
 * kata lain, status "disetujui" pada pengajuan dapat terbentuk sebagai efek samping pembacaan,
 * bukan hanya sebagai hasil tindakan aktor yang terverifikasi. Semua penyaringan wewenang
 * mengandalkan lapis pemanggil.</p>
 *
 * <h2>Riwayat/audit</h2>
 * <p>Kelas beranotasi {@link Audited} (Envers), namun {@link #hapus()} menghapus data lewat SQL
 * native sehingga penghapusan <b>tidak</b> meninggalkan jejak revisi.</p>
 *
 * @see DisposisiAlurSop
 * @see AlurSop
 * @see FormSop
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "disposisi_sop")
public class DisposisiSop extends GeneralValueObject {

	/**
	 * Versi serialisasi; nilainya sengaja sama dengan {@link DisposisiAlurSop} karena keduanya
	 * berasal dari template generator yang sama. Jangan diubah — instance entity SOP ikut
	 * diserialkan pada cache dan penyimpanan sesi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code disposisi_sop}, di-generate database. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (kolom bayangan audit). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (kolom bayangan audit). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah header ini (kolom bayangan audit).
	 *
	 * <p>Bukan pengaju. Pengaju pengajuan ada pada {@link #getDiajukanOleh()} /
	 * {@link #getMahasiswa()} / {@link #getSiswa()}; nilai di sini hanya jejak teknis siapa
	 * terakhir menyentuh baris.</p>
	 *
	 * @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna terakhir yang mengubah header ini.
	 *
	 * <p>Nilai {@code null}/kosong diabaikan agar jejak audit lama tidak tertimpa nilai hampa saat
	 * entity di-<i>rebind</i> dari form yang tidak mengirim field audit.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang mengubah header ini; nilai {@code null}/kosong
	 * diabaikan, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah header ini (kolom bayangan audit).
	 *
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA sebelum header ini di-{@code UPDATE}: mencatat stempel waktu dan pelaku
	 * perubahan lewat {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p><b>Satu-satunya callback siklus hidup pada entity ini, dan isinya murni audit teknis.</b>
	 * Tidak ada pemeriksaan wewenang, tidak ada pemeriksaan self-approval, tidak ada penegakan
	 * urutan jenjang. Tidak ada pula {@code @PrePersist}, sehingga pembuatan pengajuan baru tidak
	 * melewati callback apa pun.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir header (kolom bayangan audit), diinisialisasi ke waktu server
	 * saat object dibuat.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya diisi mesin audit, bukan kode modul.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir header.
	 *
	 * <p>Jangan disamakan dengan waktu pengajuan: untuk itu pakai {@link #getWaktu()}. Nilai ini
	 * berubah setiap kali baris tersentuh mesin audit.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berbentuk {@code "<id>-<keterangan>"}.
	 *
	 * <p>Berbeda dari {@link DisposisiAlurSop#toString()}, method ini membaca <b>field</b>
	 * {@code keterangan} langsung sehingga tidak menyentuh relasi lazy apa pun — aman dipanggil
	 * dari logging pada konteks tanpa sesi Hibernate.</p>
	 *
	 * @return teks {@code "<id>-<keterangan>"}
	 */
	public String toString() {
		return id + "-" + keterangan;
	}

	/** Prosedur (definisi SOP) yang diikuti pengajuan ini. */
	private Sop sop;
	/** Waktu pengajuan dibuat. */
	private Date waktu;
	/** Keterangan/maksud pengajuan yang ditulis pengaju. */
	private String keterangan;
	/** Penanda aktif; bawaan {@code true} bila belum diisi. */
	private Boolean aktif;
	/** Pengaju bila ia pengguna internal (pegawai/staf). */
	private Tbmuser diajukanOleh;
	/** Pengaju bila ia mahasiswa. */
	private Mahasiswa mahasiswa;
	/** Pengaju bila ia siswa. */
	private Siswa siswa;

	/** Penunjuk pintas ke baris jenjang awal (langkah pengaju). */
	private DisposisiAlurSop disposisiStart;
	/** Penunjuk pintas ke baris jenjang yang dianggap titik disetujui. */
	private DisposisiAlurSop disposisiSetuju;
	/** Penunjuk pintas ke baris jenjang terakhir yang tercatat. */
	private DisposisiAlurSop disposisiEnd;
	/** Kantong properti bebas berformat JSON untuk kebutuhan modul pemanggil. */
	private String properti;


	/**
	 * Menemukan kembali <b>dokumen modul</b> yang tertaut pada pengajuan ini — baris pada tabel
	 * modul (mis. permintaan pengadaan, surat keluar, pengajuan transfer) yang menyimpan kolom
	 * {@code disposisiSop} menunjuk ke header ini.
	 *
	 * <p>Kelas mana yang dicari ditentukan {@link FormSop#ambilClass()} milik form modul yang
	 * sedang aktif, sehingga satu method generik ini melayani semua modul yang memakai mesin SOP.
	 * Kriteria: {@code disposisiSop = header ini}, diurutkan menaik menurut id, dan diambil satu
	 * saja ({@code setMaxResults(1)}) — bila karena satu dan lain hal ada lebih dari satu dokumen
	 * tertaut, yang terpilih adalah yang paling lama.</p>
	 *
	 * <p><b>Penyaring "aktif" secara cerdas.</b> Tidak semua kelas modul punya kolom
	 * {@code aktif}. Karena itu method memeriksa metadata Hibernate
	 * ({@code getClassMetadata(...).getPropertyNames()}) lebih dahulu, dan hanya bila properti
	 * {@code aktif} benar-benar ada barulah ditambahkan syarat
	 * {@code (aktif = true OR aktif IS NULL)}. Pola ini menghindari pengecualian "property not
	 * found" tanpa perlu daftar kelas yang dipelihara manual, sekaligus memperlakukan data lama
	 * yang kolom {@code aktif}-nya masih {@code NULL} sebagai aktif.</p>
	 *
	 * <p><b>{@code FlushMode.MANUAL} — jangan dihapus.</b> Komentar di badan method menjelaskan
	 * alasannya: kueri lookup ini tidak boleh memicu <i>auto-flush</i>. Karena banyak getter di
	 * ekosistem ini bersifat destruktif (menulis balik ke field saat dibaca), auto-flush akan
	 * menuliskan entity yang "kotor" di tengah transaksi menampilkan SOP — yang terbukti memicu
	 * <i>deadlock</i>/<i>statement timeout</i> pada tabel
	 * {@code pembayaran_termin_master_asset} dan {@code disposisi_sop}. Penulisan yang memang
	 * disengaja tetap ter-flush pada commit transaksinya masing-masing.</p>
	 *
	 * <p>Seluruh badan dibungkus {@code try/catch} yang mencatat galat ke audit dan mengembalikan
	 * {@code null}; kegagalan menemukan dokumen tidak boleh menggagalkan tampilan riwayat SOP.</p>
	 *
	 * @param session sesi Hibernate yang dipakai untuk kueri
	 * @param formSop form modul yang menentukan kelas dokumen yang dicari
	 * @return dokumen modul yang tertaut, atau {@code null} bila tidak ada/gagal dimuat
	 */
	public GeneralValueObject ambil(Session session, FormSop formSop) {
		GeneralValueObject generalValueObject = null;
		try {
			DisposisiSop disposisiSop = this;
			// 1. Inisialisasi Criteria dasar
			org.hibernate.Criteria criteria = session.createCriteria(formSop.ambilClass())
					.addOrder(Order.asc("id"))
					.add(Restrictions.eq("disposisiSop", disposisiSop))
					.setMaxResults(1);

			// 2. Pengecekan Cerdas (Reflection/Metadata) apakah class ini punya kolom 'aktif'
			org.hibernate.metadata.ClassMetadata meta = session.getSessionFactory().getClassMetadata(formSop.ambilClass());
			if (meta != null) {
				boolean hasAktif = false;
				String[] propertiNames = meta.getPropertyNames();
				for (String prop : propertiNames) {
					if (prop.equals("aktif")) {
						hasAktif = true;
						break;
					}
				}

				// 3. Jika kolom 'aktif' ditemukan, tambahkan filter (aktif = true ATAU aktif IS NULL)
				if (hasAktif) {
					criteria.add(Restrictions.or(
							Restrictions.eq("aktif", true),
							Restrictions.isNull("aktif")
					));
				}
			}

			// 4. Eksekusi Query.
			// FlushMode MANUAL: query LOOKUP ini TIDAK boleh memicu auto-flush. Auto-flush akan
			// menuliskan entitas "kotor" (mis. akibat getter ber-efek samping seperti getLunas/
			// getKodeInvoice) di tengah transaksi tampil SOP, yang terbukti memicu deadlock /
			// statement timeout pada tabel pembayaran_termin_master_asset & disposisi_sop. Penulisan
			// yang memang disengaja tetap di-flush saat commit transaksinya masing-masing.
			criteria.setFlushMode(org.hibernate.FlushMode.MANUAL);
			generalValueObject = (GeneralValueObject) criteria.uniqueResult();

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sop/DisposisiSop.java:148");
		}
		
		return generalValueObject;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA.
	 */
	public DisposisiSop() {
	}

	/**
	 * Membuat instance ringan yang hanya membawa kunci utama.
	 *
	 * <p>Berguna sebagai acuan pada kriteria kueri (mis. {@code Restrictions.eq("disposisiSop",
	 * new DisposisiSop(id))}) tanpa perlu memuat seluruh baris dari database.</p>
	 *
	 * @param id kunci utama header pengajuan
	 */
	public DisposisiSop(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kunci utama header pengajuan.
	 *
	 * <p>Nilai {@code null} berarti pengajuan belum tersimpan. Id ini yang dipakai sebagai kunci
	 * penghubung pada semua tabel modul (kolom {@code disposisi_sop}) — lihat daftar tabel pada
	 * {@link #hapus()}.</p>
	 *
	 * @return id header pengajuan, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Normalnya hanya dipanggil Hibernate setelah {@code INSERT}.
	 *
	 * @param id kunci utama header pengajuan
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan/maksud pengajuan yang ditulis pengaju.
	 *
	 * <p>Nilai ini pula yang muncul sebagai catatan pada baris jenjang awal, karena
	 * {@link DisposisiAlurSop#getKeterangan()} menyalinnya untuk jenjang {@code start}. Getter ini
	 * sendiri tidak destruktif: bila kosong, ia hanya <i>mengembalikan</i> string kosong tanpa
	 * menulis balik ke field.</p>
	 *
	 * @return keterangan pengajuan; string kosong bila belum diisi (tidak pernah {@code null})
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan == null ? "" : this.keterangan;
	}

	/**
	 * Menyetel keterangan/maksud pengajuan.
	 *
	 * @param keterangan keterangan pengajuan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan prosedur ({@link Sop}) yang diikuti pengajuan ini.
	 *
	 * <p>Dari sinilah keseluruhan definisi alur berasal: {@link AlurSop} milik prosedur ini yang
	 * menentukan jenjang apa saja yang harus dilalui, siapa aktornya, dan di mana letak titik
	 * persetujuan. Pemuatan lewat {@code check()} agar aman terhadap proxy lazy yang lepas sesi.</p>
	 *
	 * @return prosedur yang diikuti, atau {@code null} bila belum diisi/tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sop", nullable = true)
	public Sop getSop() {
		sop = check(sop);
		return sop;
	}

	/**
	 * Menetapkan prosedur yang diikuti pengajuan ini.
	 *
	 * <p>Setter polos: tidak ada pemeriksaan bahwa prosedur masih aktif, bahwa pengaju berhak
	 * memakai prosedur tersebut, ataupun bahwa baris-baris jenjang yang sudah terlanjur dibuat
	 * masih sesuai dengan prosedur yang baru. Mengganti nilai ini pada pengajuan yang sudah
	 * berjalan akan membuat riwayat dan definisi tidak lagi sejalan.</p>
	 *
	 * @param sop prosedur yang diikuti
	 */
	public void setSop(Sop sop) {
		this.sop = sop;
	}

	/**
	 * Mengembalikan waktu pengajuan.
	 *
	 * <p>Bila belum diisi, dikembalikan <b>waktu sekarang</b> — bukan {@code null}. Nilai bawaan
	 * ini hanya dikembalikan, tidak ditulis balik ke field, sehingga getter tidak destruktif;
	 * konsekuensinya, selama nilainya belum benar-benar disetel, dua pemanggilan berturut-turut
	 * dapat memberi waktu yang sedikit berbeda. Jenjang awal menyalin nilai ini sebagai waktu
	 * langkahnya (lihat {@link DisposisiAlurSop#getWaktu()}).</p>
	 *
	 * @return waktu pengajuan; waktu sekarang bila belum diisi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? new Date() : waktu;
	}

	/**
	 * Menyetel waktu pengajuan.
	 *
	 * @param waktu waktu pengajuan
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan penanda aktif header pengajuan, dengan bawaan {@code true} bila belum diisi.
	 *
	 * <p>Perlakuan "null berarti aktif" ini penting untuk data lama yang dibuat sebelum kolom
	 * tersebut ada, dan ia sejalan dengan penyaring pada {@link #ambil(Session, FormSop)} yang juga
	 * menerima {@code aktif IS NULL}. Berbeda dari {@link DisposisiAlurSop#getAktif()} yang
	 * menghitung sendiri cabang mati, penanda di sini murni tersimpan apa adanya.</p>
	 *
	 * @return {@code true} bila pengajuan aktif
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif header pengajuan.
	 *
	 * @param aktif penanda aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan pengaju bila ia pengguna internal (pegawai/staf).
	 *
	 * <p>Inilah identitas "pemilik" pengajuan pada jalur internal, dan nilainya disalin ke baris
	 * jenjang awal maupun jenjang {@code kembaliKePengaju} oleh
	 * {@link DisposisiAlurSop#getDiajukanOleh()}.</p>
	 *
	 * <p><b>Getter destruktif:</b> bila slot mahasiswa atau siswa terisi, {@code diajukanOleh}
	 * dikosongkan agar satu pengajuan hanya punya satu jenis pengaju. Karena ini properti
	 * persisten, pengosongan itu dapat ikut tersimpan ke kolom {@code diajukan_oleh} pada flush
	 * berikutnya.</p>
	 *
	 * <p>Perlu dicatat dari sisi pengendalian: nilai ini adalah satu-satunya acuan "siapa pengaju"
	 * yang tersedia bila kelak hendak ditambahkan penjaga <i>self-approval</i> — saat ini tidak ada
	 * satu pun kode di entity ini yang membandingkannya dengan pelaku pada baris jenjang
	 * persetujuan.</p>
	 *
	 * @return pengguna internal pengaju, atau {@code null} bila pengajunya mahasiswa/siswa atau
	 *         belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diajukan_oleh", nullable = true)
	public Tbmuser getDiajukanOleh() {
		diajukanOleh = check(diajukanOleh);
		if (getMahasiswa() != null || getSiswa() != null) {
			diajukanOleh = null;
		}
		return diajukanOleh;
	}

	/**
	 * Menetapkan pengaju berupa pengguna internal.
	 *
	 * <p>Setter polos tanpa validasi: tidak diperiksa apakah pengguna tersebut berhak mengajukan
	 * prosedur ini, dan tidak diperiksa apakah slot pengaju lain sudah terisi (pembersihannya baru
	 * terjadi saat getter dibaca).</p>
	 *
	 * @param diajukanOleh pengguna internal pengaju
	 */
	public void setDiajukanOleh(Tbmuser diajukanOleh) {
		this.diajukanOleh = diajukanOleh;
	}

	/**
	 * Mengembalikan pengaju bila ia mahasiswa (jalur perguruan tinggi).
	 *
	 * <p>Berbeda dari {@link #getDiajukanOleh()}, getter ini tidak destruktif — ia hanya
	 * me-resolve proxy lazy lewat {@code check()}. Slot mahasiswa "menang" atas slot pengguna
	 * internal, karena getter pengguna internal-lah yang mengosongkan diri ketika slot ini
	 * terisi.</p>
	 *
	 * @return mahasiswa pengaju, atau {@code null} bila bukan jalur mahasiswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menetapkan pengaju berupa mahasiswa. Setter polos tanpa validasi.
	 *
	 * @param mahasiswa mahasiswa pengaju
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan pengaju bila ia siswa (jalur sekolah).
	 *
	 * <p>Kembar dari {@link #getMahasiswa()} untuk instalasi sekolah, dan sama-sama tidak
	 * destruktif. Sama pula perlakuannya terhadap slot pengguna internal: keberadaannya membuat
	 * {@link #getDiajukanOleh()} mengembalikan {@code null}.</p>
	 *
	 * @return siswa pengaju, atau {@code null} bila bukan jalur siswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menetapkan pengaju berupa siswa. Setter polos tanpa validasi.
	 *
	 * @param siswa siswa pengaju
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan penunjuk pintas ke <b>baris jenjang awal</b> pengajuan ini (langkah pengaju).
	 *
	 * <p>Dari baris inilah rantai {@code setelahnya} dapat ditelusuri maju untuk merekonstruksi
	 * seluruh riwayat. Penunjuk ini bersifat cache navigasi: kebenarannya bergantung pada lapis
	 * service yang mengisinya saat pengajuan dibuat, dan tidak ada mekanisme di entity yang
	 * memverifikasi bahwa baris yang ditunjuk benar-benar milik pengajuan ini atau benar-benar
	 * berjenjang {@code start}.</p>
	 *
	 * @return baris jenjang awal, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_start", nullable = true)
	public DisposisiAlurSop getDisposisiStart() {
		disposisiStart = check(disposisiStart);
		return disposisiStart;
	}

	/**
	 * Menetapkan penunjuk ke baris jenjang awal.
	 *
	 * <p>Berbeda dari {@link #setDisposisiEnd(DisposisiAlurSop)} dan
	 * {@link #setDisposisiSetuju(DisposisiAlurSop)} yang menyaring baris tanpa id, setter ini
	 * menerima apa pun termasuk {@code null} dan baris yang belum tersimpan.</p>
	 *
	 * @param disposisiStart baris jenjang awal
	 */
	public void setDisposisiStart(DisposisiAlurSop disposisiStart) {
		this.disposisiStart = disposisiStart;
	}

	/**
	 * Nilai bawaan kolom {@code properti}: teks {@link JSONObject} kosong ({@code "{}"}).
	 *
	 * <p>{@code public static} dan tidak final mengikuti pola entity AIS lainnya; perlakukan
	 * sebagai tetapan dan jangan menulisinya.</p>
	 */
	public static String JSON = new JSONObject().toString();

	/**
	 * Mengembalikan kantong properti bebas berformat JSON milik pengajuan ini.
	 *
	 * <p>Bila kosong dikembalikan {@code "{}"} ({@link #JSON}) agar pemanggil dapat langsung
	 * mengurai tanpa pemeriksaan tambahan. Nilai bawaan hanya dikembalikan, tidak ditulis balik ke
	 * field, jadi getter ini tidak destruktif.</p>
	 *
	 * @return teks JSON properti; {@code "{}"} bila belum diisi
	 */
	@Column(name = "properti", nullable = true, columnDefinition = "text")
	public String getProperti() {
		return properti == null || properti.isEmpty() ? JSON : properti;
	}

	/**
	 * Menyetel kantong properti bebas berformat JSON. Tidak ada pemeriksaan kesahihan JSON.
	 *
	 * @param properti teks JSON properti
	 */
	public void setProperti(String properti) {
		this.properti = properti;
	}

	@SuppressWarnings({})
	public boolean hapus() {

		DisposisiSop disposisiSop = this;
		String[] hps = new String[] { "akunting.daftar_pengajuan_transfer", "dana_talangan", "akunting.kas_kecil",
				"akunting.penggantian_kas_kecil", "akunting.pertangungjawaban", "akunting.proses_transfer",
				"disposisi_alur_sop", "akunting.proses_transitori", "uang_muka", "uang_muka",
				"asset.pembayaran_dp_master_asset", "asset.pembayaran_dp_master_asset",
				"asset.pembayaran_pengadaan_master_asset", "asset.pembayaran_termin_master_asset",
				"asset.pemesanan_pengadaan_master_asset", "asset.peminjaman_master_asset",
				"asset.penerimaan_pengadaan_master_asset", "asset.pengembalian_master_asset",
				"asset.penghapusan_master_asset", "asset.perbaikan_asset", "asset.perjanjian_kerjasama_master_asset",
				"asset.permintaan_pengadaan_master_asset", "sekolah.kelompok_pendaftaran_psb", "disposisi_alur_sop",
				"surat.surat_keluar", "surat.surat_masuk", "catatan_administrasi", "pengaduan", "pengajuan_mahasiswa",
				"pengajuan_pegawai", "ruang_pmb", "library.seleksi_vendor", "disposisi_alur_sop" };

		for (String c : hps) {

			try {

				String sql = "delete from " + c + " where disposisi_sop=" + disposisiSop.getId();
				Common.updateSql(sql);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sop/DisposisiSop.java:290");
			}

		}

		try {

			String sql = "delete from disposisi_sop where id=" + disposisiSop.getId();
			System.out.println("sql -> " + sql);
			Common.updateSql(sql);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sop/DisposisiSop.java:301");
		}

		return true;

	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_end", nullable = true)
	public DisposisiAlurSop getDisposisiEnd() {
		disposisiEnd = check(disposisiEnd);

		return disposisiEnd;
	}

	public void setDisposisiEnd(DisposisiAlurSop disposisiEnd) {
		this.disposisiEnd = disposisiEnd == null || disposisiEnd.getId() == null ? null : disposisiEnd;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_setujui", nullable = true)
	public DisposisiAlurSop getDisposisiSetuju() {

		try {
			disposisiSetuju = check(disposisiSetuju);

			/*
			 * ROOT CAUSE FIX: sebelumnya getDisposisiEnd() dipanggil berulang kali di
			 * dalam satu ekspresi boolean. Karena getDisposisiEnd() memuat proxy lazy
			 * Hibernate (via check()), dua pemanggilan berturut-turut bisa memberi hasil
			 * yang tidak konsisten (mis. non-null lalu null) saat dipanggil dari thread
			 * background dengan siklus-hidup session sendiri (lihat PenggunaanAnggaran$1)
			 * -> memicu NPE di rantai getter. Ambil sekali ke variabel lokal lalu
			 * null-check variabel tsb, bukan getter-nya.
			 */
			DisposisiAlurSop end = getDisposisiEnd();
			ais.database.model.sop.AlurSop endAlur = end == null ? null : end.getAlurSop();

			if (disposisiSetuju == null) {
				if (end != null && endAlur != null
						&& (Boolean.TRUE.equals(endAlur.getJikaProsesDisetujuiMakaSelesai()) || end.setujui())) {
					disposisiSetuju = end;
				}
			}

			DisposisiAlurSop setuju = disposisiSetuju;
			if (setuju != null && setuju.getAlurSop() != null && !setuju.setujui()) {
				DisposisiAlurSop setelahnya = setuju.getSetelahnya();
				ais.database.model.sop.AlurSop setelahnyaAlur = setelahnya == null ? null : setelahnya.getAlurSop();
				boolean tidakBolehLanjut = (setelahnya != null && setelahnyaAlur != null
						&& !Boolean.TRUE.equals(setelahnyaAlur.getPersetujuanAdaDiSini()))
						|| !Boolean.TRUE.equals(setuju.getAlurSop().getJikaProsesDisetujuiMakaSelesai());
				if (tidakBolehLanjut) {
					disposisiSetuju = null;
				}
			}

			if (end != null && end.getDiajukanOleh() != null && end.getSetelahnya() == null && endAlur != null
					&& Boolean.TRUE.equals(endAlur.getJikaProsesDisetujuiMakaSelesai())) {
				disposisiSetuju = end;
			}

			DisposisiAlurSop finalCandidate = disposisiSetuju;
			if (finalCandidate != null && finalCandidate.getId() != null && finalCandidate.getDiajukanOleh() == null
					&& finalCandidate.getMahasiswa() == null && finalCandidate.getSiswa() == null) {
				disposisiSetuju = null;
			}
		}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sop/DisposisiSop.java:352");
			// TODO: handle exception
		}

		return disposisiSetuju;
	}

	public void setDisposisiSetuju(DisposisiAlurSop a) {
		this.disposisiSetuju = a == null || a.getId() == null ? null : a;

	}

}
