package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import ais.action.master.prestasi.DasbordPrestasi;
import java.util.ArrayList;
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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
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

import ais.action.master.dashboard.sekolah.DashboardRekapPrestasiSiswaBerdasarCabang;
import ais.action.master.dashboard.sekolah.DashboardRekapPrestasiSiswaBerdasarKategori;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CabangPrestasiSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KategoriPrestasiSiswa;
import ais.database.model.sekolah.PrestasiSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk prestasi siswa. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchpenyelenggara}, {@code
 * AmbilDataSiswaBanbox searchsiswa}, {@code MyDatebox searchmulai}, {@code MyDatebox searchsampai};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi
 * domain lain ({@code onDasbor()}, {@code onKategoriPrestasiSiswa()}, {@code onCabangPrestasiSiswa()}, {@code
 * onRekapCabang()}, {@code onRekapKategori()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas
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
public class PrestasiSiswaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchpenyelenggara;
	private AmbilDataSiswaBanbox searchsiswa;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;
	private Combobox searchstatus;
	private Combobox searchta;
	private Combobox searchsekolah;
	private Combobox searchyayasan;
	private Combobox searchcabangPrestasiSiswa;
	private Combobox searchkategoriPrestasiSiswa;
	protected AmbilDataGuruBanbox searchguru;

	private Textbox nama;
	private MyDatebox tanggal;
	private MyDatebox tanggalSelesai;
	private AmbilDataSiswaBanbox siswa;
	private Combobox sekolah;
	private Combobox yayasan;
	private Checkbox prestasiLuarKampus;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Textbox keterangan;

	private PrestasiSiswa prestasiSiswa;
	private MyToolbarbuttonConfig add;

	protected LampiranLain lainSiswa;
	private Tbmuser tbmuser;
	private Textbox tempat;
	private Textbox juara;
	private Intbox peringkat;
	private Textbox penyelenggara;
	private Textbox nomorSertifikat;

	private Combobox cabangPrestasiSiswa;
	private Combobox kategoriPrestasiSiswa;
	private Textbox jumlahPeserta;
	private Textbox capaian;
	private Textbox url;

	private MyToolbarbuttonConfig uploadData;

	private Siswa mhs;
	private Row rowYayasan;
	private Row rowSekolah;

	private Tabpanel tabDasbor;

	public void onDasbor(Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordPrestasi dasbord = new DasbordPrestasi(DasbordPrestasi.Lingkup.SISWA);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Prestasi Siswa",
				"Pencapaian dan penghargaan yang diraih siswa.");
		}
	}

	private Tabpanel kategoriPrestasiSiswaTab;

	public void onKategoriPrestasiSiswa(Event event) {
		if (kategoriPrestasiSiswaTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kategoriPrestasiSiswaTab);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/kategori_prestasi_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel cabangPrestasiSiswaTab;

	public void onCabangPrestasiSiswa(Event event) {
		if (cabangPrestasiSiswaTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(cabangPrestasiSiswaTab);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/cabang_prestasi_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel cabangRekapTab;

	public void onRekapCabang(Event event) {
		if (cabangRekapTab.getChildren().size() == 0) {
			DashboardRekapPrestasiSiswaBerdasarCabang window = new DashboardRekapPrestasiSiswaBerdasarCabang();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, cabangRekapTab,
				"Rekap per Cabang", "Sebaran prestasi siswa berdasarkan cabang ilmu atau bidang kompetisi.");
		}
	}

	private Tabpanel kategoriRekapTab;
	private Intbox tahun;

	public void onRekapKategori(Event event) {
		if (kategoriRekapTab.getChildren().size() == 0) {
			DashboardRekapPrestasiSiswaBerdasarKategori window = new DashboardRekapPrestasiSiswaBerdasarKategori();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, kategoriRekapTab,
				"Rekap per Kategori", "Sebaran prestasi siswa berdasarkan kategori kompetisi atau penghargaan.");
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private MyColumnConfig colNama;
	private PrestasiSiswa prestasiSiswaSelected = null;
	private Textbox namaEn;
	private Textbox alamat;
	private Textbox noSk;
	private MyDatebox tglSk;
	private AmbilDataGuruBanbox guru;
	private AmbilDataGuruBanbox guru2;
	private AmbilDataGuruBanbox guru3;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.getSiswa() != null && colNama != null) {
			colNama.setWidth("0px");
		}

		Common.generateTahunAjaranDanSemua(searchta);
		Common.selectComboItem(searchta, null);

		kategoriPrestasiSiswaTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.ambilGuru() == null);
		cabangPrestasiSiswaTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.ambilGuru() == null);
		cabangRekapTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.ambilGuru() == null);

		kategoriRekapTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.ambilGuru() == null);

		searchguru.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(KategoriPrestasiSiswa.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			KategoriPrestasiSiswa kategoriPrestasiSiswa = new KategoriPrestasiSiswa();
			kategoriPrestasiSiswa.setNama("Internasional");
			session.save(kategoriPrestasiSiswa);

			kategoriPrestasiSiswa = new KategoriPrestasiSiswa();
			kategoriPrestasiSiswa.setNama("Nasional");
			session.save(kategoriPrestasiSiswa);

			kategoriPrestasiSiswa = new KategoriPrestasiSiswa();
			kategoriPrestasiSiswa.setNama("Regional");
			session.save(kategoriPrestasiSiswa);
		}
		count = ((Number) session.createCriteria(KategoriPrestasiSiswa.class).add(Restrictions.eq("nama", "Kab/Kota"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {
			KategoriPrestasiSiswa kategoriPrestasiSiswa = new KategoriPrestasiSiswa();
			kategoriPrestasiSiswa.setNama("Kab/Kota");
			session.save(kategoriPrestasiSiswa);

			kategoriPrestasiSiswa = new KategoriPrestasiSiswa();
			kategoriPrestasiSiswa.setNama("Kecamatan");
			session.save(kategoriPrestasiSiswa);

			kategoriPrestasiSiswa = new KategoriPrestasiSiswa();
			kategoriPrestasiSiswa.setNama("Kampus/Sekolah");
			session.save(kategoriPrestasiSiswa);

			kategoriPrestasiSiswa = new KategoriPrestasiSiswa();
			kategoriPrestasiSiswa.setNama("Lain-Lain");
			session.save(kategoriPrestasiSiswa);
		}

		count = ((Number) session.createCriteria(CabangPrestasiSiswa.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			CabangPrestasiSiswa cabangPrestasiSiswa = new CabangPrestasiSiswa();
			cabangPrestasiSiswa.setNama("Seni");
			session.save(cabangPrestasiSiswa);

			cabangPrestasiSiswa = new CabangPrestasiSiswa();
			cabangPrestasiSiswa.setNama("Olah Raga");
			session.save(cabangPrestasiSiswa);

			cabangPrestasiSiswa = new CabangPrestasiSiswa();
			cabangPrestasiSiswa.setNama("Kejuaraan Ilmiah");
			session.save(cabangPrestasiSiswa);

		}

		count = ((Number) session.createCriteria(CabangPrestasiSiswa.class).add(Restrictions.eq("nama", "Lain-Lain"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {
			CabangPrestasiSiswa cabangPrestasiSiswa = new CabangPrestasiSiswa();
			cabangPrestasiSiswa.setKode("9");
			cabangPrestasiSiswa.setNama("Lain-Lain");
			session.save(cabangPrestasiSiswa);
		}

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		Common.insertComboDanSemua(searchkategoriPrestasiSiswa, "nama", KategoriPrestasiSiswa.class);
		Common.insertComboDanSemua(searchcabangPrestasiSiswa, "nama", CabangPrestasiSiswa.class);

		Comboitem comboitem = new Comboitem(PrestasiSiswa.BELUM_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PrestasiSiswa.BELUM_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PrestasiSiswa.SEDANG_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PrestasiSiswa.SEDANG_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PrestasiSiswa.DISETUJUI);
		if (comboitem != null) { comboitem.setValue(PrestasiSiswa.DISETUJUI); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PrestasiSiswa.DITOLAK);
		if (comboitem != null) { comboitem.setValue(PrestasiSiswa.DITOLAK); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchstatus.appendChild(comboitem);
		if (searchstatus != null) { searchstatus.setReadonly(true); }
		if (searchstatus != null) { searchstatus.setSelectedItem(comboitem); }

		searchsiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (execution.getParameter("siswa") != null) {
			mhs = (Siswa) HibernateUtil.currentSession().createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("siswa")))).uniqueResult();
		} else {
			mhs = tbmuser == null ? null : tbmuser.getSiswa();
		}

		if (execution.getParameter("prestasi") != null) {
			prestasiSiswaSelected = (PrestasiSiswa) GeneralValueObject.ambilData(PrestasiSiswa.class,
					execution.getParameter("prestasi").toString());
		}

		if (execution.getParameter("sekolah") != null) {
			Sekolah sekolahSelected = (Sekolah) HibernateUtil.currentSession().createCriteria(Sekolah.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("sekolah")))).uniqueResult();
			Common.selectComboItem(true, searchsekolah, sekolahSelected);

			Common.selectComboItem(true, searchyayasan, sekolahSelected == null ? null : sekolahSelected.getYayasan());

		}

		if (execution.getParameter("cabangPrestasiSiswa") != null) {
			CabangPrestasiSiswa cabangPrestasiSiswaSelected = (CabangPrestasiSiswa) HibernateUtil.currentSession()
					.createCriteria(CabangPrestasiSiswa.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("cabangPrestasiSiswa"))))
					.uniqueResult();
			Common.selectComboItem(true, searchcabangPrestasiSiswa, cabangPrestasiSiswaSelected);
		}

		if (execution.getParameter("kategoriPrestasiSiswa") != null) {
			KategoriPrestasiSiswa kategoriPrestasiSiswaSelected = (KategoriPrestasiSiswa) HibernateUtil.currentSession()
					.createCriteria(KategoriPrestasiSiswa.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("kategoriPrestasiSiswa"))))
					.uniqueResult();
			Common.selectComboItem(true, searchkategoriPrestasiSiswa, kategoriPrestasiSiswaSelected);
		}

		if (execution.getParameter("tahunAjaran") != null) {
			String tahunAjaran = execution.getParameter("tahunAjaran");
			Common.selectComboItem(true, searchta, tahunAjaran);
		}

		if (mhs != null) {
			searchsiswa.setAttribute("siswa", mhs);
			searchsiswa.setDisabled(true);
			searchsiswa.setValue(mhs.getNama());
		}

		if (add != null) { add.setVisible(tbmuser != null); }

		// add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE)
		// &&
		// tbmuser.getSiswa() == null
		// && tbmuser.ambilGuru() == null);
		// add.setTooltiptext("Tambah");

		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onDasbor(null);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "siswa", "guru", "guru2", "guru3", "nama", "namaEn", "tempat",
				"penyelenggara", "juara", "peringkat", "tanggal", "tanggalSelesai", "nomorSertifikat",
				"cabangPrestasiSiswa", "kategoriPrestasiSiswa", "jumlahPeserta", "capaian", "url", "yayasan", "sekolah",
				"tahunAkademik", "jenisSemester", "tahun", "status", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PrestasiSiswa.class, contents);
		upload.setVisible(
				(add != null && add.isVisible()) && tbmuser != null && tbmuser.getSiswa() == null && tbmuser.ambilGuru() == null);
		Common.appendKeToolbar(upload, add, comp);

		if (uploadData != null) { uploadData.setVisible(upload.isVisible()); }

		if (mhs != null) {

		}

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PrestasiSiswaAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PrestasiSiswaAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PrestasiSiswaAction
	 */
	class PrestasiSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PrestasiSiswa prestasiSiswa = (PrestasiSiswa) arg1;

			try {
				if (prestasiSiswaSelected != null && prestasiSiswaSelected.getId().equals(prestasiSiswa.getId())) {
					arg0.setStyle("background-color:yellow");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PrestasiSiswaAction.java:437");
				// TODO: handle exception
			}

			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);

			Vbox myvbox = new Vbox();
			myvbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(prestasiSiswa.getSiswa()).setParent(myvbox);

			new Label(prestasiSiswa.getSiswa().getNim() + "-" + prestasiSiswa.getSiswa().getNama()).setParent(myvbox);

			Vbox a = RevisiHelper.createNewRevisi(PrestasiSiswa.class, prestasiSiswa, prestasiSiswa.getNama());

			new Label(prestasiSiswa.getNamaEn()).setParent(a);

			a.setParent(arg0);

			myvbox = new Vbox();
			myvbox.setParent(detail);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, prestasiSiswa.getId(), PrestasiSiswa.class.getName(),
					"Lampiran", false, null, null, false, false, false, false);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new MyLabelAgakKecil("Tempat: " + prestasiSiswa.getTempat()).setParent(myvbox);
			new MyLabelAgakKecil("Penyelenggara: " + prestasiSiswa.getPenyelenggara()).setParent(myvbox);
			new MyLabelAgakKecil("Juara: " + prestasiSiswa.getJuara()).setParent(myvbox);
			new MyLabelAgakKecil(
					"Peringkat: " + (prestasiSiswa.getPeringkat() == null ? "" : prestasiSiswa.getPeringkat()))
					.setParent(myvbox);
			new MyLabelAgakKecil("Tanggal: "
					+ (prestasiSiswa.getTanggal() == null ? "" : Common.dateFormat1.get().format(prestasiSiswa.getTanggal()))
					+ (prestasiSiswa.getTanggalSelesai() == null ? ""
							: " s.d " + Common.dateFormat1.get().format(prestasiSiswa.getTanggalSelesai())))
					.setParent(myvbox);
			new MyLabelAgakKecil("TA/Smt: " + prestasiSiswa.getTahunAkademik() + "/" + prestasiSiswa.getJenisSemester())
					.setParent(myvbox);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new MyLabelAgakKecil("Cabang: " + (prestasiSiswa.getCabangPrestasiSiswa() == null ? ""
					: prestasiSiswa.getCabangPrestasiSiswa().getNama())).setParent(myvbox);
			new MyLabelAgakKecil("Kategori: " + (prestasiSiswa.getKategoriPrestasiSiswa() == null ? ""
					: prestasiSiswa.getKategoriPrestasiSiswa().getNama())).setParent(myvbox);
			new MyLabelAgakKecil("Jml Peserta: " + prestasiSiswa.getJumlahPeserta()).setParent(myvbox);
			new MyLabelAgakKecil("Link: " + prestasiSiswa.getUrl()).setParent(myvbox);

			new Label(prestasiSiswa.getCapaian()).setParent(arg0);

			new Label(prestasiSiswa.getNomorSertifikat()).setParent(arg0);
			// Kolom aksi rapi: semua tombol dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi.
			// aksiBoxRef menampung Vbox pembungkus supaya visibilitas grup tetap bisa
			// diubah dari listener Combobox status (perilaku sama dgn Hbox toolbar lama).
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			final Vbox[] aksiBoxRef = new Vbox[1];
			final MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
					"/img/Finance-Invoice-icon.png");
			final Hbox myHbox = new Hbox();
			myHbox.setVisible(prestasiSiswa.getStatus().equals(PrestasiSiswa.DISETUJUI));
			if (mhs == null && tbmuser != null) {
				final Combobox status = new Combobox();
				Comboitem comboitem = new Comboitem(PrestasiSiswa.BELUM_DIPROSES);
				comboitem.setValue(PrestasiSiswa.BELUM_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PrestasiSiswa.SEDANG_DIPROSES);
				comboitem.setValue(PrestasiSiswa.SEDANG_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PrestasiSiswa.DISETUJUI);
				comboitem.setValue(PrestasiSiswa.DISETUJUI);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PrestasiSiswa.DITOLAK);
				comboitem.setValue(PrestasiSiswa.DITOLAK);
				status.appendChild(comboitem);

				Common.selectComboItem(status, prestasiSiswa.getStatus());
				status.setParent(arg0);
				status.setReadonly(true);
				status.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						prestasiSiswa.setStatus((String) (status.getSelectedItem() == null
								|| status.getSelectedItem().getValue() == null ? null
										: status.getSelectedItem().getValue()));
						Common.refreshUpdate(prestasiSiswa);
						if (aksiBoxRef[0] != null) {
							aksiBoxRef[0].setVisible(!prestasiSiswa.getStatus().equals(PrestasiSiswa.DISETUJUI));
						}

						if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
								&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
							buttonTagihan.setVisible(prestasiSiswa.getStatus().equals(PrestasiSiswa.DISETUJUI));
						}
						myHbox.setVisible(prestasiSiswa.getStatus().equals(PrestasiSiswa.DISETUJUI));
					}
				};
				status.addEventListener("onChange", eventListener);
			} else {
				new Label(prestasiSiswa.getStatus()).setParent(arg0);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil("Luar kampus ? " + (prestasiSiswa.getPrestasiLuarKampus() ? "Ya" : "Tidak"))
					.setParent(vbox);
			if (!prestasiSiswa.getPrestasiLuarKampus() && prestasiSiswa.getYayasan() != null)
				new MyLabelAgakKecil(
						prestasiSiswa.getYayasan() == null ? "Semua" : prestasiSiswa.getYayasan().getNama())
						.setParent(vbox);
			if (!prestasiSiswa.getPrestasiLuarKampus() && prestasiSiswa.getSekolah() != null)
				new MyLabelAgakKecil(
						prestasiSiswa.getSekolah() == null ? "Semua" : prestasiSiswa.getSekolah().getNama())
						.setParent(vbox);

			new Label(prestasiSiswa.getKeterangan()).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(prestasiSiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			// button.setVisible(delete);
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

											if (prestasiSiswaSelected != null
													&& prestasiSiswaSelected.getId().equals(prestasiSiswa.getId())) {
												prestasiSiswaSelected = null;
											}

											Common.refreshDelete(prestasiSiswa);
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
			aksiButtons.add(button);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);

			aksiBoxRef[0] = ais.ui.util.UIHelper.buatBarisAksi(vbox1, 3, aksiButtons);
			aksiBoxRef[0].setVisible(!prestasiSiswa.getStatus().equals(PrestasiSiswa.DISETUJUI) && tbmuser != null);

			myHbox.setParent(vbox1);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PrestasiSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final PrestasiSiswa prestasiSiswa) throws Exception {
		this.prestasiSiswa = prestasiSiswa;
		addWindow.setTitle(prestasiSiswa.getId() == null ? "Tambah Prestasi Kesiswaan" : "Ubah Prestasi Kesiswaan");
		Common.clear(addWindow);
		addWindow.setWidth("700px");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kejuaraan *"));
		row.appendChild(nama = new Textbox(prestasiSiswa.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kejuaraan (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(prestasiSiswa.getNamaEn()));
		namaEn.setWidth("90%");
		namaEn.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kejuaraan *"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		tanggal = new MyDatebox(prestasiSiswa.getTanggal());
		hbox.appendChild(tanggal);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		tanggalSelesai = new MyDatebox(prestasiSiswa.getTanggalSelesai());
		hbox.appendChild(tanggalSelesai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Siswa *"));
		row.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setWidth("90%");
		siswa.setReadonly(true);

		if (mhs != null) {
			siswa.setAttribute("siswa", mhs);
			siswa.setDisabled(true);
			siswa.setValue(mhs.getNama());
		} else {
			siswa.setAttribute("siswa", prestasiSiswa.getSiswa());
			siswa.setValue(prestasiSiswa.getSiswa() == null ? "" : prestasiSiswa.getSiswa().getNama());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru I"));
		row.appendChild(guru = new AmbilDataGuruBanbox());

		if (searchguru.getAttribute("guru") != null) {
			prestasiSiswa.setGuru((Guru) searchguru.getAttribute("guru"));
			guru.setDisabled(searchguru.isDisabled());
		}

		guru.setAttribute("guru", prestasiSiswa.getGuru());
		guru.setAttribute("myValue", prestasiSiswa.getGuru());
		guru.setValue(prestasiSiswa.getGuru() == null ? "" : prestasiSiswa.getGuru().getNamaGuru());
		guru.setWidth("90%");
		guru.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru II"));
		row.appendChild(guru2 = new AmbilDataGuruBanbox());
		guru2.setAttribute("guru", prestasiSiswa.getGuru2());
		guru2.setAttribute("myValue", prestasiSiswa.getGuru2());
		guru2.setValue(prestasiSiswa.getGuru2() == null ? "" : prestasiSiswa.getGuru2().getNamaGuru());
		guru2.setWidth("90%");
		guru2.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru III"));
		row.appendChild(guru3 = new AmbilDataGuruBanbox());
		guru3.setAttribute("guru", prestasiSiswa.getGuru3());
		guru3.setAttribute("myValue", prestasiSiswa.getGuru3());
		guru3.setValue(prestasiSiswa.getGuru3() == null ? "" : prestasiSiswa.getGuru3().getNamaGuru());
		guru3.setWidth("90%");
		guru3.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat Kejuaraan *"));
		row.appendChild(tempat = new Textbox(prestasiSiswa.getTempat()));
		tempat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Juara ke *"));
		row.appendChild(juara = new Textbox(prestasiSiswa.getJuara()));
		juara.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peringkat"));
		row.appendChild(peringkat = new Intbox(prestasiSiswa.getPeringkat()));
		peringkat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyelenggara *"));
		row.appendChild(penyelenggara = new Textbox(prestasiSiswa.getPenyelenggara()));
		penyelenggara.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Sertifikat Prestasi"));
		row.appendChild(nomorSertifikat = new Textbox(prestasiSiswa.getNomorSertifikat()));
		nomorSertifikat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cabang *"));
		row.appendChild(cabangPrestasiSiswa = new Combobox());
		Common.insertCombo(cabangPrestasiSiswa, "nama", CabangPrestasiSiswa.class);
		Common.selectComboItem(cabangPrestasiSiswa, prestasiSiswa.getCabangPrestasiSiswa());
		cabangPrestasiSiswa.setWidth("90%");
		cabangPrestasiSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori *"));
		row.appendChild(kategoriPrestasiSiswa = new Combobox());
		Common.insertCombo(kategoriPrestasiSiswa, "nama", KategoriPrestasiSiswa.class);
		Common.selectComboItem(kategoriPrestasiSiswa, prestasiSiswa.getKategoriPrestasiSiswa());
		kategoriPrestasiSiswa.setWidth("90%");
		kategoriPrestasiSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Peserta"));
		row.appendChild(jumlahPeserta = new Textbox(prestasiSiswa.getJumlahPeserta()));
		jumlahPeserta.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Capaian"));
		row.appendChild(capaian = new Textbox(prestasiSiswa.getCapaian()));
		capaian.setWidth("90%");
		capaian.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link / URL"));
		row.appendChild(url = new Textbox(prestasiSiswa.getUrl()));
		url.setWidth("90%");
		url.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Apakah kejuaraan diluar sekolah ?"));
		row.appendChild(prestasiLuarKampus = new Checkbox());
		prestasiLuarKampus.setChecked(prestasiSiswa.getPrestasiLuarKampus());

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initYayasanDanSekolahDanSemua(yayasan = new Combobox(), sekolah = new Combobox(), null, null);
		if (prestasiSiswa.getYayasan() == null && tbmuser.ambilYayasan() != null) {
			prestasiSiswa.setYayasan(tbmuser.ambilYayasan());
		}
		rowYayasan = new MyFormRow();
		rowYayasan.setStyle("border:0px;background: transparent;");
		rowYayasan.setParent(rows);
		rowYayasan.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		rowYayasan.appendChild(yayasan);
		Common.selectComboItem(yayasan, prestasiSiswa.getYayasan());
		yayasan.setWidth("90%");

		if (yayasan.getSelectedItem() != null && yayasan.getSelectedItem().getValue() != null) {
			Common.insertCombo(sekolah, new String[] { "nama", "kodeEpsbed" }, "jenjang", Sekolah.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false));
		}

		rowSekolah = new MyFormRow();
		rowSekolah.setStyle("border:0px;background: transparent;");
		rowSekolah.setParent(rows);
		rowSekolah.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		rowSekolah.appendChild(sekolah);
		sekolah.setWidth("90%");
		Common.pilihSekolah(sekolah, prestasiSiswa.getSekolah());

		if (prestasiSiswa.getSekolah() == null) {
			if (tbmuser.ambilSekolah() != null
					|| (tbmuser.getSiswa() != null && tbmuser.getSiswa().getSekolah() != null)) {
				Common.pilihSekolah(sekolah,
						tbmuser == null || tbmuser.ambilSekolah() == null ? tbmuser.getSiswa().getSekolah()
								: tbmuser.ambilSekolah());
				sekolah.setDisabled(true);
			} else {
				sekolah.setDisabled(false);
			}
		}

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowYayasan.setVisible(!prestasiLuarKampus.isChecked());
				rowSekolah.setVisible(!prestasiLuarKampus.isChecked());
			}
		};

		prestasiLuarKampus.addEventListener("onClick", eventListener);
		eventListener.onEvent(null);

		Common.generateTahunAjaran(tahunAkademik = new Combobox());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, prestasiSiswa.getTahunAkademik());

		jenisSemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		jenisSemester.setSelectedIndex(1);
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setReadonly(true);

		Common.selectComboItem(jenisSemester, prestasiSiswa.getJenisSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		row.appendChild(tahun = new Intbox(prestasiSiswa.getTahun()));
		tahun.setWidth("90%");
		tahun.setReadonly(true);

		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					tahun.setValue(Integer.parseInt(tahunAkademik.getValue().split("/")[0]));
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi / Alamat"));
		row.appendChild(alamat = new Textbox(prestasiSiswa.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor SK"));
		row.appendChild(noSk = new Textbox(prestasiSiswa.getNoSk()));
		noSk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK"));
		row.appendChild(tglSk = new MyDatebox(prestasiSiswa.getTglSk()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(prestasiSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Scan / foto bukti prestasi"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, prestasiSiswa.getId(), PrestasiSiswa.class.getName(),
				"Lampiran bukti prestasi", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainSiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran kegiatan lebih dari satu file, zip dulu semua file tersebut");

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
			MyMessageboxConfig.show("Nama Kejuaraan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Mulai Kejuaraan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tanggalSelesai.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Selesai Kejuaraan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (siswa.getAttribute("siswa") == null) {
			MyMessageboxConfig.show("Siswa harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tempat.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Tempat Kejuaraan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (juara.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Juara ke, harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (penyelenggara.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Penyelenggara harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
//		if (nomorSertifikat.getValue().trim().equals("")) {
//			MyMessageboxConfig.show("Nomor sertifikat kejuaraan harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

		if (cabangPrestasiSiswa.getSelectedItem() == null) {
			MyMessageboxConfig.show("Cabang Kejuaraan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kategoriPrestasiSiswa.getSelectedItem() == null) {
			MyMessageboxConfig.show("Kategori Kejuaraan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

//		if (jumlahPeserta.getValue().trim().equals("")) {
//			MyMessageboxConfig.show("Jumlah peserta kejuaraan harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}
//		if (capaian.getValue().trim().equals("")) {
//			MyMessageboxConfig.show("Capaian kejuaraan harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

		if (!prestasiLuarKampus.isChecked()
				&& (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show(
					"Jika prestasi di dalam kampus, maka data " + Common.getBahasaConfig("Yayasan") + " harus diisi",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

//		try {
//
//			if (prestasiSiswa != null && prestasiSiswa.getId() != null) {
//				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
//
//				int jumlah = ((Number) streamingSession.createCriteria(LampiranLain.class)
//						.setProjection(Projections.rowCount()).add(Restrictions.eq("ref", prestasiSiswa.getId()))
//						.add(Restrictions.eq("jenis", PrestasiSiswa.class.getName())).uniqueResult()).intValue();
//
//				StreamingHibernateUtil.getInstance().closeSession();
//
//				if (jumlah == 0) {
//					MyMessageboxConfig.show("File scan / foto sertifikat prestasi harus diupload", "Peringatan",
//							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
//					return false;
//				}
//			} else {
//				if (lainSiswa == null) {
//					MyMessageboxConfig.show("File scan / foto sertifikat prestasi harus diupload", "Peringatan",
//							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
//					return false;
//				}
//			}
//
//		} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/sekolah/PrestasiSiswaAction.java:1078");
//			StreamingHibernateUtil.getInstance().rollbackTransaction();
//			e1.printStackTrace();
//		}

		Session session = HibernateUtil.currentSession();
		if (prestasiSiswa.getId() != null) {
			prestasiSiswa = (PrestasiSiswa) session.load(PrestasiSiswa.class, prestasiSiswa.getId());

		}

		// private Combobox cabangPrestasiSiswa;
		// private Combobox kategoriPrestasiSiswa;
		// private Textbox jumlahPeserta;
		// private Textbox capaian;
		// private Textbox url;

		prestasiSiswa.setPeringkat(peringkat.getValue());
		prestasiSiswa.setCabangPrestasiSiswa((CabangPrestasiSiswa) (cabangPrestasiSiswa.getSelectedItem() == null ? null
				: cabangPrestasiSiswa.getSelectedItem().getValue()));
		prestasiSiswa.setKategoriPrestasiSiswa(
				(KategoriPrestasiSiswa) (kategoriPrestasiSiswa.getSelectedItem() == null ? null
						: kategoriPrestasiSiswa.getSelectedItem().getValue()));
		prestasiSiswa.setJumlahPeserta(jumlahPeserta.getValue());
		prestasiSiswa.setCapaian(capaian.getValue());
		prestasiSiswa.setUrl(url.getValue());

		prestasiSiswa.setPrestasiLuarKampus(prestasiLuarKampus.isChecked());
		prestasiSiswa.setTanggal(tanggal.getValue());
		prestasiSiswa.setTanggalSelesai(tanggalSelesai.getValue());
		prestasiSiswa.setNama(nama.getValue());
		prestasiSiswa.setNamaEn(namaEn.getValue());
		prestasiSiswa.setTempat(tempat.getValue());
		prestasiSiswa.setJuara(juara.getValue());
		prestasiSiswa.setNomorSertifikat(nomorSertifikat.getValue());
		prestasiSiswa.setSiswa((Siswa) siswa.getAttribute("siswa"));
		prestasiSiswa.setKeterangan(keterangan.getValue());
		prestasiSiswa.setPenyelenggara(penyelenggara.getValue());
		prestasiSiswa.setTanggal(tanggal.getValue());
		prestasiSiswa.setGuru((Guru) guru.getAttribute("guru"));

		prestasiSiswa.setGuru2((Guru) guru2.getAttribute("guru"));
		prestasiSiswa.setGuru3((Guru) guru3.getAttribute("guru"));
		prestasiSiswa.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? null
						: sekolah.getSelectedItem().getValue()));
		prestasiSiswa.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null ? null
						: yayasan.getSelectedItem().getValue()));

		prestasiSiswa.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		prestasiSiswa.setJenisSemester((String) jenisSemester.getSelectedItem().getValue());

		prestasiSiswa.setAlamat(alamat.getValue());
		prestasiSiswa.setNoSk(noSk.getValue());
		prestasiSiswa.setTglSk(tglSk.getValue());

		Common.refreshSaveOrUpdate(session, prestasiSiswa);

		if (lainSiswa != null && lainSiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainSiswa);
				lainSiswa.setRef(prestasiSiswa.getId());

				session.getTransaction().begin();
				session.update(lainSiswa);
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
		Criteria criteria = session.createCriteria(PrestasiSiswa.class).createAlias("siswa", "siswa")
				.createAlias("siswa.sekolah", "sekolah")

				.createAlias("kelasSiswa", "kelasSiswa")

				.add((searchguru == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchguru.getAttribute("guru") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("guru3", searchguru.getAttribute("guru")),
								Restrictions.or(Restrictions.eq("guru2", searchguru.getAttribute("guru")),
										Restrictions.or(Restrictions.eq("guru", searchguru.getAttribute("guru")),
												Restrictions.eq("kelasSiswa.guruPembina",
														searchguru.getAttribute("guru")))))))

				.add((searchsiswa == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsiswa.getAttribute("siswa") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("siswa", searchsiswa.getAttribute("siswa"))))

				.add((searchmulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmulai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tanggal", searchmulai.getValue())))

				.add((searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsampai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.le("tanggal", searchsampai.getValue())));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}

		if (order)
			criteria.addOrder(Order.desc("id")); // pengajuan terkini di atas

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchpenyelenggara.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("penyelenggara", searchpenyelenggara.getValue().trim(),
								MatchMode.ANYWHERE))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						|| searchstatus.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.add(searchcabangPrestasiSiswa.getSelectedItem() == null
						|| searchcabangPrestasiSiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("cabangPrestasiSiswa",
										searchcabangPrestasiSiswa.getSelectedItem().getValue()))

				.add(searchkategoriPrestasiSiswa.getSelectedItem() == null
						|| searchkategoriPrestasiSiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kategoriPrestasiSiswa",
										searchkategoriPrestasiSiswa.getSelectedItem().getValue()))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", searchta.getSelectedItem().getValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| (tbmuser != null && tbmuser.getSiswa() != null) ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("siswa.sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| (tbmuser != null && tbmuser.getSiswa() != null) ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah.yayasan", searchyayasan, false));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PrestasiSiswa> myPrestasiSiswas;

		if (prestasiSiswaSelected != null) {
			myPrestasiSiswas = new ArrayList<PrestasiSiswa>();
			myPrestasiSiswas.add(prestasiSiswaSelected);
			myPrestasiSiswas.addAll(initCriteria(true).add(Restrictions.ne("id", prestasiSiswaSelected.getId()))
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list());
		} else {
			myPrestasiSiswas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		}

		ListModel strset = new SimpleListModel(myPrestasiSiswas);
		grid.setRowRenderer(new PrestasiSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
