package ais.action.master;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Blob;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
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
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Fileupload;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.virtualaccount.DownloadNoRegistrasiCalonMahasiswaBankNtt;
import ais.action.master.helper.virtualaccount.DownloadNoUjianCalonMahasiswaBankNtt;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankNtt;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.UploadVirtualAccount;
import ais.database.model.VirtualAccountBank;
import ais.database.model.file.FileFoto;
import ais.database.model.file.UploadVirtualAccountFileContent;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyFileUploadConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk upload virtual account bank ntt. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkode}, {@code Combobox
 * searchTahunAjaran}, {@code Combobox jenisSemester}, {@code Combobox tahunAkademik}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onDownloadTagihanMahasiswa()}, {@code onDownloadTagihanNoRegCalonMahasiswa()},
 * {@code onDownloadTagihanNoUjianCalonMahasiswa()}, {@code upload()}, {@code onSearchDefault()}); mutasi data
 * ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas
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
public class UploadVirtualAccountBankNTTAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	protected Combobox searchTahunAjaran;
	protected Combobox jenisSemester;

	private Combobox tahunAkademik;
	private Combobox ganjilGenap;
	private Combobox jenisUpload;
	private Textbox keterangan;

	private boolean delete = false;

	private UploadVirtualAccount uploadVirtualAccount;
	private MyToolbarbuttonConfig add;

	private Tabpanel downloadTagihanMahasiswa;
	private Tabpanel downloadTagihanNoRegCalonMahasiswa;
	private Tabpanel downloadTagihanNoUjianCalonMahasiswa;

	public void onDownloadTagihanMahasiswa(Event event) {
		if (downloadTagihanMahasiswa.getChildren().size() == 0) {
			DownloadTagihanMahasiswaBankNtt downloadTagihanMahasiswa = new DownloadTagihanMahasiswaBankNtt();
			downloadTagihanMahasiswa.setHeight("100%");
			downloadTagihanMahasiswa.setWidth("100%");
			downloadTagihanMahasiswa.setParent(this.downloadTagihanMahasiswa);
		}
	}

	public void onDownloadTagihanNoRegCalonMahasiswa(Event event) {
		if (downloadTagihanNoRegCalonMahasiswa.getChildren().size() == 0) {
			DownloadNoRegistrasiCalonMahasiswaBankNtt downloadTagihanMahasiswa = new DownloadNoRegistrasiCalonMahasiswaBankNtt();
			downloadTagihanMahasiswa.setHeight("100%");
			downloadTagihanMahasiswa.setWidth("100%");
			downloadTagihanMahasiswa.setParent(this.downloadTagihanNoRegCalonMahasiswa);
		}
	}

	public void onDownloadTagihanNoUjianCalonMahasiswa(Event event) {
		if (downloadTagihanNoUjianCalonMahasiswa.getChildren().size() == 0) {
			DownloadNoUjianCalonMahasiswaBankNtt downloadTagihanMahasiswa = new DownloadNoUjianCalonMahasiswaBankNtt();
			downloadTagihanMahasiswa.setHeight("100%");
			downloadTagihanMahasiswa.setWidth("100%");
			downloadTagihanMahasiswa.setParent(this.downloadTagihanNoUjianCalonMahasiswa);
		}
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

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
		if (searchTahunAjaran != null) { searchTahunAjaran.setSelectedIndex(searchTahunAjaran.getChildren().size() - 1); }

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		jenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		jenisSemester.appendChild(comboitem);
		if (jenisSemester != null) { jenisSemester.setReadonly(true); }

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class UploadVirtualAccountRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final UploadVirtualAccount uploadVirtualAccount = (UploadVirtualAccount) arg1;

			new Label(uploadVirtualAccount.getKode()).setParent(arg0);
			new Label(
					uploadVirtualAccount.getTahunAkademik() == null ? "Semua" : uploadVirtualAccount.getTahunAkademik())
					.setParent(arg0);
			new Label(uploadVirtualAccount.getGanjilGenap() == null ? "Semua" : uploadVirtualAccount.getGanjilGenap())
					.setParent(arg0);

			RevisiHelper
					.createNewRevisi(UploadVirtualAccount.class, uploadVirtualAccount, uploadVirtualAccount.getNama())
					.setParent(arg0);
			new Label(uploadVirtualAccount.getTipe()).setParent(arg0);
			new Label(uploadVirtualAccount.getJenisUpload() == null ? "Semua" : uploadVirtualAccount.getJenisUpload())
					.setParent(arg0);
			new Label(uploadVirtualAccount.getTerupload() + "").setParent(arg0);
			new Label(Common.dateFormat.get().format(uploadVirtualAccount.getTanggal_dirubah())).setParent(arg0);

			new Label(uploadVirtualAccount.getOleh()).setParent(arg0);
			new Label(uploadVirtualAccount.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", FileFoto.icon(null));
			toolbarbutton.setOrient("vertical");
			toolbarbutton.setParent(toolbar);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Session session = null;
					try {
						session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();

						UploadVirtualAccountFileContent myuploadVirtualAccountFileContent = (UploadVirtualAccountFileContent) session
								.createCriteria(UploadVirtualAccountFileContent.class)
								.add(Restrictions.eq("ref", uploadVirtualAccount.getId())).setMaxResults(1)
								.uniqueResult();
						Filedownload.save(myuploadVirtualAccountFileContent.ambilFile(),
								myuploadVirtualAccountFileContent.getFileMimeType());

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					} finally {
						// FIX bocor: session dedikasi (getSessionFactory().openSession(), TIDAK di MAP) tak ditutup
						// oleh StreamingHibernateUtil.closeSession() (yg menutup sesi MAP). Tutup manual di finally.
						if (session != null) {
							try { session.clear(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBankNTTAction.java:247");}
							try { session.disconnect(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBankNTTAction.java:248");}
							try { session.close(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBankNTTAction.java:249");}
						}
					}

				}

			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Re-Upload", "/img/settings_16x16.png");
			button.setTooltiptext("Re-Upload");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = null;
							try {
								session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();

								UploadVirtualAccountFileContent myuploadVirtualAccountFileContent = (UploadVirtualAccountFileContent) session
										.createCriteria(UploadVirtualAccountFileContent.class)
										.add(Restrictions.eq("ref", uploadVirtualAccount.getId())).setMaxResults(1)
										.uniqueResult();
								File file = myuploadVirtualAccountFileContent.ambilFile();

								upload(file, uploadVirtualAccount);

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							} finally {
								// FIX bocor: session dedikasi (getSessionFactory().openSession(), TIDAK di MAP) tak ditutup
								// oleh StreamingHibernateUtil.closeSession() (yg menutup sesi MAP). Tutup manual di finally.
								if (session != null) {
									try { session.clear(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBankNTTAction.java:288");}
									try { session.disconnect(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBankNTTAction.java:289");}
									try { session.close(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/master/UploadVirtualAccountBankNTTAction.java:290");}
								}
							}

							onSearchDefault(null);
						}
					});

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Delete", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.setOrient("vertical");
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
											Session session = HibernateUtil.currentSession();

											session.createSQLQuery(
													"delete from kegiatan where upload_virtual_account = "
															+ uploadVirtualAccount.getId())
													.executeUpdate();

											Common.refreshDelete(session, uploadVirtualAccount);
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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new UploadVirtualAccount());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private String namaFile = null;
	private String typeFile = null;
	private File file = null;
	private Label labelNama;

	private void init(final UploadVirtualAccount uploadVirtualAccount) throws Exception {

		tahunAkademik = new Combobox();
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		ganjilGenap = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		ganjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		ganjilGenap.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		ganjilGenap.appendChild(comboitem);

		this.uploadVirtualAccount = uploadVirtualAccount;
		addWindow.setTitle("Upload Data Virtual Account");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		if (uploadVirtualAccount.getKode() == null || uploadVirtualAccount.getKode().trim().isEmpty()) {

			String kode = (String) HibernateUtil.currentSession().createCriteria(UploadVirtualAccount.class)
					.setProjection(Projections.property("kode")).setMaxResults(1).addOrder(Order.desc("id"))
					.uniqueResult();

			if (kode != null) {
				uploadVirtualAccount.setKode(kode);
			}
		}

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File"));
		Fileupload fileupload = new MyFileUploadConfig("Upload Virtual Account");
		row.appendChild(fileupload);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
						if (media.getName().toLowerCase().endsWith("csv")) {

							InputStream inputStream = media.getStreamData();
							namaFile = media.getName();
							typeFile = media.getContentType();

							file = new File(Common.REAL_PATH + "/tmp/upload_Virtual_Account_"
									+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + namaFile);
							System.out.println("file upload -> " + file.getAbsolutePath());
							IOUtils.copyLarge(inputStream, new FileOutputStream(file));

							labelNama.setValue(namaFile);
						} else {
							MyMessageboxConfig.show("File yang anda upload harus CSV. " + media, "Error",
									MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		};
		fileupload.addEventListener("onUpload", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama File"));
		labelNama = new Label(uploadVirtualAccount.getNama());
		row.appendChild(labelNama);

		jenisUpload = new Combobox();
		comboitem = new MyComboitemConfig(UploadVirtualAccount.JENIS_UPLOAD_NIM);
		comboitem.setValue(UploadVirtualAccount.JENIS_UPLOAD_NIM);
		jenisUpload.appendChild(comboitem);

		comboitem = new MyComboitemConfig(UploadVirtualAccount.JENIS_UPLOAD_NO_REG);
		comboitem.setValue(UploadVirtualAccount.JENIS_UPLOAD_NO_REG);
		jenisUpload.appendChild(comboitem);

		comboitem = new MyComboitemConfig(UploadVirtualAccount.JENIS_UPLOAD_NO_UJIAN);
		comboitem.setValue(UploadVirtualAccount.JENIS_UPLOAD_NO_UJIAN);
		jenisUpload.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		jenisUpload.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Upload"));
		row.appendChild(jenisUpload);
		jenisUpload.setWidth("90%");

		Common.selectComboItem(jenisUpload, uploadVirtualAccount.getJenisUpload());
		jenisUpload.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik);
		Common.selectComboItem(tahunAkademik, uploadVirtualAccount.getTahunAkademik());
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(ganjilGenap);
		Common.selectComboItem(ganjilGenap, uploadVirtualAccount.getGanjilGenap());
		ganjilGenap.setWidth("90%");
		ganjilGenap.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				uploadVirtualAccount.getKeterangan() == null ? "" : uploadVirtualAccount.getKeterangan()));
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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan dan Proses", "/img/save.gif");
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
		if (file == null) {
			MyMessageboxConfig.show("Masukkan file yang di-upload ", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisUpload.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu jenis upload", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tahunAkademik.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu tahun akademik", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (ganjilGenap.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu semester", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (uploadVirtualAccount.getId() != null) {
			uploadVirtualAccount = (UploadVirtualAccount) session.load(UploadVirtualAccount.class,
					uploadVirtualAccount.getId());
		}

		uploadVirtualAccount.setJenisUpload((String) jenisUpload.getSelectedItem().getValue());
		uploadVirtualAccount.setBank(UploadVirtualAccount.NTT);
		uploadVirtualAccount.setKode("NTT");
		uploadVirtualAccount.setNama(namaFile);
		uploadVirtualAccount.setTipe(typeFile);
		uploadVirtualAccount.setKeterangan(keterangan.getValue());
		uploadVirtualAccount.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		uploadVirtualAccount.setGanjilGenap((String) ganjilGenap.getSelectedItem().getValue());

		if (uploadVirtualAccount.getId() != null) {
			session.update(uploadVirtualAccount);
		} else {
			session.save(uploadVirtualAccount);
		}

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = StreamingHibernateUtil.getInstance().currentSession();

				UploadVirtualAccountFileContent uploadVirtualAccountFileContent = new UploadVirtualAccountFileContent();
				Blob blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(new FileInputStream(file)));
				uploadVirtualAccountFileContent.setFoto(blob);
				uploadVirtualAccountFileContent.setNama(uploadVirtualAccount.getNama());
				uploadVirtualAccountFileContent.setFileMimeType(uploadVirtualAccount.getTipe());

				uploadVirtualAccountFileContent.setRef(uploadVirtualAccount.getId());
				uploadVirtualAccountFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());
				uploadVirtualAccountFileContent.setRef(uploadVirtualAccount.getId());

				session.getTransaction().begin();
				session.save(uploadVirtualAccountFileContent);
				session.getTransaction().commit();
				StreamingHibernateUtil.getInstance().closeSession();

				upload(file, uploadVirtualAccount);

				onSearchDefault(null);
			}
		}, "Harap tunggu, sedang melakukan proses upload data ..");

		return true;
	}

	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
	// private SimpleDateFormat dateFormatTime = new SimpleDateFormat("dd/MM/yy
	// HH:mm:ss");

	public void upload(final File file, final UploadVirtualAccount uploadVirtualAccount) throws Exception {

		final List<String> peringatan = new ArrayList<String>();
		final Intbox jumlahBerhasil = new Intbox(0);

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

					uploadVirtualAccount.setPeringatan(peringatan.toString());
					uploadVirtualAccount.setTerupload(jumlahBerhasil.getValue());
					Common.refreshUpdate(uploadVirtualAccount);

					MyMessageboxConfig.show("Upload data selesai dilakukan.\n" + "Jumlah data ter-upload = "
							+ Common.numberFormat.get().format(jumlahBerhasil.getValue()) + ".\n\n" + (peringatan.toString()),
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

		new Thread(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				try {

				try {
					String line = "";
					String cvsSplitBy = "|";

					@SuppressWarnings("resource")
					BufferedReader br = new BufferedReader(new FileReader(file));
					int rowCount = 0;
					while ((line = br.readLine()) != null) {
						rowCount++;
					}

					br = new BufferedReader(new FileReader(file));
					int i = 0;
					while ((line = br.readLine()) != null) {

						// use comma as separator
						String[] myData = StringUtils.split(line, cvsSplitBy);

						try {

							String nama = null;
							String nilai = null;
							String nim = null;
							try {
								nim = org.apache.commons.lang3.StringUtils.replace(myData[1].trim(), "\"", "");
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							Date tgl = null;
							try {
								String tangggal = myData[0];
								tgl = dateFormat.parse(org.apache.commons.lang3.StringUtils.replace(tangggal.trim(), "\"", ""));

								nama = myData[2];
								nilai = myData[3];
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

							if (nim != null && tgl != null) {

								System.out.println("tangggal = " + Common.dateFormat3.get().format(tgl) + ", nama = " + nama
										+ ", nilai = " + nilai);

								label.setValue("Upload data tangggal = " + Common.dateFormat3.get().format(tgl) + ", nama = "
										+ nama + ", nilai = " + nilai + " ("
										+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

								Session session = HibernateUtil.currentNativeSession();

								try {

									VirtualAccountBank virtualAccountBankNtt = (VirtualAccountBank) session
											.createCriteria(VirtualAccountBank.class)
											.add(Restrictions.eq("terjadiKendala", false))
											.add(Restrictions.eq("kode", nim)).uniqueResult();

									if (virtualAccountBankNtt.getTotal() < 0.01) {
										Double total = 0.0;
										try {
											String t = org.apache.commons.lang3.StringUtils.replace(myData[3].trim(), "\"", "");
											System.out.println("t => " + t);
											total = Double.parseDouble(t.trim());
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										System.out.println("total => " + total);
										virtualAccountBankNtt.setTotal(total);
									}

									if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {

										JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
										Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
										BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt
												.getBiodataCalonMahasiswa();

										Integer semester = virtualAccountBankNtt.getSemester();

										Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null
												? null
												: session.createCriteria(Kegiatan.class)
														.add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan()))
														.uniqueResult());

										if (kegiatan == null || kegiatan.getId() == null) {

											kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class)
													.addOrder(Order.asc("id"))

													.add(biodataCalonMahasiswa != null
															? Restrictions.eq("calonMahasiswa", biodataCalonMahasiswa)
															: Restrictions.eq("mahasiswa", mahasiswa))
													.add(Restrictions.eq("jenisKegiatan",
															virtualAccountBankNtt.getJenisKegiatan()))
													.add(Restrictions.eq("semster", semester))

													.setMaxResults(1).uniqueResult();
										}

										if (kegiatan == null || kegiatan.getId() == null) {
											kegiatan = new Kegiatan();
										}

										kegiatan.setKodeUnikLain(virtualAccountBankNtt.getKodeUnikLain());
										kegiatan.setJadwalPembayaran(virtualAccountBankNtt.getJadwalPembayaran());
										kegiatan.setNama(nama);
										kegiatan.setUploadVirtualAccount(uploadVirtualAccount);
										kegiatan.setAmount(virtualAccountBankNtt.getTotal());
										kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
										kegiatan.setMahasiswa(mahasiswa);
										kegiatan.setTahunAkademik(virtualAccountBankNtt.getTahunAkademik());
										kegiatan.setSemster(semester);
										kegiatan.setJenisKegiatan(jenisKegiatan);
										kegiatan.setTanggal(tgl);
										kegiatan.setValidated(1);
										kegiatan.setValidator(uploadVirtualAccount.getBank().toUpperCase() + " ("
												+ uploadVirtualAccount.getOleh() + ")");

										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, kegiatan);
										session.getTransaction().commit();

										List<Long> detailBiayasId = new ArrayList<Long>();
										for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(),
												",")) {
											try {
												detailBiayasId.add(Long.parseLong(id.trim()));
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}

										Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
												.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
														: Restrictions.in("id", detailBiayasId))
												.list();

										Double nilaiBiayaHarusDiBayars = 0.0;
										for (DetailBiaya detailBiaya : detailBiayas) {
											Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);
											DetailKegiatan detailKegiatan = kegiatan
													.ambilSatuDetailKegiatan(detailBiaya, true);
											if (detailKegiatan == null) {
												detailKegiatan = new DetailKegiatan();
												detailKegiatan.setBiaya(biaya);

												detailKegiatan.setDetailBiaya(detailBiaya);
												detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
												detailKegiatan.setKegiatan(kegiatan);
												// detailKegiatan.setAkunKredit(detailBiaya.getItemBiaya()
												// == null ? null
												// :
												// detailBiaya.getItemBiaya().getAkunKredit());
												session.getTransaction().begin();
												session.save(detailKegiatan);
												session.getTransaction().commit();
											}

											nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan,
													detailBiaya);

										}

										String bulanan = virtualAccountBankNtt.getBulanan();
										System.out.println("bulanan => " + bulanan + ", nama => " + nama);
										if (bulanan == null || bulanan.trim().isEmpty()) {
											Collection<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans = PembayaranUtil
													.getInstance()
													.getPengaturanPembayaranBulananTanpaTampilYangSudahDibayarOld(
															session, biodataCalonMahasiswa, mahasiswa, semester,
															jenisKegiatan, kegiatan, detailBiayas, false);
											System.out.println(
													"pengaturanPembayaranBulanans => " + pengaturanPembayaranBulanans);
											bulanan = "";
											for (PengaturanPembayaranBulanan biaya : pengaturanPembayaranBulanans) {
												bulanan += (bulanan.isEmpty() ? biaya.getId() : "," + biaya.getId());
											}
											System.out.println("bulanan baru => " + bulanan + ", nama => " + nama);
										}

										for (String idPemBul : StringUtils.split(bulanan, ",")) {
											if (Common.isNumber(idPemBul)) {
												PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
														.createCriteria(PengaturanPembayaranBulanan.class)
														.add(Restrictions.idEq(Long.parseLong(idPemBul)))
														.uniqueResult();

												if (pengaturanPembayaranBulanan != null) {
													String ref = "ntt-" + kegiatan.getId() + "-"
															+ pengaturanPembayaranBulanan.getId();
													CicilanPembayaran cicilanPembayaran = new CicilanPembayaran(
															pengaturanPembayaranBulanan.getDetailBiaya());
													cicilanPembayaran.setRef(ref);
													cicilanPembayaran.setValidator("bank NTT");
													// cicilanPembayaran.setKe(all.size()
													// + 1);
													cicilanPembayaran.setKegiatan(kegiatan);
													cicilanPembayaran.setItemBiaya(pengaturanPembayaranBulanan
															.getDetailBiaya().getItemBiaya());
													cicilanPembayaran.setPengaturanPembayaranBulanan(
															pengaturanPembayaranBulanan);
													// cicilanPembayaran
													// .setNilai(Common.isNumber(nilai)
													// ?
													// Double.parseDouble(nilai)
													// :
													// pengaturanPembayaranBulanan
													// .ambilNominalModifikasi(mahasiswa,
													// semester));
													cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());
													cicilanPembayaran.setNilai(pengaturanPembayaranBulanan
															.ambilNominalModifikasi(mahasiswa, semester));
													cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
													cicilanPembayaran.setTanggal(tgl);
													cicilanPembayaran.setJenisPembayaran(ConstantValues.TUNAI);
													cicilanPembayaran.setDenda(0.0);
													cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
													session.getTransaction().begin();
													if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
													session.getTransaction().commit();
												}
											}
										}

										jumlahBerhasil.setValue(jumlahBerhasil.getValue() + 1);

										Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session,
												kegiatan);
										Double jumlah = d[0];
										Double denda = d[1];
										kegiatan.setDenda(denda);
										kegiatan.setAmountTerhutang(
												nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda));
										kegiatan.setAmount(jumlah.doubleValue() > 0.1 ? jumlah.doubleValue()
												: virtualAccountBankNtt.getTotal());
										kegiatan.setValidator("bank NTT");

										session.getTransaction().begin();
										Common.refreshUpdate(session, kegiatan);
										session.getTransaction().commit();

										VirtualAccountBank.updateVa(virtualAccountBankNtt, tgl, kegiatan, "",
												"bank NTT");
										VirtualAccountBank.updateTotal(virtualAccountBankNtt,
												jumlah.doubleValue() > 0.1 ? jumlah.doubleValue()
														: virtualAccountBankNtt.getTotal());

									} else {
										String p = "GAGAL : KODE \"" + nim + "\" tidak ditemukan, nama = " + nama
												+ ", Tanggal = " + Common.dateFormat3.get().format(tgl) + ", nilai = " + nilai
												+ " \n";
										peringatan.add(p);
									}

								} catch (Exception e) {
									String p = e.getMessage();
									peringatan.add(p);
									Common.tampilErrorJikaAdmin(e);
								}

								HibernateUtil.closeSession();

							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
						i++;
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					peringatan.add("File tidak bisa terbaca..");
				}

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(UploadVirtualAccount.class);

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))

				.add(jenisSemester.getSelectedItem() == null || jenisSemester.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ganjilGenap", jenisSemester.getSelectedItem().getValue()));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<UploadVirtualAccount> uploadVirtualAccount = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(uploadVirtualAccount);
		grid.setRowRenderer(new UploadVirtualAccountRenderer());
		grid.setModelCheckMobile(strset);

	}

}
