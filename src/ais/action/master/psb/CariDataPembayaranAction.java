package ais.action.master.psb;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.bni.BniBackandProsess;
import ais.action.master.bri.BriBackandProsess;
import ais.action.master.sekolah.util.PembayaranSiswaUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.bni.BniRequest;
import ais.database.model.bri.BriRequest;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk cari data pembayaran. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox noRegistrasi}, {@code Textbox
 * pinPassword}, {@code Row tampilanPin}, {@code CalonSiswa calonSiswa}, {@code MyGrid grid}, {@code North
 * uploadMenu}, {@code North menuLogin}, {@code Combobox tahun}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data
 * ({@code onReset()}); operasi domain lain ({@code onLogin()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class CariDataPembayaranAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1155733365712985677L;

	private Textbox noRegistrasi;
	private Textbox pinPassword;
	private Row tampilanPin;

	private CalonSiswa calonSiswa;
	private MyGrid grid;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void onLogin(Event event) throws Exception {
		if (noRegistrasi.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Nomor Pendaftaran belum diisi. Langkah yang dapat dilakukan: (1) Ketik nomor pendaftaran atau nomor ujian Anda pada kolom yang tersedia; (2) Pastikan tidak ada spasi di awal atau akhir; (3) Ulangi proses masuk. Jika masih mengalami kendala, hubungi panitia penerimaan siswa baru.", "PERINGATAN", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Session session = HibernateUtil.currentSession();
		if (tampilanPin != null && tampilanPin.isVisible()) {
			if (pinPassword.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Mohon maaf, PIN / Kata Sandi belum diisi. Langkah yang dapat dilakukan: (1) Ketik PIN atau kata sandi yang diberikan panitia pada kolom yang tersedia; (2) Pastikan penulisan sudah benar (perhatikan huruf besar/kecil); (3) Ulangi proses masuk. Jika masih mengalami kendala, hubungi panitia penerimaan siswa baru.", "PERINGATAN", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}
			calonSiswa = (CalonSiswa) ConstantValues.simpleObject(
					session.createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
							.add(Restrictions.ilike("pinPassword", pinPassword.getValue().trim(), MatchMode.EXACT))
							.add(Restrictions.or(
									Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
									Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
					CalonSiswa.class);
		} else {
			calonSiswa = (CalonSiswa) ConstantValues.simpleObject(
					session.createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
							.add(Restrictions.or(
									Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
									Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
					CalonSiswa.class);
		}
		if (calonSiswa == null) {
			if (tampilanPin != null && tampilanPin.isVisible()) {
				MyMessageboxConfig.show(
						"Mohon maaf, Calon Siswa dengan nomor pendaftaran \"" + noRegistrasi.getValue() + "\" dan PIN / Password yang dimasukkan tidak ditemukan. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan nomor pendaftaran; (2) Pastikan PIN / Password sudah benar; (3) Hubungi panitia penerimaan siswa baru jika masih mengalami kendala.",
						"PERINGATAN", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			} else {
				MyMessageboxConfig.show(
						"Mohon maaf, Calon Siswa dengan nomor pendaftaran \"" + noRegistrasi.getValue()
								+ "\" tidak ditemukan, atau waktu terakhir bisa masuk telah terlewat. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan nomor pendaftaran; (2) Pastikan masih dalam periode waktu yang ditentukan; (3) Hubungi panitia penerimaan siswa baru jika masih mengalami kendala.",
						"PERINGATAN", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
			return;
		}

		if (calonSiswa != null) {

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(calonSiswa.getTanggalLahir());
			int thn = calendar.get(Calendar.YEAR);
			int bln = calendar.get(Calendar.MONTH);
			int tgl = calendar.get(Calendar.DATE);
			boolean kondisiTglLahir = (tahun.getSelectedItem() == null ? false
					: tahun.getSelectedItem().getValue().equals(thn))
					&& (bulan.getSelectedItem() == null ? false : bulan.getSelectedItem().getValue().equals(bln))
					&& (tanggal.getSelectedItem() == null ? false : tanggal.getSelectedItem().getValue().equals(tgl));

			if (kondisiTglLahir) {

				onSearchDefault(null);

				menuLogin.setVisible(false);

				Common.clear(uploadMenu);

				uploadMenu.setHeight("100px");

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(uploadMenu);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("40%");

				column = new MyColumnConfig();
				column.setParent(columns);

				final Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

				row.appendChild(new ais.ui.util.MyLabelBold("Nomor Pendaftaran"));
				row.appendChild(new ais.ui.util.MyLabelBold(calonSiswa.getNoRegistrasi()));

				if (calonSiswa.getNoUjian() != null) {
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelBold("Nomor Ujian"));
					row.appendChild(new ais.ui.util.MyLabelBold(calonSiswa.getNoUjian()));
				}

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelBold("Nama"));
				row.appendChild(new ais.ui.util.MyLabelBold(calonSiswa.getNama()));

			} else {
				MyMessageboxConfig.show("Mohon maaf, Nomor Referensi atau Tanggal Lahir yang Anda masukkan tidak sesuai dengan data kami. Langkah yang dapat dilakukan: (1) Periksa kembali nomor pendaftaran atau referensi yang dimasukkan; (2) Pastikan Tahun, Bulan, dan Tanggal Lahir sudah dipilih dengan benar; (3) Ulangi proses masuk. Jika masih mengalami kendala, hubungi panitia penerimaan siswa baru.",
						"PERINGATAN", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
		} else {
			MyMessageboxConfig.show(
					"Mohon maaf, nomor registrasi Anda tidak ditemukan dalam sistem. Langkah yang dapat dilakukan: (1) Pastikan Anda telah menyelesaikan prosedur pembayaran pendaftaran terlebih dahulu; (2) Periksa kembali nomor registrasi yang dimasukkan; (3) Hubungi panitia penerimaan siswa baru untuk konfirmasi status pendaftaran Anda. Jika masih mengalami kendala, hubungi Administrator.",
					"PERINGATAN", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		}
	}

	private North uploadMenu;
	private North menuLogin;
	private Combobox tahun;
	private Combobox bulan;
	private Combobox tanggal;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (tampilanPin != null) { tampilanPin.setVisible(false); }
		MyComboitemConfig comboitem;
		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 80; i < ais.ui.util.WaktuUtil
				.getCalendar().get(Calendar.YEAR) + 1; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setValue(i);
			comboitem.setLabel(i + "");
			tahun.appendChild(comboitem);
		}

		for (int i = 1; i <= 31; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setValue(i);
			comboitem.setLabel(i + "");
			tanggal.appendChild(comboitem);
		}

		Common.createComboBulan(bulan);
		Common.initLaguage();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void onSearchDefault(Event event) throws Exception {
		Session session = HibernateUtil.currentSession();

		List ruangPaketList = session.createCriteria(PembayaranSiswa.class)
				.add(Restrictions.eq("calonSiswa", calonSiswa)).list();

		List request = session.createCriteria(BriRequest.class)
				.add(Restrictions.gt("bill_expired", ais.ui.util.WaktuUtil.getDate()))
				.add(Restrictions.ne("status", "Payment Sukses")).add(Restrictions.eq("calonSiswa", calonSiswa)).list();
		System.out.println("request => " + request.size());
		ruangPaketList.addAll(request);

		request = session.createCriteria(BniRequest.class)
				.add(Restrictions.gt("billExpired", ais.ui.util.WaktuUtil.getDate()))
				.add(Restrictions.ne("status", "Payment Sukses")).add(Restrictions.eq("calonSiswa", calonSiswa)).list();
		System.out.println("request => " + request.size());
		ruangPaketList.addAll(request);

		ListModel strset = new SimpleListModel(ruangPaketList);
		grid.setRowRenderer(new CalonRenderer());
		grid.setModelCheckMobile(strset, true);

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link CariDataPembayaranAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link CariDataPembayaranAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see CariDataPembayaranAction
	 */
	class CalonRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			System.out.println("arg1 => " + arg1);
			if (arg1 instanceof BriRequest) {
				final BriRequest briRequest = (BriRequest) arg1;

				CommonMedia.tampilkanGambarKecil(briRequest.getCalonSiswa()).setParent(arg0);

				new Label(briRequest.getCalonSiswa().getNoRegistrasi()).setParent(arg0);
				new Label(briRequest.getCalonSiswa().getNoUjian()).setParent(arg0);
				new Label(briRequest.getCalonSiswa().getNama().toUpperCase()).setParent(arg0);

				new Label(briRequest.getVa()).setParent(arg0);
				Hbox toolbar = new Hbox();
				toolbar.setParent(arg0);
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cek Pembayaran Pembayaran", "/img/print.png");
				button.setTooltiptext("Cetak");
				button.setOrient("vertical");
				button.addEventListener("onClick", new EventListener() {
					@SuppressWarnings({})
					@Override
					public void onEvent(Event event) throws Exception {
						Session session = HibernateUtil.currentNativeSession();
						session.refresh(briRequest);
						briRequest.setHapusCicilanSebelumnya(true);
						briRequest.setCheckUlang(true);
						session.getTransaction().begin();
						Common.refreshUpdate(session, briRequest);
						session.getTransaction().commit();

						BriBackandProsess.checkSatu(briRequest, session);
						HibernateUtil.closeSession();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								if (Common.getApakahAdmin())
									MyMessageboxConfig.show(
											"Cek ulang telah dilakukan, status pembayaran adalah "
													+ briRequest.getStatus(),
											"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
							}
						});
					}
				});
				button.setParent(toolbar);
			} else if (arg1 instanceof BniRequest) {
				final BniRequest bniRequest = (BniRequest) arg1;

				CommonMedia.tampilkanGambarKecil(bniRequest.getCalonSiswa()).setParent(arg0);

				new Label(bniRequest.getCalonSiswa().getNoRegistrasi()).setParent(arg0);
				new Label(bniRequest.getCalonSiswa().getNoUjian()).setParent(arg0);
				new Label(bniRequest.getCalonSiswa().getNama().toUpperCase()).setParent(arg0);

				new Label(bniRequest.getVa()).setParent(arg0);
				Hbox toolbar = new Hbox();
				toolbar.setParent(arg0);
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cek Pembayaran Pembayaran", "/img/print.png");
				button.setTooltiptext("Cetak");
				button.setOrient("vertical");
				button.addEventListener("onClick", new EventListener() {
					@SuppressWarnings({})
					@Override
					public void onEvent(Event event) throws Exception {
						Session session = HibernateUtil.currentNativeSession();
						session.refresh(bniRequest);
						bniRequest.setHapusCicilanSebelumnya(true);
						bniRequest.setCheckUlang(true);
						session.getTransaction().begin();
						Common.refreshUpdate(session, bniRequest);
						session.getTransaction().commit();
						CalonSiswa siswa = bniRequest.getCalonSiswa();
						String ipClient = (Common.getKonfigurasi("bni_ip_client", "").getNilai());
						if (!ipClient.trim().isEmpty()) {
							ipClient = ipClient + "/BniForwarder";
						}
						String strURL = !ipClient.trim()
								.isEmpty()
										? ipClient
										: (siswa != null && siswa.getSekolah() != null
												&& !siswa.getSekolah().getBniGatewayUrl().isEmpty()
														? siswa.getSekolah().getBniGatewayUrl()
														: (Common
																.getKonfigurasi("bni_gateway_url",
																		"https://apibeta.bni-ecollection.com/")
																.getNilai()));

						BniBackandProsess.check(strURL, bniRequest, session);
						HibernateUtil.closeSession();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (Common.getApakahAdmin())
									MyMessageboxConfig.show(
											"Cek ulang telah dilakukan, status pembayaran adalah "
													+ bniRequest.getStatus(),
											"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
							}
						});
					}
				});
				button.setParent(toolbar);
			} else if (arg1 instanceof PembayaranSiswa) {
				final PembayaranSiswa pembayaranSiswa = (PembayaranSiswa) arg1;

				CommonMedia.tampilkanGambarKecil(pembayaranSiswa.getCalonSiswa()).setParent(arg0);

				new Label(pembayaranSiswa.getCalonSiswa().getNoRegistrasi()).setParent(arg0);
				new Label(pembayaranSiswa.getCalonSiswa().getNoUjian()).setParent(arg0);
				new Label(pembayaranSiswa.getCalonSiswa().getNama().toUpperCase()).setParent(arg0);

				new Label(pembayaranSiswa.toString()).setParent(arg0);
				Hbox toolbar = new Hbox();
				toolbar.setParent(arg0);
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
				button.setTooltiptext("Cetak");
				button.setOrient("vertical");
				button.addEventListener("onClick", new EventListener() {
					@SuppressWarnings({})
					@Override
					public void onEvent(Event event) throws Exception {
						PembayaranSiswaUtil.cetakBri(pembayaranSiswa, null);

					}
				});
				button.setParent(toolbar);
			}

		}

	}

	public void onReset() {
		noRegistrasi.setValue("");

	}

}
