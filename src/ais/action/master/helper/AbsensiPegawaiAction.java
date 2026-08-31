package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Box;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.report.format1.akademik.LaporanAbsensiPegawaiPerHari;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPayroll;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.payroll.ItemGaji;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyHtml;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk absensi pegawai. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Date date}, {@code Paging paging},
 * {@code MyGrid grid}, {@code boolean edit}, {@code List statusabsensis}, {@code Calendar calendar}, {@code
 * MyTextbox kode}, {@code MyTextbox nama}; inisialisasi/lifecycle ({@code initCriteria()}); pembacaan/pencarian
 * ({@code loadData()}); operasi domain lain ({@code display()}); konfigurasi constructor: {@code calendar},
 * {@code edit}, {@code statusabsensis}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class AbsensiPegawaiAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Date date;
	private Paging paging;
	private MyGrid grid;
	private boolean edit = false;
	private List<Statusabsensi> statusabsensis;

	private Calendar calendar;

	@SuppressWarnings("unchecked")
	public AbsensiPegawaiAction(Date date) {
		super();
		this.date = date;
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(date);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		Session session = HibernateUtil.currentSession();
		statusabsensis = session.createCriteria(Statusabsensi.class).addOrder(Order.asc("nama")).list();
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(AbsensiPegawaiAction.this);
				if (isOpen()) {
					display();
				}
			}
		});

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AbsensiPegawaiAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AbsensiPegawaiAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AbsensiPegawaiAction
	 */
	class PegawaiRenderer extends ais.ui.util.MyRowRenderer {

		public PegawaiRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final Pegawai pegawai = (Pegawai) data;
			CommonMedia.tampilkanGambarKecil(pegawai).setParent(row);
			new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(row);
			RevisiHelper.createNewRevisi(Pegawai.class, pegawai, pegawai.getNama()).setParent(row);

			final StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = CommonPayroll
					.getDefaultStatuskehadiranKaryawanHarian(date, pegawai, null, null, null, null);

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

			if (urls.isEmpty() && urlsPulang.isEmpty()) {
				new Label().setParent(row);
			} else {

				MyDetail detail = new MyDetail();
				detail.setParent(row);
				detail.setOpen(true);

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
								+ "', title: 'Foto', w: 1200, h: 600});\" ><image style=\"height:200px;\" src=\"" + u
								+ "\"></image></a>");
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
								+ "', title: 'Foto', w: 1200, h: 600});\" ><image style=\"height:200px;\" src=\"" + u
								+ "\"></image></a>");
						box.appendChild(myHtml);
					}
				}
			}

			final Integer bln = calendar.get(Calendar.MONTH) + 1;
			final Integer thn = calendar.get(Calendar.YEAR);
			final Integer tgl = calendar.get(Calendar.DATE);
			final Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

			final Timebox masuk = new ais.ui.util.MyTimebox(statuskehadiranKaryawanHarian.getMasukjam());
			final Timebox keluar = new ais.ui.util.MyTimebox(statuskehadiranKaryawanHarian.getPulangJam());

			masuk.setCols(3);
			keluar.setCols(3);

			final Label jumlahJamMasuk = new Label(
					Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahJamMasuk()) + " ("
							+ Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.getWaktuJamMasuk()) + ")");

			final Label jumlahCepatKeluar = new Label(
					Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar()) + " ("
							+ Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
							+ ")");

			final Label jumlahTerlambat = new Label(
					Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahTerlambat()) + " ("
							+ Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.getWaktuTerlambat()) + ")");

			final Label jumlahLemburMasuk = new Label(
					Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk()) + " ("
							+ Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
							+ ")");

			final Label infoShift = new Label(statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
					: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai().toString());

			if (edit && statuskehadiranKaryawanHarian.getCutiDanIzin() == null) {
				CutiDanIzin cutiDanIzin = statuskehadiranKaryawanHarian.getCutiDanIzin();
				masuk.setDisabled(statuskehadiranKaryawanHarian.ambilMasukjam() == null
						&& (cutiDanIzin == null || !cutiDanIzin.getSetujui()));
				keluar.setDisabled(statuskehadiranKaryawanHarian.ambilPulangjam() == null);

				final Combobox radiogroup = new Combobox();
				radiogroup.setReadonly(true);
				radiogroup.setParent(row);
				for (final Statusabsensi statusabsensi : statusabsensis) {
					final Comboitem a = new Comboitem(statusabsensi.getNama());
					a.setValue(statusabsensi);
					radiogroup.appendChild(a);

				}
				Common.selectComboItem(radiogroup, statuskehadiranKaryawanHarian.getStatusabsensi());
				radiogroup.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Statusabsensi statusabsensi = (Statusabsensi) (radiogroup.getSelectedItem() == null ? null
								: radiogroup.getSelectedItem().getValue());
						CutiDanIzin cutiDanIzin = statuskehadiranKaryawanHarian.getCutiDanIzin();
						if (statusabsensi != null && statusabsensi.getNama().equalsIgnoreCase("Masuk")
								|| statusabsensi.getNama().equalsIgnoreCase("Hadir")) {
							if (statuskehadiranKaryawanHarian.getMasukjam() == null
									&& (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {
								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.set(Calendar.HOUR_OF_DAY, 8);
								calendar.set(Calendar.MINUTE, 30);
								calendar.set(Calendar.SECOND, 0);
								statuskehadiranKaryawanHarian.setMasukjam(calendar.getTime());
							}

							if (statuskehadiranKaryawanHarian.getPulangJam() == null) {
								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.set(Calendar.HOUR_OF_DAY, 16);
								calendar.set(Calendar.MINUTE, 30);
								calendar.set(Calendar.SECOND, 0);
								statuskehadiranKaryawanHarian.setPulangJam(calendar.getTime());
							}
						} else {
							statuskehadiranKaryawanHarian.setMasukjam(null);
							statuskehadiranKaryawanHarian.setPulangJam(null);
							statuskehadiranKaryawanHarian.setLamburMulai(null);
							statuskehadiranKaryawanHarian.setLamburSampai(null);
						}

						Session session = HibernateUtil.currentSession();
						statuskehadiranKaryawanHarian.setStatusabsensi(statusabsensi);

						masuk.setValue(statuskehadiranKaryawanHarian.ambilMasukjam());
						keluar.setValue(statuskehadiranKaryawanHarian.ambilPulangjam());

						masuk.setDisabled(statuskehadiranKaryawanHarian.ambilMasukjam() == null
								&& (cutiDanIzin == null || !cutiDanIzin.getSetujui()));
						keluar.setDisabled(statuskehadiranKaryawanHarian.ambilPulangjam() == null);

						statuskehadiranKaryawanHarian.setBulan(bln);
						statuskehadiranKaryawanHarian.setTahun(thn);
						statuskehadiranKaryawanHarian.setTgl(tgl);
						statuskehadiranKaryawanHarian.setMinggu(hari);

						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.setTime(date);
						String haris = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];

						statuskehadiranKaryawanHarian
								.setDetailJenisShiftPegawai(CommonPayroll.getDetailJenisShiftPegawai(pegawai, null,
										null, statuskehadiranKaryawanHarian.ambilMasukjam(),
										statuskehadiranKaryawanHarian.getTanggal(), haris,
										statuskehadiranKaryawanHarian.getLiburNasional() != null));

						Common.refreshSaveOrUpdate(session, statuskehadiranKaryawanHarian);

						jumlahJamMasuk.setValue(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahJamMasuk())
										+ " (" + Common.dateFormat1.get()
												.format(statuskehadiranKaryawanHarian.getWaktuJamMasuk())
										+ ")");

						jumlahLemburMasuk.setValue(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk())
										+ " (" + Common.dateFormat1.get()
												.format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
										+ ")");

						jumlahCepatKeluar.setValue(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar())
										+ " (" + Common.dateFormat1.get()
												.format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
										+ ")");

						jumlahTerlambat.setValue(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahTerlambat())
										+ " (" + Common.dateFormat1.get()
												.format(statuskehadiranKaryawanHarian.getWaktuTerlambat())
										+ ")");

						infoShift.setValue(statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
								: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai().toString());
					}
				});

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

						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.setTime(date);
						String haris = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];

						statuskehadiranKaryawanHarian
								.setDetailJenisShiftPegawai(CommonPayroll.getDetailJenisShiftPegawai(pegawai, null,
										null, statuskehadiranKaryawanHarian.ambilMasukjam(),
										statuskehadiranKaryawanHarian.getTanggal(), haris,
										statuskehadiranKaryawanHarian.getLiburNasional() != null));

						Common.refreshSaveOrUpdate(session, statuskehadiranKaryawanHarian);

						jumlahJamMasuk.setValue(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahJamMasuk())
										+ " (" + Common.dateFormat1.get()
												.format(statuskehadiranKaryawanHarian.getWaktuJamMasuk())
										+ ")");

						jumlahLemburMasuk.setValue(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk())
										+ " (" + Common.dateFormat1.get()
												.format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
										+ ")");

						jumlahCepatKeluar.setValue(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar())
										+ " (" + Common.dateFormat1.get()
												.format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
										+ ")");

						jumlahTerlambat.setValue(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahTerlambat())
										+ " (" + Common.dateFormat1.get()
												.format(statuskehadiranKaryawanHarian.getWaktuTerlambat())
										+ ")");

						infoShift.setValue(statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
								: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai().toString());
					}
				});

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

						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.setTime(date);
						String haris = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];

						statuskehadiranKaryawanHarian
								.setDetailJenisShiftPegawai(CommonPayroll.getDetailJenisShiftPegawai(pegawai, null,
										null, statuskehadiranKaryawanHarian.ambilMasukjam(),
										statuskehadiranKaryawanHarian.getTanggal(), haris,
										statuskehadiranKaryawanHarian.getLiburNasional() != null));

						Common.refreshSaveOrUpdate(session, statuskehadiranKaryawanHarian);

						jumlahJamMasuk.setValue(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahJamMasuk())
										+ " (" + Common.dateFormat1.get()
												.format(statuskehadiranKaryawanHarian.getWaktuJamMasuk())
										+ ")");

						jumlahLemburMasuk.setValue(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk())
										+ " (" + Common.dateFormat1.get()
												.format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
										+ ")");

						jumlahCepatKeluar.setValue(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar())
										+ " (" + Common.dateFormat1.get()
												.format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
										+ ")");

						jumlahTerlambat.setValue(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahTerlambat())
										+ " (" + Common.dateFormat1.get()
												.format(statuskehadiranKaryawanHarian.getWaktuTerlambat())
										+ ")");

						infoShift.setValue(statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
								: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai().toString());
					}
				});

				jumlahJamMasuk.setParent(row);
				infoShift.setParent(row);

				jumlahLemburMasuk.setParent(row);
				jumlahCepatKeluar.setParent(row);
				jumlahTerlambat.setParent(row);

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
						Common.refreshSaveOrUpdate(session, statuskehadiranKaryawanHarian);

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

				row.appendChild(new Label(statuskehadiranKaryawanHarian.getStatusabsensi().getNama()));

				row.appendChild(new Label(statuskehadiranKaryawanHarian.ambilMasukjam() == null ? ""
						: Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.ambilMasukjam())));

				row.appendChild(new Label(statuskehadiranKaryawanHarian.ambilPulangjam() == null ? ""
						: Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.ambilPulangjam())));

				jumlahJamMasuk.setParent(row);
				infoShift.setParent(row);

				jumlahLemburMasuk.setParent(row);
				jumlahCepatKeluar.setParent(row);
				jumlahTerlambat.setParent(row);

				statuskehadiranKaryawanHarian.renderKeteranganLink(row);

			}

		}
	}

	private MyTextbox kode;
	private MyTextbox nama;

	private Criteria initCriteria(boolean order) {

		Criterion critKode = Restrictions.sqlRestriction("false");
		if (!kode.getValue().trim().equals("")) {
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critKode = Restrictions.sqlRestriction("true");
		}

		Criterion critNama = Restrictions.sqlRestriction("false");
		if (!nama.getValue().trim().equals("")) {
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critNama = Restrictions.sqlRestriction("true");
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pegawai.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))

				.add(critKode).add(critNama);

		if (order)
			criteria.addOrder(Order.asc("nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pegawai> pegawais = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pegawais);
		grid.setRowRenderer(new PegawaiRenderer());
		grid.setModelCheckMobile(strset);
		grid.renderAll();
	}

	private void display() {

		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar Pegawai"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode:")));
		toolbar.appendChild(kode = new MyTextbox());
		kode.setWidth("80px");
		kode.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama:")));
		toolbar.appendChild(nama = new MyTextbox());
		nama.setWidth("80px");
		nama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		Toolbarbutton search;
		toolbar.appendChild(search = new MyToolbarbuttonConfig("", "/img/svg/search.svg"));
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		Toolbarbutton cetak;
		toolbar.appendChild(cetak = new MyToolbarbuttonConfig("", "/img/print.png"));
		cetak.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanAbsensiPegawaiPerHari laporanAbsensiPegawaiPerHari = new LaporanAbsensiPegawaiPerHari(date);
				laporanAbsensiPegawaiPerHari.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				laporanAbsensiPegawaiPerHari.setTitle("Laporan Absensi Per Tanggal");
				laporanAbsensiPegawaiPerHari.setClosable(true);
				laporanAbsensiPegawaiPerHari.setHeight("95%");
				laporanAbsensiPegawaiPerHari.setWidth("90%");
				laporanAbsensiPegawaiPerHari.onModal();
			}
		});

		grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setWidth("0px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("NIP");
		column.setWidth("8%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Masuk");
		column.setWidth("9%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Pulang");
		column.setWidth("9%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jam {" + ItemGaji.V_JAM + "}");
		column.setWidth("9%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Shift");
		column.setWidth("14%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Lembur {" + ItemGaji.V_LEM + "}");
		column.setWidth("9%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Cepat {" + ItemGaji.V_CEP + "}");
		column.setWidth("9");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Terlambat {" + ItemGaji.V_TERL + "}");
		column.setWidth("9");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
		paging.setParent(groupbox);

		loadData(null);
	}

}
