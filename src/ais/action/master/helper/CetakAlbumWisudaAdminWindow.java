package ais.action.master.helper;

import java.io.File;
import java.io.Serializable;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Judisium;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.Skripsi;
import ais.database.model.Wisuda;
import ais.database.model.file.FotoMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk cetak album wisuda admin window. Kelas ini memberi nama dan batas tanggung
 * jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox wisudaKe}, {@code Toolbar
 * toolbar}, {@code MyButtonConfig cetak}, {@code MyButtonConfig batal}; inisialisasi/lifecycle ({@code init()});
 * pembacaan/pencarian ({@code getDataAlbumWisudaAdmin()}); pelaporan/ekspor ({@code onCetakAlbumWisudaAdmin()});
 * operasi domain lain ({@code generateImageAlbumWisudaAdmin()}); konfigurasi constructor: {@code wisudaKe}.
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class CetakAlbumWisudaAdminWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5620991583788581962L;

	private Combobox wisudaKe;

	private Toolbar toolbar;
	private MyButtonConfig cetak;
	private MyButtonConfig batal;

	public CetakAlbumWisudaAdminWindow() {
		super();
		try {
			Common.insertCombo(wisudaKe = new Combobox(), "wisudaKe", Wisuda.class);
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() {

		setClosable(true);
		setTitle("No. Registrasi Wisuda");
		setWidth("500px");
		setHeight("140px");
		setPosition("center");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Cetak Album Wisuda");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
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
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("70%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Wisuda ke"));
		row.appendChild(wisudaKe);
		wisudaKe.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		cetak = new MyButtonConfig("Cetak");
		batal = new MyButtonConfig("Batal");

		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onCetakAlbumWisudaAdmin(event);
			}
		});
		cetak.setParent(toolbar);

		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				CetakAlbumWisudaAdminWindow.this.detach();
			}
		});
		batal.setParent(toolbar);

	}

	@SuppressWarnings({})
	public boolean onCetakAlbumWisudaAdmin(Event event) throws Exception {
		if (wisudaKe.getSelectedItem() == null) {
			MyMessageboxConfig.show("Wisuda-ke harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		generateImageAlbumWisudaAdmin(event);
		Wisuda wisuda = (Wisuda) wisudaKe.getSelectedItem().getValue();
		List<Map<String, Serializable>> maps = getDataAlbumWisudaAdmin();

		final Map<String, Integer> parameters = new HashMap<String, Integer>();
		parameters.put("wisuda_ke", wisuda.getWisudaKe());

		Report.generatePDFReport("pdf", parameters, "Album_Wisuda_Admin", ais.ui.util.WaktuUtil.getDate(), maps);

		return true;

	}

	@SuppressWarnings("unchecked")
	public void generateImageAlbumWisudaAdmin(Event event) throws Exception {
		Session session = HibernateUtil.currentSession();

		List<PendaftaranWisuda> listPendaftaranWisuda = session.createCriteria(PendaftaranWisuda.class)
				.addOrder(Order.asc("id")).add(Restrictions.eq("wisuda", wisudaKe.getSelectedItem().getValue())).list();

		// create directory
		String strDirectoy = Sessions.getCurrent().getWebApp().getRealPath("/tmp");
		(new File(strDirectoy)).mkdir();

		try {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			Iterator<?> itr = listPendaftaranWisuda.iterator();
			while (itr.hasNext()) {
				PendaftaranWisuda beanPendaftaranWisuda = (PendaftaranWisuda) itr.next();

				FotoMahasiswa fotobm = (FotoMahasiswa) streamingSession.createCriteria(FotoMahasiswa.class)
						.addOrder(Order.desc("id"))
						.add(Restrictions.eq("mahasiswa", beanPendaftaranWisuda.getMahasiswa().getId()))
						.setMaxResults(1).uniqueResult();

				// Blob content = fotobm.getFoto();

				try {

					// File blobFile = new File(Common.folderFoto +
					// fotobm.getNamaFile());
					File blobFile = new File(strDirectoy + "/" + fotobm.getNama());
					CommonMedia.getFileFotoDenganFile(fotobm, blobFile);
					// FileOutputStream outStream = new
					// FileOutputStream(blobFile);
					// InputStream inStream = content.// getBinaryStream();

					// int length = -1;
					// int size = 4096;
					// byte[] buffer = new byte[size];
					//
					// while ((length = inStream.read(buffer)) != -1) {
					// outStream.write(buffer, 0, length);
					// outStream.flush();
					// }
					//
					// inStream.close();
					// outStream.close();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					System.out.println("ERROR(djv_exportBlob) Unable to export: " + fotobm.getNama());
				}

			}
			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Serializable>> getDataAlbumWisudaAdmin() {

		Session session = HibernateUtil.currentSession();
		List<PendaftaranWisuda> listPendaftaranWisuda = session.createCriteria(PendaftaranWisuda.class)
				.addOrder(Order.asc("id")).add(Restrictions.eq("wisuda", wisudaKe.getSelectedItem().getValue())).list();

		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
		Iterator<?> itr = listPendaftaranWisuda.iterator();

		try {
			while (itr.hasNext()) {
				PendaftaranWisuda beanPendaftaranWisuda = (PendaftaranWisuda) itr.next();
				Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
				map.put("nim", beanPendaftaranWisuda.getMahasiswa().getNim());
				map.put("nama", beanPendaftaranWisuda.getMahasiswa().getNama());
				map.put("alamat", beanPendaftaranWisuda.getMahasiswa().getAlamat());
				map.put("telp", beanPendaftaranWisuda.getMahasiswa().getTelp());
				map.put("program_studi", beanPendaftaranWisuda.getMahasiswa().getJenjang().getNama());
				map.put("fakultas", beanPendaftaranWisuda.getMahasiswa().getJurusan().getFakultas().getNama());

				Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class)
						.add(Restrictions.eq("mahasiswa", beanPendaftaranWisuda.getMahasiswa())).uniqueResult();
				map.put("judul_skripsi", skripsi.getJudul());

				Mahasiswa mahasiswa = beanPendaftaranWisuda.getMahasiswa();
				mahasiswa.putPhoto(map);
				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa,
						mahasiswa.getSemesterLulus() == null ? mahasiswa.currentSemester()
								: mahasiswa.getSemesterLulus(),
						null, null);

				map.put("tanggal_masuk", mahasiswa.getTanggalKegiatanBelajarMengajar());
				map.put("tanggal_lulus", mahasiswa.getTanggalLulus());
				map.put("tanggal_masuk_str", mahasiswa.getTanggalKegiatanBelajarMengajar() == null ? ""
						: Common.dateFormat11.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar()));
				map.put("tanggal_lulus_str", mahasiswa.getTanggalLulus() == null ? ""
						: Common.dateFormat11.get().format(mahasiswa.getTanggalLulus()));
				map.put("tahun_lulus", mahasiswa.getTahunLulus());
				map.put("program", mahasiswa.getProgram());

				map.put("semesterMulai", mahasiswa.getSemesterMulai());
				map.put("semester", krsMahasiswa.getSemester());
				map.put("nama_mahasiswa", mahasiswa.getNama());
				map.put("nama", mahasiswa.getNama());
				map.put("tahunangkatan", mahasiswa.getTahunangkatan());
				map.put("nim", mahasiswa.getNim());
				map.put("jurusan", mahasiswa.getJurusan().getNama());
				map.put("nama_jurusan", mahasiswa.getJurusan().getNama());
				map.put("id_fakultas", mahasiswa.getJurusan().getFakultas().getId());
				map.put("fakultas_id", mahasiswa.getJurusan().getFakultas().getId());
				map.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				map.put("nama_fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				map.put("jenjang", mahasiswa.getJurusan().getJenjang().getNama());

				map.put("dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
						: krsMahasiswa.getDosenPa().getNama());
				map.put("nip_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
						: krsMahasiswa.getDosenPa().getCode());
				map.put("nidn_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
						: krsMahasiswa.getDosenPa().getNidn());

				map.put("nama_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getNama());
				map.put("nip_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getCode());
				map.put("nidn_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getNidn());

				map.put("nama_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
				map.put("nip_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
				map.put("nidn_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

				map.put("nama_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
				map.put("nip_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
				map.put("nidn_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

				map.put("nama_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getNama());
				map.put("nip_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getCode());
				map.put("nidn_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getNidn());

				map.put("nama_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getNama());
				map.put("nip_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getCode());
				map.put("nidn_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getNidn());

				map.put("nama_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNama());
				map.put("nip_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getCode());
				map.put("nidn_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNidn());
				Double ipmhs = krsMahasiswa.getIps();
				Double ipkmhs = krsMahasiswa.getIpk();

				Integer sksmhss = krsMahasiswa.getSksYangDiambil();
				Integer sksmhs = krsMahasiswa.getSksk();
				map.put("ipk", ipkmhs);
				map.put("ips", ipmhs);
				map.put("sksk", sksmhs);
				map.put("sks", sksmhss);

				map.put("ip_kumulatif", ipkmhs);

				map.put("ip_semester", ipmhs);
				map.put("judulSkripsi", mahasiswa.getJudulSkripsi());
				map.put("tahun_masuk", mahasiswa.getTahunangkatan());
				map.put("tahun_lulus", mahasiswa.getTahunLulus());
				map.put("tanggalYudisium", mahasiswa.getTanggalYudisium());
				map.put("tempatlahir", mahasiswa.getTempatlahir());
				map.put("tanggallahir", mahasiswa.getTanggallahir());
				map.put("tanggal_lahir", mahasiswa.getTanggallahir());
				map.put("kelamin", mahasiswa.getKelamin());
				map.put("agama", mahasiswa.getAgama() == null ? "" : mahasiswa.getAgama().getNama());
				String alamatlengkap = mahasiswa.getAlamat();
				Judisium judisium = Common.hitungJudisium(mahasiswa, krsMahasiswa);
				map.put("judisium", judisium == null ? "" : judisium.getNama());
				map.put("judisium", judisium == null ? "" : judisium.getNamaen());

				map.put("no_ijazah1", mahasiswa.getNoIjazah1());
				map.put("gelar", mahasiswa.getJurusan().getGelar());

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				String ActualDate = Common.databaseDateFormat.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar());
				String ActualLulusSekarang = Common.databaseDateFormat.get()
						.format(mahasiswa.getTanggalLulus() == null ? ais.ui.util.WaktuUtil.getDate()
								: mahasiswa.getTanggalLulus());
				java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
				java.time.LocalDate currentdate = java.time.LocalDate.parse(ActualLulusSekarang, formatter);
				Period period = Period.between(dt, currentdate);
				System.out.println("Years " + period.getYears()); // Years 2
				System.out.println("Months " + period.getMonths()); // Months
																	// 1
				System.out.println("Days " + period.getDays()); // Days 11

				int workDays = 0;
				LocalDate jamesBirthDay = new LocalDate(mahasiswa.getTanggalKegiatanBelajarMengajar());
				LocalDate now = new LocalDate(mahasiswa.getTanggalLulus() == null ? ais.ui.util.WaktuUtil.getDate()
						: mahasiswa.getTanggalLulus());
				workDays = Days.daysBetween(jamesBirthDay, now).getDays();
				map.put("lama_sudi", workDays);

				try {
					map.put("masa_studi_dan_sisa", mahasiswa.ambilMasaStudi());

					map.put("masa_studi_tahun", period.getYears());
					map.put("masa_studi_semester", workDays / 183);

					map.put("masa_studi", period.getYears() + " tahun, " + period.getMonths() + " bulan, "
							+ period.getDays() + " hari. ");

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/CetakAlbumWisudaAdminWindow.java:404");
				}

				map.put("masa_studi_tahun_info",
						period.getYears() + " (" + IndonesianNumberToWords.convert(period.getYears()) + ") tahun");
				map.put("nama_cap", Common.capitailizeWord(mahasiswa.getNama()));

				map.put("bahasa_pengantar", mahasiswa.getJurusan().getBahasaPengantar());
				map.put("nama_asli", mahasiswa.getNama());
				map.put("tempat_cap", Common.capitailizeWord(mahasiswa.getTempatlahir()));
				map.put("tempat", mahasiswa.getTempatlahir());
				map.put("tanggal_lahir_m", mahasiswa.getTanggallahirManual());
				map.put("nim", mahasiswa.getNim());
				map.put("jenjang_syarat", mahasiswa.getJenjang().getSyarat());
				map.put("jenjang", mahasiswa.getJenjang().getKeterangan());
				map.put("jenjang_en", mahasiswa.getJenjang().getKeteranganEn());
				map.put("tanggal_lulus_id", mahasiswa.getTanggalLulus() == null ? "..........."
						: Common.dateFormat2.get().format(mahasiswa.getTanggalLulus()));

				map.put("tanggal_lulus_en", mahasiswa.getTanggalLulus() == null ? "..........."
						: Common.dateFormat2En.get().format(mahasiswa.getTanggalLulus()));

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(mahasiswa.getTanggalKegiatanBelajarMengajar());
				int tanggal_tgl = calendar.get(Calendar.DATE);
				int tahun = calendar.get(Calendar.YEAR);

				map.put("tanggal_satuan_masuk", tanggal_tgl);
				map.put("bulan_satuan_masuk", Common.monthFormat2.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar()));
				map.put("tahun_satuan_masuk", tahun);

				// map.put("tanggal_satuan_lulus_en", tanggal==1? );

				if (mahasiswa.getTanggalLulus() == null) {
					map.put("tanggal_satuan_lulus", "..");
					map.put("bulan_satuan_lulus", ".....");
					map.put("tahun_satuan_lulus", "....");

					map.put("tanggal_satuan_lulus_en", "..");
					map.put("bulan_satuan_lulus_en", ".....");
					map.put("tahun_satuan_lulus_en", "....");
				} else {
					calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(mahasiswa.getTanggalLulus());
					tanggal_tgl = calendar.get(Calendar.DATE);
					tahun = calendar.get(Calendar.YEAR);

					map.put("bulan_satuan_lulus_en", mahasiswa.getTanggalLulus() == null ? ""
							: Common.monthFormat2En.get().format(mahasiswa.getTanggalLulus()));
					map.put("tahun_satuan_lulus_en", tahun);

					map.put("tanggal_satuan_lulus", tanggal_tgl);
					map.put("bulan_satuan_lulus", mahasiswa.getTanggalLulus() == null ? ""
							: Common.monthFormat2.get().format(mahasiswa.getTanggalLulus()));
					map.put("tahun_satuan_lulus", tahun);

					// map.put("tanggal_satuan_lulus_en", tanggal==1? );
					map.put("bulan_satuan_lulus_en", mahasiswa.getTanggalLulus() == null ? ""
							: Common.monthFormat2En.get().format(mahasiswa.getTanggalLulus()));
					map.put("tahun_satuan_lulus_en", tahun);
				}

				map.put("jurusan", mahasiswa.getJurusan().getNama());
				map.put("jurusan_en", mahasiswa.getJurusan().getNamaEn());
				map.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				map.put("sk_akreditasi", mahasiswa.getJurusan().getNoSkAkreditasi());
				map.put("fakultas_en", mahasiswa.getJurusan().getFakultas().getNamaEn());
				map.put("gelar", mahasiswa.getJurusan().getGelar());
				map.put("gelar_singkat", mahasiswa.getJurusan().getSingkatanGelar());

				map.put("no_ijazah_1", mahasiswa.getNoIjazah1());
				map.put("no_ijazah_2", mahasiswa.getNoIjazah2());
				map.put("no_akta_1", mahasiswa.getNoAkta1());
				map.put("no_akta_2", mahasiswa.getNoAkta2());
				map.put("gelar_en", mahasiswa.getJurusan().getGelarEn());
				map.put("gelar_en_singkat", mahasiswa.getJurusan().getSingkatanGelarEn());

				map.put("noHp", mahasiswa.getTelp());
				map.put("alamatlengkap", alamatlengkap);
				map.put("email", mahasiswa.getEmail());

				maps.add(map);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return maps;
	}

}
