package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.jsoup.Jsoup;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyChart;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.SimplePieModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaDiskusi;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.WaktuUtil;

/**
 * Helper composer ZK untuk fitur diskusi/tanya-jawab pada satu {@link Pertemuan} — menampilkan
 * seluruh {@link PertemuanPunyaDiskusi} (komentar dan balasan berjenjang, gaya gelembung media
 * sosial via {@code ais.ui.util.DiskusiUiHelper}) dari berbagai jenis penulis (mahasiswa, dosen,
 * calon mahasiswa, siswa, calon siswa, atau user umum), lengkap dengan lampiran (unggah langsung
 * atau Google Drive), balas-komentar berjenjang, dan (untuk admin/dosen) kontrol pengaturan diskusi
 * per pertemuan (tutup diskusi, izinkan unggah lampiran langsung/Google Drive).
 *
 * <p>
 * Untuk tampilan admin/dosen (bukan konteks mahasiswa/siswa/calon tunggal), {@link #display}
 * membangun tiga tab: "Isi Diskusi" (daftar komentar), "Peserta Diskusi" (daftar seluruh
 * peserta pertemuan dengan filter Semua/Ikut Diskusi/Tidak Ikut Diskusi/Akses/Tidak Akses, plus
 * tombol massal "Tidak ikut diskusi dianggap alpa" dan "Tidak akses dianggap alpa"), dan
 * "Statistik" (donut chart partisipasi diskusi via {@code HtmlChartHelper}). Data diskusi
 * dikelompokkan ke {@link #diskusis} (peta id pemilik → daftar id diskusi) untuk mempercepat
 * pencarian per peserta, dengan bulk-fetch eksplisit untuk menghindari N+1 query.
 * </p>
 *
 * <p>
 * Tiga method statis menyediakan aksi massal terkait kehadiran berbasis aktivitas: {@link
 * #aksesDianggapHadir} (menandai hadir seluruh peserta yang mengakses suatu {@link Tugas}/
 * {@link Pertemuan} dalam rentang waktu tertentu), {@link #tidakAksesDianggapAlpa} (menandai alpa
 * peserta yang TIDAK mengakses), dan {@link #diskusiDianggapHadir} (menandai hadir seluruh peserta
 * yang berpartisipasi dalam diskusi pertemuan, dengan isi diskusi disalin ke keterangan absensi).
 * Ketiganya dijalankan setelah konfirmasi dialog dan dalam transaksi terpisah dengan sesi Hibernate
 * yang dibuka/ditutup manual (bukan sesi thread-local) karena berjalan lewat
 * {@link Common#createDefaultTimer} setelah event asal selesai.
 * </p>
 *
 * <p>
 * Penulisan komentar baru (baik top-level maupun balasan) melalui dua jalur: inline langsung pada
 * baris ({@link #displayRow}, textbox + tombol Kirim/Upload) atau jendela modal terpisah
 * ({@link #onAddKomentar(Event, PertemuanPunyaDiskusi)}/{@link #onAddKomentar(PertemuanPunyaDiskusi,
 * PertemuanPunyaDiskusi, TreeSet, Mahasiswa, Dosen, BiodataCalonMahasiswa, Siswa, CalonSiswa,
 * EventListener) varian statis}, keduanya bermuara ke {@link #init}/{@link #onSave}). Setelah
 * simpan, {@link CommonEmail#infoAdaDiskusiPerkuliahan} mengirim notifikasi email adanya diskusi
 * baru.
 * </p>
 *
 * <p>
 * Mengimplementasikan {@link DataLoader} ({@link #loadData(Object)}). Method privat
 * {@link #closeHibernateSessionQuietly(Session)} adalah util bersama untuk menutup sesi Hibernate
 * manual (disconnect+close, exception ditelan) yang dipakai di seluruh operasi database kelas ini
 * yang berjalan di luar sesi thread-local.
 * </p>
 */
public class PertemuanPunyaDiskusiHelper implements DataLoader {

	private Pertemuan pertemuan;
	private Textbox isi;

	private Mahasiswa mahasiswa;
	private Dosen dosen;
	private PertemuanPunyaDiskusi pertemuanPunyaDiskusi;

	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	protected LampiranLain lampiran;
	private EventListener eventListener;
	private TreeSet<Long> pertemuanPunyaDiskusisa = null;
	private TreeSet<Long> pertemuanPunyaDiskusisaStatic = null;
	private Center center;
	private boolean tampilInfo;
	private MyCheckboxConfig urutkan;
	private Long selectedDiskusi = null;
	private Siswa siswa;
	private CalonSiswa calonSiswa;

