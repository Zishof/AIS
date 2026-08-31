package ais.action.master.sirkulasisurat;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
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
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.master.sirkulasisurat.helper.AmbilDataPeminjamSuratBanbox;
import ais.action.master.sirkulasisurat.helper.KembaliSuratItemDetailAction;
import ais.action.master.sirkulasisurat.helper.KembaliSuratItemPunyaItemHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.servlet.Wa;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sirkulasisurat.KembaliSuratItem;
import ais.database.model.sirkulasisurat.KembaliSuratItemDetail;
import ais.database.model.sirkulasisurat.PeminjamSurat;
import ais.database.model.sirkulasisurat.PeminjamanSuratItem;
import ais.database.model.sirkulasisurat.PeminjamanSuratItemDetail;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.SuratMasuk;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.DataCriteria;
import ais.ui.util.FormSop;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk kembali surat item. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code MyGrid
 * grid}, {@code Paging paging}, {@code Textbox searchkode}, {@code Textbox searchbarkode}, {@code Textbox
 * searchjudul}, {@code AmbilDataPeminjamSuratBanbox searchpeminjamSurat}, {@code AmbilDataSatuanKerjaBanbox
 * searchparent}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}, {@code ambil()}, {@code
 * ambilClass()}); mutasi data ({@code setujui()}, {@code onSave()}, {@code setPersetujuan()}); pelaporan/ekspor
 * ({@code cetakData()}); operasi domain lain ({@code createriaDetail()}, {@code onAdd()}, {@code form()}, {@code
 * kirim()}, {@code onKodePeminjamSurat()}, {@code istilah()}). Bagian lain dari kontrak tetap mengikuti kelas
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
public class KembaliSuratItemAction extends GenericAutowireComposer implements FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchkode;
	private Textbox searchbarkode;
	private Textbox searchjudul;
	private AmbilDataPeminjamSuratBanbox searchpeminjamSurat;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private Textbox searchkodeangota;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;

	private MyTextbox kode;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;
	// private AmbilDataPeminjamanSuratItemBanbox peminjamanSuratItem;

	private boolean edit = false;
	private boolean delete = false;

	private KembaliSuratItem kembaliSuratItem;
	private MyToolbarbuttonConfig add;
	private MyGrid gridItem;
	private KembaliSuratItemPunyaItemHelper kembaliSuratItemPunyaItemHelper;

	private PeminjamanSuratItem peminjamanSuratItem;

	private String barcodeItem = null;

	private String tipe = "surat";
	private DisposisiSop disposisiSop;
	private boolean persetujuan = false;
	private AmbilDataPeminjamSuratBanbox peminjamSurat;
	private Label labelKet;
	private Label lablTgl;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	public KembaliSuratItemAction() {
		super();
	}

	public KembaliSuratItemAction(String tipe) {
		super();
		this.tipe = tipe;
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
		searchpeminjamSurat.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchkodeangota.addEventListener("onFocus", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				searchkodeangota.select();
			}
		});

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		if (searchmulai != null) { searchmulai.setReadonly(true); }
		if (searchsampai != null) { searchsampai.setReadonly(true); }

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 7);
		if (searchmulai != null) { searchmulai.setValue(calendar.getTime()); }
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (searchsampai != null) { searchsampai.setValue(calendar.getTime()); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "kembaliSuratItem", "suratMasuk", "tanggal", "denda",
				"dibayarSejumlah", "telahDibayar", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(KembaliSuratItemDetail.class,
				new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {
						return KembaliSuratItemAction.this.createriaDetail(order);
					}
				}, "Download Data", "/img/print.png", contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

	        FilterLanjutHelper.setup(comp);
}

	private Criteria createriaDetail(Boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KembaliSuratItemDetail.class).createCriteria("kembaliSuratItem");

		criteria.createAlias("peminjamanSuratItem", "peminjamanSuratItem")

				.add(searchpeminjamSurat.getAttribute("peminjamSurat") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("peminjamanSuratItem.peminjamSurat",
								searchpeminjamSurat.getAttribute("peminjamSurat")))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchmulai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tanggalPembuatan", searchmulai.getValue()))
				.add(searchsampai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.le("tanggalPembuatan", searchsampai.getValue()));

		if (order)
			criteria.addOrder(Order.desc("tanggalPembuatan"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	private void setujui(final KembaliSuratItem kembaliSuratItem) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();
				session.refresh(kembaliSuratItem);

				kembaliSuratItem.setDisetujuiOleh(Common.getCurrentUser());
				kembaliSuratItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

				Common.refreshUpdate(session, kembaliSuratItem);

				List<PeminjamanSuratItem> peminjamanSuratItems = session.createCriteria(KembaliSuratItemDetail.class)
						.createAlias("peminjamanSuratItemDetail", "peminjamanSuratItemDetail")
						.setProjection(Projections.groupProperty("peminjamanSuratItemDetail.peminjamanSuratItem"))
						.add(Restrictions.eq("kembaliSuratItem", kembaliSuratItem)).list();
				for (PeminjamanSuratItem peminjamanSuratItem : peminjamanSuratItems) {
					peminjamanSuratItem.setKembaliSuratItem(kembaliSuratItem);
					Common.refreshUpdate(session, peminjamanSuratItem);
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});
			}
		});

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KembaliSuratItemAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KembaliSuratItemAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KembaliSuratItemAction
	 */
	class KembaliSuratItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KembaliSuratItem kembaliSuratItem = (KembaliSuratItem) arg1;
			PeminjamanSuratItem peminjamanSuratItem = kembaliSuratItem.getPeminjamanSuratItem();

			(new KembaliSuratItemDetailAction(kembaliSuratItem)).setParent(arg0);

			RevisiHelper.createNewRevisi(KembaliSuratItem.class, kembaliSuratItem, kembaliSuratItem.getKode())
					.setParent(arg0);

			new Label(kembaliSuratItem.getPeminjamanSuratItem().getPeminjamSurat() == null ? ""
					: kembaliSuratItem.getPeminjamanSuratItem().getPeminjamSurat().getNama()).setParent(arg0);

			final Html htmldenda = new ais.ui.util.MyHtml();
			htmldenda.setParent(arg0);

			new Label(kembaliSuratItem.getDibuatOleh() == null ? "" : kembaliSuratItem.getDibuatOleh().getUserNama())
					.setParent(arg0);
			new Label(kembaliSuratItem.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(kembaliSuratItem.getTanggalPembuatan())).setParent(arg0);

			(new Label(kembaliSuratItem.getDisetujuiOleh() == null ? ""
					: kembaliSuratItem.getDisetujuiOleh().getUserNama())).setParent(arg0);

			(new Label(kembaliSuratItem.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(kembaliSuratItem.getTanggalPersetujuan()))).setParent(arg0);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(kembaliSuratItem.getKeterangan())).setParent(vbox1);
			if (kembaliSuratItem.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + kembaliSuratItem.getDisposisiSop().getKeterangan() + " ("
						+ kembaliSuratItem.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(kembaliSuratItem.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Kembali Item");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					Report.generatePDFReport(Report.PDF, parameter(kembaliSuratItem), "pengembalian_arsip",
							kembaliSuratItem.getTanggalPembuatan());
				}

			});
			button.setParent(toolbar);

			// final MyToolbarbuttonConfig disetujui = new
			// MyToolbarbuttonConfig("",
			// "/img/svg/check2.svg");
			//
			// final MyToolbarbuttonConfig dibatalkan = new
			// MyToolbarbuttonConfig("",
			// "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kembaliSuratItem);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											List<KembaliSuratItemDetail> kembaliSuratItemDetails = session
													.createCriteria(KembaliSuratItemDetail.class)
													.add(Restrictions.eq("kembaliSuratItem", kembaliSuratItem)).list();
											for (KembaliSuratItemDetail kembaliSuratItemDetail : kembaliSuratItemDetails) {
												Common.refreshDelete(session, kembaliSuratItemDetail);
											}

											session.createSQLQuery("delete from surat.kembali_surat_item where id="
													+ kembaliSuratItem.getId()).executeUpdate();

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
			hapus.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);

			PeminjamanSuratItem p = peminjamanSuratItem;
			String content = LibraryUtil.tampilanSummaryPeminjaman(kembaliSuratItem, p);
			htmldenda.setContent(content);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new KembaliSuratItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "deprecation" })
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		this.kembaliSuratItem = (KembaliSuratItem) generalValueObject;
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pengembalian"));
		String mykode = kembaliSuratItem.getKode();

		row.appendChild(kode = new MyTextbox(kembaliSuratItem.getKode() == null ? mykode : kembaliSuratItem.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		if (mykode == null || mykode.trim().isEmpty()) {
			mykode = LibraryUtil.generateCode(KembaliSuratItem.class, 8, "KMB");
			kode.setValue(mykode);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengembalian"));

		tanggalPembuatan = new MyDatebox(
				kembaliSuratItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: kembaliSuratItem.getTanggalPembuatan());
		lablTgl = new Label(Common.dateFormat3.get()
				.format(kembaliSuratItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: kembaliSuratItem.getTanggalPembuatan()));
		if (persetujuan) {
			row.appendChild(lablTgl);
		} else {
			row.appendChild(tanggalPembuatan);
		}

		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());

		tanggalPembuatan.setWidth("90%");
		tanggalPembuatan.setReadonly(true);

		final MyToolbarbuttonConfig perpanjang = new MyToolbarbuttonConfig("Perpanjang Peminjaman", "/img/corner.gif");
		perpanjang.setVisible(false);

		final MyToolbarbuttonConfig batalPerpanjang = new MyToolbarbuttonConfig("Batal Perpanjang",
				"/img/svg/warning-outline.svg");

		batalPerpanjang.setVisible(false);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (kembaliSuratItem.getPeminjamanSuratItem() == null && tbmuser != null && tbmuser.getPegawai() != null) {

			peminjamanSuratItem = (PeminjamanSuratItem) HibernateUtil.currentSession()
					.createCriteria(PeminjamanSuratItemDetail.class)
					.setProjection(Projections.property("peminjamanSuratItem"))
					.createAlias("peminjamanSuratItem", "peminjamanSuratItem")
					.createAlias("peminjamanSuratItem.peminjamSurat", "peminjamSurat")
					.add(Restrictions.eq("peminjamSurat.pegawai", tbmuser.getPegawai())).addOrder(Order.desc("id"))
					.setMaxResults(1).uniqueResult();

			if (peminjamanSuratItem != null && peminjamanSuratItem.getId() != null) {
				KembaliSuratItemAction.this.kembaliSuratItem.setPeminjamanSuratItem(peminjamanSuratItem);

			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Data Peminjaman"));

		peminjamSurat = new AmbilDataPeminjamSuratBanbox();
		peminjamSurat.setReadonly(true);
		if (persetujuan) {
			row.appendChild(new Label(kembaliSuratItem.getPeminjamanSuratItem() == null
					|| kembaliSuratItem.getPeminjamanSuratItem().getPeminjamSurat() == null ? ""
							: kembaliSuratItem.getPeminjamanSuratItem().getPeminjamSurat().getNama()));
		} else {
			row.appendChild(peminjamSurat);
		}

		peminjamSurat.setAttribute("peminjamSurat", kembaliSuratItem.getPeminjamanSuratItem() == null ? null
				: kembaliSuratItem.getPeminjamanSuratItem().getPeminjamSurat());
		peminjamSurat.setValue(kembaliSuratItem.getPeminjamanSuratItem() == null
				|| kembaliSuratItem.getPeminjamanSuratItem().getPeminjamSurat() == null ? ""
						: kembaliSuratItem.getPeminjamanSuratItem().getPeminjamSurat().toString());
		peminjamSurat.setWidth("90%");

		if (kembaliSuratItem.getPeminjamanSuratItem() != null
				&& kembaliSuratItem.getPeminjamanSuratItem().getPeminjamSurat() != null) {
			peminjamSurat.setDisabled(true);
		}

		keterangan = new MyTextbox(kembaliSuratItem.getKeterangan() == null ? "" : kembaliSuratItem.getKeterangan());
		labelKet = new Label(kembaliSuratItem.getKeterangan());

		peminjamSurat.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();
				PeminjamSurat peminjamSurat = (PeminjamSurat) KembaliSuratItemAction.this.peminjamSurat
						.getAttribute("peminjamSurat");

				peminjamanSuratItem = (PeminjamanSuratItem) session.createCriteria(PeminjamanSuratItemDetail.class)
						.createAlias("peminjamanSuratItem", "peminjamanSuratItem")
						.setProjection(Projections.property("peminjamanSuratItem"))
						.add(Restrictions.eq("peminjamanSuratItem.peminjamSurat", peminjamSurat))

//						.add(Restrictions.isNull("kembaliSuratItemDetail"))

						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

				if (peminjamanSuratItem != null) {

					KembaliSuratItemAction.this.kembaliSuratItem = (KembaliSuratItem) session
							.createCriteria(KembaliSuratItem.class)
							.add(Restrictions.eq("peminjamanSuratItem", peminjamanSuratItem)).addOrder(Order.desc("id"))
							.setMaxResults(1).uniqueResult();
					if (KembaliSuratItemAction.this.kembaliSuratItem == null) {
						KembaliSuratItemAction.this.kembaliSuratItem = new KembaliSuratItem();
						KembaliSuratItemAction.this.kembaliSuratItem.setPeminjamanSuratItem(peminjamanSuratItem);
					}

					keterangan.setValue(KembaliSuratItemAction.this.kembaliSuratItem.getKeterangan());
					labelKet.setValue(KembaliSuratItemAction.this.kembaliSuratItem.getKeterangan());

					lablTgl.setValue(Common.dateFormat3.get()
							.format(kembaliSuratItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
									: kembaliSuratItem.getTanggalPembuatan()));

					tanggalPembuatan
							.setValue(kembaliSuratItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
									: kembaliSuratItem.getTanggalPembuatan());

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							kembaliSuratItemPunyaItemHelper.setPeminjamanSuratItem(
									KembaliSuratItemAction.this.kembaliSuratItem.getPeminjamanSuratItem());
						}
					}, "Sedang menyiapkan data item yang dipinjam peminjamSurat ...\nHarap tunggu");

				} else {

					MyMessageboxConfig.show(
							tipe + " dengan kode \"" + KembaliSuratItemAction.this.peminjamSurat.getValue().trim()
									+ "\" belum melakukan proses peminjaman",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									KembaliSuratItemAction.this.peminjamSurat.setAttribute("peminjamSurat", null);
									KembaliSuratItemAction.this.peminjamSurat.setValue("");
								}
							});

				}

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));

		if (persetujuan) {
			row.appendChild(labelKet);
		} else {
			row.appendChild(keterangan);
		}
		keterangan.setWidth("90%");
		keterangan.setRows(10);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		row.appendChild((kembaliSuratItemPunyaItemHelper = new KembaliSuratItemPunyaItemHelper(tipe, persetujuan))
				.initDetail(gridItem = new MyGrid(), kembaliSuratItem, barcodeItem));

		if (kembaliSuratItem.getPeminjamanSuratItem() != null) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kembaliSuratItemPunyaItemHelper.setPeminjamanSuratItem(gridItem,
							kembaliSuratItem.getPeminjamanSuratItem());
				}
			}, "Sedang menyiapkan data item yang dipinjam peminjamSurat ...\nHarap tunggu");
		}

		return grid;
	}

	private void init(final KembaliSuratItem kembaliSuratItem) throws Exception {
		this.kembaliSuratItem = kembaliSuratItem;
		this.peminjamanSuratItem = kembaliSuratItem.getPeminjamanSuratItem();
		addWindow.setTitle(kembaliSuratItem.getId() == null ? "Tambah Kembali Item" : "Ubah Kembali Item");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(kembaliSuratItem, disposisiSop, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
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
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings({})
	public static void kirim(final KembaliSuratItem kembaliSuratItem, final String tipe) throws Exception {

		final File file = Report.generatePDFReport(Report.PDF, parameter(kembaliSuratItem), "pengembalian_arsip",
				kembaliSuratItem.getTanggalPembuatan(), null, Common.locale, null);

		final PeminjamanSuratItem peminjamanSuratItem = kembaliSuratItem.getPeminjamanSuratItem();

		PeminjamSurat peminjamSurat = peminjamanSuratItem.getPeminjamSurat();

		Siswa siswa = peminjamSurat.getSiswa();
		Mahasiswa mahasiswa = peminjamSurat.getMahasiswa();
		Tbmuser tbmuser = peminjamSurat.getTbmuser();

		JSONArray userIds = new JSONArray();
		if (siswa != null && !siswa.getNomorIndukNasional().isEmpty()) {
			userIds.put(siswa.getNomorIndukNasional());
		} else if (mahasiswa != null && mahasiswa.getNim() != null) {
			userIds.put(mahasiswa.getNim());
		} else if (tbmuser != null && tbmuser.getUserId() != null) {
			userIds.put(tbmuser.getUserId());
		}

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Sekolah sekolahD = SekolahUtil.getSekolah();

		final String sekolah = siswa != null ? siswa.getSekolah().getNama()
				: mahasiswa != null && mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null
						&& mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null
								? mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama()
								: peminjamSurat.getDosen() != null
										&& peminjamSurat.getDosen().getPerguruanTinggi() != null
												? peminjamSurat.getDosen().getPerguruanTinggi().getNama()
												: peminjamSurat.getGuru() != null
														&& peminjamSurat.getGuru().getSekolah() != null
																? peminjamSurat.getGuru().getSekolah().getNama()
																: sekolahD != null ? sekolahD.getNama()
																		: perguruanTinggi != null
																				? perguruanTinggi.getNama()
																				: "";

		final String nama = (siswa != null ? siswa.getNama()
				: mahasiswa != null ? mahasiswa.getNama()
						: peminjamSurat.getDosen() != null ? peminjamSurat.getDosen().getNama()
								: peminjamSurat.getGuru() != null ? peminjamSurat.getGuru().getNama()
										: peminjamSurat.getPegawai() != null ? peminjamSurat.getPegawai().getNama()
												: peminjamSurat.getTbmuser() != null
														? peminjamSurat.getTbmuser().getUserNama()
														: "");

		String info = "Atas nama: " + nama;
		String subject = "Informasi Peminjaman Berhasil => " + info;

		String body = "**Yth. " + nama + ",**\r\n" + "\r\n" + "Dengan hormat,\r\n" + "\r\n"
				+ "Kami memberitahukan bahwa pengembalian \"" + tipe + "\" di " + sekolah
				+ " telah berhasil diproses pada " + Common.dateFormat61.get().format(kembaliSuratItem.getTanggalPembuatan())
				+ ".\r\n" + "\r\n" + "Berikut adalah rincian pengembalian Anda:\r\n" + "\r\n"
				+ "*   **Nama Peminjam:** " + nama + "\r\n" + "*   **Tanggal Pengembalian:** "
				+ Common.dateFormat51.get().format(kembaliSuratItem.getTanggalPembuatan()) + "\r\n" + "\r\n"
				+ "Kami mengucapkan terima kasih atas pengembalian buku yang telah dilakukan tepat waktu. Kami berharap Anda dapat terus memanfaatkan layanan dan koleksi yang kami sediakan. Detail mengenai "
				+ tipe + " yang Anda kembalikan dapat Anda lihat pada sistem/portal atau pada file terlampir.\r\n"
				+ "\r\n" + "Terima kasih atas partisipasi Anda.";

		String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();

		String emailUser = "";
		if (siswa != null && siswa.getAlamatEmail() != null && Common.isValidEmailAddress(siswa.getAlamatEmail())) {
			emailUser += emailUser.trim().isEmpty() ? siswa.getAlamatEmail().trim()
					: "," + siswa.getAlamatEmail().trim();
		}
		if (mahasiswa != null && mahasiswa.getEmail() != null && Common.isValidEmailAddress(mahasiswa.getEmail())) {
			emailUser += emailUser.trim().isEmpty() ? mahasiswa.getEmail().trim() : "," + mahasiswa.getEmail().trim();
		}
		if (peminjamSurat.getDosen() != null && peminjamSurat.getDosen().getEmail() != null
				&& Common.isValidEmailAddress(peminjamSurat.getDosen().getEmail())) {
			emailUser += emailUser.trim().isEmpty() ? peminjamSurat.getDosen().getEmail().trim()
					: "," + peminjamSurat.getDosen().getEmail().trim();
		}
		if (peminjamSurat.getGuru() != null && peminjamSurat.getGuru().getAlamatEmail() != null
				&& Common.isValidEmailAddress(peminjamSurat.getGuru().getAlamatEmail())) {
			emailUser += emailUser.trim().isEmpty() ? peminjamSurat.getGuru().getAlamatEmail().trim()
					: "," + peminjamSurat.getGuru().getAlamatEmail().trim();
		}
		if (peminjamSurat.getPegawai() != null && peminjamSurat.getPegawai().getEmail() != null
				&& Common.isValidEmailAddress(peminjamSurat.getPegawai().getEmail())) {
			emailUser += emailUser.trim().isEmpty() ? peminjamSurat.getPegawai().getEmail().trim()
					: "," + peminjamSurat.getPegawai().getEmail().trim();
		}
		if (peminjamSurat.getTbmuser() != null && peminjamSurat.getTbmuser().getEmail() != null
				&& Common.isValidEmailAddress(peminjamSurat.getTbmuser().getEmail())) {
			emailUser += emailUser.trim().isEmpty() ? peminjamSurat.getTbmuser().getEmail().trim()
					: "," + peminjamSurat.getTbmuser().getEmail().trim();
		}
		JSONArray attachmentsData = null;

		MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser, kembaliSuratItem, attachmentsData, false,
				file);

		if (Common.bolehKonfigurasi("aktifkan_kirim_notif_pengembalian_buku_perpustakaan_ke_wa")) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal",
							"*Pesan ini dibuat secara otomatis oleh sistem sebagai notifikasi/pemberitahuan kepada Anda*\n\n")
							.getNilai();

					Set<String> forms = new HashSet<String>();
					forms.add(peminjamanSuratItem.getPeminjamSurat().getTelp());
					forms.add(peminjamanSuratItem.getPeminjamSurat().getHp());

					if (peminjamanSuratItem.getPeminjamSurat().getSiswa() != null) {
						forms.addAll(peminjamanSuratItem.getPeminjamSurat().getSiswa().ambilTelp());
					}

					for (String from : forms) {

						if (from != null && !from.trim().isEmpty()
								&& !(from == null || from.toString().trim().isEmpty()
										|| from.toString().trim().equals("00000000000000000000")
										|| from.toString().trim().equals("000000000"))) {

							String body = "**Yth. " + nama + ",**\r\n" + "\r\n" + "Dengan hormat,\r\n" + "\r\n"
									+ "Kami memberitahukan bahwa pengembalian \"" + tipe + "\" di " + sekolah
									+ " telah berhasil diproses pada "
									+ Common.dateFormat61.get().format(kembaliSuratItem.getTanggalPembuatan()) + ".\r\n"
									+ "\r\n" + "Berikut adalah rincian pengembalian Anda:\r\n" + "\r\n"
									+ "*   **Nama Peminjam:** " + nama + "\r\n" + "*   **Tanggal Pengembalian:** "
									+ Common.dateFormat51.get().format(kembaliSuratItem.getTanggalPembuatan()) + "\r\n"
									+ "\r\n"
									+ "Kami mengucapkan terima kasih atas pengembalian buku yang telah dilakukan tepat waktu. Kami berharap Anda dapat terus memanfaatkan layanan dan koleksi yang kami sediakan. Detail mengenai "
									+ tipe
									+ " yang Anda kembalikan dapat Anda lihat pada sistem/portal atau pada file terlampir.\r\n"
									+ "\r\n" + "Terima kasih atas partisipasi Anda.";

							String urlD = Common.getRequestHostWithProtocolSimple()
									+ file.getAbsolutePath().split("webapps")[1];

							Wa.kirimWaViaUltramsg(from, dawal + body, "Bukti_Pengembalian.pdf", urlD);
						}
					}
				}
			}, "", false, 2000);
		}
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		PeminjamSurat peminjamSurat = (PeminjamSurat) KembaliSuratItemAction.this.peminjamSurat
				.getAttribute("peminjamSurat");

		final List<Row> rowsItem = gridItem.getRows().getChildren();
		for (Row row : rowsItem) {
			KembaliSuratItemDetail kembaliSuratItemDetail = (KembaliSuratItemDetail) row
					.getAttribute("kembaliSuratItemDetail");
			if (kembaliSuratItemDetail.getSuratMasuk() == null) {
				MyMessageboxConfig.show(tipe + " harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (kembaliSuratItem.getId() != null) {
			kembaliSuratItem = (KembaliSuratItem) session.load(KembaliSuratItem.class, kembaliSuratItem.getId());

		}

		peminjamanSuratItem = (PeminjamanSuratItem) session.createCriteria(PeminjamanSuratItemDetail.class)
				.createAlias("peminjamanSuratItem", "peminjamanSuratItem")
				.setProjection(Projections.property("peminjamanSuratItem"))
				.add(Restrictions.eq("peminjamanSuratItem.peminjamSurat", peminjamSurat))

				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

		kembaliSuratItem.setPeminjamanSuratItem(peminjamanSuratItem);
		kembaliSuratItem.setKode(kode.getValue());
		kembaliSuratItem.setKeterangan(keterangan.getValue());
		kembaliSuratItem.setTanggalPembuatan(tanggalPembuatan.getValue());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			kembaliSuratItem.setDisposisiSop(disposisiSop);
		}

		if (kembaliSuratItem.getId() != null) {
			Common.refreshUpdate(session, kembaliSuratItem);
		} else {
			kembaliSuratItem.setDibuatOleh(Common.getCurrentUser());

			kembaliSuratItem.setIndex(LibraryUtil.generateMaxByPerpustakaan(KembaliSuratItem.class) + 1);
			String mykode = LibraryUtil.generateCode(KembaliSuratItem.class, 8, "KMB");
			kode.setValue(mykode);
			kembaliSuratItem.setKode(mykode);
			session.save(kembaliSuratItem);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				for (Row row : rowsItem) {
					try {
						KembaliSuratItemDetail kembaliSuratItemDetail = (KembaliSuratItemDetail) row
								.getAttribute("kembaliSuratItemDetail");
						if (kembaliSuratItemDetail.getId() != null) {
							session.refresh(kembaliSuratItemDetail);
						}
						kembaliSuratItemDetail.setKembaliSuratItem(kembaliSuratItem);

						Datebox datebox = (Datebox) row.getAttribute("tanggal");

						PeminjamanSuratItemDetail peminjamanSuratItemDetail = kembaliSuratItemDetail
								.getPeminjamanSuratItemDetail();
						session.refresh(peminjamanSuratItemDetail);
						peminjamanSuratItemDetail.setTanggalKembali(datebox.getValue());

						Common.refreshSaveOrUpdate(session, kembaliSuratItemDetail);

						peminjamanSuratItemDetail.setKembaliSuratItemDetail(kembaliSuratItemDetail);
						Common.refreshSaveOrUpdate(session, peminjamanSuratItemDetail);

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

				setujui(KembaliSuratItemAction.this.kembaliSuratItem);

			}
		});

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(KembaliSuratItem.class)

		;
		boolean ada = !searchbarkode.getValue().trim().isEmpty() || !searchjudul.getValue().trim().isEmpty();
		if (ada) {
			List<Long> ids = session.createCriteria(KembaliSuratItemDetail.class)

					.createAlias("suratMasuk", "suratMasuk", Criteria.LEFT_JOIN)

					.add(searchjudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("suratMasuk.perihal", searchjudul.getValue().trim(),
									MatchMode.ANYWHERE))

					.add(searchbarkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.ilike("suratMasuk.kode", searchbarkode.getValue().trim(),
											MatchMode.EXACT),
									Restrictions.ilike("suratMasuk.noSurat", searchbarkode.getValue().trim(),
											MatchMode.EXACT)))

					.setProjection(Projections.groupProperty("kembaliSuratItem.id"))

					.setMaxResults(32760)

					.list();

			if (!ids.isEmpty()) {
				criteria.add(Restrictions.in("id", ids));
			}
		}

		criteria.createAlias("peminjamanSuratItem", "peminjamanSuratItem", Criteria.LEFT_JOIN)

				.add(Restrictions.or(
						Restrictions.or(Restrictions.isNull("peminjamanSuratItem.satuanKerja"),
								satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(
												parent == null ? Restrictions.isNull("peminjamanSuratItem.satuanKerja")
														: Restrictions.sqlRestriction("false"),
												Restrictions.in("peminjamanSuratItem.satuanKerja", satuanKerjas))),
						Restrictions
								.or(Restrictions.isNull("peminjamanSuratItem.kepadaSatuanKerja"),
										satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
												: Restrictions.or(
														parent == null
																? Restrictions
																		.isNull("peminjamanSuratItem.kepadaSatuanKerja")
																: Restrictions.sqlRestriction("false"),
														Restrictions.in("peminjamanSuratItem.kepadaSatuanKerja",
																satuanKerjas)))))

				.add((searchpeminjamSurat == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpeminjamSurat.getAttribute("peminjamSurat") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("peminjamanSuratItem.peminjamSurat",
								searchpeminjamSurat.getAttribute("peminjamSurat"))))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add((searchmulai == null || searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(searchmulai.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(searchsampai.getValue()) + "')")));

		if (order)
			criteria.addOrder(Order.desc("tanggalPembuatan"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchbarkode == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);
		List<KembaliSuratItem> kembaliSuratItem = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kembaliSuratItem);
		grid.setRowRenderer(new KembaliSuratItemRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void onKodePeminjamSurat(Event event) throws Exception {
		if (searchkodeangota.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Peminjam Surat belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Kode Peminjam Surat; (2) isikan kode peminjam surat secara lengkap; (3) klik tombol Cari untuk mencari data peminjam. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		PeminjamSurat peminjamSurat = (PeminjamSurat) session.createCriteria(PeminjamSurat.class)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("dosen", "dosen", Criteria.LEFT_JOIN).createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)
				.add(Restrictions.or(
						Restrictions.ilike("pegawai.mycode", searchkodeangota.getValue().trim(), MatchMode.EXACT),
						Restrictions.or(
								Restrictions.ilike("pegawai.code", searchkodeangota.getValue().trim(), MatchMode.EXACT),
								Restrictions.or(Restrictions.or(
										Restrictions.ilike("kode", searchkodeangota.getValue().trim(), MatchMode.EXACT),
										Restrictions.ilike("mahasiswa.nim", searchkodeangota.getValue().trim(),
												MatchMode.EXACT)),
										Restrictions.ilike("dosen.mycode", searchkodeangota.getValue().trim(),
												MatchMode.EXACT)))))
				.setMaxResults(1).uniqueResult();

		if (peminjamSurat == null) {
			peminjamSurat = (PeminjamSurat) session.createCriteria(PeminjamanSuratItemDetail.class)
					.createAlias("suratMasuk", "suratMasuk").createAlias("peminjamanSuratItem", "peminjamanSuratItem")
					.setProjection(Projections.property("peminjamanSuratItem.peminjamSurat"))
					.add(Restrictions.or(Restrictions.eq("suratMasuk.noSurat", searchkodeangota.getValue().trim()),
							Restrictions.eq("suratMasuk.kode", searchkodeangota.getValue().trim()))

					).add(Restrictions.isNull("kembaliSuratItemDetail")).addOrder(Order.desc("id")).setMaxResults(1)
					.uniqueResult();

			if (peminjamSurat != null) {
				barcodeItem = searchkodeangota.getValue().trim();
			} else {
				barcodeItem = null;
			}
		} else {
			barcodeItem = null;
		}

		if (peminjamSurat == null) {
			MyMessageboxConfig.show("Kode " + tipe + " \"" + searchkodeangota.getValue().trim() + "\" tidak ditemukan",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							searchkodeangota.focus();
							searchkodeangota.select();
						}
					});
			return;
		}

		PeminjamanSuratItem peminjamanSuratItem = (PeminjamanSuratItem) session
				.createCriteria(PeminjamanSuratItemDetail.class)
				.createAlias("peminjamanSuratItem", "peminjamanSuratItem")
				.setProjection(Projections.property("peminjamanSuratItem"))
				.add(Restrictions.eq("peminjamanSuratItem.peminjamSurat", peminjamSurat))
