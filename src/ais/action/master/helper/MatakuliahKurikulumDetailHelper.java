package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panelchildren;
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
import org.zkoss.zul.Window;

import ais.action.master.RencanaTahunAkademikAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.KurikulumPunyaMatakuliahDetailDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.DataPunyaArtikel;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.KurikulumPunyaMatakuliahPunyaItem;
import ais.database.model.MatakuliahPunyaBukuBahanAjar;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class MatakuliahKurikulumDetailHelper implements DataLoader {

	private MyGrid grid;
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah;
	private List<KurikulumPunyaMatakuliahDetail> kurikulumPunyaMatakuliahDetails = null;

	private VideoPertemuanHelper videoPertemuanHelper;
	private AudioPertemuanHelper audioPertemuanHelper;
	private FilePerkuliahanHelper filePerkuliahanHelper;

	private Boolean add = false;
	private Boolean delete = false;

	private Tbmuser tbmuser = Common.getCurrentUser();
	private North north;
	private Perkuliahan perkuliahan;
	public MyDatebox tanggalMulaiPerkuliahan;
	private MyCheckboxConfig lewatiTanggalMerahNasional = null;

	public MatakuliahKurikulumDetailHelper() {
		filePerkuliahanHelper = new FilePerkuliahanHelper(null, null);
		videoPertemuanHelper = new VideoPertemuanHelper(true, false);
		audioPertemuanHelper = new AudioPertemuanHelper(true, false);
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public void setHariMulai(String hari) {
		if (tanggalMulaiPerkuliahan != null && hari != null) {
			RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction
					.getCurrentRencanaTahunAkademik(ais.ui.util.WaktuUtil.getDate());
			if (rencanaTahunAkademik != null && rencanaTahunAkademik.getTanggalMulai() != null) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(rencanaTahunAkademik.getTanggalMulai());
				while (true) {

					int dayOfweek = calendar.get(Calendar.DAY_OF_WEEK);
					if (hari.equals(Common.haris[dayOfweek - 1])) {
						tanggalMulaiPerkuliahan.setValue(calendar.getTime());
						break;
					}

					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
				}
			}
		}
	}

	public void simpan(Perkuliahan perkuliahan) {
		Date tgl = tanggalMulaiPerkuliahan == null ? null : tanggalMulaiPerkuliahan.getValue();
		MatakuliahKurikulumDetailHelper.simpan(perkuliahan, kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahDetails,
				tgl, lewatiTanggalMerahNasional == null ? true : lewatiTanggalMerahNasional.isChecked());
	}

	public static void simpan(Perkuliahan perkuliahan, KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			List<KurikulumPunyaMatakuliahDetail> kurikulumPunyaMatakuliahDetails, Date tgl,
			boolean lewatiTanggalMerahNasional) {
		if (kurikulumPunyaMatakuliahDetails != null && !kurikulumPunyaMatakuliahDetails.isEmpty()) {

			if (tgl != null && perkuliahan != null && perkuliahan.getId() != null) {

				copyLampiran(kurikulumPunyaMatakuliah, perkuliahan);

				perkuliahan.setLewatiTanggalMerahNasional(lewatiTanggalMerahNasional);
				perkuliahan.setTanggalMulaiPerkuliahan(tgl);
				Common.refreshUpdate(perkuliahan);

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(tgl);
				Session session = HibernateUtil.currentSession();
				for (KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail : kurikulumPunyaMatakuliahDetails) {
					Pertemuan pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail",
									kurikulumPunyaMatakuliahDetail.getId()))
							.add(Restrictions.eq("perkuliahan", perkuliahan)).setMaxResults(1).uniqueResult();

					if (pertemuan == null) {
						pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("tanggal", calendar.getTime()))
								.add(Restrictions.eq("perkuliahan", perkuliahan)).setMaxResults(1).uniqueResult();
					}

					if (pertemuan == null) {
						pertemuan = new Pertemuan();
						pertemuan.setPerkuliahan(perkuliahan);
						pertemuan.setTopik(kurikulumPunyaMatakuliahDetail.getTopik());
						pertemuan.setIndikator(kurikulumPunyaMatakuliahDetail.getIndikator());
						pertemuan.setWaktupembelajaran(kurikulumPunyaMatakuliahDetail.getWaktupembelajaran());
						pertemuan.setPengalamanBelajar(kurikulumPunyaMatakuliahDetail.getPengalamanBelajar());
						pertemuan.setTugasDanPenilaian(kurikulumPunyaMatakuliahDetail.getTugasDanPenilaian());
						pertemuan.setBukuRujukan1(kurikulumPunyaMatakuliahDetail.getBukuRujukan1());
						pertemuan.setStatusPertemuan(kurikulumPunyaMatakuliahDetail.getStatusPertemuan());
						pertemuan.setPertemuanKe(kurikulumPunyaMatakuliahDetail.getNomorUrut());
						pertemuan.setMetodePembelajaran(kurikulumPunyaMatakuliahDetail.getMetodePembelajaran());
						pertemuan.setTanggal(calendar.getTime());
						pertemuan.setKurikulumPunyaMatakuliahDetail(kurikulumPunyaMatakuliahDetail.getId());
						pertemuan.setRuang(perkuliahan.getRuang());
						pertemuan.setWaktuMulai(perkuliahan.getWaktuMulai());
						pertemuan.setWaktuSelesai(perkuliahan.getWaktuSelesai());
						session.save(pertemuan);

						copyLampiran(kurikulumPunyaMatakuliahDetail, pertemuan);
					}

					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 7);
					if (lewatiTanggalMerahNasional) {
						while (Common.isHolidayMerahDanAtauHariLibur(calendar.getTime())) {
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 7);
						}
					}
					System.out.println("calendar = " + Common.dateFormat1.get().format(calendar.getTime()));
				}
			}
		}
	}

	public static void copyLampiran(KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah, Perkuliahan perkuliahan) {

		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			LampiranLain lama = LampiranLain.ambil(perkuliahan.getId(), LampiranLain.SILABUS);

			if (lama == null) {
				LampiranLain c = LampiranLain.ambil(kurikulumPunyaMatakuliah.getId(),
						LampiranLain.SILABUS + KurikulumPunyaMatakuliah.class.getName());
				if (c != null) {
					LampiranLain copy = (LampiranLain) c.clone();
					copy.setRef(perkuliahan.getId());
					copy.setJenis(LampiranLain.SILABUS);
					copy.setCopyDari(c);
					session.getTransaction().begin();
					session.save(copy);
					session.getTransaction().commit();
				}
			}

			lama = LampiranLain.ambil(perkuliahan.getId(), LampiranLain.SAP);

			if (lama == null) {
				LampiranLain c = LampiranLain.ambil(kurikulumPunyaMatakuliah.getId(),
						LampiranLain.SAP + KurikulumPunyaMatakuliah.class.getName());
				if (c != null) {
					LampiranLain copy = (LampiranLain) c.clone();
					copy.setRef(perkuliahan.getId());
					copy.setJenis(LampiranLain.SAP);
					copy.setCopyDari(c);
					session.getTransaction().begin();
					session.save(copy);
					session.getTransaction().commit();
				}
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/MatakuliahKurikulumDetailHelper.java:236");
		}

	}

	@SuppressWarnings("unchecked")
	public static void copyLampiran(KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail,
			Pertemuan pertemuan) {

		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			List<PertemuanFileContent> pertemuanFileContents = session.createCriteria(PertemuanFileContent.class)
					.addOrder(Order.desc("id"))
					.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail", kurikulumPunyaMatakuliahDetail.getId()))
					.list();
			for (PertemuanFileContent c : pertemuanFileContents) {
				PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
				pertemuanFileContent.setFoto(c.getFoto());
				pertemuanFileContent.setNama(c.getNama());
				pertemuanFileContent.setFileMimeType(c.getFileMimeType());
				pertemuanFileContent.setCopyDari(c);
				pertemuanFileContent.setLokasiFisik(c.getLokasiFisik());
				pertemuanFileContent.setKurikulumPunyaMatakuliah(null);
				pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(null);
				pertemuanFileContent.setPertemuan(pertemuan.getId());
				pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());
				session.getTransaction().begin();
				session.save(pertemuanFileContent);
				session.getTransaction().commit();
			}

			List<VideoPertemuan> videoPertemuans = session.createCriteria(VideoPertemuan.class)
					.addOrder(Order.desc("id"))
					.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail", kurikulumPunyaMatakuliahDetail.getId()))
					.list();
			for (VideoPertemuan c : videoPertemuans) {
				VideoPertemuan videoPertemuan = new VideoPertemuan();
				videoPertemuan.setFoto(c.getFoto());
				videoPertemuan.setNama(c.getNama());
				videoPertemuan.setJurusan(c.getJurusan());
				videoPertemuan.setKeterangan(c.getKeterangan());
				videoPertemuan.setKeteranganTambahan(c.getKeteranganTambahan());
				videoPertemuan.setTahunAkademik(c.getTahunAkademik());
				videoPertemuan.setType(c.getType());
				videoPertemuan.setUkuran(c.getUkuran());

				videoPertemuan.setKurikulumPunyaMatakuliah(null);
				videoPertemuan.setKurikulumPunyaMatakuliahDetail(null);
				videoPertemuan.setPertemuan(pertemuan.getId());
				session.getTransaction().begin();
				session.save(videoPertemuan);
				session.getTransaction().commit();
			}

			List<AudioPertemuan> audioPertemuans = session.createCriteria(AudioPertemuan.class)
					.addOrder(Order.desc("id"))
					.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail", kurikulumPunyaMatakuliahDetail.getId()))
					.list();
			for (AudioPertemuan c : audioPertemuans) {
				AudioPertemuan audioPertemuan = new AudioPertemuan();
				audioPertemuan.setFoto(c.getFoto());
				audioPertemuan.setNama(c.getNama());
				audioPertemuan.setJurusan(c.getJurusan());
				audioPertemuan.setKeterangan(c.getKeterangan());
				audioPertemuan.setKeteranganTambahan(c.getKeteranganTambahan());
				audioPertemuan.setTahunAkademik(c.getTahunAkademik());
				audioPertemuan.setType(c.getType());
				audioPertemuan.setUkuran(c.getUkuran());

				audioPertemuan.setKurikulumPunyaMatakuliah(null);
				audioPertemuan.setKurikulumPunyaMatakuliahDetail(null);
				audioPertemuan.setPertemuan(pertemuan.getId());
				session.getTransaction().begin();
				session.save(audioPertemuan);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/MatakuliahKurikulumDetailHelper.java:317");
		}

	}

	public void tampilTombolBuatKurikulumPunyaMatakuliahDetail(Toolbar toolbar, final EventListener eventListener) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Buat Rencana Pembelajaran", "/img/new.gif");
		button.setVisible(add && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Window window = new Window();
				window.setHeight("95%");
				window.setWidth("90%");
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);
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
				column.setWidth("15%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
				row.appendChild(new ais.ui.util.MyLabelBoldAja(kurikulumPunyaMatakuliah.getMatakuliah().getKode()
						+ " - " + kurikulumPunyaMatakuliah.getMatakuliah().getNama()));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum"));
				row.appendChild(new ais.ui.util.MyLabelBoldAja(kurikulumPunyaMatakuliah.getKurikulum().getNama()));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
				row.appendChild(new ais.ui.util.MyLabelBoldAja(kurikulumPunyaMatakuliah.getSemester() + ""));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi Pembelajaran *"));
				final MyTextbox deskripsiPembelajaran;
				row.appendChild(
						deskripsiPembelajaran = new MyTextbox(kurikulumPunyaMatakuliah.getDeskripsiPembelajaran()));
				deskripsiPembelajaran.setRows(3);
				deskripsiPembelajaran.setWidth("90%");

				Common.initKeterangan(rows,
						"Contoh untuk matakuliah Sistem Multimedia : Tujuan utama dari mata kuliah ini adalah membekali mahasiswa dengan berbagai kemampuan dalam membangun sistem multimedia melalui pemahaman akan konsep dari sub-sistem penyusunnya........");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Capaian / Kompetensi *"));
				final MyTextbox kompetensi;
				row.appendChild(kompetensi = new MyTextbox(kurikulumPunyaMatakuliah.getCapaianPembelajaranProdi()));
				kompetensi.setRows(2);
				kompetensi.setWidth("90%");

				Common.initKeterangan(rows,
						"Contoh untuk matakuliah Sistem Multimedia : Mahasiswa memiliki pemahaman mengenai konsep dasar multimedia dan komponen pembentuk sistem multimedia........");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran"));
				Hbox hbox = new Hbox();
				hbox.setParent(row);
				Hbox hbox1 = new Hbox();
				hbox1.setParent(hbox);
				LampiranLain.createDownloadUploadFileLain(hbox1, kurikulumPunyaMatakuliah.getId(),
						KurikulumPunyaMatakuliah.class.getName(), "Silabus", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, null, false, false, false, tbmuser != null && tbmuser.getMahasiswa() == null
								&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);

				Common.initKeterangan(rows,
						"Berupa file silabus atau rencana pembelajaran kuliah, file ini tidak harus diupload, namun sangat dianjurkan diupload, sehingga semua mahasiswa yang mengikuti perkuliahan dapat melihat silabus atau rencana pembelajaran selama satu semester");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Pertemuan"));
				final MyIntbox jumlahKurikulumPunyaMatakuliahDetail;
				row.appendChild(jumlahKurikulumPunyaMatakuliahDetail = new MyIntbox(
						kurikulumPunyaMatakuliah.getJumlahPertemuanPerkuliahanDefault()));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig inti = new MyCheckboxConfig(
						"Inti menurut rujukan peer group / SK Mendiknas 045/2002 (ps. 3 ayat 2e)");
				row.appendChild(inti);
				inti.setChecked(kurikulumPunyaMatakuliah.getInti());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig institusional = new MyCheckboxConfig("Institusional");
				row.appendChild(institusional);
				institusional.setChecked(kurikulumPunyaMatakuliah.getInstitusional());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig terdapatTugas = new MyCheckboxConfig(
						"Bobot Tugas, mata kuliah yang dalam penentuan nilai akhirnya memberikan bobot pada tugas-tugas (praktikum/praktek, PR atau makalah) >= 20%.");
				row.appendChild(terdapatTugas);
				terdapatTugas.setChecked(kurikulumPunyaMatakuliah.getTerdapatTugas());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("UTS"));
				final MyCheckboxConfig uts;
				row.appendChild(uts = new MyCheckboxConfig("Di pertengahan pertamuan merupakan jadwal UTS"));
				uts.setChecked(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("UAS"));
				final MyCheckboxConfig uas;
				row.appendChild(uas = new MyCheckboxConfig("Di akhir pertamuan merupakan jadwal UAS"));
				uas.setChecked(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Hapus pertamuan"));
				final MyCheckboxConfig hapus;
				row.appendChild(hapus = new MyCheckboxConfig("Hapus pertamuan yang sebelumnya sudah ada"));

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
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				final MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (deskripsiPembelajaran.getValue().trim().isEmpty()) {
							MyMessageboxConfig.show("Mohon maaf, deskripsi pembelajaran belum diisi. Langkah yang dapat dilakukan: (1) isi kolom deskripsi pembelajaran pada form yang tersedia; (2) pastikan deskripsi tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						if (kompetensi.getValue().trim().isEmpty()) {
							MyMessageboxConfig.show("Mohon maaf, capaian/kompetensi pembelajaran belum diisi. Langkah yang dapat dilakukan: (1) isi kolom capaian atau kompetensi pembelajaran; (2) pastikan kolom tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						if (jumlahKurikulumPunyaMatakuliahDetail.getValue() == null) {
							MyMessageboxConfig.show("Mohon maaf, jumlah pertemuan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom jumlah pertemuan dengan angka yang sesuai; (2) pastikan nilai tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						save.setDisabled(true);
						Session session = null;
						Transaction transaction = null;
						try {
							session = HibernateUtil.currentNativeSession();
							transaction = session.beginTransaction();
							KurikulumPunyaMatakuliah dataInduk = (KurikulumPunyaMatakuliah) session.get(
									KurikulumPunyaMatakuliah.class, kurikulumPunyaMatakuliah.getId());
							if (dataInduk == null) throw new IllegalStateException("Data kurikulum mata kuliah tidak ditemukan.");
							dataInduk.setDeskripsiPembelajaran(deskripsiPembelajaran.getValue());
							dataInduk.setCapaianPembelajaranProdi(kompetensi.getValue());
							dataInduk.setJumlahPertemuanPerkuliahanDefault(jumlahKurikulumPunyaMatakuliahDetail.getValue());
							dataInduk.setInti(inti.isChecked());
							dataInduk.setInstitusional(institusional.isChecked());
							dataInduk.setTerdapatTugas(terdapatTugas.isChecked());
							session.update(dataInduk);
							if (hapus.isChecked()) {
								session.createSQLQuery("delete from kurikulum_punya_matakuliah_detail where kurikulum_punya_matakuliah=:id")
										.setLong("id", dataInduk.getId()).executeUpdate();
								session.flush(); session.clear();
								dataInduk = (KurikulumPunyaMatakuliah) session.get(KurikulumPunyaMatakuliah.class, kurikulumPunyaMatakuliah.getId());
							}
							for (int i = 1; i <= jumlahKurikulumPunyaMatakuliahDetail.getValue(); i++) {
								KurikulumPunyaMatakuliahDetail detail = (KurikulumPunyaMatakuliahDetail) session
										.createCriteria(KurikulumPunyaMatakuliahDetail.class)
										.add(Restrictions.eq("kurikulumPunyaMatakuliah", dataInduk))
										.add(Restrictions.eq("nomorUrut", i)).setMaxResults(1).uniqueResult();
								if (detail == null) {
									detail = new KurikulumPunyaMatakuliahDetail(); detail.setNomorUrut(i);
									StatusPertemuan statusPertemuan = ConstantValues.TATAP_MUKA;
									if (uas.isChecked() && i == jumlahKurikulumPunyaMatakuliahDetail.getValue()) {
										statusPertemuan = ConstantValues.UAS; detail.setTopik("Pertemuan ke " + i + " : UAS");
										detail.setMetodePembelajaran("Mengerjakan soal UAS");
									}
									if (uts.isChecked() && i == (jumlahKurikulumPunyaMatakuliahDetail.getValue() / 2)) {
										statusPertemuan = ConstantValues.UTS; detail.setTopik("Pertemuan ke " + i + " : UTS");
										detail.setMetodePembelajaran("Mengerjakan soal UTS");
									}
									if (statusPertemuan == null || statusPertemuan.getId() == null) throw new IllegalStateException("Status pertemuan belum dikonfigurasi.");
									detail.setStatusPertemuan((StatusPertemuan) session.get(StatusPertemuan.class, statusPertemuan.getId()));
									detail.setKurikulumPunyaMatakuliah(dataInduk); session.save(detail);
								}
							}
							session.flush(); transaction.commit();
							kurikulumPunyaMatakuliah.setDeskripsiPembelajaran(deskripsiPembelajaran.getValue());
							kurikulumPunyaMatakuliah.setCapaianPembelajaranProdi(kompetensi.getValue());
							kurikulumPunyaMatakuliah.setJumlahPertemuanPerkuliahanDefault(jumlahKurikulumPunyaMatakuliahDetail.getValue());
							window.detach(); Common.createDefaultTimer(eventListener);
						} catch (Exception e) {
							if (transaction != null && transaction.isActive()) try { transaction.rollback(); } catch (Exception rollbackError) { ais.common.ErrorAuditUtil.record(rollbackError, "auto-audit MatakuliahKurikulumDetailHelper:simpan-rollback"); }
							save.setDisabled(false); Common.tampilErrorJikaAdmin(e);
							PesanFormalHelper.tampilkanGagalException("Menyimpan rencana pembelajaran", e,
									new String[] { "Pastikan deskripsi, capaian/kompetensi, dan jumlah pertemuan sudah benar.", "Coba simpan kembali. Jika masih gagal, kirimkan waktu kejadian kepada Administrator." });
						} finally {
							if (session != null && session.isOpen()) try { session.close(); } catch (Exception closeError) { ais.common.ErrorAuditUtil.record(closeError, "auto-audit MatakuliahKurikulumDetailHelper:simpan-close"); }
							try { HibernateUtil.closeSession(); } catch (Exception closeError) { ais.common.ErrorAuditUtil.record(closeError, "auto-audit MatakuliahKurikulumDetailHelper:simpan-close-context"); }
						}
					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});
		button.setParent(toolbar);
	}

	class DetailMatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");

			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail = (KurikulumPunyaMatakuliahDetail) data;

			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			Hbox hb = MatakuliahKurikulumDetailHelper.createKeterangan(kurikulumPunyaMatakuliahDetail,
					new DataLoader() {

						@Override
						public void loadData(Object value) {
							MatakuliahKurikulumDetailHelper.this.loadData(value);
						}
					});
			detail.appendChild(hb);

			if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.ambilDosen() == null) {

				final Textbox topik = new Textbox(kurikulumPunyaMatakuliahDetail.getTopik());
				topik.setWidth("90%");
				topik.setRows(2);
				topik.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setTopik(topik.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				topik.setParent(row);

				final Textbox indikator = new Textbox(kurikulumPunyaMatakuliahDetail.getIndikator());
				indikator.setWidth("90%");
				indikator.setRows(2);
				indikator.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setIndikator(indikator.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				indikator.setParent(row);

				final Textbox waktupembelajaran = new Textbox(kurikulumPunyaMatakuliahDetail.getWaktupembelajaran());
				waktupembelajaran.setWidth("90%");
				waktupembelajaran.setRows(2);
				waktupembelajaran.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setWaktupembelajaran(waktupembelajaran.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				waktupembelajaran.setParent(row);

				final Textbox pengalamanBelajar = new Textbox(kurikulumPunyaMatakuliahDetail.getPengalamanBelajar());
				pengalamanBelajar.setWidth("90%");
				pengalamanBelajar.setRows(2);
				pengalamanBelajar.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setPengalamanBelajar(pengalamanBelajar.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				pengalamanBelajar.setParent(row);

				final Textbox tugasDanPenilaian = new Textbox(kurikulumPunyaMatakuliahDetail.getTugasDanPenilaian());
				tugasDanPenilaian.setWidth("90%");
				tugasDanPenilaian.setRows(2);
				tugasDanPenilaian.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setTugasDanPenilaian(tugasDanPenilaian.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				tugasDanPenilaian.setParent(row);

				final Textbox bukuRujukan1 = new Textbox(kurikulumPunyaMatakuliahDetail.getBukuRujukan1());
				bukuRujukan1.setWidth("90%");
				bukuRujukan1.setRows(2);
				bukuRujukan1.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setBukuRujukan1(bukuRujukan1.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				bukuRujukan1.setParent(row);

				final Textbox bukuRujukan2 = new Textbox(kurikulumPunyaMatakuliahDetail.getBukuRujukan2());
				bukuRujukan2.setWidth("90%");
				bukuRujukan2.setRows(2);
				bukuRujukan2.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setBukuRujukan2(bukuRujukan2.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				bukuRujukan2.setParent(row);

				final Textbox metodePembelajaran = new Textbox(kurikulumPunyaMatakuliahDetail.getMetodePembelajaran());
				metodePembelajaran.setWidth("90%");
				metodePembelajaran.setRows(2);
				metodePembelajaran.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setMetodePembelajaran(metodePembelajaran.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				metodePembelajaran.setParent(row);

				final Combobox combobox = new Combobox();
				Common.insertCombo(combobox, "nama", StatusPertemuan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
				Common.selectComboItem(combobox, kurikulumPunyaMatakuliahDetail.getStatusPertemuan());
				combobox.setWidth("90%");
				combobox.setParent(row);
				combobox.setReadonly(true);

				combobox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kurikulumPunyaMatakuliahDetail
								.setStatusPertemuan((StatusPertemuan) combobox.getSelectedItem().getValue());
						Session session = HibernateUtil.currentSession();
						Common.refreshUpdate(session, (kurikulumPunyaMatakuliahDetail));
					}
				});
			} else {
				new Label(kurikulumPunyaMatakuliahDetail.getTopik()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getIndikator()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getWaktupembelajaran()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getPengalamanBelajar()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getTugasDanPenilaian()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getBukuRujukan1()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getMetodePembelajaran()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getStatusPertemuan() == null ? ""
						: kurikulumPunyaMatakuliahDetail.getStatusPertemuan().getNama()).setParent(row);
			}

			Hbox toolbar = new Hbox();
			toolbar.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.ambilDosen() == null);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(delete);
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

											Common.refreshDelete(kurikulumPunyaMatakuliahDetail);

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();

		kurikulumPunyaMatakuliahDetails = session.createCriteria(KurikulumPunyaMatakuliahDetail.class)
				.addOrder(Order.asc("nomorUrut"))
				.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah)).list();
		ListModel strset = new SimpleListModel(kurikulumPunyaMatakuliahDetails);
		grid.setRowRenderer(new DetailMatakuliahRenderer());
		grid.setModelCheckMobile(strset);

		Common.clear(north);

		MyGrid mygrid = new MyGrid();
		mygrid.setWidth("100%");
		mygrid.setParent(north);

		Columns columns = new Columns();
		columns.setParent(mygrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(mygrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi:"));
		row.appendChild(new MyLabelAgakKecil(kurikulumPunyaMatakuliah.getDeskripsiPembelajaran()));

		row.appendChild(new ais.ui.util.MyLabelConfig("Kompetensi:"));
		row.appendChild(new MyLabelAgakKecil(kurikulumPunyaMatakuliah.getCapaianPembelajaranProdi()));

		row = new MyFormRow();
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran:"));

		Hbox hbox = new Hbox();
		hbox.setParent(row);
		Hbox hbox1 = new Hbox();
		hbox1.setParent(hbox);
		LampiranLain.createDownloadUploadFileLain(hbox1, kurikulumPunyaMatakuliah.getId(),
				KurikulumPunyaMatakuliah.class.getName(), "Silabus", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, tbmuser != null && tbmuser.getMahasiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);

		if (perkuliahan != null) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Tgl. Mulai :"));
			hbox = new Hbox();
			row.appendChild(hbox);
			hbox.appendChild(tanggalMulaiPerkuliahan = new MyDatebox(perkuliahan.getTanggalMulaiPerkuliahan()));
			tanggalMulaiPerkuliahan.setReadonly(true);
			lewatiTanggalMerahNasional = new MyCheckboxConfig("Lewati tanggal merah / hari libur");
			lewatiTanggalMerahNasional.setChecked(perkuliahan.getLewatiTanggalMerahNasional());
			hbox.appendChild(lewatiTanggalMerahNasional);
		} else {
			ais.ui.util.ZkCompat.setSpans(row, "1,3");
		}
	}

	public void display(final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah, final Perkuliahan perkuliahan,
			final Component component) {
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliah;
		this.perkuliahan = perkuliahan;
		Common.clear(component);

		final Tabbox tabbox = new Tabbox();

		if (component instanceof Center) {
			tabbox.setParent(component);
		} else {
			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 600px;");
			groupbox.setParent(component);
			tabbox.setParent(groupbox);
		}

		tabbox.setWidth("100%");
		tabbox.setHeight("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		Tab tab = new Tab("Rencana Pembelajaran");
		tab.setParent(tabs);

		final Tab tabFile = new Tab("File");
		tabFile.setParent(tabs);

		final Tab tabReferensi = new Tab("Buku Referensi");
		tabReferensi.setParent(tabs);

		final Tab tabBukuAjar = new Tab("Buku Ajar");
		Session session = HibernateUtil.currentSession();
		int jumlahBukuAjar = ((Number) session.createCriteria(MatakuliahPunyaBukuBahanAjar.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("matakuliah", kurikulumPunyaMatakuliah.getMatakuliah())).uniqueResult())
				.intValue();
		tabBukuAjar.setLabel("Buku Diktat / Ajar " + (jumlahBukuAjar == 0 ? "" : "(" + jumlahBukuAjar + ")"));
		tabBukuAjar.setParent(tabs);

		final Tab tabArtikel = new Tab("Artikel");
		tabArtikel.setParent(tabs);

		final Tab tabAudio = new Tab("Audio");
		tabAudio.setParent(tabs);

		final Tab tabVideo = new Tab("Video");
		tabVideo.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		if (kurikulumPunyaMatakuliah.getKurikulum().apakahObe(perkuliahan == null ? null : perkuliahan.getTahunAjaran(),
				perkuliahan == null ? null : perkuliahan.getGanjilGenap())) {
			tabpanel.setHeight("1200px");

			org.zkoss.zul.Div wadahObe = new org.zkoss.zul.Div();
			wadahObe.setStyle("width:100%;height:100%;min-height:1100px;overflow:auto;");
			wadahObe.setParent(tabpanel);

			MyInclude iframe = new MyInclude("/pages/master/rps_obe.zul?kur=" + kurikulumPunyaMatakuliah.getId()
					+ (perkuliahan != null && perkuliahan.getId() != null ? "&perkuliahan=" + perkuliahan.getId()
							: ""));
			iframe.setWidth("100%");
			iframe.setParent(wadahObe);

		} else {

			MyPanel panel = new MyPanel();
			panel.setParent(tabpanel);
			panel.setWidth("100%");
			if (component instanceof Center) {
				panel.setHeight("100%");
			} else {
				panel.setHeight("1200px");
			}

			panel.setBorder("none");
			panel.setStyle("border:0px;");

			Toolbar toolbar = new Toolbar();
			toolbar.setVisible(tbmuser != null && tbmuser != null && tbmuser.getMahasiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
			// toolbar.setHeight("25px");
			toolbar.setParent(panel);
			tampilTombolBuatKurikulumPunyaMatakuliahDetail(toolbar, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});

			String[] contents = new String[] { "id", "kurikulumPunyaMatakuliah", "nomorUrut", "indikator", "topik",
					"metodePembelajaran", "pengalamanBelajar", "waktupembelajaran", "tugasDanPenilaian", "bukuRujukan1",
					"statusPertemuan" };
			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

				@Override
				public Criteria initCriteria(boolean order) {
					Session session = HibernateUtil.currentSession();

					return session.createCriteria(KurikulumPunyaMatakuliahDetail.class).addOrder(Order.asc("nomorUrut"))
							.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah));
				}
			}, contents);
			toolbar.appendChild(cetakToolbarbutton);

			MyToolbarbuttonConfig upload = Common.uploadData(new DataSearchDefault() {

				@Override
				public void onSearchDefault(Event event) {
					loadData(null);
				}
			}, KurikulumPunyaMatakuliahDetail.class, contents);
			toolbar.appendChild(upload);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus Semua Rencana Pembelajaran",
					"/img/svg/trash.svg");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											KurikulumPunyaMatakuliahDetailDao kurikulumPunyaMatakuliahDetailDao = DaoFactory
													.getInstance().getKurikulumPunyaMatakuliahDetailDao();

											for (KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail : kurikulumPunyaMatakuliahDetails) {
												kurikulumPunyaMatakuliahDetailDao
														.delete(kurikulumPunyaMatakuliahDetail);
											}

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);

			Panelchildren panelchildren = new Panelchildren();
			panelchildren.setParent(panel);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panelchildren);

			north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			grid = new MyGrid();
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);
			grid.getPagingChild().setMold("os");
			grid.setParent(center);

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("0%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kemampuan akhir pembelajaran");
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kriteria, Indikator & Bobot penilaian");
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Waktu pembelajaran");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Pengalaman Belajar");
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Tugas Dan Penilaian");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Bahan Kajian");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Referensi");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Metode Pembelajaran");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Jenis");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("5%");

			loadData(null);
		}

		final Tabpanel filePerkuliahan = new ais.ui.util.MyTabpanel();
		filePerkuliahan.setParent(tabpanels);
		filePerkuliahan.setHeight("1250px");
		tabFile.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (filePerkuliahan.getChildren().size() == 0) {
					filePerkuliahanHelper.createFile(null, null, kurikulumPunyaMatakuliah, null, filePerkuliahan, null);
				}
			}
		});

		final Tabpanel tabpanelReferensi = new ais.ui.util.MyTabpanel();
		int jumlahReferensi = ((Number) session.createCriteria(KurikulumPunyaMatakuliahPunyaItem.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah)).uniqueResult()).intValue();
		tabReferensi.setLabel("Buku Referensi " + (jumlahReferensi == 0 ? "" : "(" + jumlahReferensi + ")"));

		tabpanelReferensi.setParent(tabpanels);
		tabReferensi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelReferensi.getChildren().size() == 0) {
					tabpanelReferensi.setHeight("1250px");
					KurikulumPunyaMatakuliahPunyaItemHelper kurikulumPunyaMatakuliahPunyaItemHelper = new KurikulumPunyaMatakuliahPunyaItemHelper();
					kurikulumPunyaMatakuliahPunyaItemHelper.display(kurikulumPunyaMatakuliah, tabpanelReferensi);
				}
			}
		});

		final Tabpanel tabpanelBukuAjar = new ais.ui.util.MyTabpanel();
		tabpanelBukuAjar.setParent(tabpanels);
		tabpanelBukuAjar.setHeight("450px");
		tabBukuAjar.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelBukuAjar.getChildren().size() == 0) {

					BukuBahanAjarHelper bukuBahanAjarHelper = new BukuBahanAjarHelper();
					bukuBahanAjarHelper.display(kurikulumPunyaMatakuliah.getMatakuliah(), tabpanelBukuAjar, null);
				}
			}
		});

		final Tabpanel tabpanelArtikel = new ais.ui.util.MyTabpanel();
		int jumlahArtikel = ((Number) session.createCriteria(DataPunyaArtikel.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah)).uniqueResult()).intValue();
		tabArtikel.setLabel("Artikel " + (jumlahArtikel == 0 ? "" : "(" + jumlahArtikel + ")"));

		tabpanelArtikel.setParent(tabpanels);
		tabpanelArtikel.setHeight("1250px");
		tabArtikel.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelArtikel.getChildren().size() == 0) {
					DataPunyaArtikelHelper dataPunyaArtikelHelper = new DataPunyaArtikelHelper();
					dataPunyaArtikelHelper.display(null, null, null, null, null, null, kurikulumPunyaMatakuliah,
							tabpanelArtikel);
				}
			}
		});

		final Tabpanel audioPerkuliahan = new ais.ui.util.MyTabpanel();
		audioPerkuliahan.setParent(tabpanels);
		audioPerkuliahan.setHeight("1250px");
		tabAudio.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (audioPerkuliahan.getChildren().size() == 0) {
					audioPertemuanHelper.display(null, kurikulumPunyaMatakuliah, null, audioPerkuliahan, null);
				}
			}
		});

		final Tabpanel videoPerkuliahan = new ais.ui.util.MyTabpanel();
		videoPerkuliahan.setParent(tabpanels);
		videoPerkuliahan.setHeight("1250px");
		tabVideo.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (videoPerkuliahan.getChildren().size() == 0) {
					videoPertemuanHelper.display(null, kurikulumPunyaMatakuliah, null, videoPerkuliahan, null);
				}
			}
		});

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();
			int videoPertemuans = ((Number) session.createCriteria(VideoPertemuan.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah.getId())).uniqueResult())
					.intValue();

			tabVideo.setLabel("Video" + (videoPertemuans == 0 ? "" : " (" + videoPertemuans + " video)"));

			int audioPertemuans = ((Number) session.createCriteria(AudioPertemuan.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah.getId())).uniqueResult())
					.intValue();

			tabAudio.setLabel("Audio" + (audioPertemuans == 0 ? "" : " (" + audioPertemuans + " audio)"));

			int file = ((Number) session.createCriteria(PertemuanFileContent.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah.getId())).uniqueResult())
					.intValue();

			tabFile.setLabel("File" + (file == 0 ? "" : " (" + file + " file)"));

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static Hbox createKeterangan(final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail,
			final DataLoader dataLoader) {

		Hbox hbox = new Hbox();

		try {

			Session streamSession = StreamingHibernateUtil.getInstance().currentSession();

			hbox = new Hbox();

			String sql = "select (select count(id) from audio_pertemuan where kurikulumpunyamatakuliahdetail = "
					+ kurikulumPunyaMatakuliahDetail.getId()
					+ ") as audio, (select count(id) from video_pertemuan where kurikulumpunyamatakuliahdetail = "
					+ kurikulumPunyaMatakuliahDetail.getId()
					+ ") as video, (select count(id) from pertemuan_file_content where kurikulumpunyamatakuliahdetail = "
					+ kurikulumPunyaMatakuliahDetail.getId() + ") as file";

			List<Object[]> objects = streamSession.createSQLQuery(sql).list();

			if (objects != null && objects.size() != 0) {
				Object[] numbers = objects.get(0);
				Number audio = (Number) numbers[0];
				Number video = (Number) numbers[1];
				Number file = (Number) numbers[2];

				A a = new A("File : " + file + ", ");
				a.setStyle("font-size:12px" + (file.intValue() > 0 ? ";color:red;" : ""));
				a.setHref("");
				a.setParent(hbox);
				if (dataLoader != null) {
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							new KurikulumPunyaMatakuliahHelper().display(kurikulumPunyaMatakuliahDetail, dataLoader, 0);

						}
					});
				}

				a = new A("Audio : " + audio + ", ");
				a.setStyle("font-size:12px" + (audio.intValue() > 0 ? ";color:red;" : ""));
				a.setHref("");
				a.setParent(hbox);
				if (dataLoader != null) {
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							new KurikulumPunyaMatakuliahHelper().display(kurikulumPunyaMatakuliahDetail, dataLoader, 1);

						}
					});
				}
				a = new A("Video : " + video + ".");
				a.setStyle("font-size:12px" + (video.intValue() > 0 ? ";color:red;" : ""));
				a.setHref("");
				a.setParent(hbox);
				if (dataLoader != null) {
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							new KurikulumPunyaMatakuliahHelper().display(kurikulumPunyaMatakuliahDetail, dataLoader, 2);

						}
					});
				}

			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			StreamingHibernateUtil.getInstance().rollbackTransaction();
		}

		return hbox;
	}

}
