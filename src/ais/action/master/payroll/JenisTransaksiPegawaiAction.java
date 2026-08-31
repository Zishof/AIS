package ais.action.master.payroll;

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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.payroll.JenisTransaksiPegawai;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk jenis transaksi pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Window addWindow}, {@code MyGrid grid},
 * {@code Paging paging}, {@code MyTextbox searchnama}, {@code org.zkoss.zul.Textbox searchkode}, {@code
 * MyTextbox kode}, {@code MyTextbox nama}, {@code AmbilDataAkunBanbox akun}; inisialisasi/lifecycle ({@code
 * doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); validasi/perhitungan ({@code checkNamaJenisTransaksiPegawai()}, {@code
 * checkKodeJenisTransaksiPegawai()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class JenisTransaksiPegawaiAction extends GenericAutowireComposer
		implements DataInitDefault, DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private MyGrid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private org.zkoss.zul.Textbox searchkode;

	private MyTextbox kode;
	private MyTextbox nama;
	private AmbilDataAkunBanbox akun;
	private AmbilDataAkunBanbox akunDebet;
	private MyTextbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisTransaksiPegawai jenisTransaksiPegawai;
	private MyToolbarbuttonConfig add;
	private Radiogroup jenisTransaksi;
	private MyTextbox formula;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "formula", "akun", "akunDebet",
				"aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisTransaksiPegawai.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisTransaksiPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class JenisTransaksiPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisTransaksiPegawai jenisTransaksiPegawai = (JenisTransaksiPegawai) arg1;

			new Label(jenisTransaksiPegawai.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisTransaksiPegawai.class, jenisTransaksiPegawai,
					jenisTransaksiPegawai.getNama()).setParent(arg0);

			new Label(jenisTransaksiPegawai.getFormula()).setParent(arg0);

			new Label(jenisTransaksiPegawai.getJenisTransaksi().equals(1) ? "Debet" : "Kredit").setParent(arg0);

			new Label(jenisTransaksiPegawai.getAkun() == null ? "" : jenisTransaksiPegawai.getAkun().getNama())
					.setParent(arg0);

			new Label(
					jenisTransaksiPegawai.getAkunDebet() == null ? "" : jenisTransaksiPegawai.getAkunDebet().getNama())
					.setParent(arg0);
			new Label(jenisTransaksiPegawai.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisTransaksiPegawai.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisTransaksiPegawai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisTransaksiPegawai);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisTransaksiPegawai, JenisTransaksiPegawaiAction.this)
					.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisTransaksiPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		init((JenisTransaksiPegawai) obj);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisTransaksiPegawai jenisTransaksiPegawai) {
		this.jenisTransaksiPegawai = jenisTransaksiPegawai;
		addWindow.setTitle(jenisTransaksiPegawai.getId() == null ? "Tambah Jenis Transaksi Pegawai" : "Ubah Jenis Transaksi Pegawai");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Jenis Transaksi *")));
		row.appendChild(kode = new MyTextbox(jenisTransaksiPegawai.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Jenis Transaksi *")));
		row.appendChild(
				nama = new MyTextbox(jenisTransaksiPegawai.getNama() == null ? "" : jenisTransaksiPegawai.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Formula")));
		row.appendChild(formula = new MyTextbox(jenisTransaksiPegawai.getFormula()));
		formula.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tipe Transaksi *")));
		row.appendChild(jenisTransaksi = new Radiogroup());
		Radio radio = new Radio();
		radio.setLabel("Debet");
		radio.setAttribute("value", 1);
		jenisTransaksi.appendChild(radio);

		radio = new Radio();
		radio.setLabel("Kredit");
		radio.setAttribute("value", -1);
		jenisTransaksi.appendChild(radio);

		Common.selectRadioItem(jenisTransaksi, jenisTransaksiPegawai.getJenisTransaksi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Akun Kredit *")));
		row.appendChild(akun = new AmbilDataAkunBanbox());
		akun.setValue(jenisTransaksiPegawai.getAkun() == null ? "" : jenisTransaksiPegawai.getAkun().getNama());
		akun.setAttribute("akun", jenisTransaksiPegawai.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Akun Debet  *")));
		row.appendChild(akunDebet = new AmbilDataAkunBanbox());
		akunDebet.setValue(
				jenisTransaksiPegawai.getAkunDebet() == null ? "" : jenisTransaksiPegawai.getAkunDebet().getNama());
		akunDebet.setAttribute("akun", jenisTransaksiPegawai.getAkunDebet());
		akunDebet.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				jenisTransaksiPegawai.getKeterangan() == null ? "" : jenisTransaksiPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Kode Jenis Transaksi Pegawai wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Kode pada kolom yang tersedia; (2) pastikan Kode tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Nama Jenis Transaksi Pegawai wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama pada kolom yang tersedia; (2) pastikan Nama tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (akun.getAttribute("akun") == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Akun kredit wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Akun kredit yang sesuai pada pilihan yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (akunDebet.getAttribute("akun") == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Akun debet wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Akun debet yang sesuai pada pilihan yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		/*
		 * if (keterangan.getValue().trim().equals("")) {
		 * Messagebox.show("Keterangan harus diisi", "Peringatan", Messagebox.OK,
		 * Messagebox.EXCLAMATION); return false; }
		 */

		boolean i = checkKodeJenisTransaksiPegawai();
		if (i) {
			MyMessageboxConfig.show(
					"Mohon maaf, Kode Jenis Transaksi Pegawai yang Bapak/Ibu masukkan sudah terdaftar di dalam basis data. Langkah yang dapat dilakukan: (1) gunakan Kode yang berbeda; (2) periksa kembali daftar Jenis Transaksi Pegawai yang telah ada; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		i = checkNamaJenisTransaksiPegawai();
		if (i) {
			MyMessageboxConfig.show(
					"Mohon maaf, Nama Jenis Transaksi Pegawai yang Bapak/Ibu masukkan sudah terdaftar di dalam basis data. Langkah yang dapat dilakukan: (1) gunakan Nama yang berbeda; (2) periksa kembali daftar Jenis Transaksi Pegawai yang telah ada; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisTransaksiPegawai.getId() != null) {
			jenisTransaksiPegawai = (JenisTransaksiPegawai) session.load(JenisTransaksiPegawai.class,
					jenisTransaksiPegawai.getId());
		}
		jenisTransaksiPegawai.setJenisTransaksi((Integer) (jenisTransaksi.getSelectedItem() == null ? null
				: jenisTransaksi.getSelectedItem().getAttribute("value")));
		jenisTransaksiPegawai.setAkunDebet((Akun) akunDebet.getAttribute("akun"));
		jenisTransaksiPegawai.setAkun((Akun) akun.getAttribute("akun"));
		jenisTransaksiPegawai.setKode(kode.getValue().trim());
		jenisTransaksiPegawai.setNama(nama.getValue().trim());
		jenisTransaksiPegawai.setFormula(formula.getValue());
		jenisTransaksiPegawai.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisTransaksiPegawai);
		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<JenisTransaksiPegawai> jenisTransaksiPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisTransaksiPegawai);
		grid.setRowRenderer(new JenisTransaksiPegawaiRenderer());
		grid.setModelCheckMobile(strset);

		grid.renderAll();

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisTransaksiPegawai.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	public Boolean checkNamaJenisTransaksiPegawai() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisTransaksiPegawai.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.jenisTransaksiPegawai.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisTransaksiPegawai.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkKodeJenisTransaksiPegawai() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisTransaksiPegawai.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.jenisTransaksiPegawai.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisTransaksiPegawai.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
