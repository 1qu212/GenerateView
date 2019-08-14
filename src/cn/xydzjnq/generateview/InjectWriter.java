package cn.xydzjnq.generateview;

import cn.xydzjnq.generateview.util.PathUtils;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class InjectWriter extends WriteCommandAction {
    Project project;
    PsiFile psiFile;
    PsiClass psiClass;
    ArrayList<Element> elementList;
    PsiElementFactory psiElementFactory;

    public InjectWriter(PsiFile psiFile, PsiClass psiClass, ArrayList<Element> elementList) {
        super(psiClass.getProject(), psiFile);
        project = psiClass.getProject();
        this.psiFile = psiFile;
        this.psiClass = psiClass;
        this.elementList = elementList;
        psiElementFactory = JavaPsiFacade.getElementFactory(project);
    }

    @Override
    protected void run(@NotNull Result result) throws Throwable {
        wirteFindView();
        importPackage();
    }

    private void wirteFindView() {
        PsiMethod[] onCreates = psiClass.findMethodsByName("onCreate", false);
        if (onCreates != null && onCreates.length > 0) {
            PsiMethod onCreate = onCreates[0];
            for (PsiStatement statement : onCreate.getBody().getStatements()) {
                // 寻找setContentView语句
                if (statement.getFirstChild() instanceof PsiMethodCallExpression) {
                    PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) statement.getFirstChild()).getMethodExpression();
                    if (methodExpression.getText().equals("setContentView")) {
                        onCreate.getBody().addAfter(psiElementFactory.createStatementFromText("initView();", psiClass), statement);
                        break;
                    }
                }
            }
        }
        PsiMethod psiMethod = (PsiMethod) psiClass.add(psiElementFactory.createMethodFromText("private void initView() {}", psiClass));
        for (Element element : elementList) {
            String field = null;
            if (element.getType().contains(".")) {
                field = "private " + element.getType() + " " + element.getName() + ";";
            } else if (PathUtils.viewPaths.containsKey(element.getType())) {
                field = PathUtils.viewPaths.get(element.getType()) + " " + element.getName() + ";";
            } else {
                field = "private android.widget." + element.getType() + " " + element.getName() + ";";
            }
            PsiField psiField = psiElementFactory.createFieldFromText(field, psiClass);
            psiClass.add(psiField);
            String methodLine = element.getName() + " = findViewById(R.id." + element.getId() + ");";
            PsiStatement psiStatement = psiElementFactory.createStatementFromText(methodLine, psiClass);
            psiMethod.getBody().add(psiStatement);
        }
    }

    private void importPackage() {
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
        styleManager.optimizeImports(psiFile);
        styleManager.shortenClassReferences(psiClass);
        new ReformatCodeProcessor(project, psiFile, null, false).runWithoutProgress();
    }
}
