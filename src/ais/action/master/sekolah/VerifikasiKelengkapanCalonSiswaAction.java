package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zul.Combobox;
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
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.VerifikasiKelengkapanCalonSiswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class VerifikasiKelengkapanCalonSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa;
	private MyToolbarbuttonConfig add;

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
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);
		Session session = HibernateUtil.currentSession();

		Sekolah sekolah = SekolahUtil.getSekolah();
		Yayasan yayasan = SekolahUtil.getYayasan();

		int count = ((Number) session.createCriteria(VerifikasiKelengkapanCalonSiswa.class)
				.add(sekolah != null && sekolah.getId() != null ? Restrictions.eq("sekolah", sekolah)
						: Restrictions.sqlRestriction("true"))
				.add(yayasan != null && yayasan.getId() != null ? Restrictions.eq("yayasan", yayasan)
						: Restrictions.sqlRestriction("true"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {

			String[] verifikasiKelengkapanCalonSiswaes = new String[] {
					"Fotocopy Ijazah atau Surat Keterangan Hasil Ujian (SKHU) yang telah dilegalisir dari sekolah",
					"Fotocopy raport",
					"Bukti prestasi asli dan 1 (satu ) lembar fotocopy sertifikat kejuaraan/lomba/olimpiade bidang sains, teknologi, serta seni lukis dan/atau seni rupa ditingkat nasional/internasional dan olahraga sesuai data yang diupload sebanyak 1 (satu) lembar",
					"Fotocopy identitas diri (Kartu Pelajar/KTP/SIM)",
					"Fotocopy Kartu Keluarga yang telah dilegalisir pejabat yang berwenang",
					"Surat keterangan tidak mampu dari lurah setempat dan kartu Raskin atau kartu penerima BLSM bagi peserta dari keluarga tidak mampu",
					"Pas photo warna terbaru." };
			for (String k : verifikasiKelengkapanCalonSiswaes) {
				if (k != null) {
					VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa = new VerifikasiKelengkapanCalonSiswa();
					verifikasiKelengkapanCalonSiswa.setNama(k.toString().trim());
					verifikasiKelengkapanCalonSiswa.setSekolah(sekolah);
					verifikasiKelengkapanCalonSiswa.setYayasan(yayasan);
					verifikasiKelengkapanCalonSiswa.setKeterangan("" + k.toString().trim());
					session.save(verifikasiKelengkapanCalonSiswa);
					session.flush();
				}
			}

		}

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

		String[] contents = new String[] { "id", "nama", "sekolah", "yayasan", "wajibUploadSebelumUjian", "wajibUploadSebelumInterview", "wajibVerifikasiSebelumUjian", "wajibVerifikasiSebelumInterview" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, VerifikasiKelengkapanCalonSiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class VerifikasiKelengkapanCalonSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa = (VerifikasiKelengkapanCalonSiswa) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(VerifikasiKelengkapanCalonSiswa.class, verifikasiKelengkapanCalonSiswa,
					verifikasiKelengkapanCalonSiswa.getNama())).setParent(arg0);
			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, verifikasiKelengkapanCalonSiswa.getId(),
					VerifikasiKelengkapanCalonSiswa.class.getName(), "Lampiran", false, null, null, false, false, false,
					false);

			new Label(verifikasiKelengkapanCalonSiswa.getSekolah() == null ? ""
					: verifikasiKelengkapanCalonSiswa.getSekolah().getNama()).setParent(arg0);
			new Label(verifikasiKelengkapanCalonSiswa.getYayasan() == null ? ""
					: verifikasiKelengkapanCalonSiswa.getYayasan().getNama()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(verifikasiKelengkapanCalonSiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					verifikasiKelengkapanCalonSiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(verifikasiKelengkapanCalonSiswa);
				}
			});

			final MyCheckboxConfig cbUjian = new MyCheckboxConfig("Wajib Upload sebelum Ujian");
			cbUjian.setDisabled(!edit);
			cbUjian.setChecked(verifikasiKelengkapanCalonSiswa.getWajibUploadSebelumUjian());
			cbUjian.setParent(arg0);
			cbUjian.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					verifikasiKelengkapanCalonSiswa.setWajibUploadSebelumUjian(cbUjian.isChecked());
					Common.refreshSaveOrUpdate(verifikasiKelengkapanCalonSiswa);
				}
			});

			final MyCheckboxConfig cbInterview = new MyCheckboxConfig("Wajib Upload sebelum Interview");
			cbInterview.setDisabled(!edit);
			cbInterview.setChecked(verifikasiKelengkapanCalonSiswa.getWajibUploadSebelumInterview());
			cbInterview.setParent(arg0);
			cbInterview.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					verifikasiKelengkapanCalonSiswa.setWajibUploadSebelumInterview(cbInterview.isChecked());
					Common.refreshSaveOrUpdate(verifikasiKelengkapanCalonSiswa);
				}
			});

			final MyCheckboxConfig cbVerifUjian = new MyCheckboxConfig("Wajib Verifikasi sebelum Ujian");
			cbVerifUjian.setDisabled(!edit);
			cbVerifUjian.setChecked(verifikasiKelengkapanCalonSiswa.getWajibVerifikasiSebelumUjian());
			cbVerifUjian.setParent(arg0);
			cbVerifUjian.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					verifikasiKelengkapanCalonSiswa.setWajibVerifikasiSebelumUjian(cbVerifUjian.isChecked());
					Common.refreshSaveOrUpdate(verifikasiKelengkapanCalonSiswa);
				}
			});

			final MyCheckboxConfig cbVerifInterview = new MyCheckboxConfig("Wajib Verifikasi sebelum Interview");
			cbVerifInterview.setDisabled(!edit);
			cbVerifInterview.setChecked(verifikasiKelengkapanCalonSiswa.getWajibVerifikasiSebelumInterview());
			cbVerifInterview.setParent(arg0);
			cbVerifInterview.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					verifikasiKelengkapanCalonSiswa.setWajibVerifikasiSebelumInterview(cbVerifInterview.isChecked());
					Common.refreshSaveOrUpdate(verifikasiKelengkapanCalonSiswa);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, verifikasiKelengkapanCalonSiswa,
					VerifikasiKelengkapanCalonSiswaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new VerifikasiKelengkapanCalonSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		verifikasiKelengkapanCalonSiswa = (VerifikasiKelengkapanCalonSiswa) obj;
		init(verifikasiKelengkapanCalonSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected LampiranLain lainSiswa;
	private Combobox yayasan;
	private Combobox sekolah;

	private void init(VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa) {
		this.verifikasiKelengkapanCalonSiswa = verifikasiKelengkapanCalonSiswa;
		addWindow.setTitle(verifikasiKelengkapanCalonSiswa.getId() == null ? "Tambah Verifikasi Kelengkapan Calon Siswa" : "Ubah Verifikasi Kelengkapan Calon Siswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Verifikasi Kelengkapan Berkas *"));
		row.appendChild(nama = new Textbox(verifikasiKelengkapanCalonSiswa.getNama()));
		nama.setWidth("90%");
		nama.setRows(3);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, verifikasiKelengkapanCalonSiswa.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, verifikasiKelengkapanCalonSiswa.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(verifikasiKelengkapanCalonSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Berkas"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, verifikasiKelengkapanCalonSiswa.getId(),
				VerifikasiKelengkapanCalonSiswa.class.getName(), "Lampiran", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainSiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran lebih dari satu file, zip dulu semua file tersebut");

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
			MyMessageboxConfig.show("Nama Verifikasi Kelengkapan Calon Siswa harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Yayasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (verifikasiKelengkapanCalonSiswa.getId() != null) {
			verifikasiKelengkapanCalonSiswa = (VerifikasiKelengkapanCalonSiswa) session
					.load(VerifikasiKelengkapanCalonSiswa.class, verifikasiKelengkapanCalonSiswa.getId());

		}

		verifikasiKelengkapanCalonSiswa.setNama(nama.getValue());
		verifikasiKelengkapanCalonSiswa.setKeterangan(keterangan.getValue());

		verifikasiKelengkapanCalonSiswa.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));
		verifikasiKelengkapanCalonSiswa.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, verifikasiKelengkapanCalonSiswa);

		if (lainSiswa != null && lainSiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainSiswa);
				lainSiswa.setRef(verifikasiKelengkapanCalonSiswa.getId());

				session.getTransaction().begin();
				session.update(lainSiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(VerifikasiKelengkapanCalonSiswa.class)

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswa = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				VerifikasiKelengkapanCalonSiswa.class);
		ListModel strset = new SimpleListModel(verifikasiKelengkapanCalonSiswa);
		grid.setRowRenderer(new VerifikasiKelengkapanCalonSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
