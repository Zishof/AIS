package ais.action.master.payroll;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.akunting.helper.AmbilDataPegawaiNotDefaultBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.payroll.JenisTransaksiPegawai;
import ais.database.model.payroll.TransaksiPegawai;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class TransaksiPegawaiAction extends GenericAutowireComposer
		implements DataInitDefault, DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private MyGrid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private AmbilDataPegawaiNotDefaultBanbox searchpegawai;
	private Combobox searchJenisTransaksi;
	private MyDatebox searchMulai;
	private MyDatebox searchSampai;

	private AmbilDataPegawaiNotDefaultBanbox pegawai;
	private Combobox jenisTransaksiPegawai;
	private MyDoublebox nilai;
	private MyDatebox tanggal;
	private MyTextbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private TransaksiPegawai transaksiPegawai;
	private MyToolbarbuttonConfig add;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchpegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Common.insertCombo(searchJenisTransaksi, new String[] { "nama", "formula" }, "kode",
				JenisTransaksiPegawai.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "pegawai", "jenisTransaksiPegawai", "nilai", "tanggal", "keterangan",
				"postingHistory" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(TransaksiPegawai.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, TransaksiPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class TransaksiPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			TransaksiPegawai transaksiPegawai = (TransaksiPegawai) arg1;

			RevisiHelper
					.createNewRevisi(TransaksiPegawai.class, transaksiPegawai, transaksiPegawai.getPegawai().getNama())
					.setParent(arg0);
			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			new Label(transaksiPegawai.getJenisTransaksiPegawai().toString()).setParent(hbox);
			String formula = transaksiPegawai.getJenisTransaksiPegawai().getFormula();
			if (!formula.isEmpty()) {
				hbox.appendChild(new Label(formula));
			}

			new Label(Common.numberFormat.get().format(transaksiPegawai.getNilai())).setParent(arg0);
			new Label(Common.dateFormat3.get().format(transaksiPegawai.getTanggal())).setParent(arg0);
			new Label(transaksiPegawai.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: transaksiPegawai.getPostingHistory().toString()).setParent(arg0);
			new Label(transaksiPegawai.getKeterangan()).setParent(arg0);

			if (transaksiPegawai.getPengajuanTransaksiPegawai() == null) {
				Common.copyEditDeleteButtons(edit, delete, transaksiPegawai, TransaksiPegawaiAction.this)
						.setParent(arg0);
			} else {
				new Label().setParent(arg0);
			}
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new TransaksiPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		init((TransaksiPegawai) obj);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(TransaksiPegawai transaksiPegawai) {
		this.transaksiPegawai = transaksiPegawai;
		addWindow.setTitle(transaksiPegawai.getId() == null ? "Tambah Transaksi Pegawai" : "Ubah Transaksi Pegawai");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label("Pegawai / Karyawan"));
		row.appendChild(pegawai = new AmbilDataPegawaiNotDefaultBanbox());
		pegawai.setAttribute("pegawai", transaksiPegawai.getPegawai());
		pegawai.setValue(transaksiPegawai.getPegawai() == null ? "" : transaksiPegawai.getPegawai().getNama());
		pegawai.setWidth("90%");
		pegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis transaksi")));
		row.appendChild(jenisTransaksiPegawai = new Combobox());
		Common.insertCombo(jenisTransaksiPegawai, "nama", "kode", JenisTransaksiPegawai.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenisTransaksiPegawai, transaksiPegawai.getJenisTransaksiPegawai());
		jenisTransaksiPegawai.setWidth("90%");
		jenisTransaksiPegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nilai transaksi")));
		row.appendChild(nilai = new MyDoublebox(transaksiPegawai.getNilai()));
		nilai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu dan tanggal transaksi")));
		row.appendChild(tanggal = new MyDatebox(transaksiPegawai.getTanggal()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				transaksiPegawai.getKeterangan() == null ? "" : transaksiPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (pegawai.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Pegawai / Karyawan belum diisi. Data transaksi tidak dapat disimpan sebelum pegawai ditentukan. Langkah yang dapat dilakukan: (1) Klik kolom Pegawai / Karyawan; (2) Pilih pegawai yang dituju dari daftar; (3) Ulangi proses penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (jenisTransaksiPegawai.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Jenis Transaksi belum diisi. Data transaksi tidak dapat disimpan sebelum jenis transaksi dipilih. Langkah yang dapat dilakukan: (1) Klik kolom Jenis Transaksi; (2) Pilih salah satu jenis transaksi yang tersedia; (3) Ulangi proses penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (transaksiPegawai.getId() != null) {
			transaksiPegawai = (TransaksiPegawai) session.load(TransaksiPegawai.class, transaksiPegawai.getId());

		}
		transaksiPegawai.setNilai(nilai.getValue());
		transaksiPegawai.setTanggal(tanggal.getValue());

		transaksiPegawai
				.setJenisTransaksiPegawai((JenisTransaksiPegawai) jenisTransaksiPegawai.getSelectedItem().getValue());
		transaksiPegawai.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		transaksiPegawai.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, transaksiPegawai);
		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<TransaksiPegawai> transaksiPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(transaksiPegawai);
		grid.setRowRenderer(new TransaksiPegawaiRenderer());
		grid.setModelCheckMobile(strset);

		grid.renderAll();

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TransaksiPegawai.class);
		if (order)
			criteria.addOrder(Order.desc("tanggal"));
		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("keterangan", searchnama.getValue(), MatchMode.ANYWHERE)))
				.add((searchpegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("pegawai", searchpegawai.getAttribute("pegawai"))))
				.add(searchJenisTransaksi.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jenisTransaksiPegawai", searchJenisTransaksi.getSelectedItem().getValue()))
				.add((searchMulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchMulai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tanggal", searchMulai.getValue())))
				.add((searchSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchSampai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.le("tanggal", searchSampai.getValue())));
		return criteria;
	}

}
