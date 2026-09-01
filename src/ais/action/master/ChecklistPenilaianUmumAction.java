package ais.action.master;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AngketPenilaianUmum;
import ais.database.model.ChecklistPenilaianUmum;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.SubGrupChecklistPenilaianUmum;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk checklist penilaian umum. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchjurusan}, {@code Combobox
 * searchprogram}, {@code Combobox searchfakultas}, {@code Combobox searchyayasan}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onSubGrupAngketUmum()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
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
public class ChecklistPenilaianUmumAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchjurusan;
	private Combobox searchprogram;
	private Combobox searchfakultas;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Checkbox searchaktif;

	private Row hbFakultasLabel;
	private Row hbYayasan;

	private Textbox nama;
	private Intbox nomorUrut;
	private Combobox grupChecklistPenilaianUmum;
	private Combobox searchGrup;
	private Combobox searchDiperuntukkan;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private ChecklistPenilaianUmum checklistPenilaianUmum;
	private MyToolbarbuttonConfig add;
	private boolean pt = false;
	private boolean ya = false;
	private ArrayList<String> diperuntukkans;
	private JSONObject pilihan;
	private Row rowPilihan;
	private Combobox subGrupChecklistPenilaianUmum;

	private Tabpanel subGrupAngketUmum;

	public void onSubGrupAngketUmum(Event event) {
		if (subGrupAngketUmum != null && subGrupAngketUmum.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(subGrupAngketUmum);
			MyInclude iframe = new MyInclude("/pages/master/sub_grup_checklist_penilaian_umum.zul");
			iframe.setParent(window);
		}
	}

	public static String[] contents = new String[] { "id", "isi", "nomorUrut", "grupChecklistPenilaianUmum",
			"subGrupChecklistPenilaianUmum", "aktif", "keterangan", "pilihan" };

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

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }
		if (hbYayasan != null) { hbYayasan.setVisible(ya); }

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(AngketPenilaianUmum.class).add(Restrictions.eq("kode", "002.000"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {
			AngketPenilaianUmum angketPenilaianUmum = new AngketPenilaianUmum();
			angketPenilaianUmum.setKode("002.000");
			angketPenilaianUmum.setIsi("ANGKET PENILAIAN KUALITAS DAN KEPUASAN LULUSAN");
			angketPenilaianUmum.setPetunjuk("Yth. Sdr/i lulusan\n\n"
					+ "Dalam upaya memperoleh masukan untuk perencanaan dan evaluasi kinerja dalam memberikan layanan kepada para pemangku kepentingan khususnya mahasiswa, kampus melalui unit Akademik dan Kemahasiswaan melakukan survei tentang penilaian kualitas dan kepuasan bagi para lulusan/alumni. Untuk itu kami berharap Saudara/i berkenan melengkapi angket ini dengan memberikan jawaban yang sejujurnya sesuai dengan yang dialami selama menjadi mahasiswa. Jawaban sejujurnya akan sangat bermanfaat bagi peningkatan kualitas layanan di masa yang akan datang.\n\n"
					+ "Sesuai dengan yang Saudara ketahui, berilah penilaian secara jujur, objektif, dan penuh tanggung jawab. Penilaian dilakukan terhadap aspek-aspek dalam tabel berikut dengan cara memilih angka (1-5) pada kolom skor.  "
					+ "\n 1 = sangat tidak baik/sangat rendah/tidak pernah\n 2 = tidak baik/rendah/jarang "
					+ "\n 3 = biasa/cukup/kadang-kadang \n 4 = baik/tinggi/sering "
					+ "\n 5 = sangat baik/sangat tinggi/selalu");
			session.save(angketPenilaianUmum);

			GrupChecklistPenilaianUmum grupChecklistPenilaianUmum = new GrupChecklistPenilaianUmum();
			grupChecklistPenilaianUmum.setAngketPenilaianUmum(angketPenilaianUmum);
			grupChecklistPenilaianUmum.setKode("002.001");
			grupChecklistPenilaianUmum.setIsi("Pendidikan dan Pengajaran");
			grupChecklistPenilaianUmum.setDiperuntukkan(GrupChecklistPenilaianUmum.UNTUK_ALUMNI);
			session.save(grupChecklistPenilaianUmum);

			String text = "D o s e n;" + "Kurikulum;" + "Staf Pendukung (Laboran);" + "Staf Pendukung (Asisten);"
					+ "Teori untuk menunjang pengetahuan dan keterampilan;"
					+ "Praktikum di dalam kampus untuk menunjang pengetahuan dan keterampilan;"
					+ "Praktek lapangan (field lab.) untuk menunjang pengetahuan dan keterampilan;"
					+ "Fasilitas perkuliahan (ruang kuliah, fasilitas audio-visual);"
					+ "Fasilitas laboratorium dalam kampus (ruang lab, bahan praktikum);" + "Pembimbing Akademik;"
					+ "Kemudahan komunikasi/konsultasi dengan dosen, baik di dalam maupun di luar jam kuliah;"
					+ "Kemudahan komunikasi/konsultasi antara karyawan dengan mahasiswa;"
					+ "Kesediaan dosen untuk menyelesaikan masalah yang dihadapi mahasiswa;"
					+ "Kesediaan karyawan untuk menyelesaikan masalah yang dihadapi mahasiswa;"
					+ "Kesediaan pimpinan fakultas untuk menyelesaikan masalah yang dihadapi mahasiswa;"
					+ "Profesionalisme dan pengetahuan yang luas dari staf karyawan dan dosen;"
					+ "Adanya jaminan keamanan dan kenyamanan bagi Mahasiswa;"
					+ "Program Studi selalu berupaya untuk meningkatkan daya saing para lulusannya.";
			for (String s : text.split(";")) {
				ChecklistPenilaianUmum checklistPenilaianUmum = new ChecklistPenilaianUmum();
				checklistPenilaianUmum.setGrupChecklistPenilaianUmum(grupChecklistPenilaianUmum);
				checklistPenilaianUmum.setIsi(s);
				session.save(checklistPenilaianUmum);
			}

			grupChecklistPenilaianUmum = new GrupChecklistPenilaianUmum();
			grupChecklistPenilaianUmum.setAngketPenilaianUmum(angketPenilaianUmum);
			grupChecklistPenilaianUmum.setKode("002.002");
			grupChecklistPenilaianUmum.setIsi("Administrasi");
			grupChecklistPenilaianUmum.setDiperuntukkan(GrupChecklistPenilaianUmum.UNTUK_ALUMNI);
			session.save(grupChecklistPenilaianUmum);

			text = "Pelayanan secara menyeluruh dari pegawai;" + "Jadwal perkuliahan;" + "Jadwal Ujian;"
					+ "Pengumuman Nilai;" + "Penyebaran informasi;" + "Pelayanan akademik;" + "Pelayanan kemahasiswaan;"
					+ "Pelayanan kesehatan";
			for (String s : text.split(";")) {
				ChecklistPenilaianUmum checklistPenilaianUmum = new ChecklistPenilaianUmum();
				checklistPenilaianUmum.setGrupChecklistPenilaianUmum(grupChecklistPenilaianUmum);
				checklistPenilaianUmum.setIsi(s);
				session.save(checklistPenilaianUmum);
			}

			grupChecklistPenilaianUmum = new GrupChecklistPenilaianUmum();
			grupChecklistPenilaianUmum.setAngketPenilaianUmum(angketPenilaianUmum);
			grupChecklistPenilaianUmum.setKode("002.003");
			grupChecklistPenilaianUmum.setIsi("Fasilitas Mahasiswa");
			grupChecklistPenilaianUmum.setDiperuntukkan(GrupChecklistPenilaianUmum.UNTUK_ALUMNI);
			session.save(grupChecklistPenilaianUmum);

			text = "Fasilitas computer dan internet;" + "Kegiatan ekstra-kurikuler penunjang akademik;"
					+ "Organisasi kemahasiswaan;" + "Konsultasi (konseling) mahasiswa;"
					+ "Sumber pustaka di perpustakaan (buku, jurnal, bulletin, fasilitas on-line);"
					+ "Pelayanan perpustakaan;"
					+ "Fasilitas yang dimiliki oleh kampus (Gedung, Laboratorium, Tempat parkir dan Papan pengumuman) cukup memadai;"
					+ "Fasilitas olah raga, seni dan rekreasi;" + "Kantin, kafetaria;"
					+ "Keamanan dan keselamatan kampus;" + "Jalur untuk memberikan keluhan dan umpan balik;"
					+ "Beasiswa (informasi dan pelayanan);" + "Pelayanan Bank dan Pos;" + "Aktivitas Alumni";
			for (String s : text.split(";")) {
				ChecklistPenilaianUmum checklistPenilaianUmum = new ChecklistPenilaianUmum();
				checklistPenilaianUmum.setGrupChecklistPenilaianUmum(grupChecklistPenilaianUmum);
				checklistPenilaianUmum.setIsi(s);
				session.save(checklistPenilaianUmum);
			}
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		GrupChecklistPenilaianUmumAction.diperuntukkan(searchDiperuntukkan);
		GrupChecklistPenilaianUmumAction.diperuntukkanPertemuan(searchDiperuntukkan);

		searchDiperuntukkan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				LogicalExpression crit = Restrictions.and(
						searchDiperuntukkan.getSelectedItem() == null
								|| searchDiperuntukkan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("diperuntukkan",
												searchDiperuntukkan.getSelectedItem().getValue()),
						Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

				Common.insertCombo(searchGrup, "isi", "diperuntukkan", GrupChecklistPenilaianUmum.class, crit);
				onSearchDefault(null);
			}
		});

		diperuntukkans = new ArrayList<String>();
		if (pt) {
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_ALUMNI);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_MAHASISWA);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_DOSEN);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_ADMIN);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_ASISTEN);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_PERKULIAHAN);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_BIMBINGAN);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_SIDANG);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_KKN);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_PKL);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_AKADEMIK);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_KEGIATAN);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_WISUDA);

		}

		if (ya) {
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_PELAJARAN);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_SISWA);
			diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_GURU);
		}
		diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_LINK_UMUM);
		diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_UMUM);
		diperuntukkans.add(GrupChecklistPenilaianUmum.UNTUK_ORANG_TUA);

		Common.insertCombo(grupChecklistPenilaianUmum = new Combobox(), "isi", "diperuntukkan",
				GrupChecklistPenilaianUmum.class,

				Restrictions.and(Restrictions.in("diperuntukkan", diperuntukkans),
						Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))

				));

		Common.insertComboDanSemua(searchGrup, "isi", "diperuntukkan", GrupChecklistPenilaianUmum.class,

				Restrictions.and(Restrictions.in("diperuntukkan", diperuntukkans),
						Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))

				));
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.initPrograms(searchprogram);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(ChecklistPenilaianUmum.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final Label peringatan = new Label("");

							final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
							Clients.showBusy(label.getValue());
							final Timer timer = new Timer(200);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							timer.setRepeats(true);
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.showBusy(label.getValue());
									if (label.getValue().isEmpty()) {
										System.out.println("loading file " + file.getAbsolutePath());
										MyMessageboxConfig.show("Upload data berhasil dilakukan."
												+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
												new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														onSearchDefault(arg0);
													}
												});
										Clients.clearBusy();
										timer.detach();
									}

								}
							});
							timer.start();

							Thread uploadThread = new Thread(new Runnable() {

								@Override
								public void run() {
									try {

									try {

										XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
										XSSFSheet sheet = workbook.getSheetAt(0);

										ClassMetadata classMetadata = HibernateUtil
												.getClassMetadata(ChecklistPenilaianUmum.class);
										Session session = HibernateUtil.currentNativeSession();

										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 1; i < rowCount; i++) {
											try {

												Long id = Common.getSheetContentAsLong(sheet, 0, i);
												ChecklistPenilaianUmum checklistPenilaianUmum = id == null
														|| id.equals(-1L)
																? null
																: (ChecklistPenilaianUmum) session
																		.createCriteria(ChecklistPenilaianUmum.class)
																		.add(Restrictions.idEq(id)).uniqueResult();

												if (checklistPenilaianUmum == null) {
													checklistPenilaianUmum = new ChecklistPenilaianUmum();
												}

												Common.setObjectValues(classMetadata, checklistPenilaianUmum, contents,
														1, sheet, i);

												session.getTransaction().begin();
												session.saveOrUpdate(checklistPenilaianUmum);
												session.getTransaction().commit();

												label.setValue("Upload data \"" + checklistPenilaianUmum.getKode()
														+ " - " + checklistPenilaianUmum.getNama() + "\" ("
														+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}

										}
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/ChecklistPenilaianUmumAction.java:425");
									}

									HibernateUtil.closeSession();

									label.setValue("");
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}, "ais-upload-angket-checklist");
							uploadThread.setDaemon(true);
							uploadThread.start();

						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link ChecklistPenilaianUmumAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ChecklistPenilaianUmumAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see ChecklistPenilaianUmumAction
	 */
	class ChecklistPenilaianUmumRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ChecklistPenilaianUmum checklistPenilaianUmum = (ChecklistPenilaianUmum) arg1;

			RevisiHelper.createNewRevisi(ChecklistPenilaianUmum.class, checklistPenilaianUmum,
					checklistPenilaianUmum.getIsi()).setParent(arg0);
			new Label(checklistPenilaianUmum.getNomorUrut() == null ? ""
					: checklistPenilaianUmum.getNomorUrut().toString()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(checklistPenilaianUmum.getGrupChecklistPenilaianUmum() == null ? ""
					: checklistPenilaianUmum.getGrupChecklistPenilaianUmum().getIsi()).setParent(vbox);

			new Label(checklistPenilaianUmum.getSubGrupChecklistPenilaianUmum() == null ? ""
					: checklistPenilaianUmum.getSubGrupChecklistPenilaianUmum().getNama()).setParent(vbox);

			AngketPenilaianUmum angketPenilaianUmum = checklistPenilaianUmum.getGrupChecklistPenilaianUmum() == null
					? null
					: checklistPenilaianUmum.getGrupChecklistPenilaianUmum().getAngketPenilaianUmum();

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(angketPenilaianUmum == null || angketPenilaianUmum.getFakultas() == null ? ""
					: angketPenilaianUmum.getFakultas().getNama()).setParent(vbox);
			new Label(angketPenilaianUmum == null || angketPenilaianUmum.getYayasan() == null ? ""
					: angketPenilaianUmum.getYayasan().getNama()).setParent(vbox);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(angketPenilaianUmum == null || angketPenilaianUmum.getJurusan() == null ? ""
					: angketPenilaianUmum.getJurusan().getNama()).setParent(vbox);
			new Label(angketPenilaianUmum == null || angketPenilaianUmum.getSekolah() == null ? ""
					: angketPenilaianUmum.getSekolah().getNama()).setParent(vbox);

			new Label(angketPenilaianUmum == null || angketPenilaianUmum.getProgram() == null
					|| angketPenilaianUmum.getProgram().trim().isEmpty() ? "" : angketPenilaianUmum.getProgram())
					.setParent(arg0);

			JSONObject pilihan = new JSONObject(checklistPenilaianUmum.getPilihan());
			Hbox hbox = new Hbox();
			arg0.appendChild(hbox);
			GrupChecklistPenilaianUmum grup = checklistPenilaianUmum.getGrupChecklistPenilaianUmum();
			if (grup != null && grup.getAngketPenilaianUmum() != null) {
				for (int i = 1; i <= grup.getAngketPenilaianUmum().getJumlahPilihan(); i++) {
					Label myTextbox = new Label(pilihan.isNull(i + "") ? i + "" : pilihan.getString(i + ""));
					hbox.appendChild(myTextbox);
				}
			}

			new Label(checklistPenilaianUmum.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(checklistPenilaianUmum.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					checklistPenilaianUmum.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(checklistPenilaianUmum);
				}
			});

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
					init(checklistPenilaianUmum);
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

											Common.refreshDelete(checklistPenilaianUmum);

											// agamaDao.commitTransaction();
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
		init(new ChecklistPenilaianUmum());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(ChecklistPenilaianUmum checklistPenilaianUmum) throws Exception {
		this.checklistPenilaianUmum = checklistPenilaianUmum;
		addWindow.setTitle(checklistPenilaianUmum.getId() == null ? "Tambah Angket Penilaian Umum" : "Ubah Angket Penilaian Umum");
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Isi *"));
		row.appendChild(
				nama = new Textbox(checklistPenilaianUmum.getIsi() == null ? "" : checklistPenilaianUmum.getIsi()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut"));
		nomorUrut = new Intbox();
		if (checklistPenilaianUmum.getNomorUrut() != null) {
			nomorUrut.setValue(checklistPenilaianUmum.getNomorUrut());
		}
		row.appendChild(nomorUrut);
		nomorUrut.setCols(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Angket Umum *"));
		row.appendChild(grupChecklistPenilaianUmum);
		Common.selectComboItem(grupChecklistPenilaianUmum,
				checklistPenilaianUmum.getGrupChecklistPenilaianUmum() == null ? null
						: checklistPenilaianUmum.getGrupChecklistPenilaianUmum());
		grupChecklistPenilaianUmum.setWidth("90%");
		grupChecklistPenilaianUmum.setReadonly(true);

		if (searchGrup != null && searchGrup.getSelectedItem() != null
				&& searchGrup.getSelectedItem().getValue() != null) {
			Common.selectComboItem(true, grupChecklistPenilaianUmum, searchGrup.getSelectedItem().getValue());
			grupChecklistPenilaianUmum.setDisabled(true);
		} else {
			grupChecklistPenilaianUmum.setDisabled(false);
		}

		pilihan = new JSONObject(checklistPenilaianUmum.getPilihan());
		rowPilihan = new MyFormRow();
		rowPilihan.setParent(rows);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowPilihan);

				GrupChecklistPenilaianUmum grup = (GrupChecklistPenilaianUmum) (grupChecklistPenilaianUmum
						.getSelectedItem() == null ? null : grupChecklistPenilaianUmum.getSelectedItem().getValue());

				if (grup != null && grup.getAngketPenilaianUmum() != null) {
					rowPilihan.appendChild(new ais.ui.util.MyLabelConfig("Pilihan"));
					Hbox hbox = new Hbox();
					rowPilihan.appendChild(hbox);
					for (int i = 1; i <= grup.getAngketPenilaianUmum().getJumlahPilihan(); i++) {
						final MyTextbox myTextbox = new MyTextbox(
								pilihan.isNull(i + "") ? i + "" : pilihan.getString(i + ""));
						final int index = i;
						myTextbox.setParent(hbox);
						myTextbox.setCols(10);
						myTextbox.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pilihan.put(index + "", myTextbox.getValue().trim());
							}
						});
					}
				}
			}
		};

		eventListener.onEvent(null);
		grupChecklistPenilaianUmum.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sub Grup Angket Umum"));
		row.appendChild(subGrupChecklistPenilaianUmum = new Combobox());
		Common.insertComboDanSemua(subGrupChecklistPenilaianUmum, new String[] { "nama" }, "keterangan",
				SubGrupChecklistPenilaianUmum.class, "=Tanpa Sub Grup Angket Umum=", Restrictions.eq("aktif", true));
		Common.selectComboItem(subGrupChecklistPenilaianUmum,
				checklistPenilaianUmum.getSubGrupChecklistPenilaianUmum());
		subGrupChecklistPenilaianUmum.setWidth("90%");
		subGrupChecklistPenilaianUmum.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				checklistPenilaianUmum.getKeterangan() == null ? "" : checklistPenilaianUmum.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (grupChecklistPenilaianUmum.getSelectedItem() == null) {
			MyMessageboxConfig.show("Grup harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (checklistPenilaianUmum.getId() != null) {
			checklistPenilaianUmum = (ChecklistPenilaianUmum) session.load(ChecklistPenilaianUmum.class,
					checklistPenilaianUmum.getId());

		}

		checklistPenilaianUmum.setIsi(nama.getValue());
		checklistPenilaianUmum.setNomorUrut(nomorUrut.getValue());
		checklistPenilaianUmum.setGrupChecklistPenilaianUmum(
				(GrupChecklistPenilaianUmum) (grupChecklistPenilaianUmum.getSelectedItem() == null ? null
						: grupChecklistPenilaianUmum.getSelectedItem().getValue()));

		checklistPenilaianUmum.setSubGrupChecklistPenilaianUmum(
				(SubGrupChecklistPenilaianUmum) (subGrupChecklistPenilaianUmum.getSelectedItem() == null ? null
						: subGrupChecklistPenilaianUmum.getSelectedItem().getValue()));

		checklistPenilaianUmum.setPilihan(pilihan.toString());
		checklistPenilaianUmum.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, checklistPenilaianUmum);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		String untuk = (String) (searchDiperuntukkan.getSelectedItem() == null ? null
				: searchDiperuntukkan.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ChecklistPenilaianUmum.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order) {
			criteria.addOrder(Order.asc("nomorUrut"));
			criteria.addOrder(Order.asc("isi"));
			criteria.addOrder(Order.asc("id"));
		}
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("isi", searchnama.getValue(), MatchMode.ANYWHERE));
		criteria.add(searchGrup.getSelectedItem() == null || searchGrup.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("grupChecklistPenilaianUmum", searchGrup.getSelectedItem().getValue()));

		criteria.createAlias("grupChecklistPenilaianUmum", "grupChecklistPenilaianUmum", Criteria.LEFT_JOIN)

				.add(untuk == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("grupChecklistPenilaianUmum.diperuntukkan", untuk))

				.add(Restrictions.in("grupChecklistPenilaianUmum.diperuntukkan", diperuntukkans))

				.createAlias("grupChecklistPenilaianUmum.angketPenilaianUmum", "angketPenilaianUmum",
						Criteria.LEFT_JOIN)

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("angketPenilaianUmum.jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("angketPenilaianUmum.jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("angketPenilaianUmum.fakultas"),
								CommonSearchFilterHelper.eqSelectedWithId("angketPenilaianUmum.fakultas", searchfakultas, false)))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("angketPenilaianUmum.sekolah"),
								CommonSearchFilterHelper.eqSelectedWithId("angketPenilaianUmum.sekolah", searchsekolah, false)))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("angketPenilaianUmum.yayasan"),
								CommonSearchFilterHelper.eqSelectedWithId("angketPenilaianUmum.yayasan", searchyayasan, false)))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("angketPenilaianUmum.program"), Restrictions.eq(
										"angketPenilaianUmum.program", searchprogram.getSelectedItem().getValue())));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ChecklistPenilaianUmum> checklistPenilaianUmum = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(checklistPenilaianUmum);
		grid.setRowRenderer(new ChecklistPenilaianUmumRenderer());
		grid.setModelCheckMobile(strset);

	}



}
