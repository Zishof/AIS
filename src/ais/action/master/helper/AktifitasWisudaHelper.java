package ais.action.master.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.classroom.ClassRoomUtil;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.Wisuda;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class AktifitasWisudaHelper {

	protected PenjadwalanWisudaHelper penjadwalanHelper = new PenjadwalanWisudaHelper();

	public AktifitasWisudaHelper() {
	}

	public Toolbar initAgendaWisuda(final Wisuda wisuda, final DataLoader dataLoader) {

		Tbmuser tbmuser = Common.getCurrentUser();
		Toolbar hbox = new Toolbar();
		hbox.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Agenda Kegiatan", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				penjadwalanHelper.display(wisuda, dataLoader);
			}

		});

		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(wisuda, true);
			}

		});
		button.setParent(hbox);

		PenjadwalanHelper.tampilTombolAmbil(hbox, null, null, null, null, null, null, wisuda, dataLoader);

		AktifitasPerkuliahanHelper.tampilCalender(hbox, dataLoader, wisuda);

		ClassRoomUtil.createButton(wisuda, dataLoader).setParent(hbox);
		RecoveryPertemuanHelper.button(wisuda, new EventListener() {
			
			@Override
			public void onEvent(Event arg0) throws Exception {
				wisuda.belum();
				dataLoader.loadData(null);
			}
		}).setParent(hbox);
		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(hbox);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				wisuda.belum();
				dataLoader.loadData(null);
			}
		});

		return hbox;
	}

	public void initDetail(final Wisuda wisuda, final MyDiv groupbox) throws Exception {
		initDetail(wisuda, null, groupbox);
	}

	public void initDetail(final Wisuda wisuda, final DataLoader mydataLoader, final MyDiv groupbox) throws Exception {

		final DataLoader dataLoader = mydataLoader == null ? new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(wisuda, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} : mydataLoader;

		groupbox.setStyle("border: none;");
		Common.clear(groupbox);

		groupbox.appendChild(initAgendaWisuda(wisuda, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(wisuda, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		}));

		TreeMap<String, Long> pertemuans = wisuda.ambilPertemuan();
		System.out.println("pertemuans -> " + pertemuans.size());
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& pertemuans.isEmpty()) {

			Pertemuan pertemuan = new Pertemuan();

			pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
			pertemuan.setTanggal(wisuda.getTanggal() == null ? ais.ui.util.WaktuUtil.getDate() : wisuda.getTanggal());
			pertemuan.setWisuda(wisuda);
			pertemuan.setTopik("Agenda wisuda \"" + wisuda.infoSimple() + "\"");
			pertemuan.setWaktuMulai(Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));
			pertemuan.setWaktuSelesai(Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));

			try {
				Session session = HibernateUtil.currentSession();
				session.save(pertemuan);
				session.flush();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasWisudaHelper.java:155");
			}
			pertemuans.put(Common.dateFormat8.get().format(pertemuan.getTanggal()) + "_" + pertemuan.getId(),
					pertemuan.getId());

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					wisuda.belum();
					initDetail(wisuda, dataLoader, groupbox);
				}
			});

			return;
		}

		groupbox.setStyle("height:6000px;");

		MyGrid grid = new MyGrid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("height: 6000px;");
		grid.setSclass("fgrid");
		grid.setMold("paging");
		grid.setPageSize(1);
		grid.getPagingChild().setMold("os");
		grid.setPagingPosition("top");

		Rows rows = new Rows();
		rows.setParent(grid);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
		calendar1.set(Calendar.DATE, calendar1.get(Calendar.DATE) + 6);

		boolean urut = false;
		try {
			String pil = tbmuser.retreive("urutkan_diskusi_berdasarkan_terlama");
			urut = (pil == null || pil.trim().isEmpty() ? false : Boolean.parseBoolean(pil));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasWisudaHelper.java:199");
			// TODO: handle exception
		}

		boolean mobile = Common.isMobile();
		int selected = 0;
		Date sekarang = WaktuUtil.getDate();
		for (Long pertemuanid : pertemuans.values()) {
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
			if (pertemuan != null) {
				final Row rowUtama = new Row();
				rowUtama.setParent(rows);
				rowUtama.setValign("top");
				if (pertemuan.getTanggal() != null && pertemuan.getTanggal().before(sekarang)) {
					selected++;
				}
				String tgl = pertemuan.getTanggal() == null ? "-"
						: Common.dateFormat11.get().format(pertemuan.getTanggal()) + " "
								+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
										: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai());

				Groupbox pertemuanBox = new ais.ui.util.MyGroupboxStyled();
				pertemuanBox.setWidth(mobile ? "93%" : "95%");
				rowUtama.appendChild(pertemuanBox);
				MyCaptionStyled c;
				pertemuanBox.appendChild(
						c = new MyCaptionStyled("Pertemuan ke-" + pertemuan.getPertemuanKe() + ", " + tgl));
				c.setStyle("font-size:12px;font-weight: bolder;text-decoration: none;color:"
						+ pertemuan.warna().split(",")[0] + ";border: 1px solid " + pertemuan.warna().split(",")[0]
						+ ";\r\n" + "  padding: 5px;" + "  background-color: rgba(169,169,169,0.4);" + "  border-radius: 5px 15px;");

				Vbox a = RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan,
						pertemuan.getTanggal() == null ? "-"
								: Common.dateFormat11.get().format(pertemuan.getTanggal()) + " "
										+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
												: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai()));

				Vbox vbox = new Vbox();
				vbox.setParent(pertemuanBox);

				a.setParent(vbox);
				new Label(pertemuan.getTopik()).setParent(vbox);

				DashboardTimelinePertemuan.displayCatatan(vbox, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
					}
				}, pertemuan, tbmuser, mobile);

				Component aa = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, null, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
					}
				});

				Component bb = AbsensiHelper.createTombolAbsen(pertemuan, true, dataLoader);

				AktifitasPerkuliahanHelper.createKeterangan(pertemuan, dataLoader, aa, bb,
						DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuan)).setParent(vbox);

				pertemuan.masukkanData("akses");
				if (Common.bolehKonfigurasi("komentar_tampil_di_halaman_utama_elearning")) {
					Vbox vbox2 = new Vbox();
					vbox2.setParent(pertemuanBox);
					if (!pertemuan.udah()) {
						Session session = HibernateUtil.currentSession();
						pertemuan.reInitPertemuanPunyaDiskusi(session);
					}

					TreeSet<Long> pertemuanPunyaDiskusisa = pertemuan.ambilPertemuanPunyaDiskusiTotal(urut);
					DashboardTimelinePertemuan.loadKomentarDetail(null, "42px", pertemuanPunyaDiskusisa, pertemuan,
							vbox2, "background-color: rgba(255,255,255,0.5);", 0, 10, false, null);
				}
			}
		}
		try {
			grid.getPagingChild().setActivePage(selected);
		} catch (Exception e) {
			try {
				grid.getPagingChild().setActivePage(selected - 1);
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasWisudaHelper.java:283");
				// TODO: handle exception
			}
		}
	}

}
