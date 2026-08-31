package ais.action.master;

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

import ais.action.master.helper.AmbilDataNegaraBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisKerjasama;
import ais.database.model.Jurusan;
import ais.database.model.KerjasamaAntarInstansi;
import ais.database.model.Negara;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk kerjasama antar instansi. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox jenissemester}, {@code Textbox
 * nama}, {@code Combobox jenisKerjasama}, {@code AmbilDataNegaraBanbox negara}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onJenisKerjasama()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
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
public class KerjasamaAntarInstansiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox jenissemester;
	private Textbox nama;
	private Combobox jenisKerjasama;
	private AmbilDataNegaraBanbox negara;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private Textbox manfaat;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KerjasamaAntarInstansi kerjasamaAntarInstansi;
	private MyToolbarbuttonConfig add;
	private Combobox fakultas;
	private Combobox jurusan;

	private Tabpanel jenisKerjasamaTab;
	private Combobox tingkat;
	private Textbox bukti;
	private Combobox jenis;
	protected LampiranLain lainMahasiswa;

	public void onJenisKerjasama(Event event) {
		if (jenisKerjasamaTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisKerjasamaTab);
			MyInclude iframe = new MyInclude("/pages/master/jenis_kerjasama.zul");
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
		Common.insertComboDanSemua(jenissemester, "nama", JenisKerjasama.class);

		String[] contents = new String[] { "id", "nama", "jenisKerjasama", "fakultas", "jurusan", "negara", "mulai",
				"sampai", "manfaat", "tingkat", "jenis", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KerjasamaAntarInstansi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class KerjasamaAntarInstansiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KerjasamaAntarInstansi kerjasamaAntarInstansi = (KerjasamaAntarInstansi) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(KerjasamaAntarInstansi.class, kerjasamaAntarInstansi,
					kerjasamaAntarInstansi.getNama())).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(a);
			LampiranLain.createDownloadUploadFileLain(hbox, kerjasamaAntarInstansi.getId(),
					KerjasamaAntarInstansi.class.getName(), "Bukti", false, null, null, false, false, false, false);

			new Label(kerjasamaAntarInstansi.getJenisKerjasama() == null ? ""
					: kerjasamaAntarInstansi.getJenisKerjasama().getNama()).setParent(arg0);
			new Label(kerjasamaAntarInstansi.getTingkat()).setParent(arg0);
			new Label(kerjasamaAntarInstansi.getJenis()).setParent(arg0);
			new Label(kerjasamaAntarInstansi.getNegara() == null ? ""
					: kerjasamaAntarInstansi.getNegara().getNamaNegara()).setParent(arg0);
			new Label(kerjasamaAntarInstansi.getMulai() == null ? ""
					: Common.dateFormat2.get().format(kerjasamaAntarInstansi.getMulai())).setParent(arg0);
			new Label(kerjasamaAntarInstansi.getSampai() == null ? ""
					: Common.dateFormat2.get().format(kerjasamaAntarInstansi.getSampai())).setParent(arg0);
			new Label(kerjasamaAntarInstansi.getFakultas() == null ? "Semua"
					: kerjasamaAntarInstansi.getFakultas().getNama()).setParent(arg0);
			new Label(kerjasamaAntarInstansi.getJurusan() == null ? "Semua"
					: kerjasamaAntarInstansi.getJurusan().getNama()).setParent(arg0);
			new Label(kerjasamaAntarInstansi.getManfaat()).setParent(arg0);
			new Label(kerjasamaAntarInstansi.getBukti()).setParent(arg0);

			new Label(kerjasamaAntarInstansi.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, kerjasamaAntarInstansi, KerjasamaAntarInstansiAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KerjasamaAntarInstansi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kerjasamaAntarInstansi = (KerjasamaAntarInstansi) obj;
		init(kerjasamaAntarInstansi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KerjasamaAntarInstansi kerjasamaAntarInstansi) {
		this.kerjasamaAntarInstansi = kerjasamaAntarInstansi;
		addWindow.setTitle(kerjasamaAntarInstansi.getId() == null ? "Tambah Kerjasama Antar Instansi" : "Ubah Kerjasama Antar Instansi");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Lembaga Mitra Kerjasama *"));
		row.appendChild(nama = new Textbox(kerjasamaAntarInstansi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kerjasama *"));
		row.appendChild(jenisKerjasama = new Combobox());
		Common.insertCombo(jenisKerjasama, "nama", JenisKerjasama.class);
		Common.selectComboItem(jenisKerjasama, kerjasamaAntarInstansi.getJenisKerjasama());
		jenisKerjasama.setWidth("90%");
		jenisKerjasama.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tingkat *"));
		row.appendChild(tingkat = new Combobox());
		tingkat.setWidth("90%");
		tingkat.setReadonly(true);

		for (String s : KerjasamaAntarInstansi.TINGKAT) {
			MyComboitemConfig comboitemConfig = new MyComboitemConfig(s);
			comboitemConfig.setValue(s);
			tingkat.appendChild(comboitemConfig);
		}
		Common.selectComboItem(tingkat, kerjasamaAntarInstansi.getTingkat());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe *"));
		row.appendChild(jenis = new Combobox());
		jenis.setWidth("90%");
		jenis.setReadonly(true);

		for (String s : KerjasamaAntarInstansi.JENIS) {
			MyComboitemConfig comboitemConfig = new MyComboitemConfig(s);
			comboitemConfig.setValue(s);
			jenis.appendChild(comboitemConfig);
		}
		Common.selectComboItem(jenis, kerjasamaAntarInstansi.getJenis());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Negara Kerjasama *"));
		row.appendChild(negara = new AmbilDataNegaraBanbox());
		negara.setAttribute("negara", kerjasamaAntarInstansi.getNegara());
		negara.setValue(
				kerjasamaAntarInstansi.getNegara() == null ? "" : kerjasamaAntarInstansi.getNegara().getNamaNegara());
		negara.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas = new Combobox());
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));
		row.appendChild(jurusan = new Combobox());
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);

		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		Common.selectComboItem(fakultas, kerjasamaAntarInstansi.getFakultas());
		Common.pilihJurusan(jurusan, kerjasamaAntarInstansi.getJurusan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Kerjasama *"));
		row.appendChild(mulai = new MyDatebox(kerjasamaAntarInstansi.getMulai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berakhir Kerjasama"));
		row.appendChild(sampai = new MyDatebox(kerjasamaAntarInstansi.getSampai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bentuk Kegiatan / Manfaat"));
		row.appendChild(manfaat = new Textbox(kerjasamaAntarInstansi.getManfaat()));
		manfaat.setWidth("90%");
		manfaat.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bukti Kerjasama"));
		row.appendChild(bukti = new Textbox(kerjasamaAntarInstansi.getBukti()));
		bukti.setWidth("90%");
		bukti.setRows(3);

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Bukti Kerjasama"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, kerjasamaAntarInstansi.getId(),
				KerjasamaAntarInstansi.class.getName(), "Lampiran Bukti Kerjasama", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows,
				"Jika file lampiran bukti kerjasama lebih dari satu file, zip dulu semua file tersebut");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kerjasamaAntarInstansi.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kerjasama",
					"Kolom Nama Kerjasama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Kerjasama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisKerjasama.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Kerjasama",
					"Kolom Jenis Kerjasama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Kerjasama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tingkat.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tingkat Kerjasama",
					"Kolom Tingkat Kerjasama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tingkat Kerjasama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenis.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tipe Kerjasama",
					"Kolom Tipe Kerjasama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tipe Kerjasama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (mulai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal Mulai Kerjasama",
					"Kolom Tanggal Mulai Kerjasama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal Mulai Kerjasama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (negara.getAttribute("negara") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Negara Kerjasama",
					"Kolom Negara Kerjasama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Negara Kerjasama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kerjasamaAntarInstansi.getId() != null) {
			kerjasamaAntarInstansi = (KerjasamaAntarInstansi) session.load(KerjasamaAntarInstansi.class,
					kerjasamaAntarInstansi.getId());

		}

		kerjasamaAntarInstansi.setNama(nama.getValue());
		kerjasamaAntarInstansi.setJenisKerjasama((JenisKerjasama) jenisKerjasama.getSelectedItem().getValue());
		kerjasamaAntarInstansi.setNegara((Negara) negara.getAttribute("negara"));
		kerjasamaAntarInstansi.setMulai(mulai.getValue());
		kerjasamaAntarInstansi.setSampai(sampai.getValue());
		kerjasamaAntarInstansi.setManfaat(manfaat.getValue());
		kerjasamaAntarInstansi.setKeterangan(keterangan.getValue());
		kerjasamaAntarInstansi.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null ? null : (jurusan.getSelectedItem().getValue())));
		kerjasamaAntarInstansi.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null ? null : fakultas.getSelectedItem().getValue()));
		kerjasamaAntarInstansi.setTingkat((String) tingkat.getSelectedItem().getValue());
		kerjasamaAntarInstansi.setBukti(bukti.getValue());
		kerjasamaAntarInstansi.setJenis((String) jenis.getSelectedItem().getValue());
		Common.refreshSaveOrUpdate(session, kerjasamaAntarInstansi);

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(kerjasamaAntarInstansi.getId());

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
		Criteria criteria = session.createCriteria(KerjasamaAntarInstansi.class);
		criteria.add(jenissemester.getSelectedItem() == null || jenissemester.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("true")
				: Restrictions.eq("jenisKerjasama", jenissemester.getSelectedItem().getValue()));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KerjasamaAntarInstansi> kerjasamaAntarInstansi = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kerjasamaAntarInstansi);
		grid.setRowRenderer(new KerjasamaAntarInstansiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
