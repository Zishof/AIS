package ais.action.master.employ;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.employ.KenaikanGajiBerkalaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Pegawai;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.KenaikanGajiBerkala;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk kenaikan gaji berkala. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code String DISETUJUI}, {@code String BELUM_DIPROSES}, {@code
 * AmbilDataPegawaiBanbox ambilDataPegawaiBanbox}, {@code AmbilDataPegawaiBanbox searchpegawai}, {@code Textbox
 * noSK}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi
 * domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
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
public class KenaikanGajiBerkalaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private static final String DISETUJUI = "DISETUJUI";
	private static final String BELUM_DIPROSES = "BELUM DIPROSES";

	private AmbilDataPegawaiBanbox ambilDataPegawaiBanbox = new AmbilDataPegawaiBanbox();
	private AmbilDataPegawaiBanbox searchpegawai;
	// private Textbox searchnama;

	// private Combobox pegawai;
	private Textbox noSK;
	private MyDatebox tanggalSK;
	private Intbox masaKerjaTahun;
	private Intbox masaKerjaBulan;
	private Combobox gajiPokokBaru;
	private MyDatebox tmt;
	private MyDatebox naikBerikutnya;
	private Radiogroup status;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KenaikanGajiBerkala kenaikanGajiBerkala;
	private MyToolbarbuttonConfig add;
	private Pegawai pegawai;

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

		if (session.getAttribute("pegawai") == null) {
			pegawai = (Pegawai) session.getAttribute("pegawai");
		}
		if (Common.getCurrentUser().getDosen() != null) {
			Dosen dosen = Common.getCurrentUser().getDosen();
			pegawai = (Pegawai) HibernateUtil.currentSession().createCriteria(Pegawai.class)
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.eq("dosen", dosen)).setMaxResults(1).uniqueResult();
			if (dosen != null && pegawai == null) {
				MyMessageboxConfig.show("Anda Belum Memiliki Data Kepegawaian", MyMessageboxConfig.INFORMATION,
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
		}

		if (this.pegawai != null) {
			searchpegawai.setAttribute("pegawai", pegawai);
			searchpegawai.setValue(pegawai.toString());
			searchpegawai.setDisabled(true);
		}

		// Common.insertCombo(pegawai, "nama", Pegawai.class);
		Common.insertCombo(gajiPokokBaru, "nama", GajiPokok.class);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KenaikanGajiBerkalaAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KenaikanGajiBerkalaAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KenaikanGajiBerkalaAction
	 */
	class KenaikanGajiBerkalaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KenaikanGajiBerkala kenaikanGajiBerkala = (KenaikanGajiBerkala) arg1;

			// new Label(kenaikanGajiBerkala.getPegawai().getNama())
			// .setParent(arg0);
			RevisiHelper.createNewRevisi(KenaikanGajiBerkala.class, kenaikanGajiBerkala,
					kenaikanGajiBerkala.getPegawai().getNama()).setParent(arg0);
			new Label(kenaikanGajiBerkala.getNoSK() == null ? "" : kenaikanGajiBerkala.getNoSK()).setParent(arg0);
			new Label(kenaikanGajiBerkala.getTanggalSK() == null ? ""
					: Common.dateFormat2.get().format(kenaikanGajiBerkala.getTanggalSK())).setParent(arg0);
			new Label(kenaikanGajiBerkala.getMasaKerjaTahun() == null ? ""
					: kenaikanGajiBerkala.getMasaKerjaTahun().toString()).setParent(arg0);
			new Label(kenaikanGajiBerkala.getMasaKerjaBulan() == null ? ""
					: kenaikanGajiBerkala.getMasaKerjaBulan().toString()).setParent(arg0);
			new Label(
					kenaikanGajiBerkala.getTmt() == null ? "" : Common.dateFormat2.get().format(kenaikanGajiBerkala.getTmt()))
					.setParent(arg0);
			new Label(kenaikanGajiBerkala.getGaji() == null ? "" : kenaikanGajiBerkala.getGaji().toString())
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kenaikanGajiBerkala);
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
											KenaikanGajiBerkalaDao kenaikanGajiBerkalaDao = DaoFactory.getInstance()
													.getKenaikanGajiBerkalaDao();
											kenaikanGajiBerkalaDao.delete((kenaikanGajiBerkala));
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

			// button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			// button.setTooltiptext("Generate SK");
			// button.addEventListener("onClick", new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// // TODO Auto-generated method stub
			// GenerateSkHelper generateSkHelper = new GenerateSkHelper();
			// generateSkHelper.display(addWindow);
			// addWindow.setVisible(true);
			// addWindow.onModal();
			//
			// }
			// });
			// button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new KenaikanGajiBerkala());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KenaikanGajiBerkala kenaikanGajiBerkala) {
		this.kenaikanGajiBerkala = kenaikanGajiBerkala;
		addWindow.setTitle(kenaikanGajiBerkala.getId() == null ? "Tambah Kenaikan Gaji Berkala" : "Ubah Kenaikan Gaji Berkala");
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
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("80%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		// row.appendChild(pegawai = new Combobox());
		// Common.insertCombo(pegawai, "nama", Pegawai.class);
		// Common.selectComboItem(pegawai,
		// kenaikanGajiBerkala.getPegawai() == null ? ""
		// : kenaikanGajiBerkala.getPegawai());
		// pegawai.setWidth("90%");
		row.appendChild(ambilDataPegawaiBanbox);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Pegawai pegawaiSelected = (Pegawai) ambilDataPegawaiBanbox.getAttribute("pegawai");
				// Session session = HibernateUtil.currentSession();

				if (pegawaiSelected != null) {
					Common.selectComboItem(gajiPokokBaru, null);
				}
			}
		};

		ambilDataPegawaiBanbox.setValue(kenaikanGajiBerkala.getPegawai() == null ? ""
				: kenaikanGajiBerkala.getPegawai().getCode() + " - " + kenaikanGajiBerkala.getPegawai().getNama());
		ambilDataPegawaiBanbox.setAttribute("pegawai", kenaikanGajiBerkala.getPegawai());
		ambilDataPegawaiBanbox.setEventListener(eventListener);
		ambilDataPegawaiBanbox.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No SK"));
		row.appendChild(noSK = new Textbox(kenaikanGajiBerkala.getNoSK() == null ? "" : kenaikanGajiBerkala.getNoSK()));
		noSK.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK"));
		row.appendChild(
				tanggalSK = new MyDatebox(kenaikanGajiBerkala.getTanggalSK() == null ? ais.ui.util.WaktuUtil.getDate()
						: kenaikanGajiBerkala.getTanggalSK()));
