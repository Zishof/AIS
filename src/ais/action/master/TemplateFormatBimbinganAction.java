package ais.action.master;

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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.FormatNilaiProposalSkripsi;
import ais.database.model.GeneralValueObject;
import ais.database.model.TemplateFormatBimbingan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TemplateFormatBimbinganAction extends GenericAutowireComposer
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
	private MyIntbox setelahHari;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private TemplateFormatBimbingan templateFormatBimbingan;
	private MyToolbarbuttonConfig add;

	private FormatNilaiProposalSkripsi formatNilaiProposalSkripsi;
	private MyCheckboxConfig bolehDiubahOlehMahasiswa;
	private MyCheckboxConfig bolehDiubahOlehDosen;

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

		String pFnps = execution.getParameter("formatNilaiProposalSkripsi");
		// Parameter bisa berupa STRING "null"/kosong/non-angka (mis. dari Include/URL) yang lolos
		// cek != null lalu menggagalkan Long.parseLong -> NumberFormatException. Validasi dulu.
		if (pFnps != null && !pFnps.trim().isEmpty() && !"null".equalsIgnoreCase(pFnps.trim())
				&& Common.isNumber(pFnps.trim())) {
			formatNilaiProposalSkripsi = (FormatNilaiProposalSkripsi) HibernateUtil.currentSession()
					.createCriteria(FormatNilaiProposalSkripsi.class)
					.add(Restrictions.idEq(Long.parseLong(pFnps.trim())))
					.uniqueResult();
		}

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

		String[] contents = new String[] { "id", "nama", "formatNilaiProposalSkripsi", "setelahHari",
				"bolehDiubahOlehMahasiswa", "bolehDiubahOlehDosen", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, TemplateFormatBimbingan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		if (formatNilaiProposalSkripsi != null) {
			TemplateFormatBimbingan.createDefaultTemplateFormatBimbingan(HibernateUtil.currentSession(),
					formatNilaiProposalSkripsi);
		}
		onSearchDefault(null);
	}

	class TemplateFormatBimbinganRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final TemplateFormatBimbingan templateFormatBimbingan = (TemplateFormatBimbingan) arg1;

			if (templateFormatBimbingan.getDefaultAwalBimbingan()) {
				TemplateFormatBimbingan.templateFormatBimbingans
						.put(templateFormatBimbingan.getFormatNilaiProposalSkripsi().getId(), templateFormatBimbingan);
			}

			RevisiHelper.createNewRevisi(TemplateFormatBimbingan.class, templateFormatBimbingan,
					templateFormatBimbingan.getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(templateFormatBimbingan.getSetelahHari())).setParent(arg0);
			new Label(templateFormatBimbingan.getBolehDiubahOlehDosen() ? "Ya" : "Tidak").setParent(arg0);
			new Label(templateFormatBimbingan.getBolehDiubahOlehMahasiswa() ? "Ya" : "Tidak").setParent(arg0);
			new Label(templateFormatBimbingan.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, templateFormatBimbingan.getDefaultAwalBimbingan() ? false : delete,
					templateFormatBimbingan, TemplateFormatBimbinganAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new TemplateFormatBimbingan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		templateFormatBimbingan = (TemplateFormatBimbingan) obj;
		init(templateFormatBimbingan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(TemplateFormatBimbingan templateFormatBimbingan) {
		this.templateFormatBimbingan = templateFormatBimbingan;
		addWindow.setTitle(templateFormatBimbingan.getId() == null ? "Tambah Template Format Bimbingan" : "Ubah Template Format Bimbingan");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Template"));
		row.appendChild(nama = new Textbox(templateFormatBimbingan.getNama()));
		nama.setWidth("90%");

		Common.initKeterangan(rows, "Contoh : BAB I, BAB II, BAB III, BAB IV, Sidang");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Setelah Hari (dari pertemuan awal)"));
		row.appendChild(setelahHari = new MyIntbox(templateFormatBimbingan.getSetelahHari()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(bolehDiubahOlehMahasiswa = new MyCheckboxConfig("Boleh Diubah Oleh Mahasiswa"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(bolehDiubahOlehDosen = new MyCheckboxConfig("Boleh Diubah Oleh Dosen"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(templateFormatBimbingan.getKeterangan()));
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
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Template",
					"Kolom Nama Template belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Template.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaTemplateFormatBimbingan();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Template",
					"Nama Template sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama template yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (templateFormatBimbingan.getId() != null) {
			templateFormatBimbingan = (TemplateFormatBimbingan) session.load(TemplateFormatBimbingan.class,
					templateFormatBimbingan.getId());
		} else {
			templateFormatBimbingan.setDefaultAwalBimbingan(false);
		}
		templateFormatBimbingan.setFormatNilaiProposalSkripsi(formatNilaiProposalSkripsi);
		templateFormatBimbingan.setNama(nama.getValue());
		templateFormatBimbingan.setKeterangan(keterangan.getValue());
		templateFormatBimbingan.setSetelahHari(setelahHari.getValue());
		templateFormatBimbingan.setBolehDiubahOlehDosen(bolehDiubahOlehDosen.isChecked());
		templateFormatBimbingan.setBolehDiubahOlehMahasiswa(bolehDiubahOlehMahasiswa.isChecked());

		Common.refreshSaveOrUpdate(session, templateFormatBimbingan);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		// KE-14: createCriteria via currentSession() butuh transaksi aktif (dipanggil saat include/render
		// doAfterCompose -> tak ada tx). Pakai currentNativeSession() (tak butuh tx-wrapper, ditutup FilterJSP).
		Session session = HibernateUtil.currentNativeSession();
		Criteria criteria = session.createCriteria(TemplateFormatBimbingan.class)
				.add(Restrictions.eq("formatNilaiProposalSkripsi", formatNilaiProposalSkripsi));

		if (order)
			criteria.addOrder(Order.asc("setelahHari"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TemplateFormatBimbingan> templateFormatBimbingan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(templateFormatBimbingan);
		grid.setRowRenderer(new TemplateFormatBimbinganRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaTemplateFormatBimbingan() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(TemplateFormatBimbingan.class)
				.add(Restrictions.eq("formatNilaiProposalSkripsi", formatNilaiProposalSkripsi))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.templateFormatBimbingan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.templateFormatBimbingan.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
