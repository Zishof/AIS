package ais.action.master.employ;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.employ.Golongan;
import ais.database.model.employ.Transport;
import ais.database.model.employ.Peraturan;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TransportAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchgolongan;
	private Combobox searchperaturan;
	private Intbox searchmasakerja;

	private Combobox golongan;
	private Combobox masaKerja;
	private MyDoublebox gaji;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Transport transport;
	private MyToolbarbuttonConfig add;

	private MyDatebox tanggalEfektif;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.insertComboDanSemua(searchgolongan, "nama", Golongan.class, Restrictions.eq("aktif", true));
		Common.insertComboDanSemua(searchperaturan, "nama", Peraturan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		masaKerja = new Combobox();
		MyComboitemConfig comboitem;
		for (int i = 0; i < 63; i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			comboitem.setParent(masaKerja);
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "keterangan", "golongan", "peraturan", "masaKerja", "transport",
				"tanggalEfektif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Transport.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Transport.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class TransportRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Transport transport = (Transport) arg1;

			new Label(transport.getGolongan() == null ? "" : transport.getGolongan().getNama()).setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Transport.class, transport,
					transport.getPeraturan() == null ? "-" : transport.getPeraturan().getNama())).setParent(arg0);
			if (transport.getPeraturan() != null) {
				Vbox myvbox = new Vbox();
				myvbox.setParent(a);

				Hbox hbox = new Hbox();
				hbox.setParent(myvbox);
				LampiranLain.createDownloadUploadFileLain(hbox, transport.getPeraturan().getId(),
						Peraturan.class.getName(), "Peraturan Dokumen", false, null, null, false, false, false, false);
			}

			new Label(transport.getMasaKerja() == null ? "0"
					: Common.numberFormat.get().format(transport.getMasaKerja()) + " th").setParent(arg0);

			new Label(transport.getTransport() == null ? "0" : Common.numberFormat.get().format(transport.getTransport()))
					.setParent(arg0);

			new Label(transport.getTanggalEfektif() == null ? "0"
					: Common.dateFormat1.get().format(transport.getTanggalEfektif())).setParent(arg0);
			new Label(transport.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(transport);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(transport);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
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

	public void onAdd(Event event) throws Exception {
		init(new Transport());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Transport transport) throws Exception {
		this.transport = transport;
		addWindow.setTitle(transport.getId() == null ? "Tambah Transport" : "Ubah Transport");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Golongan *"));
		row.appendChild(golongan = new Combobox());
		Common.insertCombo(golongan, "nama", Golongan.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(golongan, transport.getGolongan());
		golongan.setWidth("90%");
		golongan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Kerja (MK) *"));
		row.appendChild(masaKerja);
		Common.selectComboItem(masaKerja, transport.getMasaKerja() == null ? null : transport.getMasaKerja());
		masaKerja.setWidth("90%");
		masaKerja.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Transport (TRANSPORT) *"));
		row.appendChild(gaji = new MyDoublebox(transport.getTransport()));
		gaji.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Efektif *"));
		row.appendChild(tanggalEfektif = new MyDatebox(transport.getTanggalEfektif()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(transport.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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

	public boolean onSave(Event event) throws Exception {

		if (golongan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Golongan belum dipilih. Langkah yang dapat dilakukan: (1) pilih Golongan dari dropdown pada form; (2) pastikan data golongan sudah tersedia di master; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (masaKerja.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Masa Kerja belum dipilih. Langkah yang dapat dilakukan: (1) pilih Masa Kerja dari dropdown pada form; (2) pastikan data masa kerja sudah tersedia di master; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (gaji.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tunjangan Transport belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Transport pada form; (2) pastikan nilai berupa angka yang valid; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (transport.getId() != null) {
			transport = (Transport) session.load(Transport.class, transport.getId());

		}

		transport.setGolongan(
				(Golongan) (golongan.getSelectedItem() == null ? null : golongan.getSelectedItem().getValue()));
		// transport.setMasaKerja(masaKerja.getValue());

		transport.setTransport(gaji.getValue());
		transport.setMasaKerja(
				(Integer) (masaKerja.getSelectedItem() == null ? null : masaKerja.getSelectedItem().getValue()));
		transport.setKeterangan(keterangan.getValue());

		transport.setTanggalEfektif(tanggalEfektif.getValue());

		if (transport.getId() != null) {
			session.save(transport);
		} else {
			Common.refreshUpdate(session, transport);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Transport.class).createAlias("golongan", "golongan")
				.add(Restrictions.eq("golongan.aktif", true));
		if (order)
			criteria.addOrder(Order.asc("golongan.id")).addOrder(Order.asc("masaKerja"));
		criteria.add(searchgolongan.getSelectedItem() == null || searchgolongan.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("golongan", searchgolongan.getSelectedItem().getValue()))
				.add(searchperaturan.getSelectedItem() == null || searchperaturan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("peraturan", searchperaturan.getSelectedItem().getValue()))
				.add(searchmasakerja.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("masaKerja", searchmasakerja.getValue()))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Transport> transport = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(transport);
		grid.setRowRenderer(new TransportRenderer());
		grid.setModelCheckMobile(strset);

	}

}
