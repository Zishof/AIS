package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import ais.ui.util.MyCkEditor;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.East;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Groupbox;
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

import ais.action.master.helper.DisplayGaleriFotoHelper;
import ais.action.master.helper.GaleriFotoHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GaleriFoto;
import ais.database.model.GeneralValueObject;
import ais.database.model.file.GaleriFotoImage;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class GaleriFotoAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	protected Integer jenis = null;

	private GaleriFoto galeriFoto;
	private MyToolbarbuttonConfig add;
	private MyGrid gridGaleriFoto;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (jenis == null) {
			jenis = GaleriFoto.ALUMNI;
		}

		if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
			add.setTooltiptext("Tambah");

			edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		if (add != null) {
			String[] contents = new String[] { "id", "nama", "mulai", "sampai", "keterangan" };
			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(GaleriFoto.class, this, contents);
			Common.appendKeToolbar(cetakToolbarbutton, add, comp);

			MyToolbarbuttonConfig upload = Common.uploadData(this, GaleriFoto.class, contents);
			upload.setVisible((add != null && add.isVisible()) && edit && delete);
			Common.appendKeToolbar(upload, add, comp);
		}
	}

	class GaleriFotoRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GaleriFoto galeriFoto = (GaleriFoto) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								DisplayGaleriFotoHelper displayGaleriFotoHelper = new DisplayGaleriFotoHelper(
										new MyGrid());
								Groupbox div = displayGaleriFotoHelper.initDetail(galeriFoto);
								detail.appendChild(div);
							}
						});
					}
				}
			});

			RevisiHelper.createNewRevisi(GaleriFoto.class, galeriFoto, galeriFoto.getNama()).setParent(arg0);
			new Label(galeriFoto.getMulai() == null ? "" : Common.dateFormat4.get().format(galeriFoto.getMulai()))
					.setParent(arg0);
			new Label(galeriFoto.getSampai() == null ? "" : Common.dateFormat4.get().format(galeriFoto.getSampai()))
					.setParent(arg0);
			new Label(galeriFoto.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, galeriFoto, GaleriFotoAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new GaleriFoto());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		galeriFoto = (GaleriFoto) obj;
		init(galeriFoto);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(GaleriFoto galeriFoto) throws Exception {
		this.galeriFoto = galeriFoto;
		addWindow.setTitle(galeriFoto.getId() == null ? "Tambah Galeri Foto" : "Ubah Galeri Foto");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Galeri Foto *"));
		row.appendChild(nama = new Textbox(galeriFoto.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal mulai kegiatan dalam Foto"));
		row.appendChild(mulai = new MyDatebox(galeriFoto.getMulai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal sampai kegiatan dalam Foto"));
		row.appendChild(sampai = new MyDatebox(galeriFoto.getSampai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(galeriFoto.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		East east = new East();
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setParent(borderlayout);
		east.setWidth("75%");
		east.appendChild(new GaleriFotoHelper(gridGaleriFoto = new MyGrid()).initDetail(galeriFoto));

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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
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

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Judul Galeri Foto",
					"Kolom Judul Galeri Foto belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Judul Galeri Foto.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		List<Row> rowsGaleriFoto = gridGaleriFoto.getRows().getChildren();
		for (Row row : rowsGaleriFoto) {
			GaleriFotoImage galeriFotoImage = (GaleriFotoImage) row.getAttribute("galeriFotoImage");
			if (galeriFotoImage.getHalaman() == null) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data Gambar ke",
						"Kolom Gambar ke belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
						new String[] {
								"Isi/pilih terlebih dahulu Gambar ke.",
								"Ulangi proses penyimpanan setelah kolom tersebut terisi."
						});
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (galeriFoto.getId() != null) {
			galeriFoto = (GaleriFoto) session.load(GaleriFoto.class, galeriFoto.getId());

		}

		galeriFoto.setNama(nama.getValue());
		galeriFoto.setKeterangan(keterangan.getValue());
		galeriFoto.setJenis(jenis);
		galeriFoto.setMulai(mulai.getValue());
		galeriFoto.setSampai(sampai.getValue());

		Common.refreshSaveOrUpdate(session, galeriFoto);

		Session mysession = StreamingHibernateUtil.getInstance().currentSession();
		try {
			for (Row row : rowsGaleriFoto) {
				MyCkEditor keterangan = (MyCkEditor) row.getAttribute("keterangan");
				GaleriFotoImage galeriFotoImage = (GaleriFotoImage) row.getAttribute("galeriFotoImage");

				mysession.refresh(galeriFotoImage);

				galeriFotoImage.setGaleriFoto(galeriFoto.getId());
				galeriFotoImage.setKeterangan(keterangan.getValue());
				mysession.getTransaction().begin();
				mysession.saveOrUpdate(galeriFotoImage);
				mysession.getTransaction().commit();
			}
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		StreamingHibernateUtil.getInstance().closeSession();

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(GaleriFoto.class).add(Restrictions.eq("jenis", jenis));

		if (order)
			criteria.addOrder(Order.desc("mulai")).addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<GaleriFoto> galeriFoto = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(galeriFoto);
		grid.setRowRenderer(new GaleriFotoRenderer());
		grid.setModelCheckMobile(strset);

	}

}
