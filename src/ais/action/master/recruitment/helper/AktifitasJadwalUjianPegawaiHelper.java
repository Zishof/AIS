package ais.action.master.recruitment.helper;

import java.util.Calendar;
import java.util.TreeMap;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
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

import ais.action.master.helper.AbsensiHelper;
import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.classroom.ClassRoomUtil;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.recruitment.JadwalUjianPegawai;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

public class AktifitasJadwalUjianPegawaiHelper {

	protected PenjadwalanUjianPegawaiHelper penjadwalanHelper = new PenjadwalanUjianPegawaiHelper();

	public AktifitasJadwalUjianPegawaiHelper() {

	}

	public Toolbar initAgendaJadwalUjianPegawai(final JadwalUjianPegawai jadwalUjianPegawai,
			final DataLoader dataLoader) {

		CalonPegawai calonPegawai = Common.getCurrentUser() == null ? null : Common.getCurrentUser().getCalonPegawai();

		Toolbar hbox = new Toolbar();
		hbox.setVisible(calonPegawai == null);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Agenda Jadwal Ujian Pegawai", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				penjadwalanHelper.display(jadwalUjianPegawai, dataLoader);
			}

		});

		button.setParent(hbox);
		
		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(jadwalUjianPegawai, true);
			}

		});
		button.setParent(hbox);

		AktifitasPerkuliahanHelper.tampilCalender(hbox, dataLoader, jadwalUjianPegawai);

		ClassRoomUtil.createButton(jadwalUjianPegawai, dataLoader).setParent(hbox);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(hbox);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalUjianPegawai.belum();
				dataLoader.loadData(null);
			}
		});

		return hbox;
	}

	public void initDetail(final JadwalUjianPegawai jadwalUjianPegawai, final Div groupbox) throws Exception {
		initDetail(jadwalUjianPegawai, null, groupbox, true);
	}

	public void initDetail(final JadwalUjianPegawai jadwalUjianPegawai, final DataLoader mydataLoader,
			final Div groupbox, boolean check) throws Exception {

		final DataLoader dataLoader = mydataLoader == null ? new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(jadwalUjianPegawai, groupbox);
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

		Tab tab = new Tab("Agenda " + jadwalUjianPegawai.getNama());
		tab.setParent(tabs);
		tab.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					initDetail(jadwalUjianPegawai, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		ais.ui.util.MyDiv myGroupbox = new ais.ui.util.MyDiv();
		myGroupbox.setStyle("min-height: 500px;");
		myGroupbox.setParent(tabpanel);
		myGroupbox.appendChild(initAgendaJadwalUjianPegawai(jadwalUjianPegawai, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(jadwalUjianPegawai, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		}));

		TreeMap<String, Long> pertemuans = jadwalUjianPegawai.ambilPertemuan();
		Tbmuser tbmuser = Common.getCurrentUser();
		if (check && tbmuser != null && tbmuser.getCalonPegawai() == null && tbmuser.getCalonPegawai() == null
				&& pertemuans.isEmpty()) {

			Pertemuan pertemuan = new Pertemuan();

			pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
			pertemuan.setTanggal(jadwalUjianPegawai.getWaktuMulai());
			pertemuan.setJadwalUjianPegawai(jadwalUjianPegawai);
			pertemuan.setTopik(jadwalUjianPegawai.getNama());
			pertemuan.setWaktuMulai(Common.dateFormat3.get().format(jadwalUjianPegawai.getWaktuMulai()));
			pertemuan.setWaktuSelesai(Common.dateFormat3.get().format(jadwalUjianPegawai.getWaktuSampai()));

			try {
				Session session = HibernateUtil.currentSession();
				session.save(pertemuan);
				session.flush();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/recruitment/helper/AktifitasJadwalUjianPegawaiHelper.java:184");
			}

			pertemuans.put(Common.dateFormat8.get().format(pertemuan.getTanggal()) + "_" + pertemuan.getId(),
					pertemuan.getId());

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jadwalUjianPegawai.belum();
					initDetail(jadwalUjianPegawai, dataLoader, groupbox, false);
				}
			});

			return;
		}

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(myGroupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Tanggal / Waktu");
		column.setWidth("25%");
		column.setParent(columns);

		column = new MyColumnConfig("Materi");
		column.setParent(columns);

		column = new MyColumnConfig("Absen");
		column.setParent(columns);
		column.setWidth("10%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
		calendar1.set(Calendar.DATE, calendar1.get(Calendar.DATE) + 6);

		for (Long pertemuanid : pertemuans.values()) {
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
			if (pertemuan != null) {
				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

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

				Vbox vbox = new Vbox();
				vbox.setParent(row);
				new Label(pertemuan.getTopik()).setParent(vbox);
				AktifitasPerkuliahanHelper.createKeterangan(pertemuan, dataLoader).setParent(vbox);

				row.appendChild(AbsensiHelper.createTombolAbsen(pertemuan, dataLoader));
			}
		}
	}

}
