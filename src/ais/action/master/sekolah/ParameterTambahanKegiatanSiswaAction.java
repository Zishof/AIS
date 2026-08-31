package ais.action.master.sekolah;

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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataParameterTambahanBanyak;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.sekolah.KelompokKegiatanSiswa;
import ais.database.model.sekolah.ParameterTambahanKegiatanSiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk parameter tambahan kegiatan siswa. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchkelompokKegiatanSiswa}, {@code
 * Combobox kelompokKegiatanSiswa}, {@code Combobox parameterTambahan}, {@code MyToolbarbuttonConfig find};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onResetParameter()},
 * {@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
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
public class ParameterTambahanKegiatanSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchkelompokKegiatanSiswa;

	private Combobox kelompokKegiatanSiswa;
	private Combobox parameterTambahan;

	private MyToolbarbuttonConfig find;

	private boolean edit = true;
	private boolean delete = true;

	private ParameterTambahanKegiatanSiswa parameterTambahanKegiatanSiswa;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void onResetParameter(Event event) {
		Common.insertCombo(searchkelompokKegiatanSiswa, "nama", KelompokKegiatanSiswa.class);
		if (!searchkelompokKegiatanSiswa.getChildren().isEmpty()) {
			searchkelompokKegiatanSiswa.setSelectedIndex(0);
		}
		onSearchDefault(null);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		KelompokKegiatanSiswa.checkCreateDefault();

		Common.insertCombo(searchkelompokKegiatanSiswa, "nama", KelompokKegiatanSiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (!searchkelompokKegiatanSiswa.getChildren().isEmpty()) {
			searchkelompokKegiatanSiswa.setSelectedIndex(0);
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "parameterTambahan", "kelompokKegiatanSiswa" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ParameterTambahanKegiatanSiswa.class, contents);
		Common.appendKeToolbar(upload, find, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link ParameterTambahanKegiatanSiswaAction}. Kelas ini menerjemahkan
	 * satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ParameterTambahanKegiatanSiswaAction} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see ParameterTambahanKegiatanSiswaAction
	 */
	class ParameterTambahanKegiatanSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ParameterTambahanKegiatanSiswa parameterTambahanKegiatanSiswa = (ParameterTambahanKegiatanSiswa) arg1;
			new Label(parameterTambahanKegiatanSiswa.getKelompokKegiatanSiswa().getNama()).setParent(arg0);

			RevisiHelper.createNewRevisi(ParameterTambahanKegiatanSiswa.class, parameterTambahanKegiatanSiswa,
					parameterTambahanKegiatanSiswa.getParameterTambahan().getLabelInputan()).setParent(arg0);
			new Label(parameterTambahanKegiatanSiswa.getParameterTambahan().getHarusMenyertakanLampiran() ? "Ya"
					: "Tidak").setParent(arg0);

			new Label(parameterTambahanKegiatanSiswa.getParameterTambahan().getTipeDataInputan()).setParent(arg0);
			new Label(parameterTambahanKegiatanSiswa.getParameterTambahan().getNilaiDataInputan()).setParent(arg0);

			final MyCheckboxConfig wajib = new MyCheckboxConfig("Isian Wajib");
			wajib.setChecked(parameterTambahanKegiatanSiswa.getWajibDiisi());
			wajib.setParent(arg0);
			wajib.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanKegiatanSiswa.setWajibDiisi(wajib.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanKegiatanSiswa);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(parameterTambahanKegiatanSiswa);
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

											Common.refreshDelete(parameterTambahanKegiatanSiswa);

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

		if (searchkelompokKegiatanSiswa.getSelectedItem() == null
				|| searchkelompokKegiatanSiswa.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sebelum bisa menambah data, kelompok harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							searchkelompokKegiatanSiswa.focus();
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

						ParameterTambahanKegiatanSiswa parameterTambahanKegiatanSiswa = new ParameterTambahanKegiatanSiswa();
						parameterTambahanKegiatanSiswa.setParameterTambahan(parameterTambahan);

						parameterTambahanKegiatanSiswa.setKelompokKegiatanSiswa(
								(KelompokKegiatanSiswa) (searchkelompokKegiatanSiswa.getSelectedItem() == null ? null
										: searchkelompokKegiatanSiswa.getSelectedItem().getValue()));

						session.save(parameterTambahanKegiatanSiswa);

					}

					onSearchDefault(arg0);

				}

			}
		});

		window.onModal();

	}

	private void init(ParameterTambahanKegiatanSiswa parameterTambahanKegiatanSiswa) {

		this.parameterTambahanKegiatanSiswa = parameterTambahanKegiatanSiswa;
		addWindow.setTitle(parameterTambahanKegiatanSiswa.getId() == null ? "Tambah Parameter" : "Ubah Parameter");
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
		Common.insertCombo(kelompokKegiatanSiswa = new Combobox(), "nama", "keterangan", KelompokKegiatanSiswa.class);
		Common.selectComboItem(kelompokKegiatanSiswa, parameterTambahanKegiatanSiswa.getKelompokKegiatanSiswa());
		row.appendChild(kelompokKegiatanSiswa);
		kelompokKegiatanSiswa.setWidth("90%");
		kelompokKegiatanSiswa.setReadonly(true);

		Common.insertCombo(parameterTambahan = new Combobox(),
				new String[] { "labelInputan", "tipeDataInputan", "nilaiDataInputan" }, ParameterTambahan.class);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Parameter"));
		row.appendChild(parameterTambahan);
		parameterTambahan.setWidth("90%");
		Common.selectComboItem(parameterTambahan, parameterTambahanKegiatanSiswa.getParameterTambahan());

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

		if (kelompokKegiatanSiswa.getSelectedItem() == null) {
			MyMessageboxConfig.show("Kelompok Parameter harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (parameterTambahan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Nama Parameter harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (parameterTambahanKegiatanSiswa.getId() != null) {
			parameterTambahanKegiatanSiswa = (ParameterTambahanKegiatanSiswa) session
					.load(ParameterTambahanKegiatanSiswa.class, parameterTambahanKegiatanSiswa.getId());

		}

		parameterTambahanKegiatanSiswa.setKelompokKegiatanSiswa(
				(KelompokKegiatanSiswa) (kelompokKegiatanSiswa.getSelectedItem() == null ? null
						: kelompokKegiatanSiswa.getSelectedItem().getValue()));

		parameterTambahanKegiatanSiswa
				.setParameterTambahan((ParameterTambahan) parameterTambahan.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, parameterTambahanKegiatanSiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ParameterTambahanKegiatanSiswa.class)
				.createAlias("parameterTambahan", "parameterTambahan");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("parameterTambahan.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchkelompokKegiatanSiswa.getSelectedItem() == null
						|| searchkelompokKegiatanSiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kelompokKegiatanSiswa",
										searchkelompokKegiatanSiswa.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ParameterTambahanKegiatanSiswa> parameterTambahanKegiatanSiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(parameterTambahanKegiatanSiswa);
		grid.setRowRenderer(new ParameterTambahanKegiatanSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
