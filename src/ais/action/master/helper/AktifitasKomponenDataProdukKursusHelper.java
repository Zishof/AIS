package ais.action.master.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.kursus.KomponenDataProdukKursus;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Composer ZK untuk tampilan "timeline" e-Learning satu {@link KomponenDataProdukKursus} (mirip
 * {@code AktifitasPerkuliahanHelper} tetapi untuk komponen produk kursus, bukan matakuliah reguler):
 * menampilkan grid pertemuan berpaging satu-per-halaman ({@code pageSize=1}) dengan navigasi otomatis
 * ke pertemuan pada/terdekat hari ini saat pertama dibuka, setiap halaman menampilkan judul topik,
 * pengajar, catatan diskusi, tombol konferensi video, tombol absen, dan (bila diaktifkan lewat
 * konfigurasi {@code komentar_tampil_di_halaman_utama_elearning}) komentar diskusi pertemuan.
 *
 * <p>
 * {@link #initDetail} melakukan <b>auto-generate</b> baris {@link Pertemuan} bila jumlah pertemuan
 * yang sudah ada belum mencapai {@code komponenDataProdukKursus.getJumlahPertemuan()} — hanya
 * dijalankan untuk pengguna operator (bukan mahasiswa/siswa/calon siswa/calon mahasiswa), dengan
 * tanggal tiap pertemuan baru dijadwalkan berjarak 1 minggu dari pertemuan sebelumnya mulai dari
 * {@code komponenDataProdukKursus.getMulai()}.
 * </p>
 */
public class AktifitasKomponenDataProdukKursusHelper {

	/** Helper penjadwalan pertemuan komponen kursus, dipakai tombol "Agenda Kegiatan" pada {@link #initAgendaKomponenDataProdukKursus}. */
	protected PenjadwalanKomponenDataProdukKursusHelper penjadwalanHelper = new PenjadwalanKomponenDataProdukKursusHelper();

	/** Konstruktor tanpa argumen; tidak ada inisialisasi state khusus selain {@link #penjadwalanHelper}. */
	public AktifitasKomponenDataProdukKursusHelper() {
	}

	/**
	 * Membangun toolbar aksi (Agenda Kegiatan, Refresh) untuk komponen produk kursus yang diberikan,
	 * tampil hanya bagi pengguna non-mahasiswa/non-siswa.
	 *
	 * @param komponenDataProdukKursus komponen kursus terkait
	 * @param dataLoader                callback yang dipanggil ulang setelah aksi toolbar selesai
	 * @return toolbar siap ditambahkan ke komponen lain
	 */
	public Toolbar initAgendaKomponenDataProdukKursus(final KomponenDataProdukKursus komponenDataProdukKursus,
			final DataLoader dataLoader) {

		Tbmuser tbmuser = Common.getCurrentUser();
		Toolbar hbox = new Toolbar();
		hbox.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Agenda Kegiatan", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				penjadwalanHelper.display(komponenDataProdukKursus, dataLoader);
			}

		});

		button.setParent(hbox);

//		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
//		button.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event event) throws Exception {
//				CommonReportHelper.onLaporanAbsensi(komponenDataProdukKursus, true);
//			}
//
//		});
//		button.setParent(hbox);

//		PenjadwalanHelper.tampilTombolAmbil(hbox, null, null, null, null, null, komponenDataProdukKursus, null, dataLoader);

//		AktifitasPerkuliahanHelper.tampilCalender(hbox, dataLoader, komponenDataProdukKursus);

