package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.Calendar;
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
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.DetailBiayaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailBiaya;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class DetailBiayaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 850318146245565078L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Combobox tahunAkademik;
	private Combobox jenisBiaya;
	private Combobox itemBiaya;
	private Combobox wnaAtauWni;
	private Combobox jenisSeleksi;
	private Combobox program;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox jenjang;
	private Combobox semester;
	private Combobox angkatan;
	private Combobox bahasa;
	private MyDoublebox nilaiBiaya;

	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchjeniskegiatan;
	private Combobox searchitembiaya;
	private Combobox searchtahunakademik;
	private Combobox searchtahunangkatan;

	private DetailBiaya detailBiaya;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	private JenisKegiatan selectedJenisKegiatan;

	// @Override
	// public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
	// org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
	// org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
	// Common.doCheckSecurity();
	// return super.doBeforeCompose(page, parent, compInfo);
	// }

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		// if (session.getAttribute("usersTemp") == null
		// || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
		// session.removeAttribute("usersTemp");
		// Common.goLogoff();
		// return;
		// }

		if (session.getAttribute("jenisKegiatan") != null) {
			selectedJenisKegiatan = (JenisKegiatan) session.getAttribute("jenisKegiatan");
			session.removeAttribute("jenisKegiatan");
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Common.insertCombo(jenisBiaya = new Combobox(), "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(itemBiaya = new Combobox(), "nama", ItemBiaya.class);

		Common.insertCombo(searchjeniskegiatan, "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (selectedJenisKegiatan != null) {
			Common.selectComboItem(searchjeniskegiatan, selectedJenisKegiatan);
			searchjeniskegiatan.setDisabled(true);
		}

		Common.insertCombo(searchitembiaya, "nama", ItemBiaya.class);

		wnaAtauWni = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(ais.database.model.Mahasiswa.WNA); }
		if (comboitem != null) { comboitem.setValue(ais.database.model.Mahasiswa.WNA); }
		wnaAtauWni.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(ais.database.model.Mahasiswa.WNI); }
		if (comboitem != null) { comboitem.setValue(ais.database.model.Mahasiswa.WNI); }
		wnaAtauWni.appendChild(comboitem);

		bahasa = new Combobox();
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Common.locale.getLanguage()); }
		if (comboitem != null) { comboitem.setValue(Common.locale.getLanguage()); }
		bahasa.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Common.localeEn.getLanguage()); }
		if (comboitem != null) { comboitem.setValue(Common.localeEn.getLanguage()); }
		bahasa.appendChild(comboitem);

		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		searchtahunakademik = Common.generateTahunAjaranDanSemua(searchtahunakademik);
		if (searchtahunakademik != null) { searchtahunakademik.setSelectedItem(null); }

		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 10; i <= ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
				+ 5; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			searchtahunangkatan.appendChild(comboitem);
		}

		Common.insertCombo(jenisSeleksi = new Combobox(), "nama", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		program = Common.initPrograms(program);
		
		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		class FakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(jurusan);
				jurusan.setSelectedItem(null);
				if (fakultas.getSelectedItem() == null||fakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
			}

		}

		fakultas.addEventListener("onChange", new FakultasEventListener());

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		Common.insertCombo(jenjang = new Combobox(), "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

	        FilterLanjutHelper.setup(comp);
}

	class detailBiayaRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			final DetailBiaya detailBiaya = (DetailBiaya) arg1;
			new Label(detailBiaya.getTahunAkademik()).setParent(arg0);
			new Label(detailBiaya.getJenisKegiatan() == null ? null : detailBiaya.getJenisKegiatan().getNamaKegiatan())
					.setParent(arg0);
			new Label(detailBiaya.getItemBiaya() == null ? null : detailBiaya.getItemBiaya().getNama()).setParent(arg0);
			new Label(detailBiaya.getWnaAtauWni() == null ? null : detailBiaya.getWnaAtauWni()).setParent(arg0);
			new Label(detailBiaya.getFakultas() == null ? null : detailBiaya.getFakultas().getNama()).setParent(arg0);
			new Label(detailBiaya.getJurusan() == null ? null : detailBiaya.getJurusan().getNama()).setParent(arg0);
			new Label(detailBiaya.getAngkatan() == null ? null : detailBiaya.getAngkatan().toString()).setParent(arg0);

			new Label(detailBiaya.getBahasa() == null ? null : detailBiaya.getBahasa()).setParent(arg0);

			new Label((detailBiaya.getNilaiBiaya() == null ? new Double(0.0) : detailBiaya.getNilaiBiaya()) == null
					? null
					: Common.numberFormat.get().format(
							detailBiaya.getNilaiBiaya() == null ? new Double(0.0) : detailBiaya.getNilaiBiaya()))
									.setParent(arg0);
			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);

			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(detailBiaya);
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

									Common.refreshDelete(detailBiaya);

									onSearchDefault(event);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e); 
									MyMessageboxConfig
											.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
													+ e.getMessage());
								}

							}

						}
					});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new DetailBiaya());
		addWindow.setVisible(true);
		addWindow.onModal();

	}

	private void init(DetailBiaya detailBiaya) {
		this.detailBiaya = detailBiaya;
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		Common.selectComboItem(tahunAkademik,
				detailBiaya.getTahunAkademik() == null || detailBiaya.getTahunAkademik().trim().equals("") ? null
						: detailBiaya.getTahunAkademik());
		row.appendChild(tahunAkademik);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Biaya"));
		Common.selectComboItem(jenisBiaya, detailBiaya.getJenisKegiatan());
		row.appendChild(jenisBiaya);
		jenisBiaya.setWidth("90%");

		if (selectedJenisKegiatan != null) {
			Common.selectComboItem(jenisBiaya, selectedJenisKegiatan);
			jenisBiaya.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item Biaya"));
		Common.selectComboItem(itemBiaya, detailBiaya.getItemBiaya());
		row.appendChild(itemBiaya);
		itemBiaya.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kewarganegaraan"));
		Common.selectComboItem(wnaAtauWni,
				detailBiaya.getWnaAtauWni() == null || detailBiaya.getWnaAtauWni().trim().equals("") ? null
						: detailBiaya.getWnaAtauWni());
		row.appendChild(wnaAtauWni);
		wnaAtauWni.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Seleksi"));
		Common.selectComboItem(jenisSeleksi, detailBiaya.getJenisSeleksi());
		row.appendChild(jenisSeleksi);
		jenisSeleksi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program, detailBiaya.getProgram() == null || detailBiaya.getProgram().trim().equals("")
				? null : detailBiaya.getProgram());
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas, detailBiaya.getFakultas() == null ? null : detailBiaya.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, detailBiaya.getJurusan() == null ? null : detailBiaya.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		Common.selectComboItem(jenjang, detailBiaya.getJenjang());
		row.appendChild(jenjang);
		jenjang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semester = new Combobox());
		for (int i = 0; i < 21; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			if (i == 0) {
				comboitem.setLabel("Semua-Semester");
			} else {
				comboitem.setLabel(i + "");
			}
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}
		Common.selectComboItem(semester, detailBiaya.getSemester() == null ? 0 : detailBiaya.getSemester());
		semester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		angkatan = new Combobox();

		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 10; i <= ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
				+ 5; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			angkatan.appendChild(comboitem);
		}
		Common.selectComboItem(angkatan, detailBiaya.getAngkatan() == null ? null : detailBiaya.getAngkatan());
		row.appendChild(angkatan);
		angkatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Biaya"));
		row.appendChild(
				nilaiBiaya = new MyDoublebox(detailBiaya.getNilaiBiaya() == null ? 0.0 : detailBiaya.getNilaiBiaya()));
		nilaiBiaya.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bahasa"));
		Common.selectComboItem(bahasa, detailBiaya.getBahasa());
		row.appendChild(bahasa);
		bahasa.setWidth("90%");

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

		// if (tahunAkademik.getSelectedItem() == null) {
		// MyMessageboxConfig.show("Tahun harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		if (jenisBiaya.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Biaya",
					"Kolom Jenis Biaya belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Biaya.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (itemBiaya.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Item Pembayaran",
					"Kolom Item Pembayaran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Item Pembayaran.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		// if (wnaAtauWni.getSelectedItem() == null) {
		// MyMessageboxConfig.show("Kewarganegaraan harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		// if (jenisSeleksi.getSelectedItem() == null) {
		// MyMessageboxConfig.show("Jenis Seleksi harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		// if (program.getSelectedItem() == null||program.getSelectedItem().getValue() == null) {
		// MyMessageboxConfig.show("Program harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		// if (fakultas.getSelectedItem() == null||fakultas.getSelectedItem().getValue() == null) {
		// MyMessageboxConfig.show("Fakultas"
		// + " Biaya harus diisi", "Peringatan", MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		// if (jurusan.getSelectedItem() == null||jurusan.getSelectedItem().getValue() == null) {
		// MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus
		// diisi",
		// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		// if (jenjang.getSelectedItem() == null) {
		// MyMessageboxConfig.show("Jenjang harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		// if (semester.getSelectedItem() == null||semester.getSelectedItem().getValue()==null) {
		// MyMessageboxConfig.show("Semester harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		// if (angkatan.getSelectedItem() == null) {
		// MyMessageboxConfig.show("Angkatan harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		if (nilaiBiaya.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nilai Biaya",
					"Kolom Nilai Biaya belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nilai Biaya.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		DetailBiayaDao detailBiayaDao = DaoFactory.getInstance().getDetailBiayaDao();
		if (detailBiaya.getId() != null) {
			detailBiaya = detailBiayaDao.load(detailBiaya.getId());
		}
		detailBiaya.setBahasa((String) (bahasa.getSelectedItem() == null ? null : bahasa.getSelectedItem().getValue()));

		detailBiaya.setTahunAkademik(
				(String) (tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null ? null : tahunAkademik.getSelectedItem().getValue()));
		detailBiaya.setJenisKegiatan((JenisKegiatan) (jenisBiaya.getSelectedItem() == null ? null
				: jenisBiaya.getSelectedItem().getValue()));
		detailBiaya.setItemBiaya(
				(ItemBiaya) (itemBiaya.getSelectedItem() == null ? null : itemBiaya.getSelectedItem().getValue()));
		detailBiaya.setWnaAtauWni(
				(String) (wnaAtauWni.getSelectedItem() == null ? null : wnaAtauWni.getSelectedItem().getValue()));
		detailBiaya.setJenisSeleksi((JenisSeleksi) (jenisSeleksi.getSelectedItem() == null ? null
				: jenisSeleksi.getSelectedItem().getValue()));
		detailBiaya
				.setProgram((String) (program.getSelectedItem() == null||program.getSelectedItem().getValue() == null ? null : program.getSelectedItem().getValue()));
		detailBiaya.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue()==null ? null : fakultas.getSelectedItem().getValue()));
		detailBiaya.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue()==null ? null : jurusan.getSelectedItem().getValue()));
		detailBiaya.setJenjang(
				(Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue()));
		detailBiaya.setSemester(
				(Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue()));
		detailBiaya.setAngkatan(
				(Integer) (angkatan.getSelectedItem() == null ? null : angkatan.getSelectedItem().getValue()));
		detailBiaya.setNilaiBiaya(nilaiBiaya.getValue() == null ? null : nilaiBiaya.getValue().doubleValue());
		detailBiaya.setNama(Common.getCurrentUser().getUserId() + "/" + Common.getCurrentUser().getUserNama());

		if (detailBiaya.getId() != null) {
			detailBiayaDao.update(detailBiaya);
		} else {
			detailBiayaDao.save(detailBiaya);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		JenisKegiatan jenisKegiatan = (JenisKegiatan) (searchjeniskegiatan.getSelectedItem() == null ? null
				: searchjeniskegiatan.getSelectedItem().getValue());
		ItemBiaya itemBiaya = (ItemBiaya) (searchitembiaya.getSelectedItem() == null ? null
				: searchitembiaya.getSelectedItem().getValue());

		String tahunAkademik = (String) (searchtahunakademik.getSelectedItem() == null || searchtahunakademik.getSelectedItem().getValue() == null ? null
				: searchtahunakademik.getSelectedItem().getValue());

		Integer angkatan = (Integer) (searchtahunangkatan.getSelectedItem() == null ? null
				: searchtahunangkatan.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DetailBiaya.class);
		// .add(Restrictions.or(
		// Restrictions.eq("merupakanPembayaran", false),
		// Restrictions.isNull("merupakanPembayaran")))
		if (order)
			criteria.addOrder(Order.desc("id"));
		if (order)
			criteria.addOrder(Order.asc("tahunAkademik"));

		criteria.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("tahunAkademik", tahunAkademik))

		.add(angkatan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("angkatan", angkatan))

		.add(itemBiaya == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("itemBiaya", itemBiaya))

		.add(jenisKegiatan == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("jenisKegiatan", jenisKegiatan))

		.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
				: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DetailBiaya> detailBiaya = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(detailBiaya);
		grid.setRowRenderer(new detailBiayaRenderer());
		grid.setModelCheckMobile(strset);

	}
}
