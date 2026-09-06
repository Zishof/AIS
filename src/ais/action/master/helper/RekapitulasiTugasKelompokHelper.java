package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.RencanaTahunAkademikAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.VOPembelajaran;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.WaktuUtil;

/**
 * Helper composer ZK yang menampilkan rekapitulasi/timeline {@link TugasKelompok} (tugas
 * kelompok) milik satu user ({@link Tbmuser}), dengan pola sumber-pertemuan yang sama dengan
 * {@link RekapitulasiPertemuanHelper} (berdasarkan {@code perkuliahan} yang diberikan, atau peran
 * user sebagai mahasiswa/dosen/guru/siswa/admin sekolah).
 *
 * <p>
 * Berbeda dari {@link RekapitulasiPertemuanHelper}, kelas ini menambahkan: (1) kotak pencarian teks
 * bebas ({@code cari}, mencocokkan keterangan/judul/nama tugas, ILIKE anywhere) dan (2) paging
 * server-side manual dengan logika "lompat ke halaman yang berisi tugas dengan tanggal terdekat hari
 * ini" (dihitung dari query COUNT terpisah lalu dijepit/clamp ke rentang halaman valid — lihat
 * komentar kode pada method {@code reload} terkait perbaikan {@code WrongValueException}). Setiap
 * baris ditampilkan sebagai groupbox berjudul (judul tugas) yang saat diklik membuka
 * {@link PertemuanHelper} untuk pertemuan terkait; tautan tanggal pertemuan juga membuka helper yang
 * sama. Tombol "buat baru" didelegasikan ke {@link RekapitulasiUjianHelper#buatbaru}.
 * </p>
 */
public class RekapitulasiTugasKelompokHelper {

	/** Seperti {@link #display(Component, Tbmuser, VOPembelajaran)} dengan {@code perkuliahan=null} (memakai sumber pertemuan berbasis user). */
	public static void display(Component parent, final Tbmuser tbmuser) {
		display(parent, tbmuser, null);
	}

	/**
	 * Membangun UI rekap tugas kelompok (kotak cari + filter tanggal + tombol buat baru/refresh +
	 * grid berpaging) di dalam {@code parent}, lalu memuat data awal lewat {@link #reload}. Rentang
	 * tanggal default diambil dari {@link RencanaTahunAkademik} yang sedang berlaku (±1 bulan), dan
	 * grid disegarkan otomatis lewat timer berkala.
	 *
	 * @param parent      komponen induk ZK tempat UI dibangun
	 * @param tbmuser     user yang rekap tugas kelompoknya ditampilkan
	 * @param perkuliahan bila diberikan, rekap dibatasi hanya pada tugas milik objek pembelajaran
	 *                    ini ({@link Perkuliahan} atau {@link JadwalPelajaran}); bila {@code null},
	 *                    sumber pertemuan ditentukan dari peran {@code tbmuser}
	 */
	public static void display(Component parent, final Tbmuser tbmuser, final VOPembelajaran perkuliahan) {

		Borderlayout subBorderlayoutUtama = new Borderlayout();
		subBorderlayoutUtama.setParent(parent);

		final Center center = new Center();
		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(subBorderlayoutUtama);
		north.setHeight("38px");
		Toolbar hbox = new Toolbar();
		hbox.setParent(north);

		final Textbox cari = new Textbox();
		hbox.appendChild(new MyLabelConfig("Cari:"));
		hbox.appendChild(cari);
		cari.setCols(10);

		RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction
				.getCurrentRencanaTahunAkademik(WaktuUtil.getDate());

		Calendar calendarMulai = Calendar.getInstance();
		calendarMulai
				.setTime(rencanaTahunAkademik == null ? WaktuUtil.getDate() : rencanaTahunAkademik.getTanggalMulai());
		calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 1);

