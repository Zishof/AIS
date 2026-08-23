package ais.action.master;

import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.maintenance.LoginAlumniAction;
import ais.action.master.helper.AmbilDataDosenSkripsiBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.ParameterTambahanAlumniListener;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanTranskipAkademik;
import ais.common.ChecklistPenilaianHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailKegiatan;
import ais.database.model.Dosen;
import ais.database.model.ItemBiaya;
import ais.database.model.Judisium;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.QuotaWisudaUntukFakultas;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.Wisuda;
import ais.database.model.file.LampiranLain;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHboxToolbar;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class PendaftaranWisudaMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3254720096686026528L;
	private Mahasiswa mahasiswa;
	private Skripsi skripsi;
	private PendaftaranWisuda pendaftaranWisuda;

	private Combobox ukuranToga;
	private Combobox wisudaKe;
	Label searchstatusdaftarwisuda;

	MyWindow win;
	// private Tbmuser tbmuser;
	private AmbilDataMahasiswaBanbox bandboxMahasiswa;

	private String label_skripsi;
	private MyDatebox tanggalDaftarWisuda;

	private LampiranLain lainMahasiswa = null;

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

		label_skripsi = Common.getKonfigurasi("label_skripsi", "skripsi").getNilai();
		Tbmuser tbmuser = Common.getCurrentUser();

		win = (MyWindow) comp;
		if (execution.getParameter("mahasiswa") != null) {
			mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(),
					Long.parseLong(execution.getParameter("mahasiswa")));
		} else {
			mahasiswa = tbmuser.getMahasiswa();
		}

		loadDataMahasiswa(mahasiswa);

	}

	public void onDataSkripsi(Event event) throws Exception {

		mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");

		Session session = HibernateUtil.currentSession();
		skripsi = (Skripsi) session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.setMaxResults(1).uniqueResult();
		if (skripsi == null) {
			skripsi = new Skripsi();
		}

		SkripsiAction.onAddExternal(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				skripsi = (Skripsi) arg0.getData();
				mahasiswa = skripsi.getMahasiswa();
				Session session = HibernateUtil.currentSession();
				pendaftaranWisuda = (PendaftaranWisuda) session.createCriteria(PendaftaranWisuda.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

				if (pendaftaranWisuda == null) {
					pendaftaranWisuda = new PendaftaranWisuda();
				}

				init(skripsi, pendaftaranWisuda);

			}
		}, skripsi, mahasiswa);
	}

	public void onDataTracer(Event event) throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null || tbmuser.getMahasiswa() == null) {
			MyMessageboxConfig.show("Mohon maaf, fitur ini hanya dapat diakses setelah Bapak/Ibu masuk (login) sebagai mahasiswa atau alumni. Silakan masuk terlebih dahulu menggunakan akun mahasiswa atau alumni, kemudian ulangi proses ini.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					});
			return;
		}

		mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");

		Session session = HibernateUtil.currentSession();
		skripsi = (Skripsi) session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.setMaxResults(1).uniqueResult();
		if (skripsi == null) {
			MyMessageboxConfig.showFormatCb("Mohon maaf, data \"{V1}\" belum lengkap dan harus dilengkapi terlebih dahulu. Langkah yang dapat dilakukan: (1) Tekan tombol \"OK\" untuk membuka formulir \"{V1}\"; (2) Lengkapi seluruh isian yang diperlukan; (3) Simpan data, kemudian ulangi proses ini.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onDataSkripsi(arg0);
						}
					}, label_skripsi, label_skripsi);
			return;
		}

		LoginAlumniAction.onAddExternal(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				mahasiswa = skripsi.getMahasiswa();
				Session session = HibernateUtil.currentSession();
				pendaftaranWisuda = (PendaftaranWisuda) session.createCriteria(PendaftaranWisuda.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

				if (pendaftaranWisuda == null) {
					pendaftaranWisuda = new PendaftaranWisuda();
				}

				init(skripsi, new PendaftaranWisuda());

			}
		}, mahasiswa);
	}

	public void loadDataMahasiswa(final Mahasiswa mahasiswa) throws InterruptedException {
		win.getFellowIfAny("window");
		Common.clear(win);
		this.mahasiswa = mahasiswa;
		if (mahasiswa != null) {
			Session session = HibernateUtil.currentSession();

			pendaftaranWisuda = (PendaftaranWisuda) session.createCriteria(PendaftaranWisuda.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

			if (Common.getKonfigurasi("data_skripsi_harus_diinput_sebelum_daftar_wisuda", Konfigurasi.AKTIF).getNilai()
					.equals(Konfigurasi.TIDAK_AKTIF)) {
				init(skripsi, pendaftaranWisuda == null ? new PendaftaranWisuda() : pendaftaranWisuda);
			} else {

				skripsi = (Skripsi) session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
						.setMaxResults(1).uniqueResult();
				if (pendaftaranWisuda != null && skripsi != null) {
					init(skripsi, pendaftaranWisuda);
					return;
				}

				if (skripsi == null) {

					MyMessageboxConfig.showFormatCb(
							"Mahasiswa dengan NIM {V1} atas nama {V2} belum dapat mendaftar wisuda dikarenakan data \"{V3}\" belum tersedia di dalam sistem.\n\nApakah Bapak/Ibu berkenan untuk memasukkan data \"{V4}\" sekarang? Tekan tombol \"OK\" untuk melengkapi data \"{V4}\", atau tombol \"Cancel\" untuk membatalkan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											SkripsiAction.onAddExternal(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

													skripsi = (Skripsi) arg0.getData();
													PendaftaranWisudaMahasiswaAction.this.mahasiswa = skripsi
															.getMahasiswa();

													init(skripsi, new PendaftaranWisuda());

												}
											}, new Skripsi(), mahasiswa);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}

									} else {
										init(skripsi, new PendaftaranWisuda());
									}

								}
							}, mahasiswa.getNim(), mahasiswa.getNama(), label_skripsi, label_skripsi);

					return;
				}

				else if (skripsi != null && pendaftaranWisuda == null) {
					init(skripsi, new PendaftaranWisuda());
				}
			}
		} else {
			init(skripsi, pendaftaranWisuda);
		}

	}

	@SuppressWarnings("deprecation")
	private void init(final Skripsi skripsi, final PendaftaranWisuda pendaftaranWisuda) throws InterruptedException {

		ukuranToga = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("S");
		comboitem.setValue(1);
		ukuranToga.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("M");
		comboitem.setValue(2);
		ukuranToga.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("L");
		comboitem.setValue(3);
		ukuranToga.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("XL");
		comboitem.setValue(4);
		ukuranToga.appendChild(comboitem);

		Common.insertCombo(wisudaKe = new Combobox(),
				new String[] { "wisudaKe", "moto", "keterangan", "maksimalQuota" }, Wisuda.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		this.skripsi = skripsi;
		this.pendaftaranWisuda = pendaftaranWisuda;

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(win);
		borderlayout.setStyle("border:0px;");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setWidth("75%");
		columns.appendChild(column);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa")));
		row.appendChild(bandboxMahasiswa = new AmbilDataMahasiswaBanbox());
		bandboxMahasiswa.setWidth("90%");

		if (Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() != null
				|| this.mahasiswa != null) {
			Mahasiswa mahasiswa = Common.getCurrentUser().getMahasiswa() == null ? this.mahasiswa
					: Common.getCurrentUser().getMahasiswa();
			bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
			bandboxMahasiswa.setAttribute("myValue", mahasiswa);
			bandboxMahasiswa.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			// bandboxMahasiswa.setId("mhs_" + mahasiswa.getId());
			// bandboxMahasiswa.setDisabled(true);
		}
		bandboxMahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataMahasiswa((Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa"));

			}
		});

		if (execution.getParameter("mahasiswa") == null) {

		} else {
			bandboxMahasiswa.setDisabled(true);
		}

		if (mahasiswa == null) {
			return;
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(new Label(mahasiswa.getNim()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(new Label(mahasiswa.getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(new Label(mahasiswa.getJurusan().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(new Label(mahasiswa.getJurusan().getFakultas().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(new Label(mahasiswa.getTahunangkatan() + " (" + mahasiswa.getSemesterMulai() + ")"));

		Tbmuser tbmuser = Common.getCurrentUser();

		if (skripsi != null) {

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(
					"Judul " + Common.getKonfigurasi("label_skripsi", "skripsi").getNilai()));
			row.appendChild(new Label(skripsi.getJudul() == null ? "" : skripsi.getJudul()));

			final Textbox judul = new Textbox(skripsi.getJudul());
			judul.setWidth("90%");
			judul.setRows(3);
			judul.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					skripsi.setJudul(judul.getValue());
					Common.refreshUpdate(skripsi);
				}
			});

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pembimbing 1"));

			final AmbilDataDosenSkripsiBanbox pembimbing = new AmbilDataDosenSkripsiBanbox();

			if ((tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) || Common.bolehKonfigurasi("saat_daftar_sidang_mahasiswa_bisa_menentukan_pembimbing", Konfigurasi.TIDAK_AKTIF)) {
				row.appendChild(pembimbing);
			} else {
				row.appendChild(new Label(skripsi.getPembimbing() == null ? "" : skripsi.getPembimbing().getNama()));
			}

			pembimbing.setValue(skripsi.getPembimbing() == null ? "" : skripsi.getPembimbing().getNama());
			pembimbing.setAttribute("myValue", skripsi.getPembimbing());
			pembimbing.setAttribute("dosen", skripsi.getPembimbing());
			pembimbing.setWidth("90%");
			pembimbing.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					skripsi.setPembimbing((Dosen) pembimbing.getAttribute("dosen"));
					Common.refreshUpdate(skripsi);
				}
			});

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pembimbing 2"));

			final AmbilDataDosenSkripsiBanbox ketuaSidang = new AmbilDataDosenSkripsiBanbox();
			if ((tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) || Common.bolehKonfigurasi("saat_daftar_sidang_mahasiswa_bisa_menentukan_pembimbing", Konfigurasi.TIDAK_AKTIF)) {
				row.appendChild(ketuaSidang);
			} else {
				row.appendChild(new Label(skripsi.getKetuaSidang() == null ? "" : skripsi.getKetuaSidang().getNama()));
			}
			ketuaSidang.setValue(skripsi.getKetuaSidang() == null ? "" : skripsi.getKetuaSidang().getNama());
			ketuaSidang.setAttribute("myValue", skripsi.getKetuaSidang());
			ketuaSidang.setAttribute("dosen", skripsi.getKetuaSidang());
			ketuaSidang.setWidth("90%");

			ketuaSidang.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					skripsi.setKetuaSidang((Dosen) ketuaSidang.getAttribute("dosen"));
					Common.refreshUpdate(skripsi);
				}
			});

			// row = new MyFormRow();
			//			// row.setParent(rows);
			// row.appendChild(new ais.ui.util.MyLabelConfig("Pembimbing III"));
			// if (skripsi != null) {
			// final AmbilDataDosenSkripsiBanbox pembimbing3 = new
			// AmbilDataDosenSkripsiBanbox();
			// if ((tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa()
			// == null) || Common
			// .getKonfigurasi("saat_daftar_sidang_mahasiswa_bisa_menentukan_pembimbing",
			// Konfigurasi.TIDAK_AKTIF)
			// .getNilai().equalsIgnoreCase(Konfigurasi.AKTIF)) {
			// row.appendChild(pembimbing3);
			// } else {
			// row.appendChild(new Label(skripsi.getPembimbing3() == null ? "" :
			// skripsi.getPembimbing3().getNama()));
			// }
			// pembimbing3.setValue(skripsi.getPembimbing3() == null ? "" :
			// skripsi.getPembimbing3().getNama());
			// pembimbing3.setAttribute("myValue", skripsi.getPembimbing3());
			// pembimbing3.setAttribute("dosen", skripsi.getPembimbing3());
			// pembimbing3.setWidth("90%");
			//
			// pembimbing3.setEventListener(new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// skripsi.setPembimbing3((Dosen) pembimbing3.getAttribute("dosen"));
			// Common.refreshUpdate(skripsi);
			// }
			// });
			// }

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Penguji 1"));
			row.appendChild(new Label(skripsi.getPenguji1() == null || skripsi.getPenguji1().getNama() == null ? ""
					: skripsi.getPenguji1().getNama()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Penguji 2"));
			row.appendChild(new Label(skripsi.getPenguji2() == null || skripsi.getPenguji2().getNama() == null ? ""
					: skripsi.getPenguji2().getNama()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Penguji 3"));
			row.appendChild(new Label(skripsi.getPenguji3() == null || skripsi.getPenguji3().getNama() == null ? ""
					: skripsi.getPenguji3().getNama()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Penguji 4"));
			row.appendChild(new Label(skripsi.getPenguji4() == null || skripsi.getPenguji4().getNama() == null ? ""
					: skripsi.getPenguji4().getNama()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Total"));
			row.appendChild(new Label(Common.numberFormat.get().format(skripsi.getTotalNilai())));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Huruf"));
			row.appendChild(new Label(skripsi.getNilaiHuruf() == null ? "" : skripsi.getNilaiHuruf()));

		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Pendaftaran Wisuda"));
		row.appendChild(searchstatusdaftarwisuda = new Label(
				pendaftaranWisuda.getId() == null ? "Belum Terdaftar Wisuda" : "Sudah Terdaftar Wisuda"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Bagian Keuangan"));
		row.appendChild(new Label(
				pendaftaranWisuda.getStatusPersetujuanKeuangan().equals(0) ? "Belum Menyetujui" : "Sudah Menyetujui"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Bagian Administrasi"));
		row.appendChild(new Label(pendaftaranWisuda.getStatusPersetujuanAdministrasi().equals(0) ? "Belum Menyetujui"
				: "Sudah Menyetujui"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Bagian Administrasi " + "Fakultas"));
		row.appendChild(
				new Label(pendaftaranWisuda.getStatusPersetujuanAdministrasiFakultas().equals(0) ? "Belum Menyetujui"
						: "Sudah Menyetujui"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Bagian Perpustakaan"));
		row.appendChild(new Label(pendaftaranWisuda.getStatusPersetujuanPerpustakaan().equals(0) ? "Belum Menyetujui"
				: "Sudah Menyetujui"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Bagian Perpustakaan " + "Fakultas"));
		row.appendChild(
				new Label(pendaftaranWisuda.getStatusPersetujuanPerpustakaanFakultas().equals(0) ? "Belum Menyetujui"
						: "Sudah Menyetujui"));

		// ayu
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ukuran Toga"));
		Common.selectComboItem(ukuranToga, pendaftaranWisuda.getUkuranToga());
		row.appendChild(ukuranToga);

		ukuranToga.setReadonly(true);
		if (ukuranToga.getSelectedItem() == null && ukuranToga.getChildren().size() > 0) {
			ukuranToga.setSelectedIndex(0);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Wisuda Ke"));
		Common.selectComboItem(wisudaKe, pendaftaranWisuda.getWisuda());
		row.appendChild(wisudaKe);

		wisudaKe.setReadonly(true);
		if (wisudaKe.getSelectedItem() == null && wisudaKe.getChildren().size() > 0) {
			wisudaKe.setSelectedIndex(0);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pendaftaran"));
		row.appendChild(tanggalDaftarWisuda = new MyDatebox(pendaftaranWisuda.getTanggalDaftarWisuda()));

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Form Persyaratan Wisuda"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pendaftaranWisuda.getId(), PendaftaranWisuda.class.getName(),
				"Form Persyaratan Wisuda", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows,
				"Jika file Form Persyaratan Wisuda lebih dari satu file, zip dulu semua file tersebut");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		MyHboxToolbar toolbar = new MyHboxToolbar();
		toolbar.setParent(row);

		final MyToolbarbutton daftar = new MyToolbarbutton("fa-graduation-cap", "Daftar Wisuda");
		final MyToolbarbutton tracer = new MyToolbarbutton("fa-check-square-o", "Tracer Study");
		final MyToolbarbutton batal = new MyToolbarbutton("fa-ban ", "Batal");

		daftar.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (ukuranToga.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, ukuran toga belum dipilih. Bapak/Ibu diharapkan memilih salah satu ukuran toga terlebih dahulu sebelum melanjutkan proses pendaftaran wisuda.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (wisudaKe.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, periode wisuda belum dipilih. Bapak/Ibu diharapkan memilih periode wisuda (Wisuda Ke) terlebih dahulu sebelum melanjutkan proses pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
					MyMessageboxConfig.show("Mohon maaf, data mahasiswa belum dipilih. Bapak/Ibu diharapkan memilih mahasiswa terlebih dahulu sebelum melanjutkan proses pendaftaran wisuda.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (!checkQuota((Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa"))) {
					return;
				}

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getMahasiswa() != null) {
					if (Common.bolehKonfigurasi("saat_pendaftaran_wisuda_harus_mengisi_tracer")) {

						List<Object[]> datas = ChecklistPenilaianHelper.getJadwalChecklistUmum(tbmuser);
						for (Object[] obj : datas) {
							final String tahunakademik = (String) obj[0];
							final String semester = (String) obj[1];
							if (ChecklistPenilaianHelper.checkStatusChecklistUmum(tahunakademik, semester, tbmuser)) {

								MyMessageboxConfig.showFormatCb(
										"Mohon maaf, data Tracer Study untuk tahun akademik {V1} semester {V2} belum lengkap. Untuk dapat melanjutkan proses pendaftaran wisuda, Bapak/Ibu diharapkan melengkapi data Tracer Study terlebih dahulu. Langkah yang dapat dilakukan: (1) Tekan tombol \"OK\" untuk membuka formulir Tracer Study; (2) Lengkapi seluruh isian yang diperlukan; (3) Simpan data, kemudian ulangi proses pendaftaran wisuda.",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
										new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												onDataTracer(arg0);
											}
										}, tahunakademik, semester);
								return;
							}
						}

						BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

						if (!ParameterTambahanAlumniListener.validate(biodataMahasiswa, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// TODO Auto-generated method stub

							}
						}, true, false)) {
							MyMessageboxConfig.show("Mohon maaf, data Tracer Study belum lengkap. Untuk dapat melanjutkan proses pendaftaran wisuda, Bapak/Ibu diharapkan melengkapi data Tracer Study terlebih dahulu. Langkah yang dapat dilakukan: (1) Tekan tombol \"OK\" untuk membuka formulir Tracer Study; (2) Lengkapi seluruh isian yang diperlukan; (3) Simpan data, kemudian ulangi proses pendaftaran wisuda.", "Informasi", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											onDataTracer(arg0);
										}
									});
							return;
						}
					}

				}
				// save data ke database
				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin melakukan pendaftaran wisuda dengan data yang telah diisi? Data pendaftaran akan disimpan ke dalam sistem. Tekan tombol \"OK\" untuk melanjutkan, atau tombol \"Cancel\" untuk membatalkan.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.CANCEL)
									return;
								else {
									onSave(event);
									daftar.setDisabled(true);
									batal.setDisabled(false);
									searchstatusdaftarwisuda.setValue("Sudah Terdaftar Wisuda");
								}

							}
						});

			}
		});
		daftar.setParent(toolbar);

		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				// save data ke database
				// onSave(event);

				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan pendaftaran wisuda ini? Data pendaftaran wisuda yang telah tersimpan akan dihapus dari sistem dan tindakan ini tidak dapat dibatalkan. Tekan tombol \"OK\" untuk melanjutkan pembatalan, atau tombol \"Cancel\" untuk tetap mempertahankan pendaftaran.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Common.refreshDelete(pendaftaranWisuda);

										// onSearchDefault(event);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig.show(Common.pesan(
												"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain di dalam sistem. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) Pastikan seluruh data yang berkaitan dengan pendaftaran ini telah dilepas atau dihapus terlebih dahulu; (2) Ulangi proses pembatalan; (3) Apabila kendala masih berlanjut, mohon menghubungi administrator sistem.",
												e.getMessage()));
									}
									daftar.setDisabled(false);
									batal.setDisabled(true);
									searchstatusdaftarwisuda.setValue("Belum Terdaftar Wisuda");
								}

							}
						});

			}
		});
		batal.setParent(toolbar);
		tracer.setVisible(Common.bolehKonfigurasi("saat_pendaftaran_wisuda_harus_mengisi_tracer"));
		tracer.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				onDataTracer(event);

			}
		});
		tracer.setParent(toolbar);

		if (skripsi != null) {
			MyToolbarbutton buttonSkripsi = new MyToolbarbutton("fa-pencil-square-o", "Ubah " + label_skripsi);
			buttonSkripsi.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onDataSkripsi(arg0);
				}
			});
			buttonSkripsi.setParent(toolbar);
		}

		MyToolbarbutton cetak = new MyToolbarbutton("fa-print", "Bukti Pendaftaran");
		cetak.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

				// Verifikasi dulu bhw referensi pendaftaranWisuda yg dipegang layar ini
				// (bisa jadi proxy basi kalau barisnya sudah dihapus proses/pengguna lain
				// sejak halaman dimuat) masih ada di DB, sebelum getId()/getNoKursi()/dsb
				// dipanggil (bisa memicu ObjectNotFoundException atau NPE saat lazy-init).
				if (!pastikanPendaftaranWisudaMasihAda(HibernateUtil.currentSession())) {
					return;
				}

				if (PendaftaranWisudaMahasiswaAction.this.pendaftaranWisuda.getId() == null) {
					MyMessageboxConfig.show("Mohon maaf, bukti pendaftaran belum dapat dicetak karena data pendaftaran wisuda belum tersimpan. Bapak/Ibu diharapkan menekan tombol \"Simpan / Daftar Wisuda\" terlebih dahulu, kemudian mencetak bukti pendaftaran.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (PendaftaranWisudaMahasiswaAction.this.pendaftaranWisuda.getNoKursi() == null
						|| PendaftaranWisudaMahasiswaAction.this.pendaftaranWisuda.getNoKursi().isEmpty()) {
					String noKursi = PendaftaranWisudaMahasiswaAction.this.pendaftaranWisuda.getId().toString();

					while (noKursi.length() < 8) {
						noKursi = "0" + noKursi;
					}

					PendaftaranWisudaMahasiswaAction.this.pendaftaranWisuda.setNoKursi(noKursi);
					Common.refreshSaveOrUpdate(PendaftaranWisudaMahasiswaAction.this.pendaftaranWisuda);
				}

				@SuppressWarnings("rawtypes")
				Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("id_mahasiswa",
						PendaftaranWisudaMahasiswaAction.this.pendaftaranWisuda.getMahasiswa().getId());
				parameters.put("id_pendaftaran_wisuda",
						PendaftaranWisudaMahasiswaAction.this.pendaftaranWisuda.getId());

				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(
						PendaftaranWisudaMahasiswaAction.this.pendaftaranWisuda.getMahasiswa());
				Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs");

				Judisium judisium = Common.hitungJudisium(
						PendaftaranWisudaMahasiswaAction.this.pendaftaranWisuda.getMahasiswa(), krsMahasiswa);
				parameters.put("judisium", judisium == null ? "" : judisium.getNama());
				parameters.put("judisium_en", judisium == null ? "" : judisium.getNamaen());

				try {
					BiodataMahasiswa biodataMahasiswa = pendaftaranWisuda.getMahasiswa().ambilBiodata();
					Common.insertProperty(BiodataMahasiswa.class, biodataMahasiswa, parameters, "bio");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PendaftaranWisudaMahasiswaAction.java:825");
					// TODO: handle exception
				}
				try {
					Common.insertProperty(Mahasiswa.class,
							PendaftaranWisudaMahasiswaAction.this.pendaftaranWisuda.getMahasiswa(), parameters, "mhs");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PendaftaranWisudaMahasiswaAction.java:831");
					// TODO: handle exception
				}
				try {
					Common.insertProperty(Skripsi.class,
							PendaftaranWisudaMahasiswaAction.this.pendaftaranWisuda.getSkripsi(), parameters,
							"skripsi");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PendaftaranWisudaMahasiswaAction.java:838");
					// TODO: handle exception
				}

				Report.generatePDFReport(Report.PDF, parameters, "kartu_daftar_wisuda",
						ais.ui.util.WaktuUtil.getDate());
			}
		});
		cetak.setParent(toolbar);

		MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-certificate", "Cek Data Transkrip dan Preview Ijazah");
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanTranskipAkademik laporanIjazahAkademik = new LaporanTranskipAkademik(mahasiswa);
				laporanIjazahAkademik.setTitle("Preview Ijazah");
				laporanIjazahAkademik.setClosable(true);
				laporanIjazahAkademik.setHeight("95%");
				laporanIjazahAkademik.setWidth("90%");
				laporanIjazahAkademik.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				laporanIjazahAkademik.onModal();
			}
		});
		toolbar.appendChild(toolbarbutton);
	}

	@SuppressWarnings("unchecked")
	private boolean checkQuota(Mahasiswa mahasiswa) throws Exception {
		this.mahasiswa = mahasiswa;
		Session session = HibernateUtil.currentSession();

		if (Common.bolehKonfigurasi("data_skripsi_harus_diinput_sebelum_daftar_wisuda")) {
			skripsi = (Skripsi) session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
					.addOrder(Order.desc("semester")).add(Restrictions.gt("totalNilai", 0.1)).setMaxResults(1)
					.uniqueResult();

			if (skripsi == null) {
				skripsi = (Skripsi) session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
						.addOrder(Order.desc("semester")).setMaxResults(1).uniqueResult();
			}

			if (skripsi == null) {
				MyMessageboxConfig.showFormat("Mohon maaf, data \"{V1}\" belum tersedia di dalam sistem dan harus diunggah terlebih dahulu sebelum pendaftaran wisuda dapat diproses. Langkah yang dapat dilakukan: (1) Buka menu data \"{V1}\"; (2) Unggah berkas yang diperlukan; (3) Simpan data, kemudian ulangi proses pendaftaran wisuda.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, label_skripsi);
				return false;
			}

			if (skripsi.getAbstrack().trim().isEmpty()) {
				MyMessageboxConfig.showFormatCb("Mohon maaf, data \"{V1}\" belum lengkap karena Abstrak belum tersedia. Bapak/Ibu diharapkan melengkapi Abstrak terlebih dahulu. Langkah yang dapat dilakukan: (1) Tekan tombol \"OK\" untuk membuka formulir \"{V1}\"; (2) Isikan Abstrak beserta data lain yang diperlukan; (3) Simpan data, kemudian ulangi proses pendaftaran wisuda.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onDataSkripsi(arg0);
							}
						}, label_skripsi);
				return false;
			}

			if (skripsi.getJudul().trim().isEmpty()) {
				MyMessageboxConfig.showFormatCb("Mohon maaf, data \"{V1}\" belum lengkap karena Judul belum tersedia. Bapak/Ibu diharapkan melengkapi Judul terlebih dahulu. Langkah yang dapat dilakukan: (1) Tekan tombol \"OK\" untuk membuka formulir \"{V1}\"; (2) Isikan Judul beserta data lain yang diperlukan; (3) Simpan data, kemudian ulangi proses pendaftaran wisuda.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onDataSkripsi(arg0);
							}
						}, label_skripsi);
				return false;
			}

			if (skripsi.getTotalNilai() < 0.01) {
				MyMessageboxConfig.showFormatCb("Mohon maaf, data \"{V1}\" belum dapat diproses karena belum memiliki nilai. Bapak/Ibu diharapkan memastikan proses penilaian telah diselesaikan terlebih dahulu. Langkah yang dapat dilakukan: (1) Tekan tombol \"OK\" untuk membuka formulir \"{V1}\"; (2) Pastikan nilai telah diinput oleh dosen/petugas yang berwenang; (3) Simpan data, kemudian ulangi proses pendaftaran wisuda.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onDataSkripsi(arg0);
							}
						}, label_skripsi);
				return false;
			}
			// if (skripsi.getPembimbing() == null) {
			// MyMessageboxConfig.show(
			// "Data " + label_skripsi + " belum lengkap. Pembimbing utama
			// (Pembimbing 1) harus dipilih",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION, new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// onDataSkripsi(arg0);
			// }
			// });
			// return false;
			// }
			//
			// if (skripsi.getAwalBimbingan() == null) {
			// MyMessageboxConfig.show("Data " + label_skripsi + " belum
			// lengkap. Tanggal awal bimbingan",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION, new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// onDataSkripsi(arg0);
			// }
			// });
			// return false;
			// }
			//
			// if (skripsi.getAkhirBimbingan() == null) {
			// MyMessageboxConfig.show("Data " + label_skripsi + " belum
			// lengkap. Tanggal akhir bimbingan",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION, new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// onDataSkripsi(arg0);
			// }
			// });
			// return false;
			// }

			if (skripsi != null && skripsi.getId() != null) {
				try {

					LampiranLain lam = LampiranLain.ambil(skripsi.getId(), LampiranLain.SKRIPSI);

					if (lam == null) {
						MyMessageboxConfig.showFormatCb(
								"Mohon maaf, lampiran \"{V1}\" belum diunggah. Bapak/Ibu diharapkan mengunggah lampiran \"{V1}\" dalam bentuk berkas PDF terlebih dahulu. Langkah yang dapat dilakukan: (1) Tekan tombol \"OK\" untuk membuka formulir \"{V1}\"; (2) Unggah berkas dalam format PDF; (3) Simpan data, kemudian ulangi proses pendaftaran wisuda.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										onDataSkripsi(arg0);
									}
								}, label_skripsi);
						return false;
					}

				} catch (Exception e1) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/PendaftaranWisudaMahasiswaAction.java:985");
				}
			}

			// if (skripsi.getPenguji1() == null) {
			// MyMessageboxConfig.show("Data " + label_skripsi + " belum
			// lengkap. Penguji I harus diisi", "Peringatan",
			// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new
			// EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// onDataSkripsi(arg0);
			// }
			// });
			// return false;
			// }
			//
			// if (skripsi.getTanggalSidang() == null) {
			// MyMessageboxConfig.show("Data " + label_skripsi + " belum
			// lengkap. Tanggal sidang harus diisi",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION, new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// onDataSkripsi(arg0);
			// }
			// });
			// return false;
			// }
			//
			// if (skripsi.getWaktuSidang().trim().isEmpty()) {
			// MyMessageboxConfig.show("Data " + label_skripsi + " belum
			// lengkap. Waktu mulai sidang harus diisi",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION, new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// onDataSkripsi(arg0);
			// }
			// });
			// return false;
			// }
			//
			// if (skripsi.getWaktuSampaiSidang().trim().isEmpty()) {
			// MyMessageboxConfig.show("Data " + label_skripsi + " belum
			// lengkap. Waktu sampai sidang harus diisi",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION, new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// onDataSkripsi(arg0);
			// }
			// });
			// return false;
			// }
		}

		Wisuda wisuda = (Wisuda) (wisudaKe.getSelectedItem() == null ? null : wisudaKe.getSelectedItem().getValue());
		if (wisuda == null) {
			MyMessageboxConfig.show("Mohon maaf, periode wisuda belum dipilih. Bapak/Ibu diharapkan memilih salah satu periode wisuda terlebih dahulu untuk dapat melanjutkan proses pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							wisudaKe.focus();
						}
					});
			return false;
		}

		if (!wisuda.getHanyaGunakanKuotaPerguruanTinggi()) {
			QuotaWisudaUntukFakultas quotaWisudaUntukFakultas = (QuotaWisudaUntukFakultas) session
					.createCriteria(QuotaWisudaUntukFakultas.class)
					.add(Restrictions.eq("fakultas", mahasiswa.getJurusan().getFakultas()))
					.add(Restrictions.eq("wisuda", wisuda)).uniqueResult();

			if (quotaWisudaUntukFakultas == null) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, kuota wisuda untuk Fakultas {V1} belum ditetapkan sehingga pendaftaran belum dapat diproses. Bapak/Ibu diharapkan segera menghubungi bagian akademik agar kuota wisuda untuk fakultas tersebut dapat dibuat terlebih dahulu.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						mahasiswa.getJurusan().getFakultas().getNama());
				return false;
			}

			Integer quotaWisuda = quotaWisudaUntukFakultas.getQuota();

			Integer jmlWisudaUntukFakultas = ((Number) session.createCriteria(PendaftaranWisuda.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("wisuda", (Wisuda) wisudaKe.getSelectedItem().getValue()))
					.createCriteria("mahasiswa").createCriteria("jurusan", Criteria.LEFT_JOIN)
					.add(Restrictions.eq("fakultas", mahasiswa.getJurusan().getFakultas())).uniqueResult()).intValue();

			if (jmlWisudaUntukFakultas >= quotaWisuda) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, kuota wisuda untuk Fakultas {V1} (sebanyak {V2} peserta) telah terpenuhi. Untuk saat ini Bapak/Ibu tercatat masuk ke dalam daftar tunggu (waiting list). Mohon menunggu informasi lebih lanjut atau menghubungi bagian akademik.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						mahasiswa.getJurusan().getFakultas().getNama(), quotaWisuda);
				return false;
			}
		}

		Integer quotaWisuda = wisuda.getMaksimalQuota();

		Integer jmlWisudawan = ((Number) session.createCriteria(PendaftaranWisuda.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("wisuda", (Wisuda) wisudaKe.getSelectedItem().getValue())).uniqueResult())
				.intValue();

		if (jmlWisudawan >= quotaWisuda) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, kuota maksimal wisuda (sebanyak {V1} peserta) telah terpenuhi. Untuk saat ini Bapak/Ibu tercatat masuk ke dalam daftar tunggu (waiting list). Mohon menunggu informasi lebih lanjut atau menghubungi bagian akademik.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, quotaWisuda);
			return false;
		}

		if (!Common.checkStatusPembayaranMahasiswaPengajuanWisuda(mahasiswa.currentSemester(), mahasiswa)) {
			Double harusLunas = 90.0;
			try {
				harusLunas = Double.parseDouble(Common
						.getKonfigurasi("batas_terendah_persen_pembayaran_sebelum_wisuda", "90").getNilai().trim());
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			MyMessageboxConfig.showFormat(
					"Mohon maaf, untuk dapat mengajukan wisuda, mahasiswa \"{V1}\" terlebih dahulu harus melunasi minimal {V2}% dari biaya perkuliahan pada semester {V3}. Bapak/Ibu diharapkan menghubungi bagian keuangan untuk memperoleh informasi rincian tagihan serta menyelesaikan kewajiban pembayaran, kemudian mengulangi proses pendaftaran wisuda.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
					mahasiswa.toString(), harusLunas, (mahasiswa.currentSemester()));

			return false;
		}

		if (Common.bolehKonfigurasi("pengajuan_wisuda_harus_telah_mengembalikan_buku_perpustakaan")) {

			List<PeminjamanPengadaanItemDetail> objects = session.createCriteria(PeminjamanPengadaanItemDetail.class)
					.add(Restrictions.isNull("kembaliPengadaanItemDetail"))
					.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
					.createAlias("peminjamanPengadaanItem.anggota", "anggota")
					.add(Restrictions.eq("anggota.mahasiswa", mahasiswa)).list();
			String content = "";
			for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail : objects) {
				content += "Item atau buku \"" + peminjamanPengadaanItemDetail.getItem().getNama() + "\" terlambat "
						+ Common.numberFormat.get().format(peminjamanPengadaanItemDetail.getJumlahHariTerlambat())
						+ " hari di perpustakaan "
						+ peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem().getPerpustakaan().getNama()
						+ ".\n";
			}

			if (!content.trim().isEmpty()) {
				MyMessageboxConfig
						.showFormat("Mohon maaf, untuk dapat mengajukan wisuda, Bapak/Ibu terlebih dahulu harus mengembalikan seluruh buku perpustakaan yang masih dipinjam. Berikut rincian buku yang belum dikembalikan:\n\n{V1}\nBapak/Ibu diharapkan segera mengembalikan buku-buku tersebut ke perpustakaan terkait, kemudian mengulangi proses pendaftaran wisuda.",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, content);

				return false;
			}

		}

		String kodeItemBiaya = Common.getKonfigurasi("kode_item_biaya_wisuda", "").getNilai().trim();
		if (!kodeItemBiaya.trim().isEmpty()) {

			for (String kode : kodeItemBiaya.trim().split(",")) {
				ItemBiaya itemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
						.add(Restrictions.eq("kode", kode.trim())).setMaxResults(1).uniqueResult();
				if (itemBiaya != null) {

					int jumlahKeg = ((Number) session.createCriteria(DetailKegiatan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.createAlias("kegiatan", "kegiatan").add(Restrictions.eq("itemBiaya", itemBiaya))
							.add(Restrictions.eq("bukanTagihan", false)).add(Restrictions.gt("biaya", 0.1))
							.add(Restrictions.eq("kegiatan.mahasiswa", mahasiswa)).setProjection(Projections.rowCount())
							.uniqueResult()).intValue();
					if (jumlahKeg > 0) {
						int jumlah = ((Number) session.createCriteria(CicilanPembayaran.class)
								.createAlias("kegiatan", "kegiatan").add(Restrictions.eq("itemBiaya", itemBiaya))
								.add(Restrictions.eq("kegiatan.mahasiswa", mahasiswa))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();
						if (jumlah == 0) {
							MyMessageboxConfig.showFormat(
									"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} tercatat belum melunasi biaya {V3} - {V4}. Untuk dapat melanjutkan proses pendaftaran wisuda, Bapak/Ibu diharapkan menghubungi bagian keuangan terlebih dahulu guna menyelesaikan pembayaran biaya tersebut.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
									mahasiswa.getNim(), mahasiswa.getNama(), itemBiaya.getKode(), itemBiaya.getNama());
							return false;
						}
					}
				}
			}

		}

		return true;
	}

	public void onSave(Event event) throws InterruptedException {
		try {

			Session session = HibernateUtil.currentSession();

			if (ukuranToga.getSelectedItem() == null) {
				MyMessageboxConfig.show("Mohon maaf, ukuran toga belum dipilih. Bapak/Ibu diharapkan memilih salah satu ukuran toga terlebih dahulu sebelum data pendaftaran wisuda disimpan.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}
			if (wisudaKe.getSelectedItem() == null || wisudaKe.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Mohon maaf, periode wisuda belum dipilih. Bapak/Ibu diharapkan memilih periode wisuda terlebih dahulu sebelum data pendaftaran disimpan.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}

			if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
				MyMessageboxConfig.show("Mohon maaf, data mahasiswa belum dipilih. Bapak/Ibu diharapkan memilih mahasiswa terlebih dahulu sebelum data pendaftaran wisuda disimpan.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}

			// Pakai pastikanPendaftaranWisudaMasihAda() (get(), bukan load()) SEBELUM
			// setSkripsi()/dsb memakai pendaftaranWisuda: load() menghasilkan lazy proxy
			// yang baru gagal (ObjectNotFoundException) saat properti diakses nanti kalau
			// baris sudah dihapus dari DB sementara layar ini masih memegang referensi lama.
			if (!pastikanPendaftaranWisudaMasihAda(session)) {
				return;
			}

			mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
			if (!checkQuota(mahasiswa)) {
				return;
			}

			if (pendaftaranWisuda == null || pendaftaranWisuda.getId() == null) {
				PendaftaranWisuda temp = (PendaftaranWisuda) session.createCriteria(PendaftaranWisuda.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("wisuda", (Wisuda) wisudaKe.getSelectedItem().getValue())).setMaxResults(1)
						.uniqueResult();

				if (temp != null) {
					pendaftaranWisuda = temp;
				}
			}

			pendaftaranWisuda.setSkripsi(skripsi);
			pendaftaranWisuda.setMahasiswa(mahasiswa);
			pendaftaranWisuda.setTanggalDaftarWisuda(ais.ui.util.WaktuUtil.getDate());
			pendaftaranWisuda.setUkuranToga((Integer) ukuranToga.getSelectedItem().getValue());
			pendaftaranWisuda.setWisuda((Wisuda) wisudaKe.getSelectedItem().getValue());
			pendaftaranWisuda.setTanggalDaftarWisuda(tanggalDaftarWisuda.getValue());

			if (pendaftaranWisuda.getId() != null) {
				session.update(pendaftaranWisuda);
			} else {
				session.save(pendaftaranWisuda);
			}

			session.flush();

			if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(lainMahasiswa);
					lainMahasiswa.setRef(pendaftaranWisuda.getId());

					session.getTransaction().begin();
					session.update(lainMahasiswa);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}

			MyMessageboxConfig.show("Alhamdulillah, data pendaftaran wisuda Bapak/Ibu telah berhasil disimpan dan tercatat sebagai peserta wisuda. Silakan menyimpan atau mencetak bukti pendaftaran sebagai arsip.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			MyMessageboxConfig.show("Mohon maaf, data pendaftaran wisuda gagal disimpan. Langkah yang dapat dilakukan: (1) Periksa kembali kelengkapan seluruh isian yang diperlukan; (2) Ulangi proses penyimpanan; (3) Apabila kendala masih berlanjut, mohon menghubungi administrator sistem.");
		}
	}

	/**
	 * Memastikan referensi {@link #pendaftaranWisuda} yang dipegang layar ini (field kelas,
	 * bertahan sepanjang umur layar/konversasi ZK) masih benar-benar ada di database sebelum
	 * dipakai. Field ini bisa menjadi proxy Hibernate basi (stale) jika baris PendaftaranWisuda
	 * yang bersangkutan sudah dihapus oleh proses/pengguna lain di antara saat layar dimuat dan
	 * saat aksi ini dijalankan (mis. tombol "Batal" dari sesi/tab lain). Memanggil setter/getter
	 * apa pun (termasuk getId()) pada proxy basi tsb dapat memicu inisialisasi lazy yang gagal:
	 * {@link org.hibernate.ObjectNotFoundException} bila baris memang sudah tidak ada, atau
	 * {@link NullPointerException} bila proxy tsb sudah tidak lagi terhubung ke sesi Hibernate
	 * yang membuatnya. Dipakai get() (bukan load()) agar hasilnya null bila tak ditemukan,
	 * sehingga dapat dideteksi dgn aman tanpa membiarkan exception tsb terlempar ke pengguna.
	 *
	 * @param session sesi Hibernate aktif yang dipakai untuk memuat ulang data.
	 * @return {@code true} bila data masih ada (atau memang belum pernah tersimpan / id null,
	 *         sehingga tidak perlu diverifikasi) dan {@link #pendaftaranWisuda} sudah diperbarui
	 *         dgn hasil segar dari database; {@code false} bila data sudah tidak tersedia lagi
	 *         (pesan peringatan sudah ditampilkan ke pengguna, pemanggil wajib menghentikan proses).
	 * @throws InterruptedException diteruskan dari {@code MyMessageboxConfig.show(...)}.
	 */
	private boolean pastikanPendaftaranWisudaMasihAda(Session session) throws InterruptedException {
		try {
			if (pendaftaranWisuda == null || pendaftaranWisuda.getId() == null) {
				return true;
			}

			PendaftaranWisuda segar = (PendaftaranWisuda) session.get(PendaftaranWisuda.class,
					pendaftaranWisuda.getId());
			if (segar == null) {
				MyMessageboxConfig.show(
						"Mohon maaf, data pendaftaran wisuda ini sudah tidak tersedia di database (mungkin telah dihapus oleh proses/pengguna lain). Silakan tutup formulir ini lalu buka kembali sebelum melanjutkan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			pendaftaranWisuda = segar;
			return true;
		} catch (InterruptedException ie) {
			throw ie;
		} catch (Exception e) {
			// Proxy basi (mis. NPE dari AbstractLazyInitializer krn sesi lama yg membuat
			// proxy ini sudah tertutup) ditangkap di sini agar pengguna mendapat pesan yang
			// jelas alih-alih layar error teknis.
			Common.tampilErrorJikaAdmin(e);
			MyMessageboxConfig.show(
					"Mohon maaf, data pendaftaran wisuda ini sudah tidak tersedia atau tidak dapat dimuat ulang. Silakan tutup formulir ini lalu buka kembali sebelum melanjutkan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
	}
}
