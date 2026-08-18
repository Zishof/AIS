package ais.ui.render;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.PembayaranNominalModifikasiHelper;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisDiskonMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jenjang;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.VOMahasiswa;
import ais.database.model.akunting.GrupTransaksi;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class DetailPembayaranMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

	private Kegiatan kegiatan;
	private Label labelFooterTagihan;
	private List<MyDoubleboxMin> pengurangan;
	private Map<Long, Double> dataTagihan;
	private EventListener eventListener;

	private Tbmuser tbmuser;
	private Grid gridCicilan;
	private Mahasiswa mahasiswa;
	private Integer semester;

	private Grid currentGrid;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private JadwalPembayaran jadwalPembayaran;
	private Date tanggalValidasi;
	private Collection<DetailKegiatan> detailKegiatans;
	private Label labelFooterDibayar;
	private Label labelFooterKekurangan;
	private Label terbilang;
	private Label terbilangTagihan;
	private Label terbilangSisa;
	private String tahunAkademik;
	private List<CicilanPembayaran> cicilanPembayarans;
	private Label terbilangSisaPersen;

	public List<Long> bul = new ArrayList<Long>();
	public List<Long> det = new ArrayList<Long>();

	public DetailPembayaranMahasiswaRenderer(Kegiatan kegiatan, JadwalPembayaran jadwalPembayaran,
			Label labelFooterTagihan, Label labelFooterDibayar, Label labelFooterKekurangan, Label terbilang,
			Label terbilangTagihan, Label terbilangSisa, Label terbilangSisaPersen, List<MyDoubleboxMin> pengurangan,
			EventListener eventListener, Grid gridCicilan, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Integer semester, String tahunAkademik,
			Map<Long, Double> dataTagihan, Grid currentGrid, Collection<DetailKegiatan> detailKegiatans,
			EventListener refrsh) {
		this.eventListener = eventListener;
		this.kegiatan = kegiatan;
		this.jadwalPembayaran = jadwalPembayaran;
		this.tanggalValidasi = new Date();
		this.labelFooterTagihan = labelFooterTagihan;
		this.labelFooterDibayar = labelFooterDibayar;
		this.labelFooterKekurangan = labelFooterKekurangan;

		this.terbilang = terbilang;
		this.terbilangTagihan = terbilangTagihan;
		this.terbilangSisa = terbilangSisa;
		this.terbilangSisaPersen = terbilangSisaPersen;

		this.pengurangan = pengurangan;
		this.gridCicilan = gridCicilan;
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		this.semester = semester;
		this.dataTagihan = dataTagihan;
		this.currentGrid = currentGrid;
		this.detailKegiatans = detailKegiatans;
		this.tahunAkademik = tahunAkademik;
		this.tbmuser = Common.getCurrentUser();
	}

	private static void executeNativeUpdateTransaction(GeneralValueObject entity) {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.currentNativeSession();
			tx = session.beginTransaction();
			Common.refreshUpdate(session, entity);
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:141");
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:144");
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.disconnect();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:149");
				}
				try {
					session.close();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:153");
				}
			}
			try {
				HibernateUtil.closeSession();
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:158");
			}
		}
	}

	private static void executeNativeSaveTransaction(GeneralValueObject entity) {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.currentNativeSession();
			tx = session.beginTransaction();
			session.save(entity);
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:175");
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:178");
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.disconnect();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:183");
				}
				try {
					session.close();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:187");
				}
			}
			try {
				HibernateUtil.closeSession();
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:192");
			}
		}
	}

	private static void executeNativeDeleteTransaction(GeneralValueObject entity) {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.currentNativeSession();
			tx = session.beginTransaction();
			Object merged = session.get(entity.getClass(), entity.getId());
			if (merged != null) {
				session.delete(merged);
			}
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:212");
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:215");
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.disconnect();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:220");
				}
				try {
					session.close();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:224");
				}
			}
			try {
				HibernateUtil.closeSession();
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:229");
			}
		}
	}

	private static boolean menggunakanDendaCustom(DetailKegiatan detailKegiatan) {
		return detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom();
	}

	private static Double nilaiDendaCustom(DetailKegiatan detailKegiatan) {
		return detailKegiatan == null ? 0.0 : detailKegiatan.getDendaCustom();
	}

	private Double ambilJumlahDetailBiayaTanpaDendaCustom(DetailKegiatan detailKegiatan, DetailBiaya detailBiaya,
			Double fallback) {
		if (!menggunakanDendaCustom(detailKegiatan)) {
			return fallback == null ? 0.0 : fallback;
		}
		Boolean menggunakanDendaCustom = detailKegiatan.getMenggunakanDendaCustom();
		Double dendaCustom = detailKegiatan.getDendaCustom();
		try {
			detailKegiatan.setMenggunakanDendaCustom(false);
			return Kegiatan.ambilJumlahTagihan(detailKegiatan, kegiatan, detailBiaya, false);
		} finally {
			detailKegiatan.setMenggunakanDendaCustom(menggunakanDendaCustom);
			detailKegiatan.setDendaCustom(dendaCustom);
		}
	}

	private Double ambilJumlahBulananTanpaDendaCustom(DetailKegiatan detailKegiatan, DetailBiaya detailBiaya,
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan, Double fallback) {
		if (!menggunakanDendaCustom(detailKegiatan)) {
			return fallback == null ? 0.0 : fallback;
		}
		Boolean menggunakanDendaCustom = detailKegiatan.getMenggunakanDendaCustom();
		Double dendaCustom = detailKegiatan.getDendaCustom();
		try {
			detailKegiatan.setMenggunakanDendaCustom(false);
			return Kegiatan.ambilJumlahTagihan(detailKegiatan, detailBiaya, kegiatan, mahasiswa, semester,
					pengaturanPembayaranBulanan);
		} finally {
			detailKegiatan.setMenggunakanDendaCustom(menggunakanDendaCustom);
			detailKegiatan.setDendaCustom(dendaCustom);
		}
	}

	private Double hitungTagihanDenganDendaCustom(Double pokok, DetailKegiatan detailKegiatan) {
		return (pokok == null ? 0.0 : pokok) + nilaiDendaCustom(detailKegiatan);
	}

	@SuppressWarnings({ "unchecked" })
	public List<PengaturanPembayaranBulanan> ubahWarnaStatus(List<CicilanPembayaran> cicilanPembayarans) {
		this.cicilanPembayarans = cicilanPembayarans;

		System.out.println("[TAGIHAN-DEBUG] ==> DetailPembayaranMahasiswaRenderer.ubahWarnaStatus jumlah cicilanPembayarans="
				+ (cicilanPembayarans == null ? 0 : cicilanPembayarans.size()) + " currentGrid="
				+ (currentGrid == null ? "null" : "ada, jumlah baris=" + currentGrid.getRows().getChildren().size()));

		Map<Long, PengaturanPembayaranBulanan> map = new HashMap<Long, PengaturanPembayaranBulanan>();
		if (currentGrid != null) {

			List<Row> rowsLocal = currentGrid.getRows().getChildren();
			for (Row row : rowsLocal) {
				DetailBiaya detailBiayaLogRow = (DetailBiaya) row.getAttribute("myValue");
				System.out.println("[TAGIHAN-DEBUG]   baris grid: myValue(detailBiaya)="
						+ (detailBiayaLogRow == null ? "null"
								: "id=" + detailBiayaLogRow.getId() + " itemBiaya="
										+ (detailBiayaLogRow.getItemBiaya() == null ? "null"
												: detailBiayaLogRow.getItemBiaya().getId() + "-" + detailBiayaLogRow.getItemBiaya().getNama())
										+ " bayarKe=" + detailBiayaLogRow.getBayarKe() + " semester=" + detailBiayaLogRow.getSemester())
						+ " | pengaturanPembayaranBulanan="
						+ (row.getAttribute("pengaturanPembayaranBulanan") == null ? "null"
								: ((PengaturanPembayaranBulanan) row.getAttribute("pengaturanPembayaranBulanan")).getId()));
				if (row.getAttribute("pengaturanPembayaranBulanan") != null) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) row
							.getAttribute("pengaturanPembayaranBulanan");
					map.put(pengaturanPembayaranBulanan.getId(), pengaturanPembayaranBulanan);
				}
			}

			List<Row> rows = currentGrid.getRows().getChildren();
			for (Row row : rows) {
				DetailBiaya detailBiaya = (DetailBiaya) row.getAttribute("myValue");

				for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
					if (cicilanPembayaran != null && cicilanPembayaran.getId() != null) {
						if (detailBiaya != null && cicilanPembayaran.getItemBiaya() != null && cicilanPembayaran
								.getItemBiaya().getId().equals(detailBiaya.getItemBiaya().getId())) {

							if (row.getAttribute("pengaturanPembayaranBulanan") == null) {
								Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
								if (jumlah.intValue() == cicilanPembayaran.getNilai().intValue()
										&& cicilanPembayaran.getDetailBiaya() == null) {
									cicilanPembayaran.setDetailBiaya(detailBiaya);
									executeNativeUpdateTransaction((GeneralValueObject) cicilanPembayaran);
								}
							}
						}
					}
				}
			}

			Map<String, Double> nilais = new HashMap<String, Double>();
			// PERBAIKAN "tagihan tampil dobel, masing-masing menampilkan total pembayaran yang
			// SAMA": nilais di atas dijumlah HANYA per (itemBiaya, bayarKe), mencakup SELURUH
			// pembayaran mahasiswa ini utk item tsb -- kalau ada >1 baris DetailBiaya dgn item+
			// bayarKe yg sama tampil sekaligus (mis. duplikat data lama, atau item yg sama muncul
			// di semester berbeda), SEMUA baris itu ikut menampilkan TOTAL YANG SAMA (seolah tiap
			// baris lunas sendiri-sendiri, padahal itu angka gabungan). Tambahkan peta KEDUA yang
			// menjumlah HANYA cicilan yang benar-benar ber-FK ke detail_biaya SPESIFIK baris ini --
			// dipakai duluan di titik penerapan (baris ~632) sebelum jatuh ke peta lama sbg fallback
			// utk cicilan lama yg belum sempat ber-FK detailBiaya.
			Map<Long, Double> nilaisPerDetailBiaya = new HashMap<Long, Double>();
			Map<Long, Double> nilaisPerItemBiaya = new HashMap<Long, Double>();
			Map<String, Integer> jumlahBarisTagihanPerKey = new HashMap<String, Integer>();
			Map<Long, Integer> jumlahBarisTagihanPerItem = new HashMap<Long, Integer>();
			if (currentGrid != null && currentGrid.getRows() != null) {
				List<Row> barisTagihan = currentGrid.getRows().getChildren();
				for (Row rowTagihan : barisTagihan) {
					DetailBiaya detailBiayaTagihan = (DetailBiaya) rowTagihan.getAttribute("myValue");
					if (detailBiayaTagihan != null && detailBiayaTagihan.getItemBiaya() != null
							&& detailBiayaTagihan.getItemBiaya().getId() != null) {
						Long itemBiayaIdTagihan = detailBiayaTagihan.getItemBiaya().getId();
						String keyTagihan = detailBiayaTagihan.getItemBiaya().getId() + "_"
								+ detailBiayaTagihan.getBayarKe();
						Integer totalBaris = jumlahBarisTagihanPerKey.get(keyTagihan);
						jumlahBarisTagihanPerKey.put(keyTagihan, totalBaris == null ? 1 : totalBaris + 1);
						Integer totalBarisItem = jumlahBarisTagihanPerItem.get(itemBiayaIdTagihan);
						jumlahBarisTagihanPerItem.put(itemBiayaIdTagihan,
								totalBarisItem == null ? 1 : totalBarisItem + 1);
					}
				}
			}
			for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
				if (cicilanPembayaran != null && cicilanPembayaran.getId() != null
						&& cicilanPembayaran.getItemBiaya() != null) {

					Long itemBiayaIdCicilan = cicilanPembayaran.getItemBiaya().getId();
					String key = itemBiayaIdCicilan + "_" + cicilanPembayaran.getBayarKe();
					if (nilais.keySet().contains(key)) {
						Double nilai = nilais.get(key) + cicilanPembayaran.getNilai();
						nilais.put(key, nilai);
					} else {
						nilais.put(key, cicilanPembayaran.getNilai());
					}
					Double nilaiItem = nilaisPerItemBiaya.containsKey(itemBiayaIdCicilan)
							? nilaisPerItemBiaya.get(itemBiayaIdCicilan) + cicilanPembayaran.getNilai()
							: cicilanPembayaran.getNilai();
					nilaisPerItemBiaya.put(itemBiayaIdCicilan, nilaiItem);

					if (cicilanPembayaran.getDetailBiaya() != null
							&& cicilanPembayaran.getDetailBiaya().getId() != null) {
						Long dbIdKey = cicilanPembayaran.getDetailBiaya().getId();
						Double nilaiDb = nilaisPerDetailBiaya.containsKey(dbIdKey)
								? nilaisPerDetailBiaya.get(dbIdKey) + cicilanPembayaran.getNilai()
								: cicilanPembayaran.getNilai();
						nilaisPerDetailBiaya.put(dbIdKey, nilaiDb);
					}
				}
			}

			System.out.println("[TAGIHAN-DEBUG]   peta nilais (per itemBiaya_bayarKe, GABUNGAN semua baris sejenis) = " + nilais);
			System.out.println("[TAGIHAN-DEBUG]   peta nilaisPerDetailBiaya (per detailBiaya id, SPESIFIK) = " + nilaisPerDetailBiaya);

			for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
				if (cicilanPembayaran != null && cicilanPembayaran.getId() != null) {

					rows = currentGrid.getRows().getChildren();
					for (Row row : rows) {
						try {
							DetailKegiatan detailKegiatan = (DetailKegiatan) row.getAttribute("detailKegiatan");
							Toolbarbutton toolbarbutton = (Toolbarbutton) row.getAttribute("add_item");

							if (row.getAttribute("pengaturanPembayaranBulanan") != null) {
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) row
										.getAttribute("pengaturanPembayaranBulanan");
								if (pengaturanPembayaranBulanan != null) {

									if (cicilanPembayaran.getItemBiaya() != null
											&& pengaturanPembayaranBulanan.getDetailBiaya() != null
											&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
											&& cicilanPembayaran.getItemBiaya().getId().equals(
													pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())) {

										boolean wajibAngsuran = kegiatan != null
												&& kegiatan.getJenisKegiatan() != null
												&& Boolean.TRUE.equals(kegiatan.getJenisKegiatan().getHanyaBerupaAngsuran());
										// Cicilan dengan PPB null bisa terjadi setelah recovery; re-link jika:
										// (a) jenis kegiatan wajib angsuran, atau (b) bayarKe cocok dengan bulan PPB
										boolean ppbNullButRequired = cicilanPembayaran.getPengaturanPembayaranBulanan() == null
												&& (wajibAngsuran
														|| (pengaturanPembayaranBulanan.getBulan() != null
																&& pengaturanPembayaranBulanan.getBulan()
																		.equals(cicilanPembayaran.getBayarKe())));
										boolean ppbRealBulanMatch = cicilanPembayaran.getPengaturanPembayaranBulanan() != null
												&& cicilanPembayaran.getPengaturanPembayaranBulanan().getRealBulan()
														.equals(pengaturanPembayaranBulanan.getRealBulan());

										if (ppbNullButRequired || ppbRealBulanMatch) {

											// Re-link PPB dan detailBiaya jika null (recovery / korupsi cross-session proxy)
											if (cicilanPembayaran.getPengaturanPembayaranBulanan() == null) {
												cicilanPembayaran
														.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
												if (cicilanPembayaran.getDetailBiaya() == null
														&& pengaturanPembayaranBulanan.getDetailBiaya() != null) {
													cicilanPembayaran.setDetailBiaya(
															pengaturanPembayaranBulanan.getDetailBiaya());
												}
												executeNativeUpdateTransaction((GeneralValueObject) cicilanPembayaran);
											}

											if (detailKegiatan != null && detailKegiatan.getTanggal() != null) {
												if (cicilanPembayaran.getTanggalTagihan() == null
														|| !Common.dateFormat83
																.get().format(cicilanPembayaran.getTanggalTagihan())
																.equals(Common.dateFormat83
																		.get().format(detailKegiatan.getTanggal()))) {

													// Guard: pastikan PPB & detailBiaya tidak null sebelum disimpan
													if (cicilanPembayaran.getPengaturanPembayaranBulanan() == null
															&& pengaturanPembayaranBulanan != null) {
														cicilanPembayaran.setPengaturanPembayaranBulanan(
																pengaturanPembayaranBulanan);
													}
													if (cicilanPembayaran.getDetailBiaya() == null
															&& pengaturanPembayaranBulanan.getDetailBiaya() != null) {
														cicilanPembayaran.setDetailBiaya(
																pengaturanPembayaranBulanan.getDetailBiaya());
													}
													cicilanPembayaran.setTanggalTagihan(detailKegiatan.getTanggal());
													executeNativeUpdateTransaction(
															(GeneralValueObject) cicilanPembayaran);
												}
											}

											Number sumCicilan = VOMahasiswa.hitungTotalCicilan(kegiatan,
													pengaturanPembayaranBulanan, cicilanPembayarans);

											Double nilai = sumCicilan == null ? 0.0 : sumCicilan.doubleValue();
											Double jml = Kegiatan.ambilJumlahTagihan(detailKegiatan,
													pengaturanPembayaranBulanan.getDetailBiaya(), kegiatan, mahasiswa,
													semester, pengaturanPembayaranBulanan);
											jml = ambilJumlahBulananTanpaDendaCustom(detailKegiatan,
													pengaturanPembayaranBulanan.getDetailBiaya(),
													pengaturanPembayaranBulanan, jml);

											JadwalPembayaran jdw = jadwalPembayaran != null
													&& jadwalPembayaran.getKhususUntukNim() != null
													&& jadwalPembayaran.getKhususUntukNim()
															.contains("," + mahasiswa.getNim() + ",") ? jadwalPembayaran
																	: null;

											Double hasilDenda = detailKegiatan != null
													&& (detailKegiatan.getBatalkanDenda() || jml.intValue() == 0)
															? jml
															: menggunakanDendaCustom(detailKegiatan)
																			? hitungTagihanDenganDendaCustom(jml,
																					detailKegiatan)
																			: pengaturanPembayaranBulanan.checkDenda(
																					jml, cicilanPembayaran.getTanggal(),
																					jdw,
																					jadwalPembayaran == null ? null
																							: jadwalPembayaran
																									.getJenisKegiatan());

											if (detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom()) {
												pengaturanPembayaranBulanan.setInfoDenda(" Penambahan denda senilai "
														+ Common.numberFormat.get().format(detailKegiatan.getDendaCustom())
														+ ".");
											}

											Double nilaiDenda = hasilDenda - jml;

											Component tag = (Component) row.getAttribute("tag");
											if (tag != null && hasilDenda != null && tag instanceof Label) {
												((Label) tag).setValue(Common.numberFormat.get().format(hasilDenda));
											}

											if (detailKegiatan != null && !detailKegiatan.getMenggunakanDendaCustom()) {
												detailKegiatan.setDendaCustom(nilaiDenda);
											}

											if (detailKegiatan != null
													&& (detailKegiatan.getBatalkanDenda() || jml.intValue() == 0)) {
												cicilanPembayaran.setDenda(0.0);
											}

											if (cicilanPembayaran != null && cicilanPembayaran.getId() != null
													&& (cicilanPembayaran.getDenda().intValue() != nilaiDenda
															.intValue())) {
												// Guard: pastikan PPB & detailBiaya tidak null sebelum disimpan
												if (cicilanPembayaran.getPengaturanPembayaranBulanan() == null
														&& pengaturanPembayaranBulanan != null) {
													cicilanPembayaran.setPengaturanPembayaranBulanan(
															pengaturanPembayaranBulanan);
												}
												if (cicilanPembayaran.getDetailBiaya() == null
														&& pengaturanPembayaranBulanan.getDetailBiaya() != null) {
													cicilanPembayaran.setDetailBiaya(
															pengaturanPembayaranBulanan.getDetailBiaya());
												}
												cicilanPembayaran.setDenda(nilaiDenda);
												executeNativeUpdateTransaction((GeneralValueObject) cicilanPembayaran);
											}

											if (nilai != null && ((nilai > hasilDenda && nilai < -0.1)
													|| (nilai < hasilDenda && nilai > 0.1))) {
												/* sclass ikut dipasang karena rule css modern
												 * memaksa background sel dengan !important
												 * sehingga inline style tr saja tertimpa */
												//row.setStyle("background-color: #fef2f2;");
												//row.setSclass("ais-status-kurang");
											}

											boolean benar = nilai != null && (nilai < -0.1 || nilai > 0.1)
													&& (nilai.intValue() >= hasilDenda.intValue()
															|| nilai.intValue() == hasilDenda.intValue());

											// FIX NPE rutin: jadwalPembayaran nullable, lihat penjelasan di hitungUlang().
											toolbarbutton.setVisible(jadwalPembayaran == null
													|| jadwalPembayaran.getJenisKegiatan() == null
													|| !jadwalPembayaran.getJenisKegiatan().getTidakBolehMengangsur());
											if (benar) {
												//row.setStyle("background-color: #f1f5f9;");
												//row.setSclass("ais-status-lunas");
												toolbarbutton.setVisible(false);
												map.remove(pengaturanPembayaranBulanan.getId());
											} else if (nilai != null && hasilDenda != null && hasilDenda > 0.1
													&& (nilai < -0.1 || nilai > 0.1)
													&& nilai.intValue() < hasilDenda.intValue()) {
												//row.setStyle("background-color: #fef2f2;");
												//row.setSclass("ais-status-kurang");
											}

											MyLabelAgakKecil dibayar = (MyLabelAgakKecil) row.getAttribute("dibayar");
											if (dibayar != null && nilai != null) {
												dibayar.setValue(Common.numberFormat.get().format(nilai));
											}

											MyLabelAgakKecil kurang = (MyLabelAgakKecil) row.getAttribute("kurang");
											Double sisa = (hasilDenda == null ? 0.0 : hasilDenda)
													- (nilai == null ? 0.0 : nilai);
											if (kurang != null) {
												kurang.setValue(Common.numberFormat.get().format(sisa));
												// Indikator warna: hijau=lunas, kuning=cicilan, merah=belum
												double tagTotal = hasilDenda != null ? hasilDenda : 0.0;
												if (sisa <= 0.5 && tagTotal > 0.5) {
													kurang.setStyle("color:#15803d;font-weight:600;");
												} else if (sisa < tagTotal - 0.5) {
													kurang.setStyle("color:#a16207;");
												} else {
													kurang.setStyle("color:#b91c1c;");
												}
											}

											Label ket = null;
											try {
												ket = (Label) row.getAttribute("ket");
												if (ket != null) {
													String desc = pengaturanPembayaranBulanan.getKeterangan();
													desc = (desc.isEmpty()
															? (pengaturanPembayaranBulanan.getDetailBiaya()
																	.getItemBiaya().getNama())
															: desc) + ",  " + pengaturanPembayaranBulanan.getNamaBulan()
															+ " " + ", nominal Rp. " + Common.numberFormat.get().format(jml)
															+ (hasilDenda.intValue() > jml.intValue()
																	? pengaturanPembayaranBulanan.getInfoDenda()
																	: "");

													desc = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
															.getKode()
															+ " " + desc
															+ (detailKegiatan == null
																	|| detailKegiatan.getUraian() == null ? ""
																			: " " + detailKegiatan.getUraian());

													ket.setValue(desc);
												}
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:546");
											}

											MyLabelAgakKecilBoldMerah infoDenda = null;
											try {
												infoDenda = (MyLabelAgakKecilBoldMerah) row.getAttribute("infoDenda");
												if (infoDenda != null) {
													if (cicilanPembayaran.getDenda().intValue() == 0) {
														infoDenda.setValue("");
														MyCheckboxConfig batalkanDenda = (MyCheckboxConfig) row
																.getAttribute("batalkanDenda");
														if (batalkanDenda != null) {
															batalkanDenda.setVisible(false);
														}
													} else {
														infoDenda.setValue(pengaturanPembayaranBulanan.getInfoDenda());
														MyCheckboxConfig batalkanDenda = (MyCheckboxConfig) row
																.getAttribute("batalkanDenda");
														if (batalkanDenda != null) {
															batalkanDenda.setVisible(true);
														}
													}
												}
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:570");
											}

											break;
										}
									}
								}
							} else if (row.getAttribute("myValue") != null) {

								DetailBiaya detailBiaya = (DetailBiaya) row.getAttribute("myValue");
								if (detailBiaya != null
										&& detailBiaya.getBayarKe().equals(cicilanPembayaran.getBayarKe())
										&& cicilanPembayaran.getItemBiaya() != null && cicilanPembayaran.getItemBiaya()
												.getId().equals(detailBiaya.getItemBiaya().getId())) {
									try {
										if (detailKegiatan != null && detailKegiatan.getTanggal() != null) {
											if (cicilanPembayaran.getTanggalTagihan() == null || !Common.dateFormat83
													.get().format(cicilanPembayaran.getTanggalTagihan())
													.equals(Common.dateFormat83.get().format(detailKegiatan.getTanggal()))) {

												cicilanPembayaran.setTanggalTagihan(detailKegiatan.getTanggal());
												executeNativeUpdateTransaction((GeneralValueObject) cicilanPembayaran);
											}
										} else {
											Date defaultTanggalTagihan = detailBiaya.getDefaultTanggalTagihan();
											if (defaultTanggalTagihan != null) {
												if (cicilanPembayaran.getTanggalTagihan() == null
														|| !Common.dateFormat83
																.get().format(cicilanPembayaran.getTanggalTagihan())
																.equals(Common.dateFormat83
																		.get().format(defaultTanggalTagihan))) {
													cicilanPembayaran.setTanggalTagihan(defaultTanggalTagihan);
													executeNativeUpdateTransaction(
															(GeneralValueObject) cicilanPembayaran);
												}
												}
											}

										// FIX NPE rutin: jadwalPembayaran nullable, lihat penjelasan di hitungUlang().
										toolbarbutton.setVisible(jadwalPembayaran == null
												|| jadwalPembayaran.getJenisKegiatan() == null
												|| !jadwalPembayaran.getJenisKegiatan().getTidakBolehMengangsur());
										Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
										jumlah = ambilJumlahDetailBiayaTanpaDendaCustom(detailKegiatan, detailBiaya,
												jumlah);

										Double nilaiDenda = detailKegiatan != null
												&& detailKegiatan.getMenggunakanDendaCustom()
														? detailKegiatan.getDendaCustom()
														: 0.0;
										Double jumlahDenganDenda = jumlah + nilaiDenda;

										if (cicilanPembayaran != null && cicilanPembayaran.getId() != null
												&& (cicilanPembayaran.getDenda().intValue() != nilaiDenda.intValue())) {
											cicilanPembayaran.setDenda(nilaiDenda);
											executeNativeUpdateTransaction((GeneralValueObject) cicilanPembayaran);
										}

										String key = detailBiaya.getItemBiaya().getId() + "_"
												+ detailBiaya.getBayarKe();
										// PERBAIKAN dobel-tampil (lihat komentar di pembangunan peta
										// nilaisPerDetailBiaya di atas): utamakan total yang benar-benar
										// ber-FK ke DetailBiaya SPESIFIK baris ini; fallback ke peta lama
										// (per item+bayarKe, mencakup SEMUA baris sejenis) hanya bila baris
										// ini belum punya cicilan yang ber-FK detailBiaya sama sekali.
										boolean pakaiPetaSpesifik = detailBiaya.getId() != null
												&& nilaisPerDetailBiaya.containsKey(detailBiaya.getId());
										Double nilai = pakaiPetaSpesifik ? nilaisPerDetailBiaya.get(detailBiaya.getId())
												: nilais.get(key);
										Double nilaiGabungan = nilais.get(key);
										Double nilaiGabunganItem = nilaisPerItemBiaya.get(detailBiaya.getItemBiaya().getId());
										Integer jumlahBarisKey = jumlahBarisTagihanPerKey.get(key);
										Integer jumlahBarisItem = jumlahBarisTagihanPerItem.get(detailBiaya.getItemBiaya().getId());
										if (pakaiPetaSpesifik && nilaiGabungan != null && nilai != null
												&& (jumlahBarisKey == null || jumlahBarisKey.intValue() <= 1)
												&& nilaiGabungan.doubleValue() > nilai.doubleValue()) {
											nilai = nilaiGabungan;
										}
										if (nilaiGabunganItem != null && (nilai == null
												|| nilaiGabunganItem.doubleValue() > nilai.doubleValue())
												&& (jumlahBarisItem == null || jumlahBarisItem.intValue() <= 1)) {
											nilai = nilaiGabunganItem;
										}

										System.out.println("[TAGIHAN-DEBUG]   render baris: detailBiayaId=" + detailBiaya.getId()
												+ " itemBiaya=" + detailBiaya.getItemBiaya().getId() + "-" + detailBiaya.getItemBiaya().getNama()
												+ " bayarKe=" + detailBiaya.getBayarKe() + " kunci=\"" + key + "\""
												+ " jumlahTagihan=" + jumlah + " jumlahDenganDenda=" + jumlahDenganDenda
												+ " | dibayar diambil dari " + (pakaiPetaSpesifik ? "peta SPESIFIK per-detailBiaya" : "peta GABUNGAN item+bayarKe (fallback)")
												+ " | gabungan=" + nilaiGabungan + " jumlahBarisKey=" + jumlahBarisKey
												+ " | gabunganItem=" + nilaiGabunganItem + " jumlahBarisItem=" + jumlahBarisItem
												+ " = " + nilai);

										boolean benar = nilai != null && (nilai < -0.1 || nilai > 0.1)
												&& (nilai.intValue() >= jumlahDenganDenda.intValue()
														|| nilai.intValue() == jumlahDenganDenda.intValue());

										if (benar) {
											//row.setStyle("background-color: #f1f5f9;");
											//row.setSclass("ais-status-lunas");
											toolbarbutton.setVisible(false);
										} else if (nilai != null && jumlahDenganDenda != null && jumlahDenganDenda > 0.1
												&& (nilai < -0.1 || nilai > 0.1)
												&& nilai.intValue() < jumlahDenganDenda.intValue()) {
											//row.setStyle("background-color: #fef2f2;");
											//row.setSclass("ais-status-kurang");
										}

										MyLabelAgakKecil dibayar = (MyLabelAgakKecil) row.getAttribute("dibayar");
										if (dibayar != null && nilai != null) {
											dibayar.setValue(Common.numberFormat.get().format(nilai));
										}

										MyLabelAgakKecil kurang = (MyLabelAgakKecil) row.getAttribute("kurang");
										Double sisa = (jumlahDenganDenda == null ? 0.0 : jumlahDenganDenda)
												- (nilai == null ? 0.0 : nilai);
										if (kurang != null) {
											kurang.setValue(Common.numberFormat.get().format(sisa));
											double tagTotal2 = jumlahDenganDenda != null ? jumlahDenganDenda : 0.0;
											if (sisa <= 0.5 && tagTotal2 > 0.5) {
												kurang.setStyle("color:#15803d;font-weight:600;");
											} else if (sisa < tagTotal2 - 0.5) {
												kurang.setStyle("color:#a16207;");
											} else {
												kurang.setStyle("color:#b91c1c;");
											}
										}

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
								}
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			}
		}

		// Tampilkan "Hapus Tagihan" hanya untuk baris yang belum ada pembayarannya.
		// Dilakukan setelah semua loop cicilan selesai sehingga label "dibayar" sudah final.
		if (currentGrid != null && currentGrid.getRows() != null) {
			for (Object rowObj : currentGrid.getRows().getChildren()) {
				if (!(rowObj instanceof Row)) continue;
				Row row = (Row) rowObj;
				Toolbarbutton btnHapus = (Toolbarbutton) row.getAttribute("btnHapus");
				if (btnHapus == null) continue;
				MyLabelAgakKecil dibayar = (MyLabelAgakKecil) row.getAttribute("dibayar");
				boolean adaPembayaran = dibayar != null && dibayar.getValue() != null
						&& !dibayar.getValue().isEmpty() && !dibayar.getValue().equals("0");
				btnHapus.setVisible(!adaPembayaran);
			}
		}

		return new ArrayList<PengaturanPembayaranBulanan>(map.values());
	}

	@Override
	public void render(final Row rowPembayaran, Object arg1) throws Exception {

		buatBaruJikaBelumAda();

		rowPembayaran.setValign("top");
		DetailKegiatan tempdata = null;
		DetailBiaya tempdetailBiaya = null;
		PengaturanPembayaranBulanan temppengaturanPembayaranBulanan = null;

		if (arg1 instanceof DetailBiaya) {
			tempdetailBiaya = (DetailBiaya) arg1;
		} else if (arg1 instanceof PengaturanPembayaranBulanan) {
			temppengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) arg1;
			if (temppengaturanPembayaranBulanan != null) {
				tempdetailBiaya = temppengaturanPembayaranBulanan.getDetailBiaya();
			}
		}

		// Mode angsuran: jangan render baris DetailBiaya murni (tanpa ppb) agar tidak
		// membuat DetailKegiatan polusi DB dan tidak menampilkan duplikat row di layar.
		// KECUALI: jika jenjang mahasiswa ini khusus dikonfigurasi sebagai "bukan angsuran"
		// melalui modeAngsuranUntukJenjang → tetap render sebagai billing reguler.
		if (temppengaturanPembayaranBulanan == null && tempdetailBiaya != null
				&& kegiatan != null && kegiatan.getJenisKegiatan() != null
				&& Boolean.TRUE.equals(kegiatan.getJenisKegiatan().getHanyaBerupaAngsuran())) {
			boolean bukanAngsuranUntukJenjangIni = false;
			try {
				Jenjang mhsJenjang = null;
				if (mahasiswa != null) {
					mhsJenjang = mahasiswa.getJurusan() != null
							? mahasiswa.getJurusan().getJenjang() : mahasiswa.getJenjang();
				} else if (biodataCalonMahasiswa != null) {
					mhsJenjang = biodataCalonMahasiswa.getJenjang();
				}
				// Per-jenjang PER-SEMESTER (dan per-angkatan, format TAHUN:SMT): baris
				// reguler hanya disembunyikan bila aturan "harus angsuran" benar-benar
				// mengenai jenjang+semester+angkatan ini (TRUE). FALSE maupun null
				// (semester/angkatan di luar daftar "Berlaku di smt") → tetap dirender
				// sebagai billing reguler agar tagihan tidak lenyap dari layar.
				Integer angkatanMhs = mahasiswa != null ? mahasiswa.getTahunangkatan()
						: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getTahun() : null);
				Boolean modeAngsuran = kegiatan.getJenisKegiatan().modeAngsuranUntukJenjang(mhsJenjang, semester,
						angkatanMhs);
				bukanAngsuranUntukJenjangIni = !Boolean.TRUE.equals(modeAngsuran);
				if (ais.database.model.JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
						"[DEBUG-ANGSURAN][renderer] mhs=" + (mahasiswa != null ? mahasiswa.getNim() : "null")
						+ " jenjang=" + (mhsJenjang != null ? mhsJenjang.getNama() : "null")
						+ " modeAngsuran=" + modeAngsuran + " bukanAngsuranUntukJenjangIni=" + bukanAngsuranUntukJenjangIni);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:737");}
			if (!bukanAngsuranUntukJenjangIni) {
				rowPembayaran.setVisible(false);
				return;
			}
		}

		boolean isStaf = tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null;

		if (temppengaturanPembayaranBulanan != null || tempdetailBiaya != null) {
			if (kegiatan != null && kegiatan.getId() != null && tempdata == null) {

				tempdata = temppengaturanPembayaranBulanan != null
						? kegiatan.ambilSatuDetailKegiatan(temppengaturanPembayaranBulanan, detailKegiatans)
						: kegiatan.ambilSatuDetailKegiatan(tempdetailBiaya);

				if (tempdata == null) {
					tempdata = new DetailKegiatan();
					tempdata.setPengaturanPembayaranBulanan(temppengaturanPembayaranBulanan);
					tempdata.setUraian("");
					tempdata.setDetailBiaya(tempdetailBiaya);
					tempdata.setKeterangan(tempdetailBiaya == null ? "" : tempdetailBiaya.getKeterangan());
					tempdata.setKegiatan(kegiatan);
					executeNativeSaveTransaction((GeneralValueObject) tempdata);
				}
			}
		}

		JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
				&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",") ? jadwalPembayaran
						: null;

		final DetailKegiatan detailKegiatan = tempdata;
		final MyLabelAgakKecil kurang = new MyLabelAgakKecil("0");
		MyLabelAgakKecil dibayar = new MyLabelAgakKecil("0");
		final DetailBiaya detailBiaya;

		final PengaturanPembayaranBulanan pengaturanPembayaranBulanan;

		final MyToolbarbuttonConfig prosesPembayaran = new MyToolbarbuttonConfig("Proses Bayar",
				"/img/svg/check-square.svg");
		prosesPembayaran.setOrient("vertical");
		prosesPembayaran.setAttribute("janganDisabled", true);
		prosesPembayaran.setStyle("font-size:9px;");
		rowPembayaran.setAttribute("add_item", prosesPembayaran);

		Double jml = 0.0;
		if (arg1 instanceof DetailBiaya) {
			pengaturanPembayaranBulanan = null;
			detailBiaya = (DetailBiaya) arg1;

			// PERBAIKAN NPE (ID:366): detailKegiatan.getDetailBiaya() bisa saja berupa DetailBiaya
			// TRANSIENT (belum ter-simpan, id=null) -- terjadi saat SetingBiayaHelper membuat baris
			// DetailBiaya ad-hoc di memori (getDefaultSettingBiaya/getDetailBiayaDefault) untuk
			// kombinasi prodi/semester yang belum punya baris tersimpan di database (mis. Setting
			// Biaya untuk prodi tertentu semester ini belum lengkap). getId() lama dipanggil langsung
			// tanpa cek null -> NPE. Ambil id ke variabel dulu, null-safe.
			Long idDetailBiayaTerpasang = detailKegiatan == null || detailKegiatan.getDetailBiaya() == null ? null
					: detailKegiatan.getDetailBiaya().getId();
			if (detailBiaya != null && detailKegiatan != null && detailKegiatan.getDetailBiaya() != null
					&& idDetailBiayaTerpasang != null && !idDetailBiayaTerpasang.equals(detailBiaya.getId())) {
				detailKegiatan.setDetailBiaya(detailBiaya);
				detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
				detailKegiatan.setKegiatan(kegiatan);
				executeNativeUpdateTransaction((GeneralValueObject) detailKegiatan);
			}

			rowPembayaran.setAttribute("myValue", detailBiaya);
			jml = Kegiatan.ambilJumlahTagihan(detailKegiatan, kegiatan, detailBiaya, false);
			jml = ambilJumlahDetailBiayaTanpaDendaCustom(detailKegiatan, detailBiaya, jml);
			dataTagihan.put(detailBiaya.getId(), jml);

			if (gridCicilan != null) {

				String keterangan = detailBiaya.getKeterangan();

				Vbox vbox = new Vbox();
				vbox.setParent(rowPembayaran);
				final Label ket;
				(ket = new Label(detailBiaya.getItemBiaya().getKode() + " " + keterangan
						+ (detailKegiatan == null || detailKegiatan.getUraian() == null ? ""
								: " " + detailKegiatan.getUraian())))
						.setParent(vbox);

				// Ikon "mata": untuk item biaya yang nominalnya dihitung dari SKS/matakuliah diambil,
				// tampilkan rincian perkuliahan (grid + download PDF/Excel).
				ais.action.master.helper.RincianPerkuliahanTagihanHelper.tambahIkonMata(vbox, mahasiswa, semester,
						detailBiaya.getItemBiaya());

				if (detailBiaya.getDefaultTanggalDeadline() != null) {
					new MyLabelAgakKecilBoldMerah(
							"Deadline : " + Common.dateFormat4.get().format(detailBiaya.getDefaultTanggalDeadline()))
							.setParent(vbox);

					if (!detailBiaya.getInfoDenda().isEmpty()) {
						MyLabelAgakKecilBoldMerah infoDenda;
						(infoDenda = new MyLabelAgakKecilBoldMerah(detailBiaya.getInfoDenda())).setParent(vbox);
						rowPembayaran.setAttribute("infoDenda", infoDenda);
					}
				}

				Double hasilDenda = detailKegiatan != null && (detailKegiatan.getBatalkanDenda() || jml.intValue() == 0)
						? jml
						: menggunakanDendaCustom(detailKegiatan) ? hitungTagihanDenganDendaCustom(jml, detailKegiatan)
								: detailBiaya.checkDenda(jml, tanggalValidasi, jdw,
										jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan(), null);

				if (detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom()) {
					detailBiaya.setInfoDenda(" Penambahan denda senilai "
							+ Common.numberFormat.get().format(detailKegiatan.getDendaCustom()) + ".");
				}

				Double nilaiDenda = hasilDenda - jml;
				if (detailKegiatan != null && !detailKegiatan.getMenggunakanDendaCustom()) {
					detailKegiatan.setDendaCustom(nilaiDenda);
				}
				if (!detailBiaya.getInfoDenda().isEmpty()) {
					jml = hasilDenda;
					dataTagihan.put(detailBiaya.getId(), jml);
				}

				if (detailKegiatan != null && detailKegiatan.getTanggal() != null) {
					RevisiHelper
							.createNewRevisi(DetailKegiatan.class, detailKegiatan,
									"Tgl tagihan: " + Common.dateFormat2.get().format(detailKegiatan.getTanggal()))
							.setParent(vbox);
				}

				boolean b = detailBiaya != null && detailBiaya.getDetailSettingBiaya() != null
						&& detailBiaya.getDetailSettingBiaya().getSettingBiaya() != null
						&& detailBiaya.getDetailSettingBiaya().getSettingBiaya().getJumlahPembayaran() > 1;

				if (b) {
					RevisiHelper
							.createNewRevisi(DetailSettingBiaya.class, detailBiaya.getDetailSettingBiaya(),
									"Tagihan ke-" + detailBiaya.getBayarKe() + "-" + detailBiaya.getId())
							.setParent(vbox);
				}

				if (isStaf) {
					final DetailBiaya dbFinal = detailBiaya;
					final Row rowFinal = rowPembayaran;
					Toolbarbutton btnHapus = new Toolbarbutton("Hapus Tagihan");
					btnHapus.setStyle("font-size:10px; color:#c00;");
					btnHapus.setVisible(false);
					btnHapus.setParent(vbox);
					rowPembayaran.setAttribute("btnHapus", btnHapus);
					btnHapus.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show(
									"Hapus tagihan ini dari sistem?\n" + dbFinal.getItemBiaya().getNama(),
									"Konfirmasi Hapus", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
									MyMessageboxConfig.QUESTION, new EventListener() {
										@Override
										public void onEvent(Event ev) throws Exception {
											int ans = Integer.parseInt(ev.getData().toString());
											if (ans == MyMessageboxConfig.OK) {
												executeNativeDeleteTransaction(dbFinal);
												rowFinal.setVisible(false);
											}
										}
									});
						}
					});
				// "Lihat Sumber": tagihan semester (non-bulanan) bersumber dari satu baris
				// SettingBiaya -- buka langsung dialog ubah SettingBiaya tsb (pola sama dg
				// DaftarUlangMahasiswaBaruAction.java yg sudah lebih dulu memakai onAddExternal).
				Toolbarbutton btnLihatSumber = new Toolbarbutton("Lihat Sumber");
				btnLihatSumber.setStyle("font-size:10px; color:#1d4ed8;");
				btnLihatSumber.setParent(vbox);
				btnLihatSumber.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (dbFinal.getSettingBiaya() == null) {
							MyMessageboxConfig.show(
								"Sumber Setting Biaya untuk tagihan ini tidak ditemukan (kemungkinan baris setting biaya sudah dihapus atau diubah oleh admin lain). "
									+ "Silakan periksa manual melalui menu Setting Biaya menggunakan kombinasi Jenjang/Prodi/Angkatan/Semester pada tagihan ini. "
									+ "Jika Anda yakin seharusnya ada, hubungi Administrator/Pengembang Sistem dengan melampirkan tangkapan layar (screenshot) tagihan ini.",
								"Sumber Tidak Ditemukan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return;
						}
						ais.action.master.SetingBiayaAction.onAddExternal(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								DetailPembayaranMahasiswaRenderer.this.eventListener
									.onEvent(new Event("", null, kegiatan));
							}
						}, dbFinal.getSettingBiaya());
					}
				});
				}

				Hbox hbox = new Hbox();

				if (kegiatan != null && kegiatan.getMahasiswa() != null && detailBiaya != null
						&& detailBiaya.getItemBiaya() != null && kegiatan.getMahasiswa().getKelompokMahasiswa() != null
						&& kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtMulai() <= kegiatan.getSemster()
						&& kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtSampai() >= kegiatan.getSemster()
						&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa() != null
						&& !(detailKegiatan != null && detailKegiatan.adaDiskon())
						&& !kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().ambilItemBiayaIds()
								.isEmpty()
						&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().ambilItemBiayaIds()
								.contains(detailBiaya.getItemBiaya().getId())) {

					RevisiHelper
							.createNewRevisi(JenisDiskonMahasiswa.class,
									kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa(),
									kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().getNama())
							.setParent(vbox);

				} else {

					if (kegiatan.getCalonMahasiswa() != null && kegiatan.getCalonMahasiswa().getJenisSeleksi() != null
							&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa() != null
							&& !(detailKegiatan != null && detailKegiatan.adaDiskon())
							&& !kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.ambilItemBiayaIds().isEmpty()
							&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.ambilItemBiayaIds().contains(detailBiaya.getItemBiaya().getId())
							&& (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterMulai() == null
									|| (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterMulai() != null
											&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
													.getSemesterMulai() <= kegiatan.getSemster()))
							&& (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterSampai() == null
									|| (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterSampai() != null
											&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
													.getSemesterSampai() >= kegiatan.getSemster()))) {
						RevisiHelper.createNewRevisi(JenisDiskonMahasiswa.class,
								kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa(),
								kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getNama())
								.setParent(vbox);

					} else if (kegiatan.getMahasiswa() != null && kegiatan.getMahasiswa().getJenisSeleksi() != null
							&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa() != null
							&& !(detailKegiatan != null && detailKegiatan.adaDiskon())
							&& !kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
									.isEmpty()
							&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
									.contains(detailBiaya.getItemBiaya().getId())
							&& (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterMulai() == null
									|| (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterMulai() != null
											&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
													.getSemesterMulai() <= kegiatan.getSemster()))
							&& (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterSampai() == null
									|| (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterSampai() != null
											&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
													.getSemesterSampai() >= kegiatan.getSemster()))) {
						RevisiHelper
								.createNewRevisi(JenisDiskonMahasiswa.class,
										kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa(),
										kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getNama())
								.setParent(vbox);

					} else {
						if (detailKegiatan != null && detailKegiatan.adaDiskon()
								&& detailKegiatan.getDiskonMahasiswaData() != null
								&& detailKegiatan.getDiskonMahasiswaData().getJenisDiskonMahasiswa() != null) {
							RevisiHelper
									.createNewRevisi(DetailKegiatan.class, detailKegiatan,
											detailKegiatan.getDiskonMahasiswaData().getJenisDiskonMahasiswa().getNama())
									.setParent(vbox);
						}

						if (detailKegiatan != null && detailKegiatan.adaDiskon()
								&& detailKegiatan.getDiskonMahasiswaData2() != null && detailKegiatan.adaDiskon()
								&& detailKegiatan.getDiskonMahasiswaData2().getJenisDiskonMahasiswa() != null) {
							RevisiHelper
									.createNewRevisi(DetailKegiatan.class, detailKegiatan, detailKegiatan
											.getDiskonMahasiswaData2().getJenisDiskonMahasiswa().getNama())
									.setParent(vbox);
						}

						if (detailKegiatan != null && detailKegiatan.adaDiskon()
								&& detailKegiatan.getDiskonMahasiswaData3() != null && detailKegiatan.adaDiskon()
								&& detailKegiatan.getDiskonMahasiswaData3().getJenisDiskonMahasiswa() != null) {
							RevisiHelper
									.createNewRevisi(DetailKegiatan.class, detailKegiatan, detailKegiatan
											.getDiskonMahasiswaData3().getJenisDiskonMahasiswa().getNama())
									.setParent(vbox);
						}
					}
				}

				RevisiHelper.createNewRevisi(DetailBiaya.class, detailBiaya, "T").setParent(hbox);

				hbox.setParent(vbox);
				prosesPembayaran.setParent(hbox);

				final Textbox uraian = new Textbox(
						detailKegiatan == null || detailKegiatan.getUraian() == null ? "" : detailKegiatan.getUraian());
				uraian.setCols(15);
				uraian.setRows(3);
				uraian.setVisible(false);
				uraian.setParent(vbox);

				final MyDatebox tgl = new MyDatebox(
						detailKegiatan == null ? (detailBiaya == null ? null : detailBiaya.getDefaultTanggalTagihan())
								: detailKegiatan.getTanggal());
				tgl.setCols(10);
				tgl.setFormat(Common.dateFormat.get().toPattern());
				tgl.setVisible(false);
				tgl.setReadonly(false);

				if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getTanggalTagihanRegistrasi() != null
						&& kegiatan.getJenisKegiatan() != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
						&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()
								.equals(kegiatan.getJenisKegiatan().getId())) {
					new Label(Common.dateFormat1.get().format(
							kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getTanggalTagihanRegistrasi()));
				} else if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getTanggalTagihanDaftarUlang() != null
						&& kegiatan.getJenisKegiatan() != null
						&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
						&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()
								.equals(kegiatan.getJenisKegiatan().getId())) {
					new Label(Common.dateFormat1.get().format(
							kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getTanggalTagihanDaftarUlang()));
				} else {
					tgl.setParent(vbox);
				}

				final Hbox hbox2 = new Hbox();
				hbox2.setVisible(false);
				final MyDoublebox nilaiDendaCustom = new MyDoublebox(
						detailKegiatan == null ? 0.0 : detailKegiatan.getDendaCustom());
				nilaiDendaCustom.setCols(5);
				hbox2.appendChild(nilaiDendaCustom);
				nilaiDendaCustom.setDisabled(detailKegiatan == null || !detailKegiatan.getMenggunakanDendaCustom());

				final MyCheckboxConfig dendaKustom = new MyCheckboxConfig("Denda Kustom");
				dendaKustom.setChecked(detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom());
				dendaKustom.setStyle("font-size:8px;");
				hbox2.appendChild(dendaKustom);

				final MyCheckboxConfig bukanTagihan = new MyCheckboxConfig("Tidak Aktif");
				bukanTagihan.setChecked(detailKegiatan != null && !detailKegiatan.getAktif());
				bukanTagihan.setStyle("font-size:8px;");
				hbox2.appendChild(bukanTagihan);

				hbox2.setParent(vbox);

				nilaiDendaCustom.setVisible(detailKegiatan == null
						|| (!detailKegiatan.getBukanTagihan() && !detailKegiatan.getItemBiaya().getNilaiBisaDiubah()));
				dendaKustom.setVisible(detailKegiatan == null
						|| (!detailKegiatan.getBukanTagihan() && !detailKegiatan.getItemBiaya().getNilaiBisaDiubah()));

				final EventListener eventListenerCustom = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (kegiatan != null && kegiatan.getId() != null) {
							DetailKegiatan dk = detailKegiatan;
							if (dk == null) {
								dk = new DetailKegiatan();
							}
							dk.setMenggunakanDendaCustom(dendaKustom.isChecked());
							dk.setDendaCustom(nilaiDendaCustom.getValue());
							dk.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
							dk.setUraian(uraian.getValue());
							dk.setDetailBiaya(detailBiaya);
							dk.setTanggal(tgl.getValue());
							dk.setTanggalCustom(tgl.getValue());
							dk.setKeterangan(detailBiaya.getKeterangan());
							dk.setKegiatan(kegiatan);
							dk.setAktif(!bukanTagihan.isChecked());

							if (dk.getId() == null) {
								executeNativeSaveTransaction((GeneralValueObject) dk);
							} else {
								executeNativeUpdateTransaction((GeneralValueObject) dk);
							}

							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
											: kegiatan.ambilDetailKegiatan(true);
									Common.clear(rowPembayaran);
									render(rowPembayaran, detailBiaya);
								}
							});
						}
					}
				};

				bukanTagihan.addEventListener("onClick", eventListenerCustom);

				dendaKustom.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						DetailKegiatan dk = detailKegiatan;
						if (dk == null) {
							dk = new DetailKegiatan();
						}
						dk.setMenggunakanDendaCustom(dendaKustom.isChecked());
						dk.setDendaCustom(nilaiDendaCustom.getValue());

						if (dk.getId() == null) {
							executeNativeSaveTransaction((GeneralValueObject) dk);
						} else {
							executeNativeUpdateTransaction((GeneralValueObject) dk);
						}

						nilaiDendaCustom
								.setDisabled(detailKegiatan == null || !detailKegiatan.getMenggunakanDendaCustom());
					}
				});
				nilaiDendaCustom.addEventListener("onChange", eventListenerCustom);

				rowPembayaran.setAttribute("uraian", uraian);
				rowPembayaran.setAttribute("tgl", tgl);
				rowPembayaran.setAttribute("ket", ket);
				rowPembayaran.setAttribute("detailKegiatan", detailKegiatan);

				final MyToolbarbuttonConfig toolbarbuttonedit = new MyToolbarbuttonConfig(
						dendaKustom.isVisible() ? "Tgl, Uraian, dan Denda" : "Tgl dan Uraian",
						"/img/svg/edit-box-line.svg");
				toolbarbuttonedit.setOrient("vertical");
				final MyToolbarbuttonConfig toolbarbuttonsave = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				toolbarbuttonsave.setOrient("vertical");
				toolbarbuttonedit.setStyle("font-size:9px;");
				toolbarbuttonedit.setParent(hbox);

				toolbarbuttonedit.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						uraian.setVisible(true);
						tgl.setVisible(true);
						hbox2.setVisible(true);
						toolbarbuttonsave.setVisible(true);
						toolbarbuttonedit.setVisible(false);
					}
				});

				toolbarbuttonsave.setVisible(false);
				toolbarbuttonsave.setParent(hbox);
				toolbarbuttonsave.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						uraian.setVisible(false);
						tgl.setVisible(false);
						hbox2.setVisible(false);
						toolbarbuttonedit.setVisible(true);
						toolbarbuttonsave.setVisible(false);
						ket.setValue(detailBiaya.getKeterangan() + " " + uraian.getValue());
						eventListenerCustom.onEvent(arg0);
					}
				});

			} else {
				Label ket;
				(ket = new Label(detailBiaya.getItemBiaya().getKode() + " " + detailBiaya.getKeterangan()))
						.setParent(rowPembayaran);
				rowPembayaran.setAttribute("ket", ket);
			}

			DetailKegiatan.populatePembayaran(detailBiaya, null, kegiatan, jml);

		} else if (arg1 instanceof PengaturanPembayaranBulanan) {
			pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) arg1;
			rowPembayaran.setAttribute("pengaturanPembayaranBulanan", pengaturanPembayaranBulanan);

			detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();

			if (pengaturanPembayaranBulanan != null && detailKegiatan != null
					&& (detailKegiatan.getPengaturanPembayaranBulanan() == null || !detailKegiatan
							.getPengaturanPembayaranBulanan().getId().equals(pengaturanPembayaranBulanan.getId()))) {
				detailKegiatan.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				detailKegiatan.setDetailBiaya(detailBiaya);
				detailKegiatan.setKegiatan(kegiatan);
				executeNativeUpdateTransaction((GeneralValueObject) detailKegiatan);
			}

			rowPembayaran.setAttribute("myValue", detailBiaya);
			/*
			 * Item yang di "Pengaturan Pembayaran Bulanan" di-nol-kan (tidak diatur) tidak
			 * boleh tampil memakai nilai tersimpan (detailKegiatan.getBiaya) yang basi.
			 * Samakan dengan overload kanonik Kegiatan.ambilJumlahTagihan(.., koleksi
			 * DetailKegiatan): nilai tersimpan hanya dipakai untuk item yang "nilai bisa
			 * diubah"; selain itu pakai nominal pengaturan bulanan (ni). Dengan begitu item
			 * bernominal 0 menghasilkan tagihan 0 -> baris otomatis disembunyikan di bawah.
			 */
			// detailKegiatanUntukNilai=null mencegah penggunaan nilai tersimpan (getBiaya) yang
			// basi untuk item harga-tetap. Namun saat null, hitungDiskon di dalam
			// ambilJumlahTagihan tidak bisa menyimpan diskon ke DB → diskon=0 → tagihan
			// tidak terpotong untuk bulan-bulan baru (DetailKegiatan nilaiBisaDiubah=false).
			jml = Kegiatan.ambilJumlahTagihan(detailKegiatan, detailBiaya, kegiatan, mahasiswa, semester,
					pengaturanPembayaranBulanan);
			jml = ambilJumlahBulananTanpaDendaCustom(detailKegiatan, detailBiaya, pengaturanPembayaranBulanan, jml);

			if (jml.intValue() == 0
					&& (detailKegiatan == null || detailKegiatan.getDiskon() == null
							|| detailKegiatan.getDiskon().intValue() == 0)
					&& !pengaturanPembayaranBulanan.getTetapDitampilkanWalaupunNol()) {
				if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
					rowPembayaran.setVisible(true);
				} else {
					rowPembayaran.setVisible(false);
				}
			}

			int tahapan = 0;
			if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan) {
				try {
					String bln = Common.BULAN[pengaturanPembayaranBulanan.getRealBulan() - 1];
					tahapan = Common.poulateTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan(), semester,
							mahasiswa.getSemesterMulai()).get(bln);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:1226");
				}
			}

			Double hasilDenda = detailKegiatan != null && (detailKegiatan.getBatalkanDenda() || jml.intValue() == 0)
					? jml
					: menggunakanDendaCustom(detailKegiatan) ? hitungTagihanDenganDendaCustom(jml, detailKegiatan)
							: pengaturanPembayaranBulanan.checkDenda(jml, tanggalValidasi, jdw,
									jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan());

			if (detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom()) {
				pengaturanPembayaranBulanan.setInfoDenda(" Penambahan denda senilai "
						+ Common.numberFormat.get().format(detailKegiatan.getDendaCustom()) + ".");
			}

			Double nilaiDenda = hasilDenda - jml;
			if (detailKegiatan != null && !detailKegiatan.getMenggunakanDendaCustom()) {
				detailKegiatan.setDendaCustom(nilaiDenda);
			}

			// Fix NPE: baris bulanan yatim (detailBiaya/ItemBiaya tautannya hilang) dulu
			// bikin render() crash total ("Unknown exception: NullPointerException", baris
			// kosong tanpa isi) begitu keterangan PPB kosong DAN detailBiaya null. Fallback
			// ke label netral agar baris tetap tampil & bisa dihapus admin (Hapus Tagihan).
			String desc = pengaturanPembayaranBulanan.getKeterangan();
			desc = (desc.isEmpty() ? (detailBiaya == null || detailBiaya.getItemBiaya() == null
					? "(Item Biaya tidak ditemukan)"
					: detailBiaya.getItemBiaya().getNama()) : desc)
					+ ",  " + pengaturanPembayaranBulanan.getNamaBulan() + " " + ", nominal Rp. "
					+ Common.numberFormat.get().format(jml)
					+ (hasilDenda.intValue() > jml.intValue() ? pengaturanPembayaranBulanan.getInfoDenda() : "")
					+ (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahapan > 0 ? ", tahap " + tahapan : "");

			if (!pengaturanPembayaranBulanan.getInfoDenda().isEmpty()) {
				jml = hasilDenda;
			}

			dataTagihan.put(pengaturanPembayaranBulanan.getId(), jml);

			if (gridCicilan != null) {

				Vbox vbox = new Vbox();
				vbox.setParent(rowPembayaran);
				final Label ket;
				(ket = new Label((detailBiaya == null || detailBiaya.getItemBiaya() == null ? ""
						: detailBiaya.getItemBiaya().getKode() + " ") + desc
						+ (detailKegiatan == null || detailKegiatan.getUraian() == null ? ""
								: " " + detailKegiatan.getUraian())))
						.setParent(vbox);

				if (pengaturanPembayaranBulanan.getDeadline() != null) {
					new MyLabelAgakKecilBoldMerah(
							"Deadline : " + Common.dateFormat4.get().format(pengaturanPembayaranBulanan.getDeadline()))
							.setParent(vbox);

					if (!pengaturanPembayaranBulanan.getInfoDenda().isEmpty()) {
						MyLabelAgakKecilBoldMerah infoDenda;
						(infoDenda = new MyLabelAgakKecilBoldMerah(pengaturanPembayaranBulanan.getInfoDenda()))
								.setParent(vbox);
						rowPembayaran.setAttribute("infoDenda", infoDenda);
					}
				}

				if (detailKegiatan != null && detailKegiatan.getTanggal() != null) {
					RevisiHelper
							.createNewRevisi(DetailKegiatan.class, detailKegiatan,
									"Tgl tagihan: " + Common.dateFormat2.get().format(detailKegiatan.getTanggal()))
							.setParent(vbox);
				}

				boolean b = detailBiaya != null && detailBiaya.getDetailSettingBiaya() != null
						&& detailBiaya.getDetailSettingBiaya().getSettingBiaya() != null
						&& detailBiaya.getDetailSettingBiaya().getSettingBiaya().getJumlahPembayaran() > 1;

				if (b) {
					RevisiHelper
							.createNewRevisi(DetailSettingBiaya.class, detailBiaya.getDetailSettingBiaya(),
									"Tagihan ke-" + detailBiaya.getBayarKe() + "-" + detailBiaya.getId())
							.setParent(vbox);
				}

				if ((detailKegiatan != null && (detailKegiatan.getBatalkanDenda()))
						|| !pengaturanPembayaranBulanan.getInfoDenda().isEmpty()) {

					if (tbmuser != null && tbmuser.getMahasiswa() != null
							&& (detailKegiatan != null && (detailKegiatan.getBatalkanDenda()))) {
						new MyLabelAgakKecilBoldMerah("Denda tidak diberlakukan").setParent(vbox);
					} else if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
							&& (detailKegiatan == null || !detailKegiatan.getMenggunakanDendaCustom())) {
						final MyCheckboxConfig batalkanDenda = new MyCheckboxConfig("Batalkan Denda");
						rowPembayaran.setAttribute("batalkanDenda", batalkanDenda);
						batalkanDenda.setChecked(detailKegiatan != null && detailKegiatan.getBatalkanDenda());
						batalkanDenda.setParent(vbox);
						batalkanDenda.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								DetailKegiatan dk = detailKegiatan;
								if (dk == null) {
									dk = new DetailKegiatan();
								}
								dk.setBatalkanDenda(batalkanDenda.isChecked());
								dk.setKeterangan(detailBiaya == null ? "" : detailBiaya.getKeterangan());
								dk.setKegiatan(kegiatan);

								if (dk.getId() == null) {
									executeNativeSaveTransaction((GeneralValueObject) dk);
								} else {
									executeNativeUpdateTransaction((GeneralValueObject) dk);
								}

								if (pengaturanPembayaranBulanan != null) {
									Session session = null;
									Kegiatan k = null;
									try {
										session = HibernateUtil.currentNativeSession();
										k = (Kegiatan) session.createCriteria(Kegiatan.class)
												.add(Restrictions.idEq(kegiatan.getId())).uniqueResult();
									} catch (Exception ex) {
										ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:1337");
									} finally {
										if (session != null && session.isOpen()) {
											try {
												session.disconnect();
											} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:1342");
											}
											try {
												session.close();
											} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:1346");
											}
										}
										try {
											HibernateUtil.closeSession();
										} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:1351");
										}
									}

									if (k != null) {
										if (batalkanDenda.isChecked()) {
											String d = k.getPembatalanDenda() == null ? "" : k.getPembatalanDenda();
											d += "," + pengaturanPembayaranBulanan.getId() + ",";
											k.setPembatalanDenda(d);
										} else {
											String d = k.getPembatalanDenda() == null ? "" : k.getPembatalanDenda();
											d = org.apache.commons.lang3.StringUtils.replace(d,
													"," + pengaturanPembayaranBulanan.getId() + ",", "");
											k.setPembatalanDenda(d);
										}
										kegiatan.setPembatalanDenda(k.getPembatalanDenda());
										executeNativeUpdateTransaction((GeneralValueObject) k);
									}
								}

								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.clear(rowPembayaran);
										render(rowPembayaran, pengaturanPembayaranBulanan);
									}
								});
							}
						});
					}
				}

				if (isStaf) {
					final PengaturanPembayaranBulanan ppbFinal = pengaturanPembayaranBulanan;
					final Row rowFinal = rowPembayaran;
					Toolbarbutton btnHapus = new Toolbarbutton("Hapus Tagihan");
					btnHapus.setStyle("font-size:10px; color:#c00;");
					btnHapus.setVisible(false);
					btnHapus.setParent(vbox);
					rowPembayaran.setAttribute("btnHapus", btnHapus);
					btnHapus.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show(
									"Hapus tagihan ini dari sistem?\n"
											+ (ppbFinal.getDetailBiaya() == null
													|| ppbFinal.getDetailBiaya().getItemBiaya() == null ? "(Item Biaya tidak ditemukan)"
													: ppbFinal.getDetailBiaya().getItemBiaya().getNama())
											+ " " + ppbFinal.getNamaBulan(),
									"Konfirmasi Hapus", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
									MyMessageboxConfig.QUESTION, new EventListener() {
										@Override
										public void onEvent(Event ev) throws Exception {
											int ans = Integer.parseInt(ev.getData().toString());
											if (ans == MyMessageboxConfig.OK) {
												executeNativeDeleteTransaction(ppbFinal);
												rowFinal.setVisible(false);
											}
										}
									});
						}
					});
				// "Lihat Sumber": tagihan bulanan bersumber dari cakupan (cohort) pencarian di
				// layar "Pengaturan Tagihan Bulanan" -- buka layar tsb pra-filter ke kombinasi
				// Jenjang/Prodi/Angkatan/Semester/dll milik DetailBiaya sumber baris ini (pola
				// sama dg DaftarUlangMahasiswaBaruAction.java yg sudah lebih dulu memakainya).
				Toolbarbutton btnLihatSumberBulanan = new Toolbarbutton("Lihat Sumber");
				btnLihatSumberBulanan.setStyle("font-size:10px; color:#1d4ed8;");
				btnLihatSumberBulanan.setParent(vbox);
				btnLihatSumberBulanan.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						DetailBiaya sumberBulanan = ppbFinal.getDetailBiaya();
						if (sumberBulanan == null) {
							MyMessageboxConfig.show(
								"Sumber pengaturan tagihan bulanan untuk baris ini tidak ditemukan (kemungkinan data pengaturan tagihan bulanan sudah dihapus atau diubah oleh admin lain). "
									+ "Silakan periksa manual melalui menu Pengaturan Tagihan Bulanan. "
									+ "Jika Anda yakin seharusnya ada, hubungi Administrator/Pengembang Sistem dengan melampirkan tangkapan layar (screenshot) tagihan ini.",
								"Sumber Tidak Ditemukan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return;
						}
						try {
							String url = "/pages/master/detail_biaya_excel.zul?searchSemester="
									+ (sumberBulanan.getSemester() == null ? "-1" : sumberBulanan.getSemester())
									+ "&searchTahunAjaran=" + URLEncoder.encode(sumberBulanan.getTahunAkademik() == null ? "" : sumberBulanan.getTahunAkademik(), "UTF-8")
									+ "&labelAngkatan=" + (sumberBulanan.getAngkatan() == null ? "-1" : sumberBulanan.getAngkatan())
									+ "&searchMulaiBelajarDiSemester=" + URLEncoder.encode(sumberBulanan.getMulaiBelajarDiSemester() == null ? "" : sumberBulanan.getMulaiBelajarDiSemester(), "UTF-8")
									+ "&searchProgram=" + URLEncoder.encode(sumberBulanan.getProgram() == null ? "" : sumberBulanan.getProgram(), "UTF-8")
									+ "&searchJenjang=" + (sumberBulanan.getJurusan() == null ? "-1" : sumberBulanan.getJurusan().getJenjang().getId())
									+ "&searchJurusan=" + (sumberBulanan.getJurusan() == null ? "-1" : sumberBulanan.getJurusan().getId())
									+ "&searchStatusMahasiswa=" + (sumberBulanan.getStatusMahasiswa() == null ? "-1" : sumberBulanan.getStatusMahasiswa().getId())
									+ "&searchStatusAwalMahasiswa=" + (sumberBulanan.getStatusAwalMahasiswa() == null ? "-1" : sumberBulanan.getStatusAwalMahasiswa().getId())
									+ "&searchJenisKegiatan=" + (sumberBulanan.getJenisSeleksi() == null ? "-1" : sumberBulanan.getJenisSeleksi().getId())
									+ "&searchPaket=" + (sumberBulanan.getPaket() == null ? "-1" : sumberBulanan.getPaket().getId())
									// Langsung buka popup "Pengaturan Pembayaran Bulanan" (simulasi klik
									// tombol "Rencana Angsuran") begitu hasil pencarian ketemu, agar staf
									// langsung diarahkan ke tempat tagihan bulanan ini didefinisikan --
									// tidak perlu klik manual lagi (lihat NewDetailBiayaExcelAction.java).
									+ "&autoBukaRencanaAngsuran=1";
							Common.displayWindow(url, true);
						} catch (Exception e) {
							ais.common.PesanFormalHelper.tampilkanGagalException("membuka sumber tagihan bulanan", e,
								new String[] { "Coba ulangi beberapa saat lagi.",
									"Jika masih gagal, buka manual menu \"Pengaturan Tagihan Bulanan\" lalu cari kombinasi Jenjang/Prodi/Angkatan/Semester yang sesuai." });
						}
					}
				});
				}

				Hbox hbox = new Hbox();

				if (kegiatan != null && kegiatan.getMahasiswa() != null && detailBiaya != null
						&& detailBiaya.getItemBiaya() != null && kegiatan.getMahasiswa().getKelompokMahasiswa() != null
						&& kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtMulai() <= kegiatan.getSemster()
						&& kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtSampai() >= kegiatan.getSemster()
						&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa() != null
						&& !(detailKegiatan != null && detailKegiatan.adaDiskon())
						&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa()
								.getItemBiaya() != null
						&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().getItemBiaya()
								.getId().equals(detailBiaya.getItemBiaya().getId())) {

					RevisiHelper
							.createNewRevisi(JenisDiskonMahasiswa.class,
									kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa(),
									kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().getNama())
							.setParent(vbox);

				} else {
					if (kegiatan.getCalonMahasiswa() != null && kegiatan.getCalonMahasiswa().getJenisSeleksi() != null
							&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa() != null
							&& !(detailKegiatan != null && detailKegiatan.adaDiskon())
							&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getItemBiaya() != null
							&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getItemBiaya()
									.getId().equals(detailBiaya.getItemBiaya().getId())
							&& (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterMulai() == null
									|| (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterMulai() != null
											&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
													.getSemesterMulai() <= kegiatan.getSemster()))
							&& (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterSampai() == null
									|| (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterSampai() != null
											&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
													.getSemesterSampai() >= kegiatan.getSemster()))) {
						RevisiHelper.createNewRevisi(JenisDiskonMahasiswa.class,
								kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa(),
								kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getNama())
								.setParent(vbox);

					} else if (kegiatan.getMahasiswa() != null && kegiatan.getMahasiswa().getJenisSeleksi() != null
							&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa() != null
							&& !(detailKegiatan != null && detailKegiatan.adaDiskon())
							&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getItemBiaya() != null
							&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getItemBiaya()
									.getId().equals(detailBiaya.getItemBiaya().getId())
							&& (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterMulai() == null
									|| (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterMulai() != null
											&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
													.getSemesterMulai() <= kegiatan.getSemster()))
							&& (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterSampai() == null
									|| (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterSampai() != null
											&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
													.getSemesterSampai() >= kegiatan.getSemster()))) {
						RevisiHelper
								.createNewRevisi(JenisDiskonMahasiswa.class,
										kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa(),
										kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getNama())
								.setParent(vbox);

					} else if (detailKegiatan != null && detailKegiatan.adaDiskon()
							&& detailKegiatan.getDiskonMahasiswaData() != null
							&& detailKegiatan.getDiskonMahasiswaData().getJenisDiskonMahasiswa() != null) {
						RevisiHelper
								.createNewRevisi(DetailKegiatan.class, detailKegiatan,
										detailKegiatan.getDiskonMahasiswaData().getJenisDiskonMahasiswa().getNama())
								.setParent(hbox);
					}
				}

				RevisiHelper.createNewRevisi(PengaturanPembayaranBulanan.class, pengaturanPembayaranBulanan, "T")
						.setParent(hbox);
				hbox.setParent(vbox);
				prosesPembayaran.setParent(hbox);

				final Textbox uraian = new Textbox(
						detailKegiatan == null || detailKegiatan.getUraian() == null ? "" : detailKegiatan.getUraian());
				uraian.setCols(15);
				uraian.setRows(3);
				uraian.setVisible(false);
				uraian.setParent(vbox);

				final MyDatebox tgl = new MyDatebox(
						detailKegiatan == null ? (detailBiaya == null ? null : detailBiaya.getDefaultTanggalTagihan())
								: detailKegiatan.getTanggal());
				tgl.setFormat(Common.dateFormat.get().toPattern());
				tgl.setCols(10);
				tgl.setVisible(false);
				tgl.setReadonly(false);

				if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getTanggalTagihanRegistrasi() != null
						&& kegiatan.getJenisKegiatan() != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
						&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()
								.equals(kegiatan.getJenisKegiatan().getId())) {
					new Label(Common.dateFormat1.get().format(
							kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getTanggalTagihanRegistrasi()));
				} else if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getTanggalTagihanDaftarUlang() != null
						&& kegiatan.getJenisKegiatan() != null
						&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
						&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()
								.equals(kegiatan.getJenisKegiatan().getId())) {
					new Label(Common.dateFormat1.get().format(
							kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getTanggalTagihanDaftarUlang()));
				} else {
					tgl.setParent(vbox);
				}

				final Hbox hbox2 = new Hbox();
				hbox2.setVisible(false);
				final MyDoublebox nilaiDendaCustom = new MyDoublebox(
						detailKegiatan == null ? 0.0 : detailKegiatan.getDendaCustom());
				nilaiDendaCustom.setCols(5);
				hbox2.appendChild(nilaiDendaCustom);
				nilaiDendaCustom.setDisabled(detailKegiatan == null || !detailKegiatan.getMenggunakanDendaCustom());

				final MyCheckboxConfig dendaKustom = new MyCheckboxConfig("Denda Kustom");
				dendaKustom.setChecked(detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom());
				dendaKustom.setStyle("font-size:8px;");
				hbox2.appendChild(dendaKustom);

				final MyCheckboxConfig bukanTagihan = new MyCheckboxConfig("Tidak Aktif");
				bukanTagihan.setChecked(detailKegiatan != null && !detailKegiatan.getAktif());
				bukanTagihan.setStyle("font-size:8px;");
				hbox2.appendChild(bukanTagihan);

				hbox2.setParent(vbox);

				rowPembayaran.setAttribute("uraian", uraian);
				rowPembayaran.setAttribute("tgl", tgl);
				rowPembayaran.setAttribute("ket", ket);
				rowPembayaran.setAttribute("detailKegiatan", detailKegiatan);

				nilaiDendaCustom.setVisible(detailKegiatan == null || (!detailKegiatan.getBukanTagihan()));
				dendaKustom.setVisible(detailKegiatan == null || (!detailKegiatan.getBukanTagihan()));

				final EventListener eventListenerCustom = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (kegiatan != null && kegiatan.getId() != null) {
							DetailKegiatan dk = detailKegiatan;
							if (dk == null) {
								dk = new DetailKegiatan();
							}
							dk.setMenggunakanDendaCustom(dendaKustom.isChecked());
							dk.setDendaCustom(nilaiDendaCustom.getValue());
							dk.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
							dk.setUraian(uraian.getValue());
							dk.setDetailBiaya(detailBiaya);
							dk.setTanggal(tgl.getValue());
							dk.setTanggalCustom(tgl.getValue());
							dk.setKeterangan(detailBiaya.getKeterangan());
							dk.setAktif(!bukanTagihan.isChecked());
							dk.setKegiatan(kegiatan);

							if (dk.getId() == null) {
								executeNativeSaveTransaction((GeneralValueObject) dk);
							} else {
								executeNativeUpdateTransaction((GeneralValueObject) dk);
							}

							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
											: kegiatan.ambilDetailKegiatan(true);
									Common.clear(rowPembayaran);
									render(rowPembayaran, pengaturanPembayaranBulanan);
								}
							});
						}
					}
				};

				dendaKustom.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						DetailKegiatan dk = detailKegiatan;
						if (dk == null) {
							dk = new DetailKegiatan();
						}
						dk.setMenggunakanDendaCustom(dendaKustom.isChecked());
						dk.setDendaCustom(nilaiDendaCustom.getValue());

						if (dk.getId() == null) {
							executeNativeSaveTransaction((GeneralValueObject) dk);
						} else {
							executeNativeUpdateTransaction((GeneralValueObject) dk);
						}

						nilaiDendaCustom
								.setDisabled(detailKegiatan == null || !detailKegiatan.getMenggunakanDendaCustom());
					}
				});
				nilaiDendaCustom.addEventListener("onChange", eventListenerCustom);

				final MyToolbarbuttonConfig toolbarbuttonedit = new MyToolbarbuttonConfig(
						dendaKustom.isVisible() ? "Tgl, Uraian, dan Denda" : "Tgl dan Uraian",
						"/img/svg/edit-box-line.svg");
				toolbarbuttonedit.setOrient("vertical");
				final MyToolbarbuttonConfig toolbarbuttonsave = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				toolbarbuttonsave.setOrient("vertical");

				toolbarbuttonedit.setStyle("font-size:9px;");
				toolbarbuttonsave.setStyle("font-size:9px;");

				toolbarbuttonedit.setParent(hbox);

				toolbarbuttonedit.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						uraian.setVisible(true);
						tgl.setVisible(true);
						hbox2.setVisible(true);
						toolbarbuttonsave.setVisible(true);
						toolbarbuttonedit.setVisible(false);
					}
				});

				final String d = desc;

				toolbarbuttonsave.setVisible(false);
				toolbarbuttonsave.setParent(hbox);
				toolbarbuttonsave.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						uraian.setVisible(false);
						tgl.setVisible(false);
						hbox2.setVisible(false);
						toolbarbuttonedit.setVisible(true);
						toolbarbuttonsave.setVisible(false);
						ket.setValue(d + " " + uraian.getValue());
						eventListenerCustom.onEvent(arg0);
					}
				});

			} else {
				Label ket;
				(ket = new Label(detailBiaya.getItemBiaya().getKode() + " " + desc)).setParent(rowPembayaran);
				rowPembayaran.setAttribute("ket", ket);
			}

			DetailKegiatan.populatePembayaran(null, pengaturanPembayaranBulanan, kegiatan, jml);

		} else {
			dibayar.setParent(rowPembayaran);
			rowPembayaran.setAttribute("dibayar", dibayar);

			kurang.setParent(rowPembayaran);
			rowPembayaran.setAttribute("kurang", kurang);
			return;
		}

		if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {

			if (tbmuser == null || tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null
					|| !detailBiaya.getItemBiaya().getNilaiBisaDiubah()
					|| (detailKegiatan != null && detailKegiatan.getKunci() != null)) {

				Vbox vbox = new Vbox();
				vbox.setAlign("right");
				vbox.setWidth("100%");
				vbox.setParent(rowPembayaran);

				Label tag;
				(tag = new Label(Common.numberFormat.get().format(-Math.abs(jml)))).setParent(vbox);
				tag.setWidth("100%");
				rowPembayaran.setAttribute("tag", tag);

				tampilkanNominalAsliSebelumDiskon(vbox, rowPembayaran, jml, detailKegiatan, pengaturanPembayaranBulanan,
						detailBiaya, true);

				DetailPembayaranMahasiswaRenderer.tampilkanKunci(vbox, detailKegiatan, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.clear(rowPembayaran);
						render(rowPembayaran, pengaturanPembayaranBulanan);
					}
				}, tbmuser);

				prosesPembayaran.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event a) throws Exception {
						double kekurangan = 0.0;
						try {
							kekurangan = Common.numberFormat.get().parse(kurang.getValue()).doubleValue();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:1712");
						}
						masukkanNilai(-Math.abs(kekurangan), detailBiaya, pengaturanPembayaranBulanan);
						hitungUlang();
					}
				});

			} else {

				Vbox vbox = new Vbox();
				vbox.setAlign("right");
				vbox.setWidth("100%");
				vbox.setParent(rowPembayaran);

				final MyDoubleboxMin doubleboxMin = new MyDoubleboxMin(-Math.abs(jml));
				if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
					doubleboxMin.setDisabled(true);
				}

				rowPembayaran.setAttribute("tag", doubleboxMin);
				doubleboxMin.setAttribute("itemBiaya", detailBiaya);
				doubleboxMin.setWidth("90%");
				doubleboxMin.setParent(vbox);
				doubleboxMin.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						buatBaruJikaBelumAda();
						if (kegiatan != null && kegiatan.getId() != null) {
							DetailKegiatan dk = detailKegiatan;
							if (dk == null) {
								dk = new DetailKegiatan();
							}
							dk.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
							dk.setBiaya(doubleboxMin.getValue());
							dk.setDetailBiaya(detailBiaya);
							dk.setKeterangan(detailBiaya.getKeterangan());
							dk.setKegiatan(kegiatan);

							if (dk.getId() == null) {
								executeNativeSaveTransaction((GeneralValueObject) dk);
							} else {
								executeNativeUpdateTransaction((GeneralValueObject) dk);
							}
						}

						doubleboxMin
								.setValue(-Math.abs(doubleboxMin.getValue() == null ? 0.0 : doubleboxMin.getValue()));
						DetailPembayaranMahasiswaRenderer.this.eventListener.onEvent(new Event("", null, kegiatan));

						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
										: kegiatan.ambilDetailKegiatan(true);
								ubahWarnaStatus(cicilanPembayarans);
							}
						});
					}
				});
				pengurangan.add(doubleboxMin);

				DetailPembayaranMahasiswaRenderer.tampilkanKunci(vbox, detailKegiatan, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.clear(rowPembayaran);
						render(rowPembayaran, pengaturanPembayaranBulanan);
					}
				}, tbmuser);

				prosesPembayaran.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event a) throws Exception {
						double kekurangan = 0.0;
						try {
							kekurangan = Common.numberFormat.get().parse(kurang.getValue()).doubleValue();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:1788");
						}
						masukkanNilai(kekurangan, detailBiaya, pengaturanPembayaranBulanan);
						hitungUlang();
					}
				});

				tampilkanNominalAsliSebelumDiskon(vbox, rowPembayaran, jml, detailKegiatan, pengaturanPembayaranBulanan,
						detailBiaya, true);
			}
		} else {

			if (tbmuser == null || tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null
					|| !detailBiaya.getItemBiaya().getNilaiBisaDiubah()
					|| (detailKegiatan != null && detailKegiatan.getKunci() != null)) {

				Vbox vbox = new Vbox();
				vbox.setWidth("100%");
				vbox.setAlign("right");
				vbox.setParent(rowPembayaran);

				Label tag;
				(tag = new Label(Common.numberFormat.get().format(jml))).setParent(vbox);
				tag.setWidth("100%");
				rowPembayaran.setAttribute("tag", tag);

				DetailPembayaranMahasiswaRenderer.tampilkanKunci(vbox, detailKegiatan, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.clear(rowPembayaran);
						render(rowPembayaran, pengaturanPembayaranBulanan);
					}
				}, tbmuser);

				prosesPembayaran.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event a) throws Exception {
						double kekurangan = 0.0;
						try {
							kekurangan = Common.numberFormat.get().parse(kurang.getValue()).doubleValue();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:1829");
						}
						masukkanNilai(kekurangan, detailBiaya, pengaturanPembayaranBulanan);
						hitungUlang();
					}
				});

				tampilkanNominalAsliSebelumDiskon(vbox, rowPembayaran, jml, detailKegiatan, pengaturanPembayaranBulanan,
						detailBiaya, false);

			} else {

				Vbox vbox = new Vbox();
				vbox.setAlign("right");
				vbox.setWidth("100%");
				vbox.setParent(rowPembayaran);

				final MyDoublebox nilai;
				(nilai = new MyDoublebox(jml)).setParent(vbox);
				if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
					nilai.setDisabled(true);
				}
				rowPembayaran.setAttribute("tag", nilai);
				nilai.setAttribute("itemBiaya", detailBiaya);
				nilai.setWidth("90%");
				nilai.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						buatBaruJikaBelumAda();
						if (kegiatan != null && kegiatan.getId() != null) {
							DetailKegiatan dk = detailKegiatan;
							if (dk == null) {
								dk = new DetailKegiatan();
							}
							dk.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
							dk.setBiaya(nilai.getValue());
							dk.setDetailBiaya(detailBiaya);
							dk.setKeterangan(detailBiaya.getKeterangan());
							dk.setKegiatan(kegiatan);

							if (dk.getId() == null) {
								executeNativeSaveTransaction((GeneralValueObject) dk);
							} else {
								executeNativeUpdateTransaction((GeneralValueObject) dk);
							}
						}

						DetailPembayaranMahasiswaRenderer.this.eventListener.onEvent(new Event("", null, kegiatan));

						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
										: kegiatan.ambilDetailKegiatan(true);
								ubahWarnaStatus(cicilanPembayarans);
							}
						});
					}
				});

				DetailPembayaranMahasiswaRenderer.tampilkanKunci(vbox, detailKegiatan, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.clear(rowPembayaran);
						render(rowPembayaran, pengaturanPembayaranBulanan);
					}
				}, tbmuser);

				prosesPembayaran.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event a) throws Exception {
						double kekurangan = 0.0;
						try {
							kekurangan = Common.numberFormat.get().parse(kurang.getValue()).doubleValue();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:1904");
						}
						masukkanNilai(kekurangan, detailBiaya, pengaturanPembayaranBulanan);
						hitungUlang();
					}
				});

				tampilkanNominalAsliSebelumDiskon(vbox, rowPembayaran, jml, detailKegiatan, pengaturanPembayaranBulanan,
						detailBiaya, false);
			}
		}

		dibayar.setParent(rowPembayaran);
		rowPembayaran.setAttribute("dibayar", dibayar);
		kurang.setParent(rowPembayaran);
		rowPembayaran.setAttribute("kurang", kurang);

		if (Common.bolehKonfigurasi("untuk_login_mahasiswa_tidak_ditampilkan_pilihan_pembayaran_detail", Konfigurasi.TIDAK_AKTIF)) {
			if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						prosesPembayaran.setVisible(false);
					}
				});
			}
		}
	}

	/**
	 * Delegasi ke {@link TagihanDiskonHelper#tampilkanNominalAsliSebelumDiskon} —
	 * logika dan JavaDoc lengkap ada di sana.
	 * Method ini dipertahankan agar keempat call-site di kelas ini tidak perlu diubah.
	 */
	private void tampilkanNominalAsliSebelumDiskon(Vbox vbox, Row row, Double jml, DetailKegiatan detailKegiatan,
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan, DetailBiaya detailBiaya, boolean negatif) {
		TagihanDiskonHelper.tampilkanNominalAsliSebelumDiskon(
				vbox, row, jml, detailKegiatan, pengaturanPembayaranBulanan, detailBiaya, negatif);
	}

	public static void tampilkanKunci(final Vbox vbox1, final DetailKegiatan detailKegiatan, final EventListener refrsh,
			final Tbmuser tbmuser) {
		try {
			if (detailKegiatan != null && tbmuser.getMahasiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& (detailKegiatan.getItemBiaya() != null && detailKegiatan.getItemBiaya().getNilaiBisaDiubah())) {

				final Toolbarbutton bukaKunciDetail = new ais.ui.util.MyToolbarbuttonConfig(
						detailKegiatan.getKunci() == null ? "" : detailKegiatan.getKunci().getUserNama(),
						"/img/svg/unlock.svg");
				final Toolbarbutton kunciDetail = new ais.ui.util.MyToolbarbuttonConfig(
						detailKegiatan.getKunci() == null ? "Kunci" : detailKegiatan.getKunci().getUserNama(),
						"/img/svg/lock.svg");

				final MyCheckboxConfig bukanTagihan = new MyCheckboxConfig("Bukan Tagihan");
				bukanTagihan.setChecked(detailKegiatan.getBukanTagihan());

				Hbox vbox = new Hbox();
				vbox.setParent(vbox1);

				bukaKunciDetail.setParent(vbox);
				kunciDetail.setParent(vbox);
				bukanTagihan.setParent(vbox);

				bukaKunciDetail.setStyle("font-size:8px;");
				kunciDetail.setStyle("font-size:8px;");
				bukanTagihan.setStyle("font-size:7px;");

				bukanTagihan.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(final Event event1) throws Exception {
						MyMessageboxConfig.show(
								"Apakah yakin ini " + (bukanTagihan.isChecked() ? "bukan" : "adalah") + " tagihan ?",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											detailKegiatan.setBukanTagihan(bukanTagihan.isChecked());
											executeNativeUpdateTransaction((GeneralValueObject) detailKegiatan);
											Common.createDefaultTimer(refrsh);
										}
									}
								});
					}
				});
				bukanTagihan.setVisible(detailKegiatan.getKunci() == null);

				kunciDetail.setTooltiptext("Klik untuk meng-kunci tagihan ini");

				if (detailKegiatan.getKunci() != null) {
					bukaKunciDetail.setTooltiptext(
							"Dikunci oleh " + detailKegiatan.getKunci().getUserId() + ", klik untuk membuka kunci");
				} else {
					bukaKunciDetail.setTooltiptext("klik untuk membuka kunci");
				}

				kunciDetail.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(final Event event1) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin mengunci tagihan ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											detailKegiatan.setBiayaTemporary(detailKegiatan.getBiaya());
											detailKegiatan.setKunci(tbmuser);
											executeNativeUpdateTransaction((GeneralValueObject) detailKegiatan);
											Common.createDefaultTimer(refrsh);
										}
									}
								});
					}
				});
				kunciDetail.setVisible(detailKegiatan.getKunci() == null && !detailKegiatan.getBukanTagihan());
				kunciDetail.setOrient("vertical");

				bukaKunciDetail.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(final Event event1) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin membuka kunci tagihan ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											detailKegiatan.setKunci(null);
											executeNativeUpdateTransaction((GeneralValueObject) detailKegiatan);
											Common.createDefaultTimer(refrsh);
										}
									}
								});
					}
				});
				bukaKunciDetail.setVisible(detailKegiatan.getKunci() != null);
				if (detailKegiatan.getKunci() != null) {
					bukaKunciDetail.setTooltiptext("Dikunci oleh " + detailKegiatan.getKunci().getUserId());
				}

				bukaKunciDetail.setOrient("vertical");
				kunciDetail.setOrient("vertical");
				bukaKunciDetail.setVisible(detailKegiatan.getKunci() != null);
				kunciDetail.setVisible(detailKegiatan.getKunci() == null);

				if (tbmuser != null && tbmuser.hakAkses() != null
						&& !tbmuser.hakAkses().getRoleId().equalsIgnoreCase("keu")) {
					bukaKunciDetail.setDisabled(tbmuser == null || detailKegiatan.getKunci() == null
							|| !detailKegiatan.getKunci().getUserId().equals(tbmuser.getUserId()));
					if (Common.getApakahAdmin()) {
						kunciDetail.setDisabled(false);
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2060");
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void buatBaruJikaBelumAda() {
		try {
			if (kegiatan == null || kegiatan.getId() == null) {
				if (biodataCalonMahasiswa != null) {
					kegiatan = new Kegiatan();
					kegiatan.setAmount(0.0);
					kegiatan.setJadwalPembayaran(jadwalPembayaran);
					kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
					kegiatan.setTahunAkademik(tahunAkademik);
					kegiatan.setSemster(semester);
					kegiatan.setProgram(biodataCalonMahasiswa.getProgram());
					kegiatan.setTanggal(tanggalValidasi);
					kegiatan.setValidated(1);
					kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
					// FIX NPE rutin: jadwalPembayaran nullable (lihat konstruktor & pemakaian
					// ternary null-safe lain di file ini) -- dereference berulang tanpa guard di
					// bawah bisa membatalkan pembuatan Kegiatan baru utk calon mahasiswa ini.
					// Ambil sekali ke variabel null-safe, jenisKegiatan null tetap diterima oleh
					// method2 di bawah (lihat pemanggilan lain di file ini yg juga kirim null).
					JenisKegiatan jenisKegiatan = jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan();
					kegiatan.setJenisKegiatan(jenisKegiatan);

					try {
						Collection mydetailBiayas = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(
								biodataCalonMahasiswa, jenisKegiatan,
								biodataCalonMahasiswa.getProdiLulus() == null ? biodataCalonMahasiswa.getProdi1()
										: biodataCalonMahasiswa.getProdiLulus(),
								semester, false);
						int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(null,
								biodataCalonMahasiswa, jenisKegiatan, semester, mydetailBiayas,
								false, true);
						if (countPengaturanBulanan > 0) {
							mydetailBiayas = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(
									biodataCalonMahasiswa, null, semester, jenisKegiatan,
									mydetailBiayas, false, true);
						}
						Double nilaiBiayaHarusDiBayars = 0.0;
						for (Object o : mydetailBiayas) {
							if (o instanceof DetailBiaya) {
								DetailBiaya detailBiaya = (DetailBiaya) o;
								nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
							} else if (o instanceof PengaturanPembayaranBulanan) {
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
								nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, null, semester,
										pengaturanPembayaranBulanan);
							}
						}
						kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

					executeNativeSaveTransaction((GeneralValueObject) kegiatan);
					detailKegiatans = kegiatan.ambilDetailKegiatan(true);

				} else if (mahasiswa != null) {
					kegiatan = new Kegiatan();
					kegiatan.setAmount(0.0);
					kegiatan.setJadwalPembayaran(jadwalPembayaran);
					kegiatan.setMahasiswa(mahasiswa);
					kegiatan.setTahunAkademik(tahunAkademik);
					kegiatan.setSemster(semester);
					kegiatan.setProgram(mahasiswa.getProgram());
					StatusMahasiswa statusMahasiswa = Common.currentStatus(mahasiswa).getStatusMahasiswa();
					// FIX NPE rutin: jadwalPembayaran nullable, sama seperti cabang
					// biodataCalonMahasiswa di atas. Fallback tanggal ke tanggalValidasi
					// (selalu terisi via konstruktor, lihat baris ~2088 utk cabang lain).
					JenisKegiatan jenisKegiatan = jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan();
					kegiatan.setTanggal(jadwalPembayaran == null ? tanggalValidasi : jadwalPembayaran.getStartDate());
					kegiatan.setValidated(1);
					kegiatan.setStatusMahasiswa(statusMahasiswa);
					kegiatan.setJenisKegiatan(jenisKegiatan);
					Double nilaiBiayaHarusDiBayars = 0.0;
					try {
						Collection mydetailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa,
								semester, jenisKegiatan, false);
						int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(null, mahasiswa,
								jenisKegiatan, semester, mydetailBiayas, false, true);
						if (countPengaturanBulanan > 0) {
							mydetailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester,
									jenisKegiatan, "-1", true);
						}
						for (Object o : mydetailBiayas) {
							if (o instanceof DetailBiaya) {
								DetailBiaya detailBiaya = (DetailBiaya) o;
								nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
							} else if (o instanceof PengaturanPembayaranBulanan) {
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
								nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, null, mahasiswa,
										semester, pengaturanPembayaranBulanan);
							}
						}
						kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

					executeNativeSaveTransaction((GeneralValueObject) kegiatan);
					detailKegiatans = kegiatan.ambilDetailKegiatan(true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2157");
		}
	} 

	public Double hitungUlang() {
		Rows rows = (Rows) currentGrid.getRows();
		Double nilaiBiayaHarusDiBayars = 0.0;
		Double telahDibayar = 0.0;
		if (rows != null && rows.getChildren() != null) {
			for (int i = 0; i < rows.getChildren().size(); i++) {
				Row myRow = (Row) rows.getChildren().get(i);
				Double nilaiBiayas = 0.0;
				Double nilaiDibayar = 0.0;
				try {
					Component component = (Component) myRow.getAttribute("tag");
					if (component != null) {
						if (component instanceof Label) {
							Label myLabel = (Label) component;
							nilaiBiayas = myLabel.getValue() == null || myLabel.getValue().trim().isEmpty() ? 0.0
									: Common.numberFormat.get().parse(myLabel.getValue()).doubleValue();
							if (nilaiBiayas > 0.0
									|| (kegiatan == null || !kegiatan.getJenisKegiatan().getAbaikanNilaiMinus())) {
								nilaiBiayaHarusDiBayars += nilaiBiayas;
							}
						} else if (component instanceof Doublebox) {
							Doublebox myLabel = (Doublebox) component;
							nilaiBiayas = (myLabel.getValue() == null ? 0.0 : (myLabel.getValue()));
							if (nilaiBiayas > 0.0
									|| (kegiatan == null || !kegiatan.getJenisKegiatan().getAbaikanNilaiMinus())) {
								nilaiBiayaHarusDiBayars += nilaiBiayas;
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2190");
				}

				try {
					Label myLabel = (Label) myRow.getAttribute("dibayar");
					nilaiDibayar = myLabel.getValue() == null || myLabel.getValue().trim().isEmpty() ? 0.0
							: Common.numberFormat.get().parse(myLabel.getValue()).doubleValue();
					if (nilaiBiayas > 0.0
							|| (kegiatan == null || !kegiatan.getJenisKegiatan().getAbaikanNilaiMinus())) {
						telahDibayar += nilaiDibayar;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2201");
				}

				try {
					Label kurang = (Label) myRow.getAttribute("kurang");
					Toolbarbutton toolbarbutton = (Toolbarbutton) myRow.getAttribute("add_item");
					// FIX NPE rutin: jadwalPembayaran nullable (lihat konstruktor & pemakaian
					// ternary lain di file ini, mis. baris ~415/765/829) -- dereference langsung
					// di sini tanpa guard MEMBATALKAN sisa blok (kurang.setValue + status warna)
					// utk baris ini setiap kali jadwalPembayaran null. null diperlakukan sbg
					// "tak ada pembatasan" (default visible), selaras pola kegiatan==null di atas.
					toolbarbutton.setVisible(jadwalPembayaran == null || jadwalPembayaran.getJenisKegiatan() == null
							|| !jadwalPembayaran.getJenisKegiatan().getTidakBolehMengangsur());
					kurang.setValue(Common.numberFormat.get().format(nilaiBiayas - nilaiDibayar));

					if (nilaiBiayas > 0.1) {
						boolean benar = (nilaiDibayar < -0.1 || nilaiDibayar > 0.1)
								&& (nilaiDibayar.intValue() >= nilaiBiayas.intValue()
										|| nilaiDibayar.intValue() == nilaiBiayas.intValue());

						if (benar) {
							/* Samakan dgn ubahWarnaStatus: warna SOLID + sclass status.
							 * rgba(...,0.4) semi-transparan tampak "belang" karena ZK
							 * menyalin style tr ke setiap td → 0.4 menumpuk 0.4 tak rata.
							 * CSS .ais-status-lunas memberi warna RATA (#f1f5f9) + aksen. */
							//myRow.setStyle("background-color: #f1f5f9;");
							//myRow.setSclass("ais-status-lunas");
							toolbarbutton.setVisible(false);
						} else if (nilaiDibayar != null && nilaiBiayas != null && nilaiBiayas > 0.1
								&& (nilaiDibayar < -0.1 || nilaiDibayar > 0.1)
								&& nilaiDibayar.intValue() < nilaiBiayas.intValue()) {
							//myRow.setStyle("background-color: #fef2f2;");
							//myRow.setSclass("ais-status-kurang");
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2230");
				}
			}

			labelFooterTagihan.setStyle("font-weight: bolder;text-align: right;");
			labelFooterTagihan.setValue(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars));
			labelFooterDibayar.setStyle("font-weight: bolder;text-align: right;");
			labelFooterDibayar.setValue(Common.numberFormat.get().format(telahDibayar));
			labelFooterKekurangan.setStyle("font-weight: bolder;text-align: right;");
			labelFooterKekurangan.setValue(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars - telahDibayar));
		}

		terbilang.setStyle("text-align: right;color:blue;font-weight: bolder;");
		terbilang.setValue("Tagihan : " + Common
				.kapitalAwalKata(IndonesianNumberToWords.convert(nilaiBiayaHarusDiBayars.longValue()) + " rupiah"));

		terbilangTagihan.setStyle("text-align: right;color:green;font-weight: bolder;");
		terbilangTagihan.setValue("Dibayar : "
				+ Common.kapitalAwalKata(IndonesianNumberToWords.convert(telahDibayar.longValue()) + " rupiah"));

		Double sisa = nilaiBiayaHarusDiBayars - telahDibayar;
		if (sisa >= 0.0) {
			terbilangSisa.setStyle("text-align: right;color:red;font-weight: bolder;");
			terbilangSisa.setValue("Kekurangan :  "
					+ Common.kapitalAwalKata(IndonesianNumberToWords.convert(Math.abs(sisa.longValue())) + " rupiah"));
		} else {
			terbilangSisa.setStyle("text-align: right;color:brown;font-weight: bolder;");
			terbilangSisa.setValue("Kelebihan :  "
					+ Common.kapitalAwalKata(IndonesianNumberToWords.convert(Math.abs(sisa.longValue())) + " rupiah"));
		}

		Double persen = ((telahDibayar == null ? 0.0 : telahDibayar) * 100.0)
				/ (nilaiBiayaHarusDiBayars == null ? 0.0 : nilaiBiayaHarusDiBayars);
		terbilangSisaPersen.setStyle("text-align: right;color:brown;font-weight: bolder;");
		terbilangSisaPersen.setValue("Persen dibayar :  " + Common.numberFormat.get().format(persen) + "%");

		try {
			if (kegiatan != null && kegiatan.getId() != null && kegiatan.getJenisKegiatan() != null
					&& (kegiatan.getAmount().intValue() != telahDibayar.intValue()
							|| kegiatan.getAmountTerhutang().intValue() != sisa.intValue())) {
				kegiatan.setTanggal(tanggalValidasi);
				kegiatan.setAmount(telahDibayar);
				kegiatan.setAmountTerhutang(sisa);
				executeNativeUpdateTransaction((GeneralValueObject) kegiatan);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2276");
		}

		return nilaiBiayaHarusDiBayars;
	}

	@SuppressWarnings("unchecked")
	private void masukkanNilai(Double nilai, DetailBiaya detailBiaya,
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {

		if (pengaturanPembayaranBulanan != null && pengaturanPembayaranBulanan.getId() != null) {
			if (bul.contains(pengaturanPembayaranBulanan.getId())) {
				try {
					MyMessageboxConfig.show("Tagihan ini sudah Anda pilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2292");
				}
				return;
			} else {
				bul.add(pengaturanPembayaranBulanan.getId());
			}
		} else if (detailBiaya != null && detailBiaya.getId() != null && detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getId() != null) {
			if (det.contains(detailBiaya.getId())) {
				try {
					MyMessageboxConfig.show("Tagihan ini sudah Anda pilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2305");
				}
				return;
			} else {
				det.add(detailBiaya.getId());
			}
		}

		boolean tidakBolehUbah = (tbmuser != null
				&& (tbmuser.getBiodataCalonMahasiswa() != null || tbmuser.getMahasiswa() != null)
				&& detailBiaya.getItemBiaya() != null && !detailBiaya.getItemBiaya().getMahasiswaBolehMencicilkan())
				|| (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getMahasiswa() == null
						&& detailBiaya.getItemBiaya() != null
						&& !detailBiaya.getItemBiaya().getAdminBolehMencicilkan());

		// FIX NPE rutin: jadwalPembayaran nullable, lihat penjelasan di hitungUlang().
		if (jadwalPembayaran != null && jadwalPembayaran.getJenisKegiatan() != null
				&& jadwalPembayaran.getJenisKegiatan().getTidakBolehMengangsur()) {
			tidakBolehUbah = true;
		}

		boolean ditemukan = false;
		Rows rows = gridCicilan.getRows();
		List<Row> listRow = rows.getChildren();
		for (Row row : listRow) {
			MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");

			if (jumlahCicilan != null
					&& (jumlahCicilan.getValue() == null || jumlahCicilan.getValue().intValue() == 0)) {
				Clients.scrollIntoView(row);
				row.setVisible(true);
				MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
				MyDatebox tanggalKwitansi = (MyDatebox) row.getAttribute("tanggalKwitansi");
				Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
				Combobox myCaraBayar = (Combobox) row.getAttribute("caraBayar");
				Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
						&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan") : null);

				List<Comboitem> comboitems = myItemBiaya.getChildren();

				for (Comboitem comboitem : comboitems) {
					Object val = comboitem.getValue();
					if (val instanceof PengaturanPembayaranBulanan) {
						if (pengaturanPembayaranBulanan != null) {
							PengaturanPembayaranBulanan bulanan = (PengaturanPembayaranBulanan) val;
							if (bulanan.getId().equals(pengaturanPembayaranBulanan.getId())) {
								myItemBiaya.setSelectedItem(comboitem);
								jumlahCicilan.setValue(nilai);
								tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
								tanggalKwitansi.setValue(ais.ui.util.WaktuUtil.getDate());
								if (!myCaraBayar.getChildren().isEmpty()) {
									myCaraBayar.setSelectedIndex(0);
								}

								EventListener itemBiayaEventListener = (EventListener) myItemBiaya
										.getAttribute("itemBiayaEventListener");
								try {
									itemBiayaEventListener.onEvent(new Event("", null, bulanan));
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}

								tanggal.setDisabled(false);
								tanggalKwitansi.setDisabled(false);
								jumlahCicilan.setValue(nilai);
								keterangan.setDisabled(false);
								myCaraBayar.setDisabled(false);
								ditemukan = true;
								break;
							}
						} else {
							PengaturanPembayaranBulanan bulanan = (PengaturanPembayaranBulanan) val;
							if (bulanan.getDetailBiaya().getItemBiaya().getId()
									.equals(detailBiaya.getItemBiaya().getId())) {
								myItemBiaya.setSelectedItem(comboitem);
								tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
								tanggalKwitansi.setValue(ais.ui.util.WaktuUtil.getDate());
								if (!myCaraBayar.getChildren().isEmpty()) {
									myCaraBayar.setSelectedIndex(0);
								}

								EventListener itemBiayaEventListener = (EventListener) myItemBiaya
										.getAttribute("itemBiayaEventListener");
								try {
									itemBiayaEventListener.onEvent(new Event("", null, bulanan));
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}

								tanggalKwitansi.setDisabled(false);
								tanggal.setDisabled(false);
								jumlahCicilan.setValue(nilai);
								keterangan.setDisabled(false);
								myCaraBayar.setDisabled(false);
								ditemukan = true;
								break;
							}
						}
					} else if (val instanceof DetailBiaya) {
						DetailBiaya detailBiayaData = (DetailBiaya) val;
						if (detailBiayaData.getId().equals(detailBiaya.getId())) {
							myItemBiaya.setSelectedItem(comboitem);
							jumlahCicilan.setValue(nilai);
							tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
							tanggalKwitansi.setValue(ais.ui.util.WaktuUtil.getDate());
							if (!myCaraBayar.getChildren().isEmpty()) {
								myCaraBayar.setSelectedIndex(0);
							}

							EventListener itemBiayaEventListener = (EventListener) myItemBiaya
									.getAttribute("itemBiayaEventListener");
							try {
								itemBiayaEventListener.onEvent(new Event("", null, detailBiayaData));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2416");
							}

							EventListener jumlahCicilanEventListener = (EventListener) jumlahCicilan
									.getAttribute("jumlahCicilanEventListener");
							try {
								jumlahCicilanEventListener.onEvent(null);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}

							jumlahCicilan.disabledPaksa(tidakBolehUbah);
							tanggalKwitansi.setDisabled(false);
							tanggal.setDisabled(false);
							jumlahCicilan.setValue(nilai);
							keterangan.setDisabled(false);
							myCaraBayar.setDisabled(false);
							ditemukan = true;
							break;
						}
					}
				}
				break;
			}
		}

		if (!ditemukan) {
			for (Row row : listRow) {
				MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");

				if (jumlahCicilan != null
						&& (jumlahCicilan.getValue() == null || jumlahCicilan.getValue().intValue() == 0)) {
					row.setVisible(true);
					Clients.scrollIntoView(row);
					MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
					MyDatebox tanggalKwitansi = (MyDatebox) row.getAttribute("tanggalKwitansi");
					Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
					Combobox myCaraBayar = (Combobox) row.getAttribute("caraBayar");
					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
							&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
									: null);

					if (pengaturanPembayaranBulanan != null) {
						Common.selectComboItem(true, myItemBiaya, pengaturanPembayaranBulanan);
						myItemBiaya.setDisabled(true);

						tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
						tanggalKwitansi.setValue(ais.ui.util.WaktuUtil.getDate());
						if (!myCaraBayar.getChildren().isEmpty()) {
							myCaraBayar.setSelectedIndex(0);
						}

						EventListener itemBiayaEventListener = (EventListener) myItemBiaya
								.getAttribute("itemBiayaEventListener");
						try {
							itemBiayaEventListener.onEvent(null);
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

						tanggalKwitansi.setDisabled(false);
						tanggal.setDisabled(false);
						keterangan.setDisabled(false);
						myCaraBayar.setDisabled(false);
						jumlahCicilan.setValue(nilai);
						break;
					} else if (detailBiaya != null) {
						Common.selectComboItem(true, myItemBiaya, detailBiaya);
						myItemBiaya.setDisabled(true);

						jumlahCicilan.setValue(nilai);
						tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
						tanggalKwitansi.setValue(ais.ui.util.WaktuUtil.getDate());
						if (!myCaraBayar.getChildren().isEmpty()) {
							myCaraBayar.setSelectedIndex(0);
						}

						EventListener itemBiayaEventListener = (EventListener) myItemBiaya
								.getAttribute("itemBiayaEventListener");
						try {
							itemBiayaEventListener.onEvent(null);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2497");
						}

						EventListener jumlahCicilanEventListener = (EventListener) jumlahCicilan
								.getAttribute("jumlahCicilanEventListener");
						try {
							jumlahCicilanEventListener.onEvent(null);
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

						jumlahCicilan.disabledPaksa(tidakBolehUbah);
						tanggalKwitansi.setDisabled(false);
						tanggal.setDisabled(false);
						jumlahCicilan.setValue(nilai);
						keterangan.setDisabled(false);
						myCaraBayar.setDisabled(false);
						break;
					}
				}
			}
		}
	}
	
	
	
	/**
	 * Merender satu panel pusat Analisis Tagihan dan Pembayaran. Seluruh grafik lama berbasis
	 * Grafik dibuat memakai HTML/CSS ringan agar tampilan cepat dibuka, hemat memori,
	 * dan tetap kompatibel Java 1.7/ZKoss 5.5.
	 *
	 * @param cicilans daftar riwayat pembayaran mahasiswa/calon mahasiswa
	 * @param parent   komponen ZK tempat panel analisis ditempelkan
	 */
	public void tampilDasboard(List<CicilanPembayaran> cicilans, Component parent) {
		try {
			Common.clear(parent);

			Kegiatan kegiatanDashboard = this.kegiatan;
			if (cicilans != null && !cicilans.isEmpty()) {
				CicilanPembayaran cicilanPertama = cicilans.get(0);
				if (cicilanPertama != null && cicilanPertama.getKegiatan() != null) {
					kegiatanDashboard = cicilanPertama.getKegiatan();
				}
			}
			try {
				if (kegiatanDashboard != null && kegiatanDashboard.getId() != null) {
					Common.refresh(kegiatanDashboard);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2546");
			}

			DashboardPembayaranData data = hitungDashboardPembayaran(cicilans, kegiatanDashboard);
			Vbox mainContainer = new Vbox();
			mainContainer.setWidth("100%");
			mainContainer.setStyle("padding:10px 0 4px 0;box-sizing:border-box;overflow:auto;");
			mainContainer.setParent(parent);

			Html htmlDashboard = new Html();
			htmlDashboard.setContent(buildAnalisisPembayaranHtml(data, Common.isMobile()));
			htmlDashboard.setParent(mainContainer);

			tampilPanelJurnalPembayaranMahasiswa(cicilans, kegiatanDashboard, mainContainer);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public void pasangPanelAnalisisPembayaran(final List<CicilanPembayaran> cicilans, final Row rowUtama) {
		if (rowUtama == null || rowUtama.getParent() == null) {
			return;
		}
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Row rowDashboard = new Row();
				rowUtama.getParent().appendChild(rowDashboard);
				try {
					hitungUlang();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				tampilDasboard(cicilans, rowDashboard);
			}
		});
	}

	private void tampilPanelJurnalPembayaranMahasiswa(List<CicilanPembayaran> cicilans, Kegiatan kegiatanDashboard,
			Component parent) {
		try {
			if (parent == null) {
				return;
			}

			List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
			List<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans = new ArrayList<PengaturanPembayaranBulanan>();
			kumpulkanSumberAkunJurnalDariGrid(detailBiayas, pengaturanPembayaranBulanans);

			Vbox panel = new Vbox();
			panel.setWidth("100%");
			panel.setStyle("margin:0 0 10px 0;padding:13px;border:1px solid #dbe4f0;border-radius:14px;background:#ffffff;box-shadow:0 1px 5px rgba(15,23,42,0.08);box-sizing:border-box;");
			panel.setParent(parent);

			Html header = new Html();
			header.setContent("<div style='display:flex;align-items:flex-start;justify-content:space-between;gap:10px;margin-bottom:10px;font-family:Arial, sans-serif;'>"
					+ "<div><div style='font-size:15px;font-weight:700;color:#0f172a;'>Preview Jurnal Akuntansi Pembayaran</div>"
					+ "<div style='font-size:11px;color:#64748b;margin-top:2px;'>Debet berasal dari cara bayar atau tabungan. Kredit berasal dari akun item biaya, piutang, dibayar dimuka, pendapatan, dan denda jika ada.</div></div>"
					+ "<div style='font-size:11px;font-weight:700;color:#1d4ed8;background:#eff6ff;border:1px solid #bfdbfe;border-radius:999px;padding:5px 11px;white-space:nowrap;'>Preview</div>"
					+ "</div>");
			header.setParent(panel);

			Grid gridJurnal = GrupTransaksi.tampilkanJurnalPembayaranMahasiswa(cicilans, detailBiayas,
					pengaturanPembayaranBulanans, kegiatanDashboard);
			gridJurnal.setWidth("100%");
			gridJurnal.setHeight("280px");
			gridJurnal.setParent(panel);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	private void kumpulkanSumberAkunJurnalDariGrid(List<DetailBiaya> detailBiayas,
			List<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans) {
		if (currentGrid == null || currentGrid.getRows() == null) {
			return;
		}
		try {
			List<Row> rows = currentGrid.getRows().getChildren();
			for (int i = 0; i < rows.size(); i++) {
				Row row = rows.get(i);
				if (row == null) {
					continue;
				}

				PengaturanPembayaranBulanan bulanan = null;
				try {
					bulanan = (PengaturanPembayaranBulanan) row.getAttribute("pengaturanPembayaranBulanan");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2635");
				}
				if (bulanan != null) {
					tambahPengaturanPembayaranBulananUntukJurnal(pengaturanPembayaranBulanans, bulanan);
					try {
						tambahDetailBiayaUntukJurnal(detailBiayas, bulanan.getDetailBiaya());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2641");
					}
				}

				DetailBiaya detailBiaya = null;
				try {
					detailBiaya = (DetailBiaya) row.getAttribute("myValue");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2648");
				}
				if (detailBiaya == null) {
					try {
						DetailKegiatan detailKegiatan = (DetailKegiatan) row.getAttribute("detailKegiatan");
						detailBiaya = detailKegiatan == null ? null : detailKegiatan.getDetailBiaya();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2654");
					}
				}
				tambahDetailBiayaUntukJurnal(detailBiayas, detailBiaya);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void tambahDetailBiayaUntukJurnal(List<DetailBiaya> list, DetailBiaya detailBiaya) {
		if (list == null || detailBiaya == null) {
			return;
		}
		Long id = null;
		try {
			id = detailBiaya.getId();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2671");
		}
		for (int i = 0; i < list.size(); i++) {
			DetailBiaya lama = list.get(i);
			try {
				if (id != null && lama != null && id.equals(lama.getId())) {
					return;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2679");
			}
		}
		list.add(detailBiaya);
	}

	private void tambahPengaturanPembayaranBulananUntukJurnal(List<PengaturanPembayaranBulanan> list,
			PengaturanPembayaranBulanan bulanan) {
		if (list == null || bulanan == null) {
			return;
		}
		Long id = null;
		try {
			id = bulanan.getId();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2693");
		}
		for (int i = 0; i < list.size(); i++) {
			PengaturanPembayaranBulanan lama = list.get(i);
			try {
				if (id != null && lama != null && id.equals(lama.getId())) {
					return;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2701");
			}
		}
		list.add(bulanan);
	}

	private DashboardPembayaranData hitungDashboardPembayaran(List<CicilanPembayaran> cicilans,
			Kegiatan kegiatanDashboard) {
		DashboardPembayaranData data = new DashboardPembayaranData();
		data.tagihan = ambilAngkaLabel(labelFooterTagihan);
		data.dibayar = ambilAngkaLabel(labelFooterDibayar);
		data.sisa = ambilAngkaLabel(labelFooterKekurangan);

		try {
			if ((data.tagihan <= 0.0 || data.dibayar <= 0.0) && kegiatanDashboard != null) {
				double tagihanKegiatan = kegiatanDashboard.getTagihan();
				double dibayarKegiatan = kegiatanDashboard.getDibayar();
				if (data.tagihan <= 0.0 && tagihanKegiatan > 0.0) {
					data.tagihan = tagihanKegiatan;
				}
				if (data.dibayar <= 0.0 && dibayarKegiatan > 0.0) {
					data.dibayar = dibayarKegiatan;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2725");
		}

		if (data.tagihan <= 0.0 && dataTagihan != null && !dataTagihan.isEmpty()) {
			double totalTagihanMap = 0.0;
			for (Double nilai : dataTagihan.values()) {
				if (nilai != null) {
					totalTagihanMap += nilai.doubleValue();
				}
			}
			if (totalTagihanMap > 0.0) {
				data.tagihan = totalTagihanMap;
			}
		}

		if (data.sisa <= 0.0 && data.tagihan > data.dibayar) {
			data.sisa = data.tagihan - data.dibayar;
		}
		if (data.sisa < 0.0) {
			data.sisa = 0.0;
		}
		data.persenLunas = hitungPersen(data.dibayar, data.tagihan <= 0.0 ? data.dibayar + data.sisa : data.tagihan);

		if (cicilans == null) {
			cicilans = new ArrayList<CicilanPembayaran>();
		}

		SimpleDateFormat sdfTanggalSort = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat sdfTanggalTampil = new SimpleDateFormat("dd-MM-yyyy");
		for (int i = 0; i < cicilans.size(); i++) {
			CicilanPembayaran cp = cicilans.get(i);
			if (cp == null || cp.getId() == null) {
				continue;
			}
			double nilai = cp.getNilai() == null ? 0.0 : cp.getNilai().doubleValue();
			data.jumlahTransaksi++;
			data.totalRiwayat += nilai;

			Date tanggal = cp.getTanggal();
			String keyTanggal = tanggal == null ? "Tanpa Tanggal" : sdfTanggalSort.format(tanggal);
			String labelTanggal = tanggal == null ? "Tanpa Tanggal" : sdfTanggalTampil.format(tanggal);
			tambahNilai(data.trendHarian, keyTanggal, nilai);
			data.labelTrend.put(keyTanggal, labelTanggal);

			String keyItem = getNamaItemCicilan(cp);
			tambahNilai(data.komposisiItem, keyItem, nilai);

			String keyCaraBayar = getNamaCaraBayar(cp);
			tambahNilai(data.komposisiCaraBayar, keyCaraBayar, nilai);

			if (tanggal != null && (data.tanggalTerakhir == null || tanggal.after(data.tanggalTerakhir))) {
				data.tanggalTerakhir = tanggal;
				data.nominalTerakhir = nilai;
				data.caraTerakhir = keyCaraBayar;
			}
		}

		if (data.dibayar <= 0.0 && data.totalRiwayat > 0.0) {
			data.dibayar = data.totalRiwayat;
		}
		if (data.tagihan <= 0.0 && (data.dibayar + data.sisa) > 0.0) {
			data.tagihan = data.dibayar + data.sisa;
		}
		data.sisa = data.tagihan > data.dibayar ? data.tagihan - data.dibayar : 0.0;
		data.persenLunas = hitungPersen(data.dibayar, data.tagihan <= 0.0 ? data.dibayar + data.sisa : data.tagihan);

		data.rataRata = data.jumlahTransaksi == 0 ? 0.0 : data.totalRiwayat / data.jumlahTransaksi;
		isiDataTagihan(data, kegiatanDashboard);
		data.itemTerbesar = cariKeyTerbesar(data.komposisiItem);
		data.itemTerbesarNilai = ambilNilai(data.komposisiItem, data.itemTerbesar);
		data.caraTerbesar = cariKeyTerbesar(data.komposisiCaraBayar);
		data.caraTerbesarNilai = ambilNilai(data.komposisiCaraBayar, data.caraTerbesar);
		return data;
	}

	@SuppressWarnings("unchecked")
	private void isiDataTagihan(DashboardPembayaranData data, Kegiatan kegiatanDashboard) {
		if (data == null) {
			return;
		}
		List<Long> detailYangSudahDihitung = new ArrayList<Long>();
		List<Long> bulananYangSudahDihitung = new ArrayList<Long>();
		try {
			if (currentGrid != null && currentGrid.getRows() != null) {
				List<Row> rows = currentGrid.getRows().getChildren();
				for (Row row : rows) {
					if (row == null) {
						continue;
					}

					PengaturanPembayaranBulanan bulanan = null;
					try {
						bulanan = (PengaturanPembayaranBulanan) row.getAttribute("pengaturanPembayaranBulanan");
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2818");
					}
					if (bulanan != null) {
						DetailKegiatan detailKegiatanRow = null;
						try {
							detailKegiatanRow = (DetailKegiatan) row.getAttribute("detailKegiatan");
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2824");
						}
						tambahPengaturanBulananKeTagihan(data, bulanan, detailKegiatanRow, kegiatanDashboard,
								bulananYangSudahDihitung);
						continue;
					}

					DetailBiaya detailBiaya = null;
					try {
						detailBiaya = (DetailBiaya) row.getAttribute("myValue");
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2834");
					}
					tambahDetailBiayaKeTagihan(data, detailBiaya, kegiatanDashboard, detailYangSudahDihitung);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2839");
		}

		try {
			if ((data.komposisiTagihan == null || data.komposisiTagihan.isEmpty()) && detailKegiatans != null) {
				for (DetailKegiatan detailKegiatan : detailKegiatans) {
					if (detailKegiatan == null || detailKegiatan.getBukanTagihan()) {
						continue;
					}
					PengaturanPembayaranBulanan bulanan = null;
					try {
						bulanan = detailKegiatan.getPengaturanPembayaranBulanan();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2851");
					}
					if (bulanan != null) {
						tambahPengaturanBulananKeTagihan(data, bulanan, detailKegiatan, kegiatanDashboard,
								bulananYangSudahDihitung);
						continue;
					}
					DetailBiaya detailBiaya = null;
					try {
						detailBiaya = detailKegiatan.getDetailBiaya();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2861");
					}
					if (detailBiaya != null) {
						tambahDetailBiayaKeTagihan(data, detailBiaya, kegiatanDashboard, detailYangSudahDihitung);
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2868");
		}

		if ((data.komposisiTagihan == null || data.komposisiTagihan.isEmpty()) && dataTagihan != null
				&& !dataTagihan.isEmpty()) {
			for (Map.Entry<Long, Double> entry : dataTagihan.entrySet()) {
				if (entry != null && entry.getValue() != null) {
					tambahNilaiTagihanAtauBantuan(data, "Item " + entry.getKey(), entry.getValue().doubleValue());
				}
			}
		}

		data.totalTagihanItem = 0.0;
		for (Double nilai : data.komposisiTagihan.values()) {
			if (nilai != null && nilai.doubleValue() > 0.0) {
				data.totalTagihanItem += nilai.doubleValue();
			}
		}
		data.totalBantuanPotongan = 0.0;
		for (Double nilai : data.komposisiBantuanPotongan.values()) {
			if (nilai != null && nilai.doubleValue() < 0.0) {
				data.totalBantuanPotongan += Math.abs(nilai.doubleValue());
			}
		}
		if (data.totalTagihanItem <= 0.0 && data.tagihan > 0.0) {
			data.totalTagihanItem = data.tagihan;
			tambahNilai(data.komposisiTagihan, "Total Tagihan", data.tagihan);
		}
		if (data.tagihan <= 0.0 && data.totalTagihanItem > 0.0) {
			data.tagihan = data.totalTagihanItem;
		}
		data.jumlahItemTagihan = data.komposisiTagihan.size();
		data.jumlahItemBantuanPotongan = data.komposisiBantuanPotongan.size();
		data.tagihanTerbesar = cariKeyTerbesar(data.komposisiTagihan);
		data.tagihanTerbesarNilai = ambilNilai(data.komposisiTagihan, data.tagihanTerbesar);
		data.bantuanPotonganTerbesar = cariKeyTerbesarAbsolut(data.komposisiBantuanPotongan);
		data.bantuanPotonganTerbesarNilai = ambilNilai(data.komposisiBantuanPotongan, data.bantuanPotonganTerbesar);
		data.rataRataTagihan = data.jumlahItemTagihan == 0 ? 0.0 : data.totalTagihanItem / data.jumlahItemTagihan;
	}
	
	private boolean pengaturanBulananAktif(PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {
		try {
			return pengaturanPembayaranBulanan != null && Boolean.TRUE.equals(pengaturanPembayaranBulanan.getAktif());
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean pengaturanBulananMemangDitagihkan(PengaturanPembayaranBulanan pengaturanPembayaranBulanan,
			Mahasiswa mahasiswa, Integer semester) {
		try {
			if (!pengaturanBulananAktif(pengaturanPembayaranBulanan)) {
				return false;
			}
			Double nilai = hitungNominalBulananMurni(pengaturanPembayaranBulanan, mahasiswa, semester);
			if (!isZero(nilai)) {
				return true;
			}
			DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
			ItemBiaya itemBiaya = detailBiaya == null ? null : detailBiaya.getItemBiaya();
			if (itemBiaya != null && ItemBiaya.DIKALI_NILAI_MINUS.equals(itemBiaya.getPenghitungan())) {
				return true;
			}
			if (Boolean.TRUE.equals(pengaturanPembayaranBulanan.getTetapDitampilkanWalaupunNol())) {
				return true;
			}
			if (bolehTampilkanNolNilaiBisaDiubah() && itemBiaya != null && Boolean.TRUE.equals(itemBiaya.getNilaiBisaDiubah())) {
				return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2937");
		}
		return false;
	}
	
	private boolean bolehTampilkanNolNilaiBisaDiubah() {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(
					"tampilkan_pengaturan_bulanan_nol_nilai_bisa_diubah", Konfigurasi.TIDAK_AKTIF);
			return konfigurasi != null && Konfigurasi.AKTIF.equals(konfigurasi.getNilai());
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean isZero(Double value) {
		return value == null || Math.abs(value.doubleValue()) < 0.01;
	}
	
	private Double hitungNominalBulananMurni(PengaturanPembayaranBulanan pengaturanPembayaranBulanan,
			Mahasiswa mahasiswa, Integer semester) {
		Double nilai = Double.valueOf(0.0);
		if (!pengaturanBulananAktif(pengaturanPembayaranBulanan)) {
			return nilai;
		}

		try {
			Double nominalModifikasi = PembayaranNominalModifikasiHelper.ambilNominalModifikasi(
					pengaturanPembayaranBulanan, mahasiswa, semester);
			if (nominalModifikasi != null) {
				nilai = nominalModifikasi;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2969");
		}

		if (!isZero(nilai)) {
			return nilai;
		}

		try {
			Double nominalAsli = pengaturanPembayaranBulanan.getNominal();
			if (nominalAsli != null) {
				nilai = nominalAsli;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:2981");
		}

		return nilai == null ? Double.valueOf(0.0) : nilai;
	}

	private Double hitungTagihanBulananAman(DetailKegiatan detailKegiatan, DetailBiaya detailBiaya, Kegiatan kegiatan,
			Mahasiswa mahasiswa, Integer semester, PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {
		Double nilai = null;

		if (pengaturanPembayaranBulanan != null) {
			try {
				if (detailKegiatan != null && detailKegiatan.getBiaya() != null
						&& Math.abs(detailKegiatan.getBiaya().doubleValue()) > 0.01) {
					return detailKegiatan.getBiaya();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:hitungTagihanBulananAman-detail-custom");
			}

			if (!pengaturanBulananMemangDitagihkan(pengaturanPembayaranBulanan, mahasiswa, semester)) {
				return Double.valueOf(0.0);
			}

			nilai = hitungNominalBulananMurni(pengaturanPembayaranBulanan, mahasiswa, semester);
			if (!isZero(nilai)) {
				return nilai;
			}

			try {
				if (pengaturanPembayaranBulanan.getId() != null) {
					nilai = ambilNilaiDataTagihanById(pengaturanPembayaranBulanan.getId());
					if (nilai != null && !isZero(nilai)) {
						return nilai;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3008");
			}

			try {
				DetailBiaya detail = detailBiaya == null ? pengaturanPembayaranBulanan.getDetailBiaya() : detailBiaya;
				ItemBiaya itemBiaya = detail == null ? null : detail.getItemBiaya();
				if (itemBiaya != null && ItemBiaya.DIKALI_NILAI_MINUS.equals(itemBiaya.getPenghitungan())) {
					return nilai == null ? Double.valueOf(0.0) : nilai;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3017");
			}

			return nilai == null ? Double.valueOf(0.0) : nilai;
		}

		try {
			nilai = Kegiatan.ambilJumlahTagihan(detailKegiatan, detailBiaya, kegiatan, mahasiswa, semester,
					pengaturanPembayaranBulanan);
			if (nilai != null && Math.abs(nilai.doubleValue()) > 0.01) {
				return nilai;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3029");
		}

		try {
			nilai = Kegiatan.ambilJumlahTagihan(kegiatan, null, mahasiswa, semester, pengaturanPembayaranBulanan);
			if (nilai != null && Math.abs(nilai.doubleValue()) > 0.01) {
				return nilai;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3037");
		}

		try {
			nilai = Kegiatan.ambilJumlahTagihan(kegiatan, null, semester, pengaturanPembayaranBulanan);
			if (nilai != null && Math.abs(nilai.doubleValue()) > 0.01) {
				return nilai;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3045");
		}

		try {
			if (detailKegiatan != null && detailKegiatan.getBiaya() != null
					&& Math.abs(detailKegiatan.getBiaya().doubleValue()) > 0.01) {
				return detailKegiatan.getBiaya();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3053");
		}

		try {
			if (detailBiaya != null && detailBiaya.getId() != null) {
				nilai = ambilNilaiDataTagihanById(detailBiaya.getId());
				if (nilai != null) {
					return nilai;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3063");
		}

		return nilai == null ? Double.valueOf(0.0) : nilai;
	}
	
	private Double ambilNilaiDataTagihanById(Long id) {
		if (id == null || dataTagihan == null || dataTagihan.isEmpty()) {
			return null;
		}
		try {
			return dataTagihan.get(id);
		} catch (Exception e) {
			return null;
		}
	}


	private void tambahPengaturanBulananKeTagihan(DashboardPembayaranData data,
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan, DetailKegiatan detailKegiatan,
			Kegiatan kegiatanDashboard, List<Long> bulananYangSudahDihitung) {
		if (data == null || pengaturanPembayaranBulanan == null) {
			return;
		}
		try {
			if (pengaturanPembayaranBulanan.getId() != null && bulananYangSudahDihitung != null
					&& bulananYangSudahDihitung.contains(pengaturanPembayaranBulanan.getId())) {
				return;
			}
			if (pengaturanPembayaranBulanan.getId() != null && bulananYangSudahDihitung != null) {
				bulananYangSudahDihitung.add(pengaturanPembayaranBulanan.getId());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3095");
		}
		DetailBiaya detailBiaya = null;
		try {
			detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3100");
		}
		double nilai = hitungTagihanBulananAman(detailKegiatan, detailBiaya,
				kegiatanDashboard == null ? kegiatan : kegiatanDashboard, mahasiswa, semester, pengaturanPembayaranBulanan)
				.doubleValue();
		tambahNilaiTagihanAtauBantuan(data, getNamaItemPengaturanBulanan(pengaturanPembayaranBulanan), nilai);
	}

	private String getNamaItemPengaturanBulanan(PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {
		if (pengaturanPembayaranBulanan == null) {
			return "Tagihan Bulanan";
		}
		String nama = "";
		try {
			if (pengaturanPembayaranBulanan.getDetailBiaya() != null
					&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
					&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() != null) {
				nama = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3119");
		}
		try {
			if (isEmpty(nama) && pengaturanPembayaranBulanan.getKeterangan() != null
					&& pengaturanPembayaranBulanan.getKeterangan().trim().length() > 0) {
				nama = pengaturanPembayaranBulanan.getKeterangan().trim();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3126");
		}
		try {
			String bulan = pengaturanPembayaranBulanan.getNamaBulan();
			if (bulan != null && bulan.trim().length() > 0) {
				nama = (isEmpty(nama) ? "Tagihan Bulanan" : nama) + " - " + bulan.trim();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3133");
		}
		return isEmpty(nama) ? "Tagihan Bulanan" : nama;
	}


	private void tambahDetailBiayaKeTagihan(DashboardPembayaranData data, DetailBiaya detailBiaya,
			Kegiatan kegiatanDashboard, List<Long> detailYangSudahDihitung) {
		if (data == null || detailBiaya == null) {
			return;
		}
		try {
			if (detailBiaya.getId() != null && detailYangSudahDihitung != null
					&& detailYangSudahDihitung.contains(detailBiaya.getId())) {
				return;
			}
			if (detailBiaya.getId() != null && detailYangSudahDihitung != null) {
				detailYangSudahDihitung.add(detailBiaya.getId());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3152");
		}
		double nilai = ambilNilaiDetailTagihan(detailBiaya, kegiatanDashboard);
		tambahNilaiTagihanAtauBantuan(data, getNamaItemDetailBiaya(detailBiaya), nilai);
	}

	private double ambilNilaiDetailTagihan(DetailBiaya detailBiaya, Kegiatan kegiatanDashboard) {
		if (detailBiaya == null) {
			return 0.0;
		}

		Double nilaiDariGrid = ambilNilaiDataTagihan(detailBiaya);
		if (nilaiDariGrid != null && Math.abs(nilaiDariGrid.doubleValue()) > 0.01) {
			return nilaiDariGrid.doubleValue();
		}

		Double nilaiAsliDetail = ambilTotalAsliDetailBiaya(detailBiaya);
		if (nilaiAsliDetail != null && nilaiAsliDetail.doubleValue() < -0.01) {
			return nilaiAsliDetail.doubleValue();
		}

		try {
			if (kegiatanDashboard != null) {
				Double nilai = Kegiatan.ambilJumlahTagihan(kegiatanDashboard, detailBiaya);
				if (nilai != null && Math.abs(nilai.doubleValue()) > 0.01) {
					if (nilaiAsliDetail != null && nilaiAsliDetail.doubleValue() < -0.01
							&& nilai.doubleValue() > 0.0) {
						return nilaiAsliDetail.doubleValue();
					}
					return nilai.doubleValue();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3184");
		}

		return nilaiAsliDetail == null ? 0.0 : nilaiAsliDetail.doubleValue();
	}

	private Double ambilNilaiDataTagihan(DetailBiaya detailBiaya) {
		if (detailBiaya == null || detailBiaya.getId() == null || dataTagihan == null || dataTagihan.isEmpty()) {
			return null;
		}
		try {
			Double nilai = dataTagihan.get(detailBiaya.getId());
			if (nilai != null) {
				return nilai;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3199");
		}
		return null;
	}

	private Double ambilTotalAsliDetailBiaya(DetailBiaya detailBiaya) {
		if (detailBiaya == null) {
			return null;
		}
		try {
			Double nilai = detailBiaya.hitungTotal();
			return nilai;
		} catch (Exception e) {
			return null;
		}
	}

	private String getNamaItemDetailBiaya(DetailBiaya detailBiaya) {
		try {
			if (detailBiaya != null && detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getNama() != null) {
				return detailBiaya.getItemBiaya().getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3221");
		}
		try {
			if (detailBiaya != null && detailBiaya.getKeterangan() != null && detailBiaya.getKeterangan().trim().length() > 0) {
				return detailBiaya.getKeterangan().trim();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3227");
		}
		return "Lain-lain";
	}

	private String buildAnalisisPembayaranHtml(DashboardPembayaranData data, boolean mobile) {
		StringBuilder html = new StringBuilder();
		String status = data.persenLunas >= 100 ? "Lunas" : (data.persenLunas > 0 ? "Sebagian" : "Belum Bayar");
		String statusColor = data.persenLunas >= 100 ? "#15803d" : (data.persenLunas > 0 ? "#b45309" : "#b91c1c");
		String gridCol = mobile ? "1fr" : "repeat(4,minmax(130px,1fr))";
		String chartCol = mobile ? "1fr" : "1.25fr 1fr";

		html.append("<div style='margin:0 0 10px 0;padding:13px;border:1px solid #dbe4f0;border-radius:14px;background:#ffffff;box-shadow:0 1px 5px rgba(15,23,42,0.08);box-sizing:border-box;font-family:Arial, sans-serif;'>");
		html.append("<div style='display:flex;align-items:flex-start;justify-content:space-between;gap:10px;margin-bottom:12px;'>");
		html.append("<div><div style='font-size:15px;font-weight:700;color:#0f172a;'>Analisis Tagihan dan Pembayaran</div>");
		html.append("<div style='font-size:11px;color:#64748b;margin-top:2px;'>Ringkasan tagihan, pembayaran, dan sisa biaya agar petugas mudah memastikan status pembayaran sebelum transaksi disimpan.</div></div>");
		html.append("<div style='font-size:11px;font-weight:700;color:").append(statusColor).append(";background:#f8fafc;border:1px solid #e2e8f0;border-radius:999px;padding:5px 11px;white-space:nowrap;'>").append(status).append("</div>");
		html.append("</div>");

		html.append("<div style='display:grid;grid-template-columns:").append(gridCol).append(";gap:8px;margin-bottom:12px;'>");
		appendMetricCardHtml(html, "Total Tagihan", data.tagihan, "#eff6ff", "#1d4ed8");
		appendMetricCardHtml(html, "Total Pembayaran", data.dibayar, "#f0fdf4", "#15803d");
		appendMetricCardHtml(html, "Sisa Tagihan", data.sisa, "#fff7ed", "#c2410c");
		appendTextCardHtml(html, "Jumlah Transaksi", data.jumlahTransaksi + " Record", "#f8fafc", "#334155");
		html.append("</div>");

		appendProgressPelunasanHtml(html, data);
		appendRingkasanTagihanHtml(html, data, mobile);

		html.append("<div style='display:grid;grid-template-columns:").append(chartCol).append(";gap:10px;margin-top:12px;'>");
		appendTrendChartHtml(html, data);
		appendDistribusiItemHtml(html, data);
		html.append("</div>");

		html.append("<div style='display:grid;grid-template-columns:").append(chartCol).append(";gap:10px;margin-top:10px;'>");
		appendDistribusiCaraHtml(html, data);
		html.append(ais.ui.util.PembayaranDashboardHtmlUtil.buildSpiderWebOnly("Spider Pembayaran", "Perbandingan singkat antara tagihan, pembayaran, sisa, frekuensi, dan rata-rata transaksi.", data.tagihan, data.dibayar, data.sisa, data.totalRiwayat, data.jumlahTransaksi, data.rataRata));
		html.append("</div>");
		html.append("<div style='margin-top:10px;'>");
		appendInsightHtml(html, data);
		html.append("</div>");
		html.append("</div>");
		return html.toString();
	}

	private void appendMetricCardHtml(StringBuilder html, String label, double value, String background, String color) {
		html.append("<div style='background:").append(background).append(";border:1px solid rgba(15,23,42,0.06);border-radius:11px;padding:10px;box-sizing:border-box;min-height:86px;'>");
		html.append("<div style='font-size:10px;color:#64748b;font-weight:700;text-transform:uppercase;letter-spacing:.02em;'>").append(escapeHtml(label)).append("</div>");
		html.append("<div style='font-size:16px;color:").append(color).append(";font-weight:700;margin-top:4px;'>Rp ").append(formatNumber(value)).append("</div>");
		html.append("<div style='font-size:10px;color:#475569;margin-top:5px;line-height:1.35;'>").append(escapeHtml(formatTerbilangRupiah(value))).append("</div>");
		html.append("</div>");
	}

	private String formatTerbilangRupiah(double value) {
		try {
			String prefix = value < 0.0 ? "Minus " : "";
			return prefix + Common.kapitalAwalKata(
					IndonesianNumberToWords.convert(Math.abs((long) value)) + " rupiah");
		} catch (Exception e) {
			return "-";
		}
	}

	private void appendTextCardHtml(StringBuilder html, String label, String value, String background, String color) {
		html.append("<div style='background:").append(background).append(";border:1px solid rgba(15,23,42,0.06);border-radius:11px;padding:10px;box-sizing:border-box;'>");
		html.append("<div style='font-size:10px;color:#64748b;font-weight:700;text-transform:uppercase;letter-spacing:.02em;'>").append(escapeHtml(label)).append("</div>");
		html.append("<div style='font-size:16px;color:").append(color).append(";font-weight:700;margin-top:4px;'>").append(escapeHtml(value)).append("</div>");
		html.append("</div>");
	}

	private void appendProgressPelunasanHtml(StringBuilder html, DashboardPembayaranData data) {
		int persenDibayar = data.persenLunas;
		int persenSisa = hitungPersen(data.sisa, data.tagihan <= 0.0 ? data.dibayar + data.sisa : data.tagihan);
		html.append("<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:10px;margin-bottom:8px;'>");
		html.append("<div style='display:flex;justify-content:space-between;gap:8px;font-size:11px;color:#334155;font-weight:700;margin-bottom:6px;'>");
		html.append("<span>Komposisi Pelunasan</span><span>").append(persenDibayar).append("% dibayar</span></div>");
		html.append("<div style='height:16px;background:#e2e8f0;border-radius:999px;overflow:hidden;display:flex;'>");
		html.append("<div style='height:16px;width:").append(persenDibayar).append("%;background:#22c55e;'></div>");
		html.append("<div style='height:16px;width:").append(persenSisa).append("%;background:#fb923c;'></div>");
		html.append("</div>");
		html.append("<div style='display:flex;justify-content:space-between;gap:8px;font-size:10px;color:#64748b;margin-top:6px;'>");
		html.append("<span>Dibayar: Rp ").append(formatNumber(data.dibayar)).append("</span>");
		html.append("<span>Sisa: Rp ").append(formatNumber(data.sisa)).append("</span>");
		html.append("</div></div>");
	}

	private void appendRingkasanTagihanHtml(StringBuilder html, DashboardPembayaranData data, boolean mobile) {
		String gridCol = mobile ? "1fr" : "repeat(4,minmax(130px,1fr))";
		String chartCol = mobile ? "1fr" : "1.15fr .85fr";
		html.append("<div style='margin-top:12px;border-top:1px dashed #cbd5e1;padding-top:12px;'>");
		html.append("<div style='font-size:14px;font-weight:700;color:#0f172a;margin-bottom:3px;'>Ringkasan Tagihan</div>");
		html.append("<div style='font-size:10px;color:#64748b;margin-bottom:10px;'>Biaya minus seperti beasiswa, bantuan, potongan, atau koreksi dipisahkan dari tagihan utama agar tidak tampil sebagai tagihan terbesar.</div>");
		html.append("<div style='display:grid;grid-template-columns:").append(gridCol).append(";gap:8px;margin-bottom:10px;'>");
		appendTextCardHtml(html, "Jumlah Tagihan", data.jumlahItemTagihan + " Item", "#f8fafc", "#334155");
		appendTextCardHtml(html, "Tagihan Terbesar", isEmpty(data.tagihanTerbesar) ? "-" : shorten(data.tagihanTerbesar, 32), "#fff7ed", "#c2410c");
		appendMetricCardHtml(html, "Nilai Terbesar", data.tagihanTerbesarNilai, "#eff6ff", "#1d4ed8");
		appendMetricCardHtml(html, "Bantuan/Potongan", data.totalBantuanPotongan, "#f0fdf4", "#15803d");
		html.append("</div>");
		if (data.totalBantuanPotongan > 0.0) {
			html.append("<div style='margin-bottom:10px;background:#f0fdf4;border:1px solid #bbf7d0;border-radius:12px;padding:10px;box-sizing:border-box;'>");
			html.append("<div style='font-size:12px;font-weight:700;color:#166534;margin-bottom:3px;'>Bantuan, Beasiswa, Potongan, dan Koreksi Minus</div>");
			html.append("<div style='font-size:10px;color:#475569;line-height:1.5;'>Nilai minus tidak dihitung sebagai tagihan terbesar. Nilai tersebut ditampilkan sebagai pengurang tagihan agar petugas tidak keliru memprioritaskan pembayaran.</div>");
			appendHorizontalDistributionHtml(html, data.komposisiBantuanPotongan, data.totalBantuanPotongan, 5);
			html.append("</div>");
		}
		html.append("<div style='display:grid;grid-template-columns:").append(chartCol).append(";gap:10px;'>");
		appendDistribusiTagihanHtml(html, data);
		appendPrioritasTagihanHtml(html, data);
		html.append("</div>");
		html.append("<div style='display:grid;grid-template-columns:").append(chartCol).append(";gap:10px;margin-top:10px;'>");
		appendCatatanTagihanHtml(html, data);
		appendSpiderTagihanHtml(html, data);
		html.append("</div></div>");
	}

	private void appendDistribusiTagihanHtml(StringBuilder html, DashboardPembayaranData data) {
		html.append("<div style='background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;padding:11px;box-sizing:border-box;'>");
		html.append("<div style='font-size:13px;font-weight:700;color:#0f172a;margin-bottom:3px;'>Komposisi Tagihan</div>");
		html.append("<div style='font-size:10px;color:#64748b;margin-bottom:10px;'>Biaya positif dikelompokkan menurut item tagihan. Nilai minus ditampilkan terpisah sebagai bantuan/potongan.</div>");
		appendHorizontalDistributionHtml(html, data.komposisiTagihan, data.totalTagihanItem, 7);
		html.append("</div>");
	}

	private void appendPrioritasTagihanHtml(StringBuilder html, DashboardPembayaranData data) {
		html.append("<div style='background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;padding:11px;box-sizing:border-box;'>");
		html.append("<div style='font-size:13px;font-weight:700;color:#0f172a;margin-bottom:3px;'>Urutan Tagihan Terbesar</div>");
		html.append("<div style='font-size:10px;color:#64748b;margin-bottom:10px;'>Hanya biaya positif yang diurutkan. Beasiswa atau bantuan minus tidak menjadi tagihan terbesar.</div>");
		List<DashboardEntry> entries = toSortedEntries(data.komposisiTagihan);
		if (entries.isEmpty()) {
			appendEmptyStateHtml(html, "Belum ada tagihan yang bisa diurutkan.");
		} else {
			int batas = Math.min(entries.size(), 5);
			for (int i = 0; i < batas; i++) {
				DashboardEntry entry = entries.get(i);
				int persen = hitungPersen(entry.value, data.totalTagihanItem);
				html.append("<div style='display:flex;align-items:center;gap:9px;margin-bottom:9px;'>");
				html.append("<div style='width:28px;height:28px;line-height:28px;text-align:center;border-radius:999px;background:#eff6ff;color:#1d4ed8;font-weight:700;font-size:11px;'>").append(i + 1).append("</div>");
				html.append("<div style='flex:1;min-width:0;'><div style='display:flex;justify-content:space-between;gap:8px;font-size:10px;color:#334155;'>");
				html.append("<span style='font-weight:700;'>").append(escapeHtml(shorten(entry.label, 38))).append("</span><span>Rp ").append(formatNumber(entry.value)).append("</span></div>");
				html.append("<div style='height:7px;background:#f1f5f9;border-radius:999px;overflow:hidden;margin-top:4px;'>");
				html.append("<div style='height:7px;width:").append(persen).append("%;background:").append(getPaletteColor(i)).append(";border-radius:999px;'></div></div></div></div>");
			}
		}
		html.append("</div>");
	}

	private void appendCatatanTagihanHtml(StringBuilder html, DashboardPembayaranData data) {
		html.append("<div style='background:#f8fbff;border:1px solid #dbeafe;border-radius:12px;padding:11px;box-sizing:border-box;'>");
		html.append("<div style='font-size:13px;font-weight:700;color:#1e3a8a;margin-bottom:3px;'>Catatan Tagihan</div>");
		html.append("<div style='font-size:10px;color:#64748b;margin-bottom:10px;'>Ringkasan singkat sebelum memilih atau menyimpan pembayaran.</div>");
		html.append("<div style='font-size:11px;color:#334155;line-height:1.7;'>");
		if (data.totalTagihanItem <= 0.0) {
			html.append("Belum ada daftar tagihan yang dapat diringkas. Pilih mahasiswa, semester, atau jenis pembayaran terlebih dahulu.");
		} else {
			html.append("Ada <b>").append(data.jumlahItemTagihan).append("</b> item tagihan positif dengan total <b>Rp ").append(formatNumber(data.totalTagihanItem)).append("</b>.");
			html.append("<br/>Tagihan terbesar: <b>").append(escapeHtml(isEmpty(data.tagihanTerbesar) ? "-" : data.tagihanTerbesar)).append("</b> sebesar <b>Rp ").append(formatNumber(data.tagihanTerbesarNilai)).append("</b>.");
			if (data.totalBantuanPotongan > 0.0) {
				html.append("<br/>Bantuan/potongan minus: <b>Rp ").append(formatNumber(data.totalBantuanPotongan)).append("</b>");
				if (!isEmpty(data.bantuanPotonganTerbesar)) {
					html.append(" dari ").append(escapeHtml(shorten(data.bantuanPotonganTerbesar, 42)));
				}
				html.append(".");
			}
			html.append("<br/>Gunakan daftar tagihan di bawah untuk memastikan item yang dibayar sudah sesuai.");
		}
		html.append("</div></div>");
	}

	private void appendSpiderTagihanHtml(StringBuilder html, DashboardPembayaranData data) {
		double max = Math.max(Math.max(data.totalTagihanItem, data.tagihanTerbesarNilai), Math.max(Math.max(data.sisa, data.rataRataTagihan), data.totalBantuanPotongan));
		if (max <= 0.0) {
			max = 1.0;
		}
		int pTotal = hitungPersen(data.totalTagihanItem, max);
		int pTerbesar = hitungPersen(data.tagihanTerbesarNilai, max);
		int pSisa = hitungPersen(data.sisa, max);
		int pJumlah = data.jumlahItemTagihan <= 0 ? 0 : Math.min(100, data.jumlahItemTagihan * 12);
		int pRata = hitungPersen(data.rataRataTagihan, max);
		int pBantuan = hitungPersen(data.totalBantuanPotongan, max);
		html.append("<div style='background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;padding:11px;box-sizing:border-box;'>");
		html.append("<div style='font-size:13px;font-weight:700;color:#0f172a;margin-bottom:3px;'>Spider Tagihan</div>");
		html.append("<div style='font-size:10px;color:#64748b;margin-bottom:10px;'>Perbandingan cepat antara total tagihan positif, biaya terbesar, sisa, jumlah item, rata-rata, dan bantuan/potongan.</div>");
		html.append("<div style='display:grid;grid-template-columns:135px 1fr;gap:10px;align-items:center;'>");
		html.append("<div style='width:128px;height:128px;border-radius:50%;position:relative;background:repeating-radial-gradient(circle,#ffffff 0,#ffffff 17px,#e2e8f0 18px,#e2e8f0 19px),conic-gradient(from -90deg,#2563eb 0deg ").append(pTotal * 3.6).append("deg,#16a34a ").append(pTotal * 3.6).append("deg ").append((pTotal + pTerbesar) * 1.8).append("deg,#f97316 ").append((pTotal + pTerbesar) * 1.8).append("deg ").append(((pTotal + pTerbesar + pSisa) * 1.2)).append("deg,#7c3aed ").append(((pTotal + pTerbesar + pSisa) * 1.2)).append("deg 360deg);box-shadow:inset 0 0 0 1px #cbd5e1;'>");
		html.append("<div style='position:absolute;left:39px;top:39px;width:50px;height:50px;border-radius:50%;background:#fff;border:1px solid #e2e8f0;text-align:center;font-size:11px;color:#0f172a;font-weight:700;line-height:50px;'>").append(pSisa).append("%</div></div>");
		html.append("<div>");
		appendSpiderLegendMini(html, "Total", pTotal, "#2563eb");
		appendSpiderLegendMini(html, "Terbesar", pTerbesar, "#16a34a");
		appendSpiderLegendMini(html, "Sisa", pSisa, "#f97316");
		appendSpiderLegendMini(html, "Jumlah Item", pJumlah, "#7c3aed");
		appendSpiderLegendMini(html, "Rata-rata", pRata, "#0891b2");
		appendSpiderLegendMini(html, "Bantuan/Potongan", pBantuan, "#15803d");
		html.append("</div></div></div>");
	}

	private void appendSpiderLegendMini(StringBuilder html, String label, int percent, String color) {
		html.append("<div style='margin-bottom:7px;'>");
		html.append("<div style='display:flex;justify-content:space-between;font-size:10px;color:#334155;'>");
		html.append("<span><span style='display:inline-block;width:8px;height:8px;border-radius:50%;background:").append(color).append(";margin-right:5px;'></span>").append(escapeHtml(label)).append("</span><span>").append(percent).append("%</span></div>");
		html.append("<div style='height:7px;background:#f1f5f9;border-radius:999px;overflow:hidden;margin-top:3px;'>");
		html.append("<div style='height:7px;width:").append(percent).append("%;background:").append(color).append(";border-radius:999px;'></div></div></div>");
	}

	private void appendTrendChartHtml(StringBuilder html, DashboardPembayaranData data) {
		html.append("<div style='background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;padding:11px;box-sizing:border-box;'>");
		html.append("<div style='font-size:13px;font-weight:700;color:#0f172a;margin-bottom:3px;'>Tren Pembayaran per Tanggal</div>");
		html.append("<div style='font-size:10px;color:#64748b;margin-bottom:10px;'>Uang masuk dari tanggal ke tanggal.</div>");
		if (data.trendHarian.isEmpty()) {
			appendEmptyStateHtml(html, "Belum ada transaksi pembayaran yang bisa ditampilkan sebagai tren.");
		} else {
			double max = maxValue(data.trendHarian);
			html.append("<div style='height:190px;border-left:1px solid #cbd5e1;border-bottom:1px solid #cbd5e1;display:flex;align-items:flex-end;gap:8px;padding:8px 8px 0 8px;overflow-x:auto;background:linear-gradient(to top,#f8fafc,#ffffff);'>");
			int index = 0;
			for (Map.Entry<String, Double> entry : data.trendHarian.entrySet()) {
				int tinggi = max <= 0.0 ? 0 : (int) Math.round((Math.abs(entry.getValue()) / max) * 160.0);
				if (tinggi < 6 && Math.abs(entry.getValue()) > 0.0) {
					tinggi = 6;
				}
				String labelTanggal = data.labelTrend.get(entry.getKey());
				if (labelTanggal == null) {
					labelTanggal = entry.getKey();
				}
				html.append("<div style='min-width:54px;text-align:center;display:flex;flex-direction:column;align-items:center;justify-content:flex-end;'>");
				html.append("<div style='font-size:9px;color:#334155;margin-bottom:3px;white-space:nowrap;'>Rp ").append(formatNumber(entry.getValue())).append("</div>");
				html.append("<div title='").append(escapeHtml(labelTanggal)).append(" : Rp ").append(formatNumber(entry.getValue())).append("' style='width:28px;height:").append(tinggi).append("px;border-radius:7px 7px 0 0;background:").append(getPaletteColor(index)).append(";box-shadow:inset -3px 0 rgba(15,23,42,0.12);'></div>");
				html.append("<div style='font-size:9px;color:#64748b;margin-top:4px;white-space:nowrap;'>").append(escapeHtml(labelTanggal)).append("</div>");
				html.append("</div>");
				index++;
			}
			html.append("</div>");
		}
		html.append("</div>");
	}

	private void appendDistribusiItemHtml(StringBuilder html, DashboardPembayaranData data) {
		html.append("<div style='background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;padding:11px;box-sizing:border-box;'>");
		html.append("<div style='font-size:13px;font-weight:700;color:#0f172a;margin-bottom:3px;'>Distribusi Item Akademik</div>");
		html.append("<div style='font-size:10px;color:#64748b;margin-bottom:10px;'>Pembayaran masuk per item biaya.</div>");
		appendHorizontalDistributionHtml(html, data.komposisiItem, data.totalRiwayat, 6);
		html.append("</div>");
	}

	private void appendDistribusiCaraHtml(StringBuilder html, DashboardPembayaranData data) {
		html.append("<div style='background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;padding:11px;box-sizing:border-box;'>");
		html.append("<div style='font-size:13px;font-weight:700;color:#0f172a;margin-bottom:3px;'>Channel Pembayaran</div>");
		html.append("<div style='font-size:10px;color:#64748b;margin-bottom:10px;'>Uang masuk menurut cara bayar.</div>");
		appendHorizontalDistributionHtml(html, data.komposisiCaraBayar, data.totalRiwayat, 5);
		html.append("</div>");
	}

	private void appendHorizontalDistributionHtml(StringBuilder html, Map<String, Double> map, double total, int maxRows) {
		if (map == null || map.isEmpty()) {
			appendEmptyStateHtml(html, "Belum ada data distribusi.");
			return;
		}
		List<DashboardEntry> entries = toSortedEntries(map);
		int batas = Math.min(entries.size(), maxRows);
		for (int i = 0; i < batas; i++) {
			DashboardEntry entry = entries.get(i);
			double nilaiTampil = entry.value;
			int persen = hitungPersen(Math.abs(nilaiTampil), Math.abs(total));
			html.append("<div style='margin-bottom:9px;'>");
			html.append("<div style='display:flex;justify-content:space-between;gap:8px;font-size:10px;color:#334155;'>");
			html.append("<span style='font-weight:700;'>").append(escapeHtml(shorten(entry.label, 45))).append("</span><span>Rp ").append(formatNumber(entry.value)).append(" (" ).append(persen).append("%)</span></div>");
			html.append("<div style='height:9px;background:#f1f5f9;border-radius:999px;overflow:hidden;margin-top:3px;'>");
			html.append("<div style='height:9px;width:").append(persen).append("%;background:").append(getPaletteColor(i)).append(";border-radius:999px;'></div>");
			html.append("</div></div>");
		}
		if (entries.size() > maxRows) {
			html.append("<div style='font-size:10px;color:#64748b;'>+").append(entries.size() - maxRows).append(" item lain.</div>");
		}
	}

	private void appendInsightHtml(StringBuilder html, DashboardPembayaranData data) {
		html.append("<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:11px;box-sizing:border-box;'>");
		html.append("<div style='font-size:13px;font-weight:700;color:#0f172a;margin-bottom:8px;'>Insight Pembayaran</div>");
		html.append("<div style='display:grid;grid-template-columns:1fr;gap:7px;'>");
		appendInsightRowHtml(html, "Total Riwayat Masuk", "Rp " + formatNumber(data.totalRiwayat));
		appendInsightRowHtml(html, "Terbilang Tagihan", formatTerbilangRupiah(data.tagihan));
		appendInsightRowHtml(html, "Terbilang Dibayar", formatTerbilangRupiah(data.dibayar));
		appendInsightRowHtml(html, "Terbilang Sisa", formatTerbilangRupiah(data.sisa));
		appendInsightRowHtml(html, "Rata-rata Transaksi", "Rp " + formatNumber(data.rataRata));
		appendInsightRowHtml(html, "Tagihan Terbesar", isEmpty(data.tagihanTerbesar) ? "-" : shorten(data.tagihanTerbesar, 38) + " / Rp " + formatNumber(data.tagihanTerbesarNilai));
		appendInsightRowHtml(html, "Bantuan/Potongan", data.totalBantuanPotongan <= 0.0 ? "-" : "Rp " + formatNumber(data.totalBantuanPotongan) + (isEmpty(data.bantuanPotonganTerbesar) ? "" : " / " + shorten(data.bantuanPotonganTerbesar, 38)));
		appendInsightRowHtml(html, "Item Pembayaran Terbesar", isEmpty(data.itemTerbesar) ? "-" : shorten(data.itemTerbesar, 38) + " / Rp " + formatNumber(data.itemTerbesarNilai));
		appendInsightRowHtml(html, "Channel Dominan", isEmpty(data.caraTerbesar) ? "-" : shorten(data.caraTerbesar, 38) + " / Rp " + formatNumber(data.caraTerbesarNilai));
		String transaksiTerakhir = data.tanggalTerakhir == null ? "-" : Common.dateFormat1.get().format(data.tanggalTerakhir)
				+ " / Rp " + formatNumber(data.nominalTerakhir) + " / " + safe(data.caraTerakhir);
		appendInsightRowHtml(html, "Transaksi Terakhir", transaksiTerakhir);
		html.append("</div></div>");
	}

	private void appendInsightRowHtml(StringBuilder html, String label, String value) {
		html.append("<div style='display:flex;justify-content:space-between;gap:10px;border-bottom:1px dashed #cbd5e1;padding-bottom:5px;font-size:11px;'>");
		html.append("<span style='color:#64748b;'>").append(escapeHtml(label)).append("</span>");
		html.append("<span style='color:#0f172a;font-weight:700;text-align:right;'>").append(escapeHtml(value)).append("</span>");
		html.append("</div>");
	}

	private void appendEmptyStateHtml(StringBuilder html, String message) {
		html.append("<div style='padding:18px;text-align:center;color:#64748b;background:#f8fafc;border:1px dashed #cbd5e1;border-radius:10px;font-size:11px;'>");
		html.append(escapeHtml(message));
		html.append("</div>");
	}

	private double ambilAngkaLabel(Label label) {
		if (label == null || label.getValue() == null) {
			return 0.0;
		}
		String value = label.getValue().trim();
		if (value.length() == 0) {
			return 0.0;
		}
		try {
			return Common.numberFormat.get().parse(value).doubleValue();
		} catch (Exception e) {
			try {
				return Double.parseDouble(value.replace("Rp", "").replace(" ", "").replace(".", "").replace(",", "."));
			} catch (Exception ex) {
				return 0.0;
			}
		}
	}

	private String getNamaItemCicilan(CicilanPembayaran cp) {
		try {
			if (cp.getPengaturanPembayaranBulanan() != null
					&& cp.getPengaturanPembayaranBulanan().getDetailBiaya() != null
					&& cp.getPengaturanPembayaranBulanan().getDetailBiaya().getItemBiaya() != null
					&& cp.getPengaturanPembayaranBulanan().getDetailBiaya().getItemBiaya().getNama() != null) {
				return cp.getPengaturanPembayaranBulanan().getDetailBiaya().getItemBiaya().getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3559");
		}
		try {
			if (cp.getItemBiaya() != null && cp.getItemBiaya().getNama() != null) {
				return cp.getItemBiaya().getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3565");
		}
		try {
			if (cp.getKeterangan() != null && cp.getKeterangan().trim().length() > 0) {
				return cp.getKeterangan().trim();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3571");
		}
		return "Lain-lain";
	}

	private String getNamaCaraBayar(CicilanPembayaran cp) {
		try {
			if (cp.getJenisPembayaran() != null && cp.getJenisPembayaran().getNama() != null) {
				return cp.getJenisPembayaran().getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/DetailPembayaranMahasiswaRenderer.java:3581");
		}
		return "Tunai / Lainnya";
	}

	private void tambahNilai(Map<String, Double> map, String key, double value) {
		if (map == null) {
			return;
		}
		key = isEmpty(key) ? "Lain-lain" : key;
		Double lama = map.get(key);
		map.put(key, (lama == null ? 0.0 : lama.doubleValue()) + value);
	}

	private boolean pisahkanTagihanMinusSebagaiBantuan() {
		try {
			return Common.bolehKonfigurasi("pembayaran_dashboard_pisahkan_tagihan_minus");
		} catch (Exception e) {
			return true;
		}
	}

	private void tambahNilaiTagihanAtauBantuan(DashboardPembayaranData data, String label, double nilai) {
		if (data == null || Math.abs(nilai) < 0.01) {
			return;
		}
		if (nilai < 0.0 && pisahkanTagihanMinusSebagaiBantuan()) {
			tambahNilai(data.komposisiBantuanPotongan, label, nilai);
		} else if (nilai > 0.0) {
			tambahNilai(data.komposisiTagihan, label, nilai);
		}
	}

	private double ambilNilai(Map<String, Double> map, String key) {
		if (map == null || key == null || !map.containsKey(key) || map.get(key) == null) {
			return 0.0;
		}
		return map.get(key).doubleValue();
	}

	private String cariKeyTerbesar(Map<String, Double> map) {
		String key = null;
		double max = 0.0;
		if (map == null) {
			return null;
		}
		for (Map.Entry<String, Double> entry : map.entrySet()) {
			double nilai = entry.getValue() == null ? 0.0 : entry.getValue().doubleValue();
			if (nilai <= 0.0) {
				continue;
			}
			if (key == null || nilai > max) {
				key = entry.getKey();
				max = nilai;
			}
		}
		return key;
	}

	private String cariKeyTerbesarAbsolut(Map<String, Double> map) {
		String key = null;
		double max = 0.0;
		if (map == null) {
			return null;
		}
		for (Map.Entry<String, Double> entry : map.entrySet()) {
			double nilai = entry.getValue() == null ? 0.0 : Math.abs(entry.getValue().doubleValue());
			if (key == null || nilai > max) {
				key = entry.getKey();
				max = nilai;
			}
		}
		return key;
	}

	private double maxValue(Map<String, Double> map) {
		double max = 0.0;
		if (map == null) {
			return max;
		}
		for (Map.Entry<String, Double> entry : map.entrySet()) {
			double nilai = entry.getValue() == null ? 0.0 : Math.abs(entry.getValue().doubleValue());
			if (nilai > max) {
				max = nilai;
			}
		}
		return max;
	}

	private int hitungPersen(double value, double total) {
		if (total <= 0.0 || value <= 0.0) {
			return 0;
		}
		int persen = (int) Math.round((Math.abs(value) / Math.abs(total)) * 100.0);
		if (persen < 0) {
			return 0;
		}
		return persen > 100 ? 100 : persen;
	}

	private List<DashboardEntry> toSortedEntries(Map<String, Double> map) {
		List<DashboardEntry> entries = new ArrayList<DashboardEntry>();
		if (map == null) {
			return entries;
		}
		for (Map.Entry<String, Double> entry : map.entrySet()) {
			entries.add(new DashboardEntry(entry.getKey(), entry.getValue() == null ? 0.0 : entry.getValue().doubleValue()));
		}
		for (int i = 0; i < entries.size(); i++) {
			for (int j = i + 1; j < entries.size(); j++) {
				if (Math.abs(entries.get(j).value) > Math.abs(entries.get(i).value)) {
					DashboardEntry temp = entries.get(i);
					entries.set(i, entries.get(j));
					entries.set(j, temp);
				}
			}
		}
		return entries;
	}

	private String getPaletteColor(int index) {
		String[] colors = new String[] { "#2563eb", "#16a34a", "#f97316", "#dc2626", "#7c3aed", "#0891b2",
				"#db2777", "#65a30d", "#ca8a04", "#475569" };
		return colors[Math.abs(index) % colors.length];
	}

	private String formatNumber(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}

	private boolean isEmpty(String value) {
		return value == null || value.trim().length() == 0;
	}

	private String shorten(String value, int maxLength) {
		if (value == null) {
			return "";
		}
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, Math.max(0, maxLength - 3)) + "...";
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		String result = value;
		result = result.replace("&", "&amp;");
		result = result.replace("<", "&lt;");
		result = result.replace(">", "&gt;");
		result = result.replace("\"", "&quot;");
		result = result.replace("'", "&#39;");
		return result;
	}

	private static class DashboardEntry {
		private String label;
		private double value;

		private DashboardEntry(String label, double value) {
			this.label = label;
			this.value = value;
		}
	}

	private static class DashboardPembayaranData {
		private double tagihan;
		private double dibayar;
		private double sisa;
		private int persenLunas;
		private int jumlahTransaksi;
		private double totalRiwayat;
		private double rataRata;
		private int jumlahItemTagihan;
		private int jumlahItemBantuanPotongan;
		private double totalTagihanItem;
		private double totalBantuanPotongan;
		private double rataRataTagihan;
		private String tagihanTerbesar;
		private double tagihanTerbesarNilai;
		private String bantuanPotonganTerbesar;
		private double bantuanPotonganTerbesarNilai;
		private String itemTerbesar;
		private double itemTerbesarNilai;
		private String caraTerbesar;
		private double caraTerbesarNilai;
		private Date tanggalTerakhir;
		private double nominalTerakhir;
		private String caraTerakhir;
		private TreeMap<String, Double> trendHarian = new TreeMap<String, Double>();
		private Map<String, String> labelTrend = new HashMap<String, String>();
		private Map<String, Double> komposisiTagihan = new HashMap<String, Double>();
		private Map<String, Double> komposisiBantuanPotongan = new HashMap<String, Double>();
		private Map<String, Double> komposisiItem = new HashMap<String, Double>();
		private Map<String, Double> komposisiCaraBayar = new HashMap<String, Double>();
	}

}
