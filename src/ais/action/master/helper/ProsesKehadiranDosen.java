package ais.action.master.helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.West;

import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KehadiranDosenBulanan;
import ais.database.model.Kelas;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.StatusPertemuan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class ProsesKehadiranDosen extends MyWindow {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox tahunAkademik;
	private Combobox genapGanjil;
	private Combobox fakultas;
	private Combobox jurusan;

	private Textbox matakuliah;
	private AmbilDataKelasBanbox kelas;
	private AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
	private AmbilDataDosenBanbox dosen;
	private MyCheckboxConfig semesterPendek;
	private MyCheckboxConfig ekstrakurikuler;

	private MyDatebox mulai;
	private MyDatebox sampai;

	private AmbilDataMatakuliahBanbox matkul;

	private Combobox tetap;

	private ArrayList<MyCheckboxConfig> listJenisPertemuan;

	public ProsesKehadiranDosen() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"menampilkan jendela proses kehadiran dosen",
					e, new String[] {
							"Muat ulang (refresh) halaman ini lalu coba buka jendela kembali.",
							"Periksa koneksi jaringan Anda ke server aplikasi.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public ProsesKehadiranDosen(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() {

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		genapGanjil = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		genapGanjil.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		genapGanjil.appendChild(comboitem);
		genapGanjil.setReadonly(true);

		Common.selectComboItem(genapGanjil, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		genapGanjil.setReadonly(true);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(genapGanjil);
		genapGanjil.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(this.kelas = new AmbilDataKelasBanbox());
		kelas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(matkul = new AmbilDataMatakuliahBanbox());
		matkul.setCols(6);
		hbox.appendChild(new Label(""));
		hbox.appendChild(this.matakuliah = new Textbox());
		matakuliah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Tetap/Bukan Tetap"));
		row.appendChild(tetap = new Combobox());
		tetap.setWidth("90%");
		tetap.setReadonly(true);

		Comboitem comboitem2 = new Comboitem("Semua");
		comboitem2.setValue(null);
		tetap.appendChild(comboitem2);

		comboitem2 = new Comboitem("Tetap");
		comboitem2.setValue(1);
		tetap.appendChild(comboitem2);

		comboitem2 = new Comboitem("Tidak Tetap");
		comboitem2.setValue(0);
		tetap.appendChild(comboitem2);

		tetap.setSelectedIndex(0);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(this.dosen = new AmbilDataDosenBanbox());
		dosen.setWidth("90%");

		int tanggalMulaiAbsensi = 1;
		try {
			tanggalMulaiAbsensi = Integer.parseInt(Common.getKonfigurasi("tanggal_mulai_absensi", "1").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesKehadiranDosen.java:233");
		}
		Calendar calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
		calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) - 1);
		calendarUtama.set(Calendar.DATE, tanggalMulaiAbsensi);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(mulai = new MyDatebox(calendarUtama.getTime()));
		mulai.setReadonly(true);

		calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) + 1);
		calendarUtama.set(Calendar.DATE, calendarUtama.get(Calendar.DATE) - 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(sampai = new MyDatebox(calendarUtama.getTime()));
		sampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan"));
		row.appendChild(masaPerkuliahan = new AmbilDataMasaPerkuliahanBanbox());
		masaPerkuliahan.setWidth("90%");
		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jurusan.getSelectedItem() != null) {
					masaPerkuliahan.setJurusanSelected((Jurusan) jurusan.getSelectedItem().getValue());
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.semesterPendek = new MyCheckboxConfig("Semester Pendek"));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.ekstrakurikuler = new MyCheckboxConfig("Ekstrakurikuler"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pertemuan"));
		row.appendChild(new ais.ui.util.MyLabelConfig());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				proses();
			}
		});
		print.setParent(row);

		listJenisPertemuan = new ArrayList<MyCheckboxConfig>();
		for (Object o : ConstantValues.ambilBerdasarClass(StatusPertemuan.class).values()) {
			StatusPertemuan statusPertemuan = (StatusPertemuan) o;
			if (statusPertemuan.getAktif()) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig());
				MyCheckboxConfig checkbox = new MyCheckboxConfig(statusPertemuan.getNama());
				checkbox.setChecked(true);
				checkbox.setAttribute("statusPertemuan", statusPertemuan);
				row.appendChild(checkbox);
				listJenisPertemuan.add(checkbox);
			}
		}

	}

	@SuppressWarnings("rawtypes")
	private Map<String, Map> dataHadir = new TreeMap<String, Map>();
	@SuppressWarnings("rawtypes")
	private Map<String, Map> dataHadirPerDosen = new TreeMap<String, Map>();

	private void proses() {

		Common.clear(center);
		dataHadir.clear();
		dataHadirPerDosen.clear();

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean hanyaYgStatusMasuk = true;

				Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null
						|| jurusan.getSelectedItem().getValue() == null ? null : jurusan.getSelectedItem().getValue());
				Fakultas myFakultas = (Fakultas) (fakultas.getSelectedItem() == null
						|| fakultas.getSelectedItem().getValue() == null ? null
								: fakultas.getSelectedItem().getValue());
				MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) ProsesKehadiranDosen.this.masaPerkuliahan
						.getAttribute("masaPerkuliahan");

				List<Long> statusPertemuans = new ArrayList<Long>();
				for (MyCheckboxConfig config : listJenisPertemuan) {
					if (config.isChecked()) {
						StatusPertemuan statusPertemuan = (StatusPertemuan) config.getAttribute("statusPertemuan");
						statusPertemuans.add(statusPertemuan.getId());
					}
				}

				List<Map<String, Serializable>> maps = CommonReportHelper.generateParameterMapAbsensiRinciDosen(
						myFakultas, myJurusan,
						kelas.getAttribute("kelas") == null ? null : ((Kelas) kelas.getAttribute("kelas")).getNama(),
						masaPerkuliahan, tahunAkademik.getSelectedItem().getValue().toString(),
						genapGanjil.getSelectedItem() == null || genapGanjil.getSelectedItem().getValue() == null ? null
								: genapGanjil.getSelectedItem().getValue().toString(),
						semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null,
						ekstrakurikuler.isChecked() ? Perkuliahan.EKSTRA : null, (Dosen) dosen.getAttribute("dosen"),
						matakuliah.getValue().trim(), (Matakuliah) matkul.getAttribute("matakuliah"), mulai.getValue(),
						sampai.getValue(), hanyaYgStatusMasuk, false, true,
						(Integer) tetap.getSelectedItem().getValue(), statusPertemuans);

				List<String> sudahAda = new ArrayList<String>();
				List<String> sudahAdaLagi = new ArrayList<String>();
				for (Map<String, Serializable> map : maps) {
					Long dosen_id = (Long) map.get("dosen_id");
					Long perkuliahan = (Long) map.get("perkuliahan_id");
					Integer sksdata = (Integer) map.get("sks");
					Pertemuan pertemuan = (Pertemuan) map.get("pertemuan");

					Integer jumlah_dosen = (Integer) map.get("jumlah_dosen");

					if (dosen_id == null && perkuliahan == null && sksdata == null && jumlah_dosen == null
							&& pertemuan == null) {
						continue;
					}

					Double sks = jumlah_dosen > 0 ? (sksdata.doubleValue() / jumlah_dosen.doubleValue())
							: sksdata.doubleValue();

					Perkuliahan perkuliahanData = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
							perkuliahan, true);

					Matakuliah matakuliah = perkuliahanData.getMatakuliah();

					Integer sksdatamk = (Integer) map.get("sks_" + matakuliah.getId());
					if (sksdatamk == null) {
						sksdatamk = 0;
					}

					String key = dosen_id + "_" + perkuliahan;

