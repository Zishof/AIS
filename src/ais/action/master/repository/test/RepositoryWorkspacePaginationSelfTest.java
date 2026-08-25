package ais.action.master.repository.test;

import java.lang.reflect.Method;
import ais.action.master.repository.RepositoryWorkflowService;

public final class RepositoryWorkspacePaginationSelfTest {
    private RepositoryWorkspacePaginationSelfTest(){}
    public static void main(String[] args)throws Exception{Method size=RepositoryWorkflowService.class.getDeclaredMethod("pageSize",int.class);size.setAccessible(true);check(((Integer)size.invoke(null,-1)).intValue()==20,"Fallback page size salah.");check(((Integer)size.invoke(null,1)).intValue()==5,"Minimum page size salah.");check(((Integer)size.invoke(null,999)).intValue()==100,"Maximum page size salah.");Method workflow=RepositoryWorkflowService.class.getDeclaredMethod("workflowStatus",String.class);workflow.setAccessible(true);check("DRAFT".equals(workflow.invoke(null,"draft")),"Normalisasi status gagal.");check("".equals(workflow.invoke(null,"DROP TABLE")),"Status asing tidak ditolak.");Method review=RepositoryWorkflowService.class.getDeclaredMethod("reviewStatus",String.class);review.setAccessible(true);check("IN_REVIEW".equals(review.invoke(null,"IN_REVIEW")),"Status review valid ditolak.");check("".equals(review.invoke(null,"PUBLISHED")),"Status non-antrean diterima.");System.out.println("RepositoryWorkspacePaginationSelfTest OK bounds and status allow-list");}
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
