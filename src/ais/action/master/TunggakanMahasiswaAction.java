package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.TunggakanMahasiswaBaruProcessor;
import ais.action.master.helper.util.TunggakanMahasiswaDaftarUlangProcessor;
import ais.action.report.format1.keuangan.LaporanRekapitulasiTunggakanMahasiswa;
import ais.action.report.format1.keuangan.LaporanRekapitulasiTunggakanMahasiswaDetail;
import ais.action.ws.util.CommonUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKegiatan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Konsentrasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.TunggakanMahasiswa;
import ais.database.model.TunggakanMahasiswaDetail;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TunggakanMahasiswaAction extends GenericAutowireComposer implements DataLoader {

	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchnim;
	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Decimalbox searchtahun;
	private Combobox searchkonsentrasi;
	private Combobox searchstatus;
	private Combobox searchprogram;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchjenjang;
	private Combobox kewarganegaraan;
	private MyCheckboxConfig searchdosenPA;
	private AmbilDataDosenBanbox searchdosen;

	private Tabpanel rekapTunggakan;
	private Tabpanel rekapTunggakanDetail;
	private Tabpanel prosesUlangTunggakan;
	private Tabpanel prosesUlangTunggakanMahasiswaBaru;

	private TunggakanMahasiswaDaftarUlangProcessor tunggakanMahasiswaDaftarUlangProcessor;
	private TunggakanMahasiswaBaruProcessor tunggakanMahasiswaBaruProcessor;

	private boolean edit = false;
	// private boolean delete = false;

	public void onTampilRekapTunggakanMahasiswa(Event event) {
		if (rekapTunggakan.getChildren().size() == 0) {
			LaporanRekapitulasiTunggakanMahasiswa laporanRekapitulasiTunggakanMahasiswa = new LaporanRekapitulasiTunggakanMahasiswa();
			laporanRekapitulasiTunggakanMahasiswa.setHeight("100%");
			laporanRekapitulasiTunggakanMahasiswa.setWidth("100%");
			laporanRekapitulasiTunggakanMahasiswa.setParent(rekapTunggakan);
		}
	}

	public void onTampilRekapTunggakanMahasiswaDetail(Event event) {
		if (rekapTunggakanDetail.getChildren().size() == 0) {
			LaporanRekapitulasiTunggakanMahasiswaDetail laporanRekapitulasiTunggakanMahasiswaDetail = new LaporanRekapitulasiTunggakanMahasiswaDetail();
			laporanRekapitulasiTunggakanMahasiswaDetail.setHeight("100%");
			laporanRekapitulasiTunggakanMahasiswaDetail.setWidth("100%");
			laporanRekapitulasiTunggakanMahasiswaDetail.setParent(rekapTunggakanDetail);
		}
	}

	@SuppressWarnings("deprecation")
	public void onProsesUlangTunggakan(Event event) {
		if (prosesUlangTunggakan.getChildren().size() == 0) {
			MyWindow window = new MyWindow();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(prosesUlangTunggakan);

			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			groupbox.setParent(window);
			groupbox.setHeight("100%");
			groupbox.setWidth("95%");
			// groupbox.appendChild(new MyCaptionStyled("Proses Ulang "));

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(groupbox);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("");
			column.setParent(columns);
			column.setWidth("25%");

			column = new MyColumnConfig("");
			column.setParent(columns);

			final Combobox tahunAkademik = Common.generateTahunAjaran(null);
			final MyCheckboxConfig besihkanDulu = new MyCheckboxConfig();
			final MyCheckboxConfig besihkanDuluDetail = new MyCheckboxConfig();
			final Combobox jenisSemester = new Combobox();
			final Label proses = new Label();
			final MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(
					"Proses Ulang Tagihan/Tunggakan Mahasiswa", "/img/process-accept-icon-kecil.png");
			final Progressmeter progressmeter = new Progressmeter();

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();row.setValign("top");
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
			row.appendChild(tahunAkademik);
			tahunAkademik.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
			row.appendChild(jenisSemester);
			jenisSemester.setWidth("90%");
			jenisSemester.setWidth("90%");

			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Bersihkan Dulu"));
			row.appendChild(besihkanDulu);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Reload Detail Tunggakan"));
			row.appendChild(besihkanDuluDetail);

			MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			jenisSemester.appendChild(comboitem);
			comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			jenisSemester.appendChild(comboitem);

			Boolean ganjil = CommonUtil.isNowSemensterGanjil();
			Common.selectComboItem(jenisSemester, ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Proses"));
			row.appendChild(proses);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(progressmeter);
			progressmeter.setWidth("90%");

			final Timer timer = new Timer(500);
			timer.setRepeats(true);
			timer.setParent(window);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {
						Integer jmlmhs = TunggakanMahasiswaDaftarUlangProcessor.mahasiswas.size();
						Integer jmlmhsTerproses = TunggakanMahasiswaDaftarUlangProcessor.mahasiswasSudah.size();

						tahunAkademik.setDisabled(!jmlmhs.equals(0) && !jmlmhs.equals(jmlmhsTerproses));
						jenisSemester.setDisabled(!jmlmhs.equals(0) && !jmlmhs.equals(jmlmhsTerproses));
						toolbarbutton.setDisabled(!jmlmhs.equals(0) && !jmlmhs.equals(jmlmhsTerproses));

						proses.setValue(
								"Memproses " + jmlmhsTerproses + " dari " + jmlmhs + " data ("
										+ (Common.numberFormat.get()
												.format(jmlmhsTerproses.doubleValue() * 100.0 / jmlmhs.doubleValue()))
										+ " %)");

						if (jmlmhs.equals(0)) {
							proses.setValue("0 %");
						}

						progressmeter.setValue(jmlmhs < 1 ? 0 : jmlmhsTerproses * 100 / jmlmhs);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e); 
					}
				}

			});

			timer.start();

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			toolbarbutton.setParent(row);

			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show(
							"Apakah yakin ingin melakukan Proses Ulang Tagihan/Tunggakan Mahasiswa ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										String ta = (String) (tahunAkademik.getSelectedItem() == null
												|| tahunAkademik.getSelectedItem().getValue() == null ? ""
														: tahunAkademik.getSelectedItem().getValue());
										String js = (String) (jenisSemester.getSelectedItem() == null ? ""
												: jenisSemester.getSelectedItem().getValue());

										tunggakanMahasiswaDaftarUlangProcessor = new TunggakanMahasiswaDaftarUlangProcessor(
												besihkanDulu.isChecked(), besihkanDuluDetail.isChecked(), true, ta, js);

										Thread thread = new Thread(tunggakanMahasiswaDaftarUlangProcessor);
										thread.start();

										tahunAkademik.setDisabled(true);
										jenisSemester.setDisabled(true);
										toolbarbutton.setDisabled(true);

									}

								}
							});

				}
			});
		}
	}

	@SuppressWarnings("deprecation")
	public void onProsesUlangTunggakanMahasiswaBaru(Event event) {
		if (prosesUlangTunggakanMahasiswaBaru.getChildren().size() == 0) {
			MyWindow window = new MyWindow();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(prosesUlangTunggakanMahasiswaBaru);

			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			groupbox.setParent(window);
			groupbox.setHeight("100%");
			groupbox.setWidth("95%");
			// groupbox.appendChild(new MyCaptionStyled("Proses Ulang "));

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(groupbox);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("");
			column.setParent(columns);
			column.setWidth("25%");

			column = new MyColumnConfig("");
			column.setParent(columns);

			final Combobox tahunAkademik = Common.generateTahunAjaran(null);
			final MyCheckboxConfig besihkanDulu = new MyCheckboxConfig();
			final MyCheckboxConfig besihkanDuluDetail = new MyCheckboxConfig();
			final Label proses = new Label();
			final MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(
					"Proses Ulang Tagihan/Tunggakan Mahasiswa", "/img/process-accept-icon-kecil.png");
			final Progressmeter progressmeter = new Progressmeter();

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();row.setValign("top");
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
			row.appendChild(tahunAkademik);
			tahunAkademik.setWidth("90%");

			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Bersihkan Dulu"));
			row.appendChild(besihkanDulu);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Reload Detail Tunggakan"));
			row.appendChild(besihkanDuluDetail);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Proses"));
			row.appendChild(proses);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(progressmeter);
			progressmeter.setWidth("90%");

			final Timer timer = new Timer(500);
			timer.setRepeats(true);
			timer.setParent(window);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {
						Integer jmlmhs = TunggakanMahasiswaBaruProcessor.mahasiswas.size();
						Integer jmlmhsTerproses = TunggakanMahasiswaBaruProcessor.mahasiswasSudah.size();

						tahunAkademik.setDisabled(!jmlmhs.equals(0) && !jmlmhs.equals(jmlmhsTerproses));
						toolbarbutton.setDisabled(!jmlmhs.equals(0) && !jmlmhs.equals(jmlmhsTerproses));

						proses.setValue(
								"Memproses " + jmlmhsTerproses + " dari " + jmlmhs + " data ("
										+ (Common.numberFormat.get()
												.format(jmlmhsTerproses.doubleValue() * 100.0 / jmlmhs.doubleValue()))
										+ " %)");

						if (jmlmhs.equals(0)) {
							proses.setValue("0 %");
						}

						progressmeter.setValue(jmlmhs < 1 ? 0 : jmlmhsTerproses * 100 / jmlmhs);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e); 
					}
				}

			});

			timer.start();

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			toolbarbutton.setParent(row);

			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show(
							"Apakah yakin ingin melakukan Proses Ulang Tagihan/Tunggakan Mahasiswa Baru ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										String ta = (String) (tahunAkademik.getSelectedItem() == null
												|| tahunAkademik.getSelectedItem().getValue() == null ? ""
														: tahunAkademik.getSelectedItem().getValue());

										tunggakanMahasiswaBaruProcessor = new TunggakanMahasiswaBaruProcessor(
												besihkanDulu.isChecked(), besihkanDuluDetail.isChecked(), true, ta);

										Thread thread = new Thread(tunggakanMahasiswaBaruProcessor);
										thread.start();

										tahunAkademik.setDisabled(true);
										toolbarbutton.setDisabled(true);

									}

								}
							});

				}
			});
		}
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub

		super.doAfterCompose(comp);
		Common.initLaguage();

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPrograms(searchprogram);
		Common.insertCombo(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		Common.insertCombo(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		kewarganegaraan = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(ais.database.model.Mahasiswa.WNI); }
		if (comboitem != null) { comboitem.setValue(ais.database.model.Mahasiswa.WNI); }
		kewarganegaraan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(ais.database.model.Mahasiswa.WNA); }
		if (comboitem != null) { comboitem.setValue(ais.database.model.Mahasiswa.WNA); }
		kewarganegaraan.appendChild(comboitem);

		class SearchJurusanEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchkonsentrasi);
				searchkonsentrasi.setSelectedItem(null);
				if (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchkonsentrasi, "nama", Konsentrasi.class,
						CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false));
			}

		}
		searchjurusan.addEventListener("onChange", new SearchJurusanEventListener());

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			Common.clear(searchjurusan);

			Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			searchfakultas.setDisabled(true);
		} else {
			searchfakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			Common.clear(searchkonsentrasi);

			Common.insertCombo(searchkonsentrasi, "nama", Konsentrasi.class,
					Restrictions.eq("jurusan", tbmuser.ambilJurusan()));
			searchjurusan.setDisabled(true);
		} else {
			searchjurusan.setDisabled(false);
		}

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
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
	}

	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final TunggakanMahasiswa tunggakanMahasiswa = (TunggakanMahasiswa) arg1;
			final Mahasiswa mahasiswa = tunggakanMahasiswa.getMahasiswa();

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						Session session = HibernateUtil.currentSession();
						List<TunggakanMahasiswaDetail> tunggakanMahasiswaDetails = session
								.createCriteria(TunggakanMahasiswaDetail.class)
								.add(Restrictions.eq("tunggakanMahasiswa", tunggakanMahasiswa)).list();

						List<DetailKegiatan> detailKegiatans = tunggakanMahasiswa.getKegiatan() == null
								? new ArrayList<DetailKegiatan>()
								: session.createCriteria(DetailKegiatan.class)
										.add(Restrictions.eq("kegiatan", tunggakanMahasiswa.getKegiatan())).list();

						Double totalBiaya = 0.0;
						for (TunggakanMahasiswaDetail detailBiaya : tunggakanMahasiswaDetails) {
							totalBiaya += detailBiaya.getNilaiBiaya() == null ? 0.0 : detailBiaya.getNilaiBiaya();
						}
						// tunggakanMahasiswa.setJumlahTunggakan(totalBiaya);
						// Common.refreshUpdate(session, (tunggakanMahasiswa));

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("100%");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						final MyTabConfig tabSoal = new MyTabConfig("Informasi Detail Biaya Tagihan");
						tabSoal.setParent(tabs);

						MyTabConfig tabJawaban = new MyTabConfig("Informasi Detail Biaya Terbayar");
						tabJawaban.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setParent(tabpanels);

						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						groupbox.setParent(tabpanelUtama);

						MyGrid grid = new MyGrid();
						grid.setWidth("100%");
						grid.setParent(groupbox);
						grid.setWidth("100%");
						grid.setHeight("100%");

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig("Item Biaya");
						column.setParent(columns);

						column = new MyColumnConfig("Jumlah");
						column.setAlign("right");
						column.setParent(columns);

						Rows rows = new Rows();
						rows.setParent(grid);

						for (TunggakanMahasiswaDetail detailBiaya : tunggakanMahasiswaDetails) {
							MyFormRow row = new MyFormRow();row.setValign("top");
							row.setParent(rows);

							row.appendChild(new ais.ui.util.MyLabelConfig(
									detailBiaya.getItemBiaya() == null ? "" : detailBiaya.getItemBiaya().getNama()));
							row.appendChild(new ais.ui.util.MyLabelConfig(detailBiaya.getNilaiBiaya() == null ? ""
									: Common.numberFormat.get().format(detailBiaya.getNilaiBiaya())));
						}

						tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setParent(tabpanels);

						groupbox = new MyDiv();
						groupbox.setStyle("min-height: 200px;");
						groupbox.setParent(tabpanelUtama);

						grid = new MyGrid();// grid.setOddRowSclass("non-odd");
						grid.setWidth("100%");
						grid.setParent(groupbox);
						grid.setWidth("100%");
						grid.setHeight("100%");

						columns = new Columns();
						columns.setParent(grid);

						column = new MyColumnConfig("Item Biaya");
						column.setParent(columns);

						column = new MyColumnConfig("Jumlah");
						column.setAlign("right");
						column.setParent(columns);

						rows = new Rows();
						rows.setParent(grid);

						for (DetailKegiatan detailBiaya : detailKegiatans) {
							MyFormRow row = new MyFormRow();row.setValign("top");
							row.setParent(rows);

							row.appendChild(new ais.ui.util.MyLabelConfig(detailBiaya.getDetailBiaya() == null
									|| detailBiaya.getDetailBiaya().getItemBiaya() == null ? ""
											: detailBiaya.getDetailBiaya().getItemBiaya().getNama()));
							row.appendChild(new ais.ui.util.MyLabelConfig(detailBiaya.getBiaya() == null ? ""
									: Common.numberFormat.get().format(detailBiaya.getBiaya())));
						}

					}

				}
			});

			RevisiHelper.createNewRevisi(TunggakanMahasiswa.class, tunggakanMahasiswa, mahasiswa.getNim())
					.setParent(arg0);

			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);
			new Label(mahasiswa.getWarganegara() == null ? "" : mahasiswa.getWarganegara()).setParent(arg0);
			new Label(mahasiswa.getSemesterMulai() == null ? "" : mahasiswa.getSemesterMulai()).setParent(arg0);

			new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getNama()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);

			new Label(tunggakanMahasiswa.getTahunAkademik()).setParent(arg0);
			new Label(tunggakanMahasiswa.getSemester() + "").setParent(arg0);
			new Label(tunggakanMahasiswa.getJumlahTunggakan() == null ? ""
					: Common.numberFormat.get().format(tunggakanMahasiswa.getJumlahTunggakan())).setParent(arg0);

			new Label(tunggakanMahasiswa.getKegiatan() == null
					|| tunggakanMahasiswa.getKegiatan().getAmountTerhutang() == null
							? "Belum bayar"
							: Common.numberFormat.get().format(tunggakanMahasiswa.getKegiatan().getAmountTerhutang()) + " ("
									+ Common.numberFormat.get().format(tunggakanMahasiswa.getKegiatan().getPersentaseLunas())
									+ "%)").setParent(arg0);

			if (tunggakanMahasiswa.getKegiatan() != null) {
				final MyCheckboxConfig dianggapLunas = new MyCheckboxConfig("Lunas");
				dianggapLunas.setParent(arg0);
				dianggapLunas.setDisabled(!edit);
				dianggapLunas.setChecked(
						tunggakanMahasiswa.getDianggapLunas() != null && tunggakanMahasiswa.getDianggapLunas());
				dianggapLunas.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						tunggakanMahasiswa.setDianggapLunas(dianggapLunas.isChecked());
						Session session = HibernateUtil.currentSession();
						session.update(tunggakanMahasiswa);
					}
				});
			} else {
				new Label(ais.common.Common.getBahasaConfig("Belum bayar")).setParent(arg0);
			}

			new Label(tunggakanMahasiswa.getKegiatan() == null ? "0"
					: Common.numberFormat.get().format(tunggakanMahasiswa.getKegiatan().getAmount())).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			// button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
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

											Common.refreshDelete(tunggakanMahasiswa);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(arg0);

		}

	}

	public Criteria initCriteria(boolean order) {

		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (statusMahasiswa != null) {
			String sql = "this_.mahasiswa in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ statusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
					+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
			System.out.println("sql=>" + sql);
			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");
		Criteria criteria = session.createCriteria(TunggakanMahasiswa.class).add(criteriaStatus)
				.add(Restrictions.ge("jumlahTunggakan", 1.0)).createCriteria("mahasiswa")
				.add(dosen != null ? Restrictions.eq("dosen", dosen.getId()) : Restrictions.sqlRestriction("1=1"))
				.add(searchdosenPA.isChecked() ? Restrictions.isNull("dosen") : Restrictions.sqlRestriction("1=1"));
		if (order)
			criteria.addOrder(Order.desc("tahunangkatan"));
		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));
		criteria.add(searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(searchnim.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nim", searchnim.getValue(), MatchMode.ANYWHERE))
				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchStatusAwalMahasiswa.getSelectedItem() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("statusAwalMahasiswa",
										searchStatusAwalMahasiswa.getSelectedItem().getValue()))
				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", searchtahun.getValue().intValue()))

				.add(searchkonsentrasi.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("konsentrasi", searchkonsentrasi.getSelectedItem().getValue()))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TunggakanMahasiswa> tunggakanMahasiswas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(tunggakanMahasiswas);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public void loadData(Object value) {
		onSearchDefault(null);
	}

}
