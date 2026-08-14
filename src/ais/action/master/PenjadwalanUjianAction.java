package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.AbsensiHelper;
import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.master.helper.AmbilDataKurikulumBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.PenjadwalanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.akademik.LaporanDaftarUjian;
import ais.action.report.format1.akademik.LaporanJadwalKartuUjian;
import ais.action.report.format1.akademik.LaporanJadwalPengawasUjian;
import ais.action.report.format1.akademik.LaporanJadwalUjian;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.action.master.helper.FilterLanjutHelper;

public class PenjadwalanUjianAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 3786091220301468178L;
	protected MyGrid grid;
	protected Paging paging;

	protected AmbilDataMatakuliahBanbox searchmatakuliah;
	protected AmbilDataDosenBanbox searchdosen;
	protected AmbilDataMahasiswaBanbox searchasisten;

	protected Combobox searchhari;

	protected AmbilDataKelasBanbox searchkelas;
	protected MyCheckboxConfig searchparalel;
	protected MyCheckboxConfig searchtanpakelas;
	protected AmbilDataRuangBanbox searchruang;

	protected Combobox searchTahap;

	protected Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;
	protected Combobox searchsemester;

	protected Combobox searchprogram;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;

	protected AmbilDataMasaPerkuliahanBanbox searchmasaperkulaiahan;
	protected AmbilDataKurikulumBanbox searchkurikulum;
	protected Textbox searchnamamhs;
	protected Textbox searchnim;

	protected Textbox searchnamadsn;
	protected Textbox searchnamamk;
	protected Textbox searchKeterangan;
	private boolean edit = false;
	protected Textbox searchnamaasisten;

	protected MyToolbarbuttonConfig find;

	protected Tbmuser tbmuser = Common.getCurrentUser();

	protected Tbmuser users;

	protected SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm");

	protected Tabpanel laporanJadwalUjian;
	protected Tabpanel laporanJadwalPengawasUjian;
	protected Tabpanel laporanDaftarHadirUjian;
	protected Tabpanel laporanKartuUjian;

	protected Integer semesterPendek = null;
	protected Integer ekstrakurikuler = null;
	private Dosen dosen;

	protected PenjadwalanHelper penjadwalanHelper = new PenjadwalanHelper();

	public void onTampiliLaporanJadwalPengawasUjian(Event event) {

		if (laporanJadwalPengawasUjian.getChildren().size() == 0) {
			LaporanJadwalPengawasUjian laporanJadwalUjian = new LaporanJadwalPengawasUjian();
			laporanJadwalUjian.setHeight("100%");
			laporanJadwalUjian.setWidth("100%");
			laporanJadwalUjian.setParent(this.laporanJadwalPengawasUjian);
		}
	}

	public void onTampiliLaporanJadwalUjian(Event event) {

		if (laporanJadwalUjian.getChildren().size() == 0) {
			LaporanJadwalUjian laporanJadwalUjian = new LaporanJadwalUjian();
			laporanJadwalUjian.setHeight("100%");
			laporanJadwalUjian.setWidth("100%");
			laporanJadwalUjian.setParent(this.laporanJadwalUjian);
		}
	}

	public void onTampilDaftarHadirUjian(Event event) {

		if (laporanDaftarHadirUjian.getChildren().size() == 0) {
			LaporanDaftarUjian laporanDaftarUjian = new LaporanDaftarUjian();
			laporanDaftarUjian.setHeight("100%");
			laporanDaftarUjian.setWidth("100%");
			laporanDaftarUjian.setParent(this.laporanDaftarHadirUjian);
		}
	}

	public void onTampiliKartuUjian(Event event) {

		if (laporanKartuUjian.getChildren().size() == 0) {
			LaporanJadwalKartuUjian laporanJadwalUjian = new LaporanJadwalKartuUjian();
			laporanJadwalUjian.setHeight("100%");
			laporanJadwalUjian.setWidth("100%");
			laporanJadwalUjian.setParent(this.laporanKartuUjian);
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

		tbmuser = Common.getCurrentUser();
		dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		if (mynorth != null && tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {

			if (laporanJadwalUjian != null) {
				laporanJadwalUjian.setVisible(false);
				laporanJadwalUjian.getLinkedTab().setVisible(false);

				laporanKartuUjian.setVisible(false);
				laporanKartuUjian.getLinkedTab().setVisible(false);

				laporanJadwalPengawasUjian.setVisible(false);
				laporanJadwalPengawasUjian.getLinkedTab().setVisible(false);
				laporanDaftarHadirUjian.setVisible(false);
				laporanDaftarHadirUjian.getLinkedTab().setVisible(false);
			}

			Common.clear(mynorth);

			ta = new Combobox();
			smt = new Combobox();
			hari = new Combobox();

			Comboitem comboitem;
			for (String h : Common.haris) {
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(h);
				comboitem.setValue(h);
				hari.appendChild(comboitem);
			}
			comboitem = new Comboitem();
			comboitem.setLabel("=hari=");
			comboitem.setValue(null);
			hari.appendChild(comboitem);
			hari.setReadonly(true);
			hari.setSelectedItem(comboitem);

			comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			smt.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			smt.appendChild(comboitem);

			MyComboitemConfig comboitemSp = new MyComboitemConfig();
			comboitemSp.setLabel(Perkuliahan.SP);
			comboitemSp.setValue(Perkuliahan.SP);
			smt.appendChild(comboitemSp);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("=smt=");
			comboitem.setValue(null);
			smt.appendChild(comboitem);
			smt.setReadonly(true);

			if (semesterPendek != null) {
				smt.setSelectedItem(comboitemSp);
				smt.setDisabled(true);
			} else {
				smt.setSelectedItem(comboitem);
			}
			Common.generateTahunAjaranDanSemua(ta);
			Common.selectComboItem(ta, Common.getCurrentTahunAkademik());
			keyword = new Textbox();
			keyword.setCols(Common.isMobile() ? 5 : 10);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(new Event("cari"));
				}
			};

			ta.addEventListener("onChange", eventListener);
			smt.addEventListener("onChange", eventListener);
			keyword.addEventListener("onOK", eventListener);
			hari.addEventListener("onChange", eventListener);

			int jumlahDataDalamSatuHalamanElearning = 10;
			Common.initPagingCustom(paging, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			}, jumlahDataDalamSatuHalamanElearning);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");

			Toolbar toolbar = new Toolbar();
			if (!Common.isMobile())
				toolbar.appendChild(new MyLabelBoldConfig("TA :"));
			else
				ta.setCols(3);
			toolbar.appendChild(ta);

			if (!Common.isMobile()) {
				toolbar.appendChild(new Space());
				toolbar.appendChild(new MyLabelBoldConfig("Smt :"));
			} else
				smt.setCols(2);
			toolbar.appendChild(smt);
			if (!Common.isMobile()) {
				toolbar.appendChild(new Space());
				toolbar.appendChild(new MyLabelBoldConfig("Hari :"));
			} else
				hari.setCols(3);
			toolbar.appendChild(hari);

			if (!Common.isMobile()) {
				toolbar.appendChild(new Space());
				toolbar.appendChild(new MyLabelBoldConfig("Dosen/Mk :"));
			}
			toolbar.appendChild(keyword);
			toolbar.appendChild(button);
			toolbar.appendChild(new Space());
			toolbar.setParent(mynorth);

			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(new Event("cari"));
				}
			});
			onSearchDefault(null);

		} else {

			if (searchasisten != null) {

				if (tbmuser != null && tbmuser.getMahasiswa() != null) {
					searchasisten.setAttribute("mahasiswa", tbmuser.getMahasiswa());
					searchasisten.setValue(tbmuser.getMahasiswa().getNama());
					searchasisten.setDisabled(true);
				}

				searchasisten.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});
			}
			for (String h : Common.haris) {
				MyComboitemConfig comboitem = new MyComboitemConfig();
				comboitem.setLabel(h);
				comboitem.setValue(h);
				searchhari.appendChild(comboitem);
			}

			MyComboitemConfig comboitem = new MyComboitemConfig();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			searchhari.appendChild(comboitem);
			searchhari.setSelectedItem(comboitem);

			searchJenisSemester.setReadonly(true);
			searchTahunAjaran.setReadonly(true);

			searchkelas.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

			searchkurikulum.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

			searchmasaperkulaiahan.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

			searchmatakuliah.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});
			searchdosen.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});

			searchruang.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			searchJenisSemester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			searchJenisSemester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			searchJenisSemester.appendChild(comboitem);

			// Filter "Semester Pendek (SP)" — pengganti tab "SP" yang dihilangkan. Bila dipilih, daftar
			// disaring HANYA perkuliahan Semester Pendek (statusSemesterPendek = SEMESTER_PENDEK), tanpa
			// batasan Ganjil/Genap — persis seperti perilaku tab "SP" dulu. Nilai combo = Perkuliahan.SP.
			comboitem = new MyComboitemConfig();
			if (comboitem != null) { comboitem.setLabel("Semester Pendek (SP)"); }
			if (comboitem != null) { comboitem.setValue(Perkuliahan.SP); }
			searchJenisSemester.appendChild(comboitem);

			if (Common.bolehKonfigurasi("pilihan_semester_di_perkuliahan_dibuat_default_semua_aja")) {
				searchJenisSemester.setSelectedItem(comboitem);
			} else {
				Common.selectComboItem(searchJenisSemester,
						Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
			}

			final EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(searchsemester);
					searchsemester.setSelectedItem(null);

					if (searchJenisSemester.getSelectedItem() == null) {
						return;
					}

					// "Semua" (null) DAN "Semester Pendek (SP)" sama-sama menampilkan seluruh nomor semester,
					// karena SP tidak dibatasi ganjil/genap.
					if (searchJenisSemester.getSelectedItem().getValue() == null
							|| Perkuliahan.SP.equals(searchJenisSemester.getSelectedItem().getValue())) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
						searchsemester.appendChild(comboitem);
						for (int i = 1; i < 30; i++) {
							comboitem = new MyComboitemConfig();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemester.appendChild(comboitem);
						}
					} else {

						Boolean genap = searchJenisSemester.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
						searchsemester.appendChild(comboitem);
						if (genap) {
							for (int i : Common.genap) {
								if (i == 0)
									continue;
								comboitem = new MyComboitemConfig();
								comboitem.setLabel(i + "");
								comboitem.setValue(i);
								searchsemester.appendChild(comboitem);
							}
						} else {
							for (int i : Common.ganjil) {
								comboitem = new MyComboitemConfig();
								comboitem.setLabel(i + "");
								comboitem.setValue(i);
								searchsemester.appendChild(comboitem);
							}
						}
					}

					searchsemester.setSelectedIndex(0);
					searchsemester.setReadonly(true);
				}
			};

			searchJenisSemester.addEventListener("onChange", eventListener);
			eventListener.onEvent(null);

			Common.generateTahunAjaranDanSemua(searchTahunAjaran);
			Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

			Common.initPrograms(searchprogram);

			// System.out.println(users);
			if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
				// Common.selectComboItem(searchdosen, dosen);
				searchdosen.setValue(dosen.getNama());
				searchdosen.setAttribute("myValue", dosen);
				searchdosen.setDisabled(true);
			}

			final String[] contents = new String[] { "id", "matakuliah", "dosen1", "jurusan", "ruang", "semester",
					"program", "waktuMulai", "waktuSelesai", "hari", "tahunAjaran", "kelas" };

			List<String> columnHeadersAdding = new ArrayList<String>();
			columnHeadersAdding.add("Tanggal UTS");
			columnHeadersAdding.add("Jam Mulai UTS");
			columnHeadersAdding.add("Jam Sampai UTS");
			columnHeadersAdding.add("Ruang UTS");
			columnHeadersAdding.add("Tanggal UAS");
			columnHeadersAdding.add("Jam Mulai UAS");
			columnHeadersAdding.add("Jam Sampai UAS");
			columnHeadersAdding.add("Ruang UAS");

			EventListener dataAdding = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = (Object[]) arg0.getData();
					Perkuliahan perkuliahan = (Perkuliahan) objects[0];

					XSSFRow row = (XSSFRow) objects[2];
					XSSFWorkbook workbook = (XSSFWorkbook) objects[3];

					final XSSFCellStyle hlink_style = workbook.createCellStyle();
					hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
					hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

					Pertemuan uts = Common.ambilPertemuan(ConstantValues.UTS, perkuliahan);
					Pertemuan uas = Common.ambilPertemuan(ConstantValues.UAS, perkuliahan);

					XSSFCell cell = row.createCell(contents.length);
					cell.setCellStyle(hlink_style);
					cell.setCellValue(uts.getTanggal() == null ? "" : Common.dateFormat1.get().format(uts.getTanggal()));

					cell = row.createCell(contents.length + 1);
					cell.setCellStyle(hlink_style);
					cell.setCellValue(uts.getWaktuMulai());

					cell = row.createCell(contents.length + 2);
					cell.setCellStyle(hlink_style);
					cell.setCellValue(uts.getWaktuSelesai());

					cell = row.createCell(contents.length + 3);
					cell.setCellStyle(hlink_style);
					cell.setCellValue(uts.getRuang() == null ? "" : uts.getRuang().toString());

					cell = row.createCell(contents.length + 4);
					cell.setCellStyle(hlink_style);
					cell.setCellValue(uas.getTanggal() == null ? "" : Common.dateFormat1.get().format(uas.getTanggal()));

					cell = row.createCell(contents.length + 5);
					cell.setCellStyle(hlink_style);
					cell.setCellValue(uas.getWaktuMulai());

					cell = row.createCell(contents.length + 6);
					cell.setCellStyle(hlink_style);
					cell.setCellValue(uas.getWaktuSelesai());

					cell = row.createCell(contents.length + 7);
					cell.setCellStyle(hlink_style);
					cell.setCellValue(uas.getRuang() == null ? "" : uas.getRuang().toString());

				}
			};

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Perkuliahan.class, this,
					"Download Data Jadwal", "/img/print.png", columnHeadersAdding, dataAdding, true, null, contents);
			Common.appendKeToolbar(cetakToolbarbutton, find, comp);

			MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
					"Upload Data Jadwal" + Common.ukuranLabelFileUpload(), "/img/excel.png");
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
						// System.out.println("file = " +
						// file.getAbsolutePath());
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
								uploadDataMahasiswa(file, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										onSearchDefault(arg0);
										Clients.clearBusy();
									}
								}, contents);
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
			Common.appendKeToolbar(upload, find, comp);
		}
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	public void uploadDataMahasiswa(final File file, final EventListener eventListener, final String[] contents)
			throws Exception {

		final Label peringatan = new Label("");

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					MyMessageboxConfig.show(
							"Upload data jadwal berhasil dilakukan."
									+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					Session session = HibernateUtil.currentNativeSession();
					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						@SuppressWarnings("rawtypes")
						Map datum = null;
						try {

							Long id = Common.getSheetContentAsLong(sheet, 0, i);
							Perkuliahan perkuliahan = id == null || id.equals(-1L) ? null
									: (Perkuliahan) session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.idEq(id))
											.uniqueResult();
							if (perkuliahan == null) {
								continue;
							}

							Pertemuan uts = Common.ambilPertemuan(ConstantValues.UTS, perkuliahan);
							Pertemuan uas = Common.ambilPertemuan(ConstantValues.UAS, perkuliahan);

							Date tglUts = Common.getSheetContentAsDate(sheet, contents.length, i);
							String mulaiUts = Common.getSheetContentAsString(sheet, contents.length + 1, i);
							String sampaiUts = Common.getSheetContentAsString(sheet, contents.length + 2, i);
							Ruang ruangUts = (Ruang) Common.getSheetContentAsObject(sheet, contents.length + 3, i,
									Ruang.class);

							System.out.println("tglUts=>" + tglUts + ", mulaiUts=>" + mulaiUts + ", sampaiUts=>"
									+ sampaiUts + ", ruangUts=>" + ruangUts);

							uts.setTanggal(tglUts);
							uts.setRuang(ruangUts);
							uts.setWaktuMulai(mulaiUts);
							uts.setWaktuSelesai(sampaiUts);

							Date tglUas = Common.getSheetContentAsDate(sheet, contents.length + 4, i);
							String mulaiUas = Common.getSheetContentAsString(sheet, contents.length + 5, i);
							String sampaiUas = Common.getSheetContentAsString(sheet, contents.length + 6, i);
							Ruang ruangUas = (Ruang) Common.getSheetContentAsObject(sheet, contents.length + 7, i,
									Ruang.class);

							System.out.println("tglUas=>" + tglUas + ", mulaiUas=>" + mulaiUas + ", sampaiUas=>"
									+ sampaiUas + ", ruangUas=>" + ruangUas);

							uas.setTanggal(tglUas);
							uas.setRuang(ruangUas);
							uas.setWaktuMulai(mulaiUas);
							uas.setWaktuSelesai(sampaiUas);

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, uts);
							session.getTransaction().commit();

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, uas);
							session.getTransaction().commit();

							label.setValue("Upload data \"" + perkuliahan.toString() + "\" ("
									+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

						} catch (Exception e) {
							System.out.println("error --> datum=>" + datum);
							Common.tampilErrorJikaAdmin(e);
							try {
								HibernateUtil.rollbackTransaction();
							} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/PenjadwalanUjianAction.java:779");

							}
						}
					}
					HibernateUtil.closeSession();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/PenjadwalanUjianAction.java:787");
				}

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

	protected void reloadPertemuan(final Pertemuan pertemuan, final Groupbox groupbox) throws Exception {
		Common.clear(groupbox);
		groupbox.appendChild(new Caption(pertemuan.info()));
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(groupbox);
		Rows rows = new Rows();
		rows.setParent(grid);

		final MyDatebox tanggal = pertemuan.getTanggal() == null ? new MyDatebox()
				: new MyDatebox(pertemuan.getTanggal());
		tanggal.setFormat("dd-MM-yyyy");
		tanggal.setReadonly(false);

		Date dateMulai = null;
		Date dateSelesai = null;
		try {
			if (!pertemuan.getWaktuMulai().equals(""))
				dateMulai = dateFormat.parse(pertemuan.getWaktuMulai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PenjadwalanUjianAction.java:817");
			// Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (!pertemuan.getWaktuSelesai().equals(""))
				dateSelesai = dateFormat.parse(pertemuan.getWaktuSelesai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PenjadwalanUjianAction.java:823");
			// Common.tampilErrorJikaAdmin(e);
		}

		final MyTimebox waktuMulai = new MyTimebox(dateMulai == null ? ais.ui.util.WaktuUtil.getDate() : dateMulai);
		final MyTimebox waktuSelesai = new MyTimebox(
				dateSelesai == null ? ais.ui.util.WaktuUtil.getDate() : dateSelesai);
		final AmbilDataRuangBanbox ruang = new AmbilDataRuangBanbox();
		ruang.setValue(pertemuan.getRuang() == null ? "" : pertemuan.getRuang().getNama());
		ruang.setAttribute("ruang", pertemuan.getRuang());

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		// Sebelumnya 85% → total kolom 110% sehingga isi form meluber ke kanan (tidak rapi).
		// Dijadikan 75% agar 25% + 75% = 100% (pas selebar grid).
		column.setWidth("75%");

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan, "Tanggal " + pertemuan.getStatusPertemuan().getNama())
				.setParent(row);
		tanggal.setParent(row);
		// tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		new Label("Waktu " + pertemuan.getStatusPertemuan().getNama()).setParent(row);
		Hbox hbox = new Hbox();
		hbox.setWidth("90%");
		hbox.setAlign("center");
		hbox.setStyle("gap:8px;");
		row.appendChild(hbox);
		// Lebar tetap agar kotak jam mulai & selesai sejajar rapi dan label "s.d" berada di
		// tengah keduanya (sebelumnya 90% membuat jam selesai terdorong jauh ke kanan).
		waktuMulai.setParent(hbox);
		waktuMulai.setWidth("120px");
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(waktuSelesai);
		waktuSelesai.setWidth("120px");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang " + pertemuan.getStatusPertemuan().getNama()));
		row.appendChild(ruang);
		ruang.setWidth("90%");

		Pegawai petugas = (Pegawai) (pertemuan.getPetugas() == null ? null
				: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas()));

		Pegawai petugas2 = (Pegawai) (pertemuan.getPetugas2() == null ? null
				: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas2()));

		Pegawai petugas3 = (Pegawai) (pertemuan.getPetugas3() == null ? null
				: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas3()));

		Pegawai petugas4 = (Pegawai) (pertemuan.getPetugas4() == null ? null
				: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas4()));

		Dosen pjawabDosen = (Dosen) (pertemuan.getPjDosen() == null ? null
				: ConstantValues.ambil(Dosen.class.getName(), pertemuan.getPjDosen()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pengawas " + pertemuan.getStatusPertemuan().getNama()));

		// Wadah pengawas memakai flex-wrap agar keempat kotak pengawas tertata rapi dan
		// otomatis turun baris bila tidak muat (sebelumnya Hbox lebar 90% membuat kotak
		// melebar/meluber keluar kolom dan tidak sejajar).
		org.zkoss.zul.Div pengawasFlow = new org.zkoss.zul.Div();
		pengawasFlow.setWidth("90%");
		pengawasFlow.setStyle("display:flex;flex-wrap:wrap;gap:6px;align-items:center;");
		row.appendChild(pengawasFlow);
		final AmbilDataPegawaiBanbox pegawai;
		pengawasFlow.appendChild(pegawai = new AmbilDataPegawaiBanbox(false));
		pegawai.setWidth("150px");
		pegawai.setAttribute("pegawai", petugas);
		pegawai.setValue(petugas == null ? null : petugas.getNama());
		pegawai.setReadonly(true);

		final AmbilDataPegawaiBanbox pegawai2;
		pengawasFlow.appendChild(pegawai2 = new AmbilDataPegawaiBanbox(false));
		pegawai2.setWidth("150px");
		pegawai2.setAttribute("pegawai", petugas2);
		pegawai2.setValue(petugas2 == null ? null : petugas2.getNama());
		pegawai2.setReadonly(true);

		final AmbilDataPegawaiBanbox pegawai3;
		pengawasFlow.appendChild(pegawai3 = new AmbilDataPegawaiBanbox(false));
		pegawai3.setWidth("150px");
		pegawai3.setAttribute("pegawai", petugas3);
		pegawai3.setValue(petugas3 == null ? null : petugas3.getNama());
		pegawai3.setReadonly(true);

		final AmbilDataPegawaiBanbox pegawai4;
		pengawasFlow.appendChild(pegawai4 = new AmbilDataPegawaiBanbox(false));
		pegawai4.setWidth("150px");
		pegawai4.setAttribute("pegawai", petugas4);
		pegawai4.setValue(petugas4 == null ? null : petugas4.getNama());
		pegawai4.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Penanggungjawab Dosen " + pertemuan.getStatusPertemuan().getNama()));

		final AmbilDataDosenBanbox pjDosen;
		row.appendChild(pjDosen = new AmbilDataDosenBanbox(false));
		pjDosen.setWidth("90%");
		pjDosen.setAttribute("dosen", pjawabDosen);
		pjDosen.setValue(pjawabDosen == null ? null : pjawabDosen.getNama());
		pjDosen.setReadonly(true);

		final Textbox catatan = new Textbox(pertemuan.getCatatan());
		catatan.setWidth("90%");
		catatan.setRows(3);

		class PertemuanChangeListener implements EventListener {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pegawai petugas = (Pegawai) pegawai.getAttribute("pegawai");
				Pegawai petugas2 = (Pegawai) pegawai2.getAttribute("pegawai");
				Pegawai petugas3 = (Pegawai) pegawai3.getAttribute("pegawai");
				Pegawai petugas4 = (Pegawai) pegawai4.getAttribute("pegawai");
				Dosen pjawabDosen = (Dosen) pjDosen.getAttribute("dosen");
				System.out.println("========= Ganti Waktu ujian =========");
				pertemuan.setTanggal(tanggal.getValue());
				pertemuan.setTanggalEdit(tanggal.getValue());
				pertemuan.setMulai(tanggal.getValue());
				pertemuan.setRuang((Ruang) ruang.getAttribute("ruang"));
				pertemuan.setWaktuMulai(dateFormat.format(waktuMulai.getValue()));
				pertemuan.setWaktuSelesai(dateFormat.format(waktuSelesai.getValue()));
				pertemuan.setPetugas(petugas == null ? null : petugas.getId());
				pertemuan.setPetugas2(petugas2 == null ? null : petugas2.getId());
				pertemuan.setPetugas3(petugas3 == null ? null : petugas3.getId());
				pertemuan.setPetugas4(petugas4 == null ? null : petugas4.getId());
				pertemuan.setPjDosen(pjawabDosen == null ? null : pjawabDosen.getId());
				pertemuan.setCatatan(catatan.getValue());

				HibernateUtil.currentSession().update(pertemuan);

			}

		}

		PertemuanChangeListener changeListener = new PertemuanChangeListener();
		tanggal.addEventListener(Events.ON_CHANGE, changeListener);
		waktuMulai.addEventListener(Events.ON_CHANGE, changeListener);
		waktuSelesai.addEventListener(Events.ON_CHANGE, changeListener);
		ruang.setEventListener(changeListener);
		pegawai.setEventListener(changeListener);
		pegawai2.setEventListener(changeListener);
		pegawai3.setEventListener(changeListener);
		pegawai4.setEventListener(changeListener);
		pjDosen.setEventListener(changeListener);
		catatan.addEventListener(Events.ON_CHANGE, changeListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan " + pertemuan.getStatusPertemuan().getNama()));
		row.appendChild(catatan);
		catatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		// Baris tombol aksi dibentangkan penuh (colspan 2) dan di-tengah-kan agar tombol-tombol
		// (Daftar Hadir, Absensi, Tugas, dll.) tertata rapi, tidak terpotong/menggantung di kanan.
		org.zkoss.zul.Cell aksiCell = new org.zkoss.zul.Cell();
		aksiCell.setColspan(2);
		aksiCell.setStyle("text-align:center;");
		aksiCell.setParent(row);

		Component bb = AbsensiHelper.createTombolAbsen(pertemuan, true, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					reloadPertemuan(pertemuan, groupbox);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});

		MyToolbarbutton button = new MyToolbarbutton("fa-file-text-o",
				"Daftar Hadir Peserta " + pertemuan.getStatusPertemuan().getNama());
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(pertemuan.getPerkuliahan(),
						pertemuan.getStatusPertemuan().getNama());

			}

		});

		// === SATU "button group" ringkas & ter-tengah ===
		// Sebelumnya tombol Kehadiran/Absen (bb) & "Daftar Hadir Peserta" (button) dirender lewat
		// layout dashboard generik (createKeteranganData) sehingga pada form penjadwalan ujian ini
		// tampil TERPISAH SANGAT JAUH di ujung kiri & kanan baris. Di sini keduanya cukup dikumpulkan
		// dalam SATU wadah flex yang dirapatkan (gap kecil) dan di-tengah-kan (justify-content:center),
		// sehingga membentuk satu kelompok tombol yang rapi dan tidak menggantung di tepi.
		org.zkoss.zul.Div grupTombolAksi = new org.zkoss.zul.Div();
		grupTombolAksi.setStyle("display:flex;flex-wrap:wrap;gap:8px;justify-content:center;"
				+ "align-items:center;width:100%;box-sizing:border-box;padding:4px 0;");
		grupTombolAksi.setParent(aksiCell);

		if (bb != null) {
			bb.setParent(grupTombolAksi);
		}
		button.setParent(grupTombolAksi);

	}

	protected Groupbox generatePertemuan(final Pertemuan pertemuan) throws Exception {
		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setStyle("min-height: 200px;");
		reloadPertemuan(pertemuan, groupbox);
		return groupbox;
	}

	class PerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Perkuliahan perkuliahan = (Perkuliahan) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener(Events.ON_OPEN, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (!detail.isOpen())
						return;

					Vbox vbox = new Vbox();
					vbox.setHeight("100%");
					vbox.setWidth("100%");
					vbox.setParent(detail);
					List<Pertemuan> utsPertemuans = perkuliahan.ambilPertemuanList();
					if (utsPertemuans.isEmpty()) {
						MyMessageboxConfig.show("Agenda ujian belum dibuat", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}
					for (Pertemuan pertemuan : utsPertemuans) {
						if (pertemuan.getStatusPertemuan() != null && pertemuan.getTanggal() != null
								&& pertemuan.getStatusPertemuan().getUjian()) {
							vbox.appendChild(generatePertemuan(pertemuan));
						}
					}
					utsPertemuans = null;
				}
			});

			ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(arg0, perkuliahan);

			new Label(perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama()).setParent(arg0);
			ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(arg0, perkuliahan, true);
			new Label(perkuliahan.getSemester()
					+ (perkuliahan.getKelas() == null || perkuliahan.getKelas().equals("") ? ""
							: " " + perkuliahan.getKelas())
					+ " (" + Common.labelJenisSemester(perkuliahan) + ")")
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			boolean adaUts = false;
			boolean adaUas = false;
			List<Pertemuan> utsPertemuans = perkuliahan.ambilPertemuanList();
			for (final Pertemuan pertemuan : utsPertemuans) {

				if (ConstantValues.UTS != null && pertemuan.getStatusPertemuan() != null
						&& pertemuan.getStatusPertemuan().getId().equals(ConstantValues.UTS.getId())) {
					adaUts = true;
				}

				if (ConstantValues.UAS != null && pertemuan.getStatusPertemuan() != null
						&& pertemuan.getStatusPertemuan().getId().equals(ConstantValues.UAS.getId())) {
					adaUas = true;
				}

				if (pertemuan.getStatusPertemuan() != null && pertemuan.getTanggal() != null
						&& pertemuan.getStatusPertemuan().getUjian()) {

					if (edit) {
						Hbox hbox = new Hbox();
						hbox.setParent(vbox);

						new Label(pertemuan.getStatusPertemuan().getNama()).setParent(hbox);
						final MyDatebox tanggal = new MyDatebox(pertemuan.getTanggal());
						tanggal.setParent(hbox);
						tanggal.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();
								session.refresh(pertemuan);
								pertemuan.setTanggal(tanggal.getValue());
								pertemuan.setTanggalEdit(tanggal.getValue());
								pertemuan.setMulai(tanggal.getValue());

								session.update(pertemuan);
								session.flush();
							}
						});
					} else {

						new Label(pertemuan.getStatusPertemuan().getNama() + ": "
								+ (pertemuan == null || pertemuan.getTanggal() == null ? ""
										: Common.dateFormat4.get().format(pertemuan.getTanggal())))
								.setParent(vbox);
					}

				}
			}
			utsPertemuans = null;
			if (!adaUts && perkuliahan != null && perkuliahan.getMatakuliah() != null
					&& perkuliahan.getMatakuliah().getTerdapatUts()) {
				if (edit) {
					MyToolbarbuttonConfig button = PenjadwalanHelper.buatSatuPertemuan(perkuliahan, tbmuser,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									perkuliahan.belum();
									try {
										onSearchDefault(arg0);
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}
								}
							}, ConstantValues.UTS);
					button.setLabel("Tambah Jadwal UTS");
					button.setParent(vbox);
				}
			}

			if (!adaUas && perkuliahan != null && perkuliahan.getMatakuliah() != null
					&& perkuliahan.getMatakuliah().getTerdapatUas()) {
				if (edit) {
					MyToolbarbuttonConfig button = PenjadwalanHelper.buatSatuPertemuan(perkuliahan, tbmuser,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									perkuliahan.belum();
									try {
										onSearchDefault(arg0);
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}
								}
							}, ConstantValues.UAS);
					button.setLabel("Tambah Jadwal UAS");
					button.setParent(vbox);
				}
			}

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Agenda", "/img/jadwal.png");
			button.setTooltiptext("Ubah Agenda");

			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					penjadwalanHelper.display(perkuliahan, new DataLoader() {

						@Override
						public void loadData(Object value) {
							perkuliahan.belum();
							try {
								onSearchDefault(null);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}
					});
				}

			});

			button.setParent(vbox);

		}
	}

	public boolean onSave(Event event) throws Exception {

		return true;

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criterion criterionMhs = Restrictions.sqlRestriction("true");
		if (!searchnim.getValue().trim().isEmpty() || !searchnamamhs.getValue().trim().isEmpty()) {
			String sql = "this_.id in (select perkuliahan from detailperkuliahan a inner join mahasiswa b on (a.mahasiswa = b.id) where perkuliahan is not null and b.nama ilike '%"
					+ searchnamamhs.getValue().trim() + "%' and b.nim ilike '%" + searchnim.getValue().trim()
					+ "%' group by perkuliahan)";
			criterionMhs = Restrictions.sqlRestriction(sql);
		}

		if ((searchasisten != null && searchasisten.getAttribute("mahasiswa") != null)
				|| !searchnamaasisten.getValue().trim().isEmpty()) {
			Mahasiswa mahasiswa = (Mahasiswa) searchasisten.getAttribute("mahasiswa");
			String sql = "this_.id in (select perkuliahan from mahasiswa_jadi_asisten a inner join mahasiswa b on (a.mahasiswa=b.id) where perkuliahan is not null and a.aktif=true "
					+ (mahasiswa == null ? "" : "and a.mahasiswa=" + mahasiswa.getId())
					+ (searchnamaasisten.getValue().trim().isEmpty() ? ""
							: "and (b.nama ilike '%" + searchnamaasisten.getValue().trim() + "%' or b.nim ilike '%"
									+ searchnamaasisten.getValue().trim() + "%')")
					+ " group by perkuliahan)";
			criterionMhs = Restrictions.and(criterionMhs, Restrictions.sqlRestriction(sql));
		}

		Criteria criteria = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(searchKeterangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchKeterangan.getValue().trim(), MatchMode.ANYWHERE));

		if (ekstrakurikuler != null && ekstrakurikuler.equals(Perkuliahan.EKSTRA)) {
			criteria.createAlias("matakuliah", "matakuliah").add(Restrictions.eq("matakuliah.extraKulikuler", true));
		} else {
			criteria.createAlias("matakuliah", "matakuliah")
					.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
							Restrictions.eq("matakuliah.extraKulikuler", false)));
		}

		if (ConstantValues.aktifkanTahapanKurikulum) {
			criteria.createAlias("kurikulumPunyaMatakuliah", "kurikulumPunyaMatakuliah", Criteria.LEFT_JOIN)
					.add((searchTahap != null && searchTahap.getSelectedItem() != null
							&& searchTahap.getSelectedItem().getValue() != null
							&& searchTahap.getSelectedItem().getValue().equals(-1))
									? Restrictions.sqlRestriction("true")
									: (searchTahap != null && searchTahap.getSelectedItem() != null
											&& searchTahap.getSelectedItem().getValue() != null
													? Restrictions.eq("kurikulumPunyaMatakuliah.tahap",
															searchTahap.getSelectedItem().getValue())
													: Restrictions.or(Restrictions.isNull("kurikulumPunyaMatakuliah"),
															Restrictions.isNull("kurikulumPunyaMatakuliah.tahap"))));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		Criterion criterion = searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("dosen1", searchdosen.getAttribute("myValue")),
						Restrictions.eq("dosen2", searchdosen.getAttribute("myValue")));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", searchdosen.getAttribute("myValue")));

		Criterion criterionNamaDosn = Restrictions.sqlRestriction("1=1");
		if (!searchnamadsn.getValue().trim().isEmpty()) {
			criteria.createAlias("dosen1", "dosen1", Criteria.LEFT_JOIN)
					.createAlias("dosen2", "dosen2", Criteria.LEFT_JOIN)
					.createAlias("dosen3", "dosen3", Criteria.LEFT_JOIN)
					.createAlias("dosen4", "dosen4", Criteria.LEFT_JOIN)
					.createAlias("dosen5", "dosen5", Criteria.LEFT_JOIN)
					.createAlias("dosen6", "dosen6", Criteria.LEFT_JOIN)
					.createAlias("dosen7", "dosen7", Criteria.LEFT_JOIN)
					.createAlias("dosen8", "dosen8", Criteria.LEFT_JOIN)
					.createAlias("dosen9", "dosen9", Criteria.LEFT_JOIN)
					.createAlias("dosen10", "dosen10", Criteria.LEFT_JOIN);

			criterionNamaDosn = Restrictions.ilike("dosen1.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE);

			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen2.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen3.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen4.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen5.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen6.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen7.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen8.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen9.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen10.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
		}

		criteria

				.add(searchnamamk.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("matakuliah.kode", searchnamamk.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("matakuliah.nama", searchnamamk.getValue().trim(),
										MatchMode.ANYWHERE)))

				.add(criterionNamaDosn).add(criterionMhs)

				.add((searchkurikulum == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkurikulum.getAttribute("kurikulum") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kurikulum", searchkurikulum.getAttribute("kurikulum"))))

				.add((searchkelas == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkelas.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kelas", searchkelas.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchparalel.isChecked() ? Restrictions.or(Restrictions.sqlRestriction(
						"this_.id in (select perkuliahan_paralel from perkuliahan where perkuliahan_paralel is not null)"),
						Restrictions.eq("merupakan_paralel", true)) : Restrictions.sqlRestriction("1=1"))

				.add(searchtanpakelas.isChecked()
						? Restrictions.or(Restrictions.eq("kelas", ""), Restrictions.isNull("kelas"))
						: Restrictions.sqlRestriction("1=1"))

				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))

				.add(criterion)

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add((searchmasaperkulaiahan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmasaperkulaiahan.getAttribute("masaPerkuliahan") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("masaPerkuliahan", searchmasaperkulaiahan.getAttribute("masaPerkuliahan"))))

				.add((searchmatakuliah == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmatakuliah.getAttribute("matakuliah") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("matakuliah", searchmatakuliah.getAttribute("matakuliah"))))

				.add(searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("hari", searchhari.getSelectedItem().getValue()))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))

				// Ganjil/Genap: TIDAK dibatasi bila "Semua" (null) ATAU "Semester Pendek (SP)" dipilih
				// (SP lintas ganjil/genap — filternya cukup lewat statusSemesterPendek di bawah).
				.add(searchJenisSemester.getSelectedItem() == null
						|| searchJenisSemester.getSelectedItem().getValue() == null
						|| Perkuliahan.SP.equals(searchJenisSemester.getSelectedItem().getValue())
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("ganjilGenap", searchJenisSemester.getSelectedItem().getValue()))

				.add(searchsemester.getSelectedItem() == null || searchsemester.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("semester", searchsemester.getSelectedItem().getValue()))

				// statusSemesterPendek: bila filter "Jenis Smt = Semester Pendek (SP)" dipilih, tampilkan
				// HANYA perkuliahan SP (statusSemesterPendek = SEMESTER_PENDEK) — persis tab "SP" lama.
				// Selain itu pakai perilaku lama (field semesterPendek: null → non-SP, else → SP).
				.add((searchJenisSemester.getSelectedItem() != null
						&& Perkuliahan.SP.equals(searchJenisSemester.getSelectedItem().getValue()))
								? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
								: semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
										: Restrictions.eq("statusSemesterPendek", semesterPendek))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
		return criteria;
	}

	private North mynorth;
	private Combobox ta;
	private Combobox smt;
	private Combobox hari;
	private Textbox keyword;

	private List<Perkuliahan> perkuliahans;
	protected Boolean merupakanRemedial = false;
	protected Boolean merupakanPraPerkuliahan = false;

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (mynorth != null && tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			if (event != null && event.getName().equalsIgnoreCase("cari")) {
				try {
					Session session = HibernateUtil.currentNativeSession();
					dosen.reInitPerkuliahan(session);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				HibernateUtil.closeSession();
			}
			String tahunAkademik = (String) (ta.getSelectedItem() == null || ta.getSelectedItem().getValue() == null
					? null
					: ta.getSelectedItem().getValue());
			String jenisSemester = smt.getSelectedItem() == null || smt.getSelectedItem().getValue() == null ? null
					: smt.getSelectedItem().getValue().toString();

			String hr = hari.getSelectedItem() == null || hari.getSelectedItem().getValue() == null ? null
					: hari.getSelectedItem().getValue().toString();
			int jumlahDataDalamSatuHalamanElearning = 10;
			Session session = HibernateUtil.currentSession();
			Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hr,
					keyword.getValue().trim(), "", merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial,
					false, true, true, true, true, true, true, true, true, true, TampilanELearningAction.PERKULIAHAN,
					jumlahDataDalamSatuHalamanElearning * (paging == null ? 0 : paging.getActivePage()),
					jumlahDataDalamSatuHalamanElearning);
			perkuliahans = (List<Perkuliahan>) objects[0];
			Integer size = (Integer) objects[1];
			paging.setPageSize(jumlahDataDalamSatuHalamanElearning);
			paging.setMold("os");
			paging.setTotalSize(size);
			paging.setVisible(size > jumlahDataDalamSatuHalamanElearning);
			try {
				((South) paging.getParent()).setHeight(paging.isVisible() ? "30px" : "0px");
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		} else {

			Common.initPaging(initCriteria(false), paging);

			perkuliahans = ConstantValues.simpleList(
					initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
					Perkuliahan.class);

		}
		ListModel strset = new SimpleListModel(perkuliahans);
		grid.setRowRenderer(new PerkuliahanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
