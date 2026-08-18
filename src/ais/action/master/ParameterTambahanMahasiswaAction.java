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
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
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
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.KelompokParameterTambahanMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class ParameterTambahanMahasiswaAction extends GenericAutowireComposer
		implements DataSearchDefault, DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchjurusan;
	private Combobox searchfakultas;
	private Combobox searchprogram;
	private Combobox searchjenjang;
	private Combobox searchkelompokParameterTambahanMahasiswa;

	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;
	private Combobox jenjang;
	private Combobox kelompokParameterTambahanMahasiswa;
	private Combobox parameterTambahan;

	private MyToolbarbuttonConfig find;

	private boolean edit = true;
	private boolean delete = true;

	private ParameterTambahanMahasiswa parameterTambahanMahasiswa;
	private Tabpanel manajemenKelompok;
	private Tabpanel manajemenParameter;
	private Tabpanel konfigurasiBiodataMahasiswa;
	private Tabpanel manajemenKelompokAlumni;
	private Tabpanel manajemenAlumni;

	public void onResetParameter(Event event) {
		Common.insertCombo(searchkelompokParameterTambahanMahasiswa, "nama", KelompokParameterTambahanMahasiswa.class);
		if (!searchkelompokParameterTambahanMahasiswa.getChildren().isEmpty()) {
			searchkelompokParameterTambahanMahasiswa.setSelectedIndex(0);
		}
		onSearchDefault(null);
	}

	public void onKonfigurasiBiodataMahasiswa(Event event) {
		if (konfigurasiBiodataMahasiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(konfigurasiBiodataMahasiswa);
			MyInclude iframe = new MyInclude("/pages/master/konfigurasi_biodata_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenKelompok(Event event) {
		if (manajemenKelompok.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKelompok);
			MyInclude iframe = new MyInclude("/pages/master/kelompok_parameter_tambahan_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenParameter(Event event) {
		if (manajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenKelompokAlumni(Event event) {
		if (manajemenKelompokAlumni.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKelompokAlumni);
			MyInclude iframe = new MyInclude("/pages/master/kelompok_parameter_tambahan_alumni.zul");
			iframe.setParent(window);
		}
	}

	public void onAlumni(Event event) {
		Common.clear(manajemenAlumni);

		MyWindow window = new MyWindow("", "none", false);
		window.setHeight("100%");
		window.setWidth("100%");
		window.setParent(manajemenAlumni);
		MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan_alumni.zul");
		iframe.setParent(window);
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

		KelompokParameterTambahanMahasiswa.checkCreateDefault();

		if (searchkelompokParameterTambahanMahasiswa != null) {
			Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), searchfakultas,
					searchjurusan);

			program = Common.initPrograms(program);

			Common.initPrograms(searchprogram);

			Common.insertCombo(searchkelompokParameterTambahanMahasiswa, "nama",
					KelompokParameterTambahanMahasiswa.class);
			if (!searchkelompokParameterTambahanMahasiswa.getChildren().isEmpty()) {
				searchkelompokParameterTambahanMahasiswa.setSelectedIndex(0);
			}
			Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.insertComboDanSemua(jenjang = new Combobox(), "nama", Jenjang.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

			onSearchDefault(null);
			Common.initPaging(paging, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);

				}
			});

			String[] contents = new String[] { "id", "parameterTambahan", "tampilDiSemuaTahunAngkatan",
					"tahunAngkatans", "kelompokParameterTambahanMahasiswa", "fakultas", "jurusan", "program",
					"jenjang" };

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
			Common.appendKeToolbar(cetakToolbarbutton, find, comp);

			MyToolbarbuttonConfig upload = Common.uploadData(this, ParameterTambahanMahasiswa.class, contents);
			Common.appendKeToolbar(upload, find, comp);
		}
	        FilterLanjutHelper.setup(comp);
}

	class ParameterTambahanMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		private void tampil(final ParameterTambahanMahasiswa parameterTambahanMahasiswa, final Vbox vbox) {
			Common.clear(vbox);
			if (!parameterTambahanMahasiswa.getTampilDiSemuaTahunAngkatan()) {
				List<Integer> tahunAngkatanPendaftarans = HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.isNotNull("tahunangkatan"))
						.setProjection(Projections.groupProperty("tahunangkatan")).list();
				Collections.sort(tahunAngkatanPendaftarans);
				List<Integer> idsSelected = new ArrayList<Integer>();
				for (String s : parameterTambahanMahasiswa.getTahunAngkatans().split(";")) {
					if (!s.isEmpty() && Common.isNumber(s)) {
						idsSelected.add(Integer.parseInt(s.trim()));
					}
				}
				for (Integer tahunAngkatanPendaftaran : tahunAngkatanPendaftarans) {
					final MyCheckboxConfig checkbox = new MyCheckboxConfig("Angkatan " + tahunAngkatanPendaftaran);
					checkbox.setAttribute("tahunAngkatanPendaftaran", tahunAngkatanPendaftaran);
					checkbox.setChecked(idsSelected.contains(tahunAngkatanPendaftaran));
					checkbox.setParent(vbox);
					checkbox.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<MyCheckboxConfig> checkboxs = vbox.getChildren();
							String tahunAngkatans = "";
							for (MyCheckboxConfig checkbox : checkboxs) {
								if (checkbox.isChecked()) {
									Integer tahunAngkatanPendaftaran = (Integer) checkbox
											.getAttribute("tahunAngkatanPendaftaran");
									tahunAngkatans += ";" + tahunAngkatanPendaftaran + ";";
								}
							}
							parameterTambahanMahasiswa.setTahunAngkatans(tahunAngkatans);
							Common.refreshSaveOrUpdate(parameterTambahanMahasiswa);
						}
					});
				}

			}
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ParameterTambahanMahasiswa parameterTambahanMahasiswa = (ParameterTambahanMahasiswa) arg1;
			new Label(parameterTambahanMahasiswa.getKelompokParameterTambahanMahasiswa().getNama()).setParent(arg0);
			new Label(parameterTambahanMahasiswa.getFakultas() == null ? "Semua"
					: parameterTambahanMahasiswa.getFakultas().getNama()).setParent(arg0);
			new Label(parameterTambahanMahasiswa.getJurusan() == null ? "Semua"
					: parameterTambahanMahasiswa.getJurusan().getNama()).setParent(arg0);
			new Label(parameterTambahanMahasiswa.getProgram() == null
					|| parameterTambahanMahasiswa.getProgram().trim().isEmpty() ? "Semua"
							: parameterTambahanMahasiswa.getProgram()).setParent(arg0);
			new Label(parameterTambahanMahasiswa.getJenjang() == null ? "Semua"
					: parameterTambahanMahasiswa.getJenjang().getNama()).setParent(arg0);

			RevisiHelper.createNewRevisi(ParameterTambahanMahasiswa.class, parameterTambahanMahasiswa,
					parameterTambahanMahasiswa.getParameterTambahan().getLabelInputan()).setParent(arg0);
			new Label(parameterTambahanMahasiswa.getParameterTambahan().getHarusMenyertakanLampiran() ? "Ya" : "Tidak")
					.setParent(arg0);

			new Label(parameterTambahanMahasiswa.getParameterTambahan().getTipeDataInputan()).setParent(arg0);
			new Label(parameterTambahanMahasiswa.getParameterTambahan().getNilaiDataInputan()).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			hbox.setWidth("100%");
			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Semua");
			checkbox.setChecked(parameterTambahanMahasiswa.getTampilDiSemuaTahunAngkatan());
			checkbox.setParent(hbox);

			final Vbox vbox = new Vbox();
			vbox.setWidth("100%");
			vbox.setParent(hbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanMahasiswa.setTampilDiSemuaTahunAngkatan(checkbox.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanMahasiswa);
					tampil(parameterTambahanMahasiswa, vbox);
				}
			});
			tampil(parameterTambahanMahasiswa, vbox);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(parameterTambahanMahasiswa);
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

											Common.refreshDelete(parameterTambahanMahasiswa);

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

	@SuppressWarnings("unchecked")
	public void onAdd(Event event) throws Exception {

		if (searchkelompokParameterTambahanMahasiswa.getSelectedItem() == null
				|| searchkelompokParameterTambahanMahasiswa.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sebelum bisa menambah data, kelompok harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							searchkelompokParameterTambahanMahasiswa.focus();
						}
					});
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
			public void onEvent(Event arg0) throws Exception {

				List<ParameterTambahan> parameterTambahans = (List<ParameterTambahan>) arg0.getData();

				if (parameterTambahans != null) {
					Session session = HibernateUtil.currentSession();
					for (ParameterTambahan parameterTambahan : parameterTambahans) {

						ParameterTambahanMahasiswa parameterTambahanMahasiswa = new ParameterTambahanMahasiswa();
						parameterTambahanMahasiswa.setParameterTambahan(parameterTambahan);
						parameterTambahanMahasiswa.setProgram((String) (searchprogram.getSelectedItem() == null
								|| searchprogram.getSelectedItem().getValue() == null ? null
										: searchprogram.getSelectedItem().getValue()));

						parameterTambahanMahasiswa.setJenjang((Jenjang) (searchjenjang.getSelectedItem() == null
								|| searchjenjang.getSelectedItem().getValue() == null ? null
										: searchjenjang.getSelectedItem().getValue()));

						parameterTambahanMahasiswa.setKelompokParameterTambahanMahasiswa(
								(KelompokParameterTambahanMahasiswa) (searchkelompokParameterTambahanMahasiswa
										.getSelectedItem() == null ? null
												: searchkelompokParameterTambahanMahasiswa.getSelectedItem()
														.getValue()));

						parameterTambahanMahasiswa.setFakultas((Fakultas) (searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null ? null
										: searchfakultas.getSelectedItem().getValue()));

						parameterTambahanMahasiswa.setJurusan((Jurusan) (searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
								|| searchjurusan.getSelectedItem().getValue() == null ? null
										: searchjurusan.getSelectedItem().getValue()));

						session.save(parameterTambahanMahasiswa);

					}

					onSearchDefault(arg0);

				}

			}
		});

		window.onModal();

	}

	private void init(ParameterTambahanMahasiswa parameterTambahanMahasiswa) {
		final Fakultas selectedFakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		final Jurusan selectedJurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		this.parameterTambahanMahasiswa = parameterTambahanMahasiswa;
		addWindow.setTitle(parameterTambahanMahasiswa.getId() == null ? "Tambah Parameter" : "Ubah Parameter");
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok"));
		Common.insertCombo(kelompokParameterTambahanMahasiswa = new Combobox(), "nama", "keterangan",
				KelompokParameterTambahanMahasiswa.class);
		Common.selectComboItem(kelompokParameterTambahanMahasiswa,
				parameterTambahanMahasiswa.getKelompokParameterTambahanMahasiswa());
		row.appendChild(kelompokParameterTambahanMahasiswa);
		kelompokParameterTambahanMahasiswa.setWidth("90%");
		kelompokParameterTambahanMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				parameterTambahanMahasiswa.getFakultas() == null ? null : parameterTambahanMahasiswa.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (selectedFakultas != null) {
			Common.selectComboItem(fakultas, selectedFakultas);
			// fakultas.setDisabled(true);
		}

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				parameterTambahanMahasiswa.getJurusan() == null ? null : parameterTambahanMahasiswa.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		if (selectedJurusan != null) {
			Common.pilihJurusan(jurusan, selectedJurusan);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		Common.selectComboItem(jenjang,
				parameterTambahanMahasiswa.getJenjang() == null ? null : parameterTambahanMahasiswa.getJenjang());
		row.appendChild(jenjang);
		jenjang.setWidth("90%");

		Common.insertCombo(parameterTambahan = new Combobox(),
				new String[] { "labelInputan", "tipeDataInputan", "nilaiDataInputan" }, ParameterTambahan.class);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Parameter"));
		row.appendChild(parameterTambahan);
		parameterTambahan.setWidth("90%");
		Common.selectComboItem(parameterTambahan, parameterTambahanMahasiswa.getParameterTambahan());

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

		if (kelompokParameterTambahanMahasiswa.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelompok Parameter",
					"Kolom Kelompok Parameter belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kelompok Parameter.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (parameterTambahan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Parameter",
					"Kolom Nama Parameter belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Parameter.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (parameterTambahanMahasiswa.getId() != null) {
			parameterTambahanMahasiswa = (ParameterTambahanMahasiswa) session.load(ParameterTambahanMahasiswa.class,
					parameterTambahanMahasiswa.getId());

		}
		parameterTambahanMahasiswa.setProgram(
				(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));

		parameterTambahanMahasiswa.setJenjang(
				(Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue()));

		parameterTambahanMahasiswa.setKelompokParameterTambahanMahasiswa(
				(KelompokParameterTambahanMahasiswa) (kelompokParameterTambahanMahasiswa.getSelectedItem() == null
						? null
						: kelompokParameterTambahanMahasiswa.getSelectedItem().getValue()));

		parameterTambahanMahasiswa.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		parameterTambahanMahasiswa.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		parameterTambahanMahasiswa
				.setParameterTambahan((ParameterTambahan) parameterTambahan.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, parameterTambahanMahasiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ParameterTambahanMahasiswa.class).createAlias("parameterTambahan",
				"parameterTambahan");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("parameterTambahan.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))
				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						|| searchjenjang.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()))
				.add(searchkelompokParameterTambahanMahasiswa.getSelectedItem() == null
						|| searchkelompokParameterTambahanMahasiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kelompokParameterTambahanMahasiswa",
										searchkelompokParameterTambahanMahasiswa.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ParameterTambahanMahasiswa> parameterTambahanMahasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(parameterTambahanMahasiswa);
		grid.setRowRenderer(new ParameterTambahanMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/*
	 * public Boolean checkNamaAgama() {
	 * 
	 * Integer kotaCount = null; Session session = HibernateUtil.currentSession();
	 * kotaCount = ((Number) session .createCriteria(Agama.class)
	 * .setProjection(Projections.rowCount()) .add(Restrictions.eq("nama",
	 * nama.getValue().trim())) .add(this.parameterTambahanMahasiswa.getId() == null
	 * ? Restrictions .sqlRestriction("1=1") : Restrictions.ne("id",
	 * this.parameterTambahanMahasiswa.getId())).uniqueResult()) .intValue();
	 * 
	 * return !kotaCount.equals(0); }
	 */

}
