package ais.action.master.generic.v2.adapter;

import java.io.Serializable;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;

/**
 * Kontrak adapter alur persetujuan (approval workflow) untuk kerangka CRUD generik
 * {@code generic/v2}: memungkinkan entitas yang dikelola action-layer generik memiliki langkah
 * setuju/tolak sebelum data dianggap final, tanpa mengikat kerangka generik ke logika bisnis
 * persetujuan spesifik tiap entitas.
 */
public interface GenericCrudApprovalAdapter {
    /** Menyetujui baris ber-{@code id}, mencatat {@code reason} sebagai alasan/catatan persetujuan pada jejak audit. */
    GenericCrudResult approve(Serializable id, String reason, GenericCrudRequestContext context) throws Exception;
    /** Menolak baris ber-{@code id}, mencatat {@code reason} sebagai alasan penolakan pada jejak audit. */
    GenericCrudResult reject(Serializable id, String reason, GenericCrudRequestContext context) throws Exception;
}
