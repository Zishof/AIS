package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Html;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.keuangan.DashboardRekapTunggakanMahasiswa;
import ais.action.master.dashboard.keuangan.DashboardStatistikPembayaran;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.KegiatanHelper;
import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.master.helper.KegiatanProsesHeper;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiKegiatanHelper;
import ais.action.report.CommonReportHelper;
import ais.action.report.helper.keuangan.LaporanRekapMahasiswaBelumBayarWindow;
import ais.action.report.helper.keuangan.LaporanRekapMahasiswaSudahBayarWindow;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.UploadVirtualAccount;
import ais.database.model.VOMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class KegiatanAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 *
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/**
	 * Overload kompatibilitas: logika update batas studi sudah dipindah ke
	 * {@link KegiatanHelper#updateBatasStudiMahasiswa(Mahasiswa, Session, Integer, boolean)}.
	 * Method statik ini dipertahankan agar pemanggil lama (mis. {@code PembayaranUtil} versi
	 * lama) yang memanggil {@code KegiatanAction.updateBatasStudiMahasiswa(...)} tetap compile
	 * tanpa mengubah logikanya; cukup mendelegasikan ke helper.
	 */
	public static void updateBatasStudiMahasiswa(Mahasiswa mahasiswa, Session session, Integer smt,
			boolean checkStatusPembayaranMahasiswa) {
		KegiatanHelper.updateBatasStudiMahasiswa(mahasiswa, session, smt, checkStatusPembayaranMahasiswa);
	}

	private MyGrid grid;

	private Paging paging;

	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox jenissemester;
	private Combobox searchjurusan;
	private Decimalbox searchtahun;
	private Combobox searchjenjang;
	private Combobox searchprogram;
	private Combobox ta;
	private Combobox searchJenisPembayaran;
	private Textbox searchnamamhs;
	private AmbilDataDosenBanbox searchdosen;
	private Textbox searchrekon;
	private MyDatebox start;
	private MyDatebox end;
	private Checkbox searchaktif;
	private Checkbox searchlunas;
	private Checkbox searchTelahMembayar;
	private Checkbox searchKelebihan;
	private Checkbox searchBelumMembayar;

	private Checkbox searchaktifAja;

	private MyToolbarbuttonConfig uploadData;
	private MyToolbarbuttonConfig downloadFormatPembayaran;

	public static String[] DATA = new String[] { "id", "refNumber", "mahasiswa.nim", "mahasiswa.nama",
			"calonMahasiswa.noRegistrasi", "calonMahasiswa.nama", "jenisKegiatan.nama", "statusAwalMahasiswa.nama",
			"tahunAkademik", "tahunAngkatan", "program", "semster", "validated", "validator", "jadwalPembayaran",
			"dibayar", "tagihan", "denda", "apakahLunas", "persentase", "pengurangan", "uploadVirtualAccount",
			"cicilans", "detailKegiatans", "bulans", "tagihans", "jurusan.nama" };

	public static String[] DATA_CALON = new String[] { "id", "calonMahasiswa.noRegistrasi", "calonMahasiswa.nama",
			"jenisKegiatan.nama", "statusAwalMahasiswa.nama", "tahunAkademik", "tahunAngkatan", "program", "semster",
			"dibayar", "tagihan", "denda", "apakahLunas", "persentase", "jurusan.nama" };

	public static String[] DATA_MHS = new String[] { "id", "mahasiswa.nim", "mahasiswa.nama", "jenisKegiatan.nama",
			"statusAwalMahasiswa.nama", "tahunAkademik", "tahunAngkatan", "program", "semster", "dibayar", "tagihan",
			"denda", "apakahLunas", "persentase", "jurusan.nama" };

	private Html dashboardHtml;
	private Html progressHtml;

	private Tabpanel cicilan;

	private Boolean va = null;

	public void onCicilan(Event event) {

		if (cicilan.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(cicilan);
			MyInclude iframe = new MyInclude("/pages/master/cicilan_pembayaran.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel sejarah;

	public void onSejarah(Event event) {

		if (sejarah.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(sejarah);
			MyInclude iframe = new MyInclude("/pages/master/log_pembayaran.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tunggakan;

	public void onTunggakan(Event event) throws Exception {

		if (tunggakan.getChildren().isEmpty()) {
			DashboardRekapTunggakanMahasiswa laporan = new DashboardRekapTunggakanMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, tunggakan,
				"Rekap Tunggakan", "Daftar mahasiswa yang masih memiliki tagihan belum lunas.");
		}
	}

	private Tabpanel perbandinganPembayaran;

	public void onPerbandinganPembayaran(Event event) throws Exception {

		if (perbandinganPembayaran.getChildren().isEmpty()) {
			DashboardStatistikPembayaran laporan = new DashboardStatistikPembayaran();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, perbandinganPembayaran,
				"Statistik Pembayaran", "Perbandingan tren dan pola pembayaran mahasiswa antar periode.");
		}
	}

	private Tabpanel belumBayar;

	public void onBelumBayar(Event event) throws Exception {

		if (belumBayar.getChildren().isEmpty()) {
			LaporanRekapMahasiswaBelumBayarWindow laporan = new LaporanRekapMahasiswaBelumBayarWindow();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(belumBayar);
		}
	}

	private Tabpanel sudahBayar;

	public void onSudahBayar(Event event) throws Exception {

		if (sudahBayar.getChildren().isEmpty()) {
			LaporanRekapMahasiswaSudahBayarWindow laporan = new LaporanRekapMahasiswaSudahBayarWindow();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(sudahBayar);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (execution.getParameter("va") != null) {
			va = Boolean.parseBoolean(execution.getParameter("va"));
		}

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Common.generateTahunAjaranDanSemua(ta);
//		Common.selectComboItem(ta, Common.getCurrentTahunAkademik());

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 7);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		System.out.println("va = " + va);

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.initPrograms(searchprogram);

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		jenissemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		jenissemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		jenissemester.appendChild(comboitem);
		if (jenissemester != null) { jenissemester.setSelectedItem(comboitem); }
		if (jenissemester != null) { jenissemester.setReadonly(true); }

		Common.insertComboDanSemua(searchJenisPembayaran, "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Tbmuser tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen mydosen = tbmuser.ambilDosen();
			searchdosen.setValue(mydosen.getNama());
			searchdosen.setAttribute("myValue", mydosen);
			searchdosen.setAttribute("dosen", mydosen);
			searchdosen.setDisabled(true);
		}
		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		// uploadData.setVisible(CommonPrivilages
		// .checkPrevilages(CommonPrivilages.UPDATE)
		// && CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));

		if (uploadData != null) { uploadData.setVisible(false); }

		if (downloadFormatPembayaran != null) { downloadFormatPembayaran.setVisible(uploadData.isVisible()); }

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, DATA);
		Common.appendKeToolbar(cetakToolbarbutton, downloadFormatPembayaran, comp);

		MyToolbarbuttonConfig prosesUlang = KegiatanProsesHeper.prosesUlangTagihan("Proses Tagihan", "/img/excel.png");
		prosesUlang.setVisible(tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
				&& Common.bolehKonfigurasi("tampilkan_tombol_proses_tagihan"));
		
		MyToolbarbuttonConfig btnSingkron = KegiatanProsesHeper.singkronkanDataCicilan("Singkronkan Data Cicilan", "/img/refresh.png");

		btnSingkron.setVisible(tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
		        && Common.bolehKonfigurasi("tampilkan_tombol_proses_tagihan"));

		// Masukkan ke parent (misalnya toolbar/div)
		Common.appendKeToolbar(btnSingkron, downloadFormatPembayaran, comp);

//		if (KegiatanHelper.prosestagihan) {
//			prosesUlang.setDisabled(true);
//		} else {
//			prosesUlang.setDisabled(false);
//		}

		Common.appendKeToolbar(prosesUlang, downloadFormatPembayaran, comp);

		MyToolbarbuttonConfig prosesSuratTagihan = KegiatanProsesHeper.prosesSuratTagihan("Surat Tagihan",
				"/img/options-icon.png");
		prosesSuratTagihan.setVisible(tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
				&& Common.bolehKonfigurasi("tampilkan_tombol_surat_tagihan"));
		Common.appendKeToolbar(prosesSuratTagihan, downloadFormatPembayaran, comp);

//		if (KegiatanHelper.prosestagihan) {
//			prosesSuratTagihan.setDisabled(true);
//		} else {
//			prosesSuratTagihan.setDisabled(false);
//		}

		prosesTagihan(downloadFormatPembayaran.getParent());
	}

	class DetailKegiatanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final DetailKegiatan detailKegiatan = (DetailKegiatan) arg1;
			new Label(detailKegiatan.getDetailBiaya() == null || detailKegiatan.getDetailBiaya().getItemBiaya() == null
					? ""
					: detailKegiatan.getDetailBiaya().getItemBiaya().getId() + "").setParent(arg0);
			new Label(detailKegiatan.getDetailBiaya() == null || detailKegiatan.getDetailBiaya().getItemBiaya() == null
					? ""
					: detailKegiatan.getDetailBiaya().getItemBiaya().getNama() + "").setParent(arg0);
			new Label(detailKegiatan.getDetailBiaya() == null ? ""
					: Common.numberFormat.get().format(detailKegiatan.getBiaya())).setParent(arg0);

		}
	}

	class KegiatanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {

			try {
				arg0.setValign("top");
				// TODO Auto-generated method stub
				final Kegiatan kegiatan = (Kegiatan) arg1;
				final MyDetail detail = new MyDetail();
				detail.setParent(arg0);
				detail.addEventListener("onOpen", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						Common.clear(detail);
						if (detail.isOpen()) {
							Common.clear(detail);
							Session session = HibernateUtil.currentSession();
							LogHostToHost logHostToHost = (LogHostToHost) session.createCriteria(LogHostToHost.class)
									.add(Restrictions.eq("kegiatan", kegiatan)).setMaxResults(1).uniqueResult();
							if (logHostToHost != null) {
								new ais.ui.util.MyHtml(
										"<font>Request:<br>" + logHostToHost.getNama()
												+ "<br><br>Response:<br>" + logHostToHost.getKeterangan() + "</font>")
										.setParent(detail);
							}
						}
					}
				});

				Vbox vbox1 = new Vbox();
				vbox1.setParent(arg0);

				if (kegiatan.getUploadVirtualAccount() != null) {

					new Label(kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()).setParent(vbox1);
					RevisiHelper.createNewRevisi(UploadVirtualAccount.class, kegiatan.getUploadVirtualAccount(),
							kegiatan.getUploadVirtualAccount().getNama()).setParent(vbox1);
				} else {
					new Label(kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()).setParent(vbox1);
				}

				if (kegiatan.getCalonMahasiswa() != null) {

					Vbox vbox = new Vbox();
					vbox.setParent(vbox1);
					if (kegiatan.getJenisKegiatan() != null && kegiatan.getJenisKegiatan().getId()
							.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
						new Label(kegiatan.getCalonMahasiswa() == null ? ""
								: (kegiatan.getCalonMahasiswa().getNoUjian() == null
										? kegiatan.getCalonMahasiswa().getNoRegistrasi()
										: kegiatan.getCalonMahasiswa().getNoUjian()))
								.setParent(vbox);
					} else {
						new Label(kegiatan.getCalonMahasiswa() == null ? ""
								: kegiatan.getCalonMahasiswa().getNoRegistrasi()).setParent(vbox);
					}

					if (kegiatan.getCalonMahasiswa().getMahasiswa() != null) {
						new Label("NIM: " + kegiatan.getCalonMahasiswa().getMahasiswa().getNim()).setParent(vbox);
					}

					RevisiHelper
							.createNewRevisi(Kegiatan.class, kegiatan,
									kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNama())
							.setParent(arg0);

					new Label(Common.numberFormat.get().format(kegiatan.getDibayar())).setParent(arg0);
					Double tagihanSegarCalon = KegiatanPersistenceHelper.hitungTagihanSegarKonsisten(kegiatan);
					new Label(Common.numberFormat.get()
							.format(tagihanSegarCalon != null ? tagihanSegarCalon : kegiatan.getTagihan()))
							.setParent(arg0);
					new Label(Common.numberFormat.get().format(kegiatan.getDenda())).setParent(arg0);

					new Label(
							kegiatan.getCalonMahasiswa() == null || kegiatan.getCalonMahasiswa().getProdiLulus() == null
									? (kegiatan.getCalonMahasiswa().getProdi1() == null ? ""
											: kegiatan.getCalonMahasiswa().getProdi1().getNama())
											+ (kegiatan.getCalonMahasiswa().getProdi2() == null ? ""
													: ", " + kegiatan.getCalonMahasiswa().getProdi2().getNama())
											+ (kegiatan.getCalonMahasiswa().getProdi3() == null ? ""
													: ", " + kegiatan.getCalonMahasiswa().getProdi3().getNama())
											+ (kegiatan.getCalonMahasiswa().getProdi4() == null ? ""
													: ", " + kegiatan.getCalonMahasiswa().getProdi4().getNama())
											+ (kegiatan.getCalonMahasiswa().getProdi5() == null ? ""
													: ", " + kegiatan.getCalonMahasiswa().getProdi5().getNama())
									: kegiatan.getCalonMahasiswa().getProdiLulus().getNama())
							.setParent(arg0);

				} else if (kegiatan.getMahasiswa() != null) {

					new Label(kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNim()).setParent(vbox1);

					RevisiHelper
							.createNewRevisi(Kegiatan.class, kegiatan,
									kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNama())
							.setParent(arg0);

					new Label(Common.numberFormat.get().format(kegiatan.getDibayar())).setParent(arg0);
					Double tagihanSegarMhs = KegiatanPersistenceHelper.hitungTagihanSegarKonsisten(kegiatan);
					new Label(Common.numberFormat.get()
							.format(tagihanSegarMhs != null ? tagihanSegarMhs : kegiatan.getTagihan()))
							.setParent(arg0);
					new Label(Common.numberFormat.get().format(kegiatan.getDenda())).setParent(arg0);
					new Label(kegiatan.getMahasiswa() == null || kegiatan.getMahasiswa().getJurusan() == null ? ""
							: kegiatan.getMahasiswa().getJurusan().getNama()).setParent(arg0);

				}
				new Label(kegiatan.getSemster() + "").setParent(arg0);
				new Label(kegiatan.getBulan() == null ? "N/A" : kegiatan.getBulan().toString()).setParent(arg0);
				new Label(Common.dateFormat3.get().format(kegiatan.getTanggal())).setParent(arg0);
				new Label(kegiatan.getJenisKegiatan() == null ? "" : kegiatan.getJenisKegiatan().getNamaKegiatan())
						.setParent(arg0);

				new Label((kegiatan.getApakahLunas() ? "Ya" : "Tidak") + " ("
						+ Common.numberFormat.get().format(kegiatan.getPersentaseLunas()) + "%)").setParent(arg0);

				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
				checkbox.setChecked(kegiatan.getAktif());
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kegiatan.setAktif(checkbox.isChecked());
						Common.refreshSaveOrUpdate(kegiatan);
					}
				});

				Vbox buttonVbox = new Vbox();
				buttonVbox.setParent(arg0);
				Hbox toolbar = new Hbox();
				toolbar.setParent(buttonVbox);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/options.png");
				button.setOrient("vertical");
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								try {
									Session session = HibernateUtil.currentNativeSession();

									if (kegiatan.getCalonMahasiswa() != null) {
										KegiatanHelper.checkKegiatanCalonMahasiswa(kegiatan,
												kegiatan.getJenisKegiatan(), kegiatan.getCalonMahasiswa(),
												kegiatan.getSemster(), kegiatan.getTahunAkademik(), true,
												kegiatan.getJadwalPembayaran(), false, true, null, session);
									} else if (kegiatan.getMahasiswa() != null) {
										KegiatanHelper.checkKegiatanMahasiswa(kegiatan, kegiatan.getJenisKegiatan(),
												kegiatan.getMahasiswa(), kegiatan.getSemster(),
												kegiatan.getTahunAkademik(), true, kegiatan.getJadwalPembayaran(),
												false, true, null, session);
									}
									// session.disconnect();
									if (session.isOpen()) {
										session.disconnect();
										session.close();
									}

								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}
								HibernateUtil.closeSession();

								onSearchDefault(arg0);
							}
						});

					}

				});
				toolbar.appendChild(button);

				if (kegiatan.getCalonMahasiswa() != null) {
					button = new MyToolbarbuttonConfig("Tagihan", "/img/Finance-Invoice-icon.png");
					button.setOrient("vertical");
					button.setParent(toolbar);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							// PERBAIKAN (data mahasiswa tertukar saat "Lihat Tagihan"): dibuka LANGSUNG sebagai
							// method static (pola sama dgn SetingBiayaAction.onAddExternal), bukan lagi via
							// IFRAME + URL query-string -- menghilangkan celah ID salah ter-embed di URL.
							ais.action.master.InformasiPembayaranMahasiswaAction.onViewExternal(null,
								kegiatan.getCalonMahasiswa(), kegiatan.getJenisKegiatan());

						}
					});
				} else {

					button = new MyToolbarbuttonConfig("Tagihan", "/img/Finance-Invoice-icon.png");
					button.setOrient("vertical");
					button.setParent(toolbar);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							// PERBAIKAN (data mahasiswa tertukar saat "Lihat Tagihan"): dibuka LANGSUNG sebagai
							// method static (pola sama dgn SetingBiayaAction.onAddExternal), bukan lagi via
							// IFRAME + URL query-string -- menghilangkan celah ID salah ter-embed di URL.
							ais.action.master.InformasiPembayaranMahasiswaAction.onViewExternal(kegiatan.getMahasiswa(),
								null, kegiatan.getJenisKegiatan());

						}
					});
				}

				toolbar = new Hbox();
				toolbar.setParent(buttonVbox);

				button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
				button.setTooltiptext("Cetak");
				button.setOrient("vertical");
				button.addEventListener("onClick", new EventListener() {
					@SuppressWarnings({})
					@Override
					public void onEvent(Event event) throws Exception {
						if (kegiatan.getMahasiswa() != null) {
							CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, false);
						} else {
							CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, false);

						}
					}
				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("Reversal", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Reversal");
				button.setOrient("vertical");

				Tbmuser tbmuser = Common.getCurrentUser();
				boolean bolehMerubahCicilan = false;
				String admLain = Common.getKonfigurasi("admin_yang_bisa_menghapus_data_pembayaran_mahasiswa", "am")
						.getNilai();
				String[] aa = admLain.split(";");
				for (String a : aa) {
					try {
						bolehMerubahCicilan = a.trim().equalsIgnoreCase(tbmuser.hakAkses().getRoleId());
						if (bolehMerubahCicilan) {
							break;
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);

					}
				}

				if (!bolehMerubahCicilan) {
					admLain = Common.getKonfigurasi("admin_lain_bisa_menghapus_pembayaran_mahasiswa", "").getNilai();
					aa = admLain.split(";");
					for (String a : aa) {
						try {
							bolehMerubahCicilan = a.trim().equalsIgnoreCase(tbmuser.getUserId());
							if (bolehMerubahCicilan) {
								break;
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);

						}
					}
				}

				button.setVisible(bolehMerubahCicilan
						|| (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
								&& tbmuser.hakAkses().getRoleId().trim().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)
								&& va == null));

				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						MyMessageboxConfig.show("Apakah yakin ingin melakukan reversal pada pembayaran ini ?",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												boolean hasil = PembayaranUtil.getInstance()
														.dropKegiatanLangsung(kegiatan);

												if (hasil) {
													MyMessageboxConfig.show("Reversal berhasil dilakukan",
															"Pemberitahuan", MyMessageboxConfig.OK,
															MyMessageboxConfig.INFORMATION);
												} else {
													MyMessageboxConfig.show("Reversal gagal dilakukan", "Pemberitahuan",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												onSearchDefault(null);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(
														"Data ini tidak dapat Reversal .., error-nya adalah sbagai berikut:"
																+ e.getMessage());
											}

										}

									}
								});

					}

				});
				button.setParent(toolbar);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
				arg0.setVisible(false);
			}

		}

	}

	public Criteria initCriteria(boolean order) {
		return initCriteria(order, false);
	}

	public Criteria initCriteria(boolean order, boolean cicilan) {
		Session session = HibernateUtil.currentSession();

		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");

		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		Criteria criteria;

		if (cicilan) {
			criteria = session.createCriteria(CicilanPembayaran.class).createCriteria("kegiatan");
		} else {
			criteria = session.createCriteria(Kegiatan.class);
		}

		criteria.add(searchaktifAja.isChecked()
				? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
				: Restrictions.sqlRestriction("true"))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(ta.getSelectedItem() == null || ta.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", ta.getSelectedItem().getValue()))

				.add(searchaktif == null || searchaktif.isChecked() ? Restrictions.eq("lunas", false) : Restrictions.sqlRestriction("true"))

				.add(searchlunas.isChecked() ? Restrictions.eq("lunas", true) : Restrictions.sqlRestriction("true"))
				.add(searchKelebihan.isChecked() ? Restrictions.lt("amountTerhutang", -0.1)
						: Restrictions.sqlRestriction("true"))

				.add(searchTelahMembayar.isChecked() ? Restrictions.ge("amount", 0.1)
						: Restrictions.sqlRestriction("true"))

				.add(searchBelumMembayar.isChecked()
						? Restrictions.and(Restrictions.le("amount", 0.099), Restrictions.ge("amountTerhutang", 0.099))
						: Restrictions.sqlRestriction("true"))

				.add(searchJenisPembayaran.getSelectedItem() == null
						|| searchJenisPembayaran.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisKegiatan", searchJenisPembayaran.getSelectedItem().getValue()))

				.add(jenissemester.getSelectedItem() == null || jenissemester.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: jenissemester.getSelectedItem().getValue().toString().equalsIgnoreCase(Perkuliahan.GENAP)
								? Restrictions.in("semster", Common.genap)
								: Restrictions.in("semster", Common.ganjil));

		if (searchrekon != null && !searchrekon.getValue().trim().isEmpty()) {
			criteria.createAlias("uploadVirtualAccount", "uploadVirtualAccount").add(
					Restrictions.ilike("uploadVirtualAccount.nama", searchrekon.getValue().trim(), MatchMode.ANYWHERE));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria

				.add(va != null && va ? Restrictions.isNotNull("uploadVirtualAccount")
						: Restrictions.sqlRestriction("true"))

				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)

				.add(dosen != null ? Restrictions.eq("mahasiswa.dosen", dosen.getId())
						: Restrictions.sqlRestriction("1=1"))

				.createAlias("calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)

				.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa.prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa.prodi1", "prodi1", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa.prodi2", "prodi2", Criteria.LEFT_JOIN)

				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("mahasiswa.tahunangkatan", searchtahun.getValue().intValue()),
								Restrictions.eq("calonMahasiswa.tahun", searchtahun.getValue().intValue())))

				.add(jurusan == null ? Restrictions.sqlRestriction("1=1")

						: Restrictions.or(Restrictions.eq("mahasiswa.jurusan", jurusan), Restrictions.or(
								Restrictions.eq("calonMahasiswa.prodiLulus", jurusan),
								Restrictions.and(Restrictions.isNull("calonMahasiswa.prodiLulus"), Restrictions.or(
										Restrictions.eq("calonMahasiswa.prodi5", jurusan),
										Restrictions.or(Restrictions.eq("calonMahasiswa.prodi2", jurusan),
												Restrictions.or(Restrictions.eq("calonMahasiswa.prodi1", jurusan),
														Restrictions.or(
																Restrictions.eq("calonMahasiswa.prodi3", jurusan),
																Restrictions.eq("calonMahasiswa.prodi4", jurusan))))))))

				)

				.add(searchnamamhs.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nama", searchnamamhs.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("calonMahasiswa.nama", searchnamamhs.getValue().trim(),
										MatchMode.ANYWHERE)))

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.tanggal) between date('" + Common.databaseDateFormat.get().format(start.getValue())
								+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("prodi2.fakultas", fakultas),
								Restrictions.or(Restrictions.eq("prodi1.fakultas", fakultas),
										Restrictions.or(Restrictions.eq("jurusan.fakultas", fakultas),
												Restrictions.eq("prodiLulus.fakultas", fakultas)))))

				.add(Restrictions.or(
						Restrictions.or(Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("calonMahasiswa.nim", searchnama.getValue().trim(),
										MatchMode.ANYWHERE)),
								Restrictions.ilike("calonMahasiswa.noRegistrasi", searchnama.getValue().trim(),
										MatchMode.ANYWHERE)),
						Restrictions.ilike("calonMahasiswa.noUjian", searchnama.getValue().trim())));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Kegiatan> kegiatan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kegiatan);
		grid.setRowRenderer(new KegiatanRenderer());
		grid.setModelCheckMobile(strset);

		refreshDashboardAman();
	}

	private void refreshDashboardAman() {
		try {
			// paging.getTotalSize() SUDAH dihitung oleh Common.initPaging(...) di onSearchDefault
			// tepat sebelum method ini dipanggil -> kirimkan langsung agar refreshFromCriteria TIDAK
			// menjalankan ulang SELECT count(*) yang sama (query 5-join ini berat, jangan dobel).
			ais.action.master.helper.GenericActionDashboardHelper.refreshFromCriteria(dashboardHtml, progressHtml, this,
					"Dasbor Catatan Pembayaran",
					"Pantau data catatan pembayaran mahasiswa, jumlah tagihan, status lunas, dan pola pembayaran berdasarkan filter yang sedang dipakai.",
					paging == null ? -1L : paging.getTotalSize());
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:883");
			}
		}
	}

	public void prosesTagihan(final Component parent) {
		// -------------------------------------------------------------------------
		// 1. TOMBOL REKAP PEMBAYARAN
		// -------------------------------------------------------------------------
		MyToolbarbuttonConfig btnRekapPembayaran = new MyToolbarbuttonConfig("Rekap Pembayaran", "/img/excel.png");
		btnRekapPembayaran.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/pembayaran_mahasiswa_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");
				final File file = new File(filename);
				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				final Intbox colS = new Intbox(10);

				Clients.showBusy(label.getValue());

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Clients.showBusy(label.getValue());
							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {
								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								Common.clear(center);
								spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());

								spreadsheet.setMaxrows(intbox.getValue() + 3);
								spreadsheet.setMaxcolumns(colS.getValue());
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										try {
											Filedownload.save(new FileInputStream(file),
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}
						} catch (Exception e) {
							Clients.clearBusy();
						}
					}
				});
				timer.start();

				new Thread(new Runnable() {
					@SuppressWarnings({ "unchecked" })
					@Override
					public void run() {
						Session session = null;
						FileOutputStream fileOut = null;
						try {
							session = HibernateUtil.currentNativeSession();

							List<CicilanPembayaran> cicilanPembayarans = initCriteria(true, true).setMaxResults(1048576)
									.list();

							XSSFWorkbook workbook = new XSSFWorkbook();
							XSSFSheet sheet = workbook.createSheet("Rekap Pembayaran");
							sheet.setDefaultColumnWidth(20);
							int rowIndex = 0;

							XSSFRow rowhead = sheet.createRow((short) 0);
							rowhead.createCell(0).setCellValue("NIM/No.Reg");
							rowhead.createCell(1).setCellValue("Nama");
							rowhead.createCell(2).setCellValue("Fakultas");
							rowhead.createCell(3).setCellValue("Prodi");
							rowhead.createCell(4).setCellValue("Semester");
							rowhead.createCell(5).setCellValue("Jenis Pembayaran");
							rowhead.createCell(6).setCellValue("Item Biaya");
							rowhead.createCell(7).setCellValue("Bulan");
							rowhead.createCell(8).setCellValue("Nominal");
							rowhead.createCell(9).setCellValue("Hari/Tanggal/Waktu");
							rowhead.createCell(10).setCellValue("Keterangan");

							colS.setValue(11);

							for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
								try {
									BiodataCalonMahasiswa biodataCalonMahasiswa = cicilanPembayaran.getKegiatan()
											.getCalonMahasiswa();
									Mahasiswa mahasiswa = cicilanPembayaran.getKegiatan().getMahasiswa();

									rowIndex++;
									XSSFRow row = sheet.createRow(rowIndex);

									if (mahasiswa != null) {
										row.createCell(0).setCellValue(mahasiswa.getNim());
										row.createCell(1).setCellValue(mahasiswa.getNama());
										row.createCell(2)
												.setCellValue(mahasiswa.getJurusan() == null
														|| mahasiswa.getJurusan().getFakultas() == null ? ""
																: mahasiswa.getJurusan().getFakultas().getNama());
										row.createCell(3).setCellValue(
												mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());
										row.createCell(4).setCellValue(cicilanPembayaran.getKegiatan().getSemster());
										row.createCell(5).setCellValue(
												cicilanPembayaran.getKegiatan().getJenisKegiatan().getNamaKegiatan());
									} else if (biodataCalonMahasiswa != null) {
										row.createCell(0).setCellValue(biodataCalonMahasiswa.getNoRegistrasi());
										row.createCell(1).setCellValue(biodataCalonMahasiswa.getNama());

										Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus();
										if (jurusan == null) {
											jurusan = biodataCalonMahasiswa.getProdi1();
										}

										row.createCell(2)
												.setCellValue(jurusan == null || jurusan.getFakultas() == null ? ""
														: jurusan.getFakultas().getNama());
										row.createCell(3).setCellValue(jurusan == null ? "" : jurusan.getNama());
										row.createCell(4).setCellValue(cicilanPembayaran.getKegiatan().getSemster());
										row.createCell(5).setCellValue(
												cicilanPembayaran.getKegiatan().getJenisKegiatan().getNamaKegiatan());
									}

									row.createCell(6).setCellValue(cicilanPembayaran.getItemBiaya().getNama());
									row.createCell(7)
											.setCellValue(cicilanPembayaran.getPengaturanPembayaranBulanan() == null ? 0
													: cicilanPembayaran.getPengaturanPembayaranBulanan()
															.getRealBulan());
									row.createCell(8)
											.setCellValue(Common.numberFormat.get().format(cicilanPembayaran.getNilai()));
									row.createCell(9)
											.setCellValue(Common.dateFormat5.get().format(cicilanPembayaran.getTanggal()));
									row.createCell(10).setCellValue(cicilanPembayaran.getKeterangan());
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}

								// Clear session per 100 rows to prevent OutOfMemory
								if (rowIndex % 100 == 0) {
									session.flush();
									session.clear();
								}
							}

							intbox.setValue(cicilanPembayarans.size());

							fileOut = new FileOutputStream(filename);
							workbook.write(fileOut);
							fileOut.close();
							fileOut = null;
							label.setValue("");
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							label.setValue("-");
						} finally {
							if (fileOut != null) {
								try {
									fileOut.close();
								} catch (IOException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1092");
								}
							}
							if (session != null && session.isOpen()) {
								session.disconnect();
								session.close();
							}
							HibernateUtil.closeSession();
						}
					}
				}).start();
			}
		});
		parent.appendChild(btnRekapPembayaran);

		// -------------------------------------------------------------------------
		// 2. TOMBOL REKAP TAGIHAN DAN PEMBAYARAN
		// -------------------------------------------------------------------------
		MyToolbarbuttonConfig btnRekapTagihanPembayaran = new MyToolbarbuttonConfig("Rekap Tagihan dan Pembayaran",
				"/img/excel.png");
		btnRekapTagihanPembayaran.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/tagihan_dan_pembayaran_mahasiswa_" + URLEncoder.encode(
								Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");
				final File file = new File(filename);
				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				final Intbox colS = new Intbox(10);

				Clients.showBusy(label.getValue());

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Clients.showBusy(label.getValue());
							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {
								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								Common.clear(center);
								spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());

								spreadsheet.setMaxrows(intbox.getValue() + 3);
								spreadsheet.setMaxcolumns(colS.getValue());
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										try {
											Filedownload.save(new FileInputStream(file),
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}
						} catch (Exception e) {
							Clients.clearBusy();
						}
					}
				});
				timer.start();

				new Thread(new Runnable() {

					private Long proses(int rowIndex, XSSFSheet sheet, Long id, Kegiatan kegiatan,
							DetailBiaya detailBiaya, PengaturanPembayaranBulanan pengaturanPembayaranBulanan,
							List<CicilanPembayaran> cicilanPembayarans2, Collection<DetailKegiatan> detailKegiatans,
							Map<String, Double> nilais) {
						if (detailBiaya != null) {
							id = detailBiaya.getId();
						} else if (pengaturanPembayaranBulanan != null) {
							id = pengaturanPembayaranBulanan.getId();
						}

						if (detailBiaya == null && pengaturanPembayaranBulanan == null) {
							return -1L;
						}

						DetailBiaya tempdetailBiaya = null;
						PengaturanPembayaranBulanan temppengaturanPembayaranBulanan = null;

						if (pengaturanPembayaranBulanan != null) {
							temppengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) pengaturanPembayaranBulanan;
							if (temppengaturanPembayaranBulanan != null) {
								tempdetailBiaya = temppengaturanPembayaranBulanan.getDetailBiaya();
							}
						} else if (detailBiaya != null) {
							tempdetailBiaya = (DetailBiaya) detailBiaya;
						}

						DetailKegiatan tempdata = kegiatan == null ? null
								: temppengaturanPembayaranBulanan != null
										? kegiatan.ambilSatuDetailKegiatan(temppengaturanPembayaranBulanan,
												detailKegiatans)
										: kegiatan.ambilSatuDetailKegiatan(tempdetailBiaya);
						DetailKegiatan detailKegiatan = tempdata;
						if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
							return 0L;
						}

						BiodataCalonMahasiswa biodataCalonMahasiswa = kegiatan.getCalonMahasiswa();
						Mahasiswa mahasiswa = kegiatan.getMahasiswa();
						XSSFRow row = sheet.createRow(rowIndex);

						if (mahasiswa != null) {
							row.createCell(0).setCellValue(mahasiswa.getNim());
							row.createCell(1).setCellValue(mahasiswa.getNama());
							row.createCell(2).setCellValue(
									mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
											: mahasiswa.getJurusan().getFakultas().getNama());
							row.createCell(3).setCellValue(
									mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());
							row.createCell(4).setCellValue(mahasiswa.getProgram());
							row.createCell(5).setCellValue(kegiatan.getSemster());
							row.createCell(6).setCellValue(kegiatan.getJenisKegiatan() == null ? ""
									: kegiatan.getJenisKegiatan().getNamaKegiatan());
						} else if (biodataCalonMahasiswa != null) {
							row.createCell(0).setCellValue(biodataCalonMahasiswa.getNoRegistrasi());
							row.createCell(1).setCellValue(biodataCalonMahasiswa.getNama());

							Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus();
							if (jurusan == null) {
								jurusan = biodataCalonMahasiswa.getProdi1();
							}

							row.createCell(2).setCellValue(jurusan == null || jurusan.getFakultas() == null ? ""
									: jurusan.getFakultas().getNama());
							row.createCell(3).setCellValue(jurusan == null ? "" : jurusan.getNama());
							row.createCell(4).setCellValue(biodataCalonMahasiswa.getProgram());
							row.createCell(5).setCellValue(kegiatan.getSemster());
							row.createCell(6).setCellValue(kegiatan.getJenisKegiatan() == null ? ""
									: kegiatan.getJenisKegiatan().getNamaKegiatan());
						}

						Double dibayar = 0.0;
						if (pengaturanPembayaranBulanan != null) {
							Number sumCicilan = VOMahasiswa.hitungTotalCicilan(kegiatan, pengaturanPembayaranBulanan,
									cicilanPembayarans2);
							dibayar = sumCicilan == null ? 0.0 : sumCicilan.doubleValue();
						} else {
							String key = detailBiaya.getItemBiaya().getId() + "_" + detailBiaya.getBayarKe();
							if (nilais.containsKey(key)) {
								dibayar = nilais.get(key);
							}
						}

						StringBuilder tgl = new StringBuilder(); // Optimalisasi memori string
						if (pengaturanPembayaranBulanan != null) {
							for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans2) {
								if (cicilanPembayaran != null) {
									PengaturanPembayaranBulanan p = cicilanPembayaran.getPengaturanPembayaranBulanan();
									if (p != null
											&& p.getDetailBiaya().getItemBiaya().getId().equals(
													pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())
											&& p.getRealBulan().equals(pengaturanPembayaranBulanan.getRealBulan())) {
										tgl.append(Common.dateFormat5.get().format(cicilanPembayaran.getTanggal()))
												.append(", ");
									}
								}
							}
						} else {
							for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans2) {
								if (cicilanPembayaran != null) {
									DetailBiaya p = cicilanPembayaran.getDetailBiaya();
									if (p != null && p.getId().equals(detailBiaya.getId())) {
										tgl.append(Common.dateFormat5.get().format(cicilanPembayaran.getTanggal()))
												.append(", ");
									} else if (cicilanPembayaran.getItemBiaya() != null
											&& cicilanPembayaran.getItemBiaya().getId()
													.equals(detailBiaya.getItemBiaya().getId())
											&& cicilanPembayaran.getBayarKe().equals(detailBiaya.getBayarKe())) {
										tgl.append(Common.dateFormat5.get().format(cicilanPembayaran.getTanggal()))
												.append(", ");
									}
								}
							}
						}

						Date tanggalBayar = WaktuUtil.getDate();
						if (pengaturanPembayaranBulanan != null) {
							for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans2) {
								try {
									if (cicilanPembayaran.getItemBiaya() != null
											&& cicilanPembayaran.getItemBiaya().getId().equals(
													pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())
											&& pengaturanPembayaranBulanan.getDetailBiaya().getBayarKe()
													.equals(cicilanPembayaran.getBayarKe())
											&& cicilanPembayaran.getKegiatan().getId().equals(kegiatan.getId())) {
										if (pengaturanPembayaranBulanan != null
												&& cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
											PengaturanPembayaranBulanan p = cicilanPembayaran
													.getPengaturanPembayaranBulanan();
											if (p.getDetailBiaya().getItemBiaya().getId().equals(
													pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())
													&& p.getRealBulan()
															.equals(pengaturanPembayaranBulanan.getRealBulan())) {
												tanggalBayar = cicilanPembayaran.getTanggal();
											}
										}
									}
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
						}

						row.createCell(7).setCellValue(pengaturanPembayaranBulanan != null
								? ((pengaturanPembayaranBulanan.getDetailBiaya() != null
										&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null)
												? pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()
														+ " " + pengaturanPembayaranBulanan.getRealBulan()
												: "")
								: (detailBiaya.getItemBiaya() == null ? "" : detailBiaya.getItemBiaya().getNama()));

						Double nilaiBiayaHarusDiBayars = 0.0;
						if (pengaturanPembayaranBulanan != null) {
							Double tmpTagihan = mahasiswa != null
									? Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, mahasiswa,
											kegiatan.getSemster(), pengaturanPembayaranBulanan)
									: Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, kegiatan.getSemster(),
											pengaturanPembayaranBulanan);
							nilaiBiayaHarusDiBayars = tmpTagihan == null ? 0.0 : tmpTagihan;
							if (pengaturanPembayaranBulanan.getDetailBiaya() != null
									&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
									&& ItemBiaya.DIKALI_NILAI_MINUS.equals(
											pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
													.getPenghitungan())) {
								nilaiBiayaHarusDiBayars = -Math.abs(nilaiBiayaHarusDiBayars);
							}
						} else {
							Double tmpTagihan2 = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, false);
							nilaiBiayaHarusDiBayars = tmpTagihan2 == null ? 0.0 : tmpTagihan2;
							if (detailBiaya.getItemBiaya() != null
									&& ItemBiaya.DIKALI_NILAI_MINUS.equals(
											detailBiaya.getItemBiaya().getPenghitungan())) {
								nilaiBiayaHarusDiBayars = -Math.abs(nilaiBiayaHarusDiBayars);
							}
						}

						Double hasilDenda = detailKegiatan != null
								&& (detailKegiatan.getBatalkanDenda() || nilaiBiayaHarusDiBayars.intValue() == 0)
										? nilaiBiayaHarusDiBayars
										: detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom()
												? nilaiBiayaHarusDiBayars
												: pengaturanPembayaranBulanan != null
														? pengaturanPembayaranBulanan.checkDenda(nilaiBiayaHarusDiBayars,
																tanggalBayar, null, kegiatan.getJenisKegiatan())
														: nilaiBiayaHarusDiBayars;

						if (detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom()
								&& pengaturanPembayaranBulanan != null) {
							pengaturanPembayaranBulanan.setInfoDenda(" Penambahan denda senilai "
									+ Common.numberFormat.get().format(detailKegiatan.getDendaCustom()) + ".");
						}

						Double nilaiDenda = hasilDenda - nilaiBiayaHarusDiBayars;
						Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, pengaturanPembayaranBulanan,
								cicilanPembayarans2);

						nilaiBiayaHarusDiBayars = hasilDenda.intValue() > nilaiBiayaHarusDiBayars.intValue()
								? hasilDenda
								: nilaiBiayaHarusDiBayars;

						if ((detailBiaya != null && detailBiaya.getItemBiaya() != null
								&& ItemBiaya.DIKALI_NILAI_MINUS.equals(
										detailBiaya.getItemBiaya().getPenghitungan()))
								|| (pengaturanPembayaranBulanan != null
										&& pengaturanPembayaranBulanan.getDetailBiaya() != null
										&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
										&& ItemBiaya.DIKALI_NILAI_MINUS.equals(
												pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
														.getPenghitungan()))) {
							nilaiBiayaHarusDiBayars = -Math.abs(nilaiBiayaHarusDiBayars);
							telahDibayar = (telahDibayar == null ? 0.0 : -Math.abs(telahDibayar.doubleValue()));
						}

						row.createCell(8).setCellValue(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars));
						row.createCell(9).setCellValue(Common.numberFormat.get().format(dibayar));
						row.createCell(10).setCellValue(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars - dibayar));
						row.createCell(11).setCellValue(tgl.toString());
						row.createCell(12).setCellValue(Common.numberFormat.get().format(nilaiDenda));
						Double tot = nilaiBiayaHarusDiBayars + dibayar;
						if (tot.intValue() == 0) {
							return -1L;
						}

						return id;
					}

					@SuppressWarnings({ "rawtypes", "unchecked" })
					@Override
					public void run() {
						Session session = null;
						FileOutputStream fileOut = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();

							List<Long> kegiatansid = initCriteria(true, false).setProjection(Projections.property("id"))
									.setMaxResults(1048576).list();
							int size = kegiatansid.size();

							XSSFWorkbook workbook = new XSSFWorkbook();
							XSSFSheet sheet = workbook.createSheet("Rekap Tagihan dan Pembayaran");
							sheet.setDefaultColumnWidth(20);

							XSSFRow rowhead = sheet.createRow((short) 0);
							rowhead.createCell(0).setCellValue("NIM/No.Reg");
							rowhead.createCell(1).setCellValue("Nama");
							rowhead.createCell(2).setCellValue("Fakultas");
							rowhead.createCell(3).setCellValue("Prodi");
							rowhead.createCell(4).setCellValue("Program");
							rowhead.createCell(5).setCellValue("Semester");
							rowhead.createCell(6).setCellValue("Jenis Pembayaran");
							rowhead.createCell(7).setCellValue("Item Biaya");
							rowhead.createCell(8).setCellValue("Tagihan");
							rowhead.createCell(9).setCellValue("Dibayar");
							rowhead.createCell(10).setCellValue("Sisa");
							rowhead.createCell(11).setCellValue("Terakhir bayar Hari/Tanggal/Waktu");
							rowhead.createCell(12).setCellValue("Denda");
							colS.setValue(13);

							Long id = null;
							int rowIndex = 1;
							int index = 1;

							// Optimalisasi Loop: Langsung tarik dan proses di satu iterasi agar RAM tidak
							// meledak
							for (Long kegiatanid : kegiatansid) {
								try {
									Kegiatan kegiatan = (Kegiatan) GeneralValueObject.ambilData(Kegiatan.class,
											kegiatanid.toString(), true);
									if (kegiatan != null) {
										label.setValue("Memproses data tagihan dan pembayaran " + kegiatan + " ("
												+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

										Collection detailBiayas = new ArrayList();
										if (kegiatan.getCalonMahasiswa() != null) {
											BiodataCalonMahasiswa calonMahasiswa = kegiatan.getCalonMahasiswa();
											Jurusan prodiLulus = calonMahasiswa.getProdiLulus();
											if (prodiLulus == null || prodiLulus.getId() == null) {
												Jurusan myjurusan1 = calonMahasiswa.getProdi1() == null
														? calonMahasiswa.getProdi2()
														: calonMahasiswa.getProdi1();
												detailBiayas.addAll(PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(
														calonMahasiswa, kegiatan.getJenisKegiatan(), myjurusan1,
														kegiatan.getSemster(), false));
											} else {
												detailBiayas.addAll(PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(
														calonMahasiswa, kegiatan.getJenisKegiatan(), prodiLulus,
														kegiatan.getSemster(), false));
											}

											int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session,
													kegiatan.getCalonMahasiswa(), kegiatan.getJenisKegiatan(),
													kegiatan.getSemster(), detailBiayas, false, true);
											if (countPengaturanBulanan > 0) {
												detailBiayas = PembayaranUtil.getInstance()
														.getPengaturanPembayaranSemua(kegiatan.getCalonMahasiswa(),
																session, kegiatan.getSemster(),
																kegiatan.getJenisKegiatan(), detailBiayas, false,
																false);
											}
										} else if (kegiatan.getMahasiswa() != null) {
											detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(
													kegiatan.getMahasiswa(), kegiatan.getSemster(),
													kegiatan.getJenisKegiatan(), false);

											int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session,
													kegiatan.getMahasiswa(), kegiatan.getJenisKegiatan(),
													kegiatan.getSemster(), detailBiayas, false, true);
											if (countPengaturanBulanan > 0) {
												detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(
														kegiatan.getMahasiswa(), kegiatan.getSemster(),
														kegiatan.getJenisKegiatan(), "-1", false);
											}
										}

										List<CicilanPembayaran> mycicilanPembayarans = kegiatan.ambilCicilan();
										Map<String, Double> nilais = new HashMap<String, Double>();
										for (CicilanPembayaran cicilanPembayaran : mycicilanPembayarans) {
											if (cicilanPembayaran != null && cicilanPembayaran.getId() != null
													&& cicilanPembayaran.getItemBiaya() != null) {
												String key = cicilanPembayaran.getItemBiaya().getId() + "_"
														+ cicilanPembayaran.getBayarKe();
												if (nilais.containsKey(key)) {
													nilais.put(key, nilais.get(key) + cicilanPembayaran.getNilai());
												} else {
													nilais.put(key, cicilanPembayaran.getNilai());
												}
											}
										}

										Collection<DetailKegiatan> detailKegiatans = kegiatan.getId() == null ? null
												: kegiatan.ambilDetailKegiatan(false);

										for (Object o : detailBiayas) {
											try {
												DetailBiaya detailBiaya = null;
												PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
												if (o instanceof PengaturanPembayaranBulanan) {
													pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
												} else if (o instanceof DetailBiaya) {
													detailBiaya = (DetailBiaya) o;
												}

												Long kembali = proses(rowIndex, sheet, id, kegiatan, detailBiaya,
														pengaturanPembayaranBulanan, mycicilanPembayarans,
														detailKegiatans, nilais);
												if (kembali != null && !kembali.equals(-1L)) {
													id = kembali;
													rowIndex++;
												}
											} catch (Exception e) {
												try { ais.common.Common.tampilErrorJikaAdmin(e); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1556");}
											}
										}

										// Membuang objek yang tidak terpakai dari RAM
										mycicilanPembayarans.clear();
										detailBiayas.clear();
										nilais.clear();
									}
								} catch (Exception e) {
									try { ais.common.Common.tampilErrorJikaAdmin(e); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1566");}
								}

								// Clear Cache Hibernate setiap 50 Kegiatan diproses
								if (index % 50 == 0 && session != null && session.isOpen()) {
									try { session.flush(); } catch (Exception eFlush) { ais.common.ErrorAuditUtil.record(eFlush, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1571"); /* session read-only */ }
									try { session.clear(); } catch (Exception eClear) { ais.common.ErrorAuditUtil.record(eClear, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1572"); /* non-critical */ }
								}
								index++;
							}

							intbox.setValue(rowIndex + 1);

							fileOut = new FileOutputStream(filename);
							workbook.write(fileOut);
							fileOut.close();
							fileOut = null;
							label.setValue("");

						} catch (Exception e) {
							try { Common.tampilErrorJikaAdmin(e); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1586");}
							label.setValue("-");
						} finally {
							if (fileOut != null) {
								try {
									fileOut.close();
								} catch (IOException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1592");
								}
							}
							if (session != null && session.isOpen()) {
								try { session.clear(); } catch (Exception eSc) { ais.common.ErrorAuditUtil.record(eSc, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1596");}
								try { session.disconnect(); } catch (Exception eSd) { ais.common.ErrorAuditUtil.record(eSd, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1597");}
								try { session.close(); } catch (Exception eSx) { ais.common.ErrorAuditUtil.record(eSx, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1598");}
							}
							HibernateUtil.closeSession();
						}
					}
				}).start();
			}
		});
		parent.appendChild(btnRekapTagihanPembayaran);

		// -------------------------------------------------------------------------
		// 3. TOMBOL REKAP PER PRODI DAN ANGKATAN
		// -------------------------------------------------------------------------
		MyToolbarbuttonConfig btnRekapProdiAngkatan = new MyToolbarbuttonConfig("Rekap Per Prodi dan Angkatan",
				"/img/excel.png");
		btnRekapProdiAngkatan.addEventListener("onClick", new EventListener() {

			class DataVo {
				public Jurusan jurusan;
				public Integer angkatan;
				public Set<Long> jmlMhs = new HashSet<Long>();
				public Set<Long> jmlMhsBlmByr = new HashSet<Long>();
				public Set<Long> jmlMhsBlmLunas = new HashSet<Long>();
				public Set<Long> jmlMhsLunas = new HashSet<Long>();

				public Double tagihan = 0.0;
				public Double dibayar = 0.0;
			}

			private TreeMap<String, DataVo> treeMap;

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (searchJenisPembayaran.getSelectedItem() == null
						|| searchJenisPembayaran.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Filter jenis pembayaran harus dipilih", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				treeMap = new TreeMap<String, DataVo>();

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/tagihan_dan_pembayaran_mahasiswa_" + URLEncoder.encode(
								Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");
				final File file = new File(filename);
				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				final Intbox colS = new Intbox(10);

				Clients.showBusy(label.getValue());

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Clients.showBusy(label.getValue());
							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {
								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								Common.clear(center);
								spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());

								spreadsheet.setMaxrows(intbox.getValue() + 3);
								spreadsheet.setMaxcolumns(colS.getValue());
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										try {
											Filedownload.save(new FileInputStream(file),
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}
						} catch (Exception e) {
							Clients.clearBusy();
						}
					}
				});
				timer.start();

				new Thread(new Runnable() {
					private void proses(Kegiatan kegiatan) {
						BiodataCalonMahasiswa biodataCalonMahasiswa = kegiatan.getCalonMahasiswa();
						Mahasiswa mahasiswa = kegiatan.getMahasiswa();

						Jurusan jurusan = null;
						Integer tahun = null;
						String key = "";

						if (mahasiswa != null && mahasiswa.getJurusan() != null) {
							jurusan = mahasiswa.getJurusan();
							tahun = mahasiswa.getTahunangkatan();
							key = mahasiswa.getJurusan().getId() + "_" + mahasiswa.getTahunangkatan();
						} else if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getProdiLulus() != null) {
							jurusan = biodataCalonMahasiswa.getProdiLulus();
							tahun = biodataCalonMahasiswa.getTahun();
							key = biodataCalonMahasiswa.getProdiLulus().getId() + "_"
									+ biodataCalonMahasiswa.getTahun();
						} else if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getProdi1() != null) {
							jurusan = biodataCalonMahasiswa.getProdi1();
							tahun = biodataCalonMahasiswa.getTahun();
							key = biodataCalonMahasiswa.getProdi1().getId() + "_" + biodataCalonMahasiswa.getTahun();
						} else {
							return; // Skip jika tidak ada mapping valid untuk mencegah error
						}

						DataVo dataVo = treeMap.get(key);
						if (dataVo == null) {
							dataVo = new DataVo();
						}

						dataVo.angkatan = tahun;
						dataVo.jurusan = jurusan;
						if (mahasiswa != null) {
							dataVo.jmlMhs.add(mahasiswa.getId());
						} else if (biodataCalonMahasiswa != null) {
							dataVo.jmlMhs.add(biodataCalonMahasiswa.getId());
						}

						if (kegiatan.getPersentaseLunas().intValue() == 0) {
							if (mahasiswa != null) {
								dataVo.jmlMhsBlmByr.add(mahasiswa.getId());
							} else if (biodataCalonMahasiswa != null) {
								dataVo.jmlMhsBlmByr.add(biodataCalonMahasiswa.getId());
							}
						} else if (kegiatan.getPersentaseLunas() > 0.1 && kegiatan.getPersentaseLunas() < 99.99) {
							if (mahasiswa != null) {
								dataVo.jmlMhsBlmLunas.add(mahasiswa.getId());
							} else if (biodataCalonMahasiswa != null) {
								dataVo.jmlMhsBlmLunas.add(biodataCalonMahasiswa.getId());
							}
						} else if (kegiatan.getPersentaseLunas().intValue() == 100) {
							if (mahasiswa != null) {
								dataVo.jmlMhsLunas.add(mahasiswa.getId());
							} else if (biodataCalonMahasiswa != null) {
								dataVo.jmlMhsLunas.add(biodataCalonMahasiswa.getId());
							}
						}

						dataVo.tagihan = dataVo.tagihan + (kegiatan.getAmount() + kegiatan.getAmountTerhutang());
						dataVo.dibayar = dataVo.dibayar + (kegiatan.getAmount());

						treeMap.put(key, dataVo);
					}

					@SuppressWarnings({ "unchecked" })
					@Override
					public void run() {
						Session session = null;
						FileOutputStream fileOut = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();
							List<Long> kegiatansid = initCriteria(true, false).setProjection(Projections.property("id"))
									.setMaxResults(1048576).list();

							int size = kegiatansid.size();
							int index = 1;

							for (Long kegiatanid : kegiatansid) {
								try {
									Kegiatan kegiatan = (Kegiatan) GeneralValueObject.ambilData(Kegiatan.class,
											kegiatanid.toString(), true);
									if (kegiatan != null) {
										label.setValue("Memproses data tagihan " + kegiatan + " ("
												+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");
										proses(kegiatan);
									}

									// Clear session per 50 rows
									if (index % 50 == 0 && session != null && session.isOpen()) {
										try { session.flush(); } catch (Exception eFlush) { ais.common.ErrorAuditUtil.record(eFlush, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1820"); /* session read-only */ }
										try { session.clear(); } catch (Exception eClear) { ais.common.ErrorAuditUtil.record(eClear, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1821"); /* non-critical */ }
									}
									index++;
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}
							}

							XSSFWorkbook workbook = new XSSFWorkbook();
							XSSFSheet sheet = workbook.createSheet("Rekap Tagihan dan Pembayaran");
							sheet.setDefaultColumnWidth(20);

							XSSFRow rowhead = sheet.createRow((short) 0);
							rowhead.createCell(0).setCellValue("Prodi");
							rowhead.createCell(1).setCellValue("Angkatan");
							rowhead.createCell(2).setCellValue("Jumlah Blm Bayar (sesuai filter)");
							rowhead.createCell(3).setCellValue("Jumlah Blm Lunas (sesuai filter)");
							rowhead.createCell(4).setCellValue("Jumlah Lunas (sesuai filter)");
							rowhead.createCell(5).setCellValue("Total Tagihan");
							rowhead.createCell(6).setCellValue("Total Dibayar");
							rowhead.createCell(7).setCellValue("%");

							colS.setValue(12);
							int rowIndex = 1;
							for (DataVo dataVo : treeMap.values()) {
								XSSFRow row = sheet.createRow(rowIndex);
								row.createCell(0).setCellValue(dataVo.jurusan.getNama());
								row.createCell(1).setCellValue(dataVo.angkatan);
								row.createCell(2).setCellValue(Common.numberFormat.get().format(dataVo.jmlMhsBlmByr.size()));
								row.createCell(3)
										.setCellValue(Common.numberFormat.get().format(dataVo.jmlMhsBlmLunas.size()));
								row.createCell(4).setCellValue(Common.numberFormat.get().format(dataVo.jmlMhsLunas.size()));
								row.createCell(5).setCellValue(Common.numberFormat.get().format(dataVo.tagihan));
								row.createCell(6).setCellValue(Common.numberFormat.get().format(dataVo.dibayar));
								row.createCell(7).setCellValue(
										Common.numberFormat.get().format((dataVo.dibayar * 100.0) / dataVo.tagihan) + " %");
								rowIndex++;
							}

							intbox.setValue(rowIndex + 1);

							fileOut = new FileOutputStream(filename);
							workbook.write(fileOut);
							fileOut.close();
							fileOut = null;
							label.setValue("");

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							label.setValue("-");
						} finally {
							if (fileOut != null) {
								try {
									fileOut.close();
								} catch (IOException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1875");
								}
							}
							if (session != null && session.isOpen()) {
								try { session.clear(); } catch (Exception eSc) { ais.common.ErrorAuditUtil.record(eSc, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1879");}
								try { session.disconnect(); } catch (Exception eSd) { ais.common.ErrorAuditUtil.record(eSd, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1880");}
								try { session.close(); } catch (Exception eSx) { ais.common.ErrorAuditUtil.record(eSx, "auto-audit(empty-catch) src/ais/action/master/KegiatanAction.java:1881");}
							}
							HibernateUtil.closeSession();
						}
					}
				}).start();
			}
		});
		parent.appendChild(btnRekapProdiAngkatan);

		// -------------------------------------------------------------------------
		// 4. TOMBOL HISTORY
		// -------------------------------------------------------------------------
		MyToolbarbuttonConfig btnHistory = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		btnHistory.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				RevisiKegiatanHelper revisiHelper = new RevisiKegiatanHelper(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();
			}
		});
		btnHistory.setParent(parent);
	}

}
