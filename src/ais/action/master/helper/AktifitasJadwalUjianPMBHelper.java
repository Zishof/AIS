package ais.action.master.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.classroom.ClassRoomUtil;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DataPunyaArtikel;
import ais.database.model.DataPunyaBukuBahanAjar;
import ais.database.model.DataPunyaItem;
import ais.database.model.GeneralValueObject;
import ais.database.model.JadwalUjianPMB;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class AktifitasJadwalUjianPMBHelper {

	protected PenjadwalanUjianPMBHelper penjadwalanHelper = new PenjadwalanUjianPMBHelper();

	public AktifitasJadwalUjianPMBHelper() {

	}

	public Toolbar initAgendaJadwalUjianPMB(final JadwalUjianPMB jadwalUjianPMB, final DataLoader dataLoader) {

		Mahasiswa mahasiswa = Common.getCurrentUser() == null ? null : Common.getCurrentUser().getMahasiswa();

		Toolbar hbox = new Toolbar();
		hbox.setVisible(mahasiswa == null);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Agenda Ujian PMB", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				penjadwalanHelper.display(jadwalUjianPMB, dataLoader);
			}

		});

		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(jadwalUjianPMB, true);
			}

		});
		button.setParent(hbox);

		AktifitasPerkuliahanHelper.tampilCalender(hbox, dataLoader, jadwalUjianPMB);

		ClassRoomUtil.createButton(jadwalUjianPMB, dataLoader).setParent(hbox);
		RecoveryPertemuanHelper.button(jadwalUjianPMB, new EventListener() {
			
			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalUjianPMB.belum();
				dataLoader.loadData(null);
			}
		}).setParent(hbox);
		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(hbox);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalUjianPMB.belum();
				dataLoader.loadData(null);
			}
		});

		return hbox;
	}

	public void initDetail(final JadwalUjianPMB jadwalUjianPMB, final Div groupbox) throws Exception {
		initDetail(jadwalUjianPMB, null, groupbox);
	}

	public void initDetail(final JadwalUjianPMB jadwalUjianPMB, final DataLoader mydataLoader, final Div groupbox)
			throws Exception {

		final DataLoader dataLoader = mydataLoader == null ? new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(jadwalUjianPMB, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} : mydataLoader;

		groupbox.setStyle("border: none;");
		Common.clear(groupbox);

		final Tabbox tabbox = new Tabbox();
		tabbox.setSclass("ais-aktifitas-tabbox");
		tabbox.setParent(groupbox);
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		Tab tab = new Tab("Agenda " + jadwalUjianPMB.getNama());
		tab.setParent(tabs);
		tab.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					initDetail(jadwalUjianPMB, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		final MyTabConfig tabReferensi = new MyTabConfig("Buku Referensi");
		tabReferensi.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		ais.ui.util.MyDiv myGroupbox = new ais.ui.util.MyDiv();
		myGroupbox.setStyle("min-height: 500px;");
		myGroupbox.setParent(tabpanel);
		myGroupbox.appendChild(initAgendaJadwalUjianPMB(jadwalUjianPMB, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(jadwalUjianPMB, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		}));

		TreeMap<String, Long> pertemuans = jadwalUjianPMB.ambilPertemuan();
		tabpanels.setStyle("height: " + (pertemuans.size() * 6000) + "px;");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& pertemuans.isEmpty()) {

			Pertemuan pertemuan = new Pertemuan();

			pertemuan.setStatusPertemuan(ConstantValues.UJIAN_ONLINE);
			pertemuan.setTanggal(jadwalUjianPMB.getWaktuMulai());
			pertemuan.setJadwalUjianPMB(jadwalUjianPMB);
			pertemuan.setTopik(jadwalUjianPMB.getNama());
			pertemuan.setWaktuMulai(Common.dateFormat3.get().format(jadwalUjianPMB.getWaktuMulai()));
			pertemuan.setWaktuSelesai(Common.dateFormat3.get().format(jadwalUjianPMB.getWaktuSampai()));

			try {
				Session session = HibernateUtil.currentSession();
				session.save(pertemuan);
				session.flush();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasJadwalUjianPMBHelper.java:203");
			}
			pertemuans.put(Common.dateFormat8.get().format(pertemuan.getTanggal()) + "_" + pertemuan.getId(),
					pertemuan.getId());

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jadwalUjianPMB.belum();
					initDetail(jadwalUjianPMB, dataLoader, groupbox);
				}
			});
			return;
		}

		myGroupbox.setStyle("height:6000px;");

		MyGrid grid = new MyGrid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(myGroupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("height: 6000px;");
		grid.setSclass("fgrid");
		grid.setMold("paging");
		grid.setPageSize(1);
		grid.getPagingChild().setMold("os");
		grid.setPagingPosition("top");

		Columns columns = new Columns();
		columns.setParent(grid);

		if (Common.isMobile()) {
			MyColumnConfig column = new MyColumnConfig("No.");
			column.setParent(columns);
			column.setWidth("30px");

			column = new MyColumnConfig("Materi");
			column.setParent(columns);
		} else {

			MyColumnConfig column = new MyColumnConfig("No.");
			column.setParent(columns);
			column.setWidth("30px");

			column = new MyColumnConfig("Tanggal / Waktu");
			column.setWidth("25%");
			column.setParent(columns);

			column = new MyColumnConfig("Materi");
			column.setParent(columns);

		}

		Rows rows = new Rows();
		rows.setParent(grid);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
		calendar1.set(Calendar.DATE, calendar1.get(Calendar.DATE) + 6);
		int selected = 0;
		Date sekarang = WaktuUtil.getDate();
		boolean mobile = Common.isMobile();
		for (Long pertemuanid : pertemuans.values()) {
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
			if (pertemuan != null) {
				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setValign("top");
				row.setParent(rows);

				if (pertemuan.getTanggal() != null && pertemuan.getTanggal().before(sekarang)) {
					selected++;
				}

				new Label(pertemuan.getPertemuanKe() + "").setParent(row);

				if (pertemuan.getTanggal() != null && calendar.getTime().before(pertemuan.getTanggal())
						&& calendar1.getTime().after(pertemuan.getTanggal())) {
					row.setStyle("background-color: rgba(144,238,144,0.4);");
				} else if (pertemuan.getTanggal() != null && calendar.getTime().after(pertemuan.getTanggal())) {
					row.setStyle("background-color: rgba(169,169,169,0.4);");
				} else {
				}

				Vbox a = RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan,
						pertemuan.getTanggal() == null ? "-"
								: Common.dateFormat11.get().format(pertemuan.getTanggal()) + " "
										+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
												: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai()));
				a.appendChild(new Label(
						pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama()));
				a.setParent(row);

				Component aa = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, null, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
					}
				});

				Component bb = AbsensiHelper.createTombolAbsen(pertemuan, true, dataLoader);

				Vbox vbox = new Vbox();
				vbox.setParent(Common.isMobile() ? a : row);
				new Label(pertemuan.getTopik()).setParent(vbox);

				AktifitasPerkuliahanHelper.createKeterangan(pertemuan, dataLoader, aa, bb,
						DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuan)).setParent(vbox);

				DashboardTimelinePertemuan.displayCatatan(vbox, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
					}
				}, pertemuan, tbmuser, mobile);

			}
		}

		try {
			grid.getPagingChild().setActivePage(selected);
		} catch (Exception e) {
			try {
				grid.getPagingChild().setActivePage(selected - 1);
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasJadwalUjianPMBHelper.java:332");
				// TODO: handle exception
			}
		}

		Session session = HibernateUtil.currentSession();
		final Tabpanel tabpanelReferensi = new ais.ui.util.MyTabpanel();
		int jumlahReferensi = ((Number) session.createCriteria(DataPunyaItem.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("jadwalUjianPMB", jadwalUjianPMB))
				.uniqueResult()).intValue();
		tabReferensi.setLabel("Buku Referensi " + (jumlahReferensi == 0 ? "" : "(" + jumlahReferensi + ")"));

		tabpanelReferensi.setParent(tabpanels);
		tabpanelReferensi.setHeight("1250px");
		tabReferensi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelReferensi.getChildren().size() == 0) {

					DataPunyaItemHelper dataPunyaItemHelper = new DataPunyaItemHelper();
					dataPunyaItemHelper.display(null, null, jadwalUjianPMB, null, null, tabpanelReferensi);
				}
			}
		});

		final MyTabConfig tabBukuAjar = new MyTabConfig("Buku Bahan Ajar");
		tabBukuAjar.setParent(tabs);

		final Tabpanel tabpanelBukuAjar = new ais.ui.util.MyTabpanel();
		int jumlahBukuAjar = ((Number) session.createCriteria(DataPunyaBukuBahanAjar.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("jadwalUjianPMB", jadwalUjianPMB))
				.uniqueResult()).intValue();
		tabBukuAjar.setLabel("Buku Bahan Ajar " + (jumlahBukuAjar == 0 ? "" : "(" + jumlahBukuAjar + ")"));

		tabpanelBukuAjar.setParent(tabpanels);
		tabpanelBukuAjar.setHeight("1250px");
		tabBukuAjar.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelBukuAjar.getChildren().size() == 0) {

					DataPunyaBukuAjarHelper dataPunyaBukuAjarHelper = new DataPunyaBukuAjarHelper();
					dataPunyaBukuAjarHelper.display(null, null, jadwalUjianPMB, null, null, tabpanelBukuAjar);
				}
			}
		});

		final MyTabConfig tabArtikel = new MyTabConfig("Artikel");
		tabArtikel.setParent(tabs);

		final Tabpanel tabpanelArtikel = new ais.ui.util.MyTabpanel();
		int jumlahArtikel = ((Number) session.createCriteria(DataPunyaArtikel.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("jadwalUjianPMB", jadwalUjianPMB))
				.uniqueResult()).intValue();
		tabArtikel.setLabel("Artikel " + (jumlahArtikel == 0 ? "" : "(" + jumlahArtikel + ")"));

		tabpanelArtikel.setParent(tabpanels);
		tabpanelArtikel.setHeight("1250px");
		tabArtikel.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelArtikel.getChildren().size() == 0) {

					DataPunyaArtikelHelper dataPunyaArtikelHelper = new DataPunyaArtikelHelper();
					dataPunyaArtikelHelper.display(null, null, jadwalUjianPMB, null, null, null, null, tabpanelArtikel);
				}
			}
		});
	}

}
