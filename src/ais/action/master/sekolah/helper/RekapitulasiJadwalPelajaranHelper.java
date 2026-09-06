package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.TampilanELearningAction;
import ais.action.master.helper.DetailpertemuanHelper;
import ais.action.master.sekolah.JadwalPelajaranAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.VoKelasPunyaSiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Utilitas UI ZK (statis) untuk menampilkan rekapitulasi jadwal pelajaran/mengajar sekolah:
 * membangun panel pencarian dan daftar {@link JadwalPelajaran} yang dikelompokkan per guru/kelas
 * dengan statistik opsional ({@link #display}), memuat ulang datanya sesuai filter tahun ajaran/
 * semester/kata kunci ({@link #reload}), dan menyediakan dialog tambah/ubah jadwal mengajar
 * tunggal ({@link #tambahJadwalMengajar}) yang dapat dikaitkan ke {@link KelasLesSiswa} les privat
 * bila diberikan.
 */
public class RekapitulasiJadwalPelajaranHelper {

	/**
	 * Membuka dialog modal untuk menambah atau mengubah satu {@link JadwalPelajaran}: memilih
	 * kelas siswa dan (opsional) kelas les siswa, mata pelajaran, hari/jam, dan detail lainnya.
	 * Bila {@code lesSiswa} diberikan, jadwal yang dibuat otomatis dikaitkan sebagai jadwal les
	 * privat untuknya. {@code eventListenerData} dipanggil dengan {@link JadwalPelajaran} hasil
	 * simpan setelah dialog ditutup dengan sukses.
	 */
	public static void tambahJadwalMengajar(final EventListener eventListenerData, final KelasLesSiswa lesSiswa)
			throws Exception {
		final MyWindow window = new MyWindow("Tambah atau Ubah Jadwal Mengajar", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("350px");
		window.setWidth("600px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center a = new Center();
		a.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(a);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();

		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas Siswa *"));
		final AmbilDataKelasSiswaSemuaBanbox pilihKelasSiswa;
		row.appendChild(pilihKelasSiswa = new AmbilDataKelasSiswaSemuaBanbox());
		pilihKelasSiswa.setReadonly(true);
		pilihKelasSiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas Les Siswa *"));
		final AmbilDataKelasLesSiswaBanbox pilihKelasLesSiswa;
		row.appendChild(pilihKelasLesSiswa = new AmbilDataKelasLesSiswaBanbox());
		pilihKelasLesSiswa.setWidth("90%");
		pilihKelasLesSiswa.setAttribute("kelasLesSiswa", lesSiswa);
		pilihKelasLesSiswa.setAttribute("kelas", lesSiswa);
		pilihKelasLesSiswa.setValue(lesSiswa == null ? "" : lesSiswa.getNama());

		if (lesSiswa != null) {
			pilihKelasLesSiswa.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		final Combobox semester;
		row.appendChild(semester = new Combobox());
		Comboitem comboitem = new Comboitem(JadwalPelajaran.GANJIL);
		comboitem.setValue(1);
		semester.appendChild(comboitem);
		comboitem = new Comboitem(JadwalPelajaran.GENAP);
		comboitem.setValue(2);
		semester.appendChild(comboitem);
		Common.selectComboItem(true, semester, Common.isNowSemensterGanjil() ? 1 : 2);
		semester.setWidth("90%");
		semester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mata Pelajaran *"));
		final Combobox matapelajaran;
		row.appendChild(matapelajaran = new Combobox());
		matapelajaran.setWidth("90%");
		matapelajaran.setReadonly(true);

		final EventListener eventListenerPilihKelas = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pilihKelasSiswa.getParent().setVisible(pilihKelasLesSiswa.getAttribute("kelasLesSiswa") == null);
				semester.getParent().setVisible(pilihKelasLesSiswa.getAttribute("kelasLesSiswa") == null);
				matapelajaran.getParent().setVisible(pilihKelasLesSiswa.getAttribute("kelasLesSiswa") == null);
				pilihKelasLesSiswa.getParent().setVisible(pilihKelasSiswa.getAttribute("kelasSiswa") == null);
			}
		};

		pilihKelasLesSiswa.setEventListener(eventListenerPilihKelas);

		try {
			eventListenerPilihKelas.onEvent(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/RekapitulasiJadwalPelajaranHelper.java:151");
		}

		pilihKelasSiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListenerPilihKelas.onEvent(arg0);
				KelasSiswa kelasSiswa = (KelasSiswa) pilihKelasSiswa.getAttribute("kelasSiswa");
				if (kelasSiswa != null) {

					Sekolah s = kelasSiswa.getSekolah();
					System.out.println("s => " + s);

					List<Long> longs = kelasSiswa == null ? new ArrayList<Long>() : kelasSiswa.ambilMk();

					Common.insertCombo(matapelajaran, new String[] { "nama" }, Matapelajaran.class,

							Restrictions.and(
									longs.isEmpty() ? Restrictions.sqlRestriction("true")
											: Restrictions.not(Restrictions.in("id", longs)),

									Restrictions.and(
											Restrictions.or(Restrictions.isNull("sekolah"),
													Restrictions.eq("sekolah", s)),
											Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))));

					matapelajaran.setReadonly(true);
				}
			}
		});

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				KelasSiswa kelasSiswa = (KelasSiswa) pilihKelasSiswa.getAttribute("kelasSiswa");

				KelasLesSiswa kelasLesSiswa = (KelasLesSiswa) pilihKelasLesSiswa.getAttribute("kelasLesSiswa");

				Matapelajaran mk = (Matapelajaran) (kelasLesSiswa != null ? kelasLesSiswa.getMatapelajaran()
						: (matapelajaran.getSelectedItem() == null ? null
								: matapelajaran.getSelectedItem().getValue()));
				if (kelasLesSiswa != null || (kelasSiswa != null && mk != null)) {

					JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) ConstantValues
							.simpleObject(
									kelasLesSiswa != null
											? HibernateUtil.currentSession().createCriteria(JadwalPelajaran.class)
													.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa))
													.setMaxResults(1)
											: HibernateUtil.currentSession().createCriteria(JadwalPelajaran.class)
													.add(Restrictions.eq("matapelajaran", mk))
													.add(Restrictions.eq("kelas", kelasSiswa))
													.add(Restrictions.eq("tahunAjaran", kelasSiswa.getTahunAjaran()))
													.add(Restrictions.eq("semester",
															semester.getSelectedItem().getValue()))
													.setMaxResults(1),
									JadwalPelajaran.class);

					if (jadwalPelajaran == null) {
						jadwalPelajaran = new JadwalPelajaran();
						jadwalPelajaran.setMatapelajaran(mk);
						jadwalPelajaran.setSemester((Integer) semester.getSelectedItem().getValue());
						jadwalPelajaran.setTahunAjaran(kelasSiswa == null ? null : kelasSiswa.getTahunAjaran());
						jadwalPelajaran.setKelas(kelasSiswa);
						jadwalPelajaran.setKelasLesSiswa(kelasLesSiswa);
					}

					window.detach();

					JadwalPelajaranAction.onAddExternal(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) arg0.getData();

							eventListenerData.onEvent(new Event("", null, jadwalPelajaran));

						}
					}, jadwalPelajaran);

				}

			}
		};

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Lanjut", "/img/save.gif");
		save.setTooltiptext("Lanjut Tambah Data");
		save.addEventListener("onClick", eventListener);
		save.setParent(toolbar);

		window.onModal();
	}

	/**
	 * Membangun kerangka panel rekapitulasi jadwal pelajaran pada {@code parent}: toolbar
	 * pencarian (kata kunci, tahun ajaran, semester — default semester berjalan), tombol "Tambah
	 * Jadwal Mengajar" (tampil hanya untuk user non-siswa/calon siswa/mahasiswa/calon mahasiswa —
	 * yaitu guru/staf) yang membuka {@link #tambahJadwalMengajar}, dan tombol Refresh. Perubahan
	 * filter memuat ulang data via {@link #reload} secara otomatis (dengan jeda kecil/debounce
	 * lewat {@link Common#createDefaultTimer} saat pertama kali dibuka).
	 *
	 * @param tampilStatistik menampilkan ringkasan statistik tambahan pada hasil rekapitulasi
	 */
	public static void display(Component parent, final Tbmuser tbmuser, final boolean tampilStatistik) {

		Borderlayout subBorderlayoutUtama = new Borderlayout();
		subBorderlayoutUtama.setWidth("100%");
		subBorderlayoutUtama.setHeight("100%");
		subBorderlayoutUtama.setParent(parent);

		final Center center = new Center();
		center.setAutoscroll(true);
		center.setSclass("elearning-sekolah-ringkasan-center");
		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(subBorderlayoutUtama);

		Toolbar hbox = new Toolbar();
		hbox.setParent(north);

		final Textbox cari = new Textbox();
		hbox.appendChild(new MyLabelConfig("Cari:"));
		hbox.appendChild(cari);
		cari.setCols(10);

		hbox.appendChild(new MyLabelConfig("Tahun Ajaran"));
		final Combobox tahunAkademik = Common.generateTahunAjaran(null);
		tahunAkademik.setCols(8);
		hbox.appendChild(tahunAkademik);
		tahunAkademik.setReadonly(true);
		hbox.appendChild(new MyLabelConfig("Semester"));
		final Combobox semester = new Combobox();
		semester.setCols(4);
		hbox.appendChild(semester);
		semester.setReadonly(true);

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(JadwalPelajaran.GANJIL);
		comboitem.setValue(JadwalPelajaran.GANJIL);
		semester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(JadwalPelajaran.GENAP);
		comboitem.setValue(JadwalPelajaran.GENAP);
		semester.appendChild(comboitem);

		Common.selectComboItem(semester,
				Common.isNowSemensterGanjil() ? JadwalPelajaran.GANJIL : JadwalPelajaran.GENAP);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		refresh.setTooltiptext("Tambah Jadwal Mengajar");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				RekapitulasiJadwalPelajaranHelper.tambahJadwalMengajar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) arg0.getData();
						String ta = jadwalPelajaran.getTahunAjaran();
						String smt = jadwalPelajaran.getSemester() % 2 == 0 ? JadwalPelajaran.GENAP
								: JadwalPelajaran.GANJIL;
						reload(tbmuser, center, ta, smt, cari.getValue().trim(), true, -1, tampilStatistik);
					}
				}, null);

			}
		});
		refresh.setParent(hbox);
		refresh.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getMahasiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null);

		refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
		refresh.setTooltiptext("Refresh");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (tahunAkademik.getSelectedItem() == null || semester.getSelectedItem() == null) {
					ais.ui.util.MyMessageboxConfig.show("Pilih tahun akademik dan semester terlebih dahulu.");
					return;
				}
				String ta = (String) (tahunAkademik.getSelectedItem().getValue());
				String smt = (String) semester.getSelectedItem().getValue();
				reload(tbmuser, center, ta, smt, cari.getValue().trim(), true, -1, tampilStatistik);
			}
		});
		refresh.setParent(hbox);

		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(subBorderlayoutUtama);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String ta = (String) (tahunAkademik.getSelectedItem().getValue());
				String smt = (String) semester.getSelectedItem().getValue();
				reload(tbmuser, center, ta, smt, cari.getValue().trim(), false, -1, tampilStatistik);
			}
		};

		Common.createDefaultTimer(eventListener, "Loading data..", false, 2000);
		cari.addEventListener("onOK", eventListener);
		tahunAkademik.addEventListener("onChange", eventListener);
		semester.addEventListener("onChange", eventListener);
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	/**
	 * Memuat ulang isi {@code center} sesuai filter tahun ajaran ({@code ta}), semester
	 * ({@code smt}), dan kata kunci ({@code cari}): mengambil {@link JadwalPelajaran} milik user
	 * (guru/siswa/mahasiswa sesuai perannya) yang cocok, lalu merender ringkasannya (dikelompokkan
	 * dan, bila {@code tampilStatistik}, disertai statistik tambahan).
	 *
	 * @param refreh menandai pemuatan ulang eksplisit (mis. dari tombol Refresh) vs otomatis
	 * @param page   halaman data yang dimuat (paginasi), {@code -1} untuk halaman awal/reset
	 */
	private static void reload(final Tbmuser tbmuser, final Center center, final String ta, final String smt,
			final String cari, boolean refreh, int page, final boolean tampilStatistik) {
		if (center != null) {
			Common.clear(center);
		}

		Paging paging = null;

		List<JadwalPelajaran> jadwalPelajarans = new ArrayList<JadwalPelajaran>();

		paging = new Paging();
		Sekolah sekolah = SekolahUtil.getSekolah();
		Session session = HibernateUtil.currentNativeSession();
		int size = ((Number) TampilanELearningAction.initStaticCriteria(false, TampilanELearningAction.PELAJARAN, cari,
				tbmuser.ambilFakultas(), tbmuser.ambilJurusan(),
				tbmuser.ambilProgram() == null ? null : tbmuser.ambilProgram().getNama(),
				sekolah != null && sekolah.getId() != null ? sekolah.getYayasan() : tbmuser.ambilYayasan(),
				sekolah != null && sekolah.getId() != null ? sekolah : tbmuser.ambilSekolah(), ta, smt, null, false,
				false, false, false, true, true, true, true, true, true, true, true, true, "", tbmuser, session)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		/*
		 * Satu sumber ukuran halaman wajib dipakai oleh Paging, perhitungan jumlah
		 * halaman, limit, dan offset query. Sebelumnya query selalu mengambil 10
		 * jadwal tetapi Paging memakai Common.ROWS_COUNT_ON_PAGE. Jika konfigurasi
		 * global lebih dari 10, jadwal urutan ke-11 dan seterusnya tidak memiliki
		 * halaman yang dapat dibuka sehingga kelas tampak hilang.
		 */
		Integer jumlahDataDalamSatuHalaman = Common.ROWS_COUNT_ON_PAGE > 0
				? Common.ROWS_COUNT_ON_PAGE : 10;
		Integer halaman = page == -1 ? paging.getActivePage() : page;
		if (halaman == null || halaman < 0) {
			halaman = 0;
		}

		paging.setPageSize(jumlahDataDalamSatuHalaman);
		paging.setPageIncrement(Common.isMobile() ? 5 : 10);
		paging.setMold("os");
		paging.setTotalSize(size);
		paging.setVisible(size > jumlahDataDalamSatuHalaman);
		paging.setDetailed(true);

		/* Clamp: setActivePage() sebelumnya dipanggil dengan parameter mentah `page`
		 * (bukan `halaman`), padahal `page` adalah sentinel -1 dari pemanggil yang
		 * berarti "pakai halaman aktif sekarang" (lihat inisialisasi/refresh/onOK/onChange
		 * di atas yang mengirim page=-1). Akibatnya ZK menerima -1 langsung dan melempar
		 * WrongValueException "Unable to set active page to -1 since only 1 pages" begitu
		 * hasil pencarian/filter kosong (size=0 -> cuma 1 halaman valid: index 0). Batasi
		 * juga batas atas terhadap jumlah halaman riil supaya aman bila offset lama
		 * melebihi total setelah data/filter berubah. */
		int totalHalaman = (int) Math.ceil(size / (double) jumlahDataDalamSatuHalaman);
		if (totalHalaman < 1) {
			totalHalaman = 1;
		}
		if (halaman >= totalHalaman) {
			halaman = totalHalaman - 1;
		}

		try {
			paging.setActivePage(halaman);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/RekapitulasiJadwalPelajaranHelper.java:396");
			// TODO: handle exception
		}

		paging.addEventListener("onPaging", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Paging paging = (Paging) arg0.getTarget();

				reload(tbmuser, center, ta, smt, cari, false, paging.getActivePage(), tampilStatistik);

				Clients.scrollIntoView(paging.getParent().getParent());
			}
		});

		List<JadwalPelajaran> voPembelajarans = ConstantValues
				.simpleList(TampilanELearningAction
						.initStaticCriteria(true, TampilanELearningAction.PELAJARAN, cari, tbmuser.ambilFakultas(),
								tbmuser.ambilJurusan(),
								tbmuser.ambilProgram() == null ? null : tbmuser.ambilProgram().getNama(),
								sekolah != null && sekolah.getId() != null ? sekolah.getYayasan()
										: tbmuser.ambilYayasan(),
								sekolah != null && sekolah.getId() != null ? sekolah : tbmuser.ambilSekolah(), ta, smt,
								null, false, false, false, false, true, true, true, true, true, true, true, true, true,
								"", tbmuser, session)
						.setMaxResults(jumlahDataDalamSatuHalaman).setFirstResult(jumlahDataDalamSatuHalaman * halaman),
						JadwalPelajaran.class);
		for (VOPembelajaran voPembelajaran : voPembelajarans) {
			if (voPembelajaran instanceof JadwalPelajaran) {
				jadwalPelajarans.add((JadwalPelajaran) voPembelajaran);
			}
		}

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setSclass("dgrid elearning-sekolah-ringkasan-grid");
		/*
		 * Jangan mengunci grid ke tinggi Center. Satu jadwal dapat menghasilkan baris
		 * ringkasan yang tinggi; height 100% membuat body grid berhenti di batas
		 * viewport dan jadwal berikutnya terlihat terpotong. Grid dibiarkan tumbuh
		 * mengikuti semua baris, sedangkan Center yang autoscroll menangani halaman
		 * yang lebih panjang.
		 */
		grid.setHeight("auto");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("18%");

		column = new MyColumnConfig();
		column.setWidth("24%");
		column.setParent(columns);

		if (tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) {

			column = new MyColumnConfig();
			column.setWidth("42%");
			column.setParent(columns);

			column = new MyColumnConfig();
			column.setWidth("16%");
			column.setParent(columns);
		} else {
			column = new MyColumnConfig();
			column.setParent(columns);
		}

		Rows rows = new Rows();

		rows.setParent(grid);

		boolean mobile = Common.isMobile();

		for (final JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			try {
				/* Mode ringkas dua kolom menjaga ikon statistik tetap berada di dalam
				 * kolom pada laptop. Mode horizontal lama memakai Hbox tanpa wrap dan
				 * mendorong tombol Kehadiran/Penilaian keluar dari viewport. */
				Common.getDeskripsiJadwalPelajaranHbox(jadwalPelajaran, tampilStatistik, false, row);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/RekapitulasiJadwalPelajaranHelper.java:475");
			}

			if (tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) {

				Vbox vboxUtama = new Vbox();
				row.appendChild(vboxUtama);

				Div toolbar = new Div();
				toolbar.setWidth("100%");
				toolbar.setStyle(mobile
						? "display:flex; flex-direction:column; gap:6px;"
						: "display:flex; flex-wrap:wrap; gap:8px 12px; align-items:flex-start;");
				vboxUtama.appendChild(toolbar);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Kehadiran", "/img/svg/user-list-thin.svg");
				button.setTooltiptext("Ubah Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						final MyWindow window = new MyWindow("Kehadiran", "none", true);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(window);

						Center a = new Center();
						a.setParent(borderlayout);

						MyGrid grid = new MyGrid();
						grid.setWidth("100%");
						grid.setParent(a);
						grid.setHeight("100%");

						Columns columns = new Columns();
						columns.setParent(grid);
						MyColumnConfig column = new MyColumnConfig();
						column.setParent(columns);

						Rows rows = new Rows();

						rows.setParent(grid);

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);

						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 2200px;");
						groupbox.setWidth("100%");
						new DetailpertemuanHelper().displayDetailPertemuan(jadwalPelajaran, groupbox);
						row.appendChild(groupbox);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						// toolbar.setHeight("25px");
						toolbar.setParent(south);
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
						cancel.setTooltiptext("Selesai");
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}
						});
						cancel.setParent(toolbar);
						window.onModal();
					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("Penilaian", "/img/svg/check2-all.svg");
				button.setTooltiptext("Ubah Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						final MyWindow window = new MyWindow("Penilaian", "none", true);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(window);

						Center a = new Center();
						a.setParent(borderlayout);

						MyGrid grid = new MyGrid();
						grid.setWidth("100%");
						grid.setParent(a);
						grid.setHeight("100%");

						Columns columns = new Columns();
						columns.setParent(grid);
						MyColumnConfig column = new MyColumnConfig();
						column.setParent(columns);

						Rows rows = new Rows();

						rows.setParent(grid);

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);

						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 2200px;");
						groupbox.setWidth("100%");

						Session session = HibernateUtil.currentSession();

						if (jadwalPelajaran.getKelas() != null) {
							Criteria criteria = session.createCriteria(KelasSiswaPunyaSiswa.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("kelasSiswa", jadwalPelajaran.getKelas()))

									.createAlias("siswa", "siswa");

							criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.tahunMasuk"))
									.addOrder(Order.asc("siswa.namaSiswa")).addOrder(Order.desc("siswa.id"));

							Tbmuser tbmuser = Common.getCurrentUser();
							if (tbmuser != null && tbmuser.getOrangTua() != null
									&& !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
								criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
							}

							List<? extends VoKelasPunyaSiswa> siswas = criteria.list();
							DetailPenilaianSiswaHelper.displayPenilaian(jadwalPelajaran,
									jadwalPelajaran.getKurikulumPunyaMatapelajaran(), groupbox,
									jadwalPelajaran.getKelas(), siswas);
						} else if (jadwalPelajaran.getKelasLesSiswa() != null) {
							Criteria criteria = session.createCriteria(KelasLesSiswaPunyaSiswa.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("kelasLesSiswa", jadwalPelajaran.getKelasLesSiswa()))
									.createAlias("siswa", "siswa");

							criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.tahunMasuk"))
									.addOrder(Order.asc("siswa.namaSiswa")).addOrder(Order.desc("siswa.id"));

							Tbmuser tbmuser = Common.getCurrentUser();
							if (tbmuser != null && tbmuser.getOrangTua() != null
									&& !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
								criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
							}

							List<? extends VoKelasPunyaSiswa> siswas = criteria.list();
							DetailPenilaianSiswaHelper.displayPenilaian(jadwalPelajaran,
									jadwalPelajaran.getKurikulumPunyaMatapelajaran(), groupbox,
									jadwalPelajaran.getKelas(), siswas);
						}

						row.appendChild(groupbox);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						// toolbar.setHeight("25px");
						toolbar.setParent(south);
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
						cancel.setTooltiptext("Selesai");
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}
						});
						cancel.setParent(toolbar);
						window.onModal();
					}

				});
				button.setParent(toolbar);

				toolbar = new Div();
				toolbar.setWidth("100%");
				toolbar.setStyle(mobile
						? "display:flex; flex-direction:column; gap:6px;"
						: "display:flex; flex-wrap:wrap; gap:8px 12px; align-items:flex-start;");
				vboxUtama.appendChild(toolbar);

				button = new MyToolbarbuttonConfig("Agenda", "/img/svg/calendar-check.svg");
				button.setTooltiptext("Ubah Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						final MyWindow window = new MyWindow("Agenda", "none", true);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(window);

						Center a = new Center();
						a.setParent(borderlayout);

						MyGrid grid = new MyGrid();
						grid.setWidth("100%");
						grid.setParent(a);
						grid.setHeight("100%");

						Columns columns = new Columns();
						columns.setParent(grid);
						MyColumnConfig column = new MyColumnConfig();
						column.setParent(columns);

						Rows rows = new Rows();

						rows.setParent(grid);

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);

						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 2200px;");
						groupbox.setWidth("100%");

						AktifitasPembelajaranHelper aktifitasPerkuliahanHelper = new AktifitasPembelajaranHelper(
								tbmuser.getSiswa(), null);
						aktifitasPerkuliahanHelper.tampikanTab = true;
						aktifitasPerkuliahanHelper.initDetail(jadwalPelajaran, groupbox, 0, 1);

						row.appendChild(groupbox);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						// toolbar.setHeight("25px");
						toolbar.setParent(south);
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
						cancel.setTooltiptext("Selesai");
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}
						});
						cancel.setParent(toolbar);
						window.onModal();
					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("Jadwal", "/img/svg/calendar2.svg");
				button.setTooltiptext("Ubah Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						JadwalPelajaranAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) arg0.getData();

								String ta = jadwalPelajaran.getTahunAjaran();
								String smt = jadwalPelajaran.getSemester() % 2 == 0 ? JadwalPelajaran.GENAP
										: JadwalPelajaran.GANJIL;
								reload(tbmuser, center, ta, smt, cari, true, -1, tampilStatistik);
							}
						}, jadwalPelajaran);
					}

				});
				button.setParent(toolbar);

			}

		}
		try {
			if (paging != null) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				if (tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) {
					ais.ui.util.ZkCompat.setSpans(row, "4");
				} else {
					ais.ui.util.ZkCompat.setSpans(row, "3");
				}
				row.setParent(rows);
				row.appendChild(paging);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/RekapitulasiJadwalPelajaranHelper.java:765");
		}

		// session.disconnect();
		closeHibernateSessionQuietly(session);
		HibernateUtil.closeSession();
	}


	/** Menutup {@code session} Hibernate secara diam-diam, menelan (dan mencatat) galat bila penutupan gagal. */
	private static void closeHibernateSessionQuietly(Session session) {
		ais.common.ElearningSessionUtil.closeQuietly(session);
	}

}
