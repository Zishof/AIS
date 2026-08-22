package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.Collections;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataParameterTambahanBanyak;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GrupChecklistPenilaianDosen;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanAngketUmum;
import ais.database.model.sekolah.GrupChecklistPenilaianGuru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class ParameterTambahanAngketUmumAction extends GenericAutowireComposer
		implements DataSearchDefault, DataCriteria {

	private static final long serialVersionUID = -5779730267402400328L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchjurusan;
	private Combobox searchfakultas;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchprogram;
	private Combobox searchjenjang;
	private Combobox searchgrupChecklistPenilaianUmum;
	private Combobox searchgrupChecklistPenilaianDosen;
	private Combobox searchgrupChecklistPenilaianGuru;

	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox program;
	private Combobox jenjang;
	private Combobox grupChecklistPenilaianUmum;
	private Combobox grupChecklistPenilaianDosen;
	private Combobox grupChecklistPenilaianGuru;
	private Combobox parameterTambahan;

	private MyToolbarbuttonConfig find;

	private boolean edit = true;
	private boolean delete = true;
	private boolean pt;
	private boolean ya;

	private Row hbFakultasLabel;
	private Row hbYayasan;
	private Tabpanel manajemenParameter;

	private ParameterTambahanAngketUmum parameterTambahanAngketUmum;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		initFilterCombo();
		initToolbar();
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	private void initFilterCombo() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);
		Common.initPrograms(searchprogram);

		if (hbFakultasLabel != null) {
			hbFakultasLabel.setVisible(pt && searchfakultas != null && searchfakultas.getChildren().size() > 1);
		}
		if (hbYayasan != null) {
			hbYayasan.setVisible(ya);
		}

		populateSearchGroupCombos(false);

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), searchfakultas,
				searchjurusan);
		program = Common.initPrograms(program);
		Common.insertComboDanSemua(jenjang = new Combobox(), "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
	}

	private void populateSearchGroupCombos(boolean selectFirst) {
		if (searchgrupChecklistPenilaianUmum != null) {
			Common.insertComboDanSemua(searchgrupChecklistPenilaianUmum, "isi", "keterangan",
					GrupChecklistPenilaianUmum.class,
					Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
			selectFirstIfNeeded(searchgrupChecklistPenilaianUmum, selectFirst);
		}
		if (searchgrupChecklistPenilaianDosen != null) {
			Common.insertComboDanSemua(searchgrupChecklistPenilaianDosen, "isi", "keterangan",
					GrupChecklistPenilaianDosen.class,
					Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
			selectFirstIfNeeded(searchgrupChecklistPenilaianDosen, false);
		}
		if (searchgrupChecklistPenilaianGuru != null) {
			Common.insertComboDanSemua(searchgrupChecklistPenilaianGuru, "isi", "keterangan",
					GrupChecklistPenilaianGuru.class,
					Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
			selectFirstIfNeeded(searchgrupChecklistPenilaianGuru, false);
		}
	}

	private void selectFirstIfNeeded(Combobox combo, boolean selectFirst) {
		try {
			if (combo != null && selectFirst && !combo.getChildren().isEmpty()) {
				combo.setSelectedIndex(0);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initToolbar() {
		String[] contents = new String[] { "id", "parameterTambahan", "tampilDiSemuaTahunAngkatan",
				"tahunAngkatans", "grupChecklistPenilaianUmum", "grupChecklistPenilaianDosen",
				"grupChecklistPenilaianGuru", "fakultas", "jurusan", "yayasan", "sekolah", "program", "jenjang" };

		if (find != null && find.getParent() != null) {
			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
			find.getParent().appendChild(cetakToolbarbutton);

			MyToolbarbuttonConfig upload = Common.uploadData(this, ParameterTambahanAngketUmum.class, contents);
			find.getParent().appendChild(upload);
		}
	}

	public void onResetParameter(Event event) {
		populateSearchGroupCombos(false);
		onSearchDefault(null);
	}

	public void onManajemenParameter(Event event) {
		if (manajemenParameter == null) {
			return;
		}
		if (manajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan.zul");
			iframe.setParent(window);
		}
	}

	class ParameterTambahanAngketUmumRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		private void tampil(final ParameterTambahanAngketUmum data, final Vbox vbox) {
			Common.clear(vbox);
			if (data == null || data.getTampilDiSemuaTahunAngkatan()) {
				return;
			}

			List<Integer> tahunAngkatanPendaftarans = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNotNull("tahunangkatan"))
					.setProjection(Projections.groupProperty("tahunangkatan")).list();
			Collections.sort(tahunAngkatanPendaftarans);

			List<Integer> idsSelected = new ArrayList<Integer>();
			String tahunAngkatans = data.getTahunAngkatans();
			for (String s : tahunAngkatans.split(";")) {
				if (!s.isEmpty() && Common.isNumber(s)) {
					idsSelected.add(Integer.valueOf(Integer.parseInt(s.trim())));
				}
			}

			for (Integer tahunAngkatanPendaftaran : tahunAngkatanPendaftarans) {
				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Angkatan " + tahunAngkatanPendaftaran);
				checkbox.setAttribute("tahunAngkatanPendaftaran", tahunAngkatanPendaftaran);
				checkbox.setChecked(idsSelected.contains(tahunAngkatanPendaftaran));
				checkbox.setParent(vbox);
				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						String tahunAngkatans = "";
						List children = vbox.getChildren();
						for (Object child : children) {
							if (child instanceof MyCheckboxConfig) {
								MyCheckboxConfig cb = (MyCheckboxConfig) child;
								if (cb.isChecked()) {
									Integer tahun = (Integer) cb.getAttribute("tahunAngkatanPendaftaran");
									tahunAngkatans += ";" + tahun + ";";
								}
							}
						}
						data.setTahunAngkatans(tahunAngkatans);
						Common.refreshSaveOrUpdate(data);
					}
				});
			}
		}

		@Override
		public void render(final Row row, Object item) throws Exception {
			row.setValign("top");
			final ParameterTambahanAngketUmum data = (ParameterTambahanAngketUmum) item;
			final ParameterTambahan parameter = data.getParameterTambahan();

			new Label(targetLabel(data)).setParent(row);

			Vbox unitBox = new Vbox();
			unitBox.setParent(row);
			new Label(data.getFakultas() == null ? "Semua Fakultas" : data.getFakultas().getNama()).setParent(unitBox);
			new Label(data.getYayasan() == null ? "Semua Yayasan" : data.getYayasan().getNama()).setParent(unitBox);

			Vbox subUnitBox = new Vbox();
			subUnitBox.setParent(row);
			new Label(data.getJurusan() == null ? "Semua Prodi" : data.getJurusan().getNama()).setParent(subUnitBox);
			new Label(data.getSekolah() == null ? "Semua Sekolah" : data.getSekolah().getNama()).setParent(subUnitBox);

			new Label(isBlank(data.getProgram()) ? "Semua" : data.getProgram()).setParent(row);
			new Label(data.getJenjang() == null ? "Semua" : data.getJenjang().getNama()).setParent(row);

			RevisiHelper.createNewRevisi(ParameterTambahanAngketUmum.class, data,
					parameter == null ? "" : parameter.getLabelInputan()).setParent(row);
			new Label(parameter != null && parameter.getHarusMenyertakanLampiran() ? "Ya" : "Tidak").setParent(row);
			new Label(parameter == null ? "" : parameter.getTipeDataInputan()).setParent(row);
			new Label(parameter == null ? "" : parameter.getNilaiDataInputan()).setParent(row);

			Hbox hbox = new Hbox();
			hbox.setParent(row);
			hbox.setWidth("100%");
			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Semua");
			checkbox.setChecked(data.getTampilDiSemuaTahunAngkatan());
			checkbox.setParent(hbox);

			final Vbox vbox = new Vbox();
			vbox.setWidth("100%");
			vbox.setParent(hbox);
			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					data.setTampilDiSemuaTahunAngkatan(checkbox.isChecked());
					Common.refreshSaveOrUpdate(data);
					tampil(data, vbox);
				}
			});
			tampil(data, vbox);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(data);
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
											Common.refreshDelete(data);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus karena berelasi dengan data lainnya, error-nya adalah: "
															+ e.getMessage());
										}
									}
								}
							});
				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(row);
		}
	}

	@SuppressWarnings("unchecked")
	public void onAdd(Event event) throws Exception {
		Object target = selectedSearchTarget();
		if (target == null) {
			init(new ParameterTambahanAngketUmum());
			addWindow.setVisible(true);
			addWindow.onModal();
			return;
		}

		List<ParameterTambahan> parameterTambahans = initCriteria(false)
				.setProjection(Projections.groupProperty("parameterTambahan")).list();

		AmbilDataParameterTambahanBanyak window = new AmbilDataParameterTambahanBanyak(parameterTambahans);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setWidth("90%");
		window.setHeight("90%");
		window.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				List<ParameterTambahan> parameterTambahans = (List<ParameterTambahan>) event.getData();
				if (parameterTambahans == null || parameterTambahans.isEmpty()) {
					return;
				}

				Session session = HibernateUtil.currentSession();
				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					ParameterTambahanAngketUmum data = new ParameterTambahanAngketUmum();
					data.setParameterTambahan(parameterTambahan);
					applyFilterValueToData(data);
					session.save(data);
				}
				onSearchDefault(event);
			}
		});
		window.onModal();
	}

	private void init(ParameterTambahanAngketUmum data) {
		this.parameterTambahanAngketUmum = data;
		addWindow.setTitle(data.getId() == null ? "Tambah Parameter Angket" : "Ubah Parameter Angket");
		Common.clear(addWindow);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Angket Umum"));
		Common.insertComboDanSemua(grupChecklistPenilaianUmum = new Combobox(), "isi", "keterangan",
				GrupChecklistPenilaianUmum.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(true, grupChecklistPenilaianUmum, data.getGrupChecklistPenilaianUmum());
		row.appendChild(grupChecklistPenilaianUmum);
		grupChecklistPenilaianUmum.setWidth("90%");
		grupChecklistPenilaianUmum.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Angket Dosen"));
		Common.insertComboDanSemua(grupChecklistPenilaianDosen = new Combobox(), "isi", "keterangan",
				GrupChecklistPenilaianDosen.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(true, grupChecklistPenilaianDosen, data.getGrupChecklistPenilaianDosen());
		row.appendChild(grupChecklistPenilaianDosen);
		grupChecklistPenilaianDosen.setWidth("90%");
		grupChecklistPenilaianDosen.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Angket Guru"));
		Common.insertComboDanSemua(grupChecklistPenilaianGuru = new Combobox(), "isi", "keterangan",
				GrupChecklistPenilaianGuru.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(true, grupChecklistPenilaianGuru, data.getGrupChecklistPenilaianGuru());
		row.appendChild(grupChecklistPenilaianGuru);
		grupChecklistPenilaianGuru.setWidth("90%");
		grupChecklistPenilaianGuru.setReadonly(true);

		addTargetComboListener(grupChecklistPenilaianUmum, grupChecklistPenilaianDosen, grupChecklistPenilaianGuru);
		addTargetComboListener(grupChecklistPenilaianDosen, grupChecklistPenilaianUmum, grupChecklistPenilaianGuru);
		addTargetComboListener(grupChecklistPenilaianGuru, grupChecklistPenilaianUmum, grupChecklistPenilaianDosen);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas, data.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, data.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null, true, false);
		Common.selectComboItem(yayasan, data.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		Common.pilihSekolah(sekolah, data.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program, data.getProgram());
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		Common.selectComboItem(jenjang, data.getJenjang());
		row.appendChild(jenjang);
		jenjang.setWidth("90%");

		Common.insertCombo(parameterTambahan = new Combobox(),
				new String[] { "labelInputan", "tipeDataInputan", "nilaiDataInputan" }, ParameterTambahan.class);
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Parameter"));
		row.appendChild(parameterTambahan);
		parameterTambahan.setWidth("90%");
		Common.selectComboItem(parameterTambahan, data.getParameterTambahan());

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

	private void addTargetComboListener(final Combobox selected, final Combobox other1, final Combobox other2) {
		if (selected == null) {
			return;
		}
		selected.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (selected.getSelectedItem() != null && selected.getSelectedItem().getValue() != null) {
					clearComboSelection(other1);
					clearComboSelection(other2);
				}
			}
		});
	}

	private void clearComboSelection(Combobox combo) {
		try {
			if (combo != null) {
				combo.setSelectedItem(null);
				combo.setValue("");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (selectedFormTargetCount() != 1) {
			MyMessageboxConfig.show("Pilih salah satu target: Kelompok Angket Umum, Dosen, atau Guru", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (parameterTambahan == null || parameterTambahan.getSelectedItem() == null
				|| parameterTambahan.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Parameter",
					"Kolom Nama Parameter belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Parameter.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (parameterTambahanAngketUmum.getId() != null) {
			parameterTambahanAngketUmum = (ParameterTambahanAngketUmum) session.load(ParameterTambahanAngketUmum.class,
					parameterTambahanAngketUmum.getId());
		}

		parameterTambahanAngketUmum.setProgram(selectedStringValue(program));
		parameterTambahanAngketUmum.setJenjang((Jenjang) selectedValue(jenjang));
		parameterTambahanAngketUmum.setFakultas((Fakultas) selectedValue(fakultas));
		parameterTambahanAngketUmum.setJurusan((Jurusan) selectedValue(jurusan));
		parameterTambahanAngketUmum.setYayasan((Yayasan) selectedValue(yayasan));
		parameterTambahanAngketUmum.setSekolah((Sekolah) selectedValue(sekolah));
		parameterTambahanAngketUmum.setGrupChecklistPenilaianUmum((GrupChecklistPenilaianUmum) selectedValue(grupChecklistPenilaianUmum));
		parameterTambahanAngketUmum.setGrupChecklistPenilaianDosen((GrupChecklistPenilaianDosen) selectedValue(grupChecklistPenilaianDosen));
		parameterTambahanAngketUmum.setGrupChecklistPenilaianGuru((GrupChecklistPenilaianGuru) selectedValue(grupChecklistPenilaianGuru));
		parameterTambahanAngketUmum.setParameterTambahan((ParameterTambahan) parameterTambahan.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, parameterTambahanAngketUmum);
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ParameterTambahanAngketUmum.class)
				.createAlias("parameterTambahan", "parameterTambahan");

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		criteria.add(isBlank(searchValue(searchnama)) ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("parameterTambahan.nama", searchValue(searchnama), MatchMode.ANYWHERE));
		addEqIfSelected(criteria, "jurusan", searchjurusan);
		addEqIfSelected(criteria, "fakultas", searchfakultas);
		addEqIfSelected(criteria, "sekolah", searchsekolah);
		addEqIfSelected(criteria, "yayasan", searchyayasan);
		addEqIfSelected(criteria, "program", searchprogram);
		addEqIfSelected(criteria, "jenjang", searchjenjang);
		addEqIfSelected(criteria, "grupChecklistPenilaianUmum", searchgrupChecklistPenilaianUmum);
		addEqIfSelected(criteria, "grupChecklistPenilaianDosen", searchgrupChecklistPenilaianDosen);
		addEqIfSelected(criteria, "grupChecklistPenilaianGuru", searchgrupChecklistPenilaianGuru);
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<ParameterTambahanAngketUmum> datas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(datas);
		grid.setRowRenderer(new ParameterTambahanAngketUmumRenderer());
		grid.setModelCheckMobile(strset);
	}

	private void addEqIfSelected(Criteria criteria, String property, Combobox combo) {
		Object value = selectedValue(combo);
		// Item "Semua" pada combo kadang berupa ENTITY PLACEHOLDER yang TRANSIENT (id null,
		// mis. Sekolah/Yayasan "Semua"). Bila dipakai di Restrictions.eq, Hibernate akan
		// melempar TransientObjectException ("unsaved transient instance ... Sekolah") saat
		// mengikat parameter. Perlakukan entity tanpa id sebagai "tanpa filter".
		if (value instanceof ais.database.model.GeneralValueObject
				&& ((ais.database.model.GeneralValueObject) value).getId() == null) {
			value = null;
		}
		criteria.add(value == null ? Restrictions.sqlRestriction("true") : Restrictions.eq(property, value));
	}

	private Object selectedValue(Combobox combo) {
		return combo == null || combo.getSelectedItem() == null ? null : combo.getSelectedItem().getValue();
	}

	private String selectedStringValue(Combobox combo) {
		Object value = selectedValue(combo);
		return value == null ? null : value.toString();
	}

	private String searchValue(Textbox textbox) {
		return textbox == null || textbox.getValue() == null ? "" : textbox.getValue().trim();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private Object selectedSearchTarget() {
		Object umum = selectedValue(searchgrupChecklistPenilaianUmum);
		Object dosen = selectedValue(searchgrupChecklistPenilaianDosen);
		Object guru = selectedValue(searchgrupChecklistPenilaianGuru);
		int count = 0;
		Object target = null;
		if (umum != null) {
			count++;
			target = umum;
		}
		if (dosen != null) {
			count++;
			target = dosen;
		}
		if (guru != null) {
			count++;
			target = guru;
		}
		return count == 1 ? target : null;
	}

	private void applyFilterValueToData(ParameterTambahanAngketUmum data) {
		data.setProgram(selectedStringValue(searchprogram));
		data.setJenjang((Jenjang) selectedValue(searchjenjang));
		data.setGrupChecklistPenilaianUmum((GrupChecklistPenilaianUmum) selectedValue(searchgrupChecklistPenilaianUmum));
		data.setGrupChecklistPenilaianDosen((GrupChecklistPenilaianDosen) selectedValue(searchgrupChecklistPenilaianDosen));
		data.setGrupChecklistPenilaianGuru((GrupChecklistPenilaianGuru) selectedValue(searchgrupChecklistPenilaianGuru));
		data.setFakultas((Fakultas) selectedValue(searchfakultas));
		data.setJurusan((Jurusan) selectedValue(searchjurusan));
		data.setSekolah((Sekolah) selectedValue(searchsekolah));
		data.setYayasan((Yayasan) selectedValue(searchyayasan));
	}

	private int selectedFormTargetCount() {
		int count = 0;
		if (selectedValue(grupChecklistPenilaianUmum) != null) {
			count++;
		}
		if (selectedValue(grupChecklistPenilaianDosen) != null) {
			count++;
		}
		if (selectedValue(grupChecklistPenilaianGuru) != null) {
			count++;
		}
		return count;
	}

	private String targetLabel(ParameterTambahanAngketUmum data) {
		if (data == null) {
			return "";
		}
		try {
			if (data.getGrupChecklistPenilaianUmum() != null) {
				return "Umum - " + data.getGrupChecklistPenilaianUmum().getIsi();
			}
			if (data.getGrupChecklistPenilaianDosen() != null) {
				return "Dosen - " + data.getGrupChecklistPenilaianDosen().getIsi();
			}
			if (data.getGrupChecklistPenilaianGuru() != null) {
				return "Guru - " + data.getGrupChecklistPenilaianGuru().getIsi();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return "Belum dipilih";
	}
}
