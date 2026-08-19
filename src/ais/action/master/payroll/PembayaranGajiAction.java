package ais.action.master.payroll;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.payroll.detail.PembayaranGajiPunyaPegawaiAction;
import ais.action.master.payroll.helper.PembayaranGajiPunyaPegawaiHelper;
import ais.action.master.payroll.util.PembayaranItemGajiPegawaiTreeModel;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.format1.employ.LaporanPembayaranGaji;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.akunting.StandingInstruction;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.payroll.CaraPembayaranGaji;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataInitDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

public class PembayaranGajiAction extends GenericAutowireComposer implements DataInitDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyGrid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private MyTextbox searchnamaPegawai;

	private Combobox bulan;
	private Intbox tahun;
	private Combobox caraBayar;
	private Combobox caraBayarByr;

	private boolean persetujuan = false;

	private MyWindow addWindow;
	private PembayaranGaji pembayaranGaji;
	private MyDatebox waktuBayar;
	private Combobox bulanByr;
	private Intbox thnByr;
	private Textbox keterangan;

	private boolean delete = false;
	private boolean edit = false;
	private boolean approve = false;
	private boolean reject = false;

	private SatuanKerja satuanKerja = null;
	private CaraPembayaranGaji caraPembayaranGajiDefault;
	private PembayaranGaji pembayaranGajiCopy = null;
	private AmbilDataWorkspaceBanbox workspace;
	private AmbilDataSatuanKerjaBanbox satuanKerjaBox;
	private MyCheckboxConfig tanpaAnggaran;
	private MyCheckboxConfig setujui;
	private MyDatebox tanggalPembuatan;
	private Label kode;
	private MyGrid gridPembayaranGaji;
	private DisposisiSop disposisiSop = null;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulan.appendChild(comboitem);
		}

		Comboitem comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		bulan.appendChild(comboitem);
		if (bulan != null) { bulan.setSelectedItem(comboitem); }
		if (bulan != null) { bulan.setReadonly(true); }

		if (tahun != null) { tahun.setValue(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)); }

		satuanKerja = Common.getSatuanKerja();
		caraPembayaranGajiDefault = (CaraPembayaranGaji) HibernateUtil.currentSession()
				.createCriteria(CaraPembayaranGaji.class).add(Restrictions.eq("defaultPembayaran", true))
				.add(Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.and(Restrictions.isNotNull("akun"),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))))
				.setMaxResults(1).uniqueResult();

		Common.insertComboDanSemua(caraBayar, new String[] { "nama", "satuanKerja" }, "akun", CaraPembayaranGaji.class,
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.and(Restrictions.isNotNull("akun"),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

		if (caraBayar != null) { caraBayar.setReadonly(true); }

		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class PembayaranGajiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PembayaranGaji pembayaranGaji = (PembayaranGaji) arg1;

			new PembayaranGajiPunyaPegawaiAction(pembayaranGaji, pembayaranGaji.getDisetujuiOleh() != null)
					.setParent(arg0);

			if (pembayaranGaji.getDisetujuiOleh() != null) {
				StandingInstruction.simpanPembayaranGajiPunyaPegawai(pembayaranGaji);
			}

			RevisiHelper.createNewRevisi(PembayaranGaji.class, pembayaranGaji,
					Common.dateFormat3.get().format(pembayaranGaji.getWaktuBayar())).setParent(arg0);
			new Label(pembayaranGaji.getBulan() + "").setParent(arg0);
			new Label(pembayaranGaji.getTahun() + "").setParent(arg0);
			try {
				new Label(pembayaranGaji.getCaraPembayaranGaji() == null ? ""
						: pembayaranGaji.getCaraPembayaranGaji().getNama()).setParent(arg0);
				new Label(pembayaranGaji.getSatuanKerja() == null ? "" : pembayaranGaji.getSatuanKerja().getNama())
						.setParent(arg0);
			} catch (Exception e) {
				new Label("").setParent(arg0);
				new Label("").setParent(arg0);
			}
//			if (pembayaranGaji.getNilai() < 0.01) {
//				PembayaranGaji.hitungUlang(pembayaranGaji);
//			}
			new Label(Common.numberFormat.get().format(pembayaranGaji.getNilai())).setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(
					pembayaranGaji.getDibuatOleh() == null ? "" : pembayaranGaji.getDibuatOleh().getUserNama())
					.setParent(a);
			new MyLabelAgakKecil(pembayaranGaji.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(pembayaranGaji.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh = new MyLabelAgakKecil(
					pembayaranGaji.getDisetujuiOleh() == null ? "" : pembayaranGaji.getDisetujuiOleh().getUserNama());

			final MyLabelAgakKecil disetujuiTanggal = new MyLabelAgakKecil(
					pembayaranGaji.getTanggalPersetujuan() == null ? ""
							: Common.dateFormat3.get().format(pembayaranGaji.getTanggalPersetujuan()));

			final MyLabelAgakKecilBoldMerah ditolakOleh = new MyLabelAgakKecilBoldMerah(
					pembayaranGaji.getDitolakOleh() == null ? ""
							: "Ditolak oleh " + pembayaranGaji.getDitolakOleh().getUserNama());
			final MyLabelAgakKecilBoldMerah tanggalDitolak = new MyLabelAgakKecilBoldMerah(
					pembayaranGaji.getTanggalDitolak() == null ? ""
							: Common.dateFormat3.get().format(pembayaranGaji.getTanggalDitolak()));

			ditolakOleh.setParent(a);
			tanggalDitolak.setParent(a);
			disetujuiOleh.setParent(a);
			disetujuiTanggal.setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(pembayaranGaji.getKeterangan())).setParent(vbox1);
			if (pembayaranGaji.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pembayaranGaji.getDisposisiSop().getKeterangan() + " ("
						+ pembayaranGaji.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pembayaranGaji.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			if (pembayaranGaji.getDisposisiSop() != null && !pembayaranGaji.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (pembayaranGaji.getDisetujuiOleh() == null && edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(pembayaranGaji.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pembayaranGaji.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(pembayaranGaji);
					}
				});
			} else {
				new Label(pembayaranGaji.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			Hbox hbxAksi = Common.copyEditDeleteButtons(edit, delete, pembayaranGaji, PembayaranGajiAction.this);
			aksiButtons.addAll(ais.ui.util.UIHelper.ambilItemAksi(hbxAksi));

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(pembayaranGaji, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(arg0);
						}
					});
				}
			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");
			final MyToolbarbuttonConfig ditolak = new MyToolbarbuttonConfig("", "/img/svg/deny.svg");
			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");

			disetujui.setVisible(
					approve && pembayaranGaji.getDisetujuiOleh() == null && pembayaranGaji.getDitolakOleh() == null);
			ditolak.setVisible(
					reject && pembayaranGaji.getDisetujuiOleh() == null && pembayaranGaji.getDitolakOleh() == null);

			dibatalkan.setVisible(
					reject && (pembayaranGaji.getDisetujuiOleh() != null || pembayaranGaji.getDitolakOleh() != null));

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui pembayaran gaji ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pembayaranGaji.setDisetujuiOleh(Common.getCurrentUser());
										pembayaranGaji.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, pembayaranGaji);

										disetujuiTanggal.setValue(pembayaranGaji.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(pembayaranGaji.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pembayaranGaji.getDisetujuiOleh() == null ? ""
												: pembayaranGaji.getDisetujuiOleh().getUserNama());

										ditolakOleh.setValue(pembayaranGaji.getDitolakOleh() == null ? ""
												: "Ditolak oleh " + pembayaranGaji.getDitolakOleh().getUserNama());
										tanggalDitolak.setValue(pembayaranGaji.getTanggalDitolak() == null ? ""
												: Common.dateFormat3.get().format(pembayaranGaji.getTanggalDitolak()));

										disetujui.setVisible(approve && pembayaranGaji.getDisetujuiOleh() == null
												&& pembayaranGaji.getDitolakOleh() == null);
										ditolak.setVisible(reject && pembayaranGaji.getDisetujuiOleh() == null
												&& pembayaranGaji.getDitolakOleh() == null);
										dibatalkan.setVisible(reject && (pembayaranGaji.getDisetujuiOleh() != null
												|| pembayaranGaji.getDitolakOleh() != null));

									}
								}
							});
				}

			});
			aksiButtons.add(disetujui);

			ditolak.setTooltiptext("Ditolak");

			ditolak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menolak pembayaran gaji ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();
										pembayaranGaji.setDitolakOleh(Common.getCurrentUser());
										pembayaranGaji.setTanggalDitolak(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, pembayaranGaji);

										ditolakOleh.setValue(pembayaranGaji.getDitolakOleh() == null ? ""
												: "Ditolak oleh " + pembayaranGaji.getDitolakOleh().getUserNama());
										tanggalDitolak.setValue(pembayaranGaji.getTanggalDitolak() == null ? ""
												: Common.dateFormat3.get().format(pembayaranGaji.getTanggalDitolak()));

										disetujuiTanggal.setValue(pembayaranGaji.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(pembayaranGaji.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pembayaranGaji.getDisetujuiOleh() == null ? ""
												: pembayaranGaji.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && pembayaranGaji.getDisetujuiOleh() == null
												&& pembayaranGaji.getDitolakOleh() == null);
										ditolak.setVisible(reject && pembayaranGaji.getDisetujuiOleh() == null
												&& pembayaranGaji.getDitolakOleh() == null);
										dibatalkan.setVisible(reject && (pembayaranGaji.getDisetujuiOleh() != null
												|| pembayaranGaji.getDitolakOleh() != null));

									}
								}
							});
				}

			});
			aksiButtons.add(ditolak);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan pembayaran gaji ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pembayaranGaji.setDisetujuiOleh(null);
										pembayaranGaji.setTanggalPersetujuan(null);
										pembayaranGaji.setDitolakOleh(null);
										pembayaranGaji.setTanggalDitolak(null);

										Common.refreshUpdate(session, pembayaranGaji);

										disetujuiTanggal.setValue(pembayaranGaji.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(pembayaranGaji.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pembayaranGaji.getDisetujuiOleh() == null ? ""
												: pembayaranGaji.getDisetujuiOleh().getUserNama());

										ditolakOleh.setValue(pembayaranGaji.getDitolakOleh() == null ? ""
												: "Ditolak oleh " + pembayaranGaji.getDitolakOleh().getUserNama());
										tanggalDitolak.setValue(pembayaranGaji.getTanggalDitolak() == null ? ""
												: Common.dateFormat3.get().format(pembayaranGaji.getTanggalDitolak()));

										disetujui.setVisible(approve && pembayaranGaji.getDisetujuiOleh() == null
												&& pembayaranGaji.getDitolakOleh() == null);

										ditolak.setVisible(reject && pembayaranGaji.getDisetujuiOleh() == null
												&& pembayaranGaji.getDitolakOleh() == null);
										dibatalkan.setVisible(reject && (pembayaranGaji.getDisetujuiOleh() != null
												|| pembayaranGaji.getDitolakOleh() != null));

									}
								}
							});
				}

			});
			aksiButtons.add(dibatalkan);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}

	}

	public static void cetak(final PembayaranGaji pembayaranGaji, final EventListener eventListenerData)
			throws Exception {

		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PembayaranGaji.hitungUlang(pembayaranGaji);

				LaporanPembayaranGaji buktiPengeluaranKas = new LaporanPembayaranGaji(pembayaranGaji);
				buktiPengeluaranKas.setTitle("Laporan");
				buktiPengeluaranKas.setClosable(true);
				buktiPengeluaranKas.setHeight("90%");
				buktiPengeluaranKas.setWidth("900px");
				buktiPengeluaranKas.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				buktiPengeluaranKas.onModal();

				if (eventListenerData != null)
					eventListenerData.onEvent(arg0);
			}
		});

		new Thread(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();
				List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = session
						.createCriteria(PembayaranGajiPunyaPegawai.class)
						.add(Restrictions.eq("pembayaranGaji", pembayaranGaji)).list();
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
				HibernateUtil.closeSession();

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.MONTH, pembayaranGaji.getBulan() - 1);
				calendar.set(Calendar.YEAR, pembayaranGaji.getTahun());
				int size = pembayaranGajiPunyaPegawais.size();
				int index = 0;
				for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {

					index++;

					if (pembayaranGajiPunyaPegawai.getPegawai() != null
							&& pembayaranGajiPunyaPegawai.getFormatItemGaji() != null) {

						label.setValue(
								"Memproses data rencana gaji " + pembayaranGajiPunyaPegawai.getPegawai().getNama()
										+ " (" + Common.numberFormat.get().format((index * 100.0) / size) + "%)");

						PembayaranItemGajiPegawaiTreeModel pembayaranItemGajiPegawaiTreeModel = new PembayaranItemGajiPegawaiTreeModel(
								false, pembayaranGajiPunyaPegawai);
						try {
							pembayaranItemGajiPegawaiTreeModel.reset(calendar.getTime(), null,
									pembayaranGaji.getBulan(), pembayaranGaji.getTahun());
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
						pembayaranItemGajiPegawaiTreeModel.setLunas(calendar.getTime());
					}
				}

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnamaPegawai == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);
		List<PembayaranGaji> pembayaranGaji = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pembayaranGaji);
		grid.setRowRenderer(new PembayaranGajiRenderer());
		grid.setModelCheckMobile(strset);

		grid.renderAll();

	}

	private Checkbox searchaktif;

	@SuppressWarnings("unchecked")
	private Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		List<Long> dataPeg = new ArrayList<Long>();
		if (!searchnamaPegawai.getValue().trim().isEmpty()) {
			dataPeg = session.createCriteria(PembayaranGajiPunyaPegawai.class)
					.add(Restrictions.isNotNull("pembayaranGaji"))
					.setProjection(Projections.groupProperty("pembayaranGaji.id")).createAlias("pegawai", "pegawai")
					.add((searchnamaPegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.or(
							Restrictions.ilike("pegawai.nama", searchnamaPegawai.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("pegawai.mycode", searchnamaPegawai.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("pegawai.code", searchnamaPegawai.getValue().trim(),
											MatchMode.ANYWHERE)))

					)).list();
		}

		Criteria criteria = session.createCriteria(PembayaranGaji.class)

				.add((searchnamaPegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (!searchnamaPegawai.getValue().trim().isEmpty() && dataPeg.isEmpty()
						? Restrictions.sqlRestriction("false")
						: dataPeg.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", dataPeg)))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(bulan.getSelectedItem() == null || bulan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("bulan", bulan.getSelectedItem().getValue()))

				.add(caraBayar.getSelectedItem() == null || caraBayar.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("caraPembayaranGaji", caraBayar.getSelectedItem().getValue()))

				.add(tahun.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahun", tahun.getValue()));
		if (order)
			criteria.addOrder(Order.desc("waktuBayar")).addOrder(Order.desc("id"));
		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("keterangan", searchnama.getValue().trim(), MatchMode.ANYWHERE)));
		return criteria;
	}

	public void onAdd(Event event) throws Exception {
		init(new PembayaranGaji());
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		// TODO Auto-generated method stub
		this.pembayaranGaji = (PembayaranGaji) obj;

		this.pembayaranGajiCopy = (PembayaranGaji) obj.getCopyDari();

		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");

		disposisiSop = null;
		center.appendChild(form(obj, null, save, null));

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
		addWindow.setVisible(true);
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, kolom Kode Pembayaran Gaji belum diisi. Langkah yang dapat dilakukan: (1) isikan Kode pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (thnByr.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, kolom Tahun Pembayaran Gaji belum diisi. Langkah yang dapat dilakukan: (1) isikan Tahun pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (caraBayarByr.getSelectedItem() == null || caraBayarByr.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Cara Pembayaran belum dipilih. Langkah yang dapat dilakukan: (1) pilih Cara Pembayaran yang sesuai pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		try {
			if (pembayaranGaji == null || pembayaranGaji.getId() == null) {
				Session session = HibernateUtil.currentSession();
				Number n = ((Number) session.createCriteria(PembayaranGaji.class).setProjection(Projections.rowCount())
						.add(Restrictions.eq("tahun", thnByr.getValue()))
						.add(Restrictions.eq("bulan", bulanByr.getSelectedItem().getValue()))
						.add(Restrictions.eq("caraPembayaranGaji", caraBayarByr.getSelectedItem().getValue()))
						.add(satuanKerjaBox.getAttribute("satuanKerja") != null
								? Restrictions.eq("satuanKerja", satuanKerjaBox.getAttribute("satuanKerja"))
								: Restrictions.isNull("satuanKerja"))
						.uniqueResult());

				if (n.intValue() > 0) {
					MyMessageboxConfig.show(
							"Pembayaran gaji bulan, tahun, cara bayar, dan satuan kerja sudah ada di database",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/PembayaranGajiAction.java:706");
			// TODO: handle exception
		}

//		boolean i = check();
//		if (i) {
//			MyMessageboxConfig.show("Pembayaran gaji bulan, tahun, dan cara bayar sudah ada di database", "Peringatan",
//					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
//			return false;
//		}

		List<Row> rowsMasterAsset = gridPembayaranGaji.getRows().getChildren();
		for (Row row : rowsMasterAsset) {
			PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai = (PembayaranGajiPunyaPegawai) row
					.getAttribute("pembayaranGajiPunyaPegawai");
			if (pembayaranGajiPunyaPegawai.getPegawai() == null) {
				MyMessageboxConfig.show("Mohon maaf, data Pegawai pada salah satu baris belum diisi. Langkah yang dapat dilakukan: (1) pastikan semua baris memiliki data pegawai yang terpilih; (2) hapus baris yang tidak memiliki pegawai atau pilihkan pegawai yang sesuai; (3) simpan kembali data ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		try {
			if (pembayaranGaji != null && pembayaranGaji.getId() != null) {
				pembayaranGaji = (PembayaranGaji) session.createCriteria(PembayaranGaji.class)
						.add(Restrictions.idEq(pembayaranGaji.getId())).uniqueResult();

			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		try {
			if (pembayaranGaji == null) {
				pembayaranGaji = new PembayaranGaji();
			}
		} catch (Exception e) {
			pembayaranGaji = new PembayaranGaji();
		}

		try {
			pembayaranGaji.setWaktuBayar(waktuBayar.getValue());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		try {

			pembayaranGaji.setKeterangan(keterangan.getValue());
			pembayaranGaji.setCaraPembayaranGaji((CaraPembayaranGaji) caraBayarByr.getSelectedItem().getValue());
			pembayaranGaji.setBulan((Integer) bulanByr.getSelectedItem().getValue());
			pembayaranGaji.setTahun(thnByr.getValue());

			Workspace work = (Workspace) workspace.getAttribute("workspace");

			pembayaranGaji.setWorkspace(work);
			pembayaranGaji.setTanpaAnggaran(tanpaAnggaran.isChecked());
			pembayaranGaji.setSatuanKerja((SatuanKerja) satuanKerjaBox.getAttribute("satuanKerja"));
			pembayaranGaji.setTanggalPembuatan(tanggalPembuatan.getValue());

			if (disposisiSop != null && disposisiSop.getId() != null) {
				pembayaranGaji.setDisposisiSop(disposisiSop);
			}

			if (pembayaranGaji.getId() != null) {
				Common.refreshUpdate(session, pembayaranGaji);
			} else {
				pembayaranGaji.setDibuatOleh(Common.getCurrentUser());
				String noAgenda = generateCode(true);
				kode.setValue(noAgenda);
				pembayaranGaji.setKode(kode.getValue());
				session.save(pembayaranGaji);
				session.flush();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		for (Row row : rowsMasterAsset) {
			try {
				PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai = (PembayaranGajiPunyaPegawai) row
						.getAttribute("pembayaranGajiPunyaPegawai");
				if (pembayaranGajiPunyaPegawai != null) {
					pembayaranGajiPunyaPegawai.setPembayaranGaji(pembayaranGaji);
					if (pembayaranGajiPunyaPegawai.getId() != null) {
						Common.refreshUpdate(session, pembayaranGajiPunyaPegawai);
					} else {
						session.save(pembayaranGajiPunyaPegawai);
						session.flush();
					}
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		if (pembayaranGaji.getDisetujuiOleh() != null) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					StandingInstruction.simpanPembayaranGajiPunyaPegawai(pembayaranGaji);
				}
			});
		}

		if (pembayaranGajiCopy != null) {

			final Label label = Common.displayLoadBar(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					PembayaranGaji.hitungUlang(pembayaranGaji);

					onSearchDefault(arg0);
				}
			});

			new Thread(new Runnable() {

				@Override
				public void run() {
					try {

					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.MONTH, pembayaranGaji.getBulan() - 1);
					calendar.set(Calendar.YEAR, pembayaranGaji.getTahun());

					Session session = HibernateUtil.currentNativeSession();
					List<Pegawai> pegawais = ConstantValues
							.simpleList(
									session.createCriteria(PembayaranGajiPunyaPegawai.class)
											.setProjection(Projections.groupProperty("pegawai.id"))
											.add(Restrictions.isNotNull("pegawai"))
											.add(Restrictions.eq("pembayaranGaji", pembayaranGajiCopy)),
									Pegawai.class, false);
					// session.disconnect();
					if (session.isOpen()) {session.disconnect();session.close();}
					HibernateUtil.closeSession();

					int size = pegawais.size();
					int index = 0;
					for (Pegawai pegawai : pegawais) {
						index++;
						label.setValue("Memproses data gaji " + pegawai.getNama() + " ("
								+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

						for (FormatItemGaji formatItemGaji : pegawai.ambilFormatItemGajis()) {

							session = HibernateUtil.currentNativeSession();
							PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai = (PembayaranGajiPunyaPegawai) session
									.createCriteria(PembayaranGajiPunyaPegawai.class)
									.add(Restrictions.or(Restrictions.isNull("formatItemGaji"),
											Restrictions.eq("formatItemGaji", formatItemGaji)))
									.add(Restrictions.eq("pembayaranGaji", pembayaranGaji))
									.add(Restrictions.eq("pegawai", pegawai)).setMaxResults(1).uniqueResult();

							if (pembayaranGajiPunyaPegawai == null) {
								pembayaranGajiPunyaPegawai = new PembayaranGajiPunyaPegawai();
								pembayaranGajiPunyaPegawai.setPegawai(pegawai);
								pembayaranGajiPunyaPegawai.setFormatItemGaji(formatItemGaji);
								pembayaranGajiPunyaPegawai.setKeterangan("");
								pembayaranGajiPunyaPegawai.setPembayaranGaji(pembayaranGaji);

								session.getTransaction().begin();
								session.save(pembayaranGajiPunyaPegawai);
								session.getTransaction().commit();

							}

							if (pembayaranGajiPunyaPegawai.getId() != null
									&& pembayaranGajiPunyaPegawai.getFormatItemGaji() == null) {
								pembayaranGajiPunyaPegawai.setFormatItemGaji(formatItemGaji);
								session.getTransaction().begin();
								session.update(pembayaranGajiPunyaPegawai);
								session.getTransaction().commit();
							}

							// session.disconnect();
							if (session.isOpen()) {session.disconnect();session.close();}
							HibernateUtil.closeSession();

							if (pembayaranGajiPunyaPegawai.getPegawai() != null && formatItemGaji != null) {
								PembayaranItemGajiPegawaiTreeModel pembayaranItemGajiPegawaiTreeModel = new PembayaranItemGajiPegawaiTreeModel(
										false, pembayaranGajiPunyaPegawai);
								try {
									pembayaranItemGajiPegawaiTreeModel.reset(calendar.getTime(), null,
											pembayaranGaji.getBulan(), pembayaranGaji.getTahun());
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}
								pembayaranItemGajiPegawaiTreeModel.setLunas(calendar.getTime());
							}
						}
					}

					label.setValue("");
									} finally {
						ais.database.hibernate.HibernateUtil.closeSession();
					}
				}
			}).start();

		}

		return true;
	}

//	public Boolean check() {
//
//		Integer kotaCount = null;
//		Session session = HibernateUtil.currentSession();
//		kotaCount = ((Number) session.createCriteria(PembayaranGaji.class).setProjection(Projections.rowCount())
//				.add(Restrictions.eq("tahun", thnByr.getValue()))
//				.add(Restrictions.eq("bulan", bulanByr.getSelectedItem().getValue()))
//				.add(Restrictions.eq("caraPembayaranGaji", caraBayarByr.getSelectedItem().getValue()))
//				.add(this.pembayaranGaji == null || this.pembayaranGaji.getId() == null
//						? Restrictions.sqlRestriction("1=1")
//						: Restrictions.ne("id", this.pembayaranGaji.getId()))
//				.uniqueResult()).intValue();
//
//		return !kotaCount.equals(0);
//	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop, MyToolbarbuttonConfig save,
			final EventListener setujuiData) throws Exception {
		pembayaranGaji = (PembayaranGaji) generalValueObject;

		if (pembayaranGaji != null && pembayaranGaji.getId() != null) {
			StandingInstruction.simpanPembayaranGajiPunyaPegawai(pembayaranGaji);
		}

		this.disposisiSop = disposisiSop;
		MyGrid grid = new MyGrid();
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

		tanggalPembuatan = new MyDatebox(pembayaranGaji.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
				: pembayaranGaji.getTanggalPembuatan());

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		if (pembayaranGaji.getKode() == null || pembayaranGaji.getKode().trim().isEmpty()) {
			String noAgenda = generateCode(false);
			pembayaranGaji.setKode(noAgenda);
		}
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		kode = new Label(pembayaranGaji.getKode());
		if (persetujuan) {
			row.appendChild(new Label(pembayaranGaji.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan"));

		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat51.get()
					.format(pembayaranGaji.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
							: pembayaranGaji.getTanggalPembuatan())));
		} else {
			row.appendChild(tanggalPembuatan);
		}

		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setWidth("90%");

		workspace = new AmbilDataWorkspaceBanbox(false);

		final MyFormRow rowSatker = new MyFormRow();
		rowSatker.setParent(rows);
		rowSatker.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerjaBox = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerjaBox
				.setValue(pembayaranGaji.getSatuanKerja() == null ? "" : pembayaranGaji.getSatuanKerja().getNama());
		satuanKerjaBox.setAttribute("satuanKerja", pembayaranGaji.getSatuanKerja());
		satuanKerjaBox.setReadonly(true);
		if (persetujuan) {
			rowSatker.appendChild(new Label(
					pembayaranGaji.getSatuanKerja() == null ? "" : pembayaranGaji.getSatuanKerja().getNama()));
		} else {
			rowSatker.appendChild(satuanKerjaBox);
		}
		satuanKerjaBox.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu dibayar *"));
		row.appendChild(waktuBayar = new MyDatebox(pembayaranGaji.getWaktuBayar()));
		waktuBayar.setAttribute("janganDisabled", true);
		waktuBayar.setReadonly(true);

		bulanByr = new Combobox();
		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulanByr.appendChild(comboitem);
		}

		Common.selectComboItem(bulanByr, pembayaranGaji.getBulan());
		bulanByr.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bulan *"));
		if (pembayaranGaji.getId() == null) {
			row.appendChild(bulanByr);
		} else {
			row.appendChild(new Label(Common.BULAN[pembayaranGaji.getBulan() - 1]));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));

		thnByr = new Intbox(pembayaranGaji.getTahun());
		if (pembayaranGaji.getId() == null) {
			row.appendChild(thnByr);
		} else {
			row.appendChild(new Label(pembayaranGaji.getTahun().toString()));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cara Bayar *"));
		row.appendChild(caraBayarByr = new Combobox());
		caraBayarByr.setReadonly(true);

		EventListener eventListenerSatuanKerja = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				SatuanKerja satuanKerja = (SatuanKerja) satuanKerjaBox.getAttribute("satuanKerja");

				Common.insertCombo(caraBayarByr, "nama", "akun", CaraPembayaranGaji.class, Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.and(Restrictions.isNotNull("akun"),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));
				Common.selectComboItem(true, caraBayarByr,
						pembayaranGaji.getCaraPembayaranGaji() == null ? caraPembayaranGajiDefault
								: pembayaranGaji.getCaraPembayaranGaji());
			}
		};

		satuanKerjaBox.setEventListener(eventListenerSatuanKerja);

		eventListenerSatuanKerja.onEvent(null);

		final MyFormRow rowAnggaran = new MyFormRow();
		rowAnggaran.setParent(rows);
		rowAnggaran.appendChild(new ais.ui.util.MyLabelConfig("Anggaran"));
		workspace.setValue(pembayaranGaji.getWorkspace() == null ? "" : pembayaranGaji.getWorkspace().toString());
		workspace.setAttribute("workspace", pembayaranGaji.getWorkspace());
		workspace.setWidth("90%");
		workspace.setReadonly(true);

		if (persetujuan) {
			rowAnggaran.appendChild(
					new Label(pembayaranGaji.getWorkspace() == null ? "" : pembayaranGaji.getWorkspace().toString()));
		} else {
			rowAnggaran.appendChild(workspace);
		}

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampilkan_tanpa_anggaran"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		tanpaAnggaran = new MyCheckboxConfig("Merupakan tanpa anggaran");
		if (persetujuan) {
			row.appendChild(
					new Label("Merupakan tanpa anggaran ? " + (pembayaranGaji.getTanpaAnggaran() ? "Ya" : "Tidak")));
		} else {
			row.appendChild(tanpaAnggaran);
		}

		tanpaAnggaran.setChecked(pembayaranGaji.getTanpaAnggaran());

		EventListener tanpaAnggaranEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowAnggaran.setVisible(!tanpaAnggaran.isChecked());

			}
		};
		tanpaAnggaran.addEventListener("onClick", tanpaAnggaranEventListener);
		tanpaAnggaranEventListener.onEvent(null);

		if (pembayaranGaji.getId() != null) {
			Common.freezeGanti(rows, true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pembayaranGaji.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		MyFormRow rowData = new MyFormRow();
		rowData.setValign("top");
		rowData.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowData, "2");
		rowData.appendChild(new PembayaranGajiPunyaPegawaiHelper(gridPembayaranGaji = new MyGrid(), persetujuan)
				.display(pembayaranGaji, bulanByr, thnByr));

		row = new MyFormRow();
		row.setVisible(persetujuan && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Penerimaan"));
		row.appendChild(setujui = new MyCheckboxConfig("Setujui Penerimaan Barang / Jasa ini"));
		setujui.setChecked(pembayaranGaji.getDisetujuiOleh() != null);

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox && setujui != arg0.getTarget()) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("selesai");
					if (selesai != null && selesai) {
						setujui.setChecked(true);
						setujui.setDisabled(true);
					} else {
						setujui.setChecked(false);
						setujui.setDisabled(false);
					}
				}
			}
		});

		if (setujuiData != null) {
			setujui.addEventListener("onClick", setujuiData);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setujuiData.onEvent(new Event("", null, pembayaranGaji.getDisetujuiOleh() != null));
				}
			});
		}

		return grid;
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pembayaran Gaji";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return pembayaranGaji;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PembayaranGaji.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	private String generateCode(boolean tambah) {
		if (NomorSuratAlurPengadaan.PEMBAYARAN_GAJI_PEGAWAI == null
				|| NomorSuratAlurPengadaan.PEMBAYARAN_GAJI_PEGAWAI.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PEMBAYARAN_GAJI_PEGAWAI.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PEMBAYARAN_GAJI_PEGAWAI.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PEMBAYARAN_GAJI_PEGAWAI.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PEMBAYARAN_GAJI_PEGAWAI.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurPengadaan.PEMBAYARAN_GAJI_PEGAWAI.getNomorSurat().format(index,
				tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue());
		return noAgenda;
	}

	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PembayaranGaji.class)
				.createAlias("nomorSuratAlurPengadaan", "nomorSuratAlurPengadaan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurPengadaan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurPengadaan.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang)) || nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
