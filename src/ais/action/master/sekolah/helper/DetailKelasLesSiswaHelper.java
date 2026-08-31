package ais.action.master.sekolah.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.SertifikatAction;
import ais.action.master.helper.DetailpertemuanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataSiswaBanyak;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBoldHijau;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK yang menampilkan dan mengelola detail satu {@link KelasLesSiswa} (kelas les/ekstra
 * kurikuler sekolah) dalam empat tab: Peserta (daftar siswa anggota kelas les — {@link
 * KelasLesSiswaPunyaSiswa} — dengan status kewajiban pembayaran per siswa, status aktif, status
 * kelulusan/persetujuan {@code acc}, cetak sertifikat kelulusan, dan hapus), Pertemuan (jadwal
 * pertemuan kelas les, didelegasikan ke {@link JadwalPelajaran} terkait), Kehadiran (rekap absensi,
 * lewat {@link CommonReportHelper}/{@link DetailpertemuanHelper}), dan Penilaian. Baris peserta
 * menampilkan daftar kewajiban pembayaran yang belum lunas ({@link PengaturanBiaya}/
 * {@link JenisBiayaSekolah}) langsung di grid; status aktif dikunci (tidak bisa diubah) bila kelas
 * les memiliki syarat pengaturan pembayaran yang berlaku. Mendukung unggah massal penempatan siswa
 * ke kelas les dari file Excel ({@link #uploadDataSiswa}). Mengimplementasikan {@link DataLoader}
 * dan {@link DataCriteria} agar dapat dipasang ke komponen baku (paging, tombol cetak).
 */
public class DetailKelasLesSiswaHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	// private Siswa siswa;
	private KelasLesSiswa kelasLesSiswa;
	private boolean delete = false;

	private Textbox nama;
	private Intbox angkatan;
	private boolean create;

	private Paging paging;
	private Combobox statusAktif;
	private Combobox statusLulus;

	/** Membuat helper baru: mengevaluasi hak akses hapus/tambah dan menyiapkan komponen paging yang memuat ulang data saat halaman berganti. */
	public DetailKelasLesSiswaHelper() {
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		create = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
	}

	/** Renderer baris grid daftar peserta kelas les: foto, NIS (link riwayat revisi), nama, daftar kewajiban pembayaran belum lunas, nomor urut (editable), checkbox aktif/lulus (masing-masing tersimpan otomatis dan mengatur visibilitas tombol sertifikat/hapus), dan tombol cetak sertifikat/hapus. */
	class DetailPARenderer extends ais.ui.util.MyRowRenderer {

		public DetailPARenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = (KelasLesSiswaPunyaSiswa) data;
			final Siswa siswa = kelasLesSiswaPunyaSiswa.getSiswa();

			CommonMedia.tampilkanGambarKecil(siswa).setParent(row);
			RevisiHelper.createNewRevisi(Siswa.class, siswa, siswa.getNomorInduk()).setParent(row);

			new Label(siswa.getNama()).setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			List<Object[]> syarats = kelasLesSiswaPunyaSiswa.ambilBelumBayar();
			if (!syarats.isEmpty()) {
				new MyLabelAgakKecilBoldMerah("Belum membayar").setParent(vbox);
				for (Object[] objects : syarats) {

					if (objects[0] instanceof PengaturanBiaya) {
						PengaturanBiaya biaya = (PengaturanBiaya) objects[0];
						String bulan = objects[1] + "";
						String tahun = objects[2] + "";

						String b = "";
						if (!bulan.isEmpty() && !tahun.isEmpty()) {
							b = " bulan " + bulan + " tahun " + tahun;
						}

						RevisiHelper.createNewRevisi(PengaturanBiaya.class, biaya, biaya + b).setParent(vbox);
					} else if (objects[0] instanceof JenisBiayaSekolah) {
						JenisBiayaSekolah jenisBiayaSekolah = (JenisBiayaSekolah) objects[0];
						RevisiHelper.createNewRevisi(JenisBiayaSekolah.class, jenisBiayaSekolah, jenisBiayaSekolah.getNama()).setParent(vbox);
					}
				}
			} else {
				new MyLabelAgakKecilBoldHijau("Tidak ada kewajiban pembayaran").setParent(vbox);
			}

			final Intbox nomorUrut = new Intbox(kelasLesSiswaPunyaSiswa.getNomorUrut());
			nomorUrut.setParent(row);
			nomorUrut.setWidth("90%");

			nomorUrut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelasLesSiswaPunyaSiswa.setNomorUrut(nomorUrut.getValue());
					Common.refreshUpdate(kelasLesSiswaPunyaSiswa);
				}
			});

			final MyToolbarbuttonConfig cetakToolbarbuttonSertifikat = new MyToolbarbuttonConfig("Sertifikat",
					"/img/certificate-icon.png");
			final MyCheckboxConfig acc = new MyCheckboxConfig("Lulus");
			final MyToolbarbuttonConfig deleteButton = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");

			final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
			aktif.setChecked(kelasLesSiswaPunyaSiswa.getAktif());
			aktif.setParent(row);
			aktif.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelasLesSiswaPunyaSiswa.setAktif(aktif.isChecked());
					Common.refreshSaveOrUpdate(kelasLesSiswaPunyaSiswa);
					cetakToolbarbuttonSertifikat.setVisible(kelasLesSiswaPunyaSiswa.getAcc()
							&& kelasLesSiswaPunyaSiswa.getAktif() && kelasLesSiswa.getSertifikat() != null);
					deleteButton.setVisible(!kelasLesSiswaPunyaSiswa.getAcc() && delete);
				}
			});

			if (kelasLesSiswaPunyaSiswa.getKelasLesSiswa() != null) {
				if (!kelasLesSiswa.getSyaratPengaturanPembayaran().isEmpty()) {
					aktif.setDisabled(true);
				}
			}

			cetakToolbarbuttonSertifikat
					.setVisible(kelasLesSiswaPunyaSiswa.getAcc() && kelasLesSiswa.getSertifikat() != null);

			cetakToolbarbuttonSertifikat.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					SertifikatAction.cetakSertifikat(kelasLesSiswaPunyaSiswa);
				}
			});
			cetakToolbarbuttonSertifikat.setOrient("vertical");

			deleteButton.setOrient("vertical");

			acc.setChecked(kelasLesSiswaPunyaSiswa.getAcc());
			acc.setParent(row);
			acc.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelasLesSiswaPunyaSiswa.setAcc(acc.isChecked());
					Common.refreshSaveOrUpdate(kelasLesSiswaPunyaSiswa);
					cetakToolbarbuttonSertifikat.setVisible(kelasLesSiswaPunyaSiswa.getAcc()
							&& kelasLesSiswaPunyaSiswa.getAktif() && kelasLesSiswa.getSertifikat() != null);
					deleteButton.setVisible(!kelasLesSiswaPunyaSiswa.getAcc() && delete);
				}
			});
			cetakToolbarbuttonSertifikat
					.setVisible(kelasLesSiswaPunyaSiswa.getAcc() && kelasLesSiswa.getSertifikat() != null);

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			deleteButton.setTooltiptext("Hapus Data");
			deleteButton.setOrient("vertical");
			deleteButton.setVisible(!kelasLesSiswaPunyaSiswa.getAcc() && delete);
			deleteButton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(kelasLesSiswaPunyaSiswa);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(null);
												}
											});

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}

			});
			aksiButtons.add(cetakToolbarbuttonSertifikat);
			aksiButtons.add(deleteButton);

			ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);

		}

	}

	/**
	 * Menyusun kriteria pencarian siswa peserta kelas les aktif, difilter status aktif, status
	 * lulus/{@code acc}, nama/NIS/NISN, dan angkatan, diurutkan nomor urut lalu nama bila diminta.
	 *
	 * @param order {@code true} untuk menyertakan pengurutan
	 * @return kriteria Hibernate siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelasLesSiswaPunyaSiswa.class)

				.add(statusAktif.getSelectedItem() == null || statusAktif.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("aktif", statusAktif.getSelectedItem().getValue()))

				.add(statusLulus.getSelectedItem() == null || statusLulus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("acc", statusLulus.getSelectedItem().getValue()))

				.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa))

				.createAlias("siswa", "siswa")

				.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("siswa.namaSiswa", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("siswa.nomorInduk", nama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("siswa.nomorIndukNasional", nama.getValue().trim(),
												MatchMode.ANYWHERE))))
				.add(angkatan == null || angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("siswa.tahunMasuk", angkatan.getValue()));

		if (order) {
			criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.namaSiswa"))
					.addOrder(Order.desc("siswa.id"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	/** Memuat halaman siswa peserta kelas les sesuai kriteria pencarian dan halaman paging aktif, lalu merender hasilnya ke grid. */
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<KelasLesSiswaPunyaSiswa> siswa = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				KelasLesSiswaPunyaSiswa.class);

		ListModel strset = new SimpleListModel(siswa);
		grid.setRowRenderer(new DetailPARenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun panel detail kelas les di dalam komponen target dengan empat tab: Peserta (grid
	 * daftar siswa dengan pencarian dan paging), Pertemuan (jadwal pertemuan, dimuat lazy saat tab
	 * diklik), Kehadiran (rekap absensi), dan Penilaian.
	 *
	 * @param kelasLesSiswa kelas les yang detailnya ditampilkan/dikelola
	 * @param component     komponen kontainer target, dibersihkan dan diisi ulang oleh method ini
	 * @param window        jendela pemanggil (konteks tambahan untuk dialog terkait)
	 */
	public void displayDetailPA(final KelasLesSiswa kelasLesSiswa, final Component component, final MyWindow window) {

		this.kelasLesSiswa = kelasLesSiswa;
		Common.clear(component);

		Tabbox tabbox = new Tabbox();
		tabbox.setStyle("min-height:11000px");
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Peserta", "/img/home-icon.png");
		tab1.setParent(tabs);

		MyTabConfig tab1Pertemuan = new MyTabConfig("Pertemuan", "/img/home-icon.png");
		tab1Pertemuan.setParent(tabs);

		MyTabConfig tab1Kehadiran = new MyTabConfig("Kehadiran", "/img/Document-Text-icon.png");
		tab1Kehadiran.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Penilaian", "/img/Document-Text-icon.png");
		tab2.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("5000px");

		final Tabpanel tabpanelPertemuan = new ais.ui.util.MyTabpanel();
		tabpanelPertemuan.setStyle("min-height: 200px;");
		tabpanelPertemuan.setHeight("2000px");
		tabpanelPertemuan.setParent(tabpanels);

		tab1Pertemuan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();
				JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) ConstantValues.simpleObject(
						session.createCriteria(JadwalPelajaran.class)
								.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa)).setMaxResults(1),
						JadwalPelajaran.class);
				if (jadwalPelajaran == null) {
					MyMessageboxConfig.show("Jadwal pembelajaran harus dibuat, klim OK untuk membuat jadwal baru",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									RekapitulasiJadwalPelajaranHelper.tambahJadwalMengajar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) arg0.getData();
											Tbmuser tbmuser = Common.getCurrentUser();
											AktifitasPembelajaranHelper aktifitasPembelajaranHelper = new AktifitasPembelajaranHelper(
													tbmuser == null ? null : tbmuser.getSiswa(),
													tbmuser == null ? null : tbmuser.getCalonSiswa());

											aktifitasPembelajaranHelper.initDetail(jadwalPelajaran, tabpanelPertemuan,
													0, 1);
										}
									}, kelasLesSiswa);
								}
							});
				} else {

					if (tabpanelPertemuan.getChildren().isEmpty()) {
						Tbmuser tbmuser = Common.getCurrentUser();
						AktifitasPembelajaranHelper aktifitasPembelajaranHelper = new AktifitasPembelajaranHelper(
								tbmuser == null ? null : tbmuser.getSiswa(),
								tbmuser == null ? null : tbmuser.getCalonSiswa());

						aktifitasPembelajaranHelper.initDetail(jadwalPelajaran, tabpanelPertemuan, 0, 1);

					}
				}
			}
		});

		final Tabpanel tabpanelKehadiran = new ais.ui.util.MyTabpanel();
		tabpanelKehadiran.setStyle("min-height: 200px;");
		tabpanelKehadiran.setHeight("2000px");
		tabpanelKehadiran.setParent(tabpanels);

		tab1Kehadiran.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelKehadiran.getChildren().isEmpty()) {

					Session session = HibernateUtil.currentSession();
					JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) ConstantValues.simpleObject(
							session.createCriteria(JadwalPelajaran.class)
									.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa)).setMaxResults(1),
							JadwalPelajaran.class);
					if (jadwalPelajaran == null) {
						MyMessageboxConfig.show("Jadwal pembelajaran harus dibuat, klim OK untuk membuat jadwal baru",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										RekapitulasiJadwalPelajaranHelper.tambahJadwalMengajar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) arg0.getData();
												DetailpertemuanHelper detailpertemuanHelper = new DetailpertemuanHelper();
												detailpertemuanHelper.displayDetailPertemuan(jadwalPelajaran,
														tabpanelKehadiran);
											}
										}, kelasLesSiswa);
									}
								});
					} else {

						if (tabpanelPertemuan.getChildren().isEmpty()) {
							DetailpertemuanHelper detailpertemuanHelper = new DetailpertemuanHelper();
							detailpertemuanHelper.displayDetailPertemuan(jadwalPelajaran, tabpanelKehadiran);

						}
					}

				}
			}
		});

		final Tabpanel panelKurikulum = new ais.ui.util.MyTabpanel();
		panelKurikulum.setParent(tabpanels);
		panelKurikulum.setHeight("5000px");

		tab2.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelKurikulum.getChildren().isEmpty()) {

					List<KelasLesSiswaPunyaSiswa> siswas = ConstantValues.simpleList(initCriteria(true),
							KelasLesSiswaPunyaSiswa.class);

					DetailPenilaianLesSiswaHelper.displayPenilaian(panelKurikulum, kelasLesSiswa, siswas);
				}
			}
		});

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(tabpanel1);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(8);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbar.appendChild(angkatan = new Intbox());

		statusAktif = new Combobox();
		Comboitem comboitem = new Comboitem("Semua Status");
		comboitem.setValue(null);
		statusAktif.appendChild(comboitem);
		comboitem = new Comboitem("Aktif");
		comboitem.setValue(true);
		statusAktif.appendChild(comboitem);
		comboitem = new Comboitem("Tidak Aktif");
		comboitem.setValue(false);
		statusAktif.appendChild(comboitem);
		statusAktif.setReadonly(true);
		statusAktif.setSelectedIndex(0);

		statusLulus = new Combobox();
		comboitem = new Comboitem("Semua Status Lulus");
		comboitem.setValue(null);
		statusLulus.appendChild(comboitem);
		comboitem = new Comboitem("Lulus");
		comboitem.setValue(true);
		statusLulus.appendChild(comboitem);
		comboitem = new Comboitem("Belum Lulus");
		comboitem.setValue(false);
		statusLulus.appendChild(comboitem);
		statusLulus.setReadonly(true);
		statusLulus.setSelectedIndex(0);

		toolbar.appendChild(statusAktif);
		toolbar.appendChild(statusLulus);
		statusAktif.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}

		});
		statusLulus.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}

		});
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}

		});
		angkatan.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}

		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(kelasLesSiswa, false);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Siswa", "/img/new.gif");
		button.setVisible(create);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Session session = HibernateUtil.currentSession();
				List<Siswa> siswas = session.createCriteria(KelasLesSiswaPunyaSiswa.class)
						.setProjection(Projections.groupProperty("siswa"))
						.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa)).list();

				AmbilDataSiswaBanyak ambilDataSiswaBanyak = new AmbilDataSiswaBanyak(siswas);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataSiswaBanyak);
				ambilDataSiswaBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Siswa> siswas = (List<Siswa>) arg0.getData();

						Session session = HibernateUtil.currentSession();
						for (Siswa siswa : siswas) {

							KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = (KelasLesSiswaPunyaSiswa) session
									.createCriteria(KelasLesSiswaPunyaSiswa.class).add(Restrictions.eq("siswa", siswa))
									.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa)).setMaxResults(1)
									.uniqueResult();
							if (kelasLesSiswaPunyaSiswa == null) {
								kelasLesSiswaPunyaSiswa = new KelasLesSiswaPunyaSiswa();
							}

							kelasLesSiswaPunyaSiswa.setKelasLesSiswa(kelasLesSiswa);
							kelasLesSiswaPunyaSiswa.setSiswa(siswa);
							Common.refreshSaveOrUpdate(kelasLesSiswaPunyaSiswa);

						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});
					}
				});
				ambilDataSiswaBanyak.setWidth("850px");
				ambilDataSiswaBanyak.setHeight("97%");
				ambilDataSiswaBanyak.setVisible(true);
				ambilDataSiswaBanyak.onModal();

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();
										List<KelasLesSiswaPunyaSiswa> kelasLesSiswaPunyaSiswas = session
												.createCriteria(KelasLesSiswaPunyaSiswa.class)
												.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa)).list();
										for (KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa : kelasLesSiswaPunyaSiswas) {
											session.delete(kelasLesSiswaPunyaSiswa);
											session.flush();
										}

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												loadData(null);
											}
										});

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig.show(
												"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
														+ e.getMessage());
									}

								}

							}
						});

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Copy siswa dari kelas lain", "/img/svg/edit-copy.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow window = new MyWindow("Copy siswa dari kelas lain", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("200px");
				window.setWidth("300px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				ais.ui.util.ZkCompat.setFlex(center, true);
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("30%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Kelas *"));
				final Combobox searchkelas;
				row.appendChild(searchkelas = new Combobox());
				searchkelas.setWidth("90%");

				EventListener kelasEvent = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Tbmuser tbmuser = Common.getCurrentUser();
						Sekolah s = tbmuser == null ? null : tbmuser.ambilSekolah();
						System.out.println("s => " + s);
						Common.insertComboDanSemua(searchkelas, new String[] { "nama", "ruang" }, "keterangan",
								KelasLesSiswa.class,
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"),
												s == null ? Restrictions.sqlRestriction("true")
														: Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

						Common.selectComboItem(searchkelas, null);
					}
				};

				kelasEvent.onEvent(null);

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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Copy data siswa", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {

						KelasLesSiswa kelasLesSiswaPilih = (KelasLesSiswa) (searchkelas.getSelectedItem() == null ? null
								: searchkelas.getSelectedItem().getValue());

						if (kelasLesSiswaPilih == null) {
							MyMessageboxConfig.show("Kelas harus diisi", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						Session session = HibernateUtil.currentSession();
						List<KelasLesSiswaPunyaSiswa> kelasLesSiswaPunyaSiswas = session
								.createCriteria(KelasLesSiswaPunyaSiswa.class)
								.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswaPilih))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.list();

						for (KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa : kelasLesSiswaPunyaSiswas) {
							KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswaLama = (KelasLesSiswaPunyaSiswa) session
									.createCriteria(KelasLesSiswaPunyaSiswa.class)
									.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa))
									.add(Restrictions.eq("siswa", kelasLesSiswaPunyaSiswa.getSiswa())).setMaxResults(1)
									.uniqueResult();
							if (kelasLesSiswaPunyaSiswaLama == null) {
								kelasLesSiswaPunyaSiswaLama = new KelasLesSiswaPunyaSiswa();
								kelasLesSiswaPunyaSiswaLama.setSiswa(kelasLesSiswaPunyaSiswa.getSiswa());
								kelasLesSiswaPunyaSiswaLama.setKelasLesSiswa(kelasLesSiswa);
								session.save(kelasLesSiswaPunyaSiswaLama);
								session.flush();

							}
						}

						window.detach();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(arg0);
							}
						});

					}
				});
				save.setParent(toolbar);

				window.setVisible(true);
				window.onModal();

			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "siswa.nomorInduk",
				"siswa.nomorIndukNasional", "siswa.namaSiswa", "nomorUrut", "siswa.tahunMasuk", "siswa.sekolah.nama",
				"siswa.sekolah.yayasan", "siswa.penjurusanSekolah.nama", "aktif", "acc");
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload " + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							uploadDataSiswa(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(arg0);
									Clients.clearBusy();
								}
							});
						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(upload);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIS");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Syarat Pembayaran");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.Urut");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aktif");
		column.setWidth("6%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Lulus");
		column.setWidth("6%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("12%");

		loadData(null);

	}

	/**
	 * Memproses unggahan massal penempatan siswa ke kelas les dari file Excel (.xlsx): membaca setiap
	 * baris di thread latar belakang terpisah, meresolusi entitas {@link Siswa} per baris, dan
	 * mendaftarkannya sebagai peserta {@code kelasLesSiswa}. Menampilkan indikator sibuk dan progres
	 * persentase lewat {@link Timer}, lalu menampilkan pesan selesai dan memanggil
	 * {@code eventListener} saat proses tuntas.
	 *
	 * @param file          file Excel (.xlsx) yang sudah diunggah dan tersimpan di disk
	 * @param eventListener callback yang dipanggil setelah proses upload selesai (mis. memuat ulang layar)
	 * @throws Exception diteruskan apa adanya dari kegagalan I/O atau parsing workbook
	 */
	public void uploadDataSiswa(final File file, final EventListener eventListener) throws Exception {

		final StringBuilder laporan = new StringBuilder();
		final int[] jumlah = {0, 0}; // [0]=berhasil, [1]=gagal/dilewati

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data siswa.."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					timer.detach();
					Clients.clearBusy();
					String isi = "Laporan Upload Siswa ke Kelas Les\n"
							+ "==================================\n"
							+ "Berhasil : " + jumlah[0] + " siswa\n"
							+ "Gagal    : " + jumlah[1] + " baris\n\n"
							+ laporan.toString();
					try {
						org.zkoss.zul.Filedownload.save(isi.getBytes("UTF-8"), "text/plain", "laporan_upload_siswa_kelas_les.txt");
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					MyMessageboxConfig.show(
							"Upload selesai.\nBerhasil: " + jumlah[0] + " siswa, Gagal/Dilewati: " + jumlah[1]
									+ " baris.\nLaporan rinci telah diunduh.",
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
				}
			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				Session session = null;
				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					// Gunakan openSession() bukan currentNativeSession(): getSheetContentAsObject()
					// menutup native session ThreadLocal di dalam loop, sehingga session yang
					// diperoleh dari currentNativeSession() menjadi invalid setelah iterasi pertama.
					session = HibernateUtil.openSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							Siswa siswa = (Siswa) Common.getSheetContentAsObject(sheet, 0, i, Siswa.class);
							Integer nomorUrut = Common.getSheetContentAsInteger(sheet, 3, i);
							if (siswa != null && siswa.getId() != null) {
								Siswa siswaSafe = (Siswa) session.get(Siswa.class, siswa.getId());
								if (siswaSafe == null) {
									laporan.append("Baris ").append(i).append(": GAGAL - siswa tidak ditemukan di DB\n");
									jumlah[1]++;
									continue;
								}

								KelasLesSiswaPunyaSiswa ksps = (KelasLesSiswaPunyaSiswa) session
										.createCriteria(KelasLesSiswaPunyaSiswa.class)
										.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa))
										.add(Restrictions.eq("siswa", siswaSafe)).setMaxResults(1).uniqueResult();
								if (ksps == null) {
									ksps = new KelasLesSiswaPunyaSiswa();
								}
								ksps.setNomorUrut(nomorUrut);
								ksps.setKelasLesSiswa(kelasLesSiswa);
								ksps.setSiswa(siswaSafe);
								session.getTransaction().begin();
								Common.refreshSaveOrUpdate(session, ksps);
								session.getTransaction().commit();

								laporan.append("Baris ").append(i).append(": OK - ")
										.append(siswaSafe.getNim()).append(" ").append(siswaSafe.getNama()).append("\n");
								jumlah[0]++;
								label.setValue("Upload \"" + siswaSafe.getNim() + " - " + siswaSafe.getNama() + "\" ("
										+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else {
								String cellVal = Common.getCellContent(Common.getCell(sheet, 0, i));
								if (cellVal != null && !cellVal.trim().isEmpty()) {
									laporan.append("Baris ").append(i).append(": DILEWATI - NIS '")
											.append(cellVal).append("' tidak ditemukan\n");
									jumlah[1]++;
								}
							}

						} catch (Exception e) {
							laporan.append("Baris ").append(i).append(": ERROR - ").append(e.getMessage()).append("\n");
							jumlah[1]++;
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					laporan.append("ERROR FATAL: ").append(e1.getMessage()).append("\n");
					e1.printStackTrace();
					ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/helper/DetailKelasLesSiswaHelper.java:uploadDataSiswa");
				} finally {
					HibernateUtil.closeSessionQuietly(session);
					HibernateUtil.closeSession();
				}

				label.setValue("");
			}
		}).start();
	}

}
