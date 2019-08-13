package cn.xydzjnq.generateview;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class InjectWriter extends WriteCommandAction {
    Project project;
    PsiClass psiClass;
    ArrayList<Element> elementList;
    PsiElementFactory psiElementFactory;

    public InjectWriter(PsiClass psiClass, ArrayList<Element> elementList) {
        super(psiClass.getProject());
        project = psiClass.getProject();
        this.psiClass = psiClass;
        this.elementList = elementList;
        psiElementFactory = JavaPsiFacade.getElementFactory(project);
    }

    @Override
    protected void run(@NotNull Result result) throws Throwable {
        wirteFindView();
    }

    private void wirteFindView() {
        PsiMethod onCreate = psiClass.findMethodsByName("onCreate", false)[0];
        for (PsiStatement statement : onCreate.getBody().getStatements()) {
            // 寻找setContentView语句
            if (statement.getFirstChild() instanceof PsiMethodCallExpression) {
                PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) statement.getFirstChild()).getMethodExpression();
                if (methodExpression.getText().equals("setContentView")) {
                    onCreate.getBody().addAfter(psiElementFactory.createStatementFromText("findViews();", psiClass), statement);
                    break;
                }
            }
        }
        PsiMethod psiMethod = (PsiMethod) psiClass.add(psiElementFactory.createMethodFromText("private void findViews() {}", psiClass));
        for (Element element : elementList) {
            String field = "private " + element.getType() + " " + element.getName() + ";";
            PsiField psiField = psiElementFactory.createFieldFromText(field, psiClass);
            psiClass.add(psiField);
            String methodLine = element.getName() + " = (" + element.getType() + ")findViewById(R.id." + element.getId() + ");";
            PsiStatement psiStatement = psiElementFactory.createStatementFromText(methodLine, psiClass);
            psiMethod.getBody().add(psiStatement);
        }
    }
}
