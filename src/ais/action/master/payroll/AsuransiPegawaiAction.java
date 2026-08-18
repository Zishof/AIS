package ais.action.master.payroll;

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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.payroll.AsuransiPegawai;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AsuransiPegawaiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;
	private Combobox cbJenis; // Penambahan Komponen Combobox untuk Jenis

	private boolean edit = false;
	private boolean delete = false;

	private AsuransiPegawai asuransiPegawai;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private MyDoublebox tarif;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

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

		String[] contents = new String[] { "id", "kode", "nama", "jenis", "keterangan", "aktif", "tarif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(AsuransiPegawai.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AsuransiPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class AsuransiPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final AsuransiPegawai asuransiPegawai = (AsuransiPegawai) arg1;
			new Label(asuransiPegawai.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(AsuransiPegawai.class, asuransiPegawai, asuransiPegawai.getNama())
					.setParent(arg0);
			
			// Menampilkan Label Jenis (diubah dari kode ke string yang lebih user-friendly)
			String jenisLabel = "Untuk Keduanya";
			if (AsuransiPegawai.JENIS_KHUSUS_UNTUK_PEGAWAI.equals(asuransiPegawai.getJenis())) {
				jenisLabel = "Khusus Pegawai";
			} else if (AsuransiPegawai.JENIS_KHUSUS_UNTUK_KELUARGA.equals(asuransiPegawai.getJenis())) {
				jenisLabel = "Khusus Keluarga";
			}
			new Label(jenisLabel).setParent(arg0);
			
			new Label(Common.numberFormat.get().format(asuransiPegawai.getTarif()) ).setParent(arg0);
			new Label(asuransiPegawai.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(asuransiPegawai.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					asuransiPegawai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(asuransiPegawai);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, asuransiPegawai, AsuransiPegawaiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new AsuransiPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		asuransiPegawai = (AsuransiPegawai) obj;
		init(asuransiPegawai);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(AsuransiPegawai asuransiPegawai) {
		this.asuransiPegawai = asuransiPegawai;
		addWindow.setTitle(asuransiPegawai.getId() == null ? "Tambah Asuransi Pegawai" : "Ubah Asuransi Pegawai");
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
		row.appendChild(new MyLabelConfig("Kode Asuransi Pegawai"));
		row.appendChild(kode = new Textbox(asuransiPegawai.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Nama Asuransi Pegawai"));
		row.appendChild(nama = new Textbox(asuransiPegawai.getNama()));
		nama.setWidth("90%");

		// PENAMBAHAN FIELD JENIS
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Jenis Asuransi"));
		cbJenis = new Combobox();
		cbJenis.setReadonly(true);
		cbJenis.setWidth("90%");
		
		MyComboitemConfig ciPegawai = new MyComboitemConfig("Khusus Untuk Pegawai");
		ciPegawai.setValue(AsuransiPegawai.JENIS_KHUSUS_UNTUK_PEGAWAI);
		cbJenis.appendChild(ciPegawai);
		
		MyComboitemConfig ciKeluarga = new MyComboitemConfig("Khusus Untuk Keluarga");
		ciKeluarga.setValue(AsuransiPegawai.JENIS_KHUSUS_UNTUK_KELUARGA);
		cbJenis.appendChild(ciKeluarga);
		
		MyComboitemConfig ciKeduanya = new MyComboitemConfig("Untuk Keduanya");
		ciKeduanya.setValue(AsuransiPegawai.JENIS_UNTUK_KEDUANYA);
		cbJenis.appendChild(ciKeduanya);
		
		// Set default selection based on current object
		if (AsuransiPegawai.JENIS_KHUSUS_UNTUK_PEGAWAI.equals(asuransiPegawai.getJenis())) {
			cbJenis.setSelectedItem(ciPegawai);
		} else if (AsuransiPegawai.JENIS_KHUSUS_UNTUK_KELUARGA.equals(asuransiPegawai.getJenis())) {
			cbJenis.setSelectedItem(ciKeluarga);
		} else {
			cbJenis.setSelectedItem(ciKeduanya); // Default
		}
		
		row.appendChild(cbJenis);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(asuransiPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tarif"));
		row.appendChild(tarif = new MyDoublebox(asuransiPegawai.getTarif()));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, kolom Nama Asuransi Pegawai belum diisi. Langkah yang dapat dilakukan: (1) isikan Nama Asuransi Pegawai pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaAsuransiPegawai();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Nama Asuransi Pegawai yang Bapak/Ibu masukkan sudah terdaftar di dalam basis data. Langkah yang dapat dilakukan: (1) gunakan Nama Asuransi Pegawai yang berbeda; (2) periksa kembali daftar asuransi pegawai yang telah ada; (3) simpan kembali data ini.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (asuransiPegawai.getId() != null) {
			asuransiPegawai = (AsuransiPegawai) session.load(AsuransiPegawai.class, asuransiPegawai.getId());
		}

		asuransiPegawai.setKode(kode.getValue());
		asuransiPegawai.setNama(nama.getValue());
		
		// MENYIMPAN NILAI JENIS
		if (cbJenis.getSelectedItem() != null) {
			asuransiPegawai.setJenis((String) cbJenis.getSelectedItem().getValue());
		} else {
			asuransiPegawai.setJenis(AsuransiPegawai.JENIS_UNTUK_KEDUANYA); // fallback
		}
		
		asuransiPegawai.setKeterangan(keterangan.getValue());
		asuransiPegawai.setTarif(tarif.getValue());

		Common.refreshSaveOrUpdate(session, asuransiPegawai);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AsuransiPegawai.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<AsuransiPegawai> asuransiPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(asuransiPegawai);
		grid.setRowRenderer(new AsuransiPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaAsuransiPegawai() {
		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(AsuransiPegawai.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.asuransiPegawai.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.asuransiPegawai.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}