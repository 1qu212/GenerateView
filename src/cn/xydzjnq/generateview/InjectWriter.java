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
import java.util.List;

public class InjectWriter extends WriteCommandAction {
    Project project;
    PsiElement psiElement;
    PsiFile psiFile;
    PsiClass psiClass;
    ArrayList<Element> elementList;
    String viewHolderName;
    PsiElementFactory psiElementFactory;
    private static final int ACTIVITY = 0;
    private static final int FRAGMENT = 1;
    private static final int SETCONTENTVIEW = 2;
    private static final int INFLATE = 3;
    private int initViewType = 2;
    private int offset;

    public InjectWriter(PsiElement psiElement, PsiFile psiFile, PsiClass psiClass, ArrayList<Element> elementList, String viewHolderName) {
        super(psiClass.getProject(), psiFile);
        project = psiClass.getProject();
        this.psiElement = psiElement;
        this.psiFile = psiFile;
        this.psiClass = psiClass;
        this.elementList = elementList;
        this.viewHolderName = viewHolderName;
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
                } else if (ClassTypeUtils.recycleViewAdapters.contains(element.getQualifiedName())) {
                    generateViewHolder(true);
                    return;
                } else if (ClassTypeUtils.adapters.contains(element.getQualifiedName())) {
                    generateViewHolder(false);
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
                PsiMethod[] psiMethods = psiClass.getAllMethods();
                PsiMethod psiMethod = null;
                for (PsiMethod method : psiMethods) {
                    if ("initView".equals(method.getName())) {
                        psiMethod = method;
                        break;
                    }
                }
                if (psiMethod == null) {
                    psiMethod = (PsiMethod) psiClass.add(psiElementFactory.createMethodFromText("private void initView() {}", psiClass));
                }
                List<String> statementTextList = new ArrayList<>();
                for (PsiStatement statement : psiMethod.getBody().getStatements()) {
                    statementTextList.add(statement.getText());
                }
                for (Element element : elementList) {
                    String methodLine = element.getName() + " = findViewById(R.id." + element.getId() + ");";
                    PsiStatement psiStatement = psiElementFactory.createStatementFromText(methodLine, psiMethod);
                    if (!statementTextList.contains(psiStatement.getText())) {
                        psiMethod.getBody().add(psiStatement);
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
                    }
                }
            }
            break;
            case FRAGMENT:
            case INFLATE: {
                PsiMethod[] psiMethods = psiClass.getAllMethods();
                PsiMethod psiMethod = null;
                for (PsiMethod method : psiMethods) {
                    if ("initView".equals(method.getName())) {
                        psiMethod = method;
                        break;
                    }
                }
                if (psiMethod == null) {
                    psiMethod = (PsiMethod) psiClass.add(psiElementFactory.createMethodFromText("private void initView(android.view.View view) {}", psiClass));
                }
                List<String> statementTextList = new ArrayList<>();
                for (PsiStatement statement : psiMethod.getBody().getStatements()) {
                    statementTextList.add(statement.getText());
                }
                for (Element element : elementList) {
                    String methodLine = element.getName() + " = view.findViewById(R.id." + element.getId() + ");";
                    PsiStatement psiStatement = psiElementFactory.createStatementFromText(methodLine, psiMethod);
                    if (!statementTextList.contains(psiStatement.getText())) {
                        psiMethod.getBody().add(psiStatement);
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
                    }
                }
            }
            break;
        }
    }

    private void generateViewHolder(boolean isRecycleViewHolder) {
        PsiClass[] psiClasses = psiClass.getAllInnerClasses();
        PsiClass viewHolder = null;
        PsiMethod psiMethod = null;
        for (PsiClass clazz : psiClasses) {
            if (viewHolderName.equals(clazz.getName())) {
                viewHolder = clazz;
                PsiMethod[] psiMethods = viewHolder.getAllMethods();
                for (PsiMethod method : psiMethods) {
                    if (viewHolderName.equals(method.getName())) {
                        psiMethod = method;
                        break;
                    }
                }
                break;
            }
        }
        if (viewHolder == null) {
            viewHolder = psiElementFactory.createClass(viewHolderName);
            PsiModifierList classModifierList = viewHolder.getModifierList();
            if (classModifierList != null) {
                classModifierList.setModifierProperty(PsiModifier.PUBLIC, true);
                classModifierList.setModifierProperty(PsiModifier.STATIC, true);
            }
            if (isRecycleViewHolder) {
                offset = viewHolder.getTextOffset();
                PsiElement psiElement = viewHolder.findElementAt(offset + 3);
                PsiKeyword psiKeyword = (PsiKeyword) viewHolder.addAfter(psiElementFactory.createKeyword("extends"), psiElement);
                PsiStatement psiStatement = psiElementFactory.createStatementFromText("RecyclerView.ViewHolder", viewHolder);
                viewHolder.addAfter(psiStatement, psiKeyword);
            }
            psiMethod = (PsiMethod) viewHolder.add(psiElementFactory.createMethodFromText("public "
                    + viewHolderName + "(android.view.View itemView) {}", viewHolder));
            if (isRecycleViewHolder) {
                PsiStatement psiStatement = psiElementFactory.createStatementFromText("super(itemView);", viewHolder);
                psiMethod.getBody().add(psiStatement);
            }
            initView(viewHolder, psiMethod);
            psiClass.add(viewHolder);
        } else {
            initView(viewHolder, psiMethod);
        }
    }

    private void initView(PsiClass viewHolder, PsiMethod psiMethod) {
        List<String> statementTextList = new ArrayList<>();
        for (PsiStatement statement : psiMethod.getBody().getStatements()) {
            statementTextList.add(statement.getText());
        }
        for (Element element : elementList) {
            String methodLine = element.getName() + " = itemView.findViewById(R.id." + element.getId() + ");";
            PsiStatement psiStatement = psiElementFactory.createStatementFromText(methodLine, viewHolder);
            if (!statementTextList.contains(psiStatement.getText())) {
                psiMethod.getBody().add(psiStatement);
                String field = null;
                if (element.getType().contains(".")) {
                    field = "private " + element.getType() + " " + element.getName() + ";";
                } else if (ClassTypeUtils.viewPaths.containsKey(element.getType())) {
                    field = ClassTypeUtils.viewPaths.get(element.getType()) + " " + element.getName() + ";";
                } else {
                    field = "private android.widget." + element.getType() + " " + element.getName() + ";";
                }
                PsiField psiField = psiElementFactory.createFieldFromText(field, viewHolder);
                viewHolder.add(psiField);
            }
        }
    }

    private void importPackage() {
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
        styleManager.optimizeImports(psiFile);
        styleManager.shortenClassReferences(psiClass);
        new ReformatCodeProcessor(project, psiFile, null, false).runWithoutProgress();
    }
}
