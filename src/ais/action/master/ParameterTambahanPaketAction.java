package ais.action.master;

import java.util.ArrayList;
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
import ais.common.CommonPrivilages;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.KelompokParameterTambahanCalonMahasiswa;
import ais.database.model.Paket;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPaket;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk parameter tambahan paket. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchpaket}, {@code Combobox
 * searchkelompokParameterTambahanCalonMahasiswa}, {@code Combobox kelompokParameterTambahanCalonMahasiswa},
 * {@code Combobox paket}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code
 * init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code
 * onResetParameter()}, {@code onSave()}); operasi domain lain ({@code onKonfigurasiCalonBiodataMahasiswa()},
 * {@code onKonfigurasiLoginCalonMahasiswa()}, {@code onManajemenKelompok()}, {@code onManajemenParameter()},
 * {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class ParameterTambahanPaketAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchpaket;
	private Combobox searchkelompokParameterTambahanCalonMahasiswa;

	private Combobox kelompokParameterTambahanCalonMahasiswa;
	private Combobox paket;
	private Combobox parameterTambahan;

	private boolean edit = false;
	private boolean delete = false;

	private MyToolbarbuttonConfig find;
	private ParameterTambahanPaket parameterTambahanPaket;

	private Paket selectedPaket;
	private Label labelsearchpaket;

	public void onResetParameter(Event event) {
		Common.insertCombo(searchkelompokParameterTambahanCalonMahasiswa, "nama",
				KelompokParameterTambahanCalonMahasiswa.class);
		if (!searchkelompokParameterTambahanCalonMahasiswa.getChildren().isEmpty()) {
			searchkelompokParameterTambahanCalonMahasiswa.setSelectedIndex(0);
		}
		onSearchDefault(null);
	}

	private Tabpanel manajemenKelompok;

	private Tabpanel konfigurasiCalonBiodataMahasiswa;

	public void onKonfigurasiCalonBiodataMahasiswa(Event event) {
		if (konfigurasiCalonBiodataMahasiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(konfigurasiCalonBiodataMahasiswa);
			MyInclude iframe = new MyInclude("/pages/master/konfigurasi_biodata_calon_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel konfigurasiLoginCalonMahasiswa;

	public void onKonfigurasiLoginCalonMahasiswa(Event event) {
		if (konfigurasiLoginCalonMahasiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(konfigurasiLoginCalonMahasiswa);
			MyInclude iframe = new MyInclude("/pages/master/konfigurasi_login_calon_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenKelompok(Event event) {
		if (manajemenKelompok.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKelompok);
			MyInclude iframe = new MyInclude("/pages/master/kelompok_parameter_tambahan_calon_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel manajemenParameter;

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

	// private MyToolbarbuttonConfig add;

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

		KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa = KelompokParameterTambahanCalonMahasiswa
				.checkCreateDefault();
		HibernateUtil.currentSession()
				.createSQLQuery("update parameter_tambahan_paket set kelompok_parameter_tambahan_calon_mahasiswa="
						+ kelompokParameterTambahanCalonMahasiswa.getId()
						+ " where kelompok_parameter_tambahan_calon_mahasiswa is null;")
				.executeUpdate();

		Common.insertCombo(paket = new Combobox(), "nama", "keterangan", Paket.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertCombo(searchpaket, "nama", "keterangan", Paket.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (execution.getParameter("paket") != null) {
			selectedPaket = (Paket) HibernateUtil.currentSession().createCriteria(Paket.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("paket")))).uniqueResult();
			Common.selectComboItem(searchpaket, selectedPaket);
			searchpaket.setDisabled(true);
		} else {
			labelsearchpaket.setVisible(false);
			searchpaket.setVisible(false);
		}

		Common.insertCombo(searchkelompokParameterTambahanCalonMahasiswa, "nama",
				KelompokParameterTambahanCalonMahasiswa.class);
		if (!searchkelompokParameterTambahanCalonMahasiswa.getChildren().isEmpty()) {
			searchkelompokParameterTambahanCalonMahasiswa.setSelectedIndex(0);
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

		String[] contents = new String[] { "id", "parameterTambahan", "tampilDiSemuaGelombang", "gelombangs",
				"kelompokParameterTambahanCalonMahasiswa", "paket" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ParameterTambahanPaket.class, contents);
		Common.appendKeToolbar(upload, find, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link ParameterTambahanPaketAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ParameterTambahanPaketAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code tampil()}, {@code render}(). Aturan
	 * bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see ParameterTambahanPaketAction
	 */
	class ParameterTambahanPaketRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		private void tampil(final ParameterTambahanPaket parameterTambahanPaket, String tahunAkademik,
				final Vbox vbox) {
			Common.clear(vbox);
			if (!parameterTambahanPaket.getTampilDiSemuaGelombang()) {
				List<GelombangPendaftaran> gelombangPendaftarans = HibernateUtil.currentSession()
						.createCriteria(GelombangPendaftaran.class).add(Restrictions.eq("tahunAkademik", tahunAkademik))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.desc("id")).list();
				List<Long> idsSelected = new ArrayList<Long>();
				for (String s : parameterTambahanPaket.getGelombangs().split(";")) {
					if (!s.isEmpty() && Common.isNumber(s)) {
						idsSelected.add(Long.parseLong(s.trim()));
					}
				}
				for (GelombangPendaftaran gelombangPendaftaran : gelombangPendaftarans) {
					final MyCheckboxConfig checkbox = new MyCheckboxConfig(
							gelombangPendaftaran.getNama() + " " + gelombangPendaftaran.getTahunAkademik() + " "
									+ (gelombangPendaftaran.getJenisSeleksi() == null ? ""
											: gelombangPendaftaran.getJenisSeleksi().getNama())
									+ " " + gelombangPendaftaran.getJenisSemester());
					checkbox.setAttribute("gelombangPendaftaran", gelombangPendaftaran);
					checkbox.setChecked(idsSelected.contains(gelombangPendaftaran.getId()));
					checkbox.setParent(vbox);
					checkbox.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<MyCheckboxConfig> checkboxs = vbox.getChildren();
							String gelombangs = "";
							for (MyCheckboxConfig checkbox : checkboxs) {
								if (checkbox.isChecked()) {
									GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) checkbox
											.getAttribute("gelombangPendaftaran");
									gelombangs += ";" + gelombangPendaftaran.getId() + ";";
								}
							}
							parameterTambahanPaket.setGelombangs(gelombangs);
							Common.refreshSaveOrUpdate(parameterTambahanPaket);
						}
					});
				}

			}
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ParameterTambahanPaket parameterTambahanPaket = (ParameterTambahanPaket) arg1;
			new Label(parameterTambahanPaket.getKelompokParameterTambahanCalonMahasiswa().getNama()).setParent(arg0);

			RevisiHelper.createNewRevisi(ParameterTambahanPaket.class, parameterTambahanPaket,
					parameterTambahanPaket.getParameterTambahan().getLabelInputan()).setParent(arg0);
			new Label(parameterTambahanPaket.getParameterTambahan().getHarusMenyertakanLampiran() ? "Ya" : "Tidak")
					.setParent(arg0);

			new Label(parameterTambahanPaket.getParameterTambahan().getTipeDataInputan()).setParent(arg0);
			new Label(parameterTambahanPaket.getParameterTambahan().getNilaiDataInputan()).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			hbox.setWidth("100%");
			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Semua");
			checkbox.setChecked(parameterTambahanPaket.getTampilDiSemuaGelombang());
			checkbox.setParent(hbox);

			final Combobox tahunAjaran;
			Common.generateTahunAjaran(tahunAjaran = new Combobox());
			hbox.appendChild(tahunAjaran);
			tahunAjaran.setCols(4);
			String tahunAkademikPenerimaanMahasiswaBaru = Common
					.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik())
					.getNilai();
			Common.selectComboItem(tahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);
			tahunAjaran.setReadonly(true);

			tahunAjaran.setVisible(!checkbox.isChecked());

			final Vbox vbox = new Vbox();
			vbox.setWidth("100%");
			vbox.setParent(hbox);

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tahunAjaran.setVisible(!checkbox.isChecked());
					parameterTambahanPaket.setTampilDiSemuaGelombang(checkbox.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanPaket);
					String ta = (String) tahunAjaran.getSelectedItem().getValue();
					tampil(parameterTambahanPaket, ta, vbox);
				}
			});

			EventListener gelombangEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String ta = (String) tahunAjaran.getSelectedItem().getValue();
					tampil(parameterTambahanPaket, ta, vbox);
				}
			};

			gelombangEventListener.onEvent(null);
			tahunAjaran.addEventListener("onChange", gelombangEventListener);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(parameterTambahanPaket);
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

									Common.refreshDelete(parameterTambahanPaket);

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

	@SuppressWarnings("unchecked")
	public void onAdd(Event event) throws Exception {
		if (!CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE)) {
			return;
		}

		if (searchkelompokParameterTambahanCalonMahasiswa.getSelectedItem() == null
				|| searchkelompokParameterTambahanCalonMahasiswa.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sebelum bisa menambah data, kelompok harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							searchkelompokParameterTambahanCalonMahasiswa.focus();
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

						ParameterTambahanPaket parameterTambahanPaket = new ParameterTambahanPaket();
						parameterTambahanPaket.setParameterTambahan(parameterTambahan);
						parameterTambahanPaket.setPaket(selectedPaket);
						parameterTambahanPaket.setKelompokParameterTambahanCalonMahasiswa(
								(KelompokParameterTambahanCalonMahasiswa) (searchkelompokParameterTambahanCalonMahasiswa
										.getSelectedItem() == null ? null
												: searchkelompokParameterTambahanCalonMahasiswa.getSelectedItem()
														.getValue()));

						session.save(parameterTambahanPaket);

					}

					onSearchDefault(arg0);

				}

			}
		});

		window.onModal();

	}

	private void init(ParameterTambahanPaket parameterTambahanPaket) {
		this.parameterTambahanPaket = parameterTambahanPaket;
		addWindow.setTitle(parameterTambahanPaket.getId() == null ? "Tambah Parameter" : "Ubah Parameter");
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
		Common.insertCombo(kelompokParameterTambahanCalonMahasiswa = new Combobox(), "nama", "keterangan",
				KelompokParameterTambahanCalonMahasiswa.class);
		Common.selectComboItem(kelompokParameterTambahanCalonMahasiswa,
				parameterTambahanPaket.getKelompokParameterTambahanCalonMahasiswa());
		row.appendChild(kelompokParameterTambahanCalonMahasiswa);
		kelompokParameterTambahanCalonMahasiswa.setWidth("90%");
		kelompokParameterTambahanCalonMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket"));
		Common.selectComboItem(paket,
				parameterTambahanPaket.getPaket() == null ? null : parameterTambahanPaket.getPaket());
		row.appendChild(paket);
		paket.setWidth("90%");

		if (selectedPaket != null) {
			Common.selectComboItem(paket, selectedPaket);
			paket.setDisabled(true);
		}

		Common.insertCombo(parameterTambahan = new Combobox(),
				new String[] { "labelInputan", "tipeDataInputan", "nilaiDataInputan" }, ParameterTambahan.class);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Parameter"));
		row.appendChild(parameterTambahan);
		parameterTambahan.setWidth("90%");
		Common.selectComboItem(parameterTambahan, parameterTambahanPaket.getParameterTambahan());

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

		if (kelompokParameterTambahanCalonMahasiswa.getSelectedItem() == null) {
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
		if (parameterTambahanPaket.getId() != null) {
			parameterTambahanPaket = (ParameterTambahanPaket) session.load(ParameterTambahanPaket.class,
					parameterTambahanPaket.getId());

		}
		parameterTambahanPaket.setKelompokParameterTambahanCalonMahasiswa(
				(KelompokParameterTambahanCalonMahasiswa) (kelompokParameterTambahanCalonMahasiswa
						.getSelectedItem() == null ? null
								: kelompokParameterTambahanCalonMahasiswa.getSelectedItem().getValue()));

		parameterTambahanPaket
				.setPaket((Paket) (paket.getSelectedItem() == null ? null : paket.getSelectedItem().getValue()));
		parameterTambahanPaket.setParameterTambahan((ParameterTambahan) parameterTambahan.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, parameterTambahanPaket);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ParameterTambahanPaket.class).createAlias("parameterTambahan",
				"parameterTambahan");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchkelompokParameterTambahanCalonMahasiswa.getSelectedItem() == null
				|| searchkelompokParameterTambahanCalonMahasiswa.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kelompokParameterTambahanCalonMahasiswa",
								searchkelompokParameterTambahanCalonMahasiswa.getSelectedItem().getValue()))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("parameterTambahan.nama", searchnama.getValue().trim(),
								MatchMode.ANYWHERE))
				.add(searchpaket.getSelectedItem() == null ? Restrictions.isNull("paket")
						: Restrictions.eq("paket", searchpaket.getSelectedItem().getValue()));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ParameterTambahanPaket> parameterTambahanPaket = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(parameterTambahanPaket);
		grid.setRowRenderer(new ParameterTambahanPaketRenderer());
		grid.setModelCheckMobile(strset);

	}

	/*
	 * public Boolean checkNamaAgama() {
	 * 
	 * Integer kotaCount = null; Session session =
	 * HibernateUtil.currentSession(); kotaCount = ((Number) session
	 * .createCriteria(Agama.class) .setProjection(Projections.rowCount())
	 * .add(Restrictions.eq("nama", nama.getValue().trim()))
	 * .add(this.parameterTambahanPaket.getId() == null ? Restrictions
	 * .sqlRestriction("1=1") : Restrictions.ne("id",
	 * this.parameterTambahanPaket.getId())).uniqueResult()) .intValue();
	 * 
	 * return !kotaCount.equals(0); }
	 */

}
