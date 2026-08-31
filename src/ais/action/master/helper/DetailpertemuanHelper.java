package ais.action.master.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
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
import org.zkoss.zul.A;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.sekolah.helper.PenjadwalanSiswaHelper;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.VoKelasPunyaSiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper ZK yang menampilkan matriks kehadiran (peserta &times; pertemuan) untuk satu
 * {@link VOPembelajaran} — baik perkuliahan ({@link Perkuliahan}) maupun jadwal pelajaran
 * sekolah ({@link ais.database.model.sekolah.JadwalPelajaran}). Satu baris grid mewakili satu
 * peserta (mahasiswa/siswa, atau — khusus perkuliahan tanpa mahasiswa terdaftar — satu dosen
 * pengampu direpresentasikan sebagai id semu negatif), satu kolom mewakili satu pertemuan;
 * setiap sel menampilkan kode status absensi ({@code "-"} bila belum ada catatan). Karena
 * jumlah pertemuan bisa banyak, tampilan dipecah otomatis menjadi beberapa tab "Pertemuan ke X
 * sd Y" berisi maksimal 16 kolom per tab ({@link #displayDetailPertemuan}) memakai
 * {@link ais.ui.util.MyButtonTabbox} data-driven.
 *
 * <p>
 * Header tiap kolom pertemuan adalah tombol yang membuka {@link PertemuanHelper} untuk mengedit
 * pertemuan tersebut, plus indikator jumlah peserta yang pernah "online" pada sesi video
 * conference ({@link ais.action.master.dashboard.admin.DashboardTimelinePertemuan}). Toolbar
 * atas menyediakan pencarian peserta, tombol Agenda Pertemuan (penjadwalan lewat
 * {@link PenjadwalanHelper}/{@link ais.action.master.sekolah.helper.PenjadwalanSiswaHelper}),
 * laporan absensi/UTS/UAS ({@link ais.action.report.CommonReportHelper}), serta unduh/unggah
 * data absensi dalam format Excel — unduh menghasilkan template dua baris header (baris 0
 * berisi id pertemuan tersembunyi, baris 1 label "Ke-N"), unggah membaca kembali baris 0
 * tersebut untuk memetakan kolom ke id pertemuan sehingga proses kebal terhadap perubahan
 * urutan/penambahan pertemuan di antara unduh dan unggah. Proses unggah berjalan di thread
 * terpisah dengan sesi Hibernate miliknya sendiri (bukan sesi native bersama) dan melaporkan
 * statistik jujur (jumlah sel tersimpan/dilewati/baris gagal) alih-alih pesan sukses generik.
 * </p>
 */
public class DetailpertemuanHelper implements DataLoader {

	private VOPembelajaran voPembelajaran;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMMM yyyy", Common.locale);

	private List<Pertemuan> pertemuans;
	private Textbox nama;

	private Tbmuser tbmuser = Common.getCurrentUser();

	/**
	 * Renderer baris untuk matriks absensi perkuliahan. Data baris berupa {@code Long}: bila
	 * negatif, direpresentasikan sebagai baris dosen pengampu (id sebenarnya {@code Math.abs(id)});
	 * bila positif, merujuk ke {@link Detailperkuliahan} (satu mahasiswa terdaftar). Kolom sisanya
	 * (sejumlah pertemuan dalam rentang {@code mulai}..{@code sampai}) diisi kode status absensi.
	 */
	class DetailPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		private int mulai;
		private int sampai;

