package cn.xydzjnq.generateview;

import cn.xydzjnq.generateview.util.JBPopupUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

import javax.swing.*;

public class GenerateViewAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        CaretModel caretModel = editor.getCaretModel();
        PsiElement psiElement = psiFile.findElementAt(caretModel.getOffset());
        if (psiElement != null && psiElement.getParent().getText().contains("R.layout.")) {
            String name = String.format("%s.xml", psiElement.getText());
            Module module = ModuleUtil.findModuleForPsiElement(psiElement);
            GlobalSearchScope scope = GlobalSearchScope.moduleScope(module);
            PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, name, scope);
            if (psiFiles.length == 0) {
                psiFiles = FilenameIndex.getFilesByName(project, name, new EverythingGlobalScope(project));
            }
            if (psiFiles.length != 0) {
                PsiFile xmlPsiFile = psiFiles[0];
                JDialog jDialog = new JDialog();
                jDialog.setTitle("Generate View");
                jDialog.pack();
                jDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                jDialog.setVisible(true);
            } else {
                JBPopupUtils.showError(project, "没有找到对应的xml文件");
            }
        } else {
            JBPopupUtils.showError(project, "请定位到要生成的view处");
        }
    }
}
