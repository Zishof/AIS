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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
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
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.payroll.helper.AmbilDataFormatItemGajiBanbox;
import ais.action.master.payroll.helper.AmbilDataItemGajiBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.ItemGaji;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class ItemGajiAction extends GenericAutowireComposer
		implements DataInitDefault, DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Textbox searchnama;
	private AmbilDataFormatItemGajiBanbox searchFormatItemGaji;

	private Textbox nama;
	private AmbilDataFormatItemGajiBanbox formatItemGaji;
	private Textbox defaultFormula;
	private Textbox kode;
	private MyIntbox urutan;
	private AmbilDataItemGajiBanbox parent;
	private AmbilDataAkunBanbox akun;
	private AmbilDataAkunBanbox akunDebet;
	private Textbox keterangan;
	private Checkbox aktif;
	private Checkbox tampilkanDiSlip;

	private boolean edit = false;
	private boolean delete = false;

	private ItemGaji itemGaji;
	private MyToolbarbuttonConfig add;
	private MyCheckboxConfig nilaiVariableBisaDiubah;
	private MyCheckboxConfig jadikan0JikaMinus;
	private MyCheckboxConfig space;
	private MyCheckboxConfig finalGaji;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		searchFormatItemGaji.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (searchFormatItemGaji != null) { searchFormatItemGaji.setReadonly(true); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

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

		String[] contents = new String[] { "id", "kode", "nama", "formatItemGaji", "parent", "nomorUrut",
				"jadikan0JikaMinus", "tampilkanDiSlip", "defaultFormula", "akun", "akunDebet",
				"nilaiVariableBisaDiubah", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(ItemGaji.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ItemGaji.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class ItemGajiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ItemGaji itemGaji = (ItemGaji) arg1;

			new Label(itemGaji.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(ItemGaji.class, itemGaji, itemGaji.getNama()).setParent(arg0);

			new Label(itemGaji.getDefaultFormula()).setParent(arg0);
			new Label(itemGaji.getNomorUrut().toString()).setParent(arg0);

			new Label(itemGaji.getParent() == null ? "" : itemGaji.getParent().getNama()).setParent(arg0);
			try {
				new Label(itemGaji.getAkun() == null ? "" : itemGaji.getAkun().getNama()).setParent(arg0);
			} catch (Exception e) {
				new Label().setParent(arg0);
			}
			try {
				new Label(itemGaji.getAkunDebet() == null ? "" : itemGaji.getAkunDebet().getNama()).setParent(arg0);
			} catch (Exception e) {
				new Label().setParent(arg0);
			}
			new Label(itemGaji.getAktif() != null && itemGaji.getAktif() ? "Aktif" : "Tidak").setParent(arg0);

			new Label(itemGaji.getFormatItemGaji() == null ? "" : itemGaji.getFormatItemGaji().toString())
					.setParent(arg0);

			new Label(itemGaji.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, itemGaji, ItemGajiAction.this).setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new ItemGaji());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		init((ItemGaji) obj);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(ItemGaji itemGaji) throws Exception {
		this.itemGaji = itemGaji;
		addWindow.setTitle(itemGaji.getId() == null ? "Tambah Item Gaji" : "Ubah Item Gaji");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Item Gaji")));
		row.appendChild(kode = new Textbox(itemGaji.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Item Gaji")));
		row.appendChild(nama = new Textbox(itemGaji.getNama() == null ? "" : itemGaji.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Format Item Gaji")));
		row.appendChild(formatItemGaji = new AmbilDataFormatItemGajiBanbox());
		formatItemGaji.setValue(itemGaji.getFormatItemGaji() == null ? "" : itemGaji.getFormatItemGaji().getNama());
		formatItemGaji.setAttribute("formatItemGaji", itemGaji.getFormatItemGaji());
		formatItemGaji.setWidth("90%");
		formatItemGaji.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Formula Penghitungan")));
		row.appendChild(defaultFormula = new Textbox(itemGaji.getDefaultFormula()));
		defaultFormula.setWidth("90%");
		defaultFormula.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nomor Urutan")));
		row.appendChild(urutan = new MyIntbox(itemGaji.getNomorUrut()));
		urutan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new Textbox(itemGaji.getKeterangan() == null ? "" : itemGaji.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Item Gaji Parent")));
		row.appendChild(parent = new AmbilDataItemGajiBanbox(true));
		parent.setValue(itemGaji.getParent() == null ? "" : itemGaji.getParent().toString());
		parent.setAttribute("itemGaji", itemGaji.getParent());
		parent.setWidth("90%");

		Akun a = itemGaji.ambilAkun();

		row = new MyFormRow();
		row.setStyle("border:0px;background: transakun;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Akun Kredit Item Gaji")));
		akun = new AmbilDataAkunBanbox();
		if (a != null && a.getId() != null) {
			itemGaji.setAkun(a);
			new Label(a.getKode() + " " + a.getNama()).setParent(row);
		} else {
			row.appendChild(akun);
		}
		akun.setValue(itemGaji.getAkun() == null ? "" : itemGaji.getAkun().getNama());
		akun.setAttribute("akun", itemGaji.getAkun());
		akun.setWidth("90%");

		a = itemGaji.ambilAkunDebet();

		row = new MyFormRow();
		row.setStyle("border:0px;background: transakun;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Akun Debet Item Gaji")));
		akunDebet = new AmbilDataAkunBanbox();
		if (a != null && a.getId() != null) {
			itemGaji.setAkunDebet(a);
			new Label(a.getKode() + " " + a.getNama()).setParent(row);
		} else {
			row.appendChild(akunDebet);
		}
		akunDebet.setValue(itemGaji.getAkunDebet() == null ? "" : itemGaji.getAkunDebet().getNama());
		akunDebet.setAttribute("akun", itemGaji.getAkunDebet());
		akunDebet.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(nilaiVariableBisaDiubah = new MyCheckboxConfig("Nilai Variable Bisa Diubah"));
		nilaiVariableBisaDiubah.setChecked(itemGaji.getNilaiVariableBisaDiubah());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(aktif = new MyCheckboxConfig("Aktif"));
		aktif.setChecked(itemGaji.getAktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(tampilkanDiSlip = new MyCheckboxConfig("Tampilkan item gaji ini di Slip Gaji"));
		tampilkanDiSlip.setChecked(itemGaji.getTampilkanDiSlip());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(jadikan0JikaMinus = new MyCheckboxConfig("Jika hasil pengitungan minus, jadikan 0"));
		jadikan0JikaMinus.setChecked(itemGaji.getJadikan0JikaMinus());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(finalGaji = new MyCheckboxConfig("Merupuakan perhitungan final / hasil total gaji"));
		finalGaji.setChecked(itemGaji.getFinalGaji());

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyHtml("<hr>"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(space = new MyCheckboxConfig("Item ini hanya space kosong"));
		space.setChecked(itemGaji.getSpace());

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
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
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Kode wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Kode pada kolom yang tersedia; (2) pastikan Kode tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Nama wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama pada kolom yang tersedia; (2) pastikan Nama tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (formatItemGaji.getAttribute("formatItemGaji") == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Format Item Gaji wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Format Item Gaji yang sesuai; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (urutan.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Nomor Urut wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nomor Urut pada kolom yang tersedia; (2) pastikan Nomor Urut tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (itemGaji.getId() != null) {
			itemGaji = (ItemGaji) session.load(ItemGaji.class, itemGaji.getId());

		}

		ItemGaji itemGajiParent = (ItemGaji) parent.getAttribute("itemGaji");

		itemGaji.setTampilkanDiSlip(tampilkanDiSlip.isChecked());
		itemGaji.setAkunDebet((Akun) akunDebet.getAttribute("akun"));
		itemGaji.setAkun((Akun) akun.getAttribute("akun"));
		itemGaji.setKode(kode.getValue().trim());
		itemGaji.setFormatItemGaji((FormatItemGaji) formatItemGaji.getAttribute("formatItemGaji"));
		itemGaji.setDefaultFormula(defaultFormula.getValue().trim());
		itemGaji.setNomorUrut(urutan.getValue());
		itemGaji.setParent(itemGajiParent);
		itemGaji.setAktif(aktif.isChecked());
		itemGaji.setNama(nama.getValue());
		itemGaji.setKeterangan(keterangan.getValue());
		itemGaji.setTampilkanDiSlip(tampilkanDiSlip.isChecked());
		itemGaji.setJadikan0JikaMinus(jadikan0JikaMinus.isChecked());
		itemGaji.setNilaiVariableBisaDiubah(nilaiVariableBisaDiubah.isChecked());
		itemGaji.setSpace(space.isChecked());
		itemGaji.setFinalGaji(finalGaji.isChecked());

		Common.refreshSaveOrUpdate(session, itemGaji);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ItemGaji.class);
		if (order)
			criteria.addOrder(Order.asc("formatItemGaji")).addOrder(Order.asc("nomorUrut"));
		criteria.add(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add((searchFormatItemGaji == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchFormatItemGaji.getAttribute("formatItemGaji") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("formatItemGaji", searchFormatItemGaji.getAttribute("formatItemGaji"))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ItemGaji> itemGaji = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(itemGaji);
		grid.setRowRenderer(new ItemGajiRenderer());
		grid.setModelCheckMobile(strset);

//		grid.renderAll();

	}

	public Textbox getKode() {
		return kode;
	}

	public void setKode(Textbox kode) {
		this.kode = kode;
	}

}
