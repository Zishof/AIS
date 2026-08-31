package ais.action.master.penelitiandanpengabdian;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.DetailPenelitianDanPengabdianHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.master.penelitiandanpengabdian.helper.PengajuanPenelitianDanPengabdianHelper;
import ais.action.master.penelitiandanpengabdian.helper.TahapanPelaporanPenelitianDanPengabdianHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DiskusiPenelitianDanPengabdian;
import ais.database.model.Mahasiswa;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.JenisPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.PenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.TipePenelitianDanPengabdian;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk penelitian dan pengabdian. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code MyWindow
 * addWindowAttachment}, {@code MyWindow addWindowPengajuan}, {@code Paging paging}, {@code MyGrid grid}, {@code
 * Combobox searchjenis}, {@code Textbox searchkode}, {@code Textbox searchnama}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initPendahuluan()}, {@code initProfile()}, {@code
 * initProsedur()}, {@code initPelaksanaan()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data
 * ({@code onSave()}); operasi domain lain ({@code diperuntukkan()}, {@code onAdd()}, {@code kirimEmail()},
 * {@code kirimEmailKeKorespondensi()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
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
public class PenelitianDanPengabdianAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyWindow addWindowAttachment;
	private MyWindow addWindowPengajuan;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchjenis;
	private Textbox searchkode;
	private Textbox searchnama;

	private Textbox judul;
	private MyCkEditor pendahuluan;

	private boolean edit = false;
	private boolean delete = false;

	private PenelitianDanPengabdian penelitianDanPengabdian;
	private MyToolbarbuttonConfig add;
	private Intbox tahun;
	private Combobox diperuntukkan;
	private Combobox jenisPenelitianDanPengabdian;
	private MyDatebox tanggalMulaiPengajuan;
	private MyDatebox tanggalSampaiPengajuan;
	private MyCkEditor tujuan;
	private MyCkEditor luaranPenelitian;
	private MyCkEditor kriteriaDanPengusulan;
	private MyCkEditor sistematika;
	private MyCkEditor seleksiDanEvaluasi;
	private MyCkEditor sampul;
	private MyCkEditor pengesahan;
	private MyCkEditor sumberDana;
	private MyCkEditor pelaksanaan;
	private MyCkEditor pelaporan;
	private MyCkEditor deskEvaluasi;
	private MyCkEditor pembahasan;
	private MyCkEditor monitoringDanEvaluasi;
	private MyCkEditor kelayakan;
	private MyCkEditor lampiranUmum;
	private Textbox korespondensi;
	private Combobox tipePenelitianDanPengabdian;
	private MyCheckboxConfig publik;
	private Combobox tahunAkademik;
	private Combobox semester;
	// private Intbox sks;
	private Intbox sks;
	private Textbox korespondensiGrupPengguna;

	private void diperuntukkan(Combobox diperuntukkan) {
		MyComboitemConfig comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_UMUM);
		comboitem.setValue(PengumumanAkademis.UNTUK_UMUM);
		diperuntukkan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_DOSEN);
		comboitem.setValue(PengumumanAkademis.UNTUK_DOSEN);
		diperuntukkan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_MAHASISWA);
		comboitem.setValue(PengumumanAkademis.UNTUK_MAHASISWA);
		diperuntukkan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_PEGAWAI);
		comboitem.setValue(PengumumanAkademis.UNTUK_PEGAWAI);
		diperuntukkan.appendChild(comboitem);

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
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.insertCombo(searchjenis, "isi", JenisPenelitianDanPengabdian.class);

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

	}

	class PenelitianDanPengabdianRenderer extends ais.ui.util.MyRowRenderer {

		private DetailPenelitianDanPengabdianHelper detailPenelitianDanPengabdianHelper = new DetailPenelitianDanPengabdianHelper();
		private PengajuanPenelitianDanPengabdianHelper pengajuanPenelitianDanPengabdianHelper = new PengajuanPenelitianDanPengabdianHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenelitianDanPengabdian penelitianDanPengabdian = (PenelitianDanPengabdian) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detail.getChildren().isEmpty()) {

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("7000px");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						MyTabConfig tab3 = new MyTabConfig("Pengajuan");
						tab3.setParent(tabs);

						MyTabConfig tab4 = new MyTabConfig("Tahapan Pelaporan Pelaksanaan");
						tab4.setParent(tabs);

						MyTabConfig tab2 = new MyTabConfig("Lampiran");
						tab2.setParent(tabs);

						MyTabConfig tab1 = new MyTabConfig("Diskusi");
						tab1.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
						tabpanel3.setHeight("7000px");
						tabpanel3.setParent(tabpanels);
						pengajuanPenelitianDanPengabdianHelper.displayPengajuan(false, null,
								penelitianDanPengabdian.getDiperuntukkan(), penelitianDanPengabdian, tabpanel3,
								addWindowPengajuan, penelitianDanPengabdian.getTipePenelitianDanPengabdian(), "6500px");

						Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
						tabpanel4.setHeight("7000px");
						tabpanel4.setParent(tabpanels);
						TahapanPelaporanPenelitianDanPengabdianHelper tahapan = new TahapanPelaporanPenelitianDanPengabdianHelper();
						tahapan.displayTahapanPelaporan(false, penelitianDanPengabdian.getTipePenelitianDanPengabdian(),
								penelitianDanPengabdian, null, tabpanel4);

						final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
						tabpanel2.setHeight("7000px");
						tabpanel2.setParent(tabpanels);
						detailPenelitianDanPengabdianHelper.displayAttachment(penelitianDanPengabdian, tabpanel2,
								addWindowAttachment);

						Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
						tabpanel1.setParent(tabpanels);
						tabpanel1.setHeight("7000px");
						detailPenelitianDanPengabdianHelper.displayDetailPengumuman(penelitianDanPengabdian, tabpanel1,
								addWindow);

					}

				}
			});

			RevisiHelper.createNewRevisi(PenelitianDanPengabdian.class, penelitianDanPengabdian,
					penelitianDanPengabdian.getJenisPenelitianDanPengabdian().getIsi()).setParent(arg0);

			new Label(penelitianDanPengabdian.getTipePenelitianDanPengabdian() == null ? ""
					: penelitianDanPengabdian.getTipePenelitianDanPengabdian().getIsi()).setParent(arg0);

			final Vbox hbox1 = new Vbox();
			hbox1.setParent(arg0);
			RevisiHelper.createNewRevisi(PenelitianDanPengabdian.class, penelitianDanPengabdian,
					penelitianDanPengabdian.getJudul()).setParent(hbox1);
			hbox1.appendChild(new Label("Tahun: " + penelitianDanPengabdian.getTahun() + ""));
			hbox1.appendChild(new Label("Untuk: " + penelitianDanPengabdian.getDiperuntukkan()));
			hbox1.appendChild(new Label("TA: " + penelitianDanPengabdian.getTahunAkademik()));
			hbox1.appendChild(new Label("Smt: " + penelitianDanPengabdian.getSemester()));

			String tgl = Common.dateFormat1.get().format(penelitianDanPengabdian.getTanggalMulaiPengajuan()) + " s.d "
					+ Common.dateFormat1.get().format(penelitianDanPengabdian.getTanggalSampaiPengajuan());
			new Label(tgl).setParent(arg0);

			final Hbox myhbox = new Hbox();
			myhbox.setParent(arg0);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Vbox hbox = new Vbox();
					hbox.setParent(myhbox);

					Session session = HibernateUtil.currentSession();
					int pengajuanBelumDiproses = ((Number) session
							.createCriteria(PengajuanPenelitianDanPengabdian.class)
							.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"),
									Restrictions.isNotNull("tbmuser")))
							.add(Restrictions.eq("status", PengajuanPenelitianDanPengabdian.BELUM_DIPROSES))
							.add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					int pengajuanSedangDiproses = ((Number) session
							.createCriteria(PengajuanPenelitianDanPengabdian.class)
							.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"),
									Restrictions.isNotNull("tbmuser")))
							.add(Restrictions.eq("status", PengajuanPenelitianDanPengabdian.SEDANG_DIPROSES))
							.add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					int pengajuanDisetujui = ((Number) session.createCriteria(PengajuanPenelitianDanPengabdian.class)
							.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"),
									Restrictions.isNotNull("tbmuser")))
							.add(Restrictions.eq("status", PengajuanPenelitianDanPengabdian.DISETUJUI))
							.add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					int pengajuanDitolak = ((Number) session.createCriteria(PengajuanPenelitianDanPengabdian.class)
							.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"),
									Restrictions.isNotNull("tbmuser")))
							.add(Restrictions.eq("status", PengajuanPenelitianDanPengabdian.DITOLAK))
							.add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();

					int pengajuanLampiran = ((Number) session.createCriteria(LampiranPenelitianDanPengabdian.class)
							.add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();

					int pengajuanDiskusi = ((Number) session.createCriteria(DiskusiPenelitianDanPengabdian.class)
							.add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();

					hbox.appendChild(new MyLabelAgakKecil(
							"1. Belum diproses " + Common.numberFormat.get().format(pengajuanBelumDiproses)));
					hbox.appendChild(new MyLabelAgakKecil(
							"2. Sedang diproses " + Common.numberFormat.get().format(pengajuanSedangDiproses)));
					hbox.appendChild(
							new MyLabelAgakKecil("3. Disetujui " + Common.numberFormat.get().format(pengajuanDisetujui)));
					hbox.appendChild(
							new MyLabelAgakKecil("4. Ditolak " + Common.numberFormat.get().format(pengajuanDitolak)));
					hbox.appendChild(
							new MyLabelAgakKecil("5. Lampiran " + Common.numberFormat.get().format(pengajuanLampiran)));
					hbox.appendChild(
							new MyLabelAgakKecil("6. Diskusi " + Common.numberFormat.get().format(pengajuanDiskusi)));

					hbox = new Vbox();
					hbox.setParent(myhbox);

					int pengajuanTahap1 = ((Number) session.createCriteria(PengajuanPenelitianDanPengabdian.class)
							.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"),
									Restrictions.isNotNull("tbmuser")))
							.add(Restrictions.eq("tahapPengajuan", PengajuanPenelitianDanPengabdian.TAHAP_PROPOSAL))
							.add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();

					hbox.appendChild(new MyLabelAgakKecil("A. " + PengajuanPenelitianDanPengabdian.TAHAP_PROPOSAL + " "
							+ Common.numberFormat.get().format(pengajuanTahap1)));

					int pengajuanTahap2 = ((Number) session.createCriteria(PengajuanPenelitianDanPengabdian.class)
							.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"),
									Restrictions.isNotNull("tbmuser")))
							.add(Restrictions.eq("tahapPengajuan",
									PengajuanPenelitianDanPengabdian.TAHAP_PENGUMPULAN_DATA))
							.add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();

					hbox.appendChild(
							new MyLabelAgakKecil("B. " + PengajuanPenelitianDanPengabdian.TAHAP_PENGUMPULAN_DATA + " "
									+ Common.numberFormat.get().format(pengajuanTahap2)));

					int pengajuanTahap3 = ((Number) session.createCriteria(PengajuanPenelitianDanPengabdian.class)
							.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"),
									Restrictions.isNotNull("tbmuser")))
							.add(Restrictions.eq("tahapPengajuan",
									PengajuanPenelitianDanPengabdian.TAHAP_ANALISIS_DATA))
							.add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();

					hbox.appendChild(new MyLabelAgakKecil("C. " + PengajuanPenelitianDanPengabdian.TAHAP_ANALISIS_DATA
							+ " " + Common.numberFormat.get().format(pengajuanTahap3)));

					int pengajuanTahap4 = ((Number) session.createCriteria(PengajuanPenelitianDanPengabdian.class)
							.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"),
									Restrictions.isNotNull("tbmuser")))
							.add(Restrictions.eq("tahapPengajuan",
									PengajuanPenelitianDanPengabdian.TAHAP_LAPORAN_AKHIR))
							.add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();

					hbox.appendChild(new MyLabelAgakKecil("D. " + PengajuanPenelitianDanPengabdian.TAHAP_LAPORAN_AKHIR
							+ " " + Common.numberFormat.get().format(pengajuanTahap4)));
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(penelitianDanPengabdian.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					penelitianDanPengabdian.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(penelitianDanPengabdian);
				}
			});

			final MyCheckboxConfig checkboxBuka = new MyCheckboxConfig("Buka");
			checkboxBuka.setChecked(penelitianDanPengabdian.getDibuka());
			checkboxBuka.setParent(arg0);
			checkboxBuka.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					penelitianDanPengabdian.setDibuka(checkboxBuka.isChecked());
					Common.refreshSaveOrUpdate(penelitianDanPengabdian);
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			Session session = HibernateUtil.currentSession();
			for (String username : StringUtils.split(penelitianDanPengabdian.getKorespondensi(), ",")) {
				Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.idEq(username))
						.uniqueResult();
				String oleh = "";
				if (tbmuser != null) {
					oleh = (tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")");
					new Label(i + ". " + oleh).setParent(vbox);
				} else {
					Mahasiswa anggota = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("nim", username)).setMaxResults(1).uniqueResult();
					if (anggota != null) {
						oleh = (anggota.getNim() + " " + anggota.getNama());
						new Label(i + ". " + oleh).setParent(vbox);
					}
				}
				i++;
			}

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(penelitianDanPengabdian);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

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

											Common.refreshDelete(penelitianDanPengabdian);

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
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PenelitianDanPengabdian());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	private void initPendahuluan(Tabpanel parent) {
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(parent);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("90%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun (*)"));
		row.appendChild(tahun = new Intbox(penelitianDanPengabdian.getTahun()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diperuntukkan (*)"));
		row.appendChild(diperuntukkan = new Combobox());
		diperuntukkan(diperuntukkan);
		Common.selectComboItem(diperuntukkan, penelitianDanPengabdian.getDiperuntukkan());
		diperuntukkan.setWidth("90%");
		diperuntukkan.setReadonly(true);

		List<JenisPenelitianDanPengabdian> jenisPenelitianDanPengabdians = new ArrayList<JenisPenelitianDanPengabdian>();
		Session session = HibernateUtil.currentSession();
		List<JenisPenelitianDanPengabdian> temp = session.createCriteria(JenisPenelitianDanPengabdian.class)
				.addOrder(Order.asc("isi")).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (JenisPenelitianDanPengabdian jenisPenelitianDanPengabdian : temp) {
			int count = ((Number) session.createCriteria(JenisPenelitianDanPengabdian.class)
					.add(Restrictions.eq("parent", jenisPenelitianDanPengabdian)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			if (count == 0) {
				jenisPenelitianDanPengabdians.add(jenisPenelitianDanPengabdian);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok (*)"));
		row.appendChild(jenisPenelitianDanPengabdian = new Combobox());
		jenisPenelitianDanPengabdian.setWidth("90%");
		Common.insertComboItems(jenisPenelitianDanPengabdian, new String[] { "isi", "parent" }, "kode",
				jenisPenelitianDanPengabdians);
		Common.selectComboItem(jenisPenelitianDanPengabdian, penelitianDanPengabdian.getJenisPenelitianDanPengabdian());
		jenisPenelitianDanPengabdian.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe (*)"));
		row.appendChild(tipePenelitianDanPengabdian = new Combobox());
		tipePenelitianDanPengabdian.setWidth("90%");
		Common.insertCombo(tipePenelitianDanPengabdian, "isi", "kode", TipePenelitianDanPengabdian.class);
		Common.selectComboItem(tipePenelitianDanPengabdian, penelitianDanPengabdian.getTipePenelitianDanPengabdian());
		tipePenelitianDanPengabdian.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengajuan (*)"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(tanggalMulaiPengajuan = new MyDatebox(
				penelitianDanPengabdian.getTanggalMulaiPengajuan() == null ? ais.ui.util.WaktuUtil.getDate()
						: penelitianDanPengabdian.getTanggalMulaiPengajuan()));
//		tanggalMulaiPengajuan.setConstraint("no empty");

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(tanggalSampaiPengajuan = new MyDatebox(
				penelitianDanPengabdian.getTanggalSampaiPengajuan() == null ? ais.ui.util.WaktuUtil.getDate()
						: penelitianDanPengabdian.getTanggalSampaiPengajuan()));
//		tanggalSampaiPengajuan.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik (*)"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		Common.selectComboItem(tahunAkademik, penelitianDanPengabdian.getTahunAkademik());
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		semester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);

		Common.selectComboItem(semester, penelitianDanPengabdian.getSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (*)"));
		row.appendChild(semester);
		semester.setReadonly(true);

		tanggalMulaiPengajuan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tanggalMulaiPengajuan.getValue() != null) {
					Common.selectComboItem(tahunAkademik,
							Common.getCurrentTahunAkademik(tanggalMulaiPengajuan.getValue()));
					Common.selectComboItem(semester,
							Common.isNowSemensterGanjil(tanggalMulaiPengajuan.getValue()) ? Perkuliahan.GANJIL
									: Perkuliahan.GENAP);
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SKS (* untuk keperluan akreditasi)"));
		row.appendChild(sks = new Intbox(penelitianDanPengabdian.getSks()));
		sks.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Proposal bisa dibaca secara publik"));
		row.appendChild(publik = new MyCheckboxConfig());
		publik.setChecked(penelitianDanPengabdian.getPublik());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kluster (*)"));
		row.appendChild(judul = new Textbox(penelitianDanPengabdian.getJudul()));
		judul.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendahuluan"));
		pendahuluan = new MyCkEditor();
		pendahuluan.setValue(penelitianDanPengabdian.getPendahuluan());
		row.appendChild(pendahuluan);
		pendahuluan.setWidth("98%");
		pendahuluan.setHeight("250px");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Koresponden"));
		row.appendChild(korespondensi = new Textbox(penelitianDanPengabdian.getKorespondensi()));
		korespondensi.setWidth("90%");
		korespondensi.setRows(3);

		if (korespondensi.getValue().trim().isEmpty()) {
			korespondensi.setValue(Common.getCurrentUser().getUserId());
		}

		Common.initKeterangan(rows,
				"Untuk memasukkan banyak Koresponden, masukkan username masing-masing pengguna dengan pemisah tanda koma (,)");

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Koresponden", "/img/user_male_add.png");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tambah Koresponden"));
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								korespondensi.setValue(korespondensi.getValue()
										+ (korespondensi.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Koresponden Grup Pengguna"));
		row.appendChild(
				korespondensiGrupPengguna = new Textbox(penelitianDanPengabdian.getKorespondensiGrupPengguna()));
		korespondensiGrupPengguna.setWidth("90%");
		korespondensiGrupPengguna.setRows(3);
	}

	private void initProfile(Tabpanel parent) {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(parent);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab = new MyTabConfig("Pendahuluan");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Tujuan");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Luaran Penelitian");
		tabs.appendChild(tab);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		initPendahuluan(tabpanel);

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tujuan = new MyCkEditor();
		tujuan.setValue(penelitianDanPengabdian.getTujuan());
		tabpanel.appendChild(tujuan);
		tujuan.setWidth("98%");
		tujuan.setHeight("320px");

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		luaranPenelitian = new MyCkEditor();
		luaranPenelitian.setValue(penelitianDanPengabdian.getLuaranPenelitian());
		tabpanel.appendChild(luaranPenelitian);
		luaranPenelitian.setWidth("98%");
		luaranPenelitian.setHeight("320px");

	}

	private void initProsedur(Tabpanel parent) {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(parent);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab = new MyTabConfig("Kriteria dan Pengusulan");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Sistematika");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Seleksi dan Evaluasi");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Sampul");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Pengesahan");
		tabs.appendChild(tab);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		kriteriaDanPengusulan = new MyCkEditor();
		kriteriaDanPengusulan.setValue(penelitianDanPengabdian.getKriteriaDanPengusulan());
		tabpanel.appendChild(kriteriaDanPengusulan);
		kriteriaDanPengusulan.setWidth("98%");
		kriteriaDanPengusulan.setHeight("320px");

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		sistematika = new MyCkEditor();
		sistematika.setValue(penelitianDanPengabdian.getSistematika());
		tabpanel.appendChild(sistematika);
		sistematika.setWidth("98%");
		sistematika.setHeight("320px");

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		seleksiDanEvaluasi = new MyCkEditor();
		seleksiDanEvaluasi.setValue(penelitianDanPengabdian.getSeleksiDanEvaluasi());
		tabpanel.appendChild(seleksiDanEvaluasi);
		seleksiDanEvaluasi.setWidth("98%");
		seleksiDanEvaluasi.setHeight("320px");

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		sampul = new MyCkEditor();
		sampul.setValue(penelitianDanPengabdian.getSampul());
		tabpanel.appendChild(sampul);
		sampul.setWidth("98%");
		sampul.setHeight("320px");

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		pengesahan = new MyCkEditor();
		pengesahan.setValue(penelitianDanPengabdian.getPengesahan());
		tabpanel.appendChild(pengesahan);
		pengesahan.setWidth("98%");
		pengesahan.setHeight("320px");
	}

	private void initPelaksanaan(Tabpanel parent) {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(parent);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab = new MyTabConfig("Sumber Dana");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Pelaksanaan");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Pelaporan");
		tabs.appendChild(tab);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		sumberDana = new MyCkEditor();
		sumberDana.setValue(penelitianDanPengabdian.getSumberDana());
		tabpanel.appendChild(sumberDana);
		sumberDana.setWidth("98%");
		sumberDana.setHeight("320px");

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		pelaksanaan = new MyCkEditor();
		pelaksanaan.setValue(penelitianDanPengabdian.getPelaksanaan());
		tabpanel.appendChild(pelaksanaan);
		pelaksanaan.setWidth("98%");
		pelaksanaan.setHeight("320px");

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		pelaporan = new MyCkEditor();
		pelaporan.setValue(penelitianDanPengabdian.getPelaporan());
		tabpanel.appendChild(pelaporan);
		pelaporan.setWidth("98%");
		pelaporan.setHeight("320px");

	}

	private void initInstrumenPenilaian(Tabpanel parent) {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(parent);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab = new MyTabConfig("Desk Evaluasi");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Pembahasan");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Monitoring dan Evaluasi");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Kelayakan");
		tabs.appendChild(tab);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		deskEvaluasi = new MyCkEditor();
		deskEvaluasi.setValue(penelitianDanPengabdian.getDeskEvaluasi());
		tabpanel.appendChild(deskEvaluasi);
		deskEvaluasi.setWidth("98%");
		deskEvaluasi.setHeight("320px");

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		pembahasan = new MyCkEditor();
		pembahasan.setValue(penelitianDanPengabdian.getPembahasan());
		tabpanel.appendChild(pembahasan);
		pembahasan.setWidth("98%");
		pembahasan.setHeight("320px");

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		monitoringDanEvaluasi = new MyCkEditor();
		monitoringDanEvaluasi.setValue(penelitianDanPengabdian.getMonitoringDanEvaluasi());
		tabpanel.appendChild(monitoringDanEvaluasi);
		monitoringDanEvaluasi.setWidth("98%");
		monitoringDanEvaluasi.setHeight("320px");

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		kelayakan = new MyCkEditor();
		kelayakan.setValue(penelitianDanPengabdian.getKelayakan());
		tabpanel.appendChild(kelayakan);
		kelayakan.setWidth("98%");
		kelayakan.setHeight("320px");
	}

	private void init(PenelitianDanPengabdian penelitianDanPengabdian) {
		this.penelitianDanPengabdian = penelitianDanPengabdian;
		addWindow.setTitle(penelitianDanPengabdian.getId() == null ? "Tambah Penelitian dan Pengabdian" : "Ubah Penelitian dan Pengabdian");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab = new MyTabConfig("Profile");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Prosedur");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Pelaksanaan");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Instrumen Penilaian");
		tabs.appendChild(tab);

		tab = new MyTabConfig("Lampiran umum");
		tabs.appendChild(tab);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		initProfile(tabpanel);

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		initProsedur(tabpanel);

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		initPelaksanaan(tabpanel);

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		initInstrumenPenilaian(tabpanel);

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		lampiranUmum = new MyCkEditor();
		lampiranUmum.setValue(penelitianDanPengabdian.getLampiranUmum());
		tabpanel.appendChild(lampiranUmum);
		lampiranUmum.setWidth("98%");
		lampiranUmum.setHeight("320px");

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

		if (diperuntukkan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Diperuntukkan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisPenelitianDanPengabdian.getSelectedItem() == null) {
			MyMessageboxConfig.show("Kelompok Penelitian dan Pengabdian harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tipePenelitianDanPengabdian.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tipe Penelitian atau Pengabdian", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (judul.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kluster harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (penelitianDanPengabdian.getId() != null) {
			penelitianDanPengabdian = (PenelitianDanPengabdian) session.load(PenelitianDanPengabdian.class,
					penelitianDanPengabdian.getId());

		}
		penelitianDanPengabdian.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		penelitianDanPengabdian.setSemester((String) semester.getSelectedItem().getValue());
		penelitianDanPengabdian.setSks(sks.getValue());
		penelitianDanPengabdian.setPublik(publik.isChecked());
		penelitianDanPengabdian.setDiperuntukkan((String) diperuntukkan.getSelectedItem().getValue());
		penelitianDanPengabdian.setTahun(tahun.getValue());
		penelitianDanPengabdian.setTanggalMulaiPengajuan(tanggalMulaiPengajuan.getValue());
		penelitianDanPengabdian.setTanggalSampaiPengajuan(tanggalSampaiPengajuan.getValue());
		penelitianDanPengabdian.setTipePenelitianDanPengabdian(
				(TipePenelitianDanPengabdian) tipePenelitianDanPengabdian.getSelectedItem().getValue());
		penelitianDanPengabdian.setJenisPenelitianDanPengabdian(
				(JenisPenelitianDanPengabdian) jenisPenelitianDanPengabdian.getSelectedItem().getValue());
		penelitianDanPengabdian.setJudul(judul.getValue());
		penelitianDanPengabdian.setPendahuluan(pendahuluan.getValue());
		penelitianDanPengabdian.setTujuan(tujuan.getValue());
		penelitianDanPengabdian.setLuaranPenelitian(luaranPenelitian.getValue());
		penelitianDanPengabdian.setKriteriaDanPengusulan(kriteriaDanPengusulan.getValue());
		penelitianDanPengabdian.setSistematika(sistematika.getValue());
		penelitianDanPengabdian.setSeleksiDanEvaluasi(seleksiDanEvaluasi.getValue());
		penelitianDanPengabdian.setSampul(sampul.getValue());
		penelitianDanPengabdian.setPengesahan(pengesahan.getValue());

		penelitianDanPengabdian.setSumberDana(sumberDana.getValue());
		penelitianDanPengabdian.setPelaksanaan(pelaksanaan.getValue());
		penelitianDanPengabdian.setPelaporan(pelaporan.getValue());

		penelitianDanPengabdian.setDeskEvaluasi(deskEvaluasi.getValue());
		penelitianDanPengabdian.setPembahasan(pembahasan.getValue());
		penelitianDanPengabdian.setMonitoringDanEvaluasi(monitoringDanEvaluasi.getValue());
		penelitianDanPengabdian.setKelayakan(kelayakan.getValue());

		penelitianDanPengabdian.setLampiranUmum(lampiranUmum.getValue());

		penelitianDanPengabdian.setKorespondensiGrupPengguna(korespondensiGrupPengguna.getValue());

		Tbmuser tbmuser = Common.getCurrentUser();
		penelitianDanPengabdian.setKorespondensi(
				korespondensi.getValue().trim().isEmpty() ? tbmuser.getUserId() : korespondensi.getValue().trim());

		Common.refreshSaveOrUpdate(session, penelitianDanPengabdian);

		PenelitianDanPengabdianAction.kirimEmailKeKorespondensi(penelitianDanPengabdian);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenelitianDanPengabdian.class);

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchjenis.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
				: Restrictions.eq("jenisPenelitianDanPengabdian", searchjenis.getSelectedItem().getValue()))
				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("judul", searchkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("pendahuluan", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenelitianDanPengabdian> penelitianDanPengabdian = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penelitianDanPengabdian);
		grid.setRowRenderer(new PenelitianDanPengabdianRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static void kirimEmail(final DiskusiPenelitianDanPengabdian diskusiPenelitianDanPengabdian) {
		if (!diskusiPenelitianDanPengabdian.getCatatan().trim().isEmpty()) {
			Common.createDefaultTimer(new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Tbmuser tbmuser = Common.getCurrentUser();
					String emailUser = "";

					JSONArray userIds = new JSONArray();
					userIds.put(tbmuser.getUserId());

					if (tbmuser != null && tbmuser.getEmail() != null
							&& Common.isValidEmailAddress(tbmuser.getEmail())) {
						emailUser += emailUser.trim().isEmpty() ? tbmuser.getEmail().trim()
								: "," + tbmuser.getEmail().trim();
					}

					List<String> emails = diskusiPenelitianDanPengabdian.getPenelitianDanPengabdian().getKorespondensi()
							.trim().isEmpty()
									? new ArrayList<String>()
									: HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
											.add(Restrictions.in("userId",
													diskusiPenelitianDanPengabdian.getPenelitianDanPengabdian()
															.getKorespondensi().trim().split(",")))
											.setProjection(Projections.groupProperty("email")).list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					emails = HibernateUtil.currentSession().createCriteria(DiskusiPenelitianDanPengabdian.class)
							.add(Restrictions.eq("penelitianDanPengabdian",
									diskusiPenelitianDanPengabdian.getPenelitianDanPengabdian()))
							.createAlias("mahasiswa", "mahasiswa")
							.setProjection(Projections.groupProperty("mahasiswa.email")).list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					emails = HibernateUtil.currentSession().createCriteria(DiskusiPenelitianDanPengabdian.class)
							.add(Restrictions.eq("penelitianDanPengabdian",
									diskusiPenelitianDanPengabdian.getPenelitianDanPengabdian()))
							.createAlias("tbmuser", "tbmuser").setProjection(Projections.groupProperty("tbmuser.email"))

							.list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					emails = HibernateUtil.currentSession().createCriteria(DiskusiPenelitianDanPengabdian.class)
							.add(Restrictions.eq("penelitianDanPengabdian",
									diskusiPenelitianDanPengabdian.getPenelitianDanPengabdian()))
							.createAlias("mahasiswaBalasan", "mahasiswaBalasan")
							.setProjection(Projections.groupProperty("mahasiswaBalasan.email")).list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					emails = HibernateUtil.currentSession().createCriteria(DiskusiPenelitianDanPengabdian.class)
							.add(Restrictions.eq("penelitianDanPengabdian",
									diskusiPenelitianDanPengabdian.getPenelitianDanPengabdian()))
							.createAlias("tbmuserBalasan", "tbmuserBalasan")
							.setProjection(Projections.groupProperty("tbmuserBalasan.email"))

							.list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					tbmuser = diskusiPenelitianDanPengabdian.getTbmuser();
					Mahasiswa mahasiswa = diskusiPenelitianDanPengabdian.getMahasiswa();

					// System.out.println("emailUser = " + emailUser);

					if (!emailUser.trim().isEmpty()) {
						String subject = "Komentar penelitian dan pengabdian => "
								+ diskusiPenelitianDanPengabdian.getPenelitianDanPengabdian().getJudul();

						String body = "Komentar dari "
								+ (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")"
										: (mahasiswa == null ? "" : mahasiswa.getNim() + " " + mahasiswa.getNama()));

						body += diskusiPenelitianDanPengabdian.getCatatan() + "<br><br>Isi Pendahuluan<hr>"
								+ diskusiPenelitianDanPengabdian.getPenelitianDanPengabdian().getPendahuluan();

						body += "<br><br>Komentar Lainnya<hr>";

						List<DiskusiPenelitianDanPengabdian> komentars = HibernateUtil.currentSession()
								.createCriteria(DiskusiPenelitianDanPengabdian.class)
								.add(Restrictions.eq("penelitianDanPengabdian",
										diskusiPenelitianDanPengabdian.getPenelitianDanPengabdian()))
								.list();
						body += "<ul>";
						for (DiskusiPenelitianDanPengabdian komentar : komentars) {
							tbmuser = komentar.getTbmuser();
							mahasiswa = komentar.getMahasiswa();
							body += "<li>" + (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")"
									: (mahasiswa == null ? "" : mahasiswa.getNim() + " " + mahasiswa.getNama()));
							body += " : " + komentar.getCatatan() + " "
									+ Common.dateFormat.get().format(komentar.getTanggal());
							body += "</li>";
						}
						body += "</ul>";

						String url = Common
								.getKonfigurasi("alamat_url_sistem_penelitian_dan_pengabdian",
										"http://simlitabmas.ecampus.id")
								.getNilai() + "/penelitian/index?penelitianDanPengabdian="
								+ diskusiPenelitianDanPengabdian.getPenelitianDanPengabdian().getId()
								+ "&jenisPenelitianDanPengabdian=" + diskusiPenelitianDanPengabdian
										.getPenelitianDanPengabdian().getJenisPenelitianDanPengabdian().getId();

						body += "<br><br><hr>Untuk informasi lebih lanjut bisa dilihat di " + url
								+ "<br><br>Terima Kasih";

						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser, diskusiPenelitianDanPengabdian);
					}
				}
			});
		}
	}

	public static void kirimEmailKeKorespondensi(final PenelitianDanPengabdian penelitianDanPengabdian) {
		if (!penelitianDanPengabdian.getPendahuluan().trim().isEmpty()
				&& !penelitianDanPengabdian.getKorespondensi().isEmpty()) {
			Common.createDefaultTimer(new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					String emailUser = "";

					JSONArray userIds = new JSONArray();
					for (String email : penelitianDanPengabdian.getKorespondensi().trim().split(",")) {
						if (!email.trim().isEmpty()) {
							userIds.put(email);
						}
					}

					List<String> emails = HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.in("userId",
									penelitianDanPengabdian.getKorespondensi().trim().split(",")))
							.setProjection(Projections.groupProperty("email")).list();
					for (String email : emails) {
						if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
							emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
						}
					}

					// System.out.println("emailUser = " + emailUser);

					if (!emailUser.trim().isEmpty()) {
						String subject = "Korespondensi penelitian dan pengabdian => "
								+ penelitianDanPengabdian.getJudul();
						Tbmuser tbmuser = Common.getCurrentUser();
						String body = "Anda ditugaskan sebagai koresponsi pada penelitian dan pengabdian \""
								+ penelitianDanPengabdian.getJudul() + "\" oleh "
								+ (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "");

						body += "<br><b>Pendahuluan</b> : <hr>" + penelitianDanPengabdian.getPendahuluan();
						body += "<br><b>Tujuan</b> : <hr>" + penelitianDanPengabdian.getTujuan();
						body += "<br><b>Luaran Penelitian</b> : <hr>" + penelitianDanPengabdian.getLuaranPenelitian();
						body += "<br><b>Kriterian dan Pengusulan</b> : <hr>"
								+ penelitianDanPengabdian.getKriteriaDanPengusulan();
						body += "<br><b>Sistematika</b> : <hr>" + penelitianDanPengabdian.getSistematika();
						body += "<br><b>Seleksi dan Evaluasi</b> : <hr>"
								+ penelitianDanPengabdian.getSeleksiDanEvaluasi();
						body += "<br><b>Sampul</b> : <hr>" + penelitianDanPengabdian.getSampul();
						body += "<br><b>Pengesahan</b> : <hr>" + penelitianDanPengabdian.getPengesahan();
						body += "<br><b>Sumber dana</b> : <hr>" + penelitianDanPengabdian.getSumberDana();
						body += "<br><b>Pelaksanaan</b> : <hr>" + penelitianDanPengabdian.getPelaksanaan();
						body += "<br><b>Pelaporan</b> : <hr>" + penelitianDanPengabdian.getPelaporan();
						body += "<br><b>Desk Evaluasi</b> : <hr>" + penelitianDanPengabdian.getDeskEvaluasi();
						body += "<br><b>Monitoring dan Evaluasi</b> : <hr>"
								+ penelitianDanPengabdian.getMonitoringDanEvaluasi();
						body += "<br><b>Lampiran Umum</b> : <hr>" + penelitianDanPengabdian.getLampiranUmum();

						body += "<br><br>Komentar<hr>";

						List<DiskusiPenelitianDanPengabdian> komentars = HibernateUtil.currentSession()
								.createCriteria(DiskusiPenelitianDanPengabdian.class)
								.add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian)).list();
						body += "<ul>";
						for (DiskusiPenelitianDanPengabdian komentar : komentars) {
							tbmuser = komentar.getTbmuser();
							Mahasiswa mahasiswa = komentar.getMahasiswa();
							body += "<li>" + (tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")"
									: (mahasiswa == null ? "" : mahasiswa.getNim() + " " + mahasiswa.getNama()));
							body += " : " + komentar.getCatatan() + " "
									+ Common.dateFormat.get().format(komentar.getTanggal());
							body += "</li>";
						}
						body += "</ul>";

						String url = Common
								.getKonfigurasi("alamat_url_sistem_penelitian_dan_pengabdian",
										"http://simlitabmas.ecampus.id")
								.getNilai() + "/penelitian/index?penelitianDanPengabdian="
								+ penelitianDanPengabdian.getId() + "&jenisPenelitianDanPengabdian="
								+ penelitianDanPengabdian.getJenisPenelitianDanPengabdian().getId();

						body += "<br><br><hr>Untuk informasi lebih lanjut bisa dilihat di " + url
								+ "<br><br>Terima Kasih";

						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser, penelitianDanPengabdian);
					}
				}
			});
		}
	}

}
