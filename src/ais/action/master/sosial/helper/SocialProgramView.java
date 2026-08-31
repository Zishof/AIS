package ais.action.master.sosial.helper;
import java.math.BigDecimal;
/**
 * DTO ringkas (bukan entitas Hibernate) untuk menampilkan satu program donasi/sosial pada
 * antarmuka publik: identitas ({@code id}, {@code slug}, {@code name}), ringkasan ({@code summary},
 * {@code story}, {@code cover}), kategori dana ({@code fundTypeId}/{@code fundType}), lokasi, serta
 * angka agregat penggalangan dana ({@code target}, {@code collected}, {@code distributed},
 * {@code balance}, {@code minimum} donasi, {@code beneficiaries}). Flag {@code featured} menandai
 * program yang ditonjolkan, {@code restricted} menandai dana yang penggunaannya dibatasi (mis. hanya
 * untuk kategori tertentu). Digunakan oleh {@link SocialDistributionService} sebagai bentuk data yang
 * dikirim ke tampilan/API tanpa membawa seluruh graf entitas Hibernate.
 */
public final class SocialProgramView { public Long id,fundTypeId; public String slug,name,summary,story,cover,fundType,unit,location; public BigDecimal target,collected,distributed,balance,minimum; public Integer beneficiaries; public boolean featured,restricted; }