//		tanggalSK.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Kerja Bulan"));
		row.appendChild(masaKerjaBulan = new Intbox(
				kenaikanGajiBerkala.getMasaKerjaBulan() == null ? 0 : kenaikanGajiBerkala.getMasaKerjaBulan()));
		masaKerjaBulan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Kerja Tahun"));
		row.appendChild(masaKerjaTahun = new Intbox(
				kenaikanGajiBerkala.getMasaKerjaTahun() == null ? 0 : kenaikanGajiBerkala.getMasaKerjaTahun()));
		masaKerjaTahun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gaji Pokok Baru"));
		row.appendChild(gajiPokokBaru = new Combobox());
		Common.insertCombo(gajiPokokBaru, "gaji", GajiPokok.class);
		Common.selectComboItem(gajiPokokBaru,
				kenaikanGajiBerkala.getGajiPokokBaru() == null ? "" : kenaikanGajiBerkala.getGajiPokokBaru());
		gajiPokokBaru.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TMT"));
		row.appendChild(tmt = new MyDatebox(
				kenaikanGajiBerkala.getTmt() == null ? ais.ui.util.WaktuUtil.getDate() : kenaikanGajiBerkala.getTmt()));
//		tmt.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Naik Berikutnya"));
		row.appendChild(naikBerikutnya = new MyDatebox(
				kenaikanGajiBerkala.getNaikBerikutnya() == null ? ais.ui.util.WaktuUtil.getDate()
						: kenaikanGajiBerkala.getNaikBerikutnya()));