//					System.out.println("key -> " + key);

					Map d = dataHadir.get(key);
					if (d == null) {
						d = new HashMap();
						d.put("sks", 0.0);
						d.put("jumlah_hadir", 0);
						d.put("jumlah_hadir_hari", 0);
						d.put("perkuliahan", perkuliahan);
						d.put("jurusan", perkuliahanData.getJurusan().getId());
						d.put("dosen", dosen_id);
						dataHadir.put(key, d);
					}

					Integer jumlah_hadir = (Integer) d.get("jumlah_hadir");
					jumlah_hadir++;
					d.put("jumlah_hadir", jumlah_hadir);

					Integer jumlah_hadir_hari = (Integer) d.get("jumlah_hadir_hari");

					String keyTgl = dosen_id + "_" + Common.dateFormat1.get().format(pertemuan.getTanggal());

					if (!sudahAda.contains(keyTgl)) {
						jumlah_hadir_hari++;
						sudahAda.add(keyTgl);
					}
					d.put("jumlah_hadir_hari", jumlah_hadir_hari);

					Integer jumlah_mk = (Integer) d.get("jumlah_mk");
					if (jumlah_mk == null) {
						jumlah_mk = 0;
					}

					Double jumlah_sks_mk = (Double) d.get("jumlah_sks_mk");
					if (jumlah_sks_mk == null) {
						jumlah_sks_mk = 0.0;
					}
					jumlah_sks_mk += sksdatamk.doubleValue();

					String keyMk = dosen_id + "_mk_" + matakuliah.getId();

					if (!sudahAda.contains(keyMk)) {
						jumlah_mk++;
						sudahAda.add(keyMk);
					}
					d.put("jumlah_mk", jumlah_mk);
					d.put("jumlah_sks_mk", jumlah_sks_mk);

					Double sksTotal = (Double) d.get("sks");
					sksTotal += sks;
					d.put("sks", sksTotal);

					key = dosen_id + "_" + (perkuliahanData == null || perkuliahanData.getJurusan() == null ? ""
							: perkuliahanData.getJurusan().getId());

					d = dataHadirPerDosen.get(key);
					if (d == null) {
						d = new HashMap();
						d.put("sks", 0.0);
						d.put("jumlah_hadir", 0);
						d.put("jumlah_hadir_hari", 0);
						d.put("jurusan",
								perkuliahanData.getJurusan() == null ? -1L : perkuliahanData.getJurusan().getId());
						d.put("dosen", dosen_id);
						dataHadirPerDosen.put(key, d);
					}

					jumlah_hadir = (Integer) d.get("jumlah_hadir");
					jumlah_hadir++;
					d.put("jumlah_hadir", jumlah_hadir);
					d.put("jumlah_mk", jumlah_mk);
					d.put("jumlah_sks_mk", jumlah_sks_mk);
					jumlah_hadir_hari = (Integer) d.get("jumlah_hadir_hari");
					if (!sudahAdaLagi.contains(keyTgl)) {
						jumlah_hadir_hari++;
						sudahAdaLagi.add(keyTgl);
					}

					d.put("jumlah_hadir_hari", jumlah_hadir_hari);

					sksTotal = (Double) d.get("sks");
					sksTotal += sks;
					d.put("sks", sksTotal);
				}

				Tabbox tabbox = new Tabbox();
				tabbox.setParent(center);
				tabbox.setHeight("100%");
				tabbox.setWidth("100%");

				Tabs tabs = new Tabs();
				tabs.setParent(tabbox);

				final MyTabConfig tabSoal = new MyTabConfig("Daftar Kehadiran Per Perkuliahan");
				tabSoal.setParent(tabs);

				MyTabConfig tab1AsistenMahasiswa = new MyTabConfig();
				tab1AsistenMahasiswa.setParent(tabs);
				tab1AsistenMahasiswa.setLabel("Daftar Kehadiran Per Dosen");

				MyTabConfig tab1TotalSKS = new MyTabConfig();
				tab1TotalSKS.setParent(tabs);
				tab1TotalSKS.setLabel("Total SKS Per Dosen");

				Tabpanels tabpanels = new Tabpanels();
				tabpanels.setParent(tabbox);

				Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
				tabpanelUtama.setStyle("min-height: 2200px;");
				tabpanelUtama.setParent(tabpanels);

				Div div = new Div();
				div.setParent(tabpanelUtama);

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(div);

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(sampai.getValue());
				final int bln = calendar.get(Calendar.MONTH);
				final int tahun = calendar.get(Calendar.YEAR);

				final boolean sp = semesterPendek.isChecked();

				final String param = "DSN_PERK" + (sp ? "_SP" : "");

				final String param_hdr = "HDR_" + param;
				final String param_sks = "SKS_" + param;
				final String param_total_sks = "SKS_TOTAL_" + param;
				final String bulan = Common.BULAN[bln];

				MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh-cw.svg");
				print.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						proses();
					}
				});
				print.setParent(toolbar);

				Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("Dosen dan Prodi bulan " + bulan,
						"/img/svg/check-circled-outline.svg");
				toolbar.appendChild(toolbarbutton);
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
						parameters.put("bulan", bulan);
						parameters.put("tahun", tahun);
						List<Map> maps = new ArrayList<Map>();
						Session session = HibernateUtil.currentSession();
						for (Map map : dataHadirPerDosen.values()) {
							String key = param + "_" + bln + "_" + tahun;
							Dosen dosen = (Dosen) ConstantValues.ambil(Dosen.class.getName(),
									(Serializable) map.get("dosen"));
							Jurusan jurusan = (Jurusan) ConstantValues.ambil(Jurusan.class.getName(),
									(Serializable) map.get("jurusan"));
							Double sks = (Double) map.get("sks");
							Integer jumlah_hadir = (Integer) map.get("jumlah_hadir");
							Integer jumlah_hadir_hari = (Integer) map.get("jumlah_hadir_hari");
							Integer jumlah_mk = (Integer) map.get("jumlah_mk");
							Double jumlah_sks_mk = (Double) map.get("jumlah_sks_mk");

							session.refresh(dosen);
							JSONArray array = new JSONArray(dosen.getFormula());
							JSONObject jsonObject = null;
							for (int i = 0; i < array.length(); i++) {
								JSONObject temp = array.getJSONObject(i);
								String keyData = (temp.get("key") + "");
								if (keyData.equalsIgnoreCase(key)) {
									jsonObject = temp;
									break;
								}
							}

							if (jsonObject == null) {
								jsonObject = new JSONObject();
								array.put(jsonObject);
							}

							jsonObject.put("key", key);
							jsonObject.put("sks", sks);
							jsonObject.put("hdr", jumlah_hadir);
							jsonObject.put("hdr_hr", jumlah_hadir_hari);

							dosen.setFormula(array.toString());
							Common.refreshUpdate(session, dosen);
							session.flush();

							map.put("dosen_id", dosen.getId());
							map.put("dosen_nama", dosen.getNama());
							map.put("jurusan_id", jurusan.getId());
							map.put("jurusan_nama", jurusan.getNama());

							map.put("sks", sks);
							map.put("jumlah_hadir", jumlah_hadir);
							map.put("jumlah_hadir_hari", jumlah_hadir_hari);
							map.put("jumlah_mk", jumlah_mk);
							map.put("jumlah_sks_mk", jumlah_sks_mk);
							maps.add(map);
						}

						parameters.put("maps", maps);

						Report.generatePDFReport(Report.PDF, parameters, "Kehadiran_Dosen",
								ais.ui.util.WaktuUtil.getDate(), maps);
					}
				});

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(div);
				grid.setWidth("100%");
				grid.setHeight("100%");
				grid.setMold("paging");
				grid.setPageSize(20);
				grid.getPagingChild().setMold("os");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig("Dosen");
				column.setWidth("25%");
				column.setParent(columns);
				column = new MyColumnConfig("Prodi");
				column.setParent(columns);
				column.setWidth("20%");
				column = new MyColumnConfig("Perkuliahan");
				column.setParent(columns);
				column.setWidth("40%");

				column = new MyColumnConfig("SKS");
				column.setParent(columns);
				column.setAlign("right");

				column = new MyColumnConfig("Hdr");
				column.setParent(columns);
				column.setAlign("right");

				Rows rows = new Rows();
				rows.setParent(grid);

				for (Map map : dataHadir.values()) {

					Long perkuliahanId = (Long) map.get("perkuliahan");
					Double sks = (Double) map.get("sks");
					Integer jumlah_hadir = (Integer) map.get("jumlah_hadir");

					Dosen dosen = (Dosen) ConstantValues.ambil(Dosen.class.getName(), (Serializable) map.get("dosen"));
					Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
							perkuliahanId, true);

					System.out.println("perkuliahanId -> " + perkuliahanId + ", perkuliahan -> " + perkuliahan);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label(dosen.getNama()));
					row.appendChild(new Label(perkuliahan == null || perkuliahan.getJurusan() == null ? ""
							: perkuliahan.getJurusan().getNama()));
					row.appendChild(new Label(perkuliahan == null ? "" : perkuliahan.infoSimple()));
					row.appendChild(new Label(Common.numberFormat.get().format(sks)));
					row.appendChild(new Label(Common.numberFormat.get().format(jumlah_hadir)));
				}

				tabpanelUtama = new ais.ui.util.MyTabpanel();
				tabpanelUtama.setStyle("min-height: 2200px;");
				tabpanelUtama.setParent(tabpanels);

				div = new Div();
				div.setParent(tabpanelUtama);

				toolbar = new Toolbar();
				toolbar.setParent(div);

				print = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh-cw.svg");
				print.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						proses();
					}
				});
				print.setParent(toolbar);

				toolbarbutton = new MyToolbarbuttonConfig("Rekap Kehadiran Dosen bulan " + bulan
						+ ", Parameter Hadir = " + param_hdr + ", Parameter SKS = " + param_sks,
						"/img/svg/check-circled-outline.svg");
				toolbar.appendChild(toolbarbutton);
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
						parameters.put("bulan", bulan);
						parameters.put("tahun", tahun);
						parameters.put("mulai", mulai.getValue());
						parameters.put("sampai", sampai.getValue());
						List<Map> maps = new ArrayList<Map>();

						Map<Long, Map> mapsData = new TreeMap<Long, Map>();
						for (Map map : dataHadirPerDosen.values()) {
							Dosen dosen = (Dosen) ConstantValues.ambil(Dosen.class.getName(),
									(Serializable) map.get("dosen"));
							Jurusan jurusan = (Jurusan) ConstantValues.ambil(Jurusan.class.getName(),
									(Serializable) map.get("jurusan"));
							Double sks = (Double) map.get("sks");
							Integer jumlah_hadir = (Integer) map.get("jumlah_hadir");
							Integer jumlah_hadir_hari = (Integer) map.get("jumlah_hadir_hari");
							Integer jumlah_mk = (Integer) map.get("jumlah_mk");
							Double jumlah_sks_mk = (Double) map.get("jumlah_sks_mk");
							Map dataMap = mapsData.get(dosen.getId());
							if (dataMap == null) {
								dataMap = new HashMap();
								mapsData.put(dosen.getId(), dataMap);
								maps.add(dataMap);
							}
							dataMap.put("dosen", dosen);
							dataMap.put("dosen_id", dosen.getId());
							dataMap.put("dosen_nama", dosen.getNama());

							Integer jumlah_hadirJenjang = (Integer) dataMap
									.get("jumlah_hadir_" + jurusan.getJenjang().getNama());
							if (jumlah_hadirJenjang == null) {
								jumlah_hadirJenjang = 0;
							}

							jumlah_hadirJenjang += jumlah_hadir;

							dataMap.put("jumlah_hadir_" + jurusan.getJenjang().getNama(), jumlah_hadirJenjang);

							Double sksJenjang = (Double) dataMap.get("sks_" + jurusan.getJenjang().getNama());
							if (sksJenjang == null) {
								sksJenjang = 0.0;
							}

							sksJenjang += sks;

							dataMap.put("sks_" + jurusan.getJenjang().getNama(), sksJenjang);

							Double sks_semua = (Double) dataMap.get("sks_semua");
							if (sks_semua == null) {
								sks_semua = 0.0;
							}

							sks_semua += sks;

							dataMap.put("sks_semua", sks_semua);

							Integer jumlah_hadir_semua = (Integer) dataMap.get("jumlah_hadir_semua");
							if (jumlah_hadir_semua == null) {
								jumlah_hadir_semua = 0;
							}

							jumlah_hadir_semua += jumlah_hadir;

							dataMap.put("jumlah_hadir_semua", jumlah_hadir_semua);

							Integer jumlah_hadir_hariJenjang = (Integer) dataMap
									.get("jumlah_hadir_hari_" + jurusan.getJenjang().getNama());
							if (jumlah_hadir_hariJenjang == null) {
								jumlah_hadir_hariJenjang = 0;
							}

							jumlah_hadir_hariJenjang += jumlah_hadir_hari;

							dataMap.put("jumlah_hadir_hari_" + jurusan.getJenjang().getNama(),
									jumlah_hadir_hariJenjang);

							Integer jumlah_hadir_hari_semua = (Integer) dataMap.get("jumlah_hadir_hari_semua");
							if (jumlah_hadir_hari_semua == null) {
								jumlah_hadir_hari_semua = 0;
							}

							jumlah_hadir_hari_semua += jumlah_hadir_hari;

							dataMap.put("jumlah_hadir_hari_semua", jumlah_hadir_hari_semua);

							Session session = HibernateUtil.currentSession();
							KehadiranDosenBulanan kehadiranDosenBulanan = (KehadiranDosenBulanan) session
									.createCriteria(KehadiranDosenBulanan.class)
									.add(Restrictions.eq("bulan", (bln + 1)))
									.add(Restrictions.eq("dosen", dosen.getId())).add(Restrictions.eq("tahun", tahun))
									.setMaxResults(1).uniqueResult();
							if (kehadiranDosenBulanan == null) {
								kehadiranDosenBulanan = new KehadiranDosenBulanan();
							}

							int jml = 0;
							for (String ke : dataHadir.keySet()) {
								if (ke.startsWith(dosen.getId() + "_")) {
									jml++;
								}
							}
							kehadiranDosenBulanan.setSkspecahanmk(jumlah_sks_mk);

							kehadiranDosenBulanan.setJmlMk(jumlah_mk);
							kehadiranDosenBulanan.setBulan((bln + 1));
							kehadiranDosenBulanan.setTahun(tahun);
							kehadiranDosenBulanan.setDosen(dosen.getId());
							kehadiranDosenBulanan.setNama(bulan + " " + tahun);
							kehadiranDosenBulanan.setKeterangan("kehadiran bulan " + bulan + " " + tahun);

							kehadiranDosenBulanan.setJmlKelas(jml);
							kehadiranDosenBulanan.setSks(sks_semua.intValue());
							kehadiranDosenBulanan.setSkspecahan(sks_semua);
							kehadiranDosenBulanan.setHr(jumlah_hadir_hari_semua);
							kehadiranDosenBulanan.setMasuk(jumlah_hadir_semua);

							kehadiranDosenBulanan.setTanggalMulai(mulai.getValue());
							kehadiranDosenBulanan.setTanggalSampai(sampai.getValue());

							Common.refreshSaveOrUpdate(session, kehadiranDosenBulanan);
							session.flush();

						}
						parameters.put("maps", maps);
						Report.generatePDFReport(Report.PDF, parameters, "Kehadiran_Dosen_Semua",
								ais.ui.util.WaktuUtil.getDate(), maps);

					}
				});

				grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(div);
				grid.setWidth("100%");
				grid.setHeight("100%");
				grid.setMold("paging");
				grid.setPageSize(20);
				grid.getPagingChild().setMold("os");

				columns = new Columns();
				columns.setParent(grid);

				column = new MyColumnConfig("Prodi");
				column.setParent(columns);
				column.setWidth("20%");

				column = new MyColumnConfig("Dosen");
				column.setWidth("40%");
				column.setParent(columns);

				column = new MyColumnConfig("SKS");
				column.setParent(columns);
				column.setAlign("right");

				column = new MyColumnConfig("Hdr");
				column.setParent(columns);
				column.setAlign("right");

				rows = new Rows();
				rows.setParent(grid);

				for (Map map : dataHadirPerDosen.values()) {
					Double sks = (Double) map.get("sks");
					Integer jumlah_hadir = (Integer) map.get("jumlah_hadir");
					Dosen dosen = (Dosen) ConstantValues.ambil(Dosen.class.getName(), (Serializable) map.get("dosen"));
//					Integer jumlah_mk = (Integer) map.get("jumlah_mk");
					Jurusan jurusan = (Jurusan) ConstantValues.ambil(Jurusan.class.getName(),
							(Serializable) map.get("jurusan"));
//					Double jumlah_sks_mk = (Double) map.get("jumlah_sks_mk");

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label(jurusan == null ? "" : jurusan.getNama()));
					row.appendChild(new Label(dosen.getNama()));
					row.appendChild(new Label(Common.numberFormat.get().format(sks)));
					row.appendChild(new Label(Common.numberFormat.get().format(jumlah_hadir)));
				}

				final Tabpanel tabpanelTotalSKS = new ais.ui.util.MyTabpanel();
				tabpanelTotalSKS.setStyle("min-height: 2200px;");
				tabpanelTotalSKS.setParent(tabpanels);

				tab1TotalSKS.addEventListener("onClick", new EventListener() {

					private void reload() {

						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.setTime(sampai.getValue());
						final int bln = calendar.get(Calendar.MONTH);
						final int tahun = calendar.get(Calendar.YEAR);
						final String bulan = Common.BULAN[bln];

						Common.clear(tabpanelTotalSKS);

						Div div = new Div();
						div.setParent(tabpanelTotalSKS);

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(div);

						MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Refresh",
								"/img/svg/refresh-cw.svg");
						toolbar.appendChild(toolbarbutton);
						toolbarbutton.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								reload();
							}
						});

						toolbarbutton = new MyToolbarbuttonConfig(
								"Total SKS Dosen bulan " + bulan + ", Parameter Total SKS = " + param_total_sks,
								"/img/svg/check-circled-outline.svg");
						toolbar.appendChild(toolbarbutton);
						toolbarbutton.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
								parameters.put("bulan", bulan);
								parameters.put("tahun", tahun);
								parameters.put("mulai", mulai.getValue());
								parameters.put("sampai", sampai.getValue());
								List<Map> maps = new ArrayList<Map>();

								Session session = HibernateUtil.currentSession();

								List<Dosen> dosens = ConstantValues.simpleList(
										session.createCriteria(Dosen.class)
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.addOrder(Order.asc("nama")),
										Dosen.class);

								for (Dosen dosen : dosens) {

									Criterion criterion = dosen == null ? Restrictions.sqlRestriction("1=1")
											: Restrictions.or(Restrictions.eq("dosen1", dosen),
													Restrictions.eq("dosen2", dosen));

									criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
									criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
									criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
									criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
									criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
									criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
									criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
									criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

									Number totalsks = (Number) session.createCriteria(Perkuliahan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(!sp ? Restrictions.isNull("statusSemesterPendek")
													: Restrictions.eq("statusSemesterPendek",
															Perkuliahan.SEMESTER_PENDEK))

											.add(tahunAkademik.getSelectedItem() == null
													|| tahunAkademik.getSelectedItem().getValue() == null
															? Restrictions.sqlRestriction("1=1")
															: Restrictions.eq("tahunAjaran",
																	tahunAkademik.getSelectedItem().getValue()))

											.add(genapGanjil.getSelectedItem() == null
													|| genapGanjil.getSelectedItem().getValue() == null
															? Restrictions.sqlRestriction("1=1")
															: Restrictions.eq("ganjilGenap",
																	genapGanjil.getSelectedItem().getValue()))

											.add(criterion).createAlias("matakuliah", "matakuliah")
											.setProjection(Projections.sum("matakuliah.sks")).uniqueResult();

									Number totalmk = (Number) session.createCriteria(Perkuliahan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(!sp ? Restrictions.isNull("statusSemesterPendek")
													: Restrictions.eq("statusSemesterPendek",
															Perkuliahan.SEMESTER_PENDEK))

											.add(tahunAkademik.getSelectedItem() == null
													|| tahunAkademik.getSelectedItem().getValue() == null
															? Restrictions.sqlRestriction("1=1")
															: Restrictions.eq("tahunAjaran",
																	tahunAkademik.getSelectedItem().getValue()))

											.add(genapGanjil.getSelectedItem() == null
													|| genapGanjil.getSelectedItem().getValue() == null
															? Restrictions.sqlRestriction("1=1")
															: Restrictions.eq("ganjilGenap",
																	genapGanjil.getSelectedItem().getValue()))

											.add(criterion).createAlias("matakuliah", "matakuliah")
											.setProjection(Projections.countDistinct("matakuliah.id")).uniqueResult();

									Map map = new HashMap();
									map.put("nidn", dosen.getNidn());
									map.put("nuptk", dosen.getNuptk());

									map.put("nama", dosen.getNama());
									map.put("code", dosen.getCode());
									map.put("mycode", dosen.getMycode());
									map.put("jurusan", dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama());
									map.put("totalsks", totalsks == null ? 0 : totalsks.intValue());
									map.put("totalmk", totalmk == null ? 0 : totalmk.intValue());

									maps.add(map);

									KehadiranDosenBulanan kehadiranDosenBulanan = (KehadiranDosenBulanan) session
											.createCriteria(KehadiranDosenBulanan.class)
											.add(Restrictions.eq("bulan", (bln + 1)))
											.add(Restrictions.eq("dosen", dosen.getId()))
											.add(Restrictions.eq("tahun", tahun)).setMaxResults(1).uniqueResult();
									if (kehadiranDosenBulanan == null) {
										kehadiranDosenBulanan = new KehadiranDosenBulanan();
									}
									kehadiranDosenBulanan.setJmlMk(totalmk == null ? 0 : totalmk.intValue());
									kehadiranDosenBulanan.setBulan((bln + 1));
									kehadiranDosenBulanan.setTahun(tahun);
									kehadiranDosenBulanan.setDosen(dosen.getId());
									kehadiranDosenBulanan.setNama(bulan + " " + tahun);
									kehadiranDosenBulanan.setKeterangan("kehadiran bulan " + bulan + " " + tahun);

									kehadiranDosenBulanan.setSksTotal(totalsks == null ? 0 : totalsks.intValue());

									kehadiranDosenBulanan.setTanggalMulai(mulai.getValue());
									kehadiranDosenBulanan.setTanggalSampai(sampai.getValue());

									Common.refreshSaveOrUpdate(session, kehadiranDosenBulanan);
									session.flush();
								}

								parameters.put("maps", maps);
								Report.generatePDFReport(Report.PDF, parameters, "Total_SKS_Dosen",
										ais.ui.util.WaktuUtil.getDate(), maps);

							}
						});

						Session session = HibernateUtil.currentSession();

						List<Dosen> dosens = ConstantValues.simpleList(
								session.createCriteria(Dosen.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.addOrder(Order.asc("nama")),
								Dosen.class);

						MyGrid grid = new MyGrid();
						grid.setWidth("100%");
						grid.setParent(div);
						grid.setWidth("100%");
						grid.setHeight("100%");
						grid.setMold("paging");
						grid.setPageSize(20);
						grid.getPagingChild().setMold("os");

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig("Dosen");
						column.setWidth("40%");
						column.setParent(columns);

						column = new MyColumnConfig("Total SKS");
						column.setParent(columns);
						column.setAlign("right");

						Rows rows = new Rows();
						rows.setParent(grid);

						for (Dosen dosen : dosens) {

							Criterion criterion = dosen == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.eq("dosen1", dosen),
											Restrictions.eq("dosen2", dosen));

							criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

							Number totalsks = (Number) session.createCriteria(Perkuliahan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

									.add(!sp ? Restrictions.isNull("statusSemesterPendek")
											: Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK))

									.add(tahunAkademik.getSelectedItem() == null
											|| tahunAkademik.getSelectedItem().getValue() == null
													? Restrictions.sqlRestriction("1=1")
													: Restrictions.eq("tahunAjaran",
															tahunAkademik.getSelectedItem().getValue()))

									.add(genapGanjil.getSelectedItem() == null
											|| genapGanjil.getSelectedItem().getValue() == null
													? Restrictions.sqlRestriction("1=1")
													: Restrictions.eq("ganjilGenap",
															genapGanjil.getSelectedItem().getValue()))

									.add(criterion).createAlias("matakuliah", "matakuliah")
									.setProjection(Projections.sum("matakuliah.sks")).uniqueResult();

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);
							row.appendChild(new Label(dosen.getNama()));
							row.appendChild(new Label(
									Common.numberFormat.get().format(totalsks == null ? 0 : totalsks.intValue())));
						}

					}

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelTotalSKS.getChildren().isEmpty()) {
							reload();
						}

					}
				});

			}
		});

	}

}
