package ais.action.master.kursus;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.kursus.helper.AmbilDataPesertaKursusBanbox;
import ais.action.master.kursus.helper.AmbilDataProdukKursusBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.UangMuka;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.kursus.PesertaPunyaProdukKursus;
import ais.database.model.kursus.ProdukKursus;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk peserta punya produk kursus. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkode}, {@code Textbox
 * searchproduk}, {@code Combobox searchtsatus}, {@code Textbox keterangan}; inisialisasi/lifecycle ({@code
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
public class PesertaPunyaProdukKursusAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Textbox searchproduk;
	private Combobox searchtsatus;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private PesertaPunyaProdukKursus pesertaPunyaProdukKursus;
	private MyToolbarbuttonConfig add;
	private Label kode;
	private AmbilDataPesertaKursusBanbox pesertaKursus;
	private AmbilDataProdukKursusBanbox produkKursus;
	private Radiogroup status;
	private MyDatebox waktuBeli;

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

		Comboitem comboitem = new Comboitem(PesertaPunyaProdukKursus.PESAN);
		if (comboitem != null) { comboitem.setValue(PesertaPunyaProdukKursus.PESAN); }
		searchtsatus.appendChild(comboitem);

		comboitem = new Comboitem(PesertaPunyaProdukKursus.TERBELI);
		if (comboitem != null) { comboitem.setValue(PesertaPunyaProdukKursus.TERBELI); }
		searchtsatus.appendChild(comboitem);

		comboitem = new Comboitem(PesertaPunyaProdukKursus.BATAL);
		if (comboitem != null) { comboitem.setValue(PesertaPunyaProdukKursus.BATAL); }
		searchtsatus.appendChild(comboitem);

		comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchtsatus.appendChild(comboitem);

		if (searchtsatus != null) { searchtsatus.setReadonly(true); }
		if (searchtsatus != null) { searchtsatus.setSelectedItem(comboitem); }

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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "produkKursus", "pesertaKursus",
				"waktuBeli", "status" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PesertaPunyaProdukKursus.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PesertaPunyaProdukKursus.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PesertaPunyaProdukKursusAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PesertaPunyaProdukKursusAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PesertaPunyaProdukKursusAction
	 */
	class PesertaPunyaProdukKursusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			PesertaPunyaProdukKursus pesertaPunyaProdukKursus = (PesertaPunyaProdukKursus) arg1;
			new Label(pesertaPunyaProdukKursus.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(PesertaPunyaProdukKursus.class, pesertaPunyaProdukKursus,
					pesertaPunyaProdukKursus.getNama()).setParent(arg0);

			new Label(pesertaPunyaProdukKursus.getWaktuBeli() == null ? ""
					: Common.dateFormat5.get().format(pesertaPunyaProdukKursus.getWaktuBeli())).setParent(arg0);

			new Label(pesertaPunyaProdukKursus.getStatus()).setParent(arg0);

			new Label(pesertaPunyaProdukKursus.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, pesertaPunyaProdukKursus, PesertaPunyaProdukKursusAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PesertaPunyaProdukKursus());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pesertaPunyaProdukKursus = (PesertaPunyaProdukKursus) obj;
		init(pesertaPunyaProdukKursus);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PesertaPunyaProdukKursus pesertaPunyaProdukKursus) {
		this.pesertaPunyaProdukKursus = pesertaPunyaProdukKursus;
		addWindow.setTitle(pesertaPunyaProdukKursus.getId() == null ? "Tambah Pesanan Peserta" : "Ubah Pesanan Peserta");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pemesanan"));
		row.appendChild(kode = new Label(pesertaPunyaProdukKursus.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peserta *"));
		row.appendChild(pesertaKursus = new AmbilDataPesertaKursusBanbox());
		pesertaKursus.setAttribute("pesertaKursus", pesertaPunyaProdukKursus.getPesertaKursus());
		pesertaKursus.setAttribute("myValue", pesertaPunyaProdukKursus.getPesertaKursus());
		pesertaKursus.setValue(pesertaPunyaProdukKursus.getPesertaKursus() == null ? ""
				: pesertaPunyaProdukKursus.getPesertaKursus().getNama());
		pesertaKursus.setWidth("90%");
		pesertaKursus.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Produk *"));
		row.appendChild(produkKursus = new AmbilDataProdukKursusBanbox());
		produkKursus.setAttribute("produkKursus", pesertaPunyaProdukKursus.getProdukKursus());
		produkKursus.setAttribute("myValue", pesertaPunyaProdukKursus.getProdukKursus());
		produkKursus.setValue(pesertaPunyaProdukKursus.getProdukKursus() == null ? ""
				: pesertaPunyaProdukKursus.getProdukKursus().getNama());
		produkKursus.setWidth("90%");
		produkKursus.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Pesan"));
		waktuBeli = new MyDatebox(pesertaPunyaProdukKursus.getWaktuBeli());
		waktuBeli.setReadonly(true);
		waktuBeli.setFormat(Common.dateFormat3.get().toPattern());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status *"));

		status = new Radiogroup();
		row.appendChild(status);

		Radio radio = new Radio(PesertaPunyaProdukKursus.PESAN);
		radio.setAttribute("value", PesertaPunyaProdukKursus.PESAN);
		status.appendChild(radio);

		radio = new Radio(PesertaPunyaProdukKursus.TERBELI);
		radio.setAttribute("value", PesertaPunyaProdukKursus.TERBELI);
		status.appendChild(radio);

		radio = new Radio(PesertaPunyaProdukKursus.BATAL);
		radio.setAttribute("value", PesertaPunyaProdukKursus.BATAL);
		status.appendChild(radio);

		Common.selectRadioItem(status, pesertaPunyaProdukKursus.getStatus());
		
		
		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("checkbox");
					if (selesai != null && selesai) {
						Common.selectRadioItem(status, UangMuka.DISETUJU);
						Common.freeze(status, true);
					} else {
						status.setSelectedItem(null);
						Common.freeze(status, false);
					}
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pesertaPunyaProdukKursus.getKeterangan()));
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
		if (pesertaKursus.getAttribute("pesertaKursus") == null) {
			MyMessageboxConfig.show("Peserta harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (produkKursus.getAttribute("produkKursus") == null) {
			MyMessageboxConfig.show("Produk harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pesertaPunyaProdukKursus.getId() != null) {
			pesertaPunyaProdukKursus = (PesertaPunyaProdukKursus) session.load(PesertaPunyaProdukKursus.class,
					pesertaPunyaProdukKursus.getId());

		}

		PesertaKursus pesertaKursus = (PesertaKursus) this.pesertaKursus.getAttribute("pesertaKursus");
		ProdukKursus produkKursus = (ProdukKursus) this.produkKursus.getAttribute("produkKursus");

		pesertaPunyaProdukKursus.setKode(kode.getValue());
		pesertaPunyaProdukKursus.setNama(pesertaKursus.getNama() + " " + produkKursus.getNama());
		pesertaPunyaProdukKursus.setPesertaKursus(pesertaKursus);
		pesertaPunyaProdukKursus.setProdukKursus(produkKursus);
		pesertaPunyaProdukKursus.setStatus((String) status.getAttribute("value"));
		pesertaPunyaProdukKursus.setWaktuBeli(waktuBeli.getValue());
		pesertaPunyaProdukKursus.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, pesertaPunyaProdukKursus);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PesertaPunyaProdukKursus.class)
				.createAlias("pesertaKursus", "pesertaKursus").createAlias("produkKursus", "produkKursus");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("pesertaKursus.kode", searchnama.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("pesertaKursus.nama", searchnama.getValue().trim(),
										MatchMode.ANYWHERE))

				)

				.add(searchproduk.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("produkKursus.kode", searchproduk.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("produkKursus.nama", searchproduk.getValue().trim(),
										MatchMode.ANYWHERE)))

				.add(searchtsatus.getSelectedItem() == null || searchtsatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("status", searchtsatus.getSelectedItem().getValue()))

		;
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PesertaPunyaProdukKursus> pesertaPunyaProdukKursus = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pesertaPunyaProdukKursus);
		grid.setRowRenderer(new PesertaPunyaProdukKursusRenderer());
		grid.setModelCheckMobile(strset);

	}

}