//				.add(Restrictions.isNull("kembaliSuratItemDetail"))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

		if (peminjamanSuratItem != null) {

			KembaliSuratItem kembaliSuratItem = (KembaliSuratItem) session.createCriteria(KembaliSuratItem.class)
					.add(Restrictions.eq("peminjamanSuratItem", peminjamanSuratItem)).addOrder(Order.desc("id"))
					.setMaxResults(1).uniqueResult();
			if (kembaliSuratItem != null) {
				init(kembaliSuratItem);
				addWindow.setVisible(true);
				addWindow.onModal();
			} else {
				kembaliSuratItem = new KembaliSuratItem();
				kembaliSuratItem.setPeminjamanSuratItem(peminjamanSuratItem);
				init(kembaliSuratItem);
				addWindow.setVisible(true);
				addWindow.onModal();
			}
		} else {

			MyMessageboxConfig.show(
					tipe + " dengan kode \"" + searchkodeangota.getValue().trim()
							+ "\" belum melakukan proses peminjaman",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							searchkodeangota.focus();
							searchkodeangota.select();
						}
					});

		}

		searchkodeangota.setValue("");
		searchkodeangota.focus();
		searchkodeangota.select();
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengembalian " + tipe;
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return kembaliSuratItem;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return KembaliSuratItem.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		KembaliSuratItem kembaliSuratItem = (KembaliSuratItem) generalValueObject;
		File file = Report.generateFileReport(Report.PDF, parameter(kembaliSuratItem), "pengembalian_arsip",
				ais.ui.util.WaktuUtil.getDate(), null, new Toolbar());
		return file;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map parameter(KembaliSuratItem kembaliSuratItem) throws Exception {

		PeminjamanSuratItem peminjamanSuratItem = kembaliSuratItem.getPeminjamanSuratItem();

		File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/barcode_" + peminjamanSuratItem.getKode() + ".png");
		Barcode mybarcode = BarcodeFactory.createCode128B(peminjamanSuratItem.getKode());
		BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
		String kodeBarcode = myfilebarcode.getAbsolutePath();

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", peminjamanSuratItem.getId());
		parameters.put("kode_barcode", kodeBarcode);

		parameters.put("info", LibraryUtil.tampilanSummaryPeminjaman(null, peminjamanSuratItem));

		Common.insertProperty(KembaliSuratItem.class, kembaliSuratItem, parameters, "kembali", 2,
				"peminjamanSuratItem");
		Common.insertProperty(PeminjamanSuratItem.class, peminjamanSuratItem, parameters, "", 1, "kembaliSuratItem");

		List<Map> maps = new ArrayList<Map>();
		List<KembaliSuratItemDetail> objects = HibernateUtil.currentSession()
				.createCriteria(KembaliSuratItemDetail.class).add(Restrictions.eq("kembaliSuratItem", kembaliSuratItem))
				.list();

		for (KembaliSuratItemDetail kembaliSuratItemDetail : objects) {
			SuratMasuk suratMasuk = kembaliSuratItemDetail.getSuratMasuk();
			Map map = new HashMap();
			Common.insertProperty(KembaliSuratItemDetail.class, kembaliSuratItemDetail, map, "", 1, "suratMasuk");
			Common.insertProperty(SuratMasuk.class, suratMasuk, map, "suratMasuk", 2);
			maps.add(map);
		}
		parameters.put("maps", maps);

		DisposisiAlurSop.parameterMap(kembaliSuratItem.getDisposisiSop(), parameters);

		return parameters;
	}
}