//		ClassRoomUtil.createButton(komponenDataProdukKursus, dataLoader).setParent(hbox);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(hbox);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				komponenDataProdukKursus.belum();
				dataLoader.loadData(null);
			}
		});

		return hbox;
	}

	/** Seperti {@link #initDetail(KomponenDataProdukKursus, DataLoader, Component, boolean)} dengan {@code dataLoader} default yang memuat ulang tampilan ini sendiri. */
	public void initDetail(KomponenDataProdukKursus komponenDataProdukKursus, Component groupbox, boolean tatapMuka)
			throws Exception {
		initDetail(komponenDataProdukKursus, null, groupbox, tatapMuka);
	}

	/**
	 * Membangun tampilan timeline pertemuan komponen kursus ke dalam {@code groupbox}: toolbar aksi,
	 * lalu auto-generate pertemuan yang belum ada (khusus operator, lihat dokumentasi kelas), lalu
	 * grid berpaging satu pertemuan per halaman yang otomatis membuka halaman pertemuan pertama yang
	 * jatuh pada/setelah hari ini.
	 *
	 * @param komponenDataProdukKursus komponen kursus yang timeline-nya ditampilkan
	 * @param mydataLoader              callback pemuat ulang kustom; {@code null} memakai default yang
	 *                                   memanggil kembali method ini
	 * @param groupbox                  container ZK yang akan diisi (dibersihkan lebih dulu)
	 * @param tatapMuka                  status pertemuan default untuk pertemuan baru yang di-generate
	 *                                    ({@code true}=tatap muka, {@code false}=daring)
	 * @throws Exception diteruskan dari kegagalan Hibernate saat auto-generate pertemuan
	 */
	public void initDetail(final KomponenDataProdukKursus komponenDataProdukKursus, final DataLoader mydataLoader,
			final Component groupbox, final boolean tatapMuka) throws Exception {

		final DataLoader dataLoader = mydataLoader == null ? new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(komponenDataProdukKursus, groupbox, tatapMuka);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} : mydataLoader;

		Common.clear(groupbox);

		groupbox.appendChild(initAgendaKomponenDataProdukKursus(komponenDataProdukKursus, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(komponenDataProdukKursus, groupbox, tatapMuka);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		}));

		TreeMap<String, Long> pertemuans = komponenDataProdukKursus.ambilPertemuan();
		System.out.println("pertemuans -> " + pertemuans.size());
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& pertemuans.size() != komponenDataProdukKursus.getJumlahPertemuan()) {

			Calendar mulai = Calendar.getInstance();
			mulai.setTime(komponenDataProdukKursus.getMulai() == null ? ais.ui.util.WaktuUtil.getDate()
					: komponenDataProdukKursus.getMulai());

			for (int pertemuanKe = 1; pertemuanKe <= komponenDataProdukKursus.getJumlahPertemuan(); pertemuanKe++) {
				Session session = HibernateUtil.currentSession();
				Pertemuan pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("komponenDataProdukKursus", komponenDataProdukKursus))
						.add(Restrictions.eq("pertemuanKe", pertemuanKe)).setMaxResults(1).uniqueResult();
				if (pertemuan == null) {
					pertemuan = new Pertemuan();
					pertemuan.setStatusPertemuan(tatapMuka ? ConstantValues.TATAP_MUKA : ConstantValues.DARING);
					pertemuan.setTanggal(mulai.getTime());
					pertemuan.setKomponenDataProdukKursus(komponenDataProdukKursus);
					pertemuan.setTopik("Agenda materi \"" + komponenDataProdukKursus.getNama() + "\"");
					pertemuan.setWaktuMulai(Common.timeFormat2.get().format(mulai.getTime()));
					pertemuan.setWaktuSelesai(Common.timeFormat2.get().format(mulai.getTime()));

					try {
						session.save(pertemuan);
						session.flush();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasKomponenDataProdukKursusHelper.java:158");
					}
				}

				pertemuans.put(Common.dateFormat8.get().format(pertemuan.getTanggal()) + "_" + pertemuan.getId(),
						pertemuan.getId());

				mulai.set(Calendar.WEEK_OF_YEAR, mulai.get(Calendar.WEEK_OF_YEAR) + 1);
			}

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenDataProdukKursus.belum();
					initDetail(komponenDataProdukKursus, dataLoader, groupbox, tatapMuka);
				}
			});

			return;
		}