	/**
	 * Membuat helper untuk satu identitas penulis diskusi. Tepat satu parameter biasanya diisi
	 * sesuai jenis user yang sedang login (atau semuanya {@code null} untuk konteks admin/dosen
	 * yang hanya melihat, bukan menulis, sebagai penulis tertentu).
	 *
	 * @param mahasiswa             mahasiswa penulis, boleh {@code null}
	 * @param dosen                 dosen penulis, boleh {@code null}
	 * @param biodataCalonMahasiswa calon mahasiswa penulis, boleh {@code null}
	 * @param siswa                 siswa penulis, boleh {@code null}
	 * @param calonSiswa            calon siswa penulis, boleh {@code null}
	 */
	public PertemuanPunyaDiskusiHelper(Mahasiswa mahasiswa, Dosen dosen, BiodataCalonMahasiswa biodataCalonMahasiswa,
			Siswa siswa, CalonSiswa calonSiswa) {
		this.siswa = siswa;
		this.calonSiswa = calonSiswa;
		this.mahasiswa = mahasiswa;
		this.dosen = dosen;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Merender satu baris {@link PertemuanPunyaDiskusi} ke {@code rowUtama}. Dua mode: bila
	 * {@code editable=true}, baris dirender sebagai FORM PENULISAN komentar baru (bukan menampilkan
	 * {@code pertemuanPunyaDiskusi} yang diberikan sebagai isi) — textbox + tombol Kirim/Upload
	 * lampiran/Google Drive, checkbox pengaturan pertemuan (tutup diskusi, izinkan lampiran) yang
	 * hanya tampil bagi admin/dosen non-mahasiswa, dan checkbox urutkan-terlama (disimpan sebagai
	 * preferensi user via {@code tbmuser.put}); bila {@code editable=false}, baris menampilkan ISI
	 * komentar yang sudah ada — foto+nama penulis (mahasiswa/siswa/calon mahasiswa/calon siswa/
	 * dosen/user umum, resolusi berurutan), waktu, isi (URL otomatis diubah jadi tautan, HTML lain
	 * di-escape via Jsoup), lampiran, dan tombol aksi (dikembalikan sebagai {@link Hbox} untuk diisi
	 * pemanggil dengan Balas/Hapus).
	 *
	 * @param pertemuanPunyaDiskusi   diskusi yang direpresentasikan baris ini (diabaikan isinya saat
	 *                                {@code editable=true})
	 * @param pertemuanPunyaDiskusisa set id diskusi milik pertemuan, diperbarui saat komentar baru
	 *                                dikirim inline
	 * @param rowUtama                baris grid tujuan render
	 * @param mahasiswa               mahasiswa penulis (untuk mode editable), boleh {@code null}
	 * @param biodataCalonMahasiswa   calon mahasiswa penulis (untuk mode editable), boleh {@code null}
	 * @param dosen                   dosen penulis (untuk mode editable), boleh {@code null}
	 * @param editable                {@code true} untuk merender form penulisan baru; {@code false}
	 *                                untuk menampilkan isi komentar yang sudah ada
	 * @param eventListener           callback penyegar tampilan setelah aksi (kirim komentar, ubah
	 *                                pengaturan pertemuan)
	 * @param eventListenerUtama      callback tambahan untuk memicu penyegaran urutan (dipakai
	 *                                checkbox "Terlama"); bila {@code null}, checkbox tersebut
	 *                                disembunyikan
	 * @return {@link Hbox} kosong (mode editable, berisi tombol pengaturan) atau berisi slot
	 *         tombol aksi Balas/Hapus (mode tampil) untuk diisi lebih lanjut oleh pemanggil
	 */
	public static Hbox displayRow(final PertemuanPunyaDiskusi pertemuanPunyaDiskusi,
			final TreeSet<Long> pertemuanPunyaDiskusisa, Row rowUtama, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final Dosen dosen, final boolean editable,
			final EventListener eventListener, final EventListener eventListenerUtama) throws Exception {
		rowUtama.setValign("top");

		Hbox hboxUtama = new Hbox();
		// Gaya komentar modern (gelembung media-sosial) — gaya terpusat di css_utama.css blok
		// ais-diskusi-* via ais.ui.util.DiskusiUiHelper.
		hboxUtama.setSclass(ais.ui.util.DiskusiUiHelper.SCLASS_ROW);
		hboxUtama.setParent(rowUtama);

		boolean bolehUloadDiKomentar = pertemuanPunyaDiskusi.getPertemuan().getIzinkanUploadLampiranDiKomentar();

		Vbox vbox = new Vbox();
		boolean mobile = Common.isMobile();

		Tbmuser usrkomentar = pertemuanPunyaDiskusi.getTbmuser();

		if (mobile) {
			if (pertemuanPunyaDiskusi.getMahasiswa() != null) {
				CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getMahasiswa(), "42px", "right").setParent(vbox);
			} else if (pertemuanPunyaDiskusi.getSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getSiswa(), "42px", "right").setParent(vbox);
			} else if (pertemuanPunyaDiskusi.getBiodataCalonMahasiswa() != null) {
				CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getBiodataCalonMahasiswa(), "42px", "right").setParent(vbox);
			} else if (pertemuanPunyaDiskusi.getCalonSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getCalonSiswa(), "42px", "right").setParent(vbox);
			} else if (pertemuanPunyaDiskusi.getDosen() != null) {
				CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getDosen(), "42px", "right").setParent(vbox);
			} else if (usrkomentar != null) {
				CommonMedia.tampilkanGambarKecil(usrkomentar, "42px", "right").setParent(vbox);
			} else {
				new Label().setParent(vbox);
			}
		} else {
			if (pertemuanPunyaDiskusi.getMahasiswa() != null) {
				CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getMahasiswa(), "42px", "right").setParent(hboxUtama);
			} else if (pertemuanPunyaDiskusi.getSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getSiswa(), "42px", "right").setParent(hboxUtama);
			} else if (pertemuanPunyaDiskusi.getBiodataCalonMahasiswa() != null) {
				CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getBiodataCalonMahasiswa(), "42px", "right").setParent(hboxUtama);
			} else if (pertemuanPunyaDiskusi.getCalonSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getCalonSiswa(), "42px", "right").setParent(hboxUtama);
			} else if (pertemuanPunyaDiskusi.getDosen() != null) {
				CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getDosen(), "42px", "right").setParent(hboxUtama);
			} else if (usrkomentar != null) {
				CommonMedia.tampilkanGambarKecil(usrkomentar, "42px", "right").setParent(hboxUtama);
			} else {
				new Label().setParent(hboxUtama);
			}
		}

		final Tbmuser tbmuser = Common.getCurrentUser();

		if (editable) {

			vbox.setParent(hboxUtama);
			vbox.setWidth("90%");
			Hbox tombol = new Hbox();

			final MyCheckboxConfig myCheckboxConfig = new MyCheckboxConfig("Tutup diskusi");
			myCheckboxConfig.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
			myCheckboxConfig.setStyle("font-size:12px;");
			myCheckboxConfig.setChecked(pertemuanPunyaDiskusi.getPertemuan().getKomentarDitutup());
			myCheckboxConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event aa) throws Exception {
					Session session = null;
					Transaction tx = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						tx = session.beginTransaction();
						Pertemuan pertemuan = (Pertemuan) session.get(Pertemuan.class, pertemuanPunyaDiskusi.getPertemuan().getId());
						if(pertemuan != null) {
							pertemuan.setKomentarDitutup(myCheckboxConfig.isChecked());
							session.update(pertemuan);
							pertemuanPunyaDiskusi.setPertemuan(pertemuan);
						}
						tx.commit();
						Common.createDefaultTimer(eventListener);
					} catch(Exception e) {
						if(tx != null) tx.rollback();
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:209");
					} finally {
						closeHibernateSessionQuietly(session);
					}
				}
			});

			final MyCheckboxConfig myCheckboxConfiglampiran = new MyCheckboxConfig("Izinkan upload file langsung");
			myCheckboxConfiglampiran
					.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
							&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
							&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
			myCheckboxConfiglampiran.setStyle("font-size:12px;");
			myCheckboxConfiglampiran
					.setChecked(pertemuanPunyaDiskusi.getPertemuan().getIzinkanUploadLampiranDiKomentar());
			myCheckboxConfiglampiran.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event aa) throws Exception {
					Session session = null;
					Transaction tx = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						tx = session.beginTransaction();
						Pertemuan pertemuan = (Pertemuan) session.get(Pertemuan.class, pertemuanPunyaDiskusi.getPertemuan().getId());
						if(pertemuan != null) {
							pertemuan.setIzinkanUploadLampiranDiKomentar(myCheckboxConfiglampiran.isChecked());
							session.update(pertemuan);
							pertemuanPunyaDiskusi.setPertemuan(pertemuan);
						}
						tx.commit();
						Common.createDefaultTimer(eventListener);
					} catch(Exception e) {
						if(tx != null) tx.rollback();
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:243");
					} finally {
						closeHibernateSessionQuietly(session);
					}
				}
			});

			final MyCheckboxConfig izinkanUploadLampiranDiGrive = new MyCheckboxConfig("Izinkan lampiran");
			izinkanUploadLampiranDiGrive
					.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
							&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
							&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
			izinkanUploadLampiranDiGrive.setStyle("font-size:12px;");
			izinkanUploadLampiranDiGrive
					.setChecked(pertemuanPunyaDiskusi.getPertemuan().getIzinkanUploadLampiranDiGrive());
			izinkanUploadLampiranDiGrive.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event aa) throws Exception {
					Session session = null;
					Transaction tx = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						tx = session.beginTransaction();
						Pertemuan pertemuan = (Pertemuan) session.get(Pertemuan.class, pertemuanPunyaDiskusi.getPertemuan().getId());
						if(pertemuan != null) {
							pertemuan.setIzinkanUploadLampiranDiGrive(izinkanUploadLampiranDiGrive.isChecked());
							session.update(pertemuan);
							pertemuanPunyaDiskusi.setPertemuan(pertemuan);
						}
						tx.commit();
						Common.createDefaultTimer(eventListener);
					} catch(Exception e) {
						if(tx != null) tx.rollback();
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:277");
					} finally {
						closeHibernateSessionQuietly(session);
					}
				}
			});

			if (pertemuanPunyaDiskusi.getPertemuan().getKomentarDitutup()) {
				MyLabelAgakKecilBold lbl;
				tombol.appendChild(lbl = new MyLabelAgakKecilBold("Untuk pertemuan ini, diskusi telah ditutup"));
				lbl.setStyle("font-size:11px;font-weight: bolder;color:red;");
				tombol.setParent(vbox);
				myCheckboxConfig.setParent(tombol);
			} else {
				final Textbox textbox = new Textbox();
				EventListener kirimEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (textbox.getValue().isEmpty()) {
							MyMessageboxConfig.show("Masukkan isi diskusi Anda", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						pertemuanPunyaDiskusi.setIsi(textbox.getValue());
						pertemuanPunyaDiskusi.setDosen(dosen);
						pertemuanPunyaDiskusi.setMahasiswa(mahasiswa);
						pertemuanPunyaDiskusi.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
						Tbmuser tbmuser = Common.getCurrentUser();
						if (tbmuser != null && tbmuser.getUserPassword() != null
								&& !tbmuser.getUserPassword().trim().equals("")) {
							pertemuanPunyaDiskusi.setTbmuser(tbmuser);
						}
						Common.refreshSaveOrUpdate(pertemuanPunyaDiskusi);
						// Guard: id bisa null bila simpan belum meng-assign id (mis. gagal/tertunda).
						// TreeSet di sini memakai comparator urutan-terbalik yang melempar NPE bila
						// elemen null -> tambah hanya jika id sudah ada.
						if (pertemuanPunyaDiskusi.getId() != null) {
							pertemuanPunyaDiskusisa.add(pertemuanPunyaDiskusi.getId());
						}
						Common.createDefaultTimer(eventListener);

						CommonEmail.infoAdaDiskusiPerkuliahan(pertemuanPunyaDiskusi.getPertemuan(), pertemuanPunyaDiskusi);
					}
				};

				Hbox comment = new Hbox();
				comment.setParent(vbox);

				textbox.setParent(comment);
				textbox.setWidth(mobile ? "150px" : "260px");
				textbox.setStyle("border: 1px solid #9fb8bf;border-radius: 5px;");
				textbox.setRows(2);
				textbox.addEventListener("onOK", kirimEventListener);

				Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("Kirim", "/img/Messaging-Sent-icon.png");
				toolbarbutton.setOrient("vertical");
				toolbarbutton.setStyle("font-size:10px");
				comment.appendChild(toolbarbutton);
				toolbarbutton.addEventListener("onClick", kirimEventListener);

				if (bolehUloadDiKomentar) {

					toolbarbutton = new MyToolbarbuttonConfig("Upload", "/img/Folders-Uploads-Folder-icon.png");
					toolbarbutton.setOrient("vertical");
					toolbarbutton.setStyle("font-size:10px");
					comment.appendChild(toolbarbutton);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							EventListener uploadeventListener = new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									pertemuanPunyaDiskusi.setIsi(textbox.getValue().trim().isEmpty() ? "File Terlampir" : textbox.getValue().trim());
									pertemuanPunyaDiskusi.setDosen(dosen);
									pertemuanPunyaDiskusi.setMahasiswa(mahasiswa);
									pertemuanPunyaDiskusi.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
									Tbmuser tbmuser = Common.getCurrentUser();
									if (tbmuser != null && tbmuser.getUserPassword() != null
											&& !tbmuser.getUserPassword().trim().equals("")) {
										pertemuanPunyaDiskusi.setTbmuser(tbmuser);
									}
									Common.refreshSaveOrUpdate(pertemuanPunyaDiskusi);
									pertemuanPunyaDiskusisa.add(pertemuanPunyaDiskusi.getId());

									LampiranLain copy = (LampiranLain) arg0.getData();
									if (copy != null) {
										Session session = null;
										Transaction stx = null;
										try {
											session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
											stx = session.beginTransaction();
											copy.setRef(pertemuanPunyaDiskusi.getId());
											session.update(copy);
											stx.commit();
										} catch (Exception e) {
											if(stx != null) stx.rollback();
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:380");
										} finally {
											closeHibernateSessionQuietly(session);
										}
									}
									CommonEmail.infoAdaDiskusiPerkuliahan(pertemuanPunyaDiskusi.getPertemuan(), pertemuanPunyaDiskusi);
									eventListener.onEvent(arg0);

								}
							};

							Toolbarbutton upload = FileFotoLain.tampilkanTombolUpload(null, null, uploadeventListener,
									null, null, LampiranLain.DISKUSI, false, null, "Lampiran", null, false,
									-Common.randLong(), false, null, LampiranLain.class);
							upload.setImage("/img/Folders-Uploads-Folder-icon.png");
							upload.setLabel("Upload");
							upload.setOrient("vertical");
							upload.setStyle("font-size:10px");
							EventListener elUpload = (EventListener) upload.getAttribute("eventListenerUpload");
							if (elUpload != null) {
								elUpload.onEvent(arg0);
							}
						}
					});

				}

				if (pertemuanPunyaDiskusi.getPertemuan().getIzinkanUploadLampiranDiGrive()) {
					EventListener uploadeventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							pertemuanPunyaDiskusi.setIsi(textbox.getValue().trim().isEmpty() ? "Data Terlampir" : textbox.getValue().trim());
							pertemuanPunyaDiskusi.setDosen(dosen);
							pertemuanPunyaDiskusi.setMahasiswa(mahasiswa);
							pertemuanPunyaDiskusi.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
							Tbmuser tbmuser = Common.getCurrentUser();
							if (tbmuser != null && tbmuser.getUserPassword() != null
									&& !tbmuser.getUserPassword().trim().equals("")) {
								pertemuanPunyaDiskusi.setTbmuser(tbmuser);
							}
							Common.refreshSaveOrUpdate(pertemuanPunyaDiskusi);
							pertemuanPunyaDiskusisa.add(pertemuanPunyaDiskusi.getId());

							LampiranLain copy = (LampiranLain) arg0.getData();
							if (copy != null) {
								Session session = null;
								Transaction stx = null;
								try {
									session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
									stx = session.beginTransaction();
									copy.setRef(pertemuanPunyaDiskusi.getId());
									session.update(copy);
									stx.commit();
								} catch (Exception e) {
									if(stx != null) stx.rollback();
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:437");
								} finally {
									closeHibernateSessionQuietly(session);
								}
							}
							CommonEmail.infoAdaDiskusiPerkuliahan(pertemuanPunyaDiskusi.getPertemuan(), pertemuanPunyaDiskusi);
							eventListener.onEvent(arg0);

						}
					};

					Toolbarbutton upload = FileFotoLain.tampilkanTombolUploadGdrive(null, null, uploadeventListener,
							null, null, LampiranLain.DISKUSI, false, null, "Lampiran", null, false, -Common.randLong(),
							false, LampiranLain.class);
					upload.setImage("/img/Google-Drive-icon_24.png");
					upload.setLabel("Lampiran");
					upload.setOrient("vertical");
					upload.setStyle("font-size:10px");
					comment.appendChild(upload);
				}

				final MyCheckboxConfig urutkan = new MyCheckboxConfig("Terlama");
				if (eventListenerUtama != null) {
					urutkan.setStyle("font-size:10px");
					urutkan.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tbmuser.put(urutkan.isChecked() + "", "urutkan_diskusi_berdasarkan_terlama");
							eventListenerUtama.onEvent(arg0);
						}
					});
					try {
						String pil = tbmuser.retreive("urutkan_diskusi_berdasarkan_terlama");
						urutkan.setChecked(pil == null || pil.trim().isEmpty() ? false : Boolean.parseBoolean(pil));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:472");
						// TODO: handle exception
					}

				}

				if (mobile) {
					myCheckboxConfig.setParent(tombol);
					izinkanUploadLampiranDiGrive.setParent(tombol);
					myCheckboxConfiglampiran.setParent(tombol);

					if (eventListenerUtama != null) {
						urutkan.setParent(tombol);
					}
				} else {
					Vbox opsi = new Vbox();
					opsi.setParent(comment);
					Hbox subOpsi = new Hbox();
					subOpsi.setParent(opsi);
					myCheckboxConfig.setParent(subOpsi);
					izinkanUploadLampiranDiGrive.setParent(subOpsi);
					myCheckboxConfiglampiran.setParent(opsi);

					if (eventListenerUtama != null) {
						urutkan.setParent(subOpsi);
					}
				}

				tombol.setParent(vbox);

				MyLabelAgakKecilBold lbl;
				tombol.appendChild(lbl = new MyLabelAgakKecilBold(
						"Untuk memulai diskusi, tulislah komentar, pertanyaan, atau suatu link yang mengarah ke website / file tertentu, bisa juga berupa postingan facebook, twitter, instagram, atau juga link youtube, google drive, dropbox, dll."));

				lbl.setStyle("font-size:12px;font-weight: bold;");

			}

			rowUtama.setAttribute("vbox", vbox);

			return tombol;
		} else {
			String oleh = pertemuanPunyaDiskusi.getBiodataCalonMahasiswa() != null
					? (pertemuanPunyaDiskusi.getBiodataCalonMahasiswa().getNama() + " (Calon Mahasiswa)")
					: (pertemuanPunyaDiskusi.getMahasiswa() != null
							? pertemuanPunyaDiskusi.getMahasiswa().getNama() + " (Mahasiswa)"
							: (pertemuanPunyaDiskusi.getSiswa() != null
									? pertemuanPunyaDiskusi.getSiswa().getNama() + " (Siswa)"
									: (pertemuanPunyaDiskusi.getCalonSiswa() != null
											? pertemuanPunyaDiskusi.getCalonSiswa().getNama() + " (Calon Siswa)"
											: "")));

			try {
				if (oleh.trim().equals("")) {
					oleh = pertemuanPunyaDiskusi.getDosen() != null
							? pertemuanPunyaDiskusi.getDosen().getNama() + " (Dosen)"
							: "";
				}

				if (oleh.trim().equals("")) {
					oleh = pertemuanPunyaDiskusi.getTbmuser() != null ? pertemuanPunyaDiskusi.getTbmuser().getUserNama()
							+ " (" + pertemuanPunyaDiskusi.getTbmuser().hakAkses().getRoleName() + ")" : "";
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:535");
				// TODO: handle exception
			}

			String waktu = pertemuanPunyaDiskusi.getTanggal_dirubah() == null ? ""
					: SmartDateTimeUtil.getDayString(pertemuanPunyaDiskusi.getTanggal_dirubah(), null)
							+ Common.dateFormat5.get().format(pertemuanPunyaDiskusi.getTanggal_dirubah());

			vbox.setHeight("100%");
			vbox.setWidth("100%");
			vbox.setParent(hboxUtama);

			String isi = pertemuanPunyaDiskusi.getIsi();

			List<String> urls = Common.getUrls(isi);
			for (String u : urls) {
				isi = org.apache.commons.lang3.StringUtils.replace(isi, u, "<a href='" + u + "' target='_blank'>" + u + "</a>");
			}

			try {
				isi = Jsoup.parse(isi.replaceAll("\n", "<br>")).text();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:557");
			}

			// Gelembung komentar modern (gaya media sosial) — markup & escape terpusat di
			// DiskusiUiHelper, gaya visual di css_utama.css blok ais-diskusi-*.
			new ais.ui.util.MyHtml(
					ais.ui.util.DiskusiUiHelper.bubbleKomentarHtml(oleh, waktu, isi))
					.setParent(vbox);

			for (String u : urls) {
				Common.displayUrlContent(u, vbox);
			}

			Vbox myVbox = new Vbox();
			myVbox.setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(myVbox);
			LampiranLain.createDownloadUploadFileLain(hbox, pertemuanPunyaDiskusi.getId(), LampiranLain.DISKUSI,
					LampiranLain.DISKUSI, false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, false, null, false, true);

			Hbox tombol = new Hbox();
			// Baris aksi (Balas/Hapus) tampil sebagai tautan teks halus ala media sosial.
			tombol.setSclass(ais.ui.util.DiskusiUiHelper.SCLASS_ACTIONS);
			tombol.setParent(vbox);

			rowUtama.setAttribute("vbox", vbox);

			return tombol;
		}
	}

	private java.util.Map<String, List<Long>> diskusis = new HashMap<String, List<Long>>();

	/**
	 * Memuat ulang tampilan diskusi ke {@link #center}: menyegarkan cache
	 * {@code PertemuanPunyaDiskusi} milik {@link #pertemuan} bila belum termuat, mengambil urutan
	 * diskusi sesuai preferensi {@link #urutkan}, dan (khusus konteks admin/dosen tanpa identitas
	 * peserta tunggal) membangun ulang peta {@link #diskusis} (id pemilik → daftar id diskusi, via
	 * bulk-fetch) untuk mendukung tab "Peserta Diskusi". Rendering aktual didelegasikan ke
	 * {@link DashboardTimelinePertemuan#loadKomentarDetail}.
	 *
	 * @param value tidak digunakan; ada untuk memenuhi kontrak {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		if (center != null) {
			Common.clear(center);
		}
		if (pertemuan == null) {
			return;
		}

		if (!pertemuan.udah()) {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				pertemuan.reInitPertemuanPunyaDiskusi(session);
			} catch(Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:612");
			} finally {
				closeHibernateSessionQuietly(session);
			}
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		TreeSet<Long> pertemuanPunyaDiskusisa = pertemuan.ambilPertemuanPunyaDiskusiTotal(urutkan == null ? false : urutkan.isChecked());

		try {
			if (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
					&& tbmuser.getSiswa() == null && siswa == null && calonSiswa == null) {
				diskusis.clear();

				// OPTIMASI: Bulk Fetch Data Diskusi
				Map<Long, PertemuanPunyaDiskusi> mapDiskusi = new HashMap<Long, PertemuanPunyaDiskusi>();
				if(pertemuanPunyaDiskusisa != null && !pertemuanPunyaDiskusisa.isEmpty()) {
					Session bulkSession = null;
					try {
						bulkSession = HibernateUtil.getSessionFactory().openSession();
						List<PertemuanPunyaDiskusi> listDs = ConstantValues.simpleList( bulkSession.createCriteria(PertemuanPunyaDiskusi.class)
								.add(Restrictions.in("id", pertemuanPunyaDiskusisa)),PertemuanPunyaDiskusi.class);
						for(PertemuanPunyaDiskusi p : listDs) {
							mapDiskusi.put(p.getId(), p);
						}
					} finally {
						if(bulkSession != null && bulkSession.isOpen()) try { bulkSession.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:638");}
					}
				}

				for (Long pertemuanPunyaDiskusiId : pertemuanPunyaDiskusisa) {
					if (pertemuanPunyaDiskusiId != null) {
						PertemuanPunyaDiskusi pertemuanPunyaDiskusi = mapDiskusi.get(pertemuanPunyaDiskusiId);

						if (pertemuanPunyaDiskusi != null && pertemuanPunyaDiskusi.getMahasiswa() != null) {
							List<Long> d = diskusis.get(pertemuanPunyaDiskusi.getMahasiswa().getId() + "_mhs");
							if (d == null) {
								d = new ArrayList<Long>();
								diskusis.put(pertemuanPunyaDiskusi.getMahasiswa().getId() + "_mhs", d);
							}
							d.add(pertemuanPunyaDiskusiId);
						} else if (pertemuanPunyaDiskusi != null && pertemuanPunyaDiskusi.getBiodataCalonMahasiswa() != null) {
							List<Long> d = diskusis.get(pertemuanPunyaDiskusi.getBiodataCalonMahasiswa().getId() + "_cal_mhs");
							if (d == null) {
								d = new ArrayList<Long>();
								diskusis.put(pertemuanPunyaDiskusi.getBiodataCalonMahasiswa().getId() + "_cal_mhs", d);
							}
							d.add(pertemuanPunyaDiskusiId);
						} else if (pertemuanPunyaDiskusi != null && pertemuanPunyaDiskusi.getDosen() != null) {
							List<Long> d = diskusis.get(pertemuanPunyaDiskusi.getDosen().getId() + "_dosen");
							if (d == null) {
								d = new ArrayList<Long>();
								diskusis.put(pertemuanPunyaDiskusi.getDosen().getId() + "_dosen", d);
							}
							d.add(pertemuanPunyaDiskusiId);
						}
					}
				}
			}
			DashboardTimelinePertemuan.loadKomentarDetail(null, "40px", pertemuanPunyaDiskusisa, pertemuan, center,
					"border:0px;background: transparent;", 0, 1000, tampilInfo, null, selectedDiskusi);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:674");
		}

	}

	/**
	 * Setelah konfirmasi dialog, menandai hadir ({@link ais.common.ConstantValues#MASUK}) seluruh
	 * peserta yang tercatat mengakses {@code tugas} (jenis akses ditentukan {@code akses}, mis.
	 * "audio"/"video"/dsb.) dalam rentang {@code mulai}..{@code sampai} — kecuali dosen (kecuali
	 * konfigurasi {@code dosen_ikut_masuk_ketika_di_akses_dianggap_hadir} aktif) dan peserta yang
	 * masuk daftar {@code mhsYgTidakIkut} tugas. Waktu absensi diambil dari
	 * {@code retreiveAbsensiMulai/Sampai} tugas, dengan fallback ke jam mulai/selesai pertemuan.
	 * Dijalankan dalam transaksi terpisah (bukan sesi thread-local) setelah timer default.
	 *
	 * @param tugas          sumber data akses ({@link TugasKelompok}/{@link Pertemuan}/{@link TugasPertemuan})
	 * @param akses          jenis akses yang dicek
	 * @param keterangan     teks keterangan yang ditulis ke absensi (dan dialog konfirmasi)
	 * @param mulai          batas awal rentang waktu akses yang dihitung
	 * @param sampai         batas akhir rentang waktu akses yang dihitung
	 * @param eventListener  callback dipanggil setelah proses selesai
	 */
	public static void aksesDianggapHadir(final Tugas tugas, final String akses, final String keterangan,
			final Date mulai, final Date sampai, final EventListener eventListener) throws Exception {
		MyMessageboxConfig.show(
				"Apakah yakin semua mahasiswa dan dosen yang " + keterangan + " dianggap hadir kelas ini ?",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Statusabsensi statusabsensi = ConstantValues.MASUK;
									TreeMap<String, String> d = tugas.ambilData(akses, null, "", mulai, sampai);

									boolean doseniktmasuk = Common.bolehKonfigurasi("dosen_ikut_masuk_ketika_di_akses_dianggap_hadir", Konfigurasi.TIDAK_AKTIF);

									Pertemuan pertemuan = (tugas instanceof TugasKelompok) ? ((TugasKelompok) tugas).ambilPertemuan()
											: (tugas instanceof Pertemuan) ? (Pertemuan) tugas : ((TugasPertemuan) tugas).ambilPertemuan();

									Session session = null;
									Transaction tx = null;
									try {
										session = HibernateUtil.getSessionFactory().openSession();
										tx = session.beginTransaction();

										if (pertemuan != null && pertemuan.getId() != null) {
											pertemuan = (Pertemuan) session.get(Pertemuan.class, pertemuan.getId());
										}

										for (String user : d.keySet()) {
											try {
												String[] u = user.split("-");
												String kode = u[1];
												String tipe = u.length > 2 ? u[2] : null;

												if (tipe != null && tipe.equalsIgnoreCase("Dosen") && !doseniktmasuk) {
													continue;
												}

												Long mhs = Long.parseLong(kode);
												if (!tugas.getMhsYgTidakIkut().contains("," + mhs + ",")) {

													String wMulai = pertemuan.retreiveAbsensiMulai(mhs);
													String wSampai = pertemuan.retreiveAbsensiSampai(mhs);
													if (wMulai == null || wMulai.trim().isEmpty()) {
														wMulai = pertemuan.getWaktuMulai();
													}
													if (wSampai == null || wSampai.trim().isEmpty()) {
														wSampai = pertemuan.getWaktuSelesai();
													}

													pertemuan.populate(mhs, statusabsensi, keterangan + " pada " + d.get(user), null, wMulai, wSampai, tipe);
												}
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:737");
											}
										}
										session.update(pertemuan);
										tx.commit();
									} catch(Exception e) {
										if(tx != null) tx.rollback();
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:744");
									} finally {
										closeHibernateSessionQuietly(session);
									}

									Common.createDefaultTimer(eventListener);
								}
							});
						}
					}
				});
	}

	public static void tidakAksesDianggapAlpa(final Tugas tugas, final String akses, final String keterangan,
			final Date mulai, final Date sampai, final EventListener eventListener) throws Exception {
		MyMessageboxConfig.show(
				"Apakah yakin semua mahasiswa yang " + keterangan + " dianggap alpa atau mangkir di kelas ini ?",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Statusabsensi statusabsensi = ConstantValues.TIDAK_ADA_ALASAN;
									TreeMap<String, String> d = tugas.ambilData(akses, null, "", mulai, sampai);

									Pertemuan pertemuan = (tugas instanceof TugasKelompok) ? ((TugasKelompok) tugas).ambilPertemuan()
											: (tugas instanceof Pertemuan) ? (Pertemuan) tugas : ((TugasPertemuan) tugas).ambilPertemuan();

									Session session = null;
									Transaction tx = null;
									try {
										session = HibernateUtil.getSessionFactory().openSession();
										tx = session.beginTransaction();

										if(pertemuan != null && pertemuan.getId() != null) {
											pertemuan = (Pertemuan) session.get(Pertemuan.class, pertemuan.getId());
										}

										List<Long> ada = new ArrayList<Long>();
										for (String user : d.keySet()) {
											try {
												String[] u = user.split("-");
												String kode = u[1];
												Long mhs = Long.parseLong(kode);
												ada.add(mhs);
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:796");
											}
										}

										List<Mahasiswa> mahasiswasTemorary = pertemuan.ambilMahasiswa();
										for (Mahasiswa o : mahasiswasTemorary) {
											if (!ada.contains(o.getId())) {
												Long mhs = o.getId();
												if (!tugas.getMhsYgTidakIkut().contains("," + mhs + ",")) {
													String wMulai = pertemuan.retreiveAbsensiMulai(mhs);
													String wSampai = pertemuan.retreiveAbsensiSampai(mhs);
													if (wMulai == null || wMulai.trim().isEmpty()) {
														wMulai = pertemuan.getWaktuMulai();
													}
													if (wSampai == null || wSampai.trim().isEmpty()) {
														wSampai = pertemuan.getWaktuSelesai();
													}
													pertemuan.populate(mhs, statusabsensi,
															keterangan + " sampai tanggal/waktu " + Common.dateFormat5.get().format(tugas == null || tugas.getSelesai() == null ? WaktuUtil.getDate() : tugas.getSelesai()),
															null, wMulai, wSampai, "Mahasiswa");
												}
											}
										}
										session.update(pertemuan);
										tx.commit();
									} catch(Exception e) {
										if(tx != null) tx.rollback();
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:823");
									} finally {
										closeHibernateSessionQuietly(session);
									}

									Common.createDefaultTimer(eventListener);
								}
							});
						}
					}
				});
	}

	public static void diskusiDianggapHadir(final Pertemuan pertemuan, final EventListener eventListener) throws Exception {
		MyMessageboxConfig.show(
				"Apakah yakin semua mahasiswa dan dosen yang melakukan diskusi atau tanya jawab dianggap hadir kelas ini ?",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event arg0) throws Exception {

									boolean doseniktmasuk = Common.bolehKonfigurasi("dosen_ikut_masuk_ketika_di_dikusi_dianggap_hadir", Konfigurasi.TIDAK_AKTIF);

									Session session = null;
									Transaction tx = null;
									try {
										session = HibernateUtil.getSessionFactory().openSession();
										tx = session.beginTransaction();

										Pertemuan dbPertemuan = pertemuan;
										if (dbPertemuan != null && dbPertemuan.getId() != null) {
											dbPertemuan = (Pertemuan) session.get(Pertemuan.class, dbPertemuan.getId());
										}

										// OPTIMASI: Bulk Fetch Data Diskusi
										TreeSet<Long> setDiskusis = dbPertemuan.ambilPertemuanPunyaDiskusiTotal(false);
										List<PertemuanPunyaDiskusi> listDiskusis = new ArrayList<PertemuanPunyaDiskusi>();
										if(!setDiskusis.isEmpty()) {
											listDiskusis = ConstantValues.simpleList(session.createCriteria(PertemuanPunyaDiskusi.class).add(Restrictions.in("id", setDiskusis)),PertemuanPunyaDiskusi.class);
										}

										for (PertemuanPunyaDiskusi pertemuanPunyaDiskusi : listDiskusis) {
											if (pertemuanPunyaDiskusi.getMahasiswa() != null) {
												Statusabsensi statusabsensi = ConstantValues.MASUK;

												String wMulai = dbPertemuan.retreiveAbsensiMulai(pertemuanPunyaDiskusi.getMahasiswa().getId());
												String wSampai = dbPertemuan.retreiveAbsensiSampai(pertemuanPunyaDiskusi.getMahasiswa().getId());
												if (wMulai == null || wMulai.trim().isEmpty()) wMulai = dbPertemuan.getWaktuMulai();
												if (wSampai == null || wSampai.trim().isEmpty()) wSampai = dbPertemuan.getWaktuSelesai();

												String isi = pertemuanPunyaDiskusi.getIsi();
												try { isi = Jsoup.parse(isi.replaceAll("\n", "<br>")).text(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:882");}

												dbPertemuan.populate(pertemuanPunyaDiskusi.getMahasiswa().getId(), statusabsensi,
														"Mengikuti diskusi pada hari " + Common.dateFormat5.get().format(pertemuanPunyaDiskusi.getTanggal_dirubah())
																+ " dengan isi diskusi sbb : " + isi, null, wMulai, wSampai, "Mahasiswa");
											} else if (pertemuanPunyaDiskusi.getDosen() != null && doseniktmasuk) {
												Statusabsensi statusabsensi = ConstantValues.MASUK;

												String wMulai = dbPertemuan.retreiveAbsensiMulai(pertemuanPunyaDiskusi.getDosen().getId());
												String wSampai = dbPertemuan.retreiveAbsensiSampai(pertemuanPunyaDiskusi.getDosen().getId());
												if (wMulai == null || wMulai.trim().isEmpty()) wMulai = dbPertemuan.getWaktuMulai();
												if (wSampai == null || wSampai.trim().isEmpty()) wSampai = dbPertemuan.getWaktuSelesai();

												dbPertemuan.populate(pertemuanPunyaDiskusi.getDosen().getId(), statusabsensi,
														"Mengikuti diskusi pada hari " + Common.dateFormat5.get().format(pertemuanPunyaDiskusi.getTanggal_dirubah())
																+ " dengan isi diskusi sbb : " + pertemuanPunyaDiskusi.getIsi(), null, wMulai, wSampai, "Dosen");
											}
										}

										session.update(dbPertemuan);
										tx.commit();
									} catch(Exception e) {
										if(tx != null) tx.rollback();
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:905");
									} finally {
										closeHibernateSessionQuietly(session);
									}

									Common.createDefaultTimer(eventListener);
								}
							});
						}
					}
				});
	}

	public void display(final Pertemuan pertemuan, final Component component, final boolean tampilInfo,
			final Long selectedDiskusi) {
		this.pertemuan = pertemuan;
		this.tampilInfo = tampilInfo;
		this.selectedDiskusi = selectedDiskusi;
		if (component != null) {
			Common.clear(component);
		}

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(component);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);

		final Tbmuser tbmuser = Common.getCurrentUser();

		MyToolbarbuttonConfig masuk = new MyToolbarbuttonConfig("Dianggap hadir", "/img/svg/check2.svg");
		masuk.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& pertemuan.getJadwalUjianPMB() == null && pertemuan.getJadwalUjianPSB() == null);
		masuk.setTooltiptext("Diskusi dianggap hadir");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PertemuanPunyaDiskusiHelper.diskusiDianggapHadir(pertemuan, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						new PertemuanHelper(Common.getCurrentUser().getMahasiswa(), null).display(pertemuan, PertemuanPunyaDiskusiHelper.this, 0);
					}
				});
			}
		});
		masuk.setParent(toolbar);

		final String[] contents = new String[] { "isi", "mahasiswa.nama", "tbmuser", "parent.isi" };
		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Lampiran");
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(PertemuanPunyaDiskusi.class,
				new DataCriteria() {
					@Override
					public Criteria initCriteria(boolean order) {
						return HibernateUtil.currentSession().createCriteria(PertemuanPunyaDiskusi.class)
								.addOrder(Order.asc("id")).add(Restrictions.eq("pertemuan", pertemuan));
					}
				}, "Download", FileFoto.icon(null), columnHeadersAdding, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] objects = (Object[]) arg0.getData();
						PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) objects[0];
						XSSFRow row = (XSSFRow) objects[2];
						final XSSFCellStyle hlink_style = (XSSFCellStyle) objects[6];

						LampiranLain lam = LampiranLain.ambil(pertemuanPunyaDiskusi.getId(), LampiranLain.DISKUSI);
						XSSFCell cell = row.createCell(contents.length);

						if (lam != null) {
							cell.setCellStyle(hlink_style);
							cell.setCellValue(lam.getNama());
							String url = lam.createLinkUri();
							XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper().createHyperlink(Hyperlink.LINK_URL);
							link.setAddress(url);
							cell.setHyperlink(link);
						}
					}
				}, false, null, "", contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				pertemuan.belum();
				loadData(null);
			}
		});
		button.setParent(toolbar);

		urutkan = new MyCheckboxConfig("Urutkan terlama");
		urutkan.setStyle("font-size:10px");
		urutkan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				tbmuser.put(urutkan.isChecked() + "", "urutkan_diskusi_berdasarkan_terlama");
				loadData(null);
			}
		});
		try {
			String pil = tbmuser.retreive("urutkan_diskusi_berdasarkan_terlama");
			urutkan.setChecked(pil == null || pil.trim().isEmpty() ? false : Boolean.parseBoolean(pil));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:1011");}
		urutkan.setParent(toolbar);

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				RevisiDiskusiHelper revisiHelper = new RevisiDiskusiHelper(pertemuan, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						pertemuan.belum();
						loadData(null);
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();
			}
		});
		button.setParent(toolbar);

		if (biodataCalonMahasiswa == null && mahasiswa == null && siswa == null && calonSiswa == null) {
			Center mycenter = new Center();
			mycenter.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(mycenter, true);

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(mycenter);
			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tabSoal = new MyTabConfig("Isi Diskusi");
			tabs.appendChild(tabSoal);

			MyTabConfig tab2 = new MyTabConfig("Peserta Diskusi");
			tab2.setParent(tabs);

			MyTabConfig tab3 = new MyTabConfig("Statistik");
			tabs.appendChild(tab3);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel parentT = new ais.ui.util.MyTabpanel();
			parentT.setParent(tabpanels);

			borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(parentT);

			center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			final Tabpanel tabpanelPesertaBelum = new ais.ui.util.MyTabpanel();
			tabpanelPesertaBelum.setParent(tabpanels);
			tabpanelPesertaBelum.setHeight("2000px");

			tab2.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelPesertaBelum.getChildren().isEmpty()) {

						final Combobox cariPeserta = new Combobox();
						cariPeserta.setCols(6);
						Comboitem comboitem = new Comboitem("Semua Peserta");
						comboitem.setValue(0);
						cariPeserta.appendChild(comboitem);

						comboitem = new Comboitem("Ikut Dikusi");
						comboitem.setValue(1);
						cariPeserta.appendChild(comboitem);

						comboitem = new Comboitem("Tidak Ikut Dikusi");
						comboitem.setValue(2);
						cariPeserta.appendChild(comboitem);

						comboitem = new Comboitem("Akses");
						comboitem.setValue(3);
						cariPeserta.appendChild(comboitem);

						comboitem = new Comboitem("Tidak Akses");
						comboitem.setValue(4);
						cariPeserta.appendChild(comboitem);
						cariPeserta.setSelectedIndex(0);
						cariPeserta.setReadonly(true);

						Borderlayout myborderlayoutlagi = new Borderlayout();
						myborderlayoutlagi.setParent(tabpanelPesertaBelum);

						Hbox hbox = new Hbox();
						hbox.appendChild(new MyLabelConfig("Peserta : "));
						final Textbox cari = new Textbox("");
						cari.setParent(hbox);
						cari.setCols(10);

						Center mycenterlagi = new Center();
						mycenterlagi.setParent(myborderlayoutlagi);
						ais.ui.util.ZkCompat.setFlex(mycenterlagi, true);

						final Grid gridLocal = new Grid();
						gridLocal.setSclass("dgrid fgrid");
						gridLocal.setParent(mycenterlagi);
						gridLocal.setHeight("100%");
						gridLocal.setWidth("100%");

						Columns columnsLocal = new Columns();
						columnsLocal.setParent(gridLocal);

						MyColumnConfig columnLocal = new MyColumnConfig();
						columnLocal.appendChild(hbox);
						columnLocal.setParent(columnsLocal);

						columnLocal = new MyColumnConfig("Isi Diskusi");
						columnLocal.setParent(columnsLocal);
						columnLocal.setWidth("55%");

						// PRE-FETCH BATCH DATA UNTUK RENDERER AGAR EFISIEN MEMORI
						final Map<Long, PertemuanPunyaDiskusi> prefetchMap = new HashMap<Long, PertemuanPunyaDiskusi>();
						Session cacheSession = null;
						try {
							cacheSession = HibernateUtil.getSessionFactory().openSession();
							List<Long> allDiskusisIds = new ArrayList<Long>();
							for(List<Long> ls : diskusis.values()) {
								if(ls != null) allDiskusisIds.addAll(ls);
							}
							if(!allDiskusisIds.isEmpty()) {
								List<PertemuanPunyaDiskusi> allList = ConstantValues.simpleList(cacheSession.createCriteria(PertemuanPunyaDiskusi.class).add(Restrictions.in("id", allDiskusisIds)),PertemuanPunyaDiskusi.class);
								for(PertemuanPunyaDiskusi pd : allList) {
									prefetchMap.put(pd.getId(), pd);
								}
							}
						} finally {
							if(cacheSession != null && cacheSession.isOpen()) try{ cacheSession.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:1144");}
						}

						gridLocal.setRowRenderer(new ais.ui.util.MyRowRenderer() {
							@Override
							public void render(Row arg0, Object arg1) throws Exception {
								arg0.setValign("top");
								Integer c = (Integer) cariPeserta.getSelectedItem().getValue();

								Mahasiswa oMhs = (arg1 instanceof Mahasiswa) ? (Mahasiswa) arg1 : null;
								BiodataCalonMahasiswa oCalMhs = (arg1 instanceof BiodataCalonMahasiswa) ? (BiodataCalonMahasiswa) arg1 : null;
								Dosen oDsn = (arg1 instanceof Dosen) ? (Dosen) arg1 : null;
								Siswa oSiswa = (arg1 instanceof Siswa) ? (Siswa) arg1 : null;
								CalonSiswa oCalSiswa = (arg1 instanceof CalonSiswa) ? (CalonSiswa) arg1 : null;

								Hbox hboxRow = new Hbox();
								hboxRow.setParent(arg0);
								if (oMhs != null) CommonMedia.tampilkanGambarKecil(oMhs).setParent(hboxRow);
								else if (oSiswa != null) CommonMedia.tampilkanGambarKecil(oSiswa).setParent(hboxRow);
								else if (oCalSiswa != null) CommonMedia.tampilkanGambarKecil(oCalSiswa).setParent(hboxRow);
								else if (oCalMhs != null) CommonMedia.tampilkanGambarKecil(oCalMhs).setParent(hboxRow);
								else if (oDsn != null) CommonMedia.tampilkanGambarKecil(oDsn).setParent(hboxRow);

								Vbox vb = new Vbox();
								vb.setParent(hboxRow);
								vb.appendChild(new Label(oCalSiswa != null ? oCalSiswa.getNomorInduk() : oSiswa != null ? oSiswa.getNomorInduk() : oDsn != null ? oDsn.getNidn() : oMhs == null ? oCalMhs.getNoRegistrasi() : oMhs.getNim()));
								vb.appendChild(new Label(oCalSiswa != null ? oCalSiswa.getNama() : oSiswa != null ? oSiswa.getNama() : oDsn != null ? oDsn.getNama() : oMhs == null ? oCalMhs.getNama() : oMhs.getNama()));

								Long id = oCalSiswa != null ? oCalSiswa.getId() : oSiswa != null ? oSiswa.getId() : oDsn != null ? oDsn.getId() : oMhs != null ? oMhs.getId() : oCalMhs.getId();

								TreeMap<String, String> d = pertemuan.ambilData("akses", id == null ? "-1" : id.toString());
								String aksesPertemuan = "";

								if (!d.isEmpty()) {
									Date aks = null;
									try {
										aksesPertemuan = d.values().toString().replaceAll("\\[", "").replaceAll("\\]", "");
										aks = Common.dateFormat3.get().parse(aksesPertemuan);
										aksesPertemuan = SmartDateTimeUtil.getDayString(aks, null) + Common.dateFormat5.get().format(aks);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:1183");}
								}

								MyLabelAgakKecil checkboxConfig = new MyLabelAgakKecil(d.isEmpty() ? "Belum Akses Pertemuan" : "Terakhir akses : " + aksesPertemuan);
								checkboxConfig.setParent(vb);

								List<Long> diskusi = oMhs != null ? diskusis.get(oMhs.getId() + "_mhs") : oCalMhs != null ? diskusis.get(oCalMhs.getId() + "_cal_mhs") : oDsn != null ? diskusis.get(oDsn.getId() + "_dosen") : null;

								if (c.equals(1) && (diskusi == null || diskusi.isEmpty())) arg0.setVisible(false);
								else if (c.equals(2) && (diskusi != null && !diskusi.isEmpty())) arg0.setVisible(false);
								else if (c.equals(3) && d.isEmpty()) arg0.setVisible(false);
								else if (c.equals(4) && !d.isEmpty()) arg0.setVisible(false);

								if (diskusi == null || diskusi.isEmpty()) {
									MyLabelBoldAja lbl = new MyLabelBoldAja("Tidak Ikut Diskusi");
									lbl.setStyle("font-weight: bolder;color:red;size:9px;");
									lbl.setParent(arg0);
								} else {
									StringBuilder isiBuilder = new StringBuilder();
									List<String> urls = new ArrayList<String>();
									for (Long pertemuanPunyaDiskusiId : diskusi) {
										PertemuanPunyaDiskusi pd = prefetchMap.get(pertemuanPunyaDiskusiId);
										if (pd != null) {
											String waktu = pd.getTanggal_dirubah() == null ? "" : SmartDateTimeUtil.getDayString(pd.getTanggal_dirubah(), null) + Common.dateFormat5.get().format(pd.getTanggal_dirubah());
											String iText = pd.getIsi().replaceAll("\n", "<br>");

											List<String> extUrls = Common.getUrls(iText);
											urls.addAll(extUrls);
											for (String u : extUrls) {
												iText = org.apache.commons.lang3.StringUtils.replace(iText, u, "<a href='" + u + "' target='_blank'>" + u + "</a>");
											}

											String s = iText + " (" + waktu + ")";
											try { s = Jsoup.parse(s.replaceAll("\n", "<br>")).text(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:1216");}
											if(isiBuilder.length() > 0) isiBuilder.append("<br><hr>");
											isiBuilder.append(s);
										}
									}

									String isiStr = isiBuilder.toString();
									if (isiStr.isEmpty()) {
										MyLabelBoldAja lbl = new MyLabelBoldAja("Tidak Ikut Diskusi");
										lbl.setStyle("font-weight: bolder;color:red;size:9px;");
										lbl.setParent(arg0);
									} else {
										if (urls.isEmpty()) {
											new ais.ui.util.MyHtml("<div style='font-size:10px'>" + isiStr + "</div>").setParent(arg0);
										} else {
											Vbox vbox = new Vbox();
											vbox.setParent(arg0);
											new ais.ui.util.MyHtml("<div style='font-size:10px'>" + isiStr + "</div>").setParent(vbox);
											for (String u : urls) {
												Common.displayUrlContent(u, vbox);
											}
										}
									}
								}
							}
						});

						EventListener cariAkun = new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {

								List<Mahasiswa> mahasiswasTemorary = pertemuan.ambilMahasiswa();
								List<Dosen> dsn = pertemuan.ambilDosen();
								List<GeneralValueObject> copyList = new ArrayList<GeneralValueObject>();

								List<GeneralValueObject> generalValueObjects = new ArrayList<GeneralValueObject>(mahasiswasTemorary);
								generalValueObjects.addAll(dsn);

								for (GeneralValueObject vo : generalValueObjects) {

									Mahasiswa voMhs = (vo instanceof Mahasiswa) ? (Mahasiswa) vo : null;
									Dosen voDsn = (vo instanceof Dosen) ? (Dosen) vo : null;
									BiodataCalonMahasiswa voCalMhs = null;
									Siswa voSiswa = (vo instanceof Siswa) ? (Siswa) vo : null;
									CalonSiswa voCalSiswa = null;

									Long id = voCalSiswa != null ? voCalSiswa.getId()
											: voSiswa != null ? voSiswa.getId()
													: voMhs != null ? voMhs.getId()
															: voDsn != null ? voDsn.getId()
																	: voCalMhs != null ? voCalMhs.getId()
																			: tbmuser.getSiswa() != null ? tbmuser.getSiswa().getId()
																					: tbmuser.getCalonSiswa() != null ? tbmuser.getCalonSiswa().getId() : null;

									if (id != null) {
										String cValue = cari.getValue().toLowerCase().trim();
										if (cValue.isEmpty()
												|| (voDsn != null && voDsn.getNama() != null && voDsn.getNama().toLowerCase().contains(cValue))
												|| (voMhs != null && ((voMhs.getNim() != null && voMhs.getNim().toLowerCase().contains(cValue)) || (voMhs.getNama() != null && voMhs.getNama().toLowerCase().contains(cValue))))
												|| (voSiswa != null && ((voSiswa.getNomorIndukNasional() != null && voSiswa.getNomorIndukNasional().toLowerCase().contains(cValue)) || (voSiswa.getNomorInduk() != null && voSiswa.getNomorInduk().toLowerCase().contains(cValue)) || (voSiswa.getNama() != null && voSiswa.getNama().toLowerCase().contains(cValue))))
												|| (voCalMhs != null && ((voCalMhs.getNoRegistrasi() != null && voCalMhs.getNoRegistrasi().toLowerCase().contains(cValue)) || (voCalMhs.getNama() != null && voCalMhs.getNama().toLowerCase().contains(cValue))))
										) {
											copyList.add(vo);
										}
									}
								}
								ListModel strset = new SimpleListModel(copyList);
								gridLocal.setModel(strset);
							}
						};

						cariAkun.onEvent(null);
						cari.addEventListener("onOK", cariAkun);

						Toolbarbutton btnCari = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
						btnCari.setParent(hbox);
						btnCari.addEventListener("onClick", cariAkun);

						cariPeserta.addEventListener("onChange", cariAkun);

						MyToolbarbuttonConfig btnAlpaDiskusi = new MyToolbarbuttonConfig("Tdk.ikut diskusi dianggp.alpa", "/img/Button-Delete-icon.png");
						btnAlpaDiskusi.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
								&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null
								&& pertemuan.getJadwalUjianPMB() == null && pertemuan.getJadwalUjianPSB() == null);
						btnAlpaDiskusi.setTooltiptext("Tutup");
						btnAlpaDiskusi.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								TugasMandiriHelper.tidakIkutDiskusiDiangapTidakHadir(diskusis, pertemuan, new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										new PertemuanHelper(Common.getCurrentUser().getMahasiswa(), null).display(pertemuan, new DataLoader() {
											@Override
											public void loadData(Object value) {}
										}, 0);
									}
								});
							}
						});
						btnAlpaDiskusi.setParent(hbox);

						MyToolbarbuttonConfig btnAlpaAkses = new MyToolbarbuttonConfig("Tdk.akses dianggp.alpa", "/img/Button-Delete-icon.png");
						btnAlpaAkses.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
								&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null
								&& pertemuan.getJadwalUjianPMB() == null && pertemuan.getJadwalUjianPSB() == null);
						btnAlpaAkses.setTooltiptext("Tutup");
						btnAlpaAkses.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								TugasMandiriHelper.tidakAksesDiangapTidakHadir(pertemuan, new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										new PertemuanHelper(Common.getCurrentUser().getMahasiswa(), null).display(pertemuan, new DataLoader() {
											@Override
											public void loadData(Object value) {}
										}, 0);
									}
								});
							}
						});
						btnAlpaAkses.setParent(hbox);

						cariPeserta.setParent(hbox);
					}
				}
			});

			final Tabpanel tabpanelStatistik = new ais.ui.util.MyTabpanel();
			tabpanelStatistik.setParent(tabpanels);
			tabpanelStatistik.setHeight("500px");

			tab3.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelStatistik != null) {
						Common.clear(tabpanelStatistik);
					}

					Borderlayout myborderlayoutProsentase = new Borderlayout();
					myborderlayoutProsentase.setParent(tabpanelStatistik);

					Center centerProsentase = new Center();
					centerProsentase.setParent(myborderlayoutProsentase);
					ais.ui.util.ZkCompat.setFlex(centerProsentase, true);

					Grid gridStats = new Grid();
					gridStats.setSclass("dgrid");
					gridStats.setParent(centerProsentase);

					Rows rowsStats = new Rows();

					rowsStats.setParent(gridStats);

					List<Mahasiswa> mahasiswasTemorary = pertemuan.ambilMahasiswa();
					List<Dosen> dsn = pertemuan.ambilDosen();

					int telahAkses1 = diskusis.size();
					int jumlahTotal = mahasiswasTemorary.size() + dsn.size();
					int belumAkses1 = jumlahTotal - telahAkses1;

					// Grafik komposisi partisipasi diskusi memakai DONUT HTML/CSS modern
					// (ais.ui.util.HtmlChartHelper) — menggantikan pie JFreeChart. Ringan, responsif,
					// dilengkapi penjelasan bahasa sederhana untuk pengguna awam.
					String[] labelDiskusi = new String[] { "Sudah ikut diskusi", "Belum ikut diskusi" };
					double[] nilaiDiskusi = new double[] { telahAkses1, belumAkses1 };
					String[] warnaDiskusi = new String[] { "#1877f2", "#e4e6eb" };
					String htmlDonut = ais.ui.util.HtmlChartHelper.donut(
							"Partisipasi Diskusi Kelas",
							"Menampilkan jumlah peserta yang telah berpartisipasi dalam diskusi dibandingkan dengan yang belum, dari total "
									+ jumlahTotal + " peserta.",
							labelDiskusi, nilaiDiskusi, warnaDiskusi, "ikut diskusi");

					MyFormRow rowSt = new MyFormRow();
					rowSt.setValign("top");
					rowSt.setParent(rowsStats);
					rowSt.appendChild(new ais.ui.util.MyHtml(htmlDonut));
				}
			});

		} else {
			center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
		}

		loadData(null);
	}

	private int height = 100;

	public static void onAddKomentar(PertemuanPunyaDiskusi parent, PertemuanPunyaDiskusi pertemuanPunyaDiskusi,
			final TreeSet<Long> pertemuanPunyaDiskusisa, Mahasiswa mahasiswa, Dosen dosen,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Siswa siswa, CalonSiswa calonSiswa,
			EventListener eventListener) throws Exception {
		MyWindow addWindow = new MyWindow();
		addWindow.setHeight("340px");
		addWindow.setWidth(Common.isMobile() ? "100%" : "500px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
		PertemuanPunyaDiskusiHelper pertemuanPunyaDiskusiHelper = new PertemuanPunyaDiskusiHelper(mahasiswa, dosen, biodataCalonMahasiswa, siswa, calonSiswa);
		pertemuanPunyaDiskusiHelper.pertemuan = pertemuanPunyaDiskusi.getPertemuan();
		pertemuanPunyaDiskusiHelper.eventListener = eventListener;
		pertemuanPunyaDiskusiHelper.pertemuanPunyaDiskusisaStatic = pertemuanPunyaDiskusisa;
		pertemuanPunyaDiskusiHelper.init(pertemuanPunyaDiskusi, parent, addWindow);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onAddKomentar(Event event, PertemuanPunyaDiskusi parent) throws Exception {
		MyWindow addWindow = new MyWindow();
		addWindow.setHeight("340px");
		addWindow.setWidth(Common.isMobile() ? "100%" : "500px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
		init(new PertemuanPunyaDiskusi(), parent, addWindow);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		onAddKomentar(event, null);
	}

	private void init(final PertemuanPunyaDiskusi pertemuanPunyaDiskusi, final PertemuanPunyaDiskusi parent,
			final MyWindow addWindow) throws Exception {
		this.pertemuanPunyaDiskusi = pertemuanPunyaDiskusi;
		lampiran = null;
		if (pertemuanPunyaDiskusi.getId() != null) {
			lampiran = LampiranLain.ambil(pertemuanPunyaDiskusi.getId(), LampiranLain.DISKUSI);
		}
		addWindow.setHeight("450px");
		addWindow.setWidth(Common.isMobile() ? "100%" : "750px");
		addWindow.setTitle("Diskusi dan tanya jawab");
		if (addWindow != null) {
			Common.clear(addWindow);
		}

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100%");

		Rows rows = new Rows();

		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(DashboardTimelinePertemuan.displayInfoPertemuan(pertemuan));

		if (parent != null && parent.getId() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			Tbmuser usrkomentar = parent.getTbmuser();
			Vbox vbox = new Vbox();
			vbox.setHeight("100%");
			vbox.setWidth("100%");
			vbox.setParent(row);
			if (parent.getMahasiswa() != null) CommonMedia.tampilkanGambarKecil(parent.getMahasiswa(), "42px", "right").setParent(vbox);
			else if (parent.getBiodataCalonMahasiswa() != null) CommonMedia.tampilkanGambarKecil(parent.getBiodataCalonMahasiswa(), "42px", "right").setParent(vbox);
			else if (parent.getDosen() != null) CommonMedia.tampilkanGambarKecil(parent.getDosen(), "42px", "right").setParent(vbox);
			else if (usrkomentar != null) CommonMedia.tampilkanGambarKecil(usrkomentar, "42px", "right").setParent(vbox);
			else new Label().setParent(vbox);

			String oleh = parent.getBiodataCalonMahasiswa() != null ? (parent.getBiodataCalonMahasiswa().getNama() + " (Calon Mahasiswa)") : (parent.getMahasiswa() != null ? parent.getMahasiswa().getNama() + " (Mahasiswa)" : "");
			try {
				if (oleh.trim().equals("")) oleh = parent.getDosen() != null ? parent.getDosen().getNama() + " (Dosen)" : "";
				if (oleh.trim().equals("")) oleh = parent.getTbmuser() != null ? parent.getTbmuser().getUserNama() + " (" + parent.getTbmuser().hakAkses().getRoleName() + ")" : "";
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:1493");}

			String waktu = parent.getTanggal_dirubah() == null ? "" : SmartDateTimeUtil.getDayString(parent.getTanggal_dirubah(), null) + Common.dateFormat5.get().format(parent.getTanggal_dirubah());

			String isiParent = parent.getIsi();
			final List<String> urls = Common.getUrls(isiParent);
			for (String u : urls) isiParent = org.apache.commons.lang3.StringUtils.replace(isiParent, u, "<a href='" + u + "' target='_blank'>" + u + "</a>");
			try { isiParent = Jsoup.parse(isiParent.replaceAll("\n", "<br>")).text(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:1500");}

			// Kutipan komentar yang dibalas — gaya gelembung modern yang sama (DiskusiUiHelper).
			new ais.ui.util.MyHtml(ais.ui.util.DiskusiUiHelper.bubbleKomentarHtml(oleh, waktu, isiParent)).setParent(vbox);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(isi = new Textbox());
		isi.setValue(pertemuanPunyaDiskusi.getIsi());
		isi.setWidth("99%");
		isi.setRows(5);
		isi.setStyle("border: 1px solid #9fb8bf;border-radius: 5px;");

		Common.createDefaultTimerNoBusy(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				isi.focus();
				isi.select();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelKecilBold("Untuk memulai diskusi, tulislah komentar, pertanyaan, atau suatu link yang mengarah ke website / file tertentu, bisa juga berupa postingan facebook, twitter, instagram, atau juga link youtube, google drive, dropbox, dll."));

		if (pertemuan.getIzinkanUploadLampiranDiKomentar()) {
			row = new MyFormRow();
			row.setParent(rows);

			Hbox hbox = new Hbox();
			ais.ui.util.MenuAksiBaris.pasang(hbox);
			hbox.setParent(row);
			LampiranLain.createDownloadUploadFileLain(hbox, pertemuanPunyaDiskusi.getId(), LampiranLain.DISKUSI, "Lampiran", false, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (isi.getValue().trim().isEmpty()) isi.setValue("File Terlampir");
					lampiran = (LampiranLain) arg0.getData();
				}
			}, null, false, false, false, true, null, false, true);
		}

		if (pertemuan.getIzinkanUploadLampiranDiGrive()) {
			final MyFormRow rowPreview = new MyFormRow();
			EventListener uploadeventListener = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (isi.getValue().trim().isEmpty()) isi.setValue("Data Terlampir");
					lampiran = (LampiranLain) arg0.getData();
					if (lampiran != null && lampiran.getGdrive() != null) {
						if (rowPreview != null) {
							Common.clear(rowPreview);
						}
						new ais.ui.util.MyHtml("<iframe src=\"https://drive.google.com/file/d/" + lampiran.getGdrive() + "/preview\" " + Common.getStyleContent() + "></iframe>").setParent(rowPreview);
					}
				}
			};

			Toolbarbutton upload = FileFotoLain.tampilkanTombolUploadGdrive(null, null, uploadeventListener, null, null, LampiranLain.DISKUSI, false, null, "Lampiran", null, false, -Common.randLong(), false, LampiranLain.class);
			upload.setImage("/img/Google-Drive-icon_24.png");
			upload.setLabel("Lampiran");
			upload.setOrient("vertical");
			upload.setStyle("font-size:10px");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(upload);

			rowPreview.setValign("top");
			rowPreview.setParent(rows);
			if (lampiran != null && lampiran.getGdrive() != null) {
				new ais.ui.util.MyHtml("<iframe src=\"https://drive.google.com/file/d/" + lampiran.getGdrive() + "/preview\" " + Common.getStyleContent() + "></iframe>").setParent(rowPreview);
			}
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);

		final MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Kirim Diskusi", "/img/Messaging-Child-New-Post-icon.png");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event, parent)) {
					loadData(null);
					addWindow.detach();
					if (eventListener != null) {
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								eventListener.onEvent(new Event("", save, PertemuanPunyaDiskusiHelper.this.pertemuanPunyaDiskusi));
							}
						});
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	public boolean onSave(Event event, PertemuanPunyaDiskusi parent) throws Exception {
		if (isi.getValue().isEmpty()) {
			MyMessageboxConfig.show("Masukkan isi diskusi Anda", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();

			if (pertemuanPunyaDiskusi.getId() != null) {
				pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) session.get(PertemuanPunyaDiskusi.class, pertemuanPunyaDiskusi.getId());
			}

			pertemuanPunyaDiskusi.setParent(parent);
			pertemuanPunyaDiskusi.setIsi(isi.getValue().trim());
			pertemuanPunyaDiskusi.setDosen(dosen);
			pertemuanPunyaDiskusi.setMahasiswa(mahasiswa);
			pertemuanPunyaDiskusi.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && tbmuser.getUserPassword() != null && !tbmuser.getUserPassword().trim().equals("")) {
				pertemuanPunyaDiskusi.setTbmuser(tbmuser);
			}
			pertemuanPunyaDiskusi.setPertemuan(pertemuan);

			session.saveOrUpdate(pertemuanPunyaDiskusi);
			tx.commit();
		} catch(Exception e) {
			if(tx != null) tx.rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:1644");
			throw e;
		} finally {
			closeHibernateSessionQuietly(session);
		}

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Session streamSession = null;
				Transaction stx = null;
				try {
					streamSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
					if (lampiran != null && lampiran.getId() != null) {
						stx = streamSession.beginTransaction();
						lampiran = (LampiranLain) streamSession.get(LampiranLain.class, lampiran.getId());
						if (lampiran != null) {
							lampiran.setRef(pertemuanPunyaDiskusi.getId());
							streamSession.update(lampiran);
						}
						stx.commit();
					}
				} catch (Exception e) {
					if (stx != null) stx.rollback();
					Common.tampilErrorJikaAdmin(e);
				} finally {
					if (streamSession != null && streamSession.isOpen()) try { streamSession.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:1670");}
				}

				if (pertemuanPunyaDiskusisaStatic != null) pertemuanPunyaDiskusisaStatic.add(pertemuanPunyaDiskusi.getId());
				if (pertemuanPunyaDiskusisa != null) pertemuanPunyaDiskusisa.add(pertemuanPunyaDiskusi.getId());

				CommonEmail.infoAdaDiskusiPerkuliahan(pertemuan, pertemuanPunyaDiskusi);
			}
		});

		return true;
	}

	private static void closeHibernateSessionQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			try {
				if (session.isConnected()) {
					session.disconnect();
				}
			} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:1692");
			}
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanPunyaDiskusiHelper.java:1697");
		}
	}

}
