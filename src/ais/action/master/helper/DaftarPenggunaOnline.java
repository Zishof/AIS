package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.SecurityFilter;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.OnlineUsers;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sisdes.Penduduk;
import ais.ui.util.HeapSizeDemo;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyWindow;

public class DaftarPenggunaOnline extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5620991583788581962L;

	private Toolbar toolbar;
	private MyButtonConfig batal;

	public DaftarPenggunaOnline() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private int tabIndex = 0;

	@SuppressWarnings({ "rawtypes" })
	private void init() throws Exception {

		Common.clear(this);
		setClosable(true);
		setTitle("Daftar Pengguna Online");
		setWidth("90%");
		setHeight("90%");
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);
		toolbar = new Toolbar();
		toolbar.setParent(north);
		batal = new MyButtonConfig("Refresh");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init();
			}
		});
		batal.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		if (Common.getApakahAdmin()) {

			batal = new MyButtonConfig("Bersihkan Memori Tak Terpakai");
			batal.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Runtime.getRuntime().gc();
					Runtime.getRuntime().runFinalization();
					HeapSizeDemo.check();
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							init();
						}
					});
				}
			});
			batal.setParent(toolbar);

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(center);

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			Tab tab = new Tab("Pengguna Online", "/img/online-icon_access.png");
			tab.setParent(tabs);

			Tab tab1 = new Tab("Pemakaian Memori", "/img/chart-pie-icon.png");
			tab1.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setParent(tabpanels);

			Borderlayout myborderlayout = new Borderlayout();
			myborderlayout.setParent(tabpanel);

			center = new Center();
			center.setParent(myborderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Tabpanel tabpanelBelumAkses = new ais.ui.util.MyTabpanel();
			tabpanelBelumAkses.setParent(tabpanels);

			Borderlayout myborderlayoutBelumAkses = new Borderlayout();
			myborderlayoutBelumAkses.setParent(tabpanelBelumAkses);

			Center centerBelumAkses = new Center();
			centerBelumAkses.setParent(myborderlayoutBelumAkses);
			ais.ui.util.ZkCompat.setFlex(centerBelumAkses, true);

			List freeMem = new ArrayList();
			List usedMem = new ArrayList();
			List timeLabels = new ArrayList();
			int size = HeapSizeDemo.data.size();
			int index = 0;
			synchronized (HeapSizeDemo.data) {
				for (String key : HeapSizeDemo.data.keySet()) {
					if ((size - 50) <= index) {
						Long[] val = HeapSizeDemo.data.get(key);
						freeMem.add(val[0]);
						usedMem.add(val[1]);
						String lbl = (key != null && key.length() > 11) ? key.substring(11, 19) : key;
						timeLabels.add(lbl);
					}
					index++;
				}
			}

			Html memChart = new Html(ais.ui.util.DashboardUiKit.dualLineChart(
					"Pemakaian Memori", "Tren penggunaan memori JVM secara real-time (MB).",
					timeLabels,
					freeMem, "Bebas", "#22c55e",
					usedMem, "Terpakai", "#ef4444"));
			centerBelumAkses.appendChild(memChart);

			tabbox.setSelectedIndex(tabIndex);

			tab.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tabIndex = 0;
				}
			});
			tab1.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tabIndex = 1;
				}
			});
		}

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setMold("paging");
		grid.setPageSize(5);

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig("Nama Pengguna");
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig("Semester");
		column.setWidth("10%");
		column.setParent(columns);
		column = new MyColumnConfig("Jenis Pengguna");
		// column.setWidth("25%");
		column.setParent(columns);

		column = new MyColumnConfig("Unit");
		column.setParent(columns);
		column = new MyColumnConfig("Sub Unit");
		column.setParent(columns);

		column = new MyColumnConfig("Waktu Login");
		// column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig("Lama");
		column.setWidth("10%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Map<String, OnlineUsers> set = new HashMap<String, OnlineUsers>();
		for (OnlineUsers onlineUsers : SecurityFilter.dataOnline.values()) {
			set.put(onlineUsers.getNama(), onlineUsers);
		}
		for (OnlineUsers objects : set.values()) {
			Mahasiswa mahasiswa = objects.getMahasiswa();
			Siswa siswa = objects.getSiswa();
			Guru guru = objects.getTbmuser() == null ? null : objects.getTbmuser().getGuru();
			Dosen dosen = objects.getDosen();
			Penduduk penduduk = objects.getPenduduk();
			Tbmuser tbmuser = objects.getTbmuser();
			String roleName = mahasiswa != null ? Common.getBahasa("label_mahasiswa")
					: dosen != null ? Common.getBahasa("label_dosen")
							: tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleName();
			if (roleName == null) {
				continue;
			}

			final Date jam = objects.getLogin().getLogin();

			long diff = 0L;
			long diffDetik = 0L;
			long diffMenit = 0L;
			long diffJam = 0L;
			if (jam != null) {
				diff = ais.ui.util.WaktuUtil.getDate().getTime() - jam.getTime();
				diffDetik = (diff / (1000/* * 60 * 60 * 24 */)) % 60;
				diffMenit = (diff / (1000 * 60 /** 60 * 24 */
				)) % 60;
				diffJam = (diff / (1000 * 60 * 60 /** 24 */
				));
			}
			if (guru != null) {

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				Vbox vbox = new Vbox();
				vbox.setParent(row);
				CommonMedia.tampilkanGambarKecil(guru).setParent(vbox);
				vbox.appendChild(new Label(guru.getNama()));

				row.appendChild(new Label(guru.getNuptk()));

				row.appendChild(new Label(roleName));
				row.appendChild(new Label(guru.getSekolah() == null ? "" : guru.getSekolah().getNama()));
				row.appendChild(new Label(guru.getYayasan() == null ? "" : guru.getYayasan().getNama()));

				row.appendChild(new Label(jam == null ? "" : Common.dateFormat3.get().format(jam)));

				final Label time = new Label(ais.common.Common.getBahasaConfig("0 detik 0 menit 0 jam"));
				row.appendChild(time);

				time.setValue(Common.numberFormat.get().format(diffJam) + " jam " + Common.numberFormat.get().format(diffMenit)
						+ " menit " + Common.numberFormat.get().format(diffDetik) + " detik");

			} else if (siswa != null) {

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				Vbox vbox = new Vbox();
				vbox.setParent(row);
				CommonMedia.tampilkanGambarKecil(siswa).setParent(vbox);
				vbox.appendChild(new Label(siswa.getNama()));

				row.appendChild(new Label(siswa.getKelas() == null ? "" : siswa.getKelas().getNama()));

				row.appendChild(new Label(roleName));
				row.appendChild(new Label(siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama()));
				row.appendChild(new Label(siswa.getYayasan() == null ? "" : siswa.getYayasan().getNama()));

				row.appendChild(new Label(jam == null ? "" : Common.dateFormat3.get().format(jam)));

				final Label time = new Label(ais.common.Common.getBahasaConfig("0 detik 0 menit 0 jam"));
				row.appendChild(time);

				time.setValue(Common.numberFormat.get().format(diffJam) + " jam " + Common.numberFormat.get().format(diffMenit)
						+ " menit " + Common.numberFormat.get().format(diffDetik) + " detik");

			}

			else if (penduduk != null) {

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				Vbox vbox = new Vbox();
				vbox.setParent(row);
				CommonMedia.tampilkanGambarKecil(penduduk).setParent(vbox);
				vbox.appendChild(new Label(penduduk.getNama()));

				row.appendChild(new Label(penduduk.getKelurahan() == null ? "" : penduduk.getKelurahan().getNama()));

				row.appendChild(new Label(roleName));
				row.appendChild(new Label(penduduk.getKota() == null ? "" : penduduk.getKota().getNama()));
				row.appendChild(new Label(penduduk.getPropinsi() == null ? "" : penduduk.getPropinsi().getNama()));

				row.appendChild(new Label(jam == null ? "" : Common.dateFormat3.get().format(jam)));

				final Label time = new Label(ais.common.Common.getBahasaConfig("0 detik 0 menit 0 jam"));
				row.appendChild(time);

				time.setValue(Common.numberFormat.get().format(diffJam) + " jam " + Common.numberFormat.get().format(diffMenit)
						+ " menit " + Common.numberFormat.get().format(diffDetik) + " detik");

			}

			else if (mahasiswa != null) {

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				Vbox vbox = new Vbox();
				vbox.setParent(row);
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(vbox);
				vbox.appendChild(new Label(mahasiswa.getNama()));

				String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
				Integer semster = Common.getSemester(mahasiswa.getTahunangkatan(), semesterMulai,
						mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
				row.appendChild(new Label(semster + ""));

				row.appendChild(new Label(roleName));
				row.appendChild(
						new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getNama()));
				row.appendChild(new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()));

				row.appendChild(new Label(jam == null ? "" : Common.dateFormat3.get().format(jam)));

				final Label time = new Label(ais.common.Common.getBahasaConfig("0 detik 0 menit 0 jam"));
				row.appendChild(time);

				time.setValue(Common.numberFormat.get().format(diffJam) + " jam " + Common.numberFormat.get().format(diffMenit)
						+ " menit " + Common.numberFormat.get().format(diffDetik) + " detik");

			} else if (dosen != null) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				Vbox vbox = new Vbox();
				vbox.setParent(row);
				CommonMedia.tampilkanGambarKecil(dosen).setParent(vbox);
				vbox.appendChild(new Label(dosen.getNama()));

				row.appendChild(new Label(""));
				row.appendChild(new Label(roleName));
				row.appendChild(new Label(dosen.getJurusan() == null || dosen.getJurusan().getFakultas() == null ? ""
						: dosen.getJurusan().getFakultas().getNama()));
				row.appendChild(new Label(dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama()));

				row.appendChild(new Label(jam == null ? "" : Common.dateFormat3.get().format(jam)));
				final Label time = new Label(ais.common.Common.getBahasaConfig("0 detik 0 menit 0 jam"));
				row.appendChild(time);

				time.setValue(Common.numberFormat.get().format(diffJam) + " jam " + Common.numberFormat.get().format(diffMenit)
						+ " menit " + Common.numberFormat.get().format(diffDetik) + " detik");

			} else {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				Vbox vbox = new Vbox();
				vbox.setParent(row);
				CommonMedia.tampilkanGambarKecil(tbmuser).setParent(vbox);
				vbox.appendChild(new Label(tbmuser == null ? "" : tbmuser.getUserNama()));

				row.appendChild(new Label(""));
				row.appendChild(new Label(roleName));
				row.appendChild(new Label(tbmuser == null || tbmuser.ambilJurusan() == null
						|| tbmuser.ambilJurusan().getFakultas() == null ? ""
								: tbmuser.ambilJurusan().getFakultas().getNama()));
				row.appendChild(new Label(
						tbmuser == null || tbmuser.ambilJurusan() == null ? "" : tbmuser.ambilJurusan().getNama()));

				row.appendChild(new Label(jam == null ? "" : Common.dateFormat3.get().format(jam)));
				final Label time = new Label(ais.common.Common.getBahasaConfig("0 detik 0 menit 0 jam"));
				row.appendChild(time);

				time.setValue(Common.numberFormat.get().format(diffJam) + " jam " + Common.numberFormat.get().format(diffMenit)
						+ " menit " + Common.numberFormat.get().format(diffDetik) + " detik");

			}
		}
		set = null;
		// row = new MyFormRow();
		//		// row.setParent(rows);
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		batal = new MyButtonConfig("Tutup");

		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				DaftarPenggunaOnline.this.detach();
			}
		});
		batal.setParent(toolbar);

		UserOnlineCounter.check();
	}
}