//		groupbox.setStyle("height:6000px;");

		MyGrid grid = new MyGrid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("height: 6000px;");
		grid.setSclass("fgrid");
		grid.setMold("paging");
		grid.setPageSize(1);
		grid.getPagingChild().setMold("os");
		grid.setPagingPosition("top");

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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasKomponenDataProdukKursusHelper.java:206");
			// TODO: handle exception
		}

		boolean mobile = Common.isMobile();
		int selected = 0;
		Date sekarang = WaktuUtil.getDate();
		for (Long pertemuanid : pertemuans.values()) {
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
			if (pertemuan != null) {
				Row rowUtama = new Row();
				rowUtama.setParent(rows);
				rowUtama.setValign("top");
				
				if (pertemuan.getTanggal() != null && pertemuan.getTanggal().before(sekarang)) {
					selected++;
				}

				String tgl = pertemuan.getTanggal() == null ? "-"
						: Common.dateFormat11.get().format(pertemuan.getTanggal()) + " "
								+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
										: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai());

				Groupbox pertemuanBox = new ais.ui.util.MyGroupboxStyled();
				pertemuanBox.setWidth(mobile ? "93%" : "95%");
				rowUtama.appendChild(pertemuanBox);
				MyCaptionStyled c;
				pertemuanBox.appendChild(
						c = new MyCaptionStyled("Pertemuan ke-" + pertemuan.getPertemuanKe() + ", " + tgl));
				c.setStyle("font-size:12px;font-weight: bolder;text-decoration: none;color:"
						+ pertemuan.warna().split(",")[0] + ";border: 1px solid " + pertemuan.warna().split(",")[0]
						+ ";\r\n" + "  padding: 5px;" + "  background-color: rgba(169,169,169,0.4);" + "  border-radius: 5px 15px;");

				if (mobile) {
					Vbox v = new Vbox();
					v.setParent(pertemuanBox);
					Common.displayPegawaiKomponenDataProdukKursusUmum(v, komponenDataProdukKursus, true, false, null);
				} else {
					Hbox v = Common.displayPegawaiKomponenDataProdukKursusUmum(pertemuanBox, komponenDataProdukKursus,
							true, false, null);
					v.appendChild(new Space());
				}

				Vbox a = RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan,
						pertemuan.getTanggal() == null ? "-"
								: Common.dateFormat11.get().format(pertemuan.getTanggal()) + " "
										+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
												: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai()));

				Vbox vbox = new Vbox();
				vbox.setParent(pertemuanBox);

				a.setParent(vbox);
				new Label(pertemuan.getTopik()).setParent(vbox);

				DashboardTimelinePertemuan.displayCatatan(vbox, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
					}
				}, pertemuan, tbmuser, mobile);

				Component aa = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, null, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
					}
				});

				Component bb = AbsensiHelper.createTombolAbsen(pertemuan, true, dataLoader);

				AktifitasPerkuliahanHelper.createKeterangan(pertemuan, dataLoader, aa, bb,
						DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuan)).setParent(vbox);

				pertemuan.masukkanData("akses");
				if (Common.bolehKonfigurasi("komentar_tampil_di_halaman_utama_elearning")) {
					Vbox vbox2 = new Vbox();
					vbox2.setParent(pertemuanBox);
					if (!pertemuan.udah()) {
						Session session = HibernateUtil.currentSession();
						pertemuan.reInitPertemuanPunyaDiskusi(session);
					}

					TreeSet<Long> pertemuanPunyaDiskusisa = pertemuan.ambilPertemuanPunyaDiskusiTotal(urut);
					DashboardTimelinePertemuan.loadKomentarDetail(null, "42px", pertemuanPunyaDiskusisa, pertemuan,
							vbox2, "background-color: rgba(255,255,255,0.5);", 0, 10, false, null);
				}
			}
		}
		
		try {
			grid.getPagingChild().setActivePage(selected);
		} catch (Exception e) {
			try {
				grid.getPagingChild().setActivePage(selected - 1);
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasKomponenDataProdukKursusHelper.java:303");
				// TODO: handle exception
			}
		}

	}

}
