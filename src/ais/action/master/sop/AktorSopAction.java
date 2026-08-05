package ais.action.master.sop;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
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
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.sop.AktorSop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AktorSopAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 *
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private AktorSop aktorSop;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private Textbox jenisPengguna;
	private Textbox usernamePengguna;

	private MyCheckboxConfig semuaPegawai;
	private MyCheckboxConfig semuaGuru;
	private MyCheckboxConfig semuaDosen;
	private MyCheckboxConfig semuaMahasiswa;
	private MyCheckboxConfig semuaSiswa;

	private MyCheckboxConfig kaprodiPengajuMahasiswa;
	private MyCheckboxConfig dekanPengajuMahasiswa;
	private MyCheckboxConfig kaprodiPengajuDosen;
	private MyCheckboxConfig dekanPengajuDosen;
	private MyCheckboxConfig dosenPaPengajuMahasiswa;
	private MyCheckboxConfig semuaAtasanLangsungPegawai;
	private MyCheckboxConfig semuaAtasanPejabat;

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
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "jenisPengguna", "usernamePengguna", "keterangan",
				"aktif", "semuaPegawai", "semuaGuru", "semuaDosen", "semuaMahasiswa", "semuaSiswa",
				"kaprodiPengajuMahasiswa", "dekanPengajuMahasiswa", "kaprodiPengajuDosen", "dekanPengajuDosen",
				"semuaAtasanLangsungPegawai", "semuaAtasanPejabat" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(AktorSop.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AktorSop.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class AktorSopRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final AktorSop aktorSop = (AktorSop) arg1;
			new Label(aktorSop.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(AktorSop.class, aktorSop, aktorSop.getNama()).setParent(arg0);
			new Label(aktorSop.getJenisPengguna().isEmpty() ? "Tidak ditentukan" : aktorSop.getJenisPengguna())
					.setParent(arg0);
			new Label(aktorSop.getUsernamePengguna().isEmpty() ? "Tidak ditentukan" : aktorSop.getUsernamePengguna())
					.setParent(arg0);
			new Label(aktorSop.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(aktorSop.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					aktorSop.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(aktorSop);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, aktorSop, AktorSopAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new AktorSop());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		aktorSop = (AktorSop) obj;
		init(aktorSop);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(AktorSop aktorSop) {
		this.aktorSop = aktorSop;
		addWindow.setTitle(aktorSop.getId() == null ? "Tambah Aktor SOP" : "Ubah Aktor SOP");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Aktor SOP *"));
		row.appendChild(kode = new Textbox(aktorSop.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Aktor SOP *"));
		row.appendChild(nama = new Textbox(aktorSop.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan oleh jenis pengguna"));
		row.appendChild(jenisPengguna = new Textbox(aktorSop.getJenisPengguna()));
		jenisPengguna.setWidth("90%");
		jenisPengguna.setRows(2);

		Common.initKeterangan(rows,
				"Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila boleh diajukan oleh semua aktor pengguna");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan oleh username pengguna"));
		row.appendChild(usernamePengguna = new Textbox(aktorSop.getUsernamePengguna()));
		usernamePengguna.setWidth("90%");
		usernamePengguna.setRows(2);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Username Pengguna",
				"/img/user_male_add.png");

		final MyFormRow rowAmbilPengguna = new MyFormRow();
		rowAmbilPengguna.setParent(rows);
		rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
		rowAmbilPengguna.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								usernamePengguna.setValue(usernamePengguna.getValue()
										+ (usernamePengguna.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		Common.initKeterangan(rows,
				"Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila boleh diajukan oleh semua username pengguna");

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(semuaPegawai = new MyCheckboxConfig("Berlaku untuk semua pegawai"));
		semuaPegawai.setChecked(aktorSop.getSemuaPegawai());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				semuaAtasanLangsungPegawai = new MyCheckboxConfig("Berlaku untuk semua atasan pegawai langsung"));
		semuaAtasanLangsungPegawai.setChecked(aktorSop.getSemuaAtasanLangsungPegawai());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(semuaAtasanPejabat = new MyCheckboxConfig("Berlaku untuk semua atasan jabatan"));
		semuaAtasanPejabat.setChecked(aktorSop.getSemuaAtasanPejabat());

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(semuaDosen = new MyCheckboxConfig("Berlaku untuk semua dosen"));
		semuaDosen.setChecked(aktorSop.getSemuaDosen());

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(semuaGuru = new MyCheckboxConfig("Berlaku untuk semua guru"));
		semuaGuru.setChecked(aktorSop.getSemuaGuru());

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(semuaMahasiswa = new MyCheckboxConfig("Berlaku untuk semua mahasiswa"));
		semuaMahasiswa.setChecked(aktorSop.getSemuaMahasiswa());

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(semuaSiswa = new MyCheckboxConfig("Berlaku untuk semua siswa"));
		semuaSiswa.setChecked(aktorSop.getSemuaSiswa());

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				kaprodiPengajuMahasiswa = new MyCheckboxConfig("Berlaku untuk semua kaprodi pengaju mahasiswa"));
		kaprodiPengajuMahasiswa.setChecked(aktorSop.getKaprodiPengajuMahasiswa());

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(dekanPengajuMahasiswa = new MyCheckboxConfig("Berlaku untuk semua dekan pengaju mahasiswa"));
		dekanPengajuMahasiswa.setChecked(aktorSop.getDekanPengajuMahasiswa());

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(dosenPaPengajuMahasiswa = new MyCheckboxConfig(
				"Berlaku untuk semua dosen pembimbing akademik (PA) pengaju mahasiswa"));
		dosenPaPengajuMahasiswa.setChecked(aktorSop.getDosenPaPengajuMahasiswa());

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(kaprodiPengajuDosen = new MyCheckboxConfig("Berlaku untuk semua kaprodi pengaju dosen"));
		kaprodiPengajuDosen.setChecked(aktorSop.getKaprodiPengajuDosen());

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(dekanPengajuDosen = new MyCheckboxConfig("Berlaku untuk semua dekan pengaju dosen"));
		dekanPengajuDosen.setChecked(aktorSop.getDekanPengajuDosen());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(aktorSop.getKeterangan()));
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
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Aktor SOP belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Kode Aktor SOP; (2) isikan kode yang unik dan sesuai ketentuan; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Aktor SOP belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Aktor SOP; (2) isikan nama aktor SOP yang sesuai; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (aktorSop.getId() != null) {
			aktorSop = (AktorSop) session.load(AktorSop.class, aktorSop.getId());

		}

		aktorSop.setKode(kode.getValue());
		aktorSop.setNama(nama.getValue());
		aktorSop.setKeterangan(keterangan.getValue());
		aktorSop.setJenisPengguna(jenisPengguna.getValue());
		aktorSop.setUsernamePengguna(usernamePengguna.getValue());

		aktorSop.setSemuaDosen(semuaDosen.isChecked());
		aktorSop.setSemuaGuru(semuaGuru.isChecked());
		aktorSop.setSemuaMahasiswa(semuaMahasiswa.isChecked());
		aktorSop.setSemuaPegawai(semuaPegawai.isChecked());
		aktorSop.setSemuaSiswa(semuaSiswa.isChecked());

		aktorSop.setKaprodiPengajuMahasiswa(kaprodiPengajuMahasiswa.isChecked());
		aktorSop.setKaprodiPengajuDosen(kaprodiPengajuDosen.isChecked());
		aktorSop.setDekanPengajuDosen(dekanPengajuDosen.isChecked());
		aktorSop.setDekanPengajuMahasiswa(dekanPengajuMahasiswa.isChecked());
		aktorSop.setDosenPaPengajuMahasiswa(dosenPaPengajuMahasiswa.isChecked());
		aktorSop.setSemuaAtasanLangsungPegawai(semuaAtasanLangsungPegawai.isChecked());
		aktorSop.setSemuaAtasanPejabat(semuaAtasanPejabat.isChecked());
		Common.refreshSaveOrUpdate(session, aktorSop);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AktorSop.class)
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

		List<AktorSop> aktorSop = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(aktorSop);
		grid.setRowRenderer(new AktorSopRenderer());
		grid.setModelCheckMobile(strset);

	}

}
