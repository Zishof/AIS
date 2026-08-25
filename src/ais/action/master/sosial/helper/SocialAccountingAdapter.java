package ais.action.master.sosial.helper;
import org.hibernate.Session; import ais.database.model.sosial.*;
/** Accounting boundary is deliberately fail-closed until real balanced journal posting exists. */
public class SocialAccountingAdapter { public void postPayment(Session s,SocialRequestContext c,TransaksiDonasi d){unavailable();} public void postDistribution(Session s,SocialRequestContext c,DetailPenyaluranDonasi d){unavailable();} public void postCorrection(Session s,SocialRequestContext c,SocialCorrectionEvent e){unavailable();} private void unavailable(){throw new IllegalStateException("Integrasi akuntansi Sosial berstatus STUB_NOOP dan tidak boleh diaktifkan.");} }
