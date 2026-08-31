package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.AbsensiHelper;
import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.TugasKelompokHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.akademik.LaporanMonitorJadwalPelajaran;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.calendar.CalendarUtil;
import ais.common.classroom.ClassRoomUtil;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Helper UI besar untuk menampilkan "Aktifitas Pembelajaran" — tampilan detail satu
 * {@link JadwalPelajaran} (mata pelajaran terjadwal, modul Sekolah) sebagai tab-box multi-tab:
 * Home (pendahuluan, deskripsi pembelajaran, capaian/kompetensi, lampiran pendukung RPS/SAP/absen
 * manual), Agenda (daftar {@link Pertemuan} berpaginasi dengan detail per pertemuan: status,
 * topik, dosen tamu, video conference, tombol absen, komentar/diskusi berulir), Referensi (buku
 * &amp; bahan ajar), Tugas Kelompok, Nilai (delegasi ke {@code DetailPenilaianSiswaHelper}), dan
 * Laporan (monitor perkuliahan). Sebagian besar tab dimuat malas (lazy) saat pertama diklik untuk
 * menghemat resource render awal.
 *
 * <p>
 * Perilaku dan visibilitas kontrol bergantung pada peran pemanggil: bila dikonstruksi dengan
 * {@code siswa}/{@code calonSiswa} non-null (dilihat dari sudut pandang siswa/calon siswa), tombol
 * edit konten (pendahuluan, deskripsi, dsb) dan agenda toolbar disembunyikan — tampilan menjadi
 * read-only. Bila {@code tbmuser} adalah dosen/guru, tombol edit konten dan pengelolaan agenda
 * tampil.
 * </p>
 *
 * <p>
 * <b>Catatan teknis ZK 5.5:</b> event {@code ON_SELECT} pada {@link Tabbox} (dipicu klik tab)
 * TIDAK memicu {@code onClick} pada tab individual — sehingga pemuatan lazy yang dipasang di
 * handler {@code onClick} tiap tab tidak pernah berjalan bila hanya mengandalkan seleksi tab.
 * {@link #initDetail} bekerja di sekitar ini dengan mendengarkan {@code ON_SELECT} pada tabbox dan
 * meneruskan (re-dispatch) sebagai event {@code onClick} sintetis ke tab yang sedang terpilih;
 * setiap handler tab tetap idempoten (memeriksa {@code getChildren().isEmpty()}) agar konten tidak
 * dibangun ulang.
 * </p>
 */
public class AktifitasPembelajaranHelper {

	protected PenjadwalanSiswaHelper penjadwalanHelper = new PenjadwalanSiswaHelper();
	private Siswa siswa;
	private CalonSiswa calonSiswa;
	private Integer mulai;
	public boolean tampikanTab = false;
	private Tabpanel tabpanelAgenda;

	private Tbmuser tbmuser;

	/** Membuat helper dari sudut pandang {@code siswa} atau {@code calonSiswa} (salah satu boleh null); bila keduanya null, dianggap dilihat dari sudut pandang staf/dosen. */
	public AktifitasPembelajaranHelper(Siswa siswa, CalonSiswa calonSiswa) {
		this.siswa = siswa;
		this.calonSiswa = calonSiswa;
		tbmuser = Common.getCurrentUser();
	}

	/** Sama seperti {@link #AktifitasPembelajaranHelper(Siswa, CalonSiswa)}; parameter {@code tampilLangsungRinci} saat ini tidak dipakai (tab Agenda tetap dimuat lazy seperti biasa). */
	public AktifitasPembelajaranHelper(Siswa siswa, CalonSiswa calonSiswa, boolean tampilLangsungRinci) {
		this.siswa = siswa;
		this.calonSiswa = calonSiswa;
		tbmuser = Common.getCurrentUser();
	}

	/** Membangun toolbar aksi agenda (Tambah/Ubah Agenda, buat satu pertemuan, Absensi/UTS/UAS, kalender Google, tombol kelas virtual, refresh); toolbar disembunyikan bila dilihat dari sudut pandang siswa/calon siswa atau tidak ada pengguna login. */
	public Toolbar initAgendaJadwalPelajaran(final JadwalPelajaran jadwalPelajaran, final DataLoader dataLoader) {
		Tbmuser tbmuser = Common.getCurrentUser();
		Toolbar hbox = new Toolbar();
		hbox.setVisible(tbmuser != null && siswa == null && calonSiswa == null);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah/Ubah Agenda", "/img/jadwal.png");
		button.setTooltiptext("Ubah Agenda Pelajaran");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				penjadwalanHelper.display(jadwalPelajaran, dataLoader);
			}

		});

		button.setParent(hbox);

		button = PenjadwalanSiswaHelper.buatSatuPertemuan(jadwalPelajaran, tbmuser, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalPelajaran.belum();
				dataLoader.loadData(null);
			}
		});
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(jadwalPelajaran, true);
			}

		});
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("UTS", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(jadwalPelajaran, "UTS");

			}

		});
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("UAS", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(jadwalPelajaran, "UAS");
			}

		});
		button.setParent(hbox);

		tampilCalender(hbox, dataLoader, jadwalPelajaran);

		ClassRoomUtil.createButton(jadwalPelajaran, dataLoader).setParent(hbox);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(hbox);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalPelajaran.belum();
				dataLoader.loadData(null);
			}
		});

		return hbox;
	}

	/** Menambahkan tombol "Kalender" (hanya tampil bagi staf, bukan siswa/calon siswa) yang mensinkronkan seluruh pertemuan {@code voPembelajaran} ke Google Calendar pengguna lewat {@link CalendarUtil}, menampilkan progres, lalu menyegarkan data. */
	public static void tampilCalender(Component hbox, final DataLoader dataLoader,
			final VOPembelajaran voPembelajaran) {
		final Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Kalender", FileFoto.icon("calendar.google"));
		toolbarbutton.setParent(hbox);
		toolbarbutton.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getCalonSiswa() == null);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				CalendarUtil calendarUtil = new CalendarUtil(tbmuser);

				PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

				final List<com.google.api.services.calendar.model.Event> events = new ArrayList<com.google.api.services.calendar.model.Event>();
				calendarUtil.proses(voPembelajaran.ambilPertemuan(), selectedPerguruanTinggi, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<com.google.api.services.calendar.model.Event> eventsa = (List<com.google.api.services.calendar.model.Event>) arg0
								.getData();
						events.addAll(eventsa);
					}
				});

				CalendarUtil.cretaeTimerWaiting(events, WaktuUtil.getDate(), new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
					}
				});
			}
		});
	}

	/** Membangun tampilan detail jadwal pelajaran dengan {@code DataLoader} default (memanggil {@link #tampilRinci}); lihat {@link #initDetail(JadwalPelajaran, DataLoader, Component, int, int)}. */
	public void initDetail(final JadwalPelajaran jadwalPelajaran, final Component groupbox, int mulai, int banyak)
			throws Exception {
		initDetail(jadwalPelajaran, null, groupbox, mulai, banyak);
	}

	/** Membangun tab "Home": panel pendahuluan, deskripsi pembelajaran, dan capaian/kompetensi (masing-masing mode tampil/edit dapat ditukar lewat tombol Ubah/Simpan, hanya tampil bagi staf), diikuti lampiran pendukung ({@link #tampilkanLampiran}) dan catatan arahan ke tab Agenda. */
	private void displayHeader(final JadwalPelajaran jadwalPelajaran, Component header) {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(header);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");
		grid.setStyle("min-height: 400px;");
		grid.setSclass("fgrid");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setParent(columns);
		column.setWidth("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		if (jadwalPelajaran != null) {

			final MyFormRow row = new MyFormRow();row.setValign("top");
			final MyFormRow rowEdit = new MyFormRow();
			final MyCkEditor pendahuluan = new MyCkEditor();
			final Html labelPendahuluan = new ais.ui.util.MyHtml(jadwalPelajaran.getPendahuluan());

			rowEdit.setParent(rows);
			rowEdit.setVisible(false);

			MyGroupboxStyled vbox1 = new MyGroupboxStyled();
			rowEdit.appendChild(vbox1);
			Hbox hbox = new Hbox();
			vbox1.appendChild(new MyCaptionStyled("Pendahuluan"));
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			button.setTooltiptext("Simpan Data");
			button.setVisible(siswa == null && calonSiswa == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					row.setVisible(true);
					rowEdit.setVisible(false);

					jadwalPelajaran.setPendahuluan(pendahuluan.getValue());
					Common.refreshUpdate(jadwalPelajaran);
					labelPendahuluan.setContent(jadwalPelajaran.getPendahuluan());
				}

			});
			button.setParent(hbox);
			hbox.setParent(vbox1);

			vbox1.appendChild(pendahuluan);
			pendahuluan.setValue(jadwalPelajaran.getPendahuluan());
			pendahuluan.setHeight("200px");
			pendahuluan.setWidth("850px");

			row.setParent(rows);

			vbox1 = new MyGroupboxStyled();
			row.appendChild(vbox1);
			hbox = new Hbox();
			vbox1.appendChild(new MyCaptionStyled("Pendahuluan"));
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
			button = new MyToolbarbuttonConfig("Ubah", "/img/edit-icon.png");
			button.setTooltiptext("Ubah Data");
			button.setVisible(siswa == null && calonSiswa == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					row.setVisible(false);
					rowEdit.setVisible(true);
				}

			});
			button.setParent(hbox);
			hbox.setParent(vbox1);

			vbox1.appendChild(labelPendahuluan);
		}

		if (jadwalPelajaran != null) {

			final MyFormRow row = new MyFormRow();row.setValign("top");
			final MyFormRow rowEdit = new MyFormRow();
			final Textbox pendahuluan = new Textbox();
			final Html labelPendahuluan = new ais.ui.util.MyHtml(
					jadwalPelajaran.getDeskripsiPembelajaran().replaceAll("\n", "<br>"));

			rowEdit.setParent(rows);
			rowEdit.setVisible(false);

			MyGroupboxStyled vbox1 = new MyGroupboxStyled();
			rowEdit.appendChild(vbox1);
			Hbox hbox = new Hbox();
			vbox1.appendChild(new MyCaptionStyled("Deskripsi Pembelajaran"));
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			button.setTooltiptext("Simpan Data");
			button.setVisible(siswa == null && calonSiswa == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					row.setVisible(true);
					rowEdit.setVisible(false);

					jadwalPelajaran.setDeskripsiPembelajaran(pendahuluan.getValue());
					Common.refreshUpdate(jadwalPelajaran);
					labelPendahuluan.setContent(jadwalPelajaran.getDeskripsiPembelajaran().replaceAll("\n", "<br>"));
				}

			});
			button.setParent(hbox);
			hbox.setParent(vbox1);

			vbox1.appendChild(pendahuluan);
			pendahuluan.setValue(jadwalPelajaran.getDeskripsiPembelajaran());
			pendahuluan.setRows(5);
			pendahuluan.setWidth("850px");

			row.setParent(rows);

			vbox1 = new MyGroupboxStyled();
			row.appendChild(vbox1);
			hbox = new Hbox();
			vbox1.appendChild(new MyCaptionStyled("Deskripsi Pembelajaran"));
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
			button = new MyToolbarbuttonConfig("Ubah", "/img/edit-icon.png");
			button.setTooltiptext("Ubah Data");
			button.setVisible(siswa == null && calonSiswa == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					row.setVisible(false);
					rowEdit.setVisible(true);
				}

			});
			button.setParent(hbox);
			hbox.setParent(vbox1);

			vbox1.appendChild(labelPendahuluan);

		}

		if (jadwalPelajaran != null) {

			final MyFormRow row = new MyFormRow();row.setValign("top");
			final MyFormRow rowEdit = new MyFormRow();
			final Textbox pendahuluan = new Textbox();
			final Html labelPendahuluan = new ais.ui.util.MyHtml(
					jadwalPelajaran.getCapaianPembelajaranProdi().replaceAll("\n", "<br>"));

			rowEdit.setParent(rows);
			rowEdit.setVisible(false);

			MyGroupboxStyled vbox1 = new MyGroupboxStyled();
			rowEdit.appendChild(vbox1);
			Hbox hbox = new Hbox();
			vbox1.appendChild(new MyCaptionStyled("Capaian / Kompetensi"));
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			button.setTooltiptext("Simpan Data");
			button.setVisible(siswa == null && calonSiswa == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					row.setVisible(true);
					rowEdit.setVisible(false);

					jadwalPelajaran.setCapaianPembelajaranProdi(pendahuluan.getValue());
					Common.refreshUpdate(jadwalPelajaran);
					labelPendahuluan.setContent(jadwalPelajaran.getCapaianPembelajaranProdi().replaceAll("\n", "<br>"));
				}

			});
			button.setParent(hbox);
			hbox.setParent(vbox1);

			vbox1.appendChild(pendahuluan);
			pendahuluan.setValue(jadwalPelajaran.getCapaianPembelajaranProdi());
			pendahuluan.setRows(5);
			pendahuluan.setWidth("850px");

			row.setParent(rows);

			vbox1 = new MyGroupboxStyled();
			row.appendChild(vbox1);
			hbox = new Hbox();
			vbox1.appendChild(new MyCaptionStyled("Capaian / Kompetensi"));
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
			button = new MyToolbarbuttonConfig("Ubah", "/img/edit-icon.png");
			button.setTooltiptext("Ubah Data");
			button.setVisible(siswa == null && calonSiswa == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					row.setVisible(false);
					rowEdit.setVisible(true);
				}

			});
			button.setParent(hbox);
			hbox.setParent(vbox1);

			vbox1.appendChild(labelPendahuluan);

		}

		if (jadwalPelajaran != null) {

			AktifitasPembelajaranHelper.tampilkanLampiran(rows, jadwalPelajaran.getId(),
					jadwalPelajaran.getMatapelajaran().getId(), "", "_matapelajaran",
					siswa == null && calonSiswa == null, "");
		}

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		MyLabelAgakKecilBold label = new MyLabelAgakKecilBold(
				"*)  Untuk melihat semua Agenda Pertemuan, klik menu \"Agenda\" di atas.");
		label.setStyle("font-size:9px;font-weight: bolder;color:red;");
		row.appendChild(label);
	}

	/**
	 * Menambahkan baris unggah/unduh lampiran pendukung mata pelajaran ke {@code rows}: RPS
	 * (bila konfigurasi {@code tampilkan_rps} aktif), SAP (bila {@code tampilkan_sap} aktif),
	 * Absen Manual (bila {@code tampilkan_absen_manual} aktif), dan daftar lampiran kustom
	 * tambahan dari konfigurasi {@code tampilkan_lampiran_lain_di_agenda_pelajaran} (dipisah
	 * koma). Untuk setiap jenis lampiran, {@link AktifitasPerkuliahanHelper#chekSimpan} lebih dulu
	 * memastikan/menyalin data lampiran dari referensi sumber ({@code refAmbilDari}) bila belum
	 * ada pada referensi target ({@code ref}) — memungkinkan lampiran diwarisi dari mata kuliah ke
	 * jadwal pelajaran spesifik. Setiap unggahan baru langsung dikaitkan ke {@code ref} lewat sesi
	 * Hibernate streaming terpisah.
	 *
	 * @param rows              grid baris tempat komponen ditambahkan
	 * @param ref                id referensi target (biasanya id jadwal pelajaran)
	 * @param refAmbilDari       id referensi sumber warisan lampiran (biasanya id mata pelajaran)
	 * @param tambahan           akhiran kunci lampiran pada referensi target
	 * @param tambahanAmbilDari  akhiran kunci lampiran pada referensi sumber
	 * @param bolehUpload        izinkan pengguna mengunggah/mengubah lampiran
	 * @param span               nilai colspan opsional untuk baris (kosong berarti default)
	 */
	@SuppressWarnings("deprecation")
	public static void tampilkanLampiran(Rows rows, final Long ref, final Long refAmbilDari, final String tambahan,
			final String tambahanAmbilDari, final boolean bolehUpload, final String span) {
		MyFormRow row = new MyFormRow();row.setValign("top");
		if (span != null && !span.isEmpty()) {
			ais.ui.util.ZkCompat.setSpans(row, span);
		}
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelBold("Lampiran Pendukung"));
		if (Common.bolehKonfigurasi("tampilkan_rps")) {
			AktifitasPerkuliahanHelper.chekSimpan(ref, refAmbilDari, tambahan, tambahanAmbilDari, LampiranLain.SILABUS);
			row = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(row, span);
			}
			row.setValign("top");
			row.setParent(rows);

			MyFormRow rowPreview = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(rowPreview, span);
			}
			rowPreview.setValign("top");
			rowPreview.setParent(rows);

			Hbox hbox1 = new Hbox();
			hbox1.setParent(row);
			LampiranLain.createDownloadUploadFileLain(hbox1, ref, LampiranLain.SILABUS + tambahan, "RPS", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain ttd = (LampiranLain) arg0.getData();
							try {
								Session session = StreamingHibernateUtil.getInstance().currentSession();

								session.refresh(ttd);
								ttd.setRef(ref);

								session.getTransaction().begin();
								session.update(ttd);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}

						}
					}, null, false, false, false, bolehUpload, null, false, false, rowPreview);
		}

		if (Common.bolehKonfigurasi("tampilkan_sap")) {
			AktifitasPerkuliahanHelper.chekSimpan(ref, refAmbilDari, tambahan, tambahanAmbilDari, LampiranLain.SAP);
			row = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(row, span);
			}
			row.setValign("top");
			row.setParent(rows);

			MyFormRow rowPreview = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(rowPreview, span);
			}
			rowPreview.setValign("top");
			rowPreview.setParent(rows);

			Hbox hbox1 = new Hbox();
			hbox1.setParent(row);
			LampiranLain.createDownloadUploadFileLain(hbox1, ref, LampiranLain.SAP + tambahan, "SAP", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain ttd = (LampiranLain) arg0.getData();
							try {
								Session session = StreamingHibernateUtil.getInstance().currentSession();

								session.refresh(ttd);
								ttd.setRef(ref);

								session.getTransaction().begin();
								session.update(ttd);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
						}
					}, null, false, false, false, bolehUpload, null, false, false, rowPreview);

		}

		if (Common.bolehKonfigurasi("tampilkan_absen_manual")) {
			AktifitasPerkuliahanHelper.chekSimpan(ref, refAmbilDari, tambahan, tambahanAmbilDari, "Absen Manual");
			row = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(row, span);
			}
			row.setValign("top");
			row.setParent(rows);

			MyFormRow rowPreview = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(rowPreview, span);
			}
			rowPreview.setValign("top");
			rowPreview.setParent(rows);

			Hbox hbox1 = new Hbox();
			hbox1.setParent(row);
			LampiranLain.createDownloadUploadFileLain(hbox1, ref, "Absen Manual" + tambahan, "Absen Manual", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain ttd = (LampiranLain) arg0.getData();
							try {
								Session session = StreamingHibernateUtil.getInstance().currentSession();

								session.refresh(ttd);
								ttd.setRef(ref);

								session.getTransaction().begin();
								session.update(ttd);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
						}
					}, null, false, false, false, bolehUpload, null, false, false, rowPreview);
		}

		String tampilkan_lampiran_lain_di_agenda = Common
				.getKonfigurasi("tampilkan_lampiran_lain_di_agenda_pelajaran", "").getNilai();
		if (tampilkan_lampiran_lain_di_agenda != null && !tampilkan_lampiran_lain_di_agenda.trim().isEmpty()) {
			for (String s : tampilkan_lampiran_lain_di_agenda.split(",")) {

				AktifitasPerkuliahanHelper.chekSimpan(ref, refAmbilDari, tambahan, tambahanAmbilDari, s);

				row = new MyFormRow();
				if (span != null && !span.isEmpty()) {
					ais.ui.util.ZkCompat.setSpans(row, span);
				}
				row.setValign("top");
				row.setParent(rows);

				MyFormRow rowPreview = new MyFormRow();
				if (span != null && !span.isEmpty()) {
					ais.ui.util.ZkCompat.setSpans(rowPreview, span);
				}
				rowPreview.setValign("top");
				rowPreview.setParent(rows);

				Hbox hbox1 = new Hbox();
				hbox1.setParent(row);
				LampiranLain.createDownloadUploadFileLain(hbox1, ref, s + tambahan, s, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain ttd = (LampiranLain) arg0.getData();
						try {
							Session session = StreamingHibernateUtil.getInstance().currentSession();

							session.refresh(ttd);
							ttd.setRef(ref);

							session.getTransaction().begin();
							session.update(ttd);
							session.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}, null, false, false, false, bolehUpload, null, false, false, rowPreview);
			}
		}
	}

	@SuppressWarnings({})
	public void initDetail(final JadwalPelajaran jadwalPelajaran, final DataLoader mydataLoader,
			final Component groupbox, final int mulai, final int banyak) throws Exception {
		this.mulai = mulai;
		tabpanelAgenda = new ais.ui.util.MyTabpanel();
		final DataLoader dataLoader = mydataLoader == null ? new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					tampilRinci(jadwalPelajaran, this, tabpanelAgenda, groupbox, mulai, banyak, false);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AktifitasPembelajaranHelper.java:664");
				}
			}
		} : mydataLoader;

		Common.clear(groupbox);

		final Tabbox tabbox = new Tabbox();
		tabbox.setSclass("ais-aktifitas-tabbox");
		tabbox.setParent(groupbox);
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");

		// FIX konten tab kosong (ZK 5.5): meng-klik sebuah tab memicu event ON_SELECT pada Tabbox,
		// BUKAN onClick pada tiap Tab — sehingga pemuatan lazy yang dipasang di tab.onClick (Agenda,
		// Ref., Tgs.Kel., Nilai, Laporan) TIDAK pernah jalan → tab tampak kosong (hanya "Home" yang
		// tampil karena dimuat eager). Di sini ON_SELECT Tabbox di-re-dispatch menjadi onClick ke tab
		// yang SEDANG dipilih, sehingga konten lazy-nya termuat. Idempoten: tiap handler tab menjaga
		// getChildren().isEmpty() sehingga tidak membangun ulang isinya.
		tabbox.addEventListener(org.zkoss.zk.ui.event.Events.ON_SELECT, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				org.zkoss.zul.Tab dipilih = tabbox.getSelectedTab();
				if (dipilih != null) {
					org.zkoss.zk.ui.event.Events.sendEvent(new Event("onClick", dipilih));
				}
			}
		});

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabPendahuluan = new MyTabConfig("Home", "/img/home-icon.png");
		tabPendahuluan.setParent(tabs);
		tabPendahuluan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tampikanTab = false;
			}
		});

		MyTabConfig tab = new MyTabConfig("Agenda", "/img/jadwal.png");
		tab.setParent(tabs);

		MyTabConfig tabReferensi = new MyTabConfig("Ref.", "/img/Blue-Books-icon.png");
		tabReferensi.setParent(tabs);

		MyTabConfig tabTugasKelompok = new MyTabConfig("Tgs.Kel.", "/img/Document-scheduled-tasks-icon.png");
		tabTugasKelompok.setParent(tabs);

		MyTabConfig tabPenilaian = new MyTabConfig("Nilai", "/img/svg/check2.svg");
		tabPenilaian.setParent(tabs);

		MyTabConfig tabLaporan = new MyTabConfig("Laporan", "/img/print.png");
		tabLaporan.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelPendahuluan = new ais.ui.util.MyTabpanel();
		tabpanelPendahuluan.setParent(tabpanels);
		tabpanelPendahuluan.setHeight("" + (banyak * 12000) + "px");
		displayHeader(jadwalPelajaran, tabpanelPendahuluan);

		tabpanelAgenda.setParent(tabpanels);
		tabpanelAgenda.setStyle("height: " + (banyak * 12000) + "px;");
		tab.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tampikanTab = true;
				if (tabpanelAgenda.getChildren().size() == 0) {
					tampilRinci(jadwalPelajaran, dataLoader, tabpanelAgenda, groupbox, mulai, banyak, true);
				}
			}
		});

		if (tampikanTab) {
			if (tabpanelAgenda.getChildren().size() == 0) {
				tampilRinci(jadwalPelajaran, dataLoader, tabpanelAgenda, groupbox, mulai, banyak);
				tab.setSelected(true);
			}
		}

		final Tabpanel tabpanelReferensi = new ais.ui.util.MyTabpanel();
		tabpanelReferensi.setHeight("1250px");
		tabpanelReferensi.setParent(tabpanels);
		tabReferensi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelReferensi.getChildren().size() == 0) {

					final Tabbox tabbox = new Tabbox();
					tabbox.setSclass("ais-aktifitas-tabbox");
					tabbox.setParent(tabpanelReferensi);
					tabbox.setWidth("100%");
					tabbox.setHeight("100%");

					Tabs tabs = new Tabs();
					tabs.setParent(tabbox);

					final MyTabConfig tabReferensi = new MyTabConfig("Buku");
					tabReferensi.setParent(tabs);

					final MyTabConfig tabBukuAjar = new MyTabConfig("Bahan Ajar");
					tabBukuAjar.setParent(tabs);

					Tabpanels tabpanels = new Tabpanels();
					tabpanels.setParent(tabbox);

					Tabpanel tabpanelReferensi = new ais.ui.util.MyTabpanel();
					tabpanelReferensi.setHeight("1250px");
					tabpanelReferensi.setParent(tabpanels);
					JadwalPelajaranPunyaItemHelper jadwalPelajaranPunyaItemHelper = new JadwalPelajaranPunyaItemHelper();
					jadwalPelajaranPunyaItemHelper.display(jadwalPelajaran, tabpanelReferensi);

					final Tabpanel tabpanelBukuAjar = new ais.ui.util.MyTabpanel();
					tabBukuAjar.setLabel("Buku Diktat / Ajar ");

					tabpanelBukuAjar.setParent(tabpanels);
					tabBukuAjar.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelBukuAjar.getChildren().size() == 0) {
								tabpanelBukuAjar.setHeight("1250px");
								BukuBahanAjarMatapelajaranHelper bukuBahanAjarHelper = new BukuBahanAjarMatapelajaranHelper();
								bukuBahanAjarHelper.display(jadwalPelajaran.getMatapelajaran(), tabpanelBukuAjar,
										jadwalPelajaran);
							}
						}
					});

				}
			}
		});

		final Tabpanel tabpanelTugasKelompok = new ais.ui.util.MyTabpanel();
		tabpanelTugasKelompok.setParent(tabpanels);
		tabpanelTugasKelompok.setHeight(Common.isMobile() ? "" + (banyak * 12000) + "px" : "22050px");
		tabTugasKelompok.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelTugasKelompok.getChildren().size() == 0) {

					TugasKelompokHelper tugasKelompokHelper = new TugasKelompokHelper(siswa, calonSiswa);
					tugasKelompokHelper.display(null, null, null, jadwalPelajaran, tabpanelTugasKelompok);
				}
			}
		});

		final Tabpanel tabpanelPenilaian = new ais.ui.util.MyTabpanel();
		tabpanelPenilaian.setParent(tabpanels);
		tabpanelPenilaian.setHeight("18650px");
		tabPenilaian.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPenilaian.getChildren().size() == 0) {

					Common.createDefaultTimer(new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {

							if (tbmuser != null && tbmuser.getSiswa() == null) {
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(KelasSiswaPunyaSiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("kelasSiswa", jadwalPelajaran.getKelas()))
										.createAlias("siswa", "siswa").addOrder(Order.asc("nomorUrut"))
										.addOrder(Order.asc("siswa.namaSiswa")).addOrder(Order.desc("siswa.id"));
								DetailPenilaianSiswaHelper.displayPenilaian(jadwalPelajaran,
										jadwalPelajaran.getKurikulumPunyaMatapelajaran(), tabpanelPenilaian,
										jadwalPelajaran.getKelas(),
										ConstantValues.simpleList(criteria, KelasSiswaPunyaSiswa.class));
							} else if (tbmuser != null && tbmuser.getSiswa() != null) {
								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(KelasSiswaPunyaSiswa.class)
										.add(Restrictions.eq("siswa", tbmuser.getSiswa())).createAlias("siswa", "siswa")
										.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.namaSiswa"))
										.addOrder(Order.desc("siswa.id"))
										.add(Restrictions.eq("kelasSiswa", jadwalPelajaran.getKelas()));
								DetailPenilaianSiswaHelper.displayPenilaian(jadwalPelajaran,
										jadwalPelajaran.getKurikulumPunyaMatapelajaran(), tabpanelPenilaian,
										jadwalPelajaran.getKelas(),
										ConstantValues.simpleList(criteria, KelasSiswaPunyaSiswa.class));
							}
						}
					});
				}
			}
		});

		final Tabpanel tabpanelLaporan = new ais.ui.util.MyTabpanel();
		tabpanelLaporan.setHeight("1250px");
		tabpanelLaporan.setParent(tabpanels);
		tabLaporan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelLaporan.getChildren().size() == 0) {
					LaporanMonitorJadwalPelajaran laporanMonitorPerkuliahan = new LaporanMonitorJadwalPelajaran(
							jadwalPelajaran);
					laporanMonitorPerkuliahan.setBorder("none");
					laporanMonitorPerkuliahan.setHeight("1250px");
					laporanMonitorPerkuliahan.setWidth("100%");
					tabpanelLaporan.appendChild(laporanMonitorPerkuliahan);
				}
			}
		});

	}

	@SuppressWarnings({})
	private void tampilRinci(final JadwalPelajaran jadwalPelajaran, final DataLoader dataLoader,
			final Tabpanel tabpanel, final Component groupbox, final int mulai, final int banyak) throws Exception {
		tampilRinci(jadwalPelajaran, dataLoader, tabpanel, groupbox, mulai, banyak, false);
	}

	@SuppressWarnings({ "unchecked" })
	private void tampilRinci(final JadwalPelajaran jadwalPelajaran, final DataLoader dataLoader,
			final Tabpanel tabpanel, final Component groupbox, final int m, final int banyak, boolean tampilHal)
			throws Exception {

		final List<Long> pertemuans;
		Integer jumlahParentNull = 0;

		if (jadwalPelajaran.udah()) {
			Object[] a = jadwalPelajaran.ambilPertemuan(m, banyak, tampilHal);
			pertemuans = (List<Long>) a[0];
			jumlahParentNull = (Integer) a[1];
			mulai = (Integer) a[2];
		} else {
			Session session = HibernateUtil.currentSession();
			List<Pertemuan> pertemuansTemp = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(!jadwalPelajaran.getUrutkanotomatis() ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
					.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
					.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).list();
			jadwalPelajaran.reInitPertemuan(pertemuansTemp, session);
			pertemuansTemp.clear();
			pertemuansTemp = null;

			Object[] a = jadwalPelajaran.ambilPertemuan(m, banyak, tampilHal);
			pertemuans = (List<Long>) a[0];
			jumlahParentNull = (Integer) a[1];
			mulai = (Integer) a[2];
		}

		Common.clear(tabpanel);

		final ais.ui.util.MyDiv myGroupbox = new ais.ui.util.MyDiv();
		myGroupbox.setStyle("height: " + (banyak * 12000) + "px;");
		myGroupbox.setParent(tabpanel);
		myGroupbox.appendChild(initAgendaJadwalPelajaran(jadwalPelajaran, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					tampilRinci(jadwalPelajaran, dataLoader, tabpanel, myGroupbox, mulai, banyak);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		}));

		if (jumlahParentNull > banyak) {
			final Paging paging = new Paging();
			paging.setDetailed(!Common.isMobile());
			try {
				paging.setPageSize(banyak);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AktifitasPembelajaranHelper.java:941");
			}
			paging.setMold("os");
			paging.setTotalSize(jumlahParentNull);
			// PERBAIKAN: mulai/banyak bisa merujuk halaman yang sudah tak ada lagi (mis.
			// offset lama tersimpan di komponen induk, sementara jumlah data berkurang
			// akibat filter/hapus di antara render) -> WrongValueException "since only N
			// pages". Clamp ke halaman terakhir yang valid alih-alih meledak.
			int totalHalaman = (jumlahParentNull + banyak - 1) / banyak;
			int halamanAktif = Math.max(0, Math.min(mulai / banyak, totalHalaman - 1));
			try {
				paging.setActivePage(halamanAktif);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AktifitasPembelajaranHelper.java:setActivePage");
			}
			paging.addEventListener("onPaging", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tampilRinci(jadwalPelajaran, dataLoader, tabpanel, myGroupbox, banyak * paging.getActivePage(),
							banyak, false);

				}
			});

			if (banyak == 1) {
				myGroupbox.appendChild(new MyLabelBoldConfig("Pilih pertemuan ke : "));
			}
			paging.setParent(myGroupbox);
		}

		if (tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getCalonSiswa() == null && pertemuans.isEmpty()) {
			Html html = new ais.ui.util.MyHtml(
					"<strong><font style='color:red'>Agenda Pelajaran belum dibuat</font></strong><br><br><br>");
			html.setHeight("150px");
			html.setWidth("100%");
			html.setParent(myGroupbox);

			if (tbmuser.ambilGuru() == null || jadwalPelajaran.getGuruBisaMerubahTanggalJadwalPelajaran()) {

				myGroupbox.appendChild(new Space());
				myGroupbox.appendChild(new Space());
				myGroupbox.appendChild(new Space());
				myGroupbox.appendChild(new Space());
				myGroupbox.appendChild(new Space());

				Hbox hbox = new Hbox();
				hbox.setParent(myGroupbox);

				PenjadwalanSiswaHelper.tampilTombolBuatPertemuan(hbox, jadwalPelajaran, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						jadwalPelajaran.belum();
						dataLoader.loadData(null);
					}
				});

				PenjadwalanSiswaHelper.tampilTombolAmbil(hbox, jadwalPelajaran, new DataLoader() {

					@Override
					public void loadData(Object value) {
						jadwalPelajaran.belum();
						dataLoader.loadData(null);
					}
				});
			}

		} else {
			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setSclass("fgrid");
			grid.setWidth("100%");
			grid.setParent(myGroupbox);
			grid.setWidth("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
			Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
			calendar1.set(Calendar.DATE, calendar1.get(Calendar.DATE) + 6);

			boolean urut = false;
			try {
				String pil = tbmuser.retreive("urutkan_diskusi_berdasarkan_terlama");
				urut = (pil == null || pil.trim().isEmpty() ? false : Boolean.parseBoolean(pil));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AktifitasPembelajaranHelper.java:1020");
				// TODO: handle exception
			}
			boolean mobile = Common.isMobile();
			for (Long pertemuanid : pertemuans) {
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null) {
					MyFormRow rowUtama = new MyFormRow();
					rowUtama.setParent(rows);
					rowUtama.setValign("top");

					String tgl = pertemuan.getJadwalPelajaran() == null || pertemuan.getTanggal() == null ? "-"
							: ("Rencana : " + Common.dateFormat4.get().format(pertemuan.getTanggal()) + " "
									+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
											: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai()));

					Groupbox pertemuanBox = new ais.ui.util.MyGroupboxStyled();
					pertemuanBox.setWidth(mobile ? "93%" : "95%");
					rowUtama.appendChild(pertemuanBox);
					MyCaptionStyled c;
					pertemuanBox.appendChild(
							c = new MyCaptionStyled("Pertemuan ke-" + pertemuan.getPertemuanKe() + ", " + tgl));
					c.setStyle("font-size:12px;font-weight: bolder;text-decoration: none;color:"
							+ pertemuan.warna().split(",")[0] + ";border: 1px solid " + pertemuan.warna().split(",")[0]
							+ ";\r\n" + "  padding: 5px;" + "  background-color: rgba(169,169,169,0.4);"
							+ "  border-radius: 5px 15px;");

					Vbox a = RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan,
							pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama());

					a.appendChild(new Label(pertemuan.getTanggalRealisasi() == null ? ""
							: "Realisasi : " + Common.dateFormat4.get().format(pertemuan.getTanggalRealisasi())));

					final Vbox vbox = new Vbox();
					vbox.setParent(pertemuanBox);

					a.setParent(vbox);

					new MyLabelAgakKecilBold(pertemuan.getTopik()).setParent(vbox);
//					new MyLabelAgakKecilBold(pertemuan.getMetodePembelajaran()).setParent(vbox);
//					new MyLabelAgakKecilBold(pertemuan.getBukuRujukan1()).setParent(vbox);
//					new MyLabelAgakKecilBold(pertemuan.getBukuRujukan2()).setParent(vbox);
					new MyLabelAgakKecilBold(pertemuan.getDosenTamu()).setParent(vbox);
					new MyLabelAgakKecilBold(pertemuan.getDosenTamu2()).setParent(vbox);

					DashboardTimelinePertemuan.displayCatatan(vbox, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							try {
								tampilRinci(jadwalPelajaran, dataLoader, tabpanel, groupbox, mulai, banyak, false);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AktifitasPembelajaranHelper.java:1072");
							}
						}
					}, pertemuan, tbmuser, mobile);

					Component aa = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, null, false,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									try {
										tampilRinci(jadwalPelajaran, dataLoader, tabpanel, groupbox, mulai, banyak,
												false);
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AktifitasPembelajaranHelper.java:1086");
									}
								}
							});

					Component bb = AbsensiSiswaHelper.createTombolAbsen(pertemuan, new DataLoader() {

						@Override
						public void loadData(Object value) {
							try {
								tampilRinci(jadwalPelajaran, this, tabpanel, groupbox, mulai, banyak, false);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AktifitasPembelajaranHelper.java:1098");
							}
						}
					});

					AktifitasPerkuliahanHelper.createKeterangan(pertemuan, new DataLoader() {

						@Override
						public void loadData(Object value) {
							try {
								tampilRinci(jadwalPelajaran, this, tabpanel, groupbox, mulai, banyak, false);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AktifitasPembelajaranHelper.java:1110");
							}
						}
					}, aa, bb).setParent(pertemuanBox);

					AbsensiHelper.createStatusKehadiran(jadwalPelajaran.populateDosen().values(), pertemuan)
							.setParent(pertemuanBox);

					DashboardTimelinePertemuan.tampilOnline(pertemuan, pertemuanBox, tbmuser, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							try {
								tampilRinci(jadwalPelajaran, dataLoader, tabpanel, groupbox, mulai, banyak, false);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/AktifitasPembelajaranHelper.java:1125");
							}
						}
					});

					if (Common.bolehKonfigurasi("tampilkan_komentar_di_aktifitas_jadwalPelajaran")) {
						if (!pertemuan.udah()) {
							Session session = HibernateUtil.currentSession();
							pertemuan.reInitPertemuanPunyaDiskusi(session);
						}

						Vbox vbox2 = new Vbox();
						vbox2.setParent(pertemuanBox);

						TreeSet<Long> pertemuanPunyaDiskusisa = pertemuan.ambilPertemuanPunyaDiskusiTotal(urut);
						DashboardTimelinePertemuan.loadKomentarDetail(null, "42px", pertemuanPunyaDiskusisa, pertemuan,
								vbox2, "background-color: rgba(255,255,255,0.5);", 0, 50, false, null);
					}

				}
			}
		}

	}

}
