package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AngketPenilaianUmum;
import ais.database.model.Fakultas;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk grup checklist penilaian umum. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchdiperuntukkan}, {@code Textbox
 * searchkodeangket}, {@code Textbox searchnamaangket}, {@code Combobox searchjurusan}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * diperuntukkan()}, {@code diperuntukkanPertemuan()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti
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
public class GrupChecklistPenilaianUmumAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchdiperuntukkan;
	private Textbox searchkodeangket;
	private Textbox searchnamaangket;
	private Combobox searchjurusan;
	private Combobox searchprogram;
	private Combobox searchfakultas;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private MyCheckboxConfig searchhanyaAktif;

	private Combobox statusMahasiswa;
	private Intbox mulaiAngkatan;
	private Intbox sampaiAngkatan;
	private Combobox fakultas;
	private Combobox jurusan;
	private Textbox isi;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private GrupChecklistPenilaianUmum grupChecklistPenilaianUmum;
	private MyToolbarbuttonConfig add;
	private Combobox diperuntukkan;
	private Row rowStatusMahasiswa;
	private Row rowMulaiAngkatan;
	private Row rowSampaiAngkatan;
	private Row rowFakultas;
	private Row rowJurusan;
	private AngketPenilaianUmum angket;
	private Combobox angketPenilaianUmum;
	private Combobox jenjang;
	private Combobox program;
	private boolean pt;
	private boolean ya;

	private Row hbFakultasLabel;
	private Row hbYayasan;
	private Combobox yayasan;
	private Combobox sekolah;
	private Row rowYayasan;
	private Row rowSekolah;

	public static String[] contents = new String[] { "id", "angketPenilaianUmum", "isi", "diperuntukkan",
			"statusMahasiswa", "mulaiAngkatan", "sampaiAngkatan", "fakultas", "jurusan", "yayasan", "sekolah",
			"jenjang", "program", "aktif", "keterangan" };

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public static void diperuntukkan(Combobox diperuntukkan) {
		Common.clear(diperuntukkan);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		MyComboitemConfig comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_UMUM);
		comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_UMUM);
		diperuntukkan.appendChild(comboitem);

		if (pt) {
			comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_DOSEN);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_DOSEN);
			diperuntukkan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_MAHASISWA);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_MAHASISWA);
			diperuntukkan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_ASISTEN);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_ASISTEN);
			diperuntukkan.appendChild(comboitem);
		}

		comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_ALUMNI);
		comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_ALUMNI);
		diperuntukkan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_ADMIN);
		comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_ADMIN);
		diperuntukkan.appendChild(comboitem);

		if (ya) {
			comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_SISWA);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_SISWA);
			diperuntukkan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_GURU);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_GURU);
			diperuntukkan.appendChild(comboitem);
		}

		comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_ORANG_TUA);
		comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_ORANG_TUA);
		diperuntukkan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_LINK_UMUM);
		comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_LINK_UMUM);
		diperuntukkan.appendChild(comboitem);
	}

	public static void diperuntukkanPertemuan(Combobox diperuntukkan) {

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		if (pt) {
			MyComboitemConfig comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_PERKULIAHAN);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_PERKULIAHAN);
			diperuntukkan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_BIMBINGAN);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_BIMBINGAN);
			diperuntukkan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_SIDANG);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_SIDANG);
			diperuntukkan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_KKN);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_KKN);
			diperuntukkan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_PKL);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_PKL);
			diperuntukkan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_AKADEMIK);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_AKADEMIK);
			diperuntukkan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_KEGIATAN);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_KEGIATAN);
			diperuntukkan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_WISUDA);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_WISUDA);
			diperuntukkan.appendChild(comboitem);
		}

		if (ya) {
			MyComboitemConfig comboitem = new MyComboitemConfig(GrupChecklistPenilaianUmum.UNTUK_PELAJARAN);
			comboitem.setValue(GrupChecklistPenilaianUmum.UNTUK_PELAJARAN);
			diperuntukkan.appendChild(comboitem);
		}
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(AngketPenilaianUmum.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			angket = new AngketPenilaianUmum();
			angket.setKode("001.000");
			angket.setIsi("EVALUASI PENILAIAN UMUM");
			Common.refreshSaveOrUpdate(session, angket);
		} else {
			angket = (AngketPenilaianUmum) session.createCriteria(AngketPenilaianUmum.class).setMaxResults(1)
					.uniqueResult();
		}

		diperuntukkan(searchdiperuntukkan);
		diperuntukkanPertemuan(searchdiperuntukkan);

		Common.initPrograms(searchprogram);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }
		if (hbYayasan != null) { hbYayasan.setVisible(ya); }

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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(GrupChecklistPenilaianUmum.class, this, contents);
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
												.getClassMetadata(GrupChecklistPenilaianUmum.class);
										Session session = HibernateUtil.currentNativeSession();

										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 1; i < rowCount; i++) {
											try {

												Long id = Common.getSheetContentAsLong(sheet, 0, i);
												GrupChecklistPenilaianUmum grupChecklistPenilaianUmum = id == null
														|| id.equals(-1L)
																? null
																: (GrupChecklistPenilaianUmum) session
																		.createCriteria(
																				GrupChecklistPenilaianUmum.class)
																		.add(Restrictions.idEq(id)).uniqueResult();

												if (grupChecklistPenilaianUmum == null) {
													grupChecklistPenilaianUmum = new GrupChecklistPenilaianUmum();
												}

												Common.setObjectValues(classMetadata, grupChecklistPenilaianUmum,
														contents, 1, sheet, i);

												session.getTransaction().begin();
												session.saveOrUpdate(grupChecklistPenilaianUmum);
												session.getTransaction().commit();

												label.setValue("Upload data \"" + grupChecklistPenilaianUmum.getKode()
														+ " - " + grupChecklistPenilaianUmum.getNama() + "\" ("
														+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}

										}
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/GrupChecklistPenilaianUmumAction.java:402");
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
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link GrupChecklistPenilaianUmumAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link GrupChecklistPenilaianUmumAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see GrupChecklistPenilaianUmumAction
	 */
	class GrupChecklistPenilaianUmumRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GrupChecklistPenilaianUmum grupChecklistPenilaianUmum = (GrupChecklistPenilaianUmum) arg1;
			if (grupChecklistPenilaianUmum.getAngketPenilaianUmum() == null && angket != null) {
				grupChecklistPenilaianUmum.setAngketPenilaianUmum(angket);
			}
			new Label(grupChecklistPenilaianUmum.getAngketPenilaianUmum() == null ? ""
					: grupChecklistPenilaianUmum.getAngketPenilaianUmum().getIsi()).setParent(arg0);

			RevisiHelper.createNewRevisi(GrupChecklistPenilaianUmum.class, grupChecklistPenilaianUmum,
					grupChecklistPenilaianUmum.getIsi()).setParent(arg0);

			if (grupChecklistPenilaianUmum.getDiperuntukkan().equals(GrupChecklistPenilaianUmum.UNTUK_MAHASISWA)) {
				Vbox hbox = new Vbox();
				new Label("Untuk : " + grupChecklistPenilaianUmum.getDiperuntukkan()).setParent(hbox);
				new Label("Status : " + (grupChecklistPenilaianUmum.getStatusMahasiswa() == null ? "Semua"
						: grupChecklistPenilaianUmum.getStatusMahasiswa().getNama())).setParent(hbox);
				new Label("Angkatan Mulai : " + grupChecklistPenilaianUmum.getMulaiAngkatan()).setParent(hbox);
				new Label("Angkatan Sampai : " + grupChecklistPenilaianUmum.getSampaiAngkatan()).setParent(hbox);

				new Label(Common.getBahasaConfig("Fakultas") + " : "
						+ (grupChecklistPenilaianUmum.getFakultas() == null ? "Semua"
								: grupChecklistPenilaianUmum.getFakultas().getNama()))
						.setParent(hbox);
				new Label(Common.getBahasaConfig("Jurusan") + " : "
						+ (grupChecklistPenilaianUmum.getJurusan() == null ? "Semua"
								: grupChecklistPenilaianUmum.getJurusan().getNama()))
						.setParent(hbox);

				hbox.setParent(arg0);
			} else {
				new Label(grupChecklistPenilaianUmum.getDiperuntukkan()).setParent(arg0);
			}

			AngketPenilaianUmum angketPenilaianUmum = grupChecklistPenilaianUmum.getAngketPenilaianUmum();

			Vbox vbox = new Vbox();
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

			new Label(grupChecklistPenilaianUmum.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(grupChecklistPenilaianUmum.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					grupChecklistPenilaianUmum.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(grupChecklistPenilaianUmum);
				}
			});

			// Kolom aksi rapi: semua tombol dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(grupChecklistPenilaianUmum);
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

											Common.refreshDelete(grupChecklistPenilaianUmum);

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
		init(new GrupChecklistPenilaianUmum());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(GrupChecklistPenilaianUmum grupChecklistPenilaianUmum) throws Exception {
		this.grupChecklistPenilaianUmum = grupChecklistPenilaianUmum;
		addWindow.setTitle(grupChecklistPenilaianUmum.getId() == null ? "Tambah Grup Penilaian Umum" : "Ubah Grup Penilaian Umum");
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Angket *"));
		row.appendChild(angketPenilaianUmum = new Combobox());
		angketPenilaianUmum.setReadonly(true);
		Common.insertCombo(angketPenilaianUmum, new String[] { "kode", "isi", "fakultas", "jurusan", "program" },
				AngketPenilaianUmum.class);
		Common.selectComboItem(angketPenilaianUmum, grupChecklistPenilaianUmum.getAngketPenilaianUmum());
		angketPenilaianUmum.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Grup *"));
		row.appendChild(isi = new Textbox(
				grupChecklistPenilaianUmum.getIsi() == null ? "" : grupChecklistPenilaianUmum.getIsi()));
		isi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diperuntukkan *"));
		row.appendChild(diperuntukkan = new Combobox());
		diperuntukkan(diperuntukkan);
		diperuntukkanPertemuan(diperuntukkan);
		diperuntukkan.setWidth("90%");
		diperuntukkan.setReadonly(true);

		Common.selectComboItem(diperuntukkan, grupChecklistPenilaianUmum.getDiperuntukkan());

		final MyFormRow imgRow = new MyFormRow();
		imgRow.setStyle("border:0px;background: transparent;");
		imgRow.setParent(rows);

		EventListener eventListenerData = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				imgRow.setVisible(false);
				Common.clear(imgRow);
				String d = (String) (diperuntukkan.getSelectedItem() == null ? null
						: diperuntukkan.getSelectedItem().getValue());
				if (d != null && d.equals(GrupChecklistPenilaianUmum.UNTUK_LINK_UMUM)) {
					imgRow.setVisible(true);
					imgRow.appendChild(new Label(ais.common.Common.getBahasaConfig("QR-CODE")));

					String urlH = Common.getRequestHostWithProtocol() + "/common/checklist_penilaian_umum.zul";

					String code = "umum";
					File myfilebarcode1 = new File(
							Common.ambilREAL_PATH_REPORT() + "/crcode_" + URLEncoder.encode(code, "UTF-8") + ".png");
					BarcodeCommon.generateCRCode(urlH, myfilebarcode1);

					imgRow.appendChild(
							new Image(Common.getRequestHostWithProtocol() + "/report/" + myfilebarcode1.getName()));
				}
			}

		};

		diperuntukkan.addEventListener("onChange", eventListenerData);

		eventListenerData.onEvent(null);

		rowStatusMahasiswa = new MyFormRow();
		rowStatusMahasiswa.setStyle("border:0px;background: transparent;");
		rowStatusMahasiswa.setParent(rows);
		rowStatusMahasiswa.appendChild(new Label(ais.common.Common.getBahasaConfig("Status Mahasiswa")));
		rowStatusMahasiswa.appendChild(statusMahasiswa = new Combobox());
		Common.insertComboDanSemua(statusMahasiswa, "nama", StatusMahasiswa.class);
		Common.selectComboItem(statusMahasiswa, grupChecklistPenilaianUmum.getStatusMahasiswa());
		statusMahasiswa.setWidth("90%");

		rowMulaiAngkatan = new MyFormRow();
		rowMulaiAngkatan.setStyle("border:0px;background: transparent;");
		rowMulaiAngkatan.setParent(rows);
		rowMulaiAngkatan.appendChild(new Label(ais.common.Common.getBahasaConfig("Mulai Angkatan")));
		rowMulaiAngkatan.appendChild(mulaiAngkatan = new Intbox(grupChecklistPenilaianUmum.getMulaiAngkatan()));
		rowMulaiAngkatan.setWidth("90%");

		rowSampaiAngkatan = new MyFormRow();
		rowSampaiAngkatan.setStyle("border:0px;background: transparent;");
		rowSampaiAngkatan.setParent(rows);
		rowSampaiAngkatan.appendChild(new Label(ais.common.Common.getBahasaConfig("Sampai Angkatan")));
		rowSampaiAngkatan.appendChild(sampaiAngkatan = new Intbox(grupChecklistPenilaianUmum.getSampaiAngkatan()));
		rowSampaiAngkatan.setWidth("90%");

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		rowFakultas = new MyFormRow();
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new Label(ais.common.Common.getBahasaConfig("Fakultas")));
		rowFakultas.appendChild(fakultas);
		Common.selectComboItem(fakultas, grupChecklistPenilaianUmum.getFakultas());
		fakultas.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", grupChecklistPenilaianUmum.getFakultas() == null ? tbmuser.ambilFakultas()
						: grupChecklistPenilaianUmum.getFakultas()));

		rowJurusan = new MyFormRow();
		rowJurusan.setStyle("border:0px;background: transparent;");
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new Label(ais.common.Common.getBahasaConfig("Jurusan")));
		rowJurusan.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, grupChecklistPenilaianUmum.getJurusan());

		Common.initYayasanDanSekolahDanSemua(yayasan = new Combobox(), sekolah = new Combobox(), null, null);
		rowYayasan = new MyFormRow();
		rowYayasan.setStyle("border:0px;background: transparent;");
		rowYayasan.setParent(rows);
		rowYayasan.appendChild(new Label(ais.common.Common.getBahasaConfig("Yayasan")));
		rowYayasan.appendChild(yayasan);
		Common.selectComboItem(yayasan, grupChecklistPenilaianUmum.getYayasan());
		yayasan.setWidth("90%");

		rowSekolah = new MyFormRow();
		rowSekolah.setStyle("border:0px;background: transparent;");
		rowSekolah.setParent(rows);
		rowSekolah.appendChild(new Label(ais.common.Common.getBahasaConfig("Sekolah")));
		rowSekolah.appendChild(sekolah);
		sekolah.setWidth("90%");
		Common.pilihSekolah(sekolah, grupChecklistPenilaianUmum.getSekolah());

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String peruntukan = (String) (diperuntukkan.getSelectedItem() == null ? ""
						: diperuntukkan.getSelectedItem().getValue());
				rowStatusMahasiswa.setVisible(pt && (peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_MAHASISWA)
						|| peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_ALUMNI)
						|| peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_ORANG_TUA)));
				rowMulaiAngkatan.setVisible(peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_MAHASISWA)
						|| peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_ALUMNI)
						|| peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_ORANG_TUA));
				rowSampaiAngkatan.setVisible(peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_MAHASISWA)
						|| peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_ALUMNI)
						|| peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_ORANG_TUA));
				rowFakultas.setVisible(pt && (peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_MAHASISWA)
						|| peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_ALUMNI)
						|| peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_ORANG_TUA)));
				rowJurusan.setVisible(pt && (peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_MAHASISWA)
						|| peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_ALUMNI)
						|| peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_ORANG_TUA)));

				rowYayasan.setVisible(ya && (peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_SISWA)
						|| peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_ORANG_TUA)));
				rowSekolah.setVisible(ya && (peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_SISWA)
						|| peruntukan.equals(GrupChecklistPenilaianUmum.UNTUK_ORANG_TUA)));
			}
		};

		eventListener.onEvent(null);
		diperuntukkan.addEventListener("onChange", eventListener);

		Common.insertComboDanSemua(jenjang = new Combobox(), "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		Common.selectComboItem(jenjang, grupChecklistPenilaianUmum.getJenjang());
		row.appendChild(jenjang);
		jenjang.setWidth("90%");
		jenjang.setReadonly(true);

		program = Common.initPrograms(program);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program, grupChecklistPenilaianUmum.getProgram() == null);
		row.appendChild(program);
		program.setWidth("90%");
		program.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				grupChecklistPenilaianUmum.getKeterangan() == null ? "" : grupChecklistPenilaianUmum.getKeterangan()));
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
		if (angketPenilaianUmum.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Angket",
					"Kolom Nama Angket belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Angket.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (isi.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Grup",
					"Kolom Nama Grup belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Grup.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (diperuntukkan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Diperuntukkan",
					"Kolom Diperuntukkan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Diperuntukkan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (grupChecklistPenilaianUmum.getId() != null) {
			grupChecklistPenilaianUmum = (GrupChecklistPenilaianUmum) session.load(GrupChecklistPenilaianUmum.class,
					grupChecklistPenilaianUmum.getId());

		}
		grupChecklistPenilaianUmum
				.setAngketPenilaianUmum((AngketPenilaianUmum) angketPenilaianUmum.getSelectedItem().getValue());

		grupChecklistPenilaianUmum.setIsi(isi.getValue());
		grupChecklistPenilaianUmum.setKeterangan(keterangan.getValue());
		grupChecklistPenilaianUmum.setDiperuntukkan((String) diperuntukkan.getSelectedItem().getValue());
		grupChecklistPenilaianUmum
				.setStatusMahasiswa((StatusMahasiswa) (statusMahasiswa.getSelectedItem() == null ? null
						: statusMahasiswa.getSelectedItem().getValue()));
		grupChecklistPenilaianUmum.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		grupChecklistPenilaianUmum.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		grupChecklistPenilaianUmum.setMulaiAngkatan(mulaiAngkatan.getValue());
		grupChecklistPenilaianUmum.setSampaiAngkatan(sampaiAngkatan.getValue());

		grupChecklistPenilaianUmum.setJenjang(
				(Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue()));
		grupChecklistPenilaianUmum
				.setProgram((String) (program.getSelectedItem() == null ? null : program.getSelectedItem().getValue()));

		grupChecklistPenilaianUmum.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		grupChecklistPenilaianUmum.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		Common.refreshUpdate(session, grupChecklistPenilaianUmum);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(GrupChecklistPenilaianUmum.class)

				.add(searchhanyaAktif.isChecked() ? Restrictions.eq("aktif", true)
						: Restrictions.sqlRestriction("true"))
				.createAlias("angketPenilaianUmum", "angketPenilaianUmum", Criteria.LEFT_JOIN);

		if (order)
			criteria.addOrder(Order.asc("isi"));
		criteria.add(searchdiperuntukkan.getSelectedItem() == null
				|| searchdiperuntukkan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("diperuntukkan", searchdiperuntukkan.getSelectedItem().getValue()))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("isi", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchkodeangket.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("angketPenilaianUmum.kode", searchkodeangket.getValue().trim(),
								MatchMode.ANYWHERE))
				.add(searchnamaangket.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("angketPenilaianUmum.isi", searchnamaangket.getValue().trim(),
								MatchMode.ANYWHERE));

		criteria.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.isNull("angketPenilaianUmum.jurusan"),
						CommonSearchFilterHelper.eqSelectedWithId("angketPenilaianUmum.jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("angketPenilaianUmum.fakultas"),
								CommonSearchFilterHelper.eqSelectedWithId("angketPenilaianUmum.fakultas", searchfakultas, false)))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("angketPenilaianUmum.program"),
										Restrictions.eq("angketPenilaianUmum.program",
												searchprogram.getSelectedItem().getValue())))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<GrupChecklistPenilaianUmum> grupChecklistPenilaianUmum = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(grupChecklistPenilaianUmum);
		grid.setRowRenderer(new GrupChecklistPenilaianUmumRenderer());
		grid.setModelCheckMobile(strset);

	}



}
