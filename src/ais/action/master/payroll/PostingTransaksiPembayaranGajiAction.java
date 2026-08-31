package ais.action.master.payroll;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
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
import org.zkoss.zul.North;
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
import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
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
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk posting transaksi pembayaran gaji. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Combobox bulan},
 * {@code Intbox tahun}, {@code Combobox caraBayar}, {@code boolean approve}, {@code boolean delete}, {@code
 * Paging paging}, {@code MyToolbarbuttonConfig sent}; inisialisasi/lifecycle ({@code doAfterCompose()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefaultTanpaProgress()}, {@code onSearchDefault()},
 * {@code loadDataDenganProgressPosting()}); mutasi data ({@code onBatalkanPostingSemua()}, {@code
 * onPostingSemua()}, {@code kriteriaPostingStatic()}, {@code batalkanPostingSemua()}, {@code postingSemua()});
 * operasi domain lain ({@code tambah()}, {@code isiAkun()}). Bagian lain dari kontrak tetap mengikuti kelas
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
public class PostingTransaksiPembayaranGajiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyGrid grid;

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
	private MyCheckboxConfig rinci;
	
	private North filter;
	private Row rowPosting;
	private Boolean sudah_posting = null;

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
		if (execution.getParameter("sudah_posting") != null
				&& !execution.getParameter("sudah_posting").trim().isEmpty()) {
			sudah_posting = Boolean.parseBoolean(execution.getParameter("sudah_posting").trim());
		}

		if (sudah_posting != null && filter != null) {
			filter.setVisible(false);
			if (rowPosting != null) rowPosting.setVisible(true);
		}
		
		
		
		adminLain = Common.getApakahAdmin() || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);

		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (sent == null) return;
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

