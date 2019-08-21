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
    boolean onClick;
    ArrayList<Element> clickElementList;
    PsiElementFactory psiElementFactory;
    private static final int ACTIVITY = 0;
    private static final int FRAGMENT = 1;
    private static final int SETCONTENTVIEW = 2;
    private static final int INFLATE = 3;
    private int initViewType = 2;
    private int offset;

    public InjectWriter(PsiElement psiElement, PsiFile psiFile, PsiClass psiClass, ArrayList<Element> elementList,
                        String viewHolderName, boolean onClick, ArrayList<Element> clickElementList) {
        super(psiClass.getProject(), psiFile);
        project = psiClass.getProject();
        this.psiElement = psiElement;
        this.psiFile = psiFile;
        this.psiClass = psiClass;
        this.elementList = elementList;
        this.viewHolderName = viewHolderName;
        this.onClick = onClick;
        this.clickElementList = clickElementList;
        psiElementFactory = JavaPsiFacade.getElementFactory(project);
    }

    @Override
    protected void run(@NotNull Result result) throws Throwable {
        wirteFindView();
        generateClicks();
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
            boolean hasInitView = false;
            boolean hasInitListener = false;
            for (PsiStatement statement : onCreate.getBody().getStatements()) {
                if (statement.getFirstChild() instanceof PsiMethodCallExpression) {
                    PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) statement.getFirstChild()).getMethodExpression();
                    if (methodExpression.getText().contains("initView")) {
                        hasInitView = true;
                    } else if (methodExpression.getText().contains("initListener")) {
                        hasInitListener = true;
                    }
                }
            }
            for (PsiStatement statement : onCreate.getBody().getStatements()) {
                // 寻找setContentView语句
                if (statement.getFirstChild() instanceof PsiMethodCallExpression) {
                    PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) statement.getFirstChild()).getMethodExpression();
                    if (methodExpression.getText().contains("setContentView")) {
                        if (shouldGenerateClicks() && !onClick && !hasInitListener && !hasInitView) {
                            PsiElement initListenerStatement = psiElementFactory.createStatementFromText("initListener();", psiClass);
                            onCreate.getBody().addAfter(initListenerStatement, statement);
                        }
                        if (!hasInitView) {
                            PsiElement initViewStatement = psiElementFactory.createStatementFromText("initView();", psiClass);
                            onCreate.getBody().addAfter(initViewStatement, statement);
                        }
                    } else if (methodExpression.getText().contains("initView")) {
                        if (shouldGenerateClicks() && !onClick && !hasInitListener) {
                            PsiElement initListenerStatement = psiElementFactory.createStatementFromText("initListener();", psiClass);
                            onCreate.getBody().addAfter(initListenerStatement, statement);
                        }
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
            boolean hasInitListener = false;
            for (PsiStatement statement : onCreateView.getBody().getStatements()) {
                if (statement.getFirstChild() instanceof PsiMethodCallExpression) {
                    PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) statement.getFirstChild()).getMethodExpression();
                    if (methodExpression.getText().contains("initListener")) {
                        hasInitListener = true;
                    }
                }
            }
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
                    if (shouldGenerateClicks() && !onClick && !hasInitListener) {
                        PsiElement initListenerStatement = psiElementFactory.createStatementFromText("initListener();", psiClass);
                        onCreateView.getBody().addBefore(initListenerStatement, statement);
                    }
                    break;
                }
            }
        }
        initViewType = FRAGMENT;
        initView();
    }

    private void generateViews() {
        boolean hasInitView = false;
        boolean hasInitListener = false;
        PsiCodeBlock psiCodeBlock = null;
        PsiElement element = psiElement;
        int count = 0;
        while (true) {
            if ((element = element.getParent()) instanceof PsiCodeBlock) {
                psiCodeBlock = (PsiCodeBlock) element;
                break;
            }
            count++;
            if (count >= 10) {
                break;
            }
        }
        for (PsiStatement statement : psiCodeBlock.getStatements()) {
            if (statement.getFirstChild() instanceof PsiMethodCallExpression) {
                PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) statement.getFirstChild()).getMethodExpression();
                if (methodExpression.getText().contains("initView")) {
                    hasInitView = true;
                }
                if (methodExpression.getText().contains("initListener")) {
                    hasInitListener = true;
                }
            }
        }
        for (PsiStatement statement : psiCodeBlock.getStatements()) {
            if (statement.getFirstChild() instanceof PsiMethodCallExpression) {
                PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) statement.getFirstChild()).getMethodExpression();
                if (methodExpression.getText().contains("setContentView")) {
                    if (shouldGenerateClicks() && !onClick && !hasInitListener && !hasInitView) {
                        PsiElement initListenerStatement = psiElementFactory.createStatementFromText("initListener();", psiClass);
                        statement.getParent().addAfter(initListenerStatement, statement);
                    }
                    if (!hasInitView) {
                        PsiElement initViewStatement = psiElementFactory.createStatementFromText("initView();", psiClass);
                        statement.getParent().addAfter(initViewStatement, statement);
                    }
                    initViewType = SETCONTENTVIEW;
                    initView();
                } else if (methodExpression.getText().contains("inflate")) {
                    if (shouldGenerateClicks() && !onClick && !hasInitListener && !hasInitView) {
                        PsiElement initListenerStatement = psiElementFactory.createStatementFromText("initListener();", psiClass);
                        statement.getParent().addAfter(initListenerStatement, statement);
                    }
                    initViewType = INFLATE;
                    initView();
                } else if (methodExpression.getText().contains("initView")) {
                    if (shouldGenerateClicks() && !onClick && !hasInitListener) {
                        PsiElement initListenerStatement = psiElementFactory.createStatementFromText("initListener();", psiClass);
                        statement.getParent().addAfter(initListenerStatement, statement);
                    }
                    break;
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
                    if (onClick && clickElementList.contains(element)) {
                        String onClickStatement = element.getName() + ".setOnClickListener(this);";
                        PsiStatement onClickPsiStatement = psiElementFactory.createStatementFromText(onClickStatement, psiMethod);
                        psiMethod.getBody().add(onClickPsiStatement);
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
                    if (onClick && clickElementList.contains(element)) {
                        String onClickStatement = element.getName() + ".setOnClickListener(this);";
                        PsiStatement onClickPsiStatement = psiElementFactory.createStatementFromText(onClickStatement, psiMethod);
                        psiMethod.getBody().add(onClickPsiStatement);
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

    private boolean shouldGenerateClicks() {
        if (clickElementList.size() == 0) {
            return false;
        }
        return true;
    }

    private void generateClicks() {
        if (clickElementList.size() == 0) {
            return;
        }
        if (onClick) {
            boolean hasImpl = false;
            boolean hasImplKeyword = false;
            PsiReferenceList psiReferenceList = psiClass.getImplementsList();
            if (psiReferenceList != null) {
                for (PsiJavaCodeReferenceElement element : psiReferenceList.getReferenceElements()) {
                    hasImplKeyword = true;
                    if (ClassTypeUtils.clickInterfaces.contains(element.getQualifiedName())) {
                        hasImpl = true;
                    }
                }
            }
            if (!hasImpl) {
                PsiElement[] psiElements = psiClass.getChildren();
                for (PsiElement p : psiElements) {
                    if ("{".equals(p.getText())) {
                        if (hasImplKeyword) {
                            break;
                        } else {
                            psiClass.addBefore(psiElementFactory.createKeyword("implements"), p);
                            PsiStatement psiStatement = psiElementFactory.createStatementFromText("android.view.View.OnClickListener", psiClass);
                            psiClass.addBefore(psiStatement, p);
                            break;
                        }
                    }
                }
            }
            PsiMethod[] psiMethods = psiClass.getAllMethods();
            PsiMethod psiMethod = null;
            for (PsiMethod method : psiMethods) {
                if ("onClick".equals(method.getName())) {
                    psiMethod = method;
                    break;
                }
            }
            if (psiMethod == null) {
                String methodString = "@Override\n" +
                        "    public void onClick(android.view.View view) {\n" +
                        "        switch (view.getId()) {\n";
                for (Element element : clickElementList) {
                    String caseLine = "case R.id." + element.getId() + ":\n";
                    methodString += caseLine;
                    String breakLine = "break;\n";
                    methodString += breakLine;
                }
                methodString += "        }\n" +
                        "    }";
                psiClass.add(psiElementFactory.createMethodFromText(methodString, psiClass));
            } else {
                PsiElement switchPsiElement = null;
                for (PsiElement statement : psiMethod.getBody().getStatements()[0].getLastChild().getChildren()) {
                    if (statement.getText().equals("}")) {
                        switchPsiElement = statement;
                        break;
                    }
                }
                for (Element element : clickElementList) {
                    String caseLine = "case R.id." + element.getId() + ":\n";
                    PsiStatement caseStatement = psiElementFactory.createStatementFromText(caseLine, psiMethod);
                    psiMethod.getBody().addBefore(caseStatement, switchPsiElement);
                    String breakLine = "break;\n";
                    PsiStatement breakStatement = psiElementFactory.createStatementFromText(breakLine, psiMethod);
                    psiMethod.getBody().addBefore(breakStatement, switchPsiElement);
                }
            }
        } else {
            PsiMethod[] psiMethods = psiClass.getAllMethods();
            PsiMethod psiMethod = null;
            for (PsiMethod method : psiMethods) {
                if ("initListener".equals(method.getName())) {
                    psiMethod = method;
                    break;
                }
            }
            if (psiMethod == null) {
                psiMethod = (PsiMethod) psiClass.add(psiElementFactory.createMethodFromText("private void initListener() {}", psiClass));
            }
            for (Element element : clickElementList) {
                String methodLine = element.getName() + ".setOnClickListener(new android.view.View.OnClickListener() {\n" +
                        "            @Override\n" +
                        "            public void onClick(android.view.View view) {\n" +
                        "\n" +
                        "            }\n" +
                        "        });";
                PsiStatement psiStatement = psiElementFactory.createStatementFromText(methodLine, psiMethod);
                psiMethod.getBody().add(psiStatement);
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
