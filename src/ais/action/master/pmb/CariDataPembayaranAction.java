package ais.action.master.pmb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.sql.Blob;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.BuktiPembayaranAction;
import ais.action.report.CommonReportHelper;
import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BuktiPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainBiodataCalonMahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class CariDataPembayaranAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1155733365712985677L;

	private Textbox noRegistrasi;
	private Textbox pinPassword;
	private Row tampilanPin;

	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private MyGrid grid;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void onLogin(Event event) throws Exception {
		if (noRegistrasi.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon maaf, Nomor Pendaftaran belum diisi. Langkah yang dapat dilakukan: (1) Ketikkan Nomor Pendaftaran Anda pada kolom yang tersedia; (2) Pastikan nomor sesuai dengan yang tertera pada bukti pendaftaran; (3) Ulangi proses masuk.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Session session = HibernateUtil.currentSession();
		if (tampilanPin != null && tampilanPin.isVisible()) {
			if (pinPassword.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show(
						"Mohon maaf, PIN / Kata Sandi belum diisi. Langkah yang dapat dilakukan: (1) Ketikkan PIN / Kata Sandi Anda pada kolom yang tersedia; (2) Pastikan penulisan huruf besar dan kecil sudah benar; (3) Ulangi proses masuk.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}
			biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues.simpleObject(
					session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
							.add(Restrictions.ilike("pinPassword", pinPassword.getValue().trim(), MatchMode.EXACT))
							.add(Restrictions.or(
									Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
									Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
					BiodataCalonMahasiswa.class);
		} else {
			biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues.simpleObject(
					session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
							.add(Restrictions.or(
									Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
									Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
					BiodataCalonMahasiswa.class);
		}
		if (biodataCalonMahasiswa == null) {
			if (tampilanPin != null && tampilanPin.isVisible()) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, data Calon Mahasiswa dengan Nomor Pendaftaran \"{V1}\" dan PIN / Kata Sandi yang Anda masukkan tidak ditemukan. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan Nomor Pendaftaran dan PIN / Kata Sandi; (2) Pastikan tidak terdapat spasi berlebih; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, noRegistrasi.getValue());
			} else {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, data Calon Mahasiswa dengan Nomor Pendaftaran \"{V1}\" tidak ditemukan. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan Nomor Pendaftaran; (2) Pastikan nomor sesuai dengan bukti pendaftaran; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, noRegistrasi.getValue());
			}
			return;
		}

		if (biodataCalonMahasiswa != null) {

//			if (biodataCalonMahasiswa.getDitolak()) {
//				Messagebox.show(
//						"Maaf, Anda tidak diterima / ditolak untuk login, hubungi panitia untuk informasi lebih lanjut",
//						"PERINGATAN", Messagebox.OK, Messagebox.EXCLAMATION);
//				return;
//			}
//			if (biodataCalonMahasiswa.getMundur()) {
//				Messagebox.show("Maaf, Anda dinyatakan mengundurkan diri, hubungi panitia untuk informasi lebih lanjut",
//						"PERINGATAN", Messagebox.OK, Messagebox.EXCLAMATION);
//				return;
//			}

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(biodataCalonMahasiswa.getTanggalLahir());
			int thn = calendar.get(Calendar.YEAR);
			int bln = calendar.get(Calendar.MONTH);
			int tgl = calendar.get(Calendar.DATE);
			boolean kondisiTglLahir = (tahun.getSelectedItem() == null ? false
					: tahun.getSelectedItem().getValue().equals(thn))
					&& (bulan.getSelectedItem() == null ? false : bulan.getSelectedItem().getValue().equals(bln))
					&& (tanggal.getSelectedItem() == null ? false : tanggal.getSelectedItem().getValue().equals(tgl));

			if (kondisiTglLahir) {
				Common.setLogin(biodataCalonMahasiswa);
				afterLogin();
			} else {
				MyMessageboxConfig.show(
						"Mohon maaf, Nomor Pendaftaran atau Tanggal Lahir yang Anda masukkan belum sesuai. Langkah yang dapat dilakukan: (1) Periksa kembali Nomor Pendaftaran Anda; (2) Pastikan pilihan Tanggal, Bulan, dan Tahun Lahir sudah benar; (3) Ulangi proses masuk.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
		} else {
			MyMessageboxConfig.show(
					"Mohon maaf, Nomor Pendaftaran Anda tidak ditemukan. Langkah yang dapat dilakukan: (1) Pastikan Anda telah mengikuti prosedur pembayaran dengan benar; (2) Periksa kembali penulisan Nomor Pendaftaran; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		}
	}

	@SuppressWarnings("deprecation")
	public void afterLogin() throws Exception {
		onSearchDefault(null);

		menuLogin.setVisible(false);

		Common.clear(uploadMenu);

		uploadMenu.setHeight("40px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(uploadMenu);
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

//		MyFormRow row = new MyFormRow();row.setValign("top");
////		row.setParent(rows);
//
//		row.appendChild(new ais.ui.util.MyLabelBold("Nomor Pendaftaran"));
//		row.appendChild(new ais.ui.util.MyLabelBold(biodataCalonMahasiswa.getNoRegistrasi()));
//
//		if (biodataCalonMahasiswa.getNoUjian() != null) {
////			row.setParent(rows);
//			row.appendChild(new ais.ui.util.MyLabelBold("Nomor Ujian"));
//			row.appendChild(new ais.ui.util.MyLabelBold(biodataCalonMahasiswa.getNoUjian()));
//		}
//
//		row = new MyFormRow();
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelBold("Nama"));
//		row.appendChild(new ais.ui.util.MyLabelBold(biodataCalonMahasiswa.getNama()));

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
				"Upload " + LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_PENDAFTARAN + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				new ProsesUploadBuktiPembayaran(biodataCalonMahasiswa).upload(uploadEvent,
						LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_PENDAFTARAN);
			}
		});
		hbox.appendChild(upload);

		upload = new MyToolbarbuttonConfig(
				"Upload " + LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_DAFTAR_ULANG + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				new ProsesUploadBuktiPembayaran(biodataCalonMahasiswa).upload(uploadEvent,
						LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_DAFTAR_ULANG);
			}
		});
		hbox.appendChild(upload);
	}

	public static class ProsesUploadBuktiPembayaran {

		private BiodataCalonMahasiswa biodataCalonMahasiswa;

		public ProsesUploadBuktiPembayaran(BiodataCalonMahasiswa biodataCalonMahasiswa) {
			this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		}

		@SuppressWarnings("deprecation")
		public void upload(final UploadEvent uploadEvent, final String jenis) throws Exception {
			final Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

			try {

				File folder = CommonMedia.getMediaDirectory();

				final File f = new File(folder.getAbsolutePath() + "/" + URLEncoder.encode(
						ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_" + uploadEvent.getMedia().getName(),
						"UTF-8"));

				f.createNewFile();
				FileOutputStream fileOutputStream = new FileOutputStream(f);
				try {
					IOUtils.copyLarge(media.getStreamData(), fileOutputStream);
				} catch (Exception e) {
					IOUtils.write(media.getStringData(), fileOutputStream);
				}

				fileOutputStream.close();

				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

				LampiranLainBiodataCalonMahasiswa lampiranLainBiodataCalonMahasiswa = (LampiranLainBiodataCalonMahasiswa) streamingSession
						.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
						.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId()))
						.add(Restrictions.eq("jenis", jenis)).setMaxResults(1).uniqueResult();
				if (lampiranLainBiodataCalonMahasiswa != null) {
					streamingSession.getTransaction().begin();
					streamingSession.delete(lampiranLainBiodataCalonMahasiswa);
					streamingSession.getTransaction().commit();
				}

				lampiranLainBiodataCalonMahasiswa = new LampiranLainBiodataCalonMahasiswa();
				lampiranLainBiodataCalonMahasiswa.setJenis(jenis);
				lampiranLainBiodataCalonMahasiswa.setNama(uploadEvent.getMedia().getName());
				lampiranLainBiodataCalonMahasiswa.setKeterangan(uploadEvent.getMedia().getContentType());
				lampiranLainBiodataCalonMahasiswa.setBiodataCalonMahasiswa(biodataCalonMahasiswa.getId());
				Blob blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(new FileInputStream(f)));
				lampiranLainBiodataCalonMahasiswa.setFoto(blob);

				streamingSession.getTransaction().begin();
				streamingSession.save(lampiranLainBiodataCalonMahasiswa);
				streamingSession.getTransaction().commit();

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MyMessageboxConfig.showFormat("Berkas {V1} telah berhasil diunggah. Terima kasih.",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, jenis);
					}
				};

				Session session = HibernateUtil.currentSession();
				ConstantValues.PENDAFTARAN_CALON_MAHASISWA = (JenisKegiatan) session.createCriteria(JenisKegiatan.class)
						.add(Restrictions.eq("namaKegiatan", ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)).setMaxResults(1)
						.uniqueResult();

				ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU = (JenisKegiatan) session
						.createCriteria(JenisKegiatan.class)
						.add(Restrictions.eq("namaKegiatan", ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU))
						.setMaxResults(1).uniqueResult();

				JenisKegiatan jenisKegiatanPembayaran = jenis
						.equals(LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_PENDAFTARAN)
								? ConstantValues.PENDAFTARAN_CALON_MAHASISWA
								: ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU;

				System.out.println("jenisKegiatanPembayaran => " + jenisKegiatanPembayaran);

				BuktiPembayaran buktiPembayaran = new BuktiPembayaran();
				buktiPembayaran.setJenisKegiatan(jenisKegiatanPembayaran);

				LampiranLain lainMahasiswa = new LampiranLain();
				lainMahasiswa.setRef(-Common.randLong());
				lainMahasiswa.setNama(uploadEvent.getMedia().getName());
				lainMahasiswa.setKeterangan(uploadEvent.getMedia().getContentType());
				lainMahasiswa.setJenis(BuktiPembayaran.class.getName());

				streamingSession.getTransaction().begin();
				streamingSession.save(lainMahasiswa);
				streamingSession.getTransaction().commit();

				try {
					blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(new FileInputStream(f)));
					lainMahasiswa.setFoto(blob);
					streamingSession.getTransaction().begin();
					streamingSession.update(lainMahasiswa);
					streamingSession.getTransaction().commit();

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				BuktiPembayaranAction.onAddExternal(eventListener, buktiPembayaran, biodataCalonMahasiswa,
						lainMahasiswa, jenisKegiatanPembayaran);

			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

			StreamingHibernateUtil.getInstance().closeSession();
		}
	}

	private North uploadMenu;
	private North menuLogin;
	private Combobox tahun;
	private Combobox bulan;
	private Combobox tanggal;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		biodataCalonMahasiswa = Common.isLogin();
		if (biodataCalonMahasiswa != null) {
			afterLogin();
		} else {

			if (tampilanPin != null) { tampilanPin.setVisible(false); }
			MyComboitemConfig comboitem;
			for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 80; i < ais.ui.util.WaktuUtil
					.getCalendar().get(Calendar.YEAR) + 1; i++) {
				comboitem = new MyComboitemConfig();
				comboitem.setValue(i);
				comboitem.setLabel(i + "");
				tahun.appendChild(comboitem);
			}

			for (int i = 1; i <= 31; i++) {
				comboitem = new MyComboitemConfig();
				comboitem.setValue(i);
				comboitem.setLabel(i + "");
				tanggal.appendChild(comboitem);
			}

			Common.createComboBulan(bulan);
		}
		Common.initLaguage();
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) throws Exception {
		Session session = HibernateUtil.currentSession();

		List<Kegiatan> ruangPaketList = session.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true))
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
				.add(Restrictions.ge("persentaseLunas", 0.1))
				.add(Restrictions.eq("calonMahasiswa", biodataCalonMahasiswa)).list();

		ListModel strset = new SimpleListModel(ruangPaketList);
		grid.setRowRenderer(new CalonRenderer());
		grid.setModelCheckMobile(strset, true);

	}

	class CalonRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Kegiatan kegiatan = (Kegiatan) arg1;

			CommonMedia.tampilkanGambarKecil(kegiatan.getCalonMahasiswa()).setParent(arg0);

			new Label(kegiatan.getCalonMahasiswa().getNoRegistrasi()).setParent(arg0);
			new Label(kegiatan.getCalonMahasiswa().getNoUjian()).setParent(arg0);
			new Label(kegiatan.getCalonMahasiswa().getNama().toUpperCase()).setParent(arg0);

			new Label(kegiatan.toString()).setParent(arg0);
			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, false);

				}
			});
			button.setParent(toolbar);

		}

	}

	public void onReset() {
		noRegistrasi.setValue("");

	}

}
