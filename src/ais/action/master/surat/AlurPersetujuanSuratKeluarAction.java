package ais.action.master.surat;


import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.surat.helper.AmbilDataAlurPersetujuanSuratKeluarBanbox;
import ais.action.master.surat.helper.AmbilDataAlurPersetujuanSuratMasukBanbox;
import ais.action.master.surat.helper.AmbilDataKlasifikasiSuratMasukBanbox;
import ais.action.master.surat.helper.DasboardSurat;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.surat.AlurPersetujuanSuratKeluarDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.surat.AlurPersetujuanSuratKeluar;
import ais.database.model.surat.AlurPersetujuanSuratMasuk;
import ais.database.model.surat.KlasifikasiSuratMasuk;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk alur persetujuan surat keluar. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox jenisJabatan}, {@code Textbox nama},
 * {@code AmbilDataAlurPersetujuanSuratKeluarBanbox parent}, {@code MyCheckboxConfig defaultItem};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initKelengkapanBerkas()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi
 * data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
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
public class AlurPersetujuanSuratKeluarAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Combobox jenisJabatan;
	private Textbox nama;
	private AmbilDataAlurPersetujuanSuratKeluarBanbox parent;
	private MyCheckboxConfig defaultItem;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar;
	private MyToolbarbuttonConfig add;
	private Set<ais.database.model.employ.JenisJabatan> selectedJenisJabatan;
	private AmbilDataAlurPersetujuanSuratMasukBanbox alurPersetujuanSuratMasuk;
	// private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataKlasifikasiSuratMasukBanbox klasifikasiSuratMasuk;

	private boolean pt;
	private boolean ya;
	private Row hbFakultasLabel;
	private Row hbYayasan;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;
	private Combobox searchyayasan;
	private Checkbox searchaktif;
	private Combobox searchsekolah;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox fakultas;
	private Combobox jurusan;
	private MyCheckboxConfig harusMengikutiAlur;

	private String tipe = "surat";
	private MyCheckboxConfig terdapatPilihanSelesai;

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

		if (execution.getParameter("tipe") != null && !execution.getParameter("tipe").trim().isEmpty()) {
			tipe = execution.getParameter("tipe").trim();
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }
		if (hbYayasan != null) { hbYayasan.setVisible(ya); }

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class AlurPersetujuanSuratKeluarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) arg1;

			if (alurPersetujuanSuratKeluar.getTipe() == null) {
				alurPersetujuanSuratKeluar.setTipe(tipe);
				Common.refreshUpdate(alurPersetujuanSuratKeluar);
			}

			RevisiHelper.createNewRevisi(AlurPersetujuanSuratKeluar.class, alurPersetujuanSuratKeluar,
					alurPersetujuanSuratKeluar.getNama()).setParent(arg0);
			new Label(alurPersetujuanSuratKeluar.getJenisJabatan() == null ? ""
					: alurPersetujuanSuratKeluar.getJenisJabatan().getNama()).setParent(arg0);

			new ais.ui.util.MyHtml(DasboardSurat.buildParentAlurHtmlV20(
					alurPersetujuanSuratKeluar.getParent() == null ? ""
					: alurPersetujuanSuratKeluar.getParent().toString())).setParent(arg0);

			new ais.ui.util.MyHtml(DasboardSurat.buildUnitInfoHtmlV20(
					alurPersetujuanSuratKeluar.getSatuanKerja() == null ? "" : alurPersetujuanSuratKeluar.getSatuanKerja().getNama(),
					alurPersetujuanSuratKeluar.getFakultas() == null ? "" : alurPersetujuanSuratKeluar.getFakultas().getNama(),
					alurPersetujuanSuratKeluar.getJurusan() == null ? "" : alurPersetujuanSuratKeluar.getJurusan().getNama(),
					alurPersetujuanSuratKeluar.getYayasan() == null ? "" : alurPersetujuanSuratKeluar.getYayasan().getNama(),
					alurPersetujuanSuratKeluar.getSekolah() == null ? "" : alurPersetujuanSuratKeluar.getSekolah().getNama()
			)).setParent(arg0);

			new ais.ui.util.MyHtml(DasboardSurat.buildAktifBadgeHtmlV20(
					alurPersetujuanSuratKeluar.getDefaultItem())).setParent(arg0);

			new ais.ui.util.MyHtml(DasboardSurat.buildKeteranganHtmlV20(
					alurPersetujuanSuratKeluar.getKeterangan())).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(alurPersetujuanSuratKeluar);
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
											AlurPersetujuanSuratKeluarDao alurPersetujuanSuratKeluarDao = DaoFactory
													.getInstance().getAlurPersetujuanSuratKeluarDao();
											alurPersetujuanSuratKeluarDao.delete(
													alurPersetujuanSuratKeluarDao.merge(alurPersetujuanSuratKeluar));
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
		init(new AlurPersetujuanSuratKeluar());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar) throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();
		if (alurPersetujuanSuratKeluar.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			alurPersetujuanSuratKeluar.setFakultas(tbmuser.ambilFakultas());
		}

		if (alurPersetujuanSuratKeluar.getSatuanKerja() == null && tbmuser.ambilSatuanKerja() != null) {
			alurPersetujuanSuratKeluar.setSatuanKerja(tbmuser.ambilSatuanKerja());
		}

		if (alurPersetujuanSuratKeluar.getJurusan() == null && tbmuser.ambilJurusan() != null) {
			alurPersetujuanSuratKeluar.setJurusan(tbmuser.ambilJurusan());
		}

		if (alurPersetujuanSuratKeluar.getYayasan() == null && tbmuser.ambilYayasan() != null) {
			alurPersetujuanSuratKeluar.setYayasan(tbmuser.ambilYayasan());
		}

		if (alurPersetujuanSuratKeluar.getSekolah() == null && tbmuser.ambilSekolah() != null) {
			alurPersetujuanSuratKeluar.setSekolah(tbmuser.ambilSekolah());
		}

		this.alurPersetujuanSuratKeluar = alurPersetujuanSuratKeluar;
		addWindow.setTitle(alurPersetujuanSuratKeluar.getId() == null ? "Tambah Alur Persetujuan Surat Keluar" : "Ubah Alur Persetujuan Surat Keluar");
		Common.clear(addWindow);
		addWindow.setHeight("95%");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jabatan"));
		row.appendChild(jenisJabatan = new Combobox());
		Common.insertCombo(jenisJabatan, "nama", JenisJabatan.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisJabatan, alurPersetujuanSuratKeluar.getJenisJabatan());
		jenisJabatan.setWidth("90%");
		jenisJabatan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Alur Persetujuan"));
		row.appendChild(nama = new Textbox(
				alurPersetujuanSuratKeluar.getNama() == null ? "" : alurPersetujuanSuratKeluar.getNama()));
		nama.setWidth("90%");

		jenisJabatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisJabatan myJenisJabatan = (JenisJabatan) (jenisJabatan.getSelectedItem() == null ? null
						: jenisJabatan.getSelectedItem().getValue());
				if (myJenisJabatan != null && nama.getValue().trim().equals("")) {
					nama.setValue("Alur persetujuan ke " + myJenisJabatan.getNama());
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alur Sebelumnya"));
		row.appendChild(parent = new AmbilDataAlurPersetujuanSuratKeluarBanbox(tipe));
		parent.setAttribute("alurPersetujuanSuratKeluar", alurPersetujuanSuratKeluar.getParent());
		parent.setValue(alurPersetujuanSuratKeluar.getParent() == null ? ""
				: alurPersetujuanSuratKeluar.getParent().toString());
		parent.setWidth("90%");

		if (alurPersetujuanSuratKeluar.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			alurPersetujuanSuratKeluar.setFakultas(tbmuser.ambilFakultas());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(alurPersetujuanSuratKeluar.getSatuanKerja() == null ? ""
				: alurPersetujuanSuratKeluar.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", alurPersetujuanSuratKeluar.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		MyFormRow rowFakultas = new MyFormRow();
		rowFakultas.setVisible(pt);
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new MyLabelConfig("Fakultas"));
		rowFakultas.appendChild(fakultas);
		Common.selectComboItem(fakultas, alurPersetujuanSuratKeluar.getFakultas());
		fakultas.setWidth("90%");

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", alurPersetujuanSuratKeluar.getFakultas() == null ? tbmuser.ambilFakultas()
						: alurPersetujuanSuratKeluar.getFakultas()));

		MyFormRow rowJurusan = new MyFormRow();
		rowJurusan.setVisible(pt);
		rowJurusan.setStyle("border:0px;background: transparent;");
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new MyLabelConfig("Jurusan"));
		rowJurusan.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, alurPersetujuanSuratKeluar.getJurusan());

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				alurPersetujuanSuratKeluar == null || alurPersetujuanSuratKeluar.getYayasan() == null
						? tbmuser.ambilYayasan()
						: alurPersetujuanSuratKeluar.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				alurPersetujuanSuratKeluar == null || alurPersetujuanSuratKeluar.getSekolah() == null
						? tbmuser.ambilSekolah()
						: alurPersetujuanSuratKeluar.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(defaultItem = new MyCheckboxConfig("Aktif"));
		defaultItem.setChecked(alurPersetujuanSuratKeluar.getDefaultItem());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Harus Mengikuti Alur"));
		row.appendChild(harusMengikutiAlur = new MyCheckboxConfig());
		harusMengikutiAlur.setChecked(alurPersetujuanSuratKeluar.getHarusMengikutiAlur());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Terdapat Pilihan Selesai"));
		row.appendChild(terdapatPilihanSelesai = new MyCheckboxConfig());
		terdapatPilihanSelesai.setChecked(alurPersetujuanSuratKeluar.getTerdapatPilihanSelesai());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig tercopy;
		row.appendChild(tercopy = new MyCheckboxConfig("Alur ini ter-copy ke Surat Masuk"));
		tercopy.setChecked(alurPersetujuanSuratKeluar.getAlurPersetujuanSuratMasuk() != null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alur Surat Masuk"));
		row.appendChild(alurPersetujuanSuratMasuk = new AmbilDataAlurPersetujuanSuratMasukBanbox(tipe));
		alurPersetujuanSuratMasuk.setAttribute("alurPersetujuanSuratMasuk",
				alurPersetujuanSuratKeluar.getAlurPersetujuanSuratMasuk());
		alurPersetujuanSuratMasuk.setValue(alurPersetujuanSuratKeluar.getAlurPersetujuanSuratMasuk() == null ? ""
				: alurPersetujuanSuratKeluar.getAlurPersetujuanSuratMasuk().toString());
		alurPersetujuanSuratMasuk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Surat Masuk"));
		row.appendChild(klasifikasiSuratMasuk = new AmbilDataKlasifikasiSuratMasukBanbox(tipe));
		klasifikasiSuratMasuk.setAttribute("klasifikasiSuratMasuk",
				alurPersetujuanSuratKeluar.getKlasifikasiSuratMasuk());
		klasifikasiSuratMasuk.setValue(alurPersetujuanSuratKeluar.getKlasifikasiSuratMasuk() == null ? ""
				: alurPersetujuanSuratKeluar.getKlasifikasiSuratMasuk().getKode()
						+ (alurPersetujuanSuratKeluar.getKlasifikasiSuratMasuk().getNama() == null
								|| alurPersetujuanSuratKeluar.getKlasifikasiSuratMasuk().getNama().trim().isEmpty() ? ""
										: "-" + alurPersetujuanSuratKeluar.getKlasifikasiSuratMasuk().getNama()));

		klasifikasiSuratMasuk.setWidth("90%");
		klasifikasiSuratMasuk.setReadonly(true);

		EventListener tercopyEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				alurPersetujuanSuratMasuk.getParent().setVisible(tercopy.isChecked());
				klasifikasiSuratMasuk.getParent().setVisible(tercopy.isChecked());

			}
		};

		tercopyEventListener.onEvent(null);
		tercopy.addEventListener("onClick", tercopyEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				alurPersetujuanSuratKeluar.getKeterangan() == null ? "" : alurPersetujuanSuratKeluar.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		initKelengkapanBerkas(rows);

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

	@SuppressWarnings("deprecation")
	private void initKelengkapanBerkas(Rows rows) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Jabatan Lain"));
		final MyCheckboxConfig formulirVerifikasi;
		row.appendChild(formulirVerifikasi = new MyCheckboxConfig());
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Daftar Jabatan Lain");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		Tbmuser tbmuser = Common.getCurrentUser();

		@SuppressWarnings("unchecked")
		List<JenisJabatan> jenisJabatans = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(JenisJabatan.class)

						.add(Restrictions.and(
								Restrictions.or(Restrictions.isNull("usernamePengguna"),
										Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
												MatchMode.ANYWHERE)),
								Restrictions.or(Restrictions.isNull("jenisPengguna"),
										Restrictions.ilike("jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
												MatchMode.ANYWHERE))))

						.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				JenisJabatan.class);

		if (alurPersetujuanSuratKeluar.getId() != null) {
			alurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) HibernateUtil.currentSession()
					.createCriteria(AlurPersetujuanSuratKeluar.class)
					.add(Restrictions.idEq(alurPersetujuanSuratKeluar.getId())).uniqueResult();
		}
		selectedJenisJabatan = this.alurPersetujuanSuratKeluar.getJenisJabatans();

		subGrid.setVisible(!selectedJenisJabatan.isEmpty());
		formulirVerifikasi.setChecked(!selectedJenisJabatan.isEmpty());

		formulirVerifikasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				subGrid.setVisible(formulirVerifikasi.isChecked());
			}
		});

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final JenisJabatan jenisJabatan : jenisJabatans) {

			final Checkbox checkbox = new Checkbox(jenisJabatan.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedJenisJabatan.contains(jenisJabatan));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedJenisJabatan.add(jenisJabatan);
					} else {
						selectedJenisJabatan.remove(jenisJabatan);
					}
				}
			});
		}

	}

	public boolean onSave(Event event) throws Exception {

		if (jenisJabatan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Jabatan belum dipilih. Langkah yang dapat dilakukan: (1) klik pilihan Jenis Jabatan pada formulir; (2) pilih jenis jabatan yang sesuai dari daftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Alur Persetujuan Surat Keluar belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama pada formulir; (2) isikan nama alur persetujuan secara lengkap; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		AlurPersetujuanSuratKeluarDao alurPersetujuanSuratKeluarDao = DaoFactory.getInstance()
				.getAlurPersetujuanSuratKeluarDao();
		if (alurPersetujuanSuratKeluar.getId() != null) {
			alurPersetujuanSuratKeluar = alurPersetujuanSuratKeluarDao.load(alurPersetujuanSuratKeluar.getId());

		}

		alurPersetujuanSuratKeluar
				.setParent((AlurPersetujuanSuratKeluar) parent.getAttribute("alurPersetujuanSuratKeluar"));
		alurPersetujuanSuratKeluar.setDefaultItem(defaultItem.isChecked());
		alurPersetujuanSuratKeluar.setJenisJabatan((JenisJabatan) jenisJabatan.getSelectedItem().getValue());
		alurPersetujuanSuratKeluar.setNama(nama.getValue());
		alurPersetujuanSuratKeluar.setKeterangan(keterangan.getValue());
		alurPersetujuanSuratKeluar.setJenisJabatans(selectedJenisJabatan);
		alurPersetujuanSuratKeluar.setAlurPersetujuanSuratMasuk(
				(AlurPersetujuanSuratMasuk) alurPersetujuanSuratMasuk.getAttribute("alurPersetujuanSuratMasuk"));
		alurPersetujuanSuratKeluar.setKlasifikasiSuratMasuk(
				(KlasifikasiSuratMasuk) klasifikasiSuratMasuk.getAttribute("klasifikasiSuratMasuk"));
		alurPersetujuanSuratKeluar.setTerdapatPilihanSelesai(terdapatPilihanSelesai.isChecked());
		alurPersetujuanSuratKeluar.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		alurPersetujuanSuratKeluar.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		alurPersetujuanSuratKeluar.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		alurPersetujuanSuratKeluar.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		alurPersetujuanSuratKeluar.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		alurPersetujuanSuratKeluar.setHarusMengikutiAlur(harusMengikutiAlur.isChecked());

		alurPersetujuanSuratKeluar.setTipe(tipe);

		if (alurPersetujuanSuratKeluar.getId() != null) {
			alurPersetujuanSuratKeluarDao.update(alurPersetujuanSuratKeluar);
		} else {
			alurPersetujuanSuratKeluarDao.save(alurPersetujuanSuratKeluar);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AlurPersetujuanSuratKeluar.class)

				.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

				.add(searchaktif != null && searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("defaultItem"), Restrictions.eq("defaultItem", true))
						: Restrictions.sqlRestriction("true"))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<AlurPersetujuanSuratKeluar> alurPersetujuanSuratKeluar = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(alurPersetujuanSuratKeluar);
		grid.setRowRenderer(new AlurPersetujuanSuratKeluarRenderer());
		grid.setModelCheckMobile(strset);

	}

}
