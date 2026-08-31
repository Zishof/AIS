package ais.action.master.bkd;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
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
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.BukuBahanAjarAction;
import ais.action.master.DosenPembimbingAkademikAction;
import ais.action.master.bkd.helper.BkdArtikelHelper;
import ais.action.master.bkd.helper.BkdBimbinganSkripsiHelper;
import ais.action.master.bkd.helper.BkdDosenPaHelper;
import ais.action.master.bkd.helper.BkdKegiatanDosenHelper;
import ais.action.master.bkd.helper.BkdKknHelper;
import ais.action.master.bkd.helper.BkdPenelitianDanPengabdianHelper;
import ais.action.master.bkd.helper.BkdPengajaranHelper;
import ais.action.master.bkd.helper.BkdPengujiProposalSkripsiHelper;
import ais.action.master.bkd.helper.BkdPengujiSkripsiHelper;
import ais.action.master.bkd.helper.BkdPenulisHelper;
import ais.action.master.bkd.helper.BkdPenunjangHelper;
import ais.action.master.bkd.helper.BkdPklHelper;
import ais.action.master.helper.DetailArtikelHelper;
import ais.action.master.helper.DosenMengajarHelper;
import ais.action.master.helper.DosenPerkuliahanHelper;
import ais.action.master.helper.KegiatanKedosenanPunyaDosenHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.kkn.KelompokKknAction;
import ais.action.master.penelitiandanpengabdian.helper.PengajuanPenelitianDanPengabdianHelper;
import ais.action.master.pkl.KelompokPklAction;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.AsesorPenunjangKinerjaDosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk penilaian asesor. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Paging paging}, {@code MyGrid grid},
 * {@code MyCheckboxConfig searchbelum}, {@code Combobox searchTahunAjaran}, {@code Combobox
 * searchJenisSemester}, {@code Combobox searchJenisPenilaian}, {@code Combobox
 * searchAsesorPenunjangKinerjaDosen}, {@code MyCheckboxConfig terdapatSksBeban}; inisialisasi/lifecycle ({@code
 * initJenisPenilaianAsesor()}, {@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); validasi/perhitungan ({@code checkPenilaian()}); mutasi data
 * ({@code prosesUlang()}); operasi domain lain ({@code kasihPenilaian()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PenilaianAsesorAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Paging paging;
	private MyGrid grid;

	// private Textbox pegawai;
	private MyCheckboxConfig searchbelum;
	protected Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;
	protected Combobox searchJenisPenilaian;
	protected Combobox searchAsesorPenunjangKinerjaDosen;

	private MyCheckboxConfig terdapatSksBeban;

	private boolean edit = false;
	private boolean delete = false;

	private Column colPegawai;

	private Pegawai currentPegawai;

	private MyToolbarbuttonConfig find;

	public static String[] contents = new String[] { "asesor.asesorPenunjangKinerjaDosen.nama",
			"asesemenPenilaian.pegawai", "asesemenPenilaian.sks", "asesemenPenilaian.bidang",
			"asesemenPenilaian.keterangan", "asesemenPenilaian.tahunAkademik", "asesemenPenilaian.semester",
			"asesemenPenilaian.semester", "asesemenPenilaian.jenjang", "asesemenPenilaian.masaTugas",
			"asesemenPenilaian.spesifikasi", "asesemenPenilaian.matakuliah", "asesemenPenilaian.perkuliahan",
			"asesemenPenilaian.penunjangKinerjaDosen", "asesemenPenilaian.pengajuanPenelitianDanPengabdian",
			"asesemenPenilaian.kegiatanKedosenanPunyaDosen", "asesemenPenilaian.artikel",
			"asesemenPenilaian.bukuBahanAjar", "asesemenPenilaian.bukti", "asesemenPenilaian.aktif", "pilih", "sks",
			"bukti", "keterangan" };

	public static void initJenisPenilaianAsesor(Combobox searchJenisPenilaian) {
		Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(PenilaianAsesor.ARTIKEL);
		comboitem.setValue(PenilaianAsesor.ARTIKEL);
		searchJenisPenilaian.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(PenilaianAsesor.PENGAJARAN);
		comboitem.setValue(PenilaianAsesor.PENGAJARAN);
		searchJenisPenilaian.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(PenilaianAsesor.PEMBIMBING_TA);
		comboitem.setValue(PenilaianAsesor.PEMBIMBING_TA);
		searchJenisPenilaian.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(PenilaianAsesor.PENGUJI_TA);
		comboitem.setValue(PenilaianAsesor.PENGUJI_TA);
		searchJenisPenilaian.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(PenilaianAsesor.PENGUJI_PROPOSAL_TA);
		comboitem.setValue(PenilaianAsesor.PENGUJI_PROPOSAL_TA);
		searchJenisPenilaian.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(PenilaianAsesor.KEGIATAN_DOSEN);
		comboitem.setValue(PenilaianAsesor.KEGIATAN_DOSEN);
		searchJenisPenilaian.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(PenilaianAsesor.PEMBIMBING_KKN);
		comboitem.setValue(PenilaianAsesor.PEMBIMBING_KKN);
		searchJenisPenilaian.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(PenilaianAsesor.PEMBIMBING_PKL);
		comboitem.setValue(PenilaianAsesor.PEMBIMBING_PKL);
		searchJenisPenilaian.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(PenilaianAsesor.PENULIS_BUKU);
		comboitem.setValue(PenilaianAsesor.PENULIS_BUKU);
		searchJenisPenilaian.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(PenilaianAsesor.PEMBIMBING_AKADEMIK);
		comboitem.setValue(PenilaianAsesor.PEMBIMBING_AKADEMIK);
		searchJenisPenilaian.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(PenilaianAsesor.PENELITIAN_ATAU_PENGABDIAN);
		comboitem.setValue(PenilaianAsesor.PENELITIAN_ATAU_PENGABDIAN);
		searchJenisPenilaian.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(PenilaianAsesor.PENUNJANG_DAN_LAIN_LAIN);
		comboitem.setValue(PenilaianAsesor.PENUNJANG_DAN_LAIN_LAIN);
		searchJenisPenilaian.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchJenisPenilaian.appendChild(comboitem);
		searchJenisPenilaian.setSelectedItem(comboitem);
		searchJenisPenilaian.setReadonly(true);
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private boolean tampilkanAsesor = true;

	@SuppressWarnings({})
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tampilkanAsesor = (Common.bolehKonfigurasi("tampilkan_asesor"));

		if (execution.getParameter("pegawai") != null) {
			currentPegawai = (Pegawai) HibernateUtil.currentSession().createCriteria(Pegawai.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("pegawai")))).uniqueResult();
		}

		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (colPegawai != null) { colPegawai.setVisible(currentPegawai == null); }

		if (searchTahunAjaran != null) { searchTahunAjaran.setReadonly(true); }
		if (searchJenisSemester != null) { searchJenisSemester.setReadonly(true); }

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		Common.generateTahunAjaran(searchTahunAjaran);

		if (execution.getParameter("ta") != null) {
			String ta = execution.getParameter("ta");
			Common.selectComboItem(true, searchTahunAjaran, ta);
			searchTahunAjaran.setDisabled(true);
		}

		if (execution.getParameter("smt") != null) {
			String smt = execution.getParameter("smt");
			Common.selectComboItem(true, searchJenisSemester, smt);
			searchJenisSemester.setDisabled(true);
		}

		initJenisPenilaianAsesor(searchJenisPenilaian);

//		List<AsesorPenunjangKinerjaDosen> asesorPenunjangKinerjaDosens = session
//				.createCriteria(AsesorPenunjangKinerjaDosen.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
//				.addOrder(Order.asc("nama")).list();
//		final TreeMap<String, AsesorPenunjangKinerjaDosen> treeMap = new TreeMap<String, AsesorPenunjangKinerjaDosen>();
//		for (AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen : asesorPenunjangKinerjaDosens) {
//			int asesorCount = ((Number) session.createCriteria(Asesor.class)
//					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
//					.setProjection(Projections.rowCount())
//					.add(Restrictions.eq("asesorPenunjangKinerjaDosen", asesorPenunjangKinerjaDosen))
//					.createAlias("tbmuser", "tbmuser").add(Restrictions.eq("tbmuser.pegawai", currentPegawai))
//					.setMaxResults(1).uniqueResult()).intValue();
//			if (asesorCount > 0) {
//				treeMap.put(asesorPenunjangKinerjaDosen.getNama(), asesorPenunjangKinerjaDosen);
//			}
//		}
//
//		if (treeMap.isEmpty()) {
		Common.insertCombo(searchAsesorPenunjangKinerjaDosen, "nama", AsesorPenunjangKinerjaDosen.class,
				Restrictions.eq("aktif", true));
//		} else {
//			Common.insertComboItems(searchAsesorPenunjangKinerjaDosen, "nama", new ArrayList(treeMap.values()));
//			searchAsesorPenunjangKinerjaDosen.setReadonly(true);
//			searchAsesorPenunjangKinerjaDosen.setSelectedIndex(0);
//		}

		if (execution.getParameter("asesor") != null) {
			String smt = execution.getParameter("asesor");
			Session session = HibernateUtil.currentSession();
			AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen = (AsesorPenunjangKinerjaDosen) session
					.createCriteria(AsesorPenunjangKinerjaDosen.class).add(Restrictions.idEq(Long.parseLong(smt)))
					.uniqueResult();
			Common.selectComboItem(true, searchAsesorPenunjangKinerjaDosen, asesorPenunjangKinerjaDosen);
//			searchAsesorPenunjangKinerjaDosen.setDisabled(true);
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		EventListener uploadListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

			}
		};

		MyToolbarbuttonConfig upload = Common.uploadData(this, PenilaianAsesor.class, uploadListener, contents);
		if (upload != null) { upload.setVisible(edit && Common.getApakahAdmin()); }
		Common.appendKeToolbar(upload, find, comp);

	}

	public static void checkPenilaian(Session session, List<Asesor> asesors, AsesemenPenilaian asesemenPenilaian) {
		for (Asesor asesor : asesors) {

			int count = ((Number) session.createCriteria(PenilaianAsesor.class).add(Restrictions.eq("asesor", asesor))
					.add(Restrictions.eq("asesemenPenilaian", asesemenPenilaian)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			if (count == 0) {
				PenilaianAsesor penilaianAsesor = new PenilaianAsesor();
				penilaianAsesor.setAsesemenPenilaian(asesemenPenilaian);
				penilaianAsesor.setAsesor(asesor);
				penilaianAsesor.setSks(0.0);
				session.getTransaction().begin();
				session.save(penilaianAsesor);
				session.getTransaction().commit();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void prosesUlang(final DataSearchDefault dataSearchDefault, Tbmuser tbmuser,
			final String tahunAkademik, final String semester, final String jenis,
			AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen) {

		Pegawai peg = tbmuser == null ? null : tbmuser.ambilPegawai();

		Session session = HibernateUtil.currentSession();

		final List<Pegawai> asessi = asesorPenunjangKinerjaDosen == null
				|| tbmuser == null
						? new ArrayList<Pegawai>()
						: ConstantValues
								.simpleList(
										session.createCriteria(AsesorPegawai.class).createAlias("asesor", "asesor")
												.add(Restrictions.or(Restrictions.isNull("asesor.aktif"),
														Restrictions.eq("asesor.aktif", true)))
												.setProjection(Projections.groupProperty("pegawai.id"))
												.add(Restrictions.eq("asesor.asesorPenunjangKinerjaDosen",
														asesorPenunjangKinerjaDosen))
												.add(Common.getApakahAdmin() ? Restrictions.sqlRestriction("true")
														: Restrictions.eq("asesor.tbmuser", tbmuser)),
										Pegawai.class, false);

		if (peg != null) {
			asessi.add(peg);
		}

		System.out.println("asessi => " + asessi);

		final Label label = new Label("Sedang mempersiapkan data " + (jenis == null ? "" : jenis) + " ..");
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {

					MyMessageboxConfig.show("Proses ulang data beban kerja pegawai berhasil dilakukan", "Pemberitahuan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									dataSearchDefault.onSearchDefault(null);
								}
							});

					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				for (Pegawai pegawai : asessi) {
					try {

						Session session = HibernateUtil.currentNativeSession();

						if (jenis == null || jenis.equalsIgnoreCase(PenilaianAsesor.PENGAJARAN)) {
							BkdPengajaranHelper.populate(session, pegawai, tahunAkademik, semester, label);
						}

						if (jenis == null || jenis.equalsIgnoreCase(PenilaianAsesor.ARTIKEL)) {
							BkdArtikelHelper.populate(session, pegawai, tahunAkademik, semester, label);
						}

						if (jenis == null || jenis.equalsIgnoreCase(PenilaianAsesor.PEMBIMBING_TA)) {
							BkdBimbinganSkripsiHelper.populate(session, pegawai, tahunAkademik, semester, label);
						}

						if (jenis == null || jenis.equalsIgnoreCase(PenilaianAsesor.PENGUJI_PROPOSAL_TA)) {
							BkdPengujiProposalSkripsiHelper.populate(session, pegawai, tahunAkademik, semester, label);
						}

						if (jenis == null || jenis.equalsIgnoreCase(PenilaianAsesor.PENGUJI_TA)) {
							BkdPengujiSkripsiHelper.populate(session, pegawai, tahunAkademik, semester, label);
						}

						if (jenis == null || jenis.equalsIgnoreCase(PenilaianAsesor.KEGIATAN_DOSEN)) {
							BkdKegiatanDosenHelper.populate(session, pegawai, tahunAkademik, semester, label);
						}

						if (jenis == null || jenis.equalsIgnoreCase(PenilaianAsesor.PEMBIMBING_KKN)) {
							BkdKknHelper.populate(session, pegawai, tahunAkademik, semester, label);
						}

						if (jenis == null || jenis.equalsIgnoreCase(PenilaianAsesor.PEMBIMBING_PKL)) {
							BkdPklHelper.populate(session, pegawai, tahunAkademik, semester, label);
						}

						if (jenis == null || jenis.equalsIgnoreCase(PenilaianAsesor.PEMBIMBING_AKADEMIK)) {
							BkdDosenPaHelper.populate(session, pegawai, tahunAkademik, semester, label);
						}

						if (jenis == null || jenis.equalsIgnoreCase(PenilaianAsesor.PENULIS_BUKU)) {
							BkdPenulisHelper.populate(session, pegawai, tahunAkademik, semester, label);
						}

						if (jenis == null || jenis.equalsIgnoreCase(PenilaianAsesor.PENELITIAN_ATAU_PENGABDIAN)) {
							BkdPenelitianDanPengabdianHelper.populate(session, pegawai, tahunAkademik, semester, label);
						}

						if (jenis == null || jenis.equalsIgnoreCase(PenilaianAsesor.PENUNJANG_DAN_LAIN_LAIN)) {
							BkdPenunjangHelper.populate(session, pegawai, tahunAkademik, semester, label);
						}

					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/bkd/PenilaianAsesorAction.java:464");
					}
				}

				HibernateUtil.closeSession();

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PenilaianAsesorAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PenilaianAsesorAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PenilaianAsesorAction
	 */
	class AsesorMemberikanPenilaianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenilaianAsesor penilaianAsesor = (PenilaianAsesor) arg1;

			Vbox vbox = new Vbox();
			CommonMedia.tampilkanGambarKecil(penilaianAsesor.getAsesemenPenilaian().getPegawai()).setParent(vbox);
			new Label(penilaianAsesor.getAsesemenPenilaian().getBidang() + " ("
					+ penilaianAsesor.getAsesemenPenilaian().getSpesifikasi() + ")").setParent(vbox);
			RevisiHelper.createNewRevisi(PenilaianAsesor.class, penilaianAsesor,
					penilaianAsesor.getAsesemenPenilaian().getPegawai() == null ? ""
							: penilaianAsesor.getAsesemenPenilaian().getPegawai().getNama())
					.setParent(vbox);
			vbox.setParent(arg0);

			Vbox myVbox = new Vbox();
			myVbox.setParent(arg0);
			MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
			myGroupboxStyled.setParent(myVbox);
			myGroupboxStyled
					.appendChild(new MyCaptionStyled("Asesemen " + penilaianAsesor.getAsesemenPenilaian().getBidang()));
			new MyLabelAgakKecil(penilaianAsesor.getAsesemenPenilaian().getKeterangan()).setParent(myGroupboxStyled);

			myGroupboxStyled = new MyGroupboxStyled();
			myGroupboxStyled.setParent(myVbox);
			myGroupboxStyled.appendChild(new MyCaptionStyled(
					"Penilaian " + penilaianAsesor.getAsesor().getAsesorPenunjangKinerjaDosen().getNama()));

			Asesor asesor = penilaianAsesor.getAsesor();
			Hbox hbox = new Hbox();
			hbox.setParent(myGroupboxStyled);

			if (asesor != null && tampilkanAsesor) {
				Tbmuser tbmuser = asesor.getTbmuser();
				CommonMedia.tampilkanGambarKecil(tbmuser.ambilDosen() != null ? tbmuser.ambilDosen()
						: tbmuser.ambilPegawai() != null ? tbmuser.ambilPegawai() : tbmuser).setParent(hbox);
			}

			vbox = new Vbox();
			vbox.setParent(hbox);
			if (asesor != null && tampilkanAsesor) {
				Tbmuser tbmuser = asesor.getTbmuser();
				vbox.appendChild(new Label(tbmuser.ambilDosen() != null ? tbmuser.ambilDosen().getNama()
						: tbmuser.ambilPegawai() != null ? tbmuser.ambilPegawai().getNama() : tbmuser.getUserNama()));
			}

			vbox.appendChild(new Label("Kinerja: " + Common.numberFormat.get().format(penilaianAsesor.getSks()) + " sks"));
			vbox.appendChild(new Label("Bukti: " + penilaianAsesor.getBukti()));
			vbox.appendChild(new Label("Catatan: " + penilaianAsesor.getKeterangan()));

			myVbox = new Vbox();
			myVbox.setParent(arg0);

			final AsesemenPenilaian asesemenPenilaian = penilaianAsesor.getAsesemenPenilaian();
			if (Common.getApakahAdmin() || (currentPegawai != null && currentPegawai.getDosen() != null
					&& asesemenPenilaian.getPegawai() != null && asesemenPenilaian.getPegawai().getDosen() != null
					&& asesemenPenilaian.getPegawai().getDosen().getId().equals(currentPegawai.getDosen().getId()))) {

				Hbox hbox2 = new Hbox();
				hbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("Beban: ")));
				hbox2.setParent(myVbox);
				final MyDoublebox doublebox = new MyDoublebox(asesemenPenilaian.getSks());
				doublebox.setParent(hbox2);
				doublebox.setCols(1);
				doublebox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						asesemenPenilaian.setSks(doublebox.getValue());
						Common.refreshUpdate(asesemenPenilaian);
					}
				});
				hbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("SKS")));
				Combobox combobox = new Combobox();
				combobox.setCols(6);
				Comboitem comboitem = new Comboitem(AsesemenPenilaian.OUTPUT_SELESAI);
				comboitem.setValue(AsesemenPenilaian.OUTPUT_SELESAI);
				combobox.appendChild(comboitem);

				comboitem = new Comboitem(AsesemenPenilaian.OUTPUT_LANJUTKAN);
				comboitem.setValue(AsesemenPenilaian.OUTPUT_LANJUTKAN);
				combobox.appendChild(comboitem);

				comboitem = new Comboitem(AsesemenPenilaian.OUTPUT_DIPERBAIKI);
				comboitem.setValue(AsesemenPenilaian.OUTPUT_DIPERBAIKI);
				combobox.appendChild(comboitem);

				comboitem = new Comboitem(AsesemenPenilaian.OUTPUT_DITOLAK);
				comboitem.setValue(AsesemenPenilaian.OUTPUT_DITOLAK);
				combobox.appendChild(comboitem);

				comboitem = new Comboitem(AsesemenPenilaian.OUTPUT_GAGAL);
				comboitem.setValue(AsesemenPenilaian.OUTPUT_GAGAL);
				combobox.appendChild(comboitem);

				comboitem = new Comboitem(AsesemenPenilaian.OUTPUT_LAINNYA);
				comboitem.setValue(AsesemenPenilaian.OUTPUT_LAINNYA);
				combobox.appendChild(comboitem);

				comboitem = new Comboitem(AsesemenPenilaian.OUTPUT_BEBAN_LEBIH);
				comboitem.setValue(AsesemenPenilaian.OUTPUT_BEBAN_LEBIH);
				combobox.appendChild(comboitem);

				hbox2 = new Hbox();
				hbox2.setParent(myVbox);

				hbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("Rekomendasi: ")));
				combobox.setParent(hbox2);

				combobox.setReadonly(true);
				Common.selectComboItem(combobox, asesemenPenilaian.getOutput());

				combobox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Combobox combobox = (Combobox) arg0.getTarget();

						asesemenPenilaian.setOutput((String) (combobox.getSelectedItem() == null ? null
								: combobox.getSelectedItem().getValue()));
						Common.refreshUpdate(asesemenPenilaian);
					}
				});

			} else {

				Hbox hbox2 = new Hbox();
				hbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("Beban: ")));
				hbox2.setParent(myVbox);

				hbox2.appendChild(new Label(asesemenPenilaian.getSks() == null ? ""
						: Common.numberFormat.get().format(asesemenPenilaian.getSks()) + " sks"));

				hbox2 = new Hbox();
				hbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("Rekomendasi: ")));
				hbox2.setParent(myVbox);
				myVbox.appendChild(new Label(asesemenPenilaian.getOutput()));
			}

			final MyToolbarbuttonConfig buttonPenilaian = new MyToolbarbuttonConfig("Proses Penilaian",
					"/img/svg/edit-box-line.svg");

			Hbox hbox2 = new Hbox();
			hbox2.setParent(myVbox);

			hbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("Status: ")));

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(penilaianAsesor.getAsesemenPenilaian().getAktif());
			checkbox.setParent(hbox2);
			myVbox.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					AsesemenPenilaian asesemenPenilaian = penilaianAsesor.getAsesemenPenilaian();
					asesemenPenilaian.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(asesemenPenilaian);
					buttonPenilaian.setVisible(edit && asesemenPenilaian.getAktif());
				}
			});

			hbox2 = new Hbox();
			hbox2.setParent(myVbox);

			hbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan: ")));

			final Textbox buktiBeban = new Textbox(penilaianAsesor.getAsesemenPenilaian().getBukti());
			buktiBeban.setDisabled(!edit);
			buktiBeban.setParent(hbox2);
			buktiBeban.setCols(10);
			buktiBeban.setRows(2);
			buktiBeban.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					AsesemenPenilaian asesemenPenilaian = penilaianAsesor.getAsesemenPenilaian();
					asesemenPenilaian.setBukti(buktiBeban.getValue());
					Common.refreshSaveOrUpdate(asesemenPenilaian);
				}
			});

			hbox2 = new Hbox();
			hbox2.setParent(myVbox);

			hbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("Masa Tugas: ")));

			final Textbox masaTugas = new Textbox(penilaianAsesor.getAsesemenPenilaian().getMasaTugas());
			masaTugas.setDisabled(!edit);
			masaTugas.setParent(hbox2);
			masaTugas.setCols(10);
			masaTugas.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					AsesemenPenilaian asesemenPenilaian = penilaianAsesor.getAsesemenPenilaian();
					asesemenPenilaian.setMasaTugas(masaTugas.getValue());
					Common.refreshSaveOrUpdate(asesemenPenilaian);
				}
			});

			// Kolom aksi rapi: semua tombol dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			buttonPenilaian.setTooltiptext("Ubah Data");
			buttonPenilaian.setVisible(edit && penilaianAsesor.getAsesemenPenilaian().getAktif());
			buttonPenilaian.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					PenilaianAsesorAction.kasihPenilaian(penilaianAsesor, PenilaianAsesorAction.this);
				}

			});
			aksiButtons.add(buttonPenilaian);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
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
											Common.refreshDelete(penilaianAsesor);
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
			ais.ui.util.UIHelper.buatBarisAksi(myVbox, 3, aksiButtons);

		}

	}

	public static void kasihPenilaian(PenilaianAsesor penilaianAsesor, final DataSearchDefault dataSearchDefault)
			throws Exception {
		Pegawai pegawai = penilaianAsesor.getAsesemenPenilaian().getPegawai();
		final MyWindow addWindow = new MyWindow("Penilaian Kinerja Pegawai", "none", false);
		addWindow.setHeight("97%");
		addWindow.setWidth("90%");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("280px");
		north.setAutoscroll(true);

		Hbox hbox = new Hbox();
		hbox.setPack("start");
		hbox.setAlign("center");
		north.appendChild(hbox);
		CommonMedia.tampilkanGambarKecil(penilaianAsesor.getAsesemenPenilaian().getPegawai()).setParent(hbox);
		hbox.appendChild(new Label(penilaianAsesor.getAsesemenPenilaian().getKeterangan()));

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		if (pegawai != null && pegawai.getDosen() != null
				&& penilaianAsesor.getAsesemenPenilaian().getPerkuliahan() != null) {

			DosenPerkuliahanHelper
					.createDetail(pegawai.getDosen(), penilaianAsesor.getAsesemenPenilaian().getPerkuliahan(),
							penilaianAsesor.getAsesemenPenilaian().getJenjang(),
							penilaianAsesor.getAsesemenPenilaian().getTahunAkademik(),
							penilaianAsesor.getAsesemenPenilaian().getSemester(), new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub

								}
							})
					.setParent(center);
		} else if (pegawai != null && pegawai.getDosen() != null
				&& penilaianAsesor.getAsesemenPenilaian().getMatakuliah() != null) {

			DosenMengajarHelper.createDetail(pegawai.getDosen(), penilaianAsesor.getAsesemenPenilaian().getMatakuliah(),
					penilaianAsesor.getAsesemenPenilaian().getJenjang(),
					penilaianAsesor.getAsesemenPenilaian().getTahunAkademik(),
					penilaianAsesor.getAsesemenPenilaian().getSemester(), new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							// TODO Auto-generated method stub

						}
					}).setParent(center);
		} else if (pegawai != null && penilaianAsesor.getAsesemenPenilaian().getArtikel() != null) {
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(row);
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			final MyTabConfig tabSoal = new MyTabConfig("Penilaian Asesor");
			tabSoal.setParent(tabs);

			final MyTabConfig tabPengajaran = new MyTabConfig("Rincian Data Artikel");
			tabPengajaran.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
			tabpanelUtama.setStyle("min-height: 500px;");
			tabpanelUtama.setParent(tabpanels);

			Row r = Common.tampilanScroll1(tabpanelUtama);

			Artikel artikel = penilaianAsesor.getAsesemenPenilaian().getArtikel();

			Session session = HibernateUtil.currentSession();
			session.refresh(artikel);

			DetailArtikelHelper.displayRow(r, artikel, pegawai, true);

			tabpanelUtama = new ais.ui.util.MyTabpanel();
			tabpanelUtama.setStyle("min-height: 500px;");
			tabpanelUtama.setParent(tabpanels);

			r = Common.tampilanScroll1(tabpanelUtama);
			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			save.setAttribute("parent", r);
			new DetailArtikelHelper(null).form(artikel, artikel.getDisposisiSop(), save, null);

		}

		else if (pegawai != null
				&& penilaianAsesor.getAsesemenPenilaian().getSpesifikasi().equals(PenilaianAsesor.PEMBIMBING_TA)) {
			BimbinganSkripsiAction.displayRow(center, penilaianAsesor);
		}

		else if (pegawai != null && penilaianAsesor.getAsesemenPenilaian().getSpesifikasi()
				.equals(PenilaianAsesor.PENGUJI_PROPOSAL_TA)) {
			BimbinganSkripsiAction.displayRow(center, penilaianAsesor);
		}

		else if (pegawai != null
				&& penilaianAsesor.getAsesemenPenilaian().getSpesifikasi().equals(PenilaianAsesor.PENGUJI_TA)) {
			PengujiSkripsiAction.displayRow(center, penilaianAsesor);
		}

		else if (pegawai != null
				&& penilaianAsesor.getAsesemenPenilaian().getSpesifikasi().equals(PenilaianAsesor.KEGIATAN_DOSEN)) {
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			KegiatanKedosenanPunyaDosenHelper.displayRow(row,
					penilaianAsesor.getAsesemenPenilaian().getKegiatanKedosenanPunyaDosen(), pegawai, true, false,
					null);
		}

		else if (pegawai != null
				&& penilaianAsesor.getAsesemenPenilaian().getSpesifikasi().equals(PenilaianAsesor.PEMBIMBING_KKN)) {
			KelompokKknAction.displayRow(center, penilaianAsesor);
		}

		else if (pegawai != null
				&& penilaianAsesor.getAsesemenPenilaian().getSpesifikasi().equals(PenilaianAsesor.PEMBIMBING_PKL)) {
			KelompokPklAction.displayRow(center, penilaianAsesor);
		}

		else if (pegawai != null && penilaianAsesor.getAsesemenPenilaian().getSpesifikasi()
				.equals(PenilaianAsesor.PEMBIMBING_AKADEMIK)) {
			DosenPembimbingAkademikAction.displayRow(center, penilaianAsesor);
		}

		else if (pegawai != null
				&& penilaianAsesor.getAsesemenPenilaian().getPengajuanPenelitianDanPengabdian() != null) {

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(row);
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			final MyTabConfig tabSoal = new MyTabConfig("Penilaian Asesor");
			tabSoal.setParent(tabs);

			final MyTabConfig tabPengajaran = new MyTabConfig("Rincian Data Penelitian/Pengabdian");
			tabPengajaran.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
			tabpanelUtama.setStyle("min-height: 500px;");
			tabpanelUtama.setParent(tabpanels);

			Row r = Common.tampilanScroll1(tabpanelUtama);

			PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian = penilaianAsesor.getAsesemenPenilaian()
					.getPengajuanPenelitianDanPengabdian();

			Session session = HibernateUtil.currentSession();
			session.refresh(pengajuanPenelitianDanPengabdian);

			PengajuanPenelitianDanPengabdianHelper.displayRow(r, pengajuanPenelitianDanPengabdian, pegawai, true,
					pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getTipePenelitianDanPengabdian());

			tabpanelUtama = new ais.ui.util.MyTabpanel();
			tabpanelUtama.setStyle("min-height: 500px;");
			tabpanelUtama.setParent(tabpanels);

//			TipePenelitianDanPengabdian jenis = penilaianAsesor.getAsesemenPenilaian()
//					.getPengajuanPenelitianDanPengabdian().getPenelitianDanPengabdian()
//					.getTipePenelitianDanPengabdian();

			r = Common.tampilanScroll1(tabpanelUtama);

			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			save.setAttribute("parent", r);
			(new PengajuanPenelitianDanPengabdianHelper()).form(pengajuanPenelitianDanPengabdian,
					pengajuanPenelitianDanPengabdian.getDisposisiSop(), save, null);

//			(new PengajuanPenelitianDanPengabdianHelper()).displayWindowPengajuan(r,
//					pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian(), pengajuanPenelitianDanPengabdian,
//					jenis);

		}

		else if (pegawai != null && penilaianAsesor.getAsesemenPenilaian().getBukuBahanAjar() != null) {
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			BukuBahanAjarAction.displayRow(row, penilaianAsesor.getAsesemenPenilaian().getBukuBahanAjar(), pegawai,
					true);
		}

		else if (pegawai != null && penilaianAsesor.getAsesemenPenilaian().getPenunjangKinerjaDosen() != null) {
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			PenunjangKinerjaDosenAction.displayRow(row,
					penilaianAsesor.getAsesemenPenilaian().getPenunjangKinerjaDosen(), true);

			north.setVisible(false);
		}

		South south = new South();
		south.setHeight("25px");
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
				dataSearchDefault.onSearchDefault(null);
			}
		});
		toolbar.appendChild(cancel);
		addWindow.onModal();
	}

	public Criteria initCriteria(boolean order) {

		AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen = (AsesorPenunjangKinerjaDosen) (searchAsesorPenunjangKinerjaDosen
				.getSelectedItem() == null ? null : searchAsesorPenunjangKinerjaDosen.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PenilaianAsesor.class)
				.createAlias("asesemenPenilaian", "asesemenPenilaian")
				.add(terdapatSksBeban.isChecked() ? Restrictions.gt("asesemenPenilaian.sks", 0.1)
						: Restrictions.sqlRestriction("true"))
				.createAlias("asesor", "asesor").add(Restrictions.eq("asesemenPenilaian.pegawai", currentPegawai))
				.createAlias("asesemenPenilaian.pegawai", "pegawai");
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(
				searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("asesemenPenilaian.tahunAkademik",
								searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchJenisSemester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("asesemenPenilaian.semester",
								searchJenisSemester.getSelectedItem().getValue()))

				.add(searchJenisPenilaian.getSelectedItem() == null
						|| searchJenisPenilaian.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("asesemenPenilaian.spesifikasi",
										searchJenisPenilaian.getSelectedItem().getValue()))

				.add(asesorPenunjangKinerjaDosen == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("asesor.asesorPenunjangKinerjaDosen", asesorPenunjangKinerjaDosen))

				.add(searchbelum.isChecked() ? Restrictions.le("sks", 0.1) : Restrictions.sqlRestriction("true"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenilaianAsesor> penilaianAsesor = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penilaianAsesor);
		grid.setRowRenderer(new AsesorMemberikanPenilaianRenderer());
		grid.setModelCheckMobile(strset);

	}

}
