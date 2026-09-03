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
import ais.action.master.helper.generic.AmbilDataParameterTambahanBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.KelompokParameterTambahanAlumni;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanAlumni;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk parameter tambahan alumni. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchjurusan}, {@code Combobox
 * searchfakultas}, {@code Combobox searchprogram}, {@code Combobox searchjenjang}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onResetParameter()}, {@code onSave()});
 * operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class ParameterTambahanAlumniAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

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
	private Combobox searchkelompokParameterTambahanAlumni;

	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;
	private Combobox jenjang;
	private Combobox kelompokParameterTambahanAlumni;
	private Combobox parameterTambahan;

	private MyToolbarbuttonConfig find;

	private boolean edit = false;
	private boolean delete = false;

	private ParameterTambahanAlumni parameterTambahanAlumni;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void onResetParameter(Event event) {
		Common.insertCombo(searchkelompokParameterTambahanAlumni, "nama", KelompokParameterTambahanAlumni.class);
		if (!searchkelompokParameterTambahanAlumni.getChildren().isEmpty()) {
			searchkelompokParameterTambahanAlumni.setSelectedIndex(0);
		}
		onSearchDefault(null);
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

		KelompokParameterTambahanAlumni.checkCreateDefault();

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), searchfakultas,
				searchjurusan);

		program = Common.initPrograms(program);
		Common.initPrograms(searchprogram);

		Common.insertCombo(searchkelompokParameterTambahanAlumni, "nama", KelompokParameterTambahanAlumni.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (!searchkelompokParameterTambahanAlumni.getChildren().isEmpty()) {
			searchkelompokParameterTambahanAlumni.setSelectedIndex(0);
		}
		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(jenjang = new Combobox(), "nama", Jenjang.class,
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

		String[] contents = new String[] { "id", "parameterTambahan", "tampilDiSemuaTahunAngkatan", "tahunAngkatans",
				"kelompokParameterTambahanAlumni", "fakultas", "jurusan", "program", "jenjang" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ParameterTambahanAlumni.class, contents);
		Common.appendKeToolbar(upload, find, comp);
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link ParameterTambahanAlumniAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ParameterTambahanAlumniAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code tampil()}, {@code render}(). Aturan
	 * bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see ParameterTambahanAlumniAction
	 */
	class ParameterTambahanAlumniRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		private void tampil(final ParameterTambahanAlumni parameterTambahanAlumni, final Vbox vbox) {
			Common.clear(vbox);
			if (!parameterTambahanAlumni.getTampilDiSemuaTahunAngkatan()) {
				List<Integer> tahunAngkatanPendaftarans = HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.isNotNull("tahunangkatan"))
						.setProjection(Projections.groupProperty("tahunangkatan")).list();
				Collections.sort(tahunAngkatanPendaftarans);
				List<Integer> idsSelected = new ArrayList<Integer>();
				for (String s : parameterTambahanAlumni.getTahunAngkatans().split(";")) {
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
							parameterTambahanAlumni.setTahunAngkatans(tahunAngkatans);
							Common.refreshSaveOrUpdate(parameterTambahanAlumni);
						}
					});
				}

			}
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ParameterTambahanAlumni parameterTambahanAlumni = (ParameterTambahanAlumni) arg1;
			new Label(parameterTambahanAlumni.getKelompokParameterTambahanAlumni().getNama()).setParent(arg0);
			new Label(parameterTambahanAlumni.getJurusan() == null ? "Semua"
					: parameterTambahanAlumni.getJurusan().getNama()).setParent(arg0);
			new Label(parameterTambahanAlumni.getProgram() == null
					|| parameterTambahanAlumni.getProgram().trim().isEmpty() ? "Semua"
							: parameterTambahanAlumni.getProgram()).setParent(arg0);
			new Label(parameterTambahanAlumni.getJenjang() == null ? "Semua"
					: parameterTambahanAlumni.getJenjang().getNama()).setParent(arg0);

			RevisiHelper.createNewRevisi(ParameterTambahanAlumni.class, parameterTambahanAlumni,
					parameterTambahanAlumni.getParameterTambahan().getLabelInputan()).setParent(arg0);
			new Label(parameterTambahanAlumni.getParameterTambahan().getHarusMenyertakanLampiran() ? "Ya" : "Tidak")
					.setParent(arg0);

			new Label(parameterTambahanAlumni.getParameterTambahan().getTipeDataInputan()).setParent(arg0);
			new Label(parameterTambahanAlumni.getParameterTambahan().getNilaiDataInputan()).setParent(arg0);

			final MyCheckboxConfig wajib = new MyCheckboxConfig("Isian Wajib");
			wajib.setChecked(parameterTambahanAlumni.getWajibDiisi());
			wajib.setParent(arg0);
			wajib.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanAlumni.setWajibDiisi(wajib.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanAlumni);
				}
			});

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			hbox.setWidth("100%");
			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Semua");
			checkbox.setChecked(parameterTambahanAlumni.getTampilDiSemuaTahunAngkatan());
			checkbox.setParent(hbox);

			final Vbox vbox = new Vbox();
			vbox.setWidth("100%");
			vbox.setParent(hbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanAlumni.setTampilDiSemuaTahunAngkatan(checkbox.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanAlumni);
					tampil(parameterTambahanAlumni, vbox);
				}
			});
			tampil(parameterTambahanAlumni, vbox);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(parameterTambahanAlumni);
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

											Common.refreshDelete(parameterTambahanAlumni);

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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	@SuppressWarnings("unchecked")
	public void onAdd(Event event) throws Exception {
		if (!CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE)) {
			return;
		}

		if (searchkelompokParameterTambahanAlumni.getSelectedItem() == null
				|| searchkelompokParameterTambahanAlumni.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sebelum bisa menambah data, kelompok harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							searchkelompokParameterTambahanAlumni.focus();
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

						ParameterTambahanAlumni parameterTambahanAlumni = new ParameterTambahanAlumni();
						parameterTambahanAlumni.setParameterTambahan(parameterTambahan);
						parameterTambahanAlumni.setProgram((String) (searchprogram.getSelectedItem() == null
								|| searchprogram.getSelectedItem().getValue() == null ? null
										: searchprogram.getSelectedItem().getValue()));

						parameterTambahanAlumni.setJenjang((Jenjang) (searchjenjang.getSelectedItem() == null
								|| searchjenjang.getSelectedItem().getValue() == null ? null
										: searchjenjang.getSelectedItem().getValue()));

						parameterTambahanAlumni.setKelompokParameterTambahanAlumni(
								(KelompokParameterTambahanAlumni) (searchkelompokParameterTambahanAlumni
										.getSelectedItem() == null ? null
												: searchkelompokParameterTambahanAlumni.getSelectedItem().getValue()));

						parameterTambahanAlumni.setFakultas((Fakultas) (searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
								|| searchfakultas.getSelectedItem().getValue() == null ? null
										: searchfakultas.getSelectedItem().getValue()));

						parameterTambahanAlumni.setJurusan((Jurusan) (searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
								|| searchjurusan.getSelectedItem().getValue() == null ? null
										: searchjurusan.getSelectedItem().getValue()));

						session.save(parameterTambahanAlumni);

					}

					onSearchDefault(arg0);

				}

			}
		});

		window.onModal();

	}

	private void init(ParameterTambahanAlumni parameterTambahanAlumni) {
		final Fakultas selectedFakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		final Jurusan selectedJurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		this.parameterTambahanAlumni = parameterTambahanAlumni;
		addWindow.setTitle(parameterTambahanAlumni.getId() == null ? "Tambah Parameter" : "Ubah Parameter");
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
		Common.insertCombo(kelompokParameterTambahanAlumni = new Combobox(), "nama", "keterangan",
				KelompokParameterTambahanAlumni.class);
		Common.selectComboItem(kelompokParameterTambahanAlumni,
				parameterTambahanAlumni.getKelompokParameterTambahanAlumni());
		row.appendChild(kelompokParameterTambahanAlumni);
		kelompokParameterTambahanAlumni.setWidth("90%");
		kelompokParameterTambahanAlumni.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				parameterTambahanAlumni.getFakultas() == null ? null : parameterTambahanAlumni.getFakultas());
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
				parameterTambahanAlumni.getJurusan() == null ? null : parameterTambahanAlumni.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		if (selectedJurusan != null) {
			Common.pilihJurusan(jurusan, selectedJurusan);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		Common.selectComboItem(jenjang,
				parameterTambahanAlumni.getJenjang() == null ? null : parameterTambahanAlumni.getJenjang());
		row.appendChild(jenjang);
		jenjang.setWidth("90%");

		Common.insertCombo(parameterTambahan = new Combobox(),
				new String[] { "labelInputan", "tipeDataInputan", "nilaiDataInputan" }, ParameterTambahan.class);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Parameter"));
		row.appendChild(parameterTambahan);
		parameterTambahan.setWidth("90%");
		Common.selectComboItem(parameterTambahan, parameterTambahanAlumni.getParameterTambahan());

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

		if (kelompokParameterTambahanAlumni.getSelectedItem() == null) {
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
		if (parameterTambahanAlumni.getId() != null) {
			parameterTambahanAlumni = (ParameterTambahanAlumni) session.load(ParameterTambahanAlumni.class,
					parameterTambahanAlumni.getId());

		}
		parameterTambahanAlumni.setProgram(
				(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));

		parameterTambahanAlumni.setJenjang(
				(Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue()));

		parameterTambahanAlumni.setKelompokParameterTambahanAlumni(
				(KelompokParameterTambahanAlumni) (kelompokParameterTambahanAlumni.getSelectedItem() == null ? null
						: kelompokParameterTambahanAlumni.getSelectedItem().getValue()));

		parameterTambahanAlumni.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		parameterTambahanAlumni.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		parameterTambahanAlumni
				.setParameterTambahan((ParameterTambahan) parameterTambahan.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, parameterTambahanAlumni);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ParameterTambahanAlumni.class).createAlias("parameterTambahan",
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
				.add(searchkelompokParameterTambahanAlumni.getSelectedItem() == null
						|| searchkelompokParameterTambahanAlumni.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kelompokParameterTambahanAlumni",
										searchkelompokParameterTambahanAlumni.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ParameterTambahanAlumni> parameterTambahanAlumni = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(parameterTambahanAlumni);
		grid.setRowRenderer(new ParameterTambahanAlumniRenderer());
		grid.setModelCheckMobile(strset);

	}

}