//		naikBerikutnya.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(status = new Radiogroup());
		status.appendItem(DISETUJUI, DISETUJUI);
		status.appendItem(BELUM_DIPROSES, BELUM_DIPROSES);
		if (kenaikanGajiBerkala.getStatus() == null || kenaikanGajiBerkala.getStatus().equals(BELUM_DIPROSES)) {
			status.setSelectedIndex(1);
		} else {
			status.setSelectedIndex(0);
		}
		status.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				kenaikanGajiBerkala.getKeterangan() == null ? "" : kenaikanGajiBerkala.getKeterangan()));
		keterangan.setWidth("90%");

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
		// if (pegawai.getSelectedItem() == null) {
		if (ambilDataPegawaiBanbox.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) cari dan pilih Pegawai menggunakan kolom pencarian; (2) pastikan data pegawai sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (noSK.getValue().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, No SK belum diisi. Langkah yang dapat dilakukan: (1) isi kolom No SK pada form; (2) pastikan nomor SK tidak kosong dan sesuai format; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (masaKerjaBulan.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Masa Kerja Bulan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Masa Kerja Bulan pada form; (2) pastikan nilai berupa angka bulan yang valid (0-11); (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (masaKerjaTahun.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Masa Kerja Tahun belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Masa Kerja Tahun pada form; (2) pastikan nilai berupa angka tahun yang valid; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (gajiPokokBaru.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Gaji Pokok Baru belum dipilih. Langkah yang dapat dilakukan: (1) pilih Gaji Pokok Baru dari dropdown yang tersedia; (2) pastikan data gaji pokok sudah tersedia di master; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return false;
		}

		KenaikanGajiBerkalaDao kenaikanGajiBerkalaDao = DaoFactory.getInstance().getKenaikanGajiBerkalaDao();
		if (kenaikanGajiBerkala.getId() != null) {
			kenaikanGajiBerkala = kenaikanGajiBerkalaDao.load(kenaikanGajiBerkala.getId());

		}

		// kenaikanGajiBerkala
		// .setPegawai((Pegawai) (pegawai.getSelectedItem() == null ? null
		// : pegawai.getSelectedItem().getValue()));
		kenaikanGajiBerkala.setPegawai((Pegawai) ambilDataPegawaiBanbox.getAttribute("pegawai"));
		kenaikanGajiBerkala.setNoSK(noSK.getValue());
		kenaikanGajiBerkala.setTanggalSK(tanggalSK.getValue());
		kenaikanGajiBerkala.setMasaKerjaBulan(masaKerjaBulan.getValue());
		kenaikanGajiBerkala.setMasaKerjaTahun(masaKerjaTahun.getValue());
		kenaikanGajiBerkala.setGajiPokokBaru((GajiPokok) (gajiPokokBaru.getSelectedItem() == null ? null
				: gajiPokokBaru.getSelectedItem().getValue()));
		kenaikanGajiBerkala.setGaji((GajiPokok) (gajiPokokBaru.getSelectedItem() == null ? null
				: gajiPokokBaru.getSelectedItem().getValue()));
		kenaikanGajiBerkala.setTmt(tmt.getValue());
		kenaikanGajiBerkala.setNaikBerikutnya(naikBerikutnya.getValue());
		kenaikanGajiBerkala.setStatus(status.getSelectedItem()==null||status.getSelectedItem().getValue()==null ? null : status.getSelectedItem().getValue().toString());
		kenaikanGajiBerkala.setKeterangan(keterangan.getValue());

		if (kenaikanGajiBerkala.getId() != null) {
			kenaikanGajiBerkalaDao.update(kenaikanGajiBerkala);
		} else {
			kenaikanGajiBerkalaDao.save(kenaikanGajiBerkala);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KenaikanGajiBerkala.class);
		if (order)
			criteria.addOrder(Order.asc("pegawai"));

		criteria.add((searchpegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("pegawai", searchpegawai.getAttribute("pegawai"))));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging(initCriteria(false), paging);

		List<KenaikanGajiBerkala> keiankanGajiBerkala = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(keiankanGajiBerkala);
		grid.setRowRenderer(new KenaikanGajiBerkalaRenderer());
		grid.setModelCheckMobile(strset);

	}
}