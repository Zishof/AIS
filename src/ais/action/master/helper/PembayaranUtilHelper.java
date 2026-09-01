package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.ws.util.CommonUtil;
import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AfiliasiCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.JenisTinggalMahasiswa;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Paket;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;

/**
 * Helper statis (tanpa state instance) yang menghitung <b>tagihan/billing pembayaran mahasiswa
 * dan calon mahasiswa</b> di AIS — jantung dari modul keuangan akademik: penentuan item biaya apa
 * saja yang harus dibayar seorang mahasiswa/calon mahasiswa untuk suatu {@link JenisKegiatan}
 * (mis. her-registrasi, SPP, her-her, pendaftaran ulang mahasiswa baru) pada semester tertentu,
 * baik dalam mode tagihan reguler ({@link DetailBiaya}) maupun mode angsuran/cicilan bulanan
 * ({@link PengaturanPembayaranBulanan}).
 *
 * <p><b>Entity Hibernate utama yang dipakai:</b> {@link DetailBiaya} (baris tagihan/setting biaya),
 * {@link ItemBiaya} (jenis item biaya, mis. SPP/her-her), {@link PengaturanPembayaranBulanan}
 * (satu baris cicilan bulanan yang mengacu ke {@link DetailBiaya}), {@link CicilanPembayaran}
 * (riwayat pembayaran/cicilan yang sudah dibuat untuk satu {@link ais.database.model.Kegiatan}),
 * {@link Mahasiswa} dan {@link BiodataCalonMahasiswa} (subjek tagihan), serta {@link Konfigurasi}
 * yang dibaca berulang kali via {@code Common.getKonfigurasi(...)} untuk menyalakan/mematikan
 * banyak perilaku opsional (filter kelas, filter tempat tinggal, status non-aktif dianggap aktif,
 * dsb). Query dibangun dengan Hibernate {@link Criteria}/{@link Restrictions}, sebagian memakai
 * {@code Restrictions.sqlRestriction(...)} untuk klausa SQL mentah (mis. filter
 * {@code (realbulan, item_biaya) not in (...)} pada tagihan bulanan yang sudah dibayar).</p>
 *
 * <p><b>Alur inti (lihat {@link #getDetailBiayaMahasiswadariDatabase}):</b> (1) short-circuit bila
 * mahasiswa ditandai {@code tidakAdaTagihan}, pindahan yang belum masuk semester ini, atau sudah
 * lulus sebelum semester yang diminta; (2) hasil query di-cache per mahasiswa lewat mekanisme
 * key-value generik {@link GeneralValueObject}/{@code mahasiswa.retreive(key)}/{@code mahasiswa.put(...)}
 * dengan kunci berisi id mahasiswa+jenisKegiatan+semester+bulan, sehingga panggilan berikutnya
 * dengan {@code reload=false} tidak perlu query ulang; (3) cek dulu "Setting Biaya khusus" per NIM
 * lewat {@code SetingBiayaHelper.getDetailBiayaDefault(...)} — bila NIM termasuk daftar pengecualian
 * tagihan ({@link PengecualianTagihanList}), koleksi kosong sentinel dikembalikan (BUKAN "tidak ada
 * tagihan", tapi "sengaja dikecualikan"); (4) bila jenis kegiatan dikonfigurasi mode angsuran untuk
 * jenjang/semester/angkatan mahasiswa ({@code JenisKegiatan.modeAngsuranUntukJenjang(...)}), jalur
 * dialihkan ke query {@link PengaturanPembayaranBulanan} (tagihan per bulan) alih-alih
 * {@link DetailBiaya} biasa; (5) hasil disaring lagi lewat berbagai kombinasi kriteria: jenjang,
 * jurusan, program, jenis kelamin, status mahasiswa/status awal, kewarganegaraan, kelas, jenis
 * tempat tinggal, gelombang pendaftaran, paket, dan parameter tambahan kustom (lihat
 * {@link #filterCriteriaDenganNilaiTambahan}).</p>
 *
 * <p>Class ini murni kumpulan method {@code static} tanpa field instance — tidak pernah
 * di-instansiasi (tidak ada constructor eksplisit, hanya constructor default implisit). Dua
 * konstanta {@code SQL_TRUE}/{@code SQL_FALSE} dipakai sebagai klausa {@code sqlRestriction} netral
 * agar builder criteria bisa menambahkan kondisi opsional tanpa percabangan if/else terpisah untuk
 * tiap kombinasi. <b>Kuirk yang perlu diketahui:</b> method privat {@link #jenjangCocok} dideklarasikan
 * lengkap dengan logikanya tapi <b>tidak pernah dipanggil</b> di mana pun dalam class ini (dead code
 * peninggalan refactor) — jangan berasumsi ia aktif menyaring apa pun.</p>
 *
 * <p><b>Efek samping:</b> sebagian besar method di sini adalah pembacaan (query Hibernate lewat
 * {@link Session} baru yang selalu ditutup lewat {@link #closeOpenedSession}), namun beberapa method
 * (mis. {@link #fallbackTagihanDariCicilan}) melakukan <i>write</i> nyata ke DB — memperbaiki
 * {@code DetailBiaya.nilaiBiaya} dan {@code PengaturanPembayaranBulanan.nominal} yang bernilai 0
 * berdasarkan riwayat {@link CicilanPembayaran} — dalam transaksi lokalnya sendiri. Pemanggil baru
 * sebaiknya memakai method yang sudah ada di sini (dipanggil a.l. dari action Daftar Ulang Mahasiswa
 * dan alur billing lain) daripada menulis ulang query criteria yang sama di tempat lain.</p>
 */
public class PembayaranUtilHelper {

	private static final String SQL_TRUE = "1=1";
	private static final String SQL_FALSE = "1=0";