		public DetailPerkuliahanRenderer(int mulai, int sampai) {
			this.mulai = mulai;
			this.sampai = sampai;
		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");

			Long id = (Long) data;
			if (id < 0L) {
				Dosen dosen = (Dosen) ConstantValues.ambil(Dosen.class.getName(), Math.abs(id));

				CommonMedia.tampilkanGambarKecil(dosen).setParent(row);

				new Label(dosen.getNidn()).setParent(row);
				new Label(dosen.getNama()).setParent(row);
				new Label(dosen.getCode()).setParent(row);

				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif()) {
						if (mulai <= pertemuan.getPertemuanKe() && sampai >= pertemuan.getPertemuanKe()) {
							String kode = pertemuan.retreiveAbsensiKode(dosen.getId());

							if (kode == null) {
								new Label("-").setParent(row);
							} else {
								new Label(kode).setParent(row);
							}
						}
					}
				}
			} else {

				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, data.toString());

				CommonMedia.tampilkanGambarKecil(detailperkuliahan.getMahasiswa()).setParent(row);

				new Label(detailperkuliahan.getMahasiswa().getNim()).setParent(row);
				new Label(detailperkuliahan.getMahasiswa().getNama()).setParent(row);
				new Label(detailperkuliahan.getDetailNilaiTambahan()).setParent(row);

				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif()) {
						if (mulai <= pertemuan.getPertemuanKe() && sampai >= pertemuan.getPertemuanKe()) {
							String kode = pertemuan.retreiveAbsensiKode(detailperkuliahan.getMahasiswa().getId());

							if (kode == null) {
								new Label("-").setParent(row);
							} else {
								new Label(kode).setParent(row);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Renderer baris untuk matriks absensi jadwal pelajaran sekolah, satu baris per
	 * {@link VoKelasPunyaSiswa} (siswa anggota kelas). Kode absensi tiap kolom pertemuan diisi
	 * secara tertunda lewat {@link Common#createDefaultTimer} agar rendering baris awal (foto,
	 * NIS, nama) tidak menunggu kalkulasi kehadiran seluruh pertemuan selesai.
	 */
	class KelasSiswaRenderer extends ais.ui.util.MyRowRenderer {

		private int mulai;
		private int sampai;

		public KelasSiswaRenderer(int mulai, int sampai) {
			this.mulai = mulai;
			this.sampai = sampai;
		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final VoKelasPunyaSiswa kelasSiswaPunyaSiswa = (VoKelasPunyaSiswa) data;

			CommonMedia.tampilkanGambarKecil(kelasSiswaPunyaSiswa.getSiswa()).setParent(row);

			new Label(kelasSiswaPunyaSiswa.getSiswa().getNomorInduk()).setParent(row);
			new Label(kelasSiswaPunyaSiswa.getSiswa().getNama()).setParent(row);
			new Label(kelasSiswaPunyaSiswa.getKeterangan()).setParent(row);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// Session session = HibernateUtil.currentSession();
					for (Pertemuan pertemuan : pertemuans) {
						if (pertemuan.getAktif()) {
							if (mulai <= pertemuan.getPertemuanKe() && sampai >= pertemuan.getPertemuanKe()) {
								String kode = pertemuan.retreiveAbsensiKode(kelasSiswaPunyaSiswa.getSiswa().getId());

								if (kode == null) {
									new Label("-").setParent(row);
								} else {
									new Label(kode).setParent(row);
								}
							}
						}
					}
				}
			}, "Sedang mempersiapkan data absensi.. harap menunggu..");

		}

	}

	private int mulai;
	private int sampai;
	private EventListener refrehEven;
	private MyGrid grid;

	/**
	 * Membangun satu grid matriks absensi untuk rentang kolom pertemuan {@code mulai}..{@code
	 * sampai} (dipakai per-tab saat total pertemuan &gt; 16). Menyusun kolom Foto/NIM-NIS/Nama/
	 * Ket. lalu satu kolom tombol per pertemuan aktif dalam rentang tersebut (tooltip berisi
	 * ringkasan pertemuan: tanggal, topik, metode, daftar peserta online). Diakhiri dengan
	 * memanggil {@link #reload()} untuk mengisi baris data sesuai jenis {@link VOPembelajaran}.
	 *
	 * @param value       komponen induk (di-cast dari {@link Component}) tempat grid ditempel
	 * @param pertemuanss peta label&rarr;id pertemuan milik {@link #voPembelajaran}
	 * @param mulai       nomor pertemuan awal rentang kolom yang ditampilkan
	 * @param sampai      nomor pertemuan akhir rentang kolom yang ditampilkan
	 * @param refresh     bila {@code true}, muat ulang entitas {@link Pertemuan} dari database sebelum dirender (bukan dari cache)
	 */
	public void loadData(Object value, TreeMap<String, Long> pertemuanss, int mulai, int sampai, boolean refresh) {
		this.mulai = mulai;
		this.sampai = sampai;

		Component parent = (Component) value;
		Common.clear(parent);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(parent);
		grid.setSclass("fgrid");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(voPembelajaran instanceof JadwalPelajaran ? "NIS" : "NIM");
		column.setWidth("10%");
		pertemuans = new ArrayList<Pertemuan>();
		try {

			for (Long pertemuanid : pertemuanss.values()) {
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null) {
					pertemuans.add(pertemuan);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailpertemuanHelper.java:241");
		}

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ket.");

		Session session = HibernateUtil.currentSession();
		for (final Pertemuan pertemuan : pertemuans) {
			try {

				if (refresh) {
					session.refresh(pertemuan);
				}

				if (mulai <= pertemuan.getPertemuanKe() && sampai >= pertemuan.getPertemuanKe()) {
					column = new MyColumnConfig();
					column.setParent(columns);
					Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("ke-" + pertemuan.getPertemuanKe());
					button.setWidth("100%");
					button.setStyle("font-size:8px;font-family:Arial;color:red;");

					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							PertemuanHelper tambahPertemuanHelper = new PertemuanHelper(null, null);
							tambahPertemuanHelper.display(pertemuan, new DataLoader() {

								@Override
								public void loadData(Object value) {
									try {
										refrehEven.onEvent(null);
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailpertemuanHelper.java:281");
									}

								}
							}, 0);
						}

					});

					Vbox vbox = new Vbox();
					vbox.setParent(column);
					button.setParent(vbox);

					Toolbarbutton online;
					String onl = "";
					try {
						online = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, vbox, true, true,
								new EventListener() {

									@Override
									public void onEvent(Event a) throws Exception {
										try {
											refrehEven.onEvent(null);
										} catch (Exception e) {
											// TODO Auto-generated catch block
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailpertemuanHelper.java:306");
										}
									}
								});
						int jumlah = 0;
						TreeMap<String, String> d = pertemuan.ambilData("online", null);
						if (!d.isEmpty()) {

							for (String user : d.keySet()) {
								try {
									String jam = d.get(user);
									String[] u = user.split("-");
									if (u[2].equalsIgnoreCase("Dosen") || u[2].equalsIgnoreCase("Mahasiswa")
											|| u[2].equalsIgnoreCase("Guru") || u[2].equalsIgnoreCase("Siswa")) {
										onl += onl.isEmpty() ? u[0] + " (" + jam + ")" : "," + u[0] + " (" + jam + ")";
										jumlah++;
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailpertemuanHelper.java:324");
								}
							}

						}

						online.setImage(null);
						online.setLabel("Online-" + jumlah);
						online.setWidth("100%");
						online.setStyle("font-size:7px;font-family:Arial;color:red;");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailpertemuanHelper.java:336");
					}

					column.setTooltiptext("Pertemuan ke-" + pertemuan.getPertemuanKe() + " - "
							+ (pertemuan.getTanggal() == null ? "" : dateFormat.format(pertemuan.getTanggal())) + " - "
							+ (pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getNama()) + " - Materi : "
							+ pertemuan.getTopik() + " - Metode : " + pertemuan.getMetodePembelajaran() + ", Online : "
							+ onl);

					vbox.appendChild(new MyLabelAgakKecil((pertemuan.getTanggal() == null ? ""
							: Common.simpleDateFormat2.get().format(pertemuan.getTanggal()))));
					column.setWidth("4%");
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailpertemuanHelper.java:350");
			}
		}

		reload();

	}

	/**
	 * Mengisi baris data grid sesuai jenis {@link #voPembelajaran}: untuk {@link Perkuliahan},
	 * baris berupa gabungan dosen pengampu (id semu negatif) dan mahasiswa peserta — terbatas ke
	 * anak dari orang tua bila user login adalah orang tua, atau anak sendiri bila mahasiswa,
	 * atau hasil pencarian nama/NIM bila dosen/admin; untuk {@link
	 * ais.database.model.sekolah.JadwalPelajaran}, baris berupa anggota kelas siswa atau kelas
	 * les, difilter sama (nama/NIS, atau anak siswa bila orang tua) dan disaring ulang terhadap
	 * mata pelajaran jadwal via {@code KelasSiswaPunyaSiswa.filterMk}.
	 */
	@SuppressWarnings("unchecked")
	private void reload() {
		if (voPembelajaran instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
			List<Long> detailperkuliahan = new ArrayList<Long>();

			for (Long idDosen : perkuliahan.populateDosenBuId()) {
				detailperkuliahan.add(idDosen * -1L);
			}

			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && tbmuser.getOrangTua() != null
					&& !tbmuser.getOrangTua().ambilAnakMahasiswa().isEmpty()) {
				List<Mahasiswa> es = tbmuser.getOrangTua().ambilAnakMahasiswaObject();
				for (Mahasiswa mahasiswa : es) {
					Long e = perkuliahan.ambilDetailperkuliahan(mahasiswa);
					if (e != null) {
						detailperkuliahan.add(e);
					}
				}
			} else if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
				detailperkuliahan
						.addAll(perkuliahan.ambilDetailperkuliahan(null, null, nama.getValue().trim(), true, false));
			} else if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				Long e = perkuliahan.ambilDetailperkuliahan(tbmuser.getMahasiswa());
				if (e != null) {
					detailperkuliahan.add(e);
				}
			}
			System.out.println("detailperkuliahan -> " + detailperkuliahan.size());
			ListModel strset = new SimpleListModel(detailperkuliahan);
			grid.setRowRenderer(new DetailPerkuliahanRenderer(mulai, sampai));
			grid.setModelCheckMobile(strset);
		} else if (voPembelajaran instanceof JadwalPelajaran) {
			JadwalPelajaran jadwal = (JadwalPelajaran) voPembelajaran;
			if (jadwal.getKelas() != null) {
				Session session = HibernateUtil.currentSession();
				Criteria criteria = session.createCriteria(KelasSiswaPunyaSiswa.class)

						.add(Restrictions.eq("kelasSiswa", jadwal.getKelas()))

						.createAlias("siswa", "siswa")

						.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										Restrictions.ilike("siswa.namaSiswa", nama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("siswa.nomorInduk", nama.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.ilike("siswa.nomorIndukNasional", nama.getValue().trim(),
														MatchMode.ANYWHERE))));

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getOrangTua() != null
						&& !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
					criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
				}

				criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.nomorIndukNasional"))
						.addOrder(Order.desc("siswa.id"));

				List<? extends VoKelasPunyaSiswa> siswa = ConstantValues.simpleList(criteria, KelasSiswaPunyaSiswa.class);
				ListModel strset = new SimpleListModel(KelasSiswaPunyaSiswa.filterMk(siswa, jadwal.getMatapelajaran()));
				grid.setRowRenderer(new KelasSiswaRenderer(mulai, sampai));
				grid.setModelCheckMobile(strset);
			} else if (jadwal.getKelasLesSiswa() != null) {
				Session session = HibernateUtil.currentSession();
				Criteria criteria = session.createCriteria(KelasLesSiswaPunyaSiswa.class)

						.add(Restrictions.eq("kelasLesSiswa", jadwal.getKelasLesSiswa()))

						.createAlias("siswa", "siswa")

						.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										Restrictions.ilike("siswa.namaSiswa", nama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("siswa.nomorInduk", nama.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.ilike("siswa.nomorIndukNasional", nama.getValue().trim(),
														MatchMode.ANYWHERE))));

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getOrangTua() != null
						&& !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
					criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
				}

				criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.nomorIndukNasional"))
						.addOrder(Order.desc("siswa.id"));

				List<? extends VoKelasPunyaSiswa> siswa = criteria.list();
				ListModel strset = new SimpleListModel(KelasSiswaPunyaSiswa.filterMk(siswa, jadwal.getMatapelajaran()));
				grid.setRowRenderer(new KelasSiswaRenderer(mulai, sampai));
				grid.setModelCheckMobile(strset);
			}
		}
	}

	private DataLoader getDataloader() {
		return this;
	}

	private TreeMap<String, Long> pertemuanss;

	/**
	 * Titik masuk utama: membangun seluruh UI detail pertemuan untuk {@code voPembelajaran} di
	 * dalam {@code cc} — toolbar pencarian peserta, tombol unduh/unggah Excel absensi, tombol
	 * Refresh, Agenda Pertemuan, dan (khusus perkuliahan, hanya untuk dosen/admin non-mahasiswa/
	 * siswa) laporan Absensi/UTS/UAS — diikuti grid matriks absensi yang dipecah ke beberapa tab
	 * bila jumlah pertemuan lebih dari 16.
	 *
	 * @param voPembelajaran perkuliahan atau jadwal pelajaran yang detail pertemuannya ditampilkan
	 * @param cc             komponen induk (dibersihkan lebih dulu)
	 */
	public void displayDetailPertemuan(final VOPembelajaran voPembelajaran, final Component cc) {
		this.voPembelajaran = voPembelajaran;

		pertemuanss = voPembelajaran.ambilPertemuan(refreshData);

		Common.clear(cc);
		Component component = cc;

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 40px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Peserta : ")));
		toolbar.appendChild(nama = new Textbox());
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}

		});
		button.setParent(toolbar);

		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		});

		MyToolbarbuttonConfig download = new MyToolbarbuttonConfig("Download", "/img/excel.png");
		download.setParent(toolbar);
		download.setVisible(!Common.isMobile());
		download.setTooltiptext("Download");
		download.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_absen_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

				File file;
				(file = new File(filename)).createNewFile();
				final Intbox sizedata = new Intbox(30);
				final Label label = Common
						.displayLoadBar(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot(), file);

				new Thread(new Runnable() {

					@Override
					public void run() {

						XSSFWorkbook workbook = new XSSFWorkbook();
						XSSFSheet sheet = workbook.createSheet("Absensi");
						sheet.setDefaultColumnWidth(20);
						int rowIndex = 0;

						XSSFRow rowhead1 = sheet.createRow((short) 0);

						rowhead1.createCell(0).setCellValue("");

						XSSFRow rowhead = sheet.createRow((short) 1);

						rowhead.createCell(0).setCellValue("Kode");
						rowhead.createCell(1).setCellValue("Nama");

						pertemuans = new ArrayList<Pertemuan>();
						try {
							TreeMap<String, Long> pertemuanss = voPembelajaran.ambilPertemuan();
							for (Long pertemuanid : pertemuanss.values()) {
								Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
										pertemuanid.toString());
								if (pertemuan != null) {
									pertemuans.add(pertemuan);
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailpertemuanHelper.java:547");
						}

						int i = 2;
						for (Pertemuan pertemuan : pertemuans) {
							if (pertemuan.getAktif()) {
								rowhead1.createCell(i).setCellValue(pertemuan.getId());
								rowhead.createCell(i).setCellValue(
										"Ke-" + (i - 1) + " " + (pertemuan.getStatusPertemuan() == null ? ""
												: pertemuan.getStatusPertemuan().getNama()));
								i++;
							}
						}

						boolean ss = voPembelajaran instanceof JadwalPelajaran;

						List<Long> mahasiswas = ss ? voPembelajaran.ambilSiswaById()
								: voPembelajaran.ambilMahasiswaById();
						rowIndex = 2;
						for (Long mahasiswa : mahasiswas) {

							label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
									+ Common.numberFormat.get().format(rowIndex * 100.0 / mahasiswas.size()) + " %)");
							XSSFRow row = sheet.createRow(rowIndex);

							if (ss) {
								Siswa m = (Siswa) ConstantValues.ambil(Siswa.class.getName(), mahasiswa);
								row.createCell(0).setCellValue(m == null ? "" : m.getNim());
								row.createCell(1).setCellValue(m == null ? "" : m.getNama());
							} else {

								Mahasiswa m = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), mahasiswa);
								row.createCell(0).setCellValue(m == null ? "" : m.getNim());
								row.createCell(1).setCellValue(m == null ? "" : m.getNama());
							}

							i = 2;
							for (Pertemuan pertemuan : pertemuans) {
								if (pertemuan.getAktif()) {
									Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(
											Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(mahasiswa));
									XSSFCell c = row.createCell(i);
									c.setCellValue(statusabsensi == null ? "" : statusabsensi.getKode());
									i++;
								}

							}
							rowIndex++;
						}

						Common.setStyled(sheet);
						sizedata.setValue(rowIndex + 1);

						try {
							FileOutputStream fileOut = new FileOutputStream(filename);
							workbook.write(fileOut);
							fileOut.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}

						System.out.println("Your excel file has been generated! ");

						label.setValue("");
					}
				}).start();

			}
		});

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.setTooltiptext("Alur: klik Download untuk mengambil template, isi kolom pertemuan dengan KODE "
				+ "status absen (contoh: M), lalu upload kembali file yang sama (format xlsx). "
				+ "Dua baris judul paling atas jangan diubah/dihapus.");
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " +
					// file.getAbsolutePath());
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

							final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data absen .."));
							/* statistik[0]=sel tersimpan, [1]=sel dilewati, [2]=baris gagal */
							final int[] statistik = new int[3];
							final StringBuilder infoGagal = new StringBuilder();

							Clients.showBusy(label.getValue());
							final Timer timer = new Timer(200);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							timer.setRepeats(true);
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.showBusy(label.getValue());
									if (label.getValue().isEmpty()) {
										Clients.clearBusy();
										timer.detach();
										/* Umpan balik jujur: tampilkan jumlah yang benar-benar
										 * tersimpan, bukan selalu "berhasil" (dulu pesan sukses
										 * muncul walau tidak ada satu pun data tersimpan). */
										if (statistik[0] > 0) {
											MyMessageboxConfig.show("Upload absen selesai: " + statistik[0]
													+ " status kehadiran tersimpan"
													+ (statistik[1] > 0 ? ", " + statistik[1]
															+ " sel dilewati (kosong/kode tidak dikenal)" : "")
													+ (statistik[2] > 0 ? ", " + statistik[2] + " baris gagal." : "."),
													"Pemberitahuan", MyMessageboxConfig.OK,
													MyMessageboxConfig.INFORMATION, refrehEven);
										} else {
											MyMessageboxConfig.show(
													"Tidak ada status kehadiran yang tersimpan. Pastikan memakai file template hasil tombol Download (baris judul jangan diubah/dihapus) dan isi sel dengan KODE status absen (contoh: M)."
															+ (infoGagal.length() == 0 ? ""
																	: " Info teknis: " + infoGagal),
													"Pemberitahuan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION, refrehEven);
										}
									}

								}
							});
							timer.start();

							new Thread(new Runnable() {

								@Override
								public void run() {
									Session session = null;
									try {

										XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
										XSSFSheet sheet = workbook.getSheetAt(0);

										/* Session milik thread ini sendiri. Pola lama memakai
										 * currentNativeSession + session.refresh(pertemuan) pada
										 * instance cache milik session LAIN, sehingga exception
										 * lintas-session tertelan (hanya tampil ke admin) dan
										 * data tidak pernah berubah walau pesan sukses muncul. */
										session = HibernateUtil.openSession();

										boolean ss = voPembelajaran instanceof JadwalPelajaran;

										/* Peta kolom -> id pertemuan dibaca dari BARIS-0 template
										 * (ditulis tombol Download). Kebal terhadap perubahan
										 * urutan/penambahan pertemuan di antara download-upload,
										 * tidak lagi mengandalkan urutan list di memori. */
										java.util.LinkedHashMap<Integer, Long> kolomPertemuan = new java.util.LinkedHashMap<Integer, Long>();
										XSSFRow rowId = sheet.getRow(0);
										if (rowId != null) {
											for (int c = 2; c <= rowId.getLastCellNum(); c++) {
												try {
												String idStr = Common.getCellContent(rowId.getCell(c));
												String idNormal = idStr == null ? "" : idStr.trim().replace(",", ".");
												if (idNormal.matches("[0-9]+(?:\\.[0-9]+)?")) {
													kolomPertemuan.put(Integer.valueOf(c),
															Long.valueOf(Double.valueOf(idNormal).longValue()));
												}
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailpertemuanHelper.java:728");
												}
											}
										}
										if (kolomPertemuan.isEmpty()) {
											infoGagal.append(
													"baris pertama template (id pertemuan) tidak ditemukan di file;");
										}

										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 2; i < rowCount; i++) {
											try {

												Mahasiswa mahasiswa = null;
												Siswa siswa = null;
												if (ss) {
													siswa = (Siswa) Common.getSheetContentAsObject(sheet, 0, i,
															Siswa.class);
												} else {
													mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 0, i,
															Mahasiswa.class);
												}

												if (mahasiswa == null && siswa == null) {
													continue;
												}

												Long refId = ss ? siswa.getId() : mahasiswa.getId();
												String namaPeserta = ss ? siswa.getNama() : mahasiswa.getNama();

												for (java.util.Map.Entry<Integer, Long> entry : kolomPertemuan
														.entrySet()) {
													XSSFRow dataRow = sheet.getRow(i);
													String kode = dataRow == null ? ""
															: Common.getCellContent(
																	dataRow.getCell(entry.getKey().intValue()));
													if (kode == null || kode.trim().isEmpty()) {
														continue;
													}
													Statusabsensi statusabsensi = (Statusabsensi) Common
															.getSheetContentAsObject(sheet, entry.getKey().intValue(),
																	i, Statusabsensi.class);
													if (statusabsensi == null) {
														statistik[1]++;
														continue;
													}

													session.getTransaction().begin();
													/* Pertemuan dimuat SEGAR by-id di session thread
													 * ini - bukan refresh instance lintas-session. */
													Pertemuan pertemuan = (Pertemuan) session.get(Pertemuan.class,
															entry.getValue());
													if (pertemuan == null || pertemuan.getAktif() == null
															|| !pertemuan.getAktif()) {
														statistik[1]++;
														session.getTransaction().rollback();
														continue;
													}
													String keterangan = pertemuan.retreiveAbsensiKeterangan(refId);
													String waktuMulai = pertemuan.retreiveAbsensiMulai(refId);
													String waktuSelesai = pertemuan.retreiveAbsensiSampai(refId);
													pertemuan.populate(refId, statusabsensi, keterangan, null,
															waktuMulai, waktuSelesai, ss ? "Siswa" : "Mahasiswa");
													session.update(pertemuan);
													session.getTransaction().commit();
													statistik[0]++;
												}

												label.setValue("Upload data \"" + namaPeserta + "\" ("
														+ Common.numberFormat.get().format(i * 100.0 / rowCount)
														+ " %)");

											} catch (Exception e) {
												statistik[2]++;
												if (infoGagal.length() < 300) {
													infoGagal.append(
															e.getMessage() == null ? e.toString() : e.getMessage())
															.append("; ");
												}
												try {
													if (session.getTransaction() != null
															&& session.getTransaction().isActive()) {
														session.getTransaction().rollback();
													}
												} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/DetailpertemuanHelper.java:812");
												}
												Common.tampilErrorJikaAdmin(e);
											}
										}
									} catch (Exception e1) {
										statistik[2]++;
										if (infoGagal.length() < 300) {
											infoGagal.append(
													e1.getMessage() == null ? e1.toString() : e1.getMessage());
										}
										Common.tampilErrorJikaAdmin(e1);
									} finally {
										/* Akhiri transaksi implisit + tutup session thread ini
										 * (mencegah koneksi pool "idle in transaction"). */
										ais.ui.util.AmbilDataPagingHelper.tutupSessionQuietly(session);
										label.setValue("");
									}
								}
							}).start();

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
		toolbar.appendChild(upload);

		refrehEven = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						refreshData = true;
						displayDetailPertemuan(voPembelajaran, cc);
					}
				});
			}
		};

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.addEventListener("onClick", refrehEven);
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Agenda Pertemuan", "/img/jadwal.png");
		button.setTooltiptext("Lakukan penjadwalan absensi");
		// button.setVisible(tbmuser != null && tbmuser == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (voPembelajaran instanceof Perkuliahan) {
					PenjadwalanHelper tambahPertemuanHelper = new PenjadwalanHelper();
					tambahPertemuanHelper.display((Perkuliahan) voPembelajaran, getDataloader());
				} else if (voPembelajaran instanceof JadwalPelajaran) {
					PenjadwalanSiswaHelper tambahPertemuanHelper = new PenjadwalanSiswaHelper();
					tambahPertemuanHelper.display((JadwalPelajaran) voPembelajaran, getDataloader());
				}
			}

		});
		button.setParent(toolbar);

		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null) {
			if (voPembelajaran instanceof Perkuliahan) {
				button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						if (voPembelajaran instanceof Perkuliahan) {
							CommonReportHelper.onLaporanAbsensi((Perkuliahan) voPembelajaran, true);
						} else {

						}
					}

				});
				button.setParent(toolbar);
			}

			if (voPembelajaran instanceof Perkuliahan) {
				button = new MyToolbarbuttonConfig("UTS", "/img/print.png");
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						CommonReportHelper.onLaporanAbsensi((Perkuliahan) voPembelajaran, "UTS");

					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("UAS", "/img/print.png");
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						CommonReportHelper.onLaporanAbsensi((Perkuliahan) voPembelajaran, "UAS");
					}

				});
				button.setParent(toolbar);
			}
		}

		TreeMap<String, Long> pertemuanss = voPembelajaran.ambilPertemuan();

		if (pertemuanss.size() > 16) {
			// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab
			// paginasi "Pertemuan ke X sd Y" ini data-driven (jumlahnya tergantung total
			// pertemuan), sama seperti pola "Ke-1".."Ke-N" di SetingBiayaAction yang
			// sebelumnya bermasalah blank/scroll pakai Tab/Tabpanel bawaan ZK. Konten
			// tetap eager seperti semula.
			ais.ui.util.MyButtonTabbox tabboxPertemuan = ais.ui.util.MyButtonTabbox.buat(component, "100%", null);
			int size = pertemuanss.size();
			int indexTab = 1;
			for (int i = 1; i <= pertemuanss.size(); i += 16) {

				org.zkoss.zul.Div panelPertemuan = tabboxPertemuan.tambahTab(indexTab,
						Common.getBahasaConfig("Pertemuan ke") + " " + i
								+ (size < (i + 15) ? " ke atas" : " sd " + (i + 15)));

				loadData(panelPertemuan, pertemuanss, i, i + 15, refreshData);
				indexTab++;
			}
			tabboxPertemuan.pilih(1);
		} else {
			Div div = new Div();
			groupbox.appendChild(div);
			loadData(div, pertemuanss, 1, 16, refreshData);
		}

	}

	private boolean refreshData = false;

	/** Implementasi {@link DataLoader#loadData}: memuat ulang grid tab aktif dengan rentang kolom dan flag refresh terakhir, lalu mereset flag refresh. */
	@Override
	public void loadData(Object value) {
		loadData(value, pertemuanss, mulai, sampai, refreshData);
		refreshData = false;
	}

}
