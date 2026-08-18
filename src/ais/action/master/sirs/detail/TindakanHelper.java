package ais.action.master.sirs.detail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.sirs.helper.AmbilDataTindakanBanyak;
import ais.action.master.sirs.util.CommonTarifTindakan;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.OnSave;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.BiayaTindakanPerKelas;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.PaketPerawatanDetail;
import ais.database.model.sirs.Tindakan;
import ais.database.model.sirs.TindakanDiagnosaPenyakit;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class TindakanHelper {

	private Grid gridTindakan;

	private DiagnosaPenyakit diagnosaPenyakit;

	private boolean delete = false;

	private Paging paging;
	private OnSave onSave;
	private Toolbarbutton save;

	private North north;

	public TindakanHelper(OnSave onSave, Toolbarbutton save) {
		this.save = save;
		this.onSave = onSave;
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
	}

	public Borderlayout init(DiagnosaPenyakit diagnosaPenyakit) {
		this.diagnosaPenyakit = diagnosaPenyakit;
		return display();
	}

	public Borderlayout display() {

		Borderlayout borderlayout = new Borderlayout();

		north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(north);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Data Tindakan dan Perawatan", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				if (diagnosaPenyakit.getId() == null)
					if (!onSave.onSave(event)) {
						return;
					}

				AmbilDataTindakanBanyak ambilDataTindakanBanyak = new AmbilDataTindakanBanyak(
						new ArrayList<Tindakan>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataTindakanBanyak);
				ambilDataTindakanBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						List<Tindakan> tindakans = (List<Tindakan>) arg0.getData();

						save.setDisabled(tindakans.size() == 0);

						Session session = HibernateUtil.currentSession();
						for (Tindakan tindakan : tindakans) {

							KelasPerawatan kelasPerawatan = diagnosaPenyakit.getPendaftaran()
									.getKelasPerawatan() == null ? ConstantValues.kelasNormal
											: diagnosaPenyakit.getPendaftaran().getKelasPerawatan();

							BiayaTindakanPerKelas biayaTindakanPerKelas = CommonTarifTindakan
									.getBiayaTindakanPerKelas(tindakan, kelasPerawatan);

							TindakanDiagnosaPenyakit tindakanDiagnosaPenyakit = new TindakanDiagnosaPenyakit();
							tindakanDiagnosaPenyakit.setTindakan(tindakan);
							tindakanDiagnosaPenyakit.setJumlah(1.0);
							tindakanDiagnosaPenyakit.setBiaya(
									biayaTindakanPerKelas == null || biayaTindakanPerKelas.getBiaya() == null ? 0.0
											: biayaTindakanPerKelas.getBiaya());

							tindakanDiagnosaPenyakit
									.setKelasPerawatan(diagnosaPenyakit.getPendaftaran().getKelasPerawatan() == null
											? ConstantValues.kelasNormal
											: diagnosaPenyakit.getPendaftaran().getKelasPerawatan());
							tindakanDiagnosaPenyakit.setKeterangan("");
							tindakanDiagnosaPenyakit.setDiagnosaPenyakit(diagnosaPenyakit);
							session.save(tindakanDiagnosaPenyakit);

						}

						loadData(null);
					}
				});
				ambilDataTindakanBanyak.setWidth("750px");
				ambilDataTindakanBanyak.setHeight("97%");
				ambilDataTindakanBanyak.setVisible(true);
				ambilDataTindakanBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		gridTindakan = new Grid();
		gridTindakan.setMold("paging");
		gridTindakan.setPageSize(25);
		gridTindakan.setParent(center);

		Columns columns = new Columns();

		columns.setParent(gridTindakan);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Tindakan");
		column.setWidth("40%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Waktu");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");
		loadData(null);
		return borderlayout;
	}

	class TindakanDiagnosaPenyakitRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final TindakanDiagnosaPenyakit tindakanDiagnosaPenyakit = (TindakanDiagnosaPenyakit) arg1;
			final Tindakan tindakan = tindakanDiagnosaPenyakit.getTindakan();

			new Label(tindakan.getNama()).setParent(arg0);

			final MyDoublebox jumlah;
			jumlah = new MyDoublebox(
					tindakanDiagnosaPenyakit.getJumlah() == null ? 1.0 : tindakanDiagnosaPenyakit.getJumlah());
			jumlah.setParent(arg0);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					tindakanDiagnosaPenyakit.setJumlah(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
					Common.refreshUpdate(session, (tindakanDiagnosaPenyakit));

				}
			});

			final MyDatebox tanggal;
			tanggal = new MyDatebox(tindakanDiagnosaPenyakit.getTanggal());
			tanggal.setParent(arg0);
			tanggal.setStyle("text-align:right");
			tanggal.setWidth("90%");
			tanggal.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					tindakanDiagnosaPenyakit.setTanggal(tanggal.getValue());
					Common.refreshUpdate(session, (tindakanDiagnosaPenyakit));

				}
			});

			final MyTextbox keterangan = new MyTextbox(
					tindakanDiagnosaPenyakit.getKeterangan() == null ? "" : tindakanDiagnosaPenyakit.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setParent(arg0);

			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					tindakanDiagnosaPenyakit.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (tindakanDiagnosaPenyakit));
				}
			});

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui, data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(tindakanDiagnosaPenyakit);
											loadData(null);
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/TindakanHelper.java:274");
											MyMessageboxConfig.show(Common.pesan(
																"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih terkait dengan data ini; (2) hapus terlebih dahulu seluruh data yang berelasi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.",
																	e.getMessage()));
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	@SuppressWarnings("unchecked")
	public boolean setPaket(Set<Tindakan> pakets, DiagnosaPenyakit diagnosaPenyakit) throws Exception {
		this.diagnosaPenyakit = diagnosaPenyakit;
		if (north != null) {
			north.setVisible(true);
			Common.freeze(gridTindakan, false);
		}

		if (pakets.isEmpty()) {
			return true;
		}
		Session session = HibernateUtil.currentSession();
		List<Tindakan> tindakans = new ArrayList<Tindakan>();

		for (Tindakan tindakan : pakets) {
			List<PaketPerawatanDetail> paketPerawatanDetails = session.createCriteria(PaketPerawatanDetail.class)
					.add(Restrictions.eq("paketPerawatan", tindakan)).list();
			for (PaketPerawatanDetail paketPerawatanDetail : paketPerawatanDetails) {
				if (paketPerawatanDetail.getTindakan() != null) {
					tindakans.add(paketPerawatanDetail.getTindakan());
				}
			}
		}
		if (tindakans.isEmpty()) {
			return true;
		}

		if (diagnosaPenyakit.getId() == null)
			if (!onSave.onSave(null)) {
				return false;
			}

		List<TindakanDiagnosaPenyakit> tindakanDiagnosaPenyakits = new ArrayList<TindakanDiagnosaPenyakit>();
		for (Tindakan paket : pakets) {
			List<PaketPerawatanDetail> paketPerawatanDetails = session.createCriteria(PaketPerawatanDetail.class)
					.add(Restrictions.eq("paketPerawatan", paket)).list();
			for (PaketPerawatanDetail paketPerawatanDetail : paketPerawatanDetails) {
				if (paketPerawatanDetail.getTindakan() != null) {
					Tindakan tindakan = paketPerawatanDetail.getTindakan();
					TindakanDiagnosaPenyakit tindakanDiagnosaPenyakit = (TindakanDiagnosaPenyakit) session
							.createCriteria(TindakanDiagnosaPenyakit.class).add(Restrictions.eq("tindakan", tindakan))
							.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit)).setMaxResults(1).uniqueResult();
					if (tindakanDiagnosaPenyakit == null) {
						tindakanDiagnosaPenyakit = new TindakanDiagnosaPenyakit();
						tindakanDiagnosaPenyakit.setTindakan(tindakan);
						tindakanDiagnosaPenyakit.setJumlah(paketPerawatanDetail.getJumlah());
						tindakanDiagnosaPenyakit.setKeterangan(paketPerawatanDetail.getKeterangan());
						tindakanDiagnosaPenyakit.setDiagnosaPenyakit(diagnosaPenyakit);
						session.save(tindakanDiagnosaPenyakit);
					}
					tindakanDiagnosaPenyakits.add(tindakanDiagnosaPenyakit);
				}
			}
		}

		if (north != null) {
			ListModel strset = new SimpleListModel(tindakanDiagnosaPenyakits);
			gridTindakan.setRowRenderer(new TindakanDiagnosaPenyakitRenderer());
			gridTindakan.setModel(strset);
			gridTindakan.renderAll();

			Common.freeze(gridTindakan, true);
			north.setVisible(false);
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Event event) {
		Session session = HibernateUtil.currentSession();
		List<TindakanDiagnosaPenyakit> tindakanDiagnosaPenyakits = diagnosaPenyakit == null
				|| diagnosaPenyakit.getId() == null
						? new ArrayList<TindakanDiagnosaPenyakit>()
						: session.createCriteria(TindakanDiagnosaPenyakit.class).addOrder(Order.desc("id"))
								.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit))
								.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
								.setFirstResult(
										Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
								.list();
		ListModel strset = new SimpleListModel(tindakanDiagnosaPenyakits);
		gridTindakan.setRowRenderer(new TindakanDiagnosaPenyakitRenderer());
		gridTindakan.setModel(strset);
		gridTindakan.renderAll();

	}

}