	/**
	 * Mengecek apakah {@link Jenjang} mahasiswa/calon mahasiswa termasuk daftar id jenjang yang
	 * tersimpan pada array JSON {@code jenjangAngsuranJson[key]} (format setting angsuran per
	 * jenjang). Array kosong atau JSON kosong/tidak valid dianggap "cocok untuk semua jenjang"
	 * (mengembalikan {@code true}); bila array berisi tapi jenjang mahasiswa tidak diketahui,
	 * dianggap tidak cocok. <b>Catatan:</b> method ini tidak dipanggil dari mana pun di dalam
	 * class ini saat ini (dead code).
	 *
	 * @param jenjangAngsuranJson string JSON berisi peta {@code key -> array id jenjang}
	 * @param key nama field pada JSON yang berisi daftar id jenjang yang dicek
	 * @param mahasiswa sumber jenjang bila subjeknya mahasiswa aktif (diutamakan bila keduanya diisi)
	 * @param biodataCalonMahasiswa sumber jenjang alternatif bila subjeknya calon mahasiswa
	 * @return {@code true} bila jenjang cocok/tidak ada batasan, {@code false} bila tidak cocok
	 */
	private static boolean jenjangCocok(String jenjangAngsuranJson, String key,
			Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		if (jenjangAngsuranJson == null || jenjangAngsuranJson.trim().isEmpty()) return true;
		try {
			JSONArray arr = new JSONObject(jenjangAngsuranJson).optJSONArray(key);
			if (arr == null || arr.length() == 0) return true;
			Jenjang jenjang = mahasiswa != null ? mahasiswa.getJenjang()
					: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getJenjang() : null);
			if (jenjang == null || jenjang.getId() == null) return false;
			String jenjangId = String.valueOf(jenjang.getId());
			for (int i = 0; i < arr.length(); i++) {
				if (jenjangId.equals(arr.getString(i))) return true;
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:72");}
		return false;
	}

	/**
	 * Menutup {@link Session} Hibernate lokal dengan aman: {@code clear()} lalu
	 * {@code disconnect()} lalu {@code close()}, masing-masing dibungkus try/catch terpisah
	 * (kegagalan pada satu langkah tidak menghalangi langkah berikutnya) dan hanya dijalankan
	 * bila session masih {@code isOpen()}. Dipanggil dari blok {@code finally} tiap method yang
	 * membuka session sendiri di class ini. Tidak melakukan apa-apa bila {@code session == null}.
	 *
	 * @param session session yang akan ditutup; boleh {@code null}
	 */
	private static void closeOpenedSession(Session session) {
		if (session != null) {
			try {
				if (session.isOpen()) {
					session.clear();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:82");
			}
			try {
				if (session.isOpen()) {
					session.disconnect();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:88");
			}
			try {
				if (session.isOpen()) {
					session.close();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:94");
			}
		}
	}

	/**
	 * Bentuk paling ringkas untuk mengambil tagihan reguler (non-bulanan) satu mahasiswa: mendelegasikan
	 * ke {@link #getDetailBiayaMahasiswa(Mahasiswa, Integer, JenisKegiatan, String, boolean)} dengan
	 * {@code bulan = null}.
	 *
	 * @param mahasiswa mahasiswa subjek tagihan
	 * @param semester semester akademik yang tagihannya dicari (bukan semester kalender)
	 * @param jenisKegiatan jenis kegiatan/tagihan (mis. SPP, her-registrasi); boleh {@code null} untuk semua jenis
	 * @param reload {@code true} untuk memaksa query ulang ke DB melewati cache per-mahasiswa
	 * @return koleksi {@link DetailBiaya} dan/atau {@link PengaturanPembayaranBulanan} yang berlaku
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Collection getDetailBiayaMahasiswa(Mahasiswa mahasiswa, Integer semester, JenisKegiatan jenisKegiatan,
			boolean reload) {
		return getDetailBiayaMahasiswa(mahasiswa, semester, jenisKegiatan, null, reload);
	}

	/**
	 * Sama seperti {@link #getDetailBiayaMahasiswa(Mahasiswa, Integer, JenisKegiatan, boolean)}
	 * tetapi bisa membatasi hasil pada satu bulan tagihan (untuk mode angsuran bulanan); tagihan
	 * bulan yang sudah dibayar tetap DISARING KELUAR (perilaku default, lihat overload berikutnya).
	 *
	 * @param mahasiswa mahasiswa subjek tagihan
	 * @param semester semester akademik yang tagihannya dicari
	 * @param jenisKegiatan jenis kegiatan/tagihan
	 * @param bulan nomor bulan (sebagai string) untuk tagihan bulanan, atau {@code null}/kosong untuk tagihan reguler
	 * @param reload {@code true} untuk memaksa query ulang ke DB melewati cache per-mahasiswa
	 * @return koleksi {@link DetailBiaya} dan/atau {@link PengaturanPembayaranBulanan} yang berlaku
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Collection getDetailBiayaMahasiswa(Mahasiswa mahasiswa, Integer semester, JenisKegiatan jenisKegiatan,
			String bulan, boolean reload) {
		return getDetailBiayaMahasiswa(mahasiswa, semester, jenisKegiatan, bulan, false, reload);
	}

	/**
	 * Titik masuk publik utama untuk mengambil tagihan pembayaran seorang mahasiswa; hanya
	 * meneruskan seluruh parameter apa adanya ke implementasi nyata di
	 * {@link #getDetailBiayaMahasiswadariDatabase}. Lihat method tersebut untuk penjelasan lengkap
	 * alur (cache, pengecualian NIM, mode angsuran, filter status/jenjang/dst).
	 *
	 * @param mahasiswa mahasiswa subjek tagihan
	 * @param semester semester akademik yang tagihannya dicari
	 * @param jenisKegiatan jenis kegiatan/tagihan
	 * @param bulan nomor bulan (sebagai string) untuk tagihan bulanan, atau {@code null}/kosong untuk tagihan reguler
	 * @param untukBulananTampilkanMeskipunSudahDibayar {@code true} untuk tetap menampilkan baris
	 *        bulanan yang sudah dibayar (dipakai layar riwayat), {@code false} untuk menyaringnya (default UI billing)
	 * @param reload {@code true} untuk memaksa query ulang ke DB melewati cache per-mahasiswa
	 * @return koleksi {@link DetailBiaya} dan/atau {@link PengaturanPembayaranBulanan} yang berlaku
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Collection getDetailBiayaMahasiswa(Mahasiswa mahasiswa, Integer semester, JenisKegiatan jenisKegiatan,
			String bulan, Boolean untukBulananTampilkanMeskipunSudahDibayar, boolean reload) {
		Collection d = getDetailBiayaMahasiswadariDatabase(mahasiswa, semester, jenisKegiatan, bulan,
				untukBulananTampilkanMeskipunSudahDibayar, reload);
		return d;
	}

	/**
	 * Varian yang menghitung otomatis semester akademik mahasiswa saat ini (ganjil/genap berjalan,
	 * memperhitungkan mahasiswa pindahan lewat {@code pindahKeKampusIniMasukSemester} dan
	 * {@code semesterMulai}) sebelum mendelegasikan ke
	 * {@link #getDetailBiayaMahasiswa(Mahasiswa, Integer, JenisKegiatan, String, boolean)}. Dipakai
	 * saat pemanggil tidak (atau belum) tahu semester eksplisit mahasiswa.
	 *
	 * @param mahasiswa mahasiswa subjek tagihan
	 * @param jenisKegiatan jenis kegiatan/tagihan
	 * @param bulan nomor bulan (sebagai string) untuk tagihan bulanan, atau {@code null}/kosong untuk tagihan reguler
	 * @param reload {@code true} untuk memaksa query ulang ke DB melewati cache per-mahasiswa
	 * @return koleksi {@link DetailBiaya} dan/atau {@link PengaturanPembayaranBulanan} yang berlaku
	 */
	@SuppressWarnings("rawtypes")
	public static Collection getDetailBiayaMahasiswaBerdasarkanJenisKegiatan(Mahasiswa mahasiswa, JenisKegiatan jenisKegiatan,
			String bulan, boolean reload) {
		Boolean ganjil = CommonUtil.isNowSemensterGanjil();
		Integer semester = CommonUtil.getSemester(mahasiswa.getTahunangkatan(), ganjil,
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
		return PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester, jenisKegiatan, bulan, reload);
	}

	/**
	 * Varian yang menurunkan semester akademik dari sebuah {@link JadwalPembayaran} (jadwal yang
	 * menentukan tahun akademik dan ganjil/genap eksplisit) alih-alih dari semester berjalan
	 * kalender, lalu mendelegasikan ke
	 * {@link #getDetailBiayaMahasiswa(Mahasiswa, Integer, JenisKegiatan, String, boolean)} dengan
	 * {@link JenisKegiatan} yang melekat pada jadwal tersebut.
	 *
	 * @param mahasiswa mahasiswa subjek tagihan
	 * @param jadwalPembayaran jadwal yang menentukan tahun akademik/ganjil-genap dan jenis kegiatan
	 * @param bulan nomor bulan (sebagai string) untuk tagihan bulanan, atau {@code null}/kosong untuk tagihan reguler
	 * @param reload {@code true} untuk memaksa query ulang ke DB melewati cache per-mahasiswa
	 * @return koleksi {@link DetailBiaya} dan/atau {@link PengaturanPembayaranBulanan} yang berlaku
	 */
	@SuppressWarnings("rawtypes")
	public static Collection getDetailBiayaMahasiswa(Mahasiswa mahasiswa, JadwalPembayaran jadwalPembayaran, String bulan,
			boolean reload) {
		Boolean ganjil = jadwalPembayaran.getGanjil() == null ? Common.isNowSemensterGanjil() : jadwalPembayaran.getGanjil();
		Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), jadwalPembayaran.getTahunAkademik(),
				Boolean.TRUE.equals(ganjil) ? Perkuliahan.GANJIL : Perkuliahan.GENAP, mahasiswa.getPindahKeKampusIniMasukSemester(),
				mahasiswa.getSemesterMulai());
		return PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester, jadwalPembayaran.getJenisKegiatan(), bulan, reload);
	}

	/**
	 * Mengambil daftar {@link DetailKegiatan} (baris kegiatan/aktivitas yang diikuti seorang
	 * mahasiswa atau calon mahasiswa) dari DB via query {@link Criteria} langsung (tanpa cache),
	 * opsional disaring per {@link JenisKegiatan}. Bila {@code mahasiswa} dan {@code calonMahasiswa}
	 * diisi berdua, keduanya digabung dengan OR (baris milik salah satu ikut); bila hanya satu yang
	 * diisi, hanya baris milik subjek itu yang diambil. Membuka dan selalu menutup {@link Session}
	 * miliknya sendiri.
	 *
	 * @param mahasiswa mahasiswa subjek pencarian, boleh {@code null}
	 * @param calonMahasiswa calon mahasiswa subjek pencarian, boleh {@code null}
	 * @param jenisKegiatan penyaring jenis kegiatan; {@code null} berarti semua jenis
	 * @return daftar {@link DetailKegiatan} yang cocok, atau list kosong bila terjadi error (dicatat via {@code ErrorAuditUtil})
	 */
	@SuppressWarnings("unchecked")
	public static List<DetailKegiatan> getDetailKegiatanMahasiswa(Mahasiswa mahasiswa, BiodataCalonMahasiswa calonMahasiswa,
			JenisKegiatan jenisKegiatan) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Criteria criteria = session.createCriteria(DetailKegiatan.class).createCriteria("kegiatan");
			if (jenisKegiatan != null) {
				criteria.add(Restrictions.eq("jenisKegiatan", jenisKegiatan));
			}
			if (mahasiswa != null && calonMahasiswa != null) {
				criteria.add(Restrictions.or(Restrictions.eq("mahasiswa", mahasiswa),
						Restrictions.eq("calonMahasiswa", calonMahasiswa)));
			} else if (mahasiswa != null) {
				criteria.add(Restrictions.eq("mahasiswa", mahasiswa));
			} else if (calonMahasiswa != null) {
				criteria.add(Restrictions.eq("calonMahasiswa", calonMahasiswa));
			}
			List<DetailKegiatan> detailBiaya = criteria.list();
			return detailBiaya;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PembayaranUtilHelper.java:159");
			return new ArrayList<DetailKegiatan>();
		} finally {
			closeOpenedSession(session);
		}
	}

	/**
	 * Implementasi nyata (inti) dari perhitungan tagihan pembayaran seorang mahasiswa — semua
	 * overload {@code getDetailBiayaMahasiswa(...)} akhirnya memanggil method ini. Alur ringkas:
	 * <ol>
	 * <li>Short-circuit koleksi kosong bila mahasiswa ditandai {@code tidakAdaTagihan}, belum masuk
	 * semester pindahannya, atau sudah lulus sebelum semester yang diminta (kecuali
	 * {@code jenisKegiatan.getTagihanJugaUntukAlumni()} bernilai true).</li>
	 * <li>Bila {@code reload == false}: coba ambil dari cache per-mahasiswa (kunci berisi
	 * id mahasiswa+jenisKegiatan+semester+bulan, disimpan via {@code mahasiswa.put/retreive} dan
	 * dideserialisasi lewat {@link GeneralValueObject}); cache dibuang bila ternyata berisi baris
	 * dengan semester berbeda dari yang diminta (data basi).</li>
	 * <li>Tentukan {@code statusMahasiswa} efektif (bisa dipaksa AKTIF oleh beberapa
	 * {@link Konfigurasi}: status non-aktif/non-lulus/kampus-merdeka boleh membayar seperti aktif,
	 * atau karena sedang cuti disetujui) dan tahun akademik/{@code ta} (kode tahun+semester numerik).</li>
	 * <li>Cek "Setting Biaya khusus" per NIM lewat {@code SetingBiayaHelper.getDetailBiayaDefault}
	 * SEBELUM guard mode angsuran (perbaikan agar setting khusus tidak pernah lolos ke jalur
	 * bulanan tanpa sempat dibaca) — bila NIM masuk {@link PengecualianTagihanList}, kembalikan
	 * sentinel kosong pengecualian (bukan "tidak ada tagihan").</li>
	 * <li>Bila jenis kegiatan memakai mode angsuran untuk jenjang/semester/angkatan mahasiswa ini
	 * (dan tidak ada setting biaya khusus), kembalikan koleksi kosong biasa agar pemanggil beralih
	 * ke jalur bulanan (bukan sentinel pengecualian).</li>
	 * <li>Query {@link Criteria} multi-kondisi ke {@link DetailBiaya} (atau ke
	 * {@link PengaturanPembayaranBulanan} bila {@code bulan} diisi angka), disaring lewat
	 * {@link #filterCriteriaDenganNilaiTambahan} dan berbagai atribut mahasiswa (kelas, jenis
	 * tempat tinggal, tahun akademik, status, jenjang, jurusan, angkatan, dst).</li>
	 * <li>Hasil di-dedup per {@code itemBiaya} (baris dengan id terbesar yang menang) memakai
	 * {@link TreeSet}, dilengkapi keterangan via {@code DetailBiaya.updateKeterangan(...)}, ditulis
	 * ke cache, lalu dikembalikan.</li>
	 * </ol>
	 * Membuka {@link Session} sendiri dan selalu menutupnya di {@code finally}; error tak terduga
	 * dicatat lewat {@code ErrorAuditUtil}/{@code printStackTrace} dan mengembalikan koleksi kosong
	 * (bukan melempar exception ke pemanggil).
	 *
	 * @param mahasiswa mahasiswa subjek tagihan; {@code null} langsung mengembalikan koleksi kosong
	 * @param semester semester akademik yang tagihannya dihitung
	 * @param jenisKegiatan jenis kegiatan/tagihan yang dicari
	 * @param bulan nomor bulan (string angka) untuk memfilter/menghitung tagihan bulanan, {@code null}/kosong untuk tagihan reguler
	 * @param untukBulananTampilkanMeskipunSudahDibayar bila {@code true}, baris bulanan yang sudah lunas tetap disertakan
	 * @param reload {@code true} untuk memaksa query ulang ke DB dan mengabaikan cache per-mahasiswa
	 * @return koleksi {@link DetailBiaya} dan/atau {@link PengaturanPembayaranBulanan} yang berlaku bagi mahasiswa ini
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Collection getDetailBiayaMahasiswadariDatabase(Mahasiswa mahasiswa, Integer semester,
			JenisKegiatan jenisKegiatan, String bulan, boolean untukBulananTampilkanMeskipunSudahDibayar,
			boolean reload) {

		if (mahasiswa != null && mahasiswa.getTidakAdaTagihan() != null && mahasiswa.getTidakAdaTagihan()) {
			return new TreeSet();
		}

		if (mahasiswa != null && mahasiswa.getPindahKeKampusIniMasukSemester() != null 
				&& mahasiswa.getPindahKeKampusIniMasukSemester() > 0
				&& semester != null && mahasiswa.getPindahKeKampusIniMasukSemester() > semester) {
			return new TreeSet();
		}

		if (semester != null && mahasiswa != null && mahasiswa.getStatusKeluar() != null
				&& ((mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus() < semester))) {
			if (jenisKegiatan != null && !Boolean.TRUE.equals(jenisKegiatan.getTagihanJugaUntukAlumni())) {
				return new TreeSet();
			}
		}

		String bulanKey = (bulan == null || bulan.trim().isEmpty()) ? "" : "_" + bulan.trim();
		String key = "tagihan_mhs_" + (mahasiswa != null ? mahasiswa.getId() : "null") + "_" 
				+ (jenisKegiatan != null ? jenisKegiatan.getId() : "null") + "_" + semester
				+ bulanKey + "_" + (untukBulananTampilkanMeskipunSudahDibayar ? "semua" : "belum_dibayar")
				+ "_aktif_tagihan_v2";

		if (!reload && mahasiswa != null) {
			try {
				String s = mahasiswa.retreive(key);
				JSONObject data = s == null || s.trim().isEmpty() ? null : new JSONObject(s);
				if (data != null) {
					boolean smtSalah = false;
					List d = new ArrayList();
					Iterator<String> iter = data.keys();
					while (iter.hasNext()) {
						try {
							String keyIter = iter.next();
							String value = data.get(keyIter).toString();
							if (value.equalsIgnoreCase("1")) {
								DetailBiaya detailBiaya1 = (DetailBiaya) GeneralValueObject.ambilData(DetailBiaya.class, keyIter, true);
								
								if (detailBiaya1 != null) {
									detailBiaya1.updateKeterangan(mahasiswa, semester);
									d.add(detailBiaya1);

									if (detailBiaya1.getSemester() != null && !detailBiaya1.getSemester().equals(semester)) {
										smtSalah = true;
										break;
									}
								}
							} else if (value.equalsIgnoreCase("2")) {
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) GeneralValueObject
										.ambilData(PengaturanPembayaranBulanan.class, keyIter, true);

								if (pengaturanPembayaranBulanan != null) {
									d.add(pengaturanPembayaranBulanan);
									if (pengaturanPembayaranBulanan.getDetailBiaya() != null 
											&& pengaturanPembayaranBulanan.getDetailBiaya().getSemester() != null
											&& !pengaturanPembayaranBulanan.getDetailBiaya().getSemester().equals(semester)) {
										smtSalah = true;
										break;
									}
								}
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:232");}
					}

					try {
						Collections.sort(d);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:237");}

					if (!smtSalah) {
						return d;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:243");}
		}

		if (mahasiswa == null) return new TreeSet();

		Jurusan jurusan = mahasiswa.getJurusan();
		Jenjang jenjang = jurusan != null ? jurusan.getJenjang() : mahasiswa.getJenjang();

		Integer angkatan = mahasiswa.getTahunangkatan();
		String warganegara = mahasiswa.getWarganegara();

		Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
		Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, semesterMulai, mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		Integer tahap = PengaturanPembayaranBulanan.hitungTahap(mahasiswa, semester, Common.BULAN[ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH)]);
		String mulaiBelajarDiSemester = mahasiswa.getSemesterMulai();
		
		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahap,
				jenisKegiatan != null && Boolean.TRUE.equals(jenisKegiatan.getUntukBayarSP()) ? Perkuliahan.SEMESTER_PENDEK : null, reload);

		StatusMahasiswa statusMahasiswa = null;
		if (ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa) != null) {
			statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa).getStatusMahasiswa();
		}

		HistoryStatusMahasiswa tempHistoryStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa, reload);
		String program = tempHistoryStatusMahasiswa != null ? tempHistoryStatusMahasiswa.getProgram() : mahasiswa.getProgram();
		String kelamin = mahasiswa.getKelamin();
		StatusAwalMahasiswa statusAwalMahasiswa = tempHistoryStatusMahasiswa != null ? tempHistoryStatusMahasiswa.getStatusAwalMahasiswa() : null;

		if (((statusMahasiswa != null && ConstantValues.LULUS != null && ConstantValues.LULUS.getId().equals(statusMahasiswa.getId())) || mahasiswa.getStatusKeluar() != null)
				&& mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus().equals(semester)) {
			statusMahasiswa = ConstantValues.AKTIF;
		}

		PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap, false);
		if (pendaftaranCutiMahasiswa != null && Boolean.TRUE.equals(pendaftaranCutiMahasiswa.getPersetujuan())) {
			statusMahasiswa = ConstantValues.CUTI;
		}

		try {
			Konfigurasi k1 = Common.getKonfigurasi("mahasiswa_dengan_status_non_aktif_bisa_melakukan_pembayaran_seperti_status_aktif", Konfigurasi.AKTIF);
			if (k1 != null && Konfigurasi.AKTIF.equals(k1.getNilai())) {
				if (statusMahasiswa == null || statusMahasiswa.getId().equals(ConstantValues.TIDAK_AKTIF.getId())) {
					statusMahasiswa = ConstantValues.AKTIF;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:292");}

		try {
			Konfigurasi k2 = Common.getKonfigurasi("mahasiswa_dengan_status_non_lulus_bisa_melakukan_pembayaran_seperti_status_aktif", Konfigurasi.TIDAK_AKTIF);
			if (k2 != null && Konfigurasi.AKTIF.equals(k2.getNilai())) {
				if (statusMahasiswa == null || statusMahasiswa.getId().equals(ConstantValues.LULUS.getId())) {
					statusMahasiswa = ConstantValues.AKTIF;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:301");}

		try {
			Konfigurasi k3 = Common.getKonfigurasi("mahasiswa_dengan_status_kampus_merdeka_bisa_melakukan_pembayaran_seperti_status_aktif", Konfigurasi.AKTIF);
			if (k3 != null && Konfigurasi.AKTIF.equals(k3.getNilai())) {
				if (ConstantValues.KAMPUS_MERDEKA != null && (statusMahasiswa == null || statusMahasiswa.getId().equals(ConstantValues.KAMPUS_MERDEKA.getId()))) {
					statusMahasiswa = ConstantValues.AKTIF;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:310");}

		String filterKelas = "TIDAK AKTIF";
		try {
			Konfigurasi fk = Common.getKonfigurasi("tampilkan_filter_kelas_pada_billing_pembayaran", Konfigurasi.TIDAK_AKTIF);
			if (fk != null) filterKelas = fk.getNilai();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:316");}

		String filterJenisTempatTinggalMahasiswa = "TIDAK AKTIF";
		try {
			Konfigurasi fj = Common.getKonfigurasi("tampilkan_filter_jenis_tempat_tinggal_mahasiswa_pada_billing_pembayaran", Konfigurasi.TIDAK_AKTIF);
			if (fj != null) filterJenisTempatTinggalMahasiswa = fj.getNilai();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:322");}

		String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0" : tahunAkademik.split("/")[0])
				+ (semester == null ? "0" : (semester % 2 == 0) ? "2" : "1");
		Integer ta = 0;
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:329");}

		Session session = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			// Fresh reload untuk hindari stale cache dari combobox lama
			if (jenisKegiatan != null && jenisKegiatan.getId() != null) {
				try {
					JenisKegiatan freshJk = (JenisKegiatan) session.get(JenisKegiatan.class, jenisKegiatan.getId());
					if (freshJk != null) jenisKegiatan = freshJk;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:341");}
			}

			/*
			 * Setting biaya khusus mahasiswa harus diperiksa SEBELUM guard mode
			 * bulanan/angsuran. Sebelumnya guard langsung mengembalikan koleksi kosong,
			 * sehingga setting khusus (yang memang memakai nilai/default tanggal pada
			 * Setting Biaya) tidak pernah sempat dibaca untuk jenis kegiatan bulanan.
			 */
			List<DetailBiaya> biayaDefault = SetingBiayaHelper.getDetailBiayaDefault(session,
					mahasiswa, jenisKegiatan, semester, ta);
			if (PengecualianTagihanList.adalah(biayaDefault)) {
				return PengecualianTagihanList.kosong();
			}

			if (bulan == null && jenisKegiatan != null) {
				// Per-jenjang PER-SEMESTER (dan per-angkatan bila diisi format TAHUN:SMT):
				// aturan angsuran hanya mengenai semester/angkatan yang masuk daftar
				// "Berlaku di smt" (kosong = semua) pada form Jenis Kegiatan.
				Boolean modeAngsuran = jenisKegiatan.modeAngsuranUntukJenjang(jenjang, semester,
						mahasiswa == null ? null : mahasiswa.getTahunangkatan());
				if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
						"[DEBUG-ANGSURAN][getDetailBiaya] mhs=" + (mahasiswa != null ? mahasiswa.getNim() : "null")
						+ " jk=" + jenisKegiatan.getNama() + " jenjang=" + (jenjang != null ? jenjang.getNama() : "null")
						+ " bulan=" + bulan + " modeAngsuran=" + modeAngsuran);
				if (Boolean.TRUE.equals(modeAngsuran)
						&& (biayaDefault == null || biayaDefault.isEmpty())) {
					if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
							"[DEBUG-ANGSURAN][getDetailBiaya] → TRUE tanpa setting khusus: return empty (lanjut jalur angsuran bulanan)");
					/*
					 * Ini bukan pengecualian NIM. Koleksi kosong biasa memberi tahu pemanggil
					 * agar tagihan dilayani oleh jalur angsuran/bulanan. Sentinel
					 * PengecualianTagihanList hanya boleh dipakai untuk NIM yang benar-benar
					 * tercantum pada daftar pengecualian Setting Biaya.
					 */
					return new TreeSet();
				}
				if (Boolean.TRUE.equals(modeAngsuran) && biayaDefault != null && !biayaDefault.isEmpty()
						&& JenisKegiatan.DEBUG_MODE_ANGSURAN) {
					System.out.println("[DEBUG-ANGSURAN][getDetailBiaya] → TRUE tetapi setting khusus mahasiswa ditemukan: proses setting khusus");
				}
				// FALSE (bukan angsuran) atau null → lanjut query billing reguler
				if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
						"[DEBUG-ANGSURAN][getDetailBiaya] → " + modeAngsuran + ": lanjut query billing reguler");
			}

			AfiliasiCalonMahasiswa afiliasiCalonMahasiswa = null;

			if (biayaDefault == null || biayaDefault.isEmpty()) {
				Paket paket = null;
				try {
					BiodataCalonMahasiswa biodataCalonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();
					if (biodataCalonMahasiswa != null) {
						paket = biodataCalonMahasiswa.getPaket();
						afiliasiCalonMahasiswa = biodataCalonMahasiswa.getAfiliasiCalonMahasiswa();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:371");}

				biayaDefault = SetingBiayaHelper.getDetailBiayaDefault(session, angkatan, jenjang, semester, jenisKegiatan,
						statusAwalMahasiswa, statusMahasiswa, mahasiswa.getJenisSeleksi(),
						mahasiswa.getGelombangPendaftaran(), paket, jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
						mahasiswa.getNim());
				if (PengecualianTagihanList.adalah(biayaDefault)) {
					return new TreeSet();
				}
			}

			if (JenisKegiatan.DEBUG_MODE_ANGSURAN) {
				System.out.println("[DEBUG-ANGSURAN][getDetailBiaya] biayaDefault size=" + (biayaDefault != null ? biayaDefault.size() : 0)
						+ " mhs=" + mahasiswa.getNim() + " jenjang=" + (jenjang != null ? jenjang.getNama() : "null"));
				if (biayaDefault != null) {
					for (DetailBiaya db : biayaDefault) {
						System.out.println("[DEBUG-ANGSURAN][getDetailBiaya]   biayaDefault item id=" + db.getId()
								+ " item=" + (db.getItemBiaya() != null ? db.getItemBiaya().getNama() : "null")
								+ " nilaibiaya=" + db.getNilaiBiaya()
								+ " jenjang=" + (db.getJenjang() != null ? db.getJenjang().getNama() : "null")
								+ " jurusan=" + (db.getJurusan() != null ? db.getJurusan().getNama() : "null"));
					}
				}
			}

			if (biayaDefault != null && !biayaDefault.isEmpty()) {
				for (DetailBiaya detailBiaya : biayaDefault) {
					detailBiaya.updateKeterangan(mahasiswa, semester);
				}

				JSONObject data = new JSONObject();
				for (DetailBiaya detailBiaya : biayaDefault) {
					try {
						data.put(detailBiaya.getId().toString(), "1");
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:401");}
					GeneralValueObject.masukkanData(DetailBiaya.class, detailBiaya);
				}
				mahasiswa.put(data.toString(), key);

				if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
						"[DEBUG-ANGSURAN][getDetailBiaya] EARLY RETURN via biayaDefault (" + biayaDefault.size() + " items)");
				return biayaDefault;
			}

			String kelasStr = null;
			if (Konfigurasi.AKTIF.equals(filterKelas)) {
				kelasStr = mahasiswa.getKelas();
			}

			JenisTinggalMahasiswa jenisTinggalMahasiswa = null;
			if (Konfigurasi.AKTIF.equals(filterJenisTempatTinggalMahasiswa)) {
				jenisTinggalMahasiswa = (JenisTinggalMahasiswa) session.createCriteria(BiodataMahasiswa.class)
						.setProjection(Projections.property("jenisTinggalMahasiswa"))
						.add(Restrictions.eq("mahasiswa", mahasiswa))
						.addOrder(Order.desc("id"))
						.setMaxResults(1)
						.uniqueResult();
			}

			Paket paket = null;
			try {
				BiodataCalonMahasiswa biodataCalonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();
				if (biodataCalonMahasiswa != null) {
					paket = biodataCalonMahasiswa.getPaket();
					afiliasiCalonMahasiswa = biodataCalonMahasiswa.getAfiliasiCalonMahasiswa();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:433");}

			List<ItemBiaya> detailSettingBiayas = SetingBiayaHelper.getItemBiaya(session, angkatan, jenjang, semester,
					jenisKegiatan, statusAwalMahasiswa, statusMahasiswa, mahasiswa.getJenisSeleksi(),
					mahasiswa.getGelombangPendaftaran(), paket, jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
					mahasiswa.getNim());
			if (detailSettingBiayas == null) {
				return PengecualianTagihanList.kosong();
			}

			if (JenisKegiatan.DEBUG_MODE_ANGSURAN) {
				System.out.println("[DEBUG-ANGSURAN][getDetailBiaya] getItemBiaya size="
						+ (detailSettingBiayas != null ? detailSettingBiayas.size() : 0)
						+ " angkatan=" + angkatan + " jenjang=" + (jenjang != null ? jenjang.getNama() : "null")
						+ " smt=" + semester + " statusMhs=" + (statusMahasiswa != null ? statusMahasiswa.getNama() : "null")
						+ " statusAwal=" + (statusAwalMahasiswa != null ? statusAwalMahasiswa.getNama() : "null")
						+ " mulaiBelajar=" + mulaiBelajarDiSemester + " ta=" + ta + " jurusan=" + (jurusan != null ? jurusan.getNama() : "null"));
				if (detailSettingBiayas != null) {
					for (ItemBiaya ib : detailSettingBiayas) {
						System.out.println("[DEBUG-ANGSURAN][getDetailBiaya]   itemBiaya id=" + ib.getId() + " nama=" + ib.getNama());
					}
				}
			}

			Criteria criteria = session.createCriteria(DetailBiaya.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

			if (bulan != null && !bulan.trim().isEmpty() && Common.isNumber(bulan)) {

				List<PengaturanPembayaranBulanan> yangSudahDibayarBulanans = untukBulananTampilkanMeskipunSudahDibayar
						? null
						: session.createCriteria(CicilanPembayaran.class).createAlias("kegiatan", "kegiatan")
								.add(Restrictions.eq("kegiatan.mahasiswa", mahasiswa))
								.add(Restrictions.eq("kegiatan.jenisKegiatan", jenisKegiatan))
								.add(Restrictions.eq("kegiatan.semster", semester))
								.setProjection(Projections.groupProperty("pengaturanPembayaranBulanan"))
								.add(Restrictions.isNotNull("pengaturanPembayaranBulanan")).list();

				StringBuilder sqlQueryBuilder = new StringBuilder();
				sqlQueryBuilder.append("(realbulan,item_biaya) not in (");
				
				StringBuilder sqlBuilder = new StringBuilder();
				if (yangSudahDibayarBulanans != null) {
					boolean isFirst = true;
					for (PengaturanPembayaranBulanan p : yangSudahDibayarBulanans) {
						if (p != null && p.getDetailBiaya() != null && p.getDetailBiaya().getItemBiaya() != null) {
							if (!isFirst) {
								sqlBuilder.append(",");
							}
							sqlBuilder.append("(").append(p.getRealBulan()).append(",").append(p.getDetailBiaya().getItemBiaya().getId()).append(")");
							isFirst = false;
						}
					}
					sqlQueryBuilder.append(sqlBuilder);
				}
				sqlQueryBuilder.append(")");
				
				String sql = sqlBuilder.toString();
				String sqlQuery = sqlQueryBuilder.toString();

				Konfigurasi tagihanKonfig = Common.getKonfigurasi("tagihan_pembayaran_host_to_host_per_bulan_dihitung_berdasarkan_akumulasi_bulanan_yg_belum_dibayar", Konfigurasi.TIDAK_AKTIF);
				if (tagihanKonfig != null && Konfigurasi.AKTIF.equalsIgnoreCase(tagihanKonfig.getNilai())) {

					Integer bln = bulan.trim().equals("-1") ? null
							: (Integer) session.createCriteria(PengaturanPembayaranBulanan.class)
									.add(Restrictions.eq("aktif", true))
									.add(Restrictions.eq("realBulan", Integer.parseInt(bulan.trim())))
									.setProjection(Projections.property("bulan")).setMaxResults(1)
									.addOrder(Order.desc("id")).uniqueResult();

					criteria = session.createCriteria(PengaturanPembayaranBulanan.class)
							.add(Restrictions.eq("aktif", true))
					.add(sql.trim().isEmpty() ? Restrictions.sqlRestriction(SQL_TRUE) : Restrictions.sqlRestriction(sqlQuery))
							.add(bulan.trim().equals("-1") ? Restrictions.sqlRestriction(SQL_TRUE)
									: bln != null ? Restrictions.le("bulan", bln)
											: Restrictions.eq("realBulan", Integer.parseInt(bulan.trim())))
							.createCriteria("detailBiaya");

				} else {
					criteria = session.createCriteria(PengaturanPembayaranBulanan.class)
							.add(Restrictions.eq("aktif", true))
							.add(sql.trim().isEmpty() ? Restrictions.sqlRestriction(SQL_TRUE) : Restrictions.sqlRestriction(sqlQuery))
							.add(bulan.trim().equals("-1") ? Restrictions.sqlRestriction(SQL_TRUE)
									: Restrictions.eq("realBulan", Integer.parseInt(bulan.trim())))
							.createCriteria("detailBiaya");
				}
			}

			filterCriteriaDenganNilaiTambahan(criteria, session, mahasiswa, null);

			if (kelasStr != null) {
				criteria.createAlias("kelas", "kelas").add(Restrictions.eq("kelas.nama", kelasStr));
			} else {
				criteria.add(Restrictions.isNull("kelas"));
			}

			Collection detailBiaya = criteria
					.add(detailSettingBiayas == null || detailSettingBiayas.isEmpty() ? Restrictions.sqlRestriction(SQL_FALSE) : Restrictions.in("itemBiaya", detailSettingBiayas))
					.add(Restrictions.or(Restrictions.eq("merupakanPembayaran", false), Restrictions.isNull("merupakanPembayaran")))
					.addOrder(Order.desc("id"))
					.add(jenisTinggalMahasiswa == null ? Restrictions.isNull("jenisTinggalMahasiswa") : Restrictions.eq("jenisTinggalMahasiswa", jenisTinggalMahasiswa))
					.add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.eq("statusMahasiswa", statusMahasiswa))
					.add(Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))
					.add(Restrictions.eq("mulaiBelajarDiSemester", mulaiBelajarDiSemester))
					.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
					.add(warganegara != null ? Restrictions.ilike("wnaAtauWni", warganegara, MatchMode.EXACT) : Restrictions.sqlRestriction(SQL_TRUE))
					.add(Restrictions.eq("jenjang", jenjang))
					.add(Restrictions.eq("jurusan", jurusan))
					.add(program != null ? Restrictions.ilike("program", program, MatchMode.EXACT) : Restrictions.sqlRestriction(SQL_TRUE))
					.add(Restrictions.eq("semester", semester))
					.add(jenisKegiatan != null ? Restrictions.between("semester", jenisKegiatan.getMinSmt(), jenisKegiatan.getMaxSmt()) : Restrictions.sqlRestriction(SQL_TRUE))
					.add(Restrictions.eq("angkatan", angkatan)).list();

			if (JenisKegiatan.DEBUG_MODE_ANGSURAN) {
				System.out.println("[DEBUG-ANGSURAN][getDetailBiaya] criteria query result size=" + detailBiaya.size()
						+ " tahunAkademik=" + tahunAkademik + " statusMhs=" + (statusMahasiswa != null ? statusMahasiswa.getNama() : "null")
						+ " statusAwal=" + (statusAwalMahasiswa != null ? statusAwalMahasiswa.getNama() : "null")
						+ " mulaiBelajar=" + mulaiBelajarDiSemester + " jurusan=" + (jurusan != null ? jurusan.getNama() : "null")
						+ " angkatan=" + angkatan + " smt=" + semester);
				for (Object item : detailBiaya) {
					if (item instanceof DetailBiaya) {
						DetailBiaya db = (DetailBiaya) item;
						System.out.println("[DEBUG-ANGSURAN][getDetailBiaya]   result item=" + (db.getItemBiaya() != null ? db.getItemBiaya().getNama() : "null")
								+ " nilaibiaya=" + db.getNilaiBiaya() + " jenjang=" + (db.getJenjang() != null ? db.getJenjang().getNama() : "null"));
					}
				}
			}

			List<DetailBiaya> biayaDefaultBiaya = SetingBiayaHelper.getDetailBiayaBukanDefaultBiaya(session, angkatan,
					jenjang, semester, jenisKegiatan, statusAwalMahasiswa, statusMahasiswa, mahasiswa.getJenisSeleksi(),
					mahasiswa.getGelombangPendaftaran(), paket, jurusan, program, kelamin, afiliasiCalonMahasiswa, ta);

			if (biayaDefaultBiaya != null && !biayaDefaultBiaya.isEmpty()) {
				for (DetailBiaya detailBiayaDefault : biayaDefaultBiaya) {
					detailBiayaDefault.updateKeterangan(mahasiswa, semester);
					detailBiaya.add(detailBiayaDefault);
				}
			}


			boolean nolMasukFilter = false;
			try {
				Konfigurasi knol = Common.getKonfigurasi("nol_masuk_filter_pembayaran", Konfigurasi.TIDAK_AKTIF);
				nolMasukFilter = (knol != null && Konfigurasi.AKTIF.equals(knol.getNilai()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:574");}

			if (bulan != null && !bulan.trim().isEmpty() && Common.isNumber(bulan)) {

				List detailBiayaList = (detailBiaya instanceof List) ? (List) detailBiaya : new ArrayList(detailBiaya);
				List<PengaturanPembayaranBulanan> d = saringPengaturanPembayaranBulanan(detailBiayaList, nolMasukFilter, mahasiswa, semester);

				if (d != null) {
					JSONObject data = new JSONObject();
					try {
						for (PengaturanPembayaranBulanan p : d) {
							GeneralValueObject.masukkanData(PengaturanPembayaranBulanan.class, p);
							data.put(p.getId().toString(), "2");
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:588");}
					mahasiswa.put(data.toString(), key);
				}
				
				return d != null ? d : new ArrayList();
				
			} else {

				Map<Long, Long> ids = new HashMap<Long, Long>();
				Map<Long, Object> maps = new HashMap<Long, Object>();
				
				for (Object o : detailBiaya) {
					try {
						if (o instanceof DetailBiaya) {
							DetailBiaya biaya = (DetailBiaya) o;
							Long value = ids.get(biaya.getItemBiaya().getId());
							if (value == null || value < biaya.getId()) {
								ids.put(biaya.getItemBiaya().getId(), biaya.getId());
								maps.put(biaya.getItemBiaya().getId(), biaya);
							}
						} else if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
							if (biaya.getNominal() != null && biaya.getNominal().intValue() != 0 && !maps.containsKey(biaya.getId())) {
								maps.put(biaya.getId(), biaya);
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:614");}
				}

				for (Object o : detailBiaya) {
					try {
						if (o instanceof DetailBiaya) {
							DetailBiaya biaya = (DetailBiaya) o;
							if (!maps.containsKey(biaya.getItemBiaya().getId())
									|| (nolMasukFilter && ((DetailBiaya) maps.get(biaya.getItemBiaya().getId())).getNilaiBiaya().intValue() == 0)) {
								maps.put(biaya.getItemBiaya().getId(), biaya);
							}
						} else if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
							if (!maps.containsKey(biaya.getId())
									|| (nolMasukFilter && ((PengaturanPembayaranBulanan) maps.get(biaya.getId())).getNominal().intValue() == 0)) {
								maps.put(biaya.getId(), biaya);
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:632");}
				}

				TreeSet treeSet = new TreeSet(maps.values());

				JSONObject data = new JSONObject();
				try {
					for (Object o : treeSet) {
						if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya1 = (DetailBiaya) o;
							detailBiaya1.updateKeterangan(mahasiswa, semester);
							data.put(detailBiaya1.getId().toString(), "1");
							GeneralValueObject.masukkanData(DetailBiaya.class, detailBiaya1);
						} else if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
							data.put(biaya.getId().toString(), "2");
							GeneralValueObject.masukkanData(PengaturanPembayaranBulanan.class, biaya);
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:651");}

				mahasiswa.put(data.toString(), key);
				
				return treeSet;
			}
			
		} catch(Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PembayaranUtilHelper.java:659");
			return new TreeSet();
		} finally {
			closeOpenedSession(session);
		}
	}

	/**
	 * Bentuk ringkas untuk mengambil tagihan seorang calon mahasiswa tanpa semester eksplisit
	 * (mendelegasikan ke overload dengan {@code semester = null}, mis. untuk tagihan pendaftaran
	 * yang tidak terikat semester).
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa subjek tagihan
	 * @param jenisKegiatan jenis kegiatan/tagihan yang dicari
	 * @param jurusan jurusan tujuan; boleh {@code null} (akan ditentukan otomatis di implementasi)
	 * @param reload {@code true} untuk memaksa query ulang ke DB melewati cache per-calon-mahasiswa
	 * @return koleksi {@link DetailBiaya} yang berlaku
	 */
	public static Collection<DetailBiaya> getDetailBiayaCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, Jurusan jurusan, boolean reload) {
		return getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, jurusan, null, reload);
	}

	/**
	 * Nilai jenis seleksi lama bisa tidak lagi termasuk pada gelombang yang dipilih
	 * (misalnya tersimpan Genap, sedangkan gelombang hanya menyediakan Ganjil).
	 * Gunakan pilihan yang masih sah agar pencarian billing tidak terkunci pada data
	 * lama yang sudah tidak konsisten.
	 */
	private static JenisSeleksi jenisSeleksiSesuaiGelombang(BiodataCalonMahasiswa calonMahasiswa) {
		JenisSeleksi tersimpan = calonMahasiswa == null ? null : calonMahasiswa.getJenisSeleksi();
		GelombangPendaftaran gelombang = calonMahasiswa == null ? null
				: calonMahasiswa.getGelombangPendaftaran();
		if (gelombang == null) {
			return tersimpan;
		}

		List<JenisSeleksi> pilihan = gelombang.ambilJenisSeleksi();
		if (pilihan == null || pilihan.isEmpty()) {
			return tersimpan;
		}
		for (JenisSeleksi item : pilihan) {
			if (item != null && tersimpan != null && item.getId() != null
					&& item.getId().equals(tersimpan.getId())) {
				return item;
			}
		}

		JenisSeleksi bawaanGelombang = gelombang.getJenisSeleksi();
		if (bawaanGelombang != null) {
			for (JenisSeleksi item : pilihan) {
				if (item != null && item.getId() != null && bawaanGelombang.getId() != null
						&& item.getId().equals(bawaanGelombang.getId())) {
					return item;
				}
			}
		}
		// Jika nilai lama tidak termasuk pilihan gelombang, gunakan urutan pertama yang
		// dikonfigurasi admin. Mempertahankan nilai lama membuat jenis seleksi di biodata dan
		// sumber tagihan berbeda (mis. Genap pada gelombang Ganjil).
		return pilihan.get(0);
	}

	/**
	 * Analog {@link #getDetailBiayaMahasiswadariDatabase} tetapi untuk subjek
	 * {@link BiodataCalonMahasiswa} (calon mahasiswa yang belum resmi menjadi {@link Mahasiswa}) —
	 * dipakai pada tagihan pendaftaran/her-registrasi mahasiswa baru sebelum data mahasiswa
	 * terbentuk. Alur ringkas: (1) cache per-calon-mahasiswa (kunci id+jenisKegiatan+semester,
	 * lihat {@link GeneralValueObject}); (2) resolusi {@code jenisSeleksi} yang masih sah lewat
	 * {@link #jenisSeleksiSesuaiGelombang}; (3) cek Setting Biaya khusus NIM/pengecualian tagihan;
	 * (4) tentukan apakah jenjang ini harus mode angsuran DAN benar-benar punya baris bulanan
	 * (lewat {@code PembayaranUtil.hitungBarisBulananSemester}) — bila tidak ada baris bulanan sama
	 * sekali, mode angsuran dibatalkan agar tagihan reguler tidak ikut hilang; (5) query
	 * {@link Criteria} ke {@link PengaturanPembayaranBulanan} atau {@link DetailBiaya} tergantung
	 * hasil (4), disaring lewat paket, gelombang pendaftaran, status awal, jenis seleksi, jenjang,
	 * jurusan, angkatan, dsb; (6) dedup per {@code itemBiaya}, simpan ke cache, kembalikan.
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa subjek tagihan; {@code null} atau
	 *        {@code jenisKegiatan == null} langsung mengembalikan koleksi kosong
	 * @param jenisKegiatan jenis kegiatan/tagihan yang dicari
	 * @param jurusan jurusan tujuan; bila {@code null} akan dicari otomatis (jurusan aktif pertama pada jenjang)
	 * @param semester semester akademik yang dicari; boleh {@code null} untuk tagihan tanpa semester (mis. pendaftaran)
	 * @param reload {@code true} untuk memaksa query ulang ke DB melewati cache per-calon-mahasiswa
	 * @return koleksi {@link DetailBiaya} yang berlaku bagi calon mahasiswa ini
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Collection<DetailBiaya> getDetailBiayaCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, Jurusan jurusan, Integer semester, boolean reload) {

		if (biodataCalonMahasiswa == null || jenisKegiatan == null) {
			return new TreeSet<DetailBiaya>();
		}

		String key = "tagihan_cal_mhs_" + biodataCalonMahasiswa.getId() + "_" + jenisKegiatan.getId() + "_" + semester;

		if (!reload) {
			try {
				String s = biodataCalonMahasiswa.retreive(key);
				JSONObject data = s == null || s.trim().isEmpty() ? null : new JSONObject(s);
				if (data != null) {
					List d = new ArrayList();
					Iterator<String> iter = data.keys();
					while (iter.hasNext()) {
						String keyIter = iter.next();
						String value = data.get(keyIter).toString();

						if (value.equalsIgnoreCase("1")) {
							DetailBiaya detailBiaya1 = (DetailBiaya) GeneralValueObject.ambilData(DetailBiaya.class, keyIter, true);

							if (detailBiaya1 != null && biodataCalonMahasiswa.getMahasiswa() != null) {
								detailBiaya1.updateKeterangan(biodataCalonMahasiswa.getMahasiswa(), semester);
							}
							if (detailBiaya1 != null) {
								d.add(detailBiaya1);
							}
						} else if (value.equalsIgnoreCase("2")) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) GeneralValueObject
									.ambilData(PengaturanPembayaranBulanan.class, keyIter, true);
							if (pengaturanPembayaranBulanan != null) {
								d.add(pengaturanPembayaranBulanan);
							}
						}
					}

					try {
						Collections.sort(d);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PembayaranUtilHelper.java:713");
					}

					return d;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PembayaranUtilHelper.java:719");
			}
		}

		Jenjang jenjang = jurusan != null ? jurusan.getJenjang() : biodataCalonMahasiswa.getJenjang();
		JenisSeleksi jenisSeleksi = jenisSeleksiSesuaiGelombang(biodataCalonMahasiswa);
		String program = biodataCalonMahasiswa.getProgram();
		Integer angkatan = biodataCalonMahasiswa.getTahun();
		Paket paket = biodataCalonMahasiswa.getPaket();
		GelombangPendaftaran gelombangPendaftaran = biodataCalonMahasiswa.getGelombangPendaftaran();
		String warganegara = biodataCalonMahasiswa.getKewarganegaraan();
		String kelamin = biodataCalonMahasiswa.getJenisKelamin();
		AfiliasiCalonMahasiswa afiliasiCalonMahasiswa = biodataCalonMahasiswa.getAfiliasiCalonMahasiswa();

		String tahunAkademik;
		try {
			Integer tahunAngkatanMhs = biodataCalonMahasiswa.getTahun();
			Integer semesterMulai = 0;
			Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, semesterMulai,
					biodataCalonMahasiswa.getSemesterMulai());
			tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		} catch (Exception e) {
			tahunAkademik = biodataCalonMahasiswa.getTahunAkademik();
		}

		String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0" : tahunAkademik.split("/")[0])
				+ (semester == null ? "0" : (semester % 2 == 0) ? "2" : "1");
		Integer ta = 0;
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:749");}

		Session session = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();

			// Fresh reload untuk hindari stale cache dari combobox lama (calon mahasiswa path)
			if (jenisKegiatan != null && jenisKegiatan.getId() != null) {
				try {
					JenisKegiatan freshJk = (JenisKegiatan) session.get(JenisKegiatan.class, jenisKegiatan.getId());
					if (freshJk != null) jenisKegiatan = freshJk;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:761");}
			}

			List<DetailBiaya> biayaDefault = SetingBiayaHelper.getDetailBiayaDefault(session, biodataCalonMahasiswa,
					jenisKegiatan, semester, ta);
			if (PengecualianTagihanList.adalah(biayaDefault)) {
				return PengecualianTagihanList.kosong();
			}

			if (biayaDefault == null || biayaDefault.isEmpty()) {
				biayaDefault = SetingBiayaHelper.getDetailBiayaDefault(session, angkatan, jenjang, semester, jenisKegiatan,
						biodataCalonMahasiswa.getStatusAwalMahasiswa(), ConstantValues.AKTIF,
						jenisSeleksi, biodataCalonMahasiswa.getGelombangPendaftaran(),
						biodataCalonMahasiswa.getPaket(), jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
						biodataCalonMahasiswa.getNim());
				if (PengecualianTagihanList.adalah(biayaDefault)) {
					return PengecualianTagihanList.kosong();
				}
			}
			
			if (biayaDefault != null && !biayaDefault.isEmpty()) {
				if (biodataCalonMahasiswa.getMahasiswa() != null) {
					for (DetailBiaya detailBiaya : biayaDefault) {
						detailBiaya.updateKeterangan(biodataCalonMahasiswa.getMahasiswa(), semester);
					}
				}

				JSONObject data = new JSONObject();
				for (DetailBiaya detailBiaya : biayaDefault) {
					try {
						data.put(detailBiaya.getId().toString(), "1");
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:785");}
					GeneralValueObject.masukkanData(DetailBiaya.class, detailBiaya);
				}
				biodataCalonMahasiswa.put(data.toString(), key);
				
				return biayaDefault;
			}

			if (jurusan == null) {
				jurusan = (Jurusan) session.createCriteria(Jurusan.class)
						.add(Restrictions.eq("aktif", true))
						.add(Restrictions.eq("jenjang", jenjang))
						.setMaxResults(1)
						.uniqueResult();
			}

			List<ItemBiaya> detailSettingBiayas = SetingBiayaHelper.getItemBiaya(session, angkatan, jenjang, semester,
					jenisKegiatan, biodataCalonMahasiswa.getStatusAwalMahasiswa(), ConstantValues.AKTIF,
					jenisSeleksi, biodataCalonMahasiswa.getGelombangPendaftaran(),
					biodataCalonMahasiswa.getPaket(), jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
					biodataCalonMahasiswa.getNim());
			if (detailSettingBiayas == null) {
				return PengecualianTagihanList.kosong();
			}

			// Cek apakah jenjang calon mhs ini masuk mode angsuran — terpusat via
			// modeAngsuranUntukJenjang(jenjang, semester, angkatan) sehingga aturan
			// per-jenjang SEKALIGUS per-semester dan per-angkatan ("Berlaku di smt",
			// format TAHUN:SMT) terhormati. Lalu verifikasi terhadap kenyataan billing:
			// bila kombinasi ini tidak punya baris bulanan sama sekali, JANGAN paksa
			// jalur angsuran — kuerinya akan kosong dan tagihan reguler ikut lenyap
			// (total 0 di layar admin & inquiry bank error 07).
			boolean isHarusAngsuranForJenjang = Boolean.TRUE
					.equals(jenisKegiatan.modeAngsuranUntukJenjang(jenjang, semester, angkatan));
			if (isHarusAngsuranForJenjang) {
				int barisBulanan = ais.action.ws.util.PembayaranUtil.hitungBarisBulananSemester(session,
						jenisKegiatan, jenjang, semester, angkatan, null);
				if (barisBulanan == 0) {
					isHarusAngsuranForJenjang = false;
				}
			}

			Criteria criteria = isHarusAngsuranForJenjang
					? session.createCriteria(PengaturanPembayaranBulanan.class)
							.add(Restrictions.eq("aktif", true))
							.setProjection(Projections.property("detailBiaya")).createCriteria("detailBiaya")
					: session.createCriteria(DetailBiaya.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			
			filterCriteriaDenganNilaiTambahan(criteria, session, null, biodataCalonMahasiswa);

			criteria = criteria
					.add(paket == null ? Restrictions.isNull("paket") : Restrictions.eq("paket", paket))
					.add(detailSettingBiayas == null || detailSettingBiayas.isEmpty() ? Restrictions.sqlRestriction(SQL_FALSE) : Restrictions.in("itemBiaya", detailSettingBiayas))
					.add(Restrictions.or(Restrictions.eq("merupakanPembayaran", false), Restrictions.isNull("merupakanPembayaran")));
					
			if (paket != null && Boolean.TRUE.equals(paket.getBiayaPendaftaranSemuaGelombangSama())) {
				criteria.add(Restrictions.isNull("gelombangPendaftaran"));
			} else {
				if (jenisKegiatan.getNamaKegiatan() != null && jenisKegiatan.getNamaKegiatan().equalsIgnoreCase(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
					criteria.add(Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran));
				} else {
					criteria.add(Restrictions.or(Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran), Restrictions.isNull("gelombangPendaftaran")));
				}
			}

			criteria.add(semester == null ? Restrictions.in("semester", new Integer[] { 0, 1 }) : Restrictions.eq("semester", semester))
					.add(Restrictions.ge("semester", jenisKegiatan.getMinSmt()))
					.add(Restrictions.le("semester", jenisKegiatan.getMaxSmt()))
					.add(Restrictions.eq("statusAwalMahasiswa", biodataCalonMahasiswa.getStatusAwalMahasiswa()))
					.add(Restrictions.eq("statusMahasiswa", ConstantValues.AKTIF))
					.add(warganegara != null ? Restrictions.ilike("wnaAtauWni", warganegara, MatchMode.EXACT) : Restrictions.sqlRestriction(SQL_TRUE))
					.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
					.add(Restrictions.eq("jenisSeleksi", jenisSeleksi))
					.add(Restrictions.eq("jenjang", jenjang))
					.add(Restrictions.eq("jurusan", jurusan))
					.add(program != null ? Restrictions.ilike("program", program, MatchMode.EXACT) : Restrictions.sqlRestriction(SQL_TRUE))
					.add(Restrictions.eq("angkatan", angkatan))
					.add(biodataCalonMahasiswa.getSemesterMulai() != null ? Restrictions.ilike("mulaiBelajarDiSemester", biodataCalonMahasiswa.getSemesterMulai(), MatchMode.EXACT) : Restrictions.sqlRestriction(SQL_TRUE));

			criteria.addOrder(Order.desc("id"));

			List<DetailBiaya> detailBiaya = criteria.list();

			if (isHarusAngsuranForJenjang) {
				List<DetailBiaya> biayaDefaultBiaya = SetingBiayaHelper.getDetailBiayaBukanDefaultBiaya(session, angkatan,
						jenjang, semester, jenisKegiatan, biodataCalonMahasiswa.getStatusAwalMahasiswa(),
						ConstantValues.AKTIF, jenisSeleksi,
						biodataCalonMahasiswa.getGelombangPendaftaran(), biodataCalonMahasiswa.getPaket(), jurusan, program,
						kelamin, afiliasiCalonMahasiswa, ta);
				if (biayaDefaultBiaya != null && !biayaDefaultBiaya.isEmpty()) {
					for (DetailBiaya detailBiayaDefault : biayaDefaultBiaya) {
						detailBiaya.add(detailBiayaDefault);
					}
				}
			}


			Map<Long, DetailBiaya> maps = new HashMap<Long, DetailBiaya>();
			Map<Long, Long> ids = new HashMap<Long, Long>();

			for (DetailBiaya biaya : detailBiaya) {
				if (biaya != null && biaya.getItemBiaya() != null) {
					Long value = ids.get(biaya.getItemBiaya().getId());
					if (value == null || value < biaya.getId()) {
						ids.put(biaya.getItemBiaya().getId(), biaya.getId());
						maps.put(biaya.getItemBiaya().getId(), biaya);
					}
				}
			}

			TreeSet d = new TreeSet(maps.values());

			JSONObject data = new JSONObject();
			try {
				for (Object o : d) {
					try {
						if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya1 = (DetailBiaya) o;
							data.put(detailBiaya1.getId().toString(), "1");

							if (biodataCalonMahasiswa.getMahasiswa() != null) {
								detailBiaya1.updateKeterangan(biodataCalonMahasiswa.getMahasiswa(), semester);
							}

							GeneralValueObject.masukkanData(DetailBiaya.class, detailBiaya1);
						} else if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
							data.put(biaya.getId().toString(), "2");
							GeneralValueObject.masukkanData(PengaturanPembayaranBulanan.class, biaya);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:901");}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:903");}

			biodataCalonMahasiswa.put(data.toString(), key);

			return d;
			
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PembayaranUtilHelper.java:910");
			return new TreeSet<DetailBiaya>();
		} finally {
			closeOpenedSession(session);
		}
	}

	/**
	 * Bentuk khusus untuk calon mahasiswa BARU yang jurusannya diambil dari
	 * {@code biodataCalonMahasiswa.getProdiLulus()} (jurusan hasil kelulusan seleksi), lalu
	 * mendelegasikan ke {@link #getDetailBiayaCalonMahasiswa(BiodataCalonMahasiswa, JenisKegiatan, Jurusan, boolean)}
	 * tanpa reload (memakai cache bila ada).
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa baru subjek tagihan; boleh {@code null}
	 * @param jenisKegiatan jenis kegiatan/tagihan yang dicari
	 * @return koleksi {@link DetailBiaya} yang berlaku
	 */
	public static Collection<DetailBiaya> getDetailBiayaMahasiswaBaru(BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan) {
		Jurusan jurusan = null;
		if (biodataCalonMahasiswa != null) {
			jurusan = biodataCalonMahasiswa.getProdiLulus();
		}
		return getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, jurusan, false);
	}

	/**
	 * Menambahkan (bila diaktifkan lewat {@link Konfigurasi} {@code tambah_dan_aktifkan_filter_ke_1/2/3_paramater_tambahan})
	 * hingga tiga klausa {@code Restrictions.in("nilaiTambahan1/2/3", ...)} ke {@code criteria} yang
	 * sedang dibangun, berdasarkan {@code parameterTambahanInds} milik mahasiswa (dibaca via query
	 * {@link BiodataMahasiswa} terbaru) atau calon mahasiswa. Format {@code parameterTambahanInds}
	 * adalah baris-baris {@code "label->indeks<=>nilai"} dipisah newline; hanya bagian setelah
	 * {@code "->"} pada label (indeks parameter) dan nilainya yang dipakai, digabung jadi
	 * {@code "indeks<=>nilai"} untuk dicocokkan terhadap kolom {@code nilaiTambahanN} pada entity
	 * tagihan. Tidak melakukan apa-apa bila {@code criteria} atau {@code session} {@code null}, bila
	 * tidak ada filter yang aktif, atau bila tidak ada nilai tambahan yang berhasil diparse.
	 * <b>Efek samping:</b> memodifikasi {@code criteria} yang diteruskan (menambahkan restriction),
	 * tidak mengembalikan nilai.
	 *
	 * @param criteria criteria Hibernate yang sedang dibangun untuk query tagihan; dimodifikasi in-place
	 * @param session session Hibernate aktif, dipakai untuk query {@code parameterTambahanInds} mahasiswa
	 * @param mahasiswa mahasiswa subjek, boleh {@code null} bila subjeknya calon mahasiswa
	 * @param biodataCalonMahasiswa calon mahasiswa subjek, dipakai bila {@code mahasiswa} {@code null}
	 */
	public static void filterCriteriaDenganNilaiTambahan(Criteria criteria, Session session, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa) {
		if (criteria == null || session == null) {
			return;
		}
		
		Konfigurasi konfigurasiTambahan1 = Common.getKonfigurasi("tambah_dan_aktifkan_filter_ke_1_paramater_tambahan", Konfigurasi.TIDAK_AKTIF, "-1", "", "");
		Konfigurasi konfigurasiTambahan2 = Common.getKonfigurasi("tambah_dan_aktifkan_filter_ke_2_paramater_tambahan", Konfigurasi.TIDAK_AKTIF, "-1", "", "");
		Konfigurasi konfigurasiTambahan3 = Common.getKonfigurasi("tambah_dan_aktifkan_filter_ke_3_paramater_tambahan", Konfigurasi.TIDAK_AKTIF, "-1", "", "");

		List<String> nilaiTambahan = null;
		
		boolean isAktif1 = konfigurasiTambahan1 != null && Konfigurasi.AKTIF.equals(konfigurasiTambahan1.getNilai());
		boolean isAktif2 = konfigurasiTambahan2 != null && Konfigurasi.AKTIF.equals(konfigurasiTambahan2.getNilai());
		boolean isAktif3 = konfigurasiTambahan3 != null && Konfigurasi.AKTIF.equals(konfigurasiTambahan3.getNilai());

		if (isAktif1 || isAktif2 || isAktif3) {

			String parameterTambahanInds = null;

			if (mahasiswa != null && mahasiswa.getId() != null) {
				parameterTambahanInds = (String) session.createCriteria(BiodataMahasiswa.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa))
						.addOrder(Order.desc("id"))
						.setMaxResults(1)
						.setProjection(Projections.property("parameterTambahanInds"))
						.uniqueResult();
			} else if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getParameterTambahanInds() != null) {
				parameterTambahanInds = biodataCalonMahasiswa.getParameterTambahanInds();
			}

			if (parameterTambahanInds != null && !parameterTambahanInds.trim().isEmpty()) {
				nilaiTambahan = new ArrayList<String>();
				String[] spl = parameterTambahanInds.split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					String lbl = value.length > 0 ? value[0].trim() : "";
					String val = value.length > 1 ? value[1].trim() : "";
					if (!val.isEmpty()) {
						try {
							nilaiTambahan.add(lbl.split("->")[1].trim() + "<=>" + val);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:967");}
					}
				}
			}
		}

		if (nilaiTambahan != null && !nilaiTambahan.isEmpty()) {
			if (isAktif1) {
				criteria.add(Restrictions.in("nilaiTambahan1", nilaiTambahan));
			}
			if (isAktif2) {
				criteria.add(Restrictions.in("nilaiTambahan2", nilaiTambahan));
			}
			if (isAktif3) {
				criteria.add(Restrictions.in("nilaiTambahan3", nilaiTambahan));
			}
		}
	}


	/**
	 * Membaca {@link Konfigurasi} {@code tampilkan_pengaturan_bulanan_nol_nilai_bisa_diubah}: bila
	 * aktif, baris {@link PengaturanPembayaranBulanan} bernominal 0 tetap ditampilkan asalkan
	 * {@code itemBiaya}-nya ditandai {@code nilaiBisaDiubah} (nilai bisa diedit manual oleh admin).
	 *
	 * @return {@code true} bila konfigurasi tersebut aktif, {@code false} bila tidak aktif/gagal dibaca
	 */
	private static boolean tampilkanPengaturanBulananNolNilaiBisaDiubah() {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(
					"tampilkan_pengaturan_bulanan_nol_nilai_bisa_diubah", Konfigurasi.TIDAK_AKTIF);
			return konfigurasi != null && Konfigurasi.AKTIF.equals(konfigurasi.getNilai());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Cek sederhana apakah satu baris {@link PengaturanPembayaranBulanan} berstatus aktif
	 * ({@code getAktif() == true}), aman terhadap {@code null} dan exception (mengembalikan
	 * {@code false} bila terjadi keduanya).
	 *
	 * @param pembayaranBulanan baris pengaturan bulanan yang dicek, boleh {@code null}
	 * @return {@code true} hanya bila baris tidak {@code null} dan flag aktifnya {@code true}
	 */
	private static boolean isAktifPengaturanBulanan(PengaturanPembayaranBulanan pembayaranBulanan) {
		try {
			return pembayaranBulanan != null && Boolean.TRUE.equals(pembayaranBulanan.getAktif());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Mengambil nilai nominal efektif satu baris {@link PengaturanPembayaranBulanan}: mengutamakan
	 * nominal hasil modifikasi khusus mahasiswa via
	 * {@code PembayaranNominalModifikasiHelper.ambilNominalModifikasi(...)} bila nilainya signifikan
	 * (> 0.01 secara absolut); jika tidak ada modifikasi atau modifikasinya nol, jatuh kembali ke
	 * {@code pembayaranBulanan.getNominal()} asli. Selalu mengembalikan {@link Double} tidak-null
	 * (default {@code 0.0}); semua exception ditangkap dan dicatat, tidak pernah dilempar ke pemanggil.
	 *
	 * @param pembayaranBulanan baris pengaturan bulanan yang nominalnya dicari; {@code null} menghasilkan {@code 0.0}
	 * @param mahasiswa konteks mahasiswa untuk pencarian modifikasi nominal khusus, boleh {@code null}
	 * @param semester konteks semester untuk pencarian modifikasi nominal khusus
	 * @return nominal efektif (modifikasi bila ada dan signifikan, selain itu nominal asli), tidak pernah {@code null}
	 */
	private static Double ambilNominalPengaturanBulananAman(PengaturanPembayaranBulanan pembayaranBulanan,
			Mahasiswa mahasiswa, Integer semester) {
		Double nominal = Double.valueOf(0.0);
		try {
			if (pembayaranBulanan == null) {
				return nominal;
			}
			try {
				Double nominalModifikasi = PembayaranNominalModifikasiHelper.ambilNominalModifikasi(pembayaranBulanan,
						mahasiswa, semester);
				if (nominalModifikasi != null) {
					nominal = nominalModifikasi;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1018");
			}
			if (Math.abs(nominal.doubleValue()) > 0.01) {
				return nominal;
			}
			try {
				Double nominalAsli = pembayaranBulanan.getNominal();
				if (nominalAsli != null) {
					nominal = nominalAsli;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1028");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1030");
		}
		return nominal == null ? Double.valueOf(0.0) : nominal;
	}

	/**
	 * Menentukan apakah satu baris {@link PengaturanPembayaranBulanan} layak ditampilkan di layar
	 * billing bulanan. Urutan pemeriksaan: (1) harus aktif ({@link #isAktifPengaturanBulanan}); (2)
	 * lolos bila nominal efektifnya ({@link #ambilNominalPengaturanBulananAman}) signifikan (&gt; 0.01);
	 * (3) tetap lolos meski nominal 0 bila item biayanya berjenis {@link ItemBiaya#DIKALI_NILAI_MINUS}
	 * (biaya pengurang, bisa sah bernilai 0); (4) tetap lolos bila baris ditandai eksplisit
	 * {@code tetapDitampilkanWalaupunNol}; (5) tetap lolos bila
	 * {@code tampilkanNolNilaiBisaDiubah} true dan item biayanya {@code nilaiBisaDiubah} (lihat
	 * {@link #tampilkanPengaturanBulananNolNilaiBisaDiubah}). Selain itu (nominal 0 dan tidak ada
	 * pengecualian di atas) dianggap tidak layak ditampilkan.
	 *
	 * @param pembayaranBulanan baris yang diperiksa
	 * @param tampilkanNolNilaiBisaDiubah hasil {@link #tampilkanPengaturanBulananNolNilaiBisaDiubah}, diteruskan agar tidak dibaca ulang per baris
	 * @param mahasiswa konteks mahasiswa untuk perhitungan nominal efektif, boleh {@code null}
	 * @param semester konteks semester untuk perhitungan nominal efektif
	 * @return {@code true} bila baris ini harus ditampilkan ke pengguna
	 */
	private static boolean isPengaturanBulananLayakDitampilkan(PengaturanPembayaranBulanan pembayaranBulanan,
			boolean tampilkanNolNilaiBisaDiubah, Mahasiswa mahasiswa, Integer semester) {
		try {
			if (!isAktifPengaturanBulanan(pembayaranBulanan)) {
				return false;
			}
			Double nominal = ambilNominalPengaturanBulananAman(pembayaranBulanan, mahasiswa, semester);
			if (nominal != null && Math.abs(nominal.doubleValue()) > 0.01) {
				return true;
			}
			DetailBiaya detailBiaya = pembayaranBulanan.getDetailBiaya();
			ItemBiaya itemBiaya = detailBiaya == null ? null : detailBiaya.getItemBiaya();
			if (itemBiaya != null && ItemBiaya.DIKALI_NILAI_MINUS.equals(itemBiaya.getPenghitungan())) {
				return true;
			}
			if (Boolean.TRUE.equals(pembayaranBulanan.getTetapDitampilkanWalaupunNol())) {
				return true;
			}
			if (tampilkanNolNilaiBisaDiubah && itemBiaya != null && Boolean.TRUE.equals(itemBiaya.getNilaiBisaDiubah())) {
				return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1056");
		}
		return false;
	}

	/**
	 * Bentuk ringkas tanpa konteks mahasiswa/semester (dipakai saat modifikasi nominal per-mahasiswa
	 * tidak relevan) — mendelegasikan ke overload lengkap dengan {@code mahasiswa}/{@code semester} {@code null}.
	 *
	 * @param pengaturanPembayaranBulanans koleksi mentah baris {@link PengaturanPembayaranBulanan} kandidat
	 * @param nolMasukFilter bila {@code true}, baris nol yang sudah terpilih untuk suatu (bulan, item) bisa digantikan baris lain
	 * @return daftar baris yang layak ditampilkan, terurut alami, satu baris per pasangan (bulan real, item biaya)
	 */
	@SuppressWarnings({ "rawtypes" })
	public static List<PengaturanPembayaranBulanan> saringPengaturanPembayaranBulanan(List pengaturanPembayaranBulanans,
			boolean nolMasukFilter) {
		return saringPengaturanPembayaranBulanan(pengaturanPembayaranBulanans, nolMasukFilter, null, null);
	}

	/**
	 * Menyaring dan men-dedup koleksi mentah baris {@link PengaturanPembayaranBulanan} (bisa berisi
	 * objek tipe lain, yang diabaikan) menjadi satu baris per pasangan kunci
	 * {@code (realBulan, itemBiaya.id)}: baris pertama yang ditemukan untuk suatu kunci dipertahankan,
	 * kecuali {@code nolMasukFilter} true dan baris yang sudah terpilih bernominal 0 — dalam kasus itu
	 * baris berikutnya untuk kunci yang sama menggantikannya (memberi kesempatan baris bernilai lebih
	 * bermakna untuk tampil). Baris yang tidak layak ditampilkan menurut
	 * {@link #isPengaturanBulananLayakDitampilkan} disingkirkan lebih dulu. Hasil akhir diurutkan
	 * memakai {@link Collections#sort} (urutan alami {@link PengaturanPembayaranBulanan}).
	 *
	 * @param pengaturanPembayaranBulanans koleksi mentah baris kandidat, boleh berisi {@code null}/tipe lain (diabaikan)
	 * @param nolMasukFilter bila {@code true}, baris nol yang sudah terpilih untuk suatu (bulan, item) bisa digantikan baris berikutnya
	 * @param mahasiswa konteks mahasiswa untuk perhitungan nominal efektif (nominal modifikasi khusus), boleh {@code null}
	 * @param semester konteks semester untuk perhitungan nominal efektif
	 * @return daftar baris yang layak ditampilkan, terurut alami, satu baris per pasangan (bulan real, item biaya)
	 */
	@SuppressWarnings({ "rawtypes" })
	public static List<PengaturanPembayaranBulanan> saringPengaturanPembayaranBulanan(List pengaturanPembayaranBulanans,
			boolean nolMasukFilter, Mahasiswa mahasiswa, Integer semester) {
		Map<String, PengaturanPembayaranBulanan> map = new java.util.HashMap<String, PengaturanPembayaranBulanan>(
				pengaturanPembayaranBulanans == null ? 16 : Math.max(16, pengaturanPembayaranBulanans.size()));
		
		boolean tampilkanNolNilaiBisaDiubah = tampilkanPengaturanBulananNolNilaiBisaDiubah();
		if (pengaturanPembayaranBulanans != null) {
			for (Object valueObject : pengaturanPembayaranBulanans) {
				try {
					if (valueObject instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan pembayaranBulanan = (PengaturanPembayaranBulanan) valueObject;

						if (!isPengaturanBulananLayakDitampilkan(pembayaranBulanan, tampilkanNolNilaiBisaDiubah,
								mahasiswa, semester)) {
							continue;
						}
						
						if (pembayaranBulanan.getDetailBiaya() != null
								&& pembayaranBulanan.getDetailBiaya().getItemBiaya() != null
								&& pembayaranBulanan.getRealBulan() != null) {
							String bulan = pembayaranBulanan.getRealBulan() + "-" + pembayaranBulanan.getDetailBiaya().getItemBiaya().getId();
							
							boolean isZeroFilter = false;
							if (nolMasukFilter && map.containsKey(bulan)) {
								PengaturanPembayaranBulanan existingP = map.get(bulan);
								Double existingNominal = ambilNominalPengaturanBulananAman(existingP, mahasiswa, semester);
								if (existingNominal != null && Math.abs(existingNominal.doubleValue()) <= 0.01) {
									isZeroFilter = true;
								}
							}
							
							if (!map.containsKey(bulan) || isZeroFilter) {
								map.put(bulan, pembayaranBulanan);
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1104");}
			}
		}
		
		List<PengaturanPembayaranBulanan> bulanans = new ArrayList<PengaturanPembayaranBulanan>(map.values());
		try {
			Collections.sort(bulanans);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1111");}
		
		return bulanans;
	}

	/**
	 * Overload untuk subjek mahasiswa terdaftar: mendelegasikan ke
	 * {@link #countBulanan(Session, Mahasiswa, BiodataCalonMahasiswa, JenisKegiatan, Integer, Collection, boolean, boolean)}
	 * dengan {@code biodataCalonMahasiswa = null}.
	 *
	 * @param session session Hibernate yang bisa dipakai ulang; boleh {@code null} (akan dibuka session lokal bila perlu)
	 * @param mahasiswa mahasiswa subjek
	 * @param jenisKegiatan jenis kegiatan/tagihan yang dihitung jumlah baris bulanannya
	 * @param semester semester akademik yang dihitung
	 * @param detailBiayas koleksi {@link DetailBiaya} yang jadi acuan (hasil query billing sebelumnya)
	 * @param reload {@code true} untuk memaksa hitung ulang melewati cache JSON temporary
	 * @param comitManual bila {@code true} dan transaksi dibuka lokal, transaksi tersebut di-commit di dalam method
	 * @return jumlah baris tagihan bulanan aktif yang bernilai signifikan untuk kombinasi ini
	 */
	@SuppressWarnings("rawtypes")
	public static int countBulanan(Session session, Mahasiswa mahasiswa, JenisKegiatan jenisKegiatan, Integer semester,
			Collection detailBiayas, boolean reload, boolean comitManual) {
		return countBulanan(session, mahasiswa, null, jenisKegiatan, semester, detailBiayas, reload, comitManual);
	}

	/**
	 * Overload untuk subjek calon mahasiswa: mendelegasikan ke
	 * {@link #countBulanan(Session, Mahasiswa, BiodataCalonMahasiswa, JenisKegiatan, Integer, Collection, boolean, boolean)}
	 * dengan {@code mahasiswa = null}.
	 *
	 * @param session session Hibernate yang bisa dipakai ulang; boleh {@code null}
	 * @param biodataCalonMahasiswa calon mahasiswa subjek
	 * @param jenisKegiatan jenis kegiatan/tagihan yang dihitung jumlah baris bulanannya
	 * @param semester semester akademik yang dihitung
	 * @param detailBiayas koleksi {@link DetailBiaya} yang jadi acuan
	 * @param reload {@code true} untuk memaksa hitung ulang melewati cache JSON temporary
	 * @param comitManual bila {@code true} dan transaksi dibuka lokal, transaksi tersebut di-commit di dalam method
	 * @return jumlah baris tagihan bulanan aktif yang bernilai signifikan untuk kombinasi ini
	 */
	@SuppressWarnings("rawtypes")
	public static int countBulanan(Session session, BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan,
			Integer semester, Collection detailBiayas, boolean reload, boolean comitManual) {
		return countBulanan(session, null, biodataCalonMahasiswa, jenisKegiatan, semester, detailBiayas, reload,
				comitManual);
	}

	/**
	 * Menghitung berapa banyak "baris tagihan bulanan" yang berlaku untuk mahasiswa/calon mahasiswa
	 * ini pada kombinasi jenis kegiatan + semester tertentu — dipakai UI untuk menampilkan jumlah
	 * cicilan/bulan yang harus dibayar. Alur ringkas:
	 * <ol>
	 * <li>{@code detailBiayas} yang merupakan sentinel {@link PengecualianTagihanList} langsung
	 * menghasilkan {@code 0}.</li>
	 * <li>Bila {@code jenisKegiatan} punya aturan mode angsuran per jenjang/semester/angkatan
	 * ({@code modeAngsuranUntukJenjang}): jika {@code TRUE}, hasilnya diambil dari jumlah baris
	 * bulanan NYATA di billing untuk semester ini ({@code PembayaranUtil.hitungBarisBulananSemester});
	 * jika query gagal ({@code -1}) dipertahankan perilaku lama, kembalikan {@code 1}. Jika
	 * {@code FALSE} dan {@code detailBiayas} tidak kosong (billing reguler ada), hasilnya {@code 0};
	 * jika billing reguler kosong, dicoba fallback menghitung baris
	 * {@link PengaturanPembayaranBulanan} aktif untuk jenjang+semester ini secara langsung. Jika
	 * aturan {@code null} (tidak ada aturan per-jenjang), lanjut ke penghitungan DB biasa di bawah.</li>
	 * <li>Penghitungan DB biasa: hasil di-cache per mahasiswa/calon-mahasiswa lewat
	 * {@code Common.getJSONTemporary}/{@code setJSONTemporary} (kunci berisi id+jenisKegiatan+semester);
	 * bila {@code detailBiayas} tidak kosong, dihitung {@code rowCount()} baris
	 * {@link PengaturanPembayaranBulanan} aktif yang terkait ke salah satu {@code detailBiayas} DAN
	 * (item biayanya {@link ItemBiaya#DIKALI_NILAI_MINUS} ATAU nominalnya &gt; 0.01) — dijalankan dalam
	 * transaksi (dibuka lokal bila belum ada transaksi aktif, dan di-commit hanya bila
	 * {@code comitManual}).</li>
	 * </ol>
	 * Session dan transaksi lokal (yang dibuka sendiri oleh method ini karena {@code session} null/tertutup)
	 * selalu ditutup di {@code finally}; session yang diteruskan pemanggil TIDAK ditutup oleh method ini.
	 *
	 * @param session session Hibernate yang bisa dipakai ulang oleh pemanggil; boleh {@code null}/tertutup (akan dibuka session lokal)
	 * @param mahasiswa mahasiswa subjek, saling eksklusif dengan {@code biodataCalonMahasiswa}
	 * @param biodataCalonMahasiswa calon mahasiswa subjek, saling eksklusif dengan {@code mahasiswa}
	 * @param jenisKegiatan jenis kegiatan/tagihan yang dihitung jumlah baris bulanannya
	 * @param semester semester akademik yang dihitung
	 * @param detailBiayas koleksi {@link DetailBiaya} acuan (hasil query billing reguler sebelumnya)
	 * @param reload {@code true} untuk memaksa hitung ulang melewati cache JSON temporary
	 * @param comitManual bila {@code true} dan transaksi lokal dibuka oleh method ini, transaksi tersebut di-commit di sini (bukan diserahkan ke pemanggil)
	 * @return jumlah baris tagihan bulanan aktif yang bernilai signifikan untuk kombinasi mahasiswa/jenisKegiatan/semester ini
	 */
	@SuppressWarnings({ "rawtypes" })
	public static int countBulanan(Session session, Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, Integer semester, Collection detailBiayas, boolean reload,
			boolean comitManual) {
		if (PengecualianTagihanList.adalah(detailBiayas)) {
			return 0;
		}

		// Fresh reload untuk hindari stale cache dari combobox lama
		if (jenisKegiatan != null && jenisKegiatan.getId() != null) {
			Session refreshSession = session;
			boolean isLocalRefreshSession = false;
			try {
				if (refreshSession == null || !refreshSession.isOpen()) {
					refreshSession = HibernateUtil.getSessionFactory().openSession();
					isLocalRefreshSession = true;
				}
				JenisKegiatan freshJk = (JenisKegiatan) refreshSession.get(JenisKegiatan.class, jenisKegiatan.getId());
				if (freshJk != null) jenisKegiatan = freshJk;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1145");
			} finally {
				if (isLocalRefreshSession && refreshSession != null && refreshSession.isOpen()) {
					try { refreshSession.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1148");}
				}
			}
		}

		if (jenisKegiatan != null) {
			Jenjang mhsJenjang = null;
			if (mahasiswa != null) {
				mhsJenjang = mahasiswa.getJurusan() != null
						? mahasiswa.getJurusan().getJenjang() : mahasiswa.getJenjang();
			} else if (biodataCalonMahasiswa != null) {
				mhsJenjang = biodataCalonMahasiswa.getJenjang();
			}
			Integer angkatanMhs = mahasiswa != null ? mahasiswa.getTahunangkatan()
					: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getTahun() : null);
			Boolean modeAngsuran = jenisKegiatan.modeAngsuranUntukJenjang(mhsJenjang, semester, angkatanMhs);
			if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
					"[DEBUG-ANGSURAN][countBulanan] mhs="
					+ (mahasiswa != null ? mahasiswa.getNim() : (biodataCalonMahasiswa != null ? "cln-" + biodataCalonMahasiswa.getId() : "null"))
					+ " jk=" + jenisKegiatan.getNama()
					+ " jenjang=" + (mhsJenjang != null ? mhsJenjang.getNama() : "null")
					+ " detailBiayas=" + (detailBiayas != null ? detailBiayas.size() : 0)
					+ " modeAngsuran=" + modeAngsuran);
			if (Boolean.TRUE.equals(modeAngsuran)) {
				// PER-SEMESTER: aturan per-jenjang tidak boleh menimpa kenyataan billing.
				// Konfigurasi bulanan dibuat per semester (contoh nyata: S2 smt 1-3 bulanan,
				// smt 4 sekali tagih) — hitung baris bulanan yang BENAR-BENAR ada untuk
				// semester ini; 0 berarti semester ini bukan bulanan meski jenjang ditandai
				// harus angsuran. -1 = pengecekan gagal → pertahankan perilaku lama (paksa 1).
				// CATATAN (revert 07-17): status "Tagihan Default" di SettingBiaya TIDAK lagi
				// memaksa mode menjadi bukan-bulanan — mode murni mengikuti aturan jenjang/
				// semester dan keberadaan baris bulanan di billing.
				int nyata = ais.action.ws.util.PembayaranUtil.hitungBarisBulananSemester(session,
						jenisKegiatan, mhsJenjang, semester, angkatanMhs, detailBiayas);
				if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
						"[DEBUG-ANGSURAN][countBulanan] → TRUE: baris bulanan nyata semester ini=" + nyata);
				if (nyata >= 0) {
					return nyata;
				}
				return 1;
			} else if (Boolean.FALSE.equals(modeAngsuran)) {
				if (detailBiayas != null && !detailBiayas.isEmpty()) {
					if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
							"[DEBUG-ANGSURAN][countBulanan] → FALSE + ada billing reguler (" + detailBiayas.size() + "): return 0");
					return 0;
				}
				// Billing reguler kosong → cek PPB agar bisa fallback ke angsuran
				if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
						"[DEBUG-ANGSURAN][countBulanan] → FALSE + billing reguler KOSONG: cek PPB untuk jenjang="
						+ (mhsJenjang != null ? mhsJenjang.getNama() : "null"));
				if (mhsJenjang != null) {
					Session ppbSession = session;
					boolean isLocalPpb = false;
					try {
						if (ppbSession == null || !ppbSession.isOpen()) {
							ppbSession = HibernateUtil.getSessionFactory().openSession();
							isLocalPpb = true;
						}
						@SuppressWarnings("unchecked")
						java.util.List<PengaturanPembayaranBulanan> ppbList = ppbSession
								.createCriteria(PengaturanPembayaranBulanan.class)
								.createAlias("detailBiaya", "db")
								.createAlias("db.itemBiaya", "dbItem")
								.add(Restrictions.eq("aktif", true))
								.add(Restrictions.eq("db.jenisKegiatan", jenisKegiatan))
								.add(Restrictions.eq("db.jenjang", mhsJenjang))
								// WAJIB samakan semester dengan yang sedang diminta -- tanpa filter ini baris
								// bulanan milik semester LAIN (mis. DetailBiaya.semester=2) ikut terhitung saat
								// mahasiswa sedang di semester 1, sehingga tagihan semester lain bocor tampil
								// (lihat juga getDetailBiayaMahasiswadariDatabase yang sudah pakai eq("semester",..) ketat).
								.add(Restrictions.eq("db.semester", semester))
								.add(Restrictions.or(
										Restrictions.eq("dbItem.penghitungan", ItemBiaya.DIKALI_NILAI_MINUS),
										Restrictions.gt("nominal", 0.01)))
								.list();
						// (revert 07-17) Status "Tagihan Default" TIDAK lagi menyaring hitungan —
						// seluruh baris bulanan aktif dihitung apa adanya.
						int ppbCount = ppbList == null ? 0 : ppbList.size();
						if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
								"[DEBUG-ANGSURAN][countBulanan] → PPB count=" + ppbCount);
						if (ppbCount > 0) {
							if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
									"[DEBUG-ANGSURAN][countBulanan] → return " + ppbCount + " (PPB fallback)");
							return ppbCount;
						}
					} catch (Exception e) {
						if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
								"[DEBUG-ANGSURAN][countBulanan] → PPB query ERROR: " + e.getMessage());
					} finally {
						if (isLocalPpb && ppbSession != null && ppbSession.isOpen()) {
							try { ppbSession.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1249");}
						}
					}
				}
				if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
						"[DEBUG-ANGSURAN][countBulanan] → FALSE + PPB kosong: return 0");
				return 0;
			}
			// modeAngsuran == null → tidak ada aturan per-jenjang, ikuti DB count di bawah
			if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
					"[DEBUG-ANGSURAN][countBulanan] → null: tidak ada aturan angsuran, lanjut DB count");
		}

		String key = (biodataCalonMahasiswa != null ? "cln_mhs_" + biodataCalonMahasiswa.getId()
				: "mhs_" + (mahasiswa != null ? mahasiswa.getId() : "null")) + "_" 
				+ (jenisKegiatan != null ? jenisKegiatan.getId() : "null") + "_" + semester + "_aktif_tagihan_v2";

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject = Common.getJSONTemporary(biodataCalonMahasiswa != null ? biodataCalonMahasiswa : mahasiswa, key);
			if (!reload && jsonObject != null) {
				if (jsonObject.has(key) && !jsonObject.isNull(key)) {
					return jsonObject.getInt(key);
				}
			}
		} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1274");}

		if (jsonObject == null) {
			jsonObject = new JSONObject();
		}

		int countPengaturanBulanan = 0;

		// (revert 07-17) Status "Tagihan Default" di SettingBiaya TIDAK lagi menyaring
		// penghitung mode angsuran — seluruh DetailBiaya dihitung apa adanya.
		if (detailBiayas != null && !detailBiayas.isEmpty()) {
			Session activeSession = session;
			boolean isLocalSession = false;
			boolean isLocalTransaction = false;

			try {
				if (activeSession == null || !activeSession.isOpen()) {
					activeSession = HibernateUtil.getSessionFactory().openSession();
					isLocalSession = true;
				}

				Transaction activeTransaction = activeSession.getTransaction();
				if (activeTransaction == null || !activeTransaction.isActive()) {
					activeTransaction = activeSession.beginTransaction();
					isLocalTransaction = true;
				}

				Number count = (Number) activeSession.createCriteria(PengaturanPembayaranBulanan.class)
								.createAlias("detailBiaya", "detailBiaya")
								.createAlias("detailBiaya.itemBiaya", "itemBiaya")
								.add(Restrictions.eq("aktif", true))
								.add(Restrictions.in("detailBiaya", detailBiayas))
								.add(Restrictions.or(
										Restrictions.eq("itemBiaya.penghitungan", ItemBiaya.DIKALI_NILAI_MINUS),
										Restrictions.gt("nominal", 0.01)))
								.setProjection(Projections.rowCount()).uniqueResult();

				countPengaturanBulanan = count != null ? count.intValue() : 0;

				if (isLocalTransaction) {
					activeTransaction.commit();
				}

			} catch (Exception e) {
				if (isLocalTransaction && activeSession != null && activeSession.getTransaction() != null && activeSession.getTransaction().isActive()) {
					try { activeSession.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1333");}
				}
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PembayaranUtilHelper.java:1335");
			} finally {
				if (isLocalSession && activeSession != null) {
					try { if (activeSession.isOpen()) activeSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1338");}
					try { activeSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1339");}
					try { if (activeSession.isOpen()) activeSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1340");}
				}
			}
		}

		try {
			jsonObject.put(key, countPengaturanBulanan);
			Common.setJSONTemporary(biodataCalonMahasiswa != null ? biodataCalonMahasiswa : mahasiswa, key, jsonObject);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1348");}

		return countPengaturanBulanan;
	}

	/**
	 * Fallback darurat: bila query billing reguler ({@code dataTagihanData}) kembali KOSONG untuk
	 * seorang mahasiswa/calon mahasiswa (mis. setting biaya sudah berubah/dihapus sehingga tagihan
	 * "resmi" tidak lagi bisa dihitung), method ini merekonstruksi tampilan tagihan dari
	 * <b>riwayat {@link CicilanPembayaran}</b> yang benar-benar sudah dibuat untuk mahasiswa tsb, agar
	 * layar Daftar Ulang Mahasiswa (dipanggil dari action {@code DaftarUlangMahasiswa*Action}) tidak
	 * menampilkan tagihan kosong padahal riwayat pembayaran/cicilan ada. {@code student} boleh berupa
	 * {@link Mahasiswa} atau {@link BiodataCalonMahasiswa}. Alur:
	 * <ol>
	 * <li>Tidak melakukan apa pun (return langsung) bila {@code dataTagihanData} sudah berisi data
	 * lain, {@code null}, atau {@code student} {@code null} — fallback ini murni "jaring pengaman"
	 * terakhir, bukan sumber data utama.</li>
	 * <li>Ambil semua {@link CicilanPembayaran} milik {@code student} (disaring {@code jenisKegiatan}
	 * bila diisi) dan tentukan mode: <i>angsuran</i> (bila {@code jenisKegiatan.getHanyaBerupaAngsuran()}
	 * atau ada baris cicilan ber-{@link PengaturanPembayaranBulanan}) vs <i>non-angsuran</i>
	 * (langsung ke {@link DetailBiaya}).</li>
	 * <li><b>Mode angsuran:</b> kumpulkan {@link PengaturanPembayaranBulanan} unik dari cicilan,
	 * DISARING agar hanya baris yang semester {@link DetailBiaya}-nya cocok dengan parameter
	 * {@code semester} yang masuk dataTagihanData (baris legacy tanpa semester TIDAK dianggap cocok
	 * — perbaikan agar tagihan semester lain tidak "bocor" ke semester yang sedang dibuka). Lalu,
	 * dalam transaksi terpisah, {@code nilaiBiaya}/{@code nominal} yang masih 0 diperbaiki dari
	 * jumlah {@code nilaiAsli} cicilan terkait.</li>
	 * <li><b>Mode non-angsuran:</b> kumpulkan {@link DetailBiaya} unik dari cicilan yang TIDAK
	 * memiliki {@link PengaturanPembayaranBulanan}, disaring semester sama seperti mode angsuran,
	 * dan di-dedup tambahan per pasangan {@code (itemBiaya.id, bayarKe)} — bukan per id
	 * {@link DetailBiaya} mentah — untuk menghindari baris tampil dobel akibat duplikasi
	 * {@link DetailBiaya} lama untuk item+bayarKe yang sama (baris duplikat yang dilewati dicatat ke
	 * {@code ErrorAuditUtil} agar bisa ditelusuri/dibersihkan manual). {@code nilaiBiaya}/{@code nominal}
	 * yang masih 0 diperbaiki serupa dari jumlah {@code nilaiAsli} cicilan.</li>
	 * </ol>
	 * Method ini banyak mencetak log diagnostik berprefiks {@code [TAGIHAN-DEBUG]} ke
	 * {@code System.out} (bukan hanya di mode debug) untuk membantu penelusuran kasus tagihan hilang.
	 * <b>Efek samping:</b> mengubah {@code dataTagihanData} dan {@code itemBiayas} secara in-place
	 * (tidak mengembalikan nilai), serta bisa melakukan UPDATE ke DB ({@code DetailBiaya.nilaiBiaya}
	 * dan {@code PengaturanPembayaranBulanan.nominal}) dalam transaksi lokalnya sendiri — kegagalan
	 * transaksi perbaikan di-rollback dan dilaporkan via {@code Common.tampilErrorJikaAdmin}, tidak
	 * menggagalkan pengisian {@code dataTagihanData} yang sudah terjadi sebelumnya.
	 *
	 * @param student subjek tagihan, instance {@link Mahasiswa} atau {@link BiodataCalonMahasiswa}; {@code null} membatalkan fallback
	 * @param jenisKegiatan penyaring jenis kegiatan/tagihan; boleh {@code null} untuk semua jenis kegiatan
	 * @param dataTagihanData daftar tagihan yang sedang dibangun oleh pemanggil; HANYA diisi bila datang kosong, dan diisi in-place
	 * @param itemBiayas peta {@code DetailBiaya.id -> DetailBiaya} yang ikut diisi in-place sejalan dengan {@code dataTagihanData}; boleh {@code null}
	 * @param semester semester yang SEDANG diminta layar ini; baris cicilan yang DetailBiaya-nya
	 *                 punya semester lain (non-null, tidak sama) DILEWATI -- tanpa ini, riwayat
	 *                 cicilan/PPB dari semester LAIN (mis. semester berubah lewat Excel upload)
	 *                 ikut bocor tampil di semester yang sedang dibuka (lihat catatan di countBulanan).
	 */
	@SuppressWarnings("unchecked")
	public static void fallbackTagihanDariCicilan(Object student, JenisKegiatan jenisKegiatan,
			List dataTagihanData, Map<Long, DetailBiaya> itemBiayas, Integer semester) {
		Object studentIdForLog = null;
		try {
			if (student instanceof Mahasiswa) {
				studentIdForLog = ((Mahasiswa) student).getId() + "-" + ((Mahasiswa) student).getNim();
			} else if (student instanceof BiodataCalonMahasiswa) {
				studentIdForLog = ((BiodataCalonMahasiswa) student).getId() + "-"
						+ ((BiodataCalonMahasiswa) student).getNoRegistrasi();
			}
		} catch (Exception ignoredLog) { ais.common.ErrorAuditUtil.record(ignoredLog, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:fallbackTagihanDariCicilan:log"); }
		System.out.println("[TAGIHAN-DEBUG] ==> fallbackTagihanDariCicilan student=" + studentIdForLog
				+ " jenisKegiatan=" + (jenisKegiatan == null ? "null" : jenisKegiatan.getId() + "-" + jenisKegiatan.getNamaKegiatan())
				+ " semester=" + semester + " dataTagihanData.isEmpty()="
				+ (dataTagihanData == null ? "null" : dataTagihanData.isEmpty()));

		if (dataTagihanData == null || !dataTagihanData.isEmpty() || student == null) {
			System.out.println(
					"[TAGIHAN-DEBUG] fallbackTagihanDariCicilan: DIBATALKAN lebih awal (dataTagihanData sudah berisi data lain, atau student null) -> fallback TIDAK dijalankan.");
			return;
		}
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Criteria fallbackCrit = session.createCriteria(CicilanPembayaran.class);
			Criteria kegCrit = fallbackCrit.createCriteria("kegiatan");
			if (student instanceof Mahasiswa) {
				kegCrit.add(Restrictions.eq("mahasiswa", student));
			} else {
				kegCrit.add(Restrictions.eq("calonMahasiswa", student));
			}
			if (jenisKegiatan != null && jenisKegiatan.getId() != null)
				kegCrit.add(Restrictions.eq("jenisKegiatan", jenisKegiatan));
			List<CicilanPembayaran> fallbackList = fallbackCrit.list();

			System.out.println("[TAGIHAN-DEBUG] fallbackTagihanDariCicilan: jumlah CicilanPembayaran ditemukan (semua jenisKegiatan+semester, belum difilter semester) = "
					+ (fallbackList == null ? 0 : fallbackList.size()));
			if (fallbackList != null) {
				for (CicilanPembayaran cpLog : fallbackList) {
					if (cpLog == null) {
						continue;
					}
					System.out.println("[TAGIHAN-DEBUG]   - cicilanId=" + cpLog.getId() + " ke=" + cpLog.getKe()
							+ " nilai=" + cpLog.getNilai() + " itemBiaya="
							+ (cpLog.getItemBiaya() == null ? "null" : cpLog.getItemBiaya().getId() + "-" + cpLog.getItemBiaya().getNama())
							+ " bayarKe=" + cpLog.getBayarKe() + " detailBiayaId="
							+ (cpLog.getDetailBiaya() == null ? "null (BELUM ber-FK ke DetailBiaya manapun)" : cpLog.getDetailBiaya().getId())
							+ " detailBiayaSemester="
							+ (cpLog.getDetailBiaya() == null ? "-" : cpLog.getDetailBiaya().getSemester())
							+ " detailBiayaSettingBiayaId="
							+ (cpLog.getDetailBiaya() == null || cpLog.getDetailBiaya().getSettingBiaya() == null ? "null"
									: cpLog.getDetailBiaya().getSettingBiaya().getId())
							+ " pengaturanPembayaranBulananId="
							+ (cpLog.getPengaturanPembayaranBulanan() == null ? "null" : cpLog.getPengaturanPembayaranBulanan().getId()));
				}
			}

			if (fallbackList == null || fallbackList.isEmpty()) {
				System.out.println("[TAGIHAN-DEBUG] fallbackTagihanDariCicilan: TIDAK ADA riwayat cicilan sama sekali -> keluar tanpa mengisi apa pun.");
				return;
			}

			// Tentukan mode angsuran: dari flag jenisKegiatan atau ada cicilan ber-ppb
			boolean isAngsuranMode = jenisKegiatan != null
					&& Boolean.TRUE.equals(jenisKegiatan.getHanyaBerupaAngsuran());
			if (!isAngsuranMode) {
				for (CicilanPembayaran cp : fallbackList) {
					if (cp.getPengaturanPembayaranBulanan() != null) {
						isAngsuranMode = true;
						break;
					}
				}
			}
			System.out.println("[TAGIHAN-DEBUG] fallbackTagihanDariCicilan: mode = " + (isAngsuranMode ? "ANGSURAN (PPB)" : "NON-ANGSURAN (DetailBiaya langsung)"));

			if (isAngsuranMode) {
				// === MODE ANGSURAN: kumpulkan PPB (hanya cicilan ber-ppb) ===
				Map<Long, PengaturanPembayaranBulanan> ppbMap = new HashMap<Long, PengaturanPembayaranBulanan>();
				for (CicilanPembayaran cp : fallbackList) {
					PengaturanPembayaranBulanan ppb = cp.getPengaturanPembayaranBulanan();
					if (ppb == null || ppb.getId() == null)
						continue;
					Integer dbSemester = ppb.getDetailBiaya() == null ? null : ppb.getDetailBiaya().getSemester();
					// PERBAIKAN "tagihan semester lain ikut muncul walau tidak relevan": SEBELUMNYA
					// dbSemester==null diperlakukan sbg wildcard (cocok semua semester), padahal
					// baris legacy tanpa semester (biasanya data lama sebelum field ini konsisten
					// diisi) jadi ikut nongol di SETIAP semester yg pernah dibuka -- terbukti dari
					// laporan nyata (baris yg sama muncul di smt 6 & smt 8). Kalau kita SEDANG minta
					// semester tertentu, baris yg semesternya tidak diketahui (null) TIDAK BOLEH
					// dianggap cocok -- lebih aman kosong drpd salah semester.
					if (semester != null && !semester.equals(dbSemester))
						continue;
					if (!ppbMap.containsKey(ppb.getId())) {
						ppbMap.put(ppb.getId(), ppb);
						dataTagihanData.add(ppb);
						if (itemBiayas != null && ppb.getDetailBiaya() != null
								&& ppb.getDetailBiaya().getId() != null)
							itemBiayas.put(ppb.getDetailBiaya().getId(), ppb.getDetailBiaya());
					}
				}
				// Perbaiki DetailBiaya.nilaiBiaya=0 dan PPB.nominal=0 dari sum nilaiAsli cicilan
				if (!ppbMap.isEmpty()) {
					Map<Long, Double> sumPerDb = new HashMap<Long, Double>();
					for (CicilanPembayaran cp : fallbackList) {
						PengaturanPembayaranBulanan ppb = cp.getPengaturanPembayaranBulanan();
						if (ppb == null || ppb.getDetailBiaya() == null || ppb.getDetailBiaya().getId() == null)
							continue;
						if (cp.getNilaiAsli() == null || cp.getNilaiAsli() < 0.001)
							continue;
						Long dbId = ppb.getDetailBiaya().getId();
						Double cur = sumPerDb.containsKey(dbId) ? sumPerDb.get(dbId) : 0.0;
						sumPerDb.put(dbId, cur + cp.getNilaiAsli());
					}
					if (!sumPerDb.isEmpty()) {
						Transaction txFix = null;
						try {
							txFix = session.beginTransaction();
							for (Map.Entry<Long, Double> entry : sumPerDb.entrySet()) {
								Long dbId = entry.getKey();
								double totalNilai = entry.getValue();
								DetailBiaya db = (DetailBiaya) session.get(DetailBiaya.class, dbId);
								if (db != null) {
									if (db.getNilaiBiaya() == null || db.getNilaiBiaya() < 0.01) {
										db.setNilaiBiaya(totalNilai);
										session.saveOrUpdate(db);
										if (itemBiayas != null) itemBiayas.put(dbId, db);
									}
									List<PengaturanPembayaranBulanan> ppbList = session
											.createCriteria(PengaturanPembayaranBulanan.class)
											.add(Restrictions.eq("detailBiaya", db)).list();
									for (PengaturanPembayaranBulanan ppbFix : ppbList) {
										if ((ppbFix.getNominal() == null || ppbFix.getNominal() < 0.01)
												&& ppbFix.getPersentase() != null && ppbFix.getPersentase() > 0) {
											ppbFix.setNominal(totalNilai * ppbFix.getPersentase() / 100.0);
											session.saveOrUpdate(ppbFix);
										}
									}
								}
							}
							txFix.commit();
						} catch (Exception eTx) {
							if (txFix != null)
								try { txFix.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1449");}
							Common.tampilErrorJikaAdmin(eTx);
						}
					}
				}
			} else {
				// === MODE NON-ANGSURAN: kumpulkan DetailBiaya (hanya cicilan tanpa ppb) ===
				Map<Long, DetailBiaya> fallbackMap = new HashMap<Long, DetailBiaya>();
				// PERBAIKAN "item tagihan tampil dobel di layar mahasiswa lama": dedup di sini
				// SEBELUMNYA memakai raw detail_biaya id sbg kunci -- kalau riwayat cicilan
				// mahasiswa ternyata terpecah di ANTARA DUA DetailBiaya berbeda utk Item Biaya
				// + bayarKe yg SAMA (mis. akibat duplikasi DetailBiaya lama sebelum "jaring
				// pengaman terakhir" di SetingBiayaHelper dipasang), method ini ikut menambahkan
				// KEDUA baris ke tampilan -- padahal DetailPembayaranMahasiswaRenderer sendiri
				// menjumlah "Dibayar" per (itemBiaya, bayarKe), BUKAN per detail_biaya id, jadi
				// menampilkan >1 baris utk kombinasi item+bayarKe yg sama murni duplikat tampilan
				// (masing-masing baris akan menampilkan TOTAL PEMBAYARAN YANG SAMA, seolah lunas
				// dobel), bukan mewakili tagihan yg benar-benar berbeda. Dedup sekarang JUGA per
				// (itemBiaya id + bayarKe) -- baris PERTAMA yg ditemukan (urutan dari fallbackList)
				// yang dipakai, sisanya dilewati & dicatat ke audit log agar admin/pengembang bisa
				// menelusuri baris DetailBiaya duplikat yg sebenarnya di database (idealnya baris
				// duplikat itu sendiri dibersihkan permanen, bukan cuma disembunyikan di sini).
				java.util.Set<String> itemBayarKeSudahAda = new java.util.HashSet<String>();
				for (CicilanPembayaran cp : fallbackList) {
					if (cp.getPengaturanPembayaranBulanan() != null) {
						System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] cicilanId=" + cp.getId()
								+ " DILEWATI (punya pengaturanPembayaranBulanan, seharusnya masuk mode angsuran)");
						continue;
					}
					if (cp.getDetailBiaya() == null || cp.getDetailBiaya().getId() == null) {
						System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] cicilanId=" + cp.getId()
								+ " DILEWATI (detailBiaya null/tanpa id -> cicilan ini TIDAK bisa dipetakan ke tagihan manapun)");
						continue;
					}
					Integer dbSemester = cp.getDetailBiaya().getSemester();
					// PERBAIKAN "tagihan semester lain ikut muncul walau tidak relevan" (lihat
					// komentar sama di mode angsuran di atas): dbSemester==null TIDAK LAGI
					// dianggap cocok utk semester tertentu -- baris legacy tanpa semester yg
					// jelas semestinya TIDAK ditampilkan sama sekali drpd salah semester.
					if (semester != null && !semester.equals(dbSemester)) {
						System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] cicilanId=" + cp.getId() + " detailBiayaId="
								+ cp.getDetailBiaya().getId() + " DILEWATI (semester DetailBiaya=" + dbSemester
								+ " != semester yg dicari=" + semester + ")");
						continue;
					}
					Long dbId = cp.getDetailBiaya().getId();
					if (fallbackMap.containsKey(dbId)) {
						System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] cicilanId=" + cp.getId() + " detailBiayaId="
								+ dbId + " -- DetailBiaya ini sudah diproses sebelumnya (cicilan lain miliknya), lanjut jumlahkan saja.");
						continue;
					}
					ItemBiaya itemBiayaCp = cp.getDetailBiaya().getItemBiaya();
					String itemBayarKeKey = (itemBiayaCp == null || itemBiayaCp.getId() == null ? "null"
							: itemBiayaCp.getId().toString()) + "_" + cp.getDetailBiaya().getBayarKe();
					System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] cicilanId=" + cp.getId() + " detailBiayaId=" + dbId
							+ " itemBiaya=" + (itemBiayaCp == null ? "null" : itemBiayaCp.getId() + "-" + itemBiayaCp.getNama())
							+ " bayarKe=" + cp.getDetailBiaya().getBayarKe() + " -> kunci dedup=\"" + itemBayarKeKey + "\""
							+ " | kunci ini " + (itemBayarKeSudahAda.contains(itemBayarKeKey) ? "SUDAH ADA sebelumnya" : "BELUM ADA (baris baru)"));
					if (itemBayarKeSudahAda.contains(itemBayarKeKey)) {
						System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] => DUPLIKAT TERDETEKSI: detailBiayaId=" + dbId
								+ " (kunci=" + itemBayarKeKey + ") DILEWATI dari tampilan, sudah ada baris lain utk item+bayarKe yg sama. INI KEMUNGKINAN BESAR PENYEBAB DOBEL YANG DILAPORKAN.");
						ais.common.ErrorAuditUtil.record(
								new Exception(
										"Duplikat DetailBiaya terdeteksi di fallbackTagihanDariCicilan (item+bayarKe="
												+ itemBayarKeKey + "): detailBiayaId=" + dbId
												+ " DILEWATI dari tampilan karena sudah ada baris lain utk item+bayarKe yg sama -- perlu dicek manual apakah baris DetailBiaya ini benar duplikat & sebaiknya dibersihkan."),
								"auto-audit(fallback-dedup-item-bayarke) src/ais/action/master/helper/PembayaranUtilHelper.java:fallbackTagihanDariCicilan");
						continue;
					}
					itemBayarKeSudahAda.add(itemBayarKeKey);
					fallbackMap.put(dbId, cp.getDetailBiaya());
					dataTagihanData.add(cp.getDetailBiaya());
					System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] => DetailBiaya id=" + dbId
							+ " DITAMBAHKAN ke dataTagihanData (nilaiBiaya saat ini=" + cp.getDetailBiaya().getNilaiBiaya() + ")");
					if (itemBiayas != null && cp.getDetailBiaya().getItemBiaya() != null)
						itemBiayas.put(dbId, cp.getDetailBiaya());
				}
				System.out.println("[TAGIHAN-DEBUG] fallbackTagihanDariCicilan: SELESAI mode non-angsuran -> dataTagihanData berisi "
						+ dataTagihanData.size() + " baris DetailBiaya.");
				// Perbaiki nilaiBiaya=0 dan PengaturanBulanan.nominal=0
				if (!fallbackMap.isEmpty()) {
					Map<Long, Double> sumNilaiMap = new HashMap<Long, Double>();
					for (CicilanPembayaran cp : fallbackList) {
						if (cp.getPengaturanPembayaranBulanan() != null)
							continue;
						if (cp.getDetailBiaya() != null && cp.getDetailBiaya().getId() != null
								&& cp.getNilaiAsli() != null && cp.getNilaiAsli() > 0.001) {
							Long dbId = cp.getDetailBiaya().getId();
							Double cur = sumNilaiMap.containsKey(dbId) ? sumNilaiMap.get(dbId) : 0.0;
							sumNilaiMap.put(dbId, cur + cp.getNilaiAsli());
						}
					}
					Transaction txFix = null;
					try {
						txFix = session.beginTransaction();
						for (Map.Entry<Long, DetailBiaya> entry : fallbackMap.entrySet()) {
							Long dbId = entry.getKey();
							DetailBiaya db = entry.getValue();
							if ((db.getNilaiBiaya() == null || db.getNilaiBiaya() < 0.01)
									&& sumNilaiMap.containsKey(dbId)) {
								double totalNilai = sumNilaiMap.get(dbId);
								db.setNilaiBiaya(totalNilai);
								session.saveOrUpdate(db);
								List<PengaturanPembayaranBulanan> ppbList = session
										.createCriteria(PengaturanPembayaranBulanan.class)
										.add(Restrictions.eq("detailBiaya", db)).list();
								for (PengaturanPembayaranBulanan ppb : ppbList) {
									if ((ppb.getNominal() == null || ppb.getNominal() < 0.01)
											&& ppb.getPersentase() != null && ppb.getPersentase() > 0) {
										ppb.setNominal(totalNilai * ppb.getPersentase() / 100.0);
										session.saveOrUpdate(ppb);
									}
								}
							}
						}
						txFix.commit();
					} catch (Exception eTx) {
						if (txFix != null)
							try { txFix.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1509");}
						Common.tampilErrorJikaAdmin(eTx);
					}
				}
			}
		} catch (Exception eFb) {
			Common.tampilErrorJikaAdmin(eFb);
		} finally {
			try { if (session != null) session.clear(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1517");}
			try { if (session != null) session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1518");}
			try { if (session != null) session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1519");}
		}
	}
}
