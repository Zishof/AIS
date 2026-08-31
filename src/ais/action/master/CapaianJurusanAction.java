package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
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
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CapaianJurusan;
import ais.database.model.JenisCapaianJurusan;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk capaian jurusan. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Textbox nama}, {@code Decimalbox tahunLulus}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class CapaianJurusanAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	private Textbox nama;
	private Decimalbox tahunLulus;
	private Combobox fakultas;
	private Combobox jurusan;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private CapaianJurusan capaianJurusan;
	private MyToolbarbuttonConfig add;
	private Tbmuser tbmuser;
	private Jurusan jur;
	private Textbox namaEn;
	private Decimalbox nomorUrut;
	private Combobox jenisCapaianJurusan;

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

		tbmuser = Common.getCurrentUser();

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		if (execution.getParameter("jurusan") != null && Common.isNumber(execution.getParameter("jurusan"))) {
			jur = (Jurusan) HibernateUtil.currentSession().createCriteria(Jurusan.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("jurusan").trim()))).uniqueResult();
			if (jur != null) {
				Common.selectComboItem(searchfakultas, jur.getFakultas());
				searchfakultas.setDisabled(true);

				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.eq("fakultas", jur.getFakultas()));

				Common.selectComboItem(searchjurusan, jur);
				searchfakultas.setDisabled(true);
				searchjurusan.setDisabled(true);

			}
		} else {
			jur = tbmuser.ambilJurusan();
			if (jur != null) {
				Common.selectComboItem(searchfakultas, jur == null ? null : jur.getFakultas());
				searchfakultas.setDisabled(true);
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.eq("fakultas", jur == null ? null : jur.getFakultas()));
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

		String[] contents = new String[] { "id", "nama", "namaEn", "jurusan", "tahunLulus", "jenisCapaianJurusan",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CapaianJurusan.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, CapaianJurusan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link CapaianJurusanAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link CapaianJurusanAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see CapaianJurusanAction
	 */
	class CapaianJurusanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CapaianJurusan capaianJurusan = (CapaianJurusan) arg1;

			new Label(capaianJurusan.getJurusan().getNama()).setParent(arg0);
			RevisiHelper.createNewRevisi(CapaianJurusan.class, capaianJurusan, capaianJurusan.getNama())
					.setParent(arg0);
			new Label(capaianJurusan.getNamaEn()).setParent(arg0);
			new Label(capaianJurusan.getJenisCapaianJurusan() == null ? ""
					: capaianJurusan.getJenisCapaianJurusan().getNama()).setParent(arg0);
			new Label(capaianJurusan.getTahunLulus() == null ? "Semua" : capaianJurusan.getTahunLulus().toString())
					.setParent(arg0);
			new Label(capaianJurusan.getNomorUrut().toString()).setParent(arg0);
			new Label(capaianJurusan.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(capaianJurusan.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					capaianJurusan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(capaianJurusan);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(capaianJurusan);
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
											Common.refreshDelete(capaianJurusan);
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

	public void onAdd(Event event) throws Exception {
		init(new CapaianJurusan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(CapaianJurusan capaianJurusan) {

		if (jur != null) {
			capaianJurusan.setJurusan(jur);
		}

		this.capaianJurusan = capaianJurusan;
		addWindow.setTitle(capaianJurusan.getId() == null ? "Tambah Capaian" : "Ubah Capaian");
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

		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		Common.selectComboItem(fakultas, capaianJurusan.getJurusan() == null ? tbmuser.ambilFakultas()
				: capaianJurusan.getJurusan().getFakultas());
		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas *"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi *"));
		Common.pilihJurusan(jurusan,
				capaianJurusan.getJurusan() == null ? tbmuser.ambilJurusan() : capaianJurusan.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);

		if (jur != null) {
			fakultas.setDisabled(true);
			jurusan.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Capaian"));
		row.appendChild(jenisCapaianJurusan = new Combobox());
		Common.insertComboDanSemua(jenisCapaianJurusan, "nama", JenisCapaianJurusan.class);
		Common.selectComboItem(jenisCapaianJurusan, capaianJurusan.getJenisCapaianJurusan());
		jenisCapaianJurusan.setWidth("90%");
		jenisCapaianJurusan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Isi Capaian (Bahasa Indonesia) *"));
		row.appendChild(nama = new Textbox(capaianJurusan.getNama()));
		nama.setWidth("90%");
		nama.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Isi Capaian (English)"));
		row.appendChild(namaEn = new Textbox(capaianJurusan.getNamaEn()));
		namaEn.setWidth("90%");
		namaEn.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Lulus"));
		row.appendChild(tahunLulus = new Decimalbox(
				capaianJurusan.getTahunLulus() == null ? null : new BigDecimal(capaianJurusan.getTahunLulus())));

		Common.initKeterangan(rows, "Kosongkan tahun angkatan jika berlaku untuk semua tahun lulusan");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut"));
		row.appendChild(nomorUrut = new Decimalbox(new BigDecimal(capaianJurusan.getNomorUrut())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(capaianJurusan.getKeterangan()));
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
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Isi Capaian",
					"Kolom Isi Capaian belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Isi Capaian.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;

		}

		Session session = HibernateUtil.currentSession();
		if (capaianJurusan.getId() != null) {
			capaianJurusan = (CapaianJurusan) session.load(CapaianJurusan.class, capaianJurusan.getId());

		}
		capaianJurusan.setTahunLulus(tahunLulus.getValue() == null ? null : tahunLulus.getValue().intValue());
		capaianJurusan.setNomorUrut(nomorUrut.getValue() == null ? null : nomorUrut.getValue().intValue());
		capaianJurusan.setNama(nama.getValue());
		capaianJurusan.setNamaEn(namaEn.getValue());
		capaianJurusan.setKeterangan(keterangan.getValue());
		capaianJurusan.setJurusan((Jurusan) jurusan.getSelectedItem().getValue());
		capaianJurusan
				.setJenisCapaianJurusan((JenisCapaianJurusan) (jenisCapaianJurusan.getSelectedItem() == null ? null
						: jenisCapaianJurusan.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, capaianJurusan);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CapaianJurusan.class)
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));

		if (order)
			criteria.addOrder(Order.desc("tahunLulus")).addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CapaianJurusan> capaianJurusan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(capaianJurusan);
		grid.setRowRenderer(new CapaianJurusanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
