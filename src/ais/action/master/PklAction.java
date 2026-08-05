package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
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
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.dashboard.admin.DashboardPklMahasiswa;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.pkl.PklUntukMahasiswaAction;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.PklDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisAktfitasMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatPkl;
import ais.database.model.Perkuliahan;
import ais.database.model.Pkl;
import ais.database.model.Tbmuser;
import ais.database.model.pkl.KomponenPenilaianPkl;
import ais.database.model.pkl.PersyaratanPkl;
import ais.database.model.pkl.PklPunyaKomponenPenilaianPkl;
import ais.database.model.pkl.PklPunyaPersyaratan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class PklAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	private Textbox searchketerangan;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private AmbilDataMahasiswaBanbox searchmahasiswa;

	private Textbox nama_kelompok;
	private MyDatebox tanggal_mulai;
	private MyDatebox tanggal_selesai;
	private Textbox keterangan;
	private Intbox minimalSksBolehIkutPkl;
	private MyDoublebox minimalIpkBolehIkutPkl;

	private MyCheckboxConfig aktifkanSyaratLain;
	private Intbox minimalSksBolehIkutPkl2;
	private MyDoublebox minimalIpkBolehIkutPkl2;

	private MyCheckboxConfig harusBayar;

	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;

	private Pkl pkl;
	private Textbox kodeItemBiaya;

	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;
	private Combobox tahunAkademik;
	private Combobox semester;

	private List<KomponenPenilaianPkl> selectedKomponenPenilaianPkl;
	private List<PersyaratanPkl> selectedPersyaratanPkl;

	protected Tabpanel statistik;
	private Textbox nimMhsTanpaBiaya;
	private Combobox jenisAktfitasMahasiswa;
	private MyCheckboxConfig mahasiswaBolehMerubahAgenda;
	private MyCheckboxConfig dosenBolehMerubahAgenda;

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DashboardPklMahasiswa include = new DashboardPklMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(include, statistik,
				"Statistik PKL", "Gambaran sebaran lokasi PKL, bidang industri, dan capaian penilaian mahasiswa.");
		}
	}

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
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class PklRenderer extends ais.ui.util.MyRowRenderer {

		// private PklHelper pklHelper = new PklHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pkl pkl = (Pkl) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Pkl.class, pkl, pkl.getNama_kelompok())).setParent(arg0);
			new Label(pkl.getJenisAktfitasMahasiswa() == null ? "" : pkl.getJenisAktfitasMahasiswa().getNama())
					.setParent(a);
			new Label(pkl.getJenisAktfitasMahasiswa() == null ? ""
					: pkl.getJenisAktfitasMahasiswa().getKampusMerderka() ? "Kampus Merdeka:YA"
							: "Kampus Merdeka:TIDAK")
					.setParent(a);

			new Label(pkl.getFakultas() == null ? "Semua" : pkl.getFakultas().getNama()).setParent(arg0);
			new Label(pkl.getJurusan() == null ? "Semua" : pkl.getJurusan().getNama()).setParent(arg0);
			new Label(pkl.getProgram() == null || pkl.getProgram().trim().isEmpty() ? "Semua" : pkl.getProgram())
					.setParent(arg0);
			new Label((pkl.getTanggal_mulai() == null ? "" : Common.dateFormat4.get().format(pkl.getTanggal_mulai()))
					+ (pkl.getTanggal_selesai() == null ? ""
							: " s.d " + Common.dateFormat4.get().format(pkl.getTanggal_selesai())))
					.setParent(arg0);
			new Label(pkl.getTahunAkademik() + " / " + pkl.getSemester()).setParent(arg0);
			new Label(pkl.getMinimalSksBolehIkutPkl() + " SKS, IPK min " + pkl.getMinimalIpkBolehIkutPkl())
					.setParent(arg0);

			new Label(pkl.getAktifkanSyaratLain()
					? (pkl.getMinimalSksBolehIkutPkl2() + " SKS, IPK min " + pkl.getMinimalIpkBolehIkutPkl2())
					: "").setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			Session session = HibernateUtil.currentSession();
			if (!pkl.getKodeItemBiaya().trim().isEmpty()) {

				for (String kode : pkl.getKodeItemBiaya().trim().split(",")) {
					ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.simpleObject(session
							.createCriteria(ItemBiaya.class).add(Restrictions.eq("kode", kode.trim())).setMaxResults(1),
							ItemBiaya.class);
					if (itemBiaya != null) {
						new Label(itemBiaya.getKode() + "-" + itemBiaya.getNama()).setParent(hbox);
					}
				}
			} else {
				new Label(ais.common.Common.getBahasaConfig("Tidak ada item biaya")).setParent(hbox);
			}

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pkl);
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

											Common.refreshDelete(pkl);

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
		init(new Pkl());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	private void init(Pkl pkl) throws Exception {
		this.pkl = pkl;
		Common.clear(addWindow);
		addWindow.setTitle("Pkl");
		addWindow.setWidth("98%");
		addWindow.setHeight("98%");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		West west = new West();
		west.setTitle("Pendataan");
		west.setParent(borderlayout);
		west.setWidth("30%");

		Center center = new Center();
		center.setTitle("Komponen Penilaian");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		East east = new East();
		east.setTitle("Form dan Persyaratan");
		east.setParent(borderlayout);
		east.setWidth("40%");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama *"));
		row.appendChild(nama_kelompok = new Textbox(pkl.getNama_kelompok() == null ? "" : pkl.getNama_kelompok()));
		nama_kelompok.setWidth("90%");
		// nama_kelompok.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis / Kampus Merdeka *"));
		row.appendChild(jenisAktfitasMahasiswa = new Combobox());
		Common.insertCombo(jenisAktfitasMahasiswa, "nama", "merupakanKampusMerdeka", JenisAktfitasMahasiswa.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisAktfitasMahasiswa, pkl.getJenisAktfitasMahasiswa());
		jenisAktfitasMahasiswa.setWidth("90%");
		jenisAktfitasMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendaftaran Dimulai"));
		row.appendChild(tanggal_mulai = new MyDatebox(
				pkl.getTanggal_mulai() == null ? ais.ui.util.WaktuUtil.getDate() : pkl.getTanggal_mulai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendaftaran Ditutup"));
		row.appendChild(tanggal_selesai = new MyDatebox(
				pkl.getTanggal_selesai() == null ? ais.ui.util.WaktuUtil.getDate() : pkl.getTanggal_selesai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik (*)"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		Common.selectComboItem(tahunAkademik, pkl.getTahunAkademik());
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		semester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);

		Common.selectComboItem(semester, pkl.getSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (*)"));
		row.appendChild(semester);
		semester.setReadonly(true);

		tanggal_mulai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tanggal_mulai.getValue() != null) {
					Common.selectComboItem(tahunAkademik, Common.getCurrentTahunAkademik(tanggal_mulai.getValue()));
					Common.selectComboItem(semester,
							Common.isNowSemensterGanjil(tanggal_mulai.getValue()) ? Perkuliahan.GANJIL
									: Perkuliahan.GENAP);
				}
			}
		});

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		Tbmuser tbmuser = Common.getCurrentUser();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas, pkl.getFakultas() == null ? tbmuser.ambilFakultas() : pkl.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, pkl.getJurusan() == null ? tbmuser.ambilJurusan() : pkl.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program = Common.initPrograms(null));
		Common.selectComboItem(program, pkl.getProgram());
		program.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(mahasiswaBolehMerubahAgenda = new MyCheckboxConfig("Mahasiswa boleh mengubah agenda"));
		mahasiswaBolehMerubahAgenda.setChecked(pkl.getMahasiswaBolehMerubahAgenda());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(dosenBolehMerubahAgenda = new MyCheckboxConfig("Dosen boleh mengubah agenda"));
		dosenBolehMerubahAgenda.setChecked(pkl.getDosenBolehMerubahAgenda());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(harusBayar = new MyCheckboxConfig("Harus telah membayar"));
		harusBayar.setChecked(pkl.getHarusBayar());

		Common.initKeterangan(rows, "* Mahasiswa harus telah membayar biaya perkuliahan sebelum bisa ikut mendaftar");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal SKS dan IPK"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(minimalSksBolehIkutPkl = new Intbox(pkl.getMinimalSksBolehIkutPkl()));
		hbox.appendChild(minimalIpkBolehIkutPkl = new MyDoublebox(pkl.getMinimalIpkBolehIkutPkl()));

		minimalSksBolehIkutPkl.setCols(2);
		minimalIpkBolehIkutPkl.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktifkan Syarat Lain"));
		row.appendChild(aktifkanSyaratLain = new MyCheckboxConfig());
		aktifkanSyaratLain.setChecked(pkl.getAktifkanSyaratLain());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau juga minimal SKS dan IPK"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(minimalSksBolehIkutPkl2 = new Intbox(pkl.getMinimalSksBolehIkutPkl2()));
		hbox.appendChild(minimalIpkBolehIkutPkl2 = new MyDoublebox(pkl.getMinimalIpkBolehIkutPkl2()));

		minimalSksBolehIkutPkl2.setCols(2);
		minimalIpkBolehIkutPkl2.setCols(2);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				minimalSksBolehIkutPkl2.setDisabled(!aktifkanSyaratLain.isChecked());
				minimalIpkBolehIkutPkl2.setDisabled(!aktifkanSyaratLain.isChecked());
			}
		};
		eventListener.onEvent(null);
		aktifkanSyaratLain.addEventListener("onClick", eventListener);

		MyGrid subGrid = new MyGrid();
		subGrid.setWidth("100%");
		subGrid.setParent(center);
		subGrid.setHeight("100%");

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		Session session = HibernateUtil.currentSession();
		List<KomponenPenilaianPkl> komponenPenilaianPkls = session.createCriteria(KomponenPenilaianPkl.class)
				.createAlias("parent", "parent", Criteria.LEFT_JOIN).addOrder(Order.asc("parent.nomorUrut"))
				.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		TreeMap<KomponenPenilaianPkl, List<KomponenPenilaianPkl>> dataKomponenPenilaian = new TreeMap<KomponenPenilaianPkl, List<KomponenPenilaianPkl>>();
		for (KomponenPenilaianPkl komponenPenilaianPkl : komponenPenilaianPkls) {
			if (komponenPenilaianPkl.getParent() != null) {
				if (!dataKomponenPenilaian.keySet().contains(komponenPenilaianPkl.getParent())) {
					List<KomponenPenilaianPkl> datas = new ArrayList<KomponenPenilaianPkl>();
					datas.add(komponenPenilaianPkl);
					dataKomponenPenilaian.put(komponenPenilaianPkl.getParent(), datas);
				} else {
					dataKomponenPenilaian.get(komponenPenilaianPkl.getParent()).add(komponenPenilaianPkl);
				}
			}
		}

		for (KomponenPenilaianPkl komponenPenilaianPkl : komponenPenilaianPkls) {
			if (komponenPenilaianPkl.getParent() == null && !dataKomponenPenilaian.containsKey(komponenPenilaianPkl)) {
				List<KomponenPenilaianPkl> datas = new ArrayList<KomponenPenilaianPkl>();
				dataKomponenPenilaian.put(komponenPenilaianPkl, datas);
			}
		}

		if (pkl.getId() != null) {
			HibernateUtil.currentSession().refresh(this.pkl);
		}

		if (pkl.getId() != null) {

			selectedKomponenPenilaianPkl = session.createCriteria(PklPunyaKomponenPenilaianPkl.class)
					.setProjection(Projections.groupProperty("komponenPenilaianPkl"))
					.createAlias("komponenPenilaianPkl", "komponenPenilaianPkl")
					.add(Restrictions.or(Restrictions.isNull("komponenPenilaianPkl.aktif"),
							Restrictions.eq("komponenPenilaianPkl.aktif", true)))
					.add(Restrictions.eq("pkl", pkl)).list();

		} else {
			selectedKomponenPenilaianPkl = new ArrayList<KomponenPenilaianPkl>();
		}

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);

		for (final KomponenPenilaianPkl parent : dataKomponenPenilaian.keySet()) {

			final List<KomponenPenilaianPkl> datas = dataKomponenPenilaian.get(parent);
			if (datas.isEmpty()) {
				final Checkbox checkbox = new Checkbox(parent.getNama());
				checkbox.setParent(vboxSkala);
				checkbox.setChecked(selectedKomponenPenilaianPkl.contains(parent));
				checkbox.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (checkbox.isChecked()) {
							selectedKomponenPenilaianPkl.add(parent);
						} else {
							selectedKomponenPenilaianPkl.remove(parent);
						}
					}
				});
			} else {

				final Checkbox checkboxAll = new Checkbox(parent.getNama());
				checkboxAll.setParent(vboxSkala);
				checkboxAll.setChecked(selectedKomponenPenilaianPkl.contains(parent));

				Hbox myHb = new Hbox();
				myHb.setParent(vboxSkala);

				myHb.appendChild(new Space());

				Vbox vboxSkalaSub = new Vbox();
				vboxSkalaSub.setPack("top");
				vboxSkalaSub.setParent(myHb);

				final List<Checkbox> checkboxs = new ArrayList<Checkbox>();
				for (final KomponenPenilaianPkl komponenPenilaianPkl : datas) {

					final Checkbox checkbox = new Checkbox(komponenPenilaianPkl.getNama());
					checkboxs.add(checkbox);
					checkbox.setParent(vboxSkalaSub);
					checkbox.setChecked(selectedKomponenPenilaianPkl.contains(komponenPenilaianPkl));
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (checkbox.isChecked()) {
								selectedKomponenPenilaianPkl.add(komponenPenilaianPkl);
							} else {
								selectedKomponenPenilaianPkl.remove(komponenPenilaianPkl);
							}
						}
					});

				}

				checkboxAll.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (checkboxAll.isChecked()) {
							selectedKomponenPenilaianPkl.add(parent);
						} else {
							selectedKomponenPenilaianPkl.remove(parent);
						}

						for (Checkbox checkbox : checkboxs) {
							checkbox.setChecked(checkboxAll.isChecked());
						}
						for (final KomponenPenilaianPkl komponenPenilaianPkl : datas) {
							if (checkboxAll.isChecked()) {
								selectedKomponenPenilaianPkl.add(komponenPenilaianPkl);
							} else {
								selectedKomponenPenilaianPkl.remove(komponenPenilaianPkl);
							}
						}
					}
				});

			}
		}

		subGrid = new MyGrid();
		subGrid.setWidth("100%");
		subGrid.setParent(east);
		subGrid.setHeight("100%");

		subRows = new Rows();
		subRows.setParent(subGrid);

		List<PersyaratanPkl> persyaratanPkls = session.createCriteria(PersyaratanPkl.class).addOrder(Order.asc("nama"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		if (pkl.getId() != null) {
			HibernateUtil.currentSession().refresh(this.pkl);
		}

		if (pkl.getId() != null) {
			session = HibernateUtil.currentSession();
			selectedPersyaratanPkl = session.createCriteria(PklPunyaPersyaratan.class)
					.setProjection(Projections.groupProperty("persyaratanPkl"))
					.createAlias("persyaratanPkl", "persyaratanPkl")
					.add(Restrictions.or(Restrictions.isNull("persyaratanPkl.aktif"),
							Restrictions.eq("persyaratanPkl.aktif", true)))
					.add(Restrictions.eq("pkl", pkl)).list();

		} else {
			selectedPersyaratanPkl = new ArrayList<PersyaratanPkl>();
		}
		final List<PersyaratanPkl> persyaratanPklsKomponen = new ArrayList<PersyaratanPkl>();
		Label labelLama = new Label("");
		List<Component> components = new ArrayList<Component>();
		for (final PersyaratanPkl persyaratanPkl : persyaratanPkls) {

			Row myrow = PklUntukMahasiswaAction.tampilkanPersyaratan(persyaratanPkl, null, labelLama, subRows,
					components, false, persyaratanPklsKomponen);

			final Checkbox checkbox = new Checkbox();
			checkbox.setParent(myrow);
			checkbox.setChecked(selectedPersyaratanPkl.contains(persyaratanPkl));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedPersyaratanPkl.add(persyaratanPkl);
					} else {
						selectedPersyaratanPkl.remove(persyaratanPkl);
					}
				}
			});
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Item Biaya"));
		row.appendChild(kodeItemBiaya = new Textbox(pkl.getKodeItemBiaya()));
		kodeItemBiaya.setWidth("90%");
		kodeItemBiaya.setRows(2);

		Common.initKeterangan(rows,
				"Jika syarat mengikuti pkl harus membayar biaya tertentu, masukkan kode item biaya yang harus dibayar mahasiswa yang mengikuti pkl. Jika item biaya lebih dari satu, pisahkan dengan tanda koma (,), contoh : 502,505,506 dan seterusnya. Dan juga pastikan kode item biaya benar.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM yg tidak mengikuti syarat pembayaran"));
		row.appendChild(nimMhsTanpaBiaya = new Textbox(pkl.getNimMhsTanpaBiaya()));
		nimMhsTanpaBiaya.setWidth("90%");
		nimMhsTanpaBiaya.setRows(3);

		Common.initKeterangan(rows,
				"Jika NIM mahasiswa lebih dari satu, pisahkan dengan tanda koma (,), contoh : 12345678,12345679,123456710 dan seterusnya.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ketarangan"));
		row.appendChild(keterangan = new Textbox(pkl.getKeterangan() == null ? "" : pkl.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
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
					// loadKurikulum();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		if (nama_kelompok.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisAktfitasMahasiswa.getSelectedItem() == null
				|| jenisAktfitasMahasiswa.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis aktiftas",
					"Kolom Jenis aktiftas belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis aktiftas.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		PklDao pklDao = DaoFactory.getInstance().getPklDao();
		if (pkl.getId() != null) {
			pkl = pklDao.load(pkl.getId());
		}

		pkl.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		pkl.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		pkl.setProgram((String) (program.getSelectedItem() == null ? null : program.getSelectedItem().getValue()));
		pkl.setKeterangan(keterangan.getValue());
		pkl.setNama_kelompok(nama_kelompok.getValue());
		pkl.setTanggal_mulai(tanggal_mulai.getValue());
		pkl.setTanggal_selesai(tanggal_selesai.getValue());
		pkl.setMinimalSksBolehIkutPkl(minimalSksBolehIkutPkl.getValue());
		pkl.setMinimalIpkBolehIkutPkl(minimalIpkBolehIkutPkl.getValue());
		pkl.setMinimalIpkBolehIkutPkl2(minimalIpkBolehIkutPkl2.getValue());
		pkl.setMinimalSksBolehIkutPkl2(minimalSksBolehIkutPkl2.getValue());
		pkl.setAktifkanSyaratLain(aktifkanSyaratLain.isChecked());
		pkl.setHarusBayar(harusBayar.isChecked());
		pkl.setKodeItemBiaya(kodeItemBiaya.getValue().trim());
		pkl.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		pkl.setSemester((String) semester.getSelectedItem().getValue());
		pkl.setNimMhsTanpaBiaya(nimMhsTanpaBiaya.getValue().trim());
		pkl.setJenisAktfitasMahasiswa((JenisAktfitasMahasiswa) jenisAktfitasMahasiswa.getSelectedItem().getValue());

		pkl.setMahasiswaBolehMerubahAgenda(mahasiswaBolehMerubahAgenda.isChecked());
		pkl.setDosenBolehMerubahAgenda(dosenBolehMerubahAgenda.isChecked());

		// pklDao.beginTransaction();
		if (pkl.getId() != null) {
			pklDao.update(pkl);
		} else {
			pklDao.save(pkl);
		}
		// pklDao.commitTransaction();

		Session session = HibernateUtil.currentSession();
		session.createSQLQuery("delete from pkl_punya_komponen_penilaian_pkl where pkl=" + pkl.getId()).executeUpdate();
		for (KomponenPenilaianPkl komponenPenilaianPkl : selectedKomponenPenilaianPkl) {
			PklPunyaKomponenPenilaianPkl pklPunyaKomponenPenilaianPkl = new PklPunyaKomponenPenilaianPkl();
			pklPunyaKomponenPenilaianPkl.setKomponenPenilaianPkl(komponenPenilaianPkl);
			pklPunyaKomponenPenilaianPkl.setNama(komponenPenilaianPkl.getNama());
			pklPunyaKomponenPenilaianPkl.setPkl(pkl);
			session.save(pklPunyaKomponenPenilaianPkl);
		}

		session.createSQLQuery("delete from pkl_punya_persyaratan where pkl=" + pkl.getId()).executeUpdate();
		for (PersyaratanPkl persyaratanPkl : selectedPersyaratanPkl) {
			PklPunyaPersyaratan pklPunyaPersyaratanPkl = new PklPunyaPersyaratan();
			pklPunyaPersyaratanPkl.setPersyaratanPkl(persyaratanPkl);
			pklPunyaPersyaratanPkl.setNama(persyaratanPkl.getNama());
			pklPunyaPersyaratanPkl.setPkl(pkl);
			session.save(pklPunyaPersyaratanPkl);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");

		if (mahasiswa != null) {
			Criteria criteria = session.createCriteria(MahasiswaDapatPkl.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).setProjection(Projections.property("pkl"))
					.createCriteria("pkl")

					.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
							|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			if (order)
				criteria.addOrder(Order.desc("tanggal_mulai"));
			criteria.add(Restrictions.ilike("nama_kelompok", searchnama.getValue(), MatchMode.ANYWHERE))
					.add(Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE));
			return criteria;
		} else {
			Criteria criteria = session.createCriteria(Pkl.class)

					.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
							|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			if (order)
				criteria.addOrder(Order.desc("tanggal_mulai"));
			criteria.add(Restrictions.ilike("nama_kelompok", searchnama.getValue(), MatchMode.ANYWHERE))
					.add(Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE));
			return criteria;
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pkl> pkl = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pkl);
		grid.setRowRenderer(new PklRenderer());
		grid.setModelCheckMobile(strset);

	}

}
