package cn.xydzjnq.generateview;

import cn.xydzjnq.generateview.util.ClassTypeUtils;
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
    PsiElement psiElement;
    PsiFile psiFile;
    PsiClass psiClass;
    ArrayList<Element> elementList;
    PsiElementFactory psiElementFactory;
    private static final int ACTIVITY = 0;
    private static final int FRAGMENT = 1;
    private static final int SETCONTENTVIEW = 2;
    private static final int INFLATE = 3;
    private int initViewType = 2;

    public InjectWriter(PsiElement psiElement, PsiFile psiFile, PsiClass psiClass, ArrayList<Element> elementList) {
        super(psiClass.getProject(), psiFile);
        project = psiClass.getProject();
        this.psiElement = psiElement;
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
        PsiReferenceList psiReferenceList = psiClass.getExtendsList();
        if (psiReferenceList != null) {
            for (PsiJavaCodeReferenceElement element : psiReferenceList.getReferenceElements()) {
                if (ClassTypeUtils.activitys.contains(element.getQualifiedName())) {
                    generateActivityViews();
                    return;
                } else if (ClassTypeUtils.fragments.contains(element.getQualifiedName())) {
                    generateFragmentViews();
                    return;
                } else if (ClassTypeUtils.adapters.contains(element.getQualifiedName())) {
                    generateViewHolder();
                    return;
                }
            }
        }
        generateViews();
    }

    private void generateActivityViews() {
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
        initViewType = ACTIVITY;
        initView();
    }

    private void generateFragmentViews() {
        PsiMethod[] onCreateViews = psiClass.findMethodsByName("onCreateView", false);
        if (onCreateViews != null && onCreateViews.length > 0) {
            PsiMethod onCreateView = onCreateViews[0];
            for (PsiStatement statement : onCreateView.getBody().getStatements()) {
                // 寻找inflater.inflate语句
                // 这里只判断了返回语句
                if (statement instanceof PsiReturnStatement) {
                    String returnValue = ((PsiReturnStatement) statement).getReturnValue().getText();
                    if (returnValue.contains("inflater.inflate")) {
                        onCreateView.getBody().addBefore(psiElementFactory.createStatementFromText("android.view.View view = "
                                + returnValue + ";", psiClass), statement);
                        onCreateView.getBody().addBefore(psiElementFactory.createStatementFromText("initView(view);",
                                psiClass), statement);
                        statement.replace(psiElementFactory.createStatementFromText("return view;", psiClass));
                    }
                    break;
                }
            }
        }
        initViewType = FRAGMENT;
        initView();
    }

    private void generateViews() {
        PsiElement parent = psiElement.getParent().getParent().getParent().getParent();
        if (parent != null) {
            if (parent instanceof PsiStatement) {
                PsiElement[] psiElements = parent.getChildren();
                for (PsiElement psiMethodCallExpression : psiElements) {
                    if (psiMethodCallExpression instanceof PsiMethodCallExpression) {
                        PsiReferenceExpression psiReferenceExpression = ((PsiMethodCallExpression) psiMethodCallExpression).getMethodExpression();
                        if (psiReferenceExpression.getLastChild().getText().equals("setContentView")) {
                            parent.getParent().addAfter(psiElementFactory.createStatementFromText("initView();", psiClass), parent);
                            initViewType = SETCONTENTVIEW;
                            initView();
                        } else if (psiReferenceExpression.getLastChild().getText().equals("inflate")) {
                            initViewType = INFLATE;
                            initView();
                        }
                    }
                }
            }
        }
    }

    private void initView() {
        switch (initViewType) {
            case ACTIVITY:
            case SETCONTENTVIEW: {
                PsiMethod psiMethod = (PsiMethod) psiClass.add(psiElementFactory.createMethodFromText("private void initView() {}", psiClass));
                for (Element element : elementList) {
                    String field = null;
                    if (element.getType().contains(".")) {
                        field = "private " + element.getType() + " " + element.getName() + ";";
                    } else if (ClassTypeUtils.viewPaths.containsKey(element.getType())) {
                        field = ClassTypeUtils.viewPaths.get(element.getType()) + " " + element.getName() + ";";
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
            break;
            case FRAGMENT:
            case INFLATE: {
                PsiMethod psiMethod = (PsiMethod) psiClass.add(psiElementFactory.createMethodFromText("private void initView(android.view.View view) {}", psiClass));
                for (Element element : elementList) {
                    String field = null;
                    if (element.getType().contains(".")) {
                        field = "private " + element.getType() + " " + element.getName() + ";";
                    } else if (ClassTypeUtils.viewPaths.containsKey(element.getType())) {
                        field = ClassTypeUtils.viewPaths.get(element.getType()) + " " + element.getName() + ";";
                    } else {
                        field = "private android.widget." + element.getType() + " " + element.getName() + ";";
                    }
                    PsiField psiField = psiElementFactory.createFieldFromText(field, psiClass);
                    psiClass.add(psiField);
                    String methodLine = element.getName() + " = view.findViewById(R.id." + element.getId() + ");";
                    PsiStatement psiStatement = psiElementFactory.createStatementFromText(methodLine, psiClass);
                    psiMethod.getBody().add(psiStatement);
                }
            }
            break;
        }
    }

    private void generateViewHolder() {
        String psiClassName = psiClass.getName();
        if (psiClassName.indexOf("Adapter") > -1) {
            psiClassName = psiClassName.substring(0, psiClassName.indexOf("Adapter"));
        }
        String viewHolderName = psiClassName + "ViewHolder";
        PsiClass viewHolder = psiElementFactory.createClass(viewHolderName);
        PsiModifierList classModifierList = viewHolder.getModifierList();
        if (classModifierList != null) {
            classModifierList.setModifierProperty(PsiModifier.PUBLIC, true);
            classModifierList.setModifierProperty(PsiModifier.STATIC, true);
        }
        initView(viewHolder, viewHolderName);
        psiClass.add(viewHolder);
    }

    private void initView(PsiClass psiClazz, String psiClassName) {
        PsiMethod psiMethod = (PsiMethod) psiClazz.add(psiElementFactory.createMethodFromText("public "
                + psiClassName + "(android.view.View itemView) {}", psiClazz));
        for (Element element : elementList) {
            String field = null;
            if (element.getType().contains(".")) {
                field = "private " + element.getType() + " " + element.getName() + ";";
            } else if (ClassTypeUtils.viewPaths.containsKey(element.getType())) {
                field = ClassTypeUtils.viewPaths.get(element.getType()) + " " + element.getName() + ";";
            } else {
                field = "private android.widget." + element.getType() + " " + element.getName() + ";";
            }
            PsiField psiField = psiElementFactory.createFieldFromText(field, psiClazz);
            psiClazz.add(psiField);
            String methodLine = element.getName() + " = itemView.findViewById(R.id." + element.getId() + ");";
            PsiStatement psiStatement = psiElementFactory.createStatementFromText(methodLine, psiClazz);
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
