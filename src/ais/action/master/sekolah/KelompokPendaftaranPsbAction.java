package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
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
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.KelompokPendaftaranPsb;
import ais.database.model.sekolah.PenjurusanSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk kelompok pendaftaran psb. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Combobox searchta}, {@code Combobox searchgel}, {@code Textbox
 * searchnama}, {@code Combobox searchyayasan}, {@code Combobox searchsekolah}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}, {@code ambil()}, {@code ambilClass()}); mutasi data ({@code
 * onSave()}, {@code setPersetujuan()}); pelaporan/ekspor ({@code cetakData()}); operasi domain lain ({@code
 * onAdd()}, {@code form()}, {@code istilah()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
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
public class KelompokPendaftaranPsbAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchta;
	private Combobox searchgel;

	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Textbox nama;
	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KelompokPendaftaranPsb kelompokPendaftaranPsb;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Combobox gelombangPendaftaran;
	private MyIntbox kuota;
	private Combobox penjurusanSekolah;
	private DisposisiSop disposisiSop = null;

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

		Common.generateTahunAjaranDanSemua(searchta);

		if (searchgel != null) { searchgel.setWidth("90%"); }
		Common.insertComboDanSemua(searchgel, new String[] { "nama", "tahunAjaran" }, "keterangan",
				GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (searchgel != null) { searchgel.setReadonly(true); }

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

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

		String[] contents = new String[] { "id", "gelombangPendaftaran", "nama", "kuota", "sekolah", "keterangan",
				"aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokPendaftaranPsb.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link KelompokPendaftaranPsbAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KelompokPendaftaranPsbAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KelompokPendaftaranPsbAction
	 */
	class KelompokPendaftaranPsbRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelompokPendaftaranPsb kelompokPendaftaranPsb = (KelompokPendaftaranPsb) arg1;
			new Label(kelompokPendaftaranPsb.getGelombangPendaftaran() == null ? ""
					: kelompokPendaftaranPsb.getGelombangPendaftaran().getNama()).setParent(arg0);
			new Label(kelompokPendaftaranPsb.getGelombangPendaftaran() == null ? ""
					: kelompokPendaftaranPsb.getGelombangPendaftaran().getTahunAjaran()).setParent(arg0);
			RevisiHelper.createNewRevisi(KelompokPendaftaranPsb.class, kelompokPendaftaranPsb,
					kelompokPendaftaranPsb.getNama()).setParent(arg0);
			new Label((kelompokPendaftaranPsb.getSekolah() == null ? "" : kelompokPendaftaranPsb.getSekolah().getNama())
					+ (kelompokPendaftaranPsb.getPenjurusanSekolah() == null ? ""
							: " (" + kelompokPendaftaranPsb.getPenjurusanSekolah().getNama() + ")"))
					.setParent(arg0);
			new Label(kelompokPendaftaranPsb.getKuota() + "").setParent(arg0);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			new Label(kelompokPendaftaranPsb.getKeterangan()).setParent(vbox2);

			if (kelompokPendaftaranPsb.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox2);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + kelompokPendaftaranPsb.getDisposisiSop().getKeterangan() + " ("
						+ kelompokPendaftaranPsb.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(kelompokPendaftaranPsb.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kelompokPendaftaranPsb.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokPendaftaranPsb.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kelompokPendaftaranPsb);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, kelompokPendaftaranPsb, KelompokPendaftaranPsbAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KelompokPendaftaranPsb());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kelompokPendaftaranPsb = (KelompokPendaftaranPsb) obj;
		init(kelompokPendaftaranPsb);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelompokPendaftaranPsb kelompokPendaftaranPsb) throws Exception {

		addWindow.setTitle(kelompokPendaftaranPsb.getId() == null ? "Tambah Kelompok Pendaftaran" : "Ubah Kelompok Pendaftaran");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(kelompokPendaftaranPsb, null, save, null));

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
		if (gelombangPendaftaran.getSelectedItem() == null) {
			MyMessageboxConfig.show("Gelombang pendaftaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Jenis Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Yayasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelompokPendaftaranPsb.getId() != null) {
			kelompokPendaftaranPsb = (KelompokPendaftaranPsb) session.load(KelompokPendaftaranPsb.class,
					kelompokPendaftaranPsb.getId());

		}
		kelompokPendaftaranPsb.setDisposisiSop(disposisiSop);
		kelompokPendaftaranPsb.setKuota(kuota.getValue());
		kelompokPendaftaranPsb
				.setGelombangPendaftaran((GelombangPendaftaranPsb) gelombangPendaftaran.getSelectedItem().getValue());
		kelompokPendaftaranPsb.setNama(nama.getValue());
		kelompokPendaftaranPsb.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		kelompokPendaftaranPsb.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		kelompokPendaftaranPsb.setKeterangan(keterangan.getValue());
		kelompokPendaftaranPsb
				.setPenjurusanSekolah((PenjurusanSekolah) (penjurusanSekolah.getSelectedItem() == null ? null
						: penjurusanSekolah.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, kelompokPendaftaranPsb);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelompokPendaftaranPsb.class).createAlias("gelombangPendaftaran",
				"gelombangPendaftaran");

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						|| searchta.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("gelombangPendaftaran.tahunAjaran",
										searchta.getSelectedItem().getValue()))

				.add(searchgel.getSelectedItem() == null || searchgel.getSelectedItem().getValue() == null
						|| searchgel.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("gelombangPendaftaran", searchgel.getSelectedItem().getValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelompokPendaftaranPsb> kelompokPendaftaranPsb = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokPendaftaranPsb);
		grid.setRowRenderer(new KelompokPendaftaranPsbRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop, final MyToolbarbuttonConfig save,
			EventListener setujui) throws Exception {

		this.kelompokPendaftaranPsb = (KelompokPendaftaranPsb) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran *"));
		row.appendChild(gelombangPendaftaran = new Combobox());
		gelombangPendaftaran.setWidth("90%");
		Common.insertCombo(gelombangPendaftaran, new String[] { "nama", "tahunAjaran" },
				GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(gelombangPendaftaran, kelompokPendaftaranPsb.getGelombangPendaftaran());
		gelombangPendaftaran.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok Pendaftaran *"));
		row.appendChild(nama = new Textbox(kelompokPendaftaranPsb.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kuota"));
		row.appendChild(kuota = new MyIntbox(kelompokPendaftaranPsb.getKuota()));

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, kelompokPendaftaranPsb.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, kelompokPendaftaranPsb.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penjurusan"));
		row.appendChild(penjurusanSekolah = new Combobox());
		penjurusanSekolah.setWidth("90%");
		penjurusanSekolah.setReadonly(true);

		EventListener eventListenerSekolah = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				penjurusanSekolah.getParent().setVisible(false);
				Common.clear(penjurusanSekolah);
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);

				if (s != null && s.getPenjurusanBolehDipilihSaatPsb()) {
					HibernateUtil.currentSession().refresh(s);
					Set<PenjurusanSekolah> selectedPenjurusanSekolah = s.getPenjurusanSekolahs();
					for (PenjurusanSekolah o : selectedPenjurusanSekolah) {
						if (o.getAktif() && o.getTampilkanDiPpdb()) {
							Comboitem comboitem = new Comboitem();
							comboitem.setLabel(o.getNama());
							comboitem.setDescription(o.getKeterangan());
							comboitem.setValue(o);
							penjurusanSekolah.appendChild(comboitem);
						}
					}

					if (!selectedPenjurusanSekolah.isEmpty()) {
						Comboitem comboitem = new Comboitem();
						comboitem.setLabel("Semua");
						comboitem.setDescription("Semua Penjurusan");
						comboitem.setValue(null);
						penjurusanSekolah.appendChild(comboitem);
					}

					penjurusanSekolah.getParent().setVisible(!selectedPenjurusanSekolah.isEmpty());
					Common.selectComboItem(penjurusanSekolah,
							KelompokPendaftaranPsbAction.this.kelompokPendaftaranPsb.getPenjurusanSekolah());
				}

			}
		};

		sekolah.addEventListener("onChange", eventListenerSekolah);
		Common.createDefaultTimer(eventListenerSekolah);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelompokPendaftaranPsb.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		return grid;
	}

	@Override
	public String istilah() throws Exception {
		return "Penentuan kelompok dan kuota calon siswa";
	}

	@Override
	public DataSop  ambil() throws Exception {
		// TODO Auto-generated method stub
		return kelompokPendaftaranPsb;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return KelompokPendaftaranPsb.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		// TODO Auto-generated method stub

	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