		Calendar calendarSampai = Calendar.getInstance();
		calendarSampai
				.setTime(rencanaTahunAkademik == null ? WaktuUtil.getDate() : rencanaTahunAkademik.getTanggalSampai());
		calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 1);

		final MyDatebox mulai = new MyDatebox(calendarMulai.getTime());
		mulai.setReadonly(true);

		final MyDatebox sampai = new MyDatebox(calendarSampai.getTime());
		sampai.setReadonly(true);

		if (perkuliahan == null) {
			hbox.appendChild(new MyLabelConfig("Tanggal"));
			hbox.appendChild(mulai);
			hbox.appendChild(new MyLabelConfig("sd"));
			hbox.appendChild(sampai);
		}

		RekapitulasiUjianHelper.buatbaru(tbmuser, perkuliahan, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					reload(tbmuser, center, mulai.getValue(), sampai.getValue(), cari.getValue().trim(), true, true,
							perkuliahan);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RekapitulasiTugasKelompokHelper.java:120");
					// TODO: handle exception
				}
			}
		}, 3, "Tugas Kelompok").setParent(hbox);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
		refresh.setTooltiptext("Refresh");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload(tbmuser, center, mulai.getValue(), sampai.getValue(), cari.getValue().trim(), true, true,
						perkuliahan);
			}
		});
		refresh.setParent(hbox);

		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(subBorderlayoutUtama);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload(tbmuser, center, mulai.getValue(), sampai.getValue(), cari.getValue().trim(), false, true,
						perkuliahan);
			}
		};

		Common.createDefaultTimer(eventListener);
		cari.addEventListener("onOK", eventListener);
		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);
	}

	/**
	 * Menentukan daftar id {@link Pertemuan} yang relevan (sama seperti
	 * {@link RekapitulasiPertemuanHelper}, lihat javadoc kelas), membangun grid + komponen
	 * {@link Paging} baru di dalam {@code center}, lalu mendelegasikan pemuatan data halaman
	 * pertama ke {@link #reload(List, Paging, MyGrid, Mahasiswa, boolean, Date, Date, String, VOPembelajaran)}.
	 *
	 * @param tbmuser     user pemilik rekap
	 * @param center      panel tempat grid dibangun ulang; isinya dibersihkan lebih dulu
	 * @param mulai       batas awal filter tanggal
	 * @param sampai      batas akhir filter tanggal
	 * @param cari        kata kunci pencarian bebas (keterangan/judul/nama tugas)
	 * @param refreh      bila {@code true}, sinkronkan ulang cache pertemuan mahasiswa/dosen lewat
	 *                    {@code reInitPertemuan} sebelum memuat data
	 * @param awal        diteruskan sebagai parameter {@code awal} ke overload
	 *                    {@code reload} berikutnya (memicu lompat-ke-halaman-terdekat-hari-ini)
	 * @param perkuliahan bila diberikan, batasi tugas hanya milik objek ini
	 */
	@SuppressWarnings("unchecked")
	private static void reload(final Tbmuser tbmuser, final Center center, final Date mulai, final Date sampai,
			final String cari, boolean refreh, boolean awal, final VOPembelajaran perkuliahan) {
		if (center != null) {
			Common.clear(center);
		}

		final Mahasiswa mahasiswa = tbmuser.getMahasiswa();
		Dosen dosen = tbmuser.ambilDosen();

		final Paging paging = new Paging();

		final List<Long> pertemuans = new ArrayList<Long>();
		Session session = HibernateUtil.currentSession();

		if (perkuliahan != null) {
			Criteria criteria = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(perkuliahan instanceof Perkuliahan ? Restrictions.eq("perkuliahan", perkuliahan)
							: perkuliahan instanceof JadwalPelajaran ? Restrictions.eq("jadwalPelajaran", perkuliahan)
									: Restrictions.sqlRestriction("false"));
			pertemuans.addAll(criteria.setProjection(Projections.property("id")).setMaxResults(32766).list());
		}

		else if (mahasiswa != null) {

			if (refreh) {

				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.setTime(mulai);
				calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 6);

				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.setTime(sampai);
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 6);

				mahasiswa.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
			}

			pertemuans.addAll(mahasiswa.ambilPertemuan(session).values());

		} else if (dosen != null) {

			if (refreh) {

				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.setTime(mulai);
				calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 6);

				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.setTime(sampai);
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 6);

				dosen.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
			}

			pertemuans.addAll(dosen.ambilPertemuan(session).values());
		} else if (!Common.getApakahAdmin()) {

			Sekolah sk = SekolahUtil.getSekolah();
			Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
			Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

			if (guru != null) {
				sk = guru.getSekolah();
			}
			if (siswa != null) {
				sk = siswa.getSekolah();
			}

			Calendar calendarMulai = Calendar.getInstance();
			calendarMulai.setTime(mulai);
			calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 6);

			Calendar calendarSampai = Calendar.getInstance();
			calendarSampai.setTime(sampai);
			calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 6);

			String mul = null;
			String sam = null;

			Integer bln = 1;
			Integer ke = null;

			boolean jadwalPerkuliahan = !(sk != null && sk.getId() != null);

			boolean jadwalPelajaran = (sk != null && sk.getId() != null);

			boolean jadwalKkn = jadwalPerkuliahan;

			boolean jadwalPkl = jadwalPerkuliahan;

			boolean jadwalKegiatan = jadwalPerkuliahan;

			boolean jadwalRevisi = jadwalPerkuliahan;
			boolean jadwalKonsultasi = jadwalPerkuliahan;

			boolean jadwalBimbingan = jadwalPerkuliahan;

			boolean jadwalKonsultasiLain = jadwalPerkuliahan;

			boolean tdpDiskusi = false;

			boolean tdpUjian = false;

			boolean tdpMateri = false;
			boolean tdpTugas = false;
			boolean tdpCatatan = false;
			boolean tdpAudio = false;
			boolean tdpVideo = false;
			boolean tdpDosenPengganti = false;

			String cariMk = "";
			String cariDosen = "";
			String cariTopik = "";
			String cariCatatan = "";
			String cariMahasiswa = "";
			String cariKelas = "";
			String cariRuang = "";

			Integer ekstra = null;

			boolean remedial = false;
			boolean paralel = false;
			boolean pra = false;
			StatusPertemuan statusPertemuan = null;
			boolean ujian = false;

			String day = null;

			Criteria criteria = DashboardTimelinePertemuan.initStaticCriteria(true, calendarMulai.getTime(),
					calendarSampai.getTime(), tbmuser, cari, mul, sam, bln, statusPertemuan, ke, day, tdpVideo,
					tdpAudio, tdpMateri, jadwalPerkuliahan, jadwalPelajaran, jadwalKkn, jadwalPkl, jadwalRevisi,
					jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain, jadwalKegiatan, tdpUjian, tdpDiskusi,
					tdpTugas, tdpCatatan, tdpDosenPengganti, cariTopik, cariCatatan, paralel, pra, remedial, cariMk,
					ekstra != null && ekstra.equals(Perkuliahan.EKSTRA), cariKelas, cariRuang, cariDosen, cariMahasiswa,
					ujian, sk, session);

			pertemuans.addAll(criteria.setProjection(Projections.property("id")).setMaxResults(32766).list());
		}

		paging.setDetailed(true);
		paging.setPageSize(Common.ROWS_COUNT_ON_PAGE);
		paging.setPageIncrement(Common.isMobile() ? 5 : 10);
		paging.setMold("os");

		final MyGrid grid = new MyGrid();
		paging.addEventListener("onPaging", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				reload(pertemuans, paging, grid, mahasiswa, false, mulai, sampai, cari, perkuliahan);
			}
		});

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(center);

		groupbox.appendChild(paging);

		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tugas Kelompok");

		reload(pertemuans, paging, grid, mahasiswa, awal, mulai, sampai, cari, perkuliahan);

	}

	/**
	 * Memuat satu halaman {@link TugasKelompok} ke {@code grid} sesuai kriteria (id pertemuan yang
	 * relevan, kata kunci {@code cari}, rentang tanggal {@code mulai}/{@code sampai} bila
	 * {@code perkuliahan} tidak diberikan), dan memperbarui total halaman pada {@code paging}.
	 *
	 * <p>
	 * Bila {@code awal=true}, method ini juga menghitung lewat query {@code COUNT} terpisah berapa
	 * banyak tugas yang tanggal mulainya sudah lewat ({@code date(mulai) < CURRENT_DATE}), lalu
	 * menjepit hasil bagi jumlah tersebut dengan {@link Common#ROWS_COUNT_ON_PAGE} ke rentang
	 * halaman valid ({@code [0, pageCount-1]}) sebagai halaman aktif — efeknya, tampilan otomatis
	 * "melompat" ke halaman yang memuat tugas-tugas terkini/mendatang alih-alih selalu memulai dari
	 * halaman pertama.
	 * </p>
	 *
	 * @param pertemuans  daftar id pertemuan yang membatasi tugas yang ditampilkan (diabaikan bila
	 *                    user admin dan {@code perkuliahan} null)
	 * @param paging      komponen paging yang total ukurannya diperbarui dan (bila {@code awal})
	 *                    halaman aktifnya disesuaikan
	 * @param grid        grid tujuan data dimuat
	 * @param mahasiswa   diteruskan ke {@link DetailPertemuanRenderer} secara tidak langsung lewat
	 *                    closure event refresh; tidak dipakai langsung untuk query
	 * @param awal        bila {@code true}, hitung dan set halaman aktif otomatis (lihat di atas)
	 * @param mulai       batas awal filter tanggal (diabaikan bila {@code perkuliahan} diberikan)
	 * @param sampai      batas akhir filter tanggal (diabaikan bila {@code perkuliahan} diberikan)
	 * @param cari        kata kunci pencarian bebas (keterangan/judul/nama tugas)
	 * @param perkuliahan bila diberikan, batasi tugas hanya milik objek ini
	 */
	@SuppressWarnings("unchecked")
	private static void reload(final List<Long> pertemuans, final Paging paging, final MyGrid grid,
			final Mahasiswa mahasiswa, boolean awal, final Date mulai, final Date sampai, final String cari,
			final VOPembelajaran perkuliahan) {
		Session session = HibernateUtil.currentSession();

		Criterion criterion = Common.getApakahAdmin() && perkuliahan == null ? Restrictions.sqlRestriction("true")
				: (pertemuans.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("pertemuan", pertemuans));

		int size = ((Number) session.createCriteria(TugasKelompok.class)


				.add(cari.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("keterangan", cari, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("judul", cari, MatchMode.ANYWHERE),
										Restrictions.ilike("nama", cari, MatchMode.ANYWHERE))))

				.add(perkuliahan != null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("mulai"),
								Restrictions.sqlRestriction(
										"date(this_.mulai) between date('" + Common.databaseDateFormat.get().format(mulai)
												+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')")))

				.add(perkuliahan != null && perkuliahan instanceof Perkuliahan
						? Restrictions.eq("perkuliahan", perkuliahan)
						: (perkuliahan != null && perkuliahan instanceof JadwalPelajaran
								? Restrictions.eq("jadwalPelajaran", perkuliahan)
								: criterion))

				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		paging.setTotalSize(size);
		paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE);

		if (awal) {

			int page = ((Number) session.createCriteria(TugasKelompok.class)

					.add(cari.isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("keterangan", cari, MatchMode.ANYWHERE),
									Restrictions.or(Restrictions.ilike("judul", cari, MatchMode.ANYWHERE),
											Restrictions.ilike("nama", cari, MatchMode.ANYWHERE))))

					.add(Restrictions.sqlRestriction("date(mulai)<CURRENT_DATE"))

					.add(perkuliahan != null && perkuliahan instanceof Perkuliahan
							? Restrictions.eq("perkuliahan", perkuliahan)
							: (perkuliahan != null && perkuliahan instanceof JadwalPelajaran
									? Restrictions.eq("jadwalPelajaran", perkuliahan)
									: criterion))

					.setProjection(Projections.rowCount()).uniqueResult()).intValue();

			try {
				System.out.println("page -> " + page + ", size -> " + size + ", pertemuans -> " + pertemuans.size());
				// FIX akar masalah WrongValueException "Unable to set active page to X since only
				// Y pages": target halaman dihitung dari query COUNT terpisah (page/size di atas),
				// bukan dari paging.getPageCount() yang sebenarnya menentukan jumlah halaman valid
				// pada komponen Paging ini. Selisih pembulatan antara kedua penghitungan itu bisa
				// membuat target melebihi (atau sama dengan) jumlah halaman yang benar-benar ada --
				// try/catch dengan fallback "-1" sebelumnya cuma tebak-tebakan dan masih bisa gagal
				// lagi. Jepit (clamp) target ke rentang valid [0, pageCount-1] agar selalu tepat.
				int pageCount = paging.getPageCount();
				int targetPage = (int) (page / Common.ROWS_COUNT_ON_PAGE);
				if (pageCount <= 0) {
					targetPage = 0;
				} else if (targetPage >= pageCount) {
					targetPage = pageCount - 1;
				} else if (targetPage < 0) {
					targetPage = 0;
				}
				paging.setActivePage(targetPage);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RekapitulasiTugasKelompokHelper.java:392");
			}
		}

		List<TugasKelompok> tugasKelompoks = session.createCriteria(TugasKelompok.class)


				.add(cari.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("keterangan", cari, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("judul", cari, MatchMode.ANYWHERE),
										Restrictions.ilike("nama", cari, MatchMode.ANYWHERE))))

				.add(perkuliahan != null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("mulai"),
								Restrictions.sqlRestriction(
										"date(this_.mulai) between date('" + Common.databaseDateFormat.get().format(mulai)
												+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')")))

				.add(perkuliahan != null && perkuliahan instanceof Perkuliahan
						? Restrictions.eq("perkuliahan", perkuliahan)
						: (perkuliahan != null && perkuliahan instanceof JadwalPelajaran
								? Restrictions.eq("jadwalPelajaran", perkuliahan)
								: criterion))

				.addOrder(Order.asc("mulai")).addOrder(Order.asc("id")).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(tugasKelompoks);
		grid.setModelCheckMobile(strset);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload(pertemuans, paging, grid, mahasiswa, false, mulai, sampai, cari, perkuliahan);
			}
		};

		grid.setRowRenderer(new DetailPertemuanRenderer(eventListener));

	}

	/**
	 * Perender baris grid untuk satu {@link TugasKelompok}: tautan berjudul (judul tugas, atau
	 * disembunyikan sepenuhnya bila tugas tidak punya judul) yang membuka {@link PertemuanHelper}
	 * untuk pertemuan terkait, indikator "dilihat" ({@link TampilanELearningAction#dilihat}), dan
	 * tautan info pertemuan (nomor pertemuan, tanggal mulai/selesai, topik) yang juga membuka
	 * {@link PertemuanHelper}. Baris disembunyikan bila tugas tidak punya pertemuan terkait.
	 */
	public static class DetailPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		/** Callback dipanggil untuk memuat ulang grid setelah window {@link PertemuanHelper} yang dibuka dari baris ini ditutup. */
		private EventListener eventListener;
		/** User yang sedang login; konteks mahasiswa/calon mahasiswanya diteruskan ke {@link PertemuanHelper} saat membuka detail pertemuan. */
		private Tbmuser tbmuser = Common.getCurrentUser();

		/** @param eventListener callback yang dipanggil untuk memuat ulang grid setelah window {@link PertemuanHelper} ditutup */
		public DetailPertemuanRenderer(EventListener eventListener) {

			this.eventListener = eventListener;
		}

		/**
		 * Merender satu baris untuk satu {@link TugasKelompok} (lihat javadoc kelas
		 * {@link DetailPertemuanRenderer} untuk rincian tautan yang dibangun); baris disembunyikan
		 * bila tugas tidak punya {@link Pertemuan} terkait.
		 *
		 * @param row  baris grid yang diisi
		 * @param data instance {@link TugasKelompok} untuk baris ini
		 * @throws Exception diteruskan dari kegagalan pembangunan UI atau akses data
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final TugasKelompok tugas = (TugasKelompok) data;

			final Pertemuan pertemuan = tugas.ambilPertemuan();
			if (pertemuan == null) {
				row.setVisible(false);
				return;
			}

			if (tugas.getJudultugas() != null && !tugas.getJudultugas().trim().isEmpty()) {

				String n = tugas.getJudultugas();
				if (n == null) {
					n = "";
				}
				Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
				vbox.setWidth("90%");
				vbox.setParent(row);

				String icon = "/img/svg/user-group.svg";

				Toolbarbutton downloadButton = new ais.ui.util.MyToolbarbuttonConfig(n.length() > 150 ? n.substring(0, 150) + "..." : n,
						icon);

				downloadButton.setAttribute("janganDisabled", true);
				vbox.appendChild(downloadButton);
				downloadButton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
								.display(pertemuan, new DataLoader() {

									@Override
									public void loadData(Object value) {
										try {
											eventListener.onEvent(null);
										} catch (Exception e) {
											// TODO Auto-generated catch block
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiTugasKelompokHelper.java:487");
										}

									}
								}, 3, null, tugas, null, null, null);

					}
				});

				Hbox myHbox = new Hbox();
				myHbox.setParent(vbox);

				TampilanELearningAction.dilihat(tugas, "tugas", "Akses", false).setParent(myHbox);

				if (pertemuan != null) {
					Date tgl = tugas.getMulai() == null ? pertemuan.getTanggal() : tugas.getMulai();
					Date tgl1 = tugas.getSelesai();

					A a;
					vbox.appendChild(a = new A(
							"Tugas pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info() + ", "
									+ (SmartDateTimeUtil.getDayString(tgl, null) + Common.dateFormat51.get().format(tgl))
									+ (tgl1 == null ? ""
											: " sampai dengan " + (SmartDateTimeUtil.getDayString(tgl1, null)
													+ Common.dateFormat51.get().format(tgl1)))
									+ ", " + pertemuan.getTopik()));

					a.setStyle("font-size:10px;");

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
									.display(pertemuan, new DataLoader() {

										@Override
										public void loadData(Object value) {
											try {
												eventListener.onEvent(null);
											} catch (Exception e) {
												// TODO Auto-generated catch block
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiTugasKelompokHelper.java:530");
											}
										}
									}, 3, null, tugas, null, null, null);

						}
					});
				}
			}

		}

	}

}
