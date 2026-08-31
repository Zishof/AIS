package ais.action.master;

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
import org.zkoss.zul.Columns;
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

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.SatuanKerjaPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk satuan kerja pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code AmbilDataSatuanKerjaBanbox searchparent},
 * {@code Checkbox searchaktif}, {@code Textbox keterangan}, {@code boolean edit}; inisialisasi/lifecycle ({@code
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
public class SatuanKerjaPegawaiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private Checkbox searchaktif;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private SatuanKerjaPegawai satuanKerjaPegawai;
	private MyToolbarbuttonConfig add;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataPegawaiBanbox pegawai;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

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

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

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

		String[] contents = new String[] { "id", "pegawai", "satuanKerja", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(SatuanKerjaPegawai.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, SatuanKerjaPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class SatuanKerjaPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final SatuanKerjaPegawai satuanKerjaPegawai = (SatuanKerjaPegawai) arg1;
			Pegawai pegawai = satuanKerjaPegawai.getPegawai();

			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelKecil(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(vbox);
			new MyLabelKecil(pegawai.getCode() == null ? "" : pegawai.getCode()).setParent(vbox);
			new MyLabelKecil(pegawai.getNama() == null ? "" : pegawai.getNama()).setParent(vbox);

			RevisiHelper.createNewRevisi(SatuanKerjaPegawai.class, satuanKerjaPegawai,
					satuanKerjaPegawai.getSatuanKerja().getNama()).setParent(arg0);
			new Label(satuanKerjaPegawai.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(satuanKerjaPegawai.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					satuanKerjaPegawai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(satuanKerjaPegawai);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, satuanKerjaPegawai, SatuanKerjaPegawaiAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new SatuanKerjaPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		satuanKerjaPegawai = (SatuanKerjaPegawai) obj;
		init(satuanKerjaPegawai);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(SatuanKerjaPegawai satuanKerjaPegawai) throws Exception {
		this.satuanKerjaPegawai = satuanKerjaPegawai;
		addWindow.setTitle(satuanKerjaPegawai.getId() == null ? "Tambah Satuan Kerja Pegawai" : "Ubah Satuan Kerja Pegawai");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(
				satuanKerjaPegawai.getSatuanKerja() == null ? "" : satuanKerjaPegawai.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", satuanKerjaPegawai.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");
		satuanKerja.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox());
		pegawai.setAttribute("pegawai", satuanKerjaPegawai.getPegawai());
		pegawai.setAttribute("myValue", satuanKerjaPegawai.getPegawai());
		pegawai.setValue(satuanKerjaPegawai.getPegawai() == null ? "" : satuanKerjaPegawai.getPegawai().getNama());
		pegawai.setWidth("90%");
		pegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(satuanKerjaPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(5);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data satuan kerja",
					"Kolom Nama satuan kerja belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama satuan kerja.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (pegawai.getAttribute("pegawai") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data pegawai",
					"Kolom Nama pegawai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama pegawai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (satuanKerjaPegawai.getId() != null) {
			satuanKerjaPegawai = (SatuanKerjaPegawai) session.load(SatuanKerjaPegawai.class,
					satuanKerjaPegawai.getId());

		}

		satuanKerjaPegawai.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		satuanKerjaPegawai.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		satuanKerjaPegawai.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, satuanKerjaPegawai);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(SatuanKerjaPegawai.class)

				.createAlias("pegawai", "pegawai")
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas)))
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("pegawai.nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("pegawai.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<SatuanKerjaPegawai> satuanKerjaPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(satuanKerjaPegawai);
		grid.setRowRenderer(new SatuanKerjaPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
