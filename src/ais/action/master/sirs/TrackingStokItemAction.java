package ais.action.master.sirs;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.report.Report;
import ais.action.report.format1.sirs.inventory.LaporanTrackingStokItemWindow;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.library.JenisItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.SatuanItem;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyTextbox;
import ais.action.master.helper.FilterLanjutHelper;

public class TrackingStokItemAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;

	private Grid grid;
	private MyTextbox searchnama;
	private MyTextbox searchkode;
	private MyTextbox searchbarcode;

	private Combobox searchsatuanItem;
	private Combobox searchjenisItem;
	private Combobox searchlokasi;
	private MyDatebox searchperTanggal;

	private Footer stok;
	private Footer nilai;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	private Lokasi myLokasi;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		myLokasi = Common.getCurrentLokasi();
		Common.insertCombo(searchlokasi, "nama", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(searchlokasi, myLokasi);
		// searchlokasi.setDisabled(myLokasi != null);

		if (searchperTanggal != null) { searchperTanggal.setValue(new Date()); }
		Common.insertCombo(searchsatuanItem, "nama", SatuanItem.class);
		Common.insertCombo(searchjenisItem, "nama", JenisItem.class);

		onSearchDefault(null);

	        FilterLanjutHelper.setup(comp);
}

	class JenisBarangRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			Object[] objects = (Object[]) arg1;
			Long itemId = ((Number) objects[0]).longValue();
			Date tanggalTerakhirPengadaan = (Date) objects[2];
			Number stok = (Number) objects[3];
			String gudang = (String) objects[4];
			Number nilai = (Number) objects[5];
			final ItemMedis item = (ItemMedis) ConstantValues.ambil(ItemMedis.class.getName(), itemId);

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						final Borderlayout borderlayout = new Borderlayout();

						borderlayout.setParent(detail);
						borderlayout.setHeight("400px");
						borderlayout.setWidth("100%");

						North north = new North();
						ais.ui.util.ZkCompat.setFlex(north, true);
						north.setParent(borderlayout);

						final Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(north);

						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 3);
						final MyDatebox tanggalMulai = new MyDatebox(calendar.getTime());
						final MyDatebox tanggalSampai = new MyDatebox(new Date());

						final ParameterListener parameterListener = new ParameterListener() {

							@SuppressWarnings({ "unchecked", "rawtypes" })
							@Override
							public Map<String, Serializable> generateParameters() throws Exception {
								Lokasi myLokasi = (Lokasi) (TrackingStokItemAction.this.searchlokasi
										.getSelectedItem() == null ? null
												: TrackingStokItemAction.this.searchlokasi.getSelectedItem()
														.getValue());

								Date myTanggalMulai = tanggalMulai.getValue();
								Date myTanggalSampai = tanggalSampai.getValue();
								Map parameters = new HashMap<String, Serializable>();
								Common.insertProperty(ItemMedis.class, item, parameters, "");
								parameters.put("lokasi1", myLokasi == null || myLokasi.getId() == null ? -1L : myLokasi.getId());
								parameters.put("item", item == null || item.getId() == null ? -1L : item.getId());
								parameters.put("tgl1", myTanggalMulai == null ? "2000-01-01"
										: Common.databaseDateFormat.get().format(myTanggalMulai));
								parameters.put("tgl2", myTanggalSampai == null ? "2000-01-01"
										: Common.databaseDateFormat.get().format(myTanggalSampai));
								return parameters;
							}
						};

						final EventListener eventListener = new EventListener() {

							@SuppressWarnings({ "unchecked", "rawtypes" })
							@Override
							public void onEvent(Event arg0) throws Exception {

								Map parameters = parameterListener.generateParameters();
								Common.insertProperty(ItemMedis.class, item, parameters, "");
								File file = Report.generateFileReport("sirs/tracking_stok_barang", Report.XLS,
										parameters, "sirs/tracking_stok_barang", new Date(),
										Sessions.getCurrent().getWebApp());
								CommonReport.tampilkanReportXLS(center, file, 500);
							}
						};

						toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Mulai")));
						toolbar.appendChild(tanggalMulai);
						// tanggalMulai.setWidth("90%");
						tanggalMulai.addEventListener("onChange", eventListener);

						toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Sampai")));
						toolbar.appendChild(tanggalSampai);
						// tanggalSampai.setWidth("90%");
						tanggalSampai.addEventListener("onChange", eventListener);

						South south = new South();
						south.setParent(borderlayout);
						south.appendChild(CommonReport.exportReport(parameterListener, "sirs/tracking_stok_barang"));

						eventListener.onEvent(null);
					}
				}
			});

			new Label(item.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(ItemMedis.class, item, item.getNama()).setParent(arg0);
			new Label(item.getBarcode()).setParent(arg0);
			new Label(item.getSatuanItem() == null ? "" : item.getSatuanItem().getNama()).setParent(arg0);
			new Label(item.getJenisItem() == null ? "" : item.getJenisItem().getNama()).setParent(arg0);
			new Label(item.getBatasMinimalStok() == null ? "" : Common.numberFormat.get().format(item.getBatasMinimalStok()))
					.setParent(arg0);
			new Label(stok == null ? "" : Common.numberFormat.get().format(stok)).setParent(arg0);
			new Label(nilai == null ? "" : Common.numberFormat.get().format(nilai)).setParent(arg0);
			new Label(tanggalTerakhirPengadaan == null ? "" : Common.dateFormat2.get().format(tanggalTerakhirPengadaan))
					.setParent(arg0);
			new Label(gudang).setParent(arg0);

		}

	}

	public void onCetak(Event event) throws InterruptedException {
		LaporanTrackingStokItemWindow laporanTrackingStokItemWindow = new LaporanTrackingStokItemWindow();
		laporanTrackingStokItemWindow.setTitle("Laporan Tracking Stok");
		laporanTrackingStokItemWindow.setHeight("95%");
		laporanTrackingStokItemWindow.setWidth("95%");
		laporanTrackingStokItemWindow.setClosable(true);
		page.getFirstRoot().appendChild(laporanTrackingStokItemWindow);
		laporanTrackingStokItemWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();

		JenisItem jenisItem = (JenisItem) (searchjenisItem.getSelectedItem() == null ? null
				: searchjenisItem.getSelectedItem().getValue());
		SatuanItem satuanItem = (SatuanItem) (searchsatuanItem.getSelectedItem() == null ? null
				: searchsatuanItem.getSelectedItem().getValue());

		Lokasi lokasi = (Lokasi) (searchlokasi.getSelectedItem() == null ? null
				: searchlokasi.getSelectedItem().getValue());

		String sql = "select a.item, max(c.nama) as nama_item, " + "max(a.tanggal) as tanggal_terakhir_pengadaan, "
				+ "sum((a.qty+a.qty_bonus)*b.jenis) as stok, "
				+ "max(d.nama) as lokasi, sum(((a.qty+a.qty_bonus)*b.jenis)*(e.harga_jual)) as nilai from sirs.detail_transaksi_pasien a "
				+ "inner join sirs.kode_transaksi_medis b on (a.kode_transaksi = b.id) "
				+ "left join sirs.item_medis c on (a.item = c.id) " + "left join asset.lokasi d on (a.lokasi = d.id) "
				+ "left join (select item, (case when max(harga_jual) is null then 0 else max(harga_jual) end) as harga_jual from sirs.harga_jual_item where kelas_perawatan = "
				+ ConstantValues.kelasNormalId() + " group by item ) e on (e.item = a.item) " + "where 1=1 "
				+ (searchkode.getValue().trim().equals("") ? ""
						: " and c.kode ilike '%" + searchkode.getValue().trim() + "%' ")
				+ " "
				+ (searchnama.getValue().trim().equals("") ? ""
						: " and c.nama ilike '%" + searchnama.getValue().trim() + "%' ")
				+ "  "
				+ (searchbarcode.getValue().trim().equals("") ? ""
						: "and c.barcode ilike '%" + searchbarcode.getValue().trim() + "%'")
				+ "  and c.jenis_item = " + (jenisItem == null ? "c.jenis_item" : jenisItem.getId())
				+ " and a.lokasi = " + (lokasi == null ? "a.lokasi" : lokasi.getId()) + " and c.satuan_item = "
				+ (satuanItem == null ? "c.satuan_item" : satuanItem.getId()) + " and date(a.tanggal) <= date('"
				+ (dateFormat.format(searchperTanggal.getValue() == null ? new Date() : searchperTanggal.getValue()))
				+ "') group by a.lokasi,a.item order by stok asc";

		List<Object[]> item = session.createSQLQuery(sql).list();
		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new JenisBarangRenderer());
		grid.setModel(strset);
		grid.renderAll();

		loadTotal(item);
	}

	public void loadTotal(List<Object[]> items) {

		Double mytotal = 0.0;
		Double mytotalRtr = 0.0;
		for (Object[] objects : items) {
			Number stok = (Number) objects[3];
			Number nilai = (Number) objects[5];
			mytotal += (stok == null ? 0.0 : stok.doubleValue());
			mytotalRtr += (nilai == null ? 0.0 : nilai.doubleValue());
		}

		stok.setStyle("font-weight:bold;font-size:15px;text-align:right;");
		nilai.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		stok.setLabel(Common.numberFormat.get().format(mytotal));
		nilai.setLabel(Common.numberFormat.get().format(mytotalRtr));
	}

}
