package ais.action.master.kpi;

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
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
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
import ais.action.master.kpi.helper.PenilaianKpiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.kpi.PengajuanKpi;
import ais.database.model.kpi.PenilaianKpi;
import ais.database.model.rab.SatuanKerja;
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

public class PengajuanKpiAction extends GenericAutowireComposer implements DataInitDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyGrid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private MyTextbox searchnamaPegawai;

	private Combobox searchtahun;
	private boolean persetujuan = false;

	private MyWindow addWindow;
	private PengajuanKpi pengajuanKpi;
	private Textbox nama;
	private Textbox keterangan;

	private boolean delete = false;
	private boolean edit = false;
	private boolean approve = false;
	private boolean reject = false;

	private AmbilDataSatuanKerjaBanbox satuanKerjaBox;
	private MyCheckboxConfig setujui;
	private MyDatebox tanggalPembuatan;
	private Label kode;
	private MyGrid gridPengajuanKpi;
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

		Common.generateTahunAjaran(searchtahun);

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
	}

	class PengajuanKpiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengajuanKpi pengajuanKpi = (PengajuanKpi) arg1;

			RevisiHelper.createNewRevisi(PengajuanKpi.class, pengajuanKpi, pengajuanKpi.getNama()).setParent(arg0);
			new Label(pengajuanKpi.getTa() + "").setParent(arg0);
			new Label(pengajuanKpi.getSatuanKerja() == null ? "" : pengajuanKpi.getSatuanKerja().getNama())
					.setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(pengajuanKpi.getDibuatOleh() == null ? "" : pengajuanKpi.getDibuatOleh().getUserNama())
					.setParent(a);
			new MyLabelAgakKecil(pengajuanKpi.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(pengajuanKpi.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh = new MyLabelAgakKecil(
					pengajuanKpi.getDisetujuiOleh() == null ? "" : pengajuanKpi.getDisetujuiOleh().getUserNama());

			final MyLabelAgakKecil disetujuiTanggal = new MyLabelAgakKecil(
					pengajuanKpi.getTanggalPersetujuan() == null ? ""
							: Common.dateFormat3.get().format(pengajuanKpi.getTanggalPersetujuan()));

			final MyLabelAgakKecilBoldMerah ditolakOleh = new MyLabelAgakKecilBoldMerah(
					pengajuanKpi.getDitolakOleh() == null ? ""
							: "Ditolak oleh " + pengajuanKpi.getDitolakOleh().getUserNama());
			final MyLabelAgakKecilBoldMerah tanggalDitolak = new MyLabelAgakKecilBoldMerah(
					pengajuanKpi.getTanggalDitolak() == null ? ""
							: Common.dateFormat3.get().format(pengajuanKpi.getTanggalDitolak()));

			ditolakOleh.setParent(a);
			tanggalDitolak.setParent(a);
			disetujuiOleh.setParent(a);
			disetujuiTanggal.setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(pengajuanKpi.getKeterangan())).setParent(vbox1);
			if (pengajuanKpi.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pengajuanKpi.getDisposisiSop().getKeterangan() + " ("
						+ pengajuanKpi.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pengajuanKpi.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			if (pengajuanKpi.getDisposisiSop() != null && !pengajuanKpi.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (pengajuanKpi.getDisetujuiOleh() == null && edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(pengajuanKpi.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pengajuanKpi.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(pengajuanKpi);
					}
				});
			} else {
				new Label(pengajuanKpi.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			Hbox hboxCed = Common.copyEditDeleteButtons(edit, delete, pengajuanKpi, PengajuanKpiAction.this);
			aksiButtons.addAll(ais.ui.util.UIHelper.ambilItemAksi(hboxCed));

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");
			final MyToolbarbuttonConfig ditolak = new MyToolbarbuttonConfig("", "/img/svg/deny.svg");
			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");

			disetujui.setVisible(
					approve && pengajuanKpi.getDisetujuiOleh() == null && pengajuanKpi.getDitolakOleh() == null);
			ditolak.setVisible(
					reject && pengajuanKpi.getDisetujuiOleh() == null && pengajuanKpi.getDitolakOleh() == null);

			dibatalkan.setVisible(
					reject && (pengajuanKpi.getDisetujuiOleh() != null || pengajuanKpi.getDitolakOleh() != null));

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui pengajuan KPI ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pengajuanKpi.setDisetujuiOleh(Common.getCurrentUser());
										pengajuanKpi.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, pengajuanKpi);

										disetujuiTanggal.setValue(pengajuanKpi.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(pengajuanKpi.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pengajuanKpi.getDisetujuiOleh() == null ? ""
												: pengajuanKpi.getDisetujuiOleh().getUserNama());

										ditolakOleh.setValue(pengajuanKpi.getDitolakOleh() == null ? ""
												: "Ditolak oleh " + pengajuanKpi.getDitolakOleh().getUserNama());
										tanggalDitolak.setValue(pengajuanKpi.getTanggalDitolak() == null ? ""
												: Common.dateFormat3.get().format(pengajuanKpi.getTanggalDitolak()));

										disetujui.setVisible(approve && pengajuanKpi.getDisetujuiOleh() == null
												&& pengajuanKpi.getDitolakOleh() == null);
										ditolak.setVisible(reject && pengajuanKpi.getDisetujuiOleh() == null
												&& pengajuanKpi.getDitolakOleh() == null);
										dibatalkan.setVisible(reject && (pengajuanKpi.getDisetujuiOleh() != null
												|| pengajuanKpi.getDitolakOleh() != null));

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
					MyMessageboxConfig.show("Apakah yakin ingin menolak pengajuan KPI ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();
										pengajuanKpi.setDitolakOleh(Common.getCurrentUser());
										pengajuanKpi.setTanggalDitolak(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, pengajuanKpi);

										ditolakOleh.setValue(pengajuanKpi.getDitolakOleh() == null ? ""
												: "Ditolak oleh " + pengajuanKpi.getDitolakOleh().getUserNama());
										tanggalDitolak.setValue(pengajuanKpi.getTanggalDitolak() == null ? ""
												: Common.dateFormat3.get().format(pengajuanKpi.getTanggalDitolak()));

										disetujuiTanggal.setValue(pengajuanKpi.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(pengajuanKpi.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pengajuanKpi.getDisetujuiOleh() == null ? ""
												: pengajuanKpi.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && pengajuanKpi.getDisetujuiOleh() == null
												&& pengajuanKpi.getDitolakOleh() == null);
										ditolak.setVisible(reject && pengajuanKpi.getDisetujuiOleh() == null
												&& pengajuanKpi.getDitolakOleh() == null);
										dibatalkan.setVisible(reject && (pengajuanKpi.getDisetujuiOleh() != null
												|| pengajuanKpi.getDitolakOleh() != null));

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

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan pengajuan KPI ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pengajuanKpi.setDisetujuiOleh(null);
										pengajuanKpi.setTanggalPersetujuan(null);
										pengajuanKpi.setDitolakOleh(null);
										pengajuanKpi.setTanggalDitolak(null);

										Common.refreshUpdate(session, pengajuanKpi);

										disetujuiTanggal.setValue(pengajuanKpi.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(pengajuanKpi.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pengajuanKpi.getDisetujuiOleh() == null ? ""
												: pengajuanKpi.getDisetujuiOleh().getUserNama());

										ditolakOleh.setValue(pengajuanKpi.getDitolakOleh() == null ? ""
												: "Ditolak oleh " + pengajuanKpi.getDitolakOleh().getUserNama());
										tanggalDitolak.setValue(pengajuanKpi.getTanggalDitolak() == null ? ""
												: Common.dateFormat3.get().format(pengajuanKpi.getTanggalDitolak()));

										disetujui.setVisible(approve && pengajuanKpi.getDisetujuiOleh() == null
												&& pengajuanKpi.getDitolakOleh() == null);

										ditolak.setVisible(reject && pengajuanKpi.getDisetujuiOleh() == null
												&& pengajuanKpi.getDitolakOleh() == null);
										dibatalkan.setVisible(reject && (pengajuanKpi.getDisetujuiOleh() != null
												|| pengajuanKpi.getDitolakOleh() != null));

									}
								}
							});
				}

			});
			aksiButtons.add(dibatalkan);

			// Susun semua tombol: max 3 per baris, rata tengah
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PengajuanKpi> pengajuanKpi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pengajuanKpi);
		grid.setRowRenderer(new PengajuanKpiRenderer());
		grid.setModelCheckMobile(strset);

		grid.renderAll();

	}

	private Checkbox searchaktif;
	private Combobox ta;

	@SuppressWarnings("unchecked")
	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> dataPeg = new ArrayList<Long>();
		if (!searchnamaPegawai.getValue().trim().isEmpty()) {
			dataPeg = session.createCriteria(PenilaianKpi.class).add(Restrictions.isNotNull("pengajuanKpi"))
					.setProjection(Projections.groupProperty("pengajuanKpi.id")).createAlias("pegawai", "pegawai")
					.add((searchnamaPegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.or(
							Restrictions.ilike("pegawai.nama", searchnamaPegawai.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("pegawai.mycode", searchnamaPegawai.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("pegawai.code", searchnamaPegawai.getValue().trim(),
											MatchMode.ANYWHERE)))

					)).list();
		}

		Criteria criteria = session.createCriteria(PengajuanKpi.class)

				.add((searchnamaPegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (!searchnamaPegawai.getValue().trim().isEmpty() && dataPeg.isEmpty()
						? Restrictions.sqlRestriction("false")
						: dataPeg.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", dataPeg)))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

		;
		if (order)
			criteria.addOrder(Order.desc("tanggalPembuatan")).addOrder(Order.desc("id"));
		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("keterangan", searchnama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchtahun.getSelectedItem() == null || searchtahun.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("ta", searchtahun.getSelectedItem().getValue()));
		return criteria;
	}

	public void onAdd(Event event) throws Exception {
		init(new PengajuanKpi());
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		// TODO Auto-generated method stub
		this.pengajuanKpi = (PengajuanKpi) obj;

		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");

		disposisiSop=null;center.appendChild(form(obj, null, save, null));

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
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, judul pengajuan KPI belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Judul dengan nama pengajuan yang sesuai; (2) pastikan kolom tidak kosong; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (ta.getSelectedItem() == null || ta.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, tahun akademik belum dipilih. Langkah yang dapat dilakukan: (1) pilih tahun akademik dari dropdown yang tersedia; (2) pastikan tahun akademik aktif sudah dikonfigurasi; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (satuanKerjaBox.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Mohon maaf, satuan kerja belum dipilih. Langkah yang dapat dilakukan: (1) pilih satuan kerja dari dropdown atau kolom pencarian; (2) pastikan data satuan kerja sudah dikonfigurasi; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		List<Row> rowsMasterAsset = gridPengajuanKpi.getRows().getChildren();
		for (Row row : rowsMasterAsset) {
			PenilaianKpi penilaianKpi = (PenilaianKpi) row.getAttribute("penilaianKpi");
			if (penilaianKpi == null || penilaianKpi.getPegawai() == null) {
				MyMessageboxConfig.show("Data pegawai harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		try {
			if (pengajuanKpi != null && pengajuanKpi.getId() != null) {
				pengajuanKpi = (PengajuanKpi) session.load(PengajuanKpi.class, pengajuanKpi.getId());

			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/PengajuanKpiAction.java:515");
			// TODO: handle exception
		}

		try {
			if (pengajuanKpi == null) {
				pengajuanKpi = new PengajuanKpi();
			}
		} catch (Exception e) {
			pengajuanKpi = new PengajuanKpi();
		}

		pengajuanKpi.setNama(nama.getValue().trim());
		pengajuanKpi.setKeterangan(keterangan.getValue());
		pengajuanKpi.setTa((String) ta.getSelectedItem().getValue());
		pengajuanKpi.setSatuanKerja((SatuanKerja) satuanKerjaBox.getAttribute("satuanKerja"));
		pengajuanKpi.setTanggalPembuatan(tanggalPembuatan.getValue());

		String p = "";
		for (Row row : rowsMasterAsset) {
			PenilaianKpi penilaianKpi = (PenilaianKpi) row.getAttribute("penilaianKpi");
			p += p.isEmpty() ? penilaianKpi.getId() + "" : "," + penilaianKpi.getId();
		}
		pengajuanKpi.setPenilaianKpis(p);

		if (disposisiSop != null && disposisiSop.getId() != null) {
			pengajuanKpi.setDisposisiSop(disposisiSop);
		}

		if (pengajuanKpi.getId() != null) {
			Common.refreshUpdate(session, pengajuanKpi);
		} else {
			pengajuanKpi.setDibuatOleh(Common.getCurrentUser());
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			pengajuanKpi.setKode(kode.getValue());
			session.save(pengajuanKpi);
			session.flush();
		}

		for (Row row : rowsMasterAsset) {
			PenilaianKpi penilaianKpi = (PenilaianKpi) row.getAttribute("penilaianKpi");
			penilaianKpi.setPengajuanKpi(pengajuanKpi);
			Common.refreshSaveOrUpdate(session, penilaianKpi);
		}

		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop, MyToolbarbuttonConfig save,
			final EventListener setujuiData) throws Exception {
		pengajuanKpi = (PengajuanKpi) generalValueObject;
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

		tanggalPembuatan = new MyDatebox(pengajuanKpi.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
				: pengajuanKpi.getTanggalPembuatan());

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		if (pengajuanKpi.getKode() == null || pengajuanKpi.getKode().trim().isEmpty()) {
			String noAgenda = generateCode(false);
			pengajuanKpi.setKode(noAgenda);
		}
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		kode = new Label(pengajuanKpi.getKode());
		if (persetujuan) {
			row.appendChild(new Label(pengajuanKpi.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul *"));
		nama = new Textbox(pengajuanKpi.getNama());
		if (persetujuan) {
			row.appendChild(new Label(pengajuanKpi.getNama()));
		} else {
			row.appendChild(nama);
		}
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan"));

		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat51.get()
					.format(pengajuanKpi.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
							: pengajuanKpi.getTanggalPembuatan())));
		} else {
			row.appendChild(tanggalPembuatan);
		}

		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setWidth("90%");

		final MyFormRow rowSatker = new MyFormRow();
		rowSatker.setParent(rows);
		rowSatker.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerjaBox = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerjaBox.setValue(pengajuanKpi.getSatuanKerja() == null ? "" : pengajuanKpi.getSatuanKerja().getNama());
		satuanKerjaBox.setAttribute("satuanKerja", pengajuanKpi.getSatuanKerja());
		satuanKerjaBox.setReadonly(true);
		if (persetujuan || pengajuanKpi.getId() != null) {
			rowSatker.appendChild(
					new Label(pengajuanKpi.getSatuanKerja() == null ? "" : pengajuanKpi.getSatuanKerja().getNama()));
		} else {
			rowSatker.appendChild(satuanKerjaBox);
		}
		satuanKerjaBox.setWidth("90%");

		final MyFormRow rowAnggaran = new MyFormRow();
		rowAnggaran.setParent(rows);
		rowAnggaran.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		ta = new Combobox();
		Common.generateTahunAjaran(ta);
		Common.selectComboItem(ta, pengajuanKpi.getTa());
		ta.setReadonly(true);

		if (persetujuan || pengajuanKpi.getId() != null) {
			rowAnggaran.appendChild(new Label(pengajuanKpi.getTa()));
		} else {
			rowAnggaran.appendChild(ta);
		}

		ta.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ta.setDisabled(true);
			}
		});

		satuanKerjaBox.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				satuanKerjaBox.setDisabled(true);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pengajuanKpi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		MyFormRow rowData = new MyFormRow();
		rowData.setValign("top");
		rowData.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowData, "2");
		rowData.appendChild(new PenilaianKpiHelper(gridPengajuanKpi = new MyGrid(), persetujuan).display(pengajuanKpi,
				ta, satuanKerjaBox));

		row = new MyFormRow();
		row.setVisible(persetujuan && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Penerimaan"));
		row.appendChild(setujui = new MyCheckboxConfig("Setujui Penerimaan Barang / Jasa ini"));
		setujui.setChecked(pengajuanKpi.getDisetujuiOleh() != null);

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
					setujuiData.onEvent(new Event("", null, pengajuanKpi.getDisetujuiOleh() != null));
				}
			});
		}

		return grid;
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan KPI";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return pengajuanKpi;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PengajuanKpi.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	private String generateCode(boolean tambah) {
		if (NomorSuratAlurPengadaan.PENGAJUAN_KPI_PEGAWAI == null
				|| NomorSuratAlurPengadaan.PENGAJUAN_KPI_PEGAWAI.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PENGAJUAN_KPI_PEGAWAI.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PENGAJUAN_KPI_PEGAWAI.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PENGAJUAN_KPI_PEGAWAI.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PENGAJUAN_KPI_PEGAWAI.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurPengadaan.PENGAJUAN_KPI_PEGAWAI.getNomorSurat().format(index,
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
		Number indexO = (Number) session.createCriteria(PengajuanKpi.class)
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
