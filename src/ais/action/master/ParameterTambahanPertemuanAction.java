package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Intbox;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataParameterTambahanBanyak;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KelompokParameterTambahanPertemuan;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPertemuan;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk parameter tambahan pertemuan. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox
 * searchkelompokParameterTambahanPertemuan}, {@code Combobox kelompokParameterTambahanPertemuan}, {@code
 * Combobox parameterTambahan}, {@code MyToolbarbuttonConfig find}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onSearchDefault()}); mutasi data ({@code onResetParameter()}, {@code onSave()}); operasi domain lain
 * ({@code onManajemenKelompok()}, {@code onManajemenParameter()}, {@code onAdd()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class ParameterTambahanPertemuanAction extends GenericAutowireComposer
		implements DataSearchDefault, DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Combobox searchkelompokParameterTambahanPertemuan;

	private Combobox kelompokParameterTambahanPertemuan;
	private Combobox parameterTambahan;

	private MyToolbarbuttonConfig find;

	private boolean edit = true;
	private boolean delete = true;

	private ParameterTambahanPertemuan parameterTambahanPertemuan;
	private Tabpanel manajemenKelompok;
	private Tabpanel manajemenParameter;

	private KelompokParameterTambahanPertemuan selected = null;

	public void onResetParameter(Event event) {
		Tbmuser tbmuser = Common.getCurrentUser();
		Criterion criterion = Restrictions.eq("aktif", true);

		criterion = Restrictions.and(criterion, tbmuser.ambilJurusan() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("jurusan", tbmuser.ambilJurusan()));

		criterion = Restrictions.and(criterion, tbmuser.ambilFakultas() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("fakultas", tbmuser.ambilFakultas()));

		criterion = Restrictions.and(criterion, tbmuser.ambilSekolah() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("sekolah", tbmuser.ambilSekolah()));

		criterion = Restrictions.and(criterion, tbmuser.ambilYayasan() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("yayasan", tbmuser.ambilYayasan()));

		Common.insertCombo(searchkelompokParameterTambahanPertemuan, "nama", KelompokParameterTambahanPertemuan.class,
				criterion);
		if (selected == null && !searchkelompokParameterTambahanPertemuan.getChildren().isEmpty()) {
			searchkelompokParameterTambahanPertemuan.setSelectedIndex(0);
		} else {
			Common.selectComboItem(searchkelompokParameterTambahanPertemuan, selected);
		}
		onSearchDefault(null);
	}

	public void onManajemenKelompok(Event event) {
		if (manajemenKelompok.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKelompok);
			MyInclude iframe = new MyInclude("/pages/master/kelompok_parameter_tambahan_pertemuan.zul");
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

		KelompokParameterTambahanPertemuan.checkCreateDefault();

		if (searchkelompokParameterTambahanPertemuan != null) {
			Tbmuser tbmuser = Common.getCurrentUser();

			Criterion criterion = Restrictions.eq("aktif", true);

			criterion = Restrictions.and(criterion, tbmuser.ambilJurusan() == null ? Restrictions.sqlRestriction("1=1")
					: Restrictions.eq("jurusan", tbmuser.ambilJurusan()));

			criterion = Restrictions.and(criterion, tbmuser.ambilFakultas() == null ? Restrictions.sqlRestriction("1=1")
					: Restrictions.eq("fakultas", tbmuser.ambilFakultas()));

			criterion = Restrictions.and(criterion, tbmuser.ambilSekolah() == null ? Restrictions.sqlRestriction("1=1")
					: Restrictions.eq("sekolah", tbmuser.ambilSekolah()));

			criterion = Restrictions.and(criterion, tbmuser.ambilYayasan() == null ? Restrictions.sqlRestriction("1=1")
					: Restrictions.eq("yayasan", tbmuser.ambilYayasan()));

			Common.insertCombo(searchkelompokParameterTambahanPertemuan, "nama",
					KelompokParameterTambahanPertemuan.class, criterion);
			if (selected == null && !searchkelompokParameterTambahanPertemuan.getChildren().isEmpty()) {
				searchkelompokParameterTambahanPertemuan.setSelectedIndex(0);
			} else {
				Common.selectComboItem(searchkelompokParameterTambahanPertemuan, selected);
			}

			onSearchDefault(null);
			Common.initPaging(paging, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);

				}
			});

			String[] contents = new String[] { "id", "parameterTambahan", "kelompokParameterTambahanPertemuan" };

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
			Common.appendKeToolbar(cetakToolbarbutton, find, comp);

			MyToolbarbuttonConfig upload = Common.uploadData(this, ParameterTambahanPertemuan.class, contents);
			Common.appendKeToolbar(upload, find, comp);
		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link ParameterTambahanPertemuanAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ParameterTambahanPertemuanAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see ParameterTambahanPertemuanAction
	 */
	class ParameterTambahanPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ParameterTambahanPertemuan parameterTambahanPertemuan = (ParameterTambahanPertemuan) arg1;
			new Label(parameterTambahanPertemuan.getKelompokParameterTambahanPertemuan().getNama()).setParent(arg0);

			RevisiHelper.createNewRevisi(ParameterTambahanPertemuan.class, parameterTambahanPertemuan,
					parameterTambahanPertemuan.getParameterTambahan().getLabelInputan()).setParent(arg0);
			new Label(parameterTambahanPertemuan.getParameterTambahan().getHarusMenyertakanLampiran() ? "Ya" : "Tidak")
					.setParent(arg0);

			new Label(parameterTambahanPertemuan.getParameterTambahan().getTipeDataInputan()).setParent(arg0);
			new Label(parameterTambahanPertemuan.getParameterTambahan().getNilaiDataInputan()).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			final MyCheckboxConfig perkuliahan = new MyCheckboxConfig("Perkuliahan");
			perkuliahan.setDisabled(!edit);
			perkuliahan.setChecked(parameterTambahanPertemuan.getPerkuliahan());
			perkuliahan.setParent(hbox);
			perkuliahan.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanPertemuan.setPerkuliahan(perkuliahan.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanPertemuan);
				}
			});

			final MyCheckboxConfig mahasiswaRequestTugasAkhir = new MyCheckboxConfig("Bimbingan Skr.");
			mahasiswaRequestTugasAkhir.setDisabled(!edit);
			mahasiswaRequestTugasAkhir.setChecked(parameterTambahanPertemuan.getMahasiswaRequestTugasAkhir());
			mahasiswaRequestTugasAkhir.setParent(hbox);
			mahasiswaRequestTugasAkhir.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanPertemuan.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanPertemuan);
				}
			});

			final MyCheckboxConfig skripsi = new MyCheckboxConfig("Sidang");
			skripsi.setDisabled(!edit);
			skripsi.setChecked(parameterTambahanPertemuan.getSkripsi());
			skripsi.setParent(hbox);
			skripsi.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanPertemuan.setSkripsi(skripsi.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanPertemuan);
				}
			});

			final MyCheckboxConfig krsMahasiswa = new MyCheckboxConfig("Bimbingan Akd.");
			krsMahasiswa.setDisabled(!edit);
			krsMahasiswa.setChecked(parameterTambahanPertemuan.getKrsMahasiswa());
			krsMahasiswa.setParent(hbox);
			krsMahasiswa.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanPertemuan.setKrsMahasiswa(krsMahasiswa.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanPertemuan);
				}
			});

			final MyCheckboxConfig kelompokKkn = new MyCheckboxConfig("KKN");
			kelompokKkn.setDisabled(!edit);
			kelompokKkn.setChecked(parameterTambahanPertemuan.getKelompokKkn());
			kelompokKkn.setParent(hbox);
			kelompokKkn.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanPertemuan.setKelompokKkn(kelompokKkn.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanPertemuan);
				}
			});

			final MyCheckboxConfig kelompokPkl = new MyCheckboxConfig("PKL");
			kelompokPkl.setDisabled(!edit);
			kelompokPkl.setChecked(parameterTambahanPertemuan.getKelompokPkl());
			kelompokPkl.setParent(hbox);
			kelompokPkl.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanPertemuan.setKelompokPkl(kelompokPkl.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanPertemuan);
				}
			});

			final MyCheckboxConfig formulirKegiatan = new MyCheckboxConfig("Keg.Lain");
			formulirKegiatan.setDisabled(!edit);
			formulirKegiatan.setChecked(parameterTambahanPertemuan.getFormulirKegiatan());
			formulirKegiatan.setParent(hbox);
			formulirKegiatan.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanPertemuan.setFormulirKegiatan(formulirKegiatan.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanPertemuan);
				}
			});
			
			
			
			final Intbox kolomKe = new Intbox(parameterTambahanPertemuan.getKolomKe());
			kolomKe.setWidth("90%");
			kolomKe.setParent(arg0);
			kolomKe.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanPertemuan.setKolomKe(kolomKe.getValue());
					Common.refreshSaveOrUpdate(parameterTambahanPertemuan);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(parameterTambahanPertemuan);
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

											Common.refreshDelete(parameterTambahanPertemuan);

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

		if (searchkelompokParameterTambahanPertemuan.getSelectedItem() == null
				|| searchkelompokParameterTambahanPertemuan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sebelum bisa menambah data, kelompok harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							searchkelompokParameterTambahanPertemuan.focus();
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

						ParameterTambahanPertemuan parameterTambahanPertemuan = new ParameterTambahanPertemuan();
						parameterTambahanPertemuan.setParameterTambahan(parameterTambahan);

						parameterTambahanPertemuan.setKelompokParameterTambahanPertemuan(
								(KelompokParameterTambahanPertemuan) (searchkelompokParameterTambahanPertemuan
										.getSelectedItem() == null ? null
												: searchkelompokParameterTambahanPertemuan.getSelectedItem()
														.getValue()));

						session.save(parameterTambahanPertemuan);

					}

					onSearchDefault(arg0);

				}

			}
		});

		window.onModal();

	}

	private void init(ParameterTambahanPertemuan parameterTambahanPertemuan) {

		this.parameterTambahanPertemuan = parameterTambahanPertemuan;
		addWindow.setTitle(parameterTambahanPertemuan.getId() == null ? "Tambah Parameter" : "Ubah Parameter");
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
		Common.insertCombo(kelompokParameterTambahanPertemuan = new Combobox(), "nama", "keterangan",
				KelompokParameterTambahanPertemuan.class);
		Common.selectComboItem(kelompokParameterTambahanPertemuan,
				parameterTambahanPertemuan.getKelompokParameterTambahanPertemuan());
		row.appendChild(kelompokParameterTambahanPertemuan);
		kelompokParameterTambahanPertemuan.setWidth("90%");
		kelompokParameterTambahanPertemuan.setReadonly(true);

		Common.insertCombo(parameterTambahan = new Combobox(),
				new String[] { "labelInputan", "tipeDataInputan", "nilaiDataInputan" }, ParameterTambahan.class);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Parameter"));
		row.appendChild(parameterTambahan);
		parameterTambahan.setWidth("90%");
		Common.selectComboItem(parameterTambahan, parameterTambahanPertemuan.getParameterTambahan());

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

		if (kelompokParameterTambahanPertemuan.getSelectedItem() == null) {
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
		if (parameterTambahanPertemuan.getId() != null) {
			parameterTambahanPertemuan = (ParameterTambahanPertemuan) session.load(ParameterTambahanPertemuan.class,
					parameterTambahanPertemuan.getId());

		}

		parameterTambahanPertemuan.setKelompokParameterTambahanPertemuan(
				(KelompokParameterTambahanPertemuan) (kelompokParameterTambahanPertemuan.getSelectedItem() == null
						? null
						: kelompokParameterTambahanPertemuan.getSelectedItem().getValue()));

		parameterTambahanPertemuan
				.setParameterTambahan((ParameterTambahan) parameterTambahan.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, parameterTambahanPertemuan);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		selected = (KelompokParameterTambahanPertemuan) (searchkelompokParameterTambahanPertemuan
				.getSelectedItem() == null ? null
						: searchkelompokParameterTambahanPertemuan.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ParameterTambahanPertemuan.class).createAlias("parameterTambahan",
				"parameterTambahan");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("parameterTambahan.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchkelompokParameterTambahanPertemuan.getSelectedItem() == null
						|| searchkelompokParameterTambahanPertemuan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kelompokParameterTambahanPertemuan",
										searchkelompokParameterTambahanPertemuan.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ParameterTambahanPertemuan> parameterTambahanPertemuan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(parameterTambahanPertemuan);
		grid.setRowRenderer(new ParameterTambahanPertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/*
	 * public Boolean checkNamaAgama() {
	 * 
	 * Integer kotaCount = null; Session session = HibernateUtil.currentSession();
	 * kotaCount = ((Number) session .createCriteria(Agama.class)
	 * .setProjection(Projections.rowCount()) .add(Restrictions.eq("nama",
	 * nama.getValue().trim())) .add(this.parameterTambahanPertemuan.getId() == null
	 * ? Restrictions .sqlRestriction("1=1") : Restrictions.ne("id",
	 * this.parameterTambahanPertemuan.getId())).uniqueResult()) .intValue();
	 * 
	 * return !kotaCount.equals(0); }
	 */

}
