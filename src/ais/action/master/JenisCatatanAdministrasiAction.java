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
import ais.database.model.JenisCatatanAdministrasi;
import ais.database.model.KelompokParameterTambahanCatatanAdministrasi;
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

public class JenisCatatanAdministrasiAction extends GenericAutowireComposer
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

	private JenisCatatanAdministrasi jenisCatatanAdministrasi;
	private MyToolbarbuttonConfig add;
	private Set<KelompokParameterTambahanCatatanAdministrasi> selectedKelompokParameterTambahanCatatanAdministrasi;
	protected LampiranLain lampiran;
	private AmbilDataNomorSuratBanbox nomorSurat;
	protected LampiranLain lampiran1;

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

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisCatatanAdministrasi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class JenisCatatanAdministrasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisCatatanAdministrasi jenisCatatanAdministrasi = (JenisCatatanAdministrasi) arg1;

			RevisiHelper.createNewRevisi(JenisCatatanAdministrasi.class, jenisCatatanAdministrasi,
					jenisCatatanAdministrasi.getNama()).setParent(arg0);
			new Label(jenisCatatanAdministrasi.getNomorSurat() == null ? ""
					: jenisCatatanAdministrasi.getNomorSurat().getContohFormat()).setParent(arg0);
			new Label(jenisCatatanAdministrasi.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisCatatanAdministrasi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisCatatanAdministrasi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisCatatanAdministrasi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisCatatanAdministrasi, JenisCatatanAdministrasiAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisCatatanAdministrasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisCatatanAdministrasi = (JenisCatatanAdministrasi) obj;
		init(jenisCatatanAdministrasi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisCatatanAdministrasi jenisCatatanAdministrasi) {
		this.jenisCatatanAdministrasi = jenisCatatanAdministrasi;
		addWindow.setTitle(jenisCatatanAdministrasi.getId() == null ? "Tambah Jenis Catatan Administrasi" : "Ubah Jenis Catatan Administrasi");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Catatan Administrasi *"));
		row.appendChild(nama = new Textbox(jenisCatatanAdministrasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Agenda *"));
		row.appendChild(nomorSurat = new AmbilDataNomorSuratBanbox());
		nomorSurat.setAttribute("nomorSurat", jenisCatatanAdministrasi.getNomorSurat());
		nomorSurat.setValue(jenisCatatanAdministrasi.getNomorSurat() == null ? ""
				: jenisCatatanAdministrasi.getNomorSurat().getNama());
		nomorSurat.setWidth("90%");
		nomorSurat.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisCatatanAdministrasi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lampiran1 = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan Form (jrxml atau jasper)"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenisCatatanAdministrasi.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_ADMINISTRASI, "File Laporan form jrxml", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran1 = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		lampiran = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan Rekap (jrxml atau jasper)"));
		hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenisCatatanAdministrasi.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_CATATAN_ADMINISTRASI, "File Laporan rekap jrxml", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		initKelompokParameterTambahanCatatanAdministrasi(rows);

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
	private void initKelompokParameterTambahanCatatanAdministrasi(Rows rows) {
		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Parameter Catatan Administrasi");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		@SuppressWarnings("unchecked")
		Map<Long, KelompokParameterTambahanCatatanAdministrasi> kelompokParameterTambahanCatatanAdministrasis = ConstantValues
				.ambilBerdasarClass(KelompokParameterTambahanCatatanAdministrasi.class);

		if (jenisCatatanAdministrasi != null && jenisCatatanAdministrasi.getId() != null) {
			HibernateUtil.currentSession().refresh(jenisCatatanAdministrasi);
		}

		selectedKelompokParameterTambahanCatatanAdministrasi = this.jenisCatatanAdministrasi
				.getKelompokParameterTambahanCatatanAdministrasis();
		Set<Long> ids = new HashSet<Long>();
		for (KelompokParameterTambahanCatatanAdministrasi v : selectedKelompokParameterTambahanCatatanAdministrasi) {
			ids.add(v.getId());
		}

		System.out.println("ids ->" + ids);

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final KelompokParameterTambahanCatatanAdministrasi kelompokParameterTambahanCatatanAdministrasi : kelompokParameterTambahanCatatanAdministrasis
				.values()) {
			final Checkbox checkbox = new Checkbox(kelompokParameterTambahanCatatanAdministrasi.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(ids.contains(kelompokParameterTambahanCatatanAdministrasi.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedKelompokParameterTambahanCatatanAdministrasi
								.add(kelompokParameterTambahanCatatanAdministrasi);
					} else {

						for (KelompokParameterTambahanCatatanAdministrasi a : selectedKelompokParameterTambahanCatatanAdministrasi) {
							if (a.getId().equals(kelompokParameterTambahanCatatanAdministrasi.getId())) {
								selectedKelompokParameterTambahanCatatanAdministrasi.remove(a);
								break;
							}
						}

					}

					System.out.println("selectedKelompokParameterTambahanCatatanAdministrasi => "
							+ selectedKelompokParameterTambahanCatatanAdministrasi);
				}
			});
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis catatan administrasi",
					"Kolom Nama Jenis catatan administrasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Jenis catatan administrasi.",
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
		if (jenisCatatanAdministrasi.getId() != null) {
			jenisCatatanAdministrasi = (JenisCatatanAdministrasi) session.load(JenisCatatanAdministrasi.class,
					jenisCatatanAdministrasi.getId());

		}

		jenisCatatanAdministrasi.setNama(nama.getValue());
		jenisCatatanAdministrasi.setKeterangan(keterangan.getValue());
		jenisCatatanAdministrasi
				.setKelompokParameterTambahanCatatanAdministrasis(selectedKelompokParameterTambahanCatatanAdministrasi);
		jenisCatatanAdministrasi.setNomorSurat((NomorSurat) nomorSurat.getAttribute("nomorSurat"));
		Common.refreshSaveOrUpdate(session, jenisCatatanAdministrasi);

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (lampiran != null && lampiran.getId() != null) {
				session.refresh(lampiran);
				lampiran.setRef(jenisCatatanAdministrasi.getId());

				session.getTransaction().begin();
				session.update(lampiran);
				session.getTransaction().commit();
			}

			if (lampiran1 != null && lampiran1.getId() != null) {
				session.refresh(lampiran1);
				lampiran1.setRef(jenisCatatanAdministrasi.getId());

				session.getTransaction().begin();
				session.update(lampiran1);
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
		Criteria criteria = session.createCriteria(JenisCatatanAdministrasi.class);

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

		List<JenisCatatanAdministrasi> jenisCatatanAdministrasi = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisCatatanAdministrasi);
		grid.setRowRenderer(new JenisCatatanAdministrasiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
