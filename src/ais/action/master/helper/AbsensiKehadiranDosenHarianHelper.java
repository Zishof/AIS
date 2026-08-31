package ais.action.master.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper terfokus untuk absensi kehadiran dosen harian. Tipe ini membungkus satu variasi kecil
 * dari alur yang lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan
 * implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox bulan}, {@code Combobox
 * tahun}, {@code MyGrid grid}, {@code Dosen dosen}, {@code boolean edit}; pembacaan/pencarian ({@code
 * loadData()}); operasi domain lain ({@code display()}); konfigurasi constructor: {@code edit}. Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyDetail
 */
public class AbsensiKehadiranDosenHarianHelper extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8823784546257272901L;
	private Combobox bulan;
	private Combobox tahun;
	private MyGrid grid;
	private Dosen dosen;

	private boolean edit = false;

	public AbsensiKehadiranDosenHarianHelper(Dosen dosen) {
		this.dosen = dosen;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(AbsensiKehadiranDosenHarianHelper.this);
				if (isOpen())
					display();
			}
		});
	}

	public void display() {
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar absensi dosen"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Bulan : ")));
		toolbar.appendChild(bulan = new Combobox());
		for (int i = 0; i < 12; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(Common.BULAN[i]);
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

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun : ")));
		toolbar.appendChild(tahun = new Combobox());

		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
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
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(100);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("45%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Masuk");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pulang");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		try {
			loadData(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object object) throws Exception {
		Integer bulan = (Integer) (this.bulan.getSelectedItem() == null ? null
				: this.bulan.getSelectedItem().getValue());

		Integer tahun = (Integer) (this.tahun.getSelectedItem() == null ? null
				: this.tahun.getSelectedItem().getValue());

		if (bulan == null) {
			MyMessageboxConfig.show("Mohon maaf, bulan belum dipilih. Langkah yang dapat dilakukan: (1) pilih bulan dari daftar yang tersedia; (2) pastikan periode bulan sudah dipilih dengan benar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (tahun == null) {
			MyMessageboxConfig.show("Mohon maaf, tahun belum dipilih. Langkah yang dapat dilakukan: (1) pilih tahun dari daftar yang tersedia; (2) pastikan periode tahun sudah dipilih dengan benar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, bulan - 1);
		calendar.set(Calendar.YEAR, tahun);
		calendar.set(Calendar.DATE, 1);

		int jumlahHari = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

		Session session = HibernateUtil.currentSession();
		List<Statusabsensi> statusabsensis = session.createCriteria(Statusabsensi.class).addOrder(Order.asc("nama"))
				.list();

		Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
		grid.appendChild(rows);
		rows.setParent(grid);
		Common.clear(rows);

		// Calendar pagi = ais.ui.util.WaktuUtil.getCalendar();
		// pagi.set(Calendar.SECOND, 0);
		// pagi.set(Calendar.MINUTE, 30);
		// pagi.set(Calendar.HOUR_OF_DAY, 7);
		//
		// Calendar sore = ais.ui.util.WaktuUtil.getCalendar();
		// sore.set(Calendar.SECOND, 0);
		// sore.set(Calendar.MINUTE, 0);
		// sore.set(Calendar.HOUR_OF_DAY, 17);

		for (int i = 1; i <= jumlahHari; i++) {

			calendar.set(Calendar.DATE, i);

			Date tanggal = calendar.getTime();

			final Integer bln = calendar.get(Calendar.MONTH) + 1;
			final Integer thn = calendar.get(Calendar.YEAR);
			final Integer tgl = calendar.get(Calendar.DATE);
			final Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

			StatuskehadiranKaryawanHarian karyawanHarian = (StatuskehadiranKaryawanHarian) session
					.createCriteria(StatuskehadiranKaryawanHarian.class).add(Restrictions.eq("tanggal", tanggal))
					.add(Restrictions.eq("dosen", dosen)).setMaxResults(1).uniqueResult();

			final StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian;
			if (karyawanHarian == null) {
				statuskehadiranKaryawanHarian = new StatuskehadiranKaryawanHarian();
				statuskehadiranKaryawanHarian.setBulan(bln);
				statuskehadiranKaryawanHarian.setTahun(thn);
				statuskehadiranKaryawanHarian.setTgl(tgl);
				statuskehadiranKaryawanHarian.setDosen(dosen);
				statuskehadiranKaryawanHarian.setKeterangan("");
				statuskehadiranKaryawanHarian.setMasukjam(null);
				statuskehadiranKaryawanHarian.setPulangJam(null);
				statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.BELUM_ABSEN);
				statuskehadiranKaryawanHarian.setTanggal(tanggal);
				statuskehadiranKaryawanHarian.setMinggu(hari);
				session.save(statuskehadiranKaryawanHarian);
			} else {
				statuskehadiranKaryawanHarian = karyawanHarian;
			}

			Row row = new Row();row.setValign("top");
			if (hari == 1 || hari == 7) {
				row.setStyle("border:0px;background: red;");
			}
			row.setParent(rows);

			AbsensiKehadiranDosenHarianDetailHelper absensiKehadiranDosenHarianDetailHelper = new AbsensiKehadiranDosenHarianDetailHelper(
					statuskehadiranKaryawanHarian);
			absensiKehadiranDosenHarianDetailHelper.setParent(row);

			row.appendChild(new ais.ui.util.MyLabelConfig(Common.dateFormat4.get().format(calendar.getTime())));
			Tbmuser tbmuser = Common.getCurrentUser();
			if (edit && tbmuser.ambilDosen() == null) {
				Radiogroup radiogroup = new Radiogroup();
				radiogroup.setParent(row);
				for (final Statusabsensi statusabsensi : statusabsensis) {
					final MyRadioConfig a = new MyRadioConfig(statusabsensi.getNama());
					a.setChecked(
							statuskehadiranKaryawanHarian.getStatusabsensi().getId().equals(statusabsensi.getId()));
					radiogroup.appendChild(a);
					a.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = HibernateUtil.currentSession();
							statuskehadiranKaryawanHarian.setStatusabsensi(statusabsensi);
							statuskehadiranKaryawanHarian.setBulan(bln);
							statuskehadiranKaryawanHarian.setTahun(thn);
							statuskehadiranKaryawanHarian.setTgl(tgl);
							statuskehadiranKaryawanHarian.setMinggu(hari);
							session.update((statuskehadiranKaryawanHarian));
						}
					});
				}
			} else {
				row.appendChild(
						new ais.ui.util.MyLabelConfig(statuskehadiranKaryawanHarian.getStatusabsensi().getNama()));
			}

			if (edit && tbmuser.ambilDosen() == null) {

				final Timebox masuk = new ais.ui.util.MyTimebox(statuskehadiranKaryawanHarian.getMasukjam());
				masuk.setParent(row);
				masuk.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						statuskehadiranKaryawanHarian.setMasukjam(masuk.getValue());
						statuskehadiranKaryawanHarian.setBulan(bln);
						statuskehadiranKaryawanHarian.setTahun(thn);
						statuskehadiranKaryawanHarian.setTgl(tgl);
						statuskehadiranKaryawanHarian.setMinggu(hari);
						session.update((statuskehadiranKaryawanHarian));
					}
				});

				final Timebox keluar = new ais.ui.util.MyTimebox(statuskehadiranKaryawanHarian.getPulangJam());
				keluar.setParent(row);
				keluar.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						statuskehadiranKaryawanHarian.setPulangJam(keluar.getValue());
						statuskehadiranKaryawanHarian.setBulan(bln);
						statuskehadiranKaryawanHarian.setTahun(thn);
						statuskehadiranKaryawanHarian.setTgl(tgl);
						statuskehadiranKaryawanHarian.setMinggu(hari);
						session.update((statuskehadiranKaryawanHarian));
					}
				});

				masuk.setCols(3);
				keluar.setCols(3);

				final Textbox keterangan = new Textbox(statuskehadiranKaryawanHarian.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setRows(3);
				keterangan.setParent(row);
				keterangan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						statuskehadiranKaryawanHarian.setKeterangan(keterangan.getValue());
						statuskehadiranKaryawanHarian.setBulan(bln);
						statuskehadiranKaryawanHarian.setTahun(thn);
						statuskehadiranKaryawanHarian.setTgl(tgl);
						statuskehadiranKaryawanHarian.setMinggu(hari);
						session.update((statuskehadiranKaryawanHarian));
						if (ConstantValues.aktifkanFingerPrintOtomatisDariKeterangan) {
							Date m = statuskehadiranKaryawanHarian.mulaiOtomatisUlangAbsenDariKeterangan();
							if (m != null) {
								masuk.setValue(m);
								masuk.setDisabled(true);
							} else {
								masuk.setDisabled(false);
							}

							m = statuskehadiranKaryawanHarian.sampaiOtomatisUlangAbsenDariKeterangan();
							if (m != null) {
								keluar.setValue(m);
								keluar.setDisabled(true);
							} else {
								keluar.setDisabled(false);
							}
						}
					}
				});

				if (ConstantValues.aktifkanFingerPrintOtomatisDariKeterangan) {
					Date m = statuskehadiranKaryawanHarian.mulaiOtomatisUlangAbsenDariKeterangan();
					if (m != null) {
						masuk.setValue(m);
						masuk.setDisabled(true);
					}

					m = statuskehadiranKaryawanHarian.sampaiOtomatisUlangAbsenDariKeterangan();
					if (m != null) {
						keluar.setValue(m);
						keluar.setDisabled(true);
					}
				}

			} else {
				row.appendChild(new ais.ui.util.MyLabelConfig(statuskehadiranKaryawanHarian.ambilMasukjam() == null ? ""
						: Common.timeFormat.get().format(statuskehadiranKaryawanHarian.ambilMasukjam())));

				row.appendChild(new ais.ui.util.MyLabelConfig(statuskehadiranKaryawanHarian.ambilPulangjam() == null ? ""
						: Common.timeFormat.get().format(statuskehadiranKaryawanHarian.ambilPulangjam())));

				statuskehadiranKaryawanHarian.renderKeteranganLink(row);
			}
		}

	}
}
