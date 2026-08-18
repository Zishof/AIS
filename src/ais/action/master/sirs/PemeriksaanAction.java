package ais.action.master.sirs;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataPemeriksaanBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Pemeriksaan;
import ais.ui.util.MyMessageboxConfig;

public class PemeriksaanAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Paging paging;
	private Grid grid;

	private Textbox searchnama;

	private Textbox nama;
	private Combobox jenis;
	private Textbox nilaiYangMungkin;
	private Textbox nilaiDefault;
	private Textbox normal;
	private Textbox satuan;
	private Textbox keterangan;
	private Checkbox aktif;
	private Checkbox harusDiisi;
	private Intbox jumlahBaris;

	private boolean edit = false;
	private boolean delete = false;

	private Pemeriksaan pemeriksaan;
	private Toolbarbutton add;

	private AmbilDataPemeriksaanBanbox parent;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

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
	}

	class PemeriksaanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Pemeriksaan pemeriksaan = (Pemeriksaan) arg1;

			RevisiHelper.createNewRevisi(Pemeriksaan.class, pemeriksaan, pemeriksaan.getNama()).setParent(arg0);

			new Label(pemeriksaan.getJenis()).setParent(arg0);

			new Label(pemeriksaan.getNilaiYangMungkin()).setParent(arg0);
			new Label(pemeriksaan.getNilaiDefault()).setParent(arg0);
			new Label(pemeriksaan.getNormal()).setParent(arg0);
			new Label(pemeriksaan.getSatuan()).setParent(arg0);

			new Label(pemeriksaan.getParent() == null ? "" : pemeriksaan.getParent().toString()).setParent(arg0);
			new Label(pemeriksaan.getAktif() != null && pemeriksaan.getAktif() ? "Aktif" : "Tidak").setParent(arg0);
			new Label(pemeriksaan.getHarusDiisi() ? "Ya" : "Tidak").setParent(arg0);
			new Label(pemeriksaan.getJumlahBaris().toString()).setParent(arg0);
			new Label(pemeriksaan.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pemeriksaan);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Data akan dihapus secara permanen dan tidak dapat dikembalikan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(pemeriksaan);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait; (2) pastikan data ini tidak sedang digunakan pada transaksi lain; (3) apabila kendala berlanjut, hubungi administrator sistem.",
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

	public void onAdd(Event event) throws Exception {
		init(new Pemeriksaan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Pemeriksaan pemeriksaan) throws Exception {
		this.pemeriksaan = pemeriksaan;
		addWindow.setTitle(pemeriksaan.getId() == null ? "Tambah Keluhan, Riwayat, atau Pemeriksaan" : "Ubah Keluhan, Riwayat, atau Pemeriksaan");
		Common.clear(addWindow);
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setWidth("40%");

		column = new Column();
		column.setParent(columns);
		column.setWidth("60%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Isi Keluhan, Riwayat, atau Pemeriksaan")));
		row.appendChild(nama = new Textbox(pemeriksaan.getNama() == null ? "" : pemeriksaan.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis")));
		row.appendChild(jenis = new Combobox());
		Comboitem c = new Comboitem(Pemeriksaan.JENIS_KELUHAN);
		c.setValue(Pemeriksaan.JENIS_KELUHAN);
		jenis.appendChild(c);
		c = new Comboitem(Pemeriksaan.JENIS_RIWAYAT);
		c.setValue(Pemeriksaan.JENIS_RIWAYAT);
		jenis.appendChild(c);
		c = new Comboitem(Pemeriksaan.JENIS_PERIKSA);
		c.setValue(Pemeriksaan.JENIS_PERIKSA);
		jenis.appendChild(c);
		Common.selectComboItem(jenis, pemeriksaan.getJenis());
		jenis.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nilai yang mungkin")));
		row.appendChild(nilaiYangMungkin = new Textbox(pemeriksaan.getNilaiYangMungkin()));
		nilaiYangMungkin.setWidth("90%");
		nilaiYangMungkin.setRows(3);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nilai default")));
		row.appendChild(nilaiDefault = new Textbox(pemeriksaan.getNilaiDefault()));
		nilaiDefault.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nilai Normal (jika ada)")));
		row.appendChild(normal = new Textbox(pemeriksaan.getNormal()));
		normal.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Satuan")));
		row.appendChild(satuan = new Textbox(pemeriksaan.getSatuan()));
		satuan.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jumlah baris form isian")));
		row.appendChild(jumlahBaris = new Intbox(pemeriksaan.getJumlahBaris()));
		jumlahBaris.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(
				keterangan = new Textbox(pemeriksaan.getKeterangan() == null ? "" : pemeriksaan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pemeriksaan Parent")));
		row.appendChild(parent = new AmbilDataPemeriksaanBanbox(true));
		parent.setValue(pemeriksaan.getParent() == null ? "" : pemeriksaan.getParent().toString());
		parent.setAttribute("pemeriksaan", pemeriksaan.getParent());
		parent.setWidth("90%");
		parent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// Pemeriksaan pemeriksaanParent = (Pemeriksaan) parent
				// .getAttribute("pemeriksaan");
				// System.out.println("pemeriksaanParent = "
				// + pemeriksaanParent);
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Aktif")));
		row.appendChild(aktif = new Checkbox());
		aktif.setChecked(pemeriksaan.getAktif());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Harus diisi")));
		row.appendChild(harusDiisi = new Checkbox());
		harusDiisi.setChecked(pemeriksaan.getAktif());

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
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
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Isi Keluhan, Riwayat, atau Pemeriksaan harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom Keluhan, Riwayat, atau Pemeriksaan; (2) pastikan isian tidak kosong; (3) ulangi proses penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jenis.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Jenis harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih jenis pada kolom Jenis; (2) pastikan jenis yang dipilih sudah benar; (3) ulangi proses penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pemeriksaan.getId() != null) {
			pemeriksaan = (Pemeriksaan) session.load(Pemeriksaan.class, pemeriksaan.getId());

		}

		Pemeriksaan pemeriksaanParent = (Pemeriksaan) parent.getAttribute("pemeriksaan");

		pemeriksaan.setJumlahBaris(jumlahBaris.getValue());
		pemeriksaan.setNilaiDefault(nilaiDefault.getValue().trim());
		pemeriksaan.setJenis((String) jenis.getSelectedItem().getValue());
		pemeriksaan.setNilaiYangMungkin(nilaiYangMungkin.getValue().trim());
		pemeriksaan.setNormal(normal.getValue().trim());
		pemeriksaan.setSatuan(satuan.getValue().trim());
		pemeriksaan.setParent(pemeriksaanParent);
		pemeriksaan.setAktif(aktif.isChecked());
		pemeriksaan.setHarusDiisi(harusDiisi.isChecked());
		pemeriksaan.setNama(nama.getValue());
		pemeriksaan.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, pemeriksaan);

		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pemeriksaan.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pemeriksaan> pemeriksaan = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						Pemeriksaan.class);
		ListModel strset = new SimpleListModel(pemeriksaan);
		grid.setRowRenderer(new PemeriksaanRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

	public Textbox getNilaiDefault() {
		return nilaiDefault;
	}

	public void setNilaiDefault(Textbox nilaiDefault) {
		this.nilaiDefault = nilaiDefault;
	}

}
