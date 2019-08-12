package cn.xydzjnq.generateview;

import cn.xydzjnq.generateview.util.JBPopupUtils;
import cn.xydzjnq.generateview.util.PsiFileUtils;
import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import java.util.ArrayList;

public class GenerateViewAction extends BaseGenerateAction {

    public GenerateViewAction() {
        super(null);
    }

    protected GenerateViewAction(CodeInsightActionHandler handler) {
        super(handler);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiClass psiClass = getTargetClass(editor, psiFile);
        CaretModel caretModel = editor.getCaretModel();
        PsiElement psiElement = psiFile.findElementAt(caretModel.getOffset());
        if (psiElement != null && psiElement.getParent().getText().contains("R.layout.")) {
            String name = String.format("%s.xml", psiElement.getText());
            PsiFile[] psiFiles = PsiFileUtils.getPsiFiles(psiElement, name);
            if (psiFiles.length != 0) {
                PsiFile xmlPsiFile = psiFiles[0];
                ArrayList<Element> elementList = new ArrayList<>();
                PsiFileUtils.parseElements(xmlPsiFile, elementList);
                new TemplateDialog(psiClass, elementList);
            } else {
                JBPopupUtils.showError(project, "没有找到对应的xml文件");
            }
        } else {
            JBPopupUtils.showError(project, "请定位到要生成的view处");
        }
    }
}