//		tahun.setValue(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));

		SatuanKerja satuanKerja = Common.getSatuanKerja();

		Common.insertComboDanSemua(caraBayar, "nama", "akun", CaraPembayaranGaji.class,
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.and(Restrictions.isNotNull("akun"),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

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
		
		
		if (execution.getParameter("mulai") != null) {
			try {
				mulai.setValue(Common.dateFormat8.get().parse(execution.getParameter("mulai")));
				mulai.setDisabled(true);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		if (execution.getParameter("sampai") != null) {
			try {
				sampai.setValue(Common.dateFormat8.get().parse(execution.getParameter("sampai")));
				sampai.setDisabled(true);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		rinci = new MyCheckboxConfig("Rinci");
		Common.appendKeToolbar(rinci, sent, comp);
		rinci.addEventListener("onClick", new EventListener() {

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
	 * Renderer lokal untuk layar/komponen {@link PostingTransaksiPembayaranGajiAction}. Kelas ini menerjemahkan
	 * satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PostingTransaksiPembayaranGajiAction} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PostingTransaksiPembayaranGajiAction
	 */
	class TransaksiRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PembayaranGaji pembayaranGaji = (PembayaranGaji) arg1;
			arg0.setAttribute("pembayaranGaji", pembayaranGaji);

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(PembayaranGajiPunyaPegawai.class, pembayaranGaji,
					pembayaranGaji.getSatuanKerja() == null ? "" : pembayaranGaji.getSatuanKerja().getNama()))
					.setParent(arg0);

			Set<Long> siss = new HashSet<Long>();
			// StandingInstruction / prosesStanding bisa null pada data lama -> jangan di-deref langsung.
			String prosesStandingStr = pembayaranGaji.getStandingInstruction() == null ? null
					: pembayaranGaji.getStandingInstruction().getProsesStanding();
			for (String ss : (prosesStandingStr == null ? new String[0] : prosesStandingStr.split(","))) {

				try {
					if (!ss.trim().isEmpty()) {
						siss.add(Long.parseLong(ss));
					}
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

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

			new Label(pembayaranGaji.getWaktuBayar() == null ? ""
					: Common.dateFormat6.get().format(pembayaranGaji.getWaktuBayar())).setParent(arg0);
			new Label(pembayaranGaji.getBulan() + "").setParent(arg0);
			new Label(pembayaranGaji.getTahun() + "").setParent(arg0);

			new Label(pembayaranGaji.getCaraPembayaranGaji() == null ? ""
					: pembayaranGaji.getCaraPembayaranGaji().getNama()).setParent(arg0);

			List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = session
					.createCriteria(PembayaranGajiPunyaPegawai.class)
					.add(Restrictions.eq("pembayaranGaji", pembayaranGaji)).list();

			List<Akun> akunDebet = new ArrayList<Akun>();
			List<Akun> akunKredit = new ArrayList<Akun>();

			List<Double> nilaiDebets = new ArrayList<Double>();
			List<Double> nilaiKredits = new ArrayList<Double>();

			if (rinci != null && rinci.isChecked()) {

				for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {

					List<PembayaranItemGajiPegawai> pembayaranItemGajiPegawais = ConstantValues.simpleList(
							session.createCriteria(PembayaranItemGajiPegawai.class).add(Restrictions.gt("nilai", 0.1))
									.add(Restrictions.or(Restrictions.isNotNull("akun"),
											Restrictions.isNotNull("akunDebet")))
									.add(Restrictions.eq("pembayaranGajiPunyaPegawai", pembayaranGajiPunyaPegawai)),
							PembayaranItemGajiPegawai.class);

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
					} else if (pembayaranGajiPunyaPegawai.getPembayaranGaji().getCaraPembayaranGaji() != null
							&& pembayaranGajiPunyaPegawai.getPembayaranGaji().getCaraPembayaranGaji()
									.getAkun() != null) {

						akunKredit
								.add(pembayaranGajiPunyaPegawai.getPembayaranGaji().getCaraPembayaranGaji().getAkun());
						nilaiKredits.add(pembayaranGajiPunyaPegawai.getNilai());

					}

				}

				if (!akunKredit.isEmpty() && !akunDebet.isEmpty()) {
					GrupTransaksi.tampilkanJurnal(akunDebet, nilaiDebets, akunKredit, nilaiKredits).setParent(arg0);
				} else {
					new Label("Transaksi tidak valid."
							+ (!akunKredit.isEmpty() ? " Debet: " + akunKredit + "." : " Akun debet tidak ada.")
							+ (!akunDebet.isEmpty() ? " Kredit: " + akunDebet + "." : " Akun kredit tidak ada."))
							.setParent(arg0);
				}
			} else {

				Map<Long, Double> akunsDebetsMap = new HashMap<Long, Double>();
				Map<Long, Double> akunsKreditsMap = new HashMap<Long, Double>();

				for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {

					List<PembayaranItemGajiPegawai> pembayaranItemGajiPegawais = ConstantValues.simpleList(
							session.createCriteria(PembayaranItemGajiPegawai.class).add(Restrictions.gt("nilai", 0.1))
									.add(Restrictions.or(Restrictions.isNotNull("akun"),
											Restrictions.isNotNull("akunDebet")))
									.add(Restrictions.eq("pembayaranGajiPunyaPegawai", pembayaranGajiPunyaPegawai)),
							PembayaranItemGajiPegawai.class);

					for (PembayaranItemGajiPegawai pembayaranItemGajiPegawai : pembayaranItemGajiPegawais) {
						if (pembayaranItemGajiPegawai.getAkun() != null) {

							Double n = akunsKreditsMap.get(pembayaranItemGajiPegawai.getAkun().getId());
							if (n == null) {
								n = 0.0;
							}
							n += pembayaranItemGajiPegawai.getNilai();

							akunsKreditsMap.put(pembayaranItemGajiPegawai.getAkun().getId(), n);

						}
						if (pembayaranItemGajiPegawai.getAkunDebet() != null) {

							Double n = akunsDebetsMap.get(pembayaranItemGajiPegawai.getAkunDebet().getId());
							if (n == null) {
								n = 0.0;
							}
							n += pembayaranItemGajiPegawai.getNilai();

							akunsDebetsMap.put(pembayaranItemGajiPegawai.getAkunDebet().getId(), n);

						}
					}

				}

				JSONObject jsonObjectTransfer = new JSONObject(
						pembayaranGaji.getStandingInstruction().getTransferVia());
				Iterator<String> iterator = jsonObjectTransfer.keys();

				while (iterator.hasNext()) {
					String d = iterator.next();
					Long idBank = Long.parseLong(d);
					Bank bank = (Bank) ConstantValues.ambil(Bank.class.getName(), idBank);

					JSONObject jsonObjectData = null;

					try {
						jsonObjectData = jsonObjectTransfer.isNull(idBank.toString()) ? null
								: jsonObjectTransfer.getJSONObject(idBank.toString());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/PostingTransaksiPembayaranGajiAction.java:414");
						// TODO: handle exception
					}

					Long idS = jsonObjectData == null || jsonObjectData.isNull("si")
							|| jsonObjectData.get("si").toString().trim().isEmpty() ? null
									: Long.parseLong(jsonObjectData.get("si").toString().trim());

					if (idS != null && bank != null && bank.getAkun() != null) {
						ProsesTransferStandingInstruction prosesTransferStandingInstruction = (ProsesTransferStandingInstruction) session
								.createCriteria(ProsesTransferStandingInstruction.class).add(Restrictions.idEq(idS))
								.uniqueResult();
						if (prosesTransferStandingInstruction != null
								&& prosesTransferStandingInstruction.getRealisasikanOleh() != null) {

							Double nilai = jsonObjectData.isNull("nilai") ? 0.0 : jsonObjectData.getDouble("nilai");

							Double n = akunsKreditsMap.get(bank.getAkun().getId());
							if (n == null) {
								n = 0.0;
							}
							n += nilai;

							akunsKreditsMap.put(bank.getAkun().getId(), n);

							System.out.println(
									"bank -> " + bank.getAkun().getNama() + ", n -> " + Common.numberFormat.get().format(n));
						}
					}
				}

				if (!akunsDebetsMap.isEmpty() && !akunsKreditsMap.isEmpty()) {

					for (Long key : akunsDebetsMap.keySet()) {
						Akun akun = (Akun) ConstantValues.ambil(Akun.class.getName(), key);
						if (akun != null) {
							akunDebet.add(akun);
							nilaiDebets.add(akunsDebetsMap.get(key));
						}
					}

					for (Long key : akunsKreditsMap.keySet()) {
						Akun akun = (Akun) ConstantValues.ambil(Akun.class.getName(), key);
						if (akun != null) {
							akunKredit.add(akun);
							nilaiKredits.add(akunsKreditsMap.get(key));
						}
					}

					GrupTransaksi.tampilkanJurnal(akunDebet, nilaiDebets, akunKredit, nilaiKredits).setParent(arg0);
				} else {
					new Label("Transaksi tidak valid."
							+ (!akunsDebetsMap.isEmpty() ? " Debet: " + akunsDebetsMap + "." : " Akun debet tidak ada.")
							+ (!akunsKreditsMap.isEmpty() ? " Kredit: " + akunsKreditsMap + "."
									: " Akun kredit tidak ada."))
							.setParent(arg0);
				}

			}

			new Label(pembayaranGaji.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: pembayaranGaji.getPostingHistory().toString()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			button.setTooltiptext("Batalkan Posting Data");
			button.setVisible(adminLain && pembayaranGaji.getPostingHistory() != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pembayaranGaji.setPostingHistory(null);
							Common.refreshSaveOrUpdate(pembayaranGaji);
							HibernateUtil.currentSession()
									.createSQLQuery("delete from akunting.grup_transaksi where pembayaran_gaji="
											+ pembayaranGaji.getId() + " and closing is null")
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

			if (!akunKredit.isEmpty() && !akunDebet.isEmpty()) {
				button = new MyToolbarbuttonConfig("", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Posting Data");
				button.setVisible(pembayaranGaji.getPostingHistory() == null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentNativeSession();

								session.refresh(pembayaranGaji);

								PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_PENGGAJIAN);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan("Posting manual oleh " + tbmuser.getUserNama()
										+ " pada waktu " + Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.getTransaction().begin();
								session.save(postingHistory);
								session.getTransaction().commit();

								List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = session
										.createCriteria(PembayaranGajiPunyaPegawai.class)
										.add(Restrictions.eq("pembayaranGaji", pembayaranGaji)).list();

								List<Akun> akunDebet = new ArrayList<Akun>();
								List<Akun> akunKredit = new ArrayList<Akun>();

								List<Double> nilaiDebets = new ArrayList<Double>();
								List<Double> nilaiKredits = new ArrayList<Double>();

								if (rinci != null && rinci.isChecked()) {

									for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {

										List<PembayaranItemGajiPegawai> pembayaranItemGajiPegawais = ConstantValues
												.simpleList(
														session.createCriteria(PembayaranItemGajiPegawai.class)
																.add(Restrictions.gt("nilai", 0.1))
																.add(Restrictions.or(Restrictions.isNotNull("akun"),
																		Restrictions.isNotNull("akunDebet")))
																.add(Restrictions.eq("pembayaranGajiPunyaPegawai",
																		pembayaranGajiPunyaPegawai)),
														PembayaranItemGajiPegawai.class);

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
												&& pembayaranGajiPunyaPegawai.getPembayaranGaji()
														.getCaraPembayaranGaji().getAkun() != null) {

											akunKredit.add(pembayaranGajiPunyaPegawai.getPembayaranGaji()
													.getCaraPembayaranGaji().getAkun());
											nilaiKredits.add(pembayaranGajiPunyaPegawai.getNilai());

										}

									}

									String ket = "Pembayaran gaji "
											+ (pembayaranGaji.getSatuanKerja() == null ? ""
													: pembayaranGaji.getSatuanKerja().getNama())
											+ " bulan " + pembayaranGaji.getBulan() + " tahun "
											+ pembayaranGaji.getTahun();

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;
									Boolean apakahUangMasuk = true;

									session.getTransaction().begin();
									CommonAkunting.saveTransaksi(akunDebet.toArray(new Akun[] {}),
											akunKredit.toArray(new Akun[] {}), akunDenda, akunPiutangDenda,
											postingHistory, apakahUangMasuk, ket, pembayaranGaji.getWaktuBayar(),
											nilaiDebets.toArray(new Double[] {}), nilaiKredits.toArray(new Double[] {}),
											denda, pembayaranGaji, pembayaranGaji.getSatuanKerja(), session);
									session.getTransaction().commit();

									pembayaranGaji.setPostingHistory(postingHistory);
									session.getTransaction().begin();
									Common.refreshUpdate(session, pembayaranGaji);
									session.getTransaction().commit();

									// session.disconnect();
									if (session.isOpen()) {session.disconnect();session.close();}
									HibernateUtil.closeSession();
								} else {
									Map<Long, Double> akunsDebetsMap = new HashMap<Long, Double>();
									Map<Long, Double> akunsKreditsMap = new HashMap<Long, Double>();

									for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {

										List<PembayaranItemGajiPegawai> pembayaranItemGajiPegawais = ConstantValues
												.simpleList(
														session.createCriteria(PembayaranItemGajiPegawai.class)
																.add(Restrictions.gt("nilai", 0.1))
																.add(Restrictions.or(Restrictions.isNotNull("akun"),
																		Restrictions.isNotNull("akunDebet")))
																.add(Restrictions.eq("pembayaranGajiPunyaPegawai",
																		pembayaranGajiPunyaPegawai)),
														PembayaranItemGajiPegawai.class);

										for (PembayaranItemGajiPegawai pembayaranItemGajiPegawai : pembayaranItemGajiPegawais) {
											if (pembayaranItemGajiPegawai.getAkun() != null) {

												Double n = akunsKreditsMap
														.get(pembayaranItemGajiPegawai.getAkun().getId());
												if (n == null) {
													n = 0.0;
												}
												n += pembayaranItemGajiPegawai.getNilai();

												akunsKreditsMap.put(pembayaranItemGajiPegawai.getAkun().getId(), n);

											}
											if (pembayaranItemGajiPegawai.getAkunDebet() != null) {

												Double n = akunsDebetsMap
														.get(pembayaranItemGajiPegawai.getAkunDebet().getId());
												if (n == null) {
													n = 0.0;
												}
												n += pembayaranItemGajiPegawai.getNilai();

												akunsDebetsMap.put(pembayaranItemGajiPegawai.getAkunDebet().getId(), n);

											}
										}

										Pegawai pegawai = pembayaranGajiPunyaPegawai.getPegawai();
										Bank bank = pegawai.ambilBank(pembayaranGajiPunyaPegawai.getFormatItemGaji());

										if (bank != null && bank.getAkun() != null) {

											Double n = akunsKreditsMap.get(bank.getAkun().getId());
											if (n == null) {
												n = 0.0;
											}
											n += pembayaranGajiPunyaPegawai.getNilai();

											akunsKreditsMap.put(bank.getAkun().getId(), n);

										} else if (pembayaranGajiPunyaPegawai.getPembayaranGaji()
												.getCaraPembayaranGaji() != null
												&& pembayaranGajiPunyaPegawai.getPembayaranGaji()
														.getCaraPembayaranGaji().getAkun() != null) {

											Double n = akunsKreditsMap.get(pembayaranGajiPunyaPegawai
													.getPembayaranGaji().getCaraPembayaranGaji().getAkun().getId());
											if (n == null) {
												n = 0.0;
											}
											n += pembayaranGajiPunyaPegawai.getNilai();

											akunsKreditsMap.put(pembayaranGajiPunyaPegawai.getPembayaranGaji()
													.getCaraPembayaranGaji().getAkun().getId(), n);

										}

									}

									for (Long key : akunsDebetsMap.keySet()) {
										Akun akun = (Akun) ConstantValues.ambil(Akun.class.getName(), key);
										if (akun != null) {
											akunDebet.add(akun);
											nilaiDebets.add(akunsDebetsMap.get(key));
										}
									}

									for (Long key : akunsKreditsMap.keySet()) {
										Akun akun = (Akun) ConstantValues.ambil(Akun.class.getName(), key);
										if (akun != null) {
											akunKredit.add(akun);
											nilaiKredits.add(akunsKreditsMap.get(key));
										}
									}

									String ket = "Pembayaran gaji "
											+ (pembayaranGaji.getSatuanKerja() == null ? ""
													: pembayaranGaji.getSatuanKerja().getNama())
											+ " bulan " + pembayaranGaji.getBulan() + " tahun "
											+ pembayaranGaji.getTahun();

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;
									Boolean apakahUangMasuk = true;

									session.getTransaction().begin();
									CommonAkunting.saveTransaksi(akunDebet.toArray(new Akun[] {}),
											akunKredit.toArray(new Akun[] {}), akunDenda, akunPiutangDenda,
											postingHistory, apakahUangMasuk, ket, pembayaranGaji.getWaktuBayar(),
											nilaiDebets.toArray(new Double[] {}), nilaiKredits.toArray(new Double[] {}),
											denda, pembayaranGaji, pembayaranGaji.getSatuanKerja(), session);
									session.getTransaction().commit();

									pembayaranGaji.setPostingHistory(postingHistory);
									session.getTransaction().begin();
									Common.refreshUpdate(session, pembayaranGaji);
									session.getTransaction().commit();

									// session.disconnect();
									if (session.isOpen()) {session.disconnect();session.close();}
									HibernateUtil.closeSession();
								}

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
							List<PembayaranGaji> pembayaranGajis = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (PembayaranGaji pembayaranGaji : pembayaranGajis) {
								pembayaranGaji.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pembayaranGaji);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where pembayaran_gaji="
												+ pembayaranGaji.getId() + " and closing is null")
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
		center.setTitle("Posting semua penggajian");
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
											MyMessageboxConfig.show("Alhamdulillah, posting transaksi penggajian telah berhasil dilakukan.",
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

											List<PembayaranGaji> pembayaranGajis = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											PostingHistory postingHistory = new PostingHistory(
													PostingHistory.JENIS_PENGGAJIAN);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setTanggal(tgl);
											postingHistory.setKeterangan(keterangan.getValue().trim());

											Session session = HibernateUtil.currentNativeSession();
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											for (PembayaranGaji pembayaranGaji : pembayaranGajis) {

												List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = session
														.createCriteria(PembayaranGajiPunyaPegawai.class)
														.add(Restrictions.eq("pembayaranGaji", pembayaranGaji)).list();

												if (rinci != null && rinci.isChecked()) {

													List<Akun> akunDebet = new ArrayList<Akun>();
													List<Akun> akunKredit = new ArrayList<Akun>();

													List<Double> nilaiDebets = new ArrayList<Double>();
													List<Double> nilaiKredits = new ArrayList<Double>();
													for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {

														List<PembayaranItemGajiPegawai> pembayaranItemGajiPegawais = ConstantValues
																.simpleList(session
																		.createCriteria(PembayaranItemGajiPegawai.class)
																		.add(Restrictions.gt("nilai", 0.1))
																		.add(Restrictions.or(
																				Restrictions.isNotNull("akun"),
																				Restrictions.isNotNull("akunDebet")))
																		.add(Restrictions.eq(
																				"pembayaranGajiPunyaPegawai",
																				pembayaranGajiPunyaPegawai)),
																		PembayaranItemGajiPegawai.class);

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
														Bank bank = pegawai.ambilBank(
																pembayaranGajiPunyaPegawai.getFormatItemGaji());

														if (bank != null && bank.getAkun() != null) {
															akunKredit.add(bank.getAkun());
															nilaiKredits.add(pembayaranGajiPunyaPegawai.getNilai());
														} else if (pembayaranGajiPunyaPegawai.getPembayaranGaji()
																.getCaraPembayaranGaji() != null
																&& pembayaranGajiPunyaPegawai.getPembayaranGaji()
																		.getCaraPembayaranGaji().getAkun() != null) {

															akunKredit
																	.add(pembayaranGajiPunyaPegawai.getPembayaranGaji()
																			.getCaraPembayaranGaji().getAkun());
															nilaiKredits.add(pembayaranGajiPunyaPegawai.getNilai());

														}

													}

													String ket = "Pembayaran gaji "
															+ (pembayaranGaji.getSatuanKerja() == null ? ""
																	: pembayaranGaji.getSatuanKerja().getNama())
															+ " bulan " + pembayaranGaji.getBulan() + " tahun "
															+ pembayaranGaji.getTahun();

													Akun akunDenda = null;
													Akun akunPiutangDenda = null;
													Double denda = 0.0;
													Boolean apakahUangMasuk = true;

													try {
														session.getTransaction().begin();
														CommonAkunting.saveTransaksi(akunDebet.toArray(new Akun[] {}),
																akunKredit.toArray(new Akun[] {}), akunDenda,
																akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
																pembayaranGaji.getWaktuBayar(),
																nilaiDebets.toArray(new Double[] {}),
																nilaiKredits.toArray(new Double[] {}), denda,
																pembayaranGaji, pembayaranGaji.getSatuanKerja(),
																session);
														session.getTransaction().commit();
													} catch (Exception e) {
														// TODO Auto-generated catch block
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
												} else {

													Map<Long, Double> akunsDebetsMap = new HashMap<Long, Double>();
													Map<Long, Double> akunsKreditsMap = new HashMap<Long, Double>();

													for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {

														List<PembayaranItemGajiPegawai> pembayaranItemGajiPegawais = ConstantValues
																.simpleList(session
																		.createCriteria(PembayaranItemGajiPegawai.class)
																		.add(Restrictions.gt("nilai", 0.1))
																		.add(Restrictions.or(
																				Restrictions.isNotNull("akun"),
																				Restrictions.isNotNull("akunDebet")))
																		.add(Restrictions.eq(
																				"pembayaranGajiPunyaPegawai",
																				pembayaranGajiPunyaPegawai)),
																		PembayaranItemGajiPegawai.class);

														for (PembayaranItemGajiPegawai pembayaranItemGajiPegawai : pembayaranItemGajiPegawais) {
															if (pembayaranItemGajiPegawai.getAkun() != null) {

																Double n = akunsKreditsMap.get(
																		pembayaranItemGajiPegawai.getAkun().getId());
																if (n == null) {
																	n = 0.0;
																}
																n += pembayaranItemGajiPegawai.getNilai();

																akunsKreditsMap.put(
																		pembayaranItemGajiPegawai.getAkun().getId(), n);

															}
															if (pembayaranItemGajiPegawai.getAkunDebet() != null) {

																Double n = akunsDebetsMap.get(pembayaranItemGajiPegawai
																		.getAkunDebet().getId());
																if (n == null) {
																	n = 0.0;
																}
																n += pembayaranItemGajiPegawai.getNilai();

																akunsDebetsMap.put(pembayaranItemGajiPegawai
																		.getAkunDebet().getId(), n);

															}
														}

														Pegawai pegawai = pembayaranGajiPunyaPegawai.getPegawai();
														Bank bank = pegawai.ambilBank(
																pembayaranGajiPunyaPegawai.getFormatItemGaji());

														if (bank != null && bank.getAkun() != null) {

															Double n = akunsKreditsMap.get(bank.getAkun().getId());
															if (n == null) {
																n = 0.0;
															}
															n += pembayaranGajiPunyaPegawai.getNilai();

															akunsKreditsMap.put(bank.getAkun().getId(), n);

														} else if (pembayaranGajiPunyaPegawai.getPembayaranGaji()
																.getCaraPembayaranGaji() != null
																&& pembayaranGajiPunyaPegawai.getPembayaranGaji()
																		.getCaraPembayaranGaji().getAkun() != null) {

															Double n = akunsKreditsMap
																	.get(pembayaranGajiPunyaPegawai.getPembayaranGaji()
																			.getCaraPembayaranGaji().getAkun().getId());
															if (n == null) {
																n = 0.0;
															}
															n += pembayaranGajiPunyaPegawai.getNilai();

															akunsKreditsMap.put(
																	pembayaranGajiPunyaPegawai.getPembayaranGaji()
																			.getCaraPembayaranGaji().getAkun().getId(),
																	n);

														}

													}

													List<Akun> akunDebet = new ArrayList<Akun>();
													List<Akun> akunKredit = new ArrayList<Akun>();

													List<Double> nilaiDebets = new ArrayList<Double>();
													List<Double> nilaiKredits = new ArrayList<Double>();

													for (Long key : akunsDebetsMap.keySet()) {
														Akun akun = (Akun) ConstantValues.ambil(Akun.class.getName(),
																key);
														if (akun != null) {
															akunDebet.add(akun);
															nilaiDebets.add(akunsDebetsMap.get(key));
														}
													}

													for (Long key : akunsKreditsMap.keySet()) {
														Akun akun = (Akun) ConstantValues.ambil(Akun.class.getName(),
																key);
														if (akun != null) {
															akunKredit.add(akun);
															nilaiKredits.add(akunsKreditsMap.get(key));
														}
													}

													String ket = "Pembayaran gaji "
															+ (pembayaranGaji.getSatuanKerja() == null ? ""
																	: pembayaranGaji.getSatuanKerja().getNama())
															+ " bulan " + pembayaranGaji.getBulan() + " tahun "
															+ pembayaranGaji.getTahun();

													Akun akunDenda = null;
													Akun akunPiutangDenda = null;
													Double denda = 0.0;
													Boolean apakahUangMasuk = true;

													try {
														session.getTransaction().begin();
														CommonAkunting.saveTransaksi(akunDebet.toArray(new Akun[] {}),
																akunKredit.toArray(new Akun[] {}), akunDenda,
																akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
																pembayaranGaji.getWaktuBayar(),
																nilaiDebets.toArray(new Double[] {}),
																nilaiKredits.toArray(new Double[] {}), denda,
																pembayaranGaji, pembayaranGaji.getSatuanKerja(),
																session);
														session.getTransaction().commit();
													} catch (Exception e) {
														// TODO Auto-generated catch block
														ais.common.Common.tampilErrorJikaAdmin(e);
													}

												}
											}

											// session.disconnect();
											if (session.isOpen()) {session.disconnect();session.close();}

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
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PembayaranGaji.class);
		
		
		if (sudah_posting != null) {

			try {

				

				if (sudah_posting) {
					criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN)
							.add(Restrictions.eq("postingHistory.posting", true));
				} else {
					criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN)
							.add(Restrictions.or(Restrictions.isNull("postingHistory.id"),
									Restrictions.eq("postingHistory.posting", false)));
				}

			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

		} 
		

		criteria.add(Restrictions.isNotNull("standingInstruction")).add(Restrictions.isNotNull("disetujuiOleh"))

				.add((mulai == null || sampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(waktubayar) between date('" + Common.databaseDateFormat.get().format(mulai.getValue())
								+ "') and  date('" + Common.databaseDateFormat.get().format(sampai.getValue()) + "')")))

				.add(bulan.getSelectedItem() == null || bulan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("bulan", bulan.getSelectedItem().getValue()))

				.add(caraBayar.getSelectedItem() == null || caraBayar.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("caraPembayaranGaji", caraBayar.getSelectedItem().getValue()))

				.add(tahun.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahun", tahun.getValue()));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PembayaranGaji> pembayaranGaji = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
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


	// =====================================================================
	// JALUR NON-ZK (dasbor Draft Jurnal lewat API POS)
	// PEMELIHARAAN: akun & nilai HARUS tetap identik dengan {@link #onPostingSemua}
	// cabang RINGKAS (checkbox "rinci" tidak dicentang).
	// =====================================================================

	/**
	 * Kriteria pembayaran gaji yang layak dijurnal -- sama dengan penghitung baris "Gaji" pada
	 * dasbor: sudah punya standing instruction, sudah disetujui, dan waktu bayarnya di dalam
	 * rentang.
	 */
	private static Criteria kriteriaPostingStatic(Session session, java.util.Date mulai,
			java.util.Date sampai) {
		Criteria c = session.createCriteria(PembayaranGaji.class)
				.add(Restrictions.isNotNull("standingInstruction"))
				.add(Restrictions.isNotNull("disetujuiOleh"));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(this_.waktubayar) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/** Batalkan posting SEMUA pembayaran gaji terposting dalam rentang. */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<PembayaranGaji> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (PembayaranGaji gaji : daftar) {
				try {
					String syarat = "pembayaran_gaji=" + gaji.getId() + " and closing is null";
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where " + syarat + ")").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where " + syarat)
							.executeUpdate();
					gaji.setPostingHistory(null);
					session.update(gaji);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingTransaksiPembayaranGajiAction jalur API");
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

	/**
	 * Posting SEMUA pembayaran gaji yang belum dijurnal dalam rentang.
	 *
	 * <p><b>Bentuk jurnalnya RINGKAS.</b> Layar menyediakan centang "rinci" yang menentukan
	 * satu baris jurnal per item gaji per pegawai, atau satu baris per AKUN dengan nilai yang
	 * sudah dijumlahkan. Dari API tidak ada centang itu, dan yang dipakai adalah bentuk
	 * RINGKAS: satu pembayaran gaji satu bulan bisa memuat ribuan item, dan jurnal sepanjang
	 * itu tidak terbaca di buku besar. Nilai totalnya sama persis.</p>
	 *
	 * <ul>
	 *   <li>kredit: akun tiap {@code PembayaranItemGajiPegawai} yang bernilai &gt; 0,1,
	 *       dijumlahkan per akun;</li>
	 *   <li>debet: akun debet tiap item, dijumlahkan per akun;</li>
	 *   <li>kredit tambahan: akun BANK pegawai bila ada -- selain itu akun cara pembayaran gaji;
	 *       itulah kas yang benar-benar keluar;</li>
	 *   <li>tanggal jurnal = {@code waktuBayar}, satuan kerja = milik dokumen gaji.</li>
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<Long> ids = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory"))
					.setProjection(org.hibernate.criterion.Projections.property("id")).list();

			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_PENGGAJIAN);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal pembayaran gaji dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			for (Long id : ids) {
				try {
					session = HibernateUtil.currentNativeSession();
					PembayaranGaji gaji = (PembayaranGaji) session.createCriteria(PembayaranGaji.class)
							.add(Restrictions.idEq(id)).uniqueResult();
					if (gaji == null) {
						continue;
					}
					List<PembayaranGajiPunyaPegawai> perPegawai = session
							.createCriteria(PembayaranGajiPunyaPegawai.class)
							.add(Restrictions.eq("pembayaranGaji", gaji)).list();

					java.util.Map<Long, Double> petaDebet = new java.util.HashMap<Long, Double>();
					java.util.Map<Long, Double> petaKredit = new java.util.HashMap<Long, Double>();
					for (PembayaranGajiPunyaPegawai baris : perPegawai) {
						List<PembayaranItemGajiPegawai> item = session
								.createCriteria(PembayaranItemGajiPegawai.class)
								.add(Restrictions.gt("nilai", 0.1))
								.add(Restrictions.or(Restrictions.isNotNull("akun"),
										Restrictions.isNotNull("akunDebet")))
								.add(Restrictions.eq("pembayaranGajiPunyaPegawai", baris)).list();
						for (PembayaranItemGajiPegawai it : item) {
							if (it.getAkun() != null) {
								tambah(petaKredit, it.getAkun().getId(), it.getNilai());
							}
							if (it.getAkunDebet() != null) {
								tambah(petaDebet, it.getAkunDebet().getId(), it.getNilai());
							}
						}
						Pegawai pegawai = baris.getPegawai();
						Bank bank = pegawai == null ? null : pegawai.ambilBank(baris.getFormatItemGaji());
						if (bank != null && bank.getAkun() != null) {
							tambah(petaKredit, bank.getAkun().getId(), baris.getNilai());
						} else if (gaji.getCaraPembayaranGaji() != null
								&& gaji.getCaraPembayaranGaji().getAkun() != null) {
							tambah(petaKredit, gaji.getCaraPembayaranGaji().getAkun().getId(),
									baris.getNilai());
						}
					}
					if (petaDebet.isEmpty() || petaKredit.isEmpty()) {
						// Akun item gajinya belum diatur: dilewati, bukan ditandai terposting.
						continue;
					}

					List<Akun> akunDebet = new java.util.ArrayList<Akun>();
					List<Double> nilaiDebet = new java.util.ArrayList<Double>();
					isiAkun(petaDebet, akunDebet, nilaiDebet);
					List<Akun> akunKredit = new java.util.ArrayList<Akun>();
					List<Double> nilaiKredit = new java.util.ArrayList<Double>();
					isiAkun(petaKredit, akunKredit, nilaiKredit);
					if (akunDebet.isEmpty() || akunKredit.isEmpty()) {
						continue;
					}

					String ket = "Pembayaran gaji "
							+ (gaji.getSatuanKerja() == null ? "" : gaji.getSatuanKerja().getNama())
							+ " bulan " + gaji.getBulan() + " tahun " + gaji.getTahun();

					boolean tersimpan = false;
					try {
						session.getTransaction().begin();
						CommonAkunting.saveTransaksi(akunDebet.toArray(new Akun[] {}),
								akunKredit.toArray(new Akun[] {}), null, null, postingHistory, true, ket,
								gaji.getWaktuBayar(), nilaiDebet.toArray(new Double[] {}),
								nilaiKredit.toArray(new Double[] {}), Double.valueOf(0.0), gaji,
								gaji.getSatuanKerja(), session);
						session.getTransaction().commit();
						tersimpan = true;
					} catch (Exception e) {
						try {
							session.getTransaction().rollback();
						} catch (Exception ex) {
							// rollback gagal: kegagalan aslinya yang dilaporkan
						}
						ais.common.ErrorAuditUtil.record(e,
								"PostingTransaksiPembayaranGajiAction jalur API");
					}

					if (tersimpan) {
						gaji.setPostingHistory(postingHistory);
						session.getTransaction().begin();
						session.update(gaji);
						session.getTransaction().commit();
						n++;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PostingTransaksiPembayaranGajiAction jalur API");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingTransaksiPembayaranGajiAction jalur API");
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

	/** Menjumlahkan nilai ke peta akun -> total. */
	private static void tambah(java.util.Map<Long, Double> peta, Long idAkun, Double nilai) {
		if (idAkun == null) {
			return;
		}
		Double n = peta.get(idAkun);
		peta.put(idAkun, (n == null ? 0.0 : n) + (nilai == null ? 0.0 : nilai));
	}

	/** Menerjemahkan peta akun -> total menjadi sepasang daftar akun & nilai. */
	private static void isiAkun(java.util.Map<Long, Double> peta, List<Akun> akunKe,
			List<Double> nilaiKe) {
		for (Long kunci : peta.keySet()) {
			Akun akun = (Akun) ConstantValues.ambil(Akun.class.getName(), kunci);
			if (akun != null) {
				akunKe.add(akun);
				nilaiKe.add(peta.get(kunci));
			}
		}
	}
}
