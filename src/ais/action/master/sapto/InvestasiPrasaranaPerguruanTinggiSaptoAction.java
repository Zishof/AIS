package ais.action.master.sapto;

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
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.LampiranLain;
import ais.database.model.sapto.InvestasiPrasaranaPerguruanTinggiSapto;
import ais.database.model.sapto.JenisDanaPenerimaanSapto;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk investasi prasarana perguruan tinggi sapto. Tipe ini merupakan titik
 * masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi
 * khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchPerguruanTinggi}, {@code
 * Textbox nama}, {@code Textbox keterangan}, {@code boolean edit}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class InvestasiPrasaranaPerguruanTinggiSaptoAction extends GenericAutowireComposer
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

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private InvestasiPrasaranaPerguruanTinggiSapto investasiPrasaranaPerguruanTinggiSapto;
	private MyToolbarbuttonConfig add;

	protected LampiranLain lainMahasiswa;
	private Combobox perguruanTinggi;

	private MyDoublebox nilaiInventasiSelama3TahunTerakhir;
	private MyDoublebox rencanaInventasi;
	private Combobox sumberDana;

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

		String[] contents = new String[] { "id", "perguruanTinggi", "nama", "nilaiInventasiSelama3TahunTerakhir",
				"rencanaInventasi", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, InvestasiPrasaranaPerguruanTinggiSapto.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link InvestasiPrasaranaPerguruanTinggiSaptoAction}. Kelas ini
	 * menerjemahkan satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik
	 * kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link InvestasiPrasaranaPerguruanTinggiSaptoAction}
	 * dan dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see InvestasiPrasaranaPerguruanTinggiSaptoAction
	 */
	class InvestasiPrasaranaPerguruanTinggiSaptoRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final InvestasiPrasaranaPerguruanTinggiSapto investasiPrasaranaPerguruanTinggiSapto = (InvestasiPrasaranaPerguruanTinggiSapto) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(InvestasiPrasaranaPerguruanTinggiSapto.class,
					investasiPrasaranaPerguruanTinggiSapto, investasiPrasaranaPerguruanTinggiSapto.getNama()))
							.setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, investasiPrasaranaPerguruanTinggiSapto.getId(),
					InvestasiPrasaranaPerguruanTinggiSapto.class.getName(), "Inventasi Prasarana", false, null, null,
					false, false, false, false);

			new Label(Common.numberFormat.get()
					.format(investasiPrasaranaPerguruanTinggiSapto.getNilaiInventasiSelama3TahunTerakhir()))
							.setParent(arg0);
			new Label(Common.numberFormat.get().format(investasiPrasaranaPerguruanTinggiSapto.getRencanaInventasi()))
					.setParent(arg0);

			new Label(investasiPrasaranaPerguruanTinggiSapto.getSumberDana()).setParent(arg0);
			new Label(investasiPrasaranaPerguruanTinggiSapto.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, investasiPrasaranaPerguruanTinggiSapto,
					InvestasiPrasaranaPerguruanTinggiSaptoAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new InvestasiPrasaranaPerguruanTinggiSapto());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		investasiPrasaranaPerguruanTinggiSapto = (InvestasiPrasaranaPerguruanTinggiSapto) obj;
		init(investasiPrasaranaPerguruanTinggiSapto);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(InvestasiPrasaranaPerguruanTinggiSapto investasiPrasaranaPerguruanTinggiSapto) {
		this.investasiPrasaranaPerguruanTinggiSapto = investasiPrasaranaPerguruanTinggiSapto;
		addWindow.setTitle(investasiPrasaranaPerguruanTinggiSapto.getId() == null ? "Tambah Jenis Prasarana" : "Ubah Jenis Prasarana");
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
		Common.initComboPerguruanTinggi(perguruanTinggi, investasiPrasaranaPerguruanTinggiSapto.getPerguruanTinggi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Prasarana Tambahan *"));
		row.appendChild(nama = new Textbox(investasiPrasaranaPerguruanTinggiSapto.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		Common.initKeterangan(rows,
				"- Contoh prasarana tambahan (misalnya: kantor, ruang kelas, ruang laboratorium, studio, ruang perpustakaan, kebun percobaan, ruang dosen) yang digunakan institusi dalam penyelenggaraan program / kegiatan institusi");
		Common.initKeterangan(rows,
				"- Contoh prasarana tambahan lain yang mendukung terwujudnya visi (misalnya: tempat pembinaan minat dan bakat, kesejahteraan, ruang himpunan mahasiswa, asrama mahasiswa)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Inventasi Selama 3 Tahun Terakhir"));
		row.appendChild(nilaiInventasiSelama3TahunTerakhir = new MyDoublebox(
				investasiPrasaranaPerguruanTinggiSapto.getNilaiInventasiSelama3TahunTerakhir()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rencana Inventasi"));
		row.appendChild(
				rencanaInventasi = new MyDoublebox(investasiPrasaranaPerguruanTinggiSapto.getRencanaInventasi()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sumber Dana"));
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
		Common.selectComboItem(sumberDana, investasiPrasaranaPerguruanTinggiSapto.getSumberDana());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(investasiPrasaranaPerguruanTinggiSapto.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lainMahasiswa = null;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Inventasi Prasarana"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, investasiPrasaranaPerguruanTinggiSapto.getId(),
				InvestasiPrasaranaPerguruanTinggiSapto.class.getName(), "Inventasi Prasarana", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows,
				"Jika file \"Lampiran Inventasi Prasarana\" lebih dari satu file, zip dulu semua file tersebut");

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
		if (nama.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Jenis Prasarana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (investasiPrasaranaPerguruanTinggiSapto.getId() != null) {
			investasiPrasaranaPerguruanTinggiSapto = (InvestasiPrasaranaPerguruanTinggiSapto) session
					.load(InvestasiPrasaranaPerguruanTinggiSapto.class, investasiPrasaranaPerguruanTinggiSapto.getId());

		}

		investasiPrasaranaPerguruanTinggiSapto
				.setPerguruanTinggi((PerguruanTinggi) perguruanTinggi.getSelectedItem().getValue());
		investasiPrasaranaPerguruanTinggiSapto.setNama(nama.getValue().trim());
		investasiPrasaranaPerguruanTinggiSapto
				.setNilaiInventasiSelama3TahunTerakhir(nilaiInventasiSelama3TahunTerakhir.getValue());
		investasiPrasaranaPerguruanTinggiSapto.setRencanaInventasi(rencanaInventasi.getValue());
		investasiPrasaranaPerguruanTinggiSapto.setSumberDana(
				(String) (sumberDana.getSelectedItem() == null ? null : sumberDana.getSelectedItem().getValue()));
		investasiPrasaranaPerguruanTinggiSapto.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, investasiPrasaranaPerguruanTinggiSapto);

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(investasiPrasaranaPerguruanTinggiSapto.getId());

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
		Criteria criteria = session.createCriteria(InvestasiPrasaranaPerguruanTinggiSapto.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchPerguruanTinggi.getSelectedItem() == null
				|| searchPerguruanTinggi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("perguruanTinggi", searchPerguruanTinggi.getSelectedItem().getValue()))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<InvestasiPrasaranaPerguruanTinggiSapto> investasiPrasaranaPerguruanTinggiSapto = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(investasiPrasaranaPerguruanTinggiSapto);
		grid.setRowRenderer(new InvestasiPrasaranaPerguruanTinggiSaptoRenderer());
		grid.setModelCheckMobile(strset);

	}

}
