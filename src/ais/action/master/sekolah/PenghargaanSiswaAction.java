package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.KategoriPenghargaan;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.PenghargaanSiswa;
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
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk penghargaan siswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code AmbilDataSiswaBanbox searchsiswa}, {@code
 * MyDatebox searchmulai}, {@code MyDatebox searchsampai}, {@code Combobox searchstatus}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onKategoriPenghargaan()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
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
public class PenghargaanSiswaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataSiswaBanbox searchsiswa;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;
	private Combobox searchstatus;
	private Combobox searchta;
	private Combobox searchsekolah;
	private Combobox searchyayasan;
	private Combobox searchkategoriPenghargaan;

	private Textbox nama;
	private MyDatebox tanggal;
	private MyDatebox tanggalSelesai;
	private AmbilDataSiswaBanbox siswa;
	private Combobox sekolah;
	private Combobox yayasan;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Textbox keterangan;

	private PenghargaanSiswa penghargaanSiswa;
	private MyToolbarbuttonConfig add;

	protected LampiranLain lainSiswa;
	private Tbmuser tbmuser;
	private Textbox nomorSertifikat;

	private Combobox kategoriPenghargaan;
	private Textbox capaian;
	private Textbox url;

	private Siswa mhs;
	private Row rowYayasan;
	private Row rowSekolah;

	private Tabpanel kategoriPenghargaanTab;

	private MyColumnConfig colNama;

	public void onKategoriPenghargaan(Event event) {
		if (kategoriPenghargaanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kategoriPenghargaanTab);
			MyInclude iframe = new MyInclude("/pages/master/kategori_penghargaan.zul");
			iframe.setParent(window);
		}
	}

	private Intbox tahun;
	private PenghargaanSiswa penghargaanSiswaSelected = null;
	private Textbox namaEn;
	private Textbox alamat;
	private Textbox noSk;
	private MyDatebox tglSk;

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

		tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.getSiswa() != null && colNama != null) {
			colNama.setWidth("0px");
		}

		Common.generateTahunAjaranDanSemua(searchta);
		Common.selectComboItem(searchta, null);

		kategoriPenghargaanTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.getSiswa() == null);

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(KategoriPenghargaan.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			KategoriPenghargaan kategoriPenghargaan = new KategoriPenghargaan();
			kategoriPenghargaan.setNama("Paten");
			session.save(kategoriPenghargaan);

			kategoriPenghargaan = new KategoriPenghargaan();
			kategoriPenghargaan.setNama("HaKI");
			session.save(kategoriPenghargaan);

			kategoriPenghargaan = new KategoriPenghargaan();
			kategoriPenghargaan.setNama("Nasional / Internasional");
			session.save(kategoriPenghargaan);
		}

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		Common.insertComboDanSemua(searchkategoriPenghargaan, "nama", KategoriPenghargaan.class);

		Comboitem comboitem = new Comboitem(PenghargaanSiswa.BELUM_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PenghargaanSiswa.BELUM_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PenghargaanSiswa.SEDANG_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PenghargaanSiswa.SEDANG_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PenghargaanSiswa.DISETUJUI);
		if (comboitem != null) { comboitem.setValue(PenghargaanSiswa.DISETUJUI); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PenghargaanSiswa.DITOLAK);
		if (comboitem != null) { comboitem.setValue(PenghargaanSiswa.DITOLAK); }
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

		if (execution.getParameter("penghargaan") != null) {
			penghargaanSiswaSelected = (PenghargaanSiswa) GeneralValueObject.ambilData(PenghargaanSiswa.class,
					execution.getParameter("penghargaan").toString());
		}

		if (execution.getParameter("tahunAjaran") != null) {
			String tahunAjaran = execution.getParameter("tahunAjaran");
			Common.selectComboItem(true, searchta, tahunAjaran);
		}

		if (execution.getParameter("kategoriPenghargaan") != null) {
			KategoriPenghargaan kategoriPenghargaanSelected = (KategoriPenghargaan) HibernateUtil.currentSession()
					.createCriteria(KategoriPenghargaan.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("kategoriPenghargaan"))))
					.uniqueResult();
			Common.selectComboItem(true, searchkategoriPenghargaan, kategoriPenghargaanSelected);
		}

		if (execution.getParameter("sekolah") != null) {
			Sekolah sekolahSelected = (Sekolah) HibernateUtil.currentSession().createCriteria(Sekolah.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("sekolah")))).uniqueResult();
			Common.selectComboItem(true, searchsekolah, sekolahSelected);

			Common.selectComboItem(true, searchyayasan, sekolahSelected == null ? null : sekolahSelected.getYayasan());

		}

		if (mhs != null) {
			searchsiswa.setAttribute("siswa", mhs);
			searchsiswa.setDisabled(true);
			searchsiswa.setValue(mhs.getNama());
		}

		if (add != null) { add.setVisible(tbmuser != null); }
		// add.setTooltiptext("Tambah");

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "siswa", "nama", "tanggal", "tanggalSelesai", "nomorSertifikat",
				"kategoriPenghargaan", "capaian", "url", "yayasan", "sekolah", "tahunAkademik", "jenisSemester",
				"tahun", "status", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PenghargaanSiswa.class, contents);
		upload.setVisible(
				(add != null && add.isVisible()) && tbmuser != null && tbmuser.getSiswa() == null);
		Common.appendKeToolbar(upload, add, comp);

		if (mhs != null) {

		}

	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link PenghargaanSiswaAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PenghargaanSiswaAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PenghargaanSiswaAction
	 */
	class PenghargaanSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenghargaanSiswa penghargaanSiswa = (PenghargaanSiswa) arg1;

			try {
				if (penghargaanSiswaSelected != null
						&& penghargaanSiswaSelected.getId().equals(penghargaanSiswa.getId())) {
					arg0.setStyle("background-color:yellow");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PenghargaanSiswaAction.java:295");
				// TODO: handle exception
			}

			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);

			Vbox myvbox = new Vbox();
			myvbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(penghargaanSiswa.getSiswa()).setParent(myvbox);

			new Label(penghargaanSiswa.getSiswa().getNim() + "-" + penghargaanSiswa.getSiswa().getNama())
					.setParent(myvbox);

			Vbox a = RevisiHelper.createNewRevisi(PenghargaanSiswa.class, penghargaanSiswa, penghargaanSiswa.getNama());

			new Label(penghargaanSiswa.getNamaEn()).setParent(a);

			a.setParent(arg0);

			myvbox = new Vbox();
			myvbox.setParent(detail);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, penghargaanSiswa.getId(), PenghargaanSiswa.class.getName(),
					"Lampiran", false, null, null, false, false, false, false);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new MyLabelAgakKecil("Tanggal: "
					+ (penghargaanSiswa.getTanggal() == null ? ""
							: Common.dateFormat1.get().format(penghargaanSiswa.getTanggal()))
					+ (penghargaanSiswa.getTanggalSelesai() == null ? ""
							: " s.d " + Common.dateFormat1.get().format(penghargaanSiswa.getTanggalSelesai())))
					.setParent(myvbox);
			new MyLabelAgakKecil(
					"TA/Smt: " + penghargaanSiswa.getTahunAkademik() + "/" + penghargaanSiswa.getJenisSemester())
					.setParent(myvbox);

			myvbox = new Vbox();
			myvbox.setParent(arg0);

			new MyLabelAgakKecil("Kategori: " + (penghargaanSiswa.getKategoriPenghargaan() == null ? ""
					: penghargaanSiswa.getKategoriPenghargaan().getNama())).setParent(myvbox);
			new MyLabelAgakKecil("Link: " + penghargaanSiswa.getUrl()).setParent(myvbox);

			new Label(penghargaanSiswa.getCapaian()).setParent(arg0);

			new Label(penghargaanSiswa.getNomorSertifikat()).setParent(arg0);

			// Kolom aksi rapi: semua tombol dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi.
			// aksiBoxRef menampung Vbox pembungkus supaya visibilitas grup tetap bisa
			// diubah dari listener Combobox status (perilaku sama dgn Hbox toolbar lama).
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			final Vbox[] aksiBoxRef = new Vbox[1];
			final MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
					"/img/Finance-Invoice-icon.png");
			final Hbox myHbox = new Hbox();
			myHbox.setVisible(penghargaanSiswa.getStatus().equals(PenghargaanSiswa.DISETUJUI));
			if (mhs == null && tbmuser != null) {
				final Combobox status = new Combobox();
				Comboitem comboitem = new Comboitem(PenghargaanSiswa.BELUM_DIPROSES);
				comboitem.setValue(PenghargaanSiswa.BELUM_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PenghargaanSiswa.SEDANG_DIPROSES);
				comboitem.setValue(PenghargaanSiswa.SEDANG_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PenghargaanSiswa.DISETUJUI);
				comboitem.setValue(PenghargaanSiswa.DISETUJUI);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PenghargaanSiswa.DITOLAK);
				comboitem.setValue(PenghargaanSiswa.DITOLAK);
				status.appendChild(comboitem);

				Common.selectComboItem(status, penghargaanSiswa.getStatus());
				status.setParent(arg0);
				status.setReadonly(true);
				status.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						penghargaanSiswa.setStatus((String) (status.getSelectedItem() == null
								|| status.getSelectedItem().getValue() == null ? null
										: status.getSelectedItem().getValue()));
						Common.refreshUpdate(penghargaanSiswa);
						if (aksiBoxRef[0] != null) {
							aksiBoxRef[0].setVisible(!penghargaanSiswa.getStatus().equals(PenghargaanSiswa.DISETUJUI));
						}

						if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
								&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
							buttonTagihan.setVisible(penghargaanSiswa.getStatus().equals(PenghargaanSiswa.DISETUJUI));

						}
						myHbox.setVisible(penghargaanSiswa.getStatus().equals(PenghargaanSiswa.DISETUJUI));
					}
				};
				status.addEventListener("onChange", eventListener);
			} else {
				new Label(penghargaanSiswa.getStatus()).setParent(arg0);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil(
					penghargaanSiswa.getYayasan() == null ? "Semua" : penghargaanSiswa.getYayasan().getNama())
					.setParent(vbox);
			new MyLabelAgakKecil(
					penghargaanSiswa.getSekolah() == null ? "Semua" : penghargaanSiswa.getSekolah().getNama())
					.setParent(vbox);

			new Label(penghargaanSiswa.getKeterangan()).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(penghargaanSiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
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

											if (penghargaanSiswaSelected != null && penghargaanSiswaSelected.getId()
													.equals(penghargaanSiswa.getId())) {
												penghargaanSiswaSelected = null;
											}

											Common.refreshDelete(penghargaanSiswa);

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
			aksiBoxRef[0].setVisible(
					!penghargaanSiswa.getStatus().equals(PenghargaanSiswa.DISETUJUI) && tbmuser != null);

			myHbox.setParent(vbox1);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PenghargaanSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final PenghargaanSiswa penghargaanSiswa) throws Exception {
		this.penghargaanSiswa = penghargaanSiswa;
		addWindow.setTitle(penghargaanSiswa.getId() == null ? "Tambah Penghargaan Siswa" : "Ubah Penghargaan Siswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Karya *"));
		row.appendChild(nama = new Textbox(penghargaanSiswa.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Karya (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(penghargaanSiswa.getNamaEn()));
		namaEn.setWidth("90%");
		namaEn.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pendaftaran Karya *"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		tanggal = new MyDatebox(penghargaanSiswa.getTanggal());
		hbox.appendChild(tanggal);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		tanggalSelesai = new MyDatebox(penghargaanSiswa.getTanggalSelesai());
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
			siswa.setAttribute("siswa", penghargaanSiswa.getSiswa());
			siswa.setValue(penghargaanSiswa.getSiswa() == null ? "" : penghargaanSiswa.getSiswa().getNama());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Sertifikat Penghargaan *"));
		row.appendChild(nomorSertifikat = new Textbox(penghargaanSiswa.getNomorSertifikat()));
		nomorSertifikat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bentuk Penghargaan *"));
		row.appendChild(kategoriPenghargaan = new Combobox());
		Common.insertCombo(kategoriPenghargaan, "nama", KategoriPenghargaan.class);
		Common.selectComboItem(kategoriPenghargaan, penghargaanSiswa.getKategoriPenghargaan());
		kategoriPenghargaan.setWidth("90%");
		kategoriPenghargaan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Capaian *"));
		row.appendChild(capaian = new Textbox(penghargaanSiswa.getCapaian()));
		capaian.setWidth("90%");
		capaian.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link / URL"));
		row.appendChild(url = new Textbox(penghargaanSiswa.getUrl()));
		url.setWidth("90%");
		url.setRows(2);

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initYayasanDanSekolahDanSemua(yayasan = new Combobox(), sekolah = new Combobox(), null, null);
		if (penghargaanSiswa.getYayasan() == null && tbmuser.ambilYayasan() != null) {
			penghargaanSiswa.setYayasan(tbmuser.ambilYayasan());
		}
		rowYayasan = new MyFormRow();
		rowYayasan.setStyle("border:0px;background: transparent;");
		rowYayasan.setParent(rows);
		rowYayasan.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		rowYayasan.appendChild(yayasan);
		Common.selectComboItem(yayasan, penghargaanSiswa.getYayasan());
		yayasan.setWidth("90%");

		if (yayasan.getSelectedItem() != null && yayasan.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(sekolah, new String[] { "nama", "kodeEpsbed" }, "jenjang", Sekolah.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false));
		}

		rowSekolah = new MyFormRow();
		rowSekolah.setStyle("border:0px;background: transparent;");
		rowSekolah.setParent(rows);
		rowSekolah.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		rowSekolah.appendChild(sekolah);
		sekolah.setWidth("90%");
		Common.pilihSekolah(sekolah, penghargaanSiswa.getSekolah());

		if (penghargaanSiswa.getSekolah() == null) {
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

		Common.generateTahunAjaran(tahunAkademik = new Combobox());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, penghargaanSiswa.getTahunAkademik());

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

		Common.selectComboItem(jenisSemester, penghargaanSiswa.getJenisSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		row.appendChild(tahun = new Intbox(penghargaanSiswa.getTahun()));
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
		row.appendChild(alamat = new Textbox(penghargaanSiswa.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor SK"));
		row.appendChild(noSk = new Textbox(penghargaanSiswa.getNoSk()));
		noSk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK"));
		row.appendChild(tglSk = new MyDatebox(penghargaanSiswa.getTglSk()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(penghargaanSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Scan / foto sertifikat penghargaan *"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, penghargaanSiswa.getId(), PenghargaanSiswa.class.getName(),
				"Lampiran Sertifikat", false, new EventListener() {

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

		if (nomorSertifikat.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nomor sertifikat kejuaraan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kategoriPenghargaan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Kategori Kejuaraan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (capaian.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Capaian kejuaraan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		try {

			if (penghargaanSiswa != null && penghargaanSiswa.getId() != null) {

				LampiranLain lam = LampiranLain.ambil(penghargaanSiswa.getId(), PenghargaanSiswa.class.getName());

				if (lam == null) {
					MyMessageboxConfig.show("File scan / foto sertifikat penghargaan harus diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			} else {
				if (lainSiswa == null) {
					MyMessageboxConfig.show("File scan / foto sertifikat penghargaan harus diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			}

		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/PenghargaanSiswaAction.java:798");
		}

		Session session = HibernateUtil.currentSession();
		if (penghargaanSiswa.getId() != null) {
			penghargaanSiswa = (PenghargaanSiswa) session.load(PenghargaanSiswa.class, penghargaanSiswa.getId());

		}

		// private Combobox cabangPenghargaanSiswa;
		// private Combobox kategoriPenghargaan;
		// private Textbox jumlahPeserta;
		// private Textbox capaian;
		// private Textbox url;

		penghargaanSiswa
				.setKategoriPenghargaan((KategoriPenghargaan) (kategoriPenghargaan.getSelectedItem() == null ? null
						: kategoriPenghargaan.getSelectedItem().getValue()));
		penghargaanSiswa.setCapaian(capaian.getValue());
		penghargaanSiswa.setUrl(url.getValue());

		penghargaanSiswa.setTanggal(tanggal.getValue());
		penghargaanSiswa.setTanggalSelesai(tanggalSelesai.getValue());
		penghargaanSiswa.setNama(nama.getValue());
		penghargaanSiswa.setNamaEn(namaEn.getValue());
		penghargaanSiswa.setNomorSertifikat(nomorSertifikat.getValue());
		penghargaanSiswa.setSiswa((Siswa) siswa.getAttribute("siswa"));
		penghargaanSiswa.setKeterangan(keterangan.getValue());
		penghargaanSiswa.setTanggal(tanggal.getValue());

		penghargaanSiswa.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? null
						: sekolah.getSelectedItem().getValue()));
		penghargaanSiswa.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null ? null
						: yayasan.getSelectedItem().getValue()));

		penghargaanSiswa.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		penghargaanSiswa.setJenisSemester((String) jenisSemester.getSelectedItem().getValue());

		penghargaanSiswa.setAlamat(alamat.getValue());
		penghargaanSiswa.setNoSk(noSk.getValue());
		penghargaanSiswa.setTglSk(tglSk.getValue());

		Common.refreshSaveOrUpdate(session, penghargaanSiswa);

		if (lainSiswa != null && lainSiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainSiswa);
				lainSiswa.setRef(penghargaanSiswa.getId());

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
		Criteria criteria = session.createCriteria(PenghargaanSiswa.class)

				.createAlias("siswa", "siswa").createAlias("siswa.sekolah", "sekolah")

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

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						|| searchstatus.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.add(searchkategoriPenghargaan.getSelectedItem() == null
						|| searchkategoriPenghargaan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kategoriPenghargaan",
										searchkategoriPenghargaan.getSelectedItem().getValue()))

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

		List<PenghargaanSiswa> myPenghargaanSiswas;

		if (penghargaanSiswaSelected != null) {
			myPenghargaanSiswas = new ArrayList<PenghargaanSiswa>();
			myPenghargaanSiswas.add(penghargaanSiswaSelected);
			myPenghargaanSiswas.addAll(initCriteria(true).add(Restrictions.ne("id", penghargaanSiswaSelected.getId()))
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list());
		} else {
			myPenghargaanSiswas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		}

		ListModel strset = new SimpleListModel(myPenghargaanSiswas);
		grid.setRowRenderer(new PenghargaanSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
