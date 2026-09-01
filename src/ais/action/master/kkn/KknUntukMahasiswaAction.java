package ais.action.master.kkn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataKelompokKknBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.ItemBiaya;
import ais.database.model.Kkn;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.kkn.KknPunyaPersyaratan;
import ais.database.model.kkn.MahasiswaDaftarKkn;
import ais.database.model.kkn.MahasiswaKknPersyaratan;
import ais.database.model.kkn.PersyaratanKkn;
import ais.database.model.pkl.PersyaratanPkl;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextboxAngka;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk kkn untuk mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow daftar_kkn_window}, {@code
 * Paging paging}, {@code MyGrid grid}, {@code Mahasiswa mahasiswa}, {@code Tabpanel kkn}, {@code Boolean
 * memenuhiSyarat}, {@code EventListener eventListener}; inisialisasi/lifecycle ({@code doBeforeCompose()},
 * {@code doAfterCompose()}, {@code initCriteria()}, {@code init()}); pembacaan/pencarian ({@code
 * onSearchDefault()}, {@code tampilkanPersyaratan()}); operasi domain lain ({@code onKKN()}, {@code daftar()},
 * {@code onAddExternal()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class KknUntukMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow daftar_kkn_window;
	private Paging paging;
	private MyGrid grid;

	// private MyToolbarbuttonConfig add;
	private Mahasiswa mahasiswa;

	protected Tabpanel kkn;

	public void onKKN(Event event) {

		if (kkn.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(kkn);
			include.setSrc("/pages/master/kkn/kelompok_kkn.zul");
		}
	}

	// private Textbox no_SKTM;
	// private Textbox pejabat_penandatangan;

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
		// if (session.getAttribute("usersTemp") == null
		// || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
		// session.removeAttribute("usersTemp");
		// Common.goLogoff();
		// return;
		// }
		//
		// add.setVisible(CommonPrivilages
		// .checkPrevilages(CommonPrivilages.CREATE));
		// add.setTooltiptext("Tambah");

		mahasiswa = Common.getCurrentUser().getMahasiswa();

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KknUntukMahasiswaAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KknUntukMahasiswaAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KknUntukMahasiswaAction
	 */
	class KknRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Kkn kkn = (Kkn) arg1;

			RevisiHelper.createNewRevisi(Kkn.class, kkn, kkn.getNama_kelompok()).setParent(arg0);
			new Label(kkn.getTanggal_mulai() == null ? "" : Common.dateFormat4.get().format(kkn.getTanggal_mulai()))
					.setParent(arg0);
			new Label(kkn.getTanggal_selesai() == null ? "" : Common.dateFormat4.get().format(kkn.getTanggal_selesai()))
					.setParent(arg0);

			new Label(kkn.getFakultas() == null ? "Semua" : kkn.getFakultas().getNama()).setParent(arg0);
			new Label(kkn.getJurusan() == null ? "Semua" : kkn.getJurusan().getNama()).setParent(arg0);
			new Label(kkn.getProgram() == null || kkn.getProgram().trim().isEmpty() ? "Semua" : kkn.getProgram())
					.setParent(arg0);
			new Label(kkn.getMinimalSksBolehIkutKkn() + " SKS, IPK min " + kkn.getMinimalIpkBolehIkutKkn())
					.setParent(arg0);

			new Label(kkn.getAktifkanSyaratLain()
					? (kkn.getMinimalSksBolehIkutKkn2() + " SKS, IPK min " + kkn.getMinimalIpkBolehIkutKkn2())
					: "").setParent(arg0);

			final Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					if (!kkn.getKodeItemBiaya().trim().isEmpty()) {

						for (String kode : kkn.getKodeItemBiaya().trim().split(",")) {
							ItemBiaya itemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
									.add(Restrictions.eq("kode", kode.trim())).setMaxResults(1).uniqueResult();
							if (itemBiaya != null) {
								new Label(itemBiaya.getKode() + "-" + itemBiaya.getNama()).setParent(hbox);
							}
						}
					} else {
						new Label("-").setParent(hbox);
					}
				}
			});

			Session session = HibernateUtil.currentSession();
			final MahasiswaDaftarKkn mahasiswaDaftarKkn = (MahasiswaDaftarKkn) session
					.createCriteria(MahasiswaDaftarKkn.class).add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("kkn", kkn)).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

			Label lbl;
			(lbl = new Label(mahasiswaDaftarKkn == null ? "TIDAK TERDAFTAR"
					: mahasiswaDaftarKkn.getTerima().equals(MahasiswaDaftarKkn.BELUM_DIPROSES) ? "Belum Diproses"
							: mahasiswaDaftarKkn.getTerima().equals(MahasiswaDaftarKkn.DITERIMA) ? "DITERIMA"
									: "DITOLAK"))
					.setParent(arg0);

			if (mahasiswaDaftarKkn != null && mahasiswaDaftarKkn.getTerima().equals(MahasiswaDaftarKkn.DITERIMA)) {
				MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn = (MahasiswaDapatKelompokKkn) HibernateUtil
						.currentSession().createCriteria(MahasiswaDapatKelompokKkn.class).setMaxResults(1)
						.createAlias("kelompokKkn", "kelompokKkn").add(Restrictions.eq("kelompokKkn.kkn", kkn))
						.add(Restrictions.eq("mahasiswa", mahasiswa)).uniqueResult();

				if (mahasiswaDapatKelompokKkn == null || !mahasiswaDapatKelompokKkn.getDiterima()) {
					Hbox hb = new Hbox();
					hb.setParent(arg0);
					hb.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelompok : ")));
					final AmbilDataKelompokKknBanbox ambil;
					hb.appendChild(ambil = new AmbilDataKelompokKknBanbox());
					ambil.setAttribute("kelompokKkn",
							mahasiswaDapatKelompokKkn == null ? null : mahasiswaDapatKelompokKkn.getKelompokKkn());
					ambil.setAttribute("myValue",
							mahasiswaDapatKelompokKkn == null ? null : mahasiswaDapatKelompokKkn.getKelompokKkn());
					ambil.setValue(
							mahasiswaDapatKelompokKkn == null || mahasiswaDapatKelompokKkn.getKelompokKkn() == null
									? null
									: mahasiswaDapatKelompokKkn.getKelompokKkn().getNama());
					ambil.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							KelompokKkn kelompokKkn = (KelompokKkn) ambil.getAttribute("kelompokKkn");

							MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn = (MahasiswaDapatKelompokKkn) HibernateUtil
									.currentSession().createCriteria(MahasiswaDapatKelompokKkn.class).setMaxResults(1)
									.createAlias("kelompokKkn", "kelompokKkn")
									.add(Restrictions.eq("kelompokKkn.kkn", kkn))
									.add(Restrictions.eq("mahasiswa", mahasiswa)).uniqueResult();
							if (mahasiswaDapatKelompokKkn == null) {
								mahasiswaDapatKelompokKkn = new MahasiswaDapatKelompokKkn();
							}
							mahasiswaDapatKelompokKkn.setMahasiswa(mahasiswa);
							mahasiswaDapatKelompokKkn.setKelompokKkn(kelompokKkn);
							Common.refreshSaveOrUpdate(mahasiswaDapatKelompokKkn);
						}
					});

				} else {
					lbl.setValue("DISETUJUI");
					arg0.appendChild(new Label(mahasiswaDapatKelompokKkn.getKelompokKkn().getNama()));
				}
			} else {
				arg0.appendChild(new Label());
			}

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dikumpulkan lalu dibungkus
			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Daftar", "/img/svg/check2-circle.svg");
			button.setTooltiptext("Daftar");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					init(kkn, false);
				}

			});
			aksiButtons.add(button);

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			Date sekarang = calendar.getTime();
			button.setDisabled(mahasiswaDaftarKkn != null || sekarang.after(kkn.getTanggal_selesai()));
			button.setVisible(!button.isDisabled());

			button = new MyToolbarbuttonConfig(mahasiswaDaftarKkn != null
					&& mahasiswaDaftarKkn.getTerima().equals(MahasiswaDaftarKkn.BELUM_DIPROSES) ? "Ubah" : "Lihat",
					"/img/absensi_pmb.png");
			button.setTooltiptext("Lihat");
			button.setOrient("vertical");
			button.setVisible(mahasiswaDaftarKkn != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					init(kkn, !mahasiswaDaftarKkn.getTerima().equals(MahasiswaDaftarKkn.BELUM_DIPROSES));
				}

			});
			aksiButtons.add(button);

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak Bukti", "/img/print.png");
			cetak.setOrient("vertical");
			cetak.addEventListener("onClick", new EventListener() {

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					Map parameters = ais.common.HashMapGenerator.getRand();
					parameters.put("id_mahasiswa", mahasiswa.getId());
					parameters.put("id_kkn", kkn.getId());
					mahasiswa.putPhoto(parameters);
					Report.generatePDFReport(Report.PDF, parameters, "kartu_daftar_kkn",
							ais.ui.util.WaktuUtil.getDate());
				}
			});
			aksiButtons.add(cetak);
			cetak.setVisible(mahasiswaDaftarKkn != null);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Kkn.class)
				.add(Restrictions.or(Restrictions.isNull("jurusan"),
						Restrictions.eq("jurusan", mahasiswa == null ? null : mahasiswa.getJurusan())))
				.add(Restrictions.or(Restrictions.isNull("fakultas"),
						Restrictions.eq("fakultas", mahasiswa == null ? null : mahasiswa.getJurusan().getFakultas())))
				.add(Restrictions.or(Restrictions.isNull("program"),
						Restrictions.eq("program", mahasiswa == null ? null : mahasiswa.getProgram())));

		if (order)
			criteria.addOrder(Order.desc("tanggal_mulai")).addOrder(Order.desc("id"));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (grid != null) {
			Common.initPaging(initCriteria(false), paging);

			List<Kkn> kkn = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
			ListModel strset = new SimpleListModel(kkn);
			grid.setRowRenderer(new KknRenderer());
			grid.setModelCheckMobile(strset);
		}

	}

	@SuppressWarnings("unchecked")
	public boolean daftar(Kkn kkn) throws Exception {

		Session session = HibernateUtil.currentSession();
		MahasiswaDaftarKkn mahasiswaDaftarKkn = (MahasiswaDaftarKkn) session.createCriteria(MahasiswaDaftarKkn.class)
				.add(Restrictions.eq("kkn", kkn)).add(Restrictions.eq("mahasiswa", mahasiswa)).uniqueResult();

		System.out.println("mahasiswaDaftarKkn = " + mahasiswaDaftarKkn);

		if (mahasiswaDaftarKkn == null) {
			mahasiswaDaftarKkn = new MahasiswaDaftarKkn();
			mahasiswaDaftarKkn.setTanggalDaftar(ais.ui.util.WaktuUtil.getDate());
			mahasiswaDaftarKkn.setTerima(MahasiswaDaftarKkn.BELUM_DIPROSES);
		}

		mahasiswaDaftarKkn.setMemenuhiSyarat(memenuhiSyarat);
		mahasiswaDaftarKkn.setNama(mahasiswa + "-->" + kkn.getNama());
		mahasiswaDaftarKkn.setMahasiswa(mahasiswa);
		mahasiswaDaftarKkn.setKkn(kkn);

		List<MahasiswaKknPersyaratan> mahasiswaKknPersyaratans = session.createCriteria(MahasiswaKknPersyaratan.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("kkn", kkn))
				.createAlias("persyaratanKkn", "persyaratanKkn")
				.add(Restrictions.eq("persyaratanKkn.tipeDataInputan", PersyaratanKkn.PILIHAN_CUSTOM)).list();
		Integer totalSkor = 0;
		for (MahasiswaKknPersyaratan mahasiswaKknPersyaratan : mahasiswaKknPersyaratans) {
			String val = mahasiswaKknPersyaratan.getNilaiString() == null ? ""
					: mahasiswaKknPersyaratan.getNilaiString().trim();
			String[] kol = StringUtils.split(val, ":");
			// String a = kol[0];
			Integer skor = 0;
			// Guard: nilai bisa tanpa ":" → kol.length < 2 → hindari ArrayIndexOutOfBounds.
			if (kol != null && kol.length >= 2) {
				try {
					skor = Integer.parseInt(kol[1].trim());
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
			totalSkor += skor;
		}
		mahasiswaDaftarKkn.setTotalSkor(totalSkor);

		Common.refreshSaveOrUpdate(session, mahasiswaDaftarKkn);

		MyMessageboxConfig.show("Mahasiswa dengan NIM " + mahasiswa.getNim() + " berhasil terdaftar di kkn ini",
				"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		return true;

	}

	private Boolean memenuhiSyarat = true;
	private EventListener eventListener;

	public static void onAddExternal(Event event, EventListener eventListener, Kkn kkn, Mahasiswa mahasiswa)
			throws Exception {
		KknUntukMahasiswaAction kknUntukMahasiswaAction = new KknUntukMahasiswaAction();
		kknUntukMahasiswaAction.mahasiswa = mahasiswa;
		kknUntukMahasiswaAction.eventListener = eventListener;
		kknUntukMahasiswaAction.daftar_kkn_window = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(kknUntukMahasiswaAction.daftar_kkn_window);
		kknUntukMahasiswaAction.daftar_kkn_window.setHeight("95%");
		kknUntukMahasiswaAction.daftar_kkn_window.setWidth("90%");
		kknUntukMahasiswaAction.daftar_kkn_window.setVisible(true);
		kknUntukMahasiswaAction.daftar_kkn_window.setClosable(true);
		kknUntukMahasiswaAction.daftar_kkn_window.onModal();

		kknUntukMahasiswaAction.init(kkn, false);

	}

	public static Row tampilkanPersyaratan(final PersyaratanKkn persyaratan,
			final MahasiswaKknPersyaratan temPersyaratan, Label labelLama, Rows rows, List<Component> components,
			boolean tampil, List<PersyaratanKkn> persyaratanKknsKomponen) {

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		Label check = new Label();
		if (labelLama.getValue().isEmpty() || !labelLama.getValue().equalsIgnoreCase(persyaratan.getNama())) {
			check.setValue(persyaratan.getNama());
		}
		row.appendChild(check);

		final Component component;
		if (persyaratan.getTipeDataInputan().equals(PersyaratanKkn.TEXT)) {
			component = new Textbox(temPersyaratan == null ? null : temPersyaratan.getNilaiString());
			((Textbox) component).setWidth("90%");
			((Textbox) component).focus();
		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanKkn.TANGGAL)) {
			component = new MyDatebox(temPersyaratan == null ? null : temPersyaratan.getNilaiTanggal());

			((MyDatebox) component).focus();
		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanKkn.ANGKA)) {
			component = new MyDoublebox(temPersyaratan == null ? null : temPersyaratan.getNilaiNumber());

		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanKkn.TEXT_ANGKA)) {
			component = new MyTextboxAngka(temPersyaratan == null ? null : temPersyaratan.getNilaiString());
			((Textbox) component).setWidth("90%");
			((Textbox) component).focus();
		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanKkn.PILIHAN_YA_TIDAK)) {
			component = new Combobox();
			MyComboitemConfig comboitem = new MyComboitemConfig("Ya");
			comboitem.setValue(true);
			component.appendChild(comboitem);
			comboitem = new MyComboitemConfig("Tidak");
			comboitem.setValue(false);
			component.appendChild(comboitem);
			((Combobox) component).setReadonly(true);

			Common.selectComboItem(((Combobox) component),
					temPersyaratan == null ? null : temPersyaratan.getNilaiBoolean());
		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanKkn.PILIHAN_CUSTOM)) {
			component = new Combobox();
			String[] ss = StringUtils.split(persyaratan.getNilaiDataInputan(), ";");
			Arrays.sort(ss);
			for (String s : ss) {
				String[] kol = StringUtils.split(s, ":");
				String a = kol[0];
				Integer skor = 0;
				try {
					if (kol.length > 1) skor = Integer.parseInt(kol[1].trim());
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				MyComboitemConfig comboitem = new MyComboitemConfig(a);
				comboitem.setAttribute("skor", skor);
				comboitem.setValue(s);
				component.appendChild(comboitem);
			}
			((Combobox) component).setReadonly(true);

			Common.selectComboItem(((Combobox) component),
					temPersyaratan == null ? null : temPersyaratan.getNilaiString());
		} else {
			component = null;
		}

		final Label label = new Label(persyaratan.getLabelInputan() + (persyaratan.getHarusDiisi() ? " (*)" : ""));
		final Hbox hbox = new Hbox();
		hbox.setStyle("border:0px;background: transparent;");
		if (temPersyaratan != null)
			temPersyaratan.setStatus(true);
		hbox.setVisible(temPersyaratan == null ? true : temPersyaratan.getStatus());

		if (component != null) {
			persyaratanKknsKomponen.add(persyaratan);
			component.setAttribute("persyaratan", persyaratan);
			components.add(component);

			row.appendChild(label);
			label.setWidth("97%");
			if (persyaratan.getHarusDiisi()) {
				if (component instanceof Textbox) {
//					((Textbox) component).setConstraint("no empty");
				} else if (component instanceof MyDatebox) {
//					((MyDatebox) component).setConstraint("no empty");
				} else if (component instanceof MyDoublebox) {
//					((MyDoublebox) component).setConstraint("no empty");
				} else if (component instanceof Combobox) {
//					((Combobox) component).setConstraint("no empty");
				}
			}

			component.setVisible(tampil && (temPersyaratan == null ? true : temPersyaratan.getStatus()));
			label.setVisible(temPersyaratan == null ? true : temPersyaratan.getStatus());

			Vbox vbox = new Vbox();
			row.appendChild(vbox);
			vbox.appendChild(component);
			row.setValign("top");
			row.setAttribute("component", component);
			component.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (temPersyaratan != null && temPersyaratan.getId() != null) {
						if (persyaratan.getTipeDataInputan().equals(PersyaratanKkn.TEXT)) {
							temPersyaratan.setNilaiString(((Textbox) component).getValue());
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.PILIHAN_CUSTOM)) {
							temPersyaratan
									.setNilaiString((String) (((Combobox) component).getSelectedItem() == null ? ""
											: (((Combobox) component).getSelectedItem().getValue())));
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanKkn.TANGGAL)) {
							temPersyaratan.setNilaiTanggal(((MyDatebox) component).getValue());
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanKkn.ANGKA)) {
							temPersyaratan.setNilaiNumber(((MyDoublebox) component).getValue());
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanKkn.TEXT_ANGKA)) {
							temPersyaratan.setNilaiString(((MyTextboxAngka) component).getValue());
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanKkn.PILIHAN_YA_TIDAK)) {
							// Guard: bila belum ada pilihan dipilih, getSelectedItem() null → NPE.
							org.zkoss.zul.Comboitem siYaTidak = ((Combobox) component).getSelectedItem();
							temPersyaratan.setNilaiBoolean(siYaTidak == null ? null : (Boolean) siYaTidak.getValue());
						}

						Common.refreshUpdate(temPersyaratan);
					}
				}
			});

			if (persyaratan.getHarusMenyertakanLampiran()) {
				Common.createDownloadUploadFileLampiranKkn(vbox, temPersyaratan, persyaratan.getLabelInputan());
			}
		} else {
			/*
			 * KOLOM ISIAN KOSONG. component == null terjadi bila tipeDataInputan persyaratan TIDAK
			 * dikenali -- termasuk saat admin BELUM mengatur tipenya, karena getTipeDataInputan()
			 * memberi default "java.lang.String" yang tak cocok dengan satu pun tipe (Berupa teks /
			 * angka / tanggal / pilihan). Dulu SELURUH isi baris (label + input + tombol unggah)
			 * ikut dilewati sehingga kolom kanan tampak KOSONG tanpa penjelasan apa pun.
			 *
			 * Sekarang: bila persyaratan memang wajib melampirkan berkas, tombol unggah TETAP
			 * ditampilkan (persyaratan "unggah saja"). Bila tidak, ditampilkan pesan JELAS bahwa
			 * pengaturan tipe isian belum lengkap dan perlu dilengkapi admin -- bukan lagi kosong.
			 */
			final Label label2 = new Label(
					persyaratan.getLabelInputan() + (persyaratan.getHarusDiisi() ? " (*)" : ""));
			label2.setWidth("97%");
			row.appendChild(label2);

			Vbox vbox2 = new Vbox();
			row.appendChild(vbox2);
			row.setValign("top");

			if (persyaratan.getHarusMenyertakanLampiran()) {
				Common.createDownloadUploadFileLampiranKkn(vbox2, temPersyaratan, persyaratan.getLabelInputan());
			} else {
				String namaAman = persyaratan.getNama() == null ? "" : persyaratan.getNama()
						.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
				vbox2.appendChild(new ais.ui.util.MyHtml(
						"<div style='color:#b91c1c;font-size:11px;line-height:1.4;padding:5px 8px;"
								+ "border:1px solid #fecaca;border-radius:6px;background:#fef2f2;'>"
								+ "<b>Kolom isian belum dapat ditampilkan.</b><br/>"
								+ "Jenis isian untuk persyaratan \"" + namaAman + "\" belum diatur oleh admin "
								+ "(mis. berupa teks, angka, tanggal, pilihan, atau wajib melampirkan berkas). "
								+ "Mohon menghubungi admin/pengelola KKN agar melengkapi pengaturan persyaratan "
								+ "ini terlebih dahulu, sehingga mahasiswa dapat mengisinya.</div>"));
			}
		}

		if (labelLama.getValue().isEmpty() || !labelLama.getValue().equalsIgnoreCase(persyaratan.getNama())) {
			labelLama.setValue(persyaratan.getNama());
		}

		return row;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(final Kkn kkn, Boolean hanyaLihat) throws Exception {

		daftar_kkn_window.setTitle("Pendataan Persyaratan Kkn");
		memenuhiSyarat = true;

		Common.clear(daftar_kkn_window);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		West west = new West();
		west.setParent(borderlayout);
		west.setWidth("30%");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendaftaran KKN"));
		row.appendChild(new ais.ui.util.MyLabelConfig(kkn.getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendaftaran Dimulai"));
		row.appendChild(
				new Label(kkn.getTanggal_mulai() == null ? "" : Common.dateFormat4.get().format(kkn.getTanggal_mulai())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendaftaran Ditutup"));
		row.appendChild(
				new Label(kkn.getTanggal_selesai() == null ? "" : Common.dateFormat4.get().format(kkn.getTanggal_selesai())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM Mahasiswa"));
		row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswa.getNim()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswa.getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(
				new ais.ui.util.MyLabelConfig(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getNama()));

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Borderlayout myborderlayout = new ais.ui.util.MyBorderlayout();
		myborderlayout.setParent(center);

		center = new Center();
		center.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setStyle("border:0px");
		grid.setWidth("100%");
		grid.setHeight("110%");

		columns = new Columns();
		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("25%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);

		rows = new Rows();
		rows.setParent(grid);

		row = new MyFormRow();
		row.setStyle("border:0px;background: #8dcff4;");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Persyaratan KKN"));
		ais.ui.util.ZkCompat.setSpans(row, "3");

		row = new MyFormRow();
		row.setStyle("border:0px;background: #8dcff4;");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyHtml("<hr>"));
		ais.ui.util.ZkCompat.setSpans(row, "3");

		Session session = HibernateUtil.currentSession();
		final List<PersyaratanKkn> persyaratanKkns = session.createCriteria(KknPunyaPersyaratan.class)
				.createAlias("persyaratanKkn", "persyaratanKkn")
				.add(Restrictions
						.or(Restrictions.isNull("persyaratanKkn.aktif"), Restrictions.eq("persyaratanKkn.aktif", true)))
				.add(Restrictions.eq("kkn", kkn))
				.add(Restrictions.or(Restrictions.eq("persyaratanKkn.jenisKelamin", ""),
						Restrictions.ilike("persyaratanKkn.jenisKelamin", mahasiswa.getKelamin(), MatchMode.ANYWHERE)))
				.setProjection(Projections.property("persyaratanKkn")).addOrder(Order.asc("persyaratanKkn.nama"))
				.addOrder(Order.asc("persyaratanKkn.labelInputan")).list();

		System.out.println("persyaratanKkns = " + persyaratanKkns.size());
		final List<PersyaratanKkn> persyaratanKknsKomponen = new ArrayList<PersyaratanKkn>();
		Label labelLama = new Label("");
		final List<Component> components = new ArrayList<Component>();
		for (final PersyaratanKkn persyaratan : persyaratanKkns) {

			MahasiswaKknPersyaratan mahasiswaKknPersyaratan = (MahasiswaKknPersyaratan) session
					.createCriteria(MahasiswaKknPersyaratan.class).add(Restrictions.eq("mahasiswa", mahasiswa))
					.setMaxResults(1).addOrder(Order.desc("id")).add(Restrictions.eq("kkn", kkn))
					.add(Restrictions.eq("persyaratanKkn", persyaratan)).uniqueResult();
			if (mahasiswaKknPersyaratan == null) {
				mahasiswaKknPersyaratan = new MahasiswaKknPersyaratan();
				mahasiswaKknPersyaratan.setMahasiswa(mahasiswa);
				mahasiswaKknPersyaratan.setKkn(kkn);
				mahasiswaKknPersyaratan.setPersyaratanKkn(persyaratan);
				session.save(mahasiswaKknPersyaratan);
			}

			KknUntukMahasiswaAction.tampilkanPersyaratan(persyaratan, mahasiswaKknPersyaratan, labelLama, rows,
					components, true, persyaratanKknsKomponen);
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/svg/check2-circle.svg");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String[] nims = kkn.getNimMhsTanpaBiaya().split(",");
				boolean ada = false;
				for (String nim : nims) {
					if (mahasiswa != null && mahasiswa.getNim() != null
							&& mahasiswa.getNim().trim().equalsIgnoreCase(nim.trim())) {
						ada = true;
						break;
					}
				}

				if (!ada) {
					if (!kkn.getKodeItemBiaya().trim().isEmpty()) {

						Session session = HibernateUtil.currentSession();
						for (String kode : kkn.getKodeItemBiaya().trim().split(",")) {
							ItemBiaya itemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
									.add(Restrictions.eq("kode", kode.trim())).setMaxResults(1).uniqueResult();
							if (itemBiaya != null) {
								int jumlah = ((Number) session.createCriteria(CicilanPembayaran.class)
										.createAlias("kegiatan", "kegiatan")
										.add(Restrictions.eq("itemBiaya", itemBiaya))
										.add(Restrictions.eq("kegiatan.mahasiswa", mahasiswa))
										.setProjection(Projections.rowCount()).uniqueResult()).intValue();
								if (jumlah == 0) {
									MyMessageboxConfig.show(
											"Mahasiswa dengan NIM " + mahasiswa.getNim() + " dan nama "
													+ mahasiswa.getNama() + " belum membayar biaya "
													+ itemBiaya.getKode() + "-" + itemBiaya.getNama()
													+ ". Harap menghubungi bagian keuangan untuk melakukan pembayaran.",
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									daftar_kkn_window.setVisible(false);
									return;
								}
							}
						}

					}

					if (kkn.getHarusBayar()) {

						Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(), kkn.getTahunAkademik(),
								kkn.getSemester(), mahasiswa.getPindahKeKampusIniMasukSemester(),
								mahasiswa.getSemesterMulai());

						if (!Common.checkStatusPembayaranMahasiswa(smt, mahasiswa.currentTahapan(), mahasiswa, false,
								false)) {
							MyMessageboxConfig.show("Mahasiswa dengan NIM " + mahasiswa.getNim()
									+ " tidak diperkenankan mendaftar, karena belum membayar biaya perkuliahan semester "
									+ smt + " "
									+ (mahasiswa.currentTahapan() != null && mahasiswa.currentTahapan() > 0
											? " tahap " + mahasiswa.currentTahapan()
											: ""),
									"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							daftar_kkn_window.setVisible(false);
							return;
						}
					}
				}

				if (!Common.checkSyaratKkn(mahasiswa, kkn)) {
					return;
				}
				if (Common.bolehKonfigurasi("jika_sudah_dapat_kkn_mahasiswa_tidak_boleh_mengajukan_kkn")) {
					int jumlah = ((Number) HibernateUtil.currentSession().createCriteria(MahasiswaDaftarKkn.class)
							.add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(kkn.getId() != null ? Restrictions.ne("kkn", kkn)
									: Restrictions.sqlRestriction("true"))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					if (jumlah > 0) {
						MyMessageboxConfig.show("Mahasiswa dengan NIM " + mahasiswa.getNim()
								+ " tidak diperkenankan mendaftar kkn ini, karena sudah pernah mendaftarakan KKN di tempat lain",
								"INFORMATION", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
				}

				if (!memenuhiSyarat) {
					MyMessageboxConfig.show("Mohon maaf, data pendaftaran KKN tidak memenuhi syarat yang ditentukan. Langkah yang dapat dilakukan: (1) periksa kembali semua persyaratan yang wajib dipenuhi untuk KKN ini; (2) pastikan nilai, jumlah SKS, dan dokumen persyaratan telah sesuai dengan ketentuan; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				for (final Component component : components) {

					if (component.getAttribute("persyaratan") != null) {
						PersyaratanKkn persyaratan = (PersyaratanKkn) component.getAttribute("persyaratan");
						if (!persyaratan.getHarusDiisi()) {
							continue;
						}
					}

					if (component instanceof Textbox && ((Textbox) component).getValue().trim().isEmpty()) {
						MyMessageboxConfig.show("Mohon maaf, masih terdapat persyaratan KKN yang belum diisi. Langkah yang dapat dilakukan: (1) pastikan semua kolom persyaratan KKN telah diisi dengan lengkap dan benar; (2) periksa setiap kolom teks yang masih kosong; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										((Textbox) component).focus();
									}
								});
						return;
					} else if (component instanceof MyDatebox && ((MyDatebox) component).getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, masih terdapat persyaratan KKN yang belum diisi. Langkah yang dapat dilakukan: (1) pastikan semua kolom persyaratan KKN telah diisi dengan lengkap dan benar; (2) periksa setiap kolom tanggal yang masih kosong; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										((MyDatebox) component).focus();
									}
								});
						return;
					} else if (component instanceof MyDoublebox && ((MyDoublebox) component).getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, masih terdapat persyaratan KKN yang belum diisi. Langkah yang dapat dilakukan: (1) pastikan semua kolom persyaratan KKN telah diisi dengan lengkap dan benar; (2) periksa setiap kolom angka yang masih kosong; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										((MyDoublebox) component).focus();
									}
								});
						return;
					} else if (component instanceof Combobox && ((Combobox) component).getSelectedItem() == null) {
						MyMessageboxConfig.show("Semua persyaratan harus diisi", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										((Combobox) component).focus();
									}
								});
						return;
					}
				}

//				Session session = HibernateUtil.currentSession();
//				for (final PersyaratanKkn persyaratan : persyaratanKknsKomponen) {
//					if (persyaratan.getHarusMenyertakanLampiran()) {
//						MahasiswaKknPersyaratan mahasiswaKknPersyaratan = (MahasiswaKknPersyaratan) session
//								.createCriteria(MahasiswaKknPersyaratan.class)
//								.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("kkn", kkn))
//								.addOrder(Order.desc("id")).setMaxResults(1)
//								.add(Restrictions.eq("persyaratanKkn", persyaratan)).uniqueResult();
//						if (mahasiswaKknPersyaratan == null) {
//							mahasiswaKknPersyaratan = new MahasiswaKknPersyaratan();
//							mahasiswaKknPersyaratan.setMahasiswa(mahasiswa);
//							mahasiswaKknPersyaratan.setKkn(kkn);
//							mahasiswaKknPersyaratan.setPersyaratanKkn(persyaratan);
//							session.save(mahasiswaKknPersyaratan);
//						}
//
//						FileFotoLain fileFotoLain = FileFotoLain.ambil(mahasiswaKknPersyaratan.getId(),
//								LampiranKknMahasiswa.DEFAULT_JENIS, LampiranKknMahasiswa.class, true);
//						if (fileFotoLain == null) {
//							MyMessageboxConfig.show("\"" + persyaratan.getNama() + "\" harus di upload", "Informasi",
//									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
//							return;
//						}
//
//					}
//				}

				if (daftar(kkn)) {
					onSearchDefault(null);
					daftar_kkn_window.setVisible(false);

					if (eventListener != null) {
						eventListener.onEvent(null);
					}
				}
			}
		});
		save.setParent(toolbar);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				daftar_kkn_window.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		borderlayout.setParent(daftar_kkn_window);
		daftar_kkn_window.setVisible(true);
		daftar_kkn_window.onModal();

		if (hanyaLihat) {
			Common.freeze(daftar_kkn_window, true);
			save.setVisible(false);
			cancel.setDisabled(false);
		}

	}
}
