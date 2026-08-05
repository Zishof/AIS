package ais.action.report.format1.rab;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaPegawai;

public class RabReportHelper {

	private Integer maxrevisi;

	public RabReportHelper(Integer selectedTahun, SatuanKerja satuanKerja, SumberDana sumberDana) {
		maxrevisi = (Integer) HibernateUtil.currentSession().createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", selectedTahun)).setProjection(Projections.max("revisi"))
				.uniqueResult();
		maxrevisi = maxrevisi == null ? 1 : maxrevisi;
	}

	private String getStrings(Integer deep) {
		String d = "";
		for (int i = 0; i < deep; i++) {
			d += "   ";
		}
		return d;
	}

	public void generateRencanaAnggaran(final Long parentId, final WorkspaceTreeModel workspaceTreeModel,
			final Long root, List<Workspace> workspaces, List<Map<String, Object>> maps) {

		for (final Workspace workspace : workspaces) {
			if (workspace.getParentId() == null) {
				continue;
			}
			if (workspace.getParentId().equals(root)) {
				Boolean ada = !workspaceTreeModel.isLeaf(workspace);

				List<Long> longs = new ArrayList<Long>();

				workspaceTreeModel.getParentCount(parentId, workspace.getId(), workspace.getParentId(), longs);

				Integer deep = longs.size();

				longs = null;

				if (ada) {

					Map<String, Object> map = new java.util.HashMap<String, Object>();
					map.put("workspace_id", workspace.getId());
					map.put("unique_id", workspace.getId());
					map.put("kode", workspace.getKode() == null ? "" : workspace.getKode());
					map.put("nama", workspace.getNama() == null ? ""
							: (getStrings(deep) + workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
									: " - " + workspace.getUnitOrganisasi().getNama())));

					map.put("qty1", null);
					map.put("satuan1", null);
					map.put("qty2", null);
					map.put("satuan2", null);
					map.put("vol", null);
					map.put("kali", "");
					map.put("satuan_vol", null);
					map.put("harga_satuan", null);
					map.put("realisasi_total", null);
					map.put("persen", null);

					// workspaceTreeModel.checkHargaTotal(workspace);

					map.put("harga_total", workspace.getHargaTotal().equals(0.0) ? null : workspace.getHargaTotal());
					map.put("realisasi_total",
							(workspace.getRealisasiProses() == null || workspace.getRealisasiProses().equals(0.0))
									? null
									: workspace.getRealisasiProses());

					map.put("sisa_total", (workspace.getHargaTotal() == null ? 0.0 : workspace.getHargaTotal())
							- (workspace.getRealisasiProses() == null ? 0.0 : workspace.getRealisasiProses()));

					Double persen = (((workspace.getRealisasiProses() == null
							|| workspace.getRealisasiProses().equals(0.0)) ? 0.0 : workspace.getRealisasiProses())
							* 100)
							/ ((workspace.getHargaTotal() == null || workspace.getHargaTotal().equals(0.0)) ? 0.0
									: workspace.getHargaTotal());
					map.put("persen", persen);
					maps.add(map);
					generateRencanaAnggaran(parentId, workspaceTreeModel, workspace.getId(), workspaces, maps);
				} else {
					Map<String, Object> map = new java.util.HashMap<String, Object>();
					map.put("workspace_id", workspace.getId());
					map.put("unique_id", workspace.getId());
					map.put("kode", workspace.getKode() == null ? "" : workspace.getKode());
					map.put("nama", workspace.getNama() == null ? ""
							: (getStrings(deep) + workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
									: " - " + workspace.getUnitOrganisasi().getNama())));
					map.put("qty1", workspace.getQty().equals(0.0) ? null : workspace.getQty());
					map.put("satuan1", workspace.getSatuan() == null ? null : workspace.getSatuan().getNama());
					map.put("kali", "x");
					map.put("qty2", workspace.getJmlWaktu().equals(0.0) ? null : workspace.getJmlWaktu());
					map.put("satuan2", workspace.getSatuan1() == null ? null : workspace.getSatuan1().getNama());
					map.put("vol", workspace.getVolume().equals(0.0) ? null : workspace.getVolume());
					map.put("satuan_vol", workspace.getSatuanVolume() == null ? null : workspace.getSatuanVolume());
					map.put("harga_satuan", workspace.getHargaSatuan().equals(0.0) ? null : workspace.getHargaSatuan());

					// workspaceTreeModel.checkHargaTotal(workspace);

					map.put("harga_total",
							(workspace.getHargaTotal() == null || workspace.getHargaTotal().equals(0.0)) ? null
									: workspace.getHargaTotal());
					map.put("realisasi_total",
							(workspace.getRealisasiProses() == null || workspace.getRealisasiProses().equals(0.0))
									? null
									: workspace.getRealisasiProses());

					map.put("sisa_total", (workspace.getHargaTotal() == null ? 0.0 : workspace.getHargaTotal())
							- (workspace.getRealisasiProses() == null ? 0.0 : workspace.getRealisasiProses()));

					Double persen = (((workspace.getRealisasiProses() == null
							|| workspace.getRealisasiProses().equals(0.0)) ? 0.0 : workspace.getRealisasiProses())
							* 100)
							/ ((workspace.getHargaTotal() == null || workspace.getHargaTotal().equals(0.0)) ? 0.0
									: workspace.getHargaTotal());
					map.put("persen", persen);
					maps.add(map);
				}
			}
		}

	}

	public void generateRencanaTiapBulanAnggaran(final Long parentId, final WorkspaceTreeModel workspaceTreeModel,
			final Long root, List<Workspace> workspaces, Integer tahun, List<Map<String, Object>> maps) {

		for (final Workspace workspace : workspaces) {
			if (workspace.getParentId() == null) {
				continue;
			}
			if (workspace.getParentId().equals(root)) {
				List<Long> longs = new ArrayList<Long>();

				workspaceTreeModel.getParentCount(parentId, workspace.getId(), workspace.getParentId(), longs);

				Integer deep = longs.size();

				longs = null;

				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("workspace_id", workspace.getId());
				map.put("unique_id", workspace.getId());
				map.put("kode", workspace.getKode() == null ? "" : workspace.getKode());
				map.put("nama",
						workspace.getNama() == null ? ""
								: (getStrings(deep) + workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
										: " - " + workspace.getUnitOrganisasi().getNama())));

				map.put("harga_total", workspace.getHargaTotal().equals(0.0) ? null : workspace.getHargaTotal());

				Set<Long> childsId = new HashSet<Long>();
				workspaceTreeModel.generateChildsByIds(workspace.getId(), childsId);
				List<Object[]> numbers = workspaceTreeModel.getHargaTotalPerencanaanTiapBulan(childsId, tahun);

				for (Object[] myNumbers : numbers) {
					map.put("total", myNumbers[0] == null ? 0L : ((Number) myNumbers[0]).doubleValue());
					map.put("harga_total_1", myNumbers[1] == null ? 0L : ((Number) myNumbers[1]).doubleValue());
					map.put("harga_total_2", myNumbers[2] == null ? 0L : ((Number) myNumbers[2]).doubleValue());
					map.put("harga_total_3", myNumbers[3] == null ? 0L : ((Number) myNumbers[3]).doubleValue());
					map.put("harga_total_4", myNumbers[4] == null ? 0L : ((Number) myNumbers[4]).doubleValue());
					map.put("harga_total_5", myNumbers[5] == null ? 0L : ((Number) myNumbers[5]).doubleValue());
					map.put("harga_total_6", myNumbers[6] == null ? 0L : ((Number) myNumbers[6]).doubleValue());
					map.put("harga_total_7", myNumbers[7] == null ? 0L : ((Number) myNumbers[7]).doubleValue());
					map.put("harga_total_8", myNumbers[8] == null ? 0L : ((Number) myNumbers[8]).doubleValue());
					map.put("harga_total_9", myNumbers[9] == null ? 0L : ((Number) myNumbers[9]).doubleValue());
					map.put("harga_total_10", myNumbers[10] == null ? 0L : ((Number) myNumbers[10]).doubleValue());
					map.put("harga_total_11", myNumbers[11] == null ? 0L : ((Number) myNumbers[11]).doubleValue());
					map.put("harga_total_12", myNumbers[12] == null ? 0L : ((Number) myNumbers[12]).doubleValue());
				}

				maps.add(map);
				generateRencanaTiapBulanAnggaran(parentId, workspaceTreeModel, workspace.getId(), workspaces, tahun,
						maps);

			}
		}

	}

	public void generateRencanaTriWulanAnggaran(final Long parentId, final WorkspaceTreeModel workspaceTreeModel,
			final Long root, List<Workspace> workspaces, Integer tahun, List<Map<String, Object>> maps) {

		for (final Workspace workspace : workspaces) {
			if (workspace.getParentId() == null) {
				continue;
			}
			if (workspace.getParentId().equals(root)) {
				List<Long> longs = new ArrayList<Long>();

				workspaceTreeModel.getParentCount(parentId, workspace.getId(), workspace.getParentId(), longs);

				Integer deep = longs.size();

				longs = null;

				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("workspace_id", workspace.getId());
				map.put("unique_id", workspace.getId());
				map.put("kode", workspace.getKode() == null ? "" : workspace.getKode());
				map.put("nama",
						workspace.getNama() == null ? ""
								: (getStrings(deep) + workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
										: " - " + workspace.getUnitOrganisasi().getNama())));

				map.put("harga_total", workspace.getHargaTotal().equals(0.0) ? null : workspace.getHargaTotal());

				Set<Long> childsId = new HashSet<Long>();
				workspaceTreeModel.generateChildsByIds(workspace.getId(), childsId);
				List<Object[]> numbers = workspaceTreeModel.getHargaTotalPerencanaanTriWulan(childsId, tahun);

				for (Object[] myNumbers : numbers) {
					map.put("total", myNumbers[0] == null ? 0L : ((Number) myNumbers[0]).doubleValue());
					map.put("harga_total_1", myNumbers[1] == null ? 0L : ((Number) myNumbers[1]).doubleValue());
					map.put("harga_total_2", myNumbers[2] == null ? 0L : ((Number) myNumbers[2]).doubleValue());
					map.put("harga_total_3", myNumbers[3] == null ? 0L : ((Number) myNumbers[3]).doubleValue());
					map.put("harga_total_4", myNumbers[4] == null ? 0L : ((Number) myNumbers[4]).doubleValue());
				}

				maps.add(map);
				generateRencanaTriWulanAnggaran(parentId, workspaceTreeModel, workspace.getId(), workspaces, tahun,
						maps);

			}
		}

	}

	public void generateRencanaDanRealisasiTriWulanAnggaran(final Long parentId,
			final WorkspaceTreeModel workspaceTreeModel, final Long root, List<Workspace> workspaces, Integer tahun,
			List<Map<String, Object>> maps) {

		for (final Workspace workspace : workspaces) {
			if (workspace.getParentId() == null) {
				continue;
			}
			if (workspace.getParentId().equals(root)) {
				List<Long> longs = new ArrayList<Long>();

				workspaceTreeModel.getParentCount(parentId, workspace.getId(), workspace.getParentId(), longs);

				Integer deep = longs.size();

				longs = null;

				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("workspace_id", workspace.getId());
				map.put("unique_id", workspace.getId());
				map.put("kode", workspace.getKode() == null ? "" : workspace.getKode());
				map.put("nama",
						workspace.getNama() == null ? ""
								: (getStrings(deep) + workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
										: " - " + workspace.getUnitOrganisasi().getNama())));

				map.put("harga_total", workspace.getHargaTotal().equals(0.0) ? null : workspace.getHargaTotal());

				Set<Long> childsId = new HashSet<Long>();
				workspaceTreeModel.generateChildsByIds(workspace.getId(), childsId);
				List<Object[]> numbers = workspaceTreeModel.getHargaTotalRealisasiTriWulan(childsId, tahun);

				for (Object[] myNumbers : numbers) {
					map.put("total_realisasi", myNumbers[0] == null ? 0L : ((Number) myNumbers[0]).doubleValue());
					map.put("harga_total_1_realisasi",
							myNumbers[1] == null ? 0L : ((Number) myNumbers[1]).doubleValue());
					map.put("harga_total_2_realisasi",
							myNumbers[2] == null ? 0L : ((Number) myNumbers[2]).doubleValue());
					map.put("harga_total_3_realisasi",
							myNumbers[3] == null ? 0L : ((Number) myNumbers[3]).doubleValue());
					map.put("harga_total_4_realisasi",
							myNumbers[4] == null ? 0L : ((Number) myNumbers[4]).doubleValue());
				}

				numbers = workspaceTreeModel.getHargaTotalPerencanaanTriWulan(childsId, tahun);

				for (Object[] myNumbers : numbers) {
					map.put("total_rencana", myNumbers[0] == null ? 0L : ((Number) myNumbers[0]).doubleValue());
					map.put("harga_total_1_rencana", myNumbers[1] == null ? 0L : ((Number) myNumbers[1]).doubleValue());
					map.put("harga_total_2_rencana", myNumbers[2] == null ? 0L : ((Number) myNumbers[2]).doubleValue());
					map.put("harga_total_3_rencana", myNumbers[3] == null ? 0L : ((Number) myNumbers[3]).doubleValue());
					map.put("harga_total_4_rencana", myNumbers[4] == null ? 0L : ((Number) myNumbers[4]).doubleValue());
				}
				maps.add(map);
				generateRencanaDanRealisasiTriWulanAnggaran(parentId, workspaceTreeModel, workspace.getId(), workspaces,
						tahun, maps);

			}
		}

	}

	public void generateRealisasiTriWulanAnggaran(final Long parentId, final WorkspaceTreeModel workspaceTreeModel,
			final Long root, List<Workspace> workspaces, Integer tahun, List<Map<String, Object>> maps) {

		for (final Workspace workspace : workspaces) {
			if (workspace.getParentId() == null) {
				continue;
			}
			if (workspace.getParentId().equals(root)) {
				List<Long> longs = new ArrayList<Long>();

				workspaceTreeModel.getParentCount(parentId, workspace.getId(), workspace.getParentId(), longs);

				Integer deep = longs.size();

				longs = null;

				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("workspace_id", workspace.getId());
				map.put("unique_id", workspace.getId());
				map.put("kode", workspace.getKode() == null ? "" : workspace.getKode());
				map.put("nama",
						workspace.getNama() == null ? ""
								: (getStrings(deep) + workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
										: " - " + workspace.getUnitOrganisasi().getNama())));

				map.put("harga_total", workspace.getHargaTotal().equals(0.0) ? null : workspace.getHargaTotal());

				Set<Long> childsId = new HashSet<Long>();
				workspaceTreeModel.generateChildsByIds(workspace.getId(), childsId);
				List<Object[]> numbers = workspaceTreeModel.getHargaTotalRealisasiTriWulan(childsId, tahun);

				for (Object[] myNumbers : numbers) {
					map.put("total", myNumbers[0] == null ? 0L : ((Number) myNumbers[0]).doubleValue());
					map.put("harga_total_1", myNumbers[1] == null ? 0L : ((Number) myNumbers[1]).doubleValue());
					map.put("harga_total_2", myNumbers[2] == null ? 0L : ((Number) myNumbers[2]).doubleValue());
					map.put("harga_total_3", myNumbers[3] == null ? 0L : ((Number) myNumbers[3]).doubleValue());
					map.put("harga_total_4", myNumbers[4] == null ? 0L : ((Number) myNumbers[4]).doubleValue());
				}

				maps.add(map);
				generateRealisasiTriWulanAnggaran(parentId, workspaceTreeModel, workspace.getId(), workspaces, tahun,
						maps);

			}
		}

	}

	public void generateRealisasiTiapBulanAnggaran(final Long parentId, final WorkspaceTreeModel workspaceTreeModel,
			final Long root, List<Workspace> workspaces, Integer tahun, List<Map<String, Object>> maps) {

		for (final Workspace workspace : workspaces) {
			if (workspace.getParentId() == null) {
				continue;
			}
			if (workspace.getParentId().equals(root)) {
				List<Long> longs = new ArrayList<Long>();

				workspaceTreeModel.getParentCount(parentId, workspace.getId(), workspace.getParentId(), longs);

				Integer deep = longs.size();

				longs = null;

				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("workspace_id", workspace.getId());
				map.put("unique_id", workspace.getId());
				map.put("kode", workspace.getKode() == null ? "" : workspace.getKode());
				map.put("nama",
						workspace.getNama() == null ? ""
								: (getStrings(deep) + workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
										: " - " + workspace.getUnitOrganisasi().getNama())));

				map.put("harga_total", workspace.getHargaTotal().equals(0.0) ? null : workspace.getHargaTotal());

				Set<Long> childsId = new HashSet<Long>();
				workspaceTreeModel.generateChildsByIds(workspace.getId(), childsId);
				List<Object[]> numbers = workspaceTreeModel.getHargaTotalRealisasiTiapBulan(childsId, tahun);

				for (Object[] myNumbers : numbers) {
					map.put("total", myNumbers[0] == null ? 0L : ((Number) myNumbers[0]).doubleValue());
					map.put("harga_total_1", myNumbers[1] == null ? 0L : ((Number) myNumbers[1]).doubleValue());
					map.put("harga_total_2", myNumbers[2] == null ? 0L : ((Number) myNumbers[2]).doubleValue());
					map.put("harga_total_3", myNumbers[3] == null ? 0L : ((Number) myNumbers[3]).doubleValue());
					map.put("harga_total_4", myNumbers[4] == null ? 0L : ((Number) myNumbers[4]).doubleValue());
					map.put("harga_total_5", myNumbers[5] == null ? 0L : ((Number) myNumbers[5]).doubleValue());
					map.put("harga_total_6", myNumbers[6] == null ? 0L : ((Number) myNumbers[6]).doubleValue());
					map.put("harga_total_7", myNumbers[7] == null ? 0L : ((Number) myNumbers[7]).doubleValue());
					map.put("harga_total_8", myNumbers[8] == null ? 0L : ((Number) myNumbers[8]).doubleValue());
					map.put("harga_total_9", myNumbers[9] == null ? 0L : ((Number) myNumbers[9]).doubleValue());
					map.put("harga_total_10", myNumbers[10] == null ? 0L : ((Number) myNumbers[10]).doubleValue());
					map.put("harga_total_11", myNumbers[11] == null ? 0L : ((Number) myNumbers[11]).doubleValue());
					map.put("harga_total_12", myNumbers[12] == null ? 0L : ((Number) myNumbers[12]).doubleValue());
				}

				maps.add(map);
				generateRealisasiTiapBulanAnggaran(parentId, workspaceTreeModel, workspace.getId(), workspaces, tahun,
						maps);

			}
		}

	}

	public void generateRencanaAnggaran(final Long parentId, final WorkspaceTreeModel workspaceTreeModel,
			final Long root, List<Workspace> workspaces, List<Map<String, Object>> maps, Date mulai, Date sampai) {

		for (final Workspace workspace : workspaces) {

			if (workspace.getParentId() == null) {
				continue;
			}
			if (workspace.getParentId().equals(root)) {
				// System.out.println(workspace);
				Boolean ada = !workspaceTreeModel.isLeaf(workspace);

				List<Long> longs = new ArrayList<Long>();

				workspaceTreeModel.getParentCount(parentId, workspace.getId(), workspace.getParentId(), longs);

				Integer deep = longs.size();

				longs = null;

				if (ada) {

					Map<String, Object> map = new java.util.HashMap<String, Object>();
					map.put("workspace_id", workspace.getId());
					map.put("unique_id", workspace.getId());
					map.put("kode", workspace.getKode() == null ? "" : workspace.getKode());
					map.put("nama", workspace.getNama() == null ? ""
							: (getStrings(deep) + workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
									: " - " + workspace.getUnitOrganisasi().getNama())));

					map.put("qty1", null);
					map.put("satuan1", null);
					map.put("qty2", null);
					map.put("satuan2", null);
					map.put("vol", null);
					map.put("kali", "");
					map.put("satuan_vol", null);
					map.put("harga_satuan", null);
					map.put("realisasi_total", null);
					map.put("persen", null);

					// workspaceTreeModel.checkHargaTotal(workspace);

					Double harga_total = (workspace.getHargaTotal() == null ? 0.0 : workspace.getHargaTotal());
					Double realisasi_total_bulan_lalu = workspaceTreeModel.getRealisasi(workspace, mulai);
					Double realisasi_total_bulan_ini = workspaceTreeModel.getRealisasi(workspace, mulai, sampai);
					Double realisasi_total = workspaceTreeModel.getRealisasi(workspace, sampai);
					Double sisa_total = harga_total - (realisasi_total);
					Double persen_bulan_lalu = harga_total < 1.0 ? 0.0 : realisasi_total_bulan_lalu * 100 / harga_total;

					Double persen_bulan_ini = harga_total < 1.0 ? 0.0 : realisasi_total_bulan_ini * 100 / harga_total;
					Double persen_sisa = harga_total < 1.0 ? 0.0 : sisa_total * 100 / harga_total;
					Double persen = harga_total < 1.0 ? 0.0 : realisasi_total * 100 / harga_total;

					map.put("harga_total", harga_total);
					map.put("realisasi_total_bulan_lalu", realisasi_total_bulan_lalu);
					map.put("realisasi_total_bulan_ini", realisasi_total_bulan_ini);
					map.put("realisasi_total", realisasi_total);
					map.put("sisa_total", sisa_total);

					map.put("persen_bulan_lalu", persen_bulan_lalu);
					map.put("persen_bulan_ini", persen_bulan_ini);
					map.put("persen_sisa", persen_sisa);
					map.put("persen", persen);
					// System.out.println(map);
					maps.add(map);
					generateRencanaAnggaran(parentId, workspaceTreeModel, workspace.getId(), workspaces, maps, mulai,
							sampai);
				} else {
					Map<String, Object> map = new java.util.HashMap<String, Object>();
					map.put("workspace_id", workspace.getId());
					map.put("unique_id", workspace.getId());
					map.put("kode", workspace.getKode() == null ? "" : workspace.getKode());
					map.put("nama", workspace.getNama() == null ? ""
							: (getStrings(deep) + workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
									: " - " + workspace.getUnitOrganisasi().getNama())));
					map.put("qty1", workspace.getQty().equals(0.0) ? null : workspace.getQty());
					map.put("satuan1", workspace.getSatuan() == null ? null : workspace.getSatuan().getNama());
					map.put("kali", "x");
					map.put("qty2", workspace.getJmlWaktu().equals(0.0) ? null : workspace.getJmlWaktu());
					map.put("satuan2", workspace.getSatuan1() == null ? null : workspace.getSatuan1().getNama());
					map.put("vol", workspace.getVolume().equals(0.0) ? null : workspace.getVolume());
					map.put("satuan_vol", workspace.getSatuanVolume() == null ? null : workspace.getSatuanVolume());
					map.put("harga_satuan", workspace.getHargaSatuan().equals(0.0) ? null : workspace.getHargaSatuan());

					// workspaceTreeModel.checkHargaTotal(workspace);

					Double harga_total = (workspace.getHargaTotal() == null ? 0.0 : workspace.getHargaTotal());
					Double realisasi_total_bulan_lalu = workspaceTreeModel.getRealisasi(workspace, mulai);
					Double realisasi_total_bulan_ini = workspaceTreeModel.getRealisasi(workspace, mulai, sampai);
					Double realisasi_total = workspaceTreeModel.getRealisasi(workspace, sampai);
					Double sisa_total = harga_total - (realisasi_total);
					Double persen_bulan_lalu = harga_total < 1.0 ? 0.0 : realisasi_total_bulan_lalu * 100 / harga_total;

					Double persen_bulan_ini = harga_total < 1.0 ? 0.0 : realisasi_total_bulan_ini * 100 / harga_total;
					Double persen_sisa = harga_total < 1.0 ? 0.0 : sisa_total * 100 / harga_total;
					Double persen = harga_total < 1.0 ? 0.0 : realisasi_total * 100 / harga_total;

					map.put("harga_total", harga_total);
					map.put("realisasi_total_bulan_lalu", realisasi_total_bulan_lalu);
					map.put("realisasi_total_bulan_ini", realisasi_total_bulan_ini);
					map.put("realisasi_total", realisasi_total);
					map.put("sisa_total", sisa_total);

					map.put("persen_bulan_lalu", persen_bulan_lalu);
					map.put("persen_bulan_ini", persen_bulan_ini);
					map.put("persen_sisa", persen_sisa);
					map.put("persen", persen);
					// System.out.println(map);
					maps.add(map);
				}
			}
		}

	}

	public Double getHargaTotal(final WorkspaceTreeModel workspaceTreeModel, Workspace parent,
			Collection<Workspace> workspaces) {
		Set<Workspace> set = new HashSet<Workspace>();
		collectChilds(workspaceTreeModel, parent, workspaces, set);
		Double total = 0.0;
		for (Workspace workspace : set) {
			total += (workspace.getHargaTotal());
		}
		return 0.0;
	}

	public void collectChilds(final WorkspaceTreeModel workspaceTreeModel, Workspace parent,
			Collection<Workspace> workspaces, Set<Workspace> set) {
		for (Workspace workspace : workspaces) {
			if (workspace.getParentId() != null && workspace.getParentId().equals(parent.getId())
					&& workspaceTreeModel.isLeaf(workspace)) {
				set.add(workspace);
			} else if (!parent.getId().equals(workspace.getId())) {
				collectChilds(workspaceTreeModel, workspace, workspaces, set);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void generateJadwalAnggaran(final Long parentId, final WorkspaceTreeModel workspaceTreeModel,
			final Long root, final Boolean hanyaChild, Collection<Workspace> workspaces,
			List<Map<String, Object>> maps) {

		Session session = HibernateUtil.currentSession();
		for (final Workspace workspace : workspaces) {
			if (workspace.getParentId() == null) {
				continue;
			}
			if (workspace.getParentId().equals(root)) {

				List<Long> longs = new ArrayList<Long>();

				workspaceTreeModel.getParentCount(parentId, workspace.getId(), workspace.getParentId(), longs);

				Integer deep = longs.size();

				longs = null;

				Map<String, Object> map = new java.util.HashMap<String, Object>();

				map.put("unit_id",
						workspace.getUnitOrganisasi() == null ? null : (workspace.getUnitOrganisasi().getId()));

				map.put("nama_unit",
						workspace.getUnitOrganisasi() == null ? null : (workspace.getUnitOrganisasi().getNama()));

				map.put("workspace_id", workspace.getId());
				map.put("unique_id", workspace.getId());
				map.put("kode", workspace.getKode() == null ? "" : workspace.getKode());
				map.put("nama",
						workspace.getNama() == null ? ""
								: (getStrings(deep) + workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
										: " - " + workspace.getUnitOrganisasi().getNama())));

				map.put("durasi", workspace.getDurasi());
				map.put("mulai", workspace.getMulai() == null ? "" : Common.dateFormat1.get().format(workspace.getMulai()));
				map.put("selesai",
						workspace.getSelesai() == null ? "" : Common.dateFormat1.get().format(workspace.getSelesai()));

				map.put("harga_total", workspace.getHargaTotal().equals(0.0) ? null : workspace.getHargaTotal());

				List<WorkspacePunyaPegawai> workspacePunyaPegawais = session.createCriteria(WorkspacePunyaPegawai.class)
						.add(Restrictions.eq("workspace", workspace)).list();
				String pegs = "";
				if (workspacePunyaPegawais != null) {
					int i = 0;
					for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
						if (i == 3) {
							break;
						}
						Pegawai pegawai = workspacePunyaPegawai.getPegawai();
						pegs += (pegs.equals("") ? pegawai.getNama() : ", " + pegawai.getNama());
						i++;
					}
				}

				map.put("dikerjakan",
						(workspacePunyaPegawais == null ? "0" : workspacePunyaPegawais.size()) + " peg. " + pegs);

				if (hanyaChild) {
					if (workspaceTreeModel.getChildCount(workspace) == 0) {
						maps.add(map);
					}
				} else {
					maps.add(map);
				}
				generateJadwalAnggaran(parentId, workspaceTreeModel, workspace.getId(), hanyaChild, workspaces, maps);

			}
		}

	}

	@SuppressWarnings("unchecked")
	public void generateJadwalAnggaranPerUnit(final Long parentId, final WorkspaceTreeModel workspaceTreeModel,
			final Long root, Collection<Workspace> workspaces, List<Map<String, Object>> maps) {

		Session session = HibernateUtil.currentSession();
		for (final Workspace workspace : workspaces) {
			if (workspace.getParentId() == null) {
				continue;
			}
			if (workspace.getParentId().equals(root)) {

				List<Long> longs = new ArrayList<Long>();

				workspaceTreeModel.getParentCount(parentId, workspace.getId(), workspace.getParentId(), longs);

				Integer deep = longs.size();

				longs = null;

				Map<String, Object> map = new java.util.HashMap<String, Object>();

				map.put("unit_id",
						workspace.getUnitOrganisasi() == null ? null : (workspace.getUnitOrganisasi().getId()));

				map.put("nama_unit",
						workspace.getUnitOrganisasi() == null ? null : (workspace.getUnitOrganisasi().getNama()));

				map.put("workspace_id", workspace.getId());
				map.put("unique_id", workspace.getId());
				map.put("kode", workspace.getKode() == null ? "" : workspace.getKode());
				map.put("nama",
						workspace.getNama() == null ? ""
								: (getStrings(deep) + workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
										: " - " + workspace.getUnitOrganisasi().getNama())));

				map.put("durasi", workspace.getDurasi());
				map.put("mulai", workspace.getMulai() == null ? "" : Common.dateFormat1.get().format(workspace.getMulai()));
				map.put("selesai",
						workspace.getSelesai() == null ? "" : Common.dateFormat1.get().format(workspace.getSelesai()));

				Double hargaTotal = getHargaTotal(workspaceTreeModel, workspace, workspaces);
				map.put("harga_total", hargaTotal);

				List<WorkspacePunyaPegawai> workspacePunyaPegawais = session.createCriteria(WorkspacePunyaPegawai.class)
						.add(Restrictions.eq("workspace", workspace)).list();
				String pegs = "";
				if (workspacePunyaPegawais != null) {
					int i = 0;
					for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
						if (i == 3) {
							break;
						}
						Pegawai pegawai = workspacePunyaPegawai.getPegawai();
						pegs += (pegs.equals("") ? pegawai.getNama() : ", " + pegawai.getNama());
						i++;
					}
				}

				map.put("dikerjakan",
						(workspacePunyaPegawais == null ? "0" : workspacePunyaPegawais.size()) + " peg. " + pegs);

				maps.add(map);

				generateJadwalAnggaranPerUnit(parentId, workspaceTreeModel, root, workspaces, maps);

			}
		}

	}

	public Integer getMaxrevisi() {
		return maxrevisi;
	}

}
