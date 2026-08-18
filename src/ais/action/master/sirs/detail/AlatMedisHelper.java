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
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.sirs.helper.AmbilDataAlatMedisBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.OnSave;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.AlatMedisDiagnosaPenyakit;
import ais.database.model.sirs.BiayaAlatMedisPerKelas;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.PaketPerawatanDetail;
import ais.database.model.sirs.Tindakan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class AlatMedisHelper {

	private Grid gridAlatMedis;

	private DiagnosaPenyakit diagnosaPenyakit;
	private boolean delete = false;

	private Paging paging;
	private OnSave onSave;
	private Toolbarbutton save;

	private North north;

	public AlatMedisHelper(OnSave onSave, Toolbarbutton save) {
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
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Data Alat Medis", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				if (diagnosaPenyakit.getId() == null)
					if (!onSave.onSave(event)) {
						return;
					}

				AmbilDataAlatMedisBanyak ambilDataAlatMedisBanyak = new AmbilDataAlatMedisBanyak(
						new ArrayList<AlatMedis>(), AlatMedis.JENIS_UMUM);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataAlatMedisBanyak);
				ambilDataAlatMedisBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						List<AlatMedis> alatMediss = (List<AlatMedis>) arg0.getData();

						save.setDisabled(alatMediss.size() == 0);

						Session session = HibernateUtil.currentSession();
						for (AlatMedis alatMedis : alatMediss) {

							KelasPerawatan kelasPerawatan = diagnosaPenyakit.getPendaftaran()
									.getKelasPerawatan() == null ? ConstantValues.kelasNormal
											: diagnosaPenyakit.getPendaftaran().getKelasPerawatan();

							BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = (BiayaAlatMedisPerKelas) session
									.createCriteria(BiayaAlatMedisPerKelas.class)
									.add(Restrictions.eq("alatMedis", alatMedis))
									.add(Restrictions.eq("kelasPerawatan", kelasPerawatan)).setMaxResults(1)
									.uniqueResult();

							if (biayaAlatMedisPerKelas == null) {
								MyMessageboxConfig.showFormat(
										"Mohon maaf, biaya untuk layanan \"{V1}\" pada kelas \"{V2}\" belum dimasukkan. Langkah yang dapat dilakukan: (1) lengkapi terlebih dahulu data biaya layanan tersebut pada kelas yang bersangkutan; (2) kemudian ulangi kembali proses ini.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, alatMedis.getNama(), kelasPerawatan.getNama());
								continue;
							}

							AlatMedisDiagnosaPenyakit alatMedisDiagnosaPenyakit = new AlatMedisDiagnosaPenyakit();
							alatMedisDiagnosaPenyakit.setAlatMedis(alatMedis);
							alatMedisDiagnosaPenyakit.setJumlah(1.0);
							alatMedisDiagnosaPenyakit.setBiaya(
									biayaAlatMedisPerKelas == null || biayaAlatMedisPerKelas.getBiaya() == null ? 0.0
											: biayaAlatMedisPerKelas.getBiaya());

							alatMedisDiagnosaPenyakit
									.setKelasPerawatan(diagnosaPenyakit.getPendaftaran().getKelasPerawatan() == null
											? ConstantValues.kelasNormal
											: diagnosaPenyakit.getPendaftaran().getKelasPerawatan());
							alatMedisDiagnosaPenyakit.setKeterangan("");
							alatMedisDiagnosaPenyakit.setDiagnosaPenyakit(diagnosaPenyakit);
							session.save(alatMedisDiagnosaPenyakit);

						}

						loadData(null);
					}
				});
				ambilDataAlatMedisBanyak.setWidth("750px");
				ambilDataAlatMedisBanyak.setHeight("97%");
				ambilDataAlatMedisBanyak.setVisible(true);
				ambilDataAlatMedisBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		paging.setParent(south);

		gridAlatMedis = new Grid();
		gridAlatMedis.setMold("paging");
		gridAlatMedis.setPageSize(25);
		gridAlatMedis.setParent(center);

		Columns columns = new Columns();

		columns.setParent(gridAlatMedis);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Alat Medis");
		column.setWidth("40%");

		column = new Column();
		column.setParent(columns);
		column.setVisible(false);
		column.setLabel("Jumlah alat medis");
		column.setAlign("right");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setVisible(false);
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

	class AlatMedisDiagnosaPenyakitRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final AlatMedisDiagnosaPenyakit alatMedisDiagnosaPenyakit = (AlatMedisDiagnosaPenyakit) arg1;
			final AlatMedis alatMedis = alatMedisDiagnosaPenyakit.getAlatMedis();

			new Label(alatMedis.getNama()).setParent(arg0);

			final MyDoublebox jumlah;
			jumlah = new MyDoublebox(
					alatMedisDiagnosaPenyakit.getJumlah() == null ? 1.0 : alatMedisDiagnosaPenyakit.getJumlah());
			jumlah.setParent(arg0);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					alatMedisDiagnosaPenyakit.setJumlah(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
					Common.refreshUpdate(session, (alatMedisDiagnosaPenyakit));

				}
			});

			final MyDatebox tanggal;
			tanggal = new MyDatebox(alatMedisDiagnosaPenyakit.getTanggal());
			tanggal.setParent(arg0);
			tanggal.setStyle("text-align:right");
			tanggal.setWidth("90%");
			tanggal.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					alatMedisDiagnosaPenyakit.setTanggal(tanggal.getValue());
					Common.refreshUpdate(session, (alatMedisDiagnosaPenyakit));

				}
			});

			final MyTextbox keterangan = new MyTextbox(
					alatMedisDiagnosaPenyakit.getKeterangan() == null ? "" : alatMedisDiagnosaPenyakit.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setParent(arg0);

			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					alatMedisDiagnosaPenyakit.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (alatMedisDiagnosaPenyakit));
				}
			});

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang sudah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(alatMedisDiagnosaPenyakit);
											loadData(null);
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/AlatMedisHelper.java:293");
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon hubungi administrator sistem. Rincian kesalahan: {V1}"
															, e.getMessage()));
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
	public void loadData(Event event) {
		Session session = HibernateUtil.currentSession();
		List<AlatMedisDiagnosaPenyakit> alatMedisDiagnosaPenyakits = diagnosaPenyakit == null
				|| diagnosaPenyakit.getId() == null
						? new ArrayList<AlatMedisDiagnosaPenyakit>()
						: session.createCriteria(AlatMedisDiagnosaPenyakit.class).addOrder(Order.desc("id"))
								.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit))
								.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
								.setFirstResult(
										Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
								.list();
		ListModel strset = new SimpleListModel(alatMedisDiagnosaPenyakits);
		gridAlatMedis.setRowRenderer(new AlatMedisDiagnosaPenyakitRenderer());
		gridAlatMedis.setModel(strset);
		gridAlatMedis.renderAll();

	}

	@SuppressWarnings("unchecked")
	public boolean setPaket(Set<Tindakan> pakets, DiagnosaPenyakit diagnosaPenyakit) throws Exception {
		this.diagnosaPenyakit = diagnosaPenyakit;

		if (north != null) {
			north.setVisible(true);
			Common.freeze(gridAlatMedis, false);
		}

		if (pakets.isEmpty()) {
			return true;
		}
		Session session = HibernateUtil.currentSession();
		List<AlatMedis> alatMediss = new ArrayList<AlatMedis>();

		for (Tindakan paket : pakets) {
			List<PaketPerawatanDetail> paketPerawatanDetails = session.createCriteria(PaketPerawatanDetail.class)
					.add(Restrictions.eq("paketPerawatan", paket)).list();
			for (PaketPerawatanDetail paketPerawatanDetail : paketPerawatanDetails) {
				if (paketPerawatanDetail.getAlatMedis() != null) {
					alatMediss.add(paketPerawatanDetail.getAlatMedis());
				}
			}
		}
		if (alatMediss.isEmpty()) {
			return true;
		}

		if (diagnosaPenyakit.getId() == null)
			if (!onSave.onSave(null)) {
				return false;
			}

		List<AlatMedisDiagnosaPenyakit> alatMedisDiagnosaPenyakits = new ArrayList<AlatMedisDiagnosaPenyakit>();
		for (Tindakan paket : pakets) {
			List<PaketPerawatanDetail> paketPerawatanDetails = session.createCriteria(PaketPerawatanDetail.class)
					.add(Restrictions.eq("paketPerawatan", paket)).list();
			for (PaketPerawatanDetail paketPerawatanDetail : paketPerawatanDetails) {
				if (paketPerawatanDetail.getAlatMedis() != null) {
					AlatMedis alatMedis = paketPerawatanDetail.getAlatMedis();
					AlatMedisDiagnosaPenyakit alatMedisDiagnosaPenyakit = (AlatMedisDiagnosaPenyakit) session
							.createCriteria(AlatMedisDiagnosaPenyakit.class)
							.add(Restrictions.eq("alatMedis", alatMedis))
							.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit)).setMaxResults(1).uniqueResult();
					if (alatMedisDiagnosaPenyakit == null) {
						alatMedisDiagnosaPenyakit = new AlatMedisDiagnosaPenyakit();
						alatMedisDiagnosaPenyakit.setAlatMedis(alatMedis);
						alatMedisDiagnosaPenyakit.setJumlah(paketPerawatanDetail.getJumlah());
						alatMedisDiagnosaPenyakit.setKeterangan(paketPerawatanDetail.getKeterangan());
						alatMedisDiagnosaPenyakit.setDiagnosaPenyakit(diagnosaPenyakit);
						session.save(alatMedisDiagnosaPenyakit);
					}
					alatMedisDiagnosaPenyakits.add(alatMedisDiagnosaPenyakit);
				}
			}
		}

		if (north != null) {
			ListModel strset = new SimpleListModel(alatMedisDiagnosaPenyakits);
			gridAlatMedis.setRowRenderer(new AlatMedisDiagnosaPenyakitRenderer());
			gridAlatMedis.setModel(strset);
			gridAlatMedis.renderAll();

			Common.freeze(gridAlatMedis, true);
			north.setVisible(false);
		}
		return true;
	}
}