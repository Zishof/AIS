package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.helper.DetailTagihanCalonSiswaHelper;
import ais.action.master.sekolah.helper.DetailTagihanSiswaHelper;
import ais.action.master.sekolah.helper.TagihanUtil;
import ais.action.servlet.Wa;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.WaktuUtil;

/**
 * Konfigurasi <b>paket tagihan</b> sekolah &mdash; simpul TENGAH dan paling menentukan
 * pada rantai keuangan siswa. Satu baris entity ini menjawab tiga pertanyaan sekaligus:
 * <i>biaya jenis apa</i> (lewat {@link ais.database.model.sekolah.JenisBiayaSekolah}),
 * <i>untuk siapa</i> (dimensi sasaran: angkatan, kelas, kelas les, penjurusan, status awal,
 * asrama, gelombang/paket PSB, atau daftar siswa pilihan), dan <i>kapan serta berapa lama</i>
 * (tahun ajaran, rentang bulan, tanggal tagihan per bulan, denda, kedaluwarsa). Tanpa baris
 * di sini, sebuah jenis biaya tidak akan pernah menjadi tagihan bagi siswa mana pun.
 *
 * <h3>Kedudukan pada rantai finansial (TERVERIFIKASI dari kode)</h3>
 * <pre>
 * JenisBiayaSekolah  (kepala: periode Bulanan/Harian/Sekali, gunakanCalonSiswa,
 *         |           gelombangTertentu, paketTertentu, untukTahun/untukBulan)
 *         v
 * PengaturanBiaya    (BERKAS INI: sasaran + jadwal + denda + notifikasi)
 *         |
 *         +--&gt; PengaturanBiayaItemBiaya  (jembatan M:N + tarif bawaan/min/maks/diskon)
 *         |            |
 *         |            v
 *         |    ItemBiayaSekolah          (komponen biaya: SPP, seragam, gedung, ...)
 *         |
 *         +--&gt; NominalBiaya              (tarif TERMATERIALISASI per siswa/periode)
 *                      |
 *                      v
 *                  Tagihan               (kewajiban nyata; kodeUnik = genCode(item, PB, ...))
 *                      |
 *                      v
 *         PembayaranSiswaDetail  --&gt;  PembayaranSiswa
 * </pre>
 * Perhatikan: {@link ais.database.model.sekolah.ItemBiayaSekolah} TIDAK ditunjuk langsung oleh
 * entity ini. Hubungannya selalu melalui {@code PengaturanBiayaItemBiaya}; itulah sebabnya
 * {@link #checkAdaItemBiaya(ItemBiayaSekolah)} harus memakai indeks memori {@code datasItem}
 * alih-alih relasi Hibernate. {@link ais.database.model.sekolah.NominalBiaya} menunjuk BALIK ke
 * entity ini ({@code nominalBiaya.pengaturanBiaya}) sekaligus ke {@code PengaturanBiayaItemBiaya},
 * sehingga tarif yang sudah dibekukan tetap dapat ditelusuri ke paket asalnya walau paketnya
 * kemudian diubah.
 *
 * <h3>Layar dan titik masuk</h3>
 * <ul>
 *   <li>ZK: {@code /pages/master/sekolah/pengaturan_biaya_sekolah.zul} &rarr;
 *       {@code ais.action.master.sekolah.PengaturanBiayaAction}. Label menunya <b>bukan</b>
 *       "Pengaturan Biaya": {@code MenuInitializer} mendaftarkannya sebagai
 *       <b>"Tagihan Pembayaran"</b> (id menu {@code 6518796}, induk grup {@code 5705}
 *       "Keuangan Sekolah"/"Keuangan Siswa"), sementara {@code MenuSnapshotData} menuliskannya
 *       <b>"Tagihan Siswa"</b>. Dua label berbeda untuk satu layar yang sama &mdash; sumber
 *       kebingungan yang perlu diingat saat menelusuri laporan pengguna.</li>
 *   <li>New UI: {@code WEB-INF/new/sekolah/services/pengaturan_biaya_service.jsp}
 *       (modul {@code sekolah}, halaman {@code pengaturan_biaya}) mendaftarkan entity ini pada
 *       {@code nuiServiceEntities} sehingga ia ikut ter-auto-register di Generic CRUD v2.</li>
 *   <li>API/klien: {@code /Api} rute {@code tagihan_siswa} ({@code TagihanSiswa}),
 *       {@code psb_calon_profil} ({@code PsbCalonApi}), {@code NewUiPemOnlineController},
 *       {@code CommonReportHelper}, serta layar kasir {@code PembayaranOnline}/
 *       {@code RekapPembayaran}.</li>
 * </ul>
 *
 * <h3>Dua arah kueri yang WAJIB dibedakan</h3>
 * <ol>
 *   <li><b>Siswa &rarr; paket biaya</b>: {@link #terapkanFilterPembayaran(Criteria, Siswa, CalonSiswa)}.
 *       Dipakai semua layar/pihak yang bertanya "tagihan apa saja milik siswa ini". Cakupan
 *       tenant di jalur ini bersandar pada {@code jenisBiayaSekolah.sekolah}.</li>
 *   <li><b>Paket biaya &rarr; siswa</b>: {@code DetailTagihanSiswaHelper.initCriteria(...)} /
 *       {@code DetailTagihanCalonSiswaHelper}. Dipakai layar rincian dan mesin notifikasi.
 *       Cakupan tenant di jalur ini bersandar pada {@code pengaturanBiaya.sekolah}.</li>
 * </ol>
 * <b>Catatan penting:</b> kedua arah memakai KOLOM TENANT YANG BERBEDA. Bila kolom
 * {@code sekolah} pada baris ini tidak konsisten dengan {@code jenisBiayaSekolah.sekolah}
 * (keduanya dipilih terpisah di layar simpan), satu arah bisa menemukan baris yang tidak
 * ditemukan arah lain &mdash; gejalanya "tagihan muncul di aplikasi siswa tetapi daftar
 * siswanya kosong di layar staf", atau sebaliknya.
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Pembangun kriteria (static)</b>: {@link #terapkanFilterPembayaran(Criteria, Siswa, CalonSiswa)}
 *       sebagai fasad, ditopang {@code buatCriteriaKelasDanTahunAngkatan}, {@code buatCriteriaAsrama},
 *       {@code buatCriteriaPenjurusan}, {@code buatCriteriaStatusAwalSiswa},
 *       {@code buatCriteriaSekolahDanJenisSiswa}, {@code kriteriaAktifAtauNull},
 *       {@code ambilCriterionGelombang}.</li>
 *   <li><b>Cache tagihan (static)</b>: {@link #reloadTagihan(PengaturanBiaya)},
 *       {@link #reloadTagihan(PengaturanBiaya, boolean)}, {@link #invalidasiCacheTagihan(PengaturanBiaya)},
 *       {@link #reloadSemuaTagihan(boolean)}.</li>
 *   <li><b>Indeks item biaya (static)</b>: {@link #reInit()} + {@link #checkAdaItemBiaya(ItemBiayaSekolah)}.</li>
 *   <li><b>Timer latar</b>: {@link #chekNotifikasi()} dan {@link #checkKelasLes()} &mdash; keduanya
 *       dipanggil dari {@code UserOnlineCounter} (timer aplikasi), BUKAN dari layar.</li>
 *   <li><b>Mesin notifikasi tagihan</b>: {@link #kirimTemplate(Integer, Integer)},
 *       {@link #kirimTemplate(Siswa, CalonSiswa, Integer, Integer)},
 *       {@code bangunTemplateDefaultDaftarUlang}, {@link #refreshTemplate(List)}.</li>
 *   <li><b>Jadwal tanggal tagihan</b>: {@link #ambilTahun()}, {@code hitungTanggal(Integer)},
 *       dan 12 pasang {@code getTanggalTagihanBulanN()}/{@code setTanggalTagihanBulanN(Date)}.</li>
 *   <li><b>Getter/setter properti terpetakan</b> &mdash; banyak di antaranya BUKAN getter polos
 *       (lihat bagian berikut).</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang mudah menjebak</h3>
 * <ol>
 *   <li><b>Getter yang MENULIS BALIK ke field (destruktif).</b> Sebagian besar getter di sini
 *       menormalkan atau <i>mengosongkan</i> field-nya sendiri sebelum mengembalikan nilai:
 *       {@code getYayasan()} menimpa yayasan dari {@code sekolah.getYayasan()};
 *       {@code getTahunAngkatan()} memaksa 0; {@code getBulanMulai()}/{@code getBulanSampai()}
 *       memaksa {@code null}; {@code getGelombangPendaftaranPsb()} dan {@code getPaketPsb()}
 *       memaksa {@code null}; {@code getKelasSiswa()} memaksa {@code null};
 *       {@code getKhususBuatSiswaTertentu()} memaksa {@code false}; {@code getAsramaSiswa()}/
 *       {@code getTanpaAsrama()} saling meniadakan; {@code getWaktuNotifikasi()} memaksa
 *       {@code null}; ke-12 {@code getTanggalTagihanBulanN()} MENGHITUNG dan menyimpan tanggal
 *       atau menghapusnya; serta empat getter teks berkoma
 *       ({@code getKelasBanyak()}, {@code getBulanYangTidakAdaTagihannya()},
 *       {@code getBulanYangTidakAdaDendanya()}, {@code getWajibDibayarSebelumnya()}) yang
 *       menulis ulang field dalam bentuk ternormalkan {@code ,a,b,}.
 *       Karena pemetaan Hibernate memakai AKSES PROPERTI (anotasi ada di getter) dan kelas ini
 *       {@code dynamicUpdate=true} + {@link org.hibernate.envers.Audited}, mutasi tersebut
 *       <b>ikut ter-flush ke basis data</b> begitu entity dibaca di dalam sesi yang masih
 *       hidup &mdash; lengkap dengan satu revisi Envers baru. Membaca konfigurasi biaya dapat
 *       MENGUBAH konfigurasi biaya. Jalur yang aman dari efek ini adalah yang memasang
 *       {@code setReadOnly(true)} dan {@code FlushMode.MANUAL} seperti {@link #chekNotifikasi()};
 *       jalur layar/API biasa TIDAK memasangnya.</li>
 *   <li><b>{@code toString()} bukan sekadar penampil.</b> Ia menugaskan ulang empat field relasi
 *       dari getter destruktif di atas, jadi sekadar mencetak log atau mengisi kolom
 *       {@code jenis_tagihan} pada laporan pun berpotensi memicu penulisan.</li>
 *   <li><b>Mengirim notifikasi MEMBUAT tagihan.</b>
 *       {@link #kirimTemplate(Siswa, CalonSiswa, Integer, Integer)} memanggil
 *       {@code Tagihan.ambilAtauBuat(...)} yang menyimpan baris {@code Tagihan} baru. Jadi
 *       penjadwal notifikasi adalah salah satu penulis tagihan, bukan hanya pembaca.</li>
 *   <li><b>Cache in-memory dua lapis.</b> {@code MemoryDbUtil.getAllTagihan()} (kunci
 *       {@code Tagihan.genCode(...)}) dan {@code MemoryDbUtil.getAllTagihanSudah()} (penanda
 *       "paket ini sudah dimuat"). Keduanya {@code ConcurrentHashMap} sehingga aman dihapus
 *       sambil diiterasi. Mengubah paket biaya TANPA memanggil
 *       {@link #invalidasiCacheTagihan(PengaturanBiaya)} akan membuat pembaca berikutnya
 *       memakai objek tagihan basi.</li>
 *   <li><b>{@code khususBuatSiswaTertentu} adalah saklar mode.</b> Bila {@code true}, filter
 *       kelas/angkatan/status awal DILEWATI dan daftar sasaran diambil dari tabel jembatan
 *       {@code PengaturanBiayaPunyaSiswa}. Pada
 *       {@code DetailTagihanSiswaHelper.initCriteriaDenganNama} mode ini juga <b>membuang
 *       penyaring {@code sekolah}</b> sepenuhnya &mdash; keamanannya bergantung 100&#37; pada
 *       daftar {@code PengaturanBiayaPunyaSiswa} tetap berisi siswa sekolah yang benar.</li>
 *   <li><b>{@code kelasBanyak} mencocokkan NAMA kelas, bukan id.</b> Sub-kueri pemilih siswa
 *       untuk mode ini tidak menyaring sekolah sama sekali; ia bergantung pada penyaring
 *       {@code sekolah} di kueri luar untuk membatasi hasil. Aman selama
 *       {@code khususBuatSiswaTertentu} bernilai {@code false}, tetapi selalu menghasilkan
 *       daftar id antara yang jauh lebih besar dari perlunya.</li>
 *   <li><b>{@code bulanMulai}/{@code bulanSampai} bukan 1&ndash;12.</b> Keduanya bilangan
 *       {@code YYYYMM} (mis. {@code 202407}); {@link #ambilTahun()} melakukan iterasi bilangan
 *       bulat di antaranya dan membuang nilai yang bagian bulannya di luar 1&ndash;12.</li>
 *   <li><b>{@code wajibDibayarSebelumnya}</b> menyimpan daftar id {@code Tagihan} prasyarat
 *       dalam format {@code ,id,id,} dan dibaca dengan {@code contains(","+id+",")} oleh
 *       {@code PembayaranOnline} &mdash; format berkoma itu bagian dari kontrak, jangan
 *       "dirapikan".</li>
 *   <li><b>{@code kunci} adalah kunci EDIT, bukan hak akses.</b> Bila terisi (lihat
 *       {@code PengaturanBiayaAction.tampilkanKunci}), seluruh tagihan turunan paket ini
 *       berubah menjadi hanya-baca di layar kasir/rincian.</li>
 * </ol>
 *
 * <h3>Cakupan tenant &amp; hak akses (hasil verifikasi silang)</h3>
 * <ul>
 *   <li>Entity ini memiliki properti terpetakan {@code sekolah} dan {@code yayasan}, keduanya
 *       termasuk dalam daftar putih {@code GenericCrudAutoEntityAdapter.scopeBindings()}.
 *       Artinya celah kebocoran lintas tenant Generic CRUD v2 ({@code task_7b6038ac})
 *       <b>TIDAK</b> berlaku di sini &mdash; verifikasi negatif.</li>
 *   <li>Layar ZK-nya memanggil {@code Common.doCheckSecurity()} pada {@code doBeforeCompose}
 *       dan menggerbangi tombol tambah/ubah/hapus lewat {@code CommonPrivilages}. Namun
 *       seluruh panel detail yang ditanam di layar ini (item biaya, tagihan per siswa,
 *       pemilih siswa "khusus") mewarisi hak dari SATU menu induk "Tagihan Pembayaran";
 *       tidak ada gerbang terpisah per panel.</li>
 *   <li>Semua endpoint yang membaca entity ini lewat {@code /Api} menyelesaikan identitas
 *       siswa dari TOKEN sesi ({@code ApiUtil.currentUser}), bukan dari parameter klien, jadi
 *       pola IDOR parameter-URL ({@code task_9f4af0bf}) tidak muncul pada jalur ini.
 *       Jalur yang benar-benar anonim menyentuh entity ini hanya secara TIDAK LANGSUNG,
 *       yaitu H2H bank ({@code /MncBank} &rarr; {@code VirtualAccountBank.bayarSiswa} &rarr;
 *       {@code tagihan.getPengaturanBiaya().getJenisBiayaSekolah()}); di sana entity ini
 *       hanya dibaca sebagai rujukan, tetapi kelemahan endpoint itu sendiri sudah tercatat
 *       terpisah.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.JenisBiayaSekolah
 * @see ais.database.model.sekolah.PengaturanBiayaItemBiaya
 * @see ais.database.model.sekolah.ItemBiayaSekolah
 * @see ais.database.model.sekolah.NominalBiaya
 * @see ais.database.model.sekolah.Tagihan
 * @see ais.database.model.sekolah.PengaturanBiayaPunyaSiswa
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "pengaturan_biaya", schema = "sekolah")
public class PengaturanBiaya extends GeneralValueObject {

	/**
	 * Jarak minimum (10 menit) antar catatan galat "pool koneksi penuh" pada mesin
	 * notifikasi, agar log tidak dibanjiri pesan yang sama tiap siklus timer.
	 */
	private static final long JEDA_LOG_KONEKSI_NOTIFIKASI = 10L * 60L * 1000L;
	/**
	 * Stempel waktu ({@code System.currentTimeMillis()}) catatan galat koneksi notifikasi
	 * terakhir; dibandingkan dengan {@code JEDA_LOG_KONEKSI_NOTIFIKASI}. Sengaja tidak
	 * {@code volatile}/tersinkronisasi karena hanya dipakai untuk pembatasan laju log.
	 */
	private static long terakhirLogKoneksiNotifikasi = 0L;

	/**
	 * FASAD PUBLIK arah "siswa &rarr; paket biaya": menempelkan SELURUH penyaring kelayakan
	 * tagihan ke {@code criteria} milik pemanggil, lalu mengembalikan {@code criteria} yang sama
	 * (gaya berantai). Inilah satu-satunya tempat aturan "paket biaya mana yang berlaku bagi
	 * seorang siswa" dirumuskan; semua layar, laporan, dan API pembayaran memanggilnya agar
	 * jawabannya seragam.
	 *
	 * <p>Penyaring yang ditempelkan, berurutan:</p>
	 * <ol>
	 *   <li>pada tabel {@code pengaturan_biaya} sendiri: kombinasi kelas/kelas les + tahun
	 *       angkatan ({@code buatCriteriaKelasDanTahunAngkatan}), asrama
	 *       ({@code buatCriteriaAsrama}), penjurusan ({@code buatCriteriaPenjurusan}), status awal
	 *       siswa ({@code buatCriteriaStatusAwalSiswa}), dan {@code aktif} bernilai
	 *       {@code true}/{@code null};</li>
	 *   <li>alias {@code jenisBiayaSekolah} dibuat (INNER JOIN &mdash; baris tanpa jenis biaya
	 *       otomatis tersaring habis);</li>
	 *   <li>pada {@link ais.database.model.sekolah.JenisBiayaSekolah}: penyaring gelombang PSB
	 *       ({@code ambilCriterionGelombang}), pasangan sekolah + jenis peserta didik
	 *       ({@code buatCriteriaSekolahDanJenisSiswa}), dan {@code aktif}.</li>
	 * </ol>
	 *
	 * <p><b>Cakupan tenant.</b> Batas antar sekolah dipasang di langkah (3) melalui
	 * {@code jenisBiayaSekolah.sekolah}, BUKAN melalui kolom {@code sekolah} milik entity ini.
	 * Bila kedua kolom itu tidak konsisten, hasil method ini akan berbeda dari hasil
	 * {@code DetailTagihanSiswaHelper.initCriteria} yang memakai kolom {@code sekolah} entity ini.</p>
	 *
	 * <p><b>Kasus tepi.</b> Bila {@code s_lokal} dan {@code cs_lokal} sama-sama {@code null},
	 * baris pertama melempar {@code NullPointerException} &mdash; pemanggil WAJIB mengirim
	 * setidaknya satu di antaranya. Bila {@code s_lokal} terisi, daftar kelas les calon siswa
	 * tetap ikut digabung sehingga siswa yang naik dari calon siswa tidak kehilangan tagihan
	 * lesnya. Method ini TIDAK mengeksekusi kueri dan TIDAK memasang {@code addOrder}/
	 * {@code setMaxResults}; itu tanggung jawab pemanggil.</p>
	 *
	 * @param criteria kriteria atas {@code PengaturanBiaya.class} milik pemanggil (wajib)
	 * @param s_lokal  siswa aktif yang sedang ditinjau, atau {@code null} bila calon siswa
	 * @param cs_lokal calon siswa yang sedang ditinjau, atau {@code null} bila siswa aktif
	 * @return objek {@code criteria} yang sama, sudah dilengkapi seluruh penyaring
	 * @see ais.database.model.sekolah.JenisBiayaSekolah
	 */
	public static Criteria terapkanFilterPembayaran(Criteria criteria, Siswa s_lokal, CalonSiswa cs_lokal) {
		StatusAwalSiswa sas = s_lokal != null ? s_lokal.getStatusAwalSiswa() : cs_lokal.getStatusAwalSiswa();
		PenjurusanSekolah ps = s_lokal != null ? s_lokal.getPenjurusanSekolah() : cs_lokal.getPenjurusanSekolah();
		List<Long> asramas = s_lokal != null ? s_lokal.ambilasrama() : new ArrayList<Long>();

		// 1. Filter tabel utama (PengaturanBiaya)
		criteria.add(buatCriteriaKelasDanTahunAngkatan(s_lokal, cs_lokal)).add(buatCriteriaAsrama(asramas))
				.add(buatCriteriaPenjurusan(ps)).add(buatCriteriaStatusAwalSiswa(sas))
				.add(kriteriaAktifAtauNull("aktif"));

		// 2. Alias untuk tabel relasi
		criteria.createAlias("jenisBiayaSekolah", "jenisBiayaSekolah");

		// 3. Filter tabel relasi (JenisBiayaSekolah)
		criteria.add(ambilCriterionGelombang(cs_lokal, s_lokal))
				.add(buatCriteriaSekolahDanJenisSiswa(s_lokal, cs_lokal))
				.add(kriteriaAktifAtauNull("jenisBiayaSekolah.aktif"));

		return criteria;
	}

	// ===================================================================================
	// PRIVATE HELPERS: Modul-modul logika spesifik yang hanya digunakan secara
	// internal
	// ===================================================================================

	/**
	 * Menyusun penyaring gabungan <b>kelas/kelas les</b> dan <b>tahun angkatan</b>.
	 *
	 * <p>Bentuk akhirnya: {@code khususBuatSiswaTertentu = true} <b>ATAU</b> (kondisi kelas DAN
	 * kondisi angkatan). Cabang pertama itulah yang membuat paket "khusus siswa tertentu" lolos
	 * tanpa peduli kelas maupun angkatan &mdash; sasarannya nanti dibatasi tabel jembatan
	 * {@code PengaturanBiayaPunyaSiswa}, bukan di sini.</p>
	 *
	 * <p>Kondisi kelas sendiri adalah OR antara jalur kelas les dan jalur kelas reguler. Jalur
	 * kelas les menuntut {@code kelasLesSiswa IS NULL} bila peserta didik tidak mengikuti les
	 * apa pun; jalur reguler mengizinkan {@code kelasSiswa IS NULL} (paket berlaku untuk semua
	 * kelas) atau kelas yang memang diikuti. Kondisi angkatan meloloskan
	 * {@code tahunAngkatan = 0} (berarti "semua angkatan") atau angkatan yang sama persis.</p>
	 *
	 * @param s_lokal  siswa aktif, atau {@code null}
	 * @param cs_lokal calon siswa, atau {@code null}
	 * @return kriteria kelas + angkatan yang siap ditempel ke kueri utama
	 */
	private static Criterion buatCriteriaKelasDanTahunAngkatan(Siswa s_lokal, CalonSiswa cs_lokal) {
		Integer angkatan = s_lokal != null ? s_lokal.getTahunMasuk() : cs_lokal.getTahunMasuk();
		List<Long> kelases = s_lokal != null ? s_lokal.ambilkelas() : new ArrayList<Long>();
		List<Long> kelasLes = cs_lokal != null ? cs_lokal.ambilKelasLesSiswaId() : new ArrayList<Long>();

		if (s_lokal != null) {
			kelasLes.addAll(s_lokal.ambilkelasLes());
		}

		Criterion filterAngkatan = Restrictions.or(Restrictions.eq("tahunAngkatan", 0),
				Restrictions.eq("tahunAngkatan", angkatan));

		Criterion kriteriaKelasLes = kelasLes.isEmpty() ? Restrictions.isNull("kelasLesSiswa")
				: Restrictions.in("kelasLesSiswa.id", kelasLes);

		Criterion kriteriaKelasReguler = kelases.isEmpty() ? Restrictions.isNull("kelasSiswa")
				: Restrictions.or(Restrictions.isNull("kelasSiswa"), Restrictions.in("kelasSiswa.id", kelases));

		Criterion kondisiKelasDasar = Restrictions.or(kriteriaKelasLes, kriteriaKelasReguler);

		return Restrictions.or(Restrictions.eq("khususBuatSiswaTertentu", true),
				Restrictions.and(kondisiKelasDasar, filterAngkatan));
	}

	/**
	 * Menyusun penyaring <b>asrama</b>: paket biaya lolos bila (a) ia paket umum &mdash;
	 * {@code tanpaAsrama = false} DAN {@code asramaSiswa IS NULL}; atau (b) ia cocok dengan
	 * status asrama peserta didik.
	 *
	 * <p>Untuk peserta didik yang TIDAK berasrama ({@code asramas} kosong), cabang (b) menuntut
	 * {@code tanpaAsrama} bernilai {@code true} atau {@code null} &mdash; sehingga paket yang
	 * memang ditujukan bagi non-asrama tetap terambil. Untuk yang berasrama, cabang (b) mencocokkan
	 * {@code asramaSiswa.id} dengan salah satu asrama yang diikuti.</p>
	 *
	 * @param asramas daftar id {@code AsramaSiswa} yang diikuti peserta didik; boleh kosong/{@code null}
	 * @return kriteria asrama
	 */
	private static Criterion buatCriteriaAsrama(List<Long> asramas) {
		Criterion kondisiAsramaUmum = Restrictions.and(Restrictions.eq("tanpaAsrama", false),
				Restrictions.isNull("asramaSiswa"));

		Criterion kondisiAsramaSpesifik = (asramas == null || asramas.isEmpty())
				? Restrictions.or(Restrictions.isNull("tanpaAsrama"), Restrictions.eq("tanpaAsrama", true))
				: Restrictions.in("asramaSiswa.id", asramas);

		return Restrictions.or(kondisiAsramaUmum, kondisiAsramaSpesifik);
	}

	/**
	 * Menyusun penyaring <b>penjurusan</b>. Bila peserta didik belum berjurusan
	 * ({@code ps == null}), penyaring dilumpuhkan dengan {@code 1=1} &mdash; artinya paket yang
	 * mensyaratkan jurusan tertentu pun ikut lolos. Bila berjurusan, paket lolos ketika
	 * jurusannya sama atau paket tidak menetapkan jurusan sama sekali.
	 *
	 * @param ps penjurusan peserta didik, boleh {@code null}
	 * @return kriteria penjurusan
	 */
	private static Criterion buatCriteriaPenjurusan(PenjurusanSekolah ps) {
		if (ps == null) {
			return Restrictions.sqlRestriction("1=1");
		}
		return Restrictions.or(Restrictions.eq("penjurusanSekolah", ps), Restrictions.isNull("penjurusanSekolah"));
	}

	/**
	 * Menyusun penyaring <b>status awal siswa</b> (baru / pindahan / lanjutan, dst.).
	 * Paket lolos bila ia berjenis "khusus siswa tertentu", atau tidak menetapkan status awal,
	 * atau status awalnya sama persis dengan milik peserta didik.
	 *
	 * @param sas status awal peserta didik, boleh {@code null}
	 * @return kriteria status awal
	 */
	private static Criterion buatCriteriaStatusAwalSiswa(StatusAwalSiswa sas) {
		return Restrictions.or(Restrictions.eq("khususBuatSiswaTertentu", true),
				Restrictions.or(Restrictions.isNull("statusAwalSiswa"), Restrictions.eq("statusAwalSiswa", sas)));
	}

	/**
	 * Menyusun penyaring <b>sekolah + jenis peserta didik</b> pada alias
	 * {@code jenisBiayaSekolah}. Inilah satu-satunya batas antar tenant pada jalur
	 * {@link #terapkanFilterPembayaran(Criteria, Siswa, CalonSiswa)}.
	 *
	 * <p>Untuk siswa aktif dituntut {@code gunakanCalonSiswa = false} dan
	 * {@code jenisBiayaSekolah.sekolah} sama dengan sekolah siswa; untuk calon siswa dituntut
	 * {@code gunakanCalonSiswa = true} dan sekolah calon siswa. Bila KEDUA argumen {@code null},
	 * method mengembalikan konjungsi KOSONG &mdash; secara SQL berarti tidak ada penyaring tenant
	 * sama sekali. Pemanggil yang sah selalu mengirim salah satu, dan fasadnya sudah lebih dulu
	 * melempar {@code NullPointerException} pada kasus itu, sehingga kondisi fail-open ini tidak
	 * terjangkau dari jalur yang ada; ia tetap dicatat di sini sebagai peringatan bagi pemanggil baru.</p>
	 *
	 * @param s_lokal  siswa aktif, atau {@code null}
	 * @param cs_lokal calon siswa, atau {@code null}
	 * @return konjungsi penyaring sekolah + jenis peserta didik
	 */
	private static Criterion buatCriteriaSekolahDanJenisSiswa(Siswa s_lokal, CalonSiswa cs_lokal) {
		Conjunction conjunction = Restrictions.conjunction();

		if (s_lokal != null) {
			conjunction.add(Restrictions.eq("jenisBiayaSekolah.gunakanCalonSiswa", false));
			conjunction.add(Restrictions.eq("jenisBiayaSekolah.sekolah", s_lokal.getSekolah()));
		} else if (cs_lokal != null) {
			conjunction.add(Restrictions.eq("jenisBiayaSekolah.gunakanCalonSiswa", true));
			conjunction.add(Restrictions.eq("jenisBiayaSekolah.sekolah", cs_lokal.getSekolah()));
		}

		return conjunction;
	}

	/**
	 * Pintasan untuk pola "dianggap aktif bila kolomnya {@code true} ATAU belum diisi".
	 * Dipakai baik untuk kolom {@code aktif} milik entity ini maupun untuk
	 * {@code jenisBiayaSekolah.aktif}.
	 *
	 * @param namaProperti nama properti Hibernate, boleh berawalan alias (mis. {@code "jenisBiayaSekolah.aktif"})
	 * @return kriteria {@code (prop IS NULL OR prop = true)}
	 */
	private static Criterion kriteriaAktifAtauNull(String namaProperti) {
		return Restrictions.or(Restrictions.isNull(namaProperti), Restrictions.eq(namaProperti, true));
	}

	/**
	 * Menyusun penyaring <b>gelombang pendaftaran PSB</b> &mdash; bagian paling berliku dari
	 * {@link #terapkanFilterPembayaran(Criteria, Siswa, CalonSiswa)} karena harus meniru aturan
	 * bisnis PSB: biaya apa yang boleh ditagihkan pada calon siswa bergantung pada seberapa jauh
	 * ia melangkah (baru mendaftar &rarr; terverifikasi &rarr; diterima).
	 *
	 * <p>Hasil akhirnya {@code criterion2 OR criterion1}:</p>
	 * <ul>
	 *   <li><b>criterion1</b> &mdash; penyaring berbasis status calon siswa. Bila bukan calon
	 *       siswa atau calon siswa tanpa gelombang, penyaring dilumpuhkan ({@code 1=1}). Bila
	 *       gelombangnya menyalakan {@code sesuaiKelasSaatDiterima} (untuk yang sudah diterima)
	 *       atau {@code sesuaiKelas} (untuk yang belum), penyaring juga dilumpuhkan &mdash; artinya
	 *       biaya ditentukan kelas, bukan gelombang. Selain itu, dikumpulkan daftar id
	 *       {@link ais.database.model.sekolah.JenisBiayaSekolah} yang boleh muncul: gelombang
	 *       dasar untuk yang belum diterima/belum verifikasi, ditambah varian "terverifikasi",
	 *       ditambah varian "lulus" untuk yang sudah diterima. Daftar kosong menjadi {@code 1=0}
	 *       alias tidak ada yang lolos.</li>
	 *   <li><b>criterion2</b> &mdash; penyaring gelombang "global": paket yang MENGIKAT dirinya ke
	 *       gelombang tertentu hanya boleh muncul bila gelombang itu tidak menetapkan jenis biaya
	 *       sendiri (baik {@code jenisBiayaSekolah} maupun {@code jenisBiayaSekolahLulus}). Bila
	 *       peserta didik tidak punya gelombang sama sekali, cabang ini menjadi {@code 1=0}.</li>
	 * </ul>
	 *
	 * <p><b>Kasus tepi.</b> Karena kedua cabang di-OR, cukup salah satu bernilai {@code 1=1} agar
	 * seluruh penyaring gelombang lumpuh. Nilai boolean gelombang dibaca secara defensif dengan
	 * {@code Boolean.TRUE.equals(...)} sehingga kolom {@code null} diperlakukan sebagai
	 * {@code false}, bukan melempar {@code NullPointerException}.</p>
	 *
	 * @param calonSiswa calon siswa yang ditinjau, boleh {@code null}
	 * @param siswa      siswa aktif yang ditinjau, boleh {@code null}
	 * @return kriteria gelombang PSB gabungan
	 */
	private static Criterion ambilCriterionGelombang(CalonSiswa calonSiswa, Siswa siswa) {
		// --- Bagian 1: Logika Criterion 1 (Fokus ke Filter Calon Siswa) ---
		Criterion criterion1;

		if (calonSiswa == null || calonSiswa.getGelombangPendaftaranPsb() == null) {
			// Bypass filter jika bukan calon siswa
			criterion1 = Restrictions.sqlRestriction("1=1"); // Pengganti "true" yang lebih standar di SQL
		} else {
			GelombangPendaftaranPsb gelombangCs = calonSiswa.getGelombangPendaftaranPsb();

			// Ekstrak boolean secara defensif (menghindari NullPointerException)
			boolean diterima = Boolean.TRUE.equals(calonSiswa.getTelahDiterima());
			boolean terverifikasi = Boolean.TRUE.equals(calonSiswa.getTerverifikasi());
			boolean sesuaiKelasDiterima = Boolean.TRUE.equals(gelombangCs.getSesuaiKelasSaatDiterima());
			boolean sesuaiKelas = Boolean.TRUE.equals(gelombangCs.getSesuaiKelas());

			// Cek kondisi mutlak yang otomatis meloloskan filter
			if ((diterima && sesuaiKelasDiterima) || (!diterima && sesuaiKelas)) {
				criterion1 = Restrictions.sqlRestriction("1=1");
			} else {
				// Kumpulkan daftar ID Jenis Biaya Sekolah yang diizinkan berdasarkan status
				Set<Long> validJbsIds = new HashSet<Long>();

				// Jika statusnya Terverifikasi
				if (terverifikasi) {
					if (gelombangCs.getJenisBiayaSekolah() != null)
						validJbsIds.add(gelombangCs.getJenisBiayaSekolah().getId());
					if (gelombangCs.getJenisBiayaSekolahTerverifikasi() != null)
						validJbsIds.add(gelombangCs.getJenisBiayaSekolahTerverifikasi().getId());
				}

				// Jika statusnya Telah Diterima
				if (diterima) {
					if (gelombangCs.getJenisBiayaSekolah() != null)
						validJbsIds.add(gelombangCs.getJenisBiayaSekolah().getId());
					if (gelombangCs.getJenisBiayaSekolahTerverifikasi() != null)
						validJbsIds.add(gelombangCs.getJenisBiayaSekolahTerverifikasi().getId());
					if (gelombangCs.getJenisBiayaSekolahLulus() != null)
						validJbsIds.add(gelombangCs.getJenisBiayaSekolahLulus().getId());
				}

				// Jika belum diterima dan belum diverifikasi
				if (!diterima && !terverifikasi) {
					if (gelombangCs.getJenisBiayaSekolah() != null)
						validJbsIds.add(gelombangCs.getJenisBiayaSekolah().getId());
				}

				// Buat criteria berdasarkan list ID yang terkumpul
				if (validJbsIds.isEmpty()) {
					criterion1 = Restrictions.sqlRestriction("1=0"); // "false" / tidak ada data yang lolos
				} else {
					criterion1 = Restrictions.in("jenisBiayaSekolah.id", validJbsIds);
				}
			}
		}

		// --- Bagian 2: Logika Criterion 2 (Filter Gelombang Global) ---
		Criterion criterion2;
		GelombangPendaftaranPsb gelombangGlobal = calonSiswa != null ? calonSiswa.getGelombangPendaftaranPsb()
				: (siswa != null ? siswa.getGelombangPendaftaranPsb() : null);

		if (gelombangGlobal == null) {
			criterion2 = Restrictions.sqlRestriction("1=0");
		} else if (gelombangGlobal.getJenisBiayaSekolah() == null
				&& gelombangGlobal.getJenisBiayaSekolahLulus() == null) {
			criterion2 = Restrictions.eq("gelombangPendaftaranPsb", gelombangGlobal);
		} else {
			criterion2 = Restrictions.sqlRestriction("1=0");
		}

		// --- Hasil Akhir ---
		// Berlakukan OR antara Criterion 2 dan Criterion 1 sesuai dengan kode lama Anda
		return Restrictions.or(criterion2, criterion1);
	}

	/**
	 * Varian ringkas {@link #reloadTagihan(PengaturanBiaya, boolean)} dengan
	 * {@code reload = false}: cache hanya diisi bila paket ini <i>belum pernah</i> dimuat.
	 * Aman dipanggil berkali-kali; setelah pemanggilan pertama biayanya nyaris nol.
	 *
	 * @param pengaturanBiaya paket biaya yang tagihannya hendak dimuat ke cache memori
	 */
	public static void reloadTagihan(PengaturanBiaya pengaturanBiaya) {
		reloadTagihan(pengaturanBiaya, false);
	}

	/**
	 * Membuang SELURUH entri cache {@code Tagihan} milik satu paket biaya, lalu menghapus
	 * penanda "paket ini sudah dimuat".
	 *
	 * <p>Wajib dipanggil sebelum sinkronisasi/pemuatan ulang: bila tidak, objek {@code Tagihan}
	 * lama yang sudah berubah nominalnya (atau sudah dihapus di basis data) akan tetap dipakai
	 * kembali oleh {@code Tagihan.ambilAtauBuat()} sehingga siswa melihat angka basi.</p>
	 *
	 * <p><b>Cara kerja.</b> Cache tagihan berkunci {@code Tagihan.genCode(...)} yang TIDAK memuat
	 * id paket biaya, jadi tidak ada cara mencari entri milik satu paket selain menelusuri seluruh
	 * peta dan membandingkan {@code tagihan.getPengaturanBiaya().getId()}. Penghapusan dilakukan
	 * sambil iterasi &mdash; aman karena peta yang dikembalikan {@code MemoryDbUtil} adalah
	 * {@code ConcurrentHashMap}. Setiap kegagalan per entri (mis. proxy Hibernate yatim) ditelan
	 * dan dicatat ke {@code ErrorAuditUtil} agar satu entri rusak tidak menggagalkan pembersihan
	 * entri lain.</p>
	 *
	 * <p><b>Kasus tepi.</b> Paket {@code null} atau belum ber-id diabaikan diam-diam.
	 * Efek samping: penanda {@code getAllTagihanSudah()} ikut dihapus, sehingga pembaca berikutnya
	 * akan memicu pemuatan ulang dari basis data.</p>
	 *
	 * @param pengaturanBiaya paket biaya yang cache-nya dibuang; {@code null} diabaikan
	 */
	public static void invalidasiCacheTagihan(PengaturanBiaya pengaturanBiaya) {
		if (pengaturanBiaya == null || pengaturanBiaya.getId() == null) {
			return;
		}
		Long idPengaturan = pengaturanBiaya.getId();
		Map<String, Tagihan> cache = MemoryDbUtil.getAllTagihan();
		if (cache != null) {
			for (Map.Entry<String, Tagihan> entry : cache.entrySet()) {
				try {
					Tagihan tagihan = entry.getValue();
					PengaturanBiaya pbCache = tagihan == null ? null : tagihan.getPengaturanBiaya();
					if (pbCache != null && idPengaturan.equals(pbCache.getId())) {
						cache.remove(entry.getKey());
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit PengaturanBiaya.invalidasiCacheTagihan id=" + idPengaturan);
				}
			}
		}
		MemoryDbUtil.getAllTagihanSudah().remove(idPengaturan);
	}

	/**
	 * Memuat (atau memuat ULANG) seluruh {@code Tagihan} milik satu paket biaya ke cache
	 * memori {@code MemoryDbUtil.getAllTagihan()}, berkunci {@code Tagihan.genCode(...)}.
	 *
	 * <p><b>Alur.</b> Dipakai pola <i>double-checked locking</i>: pengecekan cepat di luar kunci,
	 * lalu kunci per-id memakai {@code String.valueOf(id).intern()}, lalu pengecekan ulang di
	 * dalam kunci. Bila {@code reload} bernilai {@code true},
	 * {@link #invalidasiCacheTagihan(PengaturanBiaya)} dijalankan lebih dahulu supaya objek lama
	 * benar-benar terbuang sebelum digantikan. Kueri dijalankan pada sesi Hibernate TERPISAH
	 * ({@code openSession()}) yang selalu ditutup di {@code finally}, sehingga tidak mengganggu
	 * sesi request pemanggil.</p>
	 *
	 * <p><b>Efek samping tambahan.</b> Untuk tiap tagihan yang punya
	 * {@code PembayaranSiswaDetail} tetapi tautan baliknya kosong, tautan itu diperbaiki
	 * ({@code detail.setTagihan(tagihan)}) &mdash; perbaikan data di tengah operasi yang
	 * namanya terdengar seperti "sekadar memuat".</p>
	 *
	 * <p><b>Kasus tepi.</b> Hanya tagihan ber-{@code kodeUnik} yang dimuat; tagihan lama tanpa
	 * kode unik tidak pernah masuk cache. Kunci {@code intern()} berlaku untuk seluruh JVM dan
	 * bisa berbenturan dengan kode lain yang mengunci literal string angka yang sama &mdash;
	 * risiko yang diterima demi kesederhanaan. Seluruh galat ditelan dan dicatat, jadi kegagalan
	 * pemuatan tampil sebagai "tagihan tidak muncul", bukan sebagai error di layar.</p>
	 *
	 * @param pengaturanBiaya paket biaya yang tagihannya dimuat
	 * @param reload          {@code true} memaksa buang-dan-muat-ulang, {@code false} hanya
	 *                        memuat bila belum pernah dimuat
	 */
	@SuppressWarnings("unchecked")
	public static void reloadTagihan(PengaturanBiaya pengaturanBiaya, boolean reload) {

		// 1. Cek pertama: Eksekusi JIKA minta 'reload' ATAU data belum ada
		if (reload || !MemoryDbUtil.getAllTagihanSudah().containsKey(pengaturanBiaya.getId())) {

			// Gunakan sinkronisasi per ID agar aman dari bentrok multi-threading
			synchronized (String.valueOf(pengaturanBiaya.getId()).intern()) {

				// 2. Cek kedua (Double-Checked Locking) dengan logika yang sama
				if (reload || !MemoryDbUtil.getAllTagihanSudah().containsKey(pengaturanBiaya.getId())) {
					if (reload) {
						invalidasiCacheTagihan(pengaturanBiaya);
					}

					Session session = null;
					try {
						System.out.println("--> reloadTagihan " + pengaturanBiaya + " (Force Reload: " + reload + ")");

						// Buka Isolated Session
						session = HibernateUtil.getSessionFactory().openSession();

						List<Tagihan> tagihans = session.createCriteria(Tagihan.class)
								.add(Restrictions.isNotNull("kodeUnik"))
								.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)).list();

						for (Tagihan tagihan : tagihans) {
							String kodeUnik = Tagihan.genCode(tagihan.getItemBiayaSekolah(), pengaturanBiaya,
									tagihan.getTahunbulan(), tagihan.getSiswa(), tagihan.getCalonSiswa(),
									tagihan.getBayarKe());

							if (tagihan != null && tagihan.ambilPembayaranSiswaDetail() != null
									&& tagihan.ambilPembayaranSiswaDetail().ambilTagihan() == null) {
								tagihan.ambilPembayaranSiswaDetail().setTagihan(tagihan);
							}

							// Saat reload=true, .put() di sini akan secara otomatis menimpa (overwrite)
							// objek Tagihan lama dengan objek Tagihan terbaru hasil query DB
							MemoryDbUtil.getAllTagihan().put(kodeUnik, tagihan);
						}

						MemoryDbUtil.getAllTagihanSudah().put(pengaturanBiaya.getId(), true);

						tagihans.clear();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/PengaturanBiaya.java:280");
					} finally {
						// Tutup 'session' lokal
						if (session != null) {
							try {
								session.close();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:286");
							}
						}
					}
				}
			}
		}
	}

	// 1. Gunakan volatile agar pergantian data (swap) langsung terbaca oleh semua
	// Thread secara real-time
	/**
	 * Indeks memori "paket biaya &rarr; daftar id {@link ais.database.model.sekolah.ItemBiayaSekolah}",
	 * sumber jawaban {@link #checkAdaItemBiaya(ItemBiayaSekolah)}. Diisi sekaligus oleh
	 * {@link #reInit()} dengan teknik <i>atomic swap</i>: peta baru dibangun terpisah lalu
	 * referensinya ditukar, sehingga pembaca tidak pernah melihat peta setengah terisi.
	 * {@code volatile} agar hasil penukaran itu langsung terlihat semua thread.
	 */
	private static volatile Map<Long, List<Long>> datasItem = null;

	// 2. Tambahkan synchronized agar jika ada 50 user minta reInit bersamaan,
	// hanya 1 yang query ke DB, sisanya cukup antre milidetik
	/**
	 * Membangun ULANG indeks {@code datasItem} dari SELURUH baris
	 * {@code PengaturanBiayaItemBiaya} di basis data.
	 *
	 * <p>Ditandai {@code synchronized} pada level kelas agar lonjakan permintaan bersamaan hanya
	 * menghasilkan SATU kueri; sisanya menunggu sebentar lalu langsung memakai hasilnya. Peta
	 * disusun di penampung lokal dan baru ditukar ke {@code datasItem} setelah terisi penuh
	 * &mdash; sengaja TIDAK memakai {@code datasItem.clear()} yang akan membuat pembaca lain
	 * melihat indeks kosong di tengah proses.</p>
	 *
	 * <p><b>Kasus tepi.</b> Kueri TIDAK menyaring sekolah/yayasan &mdash; memang disengaja, karena
	 * indeks ini berkunci id paket biaya sehingga sudah terpisah dengan sendirinya per paket.
	 * Sesi Hibernate dibuka sendiri dan ditutup di {@code finally}. Bila kueri gagal, galatnya
	 * dicatat dan {@code datasItem} dibiarkan pada nilai LAMA (atau {@code null} bila belum
	 * pernah berhasil) &mdash; artinya kegagalan menghasilkan "item biaya tidak tercentang",
	 * bukan error.</p>
	 *
	 * <p>Dipanggil setelah penyimpanan di {@code PengaturanBiayaAction.onSave()} (lewat
	 * {@code Common.createDefaultTimer}) dan secara malas oleh
	 * {@link #checkAdaItemBiaya(ItemBiayaSekolah)}.</p>
	 */
	@SuppressWarnings("unchecked")
	public static synchronized void reInit() {

		Session session = null;
		try {
			// Buka Isolated Session
			session = HibernateUtil.getSessionFactory().openSession();

			// 3. ATOMIC SWAP: Buat penampung LOKAL baru.
			// Jangan pernah lakukan datasItem.clear() saat aplikasi sedang live!
			Map<Long, List<Long>> newDatasItem = new HashMap<Long, List<Long>>();

			List<PengaturanBiayaItemBiaya> pengaturanBiayaItemBiayas = ConstantValues.simpleList(
					session.createCriteria(PengaturanBiayaItemBiaya.class).addOrder(Order.asc("pengaturanBiaya")),
					PengaturanBiayaItemBiaya.class);

			for (PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya : pengaturanBiayaItemBiayas) {
				Long idPengaturan = pengaturanBiayaItemBiaya.getPengaturanBiaya().getId();
				List<Long> longs = newDatasItem.get(idPengaturan);

				if (longs == null) {
					longs = new ArrayList<Long>();
					newDatasItem.put(idPengaturan, longs);
				}
				longs.add(pengaturanBiayaItemBiaya.getItemBiayaSekolah().getId());
			}

			// 4. Setelah data terisi 100% penuh, baru kita TUKAR referensinya.
			// Ini menjamin user lain tidak akan pernah membaca Map yang setengah kosong.
			datasItem = newDatasItem;

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/PengaturanBiaya.java:333");
		} finally {
			// 5. Cukup tutup session lokal yang dibuka dari openSession()
			if (session != null) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:339");
				}
			}
		}
	}

	/**
	 * Menjawab "apakah item biaya ini termasuk dalam paket biaya ini?" tanpa menyentuh
	 * basis data, dengan membaca indeks memori {@code datasItem}.
	 *
	 * <p>Referensi peta disalin dulu ke variabel lokal supaya penukaran atomik oleh
	 * {@link #reInit()} di tengah eksekusi tidak menimbulkan hasil campur aduk. Bila indeks masih
	 * kosong, {@link #reInit()} dipanggil sekali lalu peta dibaca ulang.</p>
	 *
	 * <p><b>Kasus tepi.</b> Mengembalikan {@code false} bila indeks tetap kosong setelah
	 * {@code reInit()} (mis. basis data sedang tidak dapat dihubungi) &mdash; jadi kegagalan
	 * infrastruktur tampil sebagai "item tidak termasuk paket", bukan sebagai error. Indeks
	 * bersifat sekali-bangun; item yang baru saja ditambahkan lewat jalur lain belum terlihat
	 * sampai {@link #reInit()} dijalankan lagi. Argumen {@code null} akan melempar
	 * {@code NullPointerException} pada {@code itemBiayaSekolah.getId()}.</p>
	 *
	 * @param itemBiayaSekolah komponen biaya yang ditanyakan
	 * @return {@code true} bila item tersebut terdaftar pada paket ini
	 */
	public boolean checkAdaItemBiaya(ItemBiayaSekolah itemBiayaSekolah) {
		// Mengambil referensi map yang sedang aktif ke dalam variabel lokal agar aman
		// dari perubahan tiba-tiba
		Map<Long, List<Long>> currentMap = datasItem;

		if (currentMap == null || currentMap.isEmpty()) {
			reInit();
			currentMap = datasItem; // Ambil map terbaru setelah reInit selesai
		}

		if (currentMap != null) {
			List<Long> longs = currentMap.get(this.getId());
			return longs != null && longs.contains(itemBiayaSekolah.getId());
		}

		return false;
	}

	/**
	 * Label ringkas paket biaya untuk kombobox, log, dan kolom {@code jenis_tagihan} pada
	 * laporan: {@code "<kode JBS> - <nama JBS> - <kelas les> - <kelas> - <penjurusan> - <tahun ajaran>"},
	 * dengan bagian yang kosong dilewati.
	 *
	 * <p><b>PERINGATAN &mdash; bukan method bebas efek samping.</b> Empat field relasi
	 * ({@code jenisBiayaSekolah}, {@code kelasLesSiswa}, {@code kelasSiswa},
	 * {@code penjurusanSekolah}) ditugaskan ulang dari getternya masing-masing, dan getter-getter
	 * itu sendiri dapat MENGOSONGKAN field (lihat {@link #getKelasSiswa()}). Karena kelas ini
	 * dipetakan lewat properti dan diaudit Envers, memanggil {@code toString()} pada entity yang
	 * masih terikat sesi Hibernate berpotensi menghasilkan pembaruan baris + revisi audit saat
	 * flush. Untuk sekadar mencatat log, gunakan {@code getId()}.</p>
	 *
	 * <p>Juga memicu pemuatan LAZY untuk keempat relasi tersebut &mdash; berbahaya di luar sesi
	 * (melempar {@code LazyInitializationException}) dan mahal bila dipanggil di dalam perulangan
	 * daftar.</p>
	 *
	 * @return label paket biaya yang dapat dibaca manusia
	 */
	public String toString() {
		jenisBiayaSekolah = getJenisBiayaSekolah();
		kelasLesSiswa = getKelasLesSiswa();
		kelasSiswa = getKelasSiswa();
		penjurusanSekolah = getPenjurusanSekolah();
		return

		(jenisBiayaSekolah == null ? "" : jenisBiayaSekolah.getKode() + " - " + jenisBiayaSekolah.getNama() + " - ")
				+ (kelasLesSiswa == null ? "" : kelasLesSiswa.getNama() + " - ")
				+ (kelasSiswa == null ? "" : kelasSiswa.getNama() + " - ")
				+ (penjurusanSekolah == null ? "" : penjurusanSekolah.getNama() + " - ") + getTahunAjaran();
	}

	/**
	 * Versi serialisasi. Entity ini dipindahkan antar-desktop ZK dan disimpan pada cache
	 * memori, jadi nilainya tidak boleh diubah tanpa alasan.
	 */
	private static final long serialVersionUID = 2395396546778630651L;
	/**
	 * Kunci utama {@code sekolah.pengaturan_biaya.id} (IDENTITY).
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini; diisi lewat {@link #setOleh(String)} yang menolak nilai kosong.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini; pasangan dari {@code oleh}.
	 */
	private String olehId;

	/**
	 * @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna pengubah. <b>Penulisan diabaikan</b> bila {@code olehId} kosong
	 * atau hanya spasi &mdash; nilai lama sengaja dipertahankan agar jejak audit tidak terhapus
	 * oleh proses latar yang tidak punya konteks pengguna.
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
	 * Menyimpan nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai kosong
	 * diabaikan agar tidak menghapus jejak yang sudah ada.
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
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: mendelegasikan pencatatan waktu/pelaku perubahan ke
	 * {@code AuditTimestampInterceptor.ubah(this)} sesaat sebelum baris diperbarui.
	 * Tidak untuk dipanggil langsung.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir; diinisialisasi ke waktu server saat objek dibuat dan
	 * disegarkan oleh {@code AuditTimestampInterceptor}. Penamaan bergaris bawah dipertahankan
	 * karena sudah menjadi nama properti Hibernate dan dipakai berkas pemetaan lain.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir (kolom {@code TIMESTAMP})
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Kepala rantai biaya: {@link ais.database.model.sekolah.JenisBiayaSekolah} yang menentukan
	 * periode (Bulanan/Harian/Sekali), apakah untuk calon siswa, apakah terikat gelombang/paket
	 * PSB, serta tahun/bulan acuan. WAJIB terisi ({@code nullable = false}).
	 */
	private JenisBiayaSekolah jenisBiayaSekolah;
	/**
	 * Sekolah pemilik paket biaya ini ({@code nullable = false}); dasar cakupan tenant pada jalur "paket &rarr; siswa" dan pada Generic CRUD v2.
	 */
	private Sekolah sekolah;
	/**
	 * Jurusan sasaran; {@code null} berarti paket berlaku untuk semua jurusan.
	 */
	private PenjurusanSekolah penjurusanSekolah;
	/**
	 * Yayasan pemilik; selalu diselaraskan ulang dari {@code sekolah.getYayasan()} oleh {@link #getYayasan()}.
	 */
	private Yayasan yayasan;
	/**
	 * Angkatan/tahun masuk sasaran; nilai {@code 0} berarti "semua angkatan".
	 */
	private Integer tahunAngkatan;
	/**
	 * Rombongan belajar reguler sasaran; {@code null} berarti semua kelas. Dipaksa {@code null} bila paket menyasar kelas les.
	 */
	private KelasSiswa kelasSiswa;
	/**
	 * Kelas les/kursus sasaran; bila terisi, paket ini adalah paket biaya les dan mengabaikan kelas reguler.
	 */
	private KelasLesSiswa kelasLesSiswa;
	/**
	 * Bila {@code true} (bawaan), nominal per item diambil dari tarif bawaan {@code PengaturanBiayaItemBiaya}, bukan dari tarif per siswa.
	 */
	private Boolean gunakanBiayaDefault;
	/**
	 * Tahun ajaran berlakunya paket (format {@code "2024/2025"}); dipakai juga untuk mencocokkan {@code kelasSiswa.tahunAjaran} pada mode {@code kelasBanyak}.
	 */
	private String tahunAjaran;
	/**
	 * Asrama sasaran; {@code null} berarti paket tidak terikat asrama.
	 */
	private AsramaSiswa asramaSiswa;
	/**
	 * Bila {@code true} (bawaan), tanggal tagihan mengikuti {@code tanggalTagihan} alih-alih tanggal per bulan.
	 */
	private Boolean tanggalTagihanMengikutiDefault;
	/**
	 * Bila {@code true} (bawaan), tanggal jatuh tempo mengikuti bulan berjalan sehingga ke-12 kolom {@code tanggalTagihanBulanN} dikosongkan.
	 */
	private Boolean tanggalTagihanMengikutiBulanBerjalan;
	/**
	 * Menyalakan pengecualian bulan tanpa tagihan; daftar bulannya ada di {@code bulanYangTidakAdaTagihannya}.
	 */
	private Boolean terdapatBulanYangTidakAdaTagihannya;
	/**
	 * Daftar bulan tanpa tagihan dalam format berkoma ternormalkan {@code ,a,b,}.
	 */
	private String bulanYangTidakAdaTagihannya;
	/**
	 * Daftar bulan yang dibebaskan dari denda, format berkoma ternormalkan {@code ,a,b,}.
	 */
	private String bulanYangTidakAdaDendanya;
	/**
	 * Daftar NAMA kelas sasaran (bukan id) dalam format berkoma ternormalkan; alternatif dari {@code kelasSiswa} untuk menyasar banyak kelas sekaligus.
	 */
	private String kelasBanyak;
	/**
	 * Tanggal tagihan bawaan; {@link #getTanggalTagihan()} mengembalikan tanggal hari ini bila belum diisi.
	 */
	private Date tanggalTagihan;
	/**
	 * Awal rentang penagihan dalam bentuk {@code YYYYMM} (mis. {@code 202407}) &mdash; BUKAN nomor bulan 1&ndash;12.
	 */
	private Integer bulanMulai;
	/**
	 * Akhir rentang penagihan dalam bentuk {@code YYYYMM}; berpasangan dengan {@code bulanMulai}.
	 */
	private Integer bulanSampai;

	/**
	 * Menyalakan perhitungan denda keterlambatan untuk seluruh tagihan turunan paket ini.
	 */
	private Boolean terdapatDenda;
	/**
	 * Tanggal dalam bulan (bawaan {@code 10}) sebagai batas sebelum denda mulai dihitung.
	 */
	private Integer tanggalDeadlineDenda;
	/**
	 * Batas akhir tetap pembayaran; dipakai bila tenggat tidak mengikuti pola per bulan.
	 */
	private Date deadlineTagihan;
	/**
	 * Bila {@code true}, nilai {@code denda} diperlakukan sebagai PERSEN dari nominal, bukan rupiah tetap.
	 */
	private Boolean dendaMengunakanPersen;
	/**
	 * Saklar mode "khusus siswa tertentu": sasaran ditentukan tabel jembatan {@code PengaturanBiayaPunyaSiswa}, dan penyaring kelas/angkatan/status awal dilewati.
	 */
	private Boolean khususBuatSiswaTertentu;
	/**
	 * Besaran denda &mdash; rupiah tetap, atau persen bila {@code dendaMengunakanPersen} bernilai {@code true}.
	 */
	private Double denda;

	/**
	 * Tanggal setelah mana tagihan paket ini dianggap kedaluwarsa; ikut dikirim ke klien mobile sebagai {@code tagihanKadaluarsa}.
	 */
	private Date tagihanKadaluarsa;

	/**
	 * Tanggal jatuh tempo khusus bulan ke-1; hanya berlaku bila
	 * {@code tanggalTagihanMengikutiBulanBerjalan} bernilai {@code false}, dan dihitung otomatis
	 * oleh getternya bila masih kosong.
	 */
	private Date tanggalTagihanBulan1;
	/**
	 * Tanggal jatuh tempo khusus bulan ke-2; hanya berlaku bila
	 * {@code tanggalTagihanMengikutiBulanBerjalan} bernilai {@code false}, dan dihitung otomatis
	 * oleh getternya bila masih kosong.
	 */
	private Date tanggalTagihanBulan2;
	/**
	 * Tanggal jatuh tempo khusus bulan ke-3; hanya berlaku bila
	 * {@code tanggalTagihanMengikutiBulanBerjalan} bernilai {@code false}, dan dihitung otomatis
	 * oleh getternya bila masih kosong.
	 */
	private Date tanggalTagihanBulan3;
	/**
	 * Tanggal jatuh tempo khusus bulan ke-4; hanya berlaku bila
	 * {@code tanggalTagihanMengikutiBulanBerjalan} bernilai {@code false}, dan dihitung otomatis
	 * oleh getternya bila masih kosong.
	 */
	private Date tanggalTagihanBulan4;
	/**
	 * Tanggal jatuh tempo khusus bulan ke-5; hanya berlaku bila
	 * {@code tanggalTagihanMengikutiBulanBerjalan} bernilai {@code false}, dan dihitung otomatis
	 * oleh getternya bila masih kosong.
	 */
	private Date tanggalTagihanBulan5;
	/**
	 * Tanggal jatuh tempo khusus bulan ke-6; hanya berlaku bila
	 * {@code tanggalTagihanMengikutiBulanBerjalan} bernilai {@code false}, dan dihitung otomatis
	 * oleh getternya bila masih kosong.
	 */
	private Date tanggalTagihanBulan6;
	/**
	 * Tanggal jatuh tempo khusus bulan ke-7; hanya berlaku bila
	 * {@code tanggalTagihanMengikutiBulanBerjalan} bernilai {@code false}, dan dihitung otomatis
	 * oleh getternya bila masih kosong.
	 */
	private Date tanggalTagihanBulan7;
	/**
	 * Tanggal jatuh tempo khusus bulan ke-8; hanya berlaku bila
	 * {@code tanggalTagihanMengikutiBulanBerjalan} bernilai {@code false}, dan dihitung otomatis
	 * oleh getternya bila masih kosong.
	 */
	private Date tanggalTagihanBulan8;
	/**
	 * Tanggal jatuh tempo khusus bulan ke-9; hanya berlaku bila
	 * {@code tanggalTagihanMengikutiBulanBerjalan} bernilai {@code false}, dan dihitung otomatis
	 * oleh getternya bila masih kosong.
	 */
	private Date tanggalTagihanBulan9;
	/**
	 * Tanggal jatuh tempo khusus bulan ke-10; hanya berlaku bila
	 * {@code tanggalTagihanMengikutiBulanBerjalan} bernilai {@code false}, dan dihitung otomatis
	 * oleh getternya bila masih kosong.
	 */
	private Date tanggalTagihanBulan10;
	/**
	 * Tanggal jatuh tempo khusus bulan ke-11; hanya berlaku bila
	 * {@code tanggalTagihanMengikutiBulanBerjalan} bernilai {@code false}, dan dihitung otomatis
	 * oleh getternya bila masih kosong.
	 */
	private Date tanggalTagihanBulan11;
	/**
	 * Tanggal jatuh tempo khusus bulan ke-12; hanya berlaku bila
	 * {@code tanggalTagihanMengikutiBulanBerjalan} bernilai {@code false}, dan dihitung otomatis
	 * oleh getternya bila masih kosong.
	 */
	private Date tanggalTagihanBulan12;

	/**
	 * Status awal peserta didik sasaran (baru/pindahan/lanjutan); {@code null} berarti semua status.
	 */
	private StatusAwalSiswa statusAwalSiswa;
	/**
	 * Penanda aktif; {@code null} diperlakukan sebagai {@code true} sehingga baris lama tanpa nilai tetap berlaku.
	 */
	private Boolean aktif;
	/**
	 * Gelombang PSB sasaran; dipaksa {@code null} oleh getternya bila jenis biaya tidak menyalakan {@code gelombangTertentu}.
	 */
	private GelombangPendaftaranPsb gelombangPendaftaranPsb;
	/**
	 * Paket PSB sasaran; dipaksa {@code null} oleh getternya bila jenis biaya tidak menyalakan {@code paketTertentu}.
	 */
	private PaketPsb paketPsb;

	/**
	 * Menyalakan pengiriman notifikasi tagihan otomatis oleh timer {@link #chekNotifikasi()}.
	 */
	private Boolean aktifkanNotifikasi;
	/**
	 * Jadwal pengiriman notifikasi; hanya jam &amp; menitnya yang dicocokkan untuk periode Bulanan, sedangkan periode lain juga mencocokkan tanggal penuh.
	 */
	private Date waktuNotifikasi;
	/**
	 * Batas akhir pembayaran yang dicetak pada notifikasi; bulan/tahunnya ditimpa periode berjalan saat pesan dirakit.
	 */
	private Date batasWaktuPembayaran;
	/**
	 * Naskah notifikasi dengan tempat isian {@code [Nama Siswa]}, {@code [Nama Sekolah]}, {@code [BULAN]}, {@code [TAHUN]}, {@code [TOTAL]}, {@code [Tanggal]}, dan {@code [<kode item biaya>]}.
	 */
	private String templateNotifikasi;
	/**
	 * Menampilkan seluruh kelas pada layar rincian tagihan, bukan hanya kelas sasaran paket.
	 */
	private Boolean tampilanSemuaKelas;
	/**
	 * Menyasar peserta didik NON-asrama; saling meniadakan dengan {@code asramaSiswa}.
	 */
	private Boolean tanpaAsrama;
	/**
	 * Daftar id {@code Tagihan} prasyarat dalam format {@code ,id,id,}; dibaca {@code PembayaranOnline} untuk memaksa urutan pelunasan.
	 */
	private String wajibDibayarSebelumnya;

	/**
	 * Mode berlangganan: tagihan berikutnya terbit otomatis setelah {@code jumlahHariPenagihanBerikutnya} hari.
	 */
	private Boolean otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion;
	/**
	 * Jarak hari antar penerbitan tagihan pada mode berlangganan (bawaan {@code 30}).
	 */
	private Integer jumlahHariPenagihanBerikutnya;
	/**
	 * Pengguna yang MENGUNCI paket ini. Bila terisi, seluruh tagihan turunannya menjadi
	 * hanya-baca di layar kasir dan layar rincian &mdash; ini kunci EDIT, bukan mekanisme hak akses.
	 */
	private Tbmuser kunci;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Seluruh nilai bawaan tidak
	 * disetel di sini melainkan pada getternya masing-masing, sehingga baris lama yang kolomnya
	 * {@code null} tetap berperilaku seperti baris baru.
	 */
	public PengaturanBiaya() {
	}

	/**
	 * @return kunci utama baris ini, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id kunci utama; diisi Hibernate setelah penyimpanan
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Sekolah pemilik paket biaya. Nilai dilewatkan {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} sehingga proxy yatim/terhapus dikembalikan
	 * sebagai {@code null} alih-alih melempar galat.
	 *
	 * @return sekolah pemilik, atau {@code null} bila rujukannya sudah tidak valid
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id", nullable = false)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyimpan sekolah pemilik. Objek tanpa id diperlakukan sebagai {@code null} agar
	 * entity setengah jadi dari kombobox tidak ikut tersimpan sebagai relasi rusak.
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id menghasilkan {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Yayasan pemilik paket biaya.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Nilainya selalu DISELARASKAN ULANG dari {@code getSekolah().getYayasan()} bila sekolah
	 * terisi, jadi kolom {@code yayasan_id} di basis data tidak pernah bisa berbeda dari yayasan
	 * sekolahnya untuk waktu lama. Efeknya: memindahkan sebuah sekolah ke yayasan lain akan
	 * mengubah kolom ini pada seluruh paket biaya sekolah tersebut, diam-diam, saat baris itu
	 * pertama kali dibaca.</p>
	 *
	 * @return yayasan pemilik, atau {@code null}
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
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id menghasilkan {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Angkatan sasaran paket; {@code 0} berarti "semua angkatan".
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Nilai dipaksa menjadi {@code 0} bila paket sudah menyasar kelas tertentu
	 * ({@code kelasSiswa}), berjenis "khusus siswa tertentu", atau memakai daftar
	 * {@code kelasBanyak} &mdash; sebab pada ketiga mode itu angkatan tidak lagi relevan.
	 * <b>Dampak finansial:</b> {@code tahunAngkatan = 0} diperlakukan
	 * {@link #terapkanFilterPembayaran(Criteria, Siswa, CalonSiswa)} sebagai "berlaku untuk semua
	 * angkatan", sehingga penulisan balik ini MEMPERLUAS cakupan penagihan. Perluasan itu
	 * disengaja (dibatasi ulang oleh penyaring kelas), tetapi terjadi tanpa aksi pengguna.</p>
	 *
	 * @return angkatan sasaran; {@code 0} bila belum diisi atau bila dinetralkan oleh mode lain
	 */
	@Column(name = "tahun_angkatan", nullable = false)
	public Integer getTahunAngkatan() {
		if (getKelasSiswa() != null || getKhususBuatSiswaTertentu() || !getKelasBanyak().trim().isEmpty()) {
			tahunAngkatan = 0;
		}
		return this.tahunAngkatan == null ? 0 : tahunAngkatan;
	}

	/**
	 * @param tahunAngkatan angkatan sasaran, atau {@code 0} untuk semua angkatan
	 */
	public void setTahunAngkatan(Integer tahunAngkatan) {
		this.tahunAngkatan = tahunAngkatan;
	}

	/**
	 * Kepala rantai biaya paket ini.
	 *
	 * @return {@link ais.database.model.sekolah.JenisBiayaSekolah} pemilik, atau {@code null}
	 *         bila rujukannya sudah tidak valid ({@code check(...)})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_biaya_sekolah_id", nullable = false)
	public JenisBiayaSekolah getJenisBiayaSekolah() {
		jenisBiayaSekolah = check(jenisBiayaSekolah);
		return jenisBiayaSekolah;
	}

	/**
	 * @param jenisBiayaSekolah jenis biaya induk; menentukan periode, sasaran calon/siswa,
	 *        serta apakah gelombang/paket PSB relevan
	 */
	public void setJenisBiayaSekolah(JenisBiayaSekolah jenisBiayaSekolah) {
		this.jenisBiayaSekolah = jenisBiayaSekolah;
	}

	/**
	 * @return {@code true} (bawaan) bila nominal diambil dari tarif bawaan
	 *         {@code PengaturanBiayaItemBiaya}; {@code false} bila memakai tarif per siswa
	 */
	public Boolean getGunakanBiayaDefault() {
		return gunakanBiayaDefault == null ? true : gunakanBiayaDefault;
	}

	/**
	 * @param gunakanBiayaDefault {@code null} diperlakukan sebagai {@code true} oleh getternya
	 */
	public void setGunakanBiayaDefault(Boolean gunakanBiayaDefault) {
		this.gunakanBiayaDefault = gunakanBiayaDefault;
	}

	/**
	 * Tahun ajaran berlakunya paket.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Bila paket terikat gelombang PSB, nilainya ditimpa dari
	 * {@code gelombangPendaftaranPsb.getTahunAjaran()}. Bila kolomnya kosong, yang DIKEMBALIKAN
	 * adalah tahun akademik berjalan ({@code Common.getCurrentTahunAkademik()}) &mdash; namun
	 * nilai balikan itu TIDAK ditulis ke field, jadi baris lama tanpa tahun ajaran akan
	 * "berpindah" tahun mengikuti kalender setiap kali dibaca. Nilai ini juga dipakai mencocokkan
	 * {@code kelasSiswa.tahunAjaran} pada mode {@code kelasBanyak}, sehingga pergeseran tahun
	 * dapat membuat daftar siswa sasaran mendadak kosong.</p>
	 *
	 * @return tahun ajaran paket, atau tahun akademik berjalan bila belum diisi
	 */
	public String getTahunAjaran() {

		if (getGelombangPendaftaranPsb() != null) {
			tahunAjaran = getGelombangPendaftaranPsb().getTahunAjaran();
		}

		return tahunAjaran == null ? Common.getCurrentTahunAkademik() : tahunAjaran;
	}

	/**
	 * @param tahunAjaran tahun ajaran dalam format {@code "2024/2025"}
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

//	public Boolean getBolehMemilihRincianPembayaran() {
//		return bolehMemilihRincianPembayaran == null ? true : bolehMemilihRincianPembayaran;
//	}
//
//	public void setBolehMemilihRincianPembayaran(Boolean bolehMemilihRincianPembayaran) {
//		this.bolehMemilihRincianPembayaran = bolehMemilihRincianPembayaran;
//	}

	/**
	 * Awal rentang penagihan dalam bentuk {@code YYYYMM}.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Dipaksa {@code null} bila periode jenis biaya BUKAN "Bulanan" maupun "Harian" &mdash;
	 * rentang bulan tidak bermakna untuk biaya sekali bayar. Akibat lanjutannya:
	 * {@link #ambilTahun()} menjadi kosong dan {@code hitungTanggal(...)} jatuh ke tanggal tagihan
	 * bawaan. Mengubah periode jenis biaya dari "Bulanan" ke "Sekali" karena itu MENGHAPUS rentang
	 * bulan paket-paket turunannya secara permanen pada pembacaan berikutnya.</p>
	 *
	 * @return bulan awal {@code YYYYMM}, atau {@code null}
	 */
	public Integer getBulanMulai() {
		if (getJenisBiayaSekolah() != null && !getJenisBiayaSekolah().getPeriode().equalsIgnoreCase("Bulanan")
				&& !getJenisBiayaSekolah().getPeriode().equalsIgnoreCase("Harian")) {
			bulanMulai = null;
		}
		return bulanMulai;
	}

	/**
	 * @param bulanMulai awal rentang penagihan, format {@code YYYYMM} (mis. {@code 202407})
	 */
	public void setBulanMulai(Integer bulanMulai) {
		this.bulanMulai = bulanMulai;
	}

	/**
	 * Akhir rentang penagihan dalam bentuk {@code YYYYMM}. Perilakunya identik dengan
	 * {@link #getBulanMulai()}, termasuk penulisan balik {@code null} untuk periode selain
	 * "Bulanan"/"Harian".
	 *
	 * @return bulan akhir {@code YYYYMM}, atau {@code null}
	 */
	public Integer getBulanSampai() {
		if (getJenisBiayaSekolah() != null && !getJenisBiayaSekolah().getPeriode().equalsIgnoreCase("Bulanan")
				&& !getJenisBiayaSekolah().getPeriode().equalsIgnoreCase("Harian")) {
			bulanSampai = null;
		}
		return bulanSampai;
	}

	/**
	 * @param bulanSampai akhir rentang penagihan, format {@code YYYYMM}
	 */
	public void setBulanSampai(Integer bulanSampai) {
		this.bulanSampai = bulanSampai;
	}

	/**
	 * Tanggal tagihan bawaan paket.
	 *
	 * <p><b>Kasus tepi.</b> Bila kolomnya kosong, yang dikembalikan adalah TANGGAL HARI INI
	 * ({@code WaktuUtil.getDate()}) &mdash; bukan {@code null}. Karena tanggal ini menjadi acuan
	 * tenggat dan perhitungan denda, paket tanpa tanggal tagihan efektif "selalu jatuh tempo hari
	 * ini" dan hasilnya berubah setiap hari. Nilai tersebut tidak ditulis balik ke field.</p>
	 *
	 * @return tanggal tagihan, atau tanggal hari ini bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihan() {
		return tanggalTagihan == null ? WaktuUtil.getDate() : tanggalTagihan;
	}

	/**
	 * @param tanggalTagihan tanggal tagihan bawaan
	 */
	public void setTanggalTagihan(Date tanggalTagihan) {
		this.tanggalTagihan = tanggalTagihan;
	}

	/**
	 * @return tanggal dalam bulan sebagai batas sebelum denda dihitung; {@code 10} bila belum diisi
	 */
	public Integer getTanggalDeadlineDenda() {
		return tanggalDeadlineDenda == null ? 10 : tanggalDeadlineDenda;
	}

	/**
	 * @param tanggalDeadlineDenda tanggal dalam bulan (1&ndash;31)
	 */
	public void setTanggalDeadlineDenda(Integer tanggalDeadlineDenda) {
		this.tanggalDeadlineDenda = tanggalDeadlineDenda;
	}

	/**
	 * @return besaran denda &mdash; rupiah tetap, atau persen bila
	 *         {@link #getDendaMengunakanPersen()} bernilai {@code true}; {@code 0.0} bila belum diisi
	 */
	public Double getDenda() {
		return denda == null ? 0.0 : denda;
	}

	/**
	 * @param denda besaran denda; satuannya ditentukan {@code dendaMengunakanPersen}
	 */
	public void setDenda(Double denda) {
		this.denda = denda;
	}

	/**
	 * @return {@code true} bila {@link #getDenda()} berarti persen; {@code false} (bawaan) bila rupiah tetap
	 */
	public Boolean getDendaMengunakanPersen() {
		return dendaMengunakanPersen == null ? false : dendaMengunakanPersen;
	}

	/**
	 * @param dendaMengunakanPersen {@code true} untuk denda berbasis persentase nominal
	 */
	public void setDendaMengunakanPersen(Boolean dendaMengunakanPersen) {
		this.dendaMengunakanPersen = dendaMengunakanPersen;
	}

	/**
	 * @return batas akhir pembayaran tetap, atau {@code null} bila tenggat mengikuti pola per bulan
	 */
	public Date getDeadlineTagihan() {
		return deadlineTagihan;
	}

	/**
	 * @param deadlineTagihan batas akhir pembayaran tetap
	 */
	public void setDeadlineTagihan(Date deadlineTagihan) {
		this.deadlineTagihan = deadlineTagihan;
	}

	/**
	 * @return {@code true} bila denda keterlambatan berlaku untuk paket ini; {@code false} bila belum diisi
	 */
	public Boolean getTerdapatDenda() {
		return terdapatDenda == null ? false : terdapatDenda;
	}

	/**
	 * @param terdapatDenda menyalakan perhitungan denda
	 */
	public void setTerdapatDenda(Boolean terdapatDenda) {
		this.terdapatDenda = terdapatDenda;
	}

	/**
	 * @return {@code true} (bawaan) bila tanggal tagihan selalu mengikuti
	 *         {@link #getTanggalTagihan()} alih-alih tanggal per bulan
	 */
	public Boolean getTanggalTagihanMengikutiDefault() {
		return tanggalTagihanMengikutiDefault == null ? true : tanggalTagihanMengikutiDefault;
	}

	/**
	 * @param tanggalTagihanMengikutiDefault {@code null} diperlakukan sebagai {@code true}
	 */
	public void setTanggalTagihanMengikutiDefault(Boolean tanggalTagihanMengikutiDefault) {
		this.tanggalTagihanMengikutiDefault = tanggalTagihanMengikutiDefault;
	}

	/**
	 * @return jurusan sasaran, atau {@code null} bila paket berlaku untuk semua jurusan
	 *         (juga {@code null} bila rujukannya sudah tidak valid)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penjurusan_sekolah_id", nullable = true)
	public PenjurusanSekolah getPenjurusanSekolah() {
		penjurusanSekolah = check(penjurusanSekolah);
		return penjurusanSekolah;
	}

	/**
	 * @param penjurusanSekolah jurusan sasaran, atau {@code null} untuk semua jurusan
	 */
	public void setPenjurusanSekolah(PenjurusanSekolah penjurusanSekolah) {
		this.penjurusanSekolah = penjurusanSekolah;
	}

	/**
	 * Gelombang PSB sasaran.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Field DIKOSONGKAN bila jenis biaya induk tidak (lagi) menyalakan
	 * {@code gelombangTertentu}. Konsekuensinya berlapis: kolom {@code current_gelombang_pendaftaran_psb_id}
	 * ikut kosong di basis data, {@link #getTahunAjaran()} berhenti mengambil tahun ajaran dari
	 * gelombang, dan {@code criterion2} pada {@code ambilCriterionGelombang} tidak lagi menemukan
	 * baris ini. Mematikan opsi "gelombang tertentu" pada satu
	 * {@link ais.database.model.sekolah.JenisBiayaSekolah} karena itu memutus ikatan gelombang
	 * seluruh paket turunannya &mdash; tanpa dialog konfirmasi apa pun.</p>
	 *
	 * @return gelombang PSB sasaran, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "current_gelombang_pendaftaran_psb_id")
	public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
		gelombangPendaftaranPsb = check(gelombangPendaftaranPsb);

		if (getJenisBiayaSekolah() == null || !getJenisBiayaSekolah().getGelombangTertentu()) {
			gelombangPendaftaranPsb = null;
		}

		return this.gelombangPendaftaranPsb;
	}

	/**
	 * @param gelombangPendaftaranPsb gelombang PSB sasaran, atau {@code null}
	 */
	public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
	}

	/**
	 * Paket PSB sasaran. Perilakunya sejajar dengan
	 * {@link #getGelombangPendaftaranPsb()}: field DIKOSONGKAN bila jenis biaya induk tidak
	 * menyalakan {@code paketTertentu}.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * @return paket PSB sasaran, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket_psb")
	public PaketPsb getPaketPsb() {
		paketPsb = check(paketPsb);

		if (getJenisBiayaSekolah() == null || !getJenisBiayaSekolah().getPaketTertentu()) {
			paketPsb = null;
		}

		return paketPsb;
	}

	/**
	 * @param paketPsb paket PSB sasaran, atau {@code null}
	 */
	public void setPaketPsb(PaketPsb paketPsb) {
		this.paketPsb = paketPsb;
	}

	/**
	 * @return {@code true} (bawaan) bila tenggat mengikuti bulan berjalan; bila {@code true},
	 *         ke-12 kolom {@code tanggalTagihanBulanN} akan dikosongkan oleh getternya
	 *         masing-masing
	 */
	public Boolean getTanggalTagihanMengikutiBulanBerjalan() {
		return tanggalTagihanMengikutiBulanBerjalan == null ? true : tanggalTagihanMengikutiBulanBerjalan;
	}

	/**
	 * @param tanggalTagihanMengikutiBulanBerjalan {@code null} diperlakukan sebagai {@code true}
	 */
	public void setTanggalTagihanMengikutiBulanBerjalan(Boolean tanggalTagihanMengikutiBulanBerjalan) {
		this.tanggalTagihanMengikutiBulanBerjalan = tanggalTagihanMengikutiBulanBerjalan;
	}

	/**
	 * Memetakan nomor bulan (1&ndash;12) ke TAHUN-nya berdasarkan rentang
	 * {@code bulanMulai}&ndash;{@code bulanSampai} yang berformat {@code YYYYMM}.
	 *
	 * <p><b>Cara kerja &amp; kuirk.</b> Perulangan berjalan atas BILANGAN BULAT antara kedua nilai
	 * tersebut, mis. {@code 202407} sampai {@code 202506} &mdash; hampir seratus iterasi yang
	 * sebagian besar dibuang oleh penjagaan {@code bulan &gt; 12 || bulan &lt; 1}. Cara ini benar
	 * tetapi boros, dan dua pemeriksaan {@code m &lt; getBulanMulai()}/{@code m &gt; getBulanSampai()}
	 * di dalam badan perulangan tidak pernah bernilai benar karena batasnya sudah dipakai sebagai
	 * batas perulangan.</p>
	 *
	 * <p><b>Kasus tepi.</b> Rentang yang melintasi tahun tetap benar (bulan 1&ndash;12 tiap tahun
	 * diambil, sisanya dibuang), tetapi bila bulan yang sama muncul pada DUA tahun berbeda dalam
	 * satu rentang, entri terakhirlah yang menang &mdash; peta berkunci nomor bulan saja. Nilai
	 * yang bukan enam digit membuat {@code substring(0, 4)} melempar
	 * {@code StringIndexOutOfBoundsException}; method ini tidak menangkapnya. Bila salah satu
	 * batas {@code null} (mis. karena periode jenis biaya bukan Bulanan/Harian), peta kosong
	 * dikembalikan.</p>
	 *
	 * @return peta {@code bulan -> tahun}; kosong bila rentang belum diisi
	 */
	public Map<Integer, Integer> ambilTahun() {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();

		Integer mul = getBulanMulai();
		Integer sam = getBulanSampai();

		if (mul != null && sam != null) {
			for (int m = mul; m <= sam; m++) {
				if ((getBulanMulai() != null && m < getBulanMulai())
						|| (getBulanSampai() != null && m > getBulanSampai())) {
					continue;
				}

				int tahun = Integer.parseInt((m + "").substring(0, 4));
				int bulan = Integer.parseInt((m + "").substring(4));
				if (bulan > 12 || bulan < 1) {
					continue;
				}

				map.put(bulan, tahun);
			}
		}

		return map;
	}

	/**
	 * Menghitung tanggal jatuh tempo untuk satu bulan pada paket berperiode "Bulanan".
	 *
	 * <p>Untuk periode "Bulanan", tahun diambil dari {@link #ambilTahun()} lalu disusun tanggal
	 * 1 bulan tersebut pukul 07:00 waktu server. Bila bulan itu tidak berada dalam rentang
	 * {@code bulanMulai}&ndash;{@code bulanSampai} (sehingga tahunnya tidak ditemukan), hasilnya
	 * jatuh ke {@link #getTanggalTagihan()}. Untuk periode selain "Bulanan", hasilnya selalu
	 * {@link #getTanggalTagihan()}.</p>
	 *
	 * <p><b>Kasus tepi yang menyentuh angka uang.</b> {@link #getTanggalTagihan()} mengembalikan
	 * TANGGAL HARI INI bila kolomnya kosong, sehingga paket yang belum diisi tanggal tagihan akan
	 * memperoleh tenggat "hari ini" &mdash; dan karena hasilnya lalu DISIMPAN oleh
	 * {@code getTanggalTagihanBulanN()}, tanggal itu membeku pada hari pertama baris tersebut
	 * dibaca. Argumen {@code bulan} bernilai {@code null} menghasilkan {@code null} pada cabang
	 * "Bulanan". Variabel lokal {@code tanggalTagihan} membayangi field bernama sama; cabang
	 * {@code else} karenanya selalu bekerja atas variabel lokal yang masih {@code null}, bukan
	 * atas field entity.</p>
	 *
	 * @param bulan nomor bulan 1&ndash;12
	 * @return tanggal jatuh tempo hasil hitungan, atau {@code null} bila tidak dapat ditentukan
	 */
	private Date hitungTanggal(Integer bulan) {
		Date tanggalTagihan = null;
		if (getJenisBiayaSekolah() != null && getJenisBiayaSekolah().getPeriode().equalsIgnoreCase("Bulanan")) {

			if (bulan != null) {
				Map<Integer, Integer> tahuns = ambilTahun();
				Integer tahun = tahuns.get(bulan);
				if (tahun != null) {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.YEAR, tahun);
					calendar.set(Calendar.MONTH, bulan - 1);
					calendar.set(Calendar.DATE, 1);
					calendar.set(Calendar.HOUR_OF_DAY, 7);
					calendar.set(Calendar.MINUTE, 0);
					calendar.set(Calendar.SECOND, 0);
					tanggalTagihan = calendar.getTime();
				} else {
					tanggalTagihan = getTanggalTagihan();
				}
			}
		} else {

			if (getTanggalTagihanMengikutiDefault()) {
				tanggalTagihan = getTanggalTagihan();
			} else if (tanggalTagihan == null) {
				tanggalTagihan = getTanggalTagihan();
			}
		}

		return tanggalTagihan;
	}

	/**
	 * Tanggal jatuh tempo bulan ke-1.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Bila {@link #getTanggalTagihanMengikutiBulanBerjalan()} bernilai {@code false} dan
	 * kolomnya masih kosong, tanggal DIHITUNG sekali lewat {@code hitungTanggal(1)} lalu
	 * disimpan &mdash; membaca paket biaya karenanya dapat memateraikan 12 tanggal jatuh tempo
	 * sekaligus ke basis data. Bila mengikuti bulan berjalan, kolom justru DIKOSONGKAN.</p>
	 *
	 * @return tanggal jatuh tempo bulan ke-1, atau {@code null} bila mengikuti bulan berjalan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihanBulan1() {
		if (!getTanggalTagihanMengikutiBulanBerjalan()) {
			if (tanggalTagihanBulan1 == null) {
				tanggalTagihanBulan1 = hitungTanggal(1);
			}
		} else {
			tanggalTagihanBulan1 = null;
		}
		return tanggalTagihanBulan1;
	}

	/**
	 * @param tanggalTagihanBulan1 tanggal jatuh tempo bulan ke-1 yang ditetapkan manual
	 */
	public void setTanggalTagihanBulan1(Date tanggalTagihanBulan1) {
		this.tanggalTagihanBulan1 = tanggalTagihanBulan1;
	}

	/**
	 * Tanggal jatuh tempo bulan ke-2.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Bila {@link #getTanggalTagihanMengikutiBulanBerjalan()} bernilai {@code false} dan
	 * kolomnya masih kosong, tanggal DIHITUNG sekali lewat {@code hitungTanggal(2)} lalu
	 * disimpan &mdash; membaca paket biaya karenanya dapat memateraikan 12 tanggal jatuh tempo
	 * sekaligus ke basis data. Bila mengikuti bulan berjalan, kolom justru DIKOSONGKAN.</p>
	 *
	 * @return tanggal jatuh tempo bulan ke-2, atau {@code null} bila mengikuti bulan berjalan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihanBulan2() {
		if (!getTanggalTagihanMengikutiBulanBerjalan()) {
			if (tanggalTagihanBulan2 == null) {
				tanggalTagihanBulan2 = hitungTanggal(2);
			}
		} else {
			tanggalTagihanBulan2 = null;
		}
		return tanggalTagihanBulan2;
	}

	/**
	 * @param tanggalTagihanBulan2 tanggal jatuh tempo bulan ke-2 yang ditetapkan manual
	 */
	public void setTanggalTagihanBulan2(Date tanggalTagihanBulan2) {
		this.tanggalTagihanBulan2 = tanggalTagihanBulan2;
	}

	/**
	 * Tanggal jatuh tempo bulan ke-3.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Bila {@link #getTanggalTagihanMengikutiBulanBerjalan()} bernilai {@code false} dan
	 * kolomnya masih kosong, tanggal DIHITUNG sekali lewat {@code hitungTanggal(3)} lalu
	 * disimpan &mdash; membaca paket biaya karenanya dapat memateraikan 12 tanggal jatuh tempo
	 * sekaligus ke basis data. Bila mengikuti bulan berjalan, kolom justru DIKOSONGKAN.</p>
	 *
	 * @return tanggal jatuh tempo bulan ke-3, atau {@code null} bila mengikuti bulan berjalan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihanBulan3() {
		if (!getTanggalTagihanMengikutiBulanBerjalan()) {
			if (tanggalTagihanBulan3 == null) {
				tanggalTagihanBulan3 = hitungTanggal(3);
			}
		} else {
			tanggalTagihanBulan3 = null;
		}
		return tanggalTagihanBulan3;
	}

	/**
	 * @param tanggalTagihanBulan3 tanggal jatuh tempo bulan ke-3 yang ditetapkan manual
	 */
	public void setTanggalTagihanBulan3(Date tanggalTagihanBulan3) {
		this.tanggalTagihanBulan3 = tanggalTagihanBulan3;
	}

	/**
	 * Tanggal jatuh tempo bulan ke-4.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Bila {@link #getTanggalTagihanMengikutiBulanBerjalan()} bernilai {@code false} dan
	 * kolomnya masih kosong, tanggal DIHITUNG sekali lewat {@code hitungTanggal(4)} lalu
	 * disimpan &mdash; membaca paket biaya karenanya dapat memateraikan 12 tanggal jatuh tempo
	 * sekaligus ke basis data. Bila mengikuti bulan berjalan, kolom justru DIKOSONGKAN.</p>
	 *
	 * @return tanggal jatuh tempo bulan ke-4, atau {@code null} bila mengikuti bulan berjalan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihanBulan4() {
		if (!getTanggalTagihanMengikutiBulanBerjalan()) {
			if (tanggalTagihanBulan4 == null) {
				tanggalTagihanBulan4 = hitungTanggal(4);
			}
		} else {
			tanggalTagihanBulan4 = null;
		}
		return tanggalTagihanBulan4;
	}

	/**
	 * @param tanggalTagihanBulan4 tanggal jatuh tempo bulan ke-4 yang ditetapkan manual
	 */
	public void setTanggalTagihanBulan4(Date tanggalTagihanBulan4) {
		this.tanggalTagihanBulan4 = tanggalTagihanBulan4;
	}

	/**
	 * Tanggal jatuh tempo bulan ke-5.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Bila {@link #getTanggalTagihanMengikutiBulanBerjalan()} bernilai {@code false} dan
	 * kolomnya masih kosong, tanggal DIHITUNG sekali lewat {@code hitungTanggal(5)} lalu
	 * disimpan &mdash; membaca paket biaya karenanya dapat memateraikan 12 tanggal jatuh tempo
	 * sekaligus ke basis data. Bila mengikuti bulan berjalan, kolom justru DIKOSONGKAN.</p>
	 *
	 * @return tanggal jatuh tempo bulan ke-5, atau {@code null} bila mengikuti bulan berjalan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihanBulan5() {
		if (!getTanggalTagihanMengikutiBulanBerjalan()) {
			if (tanggalTagihanBulan5 == null) {
				tanggalTagihanBulan5 = hitungTanggal(5);
			}
		} else {
			tanggalTagihanBulan5 = null;
		}
		return tanggalTagihanBulan5;
	}

	/**
	 * @param tanggalTagihanBulan5 tanggal jatuh tempo bulan ke-5 yang ditetapkan manual
	 */
	public void setTanggalTagihanBulan5(Date tanggalTagihanBulan5) {
		this.tanggalTagihanBulan5 = tanggalTagihanBulan5;
	}

	/**
	 * Tanggal jatuh tempo bulan ke-6.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Bila {@link #getTanggalTagihanMengikutiBulanBerjalan()} bernilai {@code false} dan
	 * kolomnya masih kosong, tanggal DIHITUNG sekali lewat {@code hitungTanggal(6)} lalu
	 * disimpan &mdash; membaca paket biaya karenanya dapat memateraikan 12 tanggal jatuh tempo
	 * sekaligus ke basis data. Bila mengikuti bulan berjalan, kolom justru DIKOSONGKAN.</p>
	 *
	 * @return tanggal jatuh tempo bulan ke-6, atau {@code null} bila mengikuti bulan berjalan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihanBulan6() {
		if (!getTanggalTagihanMengikutiBulanBerjalan()) {
			if (tanggalTagihanBulan6 == null) {
				tanggalTagihanBulan6 = hitungTanggal(6);
			}
		} else {
			tanggalTagihanBulan6 = null;
		}
		return tanggalTagihanBulan6;
	}

	/**
	 * @param tanggalTagihanBulan6 tanggal jatuh tempo bulan ke-6 yang ditetapkan manual
	 */
	public void setTanggalTagihanBulan6(Date tanggalTagihanBulan6) {
		this.tanggalTagihanBulan6 = tanggalTagihanBulan6;
	}

	/**
	 * Tanggal jatuh tempo bulan ke-7.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Bila {@link #getTanggalTagihanMengikutiBulanBerjalan()} bernilai {@code false} dan
	 * kolomnya masih kosong, tanggal DIHITUNG sekali lewat {@code hitungTanggal(7)} lalu
	 * disimpan &mdash; membaca paket biaya karenanya dapat memateraikan 12 tanggal jatuh tempo
	 * sekaligus ke basis data. Bila mengikuti bulan berjalan, kolom justru DIKOSONGKAN.</p>
	 *
	 * @return tanggal jatuh tempo bulan ke-7, atau {@code null} bila mengikuti bulan berjalan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihanBulan7() {
		if (!getTanggalTagihanMengikutiBulanBerjalan()) {
			if (tanggalTagihanBulan7 == null) {
				tanggalTagihanBulan7 = hitungTanggal(7);
			}
		} else {
			tanggalTagihanBulan7 = null;
		}
		return tanggalTagihanBulan7;
	}

	/**
	 * @param tanggalTagihanBulan7 tanggal jatuh tempo bulan ke-7 yang ditetapkan manual
	 */
	public void setTanggalTagihanBulan7(Date tanggalTagihanBulan7) {
		this.tanggalTagihanBulan7 = tanggalTagihanBulan7;
	}

	/**
	 * Tanggal jatuh tempo bulan ke-8.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Bila {@link #getTanggalTagihanMengikutiBulanBerjalan()} bernilai {@code false} dan
	 * kolomnya masih kosong, tanggal DIHITUNG sekali lewat {@code hitungTanggal(8)} lalu
	 * disimpan &mdash; membaca paket biaya karenanya dapat memateraikan 12 tanggal jatuh tempo
	 * sekaligus ke basis data. Bila mengikuti bulan berjalan, kolom justru DIKOSONGKAN.</p>
	 *
	 * @return tanggal jatuh tempo bulan ke-8, atau {@code null} bila mengikuti bulan berjalan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihanBulan8() {
		if (!getTanggalTagihanMengikutiBulanBerjalan()) {
			if (tanggalTagihanBulan8 == null) {
				tanggalTagihanBulan8 = hitungTanggal(8);
			}
		} else {
			tanggalTagihanBulan8 = null;
		}
		return tanggalTagihanBulan8;
	}

	/**
	 * @param tanggalTagihanBulan8 tanggal jatuh tempo bulan ke-8 yang ditetapkan manual
	 */
	public void setTanggalTagihanBulan8(Date tanggalTagihanBulan8) {
		this.tanggalTagihanBulan8 = tanggalTagihanBulan8;
	}

	/**
	 * Tanggal jatuh tempo bulan ke-9.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Bila {@link #getTanggalTagihanMengikutiBulanBerjalan()} bernilai {@code false} dan
	 * kolomnya masih kosong, tanggal DIHITUNG sekali lewat {@code hitungTanggal(9)} lalu
	 * disimpan &mdash; membaca paket biaya karenanya dapat memateraikan 12 tanggal jatuh tempo
	 * sekaligus ke basis data. Bila mengikuti bulan berjalan, kolom justru DIKOSONGKAN.</p>
	 *
	 * @return tanggal jatuh tempo bulan ke-9, atau {@code null} bila mengikuti bulan berjalan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihanBulan9() {
		if (!getTanggalTagihanMengikutiBulanBerjalan()) {
			if (tanggalTagihanBulan9 == null) {
				tanggalTagihanBulan9 = hitungTanggal(9);
			}
		} else {
			tanggalTagihanBulan9 = null;
		}
		return tanggalTagihanBulan9;
	}

	/**
	 * @param tanggalTagihanBulan9 tanggal jatuh tempo bulan ke-9 yang ditetapkan manual
	 */
	public void setTanggalTagihanBulan9(Date tanggalTagihanBulan9) {
		this.tanggalTagihanBulan9 = tanggalTagihanBulan9;
	}

	/**
	 * Tanggal jatuh tempo bulan ke-10.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Bila {@link #getTanggalTagihanMengikutiBulanBerjalan()} bernilai {@code false} dan
	 * kolomnya masih kosong, tanggal DIHITUNG sekali lewat {@code hitungTanggal(10)} lalu
	 * disimpan &mdash; membaca paket biaya karenanya dapat memateraikan 12 tanggal jatuh tempo
	 * sekaligus ke basis data. Bila mengikuti bulan berjalan, kolom justru DIKOSONGKAN.</p>
	 *
	 * @return tanggal jatuh tempo bulan ke-10, atau {@code null} bila mengikuti bulan berjalan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihanBulan10() {
		if (!getTanggalTagihanMengikutiBulanBerjalan()) {
			if (tanggalTagihanBulan10 == null) {
				tanggalTagihanBulan10 = hitungTanggal(10);
			}
		} else {
			tanggalTagihanBulan10 = null;
		}
		return tanggalTagihanBulan10;
	}

	/**
	 * @param tanggalTagihanBulan10 tanggal jatuh tempo bulan ke-10 yang ditetapkan manual
	 */
	public void setTanggalTagihanBulan10(Date tanggalTagihanBulan10) {
		this.tanggalTagihanBulan10 = tanggalTagihanBulan10;
	}

	/**
	 * Tanggal jatuh tempo bulan ke-11.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Bila {@link #getTanggalTagihanMengikutiBulanBerjalan()} bernilai {@code false} dan
	 * kolomnya masih kosong, tanggal DIHITUNG sekali lewat {@code hitungTanggal(11)} lalu
	 * disimpan &mdash; membaca paket biaya karenanya dapat memateraikan 12 tanggal jatuh tempo
	 * sekaligus ke basis data. Bila mengikuti bulan berjalan, kolom justru DIKOSONGKAN.</p>
	 *
	 * @return tanggal jatuh tempo bulan ke-11, atau {@code null} bila mengikuti bulan berjalan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihanBulan11() {
		if (!getTanggalTagihanMengikutiBulanBerjalan()) {
			if (tanggalTagihanBulan11 == null) {
				tanggalTagihanBulan11 = hitungTanggal(11);
			}
		} else {
			tanggalTagihanBulan11 = null;
		}
		return tanggalTagihanBulan11;
	}

	/**
	 * @param tanggalTagihanBulan11 tanggal jatuh tempo bulan ke-11 yang ditetapkan manual
	 */
	public void setTanggalTagihanBulan11(Date tanggalTagihanBulan11) {
		this.tanggalTagihanBulan11 = tanggalTagihanBulan11;
	}

	/**
	 * Tanggal jatuh tempo bulan ke-12.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Bila {@link #getTanggalTagihanMengikutiBulanBerjalan()} bernilai {@code false} dan
	 * kolomnya masih kosong, tanggal DIHITUNG sekali lewat {@code hitungTanggal(12)} lalu
	 * disimpan &mdash; membaca paket biaya karenanya dapat memateraikan 12 tanggal jatuh tempo
	 * sekaligus ke basis data. Bila mengikuti bulan berjalan, kolom justru DIKOSONGKAN.</p>
	 *
	 * @return tanggal jatuh tempo bulan ke-12, atau {@code null} bila mengikuti bulan berjalan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihanBulan12() {
		if (!getTanggalTagihanMengikutiBulanBerjalan()) {
			if (tanggalTagihanBulan12 == null) {
				tanggalTagihanBulan12 = hitungTanggal(12);
			}
		} else {
			tanggalTagihanBulan12 = null;
		}
		return tanggalTagihanBulan12;
	}

	/**
	 * @param tanggalTagihanBulan12 tanggal jatuh tempo bulan ke-12 yang ditetapkan manual
	 */
	public void setTanggalTagihanBulan12(Date tanggalTagihanBulan12) {
		this.tanggalTagihanBulan12 = tanggalTagihanBulan12;
	}

	/**
	 * Saklar mode "khusus siswa tertentu".
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Dipaksa {@code false} bila paket menyasar kelas les &mdash; peserta kelas les selalu
	 * ditentukan keanggotaan kelasnya, bukan daftar siswa pilihan.</p>
	 *
	 * <p><b>Dampak keamanan/cakupan.</b> Saat bernilai {@code true}, penyaring kelas, angkatan,
	 * dan status awal DILEWATI pada {@link #terapkanFilterPembayaran(Criteria, Siswa, CalonSiswa)},
	 * dan pada {@code DetailTagihanSiswaHelper.initCriteriaDenganNama} penyaring {@code sekolah}
	 * juga dibuang seluruhnya. Yang tersisa sebagai pembatas hanyalah isi tabel jembatan
	 * {@code PengaturanBiayaPunyaSiswa}. Jadi keakuratan daftar siswa pilihan itu adalah
	 * satu-satunya penjaga agar tagihan tidak melompat ke peserta didik sekolah lain.</p>
	 *
	 * @return {@code true} bila sasaran ditentukan daftar siswa pilihan; {@code false} bila belum
	 *         diisi atau bila paket menyasar kelas les
	 */
	public Boolean getKhususBuatSiswaTertentu() {
		if (getKelasLesSiswa() != null) {
			khususBuatSiswaTertentu = false;
		}
		return khususBuatSiswaTertentu == null ? false : khususBuatSiswaTertentu;
	}

	/**
	 * @param khususBuatSiswaTertentu menyalakan mode daftar siswa pilihan
	 */
	public void setKhususBuatSiswaTertentu(Boolean khususBuatSiswaTertentu) {
		this.khususBuatSiswaTertentu = khususBuatSiswaTertentu;
	}

	/**
	 * Rombongan belajar reguler sasaran.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Dikosongkan bila paket menyasar kelas les, karena kedua sasaran itu saling meniadakan.
	 * Nilai {@code null} berarti "semua kelas" pada
	 * {@link #terapkanFilterPembayaran(Criteria, Siswa, CalonSiswa)} &mdash; jadi pengosongan ini
	 * MEMPERLUAS cakupan pada jalur reguler, meski jalur kelas les kemudian mempersempitnya lagi.</p>
	 *
	 * @return kelas sasaran, atau {@code null} untuk semua kelas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_siswa")
	public KelasSiswa getKelasSiswa() {
		kelasSiswa = check(kelasSiswa);
		if (getKelasLesSiswa() != null) {
			kelasSiswa = null;
		}
		return kelasSiswa;
	}

	/**
	 * @param kelasSiswa rombongan belajar sasaran, atau {@code null} untuk semua kelas
	 */
	public void setKelasSiswa(KelasSiswa kelasSiswa) {
		this.kelasSiswa = kelasSiswa;
	}

	/**
	 * @return status awal peserta didik sasaran, atau {@code null} untuk semua status.
	 *         Blok yang dikomentari di dalam method menunjukkan bahwa nilai bawaan
	 *         {@code ConstantValues.BARU_SISWA} pernah dipaksakan di sini &mdash; kini
	 *         {@code null} sengaja dibiarkan agar berarti "semua status"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_siswa")
	public StatusAwalSiswa getStatusAwalSiswa() {
		statusAwalSiswa = check(statusAwalSiswa);
//		if (statusAwalSiswa == null) {
//			statusAwalSiswa = ConstantValues.BARU_SISWA;
//		}
		return statusAwalSiswa;
	}

	/**
	 * @param statusAwalSiswa status awal sasaran, atau {@code null} untuk semua status
	 */
	public void setStatusAwalSiswa(StatusAwalSiswa statusAwalSiswa) {
		this.statusAwalSiswa = statusAwalSiswa;
	}

	/**
	 * @return {@code true} bila paket masih berlaku; {@code null} pada kolom diperlakukan
	 *         sebagai {@code true} sehingga baris lama tetap menagih
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif {@code false} menonaktifkan seluruh penagihan paket ini
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return {@code true} bila ada bulan yang dikecualikan dari penagihan (daftarnya di
	 *         {@link #getBulanYangTidakAdaTagihannya()}); {@code false} bila belum diisi
	 */
	public Boolean getTerdapatBulanYangTidakAdaTagihannya() {
		return terdapatBulanYangTidakAdaTagihannya == null ? false : terdapatBulanYangTidakAdaTagihannya;
	}

	/**
	 * @param terdapatBulanYangTidakAdaTagihannya menyalakan pengecualian bulan tanpa tagihan
	 */
	public void setTerdapatBulanYangTidakAdaTagihannya(Boolean terdapatBulanYangTidakAdaTagihannya) {
		this.terdapatBulanYangTidakAdaTagihannya = terdapatBulanYangTidakAdaTagihannya;
	}

	/**
	 * Daftar bulan yang dikecualikan dari penagihan.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p><b>Format kontraktual.</b> Nilai dinormalkan menjadi {@code ,a,b,} &mdash; dibungkus koma
	 * di kedua ujung, dengan koma ganda diringkas (tiga kali {@code replaceAll} berurutan, cukup
	 * untuk merapikan sampai empat koma beruntun). Pembungkusan koma itu BUKAN kosmetik: pemanggil
	 * mencocokkan keanggotaan dengan {@code contains(","+nilai+",")}, sehingga tanpa koma penutup
	 * "1" akan cocok dengan "11". Rangkaian yang hanya berisi koma dinormalkan menjadi string
	 * kosong.</p>
	 *
	 * @return daftar bulan dalam format {@code ,a,b,}; string kosong bila tidak ada pengecualian
	 */
	public String getBulanYangTidakAdaTagihannya() {
		bulanYangTidakAdaTagihannya = (bulanYangTidakAdaTagihannya == null
				|| bulanYangTidakAdaTagihannya.trim().equalsIgnoreCase(",") ? ""
						: "," + bulanYangTidakAdaTagihannya.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (bulanYangTidakAdaTagihannya.equals(",")) {
			bulanYangTidakAdaTagihannya = "";
		} else if (bulanYangTidakAdaTagihannya.equals(",,")) {
			bulanYangTidakAdaTagihannya = "";
		} else if (bulanYangTidakAdaTagihannya.equals(",,,")) {
			bulanYangTidakAdaTagihannya = "";
		}
		return bulanYangTidakAdaTagihannya;
	}

	/**
	 * @param bulanYangTidakAdaTagihannya daftar bulan berkoma; akan dinormalkan oleh getternya
	 */
	public void setBulanYangTidakAdaTagihannya(String bulanYangTidakAdaTagihannya) {
		this.bulanYangTidakAdaTagihannya = bulanYangTidakAdaTagihannya;
	}

	/**
	 * @return kelas les/kursus sasaran, atau {@code null} bila paket ini bukan paket biaya
	 *         les. Bila terisi, ia mematikan {@link #getKelasSiswa()} dan
	 *         {@link #getKhususBuatSiswaTertentu()}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_les_siswa", nullable = true)
	public KelasLesSiswa getKelasLesSiswa() {
		kelasLesSiswa = check(kelasLesSiswa);
		return kelasLesSiswa;
	}

	/**
	 * @param kelasLesSiswa kelas les sasaran, atau {@code null}
	 */
	public void setKelasLesSiswa(KelasLesSiswa kelasLesSiswa) {
		this.kelasLesSiswa = kelasLesSiswa;
	}

	/**
	 * @return {@code true} bila timer {@link #chekNotifikasi()} boleh mengirim pemberitahuan
	 *         tagihan untuk paket ini; {@code false} bila belum diisi
	 */
	public Boolean getAktifkanNotifikasi() {
		return aktifkanNotifikasi == null ? false : aktifkanNotifikasi;
	}

	/**
	 * @param aktifkanNotifikasi menyalakan notifikasi tagihan otomatis
	 */
	public void setAktifkanNotifikasi(Boolean aktifkanNotifikasi) {
		this.aktifkanNotifikasi = aktifkanNotifikasi;
	}

	/**
	 * Jadwal pengiriman notifikasi tagihan.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Dikosongkan bila notifikasi dimatikan. Hanya JAM dan MENIT yang dibandingkan untuk paket
	 * berperiode "Bulanan" (ditambah tanggal), sementara periode lain juga mencocokkan tahun dan
	 * bulan &mdash; lihat {@code waktuNotifikasiSama}.</p>
	 *
	 * @return jadwal notifikasi, atau {@code null} bila notifikasi tidak aktif
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuNotifikasi() {
		if (!getAktifkanNotifikasi()) {
			waktuNotifikasi = null;
		}
		return waktuNotifikasi;
	}

	/**
	 * @param waktuNotifikasi jadwal pengiriman notifikasi
	 */
	public void setWaktuNotifikasi(Date waktuNotifikasi) {
		this.waktuNotifikasi = waktuNotifikasi;
	}

	/**
	 * @return batas akhir pembayaran yang dicetak pada notifikasi; bulan dan tahunnya ditimpa
	 *         periode berjalan saat pesan dirakit, jadi yang bermakna di sini hanya TANGGAL-nya
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getBatasWaktuPembayaran() {
		return batasWaktuPembayaran;
	}

	/**
	 * @param batasWaktuPembayaran batas akhir pembayaran untuk naskah notifikasi
	 */
	public void setBatasWaktuPembayaran(Date batasWaktuPembayaran) {
		this.batasWaktuPembayaran = batasWaktuPembayaran;
	}

	/**
	 * @return naskah notifikasi yang sudah di-{@code trim()}, atau string kosong bila belum
	 *         diisi. Tempat isian yang dikenali: {@code [Nama Siswa]}, {@code [Nama Sekolah]},
	 *         {@code [BULAN]}, {@code [TAHUN]}, {@code [TOTAL]}, {@code [Tanggal]}, dan
	 *         {@code [<kode item biaya>]}
	 */
	@Column(name = "template_notifikasi", columnDefinition = "text")
	public String getTemplateNotifikasi() {
		return templateNotifikasi == null ? "" : templateNotifikasi.trim();
	}

	/**
	 * @param templateNotifikasi naskah notifikasi mentah (kolom {@code text})
	 */
	public void setTemplateNotifikasi(String templateNotifikasi) {
		this.templateNotifikasi = templateNotifikasi;
	}

	/**
	 * Merakit CONTOH naskah notifikasi dari daftar item biaya yang tercentang di layar,
	 * untuk ditawarkan ke kolom {@code templateNotifikasi} saat admin menekan tombol penyegar.
	 *
	 * <p>Setiap item yang tercentang menjadi satu baris rincian bernomor dengan tempat isian
	 * {@code Rp. [<kode item biaya>],-}; kode itulah yang nanti digantikan nominal nyata oleh
	 * {@link #kirimTemplate(Siswa, CalonSiswa, Integer, Integer)}. Sisa naskah adalah surat baku
	 * kepada wali murid, lengkap dengan {@code [TOTAL]} dan {@code [Tanggal]}, serta petunjuk
	 * kanal pembayaran (transfer bank, virtual account, tunai) yang MASIH BERISI titik-titik
	 * placeholder dan harus disunting admin.</p>
	 *
	 * <p><b>Catatan.</b> Method ini murni pembangun teks: ia TIDAK menyimpan apa pun dan tidak
	 * menyentuh {@code templateNotifikasi}. Ia menerima tipe UI ZK ({@code Checkbox}) di dalam
	 * kelas entity &mdash; kopling lapisan yang dipertahankan demi kompatibilitas layar
	 * {@code PengaturanBiayaAction}. Baris rincian memakai akhiran baris {@code \r\n}, sedangkan
	 * {@code bangunTemplateDefaultDaftarUlang} memakai {@code \n}; keduanya sama-sama diubah
	 * menjadi {@code &lt;br&gt;} sebelum dikirim sebagai notifikasi.</p>
	 *
	 * @param selectedItemBiayaSekolah kotak centang item biaya di layar; tiap kotak membawa
	 *        atribut {@code "pengaturanBiayaItemBiaya"}
	 * @return naskah contoh siap sunting
	 * @throws NullPointerException bila ada kotak centang tanpa atribut
	 *         {@code pengaturanBiayaItemBiaya}
	 */
	public String refreshTemplate(List<Checkbox> selectedItemBiayaSekolah) {

		String ss = "";
		int nomor = 1;
		for (Checkbox checkbox : selectedItemBiayaSekolah) {
			PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya = (PengaturanBiayaItemBiaya) checkbox
					.getAttribute("pengaturanBiayaItemBiaya");
			if (checkbox.isChecked()) {
				String s = nomor + ". " + pengaturanBiayaItemBiaya.getItemBiayaSekolah().getNama() + " : Rp. ["
						+ pengaturanBiayaItemBiaya.getItemBiayaSekolah().getKode() + "],-\r\n";

				ss += s;
				nomor++;
			}
		}

		String s = "Yth. Bapak/Ibu Wali Murid [Nama Siswa],\r\n" + "\r\n"
				+ "Kami dari [Nama Sekolah] ingin menyampaikan informasi mengenai tagihan putra/putri Bapak/Ibu untuk bulan [BULAN] [TAHUN]:\r\n"
				+ "\r\n" + "Rincian Tagihan:\r\n" + ss + "\r\n" + "Total Tagihan: Rp. [TOTAL],-\r\n" + "\r\n"
				+ "Mohon untuk segera melakukan pembayaran sebesar Rp [TOTAL],-. Pembayaran dapat dilakukan melalui:\r\n"
				+ "\r\n" + "*   Transfer Bank: .........., Nomor Rekening: .........., Atas Nama: ..........\r\n"
				+ "*   Virtual Account: Anda bisa mendapatkan kode virtual account via aplikasi mobile eSchool\r\n"
				+ "*   Pembayaran Tunai: Di kantor [Nama Sekolah]\r\n" + "\r\n"
				+ "Batas akhir pembayaran: [Tanggal]\r\n" + "\r\n"
				+ "Jika Bapak/Ibu telah melakukan pembayaran, mohon abaikan pesan ini. Jika ada pertanyaan atau kendala terkait pembayaran, silakan menghubungi bagian keuangan sekolah atau dapat langsung datang ke sekolah.\r\n"
				+ "\r\n" + "Atas perhatian dan kerjasamanya, kami mengucapkan terima kasih.\r\n" + "\r\n"
				+ "Hormat kami,";

		return s;

	}

	/**
	 * Memanaskan cache tagihan untuk banyak paket biaya sekaligus, secara paralel.
	 *
	 * <p><b>Alur.</b> (1) Sesi Hibernate terpisah dibuka untuk mengambil daftar paket biaya AKTIF,
	 * lalu segera ditutup di {@code finally}. (2) Daftar itu dikerjakan lewat kolam thread
	 * berukuran {@code DbThreadPool.safe(50)}; tiap tugas memanggil
	 * {@link #reloadTagihan(PengaturanBiaya, boolean)} yang sudah aman untuk banyak thread karena
	 * membuka sesinya sendiri dan mengunci per id. (3) {@code shutdown()} dipanggil lalu ditunggu
	 * maksimal satu jam agar proses tidak menggantung permanen bila basis data bermasalah.</p>
	 *
	 * <p><b>Kuirk penting.</b> Kueri memakai {@code setMaxResults(50)} dengan urutan
	 * {@code id} menurun &mdash; jadi yang dipanaskan hanyalah 50 paket biaya AKTIF TERBARU di
	 * SELURUH instalasi, bukan "data dua tahun ajaran lalu" seperti bunyi komentar di dalamnya,
	 * dan tanpa penyaring sekolah/yayasan sama sekali. Pada instalasi dengan lebih dari 50 paket
	 * aktif, paket milik sekolah yang jarang membuat konfigurasi baru tidak akan pernah ikut
	 * dipanaskan; ini hanya memengaruhi kecepatan (cache terisi belakangan secara malas), bukan
	 * kebenaran angka.</p>
	 *
	 * @param refresh {@code true} memaksa membuang cache lama sebelum memuat ulang
	 */
	@SuppressWarnings("unchecked")
	public static void reloadSemuaTagihan(final boolean refresh) {

		Session session = null;

		// Penampung untuk semua pengaturan biaya yang akan diproses
		List<PengaturanBiaya> semuaPengaturanBiaya = new ArrayList<PengaturanBiaya>();

		try {
			// 1. Buka Session Lokal khusus untuk nge-load daftar PengaturanBiaya
			session = HibernateUtil.getSessionFactory().openSession();

			// Ambil data Dua Tahun Ajaran Lalu
			semuaPengaturanBiaya = ConstantValues.simpleList(session.createCriteria(PengaturanBiaya.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setMaxResults(50).addOrder(Order.desc("id")), PengaturanBiaya.class);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/PengaturanBiaya.java:1129");
		} finally {
			// 2. WAJIB Tutup Session LOKAL (Jangan panggil HibernateUtil.closeSession()
			// global)
			if (session != null) {
				try {
					session.close();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/PengaturanBiaya.java:1137");
				}
			}
		}
		System.out
				.println("--> reloadSemuaTagihan " + semuaPengaturanBiaya.size() + " (Force Reload: " + refresh + ")");
		// 3. Eksekusi proses Tagihan menggunakan Thread Pool (Maksimal 10 Thread)
		if (!semuaPengaturanBiaya.isEmpty()) {

			// Membuat pool pekerja (worker) dengan kapasitas 10 thread sekaligus
			java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(ais.common.DbThreadPool.safe(50));

			for (final PengaturanBiaya pb : semuaPengaturanBiaya) {
				// Melempar tugas ke dalam antrean Thread Pool
				executor.submit(new Runnable() {
					/**
					 * Satu tugas dalam kolam thread: memanaskan cache tagihan untuk SATU paket biaya.
					 * Aman dijalankan paralel karena {@link PengaturanBiaya#reloadTagihan(PengaturanBiaya, boolean)}
					 * membuka sesi Hibernate sendiri dan mengunci per id paket. Galat ditelan dan dicatat agar
					 * satu paket bermasalah tidak menggugurkan tugas paket lainnya.
					 */
					@Override
					public void run() {
						try {
							// Fungsi reloadTagihan() milikmu sebelumnya sudah aman (thread-safe)
							// karena menggunakan session.openSession() masing-masing dan sinkronisasi ID.
							PengaturanBiaya.reloadTagihan(pb, refresh);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/PengaturanBiaya.java:1159");
						}
					}
				});
			}

			// Menginstruksikan executor untuk tidak menerima tugas baru
			executor.shutdown();

			try {
				// 4. (Opsional tapi Direkomendasikan)
				// Tunggu sampai semua thread selesai bekerja.
				// Dibatasi maksimal 1 jam agar thread tidak freeze permanen jika database
				// bermasalah.
				executor.awaitTermination(1, java.util.concurrent.TimeUnit.HOURS);
			} catch (InterruptedException e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/PengaturanBiaya.java:1175");
			}
		}
	}

	/**
	 * Tugas latar harian yang menyelaraskan kolom {@code siswa.kelasLes} dengan keanggotaan
	 * kelas les yang sebenarnya.
	 *
	 * <p><b>Pemicu.</b> Method ini dipanggil berulang kali dari timer aplikasi
	 * ({@code UserOnlineCounter}) tetapi hanya BEKERJA bila jam server tepat menunjukkan pukul
	 * 01:01 &mdash; penjadwalan "poor man's cron" tanpa penanda sudah-dijalankan. Bila timer
	 * berdetak lebih dari sekali dalam menit itu, pekerjaan yang sama dijalankan berulang; bila
	 * aplikasi sedang mati pada menit itu, sinkronisasi hari tersebut terlewat sama sekali.</p>
	 *
	 * <p><b>Alur.</b> Sebuah {@code Thread} baru dijalankan agar timer tidak terblokir. Di dalamnya
	 * seluruh {@link ais.database.model.sekolah.KelasLesSiswa} aktif ditelusuri; untuk setiap
	 * anggota ({@code KelasLesSiswaPunyaSiswa}) yang punya siswa, data siswa dan calon siswa
	 * di-{@code populate}, lalu {@code siswa.setKelasLes(kelasLesSiswa)} disimpan dengan transaksi
	 * per baris.</p>
	 *
	 * <p><b>Kasus tepi.</b> Kueri TIDAK menyaring sekolah/yayasan &mdash; disengaja, karena ini
	 * pekerjaan pemeliharaan seluruh instalasi. Karena {@code siswa.kelasLes} bersifat tunggal,
	 * siswa yang terdaftar pada LEBIH DARI SATU kelas les akan berakhir menunjuk kelas yang
	 * diproses terakhir; kolom itu memang hanya penanda tampilan, bukan sumber kebenaran
	 * keanggotaan. Galat per baris ditelan agar satu data rusak tidak menghentikan sinkronisasi
	 * lainnya. Blok {@code finally} menutup sesi lokal DAN memanggil
	 * {@code HibernateUtil.closeSession()} yang menyentuh sesi thread &mdash; aman di sini karena
	 * berjalan di thread sendiri.</p>
	 */
	public static void checkKelasLes() {

		Calendar cal = WaktuUtil.getCalendar();

		int jam = cal.get(Calendar.HOUR_OF_DAY);
		int menit = cal.get(Calendar.MINUTE);

		if (jam == 1 && menit == 1) {

			new Thread(new Runnable() {

				/**
				 * Badan tugas latar sinkronisasi kelas les (lihat {@link PengaturanBiaya#checkKelasLes()}).
				 * Berjalan di thread terpisah dengan sesi Hibernate sendiri; menelusuri seluruh kelas les aktif
				 * lalu menyegarkan kolom {@code siswa.kelasLes} setiap anggotanya dengan transaksi per baris.
				 */
				@SuppressWarnings("unchecked")
				@Override
				public void run() {

					// 1. Buka Session Baru (Isolated Session)
					// Menggunakan openSession() agar terpisah dari session HTTP request
					Session session = HibernateUtil.getSessionFactory().openSession();

					try {
						List<KelasLesSiswa> kelases = session.createCriteria(KelasLesSiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.list();

						for (KelasLesSiswa kelasLesSiswa : kelases) {
							try {

								Criteria criteria = session.createCriteria(KelasLesSiswaPunyaSiswa.class)
										.add(Restrictions.isNotNull("siswa"))
										.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa));

								List<KelasLesSiswaPunyaSiswa> kelasLesSiswaPunyaSiswas = ConstantValues
										.simpleList(criteria, KelasLesSiswaPunyaSiswa.class);

								for (KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa : kelasLesSiswaPunyaSiswas) {
									Siswa siswa = kelasLesSiswaPunyaSiswa.getSiswa();

									Siswa.populate(siswa);

									CalonSiswa calonSiswa = kelasLesSiswaPunyaSiswa.getCalonSiswa();
									if (calonSiswa != null) {
										CalonSiswa.populate(calonSiswa);
									}

									try {
										siswa.setKelasLes(kelasLesSiswa);
										session.getTransaction().begin();
										Common.refreshUpdate(session, siswa);
										Common.refreshUpdate(session, kelasLesSiswaPunyaSiswa);
										session.getTransaction().commit();

									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/PengaturanBiaya.java:1232");
									}

								}
								kelasLesSiswaPunyaSiswas.clear();
								kelasLesSiswaPunyaSiswas = null;

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}

						}

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/PengaturanBiaya.java:1246");
					} finally {
						// 2. WAJIB Tutup Session
						if (session != null && session.isOpen()) {
							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}
						}
						HibernateUtil.closeSession();
					}
				}
			}).start();
		}
	}

	/**
	 * Menutup sesi Hibernate yang dibuka sendiri ({@code openSession()}) secara berjenjang
	 * &mdash; {@code clear()}, lalu {@code disconnect()}, lalu {@code close()} &mdash; dengan setiap
	 * langkah dibungkus {@code try/catch} tersendiri sehingga kegagalan satu langkah tidak
	 * menggagalkan langkah berikutnya. Semua galat dicatat ke {@code ErrorAuditUtil}, tidak
	 * dilempar.
	 *
	 * @param session sesi yang hendak ditutup; {@code null} diabaikan
	 */
	private static void closeOpenedSessionQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1269");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1273");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1279");
		}
	}

	/**
	 * Membandingkan jadwal notifikasi dengan waktu sekarang, dengan aturan berbeda menurut
	 * periode biaya.
	 *
	 * <p>Jam dan menit harus selalu sama. Untuk paket BULANAN, cukup ditambah kesamaan TANGGAL
	 * &mdash; agar notifikasi berulang setiap bulan pada tanggal yang sama. Untuk paket non-bulanan
	 * (sekali bayar), tahun dan bulan juga harus sama sehingga notifikasi hanya terkirim sekali
	 * pada tanggal yang ditetapkan.</p>
	 *
	 * <p><b>Kasus tepi.</b> Karena presisinya menit, ketepatan pengiriman bergantung pada timer
	 * memanggil {@link #chekNotifikasi()} setidaknya sekali dalam menit tersebut; bila timer
	 * memanggil lebih dari sekali dalam menit yang sama, notifikasi terkirim ganda &mdash; tidak
	 * ada penanda "sudah dikirim" yang disimpan. Argumen {@code null} menghasilkan {@code false}.</p>
	 *
	 * @param jadwal   jadwal notifikasi paket
	 * @param sekarang waktu server saat ini
	 * @param bulanan  {@code true} bila periode jenis biaya "Bulanan"
	 * @return {@code true} bila saatnya mengirim notifikasi
	 */
	private static boolean waktuNotifikasiSama(Calendar jadwal, Calendar sekarang, boolean bulanan) {
		if (jadwal == null || sekarang == null) {
			return false;
		}
		boolean jamMenitSama = jadwal.get(Calendar.HOUR_OF_DAY) == sekarang.get(Calendar.HOUR_OF_DAY)
				&& jadwal.get(Calendar.MINUTE) == sekarang.get(Calendar.MINUTE);
		if (!jamMenitSama) {
			return false;
		}
		if (bulanan) {
			return jadwal.get(Calendar.DATE) == sekarang.get(Calendar.DATE);
		}
		return jadwal.get(Calendar.YEAR) == sekarang.get(Calendar.YEAR)
				&& jadwal.get(Calendar.MONTH) == sekarang.get(Calendar.MONTH)
				&& jadwal.get(Calendar.DATE) == sekarang.get(Calendar.DATE);
	}

	/**
	 * Titik masuk timer latar untuk mesin notifikasi tagihan: menyisir seluruh paket biaya
	 * yang notifikasinya aktif, lalu memanggil {@link #kirimTemplate(Integer, Integer)} bagi yang
	 * jadwalnya jatuh pada menit ini. Dipanggil dari {@code UserOnlineCounter} (tiap {@code index}
	 * genap, dengan penjaga agar tidak tumpang tindih), BUKAN dari layar.
	 *
	 * <p><b>Perlindungan yang sengaja dipasang &mdash; jangan dihapus.</b></p>
	 * <ul>
	 *   <li>Sesi dibuka sendiri ({@code openSession()}) dan ditutup di {@code finally}, sehingga
	 *       timer tidak memakai sesi request.</li>
	 *   <li>{@code FlushMode.MANUAL} + {@code criteria.setReadOnly(true)}: kombinasi ini mencegah
	 *       GETTER DESTRUKTIF entity ini (lihat dokumentasi kelas) ikut ter-flush menjadi
	 *       pembaruan baris + revisi Envers. Jalur inilah satu-satunya pembaca massal entity ini
	 *       yang benar-benar aman dari efek tersebut.</li>
	 *   <li>{@code jenisBiayaSekolah} di-{@code JOIN} di muka ({@code FetchMode.JOIN} + alias LEFT
	 *       JOIN) supaya {@code getPeriode()} tidak diakses setelah sesi ditutup &mdash; penyebab
	 *       {@code LazyInitializationException} pada versi terdahulu.</li>
	 *   <li>Setiap paket diproses dalam {@code try/catch} sendiri agar satu data bermasalah tidak
	 *       menghentikan pengecekan paket lain.</li>
	 * </ul>
	 *
	 * <p><b>Kasus tepi.</b> Kueri TIDAK menyaring sekolah/yayasan; ini memang tugas seluruh
	 * instalasi. Kegagalan yang disebabkan pool koneksi penuh dikenali
	 * {@code koneksiNotifikasiTidakTersedia} dan hanya dicatat sesekali
	 * ({@code logKoneksiNotifikasiTerbatas}) agar log tidak banjir. Bulan yang diteruskan ke
	 * {@link #kirimTemplate(Integer, Integer)} adalah {@code Calendar.MONTH} berbasis NOL.</p>
	 */
	@SuppressWarnings("unchecked")
	public static void chekNotifikasi() {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			session.setFlushMode(org.hibernate.FlushMode.MANUAL);
			Criteria criteria = session.createCriteria(PengaturanBiaya.class)
					.setFetchMode("jenisBiayaSekolah", FetchMode.JOIN)
					.createAlias("jenisBiayaSekolah", "jenisBiayaSekolah", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("aktifkanNotifikasi", true))
					.add(Restrictions.isNotNull("templateNotifikasi"))
					.add(Restrictions.ne("templateNotifikasi", ""))
					.add(Restrictions.isNotNull("waktuNotifikasi"));
			criteria.setReadOnly(true);

			List<PengaturanBiaya> pengaturanBiayas = criteria.list();

			if (pengaturanBiayas == null || pengaturanBiayas.isEmpty()) {
				return;
			}

			Calendar sekarang = WaktuUtil.getCalendar();
			int bulanSekarang = sekarang.get(Calendar.MONTH);
			int tahunSekarang = sekarang.get(Calendar.YEAR);

			for (PengaturanBiaya pengaturanBiaya : pengaturanBiayas) {
				try {
					if (pengaturanBiaya == null || pengaturanBiaya.getWaktuNotifikasi() == null) {
						continue;
					}

					JenisBiayaSekolah jenisBiayaSekolah = pengaturanBiaya.getJenisBiayaSekolah();
					String periode = jenisBiayaSekolah == null ? "" : jenisBiayaSekolah.getPeriode();
					boolean bulanan = "Bulanan".equalsIgnoreCase(periode);

					Calendar jadwal = WaktuUtil.getCalendar();
					jadwal.setTime(pengaturanBiaya.getWaktuNotifikasi());

					if (waktuNotifikasiSama(jadwal, sekarang, bulanan)) {
						pengaturanBiaya.kirimTemplate(tahunSekarang, bulanSekarang);
					}
				} catch (Exception e) {
					try {
						Common.tampilErrorJikaAdmin(e);
					} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1353");
					}
				}
			}
		} catch (Exception e) {
			if (koneksiNotifikasiTidakTersedia(e)) {
				logKoneksiNotifikasiTerbatas(e);
				return;
			}
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1360");
			}
		} finally {
			closeOpenedSessionQuietly(session);
		}
	}

	/**
	 * Mengenali apakah sebuah galat berakar pada pool koneksi database yang penuh/tidak
	 * tersedia, dengan menelusuri seluruh rantai {@code getCause()} dan mencocokkan nama kelas
	 * maupun pesannya terhadap empat penanda: {@code "cannot open connection"},
	 * {@code "cannotacquireresource"}, {@code "connections could not be acquired"}, dan
	 * {@code "checkout a connection has timed out"}.
	 *
	 * <p>Pencocokan berbasis TEKS memang rapuh terhadap perubahan versi pustaka, tetapi dipilih
	 * supaya tidak perlu bergantung pada tipe pengecualian spesifik C3P0/Hibernate. Bila pola
	 * berubah, dampaknya hanya log kembali ramai &mdash; bukan kesalahan fungsional.</p>
	 *
	 * @param e galat yang hendak diklasifikasi; {@code null} menghasilkan {@code false}
	 * @return {@code true} bila galat berasal dari ketiadaan koneksi database
	 */
	private static boolean koneksiNotifikasiTidakTersedia(Throwable e) {
		Throwable t = e;
		while (t != null) {
			String nama = t.getClass() == null ? "" : t.getClass().getName();
			String pesan = t.getMessage();
			String lower = (nama + " " + (pesan == null ? "" : pesan)).toLowerCase(java.util.Locale.ENGLISH);
			if (lower.indexOf("cannot open connection") >= 0 || lower.indexOf("cannotacquireresource") >= 0
					|| lower.indexOf("connections could not be acquired") >= 0
					|| lower.indexOf("checkout a connection has timed out") >= 0) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	/**
	 * Mencatat galat "pool koneksi penuh" ke {@code System.err} paling banyak sekali tiap
	 * {@code JEDA_LOG_KONEKSI_NOTIFIKASI} (10 menit).
	 *
	 * <p>Pembatasan laju memakai satu variabel statis biasa tanpa sinkronisasi; pada kondisi balapan
	 * paling buruk beberapa thread bisa mencatat bersamaan &mdash; dampaknya sekadar beberapa baris
	 * log ekstra, jadi biaya penguncian tidak sepadan.</p>
	 *
	 * @param e galat penyebab; boleh {@code null}
	 */
	private static void logKoneksiNotifikasiTerbatas(Throwable e) {
		long now = System.currentTimeMillis();
		if (now - terakhirLogKoneksiNotifikasi < JEDA_LOG_KONEKSI_NOTIFIKASI) {
			return;
		}
		terakhirLogKoneksiNotifikasi = now;
		System.err.println("Notifikasi tagihan sekolah dilewati sementara karena pool koneksi database penuh: "
				+ (e == null ? "" : e.getMessage()));
	}

	/**
	 * Mengirim notifikasi tagihan paket ini ke SELURUH peserta didik sasarannya.
	 *
	 * <p><b>Alur.</b> Jenis peserta didik ditentukan {@code jenisBiayaSekolah.getGunakanCalonSiswa()}.
	 * Untuk siswa aktif, daftar sasaran diambil dari
	 * {@code DetailTagihanSiswaHelper.initCriteria(session, this, ...)}; untuk calon siswa dari
	 * {@code DetailTagihanCalonSiswaHelper.initCriteria(...)}. Sesi Hibernate dibuka khusus untuk
	 * kueri daftar itu lalu DITUTUP sebelum perulangan pengiriman, sehingga tiap pengiriman
	 * membuka sesinya sendiri. Setiap peserta didik dikirim lewat
	 * {@link #kirimTemplate(Siswa, CalonSiswa, Integer, Integer)} di dalam {@code try/catch}
	 * sendiri.</p>
	 *
	 * <p><b>Cakupan tenant.</b> Daftar sasaran dibatasi
	 * {@code Restrictions.eq("sekolah", pengaturanBiaya.getSekolah())} di dalam helper &mdash;
	 * KECUALI bila paket ini berjenis "khusus siswa tertentu", yang membuang penyaring sekolah dan
	 * hanya bersandar pada isi {@code PengaturanBiayaPunyaSiswa}. Perhatikan bahwa kolom tenant
	 * yang dipakai di sini ({@code pengaturanBiaya.sekolah}) BERBEDA dari yang dipakai
	 * {@link #terapkanFilterPembayaran(Criteria, Siswa, CalonSiswa)}
	 * ({@code jenisBiayaSekolah.sekolah}).</p>
	 *
	 * <p><b>Efek samping yang tidak terduga dari namanya.</b> Perulangan ini pada akhirnya
	 * memanggil {@code Tagihan.ambilAtauBuat(...)} yang MENYIMPAN baris {@code Tagihan} baru.
	 * Menjalankan notifikasi karena itu menerbitkan tagihan, bukan sekadar memberi tahu.
	 * Tidak ada penanda "sudah dikirim" yang disimpan, sehingga bila timer memanggil dua kali
	 * pada menit yang sama, seluruh peserta didik menerima pesan ganda &mdash; namun tagihannya
	 * tidak berganda karena {@code Tagihan} dilindungi indeks unik {@code kodeUnik}.</p>
	 *
	 * @param tahunCurrent tahun periode pembayaran
	 * @param bulanCurrent bulan periode pembayaran berbasis NOL ({@code Calendar.MONTH})
	 */
	public void kirimTemplate(Integer tahunCurrent, Integer bulanCurrent) {

		boolean gunakanCalonSiswa = false;
		try {
			JenisBiayaSekolah jenisBiayaSekolah = getJenisBiayaSekolah();
			gunakanCalonSiswa = jenisBiayaSekolah != null && jenisBiayaSekolah.getGunakanCalonSiswa();
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1376");
			}
		}

		if (!gunakanCalonSiswa) {
			Session session = null;
			List<Siswa> siswas = new ArrayList<Siswa>();
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				siswas = ConstantValues.simpleList(
						DetailTagihanSiswaHelper.initCriteria(session, this, null, new Textbox(), null, false, false),
						Siswa.class);
			} catch (Exception e) {
				try {
					Common.tampilErrorJikaAdmin(e);
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1391");
				}
			} finally {
				closeOpenedSessionQuietly(session);
			}

			for (Siswa siswa : siswas) {
				try {
					kirimTemplate(siswa, null, tahunCurrent, bulanCurrent);
				} catch (Exception e) {
					try {
						Common.tampilErrorJikaAdmin(e);
					} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1403");
					}
				}

			}
		} else {
			Session session = null;
			List<CalonSiswa> calonSiswas = new ArrayList<CalonSiswa>();
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				calonSiswas = ConstantValues.simpleList(
						DetailTagihanCalonSiswaHelper.initCriteria(session, this, null, new Textbox(), null, false, false),
						CalonSiswa.class);
			} catch (Exception e) {
				try {
					Common.tampilErrorJikaAdmin(e);
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1419");
				}
			} finally {
				closeOpenedSessionQuietly(session);
			}

			for (CalonSiswa calonSiswa : calonSiswas) {
				try {
					kirimTemplate(null, calonSiswa, tahunCurrent, bulanCurrent);
				} catch (Exception e) {
					try {
						Common.tampilErrorJikaAdmin(e);
					} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1431");
					}
				}

			}
		}

	}

	/**
	 * Merakit dan mengirim satu notifikasi tagihan untuk SATU peserta didik, lewat tiga
	 * kanal sekaligus: notifikasi aplikasi (lonceng), surel, dan WhatsApp.
	 *
	 * <p><b>Alur lengkap.</b></p>
	 * <ol>
	 *   <li>Sesi Hibernate khusus dibuka; seluruh {@code PengaturanBiayaItemBiaya} milik paket ini
	 *       diambil.</li>
	 *   <li>Naskah diambil dari {@link #getTemplateNotifikasi()}. Bila kosong, dipakai surat baku
	 *       "Tagihan Daftar Ulang" yang dirakit {@code bangunTemplateDefaultDaftarUlang}.</li>
	 *   <li>{@link #getBatasWaktuPembayaran()} dipindahkan ke bulan/tahun periode berjalan, lalu
	 *       mengisi {@code [Tanggal]}. {@code [Nama Siswa]} dan {@code [Nama Sekolah]} diisi dari
	 *       peserta didik.</li>
	 *   <li>Untuk tiap item biaya: tarif diambil lewat {@code TagihanUtil.ambilNominalBiaya(...)},
	 *       periode ditentukan dari {@code nominalBiaya.getTahunbulan()} atau dari
	 *       {@code untukTahun}/{@code untukBulan} jenis biaya, lalu untuk tiap angsuran
	 *       ({@code bayarKe}) dipanggil {@code Tagihan.ambilAtauBuat(...)}. Nominal item mengisi
	 *       tempat isian {@code [<kode item&gt;]}, dan {@code [BULAN]}/{@code [TAHUN]} diisi dari
	 *       tagihan yang ditemukan.</li>
	 *   <li>{@code [TOTAL]} diisi, sesi ditutup, lalu pesan dikirim: satu catatan gabungan
	 *       notifikasi-halaman + surel lewat {@code MailSender.simpanNotifikasiHalaman(...)}, dan
	 *       satu pesan WhatsApp per nomor telepon lewat {@code Wa.kirimWaViaUltramsg(...)}.</li>
	 * </ol>
	 *
	 * <p><b>MENYENTUH DATA UANG &mdash; perhatikan tiga hal berikut.</b></p>
	 * <ul>
	 *   <li>Method ini MENERBITKAN TAGIHAN. {@code Tagihan.ambilAtauBuat(...)} menyimpan baris baru
	 *       bila belum ada; pengiriman notifikasi karenanya mengubah kewajiban keuangan peserta
	 *       didik. Duplikasi antar-permintaan bersamaan dicegah indeks unik {@code kodeUnik} di
	 *       sisi {@code Tagihan}, bukan oleh method ini.</li>
	 *   <li><b>Angka {@code [TOTAL]} hanya menjumlahkan SATU angsuran per item.</b> Di dalam
	 *       perulangan {@code bayarKe}, variabel {@code tagihan} ditimpa tiap iterasi dan hanya
	 *       angsuran TERAKHIR yang belum terbayar serta bernominal &gt; 0,1 yang tersisa; nilai
	 *       itulah yang ditambahkan ke {@code total} dan yang mengisi tempat isian per item.
	 *       Untuk item biaya yang dicicil lebih dari sekali dalam satu periode, angka yang
	 *       diberitahukan kepada wali murid LEBIH KECIL daripada kewajiban sebenarnya. Angka pada
	 *       layar pembayaran dan pada tagihan tetap benar &mdash; yang keliru hanya angka di dalam
	 *       pesan.</li>
	 *   <li>{@code [BULAN]} dan {@code [TAHUN]} diisi oleh item PERTAMA yang memiliki tagihan,
	 *       karena {@code replaceIgnoreCase} tidak melakukan apa-apa setelah tempat isiannya habis.
	 *       Bila item-item dalam satu paket jatuh pada bulan berbeda, pesan hanya menyebut satu
	 *       bulan.</li>
	 * </ul>
	 *
	 * <p><b>Kasus tepi lain.</b> Bila total kurang dari 0,01 method mengembalikan {@code false}
	 * dan tidak mengirim apa pun &mdash; tetapi tagihan yang sempat dibuat pada langkah 4 TETAP
	 * tersimpan. Nomor telepon pengisi seperti {@code "000000000"} disaring. Kegagalan pengiriman
	 * surel/notifikasi ditelan dan dicatat, jadi WhatsApp tetap dicoba. Blok {@code finally}
	 * menjamin sesi tertutup walau terjadi galat (idempoten lewat {@code isOpen()}), karena versi
	 * terdahulu hanya menutupnya di jalur normal sehingga bocor saat galat.</p>
	 *
	 * @param siswa        siswa aktif penerima, atau {@code null} bila calon siswa
	 * @param calonSiswa   calon siswa penerima, atau {@code null} bila siswa aktif
	 * @param tahunCurrent tahun periode pembayaran
	 * @param bulanCurrent bulan periode pembayaran berbasis NOL ({@code Calendar.MONTH})
	 * @return {@code true} bila pesan benar-benar dikirim; {@code false} bila totalnya nol
	 */
	@SuppressWarnings("unchecked")
	public boolean kirimTemplate(Siswa siswa, CalonSiswa calonSiswa, Integer tahunCurrent, Integer bulanCurrent) {

		Integer pembayaranTerakhir = PembayaranSiswa.convert(tahunCurrent, bulanCurrent + 1);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
		List<PengaturanBiayaItemBiaya> pengaturanBiayaItemBiayas = ConstantValues
				.simpleList(session.createCriteria(PengaturanBiayaItemBiaya.class)
						.add(Restrictions.eq("pengaturanBiaya.id", this.getId())), PengaturanBiayaItemBiaya.class);

		String template = getTemplateNotifikasi();
		// Bila admin belum mengisi template, pakai format baku surat ke wali murid
		// (tagihan daftar ulang) yang dirakit otomatis dari rincian + total + batas waktu.
		boolean pakaiDefaultTemplate = template == null || template.trim().isEmpty();
		StringBuilder rincianDefault = new StringBuilder();
		int[] noItemDefault = { 1 };

		Date d = getBatasWaktuPembayaran();

		if (d != null) {
			Calendar cal1 = WaktuUtil.getCalendar();
			cal1.setTime(d);
			cal1.set(Calendar.MONTH, bulanCurrent);
			cal1.set(Calendar.YEAR, tahunCurrent);
			d = cal1.getTime();
		}

		template = StringUtils.replaceIgnoreCase(template, "[Tanggal]", d == null ? "" : Common.dateFormat6.get().format(d));

		if (siswa != null) {
			template = StringUtils.replaceIgnoreCase(template, "[Nama Siswa]", siswa.getNama());
			template = StringUtils.replaceIgnoreCase(template, "[Nama Sekolah]", siswa.getSekolah().getNama());
		} else if (calonSiswa != null) {
			template = StringUtils.replaceIgnoreCase(template, "[Nama Siswa]", calonSiswa.getNama());
			template = StringUtils.replaceIgnoreCase(template, "[Nama Sekolah]", calonSiswa.getSekolah().getNama());
		}
		Double total = 0.0;
		for (PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya : pengaturanBiayaItemBiayas) {
			NominalBiaya nominalBiaya = TagihanUtil.ambilNominalBiaya(pengaturanBiayaItemBiaya, siswa,
					pembayaranTerakhir, session);

			Integer tahunbulan = nominalBiaya.getTahunbulan() != null ? nominalBiaya.getTahunbulan()
					: PembayaranSiswa.convert(nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getUntukTahun(),
							nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getUntukBulan());
			Tagihan tagihan = null;
			for (int bayarKe = 1; bayarKe <= nominalBiaya.getDibayarSebayak(); bayarKe++) {
				Double dibayarManual = null;
				Tagihan tagihanData = Tagihan.ambilAtauBuat(session, nominalBiaya.getItemBiayaSekolah(),
						nominalBiaya.getPengaturanBiaya(), siswa, calonSiswa, bayarKe, nominalBiaya, tahunbulan,
						dibayarManual, false);
				if (tagihanData != null && tagihanData.getId() != null && tagihanData.getNominal() > 0.1
						&& tagihanData.getPembayaranSiswaDetail() == null) {
					tagihan = tagihanData;
				}
			}

			total += tagihan == null ? 0.0 : tagihan.getNominal();

			if (pakaiDefaultTemplate) {
				rincianDefault.append(noItemDefault[0]++).append(". ")
						.append(pengaturanBiayaItemBiaya.getItemBiayaSekolah().getNama()).append(" : Rp. ")
						.append(Common.numberFormat.get().format(tagihan == null ? 0.0 : tagihan.getNominal()))
						.append(",-\n");
			}

			template = StringUtils.replaceIgnoreCase(template,
					"[" + pengaturanBiayaItemBiaya.getItemBiayaSekolah().getKode() + "]",
					Common.numberFormat.get().format(tagihan == null ? 0.0 : tagihan.getNominal()));

			try {
				template = StringUtils.replaceIgnoreCase(template, "[BULAN]",
						tagihan == null || tagihan.getBulan() == null ? "" : Common.BULAN[tagihan.getBulan() - 1]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1512");
				// TODO: handle exception
			}
			try {
				template = StringUtils.replaceIgnoreCase(template, "[TAHUN]",
						tagihan == null || tagihan.getTahun() == null ? "" : tagihan.getTahun() + "");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1518");
				// TODO: handle exception
			}

		}

		template = StringUtils.replaceIgnoreCase(template, "[TOTAL]", Common.numberFormat.get().format(total));

		if (pakaiDefaultTemplate) {
			template = bangunTemplateDefaultDaftarUlang(siswa, calonSiswa, total, rincianDefault.toString(), d,
					tahunCurrent, bulanCurrent);
		}

		closeOpenedSessionQuietly(session);
		session = null;

		if (total < 0.01) {
			return false;
		}

		try {
			String emailUser = "";
			if (siswa != null && siswa.getAlamatEmail() != null && Common.isValidEmailAddress(siswa.getAlamatEmail())) {
				emailUser += emailUser.trim().isEmpty() ? siswa.getAlamatEmail().trim()
						: "," + siswa.getAlamatEmail().trim();
			}
			if (calonSiswa != null && calonSiswa.getAlamatEmail() != null
					&& Common.isValidEmailAddress(calonSiswa.getAlamatEmail())) {
				emailUser += emailUser.trim().isEmpty() ? calonSiswa.getAlamatEmail().trim()
						: "," + calonSiswa.getAlamatEmail().trim();
			}
			JSONArray attachmentsData = null;
			JSONArray userIds = new JSONArray();
			if (siswa != null && !siswa.getNomorIndukNasional().isEmpty()) {
				userIds.put(siswa.getNomorIndukNasional());
			} else if (calonSiswa != null && calonSiswa.getNomorIndukNasional() != null) {
				userIds.put(calonSiswa.getNomorIndukNasional());
			}
			String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
			String subject = "Pemberitahuan Tagihan Daftar Ulang"
					+ (siswa != null ? " — " + siswa.getNama() : (calonSiswa != null ? " — " + calonSiswa.getNama() : ""));
			// Tersambung ke fitur Notifikasi terbaru: notifikasi aplikasi clickable + muncul
			// di lonceng/pusat notifikasi (classData otomatis) + email (bila aktif), satu record.
			MailSender.simpanNotifikasiHalaman(userIds, emailUser, subject, template.replaceAll("\n", "<br>"), sender,
					calonSiswa != null ? calonSiswa : siswa, null, null, "WARNING", false, true, false);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1563");
			// TODO: handle exception
		}

		String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal",
				"*Pesan ini dibuat secara otomatis oleh sistem sebagai notifikasi/pemberitahuan kepada Anda*\n\n")
				.getNilai();
		Set<String> forms = siswa != null ? siswa.ambilTelp()
				: calonSiswa != null ? calonSiswa.ambilTelp() : new HashSet<String>();
		for (String from : forms) {
			if (from != null && !from.trim().isEmpty()
					&& !(from == null || from.toString().trim().isEmpty()
							|| from.toString().trim().equals("00000000000000000000")
							|| from.toString().trim().equals("000000000"))) {

				String urlD = null;

				Sekolah sekolah = siswa != null ? siswa.getSekolah()
						: calonSiswa != null ? calonSiswa.getSekolah() : null;

				try {
					Wa.kirimWaViaUltramsg(from, dawal + template, null, urlD,
							Wa.buatProfile(sekolah, PerguruanTinggiUtil.getPerguruanTinggi()));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/PengaturanBiaya.java:1587");
				}
			}
		}

		return true;
	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1597");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1598");}
			}
		}
	}

	/**
	 * Rakit isi notifikasi <b>Tagihan Daftar Ulang</b> baku (surat ke wali murid)
	 * ketika template notifikasi belum diisi admin. Mencantumkan nama siswa, nama
	 * sekolah, tahun ajaran, bulan/tahun pembayaran, rincian per item, total nominal,
	 * dan batas akhir pembayaran — sesuai data periode &amp; nominal yang ada di sistem.
	 *
	 * @param siswa        siswa pemilik tagihan (boleh null bila calon siswa)
	 * @param calonSiswa   calon siswa (boleh null bila siswa)
	 * @param total        total nominal tagihan
	 * @param rincian      teks rincian per item (sudah berbaris-baris)
	 * @param batasAkhir   batas akhir pembayaran
	 * @param tahunCurrent tahun pembayaran
	 * @param bulanCurrent bulan pembayaran (0-11, indeks {@code Common.BULAN})
	 * @return isi pemberitahuan (teks dengan baris baru; diubah ke HTML/teks oleh pemanggil)
	 */
	private String bangunTemplateDefaultDaftarUlang(Siswa siswa, ais.database.model.sekolah.CalonSiswa calonSiswa,
			double total, String rincian, Date batasAkhir, Integer tahunCurrent, Integer bulanCurrent) {
		String namaSiswa = siswa != null ? siswa.getNama() : (calonSiswa != null ? calonSiswa.getNama() : "");
		String namaSekolah = "";
		try {
			namaSekolah = siswa != null ? siswa.getSekolah().getNama()
					: (calonSiswa != null && calonSiswa.getSekolah() != null ? calonSiswa.getSekolah().getNama() : "");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1625");
		}
		String ta = getTahunAjaran() == null ? "" : getTahunAjaran();
		String bln = "";
		try {
			if (bulanCurrent != null && bulanCurrent >= 0 && bulanCurrent < Common.BULAN.length) {
				bln = Common.BULAN[bulanCurrent];
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengaturanBiaya.java:1633");
		}
		String thn = tahunCurrent == null ? "" : tahunCurrent + "";
		String batas = batasAkhir == null ? "" : Common.dateFormat6.get().format(batasAkhir);
		String totalStr = Common.numberFormat.get().format(total);

		return "Yth. Bapak/Ibu Wali Murid " + namaSiswa + ",\n\n"
				+ "Diberitahukan kepada orang tua/wali siswa bahwa daftar ulang untuk tahun ajaran " + ta
				+ " dapat kami verifikasi bila seluruh kewajiban pada tahun ajaran sebelumnya telah diselesaikan. "
				+ "Daftar ulang tersebut menjadi penentu penempatan kelas. Mohon segera melakukan pembayaran sebelum "
				+ "batas waktu yang ditentukan.\n\n" + "Berikut ini " + namaSekolah
				+ " menyampaikan informasi mengenai tagihan putra/putri Bapak/Ibu untuk dibayarkan bulan " + bln + " "
				+ thn + " sebelum tahun ajaran baru " + ta + " dimulai.\n\n" + "Rincian Tagihan:\n" + rincian + "\n"
				+ "Total Tagihan: Rp. " + totalStr + ",-\n\n" + "Mohon untuk segera melakukan pembayaran sebesar Rp "
				+ totalStr + ",-. Pembayaran dapat dilakukan melalui aplikasi GOWL.\n" + "Batas akhir pembayaran: "
				+ batas + "\n\n"
				+ "Jika Bapak/Ibu telah melakukan pembayaran, mohon abaikan pesan ini. Jika ada pertanyaan atau kendala "
				+ "terkait pembayaran, silakan menghubungi bagian keuangan sekolah atau dapat langsung datang ke "
				+ "sekolah.\n\nAtas perhatian dan kerjasamanya, kami mengucapkan terima kasih.\n\nHormat kami,\n"
				+ namaSekolah;
	}

	/**
	 * @return {@code true} bila layar rincian menampilkan seluruh kelas, bukan hanya kelas
	 *         sasaran paket; {@code false} bila belum diisi
	 */
	public Boolean getTampilanSemuaKelas() {
		return tampilanSemuaKelas == null ? false : tampilanSemuaKelas;
	}

	/**
	 * @param tampilanSemuaKelas menyalakan tampilan lintas kelas pada layar rincian
	 */
	public void setTampilanSemuaKelas(Boolean tampilanSemuaKelas) {
		this.tampilanSemuaKelas = tampilanSemuaKelas;
	}

	/**
	 * Daftar id {@link ais.database.model.sekolah.Tagihan} yang WAJIB lunas lebih dulu
	 * sebelum tagihan paket ini boleh dibayar.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Formatnya {@code ,id,id,} &mdash; sama seperti {@link #getBulanYangTidakAdaTagihannya()},
	 * dan koma pembungkusnya bagian dari kontrak karena {@code PembayaranOnline} mencocokkannya
	 * dengan {@code contains(","+tagihan.getId()+",")}. Daftar ini adalah penegak URUTAN pelunasan;
	 * merusak formatnya berarti melumpuhkan prasyarat pembayaran secara diam-diam (gagal terbuka,
	 * bukan gagal tertutup).</p>
	 *
	 * @return daftar id tagihan prasyarat dalam format {@code ,id,id,}; string kosong bila tidak ada
	 */
	@Column(columnDefinition = "text")
	public String getWajibDibayarSebelumnya() {
		wajibDibayarSebelumnya = (wajibDibayarSebelumnya == null || wajibDibayarSebelumnya.trim().equalsIgnoreCase(",")
				? ""
				: "," + wajibDibayarSebelumnya.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (wajibDibayarSebelumnya.equals(",")) {
			wajibDibayarSebelumnya = "";
		} else if (wajibDibayarSebelumnya.equals(",,")) {
			wajibDibayarSebelumnya = "";
		} else if (wajibDibayarSebelumnya.equals(",,,")) {
			wajibDibayarSebelumnya = "";
		}
		return wajibDibayarSebelumnya == null ? "" : wajibDibayarSebelumnya.trim();
	}

	/**
	 * @param wajibDibayarSebelumnya daftar id tagihan prasyarat berkoma; dinormalkan oleh getternya
	 */
	public void setWajibDibayarSebelumnya(String wajibDibayarSebelumnya) {
		this.wajibDibayarSebelumnya = wajibDibayarSebelumnya;
	}

	/**
	 * @return {@code true} bila paket bekerja dalam mode berlangganan &mdash; tagihan
	 *         berikutnya terbit otomatis setelah
	 *         {@link #getJumlahHariPenagihanBerikutnya()} hari; {@code false} bila belum diisi
	 */
	public Boolean getOtomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion() {
		return otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion == null ? false
				: otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion;
	}

	/**
	 * @param otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion menyalakan mode
	 *        berlangganan/penagihan berulang otomatis
	 */
	public void setOtomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion(
			Boolean otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion) {
		this.otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion = otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion;
	}

	/**
	 * @return jarak hari antar penerbitan tagihan pada mode berlangganan; {@code 30} bila
	 *         belum diisi
	 */
	public Integer getJumlahHariPenagihanBerikutnya() {
		return jumlahHariPenagihanBerikutnya == null ? 30 : jumlahHariPenagihanBerikutnya;
	}

	/**
	 * @param jumlahHariPenagihanBerikutnya jarak hari antar penerbitan tagihan
	 */
	public void setJumlahHariPenagihanBerikutnya(Integer jumlahHariPenagihanBerikutnya) {
		this.jumlahHariPenagihanBerikutnya = jumlahHariPenagihanBerikutnya;
	}

	/**
	 * @return tanggal kedaluwarsa tagihan paket ini, atau {@code null}. Nilai ini diteruskan
	 *         apa adanya ke klien mobile sebagai medan {@code tagihanKadaluarsa} pada
	 *         {@code /Api tagihan_siswa}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTagihanKadaluarsa() {
		return tagihanKadaluarsa;
	}

	/**
	 * @param tagihanKadaluarsa tanggal kedaluwarsa tagihan
	 */
	public void setTagihanKadaluarsa(Date tagihanKadaluarsa) {
		this.tagihanKadaluarsa = tagihanKadaluarsa;
	}

	/**
	 * Pengguna yang mengunci paket ini.
	 *
	 * <p>Bila tidak {@code null}, layar kasir dan layar rincian memperlakukan SELURUH tagihan
	 * turunan paket ini sebagai hanya-baca ({@code DetailTagihanSiswaHelper},
	 * {@code DetailTagihanCalonSiswaHelper}, {@code PembayaranOnline}, {@code RekapPembayaran},
	 * {@code TagihanUtilCalonSiswa}). Ini kunci EDIT operasional, BUKAN mekanisme hak akses:
	 * pemasangan dan pelepasannya dilakukan lewat {@code PengaturanBiayaAction.tampilkanKunci},
	 * yang hanya menampilkan tombol bagi pengguna non-siswa/non-calon dan menonaktifkan tombol
	 * buka-kunci bagi pengguna selain pemasangnya &mdash; kecuali admin, yang selalu boleh.</p>
	 *
	 * @return pengguna pemasang kunci, atau {@code null} bila paket tidak terkunci
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kunci", nullable = true)
	public Tbmuser getKunci() {
		kunci = check(kunci);
		return kunci;
	}

	/**
	 * @param kunci pengguna pemasang kunci; {@code null} membuka kunci
	 */
	public void setKunci(Tbmuser kunci) {
		this.kunci = kunci;
	}

	/**
	 * Asrama sasaran paket.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Dikosongkan bila {@code tanpaAsrama} bernilai {@code true} &mdash; kedua sasaran saling
	 * meniadakan. Perhatikan bahwa cabang ini membaca FIELD {@code tanpaAsrama} langsung, bukan
	 * {@link #getTanpaAsrama()}, sehingga tidak terjadi rekursi tak berujung dengan getter
	 * pasangannya.</p>
	 *
	 * @return asrama sasaran, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asrama_siswa", nullable = true)
	public AsramaSiswa getAsramaSiswa() {
		if (tanpaAsrama != null && tanpaAsrama.equals(true)) {
			asramaSiswa = null;
		} else {
			asramaSiswa = check(asramaSiswa);
		}
		return asramaSiswa;
	}

	/**
	 * @param asramaSiswa asrama sasaran, atau {@code null}
	 */
	public void setAsramaSiswa(AsramaSiswa asramaSiswa) {
		this.asramaSiswa = asramaSiswa;
	}

	/**
	 * Penanda "paket ini untuk peserta didik NON-asrama".
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Dipaksa {@code false} bila {@code asramaSiswa} terisi &mdash; pasangan pengaman dari
	 * {@link #getAsramaSiswa()}. Nilai ini dibaca
	 * {@code buatCriteriaAsrama}: paket {@code tanpaAsrama = false} DAN tanpa asrama tertentu
	 * dianggap paket UMUM yang berlaku bagi semua orang.</p>
	 *
	 * @return {@code true} bila paket khusus non-asrama; {@code false} bila belum diisi
	 */
	public Boolean getTanpaAsrama() {
		asramaSiswa = check(asramaSiswa);
		if (asramaSiswa != null) {
			tanpaAsrama = false;
		}
		return tanpaAsrama == null ? false : tanpaAsrama;
	}

	/**
	 * @param tanpaAsrama {@code true} menandai paket khusus peserta didik non-asrama
	 */
	public void setTanpaAsrama(Boolean tanpaAsrama) {
		this.tanpaAsrama = tanpaAsrama;
	}

	/**
	 * Daftar bulan yang dibebaskan dari denda keterlambatan.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Dinormalkan ke format berkoma {@code ,a,b,} dengan aturan yang sama persis seperti
	 * {@link #getBulanYangTidakAdaTagihannya()}. Karena isinya menentukan bulan mana yang TIDAK
	 * dikenai denda, kerusakan format di sini langsung berdampak pada angka yang ditagihkan.</p>
	 *
	 * @return daftar bulan bebas denda dalam format {@code ,a,b,}; string kosong bila tidak ada
	 */
	@Column(columnDefinition = "text")
	public String getBulanYangTidakAdaDendanya() {
		bulanYangTidakAdaDendanya = (bulanYangTidakAdaDendanya == null
				|| bulanYangTidakAdaDendanya.trim().equalsIgnoreCase(",") ? ""
						: "," + bulanYangTidakAdaDendanya.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (bulanYangTidakAdaDendanya.equals(",")) {
			bulanYangTidakAdaDendanya = "";
		} else if (bulanYangTidakAdaDendanya.equals(",,")) {
			bulanYangTidakAdaDendanya = "";
		} else if (bulanYangTidakAdaDendanya.equals(",,,")) {
			bulanYangTidakAdaDendanya = "";
		}
		return bulanYangTidakAdaDendanya;
	}

	/**
	 * @param bulanYangTidakAdaDendanya daftar bulan bebas denda berkoma; dinormalkan oleh getternya
	 */
	public void setBulanYangTidakAdaDendanya(String bulanYangTidakAdaDendanya) {
		this.bulanYangTidakAdaDendanya = bulanYangTidakAdaDendanya;
	}

	/**
	 * Daftar NAMA kelas sasaran (bukan id kelas), alternatif dari
	 * {@link #getKelasSiswa()} untuk menyasar banyak rombongan belajar sekaligus.
	 *
	 * <p><b>Getter destruktif.</b> Method ini MENULIS BALIK ke field sebelum mengembalikan nilai; pada entity yang masih terikat sesi Hibernate, perubahan itu ikut ter-flush ke basis data beserta satu revisi Envers.</p>
	 *
	 * <p>Dinormalkan ke format berkoma {@code ,a,b,} seperti kolom teks berkoma lainnya. Nilai
	 * tidak kosong juga MEMAKSA {@link #getTahunAngkatan()} menjadi {@code 0}.</p>
	 *
	 * <p><b>Kuirk yang perlu diketahui.</b> Karena pencocokannya berdasarkan NAMA kelas, sub-kueri
	 * pemilih siswa di {@code DetailTagihanSiswaHelper} mencari {@code kelasSiswa.nama} yang cocok
	 * dengan tambahan syarat {@code kelasSiswa.tahunAjaran} sama &mdash; TANPA penyaring sekolah.
	 * Nama kelas seperti "7A" atau "XII IPA 1" lazim dipakai banyak sekolah sekaligus, sehingga
	 * daftar id antara yang dihasilkan bisa memuat siswa sekolah lain. Kebocoran nyata dicegah
	 * oleh penyaring {@code Restrictions.eq("sekolah", ...)} pada kueri LUAR &mdash; yang berlaku
	 * selama {@link #getKhususBuatSiswaTertentu()} bernilai {@code false}. Dampak yang tersisa
	 * adalah beban kueri, bukan salah sasaran.</p>
	 *
	 * @return daftar nama kelas dalam format {@code ,a,b,}; string kosong bila tidak dipakai
	 */
	@Column(columnDefinition = "text")
	public String getKelasBanyak() {
		kelasBanyak = (kelasBanyak == null || kelasBanyak.trim().equalsIgnoreCase(",") ? ""
				: "," + kelasBanyak.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (kelasBanyak.equals(",")) {
			kelasBanyak = "";
		} else if (kelasBanyak.equals(",,")) {
			kelasBanyak = "";
		} else if (kelasBanyak.equals(",,,")) {
			kelasBanyak = "";
		}
		return kelasBanyak;
	}

	/**
	 * @param kelasBanyak daftar NAMA kelas berkoma; dinormalkan oleh getternya
	 */
	public void setKelasBanyak(String kelasBanyak) {
		this.kelasBanyak = kelasBanyak;
	}

}
