package ais.action.master.payroll.detail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.action.master.payroll.util.PembayaranItemGajiPegawaiTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Bank;
import ais.database.model.Pegawai;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk pembayaran gaji punya pegawai per bank. Tipe ini merupakan titik
 * masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi
 * khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PembayaranGaji pembayaranGaji}, {@code
 * MyGrid grid}; inisialisasi/lifecycle ({@code initCriteria()}); pembacaan/pencarian ({@code loadData()});
 * operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
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
public class PembayaranGajiPunyaPegawaiPerBankAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PembayaranGaji pembayaranGaji;
	private MyGrid grid;

	public PembayaranGajiPunyaPegawaiPerBankAction(PembayaranGaji pembayaranGaji) {
		super();
		this.pembayaranGaji = pembayaranGaji;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PembayaranGajiPunyaPegawaiPerBankAction.this);
				if (isOpen()) {
					display();
				}
			}
		});

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PembayaranGajiPunyaPegawaiPerBankAction}. Kelas ini menerjemahkan
	 * satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PembayaranGajiPunyaPegawaiPerBankAction} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Date sekarang}, {@code Collection
	 * pangkats}; operasi lokal: {@code render}(). Aturan bisnis bersama tetap berada pada kelas induk atau service
	 * yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PembayaranGajiPunyaPegawaiPerBankAction
	 */
	class PembayaranGajiPunyaPegawaiRenderer extends ais.ui.util.MyRowRenderer {
		Date sekarang = WaktuUtil.getDate();
		@SuppressWarnings("rawtypes")
		Collection pangkats = ConstantValues.ambilBerdasarClass(KenaikanPangkat.class).values();

		public PembayaranGajiPunyaPegawaiRenderer() {

		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			Object[] o = (Object[]) data;
			Bank bank = (Bank) o[0];
			Double nilai = (Double) o[1];
			new Label(bank == null ? "Belum ditentukan" : bank.getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

		}
	}

	private Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PembayaranGajiPunyaPegawai.class).createAlias("pegawai", "pegawai")

				.add(Restrictions.eq("pembayaranGaji", pembayaranGaji));
		if (order)
			criteria.addOrder(Order.asc("pegawai.nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Event event) {

		List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = pembayaranGaji == null
				|| pembayaranGaji.getId() == null ? new ArrayList<PembayaranGajiPunyaPegawai>()
						: initCriteria(true).list();

		Map<Long, Double> mapsBank = new HashMap<Long, Double>();
		for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {
			Pegawai pegawai = pembayaranGajiPunyaPegawai.getPegawai();
			Bank bank = pegawai.ambilBank(pembayaranGajiPunyaPegawai.getFormatItemGaji());
			Long idBank = bank == null || bank.getId() == null ? -1L : bank.getId();
			Double nilai = mapsBank.get(idBank);
			if (nilai == null) {
				nilai = 0.0;
			}
			nilai += pembayaranGajiPunyaPegawai.getNilai();
			mapsBank.put(idBank, nilai);
		}

		List<Object[]> objects = new ArrayList<Object[]>();
		for (Long idBank : mapsBank.keySet()) {
			Bank bank = (Bank) ConstantValues.ambil(Bank.class.getName(), idBank);
			objects.add(new Object[] { bank, mapsBank.get(idBank) });
		}

		ListModel strset = new SimpleListModel(objects);
		grid.setRowRenderer(new PembayaranGajiPunyaPegawaiRenderer());
		grid.setModelCheckMobile(strset);
		grid.renderAll();
	}

	private void display() {
		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar Gaji Per Bank"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/add_item.png");
		button.setVisible(pembayaranGaji.getId() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						PembayaranGaji.hitungUlang(pembayaranGaji);

						loadData(null);
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {

						Session session = HibernateUtil.currentNativeSession();
						List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = session
								.createCriteria(PembayaranGajiPunyaPegawai.class)
								.add(Restrictions.eq("pembayaranGaji", pembayaranGaji)).list();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						HibernateUtil.closeSession();

						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.set(Calendar.MONTH, pembayaranGaji.getBulan() - 1);
						calendar.set(Calendar.YEAR, pembayaranGaji.getTahun());
						int size = pembayaranGajiPunyaPegawais.size();
						int index = 0;
						for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {

							index++;

							if (pembayaranGajiPunyaPegawai.getPegawai() != null
									&& pembayaranGajiPunyaPegawai.getFormatItemGaji() != null) {

								label.setValue("Memproses data rencana gaji "
										+ pembayaranGajiPunyaPegawai.getPegawai().getNama() + " ("
										+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

								PembayaranItemGajiPegawaiTreeModel pembayaranItemGajiPegawaiTreeModel = new PembayaranItemGajiPegawaiTreeModel(
										false, pembayaranGajiPunyaPegawai);
								try {
									pembayaranItemGajiPegawaiTreeModel.reset(calendar.getTime(), null,
											pembayaranGaji.getBulan(), pembayaranGaji.getTahun());
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/detail/PembayaranGajiPunyaPegawaiPerBankAction.java:195");
								}
								pembayaranItemGajiPegawaiTreeModel.setLunas(calendar.getTime());
							}
						}

						label.setValue("");
											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Bank");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setAlign("right");
		column.setWidth("20%");

		loadData(null);
	}

}
