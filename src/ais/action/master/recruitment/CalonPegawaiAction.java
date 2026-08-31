package ais.action.master.recruitment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.East;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;
import org.zkoss.zul.Window;

import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.KarirConfigUtil;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Agama;
import ais.database.model.GeneralValueObject;
import ais.database.model.Kota;
import ais.database.model.Propinsi;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.recruitment.CalonPegawaiPunyaDokumen;
import ais.database.model.recruitment.GelombangPendaftaranPegawai;
import ais.database.model.recruitment.VerifikasiKelengkapanCalonPegawai;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk calon pegawai. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Component addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Checkbox searchaktif}, {@code Combobox
 * searchstatus}, {@code MyCheckboxConfig diterima}, {@code MyCheckboxConfig terverifikasi};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code
 * initFilterStatusSeleksi()}, {@code buatStatusSeleksi()}, {@code initInfoKanan()}, {@code initInfo()});
 * pembacaan/pencarian ({@code ambilLinkDariKeteranganDokumen()}, {@code ambilLinkLampiranDokumen()}, {@code
 * tampilkanLinkLihatPrintDokumen()}, {@code ambilLampiranDokumen()}, {@code
 * ambilDokumenUtamaDanRapikanDuplikat()}, {@code ambilLampiranUtamaDokumen()}); validasi/perhitungan ({@code
 * statusSeleksiCheckboxCriterion()}, {@code tambahTemplateDokumenJikaValid()}); mutasi data ({@code onSave()});
 * operasi domain lain ({@code onDokumen()}, {@code onDokumenPenyedia()}, {@code falseOrNull()}, {@code
 * statusSeleksiCriterion()}, {@code onInfo()}, {@code onAddExternalDaftar()}). Bagian lain dari kontrak tetap
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
public class CalonPegawaiAction extends GenericAutowireComposer
		implements DataInitDefault, DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Component addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;
	private Combobox searchstatus;
	private MyCheckboxConfig diterima;
	private MyCheckboxConfig terverifikasi;
	private MyCheckboxConfig ditolak;
	private MyCheckboxConfig mundur;
	private MyCheckboxConfig belum;

	private Textbox kode;
	private Textbox nama;
	private Textbox alamat;
	private Textbox kodePos;
	private Textbox telp;
	private CalonPegawai calonPegawai;
	private MyToolbarbuttonConfig add;
	private Combobox gelombangPendaftaranPegawai;
	private AmbilDataKecamatanBanbox kecamatan;
	private Label propinsi;
	private Label kota;

	private Textbox email;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Tabpanel dokumenTab;

	public void onDokumen(Event event) {

		if (dokumenTab.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(dokumenTab);
			MyInclude iframe = new MyInclude("/pages/master/recruitment/verifikasi_kelengkapan_calon_pegawai.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel dokumenPenyedia;

	public void onDokumenPenyedia(Event event) {

		if (dokumenPenyedia.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(dokumenPenyedia);
			MyInclude iframe = new MyInclude("/pages/master/recruitment/calon_pegawai_punya_verifikasi_dokumen.zul");
			iframe.setParent(window);
		}
	}

	private HashMap<Long, LampiranLain> maps;
	private Row rowGalery;
	protected Rows myGridGaleri;
	private East east;
	private Rows rowsDokumen;

	private South mysouth = null;
	private Combobox jenisKelamin;
	private Textbox tempatLahir;
	private MyDatebox tanggalLahir;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private EventListener checkKesamaan = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {

			final GelombangPendaftaranPegawai gelombangPendaftaranPegawaiPsb = (GelombangPendaftaranPegawai) (CalonPegawaiAction.this.gelombangPendaftaranPegawai
					.getSelectedItem() == null ? null
							: CalonPegawaiAction.this.gelombangPendaftaranPegawai.getSelectedItem().getValue());

			if (gelombangPendaftaranPegawaiPsb != null && tanggalLahir.getValue() != null
					&& !nama.getValue().trim().isEmpty()) {
				Session session = HibernateUtil.currentSession();
				int count = ((Number) session.createCriteria(CalonPegawai.class)
						.add(Restrictions.eq("gelombangPendaftaranPegawai", gelombangPendaftaranPegawaiPsb))
						.setProjection(Projections.rowCount())

						.add(Restrictions.ilike("namaPegawai", nama.getValue().trim(), MatchMode.EXACT))

						.add(calonPegawai.getId() != null ? Restrictions.ne("id", calonPegawai.getId())
								: Restrictions.sqlRestriction("true"))

						.add(Restrictions.eq("tanggalLahir", tanggalLahir.getValue()))

						.uniqueResult()).intValue();

				System.out.println("count = " + count + ", namaPegawai = " + nama.getValue() + ", tanggalLahir = "
						+ Common.dateFormat4.get().format(tanggalLahir.getValue()));

				if (count > 0) {

					MyMessageboxConfig.show(
							"Data pendaftaran sebagai berikut :\n" + "Nama : " + nama.getValue() + "\n"
									+ "Tanggal Lahir : " + Common.dateFormat2.get().format(tanggalLahir.getValue()) + "\n"
									+ "telah terdaftar sebelumnya.\n" + "Apakah yakin ingin mengubah data ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {

									addWindow.setVisible(false);

									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												Session session = HibernateUtil.currentSession();
												CalonPegawai calonPegawai1 = (CalonPegawai) session
														.createCriteria(CalonPegawai.class)

														.add(Restrictions.eq("gelombangPendaftaranPegawai",
																gelombangPendaftaranPegawaiPsb))

														.add(Restrictions.ilike("namaPegawai", nama.getValue().trim(),
																MatchMode.EXACT))

														.add(calonPegawai.getId() != null
																? Restrictions.ne("id", calonPegawai.getId())
																: Restrictions.sqlRestriction("true"))

														.add(Restrictions.eq("tanggalLahir", tanggalLahir.getValue()))

														.setMaxResults(1).uniqueResult();

												init(calonPegawai1);
												addWindow.setVisible(true);
												if (addWindow instanceof Window) {
													((Window) addWindow).onModal();
												}
												gelombangPendaftaranPegawai.setDisabled(true);
											}
										});

									}

								}
							});

				}

			}
		}
	};
	private Combobox agama;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		initFilterStatusSeleksi();

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

		MyToolbarbuttonConfig generatePasswordCalonPegawai = new MyToolbarbuttonConfig("Password penyedia / perusahaan",
				"/img/print.png");
		generatePasswordCalonPegawai.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show(
						"Anda akan membuatkan dan mengambil username dan password penyedia / perusahaan.", "Informasi",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											final String filename = Sessions.getCurrent().getWebApp()
													.getRealPath("/tmp/user_password_calonPegawai_"
															+ URLEncoder.encode(Common.datetimeFormat2s.get()
																	.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
															+ ".xlsx");

											List<CalonPegawai> calonPegawais = initCriteria(true)
													.add(Restrictions.isNotNull("nama")).setMaxResults(1048576).list();

											XSSFWorkbook workbook = new XSSFWorkbook();
											XSSFSheet sheet = workbook.createSheet("DOSEN");
											sheet.setDefaultColumnWidth(20);
											int rowIndex = 0;

											XSSFRow rowhead = sheet.createRow((short) 0);
											rowhead.createCell(0).setCellValue("ID");
											rowhead.createCell(1).setCellValue("Username");
											rowhead.createCell(2).setCellValue("Password");
											rowhead.createCell(3).setCellValue("Nama Lengkap");
											rowhead.createCell(4).setCellValue("Email");
											rowhead.createCell(5).setCellValue("HP");

										for (CalonPegawai calonPegawai : calonPegawais) {
											if (calonPegawai.getNama() != null
													&& !calonPegawai.getNama().trim().isEmpty()) {
												rowIndex++;
												Session session = HibernateUtil.currentNativeSession();
												try {
													Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class)
															.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
															.add(Restrictions.eq("calonPegawai", calonPegawai)).setMaxResults(1).uniqueResult();
													if (tbmuser == null || tbmuser.getUserId() == null) {
														tbmuser = new Tbmuser();

														String newUsername = StringUtils.split(calonPegawai.getNama(), " ")[0] + ""
																+ RandomStringUtils.randomNumeric(3);
														newUsername = newUsername.toLowerCase().trim();

														tbmuser.setUserId(newUsername);
														tbmuser.setEmail(calonPegawai.getAlamatEmail());
														tbmuser.setIs_encripted(true);
														tbmuser.setRoot(false);
														tbmuser.setUserNama(calonPegawai.getNama());
														String passw = RandomStringUtils.randomNumeric(5);
														tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(passw.trim()));
														tbmuser.setUserRole(ConstantValues.tbmroleCalonPegawai);
														tbmuser.setUserShow(1);
														tbmuser.setCalonPegawai(calonPegawai);

														session.getTransaction().begin();
														Common.refreshSaveOrUpdate(session, tbmuser);
														session.getTransaction().commit();

														Common.saveOrUpdateUserAccess(tbmuser, null, tbmuser.getUserId(), passw.trim(),
																tbmuser.getEmail());
													}

													XSSFRow row = sheet.createRow(rowIndex);
													row.createCell(0).setCellValue(calonPegawai.getId());
													row.createCell(1).setCellValue(tbmuser.getUserId());

													try {
														row.createCell(2).setCellValue(Common.desEncrypter.get().decrypt(tbmuser.getUserPassword()));
													} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

													row.createCell(3).setCellValue(calonPegawai.getNama());
													row.createCell(4).setCellValue(calonPegawai.getAlamatEmail());
													row.createCell(5).setCellValue(calonPegawai.getTeleponPegawai());
												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
												} finally {
													KarirConfigUtil.closeNativeSession(session);
												}
											}
										}

										try {
											FileOutputStream fileOut = new FileOutputStream(filename);
											workbook.write(fileOut);
											fileOut.close();
										} catch (IOException e) {
											Common.tampilErrorJikaAdmin(e);
										}

										try {
											File file = new File(filename);
											Filedownload.save(new FileInputStream(file),
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getName());
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

										}
									});

								}

							}
						});

			}
		});
		Common.appendKeToolbar(generatePasswordCalonPegawai, add, comp);

		String[] contents = new String[] { "id", "kode", "nama", "propinsi", "kota", "kecamatan",
				"gelombangPendaftaranPegawai", "kelompokPendaftaranPegawai", "alamatPegawai", "kodePos",
				"teleponPegawai", "alamatEmail", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CalonPegawai.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, CalonPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	private void initFilterStatusSeleksi() {
		if (searchstatus == null) {
			return;
		}
		Common.clear(searchstatus);

		Comboitem comboitem = new Comboitem("Diterima");
		comboitem.setValue("Diterima");
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem("Terverifikasi");
		comboitem.setValue("Terverifikasi");
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem("Ditolak");
		comboitem.setValue("Ditolak");
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem("Mengundurkan Diri");
		comboitem.setValue("Mengundurkan Diri");
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem("Blm Ditentukan");
		comboitem.setValue("Blm Ditentukan");
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem("Semua");
		comboitem.setValue(null);
		searchstatus.appendChild(comboitem);
		searchstatus.setSelectedItem(comboitem);
		searchstatus.setReadonly(true);
	}

	private Criterion falseOrNull(String property) {
		return Restrictions.or(Restrictions.eq(property, false), Restrictions.isNull(property));
	}

	private Criterion statusSeleksiCriterion() {
		if (searchstatus == null || searchstatus.getSelectedItem() == null) {
			return Restrictions.sqlRestriction("true");
		}
		String s = searchstatus.getSelectedItem().getLabel();
		if (s == null) {
			return Restrictions.sqlRestriction("true");
		}
		if (s.equalsIgnoreCase("Diterima")) {
			return Restrictions.eq("telahDiterima", true);
		} else if (s.equalsIgnoreCase("Terverifikasi")) {
			return Restrictions.eq("terverifikasi", true);
		} else if (s.equalsIgnoreCase("Ditolak")) {
			return Restrictions.eq("ditolak", true);
		} else if (s.equalsIgnoreCase("Mengundurkan Diri")) {
			return Restrictions.eq("mengundurkanDiri", true);
		} else if (s.equalsIgnoreCase("Blm Ditentukan")) {
			return Restrictions.and(falseOrNull("telahDiterima"), Restrictions.and(falseOrNull("terverifikasi"),
					Restrictions.and(falseOrNull("mengundurkanDiri"), falseOrNull("ditolak"))));
		}
		return Restrictions.sqlRestriction("true");
	}

	private Criterion statusSeleksiCheckboxCriterion() {
		if (diterima == null && terverifikasi == null && ditolak == null && mundur == null && belum == null) {
			return Restrictions.sqlRestriction("true");
		}

		boolean pilihDiterima = diterima != null && diterima.isChecked();
		boolean pilihTerverifikasi = terverifikasi != null && terverifikasi.isChecked();
		boolean pilihDitolak = ditolak != null && ditolak.isChecked();
		boolean pilihMundur = mundur != null && mundur.isChecked();
		boolean pilihBelum = belum != null && belum.isChecked();

		if (pilihDiterima && pilihTerverifikasi && pilihDitolak && pilihMundur && pilihBelum) {
			return Restrictions.sqlRestriction("true");
		}

		Criterion criterion = Restrictions.sqlRestriction("false");
		if (pilihDiterima) {
			criterion = Restrictions.or(criterion, Restrictions.eq("telahDiterima", true));
		}
		if (pilihTerverifikasi) {
			criterion = Restrictions.or(criterion, Restrictions.eq("terverifikasi", true));
		}
		if (pilihDitolak) {
			criterion = Restrictions.or(criterion, Restrictions.eq("ditolak", true));
		}
		if (pilihMundur) {
			criterion = Restrictions.or(criterion, Restrictions.eq("mengundurkanDiri", true));
		}
		if (pilihBelum) {
			criterion = Restrictions.or(criterion, Restrictions.and(falseOrNull("telahDiterima"),
					Restrictions.and(falseOrNull("terverifikasi"),
							Restrictions.and(falseOrNull("mengundurkanDiri"), falseOrNull("ditolak")))));
		}
		return criterion;
	}

	private Vbox buatStatusSeleksi(final CalonPegawai calonPegawai) {
		Vbox vbox = new Vbox();
		vbox.setSpacing("2px");

		Radio diterimaRadio = new Radio("Diterima");
		Radio terverifikasiRadio = new Radio("Terverifikasi");
		Radio ditolakRadio = new Radio("Ditolak");
		Radio undurRadio = new Radio("Mengundurkan Diri");
		Radio belumRadio = new Radio("Blm Ditentukan");

		diterimaRadio.setStyle("font-size:8px;");
		terverifikasiRadio.setStyle("font-size:8px;");
		ditolakRadio.setStyle("font-size:8px;");
		undurRadio.setStyle("font-size:8px;");
		belumRadio.setStyle("font-size:8px;");

		final Radiogroup status = new Radiogroup();
		status.setOrient("vertical");
		status.appendChild(diterimaRadio);
		status.appendChild(terverifikasiRadio);
		status.appendChild(ditolakRadio);
		status.appendChild(undurRadio);
		status.appendChild(belumRadio);
		status.setParent(vbox);

		diterimaRadio.setDisabled(!edit);
		terverifikasiRadio.setDisabled(!edit);
		ditolakRadio.setDisabled(!edit);
		undurRadio.setDisabled(!edit);
		belumRadio.setDisabled(!edit);

		if (calonPegawai.getTerverifikasi()) {
			status.setSelectedItem(terverifikasiRadio);
		} else if (calonPegawai.getTelahDiterima()) {
			status.setSelectedItem(diterimaRadio);
		} else if (calonPegawai.getDitolak()) {
			status.setSelectedItem(ditolakRadio);
		} else if (calonPegawai.getMengundurkanDiri()) {
			status.setSelectedItem(undurRadio);
		} else {
			status.setSelectedItem(belumRadio);
		}

		EventListener statusListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (status.getSelectedItem() == null) {
					return;
				}
				String label = status.getSelectedItem().getLabel();
				if (label.equalsIgnoreCase("Diterima")) {
					calonPegawai.setTelahDiterima(true);
					calonPegawai.setTerverifikasi(false);
					calonPegawai.setDitolak(false);
					calonPegawai.setMengundurkanDiri(false);
				} else if (label.equalsIgnoreCase("Terverifikasi")) {
					calonPegawai.setTelahDiterima(false);
					calonPegawai.setTerverifikasi(true);
					calonPegawai.setDitolak(false);
					calonPegawai.setMengundurkanDiri(false);
				} else if (label.equalsIgnoreCase("Ditolak")) {
					calonPegawai.setTelahDiterima(false);
					calonPegawai.setTerverifikasi(false);
					calonPegawai.setDitolak(true);
					calonPegawai.setMengundurkanDiri(false);
				} else if (label.equalsIgnoreCase("Mengundurkan Diri")) {
					calonPegawai.setTelahDiterima(false);
					calonPegawai.setTerverifikasi(false);
					calonPegawai.setDitolak(false);
					calonPegawai.setMengundurkanDiri(true);
				} else {
					calonPegawai.setTelahDiterima(false);
					calonPegawai.setTerverifikasi(false);
					calonPegawai.setDitolak(false);
					calonPegawai.setMengundurkanDiri(false);
				}
				Common.refreshSaveOrUpdate(calonPegawai);
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});
			}
		};

		diterimaRadio.addEventListener("onClick", statusListener);
		terverifikasiRadio.addEventListener("onClick", statusListener);
		ditolakRadio.addEventListener("onClick", statusListener);
		undurRadio.addEventListener("onClick", statusListener);
		belumRadio.addEventListener("onClick", statusListener);
		return vbox;
	}

	private void initInfoKanan(CalonPegawai calonPegawai, Component parent) throws Exception {
		MyGrid grid = new MyGrid();
		grid.setParent(parent);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);
	}

	private void initInfo(CalonPegawai calonPegawai) throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);

		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		West west = new West();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		initInfoKanan(calonPegawai, west);

		Center center = new Center();
		((Window) addWindow).setTitle(
				"Informasi " + (calonPegawai == null || calonPegawai.getGelombangPendaftaranPegawai() == null ? ""
						: calonPegawai.getGelombangPendaftaranPegawai().getNama()));
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
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

	}

	private void initDaftar(final CalonPegawai calonPegawai) throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);

		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		Center center = new Center();
		((Window) addWindow).setTitle("Informasi Data Diri Calon Pegawai");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
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

		Tbmuser tbmuser = Common.getCurrentUser();

		Rows rows = new Rows();
		rows.setParent(grid);
		kode = new Textbox(calonPegawai.getNoRegistrasi());
		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIK *"));
		if (tbmuser != null && tbmuser.getCalonPegawai() != null && calonPegawai != null
				&& tbmuser.getCalonPegawai().getId().equals(calonPegawai.getId())) {
			row.appendChild(new Label(calonPegawai.getNoRegistrasi()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama *"));
		row.appendChild(nama = new Textbox(calonPegawai.getNama() == null ? "" : calonPegawai.getNama()));
		nama.setWidth("90%");

		jenisKelamin = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		jenisKelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		jenisKelamin.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin *"));
		Common.selectComboItem(jenisKelamin, calonPegawai.getJenisKelamin());
		row.appendChild(jenisKelamin);
		jenisKelamin.setWidth("90%");
		jenisKelamin.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat / Tanggal Lahir *"));
		Hbox hbox = new Hbox();
		hbox.appendChild(
				tempatLahir = new Textbox(calonPegawai.getTempatLahir() == null ? "" : calonPegawai.getTempatLahir()));
		hbox.appendChild(tanggalLahir = new MyDatebox(calonPegawai.getTanggalLahir()));
		row.appendChild(hbox);
		tempatLahir.setCols(15);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp. *"));
		row.appendChild(telp = new Textbox(calonPegawai.getTeleponPegawai()));
		telp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email *"));
		row.appendChild(email = new Textbox(calonPegawai.getAlamatEmail()));
		email.setWidth("90%");

		South south = new South();
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Daftarkan Diri Anda", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (kode.getValue().trim().equals("")) {
					MyMessageboxConfig.show("NIK harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				if (nama.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Nama harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (jenisKelamin.getSelectedItem() == null || jenisKelamin.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Jenis kelamin harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (tempatLahir.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Tempat Lahir harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (tanggalLahir.getValue() == null) {
					MyMessageboxConfig.show("Tanggal Lahir harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				if (email.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Email harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (telp.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Telp/HP harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				Session session = HibernateUtil.currentNativeSession();
				String newUsername = "";
				String passw = "";
					try {

				int count = ((Number) session.createCriteria(CalonPegawai.class)
						.add(Restrictions.eq("alamatEmail", email.getValue().trim()))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				if (count > 0) {
					MyMessageboxConfig.show("Email yang Anda masukkan telah terdaftar", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				count = ((Number) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("userId", email.getValue().trim())).setProjection(Projections.rowCount())
						.uniqueResult()).intValue();
				if (count > 0) {
					MyMessageboxConfig.show("Email yang Anda masukkan telah terdaftar", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				calonPegawai.setNoRegistrasi(kode.getValue());
				calonPegawai.setNamaPegawai(nama.getValue());
				calonPegawai.setAlamatEmail(email.getValue());
				calonPegawai.setTeleponPegawai(telp.getValue());
				calonPegawai.setTempatLahir(tempatLahir.getValue());
				calonPegawai.setTanggalLahir(tanggalLahir.getValue());
				calonPegawai.setJenisKelamin((String) jenisKelamin.getSelectedItem().getValue());

				session.getTransaction().begin();
				session.save(calonPegawai);
				session.getTransaction().commit();

				Tbmuser tbmuser = new Tbmuser();

				newUsername = calonPegawai.getAlamatEmail().isEmpty()
						? StringUtils.split(calonPegawai.getNama(), " ")[0] + "" + RandomStringUtils.randomNumeric(3)
						: calonPegawai.getAlamatEmail().split(",")[0].trim();

				newUsername = newUsername.toLowerCase().trim();

				tbmuser.setUserId(newUsername);
				tbmuser.setEmail(calonPegawai.getAlamatEmail());
				tbmuser.setIs_encripted(true);
				tbmuser.setRoot(false);
				tbmuser.setUserNama(calonPegawai.getNama());
				passw = RandomStringUtils.randomNumeric(5);
				tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(passw.trim()));
				tbmuser.setUserRole(ConstantValues.tbmrolePenyedia);
				tbmuser.setUserShow(1);
				tbmuser.setCalonPegawai(calonPegawai);

				session.getTransaction().begin();
				session.save(tbmuser);
				session.getTransaction().commit();


					} finally {
						KarirConfigUtil.closeNativeSession(session);
					}
				String subject = "Username dan Password Calon Karyawan";
				String body = "Username Anda adalah " + newUsername + " dan password " + passw + "<br><br>Terima Kasih";
				String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();

				JSONArray userIds = new JSONArray();
				userIds.put(newUsername);
				MailSender.sendMail(userIds, subject, body, sender, calonPegawai.getAlamatEmail(), calonPegawai);

				MyMessageboxConfig.show("Anda berhasil terdaftar, informasi login telah terkirm ke email", "Informasi",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								addWindow.detach();

								Tbmuser tbmuser = Common.getCurrentUser();
								if (tbmuser != null && tbmuser.getCalonPegawai() != null
										&& tbmuser.getCalonPegawai().getId().equals(calonPegawai.getId())) {
									tbmuser.setCalonPegawai(calonPegawai);

									HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent()
											.getNativeRequest();
									HttpSession session1 = request.getSession(true);
									session1.setAttribute("CalonPegawai", tbmuser.getCalonPegawai());
									session1.setAttribute("mytbmuser", tbmuser);
									session1.setAttribute("usersTemp", tbmuser);
									session1.setAttribute("user", tbmuser);

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											Executions.getCurrent().sendRedirect("");
										}
									});

								}

							}
						});
			}
		});
		save.setParent(toolbar);
	}

	public static void onInfo(Event event, EventListener eventListener, CalonPegawai calonPegawai, Integer desktopWidth,
			Integer desktopHeight) throws Exception {
		CalonPegawaiAction calonPegawaiAction = new CalonPegawaiAction();
		calonPegawaiAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(calonPegawaiAction.addWindow);
		((MyWindow) calonPegawaiAction.addWindow)
				.setHeight(Common.isMobile() ? "100%" : desktopHeight == null ? "400px" : desktopHeight + "px");
		((MyWindow) calonPegawaiAction.addWindow)
				.setWidth(Common.isMobile() ? "100%" : desktopWidth == null ? "550px" : desktopWidth + "px");

		calonPegawaiAction.initInfo(calonPegawai);

		calonPegawaiAction.addWindow.setVisible(true);
		((MyWindow) calonPegawaiAction.addWindow).onModal();
	}

	public static void onAddExternalDaftar(Event event, EventListener eventListener, CalonPegawai calonPegawai,
			Integer desktopWidth, Integer desktopHeight) throws Exception {
		CalonPegawaiAction calonPegawaiAction = new CalonPegawaiAction();
		calonPegawaiAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(calonPegawaiAction.addWindow);
		((MyWindow) calonPegawaiAction.addWindow)
				.setHeight(Common.isMobile() ? "100%" : desktopHeight == null ? "400px" : desktopHeight + "px");
		((MyWindow) calonPegawaiAction.addWindow)
				.setWidth(Common.isMobile() ? "100%" : desktopWidth == null ? "550px" : desktopWidth + "px");

		calonPegawaiAction.initDaftar(calonPegawai);

		calonPegawaiAction.addWindow.setVisible(true);
		((MyWindow) calonPegawaiAction.addWindow).onModal();
	}

	class CalonPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CalonPegawai calonPegawai = (CalonPegawai) arg1;

			new Label(calonPegawai.getNoRegistrasi()).setParent(arg0);
			RevisiHelper.createNewRevisi(CalonPegawai.class, calonPegawai, calonPegawai.getNama()).setParent(arg0);
			new Label(calonPegawai.getGelombangPendaftaranPegawai() == null ? ""
					: calonPegawai.getGelombangPendaftaranPegawai().getNama()).setParent(arg0);

			String alamat = calonPegawai.getAlamatPegawai();
			if (calonPegawai.getKecamatan() != null) {
				String c = "Kec." + calonPegawai.getKecamatan().getNama();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (calonPegawai.getKota() != null) {
				String c = "Kab/Kota." + calonPegawai.getKota().getNama();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (calonPegawai.getPropinsi() != null) {
				String c = "Prop." + calonPegawai.getPropinsi().getNama();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (!calonPegawai.getKodePos().trim().isEmpty()) {
				String c = "Kode Pos " + calonPegawai.getKodePos();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (!calonPegawai.getTeleponPegawai().trim().isEmpty()) {
				String c = "Telp. " + calonPegawai.getTeleponPegawai();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}

			if (!calonPegawai.getAlamatEmail().trim().isEmpty()) {
				String c = "Email. " + calonPegawai.getAlamatEmail();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}

			new MyLabelKecil(alamat).setParent(arg0);

			new Label(calonPegawai.getKeterangan()).setParent(arg0);

			buatStatusSeleksi(calonPegawai).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(calonPegawai.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					calonPegawai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(calonPegawai);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, calonPegawai, CalonPegawaiAction.this).setParent(arg0);

		}

	}

	public static void onAddExternal(Event event, EventListener eventListener, CalonPegawai calonPegawai,
			Integer desktopWidth, Integer desktopHeight) throws Exception {
		CalonPegawaiAction calonPegawaiAction = new CalonPegawaiAction();
		calonPegawaiAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(calonPegawaiAction.addWindow);
		((MyWindow) calonPegawaiAction.addWindow)
				.setHeight(Common.isMobile() ? "100%" : desktopHeight == null ? "95%" : desktopHeight + "px");
		((MyWindow) calonPegawaiAction.addWindow)
				.setWidth(Common.isMobile() ? "100%" : desktopWidth == null ? "750px" : desktopWidth + "px");

		calonPegawaiAction.init(calonPegawai);

		calonPegawaiAction.addWindow.setVisible(true);
		((MyWindow) calonPegawaiAction.addWindow).onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		calonPegawai = (CalonPegawai) obj;
		init(calonPegawai);
		addWindow.setVisible(true);

		if (addWindow instanceof Window) {
			((Window) addWindow).onModal();
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new CalonPegawai());
		addWindow.setVisible(true);
		if (addWindow instanceof Window) {
			((Window) addWindow).onModal();
		}
	}

	public static void onAddExternal(Component addWindow, South mysouth, CalonPegawai calonPegawai) throws Exception {
		CalonPegawaiAction calonPegawaiAction = new CalonPegawaiAction();
		calonPegawaiAction.addWindow = addWindow;
		calonPegawaiAction.mysouth = mysouth;

		calonPegawaiAction.init(calonPegawai);

	}


	private String nvl(String value) {
		return value == null ? "" : value.trim();
	}

	private String ambilLinkDariKeteranganDokumen(String keterangan) {
		keterangan = nvl(keterangan);
		int p = keterangan.indexOf("FILE:");
		if (p < 0) {
			return "";
		}
		int end = keterangan.indexOf("|", p);
		return end > p ? keterangan.substring(p + 5, end).trim() : keterangan.substring(p + 5).trim();
	}

	private String ambilLinkLampiranDokumen(LampiranLain lampiranLain, CalonPegawaiPunyaDokumen dokumen) {
		try {
			if (lampiranLain != null) {
				/*
				 * Cara standar mendapatkan URL LampiranLain adalah memakai createLinkUri().
				 * Jangan membangun URL manual dari link/gdrive lebih dulu, karena createLinkUri()
				 * sudah menangani media lokal, media lama, gdrive/link, dan konfigurasi host aplikasi.
				 */
				try {
					String url = lampiranLain.createLinkUri();
					if (url != null && !url.trim().isEmpty()) {
						return url.trim();
					}
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

				if (lampiranLain.getLink() != null && !lampiranLain.getLink().trim().isEmpty()) {
					return lampiranLain.getLink().trim();
				}
				if (lampiranLain.getGdrive() != null && !lampiranLain.getGdrive().trim().isEmpty()) {
					return lampiranLain.forwardGDriveUrl();
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return ambilLinkDariKeteranganDokumen(dokumen == null ? "" : dokumen.getKeterangan());
	}

	private void tampilkanLinkLihatPrintDokumen(Vbox parent, LampiranLain lampiranLain,
			CalonPegawaiPunyaDokumen dokumen) {
		String link = ambilLinkLampiranDokumen(lampiranLain, dokumen);
		if (link == null || link.trim().isEmpty()) {
			return;
		}

		final String linkFinal = link.trim();
		A a = new A("Lihat / Print Dokumen");
		a.setStyle("display:inline-block;margin-top:6px;font-weight:bold;color:#2563eb;text-decoration:none;cursor:pointer;");
		a.setTooltiptext("Buka dokumen lampiran pada jendela baru");
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				bukaLampiranDiWindowBaru(linkFinal);
			}
		});
		a.setParent(parent);
	}

	private void bukaLampiranDiWindowBaru(String link) {
		if (link == null || link.trim().isEmpty()) {
			return;
		}
		if (Common.isMobile()) {
			ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
		} else {
			String safeUrl = link.replace("'", "\\'");
			Clients.evalJavaScript("popupCenter({url: '" + safeUrl + "', title: 'Lampiran', w: 1200, h: 650});");
		}
	}

	private LampiranLain ambilLampiranDokumen(CalonPegawaiPunyaDokumen dokumen) {
		if (dokumen == null || dokumen.getId() == null) {
			return null;
		}
		try {
			return (LampiranLain) FileFotoLain.ambil(false, dokumen.getId(),
					CalonPegawaiPunyaDokumen.class.getName(), LampiranLain.class);
		} catch (Exception e) {
			try {
				return LampiranLain.ambil(dokumen.getId(), CalonPegawaiPunyaDokumen.class.getName());
			} catch (Exception ex) {
				return null;
			}
		}
	}

	private boolean adaLampiranDokumen(CalonPegawaiPunyaDokumen dokumen) {
		LampiranLain lampiranLain = ambilLampiranDokumen(dokumen);
		if (lampiranLain != null) {
			return true;
		}
		return nvl(dokumen == null ? "" : dokumen.getKeterangan()).indexOf("FILE:") >= 0;
	}

	private int nilaiPrioritasDokumen(CalonPegawaiPunyaDokumen dokumen) {
		if (dokumen == null) {
			return -1000;
		}
		int nilai = 0;
		if (adaLampiranDokumen(dokumen)) {
			nilai += 100;
		}
		String status = nvl(dokumen.getStatus());
		if (CalonPegawaiPunyaDokumen.VERIFIKASI.equalsIgnoreCase(status)) {
			nilai += 40;
		} else if (CalonPegawaiPunyaDokumen.REVISI.equalsIgnoreCase(status)) {
			nilai += 20;
		}
		if (!nvl(dokumen.getKeterangan()).isEmpty()) {
			nilai += 10;
		}
		if (dokumen.getId() != null) {
			nilai += dokumen.getId().intValue() % 10;
		}
		return nilai;
	}

	@SuppressWarnings("unchecked")
	private CalonPegawaiPunyaDokumen ambilDokumenUtamaDanRapikanDuplikat(Session session, CalonPegawai calonPegawai,
			VerifikasiKelengkapanCalonPegawai template) {
		if (calonPegawai == null || calonPegawai.getId() == null || template == null || template.getId() == null) {
			CalonPegawaiPunyaDokumen baru = new CalonPegawaiPunyaDokumen();
			baru.setCalonPegawai(calonPegawai);
			baru.setVerifikasiKelengkapanCalonPegawai(template);
			return baru;
		}
		List<CalonPegawaiPunyaDokumen> data = session.createCriteria(CalonPegawaiPunyaDokumen.class)
				.add(Restrictions.eq("calonPegawai", calonPegawai))
				.add(Restrictions.eq("verifikasiKelengkapanCalonPegawai", template))
				.addOrder(Order.desc("id")).list();
		if (data == null || data.isEmpty()) {
			CalonPegawaiPunyaDokumen baru = new CalonPegawaiPunyaDokumen();
			baru.setCalonPegawai(calonPegawai);
			baru.setVerifikasiKelengkapanCalonPegawai(template);
			return baru;
		}
		CalonPegawaiPunyaDokumen utama = data.get(0);
		for (CalonPegawaiPunyaDokumen kandidat : data) {
			if (nilaiPrioritasDokumen(kandidat) > nilaiPrioritasDokumen(utama)) {
				utama = kandidat;
			}
		}
		for (CalonPegawaiPunyaDokumen duplikat : data) {
			if (duplikat == null || duplikat.getId() == null || utama.getId() == null
					|| duplikat.getId().equals(utama.getId())) {
				continue;
			}
			try {
				LampiranLain lampiranDuplikat = ambilLampiranDokumen(duplikat);
				LampiranLain lampiranUtama = ambilLampiranDokumen(utama);
				if (lampiranUtama == null && lampiranDuplikat != null && lampiranDuplikat.getId() != null) {
					session.refresh(lampiranDuplikat);
					lampiranDuplikat.setRef(utama.getId());
					boolean mulaiTransaksi = session.getTransaction() == null || !session.getTransaction().isActive();
					if (mulaiTransaksi) {
						session.getTransaction().begin();
					}
					session.update(lampiranDuplikat);
					if (mulaiTransaksi) {
						session.getTransaction().commit();
					}
				}
			} catch (Exception e) {
				try {
					if (session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/recruitment/CalonPegawaiAction.java:1261");
				}
			}
		}
		return utama;
	}

	private LampiranLain ambilLampiranUtamaDokumen(CalonPegawaiPunyaDokumen dokumen) {
		if (dokumen == null) {
			return null;
		}
		LampiranLain lampiranLain = ambilLampiranDokumen(dokumen);
		if (lampiranLain != null) {
			return lampiranLain;
		}
		try {
			Session session = HibernateUtil.currentSession();
			CalonPegawai calonPegawai = dokumen.getCalonPegawai();
			VerifikasiKelengkapanCalonPegawai template = dokumen.getVerifikasiKelengkapanCalonPegawai();
			if (calonPegawai == null || template == null) {
				return null;
			}
			List<CalonPegawaiPunyaDokumen> duplikat = session.createCriteria(CalonPegawaiPunyaDokumen.class)
					.add(Restrictions.eq("calonPegawai", calonPegawai))
					.add(Restrictions.eq("verifikasiKelengkapanCalonPegawai", template))
					.addOrder(Order.desc("id")).list();
			for (CalonPegawaiPunyaDokumen d : duplikat) {
				lampiranLain = ambilLampiranDokumen(d);
				if (lampiranLain != null) {
					return lampiranLain;
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return null;
	}

	private boolean isTemplateDokumenAktif(VerifikasiKelengkapanCalonPegawai template) {
		try {
			return template != null && template.getAktif();
		} catch (Exception e) {
			return false;
		}
	}

	private void tambahTemplateDokumenJikaValid(List<VerifikasiKelengkapanCalonPegawai> tujuan, Set<Long> ids,
			Set<String> names, VerifikasiKelengkapanCalonPegawai template) {
		if (!isTemplateDokumenAktif(template)) {
			return;
		}
		Long id = template.getId();
		String nama = nvl(template.getNama()).toLowerCase();
		if (id != null && ids.contains(id)) {
			return;
		}
		if (!nama.isEmpty() && names.contains(nama)) {
			return;
		}
		if (id != null) {
			ids.add(id);
		}
		if (!nama.isEmpty()) {
			names.add(nama);
		}
		tujuan.add(template);
	}

	@SuppressWarnings("unchecked")
	private List<VerifikasiKelengkapanCalonPegawai> ambilTemplateDokumenAktif(CalonPegawai calonPegawai) {
		List<VerifikasiKelengkapanCalonPegawai> hasil = new ArrayList<VerifikasiKelengkapanCalonPegawai>();
		Set<Long> ids = new HashSet<Long>();
		Set<String> names = new HashSet<String>();
		Session session = HibernateUtil.currentSession();

		try {
			GelombangPendaftaranPegawai gelombang = calonPegawai == null ? null
					: calonPegawai.getGelombangPendaftaranPegawai();
			if (gelombang != null && gelombang.getId() != null) {
				try {
					session.refresh(gelombang);
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				Set<VerifikasiKelengkapanCalonPegawai> selected = gelombang
						.getVerifikasiKelengkapanCalonPegawais();
				if (selected != null) {
					for (VerifikasiKelengkapanCalonPegawai template : selected) {
						tambahTemplateDokumenJikaValid(hasil, ids, names, template);
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		if (hasil.isEmpty()) {
			try {
				Map<Long, VerifikasiKelengkapanCalonPegawai> map = ConstantValues
						.ambilBerdasarClass(VerifikasiKelengkapanCalonPegawai.class);
				if (map != null) {
					for (VerifikasiKelengkapanCalonPegawai template : map.values()) {
						tambahTemplateDokumenJikaValid(hasil, ids, names, template);
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (hasil.isEmpty()) {
			try {
				List<VerifikasiKelengkapanCalonPegawai> list = session
						.createCriteria(VerifikasiKelengkapanCalonPegawai.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")).list();
				for (VerifikasiKelengkapanCalonPegawai template : list) {
					tambahTemplateDokumenJikaValid(hasil, ids, names, template);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		Collections.sort(hasil);
		return hasil;
	}

	@SuppressWarnings("unchecked")
	private void initDokumen(CalonPegawai calonPegawai) {
		Tbmuser tbmuser = Common.getCurrentUser();
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(east);

		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		Center center = new Center();
		center.setTitle("Daftar Dokumen Persyaratan");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("0px");

		column = new MyColumnConfig("Dokumen");
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig("Sifat");
		column.setParent(columns);

		column = new MyColumnConfig("Status");
		column.setParent(columns);

		column = new MyColumnConfig("Keterangan");
		column.setParent(columns);
		column.setWidth("20%");

		rowsDokumen = new Rows();
		rowsDokumen.setParent(grid);

		List<VerifikasiKelengkapanCalonPegawai> verifikasiKelengkapanCalonPegawais = ambilTemplateDokumenAktif(calonPegawai);
		Session session = HibernateUtil.currentSession();
		Set<Long> templateIdsDitampilkan = new HashSet<Long>();
		Set<String> templateNamesDitampilkan = new HashSet<String>();
		for (VerifikasiKelengkapanCalonPegawai verifikasiKelengkapanCalonPegawai : verifikasiKelengkapanCalonPegawais) {

			// Portal KARIR dan modul admin hanya menampilkan template dokumen yang aktif.
			// getAktif() pada model menganggap nilai null sebagai aktif, sehingga data lama tetap aman.
			if (verifikasiKelengkapanCalonPegawai == null || !verifikasiKelengkapanCalonPegawai.getAktif()) {
				continue;
			}
			String namaTemplate = nvl(verifikasiKelengkapanCalonPegawai.getNama()).toLowerCase();
			if (verifikasiKelengkapanCalonPegawai.getId() != null
					&& templateIdsDitampilkan.contains(verifikasiKelengkapanCalonPegawai.getId())) {
				continue;
			}
			if (!namaTemplate.isEmpty() && templateNamesDitampilkan.contains(namaTemplate)) {
				continue;
			}
			if (verifikasiKelengkapanCalonPegawai.getId() != null) {
				templateIdsDitampilkan.add(verifikasiKelengkapanCalonPegawai.getId());
			}
			if (!namaTemplate.isEmpty()) {
				templateNamesDitampilkan.add(namaTemplate);
			}

			CalonPegawaiPunyaDokumen temp = ambilDokumenUtamaDanRapikanDuplikat(session, calonPegawai,
					verifikasiKelengkapanCalonPegawai);
			final CalonPegawaiPunyaDokumen calonPegawaiPunyaDokumen = temp;
			final MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rowsDokumen);

			row.setValign("top");row.setAttribute("calonPegawaiPunyaDokumen", calonPegawaiPunyaDokumen);

			MyDetail detail = new MyDetail();
			row.appendChild(detail);
			detail.setOpen(true);
			Vbox vbox = new Vbox();
			vbox.setParent(detail);
			Hbox hbox = new Hbox();

			LampiranLain lampiranLain = ambilLampiranUtamaDokumen(calonPegawaiPunyaDokumen);
			row.setValign("top");row.setAttribute("lampiranLain", lampiranLain);
			Boolean tampilUpload = !calonPegawaiPunyaDokumen.getStatus()
					.equalsIgnoreCase(CalonPegawaiPunyaDokumen.VERIFIKASI);

			LampiranLain.createDownloadUploadFileLain(hbox, calonPegawaiPunyaDokumen.getId(),
					CalonPegawaiPunyaDokumen.class.getName(), verifikasiKelengkapanCalonPegawai.getNama(), false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getData();
							row.setValign("top");row.setAttribute("lampiranLain", lampiranLain);
						}
					}, null, false, false, false, tampilUpload);

			hbox.setParent(vbox);
			tampilkanLinkLihatPrintDokumen(vbox, lampiranLain, calonPegawaiPunyaDokumen);

			row.appendChild(new Label(verifikasiKelengkapanCalonPegawai.getNama()));
			row.appendChild(new Label(verifikasiKelengkapanCalonPegawai.getWajib() ? "Wajib" : "Opsional"));

			if (tbmuser != null && tbmuser.getCalonPegawai() != null) {
				row.appendChild(new Label(calonPegawaiPunyaDokumen.getStatus()));
				row.appendChild(new Label(calonPegawaiPunyaDokumen.getKeterangan()));
			} else {
				Combobox combobox = new Combobox();
				Comboitem comboitem = new Comboitem(CalonPegawaiPunyaDokumen.BELUM);
				comboitem.setValue(CalonPegawaiPunyaDokumen.BELUM);
				combobox.appendChild(comboitem);
				comboitem = new Comboitem(CalonPegawaiPunyaDokumen.VERIFIKASI);
				comboitem.setValue(CalonPegawaiPunyaDokumen.VERIFIKASI);
				combobox.appendChild(comboitem);
				comboitem = new Comboitem(CalonPegawaiPunyaDokumen.REVISI);
				comboitem.setValue(CalonPegawaiPunyaDokumen.REVISI);
				combobox.appendChild(comboitem);
				Common.selectComboItem(combobox, calonPegawaiPunyaDokumen.getStatus());
				combobox.setWidth("90%");
				combobox.setReadonly(true);
				row.appendChild(combobox);

				combobox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Combobox combobox = (Combobox) arg0.getTarget();
						calonPegawaiPunyaDokumen.setStatus((String) combobox.getSelectedItem().getValue());
						if (calonPegawaiPunyaDokumen.getId() != null) {
							Common.refreshUpdate(calonPegawaiPunyaDokumen);
						}
					}
				});

				final Textbox keterangan = new Textbox(calonPegawaiPunyaDokumen.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setRows(2);
				keterangan.setParent(row);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						calonPegawaiPunyaDokumen.setKeterangan(keterangan.getValue());
						Common.refreshSaveOrUpdate(calonPegawaiPunyaDokumen);

					}
				};

				keterangan.addEventListener("onChange", eventListener);
			}
		}
		verifikasiKelengkapanCalonPegawais = null;
	}

	private Tabpanel mainData(final CalonPegawai calonPegawai) throws Exception {
		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel);

		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		Center center = new Center();
		center.setTitle("Informasi Data Diri Calon Pegawai");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
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

		Tbmuser tbmuser = Common.getCurrentUser();

		Rows rows = new Rows();
		rows.setParent(grid);
		kode = new Textbox(calonPegawai.getNoRegistrasi());
		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIK *"));
		if (tbmuser != null && tbmuser.getCalonPegawai() != null && calonPegawai != null
				&& tbmuser.getCalonPegawai().getId().equals(calonPegawai.getId())) {
			row.appendChild(new Label(calonPegawai.getNoRegistrasi()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama *"));
		row.appendChild(nama = new Textbox(calonPegawai.getNama() == null ? "" : calonPegawai.getNama()));
		nama.setWidth("90%");

		jenisKelamin = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		jenisKelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		jenisKelamin.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin *"));
		Common.selectComboItem(jenisKelamin, calonPegawai.getJenisKelamin());
		row.appendChild(jenisKelamin);
		jenisKelamin.setWidth("90%");
		jenisKelamin.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat / Tanggal Lahir *"));
		Hbox hbox = new Hbox();
		hbox.appendChild(
				tempatLahir = new Textbox(calonPegawai.getTempatLahir() == null ? "" : calonPegawai.getTempatLahir()));
		hbox.appendChild(tanggalLahir = new MyDatebox(calonPegawai.getTanggalLahir()));
		row.appendChild(hbox);
		tempatLahir.setCols(15);
		tanggalLahir.addEventListener("onChange", checkKesamaan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Agama"));
		row.appendChild(agama = new Combobox());
		Common.insertCombo(agama, "nama", "keterangan", Agama.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(agama, calonPegawai.getAgama());
		agama.setWidth("90%");
		agama.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis *"));
		row.appendChild(gelombangPendaftaranPegawai = new Combobox());
		Common.insertCombo(gelombangPendaftaranPegawai, new String[] { "nama" }, "keterangan",
				GelombangPendaftaranPegawai.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(gelombangPendaftaranPegawai, calonPegawai.getGelombangPendaftaranPegawai());
		gelombangPendaftaranPegawai.setWidth("90%");
		gelombangPendaftaranPegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		row.appendChild(alamat = new Textbox(calonPegawai.getAlamatPegawai()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pos"));
		row.appendChild(kodePos = new Textbox(calonPegawai.getKodePos()));
		kodePos.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp."));
		row.appendChild(telp = new Textbox(calonPegawai.getTeleponPegawai()));
		telp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kecamatan"));
		row.appendChild(kecamatan = new AmbilDataKecamatanBanbox());
		kecamatan.setValue(calonPegawai.getKecamatan() == null ? "" : calonPegawai.getKecamatan().getNama());
		kecamatan.setAttribute("wilayah", calonPegawai.getKecamatan());
		kecamatan.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Propinsi"));
		row.appendChild(propinsi);
		propinsi.setWidth("90%");
		propinsi.setAttribute("wilayah", calonPegawai.getPropinsi());

		Common.createFieldKota(rows, "Kota/Kabupaten", kota, propinsi, calonPegawai.getKota(), true);

		Common.createKotaPropinsiListenerBerdasarkanKecamatan(propinsi, kota, kecamatan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
		row.appendChild(email = new Textbox(calonPegawai.getAlamatEmail()));
		email.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(calonPegawai.getKeterangan() == null ? "" : calonPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		rowGalery = new MyFormRow();
		rowGalery.setParent(rows);
		EventListener galeryEvent = new EventListener() {

			@SuppressWarnings({ "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowGalery);
				maps = new HashMap<Long, LampiranLain>();

				rowGalery.appendChild(new ais.ui.util.MyLabelConfig("Foto "));

				Grid grid = new Grid();
				grid.setSclass("dgrid");
				grid.setWidth("100%");
				grid.setParent(rowGalery);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();

				MyColumnConfig column = new MyColumnConfig();
				columns.appendChild(column);
				grid.appendChild(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

				Hbox myHbox = new Hbox();
				myHbox.setParent(row);
				myHbox.setHeight("30px");

				Hbox hboxGambar = new Hbox();
				hboxGambar.setParent(myHbox);
				tampilkanButton(hboxGambar, this);

				row = new MyFormRow();
				row.setParent(rows);

				myGridGaleri = (Rows) Common.tampilanScroll1(row).getParent();

				columns = new Columns();
				columns.setParent(myGridGaleri.getGrid());

				column = new MyColumnConfig("Foto");
				column.setWidth("90%");
				column.setParent(columns);

				column = new MyColumnConfig("Keterangan");
				column.setParent(columns);
				column.setWidth("0px");

				column = new MyColumnConfig("Hapus");
				column.setWidth("10%");
				column.setParent(columns);

				if (calonPegawai.getId() != null) {
					try {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
								.addOrder(Order.asc("id")).add(Restrictions.eq("ref", calonPegawai.getId()))
								.add(Restrictions.ilike("jenis", "Galery_CalonPegawai_", MatchMode.START)).list();
						for (LampiranLain lampiran : lampiranLains) {
							maps.put(lampiran.getId(), lampiran);
						}

						StreamingHibernateUtil.getInstance().closeSession();

					} catch (Exception e1) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/recruitment/CalonPegawaiAction.java:1774");
					}
				}

				reloadDataGambar(calonPegawai);
			}

		};

		galeryEvent.onEvent(null);

		return tabpanel;
	}

	private void init(final CalonPegawai calonPegawai) throws Exception {
		this.calonPegawai = calonPegawai;

		if (addWindow instanceof Window) {
			((Window) addWindow).setTitle("Pendataan Calon Pegawai");
		}

		propinsi = new Label();
		kota = new Label();

		Common.clear(addWindow);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		borderlayout.setParent(addWindow);

		east = new East();
		east.setWidth("40%");
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);

		initDokumen(calonPegawai);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		Tab tab = new Tab("Identitas Data Diri");
		tab.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		tabpanels.appendChild(mainData(calonPegawai));

		if (mysouth != null) {
			tampilSimpanData(mysouth);
		} else {
			South south = new South();
			south.setParent(borderlayout);
			tampilSimpanData(south);
		}
	}

	private void tampilSimpanData(Component south) {
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		if (addWindow != null && addWindow instanceof Window) {
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
		} else {
			toolbar.setAlign("center");
			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (onSave(event)) {
						MyMessageboxConfig.show("Data berhasil tersimpan", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
					}
				}
			});
			save.setParent(toolbar);
		}
	}

	private void tampilkanButton(Hbox hboxGambar, final EventListener eventListener) {
		Common.clear(hboxGambar);
		LampiranLain.createDownloadUploadFileLain(hboxGambar, calonPegawai.getId(),
				"Galery_CalonPegawai_" + Common.getGeneratedBarCode(), "Galeri Gambar", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahapegawaiCover = (LampiranLain) arg0.getData();
						maps.put(lainMahapegawaiCover.getId(), lainMahapegawaiCover);
						reloadDataGambar(calonPegawai);

						Common.createDefaultTimer(eventListener);
					}
				});
	}

	private void reloadDataGambar(final CalonPegawai calonPegawai) throws Exception {
		Common.clear(myGridGaleri);

		for (final LampiranLain lampiranLain : maps.values()) {
			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setValign("top");
			row.setParent(myGridGaleri);

			String link = FileFotoLain.ambilLinkLampiranLain(lampiranLain, false, false, LampiranLain.class);

			Image image = new Image(link);
			image.setStyle("max-width: 256px !important;min-width: 60px !important;min-height: 300px !important;");
			image.setSclass("gambar_profile");
			image.setWidth("95%");
			image.setParent(row);

			final Textbox textbox = new Textbox(lampiranLain.getDeskripsi());
			textbox.setWidth("90%");
			textbox.setRows(12);
			textbox.setParent(row);

			textbox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lampiranLain);
						lampiranLain.setDeskripsi(textbox.getValue());

						session.getTransaction().begin();
						session.update(lampiranLain);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}
			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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

											LampiranLain d = maps.remove(lampiranLain.getId());
											System.out.println("d = > " + d);

					try {
												Session session = StreamingHibernateUtil.getInstance().currentSession();

												session.getTransaction().begin();
												session.delete(lampiranLain);
												session.getTransaction().commit();

												StreamingHibernateUtil.getInstance().closeSession();
											} catch (Exception e) {
												StreamingHibernateUtil.getInstance().rollbackTransaction();
												Common.tampilErrorJikaAdmin(e);
											}

											reloadDataGambar(calonPegawai);
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
			button.setParent(row);
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("NIK Calon Pegawai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Calon Pegawai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisKelamin.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Kelamin harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (gelombangPendaftaranPegawai.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Calon Pegawai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (calonPegawai.getId() != null) {
			calonPegawai = (CalonPegawai) session.load(CalonPegawai.class, calonPegawai.getId());

		}
		calonPegawai.setNoRegistrasi(kode.getValue());
		calonPegawai.setNim(kode.getValue());
		calonPegawai.setNomorInduk(kode.getValue());
		calonPegawai.setTempatLahir(tempatLahir.getValue());
		calonPegawai.setTanggalLahir(tanggalLahir.getValue());
		calonPegawai.setAgama((Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));
		calonPegawai.setJenisKelamin((String) jenisKelamin.getSelectedItem().getValue());
		calonPegawai.setKecamatan((Wilayah) kecamatan.getAttribute("wilayah"));
		calonPegawai.setPropinsi((Propinsi) (propinsi.getAttribute("wilayah")));
		calonPegawai.setKota((Kota) (kota.getAttribute("wilayah")));
		calonPegawai.setNamaPegawai(nama.getValue());
		calonPegawai.setKeterangan(keterangan.getValue());
		calonPegawai.setAlamatPegawai(alamat.getValue());
		calonPegawai.setAlamatEmail(email.getValue());
		calonPegawai.setKode(kode.getValue());
		calonPegawai.setKodePos(kodePos.getValue());
		calonPegawai.setTeleponPegawai(telp.getValue());
		calonPegawai.setGelombangPendaftaranPegawai(
				(GelombangPendaftaranPegawai) gelombangPendaftaranPegawai.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, calonPegawai);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getCalonPegawai() != null
				&& tbmuser.getCalonPegawai().getId().equals(calonPegawai.getId())) {
			tbmuser.setCalonPegawai(calonPegawai);
		}

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					Tbmuser tbmuser = Common.getCurrentUser();
					if (tbmuser != null && tbmuser.getCalonPegawai() != null
							&& tbmuser.getCalonPegawai().getId().equals(calonPegawai.getId())) {
						tbmuser.setCalonPegawai(calonPegawai);

						HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent()
								.getNativeRequest();
						HttpSession session1 = request.getSession(true);
						session1.setAttribute("CalonPegawai", tbmuser.getCalonPegawai());
						session1.setAttribute("mytbmuser", tbmuser);
						session1.setAttribute("usersTemp", tbmuser);
						session1.setAttribute("user", tbmuser);

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Executions.getCurrent().sendRedirect("");
							}
						});

					}

					Session session = StreamingHibernateUtil.getInstance().currentSession();

					List<Row> rows = rowsDokumen == null ? new ArrayList<Row>() : (java.util.List) rowsDokumen.getChildren();
					for (Row row : rows) {
						CalonPegawaiPunyaDokumen calonPegawaiPunyaDokumen = (CalonPegawaiPunyaDokumen) row
								.getAttribute("calonPegawaiPunyaDokumen");
						if (calonPegawaiPunyaDokumen != null) {
							calonPegawaiPunyaDokumen.setCalonPegawai(calonPegawai);
							Common.refreshSaveOrUpdate(calonPegawaiPunyaDokumen);

							LampiranLain lampiranLain = (LampiranLain) row.getAttribute("lampiranLain");
							if (lampiranLain != null && lampiranLain.getId() != null) {

								session.refresh(lampiranLain);
								lampiranLain.setRef(calonPegawaiPunyaDokumen.getId());
								session.getTransaction().begin();
								session.update(lampiranLain);
								session.getTransaction().commit();
							}
						}
					}

					if (maps != null) {
						for (LampiranLain lampiranLain : maps.values()) {

						if (lampiranLain != null && lampiranLain.getId() != null) {
							session.refresh(lampiranLain);
							lampiranLain.setRef(calonPegawai.getId());

							session.getTransaction().begin();
							session.update(lampiranLain);
							session.getTransaction().commit();
						}
					}
					}

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

				CalonPegawai.reloadGaleries(calonPegawai);
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CalonPegawai.class)
				.add(searchaktif != null && searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(statusSeleksiCriterion())
				.add(statusSeleksiCheckboxCriterion());
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<CalonPegawai> calonPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(calonPegawai);
		grid.setRowRenderer(new CalonPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
