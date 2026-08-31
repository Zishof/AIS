package ais.action.master.sapto;


import ais.common.CommonSearchFilterHelper;
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
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.LampiranLain;
import ais.database.model.sapto.DanaPenggunaanSapto;
import ais.database.model.sapto.JenisDanaPenerimaanSapto;
import ais.database.model.sapto.JenisDanaPenggunaanSapto;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk dana penggunaan sapto. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchPerguruanTinggi}, {@code
 * Combobox searchJurusan}, {@code MyDatebox tanggal}, {@code Textbox keterangan}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onJenisPenggunaanData()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
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
public class DanaPenggunaanSaptoAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchPerguruanTinggi;
	private Combobox searchJurusan;

	private MyDatebox tanggal;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private DanaPenggunaanSapto danaPenggunaanSapto;
	private MyToolbarbuttonConfig add;
	private Combobox jenisDanaPenggunaanSapto;
	private MyDoublebox nilai;

	private Tabpanel jenisPenggunaanDataTab;
	protected LampiranLain lainMahasiswa;
	private Combobox sumberDana;
	private Combobox perguruanTinggi;
	private Combobox jurusan;

	public void onJenisPenggunaanData(Event event) {
		if (jenisPenggunaanDataTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisPenggunaanDataTab);
			MyInclude iframe = new MyInclude("/pages/master/sapto/jenis_dana_penggunaan_sapto.zul");
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

		Common.initJurusanDanSemua(searchJurusan, null, "== Untuk Semua Prodi ==");

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(JenisDanaPenggunaanSapto.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {

			JenisDanaPenggunaanSapto jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Penyelenggaraan Pendidikan");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENDIDIKAN);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Penelitian Tahun 2015");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENELITIAN);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Penelitian Tahun 2016");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENELITIAN);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Penelitian Tahun 2017");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENELITIAN);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Penelitian Tahun 2018");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENELITIAN);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Pengabdian Masyarakat Tahun 2015");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENGABDIAN);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Pengabdian Masyarakat Tahun 2016");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENGABDIAN);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Pengabdian Masyarakat Tahun 2017");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENGABDIAN);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Pengabdian Masyarakat Tahun 2018");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENGABDIAN);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Pengadaan Sarana");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_SARANA);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Pengadaan Prasarana");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_PRASARANA);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Peningkatan Mutu Pegawai");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_SDM);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Peningkatan Mutu Dosen");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_SDM);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Peningkatan SDM Lain-nya");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_SDM);
			session.save(jenisDanaPenggunaanSapto);

			jenisDanaPenggunaanSapto = new JenisDanaPenggunaanSapto();
			jenisDanaPenggunaanSapto.setNama("Penggunaan Dana Lain-Lain");
			jenisDanaPenggunaanSapto.setJenisPenggunaan(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_LAIN_LAIN);
			session.save(jenisDanaPenggunaanSapto);

		}

		Common.initComboPerguruanTinggi(searchPerguruanTinggi, null);
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

		String[] contents = new String[] { "id", "perguruanTinggi", "jurusan", "tanggal", "jenisDanaPenggunaanSapto",
				"sumberDana", "nilai", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, DanaPenggunaanSapto.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class DanaPenggunaanSaptoRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final DanaPenggunaanSapto danaPenggunaanSapto = (DanaPenggunaanSapto) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(DanaPenggunaanSapto.class, danaPenggunaanSapto,
					Common.dateFormat4.get().format(danaPenggunaanSapto.getTanggal()))).setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, danaPenggunaanSapto.getId(), DanaPenggunaanSapto.class.getName(),
					"Bukti Penggunaan", false, null, null, false, false, false, false);

			new Label(danaPenggunaanSapto.getJurusan() == null ? "Semua" : danaPenggunaanSapto.getJurusan().getNama())
					.setParent(arg0);

			new Label(danaPenggunaanSapto.getJenisDanaPenggunaanSapto() == null ? ""
					: danaPenggunaanSapto.getJenisDanaPenggunaanSapto().getNama()).setParent(arg0);
			new Label(danaPenggunaanSapto.getSumberDana()).setParent(arg0);
			new Label(Common.numberFormat.get().format(danaPenggunaanSapto.getNilai())).setParent(arg0);
			new Label(danaPenggunaanSapto.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, danaPenggunaanSapto, DanaPenggunaanSaptoAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new DanaPenggunaanSapto());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		danaPenggunaanSapto = (DanaPenggunaanSapto) obj;
		init(danaPenggunaanSapto);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(DanaPenggunaanSapto danaPenggunaanSapto) {
		this.danaPenggunaanSapto = danaPenggunaanSapto;
		addWindow.setTitle(danaPenggunaanSapto.getId() == null ? "Tambah Jenis Penggunaan Dana" : "Ubah Jenis Penggunaan Dana");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perguruan Tinggi *"));
		row.appendChild(perguruanTinggi = new Combobox());
		Common.initComboPerguruanTinggi(perguruanTinggi, danaPenggunaanSapto.getPerguruanTinggi());

		jurusan = new Combobox();
		Common.initJurusanDanSemua(jurusan, null, "== Untuk Semua Prodi ==");
		Common.pilihJurusan(jurusan, danaPenggunaanSapto.getJurusan());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diperuntukkan untuk prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Penggunaan Dana *"));
		row.appendChild(tanggal = new MyDatebox(danaPenggunaanSapto.getTanggal()));
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Penggunaan Dana *"));
		row.appendChild(jenisDanaPenggunaanSapto = new Combobox());
		Common.insertCombo(jenisDanaPenggunaanSapto, "nama", "jenisPenggunaan", JenisDanaPenggunaanSapto.class);
		Common.selectComboItem(jenisDanaPenggunaanSapto, danaPenggunaanSapto.getJenisDanaPenggunaanSapto());
		jenisDanaPenggunaanSapto.setReadonly(true);
		jenisDanaPenggunaanSapto.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sumber Dana *"));
		row.appendChild(sumberDana = new Combobox());
		MyComboitemConfig comboitemConfig = new MyComboitemConfig(JenisDanaPenerimaanSapto.SUMBER_DANA_MAHASISWA);
		comboitemConfig.setValue(JenisDanaPenerimaanSapto.SUMBER_DANA_MAHASISWA);
		sumberDana.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig(JenisDanaPenerimaanSapto.SUMBER_DANA_PT_SENDIRI);
		comboitemConfig.setValue(JenisDanaPenerimaanSapto.SUMBER_DANA_PT_SENDIRI);
		sumberDana.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig(JenisDanaPenerimaanSapto.SUMBER_DANA_YAYASAN);
		comboitemConfig.setValue(JenisDanaPenerimaanSapto.SUMBER_DANA_YAYASAN);
		sumberDana.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig(JenisDanaPenerimaanSapto.SUMBER_DANA_PEMERINTAH);
		comboitemConfig.setValue(JenisDanaPenerimaanSapto.SUMBER_DANA_PEMERINTAH);
		sumberDana.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig(JenisDanaPenerimaanSapto.SUMBER_DANA_DALAM_NEGERI);
		comboitemConfig.setValue(JenisDanaPenerimaanSapto.SUMBER_DANA_DALAM_NEGERI);
		sumberDana.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig(JenisDanaPenerimaanSapto.SUMBER_DANA_LUAR_NEGERI);
		comboitemConfig.setValue(JenisDanaPenerimaanSapto.SUMBER_DANA_LUAR_NEGERI);
		sumberDana.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig(JenisDanaPenerimaanSapto.SUMBER_DANA_LAIN);
		comboitemConfig.setValue(JenisDanaPenerimaanSapto.SUMBER_DANA_LAIN);
		sumberDana.appendChild(comboitemConfig);

		sumberDana.setWidth("90%");
		sumberDana.setReadonly(true);
		Common.selectComboItem(sumberDana, danaPenggunaanSapto.getSumberDana());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai *"));
		row.appendChild(nilai = new MyDoublebox(danaPenggunaanSapto.getNilai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(danaPenggunaanSapto.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lainMahasiswa = null;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran / Bukti Penggunaan Dana"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, danaPenggunaanSapto.getId(), DanaPenggunaanSapto.class.getName(),
				"Bukti Penggunaan Dana", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows,
				"Jika file \"Lampiran / Bukti Penggunaan Dana\" lebih dari satu file, zip dulu semua file tersebut");

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
		if (perguruanTinggi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Perguruan Tinggi harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Penggunaan Dana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisDanaPenggunaanSapto.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Penggunaan Dana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sumberDana.getSelectedItem() == null) {
			MyMessageboxConfig.show("Sumber Dana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (danaPenggunaanSapto.getId() != null) {
			danaPenggunaanSapto = (DanaPenggunaanSapto) session.load(DanaPenggunaanSapto.class,
					danaPenggunaanSapto.getId());

		}

		danaPenggunaanSapto.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue()));
		danaPenggunaanSapto.setPerguruanTinggi((PerguruanTinggi) perguruanTinggi.getSelectedItem().getValue());
		danaPenggunaanSapto.setNilai(nilai.getValue());
		danaPenggunaanSapto.setTanggal(tanggal.getValue());
		danaPenggunaanSapto.setJenisDanaPenggunaanSapto(
				(JenisDanaPenggunaanSapto) jenisDanaPenggunaanSapto.getSelectedItem().getValue());
		danaPenggunaanSapto.setKeterangan(keterangan.getValue());
		danaPenggunaanSapto.setSumberDana((String) sumberDana.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, danaPenggunaanSapto);

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(danaPenggunaanSapto.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e); 
			}

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DanaPenggunaanSapto.class);

		if (order)
			criteria.addOrder(Order.desc("tanggal"));
		criteria.add(searchPerguruanTinggi.getSelectedItem() == null
				|| searchPerguruanTinggi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("perguruanTinggi", searchPerguruanTinggi.getSelectedItem().getValue()))

				.add(searchJurusan.getSelectedItem() == null || searchJurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchJurusan, false));
		if (!searchnama.getValue().trim().isEmpty()) {
			criteria.createAlias("jenisDanaPenggunaanSapto", "jenisDana")
					.add(Restrictions.ilike("jenisDana.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DanaPenggunaanSapto> danaPenggunaanSapto = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(danaPenggunaanSapto);
		grid.setRowRenderer(new DanaPenggunaanSaptoRenderer());
		grid.setModelCheckMobile(strset);

	}

}
