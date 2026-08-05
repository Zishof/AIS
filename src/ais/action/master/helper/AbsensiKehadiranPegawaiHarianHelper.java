package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonPayroll;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.Tbmuser;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.payroll.DetailJenisShiftPegawai;
import ais.database.model.payroll.ItemGaji;
import ais.database.model.payroll.JenisShiftPunyaPegawai;
import ais.database.model.payroll.LiburNasional;
import ais.database.model.payroll.LiburRutin;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyHtml;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AbsensiKehadiranPegawaiHarianHelper extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8823784546257272901L;
	private Combobox bulan;
	private Combobox tahun;
	private MyGrid grid;
	private Pegawai pegawai;
	private boolean edit = false;

	public AbsensiKehadiranPegawaiHarianHelper(Pegawai pegawai) {
		this.pegawai = pegawai;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(AbsensiKehadiranPegawaiHarianHelper.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	public Groupbox display() {
		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled(Common.getBahasaConfig("Daftar absensi pegawai")));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Bulan : ")));
		toolbar.appendChild(bulan = new Combobox());
		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulan.appendChild(comboitem);
		}

		Common.selectComboItem(bulan, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);

		bulan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		bulan.setReadonly(true);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun : ")));
		toolbar.appendChild(tahun = new Combobox());

		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			Comboitem comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}

		Common.selectComboItem(tahun, currTahun);

		tahun.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		tahun.setReadonly(true);

		MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Singkronkan", "/img/svg/check2.svg");
		toolbar.appendChild(cetakSksDosen);
		cetakSksDosen.addEventListener("onClick", new EventListener() {

		    @Override
		    public void onEvent(Event arg0) throws Exception {
		        Common.createDefaultTimer(new EventListener() {

		            @Override
		            public void onEvent(Event arg0) throws Exception {

		                final Integer bulan = (Integer) (AbsensiKehadiranPegawaiHarianHelper.this.bulan.getSelectedItem() == null 
		                        ? null : AbsensiKehadiranPegawaiHarianHelper.this.bulan.getSelectedItem().getValue());

		                final Integer tahun = (Integer) (AbsensiKehadiranPegawaiHarianHelper.this.tahun.getSelectedItem() == null 
		                        ? null : AbsensiKehadiranPegawaiHarianHelper.this.tahun.getSelectedItem().getValue());

		                if (bulan == null) {
		                    MyMessageboxConfig.show("Mohon maaf, bulan belum dipilih. Silakan pilih bulan terlebih dahulu, kemudian ulangi proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		                    return;
		                }
		                if (tahun == null) {
		                    MyMessageboxConfig.show("Mohon maaf, tahun belum dipilih. Silakan pilih tahun terlebih dahulu, kemudian ulangi proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		                    return;
		                }

		                // Menggunakan AtomicBoolean sebagai flag Thread-Safe, BUKAN komponen UI (Label)
		                final AtomicBoolean isProsesSelesai = new AtomicBoolean(false);

		                // Tampilkan loading UI di awal sebelum Thread berjalan
		                Clients.showBusy("Proses singkronisasi shift...");

		                new Thread(new Runnable() {
		                    @Override
		                    public void run() {
		                        Session session = null;
		                        try {
		                            // 1. Buka Session SATU KALI saja di luar loop untuk efisiensi
		                            session = HibernateUtil.currentNativeSession();

		                            Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		                            calendar.set(Calendar.MONTH, bulan - 1);
		                            calendar.set(Calendar.YEAR, tahun);
		                            calendar.set(Calendar.DATE, 1);

		                            int jumlahHari = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

		                            for (int i = 1; i <= jumlahHari; i++) {
		                                calendar.set(Calendar.DATE, i);
		                                Date tanggal = calendar.getTime();
		                                String hari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];

		                                Transaction tx = null;
		                                try {
		                                    // 2. Mulai transaksi untuk tiap iterasi hari
		                                    tx = session.beginTransaction();

		                                    StatuskehadiranKaryawanHarian statusKehadiran = CommonPayroll
		                                            .getDefaultStatuskehadiranKaryawanHarian(tanggal, pegawai, null, null,
		                                                    "", "", session, true);
		                                    
		                                    session.refresh(statusKehadiran);

		                                    Date waktuMasuk = statusKehadiran.ambilMasukjam() == null ? tanggal : statusKehadiran.ambilMasukjam();
		                                    boolean isLiburNasional = statusKehadiran.getLiburNasional() != null;

		                                    DetailJenisShiftPegawai jenis = CommonPayroll.getDetailJenisShiftPegawai(
		                                            pegawai, null, null, waktuMasuk, statusKehadiran.getTanggal(), 
		                                            hari, isLiburNasional);

		                                    statusKehadiran.setDetailJenisShiftPegawai(jenis);

		                                    Common.refreshSaveOrUpdate(session, statusKehadiran);
		                                    CommonPayroll.simpanDetail(session, statusKehadiran, true);

		                                    // 3. Commit data per hari -- GUARD: hanya commit bila transaksi masih
		                                    // benar-benar aktif. Root cause bug lama: Common.refreshSaveOrUpdate bisa
		                                    // menelan exception (mis. pelanggaran constraint NOT NULL yang bukan
		                                    // "unique constraint") lalu diam-diam rollback transaksi tanpa melempar
		                                    // ulang; atau CommonPayroll.simpanDetail/getDefaultStatuskehadiranKaryawanHarian
		                                    // (baru=true) sempat begin+commit ulang transaksi yang SAMA (session sama)
		                                    // sehingga tx di sini sudah tidak aktif lagi saat commit() dipanggil ->
		                                    // "TransactionException: Transaction not successfully started" (gejala jauh
		                                    // dari akar masalah, pola sama seperti bug kodeunik null di Kegiatan/Mandiri).
		                                    if (tx != null && tx.isActive()) {
		                                        tx.commit();
		                                    } else {
		                                        ais.common.ErrorAuditUtil.record(
		                                                new org.hibernate.TransactionException(
		                                                        "Transaksi tidak aktif saat hendak commit shift harian tanggal "
		                                                                + tanggal + " -- kemungkinan sudah di-rollback/di-commit lebih awal akibat error yang tertelan sebelumnya."),
		                                                "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:228");
		                                    }

		                                    // 4. MEMORY OPTIMIZATION: Bersihkan L1 Cache agar RAM tidak membengkak
		                                    session.flush();
		                                    session.clear();

		                                } catch (Exception e) {
		                                    // Cegah data korup jika terjadi error pada hari tertentu
		                                    if (tx != null && tx.isActive()) {
		                                        tx.rollback();
		                                    }
		                                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:239");
		                                    // Loop tetap lanjut memproses hari berikutnya meskipun hari ini error
		                                }
		                            }
		                        } catch (Exception e) {
		                            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:244");
		                        } finally {
		                            // 5. PASTIKAN session ditutup di blok finally untuk mencegah Connection Leak
		                            try {
		                                if (session != null && session.isOpen()) {
		                                    session.disconnect();
		                                    session.close();
		                                }
		                                HibernateUtil.closeSession(); // Jaga-jaga jika utility class butuh dipanggil
		                            } catch (Exception ex) {
		                                ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:254");
		                            } finally {
		                                // Tandai proses background telah sepenuhnya selesai
		                                isProsesSelesai.set(true);
		                            }
		                        }
		                    }
		                }).start();

		                // Timer UI untuk mengecek status background Thread
		                final Timer timer = new Timer(500);
		                timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		                timer.setRepeats(true);
		                timer.addEventListener("onTimer", new EventListener() {

		                    @Override
		                    public void onEvent(Event arg0) throws Exception {
		                        // Jika isProsesSelesai == true, berarti blok finally pada Thread sudah dieksekusi
		                        if (isProsesSelesai.get()) {
		                            Clients.clearBusy();
		                            loadData(null);
		                            timer.detach();
		                        }
		                    }
		                });
		                timer.start();

		            }
		        });
		    }
		});

		Toolbarbutton button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();
		grid.setMold("paging");
		grid.setSclass("fgrid");
		grid.setPageSize(100);
		grid.getPagingChild().setMold("os");
		grid.setPagingPosition("top");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setWidth("40px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Masuk/Pulang");
		column.setWidth("9%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jam {" + ItemGaji.V_JAM + "}");
		column.setWidth("9%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Shift");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Lembur {" + ItemGaji.V_LEM + "}");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Cepat {" + ItemGaji.V_CEP + "}");
		column.setWidth("9%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Terlambat {" + ItemGaji.V_TERL + "}");
		column.setWidth("9%");

		column = new Column("Abaikan Jarak");
		column.setParent(columns);
		column.setWidth("0px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Aksi");
		column.setAlign("center");
		column.setWidth("90px");

		try {
			loadData(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return groupbox;
	}

	private void editJam(final StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarianTemp) throws Exception {
		final MyWindow window = new MyWindow("Ubah Waktu Kehadiran", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("95%");
		window.setWidth("600px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai"));
		row.appendChild(new Label(statuskehadiranKaryawanHarianTemp.getPegawai() == null ? ""
				: statuskehadiranKaryawanHarianTemp.getPegawai().getNama()));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari / Tanggal"));
		row.appendChild(new Label(Common.dateFormat6.get().format(statuskehadiranKaryawanHarianTemp.getTanggal())));

		List<Statusabsensi> statusabsensis = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(Statusabsensi.class)
						.add(Restrictions.or(Restrictions.eq("aktif", true),
								Restrictions.in("id", new Long[] { 1L, 3L, 4L, 5L })))
						.addOrder(Order.asc("nama")),
				Statusabsensi.class);
		final Combobox absen = new Combobox();
		absen.setWidth("90%");
		absen.setReadonly(true);
		Common.insertComboItems(absen, "nama", statusabsensis);
		Common.selectComboItem(absen, statuskehadiranKaryawanHarianTemp.getStatusabsensi());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kehadiran"));
		if (statuskehadiranKaryawanHarianTemp.getCutiDanIzin() != null
				&& statuskehadiranKaryawanHarianTemp.getCutiDanIzin().getSetujui()) {
			row.appendChild(new Label(statuskehadiranKaryawanHarianTemp.getStatusabsensi() == null ? ""
					: statuskehadiranKaryawanHarianTemp.getStatusabsensi().getNama()));
		} else {
			absen.setParent(row);
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		final MyCheckboxConfig tidakAdaKehadiran;
		row.appendChild(tidakAdaKehadiran = new MyCheckboxConfig("Tidak ada kehadiran"));
		tidakAdaKehadiran.setChecked(statuskehadiranKaryawanHarianTemp.getTidakAdaKehadiran());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jam datang"));
		final MyTimebox datang = new MyTimebox(statuskehadiranKaryawanHarianTemp.getMasukjamManual());
		row.appendChild(datang);
		if (datang.getValue() == null) {
			datang.setValue(statuskehadiranKaryawanHarianTemp.getMasukjam());
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		final MyCheckboxConfig tidakAdaKedatangan;
		row.appendChild(tidakAdaKedatangan = new MyCheckboxConfig("Tidak ada kedatangan"));
		tidakAdaKedatangan.setChecked(statuskehadiranKaryawanHarianTemp.getTidakAdaKedatangan());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jam pulang"));
		final MyTimebox pulang = new MyTimebox(statuskehadiranKaryawanHarianTemp.getPulangJamManual());
		row.appendChild(pulang);
		// Utamakan jam pulang AKTUAL untuk ditampilkan di form edit:
		//   (a) KOREKSI shadow: bila jam pulang tersimpan (State/Manual) lebih AWAL dari scan pulang
		//       genuine terakhir (mis. tersimpan 07:31 padahal scan pulang asli 15:38), pakai scan asli.
		//   (b) Selain itu getPulangJam(); lalu fallback riwayat absensi online / keterangan.
		// Aturan durasi kerja minimal shift TIDAK diubah (koreksi hanya saat durasi > minimal). Operator
		// dapat melihat jam pulang yang benar lalu menekan Simpan untuk menyimpannya permanen.
		Date pulangTampil = statuskehadiranKaryawanHarianTemp.ambilPulangUntukTampil();
		if (pulangTampil != null) {
			pulang.setValue(pulangTampil);
		} else if (pulang.getValue() == null) {
			pulang.setValue(statuskehadiranKaryawanHarianTemp.getPulangJam());
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		final MyCheckboxConfig tidakAdaKepulangan;
		row.appendChild(tidakAdaKepulangan = new MyCheckboxConfig("Tidak ada kepulangan"));
		tidakAdaKepulangan.setChecked(statuskehadiranKaryawanHarianTemp.getTidakAdaKepulangan());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		final MyTextbox keterangan = new MyTextbox(statuskehadiranKaryawanHarianTemp.getKeterangan());
		row.appendChild(keterangan);
		keterangan.setWidth("95%");
		keterangan.setRows(10);
		final MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Statusabsensi statusabsensi = (Statusabsensi) (absen.getSelectedItem() == null ? null
						: absen.getSelectedItem().getValue());
				boolean masuk = statusabsensi != null && ConstantValues.MASUK != null
						&& statusabsensi.getId().equals(ConstantValues.MASUK.getId());

				datang.setDisabled(tidakAdaKedatangan.isChecked() || !masuk || tidakAdaKehadiran.isChecked());
				pulang.setDisabled(tidakAdaKepulangan.isChecked() || !masuk || tidakAdaKehadiran.isChecked());

				tidakAdaKedatangan.setDisabled(!masuk || tidakAdaKehadiran.isChecked());
				tidakAdaKepulangan.setDisabled(!masuk || tidakAdaKehadiran.isChecked());

				boolean tidakMasuk = statusabsensi != null && ConstantValues.BELUM_ABSEN != null
						&& statusabsensi.getId().equals(ConstantValues.BELUM_ABSEN.getId());

				save.setVisible(!tidakMasuk);

			}
		};

		keterangan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

				statuskehadiranKaryawanHarianTemp.setKeterangan(keterangan.getValue());

				if (ConstantValues.aktifkanFingerPrintOtomatisDariKeterangan) {
					Date m = statuskehadiranKaryawanHarianTemp.mulaiOtomatisUlangAbsenDariKeterangan();
					if (m != null) {
						datang.setValue(m);
						datang.setDisabled(true);
					} else {
						datang.setDisabled(false);
					}

					m = statuskehadiranKaryawanHarianTemp.sampaiOtomatisUlangAbsenDariKeterangan();
					if (m != null) {
						pulang.setValue(m);
						pulang.setDisabled(true);
					} else {
						pulang.setDisabled(false);
					}
				}
			}
		});

		tidakAdaKedatangan.addEventListener("onClick", eventListener);
		absen.addEventListener("onChange", eventListener);
		tidakAdaKehadiran.addEventListener("onClick", eventListener);
		tidakAdaKepulangan.addEventListener("onClick", eventListener);
		eventListener.onEvent(null);

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
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		// Aksi cepat: tandai record ini sebagai HANYA KEPULANGAN (tanpa kedatangan), isi jam pulang dari
		// scan pulang asli, lalu Simpan — untuk kasus pegawai yang tidak absen datang tetapi scan pertamanya
		// terlanjur tercatat sebagai kedatangan.
		final MyToolbarbuttonConfig hanyaPulang = new MyToolbarbuttonConfig("Jadikan Hanya Kepulangan",
				"/img/save.gif");
		hanyaPulang.setTooltiptext("Tandai tanpa kedatangan, isi jam pulang dari scan pulang, lalu simpan");
		hanyaPulang.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				// 1) Tandai TIDAK ADA KEDATANGAN & kosongkan jam datang.
				tidakAdaKedatangan.setChecked(true);
				datang.setValue(null);
				// 2) Pastikan jam pulang terisi dari scan pulang asli (koreksi shadow / riwayat).
				if (pulang.getValue() == null) {
					Date pulangAsli = statuskehadiranKaryawanHarianTemp.ambilPulangUntukTampil();
					if (pulangAsli != null) {
						pulang.setValue(pulangAsli);
					}
				}
				// 3) Segarkan status aktif/nonaktif field sesuai centang.
				eventListener.onEvent(null);
				// 4) Jalankan aksi Simpan (commit) memakai listener tombol Simpan yang sudah ada.
				org.zkoss.zk.ui.event.Events.sendEvent(new Event("onClick", save));
			}
		});
		hanyaPulang.setParent(toolbar);

		save.setTooltiptext("Proses");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				Session session = HibernateUtil.currentNativeSession();
				StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = (StatuskehadiranKaryawanHarian) (statuskehadiranKaryawanHarianTemp
						.getId() == null
								? null
								: session.createCriteria(StatuskehadiranKaryawanHarian.class)
										.add(Restrictions.idEq(statuskehadiranKaryawanHarianTemp.getId()))
										.setMaxResults(1).uniqueResult());
				if (statuskehadiranKaryawanHarian == null) {
					statuskehadiranKaryawanHarian = new StatuskehadiranKaryawanHarian();
					statuskehadiranKaryawanHarian.setTanggal(statuskehadiranKaryawanHarianTemp.getTanggal());
					statuskehadiranKaryawanHarian.setPegawai(statuskehadiranKaryawanHarianTemp.getPegawai());
					statuskehadiranKaryawanHarian.setDosen(statuskehadiranKaryawanHarianTemp.getDosen());
					statuskehadiranKaryawanHarian.setMahasiswa(statuskehadiranKaryawanHarianTemp.getMahasiswa());
					statuskehadiranKaryawanHarian.setGuru(statuskehadiranKaryawanHarianTemp.getGuru());
					statuskehadiranKaryawanHarian.setMinggu(statuskehadiranKaryawanHarianTemp.getMinggu());
					statuskehadiranKaryawanHarian
							.setLiburNasional(statuskehadiranKaryawanHarianTemp.getLiburNasional());
					statuskehadiranKaryawanHarian.setLiburRutin(statuskehadiranKaryawanHarianTemp.getLiburRutin());
				}

				Statusabsensi statusabsensi = (Statusabsensi) (absen.getSelectedItem() == null ? null
						: absen.getSelectedItem().getValue());
				if (statusabsensi != null) {
					statuskehadiranKaryawanHarian.setStatusabsensi(statusabsensi);
				}

				if (statuskehadiranKaryawanHarianTemp != null) {
					statuskehadiranKaryawanHarianTemp.setMasukjamManual(datang.getValue());
					statuskehadiranKaryawanHarianTemp.setTidakAdaKedatangan(tidakAdaKedatangan.isChecked());
					statuskehadiranKaryawanHarianTemp.setPulangJamManual(pulang.getValue());
					statuskehadiranKaryawanHarianTemp.setTidakAdaKepulangan(tidakAdaKepulangan.isChecked());
					statuskehadiranKaryawanHarianTemp.setKeterangan(keterangan.getValue().trim());
					statuskehadiranKaryawanHarianTemp.setTidakAdaKehadiran(tidakAdaKehadiran.isChecked());
					statuskehadiranKaryawanHarianTemp.setMasukjamState(datang.getValue()); 
					statuskehadiranKaryawanHarianTemp.setPulangJamState(pulang.getValue()); 
				}

				statuskehadiranKaryawanHarian.setMasukjamState(datang.getValue()); 
				statuskehadiranKaryawanHarian.setPulangJamState(pulang.getValue()); 
				
				statuskehadiranKaryawanHarian.setMasukjamManual(datang.getValue());
				statuskehadiranKaryawanHarian.setTidakAdaKedatangan(tidakAdaKedatangan.isChecked());
				statuskehadiranKaryawanHarian.setPulangJamManual(pulang.getValue());
				statuskehadiranKaryawanHarian.setTidakAdaKepulangan(tidakAdaKepulangan.isChecked());
				statuskehadiranKaryawanHarian.setKeterangan(keterangan.getValue().trim());
				statuskehadiranKaryawanHarian.setTidakAdaKehadiran(tidakAdaKehadiran.isChecked());
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, statuskehadiranKaryawanHarian);
				session.getTransaction().commit();
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
				HibernateUtil.closeSession();

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						window.detach();

						loadData(null);

					}
				});
			}
		});
		save.setParent(toolbar);

		window.onModal();

		if (ConstantValues.aktifkanFingerPrintOtomatisDariKeterangan) {
			Date m = statuskehadiranKaryawanHarianTemp.mulaiOtomatisUlangAbsenDariKeterangan();
			if (m != null) {
				datang.setValue(m);
				datang.setDisabled(true);
			} else {
				datang.setDisabled(false);
			}

			m = statuskehadiranKaryawanHarianTemp.sampaiOtomatisUlangAbsenDariKeterangan();
			if (m != null) {
				pulang.setValue(m);
				pulang.setDisabled(true);
			} else {
				pulang.setDisabled(false);
			}
		}

	}

	@SuppressWarnings("unchecked")
	public void loadData(Object object) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Tbmuser tbmuser = Common.getCurrentUser();

				Integer bulan = (Integer) (AbsensiKehadiranPegawaiHarianHelper.this.bulan.getSelectedItem() == null
						? null
						: AbsensiKehadiranPegawaiHarianHelper.this.bulan.getSelectedItem().getValue());

				Integer tahun = (Integer) (AbsensiKehadiranPegawaiHarianHelper.this.tahun.getSelectedItem() == null
						? null
						: AbsensiKehadiranPegawaiHarianHelper.this.tahun.getSelectedItem().getValue());

				if (bulan == null) {
					MyMessageboxConfig.show("Mohon maaf, bulan belum dipilih. Silakan pilih bulan terlebih dahulu, kemudian ulangi proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (tahun == null) {
					MyMessageboxConfig.show("Mohon maaf, tahun belum dipilih. Silakan pilih tahun terlebih dahulu, kemudian ulangi proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.MONTH, bulan - 1);
				calendar.set(Calendar.YEAR, tahun);
				calendar.set(Calendar.DATE, 1);

				int jumlahHari = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

				Calendar mulai = Calendar.getInstance();
				mulai.set(Calendar.YEAR, tahun);
				mulai.set(Calendar.MONTH, bulan - 1);
				mulai.set(Calendar.DATE, 1);

				Calendar sampai = Calendar.getInstance();
				sampai.set(Calendar.YEAR, tahun);
				sampai.set(Calendar.MONTH, bulan - 1);
				sampai.set(Calendar.DATE, jumlahHari);

				Session sessionmy = HibernateUtil.currentNativeSession();

				List<CutiDanIzin> cutiDanIzins = sessionmy.createCriteria(CutiDanIzin.class)
						.addOrder(Order.asc("mulai"))

						.add(Restrictions.or(Restrictions.between("mulai", mulai.getTime(), sampai.getTime()),
								Restrictions.between("sampai", mulai.getTime(), sampai.getTime())))

						.add(Restrictions.eq("pegawai", pegawai)).add(Restrictions.eq("setujui", true)).list();

//				List<Statusabsensi> statusabsensis = ConstantValues.simpleList(sessionmy
//						.createCriteria(Statusabsensi.class)
//						.add(Restrictions.or(Restrictions.eq("aktif", true),
//								Restrictions.in("id", new Long[] { 1L, 3L, 4L, 5L })))
//						.addOrder(Order.asc("nama")), Statusabsensi.class);

//				System.out.println("cutiDanIzins size " + cutiDanIzins.size() + ", jumlahHari -> " + jumlahHari
//						+ ", statusabsensis -> " + statusabsensis.size());

				Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
				grid.appendChild(rows);
				rows.setParent(grid);
				Common.clear(rows);

				Map<String, StatuskehadiranKaryawanHarian> mapStatuskehadiranKaryawanHarian = CommonPayroll
						.getDefaultStatuskehadiranKaryawanHarian(cutiDanIzins, bulan, tahun, pegawai, sessionmy, false);
				sessionmy.disconnect();
				sessionmy.close();
				HibernateUtil.closeSession();

				// === AUTO-ISI JAM PULANG DARI RIWAYAT ABSENSI ONLINE ===
				// Untuk record yang jam pulang-nya KOSONG padahal ada scan pulang online, dan durasi
				// kerja aktual (scan online pertama s.d terakhir) MELEBIHI "Waktu minimal bekerja (jam)"
				// pada shift, jam pulang diisi & disimpan otomatis (setPulangJamState + commit).
				// Memakai session terpisah dengan pola openSession + finally agar tidak bocor. Record
				// diproses saat sudah detached dari session baca, sehingga aman di-set di memori (untuk
				// tampilan) sekaligus dipersist lewat session khusus ini.
				Session sesiPulangOtomatis = null;
				try {
					int jmlDiproses = 0;
					int jmlTerisi = 0;
					sesiPulangOtomatis = HibernateUtil.openSession();
					for (StatuskehadiranKaryawanHarian skh : mapStatuskehadiranKaryawanHarian.values()) {
						if (skh != null && skh.getId() != null) {
							jmlDiproses++;
							Date hasilPulang = skh.autoUpdatePulangDariSejarah(sesiPulangOtomatis);
							if (hasilPulang != null) {
								jmlTerisi++;
							}
							// KOREKSI shadow: jam pulang tersimpan (State/Manual) lebih awal dari scan pulang
							// genuine terakhir (mis. tersimpan 07:31 padahal scan pulang asli 15:38 masuk lewat
							// jalur temp). Perbaiki agar kolom "Jam Pulang" & penggajian memakai scan asli.
							Date hasilKoreksi = skh.autoKoreksiPulangShadow(sesiPulangOtomatis);
							if (hasilKoreksi != null) {
								jmlTerisi++;
							}
						}
					}
					System.out.println("[AUTO-PULANG] pegawai="
							+ (pegawai == null ? "" : pegawai.getNama()) + " bulan=" + bulan + "/" + tahun
							+ " diproses=" + jmlDiproses + " jamPulangTerisiOtomatis=" + jmlTerisi);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:777");
				} finally {
					if (sesiPulangOtomatis != null) {
						try {
							if (sesiPulangOtomatis.isOpen()) {
								sesiPulangOtomatis.clear();
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:784");
						}
						try {
							sesiPulangOtomatis.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:788");
						}
						try {
							if (sesiPulangOtomatis.isOpen()) {
								sesiPulangOtomatis.close();
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:794");
						}
					}
				}

//				System.out.println("mapStatuskehadiranKaryawanHarian -> " + mapStatuskehadiranKaryawanHarian.size());

				for (int i = 1; i <= jumlahHari; i++) {

					try {
						calendar.set(Calendar.DATE, i);

						final Date tanggal = calendar.getTime();
						final Integer bln = calendar.get(Calendar.MONTH) + 1;
						final Integer thn = calendar.get(Calendar.YEAR);
						final Integer tgl = calendar.get(Calendar.DATE);
						final Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

						final StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = mapStatuskehadiranKaryawanHarian
								.get(Common.dateFormat83.get().format(tanggal));
						LiburRutin liburRutin = statuskehadiranKaryawanHarian.getLiburRutin();
						LiburNasional liburNasional = statuskehadiranKaryawanHarian.getLiburNasional();

						MyFormRow row = new MyFormRow();
						row.setValign("top");

						List<String> urls = new ArrayList<String>();
						if (!statuskehadiranKaryawanHarian.getLokasiAbsenDatang().isEmpty()) {
							urls.add(statuskehadiranKaryawanHarian.getLokasiAbsenDatang());
						}
						if (!statuskehadiranKaryawanHarian.getFotoAbsenDatang().isEmpty()) {
							urls.add(statuskehadiranKaryawanHarian.getFotoAbsenDatang());
						}

						List<String> urlsPulang = new ArrayList<String>();
						if (!statuskehadiranKaryawanHarian.getLokasiAbsenPulang().isEmpty()) {
							urlsPulang.add(statuskehadiranKaryawanHarian.getLokasiAbsenPulang());
						}
						if (!statuskehadiranKaryawanHarian.getFotoAbsenPulang().isEmpty()) {
							urlsPulang.add(statuskehadiranKaryawanHarian.getFotoAbsenPulang());
						}

						String sebelumnya = statuskehadiranKaryawanHarian.retreive("sejarah");

						if (urls.isEmpty() && urlsPulang.isEmpty() && (sebelumnya == null || sebelumnya.isEmpty())) {
							new MyLabelAgakKecil().setParent(row);
						} else {

							MyDetail detail = new MyDetail();
							detail.setParent(row);
							detail.setOpen(false);

							Vbox vbox = new Vbox();
							vbox.setParent(detail);

							MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
							groupboxStyled.setParent(vbox);

							groupboxStyled.appendChild(new MyCaptionStyled("Info Kedatangan"));

							Box box = Common.isMobile() ? new Vbox() : new Hbox();
							box.setWidth("100%");
							box.setParent(groupboxStyled);

							for (String u : urls) {
								if (u.contains("iframe")) {
									MyHtml myHtml = new MyHtml(u);
									box.appendChild(myHtml);
								} else if (u.contains("maps")) {
									MyHtml myHtml = new MyHtml(
											"<iframe style=\"width:100%;height:200px\" frameborder=\"0\" scrolling=\"no\" marginheight=\"0\"  marginwidth=\"0\" src=\""
													+ u + "&amp;output=embed\"></iframe>");
									box.appendChild(myHtml);
								} else if (u.contains("download")) {
									MyHtml myHtml = new MyHtml("<a onclick=\"popupCenter({url: '" + u
											+ "', title: 'Foto', w: 1200, h: 600});\" ><image style=\"height:200px;\" src=\""
											+ u + "\"></image></a>");
									box.appendChild(myHtml);
								}
							}

							groupboxStyled = new MyGroupboxStyled();
							groupboxStyled.setParent(vbox);

							groupboxStyled.appendChild(new MyCaptionStyled("Info Kepulangan"));

							box = Common.isMobile() ? new Vbox() : new Hbox();
							box.setWidth("100%");
							box.setParent(groupboxStyled);

							for (String u : urlsPulang) {
								if (u.contains("maps")) {
									MyHtml myHtml = new MyHtml(
											"<iframe style=\"width:100%;height:200px\" frameborder=\"0\" scrolling=\"no\" marginheight=\"0\"  marginwidth=\"0\" src=\""
													+ u + "&amp;output=embed\"></iframe>");
									box.appendChild(myHtml);
								} else if (u.contains("download")) {
									MyHtml myHtml = new MyHtml("<a onclick=\"popupCenter({url: '" + u
											+ "', title: 'Foto', w: 1200, h: 600});\" ><image style=\"height:200px;\" src=\""
											+ u + "\"></image></a>");
									box.appendChild(myHtml);
								}
							}

							TreeMap<String, Map<String, String>> maps = statuskehadiranKaryawanHarian.ambilSejarah();

							System.out.println("hari -> " + i + " maps -> " + maps);

							if (!maps.isEmpty()) {
								groupboxStyled = new MyGroupboxStyled();
								groupboxStyled.setParent(vbox);

								groupboxStyled.appendChild(new MyCaptionStyled("Sejarah Absensi Online"));

								Grid grid = new Grid();
								grid.setSclass("dgrid");
								grid.setWidth("100%");
								grid.setParent(groupboxStyled);
								grid.setWidth("100%");
								grid.setHeight("100%");
								grid.setSclass("dgrif");

								Columns columns = new Columns();
								columns.setParent(grid);

								MyColumnConfig column = new MyColumnConfig("Tanggal");
								column.setParent(columns);
								column.setWidth("10%");

								column = new MyColumnConfig("Info");
								column.setParent(columns);

								column = new MyColumnConfig("Foto");
								column.setParent(columns);

								column = new MyColumnConfig("Lokasi");
								column.setParent(columns);

								Rows rowsData = new Rows();
								rowsData.setParent(grid);

								for (String key : maps.keySet()) {
									try {
										MyFormRow rowData = new MyFormRow();
										rowData.setValign("top");
										rowData.setParent(rowsData);
										try {
											rowData.appendChild(new MyLabelAgakKecil(
													Common.dateFormat5.get().format(Common.dateFormat9.get().parse(key))));
										} catch (Exception e) {
											rowData.appendChild(new MyLabelAgakKecil());
										}
										rowData.appendChild(new MyHtml(maps.get(key).containsKey(key + "_info")
												? "<div style='font-size:10px;'>" + maps.get(key).get(key + "_info")
														+ "</div>"
												: ""));
										A a;
										rowData.appendChild(a = new A(maps.get(key).containsKey(key + "_foto")
												? maps.get(key).get(key + "_foto")
												: ""));
										a.addEventListener("onClick", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Clients.evalJavaScript(
														"popupCenter({url: '" + ((A) arg0.getTarget()).getLabel()
																+ "', title: 'Data', w: 1200, h: 600});");
											}
										});

										rowData.appendChild(a = new A(maps.get(key).containsKey(key + "_lokasi")
												? maps.get(key).get(key + "_lokasi")
												: ""));
										a.addEventListener("onClick", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Clients.evalJavaScript(
														"popupCenter({url: '" + ((A) arg0.getTarget()).getLabel()
																+ "', title: 'Data', w: 1200, h: 600});");
											}
										});
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:977");
									}
								}

								maps.clear();
								maps = null;
							}
						}

						if (liburRutin != null && liburRutin.getLibur()) {
							row.setStyle("border:0px;background: #d5f5dd;");
						}

						if (liburNasional != null) {
							row.setStyle("border:0px;background: pink;");
						}

						row.setParent(rows);

						RevisiHelper
								.createNewRevisi(StatuskehadiranKaryawanHarian.class, statuskehadiranKaryawanHarian,
										Common.dateFormat4.get().format(calendar.getTime())
												+ (liburNasional == null ? "" : " (" + liburNasional.toString() + ")"))
								.setParent(row);

						Vbox vboxMasukKeluar = new Vbox();
						vboxMasukKeluar.setWidth("100%");

						if (statuskehadiranKaryawanHarian.getMasukjamState() != null
								&& statuskehadiranKaryawanHarian.getPulangJamState() != null
								&& statuskehadiranKaryawanHarian.getPulangJamState()
										.before(statuskehadiranKaryawanHarian.getMasukjamState())) {
							new MyLabelAgakKecil("Pulang:"
									+ Common.timeFormat.get().format(statuskehadiranKaryawanHarian.getPulangJamState()))
									.setParent(vboxMasukKeluar);
							new MyLabelAgakKecil("Masuk:"
									+ Common.timeFormat.get().format(statuskehadiranKaryawanHarian.getMasukjamState()))
									.setParent(vboxMasukKeluar);
						} else {

							if (statuskehadiranKaryawanHarian.getTidakAdaKedatangan()) {
								new Label().setParent(vboxMasukKeluar);
							}

							else if (statuskehadiranKaryawanHarian.getMasukjamState() != null) {
								new MyLabelAgakKecil("Masuk:"
										+ Common.timeFormat.get().format(statuskehadiranKaryawanHarian.getMasukjamState()))
										.setParent(vboxMasukKeluar);
							} else {
								Date m = statuskehadiranKaryawanHarian.getMasukjam();

								new MyLabelAgakKecil("Masuk:" + (m == null ? "" : Common.timeFormat.get().format(m)))
										.setParent(vboxMasukKeluar);
							}

							if (statuskehadiranKaryawanHarian.getTidakAdaKepulangan()) {
								new Label().setParent(vboxMasukKeluar);
							}

							else if (statuskehadiranKaryawanHarian.getPulangJamState() != null) {
								new MyLabelAgakKecil("Pulang:"
										+ Common.timeFormat.get().format(statuskehadiranKaryawanHarian.getPulangJamState()))
										.setParent(vboxMasukKeluar);
							} else {
								Date m = statuskehadiranKaryawanHarian.getPulangJam();

								// FALLBACK JAM PULANG AKTUAL:
								// getPulangJam() dapat mengembalikan null MESKIPUN pegawai SUDAH melakukan
								// scan pulang (mis. QR-CODE PULANG / absensi online). Penyebabnya adalah
								// aturan durasi kerja minimal shift (waktuBekerjaMinimal) di dalam
								// StatuskehadiranKaryawanHarian.getPulangJam(): bila selisih masuk->pulang
								// lebih pendek dari batas minimal shift, jam pulang di-null-kan untuk
								// keperluan perhitungan penggajian. Akibatnya kolom "Pulang:" tampak
								// KOSONG padahal data scan-nya ada. Agar operator tetap dapat MELIHAT jam
								// kepulangan yang sebenarnya (tanpa mengubah logika penggajian sama sekali),
								// kita ambil langsung jam scan pulang terakhir dari riwayat absensi online
								// (sejarah) lalu dari keterangan fingerprint sebagai cadangan. Kedua sumber
								// ini adalah data mentah scan yang TIDAK dikenai aturan durasi minimal.
								// FAIL-SAFE AUTO-ISI JAM PULANG (bila loop batch di atas terlewat pada baris ini):
								// jika jam pulang kosong TAPI memenuhi syarat "Waktu minimal bekerja (jam) <
								// (history_pulang - history_masuk)", isi & SIMPAN jam pulang di sini juga. Session
								// dibuka HANYA bila memang memenuhi syarat (hemat resource).
								if (m == null
										&& statuskehadiranKaryawanHarian.hitungPulangOtomatisDariSejarah() != null) {
									Session sesiPulangBaris = null;
									try {
										sesiPulangBaris = HibernateUtil.openSession();
										m = statuskehadiranKaryawanHarian.autoUpdatePulangDariSejarah(sesiPulangBaris);
									} catch (Exception exPulang) {
										exPulang.printStackTrace(); ais.common.ErrorAuditUtil.record(exPulang, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1066");
									} finally {
										if (sesiPulangBaris != null) {
											try {
												if (sesiPulangBaris.isOpen()) {
													sesiPulangBaris.clear();
												}
											} catch (Exception exPulang) { ais.common.ErrorAuditUtil.record(exPulang, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1073");
											}
											try {
												sesiPulangBaris.disconnect();
											} catch (Exception exPulang) { ais.common.ErrorAuditUtil.record(exPulang, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1077");
											}
											try {
												if (sesiPulangBaris.isOpen()) {
													sesiPulangBaris.close();
												}
											} catch (Exception exPulang) { ais.common.ErrorAuditUtil.record(exPulang, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1083");
											}
										}
									}
								}

								// Tetap tampilkan jam scan asli (read-only) walau belum memenuhi syarat auto-isi.
								if (m == null) {
									m = statuskehadiranKaryawanHarian.sampaiOtomatisUlangAbsenDariSejarah();
								}
								if (m == null) {
									m = statuskehadiranKaryawanHarian.sampaiOtomatisUlangAbsenDariKeterangan();
								}

								new MyLabelAgakKecil("Pulang:" + (m == null ? "" : Common.timeFormat.get().format(m)))
										.setParent(vboxMasukKeluar);

							}
						}

						final MyLabelAgakKecil jumlahJamMasuk = new MyLabelAgakKecil(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahJamMasuk()) + " ("
										+ Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.getWaktuJamMasuk())
										+ ")");

						final MyLabelAgakKecil jumlahCepatKeluar = new MyLabelAgakKecil(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar()) + " ("
										+ Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
										+ ")");

						final MyLabelAgakKecil jumlahTerlambat = new MyLabelAgakKecil(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahTerlambat()) + " ("
										+ Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.getWaktuTerlambat())
										+ ")");

						final MyLabelAgakKecil jumlahLemburMasuk = new MyLabelAgakKecil(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk()) + " ("
										+ Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
										+ ")");

						final MyLabelAgakKecil infoShift = new MyLabelAgakKecil(
								statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
										: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai().toString());

						if (edit && statuskehadiranKaryawanHarian.getCutiDanIzin() == null) {

							new Label(statuskehadiranKaryawanHarian.getStatusabsensi() == null ? ""
									: statuskehadiranKaryawanHarian.getStatusabsensi().getNama()).setParent(row);

							vboxMasukKeluar.setParent(row);

							jumlahJamMasuk.setParent(row);

							final MyCheckboxConfig checkDetailJenisShiftPegawaiManual = new MyCheckboxConfig("Manual");
							checkDetailJenisShiftPegawaiManual.setChecked(
									statuskehadiranKaryawanHarian.getDetailJenisShiftPegawaiManual() != null);
							final Combobox detailJenisShiftPegawaiManual = new Combobox();

							Vbox vboxInfoShif = new Vbox();
							vboxInfoShif.setWidth("100%");
							vboxInfoShif.setParent(row);

							infoShift.setParent(vboxInfoShif);
							if (statuskehadiranKaryawanHarian.getDikunci() == null)
								checkDetailJenisShiftPegawaiManual.setParent(vboxInfoShif);
							if (statuskehadiranKaryawanHarian.getDikunci() == null)
								detailJenisShiftPegawaiManual.setParent(vboxInfoShif);

							detailJenisShiftPegawaiManual.setWidth("90%");
							EventListener eventListenerDetailJenisShiftPegawaiManual = new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									detailJenisShiftPegawaiManual
											.setVisible(checkDetailJenisShiftPegawaiManual.isChecked());

									if (checkDetailJenisShiftPegawaiManual.isChecked()) {
										statuskehadiranKaryawanHarian.setDetailJenisShiftPegawaiManual(
												(DetailJenisShiftPegawai) (detailJenisShiftPegawaiManual
														.getSelectedItem() == null ? null
																: detailJenisShiftPegawaiManual.getSelectedItem()
																		.getValue()));
										Session session = HibernateUtil.currentSession();
										List<Long> ids = session.createCriteria(JenisShiftPunyaPegawai.class)

												.add(Restrictions.eq("pegawai", pegawai))
												.createAlias("jenisShiftPegawai", "jenisShiftPegawai")

												.add(Restrictions.or(Restrictions.isNull("jenisShiftPegawai.aktif"),
														Restrictions.eq("jenisShiftPegawai.aktif", true)))

												.add(Restrictions.le("jenisShiftPegawai.berlakuMulai", tanggal))
												.addOrder(Order.desc("jenisShiftPegawai.berlakuMulai"))
												.add(Restrictions.or(
														Restrictions.isNull("jenisShiftPegawai.berlakuSampai"),
														Restrictions.ge("jenisShiftPegawai.berlakuSampai", tanggal)))
												.setProjection(Projections.groupProperty("jenisShiftPegawai.id"))
												.setMaxResults(1).list();
										Criterion criterions = ids.isEmpty() ? Restrictions.sqlRestriction("false")
												: Restrictions.in("jenisShiftPegawai.id", ids);
										Common.insertComboDanSemua(detailJenisShiftPegawaiManual,
												new String[] { "nama", "jenisShiftPegawai" }, "keterangan",
												DetailJenisShiftPegawai.class, "Shift dibuat otomatis", criterions);

										Common.selectComboItem(true, detailJenisShiftPegawaiManual,
												statuskehadiranKaryawanHarian.getDetailJenisShiftPegawaiManual());
										detailJenisShiftPegawaiManual.setReadonly(true);
									} else {
										statuskehadiranKaryawanHarian.setDetailJenisShiftPegawaiManual(null);
									}

									statuskehadiranKaryawanHarian.setBulan(bln);
									statuskehadiranKaryawanHarian.setTahun(thn);
									statuskehadiranKaryawanHarian.setTgl(tgl);
									statuskehadiranKaryawanHarian.setMinggu(hari);

									if (arg0 != null) {
										Common.refreshSaveOrUpdate(statuskehadiranKaryawanHarian);
									}

									jumlahJamMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahJamMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuJamMasuk())
											+ ")");

									jumlahLemburMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
											+ ")");

									jumlahCepatKeluar.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
											+ ")");

									jumlahTerlambat.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahTerlambat()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuTerlambat())
											+ ")");

									infoShift.setValue(
											statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
													: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai()
															.toString());
								}
							};

							checkDetailJenisShiftPegawaiManual.addEventListener("onClick",
									eventListenerDetailJenisShiftPegawaiManual);

							try {
								eventListenerDetailJenisShiftPegawaiManual.onEvent(null);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1239");
								// TODO: handle exception
							}

							detailJenisShiftPegawaiManual.addEventListener("onChange", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Session session = HibernateUtil.currentSession();
									statuskehadiranKaryawanHarian.setDetailJenisShiftPegawaiManual(
											(DetailJenisShiftPegawai) (detailJenisShiftPegawaiManual
													.getSelectedItem() == null ? null
															: detailJenisShiftPegawaiManual.getSelectedItem()
																	.getValue()));
									statuskehadiranKaryawanHarian.setBulan(bln);
									statuskehadiranKaryawanHarian.setTahun(thn);
									statuskehadiranKaryawanHarian.setTgl(tgl);
									statuskehadiranKaryawanHarian.setMinggu(hari);
									Common.refreshSaveOrUpdate(session, statuskehadiranKaryawanHarian);

									jumlahJamMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahJamMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuJamMasuk())
											+ ")");

									jumlahLemburMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
											+ ")");

									jumlahCepatKeluar.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
											+ ")");

									jumlahTerlambat.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahTerlambat()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuTerlambat())
											+ ")");

									infoShift.setValue(
											statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
													: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai()
															.toString());
								}
							});

							final MyCheckboxConfig checkDetailJenisShiftPegawaiLembur = new MyCheckboxConfig("Manual");
							checkDetailJenisShiftPegawaiLembur.setChecked(
									statuskehadiranKaryawanHarian.getDetailJenisShiftPegawaiLembur() != null);
							final Combobox detailJenisShiftPegawaiLembur = new Combobox();
							vboxInfoShif = new Vbox();
							vboxInfoShif.setWidth("100%");
							vboxInfoShif.setParent(row);
							jumlahLemburMasuk.setParent(vboxInfoShif);

							if (statuskehadiranKaryawanHarian.getDikunci() == null)
								detailJenisShiftPegawaiLembur.setParent(vboxInfoShif);
							if (statuskehadiranKaryawanHarian.getDikunci() == null)
								checkDetailJenisShiftPegawaiLembur.setParent(vboxInfoShif);
							detailJenisShiftPegawaiLembur.setWidth("80%");

							final Timebox lamburMulai = new ais.ui.util.MyTimebox(
									statuskehadiranKaryawanHarian.getLamburMulai());

							if (statuskehadiranKaryawanHarian.getDikunci() == null)
								vboxInfoShif.appendChild(lamburMulai);

							final Timebox lamburSampai = new ais.ui.util.MyTimebox(
									statuskehadiranKaryawanHarian.getLamburSampai());

							if (statuskehadiranKaryawanHarian.getDikunci() == null)
								vboxInfoShif.appendChild(lamburSampai);

							lamburMulai.setCols(3);
							lamburSampai.setCols(3);

							EventListener eventListenerDetailJenisShiftPegawaiLembur = new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									detailJenisShiftPegawaiLembur
											.setVisible(checkDetailJenisShiftPegawaiLembur.isChecked());

									lamburMulai.setVisible(checkDetailJenisShiftPegawaiLembur.isChecked());
									lamburSampai.setVisible(checkDetailJenisShiftPegawaiLembur.isChecked());

									if (checkDetailJenisShiftPegawaiLembur.isChecked()) {

										statuskehadiranKaryawanHarian.setDetailJenisShiftPegawaiLembur(
												(DetailJenisShiftPegawai) (detailJenisShiftPegawaiLembur
														.getSelectedItem() == null ? null
																: detailJenisShiftPegawaiLembur.getSelectedItem()
																		.getValue()));

										Session session = HibernateUtil.currentSession();
										List<Long> ids = session.createCriteria(JenisShiftPunyaPegawai.class)

												.add(Restrictions.eq("pegawai", pegawai))
												.createAlias("jenisShiftPegawai", "jenisShiftPegawai")

												.add(Restrictions.or(Restrictions.isNull("jenisShiftPegawai.aktif"),
														Restrictions.eq("jenisShiftPegawai.aktif", true)))

												.add(Restrictions.le("jenisShiftPegawai.berlakuMulai", tanggal))
												.addOrder(Order.desc("jenisShiftPegawai.berlakuMulai"))
												.add(Restrictions.or(
														Restrictions.isNull("jenisShiftPegawai.berlakuSampai"),
														Restrictions.ge("jenisShiftPegawai.berlakuSampai", tanggal)))
												.setProjection(Projections.groupProperty("jenisShiftPegawai.id"))
												.setMaxResults(1).list();
										Criterion criterions = ids.isEmpty() ? Restrictions.sqlRestriction("false")
												: Restrictions.in("jenisShiftPegawai.id", ids);
										Common.insertComboDanSemua(detailJenisShiftPegawaiLembur,
												new String[] { "nama", "jenisShiftPegawai" }, "keterangan",
												DetailJenisShiftPegawai.class, "Samakan dengan shift utama",
												criterions);

										Common.selectComboItem(true, detailJenisShiftPegawaiLembur,
												statuskehadiranKaryawanHarian.getDetailJenisShiftPegawaiLembur());
										detailJenisShiftPegawaiLembur.setReadonly(true);
									} else {
										statuskehadiranKaryawanHarian.setDetailJenisShiftPegawaiLembur(null);
									}

									statuskehadiranKaryawanHarian.setBulan(bln);
									statuskehadiranKaryawanHarian.setTahun(thn);
									statuskehadiranKaryawanHarian.setTgl(tgl);
									statuskehadiranKaryawanHarian.setMinggu(hari);

									if (arg0 != null) {
										Common.refreshSaveOrUpdate(statuskehadiranKaryawanHarian);
									}

									jumlahJamMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahJamMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuJamMasuk())
											+ ")");

									jumlahLemburMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
											+ ")");

									jumlahCepatKeluar.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
											+ ")");

									jumlahTerlambat.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahTerlambat()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuTerlambat())
											+ ")");

									infoShift.setValue(
											statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
													: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai()
															.toString());
								}
							};

							checkDetailJenisShiftPegawaiLembur.addEventListener("onClick",
									eventListenerDetailJenisShiftPegawaiLembur);

							try {
								eventListenerDetailJenisShiftPegawaiLembur.onEvent(null);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1413");
								// TODO: handle exception
							}

							EventListener eventListenerData = new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									statuskehadiranKaryawanHarian.setDetailJenisShiftPegawaiLembur(
											(DetailJenisShiftPegawai) (detailJenisShiftPegawaiLembur
													.getSelectedItem() == null ? null
															: detailJenisShiftPegawaiLembur.getSelectedItem()
																	.getValue()));

									statuskehadiranKaryawanHarian.setLamburMulai(lamburMulai.getValue());
									statuskehadiranKaryawanHarian.setLamburSampai(lamburSampai.getValue());
									statuskehadiranKaryawanHarian.setBulan(bln);
									statuskehadiranKaryawanHarian.setTahun(thn);
									statuskehadiranKaryawanHarian.setTgl(tgl);
									statuskehadiranKaryawanHarian.setMinggu(hari);

									if (arg0 != null) {
										Session session = HibernateUtil.currentSession();
										Common.refreshSaveOrUpdate(session, statuskehadiranKaryawanHarian);
									}

									jumlahJamMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahJamMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuJamMasuk())
											+ ")");

									jumlahLemburMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
											+ ")");

									jumlahCepatKeluar.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
											+ ")");

									jumlahTerlambat.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahTerlambat()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuTerlambat())
											+ ")");

									infoShift.setValue(
											statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
													: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai()
															.toString());
								}
							};

							detailJenisShiftPegawaiLembur.addEventListener("onChange", eventListenerData);
							lamburMulai.addEventListener("onChange", eventListenerData);
							lamburSampai.addEventListener("onChange", eventListenerData);

							jumlahCepatKeluar.setParent(row);
							jumlahTerlambat.setParent(row);

							if (statuskehadiranKaryawanHarian.getId() != null) {
								final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Abaikan Jarak");
								checkboxConfig.setParent(row);
								checkboxConfig.setChecked(statuskehadiranKaryawanHarian.getAbaikanJarak());
								checkboxConfig.addEventListener(Events.ON_CHECK, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										Session session = HibernateUtil.currentSession();
										session.refresh(statuskehadiranKaryawanHarian);
										statuskehadiranKaryawanHarian.setAbaikanJarak(checkboxConfig.isChecked());
										session.update(statuskehadiranKaryawanHarian);
										session.flush();
									}
								});
							} else if (statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai() != null) {
								new Label(statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai().getAbaikanJarak()
										? "Ya"
										: "Tidak").setParent(row);
							} else {
								new Label().setParent(row);
							}


							statuskehadiranKaryawanHarian.renderKeteranganLink(row);
							
							

							// Tombol aksi (Ubah + Kunci) dirapikan menjadi satu button group yang ringkas
							// & sejajar di tengah kolom, bukan ikon lepas yang berjauhan.
							Hbox toolbar = new Hbox();
							toolbar.setSpacing("2px");
							toolbar.setAlign("center");
							toolbar.setStyle("display:inline-table;width:auto;background:#f8fafc;"
									+ "border:1px solid #e2e8f0;border-radius:10px;padding:2px 5px;");
							toolbar.setParent(row);
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
							button.setTooltiptext("Ubah Data");
							button.setVisible(edit && statuskehadiranKaryawanHarian.getDikunci() == null);

							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									editJam(statuskehadiranKaryawanHarian);
								}

							});
							button.setParent(toolbar);

							GeneralValueObject.tampilKunci(toolbar, statuskehadiranKaryawanHarian, tbmuser,
									new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											loadData(null);
										}

									}, false);

						} else {

							row.appendChild(
									new MyLabelAgakKecil(statuskehadiranKaryawanHarian.getStatusabsensi().getNama()));

							row.appendChild(new MyLabelAgakKecil(statuskehadiranKaryawanHarian.ambilMasukjam() == null
									? ""
									: Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.ambilMasukjam())));

							row.appendChild(new MyLabelAgakKecil(statuskehadiranKaryawanHarian.ambilPulangjam() == null
									? ""
									: Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.ambilPulangjam())));

							jumlahJamMasuk.setParent(row);
							infoShift.setParent(row);

							jumlahLemburMasuk.setParent(row);
							jumlahCepatKeluar.setParent(row);
							jumlahTerlambat.setParent(row);

							MyLabelAgakKecil l;
							(l = new MyLabelAgakKecil(statuskehadiranKaryawanHarian.getKeterangan())).setParent(row);
							l.setMultiline(true);
							row.appendChild(new MyLabelAgakKecil());
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1562");
					}
				}

			}
		});

	}
}
