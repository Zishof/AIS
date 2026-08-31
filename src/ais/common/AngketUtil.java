package ais.common;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Messagebox;

import ais.action.master.helper.generic.AngketDosenWindow;
import ais.action.master.helper.generic.AngketGuruWindow;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BlokirMahasiswa;
import ais.database.model.ChecklistBaruPenilaianDosenOlehMahasiswa;
import ais.database.model.ChecklistPenilaianDosen;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KonfigurasiKalenderAkademik;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.BlokirSiswa;
import ais.database.model.sekolah.ChecklistBaruPenilaianGuruOlehSiswa;
import ais.database.model.sekolah.ChecklistPenilaianGuru;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Kelas utilitas statis untuk mekanisme "gerbang" (gate) angket/kuesioner penilaian dosen oleh
 * mahasiswa dan penilaian guru oleh siswa: memeriksa apakah pengguna (mahasiswa/siswa) masih punya
 * kewajiban mengisi angket penilaian yang belum lengkap untuk periode akademik berjalan, dan bila
 * ya, memaksa jendela pengisian angket muncul secara modal sebelum pengguna dapat melanjutkan
 * mengakses aplikasi. Kelas ini dipanggil dari alur login/inisialisasi sesi akademik — lihat
 * {@link #checkAngket(Mahasiswa, int)} dan {@link #checkAngket(Siswa)} sebagai titik masuk utama.
 *
 * <h2>Konsep "checklist" penilaian</h2>
 * <p>
 * Setiap perkuliahan/jadwal pelajaran yang diikuti mahasiswa/siswa pada periode tertentu memiliki
 * satu atau lebih dosen/guru pengajar. Untuk setiap kombinasi (dosen/guru, perkuliahan/jadwal),
 * sistem mendefinisikan aturan penilaian ({@link ChecklistPenilaianDosen}/
 * {@link ChecklistPenilaianGuru}) yang berlaku berdasarkan filter fakultas/jurusan (atau
 * yayasan/sekolah untuk siswa), program studi, dan angkatan. {@link #checkStatusChecklist} menyusun
 * seluruh kombinasi (id-aturan, dosen/guru-perkuliahan) yang <i>seharusnya</i> sudah dinilai, lalu
 * membandingkannya dengan kombinasi yang <i>sudah benar-benar dinilai</i> (diambil dari
 * {@link Mahasiswa#ambilChecklistBaruPenilaianDosenOlehMahasiswa}/
 * {@link Siswa#ambilChecklistBaruPenilaianGuruOlehSiswa}). Bila ada satu saja kombinasi yang belum
 * dinilai, method mengembalikan {@code true} (masih ada kewajiban) sehingga jendela angket
 * ditampilkan.
 * </p>
 *
 * <h2>Alur pemblokiran dan penentuan periode</h2>
 * <p>
 * {@link #checkAngket(Mahasiswa, int)} pertama-tama memeriksa apakah mahasiswa sedang diblokir
 * login lewat {@link BlokirMahasiswa} — bila ya, sesi langsung diinvalidasi dan pengguna dilempar
 * keluar (logoff) tanpa sempat memeriksa angket sama sekali. Bila tidak diblokir, method
 * menentukan periode/jenis semester (ganjil/genap/semester pendek) yang relevan lewat dua jalur
 * berbeda tergantung konfigurasi {@code input_angket_penilaian_dosen_harus_berdasarkan_kalender_akademik}:
 * jalur "kalender akademik" (mencari konfigurasi yang cocok dengan {@link KonfigurasiKalenderAkademik}
 * aktif saat ini) atau jalur "semester berjalan" (langsung memakai {@link Common#getKonfigurasi}
 * berbasis tahun akademik dan jenis semester saat ini serta semester sebelumnya). Kedua jalur sama-
 * sama dapat memicu jendela angket untuk periode semester berjalan, semester pendek (SP), maupun
 * satu semester ke belakang.
 * </p>
 * <p>
 * {@link #checkAngket(Siswa)} memakai pola serupa untuk siswa: memeriksa {@link BlokirSiswa},
 * menentukan periode lewat {@link KonfigurasiKalenderAkademik} teraktif untuk yayasan/sekolah siswa
 * tersebut (fallback ke tahun akademik/semester saat ini bila tidak ada kalender yang cocok), lalu
 * memanggil {@link #displayPenilaianAngket(Siswa, String, String)} bila angket sedang berlangsung.
 * </p>
 *
 * <h2>Tampilan jendela angket</h2>
 * <p>
 * {@link #displayPenilaianAngket(Mahasiswa, String, String, int, Integer)} dan
 * {@link #displayPenilaianAngket(Siswa, String, String)} membuka jendela ZK modal berisi
 * {@code AngketDosenWindow}/{@code AngketGuruWindow}, dan bila jendela tersebut tidak/belum
 * menyelesaikan seluruh pengisian, timer default ({@link Common#createDefaultTimer}) dijadwalkan
 * untuk menampilkan pesan peringatan informatif kepada pengguna. Kedua method mengembalikan
 * {@code false} bila jendela angket ditampilkan (menandakan alur pemanggil harus berhenti/menunggu),
 * dan {@code true} bila tidak ada kewajiban pengisian tersisa (alur boleh lanjut).
 * </p>
 *
 * <p>
 * Seluruh pekerjaan berat (query checklist, pembukaan jendela) dijalankan lewat
 * {@link Common#createDefaultTimer(EventListener)} — pola umum di UI ZK untuk menunda eksekusi
 * hingga setelah render awal komponen selesai, sehingga pengguna tidak melihat jendela kosong/UI
 * yang belum siap saat gerbang angket dievaluasi.
 * </p>
 */
public class AngketUtil {

	/**
	 * Menampilkan jendela angket penilaian guru untuk siswa bila statusnya masih memiliki kewajiban
	 * pengisian ({@link #checkStatusChecklist(Siswa, String, String)} bernilai {@code true}).
	 * Jendela dibangun dari jadwal pelajaran yang diikuti siswa pada tahun ajaran/semester tersebut
	 * (baik kelas reguler maupun kelas les) memakai {@code AngketGuruWindow}, ditampilkan modal, dan
	 * bila tidak segera dilengkapi, timer default menampilkan pesan peringatan berisi instruksi
	 * langkah-langkah melengkapi angket.
	 *
	 * @param siswa siswa yang statusnya diperiksa
	 * @param ta    tahun ajaran yang diperiksa
	 * @param jenis jenis semester ({@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP})
	 * @return {@code false} bila jendela angket ditampilkan (masih ada kewajiban pengisian);
	 *         {@code true} bila tidak ada kewajiban tersisa
	 * @throws Exception diteruskan dari operasi ZK/Hibernate di dalamnya
	 */
	@SuppressWarnings("unchecked")
	public static boolean displayPenilaianAngket(final Siswa siswa, final String ta, final String jenis)
			throws Exception {
		// TODO Auto-generated method stub

		if (AngketUtil.checkStatusChecklist(siswa, ta, jenis)) {

			MyWindow addWindow = new MyWindow("", "none", false);
			addWindow.setHeight("97%");
			addWindow.setWidth("900px");
			addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("97%");
			window.setWidth("97%");

			Session session = HibernateUtil.currentSession();

			String sql = "this_.kelas_id in (select kelas_id from sekolah.kelas_punya_siswa where siswa_id="
					+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";
			Criterion criterionKls = Restrictions.sqlRestriction(sql);

			sql = "this_.kelas_les_siswa in (select kelas_id from sekolah.kelas_les_punya_siswa where siswa_id="
					+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";
			Criterion criterionLes = Restrictions.sqlRestriction(sql);

			List<Long> jadwalPelajarans = session.createCriteria(JadwalPelajaran.class)
					.add(Restrictions.or(criterionKls, criterionLes)).add(Restrictions.eq("tahunAjaran", ta))
					.setProjection(Projections.property("id"))
					.add(Restrictions.eq("semester", jenis.equals(Perkuliahan.GANJIL) ? 1 : 2)).list();

			AngketGuruWindow angketDosenWindow = new AngketGuruWindow(ta, jenis, jadwalPelajarans, siswa, addWindow,
					true);
			angketDosenWindow.setParent(Common.tampilanScroll(window));
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.onModal();

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Bapak/Ibu. Penilaian Angket Guru untuk tahun akademik {V1} / {V2} sebagian atau seluruhnya belum Bapak/Ibu lakukan. Sebelum dapat melanjutkan akses aplikasi akademik ini, mohon Bapak/Ibu terlebih dahulu melengkapi pengisian Angket Guru. Langkah yang dapat dilakukan: (1) buka menu Angket Guru; (2) lengkapi seluruh penilaian yang masih kosong; (3) simpan penilaian, kemudian ulangi akses aplikasi akademik.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, ta, jenis);
				}
			});

			return false;
		} else {
			return true;
		}
	}

	/**
	 * Menampilkan jendela angket penilaian dosen untuk mahasiswa bila statusnya masih memiliki
	 * kewajiban pengisian ({@link #checkStatusChecklist(Mahasiswa, int, Integer)} bernilai
	 * {@code true}). Berbeda dari varian siswa, pembangunan jendela di sini dilakukan di dalam timer
	 * default ({@link Common#createDefaultTimer}) itu sendiri (bukan langsung), dan jendela
	 * {@code AngketDosenWindow} dibangun dari daftar perkuliahan+paralel yang diikuti mahasiswa
	 * ({@link Mahasiswa#ambilPerkuliahanDanParalel(int, Integer)}).
	 *
	 * @param mahasiswa mahasiswa yang statusnya diperiksa
	 * @param ta        tahun ajaran yang diperiksa
	 * @param jenis     jenis semester (ganjil/genap/semester pendek)
	 * @param semester  nomor semester berjalan mahasiswa
	 * @param sp        penanda semester pendek ({@link Perkuliahan#SEMESTER_PENDEK}), atau
	 *                  {@code null} untuk semester reguler
	 * @return {@code false} bila jendela angket dijadwalkan tampil (masih ada kewajiban pengisian);
	 *         {@code true} bila tidak ada kewajiban tersisa
	 * @throws Exception diteruskan dari operasi ZK/Hibernate di dalamnya
	 */
	public static boolean displayPenilaianAngket(final Mahasiswa mahasiswa, final String ta, final String jenis,
			final int semester, final Integer sp) throws Exception {
		// TODO Auto-generated method stub

		if (AngketUtil.checkStatusChecklist(mahasiswa, semester, sp)) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyWindow addWindow = new MyWindow("", "none", false);
					addWindow.setHeight("97%");
					addWindow.setWidth("900px");
					addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

					MyWindow window = new MyWindow("", "none", false);
					window.setHeight("97%");
					window.setWidth("97%");

					List<Long> perkuliahans = mahasiswa.ambilPerkuliahanDanParalel(semester, sp);
					AngketDosenWindow angketDosenWindow = new AngketDosenWindow(ta, jenis, perkuliahans, mahasiswa,
							addWindow, true);
					angketDosenWindow.setParent(Common.tampilanScroll(window));
					window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					window.onModal();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							MyMessageboxConfig.showFormat(
									"Mohon maaf, Bapak/Ibu. Penilaian Angket Dosen untuk tahun akademik {V1} / {V2} sebagian atau seluruhnya belum Bapak/Ibu lakukan. Sebelum dapat melanjutkan akses aplikasi akademik ini, mohon Bapak/Ibu terlebih dahulu melengkapi pengisian Angket Dosen. Langkah yang dapat dilakukan: (1) buka menu Angket Dosen; (2) lengkapi seluruh penilaian yang masih kosong; (3) simpan penilaian, kemudian ulangi akses aplikasi akademik.",
									"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, ta, jenis);
						}
					});
				}
			});

			return false;
		} else {
			return true;
		}
	}

	/**
	 * Memeriksa apakah {@code mahasiswa} masih memiliki kewajiban pengisian angket penilaian dosen
	 * yang belum lengkap untuk {@code semester}/{@code sp} tertentu. Menyusun seluruh kombinasi
	 * (id-aturan {@link ChecklistPenilaianDosen}, dosen-perkuliahan) yang seharusnya dinilai
	 * berdasarkan filter fakultas/jurusan/program/angkatan mahasiswa, lalu membandingkannya dengan
	 * kombinasi yang sudah tersimpan di
	 * {@link Mahasiswa#ambilChecklistBaruPenilaianDosenOlehMahasiswa(Session, boolean)}.
	 *
	 * @param mahasiswa mahasiswa yang diperiksa
	 * @param semester  nomor semester berjalan (dipakai mengambil daftar perkuliahan+paralel)
	 * @param sp        penanda semester pendek, atau {@code null}
	 * @return {@code true} bila ada minimal satu kombinasi (aturan, dosen-perkuliahan) yang belum
	 *         dinilai; {@code false} bila tidak ada perkuliahan/aturan yang berlaku, atau seluruh
	 *         kewajiban sudah terpenuhi
	 */
	@SuppressWarnings("unchecked")
	public static Boolean checkStatusChecklist(Mahasiswa mahasiswa, int semester, Integer sp) {

		// JamPerkuliahanSyncrhonizerProcessor.processChecklistPenilaianDosenOlehMahasiswa(mahasiswa);

		Fakultas fakultas = mahasiswa.getJurusan().getFakultas();
		Jurusan jurusan = mahasiswa.getJurusan();
		String program = mahasiswa.getProgram();
		String angkatan = mahasiswa.getTahunangkatan() == null ? "" : mahasiswa.getTahunangkatan().toString();

		List<Long> perkuliahans = mahasiswa.ambilPerkuliahanDanParalel(semester, sp);

		// // System.out.println("Login mahasiswa " + mahasiswa + ", jenis semester
		// " + semester + ", sp " + sp
		// + ", perkuliahans " + perkuliahans.size());
		List<String> dataperkuliahan = new ArrayList<String>();
		for (Long perkuliahanid : perkuliahans) {
			Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(), perkuliahanid);
			if (perkuliahan != null) {
				List<Dosen> dosens = perkuliahan.populateDosenBuNama();
				for (Dosen dosen : dosens) {
					dataperkuliahan.add(dosen.getId() + "-" + perkuliahan.getId());
				}
				dosens = null;
			}
		}

		// // System.out.println("Login mahasiswa " + mahasiswa + "
		// dataperkuliahan" + dataperkuliahan);

		if (dataperkuliahan.isEmpty()) {
			return false;
		}

		Session session = HibernateUtil.currentSession();
		List<Long> dataPenilaian = session.createCriteria(ChecklistPenilaianDosen.class)
				.createAlias("grupChecklistPenilaianDosen", "grupChecklistPenilaianDosen")
				.createAlias("grupChecklistPenilaianDosen.angketPenilaianDosen", "angketPenilaianDosen")

				.add(Restrictions.or(Restrictions.isNull("angketPenilaianDosen.untukMahasiswa"),
						Restrictions.eq("angketPenilaianDosen.untukMahasiswa", true)))

				.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.fakultas", fakultas),
						Restrictions.isNull("angketPenilaianDosen.fakultas")))

				.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.jurusan", jurusan),
						Restrictions.isNull("angketPenilaianDosen.jurusan")))

				.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", ""),
						Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", program),
								Restrictions.isNull("angketPenilaianDosen.program"))))

				.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.angkatan", ""),
						Restrictions.or(Restrictions.ilike("angketPenilaianDosen.angkatan", angkatan),
								Restrictions.isNull("angketPenilaianDosen.angkatan"))))

				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
				.add(Restrictions.or(Restrictions.eq("grupChecklistPenilaianDosen.aktif", true),
						Restrictions.isNull("grupChecklistPenilaianDosen.aktif")))
				.setProjection(Projections.groupProperty("id")).list();

		// // System.out.println("Login mahasiswa " + mahasiswa + " dataPenilaian "
		// + dataPenilaian);

		if (dataPenilaian.isEmpty()) {
			return false;
		}

		Map<String, ChecklistBaruPenilaianDosenOlehMahasiswa> map = mahasiswa
				.ambilChecklistBaruPenilaianDosenOlehMahasiswa(session, false);

		Set<String> datakey = map.keySet();
		Set<String> data = new HashSet<String>();
		for (String key : datakey) {
			ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa = map.get(key);
			List<Object[]> objectss = checklistBaruPenilaianDosenOlehMahasiswa.ambilValue();
			for (Object[] object : objectss) {
				Long idnilai = (Long) (object.length > 0 ? object[0] : -1L);
				data.add(idnilai + "-" + key);
			}
		}

		// // System.out.println("Login mahasiswa " + mahasiswa + " datakey " +
		// datakey + " data tersimpan " + data);

		for (Long idnilai : dataPenilaian) {
			for (String s : dataperkuliahan) {
				String key = idnilai + "-" + s;
				if (!data.contains(key)) {
					// // System.out.println(mahasiswa + ", key = " + key + " belum
					// ada");
					map = null;
					data = null;
					dataPenilaian = null;
					dataperkuliahan = null;
					perkuliahans = null;
					return true;
				}
			}
		}
		map = null;
		data = null;
		dataPenilaian = null;
		dataperkuliahan = null;
		perkuliahans = null;
		return false;
	}

	/**
	 * Varian siswa dari {@link #checkStatusChecklist(Mahasiswa, int, Integer)}: memeriksa apakah
	 * {@code siswa} masih memiliki kewajiban pengisian angket penilaian guru yang belum lengkap
	 * untuk tahun ajaran {@code ta} dan jenis semester {@code smt}. Kombinasi (id-aturan
	 * {@link ChecklistPenilaianGuru}, guru-jadwal pelajaran) yang seharusnya dinilai disusun
	 * berdasarkan filter yayasan/sekolah/program/angkatan siswa (mencakup kelas reguler maupun kelas
	 * les), lalu dibandingkan dengan kombinasi tersimpan di
	 * {@link Siswa#ambilChecklistBaruPenilaianGuruOlehSiswa(Session, boolean)}.
	 *
	 * @param siswa siswa yang diperiksa
	 * @param ta    tahun ajaran yang diperiksa
	 * @param smt   jenis semester ({@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP})
	 * @return {@code true} bila ada minimal satu kombinasi yang belum dinilai; {@code false} bila
	 *         tidak ada jadwal/aturan yang berlaku, atau seluruh kewajiban sudah terpenuhi
	 */
	@SuppressWarnings("unchecked")
	public static Boolean checkStatusChecklist(Siswa siswa, String ta, String smt) {

		Yayasan yayasan = siswa.getSekolah().getYayasan();
		Sekolah sekolah = siswa.getSekolah();
		String program = siswa.getProgram();
		String angkatan = siswa.getTahunMasuk() == null ? "" : siswa.getTahunMasuk().toString();

		String sql = "this_.kelas_id in (select kelas_id from sekolah.kelas_punya_siswa where siswa_id=" + siswa.getId()
				+ " and kelas_id is not null and aktif=true group by kelas_id)";
		Criterion criterionKls = Restrictions.sqlRestriction(sql);

		sql = "this_.kelas_les_siswa in (select kelas_id from sekolah.kelas_les_punya_siswa where siswa_id="
				+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";
		Criterion criterionLes = Restrictions.sqlRestriction(sql);

		Session session = HibernateUtil.currentSession();
		List<Long> jadwalPelajarans = session.createCriteria(JadwalPelajaran.class)
				.add(Restrictions.or(criterionKls, criterionLes)).add(Restrictions.eq("tahunAjaran", ta))
				.setProjection(Projections.property("id"))
				.add(Restrictions.eq("semester", smt.equals(Perkuliahan.GANJIL) ? 1 : 2)).list();

		System.out.println("Login siswa " + siswa + ", jenis semester " + smt + " " + ta + ", jadwalPelajarans "
				+ jadwalPelajarans.size());
		List<String> dataperkuliahan = new ArrayList<String>();
		for (Long jadwalPelajaranid : jadwalPelajarans) {
			JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) ConstantValues.ambil(JadwalPelajaran.class.getName(),
					jadwalPelajaranid);
			if (jadwalPelajaran != null) {
				List<Guru> gurus = jadwalPelajaran.populateGuruBuNama();
				for (Guru guru : gurus) {
					dataperkuliahan.add(guru.getId() + "-" + jadwalPelajaran.getId());
				}
				gurus = null;
			}
		}

		System.out.println("Login siswa " + siswa + " dataperkuliahan" + dataperkuliahan);

		if (dataperkuliahan.isEmpty()) {
			return false;
		}

		List<Long> dataPenilaian = session.createCriteria(ChecklistPenilaianGuru.class)
				.createAlias("grupChecklistPenilaianGuru", "grupChecklistPenilaianGuru")
				.createAlias("grupChecklistPenilaianGuru.angketPenilaianGuru", "angketPenilaianGuru")

				.add(Restrictions.or(Restrictions.isNull("angketPenilaianGuru.untukSiswa"),
						Restrictions.eq("angketPenilaianGuru.untukSiswa", true)))

				.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.yayasan", yayasan),
						Restrictions.isNull("angketPenilaianGuru.yayasan")))

				.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.sekolah", sekolah),
						Restrictions.isNull("angketPenilaianGuru.sekolah")))

				.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.program", ""),
						Restrictions.or(Restrictions.eq("angketPenilaianGuru.program", program),
								Restrictions.isNull("angketPenilaianGuru.program"))))

				.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.angkatan", ""),
						Restrictions.or(Restrictions.ilike("angketPenilaianGuru.angkatan", angkatan),
								Restrictions.isNull("angketPenilaianGuru.angkatan"))))

				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
				.add(Restrictions.or(Restrictions.eq("grupChecklistPenilaianGuru.aktif", true),
						Restrictions.isNull("grupChecklistPenilaianGuru.aktif")))
				.setProjection(Projections.groupProperty("id")).list();

		// // System.out.println("Login siswa " + siswa + " dataPenilaian "
		// + dataPenilaian);

		if (dataPenilaian.isEmpty()) {
			return false;
		}

		Map<String, ChecklistBaruPenilaianGuruOlehSiswa> map = siswa.ambilChecklistBaruPenilaianGuruOlehSiswa(session,
				false);

		Set<String> datakey = map.keySet();
		Set<String> data = new HashSet<String>();
		for (String key : datakey) {
			ChecklistBaruPenilaianGuruOlehSiswa checklistBaruPenilaianGuruOlehSiswa = map.get(key);
			List<Object[]> objectss = checklistBaruPenilaianGuruOlehSiswa.ambilValue();
			for (Object[] object : objectss) {
				Long idnilai = (Long) (object.length > 0 ? object[0] : -1L);
				data.add(idnilai + "-" + key);
			}
		}

		// // System.out.println("Login siswa " + siswa + " datakey " +
		// datakey + " data tersimpan " + data);

		for (Long idnilai : dataPenilaian) {
			for (String s : dataperkuliahan) {
				String key = idnilai + "-" + s;
				if (!data.contains(key)) {
					// // System.out.println(siswa + ", key = " + key + " belum
					// ada");
					map = null;
					data = null;
					dataPenilaian = null;
					dataperkuliahan = null;
					jadwalPelajarans = null;
					return true;
				}
			}
		}
		map = null;
		data = null;
		dataPenilaian = null;
		dataperkuliahan = null;
		jadwalPelajarans = null;
		return false;
	}

	/**
	 * Titik masuk utama gerbang angket untuk mahasiswa, dipanggil pada alur login/inisialisasi sesi
	 * akademik. Alurnya: (1) periksa apakah mahasiswa sedang diblokir login lewat
	 * {@link BlokirMahasiswa} — bila ya, sesi diinvalidasi dan pengguna dilogoff paksa tanpa
	 * pemeriksaan angket; (2) bila tidak diblokir, tentukan periode yang perlu diperiksa (semester
	 * berjalan, semester pendek, dan satu semester sebelumnya) memakai salah satu dari dua strategi
	 * tergantung konfigurasi {@code input_angket_penilaian_dosen_harus_berdasarkan_kalender_akademik}
	 * — berbasis {@link KonfigurasiKalenderAkademik} bila aktif, atau berbasis
	 * {@link Common#getKonfigurasi} langsung bila tidak; (3) untuk tiap periode yang sedang
	 * berlangsung (menurut konfigurasi terkait), panggil {@link #displayPenilaianAngket(Mahasiswa,
	 * String, String, int, Integer)} — begitu salah satu pemanggilan mengembalikan {@code false}
	 * (jendela angket ditampilkan), method langsung berhenti tanpa memeriksa periode berikutnya.
	 * Seluruh pekerjaan dijalankan di dalam {@link Common#createDefaultTimer(EventListener)}.
	 *
	 * @param mahasiswa  mahasiswa yang sedang login/aktif sesinya
	 * @param currentSmt nomor semester berjalan mahasiswa saat ini
	 */
	public static void checkAngket(final Mahasiswa mahasiswa, final int currentSmt) {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();

				List<String> alasans = new ArrayList<String>();

				for (Object o : ConstantValues.ambilBerdasarClass(BlokirMahasiswa.class).values()) {
					BlokirMahasiswa blokirMahasiswa = (BlokirMahasiswa) o;
					if (blokirMahasiswa.getAktif() && blokirMahasiswa.getLogin()
							&& blokirMahasiswa.getMahasiswa() != null && mahasiswa != null
							&& blokirMahasiswa.getMahasiswa().getId().equals(mahasiswa.getId())) {
						alasans.add(blokirMahasiswa.getKeterangan());
					}
				}

				if (!alasans.isEmpty()) {

					String alas = "";
					for (String s : alasans) {
						alas += alas.isEmpty() ? s : "\n\n" + s;
					}
					final String a = alas;
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Sessions.getCurrent().invalidate();
							MyMessageboxConfig.show(a, "Informasi Login", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.goLogoff();
										}
									});
						}
					}, "", false, 2000);

					return;
				}

				int tahunAngkatanMhs = mahasiswa.getTahunangkatan();
				String ta = Common.getCurrentTahunAkademik();

				if (Common.bolehKonfigurasi("input_angket_penilaian_dosen_harus_berdasarkan_kalender_akademik", Konfigurasi.TIDAK_AKTIF)) {
					String jenis = currentSmt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
					Konfigurasi konfigurasi = Common.checkKonfigurasiDenganKalenderAkademik(session,
							"checklist_penilaian_dosen_semester_berlangsung", ta, jenis, mahasiswa.getSemesterMulai(),
							mahasiswa.getJurusan().getFakultas(), mahasiswa.getJurusan(), mahasiswa.getProgram());
					boolean angketBerlangsung = konfigurasi != null && konfigurasi.getNilai().equals(Konfigurasi.AKTIF);
					// System.out.println("kalender angketBerlangsung -> " + angketBerlangsung + "
					// jenis " + jenis);
					if (angketBerlangsung) {
						if (!AngketUtil.displayPenilaianAngket(mahasiswa, ta, jenis, currentSmt, null)) {
							return;
						}
					}

					konfigurasi = Common.checkKonfigurasiDenganKalenderAkademik(session,
							"checklist_penilaian_dosen_semester_berlangsung", Common.getCurrentTahunAkademik(),
							Perkuliahan.SP, mahasiswa.getSemesterMulai(), mahasiswa.getJurusan().getFakultas(),
							mahasiswa.getJurusan(), mahasiswa.getProgram());
					boolean angketBerlangsungSp = konfigurasi != null
							&& konfigurasi.getNilai().equals(Konfigurasi.AKTIF);
					// System.out.println("kalender angketBerlangsungSp -> " + angketBerlangsungSp);
					if (angketBerlangsungSp) {
						if (!AngketUtil.displayPenilaianAngket(mahasiswa, ta, Perkuliahan.SP, currentSmt,
								Perkuliahan.SEMESTER_PENDEK)) {
							return;
						}
					}

					Konfigurasi adaYgAktif = Common.checkKonfigurasiDenganKalenderAkademikAktif(session,
							"checklist_penilaian_dosen", mahasiswa.getSemesterMulai(),
							mahasiswa.getJurusan().getFakultas(), mahasiswa.getJurusan(), mahasiswa.getProgram());

					System.out.println("kalender adaYgAktif -> " + adaYgAktif);

					if (adaYgAktif != null && adaYgAktif.getInfo1() != null && !adaYgAktif.getInfo1().isEmpty()
							&& adaYgAktif.getTahunAkademik() != null && !adaYgAktif.getTahunAkademik().isEmpty()) {

						String tahunAjaran = adaYgAktif.getTahunAkademik();
						jenis = adaYgAktif.getInfo1();

						int smtData = Common.getSemester(mahasiswa.getTahunangkatan(), tahunAjaran, jenis,
								mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

						System.out.println("kalender smtData -> " + smtData);

						if (smtData >= 1) {
							if (!AngketUtil.displayPenilaianAngket(mahasiswa, tahunAjaran, jenis, smtData, null)) {
								return;
							}
						}
					}

					int smt = currentSmt - 1;
					if (smt >= 1) {

						jenis = smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
						Integer tahunAkademikMulai = Common.getTahunAkademik(smt, tahunAngkatanMhs,
								mahasiswa.getSemesterMulai());
						String tahunAjaran = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

						konfigurasi = Common.checkKonfigurasiDenganKalenderAkademik(session,
								"checklist_penilaian_dosen", tahunAjaran, jenis, mahasiswa.getSemesterMulai(),
								mahasiswa.getJurusan().getFakultas(), mahasiswa.getJurusan(), mahasiswa.getProgram());
						boolean angket = konfigurasi != null && konfigurasi.getNilai().equals(Konfigurasi.AKTIF);
						// System.out.println("kalender angket -> " + angket + " jenis " + jenis);
						if (angket) {
							if (!AngketUtil.displayPenilaianAngket(mahasiswa, tahunAjaran, jenis, smt, null)) {
								return;
							}
						}

						konfigurasi = Common.checkKonfigurasiDenganKalenderAkademik(session,
								"checklist_penilaian_dosen", tahunAjaran, Perkuliahan.SP, mahasiswa.getSemesterMulai(),
								mahasiswa.getJurusan().getFakultas(), mahasiswa.getJurusan(), mahasiswa.getProgram());
						boolean angketSp = konfigurasi != null && konfigurasi.getNilai().equals(Konfigurasi.AKTIF);
						// System.out.println("kalender angketSp -> " + angketSp + " jenis " + jenis);
						if (angketSp) {
							if (!AngketUtil.displayPenilaianAngket(mahasiswa, tahunAjaran, Perkuliahan.SP, smt,
									Perkuliahan.SEMESTER_PENDEK)) {
								return;
							}
						}
					}
				}

				else {

					try {
						String jenis = currentSmt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
						boolean angketBerlangsung = Common
								.getKonfigurasi("checklist_penilaian_dosen_semester_berlangsung",
										Common.getCurrentTahunAkademik(), jenis)
								.getNilai().equals(Konfigurasi.AKTIF);
						// System.out.println("angketBerlangsung -> " + angketBerlangsung);
						if (angketBerlangsung) {
							if (!AngketUtil.displayPenilaianAngket(mahasiswa, ta, jenis, currentSmt, null)) {
								return;
							}
						}

						boolean angketBerlangsungSp = Common
								.getKonfigurasi("checklist_penilaian_dosen_semester_berlangsung",
										Common.getCurrentTahunAkademik(), Perkuliahan.SP)
								.getNilai().equals(Konfigurasi.AKTIF);

						if (angketBerlangsungSp) {
							if (!AngketUtil.displayPenilaianAngket(mahasiswa, ta, Perkuliahan.SP, currentSmt,
									Perkuliahan.SEMESTER_PENDEK)) {
								return;
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AngketUtil.java:531");
					}

					int smt = currentSmt - 1;

					if (smt >= 1) {
						String jenis = smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
						Integer tahunAkademikMulai = Common.getTahunAkademik(smt, tahunAngkatanMhs,
								mahasiswa.getSemesterMulai());
						String tahunAjaran = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
						Konfigurasi konfigurasi = Common.getKonfigurasi("checklist_penilaian_dosen", tahunAjaran,
								jenis);
						boolean angket = konfigurasi.getNilai().equals(Konfigurasi.AKTIF);
						// System.out.println("angket -> " + angket);
						if (angket) {
							if (!AngketUtil.displayPenilaianAngket(mahasiswa, tahunAjaran, jenis + " jenis " + jenis,
									smt, null)) {
								return;
							}
						}

						konfigurasi = Common.getKonfigurasi("checklist_penilaian_dosen", tahunAjaran, Perkuliahan.SP);
						boolean angketSp = konfigurasi.getNilai().equals(Konfigurasi.AKTIF);
						// System.out.println("angketSp -> " + angketSp);
						if (angketSp) {
							if (!AngketUtil.displayPenilaianAngket(mahasiswa, tahunAjaran, Perkuliahan.SP, smt,
									Perkuliahan.SEMESTER_PENDEK)) {
								return;
							}
						}
					}
				}
			}
		});
	}

	/**
	 * Varian siswa dari {@link #checkAngket(Mahasiswa, int)}, titik masuk gerbang angket penilaian
	 * guru pada alur login/inisialisasi sesi siswa. Alurnya: (1) periksa {@link BlokirSiswa} aktif
	 * untuk siswa tersebut yang menandakan login diblokir — bila ada, sesi diinvalidasi dan pengguna
	 * dilogoff paksa; (2) bila tidak diblokir, cari {@link KonfigurasiKalenderAkademik} teraktif
	 * (rentang tanggal mencakup hari ini) untuk yayasan/sekolah siswa dengan kunci konfigurasi
	 * {@code checklist_penilaian_guru} — bila ditemukan, tahun ajaran dan jenis semester diambil dari
	 * kalender tersebut; bila tidak, fallback ke tahun akademik/semester saat ini
	 * ({@link Common#getCurrentTahunAkademik()}/{@link Common#isNowSemensterGanjil()}); (3) bila
	 * konfigurasi menandakan angket sedang berlangsung ({@code AKTIF}), panggil
	 * {@link #displayPenilaianAngket(Siswa, String, String)}. Seluruh pekerjaan dijalankan di dalam
	 * {@link Common#createDefaultTimer(EventListener)}.
	 *
	 * @param siswa siswa yang sedang login/aktif sesinya
	 */
	public static void checkAngket(final Siswa siswa) {
		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();

				List<String> alasans = session.createCriteria(BlokirSiswa.class)
						.add(Restrictions.isNotNull("keterangan")).add(Restrictions.ne("keterangan", ""))
						.setProjection(Projections.property("keterangan")).add(Restrictions.eq("siswa", siswa))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("login", true)).list();
				if (!alasans.isEmpty()) {
					Sessions.getCurrent().invalidate();
					String alas = "";
					for (String s : alasans) {
						alas += alas.isEmpty() ? s : "\n\n" + s;
					}

					MyMessageboxConfig.show(alas, "Informasi Login", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Common.goLogoff();
								}
							});

					return;
				}

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.HOUR_OF_DAY, 1);
				Date date = calendar.getTime();

				calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.HOUR_OF_DAY, 23);
				Date date2 = calendar.getTime();

				Tbmuser tbmuser = Common.getCurrentUser();

				Sekolah sekolah = tbmuser.ambilSekolah();
				Yayasan yayasan = tbmuser.ambilYayasan();

				String jenisKonfigurasi = "checklist_penilaian_guru";

				KonfigurasiKalenderAkademik konfigurasiKalenderAkademik = (KonfigurasiKalenderAkademik) ConstantValues
						.simpleObject(session.createCriteria(KonfigurasiKalenderAkademik.class)
								.createAlias("kalenderAkademik", "kalenderAkademik")
								.createAlias("konfigurasi", "konfigurasi")
								.addOrder(Order.desc("kalenderAkademik.tanggalMulai"))

								.add(Restrictions.eq("konfigurasi.nama", jenisKonfigurasi))

								.add(Restrictions.or(Restrictions.isNull("kalenderAkademik.sekolah"),
										Restrictions.eq("kalenderAkademik.sekolah", sekolah)))

								.add(Restrictions.or(Restrictions.isNull("kalenderAkademik.yayasan"),
										Restrictions.eq("kalenderAkademik.yayasan", yayasan)))

								.add(Restrictions.le("kalenderAkademik.tanggalMulai", date))
								.add(Restrictions.ge("kalenderAkademik.tanggalSelesai", date2))

								.addOrder(Order.desc("id"))

								.setMaxResults(1), KonfigurasiKalenderAkademik.class);

				Konfigurasi konfigurasi = konfigurasiKalenderAkademik == null ? null
						: konfigurasiKalenderAkademik.getKonfigurasi();

				System.out.println("Login siswa " + siswa + " konfigurasi" + konfigurasi);

				String ta = konfigurasiKalenderAkademik == null ? Common.getCurrentTahunAkademik()
						: konfigurasiKalenderAkademik.getKalenderAkademik().getTahunAjaran();

				String jenis = konfigurasiKalenderAkademik == null
						? (Common.isNowSemensterGanjil() ? Perkuliahan.GENAP : Perkuliahan.GANJIL)
						: konfigurasiKalenderAkademik.getKalenderAkademik().getGanjilGenap();

				try {
					boolean angketBerlangsung = konfigurasi == null ? false
							: konfigurasi.getNilai().equals(Konfigurasi.AKTIF);
					if (angketBerlangsung) {
						if (!AngketUtil.displayPenilaianAngket(siswa, ta, jenis)) {
							return;
						}
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AngketUtil.java:657");
				}

			}
		});
	}
}
