package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.GrupPertemuan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengajuanIzinTidakMasukPerkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaGrupPertemuan;
import ais.database.model.Ruang;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper presensi (absensi) untuk satu {@link GrupPertemuan} (grup pertemuan/konsultasi,
 * mis. bimbingan dosen PA yang beranggotakan beberapa mahasiswa dengan satu pertemuan
 * individual masing-masing lewat {@link PertemuanPunyaGrupPertemuan}). Membangun tampilan
 * tiga panel: informasi pertemuan (tanggal/waktu/ruang/kehadiran dosen/dosen pengganti,
 * region West), daftar presensi mahasiswa yang dapat diedit (region Center), dan daftar
 * pengajuan izin/sakit yang perlu disetujui (region East).
 *
 * <p>
 * <b>Kontrol akses edit</b> — field presensi dan tombol massal ("Semua masuk", "Reset")
 * hanya AKTIF untuk pengguna staf (bukan mahasiswa/siswa) atau bila
 * {@link #mahasiswaBolehUbahAbsen} diset {@code true} (saat ini selalu {@code false},
 * diinisialisasi ulang di {@link #createListMahasiswaAbsensi}); mahasiswa/siswa hanya
 * melihat status sebagai label. Seluruh region dibekukan (readonly) via
 * {@link Common#freeze} bila konteks tampilan adalah milik mahasiswa/calon
 * mahasiswa/peserta kursus sendiri. Pengajuan izin yang sudah disetujui ditampilkan
 * sebagai status tetap (tidak dapat diedit lagi via kombo presensi biasa).
 * </p>
 */
public class AbsensiGrupPertemuanHelper {

	private MyDatebox tanggal;
	private Timebox waktuMulai;
	private Timebox waktuSelesai;
	private AmbilDataRuangBanbox ruang;
	private GrupPertemuan grupPertemuan;
	private MyGrid mahasiswaGrid;

	private List<PertemuanPunyaGrupPertemuan> mahasiswas;

	private Mahasiswa mahasiswa;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private Tabpanel tabpanelUtama;
	private MyGrid mahasiswaIzinGrid;
	private boolean mahasiswaBolehUbahAbsen;
	private Center center;

	/**
	 * Membuat helper untuk konteks tampilan tertentu. Bila {@code mahasiswa} atau
	 * {@code biodataCalonMahasiswa} terisi, tampilan yang dihasilkan dibekukan
	 * (read-only) — konteks ini dipakai saat mahasiswa membuka presensi miliknya sendiri.
	 *
	 * @param mahasiswa             mahasiswa pemilik konteks tampilan, boleh {@code null}
	 * @param biodataCalonMahasiswa konteks calon mahasiswa alternatif, boleh {@code null}
	 */
	public AbsensiGrupPertemuanHelper(final Mahasiswa mahasiswa, final BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Membuat tombol "Absen" yang membuka jendela {@link GrupPertemuanHelper} untuk
	 * mahasiswa yang sedang login mengisi presensinya sendiri pada {@code grupPertemuan}.
	 *
	 * @param grupPertemuan grup pertemuan yang akan diabsen
	 * @param dataLoader    callback pemuatan ulang data setelah jendela ditutup
	 * @return tombol toolbar siap pakai
	 */
	public static MyToolbarbuttonConfig createTombolAbsen(final GrupPertemuan grupPertemuan,
			final DataLoader dataLoader) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Absen", "/img/vendor.png");

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				new GrupPertemuanHelper(Common.getCurrentUser().getMahasiswa()).display(grupPertemuan, dataLoader, 0);
			}
		});

		return toolbarbutton;
	}

	/**
	 * Membangun seluruh tampilan presensi {@code grupPertemuan} ke dalam
	 * {@code tabpanelUtama}: region West berisi form tanggal/waktu/ruang (dapat diedit,
	 * disimpan lewat {@link #save()}), status kehadiran dosen utama, dan pemilihan
	 * dosen pengganti (staf saja); region Center berisi daftar presensi mahasiswa
	 * ({@link #createListMahasiswaAbsensi}); region East (dimuat lewat timer, setelah
	 * region lain terpasang) berisi daftar pengajuan izin/sakit
	 * ({@link #createListMahasiswaIzin}). Method langsung kembali tanpa membangun apa
	 * pun bila grup pertemuan tidak punya anggota mahasiswa.
	 *
	 * @param grupPertemuan grup pertemuan yang diabsen
	 * @param tabpanelUtama tab panel tempat tampilan dibangun
	 * @throws Exception diteruskan dari operasi tampilan/akses Hibernate
	 */
	@SuppressWarnings("unchecked")
	public void mainInit(final GrupPertemuan grupPertemuan, Tabpanel tabpanelUtama) throws Exception {
		this.tabpanelUtama = tabpanelUtama;
		Session session = HibernateUtil.currentSession();
		Criteria crit = session.createCriteria(PertemuanPunyaGrupPertemuan.class)
				.add(Restrictions.eq("grupPertemuan", grupPertemuan)).createAlias("mahasiswa", "mahasiswa")
				.addOrder(Order.asc("mahasiswa.nama"));

		mahasiswas = crit.list();
		if (mahasiswas.isEmpty()) {
			return;
		}
		this.grupPertemuan = grupPertemuan;
		Tbmuser tbmuser = Common.getCurrentUser();

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanelUtama);

		Center center = new Center();
		center.setTitle("Presensi kehadiran mahasiswa");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		West west = new West();
		west.setTitle("Informasi");
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("30%");
		west.setParent(borderlayout);

		final East east = new East();
		east.setTitle("Pengajuan Izin atau Sakit");
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("30%");
		east.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("150px");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal (*)"));
		row.appendChild(tanggal = new MyDatebox(grupPertemuan.getTanggal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(waktuMulai = new ais.ui.util.MyTimebox());
		waktuMulai.setFormat(Common.timeFormat2.get().toPattern());
		try {
			waktuMulai.setValue(grupPertemuan.getWaktuMulai() == null || grupPertemuan.getWaktuMulai().trim().isEmpty()
					? null : Common.timeFormat2.get().parse(grupPertemuan.getWaktuMulai()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiGrupPertemuanHelper.java:174");

		}

		hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
		hbox.appendChild(waktuSelesai = new ais.ui.util.MyTimebox());
		waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
		try {
			waktuSelesai.setValue(
					grupPertemuan.getWaktuSelesai() == null || grupPertemuan.getWaktuSelesai().trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(grupPertemuan.getWaktuSelesai()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiGrupPertemuanHelper.java:185");

		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));
		row.appendChild(ruang = new AmbilDataRuangBanbox());
		ruang.setReadonly(true);
		ruang.setValue(grupPertemuan.getRuang() == null ? "" : grupPertemuan.getRuang().getNama());
		ruang.setAttribute("ruang", grupPertemuan.getRuang());
		ruang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Kehadiran Dosen"));
		Vbox vbox = new Vbox();
		vbox.setParent(row);

		Dosen dosen = grupPertemuan.getDosen();
		vbox.appendChild(CommonMedia.tampilkanGambarKecil(dosen));
		vbox.appendChild(new Label(dosen.getNama()));
		vbox.appendChild(AbsensiGrupPertemuanHelper.createStatusKehadiran(dosen, mahasiswas.get(0).getPertemuan(),
				mahasiswa, biodataCalonMahasiswa));

		Dosen dsnPengganti = (Dosen) (grupPertemuan.getDosenPengganti() == null ? null
				: HibernateUtil.currentSession().createCriteria(Dosen.class)
						.add(Restrictions.idEq(grupPertemuan.getDosenPengganti())).uniqueResult());

		row = new MyFormRow();
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pengganti"));

		vbox = new Vbox();
		vbox.setParent(row);

		final Hbox dosenPenggantiHb = new Hbox();
		vbox.appendChild(dosenPenggantiHb);

		if (dsnPengganti != null) {
			Common.clear(dosenPenggantiHb);
			dosenPenggantiHb.appendChild(CommonMedia.tampilkanGambarKecil(dsnPengganti));
		}

		if (Common.getCurrentUser().getMahasiswa() == null) {
			final AmbilDataDosenBanbox dosenPengganti;
			vbox.appendChild(dosenPengganti = new AmbilDataDosenBanbox(true));
			dosenPengganti.setAttribute("dosen", dsnPengganti);
			dosenPengganti.setValue(dsnPengganti == null ? "" : dsnPengganti.getNama());
			dosenPengganti.setReadonly(true);
			dosenPengganti.setWidth("90%");

			dosenPengganti.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Dosen d = (Dosen) dosenPengganti.getAttribute("dosen");
					grupPertemuan.setDosenPengganti(d == null ? null : d.getId());
					Common.refreshUpdate(grupPertemuan);
					AbsensiGrupPertemuanHelper.this.grupPertemuan = grupPertemuan;

					if (d != null) {
						Common.clear(dosenPenggantiHb);
						dosenPenggantiHb.appendChild(CommonMedia.tampilkanGambarKecil(d));
					}
				}
			});
		} else {
			new Label(dsnPengganti == null ? "" : dsnPengganti.getNama()).setParent(vbox);
		}

		createListMahasiswaAbsensi(center);

		Common.freeze(borderlayout, mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				createListMahasiswaIzin(east);
			}
		});

	}

	/**
	 * Membangun region presensi mahasiswa ke dalam {@code parentrow}: toolbar tombol
	 * massal "Semua masuk" (menandai seluruh mahasiswa hadir, dengan konfirmasi) dan
	 * "Reset" (mengembalikan seluruh status ke {@link ConstantValues#BELUM_ABSEN},
	 * dengan konfirmasi) — hanya terlihat untuk staf atau bila
	 * {@link #mahasiswaBolehUbahAbsen} — lalu grid presensi via {@link #reloadAbsensi()}.
	 *
	 * @param parentrow kontainer ZK tempat region dibangun
	 */
	private void createListMahasiswaAbsensi(Component parentrow) {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(parentrow);

		Tbmuser tbmuser = Common.getCurrentUser();

		mahasiswaBolehUbahAbsen = false;

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		if ((tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null) || mahasiswaBolehUbahAbsen) {

			MyToolbarbuttonConfig masuk = new MyToolbarbuttonConfig("Semua masuk", "/img/svg/check2.svg");
			masuk.setParent(toolbar);
			masuk.setTooltiptext("Tutup");
			masuk.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin semua mahasiswa masuk kelas ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												for (PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan : mahasiswas) {
													Statusabsensi statusabsensi = ConstantValues.MASUK;
													Pertemuan pertemuan = pertemuanPunyaGrupPertemuan.getPertemuan();
													pertemuan.populate(
															pertemuanPunyaGrupPertemuan.getMahasiswa().getId(),
															statusabsensi, pertemuan.getWaktuMulai(),
															pertemuan.getWaktuSelesai(), "Mahasiswa");
													Common.refreshUpdate(pertemuan);
												}

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														reloadAbsensi();
													}
												});
											}
										});

									}

								}
							});

				}
			});

			masuk = new MyToolbarbuttonConfig("Reset", "/img/reply.png");
			masuk.setParent(toolbar);
			masuk.setTooltiptext("Reset");
			masuk.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin me-reset absen di kelas ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												for (PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan : mahasiswas) {
													Statusabsensi statusabsensi = ConstantValues.BELUM_ABSEN;
													Pertemuan pertemuan = pertemuanPunyaGrupPertemuan.getPertemuan();
													pertemuan.populate(
															pertemuanPunyaGrupPertemuan.getMahasiswa().getId(),
															statusabsensi, pertemuan.getWaktuMulai(),
															pertemuan.getWaktuSelesai(), "Mahasiswa");
													Common.refreshUpdate(pertemuan);
												}

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														reloadAbsensi();
													}
												});
											}
										});

									}

								}
							});

				}
			});
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		reloadAbsensi();
	}

	/**
	 * Membangun region daftar pengajuan izin/sakit ({@link PengajuanIzinTidakMasukPerkuliahan})
	 * ke dalam {@code parentrow}: grid dengan kolom OK (persetujuan)/Mahasiswa/
	 * Keterangan, dimuat lewat {@link #reloadIzinAbsensi()}.
	 *
	 * @param parentrow kontainer ZK tempat region dibangun
	 */
	private void createListMahasiswaIzin(Component parentrow) {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(parentrow);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		mahasiswaIzinGrid = new MyGrid();
		mahasiswaIzinGrid.setMold("paging");
		mahasiswaIzinGrid.setPageSize(10000);
		mahasiswaIzinGrid.setParent(center);
		mahasiswaIzinGrid.setWidth("100%");
		mahasiswaIzinGrid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(mahasiswaIzinGrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("OK");
		column.setWidth("16%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mahasiswa");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("8%");

		reloadIzinAbsensi();
	}

	/**
	 * Menyimpan perubahan pada form informasi pertemuan (waktu mulai/selesai, ruang,
	 * tanggal) ke {@link #grupPertemuan}. Menolak dan mengarahkan fokus ke field
	 * tanggal bila tab ini sedang aktif dan tanggal belum diisi.
	 *
	 * @return {@code true} bila berhasil disimpan; {@code false} bila validasi tanggal gagal
	 * @throws InterruptedException diteruskan dari dialog messagebox
	 */
	@SuppressWarnings({})
	public boolean save() throws InterruptedException {

		if (tabpanelUtama.getLinkedTab().isSelected() && tanggal.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, tanggal pertemuan belum diisi. Langkah yang dapat dilakukan: (1) klik kolom tanggal dan pilih tanggal pertemuan; (2) pastikan format tanggal sudah benar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tabpanelUtama.getLinkedTab().setSelected(true);
							tanggal.focus();
						}
					});
			return false;
		}

		grupPertemuan
				.setWaktuMulai(waktuMulai.getValue() == null ? null : Common.timeFormat2.get().format(waktuMulai.getValue()));
		grupPertemuan.setWaktuSelesai(
				waktuSelesai.getValue() == null ? null : Common.timeFormat2.get().format(waktuSelesai.getValue()));

		grupPertemuan.setRuang((Ruang) ruang.getAttribute("ruang"));

		grupPertemuan.setTanggal(tanggal.getValue());

		Common.refreshUpdate(grupPertemuan);

		return true;
	}

	/** Membangun ulang grid presensi mahasiswa (kolom Status/Mahasiswa/Keterangan) dari {@link #mahasiswas}, dirender lewat {@link MahasiswaRenderer}. */
	private void reloadAbsensi() {

		Common.clear(center);

		mahasiswaGrid = new MyGrid();
		mahasiswaGrid.setMold("paging");
		mahasiswaGrid.setPageSize(10000);
		mahasiswaGrid.setParent(center);
		mahasiswaGrid.setWidth("100%");
		mahasiswaGrid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(mahasiswaGrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mahasiswa");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("30%");

		mahasiswaGrid.setOddRowSclass("non-odd");

		ListModel strset = new SimpleListModel(mahasiswas);
		mahasiswaGrid.setRowRenderer(new MahasiswaRenderer());
		mahasiswaGrid.setModelCheckMobile(strset);

	}

	/** Membangun ulang grid pengajuan izin/sakit dari seluruh {@link Pertemuan} milik anggota {@link #grupPertemuan}, dirender lewat {@link MahasiswaIzinRenderer}. */
	@SuppressWarnings("unchecked")
	private void reloadIzinAbsensi() {

		List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();

		for (PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan : mahasiswas) {
			pertemuans.add(pertemuanPunyaGrupPertemuan.getPertemuan());
		}

		List<PengajuanIzinTidakMasukPerkuliahan> pengajuanIzinTidakMasukPerkuliahans = HibernateUtil.currentSession()
				.createCriteria(PengajuanIzinTidakMasukPerkuliahan.class).add(pertemuans.isEmpty()
						? Restrictions.sqlRestriction("false") : Restrictions.in("pertemuan", pertemuans))
				.list();
		ListModel strset = new SimpleListModel(pengajuanIzinTidakMasukPerkuliahans);
		mahasiswaIzinGrid.setRowRenderer(new MahasiswaIzinRenderer());
		mahasiswaIzinGrid.setModelCheckMobile(strset);
		mahasiswaIzinGrid.setOddRowSclass("non-odd");

	}

	/**
	 * Merender satu baris presensi mahasiswa untuk {@code pertemuanPunyaGrupPertemuan}.
	 * Baris disembunyikan bila data mahasiswa kosong. Bila mahasiswa punya pengajuan
	 * izin/sakit yang SUDAH DISETUJUI, status ditampilkan sebagai label tetap (tidak
	 * dapat diubah lewat kombo presensi). Selain itu: untuk staf (atau bila
	 * {@link #mahasiswaBolehUbahAbsen}), ditampilkan combobox status kehadiran
	 * (mengecualikan status Cuti/Cuti Hamil) beserta jam masuk-selesai opsional
	 * ({@code tampilkan_jam_masuk_absen_untuk_mahasiswa}) dan textbox keterangan —
	 * seluruhnya langsung tersimpan on-change ke {@link Pertemuan} via
	 * {@link Pertemuan#populate}; untuk mahasiswa, hanya label status+jam+keterangan
	 * yang ditampilkan.
	 *
	 * @param arg0                        baris grid yang akan diisi
	 * @param pertemuanPunyaGrupPertemuan entri keanggotaan+pertemuan individual mahasiswa
	 * @param statusabsensis              daftar status absensi yang tersedia (parameter dipertahankan untuk kompatibilitas, daftar aktual di-query ulang dari combobox)
	 * @param status                      status absensi default (parameter dipertahankan untuk kompatibilitas)
	 * @param tbmuser                     pengguna yang sedang login, menentukan mode tampilan (edit/baca saja)
	 * @throws Exception diteruskan dari operasi tampilan/akses Hibernate
	 */
	private void tampilRowAbsensi(Row arg0, final PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan,
			List<Statusabsensi> statusabsensis, Statusabsensi status, Tbmuser tbmuser) throws Exception {
		final Mahasiswa mahasiswa = pertemuanPunyaGrupPertemuan.getMahasiswa();
		final Pertemuan pertemuan = pertemuanPunyaGrupPertemuan.getPertemuan();

		if (mahasiswa == null) {
			arg0.setVisible(false);
			return;
		}

		arg0.setAttribute("pertemuan", pertemuan);
		arg0.setAttribute("mahasiswa", mahasiswa);

		final Mahasiswa mhs = mahasiswa;

		final Textbox keterangan = new Textbox(pertemuan.retreiveAbsensiKeterangan(mahasiswa.getId()));
		final Timebox waktuMulai = new ais.ui.util.MyTimebox();
		final Timebox waktuSelesai = new ais.ui.util.MyTimebox();
		waktuMulai.setVisible(Common.bolehKonfigurasi("tampilkan_jam_masuk_absen_untuk_mahasiswa"));
		waktuSelesai.setVisible(waktuMulai.isVisible());
		final Combobox kehadiran = new Combobox();

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (kehadiran.getSelectedItem() == null) {
					return;
				}
				Statusabsensi statusabsensi = (Statusabsensi) kehadiran.getSelectedItem().getValue();
				if (statusabsensi.getKode() != null && statusabsensi.getKode().trim().equals("M")) {

					if (waktuMulai.getValue() == null) {
						try {
							waktuMulai.setValue(
									pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty()
											? null : Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiGrupPertemuanHelper.java:559");

						}
					}
					if (waktuSelesai.getValue() == null) {
						try {
							waktuSelesai.setValue(
									pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty()
											? null : Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiGrupPertemuanHelper.java:568");

						}
					}
				} else {
					waktuMulai.setValue(null);
					waktuSelesai.setValue(null);
				}

				pertemuan.populate(mhs.getId(), statusabsensi, keterangan.getValue(), null,
						waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()),
						waktuSelesai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuSelesai.getValue()),
						"Mahasiswa");
				Common.refreshUpdate(pertemuan);
			}
		};

		PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan = null;
		Session session = HibernateUtil.currentSession();
		pengajuanIzinTidakMasukPerkuliahan = (PengajuanIzinTidakMasukPerkuliahan) session
				.createCriteria(PengajuanIzinTidakMasukPerkuliahan.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(Restrictions.eq("pertemuan", pertemuan)).setMaxResults(1).uniqueResult();

		Statusabsensi statusabsensi = (Statusabsensi) session.createCriteria(Statusabsensi.class)
				.add(Restrictions.idEq(pertemuan.retreiveAbsensiId(mahasiswa.getId()))).uniqueResult();

		if (tbmuser.getMahasiswa() == null || mahasiswaBolehUbahAbsen) {

			if (statusabsensi != null && pengajuanIzinTidakMasukPerkuliahan != null
					&& pengajuanIzinTidakMasukPerkuliahan.getDiizinkan()) {
				new Label(Common.getBahasaConfig(pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi().getNama()))
						.setParent(arg0);
			} else {

				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				kehadiran.setWidth("92px");
				kehadiran.setReadonly(true);
				Common.insertComboMyConfig(kehadiran, "nama", Statusabsensi.class,
						Restrictions.sqlRestriction("nama not in ('Cuti','Cuti Hamil')"));
				Common.selectComboItem(kehadiran, statusabsensi);

				kehadiran.addEventListener("onChange", eventListener);
				vbox.appendChild(kehadiran);

				Hbox hbox = new Hbox();
				hbox.setVisible(waktuMulai.isVisible());
				hbox.setParent(vbox);

				waktuMulai.setCols(2);
				waktuSelesai.setCols(2);

				hbox.appendChild(waktuMulai);
				waktuMulai.setFormat(Common.timeFormat2.get().toPattern());
				try {
					waktuMulai.setValue(Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiMulai(mahasiswa.getId())));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiGrupPertemuanHelper.java:624");

				}

				hbox.appendChild(new ais.ui.util.MyLabelConfig("s.d"));
				hbox.appendChild(waktuSelesai);
				waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
				try {
					waktuSelesai.setValue(Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiSampai(mahasiswa.getId())));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiGrupPertemuanHelper.java:633");

				}
				waktuMulai.addEventListener("onChange", eventListener);
				waktuSelesai.addEventListener("onChange", eventListener);
			}

		} else {
			if (pengajuanIzinTidakMasukPerkuliahan != null && pengajuanIzinTidakMasukPerkuliahan.getDiizinkan()) {
				new Label(pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi().getNama()).setParent(arg0);
			} else {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				new Label(statusabsensi == null ? "" : statusabsensi.getNama()).setParent(hbox);
				String wkt = pertemuan.retreiveAbsensiMulai(mahasiswa.getId()) + " s.d "
						+ pertemuan.retreiveAbsensiSampai(mahasiswa.getId());
				new Label(wkt.trim().equals("s.d") ? "" : wkt).setParent(hbox);

			}
		}

		Hbox hbox = new Hbox();
		hbox.setParent(arg0);
		// Rapikan: foto & identitas rata ATAS dan KIRI agar sejajar antar baris.
		hbox.setAlign("top");
		hbox.setStyle("align-items:flex-start; text-align:left;");
		CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);

		Vbox vbox = new Vbox();
		vbox.setParent(hbox);
		vbox.setAlign("start");
		vbox.setStyle("text-align:left; align-items:flex-start;");
		new Label(mahasiswa.getNim()).setParent(vbox);
		new Label(mahasiswa.getNama()).setParent(vbox);

		if (pengajuanIzinTidakMasukPerkuliahan != null && pengajuanIzinTidakMasukPerkuliahan.getDiizinkan()) {
			new Label(pengajuanIzinTidakMasukPerkuliahan.getKeterangan()).setParent(arg0);
		} else if (tbmuser.getMahasiswa() == null || mahasiswaBolehUbahAbsen) {

			keterangan.setWidth("90%");
			keterangan.setRows(2);
			keterangan.setParent(arg0);
			keterangan.addEventListener("onChange", eventListener);
		} else {
			new Label(pertemuan.retreiveAbsensiKeterangan(mahasiswa.getId())).setParent(arg0);
		}

	}

	/** Perender baris grid presensi utama, mendelegasikan ke {@link #tampilRowAbsensi}. */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		private List<Statusabsensi> statusabsensis;
		private Statusabsensi status;
		private Tbmuser tbmuser = Common.getCurrentUser();

		/** Memuat daftar status absensi (kecuali Cuti) dan status default dari konfigurasi {@code default_status_kehadiran}. */
		@SuppressWarnings("unchecked")
		public MahasiswaRenderer() {
			Session session = HibernateUtil.currentSession();
			statusabsensis = session.createCriteria(Statusabsensi.class)
					.add(Restrictions.not(Restrictions.ilike("nama", "cuti", MatchMode.ANYWHERE))).list();

			Konfigurasi konfigurasi = Common.getKonfigurasi("default_status_kehadiran",
					ConstantValues.BELUM_ABSEN.getKode());

			Statusabsensi statusabsensi = (Statusabsensi) session.createCriteria(Statusabsensi.class)
					.add(Restrictions.ilike("kode", konfigurasi.getNilai().trim())).setMaxResults(1).uniqueResult();
			status = statusabsensi != null ? statusabsensi : ConstantValues.BELUM_ABSEN;

		}

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) arg1;
			tampilRowAbsensi(arg0, pertemuanPunyaGrupPertemuan, statusabsensis, status, tbmuser);
		}
	}

	/** Perender baris grid pengajuan izin/sakit: kontrol persetujuan, identitas mahasiswa, keterangan, lampiran, dan tombol hapus. */
	class MahasiswaIzinRenderer extends ais.ui.util.MyRowRenderer {

		private Tbmuser tbmuser;

		public MahasiswaIzinRenderer() {
			tbmuser = Common.getCurrentUser();
		}

		/**
		 * Merender satu baris {@link PengajuanIzinTidakMasukPerkuliahan}: checkbox
		 * "Setujui" (staf saja; saat dicentang/dilepas, menyimpan persetujuan DAN
		 * menyalin status absensi pengajuan ke {@link Pertemuan} terkait via
		 * {@link Pertemuan#populate}) atau label Ya/Tidak (mahasiswa), identitas dan
		 * foto mahasiswa, status+keterangan pengajuan, lampiran unduh/unggah, dan
		 * tombol hapus (terlihat hanya untuk pengaju sendiri atau staf, dan HANYA bila
		 * pengajuan BELUM disetujui — mencegah penghapusan setelah disetujui tanpa
		 * membatalkan dulu).
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan = (PengajuanIzinTidakMasukPerkuliahan) arg1;

			if (tbmuser.getMahasiswa() == null || mahasiswaBolehUbahAbsen) {
				final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Setujui");
				checkboxConfig.setChecked(pengajuanIzinTidakMasukPerkuliahan.getDiizinkan());
				checkboxConfig.setParent(arg0);
				checkboxConfig.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pengajuanIzinTidakMasukPerkuliahan.setDiizinkan(checkboxConfig.isChecked());
						Common.refreshSaveOrUpdate(pengajuanIzinTidakMasukPerkuliahan);
						Pertemuan pertemuan = pengajuanIzinTidakMasukPerkuliahan.getPertemuan();
						pertemuan.populate(pengajuanIzinTidakMasukPerkuliahan.getMahasiswa().getId(),
								pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi(),
								pengajuanIzinTidakMasukPerkuliahan.getKeterangan(), null, pertemuan.getWaktuMulai(),
								pertemuan.getWaktuSelesai(), "Mahasiswa");
						Common.refreshSaveOrUpdate(pertemuan);

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								reloadAbsensi();
								reloadIzinAbsensi();
							}
						});
					}
				});
			} else {
				new MyLabelConfig(pengajuanIzinTidakMasukPerkuliahan.getDiizinkan() ? "Ya" : "Tidak").setParent(arg0);
			}

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(pengajuanIzinTidakMasukPerkuliahan.getMahasiswa()).setParent(hbox);

			Vbox vbox = new Vbox();
			vbox.setHeight("100%");
			vbox.setWidth("100%");
			vbox.setParent(hbox);
			new Label(pengajuanIzinTidakMasukPerkuliahan.getMahasiswa().getNim() + " - "
					+ pengajuanIzinTidakMasukPerkuliahan.getMahasiswa().getNama()).setParent(vbox);

			vbox = new Vbox();
			vbox.setHeight("100%");
			vbox.setWidth("100%");
			vbox.setParent(arg0);

			new Label("Status : "
					+ Common.getBahasaConfig(pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi().getNama()))
							.setParent(vbox);

			new Label(pengajuanIzinTidakMasukPerkuliahan.getKeterangan()).setParent(vbox);

			Vbox myVbox = new Vbox();
			myVbox.setParent(vbox);

			hbox = new Hbox();
			hbox.setParent(myVbox);
			LampiranLain.createDownloadUploadFileLain(hbox, pengajuanIzinTidakMasukPerkuliahan.getId(),
					LampiranLain.IZIN_TIDAK_MASUK, "Lampiran", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, false);

			Hbox tombol = new Hbox();
			tombol.setParent(vbox);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(!pengajuanIzinTidakMasukPerkuliahan.getDiizinkan() && ((tbmuser.getMahasiswa() != null
					&& tbmuser.getMahasiswa().getId().equals(pengajuanIzinTidakMasukPerkuliahan.getMahasiswa().getId()))
					|| tbmuser.getMahasiswa() == null || mahasiswaBolehUbahAbsen));
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
											Common.refreshDelete(pengajuanIzinTidakMasukPerkuliahan);
											reloadIzinAbsensi();
											reloadAbsensi();
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}
			});
			button.setParent(arg0);
		}
	}

	/** Delegasi tanpa efek samping tambahan ke {@link AbsensiHelper#createStatusKehadiran(Mahasiswa, Pertemuan, Mahasiswa, BiodataCalonMahasiswa, EventListener, boolean)} untuk menampilkan status kehadiran asisten. */
	public static Component createStatusKehadiran(final Mahasiswa asisten, final Pertemuan pertemuan,
			Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return AbsensiHelper.createStatusKehadiran(asisten, pertemuan, mahasiswa, biodataCalonMahasiswa,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub

					}
				}, false);
	}

	/** Delegasi tanpa efek samping tambahan ke {@link AbsensiHelper#boleh} untuk menentukan komponen kontrol status kehadiran dosen yang boleh diedit. */
	public static Component boleh(Statusabsensi statusabsensi, final Pertemuan pertemuan, final Dosen dosen) {
		return AbsensiHelper.boleh(statusabsensi, pertemuan, dosen, null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

			}
		});
	}

	/**
	 * Menampilkan status kehadiran {@code dosen} pada {@code pertemuan}: label statis
	 * bila jadwal input kehadiran dosen dibatasi ({@code kehadiranDosenHarusDiinputSesuaiJadwal})
	 * dan waktu saat ini di luar jendela waktu pertemuan, ATAU bila konteks tampilan
	 * adalah milik mahasiswa/calon mahasiswa/peserta kursus/siswa (baca saja); selain
	 * itu, kontrol yang dapat diedit lewat {@link #boleh}.
	 *
	 * @param dosen                 dosen yang statusnya ditampilkan; {@code null} menghasilkan label kosong
	 * @param pertemuan             pertemuan terkait
	 * @param mahasiswa             konteks tampilan mahasiswa, boleh {@code null}
	 * @param biodataCalonMahasiswa konteks tampilan calon mahasiswa, boleh {@code null}
	 * @return komponen status kehadiran (label atau kontrol edit)
	 */
	public static Component createStatusKehadiran(final Dosen dosen, final Pertemuan pertemuan, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa) {
		if (dosen == null) {
			return new Label();
		}

		Statusabsensi statusabsensi = null;

		if (pertemuan.getId() != null) {
			statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
					pertemuan.retreiveAbsensiId(dosen.getId()));
		}

		Date curreDate = ais.ui.util.WaktuUtil.getDate();
		Date mulai = null;
		Date selesai = null;
		try {
			mulai = Common.timeFormat2.get().parse(pertemuan.getWaktuMulai());
			selesai = Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiGrupPertemuanHelper.java:874");

		}
		Tbmuser tbmuser = Common.getCurrentUser();
		if (pertemuan != null && pertemuan.getPerkuliahan() != null) {
			
			if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
					&& pertemuan.getPerkuliahan().getKehadiranDosenHarusDiinputSesuaiJadwal()
					&& ((mulai != null && curreDate.before(mulai) || (selesai != null && curreDate.after(selesai))))) {
				return new Label(statusabsensi == null ? "-" : Common.getBahasaConfig(statusabsensi.getNama()));
			}
		}

		if (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null && tbmuser.getSiswa()==null) {
			return boleh(statusabsensi, pertemuan, dosen);
		} else {
			return new Label(statusabsensi == null ? "-" : Common.getBahasaConfig(statusabsensi.getNama()));
		}

	}
}
