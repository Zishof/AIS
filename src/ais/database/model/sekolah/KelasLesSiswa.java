package ais.database.model.sekolah;
// Generated 30 Sep 18 6:57:43 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Ruang;
import ais.database.model.Sertifikat;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;

/**
 * Entity INDUK <b>kelas les</b> sekolah — satu baris tabel {@code sekolah.kelas_les}
 * mewakili satu rombongan kegiatan pembelajaran <b>di luar rombongan belajar
 * reguler</b>: bimbingan belajar, kursus, ekstrakurikuler, tahfidz, klub, atau
 * kegiatan bersertifikat lain. Label layarnya "Kelas Les Siswa"
 * ({@code /pages/master/sekolah/kelas_les_siswa.zul}, menu id {@code 18618},
 * dikendalikan {@code ais.action.master.sekolah.KelasLesSiswaAction}).
 *
 * <h3>Bedanya dengan {@code KelasSiswa} (rombongan belajar reguler)</h3>
 * <p>Kedua entity mirip secara <i>tampilan</i> — nama empat bahasa, tingkat,
 * ruang, wali kelas, saklar kebijakan absensi/penilaian, blob absensi — tetapi
 * <b>berbeda tabel, berbeda induk pewarisan, dan berbeda peran</b>:</p>
 * <table border="1">
 *   <caption>Perbandingan ringkas</caption>
 *   <tr><th></th><th>{@code KelasSiswa}</th><th>{@code KelasLesSiswa} (berkas ini)</th></tr>
 *   <tr><td>Tabel</td><td>{@code sekolah.kelas}</td><td>{@code sekolah.kelas_les}</td></tr>
 *   <tr><td>Induk</td><td>{@code VoKunci}</td>
 *       <td>{@link VOPembelajaran} (yang sendiri turunan {@code VoKunci})</td></tr>
 *   <tr><td>Roster</td><td>{@code KelasSiswaPunyaSiswa}</td>
 *       <td>{@link KelasLesSiswaPunyaSiswa}</td></tr>
 *   <tr><td>Kalender</td><td>{@code tahunAjaran} (String tahun akademik)</td>
 *       <td>{@link #getMasaJadwalPelajaran()} (entity rentang tanggal tersendiri)</td></tr>
 *   <tr><td>Kelas aktif siswa</td><td>{@code Siswa.current_kelas_id}</td>
 *       <td>{@code Siswa.kelasLes} (kolom terpisah, lihat {@code Siswa.getKelasLes()})</td></tr>
 *   <tr><td>Kaitan biaya</td><td>tidak ada di entity induk</td>
 *       <td><b>ada</b> — {@link #getJenisBiayaSekolah()} dan
 *           {@link #getSyaratPengaturanPembayaran()}</td></tr>
 *   <tr><td>Mata pelajaran</td><td>pengecualian per kelas ({@code mpYgTidakDiambil})</td>
 *       <td><b>satu</b> {@link #getMatapelajaran()} wajib per kelas les</td></tr>
 *   <tr><td>Sertifikat</td><td>tidak ada</td><td><b>ada</b> — {@link #getSertifikat()}</td></tr>
 *   <tr><td>Guru BK</td><td>ada ({@code guruBk}, {@code absensiharusGuruBk})</td>
 *       <td>tidak ada</td></tr>
 * </table>
 *
 * <p>Konsekuensi terpenting dari pewarisan {@link VOPembelajaran}: kelas les
 * adalah <b>satuan pembelajaran ber-pertemuan</b> setara kelas kuliah. Ia
 * mewarisi seluruh mesin {@code Pertemuan}/e-learning
 * ({@code ambilPertemuan()}, {@code reInitPertemuan()}, {@code reInitTugas()},
 * {@code reInitUjian()}, {@code infoSimple()}, {@link #getCourse()}), sesuatu
 * yang {@code KelasSiswa} <b>tidak</b> punya. Tabel {@code pertemuan} memang
 * memiliki FK {@code KelasLesSiswa} tersendiri (lihat
 * {@code ais.database.model.Pertemuan.getKelasLesSiswa()}), begitu pula
 * {@code JadwalPelajaran} yang bisa menunjuk KELAS REGULER atau KELAS LES —
 * satu tabel jadwal melayani keduanya.</p>
 *
 * <h3>Arah relasi ke peserta</h3>
 * <p>Sama seperti kerabat regulernya, entity ini <b>tidak</b> menyimpan koleksi
 * peserta. Anggota kelas les adalah baris-baris {@link KelasLesSiswaPunyaSiswa}
 * yang menunjuk BALIK ke sini lewat properti {@code kelasLesSiswa} — relasi
 * satu arah <b>anak &rarr; induk</b>. Setiap pemanggil yang butuh daftar peserta
 * harus membuat query sendiri
 * ({@code createCriteria(KelasLesSiswaPunyaSiswa.class).add(Restrictions.eq("kelasLesSiswa", kelas))}),
 * pola yang dipakai {@code KelasLesSiswaAction}, {@code DetailKelasLesSiswaHelper},
 * {@code DetailPenilaianLesSiswaHelper}, dan {@code PengaturanBiaya.checkKelasLes()}.
 * Karena itu <b>tidak ada cascade delete</b> dari kelas les ke rosternya.</p>
 * <p>Perhatikan: peserta kelas les bisa berupa {@code Siswa} (murid aktif)
 * <b>maupun</b> {@code CalonSiswa} (pendaftar PSB) — {@link KelasLesSiswaPunyaSiswa}
 * punya dua FK terpisah untuk itu. Kelas les karena itu juga dipakai sebagai
 * wadah kegiatan berbayar bagi calon siswa yang belum resmi diterima.</p>
 *
 * <h3>Kelas les adalah kelas BERBAYAR (bagian yang paling non-obvious)</h3>
 * <p>Berbeda dari rombongan belajar reguler, keikutsertaan pada kelas les
 * <b>digerbangi pembayaran</b>. Ada dua mekanisme yang saling meniadakan:</p>
 * <ol>
 *   <li>{@link #getJenisBiayaSekolah()} — satu jenis biaya sekolah sebagai syarat.
 *       Bila terisi, mekanisme kedua <b>dimatikan sekaligus dihapus datanya</b>
 *       (lihat peringatan pada {@link #getSyaratPengaturanPembayaran()}).</li>
 *   <li>{@link #getSyaratPengaturanPembayaran()} — daftar CSV id
 *       {@link PengaturanBiaya} (bila periodenya "Bulanan": disertai bulan/tahun
 *       dalam bentuk {@code id_bulan_tahun}) yang harus sudah dilunasi.</li>
 * </ol>
 * <p>Penegakannya ada di sisi roster, bukan di sini:
 * {@code KelasLesSiswaPunyaSiswa.ada()} membandingkan CSV syarat ini dengan
 * {@code Siswa.getRiwayatPengaturanPembayaran()}/{@code CalonSiswa} yang sepadan,
 * lalu {@code KelasLesSiswaPunyaSiswa.getAktif()} <b>menimpa</b> flag aktif baris
 * roster dengan hasilnya. Praktisnya: peserta yang menunggak otomatis berubah
 * menjadi tidak aktif pada kelas les, tanpa ada yang menekan tombol apa pun.</p>
 *
 * <h3>Pengelompokan anggota kelas ini</h3>
 * <ol>
 *   <li><b>Jejak audit &amp; kunci</b> — {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()},
 *       {@link #getDikunci()}.</li>
 *   <li><b>Identitas kelas</b> — {@link #getNama()} beserta tiga varian bahasa
 *       {@link #getNamaEn()}/{@link #getNamaAr()}/{@link #getNamaCh()},
 *       {@link #getTingkat()}, {@link #getKeterangan()}, {@link #getAktif()}.</li>
 *   <li><b>Penempatan organisasi</b> — {@link #getSekolah()}, {@link #getYayasan()},
 *       {@link #getRuang()}.</li>
 *   <li><b>Muatan pembelajaran</b> — {@link #getMatapelajaran()},
 *       {@link #getMasaJadwalPelajaran()}, {@link #getCourse()},
 *       {@link #getUrutkanotomatis()},
 *       {@link #ambilJumlahDetailperkuliahanLangsung()}.</li>
 *   <li><b>Penanggung jawab</b> — {@link #getGuruPembina()} (label layarnya
 *       "Wali Kelas").</li>
 *   <li><b>Kebijakan absensi &amp; penilaian</b> —
 *       {@link #getAbsensiharusGuruPembina()},
 *       {@link #getPublikasiNilaiHarusTelahDiverifikasi()},
 *       {@link #getGuruBolehMemverifikasiSendiri()}. Ketiganya BERMASALAH pada
 *       kelas les; lihat "Hal non-obvious" di bawah.</li>
 *   <li><b>Syarat pembayaran</b> — {@link #getJenisBiayaSekolah()},
 *       {@link #getSyaratPengaturanPembayaran()}, {@link #ambilPengaturanBiaya()},
 *       {@link #ambilPengaturanBiayaId()},
 *       {@link #populateJenisPengaturanPembayaran(Set)}.</li>
 *   <li><b>Luaran</b> — {@link #getSertifikat()} (template sertifikat yang dicetak
 *       untuk peserta yang sudah di-ACC pada {@code DetailKelasLesSiswaHelper}).</li>
 *   <li><b>Blob absensi (KODE MATI)</b> — {@link #getAbsensi()},
 *       {@link #populate(String, Statusabsensi, String, String, String, String)},
 *       dan tujuh pembaca {@code retreiveAbsensi*}. Lihat catatan di bawah.</li>
 * </ol>
 *
 * <h3>Warisan {@code GeneralValueObject} (jangan "dirapikan")</h3>
 * <p>Rantai pewarisannya {@code KelasLesSiswa} &rarr; {@link VOPembelajaran} &rarr;
 * {@code ais.database.model.VoKunci} &rarr; {@code ais.database.model.sop.DataSop}
 * &rarr; {@link ais.database.model.GeneralValueObject}. Kelas-kelas dasar itu
 * <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} — semuanya POJO
 * abstrak biasa sehingga Hibernate <b>tidak</b> memetakan properti miliknya.
 * Karena itu pengulangan deklarasi field {@code id}, {@code oleh}, {@code olehId},
 * dan {@code tanggal_dirubah} di berkas ini <b>bukan duplikasi yang perlu
 * dibersihkan, melainkan keharusan teknis</b>: tanpa deklarasi ulang, keempat
 * kolom itu tidak akan ada di tabel.</p>
 *
 * <h3>Hal non-obvious yang perlu diketahui pemanggil</h3>
 * <ul>
 *   <li><b>Empat getter menulis balik ke field terpetakan.</b> Karena {@code @Id}
 *       dipasang pada getter (property access) dan {@code dynamicUpdate} aktif,
 *       nilai yang ditulis ulang oleh getter <b>ikut ter-flush ke basis data</b>
 *       begitu session ditutup — cukup dengan MEMBACA baris di grid:
 *       <ul>
 *         <li>{@link #getYayasan()} menimpa {@code yayasan} dengan
 *             {@code getSekolah().getYayasan()};</li>
 *         <li>{@link #getAbsensi()} memperbaiki format jam "9.400" &rarr; "09.40";</li>
 *         <li>{@link #getSyaratPengaturanPembayaran()} <b>MENGOSONGKAN</b> daftar
 *             syarat pembayaran bila {@link #getJenisBiayaSekolah()} terisi — ini
 *             yang paling berdampak, karena menghapus permanen konfigurasi
 *             finansial;</li>
 *         <li>{@link #getNamaEn()}/{@link #getNamaAr()}/{@link #getNamaCh()}
 *             mengembalikan {@link #getNama()} bila kosong, sehingga nama
 *             Indonesia akhirnya ter-<i>backfill</i> ke kolom bahasa lain.</li>
 *       </ul></li>
 *   <li><b>Saklar "Publikasi Nilai Harus Telah Diverifikasi" TIDAK BERFUNGSI pada
 *       kelas les.</b> Penegaknya ada di
 *       {@code VoKelasPunyaSiswa.retreiveDetailNilai()}/{@code retreiveTotalNilai()}
 *       lewat penjaga {@code ambilKelasSiswa() != null &&
 *       !ambilKelasSiswa().getPublikasiNilaiHarusTelahDiverifikasi()}. Method
 *       abstrak itu bertipe kembalian {@code KelasSiswa}, sehingga implementasinya
 *       di {@link KelasLesSiswaPunyaSiswa} <b>mengembalikan {@code null} apa
 *       adanya</b> ("TODO Auto-generated method stub"). Penjaga karena itu tidak
 *       pernah menyala untuk kelas les: mematikan centang ini di layar tidak
 *       melonggarkan filter nilai sama sekali (gagal-tertutup — nilai yang belum
 *       diverifikasi tetap disembunyikan).</li>
 *   <li><b>Saklar "Hanya boleh di-absen oleh Wali Kelas" berlaku TERBALIK.</b>
 *       {@code AmbilDataKelasSiswaBanbox} (kelas reguler) memakai
 *       {@code AND(absensiharusGuruPembina == true, guruPembina == guru)} sehingga
 *       kelas terkunci hanya muncul bagi wali kelasnya. Padanan kelas les,
 *       {@code AmbilDataKelasLesSiswaBanbox} baris 387, memakai <b>{@code OR}</b>
 *       untuk dua syarat yang sama — akibatnya menyalakan centang ini justru
 *       membuat kelas les tampil di pemilih absensi <b>SEMUA guru</b>, dan
 *       mematikannya membuat kelas hanya tampil bagi wali kelas. Lihat
 *       {@link #getAbsensiharusGuruPembina()}.</li>
 *   <li><b>Kolom {@code dikunci} tidak ditegakkan sama sekali.</b> Berbeda dari
 *       {@code KelasSiswa} yang setidaknya menyembunyikan tombol Simpan,
 *       {@code KelasLesSiswaAction} maupun {@code DetailKelasLesSiswaHelper}
 *       tidak pernah membaca {@link #getDikunci()}.</li>
 *   <li><b>Seluruh blok absensi adalah kode mati pada entity ini.</b>
 *       {@link #getAbsensi()}, {@link #populate} dan tujuh {@code retreiveAbsensi*}
 *       hasil salin-tempel dari {@code ais.database.model.Pertemuan}; tidak ada
 *       satu pun pemanggil di seluruh basis kode yang memakainya lewat
 *       {@code KelasLesSiswa} (semua pemanggil {@code retreiveAbsensi*} bekerja
 *       pada {@code Pertemuan}). Absensi kelas les yang sesungguhnya ditangani
 *       {@code Pertemuan} yang diwarisi dari {@link VOPembelajaran}.</li>
 *   <li><b>Kolom "kapasitas" tidak ada.</b> {@code KelasLesSiswaAction} mendaftarkan
 *       {@code "kapasitas"} pada daftar kolom cetak dan unggah Excel, padahal tidak
 *       ada properti bernama itu di seluruh rantai pewarisan — kolom hantu yang
 *       selalu kosong.</li>
 * </ul>
 *
 * <h3>Catatan keamanan yang terlihat dari sisi entity induk</h3>
 * <p><b>Verifikasi pola "Singkronkan" milik {@code KelasSiswaAction}: TERKONFIRMASI
 * TERULANG DI SINI, dan cakupannya lebih luas.</b> {@code KelasLesSiswaAction.doAfterCompose()}
 * memasang tombol toolbar "Singkronkan" <b>tanpa pemeriksaan hak apa pun</b> —
 * kontras dengan tetangganya di baris yang sama: tombol Tambah memakai
 * {@code CommonPrivilages.CREATE} dan tombol unggah Excel bahkan menuntut
 * CREATE+UPDATE+DELETE sekaligus. Tombol itu menjalankan thread latar yang, untuk
 * SETIAP peserta SETIAP kelas les hasil filter, memanggil
 * {@code Siswa.populate()}/{@code CalonSiswa.populate()} (menulis ulang seluruh
 * kolom ringkasan riwayat pembayaran siswa) lalu {@code siswa.setKelasLes(kelasLesSiswa)}
 * dan meng-{@code update} baris {@code sekolah.siswa}. Filter yayasan/sekolahnya
 * <b>fail-open</b>: bila kedua combo dibiarkan kosong (kondisi default layar),
 * kondisinya menjadi {@code Restrictions.sqlRestriction("true")} sehingga
 * jangkauannya <b>seluruh instalasi lintas tenant</b>. Perbedaan dengan versi
 * kelas reguler: di sana sinkronisasi memakai SQL native dan meminta tahun ajaran
 * lebih dulu; di sini tidak ada dialog konfirmasi sama sekali — satu klik langsung
 * jalan.</p>
 * <p><b>Panel detail roster juga tanpa gerbang</b> (pola yang sama persis dengan
 * temuan {@code DetailKelasSiswaHelper}): pada {@code DetailKelasLesSiswaHelper}
 * hanya "Ambil Siswa" ({@code CREATE}) dan tombol "Hapus" per baris
 * ({@code DELETE}) yang digerbangi, sedangkan <b>"Bersihkan" (menghapus SELURUH
 * roster kelas les), "Copy siswa dari kelas lain", dan unggah Excel roster tidak
 * punya gerbang hak sama sekali</b> — hak BACA menu "Kelas Les Siswa" sudah cukup
 * untuk mengosongkan peserta kelas les mana pun.</p>
 * <p><b>Pewarisan hak lewat menu induk, varian terburuk.</b>
 * {@code kelas_les_siswa.zul} menyisipkan dua layar master lain sebagai tab:
 * {@code onMasa()} memuat {@code /pages/master/sekolah/masa_jadwal_pelajaran.zul}
 * dan {@code onSertifikat()} memuat {@code /pages/master/sertifikat.zul}. Kedua
 * layar itu <b>tidak punya entri menu sendiri</b> di {@code MenuInitializer}/
 * {@code MenuSnapshotData}, sehingga pemeriksaan haknya jatuh ke menu terakhir yang
 * diklik pengguna — yakni menu "Kelas Les Siswa" ini. Hak atas satu menu bernilai
 * rendah karenanya membuka CRUD penuh atas master Masa Pembelajaran (yang dipakai
 * bersama oleh {@code JadwalPelajaranAction}) dan master Sertifikat (yang dipakai
 * bersama oleh {@code UjianAction} dan {@code FormulirKegiatanAction}). Seeder
 * bawaan memberi menu ini ke lima role sekaligus: {@code am}, {@code keu},
 * {@code amp}, {@code admsek}, {@code Akademik}.</p>
 * <p><b>Padanan terjadwal.</b> {@code PengaturanBiaya.checkKelasLes()} menjalankan
 * loop yang secara efektif identik dengan tombol "Singkronkan" setiap hari pukul
 * 01:01, tetapi <b>tanpa filter tenant apa pun</b> (memang disengaja, karena ia job
 * latar, bukan aksi pengguna). Artinya efek tombol itu memang "hanya" mempercepat
 * apa yang toh dilakukan tiap malam — yang tetap salah adalah tombolnya bisa
 * dipicu siapa saja yang bisa membuka layar, kapan saja, dan menulis ke tenant
 * lain.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see VOPembelajaran
 * @see KelasLesSiswaPunyaSiswa
 * @see KelasSiswa
 * @see PengaturanBiaya
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "kelas_les", schema = "sekolah")
public class KelasLesSiswa extends VOPembelajaran {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9157912161411433979L;
	/** Kunci utama tabel {@code sekolah.kelas_les}; dideklarasikan ulang karena kelas dasar tidak dipetakan Hibernate. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit); diisi {@code AuditTimestampInterceptor}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (jejak audit); diisi {@code AuditTimestampInterceptor}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Setter defensif:</b> nilai {@code null} atau string kosong/spasi
	 * <b>diabaikan diam-diam</b> — jejak audit lama dipertahankan, tidak ditimpa
	 * nilai kosong. Perilaku ini disengaja dan seragam di seluruh entity repo.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Setter defensif:</b> nilai {@code null} atau kosong diabaikan (lihat
	 * {@link #setOlehId(String)}).</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui jejak audit
	 * ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}) tepat sebelum baris
	 * ini di-{@code UPDATE}.
	 *
	 * <p>Dipanggil <b>oleh Hibernate</b>, bukan oleh kode aplikasi; delegasinya ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang
	 * membaca pengguna dari session yang sedang aktif.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Field pendukung properti {@code tanggal_dirubah}. Diinisialisasi ke waktu
	 * server saat objek dibuat sehingga baris baru selalu punya stempel waktu,
	 * bahkan bila {@link #onUpdate()} belum pernah jalan.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir (tidak pernah {@code null} untuk objek yang
	 *         dibuat lewat konstruktor, karena field diinisialisasi di deklarasi)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Field pendukung {@link #getRuang()} — ruangan tempat kelas les diselenggarakan. */
	private Ruang ruang;
	/** Field pendukung {@link #getSekolah()} — pemilik tenant tingkat sekolah. */
	private Sekolah sekolah;
	/** Field pendukung {@link #getMatapelajaran()} — mata pelajaran yang diampu kelas les ini. */
	private Matapelajaran matapelajaran;
	/** Field pendukung {@link #getNama()} — nama kelas les dalam bahasa Indonesia. */
	private String nama;
	/** Field pendukung {@link #getKeterangan()} — catatan bebas. */
	private String keterangan;
	/** Field pendukung {@link #getAktif()} — saklar aktif/arsip; {@code null} berarti aktif. */
	private Boolean aktif;
	/** Field pendukung {@link #getTingkat()} — tingkat/jenjang kelas les; {@code null} dibaca sebagai 0. */
	private Integer tingkat;
	/** Field pendukung {@link #getYayasan()} — tenant tingkat yayasan; <b>diturunkan</b> dari {@code sekolah}. */
	private Yayasan yayasan;
	/** Field pendukung {@link #getGuruPembina()} — guru penanggung jawab ("Wali Kelas" di layar). */
	private Guru guruPembina;
	/** Field pendukung {@link #getAbsensi()} — blob absensi warisan; <b>kode mati</b> pada entity ini. */
	private String absensi;
	/** Field pendukung {@link #getNamaAr()} — nama kelas les dalam aksara Arab. */
	private String namaAr;
	/** Field pendukung {@link #getNamaEn()} — nama kelas les dalam bahasa Inggris. */
	private String namaEn;
	/** Field pendukung {@link #getNamaCh()} — nama kelas les dalam aksara Tionghoa. */
	private String namaCh;

	/** Field pendukung {@link #getAbsensiharusGuruPembina()}; bawaan dibaca {@code true} bila {@code null}. */
	private Boolean absensiharusGuruPembina;
	/** Field pendukung {@link #getPublikasiNilaiHarusTelahDiverifikasi()}; bawaan dibaca {@code false} bila {@code null}. */
	private Boolean publikasiNilaiHarusTelahDiverifikasi;
	/** Field pendukung {@link #getGuruBolehMemverifikasiSendiri()}; bawaan dibaca {@code true} bila {@code null}. */
	private Boolean guruBolehMemverifikasiSendiri;
	/** Field pendukung {@link #getMasaJadwalPelajaran()} — rentang tanggal berlakunya kelas les. */
	private MasaJadwalPelajaran masaJadwalPelajaran;
	/** Field pendukung {@link #getJenisBiayaSekolah()} — syarat pembayaran bentuk pertama. */
	private JenisBiayaSekolah jenisBiayaSekolah;

	/** Field pendukung {@link #getSyaratPengaturanPembayaran()} — syarat pembayaran bentuk kedua (CSV). */
	private String syaratPengaturanPembayaran;
	/** Field pendukung {@link #getSertifikat()} — template sertifikat kelulusan kelas les. */
	private Sertifikat sertifikat;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate dan dipakai layar Tambah
	 * ({@code KelasLesSiswaAction.onAdd()}).
	 */
	public KelasLesSiswa() {
	}

	/**
	 * Konstruktor pembungkus id: membuat instance "kurus" yang hanya membawa kunci
	 * utama, berguna sebagai referensi pada kriteria query tanpa memuat baris penuh.
	 *
	 * @param id kunci utama {@code sekolah.kelas_les}
	 */
	public KelasLesSiswa(long id) {
		this.id = id;
	}

	/**
	 * Konstruktor warisan hasil generate hbm2java untuk kolom-kolom
	 * {@code nullable = false}.
	 *
	 * <p>Tidak dipakai kode aplikasi mana pun; dipertahankan agar kontrak hasil
	 * generate tidak berubah.</p>
	 *
	 * @param id      kunci utama
	 * @param ruang   ruangan penyelenggaraan
	 * @param nama    nama kelas les
	 * @param tingkat tingkat/jenjang
	 */
	public KelasLesSiswa(long id, Ruang ruang, String nama, Integer tingkat) {
		this.id = id;
		this.ruang = ruang;
		this.nama = nama;
		this.tingkat = tingkat;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} dengan strategi
	 * {@code IDENTITY} — nilainya diisi basis data saat {@code INSERT}, bukan oleh
	 * aplikasi. {@code null} berarti objek belum pernah disimpan.</p>
	 *
	 * @return id kelas les, atau {@code null} bila objek masih transient
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate. Menyetelnya manual pada objek yang
	 * sudah ada di session dapat membingungkan dirty-checking.</p>
	 *
	 * @param id kunci utama; boleh {@code null} untuk objek baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Field pendukung {@link #getDikunci()} — pengguna yang mengunci baris ini.
	 * Terisi, namun <b>tidak pernah dibaca</b> oleh layar kelas les mana pun.
	 */
	private Tbmuser dikunci;

	/**
	 * Mengembalikan pengguna yang mengunci baris ini.
	 *
	 * <p>Relasi {@code LAZY}; proxy diresolusi lewat
	 * {@code GeneralValueObject.check(Object)} sehingga pemanggil menerima objek
	 * terinisialisasi, bukan proxy mati.</p>
	 *
	 * <p><b>Catatan:</b> berbeda dari {@code KelasSiswa} yang setidaknya
	 * menyembunyikan tombol Simpan bila kelas terkunci, baik
	 * {@code KelasLesSiswaAction} maupun {@code DetailKelasLesSiswaHelper}
	 * <b>tidak pernah memeriksa</b> nilai ini — penguncian kelas les tidak
	 * ditegakkan sama sekali.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} bila baris tidak terkunci
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Menyetel pengguna pengunci baris ini.
	 *
	 * @param dikunci pengguna pengunci; {@code null} untuk membuka kunci
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Mengembalikan catatan bebas kelas les (kolom "Keterangan" di layar).
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas kelas les.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan ruangan tempat kelas les diselenggarakan.
	 *
	 * <p>Relasi {@code LAZY} opsional ({@code nullable = true}); proxy diresolusi
	 * lewat {@code check()}. Ditampilkan di grid sebagai
	 * "{@code kodeRuangan}-{@code nama}".</p>
	 *
	 * @return ruangan, atau {@code null} bila kelas les tidak terikat ruang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang_id", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);
		return this.ruang;
	}

	/**
	 * Menyetel ruangan penyelenggaraan.
	 *
	 * @param ruang ruangan; boleh {@code null}
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Mengembalikan sekolah pemilik kelas les ini — <b>pembatas tenant utama</b>
	 * bagi seluruh query pada layar kelas les.
	 *
	 * <p>Relasi {@code LAZY}; proxy diresolusi lewat {@code check()}.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik.
	 *
	 * <p><b>Normalisasi penting:</b> objek {@code Sekolah} yang ber-{@code id}
	 * {@code null} (yakni objek transient hasil resolusi tenant yang GAGAL — lihat
	 * {@code SekolahUtil.getSekolah()}) disimpan sebagai {@code null}, bukan
	 * sebagai objek kosong. Ini mencegah Hibernate mencoba meng-{@code INSERT}
	 * sekolah bayangan, tetapi juga berarti kelas les bisa berakhir <b>tanpa
	 * pemilik tenant</b> ketika resolusi tenant gagal.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek ber-id {@code null}
	 *                sama-sama menghasilkan {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik kelas les ini.
	 *
	 * <p><b>PERINGATAN — getter ini MENULIS BALIK.</b> Setiap kali dipanggil, ia
	 * menimpa field {@code yayasan} dengan {@code getSekolah().getYayasan()}.
	 * Yayasan yang di-set eksplisit lewat {@link #setYayasan(Yayasan)} akan lenyap
	 * begitu getter dipanggil, dan karena {@code yayasan} adalah properti
	 * terpetakan pada entity {@code dynamicUpdate}, perubahan itu ikut ter-flush ke
	 * kolom {@code yayasan_id}. Praktisnya kolom itu adalah <b>turunan</b> dari
	 * sekolah, bukan nilai mandiri — cukup dengan merender satu baris grid,
	 * basis data ikut berubah.</p>
	 * <p>Bila {@link #getSekolah()} {@code null} (tenant belum ditetapkan), nilai
	 * lama dipertahankan lalu tetap dilewatkan {@code check()}.</p>
	 *
	 * @return yayasan pemilik, atau {@code null} bila sekolah maupun yayasan belum
	 *         ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik.
	 *
	 * <p>Normalisasi sama dengan {@link #setSekolah(Sekolah)}: objek ber-{@code id}
	 * {@code null} disimpan sebagai {@code null}. Perlu diingat nilai yang disetel
	 * di sini <b>akan ditimpa</b> pada pembacaan {@link #getYayasan()} berikutnya
	 * bila sekolah sudah terisi.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek ber-id {@code null}
	 *                sama-sama menghasilkan {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan nama kelas les (bahasa Indonesia) dalam bentuk sudah di-{@code trim}.
	 *
	 * <p><b>Kuirk:</b> kolomnya dideklarasikan {@code nullable = false}, namun
	 * getter ini mengembalikan {@code null} bila field kosong atau hanya berisi
	 * spasi. Pemanggil yang merender grid
	 * ({@code KelasLesSiswaAction.KelasLesSiswaRenderer} membandingkan
	 * {@code getNama().equalsIgnoreCase(getNamaEn())}) karena itu akan
	 * {@code NullPointerException} pada baris bernama kosong. Layar Simpan
	 * memvalidasi nama tidak boleh kosong, sehingga baris seperti itu hanya bisa
	 * masuk lewat unggahan Excel atau tulisan langsung ke basis data.</p>
	 *
	 * @return nama kelas les tanpa spasi tepi, atau {@code null} bila kosong
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama == null || this.nama.trim().isEmpty() ? null : nama.trim();
	}

	/**
	 * Menyetel nama kelas les (bahasa Indonesia).
	 *
	 * @param nama nama kelas les; disimpan apa adanya (pemangkasan spasi terjadi di getter)
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan tingkat/jenjang kelas les.
	 *
	 * <p>Nilai {@code null} dibaca sebagai {@code 0} sehingga pemanggil aman
	 * memanggil {@code getTingkat().toString()} (dipakai langsung oleh renderer
	 * grid). Nilai {@code 0} yang dikembalikan <b>tidak</b> ditulis balik ke field,
	 * jadi kolomnya tetap {@code null} di basis data.</p>
	 *
	 * @return tingkat kelas les; {@code 0} bila belum diisi (tidak pernah {@code null})
	 */
	@Column(name = "tingkat", nullable = false)
	public Integer getTingkat() {
		return this.tingkat == null ? 0 : tingkat;
	}

	/**
	 * Menyetel tingkat/jenjang kelas les.
	 *
	 * @param tingkat tingkat; boleh {@code null}
	 */
	public void setTingkat(Integer tingkat) {
		this.tingkat = tingkat;
	}

	/**
	 * Mengembalikan status aktif kelas les.
	 *
	 * <p>Bawaan <b>aktif</b>: {@code null} dibaca sebagai {@code true}, sehingga
	 * baris lama yang belum punya kolom ini tetap muncul di layar. Filter
	 * "Tampilkan hanya yang aktif" pada {@code KelasLesSiswaAction.initCriteria()}
	 * karena itu menulis {@code isNull("aktif") OR eq("aktif", true)}, bukan
	 * sekadar {@code eq(true)}.</p>
	 *
	 * @return {@code true} bila kelas les aktif (termasuk saat kolom belum diisi)
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif kelas les.
	 *
	 * <p>Dipanggil dari checkbox "Aktif" per baris grid, yang langsung
	 * menyimpan lewat {@code Common.refreshSaveOrUpdate()}. Checkbox itu
	 * dinonaktifkan bila pengguna tidak punya hak {@code UPDATE}.</p>
	 *
	 * @param aktif status aktif; {@code null} akan dibaca sebagai aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan guru penanggung jawab kelas les (label layarnya "Wali Kelas",
	 * label kolom grid "Pembina").
	 *
	 * <p>Relasi {@code LAZY} opsional; proxy diresolusi lewat {@code check()}.
	 * Selain ditampilkan, nilai ini dipakai {@code AmbilDataKelasLesSiswaBanbox}
	 * sebagai penentu kelas les mana yang boleh diabsen guru yang sedang login —
	 * tetapi dengan logika yang terbalik, lihat
	 * {@link #getAbsensiharusGuruPembina()}. Nilai ini juga menjadi filter
	 * pencarian di layar master ({@code searchguruPembina}).</p>
	 *
	 * @return guru pembina, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru_pembina", nullable = true)
	public Guru getGuruPembina() {
		guruPembina = check(guruPembina);
		return guruPembina;
	}

	/**
	 * Menyetel guru pembina kelas les.
	 *
	 * <p><b>Efek samping layar:</b> {@code KelasLesSiswaAction.init()} memanggil
	 * setter ini <b>sebelum</b> form ditampilkan bila bandbox pencarian pembina
	 * sedang terisi — sehingga membuka form Tambah dari layar yang sudah difilter
	 * per guru otomatis memilih guru tersebut sebagai pembina.</p>
	 *
	 * @param guruPembina guru pembina; boleh {@code null}
	 */
	public void setGuruPembina(Guru guruPembina) {
		this.guruPembina = guruPembina;
	}

	/**
	 * Mengembalikan blob absensi kelas les.
	 *
	 * <p><b>KODE MATI.</b> Kolom {@code text} ini beserta seluruh API pembacanya
	 * ({@code retreiveAbsensi*}) dan penulisnya
	 * ({@link #populate(String, Statusabsensi, String, String, String, String)})
	 * merupakan salinan dari {@code ais.database.model.Pertemuan}; tidak ada satu
	 * pun pemanggil di basis kode yang memakainya lewat {@code KelasLesSiswa}.
	 * Absensi kelas les yang sesungguhnya berjalan lewat {@code Pertemuan} yang
	 * diwarisi dari {@link VOPembelajaran}.</p>
	 *
	 * <p><b>PERINGATAN — getter ini MENULIS BALIK.</b> Bila isi blob mengandung
	 * substring {@code "9.400"}, getter memperbaikinya menjadi {@code "09.40"} dan
	 * <b>menyimpan hasilnya ke field</b>. Karena properti ini terpetakan dan
	 * {@code dynamicUpdate} aktif, koreksi itu ikut ter-flush ke basis data hanya
	 * dengan membaca nilainya.</p>
	 *
	 * <p><b>Format blob:</b> catatan dipisah titik-koma, tiap catatan berisi 9
	 * ruas dipisah koma:
	 * {@code ref,statusabsensiId,kode,nama,0,keterangan,mulai,sampai,jenis}
	 * (ruas ke-5 selalu konstanta {@code "0"}).</p>
	 *
	 * @return isi blob absensi sudah di-{@code trim}; string kosong bila belum ada
	 *         (tidak pernah {@code null})
	 */
	@Column(name = "absensi", columnDefinition = "text")
	public String getAbsensi() {
		if (absensi != null && StringUtils.contains(absensi, "9.400")) {
			absensi = org.apache.commons.lang3.StringUtils.replace(absensi, "9.400", "09.40");
		}
		return absensi == null ? "" : absensi.trim();
	}

	/**
	 * Menyetel blob absensi mentah.
	 *
	 * <p>Menimpa seluruh catatan sekaligus; untuk mengubah satu catatan saja
	 * gunakan {@link #populate(String, Statusabsensi, String, String, String, String)}.</p>
	 *
	 * @param absensi blob absensi dalam format yang dijelaskan pada {@link #getAbsensi()}
	 */
	public void setAbsensi(String absensi) {
		this.absensi = absensi;
	}

	/**
	 * Membaca <b>kode</b> status kehadiran (ruas ke-3) untuk satu referensi dari
	 * blob {@link #getAbsensi()}.
	 *
	 * <p>Bagian dari API blob absensi yang berstatus kode mati pada entity ini.
	 * Catatan rusak dilewati diam-diam ({@code catch} yang hanya mencatat ke
	 * audit).</p>
	 *
	 * @param ref penanda baris absensi yang dicari (biasanya id peserta sebagai
	 *            string); {@code null} langsung menghasilkan {@code "-"}
	 * @return kode status kehadiran (mis. {@code "H"}, {@code "M"}), atau
	 *         {@code "-"} bila tidak ditemukan
	 */
	public String retreiveAbsensiKode(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswa.java:259");

				}
			}
		}

		return "-";
	}

	/**
	 * Membaca <b>nama</b> status kehadiran (ruas ke-4) untuk satu referensi dari
	 * blob {@link #getAbsensi()}.
	 *
	 * <p>Bagian dari API blob absensi yang berstatus kode mati pada entity ini.</p>
	 *
	 * @param ref penanda baris absensi yang dicari; {@code null} menghasilkan {@code "-"}
	 * @return nama status kehadiran (mis. "Hadir", "Sakit"), atau {@code "-"} bila
	 *         tidak ditemukan
	 */
	public String retreiveAbsensiNama(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[3];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswa.java:279");

				}
			}
		}

		return "-";
	}

	/**
	 * Membaca <b>keterangan</b> kehadiran (ruas ke-6) untuk satu referensi dari
	 * blob {@link #getAbsensi()}.
	 *
	 * <p>Berbeda dari {@link #retreiveAbsensiKode(String)}, pemisahan memakai
	 * {@code split(",", 9)} agar ruas kosong di ujung tidak dibuang — perlu karena
	 * keterangan/mulai/sampai/jenis kerap kosong.</p>
	 *
	 * @param ref penanda baris absensi yang dicari; {@code null} menghasilkan string kosong
	 * @return keterangan kehadiran, atau string kosong bila tidak ditemukan
	 */
	public String retreiveAbsensiKeterangan(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[5];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswa.java:299");

				}
			}
		}

		return "";
	}

	/**
	 * Membaca <b>id {@code Statusabsensi}</b> (ruas ke-2) untuk satu referensi dari
	 * blob {@link #getAbsensi()}.
	 *
	 * @param ref penanda baris absensi yang dicari; {@code null} menghasilkan {@code -1L}
	 * @return id status absensi, atau {@code -1L} bila tidak ditemukan atau tidak
	 *         dapat diurai sebagai angka
	 */
	public Long retreiveAbsensiId(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return Long.parseLong(s[1]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswa.java:319");

				}
			}
		}

		return -1L;
	}

	/**
	 * Membaca jam <b>mulai</b> ketidakhadiran (ruas ke-7) untuk satu referensi dari
	 * blob {@link #getAbsensi()}.
	 *
	 * <p>Hanya terisi bila status absensinya berkode {@code "M"} — lihat
	 * {@link #populate(String, Statusabsensi, String, String, String, String)} yang
	 * mengosongkan mulai/sampai untuk kode lain.</p>
	 *
	 * @param ref penanda baris absensi yang dicari; {@code null} menghasilkan string kosong
	 * @return jam mulai, atau string kosong bila tidak ditemukan/tidak relevan
	 */
	public String retreiveAbsensiMulai(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[6];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswa.java:339");

				}
			}
		}

		return "";
	}

	/**
	 * Membaca jam <b>selesai</b> ketidakhadiran (ruas ke-8) untuk satu referensi
	 * dari blob {@link #getAbsensi()}.
	 *
	 * @param ref penanda baris absensi yang dicari; {@code null} menghasilkan string kosong
	 * @return jam selesai, atau string kosong bila tidak ditemukan/tidak relevan
	 */
	public String retreiveAbsensiSampai(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[7];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswa.java:359");

				}
			}
		}

		return "";
	}

	/**
	 * Membaca <b>jenis</b> catatan kehadiran (ruas ke-9, ruas terakhir) untuk satu
	 * referensi dari blob {@link #getAbsensi()}.
	 *
	 * @param ref penanda baris absensi yang dicari; {@code null} menghasilkan string kosong
	 * @return jenis catatan, atau string kosong bila tidak ditemukan
	 */
	public String retreiveAbsensiJenis(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[8];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswa.java:379");

				}
			}
		}

		return "";
	}

	/**
	 * Menyisipkan atau memperbarui SATU catatan kehadiran di dalam blob
	 * {@link #getAbsensi()}, lalu menulis ulang seluruh blob.
	 *
	 * <p><b>KODE MATI pada entity ini</b> — tidak ada pemanggil; padanan hidupnya
	 * adalah {@code Pertemuan.populate(...)} yang dipakai
	 * {@code ElearningApiUtil} dan helper absensi.</p>
	 *
	 * <p>Alur kerjanya:</p>
	 * <ol>
	 *   <li>Bila kode status BUKAN {@code "M"}, {@code mulai} dan {@code sampai}
	 *       dipaksa kosong (rentang jam hanya bermakna untuk status itu).</li>
	 *   <li>{@code keterangan} disanitasi agar tidak merusak format: {@code ";"}
	 *       diganti {@code "..\n"} dan {@code ","} diganti {@code "_"}. Parameter
	 *       {@code mulai}/{@code sampai}/{@code jenis} <b>tidak</b> disanitasi.</li>
	 *   <li>Blob lama ditelusuri; catatan ber-{@code ref} sama ditimpa, catatan lain
	 *       disalin apa adanya, catatan berruas pertama kosong dibuang.</li>
	 *   <li>Bila {@code ref} belum ada, catatan baru ditambahkan di ujung.</li>
	 * </ol>
	 * <p>Parameter bernilai {@code null} berarti "pertahankan nilai lama" — nilai
	 * lama diambil kembali lewat {@code retreiveAbsensi*} yang sepadan.</p>
	 * <p><b>Efek samping:</b> menulis langsung ke field {@code absensi} (bukan lewat
	 * setter), sehingga perubahan ikut ter-flush pada penutupan session. Tidak
	 * melakukan apa pun bila {@code ref} atau {@code statusabsensi} {@code null}.</p>
	 * <p><b>Kuirk deduplikasi:</b> himpunan {@code udahAda} hanya diisi untuk
	 * {@code ref} yang cocok, sehingga catatan ganda milik referensi LAIN tetap
	 * dibiarkan ganda.</p>
	 *
	 * @param ref           penanda baris absensi (biasanya id peserta sebagai string)
	 * @param statusabsensi status kehadiran yang akan dicatat; wajib, {@code null}
	 *                      membuat method tidak melakukan apa pun
	 * @param keterangan    keterangan bebas; {@code null} berarti pertahankan nilai lama
	 * @param mulai         jam mulai; {@code null} berarti pertahankan nilai lama
	 * @param sampai        jam selesai; {@code null} berarti pertahankan nilai lama
	 * @param jenis         jenis catatan; {@code null} berarti pertahankan nilai lama
	 */
	public void populate(String ref, Statusabsensi statusabsensi, String keterangan, String mulai, String sampai,
			String jenis) {
		if (ref != null && statusabsensi != null) {

			if (statusabsensi.getKode() == null || !statusabsensi.getKode().equals("M")) {
				mulai = "";
				sampai = "";
			}

			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ";", "..\n");
			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ",", "_");
			String formatBaru = "";
			String[] nilais = getAbsensi().split(";");
			Boolean ada = false;
			Set<String> udahAda = new HashSet<String>();
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = nn.split(",");
					if (!s[0].trim().isEmpty()) {
						String formatId = (s[0]);
						if (!udahAda.contains(formatId)) {
							if (ref.equals(formatId)) {
								udahAda.add(formatId);
								aformatBaru = ref + "," + statusabsensi.getId() + "," + statusabsensi.getKode() + ","
										+ statusabsensi.getNama() + ",0,"
										+ (keterangan == null ? retreiveAbsensiKeterangan(ref) : keterangan) + ","
										+ (mulai == null ? retreiveAbsensiMulai(ref) : mulai) + ","
										+ (sampai == null ? retreiveAbsensiSampai(ref) : sampai) + ","
										+ (jenis == null ? retreiveAbsensiJenis(ref) : jenis);
								ada = true;
							} else {
								aformatBaru = nn;
							}
							if (!aformatBaru.trim().isEmpty()) {
								formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
							}
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (!ada) {
				String aformatBaru = ref + "," + statusabsensi.getId() + "," + statusabsensi.getKode() + ","
						+ statusabsensi.getNama() + ",0,"
						+ (keterangan == null ? retreiveAbsensiKeterangan(ref) : keterangan) + ","
						+ (mulai == null ? retreiveAbsensiMulai(ref) : mulai) + ","
						+ (sampai == null ? retreiveAbsensiSampai(ref) : sampai) + ","
						+ (jenis == null ? retreiveAbsensiJenis(ref) : jenis);
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			// System.out.println("formatBaru => " + formatBaru);

			absensi = formatBaru;
		}
	}

	/**
	 * Mengembalikan nama kelas les dalam bahasa Inggris.
	 *
	 * <p><b>Fallback yang ter-<i>backfill</i>:</b> bila kolomnya kosong, getter
	 * mengembalikan {@link #getNama()}. Karena properti ini terpetakan dengan
	 * property access, nilai fallback itulah yang dibaca Hibernate saat
	 * dirty-checking — sehingga nama Indonesia akhirnya <b>tersimpan permanen</b>
	 * ke kolom nama Inggris pada flush berikutnya. Renderer grid sudah
	 * mengantisipasinya dengan hanya menampilkan varian bahasa bila berbeda dari
	 * {@link #getNama()}.</p>
	 *
	 * @return nama bahasa Inggris, atau nama Indonesia bila belum diisi
	 */
	public String getNamaEn() {
		return namaEn == null ? getNama() : namaEn;
	}

	/**
	 * Menyetel nama kelas les dalam bahasa Inggris.
	 *
	 * @param namaEn nama bahasa Inggris; {@code null} mengaktifkan fallback ke {@link #getNama()}
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

	/**
	 * Mengembalikan nama kelas les dalam aksara Arab.
	 *
	 * <p>Fallback dan efek <i>backfill</i>-nya sama dengan {@link #getNamaEn()}.
	 * Baris isian ini disembunyikan ({@code setVisible(false)}) pada form
	 * Tambah/Ubah, sehingga praktis hanya terisi lewat unggahan Excel.</p>
	 *
	 * @return nama aksara Arab, atau nama Indonesia bila belum diisi
	 */
	public String getNamaAr() {
		return namaAr == null ? getNama() : namaAr;
	}

	/**
	 * Menyetel nama kelas les dalam aksara Arab.
	 *
	 * @param namaAr nama aksara Arab; {@code null} mengaktifkan fallback ke {@link #getNama()}
	 */
	public void setNamaAr(String namaAr) {
		this.namaAr = namaAr;
	}

	/**
	 * Mengembalikan nama kelas les dalam aksara Tionghoa.
	 *
	 * <p>Fallback dan efek <i>backfill</i>-nya sama dengan {@link #getNamaEn()};
	 * baris isiannya juga disembunyikan di form.</p>
	 *
	 * @return nama aksara Tionghoa, atau nama Indonesia bila belum diisi
	 */
	public String getNamaCh() {
		return namaCh == null ? getNama() : namaCh;
	}

	/**
	 * Menyetel nama kelas les dalam aksara Tionghoa.
	 *
	 * @param namaCh nama aksara Tionghoa; {@code null} mengaktifkan fallback ke {@link #getNama()}
	 */
	public void setNamaCh(String namaCh) {
		this.namaCh = namaCh;
	}

	/**
	 * Mengembalikan saklar "Hanya boleh di-absen oleh Wali Kelas".
	 *
	 * <p>Bawaannya {@code true} ({@code null} dibaca sebagai {@code true}) —
	 * artinya kelas les lama yang belum punya kolom ini dianggap TERKUNCI ke wali
	 * kelas.</p>
	 *
	 * <p><b>PERINGATAN — penegakannya TERBALIK pada kelas les.</b> Satu-satunya
	 * penegak adalah {@code AmbilDataKelasLesSiswaBanbox.onSearchDefault()}:
	 * <pre>{@code Restrictions.or(Restrictions.eq("absensiharusGuruPembina", true),
	 *                  Restrictions.eq("guruPembina", guru))}</pre>
	 * Padanan kelas reguler ({@code AmbilDataKelasSiswaBanbox}) memakai
	 * {@code Restrictions.and(...)} untuk dua syarat yang persis sama. Akibat
	 * operator {@code OR}: menyalakan saklar ini justru membuat kelas les tampil di
	 * pemilih absensi <b>SEMUA guru</b> yang login, sementara mematikannya membuat
	 * kelas hanya tampil bagi wali kelasnya. Nilai bawaan {@code true} berarti
	 * perilaku salah itulah yang berlaku untuk sebagian besar data.</p>
	 * <p>{@code AbsenPiketAction} membaca kolom bernama sama, tetapi pada entity
	 * {@code KelasSiswa}, bukan di sini.</p>
	 *
	 * @return {@code true} bila absensi dibatasi ke wali kelas menurut niat
	 *         konfigurasi (perilaku nyatanya justru sebaliknya)
	 */
	public Boolean getAbsensiharusGuruPembina() {
		return absensiharusGuruPembina == null ? true : absensiharusGuruPembina;
	}

	/**
	 * Menyetel saklar "Hanya boleh di-absen oleh Wali Kelas".
	 *
	 * @param absensiharusGuruPembina nilai saklar; {@code null} dibaca sebagai {@code true}
	 */
	public void setAbsensiharusGuruPembina(Boolean absensiharusGuruPembina) {
		this.absensiharusGuruPembina = absensiharusGuruPembina;
	}

	/**
	 * Mengembalikan saklar "Publikasi Nilai Harus Telah Diverifikasi".
	 *
	 * <p>Bawaannya {@code false} ({@code null} dibaca sebagai {@code false}).</p>
	 *
	 * <p><b>PERINGATAN — saklar ini TIDAK BERFUNGSI untuk kelas les.</b>
	 * Penegaknya, {@code VoKelasPunyaSiswa.retreiveDetailNilai()} dan
	 * {@code retreiveTotalNilai()}, memakai penjaga
	 * {@code ambilKelasSiswa() != null && !ambilKelasSiswa().getPublikasiNilaiHarusTelahDiverifikasi()}
	 * untuk melonggarkan filter "hanya nilai terverifikasi". Method abstrak
	 * {@code ambilKelasSiswa()} bertipe kembalian {@code KelasSiswa}, sehingga
	 * implementasinya di {@link KelasLesSiswaPunyaSiswa} berupa stub yang
	 * mengembalikan {@code null}. Penjaga itu karena itu tidak pernah menyala pada
	 * kelas les: mematikan centang di layar tidak mengubah apa pun (gagal-tertutup
	 * — nilai belum terverifikasi tetap disembunyikan).</p>
	 *
	 * @return {@code true} bila publikasi nilai mensyaratkan verifikasi lebih dulu
	 */
	public Boolean getPublikasiNilaiHarusTelahDiverifikasi() {
		return publikasiNilaiHarusTelahDiverifikasi == null ? false : publikasiNilaiHarusTelahDiverifikasi;
	}

	/**
	 * Menyetel saklar "Publikasi Nilai Harus Telah Diverifikasi".
	 *
	 * @param publikasiNilaiHarusTelahDiverifikasi nilai saklar; {@code null} dibaca
	 *                                             sebagai {@code false}
	 */
	public void setPublikasiNilaiHarusTelahDiverifikasi(Boolean publikasiNilaiHarusTelahDiverifikasi) {
		this.publikasiNilaiHarusTelahDiverifikasi = publikasiNilaiHarusTelahDiverifikasi;
	}

	/**
	 * Mengembalikan saklar "Guru Boleh Mem-verifikasi Sendiri Nilai".
	 *
	 * <p>Bawaannya {@code true} ({@code null} dibaca sebagai {@code true}) —
	 * artinya secara bawaan guru pengampu boleh memverifikasi nilainya sendiri.
	 * Berbeda dari dua saklar di atasnya, saklar ini <b>benar-benar ditegakkan</b>
	 * untuk kelas les: {@code DetailPenilaianLesSiswaHelper} membacanya sebelum
	 * mengizinkan tombol verifikasi nilai.</p>
	 *
	 * @return {@code true} bila guru boleh memverifikasi nilainya sendiri
	 */
	public Boolean getGuruBolehMemverifikasiSendiri() {
		return guruBolehMemverifikasiSendiri == null ? true : guruBolehMemverifikasiSendiri;
	}

	/**
	 * Menyetel saklar "Guru Boleh Mem-verifikasi Sendiri Nilai".
	 *
	 * @param guruBolehMemverifikasiSendiri nilai saklar; {@code null} dibaca sebagai {@code true}
	 */
	public void setGuruBolehMemverifikasiSendiri(Boolean guruBolehMemverifikasiSendiri) {
		this.guruBolehMemverifikasiSendiri = guruBolehMemverifikasiSendiri;
	}

	/**
	 * Mengembalikan mata pelajaran yang diampu kelas les ini.
	 *
	 * <p>Relasi {@code LAZY}; proxy diresolusi lewat {@code check()}. <b>Wajib
	 * diisi</b> menurut validasi {@code KelasLesSiswaAction.onSave()} — inilah
	 * salah satu perbedaan struktural dengan {@code KelasSiswa} yang tidak punya
	 * mata pelajaran tunggal. Combo pilihannya dibatasi mata pelajaran milik
	 * sekolah terpilih (atau yang {@code sekolah}-nya {@code null} alias global)
	 * dan berstatus aktif.</p>
	 *
	 * @return mata pelajaran kelas les, atau {@code null} pada baris lama yang
	 *         belum terisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matapelajaran_id")
	public Matapelajaran getMatapelajaran() {
		matapelajaran = check(matapelajaran);
		return matapelajaran;
	}

	/**
	 * Menyetel mata pelajaran kelas les.
	 *
	 * @param matapelajaran mata pelajaran; boleh {@code null}
	 */
	public void setMatapelajaran(Matapelajaran matapelajaran) {
		this.matapelajaran = matapelajaran;
	}

	/**
	 * Mengembalikan template sertifikat yang dicetak untuk peserta kelas les ini.
	 *
	 * <p>Relasi {@code LAZY} opsional; proxy diresolusi lewat {@code check()}.
	 * Tombol "Sertifikat" pada panel detail peserta
	 * ({@code DetailKelasLesSiswaHelper}) hanya muncul bila peserta sudah di-ACC
	 * <b>dan</b> nilai ini tidak {@code null}. Combo pilihannya diberi opsi
	 * "== Tanpa Sertifikat ==" untuk kegiatan yang memang tidak bersertifikat.</p>
	 *
	 * @return template sertifikat, atau {@code null} bila kelas les tanpa sertifikat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sertifikat", nullable = true)
	public Sertifikat getSertifikat() {
		sertifikat = check(sertifikat);
		return sertifikat;
	}

	/**
	 * Menyetel template sertifikat kelas les.
	 *
	 * @param sertifikat template sertifikat; {@code null} berarti tanpa sertifikat
	 */
	public void setSertifikat(Sertifikat sertifikat) {
		this.sertifikat = sertifikat;
	}

	/**
	 * Mengembalikan jumlah "detail perkuliahan langsung" untuk satuan pembelajaran ini.
	 *
	 * <p>Implementasi kontrak abstrak {@link VOPembelajaran}. Pada modul
	 * perkuliahan nilai ini menghitung kelas paralel yang digabung; untuk kelas les
	 * sekolah selalu <b>satu</b>, sehingga dikembalikan konstanta {@code 1}. Dipakai
	 * mesin pertemuan/statistik yang diwarisi dari kelas induk saat membagi
	 * agregat.</p>
	 *
	 * @return selalu {@code 1}
	 */
	@Override
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		// TODO Auto-generated method stub
		return 1;
	}

	/** Field pendukung {@link #getCourse()} — muatan e-learning kelas les sebagai teks JSON. */
	private String course;
	/** Field pendukung {@link #getUrutkanotomatis()}; bawaan dibaca {@code true} bila {@code null}. */
	private Boolean urutkanotomatis;

	/**
	 * Mengembalikan muatan e-learning (silabus/materi/urutan pertemuan) kelas les
	 * sebagai teks JSON.
	 *
	 * <p>Implementasi kontrak abstrak {@link VOPembelajaran}; dikonsumsi mesin
	 * pertemuan/LMS yang diwarisi dari kelas induk. Bila kolom kosong,
	 * dikembalikan {@code "{}"} (hasil {@code new JSONObject().toString()}) supaya
	 * pemanggil selalu bisa langsung mengurai tanpa cek {@code null}. Nilai
	 * pengganti itu <b>tidak</b> ditulis balik ke field, jadi kolomnya tetap
	 * {@code null} di basis data.</p>
	 *
	 * @return teks JSON muatan pembelajaran; {@code "{}"} bila belum ada (tidak
	 *         pernah {@code null})
	 */
	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	/**
	 * Menyetel muatan e-learning kelas les.
	 *
	 * @param course teks JSON muatan pembelajaran; boleh {@code null}
	 */
	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	/**
	 * Mengembalikan saklar pengurutan otomatis pertemuan/materi.
	 *
	 * <p>Implementasi kontrak abstrak {@link VOPembelajaran}. Bawaannya
	 * {@code true} ({@code null} dibaca sebagai {@code true}), sehingga kelas les
	 * lama tetap mengurutkan pertemuan secara otomatis.</p>
	 *
	 * @return {@code true} bila pertemuan/materi diurutkan otomatis
	 */
	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	/**
	 * Menyetel saklar pengurutan otomatis pertemuan/materi.
	 *
	 * @param urutkanotomatis nilai saklar; {@code null} dibaca sebagai {@code true}
	 */
	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}

	/**
	 * Mengembalikan masa/rentang waktu berlakunya kelas les.
	 *
	 * <p>Relasi {@code LAZY}; proxy diresolusi lewat {@code check()}. Ini pengganti
	 * {@code tahunAjaran} milik {@code KelasSiswa}: alih-alih string tahun
	 * akademik, kelas les memakai entity {@link MasaJadwalPelajaran} yang membawa
	 * tanggal mulai/selesai sendiri — cocok untuk kegiatan yang berjalan beberapa
	 * pekan/bulan, bukan satu tahun ajaran penuh.</p>
	 * <p><b>Wajib diisi</b> menurut validasi {@code KelasLesSiswaAction.onSave()}
	 * ("Masa atau rentan waktu pembelajaran harus diisi"). Master masa pembelajaran
	 * yang mengisi combo ini disisipkan sebagai tab di layar yang sama — lihat
	 * catatan pewarisan hak menu pada Javadoc kelas.</p>
	 *
	 * @return masa jadwal pelajaran, atau {@code null} pada baris lama
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "masa_jadwal_pelajaran")
	public MasaJadwalPelajaran getMasaJadwalPelajaran() {
		masaJadwalPelajaran = check(masaJadwalPelajaran);
		return masaJadwalPelajaran;
	}

	/**
	 * Menyetel masa/rentang waktu berlakunya kelas les.
	 *
	 * @param masaJadwalPelajaran masa jadwal pelajaran; boleh {@code null}
	 */
	public void setMasaJadwalPelajaran(MasaJadwalPelajaran masaJadwalPelajaran) {
		this.masaJadwalPelajaran = masaJadwalPelajaran;
	}

	/**
	 * Menerjemahkan CSV {@link #getSyaratPengaturanPembayaran()} menjadi daftar
	 * objek {@link PengaturanBiaya} yang sesungguhnya, terurut.
	 *
	 * <p>Tiap entri CSV berbentuk {@code id} atau {@code id_bulan_tahun}; hanya
	 * bagian sebelum garis bawah pertama yang dipakai sebagai id. Objeknya diambil
	 * dari cache {@code ConstantValues.ambil(...)}, bukan query langsung. Entri
	 * yang tidak ditemukan dilewati; duplikat dibuang; hasil akhirnya diurutkan
	 * dengan {@code Collections.sort} memakai {@code compareTo} milik
	 * {@code PengaturanBiaya}.</p>
	 * <p><b>Konsekuensi periode bulanan:</b> satu {@code PengaturanBiaya} berperiode
	 * "Bulanan" muncul di CSV sebanyak jumlah bulan yang dicakup, tetapi di daftar
	 * hasil method ini hanya muncul <b>sekali</b> — daftar ini memang untuk
	 * ditampilkan ("Syarat Pembayaran" pada kolom Keterangan di grid), bukan untuk
	 * menghitung tunggakan (untuk itu lihat
	 * {@code KelasLesSiswaPunyaSiswa.ambilBelumBayar()}).</p>
	 * <p><b>Efek samping tidak langsung:</b> memanggil
	 * {@link #getSyaratPengaturanPembayaran()} yang bersifat destruktif bila
	 * {@link #getJenisBiayaSekolah()} terisi — lihat peringatan di sana.</p>
	 *
	 * @return daftar pengaturan biaya syarat, terurut dan tanpa duplikat; kosong
	 *         bila tidak ada syarat (tidak pernah {@code null})
	 * @throws NumberFormatException bila entri CSV tidak berupa angka (berbeda dari
	 *                               {@link #ambilPengaturanBiayaId()} yang menoleransinya)
	 */
	public List<PengaturanBiaya> ambilPengaturanBiaya() {

		List<PengaturanBiaya> pengaturanBiayas = new ArrayList<PengaturanBiaya>();

		for (String kode : StringUtils.split(getSyaratPengaturanPembayaran(), ",")) {
			if (!kode.trim().isEmpty()) {

				String[] ss = StringUtils.split(kode, "_");
				String k = ss[0];

				PengaturanBiaya pengaturanBiaya = (PengaturanBiaya) ConstantValues
						.ambil(PengaturanBiaya.class.getName(), Long.parseLong(k));
				if (pengaturanBiaya != null && !pengaturanBiayas.contains(pengaturanBiaya)) {
					pengaturanBiayas.add(pengaturanBiaya);
				}
			}
		}

		Collections.sort(pengaturanBiayas);
		return pengaturanBiayas;
	}

	/**
	 * Varian ringan {@link #ambilPengaturanBiaya()} yang hanya mengembalikan
	 * <b>id</b> pengaturan biaya, tanpa memuat objeknya.
	 *
	 * <p>Dipakai {@code KelasLesSiswaAction.initPengaturanBiaya()} untuk menandai
	 * checkbox mana yang tercentang pada form Ubah. Berbeda dari kerabatnya, entri
	 * yang bukan angka <b>tidak</b> melempar exception melainkan dipetakan ke
	 * {@code -1L} (lewat {@code Common.isNumber}) sehingga tetap masuk daftar
	 * sebagai id yang pasti tidak cocok dengan apa pun.</p>
	 *
	 * @return daftar id pengaturan biaya, terurut menaik dan tanpa duplikat; kosong
	 *         bila tidak ada syarat (tidak pernah {@code null})
	 */
	public List<Long> ambilPengaturanBiayaId() {

		List<Long> pengaturanBiayas = new ArrayList<Long>();

		for (String kode : StringUtils.split(getSyaratPengaturanPembayaran(), ",")) {
			if (!kode.trim().isEmpty()) {

				String[] ss = StringUtils.split(kode, "_");
				String k = ss[0];

				Long id = !Common.isNumber(k.trim()) ? -1L : Long.parseLong(k.trim());
				if (id != null && !pengaturanBiayas.contains(id)) {
					pengaturanBiayas.add(id);
				}
			}
		}

		Collections.sort(pengaturanBiayas);
		return pengaturanBiayas;
	}

	/**
	 * Mengembalikan daftar CSV syarat pembayaran yang harus dilunasi peserta agar
	 * keikutsertaannya di kelas les tetap aktif.
	 *
	 * <p><b>Format:</b> entri dipisah koma, tiap entri berupa id
	 * {@link PengaturanBiaya} — atau {@code id_bulan_tahun} bila jenis biayanya
	 * berperiode "Bulanan" (dibangun {@link #populateJenisPengaturanPembayaran(Set)}).
	 * Penegakannya ada di {@code KelasLesSiswaPunyaSiswa.ada()}/{@code getAktif()}
	 * yang membandingkan CSV ini dengan {@code Siswa.getRiwayatPengaturanPembayaran()}
	 * (dan padanan {@code CalonSiswa}).</p>
	 *
	 * <p><b>PERINGATAN — getter ini DESTRUKTIF.</b> Ada dua jalur:</p>
	 * <ul>
	 *   <li>Bila {@link #getJenisBiayaSekolah()} terisi (ber-id bukan {@code null}),
	 *       field ini <b>dikosongkan</b> menjadi string kosong. Karena properti ini
	 *       terpetakan pada entity {@code dynamicUpdate}, pengosongan itu ikut
	 *       ter-flush ke basis data — <b>konfigurasi syarat pembayaran hilang
	 *       permanen</b> hanya karena baris dibaca, misalnya saat grid dirender
	 *       ({@code ambilPengaturanBiaya()}) atau saat status aktif peserta dihitung
	 *       ({@code KelasLesSiswaPunyaSiswa.getAktif()}). Secara niat ini "kedua
	 *       mekanisme syarat saling meniadakan", tetapi implementasinya menghapus
	 *       data, bukan sekadar mengabaikannya.</li>
	 *   <li>Bila tidak, nilai dinormalisasi: dibungkus koma di kedua ujung lalu
	 *       koma ganda dirapatkan tiga kali berturut-turut, dan hasil yang hanya
	 *       berisi koma dipetakan ke string kosong. Hasil normalisasi ini pun
	 *       ditulis balik ke field. Pembungkusan koma di kedua ujung disengaja:
	 *       {@code KelasLesSiswaPunyaSiswa.ada()} mencari dengan
	 *       {@code contains("," + s + ",")} sehingga id {@code "12"} tidak keliru
	 *       cocok dengan {@code "112"}.</li>
	 * </ul>
	 *
	 * @return CSV syarat pembayaran sudah dinormalisasi; string kosong bila tidak
	 *         ada syarat (tidak pernah {@code null})
	 */
	@Column(columnDefinition = "text")
	public String getSyaratPengaturanPembayaran() {

		if (getJenisBiayaSekolah() != null && getJenisBiayaSekolah().getId() != null) {
			syaratPengaturanPembayaran = "";
		} else {
			syaratPengaturanPembayaran = (syaratPengaturanPembayaran == null
					|| syaratPengaturanPembayaran.trim().equalsIgnoreCase(",") ? ""
							: "," + syaratPengaturanPembayaran.trim() + ",")
					.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

			if (syaratPengaturanPembayaran.equals(",")) {
				syaratPengaturanPembayaran = "";
			} else if (syaratPengaturanPembayaran.equals(",,")) {
				syaratPengaturanPembayaran = "";
			} else if (syaratPengaturanPembayaran.equals(",,,")) {
				syaratPengaturanPembayaran = "";
			}
		}
		return syaratPengaturanPembayaran == null ? "" : syaratPengaturanPembayaran.trim();
	}

	/**
	 * Menyetel daftar CSV syarat pembayaran.
	 *
	 * <p>Dipanggil {@code KelasLesSiswaAction.onSave()} dan setiap kali checkbox
	 * syarat pada form dicentang/dilepas, dengan nilai hasil
	 * {@link #populateJenisPengaturanPembayaran(Set)}. Nilai disimpan apa adanya;
	 * normalisasi terjadi di getter.</p>
	 *
	 * @param syaratPengaturanPembayaran CSV syarat pembayaran; boleh {@code null}
	 */
	public void setSyaratPengaturanPembayaran(String syaratPengaturanPembayaran) {
		this.syaratPengaturanPembayaran = syaratPengaturanPembayaran;
	}

	/**
	 * Membangun CSV {@link #getSyaratPengaturanPembayaran()} dari himpunan id
	 * {@link PengaturanBiaya} yang dipilih pengguna di form.
	 *
	 * <p>Untuk tiap id yang dipilih:</p>
	 * <ul>
	 *   <li>Objeknya diambil dari cache {@code ConstantValues}. Bila tidak ditemukan
	 *       <b>atau</b> sudah tidak aktif, id mentahnya tetap ditulis apa adanya —
	 *       syarat lama tidak hilang hanya karena pengaturan biayanya dinonaktifkan.</li>
	 *   <li>Bila jenis biayanya berperiode <b>"Bulanan"</b>, satu pilihan
	 *       di-<i>expand</i> menjadi SATU ENTRI PER BULAN sepanjang rentang
	 *       {@code bulanMulai}..{@code bulanSampai} (keduanya bilangan
	 *       {@code YYYYMM}, lihat {@code PembayaranSiswa.convert}), masing-masing
	 *       berbentuk {@code id_bulan_tahun}. Inilah sebabnya CSV bisa jauh lebih
	 *       panjang daripada jumlah centang di layar.</li>
	 *   <li>Selain "Bulanan", cukup id-nya saja.</li>
	 * </ul>
	 *
	 * <p><b>Kuirk/risiko yang perlu diketahui:</b> ekspansi bulanan mengurai
	 * {@code bulanMulai}/{@code bulanSampai} lewat
	 * {@code (nilai + "").substring(...)} tanpa cek {@code null}. Pengaturan biaya
	 * berperiode "Bulanan" yang rentang bulannya belum diisi menghasilkan string
	 * {@code "null"} dan melempar {@link NumberFormatException} yang lolos keluar
	 * dari method ini — menggagalkan penyimpanan kelas les. Pemanggilan
	 * {@code biaya.getJenisBiayaSekolah().getPeriode()} juga tidak menjaga
	 * {@code null}.</p>
	 *
	 * @param selectedPengaturanBiaya himpunan id pengaturan biaya yang dicentang;
	 *                                {@code null} menghasilkan string kosong
	 * @return CSV syarat pembayaran siap disimpan; string kosong bila tidak ada
	 *         pilihan (tidak pernah {@code null})
	 */
	public static String populateJenisPengaturanPembayaran(Set<Long> selectedPengaturanBiaya) {
		String jenisS = "";
		if (selectedPengaturanBiaya != null) {
			for (Long pengaturanBiaya : selectedPengaturanBiaya) {

				PengaturanBiaya biaya = (PengaturanBiaya) ConstantValues.ambil(PengaturanBiaya.class.getName(),
						pengaturanBiaya);

				if (biaya != null && biaya.getAktif()) {

					if (biaya.getJenisBiayaSekolah().getPeriode().equals("Bulanan")) {

						int tahunMulai = Integer.parseInt((biaya.getBulanMulai() + "").substring(0, 4));
						int bulanMulai = Integer.parseInt((biaya.getBulanMulai() + "").substring(4));

						int tahunSampai = Integer.parseInt((biaya.getBulanSampai() + "").substring(0, 4));
						int bulanSampai = Integer.parseInt((biaya.getBulanSampai() + "").substring(4));

						int bulanTahunAkhir = PembayaranSiswa.convert(tahunSampai, bulanSampai);

						Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
						cal.set(Calendar.DATE, 1);
						cal.set(Calendar.MONTH, bulanMulai - 1);
						cal.set(Calendar.YEAR, tahunMulai);

						Integer pembayaranTerakhir = 0;
						while (bulanTahunAkhir > pembayaranTerakhir) {
							int tahunCurrent = cal.get(Calendar.YEAR);
							int bulanCurrent = cal.get(Calendar.MONTH);
							int bulanCurrentPlus = bulanCurrent + 1;
							pembayaranTerakhir = PembayaranSiswa.convert(tahunCurrent, bulanCurrentPlus);

							if (biaya.getBulanMulai() != null && pembayaranTerakhir < biaya.getBulanMulai()) {
								cal.add(Calendar.MONTH, 1);
								continue;
							}
							if (biaya.getBulanSampai() != null && pembayaranTerakhir > biaya.getBulanSampai()) {
								break;
							}

							String id = (biaya.getId() + "_" + bulanCurrentPlus + "_" + tahunCurrent);

							jenisS += jenisS.isEmpty() ? id : "," + id;
							cal.add(Calendar.MONTH, 1);
						}

					} else {
						jenisS += jenisS.isEmpty() ? biaya.getId().toString() : "," + biaya.getId();
					}
				} else {
					jenisS += jenisS.isEmpty() ? pengaturanBiaya.toString() : "," + pengaturanBiaya;
				}
			}
		}
		return jenisS;
	}

	/**
	 * Mengembalikan jenis biaya sekolah yang menjadi syarat pembayaran kelas les —
	 * bentuk syarat yang <b>lebih sederhana</b> dan saling meniadakan dengan
	 * {@link #getSyaratPengaturanPembayaran()}.
	 *
	 * <p>Relasi {@code LAZY} opsional; proxy diresolusi lewat {@code check()}.
	 * Label layarnya "Syarat pembayaran", dengan opsi "== Tidak Ada ==". Combo
	 * pilihannya dibatasi jenis biaya milik sekolah terpilih (atau yang global)
	 * dan berstatus aktif.</p>
	 * <p>Bila nilai ini terisi, form menyembunyikan seluruh blok centang
	 * "Syarat Pembayaran Ikut Kelas Les" dan
	 * {@link #getSyaratPengaturanPembayaran()} <b>menghapus</b> isinya —
	 * lihat peringatan di getter tersebut. Penegakannya di sisi roster ada pada
	 * {@code KelasLesSiswaPunyaSiswa.ambilBelumBayar()} yang mendahulukan jenis
	 * biaya ini sebelum jatuh ke daftar CSV.</p>
	 *
	 * @return jenis biaya sekolah syarat, atau {@code null} bila kelas les tidak
	 *         memakai mekanisme ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_biaya_sekolah")
	public JenisBiayaSekolah getJenisBiayaSekolah() {
		jenisBiayaSekolah = check(jenisBiayaSekolah);
		return jenisBiayaSekolah;
	}

	/**
	 * Menyetel jenis biaya sekolah yang menjadi syarat pembayaran kelas les.
	 *
	 * <p><b>Perhatian:</b> mengisi nilai ini akan membuat
	 * {@link #getSyaratPengaturanPembayaran()} mengosongkan daftar CSV syarat pada
	 * pembacaan berikutnya — pastikan itu memang yang diinginkan sebelum
	 * menyimpan.</p>
	 *
	 * @param jenisBiayaSekolah jenis biaya sekolah; {@code null} berarti memakai
	 *                          mekanisme CSV {@link #getSyaratPengaturanPembayaran()}
	 */
	public void setJenisBiayaSekolah(JenisBiayaSekolah jenisBiayaSekolah) {
		this.jenisBiayaSekolah = jenisBiayaSekolah;
	}
	
	
}
