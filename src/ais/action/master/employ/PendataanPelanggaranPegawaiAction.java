package ais.action.master.employ;

import ais.action.master.pelanggaran.DasbordPelanggaran;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.employ.LaporanPendataanPelanggaranPegawai;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.employ.HukumanPegawai;
import ais.database.model.employ.PelanggaranDanHukumanPegawai;
import ais.database.model.employ.PelanggaranPegawai;
import ais.database.model.employ.PendataanPelanggaranPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk pendataan pelanggaran pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Tabpanel
 * tabDasbor}, {@code Paging paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code
 * AmbilDataSatuanKerjaBanbox searchparent}, {@code Textbox keterangan}, {@code boolean edit};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code loadHukumanPegawai()}, {@code loadPelanggaranPegawai()},
 * {@code onSearchDefault()}, {@code ambil()}, {@code ambilClass()}); mutasi data ({@code onSave()}, {@code
 * setPersetujuan()}); pelaporan/ekspor ({@code cetak()}, {@code cetakData()}); operasi domain lain ({@code
 * onDasbor()}, {@code onAdd()}, {@code form()}, {@code istilah()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
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
public class PendataanPelanggaranPegawaiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Tabpanel tabDasbor;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private PendataanPelanggaranPegawai pendataanPelanggaranPegawai;
	private MyToolbarbuttonConfig add;
	private Set<PelanggaranPegawai> selectedPelanggaranPegawai;
	private Set<HukumanPegawai> selectedHukumanPegawai;
	private Combobox pelanggaranDanHukumanPegawai;
	private AmbilDataPegawaiBanbox pegawai;
	private MyDatebox waktu;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private boolean persetujuan = false;
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

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

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

		String[] contents = new String[] { "id", "nama", "sekolah", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PendataanPelanggaranPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		onDasbor(null);
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	@SuppressWarnings({})
	public static void cetak(PendataanPelanggaranPegawai pendataanPelanggaranPegawai) throws Exception {
		Report.generatePDFReport(Report.PDF,
				LaporanPendataanPelanggaranPegawai.generateParameter(pendataanPelanggaranPegawai),
				"employ/kartu_pelanggaran_pegawai", pendataanPelanggaranPegawai.getWaktu());
	}

	public void onDasbor(org.zkoss.zk.ui.event.Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordPelanggaran dasbord = new DasbordPelanggaran(DasbordPelanggaran.Lingkup.PEGAWAI);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Data Pelanggaran Pegawai",
				"Rekapitulasi pendataan pelanggaran yang dilakukan pegawai.");
		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PendataanPelanggaranPegawaiAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PendataanPelanggaranPegawaiAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PendataanPelanggaranPegawaiAction
	 */
	class PendataanPelanggaranPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PendataanPelanggaranPegawai pendataanPelanggaranPegawai = (PendataanPelanggaranPegawai) arg1;

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(pendataanPelanggaranPegawai.getPegawai()).setParent(hbox);
			Vbox vbox = new Vbox();
			vbox.setParent(hbox);
			vbox.appendChild(new Label(pendataanPelanggaranPegawai.getPegawai().getCode()));
			vbox.appendChild(new Label(pendataanPelanggaranPegawai.getPegawai().getNama()));
			vbox.appendChild(new Label(pendataanPelanggaranPegawai.getPegawai().getUnitKerja() == null ? ""
					: pendataanPelanggaranPegawai.getPegawai().getUnitKerja().getNama()));

			RevisiHelper.createNewRevisi(PendataanPelanggaranPegawai.class, pendataanPelanggaranPegawai,
					pendataanPelanggaranPegawai.getPelanggaranDanHukumanPegawai().getNama()).setParent(arg0);
			new Label(Common.dateFormat5.get().format(pendataanPelanggaranPegawai.getWaktu())).setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			if (pendataanPelanggaranPegawai.getDisposisiSop() != null) {
				new Label(Common.simpleString(pendataanPelanggaranPegawai.getKeterangan())).setParent(a);
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pendataanPelanggaranPegawai.getDisposisiSop().getKeterangan() + " ("
						+ pendataanPelanggaranPegawai.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pendataanPelanggaranPegawai.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			} else {
				new Label(pendataanPelanggaranPegawai.getKeterangan()).setParent(a);
			}

			vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			for (PelanggaranPegawai pelanggaranPegawai : new TreeSet<PelanggaranPegawai>(
					pendataanPelanggaranPegawai.getPelanggaranPegawais())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + pelanggaranPegawai.getNama()));
				i++;
			}

			vbox = new Vbox();
			vbox.setParent(arg0);
			i = 1;
			for (HukumanPegawai hukumanPegawai : new TreeSet<HukumanPegawai>(
					pendataanPelanggaranPegawai.getHukumanPegawais())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + hukumanPegawai.getNama()));
				i++;
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(pendataanPelanggaranPegawai.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pendataanPelanggaranPegawai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pendataanPelanggaranPegawai);
				}
			});

			Hbox aa;
			(aa = Common.copyEditDeleteButtons(edit, delete, pendataanPelanggaranPegawai,
					PendataanPelanggaranPegawaiAction.this)).setParent(arg0);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Pelanggaran Pegawai");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					cetak(pendataanPelanggaranPegawai);
				}

			});
			button.setParent(aa);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PendataanPelanggaranPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pendataanPelanggaranPegawai = (PendataanPelanggaranPegawai) obj;
		init(pendataanPelanggaranPegawai);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({})
	private void init(PendataanPelanggaranPegawai pendataanPelanggaranPegawai) throws Exception {
		this.pendataanPelanggaranPegawai = pendataanPelanggaranPegawai;
		addWindow.setTitle(pendataanPelanggaranPegawai.getId() == null ? "Tambah Pelanggaran Pegawai" : "Ubah Pelanggaran Pegawai");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(pendataanPelanggaranPegawai, disposisiSop, save, null));

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

	private void loadHukumanPegawai(MyGrid subGrid) {

		Common.clear(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("HukumanPegawai"));

		PelanggaranDanHukumanPegawai pelanggaranDanHukumanPegawai = (PelanggaranDanHukumanPegawai) (this.pelanggaranDanHukumanPegawai
				.getSelectedItem() == null ? null : this.pelanggaranDanHukumanPegawai.getSelectedItem().getValue());

		if (pelanggaranDanHukumanPegawai == null) {

			Rows subRows = new Rows();
			subRows.setParent(subGrid);

			Common.initKeteranganSatuKolom(subRows, "* Jenis pelanggaran belum dipilih. Silakan pilih jenis pelanggaran terlebih dahulu, lalu ulangi proses ini.");

			return;
		}

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		HibernateUtil.currentSession().refresh(pelanggaranDanHukumanPegawai);

		Set<HukumanPegawai> hukumanPegawais = pelanggaranDanHukumanPegawai.getHukumanPegawais();

		if (pendataanPelanggaranPegawai.getId() != null) {
			HibernateUtil.currentSession().refresh(this.pendataanPelanggaranPegawai);
		}
		selectedHukumanPegawai = this.pendataanPelanggaranPegawai.getHukumanPegawais();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final HukumanPegawai hukumanPegawai : hukumanPegawais) {

			if (persetujuan) {
				if (selectedHukumanPegawai.contains(hukumanPegawai)) {
					vboxSkala.appendChild(new Label(hukumanPegawai.getNama()));
				}
			} else {

				final Checkbox checkbox = new Checkbox(hukumanPegawai.getNama());
				checkbox.setParent(vboxSkala);
				checkbox.setChecked(selectedHukumanPegawai.contains(hukumanPegawai));
				checkbox.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (checkbox.isChecked()) {
							selectedHukumanPegawai.add(hukumanPegawai);
						} else {
							selectedHukumanPegawai.remove(hukumanPegawai);
						}
					}
				});
			}
		}

	}

	private void loadPelanggaranPegawai(MyGrid subGrid) {
		Common.clear(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Pelanggaran Pegawai"));

		PelanggaranDanHukumanPegawai pelanggaranDanHukumanPegawai = (PelanggaranDanHukumanPegawai) (this.pelanggaranDanHukumanPegawai
				.getSelectedItem() == null ? null : this.pelanggaranDanHukumanPegawai.getSelectedItem().getValue());

		if (pelanggaranDanHukumanPegawai == null) {

			Rows subRows = new Rows();
			subRows.setParent(subGrid);

			Common.initKeteranganSatuKolom(subRows, "* Jenis Pelanggaran Pegawai belum dipilih. Silakan pilih jenis pelanggaran terlebih dahulu, lalu ulangi proses ini.");

			return;
		}

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		HibernateUtil.currentSession().refresh(pelanggaranDanHukumanPegawai);

		Set<PelanggaranPegawai> pelanggaranPegawais = pelanggaranDanHukumanPegawai.getPelanggaranPegawais();

		if (pendataanPelanggaranPegawai.getId() != null) {
			HibernateUtil.currentSession().refresh(this.pendataanPelanggaranPegawai);
		}
		selectedPelanggaranPegawai = this.pendataanPelanggaranPegawai.getPelanggaranPegawais();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final PelanggaranPegawai pelanggaranPegawai : pelanggaranPegawais) {

			if (persetujuan) {
				if (selectedPelanggaranPegawai.contains(pelanggaranPegawai)) {
					vboxSkala.appendChild(new Label(pelanggaranPegawai.getNama()));
				}
			} else {

				final Checkbox checkbox = new Checkbox(pelanggaranPegawai.getNama());
				checkbox.setParent(vboxSkala);
				checkbox.setChecked(selectedPelanggaranPegawai.contains(pelanggaranPegawai));
				checkbox.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (checkbox.isChecked()) {
							selectedPelanggaranPegawai.add(pelanggaranPegawai);
						} else {
							selectedPelanggaranPegawai.remove(pelanggaranPegawai);
						}
					}
				});
			}
		}

	}

	public boolean onSave(Event event) throws Exception {
		if (pegawai.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) cari dan pilih Pegawai menggunakan kolom pencarian; (2) pastikan data pegawai sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (pelanggaranDanHukumanPegawai.getSelectedItem() == null
				|| pelanggaranDanHukumanPegawai.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Pelanggaran Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) pilih Jenis Pelanggaran dari dropdown pada form; (2) pastikan data jenis pelanggaran sudah tersedia; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pendataanPelanggaranPegawai.getId() != null) {
			pendataanPelanggaranPegawai = (PendataanPelanggaranPegawai) session.load(PendataanPelanggaranPegawai.class,
					pendataanPelanggaranPegawai.getId());

		}

		pendataanPelanggaranPegawai.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		pendataanPelanggaranPegawai.setPelanggaranDanHukumanPegawai(
				(PelanggaranDanHukumanPegawai) pelanggaranDanHukumanPegawai.getSelectedItem().getValue());
		pendataanPelanggaranPegawai.setKeterangan(keterangan.getValue());
		pendataanPelanggaranPegawai.setPelanggaranPegawais(selectedPelanggaranPegawai);
		pendataanPelanggaranPegawai.setHukumanPegawais(selectedHukumanPegawai);

		if (disposisiSop != null && disposisiSop.getId() != null) {
			pendataanPelanggaranPegawai.setDisposisiSop(disposisiSop);
		}

		Common.refreshSaveOrUpdate(session, pendataanPelanggaranPegawai);

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
		Criteria criteria = session.createCriteria(PendataanPelanggaranPegawai.class).createAlias("pegawai", "pegawai")
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("pegawai.satuanKerja", satuanKerjas));

		if (order)
			criteria.addOrder(Order.desc("waktu"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("pegawai.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PendataanPelanggaranPegawai> pendataanPelanggaranPegawai = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pendataanPelanggaranPegawai);
		grid.setRowRenderer(new PendataanPelanggaranPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop, MyToolbarbuttonConfig save,
			EventListener setujui) throws Exception {

		this.pendataanPelanggaranPegawai = (PendataanPelanggaranPegawai) generalValueObject;
		this.disposisiSop = disposisiSop;
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		pegawai = new AmbilDataPegawaiBanbox();

		if (persetujuan) {
			row.appendChild(new Label(pendataanPelanggaranPegawai.getPegawai() == null ? ""
					: pendataanPelanggaranPegawai.getPegawai().getNama()));
		} else {
			row.appendChild(pegawai);
		}

		pegawai.setAttribute("pegawai", pendataanPelanggaranPegawai.getPegawai());
		pegawai.setValue(pendataanPelanggaranPegawai.getPegawai() == null ? ""
				: pendataanPelanggaranPegawai.getPegawai().getNama());
		pegawai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Pelanggaran *"));
		waktu = new MyDatebox(pendataanPelanggaranPegawai.getWaktu());

		if (persetujuan) {
			row.appendChild(new Label(pendataanPelanggaranPegawai.getWaktu() == null ? ""
					: Common.dateFormat3.get().format(pendataanPelanggaranPegawai.getWaktu())));
		} else {
			row.appendChild(waktu);
		}

		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setWidth("90%");
		waktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		keterangan = new Textbox(pendataanPelanggaranPegawai.getKeterangan());

		if (persetujuan) {
			row.appendChild(new Label(pendataanPelanggaranPegawai.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pelanggaran *"));
		pelanggaranDanHukumanPegawai = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(pendataanPelanggaranPegawai.getPelanggaranDanHukumanPegawai() == null ? ""
					: pendataanPelanggaranPegawai.getPelanggaranDanHukumanPegawai().getNama()));
		} else {
			row.appendChild(pelanggaranDanHukumanPegawai);
		}

		Common.insertCombo(pelanggaranDanHukumanPegawai, "nama", PelanggaranDanHukumanPegawai.class);
		Common.selectComboItem(pelanggaranDanHukumanPegawai,
				pendataanPelanggaranPegawai.getPelanggaranDanHukumanPegawai());
		pelanggaranDanHukumanPegawai.setWidth("90%");
		pelanggaranDanHukumanPegawai.setReadonly(true);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		final MyGrid subGridH = new MyGrid();
		row.appendChild(subGridH);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadPelanggaranPegawai(subGrid);
				loadHukumanPegawai(subGridH);
			}
		};

		pelanggaranDanHukumanPegawai.addEventListener("onChange", eventListener);

		eventListener.onEvent(null);
		return grid;
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Kedisiplinan Pegawai";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return pendataanPelanggaranPegawai;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return PendataanPelanggaranPegawai.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
