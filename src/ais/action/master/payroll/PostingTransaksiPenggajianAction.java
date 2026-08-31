package ais.action.master.payroll;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.akunting.ProsesTransferStandingInstructionAction;
import ais.action.master.akunting.helper.AmbilDataPegawaiNotDefaultBanbox;
import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Bank;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.akunting.ProsesTransferStandingInstruction;
import ais.database.model.payroll.CaraPembayaranGaji;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.database.model.payroll.PembayaranItemGajiPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk posting transaksi penggajian. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * AmbilDataPegawaiNotDefaultBanbox searchnama}, {@code Combobox bulan}, {@code Intbox tahun}, {@code Combobox
 * caraBayar}, {@code boolean approve}, {@code boolean delete}, {@code Paging paging}; inisialisasi/lifecycle
 * ({@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefaultTanpaProgress()}, {@code onSearchDefault()}, {@code loadDataDenganProgressPosting()}); mutasi
 * data ({@code onBatalkanPostingSemua()}, {@code onPostingSemua()}, {@code postingSemua()}, {@code
 * batalkanPostingSemua()}); operasi domain lain ({@code kriteriaPenggajianStatic()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PostingTransaksiPenggajianAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyGrid grid;

	private AmbilDataPegawaiNotDefaultBanbox searchnama;
	private Combobox bulan;
	private Intbox tahun;
	private Combobox caraBayar;

	private boolean approve = false;
	@SuppressWarnings("unused")
	private boolean delete = false;

	private Paging paging;

	private MyToolbarbuttonConfig sent;
	private MyDatebox mulai;
	private MyDatebox sampai;

	public boolean adminLain;
	private Tbmuser tbmuser;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();

		adminLain = Common.getApakahAdmin() || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);

		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (sent != null) { sent.setVisible(approve); }

		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i);
			bulan.appendChild(comboitem);
		}

		Comboitem comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		bulan.appendChild(comboitem);
		if (bulan != null) { bulan.setSelectedItem(comboitem); }
		if (bulan != null) { bulan.setReadonly(true); }

		if (tahun != null) { tahun.setValue(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)); }

		SatuanKerja satuanKerja = Common.getSatuanKerja();
		CaraPembayaranGaji caraPembayaranGajiDefault = (CaraPembayaranGaji) HibernateUtil.currentSession()
				.createCriteria(CaraPembayaranGaji.class).add(Restrictions.eq("defaultPembayaran", true))
				.add(Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.and(Restrictions.isNotNull("akun"),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))))
				.setMaxResults(1).uniqueResult();

		Common.insertCombo(caraBayar, "nama", "akun", CaraPembayaranGaji.class,
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.and(Restrictions.isNotNull("akun"),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));
		Common.selectComboItem(true, caraBayar, caraPembayaranGajiDefault);
		if (caraBayar != null) { caraBayar.setReadonly(true); }

		Common.appendKeToolbar(new Space(), sent, comp);
		Common.appendKeToolbar(new Space(), sent, comp);
		Common.appendKeToolbar(new Space(), sent, comp);

		Common.appendKeToolbar(new ais.ui.util.MyLabelConfig("Tanggal:"), sent, comp);

		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		Common.appendKeToolbar(mulai = new MyDatebox(calendar.getTime()), sent, comp);
		if (mulai != null) { mulai.setReadonly(true); }
		Common.appendKeToolbar(new Label(ais.common.Common.getBahasaConfig(" s.d ")), sent, comp);

		mulai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);
			}
		});

		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 8);

		Common.appendKeToolbar(sampai = new MyDatebox(calendar.getTime()), sent, comp);
		if (sampai != null) { sampai.setReadonly(true); }

		sampai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);
			}
		});

		loadDataDenganProgressPosting(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PostingTransaksiPenggajianAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PostingTransaksiPenggajianAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PostingTransaksiPenggajianAction
	 */
	class TransaksiRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai = (PembayaranGajiPunyaPegawai) arg1;
			final PembayaranGaji pembayaranGaji = pembayaranGajiPunyaPegawai.getPembayaranGaji();
			arg0.setAttribute("pembayaranGaji", pembayaranGaji);

			Pegawai pegawai = pembayaranGajiPunyaPegawai.getPegawai();

			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);
			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(PembayaranGajiPunyaPegawai.class, pembayaranGajiPunyaPegawai,
					pegawai.getNama())).setParent(arg0);

			if (pembayaranGaji != null && pembayaranGaji.getStandingInstruction() != null) {

				Set<Long> siss = new HashSet<Long>();
				for (String ss : pembayaranGaji.getStandingInstruction().getProsesStanding().split(",")) {
					siss.add(Long.parseLong(ss));

				}

				Session session = HibernateUtil.currentSession();

				for (Long s : siss) {
					final ProsesTransferStandingInstruction prosesTransferStandingInstruction = (ProsesTransferStandingInstruction) session
							.createCriteria(ProsesTransferStandingInstruction.class).add(Restrictions.idEq(s))
							.uniqueResult();
					if (prosesTransferStandingInstruction != null) {
						A a = new A(prosesTransferStandingInstruction.getKode());
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								ProsesTransferStandingInstructionAction.onAddExternal(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										onSearchDefault(arg0);
									}
								}, prosesTransferStandingInstruction);

							}
						});
						a.setStyle("font-size:12px;");
						a.setParent(aaa);
					}
				}
			}

			new Label(Common.dateFormat6.get().format(pembayaranGajiPunyaPegawai.getTanggalBayar())).setParent(arg0);
			new Label(pembayaranGaji.getBulan() + "").setParent(arg0);
			new Label(pembayaranGaji.getTahun() + "").setParent(arg0);

			new Label(pembayaranGaji.getCaraPembayaranGaji() == null ? ""
					: pembayaranGaji.getCaraPembayaranGaji().getNama()).setParent(arg0);

			new Label(Common.numberFormat.get().format(pembayaranGajiPunyaPegawai.getNilai())).setParent(arg0);

			Session session = HibernateUtil.currentSession();
			List<PembayaranItemGajiPegawai> pembayaranItemGajiPegawais = ConstantValues
					.simpleList(
							session.createCriteria(PembayaranItemGajiPegawai.class).add(Restrictions.gt("nilai", 0.1))
									.add(Restrictions.or(Restrictions.isNotNull("akun"),
											Restrictions.isNotNull("akunDebet")))
									.add(Restrictions.eq("pembayaranGajiPunyaPegawai", pembayaranGajiPunyaPegawai)),
							PembayaranItemGajiPegawai.class);

			List<Akun> akunsDebets = new ArrayList<Akun>();
			List<Akun> akunsKredits = new ArrayList<Akun>();

			List<Double> nilaiDebets = new ArrayList<Double>();
			List<Double> nilaiKredits = new ArrayList<Double>();

			for (PembayaranItemGajiPegawai pembayaranItemGajiPegawai : pembayaranItemGajiPegawais) {
				if (pembayaranItemGajiPegawai.getAkun() != null) {

					akunsKredits.add(pembayaranItemGajiPegawai.getAkun());
					nilaiKredits.add(pembayaranItemGajiPegawai.getNilai());

				}
				if (pembayaranItemGajiPegawai.getAkunDebet() != null) {

					akunsDebets.add(pembayaranItemGajiPegawai.getAkunDebet());
					nilaiDebets.add(pembayaranItemGajiPegawai.getNilai());

				}
			}

			Bank bank = pegawai.ambilBank(pembayaranGajiPunyaPegawai.getFormatItemGaji());

			if (bank != null && bank.getAkun() != null) {
				akunsKredits.add(bank.getAkun());
				nilaiKredits.add(pembayaranGajiPunyaPegawai.getNilai());
			} else if (pembayaranGajiPunyaPegawai.getPembayaranGaji().getCaraPembayaranGaji() != null
					&& pembayaranGajiPunyaPegawai.getPembayaranGaji().getCaraPembayaranGaji().getAkun() != null) {
				akunsKredits.add(pembayaranGajiPunyaPegawai.getPembayaranGaji().getCaraPembayaranGaji().getAkun());
				nilaiKredits.add(pembayaranGajiPunyaPegawai.getNilai());
			}

			if (!akunsDebets.isEmpty() && !akunsKredits.isEmpty()) {
				GrupTransaksi.tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits).setParent(arg0);
			} else {
				new Label("Transaksi tidak valid."
						+ (!akunsDebets.isEmpty() ? " Debet: " + akunsDebets + "." : " Akun debet tidak ada.")
						+ (!akunsKredits.isEmpty() ? " Kredit: " + akunsKredits + "." : " Akun kredit tidak ada."))
						.setParent(arg0);
			}

			new Label(pembayaranGajiPunyaPegawai.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: pembayaranGajiPunyaPegawai.getPostingHistory().toString()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			button.setTooltiptext("Batalkan Posting Data");
			button.setVisible(adminLain && pembayaranGajiPunyaPegawai.getPostingHistory() != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pembayaranGajiPunyaPegawai.setPostingHistory(null);
							Common.refreshSaveOrUpdate(pembayaranGajiPunyaPegawai);
							HibernateUtil.currentSession()
									.createSQLQuery(
											"delete from akunting.grup_transaksi where pembayaran_gaji_punya_pegawai="
													+ pembayaranGajiPunyaPegawai.getId() + " and closing is null")
									.executeUpdate();

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadDataDenganProgressPosting(null);
								}
							});
						}
					});

				}

			});
			button.setParent(toolbar);

			if (!akunsDebets.isEmpty() && !akunsKredits.isEmpty()) {
				button = new MyToolbarbuttonConfig("", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Posting Data");
				button.setVisible(pembayaranGajiPunyaPegawai.getPostingHistory() == null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentNativeSession();

								session.refresh(pembayaranGajiPunyaPegawai);

								PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_PENGGAJIAN);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan("Posting manual oleh " + tbmuser.getUserNama()
										+ " pada waktu " + Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.getTransaction().begin();
								session.save(postingHistory);
								session.getTransaction().commit();

								List<PembayaranItemGajiPegawai> pembayaranItemGajiPegawais = ConstantValues
										.simpleList(
												session.createCriteria(PembayaranItemGajiPegawai.class)
														.add(Restrictions.gt("nilai", 0.1))
														.add(Restrictions.or(Restrictions.isNotNull("akun"),
																Restrictions.isNotNull("akunDebet")))
														.add(Restrictions.eq("pembayaranGajiPunyaPegawai",
																pembayaranGajiPunyaPegawai)),
												PembayaranItemGajiPegawai.class);

								List<Akun> akunDebet = new ArrayList<Akun>();
								List<Akun> akunKredit = new ArrayList<Akun>();

								List<Double> nilaiDebets = new ArrayList<Double>();
								List<Double> nilaiKredits = new ArrayList<Double>();

								for (PembayaranItemGajiPegawai pembayaranItemGajiPegawai : pembayaranItemGajiPegawais) {
									if (pembayaranItemGajiPegawai.getAkun() != null) {
										akunKredit.add(pembayaranItemGajiPegawai.getAkun());

										nilaiKredits.add(pembayaranItemGajiPegawai.getNilai());

									}
									if (pembayaranItemGajiPegawai.getAkunDebet() != null) {

										akunDebet.add(pembayaranItemGajiPegawai.getAkunDebet());

										nilaiDebets.add(pembayaranItemGajiPegawai.getNilai());

									}
								}
								Pegawai pegawai = pembayaranGajiPunyaPegawai.getPegawai();
								Bank bank = pegawai.ambilBank(pembayaranGajiPunyaPegawai.getFormatItemGaji());

								if (bank != null && bank.getAkun() != null) {
									akunKredit.add(bank.getAkun());
									nilaiKredits.add(pembayaranGajiPunyaPegawai.getNilai());
								} else if (pembayaranGajiPunyaPegawai.getPembayaranGaji()
										.getCaraPembayaranGaji() != null
										&& pembayaranGajiPunyaPegawai.getPembayaranGaji().getCaraPembayaranGaji()
												.getAkun() != null) {

									akunKredit.add(pembayaranGajiPunyaPegawai.getPembayaranGaji()
											.getCaraPembayaranGaji().getAkun());

									nilaiKredits.add(pembayaranGajiPunyaPegawai.getNilai());

								}

								String ket = "Pembayaran gaji " + pembayaranGajiPunyaPegawai.getPegawai().getNama()
										+ " bulan " + pembayaranGajiPunyaPegawai.getPembayaranGaji().getBulan()
										+ " tahun " + pembayaranGajiPunyaPegawai.getPembayaranGaji().getTahun();

								Akun akunDenda = null;
								Akun akunPiutangDenda = null;
								Double denda = 0.0;
								Boolean apakahUangMasuk = true;

								session.getTransaction().begin();
								CommonAkunting.saveTransaksi(akunDebet.toArray(new Akun[] {}),
										akunKredit.toArray(new Akun[] {}), akunDenda, akunPiutangDenda, postingHistory,
										apakahUangMasuk, ket, pembayaranGajiPunyaPegawai.getTanggalBayar(),
										nilaiDebets.toArray(new Double[] {}), nilaiKredits.toArray(new Double[] {}),
										denda, pembayaranGajiPunyaPegawai,
										pembayaranGajiPunyaPegawai.getPegawai().getSatuanKerja(), session);
								session.getTransaction().commit();

								pembayaranGajiPunyaPegawai.setPostingHistory(postingHistory);
								session.getTransaction().begin();
								Common.refreshUpdate(session, pembayaranGajiPunyaPegawai);
								session.getTransaction().commit();

								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
								HibernateUtil.closeSession();

								loadDataDenganProgressPosting(null);
							}
						});

					}

				});
			}
			button.setParent(toolbar);

		}

	}

	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show(
				"Apakah Bapak/Ibu yakin ingin membatalkan posting transaksi penggajian ini? Perlu diketahui bahwa seluruh transaksi penggajian yang telah terposting akan dibatalkan.",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {
								pembayaranGajiPunyaPegawai.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pembayaranGajiPunyaPegawai);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where pembayaran_gaji_punya_pegawai="
												+ pembayaranGajiPunyaPegawai.getId() + " and closing is null")
										.executeUpdate();
							}
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDenganProgressPosting(null);
							}
						});
					}
				});

	}

	public void onPostingSemua(Event event) throws Exception {
		if (grid == null || grid.getRows() == null) {
			return;
		}

		final Window addWindow = new Window();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting data akun");
		addWindow.setWidth("700px");
		addWindow.setHeight("300px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setTitle("Posting semua transaksi penggajian");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label("Tanggal / Waktu"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Diposting oleh")));
		row.appendChild(new Label(Common.getCurrentUser().ambilPegawai() == null ? Common.getCurrentUser().getUserId()
				: Common.getCurrentUser().ambilPegawai().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		final MyTextbox keterangan;
		row.appendChild(keterangan = new MyTextbox());
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Date tgl = tanggal.getValue();
				if (tgl == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, kolom Tanggal wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan Tanggal pada kolom yang tersedia; (2) pastikan Tanggal tidak dikosongkan; (3) ulangi kembali proses ini.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (keterangan.getValue().trim().equals("")) {
					MyMessageboxConfig.show(
							"Mohon maaf, kolom Keterangan wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Keterangan pada kolom yang tersedia; (2) pastikan Keterangan tidak dikosongkan; (3) ulangi kembali proses ini.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin mem-posting transaksi penggajian ini? Perlu diketahui bahwa data yang telah diposting akan tercatat pada jurnal dan tidak dapat diubah secara langsung.",
							"Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									final Tbmuser tbmuser = Common.getCurrentUser();
									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											MyMessageboxConfig.show("Alhamdulillah, posting transaksi penggajian pegawai telah berhasil dilakukan.",
													"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															onSearchDefault(arg0);
														}
													});

											addWindow.detach();
										}
									});

									new Thread(new Runnable() {

										@Override
										public void run() {
											try {

											List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = initCriteria(
													true).add(Restrictions.isNull("postingHistory")).list();

											PostingHistory postingHistory = new PostingHistory(
													PostingHistory.JENIS_PENGGAJIAN);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setTanggal(tgl);
											postingHistory.setKeterangan(keterangan.getValue().trim());

											Session session = HibernateUtil.currentNativeSession();
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {

												Pegawai pegawai = pembayaranGajiPunyaPegawai.getPegawai();

												List<PembayaranItemGajiPegawai> pembayaranItemGajiPegawais = ConstantValues
														.simpleList(
																session.createCriteria(PembayaranItemGajiPegawai.class)
																		.add(Restrictions.gt("nilai", 0.1))
																		.add(Restrictions.or(
																				Restrictions.isNotNull("akun"),
																				Restrictions.isNotNull("akunDebet")))
																		.add(Restrictions.eq(
																				"pembayaranGajiPunyaPegawai",
																				pembayaranGajiPunyaPegawai)),
																PembayaranItemGajiPegawai.class);

												List<Akun> akunDebet = new ArrayList<Akun>();
												List<Akun> akunKredit = new ArrayList<Akun>();

												List<Double> nilaiDebets = new ArrayList<Double>();
												List<Double> nilaiKredits = new ArrayList<Double>();

												Double nilaiDebet = 0.0;
												Double nilaiKredit = 0.0;

												for (PembayaranItemGajiPegawai pembayaranItemGajiPegawai : pembayaranItemGajiPegawais) {
													if (pembayaranItemGajiPegawai.getAkun() != null) {
														akunKredit.add(pembayaranItemGajiPegawai.getAkun());
														nilaiKredits.add(pembayaranItemGajiPegawai.getNilai());

														nilaiKredit += pembayaranItemGajiPegawai.getNilai();

													}
													if (pembayaranItemGajiPegawai.getAkunDebet() != null) {
														akunDebet.add(pembayaranItemGajiPegawai.getAkunDebet());
														nilaiDebets.add(pembayaranItemGajiPegawai.getNilai());

														nilaiDebet += pembayaranItemGajiPegawai.getNilai();

													}
												}

												Bank bank = pegawai
														.ambilBank(pembayaranGajiPunyaPegawai.getFormatItemGaji());

												if (bank != null && bank.getAkun() != null) {
													akunKredit.add(bank.getAkun());
													nilaiKredits.add(pembayaranGajiPunyaPegawai.getNilai());

													nilaiKredit += pembayaranGajiPunyaPegawai.getNilai();

												} else if (pembayaranGajiPunyaPegawai.getPembayaranGaji()
														.getCaraPembayaranGaji() != null
														&& pembayaranGajiPunyaPegawai.getPembayaranGaji()
																.getCaraPembayaranGaji().getAkun() != null) {

													akunKredit.add(pembayaranGajiPunyaPegawai.getPembayaranGaji()
															.getCaraPembayaranGaji().getAkun());

													nilaiKredits.add(pembayaranGajiPunyaPegawai.getNilai());

													nilaiKredit += pembayaranGajiPunyaPegawai.getNilai();

												}

												if (nilaiDebet.intValue() == nilaiKredit.intValue()) {

													String ket = "Pembayaran gaji "
															+ pembayaranGajiPunyaPegawai.getPegawai().getNama()
															+ " bulan "
															+ pembayaranGajiPunyaPegawai.getPembayaranGaji().getBulan()
															+ " tahun "
															+ pembayaranGajiPunyaPegawai.getPembayaranGaji().getTahun();

													Akun akunDenda = null;
													Akun akunPiutangDenda = null;
													Double denda = 0.0;
													Boolean apakahUangMasuk = true;

													try {
														session.getTransaction().begin();
														boolean tersimpan = CommonAkunting.saveTransaksi(
																akunDebet.toArray(new Akun[] {}),
																akunKredit.toArray(new Akun[] {}), akunDenda,
																akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
																pembayaranGajiPunyaPegawai.getTanggalBayar(),
																nilaiDebets.toArray(new Double[] {}),
																nilaiKredits.toArray(new Double[] {}), denda,
																pembayaranGajiPunyaPegawai, pembayaranGajiPunyaPegawai
																		.getPegawai().getSatuanKerja(),
																session);
														session.getTransaction().commit();

														if (tersimpan) {
															pembayaranGajiPunyaPegawai
																	.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(pembayaranGajiPunyaPegawai);
															session.getTransaction().commit();
														}
													} catch (Exception e) {
														// TODO Auto-generated catch block
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
												}
											}

											label.setValue("");
											HibernateUtil.closeSession();
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});

			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	private Criteria initCriteria(boolean order) {
		Pegawai selectedPegawai = (Pegawai) searchnama.getAttribute("pegawai");
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PembayaranGajiPunyaPegawai.class)

				.createAlias("pembayaranGaji", "pembayaranGaji")

				.add(Restrictions.isNotNull("pembayaranGaji.disetujuiOleh"))

				.add((mulai == null || sampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(tanggal_bayar_gaji) between date('" + Common.databaseDateFormat.get().format(mulai.getValue())
								+ "') and  date('" + Common.databaseDateFormat.get().format(sampai.getValue()) + "')")))

				.add(bulan.getSelectedItem() == null || bulan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("pembayaranGaji.bulan", bulan.getSelectedItem().getValue()))

				.add(caraBayar.getSelectedItem() == null || caraBayar.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("pembayaranGaji.caraPembayaranGaji", caraBayar.getSelectedItem().getValue()))

				.add(tahun.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("pembayaranGaji.tahun", tahun.getValue()))

				.add(selectedPegawai == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("pegawai", selectedPegawai));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PembayaranGajiPunyaPegawai> pembayaranGaji = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pembayaranGaji);
		grid.setRowRenderer(new TransaksiRenderer());
		grid.setModelCheckMobile(strset);
		grid.renderAll();

	}



	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	private boolean postingJurnalLoadingAktif = false;
	private boolean postingJurnalReloadTertunda = false;

	private void loadDataDenganProgressPosting(final org.zkoss.zk.ui.event.Event event) {
		if (postingJurnalLoadingAktif) {
			postingJurnalReloadTertunda = true;
			ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Ulang Data Posting Jurnal",
					"Permintaan reload baru diterima. Data akan dimuat ulang setelah proses yang berjalan selesai.", 12);
			return;
		}
		postingJurnalLoadingAktif = true;
		postingJurnalReloadTertunda = false;
		ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Data Posting Jurnal",
				"Menyiapkan filter dan tabel data jurnal.", 7);
		Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event timerEvent) throws Exception {
				try {
					ais.ui.util.PostingJurnalLoadingUtil.update("Mengambil Data Posting Jurnal",
							"Mencari data sesuai tanggal, status posting, dan filter halaman.", 48);
					onSearchDefaultTanpaProgress(event);
					ais.ui.util.PostingJurnalLoadingUtil.update("Merapikan Tampilan",
							"Menyusun tabel, paging, status posting, dan preview jurnal.", 92);
				} finally {
					boolean reloadLagi = postingJurnalReloadTertunda;
					postingJurnalReloadTertunda = false;
					postingJurnalLoadingAktif = false;
					if (reloadLagi) {
						ais.ui.util.PostingJurnalLoadingUtil.update("Memuat Ulang Data Posting Jurnal",
								"Filter atau halaman berubah saat data sedang diproses. Data akan dimuat ulang sekarang.", 96);
						Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
							@Override
							public void onEvent(org.zkoss.zk.ui.event.Event ulangEvent) throws Exception {
								loadDataDenganProgressPosting(event);
							}
						});
					} else {
						ais.ui.util.PostingJurnalLoadingUtil.complete("Data Posting Jurnal Siap",
								"Tabel sudah selesai dimuat dan siap digunakan.", 100);
					}
				}
			}
		});
	}


	// ================================================================= jalur API dasbor draft jurnal

	/**
	 * Kriteria dokumen yang SAMA dengan baris "Penggajian Pegawai" di dasbor draft jurnal:
	 * baris gaji per pegawai yang induk pembayaran gajinya sudah disetujui, pada rentang
	 * tanggal bayar gaji.
	 */
	private static Criteria kriteriaPenggajianStatic(Session session, java.util.Date mulai,
			java.util.Date sampai) {
		Criteria c = session.createCriteria(PembayaranGajiPunyaPegawai.class)
				.createAlias("pembayaranGaji", "pembayaranGaji")
				.add(Restrictions.isNotNull("pembayaranGaji.disetujuiOleh"));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(this_.tanggal_bayar_gaji) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/**
	 * Posting SEMUA penggajian per pegawai pada rentang -- jalur API dasbor Draft Jurnal POS.
	 * Susunan jurnal per pegawai mengikuti tombol "Posting Semua" layar: tiap item gaji bernilai
	 * &gt; 0.1 masuk ke sisi akun/akunDebet-nya, lalu neto gaji dikreditkan ke akun bank pegawai
	 * (bila ada) atau akun cara pembayaran gaji, dan jurnal hanya ditulis bila total debet dan
	 * kredit seimbang (perbandingan intValue, mengikuti layar).
	 *
	 * <p>Dua cacat tombol massal layar yang dulu TIDAK diwarisi mesin ini: (1) cabang bank
	 * lupa menambah akumulator nilaiKredit sehingga pegawai ber-rekening-bank SELALU gagal
	 * penjaga keseimbangan; (2) dokumen tidak pernah DICAP ({@code setPostingHistory})
	 * sehingga hasil postingnya terhitung draf selamanya dan run massal berikutnya
	 * menjurnalkannya ulang. Keduanya kini sudah diperbaiki di tombolnya mengikuti bentuk di
	 * sini -- berikut tombol batal massal yang dulu salah tipe entitas (menampung hasil query
	 * {@code PembayaranGajiPunyaPegawai} sebagai {@code PembayaranItemGajiPegawai}) dan selalu
	 * gagal ClassCastException. Riwayat batch diberi {@code posting=true}.</p>
	 */
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<?> daftar = kriteriaPenggajianStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory")).list();
			if (daftar.isEmpty()) {
				return 0;
			}

			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_PENGGAJIAN);
			postingHistory.setTbmuser(oleh);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTanggalPosting(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setPosting(true);
			postingHistory.setKeterangan("Posting massal penggajian pegawai dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			for (Object o : daftar) {
				PembayaranGajiPunyaPegawai pg = (PembayaranGajiPunyaPegawai) o;
				if (pg == null || pg.getPegawai() == null || pg.getPembayaranGaji() == null) {
					continue;
				}
				try {
					List<?> items = session.createCriteria(PembayaranItemGajiPegawai.class)
							.add(Restrictions.gt("nilai", 0.1))
							.add(Restrictions.or(Restrictions.isNotNull("akun"),
									Restrictions.isNotNull("akunDebet")))
							.add(Restrictions.eq("pembayaranGajiPunyaPegawai", pg)).list();
					List<Akun> akunDebets = new ArrayList<Akun>();
					List<Akun> akunKredits = new ArrayList<Akun>();
					List<Double> nilaiDebets = new ArrayList<Double>();
					List<Double> nilaiKredits = new ArrayList<Double>();
					Double nilaiDebet = 0.0;
					Double nilaiKredit = 0.0;
					for (Object oi : items) {
						PembayaranItemGajiPegawai item = (PembayaranItemGajiPegawai) oi;
						if (item.getAkun() != null) {
							akunKredits.add(item.getAkun());
							nilaiKredits.add(item.getNilai());
							nilaiKredit += item.getNilai();
						}
						if (item.getAkunDebet() != null) {
							akunDebets.add(item.getAkunDebet());
							nilaiDebets.add(item.getNilai());
							nilaiDebet += item.getNilai();
						}
					}
					Bank bank = pg.getPegawai().ambilBank(pg.getFormatItemGaji());
					if (bank != null && bank.getAkun() != null) {
						akunKredits.add(bank.getAkun());
						nilaiKredits.add(pg.getNilai());
						nilaiKredit += pg.getNilai();
					} else if (pg.getPembayaranGaji().getCaraPembayaranGaji() != null
							&& pg.getPembayaranGaji().getCaraPembayaranGaji().getAkun() != null) {
						akunKredits.add(pg.getPembayaranGaji().getCaraPembayaranGaji().getAkun());
						nilaiKredits.add(pg.getNilai());
						nilaiKredit += pg.getNilai();
					}
					if (akunDebets.isEmpty() || akunKredits.isEmpty()
							|| nilaiDebet.intValue() != nilaiKredit.intValue()) {
						continue;
					}

					String ket = "Pembayaran gaji " + pg.getPegawai().getNama() + " bulan "
							+ pg.getPembayaranGaji().getBulan() + " tahun "
							+ pg.getPembayaranGaji().getTahun();

					boolean tersimpan;
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					tersimpan = CommonAkunting.saveTransaksi(akunDebets.toArray(new Akun[] {}),
							akunKredits.toArray(new Akun[] {}), null, null, postingHistory, true, ket,
							pg.getTanggalBayar(), nilaiDebets.toArray(new Double[] {}),
							nilaiKredits.toArray(new Double[] {}), 0.0, pg,
							pg.getPegawai().getSatuanKerja(), session);
					if (tersimpan) {
						pg.setPostingHistory(postingHistory);
						session.update(pg);
						session.getTransaction().commit();
						n++;
					} else {
						session.getTransaction().rollback();
					}
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingTransaksiPenggajianAction jalur API");
				}
			}

			if (n == 0) {
				// Tidak satu dokumen pun terjurnal: riwayat kosong tidak ditinggalkan.
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.delete(postingHistory);
					session.getTransaction().commit();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PostingTransaksiPenggajianAction jalur API");
				}
			}
		} finally {
			try {
				session.disconnect();
				HibernateUtil.closeSession();
			} catch (Exception e) {
				// penutupan sesi manual: kegagalannya tidak menutupi hasil posting
			}
		}
		return n;
	}

	/**
	 * Membatalkan posting SEMUA penggajian terposting pada rentang: jurnal turunannya dihapus
	 * (baris transaksi dulu, lalu grupnya -- hanya yang belum closing), lalu penandanya dilepas.
	 */
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<?> daftar = kriteriaPenggajianStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (Object o : daftar) {
				PembayaranGajiPunyaPegawai pg = (PembayaranGajiPunyaPegawai) o;
				if (pg == null) {
					continue;
				}
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where pembayaran_gaji_punya_pegawai="
							+ pg.getId() + " and closing is null)").executeUpdate();
					session.createSQLQuery(
							"delete from akunting.grup_transaksi where pembayaran_gaji_punya_pegawai="
									+ pg.getId() + " and closing is null").executeUpdate();
					pg.setPostingHistory(null);
					session.update(pg);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingTransaksiPenggajianAction jalur API");
				}
			}
		} finally {
			try {
				session.disconnect();
				HibernateUtil.closeSession();
			} catch (Exception e) {
				// penutupan sesi manual: kegagalannya tidak menutupi hasil pembatalan
			}
		}
		return n;
	}

}
