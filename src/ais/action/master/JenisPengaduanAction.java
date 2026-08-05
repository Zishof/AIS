package ais.action.master;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import ais.action.master.surat.helper.AmbilDataNomorSuratBanbox;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisPengaduan;
import ais.database.model.KelompokParameterTambahanPengaduan;
import ais.database.model.file.LampiranLain;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenisPengaduanAction extends GenericAutowireComposer
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
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisPengaduan jenisPengaduan;
	private MyToolbarbuttonConfig add;
	private Set<KelompokParameterTambahanPengaduan> selectedKelompokParameterTambahanPengaduan;
	protected LampiranLain lampiran;
	private AmbilDataNomorSuratBanbox nomorSurat;

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "nomorSurat", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisPengaduan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class JenisPengaduanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisPengaduan jenisPengaduan = (JenisPengaduan) arg1;

			RevisiHelper.createNewRevisi(JenisPengaduan.class, jenisPengaduan, jenisPengaduan.getNama())
					.setParent(arg0);
			new Label(jenisPengaduan.getNomorSurat() == null ? "" : jenisPengaduan.getNomorSurat().getContohFormat())
					.setParent(arg0);
			new Label(jenisPengaduan.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisPengaduan.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPengaduan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPengaduan);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisPengaduan, JenisPengaduanAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisPengaduan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPengaduan = (JenisPengaduan) obj;
		init(jenisPengaduan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisPengaduan jenisPengaduan) {
		this.jenisPengaduan = jenisPengaduan;
		addWindow.setTitle(jenisPengaduan.getId() == null ? "Tambah Jenis Pengaduan" : "Ubah Jenis Pengaduan");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengaduan *"));
		row.appendChild(nama = new Textbox(jenisPengaduan.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Agenda *"));
		row.appendChild(nomorSurat = new AmbilDataNomorSuratBanbox());
		nomorSurat.setAttribute("nomorSurat", jenisPengaduan.getNomorSurat());
		nomorSurat.setValue(jenisPengaduan.getNomorSurat() == null ? "" : jenisPengaduan.getNomorSurat().getNama());
		nomorSurat.setWidth("90%");
		nomorSurat.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPengaduan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lampiran = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan (jrxml atau jasper)"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenisPengaduan.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGADUAN, "File Laporan jrxml", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		initKelompokParameterTambahanPengaduan(rows);

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

	@SuppressWarnings("deprecation")
	private void initKelompokParameterTambahanPengaduan(Rows rows) {
		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Parameter Pengaduan");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		@SuppressWarnings("unchecked")
		Map<Long, KelompokParameterTambahanPengaduan> kelompokParameterTambahanPengaduans = ConstantValues
				.ambilBerdasarClass(KelompokParameterTambahanPengaduan.class);

		if (jenisPengaduan != null && jenisPengaduan.getId() != null) {
			HibernateUtil.currentSession().refresh(jenisPengaduan);
		}

		selectedKelompokParameterTambahanPengaduan = this.jenisPengaduan.getKelompokParameterTambahanPengaduans();
		Set<Long> ids = new HashSet<Long>();
		for (KelompokParameterTambahanPengaduan v : selectedKelompokParameterTambahanPengaduan) {
			ids.add(v.getId());
		}

		System.out.println("ids ->" + ids);

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final KelompokParameterTambahanPengaduan kelompokParameterTambahanPengaduan : kelompokParameterTambahanPengaduans
				.values()) {
			final Checkbox checkbox = new Checkbox(kelompokParameterTambahanPengaduan.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(ids.contains(kelompokParameterTambahanPengaduan.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedKelompokParameterTambahanPengaduan.add(kelompokParameterTambahanPengaduan);
					} else {

						for (KelompokParameterTambahanPengaduan a : selectedKelompokParameterTambahanPengaduan) {
							if (a.getId().equals(kelompokParameterTambahanPengaduan.getId())) {
								selectedKelompokParameterTambahanPengaduan.remove(a);
								break;
							}
						}

					}

					System.out.println("selectedKelompokParameterTambahanPengaduan => "
							+ selectedKelompokParameterTambahanPengaduan);
				}
			});
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Pengaduan",
					"Kolom Nama Jenis Pengaduan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Jenis Pengaduan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nomorSurat.getAttribute("nomorSurat") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Format Nomor Agenda",
					"Kolom Format Nomor Agenda belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Format Nomor Agenda.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPengaduan.getId() != null) {
			jenisPengaduan = (JenisPengaduan) session.load(JenisPengaduan.class, jenisPengaduan.getId());

		}

		jenisPengaduan.setNama(nama.getValue());
		jenisPengaduan.setKeterangan(keterangan.getValue());
		jenisPengaduan.setKelompokParameterTambahanPengaduans(selectedKelompokParameterTambahanPengaduan);
		jenisPengaduan.setNomorSurat((NomorSurat) nomorSurat.getAttribute("nomorSurat"));
		Common.refreshSaveOrUpdate(session, jenisPengaduan);

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (lampiran != null && lampiran.getId() != null) {
				session.refresh(lampiran);
				lampiran.setRef(jenisPengaduan.getId());

				session.getTransaction().begin();
				session.update(lampiran);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPengaduan.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPengaduan> jenisPengaduan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisPengaduan);
		grid.setRowRenderer(new JenisPengaduanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
